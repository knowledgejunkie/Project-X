/*
 * @(#)StripMedion
 *
 * Copyright (c) 2007 by dvb.matt, All rights reserved.
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

package net.sourceforge.dvb.projectx.parser;

import java.io.PushbackInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.File;

import java.util.Hashtable;
import java.util.ArrayList;
import java.util.Enumeration;

import net.sourceforge.dvb.projectx.xinput.XInputFile;

import net.sourceforge.dvb.projectx.common.Common;
import net.sourceforge.dvb.projectx.common.Keys;

import net.sourceforge.dvb.projectx.parser.CommonParsing;

public class StripMedion extends Object {

	private boolean debug = false;
	private int aux_number = 1;
	private String stripped = "[stripped]";
	private String parent = "";

	private Hashtable out_streams;
	private BufferedOutputStream out;
	private MedionStreamObject mso;

	/**
	 *
	 */
	public StripMedion()
	{
		debug = Common.getSettings().getBooleanProperty(Keys.KEY_DebugLog);
		out_streams = new Hashtable();
	}

	/**
	 *
	 */
	public XInputFile[] process(XInputFile xInputFile, String output)
	{
		if (output.startsWith("[res]"))
			output = xInputFile.getParent();

		parent = output + System.getProperty("file.separator") + xInputFile.getName();

		try {

			PushbackInputStream in = new PushbackInputStream(xInputFile.getInputStream());

			int count = 0;
			int buffersize = 0x10000;
			int chunksize = 0x2000;
			int chunknum = 7; //buffersize / chunksize;


			long pos = 0;
			long len = xInputFile.length();

			byte[] array = new byte[buffersize];

			int ret;

			while (pos < len)
			{
				ret = in.read(array);

				if (ret < array.length)
					in.read(array, ret, array.length - ret);

				for (int i = 0, j = chunksize; i < chunknum; i++, j += chunksize)
				{
					if (debug)
						System.out.println("med pos + " + pos + " /i " + i + " /v " + array[i]);

					mso = getOutput(0xFF & array[i]);
					mso.write(array, j, chunksize);
				}

				count++;
				pos += buffersize;
			}

			closeOutput();

			in.close();

		} catch (Exception e) {

			Common.setExceptionMessage(e);
			return null;
		}

		int num = 0;

		for (Enumeration n = out_streams.keys(); n.hasMoreElements() ; )
		{
			mso = getOutput(n.nextElement().toString());

			if (!mso.isEmpty())
				num++;
		}

		XInputFile[] xif = new XInputFile[num];
		String str;

		for (int i = 0, j = 0; i < 256; i++)
		{
			str = String.valueOf(i);

			if (out_streams.containsKey(str))
			{
				mso = (MedionStreamObject) out_streams.get(str);
				xif[j++] = new XInputFile(mso.getFile());
			}
		}

		return xif;
	}

	/**
	 *
	 */
	private void closeOutput()
	{
		for (Enumeration n = out_streams.keys(); n.hasMoreElements() ; )
		{
			mso = getOutput(n.nextElement().toString());
			mso.closeStream();
		}
	}

	/**
	 *
	 */
	private MedionStreamObject getOutput(int index)
	{
		return getOutput(String.valueOf(index));
	}

	/**
	 *
	 */
	private MedionStreamObject getOutput(String str)
	{
		if (!out_streams.containsKey(str))
			addNewOutput(str);

		return ((MedionStreamObject) out_streams.get(str));
	}

	/**
	 *
	 */
	private void addNewOutput(int index)
	{
		addNewOutput(String.valueOf(index));
	}

	/**
	 *
	 */
	private void addNewOutput(String str)
	{
		if (out_streams.containsKey(str))
			return;

		String newname = parent;

		if (str.equals("0"))
			newname = parent + stripped + ".vpes";

		else if (str.equals("1"))
			newname = parent + stripped + ".apes";

		else
			newname = parent + stripped + ".aux" + String.valueOf(aux_number++);

		out_streams.put(str, new MedionStreamObject(newname));
	}


	/**
	 *
	 */
	private class MedionStreamObject extends Object {

		private boolean isEmpty = true;
		private String name = "";
		private BufferedOutputStream stream;
		private File file;

		public MedionStreamObject(String str)
		{
			name = str;
			setStream();
		}

		private void setStream()
		{
			try {
				stream = new BufferedOutputStream( new FileOutputStream(name), 2048000);

			} catch (Exception e) {
				Common.setExceptionMessage(e);
			}
		}

		public String getName()
		{
			return name;
		}

		public void write(byte[] array, int index, int length)
		{
			try {
				stream.write(array, index, length);

			} catch (Exception e) {
				Common.setExceptionMessage(e);
			}
		}

		public void closeStream()
		{
			try {
				stream.flush();
				stream.close();

				file = new File(name);

				if (debug)
					System.out.println("rl file '" + name + "' /l " + file.length());

				if (isEmpty = file.length() < 100)
					file.delete();

			} catch (Exception e) {
				Common.setExceptionMessage(e);
			}
		}

		public File getFile()
		{
			return file;
		}

		public boolean isEmpty()
		{
			return isEmpty;
		}
	}
}
