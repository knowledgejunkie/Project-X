/*
 * @(#)ColumnLayout.java - 
 *
 * Copyright (c) 2005 by ?, All Rights Reserved. 
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

package net.sourceforge.dvb.projectx.gui;

import java.awt.LayoutManager;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;

public class ColumnLayout implements LayoutManager {

	int xInset = 2;
	int yInset = 0;
	int yGap = 0;

	public void addLayoutComponent(String s, Component c)
	{}

	public void layoutContainer(Container c)
	{
		Insets insets = c.getInsets();
		int height = yInset + insets.top;
      
		Component[] children = c.getComponents();
		Dimension compSize = null;

		for (int i = 0; i < children.length; i++)
		{
			compSize = children[i].getPreferredSize();
			children[i].setSize(compSize.width, compSize.height);
			children[i].setLocation( xInset + insets.left, height);
			height += compSize.height + yGap;
		}
	}

	public Dimension minimumLayoutSize(Container c)
	{
		Insets insets = c.getInsets();
		int height = yInset + insets.top;
		int width = 0 + insets.left + insets.right;
      
		Component[] children = c.getComponents();
		Dimension compSize = null;

		for (int i = 0; i < children.length; i++)
		{
			compSize = children[i].getPreferredSize();
			height += compSize.height + yGap;
			width = Math.max(width, compSize.width + insets.left + insets.right + xInset*2);
		}

		height += insets.bottom;
		return new Dimension( width, height);
	}
  
	public Dimension preferredLayoutSize(Container c)
	{
		return minimumLayoutSize(c);
	}
   
	public void removeLayoutComponent(Component c)
	{}
}
