/*
 * @(#)AudioFormatAAC.java - parse Audioheaders, dts
 *
 * Copyright (c) 2007 by dvb.matt, All Rights Reserved.
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

public class AudioFormatAAC extends AudioFormat {


	public AudioFormatAAC()
	{
		super();
	}

	/**
	 *
	 */
	private int[] aac_frequency_index = { 
		0, 8000, 16000, 32000, 64000, 128000, 
		11025, 22050, 44100, 88200, 176400,
		12000, 24000, 48000, 96000, 192000 
	}; 
	
	/**
	 *
	 */
	private int[] aac_bitrate_index = { 
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
	private String[] aac_acmod = { 
		"1", "DM", "2/0", "2/0", "2/0", "3/0", "2.1/0", "3.1/0", 
		"2/2", "3/2", "2/2/2", "2/2/2", "3/2/2", "3.1/2/2", "","",
		"","","","","","","","","","","","","","","","", 
		"","","","","","","","","","","","","","","","", 
		"","","","","","","","","","","","","","","","" 
	}; 

	/**
	 *
	 */
	private int[] aac_channels = { 
		1,2,2,2, 2,3,3,4, 4,5,6,6, 7,8,0,0, 
		0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0, 
		0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0, 
		0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0 
	}; 
	 
	/**
	 * parse aac Header 
	 */ 
	public int parseHeader(byte[] frame, int pos)
	{ 
		return -1;  //empty
	} 
	 
	/**
	 * parse next aac Header 
	 */ 
	public int parseNextHeader(byte[] frame, int pos)
	{ 
		return -1; //empty
	} 
	 
	/**
	 * verify current & last header 
	 */ 
	public int compareHeader()
	{ 
		if (lID != ID) 
			return 0x1; 

		else if (lLayer != Layer) 
			return 0x2; 

		else if (lSampling_frequency != Sampling_frequency) 
			return 0x4; 

		else if (lBitrate != Bitrate) 
			return 0x8; 

		else if (lMode != Mode) 
			return 0x10; 

		else if (lMode_extension != Mode_extension) 
			return 0x20; 

		else if (lSize != Size) 
			return 0x40; 

		else 
			return 0; 
	} 
	 
	/**
	 * display last aac header 
	 */ 
	public String displayHeader()
	{ 
		return ("AAC, " + aac_acmod[lMode] + "(" + aac_channels[lMode] + "), " + lSampling_frequency + "Hz, " + (lBitrate / 1000.0) + "kbps, " + lSize + "BpF"); 
	} 

}