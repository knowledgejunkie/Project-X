package net.sourceforge.dvb.projectx.xinput.ftp;

import net.sourceforge.dvb.projectx.xinput.DirType;
import net.sourceforge.dvb.projectx.xinput.XInputDirectoryIF;
import net.sourceforge.dvb.projectx.xinput.XInputFile;

//+
import java.net.URL;
//-

public class XInputDirectoryImpl implements XInputDirectoryIF {

	private DirType dirType = null;

	private FtpVO ftpVO = null;

	private FtpServer ftpServer = null;

	/**
	 * Mustn't be used
	 * 
	 * @throws UnsupportedOperationException
	 *           Because it mustn't be used!
	 */
	private XInputDirectoryImpl() {
		throw new UnsupportedOperationException("Usage is not allowed!");
	}

	/**
	 * Create a XInputDirectory of type DirType.FTP_DIR.
	 * 
	 * @param aFtpVO
	 *          Directory data to use
	 */
	public XInputDirectoryImpl(FtpVO aFtpVO) {
		ftpVO = aFtpVO;
		ftpServer = new FtpServer(ftpVO);
		dirType = DirType.FTP_DIR;
	}

	/**
	 * Create a XInputDirectory of type DirType.FILE_DIR.
	 * 
	 * @param aFileIdentifier
	 *          Directory name
	 * @throws IllegalArgumentException
	 *           If aFile is not a directory
	 */
	public XInputDirectoryImpl(String aFileIdentifier) {

		if (aFileIdentifier.startsWith("ftp://")) {
			int posColon = aFileIdentifier.indexOf(':', 6);
			int posAt = aFileIdentifier.indexOf('@', posColon);
			int posSlash = aFileIdentifier.indexOf('/', posAt);

			if (posAt == -1 || posColon == -1 || posSlash == -1) { throw new IllegalArgumentException(
					"aFileIdentifier is a malformed ftp URL!"); }

			String user = aFileIdentifier.substring(6, posColon);
			String password = aFileIdentifier.substring(posColon + 1, posAt);
			String server = aFileIdentifier.substring(posAt + 1, posSlash);
			String directory = aFileIdentifier.substring(posSlash);
			String port = null;

			int posColon2 = server.indexOf(':');
			if (posColon2 != -1)
			{
				server = server.substring(0, posColon2);
				port = server.substring(posColon2 + 1);
			}

			ftpVO = new FtpVO(server, user, password, directory, port, null);
			ftpServer = new FtpServer(ftpVO);
			dirType = DirType.FTP_DIR;

			if (!test()) {
				ftpVO = null;
				ftpServer = null;
				dirType = null;
				throw new IllegalArgumentException(aFileIdentifier + " is not a correct ftp URL!");
			}
		} else {
			throw new IllegalArgumentException(aFileIdentifier + " is not a correct ftp URL!");
		}
	}

//+
	/**
	 * Create a XInputDirectory of type DirType.FILE_DIR.
	 * 
	 * @param aFileIdentifier
	 *          Directory URL
	 * @throws IllegalArgumentException
	 *           If URL is not a directory
	 */
	public XInputDirectoryImpl(URL url) {

		if (url.getProtocol().compareTo("ftp") != -1) {

			String _host = url.getHost();

			String server = _host;
			String user = null;
			String password = null;
			String directory = null;
			String port = null;

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

			if (user == null && password == null)
			{
				user = "anonymous";
				password = "";
			}

			int _port = url.getPort();
			port = _port != -1 ? String.valueOf(_port) : null;

			String _file = url.getFile();

			i = _file.lastIndexOf("/");
			directory = _file.substring(0, i);

			ftpVO = new FtpVO(server, user, password, directory, port, null);
			ftpServer = new FtpServer(ftpVO);
			dirType = DirType.FTP_DIR;

			if (!test()) {
				ftpVO = null;
				ftpServer = null;
				dirType = null;
				throw new IllegalArgumentException(url + " is not a correct ftp URL!");
			}
		} else {
			throw new IllegalArgumentException(url + " is not a correct ftp URL!");
		}
	}
//-

	/**
	 * Get String representation of the object.
	 * 
	 * @return String representation of the object
	 */
	public String toString() {

		String s = null;

	//	s = "ftp://" + ftpVO.getUser() + ":" + ftpVO.getPassword() + "@" + ftpVO.getServer() + ftpVO.getDirectory();
		s = "ftp://" + ftpVO.getUser() + ":" + ftpVO.getPassword() + "@" + ftpVO.getServer() + ftpVO.getPort(":") + ftpVO.getDirectory();

		return s;
	}

	/**
	 * Get path of directory
	 * 
	 * @return Path of directory
	 */
	public String getDirectory() {

		return ftpVO.getDirectory();
	}

	/**
	 * Get password for the ftp server
	 * 
	 * @return Password for the ftp server
	 * @throws IllegalStateException
	 *           If file type of object is not DirType.FTP_DIR
	 */
	public String getPassword() {

		return ftpVO.getPassword();
	}

	/**
	 * Get name or ip address of the ftp server
	 * 
	 * @return Name or ip address of the ftp server
	 * @throws IllegalStateException
	 *           If file type of object is not DirType.FTP_DIR
	 */
	public String getServer() {

		return ftpVO.getServer();
	}

	/**
	 * Get name or ip address of the ftp server
	 * 
	 * @return port of the ftp server
	 * @throws IllegalStateException
	 *           If file type of object is not DirType.FTP_DIR
	 */
	public String getPort() {

		return ftpVO.getPort();
	}

	/**
	 * Get user for the ftp server
	 * 
	 * @return User for the ftp server
	 * @throws IllegalStateException
	 *           If file type of object is not DirType.FTP_DIR
	 */
	public String getUser() {

		return ftpVO.getUser();
	}

	/**
	 * Get log of communication with ftp server.
	 * 
	 * @return Log of communication with ftp server
	 * @throws IllegalStateException
	 *           If file type of object is not DirType.FTP_DIR
	 */
	public String getLog() {

		return ftpServer.getLog();
	}

	/**
	 * Get files in the directory.
	 * 
	 * @return files in the directory
	 */
	public XInputFile[] getFiles() {

		XInputFile[] xInputFiles = null;

		ftpServer.open();
		xInputFiles = ftpServer.listFiles();
		ftpServer.close();

		return xInputFiles;
	}

	/**
	 * Test if directory data is valid.
	 * 
	 * @return Test successful or not
	 */
	public boolean test() {

		return ftpServer.test();
	}

	/**
	 * Get result message after test().
	 * 
	 * @return result message after test()
	 */
	public String getTestMsg() {

		return ftpServer.getTestMsg();
	}

	/**
	 * @return Type of XInputDirectory
	 */
	public DirType getDirType() {

		return dirType;
	}
}