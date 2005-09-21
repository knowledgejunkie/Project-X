/*
 * @(#)SCAN.java - pre-scanning to check supported files
 *
 * Copyright (c) 2002-2005 by dvb.matt, All Rights Reserved. 
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;

import net.sourceforge.dvb.projectx.audio.Audio;
import net.sourceforge.dvb.projectx.common.Resource;
import net.sourceforge.dvb.projectx.common.Common;
import net.sourceforge.dvb.projectx.video.Video;
import net.sourceforge.dvb.projectx.xinput.XInputFile;
import net.sourceforge.dvb.projectx.common.Keys;
import net.sourceforge.dvb.projectx.parser.CommonParsing;

import net.sourceforge.dvb.projectx.xinput.StreamInfo;

public class Scan extends Object {

	private final String msg_1 = Resource.getString("scan.msg1");
	private final String msg_2 = Resource.getString("scan.msg2");
	private final String msg_3 = Resource.getString("scan.msg3");
	private final String msg_4 = Resource.getString("scan.msg4");
	private final String msg_5 = Resource.getString("scan.msg5");
	private final String msg_6 = Resource.getString("scan.msg6");
	private final String msg_7 = Resource.getString("scan.msg7");
	private final String msg_8 = Resource.getString("scan.msg8");
	private final String msg_9 = Resource.getString("scan.msg9");

	private String addInfo = "";
	private String playtime = "";

	private boolean hasVideo = false;

	private byte[] vbasic = new byte[12];

	private int filetype = CommonParsing.Unsupported;

	private ArrayList pidlist;
	private ArrayList video_streams;
	private ArrayList audio_streams;
	private ArrayList ttx_streams;
	private ArrayList pic_streams;

	private Audio Audio = new Audio();

	/**
	 *
	 */
	public Scan()
	{
		video_streams = new ArrayList();
		audio_streams = new ArrayList();
		ttx_streams = new ArrayList();
		pic_streams = new ArrayList();
		pidlist = new ArrayList();
	}

	/**
	 * show ScanInfos
 	 */
	public void getStreamInfo(XInputFile aXInputFile)
	{
		long length = aXInputFile.length();

		String _name = getName(aXInputFile);
		String _location = getLocation(aXInputFile);
		String _date = getDate(aXInputFile);
		String _size = getSize(aXInputFile);

		StreamInfo streamInfo = aXInputFile.getStreamInfo();

		if (aXInputFile.exists())
		{
			if (streamInfo == null)
				streamInfo = new StreamInfo();

			// type must be first when scanning
			streamInfo.setStreamInfo(aXInputFile.getFileType().getName(), getType(aXInputFile), _name, _location, _date, _size, getPlaytime(), getVideo(), getAudio(), getText(), getPics());

			streamInfo.setStreamType(filetype);
			streamInfo.setPIDs(getPIDs());
			streamInfo.setVideoHeader(getVBasic());
		}
		else
			streamInfo = new StreamInfo("", Resource.getString("ScanInfo.NotFound"), _name, _location, "", "", "");

		aXInputFile.setStreamInfo(streamInfo);
	}

	/**
	 *
	 */
	private String getType(XInputFile aXInputFile)
	{ 
		filetype = testFile(aXInputFile, true);

		return (Keys.ITEMS_FileTypes[filetype].toString() + addInfo); 
	}

	/**
	 *
	 */
	private String getName(XInputFile aXInputFile)
	{
		return aXInputFile.getName();
	}
 
	/**
	 *
	 */
	private String getLocation(XInputFile aXInputFile)
	{
		return aXInputFile.getParent();
	}
 
	/**
	 *
	 */
	private String getDate(XInputFile aXInputFile)
	{
		return DateFormat.getDateInstance(DateFormat.LONG).format(new Date(aXInputFile.lastModified()))
		       + "  " + DateFormat.getTimeInstance(DateFormat.LONG).format(new Date(aXInputFile.lastModified()));
	}
 
	/**
	 *
	 */
	private String getSize(XInputFile aXInputFile)
	{
		long length = aXInputFile.length();

		return (String.valueOf(length / 1048576) + " MB (" + Common.formatNumber(length) + " " + Resource.getString("ScanInfo.Bytes") + ")");
	}

	/**
	 *
	 */
	private String getPlaytime() 
	{ 
		return playtime; 
	}

	/**
	 *
	 */
	private Object[] getVideo()
	{ 
		return video_streams.toArray();
	}

	/**
	 *
	 */
	private Object[] getAudio()
	{ 
		return audio_streams.toArray();
	}

	/**
	 *
	 */
	private Object[] getText()
	{
		return ttx_streams.toArray();
	}

	/**
	 *
	 */
	private Object[] getPics()
	{
		return pic_streams.toArray();
	}

	/**
	 *
	 */
	private Object[] getPIDs()
	{
		return pidlist.toArray();
	}

	/**
	 *
	 */
	private String getAudioTime(long len)
	{
		return Common.formatTime_1((len * 8000) / Audio.Bitrate);
	}

	/**
	 *
	 */
	public byte[] getVBasic()
	{ 
		if (hasVideo)
			return vbasic; 

		return null;
	}

	/**
	 *
	 */
	private int AC3Audio(byte[] check)
	{
		audiocheck:
		for (int a=0; a<10000; a++)
		{
			if (Audio.AC3_parseHeader(check,a) < 0)
				continue audiocheck;

			for (int b=0; b < 17; b++)
			{
				if (Audio.AC3_parseNextHeader(check,a+Audio.Size+b) == 1)
				{
					if ( (0xFF & check[a+Audio.Size]) > 0x3f || (0xFF & check[a+Audio.Size])==0 ) //smpte
						continue audiocheck;

					audio_streams.add(Audio.AC3_saveAnddisplayHeader());

					return 1;
				}
			}
		} 

		return 0;
	}

	/* DTS stuff taken from the VideoLAN project. */ 
	/* Added by R One, 2003/12/18. */
	private void DTSAudio(byte[] check)
	{ 
		audiocheck: 
		for (int i = 0; i < 10000; i++)
		{ 
			if (Audio.DTS_parseHeader(check, i) < 0) 
				continue audiocheck; 

			for (int j = 0; j < 15; j++)
			{ 
				if (Audio.DTS_parseNextHeader(check, i + Audio.Size + j) == 1)
				{ 
					if ( (0xFF & check[i + Audio.Size]) > 0x7F || (0xFF & check[i + Audio.Size]) == 0 ) //smpte 
						continue audiocheck; 

					audio_streams.add(Audio.DTS_saveAnddisplayHeader()); 

					return; 
				} 
			} 
		} 
	} 

	/**
	 *
	 */
	private void MPEGAudio(byte[] check)
	{ 
		audiocheck:
		for (int a=0; a < 10000; a++)
		{
			if (Audio.MPA_parseHeader(check, a) < 0)
				continue audiocheck;

			if (Audio.MPA_parseNextHeader(check, a + Audio.Size) < 0)
				continue audiocheck;

			audio_streams.add(Audio.MPA_saveAnddisplayHeader());

			return;
		}
	}

	/**
	 *
	 */
	private byte[] loadPES(byte[] check, int a)
	{ 
		ByteArrayOutputStream bytecheck = new ByteArrayOutputStream();
		int jump, offs, len;
		boolean mpg1;

		for (; a < check.length; )
		{
			jump = (0xFF & check[a + 4])<<8 | (0xFF & check[a + 5]);
			mpg1 = (0x80 & check[a + 6]) == 0 ? true : false;
			offs = a + 6 + ( !mpg1 ? 3 + (0xFF & check[a+8]) : 0);
			len = jump - ( !mpg1 ? 3 + (0xFF & check[a+8]) : 0);

			if (offs + len > check.length)
				break;

			bytecheck.write(check, offs, len);

			a += 6 + jump;
		}

		return bytecheck.toByteArray();
	}

	/**
	 *
	 */
	private void loadMPG2(byte[] check, int b, boolean vdr, boolean mpg1, boolean nullpacket, int size) throws IOException
	{ 
		Hashtable table = new Hashtable();
		ScanObject scanobject;

		String str;
		int jump = 1, id;
		int end = check.length > 580000 ? 512000 : check.length - 65000;

		mpg2check:
		for (int a = b, returncode; a < end; a += jump)
		{
			if ((returncode = CommonParsing.validateStartcode(check, a)) < 0)
			{
				jump = -returncode;
				continue mpg2check; 
			}

			id = CommonParsing.getPES_IdField(check, a);
			str = String.valueOf(id);

			if (id == CommonParsing.PACK_START_CODE)
			{
				if ((0xC0 & check[4 + a]) == 0)  //mpg1
					jump = 12;

				else if ((0xC0 & check[4 + a]) == 0x40) //mpg2
					jump = 14 + (7 & check[13 + a]);

				else
					jump = 4;
			}

			//video mpg  e0..ef
			else if ( (0xF0 & id) == 0xE0 )
			{
				jump = nullpacket ? 2048 : (6 + CommonParsing.getPES_LengthField(check, a));

				if (!table.containsKey(str))
					table.put(str, new ScanObject(id));

				scanobject = (ScanObject)table.get(str);

				scanobject.write(check, a + 6 + (!mpg1 ? 3 + CommonParsing.getPES_ExtensionLengthField(check, a) : 0), jump - (!mpg1 ? 6 - 3 - CommonParsing.getPES_ExtensionLengthField(check, a) : 0) ); 
			}

			//audio mpg  c0..df
			else if ( (0xE0 & id) == 0xC0 )
			{
				jump = 6 + CommonParsing.getPES_LengthField(check, a);

				if (!table.containsKey(str))
					table.put(str, new ScanObject(id));

				scanobject = (ScanObject)table.get(str);

				scanobject.write(check, a, jump ); 
			}

			//private bd
			else if (id == CommonParsing.PRIVATE_STREAM_1_CODE)
			{
				jump = 6 + CommonParsing.getPES_LengthField(check, a);

				int pes_extensionlength = CommonParsing.getPES_ExtensionLengthField(check, a);
				boolean pes_alignment = (4 & check[a + 6]) != 0;

				if (pes_extensionlength == 0x24 && (0xF0 & check[a + 9 + pes_extensionlength])>>>4 == 1)
				{
					str = "SubID 0x" + Integer.toHexString((0xFF & check[a + 9 + pes_extensionlength])).toUpperCase();

					if (ttx_streams.indexOf(str) < 0)
						ttx_streams.add(str);
				}

				else if ( ((!mpg1 && !vdr) || (vdr && pes_alignment)) && ((0xF0 & check[a + 9 + pes_extensionlength])>>>4 == 2 || (0xF0 & check[a + 9 + pes_extensionlength])>>>4 == 3))
				{
					str = "SubID 0x" + Integer.toHexString((0xFF & check[a + 9 + pes_extensionlength])).toUpperCase();

					if (pic_streams.indexOf(str) < 0)
						pic_streams.add(str);
				}

				else
				{
					if (!vdr)
					{
						id = 0xFF & check[a + 9 + pes_extensionlength];
						str = String.valueOf(id);

						check[a + 8] = (byte)(4 + pes_extensionlength);
					}

					if (!table.containsKey(str))
						table.put(str, new ScanObject(id));

					scanobject = (ScanObject)table.get(str);

					scanobject.write(check, a, jump ); 
				}
			}

			else
			{
				switch (id)
				{
				case 0xBB:
				case 0xBC:
				case 0xBE:
				case 0xBF:
				case 0xF0:
				case 0xF1:
				case 0xF2:
				case 0xF3:
				case 0xF4:
				case 0xF5:
				case 0xF6:
				case 0xF7:
				case 0xF8:
				case 0xF9:
				case 0xFA:
				case 0xFB:
				case 0xFC:
				case 0xFD:
				case 0xFE:
				case 0xFF:
					jump = 6 + CommonParsing.getPES_LengthField(check, a); 
					break; 

				default: 
					jump = 1;
				}
			}
		}


		for (Enumeration n = table.keys(); n.hasMoreElements() ; )
		{
			str = n.nextElement().toString();
			id = Integer.parseInt(str);

			scanobject = (ScanObject)table.get(str);

			if ( (0xF0 & id) == 0xE0)
			{
				try {
					checkVid(scanobject.getData());

				} catch (Exception e) { 

					video_streams.add(msg_8); 
				}
			}

			else
			{
				try {
					checkPES(scanobject.getData());

				} catch (Exception e) { 

					audio_streams.add(msg_8); 
				}
			}
		}

		table.clear();

		return;
	}

	/**
	 *
	 */
	private void loadPVA(byte[] check, int a) throws IOException
	{ 
		Hashtable table = new Hashtable();
		ScanObject scanobject;

		String str;
		int jump, id;
		int end = check.length > 580000 ? 512000 : check.length - 65000;

		while ( a < end)
		{
			jump = (0xFF & check[a+6])<<8 | (0xFF & check[a+7]);

			if (a + 8 + ((1 & check[a+5]>>>4) * 4) + jump > check.length) 
				break;

			id = 0xFF & check[a+2];
			str = String.valueOf(id);

			if (!table.containsKey(str))
				table.put(str, new ScanObject(id));

			scanobject = (ScanObject)table.get(str);

			switch (id)
			{
			case 1:
				scanobject.write(check, a + 8 + ((1 & check[a+5]>>>4) * 4), jump ); 
				break; 

			default:
				scanobject.write(check, a + 8, jump ); 
			}

			a += 8 + jump;
		}

		for (Enumeration n = table.keys(); n.hasMoreElements() ; )
		{
			str = n.nextElement().toString();

			scanobject = (ScanObject)table.get(str);

			if (str.equals("1"))
			{
				try {
					checkVid(scanobject.getData());

				} catch  ( Exception e) { 

					video_streams.add(msg_8); 
				}
			}
			else
			{
				try {
					checkPES(scanobject.getData());

				} catch  ( Exception e) { 

					audio_streams.add(msg_8); 
				}
			}
		}

		table.clear();

		return;
	}

	/**
	 *
	 */
	private void checkPES(byte[] check)
	{ 
		checkPES(check, 0);
	}

	/**
	 *
	 */
	private void checkPES(byte[] check, int a)
	{ 
		int end = a + 8000;

		rawcheck:
		for (int returncode; a < end; a++)
		{
			if ((returncode = CommonParsing.validateStartcode(check, a)) < 0)
			{
				a += (-returncode) - 1;
				continue rawcheck;
			}

			if ( (0xE0 & check[a+3]) == 0xC0 || CommonParsing.getPES_IdField(check, a) == CommonParsing.PRIVATE_STREAM_1_CODE)
			{
				int next = a + 6 + CommonParsing.getPES_LengthField(check, a);

				if (CommonParsing.validateStartcode(check, next) < 0)
					continue rawcheck;

				if ( (0xE0 & check[a+3]) == 0xC0 && (0xE0 & check[a+3]) == (0xE0 & check[next+3]) )
				{
					MPEGAudio(loadPES(check,a));

					return;
				}

				else if (CommonParsing.getPES_IdField(check, a) == CommonParsing.PRIVATE_STREAM_1_CODE && CommonParsing.getPES_IdField(check, a) == CommonParsing.getPES_IdField(check, next))
				{
					byte buffer[] = loadPES(check, a);

					if (AC3Audio(buffer) < 1)
						DTSAudio(buffer);

					return;
				}
			}
		}
	}

	/**
	 *
	 */
	private void checkVid(byte[] check)
	{ 
		checkVid(check, check.length - 630);
	}

	/**
	 *
	 */
	private boolean checkVid(byte[] check, int length)
	{ 
		ByteArrayOutputStream bytecheck = new ByteArrayOutputStream();

		for (int i = 0, returncode, pes_ID; i < length; i++)
		{
			if ((returncode = CommonParsing.validateStartcode(check, i)) < 0 || CommonParsing.getPES_IdField(check, i) != CommonParsing.SEQUENCE_HEADER_CODE)
			{
				i += (returncode < 0 ? -returncode : 4) - 1;
				continue;
			}

			for (int j = 7, mpgtype = 1; j < 600; j++)
			{
				if ((returncode = CommonParsing.validateStartcode(check, i + j)) < 0)
				{
					j += (-returncode) - 1;
					continue;
				}

				pes_ID = CommonParsing.getPES_IdField(check, i + j);

				if (pes_ID == CommonParsing.EXTENSION_START_CODE && (0xF0 & check[4 + i + j]) == 0x10)
					mpgtype = 2;

				else if (pes_ID == CommonParsing.GROUP_START_CODE || pes_ID == CommonParsing.PICTURE_START_CODE)
				{
					hasVideo = true;
					System.arraycopy(check, i, vbasic, 0, 12);
					bytecheck.write(check, i, 20);

					video_streams.add("MPEG-" + mpgtype + ", " + Video.getVideoformatfromBytes(bytecheck.toByteArray()));

					return true;
				}
			} 
		} 

		return false;
	}

	/**
	 *
	 */
	private void PMTcheck(byte[] check, int a)
	{ 
		ByteArrayOutputStream bytecheck = new ByteArrayOutputStream();
	//	pidlist.clear();

		tscheck:
		for ( ; a < check.length - 1000; a++)
		{
			if ( check[a] != 0x47 || check[a + 188] != 0x47 || check[a + 376] != 0x47 ) 
				continue tscheck;

			if ( (0x30 & check[a + 3]) != 0x10 || check[a + 4] != 0 || check[a + 5] != 2 || (0xF0 & check[a + 6]) != 0xB0 )
			{ 
				a += 187; 
				continue tscheck; 
			}

			bytecheck.write(check, a + 4, 184);
			int pmtpid = (0x1F & check[a + 1])<<8 | (0xFF & check[a + 2]);

			if ( bytecheck.size() < 188 )
			{ 
				a += 188;

				addpack:
				for ( ; a < check.length - 500; a++)
				{
					if ( check[a] != 0x47 || check[a + 188] != 0x47 || check[a + 376] != 0x47 ) 
						continue addpack;

					if ( ((0x1F & check[a + 1])<<8 | (0xFF & check[a + 2])) != pmtpid || (0x40 & check[a + 1]) != 0 || (0x30 & check[a + 3]) != 0x10 )
					{ 
						a += 187; 
						continue addpack; 
					}

					bytecheck.write(check, a + 4, 184);

					if ( bytecheck.size() > 188 ) 
						break addpack;
				}
			}

			byte[] pmt = bytecheck.toByteArray();

			if (pmt.length > 5)
			{
				int sid = (0xFF & pmt[4])<<8 | (0xFF & pmt[5]);
				pidlist.add("" + sid);
				pidlist.add("" + pmtpid);
				addInfo = " (SID 0x" + Integer.toHexString(sid).toUpperCase() + " ,PMT 0x" + Integer.toHexString(pmtpid).toUpperCase() + ")";
			}

			int pmt_len = (0xF&pmt[2])<<8 | (0xFF&pmt[3]);

			pidsearch:
			for (int b=8, r=8; b < pmt_len-4 && b < pmt.length-6; b++)
			{
				r = b;

				if ( (0xe0 & pmt[b+1]) != 0xe0 ) 
					continue pidsearch;

				int pid = (0x1F & pmt[b+1])<<8 | (0xFF & pmt[b+2]);

				switch(0xFF & pmt[b])
				{
				case 1:
				case 2:
					getDescriptor(pmt, b+5, (b += 4+ (0xFF & pmt[b+4])), pid, 2);
					pidlist.add("" + pid); 
					break; 

				case 3:
				case 4:
					getDescriptor(pmt, b+5, (b += 4+ (0xFF & pmt[b+4])), pid, 4);
					pidlist.add("" + pid); 
					break; 

				case 0x1B:
					getDescriptor(pmt, b+5, (b += 4+ (0xFF & pmt[b+4])), pid, 0x1B);
					pidlist.add("" + pid); 
					break; 

				case 0x80:
				case 0x81:  //private data of AC3 in ATSC
				case 0x82: 
				case 0x83: 
				case 6:
					getDescriptor(pmt, b+5, (b += 4+ (0xFF & pmt[b+4])), pid, 6);
					pidlist.add("" + pid); 
					break; 

				default: 
					b += 4+ (0xFF & pmt[b+4]);
				}

				if (b < 0) 
					b = r;
			}
			return;
		} 
		return;
	}

	/**
	 *
	 */
	private void getDescriptor(byte check[], int off, int end, int pid, int type)
	{
		String str = "";
		int chunk_end = 0;

		try
		{
			loop:
			for (; off < end && off < check.length; off++)
			{
				switch(0xFF & check[off])
				{
				case 0x59:  //dvb subtitle descriptor
					type = 0x59;
					chunk_end = off + 2 + (0xFF & check[off+1]);
					str += "(";

					for (int a=off+2; a<chunk_end; a+=8)
					{
						for (int b=a; b<a+3; b++) //language
							str += (char)(0xFF & check[b]);

						int page_type = 0xFF & check[a+3];
						int comp_page_id = (0xFF & check[a+4])<<16 | (0xFF & check[a+5]);
						int anci_page_id = (0xFF & check[a+6])<<16 | (0xFF & check[a+7]);

						str += "_0x" + Integer.toHexString(page_type).toUpperCase();
						str += "_p" + comp_page_id;
						str += "_a" + anci_page_id + " ";
					}

					str += ")";
					break loop;

				case 0x56:  //teletext descriptor incl. index page + subtitle pages
					type = 0x56;
					chunk_end = off + 2 + (0xFF & check[off+1]);
					str += "(";

					for (int a=off+2; a<chunk_end; a+=5)
					{
						for (int b=a; b<a+3; b++) //language
							str += (char)(0xFF & check[b]);

						int page_type = (0xF8 & check[a+3])>>>3;
						int page_number = 0xFF & check[a+4];

						str += "_";

						switch (page_type)
						{
						case 1:
							str += "i";
							break;
						case 2:
							str += "s";
							break;
						case 3:
							str += "ai";
							break;
						case 4:
							str += "ps";
							break;
						case 5:
							str += "s.hip";
							break;
						default:
							str += "res";
						}

						str += Integer.toHexString((7 & check[a+3]) == 0 ? 8 : (7 & check[a+3])).toUpperCase();
						str += (page_number < 0x10 ? "0" : "") + Integer.toHexString(page_number).toUpperCase() + " ";
					}

					str += ")";
					//break loop;
					off++;
					off += (0xFF & check[off]);
					break;

				case 0xA:  //ISO 639 language descriptor
					str += "(";

					for (int a=off+2; a<off+5; a++)
						str += (char)(0xFF & check[a]);

					str += ")";
					off++;
					off += (0xFF & check[off]);
					break;

				case 0x6A:  //ac3 descriptor
					str += "(AC-3)";
					off++;
					off += (0xFF & check[off]);
					break;

				case 0xC3:  //VBI descriptor
					off++;

					switch (0xFF & check[off + 1])
					{
					case 4:
						str += "(VPS)";
						type = 0xC3;
						break;
					case 5:
						str += "(WSS)";
						break;
					case 6:
						str += "(CC)";
						break;
					case 1:
						str += "(EBU-TTX)";
						break;
					case 7:
						str += "(VBI)";
						break;
					}

					off += (0xFF & check[off]);
					break;

				case 0x52:  //ID of service
					chunk_end = off + 2 + (0xFF & check[off+1]);
					str += "(#" + (0xFF & check[off + 2]) + ")";
					off++;
					off += (0xFF & check[off]);
					break;

				case 0x6B:  //ancillary desc
					chunk_end = off + 2 + (0xFF & check[off + 1]);
					str += "(RDS)";
					off++;
					off += (0xFF & check[off]);
					break;

				case 0x5:  //registration descriptor
					chunk_end = off + 2 + (0xFF & check[off+1]);
					str += "(";

					for (int a=off+2; a<chunk_end; a++)
						str += (char)(0xFF & check[a]);

					str += ")";
	
				default:
					off++;
					off += (0xFF & check[off]);
				}
			}

			String out = "PID: 0x" + Integer.toHexString(pid).toUpperCase();

			switch (type)
			{
			case 0x59:
				pic_streams.add(out + str);
				break;

			case 0x56:
				ttx_streams.add(out + str);
				break;

			case 0x1B:
				video_streams.add(out + str + "(H.264)");
				break;

			case 2:
			case 0xC3:
				video_streams.add(out + str);
				break;

			case 4:
				audio_streams.add(out + str);
				break;

			default:
				audio_streams.add(out + str + "[PD]");
			}

		}
		catch (ArrayIndexOutOfBoundsException ae)
		{
			playtime += msg_6;
		}
	}

	/**
	 *
	 */
	private int testFile(XInputFile aXInputFile, boolean more)
	{
		long len = aXInputFile.length();

		int ret = testFile(aXInputFile, more, 0);

		if (ret != 0)
			return ret;

		// if type is not yet detected, try it again on a later position (10% of length)
		return testFile(aXInputFile, more, len / 10);
	}

	/**
	 *
	 */
	private int testFile(XInputFile aXInputFile, boolean more, long position)
	{
		video_streams.clear();
		audio_streams.clear();
		ttx_streams.clear();
		pic_streams.clear();
		pidlist.clear();

		addInfo = "";
		playtime = "";
		hasVideo = false; 

		boolean nullpacket = false;
		long size = 0;
		int buffersize = Integer.parseInt(Common.getSettings().getProperty(Keys.KEY_ScanBuffer));

		if (buffersize <= 0)
			buffersize = 1024000;

		int bs0 = buffersize / 100;
		int bs1 = buffersize / 50;
		int bs2 = buffersize / 10;
		int bs3 = buffersize / 4;
		int bs4 = buffersize - 65536;

		byte[] check = new byte[buffersize];
		ByteArrayOutputStream bytecheck = new ByteArrayOutputStream();

		try {

			size = aXInputFile.length();

			aXInputFile.getNewInstance().randomAccessSingleRead(check, position);

			riffcheck:
			for (int i = 0; i < bs0; i++)  //compressed as AC3,MPEG is currently better detected as ES-not RIFF
			{
				int ERRORCODE = Audio.WAV_parseHeader(check, i);

				if (ERRORCODE > -1)
				{
					Audio.saveHeader();
					if (more)
					{ 
						audio_streams.add(Audio.WAV_displayHeader());
						playtime = getAudioTime(size); 
					} 

					if (ERRORCODE > 0)
						return CommonParsing.ES_RIFF_TYPE;

					else if (Audio.lMode_extension > 1)
						break riffcheck;

					else
						return CommonParsing.ES_cRIFF_TYPE;
				}
			}

			supcheck:
			for (int i = 0; i < bs0; i++) 
			{
				if (check[i] != 0x53 || check[i + 1]!=0x50)
					continue supcheck;

				int supframe_size = (0xFF & check[i + 10])<<8 | (0xFF & check[i + 11]);
				int supframe_link = (0xFF & check[i + 12])<<8 | (0xFF & check[i + 13]);
				int supframe_check = (0xFF & check[i + 12 + supframe_link])<<8 | (0xFF & check[i + 13 + supframe_link]);
				int supframe_check2 = (0xFF & check[i + 36 + supframe_link])<<8 | (0xFF & check[i + 37 + supframe_link]);

				if (supframe_link == supframe_check - 24 && supframe_check == supframe_check2)
				{
					if (more)
					{
						int b = i + 14 + supframe_link, c = b + 24, d, xa, xe, ya, ye;
						for (d = b; d < c; d++)
						{
							switch(0xFF & check[d])
							{
							case 1:
								d++;
								continue;

							case 2:
								d += 24;
								continue;

							case 3:
							case 4:
								d += 2;
								continue;

							case 6:
								d += 4;
								continue;

							case 5:
								xa= (0xFF & check[++d])<<4 | (0xF0 & check[++d])>>>4;
								xe= (0xF & check[d])<<8 | (0xFF & check[++d]);
								ya= (0xFF & check[++d])<<4 | (0xF0 & check[++d])>>>4;
								ye= (0xF & check[d])<<8 | (0xFF & check[++d]);

								pic_streams.add("up.left x" + xa + ",y" + ya + " @ size " + (xe - xa + 1) + "*" + (ye - ya + 1));
							}
							break;
						}

						byte packet[] = new byte[10 + supframe_size];
						System.arraycopy(check, i, packet, 0, 10 + supframe_size);

					//  shows first found subpicture scaled on OSD
						Common.getSubpictureClass().decode_picture(packet, 10, true, new String[2]);
					}

					return CommonParsing.ES_SUP_TYPE;
				}
			}

			mpegtscheck:
			for (int i = 0; i < bs4; i++) 
			{ 
				if ( check[i] != 0x47 || check[i + 188] != 0x47 || check[i + 376] != 0x47 || check[i + 564] != 0x47 || check[i + 752] != 0x47) 
					continue mpegtscheck;

				PMTcheck(check, i);

				return CommonParsing.TS_TYPE;
			}

			pvacheck:
			for (int i = 0; i < bs1; i++)
			{
				if ( check[i] != 0x41 || check[i + 1] != 0x56 || check[i + 4] != 0x55 ) 
					continue pvacheck;

				int next = i + 8 + ((0xFF & check[i + 6])<<8 | (0xFF & check[i + 7]));

				if ( check[next] != 0x41 || check[next + 1] != 0x56 || check[next + 4] != 0x55 ) 
					continue pvacheck;

				else
				{
					if (more) 
						loadPVA(check, i);

					return CommonParsing.PVA_TYPE;
				}
			}

			mpgcheck:
			for (int i = 0, j, flag, returncode; i < bs2; i++)
			{
				if ((returncode = CommonParsing.validateStartcode(check, i)) < 0 || CommonParsing.getPES_IdField(check, i) != CommonParsing.PACK_START_CODE)
				{
					i += (returncode < 0 ? -returncode : 4) - 1;
					continue mpgcheck;
				}

				flag = 0xC0 & check[i + 4];

				if (flag == 0)
				{ 
					j = i + 12;

					if (CommonParsing.validateStartcode(check, j) < 0 || CommonParsing.getPES_IdField(check, j) < CommonParsing.SEQUENCE_HEADER_CODE)
						continue mpgcheck;

					if (more) 
						loadMPG2(check, i, false, true, nullpacket, bs1); 

					return CommonParsing.MPEG1PS_TYPE;
				}

				else if (flag == 0x40 )
				{
					j = i + 14 + (7 & check[i + 13]);

					if (CommonParsing.validateStartcode(check, j) < 0 || CommonParsing.getPES_IdField(check, j) < CommonParsing.SEQUENCE_HEADER_CODE)
						continue mpgcheck;

					if (more) 
						loadMPG2(check, i, Common.getSettings().getBooleanProperty(Keys.KEY_simpleMPG), false, nullpacket, bs1); 

					return CommonParsing.MPEG2PS_TYPE;
				}
			}

			vdrcheck:
			for (int i = 0, returncode; i < bs4; i++)
			{
				if ((returncode = CommonParsing.validateStartcode(check, i)) < 0 || (0xF0 & CommonParsing.getPES_IdField(check, i)) != 0xE0)
				{
					i += (returncode < 0 ? -returncode : 4) - 1;
					continue vdrcheck;
				}

				int next = i + 6 + CommonParsing.getPES_LengthField(check, i);

				if ( (next == i + 6) && (0xC0 & check[i + 6]) == 0x80 && (0xC0 & check[i + 8]) == 0)
				{ 
					addInfo = " !!(VPacketLengthField is 0)"; 
					next = i; 
					nullpacket = true; 
				}

				if (CommonParsing.validateStartcode(check, next) < 0)
					continue vdrcheck;

				else
				{
					if (more) 
						loadMPG2(check, i, !Common.getSettings().getBooleanProperty(Keys.KEY_enhancedPES), false, nullpacket, bs3); 

					return CommonParsing.PES_AV_TYPE;
				}
			}

			rawcheck:
			for (int i = 0, returncode; i < bs2; i++)
			{
				if ((returncode = CommonParsing.validateStartcode(check, i)) < 0)
				{
					i += (-returncode) - 1;
					continue rawcheck;
				}

				if ( (0xE0 & check[i + 3]) == 0xC0 || (0xFF & check[i + 3]) == 0xBD )
				{
					int next = i + 6 + CommonParsing.getPES_LengthField(check, i);

					if (CommonParsing.validateStartcode(check, next) < 0)
						continue rawcheck;

					if ( (0xE0 & check[i + 3]) == 0xC0 && (0xE0 & check[i + 3]) == (0xE0 & check[next + 3]) )
					{
						if (more)
							loadMPG2(check, i, true, false, nullpacket, bs3); 

						return CommonParsing.PES_MPA_TYPE;
					}

					else if ( (0xFF & check[i + 3]) == 0xBD && check[i + 3] == check[next + 3] )
					{
						if (more)
						{
							if (check[i + 8] == 0x24 && (0xF0 & check[i + 9 + 0x24])>>>4 == 1)
							{
								addInfo = " (TTX)";
								ttx_streams.add("SubID 0x" + Integer.toHexString((0xFF & check[i + 9 + 0x24])).toUpperCase());
							}
							else
								loadMPG2(check, i, true, false, nullpacket, bs3);
						}

						return CommonParsing.PES_PS1_TYPE;
					}
				}
			}

			//ES audio
			audiocheck:
			for (int i = 0; i < bs0; i++)
			{
				/* DTS stuff taken from the VideoLAN project. */ 
				/* Added by R One, 2003/12/18. */ 
				if (Audio.DTS_parseHeader(check, i) > 0)
				{ 
					for (int b = 0; b < 15; b++)
					{ 
						if (Audio.DTS_parseNextHeader(check, i + Audio.Size + b) == 1)
						{ 
							if ( (0xFF & check[i + Audio.Size]) > 0x7F || (0xFF & check[i + Audio.Size]) == 0 ) //smpte 
								continue audiocheck; 

							if (more)
							{ 
								audio_streams.add(Audio.DTS_saveAnddisplayHeader());
								playtime = getAudioTime(size); 
							} 

							if (b == 0) 
								return CommonParsing.ES_DTS_TYPE; 

							else 
								return CommonParsing.ES_DTS_A_TYPE;
						} 
					} 

					if (Common.getSettings().getBooleanProperty(Keys.KEY_AudioPanel_allowSpaces))
					{
						if (more) 
							audio_streams.add(Audio.DTS_saveAnddisplayHeader());

						playtime = getAudioTime(size); 

						return CommonParsing.ES_DTS_TYPE;
					}
				}

				else if (Audio.AC3_parseHeader(check, i) > 0)
				{ 
					for (int b = 0; b < 17; b++)
					{
						if (Audio.AC3_parseNextHeader(check, i + Audio.Size + b) == 1)
						{
							if ( (0xFF & check[i + Audio.Size]) > 0x3F || (0xFF & check[i + Audio.Size]) == 0 ) //smpte
								continue audiocheck;

							if (more)
							{
								audio_streams.add(Audio.AC3_saveAnddisplayHeader());
								playtime = getAudioTime(size);
							}

							if (b == 0)
								return CommonParsing.ES_AC3_TYPE;

							else 
								return CommonParsing.ES_AC3_A_TYPE;
						}
					}

					if (Common.getSettings().getBooleanProperty(Keys.KEY_AudioPanel_allowSpaces))
					{
						if (more) 
							audio_streams.add(Audio.AC3_saveAnddisplayHeader());

						playtime = getAudioTime(size); 

						return CommonParsing.ES_AC3_TYPE;
					}
				}

				else if (Audio.MPA_parseHeader(check, i) > 0)
				{
					if (!Common.getSettings().getBooleanProperty(Keys.KEY_AudioPanel_allowSpaces) && Audio.MPA_parseNextHeader(check, i + Audio.Size) < 0) 
						continue audiocheck;

					if (more)
					{
						audio_streams.add(Audio.MPA_saveAnddisplayHeader());
						playtime = getAudioTime(size);
					}

					return CommonParsing.ES_MPA_TYPE;
				}
			}


			mpvcheck:
			if (checkVid(check, bs3))
				return CommonParsing.ES_MPV_TYPE;


		} catch (Exception e) {

			playtime = msg_8;
			Common.setExceptionMessage(e);
		}

		check = null;
		//System.gc();

		return CommonParsing.Unsupported;
	}


	/**
	 *
	 */
	private class ScanObject {

		private ByteArrayOutputStream buf = null;
		private int id;
		private int type;

		private ScanObject()
		{
			buf = new ByteArrayOutputStream();
			id = 0;
		}

		private ScanObject(int val1)
		{
			buf = new ByteArrayOutputStream();
			id = val1;
		}

		private int getType()
		{
			return type;
		}

		private void write(byte data[]) throws IOException
		{
			buf.write(data);
		}

		private void write(byte data[], int offset, int length) throws IOException
		{
			buf.write(data, offset, length);
		}

		private byte[] getData() throws IOException
		{
			buf.flush();
			return buf.toByteArray();
		}

		private void reset()
		{
			buf.reset();
		}
	}

}
