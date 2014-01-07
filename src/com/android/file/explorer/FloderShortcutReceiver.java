package com.android.file.explorer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class FloderShortcutReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		Log.i("onReceive","onReceive");
		RockExplorer.addFloderShortcut(context);
	}
}

