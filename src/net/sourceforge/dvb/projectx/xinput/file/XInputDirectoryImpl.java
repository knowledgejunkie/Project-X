package net.sourceforge.dvb.projectx.xinput.file;

import java.io.File;
import java.io.FileFilter;

import net.sourceforge.dvb.projectx.xinput.DirType;
import net.sourceforge.dvb.projectx.xinput.XInputDirectoryIF;
import net.sourceforge.dvb.projectx.xinput.XInputFile;

public class XInputDirectoryImpl implements XInputDirectoryIF {

	private DirType dirType = null;

	private File file = null;

	private String testMsg = null;

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
	 * Create a XInputDirectory of type DirType.FILE_DIR.
	 * 
	 * @param aFile
	 *          Directory data to use
	 * @throws IllegalArgumentException
	 *           If aFile is not a directory
	 */
	public XInputDirectoryImpl(File aFile) {
		if (aFile.exists() && aFile.isDirectory()) {
			dirType = DirType.FILE_DIR;
			file = aFile;
		} else {
			throw new IllegalArgumentException("aFile is not a directory!");
		}
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

		File f = new File(aFileIdentifier);
		if (f.exists() && f.isDirectory()) {
			dirType = DirType.FILE_DIR;
			file = f;
		} else {
			throw new IllegalArgumentException("'" + aFileIdentifier + "' is not a directory!");
		}
	}

	/**
	 * Get String representation of the object.
	 * 
	 * @return String representation of the object
	 */
	public String toString() {

		return file.getAbsolutePath();
	}

	/**
	 * Get path of directory
	 * 
	 * @return Path of directory
	 */
	public String getDirectory() {

		return file.getParent();
	}

	/**
	 * Get password for the ftp server
	 * 
	 * @return Password for the ftp server
	 * @throws IllegalStateException
	 *           If file type of object is not DirType.FTP_DIR
	 */
	public String getPassword() {

		return "";
	}

	/**
	 * Get name or ip address of the ftp server
	 * 
	 * @return Name or ip address of the ftp server
	 * @throws IllegalStateException
	 *           If file type of object is not DirType.FTP_DIR
	 */
	public String getServer() {

		return "";
	}

	/**
	 * Get user for the ftp server
	 * 
	 * @return User for the ftp server
	 * @throws IllegalStateException
	 *           If file type of object is not DirType.FTP_DIR
	 */
	public String getUser() {

		return "";
	}

	/**
	 * Get log of communication with ftp server.
	 * 
	 * @return Log of communication with ftp server
	 * @throws IllegalStateException
	 *           If file type of object is not DirType.FTP_DIR
	 */
	public String getLog() {

		return "";
	}

	/**
	 * Get files in the directory.
	 * 
	 * @return files in the directory
	 */
	public XInputFile[] getFiles() {

		XInputFile[] xInputFiles = null;

		class MyFileFilter implements FileFilter {

			public boolean accept(File pathname) {
				return pathname.isFile();
			}
		}
		File[] files = file.listFiles(new MyFileFilter());
		xInputFiles = new XInputFile[files.length];
		for (int i = 0; i < files.length; i++) {
			xInputFiles[i] = new XInputFile(files[i]);
		}

		return xInputFiles;
	}

	/**
	 * Test if directory data is valid.
	 * 
	 * @return Test successful or not
	 */
	public boolean test() {

		boolean b = false;

		b = (file.exists() && file.isDirectory()) ? true : false;
		testMsg = b ? "Test succeeded" : "Test failed";

		return b;
	}

	/**
	 * Get result message after test().
	 * 
	 * @return result message after test()
	 */
	public String getTestMsg() {

		return testMsg;
	}

	/**
	 * @return Type of XInputDirectory
	 */
	public DirType getDirType() {
		return dirType;
	}

}