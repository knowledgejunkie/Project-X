/*
 * @(#)Audio.java - parse Audioheaders
 *
 * Copyright (c) 2003-2005 by dvb.matt, All Rights Reserved.
 * 
 * This file is part of X, a free Java based demux utility.
 * X is intended for educational purposes only, as a non-commercial test project.
 * It may not be used otherwise. Most parts are only experimental.
 * 
 * The part of audio parsing was derived from the MPEG/Audio
 * Software Simulation Group's audio codec and ATSC A/52 in a special modified manner.
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

package net.sourceforge.dvb.projectx.audio;

import java.util.Arrays;
import java.util.ArrayList;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.ByteArrayOutputStream;

import net.sourceforge.dvb.projectx.common.Common;

public class Audio extends Object {
		
	int ID=0;
	public int Layer=0;
	int Protection_bit=0;
	public int Bitrate=0;
	public int Sampling_frequency=0;
	int Padding_bit=0;
	int Private_bit=0;
	public int Mode=0;
	int Mode_extension=0;
	int Copyright=0;
	int Original=0;
	int Channel=0;
	public int Emphasis=0;
	public int Size=0;
	public int Size_base=0;
	int Bound=0; //DM10042004 081.7 int01 add
	int Sblimit; //DM10042004 081.7 int01 add
	public double Time_length=0.0;
	
	int nID=0;
	int nLayer=0;
	int nProtection_bit=0;
	int nBitrate=0;
	int nSampling_frequency=0;
	int nPadding_bit=0;
	int nPrivate_bit=0;
	int nMode=0;
	int nMode_extension=0;
	int nCopyright=0;
	int nOriginal=0;
	int nChannel=0;
	int nEmphasis=0;
	int nSize=0;
	int nSize_base=0;
	double nTime_length=0;
	
	int lID=0;
	int lLayer=0;
	int lProtection_bit=0;
	int lBitrate=0;
	int lSampling_frequency=0;
	int lPadding_bit=0;
	int lPrivate_bit=0;
	int lMode=0;
	public int lMode_extension=0;
	int lCopyright=0;
	int lOriginal=0;
	int lChannel=0;
	int lEmphasis=0;
	int lSize=0;
	int lSize_base=0;
	double lTime_length=0;
	
	int bitrate_index[][][] = {{
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
	
	int frequency_index[][] = {
		{ 22050,24000,16000,0 },	//MPG2 - 22.05,24,16khz
		{ 44100,48000,32000,0 },	//MPG1 - 44.1 ,48,32khz
		{ 11025,12000,8000,0 }		//MPG2.5 - 11.025,12,8khz
	};
	
	double time_index[] = { 0.0,103680000.0,103680000.0,34560000.0 };	//L3,L2,L1 * 90
	
	String[] dID = { 
		"MPEG-2","MPEG-1","MPEG-2.5" 
	};
	String[] dLayer = { 
		"n.a.","Layer3","Layer2","Layer1" 
	};
	String[] dCRC = { 
		"noCRC","CRC" 
	};
	String[] dMode = { 
		"stereo","jstereo","dual","mono" 
	};
	
	
	/*** parse mpa Header ***/
	//DM10042004 081.7 int01 changed
	public int MPA_parseHeader(byte[] frame, int pos)
	{
		int sblimit = 32;
	
		if ( (0xFF & frame[pos]) != 0xFF || (0xF0 & frame[pos + 1]) != 0xF0 ) 
			return -1;
	
		ID = 1 & frame[pos + 1]>>>3;
	        Emphasis = 3 & frame[pos + 3];
	
		if (ID == 1 && Emphasis == 2)
			ID = 2;
	
		if ( (Layer = 3 & frame[pos + 1]>>>1) < 1) 
			return -2;
	
		Protection_bit = (1 & frame[pos + 1]) ^ 1;
	
		if ( (Bitrate = bitrate_index[ID][Layer - 1][0xF & frame[pos + 2]>>>4]) < 1) 
			return -3;
	
		if ( (Sampling_frequency = frequency_index[ID][3 & frame[pos + 2]>>>2]) == 0) 
			return -4;
	
		Padding_bit = 1 & frame[pos + 2]>>>1;
		Private_bit = 1 & frame[pos + 2];
	
		Mode = 3 & frame[pos + 3]>>>6;
		Mode_extension = 3 & frame[pos + 3]>>>4;

		if (Mode == 0) 
			Mode_extension = 0;
	
		Bound = Mode == 1 ? ((Mode_extension + 1) << 2) : sblimit;
		Channel = Mode == 3 ? 1 : 2;
		Copyright = 1 & frame[pos + 3]>>>3;
		Original = 1 & frame[pos + 3]>>>2;
		Time_length = time_index[Layer] / Sampling_frequency;
	
		if (ID == 1 && Layer == 2)   // MPEG-1, L2 restrictions
		{
			if (Bitrate / Channel < 32000) 
				return -5; /* unsupported bitrate */

			if (Bitrate / Channel > 192000) 
				return -6; /* unsupported bitrate */
	
			if (Bitrate / Channel < 56000)
			{
				if(Sampling_frequency == 32000) 
					Sblimit = 12;
				else 
					Sblimit = 8;
			}

			else if (Bitrate / Channel < 96000) 
				Sblimit = 27;

			else
			{
				if (Sampling_frequency == 48000) 
					Sblimit = 27;
				else 
					Sblimit = 30;
			}

			if (Bound > Sblimit) 
				Bound = Sblimit;
		}

		else if (Layer == 2)  // MPEG-2
		{
			Sblimit = 30;
		}

		if (Layer < 3)
		{
			if (Bound > Sblimit) 
				Bound = Sblimit;

			Size = (Size_base = 144 * Bitrate / Sampling_frequency) + Padding_bit;

			return 1;
		}
		else
		{
			Sblimit = 32;
			Size = (Size_base = (12 * Bitrate / Sampling_frequency) * 4) + (4 * Padding_bit);

			return 2;
		}
	}
	
	/*** parse next mpa Header ***/
	public int MPA_parseNextHeader(byte[] frame, int pos) {
	
	        if ( (0xFF&frame[pos])!=0xFF || (0xF0&frame[pos+1])!=0xF0 ) 
			return -1;
	
		nID = 1&frame[pos+1]>>>3;
		nEmphasis = 3&frame[pos+3];
	
		if (nID==1 && nEmphasis==2)
			nID = 2;
	
	        if ( (nLayer = 3&frame[pos+1]>>>1) < 1) 
			return -2;
	
	        nProtection_bit = (1&frame[pos+1]) ^ 1;
	
		if ( (nBitrate = bitrate_index[nID][nLayer-1][0xF&frame[pos+2]>>>4]) < 1) 
			return -3;
	
	        if ( (nSampling_frequency = frequency_index[nID][3&frame[pos+2]>>>2]) == 0) 
			return -4;
	
	        nPadding_bit = 1&frame[pos+2]>>>1;
	        nPrivate_bit = 1&frame[pos+2];
	
	        nMode = 3&frame[pos+3]>>>6;
	        nMode_extension = 3&frame[pos+3]>>>4;
	        if (nMode==0) 
			nMode_extension=0;
	
		nChannel = (nMode==3) ? 1: 2;
		nCopyright = 1&frame[pos+3]>>>3;
		nOriginal = 1&frame[pos+3]>>>2;
		nTime_length = time_index[nLayer]/nSampling_frequency;
	
		if (nID==1 && nLayer==2) {	// MPEG-1,L2 restrictions
			if(nBitrate/Channel < 32000) 
				return -5; /* unsupported bitrate */
			if(nBitrate/Channel > 192000) 
				return -6; /* unsupported bitrate */
		}
	
		if (nLayer<3) {
			nSize = (nSize_base = 144*nBitrate/nSampling_frequency) + nPadding_bit;
			return 1;
		} else {
			nSize = (nSize_base = (12*nBitrate/nSampling_frequency)*4) + (4*nPadding_bit);
			return 2;
		}
	}
	
	/*** save last mpa header ***/
	public void saveHeader() {
		lID=ID;
		lLayer=Layer;
		lProtection_bit=Protection_bit;
		lBitrate=Bitrate;
		lSampling_frequency=Sampling_frequency;
		lPadding_bit=Padding_bit;
		lPrivate_bit=Private_bit;
		lMode=Mode;
		lMode_extension=Mode_extension;
		lCopyright=Copyright;
		lOriginal=Original;
		lChannel=Channel;
		lEmphasis=Emphasis;
		lSize=Size;
		lSize_base=Size_base;
		lTime_length=Time_length;
	}
	
	/*** verify current & last mpa header ***/
	public int MPA_compareHeader() {
		if (lID!=ID) 
			return 1;
		else if (lLayer!=Layer) 
			return 2;
		else if (lBitrate!=Bitrate) 
			return 3;
		else if (lSampling_frequency!=Sampling_frequency) 
			return 4;
		else if (lProtection_bit!=Protection_bit) 
			return 5;
		else if (lMode!=Mode){  //DM01112003 081.5++ fix
			if (Mode+lMode<2)
				return 6;
			else
				return 7;
		}else 
			return 0;
	}
	
	/*** display last mpa header ***/
	public String MPA_displayHeader() {
		return ("" + dID[lID] + ", " + dLayer[lLayer] + ", " + lSampling_frequency + "Hz, " + dMode[lMode] + ", "+ (lBitrate/1000) + "kbps, " + dCRC[lProtection_bit]);
	}
	
	/*** display last mpa header ***/
	public String MPA_saveAnddisplayHeader() {
		saveHeader();
		return MPA_displayHeader();
	}
	
	/**
	 * remove CRC from mpa 
	 **/
	public void MPA_deleteCRC(byte[] frame)
	{
		MPA_removePrivateBit(frame);

		if ((frame[1] & 1) == 1) 
			return;

		System.arraycopy(frame, 6, frame, 4, frame.length - 6);
		Arrays.fill(frame, frame.length - 2, frame.length, (byte) 0);

		frame[1] |= 1;

		Protection_bit = 1;
	}
	
	/**
	 * remove private Bit from mpa 
	 **/
	public void MPA_removePrivateBit(byte[] frame)
	{
		//MPA_deleteCRC(frame);

		if ( (frame[2] & 1) == 0) 
			return;

		frame[2] &= ~1;

		Private_bit = 0;
	}
	
	int ac3_frequency_index[] = { 
		48000,44100,32000,0 
	};
	
	int ac3_bitrate_index[] =  { 
		32000,40000,48000,56000,64000,80000,96000,
		112000,128000,160000,192000,224000,256000,
		320000,384000,448000,512000,576000,640000,
		0,0,0,0,0,0,0,0,0,0,0,0,0  // (fix4)
	};
	
	int ac3_size_table[][] = {
		{ 128,160,192,224,256,320,384,448,512,640,768,896,1024,1280,1536,1792,2080,2304,2560 },
		{ 138,174,208,242,278,348,416,486,556,696,834,974,1114,1392,1670,1950,2228,2506,2786 },
		{ 192,240,288,336,384,480,576,672,768,960,1152,1344,1536,1920,2304,2688,3120,3456,3840 }
	};
	
	String bsmod[] = {
		", CM" , ", ME" , ", K:VI" , ", K:HI" , ", K:D" , ", K:C" , ", K:E" , ", K:VO"
	};
	String cmixlev[] = {
		"", ", cm -3.0dB", ", cm -4.5dB", ", cm -6.0dB", ", cm -4.5dB"
	};
	String surmixlev[] = {
		"", ", sm -3dB", ", sm -6dB", ", sm 0dB", ", sm -6dB"
	};
	String dsurmod[] = {
		"" , ", notDS" , ", DS" , ""
	};
	
	String acmod[] = {
		"1+1","1/0","2/0","3/0","2/1","3/1","2/2","3/2"
	};
	
	int ac3_channels[] = {
		2,1,2,3,3,4,4,5
	};
	
	String lfe[][] = {
		{ ".0",".1" },
		{ "","lfe" }
	};
	
	/*** parse ac3 Header ***/
	public int AC3_parseHeader(byte[] frame, int pos)
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
	
	/*** parse ac3 Header ***/
	public int AC3_parseNextHeader(byte[] frame, int pos)
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
	
	/*** verify current & last ac3 header ***/
	public int AC3_compareHeader()
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

		else 
			return 0;
	}
	
	/*** display last ac3 header ***/
	public String AC3_displayHeader()
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
	
	/*** display last ac3 header ***/
	public String AC3_saveAnddisplayHeader()
	{
		saveHeader();

		return AC3_displayHeader();
	}
	
	//start 081.6 int07
	/* DTS stuff taken from the VideoLAN project. */ 
	/* Added by R One, 2003/12/18. */ 
	int dts_frequency_index[] = { 
		0, 8000, 16000, 32000, 64000, 128000, 
		11025, 22050, 44100, 88200, 176400,
		12000, 24000, 48000, 96000, 192000 
	}; 
	
	int dts_bitrate_index[] = { 
		32000, 56000, 64000, 96000, 112000, 128000, 
		192000, 224000, 256000, 320000, 384000, 
		448000, 512000, 576000, 640000, 768000, 
		896000, 1024000, 1152000, 1280000, 1344000, 
		1408000, 1411200, 1472000, 1536000, 1920000, 
		2048000, 3072000, 3840000, 4096000, 0, 0 
	}; 
	 
	String dts_acmod[] = { 
		"1","DM","2/0","2/0", 
		"2/0","3/0","2.1/0","3.1/0", 
		"2/2","3/2","2/2/2","2/2/2", 
		"3/2/2","3.1/2/2","","", 
		"","","","","","","","","","","","","","","","", 
		"","","","","","","","","","","","","","","","", 
		"","","","","","","","","","","","","","","","" 
	}; 
	 
	int dts_channels[] = { 
		1,2,2,2, 2,3,3,4, 4,5,6,6, 7,8,0,0, 
		0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0, 
		0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0, 
		0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0 
	}; 
	 
	/*** parse dts Header ***/ 
	public int DTS_parseHeader(byte[] frame, int pos) { 
	
		if ( frame[pos]!=0x7f || frame[pos+1]!=(byte)0xfe || frame[pos+2]!=(byte)0x80 || frame[pos+3]!=1 ) 
			return -1; 
	
		ID = 0; 
		Emphasis = 0; 
		Protection_bit = 0 ^ 1; 
	
		if ( (Sampling_frequency = dts_frequency_index[0xF&(frame[pos+8]>>>2)]) < 1) 
			return -4; 
		Bitrate = dts_bitrate_index[((3&frame[pos+8])<<3)|(7&(frame[pos+9]>>>5))]; 
		if ( Bitrate < 1) 
			return -3; 
	
		Layer = 0; 
		Padding_bit = 0; 
		Private_bit = 0; 
	
		Mode = ((0xf&frame[pos+7])<<2) | ((0xc0&frame[pos+8])>>>6); 
		Mode_extension = 0; 
		Channel = dts_channels[Mode]; 
		Copyright = 0; 
		Original = 0; 
		Size = ((1&frame[pos+4])<<6) | ((0xfc&frame[pos+5])>>>2); 
		Size = (Size+1)<<5; 
		Time_length = 90000.0*Size/Sampling_frequency; 
		Size = ((3&frame[pos+5])<<12) | ((0xff&frame[pos+6])<<4) | ((0xf0&frame[pos+7])>>>4); 
		Size++;
		Size_base = Size; 
		return 1; 
	} 
	 
	/*** parse dts Header ***/ 
	public int DTS_parseNextHeader(byte[] frame, int pos) { 
	
		if ( frame[pos]!=0x7f || frame[pos+1]!=(byte)0xfe || frame[pos+2]!=(byte)0x80 || frame[pos+3]!=1 ) 
			return -1; 
	 
		nID = 0; 
		nEmphasis = 0; 
		nProtection_bit = 0 ^ 1; 
	
		if ( (nSampling_frequency = dts_frequency_index[0xF&(frame[pos+8]>>>2)]) < 1) 
			return -4; 
		if ( (nBitrate = dts_bitrate_index[((3&frame[pos+8])<<3)|(7&(frame[pos+9]>>>5))]) < 1)
			return -3; 
	
		nLayer = 0; 
		nPadding_bit = 0; 
		nPrivate_bit = 0; 
	
		nMode = ((0xf&frame[pos+7])<<2) | ((0xc0&frame[pos+8])>>>6); 
		nMode_extension = 0; 
		nChannel = dts_channels[nMode]; 
		nCopyright = 0; 
		nOriginal = 0; 
		nSize = ((1&frame[pos+4])<<6) | ((0xfc&frame[pos+5])>>>2); 
		nSize = (nSize+1)<<5; 
		nTime_length = 90000.0*nSize/nSampling_frequency; 
		nSize = ((3&frame[pos+5])<<12) | ((0xff&frame[pos+6])<<4) | ((0xf0&frame[pos+7])>>>4);
		nSize++;
		nSize_base = nSize; 
		return 1; 
	} 
	 
	/*** verify current & last dts header ***/ 
	public int DTS_compareHeader() { 
		if (lLayer!=Layer) 
			return 1; 
		else if (lBitrate!=Bitrate) 
			return 2; 
		else if (lSampling_frequency!=Sampling_frequency) 
			return 3; 
		else if (lMode!=Mode) 
			return 4; 
		else if (lMode_extension!=Mode_extension) 
			return 5; 
		else if (lSize!=Size) 
			return 6; 
		else 
			return 0; 
	} 
	 
	/*** display last dts header ***/ 
	public String DTS_displayHeader() { 
		return ("DTS, " + dts_acmod[lMode] + "(" + dts_channels[lMode] + "), " + lSampling_frequency + "Hz, " + (lBitrate/1000.0) + "kbps, " + lSize + "BpF"); 
	} 
	 
	/*** display last dts header ***/ 
	public String DTS_saveAnddisplayHeader() { 
		saveHeader(); 
		return DTS_displayHeader(); 
	}
	//ROne18122003 
	//end 081.6 int07
	
	
	//DM30122003 081.6 int10 add+ 
	//DM25012004 081.6 int11 changed
	final int WaveChunks[] = 
	{
		0x57415645, //0 'WAVE'
		0x63756520, //1 'cue '
		0x64617461, //2 'data'
		0x66616374, //3 'fact'
		0x666D7420, //4 'fmt '
		0x696E7374, //5 'inst'
		0x6C61626C, //6 'labl'
		0x6C697374, //7 'list'
		0x6C747874, //8 'ltxt'
		0x6E6F7465, //9 'note'
		0x706C7374, //10 'plst'
		0x736D706C //11 'smpl'
	};
	
	private boolean INTEL=false;
	
	private int littleEndian(byte[] data, int offset, int len, boolean INTEL)
	{
		int value=0;
		for (int a=0; a<len; a++)
			value|=INTEL?((0xFF&data[offset+a])<<(a*8)):((0xFF&data[offset+a])<<((len-1-a)*8));
		return value;
	}
	
	private int littleEndian(int data, int len)
	{
		if (!INTEL)	
			return data;
		if (len==4) 
			return ( (0xFF&data>>>24) | (0xFF&data>>>16)<<8 | (0xFF&data>>>8)<<16 | (0xFF&data)<<24 );
		else 
			return ( (0xFF&data>>>8) | (0xFF&data)<<8 );
	}
	
	
	/*** parse RIFF_WAVE Header ***/ 
	public int WAV_parseHeader(byte[] frame, int pos)
	{ 
		INTEL=false;
		if ( frame[pos]!=0x52 || frame[pos+1]!=0x49 || frame[pos+2]!=0x46 ) 
			return -1;
	
		if ( frame[pos+3]==0x46 )
			INTEL=true;
		else if ( frame[pos+3]!=0x58 )
			return -2;
	
		ID = INTEL?0:1; 
		Emphasis = 0; 
		Protection_bit = 0^1; 
	
		java.util.Arrays.sort(WaveChunks);
	
		if (littleEndian(frame,pos+8,4,false)!=WaveChunks[0])
			return -3;
	
		int chunk=0, chunksize=0;
		for (int a=pos+12; a<frame.length-4; a++){
			if (java.util.Arrays.binarySearch(WaveChunks,(chunk=littleEndian(frame,a,4,false)))<0)
				continue;
			if (chunk==WaveChunks[4]){ //fmt chunk read info datas
				chunksize = littleEndian(frame,a+4,4,INTEL);
				Layer = littleEndian(frame,a+8,2,INTEL);   // Compression type (1=PCM)
				Channel = littleEndian(frame,a+10,2,INTEL); // channels
				Sampling_frequency = littleEndian(frame,a+12,4,INTEL); // samplerate
				Bitrate = littleEndian(frame,a+16,4,INTEL)*8; // avg bits per second
				Mode = littleEndian(frame,a+20,2,INTEL);  // block align, bytes per sample
				Size = littleEndian(frame,a+22,2,INTEL); //bits per sample
				//extrabits not of interest
			}else if (chunk==WaveChunks[2]){ //data chunk, sample data
				chunksize = littleEndian(frame,a+4,4,INTEL);
				Size_base=chunksize; // length of whole sample data
				Emphasis=a+8; // real start of whole sample data
			}else{
				chunksize = littleEndian(frame,a+4,4,INTEL);
			}
			a+=chunksize+3;
		}
	
		//PTS low+high may exists in 'fact' of MPEG1audio !
	
		if (Bitrate<1 || Sampling_frequency<1 || Channel<1)
			return -4; 
	
		Padding_bit = 0; 
		Private_bit = 0; 
		Copyright = 0; 
		Original = 0; 
		Time_length = 90000.0/Sampling_frequency;
	
		switch (Layer){
		case 1: 
			Mode_extension = 1; 
			return 1;
		case 0x50: 
			Mode_extension = 2; 
			return 0;
		case 0x55: 
			Mode_extension = 3; 
			return 0;
		case 0x2000: 
			Mode_extension = 4; 
			return 0;
		default:
			Mode_extension = 0; 
		}
	
		return 0; 
	} 
	
	String LSB_mode[] = { "F","X" };
	String compression[] = { "","PCM","MPEG","MPEG-L3","AC3" };
	
	/*** display last wav header ***/ 
	public String WAV_displayHeader()
	{ 
		return ("RIF" + LSB_mode[lID] + ", " + (lMode_extension > 0 ? compression[lMode_extension] : "tag 0x" + Integer.toHexString(Layer)) + ", " + lChannel + "-ch, " + lSampling_frequency + "Hz, " + lSize + "bit, " + (lBitrate/1000.0) + "kbps"); 
	} 
	 
	/*** display last wav header ***/ 
	public String WAV_saveAnddisplayHeader()
	{ 
		saveHeader(); 
		return WAV_displayHeader(); 
	}
	//DM30122003 081.6 int10 add-
	
	/**
	 * returns RIFF
	 */
	public byte[] getRiffHeader()
	{
		byte RIFF[] = {
			0x52, 0x49, 0x46, 0x46, 0, 0, 0, 0, 
			0x57, 0x41, 0x56, 0x45, (byte)0x66, (byte)0x6d, (byte)0x74, 0x20,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
			0, 0, 0, 0, (byte)0x64, (byte)0x61, (byte)0x74, (byte)0x61, 
			0, 0, 0, 0
		};

		return RIFF;
	}

	/**
	 * updates RIFF
	 * returns playtime as int
	 */
	public long fillRiffHeader(String file) throws IOException
	{
		RandomAccessFile riff = new RandomAccessFile(file, "rw");

		int len = (int)riff.length() - 8;

		riff.seek(3);

		if (!INTEL)
			riff.write((byte)'X');

		riff.seek(4);
		riff.writeInt(littleEndian(len, 4));  //data+chunksize

		riff.seek(16);
		riff.writeInt(littleEndian(0x10, 4));  //chunk length
		riff.writeShort(littleEndian(1, 2));   //pcm
		riff.writeShort((short)littleEndian(lChannel, 2)); //channels
		riff.writeInt(littleEndian(lSampling_frequency, 4));  //sample_freq
		riff.writeInt(littleEndian(lBitrate / 8, 4)); //byterate
		riff.writeShort((short)littleEndian(lMode, 2)); //blockalign
		riff.writeShort((short)littleEndian(lSize, 2)); //bits_per_sample

		riff.seek(40);
		riff.writeInt(littleEndian(len - 36, 4));  //data-size
	
		riff.close();

		return ((8000L * (len - 36)) / lBitrate);
	}
	
	
	//DM3003004 081.6 int18 add+
	int lpcm_frequency_index[] = { 
		48000, 96000
	};
	
	int lpcm_bps_index[] = { 
		16, 20, 24, -1
	};
	
	public int LPCM_parseHeader(byte[] frame_header, int pos)
	{ 
		INTEL=true; // force intel/wav output
	
		ID = 0xFF & frame_header[pos]; // no of frameheaders
		Padding_bit = (0xFF & frame_header[pos + 1])<<8 | (0xFF & frame_header[pos + 2]); // first_access_unit_pointer 
		Layer = 0xFF & frame_header[pos + 3]; // audio_frame_number 
	
		Protection_bit = 0^1; 
		Private_bit = 0; 
		Copyright = 0; 
		Original = 0; 
		Size_base = 0;
	
		Size = lpcm_bps_index[(3 & frame_header[pos + 4]>>>6)]; //bits per sample
		Sampling_frequency = lpcm_frequency_index[(1 & frame_header[pos + 4]>>>4)]; // samplerate
		Channel = 1 + (7 & frame_header[pos + 4]); // channels
		Emphasis = 0xFF & frame_header[pos + 5]; // dynamic_range
		Mode = (Channel * Size) / 8;  // block_align, bytes per sample
	
		Bitrate =  Channel * Sampling_frequency * Size; // bitrate per second
	
		if (Size < 1)
			return -1; 
	
		Time_length = 90000.0 / Sampling_frequency;  // 1 frame = 150 * timelength
	
		return 0; 
	} 
	
	public String LPCM_displayHeader()
	{ 
		return ("LPCM, DR-" + lEmphasis + ", " + lChannel + "-ch, " + lSampling_frequency + "Hz, " + lSize + "bit, " + (lBitrate / 1000.0) + "kbps");
	} 
	 
	public String LPCM_saveAnddisplayHeader()
	{ 
		saveHeader(); 
		return LPCM_displayHeader(); 
	}
	
	public int LPCM_compareHeader()
	{
		if (lChannel != Channel) 
			return 1; 
		else if (lSampling_frequency != Sampling_frequency) 
			return 2; 
		else if (lSize != Size) 
			return 3; 
		else if (lEmphasis != Emphasis) 
			return 4; 
		else 
			return 0; 
	} 
	//DM3003004 081.6 int18 add-





	/**
	 * RDS-Test, cant find any similars to RDS, I-RDS, RBDS i.e. group/block coding
	 * PI-Code is missing, WDR2 shall have Dx92
	 *
	 */
	ArrayList _list = new ArrayList();
	ByteArrayOutputStream bo = new ByteArrayOutputStream();
	private boolean DecodeRDS = false;
	private boolean Debug = false;

	public void initRDSDecoding(boolean b, boolean b1)
	{
		DecodeRDS = b;
		Debug = b1;
	}

	public void testRDS(byte[] frame)
	{
		if (!DecodeRDS)
			return;

		int neg_offs = frame.length - 1;

		if (frame[neg_offs] != (byte)0xFD)
		{
			neg_offs -= 2;

			if (frame[neg_offs] != (byte)0xFD)
				return;
		}

		int len = frame[neg_offs - 1];

		for (int i = neg_offs - 2, val; i > neg_offs - 2 - len; i--)
		{
			val = 0xFF & frame[i];
			_list.add("" + val);
		}

		decodeChunk(_list);
	}

	/**
	 * 
	 */
	private void decodeChunk(java.util.ArrayList list)
	{
		int index = list.indexOf("254"); //0xfe, start

		if (index < 0)
		{
			list.clear();
			return;
		}

		while (index > 0)
		{
			list.remove(0);
			index--;
		}

		int eom_index = list.indexOf("255"); //0xff, end

		if (eom_index < 0)
			return;

		int chunklen = 0;

		for (int i = 0; i < 4; i++)
			chunklen |= Integer.parseInt(list.get(1 + i).toString())<<(8 * (3 - i));

		chunklen ++;   //start_marker
		chunklen += 4; //len field itself
		chunklen += 2; //crc

		if (eom_index < chunklen)
		{
			list.remove(0);
			return;
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

		int type = -1;

		for (int i = 0; i <= eom_index; i++, list.remove(0))
		{
			if (i < 5 || i > eom_index - 3)
				continue;

			int value = Integer.parseInt(list.get(0).toString());

			if (i == 5)
			{
				type = value;
				continue;
			}

			bo.write(value);
		}

		switch (type)
		{
		case 0x0A: //RT
			printRT(bo.toByteArray());
			break;

		case 0x02: //PS program service name 
			printPS(bo.toByteArray());
			break;

		case 0x0D: //? 
		case 0x30: //? 
		case 0x40: //CT calendar
		case 0x42: //? 
		case 0x46: //?
			break;
		}

		bo.reset();
	}

	/**
	 * 
	 */
	private void printRT(byte[] array)
	{
		int index = 0;

		int offs = (0xFF & array[index])<<8 | (0xFF & array[index + 1]);

		index += 2;

		int len = 0xFF & array[index];

		index++;

		int change = 0xFF & array[index];

		index++;

		replaceChars(array, index, len);

		String str = new String(array, index, len - 1);

		Common.setMessage("-> RT (" + change + "): '" + str.trim() + "'");
	}

	/**
	 * 
	 */
	private void printPS(byte[] array)
	{
		int index = 0;

		int offs = (0xFF & array[index])<<8 | (0xFF & array[index + 1]);

		index += 2;

		int len = array.length >= index + 8 ? 8 : array.length - index;

		replaceChars(array, index, len);

		String str = new String(array, index, len - 1);

		Common.setMessage("-> PS : '" + str.trim() + "'");
	}

	/**
	 * 
	 */
	private void replaceChars(byte[] array, int offset, int length)
	{
		for (int i = offset; i < length; i++)
		{
			switch (0xFF & array[i])
			{
			case 0x91:
				array[i] = (byte)0xE4; //ä
				break;
			case 0x92:
			case 0x93:
			case 0x94:
			case 0x95:
			case 0x96:
				break;
			case 0x97:
				array[i] = (byte)0xF6; //ö
				break;
			case 0x98:
				break;
			case 0x99:
				array[i] = (byte)0xFC; //ü
				break;
			case 0x9A:
			case 0x9B:
			case 0x9C:
			case 0x9D:
			case 0x9E:
			case 0x9F:
				break;

			case 0x81:
			case 0x82:
			case 0x83:
			case 0x84:
			case 0x85:
			case 0x86:
			case 0x87:
			case 0x88:
			case 0x89:
			case 0x8A:
			case 0x8B:
			case 0x8C:
			case 0x8D:
			case 0x8E:
			case 0x8F:
				break;
			}
		}
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
	public int[] AC3RiffFormat(byte[] frame)
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
	public int[] RiffFormat(byte[] rh)
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

		return riffdata;
	}
}