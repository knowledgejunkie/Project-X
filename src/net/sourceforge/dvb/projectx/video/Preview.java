/*
 * @(#)Preview.java - prepare files for previewing
 *
 * Copyright (c) 2004-2009 by dvb.matt, All Rights Reserved.
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


package net.sourceforge.dvb.projectx.video;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.sourceforge.dvb.projectx.xinput.XInputFile;
import net.sourceforge.dvb.projectx.common.Common;
import net.sourceforge.dvb.projectx.parser.CommonParsing;

public class Preview extends Object {

	private byte preview_data[];

	private int loadSizeForward;
	private int active_collection;
	private int processed_PID;
	private int position[];

	private String processed_file;

	private boolean silent = true;

	private List positionList;
	private PreviewObject preview_object;
	private Object[] predefined_Pids;

	public Preview(int _loadSizeForward)
	{
		loadSizeForward = _loadSizeForward;

		position = new int[2];
		positionList = new ArrayList();
		processed_PID = -1;
		processed_file = "";
	}

	/**
	 *
	 */
	public String getProcessedFile()
	{
		return processed_file;
	}

	/**
	 *
	 */
	public String getProcessedPID()
	{
		if (processed_PID < 0)
			return "no (P)ID";

		return ("(P)ID 0x" + Common.adaptString(Integer.toHexString(processed_PID).toUpperCase(), 4));
	}

	/**
	 *
	 */
	public long silentload(long startposition, int size, List previewList, boolean direction, boolean all_gops, boolean fast_decode, int y_gain, Object[] _predefined_Pids, int _active_collection)
	{
		return load(startposition, size, previewList, direction, all_gops, fast_decode, y_gain, _predefined_Pids, _active_collection, true);
	}

	/**
	 *
	 */
	public long load(long startposition, int size, List previewList, boolean direction, boolean all_gops, boolean fast_decode, int y_gain, Object[] _predefined_Pids, int _active_collection)
	{
		return load(startposition, size, previewList, direction, all_gops, fast_decode, y_gain, _predefined_Pids, _active_collection, false);
	}

	/**
	 *
	 */
	public long load(long startposition, int size, List previewList, boolean direction, boolean all_gops, boolean fast_decode, int y_gain, Object[] _predefined_Pids, int _active_collection, boolean silent)
	{
		predefined_Pids = _predefined_Pids;
		active_collection = _active_collection;

		preview_data = new byte[size];

		int filetype = 0;
		int subfiletype = 0;
		int read_offset = 0;

		try {

			for (int i = 0; i < previewList.size(); i++)
			{
				preview_object = (PreviewObject) previewList.get(i);
				filetype = preview_object.getType();

				if (startposition < preview_object.getEnd())
				{
					XInputFile lXInputFile = (XInputFile) preview_object.getFile();
					lXInputFile.randomAccessOpen("r");
					lXInputFile.randomAccessSeek(startposition - preview_object.getStart());
					lXInputFile.randomAccessRead(preview_data, read_offset, size);
					lXInputFile.randomAccessClose();

					if (preview_object.getEnd() - startposition < size && i < previewList.size() - 1)
					{
						i++;

						int diff = (int)(preview_object.getEnd() - startposition);
						byte data2[] = new byte[size];

						preview_object = (PreviewObject) previewList.get(i);

						lXInputFile = (XInputFile) preview_object.getFile();
						lXInputFile.randomAccessSingleRead(data2, 0);

						System.arraycopy(data2, 0, preview_data, diff, size - diff);
						data2 = null;
					}

					subfiletype = lXInputFile.getStreamInfo().getStreamSubType();

					break;
				}
			}	

			preview_data = search(preview_data, startposition, filetype, subfiletype);

			long newposition = Common.getMpvDecoderClass().decodeArray(preview_data, direction, all_gops, fast_decode, y_gain, silent);

			for (int i = positionList.size() - 1; i >= 0; i--)
			{
				position = (int[]) positionList.get(i);
				if (position[1] <= newposition)
				{
					startposition += position[0];
					i = 0;
				}
			}

			if (positionList.size() == 0) 
				startposition += newposition;

			for (int i = 0; !silent && i < previewList.size(); i++)
			{
				preview_object = (PreviewObject)previewList.get(i);

				if (startposition < preview_object.getEnd())
				{
					processed_file = "" + i + "/" + (previewList.size() - 1) + " - " + preview_object.getFile().getName();
					break;
				}
			}

			preview_data = null;

			if (!silent)
			{
				Common.getMpvDecoderClass().setProcessedPosition(startposition, previewList);
				Common.getMpvDecoderClass().setPidAndFileInfo(getProcessedPID() + "   Part: " + getProcessedFile());
				Common.getGuiInterface().repaintPicturePanel();
			}

		} catch (Exception e) {
			Common.setExceptionMessage(e);
		}

		return startposition;
	}

	/**
	 *
	 */
	public long previewFile(XInputFile lXInputFile, long startposition, int size, boolean all_gops, boolean fast_decode, int y_gain)
	{
		preview_data = new byte[size];
		predefined_Pids = new Object[0];

		int filetype = lXInputFile.getStreamInfo().getStreamType();
		int subfiletype = lXInputFile.getStreamInfo().getStreamSubType();

		try {

			lXInputFile.randomAccessOpen("r");
			lXInputFile.randomAccessSeek(startposition);
			lXInputFile.randomAccessRead(preview_data, 0, size);
			lXInputFile.randomAccessClose();

		} catch (IOException e) {
			Common.setExceptionMessage(e);
		}

		preview_data = search(preview_data, startposition, filetype, subfiletype);

		long newposition = Common.getMpvDecoderClass().decodeArray(preview_data, false, all_gops, fast_decode, y_gain, true);

		startposition += newposition;

		preview_data = null;
		//System.gc();

		return startposition;
	}

	/**
	 *
	 */
	private byte[] search(byte data[], long startposition, int filetype, int subfiletype)
	{
		positionList.clear();

		int[] include = new int[predefined_Pids.length];

		for (int i = 0; i < include.length; i++) 
			include[i] = Integer.parseInt(predefined_Pids[i].toString().substring(2), 16);

		Arrays.sort(include);

		//do the parse
		switch (filetype)
		{
		case CommonParsing.ES_MPV_TYPE:
			return data;

		case CommonParsing.MPEG2PS_TYPE:
		case CommonParsing.PES_AV_TYPE:
			return parseMPG2(data, include);

		case CommonParsing.MPEG1PS_TYPE:
			return parseMPG1(data, include);

		case CommonParsing.PVA_TYPE:
			return parsePVA(data, include);

		case CommonParsing.TS_TYPE:
			if (subfiletype == CommonParsing.TS_TYPE_192BYTE>>>8)
				return parseTS192(data, include);
			else
				return parseTS(data, include);
		}

		return data;
	}

	/**
	 * TS 188
	 */
	private byte[] parseTS(byte[] data, int[] include)
	{
		ByteArrayOutputStream array = new ByteArrayOutputStream();

		byte[] hav_chunk = { 0x5B, 0x48, 0x4F, 0x4A, 0x49, 0x4E, 0x20, 0x41 }; //'[HOJIN A'

		boolean save = false;

		int mark = 0;
		int offset = 0;
		int ID = -1;

		int a = 0;

		for (int PID, dl = data.length - 9; a < dl; a++)
		{
			//humax chunk
			if (data[a] == 0x7F && data[a + 1] == 0x41 && data[a + 2] == 4 && data[a + 3] == (byte)0xFD)
			{
				if (save && mark <= a)
					array.write(data, mark, a - mark);

				save = false;
				a += 1183;
				mark = a + 1;

				continue;
			}

			//koscom chunk
			if (a < data.length - 5 && data[a + 2] == 0 && data[a + 3] == 0 && data[a + 4] == 0x47)
			{
				if (save && mark <= a)
					array.write(data, mark, a - mark);

				save = false;
				a += 3;
				mark = a + 1;

				continue;
			}

			//jepssen chunk
			if (a < data.length - 36 && data[a + 2] == 0 && data[a + 3] == 0 && data[a + 36] == 0x47)
			{
				if (save && mark <= a)
					array.write(data, mark, a - mark);

				save = false;
				a += 35;
				mark = a + 1;

				continue;
			}

			//ts:
		//	if (a < data.length - 188 && data[a] == 0x47)
			if (a < data.length && data[a] == 0x47)
			{
				int chunk_offset = 0;

				//handan chunk
			//	if (data[a + 188] != 0x47 && data[a + 188] != 0x7F)
				if (a < data.length - 188 && data[a + 188] != 0x47 && data[a + 188] != 0x7F)
				{
					int i = a + 188;
					int j;
					int k = a + 189;
					int l = hav_chunk.length;

					while (i > a)
					{
						j = 0;

						while (i > a && data[i] != hav_chunk[j])
							i--;

						for ( ; i > a && j < l && i + j < k; j++)
							if (data[i + j] != hav_chunk[j])
								break;

						/**
						 * found at least one byte of chunk
						 */
						if (j > 0)
						{
							/** ident of chunk doesnt match completely */
							if (j < l && i + j < k)
							{
								i--;
								continue;
							}

							/** 
							 * re-sorts packet in array 
							 */
							if (i + 0x200 + (k - i) < data.length)
							{
								chunk_offset = 0x200;
								System.arraycopy(data, i + chunk_offset, data, i, k - i - 1);
								System.arraycopy(data, a, data, a + chunk_offset, i - a);
							}

							break;
						}
					}

					//jepssen chunk
					if (chunk_offset == 0 && a < data.length - 224 && data[a + 190] == 0 && data[a + 191] == 0 && data[a + 224] == 0x47)
					{}

					//koscom chunk
					else if (chunk_offset == 0 && a < data.length - 224 && data[a + 190] == 0 && data[a + 191] == 0 && data[a + 192] == 0x47)
					{}

					else if (chunk_offset == 0)
						continue;
				}

				if (save && mark <= a)
					array.write(data, mark, a - mark);

				mark = a;

				PID = (0x1F & data[a + 1])<<8 | (0xFF & data[a + 2]);

				if (include.length > 0 && Arrays.binarySearch(include, PID) < 0)
					save = false;

				else if ((ID == PID || ID == -1) && (0xD & data[a + 3]>>>4) == 1)
				{  
					if ((0x20 & data[a + 3]) != 0)       //payload start position, adaption field
						mark = a + 5 + (0xFF & data[a + 4]);
					else
						mark = a + 4;

					if ((0x40 & data[a + 1]) != 0)    //start indicator
					{
						if (data[mark] == 0 && data[mark + 1] == 0 && data[mark + 2] == 1 && (0xF0 & data[mark + 3]) == 0xE0) //DM06032004 081.6 int18 fix
						{
							ID = PID;
							mark = mark + 9 + (0xFF & data[mark + 8]);
							int[] currentposition = { a, array.size() };
							positionList.add(currentposition);
						}
						else
							save = false;
					}

					if (ID == PID)
						save = true;
				}
				else
					save = false;

				a += 187;
				a += chunk_offset;
				mark += chunk_offset;
			}

		}

		//save last marked packet
		if (save && mark < data.length && a <= data.length)
			array.write(data, mark, a - mark);


		processed_PID = ID;

		return array.toByteArray();
	}

	/**
	 * TS 192
	 */
	private byte[] parseTS192(byte[] data, int[] include)
	{
		ByteArrayOutputStream array = new ByteArrayOutputStream();

		boolean save = false;

		int mark = 0;
		int offset = 0;
		int ID = -1;

		for (int a = 0, PID, dl = data.length - 9; a < dl; a++)
		{
			//ts:
			if (a < data.length - 192 && data[a] == 0x47)
			{
				if (save && mark <= a)
					array.write(data, mark, a - mark - 4);

				mark = a;

				PID = (0x1F & data[a + 1])<<8 | (0xFF & data[a + 2]);

				if (include.length > 0 && Arrays.binarySearch(include, PID) < 0)
					save = false;

				else if ((ID == PID || ID == -1) && (0xD & data[a + 3]>>>4) == 1)
				{  
					if ((0x20 & data[a + 3]) != 0)       //payload start position, adaption field
						mark = a + 5 + (0xFF & data[a + 4]);
					else
						mark = a + 4;

					if ((0x40 & data[a + 1]) != 0)    //start indicator
					{
						if (data[mark] == 0 && data[mark + 1] == 0 && data[mark + 2] == 1 && (0xF0 & data[mark + 3]) == 0xE0) //DM06032004 081.6 int18 fix
						{
							ID = PID;
							mark = mark + 9 + (0xFF & data[mark + 8]);
							int[] currentposition = { a, array.size() };
							positionList.add(currentposition);
						}
						else
							save = false;
					}

					if (ID == PID)
						save = true;
				}
				else
					save = false;

				a += 191;
			}

		}

		processed_PID = ID;

		return array.toByteArray();
	}

	/**
	 * PES_AV + MPG2
	 */
	private byte[] parseMPG2(byte[] pes_data, int[] include)
	{
		ByteArrayOutputStream array = new ByteArrayOutputStream();

		boolean savePacket = false;

		int mark = 0;
		int pes_headerlength = 9;
		int pes_offset = 6;
		int pes_payloadlength = 0;
		int pes_ID = -1;

		for (int offset = 0, offset2, pes_ID_tmp, returncode, j = pes_data.length - pes_headerlength; offset < j; )
		{
			if ((returncode = CommonParsing.validateStartcode(pes_data, offset)) < 0)
			{
				offset += -returncode;
				continue;
			}

			pes_ID_tmp = CommonParsing.getPES_IdField(pes_data, offset);

			if (pes_ID_tmp < CommonParsing.SYSTEM_END_CODE)
			{
				offset += 4;
				continue;
			}

			if (savePacket)
				array.write(pes_data, mark, offset - mark);

			mark = offset;

			if (include.length > 0 && Arrays.binarySearch(include, pes_ID_tmp) < 0)
				savePacket = false;

			else if ((pes_ID == pes_ID_tmp || pes_ID == -1) && (0xF0 & pes_ID_tmp) == 0xE0)
			{
				pes_ID = pes_ID_tmp;
				mark = offset + pes_headerlength + CommonParsing.getPES_ExtensionLengthField(pes_data, offset);

				int[] currentposition = { offset, array.size() };

				positionList.add(currentposition);
				savePacket = true;
			}

			else
				savePacket = false;

			offset2 = pes_ID_tmp < CommonParsing.SYSTEM_START_CODE ? 12 : 0; //BA

			pes_payloadlength = CommonParsing.getPES_LengthField(pes_data, offset);

			if (pes_payloadlength == 0) //zero length
				offset2 = pes_headerlength;

			offset += (offset2 == 0 ? (pes_offset + pes_payloadlength) : offset2);
		}

		processed_PID = pes_ID;

		return array.toByteArray();
	}

	/**
	 * MPG1
	 */
	private byte[] parseMPG1(byte[] pes_data, int[] include)
	{
		ByteArrayOutputStream array = new ByteArrayOutputStream();

		boolean savePacket = false;

		int mark = 0;
		int pes_headerlength = 7;
		int pes_offset = 6;
		int pes_payloadlength = 0;
		int pes_ID = -1;

		for (int offset = 0, offset2, pes_ID_tmp, returncode, shift, j = pes_data.length - pes_headerlength; offset < j; )
		{
			if ((returncode = CommonParsing.validateStartcode(pes_data, offset)) < 0)
			{
				offset += -returncode;
				continue;
			}

			pes_ID_tmp = CommonParsing.getPES_IdField(pes_data, offset);

			if (pes_ID_tmp < CommonParsing.SYSTEM_END_CODE)
			{
				offset += 4;
				continue;
			}

			if (savePacket)
				array.write(pes_data, mark, offset - mark);

			mark = offset;

			if (include.length > 0 && Arrays.binarySearch(include, pes_ID_tmp) < 0)
				savePacket = false;

			else if ((pes_ID == pes_ID_tmp || pes_ID == -1) && (0xF0 & pes_ID_tmp) == 0xE0)
			{
				pes_ID = pes_ID_tmp;

				shift = offset + pes_offset;

				skiploop:
				while(true)
				{
					switch (0xC0 & pes_data[shift])
					{
					case 0x40:
						shift += 2; 
						continue skiploop; 

					case 0x80:
						shift += 3; 
						continue skiploop; 

					case 0xC0:
						shift++;  
						continue skiploop; 

					case 0:
						break;
					}

					switch (0x30 & pes_data[shift])
					{
					case 0x20:
						shift += 5;  
						break skiploop; 

					case 0x30:
						shift += 10; 
						break skiploop; 

					case 0x10:
						shift += 5; 
						break skiploop; 

					case 0:
						shift++; 
						break skiploop; 
					}
				}

				mark = shift;

				int[] currentposition = { offset, array.size() };

				positionList.add(currentposition);
				savePacket = true;
			}

			else
				savePacket = false;


			offset2 = pes_ID_tmp < CommonParsing.SYSTEM_START_CODE ? 12 : 0; //BA

			pes_payloadlength = CommonParsing.getPES_LengthField(pes_data, offset);

			if (pes_payloadlength == 0) //zero length
				offset2 = pes_headerlength;

			offset += (offset2 == 0 ? (pes_offset + pes_payloadlength) : offset2);
		}

		processed_PID = pes_ID;

		return array.toByteArray();
	}

	/**
	 * PVA
	 */
	private byte[] parsePVA(byte[] pes_data, int[] include)
	{
		ByteArrayOutputStream array = new ByteArrayOutputStream();

		boolean savePacket = false;

		int mark = 0;
		int pes_headerlength = 8;
		int pes_offset = 8;
		int pes_payloadlength = 0;
		int pes_ID = -1;

		for (int offset = 0, pes_ID_tmp, j = pes_data.length - pes_headerlength; offset < j; )
		{
			if ((0xFF & pes_data[offset]) != 0x41 || (0xFF & pes_data[offset + 1]) != 0x56 || (0xFF & pes_data[offset + 4]) != 0x55)
			{
				offset++;
				continue;
			}

			if (savePacket)
				array.write(pes_data, mark, offset - mark);

			mark = offset;

			pes_ID_tmp = 0xFF & pes_data[offset + 2];

			if (pes_ID_tmp == 1)
			{
				pes_ID = 1;
				mark = ((0x10 & pes_data[offset + 5]) != 0) ? offset + pes_offset + 4 : offset + pes_offset;

				int[] currentposition = { offset, array.size() };

				positionList.add(currentposition);
				savePacket = true;
			}

			else
				savePacket = false;

			pes_payloadlength = (0xFF & pes_data[offset + 6])<<8 | (0xFF & pes_data[offset + 7]);

			offset += (pes_offset + pes_payloadlength);
		}

		processed_PID = pes_ID;

		return array.toByteArray();
	}
}

