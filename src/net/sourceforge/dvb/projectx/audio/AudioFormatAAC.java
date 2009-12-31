/*
 * @(#)AudioFormatAAC.java - parse Audioheaders,
 *
 * Copyright (c) 2007-2008 by dvb.matt, All Rights Reserved.
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
import net.sourceforge.dvb.projectx.common.Common;

public class AudioFormatAAC extends AudioFormat {

//unused !!

	public AudioFormatAAC()
	{
		super();
	}

	/**
	 *
	 */
	private int[] frequency_index = { 
		96000, 88200, 64000, 48000, 44100, 32000,
		24000, 22050, 16000, 12000, 11025, 8000, 0, 0, 0, 0
	}; 

	
	/**
	 *
	 */
	private int[] bitrate_index = { 
		32000, 56000, 64000, 96000, 112000, 128000, 
		192000, 224000, 256000, 320000, 384000, 
		448000, 512000, 576000, 640000, 768000, 
	}; 
	 
	/**
	 *
	 */
	private String[] acmod = { 
		"1", "DM", "2/0", "2/0"
	}; 

	/**
	 *
	 */
	private int[] channels = { 
		1,2,2,2, 2,3,3,4, 4,5,6,6, 7,8,0,0, 
	}; 
	 
	/**
	 * parse aac Header 
	 */ 
	public int parseHeader(byte[] frame, int pos)
	{ 
		boolean latm = false;
		int aac_length = 0;

		// 0x2B7 LATM
		latm = frame[pos] == 0x56 && (0xE0 & frame[pos + 1]) == 0xE0;

		if (latm)
		{
			aac_length = 3 + ((0x1F & frame[pos + 1])<<8 | (0xFF & frame[pos + 2]));
			pos += 3;
		}

		if (frame.length == 4) //pushmpa = 4!
			return (latm && (0xFF & frame[pos]) == 0xFF ? aac_length : -1);

		//syncword ADTS
		if ((0xFF & frame[pos]) != 0xFF && (0xE0 & frame[pos + 1]) != 0xE0)
			return -1;

		//ADTS fixed
		setID(1 & frame[pos + 1]>>>3);
		setLayer(3 & frame[pos + 1]>>>1);
		setProtectionBit(1 ^ (1 & frame[pos + 1]));
		setMode(3 & frame[pos + 2]>>>6); //profile
		setSamplingFrequency(frequency_index[0xF & frame[pos + 2]>>>2]);
		setPrivateBit(1 & frame[pos + 2]>>>1);
		setChannel(7 & frame[pos + 2]<<2 | 3 & frame[pos + 3]>>>6); //see specif.
		setCopyright(1 & frame[pos + 3]>>>5);
		setOriginal(1 & frame[pos + 3]>>>4); //home
		setEmphasis(3 & frame[pos + 3]>>>2);
	//	setFrameTimeLength(169344000.0 / getSamplingFrequency());
		setFrameTimeLength(3840);
		setBitrate(1000); 

		//ADTS variabel
		//copyright_identification_bit 1 bslbf    pos+3
		//copyright_identification_start 1 bslbf  pos+3 
		setSizeBase((0xFF & frame[pos + 4])<<5 | (0x1F & frame[pos + 5]>>3)); //frame_length 13 bslbf
		setSize(getSizeBase());
		//adts_buffer_fullness 11 bslbf
		//number_of_raw_data_blocks_in_frame 2 uimsfb

		return getLayer(); 
	} 
	 
	/**
	 * parse next aac Header 
	 */ 
	public int parseNextHeader(byte[] frame, int pos)
	{ 
		boolean latm = false;
		int aac_length = 0;

		latm = frame[pos] == 0x56 && (0xE0 & frame[pos + 1]) == 0xE0;

		if (latm)
		{
			aac_length = 3 + ((0x1F & frame[pos + 1])<<8 | (0xFF & frame[pos + 2]));
			pos += 3;
		}

		if ((0xFF & frame[pos]) != 0xFF && (0xE0 & frame[pos + 1]) != 0xE0)
			return -1;

		setNextID(1 & frame[pos + 1]>>>3);
		setNextLayer(3 & frame[pos + 1]>>>1);
		setNextProtectionBit(1 ^ (1 & frame[pos + 1]));
		setNextMode(3 & frame[pos + 2]>>>6); //profile
		setNextSamplingFrequency(frequency_index[0xF & frame[pos + 2]>>>2]);
		setNextPrivateBit(1 & frame[pos + 2]>>>1);
		setNextChannel(7 & frame[pos + 2]<<2 | 3 & frame[pos + 3]>>>6); //see specif.
		setNextCopyright(1 & frame[pos + 3]>>>5);
		setNextOriginal(1 & frame[pos + 3]>>>4); //home
		setNextEmphasis(3 & frame[pos + 3]>>>2);
	//	setNextFrameTimeLength(169344000.0 / getNextSamplingFrequency());
		setNextFrameTimeLength(3840);
		setNextBitrate(1000); 

		//ADTS variabel
		//copyright_identification_bit 1 bslbf    pos+3
		//copyright_identification_start 1 bslbf  pos+3 
		setNextSizeBase((0xFF & frame[pos + 4])<<5 | (0x1F & frame[pos + 5]>>3)); //frame_length 13 bslbf
		setNextSize(getNextSizeBase());
		//adts_buffer_fullness 11 bslbf
		//number_of_raw_data_blocks_in_frame 2 uimsfb

		return getNextLayer(); 
	} 
	 
	/**
	 * verify current & last header 
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
	 * display last aac header 
	 */ 
	public String displayHeader()
	{ 
		return ("AAC, " + acmod[getLastMode()] + "(" + channels[getLastChannel()] + "), " + getLastSamplingFrequency() + "Hz, " + (getLastBitrate() / 1000.0) + "kbps, " + getLastSize() + "BpF"); 
	} 

}