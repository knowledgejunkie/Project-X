package net.sourceforge.dvb.projectx.xinput.ftp;

import org.apache.commons.net.ftp.FTPFile;
import java.util.StringTokenizer;

public class FtpVO implements Cloneable {

	private String server = null;

	private String user = null;

	private String password = null;

	private String directory = null;

	private FTPFile ftpFile = null;

	public FtpVO(String server, String user, String password, String directory, FTPFile ftpFile) {
		this.server = server;
		this.user = user;
		this.password = password;
		this.directory = directory;
		this.ftpFile = ftpFile;
	}

	public void reset() {
		server = null;
		user = null;
		password = null;
		directory = null;
	}

	public String toString() {
		return "ftp://|" + server + "|" + directory + "|" + user + "|" + password;
	}


	public void fromString(String string) {
		// Don't use String.split(), because it is not available on jdk 1.2	
		StringTokenizer st = new StringTokenizer(string, "|");
		String[] tokens = new String[5];

		for (int i=0; st.hasMoreTokens() && i < 5; i++)
			tokens[i] = st.nextElement().toString();

		server = tokens[1];
		directory = tokens[2];
		user = tokens[3];
		password = tokens[4];
	}

	/**
	 * @return Returns the directory.
	 */
	public String getDirectory() {
		return directory;
	}

	/**
	 * @param directory
	 *          The directory to set.
	 */
	public void setDirectory(String directory) {
		this.directory = directory;
	}

	/**
	 * @return Returns the password.
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * @param password
	 *          The password to set.
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * @return Returns the server.
	 */
	public String getServer() {
		return server;
	}

	/**
	 * @param server
	 *          The server to set.
	 */
	public void setServer(String server) {
		this.server = server;
	}

	/**
	 * @return Returns the user.
	 */
	public String getUser() {
		return user;
	}

	/**
	 * @param user
	 *          The user to set.
	 */
	public void setUser(String user) {
		this.user = user;
	}

	/**
	 * @return Returns the ftpFile.
	 */
	public FTPFile getFtpFile() {
		return ftpFile;
	}

	/**
	 * @param aFtpFile
	 *          The ftpFile to set.
	 */
	public void setFtpFile(FTPFile aFtpFile) {
		ftpFile = aFtpFile;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#clone()
	 */
	public Object clone() {
		return new FtpVO(server, user, password, directory, ftpFile);
	}
}