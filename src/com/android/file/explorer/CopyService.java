package com.android.file.explorer;

import java.lang.ref.WeakReference;
import android.app.Service;
import android.os.IBinder;
import android.os.PowerManager;
import android.content.Intent;
import java.util.ArrayList;
import android.widget.TextView;

import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.Context;
import android.net.Uri;
import android.view.Window;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.view.View;
import android.view.View.OnClickListener;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.os.Handler;
import android.widget.RemoteViews;
import android.widget.Toast;
import android.util.Log;

public class CopyService extends Service {
	private final static String TAG = "CopyService";
	final boolean DEBUG = false;

	private void LOG(String str) {
		if (DEBUG) {
			Log.i(TAG, str);
		}
	}

	public final static int COPY_STATUS_IDLE = 0;
	public final static int COPY_STATUS_COPYING = 1;

	private final IBinder mBinder = new ServiceStub(this);
	private static CopyFileUtils mCopyFileUtils = null;
	private static int copystatus = COPY_STATUS_IDLE;

	private boolean is_move_copy = false;
	private Dialog choiceDialog = null;
	private Dialog mRecoverDialog = null;
	private NotificationManager notificationManager = null;
	private Notification progressNotification = null;
	private String sourceString = null;
	private String targetString = null;

	@Override
	public void onCreate() {
		super.onCreate();
		registBroadcastRec();
		LOG(" ________-------------------- >> onCreate()");
		if (notificationManager == null)
			notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		if (notificationManager != null) {
			notificationManager.cancel(0);
		}
	}

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		LOG(" ________-------------------- >> onStart()");
		if (mCopyFileUtils == null)
			mCopyFileUtils = new CopyFileUtils(this);
		if (notificationManager == null)
			notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		try {
			unregisterReceiver(mScanListener);
		} catch (IllegalArgumentException e) {
			Log.d(TAG, "unregisterReceiver error~");
		}
		notificationManager.cancel(0);
		progressNotification = null;
		mHandler.removeMessages(0);
	}

	public void testFun(String string) {
		LOG(" ___________---------    testFun(),   string = " + string);
		return;
	}

	public void startCopy(String targetDir, boolean is_move) {
		final String tmptargetDir = new String(targetDir);
		LOG(" _____--------------- startCopy(),  targetDir = " + targetDir);

		copystatus = COPY_STATUS_COPYING;
		sourceString = CopyFileUtils.multi_path.get(0).file.getPath();
		targetString = FileControl.currently_path;
		mCopyFileUtils.CopyFileInfoArray(tmptargetDir);
		is_move_copy = is_move;
	}

	public void setCopyStatus(int status) {
		LOG(" _____--------------- notifiCopyFinish()");
		copystatus = status;
	}

	public int getCopyStatus() {
		LOG(" _____--------------- getCopyStatus()    copystatus = "
				+ copystatus);
		return copystatus;
	}

	public void notificaFromActivity(boolean isrestart) {
		LOG(" _____--------------- notificaFromActivity()");
		if (!isrestart) {
			if (copystatus == COPY_STATUS_COPYING) {
				CopyFileUtils.is_pause_copy = true;
				if (choiceDialog == null) {
					choiceDialog = new Dialog(this);
					choiceDialog.getWindow().requestFeature(
							Window.FEATURE_NO_TITLE);
					View vb = View.inflate(this, R.layout.choice_dialog_layout,
							null);
					vb.findViewById(R.id.but_runbackground).setOnClickListener(
							mOnClickListener);
					vb.findViewById(R.id.but_cancel).setOnClickListener(
							mOnClickListener);
					choiceDialog.setContentView(vb);
					choiceDialog.setCanceledOnTouchOutside(false);
					choiceDialog.getWindow().setType(
							WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
					choiceDialog.setOnDismissListener(mOnDismissListener);
				}
				choiceDialog.show();
			}
		} else {
			notificationManager.cancel(0);
			progressNotification = null;
			mHandler.removeMessages(0);
		}
	}

	OnClickListener mOnClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.but_runbackground:
				LOG(" CopyService.java_____------mOnClickListener    R.id.but_runbackground");
				CopyFileUtils.is_pause_copy = false;
				progressNotification = createDownloadProgressNotification("Copying");
				notificationManager.notify(0, progressNotification);
				choiceDialog.dismiss();
				mHandler.sendEmptyMessageDelayed(0, 100);
				break;
			case R.id.but_cancel:
				LOG(" CopyService.java_____------mOnClickListener    R.id.but_cancel");
				CopyFileUtils.is_copy_finish = true;
				CopyFileUtils.is_enable_copy = false;
				CopyFileUtils.is_pause_copy = false;
				choiceDialog.dismiss();
				mHandler.sendEmptyMessageDelayed(0, 100);
				break;
			case R.id.recover_dialog_ok:
				CopyFileUtils.is_recover = true;
				CopyFileUtils.is_wait_choice_recover = false;
				CopyFileUtils.mRecoverFile = null;
				mRecoverDialog.dismiss();
				break;
			case R.id.recover_dialog_cancel:
				CopyFileUtils.is_recover = false;
				CopyFileUtils.is_wait_choice_recover = false;
				CopyFileUtils.mRecoverFile = null;
				mRecoverDialog.dismiss();
				break;
			}
		}
	};

	OnDismissListener mOnDismissListener = new OnDismissListener() {
		@Override
		public void onDismiss(DialogInterface dialog) {
		}
	};

	private Handler mHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			if (!CopyFileUtils.is_copy_finish) {
				LOG("CopyService.java--handler--!CopyFileUtils.is_copy_finish");
				if ((CopyFileUtils.cope_now_sourceFile != null)
						&& (CopyFileUtils.cope_now_targetFile != null)) {
					if (progressNotification != null) {
						final RemoteViews contentView = (RemoteViews) progressNotification.contentView;
						// float percent =
						// (((float)CopyFileUtils.mhascopyfilecount) /
						// ((float)CopyFileUtils.mallcopyfilecount)) * 100;
						float percent = ((float) (CopyFileUtils.mhascopyfileLength) / CopyFileUtils.mallcopyfileLength) * 100.0f;
						contentView.setTextViewText(
								R.id.copy_notification_progress_text, ""
										+ percent + "%");
						contentView.setProgressBar(
								R.id.copy_notification_progress_bar, 100,
								(int) percent, false);
						notificationManager.notify(0, progressNotification);
						LOG(" ____------------>  mHandler(),    percent = " + percent + "    "
                                                               + CopyFileUtils.cope_now_targetFile.length());
                                                LOG("speed is high: " + (CopyFileUtils.mIsHighSpeed?"yes":"no"));

					}
				}
				if (CopyFileUtils.mRecoverFile != null) {
					if (mRecoverDialog == null
							|| (mRecoverDialog != null && !mRecoverDialog
									.isShowing())) {
						mRecoverDialog = null;
						mRecoverDialog = new Dialog(CopyService.this);
						mRecoverDialog.getWindow().requestFeature(
								Window.FEATURE_NO_TITLE);
						View vb = View.inflate(CopyService.this,
								R.layout.recover_dialog_layout, null);
						vb.findViewById(R.id.recover_dialog_ok)
								.setOnClickListener(mOnClickListener);
						vb.findViewById(R.id.recover_dialog_cancel)
								.setOnClickListener(mOnClickListener);
						((TextView) (vb.findViewById(R.id.recover_dialog_text)))
								.setText(CopyFileUtils.mRecoverFile
										+ getString(R.string.copy_revocer_text));
						mRecoverDialog.setContentView(vb);
						mRecoverDialog.setCanceledOnTouchOutside(false);
						mRecoverDialog.getWindow().setType(
								WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
						mRecoverDialog.show();
					}
				}
				this.sendEmptyMessageDelayed(0, 500);
			} else {
				
				LOG("CopyService.java--handler--else");
				copystatus = COPY_STATUS_IDLE;
				notificationManager.cancel(0);
				progressNotification = null;

				if (is_move_copy && !CopyFileUtils.is_same_path) {
					is_move_copy = false;
					FileControl.delFileInfo(CopyFileUtils.has_copy_path);
					if (CopyFileUtils.has_copy_path.size() > 0)
						reMedioScan(CopyFileUtils.has_copy_path.get(0).file
								.getParent());
				}
				reMedioScan(FileControl.currently_path);
				if (!CopyFileUtils.is_enable_copy) {
					FileControl.DelFile(CopyFileUtils.mInterruptFile);
				}
				CopyFileUtils.is_copy_finish = false;
				if (CopyFileUtils.is_not_free_space) {
					CopyFileUtils.is_not_free_space = false;
					Toast.makeText(CopyService.this,
							getString(R.string.err_not_free_space),
							Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(CopyService.this,
							getString(R.string.copy_finish), Toast.LENGTH_SHORT)
							.show();
				}
				LOG("CopyService.java--handler--end");
			}
		}
	};

	private void registBroadcastRec() {
		IntentFilter f = new IntentFilter();
		// f.addAction(Intent.ACTION_MEDIA_EJECT);
		f.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
		f.addAction(Intent.ACTION_MEDIA_MOUNTED);
		f.addDataScheme("file");
		registerReceiver(mScanListener, f);
	}

	private BroadcastReceiver mScanListener = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			LOG("mScanListener BroadcastReceiver  -----=========-----------===========-----------========= action = "
					+ action
					+ "   intent.getData() = "
					+ intent.getData()
					+ "     getPath() = " + intent.getData().getPath());
			if (copystatus == COPY_STATUS_IDLE)
				return;
			if (action.equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
				LOG("in the mScanListener, ---- Intent.ACTION_MEDIA_UNMOUNTED");
				if (sourceString.startsWith(intent.getData().getPath())
						|| targetString.startsWith(intent.getData().getPath())) {
					LOG(" ________---------------------------- UNMOUNTED    finish() 1");
					CopyFileUtils.is_enable_copy = false;
					Toast.makeText(CopyService.this,
							getString(R.string.copy_finish), Toast.LENGTH_SHORT)
							.show();
				}
			} else if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
				LOG("in the mScanListener, ---- Intent.ACTION_MEDIA_MOUNTED");
				if (intent.getData().getPath().equals(RockExplorer.sdcard_dir)) {
					if (sourceString.startsWith(intent.getData().getPath())
							|| targetString.startsWith(intent.getData()
									.getPath())) {
						LOG(" ________---------------------------- MOUNTED    finish() 2");
						CopyFileUtils.is_enable_copy = false;
						Toast.makeText(CopyService.this,
								getString(R.string.copy_finish),
								Toast.LENGTH_SHORT).show();
					}
				}
			}
		}
	};

	private void reMedioScan(String rescanFilePath) {
		LOG("  ####  rescanFilePath = " + rescanFilePath);
		if (rescanFilePath == null)
			return;
		String scan_path = new String();

		/*
		 * if(rescanFilePath.startsWith(RockExplorer.flash_dir)){ scan_path =
		 * "flash"; }else
		 * if(rescanFilePath.startsWith(RockExplorer.sdcard_dir)){ scan_path =
		 * "sdcard"; }else if(rescanFilePath.startsWith(RockExplorer.usb_dir)){
		 * scan_path = "usb1"; }else{ return; }
		 */

		if (rescanFilePath.startsWith(RockExplorer.flash_dir)) {
			scan_path = RockExplorer.flash_dir;
		} else if (rescanFilePath.startsWith(RockExplorer.sdcard_dir)) {
			scan_path = RockExplorer.sdcard_dir;
		} else if (rescanFilePath.startsWith(RockExplorer.usb_dir)) {
			scan_path = RockExplorer.usb_dir;
		} else {
			return;
		}

		Intent intent = new Intent(Intent.ACTION_MEDIA_MOUNTED,
				Uri.parse("file://" + scan_path));
		intent.putExtra("read-only", false);
		sendBroadcast(intent);
	}

	private Notification createDownloadProgressNotification(String title) {
		final RemoteViews contentView = new RemoteViews(getPackageName(),
				R.layout.copy_notification);
		contentView.setTextViewText(R.id.copy_notification_title, title);
		contentView.setTextViewText(R.id.copy_notification_progress_text, "");
		contentView.setProgressBar(R.id.copy_notification_progress_bar, 100, 0,
				true);

		final PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(), 0);

		final Notification notification = new Notification();
		notification.icon = R.mipmap.icon;
		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		notification.contentView = contentView;
		notification.contentIntent = contentIntent;
		Intent statusintent = new Intent(this, RockExplorer.class);
		notification.contentIntent = PendingIntent.getActivity(this, 0,
				statusintent, 0);
		return notification;
	}

	static class ServiceStub extends ICopyService.Stub {
		WeakReference<CopyService> mService;

		ServiceStub(CopyService service) {
			mService = new WeakReference<CopyService>(service);
		}

		@Override
		public void testFun(String string) {
			mService.get().testFun(string);
			return;
		}

		@Override
		public void startCopy(String targetDir, boolean is_move) {
			mService.get().startCopy(targetDir, is_move);
			return;
		}

		@Override
		public void setCopyStatus(int status) {
			mService.get().setCopyStatus(status);
			return;
		}

		@Override
		public int getCopyStatus() {
			return mService.get().getCopyStatus();
		}

		@Override
		public void notificaFromActivity(boolean isrestart) {
			mService.get().notificaFromActivity(isrestart);
			return;
		}
	}
}
