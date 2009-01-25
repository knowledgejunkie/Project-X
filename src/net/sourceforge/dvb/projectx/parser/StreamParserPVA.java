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

import java.util.Arrays;
import java.util.List;
//import java.util.Hashtable;

import net.sourceforge.dvb.projectx.common.Common;
import net.sourceforge.dvb.projectx.common.Resource;
import net.sourceforge.dvb.projectx.common.Keys;
import net.sourceforge.dvb.projectx.common.JobCollection;
import net.sourceforge.dvb.projectx.common.JobProcessing;

import net.sourceforge.dvb.projectx.io.RawFile;

import net.sourceforge.dvb.projectx.xinput.XInputFile;

import net.sourceforge.dvb.projectx.parser.CommonParsing;
import net.sourceforge.dvb.projectx.parser.StreamConverter;
import net.sourceforge.dvb.projectx.parser.StreamDemultiplexer;
import net.sourceforge.dvb.projectx.parser.StreamParserBase;


/**
 * main thread
 */
public class StreamParserPVA extends StreamParserBase {

	/**
	 * 
	 */
	public StreamParserPVA()
	{
		super();
	}

	/**
	 * read bytes for overlap pva check
	 */
	private byte[] overlapPVA(JobCollection collection, byte[] overlapnext)
	{
		JobProcessing job_processing = collection.getJobProcessing();

		if (job_processing.getFileNumber() < collection.getPrimaryInputFileSegments() - 1)
		{
			try {

				((XInputFile) collection.getInputFile(job_processing.getFileNumber() + 1)).randomAccessSingleRead(overlapnext, 0);

			} catch (IOException e) {

				Common.setExceptionMessage(e);
			}
		}

		return overlapnext;
	}


	/**
	 * PVA/PSV/PSA  Parser 
	 */
	public String parseStream(JobCollection collection, XInputFile aXInputFile, int pes_streamtype, int action, String vptslog)
	{
		JobProcessing job_processing = collection.getJobProcessing();

		setFileName(collection, job_processing, aXInputFile);

		boolean Message_1 = collection.getSettings().getBooleanProperty(Keys.KEY_MessagePanel_Msg1);
		boolean Message_2 = collection.getSettings().getBooleanProperty(Keys.KEY_MessagePanel_Msg2);
		boolean ConformAudioCheck = collection.getSettings().getBooleanProperty(Keys.KEY_PVA_Audio);
		boolean Debug = collection.getSettings().getBooleanProperty(Keys.KEY_DebugLog);
		boolean OverlapCheck = collection.getSettings().getBooleanProperty(Keys.KEY_PVA_FileOverlap);
		boolean Concatenate = collection.getSettings().getBooleanProperty(Keys.KEY_Input_concatenateForeignRecords);

		CreateD2vIndex = collection.getSettings().getBooleanProperty(Keys.KEY_ExternPanel_createD2vIndex);
		SplitProjectFile = collection.getSettings().getBooleanProperty(Keys.KEY_ExternPanel_splitProjectFile);

		setOverheadSize(collection);

		boolean Overlap = collection.getSettings().getBooleanProperty(Keys.KEY_ExportPanel_Export_Overlap);

		boolean containsPts = false;
		boolean ende = false;
		boolean missing_syncword = false;
		boolean usePidfilter = false;
		boolean isTeletext;
		boolean foundObject;

		int pva_buffersize = 0x10006;

		byte[] pva_packet = new byte[pva_buffersize]; // packets usually up to 6kB
		byte[] overlapnext = new byte[0x100];
		byte[] new_pes_packet = new byte[pva_buffersize];
		byte[] new_pes_packetheader = { 0, 0, 1, (byte)0xE0, 0, 0, (byte)0x80, 0, 0 };
		byte[] new_pes_packetheader_and_pts = { 0, 0, 1, (byte)0xE0, 0, 0, (byte)0x80, (byte)0x80, 5, 0, 0, 0, 0, 0 };

		int Infoscan_Value = Integer.parseInt(collection.getSettings().getProperty(Keys.KEY_ExportPanel_Infoscan_Value));
		int CutMode = collection.getSettings().getIntProperty(Keys.KEY_CutMode);
		int pva_pid;
		int ptsflag;
		int pva_packetoffset;
		int pva_payloadlength;
		int pva_headerlength = 8;
		int pva_ptslength = 4;
		int pva_counter;
		int pre_bytes;
		int post_bytes;

		int[] newID = { 0x80, 0x90, 0xC0, 0xE0, 0xA0, 0x20 };

		job_processing.clearStatusVariables();
		int[] clv = job_processing.getStatusVariables();

		long pts = 0;
		long lastpts = 0;
		long ptsoffset = 0;
		long packet = 0;
		long count = 0;
		long size = 0;
		long base;
		long startPoint = 0;
		long starts[] = new long[collection.getPrimaryInputFileSegments()];
		long Overlap_Value = 1048576L * (collection.getSettings().getIntProperty(Keys.KEY_ExportPanel_Overlap_Value) + 1);
		long qexit;

		String file_id = aXInputFile.getFileID();
	
		String[] streamtypes = { 
			Resource.getString("parsePVA.streamtype.ac3"),
			Resource.getString("parsePVA.streamtype.ttx"),
			Resource.getString("parsePVA.streamtype.mpeg.audio"),
			Resource.getString("parsePVA.streamtype.mpeg.video")
		};

		RawFile rawfile = null;
		StreamBuffer streambuffer = null;
		streamconverter = new StreamConverter();

		demuxList = job_processing.getPVADemuxList();
		List PVAPidlist = job_processing.getPVAPidList();
//
		streamobjects = getStreamObjects(job_processing);
//

		/**
		 * re-read old streams, for next split part
		 */
		if (job_processing.getSplitPart() == 0)
		{
			PVAPidlist.clear();
			demuxList.clear();
		}
		else
		{
			for (int i = 0; i < PVAPidlist.size(); i++)
			{
				streambuffer = (StreamBuffer) PVAPidlist.get(i);
				streambuffer.reset();
				streambuffer.setStarted(true);
			}

			for (int i = 0; i < demuxList.size(); i++)
			{
				streamdemultiplexer = (StreamDemultiplexer) demuxList.get(i);
Common.setMessage(">>3 " + streamdemultiplexer);
Common.setMessage(">>4 " + streamdemultiplexer.hashCode());
				if (streamdemultiplexer.getnewID() != 0)
					newID[streamdemultiplexer.getType()]++;

				if (streamdemultiplexer.getNum() == -1) 
					continue;

				if (streamdemultiplexer.getType() == CommonParsing.MPEG_VIDEO) 
					streamdemultiplexer.initVideo2(collection, fparent);

				else 
					streamdemultiplexer.init2(collection, fparent);
			}
		}

		/**
		 * init conversions 
		 */
		initConversion(collection, fparent, action, CommonParsing.ACTION_TO_PVA, job_processing.getSplitPart());

		/**
		 * d2v project 
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
		Object[] predefined_Pids = collection.getPIDs();

		int[] include = new int[predefined_Pids.length];

		for (int i = 0; i < include.length; i++) 
			include[i] = 0xFF & Integer.parseInt(predefined_Pids[i].toString().substring(2), 16);

		if (include.length > 0)
		{
			Arrays.sort(include);

			String str = " ";

			for (int i = 0; i < include.length; i++)
				str += "0x" + Integer.toHexString(include[i]).toUpperCase() + " ";

			Common.setMessage(Resource.getString("parsePVA.special.pids") + ": {" + str + "}");

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

			if (CommonParsing.getPvaPidExtraction()) 
				rawfile = new RawFile(fparent, 	CommonParsing.getPvaPidToExtract(), MainBufferSize);

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
			if (CutMode == CommonParsing.CUTMODE_BYTE && CutpointList.size() > 0 && CommonParsing.getCutCounter() == 0)
				startPoint = Long.parseLong(CutpointList.get(CommonParsing.getCutCounter()).toString()) - ((action == CommonParsing.ACTION_DEMUX) ? OverheadSize : 0);

			if (startPoint < 0)
				startPoint = count;

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
				Common.setMessage(Resource.getString("parsePVA.continue") + " " + aXInputFile);

			base = count;
			size = count + aXInputFile.length();

			PushbackInputStream in = new PushbackInputStream(aXInputFile.getInputStream(startPoint - base), pva_buffersize);

			count += (startPoint - base);

			overlapPVA(collection, overlapnext);

			Common.updateProgressBar((action == CommonParsing.ACTION_DEMUX ? Resource.getString("parsePVA.demuxing") : Resource.getString("parsePVA.converting")) + " " + Resource.getString("parsePVA.pvafile") + " " + aXInputFile.getName(), (count - base), (size - base));

			qexit = count + (0x100000L * Infoscan_Value);


			bigloop:
			while (true)
			{
				loop:
				while (count < size)
				{ 
					while (pause())
					{}

					if (CommonParsing.isProcessCancelled() || (CommonParsing.isInfoScan() && count > qexit))
					{ 
						CommonParsing.setProcessCancelled(false);
						job_processing.setSplitSize(0); 

						break bigloop; 
					}

					/**
					 * cut end reached 
					 */
					if (job_processing.getCutComparePoint() + 20 < job_processing.getSourceVideoFrameNumber())
					{
						ende = true;
						break bigloop; 
					}

					/**
					 * cut end reached 
					 */
					if (CutMode == CommonParsing.CUTMODE_BYTE && CutpointList.size() > 0)
					{
						if (CommonParsing.getCutCounter() == CutpointList.size() && (CommonParsing.getCutCounter() & 1) == 0)
						{
							if (count > Long.parseLong(CutpointList.get(CommonParsing.getCutCounter() - 1).toString()) + OverheadSize)
							{
								ende = true;
								break bigloop;
							}
						}
					}

					in.read(pva_packet, 0, pva_headerlength);

					/**
					 * check 0x4156__55 is PVA (ascii AV) 
					 */
					if (pva_packet[0] != 0x41 || pva_packet[1] != 0x56 || pva_packet[4] != 0x55)
					{
						if (Message_2 && !missing_syncword)
							Common.setMessage(Resource.getString("parsePVA.missing.sync") + " " + count);

						// fill complete buffer...
						in.read(pva_packet, pva_headerlength, pva_buffersize - pva_headerlength);

						// ... and search next startcode
						for (int i = pva_headerlength; i < pva_buffersize - 4; i++)
						{
							if (pva_packet[i] == 0x41 && pva_packet[i + 1] == 0x56 && pva_packet[i + 4] == 0x55)
							{
								in.unread(pva_packet, i, pva_buffersize - i);

								count += i;
								missing_syncword = true;
								Common.updateProgressBar((count - base), (size - base));

								continue loop;
							}
						}

						count += pva_buffersize;
						missing_syncword = true;

						continue loop;
					}

					if (Message_2 && missing_syncword)
						Common.setMessage(Resource.getString("parsePVA.found.sync") + " " + count);

					missing_syncword = false;

					/**
					 * overlapcheck 
					 */
					if (OverlapCheck && job_processing.getFileNumber() < collection.getPrimaryInputFileSegments() - 1)
					{
						in.read(pva_packet, pva_headerlength, overlapnext.length - pva_headerlength);

						for (int i = 255; i >= 0; i--)
						{
							if (pva_packet[i] != overlapnext[i])
								break;

							if (i == 0)
							{ 
								Common.setMessage(Resource.getString("parsePVA.file.overlap") + " " + count);
								break loop;
							}
						}

						in.unread(pva_packet, pva_headerlength, overlapnext.length - pva_headerlength);
					}

					/**
					 * mark for split
					 */
					job_processing.setLastHeaderBytePosition(count);
					job_processing.setCutByteposition(count);

					packet++;

					pva_pid     = 0xFF & pva_packet[2];
					pva_counter = 0xFF & pva_packet[3];                             
					ptsflag     = 0xFF & pva_packet[5];
					containsPts = (0x10 & ptsflag) != 0;
					pre_bytes   = 3 & ptsflag>>>2;
					post_bytes  = 3 & ptsflag;
					pva_payloadlength = (0xFF & pva_packet[6])<<8 | (0xFF & pva_packet[7]);

					pva_packetoffset = pva_headerlength;

					Common.updateProgressBar((count - base), (size - base));

					//yield();

					clv[5]++;

					if (Debug) 
						System.out.println(Resource.getString("parsePVA.packs") + clv[5] + " /pid " + pva_pid + " @ " + ((count * 100 / size)) + "% " + count);

					count += pva_headerlength;

					/**
					 * pid inclusion 
					 */
					if (usePidfilter && Arrays.binarySearch(include, pva_pid) < 0)
					{
						in.skip(pva_payloadlength);
						count += pva_payloadlength;

						continue loop;
					}

					/**
					 * read pts of video pid
					 */
					if (pva_pid == 1 && containsPts)
					{
						in.read(pva_packet, pva_headerlength, pva_ptslength);

						pts = CommonParsing.readPTS(pva_packet, pva_headerlength, pva_ptslength, !CommonParsing.BYTEREORDERING, true);
						job_processing.setPvaVideoPts(pts);

						count += pva_ptslength;
						pva_packetoffset += pva_ptslength;
						pva_payloadlength -= pva_ptslength;
					}

					else 
						job_processing.setPvaVideoPts(-1);


					/**
					 * re-formatted PES data extraction
					 */
					if (CommonParsing.getPvaPidExtraction())
					{
						if (pva_pid != CommonParsing.getPvaPidToExtract())
							in.skip(pva_payloadlength);

						else
						{
							if (pva_pid == 1)
							{
								if (containsPts)
								{
									CommonParsing.setPES_LengthField(new_pes_packetheader_and_pts, 0, 8 + pva_payloadlength);
									CommonParsing.setPES_PTSField(new_pes_packetheader_and_pts, 0, pts);

									rawfile.write(new_pes_packetheader_and_pts);
								}

								else
								{
									CommonParsing.setPES_LengthField(new_pes_packetheader, 0, 3 + pva_payloadlength);

									rawfile.write(new_pes_packetheader);
								}
							}

							in.read(pva_packet, pva_packetoffset, pva_payloadlength); 
							rawfile.write(pva_packet, pva_packetoffset, pva_payloadlength);

							job_processing.countMediaFilesExportLength(pva_payloadlength);
						}

						count += pva_payloadlength; 

						continue loop;
					}

					in.read(pva_packet, pva_packetoffset, pva_payloadlength);

					count += pva_payloadlength;

					/**
					 * raw pid filter extraction
					 */
					if (action == CommonParsing.ACTION_FILTER)
					{
						streamconverter.write(job_processing, pva_packet, 0, pva_packetoffset + pva_payloadlength, null, job_processing.getCutByteposition(), CommonParsing.isInfoScan(), CutpointList);
						continue loop;
					}

					foundObject = false;

					for (int i = 0; i < PVAPidlist.size(); i++)
					{
						streambuffer = (StreamBuffer)PVAPidlist.get(i);

						foundObject = pva_pid == streambuffer.getPID();

						if (foundObject)
							break; 
					}


					/**
					 * create new PID object
					 */
					if (!foundObject)
					{   
						streambuffer = new StreamBuffer();
						streambuffer.setPID(pva_pid);

						Common.setMessage(Resource.getString("parsePVA.found.id") + Integer.toHexString(pva_pid).toUpperCase() + " @ " + job_processing.getCutByteposition()); 

						Common.getGuiInterface().addPidToExtract(Integer.toHexString(pva_pid));
						PVAPidlist.add(streambuffer);
					} 

					/**
					 * out of sequence? 
					 */
					if (Message_1)
					{
						if (streambuffer.getCounter() != -1)
						{
							if (pva_counter != streambuffer.getCounter())
							{ 
								Common.setMessage(Resource.getString("parsePVA.outof.sequence", Integer.toHexString(pva_pid).toUpperCase(), "" + packet, "" + (count-8-pva_payloadlength), "" + pva_counter, "" + streambuffer.getCounter()) + " (~" + Common.formatTime_1( (long)((CommonParsing.getVideoFramerate() / 90.0f) * job_processing.getExportedVideoFrameNumber())) + ")");
								streambuffer.setCounter(pva_counter);
							}

							streambuffer.countPVA();
						}

						else
						{ 
							streambuffer.setCounter(pva_counter);
							streambuffer.countPVA();
						}
					}

					if (pva_pid != 1 && !containsPts && !ConformAudioCheck && pva_payloadlength > post_bytes + 3)
					{
						int p = pva_packetoffset + post_bytes;

						if (pva_packet[p] == 0 && pva_packet[p + 1] == 0 && pva_packet[p + 2] == 1 && (pva_packet[p + 3] == (byte)CommonParsing.PRIVATE_STREAM_1_CODE || (0xE0 & pva_packet[p + 3]) == 0xC0)) 
							containsPts = true;
					}

					if (!containsPts)
						post_bytes = 0;

					if (streambuffer.isStarted()) 
						streamdemultiplexer = (StreamDemultiplexer) demuxList.get(streambuffer.getID());

					else
					{   // create new ID object
						String IDtype = "";

						switch (pva_pid)
						{
						case 1:
							IDtype=Resource.getString("idtype.video");
							streamdemultiplexer = new StreamDemultiplexer(collection, ptsoffset);

							streambuffer.setStarted(true);

							streamdemultiplexer.setID(0xE0);
							streamdemultiplexer.setType(CommonParsing.MPEG_VIDEO);
							streamdemultiplexer.setnewID(newID[CommonParsing.MPEG_VIDEO]++);
							streamdemultiplexer.setPID(pva_pid);
							streamdemultiplexer.setsubID(0);
							streamdemultiplexer.setStreamType(pes_streamtype);

						//	streambuffer.setID(streamdemultiplexer.hashCode());
							streambuffer.setID(demuxList.size());
							demuxList.add(streamdemultiplexer);

							if (action == CommonParsing.ACTION_DEMUX) 
								streamdemultiplexer.initVideo(collection, fparent, MainBufferSize, demuxList.size(), CommonParsing.PVA_PARSER);
							else 
								IDtype += " " + Resource.getString("idtype.mapped.to.e0") + " " + streamtypes[3];

							break; 

						case 2:
							IDtype = Resource.getString("idtype.main.audio");
							//do not break

						default: 
							IDtype = Resource.getString("idtype.additional"); 

							if (!containsPts) 
								continue loop;

							int streamID = 0xFF & pva_packet[pva_packetoffset + post_bytes + 3];

							if ((0xE0 & streamID) != 0xC0 && streamID != CommonParsing.PRIVATE_STREAM_1_CODE)
							{ 
								streambuffer.setneeded(false); 
								break; 
							}

							streamdemultiplexer = new StreamDemultiplexer(collection, ptsoffset);

							streambuffer.setStarted(true);

							streamdemultiplexer.setPID(pva_pid);
							streamdemultiplexer.setType(streamID != CommonParsing.PRIVATE_STREAM_1_CODE ? CommonParsing.MPEG_AUDIO : CommonParsing.AC3_AUDIO);    // MPA?
							streamdemultiplexer.setID(streamID);
							streamdemultiplexer.setsubID(0);

							isTeletext = (0xFF & pva_packet[pva_packetoffset + post_bytes + 8]) == 0x24;

							streamdemultiplexer.setnewID((streamID == CommonParsing.PRIVATE_STREAM_1_CODE ? (isTeletext ? newID[CommonParsing.TELETEXT]++ : newID[CommonParsing.AC3_AUDIO]++) : newID[CommonParsing.MPEG_AUDIO]++));
							streamdemultiplexer.setTTX(isTeletext);
							streamdemultiplexer.setStreamType(pes_streamtype);

						//	streambuffer.setID(streamdemultiplexer.hashCode());
							streambuffer.setID(demuxList.size());
							demuxList.add(streamdemultiplexer);

							IDtype += " " + Resource.getString("idtype.has.pesid") + Integer.toHexString(streamID).toUpperCase() + " " + streamtypes[streamdemultiplexer.getType()];

							if (action == CommonParsing.ACTION_DEMUX) 
								streamdemultiplexer.init(collection, fparent, MainBufferSize / demuxList.size(), demuxList.size(), CommonParsing.PVA_PARSER);
							else 
								IDtype += " " + Resource.getString("idtype.mapped.to") + Integer.toHexString(streamdemultiplexer.getnewID()).toUpperCase();

							break; 
						}

						Common.setMessage(Resource.getString("parsePVA.id.0x") + Integer.toHexString(pva_pid).toUpperCase() + " " + IDtype);
					}


					if (!streamdemultiplexer.StreamEnabled())
						continue loop;

					/**
					 * packet to streamdemultiplexer
					 */
					if (action == CommonParsing.ACTION_DEMUX)
					{
						if (streamdemultiplexer.getType() == CommonParsing.MPEG_VIDEO) 
							streamdemultiplexer.writeVideo(job_processing, pva_packet, pva_packetoffset, pva_payloadlength, false, CutpointList, ChapterpointList);

						else
						{ 
							if (containsPts)
							{
								if (post_bytes > 0)
								{
									streamdemultiplexer.write(job_processing, pva_packet, pva_packetoffset, post_bytes, false);

									pva_packetoffset += post_bytes;
									pva_payloadlength -= post_bytes;
								}

								CommonParsing.setPES_LengthField(pva_packet, pva_packetoffset, pva_payloadlength - 6);
							}

							streamdemultiplexer.write(job_processing, pva_packet, pva_packetoffset, pva_payloadlength, containsPts);
						}

						if (streamdemultiplexer.getPTS() > lastpts) 
							lastpts = streamdemultiplexer.getPTS();

						/**
						 * split size reached 
						 */
						if (job_processing.getSplitSize() > 0 && job_processing.getSplitSize() < job_processing.getAllMediaFilesExportLength()) 
							break loop;

						continue loop;
					}

					/**
					 * packet to streamconverter
					 * create new pes_header + pts of video 
					 */
					if (pva_pid == 1)
					{
						if (containsPts)
						{
							System.arraycopy(new_pes_packetheader_and_pts, 0, new_pes_packet, 0, new_pes_packetheader_and_pts.length);
							System.arraycopy(pva_packet, pva_packetoffset, new_pes_packet, new_pes_packetheader_and_pts.length, pva_payloadlength);

							CommonParsing.setPES_LengthField(new_pes_packet, 0, new_pes_packetheader_and_pts.length - 6 + pva_payloadlength);
							CommonParsing.setPES_PTSField(new_pes_packet, 0, pts);
						}

						else
						{
							System.arraycopy(new_pes_packetheader, 0, new_pes_packet, 0, new_pes_packetheader.length);
							System.arraycopy(pva_packet, pva_packetoffset, new_pes_packet, new_pes_packetheader.length, pva_payloadlength);

							CommonParsing.setPES_LengthField(new_pes_packet, 0, new_pes_packetheader.length - 6 + pva_payloadlength);
						}
					}

					/**
					 * create new pes_header + pts of others
					 */
					else
					{
						/**
						 * flag defines existence of an own pes_header, immediately following the pva_header
						 * but is misused by several app's to include up to 3 alignment bytes (to DWORD) <- only allowed for video
						 */
						if (containsPts)
						{
							if (post_bytes > 0)
							{
								System.arraycopy(new_pes_packetheader, 0, new_pes_packet, 0, new_pes_packetheader.length);
								System.arraycopy(pva_packet, pva_packetoffset, new_pes_packet, new_pes_packetheader.length, post_bytes);

								CommonParsing.setPES_LengthField(new_pes_packetheader, 0, 3 + post_bytes);
								CommonParsing.setPES_IdField(new_pes_packetheader, 0, streamdemultiplexer.getID());

								streamconverter.write(job_processing, new_pes_packet, streamdemultiplexer, job_processing.getCutByteposition(), CommonParsing.isInfoScan(), CutpointList);

								pva_packetoffset += post_bytes;
								pva_payloadlength -= post_bytes;

								System.arraycopy(pva_packet, pva_packetoffset, new_pes_packet, new_pes_packetheader.length, pva_payloadlength);
							}

							else
								System.arraycopy(pva_packet, pva_packetoffset, new_pes_packet, 0, pva_payloadlength);

							CommonParsing.setPES_LengthField(new_pes_packet, 0, pva_payloadlength - 6);
						}

						else
						{
							System.arraycopy(new_pes_packetheader, 0, new_pes_packet, 0, new_pes_packetheader.length);
							System.arraycopy(pva_packet, pva_packetoffset, new_pes_packet, new_pes_packetheader.length, pva_payloadlength);

							CommonParsing.setPES_LengthField(new_pes_packet, 0, new_pes_packetheader.length - 6 + pva_payloadlength);
						}
					}

					CommonParsing.setPES_IdField(new_pes_packet, 0, streamdemultiplexer.getID());

					streamconverter.write(job_processing, new_pes_packet, streamdemultiplexer, job_processing.getCutByteposition(), CommonParsing.isInfoScan(), CutpointList);

					job_processing.setLastHeaderBytePosition(count);

					/**
					 * split size reached 
					 **/
					if ( job_processing.getSplitSize() > 0 && job_processing.getSplitSize() < job_processing.getAllMediaFilesExportLength()) 
						break loop;

				}  // end while loop


				if ( job_processing.getSplitSize() > 0 && job_processing.getSplitSize() < job_processing.getAllMediaFilesExportLength())
					break bigloop;

				if (job_processing.getFileNumber() < collection.getPrimaryInputFileSegments() - 1)
				{ 
					in.close();
					//System.gc();

					if (Concatenate && action == CommonParsing.ACTION_DEMUX)
					{
						ptsoffset = nextFilePTS(collection, CommonParsing.PVA_PARSER, 0, lastpts, job_processing.getFileNumber() + 1);

						if (ptsoffset == -1) 
							ptsoffset = 0; 

						else
						{
							for (int i = 0; i < demuxList.size(); i++)
							{
								streamdemultiplexer = (StreamDemultiplexer) demuxList.get(i);
								streamdemultiplexer.PTSOffset(ptsoffset);

								if (streamdemultiplexer.getPID() == 1) 
									streamdemultiplexer.resetVideo();
							}

							job_processing.setSequenceHeader(true);
							job_processing.setNewVideoStream(true);
						}

					//	job_processing.addCellTime(String.valueOf(job_processing.getExportedVideoFrameNumber()));
					}

					XInputFile nextXInputFile = (XInputFile) collection.getInputFile(job_processing.countFileNumber(+1));
					count = size;

					in = new PushbackInputStream(nextXInputFile.getInputStream(), pva_buffersize);
					size += nextXInputFile.length();
					base = count;

					Common.setMessage(Resource.getString("parsePVA.actual.vframes") + " " + job_processing.getExportedVideoFrameNumber());
					Common.setMessage(Resource.getString("parsePVA.continue") + " " + nextXInputFile + " (" + Common.formatNumber(nextXInputFile.length()) + " bytes) @ " + base);

					Common.updateProgressBar((action == CommonParsing.ACTION_DEMUX ? Resource.getString("parsePVA.demuxing") : Resource.getString("parsePVA.converting")) + " " + Resource.getString("parsePVA.pvafile") + " " + nextXInputFile.getName());

					overlapPVA(collection, overlapnext);
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

			if (job_processing.getSplitSize() == 0)
			{
				demuxList.clear();
				PVAPidlist.clear();
			}

			if (CommonParsing.getPvaPidExtraction())
				rawfile.close();

		} catch (IOException e2) { 

			Common.setExceptionMessage(e2);
		}

		return vptslog;
	}


}

