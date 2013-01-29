/*
 * @(#)SCAN.java - pre-scanning to check supported files
 *
 * Copyright (c) 2002-2013 by dvb.matt, All Rights Reserved. 
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;

import net.sourceforge.dvb.projectx.audio.AudioFormat;
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
	private ArrayList unknown_streams;

	private AudioFormat Audio = new AudioFormat();

	/**
	 *
	 */
	public Scan()
	{
		video_streams = new ArrayList();
		audio_streams = new ArrayList();
		ttx_streams = new ArrayList();
		pic_streams = new ArrayList();
		unknown_streams = new ArrayList();
		pidlist = new ArrayList();
	}

	/**
	 * performs a pre-scan of XInputFile and saves results in StreamInfo of XInputFile
 	 */
	public void getStreamInfo(XInputFile aXInputFile)
	{
		getStreamInfo(aXInputFile, 0, -1);
	}

	/**
	 * performs a pre-scan of XInputFile and saves results in StreamInfo of XInputFile
 	 */
	public void getStreamInfo(XInputFile aXInputFile, long position)
	{
		getStreamInfo(aXInputFile, position, -1);
	}

	/**
	 * performs a pre-scan of XInputFile and saves results in StreamInfo of XInputFile
	 * forces a streamtype assignment
 	 */
	public void getStreamInfo(XInputFile aXInputFile, long position, int assigned_streamtype)
	{
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
			streamInfo.setStreamInfo(aXInputFile.getFileType().getName(), getType(aXInputFile, position, assigned_streamtype), _name, _location, _date, _size, getPlaytime(), getVideo(), getAudio(), getText(), getPics());

			streamInfo.setStreamType(filetype, addInfo);
			streamInfo.setPIDs(getPIDs());
			streamInfo.setVideoHeader(getVBasic());
		}
		else
			streamInfo = new StreamInfo("none", Resource.getString("ScanInfo.NotFound"), _name, _location, "", "", "");

		aXInputFile.setStreamInfo(streamInfo);
	}

	/**
	 * saves video basic info, for patch
	 */
	private byte[] getVBasic()
	{ 
		if (hasVideo)
			return vbasic; 

		return null;
	}

	/**
	 *
	 */
	private String getType(XInputFile aXInputFile, long position, int assigned_streamtype)
	{ 
		if (position > 0)
			filetype = testFile(aXInputFile, true, position, assigned_streamtype);

		else
			filetype = testFile(aXInputFile, true, assigned_streamtype);

		return (Keys.ITEMS_FileTypes[getStreamType()].toString() + " [" + getStreamSubType() + "] " + addInfo);
	}

	/**
	 *
	 */
	private int getStreamType()
	{ 
		return (0xFF & filetype); 
	}

	/**
	 *
	 */
	private int getStreamSubType()
	{ 
		return (0xFF & filetype>>>8); 
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
		return Common.formatTime_1((len * 8000) / Audio.getBitrate());
	}

	/**
	 *
	 */
	private int testFile(XInputFile aXInputFile, boolean more, int assigned_streamtype)
	{
		long len = aXInputFile.length();

		int ret = testFile(aXInputFile, more, 0, assigned_streamtype);

		if (ret != 0)
			return ret;

		// if type is not yet detected, try it again on a later position (10% of length)
		return testFile(aXInputFile, more, len / 10, assigned_streamtype);
	}

	/**
	 *
	 */
	private int testFile(XInputFile aXInputFile, boolean more, long position, int assigned_streamtype)
	{
		video_streams.clear();
		audio_streams.clear();
		ttx_streams.clear();
		pic_streams.clear();
		unknown_streams.clear();
		pidlist.clear();

		addInfo = "";
		playtime = "";
		hasVideo = false; 

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

			XInputFile aXif = aXInputFile.getNewInstance();

			if (aXif == null)
			{
				check = null;
				return CommonParsing.Unsupported;
			}

			aXif.randomAccessSingleRead(check, position);

			int returncode = -1;
		//	int[] mapping = { -1, -1, 5, 4, 4, 3, 2, 6, 6, 10, 9, 8, 8, 7, 7, 0, 0, 1 }; //1+16
			int[] mapping = { -1, -1, 5, 4, 4, 3, 2, 6, 6, 10, 9, 8, 8, 7, 7, 0, 0, 11 }; //1+16
			int streamtype = mapping[assigned_streamtype + 1];

			scanloop:
			for (int index = streamtype; returncode < CommonParsing.Unsupported; index++)
			{

				returncode = scanPjxOwn(aXif, check, bs0);

				if (returncode != -1)
					break;

				switch (index)
				{
				case -1:
					break;

				case 0:
					returncode = scanRiffAudio(check, bs0, more, size);
					break;

				case 1: //empty
			//		returncode = scanSubpicture(check, bs0, more, aXif.toString());
					break;

				case 2:
					returncode = scanTS(check, bs4, more);
					break;

				case 3:
					returncode = scanPVA(check, bs1, more);
					break;

				case 4:
					returncode = scanMpg12(check, bs2, bs1, more);
					break;

				case 5:
					returncode = scanPrimaryPES(check, bs4, bs3, more);
					break;

				case 6:
					returncode = scanSecondaryPES(check, bs2, bs3, more);
					break;

				case 7:
					returncode = scanDtsAudio(check, bs1, more, size);
					break;

				case 8:
					returncode = scanAc3Audio(check, bs1, more, size);
					break;

				case 9:
					returncode = scanMpgAudio(check, bs1, more, size);
					break;

				case 10:
					returncode = scanMpgVideo(check, bs3, more);
					break;

				case 11:
					returncode = scanSubpicture(check, bs0, more, aXif.toString());
					break;

				default:
					break scanloop;
				}

				if (assigned_streamtype != -1)
					returncode = assigned_streamtype;
			}

			check = null;

			if (returncode > 0)
				return returncode;

		} catch (Exception e) {

			playtime = msg_8;
			Common.setExceptionMessage(e);
		}

		check = null;

		return CommonParsing.Unsupported;
	}

	/**
	 *
	 */
	private int scanPjxOwn(XInputFile xInputFile, byte[] check, int buffersize) throws Exception
	{
		for (int i = 0; i < 16; i++)
		{
			if (check[i] != CommonParsing.PTSVideoHeader[i])
				return -1;
		}

		int length = (int) xInputFile.length();
		byte[] data = new byte[length];

		xInputFile.randomAccessSingleRead(data, 0);

		long pts1 = CommonParsing.readPTS(data, 16, 8, false, false);
		long pts2 = CommonParsing.readPTS(data, length - 24, 8, false, false);
		long pts3 = CommonParsing.readPTS(data, 32, 8, false, false);
		long pts4 = CommonParsing.readPTS(data, length - 8, 8, false, false);

		playtime = "Src  " + Common.formatTime_1(pts1 / 90L) + " -- " + Common.formatTime_1(pts2 / 90L);
		playtime += " /  Out  " + Common.formatTime_1(pts3 / 90L) + " -- " + Common.formatTime_1(pts4 / 90L);

		return CommonParsing.PJX_PTS_TYPE;
	}

	/**
	 *
	 */
	private int scanRiffAudio(byte[] check, int buffersize, boolean more, long size) throws Exception
	{
		Audio.setNewType(CommonParsing.WAV_AUDIO);

		riffcheck:
		for (int i = 0, ERRORCODE; i < buffersize; i++)  //compressed as AC3,MPEG is currently better detected as ES-not RIFF
		{
			ERRORCODE = Audio.parseHeader(check, i);

			if (ERRORCODE > -1)
			{
				Audio.saveHeader();

				if (more)
				{ 
					audio_streams.add(Audio.displayHeader());
					playtime = getAudioTime(size); 
				} 

				if (ERRORCODE > 0)
					return CommonParsing.ES_RIFF_TYPE;

				else if (Audio.getLastModeExtension() > 1)
					break riffcheck;

				else
					return CommonParsing.ES_cRIFF_TYPE;
			}
		}

		return -1;
	}

	/**
	 *
	 */
	private int scanSubpicture(byte[] check, int buffersize, boolean more, String supname) throws Exception
	{
		supcheck:
		for (int i = 0, supframe_size, supframe_link, supframe_check, supframe_check2; i < buffersize; i++) 
		{
			if (check[i] != 0x53 || check[i + 1] != 0x50)
				continue supcheck;

			supframe_size = (0xFF & check[i + 10])<<8 | (0xFF & check[i + 11]);
			supframe_link = (0xFF & check[i + 12])<<8 | (0xFF & check[i + 13]);
			supframe_check = (0xFF & check[i + 12 + supframe_link])<<8 | (0xFF & check[i + 13 + supframe_link]);
		//	supframe_check2 = (0xFF & check[i + 36 + supframe_link])<<8 | (0xFF & check[i + 37 + supframe_link]);

		//	if (supframe_link == supframe_check - 24 && supframe_check == supframe_check2) // 24 is wrong
			if ((0xFF & check[i + supframe_size + 9]) == 0xFF) // end stuffing, at least one
			{
				if (more)
				{
				//	int b = i + 14 + supframe_link, c = b + 24, d, xa, xe, ya, ye; // 24 is wrong
					int b = i + 14 + supframe_link, c = b + 24, d, xa, xe, ya, ye;

					for (d = b; d < c; d++)
					{
						switch(0xFF & check[d])
						{
						case 1:
							d++;
							continue;

						case 2:
							d += 24; // is wrong
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
							continue;
						}
						break;
					}

					byte packet[] = new byte[10 + supframe_size];

					System.arraycopy(check, i, packet, 0, 10 + supframe_size);

				//  shows first found subpicture scaled on OSD
					Common.getSubpictureClass().setColorTable(setIFOColorTable(supname));
					Common.getSubpictureClass().decode_picture(packet, 10, true, new String[2]);
				}

				return CommonParsing.ES_SUP_TYPE;
			}
		}

		return -1;
	}

	/**
	 * read colors from ifo (pjx auto generated)
	 */
	private int[] setIFOColorTable(String supname)
	{
		try {

			String nsupname = supname + ".IFO";

			File f = new File(nsupname);

			if (!f.exists())
			{
				f = new File(supname.substring(0, supname.lastIndexOf(".")) + ".IFO");

				if (!f.exists())
					return null;
				//Common.setMessage("!> no existing .ifo file : '" + supname + "'");
			}

			XInputFile xif = new XInputFile(f);

			byte[] data = new byte[64];
			int[] values = new int[16];

			xif.randomAccessSingleRead(data, 0x10B4); //read 16x 4bytes from pos 0x10B4

			for (int i = 0, j = values.length; i < j; i++)
				values[i] = YUVtoRGB(CommonParsing.getIntValue(data, i * 4, 4, !CommonParsing.BYTEREORDERING));

			return values;

		} catch (Exception e) {

			Common.setExceptionMessage(e);
		}

		return null;
	}

	/**
	 * convert colors from ifo (pjx auto generated)
	 */
	private int YUVtoRGB(int values)
	{
		int Y = 0xFF & values>>16;
		int Cr = 0xFF & values>>8;
		int Cb = 0xFF & values;

		if (Y == 0)
			return 0;

		int R = (int)((float)Y +1.402f * (Cr-128));
		int G = (int)((float)Y -0.34414 * (Cb-128) -0.71414 * (Cr-128));
		int B = (int)((float)Y +1.722 * (Cb-128));
		R = R < 0 ? 0 : (R > 0xFF ? 0xFF : R);
		G = G < 0 ? 0 : (G > 0xFF ? 0xFF : G);
		B = B < 0 ? 0 : (B > 0xFF ? 0xFF : B);
		int T = 0xFF;

		return (T<<24 | R<<16 | G<<8 | B);
	}

	/**
	 * scan TS
	 */
	private int scanTS(byte[] check, int buffersize, boolean more) throws Exception
	{
		// 188bytes TS
		for (int i = 0, j = 188; i < buffersize; i++) 
		{ 
			if ( check[i] != 0x47 || check[i + j] != 0x47 || check[i + j * 2] != 0x47 || check[i + j * 3] != 0x47 || check[i + j * 4] != 0x47 || check[i + j * 5] != 0x47 || check[i + j * 6] != 0x47) 
				continue;

			readPMT(check, i, 0);

			if (pidlist.isEmpty())
				scanTSPids(check, i, buffersize, 0, more);

			return scanTSSubtype(check, buffersize);
		}

		// 192bytes TS
		for (int i = 0, j = 192; i < buffersize; i++) 
		{ 
			if ( check[i] != 0x47 || check[i + j] != 0x47 || check[i + j * 2] != 0x47 || check[i + j * 3] != 0x47 || check[i + j * 4] != 0x47 || check[i + j * 5] != 0x47 || check[i + j * 6] != 0x47) 
				continue;

			readPMT(check, i, 4);

			if (pidlist.isEmpty())
				scanTSPids(check, i, buffersize, 4, more);

			return CommonParsing.TS_TYPE_192BYTE;
		}

		return -1;
	}

	/**
	 * scan TS sub type
	 */
	private int scanTSSubtype(byte[] check, int buffersize)
	{
		//TFrc TF5x00
		if (check[0] == 0x54 && check[1] == 0x46 && check[2] == 0x72 && check[3] == 0x63 && check[4] == 0x50 && check[5] == 0)
		{
			/**  jkit 23012009
			 * find header type (DVB-s,-t,-c), to know the
			 * start of the event info structure
			 *  
			 * validate three typical values in the transponder info
			 * for each type and take the best matching
			 */
			int isDVBsVal = 0;
			int isDVBtVal = 0;
			int isDVBcVal = 0;
			int offset = 0x34;

			// test as dvb-s transponder info
			byte Polarity = check[offset + 1];
			long Frequency =  (check[offset + 4] << 24) | (check[offset + 5] << 16)
							| (check[offset + 6] <<  8) |  check[offset + 7];
			int Symbol_Rate = (check[offset + 8] <<  8) |  check[offset + 9];
			if ((Polarity & 0x6F) == 0) 
				isDVBsVal++;
			else 
				isDVBsVal--;
			if ((Symbol_Rate >  2000) && (Symbol_Rate < 30000))  
				isDVBsVal++;
			else 
				isDVBsVal--;
			if ((Symbol_Rate  > 10000) && (Symbol_Rate  < 13000))
				isDVBsVal++;
			else 
				isDVBsVal--;

			// test as dvb-t transponder info
			byte Bandwidth = check[offset + 2];
			Frequency = (check[offset + 4] << 24) | (check[offset + 5] << 16)
					  | (check[offset + 6] <<  8) |  check[offset + 7];
			byte LP_HP_Stream = check[offset + 10];
			if ((Bandwidth >= 6) && (Bandwidth <= 8))
				isDVBtVal++;
			if (((Frequency  >= 174000) && (Frequency  <= 230000))
			 ||	((Frequency  >= 470000) && (Frequency  <= 862000)))
				isDVBtVal++;
			if ((LP_HP_Stream & 0xFE) == 0)
				isDVBtVal++;

			// test as dvb-c transponder info
			Frequency =   (check[offset + 0] << 24) | (check[offset + 1] << 16)
						| (check[offset + 2] <<  8) |  check[offset + 3];
			Symbol_Rate = (check[offset + 4] <<  8) |  check[offset + 5];
			byte Modulation = check[offset + 10];
			if ((Frequency  >= 47000) && (Frequency  <= 862000))
				isDVBcVal++;
			if ((Symbol_Rate >  2000) && (Symbol_Rate < 30000)) 
				isDVBcVal++;
			if (Modulation <= 4) 
				isDVBcVal++;

			if ((isDVBsVal >= isDVBcVal) && (isDVBsVal >= isDVBtVal)) 
			{
				// event_info_offset = 0x44; // default
				//Common.setMessage("-> topfield header has DVB-s format");
				return CommonParsing.TS_TYPE_TF5X00;
			}
			else if (isDVBtVal > isDVBcVal)
			{
				//event_info_offset = 0x44; // default
				//Common.setMessage("-> topfield header has DVB-t format");
				return CommonParsing.TS_TYPE_TF5X00;
			}
			else
			{
				/**
				 * the transponder info structure of the TF5200 is shorter,
				 * so the following data starts at 0x40
				 */
				//event_info_offset = 0x40;
				//Common.setMessage("-> topfield header has DVB-c format");
				return CommonParsing.TS_TYPE_TF5X00C;
			}
		}

		//TF4000
		else if (check[0] != 0x47 && check[188] != 0x47 && check[376] != 0x47 && check[564] == 0x47 && check[752] == 0x47 && check[940] == 0x47)
			return CommonParsing.TS_TYPE_TF4000;

		//Handan/Hojin
		for (int i = 0, j = buffersize - 10; i < j; i++)
		{
			if (check[i] != 0x5B || check[i + 1] != 0x48 || check[i + 2] != 0x4F || check[i + 3] != 0x4A || check[i + 4] != 0x49 || check[i + 5] != 0x4E || check[i + 6] != 0x20 || check[i + 7] != 0x41)
				continue;

			return CommonParsing.TS_TYPE_HANDAN;
		}

		//comag
		if (check[0] == 0 && check[1] == 0 && check[2] == 1 && check[3] == (byte)0xBA && check[14] == 0 && check[15] == 0 && check[16] == 1 && check[17] == (byte)0xBF)
			return CommonParsing.TS_TYPE_COMAG;


		return CommonParsing.TS_TYPE;
	}

	/**
	 *
	 */
	private void scanTSPids(byte[] check, int i, int buffersize, int lead, boolean more) throws Exception
	{
		String str;

		mpegtscheck:
		for (int pid, scrambling, j = 188 + lead; i < buffersize; i++) 
		{ 
			if ( check[i] != 0x47 || check[i + j] != 0x47 || check[i + j * 2] != 0x47) 
				continue mpegtscheck;

			pid = (0x1F & check[i + 1])<<8 | (0xFF & check[i + 2]);
			scrambling = (0xC0 & check[i + 3])>>>6; // scrambling

			i += j - 1;

			str = "PID 0x" + Integer.toHexString(pid).toUpperCase();

			if (scrambling > 0)
				str += "-($)";

			if (!video_streams.contains(str))
				video_streams.add(str);
		}

		addInfo += " (no PMT found)";
	}

	/**
	 *
	 */
	private int scanPVA(byte[] check, int buffersize, boolean more) throws Exception
	{
		pvacheck:
		for (int i = 0; i < buffersize; i++)
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

		return -1;
	}

	/**
	 *
	 */
	private int scanMpg12(byte[] check, int buffersize_1, int buffersize_2, boolean more) throws Exception
	{
		boolean nullpacket = false;

		mpgcheck:
		for (int i = 0, j, flag, returncode; i < buffersize_1; i++)
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
					loadMPG2(check, i, false, true, nullpacket, buffersize_2); 

				return CommonParsing.MPEG1PS_TYPE;
			}

			else if (flag == 0x40 )
			{
				j = i + 14 + (7 & check[i + 13]);

				if (CommonParsing.validateStartcode(check, j) < 0 || CommonParsing.getPES_IdField(check, j) < CommonParsing.SEQUENCE_HEADER_CODE)
					continue mpgcheck;

				if (more) 
					loadMPG2(check, i, Common.getSettings().getBooleanProperty(Keys.KEY_simpleMPG), false, nullpacket, buffersize_2); 

				return CommonParsing.MPEG2PS_TYPE;
			}
		}

		return -1;
	}

	/**
	 *
	 */
	private int scanPrimaryPES(byte[] check, int buffersize_1, int buffersize_2, boolean more) throws Exception
	{
		boolean nullpacket = false;

		vdrcheck:
		for (int i = 0, returncode; i < buffersize_1; i++)
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
					loadMPG2(check, i, !Common.getSettings().getBooleanProperty(Keys.KEY_enhancedPES), false, nullpacket, buffersize_2); 

				return CommonParsing.PES_AV_TYPE;
			}
		}

		return -1;
	}

	/**
	 *
	 */
	private int scanSecondaryPES(byte[] check, int buffersize_1, int buffersize_2, boolean more) throws Exception
	{
		boolean nullpacket = false;

		rawcheck:
		for (int i = 0, returncode; i < buffersize_1; i++)
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
						loadMPG2(check, i, true, false, nullpacket, buffersize_2); 

					return CommonParsing.PES_MPA_TYPE;
				}

				else if ( (0xFF & check[i + 3]) == 0xBD && ((0xFF & check[next + 3]) == 0xBD || (0xFF & check[next + 3]) == 0xBE))
				{
					if (more)
					{
						if (check[i + 8] == 0x24 && (0xF0 & check[i + 9 + 0x24])>>>4 == 1)
						{
							addInfo = " (TTX)";
							ttx_streams.add("SubID 0x" + Integer.toHexString((0xFF & check[i + 9 + 0x24])).toUpperCase());
						}

						else
							loadMPG2(check, i, true, false, nullpacket, buffersize_2);
					}

					return CommonParsing.PES_PS1_TYPE;
				}
			}
		}

		return -1;
	}

	/**
	 *
	 */
	private int scanDtsAudio(byte[] check, int buffersize, boolean more, long size) throws Exception
	{
		Audio.setNewType(CommonParsing.DTS_AUDIO);

		audiocheck:
		for (int i = 0; i < buffersize; i++)
		{
			/* DTS stuff taken from the VideoLAN project. */ 
			/* Added by R One, 2003/12/18. */ 
			if (Audio.parseHeader(check, i) < 1)
				continue;

			for (int b = 0; b < 15; b++)
			{ 
				if (Audio.parseNextHeader(check, i + Audio.getSize() + b) != 1)
					continue;

				if ( (0xFF & check[i + Audio.getSize()]) > 0x7F || (0xFF & check[i + Audio.getSize()]) == 0 ) //smpte 
					continue audiocheck; 

				if (more)
				{ 
					audio_streams.add(Audio.saveAndDisplayHeader());
					playtime = getAudioTime(size); 
				} 

				if (b == 0) 
					return CommonParsing.ES_DTS_TYPE; 

				else 
					return CommonParsing.ES_DTS_A_TYPE;
			} 

			if (Common.getSettings().getBooleanProperty(Keys.KEY_AudioPanel_allowSpaces))
			{
				if (more) 
					audio_streams.add(Audio.saveAndDisplayHeader());

				playtime = getAudioTime(size); 

				return CommonParsing.ES_DTS_TYPE;
			}
		}

		return -1;
	}

	/**
	 *
	 */
	private int scanAc3Audio(byte[] check, int buffersize, boolean more, long size) throws Exception
	{
		Audio.setNewType(CommonParsing.AC3_AUDIO);

		audiocheck:
		for (int i = 0, gg=0; i < buffersize; i++)
		{
			if (Audio.parseHeader(check, i) < 1)
				continue;

			for (int b = 0; b < 17; b++)
			{
				if (Audio.parseNextHeader(check, i + Audio.getSize() + b) != 1)
					continue;

				if ( (0xFF & check[i + Audio.getSize()]) > 0x3F || (0xFF & check[i + Audio.getSize()]) == 0 ) //smpte
					continue audiocheck;

				if (more)
				{
					audio_streams.add(Audio.saveAndDisplayHeader());
					playtime = getAudioTime(size);
				}

				if (b == 0)
					return CommonParsing.ES_AC3_TYPE;

				else 
					return CommonParsing.ES_AC3_A_TYPE;
			}

			if (Common.getSettings().getBooleanProperty(Keys.KEY_AudioPanel_allowSpaces))
			{
				if (more) 
					audio_streams.add(Audio.saveAndDisplayHeader());

				playtime = getAudioTime(size); 

				return CommonParsing.ES_AC3_TYPE;
			}
		}

		return -1;
	}

	/**
	 *
	 */
	private int scanMpgAudio(byte[] check, int buffersize, boolean more, long size) throws Exception
	{
		Audio.setNewType(CommonParsing.MPEG_AUDIO);

		audiocheck:
		for (int i = 0; i < buffersize; i++)
		{
			if (Audio.parseHeader(check, i) < 1)
				continue;

			if (!Common.getSettings().getBooleanProperty(Keys.KEY_AudioPanel_allowSpaces) && Audio.parseNextHeader(check, i + Audio.getSize()) < 0) 
				continue audiocheck;
//
			if (!Common.getSettings().getBooleanProperty(Keys.KEY_AudioPanel_allowSpaces) && Audio.parseNextHeader(check, i + Audio.getSize() + Audio.getNextSize()) < 0) 
				continue audiocheck;
//
			if (more)
			{
				audio_streams.add(Audio.saveAndDisplayHeader());
				playtime = getAudioTime(size);
			}

			return CommonParsing.ES_MPA_TYPE;
		}

		return -1;
	}

	/**
	 *
	 */
	private int scanMpgVideo(byte[] check, int buffersize, boolean more) throws Exception
	{
		mpvcheck:
		if (checkVid(check, buffersize))
			return CommonParsing.ES_MPV_TYPE;

		return -1;
	}

	/**
	 *
	 */
	private int AC3Audio(byte[] check)
	{
		Audio.setNewType(CommonParsing.AC3_AUDIO);

		audiocheck:
		for (int a=0; a<10000; a++)
		{
			if (Audio.parseHeader(check, a) < 0)
				continue audiocheck;

			for (int b = 0; b < 17; b++)
			{
				if (Audio.parseNextHeader(check, a + Audio.getSize() + b) == 1)
				{
					if ( (0xFF & check[a + Audio.getSize()]) > 0x3f || (0xFF & check[a + Audio.getSize()]) == 0 ) //smpte
						continue audiocheck;

					audio_streams.add(Audio.saveAndDisplayHeader());

					return 1;
				}
			}
		} 

		return 0;
	}

	/* DTS stuff taken from the VideoLAN project. */ 
	/* Added by R One, 2003/12/18. */
	private int DTSAudio(byte[] check)
	{ 
		Audio.setNewType(CommonParsing.DTS_AUDIO);

		audiocheck: 
		for (int i = 0; i < 10000; i++)
		{ 
			if (Audio.parseHeader(check, i) < 0) 
				continue audiocheck; 

			for (int j = 0; j < 15; j++)
			{ 
				if (Audio.parseNextHeader(check, i + Audio.getSize() + j) == 1)
				{ 
					if ( (0xFF & check[i + Audio.getSize()]) > 0x7F || (0xFF & check[i + Audio.getSize()]) == 0 ) //smpte 
						continue audiocheck; 

					audio_streams.add(Audio.saveAndDisplayHeader()); 

					return 1; 
				} 
			} 
		} 

		return 0;
	} 

	/**
	 *
	 */
	private int MPEGAudio(byte[] check)
	{ 
		Audio.setNewType(CommonParsing.MPEG_AUDIO);

		audiocheck:
		for (int a = 0; a < 10000; a++)
		{
			if (Audio.parseHeader(check, a) < 0)
				continue audiocheck;

			if (Audio.parseNextHeader(check, a + Audio.getSize()) < 0)
				continue audiocheck;

			audio_streams.add(Audio.saveAndDisplayHeader());

			return 1;
		}

		return 0;
	}

	/**
	 *
	 */
	private int LPCMAudio(byte[] check)
	{ 
		Audio.setNewType(CommonParsing.LPCM_AUDIO);

		audiocheck:
		for (int a = 0; a < 1000; a++)
		{
			if (Audio.parseHeader(check, a) < 0)
				continue audiocheck;

			audio_streams.add(Audio.saveAndDisplayHeader());

			return 1;
		}

		return 0;
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
			mpg1 = (0x80 & check[a + 6]) == 0;
			offs = a + 6 + ( !mpg1 ? 3 + (0xFF & check[a + 8]) : 0);
			len = jump - ( !mpg1 ? 3 + (0xFF & check[a + 8]) : 0);

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
		for (int a = b, returncode, subid; a < end; a += jump)
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
				int pes_subid = 0xFF & check[a + 9 + pes_extensionlength];

				if (pes_extensionlength == 0x24 && pes_subid>>>4 == 1)
				{
					str = "SubID 0x" + Integer.toHexString(pes_subid).toUpperCase();

					if (ttx_streams.indexOf(str) < 0)
						ttx_streams.add(str);
				}

				else if ( ((!mpg1 && !vdr) || (vdr && pes_alignment)) && (pes_subid>>>4 == 2 || pes_subid>>>4 == 3))
				{
					str = "SubID 0x" + Integer.toHexString(pes_subid).toUpperCase();

					if (pic_streams.indexOf(str) < 0)
						pic_streams.add(str);
				}

				else
				{
					if (!vdr)
					{
						id = 0xFF & check[a + 9 + pes_extensionlength];
						str = String.valueOf(id);

						check[a + 8] = (byte)((pes_subid>>>4 == 0xA ? 1 : 4) + pes_extensionlength);
					//	check[a + 8] = (byte)(4 + pes_extensionlength);
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

					if (AC3Audio(buffer) != 0) 
						return;
					
					else if (DTSAudio(buffer) != 0) 
						return;

					LPCMAudio(buffer);

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

		String[] h264_info = new String[18];
		boolean isH264 = Common.getMpvDecoderClass().parseH264(check, length, h264_info);

		if (isH264)
		{
			video_streams.add(h264_info[0] + "  " + h264_info[1] + " " + h264_info[2] + " " + h264_info[3]);
			return true;
		}

		return false;
	}

	/**
	 *
	 */
	private void readPMT(byte[] check, int a, int lead)
	{ 
		ByteArrayOutputStream bytecheck = new ByteArrayOutputStream();
	//	pidlist.clear();

		boolean ts_start = false;
		int packetlength = 188;

		tscheck:
		for (int adaptfield, pmtpid, offset; a < check.length - 1000; a++)
		{
			if ( check[a] != 0x47 || check[a + packetlength + lead] != 0x47 || check[a + (packetlength + lead) * 2] != 0x47 ) 
				continue tscheck;

			if ((0x40 & check[a + 1]) == 0) // start
				continue tscheck;

			if ((0xC0 & check[a + 3]) != 0) // scrambling
				continue tscheck;

			adaptfield = (0x30 & check[a + 3])>>>4;

			if ((adaptfield & 1) == 0) // adapt - no payload
				continue tscheck;

			offset = adaptfield == 3 ? 1 + (0xFF & check[a + 4]) : 0; //adaptlength

			if (check[a + offset + 4] != 0 || check[a + offset + 5] != 2 || (0xF0 & check[a + offset + 6]) != 0xB0)
			{ 
				a += (packetlength + lead - 1); 
				continue tscheck; 
			}

			bytecheck.write(check, a + 4 + offset, packetlength - 4 - offset);

			pmtpid = (0x1F & check[a + 1])<<8 | (0xFF & check[a + 2]);

			if ( bytecheck.size() < packetlength )
			{ 
				a += packetlength + lead;

				addpack:
				for ( ; a < check.length - 500; a++)
				{
					if ( check[a] != 0x47 || check[a + packetlength + lead] != 0x47 || check[a + (packetlength + lead) * 2] != 0x47 ) 
						continue addpack;

					if ((0x40 & check[a + 1]) != 0) // start
						continue addpack;

					if ((0xC0 & check[a + 3]) != 0) // scrambling
						continue addpack;

					adaptfield = (0x30 & check[a + 3])>>>4;

					if ((adaptfield & 1) == 0) // adapt - no payload
						continue addpack;

					offset = adaptfield == 3 ? 1 + (0xFF & check[a + 4]) : 0; //adaptlength

					if ( ((0x1F & check[a + 1])<<8 | (0xFF & check[a + 2])) != pmtpid )
					{ 
						a += (packetlength + lead - 1); 
						continue addpack; 
					}

					bytecheck.write(check, a + 4 + offset, packetlength - 4 - offset);

					if ( bytecheck.size() > packetlength ) 
						break addpack;
				}
			}

			byte[] pmt = bytecheck.toByteArray();

			if (pmt.length > 5)
			{
				int sid = (0xFF & pmt[4])<<8 | (0xFF & pmt[5]);
				pidlist.add("" + sid);
				pidlist.add("" + pmtpid);
				addInfo = " (SID 0x" + Common.adaptString(Integer.toHexString(sid).toUpperCase(), 4) + ", PMT 0x" + Common.adaptString(Integer.toHexString(pmtpid).toUpperCase(), 4) + ")";
			}

			int pmt_len = (0xF&pmt[2])<<8 | (0xFF&pmt[3]);

			pidsearch:
			for (int b=8, r=8; b < pmt_len-4 && b < pmt.length-6; b++)
			{
				r = b;

				if ( (0xe0 & pmt[b+1]) != 0xe0 && (0xe0 & pmt[b+1]) != 0) 
					continue pidsearch;

				int pid = (0x1F & pmt[b+1])<<8 | (0xFF & pmt[b+2]);

				switch(0xFF & pmt[b])
				{
				case 1:
					getDescriptor(pmt, b+5, (b += 4+ (0xFF & pmt[b+4])), pid, 1);
					pidlist.add("" + pid); 
					break; 

				case 2:
					getDescriptor(pmt, b+5, (b += 4+ (0xFF & pmt[b+4])), pid, 2);
					pidlist.add("" + pid); 
					break; 

				case 3:  //mp1a
					getDescriptor(pmt, b+5, (b += 4+ (0xFF & pmt[b+4])), pid, 3);
					pidlist.add("" + pid); 
					break; 

				case 4:  //mp2a
					getDescriptor(pmt, b+5, (b += 4+ (0xFF & pmt[b+4])), pid, 4);
					pidlist.add("" + pid); 
					break; 

				case 0x11:  //mp4a
					getDescriptor(pmt, b+5, (b += 4+ (0xFF & pmt[b+4])), pid, 0x11);
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
				case 0xA0: //user private
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
		String str2 = "";
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
							str2 += (char)(0xFF & check[b]);

						if (!str2.equals(""))
						{
							str += "{" + str2 + "}";
							str2 = "";
						}

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
							str2 += (char)(0xFF & check[b]);

						if (!str2.equals(""))
						{
							str += "{" + str2 + "}";
							str2 = "";
						}

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
					str += "{";

					for (int a=off+2; a<off+5; a++)
						if ((0xFF & check[a]) > 0)
							str += (char)(0xFF & check[a]);

					str += "}";
					off++;
					off += (0xFF & check[off]);
					break;

				case 0x6A:  //ac3 descriptor
					str += "(AC-3)";
					off++;
					off += (0xFF & check[off]);
					break;

				case 0x7C:  //aac descriptor
					str += "(AAC)";
					off++;
					off += (0xFF & check[off]);
					break;

				case 0xA0:  //user priv descriptor
					str += "(FOURCC=";

					for (int a=off+2; a<off+6; a++)
						if ((0xFF & check[a]) > 0)
							str += (char)(0xFF & check[a]);

					str += ")";
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
				//	break;//??

				default:
					off++;
					off += (0xFF & check[off]);
				}
			}

			String out = "PID: 0x" + Common.adaptString(Integer.toHexString(pid).toUpperCase(), 4);

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

			case 1:
				video_streams.add(out + str + "(MPEG-1)");
				break;

			case 2:
				video_streams.add(out + str + "(MPEG-2)");
				break;

			case 0xC3:
				video_streams.add(out + str);
				break;

			case 3:
				audio_streams.add(out + str + "(Mpg1)");
				break;

			case 4:
				audio_streams.add(out + str + "(Mpg2)");
				break;

			case 0x11:
				audio_streams.add(out + str + "(Mpg4)");
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
