/*
 * @(#)BMP.java - carries BMP stuff, access from 'all sides' 
 *
 * Copyright (c) 2004-2013 by dvb.matt, All Rights Reserved.
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

package net.sourceforge.dvb.projectx.subtitle;


//DM24042004 081.7 int02 introduced

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Enumeration;

public class BMP extends Object {

	private static final byte defaultHeader[] = {
		0x42, 0x4D, //'B','M'
		0, 0, 0, 0, // real filesize 32bit, little endian (real size*3 + header(0x36))
		0, 0, 0, 0, 
		0x36, 0, 0, 0, //bitmap info size
		0x28, 0, 0, 0, 
		0, 0, 0, 0, //hsize
		0, 0, 0, 0, //vsize
		1, 0,  //nplane
		0x18, 0, //bitcount 24b
		0, 0, 0, 0, //ncompr
		0, 0, 0, 0, //image bytesize
		(byte)0x88, 0xB, 0, 0, //nxpm 75dpi
		(byte)0x88, 0xB, 0, 0, //nypm 75dpi
		0, 0, 0, 0, //nclrused
		0, 0, 0, 0  //nclrimp
	};

	private static Hashtable bmps = new Hashtable();

	private BMP()
	{}

	public static String getContents()
	{
		return bmps.toString();
	}

	public static Enumeration getKeys()
	{
		return bmps.keys();
	}

	public static boolean isEmpty()
	{
		return bmps.isEmpty();
	}

	public static void clear()
	{
		bmps.clear();
	}

	public static void savePixels(Bitmap bitmap)
	{
		bmps.put("" + bitmap.getId(), bitmap);
	}

	public static Bitmap getBitmap(int id)
	{
		return (Bitmap)bmps.get("" + id);
	}

	private static void littleEndian(byte[] array, int aPos, int value)
	{
		for (int a=0; a<4; a++)
			array[aPos+a] = (byte)(value>>(a*8) & 0xFF);
	}

	public static void buildBMP_24bit(String outfile, String key) throws IOException
	{
		Bitmap bitmap = (Bitmap)bmps.get(key);

		if (bitmap == null)
			return;

		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		int size = 3 * width * height + height * (width & 3);

		if (size == 0)
			return;

		int pixels[] = bitmap.getPixel();

		BufferedOutputStream out = new BufferedOutputStream( new FileOutputStream( outfile + ".bmp"), 65535);

		byte BMPheader[] = new byte[defaultHeader.length];
		System.arraycopy(defaultHeader, 0, BMPheader, 0, defaultHeader.length);

		byte RGB_24bit[] = new byte[3];

		littleEndian(BMPheader , 2, (0x36 + size));
		littleEndian(BMPheader , 18, width);
		littleEndian(BMPheader , 22, height);
		littleEndian(BMPheader , 34, size);

		out.write(BMPheader);

		for (int a = height-1; a >= 0; a--)
		{
			for (int b = 0; b < width; b++)
			{
				for (int c = 0; c < 3; c++)
					RGB_24bit[c] = (byte)(pixels[b + a * width]>>(c * 8) & 0xFF);

				out.write(RGB_24bit);
			}

			out.write(new byte[width & 3]); //padding bytes
		}

		out.flush();
		out.close();
	}

	public static String buildBMP_palettized(String outfile, String key, ArrayList color_table_array, int palette) throws IOException
	{
		return buildBMP_palettized(outfile, (Bitmap)bmps.get(key), color_table_array, palette);
	}

	public static String buildBMP_palettized(String outfile, Bitmap bitmap, ArrayList color_table_array, int palette) throws IOException
	{
		return buildBMP_palettized_ARGB(outfile, bitmap, color_table_array, palette, false);
	}

	public static String buildBMP_palettized_ARGB(String outfile, Bitmap bitmap, ArrayList color_table_array, int palette, boolean alpha) throws IOException
	{
		if (bitmap == null)
			return "";

		palette = 256; //still fixed!

		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		int size = palette * 4 + width * height + height * (width & 3);

		if (size == 0)
			return "";

		outfile += ".bmp";

		BufferedOutputStream out = new BufferedOutputStream( new FileOutputStream(outfile), 65535);

		byte BMPheader[] = new byte[defaultHeader.length];
		System.arraycopy(defaultHeader, 0, BMPheader, 0, defaultHeader.length);

		byte RGB_4bit = 0; // 1 pixel_index

		if (palette == 256)
			BMPheader[28] = 8;
		else
			BMPheader[28] = 4;


		littleEndian(BMPheader , 2, (0x36 + size));
		littleEndian(BMPheader , 10, (0x36 + palette * 4)); //pixel start
		littleEndian(BMPheader , 18, width);
		littleEndian(BMPheader , 22, height);
		littleEndian(BMPheader , 34, size);

		out.write(BMPheader);

		Object color_table[] = color_table_array.toArray();
		byte base_color_index[] = new byte[4];

		//paletize 256 * 4byte BGR0 indices
		for (int a = 0, color; a < palette; a++)
		{
			if (a < color_table.length)
			{
				color = Integer.parseInt(color_table[a].toString());

				if (!alpha)
					color &= 0xFFFFFF;

				for (int b = 0; b < 4; b++)
					base_color_index[b] = (byte)(0xFF & color>>(b<<3));
			}

			out.write(base_color_index);
		}

		color_table = null;

		int pixels[] = bitmap.getPixel();

		for (int a = height - 1; a >= 0; a--)
		{
			for (int b = 0, val = 0; b < width; b++)
			{
				out.write(0xFF & getColorIndex(pixels[b + a * width], color_table_array));
			}

			out.write(new byte[width & 3]); //padding bytes
		}

		out.flush();
		out.close();

		return outfile;
	}

	private static int getColorIndex(int color, ArrayList color_table)
	{
		String color_str = "" + color;
		int index = color_table.indexOf(color_str);

		if (index != -1)
			return index;

		return (color_table.size() - 1);
	}

	public static String write_ColorTable(String outfile, ArrayList color_table_array, int palette) throws IOException
	{
		Object color_table[] = color_table_array.toArray();
		byte base_color_index[] = new byte[4];

		outfile += ".spf";

		palette = 256; //still fixed!

		BufferedOutputStream out = new BufferedOutputStream( new FileOutputStream(outfile), 65535);

		//palettize number * 4byte BGR0 indices (e.g.256 or 16)
		for (int a=0, color; a < palette; a++)
		{
			if (a < color_table.length)
			{
				color = 0xFFFFFF & Integer.parseInt(color_table[a].toString());

				for (int b=0; b < 3; b++)
					base_color_index[b] = (byte)(0xFF & color>>(b<<3));
			}

			out.write(base_color_index);
		}

		color_table = null;

		out.flush();
		out.close();

		return outfile;
	}
}