package xinput;

import java.util.Iterator;

public class XInputDirectory implements XInputDirectoryIF {

	// Implementation class
	private XInputDirectoryIF impl = null;

	private boolean debug = true;

	/**
	 * Private Constructor, don't use!
	 */
	private XInputDirectory() {
		throw new UnsupportedOperationException();
	}

	/**
	 */
	public XInputDirectory(Object aVO) {

		DirType dirType = null;
		Class[] parameterTypes = { aVO.getClass() };
		Object[] parameterValues = { aVO };

		for (Iterator dirTypes = DirType.getDirTypes().iterator(); dirTypes.hasNext();) {
			dirType = (DirType) dirTypes.next();

			if (dirType.equals(DirType.DEFAULT)) {
				continue;
			}

			try {
				if (debug) System.out.println("Try DirType '" + dirType.getName() + "'");
				impl = (XInputDirectoryIF) dirType.getImplementation().getConstructor(parameterTypes).newInstance(
						parameterValues);
				if (debug) System.out.println("Use DirType '" + dirType.getName() + "' for file '" + impl.toString() + "'");
				return;
			} catch (Exception e) {
				// Failed, try next type
				impl = null;
			}
		}
		try {
			dirType = DirType.DEFAULT;
			if (debug) System.out.println("Try default DirType '" + dirType.getName() + "'");
			impl = (XInputDirectoryIF) dirType.getImplementation().getConstructor(parameterTypes)
					.newInstance(parameterValues);
			if (debug)
					System.out.println("Use default DirType '" + dirType.getName() + "' for file '" + impl.toString() + "'");
			return;
		} catch (Exception e) {
			// Failed, no type left, so this is final failure
			impl = null;
			String s = "No matching DirType found or file doesn't exist";
			if (debug) System.out.println(s);
			throw new IllegalArgumentException(s);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object aObj) {
		return impl.equals(aObj);
	}

	/**
	 * @return
	 */
	public String getDirectory() {
		return impl.getDirectory();
	}

	/**
	 * @return
	 */
	public DirType getDirType() {
		return impl.getDirType();
	}

	/**
	 * @return
	 */
	public XInputFile[] getFiles() {
		return impl.getFiles();
	}

	/**
	 * @return
	 */
	public String getLog() {
		return impl.getLog();
	}

	/**
	 * @return
	 */
	public String getPassword() {
		return impl.getPassword();
	}

	/**
	 * @return
	 */
	public String getServer() {
		return impl.getServer();
	}

	/**
	 * @return
	 */
	public String getTestMsg() {
		return impl.getTestMsg();
	}

	/**
	 * @return
	 */
	public String getUser() {
		return impl.getUser();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return impl.hashCode();
	}

	/**
	 * @return
	 */
	public boolean test() {
		return impl.test();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return impl.toString();
	}
}