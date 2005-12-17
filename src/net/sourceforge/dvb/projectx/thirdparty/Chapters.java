/*
 * @(#)Chapters.java
 *
 * Copyright (c) 2004-2005 by dvb.matt, All Rights Reserved.
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
import java.io.PrintWriter;
import java.io.FileOutputStream;

import java.util.Arrays;
import java.util.ArrayList;

public class Chapters extends Object {

	boolean active = false;
	String file_separator = System.getProperty("file.separator");

	ArrayList list;

	public Chapters()
	{
		list = new ArrayList();
	}

	public void init(boolean b)
	{
		active = b;
		list.clear();
	}

	public void addChapter(String time)
	{
		if (!active)
			return;

		list.add(time);
	}

	public void addChapter(String time, String comment)
	{
		if (!active)
			return;

		list.add(time + " ; " + comment);
	}

	public void finish(String str, String name) throws IOException
	{
		if (!active || list.size() == 0)
			return;

		if (!str.endsWith(file_separator))
			str += file_separator;

		str += (name + ".chp.txt");

		Object chapters[] = list.toArray();

		Arrays.sort(chapters);

		write(str, chapters);

		list.clear();
	}
		
	private void write(String str, Object chapters[]) throws IOException
	{
		PrintWriter out = new PrintWriter(new FileOutputStream(str));

		for (int a=0; a < chapters.length; a++)
			out.println(chapters[a]);

		out.flush();
		out.close();
	}
}
