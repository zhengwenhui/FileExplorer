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
import android.graphics.drawable.Drawable;

/**
 * Represents a file information. An application is made of a name (or title),
 * an intent and an icon.
 */
class FileInfo {

	public File file;

	/**
	 * The application icon.
	 */
	public Drawable icon;

	/**
	 * 0: folder    1: file
	 */
	public boolean isDir;

	/**
	 * 用来标志是哪种类型的文件，只有当是文件是才有用到，文件夹时此标志无效
	 * */
	public String file_type;

	public boolean isRead;
	public boolean isWrite;

	public boolean isChoice;

	FileInfo() {
		isDir = true;
		file = null;
		icon = null;
		file_type = null;
		isChoice = false;
		isRead = true;
		isWrite = true;
	}

	public FileInfo(FileInfo info) {    	
		icon = info.icon;
		isDir = info.isDir;
		file = info.file;
		file_type = info.file_type;
		isChoice = info.isChoice;
		isRead = info.isRead;
		isWrite = info.isWrite;
	}

	public String toString() {
		return file.getPath();
	}
}
