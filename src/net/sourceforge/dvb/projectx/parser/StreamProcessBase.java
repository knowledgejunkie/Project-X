
/*
 * @(#)StreamParser
 *
 * Copyright (c) 2005 by dvb.matt, All rights reserved.
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
	public boolean SyncCheck(int[] vw, double timecount, double timelength, long timeline, int mpf, long[] vptsval, long[] vtime, boolean awrite, boolean debug)
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
}
