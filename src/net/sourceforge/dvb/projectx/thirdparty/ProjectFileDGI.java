/*
 * @(#)ProjectFileDGI
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

public class ProjectFileDGI implements ProjectFileIF
{
	/**
	 *  basic GOP line:
	 *  7 file position vob cell flags flags ...
	 */

	private ArrayList fields = new ArrayList();

	private final String version = "DGIndexProjectFile06";
	private final String number_of_files = "_NumberOfFiles"; // placeholder
	private final String file_name_x = "_FileNameX";         // placeholder
	private final String aspect_ratio = "Aspect_Ratio=";   // eg. 4:3
	private final String picture_size = "Picture_Size=";   // eg. 720x576
	private final String frame_rate = "Frame_Rate=";       // eg. 25000
	private final String location = "Location=";           // eg. 0,0,0,14AA00
	private final String finish = "FINISHED  0.00% FILM";

	private int framerate_index = -1;
	private int aspectratio_index = -1;
	private int horizontal_size = -1;
	private int vertical_size = -1;

	private int file_index = 0;

	private final String[] frame_type = { " 0", " 1", " 2", " 3" };
	private final String end_of_stream = " 9";

	private String dgi_file = "";

	private ProjectFileDGI()
	{}

	public ProjectFileDGI(String str)
	{
		init(str);
	}

	private void init(String str)
	{
		dgi_file = str + ".dgi.d2v";

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
		fields.add("Stream_Type=0");
		fields.add("iDCT_Algorithm=2 (1:MMX 2:SSEMMX 3:FPU 4:REF 5:SSE2MMX)");
		fields.add("YUVRGB_Scale=1 (0:TVScale 1:PCScale)");
		fields.add("Luminance_Filter=0,0 (Gamma, Offset)");
		fields.add("Clipping=0,0,0,0 (ClipLeft, ClipRight, ClipTop, ClipBottom)");
		fields.add(aspect_ratio);
		fields.add(picture_size);
		fields.add("Field_Operation=0 (0:None 1:ForcedFILM 2:RawFrames)");
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
		if (framerate_index != -1)
			framerate_index = val1;

		if (aspectratio_index != -1)
			aspectratio_index = val2;

		if (horizontal_size != -1)
			horizontal_size = val3;

		if (vertical_size != -1)
			vertical_size = val4;
	}

	/**
	 * frames decodable w/ a reference to a previous gop must be known here
	 * frames w/ backward encoding to this gop first I-frame write w/ a leading "1"
	 * long _position is of picture header start code of first I-frame!
	 */
	public void addGop(long _position, Gop gop)
	{
		String str = "7 " + file_index + " " + _position + " 0 0 ";

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
		if (fields.size() < 16)
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
		 * set frame_rate
		 */
		fields.set( fields.indexOf(frame_rate), frame_rate + Video.getFrameRate(framerate_index));

		/**
		 * set aspectratio
		 */
		fields.set( fields.indexOf(aspect_ratio), aspect_ratio + Video.getAspectRatio(aspectratio_index));

		/**
		 * set picture size
		 */
		fields.set( fields.indexOf(picture_size), picture_size + horizontal_size + "x" + vertical_size);

		/**
		 * get placeholder and add file_index + size
		 * "0,0," means: start at file = 0, position = 0 
		 */
		fields.set( fields.indexOf(location), location + "0,0," + file_index + "," + Long.toHexString(_filesize / 2048L).toUpperCase());

		/**
		 * finish last gop_line
		 */
		fields.set( fields.size() - 1, fields.get(fields.size() - 1).toString() + end_of_stream);

		fields.add("");
		fields.add(finish);

		try {
			BufferedWriter output = new BufferedWriter(new FileWriter(dgi_file));

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
