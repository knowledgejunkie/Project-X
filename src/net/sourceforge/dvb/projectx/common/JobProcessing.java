/*
 * @(#)JobProcessing.java - used by a collection when processing it
 *
 * Copyright (c) 2005-2013 by dvb.matt, All Rights Reserved.
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

package net.sourceforge.dvb.projectx.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Hashtable;

import net.sourceforge.dvb.projectx.parser.Gop;
import net.sourceforge.dvb.projectx.parser.GopArray;

import net.sourceforge.dvb.projectx.thirdparty.D2V;
import net.sourceforge.dvb.projectx.thirdparty.Chapters;

/**
 * saves all stuff of a collection process
 */
public class JobProcessing extends Object {

	private Gop gop;
	private GopArray gop_array;
	private D2V d2v;
	private Chapters chapters;

	private List TSPidlist;
	private List PVAPidlist;
	private List TSdemuxlist;
	private List PVAdemuxlist;
	private List VDRdemuxlist;
	private List PESdemuxlist;
	private List TempFileList;
	private List InfoAtEnd;
	private List CellTimesList;
//
	private Hashtable StreamObjects;

	private Object[] AudioStreamInfo;
	private Object[] SubpictureStreamInfo;
//
	private int[] stream_number;
	private int[] clv;

	private int SourceVideoFrameNumber;
	private int ExportedVideoFrameNumber;
	private int FileNumber;
	private int NoOfAudio;
	private int NoOfPictures;
	private int NoOfTTX;
	private int MinBitrateValue;
	private int MaxBitrateValue;
	private int SplitPartNumber;

	private boolean newvideo;
	private boolean PureVideo;
	private boolean hasSequenceHeader;
	private boolean SplitLoopActive;
	private boolean runningFromCLI;

	private String[] VBASIC;
	private String savedOutputDirectory;

	private long firstVideoPTS;
	private long CUT_BYTEPOSITION;
	private long pva_videopts;
	private long fakedPTS;
	private long LastHeader_BytePosition;
	private long NextFileStartPts;
	private long CutComparePoint;
	private long VideoExportTimeCounter;
	private long VideoExportTimeSummary;
	private long LastGopTimecode;
	private long LastGopPts;
	private long FirstAudioPts;
	private long LastSimplifiedPts;
	private long EndPtsOfGop;
	private long ProjectFileExportLength;
	private long MediaFilesExportLength;
	private long AllMediaFilesExportLength;
	private long SplitPartSize;
	private long ProjectFileSplitSize;

	/**
	 *
	 */
	public JobProcessing(JobCollection collection, boolean b, String str)
	{
		runningFromCLI = b;
		savedOutputDirectory = str;

		startProcessing(collection);
	}

	/**
	 * init the process and all variables
	 */
	private void startProcessing(JobCollection collection)
	{
		TSPidlist = new ArrayList();
		PVAPidlist = new ArrayList();
		TSdemuxlist = new ArrayList();
		PVAdemuxlist = new ArrayList();
		VDRdemuxlist = new ArrayList();
		PESdemuxlist = new ArrayList();
		TempFileList = new ArrayList();
		InfoAtEnd = new ArrayList();
		CellTimesList = new ArrayList();
//
		StreamObjects = new Hashtable();
//
		clv = new int[10];
		stream_number = new int[10]; 

		gop = new Gop(collection);
		gop_array = new GopArray();
		d2v = new D2V();
		chapters = new Chapters();

		SourceVideoFrameNumber = 0;
		ExportedVideoFrameNumber = 0;
		FileNumber = 0;
		newvideo = true;
		firstVideoPTS = -1;
		PureVideo = false;
		VBASIC = new String[5];
		CUT_BYTEPOSITION = 0;
		hasSequenceHeader = true;
		pva_videopts = -1;
		NoOfAudio = 0;
		NoOfPictures = 0;
		NoOfTTX = 0;
		fakedPTS = -1;
		LastHeader_BytePosition = 0;
		NextFileStartPts = 0;
		CutComparePoint = 0;
		VideoExportTimeCounter = 0;
		VideoExportTimeSummary = 0;
		LastGopTimecode = 0;
		LastGopPts = 0;
		FirstAudioPts = 0;
		SplitLoopActive = true;
		MinBitrateValue = 262143;
		MaxBitrateValue = 0;
		LastSimplifiedPts = 0;
		SplitPartNumber = 0;
		SplitPartSize = 0;
		EndPtsOfGop = 0;
		ProjectFileSplitSize = 0;
		ProjectFileExportLength = 0;
		MediaFilesExportLength = 0;
		AllMediaFilesExportLength = 0;
	}

	/**
	 * finish the process and all objects
	 */
	public void finishProcessing()
	{
		TSPidlist = null;
		PVAPidlist = null;
		TSdemuxlist = null;
		PVAdemuxlist = null;
		VDRdemuxlist = null;
		PESdemuxlist = null;
		TempFileList = null;
		InfoAtEnd = null;
		CellTimesList = null;
		clv = null;
		stream_number = null; 
//
		StreamObjects = null;
//
		gop = null;
		gop_array = null;
		d2v = null;
		chapters = null;

		VBASIC = null;
	}

	/**
	 *
	 */
	public boolean isRunningFromCLI()
	{
		return runningFromCLI;
	}

	/**
	 * 
	 */
	public List getTSPidList()
	{
		return TSPidlist;
	}

	/**
	 * 
	 */
	public List getPVAPidList()
	{
		return PVAPidlist;
	}

	/**
	 * 
	 */
	public List getTSDemuxList()
	{
		return TSdemuxlist;
	}

	/**
	 * 
	 */
	public List getPVADemuxList()
	{
		return PVAdemuxlist;
	}

	/**
	 * 
	 */
	public List getPrimaryPESDemuxList()
	{
		return PESdemuxlist;
	}

	/**
	 * 
	 */
	public List getSecondaryPESDemuxList()
	{
		return PESdemuxlist;
	}

	/**
	 * 
	 */
	public List getTemporaryFileList()
	{
		return TempFileList;
	}

	/**
	 * 
	 */
	public List getSummaryInfo()
	{
		return InfoAtEnd;
	}

	/**
	 * 
	 */
	public void clearSummaryInfo()
	{
		InfoAtEnd.clear();
	}

	/**
	 * 
	 */
	public void addSummaryInfo(String str)
	{
		InfoAtEnd.add(str);
	}

	/**
	 * 
	 */
	public List getCellTimes()
	{
		return CellTimesList;
	}

	/**
	 * 
	 */
	public void addCellTime(String str)
	{
		CellTimesList.add(str);
	}

	/**
	 * 
	 */
	public void addCellTime(int value)
	{
		addCellTime(String.valueOf(value));
	}

	/**
	 * 
	 */
	public int[] getStatusVariables()
	{
		return clv;
	}

	/**
	 * 
	 */
	public void clearStatusVariables()
	{
		Arrays.fill(clv, 0);
	}

	/**
	 * 
	 */
	public int[] getStreamNumbers()
	{
		return stream_number;
	}

	/**
	 * 
	 */
	public void clearStreamNumbers()
	{
		Arrays.fill(stream_number, 0);
	}

	/**
	 * 
	 */
	public String[] getStatusStrings()
	{
		return VBASIC;
	}

	/**
	 * 
	 */
	public void clearStatusStrings()
	{
		Arrays.fill(VBASIC, null);
	}

	/**
	 * 
	 */
	public Gop getGop()
	{
		return gop;
	}

	/**
	 * 
	 */
	public D2V getProjectFileD2V()
	{
		return d2v;
	}

	/**
	 * 
	 */
	public Chapters getChapters()
	{
		return chapters;
	}

	/**
	 * 
	 */
	public void setSourceVideoFrameNumber(int val)
	{
		SourceVideoFrameNumber = val;
	}
		
	/**
	 * 
	 */
	public int countSourceVideoFrameNumber(int val)
	{
		SourceVideoFrameNumber += val;

		return SourceVideoFrameNumber;
	}

	/**
	 * 
	 */
	public int getSourceVideoFrameNumber()
	{
		return SourceVideoFrameNumber;
	}

	/**
	 * 
	 */
	public void setFileNumber(int val)
	{
		FileNumber = val;
	}
		
	/**
	 * 
	 */
	public int countFileNumber(int val)
	{
		FileNumber += val;

		return FileNumber;
	}

	/**
	 * 
	 */
	public int getFileNumber()
	{
		return FileNumber;
	}

	/**
	 * 
	 */
	public boolean isNewVideoStream()
	{
		return newvideo;
	}

	/**
	 * 
	 */
	public void setNewVideoStream(boolean b)
	{
		newvideo = b;
	}

	/**
	 * 
	 */
	public long get1stVideoPTS()
	{
		return firstVideoPTS;
	}

	/**
	 * 
	 */
	public void set1stVideoPTS(long val)
	{
		firstVideoPTS = val;
	}

	/**
	 * 
	 */
	public boolean hasElementaryVideoStream()
	{
		return PureVideo;
	}

	/**
	 * 
	 */
	public void setElementaryVideoStream(boolean b)
	{
		PureVideo = b;
	}

	/**
	 * 
	 */
	public long getCutByteposition()
	{
		return CUT_BYTEPOSITION;
	}

	/**
	 * 
	 */
	public void setCutByteposition(long val)
	{
		CUT_BYTEPOSITION = val;
	}

	/**
	 * 
	 */
	public boolean hasSequenceHeader()
	{
		return hasSequenceHeader;
	}

	/**
	 * 
	 */
	public void setSequenceHeader(boolean b)
	{
		hasSequenceHeader = b;
	}

	/**
	 * 
	 */
	public GopArray getGopArray()
	{
		return gop_array;
	}

	/**
	 * 
	 */
	public long getPvaVideoPts()
	{
		return pva_videopts;
	}

	/**
	 * 
	 */
	public void setPvaVideoPts(long val)
	{
		pva_videopts = val;
	}

	/**
	 * 
	 */
	public void clearSubStreamCounters()
	{
		NoOfAudio = 0;
		NoOfPictures = 0;
		NoOfTTX = 0;
	}

	/**
	 * 
	 */
	public int countAudioStream()
	{
		return (NoOfAudio++);
	}

	/**
	 * 
	 */
	public int countPictureStream()
	{
		return (NoOfPictures++);
	}

	/**
	 * 
	 */
	public int countTeletextStream()
	{
		return (NoOfTTX++);
	}

	/**
	 * 
	 */
	public long getBorrowedPts()
	{
		return fakedPTS;
	}

	/**
	 * 
	 */
	public void setBorrowedPts(long val)
	{
		fakedPTS = val;
	}

	/**
	 * 
	 */
	public long getLastHeaderBytePosition()
	{
		return LastHeader_BytePosition;
	}

	/**
	 * 
	 */
	public void setLastHeaderBytePosition(long val)
	{
		LastHeader_BytePosition = val;
	}

	/**
	 * 
	 */
	public long getNextFileStartPts()
	{
		return NextFileStartPts;
	}

	/**
	 * 
	 */
	public void setNextFileStartPts(long val)
	{
		NextFileStartPts = val;
	}

	/**
	 * 
	 */
	public long getCutComparePoint()
	{
		return CutComparePoint;
	}

	/**
	 * 
	 */
	public void setCutComparePoint(long val)
	{
		CutComparePoint = val;
	}

	/**
	 * 
	 */
	public void setVideoExportTime(long val)
	{
		VideoExportTimeCounter = val;
	}
		
	/**
	 * 
	 */
	public long countVideoExportTime(long val)
	{
		VideoExportTimeCounter += val;

		return VideoExportTimeCounter;
	}

	/**
	 * 
	 */
	public long getVideoExportTime()
	{
		return VideoExportTimeCounter;
	}

	/**
	 * 
	 */
	public void setVideoExportTimeSummary(long val)
	{
		VideoExportTimeSummary = val;
	}

	/**
	 * 
	 */
	public long countVideoExportTimeSummary(long val)
	{
		VideoExportTimeSummary += val;

		return VideoExportTimeSummary;
	}
		
	/**
	 * 
	 */
	public long getVideoExportTimeSummary()
	{
		return VideoExportTimeSummary;
	}

	/**
	 * 
	 */
	public void setLastGopTimecode(long val)
	{
		LastGopTimecode = val;
	}
		
	/**
	 * 
	 */
	public long countLastGopTimecode(long val)
	{
		LastGopTimecode += val;

		return LastGopTimecode;
	}

	/**
	 * 
	 */
	public long getLastGopTimecode()
	{
		return LastGopTimecode;
	}

	/**
	 * 
	 */
	public void setLastGopPts(long val)
	{
		LastGopPts = val;
	}
		
	/**
	 * 
	 */
	public long getLastGopPts()
	{
		return LastGopPts;
	}

	/**
	 * 
	 */
	public void setFirstAudioPts(long val)
	{
		FirstAudioPts = val;
	}
		
	/**
	 * 
	 */
	public long getFirstAudioPts()
	{
		return FirstAudioPts;
	}

	/**
	 * 
	 */
	public void setSplitLoopActive(boolean b)
	{
		SplitLoopActive = b;
	}
		
	/**
	 * 
	 */
	public boolean isSplitLoopActive()
	{
		return SplitLoopActive;
	}

	/**
	 * determined bitrates
	 */
	public void setMinBitrate(int val)
	{
		MinBitrateValue = val;
	}
		
	/**
	 * determined bitrates
	 */
	public int getMinBitrate()
	{
		return MinBitrateValue;
	}

	/**
	 * determined bitrates
	 */
	public void setMaxBitrate(int val)
	{
		MaxBitrateValue = val;
	}
		
	/**
	 * determined bitrates
	 */
	public int getMaxBitrate()
	{
		return MaxBitrateValue;
	}

	/**
	 * pts build from video es
	 */
	public void setLastSimplifiedPts(long val)
	{
		LastSimplifiedPts = val;
	}
		
	/**
	 * pts build from video es
	 */
	public long getLastSimplifiedPts()
	{
		return LastSimplifiedPts;
	}

	/**
	 * 
	 */
	public void setSplitPart(int val)
	{
		SplitPartNumber = val;
	}
		
	/**
	 * 
	 */
	public int getSplitPart()
	{
		return SplitPartNumber;
	}

	/**
	 * 
	 */
	public void setSplitSize(long val)
	{
		SplitPartSize = val;
	}
		
	/**
	 * 
	 */
	public long getSplitSize()
	{
		return SplitPartSize;
	}

	/**
	 * 
	 */
	public void setEndPtsOfGop(long val)
	{
		EndPtsOfGop = val;
	}
		
	/**
	 * 
	 */
	public long getEndPtsOfGop()
	{
		return EndPtsOfGop;
	}

	/**
	 * 
	 */
	public void setProjectFileSplitSize(long val)
	{
		ProjectFileSplitSize = val;
	}
		
	/**
	 * 
	 */
	public long getProjectFileSplitSize()
	{
		return ProjectFileSplitSize;
	}

	/**
	 * 
	 */
	public void setProjectFileExportLength(long val)
	{
		ProjectFileExportLength = val;
	}
		
	/**
	 * 
	 */
	public long countProjectFileExportLength(long val)
	{
		ProjectFileExportLength += val;

		return ProjectFileExportLength;
	}

	/**
	 * 
	 */
	public long getProjectFileExportLength()
	{
		return ProjectFileExportLength;
	}

	/**
	 * 
	 */
	public void setExportedVideoFrameNumber(int val)
	{
		ExportedVideoFrameNumber = val;
	}
		
	/**
	 * 
	 */
	public int countExportedVideoFrameNumber(int val)
	{
		ExportedVideoFrameNumber += val;

		return ExportedVideoFrameNumber;
	}

	/**
	 * 
	 */
	public int getExportedVideoFrameNumber()
	{
		return ExportedVideoFrameNumber;
	}

	/**
	 * 
	 */
	public void setMediaFilesExportLength(long val)
	{
		MediaFilesExportLength = val;
	}
		
	/**
	 * 
	 */
	public long countMediaFilesExportLength(long val)
	{
		MediaFilesExportLength += val;

		return MediaFilesExportLength;
	}

	/**
	 * 
	 */
	public long getMediaFilesExportLength()
	{
		return MediaFilesExportLength;
	}

	/**
	 * 
	 */
	public void setAllMediaFilesExportLength(long val)
	{
		AllMediaFilesExportLength = val;
	}
		
	/**
	 * 
	 */
	public long countAllMediaFilesExportLength(long val)
	{
		AllMediaFilesExportLength += val;

		return AllMediaFilesExportLength;
	}

	/**
	 * 
	 */
	public long getAllMediaFilesExportLength()
	{
		return AllMediaFilesExportLength;
	}

	/**
	 * 
	 */
	public String getSavedOutputDirectory()
	{
		return savedOutputDirectory;
	}

	/**
	 * 
	 */
	public Hashtable getStreamObjects()
	{
		return StreamObjects;
	}

	/**
	 * 
	 */
	public void setAudioStreamInfo(Object[] obj)
	{
		AudioStreamInfo = obj;
	}

	/**
	 * 
	 */
	public String getAudioStreamLanguage(int pid)
	{
		String str = "";
		String str1 = "";

		if (AudioStreamInfo == null)
			return str;

		for (int i = 0, j = 0, k = 0; i < AudioStreamInfo.length; i++)
		{
			str1 = AudioStreamInfo[i].toString();

			if (str1.indexOf(Common.adaptString(Integer.toHexString(pid).toUpperCase(), 4)) < 0)
				continue;

			j = str1.indexOf("{");
			k = str1.indexOf("}");

			if (j > 0 && k > j)
				str = "_" + str1.substring(j + 1, k);

			break;
		}

		return str;
	}

	/**
	 * 
	 */
	public void setSubpictureStreamInfo(Object[] obj)
	{
		SubpictureStreamInfo = obj;
	}

	/**
	 * 
	 */
	public String getSubpictureStreamLanguage(int pid)
	{
		String str = "";
		String str1 = "";

		if (SubpictureStreamInfo == null)
			return str;

		for (int i = 0, j = 0, k = 0; i < SubpictureStreamInfo.length; i++)
		{
			str1 = SubpictureStreamInfo[i].toString();

			if (str1.indexOf(Common.adaptString(Integer.toHexString(pid).toUpperCase(), 4)) < 0)
				continue;

			j = str1.indexOf("{");
			k = str1.indexOf("}");

			if (j > 0 && k > j)
				str = "_" + str1.substring(j + 1, k);

			break;
		}

		return str;
	}
}
