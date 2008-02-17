/*
 * @(#)StreamParser
 *
 * Copyright (c) 2005-2006 by dvb.matt, All rights reserved.
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

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.PushbackInputStream;
import java.io.FileOutputStream;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Hashtable;

import net.sourceforge.dvb.projectx.common.Common;
import net.sourceforge.dvb.projectx.common.Resource;
import net.sourceforge.dvb.projectx.common.Keys;
import net.sourceforge.dvb.projectx.common.JobCollection;
import net.sourceforge.dvb.projectx.common.JobProcessing;

import net.sourceforge.dvb.projectx.io.StandardBuffer;

import net.sourceforge.dvb.projectx.xinput.XInputFile;
import net.sourceforge.dvb.projectx.xinput.StreamInfo;

import net.sourceforge.dvb.projectx.parser.CommonParsing;
import net.sourceforge.dvb.projectx.parser.StreamConverter;
import net.sourceforge.dvb.projectx.parser.StreamDemultiplexer;
import net.sourceforge.dvb.projectx.parser.StreamParserBase;


/**
 * main thread
 */
public class StreamParserPESPrimary extends StreamParserBase {

	private boolean Message_2;
	private boolean Debug;
	private boolean SimpleMPG;
	private boolean GetEnclosedPackets;
	private boolean IgnoreScrambledPackets;
	private boolean PreviewAllGops;
	private boolean DumpDroppedGop;
	private boolean Overlap;

	/**
	 * 
	 */
	public StreamParserPESPrimary()
	{
		super();
	}

	/**
	 * primary PES Parser
	 */
	public String parseStream(JobCollection collection, XInputFile aXInputFile, int _pes_streamtype, int action, String vptslog)
	{
		JobProcessing job_processing = collection.getJobProcessing();

		setFileName(collection, job_processing, aXInputFile);

		Debug = collection.getSettings().getBooleanProperty(Keys.KEY_DebugLog);
		Message_2 = collection.getSettings().getBooleanProperty(Keys.KEY_MessagePanel_Msg2);
		SimpleMPG = collection.getSettings().getBooleanProperty(Keys.KEY_simpleMPG);
		GetEnclosedPackets = collection.getSettings().getBooleanProperty(Keys.KEY_Input_getEnclosedPackets);
		IgnoreScrambledPackets = collection.getSettings().getBooleanProperty(Keys.KEY_TS_ignoreScrambled);
		PreviewAllGops = collection.getSettings().getBooleanProperty(Keys.KEY_Preview_AllGops);
		DumpDroppedGop = collection.getSettings().getBooleanProperty(Keys.KEY_dumpDroppedGop);
		CreateD2vIndex = collection.getSettings().getBooleanProperty(Keys.KEY_ExternPanel_createD2vIndex);
		SplitProjectFile = collection.getSettings().getBooleanProperty(Keys.KEY_ExternPanel_splitProjectFile);
		Overlap = collection.getSettings().getBooleanProperty(Keys.KEY_ExportPanel_Export_Overlap);

		setOverheadSize(collection);

		boolean isTeletext = false;
		boolean missing_startcode = false;
		boolean scrambling_messaged = false;
		boolean pes_isMpeg2;
		boolean pes_alignment;
		boolean pes_scrambled;
		boolean containsPts = false;
		boolean usePidfilter = false;
		boolean isZeroPacket = false;
		boolean isZeroSubPacket = false;
		boolean ende = false;
		boolean foundObject;

		int Infoscan_Value = Integer.parseInt(collection.getSettings().getProperty(Keys.KEY_ExportPanel_Infoscan_Value));
		int CutMode = collection.getSettings().getIntProperty(Keys.KEY_CutMode);
		int pes_streamtype = _pes_streamtype;
		int pes_payloadlength;
		int pes_packetlength;
		int pes_extensionlength;
		int pes_headerlength = 9;
		int pes_packetoffset = 6;
		int pes_extension2_id;
		int pesID;
		int subID;
		int offset;
		int returncode = 0;
		int vob_ID = 0;
		int cell_ID = 0;
		int ZeroPacketPayload = 0x17FA;

		int tmp_value1 = 0;

		int pesID0 = 0; //first video stream ID
		int dumped_packets = 0;

		job_processing.clearStatusVariables();
		int[] clv = job_processing.getStatusVariables();

		int[] newID = { 0x80, 0x90, 0xC0, 0xE0, 0xA0, 0x20 }; 

		byte[] pes_packet = new byte[0x10010];
		byte[] buffered_data;

		long count = 0;
		long base;
		long size = count;
		long qexit;
		long ptsoffset = 0;
		long lastpts = 0;
		long startPoint = 0;
		long starts[] = new long[collection.getPrimaryInputFileSegments()];
		long Overlap_Value = 1048576L * (collection.getSettings().getIntProperty(Keys.KEY_ExportPanel_Overlap_Value) + 1);

		vptslog = "-1"; //fix

		String file_id = aXInputFile.getFileID();

		streamconverter = new StreamConverter();

		Hashtable substreams = new Hashtable();
		StandardBuffer sb;

		demuxList = job_processing.getPrimaryPESDemuxList();

		/**
		 * re-read old streams, for next split part
		 */
		if (job_processing.getSplitPart() == 0) 
			demuxList.clear();

		else
		{
			for (int i = 0; i < demuxList.size(); i++)
			{
				streamdemultiplexer = (StreamDemultiplexer) demuxList.get(i);

				if (streamdemultiplexer.getnewID() != 0)
					newID[streamdemultiplexer.getType()]++;

				if (streamdemultiplexer.getNum() == -1) 
					continue;

				if (streamdemultiplexer.getType() == CommonParsing.MPEG_VIDEO)
				{ 
					streamdemultiplexer.initVideo2(collection, fparent);

					if (pesID0 == 0) //?
						pesID0 = streamdemultiplexer.getID();
				}
				else 
					streamdemultiplexer.init2(collection, fparent);
			}
		}

		/**
		 * init conversions 
		 */
		String mpeg_type_str = (Keys.ITEMS_FileTypes[pes_streamtype]).toString().toLowerCase();
		mpeg_type_str = "[" + mpeg_type_str.substring(0, mpeg_type_str.indexOf(' ')) + "]";

		initConversion(collection, fparent + (job_processing.getSplitSize() == 0 ? mpeg_type_str : ""), action, CommonParsing.ACTION_TO_VDR, job_processing.getSplitPart());

		/**
		 * d2v projectfile
	 	 */
		if (CreateD2vIndex || SplitProjectFile)
			job_processing.getProjectFileD2V().Init(fparent);

		job_processing.setMinBitrate(CommonParsing.MAX_BITRATE_VALUE);
		job_processing.setMaxBitrate(0);
		job_processing.setExportedVideoFrameNumber(0);
		job_processing.setEndPtsOfGop(-10000);
		job_processing.setSequenceHeader(true);
		job_processing.setAllMediaFilesExportLength(0);
		job_processing.setProjectFileExportLength(0);
		job_processing.setCutByteposition(0);

		/**
		 * pid inclusion 
		 */
		int[] predefined_Pids = collection.getPIDsAsInteger();
		int[] include = new int[predefined_Pids.length];

		for (int i = 0; i < include.length; i++) 
			include[i] = 0xFF & predefined_Pids[i];

		if (include.length > 0)
		{
			Arrays.sort(include);

			String str = " ";

			for (int i = 0; i < include.length; i++)
				str += "0x" + Integer.toHexString(include[i]).toUpperCase() + " ";

			Common.setMessage(Resource.getString("parsePrimaryPES.special.pes") + ": {" + str + "}");

			usePidfilter = true;
		}


		try {

			/**
			 * determine start & end byte pos. of each file segment
			 */
			for (int i = 0; i < starts.length; i++)
			{
				aXInputFile = (XInputFile) collection.getInputFile(i);
				starts[i] = size;
				size += aXInputFile.length();
			}

			aXInputFile = (XInputFile) collection.getInputFile(job_processing.getFileNumber());

			/**
			 * set start & end byte pos. of first file segment
			 */
			count = starts[job_processing.getFileNumber()];
			size = count + aXInputFile.length();

			/**
			 * split skipping first, for next split part
			 */
			if (job_processing.getSplitSize() > 0)
			{
				startPoint = job_processing.getLastHeaderBytePosition();
				startPoint -= !Overlap ? 0 : Overlap_Value;

				job_processing.setLastGopTimecode(0);
				job_processing.setLastGopPts(0);
				job_processing.setLastSimplifiedPts(0);
			}

			List CutpointList = collection.getCutpointList();
			List ChapterpointList = collection.getChapterpointList();

			/** 
			 * jump near to first cut-in point to collect more audio
			 */
			if (CutMode == CommonParsing.CUTMODE_BYTE && CutpointList.size() > 0 && CommonParsing.getCutCounter() == 0  && (!PreviewAllGops || action != CommonParsing.ACTION_DEMUX))
				startPoint = Long.parseLong(CutpointList.get(CommonParsing.getCutCounter()).toString()) - ((action == CommonParsing.ACTION_DEMUX) ? OverheadSize : 0);

			if (startPoint < 0)
				startPoint = count;  // =0

			else if (startPoint < count)
			{
				for (int i = starts.length; i > 0; i--)
					if (starts[i - 1] > startPoint)
						job_processing.countFileNumber(-1);
			}

			else if (startPoint > count)
			{
				for (int i = job_processing.getFileNumber() + 1; i < starts.length; i++)
				{
					if (starts[i] > startPoint)
						break;
					else 
						job_processing.countFileNumber(+1);
				}
			}

			aXInputFile = (XInputFile) collection.getInputFile(job_processing.getFileNumber());
			count = starts[job_processing.getFileNumber()];

			if (job_processing.getFileNumber() > 0)
				Common.setMessage(Resource.getString("parsePrimaryPES.continue") + ": " + aXInputFile);

			base = count;
			size = count + aXInputFile.length();

			PushbackInputStream in = new PushbackInputStream(aXInputFile.getInputStream(startPoint - base), pes_packet.length);

			count += (startPoint - base);

			Common.updateProgressBar((action == CommonParsing.ACTION_DEMUX ? Resource.getString("parsePrimaryPES.demuxing") : Resource.getString("parsePrimaryPES.converting")) + " " + Resource.getString("parsePrimaryPES.avpes.file") + " " + aXInputFile.getName(), (count - base), (size - base));

			qexit = count + (0x100000L * Infoscan_Value);

			//yield();

			bigloop:
			while (true)
			{
				loop:
				while (count < size)
				{
					pes_streamtype = _pes_streamtype;  // reset to original type

					Common.updateProgressBar(count, size);

					//yield();

					while (pause())
					{}

					if (CommonParsing.isProcessCancelled() || (CommonParsing.isInfoScan() && count > qexit))
					{ 
						CommonParsing.setProcessCancelled(false);
						job_processing.setSplitSize(0); 

						break bigloop; 
					}

					/**
					 * after last cut out, still read some packets to collect some audio
					 */
					if (job_processing.getCutComparePoint() + 20 < job_processing.getSourceVideoFrameNumber())
					{
						ende = true;
						break bigloop; 
					}

					/**
					 * cut mode bytepos + min 1 cutpoint
					 */
					if (CutMode == CommonParsing.CUTMODE_BYTE && CutpointList.size() > 0)
					{
						if (CommonParsing.getCutCounter() == CutpointList.size() && (CommonParsing.getCutCounter() & 1) == 0)
							if (count > Long.parseLong(CutpointList.get(CommonParsing.getCutCounter() - 1).toString()) + (action == CommonParsing.ACTION_DEMUX ? OverheadSize : 64000))
							{
								ende = true;
								break bigloop;
							}
					}

					in.read(pes_packet, 0, pes_packetoffset);

					pesID = CommonParsing.getPES_IdField(pes_packet, 0);

					/**
					 *
					 */
					if ((returncode = CommonParsing.validateStartcode(pes_packet, 0)) < 0 || pesID < CommonParsing.SYSTEM_END_CODE)
					{
						returncode = returncode < 0 ? -returncode : 4;

						if (Message_2 && !missing_startcode)
							Common.setMessage(Resource.getString("parsePrimaryPES.missing.startcode") + " " + count);

						in.read(pes_packet, pes_packetoffset, pes_packet.length - pes_packetoffset);

						int i = returncode;

						for (; i < pes_packet.length - 3; )
						{
							returncode = CommonParsing.validateStartcode(pes_packet, i);

							if (returncode < 0)
							{
								i += -returncode;
								continue;
							}

							else
							{
								in.unread(pes_packet, i, pes_packet.length - i);

								count += i;
								missing_startcode = true;

								continue loop;
							}
						}

						in.unread(pes_packet, i, pes_packet.length - i);

						count += i;
						missing_startcode = true;

						continue loop;
					}

					if (Message_2 && missing_startcode)
						Common.setMessage(Resource.getString("parsePrimaryPES.found.startcode") + " " + count);

					missing_startcode = false;
	
					if (pes_streamtype == CommonParsing.MPEG1PS_TYPE || pes_streamtype == CommonParsing.MPEG2PS_TYPE || SimpleMPG)
					{
						switch (pesID)
						{
						case CommonParsing.SYSTEM_END_CODE: 
							in.unread(pes_packet, 4, 2);
							Common.setMessage("-> skip system_end_code @ " + count);
							count += 4;
							continue loop;

						case CommonParsing.PACK_START_CODE: 
							if ((0xC0 & pes_packet[4]) == 0)  //mpg1
							{
								in.skip(pes_packetoffset);
								count += 12;
								continue loop;
							}

							else if ((0xC0 & pes_packet[4]) == 0x40) //mpg2
							{
								in.read(pes_packet, pes_packetoffset, 8);

								offset = 7 & pes_packet[13];

								count += 14;

								in.read(pes_packet, 14, offset);

								if (offset > 0)
								{
									for (int i = 0; i < offset; i++)
										if (pes_packet[14 + i] != -1)
										{
											in.unread(pes_packet, 14, offset);

											Common.setMessage("!> wrong pack header stuffing @ " + count);
											missing_startcode = true;
											continue loop;
										}
								}

								count += offset;

								continue loop;
							}

							else
							{
								in.unread(pes_packet, 4, 2);
								count += 4;
								continue loop;
							}

						case CommonParsing.PRIVATE_STREAM_2_CODE: //determine cellids
							pes_payloadlength = CommonParsing.getPES_LengthField(pes_packet, 0);

							in.read(pes_packet, pes_packetoffset, pes_payloadlength);

							if (pes_packet[pes_packetoffset] == 1) //substream id 1
							{
								int cellid = 0xFF & pes_packet[0x22];
								int vobid = (0xFF & pes_packet[0x1F])<<8 | (0xFF & pes_packet[0x20]);

								if (cell_ID != cellid || vob_ID != vobid)
								{
									Common.setMessage(Resource.getString("parsePrimaryPES.split.cellids", "" + vobid, "" + cellid, "" + count, "" + clv[6], "" + job_processing.getExportedVideoFrameNumber()));

									if (collection.getSettings().getBooleanProperty(Keys.KEY_VOB_resetPts))
									{
							//			ptsoffset = nextFilePTS(collection, CommonParsing.PRIMARY_PES_PARSER, pes_streamtype, lastpts, job_processing.getFileNumber(), (count - base));
										ptsoffset = nextFilePTS(collection, CommonParsing.PRIMARY_PES_PARSER, pes_streamtype, lastpts, ptsoffset, job_processing.getFileNumber(), (count - base));

										if (ptsoffset == -1) 
											ptsoffset = 0; 

										else
										{
											for (int i = 0; i < demuxList.size(); i++)
											{
												streamdemultiplexer = (StreamDemultiplexer)demuxList.get(i);
												streamdemultiplexer.PTSOffset(ptsoffset);

												if (streamdemultiplexer.getID() == pesID0) 
													streamdemultiplexer.resetVideo();
											}

											job_processing.setSequenceHeader(true);
											job_processing.setNewVideoStream(true);
										//	job_processing.addCellTime(String.valueOf(job_processing.getExportedVideoFrameNumber()));
										}
									}
								}

								cell_ID = cellid;
								vob_ID = vobid;
							}

							count += (pes_packetoffset + pes_payloadlength);
							continue loop;

						case CommonParsing.SYSTEM_START_CODE:
						case CommonParsing.PROGRAM_STREAM_MAP_CODE:
						case CommonParsing.PADDING_STREAM_CODE:
						case CommonParsing.ECM_STREAM_CODE:
						case CommonParsing.EMM_STREAM_CODE:
						case CommonParsing.DSM_CC_STREAM_CODE:
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
							pes_payloadlength = CommonParsing.getPES_LengthField(pes_packet, 0);

							in.skip(pes_payloadlength);

							count += (pes_packetoffset + pes_payloadlength);
							continue loop;
						}
					}

					if ( (0xF0 & pesID) != 0xE0 && (0xE0 & pesID) != 0xC0 && pesID != CommonParsing.PRIVATE_STREAM_1_CODE)
					{
						in.unread(pes_packet, 3, pes_packetoffset - 3);
						count += 3;
						continue loop;
					}

					/**
					 * mark for split at sequenceheader 
					 */
					job_processing.setLastHeaderBytePosition(count);
					job_processing.setCutByteposition(count);

					pes_payloadlength = CommonParsing.getPES_LengthField(pes_packet, 0);

					/**
					 * zeropackets of video only as PES from TS allowed
					 */
					isZeroPacket = pes_payloadlength == 0 && (0xF0 & pesID) == 0xE0;

					if (isZeroPacket)
						pes_payloadlength = ZeroPacketPayload;

					in.read(pes_packet, pes_packetoffset, pes_payloadlength + 4);

					pes_packetlength = pes_packetoffset + pes_payloadlength;

					pes_isMpeg2 = (0xC0 & pes_packet[6]) == 0x80;

					/**
					 * determine next startcode in zero packets
					 */
					if (isZeroPacket)
					{
						if (Debug) 
							System.out.println("A " + Resource.getString("parsePrimaryPES.packet.length") + " " + count);

						//start after pes header
						for (int i = pes_packetoffset + (pes_isMpeg2 ? 3 : 1); isZeroPacket && i <= pes_packetlength; )
						{
							if ((returncode = CommonParsing.validateStartcode(pes_packet, i)) < 0 || CommonParsing.getPES_IdField(pes_packet, i) < CommonParsing.SYSTEM_END_CODE)
							{
								i += (returncode < 0 ? -returncode : 4);
								continue;
							}

							/**
							 * next header found before max. size of ZeroPacketPayload
							 * handle packet as normal, the 4 added bytes will be unread in the next startcode check
							 */
							in.unread(pes_packet, i + 4, pes_packetlength - i);

							pes_packetlength = i;
							pes_payloadlength = pes_packetlength - pes_packetoffset;

							isZeroPacket = false;
						}

						CommonParsing.setPES_LengthField(pes_packet, 0, pes_payloadlength);
					}

					/**
					 * check next startcode
					 */
					if (GetEnclosedPackets && !isZeroPacket && CommonParsing.validateStartcode(pes_packet, pes_packetlength) < 0)
					{
						if (count + pes_packetlength < size)
						{
							if (Message_2 && !missing_startcode)
								Common.setMessage(Resource.getString("parsePrimaryPES.miss.startcode2", String.valueOf(count + pes_packetlength), String.valueOf(count), Integer.toHexString(pesID).toUpperCase()));

							missing_startcode = true;

							in.unread(pes_packet, pes_packetoffset, pes_payloadlength + 4);
							count += pes_packetoffset;

							continue loop;
						}
					}

					else 
						in.unread(pes_packet, pes_packetlength, 4);


					clv[5]++;

					if (Debug) 
						System.out.println(Resource.getString("parsePrimaryPES.packs") + ": " + pesID + "/" + clv[5] + "/" + (pes_packetlength) + "/" + ((count * 100 / size)) + "% " + (count));

					pes_extensionlength = CommonParsing.getPES_ExtensionLengthField(pes_packet, 0);

					pes_isMpeg2 = (0xC0 & pes_packet[6]) == 0x80;
					pes_alignment = pes_isMpeg2 && (4 & pes_packet[6]) != 0;
					pes_scrambled = pes_isMpeg2 && (0x30 & pes_packet[6]) != 0;

					count += pes_packetlength;

					zeropacketloop:
					do {
						/**
						 * exit loop
						 */
						if (isZeroPacket && count >= size)
							break zeropacketloop;

						/**
						 * read videos zerosubpacket data in a loop, if packet data is greater than max. ZeroPacketPayload
						 * the first read header data is re-used, we read only the rest
						 */
						if (isZeroSubPacket)
						{
							if (Debug) 
								System.out.println("B " + Resource.getString("parsePrimaryPES.packet.length") + " " + count + " / " + pes_isMpeg2);

							pes_payloadlength = ZeroPacketPayload;

							pes_packetlength = pes_packetoffset + pes_payloadlength;

							offset = pes_isMpeg2 ? 3 : 1;

							in.read(pes_packet, pes_packetoffset + offset, pes_payloadlength - offset + 4);
							in.unread(pes_packet, pes_packetlength, 4);

							for (int i = pes_packetoffset + offset; isZeroPacket && i <= pes_packetlength; )
							{
								if ((returncode = CommonParsing.validateStartcode(pes_packet, i)) < 0 || CommonParsing.getPES_IdField(pes_packet, i) < CommonParsing.SYSTEM_END_CODE)
								{
									i += (returncode < 0 ? -returncode : 4);
									continue;
								}

								/**
								 * next header found before max. size of ZeroPacketPayload
								 * handle packet as normal, the 4 added bytes will be unread in the next startcode check
								 */
								in.unread(pes_packet, i, pes_packetlength - i);

								pes_packetlength = i;
								pes_payloadlength = pes_packetlength - pes_packetoffset;

								// set isZeroPacket to false, if next startcode was found
								isZeroPacket = false;
							}

							CommonParsing.setValue(pes_packet, pes_packetoffset, offset, false, pes_isMpeg2 ? (0xF3 & pes_packet[pes_packetoffset])<<16 : 0x0F); // values of pes scrambling and copyright/copy prot. are taken from orig.
							CommonParsing.setPES_LengthField(pes_packet, 0, pes_payloadlength);

							count += (pes_payloadlength - offset);
						}

						isZeroSubPacket = isZeroPacket;

						/**
						 * check scrambling
						 */
						if (IgnoreScrambledPackets)
						{
							// cannot work with scrambled data
							if (pes_scrambled)
							{
								if (!scrambling_messaged)
								{
									scrambling_messaged = true;

									Common.setMessage(Resource.getString("parseTS.scrambled", Integer.toHexString(pesID).toUpperCase(), String.valueOf(clv[5]), String.valueOf(count - pes_packetlength)));
								}

								continue zeropacketloop;
							}

							else if (scrambling_messaged)
							{
								Common.setMessage(Resource.getString("parseTS.clear", Integer.toHexString(pesID).toUpperCase(), String.valueOf(clv[5]), String.valueOf(count - pes_packetlength)));
								scrambling_messaged = false;
							}
						}

						/**
						 * vdr_dvbsub determination
						 */
						pes_extension2_id = CommonParsing.getExtension2_Id(pes_packet, pes_headerlength, pes_payloadlength, pesID, pes_isMpeg2, job_processing.getLastHeaderBytePosition());

						isTeletext = false;
						subID = 0;

						if (pesID == CommonParsing.PRIVATE_STREAM_1_CODE && pes_payloadlength > 2)
						{
							offset = pes_headerlength + pes_extensionlength;

							if (offset < pes_packetlength)
							{
								subID = 0xFF & pes_packet[offset];
								isTeletext = pes_extensionlength == 0x24 && subID>>>4 == 1; 

								// vdr 1.5.x dvb-subs container
								if (pes_payloadlength >= 4 && subID>>>4 == 2)
								{
									tmp_value1 = CommonParsing.getIntValue(pes_packet, offset, 4, !CommonParsing.BYTEREORDERING);

									//vdr 1.5.x start packet of dvb-sub || subsequent packet
									if ((pes_alignment && (0xF0FFFFFF & tmp_value1) == 0x20010000) || (!pes_alignment && (0xF0FFFFFF & tmp_value1) == 0x20010001))
									{
										for (int i = offset, j = offset + 4; i < j; i++)
											pes_packet[i] = (byte) 0xFF;

										pes_extensionlength += 4;
										pes_packet[8] = (byte)(pes_extensionlength);
										pes_payloadlength -= 4;

										//pes_extension2_id = 1;
										pes_extension2_id = subID = tmp_value1>>>24;
										pes_streamtype = CommonParsing.MPEG2PS_TYPE;  //will be resetted before next packet

										if (pes_alignment)
											pes_packet[offset + 4] = (byte)(subID);
									}
								}

								//subpic in vdr_pes before 1.5.x
								if (pes_alignment && !isTeletext && (subID>>>4 == 2 || subID>>>4 == 3))
									pes_streamtype = CommonParsing.MPEG2PS_TYPE;  //will be resetted before next packet

								if (pes_streamtype != CommonParsing.MPEG1PS_TYPE && pes_streamtype != CommonParsing.MPEG2PS_TYPE && !isTeletext) 
									subID = 0; //disables LPCM too 
							}

							else if (pes_streamtype != CommonParsing.MPEG1PS_TYPE) //?
							{
								pes_extensionlength = pes_payloadlength - 3;
								pes_packet[8] = (byte)(pes_extensionlength);
							}


							/**
							 * packet buffering esp. of subpics from vdr or other pes
							 */
							if (pes_extension2_id != -1)
							{
								String str = String.valueOf(pes_extension2_id);
								offset = pes_headerlength + pes_extensionlength;

								if ( !substreams.containsKey(str))
									substreams.put(str, new StandardBuffer());

								sb = (StandardBuffer) substreams.get(str);

								// buffer raw packet data
								if (!pes_alignment)
								{
									sb.write(pes_packet, offset, pes_packetlength - offset);
									continue zeropacketloop;
								}

								// start packet, buffer this and get last completed packet
								else
								{
									buffered_data = sb.getData();

									sb.reset();
									sb.write(pes_packet, 0, pes_packetlength);

									if (buffered_data == null || buffered_data.length < 10)
										continue zeropacketloop;

									pes_packetlength = buffered_data.length;

									if (pes_packetlength > 0x10005)
									{
										Common.setMessage("!> sub packet too long: 0x" + Integer.toHexString(pesID).toUpperCase() + " /ext2_id " + pes_extension2_id);
										pes_packetlength = 0x10005;
									}

									pes_payloadlength = pes_packetlength - pes_packetoffset;

									System.arraycopy(buffered_data, 0, pes_packet, 0, pes_packetlength);

									CommonParsing.setPES_LengthField(pes_packet, 0, pes_payloadlength);

									buffered_data = null;
								}
							}
						}

						/**
						 * pesID, subID inclusion 
						 */
						if (usePidfilter)
						{
							if (Arrays.binarySearch(include, pesID) < 0 && Arrays.binarySearch(include, subID) < 0)
								continue zeropacketloop;
						}

						/**
						 * raw id filter extraction
						 */
						if (action == CommonParsing.ACTION_FILTER)
						{
							if (subID != 0)
								pes_packet[6] |= 4; //set alignment

							streamconverter.write(job_processing, pes_packet, 0, pes_packetlength, null, job_processing.getCutByteposition(), CommonParsing.isInfoScan(), CutpointList);
							continue zeropacketloop;
						}

						foundObject = false;

						/**
						 * find ID object
						 */
						for (int i = 0; i < demuxList.size(); i++)
						{
							streamdemultiplexer = (StreamDemultiplexer) demuxList.get(i);

							foundObject = pesID == streamdemultiplexer.getID() && subID == streamdemultiplexer.subID() && isTeletext == streamdemultiplexer.isTTX();

							if (foundObject)
								break; 
						}

						/**
						 * create new ID object
						 */
						if (!foundObject)
						{
							/**
							 * dump startpacket 
							 */
							if (DumpDroppedGop)
							{
								String dumpname = fparent + "(" + Integer.toHexString(pesID) + "-" + Integer.toHexString(subID) + "#" + (dumped_packets++) + "@" + (count - pes_packetlength) + ").bin";

								FileOutputStream dump = new FileOutputStream(dumpname);

								dump.write(pes_packet, 0, pes_packetlength); 
								dump.flush(); 
								dump.close();

								Common.setMessage(Resource.getString("parsePrimaryPES.dump.1st") + ": " + dumpname);
							}

							String IDtype = "";

							switch (0xF0 & pesID)
							{
							case 0xE0:
								IDtype = Resource.getString("idtype.mpeg.video");

								streamdemultiplexer = new StreamDemultiplexer(collection, ptsoffset);
								streamdemultiplexer.setID(pesID);
								streamdemultiplexer.setType(CommonParsing.MPEG_VIDEO);
								streamdemultiplexer.setnewID(newID[CommonParsing.MPEG_VIDEO]++);
								streamdemultiplexer.setsubID(0);
								streamdemultiplexer.setStreamType(pes_streamtype);

								demuxList.add(streamdemultiplexer);

								if (pesID0 == 0 || pesID0 == pesID)
								{ 
									if (action == CommonParsing.ACTION_DEMUX) 
										streamdemultiplexer.initVideo(collection, fparent, MainBufferSize, demuxList.size(), CommonParsing.PRIMARY_PES_PARSER);

									else
										IDtype += " " + Resource.getString("idtype.mapped.to") + Integer.toHexString(streamdemultiplexer.getnewID()).toUpperCase();

									pesID0 = pesID;
								}

								else 
									IDtype += Resource.getString("idtype.ignored");

								break; 

							case 0xC0:
							case 0xD0:
								IDtype = Resource.getString("idtype.mpeg.audio"); 

								streamdemultiplexer = new StreamDemultiplexer(collection, ptsoffset);
								streamdemultiplexer.setID(pesID);
								streamdemultiplexer.setType(CommonParsing.MPEG_AUDIO);
								streamdemultiplexer.setnewID(newID[CommonParsing.MPEG_AUDIO]++);
								streamdemultiplexer.setsubID(0);
								streamdemultiplexer.setStreamType(pes_streamtype);

								demuxList.add(streamdemultiplexer);

								if (action == CommonParsing.ACTION_DEMUX) 
									streamdemultiplexer.init(collection, fparent, MainBufferSize / demuxList.size(), demuxList.size(), CommonParsing.PRIMARY_PES_PARSER);
	
								else
									IDtype += " " + Resource.getString("idtype.mapped.to") + Integer.toHexString(streamdemultiplexer.getnewID()).toUpperCase();

								break; 
							}

							switch (pesID)
							{
							case CommonParsing.PRIVATE_STREAM_1_CODE:
								IDtype = Resource.getString("idtype.private.stream");
								IDtype += (isTeletext ? " TTX " : "") + (subID != 0 ? " (SubID 0x" + Integer.toHexString(subID).toUpperCase() + ")" : ""); 

								streamdemultiplexer = new StreamDemultiplexer(collection, ptsoffset);
								streamdemultiplexer.setID(pesID);
								streamdemultiplexer.setsubID(subID);
								streamdemultiplexer.setTTX(isTeletext);

								if (isTeletext)
								{
									streamdemultiplexer.setnewID(newID[CommonParsing.TELETEXT]++);
									streamdemultiplexer.setType(CommonParsing.TELETEXT);
								}

								else
								{
									switch(subID>>>4)
									{
									case 0:
										if (pes_streamtype != CommonParsing.MPEG1PS_TYPE && pes_streamtype != CommonParsing.MPEG2PS_TYPE)
										{
											streamdemultiplexer.setnewID(newID[CommonParsing.AC3_AUDIO]++);
											streamdemultiplexer.setType(CommonParsing.AC3_AUDIO);
										}
										break;

									case 8:
										streamdemultiplexer.setnewID(newID[CommonParsing.AC3_AUDIO]++);
										streamdemultiplexer.setType(CommonParsing.AC3_AUDIO);
										break;

									case 0xA:
										streamdemultiplexer.setnewID(newID[CommonParsing.LPCM_AUDIO]++);
										streamdemultiplexer.setType(CommonParsing.LPCM_AUDIO);
										break;

									case 2:
									case 3:
										streamdemultiplexer.setnewID(newID[CommonParsing.SUBPICTURE]++);
										streamdemultiplexer.setType(CommonParsing.SUBPICTURE);
										break;

									default:
										streamdemultiplexer.setType(CommonParsing.UNKNOWN);
									}
								}

								streamdemultiplexer.setStreamType(pes_streamtype);
								demuxList.add(streamdemultiplexer);

								if (action == CommonParsing.ACTION_DEMUX)
								{
									switch (streamdemultiplexer.getType())
									{
									case CommonParsing.AC3_AUDIO:
									case CommonParsing.DTS_AUDIO:
									case CommonParsing.TELETEXT:
									case CommonParsing.SUBPICTURE:
									case CommonParsing.LPCM_AUDIO:
										streamdemultiplexer.init(collection, fparent, MainBufferSize / demuxList.size(), demuxList.size(), CommonParsing.PRIMARY_PES_PARSER);
										break;

									default:
										IDtype += Resource.getString("idtype.ignored");
									}
								}

								else
								{
									if (pes_streamtype != CommonParsing.MPEG1PS_TYPE && pes_streamtype != CommonParsing.MPEG2PS_TYPE)
										IDtype += " " + Resource.getString("idtype.mapped.to") + Integer.toHexString(streamdemultiplexer.getnewID()).toUpperCase();

									else if (isTeletext || subID>>>4 == 8)
										IDtype += " " + Resource.getString("idtype.mapped.to") + Integer.toHexString(streamdemultiplexer.getnewID()).toUpperCase();

									else
										IDtype += Resource.getString("idtype.ignored"); 
								}

								if (action == CommonParsing.ACTION_TO_VDR)
								{
									if ((pes_streamtype == CommonParsing.MPEG1PS_TYPE || pes_streamtype == CommonParsing.MPEG2PS_TYPE) && subID>>>4 != 8)
										IDtype += Resource.getString("idtype.ignored"); 
								}

								break; 
							}

							Common.setMessage(Resource.getString("parsePrimaryPES.found.pesid") + Integer.toHexString(pesID).toUpperCase() + " " + IDtype + " @ " + job_processing.getCutByteposition());
						}


						if (!streamdemultiplexer.StreamEnabled())
							continue zeropacketloop;

						/**
						 * packet to streamdemultiplexer
						 */
						if (action == CommonParsing.ACTION_DEMUX)
						{
							if (streamdemultiplexer.getType() == CommonParsing.MPEG_VIDEO)
							{ 
								if (pesID0 != streamdemultiplexer.getID()) 
									continue zeropacketloop;

								streamdemultiplexer.writeVideo(job_processing, pes_packet, 0, pes_packetlength, true, CutpointList, ChapterpointList);
							}
							else 
								streamdemultiplexer.write(job_processing, pes_packet, 0, pes_packetlength, true);

							if (streamdemultiplexer.getPTS() > lastpts) 
								lastpts = streamdemultiplexer.getPTS();
						}

						/**
						 * packet to streamconverter
						 */
						else
							streamconverter.write(job_processing, pes_packet, streamdemultiplexer, job_processing.getCutByteposition(), CommonParsing.isInfoScan(), CutpointList);

					} while (isZeroPacket);


					if (action != CommonParsing.ACTION_DEMUX) 
						job_processing.setLastHeaderBytePosition(count);

					/**
					 * split size reached 
					 */
					if (job_processing.getSplitSize() > 0 && job_processing.getSplitSize() < job_processing.getAllMediaFilesExportLength()) 
						break loop;
				}


				if (job_processing.getSplitSize() > 0 && job_processing.getSplitSize() < job_processing.getAllMediaFilesExportLength())
					break bigloop;

				/**
				 * next file segment
				 */
				if (job_processing.getFileNumber() < collection.getPrimaryInputFileSegments() - 1)
				{ 
					in.close();
					//System.gc();

					if (collection.getSettings().getBooleanProperty(Keys.KEY_Input_concatenateForeignRecords) && action == CommonParsing.ACTION_DEMUX)
					{
						ptsoffset = nextFilePTS(collection, CommonParsing.PRIMARY_PES_PARSER, pes_streamtype, lastpts, job_processing.getFileNumber() + 1);

						if (ptsoffset == -1) 
							ptsoffset = 0; 

						else
						{
							for (int i = 0; i < demuxList.size(); i++)
							{
								streamdemultiplexer = (StreamDemultiplexer) demuxList.get(i);
								streamdemultiplexer.PTSOffset(ptsoffset);

								if (streamdemultiplexer.getID() == pesID0) // ??
									streamdemultiplexer.resetVideo();
							}

							job_processing.setSequenceHeader(true);
							job_processing.setNewVideoStream(true);
					//		job_processing.addCellTime(String.valueOf(job_processing.getExportedVideoFrameNumber()));
						}
					}

					XInputFile nextXInputFile = (XInputFile) collection.getInputFile(job_processing.countFileNumber(+1));
					count = size;
					base = count;

					size += nextXInputFile.length();
					in = new PushbackInputStream(nextXInputFile.getInputStream(), 0x10010);

					Common.setMessage(Resource.getString("parsePrimaryPES.actual.written") + " " + job_processing.getExportedVideoFrameNumber());
					Common.setMessage(Resource.getString("parsePrimaryPES.switch") + " " + nextXInputFile + " (" + Common.formatNumber(nextXInputFile.length()) + " bytes) @ " + base);

					Common.updateProgressBar((action == CommonParsing.ACTION_DEMUX ? Resource.getString("parsePrimaryPES.demuxing") : Resource.getString("parsePrimaryPES.converting")) + " " + Resource.getString("parsePrimaryPES.avpes.file") + " " + nextXInputFile.getName());
				}

				else 
					break bigloop;

			}

			/**
			 * file end reached for split 
			 */
			if ( (count >= size || ende) && job_processing.getSplitSize() > 0 )
				job_processing.setSplitLoopActive(false);

			in.close(); 

			vptslog = processElementaryStreams(vptslog, action, clv, collection, job_processing);

		} catch (IOException e2) { 

			Common.setExceptionMessage(e2);
		}

		return vptslog;
	}
}
