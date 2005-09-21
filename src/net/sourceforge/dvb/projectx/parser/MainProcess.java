/*
 * @(#)MainProcess
 *
 * Copyright (c) 2001-2005 by dvb.matt, All rights reserved.
 * 
 * This file is part of X, a free Java based demux utility.
 * X is intended for educational purposes only, as a non-commercial test project.
 * It may not be used otherwise. Most parts are only experimental.
 * 
 *
 * This program is free software; you can redistribute it free of charge
 * and/or modify it under the terms of the GNU General Public License as published by
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

import net.sourceforge.dvb.projectx.audio.Audio;
import net.sourceforge.dvb.projectx.audio.CRC;
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

	private Audio audio;
	private MpaConverter MPAConverter;
	private MpaDecoder MPADecoder;

	private static int ERRORCODE = 0;
	private static int MainBufferSize = 8192000;

	private static double videotimecount = 0.0;

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
		long process_time = 0;

		Common.setGlobalDebug(Common.getSettings().getBooleanProperty(Keys.KEY_DebugLog));

		JobCollection collection = null;
		JobProcessing job_processing = null;

		try {
			process_time = System.currentTimeMillis();

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
					job_processing.getChapters().finish(collection.getFirstFileBase());

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
 
					/**
					 * call the process
					 */
					processCollection(collection);

					CommonParsing.setPvaPidToExtract(0);

					break;
				}
			} 

			Common.updateProgressBar(Resource.getString("run.done", "" + d) + " " + Common.formatTime_1(System.currentTimeMillis() - process_time));

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
			collection.closeDebugLogStream();
			collection.closeNormalLogStream(Common.getMessageLog());

			Common.clearMessageLog();
		}

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
		if (Common.getSettings().getBooleanProperty(Keys.KEY_DebugLog))
			Common.setMessage("-> " + Resource.getString(Keys.KEY_DebugLog[0]));

		//normallog
		if (Common.getSettings().getBooleanProperty(Keys.KEY_NormalLog))
			Common.setMessage("-> " + Resource.getString(Keys.KEY_NormalLog[0]));

		//MPG->sPES
		if (Common.getSettings().getBooleanProperty(Keys.KEY_simpleMPG))
			Common.setMessage("-> " + Resource.getString(Keys.KEY_simpleMPG[0]));

		//sPES->MPG
		if (Common.getSettings().getBooleanProperty(Keys.KEY_enhancedPES))
			Common.setMessage("-> " + Resource.getString(Keys.KEY_enhancedPES[0]));

		//split
		if (Common.getSettings().getBooleanProperty(Keys.KEY_SplitSize))
			Common.setMessage(Resource.getString("run.split.output") + " " + Common.getSettings().getProperty(Keys.KEY_ExportPanel_SplitSize_Value) + " MB");

		//write video
		if (Common.getSettings().getBooleanProperty(Keys.KEY_WriteOptions_writeVideo))
			Common.setMessage("-> " + Resource.getString(Keys.KEY_WriteOptions_writeVideo[0]));

		//write others
		if (Common.getSettings().getBooleanProperty(Keys.KEY_WriteOptions_writeAudio))
			Common.setMessage("-> " + Resource.getString(Keys.KEY_WriteOptions_writeAudio[0]));

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
			if (Common.getSettings().getBooleanProperty(Keys.KEY_ExternPanel_createD2vIndex))
				Common.setMessage("-> " + Resource.getString("ExternPanel.d2v") + " " + Resource.getString(Keys.KEY_ExternPanel_createD2vIndex[0]));

			//d2v_2
			if (Common.getSettings().getBooleanProperty(Keys.KEY_ExternPanel_splitProjectFile))
				Common.setMessage("-> " + Resource.getString("ExternPanel.d2v") + " " + Resource.getString(Keys.KEY_ExternPanel_splitProjectFile[0]));

			//dar export limit
			if (Common.getSettings().getBooleanProperty(Keys.KEY_OptionDAR))
				Common.setMessage("-> " + Resource.getString("CollectionPanel.ExportLimits") + " " + Resource.getString(Keys.KEY_OptionDAR[0]) + " " + Keys.ITEMS_ExportDAR[Common.getSettings().getIntProperty(Keys.KEY_ExportDAR)]);

			//h_resol export limit
			if (Common.getSettings().getBooleanProperty(Keys.KEY_OptionHorizontalResolution))
				Common.setMessage("-> " + Resource.getString("CollectionPanel.ExportLimits") + " " + Resource.getString(Keys.KEY_OptionHorizontalResolution[0]) + " " + Common.getSettings().getProperty(Keys.KEY_ExportHorizontalResolution));

			//C.D.Flag
			if (Common.getSettings().getBooleanProperty(Keys.KEY_VideoPanel_clearCDF))
				Common.setMessage("-> " + Resource.getString(Keys.KEY_VideoPanel_clearCDF[0]));

			//patch2interl
			if (Common.getSettings().getBooleanProperty(Keys.KEY_VideoPanel_patchToInterlaced))
				Common.setMessage("-> " + Resource.getString(Keys.KEY_VideoPanel_patchToInterlaced[0]));

			//patch2progr
			if (Common.getSettings().getBooleanProperty(Keys.KEY_VideoPanel_patchToProgressive))
				Common.setMessage("-> " + Resource.getString(Keys.KEY_VideoPanel_patchToProgressive[0]));

			//patchfield
			if (Common.getSettings().getBooleanProperty(Keys.KEY_VideoPanel_toggleFieldorder))
				Common.setMessage("-> " + Resource.getString(Keys.KEY_VideoPanel_toggleFieldorder[0]));

			//Sequ_endcode
			if (Common.getSettings().getBooleanProperty(Keys.KEY_VideoPanel_addEndcode))
				Common.setMessage("-> " + Resource.getString(Keys.KEY_VideoPanel_addEndcode[0]));

			//Sequ_endcode on changes
			if (Common.getSettings().getBooleanProperty(Keys.KEY_VideoPanel_insertEndcode))
				Common.setMessage("-> " + Resource.getString(Keys.KEY_VideoPanel_insertEndcode[0]));

			//SDE
			if (Common.getSettings().getBooleanProperty(Keys.KEY_VideoPanel_addSde))
				Common.setMessage("-> " + Resource.getString(Keys.KEY_VideoPanel_addSde[0]) + " " + Common.getSettings().getProperty(Keys.KEY_VideoPanel_SdeValue));

			//add missing sequ_header
			if (Common.getSettings().getBooleanProperty(Keys.KEY_VideoPanel_addSequenceHeader))
				Common.setMessage("-> " + Resource.getString(Keys.KEY_VideoPanel_addSequenceHeader[0]));
		}

		//es types to demux_detect
		if (!Common.getSettings().getBooleanProperty(Keys.KEY_Streamtype_MpgVideo))
			Common.setMessage(Resource.getString("run.stream.type.disabled") + " " + Resource.getString(Keys.KEY_Streamtype_MpgVideo[0]));

		if (!Common.getSettings().getBooleanProperty(Keys.KEY_Streamtype_MpgAudio))
			Common.setMessage(Resource.getString("run.stream.type.disabled") + " " + Resource.getString(Keys.KEY_Streamtype_MpgAudio[0]));

		if (!Common.getSettings().getBooleanProperty(Keys.KEY_Streamtype_Ac3Audio))
			Common.setMessage(Resource.getString("run.stream.type.disabled") + " " + Resource.getString(Keys.KEY_Streamtype_Ac3Audio[0]));

		if (!Common.getSettings().getBooleanProperty(Keys.KEY_Streamtype_PcmAudio))
			Common.setMessage(Resource.getString("run.stream.type.disabled") + " " + Resource.getString(Keys.KEY_Streamtype_PcmAudio[0]));

		if (!Common.getSettings().getBooleanProperty(Keys.KEY_Streamtype_Teletext))
			Common.setMessage(Resource.getString("run.stream.type.disabled") + " " + Resource.getString(Keys.KEY_Streamtype_Teletext[0]));

		if (!Common.getSettings().getBooleanProperty(Keys.KEY_Streamtype_Subpicture))
			Common.setMessage(Resource.getString("run.stream.type.disabled") + " " + Resource.getString(Keys.KEY_Streamtype_Subpicture[0]));

		if (!Common.getSettings().getBooleanProperty(Keys.KEY_Streamtype_Vbi))
			Common.setMessage(Resource.getString("run.stream.type.disabled") + " " + Resource.getString(Keys.KEY_Streamtype_Vbi[0]));


		/**
		 *
		 */
		if (Common.getSettings().getBooleanProperty(Keys.KEY_PVA_FileOverlap))
			Common.setMessage("-> " + Resource.getString(Keys.KEY_PVA_FileOverlap[0]));

		if (Common.getSettings().getBooleanProperty(Keys.KEY_PVA_Audio))
			Common.setMessage("-> " + Resource.getString(Keys.KEY_PVA_Audio[0]));

		if (Common.getSettings().getBooleanProperty(Keys.KEY_VOB_resetPts))
			Common.setMessage("-> " + Resource.getString(Keys.KEY_VOB_resetPts[0]));

		if (Common.getSettings().getBooleanProperty(Keys.KEY_TS_ignoreScrambled))
			Common.setMessage("-> " + Resource.getString(Keys.KEY_TS_ignoreScrambled[0]));

		if (Common.getSettings().getBooleanProperty(Keys.KEY_TS_blindSearch))
			Common.setMessage("-> " + Resource.getString(Keys.KEY_TS_blindSearch[0]));

		if (Common.getSettings().getBooleanProperty(Keys.KEY_TS_joinPackets))
			Common.setMessage("-> " + Resource.getString(Keys.KEY_TS_joinPackets[0]));

		if (Common.getSettings().getBooleanProperty(Keys.KEY_TS_HumaxAdaption))
			Common.setMessage("-> " + Resource.getString(Keys.KEY_TS_HumaxAdaption[0]));

		if (Common.getSettings().getBooleanProperty(Keys.KEY_TS_FinepassAdaption))
			Common.setMessage("-> " + Resource.getString(Keys.KEY_TS_FinepassAdaption[0]));

		if (Common.getSettings().getBooleanProperty(Keys.KEY_TS_generatePmt))
			Common.setMessage("-> " + Resource.getString(Keys.KEY_TS_generatePmt[0]));

		if (Common.getSettings().getBooleanProperty(Keys.KEY_TS_generateTtx))
			Common.setMessage("-> " + Resource.getString(Keys.KEY_TS_generateTtx[0]));

		if (Common.getSettings().getBooleanProperty(Keys.KEY_Input_getEnclosedPackets))
			Common.setMessage("-> " + Resource.getString(Keys.KEY_Input_getEnclosedPackets[0]));

		if (Common.getSettings().getBooleanProperty(Keys.KEY_Input_concatenateForeignRecords))
			Common.setMessage("-> " + Resource.getString(Keys.KEY_Input_concatenateForeignRecords[0]));

		if (Common.getSettings().getBooleanProperty(Keys.KEY_Video_ignoreErrors))
			Common.setMessage("-> " + Resource.getString(Keys.KEY_Video_ignoreErrors[0]));

		if (Common.getSettings().getBooleanProperty(Keys.KEY_Video_trimPts))
			Common.setMessage("-> " + Resource.getString(Keys.KEY_Video_trimPts[0]));

		if (Common.getSettings().getBooleanProperty(Keys.KEY_Conversion_startWithVideo))
			Common.setMessage("-> " + Resource.getString(Keys.KEY_Conversion_startWithVideo[0]));

		if (Common.getSettings().getBooleanProperty(Keys.KEY_Conversion_addPcrToStream))
			Common.setMessage("-> " + Resource.getString(Keys.KEY_Conversion_addPcrToStream[0]));

		Common.setMessage(" ");
	}


	/**
	 * stops the processing until next user action
	 */
	private boolean pause()
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

		filesearch:
		for (int a = 0, b = -1, type = -1; a < inputfiles_size; a++)
		{
			xInputFile = ((XInputFile) input_files.get(a)).getNewInstance();

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
					job_processing.setNextFileStartPts(nextFilePTS(collection, CommonParsing.PRIMARY_PES_PARSER, CommonParsing.PES_AV_TYPE, 0, 0));

				break;

			case CommonParsing.MPEG1PS_TYPE:
				if (a == 0 && action == CommonParsing.ACTION_DEMUX)
					job_processing.setNextFileStartPts(nextFilePTS(collection, CommonParsing.PRIMARY_PES_PARSER, type, 0, 0));

				break;

			case CommonParsing.MPEG2PS_TYPE:
				if (a == 0 && action == CommonParsing.ACTION_DEMUX)
					job_processing.setNextFileStartPts(nextFilePTS(collection, CommonParsing.PRIMARY_PES_PARSER, type, 0, 0));

				break;

			case CommonParsing.PVA_TYPE:
				if (a == 0 && action == CommonParsing.ACTION_DEMUX)
					job_processing.setNextFileStartPts(nextFilePTS(collection, CommonParsing.PVA_PARSER, CommonParsing.PES_AV_TYPE, 0, 0));

				break;

			case CommonParsing.TS_TYPE:
				if (a == 0 && action == CommonParsing.ACTION_DEMUX)
					job_processing.setNextFileStartPts(nextFilePTS(collection, CommonParsing.TS_PARSER, CommonParsing.PES_AV_TYPE, 0, 0));

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

				switch (filetype)
				{
				case CommonParsing.PES_AV_TYPE:
					if (i > 0) //added
						parseSecondaryPES(collection, xInputFile, vptslog, filetype);

					else
					{
						if (!CommonParsing.getPvaPidExtraction()) 
							Common.setMessage(convertType[action]);

					//	vptslog = parsePrimaryPES(collection, xInputFile, filetype, action);
						vptslog = Common.getSettings().getBooleanProperty(Keys.KEY_enhancedPES) ? parsePrimaryPES(collection, xInputFile, CommonParsing.MPEG2PS_TYPE, action) : parsePrimaryPES(collection, xInputFile, filetype, action);

						if (action == CommonParsing.ACTION_DEMUX) 
							resetSplitMode(job_processing, vptslog);
					}

					break;

				case CommonParsing.MPEG1PS_TYPE:
					if (i > 0) 
						parseSecondaryPES(collection, xInputFile, vptslog, filetype);
	
					else
					{
						if (!CommonParsing.getPvaPidExtraction()) 
							Common.setMessage(convertType[action]);

						vptslog = parsePrimaryPES(collection, xInputFile, filetype, action);

						if (action == CommonParsing.ACTION_DEMUX) 
							resetSplitMode(job_processing, vptslog);
					}

					break;

				case CommonParsing.MPEG2PS_TYPE:
					if (i > 0) 
						parseSecondaryPES(collection, xInputFile, vptslog, filetype);

					else
					{
						if (!CommonParsing.getPvaPidExtraction()) 
							Common.setMessage(convertType[action]);

						vptslog = Common.getSettings().getBooleanProperty(Keys.KEY_simpleMPG) ? parsePrimaryPES(collection, xInputFile, CommonParsing.PES_AV_TYPE, action) : parsePrimaryPES(collection, xInputFile, filetype, action);
	
						if (action == CommonParsing.ACTION_DEMUX) 
							resetSplitMode(job_processing, vptslog);
					}

					break;

				case CommonParsing.PVA_TYPE:
					if (!CommonParsing.getPvaPidExtraction()) 
						Common.setMessage(convertType[action]);
	
					vptslog = parsePVA(collection, xInputFile, CommonParsing.PES_AV_TYPE, action, vptslog);

					if (action == CommonParsing.ACTION_DEMUX) 
						resetSplitMode(job_processing, vptslog);

					break;

				case CommonParsing.TS_TYPE:
					if (!CommonParsing.getPvaPidExtraction()) 
						Common.setMessage(convertType[action]);

					vptslog = parseTS(collection, xInputFile, CommonParsing.PES_AV_TYPE, action);

					if (action == CommonParsing.ACTION_DEMUX) 
						resetSplitMode(job_processing, vptslog);

					break;

				case CommonParsing.PES_MPA_TYPE: 
				case CommonParsing.PES_PS1_TYPE:
					if (i > 0)
					{
						resetSplitMode(job_processing, vptslog);
						parseSecondaryPES(collection, xInputFile, vptslog, CommonParsing.PES_AV_TYPE);
					}

					else
					{
						if (!CommonParsing.getPvaPidExtraction()) 
							Common.setMessage(convertType[action]);

					//	vptslog = parsePrimaryPES(collection, xInputFile, CommonParsing.PES_AV_TYPE, action);
						vptslog = Common.getSettings().getBooleanProperty(Keys.KEY_enhancedPES) ? parsePrimaryPES(collection, xInputFile, CommonParsing.MPEG2PS_TYPE, action) : parsePrimaryPES(collection, xInputFile, CommonParsing.PES_AV_TYPE, action);

						if (action == CommonParsing.ACTION_DEMUX) 
							resetSplitMode(job_processing, vptslog);
					}

					break;

				case CommonParsing.ES_AC3_A_TYPE:
					resetSplitMode(job_processing, vptslog);
					parseAudioES(collection, xInputFile, vptslog, "ac");

					break;

				case CommonParsing.ES_AC3_TYPE:
					resetSplitMode(job_processing, vptslog);
					parseAudioES(collection, xInputFile, vptslog, "ac");

					break;

				case CommonParsing.ES_MPA_TYPE:
					resetSplitMode(job_processing, vptslog);
					parseAudioES(collection, xInputFile, vptslog, "mp");

					break;

				case CommonParsing.ES_MPV_TYPE:
					vptslog = parseVideoES(collection, xInputFile);
					resetSplitMode(job_processing, vptslog);

					break;

				case CommonParsing.ES_DTS_TYPE:
					resetSplitMode(job_processing, vptslog);
					parseAudioES(collection, xInputFile, vptslog, "ac");

					break;

				case CommonParsing.ES_DTS_A_TYPE:
					resetSplitMode(job_processing, vptslog);
					parseAudioES(collection, xInputFile, vptslog, "ac");

					break;

				case CommonParsing.ES_RIFF_TYPE:
					resetSplitMode(job_processing, vptslog);
					parseAudioES(collection, xInputFile, vptslog, "wa");

					break;

				case CommonParsing.ES_SUP_TYPE:
					resetSplitMode(job_processing, vptslog);
					parseSubpictureES(collection, xInputFile, vptslog);

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

			//yield();

			if (!CommonParsing.isInfoScan())
				Common.performPostCommand(lastlist);
		}

		Common.setMessage("=> " + Common.formatNumber(job_processing.getMediaFilesExportLength()) + " " + Resource.getString("working.bytes.written"), false, 0xEFFFEF);

		//yield();

		File mpegvideolog = new File(vptslog);

		if (mpegvideolog.exists()) 
			mpegvideolog.delete();

		collection.closeDebugLogStream();
		collection.closeNormalLogStream(Common.getMessageLog());

		Common.clearMessageLog();

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




	/**
	 * split reset
	 */
	private void resetSplitMode(JobProcessing job_processing, String vptslog)
	{
		if ( vptslog.equals("-1") && job_processing.getSplitSize() > 0 )
		{ 
			job_processing.setSplitSize(0);
			Common.setMessage(Resource.getString("splitreset.novideo"));
		}
	}


	/**
	 * loadTempVideoPts
	 */
	private long[][] loadTempVideoPts(String videofile_pts, boolean debug)
	{
		if (videofile_pts.equals("-1"))
			return null;

		if (debug) 
			System.out.println("-> loading video PTS logfile...");   

		XInputFile xInputFile = new XInputFile(new File(videofile_pts));

		int vlogsize = (int)xInputFile.length() / 16;
		long[][] vptsval = new long[2][vlogsize]; 

		byte[] data = new byte[(int)xInputFile.length()];
		int pos = 0;

		try {
			InputStream pts_file = xInputFile.getInputStream();

			pts_file.read(data, 0, data.length);

			for (int i = 0; i < vlogsize; i += 2 )
			{
				vptsval[0][i] = CommonParsing.getValue(data, pos, 8, !CommonParsing.BYTEREORDERING);
				pos += 8;
				vptsval[0][i + 1] = CommonParsing.getValue(data, pos, 8, !CommonParsing.BYTEREORDERING);
				pos += 8;
				vptsval[1][i] = CommonParsing.getValue(data, pos, 8, !CommonParsing.BYTEREORDERING);
				pos += 8;
				vptsval[1][i + 1] = CommonParsing.getValue(data, pos, 8, !CommonParsing.BYTEREORDERING);
				pos += 8;

				if (debug) 
					System.out.println("#s " + i + " _" + vptsval[0][i] + " #e " + (i + 1) + " _" + vptsval[0][i + 1] + " /#s " + i + " _" + vptsval[1][i] + " #e " + (i + 1) + " _" + vptsval[1][i + 1]);
			}

			pts_file.close();

		} catch (Exception e) { 

			Common.setExceptionMessage(e);

			return null;
		}

		Common.setMessage(Resource.getString("video.msg.pts.start_end", Common.formatTime_1(vptsval[0][0] / 90)) + " " + Common.formatTime_1(vptsval[0][vptsval[0].length - 1] / 90));

		return vptsval;
	}


	/**
	 * loadTempOtherPts
	 */
	private long[][] loadTempOtherPts(String filename_pts, String message_1, String message_2, String message_3, String message_4, int es_streamtype, boolean ignoreErrors, boolean debug)
	{
		if (filename_pts.equals("-1"))
			return null;

		if (debug) 
			System.out.println("-> loading PTS logfile...");   

		XInputFile xInputFile = new XInputFile(new File(filename_pts));

		int logsize = (int)xInputFile.length() / 16;
		long[][] ptsval = new long[2][logsize + 1]; 

		ptsval[0][logsize] = -1;  
		ptsval[1][logsize] = -1;

		byte[] data = new byte[(int)xInputFile.length()];
		int pos = 0;

		try {
			InputStream pts_file = xInputFile.getInputStream();

			pts_file.read(data, 0, data.length);

			int aa = 0;

			long ptsVal;
			long ptsPos;

			for (int a = 0; a < logsize; a++)
			{
				ptsVal = CommonParsing.getValue(data, pos, 8, !CommonParsing.BYTEREORDERING);
				pos += 8;
				ptsPos = CommonParsing.getValue(data, pos, 8, !CommonParsing.BYTEREORDERING);
				pos += 8;

				if (debug) 
					System.out.println(" #" + aa + "/" + a + " _" + ptsVal + " / " + ptsPos);

				if (aa > 0 && ptsVal <= ptsval[0][aa - 1])
				{
					if (aa > 1 && Math.abs(ptsVal - ptsval[0][aa - 2]) < 150000 && Math.abs(ptsVal - ptsval[0][aa - 1]) > 500000)
					{
						aa--;

						if (debug)
							System.out.print(" <!^> ");
					}
					else
					{
						if (debug) 
							System.out.print(" <!v> ");

						continue;
					}
				}

				ptsval[0][aa] = ptsVal;
				ptsval[1][aa] = ptsPos;

				aa++;
			}

			if (aa < logsize)
			{
				Common.setMessage(Resource.getString(message_1, " " + (logsize - aa)));

				long tmp[][] = new long[2][aa];

				System.arraycopy(ptsval[0], 0, tmp[0], 0, aa);
				System.arraycopy(ptsval[1], 0, tmp[1], 0, aa);

				ptsval[0] = new long[aa + 1];

				System.arraycopy(tmp[0], 0, ptsval[0], 0, aa);

				ptsval[0][aa]= -1;
				ptsval[1] = new long[aa + 1];

				System.arraycopy(tmp[1], 0, ptsval[1], 0, aa);

				ptsval[1][aa]= -1;
			}

			pts_file.close();

			if (es_streamtype == CommonParsing.TELETEXT && ((ptsval[0][0] == 0xFFFFFFFFL) || (ptsval[0][0] == 0)))
			{
				// missing pts values
				Common.setMessage(Resource.getString(message_4));
				ignoreErrors = true;
			}

			if (ignoreErrors)
			{
				long[] tmp = { ptsval[0][0], ptsval[1][0] };

				ptsval[0] = new long[2]; 
				ptsval[0][0] = tmp[0]; 
				ptsval[0][1]= - 1;

				ptsval[1] = new long[2]; 
				ptsval[1][0] = tmp[1]; 
				ptsval[1][1]= - 1;

				Common.setMessage(Resource.getString(message_2));
			}

		} catch (Exception e) { 

			Common.setExceptionMessage(e);

			return null;
		}

		Common.setMessage(Resource.getString(message_3, Common.formatTime_1(ptsval[0][0] / 90)) + " " + Common.formatTime_1(ptsval[0][ptsval[0].length - 2] / 90));

		return ptsval;
	}

	/**
	 * 
	 */
	private int checkPTSMatch(long video_pts_values[], long data_pts_values[])
	{
		if (data_pts_values[0] < video_pts_values[0])
		{
			if (data_pts_values[data_pts_values.length - 2] < video_pts_values[0])
			{
				/**
				 * maybe does match later, jump just to end
				 */
				Common.setMessage(Resource.getString("checkpts.1st.latter"));

				return (data_pts_values.length - 2);
			}

			else
			{
				/**
				 * maybe does match later, jump just to this position
				 */
				int end = data_pts_values.length - 1;

				for (int i = 0; i < end; i++)
				{
					if (data_pts_values[i + 1] > video_pts_values[0])
						return i;
				}

				return 0;
			}
		}

		else
		{
			if (data_pts_values[0] >= video_pts_values[video_pts_values.length - 1])
			{
				Common.setMessage(Resource.getString("checkpts.last.ends"));

				return -1;
			}

			/**
			 * does match anywhere, no pre-jump
			 */
			else
				return 0;
		}
	}


	/**
	 * 
	 */
	private String infoPTSMatch(String filename_pts, String videofile_pts, boolean video_pts, boolean data_pts)
	{
		if ( !videofile_pts.equals("-1") && !filename_pts.equals("-1") && !video_pts && data_pts)
			return "? ";

		else
			return "";
	}



	/**
	 * secondary PES Parser
	 */
	private void parseSecondaryPES(JobCollection collection, XInputFile aXInputFile, String vptslog, int _pes_streamtype)
	{
		String fchild = collection.getOutputName(aXInputFile.getName());
		String fparent = collection.getOutputNameParent(fchild);

		if (Common.getSettings().getBooleanProperty(Keys.KEY_ExternPanel_appendExtension))
			fparent = collection.getOutputDirectory() + collection.getFileSeparator() + fchild;

		JobProcessing job_processing = collection.getJobProcessing();

		/**
		 * split part 
		 */
		fparent += job_processing.getSplitSize() > 0 ? "(" + job_processing.getSplitPart() + ")" : "" ;

		String paname = fparent + ".ma1";

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

					if (str.equals("tt"))
						processTeletext(collection, tempfiles.get(i).toString(), tempfiles.get(i + 2).toString(), tempfiles.get(i + 3).toString(), vptslog);

					else if (str.equals("sp"))
						processSubpicture(collection, tempfiles.get(i).toString(), tempfiles.get(i + 2).toString(), tempfiles.get(i + 3).toString(), vptslog);

					else if (str.equals("pc"))
						processLPCM(collection, tempfiles.get(i).toString(), tempfiles.get(i + 2).toString(), tempfiles.get(i + 3).toString(), vptslog);

					else 
						processAllAudio(collection, tempfiles.get(i).toString(), tempfiles.get(i + 2).toString(), tempfiles.get(i + 3).toString(), vptslog);

					return;
				}
			}
		}


		boolean Message_2 = Common.getSettings().getBooleanProperty(Keys.KEY_MessagePanel_Msg2);
		boolean Debug = collection.DebugMode();
		boolean SimpleMPG = Common.getSettings().getBooleanProperty(Keys.KEY_simpleMPG);
		boolean GetEnclosedPackets = Common.getSettings().getBooleanProperty(Keys.KEY_Input_getEnclosedPackets);
		boolean IgnoreScrambledPackets = Common.getSettings().getBooleanProperty(Keys.KEY_TS_ignoreScrambled);

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

		byte[] pes_packet = new byte[0x10006];
		byte[] buffered_data;

		long count = 0;
		long size;
		long qexit;

		job_processing.clearStatusVariables();
		int[] clv = job_processing.getStatusVariables();

		job_processing.setMinBitrate(CommonParsing.MAX_BITRATE_VALUE);
		job_processing.setMaxBitrate(0);
		job_processing.setExportedVideoFrameNumber(0);
		job_processing.setEndPtsOfGop(-10000);
		job_processing.setSequenceHeader(true);
		job_processing.setAllMediaFilesExportLength(0);
		job_processing.setProjectFileExportLength(0);

		Hashtable substreams = new Hashtable();
		StandardBuffer sb;

		List demuxList = job_processing.getSecondaryPESDemuxList();

		demuxList.clear();

		StreamDemultiplexer streamdemultiplexer = null;

		try {

			PushbackInputStream in = new PushbackInputStream(aXInputFile.getInputStream(), pes_packet.length);

			size = aXInputFile.length();

			Common.updateProgressBar(Resource.getString("parseSecondaryPES.demux.pes") + " " + aXInputFile.getName(), 0, 0);

			//yield();

			qexit = count + (0x100000L * Integer.parseInt(Common.getSettings().getProperty(Keys.KEY_ExportPanel_Infoscan_Value)));

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

								in.skip(offset);

								count += (14 + offset);
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

								Common.setMessage(Resource.getString("parseTS.scrambled", Integer.toHexString(pesID).toUpperCase(), "-", String.valueOf(count - pes_packetlength)));
							}

							continue loop;
						}

						else if (scrambling_messaged)
							Common.setMessage(Resource.getString("parseTS.clear", Integer.toHexString(pesID).toUpperCase(), "-", String.valueOf(count - pes_packetlength)));
					}

					/**
					 * vdr_dvbsub determination
					 */
					pes_extension2_id = CommonParsing.getExtension2_Id(pes_packet, pes_headerlength, pes_payloadlength, pesID, pes_isMpeg2);

					isTeletext = false;
					subID = 0;

					if (pesID == CommonParsing.PRIVATE_STREAM_1_CODE && pes_payloadlength > 2)
					{
						offset = pes_headerlength + pes_extensionlength;

						if (offset < pes_packetlength)
						{
							subID = 0xFF & pes_packet[offset];
							isTeletext = pes_extensionlength == 0x24 && subID>>>4 == 1;

							//subpic in vdr_pes
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

							streamdemultiplexer = new StreamDemultiplexer();
							streamdemultiplexer.setID(pesID);
							streamdemultiplexer.setsubID(0);
							streamdemultiplexer.setType(CommonParsing.MPEG_VIDEO);
							streamdemultiplexer.setStreamType(pes_streamtype);

							demuxList.add(streamdemultiplexer);
							break; 

						case 0xC0:
						case 0xD0:
							IDtype = Resource.getString("idtype.mpeg.audio"); 

							streamdemultiplexer = new StreamDemultiplexer();
							streamdemultiplexer.setID(pesID);
							streamdemultiplexer.setsubID(0);
							streamdemultiplexer.setType(CommonParsing.MPEG_AUDIO);
							streamdemultiplexer.setStreamType(pes_streamtype);

							demuxList.add(streamdemultiplexer);
							streamdemultiplexer.init(fparent, MainBufferSize / demuxList.size(), demuxList.size(), CommonParsing.SECONDARY_PES_PARSER);

							break; 
						}

						switch (pesID)
						{
						case CommonParsing.PRIVATE_STREAM_1_CODE:
							IDtype = Resource.getString("idtype.private.stream");
							IDtype += (isTeletext ? " TTX ": "") + (subID != 0 ? " (SubID 0x" + Integer.toHexString(subID).toUpperCase() + ")" : ""); 

							streamdemultiplexer = new StreamDemultiplexer();
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
							streamdemultiplexer.init(fparent, MainBufferSize / demuxList.size(), demuxList.size(), CommonParsing.SECONDARY_PES_PARSER);

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

			int[] stream_number = new int[10];

			for (int i = 0, es_streamtype; i < demuxList.size(); i++)
			{
				streamdemultiplexer = (StreamDemultiplexer) demuxList.get(i);
				es_streamtype = streamdemultiplexer.getType();

				if (es_streamtype == CommonParsing.MPEG_VIDEO) 
					continue;

				String[] values = streamdemultiplexer.close(job_processing, vptslog);

				if (values[0].equals("")) 
				{
					Common.setMessage(Resource.getString("parseSecondaryPES.msg.noexport") + Integer.toHexString(streamdemultiplexer.getID()).toUpperCase() + ")");
					continue;
				}

				String newfile = values[3] + (stream_number[es_streamtype] > 0 ? ("[" + stream_number[es_streamtype] + "]") : "") + "." + values[2];

				Common.renameTo(values[0], newfile);

				values[0] = newfile;
				values[3] = vptslog;

				switch (es_streamtype)
				{
				case CommonParsing.AC3_AUDIO:
				case CommonParsing.DTS_AUDIO:
					if ( streamdemultiplexer.subID() != 0 && (0xF0 & streamdemultiplexer.subID()) != 0x80 ) 
						break;

					Common.setMessage("");
					Common.setMessage(Resource.getString("parseSecondaryPES.ac3.audio") + ((streamdemultiplexer.subID() != 0) ? ("(SubID 0x" + Integer.toHexString(streamdemultiplexer.subID()).toUpperCase()+")") : ""));

					processAllAudio(collection, values[0], values[1], values[2], values[3]);
					break;

				case CommonParsing.TELETEXT: 
					Common.setMessage("");
					Common.setMessage(Resource.getString("parseSecondaryPES.teletext") + " (SubID 0x" + Integer.toHexString(streamdemultiplexer.subID()).toUpperCase() + ")");

					processTeletext(collection, values[0], values[1], values[2], values[3]);
					break;

				case CommonParsing.MPEG_AUDIO: 
					Common.setMessage("");
					Common.setMessage(Resource.getString("parseSecondaryPES.mpeg.audio") + " (0x" + Integer.toHexString(streamdemultiplexer.getID()).toUpperCase() + ")");

					processAllAudio(collection, values[0], values[1], values[2], values[3]);
					break;

				case CommonParsing.LPCM_AUDIO:
					Common.setMessage("");
					Common.setMessage(Resource.getString("parseSecondaryPES.lpcm.audio") + " (SubID 0x" + Integer.toHexString(streamdemultiplexer.subID()).toUpperCase() + ")");

					processLPCM(collection, values[0], values[1], values[2], values[3]);
					break;

				case CommonParsing.SUBPICTURE:
					Common.setMessage("");
					Common.setMessage(Resource.getString("parseSecondaryPES.subpic") + " (SubID 0x" + Integer.toHexString(streamdemultiplexer.subID()).toUpperCase() + ")");

					processSubpicture(collection, values[0], values[1], values[2], values[3]);
					break;
				}

				stream_number[es_streamtype]++;

				// save infos for output segmentation
				tempfiles.add(values[0]);
				tempfiles.add(aXInputFile);
				tempfiles.add(values[1]);
				tempfiles.add(values[2]);

				if (job_processing.getSplitSize() == 0)
				{
					new File(newfile).delete();
					new File(values[1]).delete();
				}
			}

		} catch (IOException e2) { 

			Common.setExceptionMessage(e2);
		}

		//System.gc();
	}




	/**
	 * primary PES Parser
	 */
	private String parsePrimaryPES(JobCollection collection, XInputFile aXInputFile, int _pes_streamtype, int action)
	{
		String fchild = collection.getOutputName(aXInputFile.getName());
		String fparent = collection.getOutputNameParent(fchild);

		JobProcessing job_processing = collection.getJobProcessing();

		/**
		 * split part 
		 */
		fparent += job_processing.getSplitSize() > 0 ? "(" + job_processing.getSplitPart() + ")" : "" ;

		boolean Message_2 = Common.getSettings().getBooleanProperty(Keys.KEY_MessagePanel_Msg2);
		boolean Debug = collection.DebugMode();
		boolean SimpleMPG = Common.getSettings().getBooleanProperty(Keys.KEY_simpleMPG);
		boolean GetEnclosedPackets = Common.getSettings().getBooleanProperty(Keys.KEY_Input_getEnclosedPackets);
		boolean IgnoreScrambledPackets = Common.getSettings().getBooleanProperty(Keys.KEY_TS_ignoreScrambled);
		boolean PreviewAllGops = Common.getSettings().getBooleanProperty(Keys.KEY_Preview_AllGops);
		boolean DumpDroppedGop = Common.getSettings().getBooleanProperty(Keys.KEY_dumpDroppedGop);
		boolean CreateD2vIndex = Common.getSettings().getBooleanProperty(Keys.KEY_ExternPanel_createD2vIndex);
		boolean SplitProjectFile = Common.getSettings().getBooleanProperty(Keys.KEY_ExternPanel_splitProjectFile);

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

		int Infoscan_Value = Integer.parseInt(Common.getSettings().getProperty(Keys.KEY_ExportPanel_Infoscan_Value));
		int CutMode = Common.getSettings().getIntProperty(Keys.KEY_CutMode);
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

		String vptslog = "-1";

		StreamDemultiplexer streamdemultiplexer = null;
		StreamConverter streamconverter = new StreamConverter();

		Hashtable substreams = new Hashtable();
		StandardBuffer sb;

		List demuxList = job_processing.getPrimaryPESDemuxList();

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
					streamdemultiplexer.initVideo2(fparent);

					if (pesID0 == 0) //?
						pesID0 = streamdemultiplexer.getID();
				}
				else 
					streamdemultiplexer.init2(fparent);
			}
		}

		String mpeg_type_str = (Keys.ITEMS_FileTypes[pes_streamtype]).toString().toLowerCase();
		mpeg_type_str = "[" + mpeg_type_str.substring(0, mpeg_type_str.indexOf(' ')) + "]";

		switch (action)
		{
		case CommonParsing.ACTION_TO_VDR:
			streamconverter.init(fparent + (job_processing.getSplitSize() == 0 ? mpeg_type_str : "") + ".vdr", MainBufferSize, action, job_processing.getSplitPart());
			break;

		case CommonParsing.ACTION_TO_M2P:
			streamconverter.init(fparent + (job_processing.getSplitSize() == 0 ? mpeg_type_str : "") + ".m2p", MainBufferSize, action, job_processing.getSplitPart());
			break;

		case CommonParsing.ACTION_TO_PVA:
			streamconverter.init(fparent + ".pva", MainBufferSize, action, job_processing.getSplitPart());
			break;

		case CommonParsing.ACTION_TO_TS:
			streamconverter.init(fparent + ".ts", MainBufferSize, action, job_processing.getSplitPart());
			break;

		case CommonParsing.ACTION_FILTER:
			streamconverter.init(fparent + "[filtered].pes", MainBufferSize, action, job_processing.getSplitPart());
		}

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
				startPoint = job_processing.getLastHeaderBytePosition() - (1048576L * (Common.getSettings().getIntProperty(Keys.KEY_ExportPanel_Overlap_Value) + 1));

			List CutpointList = collection.getCutpointList();
			List ChapterpointList = collection.getChapterpointList();

			/** 
			 * jump near to first cut-in point to collect more audio
			 */
			if (CutMode == CommonParsing.CUTMODE_BYTE && CutpointList.size() > 0 && CommonParsing.getCutCounter() == 0  && (!PreviewAllGops || action != CommonParsing.ACTION_DEMUX))
				startPoint = Long.parseLong(CutpointList.get(CommonParsing.getCutCounter()).toString()) - ((action == CommonParsing.ACTION_DEMUX) ? 2048000: 0);

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
							if (count > Long.parseLong(CutpointList.get(CommonParsing.getCutCounter() - 1).toString()) + (action == CommonParsing.ACTION_DEMUX ? 2048000 : 64000))
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

								in.skip(offset);

								count += (14 + offset);
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

									if (Common.getSettings().getBooleanProperty(Keys.KEY_VOB_resetPts))
									{
										ptsoffset = nextFilePTS(collection, CommonParsing.PRIMARY_PES_PARSER, pes_streamtype, lastpts, job_processing.getFileNumber(), count);

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

					/**
					 * determine next startcode in zero packets
					 */
					if (isZeroPacket)
					{
						if (Debug) 
							System.out.println("A " + Resource.getString("parsePrimaryPES.packet.length") + " " + count);

						for (int i = pes_packetoffset; isZeroPacket && i <= pes_packetlength; )
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
						System.out.print("\r"+Resource.getString("parsePrimaryPES.packs") + ": " + pesID + "/" + clv[5] + "/" + (pes_packetlength) + "/" + ((count * 100 / size)) + "% " + (count));

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
								System.out.println("B " + Resource.getString("parsePrimaryPES.packet.length") + " " + count);

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

									Common.setMessage(Resource.getString("parseTS.scrambled", Integer.toHexString(pesID).toUpperCase(), "-", String.valueOf(count - pes_packetlength)));
								}

								continue zeropacketloop;
							}

							else if (scrambling_messaged)
								Common.setMessage(Resource.getString("parseTS.clear", Integer.toHexString(pesID).toUpperCase(), "-", String.valueOf(count - pes_packetlength)));
						}

						/**
						 * vdr_dvbsub determination
						 */
						pes_extension2_id = CommonParsing.getExtension2_Id(pes_packet, pes_headerlength, pes_payloadlength, pesID, pes_isMpeg2);

						isTeletext = false;
						subID = 0;

						if (pesID == CommonParsing.PRIVATE_STREAM_1_CODE && pes_payloadlength > 2)
						{
							offset = pes_headerlength + pes_extensionlength;

							if (offset < pes_packetlength)
							{
								subID = 0xFF & pes_packet[offset];
								isTeletext = pes_extensionlength == 0x24 && subID>>>4 == 1; 

								//subpic in vdr_pes
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

							streamconverter.writePacket(job_processing, pes_packet, 0, pes_packetlength);
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

								streamdemultiplexer = new StreamDemultiplexer(ptsoffset);
								streamdemultiplexer.setID(pesID);
								streamdemultiplexer.setType(CommonParsing.MPEG_VIDEO);
								streamdemultiplexer.setnewID(newID[CommonParsing.MPEG_VIDEO]++);
								streamdemultiplexer.setsubID(0);
								streamdemultiplexer.setStreamType(pes_streamtype);

								demuxList.add(streamdemultiplexer);

								if (pesID0 == 0 || pesID0 == pesID)
								{ 
									if (action == CommonParsing.ACTION_DEMUX) 
										streamdemultiplexer.initVideo(fparent, MainBufferSize, demuxList.size(), CommonParsing.PRIMARY_PES_PARSER);

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

								streamdemultiplexer = new StreamDemultiplexer(ptsoffset);
								streamdemultiplexer.setID(pesID);
								streamdemultiplexer.setType(CommonParsing.MPEG_AUDIO);
								streamdemultiplexer.setnewID(newID[CommonParsing.MPEG_AUDIO]++);
								streamdemultiplexer.setsubID(0);
								streamdemultiplexer.setStreamType(pes_streamtype);

								demuxList.add(streamdemultiplexer);

								if (action == CommonParsing.ACTION_DEMUX) 
									streamdemultiplexer.init(fparent, MainBufferSize / demuxList.size(), demuxList.size(), CommonParsing.PRIMARY_PES_PARSER);
	
								else
									IDtype += " " + Resource.getString("idtype.mapped.to") + Integer.toHexString(streamdemultiplexer.getnewID()).toUpperCase();

								break; 
							}

							switch (pesID)
							{
							case CommonParsing.PRIVATE_STREAM_1_CODE:
								IDtype = Resource.getString("idtype.private.stream");
								IDtype += (isTeletext ? " TTX " : "") + (subID != 0 ? " (SubID 0x" + Integer.toHexString(subID).toUpperCase() + ")" : ""); 

								streamdemultiplexer = new StreamDemultiplexer(ptsoffset);
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
										streamdemultiplexer.init(fparent, MainBufferSize / demuxList.size(), demuxList.size(), CommonParsing.PRIMARY_PES_PARSER);
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

					if (Common.getSettings().getBooleanProperty(Keys.KEY_Input_concatenateForeignRecords) && action == CommonParsing.ACTION_DEMUX)
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
					Common.setMessage(Resource.getString("parsePrimaryPES.switch") + " " + nextXInputFile + " (" + Common.formatNumber(nextXInputFile.length()) + " bytes)");

					Common.updateProgressBar((action == CommonParsing.ACTION_DEMUX ? Resource.getString("parsePrimaryPES.demuxing") : Resource.getString("parsePrimaryPES.converting")) + Resource.getString("parsePrimaryPES.avpes.file") + " " + nextXInputFile.getName());
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


			if (action != CommonParsing.ACTION_DEMUX) 
				streamconverter.close(job_processing, CommonParsing.isInfoScan());

			else
			{
				for (int i = 0, NumberOfVideostreams = 0; i < demuxList.size(); i++)
				{
					streamdemultiplexer = (StreamDemultiplexer) demuxList.get(i);

					if (streamdemultiplexer.getType() == CommonParsing.MPEG_VIDEO)
					{ 
						/**
						 * accept only first video
						 */
						if (NumberOfVideostreams > 0)
						{
							Common.setMessage("!> further videostream found (ID 0x" + Integer.toHexString(streamdemultiplexer.getID()).toUpperCase() + ") -> ignored");
							continue;
						}

						/**
						 * d2v project 
						 */
						if (CreateD2vIndex || SplitProjectFile)
							job_processing.getProjectFileD2V().write(job_processing.getProjectFileExportLength(), job_processing.getExportedVideoFrameNumber());

						Common.setMessage("");
						Common.setMessage(Resource.getString("video.msg.summary") + " " + job_processing.getExportedVideoFrameNumber() + "/ " + clv[0] + "/ " + clv[1] + "/ " + clv[2] + "/ " + clv[3] + "/ " + clv[4]);

						vptslog = streamdemultiplexer.closeVideo(job_processing, collection.getOutputDirectory() + collection.getFileSeparator());

						NumberOfVideostreams++;
					}
				} 

				//System.gc();

				int[] stream_number = new int[10]; 

				for (int i = 0, es_streamtype; i < demuxList.size(); i++)
				{
					streamdemultiplexer = (StreamDemultiplexer) demuxList.get(i);
					es_streamtype = streamdemultiplexer.getType();

					if (es_streamtype ==CommonParsing.MPEG_VIDEO) 
						continue;

					String[] values = streamdemultiplexer.close(job_processing, vptslog);

					if (values[0].equals("")) 
					{
						Common.setMessage(Resource.getString("parsePrimaryPES.msg.noexport") + Integer.toHexString(streamdemultiplexer.getID()).toUpperCase() + ")");
						continue;
					}

					String newfile = values[3] + (stream_number[es_streamtype] > 0 ? ("[" + stream_number[es_streamtype] + "]") : "") + "." + values[2];

					Common.renameTo(values[0], newfile);
		
					values[0] = newfile;
					values[3] = vptslog;

					switch (es_streamtype)
					{
					case CommonParsing.AC3_AUDIO:
					case CommonParsing.DTS_AUDIO:
						if (streamdemultiplexer.subID() != 0 && (0xF0 & streamdemultiplexer.subID()) != 0x80) 
							break;

						Common.setMessage("");
						Common.setMessage(Resource.getString("parsePrimaryPES.ac3") + " " + (streamdemultiplexer.subID() != 0 ? ("(SubID 0x" + Integer.toHexString(streamdemultiplexer.subID()).toUpperCase() + ")") : ""));

						processAllAudio(collection, values[0], values[1], values[2], values[3]);
						break;

					case CommonParsing.TELETEXT: 
						Common.setMessage("");
						Common.setMessage(Resource.getString("parsePrimaryPES.teletext") + Integer.toHexString(streamdemultiplexer.subID()).toUpperCase() + ")");

						processTeletext(collection, values[0], values[1], values[2], values[3]);
						break;

					case CommonParsing.MPEG_AUDIO: 
						Common.setMessage("");
						Common.setMessage(Resource.getString("parsePrimaryPES.mpeg.audio") + Integer.toHexString(streamdemultiplexer.getID()).toUpperCase() + ")");

						processAllAudio(collection, values[0], values[1], values[2], values[3]);
						break;
	
					case CommonParsing.LPCM_AUDIO:
						Common.setMessage("");
						Common.setMessage(Resource.getString("parsePrimaryPES.lpcm.audio") + Integer.toHexString(streamdemultiplexer.subID()).toUpperCase() + ")");

						processLPCM(collection, values[0], values[1], values[2], values[3]);
						break;

					case CommonParsing.SUBPICTURE:
						Common.setMessage("");
						Common.setMessage(Resource.getString("parsePrimaryPES.subpic") + Integer.toHexString(streamdemultiplexer.subID()).toUpperCase() + ")");

						processSubpicture(collection, values[0], values[1], values[2], values[3]);
						break;
					}

					stream_number[es_streamtype]++;

					new File(newfile).delete();
					new File(values[1]).delete();
				}
			}

		} catch (IOException e2) { 

			Common.setExceptionMessage(e2);
		}

		//System.gc();

		return vptslog;
	}



	/**
	 * ts Parser
	 */
	private String parseTS(JobCollection collection, XInputFile xInputFile, int pes_streamtype, int action)
	{
		String fchild = collection.getOutputName(xInputFile.getName());
		String fparent = collection.getOutputNameParent(fchild);

		JobProcessing job_processing = collection.getJobProcessing();

		/**
		 * split part 
		 */
		fparent += job_processing.getSplitSize() > 0 ? "(" + job_processing.getSplitPart() + ")" : "" ;

		String vptslog = "-1";

		boolean Message_1 = Common.getSettings().getBooleanProperty(Keys.KEY_MessagePanel_Msg1);
		boolean Message_2 = Common.getSettings().getBooleanProperty(Keys.KEY_MessagePanel_Msg2);
		boolean Debug = collection.DebugMode();
		boolean JoinPackets = Common.getSettings().getBooleanProperty(Keys.KEY_TS_joinPackets);
		boolean HumaxAdaption = Common.getSettings().getBooleanProperty(Keys.KEY_TS_HumaxAdaption);
		boolean FinepassAdaption = Common.getSettings().getBooleanProperty(Keys.KEY_TS_FinepassAdaption);
		boolean GetEnclosedPackets = Common.getSettings().getBooleanProperty(Keys.KEY_Input_getEnclosedPackets);
		boolean IgnoreScrambledPackets = Common.getSettings().getBooleanProperty(Keys.KEY_TS_ignoreScrambled);
		boolean PcrCounter = Common.getSettings().getBooleanProperty(Keys.KEY_Conversion_PcrCounter);
		boolean BlindSearch = Common.getSettings().getBooleanProperty(Keys.KEY_TS_blindSearch);
		boolean CreateD2vIndex = Common.getSettings().getBooleanProperty(Keys.KEY_ExternPanel_createD2vIndex);
		boolean SplitProjectFile = Common.getSettings().getBooleanProperty(Keys.KEY_ExternPanel_splitProjectFile);
		boolean UseAutoPidFilter = Common.getSettings().getBooleanProperty(Keys.KEY_useAutoPidFilter);

		boolean ts_isIncomplete = false;
		boolean ts_startunit = false;
		boolean ts_hasErrors = false;
		boolean containsPts = false;
		boolean ende = false;
		boolean missing_syncword = false;
		boolean usePidfilter = false;
		boolean isTeletext;
		boolean foundObject;

		int ts_buffersize = 189;
		int ts_packetlength = 188;

		byte[] ts_packet = new byte[ts_buffersize];
		byte[] pes_packet;
		byte[] hav_chunk = { 0x5B, 0x48, 0x4F, 0x4A, 0x49, 0x4E, 0x20, 0x41 }; //'[HOJIN A'

		int Infoscan_Value = Integer.parseInt(Common.getSettings().getProperty(Keys.KEY_ExportPanel_Infoscan_Value));
		int CutMode = Common.getSettings().getIntProperty(Keys.KEY_CutMode);
		int ts_pid;
		int ts_scrambling = 0;
		int ts_adaptionfield = 0;
		int ts_counter = 0;
		int ts_adaptionfieldlength = 0;
		int payload_pesID = 0;
		int payload_psiID = 0;
		int pes_extensionlength = 0;
		int pes_payloadlength = 0;
		int pes_packetlength;
		int pes_packetoffset = 6;
		int pes_headerlength = 9;
		int pes_offset = 0;
		int pes_subID = 0;
		int pes_ID;
		int bytes_read = 0;

		int[] newID = { 0x80, 0x90, 0xC0, 0xE0, 0xA0, 0x20 };

		job_processing.clearStatusVariables();
		int[] clv = job_processing.getStatusVariables();

		long next_CUT_BYTEPOSITION = 0;
		long lastpts = 0;
		long ptsoffset = 0;
		long packet = 0;
		long count = 0;
		long size = 0;
		long base;
		long startPoint = 0;
		long starts[] = new long[collection.getPrimaryInputFileSegments()];
		long qexit;


		StreamBuffer streambuffer = null;
		StreamDemultiplexer streamdemultiplexer = null;
		StreamConverter streamconverter = new StreamConverter();

		ArrayList usedPIDs = new ArrayList();

		List demuxList = job_processing.getTSDemuxList();
		List TSPidlist = job_processing.getTSPidList();

		/**
		 * re-read old streams, for next split part
		 */
		if (job_processing.getSplitPart() == 0)
		{ 
			TSPidlist.clear();
			demuxList.clear();
		}

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
					streamdemultiplexer.initVideo2(fparent);

				else 
					streamdemultiplexer.init2(fparent);
			}
		}

		/**
		 * first split part, or one file only
		 */
		if (job_processing.getSplitPart() == 0)
		{
			StreamInfo streamInfo = xInputFile.getStreamInfo();

			int[] pids = streamInfo.getPIDs();

			if (pids.length > 0)
			{
				Common.setMessage(Resource.getString("parseTS.sid") + Integer.toHexString(pids[0]).toUpperCase());
				Common.setMessage(Resource.getString("parseTS.pmt.refers", Integer.toHexString(pids[1]).toUpperCase()));
				Common.setMessage(Resource.getString("ScanInfo.Video") + "\t" + streamInfo.getVideo());
				Common.setMessage(Resource.getString("ScanInfo.Audio") + "\t" + streamInfo.getAudio());
				Common.setMessage(Resource.getString("ScanInfo.Teletext") + "\t" + streamInfo.getTeletext());
				Common.setMessage(Resource.getString("ScanInfo.Subpicture") + "\t" + streamInfo.getSubpicture());
				Common.setMessage("");

				for (int i = 2; i < pids.length; i++)
				{
					TSPidlist.add(streambuffer = new StreamBuffer());
					streambuffer.setPID(pids[i]);
				}
			}

			else 
				Common.setMessage(Resource.getString("parseTS.no.pmt"));
		}



		/**
		 * init conversions 
		 */
		switch (action)
		{
		case CommonParsing.ACTION_TO_VDR:
			streamconverter.init(fparent + ".vdr", MainBufferSize, action, job_processing.getSplitPart());
			break;

		case CommonParsing.ACTION_TO_M2P:
			streamconverter.init(fparent + ".m2p", MainBufferSize, action, job_processing.getSplitPart());
			break;

		case CommonParsing.ACTION_TO_PVA:
			streamconverter.init(fparent + ".pva", MainBufferSize, action, job_processing.getSplitPart());
			break;

		case CommonParsing.ACTION_TO_TS:
			streamconverter.init(fparent + "(new).ts", MainBufferSize, action, job_processing.getSplitPart());
			break;

		case CommonParsing.ACTION_FILTER:
			streamconverter.init(fparent + "[filtered].ts", MainBufferSize, action, job_processing.getSplitPart());
		}

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
		int[] predefined_Pids = UseAutoPidFilter ? xInputFile.getStreamInfo().getMediaPIDs() : collection.getPIDsAsInteger();

		int[] include = new int[predefined_Pids.length];

		for (int i = 0; i < include.length; i++) 
			include[i] = 0x1FFF & predefined_Pids[i];

		if (include.length > 0)
		{
			Arrays.sort(include);

			String str = " ";

			for (int i = 0; i < include.length; i++)
				str += "0x" + Integer.toHexString(include[i]).toUpperCase() + " ";

			Common.setMessage(Resource.getString("parseTS.special.pids") + ": {" + str + "}");

			usePidfilter = true;
		}


		try {

			/**
			 * determine start & end byte pos. of each file segment
			 */
			for (int i = 0; i < starts.length; i++)
			{
				xInputFile = (XInputFile) collection.getInputFile(i);
				starts[i] = size;
				size += xInputFile.length();
			}

			xInputFile = (XInputFile) collection.getInputFile(job_processing.getFileNumber());

			/**
			 * set start & end byte pos. of first file segment
			 */
			count = starts[job_processing.getFileNumber()];
			size = count + xInputFile.length();

			/**
			 * split skipping first, for next split part
			 */
			if (job_processing.getSplitSize() > 0)
				startPoint = job_processing.getLastHeaderBytePosition() - (1048576L * (Common.getSettings().getIntProperty(Keys.KEY_ExportPanel_Overlap_Value) + 1));

			List CutpointList = collection.getCutpointList();
			List ChapterpointList = collection.getChapterpointList();

			/**
			 * jump near to first cut-in point to collect more audio
			 */
			if (CutMode == CommonParsing.CUTMODE_BYTE && CutpointList.size() > 0 && CommonParsing.getCutCounter() == 0)
				startPoint = Long.parseLong(CutpointList.get(CommonParsing.getCutCounter()).toString()) - ((action == CommonParsing.ACTION_DEMUX) ? 2048000: 0);

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

			xInputFile = (XInputFile) collection.getInputFile(job_processing.getFileNumber());
			count = starts[job_processing.getFileNumber()];

			if (job_processing.getFileNumber() > 0)
				Common.setMessage(Resource.getString("parseTS.continue") + " " + xInputFile);

			base = count;
			size = count + xInputFile.length();

			PushbackInputStream in = new PushbackInputStream(xInputFile.getInputStream(startPoint - base), 200);

			count += (startPoint - base);

			Common.updateProgressBar((action == CommonParsing.ACTION_DEMUX ? Resource.getString("parseTS.demuxing") : Resource.getString("parseTS.converting")) + " " + Resource.getString("parseTS.dvb.mpeg") + " " + xInputFile.getName(), (count - base), (size - base));

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
							if (count > Long.parseLong(CutpointList.get(CommonParsing.getCutCounter() - 1).toString()) + 2048000)
							{
								ende = true;
								break bigloop;
							}
					}

					/**
					 * regular read
					 */
					if (!ts_isIncomplete || !JoinPackets)
					{
						bytes_read = in.read(ts_packet, 0, ts_buffersize);

						/**
						 * EOF is packet aligned
						 */
						if (bytes_read == ts_packetlength && size - count == bytes_read)
							ts_packet[188] = 0x47;

						else if (bytes_read < ts_buffersize && JoinPackets)
						{
							Common.setMessage(Resource.getString("parseTS.incomplete") + " " + count);
							count += bytes_read;
							break loop;
						}
					}

					/**
					 * humax .vid workaround, skip special data chunk
					 */
					if (HumaxAdaption && ts_packet[0] == 0x7F && ts_packet[1] == 0x41 && ts_packet[2] == 4 && ts_packet[3] == (byte)0xFD)
					{
						in.skip(995);
						count += 1184;
						continue loop;
					}

					/**
					 * finepass .hav workaround, chunks fileposition index (hdd sectors) unused, because a file can be hard-cut anywhere
					 */
					if (FinepassAdaption && ts_packet[0] == 0x47 && ts_packet[188] != 0x47)
					{
						int i = ts_packetlength;
						int j;
						int k = ts_buffersize;
						int l = hav_chunk.length;

						while (i > 0)
						{
							j = 0;

							while (i > 0 && ts_packet[i] != hav_chunk[j])
								i--;

							for ( ; i > 0 && j < l && i + j < k; j++)
								if (ts_packet[i + j] != hav_chunk[j])
									break;

							/**
							 * found at least one byte of chunk
							 */
							if (j > 0)
							{
								/** ident of chunk doesnt match completely */
								if (j < l && i + j < k)
								{
									i--;
									continue;
								}

								in.skip(0x200 - (k - i));
								in.read(ts_packet, i, k - i);

								count += 0x200;

								break;
							}
						}
					}


		 			if (HumaxAdaption && ts_packet[0] == 0x47 && ts_packet[188] == 0x7F)
					{}  // do nothing, take the packet

		 			else if (ts_packet[0] != 0x47 || (GetEnclosedPackets && ts_packet[188] != 0x47) )
					{
						if (Message_2 && !missing_syncword) 
							Common.setMessage(Resource.getString("parseTS.missing.sync") + " " + count);

						if (ts_isIncomplete && JoinPackets)
						{
							Common.setMessage(Resource.getString("parseTS.comp.failed"));

							in.unread(ts_packet, 190 - bytes_read, bytes_read - 1);

							ts_isIncomplete = false;

							count++;
						}

						else
						{
							int i = 1;

							while (i < ts_buffersize)
							{
								if (ts_packet[i] == 0x47)
									break;

								i++;
							}
							
						//	in.unread(ts_packet, 1, ts_packetlength);
							in.unread(ts_packet, i, ts_buffersize - i);

							count += i;
						}

						missing_syncword = true;

						continue loop;
					}

					else if (ts_isIncomplete && JoinPackets)
						Common.setMessage(Resource.getString("parseTS.comp.ok"));


					if (Message_2 && missing_syncword)
						Common.setMessage(Resource.getString("parseTS.found.sync") + " " + count);

					missing_syncword = false;

					in.unread(ts_packet, ts_packetlength, 1);

					/**
					 * mark for split and cut
					 */
					job_processing.setLastHeaderBytePosition(count);
					next_CUT_BYTEPOSITION = count;


					if (ts_isIncomplete && JoinPackets)
					{
						count += (bytes_read - 1);
						ts_isIncomplete = false;
					}

					else
						count += ts_packetlength;


					packet++;

					ts_hasErrors  = (0x80 & ts_packet[1]) != 0;							// TS error indicator
					ts_startunit  = (0x40 & ts_packet[1]) != 0; 							// new PES packet start
					ts_pid        = (0x1F & ts_packet[1])<<8 | (0xFF & ts_packet[2]);    // the PID
					ts_scrambling = (0xC0 & ts_packet[3])>>>6;                       	// packet is scrambled (>0)
					ts_adaptionfield = (0x30 & ts_packet[3])>>>4;                    		// has adaption field ?
					ts_counter    = (0x0F & ts_packet[3]);                          		// packet counter 0..f
					ts_adaptionfieldlength = ts_adaptionfield > 1 ? (0xFF & ts_packet[4]) + 1 : 0;  		// adaption field length

					Common.updateProgressBar((count - base), (size - base));

					//yield();

					/** 
					 * pid inclusion 
					 */
					if (usePidfilter && Arrays.binarySearch(include, ts_pid) < 0)
						continue loop;

					/**
					 * raw pid filter extraction
					 */
					if (action == CommonParsing.ACTION_FILTER)
					{
						streamconverter.writePacket(job_processing, ts_packet, 0, ts_packetlength);
						continue loop;
					}

					/**
					 * 00 = reserved value
					 */
					if ((ts_adaptionfield & 1) == 0)
						continue loop;

					if (ts_adaptionfieldlength > 183 || (ts_adaptionfieldlength > 180 && ts_startunit))
						ts_hasErrors = true;

					if (ts_hasErrors)
					{
						if (Message_1)
							Common.setMessage(Resource.getString("parseTS.bit.error", Integer.toHexString(ts_pid).toUpperCase(), "" + packet, "" + (count-188)));

						continue loop;
					}

					payload_pesID = ts_startunit ? CommonParsing.getIntValue(ts_packet, 4 + ts_adaptionfieldlength, 4, !CommonParsing.BYTEREORDERING) : 0;
					payload_psiID = ts_startunit ? payload_pesID>>>16 : 0;

					foundObject = false;

					/**
					 * find PID object
					 */
					for (int i = 0; i < TSPidlist.size(); i++)
					{      
						streambuffer = (StreamBuffer)TSPidlist.get(i);

						foundObject = ts_pid == streambuffer.getPID();

						if (foundObject)
							break; 
					}

					/**
					 * create new PID object
					 */
					if (!foundObject)
					{
						TSPidlist.add(streambuffer = new StreamBuffer());
						streambuffer.setPID(ts_pid);

						/**
						 * padding packet
						 */
						if (ts_pid == 0x1FFF)
						{
							Common.setMessage(Resource.getString("parseTS.stuffing"));
							streambuffer.setneeded(false);
						}
					}

					if (Debug) 
						System.out.println("pk " + packet + " /pid " + Integer.toHexString(ts_pid) + " /pes " + Integer.toHexString(payload_pesID) + " /tn " + streambuffer.isneeded() + " /er " + ts_hasErrors + " /st " + ts_startunit + " /sc " + ts_scrambling + " /ad " + ts_adaptionfield + " /al " + ts_adaptionfieldlength);

					/**
					 * PID not of interest
					 */
					if (!streambuffer.isneeded()) 
						continue loop;


					if (IgnoreScrambledPackets)
					{
						// cannot work with scrambled data
						if (ts_scrambling > 0)
						{
							if (!streambuffer.getScram())
							{
								streambuffer.setScram(true);
								Common.setMessage(Resource.getString("parseTS.scrambled", Integer.toHexString(ts_pid).toUpperCase(), String.valueOf(packet), String.valueOf(count - 188)));
							}
							continue loop;
						}

						else
						{
							if (streambuffer.getScram())
							{
								streambuffer.setScram(false);
								Common.setMessage(Resource.getString("parseTS.clear", Integer.toHexString(ts_pid).toUpperCase(), String.valueOf(packet), String.valueOf(count - 188)));
							}
						}
					}

					/**
					 * out of sequence? 
					 * no payload == no counter++
					 */
					if (Message_1 && (PcrCounter || (!PcrCounter && (1 & ts_adaptionfield) != 0)))
					{
						if (streambuffer.getCounter() != -1)
						{
							if (streambuffer.isStarted() && ts_counter != streambuffer.getCounter())
							{
								Common.setMessage(Resource.getString("parseTS.outof.sequence", Integer.toHexString(ts_pid).toUpperCase(), String.valueOf(packet), String.valueOf(count - 188), String.valueOf(ts_counter), String.valueOf(streambuffer.getCounter())) + " (~" + Common.formatTime_1( (long)((CommonParsing.getVideoFramerate() / 90.0f) * job_processing.getExportedVideoFrameNumber())) + ")");
								streambuffer.setCounter(ts_counter);
							}

							streambuffer.count();
						}

						else
						{ 
							streambuffer.setCounter(ts_counter);
							streambuffer.count();
						}
					}

					/**
					 * buffering of subsequent packets
					 */
					if (!ts_startunit)
					{
						if (streambuffer.isneeded() && streambuffer.isStarted())
							streambuffer.writeData(ts_packet, 4 + ts_adaptionfieldlength, 184 - ts_adaptionfieldlength);
					}

					else
					{
						isTeletext = false;
						pes_subID = 0;

						if (streambuffer.getID() == -1 && payload_pesID == 0x1BD) 
						{
							pes_extensionlength = 0;
							pes_offset = 0;

							try {
								pes_extensionlength = 0xFF & ts_packet[12 + ts_adaptionfieldlength];
								pes_offset = 13 + ts_adaptionfieldlength + pes_extensionlength;
								isTeletext = (pes_extensionlength == 0x24 && (0xFF & ts_packet[pes_offset])>>>4 == 1);

								if (!isTeletext)
									pes_subID = ((0xFF & ts_packet[pes_offset]) == 0x20 && (0xFF & ts_packet[pes_offset + 1]) == 0 && (0xFF & ts_packet[pes_offset + 2]) == 0xF) ? 0x20 : 0;

							} catch (ArrayIndexOutOfBoundsException e) {

								Common.setMessage(Resource.getString("parseTS.io.error") + " / " + pes_extensionlength + " / " + pes_offset);
								Common.setExceptionMessage(e);

								streambuffer.reset();
								streambuffer.setStarted(false);

								continue loop;
							}
						}

						streambuffer.setStarted(true);

						/**
						 * create new streamdemultiplexer object
						 */
						if (streambuffer.getID() == -1)
						{
							streambuffer.setID(payload_pesID);
							String type = "";

							switch (0xFFFFFFF0 & payload_pesID)
							{
							case 0x1E0:
								type = Resource.getString("idtype.mpeg.video");

								streambuffer.setDemux(demuxList.size());

								streamdemultiplexer = new StreamDemultiplexer();
								streamdemultiplexer.setPID(ts_pid);
								streamdemultiplexer.setID(payload_pesID);
								streamdemultiplexer.setnewID(newID[CommonParsing.MPEG_VIDEO]++);
								streamdemultiplexer.setsubID(0);
								streamdemultiplexer.setType(CommonParsing.MPEG_VIDEO);
								streamdemultiplexer.setStreamType(pes_streamtype);

								demuxList.add(streamdemultiplexer);

								if (action == CommonParsing.ACTION_DEMUX)
								{
									if (newID[CommonParsing.MPEG_VIDEO] - 1 == 0xE0)
										streamdemultiplexer.initVideo(fparent, MainBufferSize / demuxList.size(), demuxList.size(), 2);

									else
									{
										type += Resource.getString("idtype.ignored");
										streambuffer.setneeded(false); 
									}
								}

								else 
									type += " " + Resource.getString("idtype.mapped.to") + Integer.toHexString(newID[CommonParsing.MPEG_VIDEO] - 1).toUpperCase();

								break;

							case 0x1C0:
							case 0x1D0:
								type = Resource.getString("idtype.mpeg.audio");

								streambuffer.setDemux(demuxList.size());

								streamdemultiplexer = new StreamDemultiplexer();
								streamdemultiplexer.setPID(ts_pid);
								streamdemultiplexer.setID(payload_pesID);
								streamdemultiplexer.setnewID(newID[CommonParsing.MPEG_AUDIO]++);
								streamdemultiplexer.setsubID(0);
								streamdemultiplexer.setType(CommonParsing.MPEG_AUDIO);
								streamdemultiplexer.setStreamType(pes_streamtype);

								demuxList.add(streamdemultiplexer);

								if (action == CommonParsing.ACTION_DEMUX) 
									streamdemultiplexer.init(fparent, MainBufferSize / demuxList.size(), demuxList.size(), 2);

								else
									type += " " + Resource.getString("idtype.mapped.to") + Integer.toHexString(newID[CommonParsing.MPEG_AUDIO] - 1).toUpperCase();

								break;
							}

							switch (payload_pesID)
							{
							case 0x1BD: 
								type = Resource.getString("idtype.private.stream");
								type += (isTeletext ? " (TTX) ": "") + (pes_subID != 0 ? " (SubID 0x" + Integer.toHexString(pes_subID).toUpperCase() + ")" : ""); 

								streambuffer.setDemux(demuxList.size());

								streamdemultiplexer = new StreamDemultiplexer();
								streamdemultiplexer.setPID(ts_pid);
								streamdemultiplexer.setID(payload_pesID);
								streamdemultiplexer.setsubID(pes_subID);
								streamdemultiplexer.setTTX(isTeletext);

								if (isTeletext)
								{
									streamdemultiplexer.setnewID(newID[CommonParsing.TELETEXT]++);
									streamdemultiplexer.setType(CommonParsing.TELETEXT);
								}

								else if (pes_subID == 0x20)
								{
									streamdemultiplexer.setnewID(newID[CommonParsing.SUBPICTURE]++);
									streamdemultiplexer.setType(CommonParsing.SUBPICTURE);
								}

								else
								{
									streamdemultiplexer.setnewID(newID[CommonParsing.AC3_AUDIO]++);
									streamdemultiplexer.setType(CommonParsing.AC3_AUDIO);
								}

								streamdemultiplexer.setStreamType(pes_subID == 0x20 ? CommonParsing.MPEG2PS_TYPE : CommonParsing.PES_AV_TYPE);
								demuxList.add(streamdemultiplexer);

								if (action == CommonParsing.ACTION_DEMUX) 
									streamdemultiplexer.init(fparent, MainBufferSize/demuxList.size(), demuxList.size(), 2);

								if (action != CommonParsing.ACTION_DEMUX && pes_subID != 0) 
								{
									type += Resource.getString("idtype.ignored");
									streambuffer.setneeded(false);
								}

								if (action != CommonParsing.ACTION_DEMUX && !isTeletext) 
									type += " " + Resource.getString("idtype.mapped.to") + Integer.toHexString(newID[CommonParsing.AC3_AUDIO] - 1).toUpperCase();

								break;

							case 0x1BF:
								Common.setMessage(Resource.getString("parseTS.priv.stream2.ignored", Integer.toHexString(ts_pid).toUpperCase()));

								break;
							}


							if (type.equals(""))
							{
								if (ts_pid == 0 && payload_psiID == 0)
									type = "(PAT)";

								else if (ts_pid == 1 && payload_psiID == 1)
									type = "(CAT)"; 

								else if (ts_pid == 2 && payload_psiID == 3)
									type = "(TSDT)"; 

								else if (ts_pid == 0x10 && (payload_psiID == 6 || payload_psiID == 0x40 || payload_psiID == 0x41))
									type = "(NIT)"; 

								else if (ts_pid == 0x11 && (payload_psiID == 0x42 || payload_psiID == 0x46))
									type = "(SDT)"; 

								else if (ts_pid == 0x11 && payload_psiID == 0x4A)
									type = "(BAT)"; 

								else if (ts_pid == 0x12 && payload_psiID >= 0x4E && payload_psiID <= 0x6F)
									type = "(EIT)"; 

								else if (ts_pid == 0x13 && payload_psiID == 0x71)
									type = "(RST)"; 

								else if (ts_pid == 0x1F && payload_psiID == 0x7F)
									type = "(SIT)"; 

								else if (ts_pid == 0x1E && payload_psiID == 0x7E)
									type = "(DIT)"; 

								else if (ts_pid == 0x14 && payload_psiID == 0x70)
									type = "(TDS)"; 

								else if (ts_pid == 0x14 && payload_psiID == 0x73)
									type = "(TOT)"; 

								else if (payload_psiID == 0x72 && ts_pid >= 0x10 && ts_pid <= 0x14)
									type = "(ST)"; 

								else
								{
									switch (payload_psiID)
									{
									case 2: 
										type = "(PMT)"; 
										break;

									case 4: 
										type = "(PSI)"; 
										break;

									case 0x82: 
										type = "(EMM)"; 
										break;

									case 0x80:
									case 0x81:
									case 0x83:
									case 0x84: 
										type = "(ECM)"; 
										break;

									case 0x43: 
									case 0x44: 
									case 0x45: 
									case 0x47: 
									case 0x48: 
									case 0x49: 
									case 0x4B: 
									case 0x4C: 
									case 0x4D: 
									case 0xFF: 
										type = "(res.)"; 
										break;

									default:
										if ((payload_psiID >= 4 && payload_psiID <= 3F) || (payload_psiID >= 0x74 && payload_psiID <= 0x7D))
										{
											type = "(res.)"; 
											break;
										}

										if (payload_psiID >= 0x80 && payload_psiID < 0xFF)
										{
											type = "(user def. 0x" + Integer.toHexString(payload_psiID).toUpperCase() + ")"; 
											break;
										}
	
										type += "(payload: ";

										for (int j = 0; j < 8; j++)
										{
											String val = Integer.toHexString((0xFF & ts_packet[4 + ts_adaptionfieldlength + j])).toUpperCase();
											type += " " + (val.length() < 2 ? ("0" + val) : val);
										}

										type += " ..)";
									}
								}

								if (ts_scrambling > 0 && !IgnoreScrambledPackets)
								{
									type += " (0x" + Long.toHexString(count - 188).toUpperCase() + " #" + packet + ") ";  // pos + packno

									if (!streambuffer.getScram()) 
										Common.setMessage(Resource.getString("parseTS.scrambled.notignored", Integer.toHexString(ts_pid).toUpperCase(), type));

									streambuffer.setScram(true);
									streambuffer.setStarted(false);
									streambuffer.setID(-1);
									streambuffer.reset();

									continue loop;
								}

								type += " (" + (count - 188) + " #" + packet + ") ";  // pos + packno
								Common.setMessage("--> PID 0x" + Integer.toHexString(ts_pid).toUpperCase() + " " + type + Resource.getString("parseTS.ignored"));

								if (!BlindSearch || type.indexOf("pay") == -1)
									streambuffer.setneeded(false);

								else
									streambuffer.setID(-1);

								continue loop;
							}

							else
							{
								type += " (" + (count - 188) + " #" + packet + ") ";  // pos + packno
								Common.setMessage(Resource.getString("parseTS.pid.has.pes", Integer.toHexString(ts_pid).toUpperCase(), Integer.toHexString(0xFF & payload_pesID).toUpperCase(), type));
								usedPIDs.add("0x" + Integer.toHexString(ts_pid));
							}
						}

						if (streambuffer.getDemux() == -1 || !streambuffer.isneeded())
							continue loop;


						streamdemultiplexer = (StreamDemultiplexer) demuxList.get(streambuffer.getDemux());

						/**
						 * pes_packet completed
						 */
						if (streamdemultiplexer.StreamEnabled())
						{
							pes_packet = streambuffer.getData().toByteArray();

							if (pes_packet.length < 6)
							{
								if (streamdemultiplexer.getPackCount() != -1) 
									Common.setMessage(Resource.getString("parseTS.lackof.pes", Integer.toHexString(ts_pid).toUpperCase()));
							}

							else if (CommonParsing.validateStartcode(pes_packet, 0) < 0)
								Common.setMessage("!> PID 0x" + Integer.toHexString(ts_pid).toUpperCase() + " - invalid start_code of buffered packet..");

							else
							{
								pes_payloadlength = CommonParsing.getPES_LengthField(pes_packet, 0);
								pes_packetlength = pes_packetoffset + pes_payloadlength;
								pes_ID = streamdemultiplexer.getID();

								/**
								 * non video packet size usually < 0xFFFF
								 */
								if (streamdemultiplexer.getType() != CommonParsing.MPEG_VIDEO)
								{
									if (action == CommonParsing.ACTION_DEMUX)
											streamdemultiplexer.write(job_processing, pes_packet, 0, pes_packetlength, true);

									else
										streamconverter.write(job_processing, pes_packet, streamdemultiplexer, next_CUT_BYTEPOSITION, CommonParsing.isInfoScan(), CutpointList);
								}

								/**
								 * special handling, video packet is possibly greater than 0xffff max. size 
								 */
								else
								{
									pes_packetlength = action == CommonParsing.ACTION_DEMUX ? 0xFFFC : 0x1800;

									for (int i = 0, j, pes_remaininglength = pes_packetlength, flags = (0xF3 & pes_packet[6])<<16; i < pes_packet.length; i += pes_remaininglength)
									{
										if (pes_packet.length - i < pes_remaininglength)
											pes_remaininglength = pes_packet.length - i;

										if (i == 0)
										{
											CommonParsing.setPES_LengthField(pes_packet, i, pes_remaininglength - pes_packetoffset);

											if (action == CommonParsing.ACTION_DEMUX)
											{
												streamdemultiplexer.writeVideo(job_processing, pes_packet, i, pes_remaininglength, true, CutpointList, ChapterpointList);
												job_processing.setCutByteposition(next_CUT_BYTEPOSITION);
											}

											else
												streamconverter.write(job_processing, pes_packet, i, streamdemultiplexer, next_CUT_BYTEPOSITION, CommonParsing.isInfoScan(), CutpointList);
										}

										else
										{
											j = i - pes_headerlength;

											CommonParsing.setValue(pes_packet, j, 4, !CommonParsing.BYTEREORDERING, 0x100 | pes_ID);
											CommonParsing.setPES_LengthField(pes_packet, j, pes_remaininglength + 3);
											CommonParsing.setValue(pes_packet, j + pes_packetoffset, 3, !CommonParsing.BYTEREORDERING, flags);

											if (action == CommonParsing.ACTION_DEMUX)
											{
												streamdemultiplexer.writeVideo(job_processing, pes_packet, j, pes_remaininglength + 3, true, CutpointList, ChapterpointList);
												job_processing.setCutByteposition(next_CUT_BYTEPOSITION);
											}

											else
												streamconverter.write(job_processing, pes_packet, j, streamdemultiplexer, next_CUT_BYTEPOSITION, CommonParsing.isInfoScan(), CutpointList);
										}
									}
								}
							}
						}

						pes_packet = null;
						streambuffer.reset();

						/**
						 * buffer actual packet data
						 */
						streambuffer.writeData(ts_packet, 4 + ts_adaptionfieldlength, 184 - ts_adaptionfieldlength);
					}

					clv[5]++;

					if (action != CommonParsing.ACTION_DEMUX) 
						job_processing.setLastHeaderBytePosition(count);

					/**
					 * split size reached 
					 */
					if (job_processing.getSplitSize() > 0 && job_processing.getSplitSize() < job_processing.getAllMediaFilesExportLength()) 
						break loop;
				}

				/**
				 * split size reached 
				 */
				if (job_processing.getSplitSize() > 0 && job_processing.getSplitSize() < job_processing.getAllMediaFilesExportLength()) 
					break bigloop;

				/**
				 * more files 
				 */
				if (job_processing.getFileNumber() < collection.getPrimaryInputFileSegments() - 1)
				{ 
					in.close();
					//System.gc();

					XInputFile nextXInputFile = (XInputFile) collection.getInputFile(job_processing.countFileNumber(+1));
					count = size;

					in = new PushbackInputStream(nextXInputFile.getInputStream(), 200);

					size += nextXInputFile.length();
					base = count;

				//	job_processing.addCellTime(String.valueOf(job_processing.getExportedVideoFrameNumber()));

					Common.setMessage(Resource.getString("parseTS.actual.vframes") + " " + job_processing.getExportedVideoFrameNumber());
					Common.setMessage(Resource.getString("parseTS.switch.to") + " " + nextXInputFile + " (" + Common.formatNumber(nextXInputFile.length()) + " bytes)");

					Common.updateProgressBar((action == CommonParsing.ACTION_DEMUX ? Resource.getString("parseTS.demuxing") : Resource.getString("parseTS.converting")) + " " + Resource.getString("parseTS.dvb.mpeg") + " " + nextXInputFile.getName());

					if (JoinPackets && bytes_read < 188 && nextXInputFile.length() >= 189 - bytes_read)
					{
						ts_isIncomplete = true;
						bytes_read = in.read(ts_packet, bytes_read, 189 - bytes_read);

						Common.setMessage(Resource.getString("parseTS.tryto.complete"));
					}
				}

				else 
					break bigloop;
			}

			Common.setMessage(Resource.getString("parseTS.packs", String.valueOf(clv[5]), String.valueOf(count * 100 / size), String.valueOf(count)));
	
			/**
			 * file end reached for split 
			 */
			if ( (count >= size || ende) && job_processing.getSplitSize() > 0 ) 
				job_processing.setSplitLoopActive(false);

			in.close(); 


			if (action != CommonParsing.ACTION_DEMUX) 
				streamconverter.close(job_processing, CommonParsing.isInfoScan());

			else
			{
				for (int i = 0, NumberOfVideostreams = 0; i < demuxList.size(); i++)
				{
					streamdemultiplexer = (StreamDemultiplexer) demuxList.get(i);

					if (streamdemultiplexer.getType() == CommonParsing.MPEG_VIDEO)
					{ 
						/**
						 * accept only first video
						 */
						if (NumberOfVideostreams > 0)
						{
							Common.setMessage("!> further videostream found (PID 0x" + Integer.toHexString(streamdemultiplexer.getPID()).toUpperCase() + ") -> ignored");
							continue;
						}

						/**
						 * d2v project 
						 */
						if (CreateD2vIndex || SplitProjectFile)
							job_processing.getProjectFileD2V().write(job_processing.getProjectFileExportLength(), job_processing.getExportedVideoFrameNumber());

						Common.setMessage("");
						Common.setMessage(Resource.getString("video.msg.summary") + " " + job_processing.getExportedVideoFrameNumber() + "/ " + clv[0] + "/ " + clv[1] + "/ " + clv[2] + "/ " + clv[3] + "/ " + clv[4]);

						vptslog = streamdemultiplexer.closeVideo(job_processing, collection.getOutputDirectory() + collection.getFileSeparator());

						NumberOfVideostreams++;
					}
				} 

				//System.gc();

				int[] stream_number = new int[10];

				for (int i = 0, es_streamtype; i < demuxList.size(); i++)
				{
					streamdemultiplexer = (StreamDemultiplexer) demuxList.get(i);
					es_streamtype = streamdemultiplexer.getType();

					if (es_streamtype == CommonParsing.MPEG_VIDEO) 
						continue;

					String[] values = streamdemultiplexer.close(job_processing, vptslog);

					if (values[0].equals("")) 
					{
						Common.setMessage(Resource.getString("parseTS.msg.noexport", Integer.toHexString(0xFF & streamdemultiplexer.getID()).toUpperCase(), Integer.toHexString(streamdemultiplexer.getPID()).toUpperCase()));
						continue;
					}

					String newfile = values[3] + (stream_number[es_streamtype] > 0 ? ("[" + stream_number[es_streamtype] + "]") : "") + "." + values[2];

					Common.renameTo(values[0], newfile);

					values[0] = newfile;
					values[3] = vptslog;

					switch (es_streamtype)
					{
					case CommonParsing.AC3_AUDIO:
					case CommonParsing.DTS_AUDIO:
						Common.setMessage("");
						Common.setMessage(Resource.getString("parseTS.ac3.audio") + Integer.toHexString(streamdemultiplexer.getPID()).toUpperCase());

						processAllAudio(collection, values[0], values[1], values[2], values[3]);
						break;

					case CommonParsing.TELETEXT:
						Common.setMessage("");
						Common.setMessage(Resource.getString("parseTS.teletext.onpid") + Integer.toHexString(streamdemultiplexer.getPID()).toUpperCase()+" (SubID 0x"+Integer.toHexString(streamdemultiplexer.subID()).toUpperCase() + ")");

						processTeletext(collection, values[0], values[1], values[2], values[3]);
						break;

					case CommonParsing.MPEG_AUDIO: 
						Common.setMessage("");
						Common.setMessage(Resource.getString("parseTS.mpeg.audio", Integer.toHexString(0xFF & streamdemultiplexer.getID()).toUpperCase(), Integer.toHexString(streamdemultiplexer.getPID()).toUpperCase()));

						processAllAudio(collection, values[0], values[1], values[2], values[3]);
						break;

					case CommonParsing.LPCM_AUDIO:
						Common.setMessage("");
						Common.setMessage(Resource.getString("parseTS.lpcm.audio") + Integer.toHexString(streamdemultiplexer.subID()).toUpperCase() + ")");

						processLPCM(collection, values[0], values[1], values[2], values[3]);
						break;

					case CommonParsing.SUBPICTURE:
						Common.setMessage("");
						Common.setMessage(Resource.getString("parseTS.subpicture") + Integer.toHexString(streamdemultiplexer.subID()).toUpperCase() + ")");

						processSubpicture(collection, values[0], values[1], values[2], values[3]);
						break;
					}

					stream_number[es_streamtype]++;

					new File(newfile).delete();
					new File(values[1]).delete();
				}
			}


			/**
			 * on InfoScan load usable PIDs in pidlist, if list was empty 
			 */
			if (CommonParsing.isInfoScan() && collection.getPIDCount() == 0)
			{
				collection.addPID(usedPIDs.toArray());

				Common.setActiveCollection(Common.getProcessedCollection());

				/* update pid list of an opened panel */
				Common.getGuiInterface().updateCollectionPanel(Common.getActiveCollection());
			}

		} catch (IOException e2) { 

			Common.setExceptionMessage(e2);
		}

		return vptslog;
	}



	/**
	 * nextfile PTS check
	 * 
	 * returns new pts offset to append
	 */
	private long nextFilePTS(JobCollection collection, int parser_type, int pes_streamtype, long lastpts, int file_number)
	{
		return nextFilePTS(collection, parser_type, pes_streamtype, lastpts, file_number, 0L);
	}


	/**
	 * nextfile PTS check
	 * 
	 * returns new pts offset to append
	 */
	private long nextFilePTS(JobCollection collection, int parser_type, int pes_streamtype, long lastpts, int file_number, long startPoint)
	{
		JobProcessing job_processing = collection.getJobProcessing();

		byte[] data;

		long pts = 0;

		int position = 0;
		int buffersize = Integer.parseInt(Common.getSettings().getProperty(Keys.KEY_ScanBuffer));
		int pes_ID;

		boolean PVA_Audio = Common.getSettings().getBooleanProperty(Keys.KEY_PVA_Audio);
		boolean containsPts;

		lastpts &= 0xFFFFFFFFL; //ignore bit33 of lastpts

		if (collection.getPrimaryInputFileSegments() > file_number)
		{
			try {

				XInputFile aXinputFile = ((XInputFile) collection.getInputFile(file_number)).getNewInstance();

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

						else if (pva_pid == 2) //mainaudio mpa
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

		if (file_number == 0 && startPoint == 0)
		{  // need global offset?
			pts &= 0xFFFFFFFFL;

			String str = Common.getSettings().getProperty(Keys.KEY_PtsShift_Value);

			if (str.equals("auto"))
			{ 
				long newpts = ((pts / 324000000L) - 1L) * 324000000L;
				Common.setMessage(Resource.getString("nextfile.shift.auto", "" + (newpts / 324000000L)));

				return newpts;
			}

			else if (!str.equals("0"))
			{ 
				Common.setMessage(Resource.getString("nextfile.shift.manual", Common.getSettings().getProperty(Keys.KEY_PtsShift_Value)));

				return ((long)(Double.parseDouble(Common.getSettings().getProperty(Keys.KEY_PtsShift_Value)) * 324000000L));
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
	private String parsePVA(JobCollection collection, XInputFile aPvaXInputFile, int pes_streamtype, int action, String vptslog)
	{
		String fchild = collection.getOutputName(aPvaXInputFile.getName());
		String fparent = collection.getOutputNameParent(fchild);

		JobProcessing job_processing = collection.getJobProcessing();

		/**
		 * split part 
		 */
		fparent += job_processing.getSplitSize() > 0 ? "(" + job_processing.getSplitPart() + ")" : "" ;

		boolean Message_1 = Common.getSettings().getBooleanProperty(Keys.KEY_MessagePanel_Msg1);
		boolean Message_2 = Common.getSettings().getBooleanProperty(Keys.KEY_MessagePanel_Msg2);
		boolean ConformAudioCheck = Common.getSettings().getBooleanProperty(Keys.KEY_PVA_Audio);
		boolean Debug = collection.DebugMode();
		boolean OverlapCheck = Common.getSettings().getBooleanProperty(Keys.KEY_PVA_FileOverlap);
		boolean Concatenate = Common.getSettings().getBooleanProperty(Keys.KEY_Input_concatenateForeignRecords);
		boolean CreateD2vIndex = Common.getSettings().getBooleanProperty(Keys.KEY_ExternPanel_createD2vIndex);
		boolean SplitProjectFile = Common.getSettings().getBooleanProperty(Keys.KEY_ExternPanel_splitProjectFile);

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

		int Infoscan_Value = Integer.parseInt(Common.getSettings().getProperty(Keys.KEY_ExportPanel_Infoscan_Value));
		int CutMode = Common.getSettings().getIntProperty(Keys.KEY_CutMode);
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
		long qexit;

	
		String[] streamtypes = { 
			Resource.getString("parsePVA.streamtype.ac3"),
			Resource.getString("parsePVA.streamtype.ttx"),
			Resource.getString("parsePVA.streamtype.mpeg.audio"),
			Resource.getString("parsePVA.streamtype.mpeg.video")
		};

		RawFile rawfile = null;
		StreamBuffer streambuffer = null;
		StreamDemultiplexer streamdemultiplexer = null;
		StreamConverter streamconverter = new StreamConverter();

		List demuxList = job_processing.getPVADemuxList();
		List PVAPidlist = job_processing.getPVAPidList();

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
			for (int i = 0; i < demuxList.size(); i++)
			{
				streamdemultiplexer = (StreamDemultiplexer) demuxList.get(i);

				if (streamdemultiplexer.getnewID() != 0)
					newID[streamdemultiplexer.getType()]++;

				if (streamdemultiplexer.getNum() == -1) 
					continue;

				if (streamdemultiplexer.getType() == CommonParsing.MPEG_VIDEO) 
					streamdemultiplexer.initVideo2(fparent);

				else 
					streamdemultiplexer.init2(fparent);
			}
		}

		/**
		 * init conversions 
		 */
		switch (action)
		{
		case CommonParsing.ACTION_TO_VDR:
			streamconverter.init(fparent + ".vdr", MainBufferSize, action, job_processing.getSplitPart());
			break;

		case CommonParsing.ACTION_TO_M2P:
			streamconverter.init(fparent + ".m2p", MainBufferSize, action, job_processing.getSplitPart());
			break;

		case CommonParsing.ACTION_TO_PVA:
			streamconverter.init(fparent + "(new).pva", MainBufferSize, action, job_processing.getSplitPart());
			break;

		case CommonParsing.ACTION_TO_TS:
			streamconverter.init(fparent + ".ts", MainBufferSize, action, job_processing.getSplitPart());
			break;

		case CommonParsing.ACTION_FILTER:
			streamconverter.init(fparent + "[filtered].pva", MainBufferSize, action, job_processing.getSplitPart());
		}


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
				aPvaXInputFile = (XInputFile) collection.getInputFile(i);
				starts[i] = size;
				size += aPvaXInputFile.length();
			}

			aPvaXInputFile = (XInputFile) collection.getInputFile(job_processing.getFileNumber());

			/**
			 * set start & end byte pos. of first file segment
			 */
			count = starts[job_processing.getFileNumber()];
			size = count + aPvaXInputFile.length();

			if (CommonParsing.getPvaPidExtraction()) 
				rawfile = new RawFile(fparent, 	CommonParsing.getPvaPidToExtract(), MainBufferSize);

			/**
			 * split skipping first, for next split part
			 */
			if (job_processing.getSplitSize() > 0)
				startPoint = job_processing.getLastHeaderBytePosition() - (1048576L * (Common.getSettings().getIntProperty(Keys.KEY_ExportPanel_Overlap_Value) + 1));

			List CutpointList = collection.getCutpointList();
			List ChapterpointList = collection.getChapterpointList();

			/**
			 * jump near to first cut-in point to collect more audio
			 */
			if (CutMode == CommonParsing.CUTMODE_BYTE && CutpointList.size() > 0 && CommonParsing.getCutCounter() == 0)
				startPoint = Long.parseLong(CutpointList.get(CommonParsing.getCutCounter()).toString()) - ((action == CommonParsing.ACTION_DEMUX) ? 2048000: 0);

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

			aPvaXInputFile = (XInputFile) collection.getInputFile(job_processing.getFileNumber());
			count = starts[job_processing.getFileNumber()];

			if (job_processing.getFileNumber() > 0)
				Common.setMessage(Resource.getString("parsePVA.continue") + " " + aPvaXInputFile);

			base = count;
			size = count + aPvaXInputFile.length();

			PushbackInputStream in = new PushbackInputStream(aPvaXInputFile.getInputStream(startPoint - base), pva_buffersize);

			count += (startPoint - base);

			overlapPVA(collection, overlapnext);

			Common.updateProgressBar((action == CommonParsing.ACTION_DEMUX ? Resource.getString("parsePVA.demuxing") : Resource.getString("parsePVA.converting")) + " " + Resource.getString("parsePVA.pvafile") + " " + aPvaXInputFile.getName(), (count - base), (size - base));

			qexit = count + (0x100000L * Infoscan_Value);

			//yield();

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
							if (count > Long.parseLong(CutpointList.get(CommonParsing.getCutCounter() - 1).toString()) + 2048000)
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

						for (int i = 256; i >= 0; i--)
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

					pva_pid = 0xFF & pva_packet[2];
					pva_counter = 0xFF & pva_packet[3];                             
					ptsflag = 0xFF & pva_packet[5];
					containsPts = (0x10 & ptsflag) != 0;
					pre_bytes = 3 & ptsflag>>>2;
					post_bytes = 3 & ptsflag;
					pva_payloadlength = (0xFF & pva_packet[6])<<8 | (0xFF & pva_packet[7]);

					pva_packetoffset = pva_headerlength;

					Common.updateProgressBar((count - base), (size - base));

					//yield();

					clv[5]++;

					if (Debug) 
						System.out.print("\r" + Resource.getString("parsePVA.packs") + clv[5] + " " + ((count * 100 / size)) + "% " + count);

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
						streamconverter.writePacket(job_processing, pva_packet, 0, pva_packetoffset + pva_payloadlength);

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
						streamdemultiplexer = (StreamDemultiplexer)demuxList.get(streambuffer.getID());

					else
					{   // create new ID object
						String IDtype = "";

						switch (pva_pid)
						{
						case 1:
							IDtype=Resource.getString("idtype.video");
							streamdemultiplexer = new StreamDemultiplexer(ptsoffset);

							streambuffer.setStarted(true);

							streamdemultiplexer.setID(0xE0);
							streamdemultiplexer.setType(CommonParsing.MPEG_VIDEO);
							streamdemultiplexer.setnewID(newID[CommonParsing.MPEG_VIDEO]++);
							streamdemultiplexer.setPID(pva_pid);
							streamdemultiplexer.setsubID(0);
							streamdemultiplexer.setStreamType(pes_streamtype);

							streambuffer.setID(demuxList.size());
							demuxList.add(streamdemultiplexer);

							if (action == CommonParsing.ACTION_DEMUX) 
								streamdemultiplexer.initVideo(fparent, MainBufferSize, demuxList.size(), CommonParsing.PVA_PARSER);
							else 
								IDtype += " " + Resource.getString("idtype.mapped.to.e0") + streamtypes[3];

							break; 

						case 2:
							IDtype=Resource.getString("idtype.main.audio");
							//do not break

						default: 
							IDtype=Resource.getString("idtype.additional"); 

							if (!containsPts) 
								continue loop;

							int streamID = 0xFF & pva_packet[pva_packetoffset + post_bytes + 3];

							if ((0xE0 & streamID) != 0xC0 && streamID != CommonParsing.PRIVATE_STREAM_1_CODE)
							{ 
								streambuffer.setneeded(false); 
								break; 
							}

							streamdemultiplexer = new StreamDemultiplexer(ptsoffset);

							streambuffer.setStarted(true);

							streamdemultiplexer.setPID(pva_pid);
							streamdemultiplexer.setType(streamID != CommonParsing.PRIVATE_STREAM_1_CODE ? CommonParsing.MPEG_AUDIO : CommonParsing.AC3_AUDIO);    // MPA?
							streamdemultiplexer.setID(streamID);
							streamdemultiplexer.setsubID(0);

							isTeletext = (0xFF & pva_packet[pva_packetoffset + post_bytes + 8]) == 0x24;

							streamdemultiplexer.setnewID((streamID == CommonParsing.PRIVATE_STREAM_1_CODE ? (isTeletext ? newID[CommonParsing.TELETEXT]++ : newID[CommonParsing.AC3_AUDIO]++) : newID[CommonParsing.MPEG_AUDIO]++));
							streamdemultiplexer.setTTX(isTeletext);
							streamdemultiplexer.setStreamType(pes_streamtype);

							streambuffer.setID(demuxList.size());
							demuxList.add(streamdemultiplexer);

							IDtype += " " + Resource.getString("idtype.has.pesid") + Integer.toHexString(streamID).toUpperCase() + " " + streamtypes[streamdemultiplexer.getType()];

							if (action == CommonParsing.ACTION_DEMUX) 
								streamdemultiplexer.init(fparent, MainBufferSize / demuxList.size(), demuxList.size(), CommonParsing.PVA_PARSER);
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
					Common.setMessage(Resource.getString("parsePVA.continue") + " " + nextXInputFile + " (" + Common.formatNumber(nextXInputFile.length()) + " bytes)");

					Common.updateProgressBar((action == CommonParsing.ACTION_DEMUX ? Resource.getString("parsePVA.demuxing") : Resource.getString("parsePVA.converting")) + Resource.getString("parsePVA.pvafile") + " " + nextXInputFile.getName());

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

			if (action != CommonParsing.ACTION_DEMUX) 
				streamconverter.close(job_processing, CommonParsing.isInfoScan());

			else
			{
				for (int i = 0, NumberOfVideostreams = 0; i < demuxList.size(); i++)
				{
					streamdemultiplexer = (StreamDemultiplexer)demuxList.get(i);

					if (streamdemultiplexer.getType() == CommonParsing.MPEG_VIDEO)
					{ 
						/**
						 * accept only first video
						 */
						if (NumberOfVideostreams > 0)
						{
							Common.setMessage("!> further videostream found (ID 0x" + Integer.toHexString(streamdemultiplexer.getPID()).toUpperCase() + ") -> ignored");
							continue;
						}

						/**
						 * d2v project 
						 */
						if (CreateD2vIndex || SplitProjectFile)
							job_processing.getProjectFileD2V().write(job_processing.getProjectFileExportLength(), job_processing.getExportedVideoFrameNumber());

						Common.setMessage("");
						Common.setMessage(Resource.getString("video.msg.summary") + " " + job_processing.getExportedVideoFrameNumber() + "/ " + clv[0] + "/ " + clv[1] + "/ " + clv[2] + "/ " + clv[3] + "/ " + clv[4]);

						vptslog = streamdemultiplexer.closeVideo(job_processing, collection.getOutputDirectory() + collection.getFileSeparator());

						NumberOfVideostreams++;
					}
				} 

				//System.gc();

				int[] stream_number = new int[10];

				for (int i = 0, es_streamtype; i < demuxList.size(); i++)
				{
					streamdemultiplexer = (StreamDemultiplexer)demuxList.get(i);
					es_streamtype = streamdemultiplexer.getType();

					if (es_streamtype == CommonParsing.MPEG_VIDEO) 
						continue;

					if (streamdemultiplexer.getID() == 0) 
						continue;

					String[] values = streamdemultiplexer.close(job_processing, vptslog);

					if (values[0].equals(""))
					{
						Common.setMessage(Resource.getString("parsePVA.msg.noexport") + Integer.toHexString(streamdemultiplexer.getPID()).toUpperCase() + " (0x" + Integer.toHexString(streamdemultiplexer.getID()).toUpperCase() + ")");
						continue;
					}

					String newfile = values[3] + ( stream_number[es_streamtype] > 0 ? ("[" + stream_number[es_streamtype] + "]") : "") + "." + values[2];

					Common.renameTo(values[0], newfile);

					values[0] = newfile;
					values[3] = vptslog;

					switch (es_streamtype)
					{
					case CommonParsing.AC3_AUDIO:
					case CommonParsing.DTS_AUDIO:
						if (streamdemultiplexer.subID() != 0 && (0xF0 & streamdemultiplexer.subID()) != 0x80)
							break;

						Common.setMessage("");
						Common.setMessage(Resource.getString("parsePVA.ac3.onid") + Integer.toHexString(streamdemultiplexer.getPID()).toUpperCase() + " " + ((streamdemultiplexer.subID() != 0) ? ("(SubID 0x" + Integer.toHexString(streamdemultiplexer.subID()).toUpperCase() + ")") : ""));

						processAllAudio(collection, values[0], values[1], values[2], values[3]);
						break;

					case CommonParsing.TELETEXT: 
						Common.setMessage("");
						Common.setMessage(Resource.getString("parsePVA.teletext.onid") + Integer.toHexString(streamdemultiplexer.getPID()).toUpperCase() + " (SubID 0x" + Integer.toHexString(streamdemultiplexer.subID()).toUpperCase() + ")");

						processTeletext(collection, values[0], values[1], values[2], values[3]);
						break;

					case CommonParsing.MPEG_AUDIO: 
						Common.setMessage("");
						Common.setMessage(Resource.getString("parsePVA.mpeg.audio.onid") + Integer.toHexString(streamdemultiplexer.getPID()).toUpperCase() + " (0x" + Integer.toHexString(streamdemultiplexer.getID()).toUpperCase() + ")");

						processAllAudio(collection, values[0], values[1], values[2], values[3]);
						break;

					case CommonParsing.LPCM_AUDIO:
						Common.setMessage("");
						Common.setMessage(Resource.getString("parsePVA.lpcm.onid") + Integer.toHexString(streamdemultiplexer.getPID()).toUpperCase() + " (SubID 0x" + Integer.toHexString(streamdemultiplexer.subID()).toUpperCase() + ")");

						processLPCM(collection, values[0], values[1], values[2], values[3]);
						break;

					case CommonParsing.SUBPICTURE:
						Common.setMessage("");
						Common.setMessage(Resource.getString("parsePVA.subpicture.onid") + Integer.toHexString(streamdemultiplexer.getPID()).toUpperCase() + " (SubID 0x" + Integer.toHexString(streamdemultiplexer.subID()).toUpperCase() + ")");

						processSubpicture(collection, values[0], values[1], values[2], values[3]);
						break;
					}

					stream_number[es_streamtype]++;

					new File(newfile).delete();
					new File(values[1]).delete();
				}
			}

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

		//yield();

		return vptslog;
	}



	/**
	 * synccheck A/V
	 */
	private boolean SyncCheck(int[] vw, double timecount, double timelength, long timeline, int mpf, long[] vptsval, long[] vtime, boolean awrite, boolean debug)
	{
		int v = vw[0];
		int w = vw[1];

		if (w < vptsval.length)
		{
			double ms1 = (double)(timeline - vptsval[w + 1]);
			double ms2 = (double)(timecount - vtime[w + 1]);

			if (debug) 
				System.out.println("A " + awrite + "/" + v + "/" + w + "/ =1 " + mpf + "/" + vtime[w + 1] + "/" + timecount + " ~2 " + vptsval[w + 1] + "/" + timeline + " ~3 " + ms1 + "/" + ms2 + "/" + (ms2 - ms1));

			if ( (double)Math.abs(ms2) <= timelength / 2.0 )
			{
				awrite = false;
				w += 2;
			}

			else if ( (double)Math.abs(ms1) <= timelength / 2.0 )
			{
				awrite = false;
				w += 2;
			}

			if (debug) 
				System.out.println("B " + awrite + "/" + v + "/" + w);
		}

		if (v < vptsval.length)
		{
			boolean show = false;
			double ms3 = (double)(timeline - vptsval[v]);
			double ms4 = (double)(timecount - vtime[v]);

			if (debug) 
				System.out.println("C " + awrite + "/" + v + "/" + w + "/ =4 " + mpf + "/" + vtime[v] + "/" + timecount + " ~5 " + vptsval[v] + "/" + timeline + " ~6 " + ms3 + "/" + ms4 + "/" + (ms4 - ms3));

			if (!awrite && (double)Math.abs(ms3) <= timelength / 2.0 )
			{
				awrite = true; 
				show = true;
				v += 2;
			}

			else if (!awrite && (double)Math.abs( (double)Math.abs(ms4) - (double)Math.abs(ms3) ) <= timelength / 2.0 )
			{
				awrite = true; 
				show = true;
				v += 2;
			}

			if (debug)
				System.out.println("D " + awrite + "/" + v + "/" + w);

			if (v < vptsval.length && awrite && (timecount + (timelength / 2.0)) > vtime[v] ) 
				awrite = false;

			if (debug) 
				System.out.println("E " + awrite + "/" + v + "/" + w);

			if (show && awrite) 
				Common.getGuiInterface().showAVOffset("" + (int)(ms3 / 90) + "/" + (int)(ms4 / 90) + "/" + (int)((ms4 - ms3) / 90));
		}

		vw[0] = v;
		vw[1] = w;

		return awrite;
	}


	/**
	 * start method for adjusting audio at timeline
	 */
	private void processAllAudio(JobCollection collection, String filename, String filename_pts, String filename_type, String videofile_pts)
	{
		processAllAudio(collection, filename, filename_pts, filename_type, videofile_pts, 0);
	}

	/**
	 * start method for adjusting audio at timeline
	 */
	private void processAllAudio(JobCollection collection, String filename, String filename_pts, String filename_type, String videofile_pts, int isElementaryStream)
	{
		processAllAudio(collection, new XInputFile(new File(filename)), filename_pts, filename_type, videofile_pts, 0);
	}

	/**
	 * start method for adjusting audio at timeline
	 */
	private void processAllAudio(JobCollection collection, XInputFile xInputFile, String filename_pts, String filename_type, String videofile_pts, int isElementaryStream)
	{
		Common.updateProgressBar(Resource.getString("audio.progress") + "  " + xInputFile.getName(), 0, 0);

		//yield();

		if (audio == null)
			audio = new Audio();

		// audio is global
		audio.initRDSDecoding(Common.getSettings().getBooleanProperty(Keys.KEY_MessagePanel_Msg7), collection.DebugMode());

		if (MPAConverter == null)
			MPAConverter = new MpaConverter();

		if (MPADecoder == null)
			MPADecoder = new MpaDecoder();

		MpaDecoder.RESET = false;

		MpaDecoder.MAX_VALUE = Common.getSettings().getBooleanProperty(Keys.KEY_AudioPanel_Normalize) ? (Integer.parseInt(Common.getSettings().getProperty(Keys.KEY_AudioPanel_NormalizeValue)) * 32767 / 100) : 32767;
		MpaDecoder.MULTIPLY = Common.getSettings().getBooleanProperty(Keys.KEY_AudioPanel_Normalize) ? 32767 : 1;
		MpaDecoder.NORMALIZE = Common.getSettings().getBooleanProperty(Keys.KEY_AudioPanel_Normalize);

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

		if (Common.getSettings().getBooleanProperty(Keys.KEY_AudioPanel_decodeMpgAudio))
		{
			Common.setMessage(Resource.getString("audio.decode"));
			Common.setMessage("\t" + Keys.ITEMS_resampleAudioMode[Common.getSettings().getIntProperty(Keys.KEY_AudioPanel_resampleAudioMode)]);

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

			//yield();

			if ( (0x10000L & CommonParsing.getAudioProcessingFlags()) != 0) 
				MpaConversionMode = 0;
		}

		CommonParsing.setAudioProcessingFlags(CommonParsing.getAudioProcessingFlags() & 3L);
	}


	/**
	 *  method for audio processing 
 	 */
	private boolean processAudio(JobCollection collection, XInputFile xInputFile, String filename_pts, String filename_type, String videofile_pts, int isElementaryStream, int MpaConversionMode)
	{
	//	String fchild = collection.getOutputName(xInputFile.getName());
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
		int es_streamtype = CommonParsing.MPEG_AUDIO; //"mp" std

		final int AC3_AUDIOSTREAM = 0;
		final int MP3_AUDIOSTREAM = 1;
		final int MP2_AUDIOSTREAM = 2;
		final int MP1_AUDIOSTREAM = 3;
		final int DTS_AUDIOSTREAM = 4;
		final int WAV_AUDIOSTREAM = 5;
		final int NO_AUDIOSTREAM = 10;

		double time_counter = 0.0;

		IDDBufferedOutputStream audiooutL;
		IDDBufferedOutputStream audiooutR;

		if (filename_type.equals("ac")) //means dts, too
			es_streamtype = CommonParsing.AC3_AUDIO;

		else if (filename_type.equals("wa"))
			es_streamtype = CommonParsing.LPCM_AUDIO;


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

			//yield();


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


			bigloop:
			while (true)
			{
				/**
				 *  AC-3/DTS Audio
				 */
				readloopdd:
				while (es_streamtype == CommonParsing.AC3_AUDIO && n < audiosize - 10)
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
					ERRORCODE = (is_AC3 || !is_DTS) ? audio.AC3_parseHeader(pushback, 0) : 0; 

					if (ERRORCODE < 1)
					{ 
						if (!is_AC3 || is_DTS)
							ERRORCODE = audio.DTS_parseHeader(pushback, 0); 

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
					}
					else
					{ 
						is_DTS = false;
						is_AC3 = true;
					}

					/**
					 * prepare fo read entire frame 
					 */
					audioin.unread(pushback);
					n -= 10;

					/**
					 * read entire frame 
					 */
					frame = new byte[audio.Size];
					audioin.read(frame, 0, audio.Size);

					/**
					 * startfileposition of current frame 
					 */
					actframe = n;

					/**
					 * expected position for following frame 
					 */
					n += audio.Size;

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
								ERRORCODE = is_DTS ? audio.DTS_parseNextHeader(push24, d) : audio.AC3_parseNextHeader(push24, d);

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

					if (is_AC3 && !is_DTS && ValidateCRC && (ERRORCODE = CRC.checkCRC16ofAC3(frame, 2, audio.Size)) != 0 )
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
					if (is_DTS) 
						returncode = audio.DTS_compareHeader(); 
					else 
						returncode = audio.AC3_compareHeader(); 

					if (returncode > 0) 
						newformat = true; 

					if (frame_counter == 0) 
						newformat = true;

					audio.saveHeader();

					/**
					 * replace not 3/2 with silence ac3 3/2 061i++ 
					 */
					if (!is_DTS && ReplaceAc3withSilence && audio.Mode != 7 )
					{
						for (int c = 0; c < Common.getAC3list().size(); c++)
						{
							byte[] ac3data = (byte[]) Common.getAC3list().get(c);

							if ( ((0xE0 & ac3data[6])>>>5) != 7 ) 
								continue;

							frame = ac3data;
							break;
						}
					}

					// timeline ist hier aktuelle audiopts

					Common.setFps(frame_counter);

					/**
					 * preloop if audio starts later than video, and i must insert 
					 */
					if ( (preloop && v>=vptsval.length) || !( preloop && vptsdata && vptsval[v] < timeline - (audio.Time_length / 2.0) ) ) 
						preloop=false;

					else
					{
						/**
						 * patch ac-3 to 3/2 
						 */
						if (!is_DTS && Patch1stAc3Header && frame_counter == 0 )  
							frame[6] = (byte)((0xF & frame[6]) | 0xE0);

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
						while (precount < timeline - (audio.Time_length / 2.0))
						{
							/**
							 * check if frame write should paused 
							 */
							if (vptsdata && w < vptsval.length)
							{ 
								double ms1 = (double) (precount - vptsval[w + 1]);
								double ms2 = (double) (time_counter - vtime[w + 1]);

								if ((double) Math.abs(ms2) <= audio.Time_length / 2.0 )
								{
									awrite = false;
									w += 2;
								}
								else if ((double) Math.abs(ms1) <= audio.Time_length / 2.0 )
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

								if (!awrite && (double) Math.abs((time_counter - vtime[v]) - (precount - vptsval[v])) <= (double) audio.Time_length / 2.0 )
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
								if ((double) Math.abs(vptsval[v] - precount) <= ((double) audio.Time_length / 2.0) )
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
								if (awrite && (double) Math.abs((time_counter - vtime[v - 2]) - (precount - vptsval[v-2])) > (double) audio.Time_length / 2.0 )
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
								if (AddRiffToAc3) 
									riffw[0].AC3RiffData(audio.AC3RiffFormat(frame)); 

								frame_counter++;
								cb++;
								ins[1]++;
								time_counter += audio.Time_length;
							}

							precount += audio.Time_length;

							if (Debug) 
								System.out.println("(6)audio frames: wri/pre/skip/ins/add " + frame_counter + "/" + cb + "/" + ce + "/" + cc + "/" + cd + "  @ " + Common.formatTime_1((long)(time_counter / 90.0) ) + "  ");

						} // end while

						n = actframe;

						audioin.unread(frame);

						if (ins[1] > 0)
							Common.setMessage(Resource.getString("audio.msg.summary.pre-insert", "" + ins[1], FramesToTime((int)ins[1],audio.Time_length)) + " " + Common.formatTime_1(ins[0] / 90L));

						continue readloopdd;
					} // end if preloop


					/****** check if frame write should paused *****/
					if (vptsdata)
					{ 
						vw[0] = v;
						vw[1] = w;

						awrite = SyncCheck(vw, time_counter, audio.Time_length, timeline, frame_counter, vptsval, vtime, awrite, Debug);

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
						System.out.println(" k)" + timeline + " l)" + (audio.Time_length / 2.0) + " u)" + audio.Size + " m)" + awrite + " n)"+w+" o)"+v+" p)"+n);

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
						String hdr = is_DTS ? audio.DTS_displayHeader() : audio.AC3_displayHeader();

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
					if (!is_DTS && Patch1stAc3Header && frame_counter == 0 )  
						frame[6] = (byte)((0xF & frame[6]) | 0xE0);

					if (Debug) 
						System.out.println("(7)audio frames: wri/pre/skip/ins/add " + frame_counter + "/" + cb + "/" + ce + "/" + cc + "/" + cd + "  @ " + Common.formatTime_1((long)(time_counter / 90.0f) ));

					if (Debug) 
						System.out.println(" x" + ((x < ptspos.length - 1) ? x + "/" + ptsval[x + 1] + "/" + ptspos[x + 1] : "-"));

					/**
					 * pts for next frame!! 
					 */
					timeline += audio.Time_length;

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
							if (AddRiffToAc3) 
								riffw[0].AC3RiffData(audio.AC3RiffFormat(frame)); 

							frame_counter++;
							time_counter += audio.Time_length;
						}

						continue readloopdd;
					}

					minSync = 0;

					if ( (double) Math.abs(ptsval[x + 1] - timeline) < (double) audio.Time_length / 2.0 )
					{
						timeline = ptsval[x + 1];
						x++;

						if (!vptsdata || (vptsdata && awrite)) {
							audiooutL.write(frame);

							/**
							 * RIFF 
							 */
							if (AddRiffToAc3) 
								riffw[0].AC3RiffData(audio.AC3RiffFormat(frame)); 

							frame_counter++;
							time_counter += audio.Time_length;
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
							if (AddRiffToAc3) 
								riffw[0].AC3RiffData(audio.AC3RiffFormat(frame)); 

							frame_counter++;
							time_counter += audio.Time_length;

							if (Debug) 
								System.out.println("(10)audio frames: wri/pre/skip/ins/add " + frame_counter + "/" + cb + "/" + ce + "/" + cc + "/" + cd + "  @ " + Common.formatTime_1((long)(time_counter / 90.0) ) + "  ");
						}

						timeline += audio.Time_length;

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


						while (ptsval[x + 1] > (timeline - (audio.Time_length / 2.0)) )
						{
							if (vptsdata && w < vptsval.length)
							{ 
								double ms1 = (double) (timeline - audio.Time_length - vptsval[w + 1]);
								double ms2 = (double) (time_counter - vtime[w + 1]);

								if ((double) Math.abs(ms2) <= audio.Time_length / 2.0 )
								{
									awrite = false;
									w += 2;
								}

								else if ((double) Math.abs(ms1) <= audio.Time_length / 2.0 )
								{
									awrite = false;
									w += 2;
								}
							}

							if (vptsdata && v < vptsval.length)
							{
								if (!awrite && (double) Math.abs((time_counter - vtime[v]) -
										(timeline - audio.Time_length - vptsval[v]) ) <= (double) audio.Time_length / 2.0 )
								{
									double ms1 = (double) (timeline - audio.Time_length - vptsval[v]);
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
								if ((double) Math.abs(vptsval[v] - (timeline - audio.Time_length)) <= ((double) audio.Time_length / 2.0) )
								{
									double ms1 = (double) (timeline - audio.Time_length - vptsval[v]);
									double ms2 = (double) (time_counter - vtime[v]);

									Common.getGuiInterface().showAVOffset("" + (int)(ms1 / 90) + "/" + (int)(ms2 / 90) + "/" + (int)((ms2 - ms1) / 90));

									if (Debug) 
										System.out.println(" ß" + ms1 + "/" + ms2 + "/" + (ms2 - ms1));

									awrite = true;
									v += 2;
								}

								if (awrite && (double) Math.abs((time_counter - vtime[v - 2]) -
										(timeline - audio.Time_length - vptsval[v - 2]) ) > (double) audio.Time_length / 2.0 )
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
								if (AddRiffToAc3) 
									riffw[0].AC3RiffData(audio.AC3RiffFormat(silentFrameBuffer.toByteArray())); 

								frame_counter++;
								time_counter += audio.Time_length;
								cc++;
								ins[1]++;
							}

							if (Debug)
							{
								System.out.println("(8)audio frames: wri/pre/skip/ins/add " + frame_counter + "/" + cb + "/" + ce + "/" + cc + "/" + cd + "  @ " + Common.formatTime_1((long)(time_counter / 90.0f) ) + " ");
								System.out.println(" t)" + timeline);
								System.out.println(" x" + ((x < ptspos.length - 1) ? x + "/" + ptsval[x + 1] + "/" + ptspos[x + 1] : "-"));
							}

							timeline += audio.Time_length;
						} // end while

						timeline -= audio.Time_length;
						insertSilenceLoop = false;
						x++;

						if (ins[1] > 0)
							Common.setMessage(Resource.getString("audio.msg.summary.insert", "" + ins[1], FramesToTime((int)ins[1],audio.Time_length)) + " " + Common.formatTime_1(ins[0] / 90L));

						/**
						 * reset PTS after inserting
						 */
						timeline = ptsval[x];

						continue readloopdd;
					} // end if insertSilenceLoop

					if ( (actframe + audio.Size) >= audiosize ) 
						break readloopdd;

				}  // end while

				/**
				 * add frames at the end 
				 */
				if (es_streamtype == CommonParsing.AC3_AUDIO && AddFrames && vptsdata && awrite && (w < vptsval.length))
				{
					timeline += audio.Time_length;
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
						while (vtime[w + 1] > time_counter && (double) Math.abs(vtime[w + 1] - time_counter) > (double) audio.Time_length / 2.0)
						{
							silentFrameBuffer.writeTo(audiooutL);

							/**
							 * RIFF 
							 */
							if (AddRiffToAc3) 
								riffw[0].AC3RiffData(audio.AC3RiffFormat(silentFrameBuffer.toByteArray())); 

							Common.getGuiInterface().showExportStatus(Resource.getString("audio.status.add")); 

							frame_counter++;
							time_counter += audio.Time_length;
							timeline += audio.Time_length;
							cd++;
							addf[1]++;

							if (Debug)
							{ 
								System.out.println("(9)audio frames: wri/pre/skip/ins/add " + frame_counter + "/" + cb + "/" + ce + "/" + cc + "/" + cd + "  @ " + Common.formatTime_1((long)(time_counter / 90.0f) ));
								System.out.print(" t)" + (long)(timeline - audio.Time_length) + " w)" + w);
							}
						}

						w += 2;
					}

					w -= 2;
					timeline -= audio.Time_length;

					if (Debug) 
						System.out.println(" eot_video:" + (vptsval[w + 1] / 90) + "ms, eot_audio:" + (timeline / 90) + "ms ");
				}


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
					if ((0xCL & CommonParsing.getAudioProcessingFlags()) != 0 || MpaDecoder.RESET)
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
					if ((ERRORCODE = audio.MPA_parseHeader(pushmpa, 0)) < 1)
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
					frame = new byte[audio.Size];

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
					n += audio.Size;

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

						if (audio.Layer > 1 && ClearCRC) 
							audio.MPA_deleteCRC(copyframe[0]);
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

							ERRORCODE = audio.MPA_parseNextHeader(pushmpa, 0);

							audioin.unread(pushmpa);

							if (ERRORCODE < 1)
							{
								audioin.unread(frame, 1, frame.length - 1);
								n = actframe + 1;

								continue readloop;
							}
						}

						layertype = audio.Layer;
					}

					if (ValidateCRC && (ERRORCODE = CRC.checkCRC16ofMPA(audio, frame)) != 0 )
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
					if ((returncode = audio.MPA_compareHeader()) > 0)
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

					// timeline ist hier aktuelle audiopts

					Common.setFps(frame_counter);

					/**
					 * message 
					 */
					if (Debug) 
						System.out.println(" k)" +timeline +" l)" + (audio.Time_length / 2.0) + " m)" + awrite + " n)" + w + " o)" + v + " p)" + n);


					/**
					 * preloop if audio starts later than video, and i must insert 
					 */
					if ( (preloop && vptsdata && v >= vptsval.length) || !( preloop && vptsdata && vptsval[v] < timeline - (audio.Time_length / 2.0) ) ) 
						preloop = false;

					else
					{
						silent_Frame[0] = new byte[audio.Size_base];	//silence without padd, std
						silent_Frame[1] = new byte[audio.Size];		//silence with padd for 22.05, 44.1

						for (int a = 0; a < 2; a++)
						{
							System.arraycopy(header_copy, 0, silent_Frame[a], 0, 4);	//copy last header data
							silent_Frame[a][1] |= 1;				//mark noCRC
							silent_Frame[a][2] |= (a * 2);				//set padding bit
						}

						int padding_counter = 1;						//count padding
						long precount=vptsval[v];
						long[] ins = { (long)time_counter, 0 };

						while ( precount < timeline- (audio.Time_length / 2.0) )
						{  //better for RTS
							/**
							 * check if frame write should paused 
							 */
							if (vptsdata && w < vptsval.length)
							{ 
								double ms1 = (double) (precount - vptsval[w + 1]);
								double ms2 = (double) (time_counter - vtime[w + 1]);

								if ( (double) Math.abs(ms2) <= audio.Time_length / 2.0 )
								{
									awrite = false;
									w += 2;
								}
								else if ((double) Math.abs(ms1) <= audio.Time_length / 2.0 )
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
										(precount - vptsval[v]) ) <= (double)audio.Time_length / 2.0 )
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
								if ((double) Math.abs(vptsval[v] - precount) <= (double) audio.Time_length / 2.0)
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
										(precount - vptsval[v - 2]) ) > audio.Time_length / 2.0 )
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
									if (audio.Layer > 0 && DecodeMpgAudio)
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
											riffw[0].RiffData(audio.RiffFormat(newframes[0])); 

											if (MpaConversionMode >= 4) 
												riffw[1].RiffData(audio.RiffFormat(newframes[1]));
										}
									}
									else
									{
										audiooutL.write(copyframe[0]); 

										/**
										 * RIFF
										 */
										if (AddRiffToMpgAudio || AddRiffToMpgAudioL3) 
											riffw[0].RiffData(audio.RiffFormat(copyframe[0]));
									}
								}
								else
								{
									//if (padding_counter==padding) padding_counter=0;	//reset padd count
									//else if (samplerate==0) padding_counter++;		//count padding

									if (audio.Layer > 0 && DecodeMpgAudio)
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
											riffw[0].RiffData(audio.RiffFormat(newframes[0])); 

											if (MpaConversionMode >= 4) 
												riffw[1].RiffData(audio.RiffFormat(newframes[1]));
										}
									}
									else
									{ 
										audiooutL.write(silent_Frame[(padding_counter > 0) ? 0 : 1]);

										/**
										 * RIFF
										 */
										if (AddRiffToMpgAudio || AddRiffToMpgAudioL3) 
											riffw[0].RiffData(audio.RiffFormat(silent_Frame[(padding_counter > 0) ? 0 : 1]));
									}
								}

								frame_counter++;
								time_counter += audio.Time_length;
								cb++;
								ins[1]++;
							}

							precount += audio.Time_length;

							if (Debug) 
								System.out.println("(5)audio frames: wri/pre/skip/ins/add " + frame_counter + "/" + cb + "/" + ce + "/" + cc + "/" + cd + "  @ " + Common.formatTime_1((long)(time_counter / 90.0f) ));
						} /** end while **/

						n = actframe;

						audioin.unread(frame);

						if (ins[1] > 0)
							Common.setMessage(Resource.getString("audio.msg.summary.pre-insert", "" + ins[1], FramesToTime((int)ins[1],audio.Time_length)) + " " + Common.formatTime_1(ins[0] / 90L));

						continue readloop;
					} 


					/**
					 * check if frame write should paused 
					 */
					if (vptsdata)
					{ 
						vw[0] = v;
						vw[1] = w;

						awrite = SyncCheck(vw, time_counter, audio.Time_length, timeline, frame_counter, vptsval, vtime, awrite, Debug);

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

							Common.setMessage(Resource.getString("audio.msg.source", audio.MPA_displayHeader()) + " " + str);

							if (Common.getSettings().getBooleanProperty(Keys.KEY_ExternPanel_createChapters))
								job_processing.getChapters().addChapter(str, audio.MPA_displayHeader());
						}

						else if (ModeChangeCount == 100) 
							Common.setMessage(Resource.getString("audio.msg.source.max"));

						else if (Debug) 
							System.out.println("=> src_audio: "+audio.MPA_displayHeader() + " @ " + Common.formatTime_1((long)(time_counter / 90.0f)));

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
					timeline += audio.Time_length;

					/**
					 * remove CRC 
					 */
					if (audio.Layer > 1 && ClearCRC) 
						audio.MPA_deleteCRC(frame);

					/**
					 * RDS test
					 */
					if (audio.Layer == 2) 
						audio.testRDS(frame);

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

						if (audio.Layer > 0 && DecodeMpgAudio) 
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
								riffw[0].RiffData(audio.RiffFormat(newframes[0])); 

								if (MpaConversionMode >= 4) 
									riffw[1].RiffData(audio.RiffFormat(newframes[1]));
							}
						}
						else
						{
			 				audiooutL.write(frame);

							/**
							 * RIFF 
							 */
							if (AddRiffToMpgAudio || AddRiffToMpgAudioL3) 
								riffw[0].RiffData(audio.RiffFormat(frame)); 
						}

						frame_counter++;
						time_counter += audio.Time_length;

						continue readloop;
					}

					minSync = 0;

					/**
					 * frame is on pes packet corner 
					 */
					if ((double) Math.abs(ptsval[x + 1] - timeline) < (double) audio.Time_length / 2.0 )
					{
						timeline = ptsval[x + 1];
						x++;

						if (vptsdata && !awrite) 
							continue readloop;

						if (audio.Layer > 0 && DecodeMpgAudio)
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
								riffw[0].RiffData(audio.RiffFormat(newframes[0])); 

								if (MpaConversionMode >= 4) 
									riffw[1].RiffData(audio.RiffFormat(newframes[1]));
							}
						}
						else
						{
							audiooutL.write(frame);

							/**
							 * RIFF 
							 */
							if (AddRiffToMpgAudio || AddRiffToMpgAudioL3) 
								riffw[0].RiffData(audio.RiffFormat(frame)); 
						}

						frame_counter++;
						time_counter += audio.Time_length;

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
						silent_Frame[0] = new byte[audio.Size_base];	//silence without padd, std
						silent_Frame[1] = new byte[audio.Size];		//silence with padd for 22.05, 44.1

						for (int a = 0; a < 2; a++)
						{
							System.arraycopy(header_copy, 0, silent_Frame[a], 0, 4);	//copy last header data
							silent_Frame[a][1] |= 1;				//mark noCRC
							silent_Frame[a][2] |= (a * 2);				//set padding bit
						}

						int padding_counter = 1;						//count padding
						long[] ins = { (long)time_counter, 0 };
		
						// solange nächster ptsval minus nächster framebeginn  ist größer der halben framezeit, füge stille ein
						while (ptsval[x + 1] > (timeline - (audio.Time_length / 2.0)))
						{
							if (vptsdata && w < vptsval.length)
							{ 
								double ms1 = (double) (timeline - audio.Time_length - vptsval[w + 1]);
								double ms2 = (double) (time_counter - vtime[w + 1]);

								if ((double) Math.abs(ms2) <= audio.Time_length / 2.0)
								{
									awrite = false;
									w += 2;
								}
								else if ((double) Math.abs(ms1) <= audio.Time_length / 2.0)
								{
									awrite = false;
									w += 2;
								}
							}

							if (vptsdata && v < vptsval.length)
							{
								if (!awrite && (double) Math.abs((time_counter - vtime[v]) -
									(timeline - audio.Time_length - vptsval[v]) ) <= (double) audio.Time_length / 2.0 )
								{
									double ms1 = (double) (timeline - audio.Time_length - vptsval[v]);
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
								if ((double) Math.abs(vptsval[v] - (timeline - audio.Time_length)) <= ((double) audio.Time_length / 2.0) )
								{
									double ms1 = (double) (timeline - audio.Time_length - vptsval[v]);
									double ms2 = (double) (time_counter - vtime[v]);

									Common.getGuiInterface().showAVOffset("" + (int)(ms1 / 90) + "/" + (int)(ms2 / 90) + "/" + (int)((ms2 - ms1) / 90));

									if (Debug) 
										System.out.println(" ß" + ms1 + "/" + ms2 + "/" + (ms2 - ms1));

									awrite = true;
									v += 2;
								}

								if (awrite && (double) Math.abs((time_counter - vtime[v - 2]) -
									(timeline - audio.Time_length - vptsval[v - 2]) ) > (double) audio.Time_length / 2.0 )
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
									if (audio.Layer > 0 && DecodeMpgAudio)
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
											riffw[0].RiffData(audio.RiffFormat(newframes[0])); 

											if (MpaConversionMode >= 4) 
												riffw[1].RiffData(audio.RiffFormat(newframes[1]));
										}
									}
									else
									{
										audiooutL.write(copyframe[0]);

										/**
										 * RIFF 
										 */
										if (AddRiffToMpgAudio || AddRiffToMpgAudioL3) 
											riffw[0].RiffData(audio.RiffFormat(copyframe[0]));
									}
								}
								else
								{
									//if (padding_counter==padding) padding_counter=0;	//reset padd count
									//else if (samplerate==0) padding_counter++;		//count padding

									if (audio.Layer > 0 && DecodeMpgAudio)
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
											riffw[0].RiffData(audio.RiffFormat(newframes[0])); 

											if (MpaConversionMode >= 4) 
												riffw[1].RiffData(audio.RiffFormat(newframes[1]));
										}
									}
									else
									{
										audiooutL.write(silent_Frame[(padding_counter > 0) ? 0 : 1]);

										/**
										 * RIFF 
										 */
										if (AddRiffToMpgAudio || AddRiffToMpgAudioL3) 
											riffw[0].RiffData(audio.RiffFormat(silent_Frame[(padding_counter > 0) ? 0 : 1])); 
									}
								}

								frame_counter++;
								time_counter += audio.Time_length;
								cc++;
								ins[1]++;
							}

							if (Debug)
							{
								System.out.println("(2)audio frames: wri/pre/skip/ins/add " + frame_counter + "/" + cb + "/" + ce + "/" + cc + "/" + cd + "  @ " + Common.formatTime_1((long)(time_counter / 90.0f) ));
								System.out.print(" t)" + timeline);
								System.out.println(" x" + ((x < ptspos.length - 1) ? x + "/" + ptsval[x + 1] + "/" + ptspos[x + 1] : "-"));
							}

							timeline += audio.Time_length;
						} // end while

						timeline -= audio.Time_length;
						insertSilenceLoop = false;
						x++;

						if (ins[1] > 0) 
							Common.setMessage(Resource.getString("audio.msg.summary.insert", "" + ins[1], FramesToTime((int)ins[1],audio.Time_length)) + " " + Common.formatTime_1(ins[0] / 90L));

						/**
						 * reset PTS after inserting
						 */
						timeline = ptsval[x];

						continue readloop;
					}

					if ( (actframe + audio.Size) >= audiosize ) 
						break readloop; 
				}  // end while

				if (Debug) 
					System.out.println("(3)audio frames: wri/pre/skip/ins/add " + frame_counter + "/" + cb + "/" + ce + "/" + cc + "/" + cd + "  @ " + Common.formatTime_1((long)(time_counter / 90.0f) ));

				/**
				 * add frames at the end 
				 */
				if (es_streamtype == CommonParsing.MPEG_AUDIO && AddFrames && vptsdata && awrite && (w < vptsval.length))
				{
					timeline += audio.Time_length;
					addf[0] = (long) time_counter;

					silent_Frame[0] = new byte[audio.Size_base];	//silence without padd, std
					silent_Frame[1] = new byte[audio.Size];		//silence with padd for 22.05, 44.1

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
							(double) Math.abs(vtime[w + 1] - time_counter) > (double) audio.Time_length / 2.0 )
						{
							if (FillGapsWithLastFrame)
							{				//add_copy prev. frame
								if (audio.Layer > 0 && DecodeMpgAudio) 
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
										riffw[0].RiffData(audio.RiffFormat(newframes[0])); 

										if (MpaConversionMode >= 4) 
											riffw[1].RiffData(audio.RiffFormat(newframes[1]));
									}
								}
								else
								{
									audiooutL.write(copyframe[0]);

									/**
									 * RIFF 
									 */
									if (AddRiffToMpgAudio || AddRiffToMpgAudioL3) 
										riffw[0].RiffData(audio.RiffFormat(copyframe[0]));
								}
							}
							else
							{	//add silence
								//if (padding_counter==padding) padding_counter=0;	//reset padd count
								//else if (samplerate==0) padding_counter++;		//count padding

								if (audio.Layer > 0 && DecodeMpgAudio)
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
										riffw[0].RiffData(audio.RiffFormat(newframes[0])); 

										if (MpaConversionMode >= 4) 
											riffw[1].RiffData(audio.RiffFormat(newframes[1]));
									}
								}
								else
								{
									audiooutL.write(silent_Frame[(padding_counter > 0) ? 0 : 1]);

									/**
									 * RIFF 
									 */
									if (AddRiffToMpgAudio || AddRiffToMpgAudioL3) 
										riffw[0].RiffData(audio.RiffFormat(silent_Frame[(padding_counter > 0) ? 0 : 1]));
								}
							}

							timeline += audio.Time_length;
							cd++;
							frame_counter++;
							addf[1]++;
							time_counter += audio.Time_length;

							Common.getGuiInterface().showExportStatus(Resource.getString("audio.status.add")); 

							if (Debug)
							{
								System.out.println("(4)audio frames: wri/pre/skip/ins/add " + frame_counter + "/" + cb + "/" + ce + "/" + cc + "/" + cd + "  @ " + Common.formatTime_1((long)(time_counter / 90.0f) ));
								System.out.print(" t)" + (long)(timeline - audio.Time_length) + " w)" + w);
							}
						}

						w += 2;
					}

					w -= 2;
					timeline -= audio.Time_length;

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

					audio.WAV_parseHeader(frame, 0);

					audioin.unread(frame, audio.Emphasis, 1000 - audio.Emphasis);

					Common.setMessage(Resource.getString("audio.msg.source", audio.WAV_saveAnddisplayHeader()) + " " + Common.formatTime_1((long)(time_counter / 90.0f)));
					layertype = WAV_AUDIOSTREAM;

					n = audio.Emphasis; //start of pcm data
					long pcm_end_pos = audio.Emphasis + audio.Size_base; //whole sample data size

					timeline = ptsval[0];

					audiooutL.write(audio.getRiffHeader());

					long sample_bytes;
					long skip_bytes;
					long sample_pts;
					long skip_pts;

					int sample_size;
					int read_size = 960000 / audio.Mode;

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
								sample_bytes = (long)Math.round(1.0 * audio.Sampling_frequency * sample_pts / 90000.0) * audio.Mode;

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
								frame_counter += (sample_bytes / audio.Mode);

								Common.setFps(frame_counter);

								if (vptsval[a + 1] > timeline)
								{
									sample_pts = vptsval[a + 1] - timeline;
									sample_bytes = (long) Math.round(1.0 * audio.Sampling_frequency * sample_pts / 90000.0) * audio.Mode;

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
									frame_counter += (sample_bytes / audio.Mode);
								}
							}
							else
							{
								skip_pts = vptsdata ? vptsval[a] - timeline : 0;
								skip_bytes = (long)Math.round(1.0 * audio.Sampling_frequency * skip_pts / 90000.0) * audio.Mode;

								sample_pts = vptsdata ? vptsval[a + 1] - vptsval[a] : (long)(1.0 * (audio.Size_base / audio.Mode) / audio.Sampling_frequency * 90000.0);
								sample_bytes = (long) Math.round(1.0 * audio.Sampling_frequency * sample_pts / 90000.0) * audio.Mode;

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
								frame_counter += (sample_bytes / audio.Mode);
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

				break;

			} // end while bigloop


			if (addf[1] > 0)
				Common.setMessage(Resource.getString("audio.msg.summary.add", "" + addf[1], FramesToTime((int)addf[1],audio.Time_length)) + " " + Common.formatTime_1(addf[0] / 90L));

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
				{ "(new).ac3","(new).mp1","(new).mp2","(new).mp3","(new).dts" }
			};

			if (Common.getSettings().getBooleanProperty(Keys.KEY_ExternPanel_renameAudio))
			{
				for (int g = 1; g < 4; g++)
				{
					pureaudio[0][g] = ".mpa";
					pureaudio[1][g] = "(new).mpa";
				}
			}

			if (DecodeMpgAudio && audio.Layer > 1)
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

			File ac3name = new File (fparent + pureaudio[isElementaryStream][0]);
			File mp1name = new File (fparent + pureaudio[isElementaryStream][1]);
			File mp2name = new File (fparent + pureaudio[isElementaryStream][2]);
			File mp3name = new File (fparent + pureaudio[isElementaryStream][3]);
			File mp2nameL = new File (fparent + "[L]" + pureaudio[0][2]);
			File mp2nameR = new File (fparent + "[R]" + pureaudio[0][2]);
			File dtsname = new File (fparent + pureaudio[isElementaryStream][4]);
			File wavname = new File (fparent + "(new).wav");

			/*** make riff ***/
			if (DecodeMpgAudio && es_streamtype == CommonParsing.MPEG_AUDIO && MpaDecoder.WAVE)
			{
				if (audio.Layer > 1)
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
				if (audio.Layer > 1)
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
			{
				audio.fillRiffHeader(newnameL);
			}


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

		//yield();

		//System.gc();

		return false;
	}


	/**
	 * decoding teletext stream
	 */
	private String FramesToTime(int framenumber, double framelength)
	{
		return String.valueOf(Math.round(framenumber * framelength / 90.0));
	}

	/**
	 * decoding teletext stream
	 */
	private void processTeletext(JobCollection collection, String filename, String filename_pts, String filename_type, String videofile_pts)
	{
		processTeletext(collection, new XInputFile(new File(filename)), filename_pts, filename_type, videofile_pts);
	}

	/**
	 * decoding teletext stream
	 */
	private void processTeletext(JobCollection collection, XInputFile xInputFile, String filename_pts, String filename_type, String videofile_pts)
	{
		int LB1 = -1;
		long size = 0;
		boolean debug = collection.DebugMode();

		boolean Message_2 = Common.getSettings().getBooleanProperty(Keys.KEY_MessagePanel_Msg2);
		boolean ShowSubpictureWindow = Common.getSettings().getBooleanProperty(Keys.KEY_showSubpictureWindow);
		boolean DecodeMegaradio = Common.getSettings().getBooleanProperty(Keys.KEY_SubtitlePanel_decodeMegaradio);
		boolean ExportTextAsUnicode = Common.getSettings().getBooleanProperty(Keys.KEY_SubtitlePanel_exportTextAsUnicode);
		boolean DecodeHiddenRows = Common.getSettings().getBooleanProperty(Keys.KEY_SubtitlePanel_decodeHiddenRows);
		boolean KeepOriginalTimcode = Common.getSettings().getBooleanProperty(Keys.KEY_SubtitlePanel_keepOriginalTimcode);

		String SubtitleExportFormat = Common.getSettings().getProperty(Keys.KEY_SubtitleExportFormat);
		String SubtitleFont = Common.getSettings().getProperty(Keys.KEY_SubtitleFont);
		String Format_SUP_Values = Common.getSettings().getProperty(Keys.KEY_SubtitlePanel_Format_SUP_Values);

		JobProcessing job_processing = collection.getJobProcessing();

		Subpicture subpicture = Common.getSubpictureClass();

		if (ShowSubpictureWindow)
			Common.getGuiInterface().showSubpicture();

		if (SubtitleExportFormat.equalsIgnoreCase(Keys.ITEMS_SubtitleExportFormat[7].toString()) || SubtitleExportFormat.equalsIgnoreCase(Keys.ITEMS_SubtitleExportFormat[6].toString())) // SUP + SON, set variables
			LB1 = subpicture.set(SubtitleFont, Format_SUP_Values);

		for (int LB = 0; LB < 2; LB++)
		{
			String[] userdefined_pages = {
				Common.getSettings().getProperty(Keys.KEY_SubtitlePanel_TtxPage1),
				Common.getSettings().getProperty(Keys.KEY_SubtitlePanel_TtxPage2),
				Common.getSettings().getProperty(Keys.KEY_SubtitlePanel_TtxPage3),
				Common.getSettings().getProperty(Keys.KEY_SubtitlePanel_TtxPage4),
				Common.getSettings().getProperty(Keys.KEY_SubtitlePanel_TtxPage5),
				Common.getSettings().getProperty(Keys.KEY_SubtitlePanel_TtxPage6)
			};

			for (int pn = 0; pn < userdefined_pages.length; pn++)
			{
				String page = "0";

				if (!DecodeMegaradio)
				{
					page = userdefined_pages[pn];

					if (page.equalsIgnoreCase("null")) 
						continue;
				}
				else
					pn = userdefined_pages.length;

			//  not supported as an elementary stream
			//	String fchild = isElementaryStream == CommonParsing.ES_TYPE ? collection.getOutputName(xInputFile.getName()) : xInputFile.getName();
			//	String fchild = collection.getOutputName(xInputFile.getName());
				String fchild = xInputFile.getName();
				String fparent = collection.getOutputNameParent(fchild);

				size = xInputFile.length();

				Common.getGuiInterface().initTtxPageMatrix(fchild);

				Teletext.clearEnhancements();

				String LBs = LB == 1 ? "[1]" : "";

				if (!DecodeMegaradio) 
					fparent += "[" + page + "]" + LBs;


				String ttxfile;

				int subtitle_type;

				if (DecodeMegaradio) 
				{
					ttxfile = fparent + ".mgr";
					subtitle_type = 0;
				}

				else if (SubtitleExportFormat.equalsIgnoreCase(Keys.ITEMS_SubtitleExportFormat[1].toString()))
				{
					ttxfile = fparent + ".sc";
					subtitle_type = 2;
				}

				else if (SubtitleExportFormat.equalsIgnoreCase(Keys.ITEMS_SubtitleExportFormat[2].toString()))
				{
					ttxfile = fparent + ".sub";
					subtitle_type = 3;
				}

				else if (SubtitleExportFormat.equalsIgnoreCase(Keys.ITEMS_SubtitleExportFormat[3].toString())) 
				{
					ttxfile = fparent + ".srt";
					subtitle_type = 4;
				}

				else if (SubtitleExportFormat.equalsIgnoreCase(Keys.ITEMS_SubtitleExportFormat[5].toString())) 
				{
					ttxfile = fparent + ".ssa";
					subtitle_type = 5;
				}

				else if (SubtitleExportFormat.equalsIgnoreCase(Keys.ITEMS_SubtitleExportFormat[7].toString())) 
				{
					ttxfile = fparent + ".sup";
					subtitle_type = 6;
				}

				else if (SubtitleExportFormat.equalsIgnoreCase(Keys.ITEMS_SubtitleExportFormat[4].toString())) 
				{
					ttxfile = fparent + ".stl";
					subtitle_type = 7;
				}

				else if (SubtitleExportFormat.equalsIgnoreCase(Keys.ITEMS_SubtitleExportFormat[6].toString())) //placeholder for .son export
				{
					ttxfile = fparent + ".ssa"; //.son
					subtitle_type = 5;  // 8
				}

				else if (SubtitleExportFormat.equalsIgnoreCase("null"))
					continue;

				else   // free format
				{
					ttxfile = fparent + ".txt";
					subtitle_type = 1;
				}

				Common.setMessage(Resource.getString("teletext.msg.output") + " " + ttxfile.substring(ttxfile.length() - 3));

				if (ExportTextAsUnicode && subtitle_type != 6)
					Common.setMessage("-> " + Resource.getString(Keys.KEY_SubtitlePanel_exportTextAsUnicode[0]));

				if (DecodeHiddenRows)
					Common.setMessage("-> " + Resource.getString(Keys.KEY_SubtitlePanel_decodeHiddenRows[0]));

				if (KeepOriginalTimcode)
					Common.setMessage("-> " + Resource.getString(Keys.KEY_SubtitlePanel_keepOriginalTimcode[0]));


				DateFormat timeformat_1 = new SimpleDateFormat("HH:mm:ss.SSS");
				timeformat_1.setTimeZone(TimeZone.getTimeZone("GMT+0:00"));

				DateFormat timeformat_2 = new SimpleDateFormat("HH:mm:ss,SSS");
				timeformat_2.setTimeZone(TimeZone.getTimeZone("GMT+0:00"));

				boolean vptsdata = false;
				boolean ptsdata = false;
				boolean write = false;
				boolean loadpage = false;
				boolean valid = false;

				long count = 0;
				long time_difference = 0;
				long source_pts = 0;
				long startPoint = 0;

				int page_value = subtitle_type == 0 ? 0x800 : Integer.parseInt(page, 16);
				int x = 0;
				int seiten = 0;
				int v = 0;
				int w = 0;

				String page_number = "";
				String subpage_number = "";

				char txline[];

				try {

					PushbackInputStream in = new PushbackInputStream(xInputFile.getInputStream(), 94);
					BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(ttxfile), 655350);
					ByteArrayOutputStream byte_buffer = new ByteArrayOutputStream();

					UnicodeWriter print_buffer = new UnicodeWriter(byte_buffer, ExportTextAsUnicode);

					Common.setMessage(Resource.getString("teletext.msg.tmpfile", xInputFile.getName(), "" + size));
					Common.setMessage(Resource.getString("teletext.msg.search") + " " + (subtitle_type == 0 ? Resource.getString("teletext.msg.megaradio") : Resource.getString("teletext.msg.page") + " " + page));

					Common.updateProgressBar(Resource.getString("teletext.progress") + " " + (subtitle_type == 0 ? Resource.getString("teletext.msg.megaradio") : Resource.getString("teletext.msg.page") + " " + page), 0, 0);

					long[] pts_value = {0};
					long[] pts_position = {0};
					long[] video_pts_value = {0};
					long[] vtime = {0};

					Common.getGuiInterface().showExportStatus(Resource.getString("teletext.status"), seiten);

					//System.gc();

					long[][] obj = loadTempOtherPts(filename_pts, "teletext.msg.discard", "audio.msg.pts.firstonly", "teletext.msg.pts.start_end", "teletext.msg.pts.missed", CommonParsing.TELETEXT, false, debug);

					if (obj != null)
					{
						pts_value = obj[0];
						pts_position = obj[1];
						ptsdata = true;
						obj = null;
					}

					obj = loadTempVideoPts(videofile_pts, debug);

					if (obj != null)
					{
						video_pts_value = obj[0];
						vtime = obj[1];
						vptsdata = true;
						obj = null;
					}

					//System.gc();

					// 1st line 
					switch (subtitle_type)
					{
					case 2:
						print_buffer.println("Subtitle File Mark:"+((CommonParsing.getVideoFramerate()==3600L) ? "2" : "1"));
						print_buffer.flush();
						byte_buffer.writeTo(out);
						byte_buffer.reset();
						break;

					case 5:
						String[] SSAhead = Teletext.getSSAHead();

						for (int a = 0; a < SSAhead.length; a++) 
							print_buffer.println(SSAhead[a]);

						print_buffer.flush();
						byte_buffer.writeTo(out);
						byte_buffer.reset();
						break;

					case 7:
						String[] STLhead = Teletext.getSTLHead(Common.getVersionName() + " on " + DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(System.currentTimeMillis())));

						for (int a = 0; a < STLhead.length; a++) 
							print_buffer.println(STLhead[a]);

						print_buffer.flush();
						byte_buffer.writeTo(out);
						byte_buffer.reset();
						break;

					case 8:  //DM14052004 081.7 int02 add, still unused!
						String[] SONhead = Teletext.getSONHead(new File(ttxfile).getParent(), (long)CommonParsing.getVideoFramerate());

						for (int a = 0; a < SONhead.length; a++) 
							print_buffer.println(SONhead[a]);

						print_buffer.flush();
						byte_buffer.writeTo(out);
						byte_buffer.reset();
					} 

					if (vptsdata && ptsdata)
					{
						int jump = checkPTSMatch(video_pts_value, pts_value);

						if (jump < 0)
						{
							Common.setMessage(Resource.getString("teletext.msg.pts.mismatch"));  

							vptsdata = false; 
							x = 0; 
						}

						else
							x = jump;
					}

					x = 0;  //start at 0

					if (ptsdata)
					{ 
						source_pts = pts_value[x]; 
						startPoint = pts_position[x]; 

						if (vptsdata) 
						{
							Common.setMessage(Resource.getString("teletext.msg.adjust.at.video"));
							time_difference = video_pts_value[0];
						}
						else
						{
							Common.setMessage(Resource.getString("teletext.msg.adjust.at.own"));
							time_difference = 0;
						}
					}

					while (count < startPoint)
						count += in.skip(startPoint-count);

					//yield();

					boolean missing_syncword = false;
					boolean vps = false;
					boolean page_match = false;
					boolean lastpage_match = false;

					int row = -1;
					int magazine = -1;
					int vbi = 0;
					int character_set = 0;
					int data_unit_id = -1;
					int required_data_unit_id = -1;

					byte packet[] = new byte[48];

					Hashtable load_buffer = new Hashtable();
					Hashtable write_buffer = new Hashtable();
					Hashtable flags = new Hashtable();

					ArrayList picture_String = new ArrayList();

					String provider = "";
					String program_title = "";
					String vps_str = "";

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

						in.read(packet);

						if (packet[1] != 0x2C && packet[47] != 0x2C && packet[1] != 0x5A && (0xFF & packet[1]) != 0x88 && (0xFF & packet[1]) != 0xFF)
						{
							if (Message_2 && !missing_syncword)
								Common.setMessage(Resource.getString("teletext.msg.syncword.lost") + " " + count);

							missing_syncword = true;
							count++;
							in.unread(packet, 1, 47);

							continue readloop;
						}

						/**
						 * stuffing packet with size 0x5A + 2
						 */
						else if (packet[1] == 0x5A && packet[0] == -1)
						{
							in.skip(0x5C - 48);
							count += 0x5C;

							continue readloop;
						}

						/**
						 * stuffing packet with size 0x88 + 2
						 */
						else if ((0xFF & packet[1]) == 0x88 && packet[0] == -1)
						{
							in.skip(0x8A - 48);
							count += 0x8A;

							continue readloop;
						}

						/**
						 * stuffing packet without size value, skips std packet size 46
						 */
						else if (packet[1] == -1 && packet[0] == -1)
						{
							in.unread(packet, 46, 2);
							count += 0x2E;

							continue readloop;
						}

						else
							in.unread(packet, 46, 2);

						if (Message_2 && missing_syncword)
							Common.setMessage(Resource.getString("teletext.msg.syncword.found") + " " + count);

						missing_syncword = false;

						count += 46;

						vps = false;
						valid = false;
						data_unit_id = 0xFF & packet[0];

						switch (data_unit_id)
						{
						case 2:    // 0x02 EBU non subtitle data
						case 3:    // 0x03 EBU subtitle data
							valid = true; 
							break; 

						case 0xC3: // VPS
							valid = true; 
							vps = true; 
							break; 

						case 0xFF: // hidden 
							if (DecodeHiddenRows)
								valid = true;
							break; 

						default:  // others, unknown
							if (debug) 
								System.out.println(" unkn_"+Integer.toHexString(0xFF & packet[0])+"/"+(count-46));
							//continue readloop;
						}

						// logging
						if (debug)
						{
							System.out.println();

							for (int a = 0; a < 46; a++)
								System.out.print(" " + ((0xFF & packet[a])<0x10 ? "0" : "") + Integer.toHexString(0xFF & packet[a]).toUpperCase());

							System.out.println();
						}

						if (!valid)
							continue readloop;

						vbi = ((0x20 & packet[2]) != 0 ? 0 : 313) + (0x1F & packet[2]);

						if (!vps)
						{
							row = 0xFF & Teletext.bytereverse((byte)((0xF & Teletext.hamming_decode(packet[4]))<<4 | (0xF & Teletext.hamming_decode(packet[5]))));
							magazine = (7 & row) == 0 ? 8 : (7 & row);
							row >>>= 3;
						}

						else
						{
							if ((0x3F & packet[2]) != 0x30)
								continue readloop;

							/**
							 * show vps status of VBI 16 in GUI
							 */
							String str = VBI.decodeVPS(packet, 2);

							if (str != null && !str.equals(vps_str))
							{
								vps_str = str;

								if (Common.getSettings().getBooleanProperty(Keys.KEY_showTtxHeader)) //interactive checkbox
									Common.getGuiInterface().updateVpsLabel(vps_str);

								Common.setMessage(Resource.getString("teletext.msg.vps", str) + " " + Common.formatTime_1(source_pts / 90));
							}

							continue readloop;
						}


						// X3/31.1 ttx provider
						if (magazine == 3 && row == 31 && packet[7] == 0x40 && packet[8] == 0x57 && provider.equals(""))
						{
							provider = Teletext.makestring(packet, 10, 34, 31, 0, 0, false).trim();
							Common.setMessage(Resource.getString("teletext.msg.provider") + " " + provider);
						}

						// X8/30.0 program title
						else if (magazine == 8 && row == 30 && packet[7] == (byte)0xA8)
						{
							String str = Teletext.makestring(packet, 26, 20, 30, 0, 0, true).trim() + " ";

							if (!str.equals(program_title))
							{
								program_title = str;
								Common.setMessage(Resource.getString("teletext.msg.program") + " " + program_title);
							}
						}


						if (row == 0)
						{
							int flag = 0;

							for (int a = 0; a < 6; a++)
								flag |= (0xF & Teletext.bytereverse( Teletext.hamming_decode(packet[8+a]) )>>>4 ) <<(a*4);

							page_number = Integer.toHexString(0xF & Teletext.bytereverse( Teletext.hamming_decode(packet[7]) )>>>4 ).toUpperCase() +
								Integer.toHexString(0xF & Teletext.bytereverse( Teletext.hamming_decode(packet[6]) )>>>4 ).toUpperCase();

							int o[] = { 0xF, 7, 0xF, 3 };
							subpage_number = "";

							for (int a = 3; a > -1; a--)
								subpage_number += Integer.toHexString(o[a] & flag>>>(a*4)).toUpperCase();

							flags.put("data_unit_id", "" + data_unit_id);
							flags.put("magazine", "" + magazine);
							flags.put("page_number", page_number);
							flags.put("subpage_number", subpage_number);
							flags.put("news", "" + (1 & flag>>>14));
							flags.put("subtitle", "" + (1 & flag>>>15));
							flags.put("erase", "" + (1 & flag>>>7));
							flags.put("suppressed_head", "" + (1 & flag>>>16));
							flags.put("update", "" + (1 & flag>>>17));
							flags.put("interrupt", "" + (1 & flag>>>18));
							flags.put("inhibit", "" + (1 & flag>>>19));
							flags.put("magazine_serial", "" + (1 & flag>>>20));
							flags.put("character_set", "" + (7 & flag>>>21));

							// page_number matches -- subpage_numer currently always accepted
							if ( page.equalsIgnoreCase( Integer.toHexString(magazine) + page_number) )
							{
								character_set = 7 & flag>>>21;
								page_match = true;
							}

							else
								page_match = false;

							Common.getGuiInterface().updateTtxPageMatrix("" + magazine + page_number);

							// show header_line in GUI
							if (Common.getSettings().getBooleanProperty(Keys.KEY_showTtxHeader) || debug) 
							{
								String str = magazine + page_number + "  " + subpage_number + "  " + Teletext.makestring(packet, 14, 32, 0, (7 & flag>>>21), 0, true) + "  " + program_title;

								if (Common.getSettings().getBooleanProperty(Keys.KEY_showTtxHeader)) //interactive checkbox
									Common.getGuiInterface().updateTtxHeader(str);

								if (debug)
									System.out.println(str);
							}

							if (debug)
								System.out.println(flags.toString());
						}

						if (ptsdata)
						{
							write = !vptsdata;

							while (pts_position[x+1] != -1 && pts_position[x+1] <= count - 46)
							{
								x++;
								source_pts = pts_value[x];
							}

							rangeloop:
							while (vptsdata && v < video_pts_value.length)  //pic_start_pts must be in range ATM
							{ 
								if (source_pts < video_pts_value[v])
								{
									//write_buffer.put("cut_in_time", "" + video_pts_value[v]); // save value for cuts
									break rangeloop;
								}

								else if (source_pts == video_pts_value[v] || source_pts < video_pts_value[v+1])
								{
									write = true;
									break rangeloop;
								}

								v += 2;

								if (v < video_pts_value.length)
								{
									//write_buffer.put("cut_out_time", "" + video_pts_value[v-1]); // save value for cuts
									time_difference += (video_pts_value[v] - video_pts_value[v-1]);
								}
							}
						}

						else
							write = true;


						// logging
						if (debug)
							System.out.println("pos "+ (count-46) + "/vbi "+vbi+"/ "+magazine+"-"+row+"-"+page_number+"-"+subpage_number+"/pts "+source_pts+"/ "+timeformat_1.format(new Date(source_pts/90))+"/ "+Integer.toHexString(page_value)+"/wr "+write+"/lo "+loadpage+"/lastp "+lastpage_match+"/pagm "+page_match+"/v "+(v<video_pts_value.length ? video_pts_value[v] : v));


						if (row != 0 && magazine != page_value>>>8) //row > 0, but not of current magazine
							continue readloop;

						if (row == 0)  //accept all 0-rows of all magazines till now
						{
							boolean interrupt_loading = false;

							//stop loading if same magazine, but other page
							//stop loading if interrupted_flag by any page only if magazine_serial == 1, means rows of diff. pages of multiple magazines don't overlap by sent order
							if (page_match || magazine == page_value>>>8 || flags.get("magazine_serial").toString().equals("1") )
								interrupt_loading = true;

							if (debug)
								System.out.println("int " + interrupt_loading +"/load "+ loadpage + "/wri "+ write + "/wb " + write_buffer.size() + "/lb " + load_buffer.size());

							// page header does not interrupt an active loading
							if (!interrupt_loading)
								continue readloop;

							// megaradio_mode, magazine 8 only, other magazines are already ignored before page check
							if (subtitle_type == 0)
							{
								switch (Integer.parseInt(page_number, 16))
								{
								case 0xA:
								case 0xB:
								case 0xC:
									loadpage=true; 
								}

								switch (Integer.parseInt(subpage_number, 16))
								{
								case 0x0D:
								case 0x0E:
								case 0x1C:
								case 0x1D:
								//case 0x2A:
								case 0x2B:
								case 0x2C:
								case 0x2D:
									loadpage=false; 
								}

								continue readloop; 
							}

							// row 0 defines out_time of current buffered page forced by an interrupt event, 
							// sets the out_time of displaying and write it out
							if (lastpage_match)
							{
								long out_time = source_pts - time_difference;

								/**
								 * adapt out_time of page, if termination of it was detected later than 80ms after last row
								 */
								Object buffer_time = load_buffer.get("buffer_time");

								if (buffer_time != null)
								{
									long l = Long.parseLong(buffer_time.toString());

									if (source_pts - l > 7200)
									{
										out_time = l - time_difference + 7200;

										if (debug)
											System.out.println("termination for in_time too late, new out_time: " + out_time);
									}
								}

								if (write)  //buffered page can be written
								{
									write_buffer.put("out_time", "" + out_time);

									while (true)
									{
										if ( !write_buffer.containsKey("active") )
											break;

										if ( !write_buffer.containsKey("in_time") )
											break;

										switch (subtitle_type)
										{
										case 1:  // free
											print_buffer.print( "in_" + timeformat_1.format( new Date( Long.parseLong( write_buffer.get("in_time").toString()) / 90) ));
											print_buffer.println( "|out_" + timeformat_1.format( new Date( Long.parseLong( write_buffer.get("out_time").toString()) / 90) )); 
											break;

										case 2:  // SC
											print_buffer.print( Teletext.SMPTE( timeformat_1.format( new Date( Long.parseLong( write_buffer.get("in_time").toString()) / 90) ), (long)CommonParsing.getVideoFramerate()) + "&");
											print_buffer.print( Teletext.SMPTE( timeformat_1.format( new Date( Long.parseLong( write_buffer.get("out_time").toString()) / 90) ), (long)CommonParsing.getVideoFramerate()) + "#");
											break;

										case 3:  // SUB
											print_buffer.print( "{" + ( (long)(Long.parseLong( write_buffer.get("in_time").toString()) / CommonParsing.getVideoFramerate())) + "}");
											print_buffer.print( "{" + ( (long)(Long.parseLong( write_buffer.get("out_time").toString()) / CommonParsing.getVideoFramerate())) + "}");
											break;

										case 4:  // SRT
											print_buffer.println( "" + (seiten + 1));
											print_buffer.print( timeformat_2.format( new Date( Long.parseLong( write_buffer.get("in_time").toString()) / 90) ));
											print_buffer.println(" --> " + timeformat_2.format( new Date( Long.parseLong( write_buffer.get("out_time").toString()) / 90) ));
											break;

										case 5:  // SSA
											print_buffer.print( Teletext.getSSALine()[0] + timeformat_1.format( new Date( Long.parseLong( write_buffer.get("in_time").toString()) / 90) ).substring(1, 11) + ",");
											print_buffer.print( timeformat_1.format( new Date( Long.parseLong( write_buffer.get("out_time").toString()) / 90) ).substring(1, 11) + Teletext.getSSALine()[1]);
											break;
	
										case 7:  // STL
											print_buffer.print( Teletext.SMPTE(timeformat_1.format( new Date( Long.parseLong( write_buffer.get("in_time").toString()) / 90) ), (long)CommonParsing.getVideoFramerate()) + ",");
											print_buffer.print( Teletext.SMPTE(timeformat_1.format( new Date( Long.parseLong( write_buffer.get("out_time").toString()) / 90) ), (long)CommonParsing.getVideoFramerate()) + ",");
											break;

										case 6:  // SUP
											long sup_in_time = Long.parseLong( write_buffer.get("in_time").toString() );

											if ( sup_in_time >= 0 )
											{
												for (int a = 1; a < 24; a++)
													if ( write_buffer.containsKey("" + a) )
														picture_String.add(write_buffer.get("" + a));

												while (picture_String.size() > subpicture.getMaximumLines()) // max. lines as defined
													picture_String.remove(0);

												subpicture.showPicTTX( picture_String.toArray(), job_processing.getStatusStrings());
												byte_buffer.write( subpicture.writeRLE( sup_in_time, 0xB4)); // alias duration 1.800 sec if out_time is missing

												if ( write_buffer.containsKey("out_time") )
													out.write(subpicture.setTime( byte_buffer.toByteArray(), Long.parseLong( write_buffer.get("out_time").toString())));
											}

											picture_String.clear();
										}

										int b = 0;

										for (int a = 1; subtitle_type != 6 && a < 24; a++)
										{
											if ( !write_buffer.containsKey("" + a) )
												continue;

											String str = write_buffer.get("" + a).toString();

											switch (subtitle_type)
											{
											case 1:  // free
											case 4:  // SRT
												print_buffer.println(str); 
												break;

											case 2:  // SC
												print_buffer.print(str); 
												break;

											case 3:  //	 SUB
											case 7:  // STL
												print_buffer.print( (b > 0 ? "|" : "") + str);
												break;

											case 5:  // SSA
												print_buffer.print( (b > 0 ? "\\n" : "") + str);
											}

											b++;
										}

										if (subtitle_type != 6 && b > 0)
										{
											print_buffer.println();
											print_buffer.flush();
											byte_buffer.writeTo(out); 
										}

										seiten++;
										Common.getGuiInterface().showExportStatus(Resource.getString("teletext.status"), seiten);
										break;
									}
								}

								byte_buffer.reset(); 
								write_buffer.clear();
							}

							lastpage_match = page_match;

							// row 0 defines completion of current page to buffer, 
							// sets the in_time of displaying but cannot write it w/o still unknown out_time
							if (loadpage)
							{
								write_buffer.clear();

								if (!vptsdata && time_difference == 0 && !KeepOriginalTimcode)
									time_difference = source_pts;

								long in_time = source_pts - time_difference;
								boolean rows = false;

								/**
								 * adapt in_time of page, if termination of it was detected later than 80ms after last row
								 */
								Object buffer_time = load_buffer.get("buffer_time");

								if (buffer_time != null)
								{
									long l = Long.parseLong(buffer_time.toString());

									if (source_pts - l > 7200)
									{
										in_time = l - time_difference + 7200;

										if (debug)
											System.out.println("termination too late, new in_time: " + in_time);
									}
								}


								// copy keys+values to clear for next page, only row 1..23 used instead of 0..31
								for (int a = 1; a < 24; a++) 
								{
									if ( !load_buffer.containsKey("" + a) )
										continue;

									rows = true; // non blank page
									write_buffer.put("" + a, load_buffer.get("" + a));
								}

								if (rows && write) // if false, in_time has to be set/updated at synccheck above until an exported gop pts area
									write_buffer.put("in_time", "" + in_time);

								if (!rows)
									lastpage_match = false;

								else
									write_buffer.put("active", "1");

								Teletext.clearEnhancements();

								load_buffer.clear();
							}

							loadpage = page_match;

							// logging
							if (debug)
								System.out.println("lo " + loadpage + "/lp_p "+lastpage_match+"/pg_m "+page_match+"/wbuf: " + write_buffer.toString());

							continue readloop;
						}

						// only rows > 0

						// logging
						if (debug)
							System.out.println("load " + loadpage + "/lbuf " + load_buffer.toString());

						// ignore if row is not of expected magazine
						if (magazine != page_value>>>8)
							continue readloop;

						// load and filter re-defined chars from X26/0..15 triplets
						if (row > 23 && subtitle_type != 0)
						{
							if (row == 29 || loadpage)
								Teletext.setEnhancements(packet, row, character_set);

							continue readloop;
						}

						if (!loadpage)
							continue readloop;

						if (subtitle_type == 0)  // megaradio, simple decode the bytes of row 1..23
						{
							for (int b = (row == 1) ? 17: 0; row < 24 && b < 39; b++) // framebytes to MSB
								out.write(Teletext.bytereverse(packet[7+b]));

							continue readloop;
						}

						// decode row 1..23 , 0=header, 24 fasttext labels, 25 supressedheader, >26 non text packets 

						String str = null;
						int[] picture_data = null;

						switch (subtitle_type)
						{
						case 1:
							str = Teletext.makestring(packet, 6, 40, row, character_set, 0, true);
							break;

						case 2:
						case 7:
						case 3:
						case 4:
							str = Teletext.makestring(packet, 6, 40, row, character_set, 0, true).trim();
							break;

						case 5:
							str = Teletext.makestring(packet, 6, 40, row, character_set, 1, true).trim();
							break;

						case 6:
							picture_data = Teletext.makepic(packet, 6, 40, row, character_set, true);
						}

						if (str != null && !str.equals(""))
							load_buffer.put("" + row, str);

						else if (picture_data != null)
							load_buffer.put("" + row, picture_data);

						if (debug) 
							System.out.println("row " + row + ": " + str + "/lb " + load_buffer.size()); 

						/**
						 * updates current timestamp of last packet
						 */
						load_buffer.put("buffer_time", String.valueOf(source_pts));

					} // return to read next packet

  
					//write out last page in buffer
					if (!write_buffer.isEmpty() && write)
					{
						long out_time = (vptsdata ? video_pts_value[video_pts_value.length - 1] : source_pts) - time_difference;
						write_buffer.put("out_time", "" + out_time);

						while (true)
						{
							if ( !write_buffer.containsKey("active") || !write_buffer.containsKey("in_time") )
								break;

							switch (subtitle_type)
							{
							case 1:  // free
								print_buffer.print( "in_" + timeformat_1.format( new Date( Long.parseLong( write_buffer.get("in_time").toString()) / 90) ));
								print_buffer.println( "|out_" + timeformat_1.format( new Date( Long.parseLong( write_buffer.get("out_time").toString()) / 90) )); 
								break;

							case 2:  // SC
								print_buffer.print( Teletext.SMPTE(timeformat_1.format( new Date( Long.parseLong( write_buffer.get("in_time").toString()) / 90) ), (long)CommonParsing.getVideoFramerate()) + "&");
								print_buffer.print( Teletext.SMPTE(timeformat_1.format( new Date( Long.parseLong( write_buffer.get("out_time").toString()) / 90) ), (long)CommonParsing.getVideoFramerate()) + "#");
								break;

							case 3:  // SUB
								print_buffer.print( "{" + ( (long)(Long.parseLong( write_buffer.get("in_time").toString()) / CommonParsing.getVideoFramerate())) + "}");
								print_buffer.print( "{" + ( (long)(Long.parseLong( write_buffer.get("out_time").toString()) / CommonParsing.getVideoFramerate())) + "}");
								break;

							case 4:  // SRT
								print_buffer.println( "" + (seiten + 1));
								print_buffer.print( timeformat_2.format( new Date( Long.parseLong( write_buffer.get("in_time").toString()) / 90) ));
								print_buffer.println(" --> " + timeformat_2.format( new Date( Long.parseLong( write_buffer.get("out_time").toString()) / 90) ));
								break;

							case 5:  // SSA
								print_buffer.print( Teletext.getSSALine()[0] + timeformat_1.format( new Date( Long.parseLong( write_buffer.get("in_time").toString()) / 90) ).substring(1, 11) + ",");
								print_buffer.print( timeformat_1.format( new Date( Long.parseLong( write_buffer.get("out_time").toString()) / 90) ).substring(1, 11) + Teletext.getSSALine()[1]);
								break;

							case 7:  // STL
								print_buffer.print( Teletext.SMPTE(timeformat_1.format( new Date( Long.parseLong( write_buffer.get("in_time").toString()) / 90) ), (long)CommonParsing.getVideoFramerate()) + ",");
								print_buffer.print( Teletext.SMPTE(timeformat_1.format( new Date( Long.parseLong( write_buffer.get("out_time").toString()) / 90) ), (long)CommonParsing.getVideoFramerate()) + ",");
								break;

							case 6:  // SUP
								long sup_in_time = Long.parseLong( write_buffer.get("in_time").toString() );

								if ( sup_in_time >= 0 )
								{
									for (int a = 1; a < 24; a++)
										if ( write_buffer.containsKey("" + a) )
											picture_String.add(write_buffer.get("" + a));

									while (picture_String.size() > subpicture.getMaximumLines())
										picture_String.remove(0);

									subpicture.showPicTTX( picture_String.toArray(), job_processing.getStatusStrings());
									byte_buffer.write( subpicture.writeRLE( sup_in_time, 0xB4)); // alias duration 2.000 sec if out_time is missing

									out.write(subpicture.setTime( byte_buffer.toByteArray(), Long.parseLong( write_buffer.get("out_time").toString())));
								}

								picture_String.clear();
							}

							int b = 0;

							for (int a = 1; subtitle_type != 6 && a < 24; a++)
							{
								if ( !write_buffer.containsKey("" + a) )
									continue;

								String str = write_buffer.get("" + a).toString();

								switch (subtitle_type)
								{
								case 1:  // free
								case 4:  // SRT
									print_buffer.println(str); 
									break;

								case 2:  // SC
									print_buffer.print(str); 
									break;

								case 3:  // SUB
								case 7:  // STL
									print_buffer.print( (b > 0 ? "|" : "") + str);
									break;

								case 5:  // SSA
									print_buffer.print( (b > 0 ? "\\n" : "") + str);
								}

								b++;
							}

							if (subtitle_type != 6 && b > 0)
							{
								print_buffer.println();
								print_buffer.flush();
								byte_buffer.writeTo(out); 
							}

							seiten++;
							Common.getGuiInterface().showExportStatus(Resource.getString("teletext.status"), seiten);
							break;
						}
					}
 
					if (debug) 
						System.out.println();

					Common.setMessage(Resource.getString("teletext.msg.summary", "" + seiten, page));

					if (seiten>0) 
						Common.setMessage(Resource.getString("msg.newfile") + " " + ttxfile);

					//yield();

					write_buffer.clear();

					in.close();
					print_buffer.flush(); 
					print_buffer.close();
					byte_buffer.flush(); 
					byte_buffer.close();
					out.flush(); 
					out.close();

					File ttxfile1 = new File(ttxfile); 

					if (subtitle_type > 0 && seiten==0) 
						ttxfile1.delete();

					else if (subtitle_type == 0)
					{
						String pts_file = fparent + ".pts";

						RandomAccessFile log = new RandomAccessFile(pts_file, "rw");

						log.writeLong(0L); 
						log.writeLong(0L); 
						log.close();

						Common.setMessage(ttxfile);
						Common.setMessage(Resource.getString("working.file.mpeg.audio"));

						//yield();

						processAllAudio(collection, ttxfile, pts_file, "mp", "-1");      /* audiofile goes to synch methode */

						new File(ttxfile).delete();
						new File(pts_file).delete();

						//System.gc();

						return;
					}

					else
					{ 
						if (subtitle_type == 6)
							Ifo.createIfo(ttxfile, subpicture.getUserColorTableArray());

						job_processing.countMediaFilesExportLength(ttxfile1.length());
						job_processing.addSummaryInfo(Resource.getString("teletext.summary", "" + job_processing.countPictureStream(), "" + seiten, "" + page, infoPTSMatch(filename_pts, videofile_pts, vptsdata, ptsdata)) + "'" + ttxfile1 + "'");
					}

				} catch (IOException e2) { 

					Common.setExceptionMessage(e2);
				}

				//System.gc();
			}  // end for

			if (LB1 < 0) 
				break;

			else
			{ 
				subpicture.set2();
				Common.setMessage("");
				Common.setMessage(Resource.getString("teletext.msg.newrun"));
			}
		}  // end for

		Common.updateProgressBar(size, size);

		if (ShowSubpictureWindow)
			Common.getGuiInterface().hideSubpicture();

		//yield();

		//System.gc();
	}

	/**
	 * subpicture elementary stream
	 */
	private void parseSubpictureES(JobCollection collection, XInputFile xInputFile, String vptslog)
	{
		processSubpicture(collection, xInputFile, "-1", "sp", vptslog, CommonParsing.ES_TYPE);
	}


	private final String subdecode_errors[] = {
		"",
		"", // -1 = correct decoded dvb-subpicture segments, can export
		"", // -2 = correct decoded dvb-subpicture segments, w/o export
		Resource.getString("subpicture.msg.error3"), // -3 = error while decoding dvb-subpicture
		Resource.getString("subpicture.msg.error4"),
		Resource.getString("subpicture.msg.error5"),
		Resource.getString("subpicture.msg.error6"),
		Resource.getString("subpicture.msg.error7"),
		Resource.getString("subpicture.msg.error8"),
		Resource.getString("subpicture.msg.error9")
	};


	/**
	 * decoding subpicture stream
	 */
	private void processSubpicture(JobCollection collection, String filename, String filename_pts, String filename_type, String videofile_pts)
	{
		processSubpicture(collection, filename, filename_pts, filename_type, videofile_pts, 0);
	}

	/**
	 * decoding subpicture stream
	 */
	private void processSubpicture(JobCollection collection, String filename, String filename_pts, String filename_type, String videofile_pts, int isElementaryStream)
	{
		processSubpicture(collection, new XInputFile(new File(filename)), filename_pts, filename_type, videofile_pts, isElementaryStream);
	}

	/**
	 * decoding subpicture stream
	 */
	private void processSubpicture(JobCollection collection, XInputFile xInputFile, String filename_pts, String filename_type, String videofile_pts, int isElementaryStream)
	{
		Subpicture subpicture = Common.getSubpictureClass();

		JobProcessing job_processing = collection.getJobProcessing();

		String fchild = isElementaryStream == CommonParsing.ES_TYPE ? collection.getOutputName(xInputFile.getName()) : xInputFile.getName();
	//	String fchild = collection.getOutputName(xInputFile.getName());
		String fparent = collection.getOutputNameParent(fchild);

		fparent += isElementaryStream == CommonParsing.ES_TYPE ? "(new)" : "";

		String subfile = fparent + ".sup";

		long size = xInputFile.length();

		byte[] parse12 = new byte[12];
		byte[] packet = new byte[0];

		long count = 0;
		long startPoint = 0;
		long time_difference = 0;
		long display_time = 0;
		long source_pts = 0;
		long new_pts = 0;
		long first_pts = -1;
		long last_pts = 0;

		int x = 0;
		int pics = 0;
		int v = 0;
		int packetlength = 0;
		int export_type = 0;
		int last_pgc_set = 0;

		boolean vptsdata = false;
		boolean ptsdata = false;
		boolean write = false;
		boolean missing_syncword = false;
		boolean DVBpicture = false;
		boolean debug = collection.DebugMode();
		boolean KeepOriginalTimcode = Common.getSettings().getBooleanProperty(Keys.KEY_SubtitlePanel_keepOriginalTimcode);
		boolean UseAdditionalOffset = Common.getSettings().getBooleanProperty(Keys.KEY_additionalOffset);
		boolean ShowSubpictureWindow = Common.getSettings().getBooleanProperty(Keys.KEY_showSubpictureWindow);
		boolean Message_2 = Common.getSettings().getBooleanProperty(Keys.KEY_MessagePanel_Msg2);

		int AdditionalOffset_Value = Common.getSettings().getIntProperty(Keys.KEY_ExportPanel_additionalOffset_Value);

		String SubpictureColorModel = Common.getSettings().getProperty(Keys.KEY_SubpictureColorModel);
		String PageId_Value = Common.getSettings().getProperty(Keys.KEY_SubtitlePanel_PageId_Value);
		String SubtitleExportFormat = Common.getSettings().getProperty(Keys.KEY_SubtitleExportFormat);

		try {
			if (ShowSubpictureWindow)
				Common.getGuiInterface().showSubpicture();

			Hashtable user_table = new Hashtable();

			ArrayList subpicture_colormodel = Common.getColorModelsList();

			if (subpicture_colormodel.indexOf(SubpictureColorModel) > 2)
				user_table = Common.getUserColourTable(SubpictureColorModel);

			subpicture.reset();

			subpicture.dvb.setIRD(2<<subpicture_colormodel.indexOf(SubpictureColorModel), user_table, debug, PageId_Value);

			Common.setMessage(Resource.getString("subpicture.msg.model", SubpictureColorModel) + " " + PageId_Value);

			if (SubtitleExportFormat.equalsIgnoreCase(Keys.ITEMS_SubtitleExportFormat[6].toString()))
			{
				subfile = fparent + ".son";
				export_type = 1;
			}

			Common.setMessage(Resource.getString("subpicture.msg.output") + " " + subfile.substring(subfile.length() - 3));

			PushbackInputStream in = new PushbackInputStream(xInputFile.getInputStream(), 65536);

			IDDBufferedOutputStream out = new IDDBufferedOutputStream(new FileOutputStream(subfile),65536);

			PrintStream print_out = new PrintStream(out);

			Common.setMessage(Resource.getString("subpicture.msg.tmpfile", xInputFile.getName(), "" + size));

			Common.updateProgressBar(Resource.getString("subpicture.progress") + " " + xInputFile.getName(), 0, 0);


			long[] ptsval = {0};
			long[] ptspos = {0};
			long[] vptsval = {0};

			long pts_offset = UseAdditionalOffset ? 90L * AdditionalOffset_Value : 0;

			//System.gc();

			long[][] obj = loadTempOtherPts(filename_pts, "subpicture.msg.discard", "audio.msg.pts.firstonly", "subpicture.msg.pts.start_end", "", CommonParsing.SUBPICTURE, false, debug);

			if (obj != null)
			{
				ptsval = obj[0];
				ptspos = obj[1];
				ptsdata = true;
				obj = null;
			}

			ptsdata = true;

			obj = loadTempVideoPts(videofile_pts, debug);

			if (obj != null)
			{
				vptsval = obj[0];
				vptsdata = true;
				obj = null;
			}

			//System.gc();


			if (vptsdata && ptsdata)
			{
				int jump = checkPTSMatch(vptsval, ptsval);

				if (jump < 0)
				{
					Common.setMessage(Resource.getString("subpicture.msg.pts.mismatch"));  

					vptsdata = false; 
					x = 0; 
				}

				else
					x = jump;

			}

			if (vptsdata && ptsdata)
			{
				Common.setMessage(Resource.getString("subpicture.msg.adjust.at.video"));

				time_difference = vptsval[0];
			}

			if (!vptsdata && ptsdata)
			{
				Common.setMessage(Resource.getString("subpicture.msg.adjust.at.own"));

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
				count += in.skip(startPoint-count);

			//yield();


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

				in.read(parse12, 0, 12);

				if (parse12[0] != 0x53 || parse12[1] != 0x50) // find "SP"
				{
					if (Message_2 && !missing_syncword)
						Common.setMessage(Resource.getString("subpicture.msg.syncword.lost") + " " + count);

					missing_syncword = true;
					count++;

					in.unread(parse12, 1, 11);

					continue readloop;
				}

				if (Message_2 && missing_syncword)
					Common.setMessage(Resource.getString("subpicture.msg.syncword.found") + " " + count);

				in.unread(parse12, 0, 12);

				missing_syncword = false;

				packetlength = ((0xFF & parse12[10])<<8 | (0xFF & parse12[11])) + 10;
				packet = new byte[packetlength];

				in.read(packet);
				count += packetlength;

				source_pts = 0;

				for (int a = 0; a < 5; a++) // 5bytes for pts, maybe wrong
					source_pts |= (0xFFL & packet[2+a])<<(a*8);

				//DM15072004 081.7 int06 add, use add. time offset if not applied by class-piddemux (e.g. ES)
				if (filename_pts.equals("-1"))
					source_pts += pts_offset;

				if (first_pts == -1)
					first_pts = source_pts;

				if (source_pts == pts_offset)
					source_pts = last_pts;

				else
					last_pts = source_pts;

				if (debug)
					System.out.println(" " + (count - packetlength) + "/ " + packetlength + "/ " + source_pts);

				if (ptsdata)
				{
					write = !vptsdata;

					rangeloop:
					while (vptsdata && v < vptsval.length)  //pic_start_pts must be in range ATM
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
							time_difference += (vptsval[v] - vptsval[v-1]);
					}
				}
				else
					write = true;

				if (!vptsdata && time_difference == 0 && !KeepOriginalTimcode)
					time_difference = source_pts;

				new_pts = source_pts - time_difference;

				if ((display_time = subpicture.decode_picture(packet, 10, Common.getGuiInterface().isSubpictureVisible(), job_processing.getStatusStrings(), new_pts, write, Common.getGuiInterface().isSubpictureVisible())) < -2)
					Common.setMessage(Resource.getString("subpicture.msg.error", subdecode_errors[Math.abs((int)display_time)], "" + (count - packetlength)));

				if (debug)
					System.out.println("PTS: source " + Common.formatTime_1(source_pts / 90) + "(" + source_pts + ")" + " /new " + Common.formatTime_1(new_pts / 90) + "(" + new_pts + ")" + " / write: " + write + " / dec.state: " + display_time);

				if (display_time < 0)  //dvb_subpic
				{
					if (!DVBpicture)
						Common.setMessage(Resource.getString("subpicture.msg.dvbsource"));

					DVBpicture = true;

					if (display_time == -1) // -1 full data, -2 forced end_time
					{
						String num = "00000" + pics;
						String outfile_base = fparent + "_st" + num.substring(num.length() - 5);

						String key, object_id_str, outfile;
						int object_id;

						for (Enumeration e = BMP.getKeys(); e.hasMoreElements() ; )
						{
							key = e.nextElement().toString();
							object_id = Integer.parseInt(key);
							object_id_str = Integer.toHexString(object_id).toUpperCase();
							outfile = outfile_base + "p" + object_id_str;

							Bitmap bitmap = BMP.getBitmap(object_id);

							if (export_type == 0)  //.sup
								out.write( subpicture.writeRLE(bitmap));

							else    //.son + .bmp
							{
								if (pics == 0)
								{
									String[] SONhead = Teletext.getSONHead(new File(subfile).getParent(), (long)CommonParsing.getVideoFramerate());

									for (int a=0; a < SONhead.length; a++) 
										print_out.println(SONhead[a]);
								}

								subpicture.updateUserColorTable(bitmap);
								outfile = BMP.buildBMP_palettized(outfile, bitmap, subpicture.getUserColorTable(), 256);

								job_processing.countMediaFilesExportLength(new File(outfile).length());

								int pgc_values = subpicture.setPGClinks();

								// a change in color_links
								if ((0xFFFF & pgc_values) != (0xFFFF & last_pgc_set))
								{
									String pgc_colors = "";

									for (int a = 0; a < 4; a++)
										pgc_colors += "" + (0xF & pgc_values>>>(a * 4)) + " ";

									print_out.println("Color\t\t(" + pgc_colors.trim() + ")");
								}

								// a change in alpha_links
								if ((0xFFFF0000 & pgc_values) != (0xFFFF0000 & last_pgc_set))
								{
									String pgc_alphas = "";

									for (int a = 0; a < 4; a++)
										pgc_alphas += "" + (0xF & pgc_values>>>((4 + a) * 4)) + " ";

									print_out.println("Contrast\t(" + pgc_alphas.trim() + ")");
								}

								last_pgc_set = pgc_values;

								print_out.println("Display_Area\t(" + Common.adaptString(bitmap.getX(), 3) + " " + Common.adaptString(bitmap.getY(), 3) + " " + Common.adaptString(bitmap.getMaxX(), 3) + " " + Common.adaptString(bitmap.getMaxY(), 3) + ")");
								print_out.println(outfile_base.substring(outfile_base.length() - 4) + "\t\t" + Common.formatTime_2(bitmap.getInTime() / 90, (long)CommonParsing.getVideoFramerate()) + "\t" + Common.formatTime_2((bitmap.getInTime() / 90) + (bitmap.getPlayTime() * 10), (long)CommonParsing.getVideoFramerate()) + "\t" + new File(outfile).getName());
							}

							//Common.setMessage(subpicture.getArea());
							//BMP.buildBMP_24bit(outfile, key);

							Common.getGuiInterface().setSubpictureTitle(" " + Resource.getString("subpicture.preview.title.dvbexport", "" + bitmap.getPageId(), "" + pics, Common.formatTime_1(new_pts / 90)) + " " + Common.formatTime_1(bitmap.getPlayTime() * 10));
						}

						if (!BMP.isEmpty())
							Common.getGuiInterface().showExportStatus(Resource.getString("subpicture.status"), ++pics);

						BMP.clear();
					}
				}

				else if (write) //dvd_subpic
				{
					for (int a = 0; a < 8; a++)
						packet[2 + a] = (byte)(0xFFL & new_pts>>>(a * 8));

					//later, to allow overlapping on cut boundaries 
					//if (display_time > 0)
					//	packet = subpicture.setTime(packet,display_time);

					out.write(packet);

					Common.getGuiInterface().showExportStatus(Resource.getString("subpicture.status"), ++pics);
					Common.getGuiInterface().setSubpictureTitle(" " + Resource.getString("subpicture.preview.title.dvdexport", "" + pics, Common.formatTime_1(new_pts / 90)) + " " + Common.formatTime_1(display_time / 90));

					String str = subpicture.isForced_Msg();

					if (str != null)
						Common.setMessage(str + " " + Resource.getString("subpicture.msg.forced") + " " + pics);
				}

				else
					Common.getGuiInterface().setSubpictureTitle(" " + Resource.getString("subpicture.preview.title.noexport"));

				if (debug)
					System.out.println(" -> " + write + "/ " + v + "/ " + new_pts + "/ " + time_difference + "/ " + pics + "/ " + display_time);
			}

			in.close();

			print_out.flush();
			print_out.close();

			out.flush(); 
			out.close();

			if (filename_pts.equals("-1"))
				Common.setMessage(Resource.getString("subpicture.msg.pts.start_end", Common.formatTime_1(first_pts / 90)) + " " + Common.formatTime_1(source_pts / 90));

			Common.setMessage(Resource.getString("subpicture.msg.summary", "" + pics));

			if (!DVBpicture && export_type == 1)
			{
				String renamed_file = subfile.substring(0, subfile.length() - 3) + "sup";
				Common.renameTo(subfile, renamed_file);
				subfile = renamed_file;
			}

			File subfile1 = new File(subfile); 

			if (pics == 0) 
				subfile1.delete();

			else
			{ 
				if (DVBpicture && export_type == 0)
					job_processing.countMediaFilesExportLength(Ifo.createIfo(subfile, subpicture.getUserColorTableArray()));

				else if (DVBpicture && export_type == 1)
					job_processing.countMediaFilesExportLength(new File( BMP.write_ColorTable(fparent, subpicture.getUserColorTable(), 256)).length());

				Common.setMessage(Resource.getString("msg.newfile") + " " + subfile);
				job_processing.countMediaFilesExportLength(subfile1.length());
				job_processing.addSummaryInfo(Resource.getString("subpicture.summary", "" + job_processing.countPictureStream(), "" + pics, infoPTSMatch(filename_pts, videofile_pts, vptsdata, ptsdata)) + "'" + subfile1 + "'");
			}

			Common.updateProgressBar(size, size);

			//yield();

		} catch (IOException e2) { 

			Common.setExceptionMessage(e2);
		}

		if (ShowSubpictureWindow)
			Common.getGuiInterface().hideSubpicture();

		//System.gc();
	}


	/**
	 * LPCM stream
	 */
	private void processLPCM(JobCollection collection, String filename, String filename_pts, String filename_type, String videofile_pts)
	{
		processLPCM(collection, filename, filename_pts, filename_type, videofile_pts, 0);
	}

	/**
	 * LPCM stream
	 */
	private void processLPCM(JobCollection collection, String filename, String filename_pts, String filename_type, String videofile_pts, int isElementaryStream)
	{
		processLPCM(collection, new XInputFile(new File(filename)), filename_pts, filename_type, videofile_pts, isElementaryStream);
	}

	/**
	 * LPCM stream
	 */
	private void processLPCM(JobCollection collection, XInputFile xInputFile, String filename_pts, String filename_type, String videofile_pts, int isElementaryStream)
	{

		String fchild = collection.getOutputName(xInputFile.getName());
		String fparent = collection.getOutputNameParent(fchild);

		JobProcessing job_processing = collection.getJobProcessing();

		String pcmfile = fparent + (isElementaryStream == CommonParsing.ES_TYPE ? "(new)": "") + ".wav";

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
		boolean debug = collection.DebugMode();
		boolean message_2 = Common.getSettings().getBooleanProperty(Keys.KEY_MessagePanel_Msg2);

		int samples = 0;
		int x = 0;
		int v = 0;
		int packetlength = 0;
		int parserlength = parser.length;

		Audio LPCM_Audio = new Audio();

		//System.gc();

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

			//yield();

			out.write( LPCM_Audio.getRiffHeader()); //wav header

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

				if (LPCM_Audio.LPCM_parseHeader(parser, 10) < 0)
					continue readloop;

				if (LPCM_Audio.LPCM_compareHeader() > 0 || samples == 0 )
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
							Common.setMessage(Resource.getString("lpcm.msg.source", LPCM_Audio.LPCM_displayHeader()) + " " + Common.formatTime_1( (long)(new_pts / 90.0f)));

						else if (ModeChangeCount == 100) 
							Common.setMessage(Resource.getString("lpcm.msg.source.max"));

						ModeChangeCount++;
						newformat = false;

						//yield();
					}

					for (int a = 0; a < packetlength; a += 2) // intel order
						Common.changeByteOrder(packet, a, 2);

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
				job_processing.addSummaryInfo(Resource.getString("lpcm.summary", "" + job_processing.countPictureStream(), "" + samples, Common.formatTime_1(playtime)) + infoPTSMatch(filename_pts, videofile_pts, vptsdata, ptsdata) + "\t'" + pcmfile1 + "'");
			}

			//yield();

		} catch (IOException e2) { 

			Common.setExceptionMessage(e2);
		}

		//System.gc();
	}


	/**
	 * check audio ES
	 */
	private void parseAudioES(JobCollection collection, XInputFile aXInputFile, String vptslog, String type)
	{
		String fchild = collection.getOutputName(aXInputFile.getName());
		String fparent = collection.getOutputNameParent(fchild);

		String audio_pts = fparent + ".pts";

		CommonParsing.logAlias(collection.getJobProcessing(), vptslog, audio_pts);

		/**
		 * audiofile goes to synch methode 
		 */
		processAllAudio(collection, aXInputFile, audio_pts, type, vptslog, CommonParsing.ES_TYPE);      

		new File(audio_pts).delete();
	}



	/**
	 * check video ES
	 */
	private String parseVideoES(JobCollection collection, XInputFile aXInputFile)
	{
		String fchild = collection.getOutputName(aXInputFile.getName());
		String fparent = collection.getOutputNameParent(fchild);

		JobProcessing job_processing = collection.getJobProcessing();

		/**
		 * split part 
		 */
		fparent += job_processing.getSplitSize() > 0 ? "(" + job_processing.getSplitPart() + ")" : "(new)" ;

		boolean CreateM2sIndex = Common.getSettings().getBooleanProperty(Keys.KEY_ExternPanel_createM2sIndex);
		boolean CreateD2vIndex = Common.getSettings().getBooleanProperty(Keys.KEY_ExternPanel_createD2vIndex);
		boolean SplitProjectFile = Common.getSettings().getBooleanProperty(Keys.KEY_ExternPanel_splitProjectFile);
		boolean WriteVideo = Common.getSettings().getBooleanProperty(Keys.KEY_WriteOptions_writeVideo);
		boolean AddSequenceEndcode = Common.getSettings().getBooleanProperty(Keys.KEY_VideoPanel_addEndcode);
		boolean RenameVideo = Common.getSettings().getBooleanProperty(Keys.KEY_ExternPanel_renameVideo);
		boolean Debug = collection.DebugMode();

		boolean first = true;
		boolean doWrite = true;
		boolean lead_sequenceheader = false;

		byte[] vgl = new byte[4];
		byte[] vptsbytes = new byte[16];
		byte[] vload = new byte[0];
		byte[] es_packet;

		long filelength = aXInputFile.length();
		long pos = 0;
		long pts = 0;
		long startPoint = 0;

		int CutMode =  Common.getSettings().getIntProperty(Keys.KEY_CutMode);
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
		String logfile = "-1";

		Common.updateProgressBar(Resource.getString("video.progress") + " " + fchild, 0, 0);

		//yield();

		StreamDemultiplexer streamdemultiplexer = new StreamDemultiplexer();

		try {

			PushbackInputStream in = new PushbackInputStream(aXInputFile.getInputStream(), 4);

			IDDBufferedOutputStream vstream = new IDDBufferedOutputStream( new FileOutputStream(fparent + ".s1"), MainBufferSize);

			if (CreateM2sIndex)
				vstream.InitIdd(fparent, 1);

			DataOutputStream vlog = new DataOutputStream( new FileOutputStream(fparent + ".s1.pts") ); 
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
				startPoint = job_processing.getLastHeaderBytePosition() - (1048576L * (Common.getSettings().getIntProperty(Keys.KEY_ExportPanel_Overlap_Value) + 1)); //go back for overlapping output
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

						if (!first)
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

						diff = (vload.length - mark - 4 < 2500) ? (vload.length - mark - 4) : 2500;

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
			Common.setMessage(Resource.getString("video.msg.summary") + " " + job_processing.getExportedVideoFrameNumber() + "/ " + clv[0] + "/ " + clv[1] + "/ " + clv[2] + "/ " + clv[3] + "/ " + clv[4]);

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

				logfile = fparent + ".s1.pts";

				CommonParsing.setVideoHeader(job_processing, videofile, logfile, clv, MPGVideotype);
			}

			if (CreateM2sIndex)
			{
				if (new File(videofile).exists())
					vstream.renameVideoIddTo(fparent);

				else
					vstream.deleteIdd();
			}

		} catch (IOException e) {

			Common.setExceptionMessage(e);
		}

		//System.gc();

		return logfile;
	}

}
