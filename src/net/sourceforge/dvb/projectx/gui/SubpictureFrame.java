/*
 * @(#)SubpictureFrame
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


package net.sourceforge.dvb.projectx.gui;

import java.awt.Image;
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


public class SubpictureFrame extends JFrame {

	String title = Resource.getString("subpicture.title");

	private Picture picture;

	/**
	 *
	 */
	public SubpictureFrame()
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
		setBounds(200, 100, 726, 621);
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
	public void repaintSubpicture()
	{
		picture.repaint();
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

		/**
		 *
		 */
		public Picture()
		{ 
			setBackground(Color.gray);
			setPreferredSize(new Dimension(720, 576));
			setMinimumSize(new Dimension(720, 576));
			setMaximumSize(new Dimension(720, 576));
		}

		/**
		 *
		 */
		public void paint(Graphics g)
		{
			Image image = Common.getSubpictureClass().getImage();

			if (image != null)
				g.drawImage(image, 0, 0, this);
		}

	}
}
