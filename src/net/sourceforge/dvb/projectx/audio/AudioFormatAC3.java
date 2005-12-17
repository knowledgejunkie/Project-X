/*
 * @(#)AudioFormatAC3.java - parse Audioheaders, ac3
 *
 * Copyright (c) 2003-2005 by dvb.matt, All Rights Reserved.
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

import net.sourceforge.dvb.projectx.audio.AudioFormat;

public class AudioFormatAC3 extends AudioFormat {
		
	public AudioFormatAC3()
	{
		super();
	}

	private int[] ac3_frequency_index = { 48000, 44100, 32000, 0 };
	
	private int[] ac3_bitrate_index =  { 
		32000,40000,48000,56000,64000,80000,96000,
		112000,128000,160000,192000,224000,256000,
		320000,384000,448000,512000,576000,640000,
		0,0,0,0,0,0,0,0,0,0,0,0,0  // (fix4)
	};
	
	private int[][] ac3_size_table = {
		{ 128,160,192,224,256,320,384,448,512,640,768,896,1024,1280,1536,1792,2080,2304,2560 },
		{ 138,174,208,242,278,348,416,486,556,696,834,974,1114,1392,1670,1950,2228,2506,2786 },
		{ 192,240,288,336,384,480,576,672,768,960,1152,1344,1536,1920,2304,2688,3120,3456,3840 }
	};
	
	private String[] bsmod = {	", CM" , ", ME" , ", K:VI" , ", K:HI" , ", K:D" , ", K:C" , ", K:E" , ", K:VO" };
	private String[] cmixlev = { "", ", cm -3.0dB", ", cm -4.5dB", ", cm -6.0dB", ", cm -4.5dB" };
	private String[] surmixlev = {	"", ", sm -3dB", ", sm -6dB", ", sm 0dB", ", sm -6dB" };
	private String[] dsurmod = { "" , ", notDS" , ", DS" , "" };
	private String[] acmod = { "1+1", "1/0", "2/0", "3/0", "2/1", "3/1", "2/2", "3/2" };
	private String[][] lfe = {{ ".0", ".1" },{ "", "lfe" }};

	private int[] ac3_channels = { 	2, 1, 2, 3, 3, 4, 4, 5 };
	
	/**
	 * parse ac3 Header 
	 */
	public int parseHeader(byte[] frame, int pos)
	{
		if ( (0xFF & frame[pos]) != 0xB || (0xFF & frame[pos+1]) != 0x77 ) 
			return -1;
	
		ID = 0;
		Emphasis = 0;
		Private_bit = 0;

		Protection_bit = 0 ^ 1;

		if ( (Sampling_frequency = ac3_frequency_index[3 & frame[pos+4]>>>6]) < 1) 
			return -4;

		if ( (Bitrate = ac3_bitrate_index[0x1F & frame[pos+4]>>>1]) < 1) 
			return -3;
	
		Layer = 7 & frame[pos+5];       //bsmod
		Padding_bit = 1 & frame[pos+4];
		Mode = 7 & frame[pos+6]>>>5;
		Mode_extension = 0;
	
		int mode = (0xFF & frame[pos+6])<<8 | (0xFF & frame[pos+7]);
		int skip=0;

		if ( (Mode & 1) > 0 && Mode != 1)  // cmix
		{
			Emphasis = 1 + (3 & frame[pos+6]>>>3);
			skip++;
		}

		if ( (Mode & 4) > 0) //surmix
		{
			Private_bit = 1 + (3 & frame[pos+6]>>>(skip > 0 ? 1 : 3));
			skip++;
		}

		if ( Mode == 2 )
		{
		        Mode_extension |= 6 & mode>>>(10 - (2 * skip));  //DS
			skip++;
		}

		if (skip < 4)
		{
		        Mode_extension |= 1 & mode>>>(12 - (2 * skip)); //lfe
			Original = 0x1F & mode>>>(7 - (2 * skip)); //dialnorm
		}
	
		Channel = ac3_channels[Mode] + (1 & Mode_extension);
		Copyright = 0;
		Time_length = 138240000.0 / Sampling_frequency;
		Size = (Size_base = ac3_size_table[3 & frame[pos+4]>>>6][0x1F & frame[pos+4]>>>1]) + Padding_bit * 2;

		return 1;
	}
	
	/**
	 * parse next ac3 Header 
	 */
	public int parseNextHeader(byte[] frame, int pos)
	{
	
		if ( (0xFF & frame[pos]) != 0xB || (0xFF & frame[pos+1]) != 0x77 ) 
			return -1;
	
		nID = 0;
		nEmphasis = 0;
		nPrivate_bit = 0;

		nProtection_bit = 0 ^ 1;
	
		if ( (nSampling_frequency = ac3_frequency_index[3 & frame[pos+4]>>>6]) < 1) 
			return -4;
	
		if ( (nBitrate = ac3_bitrate_index[0x1F & frame[pos+4]>>>1]) < 1) 
			return -3;
	
		nLayer = 7 & frame[pos+5];       //bsmod
		nPadding_bit = 1 & frame[pos+4];
	
		nMode = 7 & frame[pos+6]>>>5;
		int mode = (0xFF & frame[pos+6])<<8 | (0xFF & frame[pos+7]);
		int skip=0;
	
		if ( (nMode & 1) > 0 && nMode != 1)
		{  //cmix
			nEmphasis = 1 + (3 & frame[pos+6]>>>3);
			skip++;
		}

		if ( (nMode & 4) > 0)
		{  //surmix
			nPrivate_bit = 1 + (3 & frame[pos+6]>>>(skip > 0 ? 1 : 3));
			skip++;
		}
	
		if ( nMode == 2 )
		{  //DS mode
		        nMode_extension |= 6 & mode>>>(10 - (2 * skip));  //DS
			skip++;
		}

		if (skip < 4)
		{
		        nMode_extension |= 1 & mode>>>(12 - (2 * skip)); //lfe
			nOriginal = 0x1F & mode>>>(7 - (2 * skip)); //dialnorm
		}
	
		nChannel = ac3_channels[Mode] + (1 & nMode_extension);
		nCopyright = 0;
		nTime_length = 138240000.0 / nSampling_frequency;
		nSize = (nSize_base = ac3_size_table[3 & frame[pos+4]>>>6][5 & frame[pos+4]>>>1]) + nPadding_bit * 2;

		return 1;
	}
	
	/**
	 * compare current & last ac3 header 
	 */
	public int compareHeader()
	{
		if (lLayer != Layer)
			return 1;

		else if (lBitrate != Bitrate) 
			return 2;

		else if (lSampling_frequency != Sampling_frequency) 
			return 3;

		else if (lMode != Mode)
			return 4;

		else if (lMode_extension != Mode_extension)
			return 5;

		else if (lOriginal != Original)
			return 6;

		else if (lEmphasis != Emphasis)
			return 7;

		else 
			return 0;
	}
	
	/**
	 * display last ac3 header 
	 */
	public String displayHeader()
	{
		return ("AC-3" + bsmod[lLayer] + 
			", " + 
			acmod[lMode] + 
			lfe[1][1 & lMode_extension] + 
			"(" + 
			ac3_channels[lMode] + 
			lfe[0][1 & lMode_extension] + 
			")" + 
			", dn -" + lOriginal + "dB" +
			dsurmod[lMode_extension>>>1] + 
			cmixlev[lEmphasis] + 
			surmixlev[lPrivate_bit] + 
			", " + 
			lSampling_frequency + "Hz, " + 
			(lBitrate / 1000) + "kbps");
	}

	/**
	 * validate crc16 1 + 2
	 */
	public int validateCRC(byte[] frame, int offset, int len)
	{
		int[] g = { 1,0,1,0,0,0,0,0,0,0,0,0,0,0,0,1,1 }; // x^16 + x^15 + x^2 + 1

		int[] shift_reg = new int[16];
		int crc = 0;
		int crc1 = 0;
		int nr_bits = (len - offset) * 8;

		int words = len>>>1; //to word
		int crc1_len = (words>>>1) + (words>>>3);

		crc1_len <<= 4; //from word to bit
		crc1_len -= (offset<<3); // exclude byte offset

		for (int bit_count = 0, bit_in_byte = 0, data_bit; bit_count < nr_bits; bit_count++)
		{
			if (bit_count == crc1_len)
			{
				crc1 = 0;
				for (int i = 0; i < 16; i++)
					crc1 = ((crc1 << 1) | (shift_reg[15 - i]));

				if (crc1 != 0)
					return 1;
			}

			data_bit = (frame[offset] & 0x80>>>(bit_in_byte++)) != 0 ? 1 : 0;

			if ((bit_in_byte &= 7) == 0)
				offset++;

			data_bit ^= shift_reg[15];

			for (int i = 15; i > 0; i--)
				shift_reg[i] = g[i]==1 ? (shift_reg[i-1] ^ data_bit) : shift_reg[i-1];

			shift_reg[0] = data_bit;
		}

		for (int i=0; i<16; i++)
			crc = ((crc << 1) | (shift_reg[15-i]));

		if (crc != 0)
			return 2;

		else
			return 0;
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
	public int[] parseRiffData(byte[] frame)
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

		return riffdata;
	}

}