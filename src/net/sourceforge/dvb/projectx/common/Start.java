/*
 * @(#)Start.java - main start class 
 *
 * Copyright (c) 2005-2009 by dvb.matt, All Rights Reserved. 
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

package net.sourceforge.dvb.projectx.common;

import java.io.StringWriter;
import java.io.PrintWriter;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;

import java.util.ArrayList;
import java.util.StringTokenizer;

import net.sourceforge.dvb.projectx.xinput.XInputFile;

/**
 * the holy start
 */
public class Start extends Object {

	private static ArrayList cli_switches = new ArrayList();

	public Start()
	{}

	/**
	 * main
	 */
	public static void main(String[] args)
	{
		boolean iniSet = false;
		boolean showGUI = args.length == 0;
		boolean help = false;
		boolean switch_error = false;

		int index = -1;

		String inifile = null;
		String str = null;

		String[] version;
		Object[] environment;

		/**
		 * 
		 */
		for (int i = 0; i < args.length; i++)
			cli_switches.add(args[i]);

		try {

			/**
			 * don't change the order!!
			 */

			/**
			 * remove the stdout re-direction of sys infos
			 */
			if ((index = getSwitch(">")) >= 0)
			{
				if (index < cli_switches.size())
					cli_switches.remove(index);
			}

			/**
			 * get version switch
			 */
		//	if (getBooleanSwitch("-version"))
				System.out.println("Version: " + Common.getVersionName() + "/" + Common.getVersionDate());

			System.out.println("Reading GUI-Switch...");

			/**
			 * get gui switch
			 */
			if (getBooleanSwitch("-gui"))
				showGUI = true;

			System.out.println("Reading Help Switch...");

			/**
			 * get help switch
			 */
			if (getBooleanSwitch("-?"))
				help = true;

			System.out.println("Reading Config File Switch...");

			/**
			 * get 'config file' switch
			 */
			if ((index = getSwitch("-ini")) >= 0)
			{
				switch_error = false;

				if (index < cli_switches.size())
				{
					str = cli_switches.get(index).toString();

					if (new File(str).exists())
					{
						inifile = str;
						iniSet = true;
					}

					else
						switch_error = true;

					cli_switches.remove(index);
				}

				else
					switch_error = false;

				if (switch_error)
				{
					System.out.println("stopped, config file '" + inifile + "' not found ...");
					System.exit(1);
				}
			}
	
			System.out.println(showGUI ? "Start with GUI..." : "Start without GUI...");

			/**
			 * use 'config file'
			 */
			if (!iniSet)
			{
				System.out.println("Loading last Config or Standard File...");
				Common.setSettings();
			}

			else
			{
				System.out.println("Loading Config File: '" + inifile + "' ...");
				Common.setSettings(inifile);
			}

			/**
			 * initialize language
			 */
			Resource.loadLang(Common.getSettings().getProperty(Keys.KEY_Language));

			Common.getSettings().setProperty(Keys.KEY_Language[0], Resource.getChosenLanguage());

			System.out.println("Loading Language -> '" + Resource.getChosenLanguage() + "'");

			/**
			 * dont save settings on exit on CLI, see cli-switches
			 */
			if (args.length != 0)
				Common.getSettings().setProperty(Keys.KEY_SaveSettingsOnExit[0], "0");

			/**
			 * version
			 */
			version = Common.getVersion();

			System.out.println();
			System.out.println(version[0] + "/" + version[1] + " " + version[2] + " " + version[3]);
			System.out.println();

			/**
			 * terms
			 */
			System.out.println(Resource.getString("terms"));

			/**
			 * environment
			 */
			environment = Common.getJavaEV(Common.getSettings().getInifile());

			for (int i = 0; i < environment.length; i++)
				System.out.println(environment[i].toString());

			System.out.println();

			/**
			 * usage
			 */
			System.out.println(Resource.getString("usage"));
			System.out.println();

			/**
			 * stop on help switch
			 */
			if (help)
				Common.exitApplication(0);

			System.out.println("Loading Basic Classes...");

			/**
			 * load main stuff
			 */
			Common.init(); // access after the keys has been loaded!, separate to userinterface later

			System.out.println("Reading CLI Switches...");

			/**
			 * read cli switches
			 */
			if (readSwitches(cli_switches))
				System.out.println("Error while reading CLI Switches ...");

			System.out.println("Preparing GUI (if enabled)...");

			/**
			 * initialize the gui interface
			 */
			Common.prepareGui(showGUI); // access after the keys has been loaded!, separate to userinterface later

			System.out.println("Check Commons-Net library access...");

			/**
			 * planned to disable ftp only, if commons-net is missing
			 */
			if ((str = Common.checkLibraryAccess()) != null)
			{
				throw new Exception(str);
				//System.out.println(ret);
			}

			System.out.println("Loading AC3 frames...");

			/**
			 * load silent ac3 frames
			 */
			Common.loadAC3(); 

			//Common.clearMessageLog();

			if (Common.getSettings().getBooleanProperty(Keys.KEY_autostartWebServer))
				Common.startWebServer();

			/**
			 * starts CL processing
			 */
			if (!showGUI)
			{
				if (!Common.getSettings().getBooleanProperty(Keys.KEY_autostartWebServer))
				{
					if (Common.isCollectionListEmpty())
					{
						System.out.println("Error: No Collection to Process ...");
						Common.exitApplication(2);
					}

					System.out.println("Starting Collection Process...");

					Common.setRunningCLI(true);
					Common.setRunningProcess(true);

					Common.startMainProcess();
				}

				else
					System.out.println("Starting WebIF...");
			}

			else
			{
				if (!Common.getGuiInterface().isAvailable())
					System.out.println("Stopped! Can't start GUI, Classes not available...");

				else if (!Common.isCollectionListEmpty())
				{
					Common.getGuiInterface().addCollectionAtEnd();
					Common.getGuiInterface().showActiveCollection(0);
				}
			}
		}

		/** 
		 * catch all other unhandled exception
		 */
		catch(Exception e) 
		{
			/**
			 * in GUI mode clean GUI and show GUI message
			 */
			if (showGUI) 
			{
				/**
				 * show exception messge
				 */
				StringWriter sw = new StringWriter();
				e.printStackTrace(new PrintWriter(sw));

				/**
				 * message to the commandline
				 */
				String message = Resource.getString("StartUp.Error") + Common.getLineSeparator() + Common.getLineSeparator() + sw.toString();
				System.err.println(message);

				Common.getGuiInterface().showErrorMessageDialog(message, Resource.getString("StartUp.Error.Title"));
			}

			/**
			 * in CLI mode simply show stackTrace
			 */
			else 
			{
				System.out.println("Stopped! An Error has occured...");
				e.printStackTrace();
			}

			System.exit(1);
		}
	}

	/**
	 * create single collection
	 */
	private static JobCollection createCollection(JobCollection collection)
	{
		if (collection == null)
		{
			collection = Common.addCollection(false);
			Common.setActiveCollection(0);
		}

		return collection;
	}

	/**
	 * get switch
	 */
	private static boolean getBooleanSwitch(String str)
	{
		return (getSwitch(str) >= 0);
	}

	/**
	 * get switch
	 */
	private static int getSwitch(String str)
	{
		int index;

		if ((index = cli_switches.indexOf(str)) >= 0)
			cli_switches.remove(index);

		return index;
	}

	/**
	 * read cli
	 */
	private static boolean readSwitches(ArrayList cli_switches)
	{
		JobCollection collection = null;

		boolean error = false;
		boolean switch_error = false;

		String str = null;

		int index = -1;

		try {

			/**
			 * get switches
			 */
			if (getBooleanSwitch("-dvx1"))
				Common.getSettings().setProperty(Keys.KEY_ExternPanel_splitProjectFile[0], "1");

			if (getBooleanSwitch("-dvx2"))
			{
				Common.getSettings().setProperty(Keys.KEY_ExternPanel_splitProjectFile[0], "1");
				Common.getSettings().setProperty(Keys.KEY_AudioPanel_addRiffToAc3[0], "1");
			}

			if (getBooleanSwitch("-dvx3"))
			{
				Common.getSettings().setProperty(Keys.KEY_ExternPanel_splitProjectFile[0], "1");
				Common.getSettings().setProperty(Keys.KEY_AudioPanel_addRiffToMpgAudio[0], "1");
			}

			if (getBooleanSwitch("-dvx4"))
			{
				Common.getSettings().setProperty(Keys.KEY_ExternPanel_splitProjectFile[0], "1");
				Common.getSettings().setProperty(Keys.KEY_AudioPanel_addRiffToAc3[0], "1");
				Common.getSettings().setProperty(Keys.KEY_AudioPanel_addRiffToMpgAudio[0], "1");
			}

			/**
			 * save normal log
			 */
			if (getBooleanSwitch("-log"))
				Common.getSettings().setProperty(Keys.KEY_NormalLog[0], "1");

			/**
			 * save on exit
			 */
			if (getBooleanSwitch("-saveini"))
				Common.getSettings().setProperty(Keys.KEY_SaveSettingsOnExit[0], "1");

			/**
			 * web if
			 */
			if (getBooleanSwitch("-webif"))
				Common.getSettings().setProperty(Keys.KEY_autostartWebServer[0], "1");

			/**
			 * action type
			 */
			if (getBooleanSwitch("-demux"))
				Common.getSettings().setProperty(Keys.KEY_ConversionMode[0], "0");

			if (getBooleanSwitch("-tovdr"))
				Common.getSettings().setProperty(Keys.KEY_ConversionMode[0], "1");

			if (getBooleanSwitch("-tom2p"))
				Common.getSettings().setProperty(Keys.KEY_ConversionMode[0], "2");

			if (getBooleanSwitch("-topva"))
				Common.getSettings().setProperty(Keys.KEY_ConversionMode[0], "3");

			if (getBooleanSwitch("-tots"))
				Common.getSettings().setProperty(Keys.KEY_ConversionMode[0], "4");

			if (getBooleanSwitch("-filter"))
				Common.getSettings().setProperty(Keys.KEY_ConversionMode[0], "5");

			/**
			 * any ini key from CLI, must be a loop
			 */
			while ((index = getSwitch("-set")) >= 0)
			{
				switch_error = false;

				if (index < cli_switches.size())
				{
					str = cli_switches.get(index).toString();

					Common.getSettings().setProperty(str.substring(0, str.indexOf("=")), str.substring(str.indexOf("=") + 1));

					cli_switches.remove(index);
				}

				else
					switch_error = false;

				if (switch_error)
				{
					System.out.println("can't set property ...");
					error = true;
				}
			}

			/**
			 * split output
			 */
			if ((index = getSwitch("-split")) >= 0)
			{
				switch_error = false;

				if (index < cli_switches.size())
				{
					str = cli_switches.get(index).toString();

					try {
						int val = Integer.parseInt(str);

						Common.getSettings().setProperty(Keys.KEY_SplitSize[0], "1");
						Common.getSettings().setProperty(Keys.KEY_ExportPanel_SplitSize_Value[0], String.valueOf(val));

					} catch (Exception e) {
						switch_error = true;
					}

					cli_switches.remove(index);
				}

				else
					switch_error = false;

				if (switch_error)
				{
					System.out.println("can't set split size value ...");
					error = true;
				}
			}

			/**
			 * new output base directory
			 */
			if ((index = getSwitch("-out")) >= 0)
			{
				switch_error = false;

				collection = createCollection(collection);

				if (index < cli_switches.size())
				{
					str = cli_switches.get(index).toString();

					if (new File(str).exists())
						collection.setOutputDirectory(str);

					else
						switch_error = true;

					cli_switches.remove(index);
				}

				else
					switch_error = false;

				if (switch_error)
				{
					System.out.println("can't set output directory ...");
					error = true;
				}
			}

			/**
			 * new output name
			 */
			if ((index = getSwitch("-name")) >= 0)
			{
				switch_error = false;

				collection = createCollection(collection);

				if (index < cli_switches.size())
				{
					str = cli_switches.get(index).toString();

					if (str.length() > 0)
						collection.setOutputName(str);

					else
						switch_error = false;

					cli_switches.remove(index);
				}

				else
					switch_error = false;

				if (switch_error)
				{
					System.out.println("can't set output name ...");
					error = true;
				}
			}

			/**
			 * load point list
			 */
			if ((index = getSwitch("-cut")) >= 0)
			{
				switch_error = false;

				collection = createCollection(collection);

				if (index < cli_switches.size())
				{
					str = cli_switches.get(index).toString();

					if (new File(str).exists())
						loadCutPoints(collection, str);

					else
						switch_error = false;

					cli_switches.remove(index);
				}

				else
					switch_error = false;

				if (switch_error)
				{
					System.out.println("can't set cutpoints ...");
					error = true;
				}
			}

			/**
			 * load point list
			 */
			if ((index = getSwitch("-chp")) >= 0)
			{
				switch_error = false;

				collection = createCollection(collection);

				if (index < cli_switches.size())
				{
					str = cli_switches.get(index).toString();

					if (new File(str).exists())
						loadChapterPoints(collection, str);

					else
						switch_error = false;

					cli_switches.remove(index);
				}

				else
					switch_error = false;

				if (switch_error)
				{
					System.out.println("can't set chapterpoints ...");
					error = true;
				}
			}

			/**
			 * load PID list
			 */
			if ((index = getSwitch("-id")) >= 0)
			{
				switch_error = false;

				collection = createCollection(collection);

				if (index < cli_switches.size())
				{
					str = cli_switches.get(index).toString();

					if (str.length() > 0)
						loadIDs(collection, str);

					else
						switch_error = false;

					cli_switches.remove(index);
				}

				else
					switch_error = false;

				if (switch_error)
				{
					System.out.println("can't set pidfilter ...");
					error = true;
				}
			}

			/**
			 * jrmann1999, patch to work with batch file list (.bfl, .tpl)
			 */
			for (int i = 0; i < cli_switches.size(); i++)
			{
				collection = createCollection(collection);

				try {

					String _str = cli_switches.get(i).toString();

					str = _str.toLowerCase();

					XInputFile xif = null;

					if (str.endsWith("bfl") || str.endsWith("tpl"))
					{
						XInputFile _xif = Common.getInputFile(_str);

						if (_xif != null)
						{
							_xif.randomAccessOpen("r");
							_xif.randomAccessSeek(0);

							/**
							 * note: readline does not yet support full unicode here
							 */
							while ((str = _xif.randomAccessReadLine()) != null)
							{
								xif = Common.getInputFile(str);

								if (xif != null)
									collection.addInputFile(xif);
							}

							_xif.randomAccessClose();
						}
					}

					else
					{
						xif = Common.getInputFile(_str);

						if (xif != null)
							collection.addInputFile(xif);
					}

				} catch (Exception e) {

					System.err.println("File input error");
					error = true;
				}
			}

		} catch (Exception ex) {
			error = true;

		} catch (Error er) {
			error = true;
		}

		return error;
	}

	/**
	 *
	 */
	private static void loadIDs(JobCollection collection, String nIDs)
	{
		StringTokenizer st = new StringTokenizer(nIDs, ",");
		String nID = null;
		int i = 0;

		collection.clearPIDs();

		while (st.hasMoreTokens())
		{
			nID = st.nextToken();

			if (!nID.startsWith("0x"))
				nID = "0x" + Integer.toHexString(Integer.parseInt(nID));

			collection.addPID(nID);

			i++;
		}

		Common.setMessage(Resource.getString("msg.loading.pids", String.valueOf(collection.getPIDCount())));
	}

	/**
	 * load cutfile
	 */
	private static void loadCutPoints(JobCollection collection, String file)
	{
		try {

			BufferedReader points = new BufferedReader(new FileReader(file));
			String point = "";

			while (true)
			{
				point = points.readLine();

				if (point == null) 
					break;

				if (point.trim().equals("")) 
					continue;

				if (point.startsWith(Keys.KEY_CutMode[0])) 
				{
					Common.getSettings().setProperty(Keys.KEY_CutMode[0], point.substring(point.indexOf("=") + 1).trim());
					continue;
				}

				if (point.startsWith("(")) 
					continue;

				collection.addCutpoint(point);
			}

			points.close();

			Common.setMessage("Successful reading of " + String.valueOf(collection.getCutpointCount()) + " cutpoints");

		} catch (Exception e5) { 

			Common.setMessage("Error while reading file: '" + file + "'");
			Common.setExceptionMessage(e5);
		}
	}

	/**
	 * load chapterfile
	 */
	private static void loadChapterPoints(JobCollection collection, String file)
	{
		try {

			BufferedReader points = new BufferedReader(new FileReader(file));
			String point = "";

			while (true)
			{
				point = points.readLine();

				if (point == null) 
					break;

				if (point.trim().equals("")) 
					continue;

				collection.addChapterpoint(point);
			}

			points.close();

			Common.setMessage("Successful reading of " + String.valueOf(collection.getChapterpointCount()) + " chapterpoints");

		} catch (Exception e5) { 

			Common.setMessage("Error while reading file: '" + file + "'");
			Common.setExceptionMessage(e5);
		}
	}
}

