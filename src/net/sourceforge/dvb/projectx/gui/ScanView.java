/*
 * @(#)CutView
 *
 * Copyright (c) 2005-2006 by dvb.matt, All Rights Reserved. 
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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.MemoryImageSource;
import java.awt.Font;

import java.util.Arrays;

import javax.swing.JPanel;
import javax.swing.JSlider;


public class ScanView extends JPanel {

	private final int Top = 20;
	private final int Bottom = 184;

	private int[] image_data;

	private int width = 160;
	private int height = 90;

	private boolean matchingPoint = false;

	private boolean cut_top = true;
	private boolean cut_bottom = false;
	private boolean cut_match = false;

	private String string_bottom = "Next:";
	private String string_top = "Prev:";
	private String string_matchingpoint = "";

	private String[] string_in_out = { " -IN- ", " -OUT- " };

	private Image image_top;
	private Image image_bottom;

	private MemoryImageSource source_top;
	private MemoryImageSource source_bottom;

	private final Color BackgroundColor;
	private final Color RedColor;

	private final Font font;

	/**
	 *
	 */
	public ScanView()
	{ 
		image_data = new int[width * height];

		source_top = new MemoryImageSource(width, height, image_data, 0, width);
		source_top.setAnimated(true);
		image_top = createImage(source_top);

		source_bottom = new MemoryImageSource(width, height, image_data, 0, width);
		source_bottom.setAnimated(true);
		image_bottom = createImage(source_bottom);

		BackgroundColor = new Color(0, 35, 110);
		RedColor = new Color(255, 100, 100);

		font = new Font("Tahoma", Font.PLAIN, 14);

		setLayout(new BorderLayout());
		add(buildScanViewPanel(), BorderLayout.SOUTH);

		setBackground(Color.black);
		setVisible(true);
	}


	/**
	 *
	 */
	protected JPanel buildScanViewPanel()
	{
		JPanel panel = new JPanel(new BorderLayout());

		JSlider scanSlider = new JSlider();
		scanSlider.setMaximum(100);
		scanSlider.setMajorTickSpacing(10);
		scanSlider.setMinorTickSpacing(1);
		scanSlider.setPaintTicks(true);
		scanSlider.setValue(50);

		panel.add(scanSlider, BorderLayout.SOUTH);

		return panel;
	}

	/**
	 *
	 */
	public int getTop()
	{
		return Top;
	}

	/**
	 *
	 */
	public int getBottom()
	{
		return Bottom;
	}

	/**
	 *
	 */
	public void clearViews()
	{
		matchingPoint = false;
		clearView(Top);
		clearView(Bottom);
	}

	/**
	 *
	 */
	public void clearView(int position)
	{
		Arrays.fill(image_data, 0xFF505050);

		updateView(position);
	}

	/**
	 *
	 */
	public void updateView(int position)
	{
		updateView(position, null, -1);
	}

	/**
	 *
	 */
	public void updateView(int position, Object[] object, int index)
	{
		if (position < 100)
		{
			source_top.newPixels();
			string_top = "Prev: " + (object != null ? "#" + index + " @ " + object[index] + string_in_out[index & 1] : "= Collection Begin");
			cut_top = object != null ? (index & 1) == 0 : (!matchingPoint ? !cut_bottom : false);
		}

		else
		{
			source_bottom.newPixels();
			string_bottom = "Next: " + (object != null ? "#" + index + " @ " + object[index] + string_in_out[index & 1] : "= Collection End");
			cut_bottom = object != null ? (index & 1) == 0 : false;
		}

		repaint();
	}

	/**
	 *
	 */
	public void setImage(int[] data, Object[] object, int index, int position)
	{
		if (data == null)
		{
			clearView(position);
			return;
		}

		System.arraycopy(data, 0, image_data, 0 , data.length);

		updateView(position, object, index);
	}

	/**
	 *
	 */
	public void setMatchingPoint(boolean b, Object[] object, int index)
	{
		matchingPoint = b;
		string_matchingpoint = matchingPoint ? "This: #" + index + " @ " + object[index] + string_in_out[index & 1] : "";
		cut_match = (index & 1) == 0;
	}

	/**
	 *
	 */
	public void paint2(Graphics g)
	{
		g.setColor(BackgroundColor);
		g.fillRect(0, 0, 900, 90);
/**
		g.setFont(font);

		g.setColor(cut_top ? Color.green : RedColor);
		g.drawRect(0, Top - 1, width - 1, height + 1);
		g.drawString(string_top, 8, Top - 4);

		g.setColor(cut_bottom ? Color.green : RedColor);
		g.drawRect(Bottom, Top - 1, width - 1, height + 1);
		g.drawString(string_bottom, Bottom + 8, Top - 4);

		if (matchingPoint)
		{
			g.setColor(cut_match ? Color.green : RedColor);
			g.drawString(string_matchingpoint, 8, Bottom - 21);
		}

		g.drawImage(image_top, 0, Top, this);
		g.drawImage(image_bottom, Bottom, Top, this);
**/
	}
}
