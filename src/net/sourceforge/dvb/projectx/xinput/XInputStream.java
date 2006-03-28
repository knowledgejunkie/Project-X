/*
 * @(#)XInputStream.java
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

package net.sourceforge.dvb.projectx.xinput;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedInputStream;

import net.sourceforge.dvb.projectx.xinput.ftp.XInputFileImpl;

import net.sourceforge.dvb.projectx.common.Common;
import net.sourceforge.dvb.projectx.common.Keys;

public class XInputStream extends FilterInputStream {

	private boolean debug = false;

	private byte[] buffer = new byte[1];

	private XInputFileImpl xInputFile = null;

	/**
	 * Create stream, which is able to handle special needs of the xinput package.
	 * 
	 * @param aIs
	 *          InputStream
	 * @see java.io.FilterInputStream#FilterInputStream(InputStream in)
	 */
	public XInputStream(InputStream aIs) {
		super(Common.getSettings().getBooleanProperty(Keys.KEY_additionalInputBuffer) ? new BufferedInputStream(aIs, 1048576) : aIs);
	}

	public void setFtpFile(XInputFileImpl aIf) {
		xInputFile = aIf;
	}

	/**
	 * Takes care, that always the full amount of data is read (if possible).
	 * Blocks until it succeeds.
	 * 
	 * @see java.io.InputStream#read()
	 */
	public final int read() throws IOException {
		if (read(buffer, 0, 1) == 1)
			return (int) buffer[0];
		else
			return -1;
	}

	/**
	 * Takes care, that always the full amount of data is read (if possible).
	 * Blocks until it succeeds.
	 * 
	 * @param aBuffer
	 *          Buffer to fill with data
	 * @see java.io.InputStream#read(byte[])
	 */
	public final int read(byte[] aBuffer) throws IOException {
		return read(aBuffer, 0, aBuffer.length);
	}

	/**
	 * Takes care, that always the full amount of data is read (if possible).
	 * Blocks until it succeeds.
	 * 
	 * @param aBuffer
	 *          Buffer to keep data
	 * @param off
	 *          Offset in buffer
	 * @param len
	 *          Length of data to read
	 * @see java.io.InputStream#read(byte[], int, int)
	 */
	public final int read(byte[] aBuffer, int off, int len) throws IOException {

		if (debug) System.out.println("Enter XInputStream.read(" + aBuffer + ", " + off + ", " + len + ")");

		int result = 0;
		long read = 0;
		long readBytes = 0;
		long remaining = len;
		long retryCount = 0;

		try {
			do {
				read = super.read(aBuffer, (int)(off + readBytes), (int) remaining);
				if (debug) System.out.println("    Bytes read in this cycle: " + read);
				if (read > 0) {
					readBytes += read;
					remaining -= read;
				}
				if (read == 0) {
					retryCount++;
				} else {
					retryCount = 0;
				}

			} while ((remaining > 0) && (read != -1) && (retryCount <= 100));
			result = (int) (len - remaining);

			if ((read == -1) && (result == 0)) {
				if (debug) System.out.println("Leave XInputStream.read(aBuffer,off,len) returning -1");
				return -1;
			} else {
				if (debug) System.out.println("Leave XInputStream.read(aBuffer,off,len) returning " + result);
				return result;
			}
		} finally {
			if (debug && (readBytes != len))
					System.out.println("********** ATTENTION! Bytes to read: "
							+ len + ", Read: " + readBytes + ", Difference: " + (len - readBytes) + "**********");
		}
	}

	/**
	 * @see java.io.InputStream#close()
	 */
	public final void close() throws IOException {
		if (debug) System.out.println("Enter XInputStream.close()");

		if (xInputFile != null)
		{
			xInputFile.randomAccessClose();
			xInputFile = null;
		}

		super.close();
		if (debug) System.out.println("Leave XInputStream.close()");
	}

	/**
	 * @see java.io.InputStream#skip(long)
	 */
	public final long skip(long n) throws IOException {

		if (debug) System.out.println("Enter XInputStream.skip(" + n + ")");

		long retryCount = 0;
		long skiped = 0;
		long skipedBytes = 0;
		long remaining = n;

		try {
			do {
				skiped = super.skip(remaining);
				if (debug) System.out.println("    Bytes skiped in this cycle: " + skiped);
				skipedBytes += skiped;
				remaining -= skiped;
				if (skiped == 0) {
					retryCount++;
				} else {
					retryCount = 0;
				}
			} while ((remaining > 0) && (retryCount <= 100));
			if (debug) System.out.println("Leave XInputStream.skip(" + n + ") returning " + skipedBytes);
			return skipedBytes;
		} finally {
			if (debug && (skipedBytes != n))
					System.out.println("********** ATTENTION! Bytes to skip: " + n
							+ ", Skiped: " + skipedBytes + ", Difference: " + (n - skipedBytes) + "**********");
		}
	}
}