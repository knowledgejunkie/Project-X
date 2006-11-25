/*
 * @(#)AudioFormat.java - parse Audioheaders, basic class
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

import java.io.File;
import java.io.RandomAccessFile;
import java.io.IOException;

import net.sourceforge.dvb.projectx.parser.CommonParsing;
import net.sourceforge.dvb.projectx.common.Common;

import net.sourceforge.dvb.projectx.audio.AudioFormatDTS;
import net.sourceforge.dvb.projectx.audio.AudioFormatAC3;
import net.sourceforge.dvb.projectx.audio.AudioFormatLPCM;
import net.sourceforge.dvb.projectx.audio.AudioFormatWAV;
import net.sourceforge.dvb.projectx.audio.AudioFormatMPA;


public class AudioFormat extends Object {

	private AudioFormat impl = null;

	/**
	 *
	 */
	public AudioFormat(int type)
	{
		setNewType(type);
	}

	/**
	 *
	 */
	public AudioFormat(byte[] frame)
	{
		//unused, meant for autodetection
	}

	/**
	 *
	 */
	public AudioFormat()
	{
		init();
	}

	/**
	 *
	 */
	public void setNewType(int type)
	{
		switch (type)
		{
		case CommonParsing.DTS_AUDIO:
			impl = new AudioFormatDTS();
			break;

		case CommonParsing.AC3_AUDIO:
			impl = new AudioFormatAC3();
			break;

		case CommonParsing.LPCM_AUDIO:
			impl = new AudioFormatLPCM();
			break;

		case CommonParsing.WAV_AUDIO:
			impl = new AudioFormatWAV();
			break;

		case CommonParsing.MPEG_AUDIO:
			impl = new AudioFormatMPA();
			break;
		}
	}

	public static boolean INTEL;

	public static int ID;
	public static int Layer;
	public static int Protection_bit;
	public static int Private_bit;
	public static int Bitrate;
	public static int Sampling_frequency;
	public static int Padding_bit;
	public static int public_bit;
	public static int Mode;
	public static int Mode_extension;
	public static int Copyright;
	public static int Original;
	public static int Channel;
	public static int Emphasis;
	public static int Size;
	public static int Size_base;
	public static int Bound;
	public static int Sblimit;
	public static double Time_length;
	
	public int nID;
	public int nLayer;
	public int nProtection_bit;
	public int nPrivate_bit;
	public int nBitrate;
	public int nSampling_frequency;
	public int nPadding_bit;
	public int npublic_bit;
	public int nMode;
	public int nMode_extension;
	public int nCopyright;
	public int nOriginal;
	public int nChannel;
	public int nEmphasis;
	public int nSize;
	public int nSize_base;
	public double nTime_length;
	
	public static int lID;
	public static int lLayer;
	public static int lProtection_bit;
	public static int lPrivate_bit;
	public static int lBitrate;
	public static int lSampling_frequency;
	public static int lPadding_bit;
	public static int lpublic_bit;
	public static int lMode;
	public static int lMode_extension;
	public static int lCopyright;
	public static int lOriginal;
	public static int lChannel;
	public static int lEmphasis;
	public static int lSize;
	public static int lSize_base;
	public static double lTime_length;

	/**
	 *
	 */
	public void init()
	{
		INTEL = false;

		ID = 0;
		Layer = 0;
		Protection_bit = 0;
		Private_bit = 0;
		Bitrate = 0;
		Sampling_frequency = 0;
		Padding_bit = 0;
		Private_bit = 0;
		Mode = 0;
		Mode_extension = 0;
		Copyright = 0;
		Original = 0;
		Channel = 0;
		Emphasis = 0;
		Size = 0;
		Size_base = 0;
		Bound = 0;
		Sblimit = 32;
		Time_length = 0.0;
	}

	/**
	 * 
	 */
	public int getLastModeExtension()
	{
		return lMode_extension;
	}

	/**
	 * 
	 */
	public int getID()
	{
		return ID;
	}

	/**
	 * 
	 */
	public int getLayer()
	{
		return Layer;
	}

	/**
	 * 
	 */
	public int getBitrate()
	{
		return Bitrate;
	}

	/**
	 * 
	 */
	public int getSamplingFrequency()
	{
		return Sampling_frequency;
	}

	/**
	 * 
	 */
	public int getMode()
	{
		return Mode;
	}

	/**
	 * 
	 */
	public int getModeExtension()
	{
		return Mode_extension;
	}

	/**
	 * 
	 */
	public int getEmphasis()
	{
		return Emphasis;
	}

	/**
	 * 
	 */
	public int getSize()
	{
		return Size;
	}

	/**
	 * 
	 */
	public int getSizeBase()
	{
		return Size_base;
	}

	/**
	 * 
	 */
	public int getChannel()
	{
		return Channel;
	}

	/**
	 * 
	 */
	public double getFrameTimeLength()
	{
		return Time_length;
	}

	/**
	 *
	 */
	public int parseHeader(byte[] data, int offset, int endoffset)
	{
		int ret = 0;

		if (impl != null)
		{
			for (int i = 0; i < endoffset; i++)
				if ((ret = parseHeader(data, offset + i)) < 0)
					continue;
		}

		return ret;
	}

	/**
	 *
	 */
	public int parseHeader(byte[] frame, int offset)
	{
		return (impl == null ? 0 : impl.parseHeader(frame, offset));
	}
	
	/**
	 *
	 */
	public int parseNextHeader(byte[] frame, int offset)
	{
		return (impl == null ? 0 : impl.parseNextHeader(frame, offset));
	}

	/**
	 * save last header 
	 */
	public void saveHeader()
	{
		lID = ID;
		lLayer = Layer;
		lProtection_bit = Protection_bit;
		lPrivate_bit = Private_bit;
		lBitrate = Bitrate;
		lSampling_frequency = Sampling_frequency;
		lPadding_bit = Padding_bit;
		lPrivate_bit = Private_bit;
		lMode = Mode;
		lMode_extension = Mode_extension;
		lCopyright = Copyright;
		lOriginal = Original;
		lChannel = Channel;
		lEmphasis = Emphasis;
		lSize = Size;
		lSize_base = Size_base;
		lTime_length = Time_length;
	}
	
	/**
	 *
	 */
	public int compareHeader()
	{
		return (impl == null ? 0 : impl.compareHeader());
	}
	
	/**
	 *
	 */
	public String displayHeader()
	{
		return (impl == null ? "" : impl.displayHeader());
	}
	
	/**
	 * save and display last header 
	 */ 
	public String saveAndDisplayHeader()
	{ 
		saveHeader();

		return displayHeader(); 
	}

	/**
	 *
	 */
	public byte[] editFrame(byte[] frame, int framesize, int mode)
	{
		return (impl == null ? frame : impl.editFrame(frame, framesize, mode));
	}
		
	/**
	 *
	 */
	public void removeCRC(byte[] frame)
	{
		if (impl != null)
			impl.removeCRC(frame);
	}
		
	/**
	 *
	 */
	public int validateCRC(byte[] frame, int offset, int len)
	{
		return (impl == null ? 0 : impl.validateCRC(frame, offset, len));
	}

	/**
	 *
	 */
	public void setAncillaryDataDecoder(boolean b1, boolean b2)
	{
		if (impl != null)
			impl.setAncillaryDataDecoder(b1, b2);
	}

	/**
	 *
	 */
	public void decodeAncillaryData(byte[] frame, String frametime_str)
	{
		if (impl != null)
			impl.decodeAncillaryData(frame, frametime_str);
	}

	/**
	 * 
	 */
	public void parseRiffData(byte[] frame, int channel)
	{
		if (impl != null)
			impl.parseRiffData(frame, channel);
	}

	/**
	 * 
	 */
	public void initExtraWaveHeader(boolean bool_ACM, boolean bool_BWF, boolean bool_AC3)
	{
		if (impl != null)
			impl.initExtraWaveHeader(bool_ACM, bool_BWF, bool_AC3);
	}

	/**
	 * 
	 */
	public byte[] getExtraWaveHeader(int channel, boolean placeholder)
	{
		return (impl == null ? (new byte[0]) : impl.getExtraWaveHeader(channel, placeholder));
	}

	/**
	 * 
	 */
	public void setExtraWaveData(int[] array, int channel)
	{
		if (impl != null)
			impl.setExtraWaveData(array, channel);
	}

	/**
	 * 
	 */
	public void setExtraWaveLength(long filelength, long timelength, int channel)
	{
		if (impl != null)
			impl.setExtraWaveLength(filelength, timelength, channel);
	}

	/**
	 * returns RIFF
	 */
	public byte[] getRiffHeader()
	{
		return (new byte[] {
			0x52, 0x49, 0x46, 0x46,  //'RIFF'
			0, 0, 0, 0,  // all size LSB
			0x57, 0x41, 0x56, 0x45, //'WAVE'
			0x66, 0x6D, 0x74, 0x20, //'fmt '
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
			0, 0, 0, 0, 
			0x64, 0x61, 0x74, 0x61, // 'data'
			0, 0, 0, 0  // data size LSB
		});
	}

	/**
	 * updates std RIFF
	 */
	public void fillStdRiffHeader(String file, long time_len)
	{
		try {
			RandomAccessFile riff = new RandomAccessFile(file, "rw");

			int len = (int) riff.length() - 8;
			int bitrate = 1411200;

			riff.seek(4);
			riff.writeInt(littleEndian(len, 4, true));  //data+chunksize

			riff.seek(16);
			riff.writeInt(littleEndian(0x10, 4, true));  //chunk length
			riff.writeShort(littleEndian(1, 2, true));   //pcm
			riff.writeShort((short)littleEndian(2, 2, true)); //channels
			riff.writeInt(littleEndian(44100, 4, true));  //sample_freq
			riff.writeInt(littleEndian(bitrate / 8, 4, true)); //byterate
			riff.writeShort((short)littleEndian(4, 2, true)); //blockalign
			riff.writeShort((short)littleEndian(16, 2, true)); //bits_per_sample

			riff.seek(40);
			riff.writeInt(littleEndian(len - 36, 4, true));  //data-size
	
			riff.close();

		} catch (IOException e) {
			Common.setExceptionMessage(e);
		}
	}

	/**
	 * updates RIFF
	 * returns playtime as int
	 */
	public long fillRiffHeader(String file)
	{
		long value = 0;

		try {
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

			value = (8000L * (len - 36)) / lBitrate;

		} catch (IOException e) {
			Common.setExceptionMessage(e);
		}

		return value;
	}


	/**
	 *
	 */
	public int littleEndian(byte[] data, int offset, int len, boolean reverse)
	{
		int value = 0;

		for (int a = 0; a < len; a++)
			value |= reverse ? ((0xFF & data[offset + a])<<(a * 8)) : ((0xFF & data[offset + a])<<((len - 1 - a) * 8));

		return value;
	}

	/**
	 *
	 */
	public int getValue(byte[] data, int offset, int len, boolean reverse)
	{
		return littleEndian(data, offset, len, reverse);
	}

	/**
	 *
	 */
	public void setValue(byte[] array, int offset, int len, boolean bytereordering, int value)
	{
		for (int i = 0; bytereordering && i < len; i++)
			array[i + offset] = (byte)(0xFF & (value>>>(i * 8)));

		for (int i = 0, j = len - 1; !bytereordering && i < len; i++, j--)
			array[i + offset] = (byte)(0xFF & (value>>>(j * 8)));
	}

	/**
	 *
	 */
	public int littleEndian(int data, int len)
	{
		return littleEndian(data, len, INTEL);
	}

	/**
	 *
	 */
	public int littleEndian(int data, int len, boolean b)
	{
		if (!b)	
			return data;

		if (len == 4) 
			return ( (0xFF & data>>>24) | (0xFF & data>>>16)<<8 | (0xFF & data>>>8)<<16 | (0xFF & data)<<24 );

		else 
			return ( (0xFF & data>>>8) | (0xFF & data)<<8 );
	}


}