package net.sourceforge.dvb.projectx.xinput.ftp;

import java.util.StringTokenizer;

import org.apache.commons.net.ftp.FTPFile;

//+
import java.net.URL;
//-

public class FtpVO implements Cloneable {

	private String server = null;

	private String user = null;

	private String password = null;

	private String directory = null;
//+
	private String port = null;
//-
	private FTPFile ftpFile = null;

	public FtpVO(String server, String user, String password, String directory, FTPFile ftpFile) {
		this.server = server;
		this.user = user;
		this.password = password;
		this.directory = directory;
		this.ftpFile = ftpFile;
	}
//+
	public FtpVO(URL url) {

		setURL(url);
	}

	public void setURL(URL url) {

		String _host = url.getHost();
		server = _host;
		user = null;
		password = null;
		directory = null;

		int i = _host.indexOf("@");
		if (i != -1)
		{
			server = _host.substring(i + 1);
			user = _host.substring(0, i);

			i = user.indexOf(":");
			if (i != -1)
			{
				password = user.substring(i + 1);
				user = user.substring(0, i);
			}
		}

		int _port = url.getPort();
		port = _port != -1 ? String.valueOf(_port) : null;

		String _file = url.getFile();

		i = _file.lastIndexOf("/");
		directory = _file.substring(0, i);

		ftpFile = new FTPFile();
		ftpFile.setName(_file.substring(i + 1));
		ftpFile.setType(FTPFile.FILE_TYPE);

System.out.println("sv " + server);
System.out.println("us " + user);
System.out.println("pw " + password);
System.out.println("po " + port);
System.out.println("di " + directory);
System.out.println("fi " + ftpFile.getName());
System.out.println("ft " + ftpFile);
	}
//-

	public void reset() {
		server = null;
		user = null;
		password = null;
		directory = null;
//+
		port = null;
//-
	}

	public String toString() {
		return "ftp://|" + server + "|" + directory + "|" + user + "|" + password;
	}

	public void fromString(String string) {
		// Don't use String.split(), because it is not available on jdk 1.2
		StringTokenizer st = new StringTokenizer(string, "|");
		String[] tokens = new String[5];

		for (int i = 0; st.hasMoreTokens() && i < 5; i++)
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