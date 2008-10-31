/*
 * @(#)ProcessWindow
 *
 * Copyright (c) 2005-2008 by dvb.matt, All Rights Reserved. 
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
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.KeyEvent;

import java.io.StringWriter;
import java.io.PrintWriter;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JTextArea;
import javax.swing.JMenuItem;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.UIManager;
import javax.swing.SwingConstants;
import javax.swing.JComboBox;
import javax.swing.JRadioButton;
import javax.swing.JProgressBar;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.KeyStroke;

import javax.swing.ButtonGroup;

import net.sourceforge.dvb.projectx.gui.UISwitchListener;
import net.sourceforge.dvb.projectx.gui.ColumnLayout;
import net.sourceforge.dvb.projectx.gui.CommonGui;

import net.sourceforge.dvb.projectx.common.Keys;
import net.sourceforge.dvb.projectx.common.Resource;
import net.sourceforge.dvb.projectx.common.Common;

import net.sourceforge.dvb.projectx.parser.CommonParsing;

import net.sourceforge.dvb.projectx.gui.CheckBoxListener;

import net.sourceforge.dvb.projectx.gui.BitrateMonitor;

/**
 *
 */
public class ProcessWindow extends JFrame {

	private String title = Resource.getString("ProcessWindow.Title");

	private JLabel ttxheaderLabel;
	private JLabel ttxvpsLabel;
	private JLabel OffsetLabel;
	private JLabel StatusLabel;
	private JTextArea TextArea;
	private JViewport viewport;
	private JComboBox extractComboBox;
	private JProgressBar Progressbar;

	private String SplitPart;

	private BitrateMonitor Monitor;

	private CheckBoxListener _CheckBoxListener = new CheckBoxListener();

	ActionListener _BoxListener = new ActionListener() {
		public void actionPerformed(ActionEvent e)
		{
			String actName = e.getActionCommand();

			JCheckBoxMenuItem box = (JCheckBoxMenuItem) e.getSource();
			Common.getSettings().setBooleanProperty(actName, box.getState());
		}
	};

	/**
	 * Constructor
	 */
	public ProcessWindow()
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

		setBounds(200, 100, 740, 480);

		buildMenu();

		container.add(buildHeadPanel(), BorderLayout.NORTH);
		container.add(buildLogWindowPanel(), BorderLayout.CENTER);
		container.add(buildProgressPanel(), BorderLayout.SOUTH);

		getContentPane().add(container);

		setTitle(title);

		UIManager.addPropertyChangeListener(new UISwitchListener(getRootPane()));
	}

	/**
	 * 
	 */
	public void close()
	{ 
		focusToText();
		dispose();
	}

	/**
	 * 
	 */
	public void iconify()
	{
		focusToText();
		setState(ICONIFIED);
	}

	/**
	 * 
	 */
	private void focusToText()
	{
		TextArea.requestFocus();
	}

	/**
	 * 
	 */
	private void saveLog()
	{ 
		String str = CommonGui.getUserInput(this, "save log", "save logfile", Common.getSettings().getProperty(Keys.KEY_OutputDirectory) + System.getProperty("file.separator") + "pjx_log.txt");

		if (str != null && str.length() > 0)
		{
			try {
				PrintWriter pw = new PrintWriter(new FileOutputStream(str));

				pw.print(getLogContent());
				pw.close();

			} catch (IOException e) { 

				Common.setExceptionMessage(e);
			}
		}

		toFront();
	}

	/**
	 * 
	 */
	private String getLogContent()
	{ 
		return TextArea.getText();
	}

	/**
	 *
	 */
	protected void buildMenu()
	{
		JMenuBar menuBar = new JMenuBar();

		menuBar.add(buildFileMenu());
		menuBar.add(buildEditMenu());
		menuBar.add(buildMessageMenu());
		menuBar.add(buildPreferencesMenu());

		setJMenuBar(menuBar);
	}

	/**
	 *
	 */
	protected JMenu buildFileMenu()
	{
		JMenu fileMenu = new JMenu();
		CommonGui.localize(fileMenu, "Common.File");

		String[][] objects = {
			Keys.KEY_DebugLog,
			Keys.KEY_NormalLog
		};

		for (int i = 0; i < objects.length; i++)
		{
			JCheckBoxMenuItem box = new JCheckBoxMenuItem(Resource.getString(objects[i][0]));
			box.setToolTipText(Resource.getString(objects[i][0] + Keys.KEY_Tip));
			box.setActionCommand(objects[i][0]);
			box.setState(Common.getSettings().getBooleanProperty(objects[i]));
			box.addActionListener(_BoxListener);

			fileMenu.add(box);
		}

		fileMenu.addSeparator();

		JCheckBoxMenuItem closeOnEnd = new JCheckBoxMenuItem(Resource.getString(Keys.KEY_closeOnEnd[0]));
		closeOnEnd.setToolTipText(Resource.getString(Keys.KEY_closeOnEnd[0] + Keys.KEY_Tip));
		closeOnEnd.setActionCommand(Keys.KEY_closeOnEnd[0]);
		closeOnEnd.setState(Common.getSettings().getBooleanProperty(Keys.KEY_closeOnEnd));
		closeOnEnd.addActionListener(_BoxListener);
		
		fileMenu.add(closeOnEnd);
		fileMenu.addSeparator();

		JMenuItem kill = new JMenuItem();
		CommonGui.localize(kill, "Common.KillProcess");
		kill.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, ActionEvent.ALT_MASK));
		kill.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				Common.killMainProcess();
			}
		});

		fileMenu.add(kill);
		fileMenu.addSeparator();

		JMenuItem save = new JMenuItem();
		CommonGui.localize(save, "Common.SaveAs");
		save.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, ActionEvent.ALT_MASK));
		save.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				saveLog();
			}
		});

		fileMenu.add(save);
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
	protected JMenu buildEditMenu()
	{
		JMenu editMenu = new JMenu();
		CommonGui.localize(editMenu, "Common.Edit");

		String[][] objects = {
			Keys.KEY_minimizeMainFrame,
			Keys.KEY_hideProcessWindow,
			Keys.KEY_showSubpictureWindow,
			Keys.KEY_useAllCollections,
			Keys.KEY_ExportPanel_createSubDirNumber,
			Keys.KEY_ExportPanel_createSubDirName,
			Keys.KEY_ExportPanel_createSubDirVdr,
			Keys.KEY_enablePostProcessing,
			Keys.KEY_simpleMPG,
			Keys.KEY_enhancedPES,
			Keys.KEY_useGOPEditor
		};

		for (int i = 0, sep_offs = 3; i < objects.length; i++)
		{
			JCheckBoxMenuItem box = new JCheckBoxMenuItem(Resource.getString(objects[i][0]));
			box.setToolTipText(Resource.getString(objects[i][0] + Keys.KEY_Tip));
			box.setActionCommand(objects[i][0]);
			box.setState(Common.getSettings().getBooleanProperty(objects[i]));
			box.addActionListener(_BoxListener);

			if (i == sep_offs || i == sep_offs + 1 || i == sep_offs + 4 || i == sep_offs + 5 || i == sep_offs + 7)
				editMenu.addSeparator();

			editMenu.add(box);
		}

		return editMenu;
	}

	/**
	 *
	 */
	protected JMenu buildMessageMenu()
	{
		JMenu messageMenu = new JMenu();
		CommonGui.localize(messageMenu, "Common.Messages");

		String[][] objects = {
			Keys.KEY_MessagePanel_Msg1,
			Keys.KEY_MessagePanel_Msg2,
			Keys.KEY_MessagePanel_Msg3,
			Keys.KEY_MessagePanel_Msg8,
			Keys.KEY_MessagePanel_Msg5,
			Keys.KEY_MessagePanel_Msg6,
			Keys.KEY_MessagePanel_Msg7,
			Keys.KEY_MessagePanel_Msg4
		};

		for (int i = 0; i < objects.length; i++)
		{
			JCheckBoxMenuItem box = new JCheckBoxMenuItem(Resource.getString(objects[i][0]));
			box.setToolTipText(Resource.getString(objects[i][0] + Keys.KEY_Tip));
			box.setActionCommand(objects[i][0]);
			box.setState(Common.getSettings().getBooleanProperty(objects[i]));
			box.addActionListener(_BoxListener);

			if (i == 3 || i == 4 || i == 7)
				messageMenu.addSeparator();

			messageMenu.add(box);
		}

		return messageMenu;
	}

	/**
	 *
	 */
	protected JMenu buildPreferencesMenu()
	{
		JMenu preferencesMenu = new JMenu();
		CommonGui.localize(preferencesMenu, "Common.Preferences");

		JMenuItem preferences = new JMenuItem();
		preferences.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
		CommonGui.localize(preferences, "Common.Preferences");
		preferences.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				Common.getGuiInterface().showPreSettings();
			}
		});

		preferencesMenu.add(preferences);

		JMenuItem coll_preferences = new JMenuItem();
		coll_preferences.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK));
		CommonGui.localize(coll_preferences, "General.CollectionProperties");
		coll_preferences.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				CommonGui.getCollectionProperties().open(Common.getCollection(), Common.getActiveCollection());
			}
		});

		preferencesMenu.add(coll_preferences);

		preferencesMenu.addSeparator();

		JCheckBoxMenuItem priority = new JCheckBoxMenuItem(Resource.getString(Keys.KEY_ConversionModePriority[0]));
		priority.setToolTipText(Resource.getString(Keys.KEY_ConversionModePriority[0] + Keys.KEY_Tip));
		priority.setActionCommand(Keys.KEY_ConversionModePriority[0]);
		priority.setState(Common.getSettings().getBooleanProperty(Keys.KEY_ConversionModePriority));
		priority.addActionListener(_BoxListener);

		preferencesMenu.add(priority);

		return preferencesMenu;
	}

	/**
	 *
	 */
	protected JPanel buildHeadPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());

		panel.add(buildProcessPanel(), BorderLayout.NORTH);
		panel.add(buildActionPanel(), BorderLayout.SOUTH);

		return panel;
	}

	/**
	 *
	 */
	protected JPanel buildProcessPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
		panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), Resource.getString("MainPanel.Process")));

		JButton doitButton = new JButton(CommonGui.loadIcon("start.gif"));
		doitButton.setMnemonic('s');
		doitButton.setMaximumSize(new Dimension(60, 36));
		doitButton.setPreferredSize(new Dimension(60, 36));
		doitButton.setToolTipText(Resource.getString("button.go.Tip"));
		doitButton.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				if (!Common.isRunningProcess())
				{
					if (Common.isCollectionListEmpty())
						return;

					Common.setRunningProcess(true);

					extractComboBox.removeAllItems();

					CommonParsing.setPvaPidToExtract(-1);

					Common.startMainProcess();
				}

				else
				{
					if (!CommonParsing.isProcessPausing())
						Common.setMessage(Resource.getString("golistener.msg.paused"), true, 0xFFFFE0); 

					else
						Common.setMessage(Resource.getString("golistener.msg.resumed"), true, 0xFFFFFF); 

					CommonParsing.setProcessPausing(!CommonParsing.isProcessPausing());
				}

				focusToText();
			}
		});

		panel.add(doitButton);

		panel.add(Box.createRigidArea(new Dimension(4, 1)));

		JButton breakButton = new JButton(CommonGui.loadIcon("stop.gif"));
		breakButton.setMnemonic('c');
		breakButton.setMaximumSize(new Dimension(36, 36));
		breakButton.setPreferredSize(new Dimension(36, 36));
		breakButton.setToolTipText(Resource.getString("button.c.Tip"));
		breakButton.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				Common.breakMainProcess();
			}
		});

		panel.add(breakButton);

		panel.add(Box.createRigidArea(new Dimension(10, 1)));

		JButton scanButton = new JButton(CommonGui.loadIcon("scan.gif"));
		scanButton.setMnemonic('i');
		scanButton.setMaximumSize(new Dimension(36, 36));
		scanButton.setPreferredSize(new Dimension(36, 36));
		scanButton.setToolTipText(Resource.getString("button.i.Tip"));
		scanButton.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				if (!Common.startProcess())
					return;

				extractComboBox.removeAllItems();

				CommonParsing.setInfoScan(true);

				Common.startMainProcess();
			}
		});

		panel.add(scanButton);

		panel.add(Box.createRigidArea(new Dimension(4, 1)));

		JButton extractButton = new JButton(CommonGui.loadIcon("extract.gif"));
		extractButton.setMnemonic('e');
		extractButton.setMaximumSize(new Dimension(36, 36));
		extractButton.setPreferredSize(new Dimension(36, 36));
		extractButton.setToolTipText(Resource.getString("button.e.Tip"));
		extractButton.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				if (Common.isRunningProcess())
					return;

				if (extractComboBox.getItemCount() == 0)
					return;

				Common.setRunningProcess(true);

				CommonParsing.setPvaPidExtraction(true);
				CommonParsing.setPvaPidToExtract(Integer.parseInt(extractComboBox.getSelectedItem().toString(), 16));

				Common.setMessage(Resource.getString("golistener.msg.extracting") + extractComboBox.getSelectedItem().toString() + "...");

				Common.startMainProcess();
			}
		});

		panel.add(extractButton);

		panel.add(Box.createRigidArea(new Dimension(4, 1)));

		extractComboBox = new JComboBox();
		extractComboBox.setMaximumSize(new Dimension(44, 26));
		extractComboBox.setPreferredSize(new Dimension(44, 26));
		extractComboBox.setMaximumRowCount(5);

		panel.add(extractComboBox);

		panel.add(Box.createRigidArea(new Dimension(10, 1)));

		panel.add(buildStatusPanel());

		return panel;
	}

	/**
	 *
	 */
	protected JPanel buildStatusPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

		Monitor = new BitrateMonitor();
		Monitor.setToolTipText(Resource.getString("MainPanel.BitrateMonitor.Tip"));

		panel.add(Monitor);

		JPanel status_7 = new JPanel(new BorderLayout());
		status_7.setBorder(BorderFactory.createLoweredBevelBorder());
		status_7.add(Monitor);
		status_7.setPreferredSize(new Dimension(114, 36));
		status_7.setMaximumSize(new Dimension(114, 36));

		panel.add(status_7);


		final JLabel troughput = new JLabel();
		final JLabel collLabel = new JLabel();
		final JLabel FpsLabel = new JLabel();

		final JLabel outSize = new JLabel();
		outSize.setToolTipText(Resource.getString("MainPanel.writtenMB.Tip"));

		/**
		 *
		 */
		JPanel status_3 = new JPanel(new BorderLayout());
		status_3.setBorder(BorderFactory.createLoweredBevelBorder());
		status_3.setPreferredSize(new Dimension(100, 18));
		status_3.setMaximumSize(new Dimension(100, 18));
		status_3.setBackground(new Color(225, 225, 250));
		status_3.add(collLabel);

		JPanel status_4 = new JPanel(new BorderLayout());
		status_4.setBorder(BorderFactory.createLoweredBevelBorder());
		status_4.setPreferredSize(new Dimension(100, 18));
		status_4.setMaximumSize(new Dimension(100, 18));
		status_4.add(outSize);

		JPanel panel_2 = new JPanel();
		panel_2.setLayout(new BoxLayout(panel_2, BoxLayout.Y_AXIS));
		panel_2.add(status_3);
		panel_2.add(status_4);

		panel.add(panel_2);

		/**
		 *
		 */
		JPanel status_1 = new JPanel(new BorderLayout());
		status_1.setBorder(BorderFactory.createLoweredBevelBorder());
		status_1.setPreferredSize(new Dimension(100, 18));
		status_1.setMaximumSize(new Dimension(100, 18));
		status_1.add(troughput);

		JPanel status_2 = new JPanel(new BorderLayout());
		status_2.setBorder(BorderFactory.createLoweredBevelBorder());
		status_2.setPreferredSize(new Dimension(100, 18));
		status_2.setMaximumSize(new Dimension(100, 18));
		status_2.add(FpsLabel);

		JPanel panel_1 = new JPanel();
		panel_1.setLayout(new BoxLayout(panel_1, BoxLayout.Y_AXIS));
		panel_1.add(status_1);
		panel_1.add(status_2);

		panel.add(panel_1);

		/**
		 *
		 */
		OffsetLabel = new JLabel(Resource.getString("MainPanel.AudioVideoOffset"));
		OffsetLabel.setToolTipText(Resource.getString("MainPanel.AudioVideoOffset.Tip"));

		StatusLabel = new JLabel(Resource.getString("MainPanel.nonVideoExportStatus"));
		StatusLabel.setToolTipText(Resource.getString("MainPanel.nonVideoExportStatus.Tip"));

		JPanel status_5 = new JPanel(new BorderLayout());
		status_5.setBorder(BorderFactory.createLoweredBevelBorder());
		status_5.setPreferredSize(new Dimension(100, 18));
		status_5.setMaximumSize(new Dimension(100, 18));
		status_5.add(OffsetLabel);

		JPanel status_6 = new JPanel(new BorderLayout());
		status_6.setBorder(BorderFactory.createLoweredBevelBorder());
		status_6.setPreferredSize(new Dimension(100, 18));
		status_6.setMaximumSize(new Dimension(100, 18));
		status_6.add(StatusLabel);

		JPanel panel_3 = new JPanel();
		panel_3.setLayout(new BoxLayout(panel_3, BoxLayout.Y_AXIS));
		panel_3.add(status_5);
		panel_3.add(status_6);

		panel.add(panel_3);


		/**
		 *
		 */
		final JLabel TimeLabel = new JLabel();
		TimeLabel.setToolTipText("process time elapsed");

		final JLabel ErrorLabel = new JLabel();
		ErrorLabel.setToolTipText("warnings/error counter");

		JPanel status_8 = new JPanel(new BorderLayout());
		status_8.setBorder(BorderFactory.createLoweredBevelBorder());
		status_8.setPreferredSize(new Dimension(60, 18));
		status_8.setMaximumSize(new Dimension(60, 18));
		status_8.add(TimeLabel);

		JPanel status_9 = new JPanel(new BorderLayout());
		status_9.setBorder(BorderFactory.createLoweredBevelBorder());
		status_9.setPreferredSize(new Dimension(60, 18));
		status_9.setMaximumSize(new Dimension(60, 18));
		status_9.add(ErrorLabel);

		JPanel panel_4 = new JPanel();
		panel_4.setLayout(new BoxLayout(panel_4, BoxLayout.Y_AXIS));
		panel_4.add(status_8);
		panel_4.add(status_9);

		panel.add(panel_4);


		class Clock implements Runnable {
			private Thread clockThread = null;

			private String last_value = "";
			private String last_exp = "";
			private String last_fps = "";
			private int last_coll = 0;

			public void start()
			{
				if (clockThread == null)
				{
					clockThread = new Thread(this, "Clock_2");
					clockThread.setPriority(Thread.MIN_PRIORITY);
					clockThread.start();
				}
			}

			public void run()
			{
				Thread myThread = Thread.currentThread();

				while (clockThread == myThread)
				{
					update();

					try {

						Thread.sleep(1000);
					} catch (InterruptedException e) {}
				}
			}

			private void update()
			{
				updateFpsLabel();
				updateDataTroughputLabel();
				updateExportedSizeLabel();
				updateCollectionLabel();
				updateTimeLabel();
			}

			private void updateFpsLabel()
			{
				String str = Common.getFps();

				if (!last_fps.equals(str))
					FpsLabel.setText(str + " fps");

				last_fps = str;
			}

			private void updateDataTroughputLabel()
			{
				String str = Common.getDataTroughput();

				if (!last_value.equals(str))
					troughput.setText(str + " kB/s");

				last_value = str;
			}

			private void updateExportedSizeLabel()
			{
				String str = Common.getExportedSize();

				if (!last_exp.equals(str))
					outSize.setText(str);

				last_exp = str;
			}

			private void updateCollectionLabel()
			{
				int val = Common.isRunningProcess() ? Common.getProcessedCollection() : Common.getActiveCollection();

				if (last_coll != val)
					collLabel.setText("Collection: " + (val < 0 ? "-" : String.valueOf(val) + "/" + String.valueOf(Common.getCollectionListSize() - 1)));

				last_coll = val;
			}

			private void updateTimeLabel()
			{
				TimeLabel.setText(Common.formatTime_4(Common.getProcessTime()));
				ErrorLabel.setText(String.valueOf(Common.getErrorCount()));
			}

			public void stop()
			{
				clockThread = null;
			}
		}

		new Clock().start();

		return panel;
	}


	/**
	 *
	 */
	protected JPanel buildActionPanel()
	{
		final Object[] objects = Keys.ITEMS_ConversionMode;

		int index = Common.getSettings().getIntProperty(Keys.KEY_ConversionMode);

		ActionListener _BoxListener = new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				JCheckBox box = (JCheckBox) e.getSource();
				String str = box.getActionCommand();

				for (int i = 0; i < objects.length; i++)
				{
					if (!str.equals(objects[i].toString()))
						continue;

					Common.getSettings().setProperty(Keys.KEY_ConversionMode[0], String.valueOf(i));
				}
			}
		};

		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
		panel.setBorder(BorderFactory.createEtchedBorder());

		panel.add(new JLabel(Resource.getString("ProcessWindowPanel.Action")));
		panel.setToolTipText(Resource.getString("MainPanel.Process.Tip"));

		panel.add(Box.createRigidArea(new Dimension(10, 1)));

		ButtonGroup group = new ButtonGroup();

		for (int i = 0; i < objects.length; i++)
		{
			JCheckBox action = new JCheckBox(objects[i].toString());
			action.setActionCommand(objects[i].toString());
			action.setSelected(i == index);
			action.addActionListener(_BoxListener);

			group.add(action);
			panel.add(action);
		}

		panel.add(Box.createRigidArea(new Dimension(10, 1)));

		return panel;
	}


	/**
	 *
	 */
	protected JPanel buildLogWindowPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		JPanel main5 = new JPanel();
		main5.setLayout(new BorderLayout());

		JPanel panel_2 = new JPanel();
		panel_2.setLayout(new BoxLayout(panel_2, BoxLayout.X_AXIS));

		JLabel icon = new JLabel(CommonGui.loadIcon("_teletext.gif"));
		panel_2.add(icon);

		panel_2.add(Box.createRigidArea(new Dimension(4, 1)));

		JCheckBox box = new JCheckBox();
		box.setToolTipText(Resource.getString(Keys.KEY_showTtxHeader[0] + Keys.KEY_Tip));
		box.setActionCommand(Keys.KEY_showTtxHeader[0]);
		box.setSelected(Common.getSettings().getBooleanProperty(Keys.KEY_showTtxHeader));
		box.addActionListener(_CheckBoxListener);
		panel_2.add(box);

		main5.add(panel_2, BorderLayout.WEST);

		ttxheaderLabel = new JLabel("");
		ttxheaderLabel.setToolTipText(Resource.getString("LogwindowPanel.showTtxHeader.Tip1"));
		main5.add(ttxheaderLabel, BorderLayout.CENTER);

		ttxvpsLabel = new JLabel("");
		ttxvpsLabel.setToolTipText(Resource.getString("LogwindowPanel.showVpsLabel.Tip"));
		main5.add(ttxvpsLabel, BorderLayout.EAST);


		JPanel main6 = new JPanel();
		main6.setLayout(new GridLayout(1, 1));
		main6.add(main5);

		TextArea = new JTextArea();
		TextArea.setEditable(true);
		TextArea.setRows(16);
		TextArea.setTabSize(12);
		TextArea.setFont(new Font("Tahoma", Font.PLAIN, 12));

		JScrollPane scrolltext = new JScrollPane();
		scrolltext.setViewportView(TextArea);
		viewport = scrolltext.getViewport();
		//viewport.setScrollMode(JViewport.BLIT_SCROLL_MODE); //enable for >= JDK1.3	
		//viewport.setScrollMode(JViewport.BACKINGSTORE_SCROLL_MODE); //alternative, enable for >= JDK1.3	
		//viewport.setBackingStoreEnabled(true); // enable for < JDK1.3 

		JPanel control04 = new JPanel(new BorderLayout());
		control04.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), Resource.getString("ProcessWindowPanel.Title")));
		control04.setAlignmentX(CENTER_ALIGNMENT);
		control04.add(main6, BorderLayout.NORTH);
		control04.add(scrolltext, BorderLayout.CENTER);

		panel.add(control04);

		return panel;
	}

	/**
	 *
	 */
	protected JPanel buildProgressPanel()
	{
		JPanel progress = new JPanel();
		progress.setLayout(new BoxLayout(progress, BoxLayout.X_AXIS));

		Progressbar = new JProgressBar();
		Progressbar.setString(Resource.getString("run.status"));
		Progressbar.setStringPainted(true);

		/**
		 * bei cli nicht nutzbar
		 */
		Progressbar.addChangeListener(new ChangeListener()
		{
			int _val = 0;

			public void stateChanged(ChangeEvent e)
			{
				if (Common.isRunningCLI())
				{
				//	System.out.print("\r" + (int)(Progressbar.getPercentComplete() * 100) + "% ");
				//	System.out.print("" + Progressbar.getString());
				}

				else
				{
					Common.setFrameTitle("" + (int)(Progressbar.getPercentComplete() * 100) + "% (" + Common.getProcessedCollection() + ") " + Common.getVersionName() + "/" + Common.getVersionDate());

					/**
					 * disabled, colorchange by percent, slows down
					 *
					int val = (int)(128 * Progressbar.getPercentComplete());

					if (val != _val)
						Progressbar.setForeground(new Color(192 - val, 64 + val, 0));

					_val = val;
					 **/
				}
			}
		});

		progress.add(Progressbar);

		return progress;
	}

	/**
	 * messages of interest, also with current systems time_index
	 *
	 * @param1 - the msg
	 * @param2 - force windows visibility
	 * @param3 - background
	 */
	public void setMessage(String msg, boolean tofront, int background)
	{
		if (background != -1)
			TextArea.setBackground(new Color(background));

		/**
		 * ensure Logmsg is visible in GUI mode
		 */
		if (tofront)
			show();

		if (msg == null)
		{
			TextArea.setText(null);
			return;
		}

		TextArea.append(Common.getLineSeparator() + msg);

		// handle a crash
		try {
			viewport.setViewPosition(new Point(0, TextArea.getHeight()));
		} catch (Exception e) {}
	}

	/**
	 *
	 */
	public void updateTtxHeader(String str)
	{
		ttxheaderLabel.setText(str);
	}

	/**
	 *
	 */
	public void updateVpsLabel(String str)
	{
		ttxvpsLabel.setText(str);
	}

	/**
	 *
	 */
	public void addPidToExtract(Object obj)
	{
		extractComboBox.addItem(obj);
	}

	/**
	 *
	 */
	public void showExportStatus(String str)
	{
		StatusLabel.setText(str);
	}

	/**
	 *
	 */
	public void showExportStatus(String str, int value)
	{
		StatusLabel.setText(str + " " + value);
	}

	/**
	 *
	 */
	public void showAVOffset(String str)
	{
		OffsetLabel.setText(str);
	}

	/**
	 *
	 */
	public void resetBitrateMonitor()
	{
		Monitor.reset();
	}

	/**
	 *
	 */
	public void updateBitrateMonitor(int value, byte[] array, String str)
	{
		Monitor.update(value, array, str);
	}

	/**
	 * progress
	 *
	 * @param1 - the msg
	 */
	public void updateProgressBar(int percent)
	{
		Progressbar.setValue(percent);
	}

	/**
	 * progress
	 *
	 * @param1 - the msg
	 */
	public void updateProgressBar(String str)
	{
		Progressbar.setString(str);
		Progressbar.setStringPainted(true);
	}
}
