/*
 * @(#)SCAN.java - pre-scanning to check supported files
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


import java.io.*;
import java.util.*;

public class Scan {

String[] type = { 
	"Filetype not supported",
	"PVA (Video/Audio PES)",
	"MPEG-1 PS/SS (Video/Audio PES)",
	"MPEG-2 PS/SS (Video/Audio PES)",
	"PES (Video/Audio/TTX)",
	"PES (MPEG Audio)",
	"PES (private stream 1)",
	"ES (AC-3 Audio)",
	"ES (MPEG Audio)",
	"ES (MPEG Video)",
	"ES (AC-3 Audio) (psb. SMPTE)",
	"DVB/MPEG2 TS",
	"ES (DTS Audio)", //DM19122003 081.6 int07
	"ES (DTS Audio) (psb. SMPTE)",
	"ES (RIFF Audio)", //DM30122003 081.6 int10
	"ES (compressed RIFF Audio)",
	"ES (Subpicture 2-bit RLE)" //DM31012004 081.6 int13
};

String video =" ", audio=" ", addInfo="", origFile="", playtime="", text="", pics=""; //DM10032004 081.6 int18 add, //DM28042004 081.7 int02 changed
ArrayList pidlist = new ArrayList();
boolean hasVideo=false, nullpacket=false;
byte[] vbasic = new byte[12];
Audio Audio = new Audio();
Video vfc = new Video();
int buffersize=1024000; //DM04122003 081.6_int02
int filetype = 0; //DM26032004 081.6_int18 add

java.text.DateFormat timeformat = new java.text.SimpleDateFormat("HH:mm:ss.SSS");

//DM18062004 081.7 int05 add
RawInterface raw_interface;

//DM18062004 081.7 int05 add
public Scan()
{
	raw_interface = new RawInterface();
}

//DM26032004 081.6_int18 changed
public int inputInt(String file) { 
	filetype = testFile(file,false);
	return filetype;
}

//DM26032004 081.6_int18 changed
public String Type(String file) { 
	origFile = file; 
	filetype = testFile(file,true);
	return type[filetype] + addInfo; 
}

//DM26032004 081.6_int18 add
public boolean isSupported() { 
	return (filetype == 0 ? false : true);
}

//DM18062004 081.7 int05 changed
public String Date(String file)
{
	return raw_interface.getFileDate(file);
}
 
//DM10032004 081.6 int18 changed
public String getVideo() { 
	return video; 
}

//DM10032004 081.6 int18 changed
public String getAudio() { 
	return audio; 
}

//DM10032004 081.6 int18 add
public String getText()
{
	return text; 
}

//DM28042004 081.7 int02 add
public String getPics()
{
	return pics; 
}

//DM10032004 081.6 int18 changed
public String getPlaytime() { 
	return playtime; 
}

//DM18062004 081.7 int05 changed
public boolean isEditable()
{
	if (raw_interface.isAccessibleDisk(origFile))
		return hasVideo;
	else
		return (new File(origFile).exists() && hasVideo);
}

public String getFile() { 
	return origFile; 
}

public byte[] getVBasic() { 
	return vbasic; 
}

public int AC3Audio(byte[] check) {  //DM19122003 081.6 int07 changed
	audiocheck:
	for (int a=0; a<10000; a++) {
		if (Audio.AC3_parseHeader(check,a) < 0)
			continue audiocheck;
		for (int b=0;b<17;b++) {
			if (Audio.AC3_parseNextHeader(check,a+Audio.Size+b) == 1){
				if ( (0xFF&check[a+Audio.Size]) > 0x3f || (0xFF&check[a+Audio.Size])==0 ) //smpte
					continue audiocheck;
				audio = Audio.AC3_saveAnddisplayHeader();
				return 1;
			}
		}
	} 
	return 0;
}

/* DTS stuff taken from the VideoLAN project. */ 
/* Added by R One, 2003/12/18. */
public void DTSAudio(byte[] check) { 
	audiocheck: 
	for (int a=0; a<10000; a++) { 
		if (Audio.DTS_parseHeader(check,a) < 0) 
			continue audiocheck; 
		for (int b=0;b<15;b++) { 
			if (Audio.DTS_parseNextHeader(check,a+Audio.Size+b) == 1){ 
				if ( (0xFF&check[a+Audio.Size]) > 0x7f || (0xFF&check[a+Audio.Size])==0 ) //smpte 
					continue audiocheck; 
				audio = Audio.DTS_saveAnddisplayHeader(); 
				return; 
			} 
		} 
	} 
} 

public void MPEGAudio(byte[] check) { 
	audiocheck:
	for (int a=0; a<10000; a++) {
		if (Audio.MPA_parseHeader(check,a) < 0)
			continue audiocheck;
		if (Audio.MPA_parseNextHeader(check,a+Audio.Size) < 0)
			continue audiocheck;
		audio = Audio.MPA_saveAnddisplayHeader();
		return;
	}
}

public byte[] loadPES(byte[] check, int a) { 
	ByteArrayOutputStream bytecheck = new ByteArrayOutputStream();
	while (bytecheck.size() < 11000) {
		int jump = (255&check[a+4])<<8 | (255&check[a+5]);
		bytecheck.write(check,a+6+3+(255&check[a+8]),jump-3-(255&check[a+8]) );
		a += 6+jump;
	}
	return bytecheck.toByteArray();
}

public void loadMPG2(byte[] check, int b, boolean vdr, boolean mpg1) { 
	ByteArrayOutputStream vid = new ByteArrayOutputStream();
	ByteArrayOutputStream aud = new ByteArrayOutputStream();
	ByteArrayOutputStream ac3 = new ByteArrayOutputStream();
	int jump = 1;

	mpg2check:
	for (int a=b; a<500000; a+=jump) {
		if ( check[a]!=0 || check[a+1]!=0 || check[a+2]!=1 ) { 
			jump=1; 
			continue mpg2check; 
		}
		if ( (0xf0 & check[a+3]) == 0xe0) {
			jump = 6+((255&check[a+4])<<8 | (255&check[a+5]));
			if (nullpacket) 
				jump = 2048;
			vid.write(check,a+6+((!mpg1) ? 3+(255&check[a+8]) : 0),jump-((!mpg1) ? 6-3-(255&check[a+8]) : 0) );
		} else if ( (0xf0 & check[a+3]) == 0xc0) {
			jump = 6+((255&check[a+4])<<8 | (255&check[a+5]));
			aud.write(check,a+6+3+(255&check[a+8]),jump-6-3-(255&check[a+8]) );
		} else if ( (0xff & check[a+3]) == 0xbd) {
			jump = 6+((255&check[a+4])<<8 | (255&check[a+5]));

			//DM10032004 081.6 int18 add
			if (check[a+8]==0x24 && (0xF0&check[a+9+0x24])>>>4 == 1)
			{
				text = "SubID 0x"+Integer.toHexString((0xFF & check[a+9+0x24])).toUpperCase();
			}

			else if (!vdr) 
				ac3.write(check,a+6+7+(255&check[a+8]),jump-6-7-(255&check[a+8]) );
			else 
				ac3.write(check,a+6+3+(255&check[a+8]),jump-6-3-(255&check[a+8]) );
		} else {
			switch (0xFF & check[a+3]) {
			case 0xBB:
			case 0xBC:
			case 0xBE:
			case 0xBF:
			case 0xF0:
			case 0xF1:
			case 0xF2:
			case 0xF3:
			case 0xF4:
			case 0xF5:
			case 0xF6:
			case 0xF7:
			case 0xF8:
			case 0xF9:
			case 0xFA:
			case 0xFB:
			case 0xFC:
			case 0xFD:
			case 0xFE:
			case 0xFF: { 
				jump = 6+((255&check[a+4])<<8 | (255&check[a+5])); 
				break; 
			}
			default: 
				jump=1;
			}
		}
	}

	try 
	{
	if ( vid.size()>0 ) 
		checkVid(vid.toByteArray());
	else 
		video="no video found at a short scan";
	} 
	catch  ( Exception e) { 
		video = "error while checking"; 
	}

	try 
	{
	if ( ac3.size()>0 ) {
		byte buffer[]=ac3.toByteArray(); //DM19122003 081.6 int07 changed
		if (AC3Audio(buffer)<1)
			DTSAudio(buffer);
	} else if ( aud.size()>0 ) 
		MPEGAudio(aud.toByteArray());
	else 
		audio="no audio found at a short scan";
	} 
	catch  ( Exception e) { 
		audio = "error while checking"; 
	}

	return;
}

public void loadPVA(byte[] check, int a) { 
	ByteArrayOutputStream vid = new ByteArrayOutputStream();
	ByteArrayOutputStream aud = new ByteArrayOutputStream();
	int number = 0x10000;

	while ( a < 550000) {
		int jump = (255&check[a+6])<<8 | (255&check[a+7]);
		if (a+8+((1 & check[a+5]>>>4)*4)+jump > 700000) 
			break;
		switch (255 & check[a+2]) {
		case 1: { 
			vid.write(check,a+8+((1 & check[a+5]>>>4)*4),jump ); 
			break; 
		}
		case 0x80: 
		case 2: { 
			aud.write(check,a+8,jump ); 
			break; 
		}
		}
		a += 8+jump;
	}

	//addInfo = " ( "+Integer.toBinaryString(number).substring(4,17)+" )";

	try 
	{
	if (vid.size()>0) 
		checkVid(vid.toByteArray());
	else 
		video="no video found at a short scan";
	}
	catch  ( Exception e) { 
		video = "error while checking"; 
	}

	try 
	{
	if (aud.size()>0) 
		checkPES(aud.toByteArray());
	else 
		audio="no audio found at a short scan";
	} catch  ( Exception e) { 
		audio = "error while checking"; 
	}

	return;
}

public void checkPES(byte[] check) { 
	rawcheck:
	for (int a=0; a<8000; a++) {
		if ( check[a]!=0 || check[a+1]!=0 || check[a+2]!=1 ) 
			continue rawcheck;
		if ( (0xf0 & check[a+3])==0xc0 || check[a+3]==(byte)0xbd ) {
			int next = a+6+( (255&check[a+4])<<8 | (255&check[a+5]) );
			if ( check[next]!=0 || check[next+1]!=0 || check[next+2]!=1 ) 
				continue rawcheck;
			if ( (0xf0 & check[a+3])==0xc0 && (0xf0 & check[a+3])==(0xf0 & check[next+3]) ) {
				MPEGAudio(loadPES(check,a));
				return;
			} else if ( check[a+3]==(byte)0xbd && check[a+3]==check[next+3] ) {
				byte buffer[]=loadPES(check,a); //DM19122003 081.6 int07 changed
				if (AC3Audio(buffer)<1)
					DTSAudio(buffer);
				return;
			}
		}
	}
}

public void checkVid(byte[] check) { 
	ByteArrayOutputStream bytecheck = new ByteArrayOutputStream();
	video = "no sequence header in first 600kB";

	mpvcheck:
	for (int a=0; a<check.length-630; a++) {
		if ( check[a]!=0 || check[a+1]!=0 || check[a+2]!=1 || check[a+3]!=(byte)0xb3 ) 
			continue mpvcheck;
		for (int b=7;b<600;b++) {
			if ( check[a+b]==0 && check[a+b+1]==0 && check[a+b+2]==1 && check[a+b+3]==(byte)0xb8 ) {
				hasVideo=true;
				System.arraycopy(check,a,vbasic,0,12);
				bytecheck.write(check,a,20);
				video = vfc.videoformatByte(bytecheck.toByteArray());
				return;
			}
		} 
	} 
	return;
}

public void PMTcheck(byte[] check, int a) { 
	ByteArrayOutputStream bytecheck = new ByteArrayOutputStream();
	video = "no PMT found";
	audio = "no PMT found";
	text = "no PMT found"; //DM10032004 081.6 int18 add
	pics = "no PMT found"; //DM28042004 081.7 int02 add
	pidlist.clear();

	tscheck:
	for ( ; a<check.length-1000; a++) {
		if ( check[a]!=0x47 || check[a+188]!=0x47 || check[a+376]!=0x47 ) 
			continue tscheck;
		if ( (0x30&check[a+3])!=0x10 || check[a+4]!=0 || check[a+5]!=2 || (0xf0&check[a+6])!=0xb0 ) { 
			a+=187; 
			continue tscheck; 
		}
		bytecheck.write(check,a+4,184);
		int pmtpid = (0x1F & check[a+1])<<8 | (0xFF & check[a+2]);
		if ( bytecheck.size()<188 ) { 
			a+=188;
			addpack:
			for ( ; a<check.length-500; a++) {
				if ( check[a]!=0x47 || check[a+188]!=0x47 || check[a+376]!=0x47 ) 
					continue addpack;
				if ( ((0x1F & check[a+1])<<8 | (0xFF & check[a+2]))!=pmtpid || (0x40&check[a+1])!=0 || (0x30&check[a+3])!=0x10 ) { 
					a+=187; 
					continue addpack; 
				}
				bytecheck.write(check,a+4,184);
				if ( bytecheck.size()>188 ) 
					break addpack;
			}
		}
		byte[] pmt = bytecheck.toByteArray();
		if (pmt.length>5) {
			int sid = (0xFF & pmt[4])<<8 | (0xFF & pmt[5]);
			pidlist.add(""+sid);
			pidlist.add(""+pmtpid);
			addInfo+=" (SID 0x"+Integer.toHexString(sid).toUpperCase()+" ,PMT 0x"+Integer.toHexString(pmtpid).toUpperCase()+")";
		}
		video="PIDs:";
		audio="PIDs:";
		text="PIDs:"; //DM10032004 081.6 int18 add
		pics="PIDs:"; //DM28042004 081.7 int02 add

		int pmt_len = (0xF&pmt[2])<<8 | (0xFF&pmt[3]);  //DM30122003 081.6 int10 add

		//DM30122003 081.6 int10 changed
		//DM10032004 081.6 int18 changed  , 0xF & pmt[a+3]<<4 | 0xFF & pmt[a+4]
		pidsearch:
		for (int b=8, r=8; b < pmt_len-4 && b < pmt.length-6; b++)
		{
			r = b;
			if ( (0xe0 & pmt[b+1]) != 0xe0 ) 
				continue pidsearch;

			int pid = (0x1F & pmt[b+1])<<8 | (0xFF & pmt[b+2]);

			switch(0xFF & pmt[b])
			{
			case 1 : //DM10032004 081.6 int18 add
			case 2 :
				getIsoLanguage(pmt, b+5, (b += 4+ (0xFF & pmt[b+4])), pid, 2);
				pidlist.add(""+pid); 
				break; 

			case 3 :
			case 4 :
				getIsoLanguage(pmt, b+5, (b += 4+ (0xFF & pmt[b+4])), pid, 4);
				pidlist.add("" + pid); 
				break; 

			case 0x80:
			case 0x81:  //private data of AC3 in ATSC
			case 0x82: 
			case 0x83: 
			case 6 :
				getIsoLanguage(pmt, b+5, (b += 4+ (0xFF & pmt[b+4])), pid, 6);
				pidlist.add("" + pid); 
				break; 

			default: 
				b += 4+ (0xFF & pmt[b+4]);
			}
			if (b < 0) 
				b = r;
		}
		return;
	} 
	return;
}

//DM10032004 081.6 int18 new
//DM04052004 081.7 int02 fix
private void getIsoLanguage(byte check[], int off, int end, int pid, int type)
{
	String str = "";
	int chunk_end = 0;

	try
	{

	loop:
	for (; off < end && off < check.length; off++)
	{
		switch(0xFF & check[off])
		{
		//DM28042004 081.7 int02 add
		case 0x59:  //dvb subtitle descriptor
			type = 0x59;
			chunk_end = off + 2 + (0xFF & check[off+1]);
			str += "(";
			for (int a=off+2; a<chunk_end; a+=8)
			{
				for (int b=a; b<a+3; b++) //language
					str += (char)(0xFF & check[b]);

				int page_type = 0xFF & check[a+3];
				int comp_page_id = (0xFF & check[a+4])<<16 | (0xFF & check[a+5]);
				int anci_page_id = (0xFF & check[a+6])<<16 | (0xFF & check[a+7]);
				str += "_0x" + Integer.toHexString(page_type).toUpperCase();
				str += "_p" + comp_page_id;
				str += "_a" + anci_page_id + " ";
			}
			str += ")";
			break loop;

		case 0x56:  //teletext descriptor incl. index page + subtitle pages
			type = 0x56;
			chunk_end = off + 2 + (0xFF & check[off+1]);
			str += "(";
			for (int a=off+2; a<chunk_end; a+=5)
			{
				for (int b=a; b<a+3; b++) //language
					str += (char)(0xFF & check[b]);
				int page_type = (0x18 & check[a+3])>>>3;
				int page_number = 0xFF & check[a+4];
				str += "_" + (page_type == 2 ? "s" : "i");
				str += Integer.toHexString((7 & check[a+3]) == 0 ? 8 : (7 & check[a+3])).toUpperCase();
				str += (page_number < 0x10 ? "0" : "") + Integer.toHexString(page_number).toUpperCase() + " ";
			}
			str += ")";
			break loop;

		case 0xA:  //ISO 639 language descriptor
			str += "(";
			for (int a=off+2; a<off+5; a++)
				str += (char)(0xFF & check[a]);
			str += ")";
			off++;
			off += (0xFF & check[off]);
			break;

		case 0x6A:  //ac3 descriptor
			str += "(AC-3)";
			off++;
			off += (0xFF & check[off]);
			break;

		case 0x5:  //registration descriptor
			chunk_end = off + 2 + (0xFF & check[off+1]);
			str += "(";
			for (int a=off+2; a<chunk_end; a++)
				str += (char)(0xFF & check[a]);
			str += ")";
	
		default:
			off++;
			off += (0xFF & check[off]);
		}
	}

	String out = " 0x" + Integer.toHexString(pid).toUpperCase();

	switch (type)
	{
	case 0x59:  //DM28042004 081.7 int02 add
		pics += out + str;
		break;

	case 0x56:
		text += out + str;
		break;

	case 2:
		video += out + str;
		break;

	case 4:
		audio += out + str;
		break;

	default:
		audio += out + str + "_PD";
	}

	}
	catch (ArrayIndexOutOfBoundsException ae)
	{
		playtime += "PMT parsing error (language) ";
	}
}

public int[] getPIDs() {
	int[] b = new int[pidlist.size()];
	for (int a=0; a<pidlist.size();a++) 
		b[a] = Integer.parseInt(pidlist.get(a).toString());
	return b;
}

public String getAudioTime(long len) {
	return timeformat.format(new java.util.Date((len*8000)/Audio.Bitrate));
}

public void setBuffer(int buffersize) {
	this.buffersize=buffersize;
}

public int testFile(String infile, boolean more) { //DM04122003 081.6_int02 changed
	video = "no video found at a short scan";
	audio = "no audio found at a short scan"; 
	text = "no teletext found at a short scan"; 
	pics = "no subpicture found at a short scan";  //DM28042004 081.7 int02 add
	addInfo="";
	playtime="";
	hasVideo=false; 
	nullpacket=false;
	long size = 0;
	timeformat.setTimeZone(java.util.TimeZone.getTimeZone("GMT+0:00"));

	int bs0=buffersize/100;
	int bs1=buffersize/50;
	int bs2=buffersize/10;
	int bs3=buffersize/4;
	int bs4=buffersize-65536; //DM10122003 081.6_int04 fix, //DM23022004 081.6_int18 fix

	byte[] check= new byte[buffersize];
	ByteArrayOutputStream bytecheck = new ByteArrayOutputStream();
   
	try 
	{

	//DM18062004 081.7 int05 changed
	if (!raw_interface.getScanData(infile, check))
	{
		RandomAccessFile incheck = new RandomAccessFile( infile, "r" );
		size = incheck.length();
		incheck.seek(0);
		incheck.read(check);
		incheck.close();
	}

	riffcheck:
	for (int a=0; a<bs0; a++)  //DM30122003 081.6 int10 add, compressed as AC3,MPEG is currently better detected as ES-not RIFF
	{
		int ERRORCODE = Audio.WAV_parseHeader(check, a);
		if (ERRORCODE > -1)    //DM26032004 081.6 int18 changed
		{
			Audio.saveHeader();
			if (more)
			{ 
				audio = Audio.WAV_displayHeader(); 
				playtime = getAudioTime(size); 
			} 

			if (ERRORCODE > 0)
				return 14;

			else if (Audio.lMode_extension > 1)
				break riffcheck;

			else
				return 15;
		}
	}

	supcheck:
	for (int a=0; a<bs0; a++) //DM31012004 081.6 int13 add
	{
		if (check[a]!=0x53 || check[a+1]!=0x50)
			continue supcheck;

		int supframe_size = (0xFF & check[a+10])<<8 | (0xFF & check[a+11]);
		int supframe_link = (0xFF & check[a+12])<<8 | (0xFF & check[a+13]);
		int supframe_check = (0xFF & check[a+12+supframe_link])<<8 | (0xFF & check[a+13+supframe_link]);
		int supframe_check2 = (0xFF & check[a+36+supframe_link])<<8 | (0xFF & check[a+37+supframe_link]);

		if (supframe_link == supframe_check-24 && supframe_check == supframe_check2)
		{
			if (more)
			{
				int b=a+14+supframe_link, c=b+24, d, xa, xe, ya, ye;
				for (d=b; d<c; d++)
				{
					switch(0xFF & check[d])
					{
					case 1:
						d++;
						continue;
					case 2:
						d+=24;
						continue;
					case 3:
					case 4:
						d+=2;
						continue;
					case 6:
						d+=4;
						continue;
					case 5:
						xa= (0xFF & check[++d])<<4 | (0xF0 & check[++d])>>>4;
						xe= (0xF & check[d])<<8 | (0xFF & check[++d]);
						ya= (0xFF & check[++d])<<4 | (0xF0 & check[++d])>>>4;
						ye= (0xF & check[d])<<8 | (0xFF & check[++d]);
						pics = "up.left x" + xa + ",y" + ya + " @ size " + (xe - xa + 1) + "*" + (ye - ya + 1); //DM05052004 081.7 int02 changed,fix
					}
					break;
				}

				//DM06032004 081.6 int18 add
				byte packet[] = new byte[10+supframe_size];
				System.arraycopy(check,a,packet,0,10+supframe_size);
				X.subpicture.picture.decode_picture(packet,10,X.subpicture.isVisible());
			}
			return 16;
		}
	}

	mpegtscheck:
	for (int a=0; a<bs4; a++) { //DM17012004 081.6 int11 changed
		if ( check[a]!=0x47 || check[a+188]!=0x47 || check[a+376]!=0x47 || check[a+564]!=0x47 || check[a+752]!=0x47) 
			continue mpegtscheck;
		PMTcheck(check,a);
		return 11;
	}

	pvacheck:
	for (int a=0; a<bs1; a++) {
		if ( check[a]!=(byte)0x41 || check[a+1]!=(byte)0x56 || check[a+4]!=(byte)0x55 ) 
			continue pvacheck;
		int next = a+8+( (255&check[a+6])<<8 | (255&check[a+7]) );
		if ( check[next]!=(byte)0x41 || check[next+1]!=(byte)0x56 || check[next+4]!=(byte)0x55 ) 
			continue pvacheck;
		else {
			if (more) 
				loadPVA(check,a);
			return 1;
		}
	}

	mpgcheck:
	for (int a=0; a<bs2; a++) {
		if ( check[a]!=0 || check[a+1]!=0 || check[a+2]!=1 || check[a+3]!=(byte)0xba ) 
			continue mpgcheck;
		if ( (0xC0 & check[a+4])==0 ) { 
			if (more) 
				loadMPG2(check,a,false,true); 
			return 2;
		} else if ( (0xC0 & check[a+4])==0x40 ) {
			if (more) 
				loadMPG2(check,a,false,false); 
			return 3;
		}
	}

	vdrcheck:
	for (int a=0; a<bs4; a++) {
		if ( check[a]!=0 || check[a+1]!=0 || check[a+2]!=1 || (0xF0&check[a+3])!=0xe0 ) 
			continue vdrcheck;
		int next = a+6+( (255&check[a+4])<<8 | (255&check[a+5]) );
		if (next==a+6 && (0xC0&check[a+6])==0x80 && (0xC0&check[a+8])==0) { 
			addInfo += " !!(Vpacketsize=0)"; 
			next=a; 
			nullpacket=true; 
		}
		if ( check[next]!=0 || check[next+1]!=0 || check[next+2]!=1 ) 
			continue vdrcheck;
		else {
			if (more) 
				loadMPG2(check,a,true,false); 
			return 4;
		}
	}

	rawcheck:
	for (int a=0; a<bs2; a++) {
		if ( check[a]!=0 || check[a+1]!=0 || check[a+2]!=1 ) 
			continue rawcheck;
		if ( (0xf0 & check[a+3])==0xc0 || (255 & check[a+3])==0xbd ) {
			int next = a+6+( (255&check[a+4])<<8 | (255&check[a+5]) );
			if ( check[next]!=0 || check[next+1]!=0 || check[next+2]!=1 ) 
				continue rawcheck;
			if ( (0xE0 & check[a+3])==0xC0 && (0xE0 & check[a+3])==(0xE0 & check[next+3]) ) {
				if (more) 
					MPEGAudio(loadPES(check,a));
				return 5;
			} else if ( (255&check[a+3])==0xbd && check[a+3]==check[next+3] ) {
				if (more) {
					//DM10032004 081.6 int18 changed
					if (check[a+8]==0x24 && (0xF0&check[a+9+0x24])>>>4 == 1)
					{
						addInfo=" (TTX)";
						text = "SubID 0x"+Integer.toHexString((0xFF & check[a+9+0x24])).toUpperCase();
					}
					else
					{
						byte buffer[]=loadPES(check,a); //DM19122003 081.6 int07 changed
						if (AC3Audio(buffer)<1)
							DTSAudio(buffer);
					}
				}
				return 6;
			}
		}
	}

	audiocheck:
	for (int a=0; a<bs0; a++) {
		/* DTS stuff taken from the VideoLAN project. */ 
		/* Added by R One, 2003/12/18. */ 
		//DM20122003 081.6 int07 add
		if (Audio.DTS_parseHeader(check,a) > 0) { 
			for (int b=0;b<15;b++) { 
				if (Audio.DTS_parseNextHeader(check,a+Audio.Size+b) == 1){ 
					if ( (0xFF&check[a+Audio.Size]) > 0x7f || (0xFF&check[a+Audio.Size])==0 ) //smpte 
						continue audiocheck; 
					if (more){ 
						audio = Audio.DTS_saveAnddisplayHeader(); 
						playtime = getAudioTime(size); 
					} 
					if (b==0) 
						return 12; 
					else 
						return 13;
				} 
			} 
			if (X.RButton[7].isSelected()){ //DM30122003 081.6 int10 new
				if (more) 
					audio = Audio.DTS_saveAnddisplayHeader();
				playtime = getAudioTime(size); 
				return 12;
			}
		} else if (Audio.AC3_parseHeader(check,a) > 0) { 
			for (int b=0;b<17;b++) {
				if (Audio.AC3_parseNextHeader(check,a+Audio.Size+b) == 1){
					if ( (0xFF&check[a+Audio.Size]) > 0x3f || (0xFF&check[a+Audio.Size])==0 ) //smpte
						continue audiocheck;
					if (more){
						audio = Audio.AC3_saveAnddisplayHeader();
						playtime = getAudioTime(size);
					}
					if (b==0)
						return 7;
					else 
						return 10;
				}
			}
			if (X.RButton[7].isSelected()){ //DM30122003 081.6 int10 new
				if (more) 
					audio = Audio.AC3_saveAnddisplayHeader();
				playtime = getAudioTime(size); 
				return 7;
			}
		} else if (Audio.MPA_parseHeader(check,a) > 0) {
			if (!X.RButton[7].isSelected() && Audio.MPA_parseNextHeader(check,a+Audio.Size) < 0)  //DM30122003 081.6 int10 changed
				continue audiocheck;
			if (more){
				audio = Audio.MPA_saveAnddisplayHeader();
				playtime = getAudioTime(size);
			}
			return 8;
		}
	} 

	mpvcheck:
	for (int a=0; a<bs3; a++) {
		if ( check[a]!=0 || check[a+1]!=0 || check[a+2]!=1 ) 
			continue mpvcheck;
		if ( check[a+3]==(byte)0xb3 ) {
			for (int b=7;b<600;b++)
				if ( check[a+b]==0 && check[a+b+1]==0 && check[a+b+2]==1 && check[a+b+3]==(byte)0xb8 ) {
					if (more) { 
						hasVideo=true;
						System.arraycopy(check,a,vbasic,0,12);
						bytecheck.write(check,a,20);
						video = vfc.videoformatByte(bytecheck.toByteArray());
					}
					return 9;
				}
		} else if ( check[a+3]==(byte)0xb8 ) {
			for (int b=6;b<20;b++) 
				if ( check[a+b]==0 && check[a+b+1]==0 && check[a+b+2]==1 && check[a+b+3]==0 ) { 
					if (more) 
						video = "file doesn't start with a sequence header!";
					return 9;
				}
		} 
	} 


	} 
	catch ( IOException e ) { }

	check=null; //DM22122003 081.6 int09 new
	System.gc();

	return 0;
}


} /** end class **/
