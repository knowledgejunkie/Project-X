package net.sourceforge.dvb.projectx.xinput.ftp;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import net.sourceforge.dvb.projectx.xinput.XInputFile;

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
			ftpClient.connect(ftpVO.getServer());

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

		} catch (IOException e) {
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

		} catch (IOException e) {
			ftpInputFiles = null;
		}
		return ftpInputFiles;
	}

	public InputStream retrieveFileStream(String aFileName) throws IOException {
		return ftpClient.retrieveFileStream(aFileName);
	}

	public void close() {
		if (!isOpen) { throw new IllegalStateException("Is already closed, must be opened before!"); }

		try {
			ftpClient.logout();
			ftpClient.disconnect();
		} catch (Exception e) {
			// do nothing
		}

		isOpen = false;
	}

	public boolean test() {

		int base = 0;
		boolean error = false;
		FTPFile[] ftpFiles;

		testMsg = null;

		try {
			int reply;
			ftpClient.connect(ftpVO.getServer());

			// Check connection
			reply = ftpClient.getReplyCode();
			if (!FTPReply.isPositiveCompletion(reply)) {
				ftpClient.disconnect();
				testMsg = "Can't connect.";
				error = true;
				return false;
			}

			// Login
			if (!ftpClient.login(ftpVO.getUser(), ftpVO.getPassword())) {
				ftpClient.logout();
				testMsg = "Can't login.";
				error = true;
				return false;
			}

			// Change directory
			if (!ftpClient.changeWorkingDirectory(ftpVO.getDirectory())) {
				testMsg = "Can't change to directory";
				error = true;
				return false;
			}

			testMsg = "Everything is fine.";
			ftpClient.logout();

		} catch (IOException ex) {
			testMsg = ex.getLocalizedMessage();
		} finally {
			if (ftpClient.isConnected()) {
				try {
					ftpClient.disconnect();
				} catch (IOException f) {
				}
			}
		}
		return true;
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
