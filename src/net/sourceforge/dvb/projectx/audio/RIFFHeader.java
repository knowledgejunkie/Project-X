/*
 * @(#)RIFFHEADER.java - create a RIFF Header for nonPCM data
 *
 * Copyright (c) 2002-2005 by dvb.matt, All Rights Reserved. 
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

package net.sourceforge.dvb.projectx.audio;

public class RIFFHeader
{

	byte[] riffacm = { 82, 73, 70, 70,  0,  0,  0,  0, 87, 65, 86, 69,102,109,116, 32,
		30,  0,  0,  0, 85,  0,  1,  0,  1,  0,  0,  0,  0,  0,  0,  0,
		1,  0,  0,  0, 12,  0,  1,  0,  2,  0,  0,  0,  0,  0,  1,  0,
		113,  5,102, 97, 99,116,  4,  0,  0,  0,  0,  0,  0,  0,100, 97,
		116, 97,  0,  0,  0,  0 };

	byte[] riffbwf = { 82, 73, 70, 70,  0,  0,  0,  0, 87, 65, 86, 69,102,109,116, 32,
		40,  0,  0,  0, 80,  0,  1,  0,  1,  0,  0,  0,  0,  0,  0,  0,
		0,  0,  0,  0, 22,  0,  0,  0,  1,  0,  0,  0,  0,  0,  0,  0,
		0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,102, 97, 99,116,
		4,  0,  0,  0,  0,  0,  0,  0,100, 97,116, 97,  0,  0,  0,  0 };

	byte[] riffac3 = { 82, 73, 70, 70,  0,  0,  0,  0, 87, 65, 86, 69,102,109,116, 32,
		18,  0,  0,  0,  0, 32,  1,  0,  1,  0,  0,  0,  0,  0,  0,  0,
		0,  0,  0,  0, 18,  0,100, 97,116, 97,  0,  0,  0,  0, };

	long Samples=0, SampleCount=0;


	public void RiffData(int[] riffdata) {

		Samples += riffdata[2]; 
		SampleCount++;

		int nSamplesPerSec = (255&riffbwf[24]) | (255&riffbwf[24+1])<<8 | (255&riffbwf[24+2])<<16 | (255&riffbwf[24+3])<<24;
		int dwHeadBitrate  = (255&riffbwf[40]) | (255&riffbwf[40+1])<<8 | (255&riffbwf[40+2])<<16 | (255&riffbwf[40+3])<<24;
		int nBlockAlign    = (255&riffbwf[32]) | (255&riffbwf[32+1])<<8;

		if ( nBlockAlign == 0 ) //nBlockAlign
			for (int a=0;a<2;a++) 
				riffacm[44+a] = riffbwf[32+a] = (byte)(255 & riffdata[8]>>>(a*8));
		else if ( nBlockAlign != 1 &&  nBlockAlign != riffdata[8] ) {
			riffbwf[32] = 1; 
			riffbwf[32+1] = 0;
		}

		if ( nSamplesPerSec == 1 )  //nSamplesPerSec
			for (int a=0;a<4;a++) 
				riffacm[24+a] = riffbwf[24+a] = (byte)(255 & riffdata[2]>>>(a*8)); 
		else if ( nSamplesPerSec != 0 &&  nSamplesPerSec != riffdata[2] ) 
			for (int a=0;a<4;a++) 
				riffacm[24+a] = riffbwf[24+a] = 0;

		if ( dwHeadBitrate == 1 ) //dwHeadBitrate
			for (int a=0;a<4;a++) 
				riffbwf[40+a] = (byte)(255 & riffdata[6]>>>(a*8)); 
		else if ( dwHeadBitrate != 0 &&  dwHeadBitrate != riffdata[6] ) 
			for (int a=0;a<4;a++) 
				riffbwf[40+a] = 0; 

		if ( riffdata[3]==2 ) // fwHeadModeExt
			riffbwf[46] |= (byte)riffdata[5];  

		if ( riffbwf[22]==1 ) // nChannels
			riffacm[22] = riffbwf[22] = (byte)riffdata[4];    

		riffbwf[38] |= (byte)riffdata[1];   // fwHeadLayer
		riffbwf[44] |= (byte)riffdata[3];   // fwHeadMode
		riffbwf[48] |= (byte)riffdata[7];   // wHeadEmphasis
		riffbwf[50] |= (byte)riffdata[0];   // fwHeadFlags
    
	}


	public void AC3RiffData(int[] riffdata) {

		Samples += riffdata[2]; 
		SampleCount++;

		int nSamplesPerSec = (255&riffac3[24]) | (255&riffac3[24+1])<<8 | (255&riffac3[24+2])<<16 | (255&riffac3[24+3])<<24;
		int nBlockAlign    = (255&riffac3[32]) | (255&riffac3[32+1])<<8;

		if ( nBlockAlign == 0 )  //nBlockAlign
			for (int a=0;a<2;a++) 
				riffac3[32+a] = (byte)(255 & riffdata[8]>>>(a*8));
		else if ( nBlockAlign != 1 &&  nBlockAlign != riffdata[8] ) {
			riffac3[32] = 1; 
			riffac3[32+1] = 0;
		}

		if ( nSamplesPerSec == 1 ) //nSamplesPerSec
			for (int a=0;a<4;a++) 
				riffac3[24+a] = (byte)(255 & riffdata[2]>>>(a*8));
		else if ( nSamplesPerSec != 0 &&  nSamplesPerSec != riffdata[2] ) 
			for (int a=0;a<4;a++) 
				riffac3[24+a] = 0;

		if ( (0xFF&riffac3[22]) < riffdata[4] )   // nChannels
			riffac3[22] = (byte)riffdata[4];    
    
	}

	/** placeholder **/
	public byte[] ACMnull() { 
		return new byte[70]; 
	}
	public byte[] BWFnull() { 
		return new byte[80]; 
	}
	public byte[] AC3null() { 
		return new byte[46]; 
	} 

	/** update header **/
	public byte[] ACM() { 
		return riffacm; 
	} 
	public byte[] BWF() { 
		return riffbwf; 
	} 
	public byte[] AC3() { 
		return riffac3; 
	} 

	public void Length(long filelength, long timelength) {
		int lengthACM = (int)filelength-70;
		int lengthBWF = (int)filelength-80;
		int lengthAC3 = (int)filelength-46;

		for (int a=0;a<4;a++) {
			riffacm[4+a] = (byte)(255 & (lengthACM+62)>>>(a*8));
			riffbwf[4+a] = (byte)(255 & (lengthBWF+72)>>>(a*8));
			riffac3[4+a] = (byte)(255 & (lengthAC3+38)>>>(a*8));
			riffacm[66+a] = (byte)(255 & lengthACM>>>(a*8));
			riffbwf[76+a] = (byte)(255 & lengthBWF>>>(a*8));
			riffac3[42+a] = (byte)(255 & lengthAC3>>>(a*8));
		}

		if (filelength>100) {
			int time = (int)timelength;
			int nAvgBytePerSecACM = (int)(1000L*lengthACM / time );
			int nAvgBytePerSecBWF = (int)(1000L*lengthBWF / time );
			int nAvgBytePerSecAC3 = (int)(1000L*lengthAC3 / time );

			for (int a=0;a<4;a++) { 
				riffacm[28+a] = (byte)(255 & nAvgBytePerSecACM>>>(a*8));
				riffbwf[28+a] = (byte)(255 & nAvgBytePerSecBWF>>>(a*8));
				riffac3[28+a] = (byte)(255 & nAvgBytePerSecAC3>>>(a*8));
			}

			int fact = (int)(1L * (Samples/SampleCount) * time /1000);
			for (int a=0;a<4;a++) 
				riffacm[58+a] = riffbwf[68+a] = (byte)(255 & fact>>>(a*8));
		}
	}
}

