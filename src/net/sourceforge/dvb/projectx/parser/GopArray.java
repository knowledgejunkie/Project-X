/*
 * @(#)GopArray
 *
 * Copyright (c) 2005 by dvb.matt, All Rights Reserved.
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

/**
 * not used ATM
 */
public class GopArray extends Object {

	private byte[] gop_array;

	private int gop_offset;

	/**
	 *
	 */
	public GopArray()
	{
		gop_offset = 0;
	}

	/**
	 * maximum gop size
	 */
	private void init()
	{
		if (gop_array == null)
			gop_array = new byte[4096000];
	}

	/**
	 *
	 */
	public byte[] getGopArray()
	{
		return gop_array;
	}

	/**
	 *
	 */
	public boolean isBufferFull()
	{
		if (gop_offset >= gop_array.length)
			return true;

		return false;
	}

	/**
	 *
	 */
	public void write(byte[] array, int offset, int length)
	{
		write(array, offset, length, gop_offset);
	}

	/**
	 *
	 */
	public void write(byte[] array, int offset, int length, int _gop_offset)
	{
		init();

		if (_gop_offset >= gop_array.length)
			return;

		if (_gop_offset + length > gop_array.length)
			length = gop_array.length - _gop_offset;

		System.arraycopy(array, offset, gop_array, _gop_offset, length);

		gop_offset += length;
	}

	/**
	 *
	 */
	public byte[] read(int length)
	{
		return read(gop_offset, length);
	}

	/**
	 *
	 */
	public byte[] read(int offset, int length)
	{
		init();

		if (offset >= gop_array.length)
			return null;

		byte[] array = new byte[length];

		if (offset + length > gop_array.length)
			length = gop_array.length - offset;

		System.arraycopy(gop_array, offset, array, 0, length);

		return array;
	}

	/**
	 *
	 */
	public int getOffset()
	{
		return gop_offset;
	}
}
