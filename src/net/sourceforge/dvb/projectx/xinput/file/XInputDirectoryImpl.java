/*
 * @(#)XInputDirectoryImpl.java - implementation for files
 *
 * Copyright (c) 2004-2005 by roehrist, All Rights Reserved. 
 * 
 * This file is part of ProjectX, a free Java based demux utility.
 * By the authors, ProjectX is intended for educational purposes only, 
 * as a non-commercial test project.
 * 
 *
 * This program is free software; you can redistribute it and/or modify 
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */

package net.sourceforge.dvb.projectx.xinput.file;

import java.io.File;
import java.io.FileFilter;

import java.util.ArrayList;

import net.sourceforge.dvb.projectx.xinput.DirType;
import net.sourceforge.dvb.projectx.xinput.XInputDirectoryIF;
import net.sourceforge.dvb.projectx.xinput.XInputFile;

import net.sourceforge.dvb.projectx.common.Common;
import net.sourceforge.dvb.projectx.common.Keys;

public class XInputDirectoryImpl implements XInputDirectoryIF {

	private DirType dirType = null;

	private File file = null;

	private String testMsg = null;

	/**
	 * Mustn't be used
	 * 
	 * @throws UnsupportedOperationException
	 *           Because it mustn't be used!
	 */
	private XInputDirectoryImpl() {
		throw new UnsupportedOperationException("Usage is not allowed!");
	}

	/**
	 * Create a XInputDirectory of type DirType.FILE_DIR.
	 * 
	 * @param aFile
	 *          Directory data to use
	 * @throws IllegalArgumentException
	 *           If aFile is not a directory
	 */
	public XInputDirectoryImpl(File aFile) {
		if (aFile.exists() && aFile.isDirectory()) {
			dirType = DirType.FILE_DIR;
			file = aFile;
		} else {
			throw new IllegalArgumentException("aFile is not a directory!");
		}
	}

	/**
	 * Create a XInputDirectory of type DirType.FILE_DIR.
	 * 
	 * @param aFileIdentifier
	 *          Directory name
	 * @throws IllegalArgumentException
	 *           If aFile is not a directory
	 */
	public XInputDirectoryImpl(String aFileIdentifier) {

		File f = new File(aFileIdentifier);
		if (f.exists() && f.isDirectory()) {
			dirType = DirType.FILE_DIR;
			file = f;
		} else {
			throw new IllegalArgumentException("'" + aFileIdentifier + "' is not a directory!");
		}
	}

	/**
	 * Get String representation of the object.
	 * 
	 * @return String representation of the object
	 */
	public String toString() {

		return file.getAbsolutePath();
	}

	/**
	 * Get path of directory
	 * 
	 * @return Path of directory
	 */
	public String getDirectory() {

		return file.getParent();
	}

	/**
	 * Get password for the ftp server
	 * 
	 * @return Password for the ftp server
	 * @throws IllegalStateException
	 *           If file type of object is not DirType.FTP_DIR
	 */
	public String getPassword() {

		return "";
	}

	/**
	 * Get name or ip address of the ftp server
	 * 
	 * @return Name or ip address of the ftp server
	 * @throws IllegalStateException
	 *           If file type of object is not DirType.FTP_DIR
	 */
	public String getServer() {

		return "";
	}

	/**
	 * Get port of the ftp server
	 * 
	 * @return port of the ftp server
	 * @throws IllegalStateException
	 *           If file type of object is not DirType.FTP_DIR
	 */
	public String getPort() {

		return "";
	}

	/**
	 * Get user for the ftp server
	 * 
	 * @return User for the ftp server
	 * @throws IllegalStateException
	 *           If file type of object is not DirType.FTP_DIR
	 */
	public String getUser() {

		return "";
	}

	/**
	 * Get log of communication with ftp server.
	 * 
	 * @return Log of communication with ftp server
	 * @throws IllegalStateException
	 *           If file type of object is not DirType.FTP_DIR
	 */
	public String getLog() {

		return "";
	}

	/**
	 * Get files in the directory.
	 * 
	 * @return files in the directory
	 */
	public XInputFile[] getFiles() {

		ArrayList list = new ArrayList();

		class MyFileFilter implements FileFilter {

			public boolean accept(File pathname) {
				return pathname.isFile();
			}
		}

		File[] files = file.listFiles(new MyFileFilter());

		for (int i = 0; i < files.length; i++)
			list.add(files[i]);

		if (Common.getSettings().getBooleanProperty(Keys.KEY_InputDirectoriesDepth))
			getDirectories(file, list);  // depth 1

		XInputFile[] xInputFiles = new XInputFile[list.size()];

		for (int i = 0; i < xInputFiles.length; i++) {
			xInputFiles[i] = new XInputFile((File) list.get(i));
		}

		return xInputFiles;
	}

	/**
	 * 
	 */
	private void getDirectories(File dir, ArrayList list)
	{
		class MyFileFilter implements FileFilter {

			public boolean accept(File pathname)
			{
				return pathname.isFile();
			}
		}

		class MyDirFilter implements FileFilter {

			public boolean accept(File pathname)
			{
				return pathname.isDirectory();
			}
		}

		File[] dirs = dir.listFiles(new MyDirFilter());
		File[] files;

		for (int i = 0; i < dirs.length; i++)
		{
			files = dirs[i].listFiles(new MyFileFilter());

			for (int j = 0; j < files.length; j++)
				list.add(files[j]);

			getDirectories(dirs[i], list); // depth 2
		}
	}



	/**
	 * Test if directory data is valid.
	 * 
	 * @return Test successful or not
	 */
	public boolean test() {

		boolean b = false;

		b = (file.exists() && file.isDirectory()) ? true : false;
		testMsg = b ? "Test succeeded" : "Test failed";

		return b;
	}

	/**
	 * Get result message after test().
	 * 
	 * @return result message after test()
	 */
	public String getTestMsg() {

		return testMsg;
	}

	/**
	 * @return Type of XInputDirectory
	 */
	public DirType getDirType() {
		return dirType;
	}

}