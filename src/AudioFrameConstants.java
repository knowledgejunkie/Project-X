/*
 * @(#)AudioFrameConstants.java - old class for audio constants
 *
 * Copyright (c) 2002-2004 by dvb.matt, All Rights Reserved.
 * 
 * This file is part of X, a free Java based demux utility.
 * X is intended for educational purposes only, as a non-commercial test project.
 * It may not be used otherwise. Most parts are only experimental.
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


public class AudioFrameConstants {

int[] armode = { 0,1,2,3,3,4,4,5 };
int[] arsample = { 48000,44100,32000,0 };
int[] arbitrate =  { 0,40000,48000,56000,64000,80000,96000,112000,128000,160000,192000,224000,256000,320000,384000,448000,512000,576000,640000,0,0 };

int[] rpadding = { 0,1,1,4 };
int[] rlayer = { 0,4,2,1 };
int[][] rsample = { 
	{ 22050,24000,16000,0 }, 
	{ 44100,48000,32000,0 } 
};
int[] rmode = { 1,2,4,8 };
int[] rchnl = { 2,2,2,1 };
int[] rmext = { 1,2,4,8 };
int[] remph = { 1,2,3,4 };
int[][][] rbitrate = {
    { {  0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0 },
      {  0,8000,16000,24000,32000,40000,48000,56000,64000,80000,96000,112000,128000,144000,160000,0  },
      {  0,8000,16000,24000,32000,40000,48000,56000,64000,80000,96000,112000,128000,144000,160000,0  },
      {  0,32000,48000,56000,64000,80000,96000,112000,128000,144000,160000,176000,192000,224000,256000,0 } },
    { {  0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0 },
      {  0,32000,40000,48000,56000,64000,80000,96000,112000,128000,160000,192000,224000,256000,320000,0  },
      {  0,32000,48000,56000,64000,80000,96000,112000,128000,160000,192000,224000,256000,320000,384000,0  },
      {  0,32000,64000,96000,128000,160000,192000,224000,256000,288000,320000,352000,384000,416000,448000,0 } }
};


 /*************************
  * ac3 bitrate constants *
  *************************/
/* 96k,112k,128k,160k,192k,224k,256k,320k,384k,448k,512k,576k,640k
 *   0=48khz,1=44.1khz,2=32khz  32ms,36ms,48ms      44.1khz padding +2 bytes
 */
int[][] ac3const = {
          { 288000000,160,192,224,256,320,384,448,512,640,768,896,1024,1280,1536,1792,2080,2304,2560 },
          { 313469388,174,208,242,278,348,416,486,556,696,834,974,1114,1392,1670,1950,2228,2506,2786 },
          { 432000000,240,288,336,384,480,576,672,768,960,1152,1344,1536,1920,2304,2688,3120,3456,3840 }
};

// 1536s / 44.1 -> 34.8299ms -> 3134.691
// 1536s / 48 -> 32ms -> 2880
// 1536s / 32 -> 48ms -> 4320

/**************************
 * riffdata from mpeg audio *
 **************************/
public int[] RiffFormat(byte[] rh) {  // awaiting a frame byte array, only the header is used
	int[] riffdata = new int[10];

	// fwHeadFlags
	riffdata[0] = (0x8 & rh[1])<<1 | (0x1 & rh[1])<<3 | (0x4 & rh[3]) | (0x8 & rh[3])>>>2 | (0x1 & rh[2]);
	// fwHeadLayer
	riffdata[1] = rlayer[(0x6 & rh[1])>>>1];
	// nSamplesPerSec
	riffdata[2] = rsample[(0x8 & rh[1])>>>3][(0xc & rh[2])>>>2];
	// fwHeadMode
	riffdata[3] = rmode[(0xc0 & rh[3])>>>6];
	// nChannels
	riffdata[4] = rchnl[(0xc0 & rh[3])>>>6];
	// fwHeadModeExt
	riffdata[5] = rmext[(0x30 & rh[3])>>>4];
	// dwHeadBitrate
	riffdata[6] = rbitrate[(0x8 & rh[1])>>>3][(0x6 & rh[1])>>>1][(0xf0 & rh[2])>>>4];
	// wHeadEmphasis
	riffdata[7] = remph[(0x3 & rh[3])];
	// nBlockAlign
	riffdata[8] = (riffdata[1]==1) ? 4 * (12*riffdata[6]/riffdata[2]) :  144 * riffdata[6]/riffdata[2];
	riffdata[8] /= ( (8 & rh[1])==0 && (6 & rh[1])==1 ) ? 2 : 1 ;
	if ( (2&rh[2])!=0 ) 
		riffdata[8] += rpadding[(0x6 & rh[1])>>>1];

	return riffdata;
}

/**************************
 * riffdata from ac3 audio *
 **************************/
public int[] AC3RiffFormat(byte[] frame) {  // awaiting a frame byte array, only the header is used
	int[] riffdata = new int[10];

	// nSamplesPerSec
	riffdata[2] = arsample[(0xc0 & frame[4])>>>6];
	// nChannels
	riffdata[4] = armode[(0xe0 & frame[6])>>>5];
	// dwHeadBitrate
	riffdata[6] = arbitrate[(0x3f & frame[4])>>>1];
	// nBlockAlign
	riffdata[8] = ac3const[(0xc0 & frame[4])>>>6][(0x3E & frame[4])>>>1] + (((1&frame[4])!=0) ? 2 : 0);

	return riffdata;
}

}