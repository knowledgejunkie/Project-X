/*
 * @(#)RAWREAD.java - alias class definition, may be replaced by additional disk access
 *
 * Copyright (c) 2004 by dvb.matt, All Rights Reserved.
 * 
 * This file is part of X, a free Java based demux utility.
 * X is intended for educational purposes only, as a non-commercial test project.
 * It may not be used otherwise. Most parts are only experimental.
 *  
 *
 * This program is free software; you can redistribute it free of charge
 * and/or modify it under the terms of the GNU General Public License as published by
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

//package X
//DM24062004 081.7 int05 now required, but w/o additional access it must return 'false'

import java.io.*;
import java.util.*;

class RawRead
{
	public String GetLoadStatus()
	{
		return "";
	}

	public long getFileSize(String filename)
	{
		return -1L;
	}

	public long lastModified(String filename)
	{
		return 0L;
	}

	public void add_native_files(ArrayList arraylist)
	{
		return;
	}

	public boolean isAccessibleDisk(String sourcefile)
	{
		return false;
	}

	public boolean AccessEnabled()
	{
		return false;
	}

	// Constructor
	public RawRead()
	{}
}

