/*
 * Created on 26.08.2004
 *
 */
package net.sourceforge.dvb.projectx.xinput;

/**
 * @author Stefan
 *  
 */
public interface XInputDirectoryIF {

	/**
	 * Get String representation of the object.
	 * 
	 * @return String representation of the object
	 */
	public String toString();

	/**
	 * Get path of directory
	 * 
	 * @return Path of directory
	 */
	public String getDirectory();

	/**
	 * Get password for the ftp server
	 * 
	 * @return Password for the ftp server
	 * @throws IllegalStateException
	 *           If file type of object is not FileType.FTP_DIR
	 */
	public String getPassword();

	/**
	 * Get name or ip address of the ftp server
	 * 
	 * @return Name or ip address of the ftp server
	 * @throws IllegalStateException
	 *           If file type of object is not FileType.FTP_DIR
	 */
	public String getServer();

	/**
	 * Get user for the ftp server
	 * 
	 * @return User for the ftp server
	 * @throws IllegalStateException
	 *           If file type of object is not FileType.FTP_DIR
	 */
	public String getUser();

	/**
	 * Get log of communication with ftp server.
	 * 
	 * @return Log of communication with ftp server
	 * @throws IllegalStateException
	 *           If file type of object is not FileType.FTP_DIR
	 */
	public String getLog();

	/**
	 * Get files in the directory.
	 * 
	 * @return files in the directory
	 */
	public XInputFile[] getFiles();

	/**
	 * Test if directory data is valid.
	 * 
	 * @return Test successful or not
	 */
	public boolean test();

	/**
	 * Get result message after test().
	 * 
	 * @return result message after test()
	 */
	public String getTestMsg();

	/**
	 * @return Type of XInputDirectory
	 */
	public DirType getDirType();
}