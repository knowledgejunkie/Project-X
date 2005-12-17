/*
 * @(#)RawFileInputStream.java - alias class definition, may be replaced by
 * additional disk access
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class RawFileInputStream extends InputStream {

	private byte[] b = new byte[1];

	private String file;

	private int handle;

	private long currentpos;

	private RawReadIF rawread;

	public RawFileInputStream(RawReadIF rawread, String file) throws FileNotFoundException {
		this.rawread = rawread;
		this.file = file;
		this.handle = rawread.openFile(this.file);
		currentpos = 0;
	}

	public final int read(byte[] b, int off, int len) throws IOException {
		len = rawread.readFile(handle, b, off, len);
		currentpos += len;
		return len;
	}

	public final int read(byte[] b) throws IOException {
		return read(b, 0, b.length);
	}

	public final int read() throws IOException {
		// byte[] b = new byte[1]; is now Attribute of class
		if (read(b, 0, 1) == 1)
			return (int) b[0];
		else
			return -1;
	}

	public final int available() throws IOException {
		long avail = streamSize() - currentpos;
		if (avail > 2 * 1024 * 1024 * 104)
			return 2 * 1024 * 1024 * 104;
		else
			return (int) avail;
	}

	public final void mark(int readlimit) {
		// not implemented
	}

	public final void reset() throws IOException {
		// not implemented
	}

	public final boolean markSupported() {
		return false;
	}

	public final long skip(long n) throws IOException {

		long skipped = rawread.skipBytes(handle, n);
		currentpos += skipped;
		return skipped;
	}

	//alias required
	public final long streamSize() throws IOException {

		return rawread.getFileSize(file);
	}

	public final void close() throws IOException {
		if (handle != 0) rawread.closeFile(handle);
		handle = 0;
	}
}