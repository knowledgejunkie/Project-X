/*
 * @(#)FtpServer.java -  for ftp access
 *
 * Copyright (c) 2004-2005 by roehrist, All Rights Reserved. 
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

/*
 * requires Jakarta Commons Net library, developed by the
 * Apache Software Foundation (http://www.apache.org/).
 */

package net.sourceforge.dvb.projectx.xinput.ftp;

import java.io.IOException;
import java.io.InputStream;

import net.sourceforge.dvb.projectx.common.Resource;
import net.sourceforge.dvb.projectx.xinput.XInputFile;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

public class FtpServer {

	private FtpVO ftpVO;

	private StringCommandListener scl;

	private FTPClient ftpClient;

	private boolean isOpen;

	private String testMsg;

	private FtpServer() {
		throw new UnsupportedOperationException();
	}

	public FtpServer(FtpVO aFtpVO) {

		if (aFtpVO == null) { throw new IllegalArgumentException("aFtpVO mustn't be null!"); }

		ftpVO = aFtpVO;
		scl = new StringCommandListener();
		ftpClient = new FTPClient();
		ftpClient.addProtocolCommandListener(scl);
		isOpen = false;
	}

	public boolean open() {
		boolean isSuccessful = false;

		if (isOpen) { throw new IllegalStateException("Is already open, must be closed before!"); }

		try {
			int reply;
			ftpClient.connect(ftpVO.getServer(), ftpVO.getPortasInteger());

			reply = ftpClient.getReplyCode();

			if (!FTPReply.isPositiveCompletion(reply)) {
				ftpClient.disconnect();
				throw new IOException("Can't connect!");
			}

			if (!ftpClient.login(ftpVO.getUser(), ftpVO.getPassword())) {
				ftpClient.logout();
				throw new IOException("Can't login!");
			}

			if (!ftpClient.changeWorkingDirectory(ftpVO.getDirectory())) {
				ftpClient.logout();
				throw new IOException("Can't change directory!");
			}

			ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
			ftpClient.enterLocalPassiveMode();

			isSuccessful = true;

		} catch (Exception e) {
			if (ftpClient.isConnected()) {
				try {
					ftpClient.disconnect();
				} catch (IOException f) {
					// do nothing
				}
			}
			isSuccessful = false;
		}
		isOpen = isSuccessful;
		return isSuccessful;
	}

	public XInputFile[] listFiles() {
		FTPFile[] ftpFiles = null;
		XInputFile[] ftpInputFiles = null;
		try {
			ftpFiles = ftpClient.listFiles();
			ftpInputFiles = new XInputFile[ftpFiles.length];

			for (int i = 0; i < ftpFiles.length; i++) {
				FtpVO tempFtpVO = (FtpVO) ftpVO.clone();
				tempFtpVO.setFtpFile(ftpFiles[i]);
				ftpInputFiles[i] = new XInputFile(tempFtpVO);
			}

		} catch (Exception e) {
			ftpInputFiles = new XInputFile[0];
		}
		return ftpInputFiles;
	}

	public InputStream retrieveFileStream(String aFileName) throws IOException {
		return ftpClient.retrieveFileStream(aFileName);
	}

	public void close() {
		if (!isOpen) {
			throw new IllegalStateException("Is already closed, must be opened before!");
		} else {
			try {
				ftpClient.logout();
				ftpClient.disconnect();
			} catch (Exception e) {
				// do nothing
			}
			isOpen = false;
		}
	}

	public boolean test() {

		int base = 0;
		boolean error = false;
		FTPFile[] ftpFiles;

		testMsg = null;

		try {
			int reply;
			ftpClient.connect(ftpVO.getServer(), ftpVO.getPortasInteger());

			// Check connection
			reply = ftpClient.getReplyCode();
			if (!FTPReply.isPositiveCompletion(reply)) {
				ftpClient.disconnect();
				testMsg = Resource.getString("ftpchooser.msg.noconnect");
				return false;
			}

			// Login
			if (!ftpClient.login(ftpVO.getUser(), ftpVO.getPassword())) {
				ftpClient.logout();
				testMsg = Resource.getString("ftpchooser.msg.nologin");
				return false;
			}

			// Change directory
			if (!ftpClient.changeWorkingDirectory(ftpVO.getDirectory())) {
				testMsg = Resource.getString("ftpchooser.msg.nodirectory");
				return false;
			}

			testMsg = Resource.getString("ftpchooser.msg.success");
			ftpClient.logout();

		} catch (IOException ex) {
			testMsg = ex.getLocalizedMessage();
			error = true;
		} finally {
			if (ftpClient.isConnected()) {
				try {
					ftpClient.disconnect();
				} catch (IOException f) {
				}
			}
		}
		return !error;
	}

	public String getTestMsg() {
		return testMsg;
	}

	public String getLog() {
		return scl.getMessages();
	}
}
/*
 * int base = 0; boolean storeFile = false, binaryTransfer = false, error =
 * false; String server, username, password, remote, local, remoteDir; FTPClient
 * ftp; FTPFile[] ftpFiles;
 * 
 * server = "192.168.0.5"; username = "root"; password = "dreambox"; remote =
 * ""; local = ""; remoteDir = "/hdd/movie";
 * 
 * ftp = new FTPClient(); ftp.addProtocolCommandListener( new
 * PrintCommandListener(new PrintWriter(System.out)));
 * 
 * 
 * try { if (!ftp.login(username, password)) { ftp.logout(); error = true; }
 * else { System.out.println("Remote system is " + ftp.getSystemName());
 * 
 * ftp.setFileType(FTP.BINARY_FILE_TYPE); ftp.enterLocalPassiveMode();
 * 
 * ftp.changeWorkingDirectory(remoteDir); ftpFiles = ftp.listFiles(); for (int i =
 * 0; i < ftpFiles.length; i++) { FTPFile file = ftpFiles[i];
 * System.out.println("Listing of " + remoteDir + ": " + file.getName()); }
 * 
 * 
 * if (storeFile) { InputStream input;
 * 
 * input = new FileInputStream(local); ftp.storeFile(remote, input); } else {
 * OutputStream output;
 * 
 * output = new FileOutputStream(local); ftp.retrieveFile(remote, output); }
 * 
 * 
 * ftp.logout(); } } catch (FTPConnectionClosedException e) { error = true;
 * System.err.println("Server closed connection."); e.printStackTrace(); } catch
 * (IOException e) { error = true; e.printStackTrace(); } finally { if
 * (ftp.isConnected()) { try { ftp.disconnect(); } catch (IOException f) { // do
 * nothing } } }
 */
