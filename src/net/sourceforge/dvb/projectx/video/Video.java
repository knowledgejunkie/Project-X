/*
 * @(#)Video.java - some video constants
 *
 * Copyright (c) 2003-2005 by dvb.matt, All Rights Reserved. 
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

import java.util.StringTokenizer;

public class Video extends Object {

	private final static String[] aspectratio_table_strings = { 
		"res." , "1:1" , "4:3" , "16:9" , "2.21:1" , "0.8055" , "0.8437" , "0.9375" , 
		"0.9815" , "1.0255" , "1.0695" , "1.1250" , "1.1575" , "1.2015" , "res." , "res." 
	};

	private final static String[] framerate_table_strings = { 
		"forbidden fps" , "23.976fps" , "24fps" , "25fps" , "29.97fps" , "30fps" , "50fps" , 
		"59.94fps" , "60fps" , "n.def." , "n.def." , "n.def." , "n.def." , "n.def." , "n.def." , "n.def."
	};

	private final static float[] aspectratio_table = { 
		1.0f, 1.0f, 1.3333f, 1.7778f, 2.21f, 0.8055f, 0.8437f, 0.9375f, 
		0.9815f, 1.0255f, 1.0695f, 1.125f, 1.1575f, 1.2015f, 1.0f, 1.0f 
	};

	private final static int[] framerate_table = { 
		-1, 23976, 24000, 25000, 29970, 30000, 50000, 59940, 60000, -1, -1, -1, -1, -1, -1, -1 
	};

	/**
	 * returns aspectratio as string
	 *
	 * @return
	 */
	public static String getAspectRatio(int index)
	{
		return aspectratio_table_strings[index];
	}

	/**
	 * returns aspectratio as string
	 *
	 * @return
	 */
	public static float getAspectRatioValue(int index)
	{
		return aspectratio_table[index];
	}

	/**
	 * returns framerate as string
	 *
	 * @return
	 */
	public static int getFrameRate(int index)
	{
		return framerate_table[index];
	}

	/**
	 * returns formatted display from sequence header
	 *
	 * @param1 - source array
	 * @return - string
	 */
	public static String getVideoformatfromBytes(byte[] gop)
	{
		return "" + ((0xFF & gop[4])<<4 | (0xF0 & gop[5])>>>4) + 
			"*" + ((0xF & gop[5])<<8 | (0xFF & gop[6])) + 
			", " + framerate_table_strings[0xF & gop[7]] + 
			", " + aspectratio_table_strings[(0xFF & gop[7])>>>4] + 
			", " + ( ((0xFF & gop[8])<<10 | (0xFF & gop[9])<<2 | (0xC0 & gop[10])>>>6) * 400  ) + 
			"bps, vbv " + ( (0x1F & gop[10])<<5 | (0xF8 & gop[11])>>>3 );
	}


	/**
	 * returns Sequence End Code as array
	 *
	 * @return
	 */
	public static byte[] getSequenceEndCode()
	{
		byte[] b = { 0, 0, 1, (byte)0xB7 };

		return b;
	}

	/**
	 * returns Sequence End Code as array
	 *
	 * @return
	 */
	public static byte[] getSequenceStartCode()
	{
		byte[] b = { 0, 0, 1, (byte)0xB3 };

		return b;
	}

	/**
	 * returns std Sequence Display Ext as array
	 *
	 * @return
	 */
	public static byte[] setSequenceDisplayExtension( String str, String[] videobasics)
	{
		byte[] b = { 0, 0, 1, (byte)0xB5, 0x2B, 2, 2, 2, 0, 0, 0, 0 };

		setSequenceDisplayExtension( b, 0, str, videobasics);

		return b;
	}

	/**
	 * returns std Sequence Display Ext as array
	 *
	 * @return
	 */
	public static void setSequenceDisplayExtension( byte[] b, int offs, String str, String[] videobasics) throws ArrayIndexOutOfBoundsException
	{
		int[] size = getHVSize( str, videobasics);

		offs += (1 & b[offs + 4]) != 0 ? 8 : 5;

		b[offs] = (byte) (0xFF & size[0]>>>6);
		b[offs + 1] = (byte) (0xFC & size[0]<<2);
		b[offs + 1] |= 2;
		b[offs + 1] |= (byte) (1 & size[0]>>>13);
		b[offs + 2] = (byte) (0xFF & size[1]>>>5);
		b[offs + 3] = (byte) (0xF8 & size[1]<<3);
	}

	private static int[] getHVSize(String str, String[] videobasics)
	{
		StringTokenizer st = new StringTokenizer(str, "*");
		int[] tokens = { 720, 576 };

		for (int i = 0, val; i < 2; i++)
		{
			try {
				val = Integer.parseInt(videobasics[i].trim());
				tokens[i] = val;
			} catch (Exception e) {
			}
		}

		for (int i = 0, val; st.hasMoreTokens() && i < 2; i++)
		{
			try {
				val = Integer.parseInt(st.nextElement().toString().trim());
				tokens[i] = val;
			} catch (Exception e) {
			}
		}

		return tokens;
	}
}