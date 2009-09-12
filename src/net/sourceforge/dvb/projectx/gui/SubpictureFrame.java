/*
 * @(#)SubpictureFrame
 *
 * Copyright (c) 2005-2009 by dvb.matt, All Rights Reserved.
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

import java.awt.Font;
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

import java.io.File;
import java.util.ArrayList;

import javax.swing.event.*;
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
import javax.swing.JSlider;
import javax.swing.JLabel;
import javax.swing.JCheckBoxMenuItem;

import net.sourceforge.dvb.projectx.gui.CommonGui;

import net.sourceforge.dvb.projectx.common.Resource;
import net.sourceforge.dvb.projectx.common.Common;
import net.sourceforge.dvb.projectx.xinput.XInputFile;
import net.sourceforge.dvb.projectx.parser.CommonParsing;


public class SubpictureFrame extends JFrame {

	String title = Resource.getString("subpicture.title");
	String info = "";

	private Picture picture;

	private JSlider slider;

	private ArrayList picture_indices = null;
	private int picture_index = 0;
	private byte[] picture_data = null;
	private int[] color_table = null;
	private int PreviewFlags = 0;

	private int horizontal_offset = 0;
	private int vertical_offset = 0;

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

		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());

		panel.add(buildSizePanel(), BorderLayout.NORTH);
		panel.add(picture = new Picture(), BorderLayout.CENTER);
		panel.add(buildSliderPanel(), BorderLayout.SOUTH);

		getContentPane().add("Center", panel);

		setTitle(title);
		//setBounds(200, 0, 726, 726);
		setBounds(200, 0, 726, 750);
		setResizable(false);

		UIManager.addPropertyChangeListener(new UISwitchListener(getRootPane()));
	}

	/**
	 *
	 */
	public void close()
	{ 
		resetPreview();
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

		final JCheckBoxMenuItem background = new JCheckBoxMenuItem("use Preview Picture as Background");
		background.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, ActionEvent.CTRL_MASK));
		background.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				PreviewFlags = background.getState() ? PreviewFlags | 1 : PreviewFlags & ~1;
				getPictureData(slider.getValue());
			}
		});

		fileMenu.add(background);

		final JCheckBoxMenuItem letterbox = new JCheckBoxMenuItem("use Letterbox");
		letterbox.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, ActionEvent.CTRL_MASK));
		letterbox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				PreviewFlags = letterbox.getState() ? PreviewFlags | 2 : PreviewFlags & ~2;
				getPictureData(slider.getValue());
			}
		});

		fileMenu.add(letterbox);

		final JCheckBoxMenuItem areabox = new JCheckBoxMenuItem("don't show Multiple Areas Boundaries");
		areabox.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
		areabox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				PreviewFlags = areabox.getState() ? PreviewFlags | 4 : PreviewFlags & ~4;
				getPictureData(slider.getValue());
			}
		});

		fileMenu.add(areabox);

		final JCheckBoxMenuItem applyarea = new JCheckBoxMenuItem("don't apply Multiple Areas Replacements");
		applyarea.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, ActionEvent.CTRL_MASK));
		applyarea.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				PreviewFlags = applyarea.getState() ? PreviewFlags | 8 : PreviewFlags & ~8;
				getPictureData(slider.getValue());
			}
		});

		fileMenu.add(applyarea);

		final JCheckBoxMenuItem allopaque = new JCheckBoxMenuItem("paint all as Opaque");
		allopaque.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, ActionEvent.CTRL_MASK));
		allopaque.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				PreviewFlags = allopaque.getState() ? PreviewFlags | 0x10 : PreviewFlags & ~0x10;
				getPictureData(slider.getValue());
			}
		});

		fileMenu.add(allopaque);

		fileMenu.addSeparator();

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
	protected JPanel buildSizePanel()
	{
		JPanel panel = new JPanel();
		//panel.setLayout(new BoxLayout());

		final JSlider slider_h = new JSlider();
		slider_h.setMajorTickSpacing(32);
		slider_h.setSnapToTicks(true);
		slider_h.setMaximum(1920);
		slider_h.setValue(0);

		slider_h.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e)
			{
				horizontal_offset = slider_h.getValue();
				repaintSubpicture();
			}
		});

		panel.add(new JLabel("Preview X Offset: "));
		panel.add(slider_h);

		final JSlider slider_v = new JSlider();
		slider_v.setMajorTickSpacing(16);
		slider_v.setSnapToTicks(true);
		slider_v.setMaximum(1088);
		slider_v.setValue(0);

		slider_v.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e)
			{
				vertical_offset = slider_v.getValue();
				repaintSubpicture();
			}
		});

		panel.add(new JLabel("Preview Y Offset: "));
		panel.add(slider_v);

		return panel;
	}

	/**
	 *
	 */
	protected JPanel buildSliderPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());

		slider = new JSlider();
		slider.setMajorTickSpacing(1);
		slider.setPaintTicks(true);
		slider.setSnapToTicks(true);
		slider.setMaximum(1);
		slider.setValue(0);

		slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e)
			{
				getPictureData(slider.getValue());
			}
		});

		panel.add(slider);

		return panel;
	}

	/**
	 *
	 */
	public void setFrameTitle(String newtitle)
	{
		setTitle(title + " " + newtitle); 
		info = newtitle;
	}

	/**
	 *
	 */
	public void loadPreview(XInputFile xif)
	{
		scanIFO(xif.toString());

		if (scanData(xif))
			show();
	}

	/**
	 *
	 */
	public void resetPreview()
	{
		picture_indices = null;
		picture_index = 0;
		picture_data = null;
		color_table = null;
		info = "";
		slider.setMaximum(1);
		slider.setValue(0);
	}

	/**
	 * read colors from ifo (pjx auto generated)
	 */
	private void scanIFO(String ifoname)
	{
		try {

			String nifoname = ifoname + ".IFO";

			File f = new File(nifoname);

			if (!f.exists())
			{
				f = new File(ifoname.substring(0, ifoname.lastIndexOf(".")) + ".IFO");

				if (!f.exists())
				{
					color_table = null;
					return;
				}
			}

			XInputFile xif = new XInputFile(f);

			byte[] data = new byte[64];
			color_table = new int[16];

			xif.randomAccessSingleRead(data, 0x10B4); //read 16x 4bytes from pos 0x10B4

			for (int i = 0, j = color_table.length; i < j; i++)
				color_table[i] = YUVtoRGB(CommonParsing.getIntValue(data, i * 4, 4, !CommonParsing.BYTEREORDERING));

		} catch (Exception e) {

			color_table = null;

			Common.setExceptionMessage(e);
		}
	}

	/**
	 * convert colors from ifo (pjx auto generated)
	 */
	private int YUVtoRGB(int values)
	{
		int Y = 0xFF & values>>16;
		int Cr = 0xFF & values>>8;
		int Cb = 0xFF & values;

		if (Y == 0)
			return 0;

		int R = (int)((float)Y +1.402f * (Cr-128));
		int G = (int)((float)Y -0.34414 * (Cb-128) -0.71414 * (Cr-128));
		int B = (int)((float)Y +1.722 * (Cb-128));
		R = R < 0 ? 0 : (R > 0xFF ? 0xFF : R);
		G = G < 0 ? 0 : (G > 0xFF ? 0xFF : G);
		B = B < 0 ? 0 : (B > 0xFF ? 0xFF : B);
		int T = 0xFF;

		return (T<<24 | R<<16 | G<<8 | B);
	}

	/**
	 *
	 */
	private boolean scanData(XInputFile xif)
	{
		boolean b = false;

		picture_indices = new ArrayList();

		try {

			if (!xif.exists())
				return b;

			picture_data = new byte[(int) xif.length()];
			info = xif.toString();

			xif.randomAccessSingleRead(picture_data, 0); // read all

			long pts = 0;
			for (int i = 0; i < picture_data.length; i++)
			{
				if (picture_data[i] != 0x53 || picture_data[i + 1] != 0x50) // header
					continue;

				pts = CommonParsing.readPTS(picture_data, i + 2, 8, CommonParsing.BYTEREORDERING, false);

				picture_indices.add(new Long[] { new Long(i), new Long(CommonParsing.readPTS(picture_data, i + 2, 8, CommonParsing.BYTEREORDERING, false) / 90), null } );

				i += 8;
			}

			slider.setMaximum(picture_indices.size() - 1);

		} catch (Exception e) {

			slider.setMaximum(1);

			info = "file read error!";
			//Common.setExceptionMessage(e);
			return b;
		}

		slider.setValue(0);

		return !b;
	}

	/**
	 *
	 */
	private void getPictureData(int index)
	{
		picture_index = index;

		if (picture_index < 0 || picture_indices == null || picture_index >= picture_indices.size())
			return;

		Long[] values = (Long[]) picture_indices.get(picture_index);
		int pos = values[0].intValue(); //start pos


		int length = picture_data.length - pos;

		if (picture_index + 1 < picture_indices.size())
		{
			Long[] nvalues = (Long[]) picture_indices.get(picture_index + 1);
			int npos = nvalues[0].intValue(); //start pos

			length = npos - pos;
		}

		Common.getSubpictureClass().setColorTable(color_table);

		byte[] array = new byte[length];
		System.arraycopy(picture_data, pos, array, 0, length);

		PreviewFlags &= 0xFF; //remove subpic preview resolution
		PreviewFlags |= (0xFFF & picture.getWidth()) << 20; //add horizontal resolution
		PreviewFlags |= (0xFFF & picture.getHeight()) << 8; //add vertical resolution

		int duration = Common.getSubpictureClass().decode_picture(array, 10, true, new String[2], (PreviewFlags & 1) == 1 ? CommonGui.getPicturePanel().getPreviewImage() : null, PreviewFlags);

		values[2] = new Long(duration / 90);

		repaintSubpicture();
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
	public class Picture extends JPanel {

		private Font font;
		private int w = 720;
		private int h = 576;

		/**
		 *
		 */
		public Picture()
		{ 
			font = new Font("Tahoma", Font.PLAIN, 14);

			setBackground(Color.gray);
			setPreferredSize(new Dimension(w, 704));
			setMinimumSize(new Dimension(w, 704));
			setMaximumSize(new Dimension(w, 704));
		}

		/**
		 *
		 */
		public void paint(Graphics g)
		{
			paintPicture(g);
			paintInfoBackground(g);
			paintInfoField(g);
			paintPreviewInfo(g);
		}

		/**
		 *
		 */
		private void paintPicture(Graphics g)
		{
			Image image = Common.getSubpictureClass().getImage();  // link to image, original size

			if (image != null)
				g.drawImage(image, -horizontal_offset, -vertical_offset, this); //move to fit into preview
			//	g.drawImage(image, 0, 0, this);  // original
		}

		/**
		 *
		 */
		private void paintInfoBackground(Graphics g)
		{
			g.setColor(Color.black);
			g.fillRect(0, h, w, 140);

			//divide
			g.setColor(Color.white);
			g.fillRect(0, h, w, 2);
		}

		/**
		 *
		 */
		private void paintInfoField(Graphics g)
		{
			g.setColor(Color.white);
			g.setFont(font);
			g.drawString(info, 4, 608);
		}

		/**
		 *
		 */
		private void paintPreviewInfo(Graphics g)
		{
			if (picture_indices == null)
				return;

			if (picture_indices.size() == 0)
				return;

			Long[] values = (Long[]) picture_indices.get(picture_index);

			String str1 = "Pos: " + values[0]
						+ " / Picture: " + (picture_index + 1) + " of " + picture_indices.size()
						+ " / " + Common.getSubpictureClass().isForced_Msg(1);

			g.setColor(Color.white);
			g.setFont(font);
			g.drawString(str1, 4, 626);

			String str2 = "PTS In " + Common.formatTime_1(values[1].longValue())
						+ " Duration " + Common.formatTime_1(values[2].longValue())
						+ " PTS Out " + Common.formatTime_1(values[1].longValue() + values[2].longValue());

			if (picture_index + 1 < picture_indices.size())
			{
				Long[] nvalues = (Long[]) picture_indices.get(picture_index + 1);
				long diff = nvalues[1].longValue() - (values[1].longValue() + values[2].longValue());

				str2 += " Next In " + Common.formatTime_1(nvalues[1].longValue());

				if (diff < 0)
					str2 += " Overlap " + Common.formatTime_1(Math.abs(diff));
				else
					str2 += " Gap " + Common.formatTime_1(diff);
			}

			else
				str2 += " Next File End ";

			g.drawString(str2, 4, 644);

			paintColorIndex(g);
		}

		/**
		 *
		 */
		private void paintColorIndex(Graphics g)
		{
			g.setFont(font);

			int[] colors = Common.getSubpictureClass().getColorTable(0);

			for (int i = 0, x = 4; i < 16; i++, x += 44)
			{
				g.setColor(new Color(colors[i]));
				g.fillRect(x + 20, 580, 12, 12);

				g.setColor(Color.white);
				g.drawString(Integer.toHexString(i).toUpperCase(), x, 592);
				g.drawRect(x + 20, 580, 12, 12);
			}
		}
	}
}
