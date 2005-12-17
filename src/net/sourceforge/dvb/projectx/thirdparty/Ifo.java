/*
 * @(#)Ifo.java - carries various stuff 
 *
 * Copyright (c) 2004-2005 by dvb.matt,, All Rights Reserved. 
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

package net.sourceforge.dvb.projectx.thirdparty;


import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.File;

import java.util.zip.InflaterInputStream;

//DM10052004 081.7 int02 introduced
public final class Ifo extends Object {

	private Ifo()
	{}

	public static int RGBtoYUV(int ARGB)
	{
		int Y, Cr, Cb;

		int R = 0xFF & ARGB>>>16;
		int G = 0xFF & ARGB>>>8;
		int B = 0xFF & ARGB;

		Y  = (int)(0.299f * R +0.587f * G +0.114f * B);
		Cr = (int)(0.5f * R -0.4187f * G -0.0813f * B + 128);
		Cb = (int)(-0.1687f * R -0.3313f * G  +0.5f * B + 128);

		Y = Y < 16 ? 16 : (Y > 0xEB ? 0xEB : Y);
		Cr = Cr < 0 ? 0 : (Cr > 0xFF ? 0xFF : Cr);
		Cb = Cb < 0 ? 0 : (Cb > 0xFF ? 0xFF : Cb);

		if (Y == 0)
			return 0x108080;

		return (Y<<16 | Cr<<8 | Cb);
	}

	public static long createIfo(String file, Object color_table[]) throws IOException
	{
		file += ".IFO";

		FileOutputStream fos = new FileOutputStream(file);
		fos.write(setPGCColors(getDefaultIfo(), color_table));
		fos.flush();
		fos.close();

		return new File(file).length();
	}

	private static byte[] setPGCColors(byte ifo[], Object color_table[]) throws IOException
	{
		//VTS_PGC_1 starts at 0x1010, color_index 0 starts at offs 0xA5 (0x10B5)
		for (int a=0, color; a < 16 && a < color_table.length; a++)
		{
			color = RGBtoYUV(0xFFFFFF & Integer.parseInt(color_table[a].toString()));

			for (int b=0, val; b < 3; b++)
				ifo[0x10B5 + (a<<2) + b] = (byte)(0xFF & color>>(16-(b*8)));
		}

		return ifo;
	}

	private static byte[] getDefaultIfo() throws IOException
	{
		byte compressed_ifo[] = {
			 120, -100, -19, -103, 61, 72, 28, 65, 28, -59, -33, -100, -69, 123, -98, 119, -71, -81, -100, 31, 39, -63, 52, 41,
			 -124, -112, 70, -125, -88, -115, -122, -100, -127, 84, 6, 12, 7, -119, -126, 16, 8, -60, 70, 16, 3, 10, -79, 88, 59,
			 13, -110, 34, 90, 41, -104, 70, -71, -30, 20, 44, 20, -108, 36, 85, 16, -60, 38, -91, -74, 41, 82, 36, -28, 3, 82, 5,
			 -101, -28, -19, -50, 21, -79, 8, 22, 49, 30, -54, -5, -63, -17, 118, 102, 118, 119, -18, 63, -43, -50, -66, 45, 20, 11,
			 -59, -69, -123, -66, -2, 27, -59, -5, 3, 64, -13, 34, -114, -29, 34, -125, -1, 76, -12, -41, 41, 76, -30, 81, 67, 35,
			 -107, 126, -51, 31, -25, 28, 4, -21, -8, 11, 3, -41, 78, -31, -33, -49, 49, 92, -65, -23, 53, -113, 71, -85, 93, -121,
			 16, 66, -120, 51, -58, -124, -65, 41, -102, 96, -37, 84, -73, 24, 33, -124, 16, 66, -100, 9, -31, 19, -33, -28, -89, 109,
			 47, -51, 54, 71, 58, -17, 5, 29, -65, 106, 69, -99, 64, -38, -9, -15, -103, -50, -67, 27, -63, 108, 75, 27, 22, 39, -41,
			 -15, 126, 62, -114, -107, 120, 9, 75, -37, 107, 40, -81, -113, 97, -9, -31, -93, 112, 124, 107, 117, 25, 62, -81, 61, -50,
			 23, 124, 51, -98, -87, -60, 5, 118, -75, -106, -26, 86, -6, 66, 59, 33,  33, -124, 16, 23, 28, -5, -100, 107, -89, 9, 7,
			 -120, 1, -111, 36, -32, 44, 3, -34, 75, -96, -10, -128, -61, -20, -89, 62, 1, -39, 17, -96, 62, -30, -93, -87, 84, -35,
			 -126, -123, 16, 66, 8, -15, -49, -40, -25, -1, -27, -16, -75, 63, -124, 111, -65, 66, 8, 33, -124, -72, -32, -104, -37,
			 -107, 70, 31, 13, 62, 3, -68, -91, 63, 57, -34, 65, 39, -24, -122, -51, 4, 34, 79, -24, 26, 80, -109, -93, -93, -12, 13,
			 -32, -60, -24, 3, -101, 23, 56, -121, -128, -53, -21, 92, -50, -25, 62, -91, -101, -12, 7, -32, 93, -89, -61, 54, 79,
			 -16, -10, -128, 40, 55, 26, -47, 46, -70, 68, -65, 3, -75, 61, -12, -71, -51, 26, 98, 121, 58, 72, 121, -82, 46, 65, -57,
			 -23, 71, 32, 62, 101, 115, -120, 4, 107, -71, 116, -109, -14, -104, 100, 125, -55, 18, -112, 114, 105, -111, -66, -78, 57,
			 69, -70, -101, -50, -48, 15, 64, -122, 107, -54, -52, 83, -50, -99, -67, 98, 51, -116, -20, 22, 61, -30, -106, -25, 22, 93,
			 -96, 95, -127, -36, 29, 90, 14, -14, 13, 58, 68, 95, 3, 13, 87, -23, 51, -54, 123, 27, 89, 111, -29, 14, -48, -60, 99, -112,
			 127, -28, -21, -23, 24, -35, -73, 95, 12, -124, 16, 66, -120, -13, -59, 111, -73, 44, 80, 66
		};

		InflaterInputStream inflater = new InflaterInputStream( new ByteArrayInputStream(compressed_ifo));
		ByteArrayOutputStream uncompressed_ifo = new ByteArrayOutputStream();

		int x = 0;
		while ((x = inflater.read()) != -1)
			uncompressed_ifo.write(x);

		uncompressed_ifo.flush();

		return uncompressed_ifo.toByteArray();
	}
}