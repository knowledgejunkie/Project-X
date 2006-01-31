/*
 * @(#)PicturePanel
 * 
 * Copyright (c) 2003-2006 by dvb.matt, All Rights Reserved. 
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
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Font;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.awt.image.MemoryImageSource;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.UIManager;

import net.sourceforge.dvb.projectx.common.Resource;
import net.sourceforge.dvb.projectx.common.Common;
import net.sourceforge.dvb.projectx.common.Keys;

import net.sourceforge.dvb.projectx.gui.X_JFileChooser;

import net.sourceforge.dvb.projectx.parser.CommonParsing;

import net.sourceforge.dvb.projectx.video.PreviewObject;
import net.sourceforge.dvb.projectx.video.Video;

import net.sourceforge.dvb.projectx.xinput.StreamInfo;

public class PicturePanel extends JPanel {

	private Image StreamTypeImage = Resource.loadImage("_container.gif");
	private Image VideoImage = Resource.loadImage("_video.gif");
	private Image AudioImage = Resource.loadImage("_audio.gif");
	private Image TeletextImage = Resource.loadImage("_teletext.gif");
	private Image SubtitleImage = Resource.loadImage("_subtitle.gif");
	private Image PlaytimeImage = Resource.loadImage("_playtime.gif");
	private Image PalPlusImage = Resource.loadImage("_ppl.gif");
	private Image SubpictureImage;
	private Image image;

	private MemoryImageSource source;

	private boolean showFileInfo = false;
	private boolean isSubpictureAvailable = false;
	private boolean isOSDInfoAvailable = false;
	private boolean isOSDErrorInfo = false;
	private boolean PLAY = true;

	private StreamInfo streamInfo = null;

	private Font font_1;
	private Font font_2;
	private Font font_3;

	private int ErrorFlag = 0;
	private int bmpCount = 0;
	private int collection_number = -1;

	private final String tooltip1 = Resource.getString("mpvdecoder.tip1");

	private long cutfiles_length = 0;
	private long[] cutfiles_points = null;
	private long chapter_length = 0;
	private long[] chapter_points = null;

	private Object[] OSDInfo;

	private JPopupMenu popup;

	private Clock clock;

	/**
	 * class to control short OSD fadings
	 */
	private class Clock implements Runnable {

		private Thread clockThread = null;

		private int sleepAmount = 3000;

		public void start()
		{
			start(3000);
		}

		public void start(int value)
		{
			if (clockThread == null)
			{
				clockThread = new Thread(this, "Clock_4");
				clockThread.setPriority(Thread.MIN_PRIORITY);

				sleepAmount = value;

				clockThread.start();
			}
		}

		public void run()
		{
			Thread myThread = Thread.currentThread();

			while (clockThread == myThread)
			{
				try {
					Thread.sleep(sleepAmount); // time on screen
				} catch (InterruptedException e) {}

				if (update())
					repaint();

				stop();
			}
		}

		private boolean update()
		{
			boolean b = false;

			if (collection_number >= 0)
			{
				collection_number = -1;
				b = true;
			}

			if (showFileInfo)
			{
				showFileInfo = false;
				b = true;
			}

			if (isOSDInfoAvailable)
			{
				isOSDInfoAvailable = false;
				b = true;
			}

			return b;
		}

		public void stop()
		{
			clockThread = null;
		}
	}


	/**
	 *
	 */
	public PicturePanel()
	{
		source = new MemoryImageSource(512, 288, Common.getMpvDecoderClass().getPreviewPixel(), 0, 512);
		source.setAnimated(true);
		image = createImage(source);

		font_1 = new Font("Tahoma", Font.PLAIN, 12);
		font_2 = new Font("Tahoma", Font.BOLD, 12);
		font_3 = new Font("Tahoma", Font.BOLD, 24);

		setBackground(Color.black);
		setVisible(true);

		setToolTipText(tooltip1); // <- VORSCHLAG 1 Tooltip! 
		setSize(512, 346);

		buildPopupMenu();

		addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e)
			{
				if (e.getClickCount() >= 1 && (showFileInfo || isOSDInfoAvailable))
				{
					showFileInfo = false;
					isOSDInfoAvailable = false;
					repaint();
				}

	//			if (e.getClickCount() > 1)
	//				saveBMP(Common.getMpvDecoderClass().getPixels(), Common.getMpvDecoderClass().getWidth(), Common.getMpvDecoderClass().getHeight(), Common.getMpvDecoderClass().getAspectRatio());

				if (e.getClickCount() >= 1 && e.getModifiers() == MouseEvent.BUTTON3_MASK)
					popup.show(getParent(), e.getX(), e.getY());
			}
		});

		clock = new Clock();
	}

	/**
	 *
	 */
	protected void buildPopupMenu()
	{
		ActionListener al = new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				String actName = e.getActionCommand();

				if (actName.equals("save_1"))
					saveBMP(Common.getMpvDecoderClass().getPixels(), Common.getMpvDecoderClass().getWidth(), Common.getMpvDecoderClass().getHeight(), 0, false);

				else if (actName.equals("save_2"))
					saveBMP(Common.getMpvDecoderClass().getPixels(), Common.getMpvDecoderClass().getWidth(), Common.getMpvDecoderClass().getHeight(), Common.getMpvDecoderClass().getAspectRatio(), true);
			}
		};

		popup = new JPopupMenu("save");

		JMenuItem menuitem_1 = popup.add(Resource.getString("PreviewPanel.saveCurrentPicture"));
		menuitem_1.setActionCommand("save_1");

		JMenuItem menuitem_2 = popup.add(Resource.getString("PreviewPanel.saveCurrentPictureDAR"));
		menuitem_2.setActionCommand("save_2");

		popup.pack();

		UIManager.addPropertyChangeListener(new UISwitchListener(popup));

		menuitem_1.addActionListener(al);
		menuitem_2.addActionListener(al);
	}

	/**
	 * holds special messages
	 */
	public void startClock()
	{
		clock.start();
	}

	/**
	 * holds special messages
	 */
	public void startClock(int value)
	{
		clock.start(value);
	}

	/**
	 * updates the preview graphic
	 */
	public void paint(Graphics g)
	{
		g.setColor(Color.black);
		g.fillRect(0, 0, 600, 600);

		g.setColor(new Color(0, 35, 110));
		g.fillRect(0, 290, 514, 340);

		paintOutline(g);

		g.drawImage(image, 2, 2, this);

		g.setFont(font_1);

		g.setColor(Color.white);
		g.drawString(Common.getMpvDecoderClass().getInfo_1(), 36, 303);
		g.drawString(Common.getMpvDecoderClass().getInfo_2(), 36, 317);

		paintWSSInfo(g);
		paintErrorInfo(g);
		paintPlayInfo(g);

		g.setFont(font_2);

		paintCutInfo(g);
		paintChapterInfo(g);
		paintSubpicture(g);
		paintOSDInfo(g);
		paintFileInfo(g);

		paintCollectionNumber(g);
	}

	/**
	 * paint 
	 */
	private void paintOutline(Graphics g)
	{
		Color line_1 = new Color(191, 191, 191);
		Color line_2 = new Color(255, 255, 255);
		Color line_3 = new Color(151, 151, 151);

		g.setColor(line_3);
		g.drawLine(1, 1, 514, 1);
		g.drawLine(1, 1, 1, 400);

		g.setColor(line_1);
		g.drawLine(2, 290, 514, 290);
		g.drawLine(514, 2, 514, 400);

		g.setColor(line_2);
		g.drawLine(0, 0, 515, 0);
		g.drawLine(0, 0, 0, 400);
		g.drawLine(0, 291, 515, 291);
		g.drawLine(515, 0, 515, 400);
	}

	/**
	 * paint 
	 */
	private void paintCollectionNumber(Graphics g)
	{
		if (collection_number < 0)
			return;

		g.setFont(font_2);
		g.setColor(Color.green);
		g.drawString("Collection", 454, 16);

		g.setFont(font_3);
		g.setColor(Color.green);
		g.drawString(String.valueOf(collection_number), 474, 38);
	}

	/**
	 * paint wss info
	 */
	private void paintWSSInfo(Graphics g)
	{
		String str;

		if ((str = Common.getMpvDecoderClass().getWSSInfo()) == null)
		{
			setToolTipText(tooltip1);
			return;
		}

		g.setFont(font_2);
		g.setColor(Color.green);
		g.drawString("WSS present", 10, 16);
		g.drawString(Common.getMpvDecoderClass().getWSSFormatInfo(), 10, 30);

		if (Common.getMpvDecoderClass().getPalPlusInfo())
			g.drawImage(PalPlusImage, 8, 34, this);

		setToolTipText("<html>" + tooltip1 + "<p><p>" + str + "</html>");
	}

	/**
	 * paint play info
	 */
	private void paintPlayInfo(Graphics g)
	{
		int x[] = { 10, 10, 30 };
		int y[] = { 294, 314, 304 };

		if (PLAY)
		{
			g.setColor(Color.green);
			g.fillPolygon(x, y, 3);
		}

		else
		{
			g.setColor(Color.red);
			g.fillRect(10, 294, 20, 20);
		}
	}

	/**
	 * paint error info
	 */
	private void paintErrorInfo(Graphics g)
	{
		ErrorFlag = Common.getMpvDecoderClass().getErrors();

		if ((ErrorFlag & 1) != 0)
		{
			g.setColor(Color.white);
			g.fill3DRect(150, 120, 200, 20, true);
			g.setColor(Color.red);
			g.drawString("error while decoding frame", 160, 133);
		}

		if ((ErrorFlag & 2) != 0)
		{
			g.setColor(Color.white);
			g.fill3DRect(150, 135, 200, 20, true);
			g.setColor(Color.red);
			g.drawString("cannot find sequence header", 160, 148);
		}
	}

	/**
	 * paint cut info
	 */
	private void paintCutInfo(Graphics g)
	{
		if (cutfiles_length <= 0)
			return;

		int x1 = 10, y1 = 327, w1 = 492, h1 = 6;

		g.setColor(new Color(0, 200, 0));
		g.fillRect(x1, y1, w1, h1);
		g.setColor(Color.white);
		g.drawRect(x1 -2, y1 -2, w1 +3, h1 +3);

		/**
		 * paint cut markers
		 */
		if (cutfiles_points != null && cutfiles_points.length > 0)
		{
			int p0 = 0, p1 = 0;

			for (int i = 0; i < cutfiles_points.length; i++)
			{
				if (cutfiles_points[i] > cutfiles_length)
					break; 

				p0 = i == 0 ? 0 : (int)(cutfiles_points[i - 1] * w1 / cutfiles_length);
				p1 = (int)(cutfiles_points[i] * w1 / cutfiles_length);

				if (i % 2 == 0)
				{
					g.setColor(new Color(150, 0, 0));
					g.fillRect(x1 + p0, y1, p1 - p0, h1);
				}

				g.setColor(new Color(200, 100, 200));
				g.fillRect(x1 + p1 - 1, y1 - 4, 2, h1 + 8);

				int[] x = { x1 + p1 - 1, x1 + p1 - 5, x1 + p1 + 5 };
				int[] y = { y1 - 3, y1 - 3 - 5, y1 - 3 - 5 };

				g.fillPolygon(x, y, 3);
			}

			if ((cutfiles_points.length & 1) == 0)
			{
				p0 = (int)(cutfiles_points[cutfiles_points.length - 1] * w1 / cutfiles_length);

				g.setColor(new Color(150, 0, 0));
				g.fillRect(x1 + p0, y1, w1 - p0, h1);
			}
		}
	}

	/**
	 * paint chapter info
	 */
	private void paintChapterInfo(Graphics g)
	{
		if (chapter_length <= 0)
			return;

		int x1 = 10, y1 = 327, w1 = 492, h1 = 6;

		/**
		 * paint chapter markers
		 */
		if (chapter_points != null && chapter_points.length > 0)
		{
			int p0 = 0, p1 = 0;

			for (int i = 0; i < chapter_points.length; i++)
			{
				if (chapter_points[i] > chapter_length)
					break; 

				p0 = i == 0 ? 0 : (int)(chapter_points[i - 1] * w1 / chapter_length);
				p1 = (int)(chapter_points[i] * w1 / chapter_length);

				g.setColor(new Color(195, 205, 255));
				g.fillRect(x1 + p1 - 1, y1 - 4, 2, h1 + 8);

				int[] x = { x1 + p1 - 1, x1 + p1 - 5, x1 + p1 + 5 };
				int[] y = { y1 + h1 + 3, y1 + h1 + 3 + 5, y1 +h1 + 3 + 5 };

				g.fillPolygon(x, y, 3);
			}
		}
	}

	/**
	 * paint 
	 */
	private void paintOSDBg(Graphics g, int x3, int y3, int w3, int h3)
	{
		Color blue_1 = new Color(150, 150, 255, 210);
		Color blue_2 = new Color(50, 20, 225, 210);
		Color red_1 = new Color(255, 150, 150, 210);
		Color red_2 = new Color(225, 20, 20, 210);

		g.setColor(isOSDErrorInfo ? red_1 : blue_1);
		g.fillRect(x3, y3 + 20, w3, 220);

		g.setColor(isOSDErrorInfo ? red_2 : blue_2);
		g.fillRect(x3, y3, w3, 20);
		g.fillRect(x3, y3 + h3, w3, 34);

		g.setColor(Color.white);
		g.drawLine(x3, y3 + 20, x3 + w3 - 1, y3 + 20);
		g.drawLine(x3, y3 + 240, x3 + w3 - 1, y3 + 240);
	}

	/**
	 * paint prescan file info
	 */
	private void paintOSDInfo(Graphics g)
	{
		if (!isOSDInfoAvailable || OSDInfo == null || OSDInfo.length == 0)
			return;

		int x3 = 8; //15;
		int y3 = 8;
		int w3 = 496; //482;
		int h3 = 240;
		int yOffset = 0;

		paintOSDBg(g, x3, y3, w3, h3);

		g.setColor(Color.white);
		g.drawString(OSDInfo[0].toString(), x3 + 10, y3 + 14);
		g.drawString(OSDInfo[1].toString(), x3 + 10, y3 + h3 + 16);

		yOffset = y3 + 24;

		for (int i = 2; i < OSDInfo.length; i++)
			yOffset = paintSubInfo(g, OSDInfo[i].toString(), null, x3, yOffset);
	}


	/**
	 * paint prescan file info
	 */
	private void paintFileInfo(Graphics g)
	{
		if (!showFileInfo)
			return;

		int x3 = 8; //15;
		int y3 = 8;
		int w3 = 496; //482;
		int h3 = 240;
		int yOffset = 0;
		Object[] obj;

		paintOSDBg(g, x3, y3, w3, h3);

		g.setColor(Color.white);
		g.drawString(streamInfo.getFileSourceAndName(), x3 + 6, y3 + 14);
		g.drawString(streamInfo.getFileDate(), x3 + 6, y3 + h3 + 16);
		g.drawString(streamInfo.getFileSize(), x3 + 6, y3 + h3 + 30);

		yOffset = y3 + 24;

		yOffset = paintSubInfo(g, streamInfo.getFileType(), StreamTypeImage, x3, yOffset);

		obj = streamInfo.getVideoStreams();
		yOffset = paintSubInfo(g, obj, VideoImage, x3, yOffset);

		obj = streamInfo.getAudioStreams();
		yOffset = paintSubInfo(g, obj, AudioImage, x3, yOffset);

		obj = streamInfo.getTeletextStreams();
		yOffset = paintSubInfo(g, obj, TeletextImage, x3, yOffset);

		obj = streamInfo.getSubpictureStreams();
		yOffset = paintSubInfo(g, obj, SubtitleImage, x3, yOffset);

		yOffset = paintSubInfo(g, streamInfo.getPlaytime(), PlaytimeImage, x3, yOffset);
	}

	/**
	 * line height = 16
	 */
	private int paintSubInfo(Graphics g, String str, Image icon, int x, int y)
	{
		if (str != null && str.length() > 0)
		{
			if (y > 230)
			{
				g.fillPolygon(new int[] { x + 3, x + 17, x + 9 }, new int[] { y, y, y + 7 }, 3);
				g.drawString("...", x + 25, y + 6);
				return y;
			}

			int i = str.indexOf("\t");
			int j = icon != null ? 25 : 10;

			if (icon != null)
				g.drawImage(icon, x + 2, y, this);

			if (i >= 0)
			{
				g.drawString(str.substring(i + 1), x + j + 120, y + 12);
				g.drawString(str.substring(0, i), x + j, y + 12);
			}
			else
				g.drawString(str, x + j, y + 12);

			y += 16;
		}

		return y;
	}

	/**
	 * 
	 */
	private int paintSubInfo(Graphics g, Object[] obj, Image icon, int x, int y)
	{
		if (obj != null && obj.length > 0)
		{
			for (int i = 0; i < obj.length; i++)
				y = paintSubInfo(g, "" + (i + 1) + ".  " + obj[i].toString(), icon, x, y);
		}

		return y;
	}


	/**
	 * paint prescan file info
	 */
	private void paintSubpicture(Graphics g)
	{
		if (!isSubpictureAvailable)
			return;

		g.drawImage(SubpictureImage, 66, 2, this);
	}

	/**
	 * 
	 */
	private void loadSubpicture()
	{
		SubpictureImage = !isSubpictureAvailable ? null : Common.getSubpictureClass().getScaledImage();
	}

	/**
	 *
	 */
	public void setStreamInfo(StreamInfo _streamInfo)
	{
		streamInfo = _streamInfo.getNewInstance();  //betta to get a copy
		showFileInfo = streamInfo != null;

		if (showFileInfo && !Common.getSettings().getBooleanProperty(Keys.KEY_holdStreamInfoOnOSD))
			startClock(10000);

		isSubpictureAvailable = showFileInfo && streamInfo.getStreamType() == CommonParsing.ES_SUP_TYPE;
		loadSubpicture();

		isOSDInfoAvailable = false;
		isOSDErrorInfo = false;

		repaint();
	}

	/**
	 *
	 */
	public void setOSD(Object[] obj)
	{
		isOSDErrorInfo = false;
		setOSDMessage(obj);
	}

	/**
	 *
	 */
	public void setOSDMessage(String str, boolean b)
	{
		isOSDErrorInfo = b;
		setOSDMessage(new Object[]{ b ? "Fehler" : "Info", "", "", str });
	}

	/**
	 *
	 */
	public void setOSDMessage(Object[] obj)
	{
		setOSDMessage(obj, false);
	}

	/**
	 *
	 */
	public void setOSDMessage(Object[] obj, boolean hold)
	{
		OSDInfo = obj;
		isOSDInfoAvailable = true;
		showFileInfo = false;

		if (!hold) 
			startClock(10000);

		repaint();
	}

	/**
	 *
	 */
	public void showCollectionNumber(int value)
	{
		collection_number = value;

		startClock();
		repaint();
	}

	/**
	 *
	 */
	public void updatePreviewPixel()
	{
		source.newPixels();
	}

	/**
	 * updates cut symbols in preview info field
	 *
	 * @param1 - do_export bool
	 * @param2 - cutpoints list array
	 * @param3 - previewlist of files
	 */
	public void showCutIcon(boolean play, Object[] obj, Object list)
	{
		PLAY = play;
		List previewList = (List) list;

		if (!previewList.isEmpty())
		{
			cutfiles_length = ((PreviewObject) previewList.get(previewList.size() - 1)).getEnd();

			if (obj != null)
			{
				cutfiles_points = new long[obj.length];

				for (int i = 0; i < cutfiles_points.length; i++)
					cutfiles_points[i] = CommonParsing.parseCutValue(obj[i].toString(), false);
			}

			else
				cutfiles_points = null;
		}

		else
		{
			cutfiles_length = 0;
			cutfiles_points = null;
		}

		repaint();
	}

	/**
	 * updates chapter symbols in preview info field
	 *
	 * @param1 - do_export bool
	 * @param2 - chapterpoints list array
	 * @param3 - previewlist of files
	 */
	public void showChapterIcon(Object[] obj, Object list)
	{
		List previewList = (List) list;

		if (!previewList.isEmpty())
		{
			chapter_length = ((PreviewObject) previewList.get(previewList.size() - 1)).getEnd();

			if (obj != null)
			{
				chapter_points = new long[obj.length];

				for (int i = 0; i < chapter_points.length; i++)
					chapter_points[i] = CommonParsing.parseCutValue(obj[i].toString(), false);
			}

			else
				chapter_points = null;
		}

		else
		{
			chapter_length = 0;
			chapter_points = null;
		}

		repaint();
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
			return 0;

		int R = (int)((float)Y +1.402f * (Cr - 128));
		int G = (int)((float)Y -0.34414 * (Cb - 128) -0.71414 * (Cr - 128));
		int B = (int)((float)Y +1.722 * (Cb - 128));

		R = R < 0 ? 0 : (R > 0xFF ? 0xFF : R);
		G = G < 0 ? 0 : (G > 0xFF ? 0xFF : G);
		B = B < 0 ? 0 : (B > 0xFF ? 0xFF : B);

		return (T<<24 | R<<16 | G<<8 | B);
	}

	/**
	 * BMP 24-bit header
	 */
	private byte bmpHead[] = {
		0x42, 0x4D, //'B','M'
		0, 0, 0, 0, // real filesize 32bit, little endian (real size*3 + header(0x36))
		0, 0, 0, 0, 
		0x36, 0, 0, 0, //bitmap info size
		0x28, 0, 0, 0, 
		0, 0, 0, 0, //hsize
		0, 0, 0, 0, //vsize
		1, 0,  //nplane
		0x18, 0, //bitcount 24b
		0, 0, 0, 0, //ncompr
		0, 0, 0, 0, //image bytesize
		(byte)0x88, 0xB, 0, 0, //nxpm
		(byte)0x88, 0xB, 0, 0, //nypm
		0, 0, 0, 0,  //nclrused,
		0, 0, 0, 0   //nclrimp
	};

	/**
	 * performs change of byte order
	 *
	 * @param1 - source byte array
	 * @param2 - array position
	 * @param3 - source int value
	 */
	private void littleEndian(byte[] array, int aPos, int value)
	{
		for (int a = 0; a < 4; a++)
			array[aPos + a] = (byte)(value>>(a * 8) & 0xFF);
	}

	/**
	 *
	 */
	private final float[] aspectratio_table = { 
		1.3333f, 1.3333f, 1.3333f, 1.7778f, 2.2100f, 1.3333f, 1.3333f, 1.3333f, 
		1.3333f, 1.3333f, 1.3333f, 1.3333f, 1.3333f, 1.3333f, 1.3333f, 1.3333f 
	};

	/**
	 * saves cached preview source picture as BMP
	 *
	 * @param1 - automatic saving (demux mode - <extern> panel option)
	 * @param2 - if GUI is not visible, don't update
	 */
	private void saveBMP(int[] pixels, int horizontal_size, int vertical_size, int aspectratio, boolean useAspectRatio)
	{
		int[] sourcepixel = null;

		if (useAspectRatio)
		{
			sourcepixel = getScaledPixel(pixels, horizontal_size, vertical_size, aspectratio_table[aspectratio]);
			horizontal_size = (int) Math.round(aspectratio_table[aspectratio] * vertical_size);
		}

		else
			sourcepixel = pixels;


		int size = horizontal_size * vertical_size;

		if (size <= 0)
			return;

		X_JFileChooser chooser = CommonGui.getMainFileChooser();

		if (bmpCount == 0)
		{
			//suggest the current collection directory
		}

		String newfile = chooser.getCurrentDirectory() + System.getProperty("file.separator") + "X_picture[" + bmpCount + "].bmp";

		chooser.setSelectedFile(new File(newfile));
		chooser.rescanCurrentDirectory();
		chooser.setDialogTitle("save picture");

		int retval = chooser.showSaveDialog(this);

		if(retval == JFileChooser.APPROVE_OPTION)
		{
			File theFile = chooser.getSelectedFile();

			if (theFile != null && !theFile.isDirectory())
				newfile = theFile.getAbsolutePath();
		}

		else 
			return;


		byte[] bmp24 = new byte[3];

		littleEndian(bmpHead, 2, (54 + size * 3));
		littleEndian(bmpHead, 18, horizontal_size);
		littleEndian(bmpHead, 22, vertical_size);
		littleEndian(bmpHead, 34, (size * 3));

		try
		{
			BufferedOutputStream BMPfile = new BufferedOutputStream(new FileOutputStream(newfile), 2048000);
			BMPfile.write(bmpHead);

			for (int a = vertical_size - 1; a >= 0; a--)
				for (int b = 0, pixel = 0; b < horizontal_size; b++)
				{
					pixel = YUVtoRGB(sourcepixel[b + a * horizontal_size]);

					for (int c = 0; c < 3; c++)
						bmp24[c] = (byte)(pixel >>(c * 8) & 0xFF);

					BMPfile.write(bmp24);
				}

			BMPfile.flush();
			BMPfile.close();

			bmpCount++;
		}

		catch (IOException e)
		{}
	}

	/**
	 * create new cutimage pixel data
	 */
	private int[] getScaledPixel(int[] pixels, int horizontal_size, int vertical_size, float aspectratio)
	{
		int new_height = vertical_size;
		int new_width = (int) Math.round(aspectratio * vertical_size);
		int source_height = vertical_size;
		int source_width = horizontal_size;

		float Y = 0;
		float X = 0;
		float decimate_height = (float)source_height / new_height;
		float decimate_width = (float)source_width / new_width;

		int[] new_image = new int[new_width * new_height];

		for (int y = 0; Y < source_height && y < new_height; Y += decimate_height, y++, X = 0)
			for (int x = 0; X < source_width && x < new_width; X += decimate_width, x++)
				new_image[x + (y * new_width)] = pixels[(int)X + ((int)Y * source_width)];

		return new_image;
	}


}