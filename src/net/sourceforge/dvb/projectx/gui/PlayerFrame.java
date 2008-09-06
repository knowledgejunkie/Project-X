/*
 * @(#)PlayerFrame
 *
 * Copyright (c) 2008 by dvb.matt, All Rights Reserved.
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

import java.awt.Image;
import java.awt.image.MemoryImageSource;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.KeyEvent;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.UIManager;

import net.sourceforge.dvb.projectx.gui.CommonGui;

import net.sourceforge.dvb.projectx.common.Resource;
import net.sourceforge.dvb.projectx.common.Common;



public class PlayerFrame extends JFrame {

	String title = "Player";

	private Picture picture;
	private int width = 0;
	private int height = 0;
	private int x = 200;
	private int y = 100;

	/**
	 *
	 */
	public PlayerFrame()
	{
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e)
			{
				close();
			}
		});

		buildMenu();

		getContentPane().add("Center", picture = new Picture());

		setTitle(title);
		setLocation(x, y);
		setBounds2(width, height);
		setResizable(false);

		UIManager.addPropertyChangeListener(new UISwitchListener(getRootPane()));
	}

	/**
	 *
	 */
	public void setFrameTitle(String newtitle)
	{
		setTitle(title + " " + newtitle); 
	}

	/**
	 *
	 */
	public void setBounds2(int w, int h)
	{
		setBounds(getX(), getY(), w + 6, h + 45);
		setFrameTitle("    " + w + " * " + h);
	}

	/**
	 *
	 */
	public void repaintPicture()
	{
		repaintPicture(0);
	}

	/**
	 *
	 */
	public void repaintPicture(int hide)
	{
		if (!isVisible())
		{
			if (hide == 1)
				return;

			setVisible(true);
			toFront();
		}

		int w = Common.getMpvDecoderClass().getWidth();
		int h = Common.getMpvDecoderClass().getHeight();

		if (w != width || h != height)
		{
			width = w;
			height = h;
			setBounds2(width, height);
			picture.setSize();
		}

		picture.updateImage();
	}

	/**
	 *
	 */
	public void close()
	{ 
		dispose();
	}

	/**
	 *
	 */
	protected void buildMenu()
	{
		JMenuBar menuBar = new JMenuBar();

		menuBar.add(buildFileMenu());

		setJMenuBar(menuBar);
	}

	/**
	 *
	 */
	protected JMenu buildFileMenu()
	{
		JMenu fileMenu = new JMenu();
		CommonGui.localize(fileMenu, "Common.File");

		JMenuItem close = new JMenuItem();
		CommonGui.localize(close, "Common.Close");
		close.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.ALT_MASK));
		close.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				close();
			}
		});

		fileMenu.add(close);

		return fileMenu;
	}

	/**
	 *
	 */
	public class Picture extends JPanel {

		private MemoryImageSource source;
		private Image image;
		private int[] image_data;


		/**
		 *
		 */
		public Picture()
		{ 
			setBackground(Color.gray);

			setSize();
		}

		/**
		 * performs YUV to RGB conversion
		 */
		private int YUVtoRGB(int YUV)
		{
			int T  = 0xFF;
			int Y  = 0xFF & YUV>>>16;
			int Cb = 0xFF & YUV>>>8;
			int Cr = 0xFF & YUV;
	
			if (Y == 0)
				return 0xFF000000;

			int R = (int)((float)Y +1.402f * (Cr-128));
			int G = (int)((float)Y -0.34414 * (Cb-128) -0.71414 * (Cr-128));
			int B = (int)((float)Y +1.722 * (Cb-128));

			R = R < 0 ? 0 : (R > 0xFF ? 0xFF : R);
			G = G < 0 ? 0 : (G > 0xFF ? 0xFF : G);
			B = B < 0 ? 0 : (B > 0xFF ? 0xFF : B);
	
			return (T<<24 | R<<16 | G<<8 | B);
		}

		/**
		 *
		 */
		public void setSize()
		{
			setPreferredSize(new Dimension(width, height));
			setMinimumSize(new Dimension(width, height));
			setMaximumSize(new Dimension(width, height));

			image_data = new int[width * height];
			source = new MemoryImageSource(width, height, image_data, 0, width);
			source.setAnimated(true);
			image = createImage(source);
		}

		/**
		 *
		 */
		public void updateImage()
		{
			int[] p = Common.getMpvDecoderClass().getPixels();

			for (int i = 0, j = image_data.length; i < j; i++)
				image_data[i] = YUVtoRGB(p[i]);

			source.newPixels();
		}

		/**
		 *
		 */
		public void paint(Graphics g)
		{
			if (image != null)
				g.drawImage(image, 0, 0, this);
		}

	}
}
