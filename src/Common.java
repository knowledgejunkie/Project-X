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

//package X

import java.io.*;
import java.awt.*;
import javax.swing.*;

import java.util.zip.*;

//DM13042004 081.7 int01 introduced
public final class Common
{
	//DM18052004 081.7 int02 add
	private static java.text.DateFormat time_format_1 = new java.text.SimpleDateFormat("HH:mm:ss.SSS");
	private static java.text.DateFormat time_format_2 = new java.text.SimpleDateFormat("HH:mm:ss:SSS");

	private Common()
	{}

	// should try a while to rename with success, if file_system is blocked by another app.
	public static void renameTo(File oldfile, File newfile)
	{
		for (int a = 0; a < 10000; a++)
			if ( oldfile.renameTo(newfile) )
				return;

		X.Msg("!> cannot rename " + oldfile.toString() + " to " + newfile.toString());
		X.TextArea.setBackground(new Color(255,225,225));
	}

	public static void renameTo(String oldfile, String newfile)
	{
		renameTo(new File(oldfile), new File(newfile));
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

}