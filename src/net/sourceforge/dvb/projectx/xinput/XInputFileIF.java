/*
 * Created on 24.08.2004
 *
 */
package net.sourceforge.dvb.projectx.xinput;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;

/**
 * @author Stefan
 *  
 */
public interface XInputFileIF {

	/**
	 * Get String representation of the object.
	 * 
	 * @return String representation of the object
	 */
	public String toString();

	/**
	 * Gets the type of this XInputFile implementation.
	 * 
	 * @return FileType
	 */
	public FileType getFileType();

	/**
	 * Get url representation of the object.
	 * 
	 * @return String with url
	 */
	public String getUrl();

	/**
	 * Length of file in bytes.
	 * 
	 * @return Length of file in bytes
	 */
	public long length();

	/**
	 * Time in milliseconds from the epoch.
	 * 
	 * @return Time in milliseconds from the epoch
	 */
	public long lastModified();

	/**
	 * Checks if file exists
	 * 
	 * @return Result of check
	 */
	public boolean exists();

	/**
	 * Get Name of file
	 * 
	 * @return Name of file
	 */
	public String getName();

	/**
	 * Get Path of parent
	 * 
	 * @return Path of parent
	 */
	public String getParent();

	/**
	 * Get input stream from the file. close() on stream closes XInputFile, too.
	 * 
	 * @return Input stream from the file
	 */
	public InputStream getInputStream() throws FileNotFoundException, MalformedURLException, IOException;

	/**
	 * Opens XInputFile for random access
	 * 
	 * @param mode
	 *          Access mode as in RandomAccessFile
	 * @throws IOException
	 */
	public void randomAccessOpen(String mode) throws IOException;

	/**
	 * @throws java.io.IOException
	 */
	public void randomAccessClose() throws IOException;

	/**
	 * @param aPosition
	 *          The offset position, measured in bytes from the beginning of the
	 *          file, at which to set the file pointer.
	 * @throws java.io.IOException
	 */
	public void randomAccessSeek(long aPosition) throws IOException;

	/**
	 * @return @throws
	 *         IOException
	 */
	public long randomAccessGetFilePointer() throws IOException;

	/**
	 * @return @throws
	 *         IOException
	 */
	public int randomAccessRead() throws IOException;

	/**
	 * @param aBuffer
	 *          The buffer into which the data is read.
	 * @return @throws
	 *         java.io.IOException
	 */
	public int randomAccessRead(byte[] aBuffer) throws IOException;

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
	public int randomAccessRead(byte[] aBuffer, int aOffset, int aLength) throws IOException;

	/**
	 * @return Read line
	 * @throws IOException
	 */
	public String randomAccessReadLine() throws IOException;

	/**
	 * @param aBuffer
	 *          The data.
	 * @throws java.io.IOException
	 */
	public void randomAccessWrite(byte[] aBuffer) throws IOException;

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
	public void randomAccessSingleRead(byte[] aBuffer, long aPosition) throws IOException;

	/**
	 * @return Long value read.
	 * @throws java.io.IOException
	 */
	public long randomAccessReadLong() throws IOException;
}