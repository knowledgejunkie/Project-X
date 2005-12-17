/*
 * @(#)ProjectFile
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

import net.sourceforge.dvb.projectx.thirdparty.ProjectFileIF;

import net.sourceforge.dvb.projectx.common.Common;
import net.sourceforge.dvb.projectx.common.Resource;
import net.sourceforge.dvb.projectx.common.Keys;
import net.sourceforge.dvb.projectx.parser.Gop;

import java.util.ArrayList;


public class ProjectFile implements ProjectFileIF
{
	private ArrayList impl_list = null;

	private ProjectFileIF impl = null;

	private int file_index = 0;

	private ProjectFile()
	{}

	public ProjectFile(String str)
	{
		retrieveType(str);
	}

	private void retrieveType(String str)
	{
		impl_list = new ArrayList();

		if (Common.getSettings().getBooleanProperty(Keys.KEY_ExternPanel_createD2vIndex))
			impl_list.add(new ProjectFileD2A(str));

		if (Common.getSettings().getBooleanProperty(Keys.KEY_ExternPanel_createDgiIndex))
			impl_list.add(new ProjectFileDGI(str));

		file_index = 0;
	}

	public int getPart()
	{
		return file_index;
	}

	public boolean splitVideo()
	{
		return Common.getSettings().getBooleanProperty(Keys.KEY_ExternPanel_splitProjectFile);
	}

	public void addFileSegment(String segment_filename)
	{
		for (int i = 0; i < impl_list.size(); i++)
		{
			impl = (ProjectFileIF)impl_list.get(i);

			if (impl != null)
				impl.addFileSegment(segment_filename);
		}

		file_index++;
	}

	public void setVideoFormat(int framerate_index, int aspectratio_index, int horizontal_size, int vertical_size)
	{
		for (int i = 0; i < impl_list.size(); i++)
		{
			impl = (ProjectFileIF)impl_list.get(i);

			if (impl != null)
				impl.setVideoFormat(framerate_index, aspectratio_index, horizontal_size, vertical_size);
		}
	}

	public void addGop(long startposition, Gop gop)
	{
		for (int i = 0; i < impl_list.size(); i++)
		{
			impl = (ProjectFileIF)impl_list.get(i);

			if (impl != null)
				impl.addGop(startposition, gop);
		}
	}

	public void addAudio(long startposition, byte[] frame)
	{
		for (int i = 0; i < impl_list.size(); i++)
		{
			impl = (ProjectFileIF)impl_list.get(i);

			if (impl != null)
				impl.addAudio(startposition, frame);
		}
	}

	public void finish(long filesize)
	{
		for (int i = 0; i < impl_list.size(); i++)
		{
			impl = (ProjectFileIF)impl_list.get(i);

			if (impl != null)
				impl.finish(filesize);
		}
	}
}
