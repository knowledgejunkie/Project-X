/*
 * @(#)TS.java - constants to create TS packets
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

package net.sourceforge.dvb.projectx.thirdparty;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import net.sourceforge.dvb.projectx.audio.CRC;
import net.sourceforge.dvb.projectx.common.Common;
import net.sourceforge.dvb.projectx.common.Resource;
import net.sourceforge.dvb.projectx.common.X;
import net.sourceforge.dvb.projectx.subtitle.Teletext;

public class TS
{

	//DM14062004 081.7 int04 changed
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
		// HEADER 14 bytes
		0x54, 0x46, 0x72, 0x63,  // Id "TFrc" *
		0x50, 0,                 // Version  *
		0, 0,                    // Reserved *
		0, 0,                    // Duration in Minutes
		0, 0xa,                  // Service number in channel list (Does not matter in playback)
		0, 0,                    // Service type 0:TV 1:Radio

		// SERVICE_INFO 38 bytes starts at 15
		0, 0, 1, 0x30,           //  Reserved and Tuner (Does not matter in playback) (Tuner 1+2 flagged)
		1, 2,                    //  Service ID of TS stream 
		1, 0,                    //  PID number of PMT TS packet
		0, (byte)0xe0,           //  PID number of PCR TS packet
		0, (byte)0xe0,           //  PID number of Video TS packet  
		0, (byte)0xc0,           //  PID number of Audio TS packet, MPA as std

		0x4D,0x79,0x20,0x70,0x65,0x72,0x73,0x6F,0x6E,0x61,0x6C,0x20,0x54,0x56,0x20,0x43,0x68,0x61,0x6E,0x6E,0x65,0x6C,0,0, // File Name

		// TP_INFO 16 bytes starts at 53
		0,                       //  Satelite Index
		8, 7, 0,                 //  Polarity and Reserved (Does not matter in playback)
		0x6b, 0x6c, 0, 1,        //  Frequency  (Does not matter in playback)
		0x40, 0x1f,              //  Symbol Rate  (Does not matter in playback)
		1, 1,                    //  Transport Stream Id (Does not matter in playback)
		0, 0, 0, 0,              //  Reserved *

		// EVT_INFO 160 bytes starts at 69
		(byte)0xCD, 0x39,       //  Reserved *
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

		// EXT_EVT_INFO 1088 bytes starts at 229 
		// Extended Event text

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
		(byte)0x85, (byte)0x33, (byte)0x49, (byte)0x7e
	};

	private int count1=0, count2=0, count3=0;

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

	private static byte[] pmtTTX =  { 6, (byte)0xE0, (byte)0x90, (byte)0xF0, 0x1F, 0x52, 1, 5, 0x56, 20, 0x65, 0x6E, 0x67, 0x9, 0, 0x64, 0x65, 0x75, 0x11, 0x50, 0x67, 0x65, 0x72, 0x17, 0x77, 0x65, 0x6E, 0x67, 0x10, (byte)0x88, 0xA, 4, 0x64, 0x65, 0x75, 0 };
	private static byte[] autopmt = new byte[0];

	private static int firstID = 0xE0;
	private static boolean myTTX=false;

	//DM09082004 081.7 int08 changed
	public void setPmtPids(java.util.ArrayList PIDs) throws IOException
	{
		if (myTTX) 
			PIDs.add("" + 0x39F);

		Object[] Pids = PIDs.toArray();

		if (Pids.length == 0)
		{
			X.Msg(Resource.getString("ts.msg1"));
			autopmt = pmt;

			return;
		}

		ByteArrayOutputStream pmtout = new ByteArrayOutputStream();

		java.util.Arrays.sort(Pids);

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
				pmtMPV[2] = (byte)(0xFF&Pid);
				pmtMPV[7] = (byte)lfn++;
				pmtout.write(pmtMPV);
				break;

			case 1:    // mpeg-1 (-2) audio
				pmtMPA[2] = (byte)(0xFF&Pid);
				pmtMPA[7] = (byte)lfn++;
				pmtout.write(pmtMPA);
				break;

			case 2:    // ac3 audio
				pmtAC3[2] = (byte)(0xFF&Pid);
				pmtAC3[7] = (byte)lfn++;
				pmtout.write(pmtAC3);

				// ac3_atsc addition, same values
				pmtAC3_atsc[2] = (byte)(0xFF & Pid);
				pmtAC3_atsc[7] = (byte)(lfn - 1);
				pmtout.write(pmtAC3_atsc);

				break;

			case 3:    // ttx
				pmtTTX[2] = (byte)(0xFF&Pid);
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
		pmtout.write(CRC.generateCRC32(newpmt, 1)); //DM10042004 081.7 int01 changed

		newpmt = pmtout.toByteArray();

		int pmtpacks = ((newpmt.length - 1) / 184) + 1; // = number of needed pmt packs
		autopmt = new byte[pmtpacks * 188];

		java.util.Arrays.fill(autopmt, (byte)0xff);

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
	public byte[] getAutoPmt()
	{ 
		for (int i=0; i < autopmt.length; i+=188) 
			autopmt[i+3] = (byte)(0x10 | (0xf&(count1++)));

		return autopmt;
	}

	public int getfirstID()
	{ 
		return firstID; 
	}

	//DM09082004 081.7 int08 changed
	public void setfirstID()
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
	public byte[] getTTX(byte[] data, String pts)
	{
		byte[] tPTS = pts.getBytes();

		for (int a=0; a < tPTS.length; a++) 
			tPTS[a] = Teletext.bytereverse(Teletext.parity(tPTS[a]));

		System.arraycopy(tPTS, 0, ttx, 169, tPTS.length);
		System.arraycopy(data, 9, ttx, 13, 5);

		ttx[13] &= ~0x10;
		ttx[3] = (byte)(0x10 | (0xF & (count3++)));

		return ttx;
	}

	public byte[] getPmt()
	{ 
		pmt[3] = (byte)(0x10 | (0xf & (count1++))); 

		return pmt; 
	}

	public byte[] getPat()
	{ 
		pat[3] = (byte)(0x10 | (0xf & (count2++))); 

		return pat; 
	}

	public byte[] getPCR(long pts, int count, int PCRPid)
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

	//DM09082004 081.7 int08 changed
	public byte[] init(String name, boolean ac3, boolean myTTX, int mode)
	{ 
		count1 = count2 = count3 = 0;
		TS.myTTX = myTTX;

		java.util.Arrays.fill(pat, (byte)0xff);
		java.util.Arrays.fill(pmt, (byte)0xff);
		java.util.Arrays.fill(pcr, (byte)0xff);

		System.arraycopy(pmt1, 0, pmt, 0, pmt1.length);
		System.arraycopy(pat1, 0, pat, 0, pat1.length);
		System.arraycopy(pcr1, 0, pcr, 0, pcr1.length);

		switch (mode)
		{
		case 1:
			return initTF4000header(name, ac3);

		case 2:
			return initTF5000header(name, ac3);

		}

		return (new byte[0]);
	}

	//DM09082004 081.7 int08 outsourced from X ++
	private void updateHeader(int pos, int val)
	{
		//only last 8 bits used
		TF4000header[pos] = (byte)val;
		TF5000header[pos] = (byte)val;
	}

	public static String updateAdditionalHeader(String old_name, long time[], int mode) throws IOException
	{
		String new_name = "";

		switch (mode)
		{
		case 0:
			return old_name;

		case 1:
			new_name = old_name.substring(0, old_name.length() - 3) + ".raw";

			if (new File(new_name).exists()) 
				new File(new_name).delete();

			Common.renameTo(old_name, new_name); //DM13042004 081.7 int01 changed

			finishTF4000header(new_name, time);
			break;

		case 2:
			new_name = old_name.substring(0, old_name.length() - 3) + ".rec";

			if (new File(new_name).exists()) 
				new File(new_name).delete();

			Common.renameTo(old_name, new_name);

			finishTF5000header(new_name, time);
			break;
		}

		return new_name;
	}

	private static void finishTF4000header(String name, long time[]) throws IOException
	{
		long event[] = new long[4];
		long millis = (time[1] - time[0]) / 90L;
		short minutes = (short)(0xFFFF & (Math.round(millis / 60000f)));

		event[0] = System.currentTimeMillis();
		event[1] = event[0] - millis;
		event[2] = (event[0] / 86400000L) + 40587;
		event[3] = (event[1] / 86400000L) + 40587;

		java.util.Calendar datum = java.util.Calendar.getInstance();
		datum.setTime(new java.util.Date(event[0]));

		RandomAccessFile ts = new RandomAccessFile(name, "rw");

		ts.seek(0);
		ts.writeShort((short)event[2]);
		ts.writeByte((byte)datum.get(11));
		ts.writeByte((byte)datum.get(12));
		ts.writeShort((short)event[2]);
		ts.writeByte((byte)datum.get(11));
		ts.writeByte((byte)datum.get(12));
		ts.writeShort(minutes);

		ts.seek(0x44);
		ts.writeShort((short)event[2]);
		ts.writeByte((byte)datum.get(11));
		ts.writeByte((byte)datum.get(12));
		ts.writeShort(minutes);

		datum.setTime(new java.util.Date(event[1]));

		ts.seek(0x40);
		ts.writeShort((short)event[3]);
		ts.writeByte((byte)datum.get(11));
		ts.writeByte((byte)datum.get(12));

		ts.close();
	}

	//introduced by 'catapult' 09082004
	private static void finishTF5000header(String name, long time[]) throws IOException
	{
		long event[] = new long[4];
		long millis = (time[1] - time[0]) / 90L;
		short minutes = (short)(0xFFFF & (Math.round(millis / 60000f)));

		event[0] = System.currentTimeMillis();
		event[1] = event[0] - millis;
		event[2] = (event[0] / 86400000L) + 40587;
		event[3] = (event[1] / 86400000L) + 40587;

		java.util.Calendar datum = java.util.Calendar.getInstance();
		datum.setTime(new java.util.Date(event[0]));

		RandomAccessFile ts = new RandomAccessFile(name, "rw");

		ts.seek(0x08);
		ts.writeShort(minutes); 

		ts.seek(0x46);
		ts.writeShort(minutes);

		ts.seek(0x50);
		ts.writeShort((short)event[2]);
		ts.writeByte((byte)datum.get(11));
		ts.writeByte((byte)datum.get(12));

		datum.setTime(new java.util.Date(event[1]));

		ts.seek(0x4c);
		ts.writeShort((short)event[3]);
		ts.writeByte((byte)datum.get(11));
		ts.writeByte((byte)datum.get(12));   

		ts.close();
	}

	private byte[] initTF4000header(String name, boolean ac3)
	{
		byte header[] = new byte[564]; //TF4000
		System.arraycopy(TF4000header, 0, header, 0, TF4000header.length);

		byte file_name[] = new File(name).getName().getBytes();
		header[75] = (byte)(file_name.length - 3);
		System.arraycopy(file_name, 0, header, 76, file_name.length - 3);

		if (ac3)
		{
			header[26] = 0; 
			header[27] = (byte)0x80;  // set 1. AC3 PID as main TFaudio
		}
		else
		{
			header[26] = 0; //DM14062004 081.7 int04 changed
			header[27] = (byte)0xC0; 
		}

		return header;
	}

	private byte[] initTF5000header(String name, boolean ac3)
	{
		byte header[] = new byte[1316]; //TF5000
		System.arraycopy(TF5000header, 0, header, 0, TF5000header.length);

		byte file_name[] = new File(name).getName().getBytes();
		header[75] = (byte)(file_name.length - 3);
		System.arraycopy(file_name, 0, header, 76, file_name.length - 3);

		if (ac3)
		{
			header[26] = 0; 
			header[27] = (byte)0x80;  // set 1. AC3 PID as main TFaudio
		}
		else
		{
			header[26] = 0; //DM14062004 081.7 int04 changed
			header[27] = (byte)0xC0; //MPA
		}

		return header;
	}
	//DM09082004 081.7 int08 outsourced from X --

}