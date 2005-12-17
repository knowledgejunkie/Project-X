/*
 * @(#)XInputDirectoryImpl.java - implementation for topfield hd access
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

package net.sourceforge.dvb.projectx.xinput.topfield_raw;

import java.util.ArrayList;
import java.util.Iterator;

import net.sourceforge.dvb.projectx.xinput.DirType;
import net.sourceforge.dvb.projectx.xinput.XInputDirectoryIF;
import net.sourceforge.dvb.projectx.xinput.XInputFile;

public class XInputDirectoryImpl implements XInputDirectoryIF {

	private DirType dirType = null;

	private String testMsg = null;

	private RawInterface rawInterface = null;

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
	 * Create a XInputDirectory of type DirType.RAW_DIR.
	 * 
	 * @param aFtpVO
	 *          Directory data to use
	 */
	public XInputDirectoryImpl(DirType aDirType) {

		if (aDirType != DirType.RAW_DIR) { throw new IllegalArgumentException("aDirType is not DirType.RAW_DIR"); }

		dirType = aDirType;
		rawInterface = new RawInterface("");

		if (!rawInterface.rawRead.AccessEnabled()) { throw new IllegalArgumentException(
				"Topfield raw disk access is not enabled"); }
	}

	/**
	 * Get String representation of the object.
	 * 
	 * @return String representation of the object
	 */
	public String toString() {

		return "Topfield raw disk access";
	}

	/**
	 * Get path of directory
	 * 
	 * @return Path of directory
	 */
	public String getDirectory() {

		return "";
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

		ArrayList arrayList = new ArrayList();
		rawInterface.add_native_files(arrayList);

		XInputFile[] xInputFiles = new XInputFile[arrayList.size()];
		int i = 0;

		for (Iterator it = arrayList.iterator(); it.hasNext();) {
			xInputFiles[i++] = new XInputFile((String) it.next());
		}

		return xInputFiles;
	}

	/**
	 * Test if directory data is valid.
	 * 
	 * @return Test successful or not
	 */
	public boolean test() {

		boolean b = false;

		b = rawInterface.rawRead.AccessEnabled();
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