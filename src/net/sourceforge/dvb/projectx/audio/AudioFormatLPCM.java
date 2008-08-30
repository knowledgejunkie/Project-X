/*
 * @(#)AudioFormatLPCM.java - parse Audioheaders, lpcm
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
		setINTEL(true); // force intel/wav output
	
		setID(0xFF & frame_header[pos]); // no of frameheaders
		setPaddingBit((0xFF & frame_header[pos + 1])<<8 | (0xFF & frame_header[pos + 2])); // first_access_unit_pointer 
		setLayer(0xFF & frame_header[pos + 3]); // audio_frame_number 
	
		setProtectionBit(0^1); 
		setPrivateBit(0); 
		setCopyright(0); 
		setOriginal(0); 
		setSizeBase(0);
	
		setSize(lpcm_bps_index[(3 & frame_header[pos + 4]>>>6)]); //bits per sample
		setSamplingFrequency(lpcm_frequency_index[(1 & frame_header[pos + 4]>>>4)]); // samplerate
		setChannel(1 + (7 & frame_header[pos + 4])); // channels
		setEmphasis(0xFF & frame_header[pos + 5]); // dynamic_range
		setMode((getChannel() * getSize()) / 8);  // block_align, bytes per sample
	
		setBitrate(getChannel() * getSamplingFrequency() * getSize()); // bitrate per second
	
		if (getSize() < 1)
			return -1; 
	
		setFrameTimeLength(90000.0 / getSamplingFrequency());  // 1 frame = 150 * timelength
	
		return 0; 
	} 
	
	/**
	 *
	 */
	public int parseNextHeader(byte[] frame_header, int pos)
	{ 
		setINTEL(true); // force intel/wav output
	
		setNextID(0xFF & frame_header[pos]); // no of frameheaders
		setNextPaddingBit((0xFF & frame_header[pos + 1])<<8 | (0xFF & frame_header[pos + 2])); // first_access_unit_pointer 
		setNextLayer(0xFF & frame_header[pos + 3]); // audio_frame_number 
	
		setNextProtectionBit(0^1); 
		setNextPrivateBit(0); 
		setNextCopyright(0); 
		setNextOriginal(0); 
		setNextSizeBase(0);
	
		setNextSize(lpcm_bps_index[(3 & frame_header[pos + 4]>>>6)]); //bits per sample
		setNextSamplingFrequency(lpcm_frequency_index[(1 & frame_header[pos + 4]>>>4)]); // samplerate
		setNextChannel(1 + (7 & frame_header[pos + 4])); // channels
		setNextEmphasis(0xFF & frame_header[pos + 5]); // dynamic_range
		setNextMode((getNextChannel() * getNextSize()) / 8);  // block_align, bytes per sample
	
		setNextBitrate(getNextChannel() * getNextSamplingFrequency() * getNextSize()); // bitrate per second
	
		if (getNextSize() < 1)
			return -1; 
	
		setNextFrameTimeLength(90000.0 / getNextSamplingFrequency());  // 1 frame = 150 * timelength
	
		return 0; 
	} 

	/**
	 *
	 */
	public String displayHeader()
	{ 
		return ("LPCM, DR-" + getLastEmphasis() + ", " + getLastChannel() + "-ch, " + getLastSamplingFrequency() + "Hz, " + getLastSize() + "bit, " + (getLastBitrate() / 1000.0) + "kbps");
	} 
	 
	/**
	 *
	 */
	public int compareHeader()
	{
		if (getLastChannel() != getChannel()) 
			return 1; 

		else if (getLastSamplingFrequency() != getSamplingFrequency()) 
			return 2; 

		else if (getLastSize() != getSize()) 
			return 3; 

		else if (getLastEmphasis() != getEmphasis()) 
			return 4; 

		else 
			return 0; 
	} 

}