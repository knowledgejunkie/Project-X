/*
 * @(#)Sup2VobSub.java 
 *
 * Copyright (c) 2006 by dvb.matt, All Rights Reserved.
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

import java.io.PrintWriter;
import java.io.IOException;
import java.io.PushbackInputStream;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.File;

import java.util.Arrays;

import net.sourceforge.dvb.projectx.xinput.XInputFile;

import net.sourceforge.dvb.projectx.common.Common;
import net.sourceforge.dvb.projectx.parser.CommonParsing;

/**
 *
 */
public class Sup2VobSub extends Object {

	private PushbackInputStream inputstream;
	private BufferedOutputStream outputstream;
	private PrintWriter printstream;

	private XInputFile xInputFile;
	private String outputfile_sub;
	private String outputfile_idx;

	private long file_length;
	private long read_position;
	private long write_position;
	private long pts_value;

	private int buffersize = 12;
	private int packetsize = 0xFFFF;
	private int packsize = 2048;

	private int packetlength;
	private int startoffset;

	private byte[] sup_packet;
	private byte[] export_pack;

	private byte[] pack_header = {
		0, 0, 1, (byte)0xBA, 0x44, 2, (byte)0xC4, (byte)0x82,
		4, (byte)0xA9, 1, (byte)0x89, (byte)0xC3, (byte)0xF8
	};
	private byte[] pes_header = { 0, 0, 1, (byte)0xBD, 0, 0 };
	private byte[] padding_header = { 0, 0, 1, (byte)0xBE, 0, 0 };
	private byte[] pes_extension1 = { (byte)0x81, (byte)0x80, 5, 0, 0, 0, 0, 0 };
	private byte[] pes_extension2 = { (byte)0x80, 0, 0 };

	private byte stream_id = 0x20;

	/**
	 *
	 */
	private Sup2VobSub()
	{}

	/**
	 *
	 */
	public Sup2VobSub(String file)
	{
		if (init(file))
			parseStream();
	}

	/**
	 *
	 */
	public Sup2VobSub(String file, Object[] colour_table)
	{
		if (init(file, colour_table))
			parseStream();
	}

	/**
	 *
	 */
	private boolean init(String file)
	{
		return init(file, null);
	}

	/**
	 *
	 */
	private boolean init(String file, Object[] colour_table)
	{
		boolean b = false;

		xInputFile = new XInputFile(new File(file));

		file_length = xInputFile.length();
		read_position = 0;
		write_position = 0;

		if (!initStreams())
			return b;

		initIndex(colour_table);

		sup_packet = new byte[packetsize];
		export_pack = new byte[packsize];

		System.arraycopy(pack_header, 0, export_pack, 0, pack_header.length);
		System.arraycopy(pes_header, 0, export_pack, pack_header.length, pes_header.length);

		return !b;
	}

	/**
	 *
	 */
	private void parseStream()
	{
		while (read_position < file_length)
		{
			while (!nextStartCode())
			{}

			if (!readPicture())
				continue;

			writePicture();
		}

		closeStreams();
	}

	/**
	 *
	 */
	private boolean nextStartCode()
	{
		boolean b = false;

		startoffset = 0;

		readData(startoffset, buffersize);

		if (sup_packet[startoffset] != 0x53 || sup_packet[startoffset + 1] != 0x50)
		{
			unreadData(1, buffersize - 1);

			return b;
		}

		startoffset += 2;

		return !b;
	}

	/**
	 *
	 */
	private boolean readPicture()
	{
		boolean b = false;
		int read = 0;
		int pts_field = 8;
		int length_field = 2;

		pts_value = CommonParsing.readPTS(sup_packet, startoffset, pts_field, CommonParsing.BYTEREORDERING, false);

		startoffset += pts_field;

		packetlength = CommonParsing.getIntValue(sup_packet, startoffset, length_field, !CommonParsing.BYTEREORDERING);

		read = readData(buffersize, packetlength - length_field);

		if (read < packetlength - length_field)
		{
			Common.setMessage("!> packet too short");
			return b;
		}

		return !b;
	}

	/**
	 *
	 */
	private void writePicture()
	{
		int offset = 0;

		writePictureIndex();

		offset = writeFirstPack(offset);

		while (offset > 0)
			offset = writeSubPack(offset);
	}

	/**
	 * many nice confusing additions :)
	 */
	private int writeFirstPack(int offset)
	{
		if (offset >= packetlength)
			return -1;

		int max_size = packsize - pack_header.length - pes_header.length - pes_extension1.length - 1;
		int length = packetlength - offset >= max_size ? max_size : packetlength - offset;
		int padding = max_size - length;
		int headerlength = pes_extension1.length + 1;

		System.arraycopy(pes_extension1, 0, export_pack, pack_header.length + pes_header.length, pes_extension1.length);

		if (padding <= padding_header.length)
		{
			headerlength += padding;
			Arrays.fill(export_pack, pack_header.length + pes_header.length + pes_extension1.length, pack_header.length + pes_header.length + pes_extension1.length + padding, (byte) 0xFF);
			CommonParsing.setPES_LengthField(export_pack, pack_header.length, headerlength + length);
			CommonParsing.setValue(export_pack, pack_header.length + pes_header.length + 2, 1, !CommonParsing.BYTEREORDERING, 5 + padding);
			CommonParsing.setPES_PTSField(export_pack, pack_header.length, pts_value);
			CommonParsing.setPES_SubIdField(export_pack, pack_header.length, pes_header.length, pes_extension1.length + padding, stream_id);
		}

		else
		{
			System.arraycopy(padding_header, 0, export_pack, pack_header.length + pes_header.length + headerlength + length, padding_header.length);
			CommonParsing.setPES_LengthField(export_pack, pack_header.length + pes_header.length + headerlength + length, padding - padding_header.length);
			Arrays.fill(export_pack, pack_header.length + pes_header.length + headerlength + length + padding_header.length, packsize, (byte) 0xFF);
			CommonParsing.setPES_LengthField(export_pack, pack_header.length, headerlength + length);
			CommonParsing.setPES_PTSField(export_pack, pack_header.length, pts_value);
			CommonParsing.setPES_SubIdField(export_pack, pack_header.length, pes_header.length, pes_extension1.length, stream_id);
		}

		System.arraycopy(sup_packet, startoffset + offset, export_pack, pack_header.length + pes_header.length + headerlength, length);

		writeData();

		offset += length;

		return offset;
	}

	/**
	 *
	 */
	private int writeSubPack(int offset)
	{
		if (offset >= packetlength)
			return -1;

		int max_size = packsize - pack_header.length - pes_header.length - pes_extension2.length - 1;
		int length = packetlength - offset >= max_size ? max_size : packetlength - offset;
		int padding = max_size - length;
		int headerlength = pes_extension2.length + 1;

		System.arraycopy(pes_extension2, 0, export_pack, pack_header.length + pes_header.length, pes_extension2.length);

		if (padding <= padding_header.length)
		{
			headerlength += padding;
			Arrays.fill(export_pack, pack_header.length + pes_header.length + pes_extension2.length, pack_header.length + pes_header.length + pes_extension2.length + padding, (byte) 0xFF);
			CommonParsing.setPES_LengthField(export_pack, pack_header.length, headerlength + length);
			CommonParsing.setValue(export_pack, pack_header.length + pes_header.length + 2, 1, !CommonParsing.BYTEREORDERING, padding);
			CommonParsing.setPES_SubIdField(export_pack, pack_header.length, pes_header.length, pes_extension2.length + padding, stream_id);
		}

		else
		{
			System.arraycopy(padding_header, 0, export_pack, pack_header.length + pes_header.length + headerlength + length, padding_header.length);
			CommonParsing.setPES_LengthField(export_pack, pack_header.length + pes_header.length + headerlength + length, padding - padding_header.length);
			Arrays.fill(export_pack, pack_header.length + pes_header.length + headerlength + length + padding_header.length, packsize, (byte) 0xFF);
			CommonParsing.setPES_LengthField(export_pack, pack_header.length, length + headerlength);
			CommonParsing.setPES_SubIdField(export_pack, pack_header.length, pes_header.length, pes_extension2.length, stream_id);
		}

		System.arraycopy(sup_packet, startoffset + offset, export_pack, pack_header.length + pes_header.length + headerlength, length);

		writeData();

		offset += length;

		return offset;
	}

	/**
	 *
	 */
	private boolean initStreams()
	{
		boolean b = false;

		try {
			outputfile_sub = xInputFile + ".sub";
			outputfile_idx = xInputFile + ".idx";

			inputstream = new PushbackInputStream(xInputFile.getInputStream(), buffersize);
			outputstream = new BufferedOutputStream(new FileOutputStream(outputfile_sub), 1024000);
			printstream = new PrintWriter(new FileOutputStream(outputfile_idx));

			Common.setMessage("");
			Common.setMessage("-> create VobSub Files (idx + sub) : " + outputfile_sub);

			return !b;

		} catch (IOException e) {

			Common.setExceptionMessage(e);
		}

		return b;
	}

	/**
	 *
	 */
	private void closeStreams()
	{
		try {
			inputstream.close();

			outputstream.flush();
			outputstream.close();
			printstream.flush();
			printstream.close();

		} catch (IOException e) {

			Common.setExceptionMessage(e);
		}
	}

	/**
	 *
	 */
	private int readData(int offset, int length)
	{
		int read = 0;

		try {
			read = inputstream.read(sup_packet, offset, length);

			read_position += read;

		} catch (IOException e) {

			Common.setExceptionMessage(e);
		}

		return read;
	}

	/**
	 *
	 */
	private int unreadData(int offset, int length)
	{
		try {
			inputstream.unread(sup_packet, offset, length);
			read_position -= length;

			return length;

		} catch (IOException e) {

			Common.setExceptionMessage(e);
		}

		return 0;
	}

	/**
	 *
	 */
	private void writeData()
	{
		try {
			outputstream.write(export_pack);
			write_position += packsize;

		} catch (IOException e) {

			Common.setExceptionMessage(e);
		}
	}

	/**
	 *
	 */
	private void writePictureIndex()
	{
		printstream.print("timestamp: " + Common.formatTime_2a(pts_value / 90) + ", ");
		printstream.println("filepos: " + Common.adaptString(Long.toHexString(write_position), 9));
	}

	/**
	 *
	 */
	private void initIndex(Object[] colour_table)
	{
		printstream.println("# VobSub index file, v7 (do not modify this line!)");
		printstream.println("size: 720x576");
		printstream.println("org: 0, 0");
		printstream.println("scale: 100%, 100%");
		printstream.println("alpha: 100%");
		printstream.println("smooth: OFF");
		printstream.println("fadein/out: 0, 0");
		printstream.println("align: OFF at LEFT TOP");
		printstream.println("time offset: 0");
		printstream.println("forced subs: OFF");

		printColourTable(colour_table);

		printstream.println("custom colors: OFF, tridx: 1000, colors: 600000, 101010, ffffff, a9a9a9");
		printstream.println("langidx: 0");
		printstream.println("id: --, index: 0");
		printstream.println("");
	}

	/**
	 *
	 */
	private void printColourTable(Object[] colour_table)
	{
		int max_indices = 16;

		printstream.print("palette: ");

		String[] std_colour_table = { 
			"600000", "101010" , "ffffff", "a9a9a9", "4d4d4d", "d7d7d7", "d7d7d7", "d7d7d7", 
			"d7d7d7", "d7d7d7", "d7d7d7", "d7d7d7", "d7d7d7", "d7d7d7", "d7d7d7", "d7d7d7" 
		};

		if (colour_table == null)
			colour_table = new Object[0];

		else
		{
			for (int i = 0, j = colour_table.length; i < j && i < max_indices; i++)
			{
				printstream.print(String.valueOf(Common.adaptString(Integer.toHexString(0xFFFFFF & Integer.parseInt(colour_table[i].toString())), 6)));

				if (i < max_indices - 1)
					printstream.print(", ");
			}

			for (int i = colour_table.length, j = std_colour_table.length; i < j && i < max_indices; i++)
			{
				printstream.print(String.valueOf(Common.adaptString(std_colour_table[i], 6)));

				if (i < max_indices - 1)
					printstream.print(", ");
			}
		}

		printstream.println();
	}
}
