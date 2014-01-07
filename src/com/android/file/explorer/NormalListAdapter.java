package com.android.file.explorer;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnGenericMotionListener;

import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import android.os.Environment;
import android.util.Log;

/**
 * GridView adapter to show the list of applications and shortcuts  GalleryAdapter
 */
/*	*/
public class NormalListAdapter extends ArrayAdapter<FileInfo>{
	private final String TAG = "NormalListAdapter";
	private final LayoutInflater mInflater;
	private ArrayList<FileInfo> textview;
	
	private Resources resources;
	public String flash_dir = RockExplorer.flash_dir;
        public String sdcard_dir = RockExplorer.sdcard_dir;
	public String usb_dir = RockExplorer.usb_dir;
	public NormalListAdapter(Context context, ArrayList<FileInfo> files) {
		super(context, 0, files);
		mInflater = LayoutInflater.from(context);
		textview = files;
		resources = context.getResources();
	}
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final FileInfo info = getItem(position);
		if (convertView == null) {
			//convertView = mInflater.inflate(R.layout.folder_listview_adapter, parent, false);
			convertView = mInflater.inflate(R.layout.normal_adapter, null);
		}   
		//-----------------------------
        final ImageView image = (ImageView) convertView.findViewById(R.id.nor_image);
		final TextView text = (TextView) convertView.findViewById(R.id.nor_text);
		final TextView text_right = (TextView) convertView.findViewById(R.id.nor_right_text);
        final TextView text_choice = (TextView) convertView.findViewById(R.id.nor_text_choice);
        image.setImageDrawable(info.icon);
        if(info.file.getPath().equals(flash_dir))
        	text.setText(resources.getString(R.string.str_flash_name));
        else if(info.file.getPath().equals(usb_dir))
        	text.setText(resources.getString(R.string.str_usb1_name));
        else if(info.file.getPath().equals(sdcard_dir))
        	text.setText(resources.getString(R.string.str_sdcard_name));
        else
        	text.setText(info.file.getName());
        //text_choice.setText(info.up_file_name);
        /**/
        if(info.isChoice){
        	if(info.file.getPath().equals(flash_dir))
        		text_choice.setText(resources.getString(R.string.str_flash_name));
        	else if(info.file.getPath().equals(usb_dir))
        		text_choice.setText(resources.getString(R.string.str_usb1_name));
            else if(info.file.getPath().equals(sdcard_dir))
            	text_choice.setText(resources.getString(R.string.str_sdcard_name));
            else
            	text_choice.setText(info.file.getName());
        }else{
        	text_choice.setText(null);
        }
        
        
        String temp_right = null;
        //如下为获取文件相应的大小
        if(info.isDir){
        	temp_right = new String(""); 
        }else{
        	long temp_size = info.file.length();
        	temp_right = new String("");
        	temp_right += change_Long_to_String(temp_size);
        }
        
        //如下为获取文件相应的日期
        temp_right += " | ";        
        long lastModified = info.file.lastModified();
        temp_right += change_long_to_time(lastModified);
        
        //如下为获取文件相应的一些属性，如文件的读写权限~
        temp_right += " | ";        
        temp_right += getAttribute(info.file);
        info.isWrite = info.file.canWrite();
        info.isRead = info.file.canRead();
        
        if(info.file.getPath().endsWith(sdcard_dir) || 
        	info.file.getPath().endsWith(flash_dir) ||
        	info.file.getPath().endsWith(usb_dir)){
        	text_right.setText("");
        }else{        	
        	text_right.setText(temp_right);
        }
        convertView.setOnGenericMotionListener(ogml);
		return convertView;
	}	
	
	/*
	 * 如下为将一个文件大小的Long型数据转换成K/M/G类型来表示，且只保留小数点后两位~
	 * */
	public String change_Long_to_String(long temp_change){
		String temp_str = new String("");
		DecimalFormat df = new DecimalFormat("########.00");	//取float的小数点后两位   
		//四舍五入   
		
		if(temp_change >= 1024){	//计算 K
			float i =  temp_change / 1024f;
			if(i >= 1024f){			//计算M
				float j = i / 1024f;
				if(j >= 1024f){		//计算G
					float k = j / 1024f;
                                        temp_str += df.format(k);
                                        temp_str += " G";
				}else{
					temp_str += df.format(j);
					temp_str += " M";
				}
			}else{
				temp_str += df.format(i);
				temp_str += " K";
			}
		}else{
			temp_str += temp_change;
			temp_str += " B";
		}
		return temp_str;
	}
	
	/*
	 * 如下为将一long型时间转换成时间方式表示
	 * */
	public String change_long_to_time(long temp_time){
		//java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("M/dd/yyyy hh:mm:ss a",java.util.Locale.US); 
		//java.util.Date d = sdf.parse("5/13/2003 10:31:37 AM");  
		//long dvalue=d.getTime();  
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");  
		String mDateTime1=formatter.format(temp_time); 
		return mDateTime1;
	}
	
	/*
	 * 如下为获取一个文件的读写信息，是否是目录、可读、可写 
	 * */
	public String getAttribute(File file){
		String temp_Attr = new String("");
		if(file.isDirectory()){
			temp_Attr += "d";
		}else{
			temp_Attr +="-";
		}
		
		if(file.canRead()){
			temp_Attr += "r";
		}else{
			temp_Attr +="-";
		}
		
		if(file.canWrite()){
			temp_Attr += "w";
		}else{
			temp_Attr +="-";
		}
		
		return temp_Attr;
	}
	
	
	
	OnGenericMotionListener ogml = new OnGenericMotionListener () {

		@Override
		public boolean onGenericMotion(View v, MotionEvent event) {
			// TODO Auto-generated method stub
			int what = event.getButtonState();
			switch (what) {
			/*case MotionEvent.ACTION_DOWN: // 鼠标悬浮在上方
				System.out.println("ACTION_DOWN");
				break;	
			case MotionEvent.BUTTON_PRIMARY: // 鼠标左键
				System.out.println("BUTTON_PRIMARY");
				break;	
			case MotionEvent.BUTTON_TERTIARY: // 鼠标中键
				System.out.println("BUTTON_TERTIARY");
				break;*/		
			case MotionEvent.BUTTON_SECONDARY: // 鼠标右键
				RockExplorer.isMouseRightClick = true;
				break;	
			}
			return false;
		}
		
	};
}
