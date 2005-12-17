/*
 * @(#)FtpVO.java -  for ftp access
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

/*
 * requires Jakarta Commons Net library, developed by the
 * Apache Software Foundation (http://www.apache.org/).
 */

package net.sourceforge.dvb.projectx.xinput.ftp;

import java.util.StringTokenizer;

import org.apache.commons.net.ftp.FTPFile;

public class FtpVO implements Cloneable {

	private String server = null;

	private String user = null;

	private String password = null;

	private String directory = null;

	private String port = null;

	private FTPFile ftpFile = null;

	public FtpVO(String server, String user, String password, String directory, FTPFile ftpFile) {
		this.server = server;
		this.user = user;
		this.password = password;
		this.directory = directory;
		this.ftpFile = ftpFile;
		port = null;
	}

	public FtpVO(String server, String user, String password, String directory, String port, FTPFile ftpFile) {
		this.server = server;
		this.user = user;
		this.password = password;
		this.directory = directory;
		this.port = port;
		this.ftpFile = ftpFile;
	}

	public void reset() {
		server = null;
		user = null;
		password = null;
		directory = null;
		port = null;
	}

	public String toString() {
		return "ftp://|" + server + "|" + port + "|" + directory + "|" + user + "|" + password;
	}

	public void fromString(String string) {
		// Don't use String.split(), because it is not available on jdk 1.2
		StringTokenizer st = new StringTokenizer(string, "|");
		String[] tokens = new String[6];

		for (int i = 0; st.hasMoreTokens() && i < 6; i++)
			tokens[i] = st.nextElement().toString();

		server = tokens[1];
		directory = tokens[3];
		user = tokens[4];
		password = tokens[5];

		try {
			Integer.parseInt(tokens[2]);
			port = tokens[2];
		} catch (Exception e) {
			port = null;
		}
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
	 * @return Returns the port.
	 */
	public String getPort() {
		if (port == null || port.trim().length() == 0)
			return null;

		return port;
	}

	/**
	 * @return Returns the port.
	 */
	public String getPort(String prefix) {
		if (port == null || port.trim().length() == 0)
			return "";

		return prefix + port;
	}

	/**
	 * @return Returns the port.
	 */
	public int getPortasInteger() {
		if (port == null)
			return 21;

		return Integer.parseInt(port);
	}

	/**
	 * @param port
	 *          The port to set.
	 */
	public void setPort(int port) {
		this.port = String.valueOf(port);
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
		return new FtpVO(server, user, password, directory, port, ftpFile);
	}
}