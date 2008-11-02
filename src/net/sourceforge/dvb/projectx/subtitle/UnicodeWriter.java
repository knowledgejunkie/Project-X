/*
 * @(#)UnicodeWriter.java 
 *
 * Copyright (c) 2005-2008 by dvb.matt, All Rights Reserved.
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

import java.io.DataOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.IOException;

/**
 *
 */
public class UnicodeWriter extends Object {

	private DataOutputStream out1;

	private PrintWriter out2;

	private boolean useUnicode = false;
	private boolean useUTF8 = false;

	private short mask_1 = ~0x7F;
	private short mask_2 = ~0x7FF;

	/**
	 *
	 */
	private UnicodeWriter()
	{}

	/**
	 *
	 */
	public UnicodeWriter(ByteArrayOutputStream _out, boolean _useUTF16, boolean _useUTF8)
	{
		useUnicode = (_useUTF16 || _useUTF8); //UTF16 standard

		useUTF8 = _useUTF8;

		if (useUnicode)
			out1 = new DataOutputStream(_out);

		else
			out2 = new PrintWriter(_out, true);
	}

	/**
	 *
	 */
	public void flush() throws IOException
	{
		if (useUnicode)
			out1.flush();

		else
			out2.flush();
	}

	/**
	 *
	 */
	public void close() throws IOException
	{
		if (useUnicode)
			out1.close();

		else
			out2.close();
	}

	/**
	 *
	 */
	public void print(String str) throws IOException
	{
		if (!useUnicode)
		{
			out2.print(str);
			return;
		}

		// UTF8
		if (useUTF8)
		{
			// mark file as UTF-8, from akin
			if (out1.size() == 0) 
				out1.write( new byte[] { (byte)0xEF, (byte)0xBB, (byte)0xBF}, 0, 3); 

			char[] chars = str.toCharArray();

			for (int i = 0, j = chars.length; i < j; i++)
			{
				if ((mask_1 & chars[i]) == 0) //0xxxxxxx - 0000-007F
					out1.writeByte(chars[i]);

				else if ((mask_2 & chars[i]) == 0) //110xxxxx 10xxxxxx - 0080-07FF
					out1.writeShort(0xC080 | (0x1F00 & chars[i]<<2) | (0x3F & chars[i]));

				else //1110xxxx 10xxxxxx 10xxxxxx - 0800-FFFF
				{
					out1.writeByte(0xE0 | (0xF0000 & chars[i]<<4));
					out1.writeShort(0x8080 | (0x3F00 & chars[i]<<2) | (0x3F & chars[i]));
				}
			}

			return;
		}

		// UTF16
		/**
		 * mark file as big endian unicode
		 */
		if (out1.size() == 0)
			out1.writeChar(0xFEFF);

		out1.writeChars(str);
	}

	/**
	 *
	 */
	public void println(String str) throws IOException
	{
		if (useUnicode)
		{
			print(str);
			print(System.getProperty("line.separator"));
		}

		else
			out2.println(str);
	}

	/**
	 *
	 */
	public void println() throws IOException
	{
		if (useUnicode)
			print(System.getProperty("line.separator"));

		else
			out2.println();
	}
}
