/*
 * @(#)
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

package net.sourceforge.dvb.projectx.parser;

import java.io.IOException;
import java.io.File;
import java.io.RandomAccessFile;

import net.sourceforge.dvb.projectx.common.Resource;
import net.sourceforge.dvb.projectx.common.X;

/**
 * common stuff for parsing functions
 */
public final class CommonParsing {

	/**
	 * skip leading bytes before first valid startcodes and return fixed array
	 */
	public static byte[] searchHeader(byte[] _data, int _type, int _overhead)
	{
		int len = _data.length - _overhead;
		int start = 9 + (0xFF & _data[8]);
		int end = len - 3;
		int s = start;

		boolean found = false;

		byte[] newdata = new byte[0];

		packloop:
		for (; s < end; s++)
		{
			/**
			 * video
			 */
			if (_type == 3)
			{
				if (_data[s] != 0 || _data[s + 1] != 0  || _data[s + 2] != 1 || _data[s + 3] != (byte)0xB3) 
					continue packloop;

				found=true;
				break;
			}

			/**
			 * ac3, dts
			 */
			else if (_type == 0)
			{
				if ((_data[s] != 0xB || _data[s + 1] != 0x77) && (_data[s] != 0x7F || _data[s + 1] != (byte)0xFE || _data[s + 2] != (byte)0x80 || _data[s + 3] != 1)) 
					continue packloop;

				found=true; 
				break;
			}

			/**
			 * mpa
			 */
			else if (_type == 2)
			{
				if ( _data[s] != (byte)0xFF || (0xF0 & _data[s + 1]) != 0xF0)
					continue packloop;

				found = true; 

				break;
			}
		}

		if (!found) 
			return newdata;

		newdata = new byte[len - s + start + _overhead];

		System.arraycopy(_data, 0, newdata, 0, start);
		System.arraycopy(_data, s, newdata, start, len - s + _overhead);

		newdata[4] = (byte)((newdata.length - 6 - _overhead)>>>8);
		newdata[5] = (byte)(0xFF & (newdata.length - 6 - _overhead));

		return newdata;
	}

	/**
	 * create PTS alias 
	 */
	public static void logAlias(String _vptslog, String _datalog, long[] _options, boolean _PureVideo)
	{
		File vpts = new File(_vptslog); 

		try {
			RandomAccessFile log = new RandomAccessFile(_datalog, "rw");

			log.seek(0);

			if (vpts.exists() && vpts.length() > 0)
			{
				RandomAccessFile vlog = new RandomAccessFile(_vptslog, "r");
				long p = vlog.readLong();

				if (!_PureVideo && _options[19] == 0)
					_options[25] = p;

				log.writeLong(_options[25] + _options[28]);

				vlog.close();
			}
			else 
				log.writeLong((0L + _options[28]));

			log.writeLong(0L); 
			log.close();

			X.Msg("");
			X.Msg(Resource.getString("all.msg.pts.faked"));

		} catch (IOException e) {

			X.Msg(Resource.getString("logalias.error.io") + " " + e);
		}
	}

}