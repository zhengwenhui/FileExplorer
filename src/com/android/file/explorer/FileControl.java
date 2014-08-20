/*
 * Copyright (C) 2009 The Rockchip Android MID Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.file.explorer;
import java.io.File;
import java.util.ArrayList;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.widget.ListView;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import android.os.Environment;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;  
import java.lang.reflect.Method;  
import android.util.DisplayMetrics;
import android.content.pm.ApplicationInfo;


public class FileControl {	
	final static String TAG = "FileControl.java";
	final static boolean DEBUG = false;	//true;
	private static void LOG(String str)
	{
		if(DEBUG)
		{
			Log.d(TAG,str);
		}
	}
	public ArrayList<FileInfo> folder_array;	
	Resources resources;	
	Context context_by;
	ListView main_ListView;
	private String currently_parent = null;	
	public static String currently_path = null;

	public String flash_dir = RockExplorer.flash_dir;
	public String sdcard_dir = RockExplorer.sdcard_dir;
	public String usb_dir = RockExplorer.usb_dir;

	int currently_state;
	final int AZ_COMPOSITOR = 0;	/* 按名称排序 */
	final int TIME_COMPOSITOR = 1;	/* 按时间排序,不确定 */
	final int SIZE_COMPOSITOR = 2;	/* 按文件从小到大的顺序排序 */ 
	final int TYPE_COMPOSITOR = 3;	/* 按类型排序 */

	/* 下面添加音频文件的后缀，用来判断是否是音频文件，且各类文件的顺序也决定TYPE_COMPOSITOR中排序的位置 */
	String[] music_postfix = {".mp3", ".ogg", ".wma", ".wav", ".ape", 
			".mid", ".flac", ".mp3PRO", ".au", ".avi"};
	int size_postfix[] = new int[music_postfix.length];
	int pit_postfix[] = new int[music_postfix.length];
	ArrayList<FileInfo> type_compositor_file;

	public String str_audio_type = "audio/*";
	public String str_video_type = "video/*";
	public String str_image_type = "image/*";
	public String str_txt_type = "text/plain";
	public String str_pdf_type = "application/pdf";
	public String str_epub_type = "application/epub+zip";
	public String str_apk_type = "application/vnd.android.package-archive";

	static boolean is_enable_fill = true;
	static boolean is_finish_fill = true;

	static boolean is_first_path = true;	//用来标志是否是在第一个文件列表中（只包含Flash与sdcard）

	public static String str_last_path = null;
	public static int last_item;
	public static RockExplorer mRockExplorer;	

	public static boolean is_enable_del = true;
	public static boolean is_finish_del = true;	
	public static ArrayList<FileInfo> delFileInfo = new ArrayList<FileInfo>();

	public FileControl(Context context, String path, ListView tmp_main_listview) {

		//requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);     

		currently_parent = path;
		currently_path = path;
		currently_state = SIZE_COMPOSITOR;    		
		main_ListView = tmp_main_listview;
		resources = context.getResources();
		context_by = context;
		//File files = new File(currently_path);
		//fill(files.listFiles());
		//currently_parent = files.getParent();
		first_fill();

	}

	private void fill(File[] files) {
		is_first_path = false;
		folder_array = new ArrayList<FileInfo>();	
		LOG("in fill, is_enable_fill = " + is_enable_fill);
		LOG("in fill, files.length = " + files.length);
		for (File file : files){
			if(!is_enable_fill)
				break;
			if(file.canRead()){
				synchronized(folder_array){
					if(file.isDirectory()){		
						folder_array.add(0, changeFiletoFileInfo(file)); 
					}else{
						folder_array.add(changeFiletoFileInfo(file)); 
					}	
				}
			}
		}
		LOG("in fill, -- fill is over !!!!!!!!!!!!!!");
	}

	public void refill(String path){
		final File files = new File(path); 
		LOG("in the refill, path = " + path);
		is_enable_fill = true;		//使能此线程
		is_finish_fill = false;
		fill(files.listFiles());
		currently_path = new String(files.getPath());
		LOG("in the refill, ---- currently_path = " + currently_path);
		if(currently_path.equals(flash_dir) || 
				currently_path.equals(sdcard_dir) || currently_path.equals(usb_dir))
			currently_parent = null;
		else
			currently_parent = new String(files.getParent());

		is_finish_fill = true;
		LOG("in the refill, ---- end of refill()");
	}

	public void refillwithThread(String path){
		final File files = new File(path); 
		LOG("in the refill, path = " + path);
		is_enable_fill = true;		//使能此线程
		is_finish_fill = false;
		new Thread(){
			public void run(){
				LOG("in the refillwithThread, ----begin Thread.start()");
				fill(files.listFiles());
				currently_path = new String(files.getPath());
				LOG("in the refillwithThread, ---- currently_path = " + currently_path);
				if(currently_path.equals("/"))
					currently_parent = new String("/");
				else
					currently_parent = new String(files.getParent());

				is_finish_fill = true;
				LOG("in the refillwithThread, ----end Thread.start()");
			}
		}.start();
		LOG("in the refillwithThread, ---- after Thread.start()");
	}

	/**/
	public ArrayList<FileInfo> get_folder_array(){
		return folder_array;
	}
	public void set_folder_array(ArrayList<FileInfo> tmp_set_array){
		folder_array = tmp_set_array;
	}


	public String get_currently_parent(){
		return currently_parent;
	}

	public String get_currently_path(){
		return currently_path;
	}

	public void set_main_ListView(ListView tmp_listview){
		main_ListView = tmp_listview;
		if(main_ListView == null){
			LOG("in set_main_ListView,------------main_ListView = null");
		}
	}

	/*
	 * 重新刷主ListView
	 * */
	public void setMainAdapter(){
		LOG("in setMainAdapter,----11----folder_array size = " + folder_array.size());
		NormalListAdapter tempAdapter = new NormalListAdapter(context_by, folder_array);       	
		LOG("in setMainAdapter,----22----folder_array size = " + folder_array.size());
		main_ListView.setAdapter(tempAdapter);    	
	}

	/* 判断文件MimeType的方法 */
	public String getMIMEType(File f) 
	{ 
		String type="";
		String fName=f.getName();
		/* 取得扩展名 */
		String end=fName.substring(fName.lastIndexOf(".")+1,
				fName.length()).toLowerCase(); 

		/* 依附档名的类型决定MimeType */
		if(end.equalsIgnoreCase("mp3")||end.equalsIgnoreCase("wma")
				||end.equalsIgnoreCase("mp1")||end.equalsIgnoreCase("mp2")
				||end.equalsIgnoreCase("ogg")||end.equalsIgnoreCase("oga")
				||end.equalsIgnoreCase("flac")||end.equalsIgnoreCase("ape")
				||end.equalsIgnoreCase("wav")||end.equalsIgnoreCase("aac")
				||end.equalsIgnoreCase("m4a")||end.equalsIgnoreCase("m4r")
				||end.equalsIgnoreCase("amr")||end.equalsIgnoreCase("mid")
				||end.equalsIgnoreCase("asx")
				/*
         ||end.equalsIgnoreCase("mid")||end.equalsIgnoreCase("amr")
    	 ||end.equalsIgnoreCase("awb")||end.equalsIgnoreCase("midi")
    	 ||end.equalsIgnoreCase("xmf")||end.equalsIgnoreCase("rtttl")
    	 ||end.equalsIgnoreCase("smf")||end.equalsIgnoreCase("imy")
    	 ||end.equalsIgnoreCase("rtx")||end.equalsIgnoreCase("ota")*/)
		{
			type = str_audio_type; 
		}
		else if(end.equalsIgnoreCase("3gp")||end.equalsIgnoreCase("mp4")
				||end.equalsIgnoreCase("rmvb")||end.equalsIgnoreCase("3gpp")
				||end.equalsIgnoreCase("avi")||end.equalsIgnoreCase("rm")
				||end.equalsIgnoreCase("mov")||end.equalsIgnoreCase("flv")
				||end.equalsIgnoreCase("mkv")||end.equalsIgnoreCase("wmv")
				||end.equalsIgnoreCase("divx")||end.equalsIgnoreCase("bob")
				||end.equalsIgnoreCase("mpg")||end.equalsIgnoreCase("dat")
				||end.equalsIgnoreCase("vob")||end.equalsIgnoreCase("asf"))
		{
			type = str_video_type;
			if(end.equalsIgnoreCase("3gpp")){
				if(isVideoFile(f)){
					type = str_video_type;
				}else{
					type = str_audio_type; 
				}
			}
		}
		else if(end.equalsIgnoreCase("jpg")||end.equalsIgnoreCase("gif")
				||end.equalsIgnoreCase("png")||end.equalsIgnoreCase("jpeg")
				||end.equalsIgnoreCase("bmp"))
		{
			type = str_image_type;
		}
		else if(end.equalsIgnoreCase("txt"))
		{
			type = str_txt_type;
		}
		else if(end.equalsIgnoreCase("epub") || end.equalsIgnoreCase("pdb") || end.equalsIgnoreCase("fb2") || end.equalsIgnoreCase("rtf"))
		{
			type = str_epub_type;
		}
		else if(end.equalsIgnoreCase("pdf"))
		{
			type = str_pdf_type;
		}
		else if(end.equalsIgnoreCase("apk"))
		{
			type = str_apk_type;  
		}
		else
		{
			/* 如果无法直接打开，就跳出软件列表给用户选择 */
			type="*/*";
		}

		return type; 
	}

	/*
	 * 删除ArrayList<FileInfo> file_paths，此操作有创建个线程
	 * */
	public static void delFileInfo(final ArrayList<FileInfo> file_paths){
		is_enable_del = true;
		is_finish_del = false;
		new Thread(){
			public void run(){
				for(int file_num = 0; file_num < file_paths.size(); file_num ++){
					boolean del_successful = true;
					/*if(file_paths.get(file_num).isWrite){
	    				if(file_paths.get(file_num).file.isDirectory()){
    						del_successful = delDir(file_paths.get(file_num).file);
    					}else{
    						if(!file_paths.get(file_num).file.delete()){
							Log.e(TAG, "  ------- :   Delete file " + file_paths.get(file_num).file.getPath() + " fail~~");
						}
						if(!file_paths.get(file_num).isWrite)
							del_successful = false;
    					}
				}*/

					if(file_paths.get(file_num).file.isDirectory()){
						del_successful = delDir(file_paths.get(file_num).file);
					}else{
						if(!file_paths.get(file_num).file.delete()){
							Log.e(TAG, "  ------- :   Delete file " + file_paths.get(file_num).file.getPath() + " fail~~");
						}
					}

					if(del_successful)
						delFileInfo.add(file_paths.get(file_num));
					if(!is_enable_del){
						is_finish_del = true;
						return;
					}
				}
				is_finish_del = true;
			}
		}.start();	//启动删除文件的线程
	}
	/*
	 * 用于删除文件夹
	 * */
	public static boolean delDir(File dir){
		boolean ret = true;
		if(!is_enable_del)
			return false;
		File[] file = dir.listFiles();
		for (int i = 0; i < file.length; i++) {
			if (file[i].isFile()){
				if(!is_enable_del)
					return false;
				if(!file[i].delete()){
					Log.e(TAG, "  ------- :    Delete file " + file[i].getPath() + " fail~~");
					ret = false;
				}
			}else{
				if(!is_enable_del)
					return false;
				delDir(file[i]);
			}
		}
		dir.delete();
		return ret;
	}

	/*
	 * 用于删除中断时未拷贝完的文件
	 * */
	public static void DelFile(File mInterruptFile){
		if(mInterruptFile == null)
			return;
		LOG(" ____________ DelFile() " + mInterruptFile.getPath() + "   currently_path = " + currently_path);
		File tmp_file = null;
		if((tmp_file = new File(currently_path+File.separator+mInterruptFile.getName())).exists()){
			tmp_file.delete();
		}
	}

	/*
	 * 将一个File转换成其相应的FileInfo类
	 * */
	public FileInfo changeFiletoFileInfo(File file){
		FileInfo temp = new FileInfo();
		temp.file = file;
		//temp.musicType = isMusicFile(temp.name);
		if(file.isDirectory()){		/* 若是文件夹 */
			temp.icon = resources.getDrawable(R.drawable.folder);
			temp.isDir = true;
		}	
		else {	/* 若是文件 */
			temp.isDir = false;
			/* 调用getMIMEType()来取得MimeType */
			temp.file_type = getMIMEType(file);
			if(temp.file_type.equals(str_apk_type)){
				Drawable tmpicon = getUninstallAPKIcon(temp.file.getPath());
				if(tmpicon == null){
					tmpicon = getDrawable(temp.file_type);
				}
				temp.icon = tmpicon;
			}else{	
				temp.icon = getDrawable(temp.file_type);
			}
		}
		return temp;
	}

	private Drawable getUninstallAPKIcon(String apkPath) {
		Drawable icon = null;
		String PATH_PackageParser = "android.content.pm.PackageParser";
		String PATH_AssetManager = "android.content.res.AssetManager";
		try {
			Class pkgParserCls = Class.forName(PATH_PackageParser);
			Class[] typeArgs = new Class[1]; 
			typeArgs[0] = String.class;
			Constructor pkgParserCt = pkgParserCls.getConstructor(typeArgs);
			Object[] valueArgs = new Object[1];
			valueArgs[0] = apkPath;
			Object pkgParser = pkgParserCt.newInstance(valueArgs);
			//		Log.d("ANDROID_LAB", "pkgParser:" + pkgParser.toString());
			DisplayMetrics metrics = new DisplayMetrics();
			metrics.setToDefaults();
			// PackageParser.Package mPkgInfo = packageParser.parsePackage(new
			// File(apkPath), apkPath,
			// metrics, 0);
			typeArgs = new Class[4];
			typeArgs[0] = File.class;
			typeArgs[1] = String.class;
			typeArgs[2] = DisplayMetrics.class;
			typeArgs[3] = Integer.TYPE;
			Method pkgParser_parsePackageMtd = pkgParserCls.getDeclaredMethod("parsePackage",typeArgs);
			valueArgs = new Object[4];
			valueArgs[0] = new File(apkPath);
			valueArgs[1] = apkPath;
			valueArgs[2] = metrics;
			valueArgs[3] = 0;
			Object pkgParserPkg = pkgParser_parsePackageMtd.invoke(pkgParser, valueArgs);
			// ApplicationInfo info = mPkgInfo.applicationInfo;
			if(pkgParserPkg == null)
				return null;
			Field appInfoFld = pkgParserPkg.getClass().getDeclaredField("applicationInfo");
			ApplicationInfo info = (ApplicationInfo) appInfoFld.get(pkgParserPkg);
			//		Log.d("ANDROID_LAB", "pkg:" + info.packageName + " uid=" + info.uid);
			// Resources pRes = getResources();
			// AssetManager assmgr = new AssetManager();
			// assmgr.addAssetPath(apkPath);
			// Resources res = new Resources(assmgr, pRes.getDisplayMetrics(),
			// pRes.getConfiguration());
			Class assetMagCls = Class.forName(PATH_AssetManager);
			Constructor assetMagCt = assetMagCls.getConstructor((Class[]) null);
			Object assetMag = assetMagCt.newInstance((Object[]) null);
			typeArgs = new Class[1];
			typeArgs[0] = String.class;
			Method assetMag_addAssetPathMtd = assetMagCls.getDeclaredMethod("addAssetPath",typeArgs);
			valueArgs = new Object[1];
			valueArgs[0] = apkPath;
			assetMag_addAssetPathMtd.invoke(assetMag, valueArgs);
			Resources res = resources;
			typeArgs = new Class[3];
			typeArgs[0] = assetMag.getClass();
			typeArgs[1] = res.getDisplayMetrics().getClass();
			typeArgs[2] = res.getConfiguration().getClass();
			Constructor resCt = Resources.class.getConstructor(typeArgs);
			valueArgs = new Object[3];
			valueArgs[0] = assetMag;
			valueArgs[1] = res.getDisplayMetrics();
			valueArgs[2] = res.getConfiguration();
			res = (Resources) resCt.newInstance(valueArgs);
			CharSequence label = null;
			if (info.labelRes != 0) {
				label = res.getText(info.labelRes);
			}
			// if (label == null) {
			// label = (info.nonLocalizedLabel != null) ? info.nonLocalizedLabel
			// : info.packageName;
			// }
			//		Log.d("ANDROID_LAB", " ----------> label=" + label);
			if (info.icon != 0) {
				icon = res.getDrawable(info.icon);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return icon;
	}

	/*
	 * 通过文件类型获取相应的图标的资源ID，桌面快捷方式要用到
	 * */
	public int getDrawableId(String tmp_type){
		int drawableId = -1;
		if(tmp_type.equals(str_audio_type)){
			drawableId = R.drawable.audio;
		}else if(tmp_type.equals(str_video_type)){
			drawableId = R.drawable.video;
		}else if(tmp_type.equals(str_image_type)){
			drawableId = R.drawable.image;
		}else if(tmp_type.equals(str_txt_type) || tmp_type.equals(str_pdf_type) || tmp_type.equals(str_epub_type)){
			drawableId = R.drawable.storage_list;
		}else if(tmp_type.equals(str_apk_type)){
			drawableId = R.drawable.apk;
		}else {
			drawableId = R.drawable.blank_page;
		}
		return drawableId;
	}

	/*
	 * 通过文件类型获取相应的图标
	 * */
	public Drawable getDrawable(String tmp_type){
		Drawable d = null;
		if(tmp_type.equals(str_audio_type)){
			d = resources.getDrawable(R.drawable.audio);
		}else if(tmp_type.equals(str_video_type)){
			d = resources.getDrawable(R.drawable.video);
		}else if(tmp_type.equals(str_image_type)){
			d = resources.getDrawable(R.drawable.image);
		}else if(tmp_type.equals(str_txt_type) || tmp_type.equals(str_pdf_type) || tmp_type.equals(str_epub_type)){
			d = resources.getDrawable(R.drawable.storage_list);
		}else if(tmp_type.equals(str_apk_type)){
			d = resources.getDrawable(R.drawable.apk);
		}else {
			d = resources.getDrawable(R.drawable.blank_page);
		}
		return d;
	}

	/*
	 * 用来判断文件是否存在于当前的folder_array中
	 * */
	public boolean is_contain_file(File cFile){
		for(int i = 0; i < folder_array.size(); i ++){
			if(folder_array.get(i).file.getPath().equals(cFile.getPath())){
				return true;
			}
		}
		return false;
	}

	/* 第一次进入时的Adapter填充 */
	void first_fill(){
		is_first_path = true;
		folder_array = new ArrayList<FileInfo>();
		mRockExplorer = (RockExplorer)context_by;
		//加入SDcard目录
		if(str_last_path != null && (new File(str_last_path)).exists() && (new File(str_last_path)).listFiles() != null
				&& ((str_last_path.startsWith(flash_dir) && mRockExplorer.isMountFLASH())
						|| (str_last_path.startsWith(sdcard_dir) && mRockExplorer.isMountSD())
						|| (str_last_path.startsWith(usb_dir) && mRockExplorer.isMountUSB1()))){
			if(str_last_path.startsWith(flash_dir) || str_last_path.startsWith(sdcard_dir)){	
				//			mRockExplorer.enableButton(true);			
			}
			refill(str_last_path);
			return;
		}
		FileInfo SDcard = changeFiletoFileInfo(new File(sdcard_dir));
		SDcard.icon = resources.getDrawable(R.drawable.sdcard);
		folder_array.add(SDcard);

		//加入Flash目录
		if(new File(flash_dir).exists()){
			FileInfo SYSpath = changeFiletoFileInfo(new File(flash_dir));
			SYSpath.icon = resources.getDrawable(R.drawable.flash);
			folder_array.add(SYSpath);
		}

		//加入USB目录

		if(new File(usb_dir).exists()){
			FileInfo USBpath = changeFiletoFileInfo(new File(usb_dir));
			USBpath.icon = resources.getDrawable(R.drawable.flash);
			folder_array.add(USBpath);
		}

		currently_path = null;
		currently_parent = null;
	}

	public boolean isVideoFile(File tmp_file){
		String path = tmp_file.getPath();
		ContentResolver resolver = context_by.getContentResolver();
		String[] audiocols = new String[] {
				MediaStore.Video.Media._ID,
				MediaStore.Video.Media.DATA,
				MediaStore.Video.Media.TITLE
		};  
		LOG("in getFileUri --- path = " + path);
		StringBuilder where = new StringBuilder();
		where.append(MediaStore.Video.Media.DATA + "=" + "'" + path + "'");
		Cursor cur = resolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
				audiocols,
				where.toString(), null, null);
		if(cur.moveToFirst()){
			return true;
		}
		return false;
	}
}
