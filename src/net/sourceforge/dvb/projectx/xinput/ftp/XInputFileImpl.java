/*
 * @(#)XInputFileImpl.java - implementation for ftp access
 *
 * Copyright (c) 2004-2006 by roehrist, All Rights Reserved. 
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
 * requires Jakarta Commons Net library, developed by the
 * Apache Software Foundation (http://www.apache.org/).
 */

package net.sourceforge.dvb.projectx.xinput.ftp;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.StringTokenizer;

import net.sourceforge.dvb.projectx.common.Resource;
import net.sourceforge.dvb.projectx.common.Common;
import net.sourceforge.dvb.projectx.common.Keys;

import net.sourceforge.dvb.projectx.xinput.FileType;
import net.sourceforge.dvb.projectx.xinput.XInputFileIF;
import net.sourceforge.dvb.projectx.xinput.XInputStream;

import org.apache.commons.net.ftp.FTPFile;

import java.io.DataInputStream;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTP;

import net.sourceforge.dvb.projectx.xinput.StreamInfo;

public class XInputFileImpl implements XInputFileIF {

	private boolean debug = false;

	// Members, which are type independent
	private FileType fileType = null;

	private boolean isopen = false;

	private PushbackInputStream pbis = null;
	private File rafFile = null;
	private RandomAccessFile raf = null;
	private byte[] buffer = new byte[8];

	// Members used for type FileType.FTP
	private FtpVO ftpVO = null;

	private FTPFile ftpFile = null;

	private FTPClient client = null;

	private DataInputStream in = null;

	public XInputStream xIs = null;

	private Object constructorParameter = null;

	private StreamInfo streamInfo = null;

	private boolean supportsResume = true;

	/**
	 * Private Constructor, don't use!
	 */
	private XInputFileImpl() {

		throw new UnsupportedOperationException();
	}

	/**
	 * Create a XInputFile of type FileType.FTP.
	 * 
	 * @param aFtpVO
	 *          Directory data to use
	 * @param aFtpFile
	 *          File data to use
	 */
	public XInputFileImpl(FtpVO aFtpVO) {

		if (debug) System.out.println("Try to create XInputFile of Type FTP");

		ftpVO = aFtpVO;
		ftpFile = aFtpVO.getFtpFile();
		fileType = FileType.FTP;

		if (!exists()) { throw new IllegalArgumentException("File is not of type FileType.FTP"); }

		if (debug) System.out.println("Succeeded to create XInputFile of Type FTP");
	}

	/**
	 * Get String representation of the object.
	 * 
	 * @return String representation of the object
	 */
	public String toString() {

		String s;

		String name = ftpFile.getName();
		name = replaceStringByString(name, "Ã¤", "ä");
		name = replaceStringByString(name, "Ã¶", "ö");
		name = replaceStringByString(name, "Ã¼", "ü");
		name = replaceStringByString(name, "Ã„", "Ä");
		name = replaceStringByString(name, "Ã–", "Ö");
		name = replaceStringByString(name, "Ãœ", "Ü");
		name = replaceStringByString(name, "ÃŸ", "ß");
		name = replaceStringByString(name, "Ã¡", "á");
		name = replaceStringByString(name, "Ã ", "à");
		name = replaceStringByString(name, "Ã©", "é");
		name = replaceStringByString(name, "Ã¨", "è");
		name = replaceStringByString(name, "Ã­", "í");
		name = replaceStringByString(name, "Ã¬", "ì");
		name = replaceStringByString(name, "Ã³", "ó");
		name = replaceStringByString(name, "Ã²", "ò");
		name = replaceStringByString(name, "Ãº", "ú");
		name = replaceStringByString(name, "Ã¹", "ù");

		s = "ftp://" + ftpVO.getUser() + ":" + ftpVO.getPassword() + "@" + ftpVO.getServer() + ftpVO.getPort(":") + ftpVO.getDirectory() + "/"
				+ name;

		return s;
	}

	/**
	 * @return String, checked of arg1 and replaced with arg2 JDK 1.2.2
	 *         compatibility, replacement of newer String.replaceAll()
	 */
	private String replaceStringByString(String name, String arg1, String arg2) {

		if (name == null) return name;

		StringBuffer sb = new StringBuffer(name);

		for (int i = 0; (i = sb.toString().indexOf(arg1, i)) != -1;)
			sb.replace(i, i + 2, arg2);

		return sb.toString();
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

		String s = "ftp://" + ftpVO.getUser() + ":" + ftpVO.getPassword() + "@" + ftpVO.getServer() + ftpVO.getPort(":") + ftpVO.getDirectory() + "/"
				+ ftpFile.getName();

		return s;
	}

	/**
	 * Length of file in bytes.
	 * 
	 * @return Length of file in bytes
	 */
	public long length() {

		return ftpFile.getSize();
	}

	/**
	 * Time in milliseconds from the epoch. JDK1.2.2 adaption: Date.getTime()
	 * 
	 * @return Time in milliseconds from the epoch
	 */
	public long lastModified() {

		// JDK 1.2.2 going trough Date.getTime(), Time is rounded or 0, but date
		// seems correct
		return ftpFile.getTimestamp().getTime().getTime();

		// JDK 1.4.2 return value long is not protected
		//return ftpFile.getTimestamp().getTimeInMillis();
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

		// This method is more exact, but too expensive
		//		try {
		//			b = true;
		//			inputStream = getInputStream();
		//			inputStream.close();
		//			inputStream = null;
		//		} catch (Exception e) {
		//			b = false;
		//		}

		// If ftpFile is set, it was possible to retrieve it, so the file exists
		if (ftpFile != null) {
			b = true;
		}

		return b;
	}

	/**
	 * Get Name of file
	 * 
	 * @return Name of file
	 */
	public String getName() {

		String s = null;

		s = ftpFile.getName();
		s = replaceStringByString(s, "Ã¤", "ä");
		s = replaceStringByString(s, "Ã¶", "ö");
		s = replaceStringByString(s, "Ã¼", "ü");
		s = replaceStringByString(s, "Ã„", "Ä");
		s = replaceStringByString(s, "Ã–", "Ö");
		s = replaceStringByString(s, "Ãœ", "Ü");
		s = replaceStringByString(s, "ÃŸ", "ß");
		s = replaceStringByString(s, "Ã¡", "á");
		s = replaceStringByString(s, "Ã ", "à");
		s = replaceStringByString(s, "Ã©", "é");
		s = replaceStringByString(s, "Ã¨", "è");
		s = replaceStringByString(s, "Ã­", "í");
		s = replaceStringByString(s, "Ã¬", "ì");
		s = replaceStringByString(s, "Ã³", "ó");
		s = replaceStringByString(s, "Ã²", "ò");
		s = replaceStringByString(s, "Ãº", "ú");
		s = replaceStringByString(s, "Ã¹", "ù");

		return s;
	}

	/**
	 * Get Path of parent
	 * 
	 * @return Path of parent
	 */
	public String getParent() {

		return ftpVO.getDirectory();
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

		supportsResume = Common.getSettings().getBooleanProperty(Keys.KEY_useFtpServerResume);

		randomAccessOpen("r");

		if (supportsResume)
			client.setRestartOffset(start_position);  //void

		if (debug) System.out.println("gIS name " + getName());

		xIs = new XInputStream(client.retrieveFileStream(ftpFile.getName()));

		if (debug) System.out.println("gIS retriveStream " + client.getReplyString());

		if (!supportsResume)
			xIs.skip(start_position);

		xIs.setFtpFile(this);
		return xIs;
	}

	/**
	 * rename this file
	 *
	 * @return success
	 */
	public boolean rename() throws IOException {

	//	if (isopen) { throw new IllegalStateException("XInputFile is open!"); }
		if (isopen)
			return false;

		randomAccessOpen("r");

		String name = getName();
		String newName = null;
		boolean ret = false;

		newName = Common.getGuiInterface().getUserInputDialog(name, Resource.getString("autoload.dialog.rename") + " " + getUrl());

		if (newName != null && !newName.equals(""))
			ret = client.rename(name, newName);

		if (ret)
		{
			FTPFile[] aFtpFiles = client.listFiles();

			for (int i = 0; i < aFtpFiles.length; i++)
				if (aFtpFiles[i].getName().equals(newName) && aFtpFiles[i].isFile())
				{
					ftpFile = aFtpFiles[i];
					ftpVO.setFtpFile(ftpFile);
					break;
				}
		}

		randomAccessClose();

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

		if (mode.compareTo("r") != 0) { throw new IllegalStateException("Illegal access mode for FileType.FTP"); }

		boolean ret = false;

		client = new FTPClient();
		client.connect(ftpVO.getServer(), ftpVO.getPortasInteger()); //void
		if (debug) System.out.println("rAO connect " + client.getReplyString());

		ret = client.login(ftpVO.getUser(), ftpVO.getPassword()); //bool
		if (debug) System.out.println("rAO login " + ret + " / " + client.getReplyString());

		ret = client.changeWorkingDirectory(ftpVO.getDirectory()); //bool
		if (debug) System.out.println("rAO cwd " + ret + " / " + client.getReplyString());

		ret = client.setFileType(FTP.BINARY_FILE_TYPE); //bool
		if (debug) System.out.println("rAO binary " + ret + " / " + client.getReplyString());

		client.enterLocalPassiveMode(); //void
		if (debug) System.out.println("rAO PASV " + client.getReplyString());

		String[] commands = getUserFTPCommand();

		for (int i = 0; i < commands.length; i++)
		{
			if (commands[i] != null && commands[i].length() > 0)
			{
				client.sendCommand(commands[i]); //bool
				if (debug) System.out.println("rAO cmd " + client.getReplyString());
			}
		}

		isopen = true;
	}

	/**
	 * @throws java.io.IOException
	 */
	public String[] getUserFTPCommand() {

		StringTokenizer st = new StringTokenizer(Common.getSettings().getProperty(Keys.KEY_FtpServer_Commands), "|");
		String[] tokens = new String[st.countTokens()];

		for (int i = 0; st.hasMoreTokens(); i++)
			tokens[i] = st.nextElement().toString().trim();

		return tokens;
	}


	private boolean EOF() throws IOException {

		int ret;

		if (in != null)
		{
			ret = in.read();

			if (ret < 0)
				return true;
		}

		else if (xIs != null)
		{
			ret = xIs.read();

			if (ret < 0)
				return true;
		}

		return false;
	}

	/**
	 * @throws java.io.IOException
	 */
	public void randomAccessClose() throws IOException {

		if (!isopen) { throw new IllegalStateException("XInputFile is already closed!"); }

		//no need to abort a transfer explicitly, because we logout here

		boolean ret = false;
		if (debug) System.out.println("rAC last " + client.getReplyCode() + " / " + client.getReplyString());

		ret = client.isConnected();
		if (debug) System.out.println("rAC isCon " + ret + " / " + client.getReplyCode() + " / " + client.getReplyString());

		/**
		 * alternative kills the client instance, some server won't abort an incomplete data transfer
		 * (current box-idx 80)
		 */
		if (Common.getSettings().getBooleanProperty(Keys.KEY_killFtpClient))
		{
			if (debug) System.out.println("rAC kill ");
		}

		/**
		 * standard close of a client connection
		 */
		else
		{
			if ( !EOF() )
			{
				if (debug) System.out.println("rAC !eof ");

				ret = client.abort();
				if (debug) System.out.println("rAC abort " + ret + " / " + client.getReplyCode() + " / " + client.getReplyString());
			}

			ret = client.logout();
			if (debug) System.out.println("rAC logout " + ret + " / " + client.getReplyCode() + " / " + client.getReplyString());

			ret = client.isConnected();
			if (debug) System.out.println("rAC isCon " + ret + " / " + client.getReplyCode() + " / " + client.getReplyString());

			if (ret)
			{
				try {
					client.disconnect();
					if (debug) System.out.println("rAC disc " + client.getReplyCode() + " / " + client.getReplyString());
				} catch (IOException e) {
					if (debug) System.out.println("rAC disc-er " + e);
				}
			}
		}

		in = null;
		xIs = null;
		client = null;
		isopen = false;

		System.gc();

		if (debug) System.out.println("rAC out ");
	}

	/**
	 * @param aPosition
	 *          The offset position, measured in bytes from the beginning of the
	 *          file, at which to set the file pointer.
	 * @throws java.io.IOException
	 */
	public void randomAccessSeek(long aPosition) throws IOException {

		client.setRestartOffset(aPosition);  //void
		if (debug) System.out.println("rAS REST " + client.getReplyString() + " /aP " + aPosition);

		in = new DataInputStream(client.retrieveFileStream(ftpFile.getName()));
		if (debug) System.out.println("rAS retriveStream " + client.getReplyString());
	}

	/**
	 * @return @throws
	 *         IOException
	 */
	public long randomAccessGetFilePointer() throws IOException {

		return raf.getFilePointer();
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

		int bytesRead, totalBytes = aOffset;

		while( (bytesRead = in.read(aBuffer, totalBytes, aLength - totalBytes)) > 0)
			totalBytes += bytesRead;

		return bytesRead;
	}

	/**
	 * @return Read line
	 * @throws IOException
	 */
	public String randomAccessReadLine() throws IOException {
		
		return in.readLine();
	}

	/**
	 * @param aBuffer
	 *          The data.
	 * @throws java.io.IOException
	 */
	public void randomAccessWrite(byte[] aBuffer) throws IOException {

		throw new IllegalStateException("Illegal access for FileType.FTP");
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

		return in.readLong();
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