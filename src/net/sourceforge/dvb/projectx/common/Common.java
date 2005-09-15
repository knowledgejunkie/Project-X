/*
 * @(#)Common.java - carries various stuff, the center class
 *
 * Copyright (c) 2004-2005 by dvb.matt, All Rights Reserved.
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

package net.sourceforge.dvb.projectx.common;

import java.awt.GraphicsEnvironment;
import java.awt.Color;
import java.awt.Rectangle;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.text.NumberFormat;

import java.util.Date;
import java.util.TimeZone;

import java.io.StringWriter;
import java.io.PrintWriter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import net.sourceforge.dvb.projectx.audio.Audio;

import net.sourceforge.dvb.projectx.common.Resource;
import net.sourceforge.dvb.projectx.common.Keys;
import net.sourceforge.dvb.projectx.common.JobCollection;
import net.sourceforge.dvb.projectx.common.Settings;
import net.sourceforge.dvb.projectx.common.GuiInterface;

import net.sourceforge.dvb.projectx.parser.CommonParsing;
import net.sourceforge.dvb.projectx.parser.Scan;
import net.sourceforge.dvb.projectx.parser.MainProcess;

import net.sourceforge.dvb.projectx.subtitle.Subpicture;

import	net.sourceforge.dvb.projectx.video.MpvDecoder;

import net.sourceforge.dvb.projectx.xinput.XInputFile;
import net.sourceforge.dvb.projectx.xinput.XInputDirectory;
import net.sourceforge.dvb.projectx.xinput.topfield_raw.RawInterface;


/**
 *
 */
public final class Common extends Object {

	/* main version index */
	private static String version_name = "ProjectX 0.90 pre_03";
	private static String version_date = "15.09.2005";

	private static String line_separator = System.getProperty("line.separator");

	private static String messagelog = "";

	private static int ProcessedCollection = 0;
	private static int ActiveCollection = -1;

	private static int ProcessedPercent = 0;

	private static boolean showGUI = false;
	private static boolean runningCLI = false;
	private static boolean runningProcess = false;
	private static boolean GlobalDebug = false;

	private static boolean canAccessFtp = true;
	private static boolean canAccessRawread = true;
	private static boolean canAccessColorTable = true;
	private static boolean canAccessSilentAC3 = true;

	/** name of the colours table file */
	private static final String COLOUR_TABLES_FILENAME = "colours.tbl";
	
	/** name of the ac3 file */
	private static final String AC3_FILENAME = "ac3.bin";
	
	/**  */
	private static ArrayList subpicture_colormodels = null;

	/** list of AC3 frames */
	private static List AC3list = new ArrayList();

	private static DateFormat time_format_1 = new SimpleDateFormat("HH:mm:ss.SSS");
	private static DateFormat time_format_2 = new SimpleDateFormat("HH:mm:ss:SSS");
	private static DateFormat time_format_3 = new SimpleDateFormat("dd.MM.yy  HH:mm");

	private static byte temp_byte;

	private static Settings settings;

	/* status panel */
	private static int[] FpsValue = { 0, 0 };
	private static long[] Data_Troughput = { 0, 0 };
	private static int SplitPart = 0;
	private static String StatusString = null;

	/* linkage to x.swing components */
	private static GuiInterface guiInterface = null;

	private static Subpicture subpicture = null;

	private static Scan scan = null;

	private static MpvDecoder mpvdecoder = null;

	/**
	 * carries all new collection classes
	 */
	private static List collectionList = new ArrayList();

	/**
	 * 
	 */
	private Common()
	{}

	/**
	 * save settings, if enabled and no fatal error has occured
	 */
	public static void exitApplication(int returncode)
	{
		if (returncode == 0 && getSettings().getBooleanProperty(Keys.KEY_SaveSettingsOnExit))
			saveSettings();

		System.exit(returncode);
	}

	/**
	 * 
	 */
	public static void setSettings()
	{
		setSettings(null);
	}

	/**
	 * 
	 */
	public static void setSettings(String str)
	{
		if (settings != null)
			return;

		if (str == null)
			settings = new Settings();

		else
			settings = new Settings(str);
	}

	/**
	 * 
	 */
	public static Settings getSettings()
	{
		return settings;
	}

	/**
	 * 
	 */
	public static void saveSettings()
	{
		saveSettings(null);
	}

	/**
	 * 
	 */
	public static void saveSettings(String str)
	{
		getSettings().setProperty(Keys.KEY_Language[0], Resource.getChosenLanguage());

		getMainFrameBounds();

		settings.save(str);
	}

	/**
	 * 
	 */
	public static void init()
	{
		StatusString = Resource.getString("run.status");

		scan = new Scan();
		subpicture = new Subpicture();
		mpvdecoder = new MpvDecoder();
		subpicture_colormodels = loadColorModels();
	}

	/**
	 * 
	 */
	public static void prepareGui(boolean b)
	{
		showGUI = b;

		//prepare gui
		guiInterface = new GuiInterface(showGUI);

		//load gui
		if (showGUI())
			getGuiInterface().loadGui();
	}

	/**
	 * 
	 */
	public static GuiInterface getGuiInterface()
	{
		return guiInterface;
	}

	/**
	 * 
	 */
	public static boolean showGUI()
	{
		return showGUI;
	}

	/**
	 *
	 */
	public static Subpicture getSubpictureClass()
	{
		return subpicture;
	}

	/**
	 *
	 */
	public static Scan getScanClass()
	{
		return scan;
	}

	/**
	 *
	 */
	public static MpvDecoder getMpvDecoderClass()
	{
		return mpvdecoder;
	}

	/**
	 * 
	 */
	public static void startMainProcess()
	{
		new MainProcess().start();
	}

	/**
	 * 
	 */
	public static void setRunningProcess(boolean b)
	{
		runningProcess = b;

		if (b)
		{
			setMessage(null, true, 0xFFFFFF);

			if (getSettings().getBooleanProperty(Keys.KEY_minimizeMainFrame))
				minimizeMainFrame();

			if (getSettings().getBooleanProperty(Keys.KEY_hideProcessWindow))
				getGuiInterface().closeLogWindow();
		}

		else if (getSettings().getBooleanProperty(Keys.KEY_hideProcessWindow))
			getGuiInterface().showLogWindow();
	}

	/**
	 * 
	 */
	public static boolean isRunningProcess()
	{
		return runningProcess;
	}

	/**
	 * 
	 */
	public static void setRunningCLI(boolean b)
	{
		runningCLI = b;
	}

	/**
	 * 
	 */
	public static boolean isRunningCLI()
	{
		return runningCLI;
	}

	/**
	 * 
	 */
	public static void setGlobalDebug(boolean b)
	{
		GlobalDebug = b;
	}

	/**
	 * 
	 */
	public static boolean getGlobalDebug()
	{
		return GlobalDebug;
	}

	/**
	 * 
	 */
	public static int getProcessedCollection()
	{
		return ProcessedCollection;
	}

	/**
	 * 
	 */
	public static void setProcessedCollection(int val)
	{
		ProcessedCollection = val;
	}

	/**
	 * 
	 */
	public static int getActiveCollection()
	{
		return ActiveCollection;
	}

	/**
	 * 
	 */
	public static void setActiveCollection(int val)
	{
		ActiveCollection = val;
	}

	/**
	 * 
	 */
	public static String getLineSeparator()
	{
		return line_separator;
	}

	/**
	 * 
	 */
	public static String getVersionName()
	{
		return version_name;
	}

	/**
	 * 
	 */
	public static String getVersionDate()
	{
		return version_date;
	}

	/**
	 * Returns the Version information
	 * 
	 * @return String[]
	 */
	public static String[] getVersion()
	{
		return new String[] { 
			getVersionName(),
			getVersionDate(),
			Resource.getString("version.info"),
			Resource.getString("version.user") + System.getProperty("user.name")
		};
	}

	/**
	 * 
	 */
	public static String formatNumber(long val)
	{
		return NumberFormat.getInstance().format(val);
	}

	/**
	 * return collection
	 */
	public static boolean isCollectionListEmpty()
	{
		return collectionList.isEmpty();
	}

	/**
	 * return collectionlist size
	 */
	public static int getCollectionListSize()
	{
		return collectionList.size();
	}

	/**
	 * remove collection
	 */
	public static boolean removeCollection(int index)
	{
		if (index < 0 || index >= collectionList.size())
			return false;

		if (getCollection(index).isActive())
			return false;

		collectionList.remove(index);

		return true;
	}

	/**
	 * return collection
	 */
	public static JobCollection getCollection(int index)
	{
		if (index < 0 || index >= collectionList.size())
			return null;

		return (JobCollection) collectionList.get(index);
	}

	/**
	 * if collection doesnt exists, create one
	 */
	public static JobCollection addCollection()
	{
		return addCollection(true);
	}

	/**
	 * if collection doesnt exists, create one
	 */
	public static JobCollection addCollection(boolean append)
	{
		/**
		 * ensure only, that at least one coll exists
		 */
		if (!append && !collectionList.isEmpty())
			return null;

		/**
		 * create new coll, with current output dir.
		 */
		return addCollection(new JobCollection(getSettings().getProperty(Keys.KEY_OutputDirectory)));
	}

	/**
	 * 
	 */
	public static JobCollection addCollection(JobCollection collection)
	{
		/**
		 * add new coll
		 */
		collectionList.add(collection);

		addCollectionAtEnd();

		return collection;
	}

	/**
	 * check whether commons-net is available, to prevent malfunctions
	 */
	public static String checkLibraryAccess()
	{
		try {
			Class cls = Class.forName("org.apache.commons.net.ftp.FTPClient");

			canAccessFtp = true;

			return null;
		}
		catch (Exception exc) {}
		catch (Error err) {}

		return "\ncommons-net library not accessible! see readme.txt [ii]\nensure the correct location/classpath, related to the executed .jar\n";
	}

	/**
	 *
	 */
	public static boolean canAccessFtp()
	{
		return canAccessFtp;
	}

	/**
	 *
	 */
	public static boolean canAccessRawRead()
	{
		return canAccessRawread;
	}

	/**
	 *
	 */
	public static boolean canAccessColorTable()
	{
		return canAccessColorTable;
	}

	/**
	 *
	 */
	public static boolean canAccessSilentAC3()
	{
		return canAccessSilentAC3;
	}

	/**
	 * changes the byte order, fixed to 2bytes len ATM!
	 */
	public static void changeByteOrder(byte[] data, int off, int len)
	{
		temp_byte = data[off + 1];
		data[off + 1] = data[off];
		data[off] = temp_byte;

		if (len > 2)
			setMessage("byte swap > 2 not yet defined!!");
	}

	// should try a while to rename with success, if file_system is blocked by another app.
	public static boolean renameTo(File oldfile, File newfile)
	{
		//explicit call, otherwise java >= 1.5 ? doesnt release a closed file object immediately
		System.gc();

		for (int i = 0; i < 10000; i++)
			if ( oldfile.renameTo(newfile) )
				return true;

		setMessage(Resource.getString("common.rename_error1") + " '" + oldfile.toString() + "' " + Resource.getString("common.rename_error2") + " '" + newfile.toString() + "'", true, 0xFFE0E0);

		return false;
	}

	/**
	 *
	 */
	public static boolean renameTo(String oldfile, String newfile)
	{
		return renameTo(new File(oldfile), new File(newfile));
	}

	/**
	 *
	 */
	public static String adaptString(int str, int len)
	{
		return adaptString(String.valueOf(str), len);
	}

	/**
	 *
	 */
	public static String adaptString(String str, int len)
	{
		StringBuffer strbuf = new StringBuffer(str.trim());

		while (strbuf.length() < len)
			strbuf.insert(0, "0");

		return strbuf.toString();
	}

	/**
	 *
	 */
	public static String formatTime_1(long time_value)
	{
		time_format_1.setTimeZone(TimeZone.getTimeZone("GMT+0:00"));
		return time_format_1.format(new Date(time_value));
	}

	/**
	 *
	 */
	public static String formatTime_2(long time_value, long frame_rate)
	{
		time_format_2.setTimeZone(TimeZone.getTimeZone("GMT+0:00"));
		String time_str = time_format_2.format(new Date(time_value));

		return (time_str.substring(0, time_str.length() - 3) + adaptString((Integer.parseInt(time_str.substring(time_str.length() - 3)) * 90 / (int)frame_rate), 2));
	}

	/**
	 *
	 */
	public static String formatTime_3(long time_value)
	{
		//time_format_3.setTimeZone(TimeZone.getTimeZone("GMT+0:00"));
		return time_format_3.format(new Date(time_value));
	}

	/**
	 *
	 */
	public static Object[] getColorModels()
	{
		return subpicture_colormodels.toArray();
	}

	/**
	 *
	 */
	public static ArrayList getColorModelsList()
	{
		return subpicture_colormodels;
	}

	/**
	 *
	 */
	private static ArrayList loadColorModels()
	{
		ArrayList list = new ArrayList();

		list.add(Resource.getString("SubtitlePanel.Colormodel.Mode0"));
		list.add(Resource.getString("SubtitlePanel.Colormodel.Mode1")); 
		list.add(Resource.getString("SubtitlePanel.Colormodel.Mode2"));

		URL url = Resource.getResourceURL(COLOUR_TABLES_FILENAME);

		if (url == null)
			return list;

		try {
			BufferedReader table = new BufferedReader(new InputStreamReader(url.openStream()));
			String line;

			while( (line = table.readLine()) != null)
			{
				if ( line.trim().length() == 0 )
					continue;

				if ( line.startsWith("table") )
					list.add( (line.substring( line.indexOf("=") + 1)).trim() );
			}

			table.close();

		} catch (IOException e) {

			System.err.println("IOException loadColorModels " + e);
		}

		if (list.size() <= 3)
			canAccessColorTable = false;

		return list;
	}

	/**
	 *
	 */
	public static Hashtable getUserColourTable(String model) throws IOException
	{
		Hashtable user_table = new Hashtable();
		
		URL url = Resource.getResourceURL(COLOUR_TABLES_FILENAME);

		if ( url == null )
			return user_table;

		BufferedReader table = new BufferedReader( new InputStreamReader(url.openStream()));
		String line;
		boolean table_match = false;

		while( (line = table.readLine()) != null)
		{
			if ( line.trim().length() == 0 )
				continue;

			if ( line.startsWith("table") )
				table_match = line.substring( line.indexOf("=") + 1).trim().equals(model);

			else if (table_match)
			{
				if ( line.startsWith("model") )
					user_table.put( "model", line.substring( line.indexOf("=") + 1).trim() );

				else
					user_table.put( line.substring(0, line.indexOf("=")).trim(), line.substring( line.indexOf("=") + 1).trim() );
			}
		}

		table.close();

		if ( !user_table.isEmpty() && !user_table.containsKey("model") )
			user_table.put( "model", "16");

		return user_table;
	}

	
	/**
	 * Loads the ac3.bin file.
	 */
	public static void loadAC3() 
	{
		Audio audio = new Audio();
		AC3list.clear();

		try {
			URL url = Resource.getResourceURL(AC3_FILENAME);

			if (url != null)
			{
				BufferedInputStream bis = new BufferedInputStream(url.openStream());
				ByteArrayOutputStream bao = new ByteArrayOutputStream();

				byte[] buff = new byte[1024];
				int bytesRead = -1;

				while ((bytesRead = bis.read(buff, 0, buff.length)) != -1)
				{
					bao.write(buff, 0, bytesRead);
				}
				
				byte[] check = bao.toByteArray();
			
				setMessage(Resource.getString("ac3.msg.loading.start"));
			
				int a=0, frame_counter=0;

				while (a < check.length) 
				{
					audio.AC3_parseHeader(check,a);
					setMessage("(" + frame_counter + ") " + audio.AC3_saveAnddisplayHeader());

					byte[] ac3data = new byte[audio.Size];
					System.arraycopy(check,a,ac3data,0,audio.Size);
					AC3list.add(ac3data);
					a += audio.Size;
					frame_counter++;
				}
				check = null;
			}
		} 
		catch (IOException e5) 
		{ 
			setExceptionMessage(e5); 
			AC3list.clear();
		}
	
		if (AC3list.size() > 0)
			setMessage(Resource.getString("ac3.msg.frames", "" + AC3list.size()));

		else
			canAccessSilentAC3 = false;
	}
	
	
	/**
	 * Returns the AC3list.
	 * 
	 * @return ArrayList
	 */
	public static List getAC3list()
	{
		return AC3list;
	}


	public static Object[] getFonts()
	{
		Object[] fonts = new Object[0];

		try {
			fonts = (Object[]) GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();

		} catch (Exception exc) {
			System.out.println(Resource.getString("SubtitlePanel.Font.LoadError") + ": " + exc);

		} catch (Error err) {
			System.out.println(Resource.getString("SubtitlePanel.Font.LoadError") + ": " + err);
		}

		return fonts;
	}

	/**
	 * 
	 */
	public static void appendLogMessage(String str)
	{
		messagelog += getLineSeparator() + str;
	}

	/**
	 * 
	 */
	public static void clearMessageLog()
	{
		messagelog = "";
	}

	/**
	 * 
	 */
	public static String getMessageLog()
	{
		return messagelog;
	}

	/**
	 * messages
	 *
	 * @param1 - the msg
	 */
	public static void setMessage(Object[] obj)
	{
		for (int i = 0; obj != null && i < obj.length; i++)
			setMessage(obj[i].toString());
	}

	/**
	 * messages
	 *
	 * @param1 - the msg
	 */
	public static void setMessage(String msg)
	{
		setMessage(msg, false);
	}

	/**
	 * messages of interest, also with current systems time_index
	 *
	 * @param1 - the msg
	 * @param2 - force windows visibility
	 */
	public static void setMessage(String msg, boolean tofront)
	{
		setMessage(msg, tofront, -1);
	}

	/**
	 * messages of interest, also with current systems time_index
	 *
	 * @param1 - the msg
	 * @param2 - force windows visibility
	 */
	public static void setMessage(String msg, boolean tofront, int background)
	{
		if (msg == null)
		{
			if (!isRunningCLI()) 
				getGuiInterface().setMessage(msg, tofront, background);

			return;
		}

		if (getSettings().getBooleanProperty(Keys.KEY_MessagePanel_Msg4)) 
			msg = "[" + formatTime_1(System.currentTimeMillis()) + "] " + msg;

		if (getGlobalDebug()) 
			System.out.println(msg); 

		if (isRunningCLI()) 
			System.out.println("\r" + msg); 

		else
			getGuiInterface().setMessage(msg, tofront, background);

		appendLogMessage(msg);
	}

	/**
	 * messages
	 *
	 * @param1 - the msg
	 */
	public static void setExceptionMessage(Exception exc)
	{
		StringWriter sw = new StringWriter();
		exc.printStackTrace(new PrintWriter(sw));

		setErrorMessage(sw.toString());
	}

	/**
	 * messages
	 *
	 * @param1 - the msg
	 */
	public static void setErrorMessage(Error err)
	{
		StringWriter sw = new StringWriter();
		err.printStackTrace(new PrintWriter(sw));

		setErrorMessage(sw.toString());
	}

	/**
	 * messages
	 *
	 * @param1 - the msg
	 */
	public static void setErrorMessage(String str)
	{
		/**
		 * show error messge
		 */
		setMessage("");
		setMessage("!> an error has occured..  (please inform the autors at 'forum.dvbtechnics.info')");
		setMessage(str, true, 0xFFE0E0);
	}

	/**
	 * messages
	 *
	 * @param1 - the msg
	 */
	public static void setOSDMessage(String str)
	{
		setOSDMessage(str, false);
	}

	/**
	 * messages
	 *
	 * @param1 - the msg
	 */
	public static void setOSDErrorMessage(String str)
	{
		setOSDMessage(str, true);
	}

	/**
	 * messages
	 *
	 * @param1 - the msg
	 * @param2 - is error
	 */
	public static void setOSDMessage(String str, boolean b)
	{
		getGuiInterface().setOSDMessage(str, b);
	}

	/**
	 * post commands
	 */
	public static void performCommand(String str)
	{
		if (str == null || str.trim().length() == 0)
			return;

		try {
			Runtime.getRuntime().exec(str);

		} catch (Exception ex) { 
			setExceptionMessage(ex); 
		}
	}

	/**
	 * post commands
	 */
	public static void performPostCommand(Object[] lastlist)
	{
		if (!getSettings().getBooleanProperty(Keys.KEY_enablePostProcessing))
			return;

		String cmdl = "";

		switch (getSettings().getIntProperty(Keys.KEY_ConversionMode))
		{
		case CommonParsing.ACTION_DEMUX:
			cmdl = getSettings().getProperty(Keys.KEY_PostCommands_Cmd4);
			break;

		case CommonParsing.ACTION_TO_VDR:
			cmdl = getSettings().getProperty(Keys.KEY_PostCommands_Cmd5);
			break;

		case CommonParsing.ACTION_TO_M2P:
			cmdl = getSettings().getProperty(Keys.KEY_PostCommands_Cmd6);
			break;

		case CommonParsing.ACTION_TO_PVA:
			cmdl = getSettings().getProperty(Keys.KEY_PostCommands_Cmd7);
			break;

		case CommonParsing.ACTION_TO_TS:
			cmdl = getSettings().getProperty(Keys.KEY_PostCommands_Cmd8);

		default:
			return;
		}

		cmdl = cmdl.trim();

		if (cmdl.length() > 0 && lastlist.length > 0)
		{
			ArrayList argList = new ArrayList();

			String append_str = "";
			boolean isQuoted = false;

			int appending = cmdl.lastIndexOf(" \"?");

			if (appending < 0)
				appending = cmdl.lastIndexOf(" ?");
			else
				isQuoted = true;

			if (appending < 0)
				argList.add(cmdl.trim());

			else
			{
				append_str = cmdl.substring(appending).replace('"', ' ').trim();

				cmdl = cmdl.substring(0, appending).trim();
				argList.add(cmdl);

				appending = Integer.parseInt(append_str.substring(1));

				if (appending == 0 || appending > lastlist.length) 
					appending = lastlist.length;

				for (int l = 0; l < lastlist.length && l < appending; l++)
				{
					String str = lastlist[l].toString();
					str = str.substring(str.indexOf("'") + 1, str.length() - 1);

					if (isQuoted)
						argList.add("" + (char)34 + str + (char)34);
					else
						argList.add(str);
				}
			}

			String commandline = "";
			String[] arguments = new String[argList.size()];

			for (int l = 0; l < arguments.length; l++)
			{
				arguments[l] = argList.get(l).toString();
				commandline += l == 0 ? "" : " ";
				commandline += arguments[l];
			}

			setMessage(Resource.getString("working.post.command") + " {" + commandline + "}");


			try { 
				Runtime.getRuntime().exec(arguments); 

			} catch (IOException re)	{ 

				setExceptionMessage(re);
			}

			argList = null;
		}
	}

	/**
	 * should support loading of supported URLs/files via CLI
	 */
	public static XInputFile getInputFile(String value)
	{
		XInputFile inputValue = null;
		URL url = null;

		if (value == null)
			return null;

		try
		{
			url = new URL(value);

			String protocol = url.getProtocol();

			if (protocol.equals("ftp"))
			{
				XInputDirectory xid = new XInputDirectory(url);
				XInputFile[] xif = xid.getFiles();

				for (int i = 0; i < xif.length; i++)
				{
					if ( new URL(xif[i].toString()).getFile().equals(url.getFile()) )
					{
						inputValue = xif[i];
						break;
					}
				}
			}

			else if (protocol.equals("file"))
				inputValue = new XInputFile(new File(url.getHost() + url.getFile()));

			else
				System.out.println("!> Protocol not yet supported: " + protocol);

			return inputValue;
		}
		catch (Exception u1)
		{}

		try {
			File f = new File(value);

			if (f.exists())
			{
				inputValue = new XInputFile(f);

				return inputValue;
			}

		} catch (Exception e) {
			System.out.println("local Filesystem access: '" + value + "' > " + e);
		}

		try {
			inputValue = new XInputFile(value);
			return inputValue;

		} catch (Exception e) {
			System.out.println("ext. Filesystem access: '" + value + "' > " + e);
		}

		return null;
	}

	/**
	 * show java EV
	 */
	public static Object[] getJavaEV(String inifile)
	{
		List list = new ArrayList();

		list.add("Java Environment");
		list.add(DateFormat.getDateInstance(DateFormat.FULL).format(new Date()) + "    " + DateFormat.getTimeInstance(DateFormat.FULL).format(new Date()));
		list.add(Resource.getString("javaev.java.version") + "\t" + System.getProperty("java.version"));
		list.add(Resource.getString("javaev.java.vendor") + "\t" + System.getProperty("java.vendor"));
		list.add(Resource.getString("javaev.java.home") + "\t" + System.getProperty("java.home"));
		list.add(Resource.getString("javaev.java.vm.version") + "\t" + System.getProperty("java.vm.version"));
		list.add(Resource.getString("javaev.java.vm.vendor") + "\t" + System.getProperty("java.vm.vendor"));
		list.add(Resource.getString("javaev.java.vm.name") + "\t" + System.getProperty("java.vm.name"));
		list.add(Resource.getString("javaev.java.class.vers") + "\t" + System.getProperty("java.class.version"));
		list.add(Resource.getString("javaev.java.class.path") + "\t" + System.getProperty("java.class.path"));
		list.add(Resource.getString("javaev.java.os.name") + "\t" + System.getProperty("os.name"));
		list.add(Resource.getString("javaev.java.os.arch") + "\t" + System.getProperty("os.arch"));
		list.add(Resource.getString("javaev.java.os.version") + "\t" + System.getProperty("os.version"));
		list.add(Resource.getString("javaev.java.ini.file") + "\t" + inifile);

		String str = (new RawInterface("")).GetLoadStatus();

		if (str == null)
		{
			str = Resource.getString("rawread.msg1");
			canAccessRawread = false;
		}

		list.add(Resource.getString("javaev.java.disk.access") + "\t" + str);
		list.add(Resource.getString("javaev.java.user.lang") + "\t" + Resource.getChosenLanguage());
		list.add(Resource.getString("javaev.java.user.name") + "\t" + System.getProperty("user.name"));
		list.add(Resource.getString("javaev.java.user.home") + "\t" + System.getProperty("user.home"));

		return list.toArray();
	}


	/**
	 *
	 */
	public static String getDataTroughput()
	{
		long val = (Data_Troughput[1] - Data_Troughput[0]) / 1024L;

		Data_Troughput[1] = Data_Troughput[0] = 0;

		return String.valueOf(val < 0 ? 0 : val);
	}

	/**
	 *
	 */
	public static String getFps()
	{
		int val = FpsValue[1] - FpsValue[0];

		FpsValue[1] = FpsValue[0] = 0;

		return String.valueOf(val < 0 ? 0 : val);
	}

	/**
	 *
	 */
	public static void setFps(int val)
	{
		if (FpsValue[0] == 0)
			FpsValue[1] = FpsValue[0] = val;

		else
			FpsValue[1] = val;
	}

	/**
	 *
	 */
	public static String getExportedSize()
	{
		JobCollection collection = getCollection(getProcessedCollection());

		long val = collection == null || collection.getJobProcessing() == null ? 0 : collection.getJobProcessing().getMediaFilesExportLength() / 1048576L;

		return String.valueOf(val) + " MB - " + SplitPart;
	}

	/**
	 *
	 */
	public static void showSplitPart(int value)
	{
		SplitPart = value;
	}

	/**
	 * progress
	 *
	 * @param1 - the msg
	 */
	public static void updateProgressBar(long position, long size)
	{
		if (Data_Troughput[0] == 0)
			Data_Troughput[1] = Data_Troughput[0] = position;

		else
			Data_Troughput[1] = position;

		if (size <= 0)
			size = 1;

		int percent = (int)(position * 100 / size) + 1;

		if (percent == ProcessedPercent)
			return;

		ProcessedPercent = percent;

		getGuiInterface().updateProgressBar(ProcessedPercent);
	}

	/**
	 * progress
	 *
	 * @param1 - the msg
	 */
	public static void updateProgressBar(String str)
	{
		setStatusString(str);
		getGuiInterface().updateProgressBar(str);
	}

	/**
	 * progress
	 *
	 * @param1 - the msg
	 * @param2 - %
	 */
	public static void updateProgressBar(String str, long position, long size)
	{
		if (str != null)
			updateProgressBar(str);

		updateProgressBar(position, size);
	}

	/**
	 *
	 */
	public static String getStatusString()
	{
		return StatusString;
	}

	/**
	 *
	 */
	public static void setStatusString(String str)
	{
		StatusString = str;
	}

	/**
	 * Checks the languages of Project-X
	 */
	public static void checkAvailableLanguages() {
		try
		{
			URL url = new URL("http://project-x.sourceforge.net/optional/resources/");
			URLConnection urlConn = url.openConnection();
			BufferedReader br = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));

			List list = new ArrayList();
			String line = null;
			int index = 0;

			while ((line = br.readLine()) != null)
			{
				if ((index = line.indexOf("pjxresources_")) < 0)
					continue;
	
				list.add(line.substring(index, index + 26));
			}

			line = "cannot determine";

			if (!list.isEmpty())
			{
				line = "";

				for (int i = 0; i < list.size(); i++)
					line += "(" + i + ") " + list.get(i).toString() + "\n";
			}

			getGuiInterface().showMessageDialog(line, "online verfügbare Sprachendateien");

		} catch(Exception e) {

			setExceptionMessage(e);
		}
	}

	/**
	 * Checks the latest version of Project-X
	 */
	public static void checkVersion()
	{
		try {
			URL url = new URL("http://project-x.sourceforge.net/update/update.txt");
			URLConnection urlConn = url.openConnection();
			BufferedReader br = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));

			String line = br.readLine();
			String version = line;
			String date = null;

			if (line != null)
			{
				StringTokenizer st = new StringTokenizer(line, ";");

				if (st.hasMoreTokens())
					version = st.nextToken();

				if (st.hasMoreTokens())
					date = st.nextToken();
			}

			if (date != null)
				version += "\n" + date;

			getGuiInterface().showMessageDialog(Resource.getString("help.version.info") + "\n"+version, Resource.getString("help.version.info.title"));

		} catch(Exception e) {

			setExceptionMessage(e);
		}
	}

	/**
	 * 
	 */
	public static void getMainFrameBounds()
	{
		Rectangle rect = (Rectangle) getGuiInterface().getMainFrameBounds();

		if (rect != null)
		{
			getSettings().setIntProperty(Keys.KEY_WindowPositionMain_X[0], (int) rect.getX());
			getSettings().setIntProperty(Keys.KEY_WindowPositionMain_Y[0], (int) rect.getY());
			getSettings().setIntProperty(Keys.KEY_WindowPositionMain_Width[0], (int) rect.getWidth());
			getSettings().setIntProperty(Keys.KEY_WindowPositionMain_Height[0], (int) rect.getHeight());
		}
	}

	/**
	 *
	 */
	public static void minimizeMainFrame()
	{
		getGuiInterface().minimizeMainFrame();
	}

	/**
	 *
	 */
	public static void setFrameTitle(String str)
	{
		getGuiInterface().setMainFrameTitle(str);
	}

	/**
	 *
	 */
	public static void addCollectionAtEnd()
	{
		if (getGuiInterface() != null)
			getGuiInterface().addCollectionAtEnd();
	}

}