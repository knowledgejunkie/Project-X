/*
 * @(#)XInputFileImpl.java - implementation for files
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

package net.sourceforge.dvb.projectx.xinput.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;

import net.sourceforge.dvb.projectx.xinput.FileType;
import net.sourceforge.dvb.projectx.xinput.XInputFileIF;
import net.sourceforge.dvb.projectx.xinput.XInputStream;

import net.sourceforge.dvb.projectx.common.Common;
import net.sourceforge.dvb.projectx.common.Resource;

import net.sourceforge.dvb.projectx.xinput.StreamInfo;


public class XInputFileImpl implements XInputFileIF {

	private boolean debug = false;

	// Members, which are type independent
	private FileType fileType = null;

	private boolean isopen = false;

	private InputStream inputStream = null;

	// Members used for type FileType.FILE
	private File file = null;

	private String file_separator = System.getProperty("file.separator");

	private RandomAccessFile randomAccessFile = null;

	private Object constructorParameter = null;

	private StreamInfo streamInfo = null;

	/**
	 * Private Constructor, don't use!
	 */
	private XInputFileImpl() {

		throw new UnsupportedOperationException();
	}

	/**
	 * Create a XInputFile of type FileType.FILE.
	 * 
	 * @param aFile
	 *          File data to use
	 * @throws IllegalArgumentException
	 *           If aFile is not a file
	 */
	public XInputFileImpl(File aFile) {

		if (debug) System.out.println("Try to create XInputFile of Type FILE");

		if (!aFile.isFile()) { throw new IllegalArgumentException("File is not of type FileType.FILE"); }
		file = aFile;
		fileType = FileType.FILE;

		if (!exists()) { throw new IllegalArgumentException("File doesn't exist"); }

		if (debug) System.out.println("Succeeded to create XInputFile of Type FILE");
	}

	/**
	 * Get String representation of the object.
	 * 
	 * @return String representation of the object
	 */
	public String toString() {

		return file.getAbsolutePath();
	//	return (file.getAbsolutePath() + " (" + Common.formatNumber(length()) + " bytes)");
	}


	public void setConstructorParameter(Object obj) {
		constructorParameter = obj;
	}
			
	public Object getConstructorParameter() {
		return constructorParameter;
	}

	/**
	 * Get url representation of the object.
	 * 
	 * @return String with url
	 */
	public String getUrl() {

		return "file://" + file.getAbsolutePath();
	}

	/**
	 * Length of file in bytes.
	 * 
	 * @return Length of file in bytes
	 */
	public long length() {

		return file.length();
	}

	/**
	 * Time in milliseconds from the epoch.
	 * 
	 * @return Time in milliseconds from the epoch
	 */
	public long lastModified() {

		return file.lastModified();
	}

	/**
	 * sets Time in milliseconds from the epoch.
	 * 
	 * @return Time in milliseconds from the epoch
	 */
	public boolean setLastModified() {

		return file.setLastModified(System.currentTimeMillis());
	}

	/**
	 * Checks if file exists
	 * 
	 * @return Result of check
	 */
	public boolean exists() {

		return file.exists();
	}

	/**
	 * Get Name of file
	 * 
	 * @return Name of file
	 */
	public String getName() {

		return file.getName();
	}

	/**
	 * Get Path of parent
	 * 
	 * @return Path of parent
	 */
	public String getParent() {

		return file.getParent();
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

		XInputStream xIs = new XInputStream(new FileInputStream(file));
		xIs.skip(start_position);

		return xIs;
	}

	/**
	 * rename this file
	 * @return 
	 */
	public boolean rename() throws IOException {

	//	if (isopen) { throw new IllegalStateException("XInputFile is open!"); }
		if (isopen)
			return false;

		String parent = getParent();
		String name = getName();
		String newName = null;
		boolean ret = false;

		if ( !parent.endsWith(file_separator) )
			parent += file_separator;

		newName = Common.getGuiInterface().getUserInputDialog(name, Resource.getString("autoload.dialog.rename") + " " + parent + name);

		if (newName != null && !newName.equals(""))
		{
			if (new File(parent + newName).exists())
			{
				ret = Common.getGuiInterface().getUserConfirmationDialog(Resource.getString("autoload.dialog.fileexists"));

				if (ret)
				{
					new File(parent + newName).delete();
					ret = Common.renameTo(parent + name, parent + newName);
				}
			}
			else
				ret = Common.renameTo(parent + name, parent + newName);
		}

		if (ret)
		{
			file = new File(parent + newName);
			constructorParameter = file;
		}

		return ret;
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
		randomAccessFile = new RandomAccessFile(file, mode);
		isopen = true;
	}

	/**
	 * @throws java.io.IOException
	 */
	public void randomAccessClose() throws IOException {

		if (!isopen) { throw new IllegalStateException("XInputFile is already closed!"); }
		if (randomAccessFile != null) {
			randomAccessFile.close();
			randomAccessFile = null;
		}
		isopen = false;
	}

	/**
	 * @param aPosition
	 *          The offset position, measured in bytes from the beginning of the
	 *          file, at which to set the file pointer.
	 * @throws java.io.IOException
	 */
	public void randomAccessSeek(long aPosition) throws IOException {

		randomAccessFile.seek(aPosition);
	}

	/**
	 * @return @throws
	 *         IOException
	 */
	public long randomAccessGetFilePointer() throws IOException {
		return randomAccessFile.getFilePointer();
	}

	/**
	 * @return @throws
	 *         IOException
	 */
	public int randomAccessRead() throws IOException {
		return randomAccessFile.read();
	}

	/**
	 * @param aBuffer
	 *          The buffer into which the data is read.
	 * @return @throws
	 *         java.io.IOException
	 */
	public int randomAccessRead(byte[] aBuffer) throws IOException {
		return randomAccessFile.read(aBuffer);
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
		return randomAccessFile.read(aBuffer, aOffset, aLength);
	}

	/**
	 * @return Read line
	 * @throws IOException
	 */
	public String randomAccessReadLine() throws IOException {
		return randomAccessFile.readLine();
	}

	/**
	 * @param aBuffer
	 *          The data.
	 * @throws java.io.IOException
	 */
	public void randomAccessWrite(byte[] aBuffer) throws IOException {

		randomAccessFile.write(aBuffer);
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

		return randomAccessFile.readLong();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.sourceforge.dvb.projectx.xinput.XInputFileIF#getFileType()
	 */
	public FileType getFileType() {
		return fileType;
	}
//
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