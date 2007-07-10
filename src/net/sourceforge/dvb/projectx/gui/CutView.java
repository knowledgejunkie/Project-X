/*
 * @(#)CutView
 *
 * Copyright (c) 2006 by dvb.matt, All Rights Reserved. 
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
import java.awt.GridLayout;

import java.util.Arrays;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

/**
 *
 */
public class CutView extends JPanel {

	private final int Top = 15;
	private final int Bottom = 188;

	private int[] image_data;

	private int width = 160;
	private int height = 90;

	private int sliderPosition = 175;

	private Object index_top = null;
	private Object index_bottom = null;

	private boolean matchingPoint = false;

	private boolean cut_top = true;
	private boolean cut_bottom = false;
	private boolean cut_match = false;
	private boolean action = false;

	private String string_bottom = "Next:";
	private String string_top = "Prev:";
	private String string_matchingpoint = "";

	private String[] string_in_out = { " -IN- ", " -OUT- " };

	private Image image_top;
	private Image image_bottom;

	private MemoryImageSource source_top;
	private MemoryImageSource source_bottom;

	private Color BackgroundColor;
	private Color RedColor;

	private Font font;


	/**
	 *
	 */
	public CutView()
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

		font = new Font("Tahoma", Font.PLAIN, 12);

		setPreferredSize(new Dimension(350, 120));
		setMaximumSize(new Dimension(350, 120));
		setMinimumSize(new Dimension(350, 120));

		setBackground(Color.black);
		setVisible(true);
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
	public Object getTopIndex()
	{
		return index_top;
	}

	/**
	 *
	 */
	public Object getBottomIndex()
	{
		return index_bottom;
	}

	/**
	 *
	 */
	public void setSlider(int val)
	{
		sliderPosition = val;

		repaint();
	}

	/**
	 *
	 */
	public Object getPreviewPosition()
	{
		if (sliderPosition < 168)
			return getTopIndex();

		if (sliderPosition > 182)
			return getBottomIndex();

		return null;
	}

	/**
	 *
	 */
	public int getTransparencyValue()
	{
		double div = 255.0 / 168.0;
		int factor = 0;

		if (sliderPosition < 168)
			factor = -sliderPosition + 168;

		if (sliderPosition > 182)
			factor = sliderPosition - 182;

		div *= factor;
		factor = (int) div;
		factor = factor > 255 ? 255 : factor;

		return factor;
	}

	/**
	 *
	 */
	public void clearViews()
	{
		matchingPoint = false;
		clearView(Top);
		clearView(Bottom);

		CommonGui.getPicturePanel().setMixedPreviewPixel(null, 0);
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
			string_top = "Prev: " + (object != null ? "#" + (index + 1) + " - " + object[index] + string_in_out[index & 1] : "= Collection Begin");
			cut_top = object != null ? (index & 1) == 0 : (!matchingPoint ? !cut_bottom : false);
			index_top = object != null ? object[index] : null;
		}

		else
		{
			source_bottom.newPixels();
			string_bottom = "Next: " + (object != null ? "#" + (index + 1) + " - " + object[index] + string_in_out[index & 1] : "= Collection End");
			cut_bottom = object != null ? (index & 1) == 0 : false;
			index_bottom = object != null ? object[index] : null;
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

		scaleCutImage(data);

		updateView(position, object, index);
	}

	/**
	 * create new smaller cutimage pixel data
	 */
	private void scaleCutImage(int[] data)
	{
		int source_width = 512;
		int source_height = 288;

		float Y = 0;
		float X = 0;
		float decimate_height = (float)source_height / height;
		float decimate_width = (float)source_width / width;

		for (int y = 0, tmp1, tmp2; Y < source_height && y < height; Y += decimate_height, y++, X = 0)
		{
			tmp1 = y * width;
			tmp2 = (int)Y * source_width;

			for (int x = 0; X < source_width && x < width; X += decimate_width, x++)
				image_data[x + tmp1] = data[(int)X + tmp2];
		}
	}

	/**
	 *
	 */
	public void setMatchingPoint(boolean b, Object[] object, int index)
	{
		matchingPoint = b;
		string_matchingpoint = matchingPoint ? "This: #" + (index + 1) + " @ " + object[index] + string_in_out[index & 1] : "";
		cut_match = (index & 1) == 0;
	}

	/**
	 *
	 */
	public void paint(Graphics g)
	{
		g.setColor(BackgroundColor);
		g.fillRect(0, 0, 600, 120);

		g.setFont(font);

		g.setColor(cut_top ? Color.green : RedColor);
		g.drawRect(2, Top - 1, width - 1, height + 1);
		g.drawString(string_top, 2, Top - 4);

		g.setColor(cut_bottom ? Color.green : RedColor);
	//	g.drawRect(0, Bottom - 1, width - 1, height + 1);
	//	g.drawString(string_bottom, 8, Bottom - 4);
		g.drawRect(Bottom, Top - 1, width - 1, height + 1);
		g.drawString(string_bottom, Bottom + 2, Top - 4);

		if (matchingPoint)
		{
			g.setColor(cut_match ? Color.green : RedColor);
			g.drawString(string_matchingpoint, 2, Bottom - 21);
		}

		g.drawImage(image_top, 2, Top, this);
	//	g.drawImage(image_bottom, 0, Bottom, this);
		g.drawImage(image_bottom, Bottom, Top, this);

		paintSlider(g);
	}

	/**
	 *
	 */
	public void paintSlider(Graphics g)
	{
		int i = 6;

		g.setColor(cut_top ? Color.green : RedColor);
		g.fillRect(i, 110, 40, 6);
		g.fillRect(i + 44, 110, 32, 6);
		g.fillRect(i + 80, 110, 24, 6);
		g.fillRect(i + 108, 110, 16, 6);
		g.fillRect(i + 128, 110, 10, 6);
		g.fillRect(i + 142, 110, 6, 6);
		g.fillRect(i + 152, 110, 4, 6);

		g.setColor(cut_bottom ? Color.green : RedColor);
		g.fillRect(Bottom, 110, 4, 6);
		g.fillRect(Bottom + 8, 110, 6, 6);
		g.fillRect(Bottom + 18, 110, 10, 6);
		g.fillRect(Bottom + 32, 110, 16, 6);
		g.fillRect(Bottom + 52, 110, 24, 6);
		g.fillRect(Bottom + 80, 110, 32, 6);
		g.fillRect(Bottom + 116, 110, 40, 6);

		g.setColor(Color.yellow);
		g.drawRect(sliderPosition - 7, 108, 14, 10);
	}

}
