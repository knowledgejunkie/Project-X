/*
 * @(#)AudioFormatAC3.java - parse Audioheaders, ac3
 *
 * Copyright (c) 2003-2009 by dvb.matt, All Rights Reserved.
 * 
 * This file is part of ProjectX, a free Java based demux utility.
 * By the authors, ProjectX is intended for educational purposes only, 
 * as a non-commercial test project.
 * 
 * The part of audio parsing was derived from
 * ATSC A/52 in a special modified manner.
 *
 * crc computing derived from:
 * The simplest AC3 encoder, Copyright (c) 2000 Fabrice Bellard. 
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

import net.sourceforge.dvb.projectx.parser.CommonParsing;

public class AudioFormatAC3 extends AudioFormat {
		
	public AudioFormatAC3()
	{
		super();

		initCRCTable();
	}

	private int bsid = 0;

	private int CRC16_POLY = 0x18005; //((1 << 0) | (1 << 2) | (1 << 15) | (1 << 16));

	private int[] crc_table = new int[256];

	private int[] ac3_frequency_index = { 48000, 44100, 32000, 0 };
	
	private int[] ac3_bitrate_index =  { 
		32000, 40000, 48000, 56000, 64000, 80000, 96000,
		112000, 128000, 160000, 192000, 224000, 256000,
		320000, 384000, 448000, 512000, 576000, 640000,
		0,0,0,0,0,0,0,0,0,0,0,0,0  // (fix4)
	};

	private int[][] ac3_size_table = {
		{ 128,160,192,224,256,320,384,448,512,640,768,896,1024,1280,1536,1792,2048,2304,2560 }, //48khz
		{ 138,174,208,242,278,348,416,486,556,696,834,974,1114,1392,1670,1950,2228,2506,2786 }, //44.1khz
		{ 192,240,288,336,384,480,576,672,768,960,1152,1344,1536,1920,2304,2688,3120,3456,3840 } //32khz
	};
	
	private String[] bsmod = {	", CM" , ", ME" , ", K:VI" , ", K:HI" , ", K:D" , ", K:C" , ", K:E" , ", K:VO" };
	private String[] cmixlev = { "", ", cm -3.0dB", ", cm -4.5dB", ", cm -6.0dB", ", cm -4.5dB" };
	private String[] surmixlev = {	"", ", sm -3dB", ", sm -6dB", ", sm 0dB", ", sm -6dB" };
	private String[] dsurmod = { "" , ", notDS" , ", DS" , "" };
	private String[] acmod = { "1+1", "1/0", "2/0", "3/0", "2/1", "3/1", "2/2", "3/2" };
	private String[][] lfe = {{ ".0", ".1" },{ "", "lfe" }};

	private int[] ac3_channels = { 2, 1, 2, 3, 3, 4, 4, 5 };
	
	/**
	 * parse ac3 Header 
	 */
	public int parseHeader(byte[] frame, int pos)
	{
		if ( !hasAC3Syncword(frame, pos)) 
			return -1;
	
		setID(0);
		setEmphasis(0);
		setPrivateBit(0);
	//	setProtectionBit(0 ^ 1);
		setSamplingFrequency(getAC3SamplingFrequency(frame, pos));

		if (getSamplingFrequency() < 1) 
			return -4;

		setBitrate(getAC3Bitrate(frame, pos)); 

		if (getBitrate() < 1) 
			return -3;
	
		setProtectionBit((0x1F & frame[pos + 5]>>3));       //bsid
		setLayer(getAC3Bsmod(frame, pos));       //bsmod
		setPaddingBit(1 & frame[pos + 4]);
		setMode(getAC3Mode(frame, pos));
		setModeExtension(0);
	
		int mode = (0xFF & frame[pos + 6])<<8 | (0xFF & frame[pos + 7]);
		int skip=0;

		if ((getMode() & 1) > 0 && getMode() != 1)  // cmix
		{
			setEmphasis(1 + (3 & frame[pos + 6]>>>3));
			skip++;
		}

		if ((getMode() & 4) > 0) //surmix
		{
			setPrivateBit(1 + (3 & frame[pos + 6]>>>(skip > 0 ? 1 : 3)));
			skip++;
		}

		if (getMode() == 2)
		{
	        setModeExtension(getModeExtension() | (6 & mode>>>(10 - (2 * skip))));  //DS
			skip++;
		}

		if (skip < 4)
		{
	        setModeExtension(getModeExtension() | (1 & mode>>>(12 - (2 * skip)))); //lfe
			setOriginal(0x1F & mode>>>(7 - (2 * skip))); //dialnorm
		}

		setChannel(ac3_channels[getMode()] + (1 & getModeExtension()));
		setCopyright(0);

		setFrameTimeLength(138240000.0 / getSamplingFrequency());
		setSizeBase(ac3_size_table[3 & frame[pos + 4]>>>6][0x1F & frame[pos + 4]>>>1]);
		setSize(getSamplingFrequency() == ac3_frequency_index[1] ? getSizeBase() + (getPaddingBit() * 2) : getSizeBase());

		return 1;
	}
	
	/**
	 * parse next ac3 Header 
	 */
	public int parseNextHeader(byte[] frame, int pos)
	{
	
		if ( !hasAC3Syncword(frame, pos)) 
			return -1;
	
		setNextID(0);
		setNextEmphasis(0);
		setNextPrivateBit(0);
	//	setNextProtectionBit(0 ^ 1);

		setNextSamplingFrequency(getAC3SamplingFrequency(frame, pos));

		if (getNextSamplingFrequency() < 1) 
			return -4;

		setNextBitrate(getAC3Bitrate(frame, pos)); 

		if (getNextBitrate() < 1) 
			return -3;
	
		setNextProtectionBit((0x1F & frame[pos + 5]>>3));       //bsid
		setNextLayer(getAC3Bsmod(frame, pos));       //bsmod
		setNextPaddingBit(1 & frame[pos + 4]);
		setNextMode(getAC3Mode(frame, pos));
		setNextModeExtension(0);
//
		int mode = (0xFF & frame[pos + 6])<<8 | (0xFF & frame[pos + 7]);
		int skip=0;

		if ((getNextMode() & 1) > 0 && getNextMode() != 1)  // cmix
		{
			setNextEmphasis(1 + (3 & frame[pos + 6]>>>3));
			skip++;
		}

		if ((getNextMode() & 4) > 0) //surmix
		{
			setNextPrivateBit(1 + (3 & frame[pos + 6]>>>(skip > 0 ? 1 : 3)));
			skip++;
		}

		if (getNextMode() == 2)
		{
	        setNextModeExtension(getNextModeExtension() | (6 & mode>>>(10 - (2 * skip))));  //DS
			skip++;
		}

		if (skip < 4)
		{
	        setNextModeExtension(getNextModeExtension() | (1 & mode>>>(12 - (2 * skip)))); //lfe
			setNextOriginal(0x1F & mode>>>(7 - (2 * skip))); //dialnorm
		}

		setNextChannel(ac3_channels[getNextMode()] + (1 & getNextModeExtension()));
		setNextCopyright(0);

		setNextFrameTimeLength(138240000.0 / getNextSamplingFrequency());
		setNextSizeBase(ac3_size_table[3 & frame[pos + 4]>>>6][0x1F & frame[pos + 4]>>>1]);
		setNextSize(getNextSamplingFrequency() == ac3_frequency_index[1] ? getNextSizeBase() + (getNextPaddingBit() * 2) : getNextSizeBase());

		return 1;
	}

	/**
	 * 
	 */
	private boolean hasAC3Syncword(byte[] frame, int offs)
	{
		if (frame[offs] != 0x0B || frame[offs + 1] != 0x77)
			return false;

		return true;
	}

	/**
	 * 
	 */
	private int getAC3SamplingFrequency(byte[] frame, int offs)
	{
		return ac3_frequency_index[3 & frame[offs + 4]>>>6]; 
	}

	/**
	 * 
	 */
	private int getAC3Bitrate(byte[] frame, int offs)
	{
		return ac3_bitrate_index[0x1F & frame[offs + 4]>>>1];
	}

	/**
	 * 
	 */
	private int getAC3Bsmod(byte[] frame, int offs)
	{
		return (7 & frame[offs + 5]);
	}

	/**
	 * 
	 */
	private int getAC3Mode(byte[] frame, int offs)
	{
		return (7 & frame[offs + 6]>>>5);
	}

	/**
	 * compare current & last ac3 header 
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

		else if (getLastMode() != getMode())
			return 0x10;

		else if (getLastModeExtension() != getModeExtension())
			return 0x20;

		else if (getLastOriginal() != getOriginal())
			return 0x40;

		else if (getLastEmphasis() != getEmphasis())
			return 0x80;

		else if (getLastProtectionBit() != getProtectionBit())
			return 0x100;

		else 
			return 0;
	}
	
	/**
	 * display last ac3 header 
	 */
	public String displayHeader()
	{
		return ("AC-3" + bsmod[getLastLayer()] + 
			", " + 
			acmod[getLastMode()] + 
			lfe[1][1 & getLastModeExtension()] + 
			"(" + 
			ac3_channels[getLastMode()] + 
			lfe[0][1 & getLastModeExtension()] + 
			")" + 
			", bsid " + getLastProtectionBit() +
			", dn -" + getLastOriginal() + "dB" +
			dsurmod[getLastModeExtension()>>>1] + 
			cmixlev[getLastEmphasis()] + 
			surmixlev[getLastPrivateBit()] + 
			", " + 
			getLastSamplingFrequency() + "Hz, " + 
			(getLastBitrate() / 1000) + "kbps");
	}

	/**
	 *
	 */
	public byte[] editFrame(byte[] frame, int mode)
	{
		switch (mode)
		{
		case 1: //patch only to 3/2
			setChannelFlags(frame, 7);
		//	computeCRC(frame, framesize);
			break;

		case 2:
			frame = setNewBitrate(frame);
			break;

		case 3:
			frame = setSilence(frame);
			break;

		case 4:
			frame = setBsid(frame);
			break;
		}

		return frame;
	}

	/**
	 * fix to 3+2 channels, note the following bits will be dispointed and the frame is corrupted
	 */
	private void setChannelFlags(byte[] frame, int mode)
	{
		frame[6] = (byte)((0xF & frame[6]) | (mode<<5));
	}

	/**
	 * bitrate edit
	 */
	private byte[] setNewBitrate(byte[] frame)
	{
		int size_index_src = 0x1F & frame[4]>>>1;
		int size_index = (int)(0x1FL & (CommonParsing.getAudioProcessingFlags()>>>4));

		if (size_index_src == size_index)
			return frame;

		else if (size_index_src > size_index)
		{
			size_index = size_index_src;

			if ((CommonParsing.getAudioProcessingFlags()>>>18) > 0) // not first frame
				CommonParsing.setAudioProcessingFlags(CommonParsing.getAudioProcessingFlags() | 0xCL); //restart marker
		}

		CommonParsing.setAudioProcessingFlags((~0xFF0L & CommonParsing.getAudioProcessingFlags()) | size_index<<4); //common bitrate index

		byte[] newframe = new byte[ac3_size_table[3 & frame[4]>>>6][size_index]];

		System.arraycopy(frame, 0, newframe, 0, frame.length);

		newframe[4] &= 0xC0; //keep samplerate
		newframe[4] |= (size_index<<1); //set index

		clearCRC(newframe, frame.length);
		computeCRC(newframe, ac3_size_table[3 & newframe[4]>>>6][size_index]);

		return newframe;
	}

	/**
	 * set silence
	 */
	private byte[] setSilence(byte[] frame)
	{
		byte[] newframe = new byte[frame.length];

		System.arraycopy(frame, 0, newframe, 0, frame.length);

		int bitpos = getBSI(newframe, 0);

		if ((bitpos & 7) == 0) //byte aligned
			Arrays.fill(newframe, bitpos>>>3, newframe.length, (byte) 0);

		else
		{
			Arrays.fill(newframe, (bitpos + 1)>>>3, newframe.length, (byte) 0);
			newframe[bitpos>>>3] &= (0xFF00>>>(bitpos & 7));
		}

		clearCRC(newframe, newframe.length);
		computeCRC(newframe, ac3_size_table[3 & newframe[4]>>>6][0x1F & newframe[4]>>>1]);

		return newframe;
	}

	/**
	 * set bsid
	 */
	private byte[] setBsid(byte[] frame)
	{
		byte[] newframe = new byte[frame.length];

		System.arraycopy(frame, 0, newframe, 0, frame.length);

		if (getLastProtectionBit() == (0x1F & newframe[5]>>3))
			return newframe;
		
		newframe[5] = (byte)(getLastProtectionBit()<<3 | (7 & newframe[5]));

		clearCRC(newframe, newframe.length);
		computeCRC(newframe, ac3_size_table[3 & newframe[4]>>>6][0x1F & newframe[4]>>>1]);

	//	net.sourceforge.dvb.projectx.common.Common.setMessage("bsid " + (0x1F & frame[5]>>3) + " /n " + (0x1F & newframe[5]>>3));

		return newframe;
	}

	/**
	 * validate crc16 1 + 2
	 */
	public int validateCRC(byte[] frame, int offset, int frame_size)
	{
		// frame_size is BYTE
		int words = frame_size>>>1; //to word
		int frame_size_58 = 2 * ((words>>>1) + (words>>>3)); //frame_size_58

		int crc = -1;

		//crc1
		if ((crc = determineCRC(frame, 2, frame_size_58, 0)) != 0)
			return 1;

		//crc2
		if ((crc = determineCRC(frame, frame_size_58, frame_size, crc)) != 0)
			return 2;

		return 0;
	}

	/**
	 * clear crc
	 */
	private void clearCRC(byte[] frame, int size)
	{
		frame[2] = (byte) 0;
		frame[3] = (byte) 0;
		frame[size - 2] = (byte) 0;
		frame[size - 1] = (byte) 0;
	}

	/**
	 * compute crc16 1 + 2
	 */
	private void computeCRC(byte[] frame, int frame_size)
	{
		// frame_size is BYTE
		int words = frame_size>>>1; //to word
		int frame_size_58 = 2 * ((words>>>1) + (words>>>3)); //frame_size_58

		int crc1 = -1;
		int crc2 = -1;
		int crc_inv = -1;

		crc1 = determineCRC(frame, 4, frame_size_58, 0);

		crc_inv = pow_poly((CRC16_POLY >>> 1), (frame_size_58<<3) - 16, CRC16_POLY);

		//crc1
		crc1 = mul_poly(crc_inv, crc1, CRC16_POLY);
		frame[2] = (byte)(0xFF & (crc1 >> 8));
		frame[3] = (byte)(0xFF & crc1);

		//crc2
		crc2 = determineCRC(frame, frame_size_58, (words<<1) - 2, 0);
		frame[(2* words) - 2] = (byte)(0xFF & (crc2 >> 8));
		frame[(2* words) - 1] = (byte)(0xFF & crc2);
	}

	/**
	 * ac3 crc init table
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
	 * ac3 crc
	 */
	private int determineCRC(byte[] data, int offs, int len, int crc)
	{
		for (int i = offs; i < len; i++)
			crc = (crc_table[(0xFF & data[i]) ^ (crc >> 8)] ^ (crc << 8)) & 0xFFFF;

		return crc;
	}

	/**
	 * crc poly
	 */
	private int mul_poly(int a, int b, int poly)
	{
		int c = 0;

		while (a > 0)
		{
			if ((a & 1) > 0)
				c ^= b;

			a = a >>> 1;
			b = b << 1;

			if ((b & (1 << 16)) > 0)
				b ^= poly;
		}

		return c;
	}

	/**
	 * crc poly
	 */
	private int pow_poly(int a, int n, int poly)
	{
		int r = 1;

		while (n > 0)
		{
			if ((n & 1) > 0)
				r = mul_poly(r, a, poly);

			a = mul_poly(a, a, poly);
			n >>>= 1;
		}

		return r;
	}


	/**
	 * bsi , taken from Doc A/52
	 */
	private int getBSI(byte[] frame, int pos)
	{
		//start at frame[5]

		int[] BitPos = { pos<<3 };
		int acmod = 0;
		int val = 0;

		getBits(frame, BitPos, 16); //sync
		getBits(frame, BitPos, 16); //crc1
		getBits(frame, BitPos, 2); //freq
		getBits(frame, BitPos, 6); //size
		getBits(frame, BitPos, 5); //bsi
		getBits(frame, BitPos, 3); //bsmod

		acmod = getBits(frame, BitPos, 3); //acmod
		if ((acmod & 1) > 0 && acmod != 1)
			getBits(frame, BitPos, 2); //cmixlev // if 3 front channels
		if ((acmod & 4) > 0)
			getBits(frame, BitPos, 2); //surmixlev // if a surround channel exists 
		if (acmod == 2)
			getBits(frame, BitPos, 2); //dsurmod // if in 2/0 mode 

		getBits(frame, BitPos, 1); //lfeon
		getBits(frame, BitPos, 5); //dialnorm

		if (getBits(frame, BitPos, 1) == 1) //compre
			getBits(frame, BitPos, 8); //compr

		if (getBits(frame, BitPos, 1) == 1) //langcode
			getBits(frame, BitPos, 8); //langcod

		if (getBits(frame, BitPos, 1) == 1) //audprodie
		{
			getBits(frame, BitPos, 5); //mixlevel
			getBits(frame, BitPos, 2); //roomtyp
		}

		if (acmod == 0) // if 1+1 mode (dual mono, so some items need a second value)
		{
			getBits(frame, BitPos, 5); //dialnorm2

			if (getBits(frame, BitPos, 1) == 1) //compr2e
				getBits(frame, BitPos, 8); //compr2

			if (getBits(frame, BitPos, 1) == 1) //langcod2e
				getBits(frame, BitPos, 8); //langcod2

			if (getBits(frame, BitPos, 1) == 1) //audprodi2e
			{
				getBits(frame, BitPos, 5); //mixlevel2
				getBits(frame, BitPos, 2); //roomtyp2
			}
		}

		getBits(frame, BitPos, 1); //copyrightb
		getBits(frame, BitPos, 1); //origbs

		if (getBits(frame, BitPos, 1) == 1) //timecod1e
			getBits(frame, BitPos, 14); //timecod1

		if (getBits(frame, BitPos, 1) == 1) //timecod2e
			getBits(frame, BitPos, 14); //timecod2

		if (getBits(frame, BitPos, 1) == 1) //addbsie
		{
			val = getBits(frame, BitPos, 6); //addbsil
			getBits(frame, BitPos, (val + 1) * 8); //addbsi
		}

		return BitPos[0];
	}



	/**
	 * ac3 riff header stuff
	 */
	// 1536s / 44.1 -> 34.8299ms -> 3134.691
	// 1536s / 48 -> 32ms -> 2880
	// 1536s / 32 -> 48ms -> 4320

	private final int[] armode = { 0, 1, 2, 3, 3, 4, 4, 5 };
	private final int[] arsample = { 48000, 44100, 32000, 0 };
	private final int[] arbitrate =  { 
		0, 40000, 48000, 56000, 64000, 80000, 96000, 112000, 128000, 160000, 192000, 
		224000, 256000, 320000, 384000, 448000, 512000, 576000, 640000, 0, 0
	};

	/**
	 * ac3 bitrate constants
	 */
	/* 96k,112k,128k,160k,192k,224k,256k,320k,384k,448k,512k,576k,640k
	 *   0=48khz,1=44.1khz,2=32khz  32ms,36ms,48ms      44.1khz padding +2 bytes
	 */
	private final int[][] ac3const = {
          { 288000000,160,192,224,256,320,384,448,512,640,768,896,1024,1280,1536,1792,2080,2304,2560 },
          { 313469388,174,208,242,278,348,416,486,556,696,834,974,1114,1392,1670,1950,2228,2506,2786 },
          { 432000000,240,288,336,384,480,576,672,768,960,1152,1344,1536,1920,2304,2688,3120,3456,3840 }
	};

	/**
	 * riffdata from ac3 audio
	 * awaiting a frame byte array, only the header is used
	 */
	public void parseRiffData(byte[] frame, int channel)
	{
		int[] riffdata = new int[10];

		// nSamplesPerSec
		riffdata[2] = arsample[(0xC0 & frame[4])>>>6];
		// nChannels
		riffdata[4] = armode[(0xE0 & frame[6])>>>5];
		// dwHeadBitrate
		riffdata[6] = arbitrate[(0x3F & frame[4])>>>1];
		// nBlockAlign
		riffdata[8] = ac3const[(0xC0 & frame[4])>>>6][(0x3E & frame[4])>>>1] + (((1 & frame[4])!=0) ? 2 : 0);

		setExtraWaveData(riffdata, channel);
	}

	/**
	 * part for RIFF wave header data processing
	 */

	private WaveHeader WaveHeader_Ch1;

	/**
	 * 
	 */
	public void initExtraWaveHeader(boolean bool_ACM, boolean bool_BWF, boolean bool_AC3)
	{
		WaveHeader_Ch1 = new WaveHeader(bool_AC3);
	}

	/**
	 * 
	 */
	public byte[] getExtraWaveHeader(int channel, boolean placeholder)
	{
		if (channel == 1)
			return (placeholder ? WaveHeader_Ch1.getPlaceHolder() : WaveHeader_Ch1.getHeader());

		return (new byte[0]);
	}

	/**
	 * 
	 */
	public void setExtraWaveData(int[] array, int channel)
	{
		if (channel == 1)
			WaveHeader_Ch1.setWaveData(array);
	}

	/**
	 * 
	 */
	public void setExtraWaveLength(long filelength, long timelength, int channel)
	{
		if (channel == 1)
			WaveHeader_Ch1.setWaveLength(filelength, timelength);
	}

	/**
	 * 
	 */
	private class WaveHeader {

		private byte[] riffac3 = { 
			82, 73, 70, 70,  0,  0,  0,  0, 87, 65, 86, 69,102,109,116, 32,
			18,  0,  0,  0,  0, 32,  1,  0,  1,  0,  0,  0,  0,  0,  0,  0,
			0,  0,  0,  0, 18,  0,100, 97,116, 97,  0,  0,  0,  0, 
		};

		private long Samples = 0;
		private long SampleCount = 0;

		private final int HeaderLength_AC3 = 46;
		private final int AC3_WaveFormat = 1;

		private int WaveFormat;

		//init
		public WaveHeader(boolean bool_AC3)
		{
			WaveFormat = bool_AC3 ? AC3_WaveFormat : 0;
		}

		/**
		 * get place holder
		 */
		public byte[] getPlaceHolder()
		{
			if (WaveFormat == AC3_WaveFormat)
				return (new byte[HeaderLength_AC3]);

			return (new byte[0]);
		}

		/**
		 * get updated header 
		 */
		public byte[] getHeader()
		{ 
			if (WaveFormat == AC3_WaveFormat)
				return riffac3;

			return (new byte[0]);
		} 

		/**
		 * set wave data
		 */
		public void setWaveData(int[] riffdata)
		{
			Samples += riffdata[2]; 
			SampleCount++;

			int nSamplesPerSec = getValue(riffac3, 24, 4, true);
			int nBlockAlign    = getValue(riffac3, 32, 2, true);

			//nBlockAlign
			if (nBlockAlign == 0)  
				setValue(riffac3, 32, 2, true, riffdata[8]);

			else if (nBlockAlign != 1 &&  nBlockAlign != riffdata[8])
				setValue(riffac3, 32, 2, true, 1);

			//nSamplesPerSec
			if (nSamplesPerSec == 1)
				setValue(riffac3, 24, 4, true, riffdata[2]);

			else if (nSamplesPerSec != 0 &&  nSamplesPerSec != riffdata[2]) 
				setValue(riffac3, 24, 4, true, 0);

			// nChannels
			if ((0xFF & riffac3[22]) < riffdata[4])   
				riffac3[22] = (byte) riffdata[4];    
		}

		/**
		 * 
		 */
		public void setWaveLength(long filelength, long timelength)
		{
			int lengthAC3 = (int)filelength - HeaderLength_AC3;

			for (int i = 0; i < 4; i++)
			{
				riffac3[4 + i] = (byte)(0xFF & (lengthAC3 + 38)>>>(i * 8));
				riffac3[42 + i] = (byte)(0xFF & lengthAC3>>>(i * 8));
			}

			if (filelength <= 100)
				return;

			int time = (int)timelength;
			int nAvgBytePerSecAC3 = (int)(1000L * lengthAC3 / time);

			setValue(riffac3, 28, 4, true, nAvgBytePerSecAC3);
		}
	}

}