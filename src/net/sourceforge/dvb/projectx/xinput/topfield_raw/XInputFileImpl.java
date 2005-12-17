/*
 * @(#)XInputFileImpl.java - implementation for topfield hd access
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

import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.net.MalformedURLException;

import net.sourceforge.dvb.projectx.xinput.FileType;
import net.sourceforge.dvb.projectx.xinput.XInputFileIF;
import net.sourceforge.dvb.projectx.xinput.XInputStream;

import net.sourceforge.dvb.projectx.xinput.StreamInfo;

public class XInputFileImpl implements XInputFileIF {

	private boolean debug = false;

	// Members, which are type independent
	private FileType fileType = null;

	private boolean isopen = false;

	private PushbackInputStream pbis = null;

	private long randomAccessCurrentPosition = 0;

	private byte[] buffer = new byte[8];

	// Members used for type FileType.RAW
	private RawInterface rawInterface = null;

	private String fileName = null;

	private Object constructorParameter = null;

	private StreamInfo streamInfo = null;

	/**
	 * Private Constructor, don't use!
	 */
	private XInputFileImpl() {

		throw new UnsupportedOperationException();
	}

	/**
	 * Create a XInputFile of type FileType.RAW.
	 * 
	 * @param aFtpVO
	 *          Directory data to use
	 * @param aFtpFile
	 *          File data to use
	 */
	public XInputFileImpl(String aFileName) {

		if (debug) System.out.println("Try to create XInputFile of Type RAW");

		try {
			fileName = aFileName;
			fileType = FileType.RAW;
			rawInterface = new RawInterface(aFileName);
			rawInterface.getStream().close();

		} catch (IOException e) {
			throw new IllegalArgumentException("File is not of type FileType.RAW");
		}

		if (debug) System.out.println("Succeeded to create XInputFile of Type RAW");
	}

	/**
	 * Get String representation of the object.
	 * 
	 * @return String representation of the object
	 */
	public String toString() {
		return fileName;
	}

	public void setConstructorParameter(Object obj) {
		constructorParameter = obj;
	}
			
	public Object getConstructorParameter() {
		return constructorParameter;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.sourceforge.dvb.projectx.xinput.XInputFileIF#getFileType()
	 */
	public FileType getFileType() {
		return fileType;
	}

	/**
	 * Get url representation of the object.
	 * 
	 * @return String with url
	 */
	public String getUrl() {

		String s;

		s = "raw://" + fileName;

		return s;
	}

	/**
	 * Length of file in bytes.
	 * 
	 * @return Length of file in bytes
	 */
	public long length() {
		return rawInterface.getFileSize();
	}

	/**
	 * Time in milliseconds from the epoch.
	 * 
	 * @return Time in milliseconds from the epoch
	 */
	public long lastModified() {
		return rawInterface.rawRead.lastModified(fileName);
	}

	/**
	 * sets Time in milliseconds from the epoch.
	 * 
	 * @return success
	 */
	public boolean setLastModified() {

		return true; //later
	}

	/**
	 * Checks if file exists
	 * 
	 * @return Result of check
	 */
	public boolean exists() {

		boolean b = false;

		try {
			rawInterface.getStream().close();
			b = true;
		} catch (IOException e) {
			b = false;
		}

		return b;
	}

	/**
	 * Get Name of file
	 * 
	 * @return Name of file
	 */
	public String getName() {
		return fileName;
	}

	/**
	 * Get Path of parent
	 * 
	 * @return Path of parent
	 */
	public String getParent() {
		return "";
	}

	/**
	 * Get input stream from the file. close() on stream closes XInputFile, too.
	 * 
	 * @return Input stream from the file
	 */
	public InputStream getInputStream() throws FileNotFoundException, MalformedURLException, IOException {

		return getInputStream(0L);
	}

	/**
	 * Get input stream from the file. close() on stream closes XInputFile, too.
	 * 
	 * @return Input stream from the file
	 */
	public InputStream getInputStream(long start_position) throws FileNotFoundException, MalformedURLException, IOException {

		XInputStream xIs = new XInputStream(rawInterface.getStream());
		xIs.skip(start_position);

		return xIs;
	}

	/**
	 * @return bool
	 * not yet supported
	 */
	public boolean rename() throws IOException {
		return false;
	}

	/**
	 * Opens XInputFile for random access
	 * 
	 * @param mode
	 *          Access mode as in RandomAccessFile
	 * @throws IOException
	 */
	public void randomAccessOpen(String mode) throws IOException {

		if (isopen) { throw new IllegalStateException("XInputFile is already open!"); }

		if (mode.compareTo("r") != 0) { throw new IllegalStateException("Illegal access mode for FileType.RAW"); }
		pbis = new PushbackInputStream(getInputStream());

		randomAccessCurrentPosition = 0;
		isopen = true;
	}

	/**
	 * @throws java.io.IOException
	 */
	public void randomAccessClose() throws IOException {

		if (!isopen) { throw new IllegalStateException("XInputFile is already closed!"); }

		if (pbis != null) {
			try {
				pbis.close();
			} catch (IOException e) {
				if (debug) System.out.println(e.getLocalizedMessage());
				if (debug) e.printStackTrace();
			}
			pbis = null;
		}

		randomAccessCurrentPosition = 0;
		isopen = false;
	}

	/**
	 * @param aPosition
	 *          The offset position, measured in bytes from the beginning of the
	 *          file, at which to set the file pointer.
	 * @throws java.io.IOException
	 */
	public void randomAccessSeek(long aPosition) throws IOException {

		if (aPosition > randomAccessCurrentPosition) {
			randomAccessCurrentPosition += pbis.skip(aPosition - randomAccessCurrentPosition);
		}
		if (aPosition < randomAccessCurrentPosition) {
			randomAccessClose();
			randomAccessOpen("r");
			randomAccessCurrentPosition = pbis.skip(aPosition);
		}
	}

	/**
	 * @return @throws
	 *         IOException
	 */
	public long randomAccessGetFilePointer() throws IOException {
		return randomAccessCurrentPosition;
	}

	/**
	 * @return @throws
	 *         IOException
	 */
	public int randomAccessRead() throws IOException {
		buffer[0] = -1;
		randomAccessRead(buffer, 0, 1);
		return (int) buffer[0];
	}

	/**
	 * @param aBuffer
	 *          The buffer into which the data is read.
	 * @return @throws
	 *         java.io.IOException
	 */
	public int randomAccessRead(byte[] aBuffer) throws IOException {
		return randomAccessRead(aBuffer, 0, aBuffer.length);
	}

	/**
	 * @param aBuffer
	 *          The buffer into which the data is written.
	 * @param aOffset
	 *          The offset at which the data should be written.
	 * @param aLength
	 *          The amount of data to be read.
	 * @return @throws
	 *         IOException
	 */
	public int randomAccessRead(byte[] aBuffer, int aOffset, int aLength) throws IOException {
		int result = 0;

		result = pbis.read(aBuffer, aOffset, aLength);
		if (result != -1) {
			randomAccessCurrentPosition += result;
		}
		return result;
	}

	/**
	 * @return Read line
	 * @throws IOException
	 */
	public String randomAccessReadLine() throws IOException {
		StringBuffer sb = new StringBuffer();
		int ch = 0;

		do {
			ch = randomAccessRead();
			sb.append(ch);
		} while ((ch != '\r') && (ch != '\n') && (ch != -1));
		sb.deleteCharAt(sb.length() - 1);
		if (ch == '\r') {
			ch = randomAccessRead();
			if (ch != '\n' && (ch != -1)) {
				pbis.unread(ch);
				randomAccessCurrentPosition -= 1;
			}
		}
		return sb.toString();
	}

	/**
	 * @param aBuffer
	 *          The data.
	 * @throws java.io.IOException
	 */
	public void randomAccessWrite(byte[] aBuffer) throws IOException {

		throw new IllegalStateException("Illegal access for FileType.RAW");
	}

	/**
	 * Convinience method for a single random read access to a input file. The
	 * file is opened before and closed after read.
	 * 
	 * @param aBuffer
	 *          Buffer to fill with read bytes (up to aBuffer.length() bytes)
	 * @param aPosition
	 *          Fileposition at which we want read
	 * @throws IOException
	 */
	public void randomAccessSingleRead(byte[] aBuffer, long aPosition) throws IOException {

		randomAccessOpen("r");
		randomAccessSeek(aPosition);
		randomAccessRead(aBuffer);
		randomAccessClose();
	}

	/**
	 * @return Long value read.
	 * @throws java.io.IOException
	 */
	public long randomAccessReadLong() throws IOException {

		long l = 0;

		int bytesRead = 0;

		bytesRead = randomAccessRead(buffer, 0, 8);
		if (bytesRead < 8) { throw new EOFException("Less than 8 bytes read"); }
		l = ((long) buffer[1] << 56) + ((long) buffer[2] << 48) + ((long) buffer[3] << 40) + ((long) buffer[4] << 32)
				+ ((long) buffer[5] << 24) + ((long) buffer[6] << 16) + ((long) buffer[7] << 8) + buffer[8];

		return l;
	}

	/**
	 *
	 */
	public void setStreamInfo(StreamInfo _streamInfo)
	{
		streamInfo = _streamInfo;
	}

	/**
	 *
	 */
	public StreamInfo getStreamInfo()
	{
		return streamInfo;
	}
}