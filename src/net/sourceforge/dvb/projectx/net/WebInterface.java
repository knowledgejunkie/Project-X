/*
 * @(#)WebInterface.java - provides simple remote http access
 *
 * Copyright (C) 2005-2006 by dvb.matt, All Rights Reserved.
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

package net.sourceforge.dvb.projectx.net;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.BindException;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.IOException;

import java.util.StringTokenizer;

import net.sourceforge.dvb.projectx.common.Common;
import net.sourceforge.dvb.projectx.common.Keys;
import net.sourceforge.dvb.projectx.common.JobCollection;
import net.sourceforge.dvb.projectx.xinput.XInputFile;

public class WebInterface implements Runnable {

	private Thread thread = null;

	private long request_number = 0;

	private String last_request = "";
	private String return_string = "";
	private String access_string;

	private boolean isOnline = false;
	private boolean showLog = false;

	private ServerSocket serverSocket = null;
	private Socket connection = null;

	/**
	 *
	 */
	public void run() 
	{
		Thread myThread = Thread.currentThread();

		while (thread == myThread)
		{
			if (!runServer())
				break;

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {}
		}

		isOnline = false;
		showLog = false;
	}

	public void start()
	{
		if (thread != null)
			return;

		thread = new Thread(this, "WebIF");
		thread.setPriority(Thread.MIN_PRIORITY);
		thread.start();

		Common.setMessage("-> re-/start WebIFServer on Port: " + Common.getSettings().getProperty(Keys.KEY_WebServerPort));
	}

	/**
	 *
	 */
	public void stop()
	{
		connection = null;

		try {
			if (serverSocket != null)
				serverSocket.close();

		} catch (Exception ie) { 
		}

		thread = null;
		System.gc();

		Common.setMessage("-> close WebIFServer on Port: " + Common.getSettings().getProperty(Keys.KEY_WebServerPort));
	}

	/**
	 *
	 */
	private boolean runServer() 
	{
		int port_change;
		int port = -1;

		try {

			request_number = 0;

			while (true)
			{
				if ((port_change = getPort()) < 0)
					return false;

				if ((access_string = getAccess()) == null)
					return false;

				isOnline = true;

				// init server
				if (port_change != port)
				{
					port = port_change;
					serverSocket = new ServerSocket(port);
				}

				connection = serverSocket.accept(); // awaiting conn.

				BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));

				request_number++;

				if (!getRequest(br.readLine(), connection)) //resolve http command
					return_string = "<B>missing or wrong keyword</B>";

				// answer
				PrintWriter pw = new PrintWriter(connection.getOutputStream());

				pw.println(buildSite(return_string));
				pw.flush();

				connection.close();

			}

		} catch (Exception e) { 

			Common.setMessage("" + e);
			Common.setMessage("" + e.getMessage(), true);

			connection = null;

			try {
				serverSocket.close();

			} catch (Exception ie) { 
			}

		}


		System.gc();

		return true;
	}

	/**
	 *
	 */
	private int getPort() 
	{
		int value = -1;

		try {
			value = Common.getSettings().getIntProperty(Keys.KEY_WebServerPort);

			if (value < 0 || value > 65535)
				throw new Exception();

		} catch (Exception e) {
			Common.setMessage("!> WebIF: invalid port number: '" + value + "'", true);
			value = -1;
		}

		return value;
	}

	/**
	 *
	 */
	private String getAccess() 
	{
		String str = null;

		try {
			str = Common.getSettings().getProperty(Keys.KEY_WebServerAccess).trim();

			if (str == null || str.length() == 0)
				throw new Exception();

		} catch (Exception e) {
			Common.setMessage("!> WebIF: invalid access_string: '" + str + "'", true);
			str = null;
		}

		return str;
	}

	/**
	 *
	 */
	public boolean isOnline() 
	{
		return isOnline;
	}

	/**
	 *
	 */
	private String buildSite(String body) 
	{
		String str = 
			"HTTP/1.0 200 Ok\n" + 
			"Content-type: text/html\n\n" +
			"<HTML><HEAD>" + 
			"<META HTTP-EQUIV=\"PRAGMA\" CONTENT=\"NO-CACHE\">\n" +
			"<META HTTP-EQUIV=\"EXPIRES\" CONTENT=\"0\">\n" +
			"<META HTTP-EQUIV=\"CACHE-CONTROL\" CONTENT=\"PRIVATE\">\n" +
			"<TITLE>ProjectX Status of " + 
			Common.getVersionName() + "/" + Common.getVersionDate() +
			"</TITLE></HEAD>\n" +
			"<BODY><H1>ProjectX WebInterface</H1>\n" +
			"<BR>request# " + request_number + " - " + Common.getVersionName() + " / " + Common.getVersionDate() +
			" - " + Common.getDateAndTime() +
			"<BR><PRE>" + 
			body + 
			"</PRE></BODY></HTML>";

		return str;

		//	"<META HTTP-EQUIV=\"REFRESH\" CONTENT=\"5\">\n" +
	}

	/**
	 *
	 */
	private boolean getRequest(String str, Socket connection) 
	{
		last_request = str;

		// log the msg
		Common.setMessage("--> request# " + request_number + " from " + connection.getInetAddress().getHostAddress() + " - " + str);

		return_string = "";

		str = str.trim();

		int index_GET = str.indexOf("GET /");

		// only 'get' cmd accepted
		if (index_GET >= 3 || index_GET < 0)
			return false;

		// only http accepted
		if (!str.endsWith("HTTP/1.0") && !str.endsWith("HTTP/1.1"))
			return false;

		// raw parameters
		str = str.substring(index_GET + 5, str.indexOf("HTTP/1"));
		str = str.trim();

		// user access string 
		if (!str.startsWith(access_string))
			return false;

		StringTokenizer st = new StringTokenizer(str, "&");

		while (st.hasMoreTokens())
			getCommand(st.nextToken());

		if (showLog)
			return_string += getLog();

		// std answer body
		return_string += getCollection();

		return true;
	}

	/**
	 *
	 */
	private void getCommand(String str) 
	{
		if (str.equals("addcoll"))
		{
			Common.addCollection();
			Common.setActiveCollection(Common.getCollectionListSize() - 1);
			updateCollectionView();
		}

		else if (str.equals("removecoll"))
		{
			if (Common.removeCollection(Common.getActiveCollection()))
				updateCollectionView();
		}

		else if (str.equals("exit"))
			Common.exitApplication(0);

		else if (str.equals("start"))
		{
			if (Common.startProcess())
				Common.startMainProcess();
		}

		else if (str.equals("stop"))
			Common.breakMainProcess();

		else if (str.equals("getlog"))
			showLog = true;

		else if (str.equals("hidelog"))
			showLog = false;

		else if (str.equals("filelist"))
			return_string += getInputFileList();

		else if (str.equals("getsettings"))
			return_string += getSettings();

		else if (str.startsWith("addfile"))
		{
			JobCollection collection = Common.getCollection();

			if (collection != null)
			{
				Object[] files = Common.reloadInputDirectories();

				int index = Integer.parseInt(str.substring(7));

				if (files.length > index)
					collection.addInputFile(files[index]);
			}

			updateCollectionView();
		}

		else if (str.startsWith("removefile"))
		{
			JobCollection collection = Common.getCollection();

			if (collection != null)
				collection.removeInputFile(Integer.parseInt(str.substring(10)));

			updateCollectionView();
		}

		else if (str.startsWith("setcoll"))
		{
			Common.setActiveCollection(Integer.parseInt(str.substring(7)));
			updateCollectionView();
		}
	}

	/**
	 *
	 */
	private void updateCollectionView()
	{
		Common.getGuiInterface().showActiveCollection(Common.getActiveCollection());
	}

	/**
	 *
	 */
	private String getInputFileList() 
	{
		String str = "";

		Object[] files = Common.reloadInputDirectories();

		str += "<HR><I>List of available files:</I>";

		for (int i = 0; i < files.length; i++)
			str += "<BR><A HREF=" + access_string + "&addfile" + i + ">add file</A> " + i + " - <B>" + files[i].toString() + "</B>";

		return str;
	}

	/**
	 *
	 */
	private String getCollection() 
	{
		String str = "";

		//exit
		str += "<HR><A HREF=" + access_string + "&exit>shutdown remote application</A>";

		//settings
		str += "<HR><A HREF=" + access_string + "&getsettings>get current settings</A>";

		//status
		str += "<HR><I>Process Status:</I> <A HREF=" + access_string + "&getlog>(show message log)</A>";
		str += "<BR>" +	Common.getProcessedPercent() + "% - " + Common.getStatusString();
		str += "<BR><A HREF=" + access_string + "&start>start processing</A>";
		str += " / <A HREF=" + access_string + "&stop>stop processing</A>";

		//collection
		str += "<HR><I>Collection access:</I>";
		str += "<BR><B>active Collection: " + Common.getActiveCollection() + "</B>";
		str += "<BR><A HREF=" + access_string + "&removecoll>remove active Collection</A>";
		str += " / <A HREF=" + access_string + "&addcoll>add new Collection</A>";
		str += "<BR>choose other Collection:";

		for (int i = 0, j = Common.getCollectionListSize(); i < j; i++)
			str += " <A HREF=" + access_string + "&setcoll" + i + ">" + i + "</A>";

		JobCollection collection = Common.getCollection();

		if (collection != null)
		{
			str += "<HR><I>Infos:</I>\n";
			str += "<U>Output to:</U> '" + collection.getOutputDirectory() + "'\n";
			str += collection.getShortSummary();
			str += getCollectionFiles(collection);
		}

		return str;
	}

	/**
	 *
	 */
	private String getCollectionFiles(JobCollection collection) 
	{
		String str = "";

		Object[] files = collection.getInputFiles();

		str += "<HR><I>Content:</I> <A HREF=" + access_string + "&filelist>add file from fixed directories</A>";

		for (int i = 0; i < files.length; i++)
		{
			str += "<BR>" + i + " - <B>" + files[i].toString() + "</B> <A HREF=" + access_string + "&removefile" + i + ">remove</A>";
			str += "<BR><UL>" + ((XInputFile)files[i]).getStreamInfo().getFullInfo() + "</UL>";
		}

		return str;
	}

	/**
	 *
	 */
	private String getLog() 
	{
		String str = "";

		//status
		str += "<HR><I>current processing log:</I> <A HREF=" + access_string + "&hidelog>(hide message log)</A> <A HREF=" + access_string + "&getlog>(refresh log)</A>";
		str += "<BR>" +	Common.getMessageLog();

		return str;
	}

	/**
	 *
	 */
	private String getSettings() 
	{
		String str = "";
		String line;

		//settings
		str += "<HR><I>current settings:</I> <A HREF=" + access_string + ">(hide settings)</A>";

		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(Common.getSettings().storeProperties())));

			while ((line = br.readLine()) != null)
				str += "<BR>" +	line;

		} catch (IOException e) {
		}

		return str;
	}

}