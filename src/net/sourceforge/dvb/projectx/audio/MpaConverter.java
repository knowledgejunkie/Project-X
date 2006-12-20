/*
 * @(#)MpaConverter.java -  converter M1L2,48khz lossless
 *
 * Copyright (c) 2002-2006 by dvb.matt, All rights reserved.
 * 
 * This file is part of ProjectX, a free Java based demux utility.
 * By the authors, ProjectX is intended for educational purposes only, 
 * as a non-commercial test project.
 * 
 * The part of audio parsing was derived from the MPEG/Audio
 * Software Simulation Group's audio codec in a special modified manner.
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

/*
 * rewritten Oct 2003 by dvb.matt
 */

package net.sourceforge.dvb.projectx.audio;

import java.util.Arrays;

import net.sourceforge.dvb.projectx.audio.AudioFormat;

import net.sourceforge.dvb.projectx.common.Common;
import net.sourceforge.dvb.projectx.common.Resource;

import net.sourceforge.dvb.projectx.parser.CommonParsing;

public class MpaConverter extends Object {

	//mpeg-1 27 subbands
	private final short alloc27[][] = {
		{ 0,1,3,5,6,7,8,9,10,11,12,13,14,15,16,17 },
		{ 0,1,3,5,6,7,8,9,10,11,12,13,14,15,16,17 },
		{ 0,1,3,5,6,7,8,9,10,11,12,13,14,15,16,17 },
		{ 0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,17 },
		{ 0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,17 },
		{ 0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,17 },
		{ 0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,17 },
		{ 0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,17 },
		{ 0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,17 },
		{ 0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,17 },
		{ 0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,17 },
		{ 0,1,2,3,4,5,6,17 },
		{ 0,1,2,3,4,5,6,17 },
		{ 0,1,2,3,4,5,6,17 },
		{ 0,1,2,3,4,5,6,17 },
		{ 0,1,2,3,4,5,6,17 },
		{ 0,1,2,3,4,5,6,17 },
		{ 0,1,2,3,4,5,6,17 },
		{ 0,1,2,3,4,5,6,17 },
		{ 0,1,2,3,4,5,6,17 },
		{ 0,1,2,3,4,5,6,17 },
		{ 0,1,2,3,4,5,6,17 },
		{ 0,1,2,3,4,5,6,17 },
		{ 0,1,2,17 },
		{ 0,1,2,17 },
		{ 0,1,2,17 },
		{ 0,1,2,17 }
	};

	private final short bal27mp1[] = { 4,4,4,4,4,4,4,4,4,4,4,3,3,3,3,3,3,3,3,3,3,3,3,2,2,2,2 };
	private final short getbits[] = { 0,5,7,3,10,4,5,6,7,8,9,10,11,12,13,14,15,16 };
	private final short grouping[] = { 0,3,5,0,9,0,0,0,0,0,0,0,0,0,0,0,0,0 };
	private final int MPA_48M1L2[] = { 0,768,1152,1344,1536,1920,2304,2688,3072,3840,4608,5376,6144,7680,9216,16384 };
	private final int STEREO = 0;
	private final int JSTEREO = 1;
	private final int DUAL = 2;
	private final int SINGLE = 3;

	public static final int SINGLE_TO_3DSTEREO= 1;
	public static final int SINGLE_TO_STEREO  = 2;
	public static final int SINGLE_TO_JSTEREO = 3;
	public static final int SPLIT_INTO_SINGLE = 4;
	public static final int SPLIT_INTO_SINGLE_DOUBLED = 5;
	public static final int SPLIT_DUAL_INTO_SINGLE_DOUBLED = 6;

	private boolean[] framebuffer;

	private int maxBitSize = 0;
	private short[] Bal = new short[0];
	private short[][] Allocation;
	private int[] Sizes;
	private int[] BRindex = new int[3];
	private int Restart = 0;
	private int error_flag = 0;
	private AudioFormat Audio;

	//init
	public MpaConverter()
	{
		Audio = new AudioFormat(CommonParsing.MPEG_AUDIO);
		Sizes = MPA_48M1L2;
		Arrays.sort(Sizes);
		maxBitSize = Sizes[14];
		Bal = bal27mp1;
		Allocation = alloc27;

		framebuffer = new boolean[maxBitSize]; //max=1152bytes=9216bits
	}

	/**
	 *
	 */
	public void resetBuffer()
	{
		Arrays.fill(framebuffer, false);
	}

	/**
	 * modify mpegaudio frame 48khz 56..384kbps M1L2
	 */
	public byte[][] modifyframe(byte[] AudioFrame, int MpaConversionMode)
	{
		byte newAudioFrames[][] = new byte[2][AudioFrame.length];

		for (int i = 0; i < 3; i++)  //0-s, 1-l, 2-r
			BRindex[i] = (int)(0xF & CommonParsing.getAudioProcessingFlags()>>>(4 + (i<<2)));

		Restart = 0;
		error_flag = 0;

		//secure, but most was already done by X.java
		if (Audio.parseHeader(AudioFrame, 0) != 1 || Audio.getID() != 1 || Audio.getLayer() != 2 || Audio.getSamplingFrequency() != 48000 || 
			Audio.getBitrate() < 56000 || (Audio.getMode() == SINGLE && Audio.getBitrate() > 192000) || (Audio.getMode() != SINGLE && Audio.getBitrate() < 112000))
		{
			System.arraycopy(AudioFrame, 0, newAudioFrames[0], 0, AudioFrame.length);
			System.arraycopy(AudioFrame, 0, newAudioFrames[1], 0, AudioFrame.length); // copy frame

			Common.setMessage(Resource.getString("audio.msg.convert.disabled", "" + (CommonParsing.getAudioProcessingFlags()>>>18)));

			CommonParsing.setAudioProcessingFlags(CommonParsing.getAudioProcessingFlags() | 0x1000CL);

			return newAudioFrames;
		}

		//explicit call necessary, if crc removing is disabled in presettings
		Audio.removeCRC(AudioFrame, true);

		switch(MpaConversionMode)
		{
			case SINGLE_TO_3DSTEREO:  //single channel to 3D
			case SINGLE_TO_STEREO:  //single channel to stereo
			case SINGLE_TO_JSTEREO:  //single channel to jstereo
				if (Audio.getChannel() == 1)
					newAudioFrames = createByteArrays(makeTwoChannel(createBitArray(AudioFrame), MpaConversionMode, 0));

				else
					newAudioFrames = adaptSourceFrame(AudioFrame, newAudioFrames, MpaConversionMode);  //check to pass BR

				break;

			case SPLIT_INTO_SINGLE: //dual-stereo-jointstereo to 2 x mono
			case SPLIT_INTO_SINGLE_DOUBLED: //dual-stereo-jointstereo to 2 x jstereo - doubled mono
				if (Audio.getChannel() == 2)
					newAudioFrames = createByteArrays(splitTwoChannel(createBitArray(AudioFrame), MpaConversionMode));

				else
					newAudioFrames = adaptSourceFrame(AudioFrame, newAudioFrames, MpaConversionMode);  //check to pass BR

				break;

			case SPLIT_DUAL_INTO_SINGLE_DOUBLED: //dual-only to 2 x jstereo - doubled mono
				if (Audio.getMode() == DUAL)
					newAudioFrames = createByteArrays(splitTwoChannel(createBitArray(AudioFrame), MpaConversionMode));

				else
					newAudioFrames = adaptSourceFrame(AudioFrame, newAudioFrames, MpaConversionMode);  //check to pass BR
		}

		CommonParsing.setAudioProcessingFlags(CommonParsing.getAudioProcessingFlags() & ~0xFFFCL);

		for (int i = 0; i < 3; i++)  //0-s, 1-l, 2-r
			CommonParsing.setAudioProcessingFlags(CommonParsing.getAudioProcessingFlags() | BRindex[i]<<(4 + (i<<2)));

		CommonParsing.setAudioProcessingFlags(CommonParsing.getAudioProcessingFlags() | Restart<<2);

		if (error_flag > 0)
			Common.setMessage(Resource.getString("audio.msg.convert.error", String.valueOf(error_flag), String.valueOf(CommonParsing.getAudioProcessingFlags()>>>18)));

		return newAudioFrames;
	}

	private void setError(int error)
	{
		error_flag |= error;
	}

	//check to pass BR
	private byte[][] adaptSourceFrame(byte[] AudioFrame, byte[][] input, int MpaConversionMode)
	{
		System.arraycopy(AudioFrame, 0, input[0], 0, AudioFrame.length);
		System.arraycopy(AudioFrame, 0, input[1], 0, AudioFrame.length); // copy frame

		int[] size = new int[2];
		size[1] = size[0] = (0xF0 & AudioFrame[2])>>>4;

		switch(MpaConversionMode)
		{
			case SINGLE_TO_3DSTEREO:  //single channel to 3D
			case SINGLE_TO_STEREO:  //single channel to stereo
			case SINGLE_TO_JSTEREO:  //single channel to jstereo

				if (size[0] == BRindex[0]) //same size
					return input;

				else if (size[0] > BRindex[0])
				{ //restart with new size
					if (BRindex[0] != 0)
						Restart |= 3;

					BRindex[0] = size[0];

					return input;
				}

				break;

			case SPLIT_INTO_SINGLE: //dual-stereo-jointstereo to 2 x mono
			case SPLIT_INTO_SINGLE_DOUBLED: //dual-stereo-jointstereo to 2 x jstereo - doubled mono
			case SPLIT_DUAL_INTO_SINGLE_DOUBLED: //dual-only to 2 x jstereo - doubled mono

				if (size[0] == BRindex[1] && size[1] == BRindex[2] && Audio.getChannel() == 2) //same size
					return input;

				else
				{ //restart with new size
					if (size[0] > BRindex[1])
					{  //fill to size L
						if (BRindex[1] != 0)
							Restart |= 1;

						BRindex[1] = size[0];
					}

					if (size[1] > BRindex[2])
					{  //fill to size R
						if (BRindex[2] != 0)
							Restart |= 2;

						BRindex[2] = size[1];
					}

					if (Restart > 0)
						return input;
				}
		}

		byte[][] output = new byte[2][0];

		for (int ch = 0; ch < 2; ch++)
		{
			output[ch] = new byte[Sizes[BRindex[ch + (MpaConversionMode>>>2)]]>>>3];

			System.arraycopy(input[ch], 0, output[ch], 0, input[ch].length);

			output[ch][2] &= (byte) ~0xF0;
			output[ch][2] |= (byte) BRindex[ch + (MpaConversionMode>>>2)]<<4;

			if (MpaConversionMode < SPLIT_INTO_SINGLE)
				break;

			else if (MpaConversionMode > SPLIT_INTO_SINGLE)
			{
				if (Audio.getChannel() == 1)
					output[ch] = createByteArray(makeTwoChannel(createBitArray(output[ch]), SINGLE_TO_JSTEREO, ch + 1));
			}

			else if (BRindex[ch + 1] > 10)
				output[ch] = createByteArray(makeTwoChannel(createBitArray(output[ch]), SINGLE_TO_JSTEREO, ch + 1));
		}

		return output;
	}

	//bytearray to bitarray
	private boolean[] createBitArray(byte[] input)
	{
		boolean[] output = new boolean[maxBitSize]; //max=1152bytes=9216bits

		for (int a=0; a < input.length; a++)
			for (int b=0; b < 8; b++)
				if ( ((0x80>>>b) & input[a]) != 0 ) 
					output[b + (a<<3)] = true;

		return output;
	}

	//1ch_bitarray to 1ch_bytearray, frame in bitform must already be compliant, but will be shortened in this method to the indexed BR
	private byte[] createByteArray(boolean[] input)
	{
		int size = getSizeFromIndex(input)>>>3;
		byte[] output = new byte[size];

		for (int a=0; a < size; a++)
			for (int b=0; b < 8; b++)
				output[a] |= input[b + (a<<3)] ? 0x80>>>b : 0;

		return output;
	}

	//1ch_bitarray to 2ch_bytearray, frame in bitform must already be compliant, but will be shortened in this method to the indexed BR
	private byte[][] createByteArrays(boolean[] input)
	{
		int size = getSizeFromIndex(input)>>>3;
		byte[][] output = { new byte[size], new byte[0] };

		for (int a=0; a < size; a++)
			for (int b=0; b < 8; b++)
				output[0][a] |= input[b + (a<<3)] ? 0x80>>>b : 0;

		return output;
	}

	//2ch_bitarray to 2ch_bytearray, frame in bitform must already be compliant, but will be shortened in this method to the indexed BR
	private byte[][] createByteArrays(boolean[][] input)
	{
		int[] size = new int[2];

		for (int ch=0; ch < 2; ch++)
			size[ch] = getSizeFromIndex(input[ch])>>>3;

		byte[][] output = { new byte[size[0]], new byte[size[1]] };

		for (int ch=0; ch < 2; ch++)
			for (int a=0; a < size[ch]; a++)
				for (int b=0; b < 8; b++)
					output[ch][a] |= input[ch][b + (a<<3)] ? 0x80>>>b : 0;

		return output;
	}

	//read out BR_index in bitlength
	private int getSizeFromIndex(boolean[] input)
	{
		int size = 0;

		for (int i = 0; i < 4; i++)
			size |= input[i + 16] ? 8>>>i : 0;

		size = Sizes[size];

		return size;
	}

	//set Bitrate index
	private void setBitRateIndex(boolean[] input, int size, int channel)
	{
		if ( (size = Arrays.binarySearch(Sizes, size)) < 0);
			size = Math.abs(size) - 1;

		if (channel == 0 && size < 7)
			size = 7;

		if (channel > 0 && size < 3)
			size = 3;

		size = updateCBRIndex(size, channel);

		for (int i = 0; i < 4; i++)
			input[i + 16] = (size & 8>>i) != 0 ? true : false;
	}

	//update Bitrate index for CBR
	private int updateCBRIndex(int size, int channel)
	{
		if (BRindex[channel] == 0)  // if 0 set first BR
			BRindex[channel] = size;

		else if (BRindex[channel] >= size)
			size = BRindex[channel];

		else
		{
			Restart |= (3 - channel);
			BRindex[channel] = size;
		}

		return size;
	}

	//set channelmode
	private void setChannelMode(boolean[] input, int mode)
	{
		mode <<= 2;

		for (int a=0; a < 4; a++)
			input[a + 24] = (mode & 8>>a) != 0;
	}

	//single bitarray to one 2channel bitarray, bitrate_index must hold place for whole frame data
	private boolean[] makeTwoChannel(boolean[] input, int MpaConversionMode, int channel)
	{
		boolean[] output = new boolean[maxBitSize];
		int i = 32;
		int i_b = 32;
		int o = 32;
		int size = getSizeFromIndex(input);

		System.arraycopy(input, 0, output, 0, 32); //copy source frameheader 1:1

		switch(MpaConversionMode)
		{
			case SINGLE_TO_3DSTEREO:  //make a 3d stereo (delayed channel B)
				int _Bal_length = Bal.length;
				int[][] _allocation = new int[2][_Bal_length];
				int[][] _scfsi = new int[2][_Bal_length];

				try {
					// copy BAL
					for (int a = 0; a < _Bal_length; a++)
					{
						// ch A
						for (int b = 0; b < Bal[a]; b++)
						{
							if (input[i + b])
							{
								output[o + b] = true;
								_allocation[0][a] |= 1<<(Bal[a] - 1 - b);
							}
						}

						i += Bal[a];
						o += Bal[a]; 

						// ch B
						for (int b = 0; b < Bal[a]; b++)
						{
							if (framebuffer[i_b + b])
							{
								output[o + b] = true;
								_allocation[1][a] |= 1<<(Bal[a] - 1 - b);
							}
						}

						i_b += Bal[a];
						o += Bal[a]; 
					}

					// copy SCFSI
					for (int a = 0; a < _Bal_length; a++)
					{
						// ch A
						if (_allocation[0][a] != 0)
						{
							for (int b = 0; b < 2; b++)
							{
								if (input[i + b])
								{
									output[o + b] = true;
									_scfsi[0][a] |= 1<<(1 - b);
								}
							}

							i += 2; 
							o += 2;
						}

						// ch B
						if (_allocation[1][a] != 0)
						{
							for (int b = 0; b < 2; b++)
							{
								if (framebuffer[i_b + b])
								{
									output[o + b] = true;
									_scfsi[1][a] |= 1<<(1 - b);
								}
							}

							i_b += 2; 
							o += 2;
						}
					}

					// copy Scalefactors
					for (int a = 0, b = 0; a < _Bal_length; a++)
					{
						// ch A
						if (_allocation[0][a] != 0)
						{
							switch (_scfsi[0][a])
							{
								case 0:
									b = 18;
									break;

								case 1:
								case 3:
									b = 12;
									break;

								case 2:
									b = 6;
							}

							System.arraycopy(input, i, output, o, b);

							i += b; 
							o += b; 
						}

						// ch B
						if (_allocation[1][a] != 0)
						{
							switch (_scfsi[1][a])
							{
								case 0:
									b = 18;
									break;

								case 1:
								case 3:
									b = 12;
									break;

								case 2:
									b = 6;
							}

							System.arraycopy(framebuffer, i_b, output, o, b);

							i_b += b; 
							o += b; 
						}
					}

					// copy Samples
					for (int x = 0; x < 12; x++)
					{
						for (int a = 0, j, k; a < _Bal_length; a++)
						{
							// ch A
							if (_allocation[0][a] != 0)
							{
								j = Allocation[a][_allocation[0][a]];
								k = getbits[j];

								if (grouping[j] > 0)
								{
									System.arraycopy(input, i, output, o, k); 
									o += k;
									i += k;
								}

								else
								{
									System.arraycopy(input, i, output, o, (3 * k)); 
									o += (3 * k);
									i += (3 * k);
								}
							}

							// ch B
							if (_allocation[1][a] != 0)
							{
								j = Allocation[a][_allocation[1][a]];
								k = getbits[j];

								if (grouping[j] > 0)
								{
									System.arraycopy(framebuffer, i_b, output, o, k); 
									o += k;
									i_b += k;
								}

								else
								{
									System.arraycopy(framebuffer, i_b, output, o, (3 * k)); 
									o += (3 * k);
									i_b += (3 * k);
								}
							}
						}
					}
				} catch (Exception e) {
					setError(1);
				}

				setChannelMode(output, STEREO); //set to stereo/jstereo
				setBitRateIndex(output, o, channel); //set BR_index 

				System.arraycopy(input, 0, framebuffer, 0, input.length); //copy source frame 1:1
				break;

			case SINGLE_TO_STEREO: // make a stereo
			case SINGLE_TO_JSTEREO: // make a jointstereo mode 00,  sb 4..31 shared
			case SPLIT_DUAL_INTO_SINGLE_DOUBLED:
				int Bal_length = Bal.length;
				int[] allocation = new int[Bal_length];
				int[] scfsi = new int[Bal_length];

				try {
					// copy BAL
					for (int a=0; a < Bal_length; a++)
					{
						for (int b=0; b < Bal[a]; b++)
						{
							if (input[i + b])
							{
								output[o + b] = true;

								if (MpaConversionMode == SINGLE_TO_STEREO || a < 4)
									output[o + Bal[a] + b] = true;

								allocation[a] |= 1<<(Bal[a] - 1 - b);
							}
						}

						i += Bal[a];
						o += (MpaConversionMode == SINGLE_TO_STEREO || a < 4) ? (Bal[a]<<1) : Bal[a]; 
					}

					// copy SCFSI
					for (int a=0; a < Bal_length; a++)
					{
						if (allocation[a] != 0)
						{
							for (int b=0; b < 2; b++)
							{
								if (input[i + b])
								{
									output[o + b] = true;
									output[o + b + 2] = true;
									scfsi[a] |= 1<<(1 - b);
								}
							}

							i += 2; 
							o += 4;
						}
					}

					// copy Scalefactors
					for (int a=0, b=0; a < Bal_length; a++)
					{
						if (allocation[a] != 0)
						{
							switch (scfsi[a])
							{
								case 0:
									b = 18;
									break;

								case 1:
								case 3:
									b = 12;
									break;

								case 2:
									b = 6;
							}

							System.arraycopy(input, i, output, o, b);
							System.arraycopy(input, i, output, o + b, b);
							i += b; 
							o += b<<1; 
						}
					}

					// copy Samples
					for (int x=0; x < 12; x++)
					{
						for (int a=0, j, k; a < Bal_length; a++)
						{
							if (allocation[a] != 0)
							{
								j = Allocation[a][allocation[a]];
								k = getbits[j];

								if (grouping[j] > 0)
								{
									System.arraycopy(input, i, output, o, k); 
									o += k;

									if (MpaConversionMode == SINGLE_TO_STEREO || a < 4)
									{ 
										System.arraycopy(input, i, output, o, k); 
										o += k; 
									}

									i += k;
								}

								else
								{
									System.arraycopy(input, i, output, o, (3 * k)); 
									o += (3 * k);

									if (MpaConversionMode == SINGLE_TO_STEREO || a < 4)
									{ 
										System.arraycopy(input, i, output, o, (3 * k)); 
										o += (3 * k); 
									}

									i += (3 * k);
								}
							}
						}
					}
				} catch (Exception e) {
					setError(1);
				}

				setChannelMode(output, MpaConversionMode < SPLIT_INTO_SINGLE ? (1 & MpaConversionMode) : JSTEREO); //set to stereo/jstereo
				setBitRateIndex(output, o, channel); //set BR_index 
		}

		return output;
	}

	//2channel bitarray to 2 single channel bitarrays
	private boolean[][] splitTwoChannel(boolean[] input, int MpaConversionMode)
	{
		boolean[][] output = new boolean[2][maxBitSize];
		System.arraycopy(input, 0, output[0], 0, 32); //copy source frameheader 1:1 L
		System.arraycopy(input, 0, output[1], 0, 32); //copy source frameheader 1:1 R

		int i = 32;
		int[] o = { 32,32 };
		int Bal_length = Bal.length;
		int[][] allocation = new int[2][Bal_length];
		int[][] scfsi = new int[2][Bal_length];

		int bound = 32;

		if (Audio.getMode() == 1) //source is jointstereo
			bound=(Audio.getModeExtension() + 1) * 4; 

		if (bound == 32) 
			bound = Bal_length;

		try {
			//copy BAL
			for (int a=0; a < bound; a++)
			{
				for (int ch=0; ch < 2; ch++)
				{
					for (int b=0; b < Bal[a]; b++)
					{
						if (input[i + b])
						{
							allocation[ch][a] |= 1<<(Bal[a] - 1 - b); 
							output[ch][o[ch] + b] = true; 
						}
					}

					i += Bal[a]; 
					o[ch] += Bal[a]; 
				}
			}

			for (int a=bound; a < Bal_length; a++)
			{
				for (int b=0; b < Bal[a]; b++)
				{
					if (input[i + b])
					{
						for (int ch=0; ch < 2; ch++)
						{
							allocation[ch][a] |= 1<<(Bal[a] - 1 - b);
							output[ch][o[ch] + b] = true;
						}
					}
				}

				i += Bal[a]; 

				for (int ch=0; ch < 2; ch++)
					o[ch] += Bal[a]; 
			}

			//copy SCFSI
			for (int a=0; a < Bal_length; a++)
			{
				for (int ch=0; ch < 2; ch++)
				{
					if (allocation[ch][a] != 0)
					{
						for (int b=0; b < 2; b++)
						{
							if (input[i + b])
							{
								scfsi[ch][a] |= 1<<(1 - b);
								output[ch][o[ch] + b] = true;
							}
						}

						i += 2; 
						o[ch] += 2; 
					}
				}
			}

			//copy Scalefactors
			for (int a=0, b=0; a < Bal_length; a++)
			{
				for (int ch=0; ch < 2; ch++)
				{
					if (allocation[ch][a] != 0)
					{
						switch (scfsi[ch][a])
						{
							case 0:
								b = 18;
								break;

							case 1:
							case 3:
								b = 12;
								break;

							case 2:
								b = 6;
						}

						System.arraycopy(input, i, output[ch], o[ch], b);
						i += b;
						o[ch] += b; 
					}
				}
			}

			//copy Samples
			for (int x=0; x < 12; x++)
			{
				for (int a=0; a < bound; a++)
				{
					for (int ch=0, j, k; ch < 2; ch++)
					{
						if (allocation[ch][a] != 0)
						{
							j = Allocation[a][allocation[ch][a]];
							k = getbits[j];

							if (grouping[j] > 0)
							{
								System.arraycopy(input, i, output[ch], o[ch], k);
								i += k;
								o[ch] += k; 
							}

							else
							{
								System.arraycopy(input, i, output[ch], o[ch], (3 * k));
								i += (3 * k);
								o[ch] += (3 * k); 
							}
						}
					}
				}

				for (int a=bound, j, k; a < Bal_length; a++)
				{
					if (allocation[0][a] != 0)
					{
						j = Allocation[a][allocation[0][a]];
						k = getbits[j];

						if (grouping[j] > 0)
						{
							for (int ch=0; ch < 2; ch++)
							{
								System.arraycopy(input, i, output[ch], o[ch], k);
								o[ch] += k; 
							}

							i+=k;
						}

						else
						{
							for (int ch=0; ch < 2; ch++)
							{
								System.arraycopy(input, i, output[ch], o[ch], (3 * k));
								o[ch] += (3 * k); 
							}

							i += (3 * k);
						}
					}
				}
			}
		} catch (Exception e) {
			setError(2);
		}

		for (int ch=0; ch < 2; ch++)
		{
			setChannelMode(output[ch], SINGLE); //set to mono
			setBitRateIndex(output[ch], o[ch], ch + 1); //set BR_index 

			if (getSizeFromIndex(output[ch]) > Sizes[10] || MpaConversionMode == SPLIT_INTO_SINGLE_DOUBLED || MpaConversionMode == SPLIT_DUAL_INTO_SINGLE_DOUBLED)
				output[ch] = makeTwoChannel(output[ch], 3, ch + 1);
		}

		return output;
	}

}

/***** options[17]
	CommonParsing.setAudioProcessingFlags()
	CommonParsing.getAudioProcessingFlags();

*  bit 1,0 : if options[10]>=4,  not used anymore, but still set
*   00 = std CBR in complete file
*   01 = VBR same mode = same BR
*   10 = VBR each frame/channel its own 
*   11 = free
*  bit 3,2 : 
*   00 = no restart
*   01 = restart due left ch.
*   10 = restart due right ch.
*   11 = restart due both ch.
*
* bit 7,6,5,4 :  bitrate value of last frame 2channel
* bit 11,10,9,8 :  bitrate value of last frame left
* bit 15,14,13,12 :  bitrate value of last frame right
*
* bit 16 : set to 1 if conversion isn't possible, clear befor the next file
* bit 17 free
* bit63..18 MSB : current audioframes number
******/
