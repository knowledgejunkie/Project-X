/*
 * @(#)HpFix
 *
 * Copyright (c) 2004-2005 by dvb.matt, All rights reserved.
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

public class HpFix extends Object {

	public HpFix()
	{}

	public XInputFile process(XInputFile xInputFile)
	{
		String file_fixed = xInputFile.toString() + "[fixed].pes";

		try {

			PushbackInputStream in = new PushbackInputStream(xInputFile.getInputStream(), 9);
			BufferedOutputStream out = new BufferedOutputStream( new FileOutputStream(file_fixed), 5096000);

			byte[] header = new byte[9];
			byte[] data;

			for (int ret, len, pes_ext;;)
			{
				ret = in.read(header, 0, 9);

				if (ret < 9)
					break;

				if ((ret = CommonParsing.validateStartcode(header, 0)) < 0 || header[3] != (byte)0xBD)
				{
					ret = ret < 0 ? -ret : 4;

					in.unread(header, ret, 9 - ret);
					continue;
				}

				len = (0xFF & header[4])<<8 | (0xFF & header[5]);
				len -= 6;
				header[4] = (byte)(0xFF & len>>>8);
				header[5] = (byte)(0xFF & len);

				pes_ext = (0xFF & header[8]);
				pes_ext += 4;
				header[8] = (byte)(0xFF & pes_ext);

				out.write(header);

				data = new byte[len - 3];
				in.read(data, 0 , data.length);

				out.write(data);
			}

			in.close();
			out.flush();
			out.close();

		} catch (IOException e) {

			Common.setExceptionMessage(e);
			return null;
		}

		return (new XInputFile(new File(file_fixed)));
	}
}
