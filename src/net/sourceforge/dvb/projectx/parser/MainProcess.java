/*
 * @(#)MainProcess
 *
 * Copyright (c) 2001-2005 by dvb.matt, All rights reserved.
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

/*
 * X is completely designed as a test, therefore it mostly implements its 
 * own code instead of a derivation of an ISO reference source or 
 * any other code. Considerable effort has been expended to ensure 
 * an useful implementation, even in cases where the standards 
 * are ambiguous or misleading.
 * Do not expect any useful output, even if that may be possible.
 *
 * For a program compliant to the international standards ISO 11172
 * and ISO 13818 it is inevitable to use methods covered by patents
 * in various countries. The authors of this program disclaim any
 * liability for patent infringement caused by using, modifying or
 * redistributing this program.
 *
 */

package net.sourceforge.dvb.projectx.parser;

import java.awt.Toolkit;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.PushbackInputStream;
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.io.InputStreamReader;

import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Calendar;
import java.util.TimeZone;

import java.net.URL;

import net.sourceforge.dvb.projectx.audio.AudioFormat;
import net.sourceforge.dvb.projectx.audio.MpaConverter;
import net.sourceforge.dvb.projectx.audio.MpaDecoder;
import net.sourceforge.dvb.projectx.audio.RIFFHeader;

import net.sourceforge.dvb.projectx.subtitle.BMP;
import net.sourceforge.dvb.projectx.subtitle.Bitmap;
import net.sourceforge.dvb.projectx.subtitle.Subpicture;
import net.sourceforge.dvb.projectx.subtitle.Teletext;
import net.sourceforge.dvb.projectx.subtitle.UnicodeWriter;

import net.sourceforge.dvb.projectx.thirdparty.Chapters;
import net.sourceforge.dvb.projectx.thirdparty.D2V;
import net.sourceforge.dvb.projectx.thirdparty.Ifo;
import net.sourceforge.dvb.projectx.thirdparty.TS;

import net.sourceforge.dvb.projectx.video.Video;

import net.sourceforge.dvb.projectx.parser.VBI;
import net.sourceforge.dvb.projectx.parser.StreamBuffer;
import net.sourceforge.dvb.projectx.parser.CommonParsing;
import net.sourceforge.dvb.projectx.parser.Scan;
import net.sourceforge.dvb.projectx.parser.StreamConverter;
import net.sourceforge.dvb.projectx.parser.StreamDemultiplexer;

import net.sourceforge.dvb.projectx.parser.StreamParserBase;
import net.sourceforge.dvb.projectx.parser.StreamParser;


import net.sourceforge.dvb.projectx.xinput.XInputFile;


import net.sourceforge.dvb.projectx.io.IDDBufferedOutputStream;
import net.sourceforge.dvb.projectx.io.StandardBuffer;
import net.sourceforge.dvb.projectx.io.RawFile;

import net.sourceforge.dvb.projectx.common.JobCollection;
import net.sourceforge.dvb.projectx.common.JobProcessing;
import net.sourceforge.dvb.projectx.common.Resource;
import net.sourceforge.dvb.projectx.common.Keys;
import net.sourceforge.dvb.projectx.common.Common;

import net.sourceforge.dvb.projectx.xinput.StreamInfo;


/**
 * main thread
 */
public class MainProcess extends Thread {

	private MpaConverter MPAConverter;
	private MpaDecoder MPADecoder;

	private int ERRORCODE = 0;
	private int MainBufferSize = 8192000;


	private TS tf = new TS();

	/**
	 * run
	 */ 
	public void run()
	{
		Common.setRunningProcess(true);

		startProcessing();

		Common.setRunningProcess(false);
	}


	/**
	 * process 
	 */ 
	private void startProcessing()
	{
		boolean stop_on_error = false;

		Common.setGlobalDebug(Common.getSettings().getBooleanProperty(Keys.KEY_DebugLog));

		JobCollection collection = null;
		JobProcessing job_processing = null;

		try {
			Common.setProcessTime(System.currentTimeMillis());

			Common.updateProgressBar(Resource.getString("run.prepare.colls"), 0, 0);
			Common.getGuiInterface().showAVOffset(Resource.getString("run.av.offset"));
			Common.getGuiInterface().updateTtxHeader("");
			Common.getGuiInterface().updateVpsLabel("");
			Common.setMessage(null, false);

			if (CommonParsing.isInfoScan())
			{
				Common.setMessage(Resource.getString("run.start.quick.info"));
				Common.setMessage("");
			}

			//yield();

			int a = 0;
			int b = 0;
			int d = 0;

			/**
			 * normal processing
			 */
			if (CommonParsing.getPvaPidToExtract() == -1)
			{
				Common.setMessage(DateFormat.getDateInstance(DateFormat.FULL).format(new Date()) + "  " + DateFormat.getTimeInstance(DateFormat.FULL).format(new Date()));
				Common.setMessage(Common.getVersionName() + " (" + Common.getVersionDate() + ")");

				if (Common.getSettings().getBooleanProperty(Keys.KEY_useAllCollections)) 
					b = Common.getCollectionListSize();

				else
				{
					a = Common.getActiveCollection();
					b = a + 1;
				}

				Common.setMessage("");
				Common.setMessage(Resource.getString("run.session.infos"));

				String str;

				/**
				 * the Collection main loop
				 */
				for ( ; a < b ; a++, d++)
				{
					Common.clearMessageLog();

					// clean up before each collection run
					System.gc();

					Common.setProcessedCollection(a);

					collection = Common.getCollection(a);
					collection.startProcessing(Common.isRunningCLI());

					job_processing = collection.getJobProcessing();

				//	Common.getGuiInterface().showActiveCollection(a);  //brings mainframe to front and load preview

					Common.setMessage("");
					Common.setMessage(Resource.getString("run.working.coll") + " " + a);

					/**
					 * do nothing, if collection is empty
					 */
					if (collection.getInputFilesCount() == 0)
					{
						Common.getGuiInterface().showAVOffset(Resource.getString("run.av.offset"));

						Common.updateProgressBar("", 0, 0);

						Common.setMessage(Resource.getString("run.no.input"));

						continue;
					}

					Common.getGuiInterface().updateTtxHeader("");
					Common.getGuiInterface().updateVpsLabel("");

					CommonParsing.setCutCounter(0);  // move to collection
					CommonParsing.setCutStatus(false);  // move to collection

					tf.setfirstID();

					messageSettings();

					if ( (str = collection.checkOutputDirectory()) != null)
					{
						Common.setMessage(Resource.getString("run.write.output.notexists") + ":");
						Common.setMessage("'" + str + "'");
						continue;
					}


					if (Common.getSettings().getBooleanProperty(Keys.KEY_ExportPanel_createSubDirNumber))
					{
						str = "(" + a + ")";

						collection.setOutputDirectory( collection.getOutputDirectory() + collection.getFileSeparator() + str);

						new File(collection.getOutputDirectory()).mkdirs();
					}


					/**
					 * out directory named by first file of coll.
					 */
					if (Common.getSettings().getBooleanProperty(Keys.KEY_ExportPanel_createSubDirName))
					{
						File f = new File(collection.getFirstFileBase());

						str = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(new Date(collection.getFirstFileDate())) + "_" + collection.getFirstFileName();

						collection.setOutputDirectory( collection.getOutputDirectory() + collection.getFileSeparator() + str);

						new File(collection.getOutputDirectory()).mkdirs();
					}

					/**
					 * create index.vdr + new path
					 */
					if (Common.getSettings().getBooleanProperty(Keys.KEY_ExternPanel_createVdrIndex) && Common.getSettings().getIntProperty(Keys.KEY_ConversionMode) == 1)
					{
						str = "_" + new File(collection.getFirstFileBase()).getName() + System.getProperty("file.separator")
							+ new SimpleDateFormat("yyyy-MM-dd.HH.mm.ss.SSS").format(new Date()) + ".rec";

						collection.setOutputDirectory( collection.getOutputDirectory() + collection.getFileSeparator() + str);

						new File(collection.getOutputDirectory()).mkdirs();
					}

					Common.setMessage(Resource.getString("run.write.output.to") + " '" + collection.getOutputDirectory() + "'");


					int val = collection.getCutpointCount();

					if (val > 0)
						Common.setMessage("-> " + val + " " + Resource.getString("run.cutpoints.defined") + " ( " + Keys.ITEMS_CutMode[Common.getSettings().getIntProperty(Keys.KEY_CutMode)] + " )");

					collection.setLogFiles();

					/**
					 * gets collection priority of action type
					 */
					int action = collection.getActionType();

					if (action < 0)
						action = Common.getSettings().getIntProperty(Keys.KEY_ConversionMode);

					/** 
					 * quick pre-run for TS autoPMT 
					 * depends also on collection priority of action type
					 */ 
					if (!CommonParsing.isInfoScan() && action == CommonParsing.ACTION_TO_TS && Common.getSettings().getBooleanProperty(Keys.KEY_TS_generatePmt))
					{
						Common.setMessage("");
						Common.setMessage(Resource.getString("run.start.quick.info"));

						CommonParsing.setInfoScan(true);

						/**
						 * no split on infoscan
						 */
						long splitlen = job_processing.getSplitSize(); 
						job_processing.setSplitSize(0);

						/**
						 * call the process
						 */
						processCollection(collection);

						job_processing.setSourceVideoFrameNumber(0);
						job_processing.setFileNumber(0);

						CommonParsing.setCutCounter(0); // move to collection
						CommonParsing.setCutStatus(false); // move to collection
						CommonParsing.setInfoScan(false);

						job_processing.setSplitSize(splitlen);

						Common.setMessage("");
						Common.setMessage(Resource.getString("run.end.quick.info"));
					}


					/**
					 * M2S chapters per coll#
					 */
					job_processing.getChapters().init(Common.getSettings().getBooleanProperty(Keys.KEY_ExternPanel_createChapters));

					/**
					 * call the process
					 */
					processCollection(collection);

					job_processing.clearStatusStrings();

					/**
					 * M2S finish chapters file per coll#
					 */
					job_processing.getChapters().finish(collection.getOutputDirectory(), collection.getFirstFileName());

					/**
					 * finish collection
					 */
					collection.finishProcessing();
				}
			}

			/**
			 * extract raw pes data processing
			 */
			else
			{
				Common.setProcessedCollection(Common.getActiveCollection());

				Common.setMessage(DateFormat.getDateInstance(DateFormat.FULL).format(new Date()) + "  " + DateFormat.getTimeInstance(DateFormat.FULL).format(new Date()));
				Common.setMessage(Common.getVersionName() + " (" + Common.getVersionDate() + ")");
				Common.setMessage("");
				Common.setMessage(Resource.getString("run.session.infos"));
				Common.setMessage("");
				Common.setMessage(Resource.getString("run.working.coll") + " " + Common.getProcessedCollection());

				String str;

				/**
				 * loop is not used ATM
				 */
				for (;;)
				{
					collection = Common.getCollection(Common.getProcessedCollection());

					if (collection.getInputFilesCount() == 0)
					{
						Common.setMessage(Resource.getString("run.coll.empty"));
						break;
					}

					if ( (str = collection.checkOutputDirectory()) != null)
					{
						Common.setMessage(Resource.getString("run.write.output.notexists") + ":");
						Common.setMessage("'" + str + "'");
						break;
					}

					Common.setMessage(Resource.getString("run.write.raw") + " " + collection.getOutputDirectory());

					collection.startProcessing(Common.isRunningCLI());
 
					/**
					 * call the process
					 */
					processCollection(collection);

					CommonParsing.setPvaPidToExtract(0);

					/**
					 * finish collection
					 */
					collection.finishProcessing();

					break;
				}
			} 

			Common.updateProgressBar(Resource.getString("run.done", "" + d) + " " + Common.formatTime_1(Common.getProcessTime()));

		} catch (Exception e8) {

			Common.setMessage(Resource.getString("run.stopped"));
			Common.setExceptionMessage(e8);

			stop_on_error = true;

		} catch (Error e9)	{

			Common.setMessage(Resource.getString("run.stopped"));
			Common.setErrorMessage(e9);

			stop_on_error = true;
		}

		CommonParsing.setPvaPidExtraction(false);

		if (CommonParsing.isInfoScan())
		{
			Common.setMessage("");
			Common.setMessage(Resource.getString("run.end.quick.info"));
		}

		CommonParsing.setProcessPausing(false);
		CommonParsing.setProcessCancelled(false);

		//yield();

		CommonParsing.setInfoScan(false);

		if (stop_on_error)
		{
			Common.setMessage(Resource.getString("all.msg.error.summary", String.valueOf(Common.getErrorCount())));

			collection.closeDebugLogStream();
			collection.closeNormalLogStream(Common.getMessageLog());

			//Common.clearMessageLog();
		}

		else
			Common.setMessage(" ", false, 0xEFFFEF);

		collection.finishProcessing();

		/**
		 * exit on CLI mode
		 */
		if (Common.isRunningCLI() || Common.getSettings().getBooleanProperty(Keys.KEY_closeOnEnd))
			Common.exitApplication(stop_on_error ? 1 : 0);

		Common.getGuiInterface().resetMainFrameTitle();
	}  


	/**
	 * list settings on start
	 */
	private void messageSettings()
	{
		Common.setMessage(" ");

		//biglog
		messageSetting(Keys.KEY_DebugLog);

		//normallog
		messageSetting(Keys.KEY_NormalLog);

		//MPG->sPES
		messageSetting(Keys.KEY_simpleMPG);

		//sPES->MPG
		messageSetting(Keys.KEY_enhancedPES);

		//split
		if (Common.getSettings().getBooleanProperty(Keys.KEY_SplitSize))
			Common.setMessage(Resource.getString("run.split.output") + " " + Common.getSettings().getProperty(Keys.KEY_ExportPanel_SplitSize_Value) + " MB");

		//write video
		messageSetting(Keys.KEY_WriteOptions_writeVideo);

		//write others
		messageSetting(Keys.KEY_WriteOptions_writeAudio);

		//demux
		if (Common.getSettings().getIntProperty(Keys.KEY_ConversionMode) == CommonParsing.ACTION_DEMUX)
		{
			//add offset
			if (Common.getSettings().getBooleanProperty(Keys.KEY_additionalOffset))
				Common.setMessage(Resource.getString("run.add.time.offset", "" + Common.getSettings().getProperty(Keys.KEY_ExportPanel_additionalOffset_Value)));

			//idd
			if (Common.getSettings().getBooleanProperty(Keys.KEY_ExternPanel_createM2sIndex))
				Common.setMessage("-> " + Resource.getString("ExternPanel.createM2sIndex.Tip") + " " + Resource.getString(Keys.KEY_ExternPanel_createM2sIndex[0]));

			//d2v_1
			messageSetting(Keys.KEY_ExternPanel_createD2vIndex);

			//d2v_2
			if (Common.getSettings().getBooleanProperty(Keys.KEY_ExternPanel_splitProjectFile))
				Common.setMessage("-> " + Resource.getString(Keys.KEY_ExternPanel_createD2vIndex[0]) + " " + Resource.getString(Keys.KEY_ExternPanel_splitProjectFile[0]));

			//dar export limit
			if (Common.getSettings().getBooleanProperty(Keys.KEY_OptionDAR))
				Common.setMessage("-> " + Resource.getString("CollectionPanel.ExportLimits") + " " + Resource.getString(Keys.KEY_OptionDAR[0]) + " " + Keys.ITEMS_ExportDAR[Common.getSettings().getIntProperty(Keys.KEY_ExportDAR)]);

			//h_resol export limit
			if (Common.getSettings().getBooleanProperty(Keys.KEY_OptionHorizontalResolution))
				Common.setMessage("-> " + Resource.getString("CollectionPanel.ExportLimits") + " " + Resource.getString(Keys.KEY_OptionHorizontalResolution[0]) + " " + Common.getSettings().getProperty(Keys.KEY_ExportHorizontalResolution));

			//C.D.Flag
			messageSetting(Keys.KEY_VideoPanel_clearCDF);

			//patch2interl
			messageSetting(Keys.KEY_VideoPanel_patchToInterlaced);

			//patch2progr
			messageSetting(Keys.KEY_VideoPanel_patchToProgressive);

			//patchfield
			messageSetting(Keys.KEY_VideoPanel_toggleFieldorder);

			//Sequ_endcode
			messageSetting(Keys.KEY_VideoPanel_addEndcode);

			//Sequ_endcode on changes
			messageSetting(Keys.KEY_VideoPanel_insertEndcode);

			//SDE
			if (Common.getSettings().getBooleanProperty(Keys.KEY_VideoPanel_addSde))
				Common.setMessage("-> " + Resource.getString(Keys.KEY_VideoPanel_addSde[0]) + " " + Common.getSettings().getProperty(Keys.KEY_VideoPanel_SdeValue));

			//add missing sequ_header
			messageSetting(Keys.KEY_VideoPanel_addSequenceHeader);
		}

		boolean invers = true;

		//es types to demux_detect
		messageSetting(Resource.getString("run.stream.type.disabled"), Keys.KEY_Streamtype_MpgVideo, invers);
		messageSetting(Resource.getString("run.stream.type.disabled"), Keys.KEY_Streamtype_MpgAudio, invers);
		messageSetting(Resource.getString("run.stream.type.disabled"), Keys.KEY_Streamtype_Ac3Audio, invers);
		messageSetting(Resource.getString("run.stream.type.disabled"), Keys.KEY_Streamtype_PcmAudio, invers);
		messageSetting(Resource.getString("run.stream.type.disabled"), Keys.KEY_Streamtype_Teletext, invers);
		messageSetting(Resource.getString("run.stream.type.disabled"), Keys.KEY_Streamtype_Subpicture, invers);
		messageSetting(Resource.getString("run.stream.type.disabled"), Keys.KEY_Streamtype_Vbi, invers);

		/**
		 * enhanced
		 */
		messageSetting(Keys.KEY_PVA_FileOverlap);
		messageSetting(Keys.KEY_PVA_Audio);
		messageSetting(Keys.KEY_VOB_resetPts);
		messageSetting(Keys.KEY_TS_ignoreScrambled);
		messageSetting(Keys.KEY_TS_blindSearch);
		messageSetting(Keys.KEY_TS_joinPackets);
		messageSetting(Keys.KEY_TS_HumaxAdaption);
		messageSetting(Keys.KEY_TS_FinepassAdaption);
		messageSetting(Keys.KEY_TS_generatePmt);
		messageSetting(Keys.KEY_TS_generateTtx);
		messageSetting(Keys.KEY_Input_getEnclosedPackets);
		messageSetting(Keys.KEY_Input_concatenateForeignRecords);
		messageSetting(Keys.KEY_Video_ignoreErrors);
		messageSetting(Keys.KEY_Video_trimPts);
		messageSetting(Keys.KEY_Conversion_startWithVideo);
		messageSetting(Keys.KEY_Conversion_addPcrToStream);

		Common.setMessage(" ");
	}

	/**
	 * list settings on start
	 */
	private void messageSetting(String[] key)
	{
		if (Common.getSettings().getBooleanProperty(key))
			Common.setMessage("-> " + Resource.getString(key[0]));
	}

	/**
	 * list settings on start
	 */
	private void messageSetting(String str, String[] key)
	{
		if (Common.getSettings().getBooleanProperty(key))
			Common.setMessage(str + " " + Resource.getString(key[0]));
	}

	/**
	 * list settings on start
	 */
	private void messageSetting(String str, String[] key, boolean invers)
	{
		if (Common.getSettings().getBooleanProperty(key) != invers)
			Common.setMessage(str + " " + Resource.getString(key[0]));
	}

	/**
	 * stops the processing until next user action
	 */
	public boolean pause()
	{
		if (!CommonParsing.isProcessPausing())
			return false;

		try { 

			sleep(1000); 

		} catch (InterruptedException ie) { 

			Common.setMessage("!> interrupted suspend mode ");
			Common.setExceptionMessage(ie);
		}

		return true;
	}


	/**
	 * call the important routines
	 */
	private void processCollection(JobCollection collection)
	{
		JobProcessing job_processing = collection.getJobProcessing();

		String vptslog = "-1";
		String oldvptslog = "-1";

		MainBufferSize = Integer.parseInt(Common.getSettings().getProperty(Keys.KEY_MainBuffer));

		if (MainBufferSize <= 0)
			MainBufferSize = 4096000;

		job_processing.setSplitSize(Common.getSettings().getBooleanProperty(Keys.KEY_SplitSize) ? 0x100000L * Integer.parseInt(Common.getSettings().getProperty(Keys.KEY_ExportPanel_SplitSize_Value)) : 0);

		long splitsize = job_processing.getSplitSize();

		job_processing.setLastHeaderBytePosition(0);
		job_processing.setFirstAudioPts(0);
		job_processing.setSplitPart(0);
		job_processing.setSplitLoopActive(true);
		job_processing.setNextFileStartPts(0);
		job_processing.setCutComparePoint(10000000);
		job_processing.setVideoExportTime(0);
		job_processing.setVideoExportTimeSummary(0);
		job_processing.setLastGopTimecode(0);
		job_processing.setLastGopPts(0);
		job_processing.setLastSimplifiedPts(0);
		job_processing.setMediaFilesExportLength(0);

		CommonParsing.setVideoFramerate(3600.0);

		if (Common.getSettings().getBooleanProperty(Keys.KEY_ExternPanel_splitProjectFile)) 
			job_processing.setProjectFileSplitSize(Long.parseLong(Common.getSettings().getProperty(Keys.KEY_ExternPanel_ProjectFileSplitSize))  * 1048576L);

		job_processing.setElementaryVideoStream(false);
		job_processing.set1stVideoPTS(-1);

		VBI.reset();

		String[] convertType = { 
			Resource.getString("working.convertType.demux"),
			Resource.getString("working.convertType.makeVDR"),
			Resource.getString("working.convertType.makeMPG2"),
			Resource.getString("working.convertType.makePVA"),
			Resource.getString("working.convertType.makeTS"), 
			Resource.getString("working.convertType.packetFilter") 
		};

		/**
		 * gets collection priority of action type
		 */
		int action = collection.getActionType();

		if (action < 0)
			action = Common.getSettings().getIntProperty(Keys.KEY_ConversionMode);

		/**
		 * determine primary file segments
		 * scan only if changed (date modified) or not scanned, planned
		 */
		List input_files = collection.getInputFilesAsList();

		int inputfiles_size = input_files.size();
		int primaryInputFiles = 0;

		XInputFile xInputFile;

		StreamParserBase streamparser_basic = new StreamParserBase();

		filesearch:
		for (int a = 0, b = -1, type = -1; a < inputfiles_size; a++)
		{
			xInputFile = ((XInputFile) input_files.get(a)).getNewInstance();

			if (xInputFile == null)
				continue;

			if (xInputFile.getStreamInfo() == null)  // should already be set
				Common.getScanClass().getStreamInfo(xInputFile);

			type = xInputFile.getStreamInfo().getStreamType();

			if (b != -1 && b != type)
				break filesearch;

			switch (type)
			{
			case CommonParsing.PES_AV_TYPE:
			case CommonParsing.PES_MPA_TYPE:
			case CommonParsing.PES_PS1_TYPE:
				if (a == 0 && action == CommonParsing.ACTION_DEMUX)
					job_processing.setNextFileStartPts(streamparser_basic.nextFilePTS(collection, CommonParsing.PRIMARY_PES_PARSER, CommonParsing.PES_AV_TYPE, 0, 0));

				break;

			case CommonParsing.MPEG1PS_TYPE:
				if (a == 0 && action == CommonParsing.ACTION_DEMUX)
					job_processing.setNextFileStartPts(streamparser_basic.nextFilePTS(collection, CommonParsing.PRIMARY_PES_PARSER, type, 0, 0));

				break;

			case CommonParsing.MPEG2PS_TYPE:
				if (a == 0 && action == CommonParsing.ACTION_DEMUX)
					job_processing.setNextFileStartPts(streamparser_basic.nextFilePTS(collection, CommonParsing.PRIMARY_PES_PARSER, type, 0, 0));

				break;

			case CommonParsing.PVA_TYPE:
				if (a == 0 && action == CommonParsing.ACTION_DEMUX)
					job_processing.setNextFileStartPts(streamparser_basic.nextFilePTS(collection, CommonParsing.PVA_PARSER, CommonParsing.PES_AV_TYPE, 0, 0));

				break;

			case CommonParsing.TS_TYPE:
				if (a == 0 && action == CommonParsing.ACTION_DEMUX)
					job_processing.setNextFileStartPts(streamparser_basic.nextFilePTS(collection, CommonParsing.TS_PARSER, CommonParsing.PES_AV_TYPE, 0, 0));

				break;

			default:
				break filesearch;
			}

			b = type;

			primaryInputFiles++;
		}


		collection.setPrimaryInputFileSegments(primaryInputFiles);

		Common.getGuiInterface().resetBitrateMonitor();

		/**
		 * loop for split output segments
		 */
		while (job_processing.isSplitLoopActive())
		{
			Common.showSplitPart(job_processing.getSplitPart());

			job_processing.clearSummaryInfo();
			job_processing.clearSubStreamCounters();
			job_processing.setBorrowedPts(-1);
			job_processing.countVideoExportTimeSummary(job_processing.getVideoExportTime());
			job_processing.setVideoExportTime(0);

			/**
			 * do not need the last video pts log anymore
			 */
			if (new File(vptslog).exists()) 
				new File(vptslog).delete();

			/**
			 * loop of collection input files 
			 */
			inputfile_loop:
			for (int i = 0; i < inputfiles_size; i++)
			{
				/**
				 * skip the combined file segments and continue with secondary data
				 */
				if (i == 1 && collection.getPrimaryInputFileSegments() > 0)
					i = collection.getPrimaryInputFileSegments();

				/**
				 * secondary data empty
				 */
				if (i >= inputfiles_size)
					break;

				xInputFile = (XInputFile) input_files.get(i);

				Common.setMessage("");
				Common.setMessage(Resource.getString("working.file", "" + i, "'" + xInputFile + "'", Common.formatNumber(xInputFile.length())));

				/**
				 * was added, but is lost now
				 */
				if (!xInputFile.exists())
				{
					Common.setMessage(Resource.getString("working.file.not.found"));
					continue inputfile_loop;
				}

				/**
				 * some probs with the length, e.g. wrong time information in the FAT
				 */
				if (xInputFile.length() <= 0)
				{
					Common.setMessage(Resource.getString("working.file.not.found") + " " + Resource.getString("ScanInfo.Size") + " " + Common.formatNumber(xInputFile.length()) + " " + Resource.getString("ScanInfo.Bytes"));
					continue inputfile_loop;
				}

				/**
				 * determine filetype again
				 */
				if (xInputFile.getStreamInfo() == null)
					Common.getScanClass().getStreamInfo(xInputFile);

				int filetype = xInputFile.getStreamInfo().getStreamType();

				Common.setMessage(Resource.getString("working.filetype", Keys.ITEMS_FileTypes[filetype]));

				
				/**
				 * the parsing
				 */
				switch (filetype)
				{
				case CommonParsing.PES_AV_TYPE:
					if (i > 0) //added
						(new StreamParser(CommonParsing.SECONDARY_PES_PARSER)).parseStream(collection, xInputFile, filetype, action, vptslog);

					else
					{
						if (!CommonParsing.getPvaPidExtraction()) 
							Common.setMessage(convertType[action]);

						if (Common.getSettings().getBooleanProperty(Keys.KEY_enhancedPES))
							vptslog = (new StreamParser(CommonParsing.PRIMARY_PES_PARSER)).parseStream(collection, xInputFile, CommonParsing.MPEG2PS_TYPE, action, vptslog);
						else
							vptslog = (new StreamParser(CommonParsing.PRIMARY_PES_PARSER)).parseStream(collection, xInputFile, filetype, action, vptslog);

						if (action == CommonParsing.ACTION_DEMUX) 
							CommonParsing.resetSplitMode(job_processing, vptslog);
					}

					break;

				case CommonParsing.MPEG1PS_TYPE:
					if (i > 0) 
						(new StreamParser(CommonParsing.SECONDARY_PES_PARSER)).parseStream(collection, xInputFile, filetype, action, vptslog);
	
					else
					{
						if (!CommonParsing.getPvaPidExtraction()) 
							Common.setMessage(convertType[action]);

						vptslog = (new StreamParser(CommonParsing.PRIMARY_PES_PARSER)).parseStream(collection, xInputFile, filetype, action, vptslog);

						if (action == CommonParsing.ACTION_DEMUX) 
							CommonParsing.resetSplitMode(job_processing, vptslog);
					}

					break;

				case CommonParsing.MPEG2PS_TYPE:
					if (i > 0) 
						(new StreamParser(CommonParsing.SECONDARY_PES_PARSER)).parseStream(collection, xInputFile, filetype, action, vptslog);

					else
					{
						if (!CommonParsing.getPvaPidExtraction()) 
							Common.setMessage(convertType[action]);

						if (Common.getSettings().getBooleanProperty(Keys.KEY_simpleMPG))
							vptslog = (new StreamParser(CommonParsing.PRIMARY_PES_PARSER)).parseStream(collection, xInputFile, CommonParsing.PES_AV_TYPE, action, vptslog);
						else
							vptslog = (new StreamParser(CommonParsing.PRIMARY_PES_PARSER)).parseStream(collection, xInputFile, filetype, action, vptslog);

						if (action == CommonParsing.ACTION_DEMUX) 
							CommonParsing.resetSplitMode(job_processing, vptslog);
					}

					break;

				case CommonParsing.PVA_TYPE:
					if (i > 0) 
						Common.setMessage(Resource.getString("all.msg.noprimaryfile"));

					else
					{
						if (!CommonParsing.getPvaPidExtraction()) 
							Common.setMessage(convertType[action]);
	
						vptslog = (new StreamParser(CommonParsing.PVA_PARSER)).parseStream(collection, xInputFile, CommonParsing.PES_AV_TYPE, action, vptslog);

						if (action == CommonParsing.ACTION_DEMUX) 
							CommonParsing.resetSplitMode(job_processing, vptslog);
					}

					break;

				case CommonParsing.TS_TYPE:
					if (i > 0) 
						Common.setMessage(Resource.getString("all.msg.noprimaryfile"));

					else
					{
						if (!CommonParsing.getPvaPidExtraction()) 
							Common.setMessage(convertType[action]);

						vptslog = (new StreamParser(CommonParsing.TS_PARSER)).parseStream(collection, xInputFile, CommonParsing.PES_AV_TYPE, action, null);

						if (action == CommonParsing.ACTION_DEMUX) 
							CommonParsing.resetSplitMode(job_processing, vptslog);
					}

					break;

				case CommonParsing.PES_MPA_TYPE: 
				case CommonParsing.PES_PS1_TYPE:
					if (i > 0)
					{
						CommonParsing.resetSplitMode(job_processing, vptslog);

						(new StreamParser(CommonParsing.SECONDARY_PES_PARSER)).parseStream(collection, xInputFile, CommonParsing.PES_AV_TYPE, action, vptslog);
					}

					else
					{
						if (!CommonParsing.getPvaPidExtraction()) 
							Common.setMessage(convertType[action]);

						if (Common.getSettings().getBooleanProperty(Keys.KEY_enhancedPES))
							vptslog = (new StreamParser(CommonParsing.PRIMARY_PES_PARSER)).parseStream(collection, xInputFile, CommonParsing.MPEG2PS_TYPE, action, vptslog);
						else
							vptslog = (new StreamParser(CommonParsing.PRIMARY_PES_PARSER)).parseStream(collection, xInputFile, CommonParsing.PES_AV_TYPE, action, vptslog);

						if (action == CommonParsing.ACTION_DEMUX) 
							CommonParsing.resetSplitMode(job_processing, vptslog);
					}

					break;

				case CommonParsing.ES_MPA_TYPE:
				case CommonParsing.ES_AC3_A_TYPE:
				case CommonParsing.ES_AC3_TYPE:
				case CommonParsing.ES_DTS_TYPE:
				case CommonParsing.ES_DTS_A_TYPE:
				case CommonParsing.ES_RIFF_TYPE:
					CommonParsing.resetSplitMode(job_processing, vptslog);

					(new StreamParser(CommonParsing.ES_AUDIO_PARSER)).parseStream(collection, xInputFile, filetype, action, vptslog);

					break;

				case CommonParsing.ES_MPV_TYPE:
					vptslog = (new StreamParser(CommonParsing.ES_VIDEO_PARSER)).parseStream(collection, xInputFile, filetype, action, vptslog);

					CommonParsing.resetSplitMode(job_processing, vptslog);

					break;

				case CommonParsing.ES_SUP_TYPE:
					CommonParsing.resetSplitMode(job_processing, vptslog);

					(new StreamParser(CommonParsing.ES_SUBPICTURE_PARSER)).parseStream(collection, xInputFile, filetype, action, vptslog);

					break;

				case CommonParsing.Unsupported:
				default: 
					Common.setMessage(Resource.getString("working.file.notsupported"));
				}
			}

			/** 
			 * print end of splitpart
			 */
			if (job_processing.getSplitSize() > 0)
			{
				Common.setMessage(Resource.getString("working.end.of.part") + " " + job_processing.getSplitPart());
				job_processing.setSplitPart(job_processing.getSplitPart() + 1);
			}

			else 
				job_processing.setSplitLoopActive(false);


			Common.setMessage("");

			/**
			 * print created files summary
			 */
			Object[] lastlist = job_processing.getSummaryInfo().toArray();
			Arrays.sort(lastlist);

			Common.setMessage(Resource.getString("working.summary"));
			Common.setMessage(lastlist);

			job_processing.clearSummaryInfo();

			if (!CommonParsing.isInfoScan())
				Common.performPostCommand(lastlist);
		}

		Common.setMessage("=> " + Common.formatNumber(job_processing.getMediaFilesExportLength()) + " " + Resource.getString("working.bytes.written"));

		Common.setMessage(Resource.getString("all.msg.error.summary", String.valueOf(Common.getErrorCount())));

		File mpegvideolog = new File(vptslog);

		if (mpegvideolog.exists()) 
			mpegvideolog.delete();

		collection.closeDebugLogStream();
		collection.closeNormalLogStream(Common.getMessageLog());

		//Common.clearMessageLog();

		/**
		 * delete tempfiles which have been multi-used 
		 */
		List tempfiles = job_processing.getTemporaryFileList();

		if (!tempfiles.isEmpty())
		{
			for (int i = 0; i < tempfiles.size(); i += 2)
				if ( new File(tempfiles.get(i).toString()).exists() ) 
					new File(tempfiles.get(i).toString()).delete();

			tempfiles.clear();
		}

		job_processing.setSplitSize(splitsize);

		Toolkit.getDefaultToolkit().beep();
	}

}
