/*
 * @(#)Common.java - carries various stuff 
 *
 * Copyright (c) 2004 by dvb.matt, All Rights Reserved.
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


import java.awt.Color;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.StringTokenizer;

import javax.swing.JOptionPane;

import net.sourceforge.dvb.projectx.audio.Audio;

//DM13042004 081.7 int01 introduced
public final class Common
{
	/** name of the colours table file */
	private static final String COLOUR_TABLES_FILENAME = "colours.tbl";
	
	/** name of the ac3 file */
	private static final String AC3_FILENAME = "ac3.bin";
	
	/** list of AC3 frames */
	private static ArrayList AC3list = new ArrayList();

	//DM18052004 081.7 int02 add
	private static java.text.DateFormat time_format_1 = new java.text.SimpleDateFormat("HH:mm:ss.SSS");
	private static java.text.DateFormat time_format_2 = new java.text.SimpleDateFormat("HH:mm:ss:SSS");

	private Common()
	{}

	// should try a while to rename with success, if file_system is blocked by another app.
	public static boolean renameTo(File oldfile, File newfile)
	{
		for (int a = 0; a < 10000; a++)
			if ( oldfile.renameTo(newfile) )
				return true;

		X.Msg(Resource.getString("common.rename_error1") + " " + oldfile.toString() + " " + Resource.getString("common.rename_error2") + " " + newfile.toString(), true);
		X.TextArea.setBackground(new Color(255,225,225));

		return false;
	}

	public static boolean renameTo(String oldfile, String newfile)
	{
		return renameTo(new File(oldfile), new File(newfile));
	}

	//DM202004 081.7 int02 add
	public static int nextBits(byte buffer[], int BitPos, int N)
	{
		int Pos, Val;
		Pos = BitPos>>>3;
		Val =   (0xFF & buffer[Pos])<<24 |
			(0xFF & buffer[Pos+1])<<16 |
			(0xFF & buffer[Pos+2])<<8 |
			(0xFF & buffer[Pos+3]);
		Val <<= BitPos & 7;
		Val >>>= 32-N;
		return Val;
	}

	//DM18052004 081.7 int02 add
	public static String adaptString(int str, int len)
	{
		return adaptString(String.valueOf(str), len);
	}

	//DM18052004 081.7 int02 add
	public static String adaptString(String str, int len)
	{
		StringBuffer strbuf = new StringBuffer(str.trim());

		while (strbuf.length() < len)
			strbuf.insert(0, "0");

		return strbuf.toString();
	}

	//DM18052004 081.7 int02 add
	public static String formatTime_1(long time_value)
	{
		time_format_1.setTimeZone(java.util.TimeZone.getTimeZone("GMT+0:00"));
		return time_format_1.format(new java.util.Date(time_value));
	}

	//DM18052004 081.7 int02 add
	public static String formatTime_2(long time_value, long frame_rate)
	{
		time_format_2.setTimeZone(java.util.TimeZone.getTimeZone("GMT+0:00"));
		String time_str = time_format_2.format(new java.util.Date(time_value));

		return (time_str.substring(0, time_str.length() - 3) + adaptString((Integer.parseInt(time_str.substring(time_str.length() - 3)) * 90 / (int)frame_rate), 2));
	}


	//DM13062004 081.7 int04 add
	public static Object[] checkUserColourTable() throws IOException
	{
		URL url = Resource.getResourceURL(COLOUR_TABLES_FILENAME);

		if (url == null)
			return null;

		BufferedReader table = new BufferedReader( new InputStreamReader(url.openStream()));
		ArrayList list = new ArrayList();
		String line;

		while( (line = table.readLine()) != null)
		{
			if ( line.trim().length() == 0 )
				continue;

			if ( line.startsWith("table") )
				list.add( (line.substring( line.indexOf("=") + 1)).trim() );
		}

		table.close();

		return list.toArray();
	}

	//DM13062004 081.7 int04 add
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
				table_match = line.substring( line.indexOf("=") + 1).trim().equals(model) ? true : false;

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
			
				X.TextArea.append("\r\n\n" + Resource.getString("ac3.msg.loading.start"));
			
				int a=0, frame_counter=0;
				while (a < check.length) 
				{
					audio.AC3_parseHeader(check,a);
					X.TextArea.append("\r\n(" + frame_counter + ") " + audio.AC3_saveAnddisplayHeader());
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
			X.TextArea.append("\r\n" + Resource.getString("ac3.msg.loading.error")); 
			AC3list.clear();
		}
	
		if (AC3list.size() > 0)
		{
			X.TextArea.append("\r\n" + Resource.getString("ac3.msg.frames", ""+AC3list.size()));
		}
	}
	
	
	/**
	 * Returns the AC3list.
	 * 
	 * @return ArrayList
	 */
	public static ArrayList getAC3list() {
		return AC3list;
	}

	/**
	 * Checks the latest version of Project-X
	 */
	public static void checkVersion() {
		try
		{
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
				{
					version = st.nextToken();
				}
				if (st.hasMoreTokens())
				{
					date = st.nextToken();
				}
			}
			if (date != null)
			{
				version += "\n"+date;
			}
			JOptionPane.showMessageDialog(X.frame, Resource.getString("help.version.info") + "\n"+version, Resource.getString("help.version.info.title"), JOptionPane.INFORMATION_MESSAGE);
		}
		catch(Exception e)
		{
			X.Msg(Resource.getString("help.version.error") + " " + e);
		}
	}


	public final static int Unsupported   = 0;	//	"unsupported"
	public final static int PVA_TYPE      = 1;	//	"PVA (Video/Audio PES)"
	public final static int MPEG1PS_TYPE  = 2;	//	"MPEG-1 PS/SS (Video/Audio PES)"
	public final static int MPEG2PS_TYPE  = 3;	//	"MPEG-2 PS/SS (Video/Audio PES)"
	public final static int PES_AV_TYPE   = 4;	//	"PES (Video/Audio/TTX)"
	public final static int PES_MPA_TYPE  = 5;	//	"PES (MPEG Audio)"
	public final static int PES_PS1_TYPE  = 6;	//	"PES (private stream 1)"
	public final static int ES_AC3_TYPE   = 7;	//	"ES (AC-3 Audio)"
	public final static int ES_MPA_TYPE   = 8;	//	"ES (MPEG Audio)"
	public final static int ES_MPV_TYPE   = 9;	//	"ES (MPEG Video)"
	public final static int ES_AC3_A_TYPE = 10;	//	"ES (AC-3 Audio) (psb. SMPTE)"
	public final static int TS_TYPE       = 11;	//	"DVB/MPEG2 TS"
	public final static int ES_DTS_TYPE   = 12;	//	"ES (DTS Audio)"
	public final static int ES_DTS_A_TYPE = 13;	//	"ES (DTS Audio) (psb. SMPTE)"
	public final static int ES_RIFF_TYPE  = 14;	//	"ES (RIFF Audio)"
	public final static int ES_cRIFF_TYPE = 15;	//	"ES (compressed RIFF Audio)"
	public final static int ES_SUP_TYPE   = 16;	//	"ES (Subpicture 2-bit RLE)"


	private static String ftp_server = "192.168.0.5";
	private static String ftp_port = "21";
	private static String ftp_user = "root";
	private static String ftp_password = "dreambox";
	private static String ftp_directory = "/hdd/movie";
	private static String ftp_command = "";

	public static void setFTPServer(String server, String user, String password, String directory, String port)
	{
		ftp_server = server != null ? server : ftp_server;
		ftp_port = port != null ? port : ftp_port;
		ftp_user = user != null ? user : ftp_user;
		ftp_password = password != null ? password : ftp_password;
		ftp_directory = directory != null ? directory : ftp_directory;
	}

	public static String getFTP_Server()
	{
		return ftp_server;
	}

	public static String getFTP_Port()
	{
		return ftp_port;
	}

	public static String getFTP_User()
	{
		return ftp_user;
	}

	public static String getFTP_Password()
	{
		return ftp_password;
	}

	public static String getFTP_Directory()
	{
		return ftp_directory;
	}

	public static String getFTP_Command()
	{
		return ftp_command;
	}

	public static void setFTP_Server(String server)
	{
		ftp_server = server != null ? server : ftp_server;
	}

	public static void setFTP_Port(String port)
	{
		ftp_port = port != null ? port : ftp_port;
	}

	public static void setFTP_User(String user)
	{
		ftp_user = user != null ? user : ftp_user;
	}

	public static void setFTP_Password(String password)
	{
		ftp_password = password != null ? password : ftp_password;
	}

	public static void setFTP_Directory(String directory)
	{
		ftp_directory = directory != null ? directory : ftp_directory;
	}

	public static void setFTP_Command(String command)
	{
		ftp_command = command != null ? command : ftp_command;
	}
}