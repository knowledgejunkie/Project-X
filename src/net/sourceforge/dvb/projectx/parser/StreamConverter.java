/*
 * @(#)StreamConverter
 *
 * Copyright (c) 2005-2006 by dvb.matt, All Rights Reserved.
 * 
 * This file is part of ProjectX, a free Java based demux utility.
 * By the authors, ProjectX is intended for educational purposes only, 
 * as a non-commercial test project.
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

package net.sourceforge.dvb.projectx.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.io.File;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.PushbackInputStream;

import net.sourceforge.dvb.projectx.common.Resource;
import net.sourceforge.dvb.projectx.common.Common;
import net.sourceforge.dvb.projectx.common.Keys;
import net.sourceforge.dvb.projectx.common.JobProcessing;
import net.sourceforge.dvb.projectx.common.JobCollection;

import net.sourceforge.dvb.projectx.io.IDDBufferedOutputStream;
import net.sourceforge.dvb.projectx.thirdparty.TS;
import net.sourceforge.dvb.projectx.video.Video;
import net.sourceforge.dvb.projectx.parser.CommonParsing;
import net.sourceforge.dvb.projectx.parser.StreamDemultiplexer;

import net.sourceforge.dvb.projectx.xinput.XInputFile;


/**
 * create streams
 */
public class StreamConverter extends Object {

	private String FileName = "";

	private boolean FirstPacket = true;
	private boolean ptsover = false;
	private boolean ContainsVideo = false;
	private boolean BrokenLinkFlag = false;
	private boolean Debug = false;
	private boolean AddPcrToStream = false;
	private boolean ExportVideo = false;
	private boolean ExportNonVideo = false;
	private boolean MustStartWithVideo = false;
	private boolean SetMainAudioAc3 = false;
	private boolean GenerateTTX = false;
	private boolean GeneratePMT = false;
	private boolean PcrCounter = false;
	private boolean CreateVdrIndex = false;
	private boolean MuxPES = false;

	private byte[] PackHeader = { 0, 0, 1, (byte)0xBA, 0x44, 0, 4, 0, 4, 1, 0, (byte)0xEA, 0x63, (byte)0xF8 };  // 8000kbps
	private byte[] LeadingPackHeader = { 0, 0, 1, (byte)0xBA, 0x44, 0, 4, 0, 4, 1, 0, (byte)0xEA, 0x63, (byte)0xF8 };  // 8000kbps
	private byte[] SequenceEndCode = { 0, 0, 1, (byte)0xB9 };
	private byte[] TsStartPacketHeader = { 0x47, 0x40, 0, 0 };   // 1x = no adap, 3x adap follw
	private byte[] TsSubPacketHeader = { 0x47, 0, 0, 0 };   // 1x = no adap, 3x adap follw
	private byte[] PvaPacketHeader = { 0x41, 0x56, 0, 0, 0x55, 0, 0, 0 };
	private byte[] PvaPacketHeaderAndPTS = { 0x41, 0x56, 0, 0, 0x55, 0x10, 0, 0, 0, 0, 0, 0 };
	private byte[] SystemHeader = { 0, 0, 1, (byte)0xBB, 0, 0xC, (byte)0x80, (byte)0x9C, 0x41, 4, 0x21, 0x7F };  // 12+ byte std 1A, 1V  8Mbps (20000 * 400)
	private byte[][] sys = { 
		{ (byte)0xE0, (byte)0xE0, (byte)0xE0 },     // 224kb   0 MPV
		{ (byte)0xBD, (byte)0xC0, 0x20 },           // 4kb     1 pd_AC3
		{ (byte)0xC0, (byte)0xC0, 0x20 }          // 4kb     2 MPA
	};
	private byte[] subID = { (byte)0x80, 1, 0, 1 };       // std 0x80,1,0,1
	private byte[] sysID = { (byte)0xE0, (byte)0xC0 };  // ID adder mpv+mpa
	private byte[] adapt = { 0, 0 };
	private byte[] StuffingData = new byte[2324];

	private long[] time = new long[2];
	private long SCR_Value = 0;
	private long PmtCounter = 0;
	private long PCR_Delta = 65000;

	private final int PVA_MAINVIDEO = 1;
	private final int PVA_MAINAUDIO = 2;

	private int Action = 0;
	private int CutMode = 0;
	private int SystemHeaderInsertPoint = 26;
	private int SystemHeaderLength = 138;
	private int Packet = 0;
	private int TsHeaderMode = 0;

	private int[] PacketCounter = new int[70];       // pva counter , 1=stream1,2=stream2  4... rest
	private int[] SystemHeaderStreams = new int[2];        // number of audio[0], video[1] streams

	private List PaddingStreamPositions;
	private List IDs;

	private IDDBufferedOutputStream OutputStream;
	private ByteArrayOutputStream Buffer;

//
	private ArrayList remuxList = new ArrayList();


//
	public StreamConverter()
	{
		PaddingStreamPositions = new ArrayList();
		IDs = new ArrayList();
		Buffer = new ByteArrayOutputStream();
	}

	/**
	 *
	 */
	private void getSettings(JobCollection collection)
	{
		ExportVideo = collection.getSettings().getBooleanProperty(Keys.KEY_WriteOptions_writeVideo);
		ExportNonVideo = collection.getSettings().getBooleanProperty(Keys.KEY_WriteOptions_writeAudio);
		Debug = collection.getSettings().getBooleanProperty(Keys.KEY_DebugLog);
		PCR_Delta = collection.getSettings().getIntProperty(Keys.KEY_PcrDelta_Value);
		CutMode = collection.getSettings().getIntProperty(Keys.KEY_CutMode);
		AddPcrToStream = collection.getSettings().getBooleanProperty(Keys.KEY_Conversion_addPcrToStream);
		MustStartWithVideo = collection.getSettings().getBooleanProperty(Keys.KEY_Conversion_startWithVideo);
		SetMainAudioAc3 = collection.getSettings().getBooleanProperty(Keys.KEY_TS_setMainAudioAc3);
		GenerateTTX = collection.getSettings().getBooleanProperty(Keys.KEY_TS_generateTtx);
		GeneratePMT = collection.getSettings().getBooleanProperty(Keys.KEY_TS_generatePmt);
		TsHeaderMode = collection.getSettings().getIntProperty(Keys.KEY_TsHeaderMode);
		PcrCounter = collection.getSettings().getBooleanProperty(Keys.KEY_Conversion_PcrCounter);
		CreateVdrIndex = collection.getSettings().getBooleanProperty(Keys.KEY_ExternPanel_createVdrIndex);
	}

	/**
	 *
	 */
	public void init(JobCollection collection, String _name, int buffersize, int action, int filenumber)
	{
		FileName = _name;

		FirstPacket = true;
		ContainsVideo = false; 
		Action = action;
		BrokenLinkFlag = false;
		ptsover = false; 

		PmtCounter = 0; 
		SystemHeaderInsertPoint = 26;
		LeadingPackHeader[2] = 0; 
		time[0] = -1; 
		SCR_Value = 0;
		Packet = 0;

		getSettings(collection);

		Arrays.fill(StuffingData, (byte)0xFF);

		Buffer.reset();

		if (filenumber == 0)
		{
			IDs.clear();

			//prepare teletext insertion, at first file
			if (action == CommonParsing.ACTION_TO_TS)
				TS.buildTeletextStream(((XInputFile) collection.getInputFile(0)).toString());


			//prepare muxing of secondary pes at toVDR
			remuxList.clear();

			MuxPES = Common.getSettings().getBooleanProperty("HiddenKey.VDRExport.MuxPES", false);

			if (action == CommonParsing.ACTION_TO_VDR)
				scanSecondaryStreams(collection);
		}

		try { 

			OutputStream = new IDDBufferedOutputStream(new FileOutputStream(FileName), buffersize); 

			if (Action == CommonParsing.ACTION_TO_VDR && CreateVdrIndex)
				OutputStream.InitVdr(new File(FileName).getParent() + System.getProperty("file.separator") + "index.vdr", filenumber);

		} catch (IOException e) { 

			Common.setExceptionMessage(e);
		}
	}

	/**
	 * set broken link flag after a cut
	 */
	private void setBrokenLink(JobProcessing job_processing, byte[] pes_packet, int pes_offset)
	{
		if (CommonParsing.validateStartcode(pes_packet, pes_offset) < 0)
		{
			Common.setMessage("!> invalid start_code of packet (sbl), pos: " + job_processing.getLastHeaderBytePosition());
			return;
		}

		int pes_payloadlength = CommonParsing.getPES_LengthField(pes_packet, pes_offset);
		int pes_headerlength = 9;
		int pes_packetlength = pes_headerlength - 3 + pes_payloadlength;
		int pes_extensionlength = CommonParsing.getPES_ExtensionLengthField(pes_packet, pes_offset);

		for (int i = pes_headerlength + pes_extensionlength + pes_offset, j = pes_packetlength - 7 + pes_offset, returncode; i < j; )
		{
			if ((returncode = CommonParsing.validateStartcode(pes_packet, i)) < 0 || CommonParsing.getPES_IdField(pes_packet, i) != CommonParsing.GROUP_START_CODE)
			{
				i += returncode < 0 ? -returncode : 4;
				continue;
			}

			pes_packet[7 + i] |= 0x20;
			break;
		}

		BrokenLinkFlag = true; // even if it wasn't found
	}

	/**
	 * repack mpg1 to mpg2
	 */
	private void repackMpg1(byte[] pes_packet, int pes_offset, StreamDemultiplexer streamdemultiplexer)
	{
		if (streamdemultiplexer.getStreamType() != CommonParsing.MPEG1PS_TYPE)
			return;

		int pes_packetoffset = 6;
		int pes_headerlength = 9;
		int pes_extensionlength = 0;
		int pes_payloadlength = CommonParsing.getPES_LengthField(pes_packet, pes_offset);
		int pes_packetlength = pes_packetoffset + pes_payloadlength;
		int offset = pes_packetoffset;
		int value = 0x800000;

		skiploop:
		for (;;)
		{
			switch (0xC0 & pes_packet[offset + pes_offset])	
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

			switch (0x30 & pes_packet[offset + pes_offset])
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

		if (pes_extensionlength == 5)
			value = 0x808005;

		else if (pes_extensionlength == 10)
			value = 0x80C00A;

		System.arraycopy(pes_packet, offset + pes_offset, pes_packet, pes_headerlength + pes_offset, pes_packetlength - offset);

		CommonParsing.setValue(pes_packet, pes_packetoffset + pes_offset, 3, !CommonParsing.BYTEREORDERING, value);
		CommonParsing.setPES_LengthField(pes_packet, pes_offset, pes_packetlength - offset + 3);
	}

	/**
	 * 
	 */
	private void setSCRField(byte[] packet_header)
	{
		if (Debug) 
			System.out.println("SCR_Value " + SCR_Value);

		if (!AddPcrToStream)
			return;

		packet_header[4] = (byte)(0x44 | (0x3 & (SCR_Value>>>28)) | (0x38 & (SCR_Value>>>27)));
		packet_header[5] = (byte)(0xFF & (SCR_Value>>>20));
		packet_header[6] = (byte)(0x4 | (0x3 & (SCR_Value>>>13)) | (0xF8 & (SCR_Value>>>12)));
		packet_header[7] = (byte)(0xFF & (SCR_Value>>>5));
		packet_header[8] = (byte)(0x4 | (0xF8 & (SCR_Value<<3)));
	}

	/**
	 * 
	 */
	private void updateSCR(int length)
	{
		SCR_Value += (length * 8) / 150;
	}

	/**
	 * 
	 */
	private void updateSCR(long value)
	{
		SCR_Value += value;
	}

	/**
	 * entry point and pre functions
	 */
	public void write(JobProcessing job_processing, byte[] pes_packet, StreamDemultiplexer streamdemultiplexer, long cutposition, boolean qinfo, List cutpoints) throws IOException
	{
		write(job_processing, pes_packet, 0, pes_packet.length, streamdemultiplexer, cutposition, qinfo, cutpoints);
	}

	/**
	 * entry point and pre functions
	 */
	public void write(JobProcessing job_processing, byte[] pes_packet, int pes_offset, StreamDemultiplexer streamdemultiplexer, long cutposition, boolean qinfo, List cutpoints) throws IOException
	{
		write(job_processing, pes_packet, pes_offset, pes_packet.length, streamdemultiplexer, cutposition, qinfo, cutpoints);
	}

	/**
	 * entry point and pre functions
	 */
	public void write(JobProcessing job_processing, byte[] pes_packet, int pes_offset, int pes_packetlength, StreamDemultiplexer streamdemultiplexer, long cutposition, boolean qinfo, List cutpoints) throws IOException
	{
		/** 
		 * cut-off determination
		 */
		if (CutMode == CommonParsing.CUTMODE_BYTE && !qinfo && !CommonParsing.makecut(job_processing, cutposition + 5, cutpoints) )
		{
			BrokenLinkFlag = false;
			return;
		}

		/** 
		 * 1:1 copy filter
		 */
		if (Action == CommonParsing.ACTION_FILTER)
		{
			writePacket(job_processing, pes_packet, pes_offset, pes_packetlength);
			return;
		}

		/** 
		 * repack mpg1 pes source to mpg2
		 */
		repackMpg1(pes_packet, pes_offset, streamdemultiplexer);

		if (streamdemultiplexer.getType() == CommonParsing.MPEG_VIDEO && !BrokenLinkFlag)
			setBrokenLink(job_processing, pes_packet, pes_offset);

		if (Buffer.size() != 0)
			Buffer.reset();

		switch(Action)
		{
		case CommonParsing.ACTION_TO_VDR:
			packetizeToVDR(job_processing, pes_packet, pes_offset, streamdemultiplexer);
			return;

		case CommonParsing.ACTION_TO_M2P:
			packetizeToM2P(job_processing, pes_packet, pes_offset, streamdemultiplexer);
			return;

		case CommonParsing.ACTION_TO_PVA:
			packetizeToPVA(job_processing, pes_packet, pes_offset, streamdemultiplexer);
			return;

		case CommonParsing.ACTION_TO_TS:
			packetizeToTS(job_processing, pes_packet, pes_offset, streamdemultiplexer, qinfo);
			return;
		}
	}

	/**
	 * simply write the packet
	 */
	public void writePacket(JobProcessing job_processing, byte[] packet) throws IOException
	{
		writePacket(job_processing, packet, 0, packet.length);
	}

	/**
	 * simply write the packet
	 */
	public void writePacket(JobProcessing job_processing, byte[] packet, int offset, int length) throws IOException
	{
		if (offset < 0 || offset >= packet.length)
		{
			Common.setMessage("!> packet writing: index out of bounds, ignore it.. (" + Packet + ")");
			return;
		}

		if (offset + length > packet.length)
		{
			Common.setMessage("!> packet writing: length index out of bounds, shortened.. (" + Packet + ")");
			length = packet.length - offset;
		}

		OutputStream.write(packet, offset, length); 

		job_processing.countMediaFilesExportLength(length);
		job_processing.countAllMediaFilesExportLength(length);
	}

	/**
	 * export VDR 
	 */
	private void packetizeToVDR(JobProcessing job_processing, byte[] pes_packet, int pes_offset, StreamDemultiplexer streamdemultiplexer)
	{
		try {
			int es_streamtype = streamdemultiplexer.getType();
			int pes_streamtype = streamdemultiplexer.getStreamType();
			int newID = streamdemultiplexer.getnewID();

			if (CommonParsing.validateStartcode(pes_packet, pes_offset) < 0)
			{
				Common.setMessage("!> invalid startcode " + Packet + ", pos: " + job_processing.getLastHeaderBytePosition() + " (" + Integer.toHexString(newID) + "/" + es_streamtype + ")");
				return;
			}

			int pes_payloadlength = CommonParsing.getPES_LengthField(pes_packet, pes_offset);
			int pes_headerlength = 9;
			int pes_packetlength = pes_headerlength - 3 + pes_payloadlength;
			int offset = 0;

			if (pes_packetlength + pes_offset > pes_packet.length)
				return;

			int pes_extensionlength = CommonParsing.getPES_ExtensionLengthField(pes_packet, pes_offset);

			boolean containsPTS = CommonParsing.clearBit33ofPTS(pes_packet, pes_offset);
			CommonParsing.clearBit33ofDTS(pes_packet, pes_offset);

			switch (es_streamtype)
			{
			case CommonParsing.AC3_AUDIO:
			case CommonParsing.DTS_AUDIO:
				if (!ExportNonVideo || (0xFF & newID) != 0x80)
					return;   // not more than one stream

				if (pes_streamtype == CommonParsing.MPEG2PS_TYPE)
				{
					// skip substream-header
					pes_payloadlength -= 4;

					CommonParsing.setPES_LengthField(pes_packet, pes_offset, pes_payloadlength);

					offset = pes_headerlength + pes_extensionlength;

					writePacket(job_processing, pes_packet, pes_offset, offset); // write leading hader

					offset += 4;
				}

				break;

			case CommonParsing.MPEG_AUDIO:
				if (!ExportNonVideo)
					return;

				CommonParsing.setPES_IdField(pes_packet, pes_offset, newID);

				break;

			case CommonParsing.TELETEXT:
				if (!ExportNonVideo || (0x7F & newID) > 0x1F)
					return;   // not more than 16 streams

				CommonParsing.setPES_SubIdField(pes_packet, pes_offset, pes_headerlength, pes_extensionlength, newID - 0x80);

				break;

			case CommonParsing.MPEG_VIDEO:
				if (!ExportVideo)
					return;

				CommonParsing.setPES_IdField(pes_packet, pes_offset, newID);

				//pes remux
				if (containsPTS)
					remuxPES(job_processing, CommonParsing.getPTSfromBytes(pes_packet, pes_headerlength + pes_offset));

				break;

			case CommonParsing.LPCM_AUDIO:
			case CommonParsing.SUBPICTURE:
			default:
				return;
			}

			writePacket(job_processing, pes_packet, offset + pes_offset, pes_packetlength - offset); 

		} catch (IOException e) { 

			Common.setExceptionMessage(e);
		}
	}

	/**
	 * write MPG 
	 */
	private void packetizeToM2P(JobProcessing job_processing, byte[] pes_packet, int pes_offset, StreamDemultiplexer streamdemultiplexer)
	{
		try {

			int es_streamtype = streamdemultiplexer.getType();
			int pes_streamtype = streamdemultiplexer.getStreamType();
			int newID = streamdemultiplexer.getnewID();

			if (CommonParsing.validateStartcode(pes_packet, pes_offset) < 0)
			{
				Common.setMessage("!> invalid startcode " + Packet + ", pos: " + job_processing.getLastHeaderBytePosition() + " (" + Integer.toHexString(newID) + "/" + es_streamtype + ")");
				return;
			}

			int pes_payloadlength = CommonParsing.getPES_LengthField(pes_packet, pes_offset);
			int pes_headerlength = 9;
			int pes_packetlength = pes_headerlength - 3 + pes_payloadlength;
			int offset = 0;

			if (pes_packetlength + pes_offset > pes_packet.length)
				return;

			int pes_extensionlength = CommonParsing.getPES_ExtensionLengthField(pes_packet, pes_offset);

			boolean newSCR = false;
			boolean containsPTS;

			containsPTS = CommonParsing.clearBit33ofPTS(pes_packet, pes_offset);
			CommonParsing.clearBit33ofDTS(pes_packet, pes_offset);

			switch (es_streamtype)
			{
			case CommonParsing.AC3_AUDIO:
			case CommonParsing.DTS_AUDIO:
				if (!ExportNonVideo)
					return;

				break;

			case CommonParsing.MPEG_AUDIO:
				if (!ExportNonVideo)
					return;

				CommonParsing.setPES_IdField(pes_packet, pes_offset, newID);

				break;

			case CommonParsing.MPEG_VIDEO:
				if (!ExportVideo || (0xFF & newID) != 0xE0) // not more than one stream
					return;

				CommonParsing.setPES_IdField(pes_packet, pes_offset, newID);

				break;

			case CommonParsing.LPCM_AUDIO:
			case CommonParsing.SUBPICTURE:
			case CommonParsing.TELETEXT:
			default:
				return;
			}


			/**
			 * return, newID is not set, for mpg (curr. 0x20..,0xA0..)
			 */
			if (newID == 0) 
				return;

//  indexof,
			int a = 0;

			/**
			 * new ID arrived, align data
			 */
			for ( ; a < IDs.size(); a++)
			{
				if (newID == (0xFF & Integer.parseInt(IDs.get(a).toString()))) 
					break;
			}

			if (a == IDs.size())
			{
				if (es_streamtype != CommonParsing.MPEG_VIDEO && !ContainsVideo && MustStartWithVideo) 
					return;

				if (!CommonParsing.alignSyncword(pes_packet, pes_offset, es_streamtype))
					return;

				pes_payloadlength = CommonParsing.getPES_LengthField(pes_packet, pes_offset);
				pes_packetlength = pes_headerlength - 3 + pes_payloadlength;

				IDs.add(String.valueOf((newID > 0xDF) ? newID : (0x100 + newID)));
			}
//
			if (FirstPacket)
			{
				writePacket(job_processing, PackHeader);
				writePacket(job_processing, new byte[SystemHeaderLength - 8]);  // placeholder for systemheader

				FirstPacket = false;
			}

			if (es_streamtype == CommonParsing.AC3_AUDIO || es_streamtype == CommonParsing.DTS_AUDIO)
			{
				if (pes_streamtype == CommonParsing.MPEG2PS_TYPE)
				{
					CommonParsing.setPES_SubIdField(pes_packet, pes_offset, pes_headerlength, pes_extensionlength, newID);

					updateSCR(pes_packetlength);
					setSCRField(PackHeader);

					writePacket(job_processing, PackHeader);
					writePacket(job_processing, pes_packet, pes_offset, pes_packetlength);

					return;
				}
				else
				{
					pes_payloadlength += 4;
					pes_packetlength += 4;
					offset = pes_headerlength + pes_extensionlength;

					CommonParsing.setPES_LengthField(pes_packet, pes_offset, pes_payloadlength);
					CommonParsing.setValue(subID, pes_offset, 1, !CommonParsing.BYTEREORDERING, newID);

					updateSCR(pes_packetlength);
					setSCRField(PackHeader);

					writePacket(job_processing, PackHeader);
					writePacket(job_processing, pes_packet, pes_offset, offset);
					writePacket(job_processing, subID, pes_offset, subID.length); // write 4 byte of substreamheader
					writePacket(job_processing, pes_packet, offset + pes_offset, pes_packetlength - offset - subID.length);

					return;
				}
			}

			if (!ContainsVideo)
			{
				if (es_streamtype == CommonParsing.MPEG_VIDEO) 
					ContainsVideo = true;

				else 
					PaddingStreamPositions.add(String.valueOf(job_processing.getAllMediaFilesExportLength() + 17));
			}

			if (es_streamtype == CommonParsing.MPEG_VIDEO && containsPTS && AddPcrToStream)
			{
				for (int i = pes_headerlength + pes_extensionlength + pes_offset, picture_type, returncode; i < pes_packetlength - 6 + pes_offset; i++)
				{
					/**
					 * search picture header
					 */
					if ((returncode = CommonParsing.validateStartcode(pes_packet, i)) < 0 || CommonParsing.getPES_IdField(pes_packet, i) != CommonParsing.PICTURE_START_CODE)
					{
						i += (returncode < 0 ? -returncode : 4) - 1;
						continue;
					}

					picture_type = 7 & pes_packet[i + 5]>>>3;

					/**
					 * is I or P-Frame
					 */
				//	if (picture_type == CommonParsing.FRAME_I_TYPE || picture_type == CommonParsing.FRAME_P_TYPE)
					if (picture_type == CommonParsing.FRAME_I_TYPE)
					{
						SCR_Value = CommonParsing.getPTSfromBytes(pes_packet, pes_headerlength + pes_offset);

						updateSCR(-PCR_Delta);
						setSCRField(PackHeader);

						if (LeadingPackHeader[2] == 0) 
							System.arraycopy(PackHeader, 0, LeadingPackHeader, 0, PackHeader.length);

						writePacket(job_processing, PackHeader);

						newSCR = true;
					}

					break;
				}
			}

			if (!newSCR)
			{
				updateSCR(pes_packetlength);
				setSCRField(PackHeader);

				writePacket(job_processing, PackHeader);
			}

			writePacket(job_processing, pes_packet, pes_offset, pes_packetlength);

		} catch (IOException e) { 

			Common.setExceptionMessage(e);
		}
	}

	/**
	 * write pva 
	 */
	private void packetizeToPVA(JobProcessing job_processing, byte[] pes_packet, int pes_offset, StreamDemultiplexer streamdemultiplexer)
	{
		try {

			long pts;

			int es_streamtype = streamdemultiplexer.getType();
			int pes_streamtype = streamdemultiplexer.getStreamType();
			int newID = streamdemultiplexer.getnewID();       // read new mapped ID

			if (CommonParsing.validateStartcode(pes_packet, pes_offset) < 0)
			{
				Common.setMessage("!> invalid startcode " + Packet + ", pos: " + job_processing.getLastHeaderBytePosition() + " (" + Integer.toHexString(newID) + "/" + es_streamtype + ")");
				return;
			}

			int pes_payloadlength = CommonParsing.getPES_LengthField(pes_packet, pes_offset);
			int pes_headerlength = 9;
			int pes_packetlength = pes_headerlength - 3 + pes_payloadlength;
			int offset = 0;
			int countID = 3;

			if (pes_packetlength + pes_offset > pes_packet.length)
				return;

			int pes_extensionlength = CommonParsing.getPES_ExtensionLengthField(pes_packet, pes_offset);

			boolean containsPTS;

			containsPTS = CommonParsing.clearBit33ofPTS(pes_packet, pes_offset);
			CommonParsing.clearBit33ofDTS(pes_packet, pes_offset);

			switch (es_streamtype)
			{
			case CommonParsing.AC3_AUDIO:
			case CommonParsing.DTS_AUDIO:
			case CommonParsing.TELETEXT:
				if (!ExportNonVideo)
					return;

				break;

			case CommonParsing.MPEG_AUDIO:
				if (!ExportNonVideo)
					return;

				CommonParsing.setPES_IdField(pes_packet, pes_offset, newID);

				break;

			case CommonParsing.MPEG_VIDEO:
				if (!ExportVideo || (0xFF & newID) != 0xE0) // not more than one video
					return;

				/**
				 * looking for sequence_start_code
				 */
				for (int i = pes_headerlength + pes_extensionlength + pes_offset; !ContainsVideo && i < pes_packetlength - 4 + pes_offset; i++)
				{
					if (pes_packet[i] != 0 || pes_packet[1 + i] != 0 || pes_packet[2 + i] != 1 || pes_packet[3 + i] != (byte)CommonParsing.SEQUENCE_HEADER_CODE) 
						continue;

					ContainsVideo = true;
				}

				if (MustStartWithVideo && !ContainsVideo) 
					return; 

				offset = pes_headerlength;

				CommonParsing.setPES_IdField(pes_packet, pes_offset, newID);

				if (containsPTS)
				{
					pts = CommonParsing.getPTSfromBytes(pes_packet, offset + pes_offset);

					offset += pes_extensionlength;
					pes_payloadlength = pes_packetlength - offset; 

					CommonParsing.setValue(PvaPacketHeaderAndPTS, 2, 1, !CommonParsing.BYTEREORDERING, PVA_MAINVIDEO);
					CommonParsing.setValue(PvaPacketHeaderAndPTS, 3, 1, !CommonParsing.BYTEREORDERING, 0xFF & PacketCounter[PVA_MAINVIDEO]);
					CommonParsing.setValue(PvaPacketHeaderAndPTS, 6, 2, !CommonParsing.BYTEREORDERING, pes_payloadlength + 4);
					CommonParsing.setValue(PvaPacketHeaderAndPTS, 8, 4, !CommonParsing.BYTEREORDERING, pts);

					writePacket(job_processing, PvaPacketHeaderAndPTS);
				}

				else
				{
					offset += pes_extensionlength;
					pes_payloadlength = pes_packetlength - offset; 

					CommonParsing.setValue(PvaPacketHeader, 2, 1, !CommonParsing.BYTEREORDERING, PVA_MAINVIDEO);
					CommonParsing.setValue(PvaPacketHeader, 3, 1, !CommonParsing.BYTEREORDERING, 0xFF & PacketCounter[PVA_MAINVIDEO]);
					CommonParsing.setValue(PvaPacketHeader, 5, 1, !CommonParsing.BYTEREORDERING, 0);
					CommonParsing.setValue(PvaPacketHeader, 6, 2, !CommonParsing.BYTEREORDERING, pes_payloadlength);

					writePacket(job_processing, PvaPacketHeader);
				}

				writePacket(job_processing, pes_packet, offset + pes_offset, pes_payloadlength);

				PacketCounter[PVA_MAINVIDEO]++;

				return;

			case CommonParsing.LPCM_AUDIO:
			case CommonParsing.SUBPICTURE:
			default:
				return;
			}

			if (MustStartWithVideo && !ContainsVideo) 
				return; 

			switch (newID)
			{
			case 0xC0: //ändern
				CommonParsing.setValue(PvaPacketHeader, 2, 1, !CommonParsing.BYTEREORDERING, PVA_MAINAUDIO);
				CommonParsing.setValue(PvaPacketHeader, 3, 1, !CommonParsing.BYTEREORDERING, 0xFF & PacketCounter[PVA_MAINAUDIO]);
				CommonParsing.setValue(PvaPacketHeader, 5, 1, !CommonParsing.BYTEREORDERING, 0x10);
				CommonParsing.setValue(PvaPacketHeader, 6, 2, !CommonParsing.BYTEREORDERING, pes_packetlength);

				PacketCounter[PVA_MAINAUDIO]++;

				writePacket(job_processing, PvaPacketHeader);
				writePacket(job_processing, pes_packet, pes_offset, pes_packetlength);

				return;

			default: //ändern
				switch (0xF0 & newID)
				{
				case 0x90: //ttx ändern
					countID = newID - 0x8C; 
					break; 

				case 0x80:
					countID = newID - 0x6C; 
					break; 

				case 0xC0:
				case 0xD0:
					countID = newID - 0x9C; 
				}

				CommonParsing.setValue(PvaPacketHeader, 2, 1, !CommonParsing.BYTEREORDERING, newID);
				CommonParsing.setValue(PvaPacketHeader, 3, 1, !CommonParsing.BYTEREORDERING, 0xFF & PacketCounter[countID]);
				CommonParsing.setValue(PvaPacketHeader, 5, 1, !CommonParsing.BYTEREORDERING, 0x10);

				PacketCounter[countID]++;

				if (es_streamtype == CommonParsing.AC3_AUDIO || es_streamtype == CommonParsing.DTS_AUDIO)
				{
					if (pes_streamtype == CommonParsing.MPEG2PS_TYPE)
					{
						// skip substream-header
						pes_payloadlength -= 4;

						CommonParsing.setPES_LengthField(pes_packet, pes_offset, pes_payloadlength);
						CommonParsing.setValue(PvaPacketHeader, 6, 2, !CommonParsing.BYTEREORDERING, pes_packetlength - 4);

						offset = pes_headerlength + pes_extensionlength;

						writePacket(job_processing, PvaPacketHeader);
						writePacket(job_processing, pes_packet, pes_offset, offset);

						offset += 4;

						writePacket(job_processing, pes_packet, offset + pes_offset, pes_packetlength - offset);

						return;
					}
				}

				CommonParsing.setValue(PvaPacketHeader, 6, 2, !CommonParsing.BYTEREORDERING, pes_packetlength);

				writePacket(job_processing, PvaPacketHeader);
				writePacket(job_processing, pes_packet, pes_offset, pes_packetlength);
			}

		} catch (IOException e) { 

			Common.setExceptionMessage(e);
		}
	}


	/**
	 * mpeg-ts 
	 */
	private void packetizeToTS(JobProcessing job_processing, byte[] pes_packet, int pes_offset, StreamDemultiplexer streamdemultiplexer, boolean qinfo)
	{
		try {
			boolean pcr = false;

			boolean ttx_pts_check = false;  //search for frame pts for ttx insertion

			long pcrbase = 0;
			long pts = 0;

			int es_streamtype = streamdemultiplexer.getType();
			int pes_streamtype = streamdemultiplexer.getStreamType();
			int newID = streamdemultiplexer.getnewID();

			if (CommonParsing.validateStartcode(pes_packet, pes_offset) < 0)
			{
				Common.setMessage("!> invalid startcode " + Packet + ", pos: " + job_processing.getLastHeaderBytePosition() + " (" + Integer.toHexString(newID) + "/" + es_streamtype + ")");
				return;
			}

			int pes_payloadlength = CommonParsing.getPES_LengthField(pes_packet, pes_offset);
			int pes_headerlength = 9;
			int pes_packetlength = pes_headerlength - 3 + pes_payloadlength;
			int offset = 0;
			int countID = 3;

			if (pes_packetlength + pes_offset > pes_packet.length)
				return;

			int pes_extensionlength = CommonParsing.getPES_ExtensionLengthField(pes_packet, pes_offset);

			boolean containsPTS;

			containsPTS = CommonParsing.clearBit33ofPTS(pes_packet, pes_offset);
			CommonParsing.clearBit33ofDTS(pes_packet, pes_offset);

			switch (es_streamtype)
			{
			case CommonParsing.AC3_AUDIO:
			case CommonParsing.DTS_AUDIO:
				if (!ExportNonVideo)
					return;

				break;

			case CommonParsing.MPEG_AUDIO:
				if (!ExportNonVideo)
					return;

				break;

			case CommonParsing.TELETEXT:
				if (!ExportNonVideo)
					return;

				break;

			case CommonParsing.MPEG_VIDEO:
				if (!ExportVideo || (0xFF & newID) != 0xE0) // not more than one stream
					return;

				break;
//
			case CommonParsing.SUBPICTURE:
				if (!ExportNonVideo || !Common.getSettings().getBooleanProperty("HiddenKey.TSExport.Subpicture", false))
					return;

				break;
//
			case CommonParsing.LPCM_AUDIO:
			default:
				return;
			}


			if (!qinfo)
				if (es_streamtype != CommonParsing.MPEG_VIDEO && !ContainsVideo && MustStartWithVideo) 
					return; // must start with video  

			if (es_streamtype == CommonParsing.AC3_AUDIO || es_streamtype == CommonParsing.DTS_AUDIO)
			{
				if (pes_streamtype == CommonParsing.MPEG2PS_TYPE)
				{
					// skip substream-header
					offset = pes_headerlength + pes_extensionlength + 4;

				//	System.arraycopy(pes_packet, pes_headerlength + pes_extensionlength + pes_offset, pes_packet, offset + pes_offset, pes_packetlength - offset);
					System.arraycopy(pes_packet, offset + pes_offset, pes_packet, pes_headerlength + pes_extensionlength + pes_offset, pes_packetlength - offset);

					pes_payloadlength -= 4;
					pes_packetlength -= 4;

					CommonParsing.setPES_LengthField(pes_packet, pes_offset, pes_payloadlength);
				}
			}

//
			int a = 0;

			/**
			 * new ID arrived, save for PMT
			 */
			for (; a < IDs.size(); a++)
			{
				if (newID == (0xFF & Integer.parseInt(IDs.get(a).toString()))) 
					break;
			}

			if (a == IDs.size()) //make adaptions of 1st paket
			{
				int newID2 = newID;

				//sort the id's for PMT table, MPV has highest prior.
				if (newID2 < 0x90) //AC3 DTS SUP
					newID2 |= 0x200;

				if (newID2 < 0xC0) //TTX PCM
					newID2 |= 0x300;

				if (newID2 < 0xE0) //MPA
					newID2 |= 0x100;

				if (!qinfo) // align syncwords
				{
			//		if (es_streamtype != CommonParsing.TELETEXT)
					if (es_streamtype != CommonParsing.TELETEXT && es_streamtype != CommonParsing.SUBPICTURE)
					{
						if (!CommonParsing.alignSyncword(pes_packet, pes_offset, es_streamtype))
							return;
					}

					pes_payloadlength = CommonParsing.getPES_LengthField(pes_packet, pes_offset);
					pes_packetlength = pes_headerlength - 3 + pes_payloadlength;
				}

				IDs.add(String.valueOf(newID2));
			}
//

			CommonParsing.setValue(TsStartPacketHeader, 2, 1, !CommonParsing.BYTEREORDERING, newID);
			CommonParsing.setValue(TsSubPacketHeader, 2, 1, !CommonParsing.BYTEREORDERING, newID);

			if (es_streamtype == CommonParsing.MPEG_VIDEO) 
				CommonParsing.setPES_LengthField(pes_packet, pes_offset, 0);


			/** 
			 * add TS header
			 */
			if (FirstPacket)
			{
				Buffer.write(TS.init( job_processing, FileName, SetMainAudioAc3, GenerateTTX, TsHeaderMode));
				FirstPacket = false; 
			}


			if (containsPTS && TS.getfirstID() == (0xFF & newID))
			{
				pts = CommonParsing.getPTSfromBytes(pes_packet, pes_headerlength + pes_offset);

				pcrbase = pts - PCR_Delta;

				if ( (pts & 0xFF000000L) == 0xFF000000L ) 
					ptsover = true;

				if (ptsover && pts < 0xF0000000L) 
					pts |= 0x100000000L;

				time[1] = pts;
				pcr = true;

				if (time[0] == -1) 
					time[0] = time[1];
			}

			switch (0xF0 & newID) //ändern
			{
			case 0x80:
				countID = newID - 0x6C; 
				break; 

			case 0x90:
				countID = newID - 0x8C; 
				break; 

			case 0xC0:
			case 0xD0:
				countID = newID - 0x9C; 
				break; 

			case 0xE0:
				countID = 1;

				if (containsPTS)
				{
					pcr = false;

					for (int i = pes_headerlength + pes_extensionlength + pes_offset, picture_type, returncode; i < pes_packetlength - 6 + pes_offset; i++)
					{
						/**
						 * search picture header
						 */
						if ((returncode = CommonParsing.validateStartcode(pes_packet, i)) < 0 || CommonParsing.getPES_IdField(pes_packet, i) != CommonParsing.PICTURE_START_CODE)
						{
							i += (returncode < 0 ? -returncode : 4) - 1;
							continue;
						}

						picture_type = 7 & pes_packet[i + 5]>>>3 ;

						/**
						 * is I or P-Frame
						 */
						//if (picture_type == CommonParsing.FRAME_I_TYPE || picture_type == CommonParsing.FRAME_P_TYPE)
						if (picture_type == CommonParsing.FRAME_I_TYPE)
						{
							pcr = true; 
							ContainsVideo = true; 
						}

						if (picture_type == CommonParsing.FRAME_I_TYPE || picture_type == CommonParsing.FRAME_P_TYPE)
							ttx_pts_check = true; //work with I+P+B-frames for ttx pts check

						break;
					}
				}

				break;
			}

			if (es_streamtype == CommonParsing.MPEG_VIDEO && !ContainsVideo && MustStartWithVideo) 
				return; // must start with video, reached on qinfo

			if (30000L * PmtCounter <= job_processing.getAllMediaFilesExportLength())
			{
				Buffer.write(TS.getPAT());

				if (GeneratePMT) 
					Buffer.write(TS.getAutoPMT());

				else 
					Buffer.write(TS.getPMT());

				PmtCounter++;
			}

			/** 
			 * add own TTX
			 */
			//if (pcr && GeneratePMT && GenerateTTX)
			//	Buffer.write( TS.getTTX(pes_packet, pes_offset, Common.formatTime_1((pcrbase + PCR_Delta) / 90)) );

			//TTX sub insertion- from 0.90.4.00b28
			if (GenerateTTX && ttx_pts_check)
			{
				Buffer.write(TS.getTeletextStream(time[1]));
			}

			/** 
			 * add Pcr
			 */
			if (pcr && AddPcrToStream)
			{
				if (!PcrCounter) 
					Buffer.write(TS.getPCR(pcrbase, PacketCounter[countID], (0xFF & newID))); // no payload==no counter++

				else 
					Buffer.write(TS.getPCR(pcrbase, ++PacketCounter[countID], (0xFF & newID)));
			}

			int i = 0;
			int stuffinglength;

			while (i < pes_packetlength)
			{
				if (i == 0)
				{
					if (pes_packetlength - i < 183)
					{
						TsStartPacketHeader[3] = (byte)(0x30 | (0xF & ++PacketCounter[countID]));
						stuffinglength = 182 - (pes_packetlength - i);
						adapt[0] = (byte)(stuffinglength + 1);

						Buffer.write(TsStartPacketHeader); 
						Buffer.write(adapt); 
						Buffer.write(StuffingData, 6, stuffinglength);
						Buffer.write(pes_packet, i + pes_offset, pes_packetlength - i); 

						i += (pes_packetlength - i);
					}

					else if (pes_packetlength - i == 184)
					{
						TsStartPacketHeader[3] = (byte)(0x10 | (0xF & ++PacketCounter[countID]));

						Buffer.write(TsStartPacketHeader); 
						Buffer.write(pes_packet, i + pes_offset, 184); 

						i += 184;
					}

					else if (pes_packetlength - i < 185)
					{
						TsStartPacketHeader[3] = (byte)(0x30 | (0xF & ++PacketCounter[countID]));
						adapt[0] = 1;

						Buffer.write(TsStartPacketHeader); 
						Buffer.write(adapt); 
						Buffer.write(pes_packet, i + pes_offset, 182); 

						i += 182;
					}

					else
					{
						TsStartPacketHeader[3] = (byte)(0x10 | (0xF & ++PacketCounter[countID]));

						Buffer.write(TsStartPacketHeader); 
						Buffer.write(pes_packet, i + pes_offset, 184); 

						i += 184;
					}
				}

				else if (pes_packetlength - i < 183)
				{
					TsSubPacketHeader[3] = (byte)(0x30 | (0xF & ++PacketCounter[countID]));
					stuffinglength = 182 - (pes_packetlength - i);
					adapt[0] = (byte)(stuffinglength + 1);

					Buffer.write(TsSubPacketHeader); 
					Buffer.write(adapt); 
					Buffer.write(StuffingData, 6, stuffinglength);
					Buffer.write(pes_packet, i + pes_offset, pes_packetlength - i); 

					i += (pes_packetlength - i);
				}

				else if (pes_packetlength - i == 184)
				{
					TsSubPacketHeader[3] = (byte)(0x10 | (0xF & ++PacketCounter[countID]));

					Buffer.write(TsSubPacketHeader); 
					Buffer.write(pes_packet, i + pes_offset, 184); 

					i += 184;
				}

				else if (pes_packetlength - i < 185)
				{
					TsSubPacketHeader[3] = (byte)(0x30 | (0xF & ++PacketCounter[countID]));
					adapt[0] = 1;

					Buffer.write(TsSubPacketHeader); 
					Buffer.write(adapt); 
					Buffer.write(pes_packet, i + pes_offset, 182); 

					i += 182;
				}

				else
				{
					TsSubPacketHeader[3] = (byte)(0x10 | (0xF & ++PacketCounter[countID]));

					Buffer.write(TsSubPacketHeader); 
					Buffer.write(pes_packet, i + pes_offset, 184); 

					i += 184;
				}

				job_processing.countMediaFilesExportLength(+188);
				job_processing.countAllMediaFilesExportLength(+188);
			}

			Buffer.writeTo(OutputStream);
			Buffer.reset();

		} catch (IOException e) { 

			Common.setExceptionMessage(e);
		}
	}


	public void close(JobProcessing job_processing, boolean qinfo)
	{ 
		try {

			if (Action == CommonParsing.ACTION_TO_M2P)
				writePacket(job_processing, SequenceEndCode);

			OutputStream.flush(); 
			OutputStream.close();

			switch (Action)
			{
			case CommonParsing.ACTION_TO_M2P:
				RandomAccessFile mpg = new RandomAccessFile(FileName, "rw");

				if (LeadingPackHeader[2] == 1)
				{
					mpg.seek(0);
					mpg.write(LeadingPackHeader);
				}

				mpg.seek(SystemHeaderInsertPoint);

				Object[] ID = IDs.toArray(); 
				Arrays.sort(ID);     // write systemheader pointer stuff

				for (int i = 0; i < ID.length; i++)
				{
					int IDt = (0xFF & Integer.parseInt(ID[i].toString()));

					if ((0xF0 & IDt) == 0xE0)
					{ 
						sys[0][0] = (byte)(sysID[0]++); 

						mpg.write(sys[0]); 

						SystemHeaderInsertPoint += 3; 
						SystemHeaderStreams[1]++; 
					}

					else if ((0xF0 & IDt) == 0x80)
					{ 
						mpg.write(sys[1]); 

						SystemHeaderInsertPoint += 3; 
						SystemHeaderStreams[0]++; 
					}

					else if ((0xE0 & IDt) == 0xC0)
					{ 
						sys[2][0] = (byte)(sysID[1]++); 

						mpg.write(sys[2]); 

						SystemHeaderInsertPoint += 3; 
						SystemHeaderStreams[0]++; 
					}
				}

				/**** add padding ***/
				mpg.writeInt(0x100 | CommonParsing.PADDING_STREAM_CODE);
				mpg.writeShort((short)(0x8A - SystemHeaderInsertPoint));
				mpg.write(StuffingData, 0, (0x8A - SystemHeaderInsertPoint));

				int syslen = ((SystemHeaderStreams[0] + SystemHeaderStreams[1]) * 3) + 6;      // IDs (3byte) + std

				CommonParsing.setPES_LengthField(SystemHeader, 0, syslen);

				SystemHeader[9] = (byte)(SystemHeaderStreams[0]<<2);               // set number of audios
				SystemHeader[10] = (byte)(0x20 | (0x1F & SystemHeaderStreams[1]));   // set number of videos

				mpg.seek(14);
				mpg.write(SystemHeader);      // write system header

				for (int i = 0; ContainsVideo && MustStartWithVideo && i < PaddingStreamPositions.size(); i++)
				{
					mpg.seek(Long.parseLong(PaddingStreamPositions.get(i).toString()));
					mpg.write((byte)CommonParsing.PADDING_STREAM_CODE);
				}

				mpg.close();

				break;
			
			case CommonParsing.ACTION_TO_TS:
				if (qinfo && GeneratePMT)
					TS.setPmtPids(IDs);

				FileName = TS.updateAdditionalHeader(FileName, time, TsHeaderMode, job_processing);

				break;

			case CommonParsing.ACTION_TO_VDR:
				if (CreateVdrIndex)
				{
					if (new File(FileName).length() < 150)
						OutputStream.deleteIdd();

					else
						FileName = OutputStream.renameVdrTo(new File(FileName).getParent() + System.getProperty("file.separator"), FileName);
				}
			}


			if (new File(FileName).length() < 150) 
				new File(FileName).delete();

			else
			{ 
				Common.setMessage(Resource.getString("msg.newfile") + " " + FileName);
				job_processing.addSummaryInfo(Resource.getString("StreamConverter.Summary") + "\t'" + FileName + "'");
			}

		} catch (IOException e) { 

			Common.setExceptionMessage(e);
		}
	} 

////
	/**
	 * scan secondary streams for timestamps
	 */
	private void scanSecondaryStreams(JobCollection collection)
	{ 
		if (!MuxPES)
			return;

		try {

			XInputFile xif;

			for (int i = collection.getPrimaryInputFileSegments(), j = collection.getInputFilesCount(); i < j; i++)
			{
				xif = (XInputFile) collection.getInputFile(i);

				//use PS1 type only ATM (ttx+sub)
				if (xif.getStreamInfo().getStreamType() != CommonParsing.PES_PS1_TYPE)
					continue;

				int packet_count = 0;
				long[] time = { 0, 0 };
				long pts_value = 0;
				ArrayList indexList = new ArrayList();
				byte[] pes_packet = new byte[0x10010];

				int pes_payloadlength;
				int pes_packetlength;
				int pes_extensionlength;
				int pes_headerlength = 9;
				int pes_packetoffset = 6;
				int returncode = 0;
				int pesID = 0;

				long base = 0;
				long count = 0;
				long size = xif.length();

				PushbackInputStream in = new PushbackInputStream(xif.getInputStream(base), pes_packet.length);

				Common.setMessage("-> scanning for remux: " + xif.getName());
				Common.updateProgressBar(" scanning for remux: " + xif.getName(), count, size);

				loop:
				while (count < size)
				{
					Common.updateProgressBar(count, size);

					while (Common.waitingMainProcess())
					{}

					in.read(pes_packet, 0, pes_packetoffset);

					pesID = CommonParsing.getPES_IdField(pes_packet, 0);

					if ((returncode = CommonParsing.validateStartcode(pes_packet, 0)) < 0)
					{
						returncode = returncode < 0 ? -returncode : 4;

						in.read(pes_packet, pes_packetoffset, pes_packet.length - pes_packetoffset);

						int k = returncode;

						for (; k < pes_packet.length - 3; )
						{
							returncode = CommonParsing.validateStartcode(pes_packet, k);

							if (returncode < 0)
							{
								k += -returncode;
								continue;
							}

							else
							{
								in.unread(pes_packet, k, pes_packet.length - k);
								count += k;
								continue loop;
							}
						}

						in.unread(pes_packet, k, pes_packet.length - k);
						count += k;
						continue loop;
					}

					if (pesID != CommonParsing.PRIVATE_STREAM_1_CODE)
						continue loop;

					pes_payloadlength = CommonParsing.getPES_LengthField(pes_packet, 0);

					in.read(pes_packet, pes_packetoffset, pes_payloadlength);

					pes_packetlength = pes_packetoffset + pes_payloadlength;
					pes_extensionlength = CommonParsing.getPES_ExtensionLengthField(pes_packet, 0);

					if (CommonParsing.clearBit33ofPTS(pes_packet, 0))
						pts_value = CommonParsing.getPTSfromBytes(pes_packet, pes_headerlength);

//Common.setMessage("A cnt " + count + " / ID " + pesID + " / payl " + pes_payloadlength + " / pckl " + pes_packetlength + " / extl " + pes_extensionlength + " / pts " + pts_value);

					packet_count++;
					indexList.add(new long[] { pts_value, count, pes_packetlength });

					count += pes_packetlength;
				}

				in.close();

	//			Common.setMessage("B " + packet_count + " / " + indexList.size());

				remuxList.add(new Object[] { indexList , xif.toString(), new int[1] } );
			}

		} catch (Exception e) { 

			Common.setExceptionMessage(e);
		}
	} 

	/**
	 * fill in secondary streams in pes
	 */
	private void remuxPES(JobProcessing job_processing, long video_pts)
	{ 
		if (!MuxPES)
			return;

		Object[] obj;
		String fn;
		ArrayList indexList;
		byte[] pes_packet;
		long[] pes_values;
		int[] pes_index;

		for (int i = 0, j = remuxList.size(); i < j; i++)
		{
			obj = (Object[]) remuxList.get(i);
			indexList = (ArrayList) obj[0];
			fn = obj[1].toString();
			pes_index = (int[]) obj[2];

			try {
				RandomAccessFile pes = new RandomAccessFile(fn, "r");

				for (int k = indexList.size(); pes_index[0] < k; pes_index[0]++)
				{
					pes_values = (long[]) indexList.get(pes_index[0]);

					if (video_pts < pes_values[0])
						break;

					if (video_pts > pes_values[0] + 43200)
						continue;

					pes_packet = new byte[(int) pes_values[2]];
					pes.seek(pes_values[1]);
					pes.read(pes_packet);

					writePacket(job_processing, pes_packet);
				}

				pes.close();

			} catch (Exception e) {

				Common.setExceptionMessage(e);
			}
		}
	}

}

