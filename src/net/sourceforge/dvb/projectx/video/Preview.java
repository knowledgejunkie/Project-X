/*
 * @(#)PREVIEW.java - prepare files for previewing
 *
 * Copyright (c) 2004 by dvb.matt, All Rights Reserved.
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

import net.sourceforge.dvb.projectx.xinput.XInputFile;

public class Preview
{
	private byte preview_data[];
	private int loadSizeForward, active_collection, processed_PID;
	private String processed_file;

	private ArrayList positionList, speciallist;
	private int position[];
	private PreviewObject preview_object;
	private MPVD mpv_decoder;

	public Preview(int loadSizeForward, MPVD mpv_decoder)
	{
		this.loadSizeForward = loadSizeForward;
		this.mpv_decoder = mpv_decoder;

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
		return (Integer.toHexString(processed_PID).toUpperCase());
	}

	public long load(long startposition, int size, ArrayList previewList, boolean direction, boolean all_gops, boolean fast_decode, ArrayList speciallist, int active_collection) throws IOException
	{
		this.speciallist = speciallist;
		this.active_collection = active_collection;

		int filetype = 0;
		preview_data = new byte[size];
		int read_offset = 0;

		for (int a=0; a < previewList.size(); a++)
		{
			preview_object = (PreviewObject)previewList.get(a);
			filetype = preview_object.getType();

			if (startposition < preview_object.getEnd())
			{
				XInputFile lXInputFile = (XInputFile)preview_object.getFile();
				lXInputFile.randomAccessOpen("r");
				lXInputFile.randomAccessSeek(startposition - preview_object.getStart());
				lXInputFile.randomAccessRead(preview_data, read_offset, size);
				lXInputFile.randomAccessClose();

				if (preview_object.getEnd() - startposition < size && a < previewList.size() - 1)
				{
					a++;

					int diff = (int)(preview_object.getEnd() - startposition);
					byte data2[] = new byte[size];

					preview_object = (PreviewObject)previewList.get(a);

					lXInputFile = (XInputFile)preview_object.getFile();
					lXInputFile.randomAccessSingleRead(data2, 0);

					System.arraycopy(data2, 0, preview_data, diff, size - diff);
					data2 = null;
				}

				break;
			}
		}

		preview_data = search(preview_data, startposition, filetype);

		long newposition = MPVD.picture.decodeArray(preview_data, direction, all_gops, fast_decode);

		for (int a = positionList.size() - 1; a > -1; a--)
		{
			position = (int[])positionList.get(a);
			if (position[1] <= newposition)
			{
				startposition += position[0];
				a = 0;
			}
		}

		if (positionList.size() == 0) 
			startposition += newposition;

		for (int a=0; a < previewList.size(); a++)
		{
			preview_object = (PreviewObject)previewList.get(a);

			if (startposition < preview_object.getEnd())
			{
				processed_file = "" + (a + 1) + "/" + previewList.size() + " " + preview_object.getFile().getName();
				break;
			}
		}

		preview_data = null;
		System.gc();

		return startposition;
	}

	private byte[] search(byte data[], long startposition, int filetype)
	{
		ByteArrayOutputStream array = new ByteArrayOutputStream();
		positionList.clear();
		int mark = 0, offset = 0, ID = -1;
		boolean save = false;

		ArrayList abc = (ArrayList)speciallist.get(active_collection);
		int[] include = new int[abc.size()];

		for (int a=0; a < include.length; a++) 
			include[a] = Integer.parseInt(abc.get(a).toString().substring(2),16);

		java.util.Arrays.sort(include);

		for (int a=0; a < data.length-9; a++)
		{
			//mpg es:
			if (filetype==9)
				return data;

			//pva:
			if (filetype == 1 && (0xFF & data[a]) == 0x41 && (0xFF & data[a + 1]) == 0x56 && (0xFF & data[a + 4]) == 0x55)
			{
				if (save)
					array.write(data, mark, a - mark);

				mark = a;

				if (data[a+2]==1)
				{
					ID=1;
					mark = ((0x10 & data[a + 5]) != 0) ? a+12 : a+8;
					int currentposition[] = { a,array.size() };
					positionList.add(currentposition);
					save=true;
				}
				else
					save=false;

				a += 7 + ((0xFF & data[a+6])<<8 | (0xFF & data[a+7]));
			}


			//humax .vid workaround, skip special data chunk
			if (filetype == 11 && data[a] == 0x7F && data[a + 1] == 0x41 && data[a + 2] == 4 && data[a + 3] == (byte)0xFD)
			{
				if (save && mark <= a)
					array.write(data, mark, a - mark);

				save = false;
				a += 1183;
				mark = a + 1;

				continue;
			}

			//ts:
			//if (filetype == 11 && a < data.length-188 && data[a] == 0x47 && data[a+188] == 0x47)
			if (filetype == 11 && a < data.length-188 && data[a] == 0x47)
			{
				if (data[a + 188] != 0x47 && data[a + 188] != 0x7F)
					continue;

				if (save && mark <= a)
					array.write(data,mark,a-mark);

				mark = a;
				int PID = (0x1F & data[a+1])<<8 | (0xFF & data[a+2]);

				if (include.length > 0 && java.util.Arrays.binarySearch(include,PID)<0)
					save = false;

				else if ((ID==PID || ID==-1) && (0xD & data[a+3]>>>4)==1)
				{  
					if ((0x20 & data[a+3])!=0)       //payload start position, adaption field
						mark = a + 5 + (0xFF&data[a+4]);
					else
						mark = a + 4;

					if ((0x40 & data[a+1]) != 0)    //start indicator
					{
						if (data[mark]==0 && data[mark+1]==0 && data[mark+2]==1 && (0xF0&data[mark+3])==0xE0) //DM06032004 081.6 int18 fix
						{
							ID=PID;
							mark = mark + 9 + (0xFF&data[mark+8]);
							int currentposition[] = { a, array.size() };
							positionList.add(currentposition);
						}
						else
							save=false;
					}

					if (ID==PID)
						save=true;
				}
				else
					save=false;

				a += 187;
			}

			//mpg2-ps
			if ((filetype==3 || filetype==4) && data[a]==0 && data[a+1]==0 && data[a+2]==1)
			{
				int PID = 0xFF&data[a+3];

				if (PID < 0xB9)
					continue;

				if (save)
					array.write(data,mark,a-mark);

				mark = a;

				if (include.length>0 && java.util.Arrays.binarySearch(include,PID)<0)
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
			if (filetype==2 && data[a]==0 && data[a+1]==0 && data[a+2]==1)
			{
				int PID = 0xFF&data[a+3];

				if (PID < 0xB9)
					continue;

				if (save)
					array.write(data,mark,a-mark);

				mark = a;

				if (include.length>0 && java.util.Arrays.binarySearch(include,PID)<0)
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
					save=false;

				offset=(0xFF&data[a+3])<0xBB?11:0;
				a += (offset==0) ? (5+((0xFF&data[a+4])<<8 | (0xFF&data[a+5]))) : offset;
			}
		}

		processed_PID = ID;

		return array.toByteArray();
	}
}
