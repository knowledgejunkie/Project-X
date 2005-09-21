/*
 * @(#)StreamDemultiplexer
 *
 * Copyright (c) 2005 by dvb.matt, All Rights Reserved.
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

import java.util.ArrayList;
import java.util.List;

import java.io.File;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.BufferedWriter;
import java.io.FileWriter;

import net.sourceforge.dvb.projectx.io.IDDBufferedOutputStream;

import net.sourceforge.dvb.projectx.common.Resource;
import net.sourceforge.dvb.projectx.common.Keys;
import net.sourceforge.dvb.projectx.common.Common;
import net.sourceforge.dvb.projectx.common.JobProcessing;

import net.sourceforge.dvb.projectx.parser.CommonParsing;
import net.sourceforge.dvb.projectx.video.Video;

/**
 * demuxes all packetized data
 */
public class StreamDemultiplexer extends Object {

	private boolean ptsover = false;
	private boolean misshead = false;
	private boolean first = true;
	private boolean overlap = false;
	private boolean seqhead = false;
	private boolean isPTSwritten = false;

	private boolean WriteNonVideo;
	private boolean WriteVideo;
	private boolean Debug;
	private boolean DecodeVBI;
	private boolean RebuildPTS;
	private boolean Streamtype_MpgVideo;
	private boolean Streamtype_MpgAudio;
	private boolean Streamtype_Ac3Audio;
	private boolean Streamtype_PcmAudio;
	private boolean Streamtype_Teletext;
	private boolean Streamtype_Subpicture;
	private boolean AddSequenceEndcode;
	private boolean RenameVideo;
	private boolean CreateD2vIndex;
	private boolean CreateM2sIndex;
	private boolean SplitProjectFile;
	private boolean CreateCellTimes;

	private long AddOffset = 0;
	private long target_position = 0;
	private long ptsoffset = 0; 
	private long pts = -1;
	private long lastPTS = -1;

	private int pack = -1;
	private int pes_ID = 0;
	private int newID = 0;
	private int PID = 0;
	private int es_streamtype = 0;
	private int subid = 0x1FF;
	private int pes_streamtype = 0;
	private int lfn = -1;
	private int buffersize = 1024;
	private int sourcetype = 0;
	private int[] MPGVideotype = { 0 }; // 0 =m1v, 1 = m2v, changed at goptest

	private String FileName = "";
	private String parentname = "";
	private String[] type = { "ac", "tt", "mp", "mv", "pc", "sp", "vp" };
	private String[] source = { ".$spes$", ".$ppes$", ".$ts$", ".$pva$" };
	private String[] videoext = { ".mpv", ".mpv", ".m1v", ".m2v" };

	private IDDBufferedOutputStream out;
	private DataOutputStream pts_log;
	private ByteArrayOutputStream vidbuf;
	private ByteArrayOutputStream vptsbytes;
	private ByteArrayOutputStream packet;
	private DataOutputStream vpts;

	private byte[] subpicture_header = { 0x53, 0x50, 0, 0, 0, 0, 0, 0, 0, 0 };
	private byte[] lpcm_header = { 0x50, 0x43, 0x4D, 0, 0, 0, 0, 0, 0, 0 }; //'PCM'+5b(pts)+2b(size) 

	public StreamDemultiplexer()
	{
		getSettings();
	}

	public StreamDemultiplexer(long val)
	{
		getSettings();
		ptsoffset = val;
	}

	/**
	 * Object yet intialized
	 */
	public int getNum()
	{ 
		return lfn; 
	}

	/**
	 * get PID for later selection
	 */
	public int getPID()
	{ 
		return PID; 
	}

	/**
	 * returns PES pes_ID for later selection
	 */
	public int getID()
	{ 
		return pes_ID; 
	}

	/**
	 * set PID for later selection
	 */
	public void setPID(int val)
	{ 
		PID = val; 
	}

	/**
	 * set pes_ID for later selection
	 */
	public void setID(int val)
	{ 
		pes_ID = val; 
	}

	/**
	 * set newID for later selection /or subid
	 */
	public void setnewID(int val)
	{ 
		newID = val; 
	} 

	/**
	 * returns newID for later selection /or subid
	 */
	public int getnewID()
	{ 
		return newID; 
	}

	/**
	 * returns packet counter
	 */
	public int getPackCount()
	{ 
		return pack; 
	}

	/**
	 * returns es_streamtype
	 */
	public int getType()
	{ 
		return es_streamtype; 
	}

	/**
	 * sets stream tpye, vdr/es/mpeg1/2...
	 */
	public void setStreamType(int val)
	{ 
		pes_streamtype = val; 
	}

	/**
	 * returns stream tpye, vdr/es/mpeg1/2...
	 */
	public int getStreamType()
	{ 
		return pes_streamtype; 
	}

	/**
	 * sets type
	 */
	public void setType(int val)
	{ 
		es_streamtype = val; 
	}

	/**
	 * sets subid
	 */
	public void setsubID(int val)
	{ 
		subid = val; 
	}

	/**
	 * returns subid 
	 */
	public int subID()
	{ 
		return subid; 
	}

	/**
	 * is it TTX?
	 */
	public boolean isTTX()
	{ 
		return es_streamtype == CommonParsing.TELETEXT; 
	}

	/**
	 * set ttx
	 */
	public void setTTX(boolean b)
	{ 
		if (b) 
			es_streamtype = CommonParsing.TELETEXT; 
	}

	/** 
	 * last PTS
	 */
	public long getPTS()
	{ 
		return pts; 
	}

	/**
	 * PTS offset if needed
	 */
	public void PTSOffset(long val)
	{ 
		ptsoffset = val; 
	}

	/**
	 * stream type preselector
	 */
	public boolean StreamEnabled()
	{
		switch(newID>>>4)
		{
		case 0xE:  //video
			return Streamtype_MpgVideo;

		case 0xC:  //mpa
		case 0xD:
			return Streamtype_MpgAudio;

		case 0x8:  //ac3,mpg
			return Streamtype_Ac3Audio;

		case 0xA:  //lpcm,mpg
			return Streamtype_PcmAudio;

		case 0x9:  //ttx
			return Streamtype_Teletext;

		case 0x2:  //subpic
		case 0x3: 
			return Streamtype_Subpicture;

		default:
			return true;
		}
	}


	/**
	 *
	 */
	private void getSettings()
	{
		Streamtype_MpgVideo = Common.getSettings().getBooleanProperty(Keys.KEY_Streamtype_MpgVideo);
		Streamtype_MpgAudio = Common.getSettings().getBooleanProperty(Keys.KEY_Streamtype_MpgAudio);
		Streamtype_Ac3Audio = Common.getSettings().getBooleanProperty(Keys.KEY_Streamtype_Ac3Audio);
		Streamtype_PcmAudio = Common.getSettings().getBooleanProperty(Keys.KEY_Streamtype_PcmAudio);
		Streamtype_Teletext = Common.getSettings().getBooleanProperty(Keys.KEY_Streamtype_Teletext);
		Streamtype_Subpicture = Common.getSettings().getBooleanProperty(Keys.KEY_Streamtype_Subpicture);
		AddOffset = Common.getSettings().getBooleanProperty(Keys.KEY_additionalOffset) ? 90L * Common.getSettings().getIntProperty(Keys.KEY_ExportPanel_additionalOffset_Value) : 0;    // time offset for data
		WriteNonVideo = Common.getSettings().getBooleanProperty(Keys.KEY_WriteOptions_writeAudio);
		WriteVideo = Common.getSettings().getBooleanProperty(Keys.KEY_WriteOptions_writeVideo);
		Debug = Common.getSettings().getBooleanProperty(Keys.KEY_DebugLog);
		DecodeVBI = Common.getSettings().getBooleanProperty(Keys.KEY_Streamtype_Vbi);
		RebuildPTS = Common.getSettings().getBooleanProperty(Keys.KEY_SubtitlePanel_rebuildPTS);
		AddSequenceEndcode = Common.getSettings().getBooleanProperty(Keys.KEY_VideoPanel_addEndcode);
		RenameVideo = Common.getSettings().getBooleanProperty(Keys.KEY_ExternPanel_renameVideo);
		CreateD2vIndex = Common.getSettings().getBooleanProperty(Keys.KEY_ExternPanel_createD2vIndex);
		CreateM2sIndex = Common.getSettings().getBooleanProperty(Keys.KEY_ExternPanel_createM2sIndex);
		SplitProjectFile = Common.getSettings().getBooleanProperty(Keys.KEY_ExternPanel_splitProjectFile);
		CreateCellTimes = Common.getSettings().getBooleanProperty(Keys.KEY_ExternPanel_createCellTimes);
	}

	/**
	 * init nonVideo streams
	 */
	private void initNonVideo(String _name)
	{
		parentname = _name;
		FileName = parentname + source[sourcetype] + lfn;
		target_position = 0;

		getSettings();

		try {
			out = new IDDBufferedOutputStream(new FileOutputStream(FileName), buffersize);
			pts_log = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(FileName + ".pts"), 65535));

		} catch (IOException e) { 

			Common.setExceptionMessage(e);
		}
	}

	/**
	 * main init nonVideo
	 */
	public void init(String _name, int _buffersize, int _lfn, int _parsertype)
	{
		lfn = _lfn;
		buffersize = _buffersize;
		sourcetype = _parsertype;

		initNonVideo(_name);
	}

	/**
	 * re-init nonVideo
	 */
	public void init2(String _name)
	{
		initNonVideo(_name);
	}

	/**
	 * process nonvideo data = 1 pespacket from demux
	 */
	public void write(JobProcessing job_processing, byte[] pes_packet, boolean pes_hasHeader)
	{
		write(job_processing, pes_packet, 0, pes_packet.length, pes_hasHeader);
	}

	/**
	 * process nonvideo data = 1 pespacket from demux
	 */
	public void write(JobProcessing job_processing, byte[] pes_packet, int pes_packetoffset, int pes_payloadlength, boolean pes_hasHeader)
	{
		boolean pes_isAligned = false;

		int pes_extensionlength = 0;
		int offset = pes_packetoffset;
		int _es_streamtype = CommonParsing.AC3_AUDIO;

		pack++;

		if (pes_hasHeader)
		{
			if (CommonParsing.validateStartcode(pes_packet, offset) < 0)
			{
				Common.setMessage(Resource.getString("demux.error.audio.startcode") + " " + pack + " (" + Integer.toHexString(PID) + "/" + Integer.toHexString(pes_ID) + "/" + Integer.toHexString(newID) + "/" + es_streamtype + ")");
				return;
			}

			pes_ID = CommonParsing.getPES_IdField(pes_packet, offset);
			pes_payloadlength = CommonParsing.getPES_LengthField(pes_packet, offset);
			pes_isAligned = (pes_streamtype == CommonParsing.PES_AV_TYPE || pes_streamtype == CommonParsing.MPEG2PS_TYPE) && (4 & pes_packet[6 + offset]) != 0;

			if (pes_streamtype == CommonParsing.MPEG1PS_TYPE)
			{
				skiploop:
				while(true)
				{
					switch (0xC0 & pes_packet[6 + offset])	
					{
					case 0x40:
						offset += 2; 
						continue skiploop;

					case 0x80:
						offset += 3; 
						continue skiploop; 

					case 0xC0:
						offset++;  
						continue skiploop; 

					case 0:
						break;
					}

					switch (0x30 & pes_packet[6 + offset])
					{
					case 0x20:  //PTS
						pes_extensionlength = 5;  
						break skiploop; 

					case 0x30:  //PTS+DTS
						pes_extensionlength = 10; 
						break skiploop; 

					case 0x10:  //DTS
						offset += 5; 
						break skiploop; 

					case 0:
						offset++; 
						break skiploop; 
					}
				}
			}

			else
			{
				pes_extensionlength = CommonParsing.getPES_ExtensionLengthField(pes_packet, offset);

				if (pes_ID == CommonParsing.PRIVATE_STREAM_1_CODE && pes_extensionlength == 0x24 && (0xFF & pes_packet[9 + pes_extensionlength + offset])>>>4 == 1)
					_es_streamtype = CommonParsing.TELETEXT;

				/** 
				 * no PTS in PES_extension
				 */
				if ((0x80 & pes_packet[7 + offset]) == 0)
				{
					offset += pes_extensionlength;
					pes_extensionlength = 0;
				}

				offset += 3;
			}

			es_streamtype = pes_ID == CommonParsing.PRIVATE_STREAM_1_CODE ? _es_streamtype : CommonParsing.MPEG_AUDIO;
			subid = ((es_streamtype == CommonParsing.AC3_AUDIO || es_streamtype == CommonParsing.DTS_AUDIO || es_streamtype == CommonParsing.TELETEXT) && ((pes_streamtype == CommonParsing.PES_AV_TYPE && (pes_isAligned || es_streamtype == CommonParsing.TELETEXT)) || pes_streamtype == CommonParsing.MPEG2PS_TYPE)) ? (0xFF & pes_packet[9 + (0xFF & pes_packet[8 + pes_packetoffset]) + pes_packetoffset]) : 0;

			switch (subid>>>4)
			{
			case 8:
				if (pes_streamtype == CommonParsing.PES_AV_TYPE || pes_streamtype == CommonParsing.MPEG1PS_TYPE)
				{
					subid = 0;
					break;
				}

			case 1:
			case 2:
			case 3:
			case 9:
			case 0xA:
				break;

			default:
				if (pes_streamtype != CommonParsing.MPEG2PS_TYPE)
					subid = 0;
			}


			switch (subid>>>4)
			{
			case 0xA: //LPCM from MPG-PS
				es_streamtype = CommonParsing.LPCM_AUDIO;
				break;

			case 2:   //SubPic 0-31 from MPG-PS
			case 3:   //SubPic 32-63 from MPG-PS
				es_streamtype = CommonParsing.SUBPICTURE;
				break;

			case 8:   //AC3-DTS from MPG-PS
			case 1:   //TTX
			case 0:   //AC3-DTS from PES/VDR
				break;

			case 9:   //VBI from TS,MPG2
				if (pes_isAligned)
				{
					if (pes_streamtype == CommonParsing.MPEG1PS_TYPE || pes_streamtype == CommonParsing.PES_AV_TYPE)
						subid = 0;

					if (DecodeVBI)
						VBI.parsePES(pes_packet, pes_packetoffset);

					return;
				}
				break;

			default:
				return;
			}

			pes_payloadlength -= (offset - pes_packetoffset + pes_extensionlength);
			offset += 6;
		}

		if (!WriteNonVideo) 
			return;

		if (out == null)
			return;

		try {

			/**
			 * re-build PTS
			 */
			if (es_streamtype == CommonParsing.TELETEXT && RebuildPTS)
			{
				if (job_processing.getBorrowedPts() != lastPTS)
				{
					lastPTS = job_processing.getBorrowedPts();

					pts_log.writeLong(lastPTS);
					pts_log.writeLong(target_position);

					if (Debug)
						System.out.println(" stolen ttx PTS: " + lastPTS + " /ao " + AddOffset + " /tp " + target_position);
				}
			}

			/**
			 * read out source PTS
			 */
			else if (pes_extensionlength > 0 && pes_payloadlength >= 0)
			{
				pts = CommonParsing.getPTSfromBytes(pes_packet, offset); //returns 32bit

				pts -= job_processing.getNextFileStartPts();
				pts &= 0xFFFFFFFFL;

				if ( (pts & 0xFF000000L) == 0xFF000000L ) 
					ptsover = true;      // bit 33 was set

				if (ptsover && pts < 0xF0000000L) 
					pts |= 0x100000000L;

				pts += ptsoffset;
				pts += AddOffset;

				if (lastPTS != pts)
				{
					pts_log.writeLong(pts);
					pts_log.writeLong(target_position);
				}

				if (Debug)
					System.out.println(" pda PTS: " + pts + "/ " + AddOffset + "/ " + target_position);

				lastPTS = pts;
			} 

			/**
			 * save re-build PTS, taken from 1st mpa
			 */
			if (newID == 0xC0 && job_processing.getBorrowedPts() != lastPTS)
				job_processing.setBorrowedPts(lastPTS);

			/**
			 * skip subid and info fields
			 */
			switch(subid>>>4)
			{
			case 0xA: //LPCM, keep info fields 6 bytes
				offset += 1; //7
				pes_payloadlength -= 1; //7
				break;

			case 8: //AC3-DTS
				offset += 4; 
				pes_payloadlength -= 4; 
				break;

			case 1: //TTX
			case 2: //subpic  0.31
			case 3: //subpic 32.63
				offset += 1; 
				pes_payloadlength -= 1; 
			}

			if (pes_payloadlength <= 0)
				return;

			if (pes_extensionlength > 0)
			{
				switch(es_streamtype)
				{
				case CommonParsing.SUBPICTURE:
					CommonParsing.setValue(subpicture_header, 2, 8, CommonParsing.BYTEREORDERING, pts);

					target_position += writePacket(subpicture_header);

					/**
					 * DVB subs adaption
					 */
					if (CommonParsing.nextBits(pes_packet, (offset + pes_extensionlength) * 8, 16) == 0xF)
					{
						out.write(0xFF & (pes_payloadlength + 3)>>>8);
						out.write(0xFF & (pes_payloadlength + 3));
						out.write(0); //padding

						target_position += 3;
					}

					break;

				case CommonParsing.LPCM_AUDIO:
					CommonParsing.setValue(lpcm_header, 3, 5, CommonParsing.BYTEREORDERING, pts);

					lpcm_header[8] = (byte)(0xFF & pes_payloadlength>>>8);
					lpcm_header[9] = (byte)(0xFF & pes_payloadlength);

					target_position += writePacket(lpcm_header);
				}
			}

			/**
			 * DVB subs adaption, prevent lost packets
			 */
			else if (es_streamtype == CommonParsing.SUBPICTURE && CommonParsing.nextBits(pes_packet, (offset + pes_extensionlength) * 8, 16) == 0xF)
			{
				CommonParsing.setValue(subpicture_header, 2, 8, CommonParsing.BYTEREORDERING, 0);

				target_position += writePacket(subpicture_header);

				out.write(0xFF & (pes_payloadlength + 3)>>>8);
				out.write(0xFF & (pes_payloadlength + 3));
				out.write(0); //padding!

				target_position += 3;
			}

			target_position += writePacket(pes_packet, offset + pes_extensionlength, pes_payloadlength);

		} catch (IOException e) { 

			Common.setExceptionMessage(e);
		}
	}

	/**
	 *
	 */
	public String[] close(JobProcessing job_processing, String _vptslog)
	{ 
		String pts_log_name = FileName + ".pts";
		String parameters[] = { FileName, pts_log_name, type[es_streamtype], parentname };

		try {

			if (out == null)
			{
				parameters[0] = "";
				return parameters;
			}

			out.flush(); 
			out.close();

			pts_log.flush(); 
			pts_log.close(); 

			if (new File(pts_log_name).length() < 10) 
				CommonParsing.logAlias(job_processing, _vptslog, pts_log_name);

			if (new File(FileName).length() < 10)
			{
				new File(FileName).delete();
				new File(pts_log_name).delete();
				parameters[0] = "";
			}

		} catch (IOException e) { 

			Common.setExceptionMessage(e);
		}

		return parameters;
	} 

	/**
	 *
	 */
	public void initVideo(String _name, int _buffersize, int _lfn, int _parsertype)
	{
		getSettings();

		parentname = _name;
		lfn = _lfn;
		buffersize = _buffersize;
		sourcetype = _parsertype;

		FileName = parentname + source[sourcetype] + lfn;
		es_streamtype = CommonParsing.MPEG_VIDEO;
		MPGVideotype[0] = 0;

		try {
			out = new IDDBufferedOutputStream(new FileOutputStream(FileName), buffersize);

			if (CreateM2sIndex)
				out.InitIdd(FileName, 1);
		
			pts_log = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(FileName + ".pts"), 65535));
			packet = new ByteArrayOutputStream();
			vidbuf = new ByteArrayOutputStream();
			vptsbytes = new ByteArrayOutputStream();
			vpts = new DataOutputStream(vptsbytes);

		} catch (IOException e) { 

			Common.setExceptionMessage(e);
		}
	}

	public void initVideo2(String _name)
	{
		getSettings();

		parentname = _name;
		FileName = parentname + source[sourcetype] + lfn;

		first = true;
		MPGVideotype[0] = 0;

		try {
			out = new IDDBufferedOutputStream(new FileOutputStream(FileName), buffersize);

			if (CreateM2sIndex)
				out.InitIdd(FileName, 1);
		
			pts_log = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(FileName + ".pts"), 65535));

			packet.reset();
	 		vidbuf.reset();
			vptsbytes.reset();

		} catch (IOException e) { 

			Common.setExceptionMessage(e);
		}
	}


	/**
	 * clean Up for next foreign inputfile in case of multiple
	 */
	public void resetVideo()
	{
		if (vidbuf != null)
			vidbuf.reset(); 

		if (packet != null)
			packet.reset(); 

		if (vptsbytes != null)
			vptsbytes.reset(); 

		first = true;
	}

	public String closeVideo(JobProcessing job_processing, String workouts)
	{ 
		String logfile = "-1";

		List cell = job_processing.getCellTimes();

		int[] clv = job_processing.getStatusVariables();

		try {

			if (AddSequenceEndcode && job_processing.getExportedVideoFrameNumber() > 0)
			{
				out.write(Video.getSequenceEndCode());

				job_processing.countMediaFilesExportLength(+4);
				job_processing.countAllMediaFilesExportLength(+4);
			}

			packet.close();
			vidbuf.flush(); 
			vidbuf.close();
			out.flush();
			out.close();
			pts_log.flush(); 
			pts_log.close();
			vpts.flush(); 
			vpts.close();
			vptsbytes.flush(); 
			vptsbytes.close();

			String videofile = "";

			if (new File(FileName).length() < 10)
			{
				new File(FileName).delete();

				if (!WriteVideo && new File(FileName + ".pts").length() > 16) 
					logfile = FileName + ".pts";

				else 
					new File(FileName + ".pts").delete();
			}

			else
			{ 
				int ot = (RenameVideo || CreateD2vIndex || SplitProjectFile) ? 0 : 2;

				videofile = parentname + videoext[MPGVideotype[0] + ot];
				File newfile = new File(videofile);

				if (newfile.exists()) 
					newfile.delete();

				Common.renameTo(new File(FileName), newfile);

				logfile = FileName + ".pts";

				CommonParsing.setVideoHeader(job_processing, videofile, logfile, clv, MPGVideotype);
        
				/**
				 * celltimes.txt 
				 */
				if (CreateCellTimes && !cell.isEmpty())
				{
					BufferedWriter cellout = new BufferedWriter(new FileWriter(workouts + "CellTimes.txt"));

					for (int i = 0; i < cell.size(); i++)
					{
						cellout.write(cell.get(i).toString());
						cellout.newLine();
					}

					cellout.close();

					Common.setMessage(Resource.getString("demux.msg.celltimes", workouts));

					long fl = new File(workouts + "CellTimes.txt").length();

					job_processing.countMediaFilesExportLength(fl);
					job_processing.countAllMediaFilesExportLength(fl);
				}

				cell.clear();
			}

			if (CreateM2sIndex)
			{
				if (new File(videofile).exists())
					out.renameVideoIddTo(parentname);

				else
					out.deleteIdd();
			}

		} catch (IOException e) { 

			Common.setExceptionMessage(e);
		}

		return logfile;
	} 



	/**
	 * temporary redirected access to goptest from video-es
	 */
	public void writeVideoES(JobProcessing job_processing, IDDBufferedOutputStream _out, byte[] _vidbuf, byte[] _vptsbytes, DataOutputStream _pts_log, String _parentname, int[] _MPGVideotype, List _CutpointList, List _ChapterpointList, boolean _doWrite)
	{
		job_processing.getGop().goptest(job_processing, _out, _vidbuf, _vptsbytes, _pts_log, _parentname, _MPGVideotype, _CutpointList, _ChapterpointList, _doWrite);
	}

	/**
	 * write video 
	 * data = 1 pespacket from demux
	 */
	public void writeVideo(JobProcessing job_processing, byte[] pes_packet, boolean pes_hasHeader, List CutpointList, List ChapterpointList)
	{
		writeVideo(job_processing, pes_packet, 0, pes_packet.length, pes_hasHeader, CutpointList, ChapterpointList);
	}

	/**
	 * write video 
	 * data = 1 pespacket from demux
	 */
	public void writeVideo(JobProcessing job_processing, byte[] pes_packet, int pes_packetoffset, int pes_payloadlength, boolean pes_hasHeader, List CutpointList, List ChapterpointList)
	{
		int pes_extensionlength = 0;
		int offset = pes_packetoffset;
		byte[] data = null;

		int[] clv = job_processing.getStatusVariables();

		pack++;

		if (!pes_hasHeader)
		{
			if (job_processing.getPvaVideoPts() != -1)
			{
				offset -= 4;
				pes_extensionlength += 4;
			}
		}

		else
		{
			if (CommonParsing.validateStartcode(pes_packet, offset) < 0)
			{
				Common.setMessage(Resource.getString("demux.error.video.startcode") + " " + pack + " (" + Integer.toHexString(PID) + "/" + Integer.toHexString(pes_ID) + "/" + Integer.toHexString(newID) + "/" + es_streamtype + ")");
				return;
			}

			pes_ID = CommonParsing.getPES_IdField(pes_packet, offset);
			pes_payloadlength = CommonParsing.getPES_LengthField(pes_packet, offset);

			if (pes_streamtype == CommonParsing.MPEG1PS_TYPE)
			{
				skiploop:
				while(true)
				{
					switch (0xC0 & pes_packet[6 + offset])
					{
					case 0x40: 
						offset += 2; 
						continue skiploop; 

					case 0x80:
						offset += 3; 
						continue skiploop; 

					case 0xC0:  
						offset++;  
						continue skiploop; 

					case 0:
						break;
					}

					switch (0x30 & pes_packet[6 + offset])
					{
					case 0x20:  //PTS
						pes_extensionlength = 5;  
						break skiploop; 

					case 0x30:  //PTS + DTS
						pes_extensionlength = 10; 
						break skiploop; 

					case 0x10:  //DTS
						offset += 5; 
						break skiploop; 

					case 0:
						offset++; 
						break skiploop; 
					}
				}
			}

			else
			{
				pes_extensionlength = CommonParsing.getPES_ExtensionLengthField(pes_packet, offset);

				if ((0x80 & pes_packet[7 + offset]) == 0)
				{ 
					offset += pes_extensionlength;
					pes_extensionlength = 0;
				}

				offset += 3;
			}

			pes_payloadlength -= (offset - pes_packetoffset + pes_extensionlength);
			offset += 6;
		}

		/**
		 * read pts
		 */
		if (pes_extensionlength > 0 && pes_payloadlength >= 0)
		{
			pts = !pes_hasHeader ? job_processing.getPvaVideoPts() : CommonParsing.getPTSfromBytes(pes_packet, offset); //returns 32bit

			pts -= job_processing.getNextFileStartPts();
			pts &= 0xFFFFFFFFL;

			if ( (pts & 0xFF000000L) == 0xFF000000L ) 
				ptsover = true;      // bit 33 was set

			if (ptsover && pts < 0xF0000000L) 

				pts |= 0x100000000L;

			pts += ptsoffset;

			if (Debug)
				System.out.println(" pdv PTS: " + pts);

			isPTSwritten = false;
		} 

		try {

			if (pes_payloadlength <= 0)
				Common.setMessage(Resource.getString("demux.error.video.payload") + " (" + pack + "/" + pes_packet.length + "/" + offset + "/" + pes_extensionlength + "/" + pes_payloadlength + ")");

			else
				packet.write(pes_packet, offset + pes_extensionlength, pes_payloadlength);

			packet.flush();

			data = packet.toByteArray();

			packet.reset();

			boolean gop = false;
			boolean packetfirstframe = true;

			packloop:
			for (int i = 0, j = 0, id, returncode; i < data.length - 3; i++)
			{
				if ((returncode = CommonParsing.validateStartcode(data, i)) < 0)
				{
					i += (-returncode) - 1;
					continue packloop;
				}

				id = CommonParsing.getPES_IdField(data, i);

				/**
				 * new frame at first 
				 */
				if (!isPTSwritten && packetfirstframe && id == CommonParsing.PICTURE_START_CODE)
				{
					if (misshead && i < 3)
					{ 
						misshead = false; 
						continue packloop; 
					}

					if (pts != -1)
					{
						vpts.writeLong(pts);
						vpts.writeLong((long)vidbuf.size());
						vpts.flush();
					}

					isPTSwritten = true;
					packetfirstframe = false;
					i += 8;
				}

				else if (id == CommonParsing.SEQUENCE_HEADER_CODE || id == CommonParsing.SEQUENCE_END_CODE || id == CommonParsing.GROUP_START_CODE)
				{
					if (id == CommonParsing.SEQUENCE_HEADER_CODE) 
						seqhead = true;

					if (id == CommonParsing.GROUP_START_CODE && seqhead && vidbuf.size() < 400)
					{ 
						seqhead = false; 
						continue packloop; 
					}

					vidbuf.write(data, j, i);

					if (!first) 
						job_processing.getGop().goptest(job_processing, out, vidbuf.toByteArray(), vptsbytes.toByteArray(), pts_log, parentname, MPGVideotype, CutpointList, ChapterpointList);

					vptsbytes.reset();
					vidbuf.reset(); 

					/**
					 * split size reached 
					 */
					if (job_processing.getSplitSize() > 0 && job_processing.getSplitSize() < job_processing.getAllMediaFilesExportLength()) 
						return;

					/**
					 * d2v split reached 
					 */
					if (SplitProjectFile && job_processing.getProjectFileExportLength() > job_processing.getProjectFileSplitSize())
					{
						int part = job_processing.getProjectFileD2V().getPart() + 1;
						String newpart = parentname + "[" + part + "].mpv";

						/**
						 * sequence end code 
						 */
						if (WriteVideo && AddSequenceEndcode && job_processing.getExportedVideoFrameNumber() > 0 )
						{
							out.write(Video.getSequenceEndCode());

							job_processing.countMediaFilesExportLength(+4);
							job_processing.countAllMediaFilesExportLength(+4);
						}

						out.flush();
						out.close();
						//System.gc();
	
						out = new IDDBufferedOutputStream( new FileOutputStream(newpart), buffersize);

						/**
						 * M2S idd
						 */
						if (CreateM2sIndex)
							out.InitIdd(newpart, 1);

						job_processing.getProjectFileD2V().setFile(newpart);
						job_processing.setProjectFileExportLength(0);
					}

					if (!isPTSwritten && packetfirstframe)
					{
						if (pts != -1)
						{
							vpts.writeLong(pts);
							vpts.writeLong(vidbuf.size());
							vpts.flush();
						}

						isPTSwritten = true;
					}


					if (id == CommonParsing.SEQUENCE_END_CODE)
					{
						Common.setMessage(Resource.getString("demux.msg.skip.sec") + " " + clv[6]);

						first = true;
						job_processing.setSequenceHeader(false);

						i += 3;
						continue packloop;
					}
					else 
						vidbuf.write(data, i, data.length - i);


					if (id == CommonParsing.GROUP_START_CODE)
					{
						job_processing.setSequenceHeader(false);

						if (job_processing.getSplitPart() > 0) 
							first = false;
					}
					else if (id == CommonParsing.SEQUENCE_HEADER_CODE)
					{ 
						job_processing.setSequenceHeader(true);
						first = false;
					}

					gop = true;
					misshead = false;
					break packloop;
				}
			} // end packloop

			if (!gop)
			{ 
				if (data.length > 2)
				{
					vidbuf.write(data, 0, data.length - 3);
					packet.write(data, data.length - 3, 3);
					misshead = true;
				}
				else 
					vidbuf.write(data);
			}

		} catch (IOException e) { 

			Common.setExceptionMessage(e);
		}

		if (vidbuf.size() > 6144000)
		{
			vptsbytes.reset();
			vidbuf.reset(); 
			packet.reset();

			Common.setMessage(Resource.getString("demux.error.gop.toobig"));

			misshead = false;
			first = true;
		}
	}

	/**
	 * simply write the packet
	 */
	private int writePacket(byte[] packet) throws IOException
	{
		return writePacket(packet, 0, packet.length);
	}

	/**
	 * simply write the packet
	 */
	private int writePacket(byte[] packet, int offset, int length) throws IOException
	{
		if (offset < 0 || offset >= packet.length)
		{
			Common.setMessage("!> packet writing: index out of bounds, ignore it..");
			return 0;
		}

		if (offset + length > packet.length)
		{
			Common.setMessage("!> packet writing: length index out of bounds, shortened..");
			length = packet.length - offset;
		}

		out.write(packet, offset, length); 

		return length;
	}

}

