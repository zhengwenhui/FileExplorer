package com.android.file.explorer;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Parcelable;
import android.widget.Toast;
/**
 * 本类用于处理发送文件是过滤如  gmail 等应用
 */

public class ShareIntent {
	
	static String [] filterList = {"com.google.android.gm"};
	
	/**
	 * 设置要过滤的关键字，针对包名进行过滤
	 */
	public static void SetFilter(String [] mList){
		filterList = mList;
	}
			
	/**
    *	一次发送多个文件
    *	文件类型一般使用：x-mixmedia/*
    */	
	public static void shareMultFile(Context mcontext,String mFileType,ArrayList<Uri> uris){
		List<Intent> appList= getShareApps(mcontext,mFileType,Intent.ACTION_SEND_MULTIPLE);
		//System.out.println("appList:" + appList);
		if(appList !=null && appList.size()>0){
			for(Intent i : appList){
				i.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
			}
			Sending(mcontext, appList);
		} else {
			Toast.makeText(mcontext, mcontext.getString(R.string.no_apps_to_send),
					Toast.LENGTH_SHORT).show();
		}
	}
	
	/**
    *	一次发送一个文件，需要指文件类型
    *	 比如：image/* audio/* text/plain
    */	
	public static void shareFile(Context mcontext,String mFileType,Uri uri){
		List<Intent> appList= getShareApps(mcontext,mFileType,Intent.ACTION_SEND);
		//System.out.println("appList:" + appList);
		if(appList !=null && appList.size()>0){
			for(Intent i : appList){
				i.putExtra(Intent.EXTRA_STREAM, uri);
			}
			Sending(mcontext, appList);
		} else {
			Toast.makeText(mcontext, mcontext.getString(R.string.no_apps_to_send),
					Toast.LENGTH_SHORT).show();
		}
	} 
	
	/**
    *	统一的发送接口
    */
	private static void Sending(Context mcontext,List<Intent> appList){
		Intent chooserIntent =Intent.createChooser(appList.remove(0),mcontext.getText(R.string.edit_bluetooth));
        if(chooserIntent ==null){
            return ;
        }
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, appList.toArray(new Parcelable[]{}));
        try{
        	chooserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        	mcontext.startActivity(chooserIntent);
        }catch(android.content.ActivityNotFoundException ex){
            Toast.makeText(mcontext,"Can't find share component to share",Toast.LENGTH_SHORT).show();
        }
	}
	
    /**
    *	获取过滤后的app列表
    *	直接过滤包名
    */
	private static List<Intent> getShareApps(Context mcontext,String mFileType,String mSendIntentType){
	    Intent it =new Intent(mSendIntentType);
	    it.setType(mFileType);
	    List<ResolveInfo> sendActiviryInfo = mcontext.getPackageManager().queryIntentActivities(it,0);
	    
	    if(!sendActiviryInfo.isEmpty()){
	        List<Intent> targetedShareIntents =new ArrayList<Intent>();
	        for(ResolveInfo info : sendActiviryInfo){
	            Intent targeted =new Intent(mSendIntentType);
	            targeted.setType(mFileType);
	            ActivityInfo activityInfo = info.activityInfo;
	            //在此处过滤包名
	            if(containKeyword(activityInfo.packageName)){
	                continue;
	            }
	            targeted.setPackage(activityInfo.packageName);
	            targetedShareIntents.add(targeted);
	        }
	        return targetedShareIntents;
	    }
		return null;
	}
	
	/**
	 * 检查包名是否包含关键字
	 */
	private static boolean containKeyword(String packageName){
		for(String name : filterList){
			if(packageName.contains(name) && !("").equals(name)){
				return true;
			}
		}
		return false;
	}
}
