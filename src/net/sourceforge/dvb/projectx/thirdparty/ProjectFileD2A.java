/*
 * @(#)ProjectFileD2A
 *
 * Copyright (c) 2005 by dvb.matt, All Rights Reserved. 
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

package net.sourceforge.dvb.projectx.thirdparty;


import java.io.IOException;
import java.io.BufferedWriter;
import java.io.FileWriter;

import java.util.ArrayList;

import net.sourceforge.dvb.projectx.common.Resource;
import net.sourceforge.dvb.projectx.common.Common;
import net.sourceforge.dvb.projectx.video.Video;
import net.sourceforge.dvb.projectx.parser.Gop;

public class ProjectFileD2A implements ProjectFileIF
{
	/**
	 *  basic GOP line:
	 *  7 file position/2048 flags flags ...
	 */

	private ArrayList fields = new ArrayList();

	private final String version = "DVD2AVIProjectFile";
	private final String number_of_files = "_NumberOfFiles"; // placeholder
	private final String file_name_x = "_FileNameX";         // placeholder
	private final String frame_rate = "Frame_Rate=";       // eg. 25000
	private final String location = "Location=";           // eg. 0,0,0,14AA00
	private final String finish = "FINISHED";

	private int file_index = 0;

	private final String[] frame_type = { " 0", " 1", " 2", " 3" };
	private final String end_of_stream = " 9";

	private String d2v_file = "";

	private ProjectFileD2A()
	{}

	public ProjectFileD2A(String str)
	{
		init(str);
	}

	private void init(String str)
	{
		d2v_file = str + ".d2v";

		reset();
		addBasicFields();
	}

	private void reset()
	{
		file_index = 0;
		fields.clear();
	}

	private void addBasicFields()
	{
		fields.add(version);
		fields.add(number_of_files);
		fields.add(file_name_x);
		fields.add("");
		fields.add("Stream_Type=0,0,0");
		fields.add("iDCT_Algorithm=2");
		fields.add("YUVRGB_Scale=1");
		fields.add("Luminance=128,0");
		fields.add("Picture_Size=0,0,0,0,0,0");
		fields.add("Field_Operation=0");
		fields.add(frame_rate);
		fields.add(location);
		fields.add("");
	}

	public int getPart()
	{
		return file_index;
	}

	/**
	 * set new file segment before placeholder
	 */
	public void addFileSegment(String str)
	{
		fields.add( fields.indexOf(file_name_x), "" + str.length() + " " + str);
		file_index++;
	}

	/**
	 * sets video_data
	 */
	public void setVideoFormat(int val1, int val2, int val3, int val4)
	{
		// empty
	}

	/**
	 * long _position is of picture header start code of first I-frame!
	 */
	public void addGop(long _position, Gop gop)
	{
		String str = "7 " + file_index + " " + (_position / 2048L) + " ";

		fields.add(str);
	}

	public void addAudio(long startposition, byte[] frame)
	{
		// empty
	}


	public void finish(long _filesize)
	{
		/**
		 * no GOP line was written
		 */
		if (fields.size() < 14)
		{ 
			reset(); 
			return;
		}

		/**
		 * remove placeholder
		 */
		fields.remove( fields.indexOf(file_name_x));

		/**
		 * remove placeholder and set number of files
		 */
		fields.set( fields.indexOf(number_of_files), String.valueOf(file_index + 1));

		/**
		 * get placeholder and add file_index + size
		 * "0,0," means: start at file = 0, position = 0 
		 */
		fields.set( fields.indexOf(location), location + "0,0," + file_index + "," + Long.toHexString(_filesize / 2048L).toUpperCase());

		/**
		 * finish last gop_line
		 */
		fields.set( fields.size() - 1, fields.get(fields.size() - 1).toString() + frame_type[2] + frame_type[2] + end_of_stream);

		fields.add("");
		fields.add(finish);

		try {
			BufferedWriter output = new BufferedWriter(new FileWriter(d2v_file));

			for (int i = 0; i < fields.size(); i++)
			{
				output.write(fields.get(i).toString());
				output.newLine();
			}

			output.flush();
			output.close();

		} catch (IOException e) { 

			Common.setExceptionMessage(e); 
		}

		reset();
	}
}
