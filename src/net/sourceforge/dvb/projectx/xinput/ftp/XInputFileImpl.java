package net.sourceforge.dvb.projectx.xinput.ftp;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.net.URL;

import net.sourceforge.dvb.projectx.common.X;
import net.sourceforge.dvb.projectx.xinput.FileType;
import net.sourceforge.dvb.projectx.xinput.XInputFileIF;
import net.sourceforge.dvb.projectx.xinput.XInputStream;

import org.apache.commons.net.ftp.FTPFile;

public class XInputFileImpl implements XInputFileIF {

	private boolean debug = false;

	// Members, which are type independent
	private FileType fileType = null;

	private boolean isopen = false;

	private PushbackInputStream pbis = null;
	private File rafFile = null;
	private RandomAccessFile raf = null;
	private byte[] buffer = new byte[8];

	// Members used for type FileType.FTP
	private FtpVO ftpVO = null;

	private FTPFile ftpFile = null;

	/**
	 * Private Constructor, don't use!
	 */
	private XInputFileImpl() {

		throw new UnsupportedOperationException();
	}

	/**
	 * Create a XInputFile of type FileType.FTP.
	 * 
	 * @param aFtpVO
	 *          Directory data to use
	 * @param aFtpFile
	 *          File data to use
	 */
	public XInputFileImpl(FtpVO aFtpVO) {

		if (debug) System.out.println("Try to create XInputFile of Type FTP");

		ftpVO = aFtpVO;
		ftpFile = aFtpVO.getFtpFile();
		fileType = FileType.FTP;

		if (!exists()) { throw new IllegalArgumentException("File is not of type FileType.FTP"); }

		if (debug) System.out.println("Succeeded to create XInputFile of Type FTP");
	}

	/**
	 * Get String representation of the object.
	 * 
	 * @return String representation of the object
	 */
	public String toString() {

		String s;

		String name = ftpFile.getName();
		name = replaceStringByString(name, "Ã¤", "ä");
		name = replaceStringByString(name, "Ã¶", "ö");
		name = replaceStringByString(name, "Ã¼", "ü");
		name = replaceStringByString(name, "Ã„", "Ä");
		name = replaceStringByString(name, "Ã–", "Ö");
		name = replaceStringByString(name, "Ãœ", "Ü");
		name = replaceStringByString(name, "ÃŸ", "ß");
		name = replaceStringByString(name, "Ã¡", "á");
		name = replaceStringByString(name, "Ã ", "à");
		name = replaceStringByString(name, "Ã©", "é");
		name = replaceStringByString(name, "Ã¨", "è");
		name = replaceStringByString(name, "Ã­", "í");
		name = replaceStringByString(name, "Ã¬", "ì");
		name = replaceStringByString(name, "Ã³", "ó");
		name = replaceStringByString(name, "Ã²", "ò");
		name = replaceStringByString(name, "Ãº", "ú");
		name = replaceStringByString(name, "Ã¹", "ù");

		s = "ftp://" + ftpVO.getUser() + ":" + ftpVO.getPassword() + "@" + ftpVO.getServer() + ftpVO.getDirectory() + "/"
				+ name;

		return s;
	}

	/**
	 * @return String, checked of arg1 and replaced with arg2 JDK 1.2.2
	 *         compatibility, replacement of newer String.replaceAll()
	 */
	private String replaceStringByString(String name, String arg1, String arg2) {

		if (name == null) return name;

		StringBuffer sb = new StringBuffer(name);

		for (int i = 0; (i = sb.toString().indexOf(arg1, i)) != -1;)
			sb.replace(i, i + 2, arg2);

		return sb.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.sourceforge.dvb.projectx.xinput.XInputFileIF#getFileType()
	 */
	public FileType getFileType() {
		return fileType;
	}

	/**
	 * Get url representation of the object.
	 * 
	 * @return String with url
	 */
	public String getUrl() {

		String s;

		/**
		 * append the "type=b" string depending on the users wish, better for JRE 1.2.2 seems not parse it correctly
		 * usually TYPE I is set as std, so we don't ever need this appending
		 */
		String b = X.cBox[74].isSelected() ? ";type=b" : "";

	//	s = "ftp://" + ftpVO.getUser() + ":" + ftpVO.getPassword() + "@" + ftpVO.getServer() + ftpVO.getDirectory() + "/"
	//			+ ftpFile.getName() + ";type=b";

		s = "ftp://" + ftpVO.getUser() + ":" + ftpVO.getPassword() + "@" + ftpVO.getServer() + ftpVO.getDirectory() + "/"
				+ ftpFile.getName() + b;

		return s;
	}

	/**
	 * Length of file in bytes.
	 * 
	 * @return Length of file in bytes
	 */
	public long length() {

		return ftpFile.getSize();
	}

	/**
	 * Time in milliseconds from the epoch. JDK1.2.2 adaption: Date.getTime()
	 * 
	 * @return Time in milliseconds from the epoch
	 */
	public long lastModified() {

		// JDK 1.2.2 going trough Date.getTime(), Time is rounded or 0, but date
		// seems correct
		return ftpFile.getTimestamp().getTime().getTime();

		// JDK 1.4.2 return value long is not protected
		//return ftpFile.getTimestamp().getTimeInMillis();
	}

	/**
	 * Checks if file exists
	 * 
	 * @return Result of check
	 */
	public boolean exists() {

		boolean b = false;

		// This method is more exact, but too expensive
		//		try {
		//			b = true;
		//			inputStream = getInputStream();
		//			inputStream.close();
		//			inputStream = null;
		//		} catch (Exception e) {
		//			b = false;
		//		}

		// If ftpFile is set, it was possible to retrieve it, so the file exists
		if (ftpFile != null) {
			b = true;
		}

		return b;
	}

	/**
	 * Get Name of file
	 * 
	 * @return Name of file
	 */
	public String getName() {

		String s = null;

		s = ftpFile.getName();
		s = replaceStringByString(s, "Ã¤", "ä");
		s = replaceStringByString(s, "Ã¶", "ö");
		s = replaceStringByString(s, "Ã¼", "ü");
		s = replaceStringByString(s, "Ã„", "Ä");
		s = replaceStringByString(s, "Ã–", "Ö");
		s = replaceStringByString(s, "Ãœ", "Ü");
		s = replaceStringByString(s, "ÃŸ", "ß");
		s = replaceStringByString(s, "Ã¡", "á");
		s = replaceStringByString(s, "Ã ", "à");
		s = replaceStringByString(s, "Ã©", "é");
		s = replaceStringByString(s, "Ã¨", "è");
		s = replaceStringByString(s, "Ã­", "í");
		s = replaceStringByString(s, "Ã¬", "ì");
		s = replaceStringByString(s, "Ã³", "ó");
		s = replaceStringByString(s, "Ã²", "ò");
		s = replaceStringByString(s, "Ãº", "ú");
		s = replaceStringByString(s, "Ã¹", "ù");

		return s;
	}

	/**
	 * Get Path of parent
	 * 
	 * @return Path of parent
	 */
	public String getParent() {

		return ftpVO.getDirectory();
	}

	/**
	 * Get input stream from the file. close() on stream closes XInputFile, too.
	 * 
	 * @return Input stream from the file
	 */
	public InputStream getInputStream() throws FileNotFoundException, MalformedURLException, IOException {

		return new XInputStream((new URL(getUrl())).openConnection().getInputStream());
	}

	/**
	 * Opens XInputFile for random access
	 * 
	 * @param mode
	 *          Access mode as in RandomAccessFile
	 * @throws IOException
	 */
	public void randomAccessOpen(String mode) throws IOException {

		if (isopen) { throw new IllegalStateException("XInputFile is already open!"); }

		if (mode.compareTo("r") != 0) { throw new IllegalStateException("Illegal access mode for FileType.FTP"); }

		pbis = new PushbackInputStream(getInputStream());
		rafFile = File.createTempFile("XInputFile", "");
		raf = new RandomAccessFile(rafFile, "rw");
		isopen = true;
	}

	/**
	 * @throws java.io.IOException
	 */
	public void randomAccessClose() throws IOException {

		if (!isopen) { throw new IllegalStateException("XInputFile is already closed!"); }

		try {

		//	pbis.close();
	// close does nothing in InputStream, so we set it to null
	pbis = null;
	// call the GC here, otherwise the last user connection stands until next GC and blocks
	System.gc();

			raf.close();
			rafFile.delete();
		} catch (IOException e) {
			if (debug) System.out.println(e.getLocalizedMessage());
			if (debug) e.printStackTrace();
		} finally {
			pbis = null;
			raf = null;
			buffer = new byte[8];
			isopen = false;
		}

	}

	/**
	 * @param aPosition
	 *          The offset position, measured in bytes from the beginning of the
	 *          file, at which to set the file pointer.
	 * @throws java.io.IOException
	 */
	public void randomAccessSeek(long aPosition) throws IOException {

		if (aPosition > raf.length()) {
			fillRandomAccessBuffer(aPosition - raf.length());
		}
		raf.seek(aPosition);
	}

	/**
	 * @return @throws
	 *         IOException
	 */
	public long randomAccessGetFilePointer() throws IOException {
		return raf.getFilePointer();
	}

	/**
	 * @return @throws
	 *         IOException
	 */
	public int randomAccessRead() throws IOException {
		buffer[0] = -1;
		randomAccessRead(buffer, 0, 1);
		return (int) buffer[0];
	}

	/**
	 * @param aBuffer
	 *          The buffer into which the data is read.
	 * @return @throws
	 *         java.io.IOException
	 */
	public int randomAccessRead(byte[] aBuffer) throws IOException {
		return randomAccessRead(aBuffer, 0, aBuffer.length);
	}

	/**
	 * @param aBuffer
	 *          The buffer into which the data is written.
	 * @param aOffset
	 *          The offset at which the data should be written.
	 * @param aLength
	 *          The amount of data to be read.
	 * @return @throws
	 *         IOException
	 */
	public int randomAccessRead(byte[] aBuffer, int aOffset, int aLength) throws IOException {
		int result = 0;

		if ((raf.getFilePointer() + aLength) > raf.length()) {
			fillRandomAccessBuffer(raf.getFilePointer() + aLength - raf.length());
		}
		return raf.read(aBuffer, aOffset, aLength);
	}

	private void fillRandomAccessBuffer(long aAmount) throws IOException {
		int read = 0;
		long position = raf.getFilePointer();
		int requiredBufferSize = 65000;
		if (requiredBufferSize > buffer.length) {
			buffer = new byte[requiredBufferSize];
			if (debug) System.out.println("Buffer enhanced to " + requiredBufferSize + " bytes");
		}
		
		for (long l = 0; l < aAmount; ) {
			read = pbis.read(buffer, 0, requiredBufferSize);
			if (read == -1) {
				break;
			}
			raf.write(buffer, 0, read);
			l += read;
			if (read < requiredBufferSize) {
				break;
			}
		}
		raf.seek(position);
		if (debug) System.out.println("RandomAccessFile enhanced to " + raf.length() + " bytes");
	}
	
	/**
	 * @return Read line
	 * @throws IOException
	 */
	public String randomAccessReadLine() throws IOException {
		
		if ((raf.length() - raf.getFilePointer()) < 65000) {
			fillRandomAccessBuffer(65000);
		}
		return raf.readLine();
	}

	/**
	 * @param aBuffer
	 *          The data.
	 * @throws java.io.IOException
	 */
	public void randomAccessWrite(byte[] aBuffer) throws IOException {

		throw new IllegalStateException("Illegal access for FileType.FTP");
	}

	/**
	 * Convinience method for a single random read access to a input file. The
	 * file is opened before and closed after read.
	 * 
	 * @param aBuffer
	 *          Buffer to fill with read bytes (up to aBuffer.length() bytes)
	 * @param aPosition
	 *          Fileposition at which we want read
	 * @throws IOException
	 */
	public void randomAccessSingleRead(byte[] aBuffer, long aPosition) throws IOException {

		randomAccessOpen("r");
		randomAccessSeek(aPosition);
		randomAccessRead(aBuffer);
		randomAccessClose();
	}

	/**
	 * @return Long value read.
	 * @throws java.io.IOException
	 */
	public long randomAccessReadLong() throws IOException {

		long l = 0;

		int bytesRead = 0;

		bytesRead = randomAccessRead(buffer, 0, 8);
		if (bytesRead < 8) { throw new EOFException("Less than 8 bytes read"); }
		l = ((long) buffer[1] << 56) + ((long) buffer[2] << 48) + ((long) buffer[3] << 40) + ((long) buffer[4] << 32)
				+ ((long) buffer[5] << 24) + ((long) buffer[6] << 16) + ((long) buffer[7] << 8) + buffer[8];

		return l;
	}
}