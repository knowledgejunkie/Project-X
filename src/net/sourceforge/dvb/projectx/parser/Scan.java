/*
 * @(#)SCAN.java - pre-scanning to check supported files
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

package net.sourceforge.dvb.projectx.parser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;

import net.sourceforge.dvb.projectx.audio.Audio;
import net.sourceforge.dvb.projectx.common.Resource;
import net.sourceforge.dvb.projectx.common.X;
import net.sourceforge.dvb.projectx.common.Common;
import net.sourceforge.dvb.projectx.video.Video;
import net.sourceforge.dvb.projectx.xinput.XInputFile;

public class Scan
{
	//DM14092004 081.8.02 add
	private final String msg_1 = Resource.getString("scan.msg1");
	private final String msg_2 = Resource.getString("scan.msg2");
	private final String msg_3 = Resource.getString("scan.msg3");
	private final String msg_4 = Resource.getString("scan.msg4");
	private final String msg_5 = Resource.getString("scan.msg5");
	private final String msg_6 = Resource.getString("scan.msg6");
	private final String msg_7 = Resource.getString("scan.msg7");
	private final String msg_8 = Resource.getString("scan.msg8");
	private final String msg_9 = Resource.getString("scan.msg9");

	private final String[] type = { 
		Resource.getString("scan.unsupported"),
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


	String video =" ", audio=" ", addInfo="", playtime="", text="", pics=""; //DM10032004 081.6 int18 add, //DM28042004 081.7 int02 changed
	XInputFile origFile;
	ArrayList pidlist = new ArrayList();
	boolean hasVideo=false, nullpacket=false;
	byte[] vbasic = new byte[12];
	int buffersize=1024000; //DM04122003 081.6_int02
	int filetype = 0; //DM26032004 081.6_int18 add

	java.text.DateFormat timeformat = new java.text.SimpleDateFormat("HH:mm:ss.SSS");

	ArrayList video_streams, audio_streams, ttx_streams, pic_streams;

	Audio Audio = new Audio();

	//DM18062004 081.7 int05 add
	public Scan()
	{
		video_streams = new ArrayList();
		audio_streams = new ArrayList();
		ttx_streams = new ArrayList();
		pic_streams = new ArrayList();
	}

	//DM26032004 081.6_int18 changed
	public int inputInt(XInputFile aXInputFile)
	{ 
		filetype = testFile(aXInputFile, false);

		return filetype;
	}

	//DM26032004 081.6_int18 changed
	public String Type(XInputFile aXInputFile)
	{ 
		origFile = aXInputFile; 
		filetype = testFile(aXInputFile, true);

		return type[filetype] + addInfo; 
	}

	//DM26032004 081.6_int18 add
	public boolean isSupported()
	{ 
		return (filetype == 0 ? false : true);
	}

	//DM18062004 081.7 int05 changed
	public String Date(XInputFile aXInputFile)
	{
		return DateFormat.getDateInstance(DateFormat.LONG).format(new Date(aXInputFile.lastModified()))
		       + "  "
					 + DateFormat.getTimeInstance(DateFormat.LONG).format(new Date(aXInputFile.lastModified()));
	}
 
	private String readStreams(ArrayList streams, String str)
	{
		str = streams.get(0).toString();

		for (int a = 1; a < streams.size(); a++)
			str += "\n\r\t" + streams.get(a).toString();

		return str;
	}

	//DM10032004 081.6 int18 changed
	public String getVideo()
	{ 
		if (video_streams.size() == 0)
			return video; 

		return readStreams(video_streams, video);
	}

	//DM10032004 081.6 int18 changed
	public String getAudio()
	{ 
		if (audio_streams.size() == 0)
			return audio; 

		return readStreams(audio_streams, audio);
	}

	//DM10032004 081.6 int18 add
	public String getText()
	{
		if (ttx_streams.size() == 0)
			return text; 

		return readStreams(ttx_streams, text);
	}

	//DM28042004 081.7 int02 add
	public String getPics()
	{
		if (pic_streams.size() == 0)
			return pics; 

		return readStreams(pic_streams, pics);
	}

	//DM10032004 081.6 int18 changed
	public String getPlaytime() 
	{ 
		return playtime; 
	}

	//DM18062004 081.7 int05 changed
	public boolean isEditable()
	{
		if (origFile == null)
			return false;

		return (origFile.exists() && hasVideo);
	}

	public XInputFile getFile()
	{ 
		return origFile; 
	}

	public byte[] getVBasic()
	{ 
		return vbasic; 
	}

	//DM19122003 081.6 int07 changed
	public int AC3Audio(byte[] check)
	{
		audiocheck:
		for (int a=0; a<10000; a++)
		{
			if (Audio.AC3_parseHeader(check,a) < 0)
				continue audiocheck;

			for (int b=0; b < 17; b++)
			{
				if (Audio.AC3_parseNextHeader(check,a+Audio.Size+b) == 1)
				{
					if ( (0xFF & check[a+Audio.Size]) > 0x3f || (0xFF & check[a+Audio.Size])==0 ) //smpte
						continue audiocheck;

					audio_streams.add(Audio.AC3_saveAnddisplayHeader());

					return 1;
				}
			}
		} 

		return 0;
	}

	/* DTS stuff taken from the VideoLAN project. */ 
	/* Added by R One, 2003/12/18. */
	public void DTSAudio(byte[] check)
	{ 
		audiocheck: 
		for (int a=0; a<10000; a++)
		{ 
			if (Audio.DTS_parseHeader(check,a) < 0) 
				continue audiocheck; 

			for (int b=0; b < 15; b++)
			{ 
				if (Audio.DTS_parseNextHeader(check,a+Audio.Size+b) == 1)
				{ 
					if ( (0xFF & check[a+Audio.Size]) > 0x7f || (0xFF & check[a+Audio.Size])==0 ) //smpte 
						continue audiocheck; 

					audio_streams.add(Audio.DTS_saveAnddisplayHeader()); 

					return; 
				} 
			} 
		} 
	} 

	public void MPEGAudio(byte[] check)
	{ 
		audiocheck:
		for (int a=0; a < 10000; a++)
		{
			if (Audio.MPA_parseHeader(check, a) < 0)
				continue audiocheck;

			if (Audio.MPA_parseNextHeader(check, a + Audio.Size) < 0)
				continue audiocheck;

			audio_streams.add(Audio.MPA_saveAnddisplayHeader());

			return;
		}
	}

	public byte[] loadPES(byte[] check, int a)
	{ 
		ByteArrayOutputStream bytecheck = new ByteArrayOutputStream();
		int jump, offs, len;
		boolean mpg1;

		for (; a < check.length; )
		{
			jump = (0xFF & check[a + 4])<<8 | (0xFF & check[a + 5]);
			mpg1 = (0x80 & check[a + 6]) == 0 ? true : false;
			offs = a + 6 + ( !mpg1 ? 3 + (0xFF & check[a+8]) : 0);
			len = jump - ( !mpg1 ? 3 + (0xFF & check[a+8]) : 0);

			if (offs + len > check.length)
				break;

			bytecheck.write(check, offs, len);

			a += 6 + jump;
		}

		return bytecheck.toByteArray();
	}

	public void loadMPG2(byte[] check, int b, boolean vdr, boolean mpg1, int size) throws IOException
	{ 
		video = msg_1;
		audio = msg_2;

		Hashtable table = new Hashtable();
		ScanObject scanobject;

		String str;
		int jump = 1, id;

		mpg2check:
		for (int a=b; a < 500000; a += jump)
		{
			if ( check[a] != 0 || check[a+1] != 0 || check[a+2] != 1 )
			{ 
				jump = 1; 
				continue mpg2check; 
			}

			id = 0xFF & check[a+3];
			str = String.valueOf(id);

			if ( id == 0xBA )
			{
				jump = (0xC0 & check[a+4]) == 0 ? 12 : 14; 
			}

			//video mpg  e0..0f
			else if ( (0xF0 & id) == 0xE0 )
			{
				jump = nullpacket ? 2048 : (6 + ((0xFF & check[a+4])<<8 | (0xFF & check[a+5])) );

				if (!table.containsKey(str))
					table.put(str, new ScanObject(id));

				scanobject = (ScanObject)table.get(str);

				scanobject.write(check, a + 6 + ((!mpg1) ? 3 + (0xFF & check[a+8]) : 0), jump - ((!mpg1) ? 6 - 3 - (0xFF & check[a+8]) : 0) ); 
			}

			//audio mpg  c0..df
			else if ( (0xE0 & id) == 0xC0 )
			{
				jump = 6 + ((0xFF & check[a+4])<<8 | (0xFF & check[a+5]));

				if (!table.containsKey(str))
					table.put(str, new ScanObject(id));

				scanobject = (ScanObject)table.get(str);

				scanobject.write(check, a, jump ); 
			}

			//private bd
			else if ( id == 0xBD )
			{
				jump = 6 + ((0xFF & check[a+4])<<8 | (0xFF & check[a+5]));
				int pes_headerlength = 0xFF & check[a + 8];
				boolean pes_alignment = (4 & check[a + 6]) != 0 ? true : false;

				//DM10032004 081.6 int18 add
				if (pes_headerlength == 0x24 && (0xF0 & check[a + 9 + pes_headerlength])>>>4 == 1)
				{
					text = "SubID 0x" + Integer.toHexString((0xFF & check[a + 9 + pes_headerlength])).toUpperCase();
				}

				else if ( ((!mpg1 && !vdr) || (vdr && pes_alignment)) && ((0xF0 & check[a + 9 + pes_headerlength])>>>4 == 2 || (0xF0 & check[a + 9 + pes_headerlength])>>>4 == 3))
				{
					pics = "SubID 0x" + Integer.toHexString((0xFF & check[a + 9 + pes_headerlength])).toUpperCase();
				}

				else
				{
					if (!vdr)
					{
						id = 0xFF & check[a + 9 + pes_headerlength];
						str = String.valueOf(id);

						check[a + 8] = (byte)(4 + pes_headerlength);
					}

					if (!table.containsKey(str))
						table.put(str, new ScanObject(id));

					scanobject = (ScanObject)table.get(str);

					scanobject.write(check, a, jump ); 
				}
			}

			else
			{
				switch (id)
				{
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
				case 0xFF:
					jump = 6 + ((0xFF & check[a+4])<<8 | (0xFF & check[a+5])); 
					break; 

				default: 
					jump=1;
				}
			}
		}


		for (Enumeration n = table.keys(); n.hasMoreElements() ; )
		{
			str = n.nextElement().toString();
			id = Integer.parseInt(str);

			scanobject = (ScanObject)table.get(str);

			if ( (0xF0 & id) == 0xE0)
			{
				try 
				{
					checkVid(scanobject.getData());
				}
				catch  ( Exception e)
				{ 
					video_streams.add(msg_8); 
				}
			}
			else
			{
				try 
				{
					checkPES(scanobject.getData());
				}
				catch  ( Exception e)
				{ 
					audio_streams.add(msg_8); 
				}
			}
		}

		table.clear();

		return;
	}

	public void loadPVA(byte[] check, int a) throws IOException
	{ 
		Hashtable table = new Hashtable();
		ScanObject scanobject;

		String str;
		int jump, id;

		while ( a < 550000)
		{
			jump = (0xFF & check[a+6])<<8 | (0xFF & check[a+7]);

			if (a + 8 + ((1 & check[a+5]>>>4) * 4) + jump > 700000) 
				break;

			id = 0xFF & check[a+2];
			str = String.valueOf(id);

			if (!table.containsKey(str))
				table.put(str, new ScanObject(id));

			scanobject = (ScanObject)table.get(str);

			switch (id)
			{
			case 1:
				scanobject.write(check, a + 8 + ((1 & check[a+5]>>>4) * 4), jump ); 
				break; 

			default:
				scanobject.write(check, a + 8, jump ); 
			}

			a += 8 + jump;
		}

		video = msg_1;
		audio = msg_2;

		for (Enumeration n = table.keys(); n.hasMoreElements() ; )
		{
			str = n.nextElement().toString();

			scanobject = (ScanObject)table.get(str);

			if (str.equals("1"))
			{
				try 
				{
					checkVid(scanobject.getData());
				}
				catch  ( Exception e)
				{ 
					//video = msg_8; 
					video_streams.add(msg_8); 
				}
			}
			else
			{
				try 
				{
					checkPES(scanobject.getData());
				}
				catch  ( Exception e)
				{ 
					//audio = msg_8; 
					audio_streams.add(msg_8); 
				}
			}
		}

		table.clear();

		return;
	}

	public void checkPES(byte[] check)
	{ 
		checkPES(check, 0);
	}

	public void checkPES(byte[] check, int a)
	{ 
		int end = a + 8000;

		rawcheck:
		for (; a < end; a++)
		{
			if ( check[a] != 0 || check[a+1] != 0 || check[a+2] != 1 ) 
				continue rawcheck;

			if ( (0xE0 & check[a+3]) == 0xC0 || check[a+3] == (byte)0xBD )
			{
				int next = a + 6 + ( (0xFF & check[a+4])<<8 | (0xFF & check[a+5]) );

				if ( check[next] != 0 || check[next+1] != 0 || check[next+2] != 1 ) 
					continue rawcheck;

				if ( (0xE0 & check[a+3]) == 0xC0 && (0xE0 & check[a+3]) == (0xE0 & check[next+3]) )
				{
					MPEGAudio(loadPES(check,a));

					return;
				}

				else if ( check[a+3] == (byte)0xBD && check[a+3] == check[next+3] )
				{
					byte buffer[] = loadPES(check, a); //DM19122003 081.6 int07 changed

					if (AC3Audio(buffer) < 1)
						DTSAudio(buffer);

					return;
				}
			}
		}
	}

	public void checkVid(byte[] check)
	{ 
		ByteArrayOutputStream bytecheck = new ByteArrayOutputStream();
		video = msg_7;

		mpvcheck:
		for (int a=0; a<check.length-630; a++)
		{
			if ( check[a]!=0 || check[a+1]!=0 || check[a+2]!=1 || check[a+3]!=(byte)0xb3 ) 
				continue mpvcheck;

			for (int b=7; b < 600; b++)
			{
				if ( check[a+b]==0 && check[a+b+1]==0 && check[a+b+2]==1 && check[a+b+3]==(byte)0xb8 )
				{
					hasVideo=true;
					System.arraycopy(check,a,vbasic,0,12);
					bytecheck.write(check,a,20);

					video_streams.add(Video.videoformatByte(bytecheck.toByteArray()));

					return;
				}
			} 
		} 

		return;
	}

	public void PMTcheck(byte[] check, int a)
	{ 
		ByteArrayOutputStream bytecheck = new ByteArrayOutputStream();
		video = msg_5;
		audio = msg_5;
		text = msg_5; //DM10032004 081.6 int18 add
		pics = msg_5; //DM28042004 081.7 int02 add
		pidlist.clear();

		tscheck:
		for ( ; a < check.length - 1000; a++)
		{
			if ( check[a]!=0x47 || check[a+188]!=0x47 || check[a+376]!=0x47 ) 
				continue tscheck;

			if ( (0x30&check[a+3])!=0x10 || check[a+4]!=0 || check[a+5]!=2 || (0xf0&check[a+6])!=0xb0 )
			{ 
				a+=187; 
				continue tscheck; 
			}

			bytecheck.write(check,a+4,184);
			int pmtpid = (0x1F & check[a+1])<<8 | (0xFF & check[a+2]);

			if ( bytecheck.size() < 188 )
			{ 
				a += 188;

				addpack:
				for ( ; a<check.length-500; a++)
				{
					if ( check[a]!=0x47 || check[a+188]!=0x47 || check[a+376]!=0x47 ) 
						continue addpack;

					if ( ((0x1F & check[a+1])<<8 | (0xFF & check[a+2]))!=pmtpid || (0x40&check[a+1])!=0 || (0x30&check[a+3])!=0x10 )
					{ 
						a += 187; 
						continue addpack; 
					}

					bytecheck.write(check,a+4,184);

					if ( bytecheck.size() > 188 ) 
						break addpack;
				}
			}

			byte[] pmt = bytecheck.toByteArray();

			if (pmt.length > 5)
			{
				int sid = (0xFF & pmt[4])<<8 | (0xFF & pmt[5]);
				pidlist.add("" + sid);
				pidlist.add("" + pmtpid);
				addInfo += " (SID 0x" + Integer.toHexString(sid).toUpperCase() + " ,PMT 0x" + Integer.toHexString(pmtpid).toUpperCase() + ")";
			}

			video = "";
			audio = "";
			text = "";
			pics = "";

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
					getDescriptor(pmt, b+5, (b += 4+ (0xFF & pmt[b+4])), pid, 2);
					pidlist.add("" + pid); 
					break; 

				case 3 :
				case 4 :
					getDescriptor(pmt, b+5, (b += 4+ (0xFF & pmt[b+4])), pid, 4);
					pidlist.add("" + pid); 
					break; 

				case 0x80:
				case 0x81:  //private data of AC3 in ATSC
				case 0x82: 
				case 0x83: 
				case 6 :
					getDescriptor(pmt, b+5, (b += 4+ (0xFF & pmt[b+4])), pid, 6);
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
	private void getDescriptor(byte check[], int off, int end, int pid, int type)
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

						int page_type = (0xF8 & check[a+3])>>>3;
						int page_number = 0xFF & check[a+4];

						str += "_";

						switch (page_type)
						{
						case 1:
							str += "i";
							break;
						case 2:
							str += "s";
							break;
						case 3:
							str += "ai";
							break;
						case 4:
							str += "ps";
							break;
						case 5:
							str += "s.hip";
							break;
						default:
							str += "res";
						}

						str += Integer.toHexString((7 & check[a+3]) == 0 ? 8 : (7 & check[a+3])).toUpperCase();
						str += (page_number < 0x10 ? "0" : "") + Integer.toHexString(page_number).toUpperCase() + " ";
					}

					str += ")";
					//break loop;
					off++;
					off += (0xFF & check[off]);
					break;

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

				case 0xC3:  //VBI descriptor
					off++;

					switch (0xFF & check[off + 1])
					{
					case 4:
						str += "(VPS)";
						type = 0xC3;
						break;
					case 5:
						str += "(WSS)";
						break;
					case 6:
						str += "(CC)";
						break;
					case 1:
						str += "(EBU-TTX)";
						break;
					case 7:
						str += "(VBI)";
						break;
					}

					off += (0xFF & check[off]);
					break;

				case 0x52:  //ID of service
					chunk_end = off + 2 + (0xFF & check[off+1]);
					str += "(#" + (0xFF & check[off + 2]) + ")";
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

			String out = "PID: 0x" + Integer.toHexString(pid).toUpperCase();

			switch (type)
			{
			case 0x59:
				pic_streams.add(out + str);
				break;

			case 0x56:
				ttx_streams.add(out + str);
				break;

			case 2:
			case 0xC3:
				video_streams.add(out + str);
				break;

			case 4:
				audio_streams.add(out + str);
				break;

			default:
				audio_streams.add(out + str + "_PD");
			}

		}
		catch (ArrayIndexOutOfBoundsException ae)
		{
			playtime += msg_6;
		}
	}

	public int[] getPIDs()
	{
		int[] b = new int[pidlist.size()];

		for (int a=0; a<pidlist.size();a++) 
			b[a] = Integer.parseInt(pidlist.get(a).toString());

		return b;
	}

	public String getAudioTime(long len)
	{
		return timeformat.format(new java.util.Date((len*8000)/Audio.Bitrate));
	}

	public void setBuffer(int buffersize)
	{
		this.buffersize=buffersize;
	}

	public int testFile(XInputFile aXInputFile, boolean more)
	{
		long len = aXInputFile.length();

		int ret = testFile(aXInputFile, more, 0);

		if (ret != 0)
			return ret;

		// if type is not yet detected, try it again on a later position (10% of length)
		return testFile(aXInputFile, more, len / 10);
	}

	public int testFile(XInputFile aXInputFile, boolean more, long position)
	{
		video_streams.clear();
		audio_streams.clear();
		ttx_streams.clear();
		pic_streams.clear();

		video = msg_1;
		audio = msg_2; 
		text = msg_3; 
		pics = msg_4;  //DM28042004 081.7 int02 add
		addInfo = "";
		playtime = "";
		hasVideo = false; 
		nullpacket = false;
		long size = 0;
		timeformat.setTimeZone(java.util.TimeZone.getTimeZone("GMT+0:00"));

		int bs0 = buffersize / 100;
		int bs1 = buffersize / 50;
		int bs2 = buffersize / 10;
		int bs3 = buffersize / 4;
		int bs4 = buffersize - 65536; //DM10122003 081.6_int04 fix, //DM23022004 081.6_int18 fix

		byte[] check= new byte[buffersize];
		ByteArrayOutputStream bytecheck = new ByteArrayOutputStream();

		try 
		{
			size = aXInputFile.length();
		//	aXInputFile.randomAccessSingleRead(check, position);
			aXInputFile.getNewInstance().randomAccessSingleRead(check, position);

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
						return Common.ES_RIFF_TYPE;

					else if (Audio.lMode_extension > 1)
						break riffcheck;

					else
						return Common.ES_cRIFF_TYPE;
				}
			}

			supcheck:
			for (int a=0; a < bs0; a++) //DM31012004 081.6 int13 add
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

					return Common.ES_SUP_TYPE;
				}
			}

			mpegtscheck:
			for (int a=0; a < bs4; a++) //DM17012004 081.6 int11 changed
			{ 
				if ( check[a]!=0x47 || check[a+188]!=0x47 || check[a+376]!=0x47 || check[a+564]!=0x47 || check[a+752]!=0x47) 
					continue mpegtscheck;

				PMTcheck(check, a);

				return Common.TS_TYPE;
			}

			pvacheck:
			for (int a=0; a < bs1; a++)
			{
				if ( check[a]!=(byte)0x41 || check[a+1]!=(byte)0x56 || check[a+4]!=(byte)0x55 ) 
					continue pvacheck;

				int next = a+8+( (255&check[a+6])<<8 | (255&check[a+7]) );

				if ( check[next]!=(byte)0x41 || check[next+1]!=(byte)0x56 || check[next+4]!=(byte)0x55 ) 
					continue pvacheck;

				else
				{
					if (more) 
						loadPVA(check,a);

					return Common.PVA_TYPE;
				}
			}

			mpgcheck:
			for (int a=0; a<bs2; a++)
			{
				if ( check[a]!=0 || check[a+1]!=0 || check[a+2]!=1 || check[a+3]!=(byte)0xba ) 
					continue mpgcheck;

				if ( (0xC0 & check[a+4])==0 )
				{ 
					if (more) 
						loadMPG2(check, a, false, true, bs1); 

					return Common.MPEG1PS_TYPE;
				}

				else if ( (0xC0 & check[a+4])==0x40 )
				{
					if (more) 
					//	loadMPG2(check, a, false, false, bs1); 
						loadMPG2(check, a, X.cBox[14].isSelected(), false, bs1); 

					return Common.MPEG2PS_TYPE;
				}
			}

			vdrcheck:
			for (int a=0; a < bs4; a++)
			{
				if ( check[a]!=0 || check[a+1]!=0 || check[a+2]!=1 || (0xF0&check[a+3])!=0xe0 ) 
					continue vdrcheck;

				int next = a+6+( (255&check[a+4])<<8 | (255&check[a+5]) );

				if (next==a+6 && (0xC0&check[a+6])==0x80 && (0xC0&check[a+8])==0)
				{ 
					addInfo += " !!(Vpacketsize=0)"; 
					next=a; 
					nullpacket=true; 
				}

				if ( check[next]!=0 || check[next+1]!=0 || check[next+2]!=1 ) 
					continue vdrcheck;

				else
				{
					if (more) 
						loadMPG2(check, a, true, false, bs3); 

					return Common.PES_AV_TYPE;
				}
			}

			rawcheck:
			for (int a=0; a < bs2; a++)
			{
				if ( check[a] != 0 || check[a+1] != 0 || check[a+2] != 1 ) 
					continue rawcheck;

				if ( (0xE0 & check[a+3]) == 0xC0 || (0xFF & check[a+3]) == 0xBD )
				{
					int next = a + 6 + ( (0xFF & check[a+4])<<8 | (0xFF & check[a+5]) );

					if ( check[next] != 0 || check[next+1] != 0 || check[next+2] != 1 ) 
						continue rawcheck;

					if ( (0xE0 & check[a+3])==0xC0 && (0xE0 & check[a+3])==(0xE0 & check[next+3]) )
					{
						if (more)
							loadMPG2(check, a, true, false, bs3); 

						return Common.PES_MPA_TYPE;
					}

					else if ( (0xFF & check[a+3]) == 0xBD && check[a+3] == check[next+3] )
					{
						if (more)
						{
							//DM10032004 081.6 int18 changed
							if (check[a+8]==0x24 && (0xF0&check[a+9+0x24])>>>4 == 1)
							{
								addInfo=" (TTX)";
								text = "SubID 0x"+Integer.toHexString((0xFF & check[a+9+0x24])).toUpperCase();
							}
							else
								loadMPG2(check, a, true, false, bs3);
						}

						return Common.PES_PS1_TYPE;
					}
				}
			}

			//ES audio
			audiocheck:
			for (int a=0; a < bs0; a++)
			{
				/* DTS stuff taken from the VideoLAN project. */ 
				/* Added by R One, 2003/12/18. */ 
				//DM20122003 081.6 int07 add
				if (Audio.DTS_parseHeader(check, a) > 0)
				{ 
					for (int b=0; b < 15; b++)
					{ 
						if (Audio.DTS_parseNextHeader(check,a + Audio.Size + b) == 1)
						{ 
							if ( (0xFF & check[a + Audio.Size]) > 0x7f || (0xFF & check[a+Audio.Size])==0 ) //smpte 
								continue audiocheck; 

							if (more)
							{ 
								audio = Audio.DTS_saveAnddisplayHeader(); 
								playtime = getAudioTime(size); 
							} 

							if (b==0) 
								return Common.ES_DTS_TYPE; 

							else 
								return Common.ES_DTS_A_TYPE;
						} 
					} 

					//DM30122003 081.6 int10 new
					if (X.RButton[7].isSelected())
					{
						if (more) 
							audio = Audio.DTS_saveAnddisplayHeader();

						playtime = getAudioTime(size); 

						return Common.ES_DTS_TYPE;
					}
				}

				else if (Audio.AC3_parseHeader(check, a) > 0)
				{ 
					for (int b=0; b < 17; b++)
					{
						if (Audio.AC3_parseNextHeader(check,a + Audio.Size + b) == 1)
						{
							if ( (0xFF & check[a+Audio.Size]) > 0x3f || (0xFF & check[a + Audio.Size]) == 0 ) //smpte
								continue audiocheck;

							if (more)
							{
								audio = Audio.AC3_saveAnddisplayHeader();
								playtime = getAudioTime(size);
							}

							if (b==0)
								return Common.ES_AC3_TYPE;

							else 
								return Common.ES_AC3_A_TYPE;
						}
					}

					//DM30122003 081.6 int10 new
					if (X.RButton[7].isSelected())
					{
						if (more) 
							audio = Audio.AC3_saveAnddisplayHeader();

						playtime = getAudioTime(size); 

						return Common.ES_AC3_TYPE;
					}
				}

				else if (Audio.MPA_parseHeader(check, a) > 0)
				{
					if (!X.RButton[7].isSelected() && Audio.MPA_parseNextHeader(check, a + Audio.Size) < 0)  //DM30122003 081.6 int10 changed
						continue audiocheck;

					if (more)
					{
						audio = Audio.MPA_saveAnddisplayHeader();
						playtime = getAudioTime(size);
					}

					return Common.ES_MPA_TYPE;
				}
			}

			mpvcheck:
			for (int a=0; a < bs3; a++)
			{
				if ( check[a] != 0 || check[a+1] != 0 || check[a+2] != 1 ) 
					continue mpvcheck;

				if ( check[a+3] == (byte)0xb3 )
				{
					for (int b=7; b < 600; b++)
					{
						if ( check[a+b]==0 && check[a+b+1]==0 && check[a+b+2]==1 && check[a+b+3]==(byte)0xb8 )
						{
							if (more)
							{ 
								hasVideo=true;
								System.arraycopy(check,a,vbasic,0,12);
								bytecheck.write(check,a,20);

								video_streams.add(Video.videoformatByte(bytecheck.toByteArray()));
							}

							return Common.ES_MPV_TYPE;
						}
					}
				}
				else if ( check[a+3]==(byte)0xb8 )
				{
					for (int b=6; b < 20; b++) 
					{
						if ( check[a+b]==0 && check[a+b+1]==0 && check[a+b+2]==1 && check[a+b+3]==0 )
						{ 
							if (more) 
								video = msg_9;

							return Common.ES_MPV_TYPE;
						}
					}
				} 
			} 

		} 
		catch ( IOException e )
		{
			playtime = msg_8;
		}

		check=null; //DM22122003 081.6 int09 new
		System.gc();

		return Common.Unsupported;
	}



	class ScanObject
	{
		private ByteArrayOutputStream buf = null;
		private int id;
		private int type;

		private ScanObject()
		{
			buf = new ByteArrayOutputStream();
			id = 0;
		}

		private ScanObject(int val1)
		{
			buf = new ByteArrayOutputStream();
			id = val1;
		}

		private int getType()
		{
			return type;
		}

		private void write(byte data[]) throws IOException
		{
			buf.write(data);
		}

		private void write(byte data[], int offset, int length) throws IOException
		{
			buf.write(data, offset, length);
		}

		private byte[] getData() throws IOException
		{
			buf.flush();
			return buf.toByteArray();
		}

		private void reset()
		{
			buf.reset();
		}
	}

} /** end class **/
