/*
 * @(#)CommonParsing
 *
 * Copyright (c) 2005-2009 by dvb.matt, All Rights Reserved.
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

import java.io.IOException;
import java.io.File;
import java.io.RandomAccessFile;
import java.io.PrintWriter;
import java.io.FileOutputStream;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.ArrayList;
import java.util.List;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import net.sourceforge.dvb.projectx.common.Resource;
import net.sourceforge.dvb.projectx.common.Settings;
import net.sourceforge.dvb.projectx.common.Common;
import net.sourceforge.dvb.projectx.common.Keys;
import net.sourceforge.dvb.projectx.common.JobProcessing;


/**
 * common stuff for parsing functions
 */
public class CommonParsing extends Object {

	public final static boolean BYTEREORDERING = true;

	public final static int SECONDARY_PES_PARSER = 0;
	public final static int PRIMARY_PES_PARSER   = 1;
	public final static int TS_PARSER            = 2;
	public final static int PVA_PARSER           = 3;
	public final static int ES_VIDEO_PARSER      = 4;
	public final static int ES_AUDIO_PARSER      = 5;
	public final static int ES_SUBPICTURE_PARSER = 6;

	//max 31, more defined by a subtype
	public final static int Unsupported   = 0;	//	"unsupported"
	public final static int PES_AV_TYPE   = 1;	//	"PES (Video/Audio/TTX)"
	public final static int MPEG1PS_TYPE  = 2;	//	"MPEG-1 PS/SS (Video/Audio PES)"
	public final static int MPEG2PS_TYPE  = 3;	//	"MPEG-2 PS/SS (Video/Audio PES)"
	public final static int PVA_TYPE      = 4;	//	"PVA (Video/Audio PES)"
	public final static int TS_TYPE       = 5;	//	"DVB/MPEG2 TS"
	public final static int PES_MPA_TYPE  = 6;	//	"PES (MPEG Audio)"
	public final static int PES_PS1_TYPE  = 7;	//	"PES (private stream 1)"
	public final static int ES_MPV_TYPE   = 8;	//	"ES (MPEG Video)"
	public final static int ES_MPA_TYPE   = 9;	//	"ES (MPEG Audio)"
	public final static int ES_AC3_TYPE   = 10;	//	"ES (AC-3 Audio)"
	public final static int ES_AC3_A_TYPE = 11;	//	"ES (AC-3 Audio) (psb. SMPTE)"
	public final static int ES_DTS_TYPE   = 12;	//	"ES (DTS Audio)"
	public final static int ES_DTS_A_TYPE = 13;	//	"ES (DTS Audio) (psb. SMPTE)"
	public final static int ES_RIFF_TYPE  = 14;	//	"ES (RIFF Audio)"
	public final static int ES_cRIFF_TYPE = 15;	//	"ES (compressed RIFF Audio)"
	public final static int ES_SUP_TYPE   = 16;	//	"ES (Subpicture 2-bit RLE)"
	public final static int PJX_PTS_TYPE  = 31;	//	"PjX PTS File"

	public final static int ES_TYPE   = 1;	//	"ElementaryStream"

	public final static int TS_TYPE_TF5X00    = TS_TYPE | 1<<8;	//	"DVB/MPEG2 TS  TF5X00"
	public final static int TS_TYPE_TF5X00C   = TS_TYPE | 2<<8;	//	"DVB/MPEG2 TS  TF5X00C"
	public final static int TS_TYPE_TF4000    = TS_TYPE | 3<<8;	//	"DVB/MPEG2 TS  TF4000"
	public final static int TS_TYPE_HANDAN    = TS_TYPE | 4<<8;	//	"DVB/MPEG2 TS  HANDAN"
	public final static int TS_TYPE_192BYTE   = TS_TYPE | 5<<8;	//	"DVB/MPEG2 TS  192b"
	public final static int TS_TYPE_COMAG     = TS_TYPE | 6<<8;	//	"DVB/MPEG2 TS  COMAG"


	//	"ElementaryStream"
	public final static int UNKNOWN   = -1;
	public final static int AC3_AUDIO  = 0;
	public final static int TELETEXT   = 1;
	public final static int MPEG_AUDIO = 2;
	public final static int MPEG_VIDEO = 3;
	public final static int LPCM_AUDIO = 4;
	public final static int SUBPICTURE = 5;
	public final static int DTS_AUDIO  = 6;	//handled with in AC3_AUDIO so far
	public final static int WAV_AUDIO  = 7;	
	public final static int AAC_AUDIO  = 8;	

	public final static int ACTION_UNDEFINED = -1;
	public final static int ACTION_DEMUX  = 0;	
	public final static int ACTION_TO_VDR = 1;	
	public final static int ACTION_TO_M2P = 2;	
	public final static int ACTION_TO_PVA = 3;	
	public final static int ACTION_TO_TS  = 4;	
	public final static int ACTION_FILTER = 5;	
	public final static int ACTION_COPY   = 6;	

	public final static int CUTMODE_BYTE  = 0;	
	public final static int CUTMODE_GOP   = 1;	
	public final static int CUTMODE_FRAME = 2;	
	public final static int CUTMODE_PTS   = 3;	
	public final static int CUTMODE_TIME  = 4;	

	public final static int PICTURE_START_CODE     =    0;
	public final static int SLICE_START_CODE_MIN   =    1;
	public final static int SLICE_START_CODE_MAX   = 0xAF;
	public final static int USER_DATA_START_CODE   = 0xB2;
	public final static int SEQUENCE_HEADER_CODE   = 0xB3;
	public final static int EXTENSION_START_CODE   = 0xB5;
	public final static int SEQUENCE_END_CODE      = 0xB7;
	public final static int GROUP_START_CODE       = 0xB8;
	public final static int SYSTEM_END_CODE        = 0xB9;
	public final static int PACK_START_CODE        = 0xBA;
	public final static int SYSTEM_START_CODE      = 0xBB;
	public final static int PROGRAM_STREAM_MAP_CODE= 0xBC;
	public final static int PRIVATE_STREAM_1_CODE  = 0xBD;
	public final static int PADDING_STREAM_CODE    = 0xBE;
	public final static int PRIVATE_STREAM_2_CODE  = 0xBF;
	public final static int ECM_STREAM_CODE        = 0xF0;
	public final static int EMM_STREAM_CODE        = 0xF1;
	public final static int DSM_CC_STREAM_CODE     = 0xF2;

	public final static int FRAME_I_TYPE = 1;
	public final static int FRAME_P_TYPE = 2;
	public final static int FRAME_B_TYPE = 3;
	public final static int FRAME_D_TYPE = 4;

	public final static int MAX_BITRATE_VALUE = 262143; //3FFFF *400 = 104857200 bps
	public final static int MAX_SD_BITRATE_VALUE = 37500; // *400 = 15000000 bps

	public static byte[] PTSVideoHeader = { 0x50, 0x4A, 0x58, 0x5F, 0x50, 0x54, 0x53, 0x56, 0x49, 0x44, 0x31, 0x30, 0x30, 0, 0, 0 }; //'PJX_PTSVID100'

	private static int Pva_PidToExtract = -1;

	private static boolean Pva_PidExtraction = false;

	private static boolean InfoScan = false;
	private static boolean qbreak = false;
	private static boolean qpause = false;

	private static boolean _bool = false;

	private static int _cutcount = 0;

	private static double _videoframerate = 3600.0;

	private static long AudioProcessingFlags = 0;

	private CommonParsing()
	{}

	/**
	 *
	 */
	public static boolean isInfoScan()
	{
		return InfoScan;
	}

	/**
	 *
	 */
	public static void setInfoScan(boolean b)
	{
		InfoScan = b;
	}

	/**
	 *
	 */
	public static boolean isProcessCancelled()
	{
		return qbreak;
	}

	/**
	 *
	 */
	public static void setProcessCancelled(boolean b)
	{
		qbreak = b;
	}

	/**
	 *
	 */
	public static boolean isProcessPausing()
	{
		return qpause;
	}

	/**
	 *
	 */
	public static void setProcessPausing(boolean b)
	{
		qpause = b;
	}

	/**
	 *
	 */
	public static long getAudioProcessingFlags()
	{
		return AudioProcessingFlags;
	}

	/**
	 *
	 */
	public static void setAudioProcessingFlags(long val)
	{
		AudioProcessingFlags = val;
	}

	/**
	 *
	 */
	public static double getVideoFramerate()
	{
		return _videoframerate;
	}

	/**
	 *
	 */
	public static void setVideoFramerate(double val)
	{
		_videoframerate = val;
	}

	/**
	 *
	 */
	public static int getCutCounter()
	{
		return _cutcount;
	}

	/**
	 *
	 */
	public static void setCutCounter(int val)
	{
		_cutcount = val;
	}

	/**
	 *
	 */
	public static boolean getCutStatus()
	{
		return _bool;
	}

	/**
	 *
	 */
	public static void setCutStatus(boolean b)
	{
		_bool = b;
	}

	/**
	 *
	 */
	public static boolean getPvaPidExtraction()
	{
		return Pva_PidExtraction;
	}

	/**
	 *
	 */
	public static void setPvaPidExtraction(boolean b)
	{
		Pva_PidExtraction = b;
	}

	/**
	 *
	 */
	public static int getPvaPidToExtract()
	{
		return Pva_PidToExtract;
	}

	/**
	 *
	 */
	public static void setPvaPidToExtract(int val)
	{
		Pva_PidToExtract = val;
	}


	/**
	 * returns pts value from pes_extension
	 *
	 * @param1 - source array
	 * @param2 - array offset
	 * @return - pts
	 */
	public static long getPTSfromBytes(byte[] array, int offset)
	{
		return getPTSfromBytes(array, offset, true);
	}

	/**
	 * returns pts value from pes_extension
	 *
	 * @param1 - source array
	 * @param2 - array offset
	 * @param3 - trim to positive 32bit value
	 * @return - pts
	 */
	public static long getPTSfromBytes(byte[] array, int offset, boolean trim)
	{
		long pts = (6 & array[offset])<<29 | 
			(0xFF & array[offset + 1])<<22 | 
			(0xFE & array[offset + 2])<<14 |
			(0xFF & array[offset + 3])<<7 | 
			(0xFE & array[offset + 4])>>>1;

		if (trim)
			pts &= 0xFFFFFFFFL;

		return pts;
	}

	/**
	 * returns pts value from pes_extension
	 *
	 * @param1 - source array
	 * @param2 - array offset
	 * @param3 - trim to positive 32bit value
	 * @return - pts
	 */
	public static long readPTS(byte[] array, int offset, int length, boolean bytereordering, boolean trim)
	{
		long value = getValue(array, offset, length, bytereordering);

		if (trim)
			value &= 0xFFFFFFFFL;

		return value;
	}

	/**
	 *
	 */
	public static void setValue(byte[] array, int offset, int length, boolean bytereordering, long value)
	{
		for (int i = 0; bytereordering && i < length; i++)
			array[i + offset] = (byte)(0xFFL & (value>>>(i * 8)));

		for (int i = 0, j = length - 1; !bytereordering && i < length; i++, j--)
			array[i + offset] = (byte)(0xFFL & (value>>>(j * 8)));
	}

	/**
	 * returns value
	 *
	 * @param1 - source array
	 * @param2 - array offset
	 * @param3 - 
	 * @return - 
	 */
	public static long getValue(byte[] array, int offset, int length, boolean bytereordering)
	{
		long value = 0;

		try {

			for (int i = 0; bytereordering && i < length; i++)
				value |= (0xFFL & array[i + offset])<<(i * 8);

			for (int i = 0, j = length - 1; !bytereordering && i < length; i++, j--)
				value |= (0xFFL & array[i + offset])<<(j * 8);

		} catch (Exception e) {

			Common.setMessage("!> array index error (" + offset + "/" + length + "/" + array.length + ")");
			return 0;
		}

		return value;
	}

	public static int getIntValue(byte[] array, int offset, int length, boolean bytereordering)
	{
		return ((int) getValue(array, offset, length, bytereordering));
	}

	/**
	 * 
	 */
	public static void setPES_PTSField(byte[] array, int offset, long value)
	{
		array[9 + offset]  = (byte)(0x21 | (0xE & (value>>>29)));
		array[10 + offset] = (byte)(0xFF & (value>>>22));
		array[11 + offset] = (byte)(1 | (0xFE & (value>>>14)));
		array[12 + offset] = (byte)(0xFF & (value>>>7));
		array[13 + offset] = (byte)(1 | (0xFE & (value<<1)));
	}

	/**
	 * 
	 */
	public static void setPES_LengthField(byte[] array, int offset, int value)
	{
		array[4 + offset] = (byte)(0xFF & value>>>8);
		array[5 + offset] = (byte)(0xFF & value);
	}

	/**
	 * 
	 */
	public static void setPES_IdField(byte[] array, int offset, int value)
	{
		array[3 + offset] = (byte)(0xFF & value);
	}

	/**
	 * 
	 */
	public static void setPES_SubIdField(byte[] array, int offset, int pes_headerlength, int pes_extensionlength, int value)
	{
		array[pes_headerlength + pes_extensionlength + offset] = (byte)(0xFF & value);
	}

	/**
	 * 
	 */
	public static int getPES_LengthField(byte[] array, int offset)
	{
		int value = (0xFF & array[4 + offset])<<8 | (0xFF & array[5 + offset]);

		return value;
	}

	/**
	 * 
	 */
	public static int getPES_IdField(byte[] array, int offset)
	{
		int value = 0xFF & array[3 + offset];

		return value;
	}

	/**
	 * 
	 */
	public static int getPES_ExtensionLengthField(byte[] array, int offset)
	{
		int value = 0xFF & array[8 + offset];

		return value;
	}

	/**
	 * 
	 */
	public static boolean clearBit33ofPTS(byte[] array, int offset)
	{
		if ((0x80 & array[7 + offset]) != 0)
		{
			array[9 + offset] &= ~8;
			return true;
		}

		return false;
	}

	/**
	 * 
	 */
	public static boolean clearBit33ofDTS(byte[] array, int offset)
	{
		if ((0x40 & array[7 + offset]) != 0)
		{
			array[9 + 5 + offset] &= ~8;
			return true;
		}

		return false;
	}

	/**
	 * 
	 */
	public static int nextBits(byte buffer[], int BitPos, int N)
	{
		int Pos, Val;

		Pos = BitPos>>>3;

		Val =   (0xFF & buffer[Pos])<<24 |
			(0xFF & buffer[Pos + 1])<<16 |
			(0xFF & buffer[Pos + 2])<<8 |
			(0xFF & buffer[Pos + 3]);

		Val <<= BitPos & 7;
		Val >>>= 32-N;

		return Val;
	}

	/**
	 * check startcode
	 * return int of skip'able data (negative)
	 */
	public static int validateStartcode(byte[] pes_packet, int offset)
	{
		if (pes_packet[2 + offset] == 1)
		{
			if (pes_packet[1 + offset] == 0)
				if (pes_packet[offset] == 0)
					return 0;
		}

		else if (pes_packet[2 + offset] == 0)
		{
			if (pes_packet[1 + offset] == 0)
				return -1;
			else
				return -2;
		}

		return -3;
	}


	/**
	 * check startcode
	 * return int of skip'able data (negative)
	 */
	public static int validateMp4Startcode(byte[] pes_packet, int offset)
	{
		if (pes_packet[3 + offset] == 1)
		{
			if (pes_packet[2 + offset] == 0)
				if (pes_packet[1 + offset] == 0)
					if (pes_packet[offset] == 0)
						return 0;
		}

		else if (pes_packet[3 + offset] == 0)
		{
			if (pes_packet[2 + offset] == 0)
			{
				if (pes_packet[1 + offset] == 0)
					return -1;
				else
					return -2;
			}
			else
				return -3;
		}

		return -4;
	}

	/**
	 * skip leading bytes before first valid startcodes and return fixed array
	 */
	public static boolean alignSyncword(byte[] pes_packet, int pes_offset, int es_streamtype)
	{
		int pes_payloadlength = getPES_LengthField(pes_packet, pes_offset);
		int pes_headerlength = 9;
		int pes_packetlength = pes_headerlength - 3 + pes_payloadlength;
		int pes_extensionlength = getPES_ExtensionLengthField(pes_packet, pes_offset);

		int offset = pes_headerlength + pes_extensionlength;
		int i, j, k;

		boolean found = false;

		packloop:
		for (i = offset, j = pes_packetlength - 3, k = offset + pes_offset; i < j; i++, k++)
		{
			switch (es_streamtype)
			{
			case MPEG_VIDEO:
				if (pes_packet[k] != 0 || pes_packet[1 + k] != 0  || pes_packet[2 + k] != 1 || pes_packet[3 + k] != (byte)SEQUENCE_HEADER_CODE) 
					continue packloop;

				found = true;
				break packloop;

			case AC3_AUDIO:
			case DTS_AUDIO:
				if ((pes_packet[k] != 0xB || pes_packet[1 + k] != 0x77) && (pes_packet[k] != 0x7F || pes_packet[1 + k] != (byte)0xFE || pes_packet[2 + k] != (byte)0x80 || pes_packet[3 + k] != 1)) 
					continue packloop;

				found = true; 
				break packloop;

			case MPEG_AUDIO:
				if ( pes_packet[k] != (byte)0xFF || (0xF0 & pes_packet[1 + k]) != 0xF0)
					continue packloop;

				found = true; 
				break packloop;
			}
		}

		if (found) 
		{
			System.arraycopy(pes_packet, i + pes_offset, pes_packet, offset + pes_offset, pes_packetlength - i);

			pes_payloadlength -= (i - offset);

			setPES_LengthField(pes_packet, pes_offset, pes_payloadlength);
		}

		return found;
	}

	/**
	 * create PTS alias 
	 */
	public static void logAlias(JobProcessing job_processing, String _vptslog, String _datalog)
	{
		File vpts = new File(_vptslog); 

		try {
			RandomAccessFile log = new RandomAccessFile(_datalog, "rw");

			log.seek(0);

			if (vpts.exists() && vpts.length() > 0)
			{
				RandomAccessFile vlog = new RandomAccessFile(_vptslog, "r");
				long p = vlog.readLong();

				if (!job_processing.hasElementaryVideoStream() && job_processing.getSplitPart() == 0)
					job_processing.setFirstAudioPts(p);

				log.writeLong(job_processing.getFirstAudioPts() + (Common.getSettings().getBooleanProperty(Keys.KEY_additionalOffset) ? 90L * Common.getSettings().getIntProperty(Keys.KEY_ExportPanel_additionalOffset_Value) : 0));

				vlog.close();
			}
			else 
				log.writeLong(0L + (Common.getSettings().getBooleanProperty(Keys.KEY_additionalOffset) ? Common.getSettings().getIntProperty(Keys.KEY_ExportPanel_additionalOffset_Value) : 0));

			log.writeLong(0L); 
			log.close();

			Common.setMessage("");
			Common.setMessage(Resource.getString("all.msg.pts.faked"));

		} catch (IOException e) {

			Common.setExceptionMessage(e);
		}
	}

	/**
	 * split reset
	 */
	public static void resetSplitMode(JobProcessing job_processing, String vptslog)
	{
		if ( vptslog.equals("-1") && job_processing.getSplitSize() > 0 )
		{ 
			job_processing.setSplitSize(0);
			Common.setMessage(Resource.getString("splitreset.novideo"));
		}
	}


	/**
	 * parse cut long value
	 */
	public static String parseCutValue(long value)
	{
		if (Common.getSettings().getIntProperty(Keys.KEY_CutMode) != CUTMODE_TIME)
			return String.valueOf(value);

		String str = "";
		long frametime = 1;
		value /= 90;

		DateFormat time = new SimpleDateFormat("H:mm:ss:");
		time.setTimeZone(TimeZone.getTimeZone("GMT+0:00"));

		Calendar cal = Calendar.getInstance();
		cal.setTimeZone(TimeZone.getTimeZone("GMT+0:00"));
		cal.setTime(new Date(value));

		int frame = (cal.get(14) / ((int)frametime));
		str = time.format(new Date(value)) + (frame < 10 ? "0" : "") + frame;

		return str;
	}

	/**
	 * parse cut field value
	 */
	public static long parseCutValue(String value, boolean demux)
	{
		if (value == null)
			return 0;

		value = value.trim();

		try {
			if (Common.getSettings().getIntProperty(Keys.KEY_CutMode) != CUTMODE_TIME)
				return Long.parseLong(value);

		} catch (Exception e) {
			return -1L;
		}

		int i;
		if ( (i = value.indexOf(" ")) > -1)
			value = value.substring(0, i);

		value = value.replace('.',':');

		if (value.indexOf(":") < 0)
			return Long.parseLong(value);

		StringTokenizer st = new StringTokenizer(value, ":");
		String str = null;
		long val = 0;
		long frametime = !demux ? 90 : (long)_videoframerate;
		long mp[] = { 324000000L, 5400000L, 90000L, frametime }; //h,m,s,f

		for (int a=0; st.hasMoreTokens() && a < 4; a++)
		{
			str = st.nextToken();
			val += (mp[a] * Long.parseLong(str));
		}

		return val;
	}


	/**
	 * video timelength read from ptslogfile
	 */
	public static long calcVideoTime(String logfile)
	{
		long vtime = 0;

		try {
			long vlogsize = new File(logfile).length();
			RandomAccessFile vlog = new RandomAccessFile(logfile, "r");

			vlog.seek(vlogsize - 8);

			vtime = vlog.readLong();

			vlog.close();
		}

		catch (IOException e) {

			Common.setExceptionMessage(e);
		}

		return vtime;
	}


	/**
	 * make cut
	 */
	public static boolean makecut(JobProcessing job_processing, long comparePoint, List ctemp)
	{
		return makecut(job_processing, null, 0, comparePoint, new ArrayList(), 0, ctemp, 0, new ArrayList());
	}

	/**
	 * make cut
	 */
	public static boolean makecut(JobProcessing job_processing, String cuts_filename, long startPTS, long comparePoint, List newcut, int lastframes, List ctemp, int gopnumber, List cell)
	{
		if (ctemp.isEmpty())
			return true;

		long[] abc;

		if ( _cutcount < ctemp.size() )
		{ 
			if ( comparePoint > parseCutValue(ctemp.get(_cutcount).toString(), true) )
			{
				/**
				 * ungerade == cutout
				 */
				if ((_cutcount & 1) == 1)
				{
					_bool = false; 
					for (int c = newcut.size() - 1; c >- 1; c--)
					{
						abc = (long[])newcut.get(c);

						if ( abc[0] < parseCutValue(ctemp.get(_cutcount).toString(), true) )
						{ 
							_bool = true; 

							Common.setMessage(Resource.getString("msg.cuts.cutin", "" + gopnumber, "" + lastframes, "" + Common.formatTime_1((long)(lastframes * (double)(_videoframerate / 90.0f)) )) + " (" + comparePoint + ")");

							saveCuts(comparePoint, startPTS, lastframes, cuts_filename);

						//	if (lastframes > 0) 
						//		cell.add("" + lastframes); // celltimes for cutin

							break;
						}
					}

					if (!_bool)
					{
						Common.setMessage(Resource.getString("msg.cuts.cutout", "" + gopnumber) + " (" + comparePoint + ")");

						saveCuts(comparePoint, startPTS, lastframes, cuts_filename);
					}

					_cutcount++;

					return _bool;
				}

				/**
				 * gerade == cutin
				 */
				else
				{

					_bool = true;
					_cutcount++;

					Common.setMessage(Resource.getString("msg.cuts.cutin", "" + gopnumber, "" + lastframes, "" + Common.formatTime_1((long)(lastframes * (double)(_videoframerate / 90.0f)) )) + " (" + comparePoint + ")");

					saveCuts(comparePoint, startPTS, lastframes, cuts_filename);

				//	if (lastframes > 0) 
				//		cell.add("" + lastframes); // celltimes for cutin

					if (_cutcount >= ctemp.size()) 
						return _bool;

					for (int c = newcut.size() - 1; c >- 1; c--)
					{
						abc = (long[])newcut.get(c);

						if ( abc[0] < parseCutValue(ctemp.get(_cutcount).toString(), true) )
						{ 
							_cutcount++;

							break;
						}
					}

					return _bool;
				}
			}
		}

		else
		{ 
			if (!_bool && job_processing.getCutComparePoint() == 10000000) 
				job_processing.setCutComparePoint(comparePoint);
		}

		return _bool;
	}

	/**
	 * saves the cutpoint list from coll specials
	 */
	private static void saveCuts(long cutposition, long startPTS, long lastframes, String cuts_filename)
	{
		if ( Common.getSettings().getBooleanProperty(Keys.KEY_ExternPanel_exportPts) && cuts_filename != null)
		{
			try {
				cuts_filename += ".Xpl";

				PrintWriter pts_writer = new PrintWriter(new FileOutputStream(cuts_filename, lastframes > 0));

				if (new File(cuts_filename).length() == 0)
					pts_writer.println(Keys.KEY_CutMode[0] + "=3"); // PTS cut mode fixed for file

				pts_writer.println(startPTS);
				pts_writer.close();

				Common.setMessage(Resource.getString("msg.savecut", "" + startPTS));

			} catch (IOException e) {

				Common.setExceptionMessage(e);
			}
		}
	}


	/**
	 * set 1. videoheader
	 */
	public static void setVideoHeader(JobProcessing job_processing, String videofile, String logfile, int[] clv, int[] MPGVideotype)
	{
		long time = 0;
		String videotype[] = { "(m1v)", "(m2v)", "(h264)" };

		String frames_used[] = { 
			Resource.getString("video.msg.io.non"), 
			Resource.getString("video.msg.io.int"), 
			Resource.getString("video.msg.io.pro"), 
			Resource.getString("video.msg.io.int_pro")
		};

		if (new File(logfile).exists())
		{
			time = (calcVideoTime(logfile) / 90);
			String vt = Common.formatTime_1( time / 10 * 10 );

			Common.setMessage(Resource.getString("video.msg.length", "" + job_processing.getExportedVideoFrameNumber()) + " " + vt);
			Common.setMessage(Resource.getString("video.msg.gop.summary", "" + (0x7FFF & clv[9]>>>15), "" + (0x7FFF & clv[9]), "" + frames_used[clv[9]>>>30]));

			if (clv[8] > 0)
				Common.setMessage(Resource.getString("video.error.pts.same", "" + clv[8]));

			job_processing.getSummaryInfo().add(Resource.getString("video.summary", videotype[MPGVideotype[0]], "" + job_processing.getExportedVideoFrameNumber(), "" + vt) + "'" + videofile + "'");
		}

		if (MPGVideotype[0] > 1) // h264 without modif.
		{
			Common.setMessage(Resource.getString("video.msg.bitrate.avg", "" + ((job_processing.getMinBitrate() + job_processing.getMaxBitrate()) / 2 * 400), "" + (job_processing.getMinBitrate() * 400) + "/" + (job_processing.getMaxBitrate() * 400)));
			Common.setMessage(Resource.getString("msg.newfile") + " " + videofile);
			return;
		}

		try {

			RandomAccessFile pv2 = new RandomAccessFile(videofile, "rw");

			if (Common.getSettings().getIntProperty(Keys.KEY_ChangeBitrateInAllSequences) > 0)
			{
				if (time == 0) 
					Common.setMessage(Resource.getString("video.msg.bitrate.avg", "" + ((job_processing.getMinBitrate() + job_processing.getMaxBitrate()) / 2 * 400), "" + (job_processing.getMinBitrate() * 400) + "/" + (job_processing.getMaxBitrate() * 400)));

				else 
					Common.setMessage(Resource.getString("video.msg.bitrate.avgnom", "" + ((pv2.length() * 8000L) / time), "" + (job_processing.getMinBitrate() * 400) + "/" + (job_processing.getMaxBitrate() * 400)));

			}

			else if (Common.getSettings().getBooleanProperty(Keys.KEY_DebugLog)) 
				System.out.println();

			if (!Common.getSettings().getBooleanProperty(Keys.KEY_WriteOptions_writeVideo))
			{
				pv2.close(); 

				return; 
			}

			int max_bitrate_value = 22500; // 9.0Mbit, (video only)

			/**
			 * bitraten 
			 */
			if (Common.getSettings().getIntProperty(Keys.KEY_ChangeBitrateInFirstSequence) > 0)
			{
				int newmux = (Common.getSettings().getIntProperty(Keys.KEY_ChangeBitrateInFirstSequence) == 3) ? job_processing.getMaxBitrate() : max_bitrate_value;

				if (Common.getSettings().getIntProperty(Keys.KEY_ChangeBitrateInFirstSequence) == 2 && job_processing.getMaxBitrate() < max_bitrate_value) 
					newmux = job_processing.getMaxBitrate();

				if (Common.getSettings().getIntProperty(Keys.KEY_ChangeBitrateInFirstSequence) == 1)
				{
					if (time == 0) 
						newmux = (job_processing.getMinBitrate() + job_processing.getMaxBitrate()) / 2;   // old calcul. avg.

					else
					{
						newmux = (int)(((pv2.length() * 8000L) / time) / 400);

						if (newmux < 0 || newmux > max_bitrate_value) 
							newmux = (job_processing.getMinBitrate() + job_processing.getMaxBitrate()) / 2;   // old calcul. avg.
					}
				}

				if (Common.getSettings().getIntProperty(Keys.KEY_ChangeBitrateInFirstSequence) == 4)
				{ 
					newmux = 0x3FFFF;

					Common.setMessage(Resource.getString("video.msg.bitrate.vbr"));
				}

				else 
					Common.setMessage(Resource.getString("video.msg.bitrate.val", "" + (newmux*400)));

				pv2.seek(8);
				newmux = (newmux<<14) | ((pv2.readInt()<<18)>>>18);

				pv2.seek(8);
				pv2.writeInt(newmux);
			}

			/**
			 * patch resolution DVD-conform 
			 */
			//JLA14082003+
			//0: no patch, 1:patch unconditionally, 2:patch if <>720|352, 3:pach if <>720|704|352
			if (Common.getSettings().getIntProperty(Keys.KEY_ConditionalHorizontalPatch) != 0)
			{
				pv2.seek(4);

				int resolutionOrig = pv2.readInt();
				int hresOrg = (resolutionOrig>>>20);
				int resolution = (0xFFFFF & resolutionOrig) | Common.getSettings().getIntProperty(Keys.KEY_ConditionalHorizontalResolution)<<20;
				boolean doPatch;

				switch (Common.getSettings().getIntProperty(Keys.KEY_ConditionalHorizontalPatch))
				{
				case 2: 
					doPatch = hresOrg != 720 && hresOrg != 352; 
					break;

				case 3: 
					doPatch = hresOrg != 720 && hresOrg != 704 && hresOrg != 352; 
					break;

				default: 
					doPatch = true; 
				}

				if(doPatch)
				{
					pv2.seek(4);
					pv2.writeInt(resolution);

					Common.setMessage(Resource.getString("video.msg.resolution", "" + (resolutionOrig>>>20)+"*"+((0xFFF00&resolutionOrig)>>>8)) + " " + (resolution>>>20)+"*"+((0xFFF00&resolution)>>>8));
				}
			}
			//JLA14082003-

			pv2.close();

			Common.setMessage(Resource.getString("msg.newfile") + " " + videofile);

		} catch (IOException e) {

			Common.setExceptionMessage(e);
		}
	}

	/**
	 * vdr_dvbsub determination
	 */
	public static int getExtension2_Id(byte[] pes_packet, int pes_headerlength, int pes_payloadlength, int pesID, boolean pes_isMpeg2, long file_position)
	{
		boolean extension_error = false;
		int pes_extension2_id = -1;

		if (!pes_isMpeg2)
			return pes_extension2_id;

		else if (pesID != PRIVATE_STREAM_1_CODE && (0xE0 & pesID) != 0xC0 && (0xF0 & pesID) != 0xE0) //not pD, Aud, Vid
			return pes_extension2_id;

		//read flags
		int pes_shift = pes_headerlength; //points to 1st appendix

		pes_shift += (0x80 & pes_packet[7]) != 0 ? 5 : 0; //pes_pts
		pes_shift += (0x40 & pes_packet[7]) != 0 ? 5 : 0; //pes_dts
		pes_shift += (0x20 & pes_packet[7]) != 0 ? 6 : 0; //pes_escr
		pes_shift += (0x10 & pes_packet[7]) != 0 ? 3 : 0; //pes_esrate
		pes_shift += (4 & pes_packet[7]) != 0 ? 1 : 0; //pes_copy
		pes_shift += (2 & pes_packet[7]) != 0 ? 2 : 0; //pes_crc

		boolean pes_ext1 = (1 & pes_packet[7]) != 0; //ext1
		boolean pes_ext2 = false; //ext2

		int pes_extension_length = 0xFF & pes_packet[8]; //all data must be inside extension

		if (pes_headerlength + pes_extension_length < pes_shift)
			extension_error = true;

		else if (pes_ext1 && pes_headerlength + pes_extension_length < pes_shift + 1)
			extension_error = true;

		else if (pes_ext1)
		{
			int shift = pes_shift;

			if (6 + pes_payloadlength < pes_shift + 1)
				extension_error = true;

			else
			{
				pes_shift += (0x80 & pes_packet[shift]) != 0 ? 16 : 0; //pes_private
				pes_shift += (0x40 & pes_packet[shift]) != 0 ? 1 : 0; //pes_packfield
				pes_shift += (0x20 & pes_packet[shift]) != 0 ? 2 : 0; //pes_sequ_counter
				pes_shift += (0x10 & pes_packet[shift]) != 0 ? 2 : 0; //pes_P-STD
				// marker 3 bits

				pes_ext2 = (1 & pes_packet[shift]) != 0; //ext2

				pes_shift++; //skip flag_fields of ext1

				if (pes_headerlength + pes_extension_length < pes_shift)
					extension_error = true;

				else if (pes_ext2)
				{
					int pes_ext2_length = 0x7F & pes_packet[pes_shift];

					pes_shift++; //skip length_fields of ext2

					if (6 + pes_payloadlength < pes_shift + pes_ext2_length)
						extension_error = true;

					else if (pes_headerlength + pes_extension_length < pes_shift + pes_ext2_length)
						extension_error = true;

					else if (pesID == PRIVATE_STREAM_1_CODE)
						pes_extension2_id = 0xFF & pes_packet[pes_shift]; //read byte0 (res.) of ext2
				}
			}
		}

		if (extension_error)
			Common.setMessage("!> error in pes_extension of pes-ID 0x" + Integer.toHexString(pesID).toUpperCase() + " @ pos: " + file_position + " (" + (6 + pes_payloadlength) + " / " + (pes_headerlength + pes_extension_length) + " / " + (pes_shift + 1) + " / " + pes_ext1 + " / " + pes_ext2 + ")");

		return pes_extension2_id;
	}
}