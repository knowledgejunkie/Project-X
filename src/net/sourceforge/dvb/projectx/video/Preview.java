/*
 * @(#)Preview.java - prepare files for previewing
 *
 * Copyright (c) 2004-2005 by dvb.matt, All Rights Reserved.
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

	public String getProcessedFile()
	{
		return processed_file;
	}

	public String getProcessedPID()
	{
		if (processed_PID < 0)
			return "---";

		return (Integer.toHexString(processed_PID).toUpperCase());
	}

	public long load(long startposition, int size, List previewList, boolean direction, boolean all_gops, boolean fast_decode, Object[] _predefined_Pids, int _active_collection) throws IOException
	{
		predefined_Pids = _predefined_Pids;
		active_collection = _active_collection;

		preview_data = new byte[size];

		int filetype = 0;
		int read_offset = 0;

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

				break;
			}
		}

		preview_data = search(preview_data, startposition, filetype);

		long newposition = Common.getMpvDecoderClass().decodeArray(preview_data, direction, all_gops, fast_decode);

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

		for (int i = 0; i < previewList.size(); i++)
		{
			preview_object = (PreviewObject)previewList.get(i);

			if (startposition < preview_object.getEnd())
			{
				processed_file = "" + (i + 1) + "/" + previewList.size() + " - " + preview_object.getFile().getName();
				break;
			}
		}

		preview_data = null;
		//System.gc();

		return startposition;
	}

	private byte[] search(byte data[], long startposition, int filetype)
	{
		ByteArrayOutputStream array = new ByteArrayOutputStream();

		positionList.clear();

		byte[] hav_chunk = { 0x5B, 0x48, 0x4F, 0x4A, 0x49, 0x4E, 0x20, 0x41 }; //'[HOJIN A'

		int mark = 0;
		int offset = 0;
		int ID = -1;
		int[] include = new int[predefined_Pids.length];

		boolean save = false;


		for (int i = 0; i < include.length; i++) 
			include[i] = Integer.parseInt(predefined_Pids[i].toString().substring(2), 16);

		Arrays.sort(include);

		for (int a = 0; a < data.length - 9; a++)
		{
			//mpg es:
			if (filetype == CommonParsing.ES_MPV_TYPE)
				return data;

			//pva:
			if (filetype == CommonParsing.PVA_TYPE && (0xFF & data[a]) == 0x41 && (0xFF & data[a + 1]) == 0x56 && (0xFF & data[a + 4]) == 0x55)
			{
				if (save)
					array.write(data, mark, a - mark);

				mark = a;

				if (data[a + 2] == 1)
				{
					ID = 1;
					mark = ((0x10 & data[a + 5]) != 0) ? a + 12 : a + 8;
					int currentposition[] = { a,array.size() };
					positionList.add(currentposition);
					save = true;
				}
				else
					save = false;

				a += 7 + ((0xFF & data[a + 6])<<8 | (0xFF & data[a + 7]));
			}

			//humax .vid workaround, skip special data chunk
			if (filetype == CommonParsing.TS_TYPE && data[a] == 0x7F && data[a + 1] == 0x41 && data[a + 2] == 4 && data[a + 3] == (byte)0xFD)
			{
				if (save && mark <= a)
					array.write(data, mark, a - mark);

				save = false;
				a += 1183;
				mark = a + 1;

				continue;
			}

			//ts:
			if (filetype == CommonParsing.TS_TYPE && a < data.length - 188 && data[a] == 0x47)
			{
				int chunk_offset = 0;

				if (data[a + 188] != 0x47 && data[a + 188] != 0x7F)
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

					if (chunk_offset == 0)
						continue;
				}

				if (save && mark <= a)
					array.write(data, mark, a - mark);

				mark = a;
				int PID = (0x1F & data[a + 1])<<8 | (0xFF & data[a + 2]);

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

			//mpg2-ps
			if ((filetype == CommonParsing.MPEG2PS_TYPE || filetype == CommonParsing.PES_AV_TYPE) && data[a] == 0 && data[a + 1] == 0 && data[a + 2] == 1)
			{
				int PID = 0xFF&data[a+3];

				if (PID < 0xB9)
					continue;

				if (save)
					array.write(data,mark,a-mark);

				mark = a;

				if (include.length>0 && Arrays.binarySearch(include,PID)<0)
					save=false;

				else if ((ID==PID || ID==-1) && (0xF0&PID)==0xE0)
				{
					ID=PID;
					mark = a + 9 + (0xFF&data[a+8]);
					int currentposition[] = { a,array.size() };
					positionList.add(currentposition);
					save=true;
				}
				else
					save=false;

				offset=(0xFF&data[a+3])<0xBB?11:0;
				a += (offset==0) ? (5+((0xFF&data[a+4])<<8 | (0xFF&data[a+5]))) : offset;
			}

			//mpg1-ps
			if (filetype == CommonParsing.MPEG1PS_TYPE && data[a]==0 && data[a+1]==0 && data[a+2]==1)
			{
				int PID = 0xFF&data[a+3];

				if (PID < 0xB9)
					continue;

				if (save)
					array.write(data,mark,a-mark);

				mark = a;

				if (include.length>0 && Arrays.binarySearch(include,PID)<0)
					save=false;

				else if ((ID==PID || ID==-1) && (0xF0&PID)==0xE0){
					ID=PID;
					int shift=a+6;
					skiploop:

					while(true)
					{
						switch (0xC0&data[shift]) {
						case 0x40:	shift+=2; 
									continue skiploop; 
						case 0x80: 	shift+=3; 
									continue skiploop; 
						case 0xC0: 	shift++;  
									continue skiploop; 
						case 0: 	break;
						}
						switch (0x30&data[shift]) {
						case 0x20:	shift+=5;  
									break skiploop; 
						case 0x30: 	shift+=10; 
									break skiploop; 
						case 0x10:	shift+=5; 
									break skiploop; 
						case 0:    	shift++; 
									break skiploop; 
						}
					}

					mark = shift;
					int currentposition[] = { a,array.size() };
					positionList.add(currentposition);
					save=true;
				}
				else
					save = false;

				offset = (0xFF & data[a + 3]) < 0xBB ? 11 : 0;
				a += (offset == 0) ? (5 + ((0xFF & data[a + 4])<<8 | (0xFF & data[a + 5]))) : offset;
			}
		}

		processed_PID = ID;

		return array.toByteArray();
	}
}
