/*
 * @(#)RawREadIF.java - Interface for RawRead to load from the default package
 * via reflection
 * 
 * Copyright (c) 2004-2005 by pstorch, All Rights Reserved.
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
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 *  
 */

package net.sourceforge.dvb.projectx.xinput.topfield_raw;

import java.util.ArrayList;

public interface RawReadIF {

	// check DLL load status and errors
	public String GetLoadStatus();

	// directory read functions
	public int findOpen(String directoryname);

	public String findNextFile(int findhandle);

	public int findClose(int findhandle);

	// file read functions
	public int openFile(String filename);

	public int readFile(int filehandle, byte[] array, int offsetinbuffer, int readlen);

	public int closeFile(int filehandle);

	public long skipBytes(int filehandle, long skipbytes);

	public long getFileSize(String filename);

	public long lastModified(String filename);

	public void add_native_files(ArrayList arraylist);

	public boolean AccessEnabled();

}