/*
 * @(#)PicturePanel
 * 
 * Copyright (c) 2003-2008 by dvb.matt, All Rights Reserved. 
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

/**
 * resizable preview window
 * idea & introduced 12/2008 by Frank (The Dragon)
 */

package net.sourceforge.dvb.projectx.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.MemoryImageSource;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import net.sourceforge.dvb.projectx.common.Common;
import net.sourceforge.dvb.projectx.common.Keys;
import net.sourceforge.dvb.projectx.common.Resource;
import net.sourceforge.dvb.projectx.parser.CommonParsing;
import net.sourceforge.dvb.projectx.video.PreviewObject;
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
	private Image mixed_image;
	private Image thumb_image;

	private Image InfoBackground = Resource.loadImage("ibg.gif");
	private Image SlideBackground = Resource.loadImage("sbg.gif");

	private MemoryImageSource source;
	private MemoryImageSource mixed_source;
	private MemoryImageSource thumb_source;

	private boolean showFileInfo = false;
	private boolean isSubpictureAvailable = false;
	private boolean isThumbnailAvailable = false;
	private boolean isOSDInfoAvailable = false;
	private boolean isOSDErrorInfo = false;
	private boolean PLAY = true;
	private boolean isMixedImageAvailable = false;
	private boolean isMatrixImageAvailable = false;
	private boolean isFilterActive = false;

	private boolean fullScaled = false;

	private boolean manualzoom = false;
	private boolean definezoom = false;
	private int[] zoomrect = new int[6];

	private int[] mixed_image_array;
	private int[] thumb_image_array;

	private String mixed_image_info = "";

	private int matrix_index = -1;
	private int matrix_new_width = 128; // 100
	private int matrix_new_height = 72; // 56

	private Hashtable matrix_positions = new Hashtable();

	// 4x4 matrix
	private int[][] matrix_table = {
		{ 0, 0 }, { 128, 1 }, { 256, 1 }, { 384, 1 }, 
		{ 0, 72 }, { 128, 72 }, { 256, 72 }, { 384, 72 },
		{ 0, 144 }, { 128, 144 }, { 256, 144 }, { 384, 144 },
		{ 0, 216 }, { 128, 216 }, { 256, 216 }, { 384, 216 },
	};

	// 5x5 matrix
	// private int[][] matrix_table_5x5 = { { 2, 1 }, { 104, 1 }, { 206, 1 }, {
	// 308, 1 }, { 410, 1 }, { 2, 58 },
	// { 104, 58 }, { 206, 58 }, { 308, 58 }, { 410, 58 }, { 2, 117 }, { 104,
	// 117 }, { 206, 117 }, { 308, 117 },
	// { 410, 117 }, { 2, 174 }, { 104, 174 }, { 206, 174 }, { 308, 174 }, { 410
	// , 174 }, { 2, 231 }, { 104, 231 },
	// { 206, 231 }, { 308, 231 }, { 410, 231 } };

	private StreamInfo streamInfo = null;

	private Font font_1;
	private Font font_2;
	private Font font_3;

	private NumberFormat percentage;

	private int ErrorFlag = 0;
	private int bmpCount = 0;
	private int collection_number = -1;

	private int[] imageSizeMin = { 512, 288 };

	private final String tooltip1 = Resource.getString("mpvdecoder.tip1");

	private long cutfiles_length = 0;
	private long[] cutfiles_points = null;
	private long chapter_length = 0;
	private long[] chapter_points = null;

	private Object[] OSDInfo;

	private JPopupMenu popup;
	private JPanel sliderPanel;

	private Clock clock;

	private Dimension previewImageSize;

	private static final Color OUTLINE_COLOR_1 = Color.lightGray;
	private static final Color OUTLINE_COLOR_2 = Color.white;
	private static final Color OUTLINE_COLOR_3 = new Color(151, 151, 151);

	private Dimension infoParts = new Dimension(184, 44);

	/**
	 * class to control short OSD fadings
	 */
	private class Clock implements Runnable {

		private Thread clockThread = null;

		private int sleepAmount = 3000;

		public void start() {
			start(3000);
		}

		public void start(int value) {
			if (clockThread == null) {
				clockThread = new Thread(this, "Clock_4");
				clockThread.setPriority(Thread.MIN_PRIORITY);

				sleepAmount = value;

				clockThread.start();
			}
		}

		public void run() {
			Thread myThread = Thread.currentThread();

			while (clockThread == myThread) {
				try {
					Thread.sleep(sleepAmount); // time on screen
				}
				catch (InterruptedException e) {
				}

				if (update()) {
					repaint();
				}

				stop();
			}
		}

		private boolean update() {
			boolean b = false;

			if (collection_number >= 0) {
				collection_number = -1;
				b = true;
			}

			if (showFileInfo) {
				showFileInfo = false;
				b = true;
			}

			if (isOSDInfoAvailable) {
				isOSDInfoAvailable = false;
				b = true;
			}

			return b;
		}

		public void stop() {
			clockThread = null;
		}
	}

	/**
	 *
	 */
	public PicturePanel() {
		percentage = NumberFormat.getPercentInstance();
		percentage.setMaximumFractionDigits(2);

		source = new MemoryImageSource(imageSizeMin[0], imageSizeMin[1], Common.getMpvDecoderClass().getPreviewPixel(), 0, imageSizeMin[0]);
		source.setAnimated(true);
		image = createImage(source);

		mixed_image_array = new int[imageSizeMin[0] * imageSizeMin[1]];
		mixed_source = new MemoryImageSource(imageSizeMin[0], imageSizeMin[1], mixed_image_array, 0, imageSizeMin[0]);
		mixed_source.setAnimated(true);
		mixed_image = createImage(mixed_source);

		thumb_image_array = new int[160 * 90];
		thumb_source = new MemoryImageSource(160, 90, thumb_image_array, 0, 160);
		thumb_source.setAnimated(true);
		thumb_image = createImage(thumb_source);

		font_1 = new Font("Tahoma", Font.PLAIN, 12);
		font_2 = new Font("Tahoma", Font.BOLD, 12);
		font_3 = new Font("Tahoma", Font.BOLD, 24);

		setBackground(Color.black);
		setVisible(true);

		setToolTipText(tooltip1); // <- VORSCHLAG 1 Tooltip!
		// setSize(512, 346);

		buildPopupMenu();

		sliderPanel = buildSliderPanel();

		addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() >= 1 && (showFileInfo || isOSDInfoAvailable)) {
					showFileInfo = false;
					isOSDInfoAvailable = false;
					repaint();
				}

				if (isMatrixImageAvailable) {
					if (e.getClickCount() == 1 && e.getModifiers() == InputEvent.BUTTON1_MASK) {
						if (matrix_positions.containsKey(String.valueOf(matrix_index))) {
							long value = ((Long) matrix_positions.get(String.valueOf(matrix_index))).longValue();

							CommonGui.getCutPanel().startMatrix(value);
						}
						else {
							CommonGui.getCutPanel().stopMatrix();
						}
					}

					if (e.getClickCount() >= 2 && e.getModifiers() == InputEvent.BUTTON3_MASK) {
						if (matrix_positions.containsKey(String.valueOf(matrix_index))) {
							long value = ((Long) matrix_positions.get(String.valueOf(matrix_index))).longValue();

							disableMatrix();

							CommonGui.getCutPanel().stopMatrix();
							CommonGui.getCutPanel().preview(value);
						}
					}
				}

				else if (e.getClickCount() >= 1 && e.getModifiers() == InputEvent.BUTTON3_MASK) {
					popup.show(getParent(), e.getX(), e.getY());
				}

				if (e.getClickCount() >= 2 && e.getModifiers() == InputEvent.BUTTON1_MASK) {
					CommonGui.getPlayerFrame().repaintPicture();
				}
			}

			public void mouseEntered(MouseEvent e) {
			}

			public void mouseExited(MouseEvent e) {
			}

			public void mousePressed(MouseEvent e) {
			}

			public void mouseReleased(MouseEvent e) {
				if (!definezoom) {
					return;
				}

				definezoom = false;
				manualzoom = false;

				repaint();

				Common.getMpvDecoderClass().setZoomMode(zoomrect);
			}
		});

		addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseDragged(MouseEvent e) {
				if (Common.getMpvDecoderClass().getZoomMode() == 2) {
					zoomrect[0] = e.getX() < 0 ? 0 : e.getX();
					zoomrect[1] = e.getY() < 0 ? 0 : e.getY();

					Common.getMpvDecoderClass().setZoomMode(zoomrect);
				}

				if (!definezoom) {
					return;
				}

				if (!manualzoom) {
					manualzoom = true;
					zoomrect[0] = e.getX();
					zoomrect[1] = e.getY();
				}

				else {
					zoomrect[2] = e.getX() - zoomrect[0];
					zoomrect[3] = (int) (9.0 * zoomrect[2] / 16.0);
				}

				repaint();
			}

			public void mouseMoved(MouseEvent e) {
				int tmp_val = 0;

				if (isMatrixImageAvailable) {
					tmp_val = e.getX() / matrix_new_width + imageSizeMin[0] / matrix_new_width * (e.getY() / matrix_new_height);

					if (tmp_val < matrix_table.length) {
						matrix_index = tmp_val;
						repaint();
					}
				}

				if (!definezoom || manualzoom) {
					return;
				}

				zoomrect[4] = e.getX();
				zoomrect[5] = e.getY();

				repaint();
			}
		});

		clock = new Clock();
	}

	/**
	 *
	 */
	protected void buildPopupMenu() {
		final String[] popup_modes = { "normalzoom", "lbzoom", "zoomarea", "save_1", "save_2" };

		ActionListener al = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String actName = e.getActionCommand();

				if (actName.equals(popup_modes[3])) {
					saveBMP(Common.getMpvDecoderClass().getPixels(), Common.getMpvDecoderClass().getWidth(), Common.getMpvDecoderClass().getHeight(), 0, false);
				}
				else if (actName.equals(popup_modes[4])) {
					saveBMP(Common.getMpvDecoderClass().getPixels(), Common.getMpvDecoderClass().getWidth(), Common.getMpvDecoderClass().getHeight(), Common.getMpvDecoderClass().getAspectRatio(), true);
				}
				else if (actName.equals(popup_modes[0])) {
					Common.getMpvDecoderClass().setZoomMode(0);
				}
				else if (actName.equals(popup_modes[1])) {
					Common.getMpvDecoderClass().setZoomMode(1);
				}
				else if (actName.equals(popup_modes[2])) {
					definezoom = true;
					Arrays.fill(zoomrect, 0);
				}
			}
		};

		popup = new JPopupMenu("save");

		JMenuItem menuitem_1 = popup.add("Normal Zoom");
		menuitem_1.setActionCommand(popup_modes[0]);

		JMenuItem menuitem_2 = popup.add("LB Zoom");
		menuitem_2.setActionCommand(popup_modes[1]);

		JMenuItem menuitem_3 = popup.add("Zoom Area..");
		menuitem_3.setActionCommand(popup_modes[2]);

		popup.addSeparator();

		JMenuItem menuitem_4 = popup.add(Resource.getString("PreviewPanel.saveCurrentPicture"));
		menuitem_4.setActionCommand(popup_modes[3]);

		JMenuItem menuitem_5 = popup.add(Resource.getString("PreviewPanel.saveCurrentPictureDAR"));
		menuitem_5.setActionCommand(popup_modes[4]);

		popup.pack();

		UIManager.addPropertyChangeListener(new UISwitchListener(popup));

		menuitem_1.addActionListener(al);
		menuitem_2.addActionListener(al);
		menuitem_3.addActionListener(al);
		menuitem_4.addActionListener(al);
		menuitem_5.addActionListener(al);
	}

	/**
	 *
	 */
	protected JPanel buildSliderPanel() {
		JPanel panel = new JPanel(new BorderLayout());

		JSlider scanSlider = new JSlider();
		scanSlider.setOrientation(SwingConstants.VERTICAL);
		scanSlider.setInverted(true);
		scanSlider.setMaximum(100);
		scanSlider.setMajorTickSpacing(10);
		scanSlider.setMinorTickSpacing(1);
		scanSlider.setPaintTicks(true);
		scanSlider.setValue(50);
		scanSlider.setPreferredSize(new Dimension(30, 356));
		scanSlider.setMaximumSize(new Dimension(30, 356));
		scanSlider.setMinimumSize(new Dimension(30, 356));

		panel.add(scanSlider);

		return panel;
	}

	/**
	 *
	 */
	public JPanel getSliderPanel() {
		return sliderPanel;
	}

	/**
	 * holds special messages
	 */
	public void startClock() {
		clock.start();
	}

	/**
	 * holds special messages
	 */
	public void startClock(int value) {
		clock.start(value);
	}

	/**
	 * updates the preview graphic
	 */
	public void paint(Graphics g) {

		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

		previewImageSize = new Dimension(this.getSize().width - infoParts.width, this.getSize().height - infoParts.height);
		setPreviewSize(previewImageSize.width, previewImageSize.height);

		paintInfoBackground(g);
		paintOutline(g);

		paintPreviewPicture(g, g2);

		paintMixedCutPreviewPicture(g, g2);
		paintMatrixPreviewPicture(g);

		paintZoomInfo(g);
		paintSlideBackground(g);
		paintVideoInfo(g);
		paintWSSInfo(g);
		paintErrorInfo(g);
		paintPlayInfo(g);

		g.setFont(font_2);

		paintCutInfo(g);
		paintPositionInfo(g);
		paintChapterInfo(g);
		paintSubpicture(g, g2);
		paintOSDInfo(g);
		paintFileInfo(g);
		paintCollectionNumber(g);
		paintZoomRect(g);
	}

	/**
	 * paint background
	 */
	private void paintInfoBackground(Graphics g) {
		g.setColor(new Color(0, 35, 110));
		g.fillRect(0, 0, previewImageSize.width + 200, previewImageSize.height + 100);

		g.drawImage(InfoBackground, 0, previewImageSize.height + 2, previewImageSize.width, InfoBackground.getHeight(this), this);
	}

	/**
	 * paint background
	 */
	private void paintSlideBackground(Graphics g) {
		g.drawImage(SlideBackground, previewImageSize.width + 2, 0, infoParts.width, previewImageSize.height + infoParts.height, this);
	}

	/**
	 * paint preview
	 */
	private void paintPreviewPicture(Graphics g, Graphics2D g2) {

		//erste variante, only up-scale again the down-scaled prev.image

	// dont use, affects text negative:
	//	g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
	//	g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	//	g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);

		if (!fullScaled) 
			g2.drawImage(image, 0, 0, previewImageSize.width, previewImageSize.height, this);
		else
			g.drawImage(image, 0, 0, this);
	}

	/**
	 * paint cut mixed preview
	 */
	private void paintMixedCutPreviewPicture(Graphics g, Graphics2D g2) {
		if (!isMixedImageAvailable) {
			return;
		}

		if (!fullScaled) 
			g2.drawImage(mixed_image, 0, 0, previewImageSize.width, previewImageSize.height, this);
		else
			g.drawImage(mixed_image, 0, 0, this);

	//	g.drawImage(mixed_image, 2, 2, this);

		g.setFont(font_2);
		g.setColor(Color.green);
		g.drawString(mixed_image_info, 340, 280);
	}

	/**
	 * paint cut matrix preview
	 */
	private void paintMatrixPreviewPicture(Graphics g) {
		if (!isMatrixImageAvailable) {
			return;
		}

		g.drawImage(mixed_image, 0, 0, this);

		if (matrix_index < 0 || matrix_index >= matrix_table.length) {
			return;
		}

		if (!matrix_positions.containsKey(String.valueOf(matrix_index))) {
			return;
		}

		g.setColor(Color.green);
		g.drawRect(matrix_table[matrix_index][0], matrix_table[matrix_index][1], matrix_new_width + 1, matrix_new_height + 1);
	//	g.drawRect(matrix_table[matrix_index][0] + 1, matrix_table[matrix_index][1] + 1, matrix_new_width + 1, matrix_new_height + 1);

		g.setFont(font_2);
		g.drawString(String.valueOf(matrix_index), matrix_table[matrix_index][0] + 5, matrix_table[matrix_index][1] + 14);
		g.drawString(matrix_positions.get(String.valueOf(matrix_index)).toString(), matrix_table[matrix_index][0] + 5, matrix_table[matrix_index][1] + 54);
		g.drawString(matrix_positions.get(String.valueOf(matrix_index) + "TC").toString(), matrix_table[matrix_index][0] + 5, matrix_table[matrix_index][1] + 68);
		g.drawString(percentage.format(((Long) matrix_positions.get(String.valueOf(matrix_index))).doubleValue() / ((Long) matrix_positions.get("end")).doubleValue()), matrix_table[matrix_index][0] + 50,	matrix_table[matrix_index][1] + 14);
	}

	/**
	 * paint outline
	 */
	private void paintOutline(Graphics g) {
		int infoBackgroundHeight = infoParts.height;

		g.setColor(OUTLINE_COLOR_3);
		g.drawLine(1, 1, previewImageSize.width, 1);
		g.drawLine(1, 1, 1, previewImageSize.height + infoBackgroundHeight);

		g.setColor(OUTLINE_COLOR_1);
		g.drawLine(2, previewImageSize.height, previewImageSize.width, previewImageSize.height);
		g.drawLine(previewImageSize.width, 2, previewImageSize.width, previewImageSize.height + infoBackgroundHeight);

		g.setColor(OUTLINE_COLOR_2);
		g.drawLine(0, 0, previewImageSize.width + 1, 0);
		g.drawLine(0, 0, 0, previewImageSize.height + infoBackgroundHeight);
		g.drawLine(0, previewImageSize.height + 1, previewImageSize.width + 1, previewImageSize.height + 1);
		g.drawLine(previewImageSize.width + 1, 0, previewImageSize.width + 1, previewImageSize.height + infoBackgroundHeight);
	}

	/**
	 * paint videoinfo
	 */
	private void paintVideoInfo(Graphics g) {
		g.setFont(font_1);
		g.setColor(Color.white);

		String[] mpg_info = Common.getMpvDecoderClass().getMpgInfo();

		for (int i = 0, x = previewImageSize.width + 8, y = 18; i < mpg_info.length - 1; i++, y += 14) {
			if (mpg_info[i] != null) {
				g.drawString(mpg_info[i], x, y);
			}
		}
	}

	/**
	 * paint
	 */
	private void paintCollectionNumber(Graphics g) {
		if (collection_number < 0) {
			return;
		}

		g.setFont(font_2);
		g.setColor(Color.green);
		g.drawString("Collection", previewImageSize.width - 60, 16);

		g.setFont(font_3);
		g.setColor(Color.green);
		g.drawString(String.valueOf(collection_number), previewImageSize.width - 50, 38);
	}

	/**
	 * paint
	 */
	private void paintZoomInfo(Graphics g) {
		g.setFont(font_2);
		g.setColor(Color.green);
		g.drawString(Common.getMpvDecoderClass().getZoomInfo(), 20, previewImageSize.height - 5);
	}

	/**
	 * paint
	 */
	private void paintZoomRect(Graphics g) {
		if (!definezoom) {
			return;
		}

		if (!manualzoom) {
			g.setColor(new Color(255, 255, 255, 255));
			g.drawLine(zoomrect[4] - 10, zoomrect[5], zoomrect[4] + 10, zoomrect[5]);
			g.drawLine(zoomrect[4], zoomrect[5] - 10, zoomrect[4], zoomrect[5] + 10);
		}

		else {
			g.setColor(new Color(100, 100, 255, 120));
			g.fillRect(zoomrect[0], zoomrect[1], zoomrect[2], zoomrect[3]);

			g.setColor(new Color(255, 255, 255, 255));
			g.drawRect(zoomrect[0], zoomrect[1], zoomrect[2], zoomrect[3]);
		}
	}

	/**
	 * paint wss info
	 */
	private void paintWSSInfo(Graphics g) {
		String str;

		if ((str = Common.getMpvDecoderClass().getWSSInfo()) == null) {
			setToolTipText(tooltip1);
			return;
		}

		g.setFont(font_2);
		g.setColor(Color.green);
		g.drawString("WSS present", 10, 16);
		g.drawString(Common.getMpvDecoderClass().getWSSFormatInfo(), 10, 30);

		if (Common.getMpvDecoderClass().getPalPlusInfo()) {
			g.drawImage(PalPlusImage, 8, 34, this);
		}

		setToolTipText("<html>" + tooltip1 + "<p><p>" + str + "</html>");
	}

	/**
	 * paint play info
	 */
	private void paintPlayInfo(Graphics g) {
		int x[] = { previewImageSize.width + 8, previewImageSize.width + 8, previewImageSize.width + 28 };
		int y[] = { previewImageSize.height + 18, previewImageSize.height + 38, previewImageSize.height + 28 };

		if (isFilterActive && PLAY) {
			g.setColor(Color.yellow);
			g.drawString("Export Filter Mismatch !", previewImageSize.width + 32, previewImageSize.height + 32);

			g.fillRect(previewImageSize.width + 8, previewImageSize.height + 18, 8, 20);
			g.fillRect(previewImageSize.width + 20, previewImageSize.height + 18, 8, 20);

			// yellow border around pic
			g.drawRect(0, 0, previewImageSize.width - 1, previewImageSize.height - 1);
			g.drawRect(1, 1, previewImageSize.width - 3, previewImageSize.height - 3);
		}

		else if (PLAY) {
			g.setColor(Color.green);
			g.fillPolygon(x, y, 3);
			g.drawString("Inside Export Range", previewImageSize.width + 32, previewImageSize.height + 32);
		}

		else {
			g.setColor(Color.red);
			g.fillRect(previewImageSize.width + 8, previewImageSize.height + 18, 20, 20);
			g.drawString("Outside Export Range", previewImageSize.width + 32, previewImageSize.height + 32);

			// red border around pic
			g.drawRect(0, 0, previewImageSize.width - 1, previewImageSize.height - 1);
			g.drawRect(1, 1, previewImageSize.width - 3, previewImageSize.height - 3);
		}
	}

	/**
	 * paint error info
	 */
	private void paintErrorInfo(Graphics g) {
		ErrorFlag = Common.getMpvDecoderClass().getErrors();

		if ((ErrorFlag & 1) != 0) {
			g.setColor(Color.white);
			g.fill3DRect(150, 120, 200, 20, true);
			g.setColor(Color.red);
			g.drawString("picture decoding not possible", 160, 133);
		}

	//	if ((ErrorFlag & 4) != 0) {
		if ((ErrorFlag & 0xC) == 4) {
			g.setColor(Color.white);
			g.fill3DRect(150, 135, 200, 20, true);
			g.setColor(Color.red);
			g.drawString("not enough data in buffer", 160, 148);
		}

		if ((ErrorFlag & 2) != 0) {
			g.setColor(Color.white);
			g.fill3DRect(150, 150, 200, 20, true);
			g.setColor(Color.red);
			g.drawString("cannot find sequence header", 160, 163);
		}

		if ((ErrorFlag & 0x13) > 0x10) // 5 + 2 + 1
		{
			g.setColor(Color.white);
			g.fill3DRect(150, 150, 200, 20, true);
			g.setColor(Color.red);
			g.drawString("no Sequ. but GOP header found", 160, 163);
		}

		if ((ErrorFlag & 8) != 0) {
			g.setColor(Color.white);
		//	g.fill3DRect(150, 165, 200, 20, true);
			g.fill3DRect(150, 135, 200, 20, true);
			g.setColor(Color.red);
		//	g.drawString("data seems to be MPEG-4/H.264", 160, 178);
			g.drawString("no preview for MPEG-4/H.264", 160, 148);
		}

		if ((ErrorFlag & 0x20) != 0) {
			g.setColor(Color.white);
			g.fill3DRect(150, 165, 200, 20, true);
			g.setColor(Color.red);
			g.drawString("java instance out of memory", 160, 178);
		}
	}

	/**
	 * paint cut info
	 */
	private void paintCutInfo(Graphics g) {
		if (cutfiles_length <= 0) {
			return;
		}

		// int x1 = 10, y1 = 327, w1 = 492, h1 = 6;
		int x1 = 10, y1 = previewImageSize.height + 12, w1 = previewImageSize.width - 22, h1 = 6;

		g.setColor(new Color(0, 200, 0));
		g.fillRect(x1, y1, w1, h1);
		g.setColor(Color.white);
		g.drawRect(x1 - 2, y1 - 2, w1 + 3, h1 + 3);

		/**
		 * paint cut markers
		 */
		if (cutfiles_points != null && cutfiles_points.length > 0) {
			int p0 = 0, p1 = 0;

			for (int i = 0; i < cutfiles_points.length; i++) {
				if (cutfiles_points[i] > cutfiles_length) {
					break;
				}

				p0 = i == 0 ? 0 : (int) (cutfiles_points[i - 1] * w1 / cutfiles_length);
				p1 = (int) (cutfiles_points[i] * w1 / cutfiles_length);

				if (i % 2 == 0) {
					g.setColor(new Color(150, 0, 0));
					g.fillRect(x1 + p0, y1, p1 - p0, h1);
				}

				g.setColor(new Color(200, 100, 200));
				g.fillRect(x1 + p1 - 1, y1 - 4, 2, h1 + 8);

				// int[] x = { x1 + p1 - 1, x1 + p1 - 5, x1 + p1 + 5 };
				int[] y = { y1 - 3, y1 - 3 - 5, y1 - 3 - 5 };

				int[] x_1 = { x1 + p1 - 1, x1 + p1 - 1, x1 + p1 + 5 };
				int[] x_2 = { x1 + p1 + 1, x1 + p1 - 5, x1 + p1 + 1 };

				if (i % 2 == 0) {
					g.fillPolygon(x_1, y, 3);
				}
				else {
					g.fillPolygon(x_2, y, 3);
				}
			}

			if ((cutfiles_points.length & 1) == 0) {
				p0 = (int) (cutfiles_points[cutfiles_points.length - 1] * w1 / cutfiles_length);

				g.setColor(new Color(150, 0, 0));
				g.fillRect(x1 + p0, y1, w1 - p0, h1);
			}
		}
	}

	/**
	 * paint position info
	 */
	private void paintPositionInfo(Graphics g) {
		// int x1 = 10, y1 = 346, w1 = 492, h1 = 8;
		int x1 = 10, y1 = previewImageSize.height + 30, w1 = previewImageSize.width - 22, h1 = 8;

		List positions = Common.getMpvDecoderClass().getPositions();

		/**
		 * paint current position
		 */
		g.setColor(Color.white);
		g.fillRect(x1, y1, 2, h1); // start
		g.fillRect(x1 + w1, y1, 2, h1); // end
		g.fillRect(x1, y1 + 3, w1, 2); // axis

		long pos;
		long max = 1;
		int mark;

		if (!positions.isEmpty()) {
			max = ((Long) positions.get(positions.size() - 1)).longValue();

			for (int i = 1, j = positions.size() - 1; i < j; i++) {
				pos = ((Long) positions.get(i)).longValue();
				mark = (int) (pos * w1 / max);
				g.fillRect(x1 + mark, y1, 2, h1); // mark
			}

			pos = ((Long) positions.get(0)).longValue();
			mark = (int) (pos * w1 / max);
			g.setColor(Color.red);
			g.fillRect(x1 + mark, y1, 2, h1); // mark
		}

		g.setFont(font_2);
		g.setColor(Color.green);

		String str = Common.getMpvDecoderClass().getPidAndFileInfo();
		int sep1 = str.indexOf("-");
		int sep2 = sep1;
		if (sep1 < 0) {
			sep1 = sep2 = str.length();
		}
		else {
			sep1 -= 1;
			sep2 += 2;
		}

		g.drawString(str.substring(0, sep1), previewImageSize.width + 8, previewImageSize.height + 12); // pid
		g.drawString(str.substring(sep2), previewImageSize.width + 8, previewImageSize.height - 2); // file

		// matrix
		if (!isMatrixImageAvailable) {
			return;
		}

		for (int i = 0; i < matrix_table.length; i++) {
			if (matrix_positions.containsKey(String.valueOf(i))) {
				pos = ((Long) matrix_positions.get(String.valueOf(i))).longValue();
				mark = (int) (pos * w1 / max);
				g.fillRect(x1 + mark, y1 + 4, 2, h1 - 4); // mark matrix
				// position
			}
		}

		if (matrix_positions.containsKey(String.valueOf(matrix_index))) {
			pos = ((Long) matrix_positions.get(String.valueOf(matrix_index))).longValue();
			mark = (int) (pos * w1 / max);
			g.setColor(Color.magenta);
			g.fillRect(x1 + mark, y1, 2, h1); // mark matrix position
		}
	}

	/**
	 * paint chapter info
	 */
	private void paintChapterInfo(Graphics g) {
		if (chapter_length <= 0) {
			return;
		}

		// int x1 = 10, y1 = 327, w1 = 492, h1 = 6;
		int x1 = 10, y1 = previewImageSize.height + 22, w1 = previewImageSize.width - 22, h1 = 6;

		/**
		 * paint chapter markers
		 */
		if (chapter_points != null && chapter_points.length > 0) {
			int p0 = 0;
			int p1 = 0;

			for (int i = 0; i < chapter_points.length; i++) {
				if (chapter_points[i] > chapter_length) {
					break;
				}

				p0 = i == 0 ? 0 : (int) (chapter_points[i - 1] * w1 / chapter_length);
				p1 = (int) (chapter_points[i] * w1 / chapter_length);

				g.setColor(new Color(195, 205, 255));
				g.fillRect(x1 + p1 - 1, y1 - 4, 2, h1 + 8);

				int[] x = { x1 + p1 - 1, x1 + p1 - 5, x1 + p1 + 5 };
				int[] y = { y1 + h1 + 3, y1 + h1 + 3 + 5, y1 + h1 + 3 + 5 };

				g.fillPolygon(x, y, 3);
			}
		}
	}

	/**
	 * paint
	 */
	private void paintOSDBg(Graphics g, int x3, int y3, int w3, int h3) {
		Color blue_1 = new Color(150, 150, 255, 210);
		Color blue_2 = new Color(50, 20, 225, 210);
		Color red_1 = new Color(255, 150, 150, 210);
		Color red_2 = new Color(225, 20, 20, 210);

		g.setColor(isOSDErrorInfo ? red_1 : blue_1);
		g.fillRect(x3, y3 + 20, w3, previewImageSize.height - 70);

		g.setColor(isOSDErrorInfo ? red_2 : blue_2);
		g.fillRect(x3, y3, w3, 20);
		g.fillRect(x3, y3 + h3, w3, 34);

		g.setColor(Color.white);
		g.drawLine(x3, y3 + 20, x3 + w3 - 1, y3 + 20);
		g.drawLine(x3, y3 + previewImageSize.height - 50, x3 + w3 - 1, y3 + previewImageSize.height - 50);
	}

	/**
	 * paint prescan file info
	 */
	private void paintOSDInfo(Graphics g) {
		if (!isOSDInfoAvailable || OSDInfo == null || OSDInfo.length == 0) {
			return;
		}

		int x3 = 8; // 15;
		int y3 = 8;
		int w3 = previewImageSize.width - 18; // 482;
		int h3 = previewImageSize.height - 50;
		int yOffset = 0;

		paintOSDBg(g, x3, y3, w3, h3);

		g.setColor(Color.white);
		g.drawString(OSDInfo[0].toString(), x3 + 10, y3 + 14);
		g.drawString(OSDInfo[1].toString(), x3 + 10, y3 + h3 + 16);

		yOffset = y3 + 24;

		for (int i = 2; i < OSDInfo.length; i++) {
			yOffset = paintSubInfo(g, OSDInfo[i].toString(), null, x3, yOffset);
		}
	}

	/**
	 * paint prescan file info
	 */
	private void paintFileInfo(Graphics g) {
		if (!showFileInfo) {
			return;
		}

		int x3 = 8; // 15;
		int y3 = 8;
		int w3 = previewImageSize.width - 18; // 482;
		int h3 = previewImageSize.height - 50;
		int yOffset = 0;
		Object[] obj;

		paintOSDBg(g, x3, y3, w3, h3);

		g.setColor(Color.white);
		g.drawString(streamInfo.getFileSourceAndName(), x3 + 6, y3 + 14);
		g.drawString(streamInfo.getFileDate(), x3 + 6, y3 + h3 + 16);
		g.drawString(streamInfo.getFileSize(), x3 + 6, y3 + h3 + 30);

		//thumbnail
		if (isThumbnailAvailable)
		{
			g.setColor(Color.gray);
			g.drawRect(x3 + w3 - 160 - 5, y3 + h3 - 90 - 5, 160 + 1, 90 + 1);
			g.drawImage(thumb_image, x3 + w3 - 160 - 4, y3 + h3 - 90 - 4, this);
		}

		g.setColor(Color.white);

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
	private int paintSubInfo(Graphics g, String str, Image icon, int x, int y) {
		if (str != null && str.length() > 0) {
			if (y > previewImageSize.height - 60) {
				g.fillPolygon(new int[] { x + 3, x + 17, x + 9 }, new int[] { y, y, y + 7 }, 3);
				g.drawString("...", x + 25, y + 6);
				return y;
			}

			int i = str.indexOf("\t");
			int j = icon != null ? 25 : 10;

			if (icon != null) {
				g.drawImage(icon, x + 2, y, this);
			}

			if (i >= 0) {
				g.drawString(str.substring(i + 1), x + j + 120, y + 12);
				g.drawString(str.substring(0, i), x + j, y + 12);
			}
			else {
				g.drawString(str, x + j, y + 12);
			}

			y += 16;
		}

		return y;
	}

	/**
	 * 
	 */
	private int paintSubInfo(Graphics g, Object[] obj, Image icon, int x, int y) {
		if (obj != null && obj.length > 0) {
			for (int i = 0; i < obj.length; i++) {
				y = paintSubInfo(g, "" + (i + 1) + ".  " + obj[i].toString(), icon, x, y);
			}
		}

		return y;
	}

	/**
	 * paint prescan subpic info
	 */
	private void paintSubpicture(Graphics g, Graphics2D g2) {
		if (!isSubpictureAvailable) {
			return;
		}

	//	if (!fullScaled) 
			g2.drawImage(SubpictureImage, 0, 0, previewImageSize.width, previewImageSize.height, this);
	//	else
	//		g.drawImage(SubpictureImage, 66, 0, this);
	}

	/**
	 * 
	 */
	private void loadSubpicture() {
		SubpictureImage = !isSubpictureAvailable ? null : Common.getSubpictureClass().getScaledImage(previewImageSize.width, previewImageSize.height);
	}

	/**
	 *
	 */
	public void setPreviewSize(int w, int h) {

		if (!Common.getSettings().getBooleanProperty(Keys.KEY_Preview_fullScaled))
		{
			w = 512;
			h = 288;
		}

		if (w == imageSizeMin[0] && h == imageSizeMin[1])
			return;

		imageSizeMin[0] = w;
		imageSizeMin[1] = h;

		Common.getMpvDecoderClass().setPreviewSize(w, h);

		source = new MemoryImageSource(imageSizeMin[0], imageSizeMin[1], Common.getMpvDecoderClass().getPreviewPixel(), 0, imageSizeMin[0]);
		source.setAnimated(true);
		image = createImage(source);

		source.newPixels();

	}

	/**
	 *
	 */
	public void setStreamInfo(StreamInfo _streamInfo) {
		streamInfo = _streamInfo.getNewInstance(); // betta to get a copy
		showFileInfo = streamInfo != null;

		isThumbnailAvailable = showFileInfo && streamInfo.getThumbnail() != null && streamInfo.getThumbnail().length > 0;

		if (isThumbnailAvailable)
		{
			int[] thumb = streamInfo.getThumbnail();
			System.arraycopy(thumb, 0, thumb_image_array, 0, thumb.length);
			thumb_source.newPixels();
		}

		if (showFileInfo && !Common.getSettings().getBooleanProperty(Keys.KEY_holdStreamInfoOnOSD)) {
			startClock(10000);
		}

		isSubpictureAvailable = showFileInfo && streamInfo.getStreamType() == CommonParsing.ES_SUP_TYPE;
		loadSubpicture();

		isOSDInfoAvailable = false;
		isOSDErrorInfo = false;

		repaint();
	}

	/**
	 *
	 */
	public void setOSD(Object[] obj) {
		isOSDErrorInfo = false;
		setOSDMessage(obj);
	}

	/**
	 *
	 */
	public void setOSDMessage(String str, boolean b) {
		isOSDErrorInfo = b;
		setOSDMessage(new Object[] { b ? "Fehler" : "Info", "", "", str });
	}

	/**
	 *
	 */
	public void setOSDMessage(Object[] obj) {
		setOSDMessage(obj, false);
	}

	/**
	 *
	 */
	public void setOSDMessage(Object[] obj, boolean hold) {
		OSDInfo = obj;
		isOSDInfoAvailable = true;
		showFileInfo = false;

		if (!hold) {
			startClock(10000);
		}

		repaint();
	}

	/**
	 *
	 */
	public void showCollectionNumber(int value) {
		collection_number = value;

		startClock();
		repaint();
	}

	/**
	 *
	 */
	public Image getPreviewImage() {
		return image;
	}

	/**
	 *
	 */
	public void updatePreviewPixel() {
		source.newPixels();
	}

	/**
	 * modify transparency
	 */
	public void setMixedPreviewPixel(int[] picture, int transparency) {
		isMixedImageAvailable = picture != null && transparency > 0;

		mixed_image_info = isMixedImageAvailable ? "CutImage Mix Mode: " + transparency * 100 / 255 + "%" : "";

		if (!isMixedImageAvailable) {
			clearMixedImage();
		}
		else {
			for (int i = 0, j = mixed_image_array.length; i < j; i++) {
				mixed_image_array[i] = 0xFFFFFF & picture[i] | transparency << 24;
			}
		}

		isMatrixImageAvailable = false;

		mixed_source.newPixels();
	}

	/**
	 * 
	 */
	public void clearMixedImage() {
		Arrays.fill(mixed_image_array, 0);
	}

	/**
	 * modify matrix image
	 */
	public void setMatrixPreviewPixel(int index) {
		isMatrixImageAvailable = true;

		Common.getMpvDecoderClass().getScaledCutMatrixImage(mixed_image_array, matrix_new_width, matrix_new_height, matrix_table[index][0], matrix_table[index][1]);

		mixed_source.newPixels();
	}

	/**
	 * get matrix
	 */
	public int[][] getMatrixTable() {
		return matrix_table;
	}

	/**
	 * set matrix pos
	 */
	public void setMatrixIndexPosition(int index, long value, String str) {
		matrix_positions.put(String.valueOf(index), new Long(value)); // pos
		matrix_positions.put(String.valueOf(index) + "TC", str); // TC
	}

	/**
	 * set matrix pos
	 */
	public void setMatrixEndPosition(long value) {
		matrix_positions.put("end", new Long(value));
	}

	/**
	 * set matrix pos
	 */
	public void resetMatrixPositions(long value) {
		matrix_positions.clear();
		setMatrixEndPosition(value);
		clearMixedImage();
	}

	/**
	 * set matrix
	 */
	public void disableMatrix() {
		isMatrixImageAvailable = false;
	}

	/**
	 * get filter mismatch
	 */
	public void setFilterStatus(boolean b) {
		isFilterActive = b;
	}

	/**
	 * updates cut symbols in preview info field
	 * 
	 * @param1 - do_export bool
	 * @param2 - cutpoints list array
	 * @param3 - previewlist of files
	 */
	public void showCutIcon(boolean play, Object[] obj, Object list) {
		PLAY = play;
		List previewList = (List) list;

		if (!previewList.isEmpty()) {
			cutfiles_length = ((PreviewObject) previewList.get(previewList.size() - 1)).getEnd();

			if (obj != null) {
				cutfiles_points = new long[obj.length];

				for (int i = 0; i < cutfiles_points.length; i++) {
					cutfiles_points[i] = CommonParsing.parseCutValue(obj[i].toString(), false);
				}
			}
			else {
				cutfiles_points = null;
			}
		}

		else {
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
	public void showChapterIcon(Object[] obj, Object list) {
		List previewList = (List) list;

		if (!previewList.isEmpty()) {
			chapter_length = ((PreviewObject) previewList.get(previewList.size() - 1)).getEnd();

			if (obj != null) {
				chapter_points = new long[obj.length];

				for (int i = 0; i < chapter_points.length; i++) {
					chapter_points[i] = CommonParsing.parseCutValue(obj[i].toString(), false);
				}
			}
			else {
				chapter_points = null;
			}
		}

		else {
			chapter_length = 0;
			chapter_points = null;
		}

		repaint();
	}

	/**
	 * performs YUV to RGB conversion
	 */
	private int YUVtoRGB(int YUV) {
		int T = 0xFF;
		int Y = 0xFF & YUV >>> 16;
		int Cb = 0xFF & YUV >>> 8;
		int Cr = 0xFF & YUV;

		if (Y == 0) {
			return 0;
		}

		int R = (int) (Y + 1.402f * (Cr - 128));
		int G = (int) (Y - 0.34414 * (Cb - 128) - 0.71414 * (Cr - 128));
		int B = (int) (Y + 1.722 * (Cb - 128));

		R = R < 0 ? 0 : R > 0xFF ? 0xFF : R;
		G = G < 0 ? 0 : G > 0xFF ? 0xFF : G;
		B = B < 0 ? 0 : B > 0xFF ? 0xFF : B;

		return T << 24 | R << 16 | G << 8 | B;
	}

	/**
	 * BMP 24-bit header
	 */
	private byte bmpHead[] = { 0x42, 0x4D, // 'B','M'
			0, 0, 0, 0, // real filesize 32bit, little endian (real size*3 +
			// header(0x36))
			0, 0, 0, 0, 0x36, 0, 0, 0, // bitmap info size
			0x28, 0, 0, 0, 0, 0, 0, 0, // hsize
			0, 0, 0, 0, // vsize
			1, 0, // nplane
			0x18, 0, // bitcount 24b
			0, 0, 0, 0, // ncompr
			0, 0, 0, 0, // image bytesize
			(byte) 0x88, 0xB, 0, 0, // nxpm
			(byte) 0x88, 0xB, 0, 0, // nypm
			0, 0, 0, 0, // nclrused,
			0, 0, 0, 0 // nclrimp
	};

	/**
	 * performs change of byte order
	 * 
	 * @param1 - source byte array
	 * @param2 - array position
	 * @param3 - source int value
	 */
	private void littleEndian(byte[] array, int aPos, int value) {
		for (int i = 0; i < 4; i++) {
			array[aPos + i] = (byte) (value >> (i << 3) & 0xFF);
		}
	}

	/**
	 *
	 */
	private double[] aspectratio_table = { 
		1.3333, 1.3333, 1.3333, 1.7778, 2.2100, 
		1.3333, 1.3333, 1.3333, 1.3333,	1.3333, 
		1.3333, 1.3333, 1.3333, 1.3333, 1.3333, 1.3333 
	};

	/**
	 * saves cached preview source picture as BMP
	 * 
	 * @param1 - automatic saving (demux mode - <extern> panel option)
	 * @param2 - if GUI is not visible, don't update
	 */
	private void saveBMP(int[] pixels, int horizontal_size, int vertical_size, int aspectratio_index, boolean useAspectRatio) {
		int[] sourcepixel = null;
		int source_mb_width = (0xF & horizontal_size) != 0 ? (horizontal_size & ~0xF) + 16 : horizontal_size;

		if (useAspectRatio) {
			sourcepixel = getScaledPixel(pixels, horizontal_size, vertical_size, aspectratio_table[aspectratio_index]);

			if (sourcepixel == null) {
				sourcepixel = pixels;
			}
			else {
				horizontal_size = (int) Math.round(aspectratio_table[aspectratio_index] * vertical_size);
				source_mb_width = horizontal_size;
			}
		}
		else {
			sourcepixel = pixels;
		}

		int size = horizontal_size * vertical_size;

		if (size <= 0) {
			return;
		}

		X_JFileChooser chooser = CommonGui.getMainFileChooser();

		if (bmpCount == 0) {
			// suggest the current collection directory
		}

		String newfile = chooser.getCurrentDirectory() + System.getProperty("file.separator") + "X_picture(" + bmpCount	+ ").bmp";

		chooser.setSelectedFile(new File(newfile));
		chooser.rescanCurrentDirectory();
		chooser.setDialogTitle("save picture");

		int retval = chooser.showSaveDialog(this);

		if (retval == JFileChooser.APPROVE_OPTION) {
			File theFile = chooser.getSelectedFile();

			if (theFile != null && !theFile.isDirectory()) {
				newfile = theFile.getAbsolutePath();
			}
		}
		else {
			return;
		}

		byte[] bmp24 = new byte[3];

		// int source_mb_width = (0xF & horizontal_size) != 0 ? (horizontal_size
		// & ~0xF) + 16 : horizontal_size;
		int padding = (horizontal_size & 3) != 0 ? horizontal_size & 3 : 0;

		size = horizontal_size * vertical_size * 3 + (padding > 0 ? padding * vertical_size : 0);

		littleEndian(bmpHead, 2, (54 + size));
		littleEndian(bmpHead, 18, horizontal_size);
		littleEndian(bmpHead, 22, vertical_size);
		littleEndian(bmpHead, 34, size);

		try {
			BufferedOutputStream BMPfile = new BufferedOutputStream(new FileOutputStream(newfile), 2048000);
			BMPfile.write(bmpHead);

			byte[] padding_bytes = new byte[padding];

			for (int a = vertical_size - 1, tmp1; a >= 0; a--) {
				tmp1 = a * source_mb_width;

				for (int b = 0, pixel = 0; b < horizontal_size; b++) {
					pixel = YUVtoRGB(sourcepixel[b + tmp1]);

					for (int c = 0; c < 3; c++) {
						bmp24[c] = (byte) (pixel >> (c << 3) & 0xFF);
					}

					BMPfile.write(bmp24);
				}

				if (padding > 0) {
					BMPfile.write(padding_bytes);
				}
			}

			BMPfile.flush();
			BMPfile.close();

			bmpCount++;
		}

		catch (Exception e) {
			Common.setExceptionMessage(e);
		}
	}

	/**
	 * create new cutimage pixel data jdk122 seems to have a problem when it
	 * does multiplication !!
	 */
	private int[] getScaledPixel(int[] pixels, int horizontal_size, int vertical_size, double aspectratio) {
		int source_height = vertical_size;
		int source_width = horizontal_size;
		int source_mb_width = (0xF & horizontal_size) != 0 ? (horizontal_size & ~0xF) + 16 : horizontal_size;
		int new_height = vertical_size;
		int new_width = (int) Math.round(vertical_size * aspectratio);

		int new_size = new_width * new_height;

		// oversized or zero ?
		if (new_size > 0x1000000 || new_size <= 0) {
			return null;
		}

		float Y = 0;
		float X = 0;
		double decimate_height = (double) source_height / new_height;
		double decimate_width = (double) source_width / new_width;

		int[] new_image = new int[new_size];

		for (int y = 0; Y < source_height && y < new_height; Y += decimate_height, y++, X = 0) {
			for (int x = 0; X < source_width && x < new_width; X += decimate_width, x++) {
				new_image[x + y * new_width] = pixels[(int) X + (int) Y * source_mb_width];
			}
		}

		return new_image;
	}

}