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

import net.sourceforge.dvb.projectx.io.IDDBufferedOutputStream;

import net.sourceforge.dvb.projectx.audio.MpaDecoder;
import net.sourceforge.dvb.projectx.audio.MpaConverter;
import net.sourceforge.dvb.projectx.audio.AudioFormat;

import net.sourceforge.dvb.projectx.xinput.XInputFile;

import net.sourceforge.dvb.projectx.parser.CommonParsing;
import net.sourceforge.dvb.projectx.parser.StreamConverter;
import net.sourceforge.dvb.projectx.parser.StreamDemultiplexer;

import net.sourceforge.dvb.projectx.parser.StreamProcessBase;

/**
 * main thread
 */
public class StreamProcessAudio extends StreamProcessBase {

	private final int AC3_AUDIOSTREAM = 0;
	private final int MP3_AUDIOSTREAM = 1;
	private final int MP2_AUDIOSTREAM = 2;
	private final int MP1_AUDIOSTREAM = 3;
	private final int DTS_AUDIOSTREAM = 4;
	private final int WAV_AUDIOSTREAM = 5;
	private final int NO_AUDIOSTREAM = 10;

	private MpaConverter MPAConverter = null;
	private MpaDecoder MPADecoder = null;

	private PushbackInputStream InputStream;

	private IDDBufferedOutputStream OutputStream_Ch1;
	private IDDBufferedOutputStream OutputStream_Ch2;

	private String FileName_Ch1;
	private String FileName_Ch2;

	private boolean HasNewFormat;
	private boolean Debug;
	private boolean CreateChapters;
	private boolean WriteEnabled;
	private boolean AddWaveHeaderACM;
	private boolean AddWaveHeaderBWF;
	private boolean AddWaveHeaderAC3;
	private boolean DecodeMpgAudio;
	private boolean ReplaceAc3withSilence;
	private boolean ContainsVideoPTS;
	private boolean ContainsAudioPTS;
	private boolean CreateM2sIndex;
	private boolean Message_2;
	private boolean Message_7;
	private boolean PitchAudio;
	private boolean AllowSpaces;
	private boolean ValidateCRC;
	private boolean Patch1stAc3Header;
	private boolean FillGapsWithLastFrame;
	private boolean LimitPts;
	private boolean AllowFormatChanges;
	private boolean AddFrames;
	private boolean DownMix;
	private boolean ChangeByteorder;
	private boolean AddRiffHeader;
	private boolean AddAiffHeader;
	private boolean ClearCRC;
	private boolean IgnoreErrors;
	private boolean CreateDDWave;
	private boolean FadeInOut;
	private boolean Normalize;

	private int FadeInOutMillis;
	private int ResampleAudioMode;
	private int PitchValue;
	private int AudioType;

	private long ModeChangeCount;
	private long ModeChangeCount_JSS;
	private final long ModeChangeCount_Max = 100;

	private FrameExportInfo FrameExportInfo;

	private int MpaConversionMode;
	private final int MpaConversion_None  = 0;
	private final int MpaConversion_Mode1 = 1; //single to 3D
	private final int MpaConversion_Mode2 = 2; //single to jst
	private final int MpaConversion_Mode3 = 3; //single to st
	private final int MpaConversion_Mode4 = 4; //st-dual to 2 single
	private final int MpaConversion_Mode5 = 5; //st-dual to 2 jst - doubled single

	private long FileLength;
	private final long FileLength_Min = 100;

	private double TimeCounter;

	private long FramePosition;
	private long CurrentFramePosition;
	private long TimePosition;

	/**
	 * 
	 */
	public StreamProcessAudio(JobCollection collection, XInputFile xInputFile, String filename_pts, String filename_type, String videofile_pts, int isElementaryStream)
	{
		super();

		processStream(collection, xInputFile, filename_pts, filename_type, videofile_pts, isElementaryStream);
	}

	/**
	 * start method for adjusting audio at TimePosition
	 */
	private void processStream(JobCollection collection, XInputFile xInputFile, String filename_pts, String filename_type, String videofile_pts, int isElementaryStream)
	{
		Common.updateProgressBar(Resource.getString("audio.progress") + "  " + xInputFile.getName(), 0, 0);

		Normalize = collection.getSettings().getBooleanProperty(Keys.KEY_AudioPanel_Normalize);
		DecodeMpgAudio = collection.getSettings().getBooleanProperty(Keys.KEY_AudioPanel_decodeMpgAudio);

		if (MPAConverter == null)
			MPAConverter = new MpaConverter();

		if (MPADecoder == null)
			MPADecoder = new MpaDecoder();

		MpaDecoder.RESET = false;

		MpaDecoder.MAX_VALUE = Normalize ? (Integer.parseInt(collection.getSettings().getProperty(Keys.KEY_AudioPanel_NormalizeValue)) * 32767 / 100) : 32767;
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
		MpaConversionMode = collection.getSettings().getIntProperty(Keys.KEY_AudioPanel_losslessMpaConversionMode);

		if (MpaConversionMode > MpaConversion_None)
			Common.setMessage(Resource.getString("audio.convert") + " " + Keys.ITEMS_losslessMpaConversionMode[MpaConversionMode]);

		if (DecodeMpgAudio)
		{
			Common.setMessage(Resource.getString("audio.decode"));
			Common.setMessage("-> " + Keys.ITEMS_resampleAudioMode[collection.getSettings().getIntProperty(Keys.KEY_AudioPanel_resampleAudioMode)]);

			if (Normalize)
				Common.setMessage("-> " + Resource.getString(Keys.KEY_AudioPanel_Normalize[0]) + " " + (100 * MpaDecoder.MAX_VALUE / 32767) + "%");

			if (collection.getSettings().getBooleanProperty(Keys.KEY_AudioPanel_Downmix))
				Common.setMessage("-> " + Resource.getString(Keys.KEY_AudioPanel_Downmix[0]));
	
			if (collection.getSettings().getBooleanProperty(Keys.KEY_AudioPanel_fadeInOut))
				Common.setMessage("-> " + Resource.getString(Keys.KEY_AudioPanel_fadeInOut[0]));

			if (collection.getSettings().getBooleanProperty(Keys.KEY_AudioPanel_changeByteorder))
				Common.setMessage("-> " + Resource.getString(Keys.KEY_AudioPanel_changeByteorder[0]));

			if (collection.getSettings().getBooleanProperty(Keys.KEY_AudioPanel_addRiffHeader))
				Common.setMessage("-> " + Resource.getString(Keys.KEY_AudioPanel_addRiffHeader[0]));

			if (collection.getSettings().getBooleanProperty(Keys.KEY_AudioPanel_addAiffHeader))
				Common.setMessage("-> " + Resource.getString(Keys.KEY_AudioPanel_addAiffHeader[0]));
		}

		/**
		 * restart loop
		 */
		while (processAudio(collection, xInputFile, filename_pts, filename_type, videofile_pts, isElementaryStream))
		{
			CommonParsing.setAudioProcessingFlags(CommonParsing.getAudioProcessingFlags() & ~0xCL);

			Common.setMessage(" ");
			Common.setMessage(Resource.getString("audio.restart") + " " + ((CommonParsing.getAudioProcessingFlags()>>>18) - 1));

			if (DecodeMpgAudio && Normalize)
				Common.setMessage("-> normalize: multiply factor: " + MpaDecoder.MULTIPLY);

			if ( (0x10000L & CommonParsing.getAudioProcessingFlags()) != 0) 
				MpaConversionMode = MpaConversion_None;

			MPAConverter.resetBuffer();
		}

		CommonParsing.setAudioProcessingFlags(CommonParsing.getAudioProcessingFlags() & 3L);
	}


	/**
	 *  method for audio processing 
 	 */
	private boolean processAudio(JobCollection collection, XInputFile xInputFile, String filename_pts, String filename_type, String videofile_pts, int isElementaryStream)
	{
		String fchild = isElementaryStream == CommonParsing.ES_TYPE ? collection.getOutputName(xInputFile.getName()) : xInputFile.getName();
		String fparent = collection.getOutputNameParent(fchild);

		Common.getGuiInterface().showAVOffset(Resource.getString("MainPanel.AudioVideoOffset"));
		Common.getGuiInterface().showExportStatus(Resource.getString("MainPanel.nonVideoExportStatus"));

		JobProcessing job_processing = collection.getJobProcessing();

		if (isElementaryStream == CommonParsing.ES_TYPE && job_processing.getSplitSize() > 0) 
			fparent += "(" + job_processing.getSplitPart() + ")";

		FileName_Ch1 = fparent + ".$mpL$";
		FileName_Ch2 = fparent + ".$mpR$";

		getSettings(collection);

		messageSettings();

		boolean insertSilenceLoop = false;
		boolean preloop = true;
		boolean missing_syncword = false;
		boolean is_DTS = false;
		boolean is_AC3 = false;

		ContainsAudioPTS = false;
		ContainsVideoPTS = false;
		HasNewFormat = true;
		WriteEnabled = false;

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

		FramePosition = 0;
		CurrentFramePosition = 0;

		TimePosition = 0;
		TimeCounter = 0.0;

		ModeChangeCount = 0;
		ModeChangeCount_JSS = 0;

		FileLength = 0;

		int pitch[] = { 1, PitchValue };
		int minSync = 0;

		FrameExportInfo = new FrameExportInfo();

		AudioType = -1;

		int x = 0; 

		int v = 0;
		int w = 0;
		int[] vw = new int[2];

		int layer = 0;
		int returncode = 0;

		int es_streamtype = determineStreamType(filename_type);

		/**
		 * pre-init audioparser
		 */
		AudioFormat audio = new AudioFormat(es_streamtype);

		/**
		 * pre-check for toggling ac3<->dts
		 */
		AudioFormat test_audio = new AudioFormat(CommonParsing.DTS_AUDIO);

		try {

			//System.gc();

			long[][] obj = loadTempOtherPts(filename_pts, "audio.msg.pts.discard", "audio.msg.pts.firstonly", "audio.msg.pts.start_end", "", 0, IgnoreErrors, Debug);

			if (obj != null)
			{
				ptsval = obj[0];
				ptspos = obj[1];
				ContainsAudioPTS = true;
				obj = null;
			}

			obj = loadTempVideoPts(videofile_pts, Debug);

			if (obj != null)
			{
				vptsval = obj[0];
				vtime = obj[1];
				ContainsVideoPTS = true;
				obj = null;
			}

			//System.gc();

			FileLength = xInputFile.length();

			long[] addf = { 0, 0 };

			if (FileLength < 1000) 
				Common.setMessage(" Filesize < 1000 byte");

			initInputStream(xInputFile);

			initOutputStreams();

			initProjectFiles();

			ByteArrayOutputStream silentFrameBuffer = new ByteArrayOutputStream();


			// check pts matching area
			if (ContainsVideoPTS && ContainsAudioPTS)
			{
				int jump = checkPTSMatch(vptsval, ptsval);

				if (jump < 0)
				{
					Common.setMessage(Resource.getString("audio.msg.pts.mismatch"));  
					ContainsVideoPTS = false; 
					x = 0; 
				}

				else
					x = jump;
			}

			// processing with or without video-main-pts
			if (ContainsVideoPTS) 
				Common.setMessage(Resource.getString("audio.msg.adjust.at.videopts"));

			else if (ContainsAudioPTS && isElementaryStream != CommonParsing.ES_TYPE) 
				Common.setMessage(Resource.getString("audio.msg.adjust.at.ownpts"));


			// the index to start with processing
			if (ContainsAudioPTS)
			{ 
				TimePosition = ptsval[x]; 
				FramePosition = ptspos[x]; 
			}

			if (FramePosition > 0) 
				skipInputStream(FramePosition);

			//init extra wave header
			audio.initExtraWaveHeader(AddWaveHeaderACM, AddWaveHeaderBWF, AddWaveHeaderAC3);

			// add preceding wave header
			addWaveHeader(audio, es_streamtype);

			// main loop
		//	while ((returncode = processData(audio, es_streamtype) < 0)
		//	{}

			bigloop:
			while (true)
			{
				// init FFT/window for mpa decoding for 1 file
				initDecoder(audio, es_streamtype);

				// init decoding of ancillary data
				audio.setAncillaryDataDecoder(Message_7, Debug);

				/**
				 *  PCM Audio
				 */
				if (es_streamtype == CommonParsing.LPCM_AUDIO)
				{
					processPCMData(job_processing, audio, vptsval, ptsval);
					break bigloop;
				}


				/**
				 *  AC-3/DTS Audio
				 */
				readloopdd:
				while ((es_streamtype == CommonParsing.AC3_AUDIO || es_streamtype == CommonParsing.DTS_AUDIO) && FramePosition < FileLength - 10)
				{
					Common.updateProgressBar(FramePosition, FileLength);

					if (Debug) 
						System.out.println("FramePosition " + FramePosition);

					while (pause())
					{}

					if (isCancelled(job_processing))
						break bigloop; 

					if (ptspos[x + 1] != -1 && FramePosition > ptspos[x + 1])
					{
						Common.setMessage(Resource.getString("audio.msg.pts.wo_frame") + " (" + ptspos[x + 1] + "/" + FramePosition + ")");
						x++;
					}

					/** 
					 * read 10 bytes for headercheck 
					 */
					FramePosition += readInputStream(pushback, 0, 10);

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
							unreadInputStream(pushback, 1, 9);

							if (Message_2 && !missing_syncword)
								Common.setMessage(Resource.getString("audio.msg.syncword.lost", " " + (FramePosition - 10)) + " " + formatFrameTime(TimeCounter));

							missing_syncword = true; 
							FramePosition -= 9; 

							continue readloopdd; 
						} 

						is_DTS = true; 
						is_AC3 = false;

						//set special type
						es_streamtype = CommonParsing.DTS_AUDIO;

						audio = new AudioFormat(es_streamtype);
						audio.parseHeader(pushback, 0);
					}

					else
					{ 
						is_DTS = false;
						is_AC3 = true;
					}

					OutputStream_Ch1.setWave(CreateDDWave, is_AC3, is_DTS, audio.getBitrate());

					/**
					 * prepare fo read entire frame 
					 */
					FramePosition -= unreadInputStream(pushback, 0, 10);

					/**
					 * read entire frame 
					 */
					frame = new byte[audio.getSize()];

					readInputStream(frame, 0, audio.getSize());

					/**
					 * startfileposition of current frame 
					 */
					CurrentFramePosition = FramePosition;

					/**
					 * expected position for following frame 
					 */
					FramePosition += audio.getSize();

					if (PitchAudio)
					{  // skip a frame
						if (pitch[1] * pitch[0] == FrameExportInfo.getWrittenFrames())
						{
							Common.setMessage(Resource.getString("audio.msg.frame.discard") + " " + FrameExportInfo.getWrittenFrames() + " (" + pitch[0] + ")");
							pitch[0]++;

							continue readloopdd;
						}
					}

					/**
					 * finish loop if last frame in file is shorter than nominal size 
					 */
					if ( FramePosition > FileLength ) 
						break readloopdd; 

					/**
					 * read following frame header, not if it is the last frame 
					 * check following frameheader for valid , if not starting with next byte 
					 */
					if (FramePosition < FileLength - 10)
					{
						int d = 0;

						if (!AllowSpaces)
						{
							readInputStream(push24, 0, 24);

							miniloop:
							for (; d < (is_DTS ? 15 : 17); d++)
							{ //smpte
								ERRORCODE = audio.parseNextHeader(push24, d);

								if (ERRORCODE > 0) 
									break miniloop; 
							} 

							unreadInputStream(push24, 0, 24);
						}

						if (ERRORCODE < 1)
						{ 
							unreadInputStream(frame, 1, frame.length - 1);

							FramePosition = CurrentFramePosition + 1; 

							continue readloopdd; 
						}
						else
						{
							AudioType = is_DTS ? DTS_AUDIOSTREAM : AC3_AUDIOSTREAM;

							skipInputStream(d);

							FramePosition += d;
						}
					}

					if (ValidateCRC && (ERRORCODE = audio.validateCRC(frame, 2, audio.getSize())) != 0 )
					{
						Common.setMessage(Resource.getString("audio.msg.crc.error", "" + ERRORCODE) + " " + CurrentFramePosition);

						unreadInputStream(frame, 2, frame.length - 2);

						FramePosition = CurrentFramePosition + 2;

						continue readloopdd; 
					}

					if (Message_2 && missing_syncword)
						Common.setMessage(Resource.getString("audio.msg.syncword.found") + " " + CurrentFramePosition);

					missing_syncword = false;

					/**
					 * check for change in frametype 
					 */ 
					determineFormatChange(audio, es_streamtype);

					audio.saveHeader();

					frame = getReplacementFrame(audio, frame, es_streamtype);

					// TimePosition ist hier aktuelle audiopts

					Common.setFps(FrameExportInfo.getWrittenFrames());

					/**
					 * preloop if audio starts later than video, and i must insert 
					 */
					if ( (preloop && v >= vptsval.length) || !( preloop && ContainsVideoPTS && vptsval[v] < TimePosition - (audio.getFrameTimeLength() / 2.0) ) ) 
						preloop=false;

					else
					{
						/**
						 * patch ac-3 to 3/2 
						 */
						if (!is_DTS && Patch1stAc3Header && FrameExportInfo.getWrittenFrames() == 0)
							frame = audio.editFrame(frame, audio.getSize(), 1);

						long precount = vptsval[v];
						long[] ins = { (long)TimeCounter, 0 };

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
						while (precount < TimePosition - (audio.getFrameTimeLength() / 2.0))
						{
							/**
							 * check if frame write should paused 
							 */
							if (ContainsVideoPTS && w < vptsval.length)
							{ 
								double ms1 = (double) (precount - vptsval[w + 1]);
								double ms2 = (double) (TimeCounter - vtime[w + 1]);

								if ((double) Math.abs(ms2) <= audio.getFrameTimeLength() / 2.0 )
								{
									WriteEnabled = false;
									w += 2;
								}

								else if ((double) Math.abs(ms1) <= audio.getFrameTimeLength() / 2.0 )
								{
									WriteEnabled = false;
									w += 2;
								}
							}

							/**
							 * calculate A/V Offset for true 
							 */
							if (ContainsVideoPTS && (v < vptsval.length))
							{
								double ms3 = precount - vptsval[v];
								double ms4 = TimeCounter - vtime[v];

								if (Debug) 
									System.out.println(" ö" + ms3 + "/" + ms4 + "/" + (ms4 - ms3));

								if (!WriteEnabled && (double) Math.abs((TimeCounter - vtime[v]) - (precount - vptsval[v])) <= (double) audio.getFrameTimeLength() / 2.0 )
								{
									WriteEnabled = true;
									v += 2;

									double ms1 = precount - vptsval[v - 2];
									double ms2 = TimeCounter - vtime[v - 2];

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
									WriteEnabled = true;
									v += 2;

									double ms1 = precount - vptsval[v - 2];
									double ms2 = TimeCounter - vtime[v - 2];

									Common.getGuiInterface().showAVOffset("" + (int)(ms1 / 90) + "/" + (int)(ms2 / 90) + "/" + (int)((ms2 - ms1) / 90));

									if (Debug) 
										System.out.println(" ü" + ms1 + "/" + ms2 + "/" + (ms2 - ms1));
								}

								/**
								 * calculate A/V Offset for false 
								 */
								if (WriteEnabled && (double) Math.abs((TimeCounter - vtime[v - 2]) - (precount - vptsval[v-2])) > (double) audio.getFrameTimeLength() / 2.0 )
								{
									WriteEnabled = false;
									v -= 2;
								}
							}

							/**
							 * write message 
							 */
							Common.getGuiInterface().showExportStatus((WriteEnabled || !ContainsVideoPTS) ? Resource.getString("audio.status.pre-insert") : Resource.getString("audio.status.pause"));

							/**
							 * stop if no more audio needed 
							 */
							if (!checkLastAudioBound(ContainsVideoPTS, precount, vptsval, FileLength))
								break readloopdd;


							if (WriteEnabled)
							{
								writeChannel1(silentFrameBuffer.toByteArray());

								/**
								 * RIFF 
								 */
								if (!is_DTS && AddWaveHeaderAC3) 
									audio.parseRiffData(frame, 1); 

								FrameExportInfo.countWrittenFrames(1);
								FrameExportInfo.countPreInsertedFrames(1);

								ins[1]++;
								TimeCounter += audio.getFrameTimeLength();
							}

							precount += audio.getFrameTimeLength();

							if (Debug) 
								System.out.println(FrameExportInfo.getSummary() + "  @ " + formatFrameTime(TimeCounter) + "  ");

						} // end while

						FramePosition = CurrentFramePosition;

						unreadInputStream(frame, 0, frame.length);

						if (ins[1] > 0)
							Common.setMessage(Resource.getString("audio.msg.summary.pre-insert", "" + ins[1], FramesToTime((int)ins[1], audio.getFrameTimeLength())) + " " + formatFrameTime(ins[0]));

						continue readloopdd;
					} // end if preloop


					/**
					 * check if frame write should pause 
					 */
					if (ContainsVideoPTS)
					{ 
						vw[0] = v;
						vw[1] = w;

						WriteEnabled = SyncCheck(vw, TimeCounter, audio.getFrameTimeLength(), TimePosition, FrameExportInfo.getWrittenFrames(), vptsval, vtime, WriteEnabled, Debug);

						v = vw[0];
						w = vw[1];
					}
					//System.out.println(""+WriteEnabled+"/"+v+"/"+w);


					/**
					 * message
					 */
					Common.getGuiInterface().showExportStatus((WriteEnabled || !ContainsVideoPTS) ? Resource.getString("audio.status.write") : Resource.getString("audio.status.pause"));


					if (Debug) 
						System.out.println(" k)" + TimePosition + " l)" + (audio.getFrameTimeLength() / 2.0) + " u)" + audio.getSize() + " m)" + WriteEnabled + " FramePosition)"+w+" o)"+v+" p)"+FramePosition);

					/**
					 * stop if no more audio needed 
					 */
					if (!checkLastAudioBound(ContainsVideoPTS, TimePosition, vptsval, FileLength))
						break readloopdd;

					/**
					 * message 
					 */
					messageSourceFormat(job_processing, audio, ContainsVideoPTS, TimeCounter);

					/**
					 * remove CRC, unused
					 */
					if (ClearCRC) 
						audio.removeCRC(frame);

					/**
					 * patch ac-3 to 3/2 
					 */
					if (!is_DTS && Patch1stAc3Header && FrameExportInfo.getWrittenFrames() == 0)
						frame = audio.editFrame(frame, audio.getSize(), 1);

					if (Debug)
					{
						System.out.println(FrameExportInfo.getSummary() + "  @ " + formatFrameTime(TimeCounter));
						System.out.println(" x" + ((x < ptspos.length - 1) ? x + "/" + ptsval[x + 1] + "/" + ptspos[x + 1] : "-"));
					}

					/**
					 * pts for next frame!! 
					 */
					TimePosition += audio.getFrameTimeLength();

					silentFrameBuffer.reset();
					silentFrameBuffer.write(frame);

					/**
					 * simple sync
					 */
					if (LimitPts && ptspos[x + 1] != -1 && ptspos[x + 1] < FramePosition)
					{
						if (Debug)
							System.out.println(" minSync " + minSync + "/ " + x);

						if ( (++minSync) < 20)
							x++;
						else
							minSync = 0;
					}

					/**
					 * frame is in last pes packet or packet end not yet reached 
					 */
					if (writeSuccessiveFrame(audio, frame, newframes, ContainsVideoPTS, FramePosition, ptspos, x, es_streamtype))
						continue readloopdd;


					minSync = 0;

					if ( (double) Math.abs(ptsval[x + 1] - TimePosition) < (double) audio.getFrameTimeLength() / 2.0 )
					{
						TimePosition = ptsval[x + 1];
						x++;

						writeFrame(audio, frame, newframes, ContainsVideoPTS, es_streamtype);

						continue readloopdd;
					}

					if (ptsval[x + 1] > TimePosition) 
						insertSilenceLoop = true;

					if (ptsval[x + 1] < TimePosition)
					{
						x++;
						TimePosition = ptsval[x];

						Common.setMessage(Resource.getString("audio.msg.summary.skip") + " " + formatFrameTime(TimeCounter));

						FrameExportInfo.countSkippedFrames(1);
					}

					if (insertSilenceLoop)
					{
						writeFrame(audio, frame, newframes, ContainsVideoPTS, es_streamtype);

						TimePosition += audio.getFrameTimeLength();

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

						long[] ins = { (long)TimeCounter, 0 };


						while (ptsval[x + 1] > (TimePosition - (audio.getFrameTimeLength() / 2.0)) )
						{
							if (ContainsVideoPTS && w < vptsval.length)
							{ 
								double ms1 = (double) (TimePosition - audio.getFrameTimeLength() - vptsval[w + 1]);
								double ms2 = (double) (TimeCounter - vtime[w + 1]);

								if ((double) Math.abs(ms2) <= audio.getFrameTimeLength() / 2.0 )
								{
									WriteEnabled = false;
									w += 2;
								}

								else if ((double) Math.abs(ms1) <= audio.getFrameTimeLength() / 2.0 )
								{
									WriteEnabled = false;
									w += 2;
								}
							}

							if (ContainsVideoPTS && v < vptsval.length)
							{
								if (!WriteEnabled && (double) Math.abs((TimeCounter - vtime[v]) -
										(TimePosition - audio.getFrameTimeLength() - vptsval[v]) ) <= (double) audio.getFrameTimeLength() / 2.0 )
								{
									double ms1 = (double) (TimePosition - audio.getFrameTimeLength() - vptsval[v]);
									double ms2 = (double) (TimeCounter - vtime[v]);

									Common.getGuiInterface().showAVOffset("" + (int)(ms1 / 90) + "/" + (int)(ms2 / 90) + "/" + (int)((ms2 - ms1) / 90));

									if (Debug) 
										System.out.println(" §" + ms1 + "/" + ms2 + "/" + (ms2 - ms1));

									WriteEnabled = true;
									v += 2;
								}
							} 

							if (ContainsVideoPTS && v < vptsval.length)
							{
								if ((double) Math.abs(vptsval[v] - (TimePosition - audio.getFrameTimeLength())) <= ((double) audio.getFrameTimeLength() / 2.0) )
								{
									double ms1 = (double) (TimePosition - audio.getFrameTimeLength() - vptsval[v]);
									double ms2 = (double) (TimeCounter - vtime[v]);

									Common.getGuiInterface().showAVOffset("" + (int)(ms1 / 90) + "/" + (int)(ms2 / 90) + "/" + (int)((ms2 - ms1) / 90));

									if (Debug) 
										System.out.println(" ß" + ms1 + "/" + ms2 + "/" + (ms2 - ms1));

									WriteEnabled = true;
									v += 2;
								}

								if (WriteEnabled && (double) Math.abs((TimeCounter - vtime[v - 2]) -
										(TimePosition - audio.getFrameTimeLength() - vptsval[v - 2]) ) > (double) audio.getFrameTimeLength() / 2.0 )
								{
									WriteEnabled = false;
									v -= 2;
								}
							}

							Common.getGuiInterface().showExportStatus((WriteEnabled || !ContainsVideoPTS) ? Resource.getString("audio.status.insert") : Resource.getString("audio.status.pause"));

							if (!ContainsVideoPTS || (ContainsVideoPTS && WriteEnabled))
							{
								writeChannel1(silentFrameBuffer.toByteArray());

								/**
								 * RIFF 
								 */
								if (!is_DTS && AddWaveHeaderAC3) 
									audio.parseRiffData(silentFrameBuffer.toByteArray(), 1); 

								FrameExportInfo.countWrittenFrames(1);
								FrameExportInfo.countInsertedFrames(1);

								TimeCounter += audio.getFrameTimeLength();
								ins[1]++;
							}

							if (Debug)
							{
								System.out.println(FrameExportInfo.getSummary() + "  @ " + formatFrameTime(TimeCounter) + " ");
								System.out.println(" t)" + TimePosition);
								System.out.println(" x" + ((x < ptspos.length - 1) ? x + "/" + ptsval[x + 1] + "/" + ptspos[x + 1] : "-"));
							}

							TimePosition += audio.getFrameTimeLength();
						} // end while

						TimePosition -= audio.getFrameTimeLength();
						insertSilenceLoop = false;
						x++;

						if (ins[1] > 0)
							Common.setMessage(Resource.getString("audio.msg.summary.insert", "" + ins[1], FramesToTime((int)ins[1], audio.getFrameTimeLength())) + " " + formatFrameTime(ins[0]));

						/**
						 * reset PTS after inserting
						 */
						TimePosition = ptsval[x];

						continue readloopdd;
					} // end if insertSilenceLoop

					if ( (CurrentFramePosition + audio.getSize()) >= FileLength ) 
						break readloopdd;

				}  // end while

				/**
				 * add frames at the end 
				 */
				if ((es_streamtype == CommonParsing.AC3_AUDIO || es_streamtype == CommonParsing.DTS_AUDIO) && AddFrames && ContainsVideoPTS && WriteEnabled && (w < vptsval.length))
				{
					TimePosition += audio.getFrameTimeLength();
					addf[0] = (long) TimeCounter;

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
						while (vtime[w + 1] > TimeCounter && (double) Math.abs(vtime[w + 1] - TimeCounter) > (double) audio.getFrameTimeLength() / 2.0)
						{
							writeChannel1(silentFrameBuffer.toByteArray());

							/**
							 * RIFF 
							 */
							if (!is_DTS && AddWaveHeaderAC3) 
								audio.parseRiffData(silentFrameBuffer.toByteArray(), 1); 

							Common.getGuiInterface().showExportStatus(Resource.getString("audio.status.add")); 

							FrameExportInfo.countWrittenFrames(1);
							FrameExportInfo.countAddedFrames(1);

							TimeCounter += audio.getFrameTimeLength();
							TimePosition += audio.getFrameTimeLength();
							addf[1]++;

							if (Debug)
							{ 
								System.out.println(FrameExportInfo.getSummary() + "  @ " + formatFrameTime(TimeCounter));
								System.out.print(" t)" + (long)(TimePosition - audio.getFrameTimeLength()) + " w)" + w);
							}
						}

						w += 2;
					}

					w -= 2;
					TimePosition -= audio.getFrameTimeLength();

					if (Debug) 
						System.out.println(" eot_video:" + (vptsval[w + 1] / 90) + "ms, eot_audio:" + (TimePosition / 90) + "ms ");
				}

// mpa start

				/**
				 *  MPEG1+2 Audio Layer 1,2,3
				 */
				readloop:
				while (es_streamtype == CommonParsing.MPEG_AUDIO && FramePosition < FileLength - 4)
				{
					Common.updateProgressBar(FramePosition, FileLength);

					if (Debug) 
						System.out.println(" FramePosition" + FramePosition);

					while (pause())
					{}

					if (isCancelled(job_processing))
						break bigloop;

					/**
					 * updates global audio_error and framecounter variable
					 */
					CommonParsing.setAudioProcessingFlags((0x3FFFFL & CommonParsing.getAudioProcessingFlags()) | ((long)FrameExportInfo.getWrittenFrames())<<18);
					
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

					if (ptspos[x + 1] != -1 && FramePosition > ptspos[x + 1])
					{
						Common.setMessage(Resource.getString("audio.msg.pts.wo_frame") + " (" + ptspos[x + 1] + "/" + FramePosition + ")");
						x++;
					}

					/**
					 * read 4 bytes for headercheck 
					 */
					FramePosition += readInputStream(pushmpa, 0, 4);

					/**
					 * parse header 
					 */
					if ((ERRORCODE = audio.parseHeader(pushmpa, 0)) < 1)
					{
						unreadInputStream(pushmpa, 1, 3);

						if (Message_2 && !missing_syncword)
							Common.setMessage(Resource.getString("audio.msg.syncword.lost", " " + (FramePosition - 4)) + " " + formatFrameTime(TimeCounter));

						missing_syncword = true;
						FramePosition -= 3;

						continue readloop;
					}

					/**
					 * prepare fo read entire frame 
					 */
					FramePosition -= unreadInputStream(pushmpa, 0, 4);

					/**
					 * read entire frame 
					 */
					frame = new byte[audio.getSize()];

					readInputStream(frame, 0, audio.getSize());

					System.arraycopy(frame, 0, header_copy, 0, 4);
					header_copy[3] &= 0xCF;
					header_copy[2] &= ~2;

					/**
					 * startfileposition of current frame 
					 */
					CurrentFramePosition = FramePosition;

					/**
					 * expected position for following frame 
					 */
					FramePosition += audio.getSize();

					/**
					 * pitch 
					 */
					if (PitchAudio)
					{  // skip a frame
						if (pitch[1] * pitch[0] == FrameExportInfo.getWrittenFrames())
						{
							Common.setMessage(Resource.getString("audio.msg.frame.discard") + " " + FrameExportInfo.getWrittenFrames() + " (" + pitch[0] + ")");
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
					if (FramePosition > FileLength) 
						break readloop; 

					/**
					 * read following frame header, not if it is the last frame 
					 * check following frameheader for valid mpegaudio, if not starting with next byte 
					 */
					if (FramePosition < FileLength - 4)
					{
						if (!AllowSpaces)
						{
							readInputStream(pushmpa, 0, 4);

							ERRORCODE = audio.parseNextHeader(pushmpa, 0);

							unreadInputStream(pushmpa, 0, 4);

							if (ERRORCODE < 1)
							{
								unreadInputStream(frame, 1, frame.length - 1);

								FramePosition = CurrentFramePosition + 1;

								continue readloop;
							}
						}

						AudioType = audio.getLayer();
					}

					if (ValidateCRC && (ERRORCODE = audio.validateCRC(frame, 0, audio.getSize())) != 0 )
					{
						Common.setMessage(Resource.getString("audio.msg.crc.error", "") + " " + CurrentFramePosition);

						unreadInputStream(frame, 2, frame.length - 2);

						FramePosition = CurrentFramePosition + 2;

						continue readloop;
					}

					/**
					 * check for change in frametype
					 */
					// new pos
					if (!determineFormatChange(audio, es_streamtype))
					{
						if (!missing_syncword)
							Common.setMessage("!> change in frame type not accepted @ " + CurrentFramePosition);

						unreadInputStream(frame, 2, frame.length - 2);

						FramePosition = CurrentFramePosition + 2;

						continue readloop;
					}

					if (Message_2 && missing_syncword)
						Common.setMessage(Resource.getString("audio.msg.syncword.found") + " " + CurrentFramePosition);

					missing_syncword = false;

					/**
					 * check for change in frametype 
					 */
			//		determineFormatChange(audio, es_streamtype);

					audio.saveHeader();
					audio.decodeAncillaryData(frame, formatFrameTime(TimeCounter));

					// TimePosition ist hier aktuelle audiopts

					Common.setFps(FrameExportInfo.getWrittenFrames());

					/**
					 * message 
					 */
					if (Debug) 
						System.out.println(" k)" +TimePosition +" l)" + (audio.getFrameTimeLength() / 2.0) + " m)" + WriteEnabled + " FramePosition)" + w + " o)" + v + " p)" + FramePosition);


					/**
					 * preloop if audio starts later than video, and i must insert 
					 */
					if ( (preloop && ContainsVideoPTS && v >= vptsval.length) || !( preloop && ContainsVideoPTS && vptsval[v] < TimePosition - (audio.getFrameTimeLength() / 2.0) ) ) 
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
						long[] ins = { (long)TimeCounter, 0 };

						while ( precount < TimePosition - (audio.getFrameTimeLength() / 2.0) )
						{  //better for RTS
							/**
							 * check if frame write should pause 
							 */
							if (ContainsVideoPTS && w < vptsval.length)
							{ 
								double ms1 = (double) (precount - vptsval[w + 1]);
								double ms2 = (double) (TimeCounter - vtime[w + 1]);

								if ( (double) Math.abs(ms2) <= audio.getFrameTimeLength() / 2.0 )
								{
									WriteEnabled = false;
									w += 2;
								}
								else if ((double) Math.abs(ms1) <= audio.getFrameTimeLength() / 2.0 )
								{
									WriteEnabled = false;
									w += 2;
								}
							}

							/**
							 * calculate A/V Offset for true 
							 */
							if (ContainsVideoPTS && v < vptsval.length)
							{
								double ms3 = precount - vptsval[v];
								double ms4 = TimeCounter - vtime[v];

								if (Debug) 
									System.out.println(" ö" + ms3 + "/" + ms4 + "/" + (ms4 - ms3));

								if (!WriteEnabled && (double) Math.abs((TimeCounter - vtime[v]) -
										(precount - vptsval[v]) ) <= (double)audio.getFrameTimeLength() / 2.0 )
								{
									WriteEnabled = true;
									v += 2;

									double ms1 = precount - vptsval[v - 2];
									double ms2 = TimeCounter - vtime[v - 2];

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
									WriteEnabled = true;
									v += 2;

									double ms1 = precount - vptsval[v - 2];
									double ms2 = TimeCounter - vtime[v - 2];

									Common.getGuiInterface().showAVOffset("" + (int)(ms1 / 90) + "/" + (int)(ms2 / 90) + "/" + (int)((ms2 - ms1) / 90));

									if (Debug) 
										System.out.println(" ü" + ms1 + "/" + ms2 + "/" + (ms2 - ms1));
								}

								/**
								 * calculate A/V Offset for false 
								 */
								if (WriteEnabled && Math.abs((TimeCounter - vtime[v - 2]) -
										(precount - vptsval[v - 2]) ) > audio.getFrameTimeLength() / 2.0 )
								{
									WriteEnabled = false;
									v -= 2;
								}
							}

							/**
							 * message 
							 */
							Common.getGuiInterface().showExportStatus((WriteEnabled || !ContainsVideoPTS) ? Resource.getString("audio.status.pre-insert") : Resource.getString("audio.status.pause")); 

							/**
							 * stop if no more audio needed 
							 */
							if (!checkLastAudioBound(ContainsVideoPTS, precount, vptsval, FileLength))
								break readloop;


							if (WriteEnabled)
							{
								if (FillGapsWithLastFrame)// copy last frame
								{		
									if (audio.getLayer() > 0 && DecodeMpgAudio)
										writeChannels(MpaDecoder.decodeArray(copyframe[0]), MpaDecoder.get2ndArray());

									else if (MpaConversionMode > MpaConversion_None)
									{
										newframes = MPAConverter.modifyframe(copyframe[0], MpaConversionMode); 

										writeChannels(newframes); 

										audio.parseRiffData(newframes[0], 1); 

										if (MpaConversionMode >= MpaConversion_Mode4) 
												audio.parseRiffData(newframes[1], 2);
									}

									else
									{
										writeChannel1(copyframe[0]); 

										audio.parseRiffData(copyframe[0], 1);
									}
								}
								else
								{
									//if (padding_counter==padding) padding_counter=0;	//reset padd count
									//else if (samplerate==0) padding_counter++;		//count padding

									if (audio.getLayer() > 0 && DecodeMpgAudio)
										writeChannels(MpaDecoder.decodeArray(silent_Frame[(padding_counter > 0) ? 0 : 1]), MpaDecoder.get2ndArray());

									else if (MpaConversionMode > MpaConversion_None)
									{
										newframes = MPAConverter.modifyframe(silent_Frame[(padding_counter > 0) ? 0 : 1], MpaConversionMode);

										writeChannels(newframes);

										audio.parseRiffData(newframes[0], 1); 

										if (MpaConversionMode >= MpaConversion_Mode4) 
											audio.parseRiffData(newframes[1], 2);
									}

									else
									{ 
										writeChannel1(silent_Frame[(padding_counter > 0) ? 0 : 1]);

										audio.parseRiffData(silent_Frame[(padding_counter > 0) ? 0 : 1], 1);
									}
								}

								FrameExportInfo.countWrittenFrames(1);
								FrameExportInfo.countPreInsertedFrames(1);

								TimeCounter += audio.getFrameTimeLength();
								ins[1]++;
							}

							precount += audio.getFrameTimeLength();

							if (Debug) 
								System.out.println(FrameExportInfo.getSummary() + "  @ " + formatFrameTime(TimeCounter));
						} /** end while **/

						FramePosition = CurrentFramePosition;

						unreadInputStream(frame, 0, frame.length);

						if (ins[1] > 0)
							Common.setMessage(Resource.getString("audio.msg.summary.pre-insert", "" + ins[1], FramesToTime((int)ins[1], audio.getFrameTimeLength())) + " " + formatFrameTime(ins[0]));

						continue readloop;
					} 


					/**
					 * check if frame write should pause 
					 */
					if (ContainsVideoPTS)
					{ 
						vw[0] = v;
						vw[1] = w;

						WriteEnabled = SyncCheck(vw, TimeCounter, audio.getFrameTimeLength(), TimePosition, FrameExportInfo.getWrittenFrames(), vptsval, vtime, WriteEnabled, Debug);

						v = vw[0];
						w = vw[1];
					}
					//  System.out.println(""+WriteEnabled+"/"+v+"/"+w);

					/**
					 * message 
					 */
					Common.getGuiInterface().showExportStatus((WriteEnabled || !ContainsVideoPTS) ? Resource.getString("audio.status.write") : Resource.getString("audio.status.pause"));

					/**
					 * stop if no more audio needed 
					 */
					if (!checkLastAudioBound(ContainsVideoPTS, TimePosition, vptsval, FileLength))
						break readloop;

					/**
					 * message 
					 */
					messageSourceFormat(job_processing, audio, ContainsVideoPTS, TimeCounter);

					/**
					 * pts for next frame!! 
					 */
					TimePosition += audio.getFrameTimeLength();

					/**
					 * message 
					 */
					if (Debug)
					{
						System.out.println(FrameExportInfo.getSummary() + "  @ " + formatFrameTime(TimeCounter));
						System.out.println("ntl: " + TimePosition + " x" + ((x < ptspos.length - 1) ? x + "/" + ptsval[x + 1] + "/" + ptspos[x + 1] : "-"));
					}

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
					//	System.out.print(" tl"+TimePosition+" /px "+ptsval[x]+" /1_"+ptsval[x+1]+" /p1-tl "+(ptsval[x+1]-TimePosition)+" /pp1 "+ptspos[x+1]+" /FramePosition "+FramePosition);

					/**
					 * simple sync
					 */
					if (LimitPts && ptspos[x + 1] != -1 && ptspos[x + 1] < FramePosition)
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
					if (writeSuccessiveFrame(audio, frame, newframes, ContainsVideoPTS, FramePosition, ptspos, x, es_streamtype))
						continue readloop;

					minSync = 0;

					/**
					 * frame is on pes packet corner 
					 */
					if ((double) Math.abs(ptsval[x + 1] - TimePosition) < (double) audio.getFrameTimeLength() / 2.0 )
					{
						TimePosition = ptsval[x + 1];
						x++;

						writeFrame(audio, frame, newframes, ContainsVideoPTS, es_streamtype);

						continue readloop;
					}

					if (ptsval[x + 1] > TimePosition) 
						insertSilenceLoop = true;

					if (ptsval[x + 1] < TimePosition)
					{
						x++;
						TimePosition = ptsval[x];

						Common.setMessage(Resource.getString("audio.msg.summary.skip") + " " + formatFrameTime(TimeCounter));

						FrameExportInfo.countSkippedFrames(1);
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
						long[] ins = { (long)TimeCounter, 0 };
		
						// solange nächster ptsval minus nächster framebeginn  ist größer der halben framezeit, füge stille ein
						while (ptsval[x + 1] > (TimePosition - (audio.getFrameTimeLength() / 2.0)))
						{
							if (ContainsVideoPTS && w < vptsval.length)
							{ 
								double ms1 = (double) (TimePosition - audio.getFrameTimeLength() - vptsval[w + 1]);
								double ms2 = (double) (TimeCounter - vtime[w + 1]);

								if ((double) Math.abs(ms2) <= audio.getFrameTimeLength() / 2.0)
								{
									WriteEnabled = false;
									w += 2;
								}
								else if ((double) Math.abs(ms1) <= audio.getFrameTimeLength() / 2.0)
								{
									WriteEnabled = false;
									w += 2;
								}
							}

							if (ContainsVideoPTS && v < vptsval.length)
							{
								if (!WriteEnabled && (double) Math.abs((TimeCounter - vtime[v]) -
									(TimePosition - audio.getFrameTimeLength() - vptsval[v]) ) <= (double) audio.getFrameTimeLength() / 2.0 )
								{
									double ms1 = (double) (TimePosition - audio.getFrameTimeLength() - vptsval[v]);
									double ms2 = (double) (TimeCounter - vtime[v]);

									Common.getGuiInterface().showAVOffset("" + (int)(ms1 / 90) + "/" + (int)(ms2 / 90) + "/" + (int)((ms2 - ms1) / 90));

									if (Debug) 
										System.out.println(" §" + ms1 + "/" + ms2 + "/" + (ms2 - ms1));

									WriteEnabled = true;
									v += 2;
								}
							} 

							if (ContainsVideoPTS && v < vptsval.length)
							{
								if ((double) Math.abs(vptsval[v] - (TimePosition - audio.getFrameTimeLength())) <= ((double) audio.getFrameTimeLength() / 2.0) )
								{
									double ms1 = (double) (TimePosition - audio.getFrameTimeLength() - vptsval[v]);
									double ms2 = (double) (TimeCounter - vtime[v]);

									Common.getGuiInterface().showAVOffset("" + (int)(ms1 / 90) + "/" + (int)(ms2 / 90) + "/" + (int)((ms2 - ms1) / 90));

									if (Debug) 
										System.out.println(" ß" + ms1 + "/" + ms2 + "/" + (ms2 - ms1));

									WriteEnabled = true;
									v += 2;
								}

								if (WriteEnabled && (double) Math.abs((TimeCounter - vtime[v - 2]) -
									(TimePosition - audio.getFrameTimeLength() - vptsval[v - 2]) ) > (double) audio.getFrameTimeLength() / 2.0 )
								{
									WriteEnabled = false;
									v -= 2;
								}
							}

							/**
							 * message 
							 */
							Common.getGuiInterface().showExportStatus((WriteEnabled || !ContainsVideoPTS) ? Resource.getString("audio.status.insert") : Resource.getString("audio.status.pause")); 

  
							if (!ContainsVideoPTS || (ContainsVideoPTS && WriteEnabled))
							{
								if (FillGapsWithLastFrame)
								{
									if (audio.getLayer() > 0 && DecodeMpgAudio)
										writeChannels(MpaDecoder.decodeArray(copyframe[0]), MpaDecoder.get2ndArray());

									else if (MpaConversionMode != MpaConversion_None)
									{
										newframes = MPAConverter.modifyframe(copyframe[0], MpaConversionMode); 

										writeChannels(newframes);

										audio.parseRiffData(newframes[0], 1); 

										if (MpaConversionMode >= MpaConversion_Mode4) 
											audio.parseRiffData(newframes[1], 2);
									}
									else
									{
										writeChannel1(copyframe[0]);

										audio.parseRiffData(copyframe[0], 1);
									}
								}
								else
								{
									//if (padding_counter==padding) padding_counter=0;	//reset padd count
									//else if (samplerate==0) padding_counter++;		//count padding

									if (audio.getLayer() > 0 && DecodeMpgAudio)
										writeChannels(MpaDecoder.decodeArray(silent_Frame[(padding_counter > 0) ? 0 : 1]), MpaDecoder.get2ndArray());

									else if (MpaConversionMode != MpaConversion_None)
									{
										newframes = MPAConverter.modifyframe(silent_Frame[(padding_counter > 0) ? 0 : 1], MpaConversionMode);

										writeChannels(newframes);

										audio.parseRiffData(newframes[0], 1); 

										if (MpaConversionMode >= MpaConversion_Mode4) 
											audio.parseRiffData(newframes[1], 2);
									}
									else
									{
										writeChannel1(silent_Frame[(padding_counter > 0) ? 0 : 1]);

										audio.parseRiffData(silent_Frame[(padding_counter > 0) ? 0 : 1], 1); 
									}
								}

								FrameExportInfo.countWrittenFrames(1);
								FrameExportInfo.countInsertedFrames(1);

								TimeCounter += audio.getFrameTimeLength();
								ins[1]++;
							}

							if (Debug)
							{
								System.out.println(FrameExportInfo.getSummary() + "  @ " + formatFrameTime(TimeCounter));
								System.out.println("tl: " + TimePosition + " x" + ((x < ptspos.length - 1) ? x + "/" + ptsval[x + 1] + "/" + ptspos[x + 1] : "-"));
							}

							TimePosition += audio.getFrameTimeLength();
						} // end while

						TimePosition -= audio.getFrameTimeLength();
						insertSilenceLoop = false;
						x++;

						if (ins[1] > 0) 
							Common.setMessage(Resource.getString("audio.msg.summary.insert", "" + ins[1], FramesToTime((int)ins[1], audio.getFrameTimeLength())) + " " + formatFrameTime(ins[0]));

						/**
						 * reset PTS after inserting
						 */
						TimePosition = ptsval[x];

						continue readloop;
					}

					if ( (CurrentFramePosition + audio.getSize()) >= FileLength ) 
						break readloop; 
				}  // end while

				if (Debug) 
					System.out.println(FrameExportInfo.getSummary() + "  @ " + formatFrameTime(TimeCounter));

				/**
				 * add frames at the end 
				 */
				if (es_streamtype == CommonParsing.MPEG_AUDIO && AddFrames && ContainsVideoPTS && WriteEnabled && (w < vptsval.length))
				{
					TimePosition += audio.getFrameTimeLength();
					addf[0] = (long) TimeCounter;

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
						while ( vtime[w + 1] > TimeCounter && 
							(double) Math.abs(vtime[w + 1] - TimeCounter) > (double) audio.getFrameTimeLength() / 2.0 )
						{
							if (FillGapsWithLastFrame)	//add_copy prev. frame
							{
								if (audio.getLayer() > 0 && DecodeMpgAudio) 
									writeChannels(MpaDecoder.decodeArray(copyframe[0]), MpaDecoder.get2ndArray());

								else if (MpaConversionMode != MpaConversion_None)
								{		//modify frame
									newframes = MPAConverter.modifyframe(copyframe[0], MpaConversionMode); 

									writeChannels(newframes);

									audio.parseRiffData(newframes[0], 1); 

									if (MpaConversionMode >= MpaConversion_Mode4) 
										audio.parseRiffData(newframes[1], 2);
								}

								else
								{
									writeChannel1(copyframe[0]);

									audio.parseRiffData(copyframe[0], 1);
								}
							}

							else
							{	//add silence
								//if (padding_counter==padding) padding_counter=0;	//reset padd count
								//else if (samplerate==0) padding_counter++;		//count padding

								if (audio.getLayer() > 0 && DecodeMpgAudio)
									writeChannels(MpaDecoder.decodeArray(silent_Frame[(padding_counter > 0) ? 0 : 1]), MpaDecoder.get2ndArray());

								else if (MpaConversionMode != MpaConversion_None)
								{
									newframes = MPAConverter.modifyframe(silent_Frame[(padding_counter > 0) ? 0 : 1], MpaConversionMode);

									writeChannels(newframes);

									audio.parseRiffData(newframes[0], 1); 

									if (MpaConversionMode >= MpaConversion_Mode4) 
										audio.parseRiffData(newframes[1], 2);
								}

								else
								{
									writeChannel1(silent_Frame[(padding_counter > 0) ? 0 : 1]);

									audio.parseRiffData(silent_Frame[(padding_counter > 0) ? 0 : 1], 1);
								}
							}

							FrameExportInfo.countWrittenFrames(1);
							FrameExportInfo.countAddedFrames(1);

							TimePosition += audio.getFrameTimeLength();
							addf[1]++;
							TimeCounter += audio.getFrameTimeLength();

							Common.getGuiInterface().showExportStatus(Resource.getString("audio.status.add")); 

							if (Debug)
							{
								System.out.println(FrameExportInfo.getSummary() + "  @ " + formatFrameTime(TimeCounter));
								System.out.print(" t)" + (long)(TimePosition - audio.getFrameTimeLength()) + " w)" + w);
							}
						}

						w += 2;
					}

					w -= 2;
					TimePosition -= audio.getFrameTimeLength();

					if (Debug) 
						System.out.println(" eot_video:" + (vptsval[w + 1] / 90) + "ms, eot_audio:" + ((TimePosition) / 90) + "ms  ");
				}  //end add mpa

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
				Common.setMessage(Resource.getString("audio.msg.summary.add", "" + addf[1], FramesToTime((int)addf[1], audio.getFrameTimeLength())) + " " + formatFrameTime(addf[0]));

			Common.getGuiInterface().showExportStatus(Resource.getString("audio.status.finish")); 

			String tc = formatFrameTime(TimeCounter);
			Common.setMessage(Resource.getString("audio.msg.summary.frames", "" + FrameExportInfo.getWrittenFrames() + "-" + FrameExportInfo.getShortSummary(), "" + tc));

			if (ModeChangeCount_JSS > 0) 
				Common.setMessage(Resource.getString("audio.msg.summary.jstereo", String.valueOf(ModeChangeCount_JSS)));

			closeInputStream();

			silentFrameBuffer.close();

			closeOutputStreams();

			String[][] pureaudio = {
				{ ".ac3",".mp1",".mp2",".mp3",".dts" },
				{ ".new.ac3",".new.mp1",".new.mp2",".new.mp3",".new.dts" }
			};

			if (collection.getSettings().getBooleanProperty(Keys.KEY_ExternPanel_renameAudio))
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

			else if (AddWaveHeaderBWF || AddWaveHeaderACM) 
			{
				for (int g = 1; g < 4; g++)
				{
					pureaudio[0][g] += ".wav";
					pureaudio[1][g] += ".wav";
				}
			}

			if (AddWaveHeaderAC3)
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

			//finish wave header
			fillWaveHeader(audio, es_streamtype);

			File audioout1 = new File(FileName_Ch1);
			File audioout2 = new File(FileName_Ch2);

			job_processing.countMediaFilesExportLength(audioout1.length());
			job_processing.countMediaFilesExportLength(audioout2.length());

			String audio_type[] = { "(ac3)", "(mp3)", "(mp2)", "(mp1)", "(dts)", "(pcm)" };

			if (DecodeMpgAudio)
				audio_type[1] = audio_type[2] = "(pcm)";

			String comparedata = "";

			if (AudioType < 0)
				AudioType = NO_AUDIOSTREAM;

			else
				comparedata = Resource.getString("audio.msg.audio") + " " + job_processing.countAudioStream() + " " + audio_type[AudioType] + ":\t" + FrameExportInfo.getWrittenFrames() + " Frames\t" + tc + "\t" + infoPTSMatch(filename_pts, videofile_pts, ContainsVideoPTS, ContainsAudioPTS) + FrameExportInfo.getShortSummary();

			/**
			 *
			 */
			switch (AudioType)
			{ 
			case AC3_AUDIOSTREAM: 
				if (ac3name.exists())
					ac3name.delete();

				if (audioout1.length() < FileLength_Min) 
					audioout1.delete();

				else
				{ 
					Common.renameTo(audioout1, ac3name);

					Common.setMessage(Resource.getString("msg.newfile", "") + " '" + ac3name + "'"); 
					job_processing.addSummaryInfo(comparedata + "\t'" + ac3name + "'");
				}

				if (audioout2.length() < FileLength_Min) 
					audioout2.delete();

				OutputStream_Ch1.renameIddTo(ac3name);
				OutputStream_Ch2.deleteIdd();

				break;

			case MP3_AUDIOSTREAM:
				if ( mp3name.exists() ) 
					mp3name.delete();

				if (audioout1.length() < FileLength_Min) 
					audioout1.delete();

				else
				{ 
					Common.renameTo(audioout1, mp3name); 

					Common.setMessage(Resource.getString("msg.newfile", "") + " '" + mp3name + "'"); 
					job_processing.addSummaryInfo(comparedata + "\t'" + mp3name + "'");
				}

				if (audioout2.length() < FileLength_Min) 
					audioout2.delete();

				OutputStream_Ch1.renameIddTo(mp3name);
				OutputStream_Ch2.deleteIdd();

				break;

			case MP2_AUDIOSTREAM:
				if (MpaConversionMode >= MpaConversion_Mode4)
				{
					if ( mp2nameL.exists() ) 
						mp2nameL.delete();

					if ( mp2nameR.exists() ) 
						mp2nameR.delete();

					if (audioout2.length() < FileLength_Min) 
						audioout2.delete();

					else
					{ 
						Common.renameTo(audioout2, mp2nameR); 

						Common.setMessage(Resource.getString("msg.newfile", Resource.getString("audio.msg.newfile.right")) + " '" + mp2nameR + "'"); 
						job_processing.addSummaryInfo(comparedata + "\t'" + mp2nameR + "'"); 
					}

					if (audioout1.length() < FileLength_Min) 
						audioout1.delete();

					else
					{
						Common.renameTo(audioout1, mp2nameL); 

						Common.setMessage(Resource.getString("msg.newfile", Resource.getString("audio.msg.newfile.left")) + " '" + mp2nameL + "'"); 
						job_processing.addSummaryInfo(comparedata + "\t'" + mp2nameL + "'"); 
					}

					OutputStream_Ch1.renameIddTo(mp2nameL);
					OutputStream_Ch2.renameIddTo(mp2nameR);
				}

				else
				{
					if ( mp2name.exists() ) 
						mp2name.delete();

					if (audioout1.length() < FileLength_Min) 
						audioout1.delete();

					else
					{ 
						Common.renameTo(audioout1, mp2name); 

						Common.setMessage(Resource.getString("msg.newfile", "") + " '" + mp2name + "'"); 
						job_processing.addSummaryInfo(comparedata + "\t'" + mp2name + "'"); 
					}

					if (audioout2.length() < FileLength_Min) 
						audioout2.delete();

					OutputStream_Ch1.renameIddTo(mp2name);
					OutputStream_Ch2.deleteIdd();
				}

				break;

			case MP1_AUDIOSTREAM: 
				if ( mp1name.exists() ) 
					mp1name.delete();

				if (audioout1.length() < FileLength_Min) 
					audioout1.delete();

				else
				{ 
					Common.renameTo(audioout1, mp1name); 

					Common.setMessage(Resource.getString("msg.newfile", "") + " '" + mp1name + "'"); 
					job_processing.addSummaryInfo(comparedata + "\t'" + mp1name + "'"); 
				}

				if (audioout2.length() < FileLength_Min) 
					audioout2.delete();

				OutputStream_Ch1.renameIddTo(mp1name);
				OutputStream_Ch2.deleteIdd();

				break;

			case DTS_AUDIOSTREAM: 
				if (dtsname.exists())
					dtsname.delete();

				if (audioout1.length() < FileLength_Min) 
					audioout1.delete();

				else
				{ 
					Common.renameTo(audioout1, dtsname); 

					Common.setMessage(Resource.getString("msg.newfile", "") + " '" + dtsname + "'"); 
					job_processing.addSummaryInfo(comparedata + "\t'" + dtsname + "'");
				}

				if (audioout2.length() < FileLength_Min) 
					audioout2.delete();

				OutputStream_Ch1.renameIddTo(dtsname);
				OutputStream_Ch2.deleteIdd();

				break;

			case WAV_AUDIOSTREAM: 
				if (wavname.exists())
					wavname.delete();

				if (audioout1.length() < FileLength_Min) 
					audioout1.delete();

				else
				{ 
					Common.renameTo(audioout1, wavname); 

					Common.setMessage(Resource.getString("msg.newfile", "") + " '" + wavname + "'"); 
					job_processing.addSummaryInfo(comparedata + "\t'" + wavname + "'");
				}

				if (audioout2.length() < FileLength_Min) 
					audioout2.delete();

				OutputStream_Ch1.renameIddTo(wavname);
				OutputStream_Ch2.deleteIdd();

				break;

			case NO_AUDIOSTREAM: 
				Common.setMessage(Resource.getString("audio.msg.noaudio")); 

				audioout1.delete();
				audioout2.delete();
				OutputStream_Ch1.deleteIdd();
				OutputStream_Ch2.deleteIdd();

				break;
			}

		} catch (IOException e) {

			Common.setExceptionMessage(e);
		}

		Common.updateProgressBar(FileLength, FileLength);

		return false;
	}


	/**
	 * message source format change
	 */
	private void messageSourceFormat(JobProcessing job_processing, AudioFormat audio, boolean ContainsVideoPTS, double TimeCounter)
	{
		if (!HasNewFormat || (ContainsVideoPTS && !WriteEnabled))
			return;

		String header = audio.displayHeader();

		if (ModeChangeCount < ModeChangeCount_Max)
		{
			String str = formatFrameTime(TimeCounter);

			Common.setMessage(Resource.getString("audio.msg.source", header) + " " + str);

			if (CreateChapters)
				job_processing.getChapters().addChapter(str, header);
		}

		else if (Debug) 
			System.out.println("=> src_audio: " + header + " @ " + formatFrameTime(TimeCounter));

		if (ModeChangeCount == ModeChangeCount_Max) 
			Common.setMessage(Resource.getString("audio.msg.source.max"));

		ModeChangeCount++;
		HasNewFormat = false;
	}

	/**
	 * pts value to time value
	 */
	private String formatFrameTime(double time_value)
	{
		return Common.formatTime_1((long) (time_value / 90.0));
	}

	/**
	 * pts value to time value
	 */
	private String formatFrameTime(long time_value)
	{
		return Common.formatTime_1(time_value / 90L);
	}

	/**
	 * stop if no more audio needed 
	 */
	private boolean checkLastAudioBound(boolean ContainsVideoPTS, long time_value, long[] vptsval, long FileLength)
	{
		boolean b = true;

		if (!ContainsVideoPTS)
			return b;

		if (time_value > vptsval[vptsval.length - 1] + 10000)
		{
			Common.updateProgressBar(FileLength, FileLength);

			return !b;
		}

		return b;
	}

	/**
	 *
	 */
	private boolean writeSuccessiveFrame(AudioFormat audio, byte[] frame, byte[][] newframes, boolean ContainsVideoPTS, long FramePosition, long[] ptspos, int x, int es_streamtype)
	{
		boolean b = true;

		// frame is in last pes packet or packet end not yet reached 
		if (ptspos[x + 1] != -1 && ptspos[x + 1] <= FramePosition)
			return !b;

		//always true
		writeFrame(audio, frame, newframes, ContainsVideoPTS, es_streamtype);

		return b;
	}

	/**
	 *
	 */
	private boolean writeFrame(AudioFormat audio, byte[] frame, byte[][] newframes, boolean ContainsVideoPTS, int es_streamtype)
	{
		if (ContainsVideoPTS && !WriteEnabled) 
			return false;

		switch (es_streamtype)
		{
		case CommonParsing.DTS_AUDIO:
			writeChannel1(frame);
			break;

		case CommonParsing.AC3_AUDIO:
			if (AddWaveHeaderAC3) 
				audio.parseRiffData(frame, 1); 

			writeChannel1(frame);
			break;

		case CommonParsing.MPEG_AUDIO:
			if (audio.getLayer() > 0 && DecodeMpgAudio) 
			{
				writeChannel1(MpaDecoder.decodeArray(frame));

				if (MpaConversionMode >= MpaConversion_Mode4) 
					writeChannel2(MpaDecoder.get2ndArray());
			}

			else if (MpaConversionMode != MpaConversion_None)
			{
				newframes = MPAConverter.modifyframe(frame, MpaConversionMode);
				writeChannel1(newframes[0]);

				if (MpaConversionMode >= MpaConversion_Mode4) 
					writeChannel2(newframes[1]);

				audio.parseRiffData(newframes[0], 1); 

				if (MpaConversionMode >= MpaConversion_Mode4) 
					audio.parseRiffData(newframes[1], 2);
			}

			else
			{
				writeChannel1(frame);

				audio.parseRiffData(frame, 1); 
			}

			break;
		}

		FrameExportInfo.countWrittenFrames(1);
		TimeCounter += audio.getFrameTimeLength();

	//	if (Debug) 
	//		System.out.println(FrameExportInfo.getSummary() + "  @ " + formatFrameTime(TimeCounter) + "  ");

		return true;
	}

	/**
	 *
	 */
	private void initOutputStreams()
	{
		try {
			OutputStream_Ch1 = new IDDBufferedOutputStream(new FileOutputStream(FileName_Ch1), 2048000);
			OutputStream_Ch2 = new IDDBufferedOutputStream(new FileOutputStream(FileName_Ch2), MpaConversionMode >= 4 ? 2048000 : 65536);

		} catch (Exception e) {
			Common.setExceptionMessage(e);
		}
	}

	/**
	 *
	 */
	private void closeOutputStreams()
	{
		try {
			OutputStream_Ch1.flush(); 
			OutputStream_Ch1.close();

			OutputStream_Ch2.flush(); 
			OutputStream_Ch2.close();

		} catch (Exception e) {
			Common.setExceptionMessage(e);
		}
	}

	/**
	 *
	 */
	private void writeChannel1(byte[] array)
	{
		writeChannel(array, 1);
	}

	/**
	 *
	 */
	private void writeChannel2(byte[] array)
	{
		writeChannel(array, 2);
	}

	/**
	 *
	 */
	private void writeChannels(byte[] array)
	{
		writeChannels(array, array);
	}

	/**
	 *
	 */
	private void writeChannels(byte[][] arrays)
	{
		writeChannels(arrays[0], arrays[1]);
	}

	/**
	 *
	 */
	private void writeChannels(byte[] array1, byte[] array2)
	{
		writeChannel(array1, 1);

		if (MpaConversionMode >= MpaConversion_Mode4) 
			writeChannel(array2, 2);
	}

	/**
	 *
	 */
	private void writeChannel(byte[] array, int index)
	{
		try {
			if (index == 1)
				OutputStream_Ch1.write(array);

			else if (index == 2)
				OutputStream_Ch2.write(array);

		} catch (Exception e) {
			Common.setExceptionMessage(e);
		}
	}

	/**
	 *
	 */
	private void initInputStream(XInputFile xInputFile)
	{
		try {
			InputStream = new PushbackInputStream(xInputFile.getInputStream(), 1000000);

		} catch (Exception e) {
			Common.setExceptionMessage(e);
		}
	}

	/**
	 *
	 */
	private void closeInputStream()
	{
		try {
			InputStream.close();

		} catch (Exception e) {
			Common.setExceptionMessage(e);
		}
	}

	/**
	 *
	 */
	private int readInputStream(byte[] array)
	{
		return readInputStream(array, 0, array.length);
	}

	/**
	 *
	 */
	private int readInputStream(byte[] array, int offset, int length)
	{
		int value = 0;

		try {
			value = InputStream.read(array, offset, length);

		} catch (Exception e) {
			Common.setExceptionMessage(e);
		}

		return value;
	}

	/**
	 *
	 */
	private int unreadInputStream(byte[] array, int offset, int length)
	{
		int value = 0;

		try {
			InputStream.unread(array, offset, length);
			value = length;

		} catch (Exception e) {
			Common.setExceptionMessage(e);
		}

		return value;
	}

	/**
	 *
	 */
	private long skipInputStream(long length)
	{
		long value = 0L;

		try {
			value = InputStream.skip(length);

		} catch (Exception e) {
			Common.setExceptionMessage(e);
		}

		return value;
	}

	/**
	 *
	 */
	private boolean isCancelled(JobProcessing job_processing)
	{
		if (!CommonParsing.isProcessCancelled())
			return false;

		CommonParsing.setProcessCancelled(false);
		job_processing.setSplitSize(0); 

		return true;
	}

	/**
	 * ac3 3.2 replacement
	 */
	private byte[] getReplacementFrame(AudioFormat audio, byte[] array, int es_streamtype)
	{
		if (es_streamtype != CommonParsing.AC3_AUDIO || !ReplaceAc3withSilence)
			return array;

		//is 3/2
		if (audio.getMode() == 7)
			return array;

		AudioFormat ac3_audio = new AudioFormat(CommonParsing.AC3_AUDIO);
		byte[] ac3data;

		for (int i = 0, j = Common.getAC3list().size(); i < j; i++)
		{
			ac3data = (byte[]) Common.getAC3list().get(i);

			ac3_audio.parseHeader(ac3data, 0);

			if (ac3_audio.getMode() != 7 || ac3_audio.getSamplingFrequency() != audio.getSamplingFrequency())
				continue;

			//if (ac3_audio.getBitrate() != audio.getBitrate())
			//	continue;

			array = new byte[ac3data.length];

			System.arraycopy(ac3data, 0, array, 0, array.length);

			break;
		}

		return array;
	}

	/**
	 * check for change in frametype 
	 */
	private boolean determineFormatChange(AudioFormat audio, int es_streamtype)
	{
		boolean accept = true;
		int returncode = audio.compareHeader();

		if (returncode > 0)
		{
			if (!AllowFormatChanges && (returncode & 0x7) != 0 && FrameExportInfo.getWrittenFrames() > 0)
				return !accept;

			HasNewFormat = true;

			if (es_streamtype == CommonParsing.MPEG_AUDIO && returncode == 0x20)
			{
				ModeChangeCount_JSS++;
				HasNewFormat = false;
			}
		}

		if (FrameExportInfo.getWrittenFrames() == 0) 
			HasNewFormat = true;

		return accept;
	}

	/**
	 * prepare wave header
	 */
	private void addWaveHeader(AudioFormat audio, int es_streamtype)
	{
		if (es_streamtype == CommonParsing.MPEG_AUDIO)
		{
			writeChannel1(audio.getExtraWaveHeader(1, true));

			if (MpaConversionMode >= MpaConversion_Mode4) 
				writeChannel2(audio.getExtraWaveHeader(2, true));

		//	Common.setMessage(Resource.getString("audio.msg.addriff.acm"));
		//	Common.setMessage(Resource.getString("audio.msg.addriff.bwf"));
		} 

		else if (AddWaveHeaderAC3 && es_streamtype == CommonParsing.AC3_AUDIO)
		{
			writeChannel1(audio.getExtraWaveHeader(1, true));
			Common.setMessage(Resource.getString("audio.msg.addriff.ac3"));
		}

		else if (CreateDDWave && es_streamtype == CommonParsing.AC3_AUDIO)
			writeChannel1(audio.getRiffHeader());

		else if (CreateDDWave && es_streamtype == CommonParsing.DTS_AUDIO)
			writeChannel1(audio.getRiffHeader());
	}

	/**
	 * finish wave header
	 */
	private void fillWaveHeader(AudioFormat audio, int es_streamtype)
	{
		try {
			if (DecodeMpgAudio && es_streamtype == CommonParsing.MPEG_AUDIO && MpaDecoder.WAVE)
			{
				if (audio.getLayer() > 1)
				{
					MpaDecoder.fillRIFF(FileName_Ch1, FadeInOut, FadeInOutMillis);

					if (MpaConversionMode >= MpaConversion_Mode4) 
						MpaDecoder.fillRIFF(FileName_Ch2, FadeInOut, FadeInOutMillis);
				}

				else
				{
					MpaDecoder.deleteRIFF(FileName_Ch1);

					if (MpaConversionMode >= MpaConversion_Mode4) 
						MpaDecoder.deleteRIFF(FileName_Ch2);
				}
			}

			else if (DecodeMpgAudio && es_streamtype == CommonParsing.MPEG_AUDIO && AddAiffHeader)
			{
				if (audio.getLayer() > 1)
				{
					MpaDecoder.fillAiff(FileName_Ch1,(long)(TimeCounter / 90.0f), FadeInOut, FadeInOutMillis);

					if (MpaConversionMode >= MpaConversion_Mode4) 
						MpaDecoder.fillAiff(FileName_Ch2,(long)(TimeCounter / 90.0f), FadeInOut, FadeInOutMillis);
				}

				else
				{
					MpaDecoder.deleteAiff(FileName_Ch1);

					if (MpaConversionMode >= MpaConversion_Mode4) 
						MpaDecoder.deleteAiff(FileName_Ch2);
				}
			}

			else if ((AddWaveHeaderBWF || AddWaveHeaderACM) && es_streamtype == CommonParsing.MPEG_AUDIO)
			{
				RandomAccessFile[] rifffile = { 
					new RandomAccessFile(FileName_Ch1, "rw"), 
					new RandomAccessFile(FileName_Ch2, "rw") 
				};

				audio.setExtraWaveLength(rifffile[0].length(), (long)(TimeCounter / 90.0f), 1);
				audio.setExtraWaveLength(rifffile[1].length(), (long)(TimeCounter / 90.0f), 2);

				rifffile[0].seek(0); 
				rifffile[1].seek(0);

				rifffile[0].write(audio.getExtraWaveHeader(1, false));

				if (MpaConversionMode >= MpaConversion_Mode4) 
					rifffile[1].write(audio.getExtraWaveHeader(2, false));

				rifffile[0].close();
				rifffile[1].close();
			}

			else if (AddWaveHeaderAC3 && es_streamtype == CommonParsing.AC3_AUDIO) 
			{
				RandomAccessFile rifffile = new RandomAccessFile(FileName_Ch1, "rw");

				audio.setExtraWaveLength(rifffile.length(), (long)(TimeCounter / 90.0f), 1);

				rifffile.seek(0);

				rifffile.write(audio.getExtraWaveHeader(1, false));

				rifffile.close();
			}

			else if (es_streamtype == CommonParsing.LPCM_AUDIO)
				audio.fillRiffHeader(FileName_Ch1);

			else if (CreateDDWave && es_streamtype == CommonParsing.AC3_AUDIO)
				audio.fillStdRiffHeader(FileName_Ch1, (long)(TimeCounter / 90.0f));

			else if (CreateDDWave && es_streamtype == CommonParsing.DTS_AUDIO)
				audio.fillStdRiffHeader(FileName_Ch1, (long)(TimeCounter / 90.0f));

		} catch (Exception e) {
			Common.setExceptionMessage(e);
		}
	}

	/**
	 * init project files
	 */
	private void initProjectFiles()
	{
		if (CreateM2sIndex)
		{
			OutputStream_Ch1.InitIdd(FileName_Ch1, 2);
			OutputStream_Ch2.InitIdd(FileName_Ch2, 2);
		}
	}

	/**
	 * determine given stream type
	 */
	private int determineStreamType(String str)
	{
		int value = CommonParsing.MPEG_AUDIO; //"mp" std

		if (str.equals("ac")) //means dts, too
			value = CommonParsing.AC3_AUDIO;

		else if (str.equals("dt")) //later, other handling
			value = CommonParsing.DTS_AUDIO;

		else if (str.equals("wa"))
			value = CommonParsing.LPCM_AUDIO;

		return value;
	}

	/**
	 * messages
	 */
	private void messageSettings()
	{
		if (IgnoreErrors)
			Common.setMessage("-> " + Resource.getString(Keys.KEY_Audio_ignoreErrors[0]));

		if (AllowSpaces)
			Common.setMessage("-> " + Resource.getString(Keys.KEY_AudioPanel_allowSpaces[0]));

		if (LimitPts)
			Common.setMessage("-> " + Resource.getString(Keys.KEY_Audio_limitPts[0]));

		if (AllowFormatChanges)
			Common.setMessage("-> " + Resource.getString(Keys.KEY_Audio_allowFormatChanges[0]));

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
	}

	/**
	 * settings
	 */
	private void getSettings(JobCollection collection)
	{
		Debug = collection.getSettings().getBooleanProperty(Keys.KEY_DebugLog);
		CreateChapters = collection.getSettings().getBooleanProperty(Keys.KEY_ExternPanel_createChapters);
		AddWaveHeaderACM = collection.getSettings().getBooleanProperty(Keys.KEY_AudioPanel_addRiffToMpgAudioL3);
		AddWaveHeaderBWF = collection.getSettings().getBooleanProperty(Keys.KEY_AudioPanel_addRiffToMpgAudio);
		AddWaveHeaderAC3 = collection.getSettings().getBooleanProperty(Keys.KEY_AudioPanel_addRiffToAc3);
		ReplaceAc3withSilence = collection.getSettings().getBooleanProperty(Keys.KEY_AudioPanel_replaceAc3withSilence);
		CreateM2sIndex = collection.getSettings().getBooleanProperty(Keys.KEY_ExternPanel_createM2sIndex);
		Message_2 = collection.getSettings().getBooleanProperty(Keys.KEY_MessagePanel_Msg2);
		Message_7 = collection.getSettings().getBooleanProperty(Keys.KEY_MessagePanel_Msg7);
		PitchAudio = collection.getSettings().getBooleanProperty(Keys.KEY_AudioPanel_pitchAudio);
		AllowSpaces = collection.getSettings().getBooleanProperty(Keys.KEY_AudioPanel_allowSpaces);
		ValidateCRC = collection.getSettings().getBooleanProperty(Keys.KEY_AudioPanel_validateCRC);
		Patch1stAc3Header = collection.getSettings().getBooleanProperty(Keys.KEY_AudioPanel_patch1stAc3Header);
		FillGapsWithLastFrame = collection.getSettings().getBooleanProperty(Keys.KEY_AudioPanel_fillGapsWithLastFrame);
		LimitPts = collection.getSettings().getBooleanProperty(Keys.KEY_Audio_limitPts);
		AllowFormatChanges = collection.getSettings().getBooleanProperty(Keys.KEY_Audio_allowFormatChanges);
		AddFrames = collection.getSettings().getBooleanProperty(Keys.KEY_AudioPanel_addFrames);
		DownMix = collection.getSettings().getBooleanProperty(Keys.KEY_AudioPanel_Downmix);
		ChangeByteorder = collection.getSettings().getBooleanProperty(Keys.KEY_AudioPanel_changeByteorder);
		AddRiffHeader = collection.getSettings().getBooleanProperty(Keys.KEY_AudioPanel_addRiffHeader);
		AddAiffHeader = collection.getSettings().getBooleanProperty(Keys.KEY_AudioPanel_addAiffHeader);
		ClearCRC = collection.getSettings().getBooleanProperty(Keys.KEY_AudioPanel_clearCRC);
		IgnoreErrors = collection.getSettings().getBooleanProperty(Keys.KEY_Audio_ignoreErrors);
		CreateDDWave = collection.getSettings().getBooleanProperty(Keys.KEY_AudioPanel_createDDWave);
		FadeInOut = collection.getSettings().getBooleanProperty(Keys.KEY_AudioPanel_fadeInOut);
		Normalize = collection.getSettings().getBooleanProperty(Keys.KEY_AudioPanel_Normalize);

		FadeInOutMillis = collection.getSettings().getIntProperty(Keys.KEY_AudioPanel_fadeInOutMillis);
		ResampleAudioMode = collection.getSettings().getIntProperty(Keys.KEY_AudioPanel_resampleAudioMode);
		PitchValue = collection.getSettings().getIntProperty(Keys.KEY_AudioPanel_PitchValue);
	}

	/**
	 * init FFT/window for mpa decoding for 1 file
	 */
	private void initDecoder(AudioFormat audio, int es_streamtype)
	{
		if (es_streamtype != CommonParsing.MPEG_AUDIO)
			return;

		if (!DecodeMpgAudio)
			return;

		MpaDecoder.init_work(ResampleAudioMode);
		MpaDecoder.DOWNMIX = DownMix;
		MpaDecoder.MONO = (DownMix || MpaConversionMode == MpaConversion_Mode4);
		MpaDecoder.MOTOROLA = ChangeByteorder;
		MpaDecoder.WAVE = AddRiffHeader;
	
		if (AddRiffHeader)
			writeChannels(MpaDecoder.RIFF);

		if (AddAiffHeader)
			writeChannels(MpaDecoder.AIFF);
	}

	/**
	 * 
	 */
	private int processData(AudioFormat audio, int es_streamtype)
	{
		int returncode = 0;

		// add preceding wave header
		addWaveHeader(audio, es_streamtype);

		// init FFT/window for mpa decoding for 1 file
		initDecoder(audio, es_streamtype);

		// init decoding of ancillary data
		audio.setAncillaryDataDecoder(Message_7, Debug);

		return returncode;
	}

	/**
	 *  PCM Audio
	 */
	private void processPCMData(JobProcessing job_processing, AudioFormat audio, long[] vptsval, long[] ptsval)
	{
		// parse header
		byte[] array = new byte[1000];

		readInputStream(array);

		audio.parseHeader(array, 0);

		unreadInputStream(array, audio.getEmphasis(), 1000 - audio.getEmphasis());

		Common.setMessage(Resource.getString("audio.msg.source", audio.saveAndDisplayHeader()) + " " + formatFrameTime(TimeCounter));

		AudioType = WAV_AUDIOSTREAM;

		FramePosition = audio.getEmphasis(); //start of pcm data

		long pcm_end_pos = audio.getEmphasis() + audio.getSizeBase(); //whole sample data size

		TimePosition = ptsval[0];

		writeChannel1(audio.getRiffHeader());

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

		for (int j = 0; j < ptsval.length - 1; j++)
		{
			for (int i = 0, k = vptsval.length; i < k; i += 2)
			{
				while (pause())
				{}

				if (isCancelled(job_processing))
					return;

				//jump back (not yet) or insert silent samples
				if (ContainsVideoPTS && vptsval[i] < TimePosition)
				{
					sample_pts = vptsval[i + 1] > TimePosition ? TimePosition - vptsval[i] : vptsval[i + 1] - vptsval[i];
					sample_bytes = (long) Math.round(1.0 * audio.getSamplingFrequency() * sample_pts / 90000.0) * audio.getMode();

					if (Debug)
						System.out.println("i " + sample_pts + "/" + sample_bytes + "/" + FramePosition + "/" + TimePosition);

					for (long sample_pos = 0; sample_pos < sample_bytes; )
					{
						sample_size = (sample_bytes - sample_pos) >= read_size ? read_size : (int)(sample_bytes - sample_pos);

						if (sample_size != array.length)
							array = new byte[sample_size];

						sample_pos += sample_size;

						writeChannel1(array);
					}

					TimeCounter += sample_pts;
					FrameExportInfo.countWrittenFrames(sample_bytes / audio.getMode());

					Common.setFps(FrameExportInfo.getWrittenFrames());

					if (vptsval[i + 1] > TimePosition)
					{
						sample_pts = vptsval[i + 1] - TimePosition;
						sample_bytes = (long) Math.round(1.0 * audio.getSamplingFrequency() * sample_pts / 90000.0) * audio.getMode();

						if (Debug)
							System.out.println("b " + sample_pts + "/" + sample_bytes + "/" + FramePosition + "/" + TimePosition);

						for (long sample_pos = 0; sample_pos < sample_bytes; )
						{
							sample_size = (sample_bytes - sample_pos) >= read_size ? read_size : (int)(sample_bytes - sample_pos);

							if (sample_size != array.length)
								array = new byte[sample_size];

							readInputStream(array);

							sample_pos += sample_size;

							writeChannel1(array);
						}

						FramePosition += sample_bytes;
						TimePosition += sample_pts;
						TimeCounter += sample_pts;
						FrameExportInfo.countWrittenFrames(sample_bytes / audio.getMode());
					}
				}

				else
				{
					skip_pts = ContainsVideoPTS ? vptsval[i] - TimePosition : 0;
					skip_bytes = (long) Math.round(1.0 * audio.getSamplingFrequency() * skip_pts / 90000.0) * audio.getMode();

					sample_pts = ContainsVideoPTS ? vptsval[i + 1] - vptsval[i] : (long)(1.0 * (audio.getSizeBase() / audio.getMode()) / audio.getSamplingFrequency() * 90000.0);
					sample_bytes = (long) Math.round(1.0 * audio.getSamplingFrequency() * sample_pts / 90000.0) * audio.getMode();

					for (long skip_pos = 0; skip_pos < skip_bytes; )
						skip_pos += skipInputStream(skip_bytes - skip_pos);

					FramePosition += skip_bytes;

					if (Debug)
						System.out.println("c " + skip_pts + "/" + skip_bytes + "/" + sample_pts + "/" + sample_bytes + "/" + FramePosition + "/" + TimePosition);

					for (long sample_pos = 0; sample_pos < sample_bytes; )
					{
						sample_size = (sample_bytes - sample_pos) >= read_size ? read_size : (int)(sample_bytes - sample_pos);

						if (sample_size != array.length)
							array = new byte[sample_size];

						readInputStream(array);

						sample_pos += sample_size;

						writeChannel1(array);
					}

					TimePosition += (skip_pts + sample_pts);
					TimeCounter += sample_pts;

					FramePosition += sample_bytes;
					FrameExportInfo.countWrittenFrames(sample_bytes / audio.getMode());
				}

				if (Debug)
					System.out.println(FrameExportInfo.getSummary() + " @ " + formatFrameTime(TimeCounter));

				Common.updateProgressBar(FramePosition, FileLength);

				if (Debug) 
					System.out.println("FramePosition " + FramePosition);
			}

			break;
		}
	}


	/**
	 * wri + pre + skip + ins + add
	 */
	class FrameExportInfo {

		private int writtenFrames;
		private int preInsertedFrames;
		private int skippedFrames;
		private int insertedFrames;
		private int addedFrames;

		private String head = "Audio Frames: wri-pre-skip-ins-add: ";
		private String delim = "-";

		public FrameExportInfo()
		{
			reset();
		}

		public void reset()
		{
			writtenFrames = 0;
			preInsertedFrames = 0;
			skippedFrames = 0;
			insertedFrames = 0;
			addedFrames = 0;
		}

		public int getWrittenFrames()
		{
			return writtenFrames;
		}

		public String getSummary()
		{
			String str = head;

			str += String.valueOf(writtenFrames) + delim;
			str += getShortSummary();

			return str;
		}

		public String getShortSummary()
		{
			String str = "";

			str += String.valueOf(preInsertedFrames) + delim;
			str += String.valueOf(skippedFrames) + delim;
			str += String.valueOf(insertedFrames) + delim;
			str += String.valueOf(addedFrames);

			return str;
		}

		public void countWrittenFrames(long value)
		{
			writtenFrames += ((int) value);
		}

		public void countWrittenFrames(int value)
		{
			writtenFrames += value;
		}

		public void countPreInsertedFrames(int value)
		{
			preInsertedFrames += value;
		}

		public void countSkippedFrames(int value)
		{
			skippedFrames += value;
		}

		public void countInsertedFrames(int value)
		{
			insertedFrames += value;
		}

		public void countAddedFrames(int value)
		{
			addedFrames += value;
		}
	}
}
