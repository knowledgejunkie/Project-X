package net.sourceforge.dvb.projectx.xinput;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class XInputStream extends FilterInputStream {

	private boolean debug = false;

	private byte[] buffer = new byte[1];


	/**
	 * Create stream, which is able to handle special needs of the xinput package.
	 * 
	 * @param aIs
	 *          InputStream
	 * @see java.io.FilterInputStream#FilterInputStream(InputStream in)
	 */
	public XInputStream(InputStream aIs) {
		super(aIs);
	}

	/**
	 * Takes care, that always the full amount of data is read (if possible).
	 * Blocks until it succeeds.
	 * 
	 * @see java.io.InputStream#read()
	 */
	public final int read() throws IOException {
		// byte[] buffer = new byte[1]; is now Attribute of class
		if (read(buffer, 0, 1) == 1)
			return (int) buffer[0];
		else
			return -1;
	}

	/**
	 * Takes care, that always the full amount of data is read (if possible).
	 * Blocks until it succeeds.
	 * 
	 * @param aBuffer
	 *          Buffer to fill with data
	 * @see java.io.InputStream#read(byte[])
	 */
	public final int read(byte[] aBuffer) throws IOException {
		return read(aBuffer, 0, aBuffer.length);
	}

	/**
	 * Takes care, that always the full amount of data is read (if possible).
	 * Blocks until it succeeds.
	 * 
	 * @param aBuffer
	 *          Buffer to keep data
	 * @param off
	 *          Offset in buffer
	 * @param len
	 *          Length of data to read
	 * @see java.io.InputStream#read(byte[], int, int)
	 */
	public final int read(byte[] aBuffer, int off, int len) throws IOException {

		if (debug) System.out.println("Enter read(aBuffer,off,len)");

		int result = 0;
		long read = 0;
		long readBytes = 0;
		long remaining = len;
//		byte[] streamBuffer = new byte[len];

		try {
			do {
				read = super.read(aBuffer, (int)(off + readBytes), (int) remaining);
				if (debug) System.out.println("    Bytes read in this cycle: " + read);
				if (read > 0) {
					readBytes += read;
//					System.arraycopy(streamBuffer, 0, aBuffer, (int) (len - remaining + off), (int) read);
					remaining -= read;
				}
			} while ((remaining > 0) && (read != -1));
			result = (int) (len - remaining);

			if ((read == -1) && (result == 0)) {
				if (debug) System.out.println("Leave read(aBuffer,off,len) returning -1");
				return -1;
			} else {
				if (debug) System.out.println("Leave read(aBuffer,off,len) returning " + result);
				return result;
			}
		} finally {
			if (debug && (readBytes != len))
					System.out.println("net.sourceforge.dvb.projectx.xinput.XInputStream.read(aBuffer,off,len): Bytes to read: "
							+ len + ", Read: " + readBytes + ", Difference: " + (len - readBytes) + "\n");
		}
	}

	/**
	 * @see java.io.InputStream#close()
	 */
	public final void close() throws IOException {
		super.close();
		if (debug) System.out.println("InputStream closed!\n");
	}

	/**
	 * @see java.io.InputStream#skip(long)
	 */
	public final long skip(long n) throws IOException {

		boolean debug = true;

		if (debug) System.out.println("Enter skip(" + n + ")");

		long retryCount = 0;
		long skiped = 0;
		long skipedBytes = 0;
		long remaining = n;

		try {
			do {
				skiped = super.skip(remaining);
				if (debug) System.out.println("    Bytes skiped in this cycle: " + skiped);
				skipedBytes += skiped;
				remaining -= skiped;
				if (skiped == 0) {
					retryCount++;
				} else {
					retryCount = 0;
				}
			} while ((remaining > 0) && (retryCount <= 10));
			if (debug) System.out.println("Leave skip(" + n + ") returning " + skipedBytes);
			return skipedBytes;
		} finally {
			if (debug && (skipedBytes != n))
					System.out.println("net.sourceforge.dvb.projectx.xinput.XInputStream.skip(n):\n  Bytes to skip: " + n
							+ ", Skiped: " + skipedBytes + ", Difference: " + (n - skipedBytes) + "\n");
		}
	}
}