/*
 * @(#)AUDIO.java - parse Audioheaders
 *
 * Copyright (c) 2003-2004 by dvb.matt. 
 * 
 * This file is part of X, a free Java based demux utility.
 * X is intended for educational purposes only, as a non-commercial test project.
 * It may not be used otherwise. Most parts are only experimental.
 * 
 * The part of audio parsing was derived from the MPEG/Audio
 * Software Simulation Group's audio codec and ATSC A/52 in a special modified manner.
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

import java.io.*; //DM24012004 081.6 int11 add

public class Audio {

int ID=0;
int Layer=0;
int Protection_bit=0;
int Bitrate=0;
int Sampling_frequency=0;
int Padding_bit=0;
int Private_bit=0;
int Mode=0;
int Mode_extension=0;
int Copyright=0;
int Original=0;
int Channel=0;
int Emphasis=0;
int Size=0;
int Size_base=0;
double Time_length=0.0;

int nID=0;
int nLayer=0;
int nProtection_bit=0;
int nBitrate=0;
int nSampling_frequency=0;
int nPadding_bit=0;
int nPrivate_bit=0;
int nMode=0;
int nMode_extension=0;
int nCopyright=0;
int nOriginal=0;
int nChannel=0;
int nEmphasis=0;
int nSize=0;
int nSize_base=0;
double nTime_length=0;

int lID=0;
int lLayer=0;
int lProtection_bit=0;
int lBitrate=0;
int lSampling_frequency=0;
int lPadding_bit=0;
int lPrivate_bit=0;
int lMode=0;
int lMode_extension=0;
int lCopyright=0;
int lOriginal=0;
int lChannel=0;
int lEmphasis=0;
int lSize=0;
int lSize_base=0;
double lTime_length=0;

int bitrate_index[][][] = {{
	{-1,8000,16000,24000,32000,40000,48000,56000,64000,
	80000,96000,112000,128000,144000,160000,0 },		//MPG-2, L3
	{-1,8000,16000,24000,32000,40000,48000,56000,64000,
	80000,96000,112000,128000,144000,160000,0 },		//MPG-2, L2
	{-1,32000,48000,56000,64000,80000,96000,112000,128000,
	144000,160000,176000,192000,224000,256000,0 }		//MPG-2, L1
},{
	{-1,32000,40000,48000,56000,64000,80000,96000,
	112000,128000,160000,192000,224000,256000,320000, 0 },	//MPG-1, L3
	{-1,32000,48000,56000,64000,80000,96000,112000,
	128000,160000,192000,224000,256000,320000,384000, 0 },	//MPG-1, L2
	{-1,32000,64000,96000,128000,160000,192000,224000,
	256000,288000,320000,352000,384000,416000,448000,0 }	//MPG-1, L1
},{
	{-1, 6000, 8000, 10000, 12000, 16000, 20000, 24000,    //MPG-2.5, L3??
	28000, 320000, 40000, 48000, 56000, 64000, 80000, 0 },
	{-1, 6000, 8000, 10000, 12000, 16000, 20000, 24000,    //MPG-2.5, L2
	28000, 320000, 40000, 48000, 56000, 64000, 80000, 0 },
	{-1, 8000, 12000, 16000, 20000, 24000, 32000, 40000,    //MPG-2.5, L1
	48000, 560000, 64000, 80000, 96000, 112000, 128000, 0 }
}};

int frequency_index[][] = {
	{ 22050,24000,16000,0 },	//MPG2 - 22.05,24,16khz
	{ 44100,48000,32000,0 },	//MPG1 - 44.1 ,48,32khz
	{ 11025,12000,8000,0 }		//MPG2.5 - 11.025,12,8khz
};

double time_index[] = { 0.0,103680000.0,103680000.0,34560000.0 };	//L3,L2,L1 * 90

String[] dID = { 
	"MPEG-2","MPEG-1","MPEG-2.5" 
};
String[] dLayer = { 
	"n.a.","Layer3","Layer2","Layer1" 
};
String[] dCRC = { 
	"noCRC","CRC" 
};
String[] dMode = { 
	"stereo","jstereo","dual","mono" 
};


/*** parse mpa Header ***/
public int MPA_parseHeader(byte[] frame, int pos) {

	if ( (0xFF&frame[pos])!=0xFF || (0xF0&frame[pos+1])!=0xF0 ) 
		return -1;

	ID = 1&frame[pos+1]>>>3;
        Emphasis = 3&frame[pos+3];

	if (ID==1 && Emphasis==2)
		ID = 2;

	if ( (Layer = 3&frame[pos+1]>>>1) < 1) 
		return -2;

	Protection_bit = (1&frame[pos+1]) ^ 1;

	if ( (Bitrate = bitrate_index[ID][Layer-1][0xF&frame[pos+2]>>>4]) < 1) 
		return -3;

	if ( (Sampling_frequency = frequency_index[ID][3&frame[pos+2]>>>2]) == 0) 
		return -4;

	Padding_bit = 1&frame[pos+2]>>>1;
	Private_bit = 1&frame[pos+2];

	Mode = 3&frame[pos+3]>>>6;
	Mode_extension = 3&frame[pos+3]>>>4;
	if (Mode==0) 
		Mode_extension=0;

	Channel = (Mode==3) ? 1: 2;
	Copyright = 1&frame[pos+3]>>>3;
	Original = 1&frame[pos+3]>>>2;
	Time_length = time_index[Layer]/Sampling_frequency;

	if (ID==1 && Layer==2) {	// MPEG-1, L2 restrictions
		if(Bitrate/Channel < 32000) 
			return -5; /* unsupported bitrate */
		if(Bitrate/Channel > 192000) 
			return -6; /* unsupported bitrate */
	}

	if (Layer<3) {
		Size = (Size_base = 144*Bitrate/Sampling_frequency) + Padding_bit;
		return 1;
	} else {
		Size = (Size_base = (12*Bitrate/Sampling_frequency)*4) + (4*Padding_bit);
		return 2;
	}
}

/*** parse next mpa Header ***/
public int MPA_parseNextHeader(byte[] frame, int pos) {

        if ( (0xFF&frame[pos])!=0xFF || (0xF0&frame[pos+1])!=0xF0 ) 
		return -1;

	nID = 1&frame[pos+1]>>>3;
	nEmphasis = 3&frame[pos+3];

	if (nID==1 && nEmphasis==2)
		nID = 2;

        if ( (nLayer = 3&frame[pos+1]>>>1) < 1) 
		return -2;

        nProtection_bit = (1&frame[pos+1]) ^ 1;

	if ( (nBitrate = bitrate_index[nID][nLayer-1][0xF&frame[pos+2]>>>4]) < 1) 
		return -3;

        if ( (nSampling_frequency = frequency_index[nID][3&frame[pos+2]>>>2]) == 0) 
		return -4;

        nPadding_bit = 1&frame[pos+2]>>>1;
        nPrivate_bit = 1&frame[pos+2];

        nMode = 3&frame[pos+3]>>>6;
        nMode_extension = 3&frame[pos+3]>>>4;
        if (nMode==0) 
		nMode_extension=0;

	nChannel = (nMode==3) ? 1: 2;
	nCopyright = 1&frame[pos+3]>>>3;
	nOriginal = 1&frame[pos+3]>>>2;
	nTime_length = time_index[nLayer]/nSampling_frequency;

	if (nID==1 && nLayer==2) {	// MPEG-1,L2 restrictions
		if(nBitrate/Channel < 32000) 
			return -5; /* unsupported bitrate */
		if(nBitrate/Channel > 192000) 
			return -6; /* unsupported bitrate */
	}

	if (nLayer<3) {
		nSize = (nSize_base = 144*nBitrate/nSampling_frequency) + nPadding_bit;
		return 1;
	} else {
		nSize = (nSize_base = (12*nBitrate/nSampling_frequency)*4) + (4*nPadding_bit);
		return 2;
	}
}

/*** save last mpa header ***/
public void saveHeader() {
	lID=ID;
	lLayer=Layer;
	lProtection_bit=Protection_bit;
	lBitrate=Bitrate;
	lSampling_frequency=Sampling_frequency;
	lPadding_bit=Padding_bit;
	lPrivate_bit=Private_bit;
	lMode=Mode;
	lMode_extension=Mode_extension;
	lCopyright=Copyright;
	lOriginal=Original;
	lChannel=Channel;
	lEmphasis=Emphasis;
	lSize=Size;
	lSize_base=Size_base;
	lTime_length=Time_length;
}

/*** verify current & last mpa header ***/
public int MPA_compareHeader() {
	if (lID!=ID) 
		return 1;
	else if (lLayer!=Layer) 
		return 2;
	else if (lBitrate!=Bitrate) 
		return 3;
	else if (lSampling_frequency!=Sampling_frequency) 
		return 4;
	else if (lProtection_bit!=Protection_bit) 
		return 5;
	else if (lMode!=Mode){  //DM01112003 081.5++ fix
		if (Mode+lMode<2)
			return 6;
		else
			return 7;
	}else 
		return 0;
}

/*** display last mpa header ***/
public String MPA_displayHeader() {
	return ("" + dID[lID] + ", " + dLayer[lLayer] + ", " + lSampling_frequency + "Hz, " + dMode[lMode] + ", "+ (lBitrate/1000) + "kbps, " + dCRC[lProtection_bit]);
}

/*** display last mpa header ***/
public String MPA_saveAnddisplayHeader() {
	saveHeader();
	return MPA_displayHeader();
}

/*** delete CRC in mpa ***/
public byte[] MPA_deleteCRC(byte[] frame) {
	if ( (frame[1]&1) == 1) 
		return frame;
	byte[] newframe = new byte[frame.length];
	System.arraycopy(frame,0,newframe,0,4);
	System.arraycopy(frame,6,newframe,4,frame.length-6);
	newframe[1] |= 1;
	return newframe;
}


int ac3_frequency_index[] = { 
	48000,44100,32000,0 
};

int ac3_bitrate_index[] =  { 
	32000,40000,48000,56000,64000,80000,96000,
	112000,128000,160000,192000,224000,256000,
	320000,384000,448000,512000,576000,640000,
	0,0,0,0,0,0,0,0,0,0,0,0,0  // (fix4)
};

int ac3_size_table[][] = {
	{ 128,160,192,224,256,320,384,448,512,640,768,896,1024,1280,1536,1792,2080,2304,2560 },
	{ 138,174,208,242,278,348,416,486,556,696,834,974,1114,1392,1670,1950,2228,2506,2786 },
	{ 192,240,288,336,384,480,576,672,768,960,1152,1344,1536,1920,2304,2688,3120,3456,3840 }
};

String bsmod[] = {
	", CM" , ", ME" , ", K:VI" , ", K:HI" , ", K:D" , ", K:C" , ", K:E" , ", K:VO"
};
String cmixlev[] = {
	",cm-3.0dB",",cm-4.5dB",",cm-6.0dB",",cm-4.5dB"
};
String surmixlev[] = {
	",sm-3dB",",sm-6dB",",sm 0dB",",sm-6dB"
};
String dsurmod[] = {
	"" , ", notDS" , ", DS" , ""
};

String acmod[] = {
	"1+1","1/0","2/0","3/0","2/1","3/1","2/2","3/2"
};

int ac3_channels[] = {
	2,1,2,3,3,4,4,5
};

String lfe[][] = {
	{ ".0",".1" },
	{ "","lfe" }
};

/*** parse ac3 Header ***/
public int AC3_parseHeader(byte[] frame, int pos) {

	if ( (0xFF&frame[pos])!=0xB || (0xFF&frame[pos+1])!=0x77 ) 
		return -1;

	ID = 0;
	Emphasis = 0;
	Protection_bit = 0 ^ 1;
	if ( (Sampling_frequency = ac3_frequency_index[3&frame[pos+4]>>>6]) < 1) 
		return -4;
	if ( (Bitrate = ac3_bitrate_index[0x1F&frame[pos+4]>>>1]) < 1) 
		return -3;

	Layer = 7&frame[pos+5];       //bsmod
	Padding_bit = 1&frame[pos+4];
	Private_bit = 0;
	Mode = 7&frame[pos+6]>>>5;
	Mode_extension = 0;

	int mode = (0xFF&frame[pos+6])<<8 | (0xFF&frame[pos+7]);
	int skip=0;
	if ( (Mode&1) >0 && Mode!=1)
		skip++;
	if ( (Mode&4) > 0)
		skip++;
	if ( Mode==2 ){
	        Mode_extension |= 6 & mode>>>(10-(2*skip));  //DS
		skip++;
	}

	switch (skip){  //lfe
	case 0:
	        Mode_extension |= 1 & mode>>>12;
		break;
	case 1:
	        Mode_extension |= 1 & mode>>>10;
		break;
	case 2:
	        Mode_extension |= 1 & mode>>>8;
		break;
	case 3:
	        Mode_extension |= 1 & mode>>>6;
	}

	Channel = ac3_channels[Mode] + (1&Mode_extension);
	Copyright = 0;
	Original = 0;
	Time_length = 138240000.0/Sampling_frequency;
	Size = (Size_base = ac3_size_table[3&frame[pos+4]>>>6][0x1F&frame[pos+4]>>>1]) + Padding_bit*2;
	return 1;
}

/*** parse ac3 Header ***/
public int AC3_parseNextHeader(byte[] frame, int pos) {

	if ( (0xFF&frame[pos])!=0xB || (0xFF&frame[pos+1])!=0x77 ) 
		return -1;

	nID = 0;
	nEmphasis = 0;
	nProtection_bit = 0 ^ 1;

	if ( (nSampling_frequency = ac3_frequency_index[3&frame[pos+4]>>>6]) < 1) 
		return -4;

	if ( (nBitrate = ac3_bitrate_index[0x1F&frame[pos+4]>>>1]) < 1) 
		return -3;

	nLayer = 7&frame[pos+5];       //bsmod
	nPadding_bit = 1&frame[pos+4];
	nPrivate_bit = 0;

	nMode = 7&frame[pos+6]>>>5;
	int mode = (0xFF&frame[pos+6])<<8 | (0xFF&frame[pos+7]);
	int skip=0;

	if ( (nMode&1) >0 && nMode!=1)  //cmix
		skip++;

	if ( (nMode&4) > 0)  //surmix
		skip++;

	if ( nMode==2 ){  //DS mode
	        nMode_extension |= 6 & mode>>>(10-(2*skip));  //DS
		skip++;
	}

	switch (skip){  //lfe
	case 0:
	        nMode_extension |= 1 & mode>>>12;
		break;
	case 1:
	        nMode_extension |= 1 & mode>>>10;
		break;
	case 2:
	        nMode_extension |= 1 & mode>>>8;
		break;
	case 3:
	        nMode_extension |= 1 & mode>>>6;
	}

	nChannel = ac3_channels[Mode] + (1&nMode_extension);
	nCopyright = 0;
	nOriginal = 0;
	nTime_length = 138240000.0/nSampling_frequency;
	nSize = (nSize_base = ac3_size_table[3&frame[pos+4]>>>6][5&frame[pos+4]>>>1]) + nPadding_bit*2;
	return 1;
}

/*** verify current & last ac3 header ***/
public int AC3_compareHeader() {
	if (lLayer!=Layer)
		return 1;
	else if (lBitrate!=Bitrate) 
		return 2;
	else if (lSampling_frequency!=Sampling_frequency) 
		return 3;
	else if (lMode!=Mode)
		return 4;
	else if (lMode_extension!=Mode_extension)
		return 5;
	else 
		return 0;
}

/*** display last ac3 header ***/
public String AC3_displayHeader() {
	return ("AC-3" + bsmod[lLayer] + ", " + acmod[lMode] + lfe[1][1&lMode_extension] + 
		"(" + ac3_channels[lMode] + lfe[0][1&lMode_extension] + ")" + dsurmod[lMode_extension>>>1] + ", " + lSampling_frequency + "Hz, " + (lBitrate/1000) + "kbps");
}

/*** display last ac3 header ***/
public String AC3_saveAnddisplayHeader() {
	saveHeader();
	return AC3_displayHeader();
}

//start 081.6 int07
/* DTS stuff taken from the VideoLAN project. */ 
/* Added by R One, 2003/12/18. */ 
int dts_frequency_index[] = { 
	0, 8000, 16000, 32000, 64000, 128000, 
	11025, 22050, 44100, 88200, 176400,
	12000, 24000, 48000, 96000, 192000 
}; 

int dts_bitrate_index[] = { 
	32000, 56000, 64000, 96000, 112000, 128000, 
	192000, 224000, 256000, 320000, 384000, 
	448000, 512000, 576000, 640000, 768000, 
	896000, 1024000, 1152000, 1280000, 1344000, 
	1408000, 1411200, 1472000, 1536000, 1920000, 
	2048000, 3072000, 3840000, 4096000, 0, 0 
}; 
 
String dts_acmod[] = { 
	"1","DM","2/0","2/0", 
	"2/0","3/0","2.1/0","3.1/0", 
	"2/2","3/2","2/2/2","2/2/2", 
	"3/2/2","3.1/2/2","","", 
	"","","","","","","","","","","","","","","","", 
	"","","","","","","","","","","","","","","","", 
	"","","","","","","","","","","","","","","","" 
}; 
 
int dts_channels[] = { 
	1,2,2,2, 2,3,3,4, 4,5,6,6, 7,8,0,0, 
	0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0, 
	0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0, 
	0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0 
}; 
 
/*** parse dts Header ***/ 
public int DTS_parseHeader(byte[] frame, int pos) { 

	if ( frame[pos]!=0x7f || frame[pos+1]!=(byte)0xfe || frame[pos+2]!=(byte)0x80 || frame[pos+3]!=1 ) 
		return -1; 

	ID = 0; 
	Emphasis = 0; 
	Protection_bit = 0 ^ 1; 

	if ( (Sampling_frequency = dts_frequency_index[0xF&(frame[pos+8]>>>2)]) < 1) 
		return -4; 
	Bitrate = dts_bitrate_index[((3&frame[pos+8])<<3)|(7&(frame[pos+9]>>>5))]; 
	if ( Bitrate < 1) 
		return -3; 

	Layer = 0; 
	Padding_bit = 0; 
	Private_bit = 0; 

	Mode = ((0xf&frame[pos+7])<<2) | ((0xc0&frame[pos+8])>>>6); 
	Mode_extension = 0; 
	Channel = dts_channels[Mode]; 
	Copyright = 0; 
	Original = 0; 
	Size = ((1&frame[pos+4])<<6) | ((0xfc&frame[pos+5])>>>2); 
	Size = (Size+1)<<5; 
	Time_length = 90000.0*Size/Sampling_frequency; 
	Size = ((3&frame[pos+5])<<12) | ((0xff&frame[pos+6])<<4) | ((0xf0&frame[pos+7])>>>4); 
	Size++;
	Size_base = Size; 
	return 1; 
} 
 
/*** parse dts Header ***/ 
public int DTS_parseNextHeader(byte[] frame, int pos) { 

	if ( frame[pos]!=0x7f || frame[pos+1]!=(byte)0xfe || frame[pos+2]!=(byte)0x80 || frame[pos+3]!=1 ) 
		return -1; 
 
	nID = 0; 
	nEmphasis = 0; 
	nProtection_bit = 0 ^ 1; 

	if ( (nSampling_frequency = dts_frequency_index[0xF&(frame[pos+8]>>>2)]) < 1) 
		return -4; 
	if ( (nBitrate = dts_bitrate_index[((3&frame[pos+8])<<3)|(7&(frame[pos+9]>>>5))]) < 1)
		return -3; 

	nLayer = 0; 
	nPadding_bit = 0; 
	nPrivate_bit = 0; 

	nMode = ((0xf&frame[pos+7])<<2) | ((0xc0&frame[pos+8])>>>6); 
	nMode_extension = 0; 
	nChannel = dts_channels[nMode]; 
	nCopyright = 0; 
	nOriginal = 0; 
	nSize = ((1&frame[pos+4])<<6) | ((0xfc&frame[pos+5])>>>2); 
	nSize = (nSize+1)<<5; 
	nTime_length = 90000.0*nSize/nSampling_frequency; 
	nSize = ((3&frame[pos+5])<<12) | ((0xff&frame[pos+6])<<4) | ((0xf0&frame[pos+7])>>>4);
	nSize++;
	nSize_base = nSize; 
	return 1; 
} 
 
/*** verify current & last dts header ***/ 
public int DTS_compareHeader() { 
	if (lLayer!=Layer) 
		return 1; 
	else if (lBitrate!=Bitrate) 
		return 2; 
	else if (lSampling_frequency!=Sampling_frequency) 
		return 3; 
	else if (lMode!=Mode) 
		return 4; 
	else if (lMode_extension!=Mode_extension) 
		return 5; 
	else if (lSize!=Size) 
		return 6; 
	else 
		return 0; 
} 
 
/*** display last dts header ***/ 
public String DTS_displayHeader() { 
	return ("DTS, " + dts_acmod[lMode] + "(" + dts_channels[lMode] + "), " + lSampling_frequency + "Hz, " + (lBitrate/1000.0) + "kbps, " + lSize + "BpF"); 
} 
 
/*** display last dts header ***/ 
public String DTS_saveAnddisplayHeader() { 
	saveHeader(); 
	return DTS_displayHeader(); 
}
//ROne18122003 
//end 081.6 int07


//DM30122003 081.6 int10 add+ 
//DM25012004 081.6 int11 changed
final int WaveChunks[] = 
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

private boolean INTEL=false;

private int littleEndian(byte[] data, int offset, int len, boolean INTEL)
{
	int value=0;
	for (int a=0; a<len; a++)
		value|=INTEL?((0xFF&data[offset+a])<<(a*8)):((0xFF&data[offset+a])<<((len-1-a)*8));
	return value;
}

private int littleEndian(int data, int len)
{
	if (!INTEL)	
		return data;
	if (len==4) 
		return ( (0xFF&data>>>24) | (0xFF&data>>>16)<<8 | (0xFF&data>>>8)<<16 | (0xFF&data)<<24 );
	else 
		return ( (0xFF&data>>>8) | (0xFF&data)<<8 );
}


/*** parse RIFF_WAVE Header ***/ 
public int WAV_parseHeader(byte[] frame, int pos)
{ 
	INTEL=false;
	if ( frame[pos]!=0x52 || frame[pos+1]!=0x49 || frame[pos+2]!=0x46 ) 
		return -1;

	if ( frame[pos+3]==0x46 )
		INTEL=true;
	else if ( frame[pos+3]!=0x58 )
		return -2;

	ID = INTEL?0:1; 
	Emphasis = 0; 
	Protection_bit = 0^1; 

	java.util.Arrays.sort(WaveChunks);

	if (littleEndian(frame,pos+8,4,false)!=WaveChunks[0])
		return -3;

	int chunk=0, chunksize=0;
	for (int a=pos+12; a<frame.length-4; a++){
		if (java.util.Arrays.binarySearch(WaveChunks,(chunk=littleEndian(frame,a,4,false)))<0)
			continue;
		if (chunk==WaveChunks[4]){ //fmt chunk read info datas
			chunksize = littleEndian(frame,a+4,4,INTEL);
			Layer = littleEndian(frame,a+8,2,INTEL);   // Compression type (1=PCM)
			Channel = littleEndian(frame,a+10,2,INTEL); // channels
			Sampling_frequency = littleEndian(frame,a+12,4,INTEL); // samplerate
			Bitrate = littleEndian(frame,a+16,4,INTEL)*8; // avg bits per second
			Mode = littleEndian(frame,a+20,2,INTEL);  // block align, bytes per sample
			Size = littleEndian(frame,a+22,2,INTEL); //bits per sample
			//extrabits not of interest
		}else if (chunk==WaveChunks[2]){ //data chunk, sample data
			chunksize = littleEndian(frame,a+4,4,INTEL);
			Size_base=chunksize; // length of whole sample data
			Emphasis=a+8; // real start of whole sample data
		}else{
			chunksize = littleEndian(frame,a+4,4,INTEL);
		}
		a+=chunksize+3;
	}

	//PTS low+high may exists in 'fact' of MPEG1audio !

	if (Bitrate<1 || Sampling_frequency<1 || Channel<1)
		return -4; 

	Padding_bit = 0; 
	Private_bit = 0; 
	Copyright = 0; 
	Original = 0; 
	Time_length = 90000.0/Sampling_frequency;

	switch (Layer){
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

String LSB_mode[] = { "F","X" };
String compression[] = { "","PCM","MPEG","MPEG-L3","AC3" };

/*** display last wav header ***/ 
public String WAV_displayHeader()
{ 
	return ("RIF" + LSB_mode[lID] + ", " + (lMode_extension > 0 ? compression[lMode_extension] : "tag 0x" + Integer.toHexString(Layer)) + ", " + lChannel + "-ch, " + lSampling_frequency + "Hz, " + lSize + "bit, " + (lBitrate/1000.0) + "kbps"); 
} 
 
/*** display last wav header ***/ 
public String WAV_saveAnddisplayHeader()
{ 
	saveHeader(); 
	return WAV_displayHeader(); 
}
//DM30122003 081.6 int10 add-

//DM24012004 081.6 int11 add+
public byte[] getRiffHeader()
{
	byte RIFF[] = {
		0x52, 0x49, 0x46, 0x46, 0, 0, 0, 0, 
		0x57, 0x41, 0x56, 0x45, (byte)0x66, (byte)0x6d, (byte)0x74, 0x20,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
		0, 0, 0, 0, (byte)0x64, (byte)0x61, (byte)0x74, (byte)0x61, 
		0, 0, 0, 0
	};
	return RIFF;
}

public void fillRiffHeader(String file) throws IOException
{
	RandomAccessFile riff = new RandomAccessFile(file,"rw");
	int len = (int)riff.length()-8;
	riff.seek(3);
	if (!INTEL)
		riff.write((byte)'X');
	riff.seek(4);
	riff.writeInt(littleEndian(len,4));  //data+chunksize
	riff.seek(16);
	riff.writeInt(littleEndian(0x10,4));  //chunk length
	riff.writeShort(littleEndian(1,2));   //pcm
	riff.writeShort((short)littleEndian(lChannel,2)); //channels
	riff.writeInt(littleEndian(lSampling_frequency,4));  //sample_freq
	riff.writeInt(littleEndian(lBitrate / 8,4)); //byterate
	riff.writeShort((short)littleEndian(lMode,2)); //blockalign
	riff.writeShort((short)littleEndian(lSize,2)); //bits_per_sample
	riff.seek(40);
	riff.writeInt(littleEndian(len-36,4));  //data-size //DM13092003 fix

	riff.close();
}
//DM24012004 081.6 int11 add-


//DM3003004 081.6 int18 add+
int lpcm_frequency_index[] = { 
	48000, 96000
};

int lpcm_bps_index[] = { 
	16, 20, 24, -1
};

public int LPCM_parseHeader(byte[] frame_header, int pos)
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

public String LPCM_displayHeader()
{ 
	return ("LPCM, DR-" + lEmphasis + ", " + lChannel + "-ch, " + lSampling_frequency + "Hz, " + lSize + "bit, " + (lBitrate / 1000.0) + "kbps");
} 
 
public String LPCM_saveAnddisplayHeader()
{ 
	saveHeader(); 
	return LPCM_displayHeader(); 
}

public int LPCM_compareHeader()
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
//DM3003004 081.6 int18 add-


}