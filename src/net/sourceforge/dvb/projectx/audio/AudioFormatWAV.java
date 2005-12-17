/*
 * @(#)AudioFormatWAV.java - parse Audioheaders, wav / riff
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

import java.util.Arrays;

import net.sourceforge.dvb.projectx.audio.AudioFormat;

public class AudioFormatWAV extends AudioFormat {
		
	public AudioFormatWAV()
	{
		super();
	}

	private final int WaveChunks[] = 
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
	
	/**
	 * parse RIFF_WAVE Header 
	 */ 
	public int parseHeader(byte[] frame, int pos)
	{ 
		INTEL = false;

		if (frame[pos] != 0x52 || frame[pos + 1] != 0x49 || frame[pos + 2] != 0x46 ) 
			return -1;
	
		if (frame[pos + 3] == 0x46)
			INTEL=true;

		else if (frame[pos + 3] != 0x58)
			return -2;
	
		ID = INTEL ? 0 : 1; 
		Emphasis = 0; 
		Protection_bit = 0 ^ 1; 
	
		Arrays.sort(WaveChunks);
	
		if (littleEndian(frame, pos + 8, 4, false) != WaveChunks[0])
			return -3;
	
		int chunk = 0;
		int chunksize = 0;

		for (int a = pos + 12; a < frame.length - 4; a++)
		{
			if (Arrays.binarySearch(WaveChunks, (chunk = littleEndian(frame, a, 4, false))) < 0)
				continue;

			if (chunk == WaveChunks[4])
			{ //fmt chunk read info datas
				chunksize = littleEndian(frame, a + 4, 4, INTEL);
				Layer = littleEndian(frame, a + 8, 2, INTEL);   // Compression type (1=PCM)
				Channel = littleEndian(frame, a + 10, 2, INTEL); // channels
				Sampling_frequency = littleEndian(frame, a + 12, 4, INTEL); // samplerate
				Bitrate = littleEndian(frame, a + 16, 4, INTEL) * 8; // avg bits per second
				Mode = littleEndian(frame, a + 20, 2, INTEL);  // block align, bytes per sample
				Size = littleEndian(frame, a + 22, 2, INTEL); //bits per sample
				//extrabits not of interest
			}

			else if (chunk == WaveChunks[2])
			{ //data chunk, sample data
				chunksize = littleEndian(frame, a + 4, 4, INTEL);
				Size_base = chunksize; // length of whole sample data
				Emphasis = a + 8; // real start of whole sample data
			}

			else
				chunksize = littleEndian(frame, a + 4, 4, INTEL);

			a += chunksize + 3;
		}
	
		//PTS low+high may exists in 'fact' of MPEG1audio !
	
		if (Bitrate < 1 || Sampling_frequency < 1 || Channel < 1)
			return -4; 
	
		Padding_bit = 0; 
		Private_bit = 0; 
		Copyright = 0; 
		Original = 0; 
		Time_length = 90000.0 / Sampling_frequency;
	
		switch (Layer)
		{
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
	
	private final String[] LSB_mode = { "F", "X" };
	private final String[] compression = { "", "PCM", "MPEG", "MPEG-L3", "AC3" };
	
	/**
	 * display last wav header 
	 */ 
	public String displayHeader()
	{ 
		return ("RIF" + LSB_mode[lID] + ", " + (lMode_extension > 0 ? compression[lMode_extension] : "tag 0x" + Integer.toHexString(Layer)) + ", " + lChannel + "-ch, " + lSampling_frequency + "Hz, " + lSize + "bit, " + (lBitrate/1000.0) + "kbps"); 
	} 

}