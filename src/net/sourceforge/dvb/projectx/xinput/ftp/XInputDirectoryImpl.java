package net.sourceforge.dvb.projectx.xinput.ftp;

import net.sourceforge.dvb.projectx.xinput.DirType;
import net.sourceforge.dvb.projectx.xinput.XInputDirectoryIF;
import net.sourceforge.dvb.projectx.xinput.XInputFile;

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

			ftpVO = new FtpVO(server, user, password, directory, null);
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

	/**
	 * Get String representation of the object.
	 * 
	 * @return String representation of the object
	 */
	public String toString() {

		String s = null;

		s = "ftp://" + ftpVO.getUser() + ":" + ftpVO.getPassword() + "@" + ftpVO.getServer() + ftpVO.getDirectory();

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