package com.android.file.explorer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import android.content.Context;
import android.os.StatFs;
import android.util.Log;
import android.widget.TextView;
/** 
 * 复制文件夹或文件夹 
 */ 
public class CopyFileUtils { 
	final String TAG = "CopyFileUtils.java";
	final boolean DEBUG = false;
	private void LOG(String str)
	{
		if(DEBUG)
		{
			Log.d(TAG,str);
		}
	}

	public String flash_dir = RockExplorer.flash_dir;
	public String sdcard_dir = RockExplorer.sdcard_dir;
	public String usb_dir = RockExplorer.usb_dir;

	private final int BUFFERE_SIZE = 1024*4;

	static boolean is_not_free_space = false;	//add by cong, 2010-05-28
	static boolean is_copy_finish = false;				//用来标志是否拷贝完毕的标志	
	static boolean is_enable_copy = true;				//是否可以复制的标记，用来强制结束拷贝
	static boolean is_same_path = false;				//标志拷贝时的源路径与目标路径是否一致
	static File mInterruptFile = null;					//拷贝中断时的文件路径

	static boolean mIsHighSpeed = true;  //29xx上为了性能考虑所加的变量

	public static ArrayList<FileInfo> has_copy_path = null;	//保存已拷贝的文件路径

	public TextView source_text = null;
	public TextView target_text = null;

	static File cope_now_sourceFile = null;
	static File cope_now_targetFile = null;
	static int mhascopyfilecount = 0;			//保存已拷贝的文件个数
	static int mallcopyfilecount = 0;			//保存要拷贝的文件总个数

	static float mhascopyfileLength = 0;
	static float mallcopyfileLength = 0;

	/*
	 * 下面判断是否复盖的操作过程如下：
	 * 		1、通过mRecoverFile是否为空来判断目标文件已存在，若存在则在主线程当中弹出是否复盖的对话框，
	 * 		        且将拷贝的进程通过标志is_wait_choice_recover与while语句结合进行阻塞
	 * 		2、若在主线程中的是否复盖对话框中选择后，则将is_wait_choice_recover值为false，恢复拷贝的进程。
	 * 		  	若选择了复盖，则将  is_recover值为true，若选择不复盖则将  is_recover值为false
	 * */
	static boolean is_wait_choice_recover = true;	//用来同步是否复盖对话框的标志
	static boolean is_recover = true;				//用来标志是否要复盖已存在文件
	static File mRecoverFile = null;				//当前是否要复盖的文件

	static ArrayList<FileInfo> multi_path = null;
	static boolean is_pause_copy = false;	
	static FileControl mFileControl = null;
	private Context mContext;
	public CopyFileUtils(Context context){
		this.mContext = context;
	}

	// 复制文件 
	public void CopyFile(File sourceFile,File targetFile)  throws IOException
	{ 
		if(sourceFile.getPath().equals(targetFile.getPath())){
			is_same_path = true;
			return;
		}
		if(!ishavefreespace(sourceFile, targetFile)){
			is_not_free_space = true;
			is_enable_copy = false;
			return;
		}
		is_recover = true;
		mRecoverFile = null;
		if(targetFile.exists()){	//若要拷贝的文件在目标目录下已存在
			mRecoverFile = targetFile;
			is_wait_choice_recover = true;
			while(is_wait_choice_recover || is_pause_copy){
				try{
					mThread.sleep(1000);
				}catch(java.lang.InterruptedException e){
				}
			}
			mRecoverFile = null;
		}

		cope_now_sourceFile = sourceFile;
		cope_now_targetFile = targetFile;

		LOG(" ---- cope_now_sourceFile = " + cope_now_sourceFile.toString() + "    cope_now_targetFile = " + cope_now_targetFile.toString());
		float tmpmhascopyfileLength = mhascopyfileLength;
		if(is_recover){		
			// 若复盖则进行拷贝
			// 新建文件输入流并对它进行缓冲 
			LOG(" _____ before FileInputStream");
			FileInputStream input = new FileInputStream(sourceFile); 
			BufferedInputStream inBuff=new BufferedInputStream(input); 

			// 新建文件输出流并对它进行缓冲 
			LOG(" _____ before FileOutputStream");
			FileOutputStream output = new FileOutputStream(targetFile);
			LOG(" _____ before BufferedOutputStream"); 
			BufferedOutputStream outBuff=new BufferedOutputStream(output); 
			// 缓冲数组
			LOG(" _____ before new byte"); 
			byte[] b = new byte[BUFFERE_SIZE]; 
			int len; 
			LOG(" _____ before copy while");
			while ((len =inBuff.read(b)) != -1)  { 
				while(is_pause_copy){
					LOG("+++++++++++++++is_pause_copy == true");
					try{
						mThread.sleep(1000);
					}catch(java.lang.InterruptedException e){
					}
				}	
				//29xx为了性能考虑加的--拷贝休眠
				if(false == mIsHighSpeed) {
					try {
						mThread.sleep(1);
					} catch (java.lang.InterruptedException e) {}
				} 
				if(is_enable_copy){
					outBuff.write(b, 0, len);
				}else{		
					//若被中断则结束拷贝     

					LOG("+++++++++++++++is_enable_copy == false");   		
					break;
				}
				mhascopyfileLength = tmpmhascopyfileLength + targetFile.length();
			}
			LOG(" _____ after  copy while"); 
			// 刷新此缓冲的输出流 
			outBuff.flush(); 
			// 发送sync广播，通知设备把拷贝的内容写入外接设备中
			FileDescriptor fd = output.getFD();
			fd.sync();

			/**
			 * 通过下面这样确保拷贝的内容已经写入外接设备，不友好，会影响性能，就注释掉了。	

	long size = cope_now_sourceFile.length()/1024/1024;
	try {
	      if (size > 100) {
					System.out.println("sleep(2000)");
					Thread.sleep(3000);
				} else if (size > 50) {
					System.out.println("sleep(500)");
					Thread.sleep(500);
				} else if (size > 10) {
					System.out.println("sleep(200)");
					Thread.sleep(200);
				} else if (size > 1) {
					Thread.sleep(20);
					System.out.println("sleep(20)");
				}
	} catch (InterruptedException e) {
		e.printStackTrace();
	}
			 */

			//关闭流 
			inBuff.close(); 
			outBuff.close(); 
			output.close(); 
			input.close();
		} 
		mhascopyfileLength = tmpmhascopyfileLength + sourceFile.length();
		if(is_enable_copy)			//表示该次拷贝未被强制中断，为一次完整拷贝文件
			mhascopyfilecount ++;      
	}

	// 复制文件夹 
	public void CopyDirectiory(String sourceDir, String targetDir) throws IOException 
	{    
		is_not_free_space = false; 
		if(sourceDir.equals(targetDir)){	//若源路径与目标路径一致
			is_same_path = true;
			return;
		}
		if(!(new File(targetDir)).exists())//目标文件不存在，新建目标目录 
		{      
			(new File(targetDir)).mkdirs(); 
		}
		// 获取源文件夹当前下的文件或目录 
		File[] file = (new File(sourceDir)).listFiles(); 
		for (int i = 0; i < file.length; i++) 
		{ 
			if (file[i].isFile()) 
			{ 
				// 源文件 
				File sourceFile=file[i]; 
				// 目标文件 
				File targetFile=new File(new File(targetDir).getAbsolutePath()+File.separator+file[i].getName());
				//if(!(targetFile=new File(new File(targetDir).getAbsolutePath()+File.separator+file[i].getName())).exists())
				CopyFile(sourceFile,targetFile); 
			} 
			if(!is_enable_copy){	//若有中断拷贝，则跳出拷贝
				break;
			}
			if (file[i].isDirectory()) 
			{ 
				// 准备复制的源文件夹 
				String dir1=sourceDir + "/" + file[i].getName(); 
				// 准备复制的目标文件夹 
				String dir2=targetDir + "/"+ file[i].getName(); 
				CopyDirectiory(dir1, dir2); 
			} 
		} 
		if(!is_enable_copy){		//若有中断拷贝，则跳出拷贝，且保存未拷贝完的文件夹
			mInterruptFile = (new File(targetDir));
			LOG("CopyFileInfoArray--dir-- mInterruptFile = " + mInterruptFile.getPath());
		}
	} 

	private Thread mThread = null;    
	/*
	 * 如下为复制multi_path里的所有文件到相应的目标目录targetDir
	 * */
	public void CopyFileInfoArray(final String targetDir){
		LOG("CopyFileInfoArray~~~~111~");
		mThread = new Thread(){
			public void run(){
				LOG("CopyFileInfoArray~~~~222~");
				is_copy_finish = false;
				is_enable_copy = true;
				is_pause_copy = false;
				has_copy_path = new ArrayList<FileInfo>();	//清空已拷贝文件路径
				mhascopyfilecount = 0;						//保存已拷贝的文件个数
				mallcopyfilecount = getfilenum(multi_path);	//计算出要拷贝的文件个数
				mhascopyfileLength = 0;
				mallcopyfileLength = getFileLeng(multi_path);
				for(int i = 0; i < multi_path.size(); i ++){
					if(multi_path.get(i).file.isDirectory()){		//拷贝文件夹
						try{
							CopyDirectiory(multi_path.get(i).file.getPath(), 
									new File(targetDir).getAbsolutePath()+File.separator+multi_path.get(i).file.getName());
						}catch(IOException e){
							Log.e(TAG,"CopyDirectiory error!");
							e.printStackTrace();
						}
					}else{											//拷贝文件
						try{
							File targetFile=new File(new File(targetDir).getAbsolutePath()+File.separator+multi_path.get(i).file.getName()); 
							CopyFile(multi_path.get(i).file, targetFile);
							if(!is_enable_copy){	//若中断拷贝，保存未拷贝完的文件
								mInterruptFile = targetFile;
								LOG("CopyFileInfoArray---file- mInterruptFile = " + mInterruptFile.getPath());
							}
						}catch(IOException e){
							Log.e(TAG,"CopyDirectiory error!");
						} 
					}
					LOG("CopyFileInfoArray----" + multi_path.get(i).file.getPath());
					LOG("CopyFileInfoArray---- is_enable_copy = " + is_enable_copy);
					if(is_enable_copy){		//将已拷贝的文件加入已拷贝链中
						has_copy_path.add(multi_path.get(i));
					}else{
						break;
					}
				}
				if(is_enable_copy){			//若完整的拷贝完了所有要拷贝的文件
					mInterruptFile = null;	//标志没有中断的文件
				}else{
					mFileControl.DelFile(mInterruptFile);
					mInterruptFile = null;
				}
				is_copy_finish = true;		//标志已经拷贝完毕
				LOG(" CopyFileInfoArray~~~~333~");
			}
		};
		mThread.start();
	}

	private long getFileLeng(ArrayList<FileInfo> _multi_path){
		long num = 0;
		for(int i = 0; i < _multi_path.size(); i ++){
			if(_multi_path.get(i).file.isDirectory()){
				long tmp = getDirfileLength(_multi_path.get(i).file);
				num = num + tmp;
			}else{
				num += _multi_path.get(i).file.length();
			}
		}
		return num;
	}   

	private long getDirfileLength(File dir){
		File[] file = dir.listFiles();
		long tmp_num = 0;
		for (int i = 0; i < file.length; i++) {
			if (file[i].isFile())
				tmp_num += file[i].length();
			else
				tmp_num = tmp_num + getDirfileLength(file[i]);
		}
		return tmp_num;
	}

	/*    public ArrayList<FileInfo> get_has_copy_path(){
    	return has_copy_path;
    }*/

	/*
	 * 计算出一个ArrayList<FileInfo> multi_path里面有多少个文件
	 * */
	public int getfilenum(ArrayList<FileInfo> _multi_path){
		int num = 0;
		for(int i = 0; i < _multi_path.size(); i ++){
			if(_multi_path.get(i).file.isDirectory()){
				num = num + getDirfilenum(_multi_path.get(i).file);
			}else{
				num ++;
			}
		}
		return num;
	}
	/*
	 * 获取一个文件夹下的文件个数
	 * */
	public int getDirfilenum(File dir){
		File[] file = dir.listFiles();
		int tmp_num = 0;
		for (int i = 0; i < file.length; i++) {
			if (file[i].isFile()) 
				tmp_num ++;
			else
				tmp_num = tmp_num + getDirfilenum(file[i]);
		}
		return tmp_num;
	}
	public boolean ishavefreespace(File sourcefile, File targetfile){
		boolean tmp_re = false;

		File path_flash = new File(flash_dir);
		File path_sdcard = new File(sdcard_dir);
		File path_usb = new File(usb_dir);
		File path = path_flash;
		if(targetfile.getPath().startsWith(path_flash.getPath())){
			path = path_flash;
		}
		if(targetfile.getPath().startsWith(path_sdcard.getPath())){
			path = path_sdcard;
		}else if(targetfile.getPath().startsWith(path_usb.getPath())){
			path = path_usb;
		}
		StatFs stat = new StatFs(path.getPath());
		long blockSize = stat.getBlockSize();
		long availableBlocks = stat.getAvailableBlocks();
		long space = availableBlocks * blockSize;
		LOG("--- in ishavemorespace(), blockSize = " + blockSize + ",  availableBlocks = " + availableBlocks 
				+ ",  space = " + space);
		tmp_re = space > sourcefile.length();
		return tmp_re;
	}
} 




