package net.sourceforge.dvb.projectx.xinput;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.Iterator;

public class XInputFile implements XInputFileIF {

	// Implementation class
	private XInputFileIF impl = null;

	private boolean debug = true;

	private Object constructorParameter = null;

	/**
	 * Private Constructor, don't use!
	 */
	private XInputFile() {
		throw new UnsupportedOperationException();
	}

	/**
	 */
	public XInputFile(Object aVO) {
		if (debug) System.out.println("Enter XInputFile(Object '" + aVO + "')");

		Class[] parameterTypes = { aVO.getClass() };
		Object[] parameterValues = { aVO };
		retrieveImplementation(parameterTypes, parameterValues);
		constructorParameter = aVO;

		if (debug) System.out.println("Leave XInputFile(Object '" + aVO + "')");
	}

	public XInputFile getNewInstance() {
		if (debug) System.out.println("Enter XInputFile.getNewInstance()");
		XInputFile xif = new XInputFile(constructorParameter);
		if (debug) System.out.println("Leave XInputFile.getNewInstance() returning " + xif);
		return xif;
	}

	/**
	 */
	private void retrieveImplementation(Class[] parameterTypes, Object[] parameterValues) {
		if (debug)
				System.out.println("Enter XInputFile.retrieveImplementation(Class[] parameterTypes, Object[] parameterValues)");

		FileType fileType = null;

		for (Iterator fileTypes = FileType.getFileTypes().iterator(); fileTypes.hasNext();) {
			fileType = (FileType) fileTypes.next();

			if (fileType.equals(FileType.DEFAULT)) {
				continue;
			}

			try {
				if (debug) System.out.println("Try FileType '" + fileType.getName() + "'");
				impl = (XInputFileIF) fileType.getImplementation().getConstructor(parameterTypes).newInstance(parameterValues);
				if (debug) System.out.println("Use FileType '" + fileType.getName() + "' for file '" + impl.toString() + "'");
				if (debug)
						System.out
								.println("Leave XInputFile.retrieveImplementation(Class[] parameterTypes, Object[] parameterValues)");
				return;
			} catch (Exception e) {
				// Failed, try next type
				impl = null;
			}
		}
		try {
			fileType = FileType.DEFAULT;
			if (debug) System.out.println("Try default FileType '" + fileType.getName() + "'");
			impl = (XInputFileIF) fileType.getImplementation().getConstructor(parameterTypes).newInstance(parameterValues);
			if (debug)
					System.out.println("Use default FileType '" + fileType.getName() + "' for file '" + impl.toString() + "'");
			if (debug)
					System.out
							.println("Leave XInputFile.retrieveImplementation(Class[] parameterTypes, Object[] parameterValues)");
			return;
		} catch (Exception e) {
			// Failed, no type left, so this is final failure
			impl = null;
			if (debug) System.out.println("No matching FileType found or file doesn't exist");
			if (debug)
					System.out
							.println("Leave XInputFile.retrieveImplementation(Class[] parameterTypes, Object[] parameterValues)");
			throw new IllegalArgumentException("No matching FileType found or file doesn't exist");
		}
	}

	/**
	 * @return
	 */
	public boolean exists() {
		if (debug) System.out.println("Enter XInputFile.exists()");
		boolean b = impl.exists();
		if (debug) System.out.println("Leave XInputFile.exists() returning " + (new Boolean(b)));
		return b;
	}

	/**
	 * @return @throws
	 *         FileNotFoundException
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	public InputStream getInputStream() throws FileNotFoundException, MalformedURLException, IOException {
		if (debug) System.out.println("Enter XInputFile.getInputStream()");
		InputStream is = null;
		try {
			is = impl.getInputStream();
		} catch (IOException e) {
			if (debug) System.out.println(e.getLocalizedMessage());
			if (debug) e.printStackTrace();
			throw e;
		}
		if (debug) System.out.println("Leave XInputFile.getInputStream() returning " + is);
		return is;
	}

	/**
	 * @return
	 */
	public String getName() {
		if (debug) System.out.println("Enter XInputFile.getName()");
		String s = impl.getName();
		if (debug) System.out.println("Leave XInputFile.getName() returning " + s);
		return s;
	}

	/**
	 * @return
	 */
	public String getParent() {
		if (debug) System.out.println("Enter XInputFile.getParent()");
		String s = impl.getParent();
		if (debug) System.out.println("Leave XInputFile.getParent() returning " + s);
		return s;
	}

	/**
	 * @return
	 */
	public String getUrl() {
		if (debug) System.out.println("Enter XInputFile.getUrl()");
		String s = impl.getUrl();
		if (debug) System.out.println("Leave XInputFile.getUrl() returning " + s);
		return s;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		if (debug) System.out.println("Enter XInputFile.hashCode()");
		int i = impl.hashCode();
		if (debug) System.out.println("Leave XInputFile.hashCode() returning " + i);
		return i;
	}

	/**
	 * @return
	 */
	public long lastModified() {
		if (debug) System.out.println("Enter XInputFile.lastModified()");
		long l = impl.lastModified();
		if (debug) System.out.println("Leave XInputFile.lastModified() returning " + l);
		return l;
	}

	/**
	 * @return
	 */
	public long length() {
		if (debug) System.out.println("Enter XInputFile.length()");
		long l = impl.length();
		if (debug) System.out.println("Leave XInputFile.length() returning " + l);
		return l;
	}

	/**
	 * rename
	 * 
	 * @return 
	 */
	public boolean rename() throws IOException {
		if (debug) System.out.println("Enter XInputFile.rename()");
		boolean b = impl.rename();
		if (debug) System.out.println("Leave XInputFile.rename() returning " + b);
		return b;
	}

	/**
	 * @throws IOException
	 */
	public void randomAccessClose() throws IOException {
		if (debug) System.out.println("Enter XInputFile.randomAccessClose()");
		try {
			impl.randomAccessClose();
		} catch (IOException e) {
			if (debug) System.out.println(e.getLocalizedMessage());
			if (debug) e.printStackTrace();
			throw e;
		}
		if (debug) System.out.println("Leave XInputFile.randomAccessClose()");
	}

	/**
	 * @param aMode
	 * @throws IOException
	 */
	public void randomAccessOpen(String aMode) throws IOException {
		if (debug) System.out.println("Enter XInputFile.randomAccessOpen(String '" + aMode + "')");
		try {
			impl.randomAccessOpen(aMode);
		} catch (IOException e) {
			if (debug) System.out.println(e.getLocalizedMessage());
			if (debug) e.printStackTrace();
			throw e;
		}
		if (debug) System.out.println("Leave XInputFile.randomAccessOpen(String '" + aMode + "')");
	}

	/**
	 * @return @throws
	 *         IOException
	 */
	public int randomAccessRead() throws IOException {
		if (debug) System.out.println("Enter XInputFile.randomAccessRead()");
		int i = 0;
		try {
			i = impl.randomAccessRead();
		} catch (IOException e) {
			if (debug) System.out.println(e.getLocalizedMessage());
			if (debug) e.printStackTrace();
			throw e;
		}
		if (debug) System.out.println("Leave XInputFile.randomAccessRead() returning " + i);
		return i;
	}

	/**
	 * @param aBuffer
	 * @return @throws
	 *         IOException
	 */
	public int randomAccessRead(byte[] aBuffer) throws IOException {
		if (debug) System.out.println("Enter XInputFile.randomAccessRead(byte[] aBuffer)");
		int i = 0;
		try {
			i = impl.randomAccessRead(aBuffer);
		} catch (IOException e) {
			if (debug) System.out.println(e.getLocalizedMessage());
			if (debug) e.printStackTrace();
			throw e;
		}
		if (debug) System.out.println("Leave XInputFile.randomAccessRead(byte[] aBuffer) returning " + i);
		return i;
	}

	/**
	 * @param aBuffer
	 * @param aOffset
	 * @param aLength
	 * @return @throws
	 *         IOException
	 */
	public int randomAccessRead(byte[] aBuffer, int aOffset, int aLength) throws IOException {
		if (debug) System.out.println("Enter XInputFile.randomAccessRead(byte[] aBuffer, int aOffset, int aLength)");
		int i = 0;
		try {
			i = impl.randomAccessRead(aBuffer, aOffset, aLength);
		} catch (IOException e) {
			if (debug) System.out.println(e.getLocalizedMessage());
			if (debug) e.printStackTrace();
			throw e;
		}
		if (debug)
				System.out
						.println("Leave XInputFile.randomAccessRead(byte[] aBuffer, int aOffset, int aLength) returning " + i);
		return i;
	}

	/**
	 * @return Read line
	 * @throws IOException
	 */
	public String randomAccessReadLine() throws IOException {
		if (debug) System.out.println("Enter XInputFile.randomAccessReadLine()");
		String s = null;
		try {
			s = impl.randomAccessReadLine();
		} catch (IOException e) {
			if (debug) System.out.println(e.getLocalizedMessage());
			if (debug) e.printStackTrace();
			throw e;
		}
		if (debug) System.out.println("Leave XInputFile.randomAccessReadLine() returning " + s);
		return s;
	}

	/**
	 * @param aPosition
	 * @throws IOException
	 */
	public void randomAccessSeek(long aPosition) throws IOException {
		if (debug) System.out.println("Enter XInputFile.randomAccessSeek(long '" + aPosition + "')");
		try {
			impl.randomAccessSeek(aPosition);
		} catch (IOException e) {
			if (debug) System.out.println(e.getLocalizedMessage());
			if (debug) e.printStackTrace();
			throw e;
		}
		if (debug) System.out.println("Leave XInputFile.randomAccessSeek(long '" + aPosition + "')");
	}

	/**
	 * @return @throws
	 *         IOException
	 */
	public long randomAccessGetFilePointer() throws IOException {
		if (debug) System.out.println("Enter XInputFile.randomAccessGetFilePointer()");
		long l = 0;
		try {
			l = impl.randomAccessGetFilePointer();
		} catch (IOException e) {
			if (debug) System.out.println(e.getLocalizedMessage());
			if (debug) e.printStackTrace();
			throw e;
		}
		if (debug) System.out.println("Leave XInputFile.randomAccessGetFilePointer() returning " + l);
		return l;
	}

	/**
	 * @param aBuffer
	 * @param aPosition
	 * @throws IOException
	 */
	public void randomAccessSingleRead(byte[] aBuffer, long aPosition) throws IOException {
		if (debug) System.out.println("Enter XInputFile.randomAccessSingleRead(byte[] aBuffer, long aPosition)");
		try {
			impl.randomAccessSingleRead(aBuffer, aPosition);
		} catch (IOException e) {
			if (debug) System.out.println(e.getLocalizedMessage());
			if (debug) e.printStackTrace();
			throw e;
		}
		if (debug) System.out.println("Leave XInputFile.randomAccessSingleRead(byte[] aBuffer, long aPosition)");
	}

	/**
	 * @param aBuffer
	 * @throws IOException
	 */
	public void randomAccessWrite(byte[] aBuffer) throws IOException {
		if (debug) System.out.println("Enter XInputFile.randomAccessWrite(byte[] aBuffer)");
		try {
			impl.randomAccessWrite(aBuffer);
		} catch (IOException e) {
			if (debug) System.out.println(e.getLocalizedMessage());
			if (debug) e.printStackTrace();
			throw e;
		}
		if (debug) System.out.println("Leave XInputFile.randomAccessWrite(byte[] aBuffer)");
	}

	/**
	 * @return @throws
	 *         IOException
	 */
	public long randomAccessReadLong() throws IOException {
		if (debug) System.out.println("Enter XInputFile.readLong()");
		long l = 0;
		try {
			l = impl.randomAccessReadLong();
		} catch (IOException e) {
			if (debug) System.out.println(e.getLocalizedMessage());
			if (debug) e.printStackTrace();
			throw e;
		}
		if (debug) System.out.println("Leave XInputFile.readLong() returning " + l);
		return l;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object aObj) {
		if (debug) System.out.println("Enter XInputFile.equals(Object '" + aObj + "')");

		if (!(aObj instanceof XInputFile)) {
			if (debug) System.out.println("Leave XInputFile.equals(Object '" + aObj + "') returning false");
			return false;
		}
		XInputFile other = (XInputFile) aObj;
		if (other.getFileType().equals(getFileType()) && other.toString().equals(toString())) {
			if (debug) System.out.println("Leave XInputFile.equals(Object '" + aObj + "') returning true");
			return true;
		} else {
			if (debug) System.out.println("Leave XInputFile.equals(Object '" + aObj + "') returning false");
			return false;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.sourceforge.dvb.projectx.xinput.XInputFileIF#getFileType()
	 */
	public FileType getFileType() {
		if (debug) System.out.println("Enter XInputFile.getFileType()");
		FileType ft = impl.getFileType();
		if (debug) System.out.println("Leave XInputFile.getFileType() returning " + ft);
		return ft;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		if (debug) System.out.println("Enter XInputFile.toString()");
		String s = impl.toString();
		if (debug) System.out.println("Leave XInputFile.toString() returning " + s);
		return s;
	}
}