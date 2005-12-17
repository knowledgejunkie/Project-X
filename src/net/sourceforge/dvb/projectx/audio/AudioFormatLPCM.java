/*
 * @(#)AudioFormatLPCM.java - parse Audioheaders, lpcm
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

public class AudioFormatLPCM extends AudioFormat {
		
	public AudioFormatLPCM()
	{
		super();
	}

	private int lpcm_frequency_index[] = { 48000, 96000 };
	private int lpcm_bps_index[] = { 16, 20, 24, -1 };
	
	/**
	 *
	 */
	public int parseHeader(byte[] frame_header, int pos)
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
	
	/**
	 *
	 */
	public int parseNextHeader(byte[] frame_header, int pos)
	{ 
		INTEL=true; // force intel/wav output
	
		nID = 0xFF & frame_header[pos]; // no of frameheaders
		nPadding_bit = (0xFF & frame_header[pos + 1])<<8 | (0xFF & frame_header[pos + 2]); // first_access_unit_pointer 
		nLayer = 0xFF & frame_header[pos + 3]; // audio_frame_number 
	
		nProtection_bit = 0^1; 
		nPrivate_bit = 0; 
		nCopyright = 0; 
		nOriginal = 0; 
		nSize_base = 0;
	
		nSize = lpcm_bps_index[(3 & frame_header[pos + 4]>>>6)]; //bits per sample
		nSampling_frequency = lpcm_frequency_index[(1 & frame_header[pos + 4]>>>4)]; // samplerate
		nChannel = 1 + (7 & frame_header[pos + 4]); // channels
		nEmphasis = 0xFF & frame_header[pos + 5]; // dynamic_range
		nMode = (nChannel * nSize) / 8;  // block_align, bytes per sample
	
		nBitrate = nChannel * nSampling_frequency * nSize; // bitrate per second
	
		if (nSize < 1)
			return -1; 
	
		nTime_length = 90000.0 / nSampling_frequency;  // 1 frame = 150 * timelength
	
		return 0; 
	} 

	/**
	 *
	 */
	public String displayHeader()
	{ 
		return ("LPCM, DR-" + lEmphasis + ", " + lChannel + "-ch, " + lSampling_frequency + "Hz, " + lSize + "bit, " + (lBitrate / 1000.0) + "kbps");
	} 
	 
	/**
	 *
	 */
	public int compareHeader()
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

}