/*
 * @(#)IDCT SSE
 * 
 * Copyright (c) 2004-2005 by pstorch, All Rights Reserved. 
 *
 * This file is part of ProjectX, a free Java based demux utility.
 * By the authors, ProjectX is intended for educational purposes only, 
 * as a non-commercial test project.
 * 
 *
 * This program is free software; you can redistribute it and/or modify 
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */

package net.sourceforge.dvb.projectx.video;

/**
 * JNI java class to access idctsse.dll.
 * 
 * @author Peter Storch
 */
public class IDCTSseNative {
	
	/** indicates whether the library is loaded */
	private static boolean libraryLoaded = false;
	
	private static boolean debug = false;

	/* load the native libray */
	static
	{
		try
		{
			System.loadLibrary("idctsse");
			libraryLoaded = true;
		}
		catch(Exception e)
		{
			if (debug)
				System.out.println(e);
			//e.printStackTrace();
		}
		catch(UnsatisfiedLinkError ue)
		{
			if (debug)
				System.out.println(ue);
			//e.printStackTrace();
		}
	}
	
	/**
	 * Returns true if the library is loaded.
	 * 
	 * @return
	 */
	public static boolean isLibraryLoaded()
	{
		return libraryLoaded;
	}
	
	/**
	 * Performs the IDCT on an short[64] array.
	 * 
	 * @param block
	 * @return
	 */
	public native void referenceIDCT(short[] block);

	/**
	 * Main method for testing.
	 * 
	 * @param args
	 */
	public static void main(String args[])
	{
		IDCTSseNative idct = new IDCTSseNative();

		short[] block = new short[64];

		for (int i = 0, k = 0; i < 8; i++)
		{
			for (int j = 0; j < 8; j++)
			{
				block[k]=(short)(i*j);
				k++;
			}
		}
		System.out.println("block before");
		printArray(block);
		idct.referenceIDCT(block);
		System.out.println("block after");
		printArray(block);
	}
	
	/**
	 * Helper method to print the array.
	 * 
	 * @param block
	 */
	public static void printArray(short[] block)
	{
		for (int i = 0; i < 64; i++)
		{
			if (i % 8 == 0)
			{
				System.out.println();
			}
			System.out.print(block[i] + " ");
		}
		System.out.println();
	}
}
