/*
 * @(#)FileProperties.java
 *
 * Copyright (c) 2006-2008 by dvb.matt, All Rights Reserved. 
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
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.KeyEvent;
import java.awt.Color;
import java.awt.Graphics;
import javax.swing.event.*;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.SwingConstants;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.JSlider;
import javax.swing.JScrollPane;
import javax.swing.JViewport;

import java.io.IOException;
import java.util.Arrays;

import net.sourceforge.dvb.projectx.gui.UISwitchListener;
import net.sourceforge.dvb.projectx.gui.CommonGui;
import net.sourceforge.dvb.projectx.gui.ColumnLayout;
import net.sourceforge.dvb.projectx.gui.ComboBoxIndexListener;
import net.sourceforge.dvb.projectx.gui.ComboBoxItemListener;
import net.sourceforge.dvb.projectx.gui.CheckBoxListener;
import net.sourceforge.dvb.projectx.gui.TextFieldListener;
import net.sourceforge.dvb.projectx.gui.TextFieldKeyListener;

import net.sourceforge.dvb.projectx.common.Keys;
import net.sourceforge.dvb.projectx.common.Common;
import net.sourceforge.dvb.projectx.common.Resource;
import net.sourceforge.dvb.projectx.common.JobCollection;

import net.sourceforge.dvb.projectx.xinput.XInputFile;

import net.sourceforge.dvb.projectx.video.Preview;

import net.sourceforge.dvb.projectx.parser.HpFix;
import net.sourceforge.dvb.projectx.parser.StripAudio;
import net.sourceforge.dvb.projectx.parser.StripRelook;
import net.sourceforge.dvb.projectx.parser.StripMedion;
import net.sourceforge.dvb.projectx.parser.CommonParsing;

/**
 *
 */
public class FileProperties extends JFrame {

	private String title = Resource.getString("General.FileProperties");

	private ComboBoxIndexListener _ComboBoxIndexListener = new ComboBoxIndexListener();
	private ComboBoxItemListener _ComboBoxItemListener = new ComboBoxItemListener();
	private CheckBoxListener _CheckBoxListener = new CheckBoxListener();
	private TextFieldListener _TextFieldListener = new TextFieldListener();
	private TextFieldKeyListener _TextFieldKeyListener = new TextFieldKeyListener();

	private XInputFile inputfile = null;

	private JTextArea area = null;
	private JLabel length = null;
	private JSlider slider = null;
	private View view;

	private Color bg_color = new Color(0, 150, 0);

	private int loadSizeForward = 1024000;
	private int collection_number = 0;
	private int collection_index = 0;
	private int tmp_value = 0;

	private boolean silent = false;

	private Preview Preview = new Preview(loadSizeForward);

	/**
	 * Constructor
	 */
	public FileProperties()
	{
		addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				close();
			}
		});

		JPanel container = new JPanel();
		container.setLayout(new BorderLayout());

		buildMenu();

		container.add(buildMainPanel());
		getContentPane().add(container);

		setTitle(title);
		setBounds(200, 100, 660, 360);
		setResizable(false);

		UIManager.addPropertyChangeListener(new UISwitchListener(getRootPane()));
	}

	/**
	 *
	 */
	protected void buildMenu()
	{
		JMenuBar menuBar = new JMenuBar();

		menuBar.add(buildFileMenu());
		menuBar.add(buildEditMenu());
		menuBar.add(buildPreprocessMenu());
		menuBar.add(buildStreamMenu());

		setJMenuBar(menuBar);
	}

	/**
	 *
	 */
	protected JMenu buildFileMenu()
	{
		JMenu menu = new JMenu();
		CommonGui.localize(menu, "Common.File");

		menu.addSeparator();

		JMenuItem close = new JMenuItem();
		CommonGui.localize(close, "Common.Close");
		close.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.ALT_MASK));
		close.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				close();
			}
		});

		JMenuItem item_2 = new JMenuItem(Resource.getString("popup.rename"));
		item_2.setActionCommand("rename");

		JMenuItem item_3 = new JMenuItem(Resource.getString("popup.openhex"));
		item_3.setActionCommand("viewAsHex");

		menu.add(item_2);
		menu.addSeparator();
		menu.add(item_3);
		menu.addSeparator();
		menu.add(close);

		item_2.addActionListener(_MenuListener);
		item_3.addActionListener(_MenuListener);

		return menu;
	}

	/**
	 *
	 */
	protected JMenu buildEditMenu()
	{
		JMenu menu = new JMenu();
		CommonGui.localize(menu, "Common.Edit");

		JMenuItem item_1 = new JMenuItem(Resource.getString("popup.changeTimestamp"));
		item_1.setActionCommand("changeTimestamp");

		JMenuItem item_2 = new JMenuItem(Resource.getString("popup.patchbasics"));
		item_2.setActionCommand("editBasics");

		menu.add(item_1);
		menu.addSeparator();
		menu.add(item_2);

		item_1.addActionListener(_MenuListener);
		item_2.addActionListener(_MenuListener);

		return menu;
	}

	/**
	 *
	 */
	protected JMenu buildPreprocessMenu()
	{
		JMenu menu = new JMenu();
		CommonGui.localize(menu, "Common.Preprocess");

		JMenuItem item_1 = new JMenuItem(Resource.getString("popup.fixHpAc3"));
		item_1.setActionCommand("fixHpAc3");

		JMenuItem item_2 = new JMenuItem(Resource.getString("popup.stripAudio"));
		item_2.setActionCommand("stripAudio");

		JMenuItem item_3 = new JMenuItem("strip Relook® type 0 to separate pes..");
		item_3.setActionCommand("stripRelook");

		JMenuItem item_4 = new JMenuItem("strip Relook® type 1 to separate pes..");
		item_4.setActionCommand("stripRelook1");

		JMenuItem item_5 = new JMenuItem("strip Medion® to separate pes..");
		item_5.setActionCommand("stripMedion");


		menu.add(item_1);
		menu.addSeparator();
		menu.add(item_2);
		menu.addSeparator();
		menu.add(item_3);
		menu.add(item_4);
		menu.addSeparator();
		menu.add(item_5);

		item_1.addActionListener(_MenuListener);
		item_2.addActionListener(_MenuListener);
		item_3.addActionListener(_MenuListener);
		item_4.addActionListener(_MenuListener);
		item_5.addActionListener(_MenuListener);

		return menu;
	}

	/**
	 *
	 */
	protected JMenu buildStreamMenu()
	{
		JMenu menu = new JMenu(Resource.getString("popup.assignStreamType"));

		Object[] objects = Keys.ITEMS_FileTypes;

		for (int i = 0; i <= objects.length; i++)
		{
			JMenuItem item = new JMenuItem(i == objects.length ? Resource.getString("popup.automatic") : objects[i].toString());
			item.setActionCommand("assignStreamtype");
			item.addActionListener(_MenuListener);

			if (i == objects.length)
				menu.addSeparator();

			menu.add(item);
		}

		return menu;
	}


	/**
	 *
	 */
	protected JPanel buildMainPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

		JPanel panel_1 = new JPanel();
		panel_1.setLayout( new ColumnLayout() );
		panel_1.setBorder( BorderFactory.createTitledBorder("Preview") );

		panel_1.add(view = new View());

		panel_1.add(Box.createRigidArea(new Dimension(1, 5)));

		slider = new JSlider();
		slider.setPreferredSize(new Dimension(160, 30));
		slider.setMaximumSize(new Dimension(160, 30));
		slider.setMinimumSize(new Dimension(160, 30));
		slider.setMaximum(100);
		slider.setValue(0);

		slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e)
			{
				if (!silent)
					scanFile(32L * slider.getValue());
			}
		});

		panel_1.add(slider);

		panel_1.add(Box.createRigidArea(new Dimension(1, 5)));

		length = new JLabel("ScanPos: ");

		panel_1.add(length);

		//file info
		JPanel panel_2 = new JPanel();
		panel_2.setLayout( new ColumnLayout() );
		panel_2.setBorder( BorderFactory.createTitledBorder("File Info") );

		area = new JTextArea();
		area.setEditable(true);

		JScrollPane scroll = new JScrollPane();
		scroll.setPreferredSize(new Dimension(460, 280));
		scroll.setMaximumSize(new Dimension(460, 280));
		scroll.setMinimumSize(new Dimension(460, 280));
		scroll.setViewportView(area);
		JViewport viewport = scroll.getViewport();

		panel_2.add(scroll);

		panel.add(panel_1);
		panel.add(panel_2);

		return panel;
	}

	/**
	 *
	 */
	public void close()
	{ 
		inputfile = null;

		Common.getGuiInterface().showActiveCollection(collection_number);

		dispose();
	}

	/**
	 *
	 */
	public void open(XInputFile xInputFile, int value_1, int value_2)
	{ 
		collection_number = value_1;
		collection_index = value_2;

		inputfile = xInputFile;

		silent = true;
		slider.setMaximum((int)(inputfile.length() / 32L));
		silent = false;

		tmp_value = (int)(inputfile.getStreamInfo().getScanPosition() / 32L);

		if (tmp_value != slider.getValue())
			slider.setValue(tmp_value);

		else
			scanFile();

		show();
	}

	/**
	 *
	 */
	private void setPanelTitle()
	{ 
		setTitle(title + ": ID " + inputfile.getFileID() + " - '" + inputfile.getName() + "'");
	}

	/**
	 *
	 */
	private void scanFile()
	{ 
		setPanelTitle();
		scanFile(inputfile.getStreamInfo().getScanPosition());
	}

	/**
	 *
	 */
	private void scanFile(long value)
	{ 
		setPosInfo(value);

		Common.getScanClass().getStreamInfo(inputfile, value);

		setFileInfo();

		previewFile(value);
	}

	/**
	 *
	 */
	private void scanSpecFileType(int type)
	{ 
		long value = inputfile.getStreamInfo().getScanPosition();

		setPosInfo(value);

		Common.getScanClass().getStreamInfo(inputfile, value, type);

		setFileInfo();

		previewFile(value);
	}

	/**
	 *
	 */
	private void setPosInfo(long value)
	{ 
		inputfile.getStreamInfo().setScanPosition(value);

		length.setText("ScanPos: " + Common.formatNumber(value));
	}

	/**
	 *
	 */
	private void setFileInfo()
	{ 
		area.setText(inputfile.getStreamInfo().getFullInfo());
	}

	/**
	 *
	 */
	private void previewFile(long value)
	{ 
		switch (inputfile.getStreamInfo().getStreamType())
		{
		case CommonParsing.PES_AV_TYPE:
		case CommonParsing.MPEG1PS_TYPE:
		case CommonParsing.MPEG2PS_TYPE:
		case CommonParsing.PVA_TYPE:
		case CommonParsing.TS_TYPE:
		case CommonParsing.ES_MPV_TYPE:
			long position = Preview.previewFile(inputfile, value, loadSizeForward, Common.getSettings().getBooleanProperty(Keys.KEY_Preview_AllGops), Common.getSettings().getBooleanProperty(Keys.KEY_Preview_fastDecode), Common.getSettings().getIntProperty(Keys.KEY_Preview_YGain));
			view.setImage(Common.getMpvDecoderClass().getScaledCutImage());
			break;

		case CommonParsing.PES_MPA_TYPE:
		case CommonParsing.PES_PS1_TYPE:
			view.setImage("PES Audio");
			break;

		case CommonParsing.ES_MPA_TYPE:
		case CommonParsing.ES_AC3_TYPE:
		case CommonParsing.ES_AC3_A_TYPE:
		case CommonParsing.ES_DTS_TYPE:
		case CommonParsing.ES_DTS_A_TYPE:
		case CommonParsing.ES_RIFF_TYPE:
		case CommonParsing.ES_cRIFF_TYPE:
			view.setImage("RAW Audio");
			break;

		case CommonParsing.ES_SUP_TYPE:
			view.setImage("RAW Subpicture");
			break;

		case CommonParsing.Unsupported:
		default:
			view.setImage("Unknown");
			break;
		}

	}

	/**
	 *
	 */
	private ActionListener _MenuListener = new ActionListener() {
		public void actionPerformed(ActionEvent e)
		{
			String actName = e.getActionCommand();

			/**
			 *
			 */
			if (actName.equals("rename"))
			{
				try {
					if (inputfile.rename())
					{
						scanFile();

						Common.getGuiInterface().showActiveCollection(collection_number);
					}

					//if (((XInputFile) collection.getInputFile(index)).rename())
					//	reloadInputDirectories();

					toFront();

				} catch (IOException ioe) {}

				//updateCollectionTable(collection.getCollectionAsTable());
				//updateCollectionPanel(collection_number);
			}

			/**
			 *
			 */
			else if (actName.equals("changeTimestamp"))
			{
				if (CommonGui.getUserConfirmation("really update the timestamp of '" + inputfile.getName() + "' ?"))
				{
					inputfile.setLastModified();

					scanFile();

					Common.getGuiInterface().showActiveCollection(collection_number);
				}
				//updateCollectionTable(collection.getCollectionAsTable());
				//updateCollectionPanel(collection_number);

				toFront();
			}

			/**
			 *
			 */
			else if (actName.equals("viewAsHex"))
			{
				if (inputfile.exists())
					new HexViewer().view(inputfile.getNewInstance());
			}

			/**
			 *
			 */
			else if (actName.equals("editBasics"))
			{
				if (CommonGui.getPatchDialog().entry(inputfile))
				{
					scanFile();

					Common.getGuiInterface().showActiveCollection(collection_number);
				//	getScanInfo(xInputFile, xInputFile.getStreamInfo().getStreamType());
				}

				toFront();
			}

			/**
			 *
			 */
			else if (actName.equals("assignStreamtype"))
			{
				Object[] items = Keys.ITEMS_FileTypes;
				String str = ((JMenuItem) e.getSource()).getText();

				for (int i = 0; i < items.length; i++)
				{
					if (str.equals(items[i].toString()))
					{
						inputfile.getStreamInfo().setStreamType(i);

						scanSpecFileType(i);

						Common.getGuiInterface().showActiveCollection(collection_number);

						return;
					}
				}

				scanFile();

				Common.getGuiInterface().showActiveCollection(collection_number);
			}

			/**
			 *
			 */
			else if (actName.equals("stripRelook"))
			{
				stripRelook(0);
			}

			/**
			 *
			 */
			else if (actName.equals("stripRelook1"))
			{
				stripRelook(1);
			}

			/**
			 *
			 */
			else if (actName.equals("stripMedion"))
			{
				stripMedion();
			}

			/**
			 *
			 */
			else if (actName.equals("stripAudio"))
			{
				if (inputfile.exists() && inputfile.getStreamInfo().getStreamType() == CommonParsing.ES_RIFF_TYPE && CommonGui.getUserConfirmation("really process '" + inputfile.getName() + "' ?"))
				{
					StripAudio stripAudio = new StripAudio();

					Common.setOSDMessage("strip audio data...");

					XInputFile xInputFile = stripAudio.process(inputfile);

					JobCollection collection = Common.getCollection(collection_number);

					if (xInputFile != null)
					{
						collection.removeInputFile(collection_index);
						collection.addInputFile(collection_index, xInputFile);

						Common.getGuiInterface().showActiveCollection(collection_number);
					}
				}

				toFront();
			}
		}

		/**
		 *
		 */
		private void stripRelook(int type)
		{
			if (inputfile.exists() && inputfile.getStreamInfo().getStreamType() == CommonParsing.PES_AV_TYPE && CommonGui.getUserConfirmation("really process '" + inputfile.getName() + "' ?"))
			{
				StripRelook stripRelook = new StripRelook(type);

				Common.setOSDMessage("strip Relook® data, type " + type + "...");

				JobCollection collection = Common.getCollection(collection_number);

				XInputFile[] xif = stripRelook.process(inputfile.getNewInstance(), collection.getOutputDirectory());

				if (xif != null)
				{
					collection.removeInputFile(collection_index);

					for (int i = 0, j = collection_index; i < xif.length; i++)
					{
						if (xif[i] != null)
							collection.addInputFile(j++, xif[i]);
					}

					Common.getGuiInterface().showActiveCollection(collection_number);
				}
			}

			toFront();
		}

		/**
		 *
		 */
		private void stripMedion()
		{
			if (inputfile.exists() && inputfile.getStreamInfo().getStreamType() == CommonParsing.PES_AV_TYPE && CommonGui.getUserConfirmation("really process '" + inputfile.getName() + "' ?"))
			{
				StripMedion stripMedion = new StripMedion();

				Common.setOSDMessage("strip Medion® data...");

				JobCollection collection = Common.getCollection(collection_number);

				XInputFile[] xif = stripMedion.process(inputfile.getNewInstance(), collection.getOutputDirectory());

				if (xif != null)
				{
					collection.removeInputFile(collection_index);

					for (int i = 0, j = collection_index; i < xif.length; i++)
					{
						if (xif[i] != null)
							collection.addInputFile(j++, xif[i]);
					}

					Common.getGuiInterface().showActiveCollection(collection_number);
				}
			}

			toFront();
		}
	};

	/**
	 *
	 */
	private class View extends JPanel {
	
		private int width = 160;
		private int height = 90;
		private Image image;
		private MemoryImageSource source;
		private int[] image_data;
		private String alternative = "";

		public View()
		{
			image_data = new int[width * height];
			source = new MemoryImageSource(width, height, image_data, 0, width);
			source.setAnimated(true);
			image = createImage(source);

			setPreferredSize(new Dimension(160, 90));
			setMaximumSize(new Dimension(160, 90));
			setMinimumSize(new Dimension(160, 90));

			setBackground(Color.black);

		}

		public void setImage(int[] new_image_data)
		{
			alternative = "";
			System.arraycopy(new_image_data, 0, image_data, 0, new_image_data.length);

			inputfile.getStreamInfo().setThumbnail(image_data);

			source.newPixels();
			repaint();
		}

		public void setImage(String str)
		{
			alternative = str;
			Arrays.fill(image_data, 0);

			source.newPixels();
			repaint();
		}

		public void paint(Graphics g)
		{
			g.setColor(bg_color);
			g.fillRect(0, 0, 160, 90);

			g.drawImage(image, 0, 0, this);

			g.setColor(Color.white);
			g.drawString(alternative, 10, 30);
		}

	}
}
