/*
 * @(#)RawFileInputStream.java - alias class definition, may be replaced by additional disk access
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

package net.sourceforge.dvb.projectx.xinput.topfield_raw;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class RawFileInputStream extends InputStream {
	String file;
	int handle;
	long currentpos;
	RawReadIF rawread;

	public int read(byte[] b, int off, int len) throws IOException {
		len = rawread.readFile(handle,b,off,len);
		currentpos+=len;
		return len;
	}

	public int read(byte[] b) throws IOException {
		return read(b, 0, b.length);
	}

	public int read() throws IOException {
		byte[] b=new byte[1];
		if (read(b)==1)
			return (int)b[0];
		else
			return -1;
	}

	public int available() throws IOException {
		long avail=streamSize()-currentpos;
		if (avail>2*1024*1024*104)
			return 2*1024*1024*104;
		else
			return (int)avail;
	}

	public void mark(int readlimit) {
		// not implemented
	}

	public void reset() throws IOException {
		// not implemented
	}

	public boolean markSupported() {
		return false;
	}

	public long skip(long n) throws IOException {

		long skipped = rawread.skipBytes(handle,n);
		currentpos+=skipped;
		return skipped;
	}

	//alias required
	public long streamSize() throws IOException {

		return rawread.getFileSize(file);
	}

	public RawFileInputStream(RawReadIF rawread,String file) throws FileNotFoundException {
		this.rawread=rawread;
		this.file=file;
		this.handle=rawread.openFile(this.file);
		currentpos=0;
	}


	public void close() throws IOException {
		if (handle!=0)
			rawread.closeFile(handle);
		handle=0;
	}
}
