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
//DM24062004 081.7 int05 now required, but w/o additional access it must return 'false'

import java.awt.*;
import java.io.*;
import java.util.*;

class RawRead {

	// check DLL load status and errors
	native public String GetLoadStatus();

	// directory read functions
	native public int findOpen(String directoryname);
	native public String findNextFile(int findhandle);
	native public int findClose(int findhandle);

	// file read functions
	native public int openFile(String filename);
	native public int readFile(int filehandle,byte[] array,int offsetinbuffer,int readlen);
	native public int closeFile(int filehandle);
	native public long skipBytes(int filehandle,long skipbytes);
	native public long getFileSize(String filename);
	native public long lastModified(String filename);

	static boolean DllLoaded;

	public void add_native_files(ArrayList arraylist)
	{
		String s;
		int i=0;
		int hdl=findOpen("\\");

		if (hdl!=0)
		{
			do
			{
				s=findNextFile(hdl);

				if (s.length()!=0)
					arraylist.add(s);

				i++;

				if (i>100)
					break;
			}
			while (s.length()!=0);

			findClose(hdl);
		}

		return;
	}

	public boolean isAccessibleDisk(String sourcefile)
	{
		// make sure it's not a PC file (c: or UNC prefix) and not a Linux file (/ prefix)
		// support of URLs untested ( file:// , http:// .. )
		if (DllLoaded && sourcefile.charAt(1) != ':' && sourcefile.charAt(1) != '\\' && sourcefile.charAt(0) != '/')
			return true;

		else
			return false;
	}

	public boolean AccessEnabled()
	{
		return DllLoaded;
	}

	// Loads the file Native.DLL at run-time
	// don't load if access isn't required
	static {
		try 
		{
			System.loadLibrary("Rawread");
			DllLoaded=true;
		}
		catch (UnsatisfiedLinkError exc)
		{
			DllLoaded=false;
		}
	}

	// Constructor
	public RawRead()
	{}
}

