/*
 * @(#)TeletextPageMatrix.java - graphical representation of found TTX pages
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

package net.sourceforge.dvb.projectx.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;

import net.sourceforge.dvb.projectx.common.Resource;
import net.sourceforge.dvb.projectx.gui.UISwitchListener;

//DM17042004 081.7 int02 introduced
public class TeletextPageMatrix extends JFrame {

	public Picture picture;

	private String title = Resource.getString("ttpagematrix.title");

	private int w = 310;
	private int h = 410;

	public TeletextPageMatrix()
	{
		addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				close();
			}
		});

		UIManager.addPropertyChangeListener(new UISwitchListener((JComponent)getRootPane()));

		picture = new Picture();
		getContentPane().add("Center", picture);

		setTitle(title);
		setSize(new Dimension(w, h));
		setLocation(100, 100);
		setResizable(false);
	}

	public void close()
	{ 
		dispose();
	}


	public class Picture extends JPanel
	{
		private Color[] color = {
			Color.red,
			Color.yellow,
			Color.green,
			new Color(255, 144, 0), //orange
			new Color(144, 144, 255), //light blue
			Color.cyan,
			Color.magenta,
			Color.white,
			Color.black,
			new Color(0, 200, 0) // dark green
		};
		private BufferedImage bimg;
		private Graphics2D big;
		private int x = 0, y = 0;

		public Picture()
		{ 
			bimg = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
			big = bimg.createGraphics();
			big.setFont(new Font("Sans Serif", Font.PLAIN, 12));

			setBackground(color[8]);
			setToolTipText(Resource.getString("ttpagematrix.tip"));
			init();
		}

		public void paint(Graphics g)
		{
			if (big == null) 
				return;

			g.drawImage(bimg, 0, 0, this);
		}

		public void init()
		{
			init("");
		}

		public void init(String file)
		{
			x = 0;
			y = 0;

			big.setColor(color[8]);
			big.fillRect(0, 0, w, h);

			big.setColor(color[7]);
			big.drawString(Resource.getString("ttpagematrix.file") + ": " + file, x + 10, y + 14);

			y += 16;

			big.drawString(Resource.getString("ttpagematrix.composition1") + ": ", x + 10, y + 14);
			big.drawString(Resource.getString("ttpagematrix.composition2"), x + 20, y + 30);

			big.drawString(Resource.getString("ttpagematrix.composition3") + ":", x + 10, y + 46);

			for (int a=0; a<8; a++)
				big.drawString(" = " + (a+1), x + 28 + (a<<5), y + 62);

			for (int a=0; a<8; a++)
			{
				big.setColor(color[a]);
				big.fillRect(x + 22 + (a<<5), y + 52, 3, 12);
			}

			big.setColor(color[7]);
			big.drawString("X", x + 40, y + 80);
			big.drawString("Y", x + 8, y + 113);

			x += 20;
			y += 84;

			for (int a=0; a<16; a++)
			{
				big.drawString(Integer.toHexString(a).toUpperCase(), x + 20 + (a<<4), y + 12);
				big.drawString(Integer.toHexString(a).toUpperCase(), x + 5, y + 29 + (a<<4));
			}

			big.setColor(color[9]);
			for (int a=1; a<18; a++)
			{
				big.drawLine( x, y + (a<<4), x + 272, y + (a<<4));
				big.drawLine( x + (a<<4), y, x + (a<<4), y + 272);
			}

			x += 18;
			y += 19;

			repaint();
		}

		public void update(String page)
		{
			int pagenumber = Integer.parseInt(page, 16);
			int pm = (0xF & pagenumber>>>8) - 1;

			if (pm < 0)
				return;

			int px = x + (0xF0 & pagenumber) + ((3 & pm)<<2);
			int py = y + (0xF0 & pagenumber<<4) + ((4 & pm) != 0 ? 6 : 0);

			if ((0xFFFFFF & bimg.getRGB(px, py)) != 0)
				return;

			big.setColor(color[pm]);
			big.fillRect(px, py, 2, 5);

			if (picture.isVisible())
				repaint();
		}
	}
}
