/*
 * @(#)RawInterface.java - provides a little interface
 * 
 * Copyright (c) 2004-2005 by dvb.matt, All Rights Reserved.
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

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

import net.sourceforge.dvb.projectx.common.Resource;

public class RawInterface {

	RawReadIF rawRead;

	long stream_size;

	String sourcefile;

	public RawInterface(String aSourceFile) {
		try {
			Class rawReadClass = Class.forName("RawRead");
			rawRead = (RawReadIF) rawReadClass.newInstance();
			sourcefile = aSourceFile;
			stream_size = 0;
		} catch (Exception e) {
			throw new IllegalArgumentException("Can't instantiate RawInterface");
		}
	}

	public final void add_native_files(ArrayList arraylist) {
		rawRead.add_native_files(arraylist);
	}

	public final String GetLoadStatus() {
		if (rawRead.AccessEnabled())
			return rawRead.GetLoadStatus();
		else
			return null;
	}

	/*
	 * true bei tf raw file public boolean isAccessibleDisk(String sourcefile) {
	 * return rawRead.isAccessibleDisk(sourcefile); }
	 */
	public final long getFileSize() {
		return rawRead.getFileSize(sourcefile);
	}

	public final String getFileDate() {
		long datetime = rawRead.lastModified(sourcefile.substring(1));
		return DateFormat.getDateInstance(DateFormat.LONG).format(new Date(datetime)) + "  "
				+ DateFormat.getTimeInstance(DateFormat.LONG).format(new Date(datetime));
	}

	/*
	public boolean getScanData(byte data[]) throws IOException {
		RawFileInputStream rawin = new RawFileInputStream(rawRead, sourcefile);
		rawin.read(data, 0, data.length);
		rawin.close();
		return true;
	}
	*/

	/*
	public boolean getData(byte data[], long skip_size, int read_offset, int size) throws IOException {
		RawFileInputStream rawin = new RawFileInputStream(rawRead, sourcefile);
		rawin.skip(skip_size);
		rawin.read(data, read_offset, size);
		rawin.close();
		return true;
	}
	*/

	public final RawFileInputStream getStream() throws IOException {
		RawFileInputStream rawin = new RawFileInputStream(rawRead, sourcefile);
		stream_size = rawin.streamSize();
		return rawin;
	}

	public final long getStreamSize() {
		return stream_size;
	}
}