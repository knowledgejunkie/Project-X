
/*
 * @(#)StreamProcessBase
 *
 * Copyright (c) 2005-2009 by dvb.matt, All rights reserved.
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

import net.sourceforge.dvb.projectx.common.Common;
import net.sourceforge.dvb.projectx.common.Resource;
import net.sourceforge.dvb.projectx.common.Keys;
import net.sourceforge.dvb.projectx.common.JobCollection;
import net.sourceforge.dvb.projectx.common.JobProcessing;

import net.sourceforge.dvb.projectx.xinput.XInputFile;

import net.sourceforge.dvb.projectx.parser.CommonParsing;

/**
 * main thread
 */
public class StreamProcessBase extends Object {

	public int ERRORCODE = 0;
	public int MainBufferSize = 8192000;

	public static double sync_value_1;
	public static double sync_value_2;
	public static double sync_value_3;
	public static double sync_value_4;

	/**
	 * 
	 */
	public StreamProcessBase()
	{
		init();
	}

	/**
	 * 
	 */
	public void init()
	{
		MainBufferSize = Integer.parseInt(Common.getSettings().getProperty(Keys.KEY_MainBuffer));

		if (MainBufferSize <= 0)
			MainBufferSize = 4096000;
	}

	/**
	 * 
	 */
	public boolean pause()
	{
		return Common.waitingMainProcess();
	}

	/**
	 * 
	 */
	public String FramesToTime(int framenumber, double framelength)
	{
		return String.valueOf(Math.round(framenumber * framelength / 90.0));
	}

	/**
	 * pts value to time value
	 */
	public String formatFrameTime(double time_value)
	{
		return Common.formatTime_1((long) (time_value / 90.0));
	}

	/**
	 * loadTempVideoPts
	 */
	public long[][] loadTempVideoPts(String videofile_pts, boolean debug)
	{
		if (videofile_pts.equals("-1"))
			return null;

		if (debug) 
			System.out.println("-> loading video PTS logfile...");   

		XInputFile xInputFile = new XInputFile(new File(videofile_pts));

		int vlogsize = ((int)xInputFile.length() / 16) - 1;
		long[][] vptsval = new long[2][vlogsize]; 

		byte[] data = new byte[(int)xInputFile.length() - 16];
		int pos = 0;

		int j = 0;
		boolean reducedPts = Common.getCollection().getSettings().getBooleanProperty(Keys.KEY_Video_cutPts);

		try {
			InputStream pts_file = xInputFile.getInputStream();

			pts_file.skip(16); //header
			pts_file.read(data, 0, data.length);

			for (int i = 0; i < vlogsize; i += 2 )
			{
				vptsval[0][j] = CommonParsing.getValue(data, pos, 8, !CommonParsing.BYTEREORDERING);
				pos += 8;
				vptsval[0][j + 1] = CommonParsing.getValue(data, pos, 8, !CommonParsing.BYTEREORDERING);
				pos += 8;
				vptsval[1][j] = CommonParsing.getValue(data, pos, 8, !CommonParsing.BYTEREORDERING);
				pos += 8;
				vptsval[1][j + 1] = CommonParsing.getValue(data, pos, 8, !CommonParsing.BYTEREORDERING);
				pos += 8;

				if (debug) 
					System.out.println("#s " + i + " _ " + j + " _ " + vptsval[0][j] + " #e " + (j + 1) + " _" + vptsval[0][j + 1] + " /#s " + i + " _" + vptsval[1][j] + " #e " + (j + 1) + " _" + vptsval[1][j + 1]);

				// ignore equal time boundary
				if (reducedPts && j > 0 && Math.abs(vptsval[0][j] - vptsval[0][j - 1]) < 3 && Math.abs(vptsval[1][j] - vptsval[1][j - 1]) < 3)
				{
					vptsval[0][j - 1] = vptsval[0][j + 1];
					vptsval[1][j - 1] = vptsval[1][j + 1];
				}
				else
					j += 2;
			}

			pts_file.close();

		} catch (Exception e) { 

			Common.setExceptionMessage(e);

			return null;
		}

		if (j > 0 && j < vlogsize)
		{
			long[][] vptsval2 = new long[2][j];

			for (int i = 0; i < j; i++)
			{
				vptsval2[0][i] = vptsval[0][i];
				vptsval2[1][i] = vptsval[1][i];
			}
			
			Common.setMessage("-> " + Resource.getString("video.msg.pts.start_end", Common.formatTime_1(vptsval2[0][0] / 90)) + " " + Common.formatTime_1(vptsval2[0][vptsval2[0].length - 1] / 90));
			Common.setMessage("-> check sync at cut gaps only");

			return vptsval2;
		}

		Common.setMessage("-> " + Resource.getString("video.msg.pts.start_end", Common.formatTime_1(vptsval[0][0] / 90)) + " " + Common.formatTime_1(vptsval[0][vptsval[0].length - 1] / 90));

		return vptsval;
	}


	/**
	 * loadTempOtherPts
	 */
	public long[][] loadTempOtherPts(String filename_pts, String message_1, String message_2, String message_3, String message_4, int es_streamtype, boolean ignoreErrors, boolean debug)
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

		int readbuffer = 512000; // 32000 indices 2*8

		byte[] data = new byte[readbuffer];
		int pos = 0;

		try {
			InputStream pts_file = xInputFile.getInputStream();

			int aa = 0;

			long ptsVal;
			long ptsPos;

			for (int a = 0; a < logsize; a++)
			{
				if (pos % readbuffer == 0)
				{
					pts_file.read(data, 0, readbuffer);
					pos = 0;
				}

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

		Common.setMessage("-> " + Resource.getString(message_3, Common.formatTime_1(ptsval[0][0] / 90)) + " " + Common.formatTime_1(ptsval[0][ptsval[0].length - 2] / 90));

		return ptsval;
	}

	/**
	 * 
	 */
	public int checkPTSMatch(long video_pts_values[], long data_pts_values[])
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
	public String infoPTSMatch(String filename_pts, String videofile_pts, boolean video_pts, boolean data_pts)
	{
		if ( !videofile_pts.equals("-1") && !filename_pts.equals("-1") && !video_pts && data_pts)
			return "? ";

		else
			return "";
	}

	/**
	 * synccheck A/V
	 */
	public boolean SyncCheck(int[] vw, double timecount, double frametimelength, long timeline, int writtenframes, long[] vptsval, long[] vtime, boolean awrite, boolean debug)
	{
		return SyncCheck(vw, timecount, frametimelength, timeline, writtenframes, vptsval, vtime, awrite, debug, "");
	}

	/**
	 * synccheck A/V
	 */
	public boolean SyncCheck(int[] vw, double timecount, double frametimelength, long timeline, int writtenframes, long[] vptsval, long[] vtime, boolean awrite, boolean debug, String src)
	{
		int v = vw[0];
		int w = vw[1];

		// w = gop end time index
		if (w < vptsval.length)
		{
			sync_value_1 = (double)(timeline - vptsval[w + 1]);
			sync_value_2 = (double)(timecount - vtime[w + 1]);

			if (debug) 
				System.out.println("A " + src + " / " + awrite + "/" + v + "/" + w + "/  " + writtenframes + " #nve " + vtime[w + 1] + " /nae " + timecount + " #nvp " + vptsval[w + 1] + " /nap " + timeline + " /sy " + sync_value_2 + "/" + sync_value_1 + "/" + (sync_value_2 - sync_value_1));

			// GOP ende übereinstimmung <= halbe framelänge, mit Timecode Diff Auswertung
			// schreibpause setzen, nächstes gop ende zur berechnung vormerken
			if (Math.abs(sync_value_2) <= (frametimelength / 2.0))
			{
				awrite = false;
				w += 2;
			}

			// GOP ende übereinstimmung <= halbe framelänge, mit PTS Diff Auswertung
			// schreibpause setzen, nächstes gop ende zur berechnung vormerken
			else if (Math.abs(sync_value_1) <= (frametimelength / 2.0))
			{
				awrite = false;
				w += 2;
			}

			if (debug) 
				System.out.println("B " + src + " / " + awrite + "/" + v + "/" + w);
		}

		// v = gop start time index
		if (v < vptsval.length)
		{
			boolean show = false;

			for (; !awrite && v < vptsval.length; v += 2)
			{
				sync_value_3 = (double)(timeline - vptsval[v]); // PTS Unterschied, frame start zu  gop start
				sync_value_4 = (double)(timecount - vtime[v]); // timecode Unterschied, frame start zu  gop start
  
				if (debug) 
					System.out.println("C " + awrite + "/" + v + "/" + w + "/  " + writtenframes + " #cve " + vtime[v] + " /cae " + timecount + " #cvp " + vptsval[v] + " /cap " + timeline + " /sy " + sync_value_4 + "/" + sync_value_3 + "/" + (sync_value_4 - sync_value_3));
  
				// schreibpause, GOP start übereinstimmung <= halbe framelänge, mit PTS Diff Auswertung
				// schreibpause aufheben, nächsten gop start zur berechnung vormerken
				if (!awrite && Math.abs(sync_value_3) <= (frametimelength / 2.0))
				{
					awrite = true; 
					show = true;
					w = v;
					v += 2;
					break;
				}
  
				// schreibpause, GOP start übereinstimmung <= halbe framelänge, mit Timecode Diff + PTS Auswertung
				// schreibpause aufheben, nächsten gop start zur berechnung vormerken
				else if (!awrite && Math.abs(Math.abs(sync_value_4) - Math.abs(sync_value_3)) <= (frametimelength / 2.0))
				{
					awrite = true; 
					show = true;
					w = v; // eine Variable wuerde eigentlich auch reichen
					v += 2;
					break;
				}

				if (sync_value_3 < 0) 
					break;
			}
  
			if (debug)
				System.out.println("D " + src + " / " + awrite + "/" + v + "/" + w);
  				
  				
			// schreibmodus an, halbe framelänge + pts start ist größer als nächster gop start
			// schreibpause
			if (v < vptsval.length && awrite && (timecount + (frametimelength / 2.0)) > vtime[v] ) 
				awrite = false;
	
			if (debug) 
				System.out.println("E " + src + " / " + awrite + "/" + v + "/" + w);

			if (show && awrite) 
				Common.getGuiInterface().showAVOffset("" + (int)(sync_value_3 / 90) + "/" + (int)(sync_value_4 / 90) + "/" + (int)((sync_value_4 - sync_value_3) / 90));
		}

		vw[0] = v;
		vw[1] = w;
		vw[2] = (int) (sync_value_2 - sync_value_1);
		vw[3] = (int) (sync_value_4 - sync_value_3);

		return awrite;
	}
}
