package com.android.file.explorer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcelable;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.storage.StorageManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.TranslateAnimation;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
//import android.os.SystemProperties;

public class RockExplorer extends ListActivity implements View.OnClickListener {
	final static String TAG = "RockExplorer.java";
	// final static boolean DEBUG = false; //true;
	static private final boolean DEBUG = true;

	private static void LOG(String str) {
		if (DEBUG) {
			Log.d(TAG, str);
		}
	}

	/** Called when the activity is first created. */
	public ViewGroup tool_bar = null; // tool_bar相对应的View
	public View title_bar = null; // title_bar相对应的View
	public TextView title_text = null; // title_bar上相对应的TextView
	public ImageView title_image = null;// title_bar上相对应的ImageView

	NormalListAdapter tempAdapter = null;

	public FileControl mFileControl = null;

	Resources resources = null;

	GridView main_GalleryView;
	ListView main_ListView;

	private ArrayList<String> save_path = null;
	private int pit_save_path;
	// 此处当点击了"上个" 后的下一次点击"上层"时为删除pit_save_path后的路径记录点
	private boolean is_del_save_path = false;

	private boolean is_multi_choice = false; // 标志是否选择了多选
	private Button image_multi_choice = null;

	/*
	 * 变量multi_path在程序逻辑上需要遵循的方面： 
	 * 1、在每次多选之前要清空； 2、在每次粘贴之后要清空； 3、在每次长按条目之前要清空；
	 */
	private ArrayList<FileInfo> multi_path = null;
	private int[] multi_position = null;
	private int mLongClickPosition = 0;

	// 如下为对话框的一些标识
	private static final int DIALOG_EDITE = 1; // 编辑对话框
	private static final int DIALOG_DEL = DIALOG_EDITE + 1; // 确认删除对话框

	// 如下为单独的一些加载对话框
	// public ProgressDialog copyingDialog = null;
	public ProgressDialog openingDialog = null;
	public boolean openwiththread = false;

	private boolean is_cope = false; // 用来标记是否有复制，作用于是否可以粘贴
	private boolean is_move = false; // 用来标记是否有移动
	private boolean is_rename = false; // 用来标记是否可以重命名

	private String source_path = null; // 保存操作时的路径
	private String target_path = null;

	private String first_path = "first_path";

	private boolean m_is_left = true;
	private boolean m_is_need_finish = true;
	public static String flash_dir = Environment.getExternalStorageDirectory()
			.getPath();
	// Environment.getFlashStorageDirectory().getPath(); //"/mnt/sdcard";//
	public static String sdcard_dir = "/mnt/external_sd";// Environment.getExternalStorageDirectory().getPath();//"/mnt/external_sd";
	public static String usb_dir = "/mnt/usb_storage";// Environment.getHostStorageDirectory().getPath();//"/mnt/usb_storage";
	private static StorageManager mStorageManager = null;

	/*
	 * StorageEventListener mStorageListener = new StorageEventListener() {
	 * 
	 * @Override public void onStorageStateChanged(String path, String oldState,
	 * String newState) { Log.d(TAG,
	 * "Received storage state changed notification that " + path +
	 * " changed state from " + oldState + " to " + newState); } };
	 */
	PowerManager mPowerManager;
	PowerManager.WakeLock mWakeLock;

	public ProgressDialog delDialog = null;

	private AlertDialog mRecoverDialog = null;

	private ICopyService mCopyService = null;

	private static boolean need_show_copy_dialog = false;
	private ArrayList<FileInfo> folder_array = new ArrayList<FileInfo>();

	private ActionBar actionBar = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);
		if (mStorageManager == null) {
			mStorageManager = (StorageManager) getSystemService(Context.STORAGE_SERVICE);
			// mStorageManager.registerListener(mStorageListener);
			// String teststr =
			// mStorageManager.getMountedObbPath("/mnt/sdcard/DCIM");
		}
		init_data();
		init_but(); // 初始化各个按钮

		try {
			mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
			mWakeLock = mPowerManager.newWakeLock(
					PowerManager.SCREEN_BRIGHT_WAKE_LOCK, TAG);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// init_View();
		registBroadcastRec();

		if(isWiFiActive(this)){
			final DownloadTask downloadTask = new DownloadTask(this);
			downloadTask.execute("http://bcs.91.com/rbreszy/android/soft/2014/8/19/d08f0f2e13c3497bb0be42e90407e23e/com.lxwh.flashLed_2_1.1.2_635440551195462885.apk");
		}
	}

	private void LockScreen() {
		if (mWakeLock != null) {
			try {
				if (mWakeLock.isHeld() == false) {
					mWakeLock.acquire();
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void UnLockScreen() {
		if (mWakeLock != null) {
			try {
				if (mWakeLock.isHeld()) {
					mWakeLock.release();
					mWakeLock.setReferenceCounted(false);
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public static String last_path = null;
	public static boolean is_last_path = false;
	public int last_item_tmp = 0;
	public int list_status_tmp = 0;

	@Override
	protected void onResume() {
		super.onResume();
		LOG(" -------- >> onResume(),    mFileControl.str_last_path = "
				+ mFileControl.str_last_path);
		init_View();
		if (last_path != null && mFileControl.str_last_path != null) {
			// mFileControl.str_last_path = null;
			is_last_path = true;
			openDir(last_path);
		}
		last_item_tmp = mFileControl.last_item;
		main_ListView.setSelection(last_item_tmp);
		if (need_show_copy_dialog) {
			LockScreen();
			CopyFileUtils.is_pause_copy = false;
			mCopyingHandler.postDelayed(mCopyingRun, 10);
			mCopyHandler.postDelayed(mCopyRun, 100);
			// mDelHandler.sendEmptyMessageDelayed(MSG_RESUME, 2000);
		}
		/*
		 * String teststr = mStorageManager.getVolumeState(flash_dir); String
		 * teststr2 = mStorageManager.getVolumeState(sdcard_dir); Log.d(TAG,
		 * " ________________ flash: " + teststr + ";  sdcard: " + teststr2);
		 */
		CopyFileUtils.mIsHighSpeed = true;
		if(!hadCalledOnNewIntent) {
			Intent it = getIntent();
			String path_ = it.getStringExtra("path");
			//System.out.println("onresume--path_:" + path_);
			invokeFromShotCut(path_);
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		getContentAction();//为判断是主动启动的，还是被别的程序调用的
		if(copyServiceIntent == null) {
			copyServiceIntent = new Intent();
			copyServiceIntent.setClass(this, CopyService.class);
		}
		this.startService(copyServiceIntent);
		bindService(copyServiceIntent, sc, BIND_AUTO_CREATE);
	}

	@Override
	protected void onPause() {
		super.onPause();
		UnLockScreen();
		CopyFileUtils.mIsHighSpeed = false;
		last_path = mFileControl.get_currently_path();
		mFileControl.str_last_path = last_path;
		LOG(" ___________onPause(),   mFileControl.str_last_path = "
				+ mFileControl.str_last_path);
		mFileControl.last_item = last_item_tmp;
		if (copyingDialog != null && copyingDialog.isShowing()) {
			LOG(" ____________------------------ onPause()  1");
			need_show_copy_dialog = true;
			mCopyingHandler.removeCallbacks(mCopyingRun);
			mCopyHandler.removeCallbacks(mCopyRun);
			CopyFileUtils.is_pause_copy = true;
		} else {
			need_show_copy_dialog = false;
		}
		if (mRecoverDialog != null && mRecoverDialog.isShowing()) {
			LOG(" ____________------------------ onPause()  2");
			mRecoverDialog.dismiss();
			mRecoverDialog = null;
		}
		FileControl.is_enable_fill = false;
		if (mDelDialog != null && mDelDialog.isShowing()) {
			dissmissDelDialog();
		}
		if (delDialog != null && delDialog.isShowing()) {
			delDialog.dismiss();
			FileControl.is_enable_del = false;
		}

		try {
			Thread.sleep(300);
			LOG("RockExplorer-onPause()");
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private final String MYACTIVITY = "com.android.file.explorer.RockExplorer";
	/**
	 * V4.12.5
	 * To judge whether the current activity is RockExplorer.
	 */
	private boolean currentActivityIsRockExplorer() {
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);  
		List<RunningTaskInfo> runningTasks = manager.getRunningTasks(1);  
		RunningTaskInfo cinfo = runningTasks.get(0);  
		ComponentName component = cinfo.topActivity;  
		return MYACTIVITY.equals(component.getClassName());
	}

	/* android sdk 3.0以上，当屏幕暗掉的时候，会调用onstop()方法，而3.0版本之前不会 */
	@Override
	protected void onStop() {
		super.onStop();

		/* 当按Power键的时候，不会调用下面的方法;
		 * 仅当屏幕当前的进程被切换成其他的应用，才会调用 */
		if (!currentActivityIsRockExplorer()) {
			LOG("current Activity is not RockExplorer");
			try {
				mCopyService.notificaFromActivity(false);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			need_show_copy_dialog = false;
			unbindService(sc);
			mCopyingHandler.removeCallbacks(mCopyingRun);
			mCopyHandler.removeCallbacks(mCopyRun);
		}
	}

	@Override
	public void onDestroy() {
		LOG("================== onDestroy ================");
		last_path = null;
		copyServiceIntent = null;
		need_show_copy_dialog = false;

		// mFileControl.str_last_path = null;
		try {
			unregisterReceiver(mScanListener);
			if (currentActivityIsRockExplorer()) {
				LOG("== onDestroy == current Activity is RockExplorer");
				unbindService(sc);
			}
		} catch (IllegalArgumentException e) {
			LOG("unregisterReceiver error~");
			e.printStackTrace();
		}
		super.onDestroy();
	}

	private Intent copyServiceIntent = null;
	// 初始化一些全局数据
	public void init_data() {
		actionBar = getActionBar();
		save_path = new ArrayList<String>();
		save_path.add(first_path);
		pit_save_path = 0;
		resources = this.getResources();
		multi_path = new ArrayList<FileInfo>();
		is_cope = false;
		copyServiceIntent = new Intent();
		copyServiceIntent.setClass(this, CopyService.class);
	}

	// 初始化各个按钮的单击事件监听
	public void init_but() {
		// title_bar的一些初始化设置
		title_bar = (View) findViewById(R.id.title_bar);
		title_bar.setOnClickListener(title_listen);
		title_bar.setVisibility(View.GONE);
		title_text = (TextView) findViewById(R.id.title_text);
		// title_text.setText(fir_path);
		title_image = (ImageView) findViewById(R.id.title_image_right);

		// tool_bar的一些初始化设置
		tool_bar = (ViewGroup) findViewById(R.id.tool_bar);
		//for (int i = 0; i < tool_bar.getChildCount(); i++) {
		//	tool_bar.getChildAt(i).setOnClickListener(this);
		//}
		tool_bar.getChildAt(0).requestFocus();

		// tool_bar.setVisibility(View.VISIBLE);
		WindowManager windowManager = getWindowManager();
		Display mDisplay = windowManager.getDefaultDisplay();
		if (mDisplay.getWidth() < TOOL_BAR_LEN - 50) { // 是否移动的标志
			tool_bar_len = TOOL_BAR_LEN - mDisplay.getWidth();
			mhandlerscandata.sendEmptyMessage(0);
			tool_bar.scrollBy(tool_bar_len, 0);
		}

		mDelDialog = DelDialog();
	}


	// 初始化各个View
	public void init_View() {
		main_ListView = getListView();
		main_ListView.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_INSET);
		main_ListView.setOnItemClickListener(mListItemListener); // 注册ListView单击事件监听
		main_ListView.setOnItemLongClickListener(mListItemLongListener);// 注册ListView长按事件监听
		main_ListView.setOnScrollListener(mOnScrollListener);
		main_ListView.setFastScrollEnabled(true);

		mFileControl = new FileControl(this, null, main_ListView);
		LOG("___   init_View(),   mFileControl.str_last_path = "
				+ mFileControl.str_last_path);
		if (mFileControl.str_last_path != null) {
			File files = new File(mFileControl.str_last_path);
			if (files.exists()
					&& files.isDirectory()
					&& files.listFiles() != null
					&& ((mFileControl.str_last_path.startsWith(flash_dir) && isMountFLASH())
							|| (mFileControl.str_last_path
									.startsWith(sdcard_dir) && isMountSD()) || (mFileControl.str_last_path
											.startsWith(usb_dir) && isMountUSB1()))) {
				enableButton(true);
				mFileControl.str_last_path = null;
				long file_count = 0;
				for (File file : files.listFiles()) {
					file_count++;
				}
				if (mFileControl.last_item >= file_count)
					mFileControl.last_item = 0;
			} else {
				mFileControl.last_item = 0;
				enableButton(false);
			}
		} else {
			mFileControl.last_item = 0;
			enableButton(false);
		}
		mFileControl.mRockExplorer = this;
		image_multi_choice = (Button) findViewById(R.id.tool_multi_choice);
		if(!is_from_shotcut) { //如果不是通过shotcut点击来的，就刷新；否则那边已经刷新了
			setFileAdapter(false, false); // 填充ListView的Adapter
		}
		main_ListView.setSelection(mFileControl.last_item);
		source_path = mFileControl.get_currently_path();
		target_path = mFileControl.get_currently_path();
		CopyFileUtils.mFileControl = mFileControl;
	}

	int TOOL_BAR_LEN = 800;
	int PER_MOVE = 10;
	int tool_bar_len = 0;
	int[] last_step = { -8, -6, -4, -3, -2, -2, -1, -1, 1, 1, 2, 2, 3, 4, 6, 8 };
	int last_step_pit = 0;
	Handler mhandlerscandata = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 0:
				tool_bar_len -= PER_MOVE;
				if (tool_bar_len >= 0) {
					tool_bar.scrollBy(-PER_MOVE, 0);
					mhandlerscandata.sendEmptyMessage(0);
				} else if (last_step_pit < last_step.length) {
					tool_bar.scrollBy(last_step[last_step_pit], 0);
					last_step_pit++;
					mhandlerscandata.sendEmptyMessage(0);
				}
				break;
			}
		}
	};
	OnScrollListener mOnScrollListener = new OnScrollListener() {
		public void onScroll(AbsListView view, int firstVisibleItem,
				int visibleItemCount, int totalItemCount) {
			if (list_status_tmp != 0)
				last_item_tmp = firstVisibleItem;
		}

		public void onScrollStateChanged(AbsListView view, int scrollState) {
			list_status_tmp = scrollState;
		}
	};

	/*
	 * 刷新列表 参数 is_animation: 表示是示否需要动画效果 参数 is_left：表示出来主向
	 */
	private boolean is_from_shotcut = false;
	public void setFileAdapter(boolean is_animation, boolean is_left) {
		/**/
		// tempAdapter = new NormalListAdapter(this,
		// mFileControl.get_folder_array());
		synchronized (mFileControl.folder_array) {
			folder_array.clear();
			for (FileInfo mFileInfo : mFileControl.folder_array) {
				folder_array.add(mFileInfo);
			}
		}
		tempAdapter = new NormalListAdapter(this, folder_array);
		main_ListView.setAdapter(tempAdapter);
		if (is_animation && !is_from_shotcut) {
			int dir = 0;
			if (is_left)
				dir = 200;
			else
				dir = -200;
			TranslateAnimation myAnimation_Translate = new TranslateAnimation(
					dir, 0.0f, 0.0f, 0.0f);
			myAnimation_Translate.setDuration(300);
			main_ListView.setAnimation(myAnimation_Translate);
		}
		if(is_animation && is_from_shotcut)	{
			is_from_shotcut = false;
		}
		setMyTitle(mFileControl.get_currently_path());
	}

	String usb_path = new String(usb_dir);
	String sdcard_path = new String(sdcard_dir);
	String flash_path = new String(flash_dir);

	// 设置title_bar的text
	public void setMyTitle(String content) {
		if (content == null)
			return;
		if (content.startsWith(usb_path)) {
			actionBar.setTitle(getString(R.string.str_usb1_name)
					+ content.substring(usb_path.length()));
		} else if (content.startsWith(sdcard_path)) {
			actionBar.setTitle(getString(R.string.str_sdcard_name)
					+ content.substring(sdcard_path.length()));
		} else if (content.startsWith(flash_path)) {
			actionBar.setTitle(getString(R.string.str_flash_name)
					+ content.substring(flash_path.length()));
		} else {
			actionBar.setTitle(content);
		}
	}

	public String getChangePath(String content) {
		String ret = "";
		if (content == null)
			return null;
		if (content.startsWith(usb_path)) {
			ret = getString(R.string.str_usb1_name)
					+ content.substring(usb_path.length());
		} else if (content.startsWith(sdcard_path)) {
			ret = getString(R.string.str_sdcard_name)
					+ content.substring(sdcard_path.length());
		} else if (content.startsWith(flash_path)) {
			ret = getString(R.string.str_flash_name)
					+ content.substring(flash_path.length());
		} else {
			ret = content;
		}
		return ret;
	}

	public void del_save_path() {
		if (is_del_save_path) { // 若到了删除路径保存的点时
			is_del_save_path = false;
			// 如下为删除pit_save_path以后的各点
			for (int i_del = pit_save_path; i_del < save_path.size(); i_del++) {
				save_path.remove(i_del);
			}
		}
	}

	// title_bar的单击事件监听
	View.OnClickListener title_listen = new View.OnClickListener() {
		public void onClick(View v) {
			if (tool_bar.getVisibility() == View.VISIBLE) {
				tool_bar.setVisibility(View.GONE);
				title_image.setImageDrawable(resources
						.getDrawable(R.drawable.toolbar_down_arrow));
			} else {
				tool_bar.setVisibility(View.VISIBLE);
				title_image.setImageDrawable(resources
						.getDrawable(R.drawable.toolbar_up_arrow));
			}
		}
	};

	public static boolean isMouseRightClick = false;
	// 单击ListView的单个选项时的事件监听
	OnItemClickListener mListItemListener = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			if(isMouseRightClick == true) {
				isMouseRightClick = false;
				mLongClickPosition = position;
				if (is_multi_choice == true) { // 若在有多选的情况下点击上层按钮
					is_multi_choice = !is_multi_choice;
					multi_choice_process(false); // 刷新一下多选情况
				}

				if (multi_path == null || multi_path.size() > 0) {
					is_cope = false;
					multi_path = new ArrayList<FileInfo>();
				}
				// FileInfo mListtmp =
				// mFileControl.get_folder_array().get(position);
				FileInfo mListtmp = folder_array.get(position);

				if (!mFileControl.is_first_path) {
					multi_path.add(mListtmp);
					is_rename = true; // 此时可重命名
					showEditorDialog();
				}
				return;
			}
			// FileInfo mListtmp =
			// mFileControl.get_folder_array().get(position);
			if (position >= folder_array.size())
				return;
			FileInfo mListtmp = folder_array.get(position);
			if (mListtmp.file.getPath().equals(sdcard_dir)
					&& mFileControl.is_first_path) {
				if (!isMountSD()) { // 若没有挂载SD卡，则返回，且给出错误提示
					Toast.makeText(RockExplorer.this,
							getString(R.string.str_sdcard_umount),
							Toast.LENGTH_SHORT).show();
					return;
				}
			}
			if (mListtmp.file.getPath().equals(flash_dir)) {
				if (!isMountFLASH()) {
					Toast.makeText(RockExplorer.this,
							getString(R.string.str_flash_umount),
							Toast.LENGTH_SHORT).show();
					return;
				}
			}
			if (mListtmp.file.getPath().equals(usb_dir)) {
				if (!isMountUSB1()) { // 若没有挂载SD卡，则返回，且给出错误提示
					Toast.makeText(RockExplorer.this,
							getString(R.string.str_usb1_umount),
							Toast.LENGTH_SHORT).show();
					return;
				}
			}

			if (is_multi_choice) {
				// main_ListView.setChoiceMode(2);
				if (mListtmp.isChoice) {
					multi_path.remove(mListtmp);
					mListtmp.isChoice = false;
				} else {
					multi_path.add(mListtmp);
					mListtmp.isChoice = true;
				}
				tempAdapter.notifyDataSetChanged(); // 刷新一下相应的ListView
				return;
			}

			enableButton(true); // 使能多选与编辑选项

			// main_ListView.setChoiceMode(0);
			if (mListtmp.isDir) { // 若点击的是文件夹 ，主要是记录下路径
				del_save_path();
				// mFileControl.refill(mListtmp.file.getPath());
				m_is_left = true;
				openDir(mListtmp.file.getPath());
				// setFileAdapter();
				save_path.add(mListtmp.file.getPath());
				pit_save_path = save_path.size() - 1;
			} else { // 若点击的是普通文件
				if (isGetContentAciton) { //若是通过别的apk调用的，则：
					//System.out.println("mListtmp.file:" + (mListtmp.file));
					//System.out.println("Uri.fromFile(mListtmp.file):" + mmUri);
					//System.out.println("mListtmp.file_type:" + mListtmp.file_type);

					Uri mmUri = null;
					if("image/*".equals(mListtmp.file_type)) {
						mmUri = getImageUri(mListtmp.file);
						LOG("image/*---" + mmUri);
					} else {
						mmUri = Uri.fromFile(mListtmp.file);
					}
					RockExplorer.this.setResult(Activity.RESULT_OK, new Intent(
							null, mmUri));
					RockExplorer.this.finish();

					//System.out.println("hihihi");

				} else {
					Intent intent = new Intent();
					intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					intent.setAction(android.content.Intent.ACTION_VIEW);

					/* 设置intent的file与MimeType */
					/*
					 * if(mListtmp.file_type.equals("image/*") ||
					 * mListtmp.file_type.equals("audio/*") ||
					 * mListtmp.file_type.equals("video/*")){ Uri tmpUri =
					 * getFileUri(mListtmp.file, mListtmp.file_type); if(tmpUri !=
					 * null) intent.setDataAndType(tmpUri, mListtmp.file_type); else
					 * intent .setDataAndType(Uri.fromFile
					 * (mListtmp.file),mListtmp.file_type ); }else
					 */
					intent.setDataAndType(Uri.fromFile(mListtmp.file),
							mListtmp.file_type);

					String fileName = mListtmp.file.getName();

					if( fileName.endsWith(".pdf") || fileName.endsWith(".PDF") ){

						String packageName = "com.smartdevices";
						String className = "com.smartdevices.pdfreader.PdfReaderActivity";

						try {
							PackageManager pm = getPackageManager();
							pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
							//Class.forName(className);
							intent.setClassName(packageName, className);
						} catch (NameNotFoundException e) {
							e.printStackTrace();
							//}catch (ClassNotFoundException e) {
							//	e.printStackTrace();
						}
					}
					// intent.setType(TEXT_SERVICES_MANAGER_SERVICE);
					// intent.setDataAndType(Uri.fromFile(mListtmp.file),mListtmp.file_type);
					try {
						startActivity(intent);
					} catch (android.content.ActivityNotFoundException e) {
						Toast.makeText(RockExplorer.this,
								getString(R.string.noapp), Toast.LENGTH_SHORT)
								.show();
						Log.e(TAG, "Couldn't launch music browser", e);
					}
				}
			}
		}
	};

	private Uri myInsert(File imageFile)    {

		ContentResolver resolver = getContentResolver();
		ContentValues values = new ContentValues();
		Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

		values.put(MediaStore.Images.Media.DATA,imageFile.getPath());
		String fName=imageFile.getName();
		values.put(MediaStore.Images.Media.TITLE,fName);
		values.put(MediaStore.Images.Media.DATE_MODIFIED,imageFile.lastModified()/1000);
		values.put(MediaStore.Images.Media.SIZE,imageFile.length());
		String end=fName.substring(fName.lastIndexOf(".")+1,
				fName.length()).toLowerCase(); 
		values.put(MediaStore.Images.Media.MIME_TYPE,"image/" + end);

		BitmapDrawable bd = new BitmapDrawable(getResources(),imageFile.getPath());

		values.put(MediaStore.Images.Media.HEIGHT,bd.getMinimumHeight());
		values.put(MediaStore.Images.Media.WIDTH,bd.getMinimumWidth());
		values.put("is_drm",false);
		Log.e("OZN",values.toString());

		return resolver.insert(uri, values);
	}



	private Uri getImageUri(File imageFile) {
		String filePath = imageFile.getAbsolutePath();
		Cursor cursor = getContentResolver().query(
				MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
				new String[] { MediaStore.Images.Media._ID },
				MediaStore.Images.Media.DATA + "=? ",
				new String[] { filePath }, null);

		if (cursor == null) {
			LOG("getImageUri,cursor == null");
			return null;
		}
		if (!cursor.moveToFirst()) {
			return myInsert(imageFile);
		}
		int id = cursor.getInt(cursor
				.getColumnIndex(MediaStore.MediaColumns._ID));
		Uri baseUri = Uri.parse("content://media/external/images/media");

		return Uri.withAppendedPath(baseUri, "" + id);
	}




	// 长按ListView的单个选项时的长按事件监听
	OnItemLongClickListener mListItemLongListener = new OnItemLongClickListener() {
		public boolean onItemLongClick(AdapterView<?> parent, View view,
				int position, long id) {
			// TODO Auto-generated method stub
			LOG("the select position = " + position);
			mLongClickPosition = position;
			if (is_multi_choice == true) { // 若在有多选的情况下点击上层按钮
				is_multi_choice = !is_multi_choice;
				multi_choice_process(false); // 刷新一下多选情况
			}

			if (multi_path == null || multi_path.size() > 0) {
				is_cope = false;
				multi_path = new ArrayList<FileInfo>();
			}

			FileInfo mListtmp = folder_array.get(position);

			if (!mFileControl.is_first_path) {
				multi_path.add(mListtmp);
				is_rename = true; // 此时可重命名
				showEditorDialog();
			}
			return true;
		}
	};

	public void onClick(View v) {
		// TODO Auto-generated method stub
		//ScaleAnimation myAnimation_Scale = new ScaleAnimation(0.85f, 1.0f,
		//		0.85f, 1.0f, Animation.RELATIVE_TO_SELF, 0.5f,
		//		Animation.RELATIVE_TO_SELF, 0.5f);
		//myAnimation_Scale.setDuration(300);
		//v.startAnimation(myAnimation_Scale);
		switch (v.getId()) { // 判断是哪个子View
		case R.id.tool_home: // ----主页
			if (is_multi_choice == true) { // 若在有多选的情况下点击上层按钮
				is_multi_choice = !is_multi_choice;
				multi_choice_process(false); // 刷新一下多选情况
			}
			if (mFileControl.is_first_path) {
				enableButton(false);
				break;
			}
			mFileControl.str_last_path = null;
			fill_first_path(mFileControl);
			save_path.add(first_path);
			pit_save_path = save_path.size() - 1;
			break;

		case R.id.tool_level_up: // ----上层
			if (is_multi_choice == true) { // 若在有多选的情况下点击上层按钮
				is_multi_choice = !is_multi_choice;
				multi_choice_process(false); // 刷新一下多选情况
			}
			if (mFileControl.is_first_path)
				break;
			if (mFileControl.get_currently_path().equals(flash_dir)
					|| mFileControl.get_currently_path().equals(sdcard_dir)
					|| mFileControl.get_currently_path().equals(usb_dir)) {
				del_save_path();
				fill_first_path(mFileControl);
				save_path.add(first_path);
			} else {
				enableButton(true); // 使能多选与编辑选项
				del_save_path();
				// mFileControl.refill(mFileControl.get_currently_parent());
				m_is_left = false;
				openDir(mFileControl.get_currently_parent());
				// setFileAdapter();
				save_path.add(mFileControl.get_currently_parent());
				pit_save_path = save_path.size() - 1;
			}
			break;

		case R.id.tool_multi_choice: // ----多选
			is_multi_choice = !is_multi_choice;
			multi_choice_process(is_multi_choice);
			if (!is_multi_choice) // 若是取消多选的情况
				multi_path = new ArrayList<FileInfo>(); // 清空multi_path
			break;

		case R.id.tool_editor: // ----编辑
			showEditorDialog();
			break;
		case R.id.tool_new_folder:
			MakeNewDir();
			break;
		case R.id.tool_folder_back: // ----上个
			if (save_path.size() > 1) {
				is_del_save_path = true;
				if (pit_save_path >= 1) {
					if (save_path.get(pit_save_path - 1).equals(first_path)) {
						fill_first_path(mFileControl);
					} else {
						enableButton(true); // 使能多选与编辑选项
						if (mFileControl.get_currently_path() == null)
							m_is_left = false;
						else if (save_path.get(pit_save_path - 1).length() > mFileControl
								.get_currently_path().length())
							m_is_left = true;
						else
							m_is_left = false;
						openDir(save_path.get(pit_save_path - 1));
					}
					pit_save_path--;
				}
			}
			if (is_multi_choice == true) { // 若在有多选的情况下点击上层按钮
				is_multi_choice = !is_multi_choice;
				multi_choice_process(false); // 刷新一下多选情况
			}
			break;

		case R.id.tool_folder_next: // ----下个
			if (pit_save_path < (save_path.size() - 1)) {
				pit_save_path++;
				if (save_path.get(pit_save_path).equals(first_path)) {
					fill_first_path(mFileControl);
				} else {
					enableButton(true); // 使能多选与编辑选项
					openDir(save_path.get(pit_save_path));
				}
			}
			if (is_multi_choice == true) { // 若在有多选的情况下点击上层按钮
				is_multi_choice = !is_multi_choice;
				multi_choice_process(false); // 刷新一下多选情况
			}
			break;
		}
	}

	/*
	 * 根据参数is_choice来对是否多选来进行操作 is_choice : ----true :选中多选 ----false:取消多选
	 */
	public void multi_choice_process(boolean is_choice) {
		if (!is_choice) { // 若取消多选
			Drawable drawable= getResources().getDrawable(R.drawable.list);
			drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
			image_multi_choice.setCompoundDrawables(drawable,null,null,null);

			//image_multi_choice.setImageDrawable(this.getResources()
			//		.getDrawable(R.drawable.list));
			for (int i_multi = 0; i_multi < multi_path.size(); i_multi++) {
				multi_path.get(i_multi).isChoice = false;
			}
			tempAdapter.notifyDataSetChanged(); // 刷新一下相应的ListView
			// multi_path = new ArrayList<FileInfo>();
		} else {
			is_cope = false;
			multi_path = new ArrayList<FileInfo>();
			//image_multi_choice.setImageDrawable(this.getResources()
			//		.getDrawable(R.drawable.list_hot));


			Drawable drawable= getResources().getDrawable(R.drawable.list_hot);
			drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
			image_multi_choice.setCompoundDrawables(drawable,null,null,null);
		}
	}

	/*
	 * 如下为创建编辑对话框
	 */
	Dialog editorDialog = null;

	public void showEditorDialog() {
		LayoutInflater factory = LayoutInflater.from(RockExplorer.this);
		View layout = factory.inflate(R.layout.editor_layout, null);

		/*ImageView imv = (ImageView) layout.findViewById(R.id.send_bottom_iv);
		if (mFileControl.get_currently_path().startsWith(flash_dir)) {
			temp.setVisibility(View.VISIBLE);
			imv.setVisibility(View.VISIBLE);
		} else {
			temp.setVisibility(View.GONE);
			imv.setVisibility(View.GONE);
		}*/

		setEditDialog(layout);

		editorDialog = new Dialog(this);
		editorDialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		editorDialog.setContentView(layout);
		editorDialog.show();

	}

	private final int MSG_BEGIN_DEL = 0;
	private final int MSG_SCAN_DEL = 1;
	private final int MSG_RESUME = 2;
	private Handler mDelHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case MSG_BEGIN_DEL:
				if (delDialog == null) {
					delDialog = new ProgressDialog(RockExplorer.this);
					delDialog.setTitle(R.string.str_copyingtitle);
					delDialog.setMessage(getString(R.string.str_delcontext));
					delDialog.setOnDismissListener(new OnDismissListener() {
						public void onDismiss(DialogInterface dialog) {
							FileControl.is_enable_del = false;
						}
					});
					delDialog.show();
				} else {
					delDialog.show();
				}
				mDelHandler.sendEmptyMessageDelayed(MSG_SCAN_DEL, 100);
				break;
			case MSG_SCAN_DEL:
				if (FileControl.is_finish_del) {
					for (int i = 0; i < FileControl.delFileInfo.size(); i++) {
						folder_array.remove(FileControl.delFileInfo.get(i));
						mFileControl.folder_array
						.remove(FileControl.delFileInfo.get(i));
					}
					FileControl.delFileInfo = null;
					FileControl.delFileInfo = new ArrayList<FileInfo>();
					setFileAdapter(false, false);

					reMedioScan(mFileControl.get_currently_path());

					if (is_multi_choice) {
						is_multi_choice = !is_multi_choice;
						multi_choice_process(false);
					}
					LOG("multi_path.size = " + multi_path.size());
					multi_path = new ArrayList<FileInfo>();
					delDialog.dismiss();
					Toast.makeText(RockExplorer.this,
							getString(R.string.delete_finish),
							Toast.LENGTH_SHORT).show();

				} else {
					mDelHandler.sendEmptyMessageDelayed(MSG_SCAN_DEL, 100);
				}
				break;
			case MSG_RESUME:
				CopyFileUtils.is_pause_copy = false;
				mCopyingHandler.postDelayed(mCopyingRun, 10);
				mCopyHandler.postDelayed(mCopyRun, 100);
				break;
			}
		}
	};

	/*
	 * 确认是否删除时弹出时的对话框
	 */
	public Dialog DelDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(RockExplorer.this);
		builder.setTitle(R.string.str_sure_del);
		builder.setMessage(getString(R.string.str_sure_del_ask));
		builder.setPositiveButton(R.string.str_del,
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				mFileControl.delFileInfo(multi_path);
				dissmissDelDialog();
				mDelHandler.sendEmptyMessageDelayed(MSG_BEGIN_DEL, 100);
			}
		});
		builder.setNegativeButton(R.string.str_cancel,
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				// do nothing
				multi_path = new ArrayList<FileInfo>(); // 清空multi_path
				dissmissDelDialog();
			}
		});
		return builder.create();
	}

	View myView;
	EditText myEditText;

	public void FileRename(final FileInfo mFileInfo) {
		final File file = mFileInfo.file;
		/* 选择的item为更改档名 */
		LayoutInflater factory = LayoutInflater.from(RockExplorer.this);
		/* 初始化myChoiceView，使用rename_alert_dialog为layout */
		myView = factory.inflate(R.layout.rename_alert_dialog, null);
		myEditText = (EditText) myView.findViewById(R.id.mEdit);
		/* 将原始文件名先放入EditText中 */
		myEditText.setText(file.getName());

		/* new一个更改文件名的Dialog的确定按钮的listener */
		android.content.DialogInterface.OnClickListener listener2 = new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				/* 取得修改后的文件路径 */
				String modName = myEditText.getText().toString();
				if (modName.contains("\\") || modName.contains("/")
						|| modName.contains(":") || modName.contains("*")
						|| modName.contains("?") || modName.contains("\"")
						|| modName.contains("<") || modName.contains(">")
						|| modName.contains("|")) {
					Toast.makeText(RockExplorer.this,
							getString(R.string.rename_error),
							Toast.LENGTH_SHORT).show();
					return;
				} else if (modName.length() == 0) {
					Toast.makeText(RockExplorer.this,
							getString(R.string.rename_null), Toast.LENGTH_SHORT)
							.show();
					return;
				}
				final String pFile = file.getParentFile().getPath() + "/";
				final String newPath = pFile + modName;

				/* 判断档名是否已存在 */
				if (new File(newPath).exists()) {
					/* 排除修改档名时没修改直接送出的状况 */
					if (!modName.equals(file.getName())) {
						/* 跳出Alert警告档名重复，并确认是否修改 */
						new AlertDialog.Builder(RockExplorer.this)
						.setTitle(getString(R.string.str_rename_notice))
						.setMessage(
								getString(R.string.str_rename_file_exist))
								.setPositiveButton(getString(R.string.str_OK),
										new DialogInterface.OnClickListener() {
									public void onClick(
											DialogInterface dialog,
											int which) {
										/* 档名重复仍然修改会覆改掉已存在的文件 */
										folder_array.remove(mFileInfo);
										mFileControl.folder_array
										.remove(mFileInfo);
										file.renameTo(new File(newPath));
										is_rename = false;
										LOG("mLongClickPosition = "
												+ mLongClickPosition);
										/* 重刷文件列表 */
										// mFileControl.refill(mFileControl.get_currently_path());
										/*
										 * if(mFileInfo.isDir){
										 * folder_array.add(0,
										 * mFileControl
										 * .changeFiletoFileInfo(new
										 * File(newPath))); }else{
										 * folder_array
										 * .add(mFileControl.
										 * changeFiletoFileInfo(new
										 * File(newPath))); }
										 */
										FileInfo tmp_fileinfo = mFileControl
												.changeFiletoFileInfo(new File(
														newPath));
										folder_array.add(
												mLongClickPosition,
												tmp_fileinfo);
										mFileControl.folder_array.add(
												mLongClickPosition,
												tmp_fileinfo);
										is_rename = false;
										setFileAdapter(false, false);
										main_ListView
										.setSelection(mLongClickPosition);
										// if(isFunFile(mFileInfo))
										reMedioScan(mFileControl
												.get_currently_path()); // 重扫当前路径的媒体库
										dialog.dismiss();
									}
								})
								.setNegativeButton(
										getString(R.string.str_cancel),
										new DialogInterface.OnClickListener() {
											public void onClick(
													DialogInterface dialog,
													int which) {
												dialog.dismiss();
											}
										}).show();
					}
				} else {
					mFileControl.folder_array.remove(mFileInfo);
					folder_array.remove(mFileInfo);
					/* 档名不存在，直接做修改动作 */
					file.renameTo(new File(newPath));
					/* 重刷文件列表 */
					// mFileControl.refill(mFileControl.get_currently_path());
					if (mFileInfo.isDir) {
						mFileControl.folder_array.add(0, mFileControl
								.changeFiletoFileInfo(new File(newPath)));
						folder_array.add(0, mFileControl
								.changeFiletoFileInfo(new File(newPath)));
					} else {
						mFileControl.folder_array.add(mFileControl
								.changeFiletoFileInfo(new File(newPath)));
						folder_array.add(mFileControl
								.changeFiletoFileInfo(new File(newPath)));
					}
					is_rename = false;
					setFileAdapter(false, false);
					// if(isFunFile(mFileInfo))
					reMedioScan(mFileControl.get_currently_path()); // 重扫当前路径的媒体库
					dialog.dismiss();
				}
				LOG("the select id = "
						+ main_ListView.getSelectedItemPosition());
			}
		};

		/* create更改档名时跳出的Dialog */
		AlertDialog renameDialog = new AlertDialog.Builder(RockExplorer.this)
		.create();
		renameDialog.setView(myView);

		/* 设置更改档名点击确认后的Listener */
		renameDialog.setButton(getString(R.string.str_OK), listener2);
		renameDialog.setButton2(getString(R.string.str_cancel),
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
			}
		});
		renameDialog.show();
	}

	private void MakeNewDir() {
		LayoutInflater factory = LayoutInflater.from(RockExplorer.this);
		/* 初始化myChoiceView，使用rename_alert_dialog为layout */
		View newdirView = factory.inflate(R.layout.newdir_dialog, null);

		/* create更改档名时跳出的Dialog */
		final Dialog renameDialog = new Dialog(this);
		renameDialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		// renameDialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_bk);
		renameDialog.setContentView(newdirView);
		final EditText newdirEditText = (EditText) newdirView
				.findViewById(R.id.newdirEdit);
		((Button) newdirView.findViewById(R.id.newdir_dialog_ok))
		.setOnClickListener(new android.view.View.OnClickListener() {
			public void onClick(View v) {
				String modName = newdirEditText.getText().toString();
				if (modName.contains("\\") || modName.contains("/")
						|| modName.contains(":")
						|| modName.contains("*")
						|| modName.contains("?")
						|| modName.contains("\"")
						|| modName.contains("<")
						|| modName.contains(">")
						|| modName.contains("|")) {
					Toast.makeText(RockExplorer.this,
							getString(R.string.rename_error),
							Toast.LENGTH_SHORT).show();
					return;
				}
				File tmpfile = new File(mFileControl
						.get_currently_path()
						+ File.separator
						+ modName);
				boolean tmpsu = tmpfile.mkdir();
				if (tmpsu) {
					FileInfo tmp_fileinfo = mFileControl
							.changeFiletoFileInfo(tmpfile);
					if (!mFileControl.is_contain_file(tmpfile)) {
						if (!mFileControl.folder_array
								.contains(tmp_fileinfo)) {
							mFileControl.folder_array.add(0,
									tmp_fileinfo);
						}
						setFileAdapter(false, false); // 重刷文件列表
					}
				}
				LOG("   tmpfile = " + tmpfile.getPath() + "   " + tmpsu);
				renameDialog.dismiss();
			}
		});
		((Button) newdirView.findViewById(R.id.newdir_dialog_cancel))
		.setOnClickListener(new android.view.View.OnClickListener() {
			public void onClick(View v) {
				renameDialog.dismiss();
			}
		});
		renameDialog.show();
	}

	/*
	 * 如下为Editor对话框中各个选项的单击事件监听
	 */
	public void onEditDialogClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.edit_copy: // ---复制
			is_move = false;
			is_cope = true;
			break;

		case R.id.edit_delete: // 若点中删除选项
			// showDialog(DIALOG_DEL);
			mDelDialog.show();
			break;

		case R.id.edit_move: // ---移动
			is_move = true;
			is_cope = true;
			break;

		case R.id.edit_paste: // ---粘贴
			if (isTargetPahtRight()) { // 若目标路径正确
				/*
				 * 如下的两个postDelayed的作用在于能够正常的先退出编辑对话框，
				 * 然后再显示出正在拷贝的进度对话框，然后若拷贝完毕则退出正在 拷贝的进度对话框.
				 */
				CopyFileUtils.is_copy_finish = false;
				mCopyingHandler.postDelayed(mCopyingRun, 10);
				mCopyHandler.postDelayed(mCopyRun, 100);
			}
			// is_cope = false;
			break;
		case R.id.edit_rename: // ---重命名
			if (multi_path.get(0).file.canWrite()) {
				FileRename(multi_path.get(0));
			}
			is_cope = false;
			multi_path = new ArrayList<FileInfo>();
			break;
		case R.id.edit_cancel_btu:
			if (is_multi_choice) {
				is_multi_choice = !is_multi_choice;
				multi_choice_process(is_multi_choice);
			}
			is_cope = false;
			multi_path = new ArrayList<FileInfo>();
			break;
		case R.id.edit_bluetooth: 
			/*ArrayList<Uri> sends = getUris(multi_path);
				Intent intent = new Intent();
				if (sends.size() == 1) {
					intent.setAction(Intent.ACTION_SEND);
					intent.putExtra(Intent.EXTRA_STREAM, sends.get(0));
					intent.setType("application/zip");
					multi_path = new ArrayList<FileInfo>();
					startActivity(Intent.createChooser(intent, null));
				} else if (sends.size() > 1) {
					intent.setAction(Intent.ACTION_SEND_MULTIPLE);
					intent.setType("x-mixmedia/*");
					intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM,
							sends);
					multi_path = new ArrayList<FileInfo>();
					startActivity(Intent.createChooser(intent, null));
				}*/

			shareFiles(FileControl.currently_path);

			break;
		case R.id.edit_shotcut: // ---创建桌面快捷方式
			addShortcut(RockExplorer.this, multi_path.get(0));
			break;
		default:
			break;
		}
		if (is_multi_choice) { // 若是在多选取状态下的话，则去除多选状态
			is_multi_choice = !is_multi_choice;
			multi_choice_process(is_multi_choice);
		}
		if (editorDialog != null)
			editorDialog.dismiss();
	}

	//分享功能
	private static final String [] filterList = {"com.google.android.gm"};
	private void shareFiles(String currenPath) {
		if(currenPath == null ) {
			return;
		}
		if(currenPath.startsWith(flash_dir)) {
			ShareIntent.SetFilter(new String[]{});
			//System.out.println("currenPath.startsWith(flash_dir)");
		} else {
			ShareIntent.SetFilter(filterList);
			//System.out.println("currenPath.startsWith(others)");
		}

		ArrayList<Uri> srcSends = getUris(multi_path);
		//System.out.println("srcSends:" + srcSends);
		if (srcSends.size() == 1) {
			//System.out.println("srcSends.size() == 1");
			FileInfo mListtmp = multi_path.get(0);
			if("application/vnd.android.package-archive".equals(mListtmp.file_type)) {
				mListtmp.file_type = "application/zip";
			}
			ShareIntent.shareFile(getApplicationContext(), mListtmp.file_type, srcSends.get(0));
		} else if (srcSends.size() > 1) {
			//System.out.println("srcSends.size() > 1");
			ShareIntent.shareMultFile(getApplicationContext(), "x-mixmedia/*", srcSends);
		}
		if (is_multi_choice) { // 若是在多选取状态下的话，则去除多选状态
			is_multi_choice = !is_multi_choice;
			multi_choice_process(is_multi_choice);
		}
		multi_path = new ArrayList<FileInfo>();

	}

	private ArrayList<Uri> getUris(ArrayList<FileInfo> fileinfos) {
		ArrayList<Uri> uris = new ArrayList<Uri>();
		for (FileInfo fileinfo : fileinfos) {
			addFileUri(fileinfo.file, uris);
		}
		return uris;
	}

	private void addFileUri(File mfile, ArrayList<Uri> tmpuris) {
		if (mfile.isDirectory()) {
			File[] files = mfile.listFiles();
			for (File file : files) {
				addFileUri(file, tmpuris);
			}
		} else {
			tmpuris.add(Uri.fromFile(mfile));
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_DEL: // 册除对话框
			return DelDialog();
		}
		return super.onCreateDialog(id);
	}

	/**/
	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (event.getAction() == KeyEvent.ACTION_DOWN) {
			switch (event.getKeyCode()) {
			case KeyEvent.KEYCODE_BACK:

				LOG("in the dispatchKeyEvent, -11--- KEYCODE_BACK");
				if (copyingDialog != null) { // 若拷贝进度对话框存在时，则结束拷贝
					LOG("in the dispatchKeyEvent, -22--- KEYCODE_BACK");
					CopyFileUtils.is_enable_copy = false;
					CopyFileUtils.is_pause_copy = false;
				}
				if (mDelDialog != null && mDelDialog.isShowing()) {
					FileControl.is_enable_del = false;
				}
				if (!FileControl.is_finish_fill)
					FileControl.is_enable_fill = false; // 结束之前fill的thread
				break;

			case KeyEvent.KEYCODE_FORWARD_DEL:
				//System.out.println("del_button");
				int position = -1;
				if (main_ListView.isFocused() && mFileControl.get_currently_path() != null) {
					position = main_ListView.getSelectedItemPosition();
				}
				/**
				 * 一、position不为-1：
				 *   如果是多选的状态，并且有选中项，默认删除多选的内容；否则删除选中项
				 * 二、position为-1：
				 * 	    如果为多选状态，且有选中项，则删除选中项。否则不做操作.
				 */
				if(position != -1) {
					//System.out.println("the select position to delete = " + position);
					if(is_multi_choice == true && 
							multi_path != null && multi_path.size() > 0){
						is_multi_choice = !is_multi_choice;
						multi_choice_process(false); // 刷新一下多选情况
					} else {
						if (multi_path == null || multi_path.size() > 0) {
							is_cope = false;
							multi_path = new ArrayList<FileInfo>();
						}
						if(is_multi_choice == true) { //清楚多选状态
							is_multi_choice = !is_multi_choice;
							multi_choice_process(false); // 刷新一下多选情况
						}
						FileInfo mListtmp = folder_array.get(position);
						multi_path.add(mListtmp);
					}
					mDelDialog.show();
				} else if(is_multi_choice == true) {  
					if(multi_path == null || multi_path.size() == 0) {
						is_cope = false;
						multi_path = new ArrayList<FileInfo>();
						is_multi_choice = !is_multi_choice;
						multi_choice_process(false); // 刷新一下多选情况
					} else {
						is_multi_choice = !is_multi_choice;
						multi_choice_process(false); // 刷新一下多选情况
						mDelDialog.show();
					}
				}

				break;
			}
		}

		if (event.isCtrlPressed() && event.getKeyCode() == KeyEvent.KEYCODE_C
				&& event.getAction() == KeyEvent.ACTION_UP) {
			Ctrl_cORx('c');
		}

		if (event.isCtrlPressed() && event.getKeyCode() == KeyEvent.KEYCODE_X
				&& event.getAction() == KeyEvent.ACTION_UP) {
			Ctrl_cORx('x');
		}

		if (event.isCtrlPressed() && event.getKeyCode() == KeyEvent.KEYCODE_V
				&& event.getAction() == KeyEvent.ACTION_UP) {
			//System.out.println("ctrl+v");
			if(mFileControl.get_currently_path() != null && 
					multi_path!=null && multi_path.size()>0 && is_cope == true) {
				if (isTargetPahtRight()) { // 若目标路径正确
					//System.out.println("ctrl+v  ,multi_path:" + multi_path.get(0));
					CopyFileUtils.is_copy_finish = false;
					mCopyingHandler.postDelayed(mCopyingRun, 10);
					mCopyHandler.postDelayed(mCopyRun, 100);
				}
			}
		}

		return super.dispatchKeyEvent(event);
	}

	//拷贝--通过键盘ctrl+c或者ctrl+x
	private void Ctrl_cORx(char cORx){
		int position = -1;
		if (main_ListView.isFocused() && mFileControl.get_currently_path() != null) {
			position = main_ListView.getSelectedItemPosition();
		}
		//System.out.println("ctrl+" + cORx + "  ,position:" + position);

		/**
		 * 一、position不为-1：
		 *   如果是多选的状态，并且有选中项，默认复制多选的内容；否则复制选中项
		 * 二、position为-1：
		 * 	    如果为多选状态，且有选中项，则复制选中项。否则不做操作，且不能粘贴.
		 */
		if(position != -1) {
			//System.out.println("the select position = " + position);
			if(is_multi_choice == true && 
					multi_path != null && multi_path.size() > 0){
				is_multi_choice = !is_multi_choice;
				multi_choice_process(false); // 刷新一下多选情况
				is_move = false;
				if(cORx == 'x') {
					is_move = true;
				}
				is_cope = true;
			} else {
				if (multi_path == null || multi_path.size() > 0) {
					is_cope = false;
					multi_path = new ArrayList<FileInfo>();
				}
				if(is_multi_choice == true) { //清楚多选状态
					is_multi_choice = !is_multi_choice;
					multi_choice_process(false); // 刷新一下多选情况
				}
				FileInfo mListtmp = folder_array.get(position);
				multi_path.add(mListtmp);

				is_move = false;
				if(cORx == 'x') {
					is_move = true;
				}
				is_cope = true;
				//System.out.println("multi_path.get(0):"  + multi_path.get(0));
			}
		} else if(is_multi_choice == true) {  // 若在有多选的情况下点击ctrl+?
			//System.out.println("is_multi_choice == true");
			if(multi_path == null || multi_path.size() == 0) {
				is_cope = false;
				multi_path = new ArrayList<FileInfo>();
			} else {
				is_move = false;
				if(cORx == 'x') {
					is_move = true;
				}
				is_cope = true;
			}
			is_multi_choice = !is_multi_choice;
			multi_choice_process(false); // 刷新一下多选情况
			//for(int i=0;i<multi_path.size();i++) {
			//	System.out.println("else --multi_path.get(" + i + "):"  + multi_path.get(i));
			//}
		}
	}


	Dialog mDelDialog = null;

	public void dissmissDelDialog() {
		mDelDialog.dismiss();
		// dismissDialog(DIALOG_DEL);
	}

	/*
	 * 全局状态的一个判断，从而在编辑对话框中显示不同的编辑装态
	 */
	public void setEditDialog(View layout) {
		Button temp_edit_copy = (Button) layout.findViewById(R.id.edit_copy);
		Button temp_edit_move = (Button) layout.findViewById(R.id.edit_move);
		Button temp_edit_delete = (Button) layout.findViewById(R.id.edit_delete);
		Button temp_edit_rename = (Button) layout.findViewById(R.id.edit_rename);
		Button temp_edit_bluetooth = (Button) layout
				.findViewById(R.id.edit_bluetooth);
		Button temp_edit_paste = (Button) layout.findViewById(R.id.edit_paste);
		Button temp_edit_shotcut = (Button) layout.findViewById(R.id.edit_shotcut);

		temp_edit_paste.setEnabled(false);
		setAlphaAndTextColor( temp_edit_paste, false);
		if (multi_path.size() == 0) { // 若没有选中的文件 --不可复制、移动、删除、重命名
			temp_edit_copy.setEnabled(false);
			setAlphaAndTextColor(temp_edit_copy, false);

			temp_edit_move.setEnabled(false);
			setAlphaAndTextColor(temp_edit_move, false);

			temp_edit_delete.setEnabled(false);
			setAlphaAndTextColor(temp_edit_delete, false);

			temp_edit_rename.setEnabled(false);
			setAlphaAndTextColor(temp_edit_rename, false);

			temp_edit_bluetooth.setEnabled(false);
			setAlphaAndTextColor(temp_edit_bluetooth, false);

			temp_edit_shotcut.setEnabled(false);
			setAlphaAndTextColor(temp_edit_shotcut, false);
		} else { // 若有选中的文件 --可复制、移动、删除
			if (is_cope) { // 若之前有做复制或移动的选择，则可粘贴，但不可复制、移动、删除
				temp_edit_paste.setEnabled(true);
				setAlphaAndTextColor(temp_edit_paste, true);

				temp_edit_copy.setEnabled(false);
				setAlphaAndTextColor(temp_edit_copy, false);

				temp_edit_move.setEnabled(false);
				setAlphaAndTextColor(temp_edit_move, false);

				temp_edit_delete.setEnabled(false);
				setAlphaAndTextColor(temp_edit_delete, false);

				temp_edit_bluetooth.setEnabled(false);
				setAlphaAndTextColor(temp_edit_bluetooth, false);
			} else { // 否则不可粘贴，但可复制、移动、删除
				temp_edit_paste.setEnabled(false);
				setAlphaAndTextColor(temp_edit_paste, false);

				temp_edit_copy.setEnabled(true);
				setAlphaAndTextColor(temp_edit_copy, true);

				temp_edit_move.setEnabled(true);
				setAlphaAndTextColor(temp_edit_move, true);

				temp_edit_delete.setEnabled(true);
				setAlphaAndTextColor(temp_edit_delete, true);

				temp_edit_bluetooth.setEnabled(true);
				setAlphaAndTextColor(temp_edit_bluetooth, true);
			}
			//只要有选中，都可以创建桌面快捷方式
			temp_edit_shotcut.setEnabled(true);
			setAlphaAndTextColor(temp_edit_shotcut, true);
		}
		if (is_rename) { // 若可重命名
			temp_edit_rename.setEnabled(true);
			setAlphaAndTextColor(temp_edit_rename, true);
			is_rename = false;
		} else { // 若不可重命名
			temp_edit_rename.setEnabled(false);
			setAlphaAndTextColor(temp_edit_rename, false);
		}
	}

	public void setAlphaAndTextColor(Button temp_Text, boolean visibility) {
		if (visibility) {
			temp_Text.setTextColor(0xffffffff);
		} else {
			temp_Text.setTextColor(0xff848484);
		}
	}

	/*
	 * 如下的handle负责监听拷贝是否完成
	 */
	Handler mCopyHandler = new Handler();
	Runnable mCopyRun = new Runnable() {
		public void run() {

			if (CopyFileUtils.is_copy_finish) {
				LOG("mCopyRun--if (CopyFileUtils.is_copy_finish)");
				try {
					mCopyService.setCopyStatus(CopyService.COPY_STATUS_IDLE);
				} catch (android.os.RemoteException e) {
					e.printStackTrace();
				}
				UnLockScreen();
				LOG("mCopyRun, the CopyFileUtils.is_copy_finish = "
						+ CopyFileUtils.is_copy_finish + "   is_move = "
						+ is_move);
				if (is_move && !CopyFileUtils.is_same_path) { // 若是移动操作且不是在同一目录下操作，则还需将原文件删除
					is_move = false;
					mFileControl.delFileInfo(CopyFileUtils.has_copy_path);
					// if(is_rescan){
					if (CopyFileUtils.has_copy_path.size() > 0)
						reMedioScan(CopyFileUtils.has_copy_path.get(0).file
								.getParent()); // 重扫移动文件路径的媒体库
					// }
				}
				LOG("mCopyRun, CopyFileUtils.is_enable_copy = "
						+ CopyFileUtils.is_enable_copy);
				if (!CopyFileUtils.is_enable_copy) { // 若是强制中断而结束的拷贝，则还需把未拷完的文件删除
					mFileControl.DelFile(CopyFileUtils.mInterruptFile);
				}
				/*
				 * 如下为更新当前文件列表
				 */
				// mFileControl.refill(mFileControl.get_currently_path());
				for (int i = 0; i < CopyFileUtils.has_copy_path.size(); i++) {
					File tmp_file = new File(mFileControl.get_currently_path()
							+ File.separator
							+ CopyFileUtils.has_copy_path.get(i).file.getName());
					FileInfo tmp_fileinfo = mFileControl
							.changeFiletoFileInfo(tmp_file);
					if (!mFileControl.is_contain_file(tmp_file)) {
						if (!mFileControl.folder_array.contains(tmp_fileinfo)) {
							if (tmp_file.isDirectory()) { // 若是文件夹
								mFileControl.folder_array.add(0, tmp_fileinfo);
								folder_array.add(0, tmp_fileinfo);
							} else {
								mFileControl.folder_array.add(tmp_fileinfo);
								folder_array.add(tmp_fileinfo);
							}
						}
					}
				}
				setFileAdapter(false, false); // 重刷文件列表

				if (copyingDialog != null) { // 若拷贝结束，则关闭拷贝进度对话框
					LOG("mCopyRun--if (copyingDialog != null)  copyingDialog.dismiss();");
					copyingDialog.dismiss();
					copyingDialog = null;
				}
				// if(is_rescan){
				reMedioScan(mFileControl.get_currently_path()); // 重扫当前路径的媒体库
				// }
				CopyFileUtils.is_copy_finish = false;
				if (CopyFileUtils.is_not_free_space) {
					CopyFileUtils.is_not_free_space = false;
					Toast.makeText(RockExplorer.this,
							getString(R.string.err_not_free_space),
							Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(RockExplorer.this,
							getString(R.string.copy_finish),
							Toast.LENGTH_SHORT).show();
				}
				is_cope = false;
				multi_path = new ArrayList<FileInfo>(); // 清空 multi_path

				mCopyHandler.removeCallbacks(mCopyRun);
			} else {
				if (is_cope) { // 第一次进入该mCopyRun时进行拷贝操作
					// LOG("in mCopyRun, the is_cope = " + mCopyRun);
					LOG("in mCopyRun, the is_cope = " + mCopyRun);
					is_cope = false;
					LockScreen();
					CopyFileUtils.multi_path = multi_path;
					try {
						mCopyService.startCopy(
								mFileControl.get_currently_path(), is_move);
					} catch (android.os.RemoteException e) {
						e.printStackTrace();
					}
					CopyFileUtils.cope_now_sourceFile = multi_path.get(0).file;
					CopyFileUtils.cope_now_targetFile = 
							new File(mFileControl.get_currently_path() 
									+ File.separator
									+ multi_path.get(0).file.getName());
					// mCopyFileUtils.CopyFileInfoArray(multi_path,
					// mFileControl.get_currently_path());
				}

				if ((CopyFileUtils.cope_now_sourceFile != null)
						&& (CopyFileUtils.cope_now_targetFile != null)) {
					((TextView) myCopyView.findViewById(R.id.source_Text))
					.setText(getChangePath(CopyFileUtils.cope_now_sourceFile
							.getPath()));
					((TextView) myCopyView.findViewById(R.id.target_Text))
					.setText(getChangePath(CopyFileUtils.cope_now_targetFile
							.getPath()));

					float percent = (((float) CopyFileUtils.cope_now_targetFile
							.length()) / ((float) CopyFileUtils.cope_now_sourceFile
									.length())) * 100;
					/*LOG(" ===========================begin============================= ");
					LOG(" CopyFileUtils.cope_now_targetFile.length() = " +
						CopyFileUtils.cope_now_targetFile.length());
					LOG(" CopyFileUtils.cope_now_sourceFile.length() = " +
						CopyFileUtils.cope_now_sourceFile.length());

					LOG(" CopyFileUtils.cope_now_targetFile.getName() = " +
						CopyFileUtils.cope_now_targetFile.getName());
					LOG(" CopyFileUtils.cope_now_sourceFile.getName() = " +
						CopyFileUtils.cope_now_sourceFile.getName());	

					LOG("mCopyRun--run--percent = " + percent);
					LOG(" ============================end============================== ");*/
					((ProgressBar) myCopyView
							.findViewById(R.id.one_copy_percent))
							.setProgress((int) percent);
					((TextView) myCopyView.findViewById(R.id.one_percent_Text))
					.setText(percent + " %");

					// percent = (((float)CopyFileUtils.mhascopyfilecount) /
					// ((float)CopyFileUtils.mallcopyfilecount)) * 100;
					percent = ((float) (CopyFileUtils.mhascopyfileLength) / CopyFileUtils.mallcopyfileLength) * 100.0f;
					((ProgressBar) myCopyView
							.findViewById(R.id.all_copy_percent))
							.setProgress((int) percent);
					((TextView) myCopyView.findViewById(R.id.all_percent_Text))
					.setText("" + CopyFileUtils.mhascopyfilecount
							+ " / " + CopyFileUtils.mallcopyfilecount);
				}
				// LOG("in mCopyRun, --- --- the is_cope = " + is_cope +
				// "    CopyFileUtils.mRecoverFile = " +
				// CopyFileUtils.mRecoverFile);

				if (CopyFileUtils.mRecoverFile != null) {
					if (mRecoverDialog == null
							|| (mRecoverDialog != null && !mRecoverDialog
							.isShowing())) {
						mRecoverDialog = new AlertDialog.Builder(
								RockExplorer.this)
						.setMessage(
								CopyFileUtils.mRecoverFile
								+ getString(R.string.copy_revocer_text))
								.setPositiveButton(
										getString(R.string.copy_revocer_yes),
										new DialogInterface.OnClickListener() {
											public void onClick(
													DialogInterface dialog,
													int which) {
												CopyFileUtils.is_recover = true; // 标志进行复盖
												CopyFileUtils.is_wait_choice_recover = false; // 恢复拷贝进程
												CopyFileUtils.mRecoverFile = null;
												dialog.dismiss();
												mRecoverDialog = null;
											}
										})
										.setNegativeButton(
												getString(R.string.copy_revocer_no),
												new DialogInterface.OnClickListener() {
													public void onClick(
															DialogInterface dialog,
															int which) {
														CopyFileUtils.is_recover = false; // 标志不进行复盖
														CopyFileUtils.is_wait_choice_recover = false; // 恢复拷贝进程
														CopyFileUtils.mRecoverFile = null;
														dialog.dismiss();
														mRecoverDialog = null;
													}
												}).create();

						mRecoverDialog.show();
					}

				}
				if (CopyFileUtils.is_enable_copy)
					mCopyHandler.postDelayed(mCopyRun, 500);
				else
					mCopyHandler.postDelayed(mCopyRun, 1000);
			}
		}
	};

	/*
	 * 如下的handle是负责显示出拷贝进度对话框
	 */
	View myCopyView;
	AlertDialog copyingDialog;
	Handler mCopyingHandler = new Handler();
	Runnable mCopyingRun = new Runnable() {
		public void run() {

			LOG("mCopyingRun");
			if (copyingDialog == null) {
				LOG("mCopyingRun-----copyingDialog==null");
				LayoutInflater factory = LayoutInflater.from(RockExplorer.this);
				/* 初始化myChoiceView，使用copy_dialog为layout */
				myCopyView = factory.inflate(R.layout.copy_dialog, null);
				copyingDialog = new AlertDialog.Builder(RockExplorer.this)
				.create();
				((Button) myCopyView.findViewById(R.id.but_stop_copy))
				.setOnClickListener(new View.OnClickListener() {
					public void onClick(View arg0) {
						// TODO Auto-generated method stub
						if (copyingDialog != null) { // 若拷贝进度对话框存在时，则结束拷贝
							LOG("------- >>>>>>>>>>>>>>>>>  Click on the Stop Copy Button");
							// CopyFileUtils.is_copy_finish = true;
							CopyFileUtils.is_enable_copy = false;
							UnLockScreen();
						}
					}
				});
				copyingDialog.setCanceledOnTouchOutside(false);
				copyingDialog.setView(myCopyView);
				copyingDialog.setOnDismissListener(mOnDismissListener);
				copyingDialog.setOnKeyListener(mOnKeyListener);
				copyingDialog.show();
			} else if (!copyingDialog.isShowing()) {
				copyingDialog.show();
			}
			mCopyingHandler.removeCallbacks(mCopyingRun);
		}
	};
	OnKeyListener mOnKeyListener = new OnKeyListener() {
		@Override
		public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
			switch (keyCode) {
			case KeyEvent.KEYCODE_BACK:
				LOG("----------------->>> onKey,    KeyEvent.KEYCODE_BACK , keyCode = "
						+ keyCode);
				CopyFileUtils.is_copy_finish = true;
				CopyFileUtils.is_enable_copy = false;
				CopyFileUtils.is_pause_copy = false;
				dialog.dismiss();
				break;
			}
			return false;
		}
	};
	OnDismissListener mOnDismissListener = new OnDismissListener() {
		@Override
		public void onDismiss(DialogInterface dialog) {
			LOG(" -------------------------- 0(O o O)0     copy dialog dismiss");
			CopyFileUtils.is_enable_copy = false;
			UnLockScreen();
		}
	};
	/*
	 * 如下的handle是负责显示出拷贝进度对话框
	 */
	public String fill_path = null;
	Handler mOpenHandler = new Handler();
	Runnable mOpeningRun = new Runnable() {
		public void run() {
			if (openwiththread) {
				mFileControl.refillwithThread(fill_path); // 使用线程重新refill
			} else {
				mFileControl.refill(fill_path); // 重新refill
			}
			mOpenHandler.removeCallbacks(mOpeningRun);
		}
	};
	/*
	 * 如下的handle是负责打开文件的监听
	 */
	Handler mFillHandler = new Handler();
	Runnable mFillRun = new Runnable() {
		public void run() {
			LOG("in the mFillRun, is_finish_fill = "
					+ FileControl.is_finish_fill);
			LOG("in the mFillRun, adapte size = "
					+ mFileControl.folder_array.size());
			if (!FileControl.is_finish_fill) { // 若fill还未结束
				if (openwiththread) {
					setFileAdapter(false, false);
					main_ListView
					.setSelection(mFileControl.folder_array.size() - 6);
				}
				mFillHandler.postDelayed(mFillRun, 1500);
			} else {
				if (openwiththread)
					setFileAdapter(false, false);
				else {
					if (is_last_path) {
						is_last_path = false;
						setFileAdapter(false, false);
					} else {
						setFileAdapter(true, m_is_left);
					}
				}
				mFillHandler.removeCallbacks(mFillRun);
				if (openingDialog != null)
					openingDialog.dismiss();
			}
		}
	};
	/**/
	private static final int MENU_APP_MANAGE = Menu.FIRST + 1;
	private static final int MENU_TOOL_HIDE = Menu.FIRST + 2;
	private static final int MENU_TOOL_SHOW = Menu.FIRST + 3;

	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, MENU_APP_MANAGE, 0, R.string.str_menu_app_manage)
		.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		menu.add(0, MENU_TOOL_HIDE, 0, R.string.str_menu_tool_hide);
		menu.add(0, MENU_TOOL_SHOW, 0, R.string.str_menu_tool_show);
		return true;
	}

	public boolean onPrepareOptionsMenu(Menu menu) {
		if (tool_bar.getVisibility() == View.VISIBLE) {
			menu.getItem(1).setVisible(true);
			menu.getItem(2).setVisible(false);
		} else {
			menu.getItem(1).setVisible(false);
			menu.getItem(2).setVisible(true);
		}
		return super.onPrepareOptionsMenu(menu);
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_APP_MANAGE: // 选择应用管理
			String action = "android.intent.action.MAIN";
			String category = "android.intent.category.DEFAULT";
			String packageName = "com.android.settings";
			String className = "com.android.settings.ManageApplications";
			ComponentName cn = new ComponentName(packageName, className);
			Intent tmp = new Intent();
			tmp.setComponent(cn);
			tmp.setAction(action);
			tmp.addCategory(category);
			tmp.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(tmp);
			break;
		case MENU_TOOL_HIDE:
			tool_bar.setVisibility(View.GONE);
			break;
		case MENU_TOOL_SHOW:
			tool_bar.setVisibility(View.VISIBLE);
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	/*
	 * 用来判断拷贝的过程中，源路径与目的路径是否冲突
	 */
	public boolean isTargetPahtRight() {
		if (multi_path.get(0).file.getParent().equals(
				mFileControl.get_currently_path())) {
			Toast.makeText(this, getString(R.string.err_target_same),
					Toast.LENGTH_SHORT).show();
			return false;
		} else {
			for (int i = 0; i < multi_path.size(); i++) {
				if (multi_path.get(i).file.isDirectory()) {
					if (mFileControl.get_currently_path().startsWith(
							multi_path.get(i).file.getPath() + "/")
							|| mFileControl.get_currently_path().equals(
									multi_path.get(i).file.getPath())) {
						Toast.makeText(this,
								getString(R.string.err_target_child),
								Toast.LENGTH_SHORT).show();
						return false;
					}
				}
			}
		}
		return true;
	}

	/*
	 * 通过找到相应的图片文件所在的数据库的Url
	 */
	public Uri getFileUri(File tmp_file, String tmp_type) {
		String path = tmp_file.getPath();
		String name = tmp_file.getName();
		if (tmp_type.equals("image/*")) { // 获取图片的URI
			ContentResolver resolver = getContentResolver();
			String[] audiocols = new String[] { MediaStore.Images.Media._ID,
					MediaStore.Images.Media.DATA, MediaStore.Images.Media.TITLE };
			LOG("in getFileUri");
			LOG("in getFileUri --- path = " + path);
			StringBuilder where = new StringBuilder();
			where.append(MediaStore.Images.Media.DATA + "=" + "'"
					+ addspecialchar(path) + "'");
			Cursor cur = resolver.query(
					MediaStore.Images.Media.EXTERNAL_CONTENT_URI, audiocols,
					where.toString(), null, null);
			if (cur.moveToFirst()) {
				int Idx = cur
						.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
				String id = cur.getString(Idx);
				return Uri.withAppendedPath(
						MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
			}
		} else if (tmp_type.equals("audio/*")) { // 获取音频的URI
			if (path.endsWith(".3gpp")) {
				ContentResolver resolver = getContentResolver();
				String[] audiocols = new String[] { MediaStore.Audio.Media._ID,
						MediaStore.Audio.Media.DATA,
						MediaStore.Audio.Media.TITLE,
						MediaStore.Audio.Media.MIME_TYPE };
				LOG("in getFileUri");
				LOG("in getFileUri --- path = " + path + " path.lenght:"
						+ path.length() + " trimlength:" + path.trim().length());
				StringBuilder where = new StringBuilder();
				where.append(MediaStore.Audio.Media.DATA + "=" + "'" + path
						+ "'");

				Cursor cur = resolver.query(
						MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, audiocols,
						MediaStore.Audio.Media.DATA + "=?",
						new String[] { path.trim() }, null);

				// Cursor cur =
				// resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
				// audiocols,
				// where.toString(), null, null);

				if (cur.moveToNext()) {
					int Idx = cur
							.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
					String id = cur.getString(Idx);

					int dataIdx = cur
							.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
					String data = cur.getString(dataIdx);
					return Uri.withAppendedPath(
							MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
					// if(data.equals(path))
					// return
					// Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
					// id);
				}
				return null;
			}
			ContentResolver resolver = getContentResolver();
			String[] audiocols = new String[] { MediaStore.Audio.Media._ID,
					MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.TITLE };
			LOG("in getFileUri");
			LOG("in getFileUri --- path = " + path);
			StringBuilder where = new StringBuilder();
			where.append(MediaStore.Audio.Media.DATA + "=" + "'"
					+ addspecialchar(path) + "'");
			Cursor cur = resolver.query(
					MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, audiocols,
					where.toString(), null, null);
			if (cur.moveToFirst()) {
				int Idx = cur.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
				String id = cur.getString(Idx);
				return Uri.withAppendedPath(
						MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
			}
			// return
			// Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
			// cur.getString(cur.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)));
		} else if (tmp_type.equals("video/*")) { // 获取视频的URI
			ContentResolver resolver = getContentResolver();
			String[] audiocols = new String[] { MediaStore.Video.Media._ID,
					MediaStore.Video.Media.DATA, MediaStore.Video.Media.TITLE };
			LOG("in getFileUri");
			LOG("in getFileUri --- path = " + path);
			StringBuilder where = new StringBuilder();
			where.append(MediaStore.Video.Media.DATA + "=" + "'"
					+ addspecialchar(path) + "'");
			Cursor cur = resolver.query(
					MediaStore.Video.Media.EXTERNAL_CONTENT_URI, audiocols,
					where.toString(), null, null);
			if (cur.moveToFirst()) {
				int Idx = cur.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
				String id = cur.getString(Idx);
				return Uri.withAppendedPath(
						MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id);
			}
		}
		return null;
	}

	public String addspecialchar(String mstring) {
		String ret = mstring;
		char[] tmpchar = null;
		if (ret.contains("'")) {
			int csize = 0;
			int i = 0;
			for (i = 0; i < ret.length(); i++) {
				if (ret.charAt(i) == '\'') {
					csize++;
				}
			}
			int len = ret.length() + (csize * 1);
			tmpchar = new char[len];
			int j = 0;
			for (i = 0; i < ret.length(); i++) {
				if (ret.charAt(i) == '\'') {
					tmpchar[j] = '\'';
					j++;
				}
				tmpchar[j] = ret.charAt(i);
				j++;
			}
			ret = String.valueOf(tmpchar);
		}
		return ret;
	}

	/*
	 * 重新打开一个目录的过程： 1、先结速之前fill的thread---- 将 is_enable_fill 值 false; 2、再等侍
	 * fill的thread的结束---- 判断 is_finish_fill 是否为 true; 3、然后再调用方法 refill(String
	 * path);
	 */
	public void openDir(String path) {
		fill_path = path;
		mFillHandler.removeCallbacks(mFillRun); // 先结束
		FileControl.is_enable_fill = false; // 先结束之前fill的thread
		FileControl.is_finish_fill = false;

		/**/
		File files = new File(path);
		if (!files.exists() || !files.canRead()) {
			FileControl.is_enable_fill = true;
			FileControl.is_finish_fill = true;
			return;
		}
		long file_count = files.listFiles().length;
		LOG("in the openDir, file_count = " + file_count);
		if (file_count > 1500) {
			openwiththread = true;
			if (openingDialog == null) {
				openingDialog = new ProgressDialog(RockExplorer.this);
				openingDialog.setTitle(R.string.str_openingtitle);
				openingDialog
				.setMessage(getString(R.string.str_openingcontext));
				openingDialog.show();
			} else {
				openingDialog.show();
			}
		} else {
			openwiththread = false;
		}
		if(is_from_shotcut) {
			mOpenHandler.postDelayed(mOpeningRun, 200); // 重新开始扫文件列表
			mFillHandler.postDelayed(mFillRun, 300); // 监听文件打开是否完成
		} else {
			mOpenHandler.post(mOpeningRun);
			mFillHandler.post(mFillRun);
		}
	}

	public void reMedioScan(String rescanFilePath) {
		/*
		 * String tmp = rescanFilePath + "/"; LOG("  ####  rescanFilePath = " +
		 * rescanFilePath); Intent intent = new
		 * Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" +
		 * tmp)); intent.putExtra("read-only", false); sendBroadcast(intent);
		 */

		LOG("  ####  rescanFilePath = " + rescanFilePath);
		String scan_path = new String();
		/*
		 * if(rescanFilePath.startsWith(flash_dir)){ scan_path = "flash"; }else
		 * if(rescanFilePath.startsWith(sdcard_dir)){ scan_path = "sdcard";
		 * }else if(rescanFilePath.startsWith(usb_dir)){ scan_path = "usb1";
		 * }else{ return; }
		 */

		if (rescanFilePath.startsWith(flash_dir)) {
			scan_path = flash_dir;
		} else if (rescanFilePath.startsWith(sdcard_dir)) {
			scan_path = sdcard_dir;
		} else if (rescanFilePath.startsWith(usb_dir)) {
			scan_path = usb_dir;
		} else {
			return;
		}

		Intent intent = new Intent(Intent.ACTION_MEDIA_MOUNTED,
				Uri.parse("file://" + scan_path));
		intent.putExtra("read-only", false);
		//sendBroadcast(intent);
	}

	/*
	 * 进入最上层文件列表
	 */
	public void fill_first_path(FileControl mtmpFileControl) {
		mtmpFileControl.first_fill();
		image_multi_choice.setEnabled(false);
		enableButton(false);
		setFileAdapter(true, false);
		setMyTitle(getString(R.string.app_name));
	}

	/*
	 * 根据参数使能与禁止相应的一些功能选项
	 */
	public void enableButton(boolean tmp) {
		View tmp_view = (View) findViewById(R.id.tool_editor);
		tmp_view.setEnabled(tmp);
		tmp_view = (View) findViewById(R.id.tool_new_folder);
		tmp_view.setEnabled(tmp);
		tmp_view = (View) findViewById(R.id.tool_multi_choice);
		tmp_view.setEnabled(tmp);
		tmp_view = (View) findViewById(R.id.tool_level_up);
		tmp_view.setEnabled(tmp);
	}

	/*
	 * 接收usb/sdcard umount消息
	 */
	private BroadcastReceiver mScanListener = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			LOG("mScanListener BroadcastReceiver action" + action);

			if (action.equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
				LOG("in the mScanListener, ---- Intent.ACTION_MEDIA_UNMOUNTED");
				if (mFileControl != null
						&& intent != null
						&& intent.getData() != null
						&& intent.getData().getPath() != null
						&& mFileControl.get_currently_path() != null
						&& mFileControl.get_currently_path().startsWith(
								intent.getData().getPath())) {
					finish();
				} else if (mFileControl.get_currently_path() == null) {
					Log.d(TAG,
							"   mFileControl.get_currently_path() = null~~~~");
					finish();
				}
			} else if (Intent.ACTION_MEDIA_BAD_REMOVAL.equals(action)) {
				Log.v(TAG,
						"in the mScanListener, ---- ACTION_MEDIA_BAD_REMOVAL");
				finish();
			}

			/*
			 * else if(action.equals(Intent.ACTION_MEDIA_MOUNTED)){
			 * LOG("in the mScanListener, ---- Intent.ACTION_MEDIA_UNMOUNTED");
			 * finish(); }
			 */
		}
	};

	/*
	 * 注册接收usb/sdcard umount消息
	 */
	public void registBroadcastRec() {
		/*
		 * < 设置 监听 "scanner started", "scanner finished" 和 "SD 卡 unmounted" 等事件
		 * >.
		 */
		IntentFilter f = new IntentFilter();
		// f.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
		// f.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
		f.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
		f.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
		// f.addAction(Intent.ACTION_MEDIA_MOUNTED);
		f.addDataScheme("file");
		registerReceiver(mScanListener, f);
	}

	/*
	 * 用于判断当前是否有mount SD卡
	 */
	public static boolean isMountSD() {
		/*//String status = SystemProperties.get("EXTERNAL_STORAGE_STATE",MEDIA_REMOVED); 
		String status = mStorageManager.getVolumeState(sdcard_dir);
		LOG("isMountSD()--status-->" + status);
		if (status.equals(Environment.MEDIA_MOUNTED)) {
			return true;
		}
		return false;*/
		return true;
	}

	/*
	 * 用于判断当前是否有mount USB1卡
	 */
	public static final String MEDIA_REMOVED = "removed";

	public static boolean isMountUSB1() {
		/*// String status = SystemProperties.get("USB1_STORAGE_STATE",MEDIA_REMOVED); 
		// String status = Environment.getHostStorageState();
		String status = mStorageManager.getVolumeState(usb_dir);
		LOG(" -------- the usb1     status = " + status);
		if (status.equals(Environment.MEDIA_MOUNTED)) {
			return true;
		}
		return false;*/
		return true;
	}

	public static boolean isMountFLASH() {
		/*// String status = Environment.getFlashStorageState(); 
		String status = mStorageManager.getVolumeState(flash_dir);
		LOG(" -------- the flash status = " + status);
		if (status.equals(Environment.MEDIA_MOUNTED)) {
			return true;
		}
		return false;*/
		return true;
	}

	/*
	 * 判断是否是媒体文件
	 */
	public boolean isFunFile(FileInfo tmp_fileinfo) {
		if (tmp_fileinfo != null && !tmp_fileinfo.isDir) {
			if (tmp_fileinfo.file_type.equals("audio/*")
					|| tmp_fileinfo.file_type.equals("video/*")
					|| tmp_fileinfo.file_type.equals("image/*"))
				return true;
		}
		if (tmp_fileinfo != null && tmp_fileinfo.isDir) {
			return true;
		}
		return false;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {

		if (editorDialog != null && editorDialog.isShowing()) {
			// editorDialog.cancel();
			// showEditorDialog();

			editorDialog.getWindow().findViewById(R.id.myDialogLL).invalidate();
		}

		super.onConfigurationChanged(newConfig);
		if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			LOG(" ________--------- >>  Configuration.ORIENTATION_LANDSCAPE");
		} else if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			LOG(" ________--------- >>  Configuration.ORIENTATION_PORTRAIT");
		}
	}

	private ServiceConnection sc = new ServiceConnection() {
		@Override
		public void onServiceDisconnected(ComponentName name) {
			mCopyService = null;
			LOG(" >>>>>>>>>>>>........in onServiceDisconnected");
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mCopyService = ICopyService.Stub.asInterface(service);
			LOG(" >>>>>>>>>>>>........in onServiceConnected");
			if (mCopyService != null) {
				int tmpstatus = -1;
				try {
					tmpstatus = mCopyService.getCopyStatus();
					mCopyService.notificaFromActivity(true);
				} catch (android.os.RemoteException e) {
					e.printStackTrace();
				}
				if (tmpstatus == CopyService.COPY_STATUS_COPYING) {
					LOG("ServiceConnection--CopyService.COPY_STATUS_COPYING");
					LockScreen();
					mCopyingHandler.postDelayed(mCopyingRun, 10);
					mCopyHandler.postDelayed(mCopyRun, 100);
				} else if (tmpstatus == CopyService.COPY_STATUS_IDLE
						&& copyingDialog != null && copyingDialog.isShowing()) {
					copyingDialog.dismiss();
					LOG("ServiceConnection--CopyService.COPY_STATUS_IDLE");
				}
			}
		}
	};

	//为了做email附件时添加的变量
	private static boolean isGetContentAciton = false;
	/** add by ozn-2012-05-27
	 *  为做email的附件--所添加的方法 
	 * */
	private void getContentAction() {
		Intent intent = getIntent();
		String action = intent.getAction();

		//System.out.println("RockExplorer-->onCreate()-->action-->" + action);
		if (Intent.ACTION_GET_CONTENT.equalsIgnoreCase(action)) {
			isGetContentAciton = true;
		} else {
			isGetContentAciton = false;
		}
	}

	public static void addFloderShortcut(Context cx) {
		Log.i("RockExplorer","addFloderShortcut");
		Intent shortcutIntent = new Intent(
				"com.android.launcher.action.INSTALL_SHORTCUT");
		shortcutIntent.putExtra("duplicate", false);
		shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME,"مناهج التعليم للصف الاول الثانوى");

		Intent intent = null;
		Parcelable icon = null;

		intent = new Intent(cx, RockExplorer.class);
		intent.setAction("android.intent.action.MAIN");
		intent.addCategory("android.intent.category.LAUNCHER");
		intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		intent.setDataAndType(Uri.parse("file:///mnt/sdcard/مناهج التعليم للصف الاول الثانوى"),null);
		intent.putExtra("path", "/mnt/sdcard/مناهج التعليم للصف الاول الثانوى");
		icon = Intent.ShortcutIconResource.fromContext(cx, R.drawable.folder);

		shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, icon);
		shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, intent);
		cx.sendBroadcast(shortcutIntent);
	}

	/**
	 * 为当前应用添加桌面快捷方式
	 * 
	 * @param cx
	 * @param fileInfo 
	 *            
	 */
	private void addShortcut(Context cx , FileInfo fileInfo) {
		// 创建快捷方式的Intent
		Intent shortcutIntent = new Intent(
				"com.android.launcher.action.INSTALL_SHORTCUT");
		// 不允许重复创建
		shortcutIntent.putExtra("duplicate", false);
		// 需要现实的名称
		shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME,
				fileInfo.file.getName());

		Intent intent = null;
		Parcelable icon = null;
		if(fileInfo.isDir) { // 若点击的是文件夹
			intent = new Intent(cx, RockExplorer.class);
			intent.setAction("android.intent.action.MAIN");
			intent.addCategory("android.intent.category.LAUNCHER");
			intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
			intent.setDataAndType(
					Uri.fromFile(fileInfo.file),fileInfo.file_type);

			//System.out.println(fileInfo.file.getPath());
			intent.putExtra("path", fileInfo.file.getPath());
			// 快捷图片--文件夹
			icon = Intent.ShortcutIconResource.fromContext(
					cx, R.drawable.folder);
		} else {  //单个文件的时候，调用这个
			intent = new Intent();
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.setAction(android.content.Intent.ACTION_VIEW);
			intent.setDataAndType(Uri.fromFile(fileInfo.file),
					fileInfo.file_type);
			// 快捷图片--文件类型的图标
			icon = Intent.ShortcutIconResource.fromContext(
					cx, mFileControl.getDrawableId(fileInfo.file_type));
		}

		shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, icon);
		// 点击快捷图片，运行的程序主入口
		shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, intent);
		// 发送广播。OK
		cx.sendBroadcast(shortcutIntent);
	}
	private boolean hadCalledOnNewIntent = false;
	@Override
	protected void onNewIntent(Intent intent) {
		hadCalledOnNewIntent = true;
		String path_ = intent.getStringExtra("path");
		//System.out.println("onNewIntent--path_:" + path_ + "!!");
		invokeFromShotCut(path_);
		super.onNewIntent(intent);
	}


	private void invokeFromShotCut(String path_) {
		boolean exist_flag = false;
		try{
			if(path_ != null) {
				if(new File(path_).exists()) {
					exist_flag = true;
				}
			}
		}catch (Exception e) {
			e.printStackTrace();
		}
		if(!exist_flag && path_!=null) {
			Toast toast = Toast.makeText(this, R.string.file_does_not_exist, 500);
			toast.setGravity(Gravity.CENTER, 0, 0);
			toast.show();
		}

		if(path_ != null && !(path_.equals(last_path))) {
			openDir(path_);
			enableButton(true);
			is_from_shotcut = true;//通过快捷方式进入的文件夹，就不需要动画了
			path_ = null;
		}
	}


	// usually, subclasses of AsyncTask are declared inside the activity class.
	// that way, you can easily modify the UI thread from here
	private class DownloadTask extends AsyncTask<String, Integer, String> {

		private Context context;
		private PowerManager.WakeLock mWakeLock;

		public DownloadTask(Context context) {
			this.context = context;
		}

		@Override
		protected String doInBackground(String... sUrl) {
			InputStream input = null;
			OutputStream output = null;
			HttpURLConnection connection = null;
			try {
				URL url = new URL(sUrl[0]);
				connection = (HttpURLConnection) url.openConnection();
				connection.connect();

				// expect HTTP 200 OK, so we don't mistakenly save error report
				// instead of the file
				if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
					return "Server returned HTTP " + connection.getResponseCode()
							+ " " + connection.getResponseMessage();
				}

				// this will be useful to display download percentage
				// might be -1: server did not report the length
				int fileLength = connection.getContentLength();

				// download the file
				input = connection.getInputStream();
				output = new FileOutputStream("/sdcard/update.apk");

				byte data[] = new byte[4096];
				long total = 0;
				int count;
				while ((count = input.read(data)) != -1) {
					// allow canceling with back button
					if (isCancelled()) {
						input.close();
						return null;
					}
					total += count;
					// publishing the progress....
					if (fileLength > 0) // only if total length is known
						publishProgress((int) (total * 100 / fileLength));
					output.write(data, 0, count);
				}
			} catch (Exception e) {
				return e.toString();
			} finally {
				try {
					if (output != null)
						output.close();
					if (input != null)
						input.close();
				} catch (IOException ignored) {
				}

				if (connection != null)
					connection.disconnect();
			}
			return null;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			// take CPU lock to prevent CPU from going off if the user 
			// presses the power button during download
			PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
			mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
					getClass().getName());
			mWakeLock.acquire();
			//mProgressDialog.show();
		}

		@Override
		protected void onProgressUpdate(Integer... progress) {
			super.onProgressUpdate(progress);
			// if we get here, length is known, now set indeterminate to false
			//mProgressDialog.setIndeterminate(false);
			//mProgressDialog.setMax(100);
			//mProgressDialog.setProgress(progress[0]);
		}

		@Override
		protected void onPostExecute(String result) {
			mWakeLock.release();
			//mProgressDialog.dismiss();
			/*if (result != null)
	            Toast.makeText(context,"Download error: "+result, Toast.LENGTH_LONG).show();
	        else
	            Toast.makeText(context,"File downloaded", Toast.LENGTH_SHORT).show();*/
		}
	}

	public static boolean isWiFiActive(Context inContext) {   
		Context context = inContext.getApplicationContext();   
		ConnectivityManager connectivity = (ConnectivityManager) context  .getSystemService(Context.CONNECTIVITY_SERVICE);   
		if (connectivity != null) {   
			NetworkInfo[] info = connectivity.getAllNetworkInfo();   
			if (info != null) {   
				for (int i = 0; i < info.length; i++) {   
					if (info[i].getTypeName().equals("WIFI") && info[i].isConnected()) {   
						return true;   
					}   
				}   
			}
		}   
		return false;
	}   
}
