/*
 * @(#)StreamParser
 *
 * Copyright (c) 2005-2009 by dvb.matt, All rights reserved.
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
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.BufferedWriter;
import java.io.FileWriter;

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
import net.sourceforge.dvb.projectx.io.IDDBufferedOutputStream;

import net.sourceforge.dvb.projectx.xinput.XInputFile;
import net.sourceforge.dvb.projectx.xinput.StreamInfo;

import net.sourceforge.dvb.projectx.parser.CommonParsing;
import net.sourceforge.dvb.projectx.parser.StreamConverter;
import net.sourceforge.dvb.projectx.parser.StreamDemultiplexer;
import net.sourceforge.dvb.projectx.parser.StreamParserBase;

import net.sourceforge.dvb.projectx.video.Video;

/**
 * main thread
 */
public class StreamParserESVideo extends StreamParserBase {

	private double videotimecount = 0.0;

	/**
	 * 
	 */
	public StreamParserESVideo()
	{
		super();
	}

	/**
	 * check video ES
	 */
	public String parseStream(JobCollection collection, XInputFile aXInputFile, int pes_streamtype, int action, String vptslog)
	{
		JobProcessing job_processing = collection.getJobProcessing();

		setFileName(collection, job_processing, aXInputFile, ".new");

        boolean CreateInfoIndex = collection.getSettings().getBooleanProperty(Keys.KEY_ExternPanel_createInfoIndex);
		boolean CreateM2sIndex = collection.getSettings().getBooleanProperty(Keys.KEY_ExternPanel_createM2sIndex);

		CreateD2vIndex = collection.getSettings().getBooleanProperty(Keys.KEY_ExternPanel_createD2vIndex);
		SplitProjectFile = collection.getSettings().getBooleanProperty(Keys.KEY_ExternPanel_splitProjectFile);

		boolean WriteVideo = collection.getSettings().getBooleanProperty(Keys.KEY_WriteOptions_writeVideo);
		boolean AddSequenceEndcode = collection.getSettings().getBooleanProperty(Keys.KEY_VideoPanel_addEndcode);
		boolean RenameVideo = collection.getSettings().getBooleanProperty(Keys.KEY_ExternPanel_renameVideo);
		boolean Debug = collection.getSettings().getBooleanProperty(Keys.KEY_DebugLog);
		boolean Overlap = collection.getSettings().getBooleanProperty(Keys.KEY_ExportPanel_Export_Overlap);
		boolean CreateCellTimes = collection.getSettings().getBooleanProperty(Keys.KEY_ExternPanel_createCellTimes);

		boolean first = true;
		boolean doWrite = true;
		boolean lead_sequenceheader = false;
		boolean isHeaderless = false;

		byte[] vgl = new byte[4];
		byte[] vptsbytes = new byte[16];
		byte[] vload = new byte[0];
		byte[] es_packet;

		long filelength = aXInputFile.length();
		long pos = 0;
		long pts = 0;
		long startPoint = 0;
		long Overlap_Value = 1048576L * (collection.getSettings().getIntProperty(Keys.KEY_ExportPanel_Overlap_Value) + 1);

		int CutMode =  collection.getSettings().getIntProperty(Keys.KEY_CutMode);
		int[] MPGVideotype = { 0 };
		int load = MainBufferSize / 2;
		int mark;
		int diff;
		int extension_index;
		int part;

		job_processing.clearStatusVariables();
		int[] clv = job_processing.getStatusVariables();

		double[] fps_tabl2 = { 0, 3753.7537, 3750, 3600, 3003.003, 3000, 1800, 1501.5015, 1500, 0,0,0,0,0,0,0 };

		String[] videoext = { ".mpv", ".mpv", ".m1v", ".m2v" };

		vptslog = "-1"; //fix

		Common.updateProgressBar(Resource.getString("video.progress") + " " + fchild, 0, 0);


		streamdemultiplexer = new StreamDemultiplexer(collection);

		try {

			PushbackInputStream in = new PushbackInputStream(aXInputFile.getInputStream(), 4);

			IDDBufferedOutputStream vstream = new IDDBufferedOutputStream( new FileOutputStream(fparent + ".s1"), MainBufferSize);

			/**
			 * M2s project 
			 */
			if (CreateM2sIndex)
				vstream.InitIdd(fparent, 1);

			/**
			 * CM project 
			 */
            if (CreateInfoIndex)
                vstream.InitInfo(fparent);

			DataOutputStream vlog = new DataOutputStream( new FileOutputStream(fparent + ".s1.pts") ); 
			vlog.write(CommonParsing.PTSVideoHeader);

			ByteArrayOutputStream es_packetbuffer = new ByteArrayOutputStream();

			job_processing.setElementaryVideoStream(true);
			job_processing.setMinBitrate(CommonParsing.MAX_BITRATE_VALUE);
			job_processing.setMaxBitrate(0);
			job_processing.setExportedVideoFrameNumber(0);
			job_processing.setEndPtsOfGop(-10000);
			job_processing.setAllMediaFilesExportLength(0);
			job_processing.setProjectFileExportLength(0);
			job_processing.setCutByteposition(0);

			/**
			 * d2v project 
			 */
			if (CreateD2vIndex || SplitProjectFile)
				job_processing.getProjectFileD2V().Init(fparent);

			/**
			 * split skipping first 
			 */
			if (job_processing.getSplitSize() > 0)
			{
				startPoint = job_processing.getLastHeaderBytePosition();
				startPoint -= !Overlap ? 0 : Overlap_Value;

				doWrite = false;

				/**
				 * set to 0, because we do not jump directly to the position due to possible audio sync-lost of elementary streams
				 */
				job_processing.setLastGopTimecode(0);
				job_processing.setLastGopPts(0);
				job_processing.setLastSimplifiedPts(0);

				/**
				if (pos < startPoint) 
					startPoint = pos;

				while (pos < startPoint)
					pos += in.skip(startPoint-pos);
				**/
			}

			List CutpointList = collection.getCutpointList();
			List ChapterpointList = collection.getChapterpointList();

			/**
			 * if you do so, there's no common entry point with audio anymore,
			 * therefore only one inputfile is allowed
			 * jump to first cut-in point
			 *
			if (CutMode == CommonParsing.CUTMODE_BYTE && CommonParsing.getCutCounter() == 0 && CutpointList.size() > 0)
			{
				long startPoint = Long.parseLong(CutpointList.get(CommonParsing.getCutCounter()).toString());

				while (pos < startPoint)
					pos += in.skip(startPoint-pos);
			}
			**/


			bigloop:
			while (pos < filelength)
			{
				while (pause())
				{}

				if (CommonParsing.isProcessCancelled())
				{
					CommonParsing.setProcessCancelled(false);
					job_processing.setSplitSize(0); 

					break bigloop;
				}

				/**
				 * cut end reached 
				 */
				if (job_processing.getCutComparePoint() + 20 < job_processing.getSourceVideoFrameNumber()) 
					break bigloop;

				load = (filelength - pos < (long)load) ? (int)(filelength - pos) : load;
				vload = new byte[load];

				in.read(vload);

				pos += load;

				Common.updateProgressBar(pos, filelength);

				//yield();

				mark = 0;

				loop:
				for (int i = 0, returncode, pes_ID; i < vload.length - 3; i++)
				{
					if (CutMode == CommonParsing.CUTMODE_BYTE && CutpointList.size() > 0)
					{
						if (CommonParsing.getCutCounter() == CutpointList.size() && (CommonParsing.getCutCounter() & 1) == 0)
							if (pos + i > Long.parseLong(CutpointList.get(CommonParsing.getCutCounter() - 1).toString()))
								break bigloop;
					}

					if ((returncode = CommonParsing.validateStartcode(vload, i)) < 0)
					{
						i += (-returncode) - 1; //note i++
						continue loop;
					}

					pes_ID = CommonParsing.getPES_IdField(vload, i);

					switch (pes_ID)
					{
					case CommonParsing.PICTURE_START_CODE:
						lead_sequenceheader = false;
						i += 3;
						continue loop;

					case CommonParsing.GROUP_START_CODE:

						if (lead_sequenceheader)
						{
							lead_sequenceheader = false;
							i += 8;
							continue loop;
						}
						// do not break

					case CommonParsing.SEQUENCE_HEADER_CODE:
					case CommonParsing.SEQUENCE_END_CODE:

						if (pes_ID == CommonParsing.SEQUENCE_HEADER_CODE)
							lead_sequenceheader = true;

						es_packetbuffer.write(vload, mark, i - mark);

						mark = i;
						job_processing.setCutByteposition(pos - load + mark);

						if (job_processing.getCutByteposition() >= startPoint)
							doWrite = true;

						//if (!first)
						if (!first && !isHeaderless) //changed
						{
							es_packet = es_packetbuffer.toByteArray();
							es_packetbuffer.reset();

							firstframeloop:
							for (int j = 0, pes_ID_temp; j < 6000 && j < es_packet.length - 5; j++)
							{
								if ((returncode = CommonParsing.validateStartcode(es_packet, j)) < 0)
								{
									j += (-returncode) - 1; //note j++
									continue firstframeloop;
								}

								pes_ID_temp = CommonParsing.getPES_IdField(es_packet, j);

								if (pes_ID_temp == CommonParsing.SEQUENCE_HEADER_CODE)
									videotimecount = fps_tabl2[15 & es_packet[j + 7]];

								else if (pes_ID_temp == CommonParsing.PICTURE_START_CODE)
								{
									pts = (long)(job_processing.getSourceVideoFrameNumber() == 0 ? videotimecount * ((0xFF & es_packet[j + 4])<<2 | (0xC0 & es_packet[j + 5])>>>6) : (videotimecount * ( (0xFF & es_packet[j + 4])<<2 | (0xC0 & es_packet[j + 5])>>>6 )) + job_processing.getLastSimplifiedPts());

									CommonParsing.setValue(vptsbytes, 0, 8, !CommonParsing.BYTEREORDERING, pts);

									break firstframeloop;
								}
							}

							// route through demuxer
							streamdemultiplexer.writeVideoES(job_processing, vstream, es_packet, vptsbytes, vlog, fparent, MPGVideotype, CutpointList, ChapterpointList, doWrite);
						}

						es_packetbuffer.reset();

						if (pes_ID == CommonParsing.SEQUENCE_END_CODE)
						{
							Common.setMessage(Resource.getString("video.msg.skip.sec", "" + clv[6]) + " " + (pos - load + i));
							i += 4;
							mark = i;
						}

						job_processing.setLastHeaderBytePosition(pos - load + i);  // split marker sequence_gop start

						/**
						 * split size reached
						 */
						if (job_processing.getSplitSize() > 0 && job_processing.getSplitSize() < job_processing.getAllMediaFilesExportLength()) 
							break bigloop;

						/**
						 * d2v split reached 
						 */
						if (SplitProjectFile && job_processing.getProjectFileExportLength() > job_processing.getProjectFileSplitSize())
						{
							part = job_processing.getProjectFileD2V().getPart() + 1;
							String newpart = fparent + "[" + part + "].mpv";

							/**
							 * sequence end code 
							 */
							if (WriteVideo && AddSequenceEndcode && job_processing.getExportedVideoFrameNumber() > 0 )
							{
								vstream.write(Video.getSequenceEndCode());
								job_processing.countMediaFilesExportLength(+4);
							}

							job_processing.countAllMediaFilesExportLength(+4);

							vstream.flush();
							vstream.close();
							//System.gc();

							vstream = new IDDBufferedOutputStream( new FileOutputStream(newpart), MainBufferSize - 1000000);

							if (CreateM2sIndex)
								vstream.InitIdd(newpart, 1);

                            if (CreateInfoIndex)
                                vstream.InitInfo(newpart);

							job_processing.getProjectFileD2V().setFile(newpart);
							job_processing.setProjectFileExportLength(0);
						}

						if (pes_ID == CommonParsing.GROUP_START_CODE)
						{
							job_processing.setSequenceHeader(false);

							if (job_processing.getSplitPart() > 0) 
								first = false;
						}

						else
						{
							job_processing.setSequenceHeader(true);
							first = false;
						}

//new
						isHeaderless = false;

						diff = (vload.length - mark - 4 < 2500) ? (vload.length - mark - 4) : 2500;
//new
						if (pes_ID == CommonParsing.SEQUENCE_END_CODE)
						{
							diff = 0;
							isHeaderless = true; //skip data between sequnece end + sequence start
						}
//
						if (diff > 0)
						{
							es_packetbuffer.write(vload, mark, diff);
							i += diff;
							mark = i;
						}

						break;

					default:
						i += 3;
						// do nothing
					}


					//overload
					if (es_packetbuffer.size() > 6144000)
					{
						Arrays.fill(vptsbytes, (byte)0);
						es_packetbuffer.reset(); 
						first = true;

						Common.setMessage(Resource.getString("demux.error.gop.toobig"));
					}
				}

				/**
				 * file end reached 
				 */
				if (pos >= filelength - 1)
				{ 
					if (job_processing.getSplitSize() > 0) 
						job_processing.setSplitLoopActive(false);

					break bigloop;
				}

				diff = vload.length < 3 ? vload.length : 3;

				es_packetbuffer.write(vload, mark, vload.length - mark - diff);
				in.unread(vload, vload.length - diff, diff); 

				pos -= diff;
			}


			/**
			 * d2v project
			 */
			if (CreateD2vIndex || SplitProjectFile)
				job_processing.getProjectFileD2V().write(job_processing.getProjectFileExportLength(), job_processing.getExportedVideoFrameNumber());


			/**
			 * sequence end code
			 */
			if (WriteVideo && AddSequenceEndcode && job_processing.getExportedVideoFrameNumber() > 0 )
			{
				vstream.write(Video.getSequenceEndCode());
				job_processing.countMediaFilesExportLength(+4);
			}

			in.close();

			vstream.flush();
			vstream.close();

			vlog.flush();
			vlog.close();

			Common.setMessage("");
			Common.setMessage(Resource.getString("video.msg.summary") + " " + job_processing.getExportedVideoFrameNumber() + "-" + clv[0] + "-" + clv[1] + "-" + clv[2] + "-" + clv[3] + "-" + clv[4]);

			File newfile = new File(fparent + ".s1");

			String videofile = "";

			if (newfile.length() < 20) 
				newfile.delete();

			else if (!WriteVideo) 
				newfile.delete();

			else
			{
				extension_index = (RenameVideo || CreateD2vIndex || SplitProjectFile) ? 0 : 2;

				videofile = fparent + videoext[MPGVideotype[0] + extension_index];
				newfile = new File(videofile);

				if (newfile.exists())
					newfile.delete();

				Common.renameTo(new File(fparent + ".s1"), newfile);

				vptslog = fparent + ".s1.pts";

				CommonParsing.setVideoHeader(job_processing, videofile, vptslog, clv, MPGVideotype);
			}

			if (CreateM2sIndex)
			{
				if (new File(videofile).exists())
				{
                    String tmpFN = videofile.toString();
					vstream.renameVideoIddTo(tmpFN);
				}

				else
					vstream.deleteIdd();
			}

            if (CreateInfoIndex)
			{
                if (new File(videofile).exists())
				{
                    String tmpFN = videofile.toString();
                    vstream.renameVideoInfoTo(tmpFN);
                }

                else
                    vstream.deleteInfo();
            }

			List cell = job_processing.getCellTimes();
			String workouts = collection.getOutputDirectory() + collection.getFileSeparator();

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

		} catch (IOException e) {

			Common.setExceptionMessage(e);
		}

		return vptslog;
	}

}
