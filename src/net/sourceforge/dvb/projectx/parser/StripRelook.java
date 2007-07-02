/*
 * @(#)StripRelook
 *
 * Copyright (c) 2005-2007 by dvb.matt, All rights reserved.
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

import net.sourceforge.dvb.projectx.xinput.XInputFile;

import net.sourceforge.dvb.projectx.common.Common;
import net.sourceforge.dvb.projectx.common.Keys;

import net.sourceforge.dvb.projectx.parser.CommonParsing;

public class StripRelook extends Object {

	private boolean debug = false;
	private int type = 0;

	/**
	 *
	 */
	public StripRelook(int value)
	{
		debug = Common.getSettings().getBooleanProperty(Keys.KEY_DebugLog);
		type = value;
	}

	/**
	 *
	 */
	public XInputFile[] process(XInputFile xInputFile, String output)
	{
		if (output.startsWith("[res]"))
			output = xInputFile.getParent();

		String parent = output + System.getProperty("file.separator") + xInputFile.getName();

		String strippedfile_video = parent + "[stripped].vpes";
		String strippedfile_audio = parent + "[stripped].apes";
		String strippedfile_teletext = parent + "[stripped].tpes";

		try {

			PushbackInputStream in = new PushbackInputStream(xInputFile.getInputStream());
			BufferedOutputStream out_1 = new BufferedOutputStream( new FileOutputStream(strippedfile_video), 5120000);
			BufferedOutputStream out_2 = new BufferedOutputStream( new FileOutputStream(strippedfile_audio), 4096000);
			BufferedOutputStream out_3 = new BufferedOutputStream( new FileOutputStream(strippedfile_teletext), 4096000);

			int count = 0;
			int[] buffersize = { 0xC000, 0xE800 };

			long pos = 0;
			long len = xInputFile.length();

			byte[] array = new byte[buffersize[type]];

			int ret, seqhead, audiolength, videolength, seqoffs, frameoffs, audiooffs, teletextlength;

			while (pos < len)
			{
				ret = in.read(array);

				if (ret < array.length)
					in.read(array, ret, array.length - ret);

				seqhead = getValue(array, 0);
				audiolength = getValue(array, 4);
				videolength = getValue(array, 8);
				seqoffs = getValue(array, 12);
				frameoffs = getValue(array, 16);
				audiooffs = getValue(array, 20); //?

				teletextlength = type == 0 ? 0 : getValue(array, 32);

				if (debug)
					System.out.println("rl pos + " + pos + " /v " + videolength + " /a " + audiolength + " /t " + teletextlength);

				if (videolength > 0)
					out_1.write(array, 0x200, videolength);

				if (audiolength > 0)
					out_2.write(array, 0x9200, audiolength);

				if (teletextlength > 0)
					out_3.write(array, 0xC000, teletextlength);

				count++;
				pos += array.length;
			}

			out_1.flush();
			out_1.close();
			out_2.flush();
			out_2.close();
			out_3.flush();
			out_3.close();

			in.close();

		} catch (Exception e) {

			Common.setExceptionMessage(e);
			return null;
		}

		XInputFile[] xif = new XInputFile[3];

		xif = finishFile(strippedfile_video, xif, 0);
		xif = finishFile(strippedfile_audio, xif, 1);
		xif = finishFile(strippedfile_teletext, xif, 2);

		return xif;
	}

	/**
	 *
	 */
	private int getValue(byte[] array, int offset)
	{
		int value = 0;

		for (int i = 0; i < 4; i++)
			value |= (0xFF & array[offset + i])<<((3 - i) * 8);

		return value;
	}

	/**
	 *
	 */
	private XInputFile[] finishFile(String strippedfile, XInputFile[] xif, int index)
	{
		File file = new File(strippedfile);

		if (debug)
			System.out.println("rl file + " + index + " /l " + file.length());

		if (file.length() > 100)
			xif[index] = new XInputFile(file);

		else
			file.delete();

		return xif;
	}
}
