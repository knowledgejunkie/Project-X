package net.sourceforge.dvb.projectx.xinput.topfield_raw;

import java.util.ArrayList;
import java.util.Iterator;

import net.sourceforge.dvb.projectx.xinput.DirType;
import net.sourceforge.dvb.projectx.xinput.XInputDirectoryIF;
import net.sourceforge.dvb.projectx.xinput.XInputFile;

public class XInputDirectoryImpl implements XInputDirectoryIF {

	private DirType dirType = null;

	private String testMsg = null;

	private RawInterface rawInterface = null;

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
	 * Create a XInputDirectory of type DirType.TFRAW_DIR.
	 * 
	 * @param aFtpVO
	 *          Directory data to use
	 */
	public XInputDirectoryImpl(DirType aDirType) {

		if (aDirType != DirType.TFRAW_DIR) { throw new IllegalArgumentException("aDirType is not DirType.TFRAW_DIR"); }

		dirType = aDirType;
		rawInterface = new RawInterface("");

		if (!rawInterface.rawRead.AccessEnabled()) { throw new IllegalArgumentException(
				"Topfield raw disk access is not enabled"); }
	}

	/**
	 * Get String representation of the object.
	 * 
	 * @return String representation of the object
	 */
	public String toString() {

		return "Topfield raw disk access";
	}

	/**
	 * Get path of directory
	 * 
	 * @return Path of directory
	 */
	public String getDirectory() {

		return "";
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

		ArrayList arrayList = new ArrayList();
		rawInterface.add_native_files(arrayList);

		XInputFile[] xInputFiles = new XInputFile[arrayList.size()];
		int i = 0;

		for (Iterator it = arrayList.iterator(); it.hasNext();) {
			xInputFiles[i++] = new XInputFile((String) it.next());
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

		b = rawInterface.rawRead.AccessEnabled();
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