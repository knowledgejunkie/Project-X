/*
 * @(#)AudioFormatMPA.java - parse Audioheaders, mpa, incl. RDS
 *
 * Copyright (c) 2003-2008 by dvb.matt, All Rights Reserved.
 * 
 * This file is part of ProjectX, a free Java based demux utility.
 * By the authors, ProjectX is intended for educational purposes only, 
 * as a non-commercial test project.
 * 
 * The part of audio parsing was derived from the MPEG/Audio
 * Software Simulation Group's audio codec and ATSC A/52 in a special modified manner.
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

package net.sourceforge.dvb.projectx.audio;

import java.util.Arrays;
import java.util.ArrayList;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;

import net.sourceforge.dvb.projectx.common.Common;


public class AudioFormatMPA extends AudioFormat {
		
	private String instanced_time = "";

	private MpaConverter MPAConverter;

	public AudioFormatMPA()
	{
		super();

		instanced_time = String.valueOf(System.currentTimeMillis());

		initCRCTable();
	}

	private int CRC16_POLY = 0x18005; //((1 << 0) | (1 << 2) | (1 << 15) | (1 << 16));

	private int[] crc_table = new int[256];

	private int[][][] bitrate_index = {{
		{-1,8000,16000,24000,32000,40000,48000,56000,64000,
		80000,96000,112000,128000,144000,160000,0 },		//MPG-2, L3
		{-1,8000,16000,24000,32000,40000,48000,56000,64000,
		80000,96000,112000,128000,144000,160000,0 },		//MPG-2, L2
		{-1,32000,48000,56000,64000,80000,96000,112000,128000,
		144000,160000,176000,192000,224000,256000,0 }		//MPG-2, L1
	},{
		{-1,32000,40000,48000,56000,64000,80000,96000,
		112000,128000,160000,192000,224000,256000,320000, 0 },	//MPG-1, L3
		{-1,32000,48000,56000,64000,80000,96000,112000,
		128000,160000,192000,224000,256000,320000,384000, 0 },	//MPG-1, L2
		{-1,32000,64000,96000,128000,160000,192000,224000,
		256000,288000,320000,352000,384000,416000,448000,0 }	//MPG-1, L1
	},{
		{-1, 6000, 8000, 10000, 12000, 16000, 20000, 24000,    //MPG-2.5, L3??
		28000, 320000, 40000, 48000, 56000, 64000, 80000, 0 },
		{-1, 6000, 8000, 10000, 12000, 16000, 20000, 24000,    //MPG-2.5, L2
		28000, 320000, 40000, 48000, 56000, 64000, 80000, 0 },
		{-1, 8000, 12000, 16000, 20000, 24000, 32000, 40000,    //MPG-2.5, L1
		48000, 560000, 64000, 80000, 96000, 112000, 128000, 0 }
	}};
	
	private int frequency_index[][] = {
		{ 22050,24000,16000,0 },	//MPG2 - 22.05,24,16khz
		{ 44100,48000,32000,0 },	//MPG1 - 44.1 ,48,32khz
		{ 11025,12000,8000,0 }		//MPG2.5 - 11.025,12,8khz
	};
	
	private double time_index[] = { 0.0,103680000.0,103680000.0,34560000.0 };	//L3,L2,L1 * 90
	
	private String[] dID = { "MPEG-2", "MPEG-1", "MPEG-2.5" };
	private String[] dLayer = { "n.a.", "Layer3", "Layer2", "Layer1" };
	private String[] dCRC = { "noCRC", "CRC" };
	private String[] dMode = { "stereo", "jstereo", "dual", "mono" };

	private int Bound = 0;
	private int Sblimit = 32;

	/**
	 * parse mpa Header 
	 */
	public int parseHeader(byte[] frame, int pos)
	{
		int sblimit = 32;
	
		if ( (0xFF & frame[pos]) != 0xFF || (0xF0 & frame[pos + 1]) != 0xF0 ) 
			return -1;
	
		setID(1 & frame[pos + 1]>>>3);
		setEmphasis(3 & frame[pos + 3]);
	
		if (getID() == 1 && getEmphasis() == 2)
			setID(2);

		setLayer(3 & frame[pos + 1]>>>1);

		if (getLayer() < 1) 
			return -2;
	
		setProtectionBit((1 & frame[pos + 1]) ^ 1);
	
		setBitrate(bitrate_index[getID()][getLayer() - 1][0xF & frame[pos + 2]>>>4]); 

		if (getBitrate() < 1) 
			return -3;
	
		setSamplingFrequency(frequency_index[getID()][3 & frame[pos + 2]>>>2]);

		if (getSamplingFrequency() == 0) 
			return -4;
	
		setPaddingBit(1 & (frame[pos + 2]>>>1));
		setPrivateBit(1 & frame[pos + 2]);
	
		setMode(3 & frame[pos + 3]>>>6);
		setModeExtension(3 & frame[pos + 3]>>>4);

		if (getMode() == 0) 
			setModeExtension(0);
	
		Bound = getMode() == 1 ? ((getModeExtension() + 1) << 2) : sblimit;
		setChannel(getMode() == 3 ? 1 : 2);
		setCopyright(1 & frame[pos + 3]>>>3);
		setOriginal(1 & frame[pos + 3]>>>2);
		setFrameTimeLength(time_index[getLayer()] / getSamplingFrequency());

		if (getID() == 1 && getLayer() == 2)   // MPEG-1, L2 restrictions
		{
			if (getBitrate() / getChannel() < 32000) 
				return -5; /* unsupported bitrate */

			if (getBitrate() / getChannel() > 192000) 
				return -6; /* unsupported bitrate */
	
			if (getBitrate() / getChannel() < 56000)
			{
				if(getSamplingFrequency() == 32000) 
					Sblimit = 12;
				else 
					Sblimit = 8;
			}

			else if (getBitrate() / getChannel() < 96000) 
				Sblimit = 27;

			else
			{
				if (getSamplingFrequency() == 48000) 
					Sblimit = 27;
				else 
					Sblimit = 30;
			}

			if (Bound > Sblimit) 
				Bound = Sblimit;
		}

		else if (getLayer() == 2)  // MPEG-2
		{
			Sblimit = 30;
		}

		if (getLayer() < 3)
		{
			if (Bound > Sblimit) 
				Bound = Sblimit;

			setSizeBase((getID() == 0 && getLayer() == 1 ? 72 : 144) * getBitrate() / getSamplingFrequency());
			setSize(getSizeBase() + getPaddingBit());

			return 1;
		}

		else
		{
			Sblimit = 32;
			setSizeBase((12 * getBitrate() / getSamplingFrequency())<<2);
			setSize(getSizeBase() + (getPaddingBit()<<2));

			return 2;
		}
	}
	
	/**
	 * parse next mpa Header 
	 */
	public int parseNextHeader(byte[] frame, int pos)
	{

		if ( (0xFF & frame[pos]) != 0xFF || (0xF0 & frame[pos + 1]) != 0xF0 ) 
			return -1;
	
		setNextID(1 & frame[pos + 1]>>>3);
		setNextEmphasis(3 & frame[pos + 3]);
	
		if (getNextID() == 1 && getNextEmphasis() == 2)
			setNextID(2);

		setNextLayer(3 & frame[pos + 1]>>>1);

		if (getNextLayer() < 1) 
			return -2;
	
		setNextProtectionBit((1 & frame[pos + 1]) ^ 1);
	
		setNextBitrate(bitrate_index[getNextID()][getNextLayer() - 1][0xF & frame[pos + 2]>>>4]); 

		if (getNextBitrate() < 1) 
			return -3;
	
		setNextSamplingFrequency(frequency_index[getNextID()][3 & frame[pos + 2]>>>2]);

		if (getNextSamplingFrequency() == 0) 
			return -4;
	
		setNextPaddingBit(1 & (frame[pos + 2]>>>1));
		setNextPrivateBit(1 & frame[pos + 2]);
	
		setNextMode(3 & frame[pos + 3]>>>6);
		setNextModeExtension(3 & frame[pos + 3]>>>4);

		if (getNextMode() == 0) 
			setNextModeExtension(0);
	
		setNextChannel(getNextMode() == 3 ? 1 : 2);
		setNextCopyright(1 & frame[pos + 3]>>>3);
		setNextOriginal(1 & frame[pos + 3]>>>2);
		setNextFrameTimeLength(time_index[getNextLayer()] / getNextSamplingFrequency());

		if (getNextID() == 1 && getNextLayer() == 2)   // MPEG-1, L2 restrictions
		{
			if (getNextBitrate() / getNextChannel() < 32000) 
				return -5; /* unsupported bitrate */

			if (getNextBitrate() / getNextChannel() > 192000) 
				return -6; /* unsupported bitrate */
		}

		if (getNextLayer() < 3)
		{
			setNextSizeBase((getNextID() == 0 && getNextLayer() == 1 ? 72 : 144) * getNextBitrate() / getNextSamplingFrequency());
			setNextSize(getNextSizeBase() + getNextPaddingBit());

			return 1;
		}

		else
		{
			setNextSizeBase((12 * getNextBitrate() / getNextSamplingFrequency())<<2);
			setNextSize(getNextSizeBase() + (getNextPaddingBit()<<2));

			return 2;
		}
	}
	
	/**
	 * compare current & last mpa header 
	 */
	public int compareHeader()
	{
		if (getLastID() != getID()) 
			return 0x1;

		else if (getLastLayer() != getLayer()) 
			return 0x2;

		else if (getLastSamplingFrequency() != getSamplingFrequency()) 
			return 0x4;

		else if (getLastBitrate() != getBitrate()) 
			return 0x8;

		else if (getLastProtectionBit() != getProtectionBit()) 
			return 0x10;

		else if (getLastMode() != getMode())
		{
			if (getMode() + getLastMode() < 2)
				return 0x20;

			else
				return 0x40;
		}

		else 
			return 0;
	}
	
	/**
	 * display last mpa header 
	 */
	public String displayHeader()
	{
		return ("" + dID[getLastID()] + ", " + dLayer[getLastLayer()] + ", " + getLastSamplingFrequency() + "Hz, " + dMode[getLastMode()] + ", "+ (getLastBitrate() / 1000) + "kbps, " + dCRC[getLastProtectionBit()]);
	}

	/**
	 * link to mpa conversion
	 */
	public byte[][] convertFrame(byte[] frame, int mode)
	{
		if (MPAConverter == null)
			MPAConverter = new MpaConverter();

		byte[][] newframes = MPAConverter.modifyframe(frame, mode);

		parseRiffData(newframes[0], 1);

		if (mode >= MpaConverter.SPLIT_INTO_SINGLE)
			parseRiffData(newframes[1], 2);

		return newframes;
	}

	/**
	 * edit frame
	 */
/**	public byte[] editFrame(byte[] frame, int mode)
	{
		return frame;
	}
**/

	/**
	 * link to mpa decoder
	 */
/**	public byte[] decodeFrame(byte[] frame, int mode)
	{
		if (MPADecoder == null)
			MPADecoder = new MpaDecoder();

		return MPADecoder.decodeArray(frame);
	}
**/

	/**
	 * remove CRC from mpa 
	 **/
	public void removeCRC(byte[] frame, boolean remove)
	{
		if (getLayer() < 2 || !remove)
			return;

		removePrivateBit(frame);

		if ((frame[1] & 1) == 1) 
			return;

		System.arraycopy(frame, 6, frame, 4, frame.length - 6);
		Arrays.fill(frame, frame.length - 2, frame.length, (byte) 0);

		frame[1] |= 1;

		setProtectionBit(1);
	}
	
	/**
	 * remove private Bit from mpa 
	 **/
	private void removePrivateBit(byte[] frame)
	{
		if ( (frame[2] & 1) == 0) 
			return;

		frame[2] &= ~1;

		setPrivateBit(0);
	}

	/**
	 * crc init table
	 */
	private void initCRCTable()
	{
		for (int n = 0, c, k; n < 256; n++)
		{
			c = n << 8;

			for (k = 0; k < 8; k++)
			{
				if ((c & (1 << 15)) != 0) 
					c = ((c << 1) & 0xFFFF) ^ (CRC16_POLY & 0xFFFF);

				else
					c = c << 1;
			}

			crc_table[n] = c;
		}
	}

	/**
	 * crc
	 */
	private int determineCRC(byte[] data, int offs, int len, int crc)
	{
		int end = offs + (len>>>3);

		for (int i = offs; i < end; i++)
			crc = (crc_table[(0xFF & data[i]) ^ (crc >> 8)] ^ (crc << 8)) & 0xFFFF;

		int remaining_bits = len & 7;

		if (remaining_bits > 0)
			crc = (crc_table[((0xFF >> (8 - remaining_bits)) & (data[end] >> (8 - remaining_bits))) ^ (crc >> (16 - remaining_bits))] ^ (crc << remaining_bits)) & 0xFFFF;

		return crc;
	}

	/**
	 * 
	 */
	public int validateCRC(byte[] _data, int offs, int len)
	{
		if (getLayer() < 2 || getProtectionBit() == 0)
			return 0;

		int crc_val = (0xFF & _data[4])<<8 | (0xFF & _data[5]);

		byte[] data = new byte[_data.length];
		System.arraycopy(_data, 0, data, 0, 4);
		System.arraycopy(_data, 6, data, 4, _data.length - 6);

		int ch, sb, offset = 2, nr_bits = 16, BitPos[] = { 32 };

		if (getLayer() == 3) // BAL only, of 32 subbands
		{
			for( sb=0; sb<Bound; sb++)
				for( ch=0; ch<getChannel(); ch++)
					nr_bits += 4;

			for( sb=Bound; sb<Sblimit; sb++)
				nr_bits += 4;
		}
		else // BAL and SCFSI, of various subbands
		{
			int table_nbal[];
			int table_alloc[][];
			int allocation[][] = new int[32][2];

			if (getID()==1)
			{
				if (Sblimit > 20)
				{
					table_nbal = MpaDecoder.table_b2ab_nbal;
					table_alloc = MpaDecoder.table_b2ab;
				}
				else
				{
					table_nbal = MpaDecoder.table_b2cd_nbal;
					table_alloc = MpaDecoder.table_b2cd;
				}
			}
			else
			{
				table_nbal = MpaDecoder.table_MPG2_nbal;
				table_alloc = MpaDecoder.table_MPG2;
			}

			for( sb=0; sb<Bound; sb++)
			{
				for( ch=0; ch<getChannel(); ch++)
				{
					allocation[sb][ch] = table_alloc[sb][getBits(data, BitPos, table_nbal[sb])];
					nr_bits += table_nbal[sb];
				}
			}

			for( sb=Bound; sb<Sblimit; sb++)
			{
				allocation[sb][0] = allocation[sb][1] = table_alloc[sb][getBits(data, BitPos, table_nbal[sb])];
				nr_bits += table_nbal[sb];
			}

			for( sb=0; sb<Sblimit; sb++)
				for( ch=0; ch<getChannel(); ch++)
					if (allocation[sb][ch]>0)
						nr_bits += 2;
		}

		int crc = 0xFFFF;

		// look up table  is faster
		crc = determineCRC(data, offset, nr_bits, crc);

		return ((crc != crc_val) ? 1 : 0);
	}


	/**
	 * mpa riff header stuff
	 */
	private final int[] rpadding = { 0, 1, 1, 4 };
	private final int[] rlayer = { 0, 4, 2, 1 };
	private final int[][] rsample = { 
		{ 22050, 24000, 16000, 0 }, 
		{ 44100, 48000, 32000, 0 } 
	};
	private final int[] rmode = { 1, 2, 4, 8 };
	private final int[] rchnl = { 2, 2, 2, 1 };
	private final int[] rmext = { 1, 2, 4, 8 };
	private final int[] remph = { 1, 2, 3, 4 };
	private final int[][][] rbitrate = {
		{ {  0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0 },
		{  0,8000,16000,24000,32000,40000,48000,56000,64000,80000,96000,112000,128000,144000,160000,0  },
		{  0,8000,16000,24000,32000,40000,48000,56000,64000,80000,96000,112000,128000,144000,160000,0  },
		{  0,32000,48000,56000,64000,80000,96000,112000,128000,144000,160000,176000,192000,224000,256000,0 } },
		{ {  0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0 },
		{  0,32000,40000,48000,56000,64000,80000,96000,112000,128000,160000,192000,224000,256000,320000,0  },
		{  0,32000,48000,56000,64000,80000,96000,112000,128000,160000,192000,224000,256000,320000,384000,0  },
		{  0,32000,64000,96000,128000,160000,192000,224000,256000,288000,320000,352000,384000,416000,448000,0 } }
	};


	/**
	 * riffdata from mpeg audio
	 * awaiting a frame byte array, only the header is used
	 */
	public void parseRiffData(byte[] rh, int channel)
	{
		int[] riffdata = new int[10];

		// fwHeadFlags
		riffdata[0] = (8 & rh[1])<<1 | (1 & rh[1])<<3 | (4 & rh[3]) | (8 & rh[3])>>>2 | (1 & rh[2]);
		// fwHeadLayer
		riffdata[1] = rlayer[(6 & rh[1])>>>1];
		// nSamplesPerSec
		riffdata[2] = rsample[(8 & rh[1])>>>3][(0xC & rh[2])>>>2];
		// fwHeadMode
		riffdata[3] = rmode[(0xC0 & rh[3])>>>6];
		// nChannels
		riffdata[4] = rchnl[(0xC0 & rh[3])>>>6];
		// fwHeadModeExt
		riffdata[5] = rmext[(0x30 & rh[3])>>>4];
		// dwHeadBitrate
		riffdata[6] = rbitrate[(8 & rh[1])>>>3][(6 & rh[1])>>>1][(0xF0 & rh[2])>>>4];
		// wHeadEmphasis
		riffdata[7] = remph[(3 & rh[3])];
		// nBlockAlign
		riffdata[8] = riffdata[1] == 1 ? 4 * (12 * riffdata[6] / riffdata[2]) :  144 * riffdata[6] / riffdata[2];
		riffdata[8] /= ( (8 & rh[1]) == 0 && (6 & rh[1]) == 1 ) ? 2 : 1 ;

		if ((2 & rh[2]) != 0) 
			riffdata[8] += rpadding[(6 & rh[1])>>>1];

		setExtraWaveData(riffdata, channel);
	}



	/**
	 * RDS-Test, 
	 *
	 */
	ArrayList _list = new ArrayList();
	ByteArrayOutputStream bo = new ByteArrayOutputStream();

	private boolean DecodeRDS = false;
	private boolean Debug = false;
	private boolean hasRawData = false;

	private final byte RDS_identifier = (byte) 0xFD;
	private final int RDS_startcode = 0xFE;
	private final int RDS_endcode = 0xFF;

	private String[] rds_values = new String[9]; //buffer of messages

	private final String[] pty_list = {
		"undefined", "News", "Current Affairs", "Information", "Sport", "Education", "Drama", "Culture", "Science", 
		"Varied", "Pop Music", "Rock Music", "Easy Listening", "Light Classical", "Seriuos Classical", "Other Music", 
		"Weather", "Finance", "Children", "Social Affairs", "Religion", "Phone In", "Travel", "Leisure", "Jazz Music", 
		"Country Music", "National Music", "Oldies Music", "Folk Music", "Documentary", "Alarm Test", "Alarm"
	};

	/**
	 * RDS-char map table , unicode
	 */
	private final short[] chars = {
		0x0020, 0x0021, 0x0022, 0x0023, 0x00a4, 0x0025, 0x0026, 0x0027, 0x0028, 0x0029, 0x002a, 0x002b, 0x002c, 0x002d, 0x002e, 0x002f,
		0x0030, 0x0031, 0x0032, 0x0033, 0x0034, 0x0035, 0x0036, 0x0037, 0x0038, 0x0039, 0x003a, 0x003b, 0x003c, 0x003d, 0x003e, 0x003f,
		0x0040, 0x0041, 0x0042, 0x0043, 0x0044, 0x0045, 0x0046, 0x0047, 0x0048, 0x0049, 0x004a, 0x004b, 0x004c, 0x004d, 0x004e, 0x004f,
		0x0050, 0x0051, 0x0052, 0x0053, 0x0054, 0x0055, 0x0056, 0x0057, 0x0058, 0x0059, 0x005a, 0x005b, 0x005c, 0x005d, 0x005e, 0x005f,
		0x0060, 0x0061, 0x0062, 0x0063, 0x0064, 0x0065, 0x0066, 0x0067, 0x0068, 0x0069, 0x006a, 0x006b, 0x006c, 0x006d, 0x006e, 0x006f,
		0x0070, 0x0071, 0x0072, 0x0073, 0x0074, 0x0075, 0x0076, 0x0077, 0x0078, 0x0079, 0x007a, 0x007b, 0x007c, 0x007d, 0x007e, 0x0020,
		0x00e1, 0x00e0, 0x00e9, 0x00e8, 0x00ed, 0x00ec, 0x00f3, 0x00f2, 0x00fa, 0x00f9, 0x00d1, 0x00c7, 0x015e, 0x00df, 0x0130, 0x0132,
		0x00e2, 0x00e4, 0x00ea, 0x00eb, 0x00ee, 0x00ef, 0x00f4, 0x00f6, 0x00fb, 0x00fc, 0x00f1, 0x00e7, 0x015f, 0x011f, 0x0131, 0x0133
	};

	/**
	 *
	 */
	public void setAncillaryDataDecoder(boolean b, boolean b1)
	{
		DecodeRDS = b;
		Debug = b1;
		Arrays.fill(rds_values, null);
	}

	/**
	 *
	 */
	public String decodeAncillaryData(byte[] frame, double frametime)
	{
		if (!DecodeRDS)
			return null;

		int neg_offs = getSize() - 1;

		if (frame[neg_offs] != RDS_identifier)
		{
	//		neg_offs -= 2;

	//		if (frame[neg_offs + 2] != 0 || frame[neg_offs + 1] != 0 || frame[neg_offs] != RDS_identifier)
				return null;
		}

		int len = 0xFF & frame[neg_offs - 1];

		for (int i = neg_offs - 2, val; i > 3 && i > neg_offs - 2 - len; i--)
		{
			val = 0xFF & frame[i];
			_list.add(String.valueOf(val));
		}

		return decodeChunk(_list, frametime);
	}

	/**
	 * 
	 */
	private String decodeChunk(ArrayList list, double frametime)
	{
		int index = list.indexOf(String.valueOf(RDS_startcode));

		if (index < 0)
		{
			list.clear();
			return null;
		}

		while (index > 0)
		{
			list.remove(0);
			index--;
		}

		int eom_index = list.indexOf(String.valueOf(RDS_endcode));

		if (eom_index < 0)
			return null;

		else if (eom_index < 5) //fe xx yy zz ll aa 
		{
			list.remove(0);
			return null;
		}

		if (Debug)
		{
			String str = "";
			String str_1 = "";

			for (int i = 0; i <= eom_index; i++)
			{
				str_1 = Integer.toHexString(Integer.parseInt(list.get(i).toString())).toUpperCase();
				str += " " + (str_1.length() < 2 ? "0" + str_1 : str_1);
			}

			System.out.println("RDS:" + str);
		}

		int chunk_length = Integer.parseInt(list.get(4).toString());
		int real_length = -1;
		int type = -1;

		// fill bytearray, excluding crc + EOM, correct special bytes
		for (int i = 0, j = 0, k = 0, value, identifier_int = (0xFF & RDS_identifier); i <= eom_index; i++, list.remove(0))
		{
			value = Integer.parseInt(list.get(0).toString());

			if (i < 5 || value > identifier_int)
				continue;

			if (i == 5)
			{
				type = value;
				continue;
			}

			// coding of 0xFD,FE,FF
			if (value == identifier_int)
			{
				j = 1;
				continue;
			}

			if (j == 1)
			{
				value += identifier_int;
				j = 0;
			}

			if (k < chunk_length - 1)
				bo.write(value);

			real_length = k;
			k++;
		}

		// wrong length
		if (real_length != chunk_length)
			type = -1;

		String str = null;

		switch (type)
		{
		case 0xDA: //RASS
			getRawData(bo.toByteArray());
			break;

		case 0x0A: //RT
			str = compareMsg(getRT(bo.toByteArray()), 0, frametime);
			break;

		case 0x01: //PI
			str = compareMsg(getPI(bo.toByteArray()), 1, frametime);
			break;

		case 0x02: //PS program service name 
			str = compareMsg(getPS(bo.toByteArray()), 2, frametime);
			break;

		case 0x03: //TA
			str = compareMsg(getTP(bo.toByteArray()), 3, frametime);
			break;

		case 0x05: //MS
			str = compareMsg(getMS(bo.toByteArray()), 4, frametime);
			break;

		case 0x07: //PTY
			str = compareMsg(getPTY(bo.toByteArray()), 5, frametime);
			break;

		case 0x0D: //RTC
			str = compareMsg(getRTC(bo.toByteArray()), 6, frametime);
			break;

		case 0x30: //TMC 
			str = compareMsg("transmits TMC messages", 7, frametime);
			break;

		case 0x46: //ODA data
			str = compareMsg("transmits ODA messages", 8, frametime);
			break;

		case 0x40: //ODA SMC
		case 0x42: //ODA free 
		case 0x4A: //CT
		case 0x06: //PIN
			break;
		}

		bo.reset();

		return str;
	}

	/**
	 * 
	 */
	private void getRawData(byte[] array)
	{
		try {
			int index = 0;

			int len = 0xFF & array[index];
			int end = array.length;

			index += 5;

			int counter = 0xFF & array[index];

			index += 3;

			int bound = 0xFF & array[index];

			index++;

			try {

				if (!hasRawData)
				{
					hasRawData = true;
					Common.setMessage("-> exporting RDS data (RASS) to '" + instanced_time + "_RASS@RDS'");
				}

				BufferedOutputStream rawdata = new BufferedOutputStream(new FileOutputStream(Common.getCollection().getOutputNameParent(instanced_time + "_RASS@RDS"), true));

				for (int i = index, k; i < end; i++)
				{
					k = (0xFF & array[i]);

					rawdata.write(k);
				}

				rawdata.flush();
				rawdata.close();

			} catch (IOException ie) {
				Common.setMessage("!> error rds1");
			}

		} catch (ArrayIndexOutOfBoundsException ae) {
			Common.setMessage("!> error rds2");
		}
	}

	/**
	 * 
	 */
	private String compareMsg(String str, int index, double frametime)
	{
		if (str == null || str.equals(rds_values[index]))
			return null;

		rds_values[index] = str;

		return ("-> RDS @ " + Common.formatTime_1((long) (frametime / 90.0)) + ": " + str);
	}

	/**
	 * 
	 */
	private String getRT(byte[] array)
	{
		try {
			int index = 0;

			int dsn = 0xFF & array[index];
			int psn = 0xFF & array[index + 1];

			index += 2;

			int len = 0xFF & array[index];

			index++;

			int change = 0xFF & array[index];

			index++;

			String str = getString(array, index, len - 1);

			return ("-> RT (" + Integer.toHexString(change).toUpperCase() + "): '" + str.trim() + "'");

		} catch (ArrayIndexOutOfBoundsException ae) {}

		return null;
	}

	/**
	 * 
	 */
	private String getPS(byte[] array)
	{
		try {
			int index = 0;

			int dsn = 0xFF & array[index];
			int psn = 0xFF & array[index + 1];

			index += 2;

			int len = array.length >= index + 8 ? 8 : array.length - index;

			String str = getString(array, index, len);

			return ("-> PS (" + psn + "): '" + str.trim() + "'");

		} catch (ArrayIndexOutOfBoundsException ae) {}

		return null;
	}

	/**
	 * 
	 */
	private String getPI(byte[] array)
	{
		try {
			int index = 0;

			int dsn = 0xFF & array[index];
			int psn = 0xFF & array[index + 1];

			index += 2;

			int pi_code = (0xFF & array[index])<<8 | (0xFF & array[index + 1]);

			return ("-> PI (" + psn + "): 0x" + Integer.toHexString(pi_code).toUpperCase());

		} catch (ArrayIndexOutOfBoundsException ae) {}

		return null;
	}

	/**
	 * 
	 */
	private String getTP(byte[] array)
	{
		try {
			int index = 0;

			int dsn = 0xFF & array[index];
			int psn = 0xFF & array[index + 1];

			index += 2;

			boolean tp = (2 & array[index]) != 0;
			boolean ta = (1 & array[index]) != 0;

			return ("-> TP/TA (" + psn + "): " + (tp ? "TP" : "no TP") + " / " + (ta ? "TA on air" : "no TA"));

		} catch (ArrayIndexOutOfBoundsException ae) {}

		return null;
	}

	/**
	 * 
	 */
	private String getMS(byte[] array)
	{
		try {
			int index = 0;

			int dsn = 0xFF & array[index];
			int psn = 0xFF & array[index + 1];

			index += 2;

			boolean speech = (1 & array[index]) != 0;

			return ("-> MS (" + psn + "): " + (speech ? "Speech" : "Music"));

		} catch (ArrayIndexOutOfBoundsException ae) {}

		return null;
	}

	/**
	 * 
	 */
	private String getPTY(byte[] array)
	{
		try {
			int index = 0;

			int dsn = 0xFF & array[index];
			int psn = 0xFF & array[index + 1];

			index += 2;

			int pty = 0x1F & array[index];

			return ("-> PTY (" + psn + "): " + pty_list[pty]);

		} catch (ArrayIndexOutOfBoundsException ae) {}

		return null;
	}

	/**
	 * 
	 */
	private String getRTC(byte[] array)
	{
		try {
			int index = 0;

			String year  = "20" + Common.adaptString(Integer.toHexString(0x7F & array[index]), 2);
			String month = Common.adaptString(String.valueOf(0xF & array[index + 1]), 2);
			String date  = Common.adaptString(String.valueOf(0x1F & array[index + 2]), 2);
			String hour  = Common.adaptString(String.valueOf(0x1F & array[index + 3]), 2);
			String min   = Common.adaptString(String.valueOf(0x3F & array[index + 4]), 2);
			String sec   = Common.adaptString(String.valueOf(0x3F & array[index + 5]), 2);
			String censec= Common.adaptString(String.valueOf(0x7F & array[index + 6]), 2);

			int ltoffs   = 0xFF & array[index + 7];

			String loctime = ltoffs != 0xFF ? (((0x20 & ltoffs) != 0) ? "-" + ((0x1F & ltoffs) / 2) : "+" + ((0x1F & ltoffs) / 2)) : "\u00B1" + "0";

			return ("-> RTC (" + loctime + "h): " + year + "." + month + "." + date + "  " + hour + ":" + min + ":" + sec + "." + censec);

		} catch (ArrayIndexOutOfBoundsException ae) {}

		return null;
	}

	/**
	 * 
	 */
	private String getString(byte[] array, int offset, int length)
	{
		String str = "";

		try {

			for (int i = offset, val, j = offset + length; i < j; i++)
			{
				val = 0xFF & array[i];

				str += (val > 0x9F || val < 0x20) ? (char)chars[0] : (char)chars[val - 0x20];
			}

		} catch (ArrayIndexOutOfBoundsException ae) {}

		return str;
	}


	/**
	 * part for RIFF wave header data processing
	 */

	private WaveHeader WaveHeader_Ch1;
	private WaveHeader WaveHeader_Ch2;

	/**
	 * 
	 */
	public void initExtraWaveHeader(boolean bool_ACM, boolean bool_BWF, boolean bool_AC3)
	{
		WaveHeader_Ch1 = new WaveHeader(bool_ACM, bool_BWF);
		WaveHeader_Ch2 = new WaveHeader(bool_ACM, bool_BWF);
	}

	/**
	 * 
	 */
	public byte[] getExtraWaveHeader(int channel, boolean placeholder)
	{
		switch (channel)
		{
		case 1:
			return (placeholder ? WaveHeader_Ch1.getPlaceHolder() : WaveHeader_Ch1.getHeader());

		case 2:
			return (placeholder ? WaveHeader_Ch2.getPlaceHolder() : WaveHeader_Ch2.getHeader());
		}

		return (new byte[0]);
	}

	/**
	 * 
	 */
	public void setExtraWaveData(int[] array, int channel)
	{
		switch (channel)
		{
		case 1:
			WaveHeader_Ch1.setWaveData(array);
			break;

		case 2:
			WaveHeader_Ch2.setWaveData(array);
			break;
		}
	}

	/**
	 * 
	 */
	public void setExtraWaveLength(long filelength, long timelength, int channel)
	{
		switch (channel)
		{
		case 1:
			WaveHeader_Ch1.setWaveLength(filelength, timelength);
			break;

		case 2:
			WaveHeader_Ch2.setWaveLength(filelength, timelength);
			break;
		}
	}


	/**
	 * 
	 */
	private class WaveHeader {

		private byte[] riffacm = { 
			82, 73, 70, 70,  0,  0,  0,  0, 87, 65, 86, 69,102,109,116, 32,
			30,  0,  0,  0, 85,  0,  1,  0,  1,  0,  0,  0,  0,  0,  0,  0,
			1,  0,  0,  0, 12,  0,  1,  0,  2,  0,  0,  0,  0,  0,  1,  0,
			113,  5,102, 97, 99,116,  4,  0,  0,  0,  0,  0,  0,  0,100, 97,
			116, 97,  0,  0,  0,  0 
		};

		private byte[] riffbwf = { 
			82, 73, 70, 70,  0,  0,  0,  0, 87, 65, 86, 69,102,109,116, 32,
			40,  0,  0,  0, 80,  0,  1,  0,  1,  0,  0,  0,  0,  0,  0,  0,
			0,  0,  0,  0, 22,  0,  0,  0,  1,  0,  0,  0,  0,  0,  0,  0,
			0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,102, 97, 99,116,
			4,  0,  0,  0,  0,  0,  0,  0,100, 97,116, 97,  0,  0,  0,  0 
		};

		private long Samples = 0;
		private long SampleCount = 0;

		private final int HeaderLength_ACM = 70;
		private final int HeaderLength_BWF = 80;
		private final int ACM_WaveFormat = 1;
		private final int BWF_WaveFormat = 2;

		private int WaveFormat;

		//init
		public WaveHeader(boolean bool_ACM, boolean bool_BWF)
		{
			WaveFormat = bool_ACM ? ACM_WaveFormat : bool_BWF ? BWF_WaveFormat : 0;
		}

		/**
		 * get place holder
		 */
		public byte[] getPlaceHolder()
		{
			switch (WaveFormat)
			{
			case ACM_WaveFormat:
				return (new byte[HeaderLength_ACM]);

			case BWF_WaveFormat:
				return (new byte[HeaderLength_BWF]);
			}

			return (new byte[0]);
		}

		/**
		 * get updated header 
		 */
		public byte[] getHeader()
		{ 
			switch (WaveFormat)
			{
			case ACM_WaveFormat:
				return riffacm;

			case BWF_WaveFormat:
				return riffbwf;
			}

			return (new byte[0]);
		} 

		/**
		 * set wave data
		 */
		public void setWaveData(int[] riffdata)
		{
			Samples += riffdata[2]; 
			SampleCount++;

			int nSamplesPerSec = getValue(riffbwf, 24, 4, true);
			int dwHeadBitrate  = getValue(riffbwf, 40, 4, true);
			int nBlockAlign    = getValue(riffbwf, 32, 2, true);

			//nBlockAlign
			if (nBlockAlign == 0)
			{
				setValue(riffacm, 44, 2, true, riffdata[8]);
				setValue(riffbwf, 32, 2, true, riffdata[8]);
			}

			else if (nBlockAlign != 1 &&  nBlockAlign != riffdata[8])
				setValue(riffbwf, 32, 2, true, 1);

			//nSamplesPerSec
			if (nSamplesPerSec == 1)
			{
				setValue(riffacm, 24, 4, true, riffdata[2]);
				setValue(riffbwf, 24, 4, true, riffdata[2]);
			}

			else if (nSamplesPerSec != 0 &&  nSamplesPerSec != riffdata[2]) 
			{
				setValue(riffacm, 24, 4, true, 0);
				setValue(riffbwf, 24, 4, true, 0);
			}

			//dwHeadBitrate
			if (dwHeadBitrate == 1)
				setValue(riffbwf, 40, 4, true, riffdata[6]);

			else if (dwHeadBitrate != 0 &&  dwHeadBitrate != riffdata[6]) 
				setValue(riffbwf, 40, 4, true, 0);

			// fwHeadModeExt
			if (riffdata[3] == 2)
				riffbwf[46] |= (byte) riffdata[5];  

			// nChannels
			if (riffbwf[22] == 1)
				riffacm[22] = riffbwf[22] = (byte) riffdata[4];    

			riffbwf[38] |= (byte) riffdata[1];   // fwHeadLayer
			riffbwf[44] |= (byte) riffdata[3];   // fwHeadMode
			riffbwf[48] |= (byte) riffdata[7];   // wHeadEmphasis
			riffbwf[50] |= (byte) riffdata[0];   // fwHeadFlags
		}

		/**
		 * 
		 */
		public void setWaveLength(long filelength, long timelength)
		{
			int lengthACM = (int)filelength - HeaderLength_ACM;
			int lengthBWF = (int)filelength - HeaderLength_BWF;

			for (int i = 0; i < 4; i++)
			{
				riffacm[4 + i] = (byte)(0xFF & (lengthACM + 62)>>>(i * 8));
				riffbwf[4 + i] = (byte)(0xFF & (lengthBWF + 72)>>>(i * 8));
				riffacm[66 + i] = (byte)(0xFF & lengthACM>>>(i * 8));
				riffbwf[76 + i] = (byte)(0xFF & lengthBWF>>>(i * 8));
			}

			if (filelength <= 100)
				return;

			int time = (int)timelength;
			int nAvgBytePerSecACM = (int)(1000L * lengthACM / time);
			int nAvgBytePerSecBWF = (int)(1000L * lengthBWF / time);

			for (int i = 0; i < 4; i++)
			{ 
				riffacm[28 + i] = (byte)(0xFF & nAvgBytePerSecACM>>>(i * 8));
				riffbwf[28 + i] = (byte)(0xFF & nAvgBytePerSecBWF>>>(i * 8));
			}

			int fact = (int)(1L * (Samples/SampleCount) * time /1000);

			for (int i = 0; i < 4; i++) 
				riffacm[58 + i] = riffbwf[68 + i] = (byte)(0xFF & fact>>>(i * 8));
		}
	}

}