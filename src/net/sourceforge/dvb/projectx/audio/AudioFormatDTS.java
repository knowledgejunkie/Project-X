/*
 * @(#)AudioFormatDTS.java - parse Audioheaders, dts
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

import net.sourceforge.dvb.projectx.audio.AudioFormat;

public class AudioFormatDTS extends AudioFormat {


	public AudioFormatDTS()
	{
		super();
	}


	/* DTS stuff taken from the VideoLAN project. */ 
	/* Added by R One, 2003/12/18. */ 
	private int[] dts_frequency_index = { 
		0, 8000, 16000, 32000, 64000, 128000, 
		11025, 22050, 44100, 88200, 176400,
		12000, 24000, 48000, 96000, 192000 
	}; 
	
	/**
	 *
	 */
	private int[] dts_bitrate_index = { 
		32000, 56000, 64000, 96000, 112000, 128000, 
		192000, 224000, 256000, 320000, 384000, 
		448000, 512000, 576000, 640000, 768000, 
		896000, 1024000, 1152000, 1280000, 1344000, 
		1408000, 1411200, 1472000, 1536000, 1920000, 
		2048000, 3072000, 3840000, 4096000, 0, 0 
	}; 
	 
	/**
	 *
	 */
	private String[] dts_acmod = { 
		"1", "DM", "2/0", "2/0", "2/0", "3/0", "2.1/0", "3.1/0", 
		"2/2", "3/2", "2/2/2", "2/2/2", "3/2/2", "3.1/2/2", "","",
		"","","","","","","","","","","","","","","","", 
		"","","","","","","","","","","","","","","","", 
		"","","","","","","","","","","","","","","","" 
	}; 

	/**
	 *
	 */
	private int[] dts_channels = { 
		1,2,2,2, 2,3,3,4, 4,5,6,6, 7,8,0,0, 
		0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0, 
		0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0, 
		0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0 
	}; 
	 
	/**
	 * parse dts Header 
	 */ 
	public int parseHeader(byte[] frame, int pos)
	{ 
		if (frame[pos] != 0x7F || frame[pos + 1] != (byte)0xFE || frame[pos + 2] != (byte)0x80 || frame[pos + 3] != 1 ) 
			return -1; 
	
		setID(0); 
		setEmphasis(0); 
		setProtectionBit(0 ^ 1); 
	
		setSamplingFrequency(dts_frequency_index[0xF & (frame[pos + 8]>>>2)]);

		if (getSamplingFrequency() < 1) 
			return -4; 

		setBitrate(dts_bitrate_index[((3 & frame[pos + 8])<<3) | (7 & (frame[pos + 9]>>>5))]); 

		if (getBitrate() < 1) 
			return -3; 
	
		setLayer(0); 
		setPaddingBit(0); 
		setPrivateBit(0); 
	
		setMode(((0xF & frame[pos + 7])<<2) | ((0xC0 & frame[pos + 8])>>>6)); 
		setModeExtension(0); 
		setChannel(dts_channels[getMode()]); 
		setCopyright(0); 
		setOriginal(0); 
		setSize(((1 & frame[pos + 4])<<6) | ((0xFC & frame[pos + 5])>>>2)); 
		setSize((getSize() + 1)<<5); 
		setFrameTimeLength(90000.0 * getSize() / getSamplingFrequency()); 
		setSize(((3 & frame[pos + 5])<<12) | ((0xFF & frame[pos + 6])<<4) | ((0xF0 & frame[pos + 7])>>>4)); 
		setSize(getSize() + 1);
		setSizeBase(getSize()); 

		return 1; 
	} 
	 
	/**
	 * parse dts Header 
	 */ 
	public int parseNextHeader(byte[] frame, int pos)
	{ 
		if (frame[pos] != 0x7F || frame[pos + 1] != (byte)0xFE || frame[pos + 2] != (byte)0x80 || frame[pos + 3] != 1 ) 
			return -1; 
	
		setNextID(0); 
		setNextEmphasis(0); 
		setNextProtectionBit(0 ^ 1); 
	
		setNextSamplingFrequency(dts_frequency_index[0xF & (frame[pos + 8]>>>2)]);

		if (getNextSamplingFrequency() < 1) 
			return -4; 

		setNextBitrate(dts_bitrate_index[((3 & frame[pos + 8])<<3) | (7 & (frame[pos + 9]>>>5))]); 

		if (getNextBitrate() < 1) 
			return -3; 
	
		setNextLayer(0); 
		setNextPaddingBit(0); 
		setNextPrivateBit(0); 
	
		setNextMode(((0xF & frame[pos + 7])<<2) | ((0xC0 & frame[pos + 8])>>>6)); 
		setNextModeExtension(0); 
		setNextChannel(dts_channels[getNextMode()]); 
		setNextCopyright(0); 
		setNextOriginal(0); 
		setNextSize(((1 & frame[pos + 4])<<6) | ((0xFC & frame[pos + 5])>>>2)); 
		setNextSize((getNextSize() + 1)<<5); 
		setNextFrameTimeLength(90000.0 * getNextSize() / getNextSamplingFrequency()); 
		setNextSize(((3 & frame[pos + 5])<<12) | ((0xFF & frame[pos + 6])<<4) | ((0xF0 & frame[pos + 7])>>>4)); 
		setNextSize(getNextSize() + 1);
		setNextSizeBase(getNextSize()); 

		return 1; 
	} 
	 
	/**
	 * verify current & last dts header 
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

		else if (getLastSize() != getSize()) 
			return 0x40; 

		else 
			return 0; 
	} 
	 
	/**
	 * display last dts header 
	 */ 
	public String displayHeader()
	{ 
		return ("DTS, " + dts_acmod[getLastMode()] + "(" + dts_channels[getLastMode()] + "), " + getLastSamplingFrequency() + "Hz, " + (getLastBitrate() / 1000.0) + "kbps, " + getLastSize() + "BpF"); 
	} 
	//ROne18122003 


}