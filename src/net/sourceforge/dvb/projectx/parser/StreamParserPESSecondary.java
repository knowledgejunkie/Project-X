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
public class StreamParserPESSecondary extends StreamParserBase {

	/**
	 * 
	 */
	public StreamParserPESSecondary()
	{
		super();
	}

	/**
	 * secondary PES Parser
	 */
	public String parseStream(JobCollection collection, XInputFile aXInputFile, int _pes_streamtype, int action, String vptslog)
	{
		String fchild = collection.getOutputName(aXInputFile.getName());
		String fparent = collection.getOutputNameParent(fchild);

		if (collection.getSettings().getBooleanProperty(Keys.KEY_ExternPanel_appendExtension))
			fparent = collection.getOutputDirectory() + collection.getFileSeparator() + fchild;

		JobProcessing job_processing = collection.getJobProcessing();

		job_processing.clearStatusVariables();
		int[] clv = job_processing.getStatusVariables();

		/**
		 * split part 
		 */
		fparent += job_processing.getSplitSize() > 0 ? "(" + job_processing.getSplitPart() + ")" : "" ;

		String paname = fparent + ".ma1";

		String file_id = aXInputFile.getFileID();
	
		List tempfiles = job_processing.getTemporaryFileList();

		if (!tempfiles.isEmpty())
		{
			for (int i = 0; i < tempfiles.size(); i += 4 )
			{
				if ( tempfiles.get(i + 1).toString().equals(aXInputFile.toString()) )
				{
					Common.renameTo(tempfiles.get(i).toString(), paname);

					tempfiles.set(i, paname);

					Common.setMessage(Resource.getString("parseSecondaryPES.continue") + " " + aXInputFile);

					String str = tempfiles.get(i + 3).toString();

					if (str.equals("tt")) //vtx
						new StreamProcess(CommonParsing.TELETEXT, collection, tempfiles.get(i).toString(), tempfiles.get(i + 2).toString(), tempfiles.get(i + 3).toString(), vptslog);

					else if (str.equals("sp")) //subpics
						new StreamProcess(CommonParsing.SUBPICTURE, collection, tempfiles.get(i).toString(), tempfiles.get(i + 2).toString(), tempfiles.get(i + 3).toString(), vptslog);

					else if (str.equals("pc")) //lpcm
						new StreamProcess(CommonParsing.LPCM_AUDIO, collection, tempfiles.get(i).toString(), tempfiles.get(i + 2).toString(), tempfiles.get(i + 3).toString(), vptslog);

					else //other audio
						new StreamProcess(CommonParsing.MPEG_AUDIO, collection, tempfiles.get(i).toString(), tempfiles.get(i + 2).toString(), tempfiles.get(i + 3).toString(), vptslog);

					return vptslog;
				}
			}
		}


		boolean Message_2 = collection.getSettings().getBooleanProperty(Keys.KEY_MessagePanel_Msg2);
		boolean Debug = collection.getSettings().getBooleanProperty(Keys.KEY_DebugLog);
		boolean SimpleMPG = collection.getSettings().getBooleanProperty(Keys.KEY_simpleMPG);
		boolean GetEnclosedPackets = collection.getSettings().getBooleanProperty(Keys.KEY_Input_getEnclosedPackets);
		boolean IgnoreScrambledPackets = collection.getSettings().getBooleanProperty(Keys.KEY_TS_ignoreScrambled);

		boolean isTeletext = false;
		boolean missing_startcode = false;
		boolean scrambling_messaged = false;
		boolean pes_isMpeg2;
		boolean pes_alignment;
		boolean pes_scrambled;
		boolean containsPts = false;
		boolean foundObject;

		int pes_streamtype;
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

		int tmp_value1 = 0;

		byte[] pes_packet = new byte[0x10006];
		byte[] buffered_data;

		long count = 0;
		long size;
		long qexit;


		job_processing.setMinBitrate(CommonParsing.MAX_BITRATE_VALUE);
		job_processing.setMaxBitrate(0);
		job_processing.setExportedVideoFrameNumber(0);
		job_processing.setEndPtsOfGop(-10000);
		job_processing.setSequenceHeader(true);
		job_processing.setAllMediaFilesExportLength(0);
		job_processing.setProjectFileExportLength(0);

		Hashtable substreams = new Hashtable();
		StandardBuffer sb;

		demuxList = job_processing.getSecondaryPESDemuxList();
		demuxList.clear();


		try {

			PushbackInputStream in = new PushbackInputStream(aXInputFile.getInputStream(), pes_packet.length);

			size = aXInputFile.length();

			Common.updateProgressBar(Resource.getString("parseSecondaryPES.demux.pes") + " " + aXInputFile.getName(), 0, 0);

			qexit = count + (0x100000L * Integer.parseInt(collection.getSettings().getProperty(Keys.KEY_ExportPanel_Infoscan_Value)));

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

					in.read(pes_packet, 0, pes_packetoffset);

					pesID = CommonParsing.getPES_IdField(pes_packet, 0);

					if ((returncode = CommonParsing.validateStartcode(pes_packet, 0)) < 0 || pesID < CommonParsing.SYSTEM_END_CODE)
					{
						returncode = returncode < 0 ? -returncode : 4;

						if (Message_2 && !missing_startcode)
							Common.setMessage(Resource.getString("parseSecondaryPES.missing.startcode") + " " + count);

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
						Common.setMessage(Resource.getString("parseSecondaryPES.found.startcode") + " " + count);

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
								in.skip(6);
								count += 12;
								continue loop;
							}

							else if ((0xC0 & pes_packet[4]) == 0x40) //mpg2
							{
								in.read(pes_packet, 6, 8);

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

						case CommonParsing.SYSTEM_START_CODE:
						case CommonParsing.PROGRAM_STREAM_MAP_CODE:
						case CommonParsing.PADDING_STREAM_CODE:
						case CommonParsing.PRIVATE_STREAM_2_CODE:
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

					pes_payloadlength = CommonParsing.getPES_LengthField(pes_packet, 0);

					if (pes_payloadlength == 0)
					{ 
						Common.setMessage(Resource.getString("parseSecondaryPES.packet.length") + " " + count);

						count += pes_packetoffset;
						continue loop;
					}

					in.read(pes_packet, pes_packetoffset, pes_payloadlength + 4);

					pes_packetlength = pes_packetoffset + pes_payloadlength;

					if (GetEnclosedPackets && CommonParsing.validateStartcode(pes_packet, pes_packetlength) < 0)
					{
						if (count + pes_packetlength < size)
						{
							if (Message_2 && !missing_startcode)
								Common.setMessage(Resource.getString("parseSecondaryPES.miss.next.startcode", String.valueOf(count + pes_packetlength), String.valueOf(count), Integer.toHexString(pesID).toUpperCase()));

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
						System.out.print("\r"+Resource.getString("parseSecondaryPES.packs", String.valueOf(clv[5]), String.valueOf((count * 100 / size)), String.valueOf(count)));

					pes_extensionlength = CommonParsing.getPES_ExtensionLengthField(pes_packet, 0);

					pes_isMpeg2 = (0xC0 & pes_packet[6]) == 0x80;
					pes_alignment = pes_isMpeg2 && (4 & pes_packet[6]) != 0;
					pes_scrambled = pes_isMpeg2 && (0x30 & pes_packet[6]) != 0;

					count += pes_packetlength;

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

							continue loop;
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
					pes_extension2_id = CommonParsing.getExtension2_Id(pes_packet, pes_headerlength, pes_payloadlength, pesID, pes_isMpeg2, count - pes_packetlength);

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
								//if ((pes_alignment && tmp_value1 == 0x20010000) || (!pes_alignment && tmp_value1 == 0x20010001))
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
										pes_packet[offset + 4] = (byte)(subID);								}
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
								continue loop;
							}

							// start packet, buffer this and get last completed packet
							else
							{
								buffered_data = sb.getData();

								sb.reset();
								sb.write(pes_packet, 0, pes_packetlength);

								if (buffered_data == null || buffered_data.length < 10)
									continue loop;

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
						String IDtype = "";

						switch (0xF0 & pesID)
						{
						case 0xE0: 
							IDtype = Resource.getString("idtype.mpeg.video.ignored");

							streamdemultiplexer = new StreamDemultiplexer(collection);
							streamdemultiplexer.setID(pesID);
							streamdemultiplexer.setsubID(0);
							streamdemultiplexer.setType(CommonParsing.MPEG_VIDEO);
							streamdemultiplexer.setStreamType(pes_streamtype);

							demuxList.add(streamdemultiplexer);
							break; 

						case 0xC0:
						case 0xD0:
							IDtype = Resource.getString("idtype.mpeg.audio"); 

							streamdemultiplexer = new StreamDemultiplexer(collection);
							streamdemultiplexer.setID(pesID);
							streamdemultiplexer.setsubID(0);
							streamdemultiplexer.setType(CommonParsing.MPEG_AUDIO);
							streamdemultiplexer.setStreamType(pes_streamtype);

							demuxList.add(streamdemultiplexer);
							streamdemultiplexer.init(collection, fparent, MainBufferSize / demuxList.size(), demuxList.size(), CommonParsing.SECONDARY_PES_PARSER);

							break; 
						}

						switch (pesID)
						{
						case CommonParsing.PRIVATE_STREAM_1_CODE:
							IDtype = Resource.getString("idtype.private.stream");
							IDtype += (isTeletext ? " TTX ": "") + (subID != 0 ? " (SubID 0x" + Integer.toHexString(subID).toUpperCase() + ")" : ""); 

							streamdemultiplexer = new StreamDemultiplexer(collection);
							streamdemultiplexer.setID(pesID);
							streamdemultiplexer.setsubID(subID);

							switch (subID>>>4)
							{
							case 1:
								streamdemultiplexer.setType(CommonParsing.TELETEXT);
								break;

							case 2:
							case 3:
								streamdemultiplexer.setType(CommonParsing.SUBPICTURE);
								break;

							case 8:
								streamdemultiplexer.setType(CommonParsing.AC3_AUDIO);
								break;

							case 0xA:
								streamdemultiplexer.setType(CommonParsing.LPCM_AUDIO);
							}

							streamdemultiplexer.setTTX(isTeletext);
							streamdemultiplexer.setStreamType(pes_streamtype);

							demuxList.add(streamdemultiplexer);
							streamdemultiplexer.init(collection, fparent, MainBufferSize / demuxList.size(), demuxList.size(), CommonParsing.SECONDARY_PES_PARSER);

							break; 
						}

						Common.setMessage(Resource.getString("parseSecondaryPES.found.pesid", Integer.toHexString(pesID).toUpperCase(), IDtype, "" + (count - 6 - pes_payloadlength))); 
					}

					if (!streamdemultiplexer.StreamEnabled())
						continue loop;

					if (streamdemultiplexer.getType() == CommonParsing.MPEG_VIDEO) 
						continue loop;

					else 
						streamdemultiplexer.write(job_processing, pes_packet, 0, pes_packetlength, true);
				}

				/**
				 * loop not yet used
				 */
				break bigloop;
			}

			Common.setMessage(Resource.getString("parseSecondaryPES.packs", String.valueOf(clv[5]), String.valueOf(count * 100 / size), String.valueOf(count)));

			in.close(); 

			processNonVideoElementaryStreams(vptslog, action, clv, collection, job_processing, tempfiles, aXInputFile);

		} catch (IOException e2) { 

			Common.setExceptionMessage(e2);
		}

		return vptslog;
	}
}
