/*
 * @(#)JobCollection.java - all about a collection
 *
 * Copyright (c) 2005-2008 by dvb.matt, All Rights Reserved.
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
import java.util.List;
import java.util.Hashtable;

import java.io.File;
import java.io.RandomAccessFile;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.ByteArrayInputStream;

import net.sourceforge.dvb.projectx.xinput.XInputFile;

import net.sourceforge.dvb.projectx.parser.Gop;
import net.sourceforge.dvb.projectx.parser.GopArray;
import net.sourceforge.dvb.projectx.parser.CommonParsing;


/**
 * saves all stuff of a collection
 */
public class JobCollection extends Object {

	private List cut_points = null;
	private List chapter_points = null;
	private List input_files = null;
	private List predefined_IDs = null;

	private String output_directory = null;
	private String output_name = null;
	private String normalLog = null;

	private String file_separator = System.getProperty("file.separator");
	private String line_separator = System.getProperty("line.separator");

	private boolean debug = false;
	private boolean progress_status = false;

	private int primaryInputFileSegments = 0;

	private int action_type = -1;

	private PrintStream logging;

	private JobProcessing job_processing;

	private Hashtable cut_images;

	private Settings settings;

	/**
	 *
	 */
	private JobCollection()
	{}

	/**
	 *
	 */
	public JobCollection(String _output_directory)
	{
		cut_points = new ArrayList();
		chapter_points = new ArrayList();
		input_files = new ArrayList();
		predefined_IDs = new ArrayList();

		init(_output_directory, "", action_type);
	}

	/**
	 * 
	 */
	public JobCollection(String _output_directory, String _output_name, int _action_type, List _cut_points, List _chapter_points, List _input_files, List _predefined_IDs)
	{
		cut_points = copyListElements(_cut_points);
		chapter_points = copyListElements(_chapter_points);
		input_files = copyListElements(_input_files);
		predefined_IDs = copyListElements(_predefined_IDs);

		init(_output_directory, _output_name, _action_type);
	}

	/**
	 * 
	 */
	private void init(String _output_directory, String _output_name, int _action_type)
	{
		setOutputDirectory(_output_directory);
		setOutputName(_output_name);
		normalLog = "";
		action_type = _action_type;

		cut_images = new Hashtable();
	}

	/**
	 * 
	 */
	private List copyListElements(List sourceList)
	{
		List list = new ArrayList();

		for (int i = 0; i < sourceList.size(); i++)
			list.add(sourceList.get(i));

		return list;
	}

	/**
	 * 
	 */
	public JobCollection getNewInstance()
	{
		return (new JobCollection(output_directory, output_name, action_type, cut_points, chapter_points, input_files, predefined_IDs));
	}

	/**
	 * init the process and all variables
	 */
	public void startProcessing(boolean b)
	{
		progress_status = true;

		job_processing = new JobProcessing(this, b, getOutputDirectory());
	}

	/**
	 * finish the process and all objects
	 */
	public void finishProcessing()
	{
		progress_status = false;

		if (job_processing != null)
		{
			job_processing.finishProcessing();
			setOutputDirectory(job_processing.getSavedOutputDirectory());
		}

		job_processing = null;
	}

	/**
	 * check if a process from this coll is running
	 * hinder some modifications of files and so on
	 */
	public boolean isActive()
	{
		return progress_status;
	}

	/**
	 *
	 */
	public String getFileSeparator()
	{
		return file_separator;
	}

	/**
	 * 
	 */
	public JobProcessing getJobProcessing()
	{
		return job_processing;
	}

	/**
	 *
	 */
	public void addInputFile(Object input)
	{
		addInputFile(-1, input);
	}

	/**
	 *
	 */
	public void addInputFile(Object[] input)
	{
		for (int i = 0; i < input.length; i++) 
			addInputFile(input[i]);
	}

	/**
	 *
	 */
	public void addInputFile(int index, Object input)
	{
		if (isActive())
			return;

		if (index < 0)
			index = input_files.size();

		XInputFile xInputFile = ((XInputFile) input).getNewInstance();

		xInputFile.setFileID(Common.getNewFileID());

		input_files.add(index, xInputFile);
	//	input_files.add(index, input);

		determinePrimaryFileSegments();
	}

	/**
	 *
	 */
	public Object removeInputFile(int index)
	{
		if (isActive())
			return null;

		if (index < 0 || index >= getInputFilesCount())
			return null;

		Object obj = input_files.remove(index);

		determinePrimaryFileSegments();

		return obj;
	}

	/**
	 * remove file index, start with last
	 */
	public Object[] removeInputFile(int[] index)
	{
		if (isActive())
			return null;

		Object[] objects = new Object[index.length];

		for (int i = index.length - 1; i >= 0; i--) 
			objects[i] = removeInputFile(index[i]);

		return objects;
	}

	/**
	 *
	 */
	public void determinePrimaryFileSegments()
	{
		int primaryFilesCount = 0;  // at least one file is primary
		boolean completed = false;
		XInputFile xInputFile;

		filesearch:
		for (int i = 0, j = -1, stream_type = -1, k = getInputFilesCount(); i < k; i++)
		{
			xInputFile = (XInputFile) getInputFile(i);

			if (xInputFile.getStreamInfo() == null)
				Common.getScanClass().getStreamInfo(xInputFile);

			// continue loop to scan all files for later use
			if (completed)
				continue;

			stream_type = xInputFile.getStreamInfo().getStreamType();

			switch (stream_type)
			{
			case CommonParsing.TS_TYPE:
			case CommonParsing.PES_AV_TYPE:
			case CommonParsing.PES_MPA_TYPE:
			case CommonParsing.PES_PS1_TYPE:
			case CommonParsing.MPEG1PS_TYPE:
			case CommonParsing.MPEG2PS_TYPE:
			case CommonParsing.PVA_TYPE:
				if (j != -1 && j != stream_type)
				{
					completed = true;
					continue;
				}
				break;

			default:
				continue;
			}

			j = stream_type;

			primaryFilesCount++;
		}

		setPrimaryInputFileSegments(primaryFilesCount);
	}

	/**
	 *
	 */
	public Object getInputFile(int index)
	{
		return input_files.get(index);
	}

	/**
	 *
	 */
	public Object[] getInputFiles()
	{
		return input_files.toArray();
	}

	/**
	 *
	 */
	public List getInputFilesAsList()
	{
		return input_files;
	}

	/**
	 *
	 */
	public int getInputFilesCount()
	{
		return input_files.size();
	}

	/**
	 *
	 */
	public int getCutpointCount()
	{
		return cut_points.size();
	}

	/**
	 *
	 */
	public int getChapterpointCount()
	{
		return chapter_points.size();
	}

	/**
	 *
	 */
	public int getPIDCount()
	{
		return predefined_IDs.size();
	}

	/**
	 *
	 */
	public List getCutpointList()
	{
		return cut_points;
	}

	/**
	 *
	 */
	public List getChapterpointList()
	{
		return chapter_points;
	}

	/**
	 *
	 */
	public String getOutputNameParent(String str)
	{
		int index;

		if ( (index = str.lastIndexOf(".")) < 0)
			return (getOutputDirectory() + getFileSeparator() + str);

		return (getOutputDirectory() + getFileSeparator() + str.substring(0, index));
	}
	
	/**
	 *
	 */
	public String getOutputDirectory()
	{
		return output_directory;
	}
	
	/**
	 *
	 */
	public void setOutputDirectory(String value)
	{
		if (value.endsWith(getFileSeparator()))
			output_directory = value.substring(0, value.length() - 1);

		else
			output_directory = value;
	}

	/**
	 *
	 */
	public String checkOutputDirectory()
	{
		String str = output_directory;

		if (str == null || str.length() == 0 || str.startsWith("[res]"))
		{
			if (input_files.size() == 0)
				return "";

			str = new File(input_files.get(0).toString()).getParent();
		}

		if (checkWriteAccess(str))
		{
			output_directory = str;
			return null;
		}

		return str;
	}

	/**
	 * check write access
	 */
	private boolean checkWriteAccess(String path)
	{
		try {
			File _path = new File(path);
			String _file = path + getFileSeparator() + "~$pjx$.tmp";

			if (path == null || !_path.exists())
				return false;

			RandomAccessFile raf = new RandomAccessFile(_file, "rw");
			raf.close();

			new File(_file).delete();

		} catch (Exception ex2) {

			return false;
		}

		return true;
	}

	/**
	 *
	 */
	public String getOutputName()
	{
		return output_name;
	}

	/**
	 *
	 */
	public String getOutputName(String str)
	{
		if (output_name.length() > 0)
			return getOutputName();

		return str;
	}

	/**
	 *
	 */
	public void setOutputName(String str)
	{
		output_name = str;
	}

	/**
	 *
	 */
	public void addPID(Object value)
	{
		if (!predefined_IDs.contains(value))
			predefined_IDs.add(value);
	}

	/**
	 *
	 */
	public void addPID(Object[] values)
	{
		for (int i = 0; i < values.length; i++)
			addPID(values[i]);
	}

	/**
	 *
	 */
	public void clearPIDs()
	{
		predefined_IDs.clear();
	}

	/**
	 *
	 */
	public void removePID(Object value)
	{
		int index;

		if ((index = predefined_IDs.indexOf(value)) < 0)
			return;

		predefined_IDs.remove(index);
	}

	/**
	 *
	 */
	public void removePID(Object[] values)
	{
		for (int i = 0; i < values.length; i++)
			removePID(values[i]);
	}

	/**
	 *
	 */
	public Object[] getPIDs()
	{
		return predefined_IDs.toArray();
	}

	/**
	 *
	 */
	public int[] getPIDsAsInteger()
	{
		int len = predefined_IDs == null ? 0 : predefined_IDs.size();

		int[] array = new int[len];

		for (int i = 0; i < len; i++) 
			array[i] = Integer.parseInt(predefined_IDs.get(i).toString().substring(2), 16);

		return array;
	}

	/**
	 *
	 */
	public void addCutpoint(Object value)
	{
		if (!cut_points.contains(value))
			cut_points.add(value);
	}

	/**
	 *
	 */
	public void addCutpoint(int index, Object value)
	{
		cut_points.add(index, value);
	}

	/**
	 *
	 */
	public void addCutpoint(Object[] values)
	{
		for (int i = 0; i < values.length; i++)
			addCutpoint(values[i]);
	}

	/**
	 *
	 */
	public void clearCutpoints()
	{
		cut_points.clear();
	}

	/**
	 *
	 */
	public Object removeCutpoint(int index)
	{
		if (index < 0 || index >= cut_points.size())
			return null;

		Object obj = cut_points.remove(index);

		removeCutImage(obj);

		return obj;
	}

	/**
	 *
	 */
	public Object[] getCutpoints()
	{
		return cut_points.toArray();
	}

	/**
	 *
	 */
	public void addChapterpoint(Object value)
	{
		if (!chapter_points.contains(value))
			chapter_points.add(value);
	}

	/**
	 *
	 */
	public void addChapterpoint(int index, Object value)
	{
		chapter_points.add(index, value);
	}

	/**
	 *
	 */
	public void addChapterpoint(Object[] values)
	{
		for (int i = 0; i < values.length; i++)
			addChapterpoint(values[i]);
	}

	/**
	 *
	 */
	public void clearChapterpoints()
	{
		chapter_points.clear();
	}

	/**
	 *
	 */
	public Object removeChapterpoint(int index)
	{
		if (index < 0 || index >= chapter_points.size())
			return null;

		Object obj = chapter_points.remove(index);

		return obj;
	}

	/**
	 *
	 */
	public Object[] getChapterpoints()
	{
		return chapter_points.toArray();
	}

	/**
	 *  
	 */ 
	public String getFirstFileBase()
	{
		return (getOutputDirectory() + getFirstFileName());
	}

	/**
	 *  
	 */ 
	public String getFirstFileName()
	{
		String firstFileChild = new File(getInputFiles()[0].toString()).getName();
		int index;

		if ( (index = firstFileChild.lastIndexOf(".")) != -1 ) 
			firstFileChild = firstFileChild.substring(0, index);

		return firstFileChild;
	}

	/**
	 *  
	 */ 
	public long getFirstFileDate()
	{
		return ((XInputFile) getInputFiles()[0]).lastModified();
	}

	/**
	 *  set log files
	 */ 
	public void setLogFiles()
	{
		String str = getOutputName(getFirstFileName());

		if (Common.getSettings().getBooleanProperty(Keys.KEY_DebugLog))
		{
			debug = true;

			setDebugLogStream(getOutputDirectory() + getFileSeparator() + str + "_biglog.txt");
		} 

		//settings übergeben!!
		if (Common.getSettings().getBooleanProperty(Keys.KEY_NormalLog))
		{
			if (Common.getSettings().getBooleanProperty(Keys.KEY_ExternPanel_createVdrIndex) && Common.getSettings().getIntProperty(Keys.KEY_ConversionMode) == 1)
				normalLog = getOutputDirectory() + getFileSeparator() + "summary.vdr";

			else
				normalLog = getOutputDirectory() + getFileSeparator() + str + "_log.txt";
		}
	}

	/**
	 * set systems log output for 'big log'
	 */
	private void setDebugLogStream(String str)
	{
		try {

			logging = new PrintStream(new FileOutputStream(str));
			System.setOut(logging);

		} catch (IOException e) { 

			Common.setExceptionMessage(e);
		}
	}

	/**
	 * 
	 */
	public void closeDebugLogStream()
	{
		if ( !Common.getSettings().getBooleanProperty(Keys.KEY_DebugLog))
			return;

		if (logging != null)
		{
			logging.flush();
			logging.close();
		}

		debug = false;
	}


	/**
	 * 
	 */
	public void closeNormalLogStream(String str)
	{
		if ( !Common.getSettings().getBooleanProperty(Keys.KEY_NormalLog))
			return;

		try {
			PrintWriter nlf = new PrintWriter(new FileOutputStream(normalLog));

			nlf.print(str);
			nlf.close();

		} catch (IOException e) { 

			Common.setExceptionMessage(e);
		}
	}

	/**
	 * 
	 */
	public boolean DebugMode()
	{
		return debug;
	}


	/**
	 * 
	 */
	public void setPrimaryInputFileSegments(int val)
	{
		primaryInputFileSegments = val;
	}

	/**
	 * 
	 */
	public int getPrimaryInputFileSegments()
	{
		return primaryInputFileSegments;
	}

	/**
	 * 
	 */
	public int getSecondaryInputFileSegments()
	{
		return (getInputFilesCount() - getPrimaryInputFileSegments());
	}

	/**
	 * 
	 */
	public int[] getCutImage(Object obj)
	{
		if (obj != null && cut_images.containsKey(obj))
			return ((int[]) cut_images.get(obj));

		return null;
	}

	/**
	 * 
	 */
	public void setCutImage(String str, int[] data)
	{
		cut_images.put(str, data);
	}

	/**
	 * 
	 */
	public int[] removeCutImage(Object obj)
	{
		if (cut_images.containsKey(obj))
			return ((int[]) cut_images.remove(obj));

		return null;
	}

	/**
	 * the coll table (gui only)
	 */
	public Object[][] getCollectionAsTable()
	{
		int size = getInputFilesCount();

		Object[][] table = new Object[size > 10 ? size : 10][12];

		for (int i = 0; i < size; i++)
		{
			XInputFile xInputFile = (XInputFile) getInputFile(i);

			table[i][0] = xInputFile.getFileID();
		//	table[i][0] = xInputFile.getStreamInfo().getFileID();
			table[i][1] = xInputFile.getStreamInfo().getFileSourceBase();
			table[i][2] = i < getPrimaryInputFileSegments() ? new Integer(i) : new Integer(-i);
			table[i][3] = xInputFile.getName();
			table[i][4] = (xInputFile.getParent().length() > 0 ? xInputFile.getParent() : xInputFile.toString().substring(0, xInputFile.toString().indexOf(xInputFile.getName())));
			table[i][5] = String.valueOf(xInputFile.length() / 1048576L) + " MB";
			table[i][6] = Common.formatTime_3(xInputFile.lastModified());
			table[i][7] = new Integer(xInputFile.getStreamInfo().getVideoStreams().length);
			table[i][8] = new Integer(xInputFile.getStreamInfo().getAudioStreams().length);
			table[i][9] = new Integer(xInputFile.getStreamInfo().getTeletextStreams().length);
			table[i][10] = new Integer(xInputFile.getStreamInfo().getSubpictureStreams().length);
			table[i][11] = xInputFile.getStreamInfo().getFileType();
		}

		return table;
	}

	/**
	 * 
	 */
	public void setActionType(int value)
	{
		action_type = value;
	}

	/**
	 * 
	 */
	public int getActionType()
	{
		return action_type;
	}

	/**
	 * 
	 */
	public long getAllSizes()
	{
		long value = 0;

		for (int i = 0, j = getInputFilesCount(); i < j; i++)
			value += ((XInputFile) getInputFile(i)).length();

		return (value / 1048576L);
	}

	/**
	 * 
	 */
	public String getShortSummary()
	{
		String str = isActive() ? Resource.getString("JobCollection.InProgress") : Resource.getString("JobCollection.Idle");
		str += line_separator;
		str += Resource.getString("JobCollection.Action") + " " + (getActionType() < 0 ? Resource.getString("JobCollection.unspecified") : Keys.ITEMS_ConversionMode[getActionType()].toString());
		str += line_separator;
		str += Resource.getString("JobCollection.PrimaryFileSegments") + " " + getPrimaryInputFileSegments();
		str += line_separator;
		str += Resource.getString("JobCollection.SecondaryFiles") + " " + getSecondaryInputFileSegments();
		str += line_separator;
		str += Resource.getString("JobCollection.Cutpoints") + " " + getCutpointCount();
		str += line_separator;
		str += Resource.getString("JobCollection.Chapters") + " " + getChapterpointCount();
		str += line_separator;
		str += Resource.getString("JobCollection.PidSelection") + " " + getPIDCount();
		str += line_separator;
		str += Resource.getString("JobCollection.OwnSettings") + " " + (settings != null ? Resource.getString("General.Yes") : Resource.getString("General.No"));
		str += line_separator;
		str += Resource.getString("JobCollection.AllSize")+ " " + getAllSizes() + "MB";

		return str;
	}

	/**
	 * 
	 */
	public boolean hasSettings()
	{
		return (settings != null);
	}

	/**
	 * routing returning settings
	 */
	public Settings getSettings()
	{
		if (settings == null)
			return Common.getSettings();

		return settings;
	}

	/**
	 * collection specific settings
	 */
	public void setSettings(Settings _settings)
	{
		if (isActive())
			return;

		else if (_settings == null)
		{
			settings = null;
			return;
		}

		settings = new Settings();

		try {
			settings.loadProperties(new ByteArrayInputStream(_settings.storeProperties()));

		} catch (IOException e) {

			settings = null;
			Common.setExceptionMessage(e);
		}
	}

}
