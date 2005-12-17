/*
 * @(#)StripAudio
 *
 * Copyright (c) 2005 by dvb.matt, All rights reserved.
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

import net.sourceforge.dvb.projectx.parser.CommonParsing;

public class StripAudio extends Object {

	private byte[] DD_header = { 0x72, (byte)0xF8, 0x1F, 0x4E, 1, 0, 0, 0 };

	private byte[] DTS44_header = { (byte)0xFF, 0x1F, 0, (byte)0xE8 };

	private byte[] DTS48_header = { (byte)0xFE, 0x7F, 01, (byte)0x80 };

	private boolean isAC3 = false;

	private boolean isDTS = false;

	private boolean isDTS48 = false;

	private long val;

	/**
	 *
	 */
	public StripAudio()
	{}

	/**
	 *
	 */
	public XInputFile process(XInputFile xInputFile)
	{
		String temp_file = xInputFile.toString() + "[stripped].$es$";

		XInputFile newXInputFile;

		try {

			PushbackInputStream in = new PushbackInputStream(xInputFile.getInputStream(), 8);
			BufferedOutputStream out = new BufferedOutputStream( new FileOutputStream(temp_file), 5096000);

			int sector = 0;
			byte[] data = new byte[0x1800];

			in.skip(0x2C); //std pcm wave header, already known through streaminfo

			for (int ret, len;;)
			{
				ret = in.read(data, 0, 8);

				if (ret < 8)
					break;

				if (!verifyHeader(data))
				{
					in.unread(data, 1, 7);
					continue;
				}

				if (isAC3)
				{
					len = (0xFF & data[7])<<8 | (0xFF & data[6]);
					len >>>= 3;

					ret = in.read(data, 8, len);

					sector++;

					if (ret < len)
					{
						Common.setMessage("!> not enough data in sector " + sector + ", dropped..", true);
						continue;
					}

					Common.changeByteOrder(data, 8, len + 8);

					out.write(data, 8, len);

					in.skip(0x1800 - len - 8);
				}

				else if (isDTS)
				{
					len = 0x1000;

					ret = in.read(data, 8, len - 8);

					sector++;

					if (ret < len - 8)
					{
						Common.setMessage("!> not enough data in sector " + sector + ", dropped..", true);
						continue;
					}

					int j = 0;

					// dts44 with padding
					if (!isDTS48)
					{
						for (int i = 0, k; i < len; i += 8, j += 7)
						{
							val = (0xFFL & data[i])<<42;
							val |= (0x3FL & data[i + 1])<<50;
							val |= (0xFFL & data[i + 2])<<28;
							val |= (0x3FL & data[i + 3])<<36;
							val |= (0xFFL & data[i + 4])<<14;
							val |= (0x3FL & data[i + 5])<<22;
							val |= (0xFFL & data[i + 6]);
							val |= (0x3FL & data[i + 7])<<8;

							CommonParsing.setValue(data, j, 7, !CommonParsing.BYTEREORDERING, val);
						}
					}

					// dts48
					else
					{
						j = len;
						Common.changeByteOrder(data, 0, len);
					}

					out.write(data, 0, j);
				}
			}

			in.close();
			out.flush();
			out.close();

		} catch (IOException e) {

			Common.setExceptionMessage(e);
			return null;
		}

		if (isAC3)
			return finish(temp_file, xInputFile.toString() + "[stripped].ac3");

		else if (isDTS)
			return finish(temp_file, xInputFile.toString() + "[stripped].dts");

		return null;
	}

	/**
	 *
	 */
	private XInputFile finish(String temp_file, String new_file)
	{
		File file = new File(new_file);

		if (file.exists())
			file.delete();

		Common.renameTo(new File(temp_file), file);

		return (new XInputFile(file));
	}

	/**
	 *
	 */
	private boolean verifyHeader(byte[] header)
	{
		if (!isAC3 && !isDTS)
		{
			isAC3 = getAC3(header);

			if (!isAC3)
				isDTS = getDTS(header);
		}

		if (isAC3)
			return getAC3(header);

		if (isDTS)
			return getDTS(header);

		return false;
	}

	/**
	 *
	 */
	private boolean getAC3(byte[] header)
	{
		for (int i = 0; i < 6; i++)
			if (DD_header[i] != header[i])
				return false;

		return true;
	}

	/**
	 *
	 */
	private boolean getDTS(byte[] header)
	{
		if (!isDTS)
			isDTS48 = getDTS48(header);

		if (isDTS48)
			return getDTS48(header);

		for (int i = 0; i < 4; i++)
			if (DTS44_header[i] != header[i])
				return false;

		return true;
	}

	/**
	 *
	 */
	private boolean getDTS48(byte[] header)
	{
		for (int i = 0; i < 4; i++)
			if (DTS48_header[i] != header[i])
				return false;

		return true;
	}
}
