/*
 * @(#)Bitmap.java - provides a Bitmap buffer from painted subpic 
 *
 * Copyright (c) 2004-2011 by dvb.matt, All Rights Reserved.
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

package net.sourceforge.dvb.projectx.subtitle;

//DM24042004 081.7 int02 introduced

import java.util.ArrayList;

public class Bitmap extends Object {

	private int width = 0;
	private int height = 0;
	private int depth = 0;
	private int pixel[] = null;
	private int page_id = -1;
	private int region_id = -1;
	private int object_id = -1;
	private int x = 0;
	private int y = 0;
	private long in_time = -1;
	private int play_time = -1;
	private ArrayList color_indices = new ArrayList();
	private ArrayList color_table = new ArrayList();

	public Bitmap()
	{}

	public Bitmap(int newx, int newy, int w, int h, int[] p, int d, int page, int region, int object, long pts, int time)
	{
		x = newx;
		//y = newy;
		y = newy & ~1; //DM26052004 081.7 int03 changed
		width = w;
		height = h;
		pixel = p;
		depth = d;
		page_id = 0xFF & page;
		region_id = 0xFF & region;
		object_id = 0xFFFF & object;
		in_time = pts;
		play_time = time;
	}

	public int getX()
	{
		return x;
	}

	public int getY()
	{
		return y;
	}

	public int getMaxX()
	{
		return (x + width);
	}

	public int getMaxY()
	{
		return (y + height);
	}

	public int getWidth()
	{
		return width;
	}

	public int getHeight()
	{
		return height;
	}

	public void setWidth(int val)
	{
		width = val;
	}

	public void setHeight(int val)
	{
		height = val;
	}

	public void setPixel(int[] p)
	{
		pixel = p;
	}

	public int[] getPixel()
	{
		return pixel;
	}

	public int getDepth()
	{
		return depth;
	}

	public int getPageId()
	{
		return page_id;
	}

	public int getRegionId()
	{
		return region_id;
	}

	public int getObjectId()
	{
		return object_id;
	}

	public int getId()
	{
		//return (page_id<<24 | region_id<<16 | object_id);
		return page_id;
	}

	public void setTime(long time_1, int time_2)
	{
		in_time = time_1;
		play_time = time_2;
	}

	public long getInTime()
	{
		return in_time;
	}

	public int getPlayTime()
	{
		return play_time;
	}

	public void createColorTable()
	{
		for (int i = 0; i < pixel.length; i++)
		{
			String pixel_str = String.valueOf(pixel[i]);

			if (color_table.contains(pixel_str))
				continue;

			color_table.add(pixel_str);
		}
	}

	public void clearColorTable()
	{
		color_table.clear();
	}

	public Object[] getColorTable()
	{
		return color_table.toArray();
	}

	public ArrayList getColorTableArray()
	{
		return color_table;
	}

	// returns 0 .. 3, indices > 3 mapped to 3
	public int getColorIndex(int color_index)
	{
		String color_index_str = "" + color_index;
		int index = color_indices.indexOf(color_index_str);

		if (index != -1)
			return index;

		else if (color_indices.size() < 4)
			color_indices.add(color_index_str);

		return (color_indices.size() - 1);
	}

	public void clearColorIndices()
	{
		color_indices.clear();
	}

	public Object[] getColorIndices()
	{
		return color_indices.toArray();
	}
}
