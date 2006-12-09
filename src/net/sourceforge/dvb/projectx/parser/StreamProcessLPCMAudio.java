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
import java.io.RandomAccessFile;
import java.io.ByteArrayOutputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Date;
import java.util.TimeZone;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import net.sourceforge.dvb.projectx.common.Common;
import net.sourceforge.dvb.projectx.common.Resource;
import net.sourceforge.dvb.projectx.common.Keys;
import net.sourceforge.dvb.projectx.common.JobCollection;
import net.sourceforge.dvb.projectx.common.JobProcessing;

import net.sourceforge.dvb.projectx.xinput.XInputFile;

import net.sourceforge.dvb.projectx.parser.CommonParsing;
import net.sourceforge.dvb.projectx.parser.StreamConverter;
import net.sourceforge.dvb.projectx.parser.StreamDemultiplexer;

import net.sourceforge.dvb.projectx.parser.StreamProcessBase;

import net.sourceforge.dvb.projectx.audio.AudioFormat;

import net.sourceforge.dvb.projectx.io.IDDBufferedOutputStream;

/**
 * main thread
 */
public class StreamProcessLPCMAudio extends StreamProcessBase {

	/**
	 * 
	 */
	public StreamProcessLPCMAudio(JobCollection collection, XInputFile xInputFile, String filename_pts, String filename_type, String videofile_pts, int isElementaryStream)
	{
		super();

		processStream(collection, xInputFile, filename_pts, filename_type, videofile_pts, isElementaryStream);
	}

	/**
	 * LPCM stream
	 */
	private void processStream(JobCollection collection, XInputFile xInputFile, String filename_pts, String filename_type, String videofile_pts, int isElementaryStream)
	{

		String fchild = collection.getOutputName(xInputFile.getName());
		String fparent = collection.getOutputNameParent(fchild);

		JobProcessing job_processing = collection.getJobProcessing();

		String pcmfile = fparent + (isElementaryStream == CommonParsing.ES_TYPE ? ".new": "") + ".wav";

		byte[] parser = new byte[16];
		byte[] packet = new byte[0xFFFF];

		long size = xInputFile.length();
		long count = 0;
		long startPoint = 0;
		long time_difference = 0;
		long source_pts = 0;
		long new_pts = 0;
		long first_pts = -1;
		long packet_pts = 0;
		long ModeChangeCount = 0;

		boolean vptsdata = false;
		boolean ptsdata = false;
		boolean write = false;
		boolean missing_syncword = false;
		boolean newformat = false;
		boolean debug = collection.getSettings().getBooleanProperty(Keys.KEY_DebugLog);
		boolean message_2 = collection.getSettings().getBooleanProperty(Keys.KEY_MessagePanel_Msg2);

		int samples = 0;
		int x = 0;
		int v = 0;
		int packetlength = 0;
		int parserlength = parser.length;

		AudioFormat LPCM_Audio = new AudioFormat(CommonParsing.LPCM_AUDIO);

		try {

			PushbackInputStream in = new PushbackInputStream(xInputFile.getInputStream(), 20);

			IDDBufferedOutputStream out = new IDDBufferedOutputStream( new FileOutputStream(pcmfile), 2048000);

			Common.setMessage(Resource.getString("lpcm.msg.develop"));
			Common.setMessage(Resource.getString("lpcm.msg.tmpfile", xInputFile.getName(), "" + size));

			Common.updateProgressBar(Resource.getString("lpcm.progress") + " " + xInputFile.getName(), 0, 0);

			long[] ptsval = { 0 };
			long[] ptspos = { 0 };
			long[] vptsval = { 0 };

			long[][] obj = loadTempVideoPts(videofile_pts, debug);

			if (obj != null)
			{
				vptsval = obj[0];
				vptsdata = true;
				obj = null;
			}

			/*** 
			 * preloading audio PTS file is disabled, too big caused by too many PTS, will overload memory
			 * pts is here inluded in each packet instead
			 */
			ptsdata = true;

			if (vptsdata && ptsdata)
			{
				//int jump = checkPTSMatch(vptsval, ptsval);
				//temp. check disabled
				int jump = 0;

				if (jump < 0)
				{
					Common.setMessage(Resource.getString("lpcm.msg.pts.mismatch"));  
					vptsdata = false; 
					x = 0; 
				}

				else
					x = jump;
			}

			if (vptsdata && ptsdata)
			{
				Common.setMessage(Resource.getString("lpcm.msg.adjust.at.video"));
				time_difference = vptsval[0];
			}

			if (!vptsdata && ptsdata)
			{
				Common.setMessage(Resource.getString("lpcm.msg.adjust.at.own"));
				time_difference = 0;
			}

			if (ptsdata)
			{ 
				source_pts = ptsval[x]; 
				startPoint = ptspos[x]; 
			}

			//don't need it anymore
			ptsval = null;
			ptspos = null;

			while (count < startPoint)
				count += in.skip(startPoint - count);

			out.write(LPCM_Audio.getRiffHeader()); //wav header

			readloop:
			while ( count < size )
			{ 
				Common.updateProgressBar(count, size);

				//yield();

				while (pause())
				{}

				if (CommonParsing.isProcessCancelled())
				{ 
					CommonParsing.setProcessCancelled(false);
					job_processing.setSplitSize(0); 

					break readloop; 
				}

				//special X header
				in.read(parser, 0, parserlength);

				// find "PCM"
				if (parser[0] != 0x50 || parser[1] != 0x43 || parser[2] != 0x4D)
				{
					if (message_2 && !missing_syncword)
						Common.setMessage(Resource.getString("lpcm.msg.syncword.lost") + " " + count);

					in.unread(parser, 1, parserlength - 1);

					missing_syncword = true;
					count++;

					continue readloop;
				}

				if (message_2 && missing_syncword)
					Common.setMessage(Resource.getString("lpcm.msg.syncword.found") + " " + count);

				missing_syncword = false;
				packet_pts = 0;
				packetlength = ((0xFF & parser[8])<<8 | (0xFF & parser[9])) - 6;

				// packetlength <= 0 not handled !

				in.read(packet, 0, packetlength);

				count += parserlength;
				count += packetlength;

				// 5 bytes of packet pts, auslagern
				for (int a = 0; a < 5; a++)
					packet_pts |= (0xFFL & parser[3 + a])<<(a * 8);

				if (packet_pts != 0)
					source_pts = packet_pts;

				if (first_pts == -1)
					first_pts = source_pts;

				if (debug)
					System.out.println(" " + (count - packetlength) + "/ " + packetlength + "/ " + source_pts);

				if (LPCM_Audio.parseHeader(parser, 10) < 0)
					continue readloop;

				if (LPCM_Audio.compareHeader() > 0 || samples == 0 )
					newformat = true;

				LPCM_Audio.saveHeader();

				if (ptsdata)
				{
					write = !vptsdata;

					rangeloop:
					while (vptsdata && v < vptsval.length)  //sample_start_pts must be in range ATM
					{ 
						if (source_pts < vptsval[v])
							break rangeloop;

						else if (source_pts == vptsval[v] || source_pts < vptsval[v + 1])
						{
							write = true;
							break rangeloop;
						}

						v += 2;

						if (v < vptsval.length)
							time_difference += (vptsval[v] - vptsval[v - 1]);
					}
				}
				else
					write = true;

				if (write)
				{
					new_pts = source_pts - time_difference;

					if (newformat)
					{
						if (ModeChangeCount < 100) 
							Common.setMessage(Resource.getString("lpcm.msg.source", LPCM_Audio.displayHeader()) + " " + Common.formatTime_1( (long)(new_pts / 90.0f)));

						else if (ModeChangeCount == 100) 
							Common.setMessage(Resource.getString("lpcm.msg.source.max"));

						ModeChangeCount++;
						newformat = false;

						//yield();
					}

					Common.changeByteOrder(packet, 0, packetlength);

					if ((packetlength & 1) != 0)
						Common.setMessage(Resource.getString("lpcm.msg.error.align"));

					out.write(packet, 0, packetlength);

					samples++;

					Common.getGuiInterface().showExportStatus(Resource.getString("audio.status.write"));
				}

				else
					Common.getGuiInterface().showExportStatus(Resource.getString("audio.status.pause"));

				if (debug)
					System.out.println(" -> " + write + "/ " + v + "/ " + new_pts + "/ " + time_difference + "/ " + samples);

				Common.setFps(samples);
			}

			in.close();
			out.flush(); 
			out.close();

			if (filename_pts.equals("-1") || ptsdata)
				Common.setMessage(Resource.getString("lpcm.msg.pts.start_end", Common.formatTime_1(first_pts / 90)) + " " + Common.formatTime_1(source_pts / 90));

			Common.setMessage(Resource.getString("lpcm.msg.summary", " " + samples));

			File pcmfile1 = new File(pcmfile); 

			if (samples == 0) 
				pcmfile1.delete();

			else
			{ 
				long playtime = LPCM_Audio.fillRiffHeader(pcmfile); //update riffheader

				Common.setMessage(Resource.getString("msg.newfile") + " " + pcmfile);
				job_processing.countMediaFilesExportLength(pcmfile1.length());
				job_processing.addSummaryInfo(Resource.getString("lpcm.summary", Common.adaptString(job_processing.countPictureStream(), 2), "" + samples, Common.formatTime_1(playtime)) + infoPTSMatch(filename_pts, videofile_pts, vptsdata, ptsdata) + "\t'" + pcmfile1 + "'");
			}

		} catch (IOException e2) { 

			Common.setExceptionMessage(e2);
		}
	}


}
