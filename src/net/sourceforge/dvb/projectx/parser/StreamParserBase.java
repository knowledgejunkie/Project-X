/*
 * @(#)StreamParser
 *
 * Copyright (c) 2005 by dvb.matt, All rights reserved.
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

import net.sourceforge.dvb.projectx.common.Common;
import net.sourceforge.dvb.projectx.common.Resource;
import net.sourceforge.dvb.projectx.common.Keys;
import net.sourceforge.dvb.projectx.common.JobCollection;
import net.sourceforge.dvb.projectx.common.JobProcessing;

import net.sourceforge.dvb.projectx.xinput.XInputFile;

import net.sourceforge.dvb.projectx.parser.CommonParsing;
//
import net.sourceforge.dvb.projectx.parser.StreamConverter;
import net.sourceforge.dvb.projectx.parser.StreamDemultiplexer;
import net.sourceforge.dvb.projectx.parser.StreamProcess;

import java.util.List;
import java.util.Hashtable;

/**
 * main thread
 */
public class StreamParserBase extends Object {

	public int ERRORCODE = 0;
	public int MainBufferSize = 8192000;

//
	public static StreamDemultiplexer streamdemultiplexer;
	public static StreamConverter streamconverter;

	public static List demuxList;
	public static Hashtable streamobjects;

	public static boolean CreateD2vIndex;
	public static boolean SplitProjectFile;

	public static String fchild;
	public static String fparent;

	public static long OverheadSize;


//

	/**
	 * 
	 */
	public StreamParserBase()
	{
		init();
	}

	/**
	 * 
	 */
	public void init()
	{
		MainBufferSize = Integer.parseInt(Common.getSettings().getProperty(Keys.KEY_MainBuffer));

		if (MainBufferSize <= 0)
			MainBufferSize = 4096000;


		streamdemultiplexer = null;
		streamconverter = null;
		demuxList = null;
		streamobjects = null;

		CreateD2vIndex = false;
		SplitProjectFile = false;

		OverheadSize = 2048000;

		fchild = "";
		fparent = "";
	}

	/**
	 * 
	 */
	public String parseStream(JobCollection collection, XInputFile aXInputFile, int pes_streamtype, int action, String vptslog)
	{
		return null;
	}

	/**
	 * 
	 */
	public void setFileName(JobCollection collection, JobProcessing job_processing, XInputFile aXInputFile)
	{
		setFileName(collection, job_processing, aXInputFile, "");
	}

	/**
	 * 
	 */
	public void setFileName(JobCollection collection, JobProcessing job_processing, XInputFile aXInputFile, String extension)
	{
		fchild = collection.getOutputName(aXInputFile.getName());
		fparent = collection.getOutputNameParent(fchild);

		// split part 
		fparent += job_processing.getSplitSize() > 0 ? "(" + job_processing.getSplitPart() + ")" : extension;
	}

	/**
	 * 
	 */
	public boolean pause()
	{
		return Common.waitingMainProcess();
	}

	/**
	 * nextfile PTS check
	 * 
	 * returns new pts offset to append
	 */
	public long nextFilePTS(JobCollection collection, int parser_type, int pes_streamtype, long lastpts, int file_number)
	{
		return nextFilePTS(collection, parser_type, pes_streamtype, lastpts, file_number, 0L);
	}

	/**
	 * nextfile PTS check
	 * 
	 * returns new pts offset to append
	 */
	public long nextFilePTS(JobCollection collection, int parser_type, int pes_streamtype, long lastpts, int file_number, long startPoint)
	{
		return nextFilePTS(collection, parser_type, pes_streamtype, lastpts, 0L, file_number, 0L);
	}

	/**
	 * nextfile PTS check
	 * 
	 * returns new pts offset to append
	 */
	public long nextFilePTS(JobCollection collection, int parser_type, int pes_streamtype, long lastpts, long ptsoffset, int file_number, long startPoint)
	{
		JobProcessing job_processing = collection.getJobProcessing();

		byte[] data;

		long pts = 0;

		int position = 0;
		int buffersize = collection.getSettings().getIntProperty(Keys.KEY_ScanBuffer);
		int pes_ID;

		boolean PVA_Audio = collection.getSettings().getBooleanProperty(Keys.KEY_PVA_Audio);
		boolean containsPts;

		lastpts &= 0xFFFFFFFFL; //ignore bit33 of lastpts

		if (collection.getPrimaryInputFileSegments() > file_number)
		{
			try {

				XInputFile aXinputFile = ((XInputFile) collection.getInputFile(file_number)).getNewInstance();

				if (aXinputFile == null)
					return -1L;

				data = new byte[buffersize];

				aXinputFile.randomAccessSingleRead(data, startPoint);

				switch (parser_type)
				{
				case CommonParsing.PVA_PARSER:    // pva

					int pva_pid;
					int ptsflag;
					int pva_payloadlength;

					loop:
					while (position < buffersize - 20)
					{
						if (data[position] != 0x41 || data[position + 1] != 0x56 || data[position + 4] != 0x55)
						{
							position++; 
							continue loop;
						}

						pva_pid = 0xFF & data[position + 2];
						ptsflag = 0xFF & data[position + 5];
						pva_payloadlength = (0xFF & data[position + 6])<<8 | (0xFF & data[position + 7]);
						containsPts = (0x10 & ptsflag) != 0;

		 				if (pva_pid == 1) //video
						{
							if (containsPts)
							{
								pts = CommonParsing.readPTS(data, position + 8, 4, !CommonParsing.BYTEREORDERING, true);
								break loop;
							} 
						}

						else if (pva_pid != 0) //mainaudio mpa and other pids
						{
							ptsloop:
							for (int i = position + 8, j = (PVA_Audio && !containsPts) ? position + 8: position + 11; i < j; i++)
							{
								if (CommonParsing.validateStartcode(data, i) < 0)
									continue ptsloop;

								pes_ID = CommonParsing.getPES_IdField(data, i);

								if (pes_ID != 0xBD && (0xF0 & pes_ID) != 0xC0) 
									continue ptsloop;

								if ((0x80 & data[i + 7]) == 0) 
									break ptsloop;

								pts = CommonParsing.getPTSfromBytes(data, i + 9); //returns 32bit

								break loop;
							}
						}

						position += 8 + pva_payloadlength;
					}

					break;

				case CommonParsing.PRIMARY_PES_PARSER:    // mpg

					int pes_payloadlength = 0;
					int ptslength = 0;
					int returncode;
					int pes_offset = 0;

					boolean nullpacket = false;

					loop:
					while (position < buffersize - 20)
					{
						if ((returncode = CommonParsing.validateStartcode(data, position)) < 0)
						{ 
							position += -returncode;
							continue loop; 
						}

						pes_ID = CommonParsing.getPES_IdField(data, position);

						if (pes_ID < 0xBA)
						{
							position += 3;
							continue loop; 
						}

						if (pes_ID == 0xBA)
						{ 
							position += 12;
							continue loop; 
						}

						pes_payloadlength = CommonParsing.getPES_LengthField(data, position);

						if (pes_payloadlength == 0)
							nullpacket = true;

						if ((0xF0 & pes_ID) != 0xE0 && (0xF0 & pes_ID) != 0xC0 && pes_ID != 0xBD )
						{ 
							position += 6 + pes_payloadlength;
							continue loop; 
						}

						if (pes_streamtype == CommonParsing.MPEG1PS_TYPE)
						{
							pes_offset = 6;

							skiploop:
							while(true)
							{
								switch (0xC0 & data[position + pes_offset])
								{
								case 0x40: 
									pes_offset += 2; 
									continue skiploop; 

								case 0x80: 
									pes_offset += 3; 
									continue skiploop; 

								case 0xC0: 
									pes_offset++; 
									continue skiploop; 

								case 0:
									break; 
								}

								switch (0x30 & data[position + pes_offset])
								{
								case 0x20:
									containsPts = true; 
									break skiploop; 

								case 0x30:  
									containsPts = true; 
									break skiploop; 

								case 0x10: 
									containsPts = false; 
									break skiploop; 

								case 0:  
									containsPts = false; 
									pes_offset++; 
									break skiploop; 
								}
							}
						}

						else
						{
							containsPts = (0x80 & data[position + 7]) != 0;
							pes_offset = 9;
						}

						if (containsPts)
							pts = CommonParsing.getPTSfromBytes(data, position + pes_offset);

						position += 6 + pes_payloadlength;

						if (nullpacket)
							break;
					}

					break;
				} // end switch

			} catch (IOException e) { 

				Common.setExceptionMessage(e);
			}
		}

	//Common.setMessage("AA " + pts + " / " + ptsoffset + " / " + (pts + ptsoffset) + " / " + lastpts);
	//re-use given ptsoffset (chapterpoints of vob)
	//	pts += ptsoffset;

		if (file_number == 0 && startPoint == 0)
		{  // need global offset?
			pts &= 0xFFFFFFFFL;

			String str = collection.getSettings().getProperty(Keys.KEY_PtsShift_Value);

			if (str.equals("auto"))
			{ 
				long newpts = ((pts / 324000000L) - 1L) * 324000000L;
				Common.setMessage(Resource.getString("nextfile.shift.auto", "" + (newpts / 324000000L)));

				return newpts;
			}

			else if (!str.equals("0"))
			{ 
				Common.setMessage(Resource.getString("nextfile.shift.manual", collection.getSettings().getProperty(Keys.KEY_PtsShift_Value)));

				return ((long)(Double.parseDouble(collection.getSettings().getProperty(Keys.KEY_PtsShift_Value)) * 324000000L));
			}

			else 
				return 0L;
		}
		else
		{
			pts -= job_processing.getNextFileStartPts();
			pts &= 0xFFFFFFFFL;

			long ret = 0L;

			if (Math.abs(pts - lastpts) < 900000)
				ret = -1L;

			else if (pts > lastpts)  
				ret = 0L;

			else 
				ret = ((lastpts + 1728000L) - pts); // offset is multiple of 40,24,32,33.1,8ms

			if (ret >= 0)
				Common.setMessage(Resource.getString("nextfile.next.file.start", Common.formatTime_1(pts / 90L), Common.formatTime_1(lastpts / 90L)));

			if (ret > 0)
				Common.setMessage(Resource.getString("nextfile.next.file.start.adaption", Common.formatTime_1(ret / 90L)));

			return ret;
		}
	}

	/**
	 * 
	 */
	public Hashtable getStreamObjects(JobProcessing job_processing)
	{
		return job_processing.getStreamObjects();
	}

	/**
	 * 
	 */
	public String processElementaryStreams(String vptslog, int action, int[] clv, JobCollection collection, JobProcessing job_processing)
	{
		if (action != CommonParsing.ACTION_DEMUX)
		{
			streamconverter.close(job_processing, CommonParsing.isInfoScan());
			return vptslog;
		}

		//finish video stream
		for (int i = 0, NumberOfVideostreams = 0; i < demuxList.size(); i++)
		{
			streamdemultiplexer = (StreamDemultiplexer) demuxList.get(i);

			if (streamdemultiplexer.getType() == CommonParsing.MPEG_VIDEO)
			{ 
				// accept only first video
				if (NumberOfVideostreams > 0)
				{
					Common.setMessage("!> further videostream found (PID 0x" + Integer.toHexString(streamdemultiplexer.getPID()).toUpperCase() + " / ID 0x" + Integer.toHexString(streamdemultiplexer.getID()).toUpperCase() + ") -> ignored");
					continue;
				}

				// d2v project 
				if (CreateD2vIndex || SplitProjectFile)
					job_processing.getProjectFileD2V().write(job_processing.getProjectFileExportLength(), job_processing.getExportedVideoFrameNumber());

				Common.setMessage("");
				Common.setMessage(formatIDString(Resource.getString("ExportPanel.Streamtype.MpgVideo"), streamdemultiplexer.getPID(), streamdemultiplexer.getID(), streamdemultiplexer.subID()));
				Common.setMessage(Resource.getString("video.msg.summary") + " " + job_processing.getExportedVideoFrameNumber() + "-" + clv[0] + "-" + clv[1] + "-" + clv[2] + "-" + clv[3] + "-" + clv[4]);

				vptslog = streamdemultiplexer.closeVideo(job_processing, collection.getOutputDirectory() + collection.getFileSeparator());

				NumberOfVideostreams++;
			}
		} 

		processNonVideoElementaryStreams(vptslog, action, clv, collection, job_processing);

		return vptslog;
	}

	/**
	 * 
	 */
	public void processNonVideoElementaryStreams(String vptslog, int action, int[] clv, JobCollection collection, JobProcessing job_processing)
	{
		processNonVideoElementaryStreams(vptslog, action, clv, collection, job_processing, null, null);
	}

	/**
	 * 
	 */
	public void processNonVideoElementaryStreams(String vptslog, int action, int[] clv, JobCollection collection, JobProcessing job_processing, List tempfiles, XInputFile aXInputFile)
	{
		//finish other streams
		int[] stream_number = job_processing.getStreamNumbers(); 

		String[] key_values = { 
			Keys.KEY_Streamtype_Ac3Audio[0],
			Keys.KEY_Streamtype_Teletext[0],
			Keys.KEY_Streamtype_MpgAudio[0],
			"",
			Keys.KEY_Streamtype_PcmAudio[0],
			Keys.KEY_Streamtype_Subpicture[0],
			Keys.KEY_Streamtype_Ac3Audio[0],
			Keys.KEY_Streamtype_PcmAudio[0]
		};

		for (int i = 0, es_streamtype; i < demuxList.size(); i++)
		{
			streamdemultiplexer = (StreamDemultiplexer) demuxList.get(i);
			es_streamtype = streamdemultiplexer.getType();

			if (es_streamtype == CommonParsing.MPEG_VIDEO) 
				continue;

			// from pva - test with others
			if (streamdemultiplexer.getID() == 0) 
				continue;

			String[] values = streamdemultiplexer.close(job_processing, vptslog);

			if (values[0].equals("")) 
			{
				Common.setMessage(formatIDString(Resource.getString("StreamParser.NoExport"), streamdemultiplexer.getPID(), 0xFF & streamdemultiplexer.getID(), streamdemultiplexer.subID()));
				continue;
			}

			if (streamdemultiplexer.getStreamNumber() < 0)
			{
				streamdemultiplexer.setStreamNumber(stream_number[es_streamtype]);
				stream_number[es_streamtype]++;
			}

			String newfile = values[3] + (streamdemultiplexer.getStreamNumber() > 0 ? ("-" + Common.adaptString(stream_number[es_streamtype], 2)) : "") + "." + values[2];

			Common.renameTo(values[0], newfile);
		
			values[0] = newfile;
			values[3] = vptslog;

			switch (es_streamtype)
			{
			case CommonParsing.AC3_AUDIO:
			case CommonParsing.DTS_AUDIO:
				if (streamdemultiplexer.subID() != 0 && (0xF0 & streamdemultiplexer.subID()) != 0x80)
				{
					Common.setMessage(formatIDString(Resource.getString("StreamParser.NoExport"), streamdemultiplexer.getPID(), 0xFF & streamdemultiplexer.getID(), streamdemultiplexer.subID()));
					break;
				}

			case CommonParsing.TELETEXT: 
			case CommonParsing.MPEG_AUDIO: 
			case CommonParsing.LPCM_AUDIO:
			case CommonParsing.SUBPICTURE:
				createStreamProcess(es_streamtype, collection, values, key_values[es_streamtype]);
				break;
			}

			// save infos for output segmentation
			if (tempfiles != null)
			{
/**/				tempfiles.add(values[0]);
				tempfiles.add(aXInputFile);
				tempfiles.add(values[1]);
				tempfiles.add(values[2]);
/**/
Common.setMessage("tmpfiles " + tempfiles.size());
				if (job_processing.getSplitSize() == 0)
				{
					new File(newfile).delete();
					new File(values[1]).delete();
				}
			}

			else
			{
				new File(newfile).delete();
				new File(values[1]).delete();
			}

		}
	}

	/**
	 * 
	 */
	public String formatIDString(String str1, int pid, int id, int subid)
	{
		String str = "+> " + str1;

		str += ": PID 0x" + Common.adaptString(Integer.toHexString(pid).toUpperCase(), 4);
		str += " / PesID 0x" + Common.adaptString(Integer.toHexString(id).toUpperCase(), 2);
		str += " / SubID 0x" + Common.adaptString(Integer.toHexString(subid).toUpperCase(), 2);
		str += " :";

		return str;
	}

	/**
	 * 
	 */
	public void createStreamProcess(int es_streamtype, JobCollection collection, String[] values, String key_value)
	{
		Common.setMessage("");
		Common.setMessage(formatIDString(Resource.getString(key_value), streamdemultiplexer.getPID(), streamdemultiplexer.getID(), streamdemultiplexer.subID()));

		new StreamProcess(es_streamtype, collection, values);
	}

	/**
	 *
	 */
	public void addCellTimeFromFileSegment(JobProcessing job_processing)
	{
		//addCellTime(job_processing);
	}

	/**	 *

	 */
	public void addCellTime(JobProcessing job_processing)
	{
		job_processing.addCellTime(job_processing.getExportedVideoFrameNumber());
	}

	/**
	 * init conversions 
	 */
	public void initConversion(JobCollection collection, String parent, int action, int source, int splitpart)
	{
		if (action <= CommonParsing.ACTION_DEMUX)
			return;

		String[] ext = { "", ".vdr", ".m2p", ".pva", ".ts" };

		switch (action)
		{
		case CommonParsing.ACTION_TO_VDR:
		case CommonParsing.ACTION_TO_M2P:
		case CommonParsing.ACTION_TO_PVA:
		case CommonParsing.ACTION_TO_TS:
			streamconverter.init(collection, parent + "[remux]" + ext[action], MainBufferSize, action, splitpart);
			break;

		case CommonParsing.ACTION_FILTER:
			streamconverter.init(collection, parent + "[filter]" + ext[source], MainBufferSize, action, splitpart);
		}
	}

	/**
	 * getOverhead to collect more samples
	 */
	public void setOverheadSize(JobCollection collection)
	{
		OverheadSize = collection.getSettings().getBooleanProperty(Keys.KEY_Input_useReadOverhead) ? 2048000 : 0;
	}
}
