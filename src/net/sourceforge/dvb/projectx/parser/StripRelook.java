/*
 * @(#)StripRelook
 *
 * Copyright (c) 2005 by dvb.matt, All rights reserved.
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

package net.sourceforge.dvb.projectx.parser;

import java.io.PushbackInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.File;

import net.sourceforge.dvb.projectx.xinput.XInputFile;

import net.sourceforge.dvb.projectx.common.Common;
import net.sourceforge.dvb.projectx.parser.CommonParsing;

public class StripRelook extends Object {

	public StripRelook()
	{}

	public XInputFile[] process(XInputFile xInputFile, String output)
	{
		String parent = output + System.getProperty("file.separator") + xInputFile.getName();

		String strippedfile_video = parent + "[stripped].vpes";
		String strippedfile_audio = parent + "[stripped].apes";

		try {

			PushbackInputStream in = new PushbackInputStream(xInputFile.getInputStream());
			BufferedOutputStream out_1 = new BufferedOutputStream( new FileOutputStream(strippedfile_video), 5096000);
			BufferedOutputStream out_2 = new BufferedOutputStream( new FileOutputStream(strippedfile_audio), 5096000);

			byte[] array = new byte[0xC000];
			int count = 0;
			long pos = 0;
			long len = xInputFile.length();

			int ret, seqhead, audiolength, videolength, seqoffs, frameoffs, audiooffs;

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

				if (videolength > 0)
					out_1.write(array, 0x200, videolength);

				if (audiolength > 0)
					out_2.write(array, 0x9200, audiolength);

				count++;
				pos += array.length;
			}

			out_1.flush();
			out_1.close();
			out_2.flush();
			out_2.close();

			in.close();

		} catch (IOException e) {

			Common.setExceptionMessage(e);
			return null;
		}

		File v_file = new File(strippedfile_video);
		File a_file = new File(strippedfile_audio);

		XInputFile[] xif = new XInputFile[2];

		if (v_file.length() > 100)
			xif[0] = new XInputFile(v_file);

		else
			v_file.delete();

		if (a_file.length() > 100)
			xif[1] = new XInputFile(a_file);

		else
			a_file.delete();

		return xif;
	}

	private int getValue(byte[] array, int offset)
	{
		int value = 0;

		for (int i = 0; i < 4; i++)
			value |= (0xFF & array[offset + i])<<((3 - i) * 8);

		return value;
	}
}
