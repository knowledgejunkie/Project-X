/*
 * @(#)XInputDirectoryIF.java
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

/*
 * Created on 26.08.2004
 *
 */
package net.sourceforge.dvb.projectx.xinput;

/**
 * @author Stefan
 *  
 */
public interface XInputDirectoryIF {

	/**
	 * Get String representation of the object.
	 * 
	 * @return String representation of the object
	 */
	public String toString();

	/**
	 * Get path of directory
	 * 
	 * @return Path of directory
	 */
	public String getDirectory();

	/**
	 * Get password for the ftp server
	 * 
	 * @return Password for the ftp server
	 * @throws IllegalStateException
	 *           If file type of object is not FileType.FTP_DIR
	 */
	public String getPassword();

	/**
	 * Get name or ip address of the ftp server
	 * 
	 * @return Name or ip address of the ftp server
	 * @throws IllegalStateException
	 *           If file type of object is not FileType.FTP_DIR
	 */
	public String getServer();

	/**
	 * Get port of the ftp server
	 * 
	 * @return port of the ftp server
	 * @throws IllegalStateException
	 *           If file type of object is not FileType.FTP_DIR
	 */
	public String getPort();

	/**
	 * Get user for the ftp server
	 * 
	 * @return User for the ftp server
	 * @throws IllegalStateException
	 *           If file type of object is not FileType.FTP_DIR
	 */
	public String getUser();

	/**
	 * Get log of communication with ftp server.
	 * 
	 * @return Log of communication with ftp server
	 * @throws IllegalStateException
	 *           If file type of object is not FileType.FTP_DIR
	 */
	public String getLog();

	/**
	 * Get files in the directory.
	 * 
	 * @return files in the directory
	 */
	public XInputFile[] getFiles();

	/**
	 * Test if directory data is valid.
	 * 
	 * @return Test successful or not
	 */
	public boolean test();

	/**
	 * Get result message after test().
	 * 
	 * @return result message after test()
	 */
	public String getTestMsg();

	/**
	 * @return Type of XInputDirectory
	 */
	public DirType getDirType();
}