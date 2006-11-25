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

import net.sourceforge.dvb.projectx.parser.StreamProcessBase;
import net.sourceforge.dvb.projectx.parser.StreamProcessAudio;
import net.sourceforge.dvb.projectx.parser.StreamProcessLPCMAudio;
import net.sourceforge.dvb.projectx.parser.StreamProcessTeletext;
import net.sourceforge.dvb.projectx.parser.StreamProcessSubpicture;

/**
 * main thread
 */
public class StreamProcess extends Object {

	private StreamProcessBase impl = null;

	/**
	 * 
	 */
	public StreamProcess(int es_streamtype, JobCollection collection, String[] values)
	{
		process(es_streamtype, collection, new XInputFile(new File(values[0])), values[1], values[2], values[3], 0);
	}

	/**
	 * 
	 */
	public StreamProcess(int es_streamtype, JobCollection collection, String filename, String filename_pts, String filename_type, String videofile_pts)
	{
		process(es_streamtype, collection, new XInputFile(new File(filename)), filename_pts, filename_type, videofile_pts, 0);
	}

	/**
	 * 
	 */
	public StreamProcess(int es_streamtype, JobCollection collection, XInputFile aXInputFile, String filename_pts, String filename_type, String videofile_pts, int isElementaryStream)
	{
		process(es_streamtype, collection, aXInputFile, filename_pts, filename_type, videofile_pts, isElementaryStream);
	}

	/**
	 * 
	 */
	private void process(int es_streamtype, JobCollection collection, XInputFile aXInputFile, String filename_pts, String filename_type, String videofile_pts, int isElementaryStream)
	{
		switch (es_streamtype)
		{
		case CommonParsing.AC3_AUDIO:
		case CommonParsing.DTS_AUDIO:
		case CommonParsing.MPEG_AUDIO:
		case CommonParsing.WAV_AUDIO:
			impl = new StreamProcessAudio(collection, aXInputFile, filename_pts, filename_type, videofile_pts, isElementaryStream);
			break;

		case CommonParsing.LPCM_AUDIO:
			impl = new StreamProcessLPCMAudio(collection, aXInputFile, filename_pts, filename_type, videofile_pts, isElementaryStream);
			break;

		case CommonParsing.TELETEXT:
			impl = new StreamProcessTeletext(collection, aXInputFile, filename_pts, filename_type, videofile_pts, isElementaryStream);
			break;

		case CommonParsing.SUBPICTURE:
			impl = new StreamProcessSubpicture(collection, aXInputFile, filename_pts, filename_type, videofile_pts, isElementaryStream);
			break;

		case CommonParsing.UNKNOWN:
		case CommonParsing.MPEG_VIDEO:
		default:
			Common.setMessage("!> unsupported stream to process: " + es_streamtype);
			break;
		}
	}

}
