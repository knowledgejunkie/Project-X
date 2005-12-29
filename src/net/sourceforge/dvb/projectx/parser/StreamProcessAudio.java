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

import net.sourceforge.dvb.projectx.io.IDDBufferedOutputStream;

import net.sourceforge.dvb.projectx.audio.MpaDecoder;
import net.sourceforge.dvb.projectx.audio.MpaConverter;
import net.sourceforge.dvb.projectx.audio.AudioFormat;
import net.sourceforge.dvb.projectx.audio.RIFFHeader;

import net.sourceforge.dvb.projectx.xinput.XInputFile;

import net.sourceforge.dvb.projectx.parser.CommonParsing;
import net.sourceforge.dvb.projectx.parser.StreamConverter;
import net.sourceforge.dvb.projectx.parser.StreamDemultiplexer;

import net.sourceforge.dvb.projectx.parser.StreamProcessBase;

/**
 * main thread
 */
public class StreamProcessAudio extends StreamProcessBase {

	private MpaConverter MPAConverter = null;
	private MpaDecoder MPADecoder = null;

	/**
	 * 
	 */
	public StreamProcessAudio(JobCollection collection, XInputFile xInputFile, String filename_pts, String filename_type, String videofile_pts, int isElementaryStream)
	{
		super();

		processStream(collection, xInputFile, filename_pts, filename_type, videofile_pts, isElementaryStream);
	}

	/**
	 * start method for adjusting audio at timeline
	 */
	private void processStream(JobCollection collection, XInputFile xInputFile, String filename_pts, String filename_type, String videofile_pts, int isElementaryStream)
	{
		Common.updateProgressBar(Resource.getString("audio.progress") + "  " + xInputFile.getName(), 0, 0);

		boolean Normalize = Common.getSettings().getBooleanProperty(Keys.KEY_AudioPanel_Normalize);
		boolean DecodeMpgAudio = Common.getSettings().getBooleanProperty(Keys.KEY_AudioPanel_decodeMpgAudio);

		if (MPAConverter == null)
			MPAConverter = new MpaConverter();

		if (MPADecoder == null)
			MPADecoder = new MpaDecoder();

		MpaDecoder.RESET = false;

		MpaDecoder.MAX_VALUE = Normalize ? (Integer.parseInt(Common.getSettings().getProperty(Keys.KEY_AudioPanel_NormalizeValue)) * 32767 / 100) : 32767;
		MpaDecoder.MULTIPLY = Normalize ? 32767 : 1;
		MpaDecoder.NORMALIZE = Normalize;
		MpaDecoder.PRESCAN = MpaDecoder.NORMALIZE;

		if (MpaDecoder.MAX_VALUE > 32767)
		{
			MpaDecoder.MAX_VALUE = 32767;
			Common.setMessage(Resource.getString("audio.msg.normalize.fixed") + " 100%");
		}

		/**
		 * messages
		 */
		int MpaConversionMode = Common.getSettings().getIntProperty(Keys.KEY_AudioPanel_loslessMpaConversionMode);

		if (MpaConversionMode > 0)
			Common.setMessage(Resource.getString("audio.convert") + " " + Keys.ITEMS_loslessMpaConversionMode[MpaConversionMode]);

		if (DecodeMpgAudio)
		{
			Common.setMessage(Resource.getString("audio.decode"));
			Common.setMessage("\t" + Keys.ITEMS_resampleAudioMode[Common.getSettings().getIntProperty(Keys.KEY_AudioPanel_resampleAudioMode)]);

			if (Normalize)
				Common.setMessage("\t" + Resource.getString(Keys.KEY_AudioPanel_Normalize[0]) + " " + (100 * MpaDecoder.MAX_VALUE / 32767) + "%");

			if (Common.getSettings().getBooleanProperty(Keys.KEY_AudioPanel_Downmix))
				Common.setMessage("\t" + Resource.getString(Keys.KEY_AudioPanel_Downmix[0]));
	
			if (Common.getSettings().getBooleanProperty(Keys.KEY_AudioPanel_changeByteorder))
				Common.setMessage("\t" + Resource.getString(Keys.KEY_AudioPanel_changeByteorder[0]));

			if (Common.getSettings().getBooleanProperty(Keys.KEY_AudioPanel_addRiffHeader))
				Common.setMessage("\t" + Resource.getString(Keys.KEY_AudioPanel_addRiffHeader[0]));

			if (Common.getSettings().getBooleanProperty(Keys.KEY_AudioPanel_addAiffHeader))
				Common.setMessage("\t" + Resource.getString(Keys.KEY_AudioPanel_addAiffHeader[0]));
		}

		/**
		 * restart loop
		 */
		while (processAudio(collection, xInputFile, filename_pts, filename_type, videofile_pts, isElementaryStream, MpaConversionMode))
		{
			CommonParsing.setAudioProcessingFlags(CommonParsing.getAudioProcessingFlags() & ~0xCL);

			Common.setMessage(" ");
			Common.setMessage(Resource.getString("audio.restart") + " " + ((CommonParsing.getAudioProcessingFlags()>>>18) - 1));

			if (DecodeMpgAudio && Normalize)
				Common.setMessage("-> normalize: multiply factor: " + MpaDecoder.MULTIPLY);

			if ( (0x10000L & CommonParsing.getAudioProcessingFlags()) != 0) 
				MpaConversionMode = 0;

			MPAConverter.resetBuffer();
		}

		CommonParsing.setAudioProcessingFlags(CommonParsing.getAudioProcessingFlags() & 3L);
	}


	/**
	 *  method for audio processing 
 	 */
	private boolean processAudio(JobCollection collection, XInputFile xInputFile, String filename_pts, String filename_type, String videofile_pts, int isElementaryStream, int MpaConversionMode)
	{
		String fchild = isElementaryStream == CommonParsing.ES_TYPE ? collection.getOutputName(xInputFile.getName()) : xInputFile.getName();
		String fparent = collection.getOutputNameParent(fchild);

		Common.getGuiInterface().showAVOffset(Resource.getString("MainPanel.AudioVideoOffset"));
		Common.getGuiInterface().showExportStatus(Resource.getString("MainPanel.nonVideoExportStatus"));

		JobProcessing job_processing = collection.getJobProcessing();

		if (isElementaryStream == CommonParsing.ES_TYPE && job_processing.getSplitSize() > 0) 
			fparent += "(" + job_processing.getSplitPart() + ")";

		String newnameL = fparent + ".$mpL$";
		String newnameR = fparent + ".$mpR$";

		boolean CreateM2sIndex = Common.getSettings().getBooleanProperty(Keys.KEY_ExternPanel_createM2sIndex);
		boolean AddRiffToMpgAudioL3 = Common.getSettings().getBooleanProperty(Keys.KEY_AudioPanel_addRiffToMpgAudioL3);
		boolean AddRiffToMpgAudio = Common.getSettings().getBooleanProperty(Keys.KEY_AudioPanel_addRiffToMpgAudio);
		boolean AddRiffToAc3 = Common.getSettings().getBooleanProperty(Keys.KEY_AudioPanel_addRiffToAc3);
		boolean Message_2 = Common.getSettings().getBooleanProperty(Keys.KEY_MessagePanel_Msg2);
		boolean PitchAudio = Common.getSettings().getBooleanProperty(Keys.KEY_AudioPanel_pitchAudio);
		boolean AllowSpaces = Common.getSettings().getBooleanProperty(Keys.KEY_AudioPanel_allowSpaces);
		boolean ValidateCRC = Common.getSettings().getBooleanProperty(Keys.KEY_AudioPanel_validateCRC);
		boolean ReplaceAc3withSilence = Common.getSettings().getBooleanProperty(Keys.KEY_AudioPanel_replaceAc3withSilence);
		boolean Patch1stAc3Header = Common.getSettings().getBooleanProperty(Keys.KEY_AudioPanel_patch1stAc3Header);
		boolean FillGapsWithLastFrame = Common.getSettings().getBooleanProperty(Keys.KEY_AudioPanel_fillGapsWithLastFrame);
		boolean LimitPts = Common.getSettings().getBooleanProperty(Keys.KEY_Audio_limitPts);
		boolean AddFrames = Common.getSettings().getBooleanProperty(Keys.KEY_AudioPanel_addFrames);
		boolean DownMix = Common.getSettings().getBooleanProperty(Keys.KEY_AudioPanel_Downmix);
		boolean ChangeByteorder = Common.getSettings().getBooleanProperty(Keys.KEY_AudioPanel_changeByteorder);
		boolean AddRiffHeader = Common.getSettings().getBooleanProperty(Keys.KEY_AudioPanel_addRiffHeader);
		boolean AddAiffHeader = Common.getSettings().getBooleanProperty(Keys.KEY_AudioPanel_addAiffHeader);
		boolean DecodeMpgAudio = Common.getSettings().getBooleanProperty(Keys.KEY_AudioPanel_decodeMpgAudio);
		boolean ClearCRC = Common.getSettings().getBooleanProperty(Keys.KEY_AudioPanel_clearCRC);
		boolean IgnoreErrors = Common.getSettings().getBooleanProperty(Keys.KEY_Audio_ignoreErrors);
		boolean CreateDDWave = Common.getSettings().getBooleanProperty(Keys.KEY_AudioPanel_createDDWave);

		/**
		 * messages
		 */
		if (IgnoreErrors)
			Common.setMessage("-> " + Resource.getString(Keys.KEY_Audio_ignoreErrors[0]));

		if (AllowSpaces)
			Common.setMessage("-> " + Resource.getString(Keys.KEY_AudioPanel_allowSpaces[0]));

		if (LimitPts)
			Common.setMessage("-> " + Resource.getString(Keys.KEY_Audio_limitPts[0]));

		if (ValidateCRC)
			Common.setMessage("-> " + Resource.getString(Keys.KEY_AudioPanel_validateCRC[0]));

		if (ClearCRC)
			Common.setMessage("-> " + Resource.getString(Keys.KEY_AudioPanel_clearCRC[0]));

		if (Patch1stAc3Header)
			Common.setMessage("-> " + Resource.getString(Keys.KEY_AudioPanel_patch1stAc3Header[0]));

		if (ReplaceAc3withSilence)
			Common.setMessage("-> " + Resource.getString(Keys.KEY_AudioPanel_replaceAc3withSilence[0]));

		if (FillGapsWithLastFrame)
			Common.setMessage("-> " + Resource.getString(Keys.KEY_AudioPanel_fillGapsWithLastFrame[0]));

		if (AddFrames)
			Common.setMessage("-> " + Resource.getString(Keys.KEY_AudioPanel_addFrames[0]));

		if (CreateDDWave)
			Common.setMessage("-> " + Resource.getString(Keys.KEY_AudioPanel_createDDWave[0]));


		int ResampleAudioMode = Common.getSettings().getIntProperty(Keys.KEY_AudioPanel_resampleAudioMode);
		int PitchValue = Integer.parseInt(Common.getSettings().getProperty(Keys.KEY_AudioPanel_PitchValue));

		boolean ptsdata = false;
		boolean vptsdata = false;
		boolean insertSilenceLoop = false;
		boolean awrite = false;
		boolean preloop = true;
		boolean newformat = true;
		boolean Debug = collection.DebugMode();
		boolean missing_syncword = false;
		boolean is_DTS = false;
		boolean is_AC3 = false;

		byte[] header_copy = new byte[4];
		byte[][] newframes = new byte[2][1];
		byte[][] copyframe= new byte[2][1];
		byte[][] silent_Frame=new byte[2][0];
		byte[] pushback = new byte[10];
		byte[] frame = new byte[1];
		byte[] pushmpa = new byte[4];
		byte[] push24 = new byte[24];

		long[] ptsval = {0};
		long[] ptspos = {0};
		long[] vptsval = {0};
		long[] vtime = {0};
		long n = 0;
		long actframe = 0;
		long timeline = 0;
		long audiosize = 0;
		long ModeChangeCount = 0;

		int pitch[] = { 1, PitchValue };
		int minSync = 0;
		int[] vw = new int[2];
		int cb = 0;
		int cc = 0;
		int cd = 0;
		int ce = 0;
		int i = 0;
		int pos = 0;
		int x = 0; 
		int layertype = -1;
		int v = 0;
		int w = 0;
		int frame_counter = 0;
		int layer = 0;
		int samplerate = 0;
		int lastheader = 0;
		int padding = 0;
		int jss = 0;
		int returncode = 0;
		int[] layermode2 = { 0,0,0,0 };

		final int AC3_AUDIOSTREAM = 0;
		final int MP3_AUDIOSTREAM = 1;
		final int MP2_AUDIOSTREAM = 2;
		final int MP1_AUDIOSTREAM = 3;
		final int DTS_AUDIOSTREAM = 4;
		final int WAV_AUDIOSTREAM = 5;
		final int NO_AUDIOSTREAM = 10;

		double time_counter = 0.0;

		int es_streamtype = CommonParsing.MPEG_AUDIO; //"mp" std

		/**
		 * mp, ac, wa
		 */
		if (filename_type.equals("ac")) //means dts, too
			es_streamtype = CommonParsing.AC3_AUDIO;

		else if (filename_type.equals("dt")) //later, other handling
			es_streamtype = CommonParsing.DTS_AUDIO;

		else if (filename_type.equals("wa"))
			es_streamtype = CommonParsing.LPCM_AUDIO;

		IDDBufferedOutputStream audiooutL;
		IDDBufferedOutputStream audiooutR;

		/**
		 * pre-init audioparser
		 */
		AudioFormat audio = new AudioFormat(es_streamtype);

		// pre-check for toggling ac3<->dts
		AudioFormat test_audio = new AudioFormat(CommonParsing.DTS_AUDIO);

		try {

			//System.gc();

			long[][] obj = loadTempOtherPts(filename_pts, "audio.msg.pts.discard", "audio.msg.pts.firstonly", "audio.msg.pts.start_end", "", 0, IgnoreErrors, Debug);

			if (obj != null)
			{
				ptsval = obj[0];
				ptspos = obj[1];
				ptsdata = true;
				obj = null;
			}

			obj = loadTempVideoPts(videofile_pts, Debug);

			if (obj != null)
			{
				vptsval = obj[0];
				vtime = obj[1];
				vptsdata = true;
				obj = null;
			}

			//System.gc();

			PushbackInputStream audioin = new PushbackInputStream(xInputFile.getInputStream(), 1000000);

			audiosize = xInputFile.length();

			long[] addf = { 0, 0 };

			if (audiosize < 1000) 
				Common.setMessage(" Filesize < 1000 byte");

			audiooutL = new IDDBufferedOutputStream(new FileOutputStream(newnameL), 2048000);

			if (MpaConversionMode >= 4) // half of mainbuffer 
				audiooutR = new IDDBufferedOutputStream(new FileOutputStream(newnameR), 2048000);

			else
				audiooutR = new IDDBufferedOutputStream(new FileOutputStream(newnameR));


			ByteArrayOutputStream silentFrameBuffer = new ByteArrayOutputStream();

			if (CreateM2sIndex)
			{
				audiooutL.InitIdd(newnameL, 2);
				audiooutR.InitIdd(newnameR, 2);
			}

			if (vptsdata && ptsdata)
			{
				int jump = checkPTSMatch(vptsval, ptsval);

				if (jump < 0)
				{
					Common.setMessage(Resource.getString("audio.msg.pts.mismatch"));  
					vptsdata = false; 
					x = 0; 
				}

				else
					x = jump;
			}

			if (vptsdata) 
				Common.setMessage(Resource.getString("audio.msg.adjust.at.videopts"));

			if (ptsdata && !vptsdata && isElementaryStream != CommonParsing.ES_TYPE) 
				Common.setMessage(Resource.getString("audio.msg.adjust.at.ownpts"));



			if (ptsdata)
			{ 
				timeline = ptsval[x]; 
				n = ptspos[x]; 
			}

			if (n > 0) 
				audioin.skip(n);

			/**
			 * riff wave header 
			 */
			RIFFHeader[] riffw = new RIFFHeader[2];

			riffw[0] = new RIFFHeader();  // normal, left
			riffw[1] = new RIFFHeader();  // right

			if (es_streamtype == CommonParsing.MPEG_AUDIO)
			{
				if (AddRiffToMpgAudioL3)
				{
					audiooutL.write(riffw[0].ACMnull());

					if (MpaConversionMode >= 4) 
						audiooutR.write(riffw[1].ACMnull());

					Common.setMessage(Resource.getString("audio.msg.addriff.acm"));
				}

				else if (AddRiffToMpgAudio)
				{
					audiooutL.write(riffw[0].BWFnull());

					if (MpaConversionMode >= 4) 
						audiooutR.write(riffw[1].BWFnull());

					Common.setMessage(Resource.getString("audio.msg.addriff.bwf"));
				}
			} 

			else if (AddRiffToAc3 && es_streamtype == CommonParsing.AC3_AUDIO)
			{
				audiooutL.write(riffw[0].AC3null());
				Common.setMessage(Resource.getString("audio.msg.addriff.ac3"));
			}

			else if (CreateDDWave && es_streamtype == CommonParsing.AC3_AUDIO)
				audiooutL.write(audio.getRiffHeader());

			else if (CreateDDWave && es_streamtype == CommonParsing.DTS_AUDIO)
				audiooutL.write(audio.getRiffHeader());


			bigloop:
			while (true)
			{
				/**
				 *  AC-3/DTS Audio
				 */
				readloopdd:
				while ((es_streamtype == CommonParsing.AC3_AUDIO || es_streamtype == CommonParsing.DTS_AUDIO) && n < audiosize - 10)
				{
					Common.updateProgressBar(n, audiosize);

					//yield();

					if (Debug) 
						System.out.println(" n" + n);

					while (pause())
					{}

					if (CommonParsing.isProcessCancelled())
					{ 
						CommonParsing.setProcessCancelled(false);
						job_processing.setSplitSize(0); 

						break bigloop; 
					}

					if (ptspos[x + 1] != -1 && n > ptspos[x + 1])
					{
						Common.setMessage(Resource.getString("audio.msg.pts.wo_frame") + " (" + ptspos[x + 1] + "/" + n + ")");
						x++;
					}

					/** 
					 * read 10 bytes for headercheck 
					 */
					audioin.read(pushback, 0, 10);
					n += 10;

					/**
					 * parse header 
					 */
					ERRORCODE = (is_AC3 || !is_DTS) ? audio.parseHeader(pushback, 0) : 0; 

					if (ERRORCODE < 1)
					{ 
						if (!is_AC3 || is_DTS)
							ERRORCODE = test_audio.parseHeader(pushback, 0); 

						if (ERRORCODE < 1)
						{ 
							audioin.unread(pushback, 1, 9); 

							if (Message_2 && !missing_syncword)
								Common.setMessage(Resource.getString("audio.msg.syncword.lost", " " + (n - 10)) + " " + Common.formatTime_1((long)(time_counter / 90.0f)));

							missing_syncword = true; 
							n -= 9; 

							continue readloopdd; 
						} 

						is_DTS = true; 
						is_AC3 = false;

						//set special type
						audio = new AudioFormat(CommonParsing.DTS_AUDIO);
						audio.parseHeader(pushback, 0);
					}

					else
					{ 
						is_DTS = false;
						is_AC3 = true;
					}

					audiooutL.setWave(CreateDDWave, is_AC3, is_DTS, audio.getBitrate());

					/**
					 * prepare fo read entire frame 
					 */
					audioin.unread(pushback);
					n -= 10;

					/**
					 * read entire frame 
					 */
					frame = new byte[audio.getSize()];
					audioin.read(frame, 0, audio.getSize());

					/**
					 * startfileposition of current frame 
					 */
					actframe = n;

					/**
					 * expected position for following frame 
					 */
					n += audio.getSize();

					if (PitchAudio)
					{  // skip a frame
						if (pitch[1] * pitch[0] == frame_counter)
						{
							Common.setMessage(Resource.getString("audio.msg.frame.discard") + " " + frame_counter + " (" + pitch[0] + ")");
							pitch[0]++;

							continue readloopdd;
						}
					}

					/**
					 * finish loop if last frame in file is shorter than nominal size 
					 */
					if ( n > audiosize ) 
						break readloopdd; 

					/**
					 * read following frame header, not if it is the last frame 
					 * check following frameheader for valid , if not starting with next byte 
					 */
					if (n < audiosize - 10)
					{
						int d = 0;

						if (!AllowSpaces)
						{
							audioin.read(push24, 0, 24);

							miniloop:
							for (; d < (is_DTS ? 15 : 17); d++)
							{ //smpte
								ERRORCODE = audio.parseNextHeader(push24, d);

								if (ERRORCODE > 0) 
									break miniloop; 
							} 

							audioin.unread(push24); 
						}

						if (ERRORCODE < 1)
						{ 
							audioin.unread(frame, 1, frame.length - 1);
							n = actframe + 1; 

							continue readloopdd; 
						}
						else
						{
							layertype = is_DTS ? DTS_AUDIOSTREAM : AC3_AUDIOSTREAM;
							audioin.skip(d);
							n += d;
						}
					}

					if (ValidateCRC && (ERRORCODE = audio.validateCRC(frame, 2, audio.getSize())) != 0 )
					{
						Common.setMessage(Resource.getString("audio.msg.crc.error", "" + ERRORCODE) + " " + actframe);

						audioin.unread(frame, 2, frame.length - 2);
						n = actframe + 2;

						continue readloopdd; 
					}

					if (Message_2 && missing_syncword)
						Common.setMessage(Resource.getString("audio.msg.syncword.found") + " " + actframe);

					missing_syncword = false;

					/**
					 * check for change in frametype 
					 */ 
					if (audio.compareHeader() > 0) 
						newformat = true; 

					if (frame_counter == 0) 
						newformat = true;

					audio.saveHeader();

					/**
					 * replace not 3/2 with silence ac3 3/2 061i++ 
					 */
					if (!is_DTS && ReplaceAc3withSilence && audio.getMode() != 7 )
					{
						for (int c = 0; c < Common.getAC3list().size(); c++)
						{
							byte[] ac3data = (byte[]) Common.getAC3list().get(c);

							if ( ((0xE0 & ac3data[6])>>>5) != 7 ) 
								continue;

							frame = new byte[ac3data.length];

							System.arraycopy(ac3data, 0, frame, 0, frame.length);

							break;
						}
					}

					// timeline ist hier aktuelle audiopts

					Common.setFps(frame_counter);

					/**
					 * preloop if audio starts later than video, and i must insert 
					 */
					if ( (preloop && v>=vptsval.length) || !( preloop && vptsdata && vptsval[v] < timeline - (audio.getFrameTimeLength() / 2.0) ) ) 
						preloop=false;

					else
					{
						/**
						 * patch ac-3 to 3/2 
						 */
						if (!is_DTS && Patch1stAc3Header && frame_counter == 0)
							frame = audio.editFrame(frame, audio.getSize(), 1);
						//	frame[6] = (byte)((0xF & frame[6]) | 0xE0);

						long precount = vptsval[v];
						long[] ins = { (long)time_counter, 0 };

						silentFrameBuffer.reset();

						/**
						 * insert silence ac3
						 */
						if (!is_DTS && !FillGapsWithLastFrame)
						{
							for (int c = 0; c < Common.getAC3list().size(); c++)
							{
								byte[] ac3data = (byte[]) Common.getAC3list().get(c);

								if ( (0xFE & ac3data[4]) != (0xFE & frame[4]) || ( (7 & ac3data[5]) != (7 & frame[5]) ) || (0xE0&ac3data[6])!=(0xE0&frame[6]) ) 
									continue;

								silentFrameBuffer.write(ac3data);

								break;
							}
						}
						else 
							silentFrameBuffer.write(frame);


						/**
						 * pre inserting
						 */
						while (precount < timeline - (audio.getFrameTimeLength() / 2.0))
						{
							/**
							 * check if frame write should paused 
							 */
							if (vptsdata && w < vptsval.length)
							{ 
								double ms1 = (double) (precount - vptsval[w + 1]);
								double ms2 = (double) (time_counter - vtime[w + 1]);

								if ((double) Math.abs(ms2) <= audio.getFrameTimeLength() / 2.0 )
								{
									awrite = false;
									w += 2;
								}
								else if ((double) Math.abs(ms1) <= audio.getFrameTimeLength() / 2.0 )
								{
									awrite = false;
									w += 2;
								}
							}

							/**
							 * calculate A/V Offset for true 
							 */
							if (vptsdata && (v < vptsval.length))
							{
								double ms3 = precount - vptsval[v], ms4 = time_counter - vtime[v];

								if (Debug) 
									System.out.println(" ö" + ms3 + "/" + ms4 + "/" + (ms4 - ms3));

								if (!awrite && (double) Math.abs((time_counter - vtime[v]) - (precount - vptsval[v])) <= (double) audio.getFrameTimeLength() / 2.0 )
								{
									awrite = true;
									v += 2;
									double ms1 = precount - vptsval[v - 2];
									double ms2 = time_counter - vtime[v - 2];

									Common.getGuiInterface().showAVOffset("" + (int)(ms1 / 90) + "/" + (int)(ms2 / 90) + "/" + (int)((ms2 - ms1) / 90));

									if (Debug) 
										System.out.println(" ä" + ms1 + "/" + ms2 + "/" + (ms2 - ms1));
								}
							} 

							/**
							 * calculate A/V Offset for true 
							 */
							if ((v < vptsval.length) )
							{
								if ((double) Math.abs(vptsval[v] - precount) <= ((double) audio.getFrameTimeLength() / 2.0) )
								{
									awrite = true;
									v += 2;
									double ms1 = precount - vptsval[v - 2];
									double ms2 = time_counter - vtime[v - 2];

									Common.getGuiInterface().showAVOffset("" + (int)(ms1 / 90) + "/" + (int)(ms2 / 90) + "/" + (int)((ms2 - ms1) / 90));

									if (Debug) 
										System.out.println(" ü" + ms1 + "/" + ms2 + "/" + (ms2 - ms1));
								}

								/**
								 * calculate A/V Offset for false 
								 */
								if (awrite && (double) Math.abs((time_counter - vtime[v - 2]) - (precount - vptsval[v-2])) > (double) audio.getFrameTimeLength() / 2.0 )
								{
									awrite = false;
									v -= 2;
								}
							}

							/**
							 * write message 
							 */
							if (awrite || !vptsdata)
								Common.getGuiInterface().showExportStatus(Resource.getString("audio.status.pre-insert"));

							else
								Common.getGuiInterface().showExportStatus(Resource.getString("audio.status.pause"));

							/**
							 * stop if no more audio needed 
							 */
							if (precount > vptsval[vptsval.length - 1] + 10000)
							{
								Common.updateProgressBar(audiosize, audiosize);

								break readloopdd;
							}

							if (awrite)
							{
								silentFrameBuffer.writeTo(audiooutL);

								/**
								 * RIFF 
								 */
								if (!is_DTS && AddRiffToAc3) 
									riffw[0].AC3RiffData(audio.parseRiffData(frame)); 

								frame_counter++;
								cb++;
								ins[1]++;
								time_counter += audio.getFrameTimeLength();
							}

							precount += audio.getFrameTimeLength();

							if (Debug) 
								System.out.println("(6)audio frames: wri/pre/skip/ins/add " + frame_counter + "/" + cb + "/" + ce + "/" + cc + "/" + cd + "  @ " + Common.formatTime_1((long)(time_counter / 90.0) ) + "  ");

						} // end while

						n = actframe;

						audioin.unread(frame);

						if (ins[1] > 0)
							Common.setMessage(Resource.getString("audio.msg.summary.pre-insert", "" + ins[1], FramesToTime((int)ins[1], audio.getFrameTimeLength())) + " " + Common.formatTime_1(ins[0] / 90L));

						continue readloopdd;
					} // end if preloop


					/****** check if frame write should paused *****/
					if (vptsdata)
					{ 
						vw[0] = v;
						vw[1] = w;

						awrite = SyncCheck(vw, time_counter, audio.getFrameTimeLength(), timeline, frame_counter, vptsval, vtime, awrite, Debug);

						v = vw[0];
						w = vw[1];
					}
					//System.out.println(""+awrite+"/"+v+"/"+w);


					/**
					 * message
					 */
					if (awrite || !vptsdata) 
						Common.getGuiInterface().showExportStatus(Resource.getString("audio.status.write"));

					else 
						Common.getGuiInterface().showExportStatus(Resource.getString("audio.status.pause"));

					if (Debug) 
						System.out.println(" k)" + timeline + " l)" + (audio.getFrameTimeLength() / 2.0) + " u)" + audio.getSize() + " m)" + awrite + " n)"+w+" o)"+v+" p)"+n);

					/**
					 * stop if no more audio needed 
					 */
					if (vptsdata && timeline > vptsval[vptsval.length - 1] + 10000)
					{
						Common.updateProgressBar(audiosize, audiosize);

						break readloopdd;
					}

					/**
					 * message
					 */
					if ((newformat && awrite) || (newformat && !vptsdata))
					{
						String hdr = audio.displayHeader();

						if (ModeChangeCount < 100) 
						{
							String str = Common.formatTime_1((long)(time_counter / 90.0f));

							Common.setMessage(Resource.getString("audio.msg.source", hdr) + " " + str);

							if (Common.getSettings().getBooleanProperty(Keys.KEY_ExternPanel_createChapters))
								job_processing.getChapters().addChapter(str, hdr);
						}

						else if (ModeChangeCount == 100) 
							Common.setMessage(Resource.getString("audio.msg.source.max"));

						else if (Debug) 
							System.out.println("=> src_audio: " + hdr + " @ " + Common.formatTime_1((long)(time_counter / 90.0f)));

						ModeChangeCount++;
						newformat = false;

						//yield();
					}

					/**
					 * patch ac-3 to 3/2 
					 */
					if (!is_DTS && Patch1stAc3Header && frame_counter == 0)
						frame = audio.editFrame(frame, audio.getSize(), 1);
						//	frame[6] = (byte)((0xF & frame[6]) | 0xE0);

					if (Debug) 
						System.out.println("(7)audio frames: wri/pre/skip/ins/add " + frame_counter + "/" + cb + "/" + ce + "/" + cc + "/" + cd + "  @ " + Common.formatTime_1((long)(time_counter / 90.0f) ));

					if (Debug) 
						System.out.println(" x" + ((x < ptspos.length - 1) ? x + "/" + ptsval[x + 1] + "/" + ptspos[x + 1] : "-"));

					/**
					 * pts for next frame!! 
					 */
					timeline += audio.getFrameTimeLength();

					silentFrameBuffer.reset();
					silentFrameBuffer.write(frame);

					/**
					 * simple sync
					 */
					if (LimitPts && ptspos[x + 1] != -1 && ptspos[x + 1] < n)
					{
						if (Debug)
							System.out.println(" minSync " + minSync + "/ " + x);

						if ( (++minSync) < 20)
							x++;
						else
							minSync = 0;
					}

					if ( (ptspos[x + 1] == -1) || (ptspos[x + 1] > n ) )
					{
						if (!vptsdata || (vptsdata && awrite))
						{
							audiooutL.write(frame);

							/**
							 * RIFF 
							 */
							if (!is_DTS && AddRiffToAc3) 
								riffw[0].AC3RiffData(audio.parseRiffData(frame)); 

							frame_counter++;
							time_counter += audio.getFrameTimeLength();
						}

						continue readloopdd;
					}

					minSync = 0;

					if ( (double) Math.abs(ptsval[x + 1] - timeline) < (double) audio.getFrameTimeLength() / 2.0 )
					{
						timeline = ptsval[x + 1];
						x++;

						if (!vptsdata || (vptsdata && awrite)) {
							audiooutL.write(frame);

							/**
							 * RIFF 
							 */
							if (!is_DTS && AddRiffToAc3) 
								riffw[0].AC3RiffData(audio.parseRiffData(frame)); 

							frame_counter++;
							time_counter += audio.getFrameTimeLength();
						}

						continue readloopdd;
					}

					if (ptsval[x + 1] > timeline) 
						insertSilenceLoop = true;

					if (ptsval[x + 1] < timeline)
					{
						x++;
						timeline = ptsval[x];

						Common.setMessage(Resource.getString("audio.msg.summary.skip") + " " + Common.formatTime_1((long)time_counter / 90L));

						ce++;
					}

					if (insertSilenceLoop)
					{
						/**
						 * test , write the actual frame and then loop to fill
						 */
						if (!vptsdata || (vptsdata && awrite))
						{
							audiooutL.write(frame);

							/**
							 * RIFF 
							 */
							if (!is_DTS && AddRiffToAc3) 
								riffw[0].AC3RiffData(audio.parseRiffData(frame)); 

							frame_counter++;
							time_counter += audio.getFrameTimeLength();

							if (Debug) 
								System.out.println("(10)audio frames: wri/pre/skip/ins/add " + frame_counter + "/" + cb + "/" + ce + "/" + cc + "/" + cd + "  @ " + Common.formatTime_1((long)(time_counter / 90.0) ) + "  ");
						}

						timeline += audio.getFrameTimeLength();

						/**
						 * insert silence ac3
						 */
						if (!is_DTS && !FillGapsWithLastFrame)
						{
							for (int c = 0; c < Common.getAC3list().size(); c++)
							{
								byte[] ac3data = (byte[]) Common.getAC3list().get(c);

								if ( (0xFE & ac3data[4]) != (0xFE & frame[4]) || ( (7 & ac3data[5]) != (7 & frame[5]) ) || (0xE0 & ac3data[6]) != (0xE0 & frame[6]) ) 
									continue;

								silentFrameBuffer.reset();
								silentFrameBuffer.write(ac3data);

								break;
							}
						}

						long[] ins = { (long)time_counter, 0 };


						while (ptsval[x + 1] > (timeline - (audio.getFrameTimeLength() / 2.0)) )
						{
							if (vptsdata && w < vptsval.length)
							{ 
								double ms1 = (double) (timeline - audio.getFrameTimeLength() - vptsval[w + 1]);
								double ms2 = (double) (time_counter - vtime[w + 1]);

								if ((double) Math.abs(ms2) <= audio.getFrameTimeLength() / 2.0 )
								{
									awrite = false;
									w += 2;
								}

								else if ((double) Math.abs(ms1) <= audio.getFrameTimeLength() / 2.0 )
								{
									awrite = false;
									w += 2;
								}
							}

							if (vptsdata && v < vptsval.length)
							{
								if (!awrite && (double) Math.abs((time_counter - vtime[v]) -
										(timeline - audio.getFrameTimeLength() - vptsval[v]) ) <= (double) audio.getFrameTimeLength() / 2.0 )
								{
									double ms1 = (double) (timeline - audio.getFrameTimeLength() - vptsval[v]);
									double ms2 = (double) (time_counter - vtime[v]);

									Common.getGuiInterface().showAVOffset("" + (int)(ms1 / 90) + "/" + (int)(ms2 / 90) + "/" + (int)((ms2 - ms1) / 90));

									if (Debug) 
										System.out.println(" §" + ms1 + "/" + ms2 + "/" + (ms2 - ms1));

									awrite = true;
									v += 2;
								}
							} 

							if (vptsdata && v < vptsval.length)
							{
								if ((double) Math.abs(vptsval[v] - (timeline - audio.getFrameTimeLength())) <= ((double) audio.getFrameTimeLength() / 2.0) )
								{
									double ms1 = (double) (timeline - audio.getFrameTimeLength() - vptsval[v]);
									double ms2 = (double) (time_counter - vtime[v]);

									Common.getGuiInterface().showAVOffset("" + (int)(ms1 / 90) + "/" + (int)(ms2 / 90) + "/" + (int)((ms2 - ms1) / 90));

									if (Debug) 
										System.out.println(" ß" + ms1 + "/" + ms2 + "/" + (ms2 - ms1));

									awrite = true;
									v += 2;
								}

								if (awrite && (double) Math.abs((time_counter - vtime[v - 2]) -
										(timeline - audio.getFrameTimeLength() - vptsval[v - 2]) ) > (double) audio.getFrameTimeLength() / 2.0 )
								{
									awrite = false;
									v -= 2;
								}
							}

							if (awrite || !vptsdata) 
								Common.getGuiInterface().showExportStatus(Resource.getString("audio.status.insert"));

							else 
								Common.getGuiInterface().showExportStatus(Resource.getString("audio.status.pause")); 

							if (!vptsdata || (vptsdata && awrite))
							{
								silentFrameBuffer.writeTo(audiooutL);

								/**
								 * RIFF 
								 */
								if (!is_DTS && AddRiffToAc3) 
									riffw[0].AC3RiffData(audio.parseRiffData(silentFrameBuffer.toByteArray())); 

								frame_counter++;
								time_counter += audio.getFrameTimeLength();
								cc++;
								ins[1]++;
							}

							if (Debug)
							{
								System.out.println("(8)audio frames: wri/pre/skip/ins/add " + frame_counter + "/" + cb + "/" + ce + "/" + cc + "/" + cd + "  @ " + Common.formatTime_1((long)(time_counter / 90.0f) ) + " ");
								System.out.println(" t)" + timeline);
								System.out.println(" x" + ((x < ptspos.length - 1) ? x + "/" + ptsval[x + 1] + "/" + ptspos[x + 1] : "-"));
							}

							timeline += audio.getFrameTimeLength();
						} // end while

						timeline -= audio.getFrameTimeLength();
						insertSilenceLoop = false;
						x++;

						if (ins[1] > 0)
							Common.setMessage(Resource.getString("audio.msg.summary.insert", "" + ins[1], FramesToTime((int)ins[1], audio.getFrameTimeLength())) + " " + Common.formatTime_1(ins[0] / 90L));

						/**
						 * reset PTS after inserting
						 */
						timeline = ptsval[x];

						continue readloopdd;
					} // end if insertSilenceLoop

					if ( (actframe + audio.getSize()) >= audiosize ) 
						break readloopdd;

				}  // end while

				/**
				 * add frames at the end 
				 */
				if ((es_streamtype == CommonParsing.AC3_AUDIO || es_streamtype == CommonParsing.DTS_AUDIO) && AddFrames && vptsdata && awrite && (w < vptsval.length))
				{
					timeline += audio.getFrameTimeLength();
					addf[0] = (long) time_counter;

					/**
					 * insert silence ac3 
					 */
					if (!is_DTS && !FillGapsWithLastFrame)
					{
						for (int c = 0; c < Common.getAC3list().size(); c++)
						{
							byte[] ac3data = (byte[]) Common.getAC3list().get(c);

							if ( (0xFE & ac3data[4]) != (0xFE & frame[4]) || ( (7 & ac3data[5]) != (7 & frame[5]) ) || (0xE0 & ac3data[6]) != (0xE0 & frame[6]) ) 
								continue;

							silentFrameBuffer.reset();
							silentFrameBuffer.write(ac3data);

							break;
						}
					}

					while ( w < vptsval.length )
					{
						while (vtime[w + 1] > time_counter && (double) Math.abs(vtime[w + 1] - time_counter) > (double) audio.getFrameTimeLength() / 2.0)
						{
							silentFrameBuffer.writeTo(audiooutL);

							/**
							 * RIFF 
							 */
							if (!is_DTS && AddRiffToAc3) 
								riffw[0].AC3RiffData(audio.parseRiffData(silentFrameBuffer.toByteArray())); 

							Common.getGuiInterface().showExportStatus(Resource.getString("audio.status.add")); 

							frame_counter++;
							time_counter += audio.getFrameTimeLength();
							timeline += audio.getFrameTimeLength();
							cd++;
							addf[1]++;

							if (Debug)
							{ 
								System.out.println("(9)audio frames: wri/pre/skip/ins/add " + frame_counter + "/" + cb + "/" + ce + "/" + cc + "/" + cd + "  @ " + Common.formatTime_1((long)(time_counter / 90.0f) ));
								System.out.print(" t)" + (long)(timeline - audio.getFrameTimeLength()) + " w)" + w);
							}
						}

						w += 2;
					}

					w -= 2;
					timeline -= audio.getFrameTimeLength();

					if (Debug) 
						System.out.println(" eot_video:" + (vptsval[w + 1] / 90) + "ms, eot_audio:" + (timeline / 90) + "ms ");
				}

// mpa start
				/**
				 * init FFT/window for mpa decoding for 1 file
				 */
				if (es_streamtype == CommonParsing.MPEG_AUDIO && DecodeMpgAudio)
				{
					MpaDecoder.init_work(ResampleAudioMode);
					MpaDecoder.DOWNMIX = DownMix;
					MpaDecoder.MONO = (DownMix || MpaConversionMode == 4);
					MpaDecoder.MOTOROLA = ChangeByteorder;
					MpaDecoder.WAVE = AddRiffHeader;

					if (AddRiffHeader)
					{
						audiooutL.write(MpaDecoder.RIFF);

						if (MpaConversionMode >= 4) 
							audiooutR.write(MpaDecoder.RIFF);
					}

					else if (AddAiffHeader)
					{
						audiooutL.write(MpaDecoder.AIFF);

						if (MpaConversionMode >= 4) 
							audiooutR.write(MpaDecoder.AIFF);
					}
				}

				if (es_streamtype == CommonParsing.MPEG_AUDIO)  // audio is global
					audio.setAncillaryDataDecoder(Common.getSettings().getBooleanProperty(Keys.KEY_MessagePanel_Msg7), collection.DebugMode());

				/**
				 *  MPEG1+2 Audio Layer 1,2,3
				 */
				readloop:
				while (es_streamtype == CommonParsing.MPEG_AUDIO && n < audiosize - 4)
				{
					Common.updateProgressBar(n, audiosize);

					//yield();

					if (Debug) 
						System.out.println(" n" + n);

					while (pause())
					{}

					if (CommonParsing.isProcessCancelled())
					{ 
						CommonParsing.setProcessCancelled(false);
						job_processing.setSplitSize(0); 

						break bigloop;
					}

					/**
					 * updates global audio_error and framecounter variable
					 */
					CommonParsing.setAudioProcessingFlags((0x3FFFFL & CommonParsing.getAudioProcessingFlags()) | ((long)frame_counter)<<18);
					
					/**
					 * fix VBR & restart processing 
					 */
					if (MpaDecoder.RESET)
						return true; 

					/**
					 * fix VBR & restart processing 
					 */
					if (!MpaDecoder.PRESCAN && (0xCL & CommonParsing.getAudioProcessingFlags()) != 0)
						return true; 

					if (ptspos[x + 1] != -1 && n > ptspos[x + 1])
					{
						Common.setMessage(Resource.getString("audio.msg.pts.wo_frame") + " (" + ptspos[x + 1] + "/" + n + ")");
						x++;
					}

					/**
					 * read 4 bytes for headercheck 
					 */
					audioin.read(pushmpa, 0, 4);
					n += 4;

					/**
					 * parse header 
					 */
					if ((ERRORCODE = audio.parseHeader(pushmpa, 0)) < 1)
					{
						audioin.unread(pushmpa, 1, 3);

						if (Message_2 && !missing_syncword)
							Common.setMessage(Resource.getString("audio.msg.syncword.lost", " " + (n - 4)) + " " + Common.formatTime_1((long)(time_counter / 90.0f)));

						missing_syncword = true;
						n -= 3;

						continue readloop;
					}

					/**
					 * prepare fo read entire frame 
					 */
					audioin.unread(pushmpa);
					n -= 4;

					/**
					 * read entire frame 
					 */
					frame = new byte[audio.getSize()];

					audioin.read(frame, 0, frame.length);

					System.arraycopy(frame, 0, header_copy, 0, 4);
					header_copy[3] &= 0xCF;
					header_copy[2] &= ~2;

					/**
					 * startfileposition of current frame 
					 */
					actframe = n;

					/**
					 * expected position for following frame 
					 */
					n += audio.getSize();

					/**
					 * pitch 
					 */
					if (PitchAudio)
					{  // skip a frame
						if (pitch[1] * pitch[0] == frame_counter)
						{
							Common.setMessage(Resource.getString("audio.msg.frame.discard") + " " + frame_counter + " (" + pitch[0] + ")");
							pitch[0]++;

							continue readloop;
						}
					}

					/**
					 * save current frame for copying, delete crc if nec. 
					 */
					if (FillGapsWithLastFrame)
					{
						copyframe[0] = new byte[frame.length];
						System.arraycopy(frame, 0, copyframe[0], 0, frame.length);

						if (ClearCRC) 
							audio.removeCRC(copyframe[0]);
					}

					/** 
					 * finish loop if last frame in file is shorter than nominal size 
					 */
					if (n > audiosize) 
						break readloop; 

					/**
					 * read following frame header, not if it is the last frame 
					 * check following frameheader for valid mpegaudio, if not starting with next byte 
					 */
					if (n < audiosize - 4)
					{
						if (!AllowSpaces)
						{
							audioin.read(pushmpa, 0, 4);

							ERRORCODE = audio.parseNextHeader(pushmpa, 0);

							audioin.unread(pushmpa);

							if (ERRORCODE < 1)
							{
								audioin.unread(frame, 1, frame.length - 1);
								n = actframe + 1;

								continue readloop;
							}
						}

						layertype = audio.getLayer();
					}

					if (ValidateCRC && (ERRORCODE = audio.validateCRC(frame, 0, audio.getSize())) != 0 )
					{
						Common.setMessage(Resource.getString("audio.msg.crc.error", "") + " " + actframe);

						audioin.unread(frame, 2, frame.length - 2);

						n = actframe + 2;

						continue readloop;
					}

					if (Message_2 && missing_syncword)
						Common.setMessage(Resource.getString("audio.msg.syncword.found") + " " + actframe);

					missing_syncword = false;


					/**
					 * check for change in frametype 
					 */
					if ((returncode = audio.compareHeader()) > 0)
					{
						newformat = true;

						if (returncode == 6)
						{
							jss++;
							newformat = false;
						}
					}

					if (frame_counter == 0) 
						newformat = true;

					audio.saveHeader();

					audio.decodeAncillaryData(frame);

					// timeline ist hier aktuelle audiopts

					Common.setFps(frame_counter);

					/**
					 * message 
					 */
					if (Debug) 
						System.out.println(" k)" +timeline +" l)" + (audio.getFrameTimeLength() / 2.0) + " m)" + awrite + " n)" + w + " o)" + v + " p)" + n);


					/**
					 * preloop if audio starts later than video, and i must insert 
					 */
					if ( (preloop && vptsdata && v >= vptsval.length) || !( preloop && vptsdata && vptsval[v] < timeline - (audio.getFrameTimeLength() / 2.0) ) ) 
						preloop = false;

					else
					{
						silent_Frame[0] = new byte[audio.getSizeBase()];	//silence without padd, std
						silent_Frame[1] = new byte[audio.getSize()];		//silence with padd for 22.05, 44.1

						for (int a = 0; a < 2; a++)
						{
							System.arraycopy(header_copy, 0, silent_Frame[a], 0, 4);	//copy last header data
							silent_Frame[a][1] |= 1;				//mark noCRC
							silent_Frame[a][2] |= (a * 2);				//set padding bit
						}

						int padding_counter = 1;						//count padding
						long precount=vptsval[v];
						long[] ins = { (long)time_counter, 0 };

						while ( precount < timeline- (audio.getFrameTimeLength() / 2.0) )
						{  //better for RTS
							/**
							 * check if frame write should paused 
							 */
							if (vptsdata && w < vptsval.length)
							{ 
								double ms1 = (double) (precount - vptsval[w + 1]);
								double ms2 = (double) (time_counter - vtime[w + 1]);

								if ( (double) Math.abs(ms2) <= audio.getFrameTimeLength() / 2.0 )
								{
									awrite = false;
									w += 2;
								}
								else if ((double) Math.abs(ms1) <= audio.getFrameTimeLength() / 2.0 )
								{
									awrite = false;
									w += 2;
								}
							}

							/**
							 * calculate A/V Offset for true 
							 */
							if (vptsdata && v < vptsval.length)
							{
								double ms3 = precount - vptsval[v];
								double ms4 = time_counter - vtime[v];

								if (Debug) 
									System.out.println(" ö" + ms3 + "/" + ms4 + "/" + (ms4 - ms3));

								if (!awrite && (double) Math.abs((time_counter - vtime[v]) -
										(precount - vptsval[v]) ) <= (double)audio.getFrameTimeLength() / 2.0 )
								{
									awrite = true;
									v += 2;

									double ms1 = precount - vptsval[v - 2];
									double ms2 = time_counter - vtime[v - 2];

									Common.getGuiInterface().showAVOffset("" + (int)(ms1 / 90) + "/" + (int)(ms2 / 90) + "/" + (int)((ms2 - ms1) / 90));

									if (Debug) 
										System.out.println(" ä" + ms1 + "/" + ms2 + "/" + (ms2 - ms1));
								}
							} 

							/**
							 * calculate A/V Offset for true 
							 */
							if (v < vptsval.length)
							{
								if ((double) Math.abs(vptsval[v] - precount) <= (double) audio.getFrameTimeLength() / 2.0)
								{
									awrite = true;
									v += 2;

									double ms1 = precount - vptsval[v - 2];
									double ms2 = time_counter - vtime[v - 2];

									Common.getGuiInterface().showAVOffset("" + (int)(ms1 / 90) + "/" + (int)(ms2 / 90) + "/" + (int)((ms2 - ms1) / 90));

									if (Debug) 
										System.out.println(" ü" + ms1 + "/" + ms2 + "/" + (ms2 - ms1));
								}

								/**
								 * calculate A/V Offset for false 
								 */
								if (awrite && Math.abs((time_counter - vtime[v - 2]) -
										(precount - vptsval[v - 2]) ) > audio.getFrameTimeLength() / 2.0 )
								{
									awrite = false;
									v -= 2;
								}
							}


							/**
							 * message 
							 */
							if (awrite || !vptsdata) 
								Common.getGuiInterface().showExportStatus(Resource.getString("audio.status.pre-insert")); 

							else 
								Common.getGuiInterface().showExportStatus(Resource.getString("audio.status.pause"));  

							/**
							 * stop if no more audio needed 
							 */
							if (precount > vptsval[vptsval.length - 1] + 10000)
							{
								Common.updateProgressBar(audiosize, audiosize);

								break readloop;
							}

							if (awrite)
							{
								if (FillGapsWithLastFrame)
								{		// copy last frame
									if (audio.getLayer() > 0 && DecodeMpgAudio)
									{
										audiooutL.write(MpaDecoder.decodeArray(copyframe[0]));

										if (MpaConversionMode >= 4) 
											audiooutR.write(MpaDecoder.get2ndArray());
									}

									else if (MpaConversionMode > 0)
									{
										newframes = MPAConverter.modifyframe(copyframe[0], MpaConversionMode); 
										audiooutL.write(newframes[0]); 

										if (MpaConversionMode >= 4) 
											audiooutR.write(newframes[1]);

										/**
										 * RIFF
										 */
										if (AddRiffToMpgAudio || AddRiffToMpgAudioL3) 
										{
											riffw[0].RiffData(audio.parseRiffData(newframes[0])); 

											if (MpaConversionMode >= 4) 
												riffw[1].RiffData(audio.parseRiffData(newframes[1]));
										}
									}

									else
									{
										audiooutL.write(copyframe[0]); 

										/**
										 * RIFF
										 */
										if (AddRiffToMpgAudio || AddRiffToMpgAudioL3) 
											riffw[0].RiffData(audio.parseRiffData(copyframe[0]));
									}
								}
								else
								{
									//if (padding_counter==padding) padding_counter=0;	//reset padd count
									//else if (samplerate==0) padding_counter++;		//count padding

									if (audio.getLayer() > 0 && DecodeMpgAudio)
									{ 
										audiooutL.write(MpaDecoder.decodeArray(silent_Frame[(padding_counter > 0) ? 0 : 1]));

										if (MpaConversionMode >= 4) 
											audiooutR.write(MpaDecoder.get2ndArray());
									}
									else if (MpaConversionMode > 0)
									{
										newframes = MPAConverter.modifyframe(silent_Frame[(padding_counter > 0) ? 0 : 1], MpaConversionMode);
										audiooutL.write(newframes[0]);

										if (MpaConversionMode >= 4) 
											audiooutR.write(newframes[1]);

										/**
										 * RIFF
										 */
										if (AddRiffToMpgAudio || AddRiffToMpgAudioL3) 
										{  
											riffw[0].RiffData(audio.parseRiffData(newframes[0])); 

											if (MpaConversionMode >= 4) 
												riffw[1].RiffData(audio.parseRiffData(newframes[1]));
										}
									}
									else
									{ 
										audiooutL.write(silent_Frame[(padding_counter > 0) ? 0 : 1]);

										/**
										 * RIFF
										 */
										if (AddRiffToMpgAudio || AddRiffToMpgAudioL3) 
											riffw[0].RiffData(audio.parseRiffData(silent_Frame[(padding_counter > 0) ? 0 : 1]));
									}
								}

								frame_counter++;
								time_counter += audio.getFrameTimeLength();
								cb++;
								ins[1]++;
							}

							precount += audio.getFrameTimeLength();

							if (Debug) 
								System.out.println("(5)audio frames: wri/pre/skip/ins/add " + frame_counter + "/" + cb + "/" + ce + "/" + cc + "/" + cd + "  @ " + Common.formatTime_1((long)(time_counter / 90.0f) ));
						} /** end while **/

						n = actframe;

						audioin.unread(frame);

						if (ins[1] > 0)
							Common.setMessage(Resource.getString("audio.msg.summary.pre-insert", "" + ins[1], FramesToTime((int)ins[1], audio.getFrameTimeLength())) + " " + Common.formatTime_1(ins[0] / 90L));

						continue readloop;
					} 


					/**
					 * check if frame write should paused 
					 */
					if (vptsdata)
					{ 
						vw[0] = v;
						vw[1] = w;

						awrite = SyncCheck(vw, time_counter, audio.getFrameTimeLength(), timeline, frame_counter, vptsval, vtime, awrite, Debug);

						v = vw[0];
						w = vw[1];
					}
					//  System.out.println(""+awrite+"/"+v+"/"+w);

					/**
					 * message 
					 */
					if (awrite || !vptsdata) 
						Common.getGuiInterface().showExportStatus(Resource.getString("audio.status.write")); 

					else 
						Common.getGuiInterface().showExportStatus(Resource.getString("audio.status.pause")); 

					/**
					 * stop if no more audio needed 
					 */
					if (vptsdata && timeline > vptsval[vptsval.length - 1] + 10000)
					{
						Common.updateProgressBar(audiosize, audiosize);

						break readloop;
					}

					/**
					 * message 
					 */
					if ((newformat && awrite) || (newformat && !vptsdata))
					{
						if (ModeChangeCount < 100)
						{
							String str = Common.formatTime_1((long)(time_counter / 90.0f));

							Common.setMessage(Resource.getString("audio.msg.source", audio.displayHeader()) + " " + str);

							if (Common.getSettings().getBooleanProperty(Keys.KEY_ExternPanel_createChapters))
								job_processing.getChapters().addChapter(str, audio.displayHeader());
						}

						else if (ModeChangeCount == 100) 
							Common.setMessage(Resource.getString("audio.msg.source.max"));

						else if (Debug) 
							System.out.println("=> src_audio: "+audio.displayHeader() + " @ " + Common.formatTime_1((long)(time_counter / 90.0f)));

						ModeChangeCount++;
						newformat = false;

						//yield();
					}

					/**
					 * message 
					 */
					if (Debug) 
						System.out.println("(1)audio frames: wri/pre/skip/ins/add " + frame_counter + "/" + cb + "/" + ce + "/" + cc + "/" + cd + "  @ " + Common.formatTime_1((long)(time_counter / 90.0f) ));

					if (Debug) 
						System.out.println(" x" + ((x < ptspos.length - 1) ? x + "/" + ptsval[x + 1] + "/" + ptspos[x + 1] : "-"));

					/**
					 * pts for next frame!! 
					 */
					timeline += audio.getFrameTimeLength();

					/**
					 * remove CRC 
					 */
					if (ClearCRC) 
						audio.removeCRC(frame);

					/**
					 * copy frame header 
					 */
					System.arraycopy(frame, 0, header_copy, 0, 4);
					header_copy[3] &= 0xCF;
					header_copy[2] &= ~2;

					/**
					 * message 
					 */
					//if (Debug)
					//	System.out.print(" tl"+timeline+" /px "+ptsval[x]+" /1_"+ptsval[x+1]+" /p1-tl "+(ptsval[x+1]-timeline)+" /pp1 "+ptspos[x+1]+" /n "+n);

					/**
					 * simple sync
					 */
					if (LimitPts && ptspos[x + 1] != -1 && ptspos[x + 1] < n)
					{
						if (Debug)
							System.out.println(" minSync " + minSync + "/ " + x);

						if ((++minSync) < 20)
							x++;
						else
							minSync = 0;
					}

					/**
					 * frame is in last pes packet or packet end not yet reached 
					 */
					if ((ptspos[x + 1] == -1) || (ptspos[x + 1] > n ))
					{
						if (vptsdata && !awrite) 
							continue readloop;

						if (audio.getLayer() > 0 && DecodeMpgAudio) 
						{
							audiooutL.write(MpaDecoder.decodeArray(frame));

							if (MpaConversionMode >= 4) 
								audiooutR.write(MpaDecoder.get2ndArray());
						}
						else if (MpaConversionMode > 0)
						{
							newframes = MPAConverter.modifyframe(frame, MpaConversionMode);
							audiooutL.write(newframes[0]);

							if (MpaConversionMode >= 4) 
								audiooutR.write(newframes[1]);

							/**
							 * RIFF 
							 */
							if (AddRiffToMpgAudio || AddRiffToMpgAudioL3) 
							{  
								riffw[0].RiffData(audio.parseRiffData(newframes[0])); 

								if (MpaConversionMode >= 4) 
									riffw[1].RiffData(audio.parseRiffData(newframes[1]));
							}
						}
						else
						{
			 				audiooutL.write(frame);

							/**
							 * RIFF 
							 */
							if (AddRiffToMpgAudio || AddRiffToMpgAudioL3) 
								riffw[0].RiffData(audio.parseRiffData(frame)); 
						}

						frame_counter++;
						time_counter += audio.getFrameTimeLength();

						continue readloop;
					}

					minSync = 0;

					/**
					 * frame is on pes packet corner 
					 */
					if ((double) Math.abs(ptsval[x + 1] - timeline) < (double) audio.getFrameTimeLength() / 2.0 )
					{
						timeline = ptsval[x + 1];
						x++;

						if (vptsdata && !awrite) 
							continue readloop;

						if (audio.getLayer() > 0 && DecodeMpgAudio)
						{
							audiooutL.write(MpaDecoder.decodeArray(frame));

							if (MpaConversionMode >= 4) 
								audiooutR.write(MpaDecoder.get2ndArray());
						}
						else if (MpaConversionMode > 0)
						{
							newframes = MPAConverter.modifyframe(frame, MpaConversionMode);
							audiooutL.write(newframes[0]);

							if (MpaConversionMode >= 4) 
								audiooutR.write(newframes[1]);

							/**
							 * RIFF 
							 */
							if (AddRiffToMpgAudio || AddRiffToMpgAudioL3) 
							{
								riffw[0].RiffData(audio.parseRiffData(newframes[0])); 

								if (MpaConversionMode >= 4) 
									riffw[1].RiffData(audio.parseRiffData(newframes[1]));
							}
						}
						else
						{
							audiooutL.write(frame);

							/**
							 * RIFF 
							 */
							if (AddRiffToMpgAudio || AddRiffToMpgAudioL3) 
								riffw[0].RiffData(audio.parseRiffData(frame)); 
						}

						frame_counter++;
						time_counter += audio.getFrameTimeLength();

						continue readloop;
					}

					if (ptsval[x + 1] > timeline) 
						insertSilenceLoop = true;

					if (ptsval[x + 1] < timeline)
					{
						x++;
						timeline = ptsval[x];

						Common.setMessage(Resource.getString("audio.msg.summary.skip") + " " + Common.formatTime_1((long)time_counter / 90L));

						ce++;
					}

					if (insertSilenceLoop)
					{
						silent_Frame[0] = new byte[audio.getSizeBase()];	//silence without padd, std
						silent_Frame[1] = new byte[audio.getSize()];		//silence with padd for 22.05, 44.1

						for (int a = 0; a < 2; a++)
						{
							System.arraycopy(header_copy, 0, silent_Frame[a], 0, 4);	//copy last header data
							silent_Frame[a][1] |= 1;				//mark noCRC
							silent_Frame[a][2] |= (a * 2);				//set padding bit
						}

						int padding_counter = 1;						//count padding
						long[] ins = { (long)time_counter, 0 };
		
						// solange nächster ptsval minus nächster framebeginn  ist größer der halben framezeit, füge stille ein
						while (ptsval[x + 1] > (timeline - (audio.getFrameTimeLength() / 2.0)))
						{
							if (vptsdata && w < vptsval.length)
							{ 
								double ms1 = (double) (timeline - audio.getFrameTimeLength() - vptsval[w + 1]);
								double ms2 = (double) (time_counter - vtime[w + 1]);

								if ((double) Math.abs(ms2) <= audio.getFrameTimeLength() / 2.0)
								{
									awrite = false;
									w += 2;
								}
								else if ((double) Math.abs(ms1) <= audio.getFrameTimeLength() / 2.0)
								{
									awrite = false;
									w += 2;
								}
							}

							if (vptsdata && v < vptsval.length)
							{
								if (!awrite && (double) Math.abs((time_counter - vtime[v]) -
									(timeline - audio.getFrameTimeLength() - vptsval[v]) ) <= (double) audio.getFrameTimeLength() / 2.0 )
								{
									double ms1 = (double) (timeline - audio.getFrameTimeLength() - vptsval[v]);
									double ms2 = (double) (time_counter - vtime[v]);

									Common.getGuiInterface().showAVOffset("" + (int)(ms1 / 90) + "/" + (int)(ms2 / 90) + "/" + (int)((ms2 - ms1) / 90));

									if (Debug) 
										System.out.println(" §" + ms1 + "/" + ms2 + "/" + (ms2 - ms1));

									awrite = true;
									v += 2;
								}
							} 

							if (vptsdata && v < vptsval.length)
							{
								if ((double) Math.abs(vptsval[v] - (timeline - audio.getFrameTimeLength())) <= ((double) audio.getFrameTimeLength() / 2.0) )
								{
									double ms1 = (double) (timeline - audio.getFrameTimeLength() - vptsval[v]);
									double ms2 = (double) (time_counter - vtime[v]);

									Common.getGuiInterface().showAVOffset("" + (int)(ms1 / 90) + "/" + (int)(ms2 / 90) + "/" + (int)((ms2 - ms1) / 90));

									if (Debug) 
										System.out.println(" ß" + ms1 + "/" + ms2 + "/" + (ms2 - ms1));

									awrite = true;
									v += 2;
								}

								if (awrite && (double) Math.abs((time_counter - vtime[v - 2]) -
									(timeline - audio.getFrameTimeLength() - vptsval[v - 2]) ) > (double) audio.getFrameTimeLength() / 2.0 )
								{
									awrite = false;
									v -= 2;
								}
							}

							/**
							 * message 
							 */
							if (awrite || !vptsdata) 
								Common.getGuiInterface().showExportStatus(Resource.getString("audio.status.insert")); 

							else 
								Common.getGuiInterface().showExportStatus(Resource.getString("audio.status.pause")); 
  
							if (!vptsdata || (vptsdata && awrite))
							{
								if (FillGapsWithLastFrame)
								{
									if (audio.getLayer() > 0 && DecodeMpgAudio)
									{
										audiooutL.write(MpaDecoder.decodeArray(copyframe[0]));

										if (MpaConversionMode >= 4) 
											audiooutR.write(MpaDecoder.get2ndArray());
									}
									else if (MpaConversionMode > 0)
									{
										newframes = MPAConverter.modifyframe(copyframe[0], MpaConversionMode); 
										audiooutL.write(newframes[0]);

										if (MpaConversionMode >= 4) 
											audiooutR.write(newframes[1]);

										/**
										 * RIFF 
										 */
										if (AddRiffToMpgAudio || AddRiffToMpgAudioL3) 
										{ 
											riffw[0].RiffData(audio.parseRiffData(newframes[0])); 

											if (MpaConversionMode >= 4) 
												riffw[1].RiffData(audio.parseRiffData(newframes[1]));
										}
									}
									else
									{
										audiooutL.write(copyframe[0]);

										/**
										 * RIFF 
										 */
										if (AddRiffToMpgAudio || AddRiffToMpgAudioL3) 
											riffw[0].RiffData(audio.parseRiffData(copyframe[0]));
									}
								}
								else
								{
									//if (padding_counter==padding) padding_counter=0;	//reset padd count
									//else if (samplerate==0) padding_counter++;		//count padding

									if (audio.getLayer() > 0 && DecodeMpgAudio)
									{
										audiooutL.write(MpaDecoder.decodeArray(silent_Frame[(padding_counter > 0) ? 0 : 1]));

										if (MpaConversionMode >= 4) 
											audiooutR.write(MpaDecoder.get2ndArray());
									}
									else if (MpaConversionMode > 0)
									{
										newframes = MPAConverter.modifyframe(silent_Frame[(padding_counter > 0) ? 0 : 1], MpaConversionMode);
										audiooutL.write(newframes[0]);

										if (MpaConversionMode >= 4) 
											audiooutR.write(newframes[1]);

										/**
										 * RIFF 
										 */
										if (AddRiffToMpgAudio || AddRiffToMpgAudioL3) 
										{  
											riffw[0].RiffData(audio.parseRiffData(newframes[0])); 

											if (MpaConversionMode >= 4) 
												riffw[1].RiffData(audio.parseRiffData(newframes[1]));
										}
									}
									else
									{
										audiooutL.write(silent_Frame[(padding_counter > 0) ? 0 : 1]);

										/**
										 * RIFF 
										 */
										if (AddRiffToMpgAudio || AddRiffToMpgAudioL3) 
											riffw[0].RiffData(audio.parseRiffData(silent_Frame[(padding_counter > 0) ? 0 : 1])); 
									}
								}

								frame_counter++;
								time_counter += audio.getFrameTimeLength();
								cc++;
								ins[1]++;
							}

							if (Debug)
							{
								System.out.println("(2)audio frames: wri/pre/skip/ins/add " + frame_counter + "/" + cb + "/" + ce + "/" + cc + "/" + cd + "  @ " + Common.formatTime_1((long)(time_counter / 90.0f) ));
								System.out.print(" t)" + timeline);
								System.out.println(" x" + ((x < ptspos.length - 1) ? x + "/" + ptsval[x + 1] + "/" + ptspos[x + 1] : "-"));
							}

							timeline += audio.getFrameTimeLength();
						} // end while

						timeline -= audio.getFrameTimeLength();
						insertSilenceLoop = false;
						x++;

						if (ins[1] > 0) 
							Common.setMessage(Resource.getString("audio.msg.summary.insert", "" + ins[1], FramesToTime((int)ins[1], audio.getFrameTimeLength())) + " " + Common.formatTime_1(ins[0] / 90L));

						/**
						 * reset PTS after inserting
						 */
						timeline = ptsval[x];

						continue readloop;
					}

					if ( (actframe + audio.getSize()) >= audiosize ) 
						break readloop; 
				}  // end while

				if (Debug) 
					System.out.println("(3)audio frames: wri/pre/skip/ins/add " + frame_counter + "/" + cb + "/" + ce + "/" + cc + "/" + cd + "  @ " + Common.formatTime_1((long)(time_counter / 90.0f) ));

				/**
				 * add frames at the end 
				 */
				if (es_streamtype == CommonParsing.MPEG_AUDIO && AddFrames && vptsdata && awrite && (w < vptsval.length))
				{
					timeline += audio.getFrameTimeLength();
					addf[0] = (long) time_counter;

					silent_Frame[0] = new byte[audio.getSizeBase()];	//silence without padd, std
					silent_Frame[1] = new byte[audio.getSize()];		//silence with padd for 22.05, 44.1

					for (int a = 0; a < 2; a++)
					{
						System.arraycopy(header_copy,0, silent_Frame[a], 0, 4);	//copy last header data
						silent_Frame[a][1] |= 1;				//mark noCRC
						silent_Frame[a][2] |= (a * 2);				//set padding bit
					}

					int padding_counter = 1;						//count padding

					while (w < vptsval.length)
					{
						while ( vtime[w + 1] > time_counter && 
							(double) Math.abs(vtime[w + 1] - time_counter) > (double) audio.getFrameTimeLength() / 2.0 )
						{
							if (FillGapsWithLastFrame)
							{				//add_copy prev. frame
								if (audio.getLayer() > 0 && DecodeMpgAudio) 
								{ 
									audiooutL.write(MpaDecoder.decodeArray(copyframe[0]));

									if (MpaConversionMode >= 4) 
										audiooutR.write(MpaDecoder.get2ndArray());
								}
								else if (MpaConversionMode > 0)
								{		//modify frame
									newframes = MPAConverter.modifyframe(copyframe[0], MpaConversionMode); 
									audiooutL.write(newframes[0]);

									if (MpaConversionMode >= 4) 
										audiooutR.write(newframes[1]);

									/**
									 * RIFF 
									 */
									if (AddRiffToMpgAudio || AddRiffToMpgAudioL3) 
									{ 
										riffw[0].RiffData(audio.parseRiffData(newframes[0])); 

										if (MpaConversionMode >= 4) 
											riffw[1].RiffData(audio.parseRiffData(newframes[1]));
									}
								}
								else
								{
									audiooutL.write(copyframe[0]);

									/**
									 * RIFF 
									 */
									if (AddRiffToMpgAudio || AddRiffToMpgAudioL3) 
										riffw[0].RiffData(audio.parseRiffData(copyframe[0]));
								}
							}
							else
							{	//add silence
								//if (padding_counter==padding) padding_counter=0;	//reset padd count
								//else if (samplerate==0) padding_counter++;		//count padding

								if (audio.getLayer() > 0 && DecodeMpgAudio)
								{
									audiooutL.write(MpaDecoder.decodeArray(silent_Frame[(padding_counter > 0) ? 0 : 1]));

									if (MpaConversionMode >= 4) 
										audiooutR.write(MpaDecoder.get2ndArray());
								}
								else if (MpaConversionMode > 0)
								{		//modify frame
									newframes = MPAConverter.modifyframe(silent_Frame[(padding_counter > 0) ? 0 : 1], MpaConversionMode);
									audiooutL.write(newframes[0]);

									if (MpaConversionMode >= 4) 
										audiooutR.write(newframes[1]);

									/**
									 * RIFF 
									 */
									if (AddRiffToMpgAudio || AddRiffToMpgAudioL3) 
									{
										riffw[0].RiffData(audio.parseRiffData(newframes[0])); 

										if (MpaConversionMode >= 4) 
											riffw[1].RiffData(audio.parseRiffData(newframes[1]));
									}
								}
								else
								{
									audiooutL.write(silent_Frame[(padding_counter > 0) ? 0 : 1]);

									/**
									 * RIFF 
									 */
									if (AddRiffToMpgAudio || AddRiffToMpgAudioL3) 
										riffw[0].RiffData(audio.parseRiffData(silent_Frame[(padding_counter > 0) ? 0 : 1]));
								}
							}

							timeline += audio.getFrameTimeLength();
							cd++;
							frame_counter++;
							addf[1]++;
							time_counter += audio.getFrameTimeLength();

							Common.getGuiInterface().showExportStatus(Resource.getString("audio.status.add")); 

							if (Debug)
							{
								System.out.println("(4)audio frames: wri/pre/skip/ins/add " + frame_counter + "/" + cb + "/" + ce + "/" + cc + "/" + cd + "  @ " + Common.formatTime_1((long)(time_counter / 90.0f) ));
								System.out.print(" t)" + (long)(timeline - audio.getFrameTimeLength()) + " w)" + w);
							}
						}

						w += 2;
					}

					w -= 2;
					timeline -= audio.getFrameTimeLength();

					if (Debug) 
						System.out.println(" eot_video:" + (vptsval[w + 1] / 90) + "ms, eot_audio:" + ((timeline) / 90) + "ms  ");
				}  //end add mpa


				/**
				 *  PCM Audio
				 */
				if (es_streamtype == CommonParsing.LPCM_AUDIO)
				{
					// parse header
					frame = new byte[1000];

					audioin.read(frame);

					audio.parseHeader(frame, 0);

					audioin.unread(frame, audio.getEmphasis(), 1000 - audio.getEmphasis());

					Common.setMessage(Resource.getString("audio.msg.source", audio.saveAndDisplayHeader()) + " " + Common.formatTime_1((long)(time_counter / 90.0f)));
					layertype = WAV_AUDIOSTREAM;

					n = audio.getEmphasis(); //start of pcm data
					long pcm_end_pos = audio.getEmphasis() + audio.getSizeBase(); //whole sample data size

					timeline = ptsval[0];

					audiooutL.write(audio.getRiffHeader());

					long sample_bytes;
					long skip_bytes;
					long sample_pts;
					long skip_pts;

					int sample_size;
					int read_size = 960000 / audio.getMode();

					// Size/8 * Channel = bytes per sample
					// 16bit/8 * 2  = 4
					// per sample: Audio.Time_length = 90000.0 / Samplefre 
					// 48000hz = 1.875 ticks (of 90khz) 1 frame = 192 samples = 360ticks
					// 44100hz = 2.040816 

					for (int f = 0; f < ptsval.length - 1; f++)
					{
						for (int a = 0; a < vptsval.length; a+=2)
						{
							while (pause())
							{}

							if (CommonParsing.isProcessCancelled())
							{ 
								CommonParsing.setProcessCancelled(false);
								job_processing.setSplitSize(0); 

								break bigloop;
							}

							if (vptsdata && vptsval[a] < timeline)  //jump back (not yet) or insert silent samples
							{
								sample_pts = vptsval[a + 1] > timeline ? timeline - vptsval[a] : vptsval[a + 1] - vptsval[a];
								sample_bytes = (long)Math.round(1.0 * audio.getSamplingFrequency() * sample_pts / 90000.0) * audio.getMode();

								if (Debug)
									System.out.println("a " + sample_pts + "/" + sample_bytes + "/" + n + "/" + timeline);

								for (long sample_pos = 0; sample_pos < sample_bytes; )
								{
									sample_size = (sample_bytes - sample_pos) >= read_size ? read_size : (int)(sample_bytes - sample_pos);
									frame = new byte[sample_size];
									sample_pos += sample_size;

									audiooutL.write(frame);
								}

								time_counter += sample_pts;
								frame_counter += (sample_bytes / audio.getMode());

								Common.setFps(frame_counter);

								if (vptsval[a + 1] > timeline)
								{
									sample_pts = vptsval[a + 1] - timeline;
									sample_bytes = (long) Math.round(1.0 * audio.getSamplingFrequency() * sample_pts / 90000.0) * audio.getMode();

									if (Debug)
										System.out.println("b " + sample_pts + "/" + sample_bytes + "/" + n + "/" + timeline);

									for (long sample_pos = 0; sample_pos < sample_bytes; )
									{
										sample_size = (sample_bytes - sample_pos) >= read_size ? read_size : (int)(sample_bytes - sample_pos);
										frame = new byte[sample_size];

										audioin.read(frame);

										sample_pos += sample_size;

										audiooutL.write(frame);
									}

									n += sample_bytes;
									timeline += sample_pts;
									time_counter += sample_pts;
									frame_counter += (sample_bytes / audio.getMode());
								}
							}
							else
							{
								skip_pts = vptsdata ? vptsval[a] - timeline : 0;
								skip_bytes = (long)Math.round(1.0 * audio.getSamplingFrequency() * skip_pts / 90000.0) * audio.getMode();

								sample_pts = vptsdata ? vptsval[a + 1] - vptsval[a] : (long)(1.0 * (audio.getSizeBase() / audio.getMode()) / audio.getSamplingFrequency() * 90000.0);
								sample_bytes = (long) Math.round(1.0 * audio.getSamplingFrequency() * sample_pts / 90000.0) * audio.getMode();

								for (long skip_pos = 0; skip_pos < skip_bytes; )
									skip_pos += audioin.skip(skip_bytes - skip_pos);

								n += skip_bytes;

								if (Debug)
									System.out.println("c " + skip_pts + "/" + skip_bytes + "/" + sample_pts + "/" + sample_bytes + "/" + n + "/" + timeline);

								for (long sample_pos = 0; sample_pos < sample_bytes; )
								{
									sample_size = (sample_bytes - sample_pos) >= read_size ? read_size : (int)(sample_bytes - sample_pos);
									frame = new byte[sample_size];

									audioin.read(frame);

									sample_pos += sample_size;

									audiooutL.write(frame);
								}

								n += sample_bytes;
								timeline += (skip_pts + sample_pts);
								time_counter += sample_pts;
								frame_counter += (sample_bytes / audio.getMode());
							}

							if (Debug)
								System.out.println("(4w)audio frames: wri/pre/skip/ins/add " + frame_counter + "/" + cb + "/" + ce + "/" + cc + "/" + cd + "  @ " + Common.formatTime_1((long)(time_counter / 90.0f) ));

							Common.updateProgressBar(n, audiosize);

							//yield();

							if (Debug) 
								System.out.println(" n" + n);
						}

						break;
					}
				}

				/**
				 * restart decoding after a peak search
				 */
				if (es_streamtype == CommonParsing.MPEG_AUDIO && DecodeMpgAudio && MpaDecoder.PRESCAN && !MpaDecoder.RESET)
				{
					MpaDecoder.PRESCAN = false;
					MpaDecoder.NORMALIZE = false;

					return true;
				}

				break;

			} // end while bigloop


			if (addf[1] > 0)
				Common.setMessage(Resource.getString("audio.msg.summary.add", "" + addf[1], FramesToTime((int)addf[1], audio.getFrameTimeLength())) + " " + Common.formatTime_1(addf[0] / 90L));

			Common.getGuiInterface().showExportStatus(Resource.getString("audio.status.finish")); 

			String tc = Common.formatTime_1((long)(time_counter / 90.0f) );
			Common.setMessage(Resource.getString("audio.msg.summary.frames", "" + frame_counter + "/" + cb + "/" + ce + "/" + cc + "/" + cd, "" + tc));

			if (jss > 0) 
				Common.setMessage(Resource.getString("audio.msg.summary.jstereo", "" + jss));

			audioin.close(); 
			silentFrameBuffer.close();

			audiooutL.flush(); 
			audiooutL.close();

			audiooutR.flush(); 
			audiooutR.close();

			String[][] pureaudio = {
				{ ".ac3",".mp1",".mp2",".mp3",".dts" },
				{ ".new.ac3",".new.mp1",".new.mp2",".new.mp3",".new.dts" }
			};

			if (Common.getSettings().getBooleanProperty(Keys.KEY_ExternPanel_renameAudio))
			{
				for (int g = 1; g < 4; g++)
				{
					pureaudio[0][g] = ".mpa";
					pureaudio[1][g] = ".new.mpa";
				}
			}

			if (DecodeMpgAudio && audio.getLayer() > 1)
			{
				if (MpaDecoder.WAVE)
				{
					for (int g = 1; g < 4; g++)
					{
						pureaudio[0][g] += ".wav";
						pureaudio[1][g] += ".wav";
					}
				}

				else if (AddAiffHeader)
				{
					for (int g = 1; g < 4; g++)
					{
						pureaudio[0][g] += ".aif";
						pureaudio[1][g] += ".aif";
					}
				}

				else  
				{
					for (int g = 1; g < 4; g++)
					{
						pureaudio[0][g] += ".pcm";
						pureaudio[1][g] += ".pcm";
					}
				}
			}

			else if (AddRiffToMpgAudio || AddRiffToMpgAudioL3) 
			{
				for (int g = 1; g < 4; g++)
				{
					pureaudio[0][g] += ".wav";
					pureaudio[1][g] += ".wav";
				}
			}

			if (AddRiffToAc3)
			{
				pureaudio[0][0] += ".wav";
				pureaudio[1][0] += ".wav";
			}

			else if (CreateDDWave)
			{
				pureaudio[0][0] += ".wav";
				pureaudio[1][0] += ".wav";
				pureaudio[0][4] += ".wav";
				pureaudio[1][4] += ".wav";
			}

			File ac3name = new File (fparent + pureaudio[isElementaryStream][0]);
			File mp1name = new File (fparent + pureaudio[isElementaryStream][1]);
			File mp2name = new File (fparent + pureaudio[isElementaryStream][2]);
			File mp3name = new File (fparent + pureaudio[isElementaryStream][3]);
			File mp2nameL = new File (fparent + "[L]" + pureaudio[0][2]);
			File mp2nameR = new File (fparent + "[R]" + pureaudio[0][2]);
			File dtsname = new File (fparent + pureaudio[isElementaryStream][4]);
			File wavname = new File (fparent + ".new.wav");

			/*** make riff ***/
			if (DecodeMpgAudio && es_streamtype == CommonParsing.MPEG_AUDIO && MpaDecoder.WAVE)
			{
				if (audio.getLayer() > 1)
				{
					MpaDecoder.fillRIFF(newnameL);

					if (MpaConversionMode >= 4) 
						MpaDecoder.fillRIFF(newnameR);
				}
				else
				{
					MpaDecoder.deleteRIFF(newnameL);

					if (MpaConversionMode >= 4) 
						MpaDecoder.deleteRIFF(newnameR);
				}
			}

			else if (DecodeMpgAudio && es_streamtype == CommonParsing.MPEG_AUDIO && AddAiffHeader)
			{
				if (audio.getLayer() > 1)
				{
					MpaDecoder.fillAiff(newnameL,(long)(time_counter / 90.0f));

					if (MpaConversionMode >= 4) 
						MpaDecoder.fillAiff(newnameR,(long)(time_counter / 90.0f));
				}
				else
				{
					MpaDecoder.deleteAiff(newnameL);

					if (MpaConversionMode >= 4) 
						MpaDecoder.deleteAiff(newnameR);
				}
			}

			else if ((AddRiffToMpgAudio || AddRiffToMpgAudioL3) && es_streamtype == CommonParsing.MPEG_AUDIO)
			{
				RandomAccessFile[] rifffile = { new RandomAccessFile(newnameL, "rw"), new RandomAccessFile(newnameR, "rw") };

				riffw[0].Length( rifffile[0].length() , (long)(time_counter / 90.0f) ); 
				riffw[1].Length( rifffile[1].length() , (long)(time_counter / 90.0f) );

				rifffile[0].seek(0); 
				rifffile[1].seek(0);

				if (AddRiffToMpgAudioL3)
				{
					rifffile[0].write(riffw[0].ACM());

					if (MpaConversionMode >= 4) 
						rifffile[1].write(riffw[1].ACM());
				}
				else
				{
					rifffile[0].write(riffw[0].BWF());

					if (MpaConversionMode >= 4) 
						rifffile[1].write(riffw[1].BWF());
				}

				rifffile[0].close();
				rifffile[1].close();
			}

			else if (AddRiffToAc3 && es_streamtype == CommonParsing.AC3_AUDIO) 
			{
				RandomAccessFile rifffile = new RandomAccessFile(newnameL, "rw");

				riffw[0].Length( rifffile.length() , (long)(time_counter / 90.0f) );

				rifffile.seek(0);

				rifffile.write(riffw[0].AC3());

				rifffile.close();
			}

			else if (es_streamtype == CommonParsing.LPCM_AUDIO)
				audio.fillRiffHeader(newnameL);

			else if (CreateDDWave && es_streamtype == CommonParsing.AC3_AUDIO)
				audio.fillStdRiffHeader(newnameL, (long)(time_counter / 90.0f));

			else if (CreateDDWave && es_streamtype == CommonParsing.DTS_AUDIO)
				audio.fillStdRiffHeader(newnameL, (long)(time_counter / 90.0f));


			File audioout1 = new File(newnameL);
			File audioout2 = new File(newnameR);

			job_processing.countMediaFilesExportLength(audioout1.length());
			job_processing.countMediaFilesExportLength(audioout2.length());

			String audio_type[] = { "(ac3)", "(mp3)", "(mp2)", "(mp1)", "(dts)", "(pcm)" };

			if (DecodeMpgAudio)
				audio_type[1] = audio_type[2] = "(pcm)";

			String comparedata = "";

			if (layertype < 0)
				layertype = NO_AUDIOSTREAM;

			else
				comparedata = Resource.getString("audio.msg.audio") + " " + job_processing.countAudioStream() + " " + audio_type[layertype] + ":\t" + frame_counter + " Frames\t" + tc + "\t" + infoPTSMatch(filename_pts, videofile_pts, vptsdata, ptsdata) + cb + "/" + ce + "/" + cc + "/" + cd;

			/**
			 *
			 */
			switch (layertype)
			{ 
			case AC3_AUDIOSTREAM: 
				if (ac3name.exists())
					ac3name.delete();

				if (audioout1.length() < 100) 
					audioout1.delete();

				else
				{ 
					Common.renameTo(audioout1, ac3name);

					Common.setMessage(Resource.getString("msg.newfile", "") + " '" + ac3name + "'"); 
					job_processing.addSummaryInfo(comparedata + "\t'" + ac3name + "'");
				}

				if (audioout2.length() < 100) 
					audioout2.delete();

				audiooutL.renameIddTo(ac3name);
				audiooutR.deleteIdd();

				break;

			case MP3_AUDIOSTREAM:
				if ( mp3name.exists() ) 
					mp3name.delete();

				if (audioout1.length() < 100) 
					audioout1.delete();

				else
				{ 
					Common.renameTo(audioout1, mp3name); 

					Common.setMessage(Resource.getString("msg.newfile", "") + " '" + mp3name + "'"); 
					job_processing.addSummaryInfo(comparedata + "\t'" + mp3name + "'");
				}

				if (audioout2.length() < 100) 
					audioout2.delete();

				audiooutL.renameIddTo(mp3name);
				audiooutR.deleteIdd();

				break;

			case MP2_AUDIOSTREAM:
				if (MpaConversionMode >= 4)
				{
					if ( mp2nameL.exists() ) 
						mp2nameL.delete();

					if ( mp2nameR.exists() ) 
						mp2nameR.delete();

					if (audioout2.length() < 100) 
						audioout2.delete();

					else
					{ 
						Common.renameTo(audioout2, mp2nameR); 

						Common.setMessage(Resource.getString("msg.newfile", Resource.getString("audio.msg.newfile.right")) + " '" + mp2nameR + "'"); 
						job_processing.addSummaryInfo(comparedata + "\t'" + mp2nameR + "'"); 
					}

					if (audioout1.length() < 100) 
						audioout1.delete();

					else
					{
						Common.renameTo(audioout1, mp2nameL); 

						Common.setMessage(Resource.getString("msg.newfile", Resource.getString("audio.msg.newfile.left")) + " '" + mp2nameL + "'"); 
						job_processing.addSummaryInfo(comparedata + "\t'" + mp2nameL + "'"); 
					}

					audiooutL.renameIddTo(mp2nameL);
					audiooutR.renameIddTo(mp2nameR);
				}

				else
				{
					if ( mp2name.exists() ) 
						mp2name.delete();

					if (audioout1.length() < 100) 
						audioout1.delete();

					else
					{ 
						Common.renameTo(audioout1, mp2name); 

						Common.setMessage(Resource.getString("msg.newfile", "") + " '" + mp2name + "'"); 
						job_processing.addSummaryInfo(comparedata + "\t'" + mp2name + "'"); 
					}

					if (audioout2.length() < 100) 
						audioout2.delete();

					audiooutL.renameIddTo(mp2name);
					audiooutR.deleteIdd();
				}

				break;

			case MP1_AUDIOSTREAM: 
				if ( mp1name.exists() ) 
					mp1name.delete();

				if (audioout1.length() < 100) 
					audioout1.delete();

				else
				{ 
					Common.renameTo(audioout1, mp1name); 

					Common.setMessage(Resource.getString("msg.newfile", "") + " '" + mp1name + "'"); 
					job_processing.addSummaryInfo(comparedata + "\t'" + mp1name + "'"); 
				}

				if (audioout2.length() < 100) 
					audioout2.delete();

				audiooutL.renameIddTo(mp1name);
				audiooutR.deleteIdd();

				break;

			case DTS_AUDIOSTREAM: 
				if (dtsname.exists())
					dtsname.delete();

				if (audioout1.length() < 100) 
					audioout1.delete();

				else
				{ 
					Common.renameTo(audioout1, dtsname); 

					Common.setMessage(Resource.getString("msg.newfile", "") + " '" + dtsname + "'"); 
					job_processing.addSummaryInfo(comparedata + "\t'" + dtsname + "'");
				}

				if (audioout2.length() < 100) 
					audioout2.delete();

				audiooutL.renameIddTo(dtsname);
				audiooutR.deleteIdd();

				break;

			case WAV_AUDIOSTREAM: 
				if (wavname.exists())
					wavname.delete();

				if (audioout1.length() < 100) 
					audioout1.delete();

				else
				{ 
					Common.renameTo(audioout1, wavname); 

					Common.setMessage(Resource.getString("msg.newfile", "") + " '" + wavname + "'"); 
					job_processing.addSummaryInfo(comparedata + "\t'" + wavname + "'");
				}

				if (audioout2.length() < 100) 
					audioout2.delete();

				audiooutL.renameIddTo(wavname);
				audiooutR.deleteIdd();

				break;

			case NO_AUDIOSTREAM: 
				Common.setMessage(Resource.getString("audio.msg.noaudio")); 

				audioout1.delete();
				audioout2.delete();
				audiooutL.deleteIdd();
				audiooutR.deleteIdd();

				break;
			}

		} catch (IOException e) {

			Common.setExceptionMessage(e);
		}

		Common.updateProgressBar(audiosize, audiosize);

		return false;
	}

}
