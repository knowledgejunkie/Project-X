/*
 * @(#)UnicodeWriter.java 
 *
 * Copyright (c) 2005 by dvb.matt, All Rights Reserved.
 * 
 * This file is part of X, a free Java based demux utility.
 * X is intended for educational purposes only, as a non-commercial test project.
 * It may not be used otherwise. Most parts are only experimental.
 *  
 *
 * This program is free software; you can redistribute it free of charge
 * and/or modify it under the terms of the GNU General Public License as published by
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

	/**
	 *
	 */
	private UnicodeWriter()
	{}

	/**
	 *
	 */
	public UnicodeWriter(ByteArrayOutputStream _out, boolean _useUnicode)
	{
		useUnicode = _useUnicode;

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
		if (useUnicode)
		{
			/**
			 * mark file as big endian unicode
			 */
			if (out1.size() == 0)
				out1.writeChar(0xFEFF);

			out1.writeChars(str);
		}

		else
			out2.print(str);
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
