/*
 * @(#)TS.java - constants to create TS packets
 *
 * Copyright (c) 2002-2009 by dvb.matt, All Rights Reserved. 
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

/*
 * Arion export introduced by Cameron D (AU)
 * from 0.90.4.00b27
 */


package net.sourceforge.dvb.projectx.thirdparty;

import java.util.List;
import java.util.Calendar;
import java.util.Date;
import java.util.Arrays;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.StringTokenizer;
import java.util.ArrayList;
import net.sourceforge.dvb.projectx.parser.CommonParsing;


import net.sourceforge.dvb.projectx.common.Common;
import net.sourceforge.dvb.projectx.common.Resource;
import net.sourceforge.dvb.projectx.common.JobProcessing;

public class TS {

	private static byte[] service_name = null;
	private static byte[] event_name = null;
	private static byte[] event_text = null;


	public TS()
	{}

	private static byte[] TF4000header = {
		(byte)0xCD, 0x39, 0xc, 0, //MJD
		(byte)0xCD, 0x39, 0xc, 0, //MJD
		0, 0x3c,  //duration
		0, 0x1f,  //service
		0, 0,     //0=tv, 1=radio

		5, 0, 6,
		(byte)0xb0, //tuner
		1, 2,  //sid
		1, 0,  //pmt
		0, (byte)0xe0,  //pcr
		0, (byte)0xe0,  //vid
		0, (byte)0xc0,  //aud

		0x4D,0x79,0x20,0x70,0x65,0x72,0x73,0x6F,0x6E,0x61,0x6C,0x20,0x54,0x56,0x20,0x43,0x68,0x61,0x6E,0x6E,0x65,0x6C,0,0,

		5, 0,
		0x30, (byte)0xc0,
		0x6b, 0x6c,
		0, 1,

		0x40, 0x1f,
		1, 1,
		(byte)0xCD, 0x39, 0xb, 0,
		(byte)0xCD, 0x39, 0xc, 0,
		0, 0x3c,
		4,
		4,
		84, 69, 83, 84,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,

		1,2
	};

	//introduced by 'catapult' 09082004
	private static byte[] TF5000header = {
		// HEADER 14 bytes, start at 0
		0x54, 0x46, 0x72, 0x63,  // Id "TFrc" *
		0x50, 0,                 // Version  *
		0, 0,                    // Reserved *
		0, 0,                    // Duration in Minutes
		0, 0xa,                  // Service number in channel list (Does not matter in playback)
		0, 0,                    // Service type 0:TV 1:Radio

		// SERVICE_INFO 38 bytes starts at 14
		0, 0, 1, 0x30,           //  Reserved and Tuner (Does not matter in playback) (Tuner 1+2 flagged)
		1, 2,                    //  Service ID of TS stream 
		1, 0,                    //  PID number of PMT TS packet
		0, (byte)0xe0,           //  PID number of PCR TS packet
		0, (byte)0xe0,           //  PID number of Video TS packet  
		0, (byte)0xc0,           //  PID number of Audio TS packet, MPA as std

		0x50, 0x72, 0x69, 0x76, 0x61, 0x74, 0x65, 0x20, 0x52, 0x65, 0x63, 0x6F, 0x72, 0x64, 0x69, 0x6E, 0x67, 0, 0, 0, 0, 0, 0, 0, // Service Name

		// TP_INFO 16 bytes starts at 52
		0,                       //  Satelite Index
		8, 7, 0,                 //  Polarity and Reserved (Does not matter in playback)
		0, 0, 0x2F, (byte)0x9B,        //  Frequency  (Does not matter in playback)
		0x6B, 0x6C,              //  Symbol Rate  (Does not matter in playback)
		1, 1,                    //  Transport Stream Id (Does not matter in playback)
		0, 0, 0, 0,              //  Reserved *

		// EVT_INFO 160 bytes starts at 68
		(byte)0x80, 0x02,       //  Reserved *
		0, 0,                   //  Duration in Minutes
		0, 0x3c, 4, 4,          //  Event Id
		84, 69,                 //  Modified Julian date start time
		83,                     //  Hour of start time
		84,                     //  Minute os start time
		0, 0,                   //  Modified Julian date end time
		0,                      //  Hour of end time
		0,                      //  Minute os end time
		4,                      //  Reserved
		0,                      //  Length of name in Event text
		0,                      //  Parental rate

		// the rest is 0 so it's not defined explicitly

		// Event text

		// EXT_EVT_INFO 1088 bytes starts at 228 
		// Extended Event text

	};

	//introduced by 'jkit' 23012009
	private static byte[] TF5200header = {
		// HEADER 14 bytes, start at 0
		0x54, 0x46, 0x72, 0x63,  // Id "TFrc" *
		0x50, 0,                 // Version  *
		0, 0,                    // Reserved *
		0, 0,                    // Duration in Minutes
		0, 0xa,                  // Service number in channel list (Does not matter in playback)
		0, 0,                    // Service type 0:TV 1:Radio

		// SERVICE_INFO 38 bytes starts at 14
		0, 0, 1, 0x30,           //  Reserved and Tuner (Does not matter in playback) (Tuner 1+2 flagged)
		1, 2,                    //  Service ID of TS stream 
		1, 0,                    //  PID number of PMT TS packet
		0, (byte)0xe0,           //  PID number of PCR TS packet
		0, (byte)0xe0,           //  PID number of Video TS packet  
		0, (byte)0xc0,           //  PID number of Audio TS packet, MPA as std

		0x50, 0x72, 0x69, 0x76, 0x61, 0x74, 0x65, 0x20, 0x52, 0x65, 0x63, 0x6F, 0x72, 0x64, 0x69, 0x6E, 0x67, 0, 0, 0, 0, 0, 0, 0, // Service Name

		// TP_INFO 12 bytes starts at 52
		0, 6, 0x41, (byte)0x90, // Frequency (e.g. 410000 KHz)
		0x1A, (byte)0xF4,       // Symbol_Rate (e.g. 6900 kS/s)
		1, 1,                // Transport_Stream_Id (e.g. 0x044D) 
		0, 1,                         // Network_Id
		2,                            // Modulation 0=16QAM, 1=32QAM, 2=64QAM, 3=128QAM, 4=256QAM
		0,                            // Reserved1

		// EVT_INFO 160 bytes starts at 64
		(byte)0x80, 0x02,       //  Reserved *
		0, 0,                   //  Duration in Minutes
		0, 0x3c, 4, 4,          //  Event Id
		84, 69,                 //  Modified Julian date start time
		83,                     //  Hour of start time
		84,                     //  Minute os start time
		0, 0,                   //  Modified Julian date end time
		0,                      //  Hour of end time
		0,                      //  Minute os end time
		4,                      //  Reserved
		0,                      //  Length of name in Event text
		0,                      //  Parental rate

		// the rest is 0 so it's not defined explicitly

		// Event text

		// EXT_EVT_INFO 
		// Extended Event text

	};

	/*
	 * header code for Arion TS file (.AVR file)
	 * There are two file formats, .AVR (the AV stream), and .AVF, the real header file.
	 */
		// The AVF filename must be retained for inclusion into each AVR header file.
		// the filename format is:
		// <user-supplied-rootname>_#00n_<date-time>.AVx
		// Where n is the part number (starting from 1)
		// 	date-time is in the format yyyymmddhhmm
			// full pathname on PVR disc,
	private static StringBuffer ArionAVFPathname_onPVR = new StringBuffer( 128 );
			// local filename - as finally named on this system; may or may not include directories.
	private static StringBuffer ArionAVRLocalFilename = new StringBuffer( 1024 );
	private static File ArionAVFLocalPathname ;
		// the initial date+time used must be remembered so we can rebuild the
		// matching name for second and later file parts.
	private static StringBuffer ArionAVFTime = new StringBuffer( 128 );
		// same the initial filename prefix for building other names later
	private static StringBuffer ArionFilenameRoot = new StringBuffer( 128 );

		// we need to keep track of total file sizes and playback length.
	private static long ArionCumulativeTime = 0L;		// seconds
		// bytes (excluding header) - note Java long is 64 bits, so
		//  it has no problems with large files.
	private static long ArionCumulativeStreamSize = 0L;

	private static final int ArionAVR_HEADERSIZE = 0x8000;
	private static byte[] ArionAVR_Header = {
		// HEADER 32k bytes, start at 0
		0x41, 0x52, 0x41, 0x56,  // Id "ARAV" *
		0x10, 0,                 // unknown  *
		0, (byte)0x86,           // unknown *
		(byte)0x80, 0,                    // possibly length of header
		0x43, 0x3a, 0x5c, 0x50, 0x56, 0x52, 0x5c, 0x41, 0x56, 0x5c,     // pathname of AVF file.

		// the rest is 0 so it's not defined explicitly


	};

	private static final int ArionAVF_HEADERSIZE = 15160;		// File is 15k bytes
	private static byte[] ArionAVF_Header = {
		
		0x41, 0x52, 0x4e, 0x46,  // Id "ARNF" *
		0x11, 0, 0, 0x04,         // unknown 
		0x3f, 0x04, 0, 0x0a,     // unknown

		// Channel-related info
		0,                       // unknown
		0, 0, 0,                 // varies with broadcast channel
		0, 0x01,                 // 0x10: channel number
		0x07, 0,                 // constant
		0, 0x48,                 // constant
		0x01, 0,                 // 1 = TV, 2 = radio

		// SID/PID details
		1, 2,                 // SID
		0, 1,			// number of audio streams
		0, (byte)0xc0,		// audio PID 1
		0x7f, (byte)0xff,	// audio PID 2
		0x11, 0x10,		// 0x20:  unknown
		0, (byte)0xe0,		// PID for video
		0, 0,			// unknown
		0, (byte)0x90,		// PID for text/subtitles
		1, 0,			// varies with channel
		0,			// 0x2a:  unknown...
		0x50, 0x72, 0x6f, 0x6a, 0x65,		// Program stream name
		0x63, 0x74, 0x2d, 0x58, 0x20, 0x63, 0x6f, 0x6e,    // 0x30:
		0x76, 0x65, 0x72, 0x74, 0x65, 0x64, 0x0a, 0x00,
		0, 0, 0, 0,	  		// 0x40:
		1, 0x11,		// unknown, sometimes zero.
		0, 0,			// unknown
		0, 1,			// unknown - zero or 1
		0, 0,			// 0x4a: unknown - quite variable
		// block of zeroes - unknown
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		// EPG start - this is EPG "now" when recording started.
		2, (byte)0xca,		// size of EPG block
		7, (byte)0xd6,		//        year of scheduled start of tv program
		9,			// 0x60: month of scheduled start of tv program
		8,			//         day of scheduled start of tv program
		7,			//        hour of scheduled start of tv program
		6,			//      minute of scheduled start of tv program
		3,			// scheduled duration (hours)
		30, 			// scheduled duration (minutes)
			// the program title - filled in with file name
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,		// total 64 bytes, null padded

			// the program subtitle or description
		0x53, 0x75, 0x62, 0x74, 0x69, 0x74, 0x6c, 0x65,
		0x20, 0x69, 0x6e, 0x73, 0x65, 0x72, 0x74, 0x65,
		0x64, 0x20, 0x62, 0x79, 0x20, 0x50, 0x72, 0x6f,
		0x6a, 0x65, 0x63, 0x74, 0x2d, 0x58, 0x0a, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,


		// the rest is 0 so it's not defined explicitly - actually - filled at run-time.


	};

	/*
	 * data structure defining the layout of EPG data stored in file in two places
	 */

	private static final int ArionEPG_BLOCKSIZE = 714;
	private static byte[] Arion_EPG_Block = {
			// EPG  - this is EPG "now" during recording or when started.
		2, (byte)0xca,		// size of EPG block
		7, (byte)0xd6,		//        year of scheduled start of tv program
		9,			// 0x60: month of scheduled start of tv program
		8,			//         day of scheduled start of tv program
		7,			//        hour of scheduled start of tv program
		6,			//      minute of scheduled start of tv program
		5,			// scheduled duration (hours)
		30, 			// scheduled duration (minutes)
			// the program title
		0x70, 0x72, 0x6f, 0x67, 0x72, 0x61, 0x6d, 0x20, 0x74, 0x69, 0x74, 0x6c, 0x65, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,		// total 64 bytes, null padded

			// the program subtitle or description
		0x53, 0x75, 0x62, 0x74, 0x69, 0x74, 0x6c, 0x65,
		0x20, 0x69, 0x6e, 0x73, 0x65, 0x72, 0x74, 0x65,
		0x64, 0x20, 0x62, 0x79, 0x20, 0x50, 0x72, 0x6f,
		0x6a, 0x65, 0x63, 0x74, 0x2d, 0x58, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,		// total 128 bytes

			// program description (apparently not used)

		// the rest is 0 so it's not defined explicitly - actually - filled at run-time.
	};



	private static byte[] pmt1 = { 
		0x47,0x41,0,0x10,
		0, 2, (byte)0xB0, (byte)0x95, 1, 2,(byte)0xC1, 0, 
		0, (byte)0xE0, (byte)0xE0, (byte)0xF0, 0,
		2, (byte)0xE0, (byte)0xE0, (byte)0xF0, 3, 0x52, 1, 1, 
		3, (byte)0xE0, (byte)0xc0, (byte)0xF0, 0x9, 0x52, 1, 3, 0xA, 4, 0x64, 0x65, 0x75, 1, 
		3, (byte)0xE0, (byte)0xc1, (byte)0xF0, 0x9, 0x52, 1, 4, 0xA, 4, 0x64, 0x65, 0x75, 1, 
		3, (byte)0xE0, (byte)0xc2, (byte)0xF0, 0x9, 0x52, 1, 5, 0xA, 4, 0x64, 0x65, 0x75, 1, 
		3, (byte)0xE0, (byte)0xc3, (byte)0xF0, 0x9, 0x52, 1, 6, 0xA, 4, 0x64, 0x65, 0x75, 1, 
		6, (byte)0xE0, (byte)0x80, (byte)0xF0, 0xC, 0x52, 1, 0x11, 0x6A, 1, 0, 0xA, 4, 0x64, 0x65, 0x75, 0, 
		6, (byte)0xE0, (byte)0x81, (byte)0xF0, 0xC, 0x52, 1, 0x12, 0x6A, 1, 0, 0xA, 4, 0x64, 0x65, 0x75, 0, 
		6, (byte)0xE0, (byte)0x82, (byte)0xF0, 0xC, 0x52, 1, 0x13, 0x6A, 1, 0, 0xA, 4, 0x64, 0x65, 0x75, 0, 
		6, (byte)0xE0, (byte)0x90, (byte)0xF0, 0x10,0x52, 1, (byte)0xC2, 0x56, 5, 0x65, (byte)0x6E, 0x67, 0x9, 0, 0xA, 4, 0x64, 0x65, 0x75, 0,
		(byte)0x85, (byte)0x33, (byte)0x49, (byte)0x7e // CRC32
	};

	private static int count1=0, count2=0, count3=0;

	private static byte[] pcr = new byte[188];
	private static byte[] pat = new byte[188];
	private static byte[] pmt = new byte[188];
	private static byte[] pmtHead = { 0x47,1,0,0x10 };
	private static byte[] pmtStart = { 0, 2, (byte)0xB0, 0, 1, 2,(byte)0xC1, 0 };
	private static byte[] pmtPCR =  { 0, (byte)0xE0, (byte)0xE0, (byte)0xF0, 0 };
	private static byte[] pmtMPV =  { 2, (byte)0xE0, (byte)0xE0, (byte)0xF0, 3, 0x52, 1, 1 };
	private static byte[] pmtMPA =  { 3, (byte)0xE0, (byte)0xC0, (byte)0xF0, 0x9, 0x52, 1, 3, 0xA, 4, 0x64, 0x65, 0x75, 1 };
	private static byte[] pmtAC3 =  { 6, (byte)0xE0, (byte)0x80, (byte)0xF0, 0xC, 0x52, 1, 4, 0x6A, 1, 0, 0xA, 4, 0x64, 0x65, 0x75, 0 };
	private static byte[] pmtAC3_atsc =  { (byte)0x81, (byte)0xE0, (byte)0x80, (byte)0xF0, 0xF, 0x52, 1, 4, 0xA, 4, 0x65, 0x6E, 0x67, 0, 5, 4, 0x41, 0x43, 0x2D, 0x33 };
//	private static byte[] pmtTTX =  { 6, (byte)0xE0, (byte)0x90, (byte)0xF0, 0x1F, 0x52, 1, 5, 0x56, 20, 0x65, 0x6E, 0x67, 0x9, 0, 0x64, 0x65, 0x75, 0x11, 0x50, 0x67, 0x65, 0x72, 0x17, 0x77, 0x65, 0x6E, 0x67, 0x10, (byte)0x88, 0xA, 4, 0x64, 0x65, 0x75, 0 };
	private static byte[] pmtTTX =  { 6, (byte)0xE0, (byte)0x90, (byte)0xF0, 0x2C, 0x52, 1, 5, 0x56, 20, 0x65, 0x6E, 0x67, 0x9, 0, 0x64, 0x65, 0x75, 0x11, 0x50, 0x67, 0x65, 0x72, 0x17, 0x77, 0x65, 0x6E, 0x67, 0x10, (byte)0x88, 0xA, 4, 0x64, 0x65, 0x75, 0,
		0x45, 0x0B, 0x01, 0x06, (byte)0xE7, (byte)0xE8, (byte)0xE9, (byte)0xEA, (byte)0xEB, (byte)0xEC, 0x04, 0x01, (byte)0xF0 
	};

	private static byte[] pmtSUP =  { 6, (byte)0xE0, (byte)0x20, (byte)0xF0, 0x0D, 0x52, 1, 6, 0x59, 8, 0x64, 0x65, 0x75, 0x10, 0, 1, 0, 1 };
	private static byte[] autopmt = new byte[0];

	private static int firstID = 0xE0;
	private static boolean myTTX = false;

	public static void setPmtPids(List PIDs) throws IOException
	{
		if (myTTX) 
			PIDs.add("" + 0x39F);

		Object[] Pids = PIDs.toArray();

		if (Pids.length == 0)
		{
			Common.setMessage(Resource.getString("ts.msg1"));
			autopmt = pmt;

			return;
		}

		ByteArrayOutputStream pmtout = new ByteArrayOutputStream();

		Arrays.sort(Pids);

		int lfn = 1;  // byte 7 = substreamID for program component

		pmtout.write(pmtStart);

		firstID = (0xFF & Integer.parseInt(Pids[0].toString()));

		pmtPCR[2] = (byte)firstID;
		updateHeader(23, firstID);

		pmtout.write(pmtPCR);

		for (int a = 0; a < Pids.length; a++) // get Pid Hex: 0..=V, 1..=MPA, 2..=AC3, 3..=TTX
		{   
			int Pid = Integer.parseInt(Pids[a].toString());

			switch (0xF & (Pid>>>8)) {
			case 0:   // vid
				pmtMPV[2] = (byte)(0xFF & Pid);
				pmtMPV[7] = (byte)lfn++;
				pmtout.write(pmtMPV);
				break;

			case 1:    // mpeg-1 (-2) audio
				pmtMPA[2] = (byte)(0xFF & Pid);
				pmtMPA[7] = (byte)lfn++;
				pmtout.write(pmtMPA);
				break;

			case 2:    
				if ((0xFF & Pid) < 0x40) // sup
				{
					pmtSUP[2] = (byte)(0xFF & Pid);
					pmtSUP[7] = (byte)lfn++;
					pmtout.write(pmtSUP);
				}
				else  // ac3, dts audio
				{
					pmtAC3[2] = (byte)(0xFF & Pid);
					pmtAC3[7] = (byte)lfn++;
					pmtout.write(pmtAC3);

					// ac3_atsc addition, same values
					pmtAC3_atsc[2] = (byte)(0xFF & Pid);
					pmtAC3_atsc[7] = (byte)(lfn - 1);
					pmtout.write(pmtAC3_atsc);
				}

				break;

			case 3:    // ttx
				pmtTTX[2] = (byte)(0xFF & Pid);
				pmtTTX[7] = (byte)lfn++;
				pmtout.write(pmtTTX);
				break;
			}
		}

		byte newpmt[] = pmtout.toByteArray();
		int sectionlen = newpmt.length;
		newpmt[2] = (byte)(0xB0 | (0xF & sectionlen>>8));
		newpmt[3] = (byte)(0xFF & sectionlen);

		pmtout.reset();
		pmtout.write(newpmt);
		pmtout.write(generateCRC32(newpmt, 1)); 

		newpmt = pmtout.toByteArray();

		int pmtpacks = ((newpmt.length - 1) / 184) + 1; // = number of needed pmt packs
		autopmt = new byte[pmtpacks * 188];

		Arrays.fill(autopmt, (byte)0xff);

		int i = 0, c = 0;
		while (i < newpmt.length)
		{
			System.arraycopy(pmtHead, 0, autopmt, c, 4);

			if (newpmt.length >= i+184)
			{ 
				System.arraycopy(newpmt, i, autopmt, 4 + c, 184);
				i += 184; 
				c += 188;
			}
			else
			{
				System.arraycopy(newpmt, i, autopmt, 4 + c, newpmt.length - i);
				break;
			}
		}

		autopmt[1] = 0x41; // = set startpacket bit
		pmtout.close();
	}

	// auto PMT 
	public static byte[] getAutoPMT()
	{ 
		for (int i=0; i < autopmt.length; i+=188) 
			autopmt[i+3] = (byte)(0x10 | (0xf&(count1++)));

		return autopmt;
	}

	public static int getfirstID()
	{ 
		return firstID; 
	}

	//DM09082004 081.7 int08 changed
	public static void setfirstID()
	{ 
		firstID = 0xE0; 
		pmtPCR[2] = (byte)firstID; 
		updateHeader(23, firstID);
	}

	// PAT with section 0 and SID = 0x102, PMT = 0x100 , CRC32
	private static byte[] pat1 = {
		0x47,0x40,0,0x10,
		0, 0, (byte)0xB0, 0xd, 0, 1, 1, 0, 0, 1, 2, (byte)0xE1, 0,
		(byte)0x8f, (byte)0xa5, 0x26, (byte)0xcf,
	};

	// counter shall not be updated in PCR only paket (but do it), 42bits for PCR
	private static byte pcr1[] = {
		0x47,0,(byte)0xe0,0x20,
		(byte)0xB7,0x10,0,0,0,0,0,0
	};
/**
	private static byte ttx[] = {
		0x47,0x40,(byte)0x9F,0x10,
		0,0,1,(byte)0xBD,0,(byte)0xB2,(byte)0x84,(byte)0x80,0x24,
		(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,
		(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,
		(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,
		(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,
		(byte)0xFF,
		0x10,
		// 022C E7E4 40A8 A8CE A80B A80B 7A40
		2,44,-25,-28,64,-88,-88,-50,-88,11,-88,11,122,64,
		38,-50,117,87,-122,79,4,42,-53,-75,-110,118,103,-9,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,
		// 022C E8E4 40D9
		2,44,-24,-28,64,-39,
		//    l  i n  e    2 2  :
		4,55,-105,118,-89,4,76,76, 4,107,67,-110,4,-116,-12,28,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,
		// 022C E9E4 E3D9
		2,44,-23,-28,-29,-39,
		//    line 23 PTS
		4,55,-105,118,-89,4,76,-51, 4,107,67,-110,4,-116,-12,-99,4,11,42,-53,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4
	};

	//DM26052004 081.7 int03 changed
	public static byte[] getTTX(byte[] data, int offset, String pts)
	{
		byte[] tPTS = pts.getBytes();

		for (int a = 0; a < tPTS.length; a++) 
			tPTS[a] = Common.getTeletextClass().bytereverse(Common.getTeletextClass().parity(tPTS[a]));

		System.arraycopy(tPTS, 0, ttx, 169, tPTS.length);
		System.arraycopy(data, 9 + offset, ttx, 13, 5);

		ttx[13] &= ~0x10;
		ttx[3] = (byte)(0x10 | (0xF & (count3++)));

		return ttx;
	}
**/

//////////
	private static byte[] ttx_stream = null;
	private static long[] ttx_pts_index = null;
	private static int ttx_index = 0;

	/**
	 * read .sub text file and create complete TS TTX stream
	 * from 0.90.4.00b28
	 */
	public static byte[] getTeletextStream(long video_pts)
	{
		if (ttx_pts_index == null)
			return (new byte[0]);

		ByteArrayOutputStream bo = new ByteArrayOutputStream();

		for (int j = ttx_pts_index.length; ttx_index < j; ttx_index++)
		{
			if (video_pts < ttx_pts_index[ttx_index])
				break;

			//pts_delta, ttx_pts shall match video_pts at 40ms (3600ticks) boundary
			video_pts -= ((video_pts - ttx_pts_index[ttx_index]) / 3600L) * 3600L;

			CommonParsing.setPES_PTSField(ttx_stream, 4 + ttx_index * 376, video_pts);
			CommonParsing.setPES_PTSField(ttx_stream, 4 + 188 + ttx_index * 376, video_pts);
			
			bo.write(ttx_stream, ttx_index * 376, 376);
		}

		return bo.toByteArray();
	}

	/**
	 * read .sub text file and create complete TS TTX stream
	 * from 0.90.4.00b28
	 */
	public static void buildTeletextStream(String filename)
	{
		filename = filename.substring(0, filename.lastIndexOf(".")) + ".sub";
		File f = new File(filename);
		ttx_index = 0;

		if (!f.exists())
			return;

		try {

			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
			ByteArrayOutputStream bo = new ByteArrayOutputStream();

			String line = "", tmp = "";
			ArrayList rowList = new ArrayList();
			int pos1 = 0, pos2 = 0;
			long[] time = { 0, 0, 0 };
			byte[] pts_value = new byte[5];
			byte[] pts_value1 = new byte[8];
			StringTokenizer st;
			ArrayList indexList = new ArrayList();
			long delay = 90L * Common.getSettings().getIntProperty("TTXInsertion.Delay", 0);
			long[] framenumber = { 0, 0, 0 };

			Common.setMessage("-> build teletext stream from file: '" + filename + "' / delay = " + (delay / 90) + " ms" );

			Common.getTeletextClass().setMappingTable();

			while ((line = br.readLine()) != null)
			{
				//if (!line.startsWith("{")) //problems with file signature  reading utf files - omits a line
				//	continue;

				framenumber[0] = Long.parseLong(line.substring(pos1 = (line.indexOf("{") + 1), pos2 = line.indexOf("}")));
				framenumber[1] = Long.parseLong(line.substring(line.indexOf("{", ++pos1) + 1, pos2 = line.indexOf("}", ++pos2)));

				time[0] = delay + 90L * (1000/25) * framenumber[0];
				time[1] = delay + 90L * (1000/25) * framenumber[1];

				st = new StringTokenizer(line.substring(pos2 + 1), "|");

				rowList.clear();

				while (st.hasMoreTokens())
					rowList.add(st.nextToken());

				// every page consists of 376 (2x188) byte
				for (int i = 0; i < 2; i++, framenumber[2]++)
				{
/**
					// insert padding packets for every frame
					for (; framenumber[2] < framenumber[i]; framenumber[2]++)
					{
						Arrays.fill(pts_value, (byte) 0); // clear old value

						time[2] = delay + 90L * (1000/25) * framenumber[2];
						CommonParsing.setPES_PTSField(pts_value, -9, time[2]);

						bo.write(Common.getTeletextClass().getTTXPadding_TSPacket(count3, pts_value));

						indexList.add(new Long(time[2]));

						count3 += 2;
					}
**/
					Arrays.fill(pts_value, (byte) 0); // clear old value
					CommonParsing.setPES_PTSField(pts_value, -9, time[i]);

					bo.write(Common.getTeletextClass().getTTX_TSPacket(rowList, count3, pts_value));

					indexList.add(new Long(time[i]));

					count3 += 2;
					rowList.clear(); //2nd call for time out (placing an empty page)
				}
			}

			br.close();
/**
			FileOutputStream fos = new FileOutputStream(filename + ".ttx");
			fos.write(bo.toByteArray());
			fos.flush();
			fos.close();
**/
			ttx_stream = bo.toByteArray();

			ttx_pts_index = new long[indexList.size()];
			for (int i = 0, j = ttx_pts_index.length; i < j; i++)
				ttx_pts_index[i] = ((Long) indexList.get(i)).longValue();

		} catch (Exception e) {
			Common.setExceptionMessage(e);
		}
	}
/////////////

	/*
	 *
	 */
	public static byte[] getPMT()
	{ 
		pmt[3] = (byte)(0x10 | (0xf & (count1++))); 

		return pmt; 
	}

	public static byte[] getPAT()
	{ 
		pat[3] = (byte)(0x10 | (0xf & (count2++))); 

		return pat; 
	}

	public static byte[] getPCR(long pts, int count, int PCRPid)
	{
		/* Construct the PCR, PTS-55000 (2ms) 1Bit ~ 3Ticks (counter) */
		pcr[2] = (byte)(PCRPid);
		pcr[3] = (byte)(0x20 | (0xF & count));
		pcr[6] = (byte)(0xFF & pts>>>25);
		pcr[7] = (byte)(0xFF & pts>>>17);
		pcr[8] = (byte)(0xFF & pts>>>9);
		pcr[9] = (byte)(0xFF & pts>>>1);
		pcr[10] = (byte)((1 & pts) << 7 );
		/* PCR ext is 0, byte10+byte11 */

		return pcr;
	}

	/**
	 * init additional header
	 * copy pmt entries
	 */
	public static byte[] init( JobProcessing job_processing, String name, boolean ac3, boolean _myTTX, int mode)
	{ 
		count1 = count2 = count3 = 0;
		myTTX = _myTTX;

		Arrays.fill(pat, (byte) 0xFF);
		Arrays.fill(pmt, (byte) 0xFF);
		Arrays.fill(pcr, (byte) 0xFf);

		System.arraycopy(pmt1, 0, pmt, 0, pmt1.length);
		System.arraycopy(pat1, 0, pat, 0, pat1.length);
		System.arraycopy(pcr1, 0, pcr, 0, pcr1.length);

		switch (mode)
		{
		case 1:
			return initTFHeader(TF4000header, 564, name, ac3, mode);

		case 2:
			return initTFHeader(TF5000header, 1692, name, ac3, mode); //fmly 1316

		case 3:
			return initTFHeader(TF5000header, 3760, name, ac3, mode);

		case 4:
			return initTFHeader(TF5200header, 3760, name, ac3, mode);

		case 5:
			return initArionHeader(name, ac3, ArionAVR_HEADERSIZE , job_processing);
		}

		return (new byte[0]);
	}

	/**
	 * init topfield header
	 * set main audio
	 */
	private static byte[] initTFHeader(byte[] header, int headerlength, String name, boolean ac3, int mode)
	{
		byte newheader[] = new byte[headerlength];
		System.arraycopy(header, 0, newheader, 0, header.length);
/**
		byte file_name[] = new File(name).getName().getBytes();
		header[75] = (byte)(file_name.length - 3);
		System.arraycopy(file_name, 0, newheader, 76, file_name.length - 3);
**/
	//	primary_audio_pid = !ac3 ? 0xC0 : 0x80;

		newheader[26] = 0;
		newheader[27] = !ac3 ? (byte)0xC0 : (byte)0x80; //MPA // set 1. AC3 PID as main TFaudio

		return newheader;
	}

	/**
	 * sets PID values
	 */
	private static void updateHeader(int pos, int val)
	{
		//only last 8 bits used
		TF4000header[pos] = (byte) val;
		TF5000header[pos] = (byte) val;
		TF5200header[pos] = (byte) val;
	}

	public static String updateAdditionalHeader(String old_name, long time[], int mode, JobProcessing job_processing) throws IOException
	{
		String new_name = "";
		String[] new_ext = { "", ".raw", ".rec", ".rec", ".rec", ""};

		switch (mode)
		{
		case 0:
			return old_name;

		case 1:
		case 2:
		case 3:
		case 4:
			new_name = old_name.substring(0, old_name.length() - 3) + new_ext[mode];

			if (new File(new_name).exists()) 
				new File(new_name).delete();

			Common.renameTo(old_name, new_name);

			finishTFHeader(new_name, time, mode);
			break;

		case 5:
			new_name = finishArionheaders(old_name, time, job_processing);

			break;
		}

		return new_name;
	}

	/**
	 * save event info 
	 */
	public static void setEventInfo(byte[] data1, byte[] data2, byte[] data3)
	{
		if (data1 == null)
			service_name = null;
		else
		{
			service_name = new byte[data1.length];
			System.arraycopy(data1, 0, service_name, 0, data1.length);
		}

		if (data2 == null)
			event_name = null;
		else
		{
			event_name = new byte[data2.length];
			System.arraycopy(data2, 0, event_name, 0, data2.length);
		}

		if (data3 == null)
			event_text = null;
		else
		{
			event_text = new byte[data3.length];
			System.arraycopy(data3, 0, event_text, 0, data3.length);
		}
	}

	/**
	 * completes Topfield header
	 */
	private static void finishTFHeader(String name, long time[], int mode)
	{
		long event[] = new long[4];
		long millis = (time[1] - time[0]) / 90L;
		short minutes = (short)(0xFFFF & (Math.round(millis / 60000f)));

		event[0] = System.currentTimeMillis();
		event[1] = event[0] - millis;

		//JD 2440588  1.1.1970  = 0
		//24*60*60*1000 
		event[2] = (event[0] / 86400000L) + 2440588 - 2400001; 
		event[3] = (event[1] / 86400000L) + 2440588 - 2400001;

		Calendar datum = Calendar.getInstance();
		datum.setTime(new Date(event[0]));

		switch (mode)
		{
		case 1:
			finishTF4000header(name, time, minutes, event, datum);
			break;
		case 2:
		case 3:
			finishTF5X00header(name, time, minutes, event, datum, 4);
			break;
		case 4:
			finishTF5X00header(name, time, minutes, event, datum, 0);
		}

	}

	/**
	 * completes Topfield 4000 header
	 */
	private static void finishTF4000header(String name, long time[], short minutes, long[] event, Calendar datum)
	{
		try {

			RandomAccessFile ts = new RandomAccessFile(name, "rw");

			ts.seek(0);
			ts.writeShort((short)event[2]);
			ts.writeByte((byte)datum.get(Calendar.HOUR_OF_DAY));
			ts.writeByte((byte)datum.get(Calendar.MINUTE));
			ts.writeShort((short)event[2]);
			ts.writeByte((byte)datum.get(Calendar.HOUR_OF_DAY));
			ts.writeByte((byte)datum.get(Calendar.MINUTE));
			ts.writeShort(minutes);

			ts.seek(0x44);
			ts.writeShort((short)event[2]);
			ts.writeByte((byte)datum.get(Calendar.HOUR_OF_DAY));
			ts.writeByte((byte)datum.get(Calendar.MINUTE));
			ts.writeShort(minutes);

			datum.setTime(new Date(event[1]));

			ts.seek(0x40);
			ts.writeShort((short)event[3]);
			ts.writeByte((byte)datum.get(Calendar.HOUR_OF_DAY));
			ts.writeByte((byte)datum.get(Calendar.MINUTE));

			ts.close();

		} catch (Exception e) {

			Common.setExceptionMessage(e);
		}
	}

	/**
	 * completes Topfield 5X00 header
	 */
	//introduced by 'catapult' 09082004
	//dvb-c mod's jkit 23012009
	private static void finishTF5X00header(String name, long time[], short minutes, long[] event, Calendar datum, int event_info_offset)
	{
		try {
			RandomAccessFile ts = new RandomAccessFile(name, "rw");

			ts.seek(0x08);
			ts.writeShort(minutes); 

			if (service_name != null)
			{
				ts.seek(0x1C);
				ts.write(service_name); 
			}

			ts.seek(0x42 + event_info_offset);
			ts.writeShort(minutes);

			ts.seek(0x4C + event_info_offset);
			ts.writeShort((short)event[2]);
			ts.writeByte((byte)datum.get(Calendar.HOUR_OF_DAY));
			ts.writeByte((byte)datum.get(Calendar.MINUTE));

			datum.setTime(new Date(event[1]));
			ts.seek(0x48 + event_info_offset);
			ts.writeShort((short)event[3]); // datum
			ts.writeByte((byte)datum.get(Calendar.HOUR_OF_DAY));
			ts.writeByte((byte)datum.get(Calendar.MINUTE));   

			if (event_name != null)
			{
				ts.seek(0x51 + event_info_offset);
				ts.write(event_name); 
			}

			else
			{
				String eventname = new File(name).getName();

				if (eventname.length() > 128)
					eventname = eventname.substring(0, 128);

				ts.seek(0x51 + event_info_offset);
				ts.writeUTF(eventname); //filename

				ts.seek(0x52 + event_info_offset);
				int val = ts.read();

				ts.seek(0x51 + event_info_offset);
				ts.writeShort(val<<8);
			}

			if (event_text != null)
			{
				ts.seek(0xE0 + event_info_offset);
				ts.write(event_text); 
			}

			ts.close();

		} catch (Exception e) {

			Common.setExceptionMessage(e);
		}

		setEventInfo(null, null, null); //reset
	}

	private static byte[] initArionHeader(String name, boolean ac3, int headerlength, JobProcessing job_processing)
	{

		int splitPart = job_processing.getSplitPart();

		if ( splitPart == 0 )
		{
			/*
			 * this is the first part, so build the AVF info file as well as the AVR header
			 */
			long now = System.currentTimeMillis();		// time now

			Calendar datum = Calendar.getInstance();
			datum.setTime( new Date(now) );

			ArionAVFPathname_onPVR.setLength( 0 );
			ArionAVFTime.setLength( 0 );
			ArionFilenameRoot.setLength( 0 );
			ArionCumulativeTime  = 0L;
			ArionCumulativeStreamSize  = 0L;

		//	ArionAVFTime.append( String.format( "%1$tY%1$tm%1$td%1$tH%1$tM", now ) );
			ArionAVFTime.append(Common.formatTime_5(now));

			// ProjectX automatically appends (<partnumber>).ts to the filename.
			// we want to remove those bits and any leading path component.
			// The file will be renamed at the end (in the finishArionHeader routine),
			// we just save the details for now. 
			ArionFilenameRoot.append( new File(name).getName() );	// start with leading path removed
			int parenIndex = ArionFilenameRoot.toString().lastIndexOf( "(" );
			if ( parenIndex > 0 )
				ArionFilenameRoot.delete( parenIndex, ArionFilenameRoot.length() );

			/*
			 * now check for total length of file name:
			 * "C:\PVR\AV\"		= 10
			 * "_#001_"		= 6
			 * "yyyymmddhhmm"	= 12
			 * ".AVR"		= 4
			 *  total 32. Max allowed is 125 plus null terminator.
			 */
			if ( ArionFilenameRoot.length() > 93 )
				ArionFilenameRoot.setLength( 93 );

			String avf_name = new String(	ArionFilenameRoot.toString( ) +
							"_#001_" +
							ArionAVFTime.toString( ) +
				       			".avf"	);

			ArionAVFPathname_onPVR.append( "C:\\PVR\\AV\\" + avf_name.toUpperCase() );
			StringBuffer arionAVRPathname_onPVR = new StringBuffer( ArionAVFPathname_onPVR.toString() );
			// full DOS-style pathname is only used in AVF header.
			arionAVRPathname_onPVR.setCharAt( arionAVRPathname_onPVR.length()-1, 'R' );
			// now create AVF file in same place as new avr file.

			ArionAVFLocalPathname = new File( new File(name).getParentFile(), avf_name );

			try {
				Common.setMessage("Arion: Creating initial AVF file");
				RandomAccessFile avf_fd = new RandomAccessFile(ArionAVFLocalPathname, "rw");
				byte avf_header[] = new byte[ ArionAVF_HEADERSIZE ];
				System.arraycopy(ArionAVF_Header , 0, avf_header, 0, ArionAVF_Header.length);
				System.arraycopy(arionAVRPathname_onPVR.toString().getBytes() , 0,
								avf_header, 0x326, arionAVRPathname_onPVR.length());

				avf_header[0x1B] = 0x02;
				avf_header[0x1C] = (byte) 0x80;
				avf_header[0x1D] = !ac3 ? (byte)0xC0 : (byte)0x80; //MPA // set 1. AC3 PID as main audio
				avf_header[0x1E] = 0x00;
				avf_header[0x1F] = ac3 ? (byte)0xC0 : (byte)0x80; //MPA // set 1. AC3 PID as main audio

				// now do the time/date for EPG and recording info.
				// since we have no meaningful information we will just use current time
				// for both sets of data.
				short year = (short) datum.get( datum.YEAR );
				byte month = (byte) datum.get( datum.MONTH );
				byte day = (byte) datum.get( datum.DAY_OF_MONTH );
				byte hour = (byte) datum.get( datum.HOUR_OF_DAY );
				byte minute = (byte) datum.get( datum.MINUTE );
				avf_header[0x1f48] = 0x1b;
				avf_header[0x1f49] = (byte)0xee;
				avf_header[0x1f4a] = (byte)(year >>> 8 );
				avf_header[0x1f4b] = (byte)(year & 0xff);
				avf_header[0x1f4c] = month;
				avf_header[0x1f4d] = day;
				avf_header[0x1f4e] = hour;
				avf_header[0x1f4f] = minute;
				avf_header[0x1f50] = 1;			// one EPG entry.

				byte epg_entry[] = new byte[ ArionEPG_BLOCKSIZE ];
				System.arraycopy(Arion_EPG_Block , 0, epg_entry, 0, Arion_EPG_Block.length);
				epg_entry[2] = (byte)(year >>> 8 );
				epg_entry[3] = (byte)(year & 0xff);
				epg_entry[4] = month;
				epg_entry[5] = day;
				epg_entry[6] = hour;
				epg_entry[7] = minute;
				// assume the filename is useful as a program name.
				System.arraycopy(ArionFilenameRoot.toString().getBytes() , 0,
							epg_entry, 10, ArionFilenameRoot.length() );

				// first copy into the "epg at start" area
				System.arraycopy(epg_entry , 0, avf_header, 0x5c, epg_entry.length);
				// then create the EPG table - with a single entry.
				System.arraycopy(epg_entry , 0, avf_header, 0x1f52, epg_entry.length);

				avf_fd.write( avf_header);
				avf_fd.close( );

			} catch ( IOException e ) {
				Common.setExceptionMessage(e);
			}
		}
		/* create the local AVR filename for use in the finish.
		 */
		ArionAVRLocalFilename.setLength( 0 );
//		ArionAVRLocalFilename.append(ArionFilenameRoot.toString() +	String.format( "_#%03d_", splitPart+1) + ArionAVFTime.toString( ) + ".avr");
		ArionAVRLocalFilename.append(ArionFilenameRoot.toString() +	"_#" + Common.adaptString(splitPart + 1, 3) + "_" + ArionAVFTime.toString() + ".avr");

		// Now create the AVR header, populate, and return it to caller.
		byte header[] = new byte[headerlength];
		System.arraycopy(ArionAVR_Header , 0, header, 0, ArionAVR_Header.length);

		// copy the AVF filename into the AVR file header.
		System.arraycopy( ArionAVFPathname_onPVR.toString().getBytes(), 0, header, 10, ArionAVFPathname_onPVR.length() );


		return header;
	}

	/*
	 * called each time a file part has been written and been closed.
	 * Need to:
	 * rename AVR file to Arion style.
	 * add entries to AVF header for:
	 * size of this file, number of file parts, bitrate,
	 * total playback time (minutes), total size of stream (redundant, but...)
	 */
	// todo--- PIDs, file sizes, and total size.; bitrate.
	private static String finishArionheaders(String old_name, long time[], JobProcessing job_processing )	 throws IOException
	{
			// duration of recording - convert from 90kHz clock to ms.
		long millis = (time[1] - time[0]) / 90L;
		long duration_seconds = Math.round( millis / 1000.0f );    //total duration in seconds
		ArionCumulativeTime += duration_seconds;
		
		long duration_minutes = Math.round( ArionCumulativeTime / 60.0f );    //total duration in minutes

		int splitPart = job_processing.getSplitPart();

		File avrFile = new File( new File(old_name).getParentFile(), ArionAVRLocalFilename.toString() );
		if ( avrFile.exists() ) 
			 avrFile.delete();

		Common.renameTo( new File(old_name), avrFile);

		// now get the AVR file  length
		//RandomAccessFile ts = new RandomAccessFile(avrFile, "rw");
		long currentStreamSize = avrFile.length() - ArionAVR_HEADERSIZE;
		ArionCumulativeStreamSize  += currentStreamSize;
		//ts.close();

		if ( currentStreamSize > 0x7f9a0000 ) {
			// not sure how to abort this cleanly.
			Common.setMessage("!> Arion: File is too large " + String.valueOf(avrFile.length()) + " bytes)");
			return (new String( "noFileCreated" ) );
		}

		// now reopen the AVF file and update the timing info...
		RandomAccessFile ts = new RandomAccessFile( ArionAVFLocalPathname, "rw");

		Common.setMessage("Arion: renamed '" + old_name + "'");
		Common.setMessage("            to '" + avrFile.getPath() + "'");
		Common.setMessage("    size 0x" + Long.toHexString(currentStreamSize).toUpperCase() + ", duration " + String.valueOf(duration_seconds) + " sec (cumulative: " + String.valueOf(duration_minutes) + " min)");

		int byterate = (int) (ArionCumulativeStreamSize / ArionCumulativeTime);
		Common.setMessage("Arion: bitrate = " + String.valueOf(byterate) + " bytes/s");

		// write the nth stream size ...
		ts.seek(0x3a4 + 4 * splitPart  );
		ts.writeInt( (int)currentStreamSize );


		ts.seek(0x6c4);
		ts.writeInt( byterate );
		ts.writeInt( (int)duration_minutes );
		ts.writeShort( splitPart+1 );
		ts.seek(0x6d0);
		ts.writeLong( ArionCumulativeStreamSize );
		Common.setMessage("Arion: Written " + String.valueOf(ArionCumulativeStreamSize) + " bytes");

		ts.close();

		return old_name;
	}


	private static byte[] generateCRC32(byte[] data, int offset)
	{
		// x^32 + x^26 + x^23 + x^22 + x^16 + x^12 + x^11 + x^10 + x^8 + x^7 + x^5 + x^4 + x^2 + x + 1
		int[] g = { 1,1,1,0,1,1,0,1,1,0,1,1,1,0,0,0,1,0,0,0,0,0,1,1,0,0,1,0,0,0,0,0,1 }; 

		int[] shift_reg = new int[32];
		long crc = 0;
		byte[] crc32 = new byte[4];

		// Initialize shift register's to '1'
		Arrays.fill(shift_reg, 1);

		// Calculate nr of data bits, summa of bits
		int nr_bits = (data.length - offset) * 8;

		for (int bit_count = 0, bit_in_byte = 0, data_bit; bit_count < nr_bits; bit_count++)
		{
			// Fetch bit from bitstream
			data_bit = (data[offset] & 0x80>>>(bit_in_byte++)) != 0 ? 1 : 0;

			if ((bit_in_byte &= 7) == 0)
				offset++;

			// Perform the shift and modula 2 addition
			data_bit ^= shift_reg[31];

			for (int i = 31; i > 0; i--)
				shift_reg[i] = g[i]==1 ? (shift_reg[i - 1] ^ data_bit) : shift_reg[i - 1];

			shift_reg[0] = data_bit;
		}

		for (int i = 0; i < 32; i++)
			crc = ((crc << 1) | (shift_reg[31 - i]));

		for (int i = 0; i < 4; i++) 
			crc32[i] = (byte)(0xFF & (crc >>>((3 - i) * 8)));

		return crc32;
	}

}