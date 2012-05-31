/*
 * @(#)AudioFormat.java - parse Audioheaders, basic class
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

import java.io.RandomAccessFile;
import java.io.IOException;

import net.sourceforge.dvb.projectx.parser.CommonParsing;
import net.sourceforge.dvb.projectx.common.Common;


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

		case CommonParsing.AAC_AUDIO:
			impl = new AudioFormatAAC();//
			break;
		}
	}

	private boolean INTEL;

	private int ID;
	private int Layer;
	private int Protection_bit;
	private int Private_bit;
	private int Bitrate;
	private int Sampling_frequency;
	private int Padding_bit;
	private int Mode;
	private int Mode_extension;
	private int Copyright;
	private int Original;
	private int Channel;
	private int Emphasis;
	private int Size;
	private int Size_base;
	private double Time_length;
	
	private int nID;
	private int nLayer;
	private int nProtection_bit;
	private int nPrivate_bit;
	private int nBitrate;
	private int nSampling_frequency;
	private int nPadding_bit;
	private int nMode;
	private int nMode_extension;
	private int nCopyright;
	private int nOriginal;
	private int nChannel;
	private int nEmphasis;
	private int nSize;
	private int nSize_base;
	private double nTime_length;
	
	private int lID;
	private int lLayer;
	private int lProtection_bit;
	private int lPrivate_bit;
	private int lBitrate;
	private int lSampling_frequency;
	private int lPadding_bit;
	private int lMode;
	private int lMode_extension;
	private int lCopyright;
	private int lOriginal;
	private int lChannel;
	private int lEmphasis;
	private int lSize;
	private int lSize_base;
	private double lTime_length;

	/**
	 *
	 */
	public void init()
	{
		setINTEL(false);

		setID(0);
		setLayer(0);
		setProtectionBit(0);
		setPrivateBit(0);
		setBitrate(0);
		setSamplingFrequency(0);
		setPaddingBit(0);
		setPrivateBit(0);
		setMode(0);
		setModeExtension(0);
		setCopyright(0);
		setOriginal(0);
		setChannel(0);
		setEmphasis(0);
		setSize(0);
		setSizeBase(0);
		setFrameTimeLength(0.0);
	}


	/**
	 * 
	 */
	public boolean isINTEL()
	{
		return (impl == null ? INTEL : impl.isINTEL());
	}

	/**
	 * 
	 */
	public void setINTEL(boolean b)
	{
		if (impl != null)
			impl.setINTEL(b);

		INTEL = b;
	}

	/**
	 * 
	 */
	public int getID()
	{
		return (impl == null ? ID : impl.getID());
	}

	/**
	 * 
	 */
	public void setID(int val)
	{
		if (impl != null)
			impl.setID(val);

		ID = val;
	}

	/**
	 * 
	 */
	public int getLayer()
	{
		return (impl == null ? Layer : impl.getLayer());
	}

	/**
	 * 
	 */
	public void setLayer(int val)
	{
		if (impl != null)
			impl.setLayer(val);

		Layer = val;
	}

	/**
	 * 
	 */
	public int getBitrate()
	{
		return (impl == null ? Bitrate : impl.getBitrate());
	}

	/**
	 * 
	 */
	public void setBitrate(int val)
	{
		if (impl != null)
			impl.setBitrate(val);

		Bitrate = val;
	}

	/**
	 * 
	 */
	public int getSamplingFrequency()
	{
		return (impl == null ? Sampling_frequency : impl.getSamplingFrequency());
	}

	/**
	 * 
	 */
	public void setSamplingFrequency(int val)
	{
		if (impl != null)
			impl.setSamplingFrequency(val);

		Sampling_frequency = val;
	}

	/**
	 * 
	 */
	public int getMode()
	{
		return (impl == null ? Mode : impl.getMode());
	}

	/**
	 * 
	 */
	public void setMode(int val)
	{
		if (impl != null)
			impl.setMode(val);

		Mode = val;
	}

	/**
	 * 
	 */
	public int getModeExtension()
	{
		return (impl == null ? Mode_extension : impl.getModeExtension());
	}

	/**
	 * 
	 */
	public void setModeExtension(int val)
	{
		if (impl != null)
			impl.setModeExtension(val);

		Mode_extension = val;
	}

	/**
	 * 
	 */
	public int getEmphasis()
	{
		return (impl == null ? Emphasis : impl.getEmphasis());
	}

	/**
	 * 
	 */
	public void setEmphasis(int val)
	{
		if (impl != null)
			impl.setEmphasis(val);

		Emphasis = val;
	}

	/**
	 * 
	 */
	public int getSize()
	{
		return (impl == null ? Size : impl.getSize());
	}

	/**
	 * 
	 */
	public void setSize(int val)
	{
		if (impl != null)
			impl.setSize(val);

		Size = val;
	}

	/**
	 * 
	 */
	public int getSizeBase()
	{
		return (impl == null ? Size_base : impl.getSizeBase());
	}

	/**
	 * 
	 */
	public void setSizeBase(int val)
	{
		if (impl != null)
			impl.setSizeBase(val); //slackalan 250312
			//setSizeBase(val);

		Size_base = val;
	}

	/**
	 * 
	 */
	public int getChannel()
	{
		return (impl == null ? Channel : impl.getChannel());
	}

	/**
	 * 
	 */
	public void setChannel(int val)
	{
		if (impl != null)
			impl.setChannel(val);

		Channel = val;
	}

	/**
	 * 
	 */
	public int getPaddingBit()
	{
		return (impl == null ? Padding_bit : impl.getPaddingBit());
	}

	/**
	 * 
	 */
	public void setPaddingBit(int val)
	{
		if (impl != null)
			impl.setPaddingBit(val);

		Padding_bit = val;
	}

	/**
	 * 
	 */
	public int getPrivateBit()
	{
		return (impl == null ? Private_bit : impl.getPrivateBit());
	}

	/**
	 * 
	 */
	public void setPrivateBit(int val)
	{
		if (impl != null)
			impl.setPrivateBit(val);

		Private_bit = val;
	}

	/**
	 * 
	 */
	public int getOriginal()
	{
		return (impl == null ? Original : impl.getOriginal());
	}

	/**
	 * 
	 */
	public void setOriginal(int val)
	{
		if (impl != null)
			impl.setOriginal(val);

		Original = val;
	}

	/**
	 * 
	 */
	public int getCopyright()
	{
		return (impl == null ? Copyright : impl.getCopyright());
	}

	/**
	 * 
	 */
	public void setCopyright(int val)
	{
		if (impl != null)
			impl.setCopyright(val);

		Copyright = val;
	}

	/**
	 * 
	 */
	public int getProtectionBit()
	{
		return (impl == null ? Protection_bit : impl.getProtectionBit());
	}

	/**
	 * 
	 */
	public void setProtectionBit(int val)
	{
		if (impl != null)
			impl.setProtectionBit(val);

		Protection_bit = val;
	}

	/**
	 * 
	 */
	public double getFrameTimeLength()
	{
		return (impl == null ? Time_length : impl.getFrameTimeLength());
	}

	/**
	 * 
	 */
	public void setFrameTimeLength(double val)
	{
		if (impl != null)
			impl.setFrameTimeLength(val);

		Time_length = val;
	}

///////

	/**
	 * 
	 */
	public int getLastID()
	{
		return (impl == null ? lID : impl.getLastID());
	}

	/**
	 * 
	 */
	public void setLastID(int val)
	{
		if (impl != null)
			impl.setLastID(val);

		lID = val;
	}

	/**
	 * 
	 */
	public int getLastLayer()
	{
		return (impl == null ? lLayer : impl.getLastLayer());
	}

	/**
	 * 
	 */
	public void setLastLayer(int val)
	{
		if (impl != null)
			impl.setLastLayer(val);

		lLayer = val;
	}

	/**
	 * 
	 */
	public int getLastBitrate()
	{
		return (impl == null ? lBitrate : impl.getLastBitrate());
	}

	/**
	 * 
	 */
	public void setLastBitrate(int val)
	{
		if (impl != null)
			impl.setLastBitrate(val);

		lBitrate = val;
	}

	/**
	 * 
	 */
	public int getLastSamplingFrequency()
	{
		return (impl == null ? lSampling_frequency : impl.getLastSamplingFrequency());
	}

	/**
	 * 
	 */
	public void setLastSamplingFrequency(int val)
	{
		if (impl != null)
			impl.setLastSamplingFrequency(val);

		lSampling_frequency = val;
	}

	/**
	 * 
	 */
	public int getLastMode()
	{
		return (impl == null ? lMode : impl.getLastMode());
	}

	/**
	 * 
	 */
	public void setLastMode(int val)
	{
		if (impl != null)
			impl.setLastMode(val);

		lMode = val;
	}

	/**
	 * 
	 */
	public int getLastModeExtension()
	{
		return (impl == null ? lMode_extension : impl.getLastModeExtension());
	}

	/**
	 * 
	 */
	public void setLastModeExtension(int val)
	{
		if (impl != null)
			impl.setLastModeExtension(val);

		lMode_extension = val;
	}

	/**
	 * 
	 */
	public int getLastEmphasis()
	{
		return (impl == null ? lEmphasis : impl.getLastEmphasis());
	}

	/**
	 * 
	 */
	public void setLastEmphasis(int val)
	{
		if (impl != null)
			impl.setLastEmphasis(val);

		lEmphasis = val;
	}

	/**
	 * 
	 */
	public int getLastSize()
	{
		return (impl == null ? lSize : impl.getLastSize());
	}

	/**
	 * 
	 */
	public void setLastSize(int val)
	{
		if (impl != null)
			impl.setLastSize(val);

		lSize = val;
	}

	/**
	 * 
	 */
	public int getLastSizeBase()
	{
		return (impl == null ? lSize_base : impl.getLastSizeBase());
	}

	/**
	 * 
	 */
	public void setLastSizeBase(int val)
	{
		if (impl != null)
			impl.setLastSizeBase(val);

		lSize_base = val;
	}

	/**
	 * 
	 */
	public int getLastChannel()
	{
		return (impl == null ? lChannel : impl.getLastChannel());
	}

	/**
	 * 
	 */
	public void setLastChannel(int val)
	{
		if (impl != null)
			impl.setLastChannel(val);

		lChannel = val;
	}

	/**
	 * 
	 */
	public int getLastPaddingBit()
	{
		return (impl == null ? lPadding_bit : impl.getLastPaddingBit());
	}

	/**
	 * 
	 */
	public void setLastPaddingBit(int val)
	{
		if (impl != null)
			impl.setLastPaddingBit(val);

		lPadding_bit = val;
	}

	/**
	 * 
	 */
	public int getLastPrivateBit()
	{
		return (impl == null ? lPrivate_bit : impl.getLastPrivateBit());
	}

	/**
	 * 
	 */
	public void setLastPrivateBit(int val)
	{
		if (impl != null)
			impl.setLastPrivateBit(val);

		lPrivate_bit = val;
	}

	/**
	 * 
	 */
	public int getLastOriginal()
	{
		return (impl == null ? lOriginal : impl.getLastOriginal());
	}

	/**
	 * 
	 */
	public void setLastOriginal(int val)
	{
		if (impl != null)
			impl.setLastOriginal(val);

		lOriginal = val;
	}

	/**
	 * 
	 */
	public int getLastCopyright()
	{
		return (impl == null ? lCopyright : impl.getLastCopyright());
	}

	/**
	 * 
	 */
	public void setLastCopyright(int val)
	{
		if (impl != null)
			impl.setLastCopyright(val);

		lCopyright = val;
	}

	/**
	 * 
	 */
	public int getLastProtectionBit()
	{
		return (impl == null ? lProtection_bit : impl.getLastProtectionBit());
	}

	/**
	 * 
	 */
	public void setLastProtectionBit(int val)
	{
		if (impl != null)
			impl.setLastProtectionBit(val);

		lProtection_bit = val;
	}

	/**
	 * 
	 */
	public double getLastFrameTimeLength()
	{
		return (impl == null ? lTime_length : impl.getLastFrameTimeLength());
	}

	/**
	 * 
	 */
	public void setLastFrameTimeLength(double val)
	{
		if (impl != null)
			impl.setLastFrameTimeLength(val);

		lTime_length = val;
	}

///////


	/**
	 * 
	 */
	public int getNextID()
	{
		return (impl == null ? nID : impl.getNextID());
	}

	/**
	 * 
	 */
	public void setNextID(int val)
	{
		if (impl != null)
			impl.setNextID(val);

		nID = val;
	}

	/**
	 * 
	 */
	public int getNextLayer()
	{
		return (impl == null ? nLayer : impl.getNextLayer());
	}

	/**
	 * 
	 */
	public void setNextLayer(int val)
	{
		if (impl != null)
			impl.setNextLayer(val);

		nLayer = val;
	}

	/**
	 * 
	 */
	public int getNextBitrate()
	{
		return (impl == null ? nBitrate : impl.getNextBitrate());
	}

	/**
	 * 
	 */
	public void setNextBitrate(int val)
	{
		if (impl != null)
			impl.setNextBitrate(val);

		nBitrate = val;
	}

	/**
	 * 
	 */
	public int getNextSamplingFrequency()
	{
		return (impl == null ? nSampling_frequency : impl.getNextSamplingFrequency());
	}

	/**
	 * 
	 */
	public void setNextSamplingFrequency(int val)
	{
		if (impl != null)
			impl.setNextSamplingFrequency(val);

		nSampling_frequency = val;
	}

	/**
	 * 
	 */
	public int getNextMode()
	{
		return (impl == null ? nMode : impl.getNextMode());
	}

	/**
	 * 
	 */
	public void setNextMode(int val)
	{
		if (impl != null)
			impl.setNextMode(val);

		nMode = val;
	}

	/**
	 * 
	 */
	public int getNextModeExtension()
	{
		return (impl == null ? nMode_extension : impl.getNextModeExtension());
	}

	/**
	 * 
	 */
	public void setNextModeExtension(int val)
	{
		if (impl != null)
			impl.setNextModeExtension(val);

		nMode_extension = val;
	}

	/**
	 * 
	 */
	public int getNextEmphasis()
	{
		return (impl == null ? nEmphasis : impl.getNextEmphasis());
	}

	/**
	 * 
	 */
	public void setNextEmphasis(int val)
	{
		if (impl != null)
			impl.setNextEmphasis(val);

		nEmphasis = val;
	}

	/**
	 * 
	 */
	public int getNextSize()
	{
		return (impl == null ? nSize : impl.getNextSize());
	}

	/**
	 * 
	 */
	public void setNextSize(int val)
	{
		if (impl != null)
			impl.setNextSize(val);

		nSize = val;
	}

	/**
	 * 
	 */
	public int getNextSizeBase()
	{
		return (impl == null ? nSize_base : impl.getNextSizeBase());
	}

	/**
	 * 
	 */
	public void setNextSizeBase(int val)
	{
		if (impl != null)
			impl.setNextSizeBase(val);

		nSize_base = val;
	}

	/**
	 * 
	 */
	public int getNextChannel()
	{
		return (impl == null ? nChannel : impl.getNextChannel());
	}

	/**
	 * 
	 */
	public void setNextChannel(int val)
	{
		if (impl != null)
			impl.setNextChannel(val);

		nChannel = val;
	}

	/**
	 * 
	 */
	public int getNextPaddingBit()
	{
		return (impl == null ? nPadding_bit : impl.getNextPaddingBit());
	}

	/**
	 * 
	 */
	public void setNextPaddingBit(int val)
	{
		if (impl != null)
			impl.setNextPaddingBit(val);

		nPadding_bit = val;
	}

	/**
	 * 
	 */
	public int getNextPrivateBit()
	{
		return (impl == null ? nPrivate_bit : impl.getNextPrivateBit());
	}

	/**
	 * 
	 */
	public void setNextPrivateBit(int val)
	{
		if (impl != null)
			impl.setNextPrivateBit(val);

		nPrivate_bit = val;
	}

	/**
	 * 
	 */
	public int getNextOriginal()
	{
		return (impl == null ? nOriginal : impl.getNextOriginal());
	}

	/**
	 * 
	 */
	public void setNextOriginal(int val)
	{
		if (impl != null)
			impl.setNextOriginal(val);

		nOriginal = val;
	}

	/**
	 * 
	 */
	public int getNextCopyright()
	{
		return (impl == null ? nCopyright : impl.getNextCopyright());
	}

	/**
	 * 
	 */
	public void setNextCopyright(int val)
	{
		if (impl != null)
			impl.setNextCopyright(val);

		nCopyright = val;
	}

	/**
	 * 
	 */
	public int getNextProtectionBit()
	{
		return (impl == null ? nProtection_bit : impl.getNextProtectionBit());
	}

	/**
	 * 
	 */
	public void setNextProtectionBit(int val)
	{
		if (impl != null)
			impl.setNextProtectionBit(val);

		nProtection_bit = val;
	}

	/**
	 * 
	 */
	public double getNextFrameTimeLength()
	{
		return (impl == null ? nTime_length : impl.getNextFrameTimeLength());
	}

	/**
	 * 
	 */
	public void setNextFrameTimeLength(double val)
	{
		if (impl != null)
			impl.setNextFrameTimeLength(val);

		nTime_length = val;
	}




//////

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
		setLastID(getID());
		setLastLayer(getLayer());
		setLastProtectionBit(getProtectionBit());
		setLastPrivateBit(getPrivateBit());
		setLastBitrate(getBitrate());
		setLastSamplingFrequency(getSamplingFrequency());
		setLastPaddingBit(getPaddingBit());
		setLastPrivateBit(getPrivateBit());
		setLastMode(getMode());
		setLastModeExtension(getModeExtension());
		setLastCopyright(getCopyright());
		setLastOriginal(getOriginal());
		setLastChannel(getChannel());
		setLastEmphasis(getEmphasis());
		setLastSize(getSize());
		setLastSizeBase(getSizeBase());
		setLastFrameTimeLength(getFrameTimeLength());
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
	public byte[] editFrame(byte[] frame, int mode)
	{
		return (impl == null ? frame : impl.editFrame(frame, mode));
	}
		
	/**
	 *
	 */
	public byte[][] convertFrame(byte[] frame, int mode)
	{
		if (impl == null)
			return (new byte[][]{ frame, new byte[0] });

		return impl.convertFrame(frame, mode);
//		return (impl == null ? frame : impl.convertFrame(frame, mode));
	}
	
	/**
	 *
	 */
	public void removeCRC(byte[] frame, boolean remove)
	{
		if (impl != null)
			impl.removeCRC(frame, remove);
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
	public String decodeAncillaryData(byte[] frame, double frametime)
	{
		return (impl == null ? null : impl.decodeAncillaryData(frame, frametime));
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

			if (!isINTEL())
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
		return littleEndian(data, len, isINTEL());
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

	/**
	 * 
	 */
	public int getBits(byte buf[], int BitPos[], int N)
	{
		int Pos, Val;

		Pos = BitPos[0]>>>3;

		Val = (0xFF & buf[Pos])<<24 |
			(0xFF & buf[Pos+1])<<16 |
			(0xFF & buf[Pos+2])<<8 |
			(0xFF & buf[Pos+3]);

		Val <<= BitPos[0] & 7;
		Val >>>= 32-N;
		BitPos[0] += N;

		return Val;
	}

}