/*
 * @(#)RawFile.java - raw data extraction
 *
 * Copyright (c) 2005 by dvb.matt, All Rights Reserved.
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

package net.sourceforge.dvb.projectx.io;

import java.io.File;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import net.sourceforge.dvb.projectx.common.Common;
import net.sourceforge.dvb.projectx.common.Resource;

import net.sourceforge.dvb.projectx.io.IDDBufferedOutputStream;


/**
 * raw file from pva
 */
public class RawFile extends Object {

	IDDBufferedOutputStream out;
	String name;

	private RawFile()
	{}

	public RawFile(String new_name, int PidToExtract, int buffersize) throws IOException
	{
		name = new_name + "_0x" + Integer.toHexString(PidToExtract).toUpperCase() + ".raw";
		out = new IDDBufferedOutputStream( new FileOutputStream(name), buffersize);
	}

	public void write(byte[] data) throws IOException
	{
		out.write(data);
	}

	public void write(byte[] data, int offset, int len) throws IOException
	{
		out.write(data, offset, len);
	}

	public void close() throws IOException
	{ 
		out.flush();
		out.close();

		if (new File(name).length() < 10) 
			new File(name).delete();

		else 
			Common.setMessage(Resource.getString("msg.newfile") + " " + name);
	} 
}
