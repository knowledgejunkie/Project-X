/****************************************************************************
 *  
 *  Rawread.java
 *
 *  This code implements the interface to the C++ code. 
 *
 *  This code was developed by chrisg to access a Topfield 4000
 *
 *  Updated:
 *    2004-04-09 Adapted to show load status
 *    2004-01-25 Initial implementation
 *
*/

//package X
//DM24062004 081.7 int05 now required, but w/o additional it will never be called

import java.awt.*;
import java.io.*;
import java.util.*;

class RawFileInputStream extends InputStream {
	String file;
	int handle;
	long currentpos;
	RawRead Rawread;

	public int read(byte[] b, int off, int len) throws IOException {
		len = Rawread.readFile(handle,b,off,len);
		currentpos+=len;
		return len;
	}

	public int read(byte[] b) throws IOException {
		return read(b, 0, b.length);
	}

	public int read() throws IOException {
		byte[] b=new byte[1];
		if (read(b)==1)
			return (int)b[0];
		else
			return -1;
	}

	public int available() throws IOException {
		long avail=streamSize()-currentpos;
		if (avail>2*1024*1024*104)
			return 2*1024*1024*104;
		else
			return (int)avail;
	}

	public void mark(int readlimit) {
		// not implemented
	}

	public void reset() throws IOException {
		// not implemented
	}

	public boolean markSupported() {
		return false;
	}

	public long skip(long n) throws IOException {

		long skipped = Rawread.skipBytes(handle,n);
		currentpos+=skipped;
		return skipped;
	}

	//alias required
	public long streamSize() throws IOException {

		return Rawread.getFileSize(file);
	}

	RawFileInputStream(RawRead Rawread,String file) throws FileNotFoundException {
		this.Rawread=Rawread;
		this.file=file;
		this.handle=Rawread.openFile(this.file);
		currentpos=0;
	}


	public void close() throws IOException {
		if (handle!=0)
			Rawread.closeFile(handle);
		handle=0;
	}
}

// Note: the buffered stream below is NOT faster!

/* 
class RawFileInputStream extends InputStream {
    String file;
    int handle;
    long currentpos;
	RAWREAD Rawread;
    int readbufsize=256*1024;
    byte[] readbuf = new byte[readbufsize];
    int datainbuf;
    int posinbuf;

    public int read(byte[] b, int off, int len) throws IOException {
        int remain=len;
        int offout=off;
        int datacopied=0;
        while (remain>0) {
            int copydata=datainbuf-posinbuf;
            if (copydata>remain)
                copydata=remain;
            if (copydata>0) {
                System.arraycopy(readbuf,posinbuf,b,offout,copydata);
                remain-=copydata;
                offout+=copydata;
                posinbuf+=copydata;
                datacopied+=copydata;
            }
            if (remain>0) {
                datainbuf = Rawread.readFile(handle,readbuf,0,readbufsize);
                posinbuf = 0;
                if (datainbuf<=0)
                    break;   // no more data!
            }
        }
        currentpos+=datacopied;
        return datacopied;
    }

    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    public int read() throws IOException {
        byte[] b=new byte[1];
        if (read(b)==1)
            return (int)b[0];
	else
            return -1;
    }

    public long skip(long n) throws IOException {
        long skipped;
        int skipinbuf=datainbuf-posinbuf;
        if (skipinbuf>=n) {
            posinbuf+=n;
            skipped=n;
        } else {
            skipped = Rawread.skipBytes(handle,n-skipinbuf)+skipinbuf;
            posinbuf=0;
            datainbuf=0;
        }
        currentpos+=skipped;
        return skipped;
    }

    public long streamSize() throws IOException {

        return Rawread.getFileSize(file);
    }

    RawFileInputStream(RAWREAD Rawread,String file) throws FileNotFoundException {
        this.Rawread=Rawread;
        this.file=file;
        this.handle=Rawread.openFile(this.file);
        currentpos=0;
        datainbuf=0;
        posinbuf=0;
    }


    public void close() throws IOException {
        if (handle!=0)
            Rawread.closeFile(handle);
        handle=0;
    }
}
*/
