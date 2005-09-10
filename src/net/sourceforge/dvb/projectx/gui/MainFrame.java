/*
 * @(#)MainFrame.java - holds main gui
 *
 * Copyright (c) 2001-2005 by dvb.matt, All rights reserved.
 * 
 * This file is part of X, a free Java based demux utility.
 * X is intended for educational purposes only, as a non-commercial test project.
 * It may not be used otherwise. Most parts are only experimental.
 * 
 *
 * This program is free software; you can redistribute it free of charge
 * and/or modify it under the terms of the GNU General Public License as published by
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
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import java.util.Date;
import java.util.List;
import java.text.DateFormat;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringTokenizer;
import java.util.Locale;

import java.util.Calendar;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.KeyStroke;
import javax.swing.MenuElement;
import javax.swing.JTable;
import javax.swing.table.TableModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import java.net.URL;

import net.sourceforge.dvb.projectx.parser.CommonParsing;
import net.sourceforge.dvb.projectx.parser.MainProcess;

import net.sourceforge.dvb.projectx.xinput.DirType;
import net.sourceforge.dvb.projectx.xinput.XInputDirectory;
import net.sourceforge.dvb.projectx.xinput.XInputFile;

import net.sourceforge.dvb.projectx.gui.FtpChooser;

import net.sourceforge.dvb.projectx.common.JobCollection;
import net.sourceforge.dvb.projectx.common.JobProcessing;
import net.sourceforge.dvb.projectx.common.Common;
import net.sourceforge.dvb.projectx.common.Resource;
import net.sourceforge.dvb.projectx.common.Keys;

import net.sourceforge.dvb.projectx.gui.AboutBox;
import net.sourceforge.dvb.projectx.gui.HexViewer;
import net.sourceforge.dvb.projectx.gui.Html;
import net.sourceforge.dvb.projectx.gui.StartUp;
import net.sourceforge.dvb.projectx.gui.UISwitchListener;
import net.sourceforge.dvb.projectx.gui.CommonGui;
import net.sourceforge.dvb.projectx.gui.PatchDialog;
import net.sourceforge.dvb.projectx.gui.ColumnLayout;
import net.sourceforge.dvb.projectx.gui.MemoryMonitor;
import net.sourceforge.dvb.projectx.gui.CollectionPanel;
import net.sourceforge.dvb.projectx.gui.ComboBoxIndexListener;
import net.sourceforge.dvb.projectx.gui.ComboBoxItemListener;
import net.sourceforge.dvb.projectx.gui.CheckBoxListener;
import net.sourceforge.dvb.projectx.gui.TextFieldListener;

import net.sourceforge.dvb.projectx.gui.CommonGui;


import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.ClipboardOwner;


/**
 *
 */
public class MainFrame extends JPanel {

	private static String frametitle = "";

	private static int GlobalReturnCode = 0;

	private static boolean SilentAction = true;

	//create empty table 
	private static Object[][] FileObjectTable = new Object[5][11];

	private static CollectionPanel collection_panel;

	private static JFrame frame = new JFrame();

//	static {
//		frame.setIconImage(Resource.loadImage("pjx_icon.gif"));
//	}

	// global boxes
	private static JComboBox comboBox_0;

	/**
	 * radio buttons for look and feels in general menu
	 */
	private JRadioButtonMenuItem lf_item[] = null; 
	

	private JTable tableView;
	private JList list1;

	private JViewport viewport;
	private JTextField outfield;

	private ComboBoxIndexListener _ComboBoxIndexListener = new ComboBoxIndexListener();
	private ComboBoxItemListener _ComboBoxItemListener = new ComboBoxItemListener();
	private CheckBoxListener _CheckBoxListener = new CheckBoxListener();
	private TextFieldListener _TextFieldListener = new TextFieldListener();

	private JPopupMenu popup;
	private JFrame autoload;

	private Thread thread = null;

	private PatchDialog patch_panel;

	/**
	 * copy fileinfo to clipboard, see popup, menulistener
	 */
	private static ClipboardOwner defaultClipboardOwner = new ClipboardObserver();

	static class ClipboardObserver implements ClipboardOwner {
		public void lostOwnership(Clipboard clipboard, Transferable contents)
		{}
	}

	/**
	 * 
	 */
	private ActionListener _BoxListener = new ActionListener() {
		public void actionPerformed(ActionEvent e)
		{
			String actName = e.getActionCommand();

			JCheckBoxMenuItem box = (JCheckBoxMenuItem) e.getSource();
			Common.getSettings().setBooleanProperty(actName, box.getState());
		}
	};

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
			if (actName.equals("sendTo3"))
			{
				if (Common.isCollectionListEmpty())
					return;

				int index = tableView.getSelectedRow();

				if (index < 0)
					return;

				try {
					Runtime.getRuntime().exec( Common.getSettings().getProperty(Keys.KEY_PostCommands_Cmd3) + " \"" + Common.getCollection(Common.getActiveCollection()).getInputFile(index).toString() + "\"");

				} catch (Exception ex) {

					Common.setExceptionMessage(ex);
				}
			}

			/**
			 *
			 */
			else if (actName.equals("add"))
			{
				CommonGui.getMainFileChooser().rescanCurrentDirectory();
				CommonGui.getMainFileChooser().setDialogType(JFileChooser.OPEN_DIALOG);
				CommonGui.getMainFileChooser().setMultiSelectionEnabled(true);

				int retval = CommonGui.getMainFileChooser().showDialog(frame, null);

				if (retval == JFileChooser.APPROVE_OPTION)
				{
					File theFiles[] = CommonGui.getMainFileChooser().getSelectedFiles();

					if (theFiles == null)
						return;

					/**
					 * adaption, if multiselection doesnt work
					 */
					if (theFiles.length == 0)
					{
						theFiles = new File[1];
						theFiles[0] = CommonGui.getMainFileChooser().getSelectedFile();
					}

					if (theFiles != null)
					{
						Common.addCollection(false);

						JobCollection collection = Common.getCollection(Common.getActiveCollection());

						/**
						 * must use getAbsolutFile to ensure right ClassType, 
						 * sometimes the returned Object.getClass 
						 * from selection is NOT of java.io.File!!
						 */
	 					for (int i = 0; i < theFiles.length; i++)
							if (theFiles[i].isFile())
								collection.addInputFile( new XInputFile(theFiles[i].getAbsoluteFile()));

						updateCollectionTable(collection.getCollectionAsTable());
						updateCollectionPanel(Common.getActiveCollection());
					}

					return;
				}
			}

			/**
			 *
			 */
			else if (actName.equals("newOutName"))
			{
				int index = tableView.getSelectedRow();

				JobCollection collection = Common.getCollection(Common.getActiveCollection());

				if (index < 0 || index >= collection.getInputFilesCount())
					return;

				String name = ((XInputFile) collection.getInputFiles()[0]).getName(); 

				String newoutname = CommonGui.getUserInput( name, Resource.getString("autoload.dialog.newOutName") + " " + name, collection.getOutputName());

				if (newoutname != null)
				{
					collection.setOutputName(newoutname);

					updateOutputField(collection);
					updateCollectionTable(collection.getCollectionAsTable());
				}
			}

			/**
			 *
			 */
			else if (actName.equals("remove"))
			{
				int[] indices = tableView.getSelectedRows();

				if (indices.length > 0)
				{
					JobCollection collection = Common.getCollection(Common.getActiveCollection());

					collection.removeInputFile(indices);

					updateCollectionTable(collection.getCollectionAsTable());
					updateCollectionPanel(Common.getActiveCollection());

					tableView.clearSelection();
				}
			}

			/**
			 *
			 */
			else if (actName.equals("rename"))
			{
				int index = tableView.getSelectedRow();

				if (index < 0 || tableView.getValueAt(index, 0) == null)
					return;

				JobCollection collection = Common.getCollection(Common.getActiveCollection());

				try {
					if (((XInputFile) collection.getInputFile(index)).rename())
						reloadInputDirectories();

				} catch (IOException ioe) {}

				updateCollectionTable(collection.getCollectionAsTable());
			}

			/**
			 *
			 */
			else if (actName.equals("changeTimestamp"))
			{
				int[] indices = tableView.getSelectedRows();

				if (indices.length == 0)
					return;

				JobCollection collection = Common.getCollection(Common.getActiveCollection());

				for (int i = 0; i < indices.length; i++)
				{
					if (tableView.getValueAt(i, 0) == null)
						continue;

					XInputFile xInputFile = (XInputFile) collection.getInputFile(i);

					if (CommonGui.getUserConfirmation("really update the timestamp of '" + xInputFile.getName() + "' ?"))
						xInputFile.setLastModified();
				}

				updateCollectionTable(collection.getCollectionAsTable());
			}

			/**
			 *
			 */
			else if (actName.equals("viewAsHex"))
			{
				int index = tableView.getSelectedRow();

				if (index < 0 || tableView.getValueAt(index, 0) == null)
					return;

				JobCollection collection = Common.getCollection(Common.getActiveCollection());

				XInputFile xInputFile = (XInputFile) collection.getInputFile(index);

				if (xInputFile.exists())
					new HexViewer().view(xInputFile);
			}

			/**
			 *
			 */
			else if (actName.equals("editBasics"))
			{
				int index = tableView.getSelectedRow();

				if (index < 0 || tableView.getValueAt(index, 0) == null)
					return;

				JobCollection collection = Common.getCollection(Common.getActiveCollection());

				XInputFile xInputFile = (XInputFile) collection.getInputFile(index);

				if (patch_panel == null)
					patch_panel = new PatchDialog(frame);

				if (patch_panel.entry(xInputFile))
					ScanInfo(xInputFile);
			}

			/**
			 *
			 */
			else if (actName.equals("clipboard"))
			{
				int index = tableView.getSelectedRow();

				if (index < 0 || tableView.getValueAt(index, 0) == null)
					return;

				JobCollection collection = Common.getCollection(Common.getActiveCollection());

				XInputFile xInputFile = (XInputFile) collection.getInputFile(index);

				try {
					Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

					if (clipboard != null)
					{
						String srcData = xInputFile.getStreamInfo().getFullInfo();
						StringSelection contents = new StringSelection(srcData);
						clipboard.setContents(contents, defaultClipboardOwner);
					}
				} catch (Exception er) {
					//just return
				}

			}

			/**
			 *
			 */
			else if (actName.equals("applyAction"))
			{
				Object[] items = Keys.ITEMS_ConversionMode;
				String str = ((JMenuItem) e.getSource()).getText();

				int val = -1;

				for (int i = 0; i < items.length; i++)
				{
					if (str.equals(items[i].toString()))
					{
						val = i;
						break;
					}
				}

				Common.getCollection(Common.getActiveCollection()).setActionType(val);
			}

			/**
			 *
			 */
			else if (actName.equals("assignStreamtype"))
			{
				int index = tableView.getSelectedRow();

				if (index < 0 || tableView.getValueAt(index, 0) == null)
					return;

				JobCollection collection = Common.getCollection(Common.getActiveCollection());

				XInputFile xInputFile = (XInputFile) collection.getInputFile(index);

				Object[] items = Keys.ITEMS_FileTypes;
				String str = ((JMenuItem) e.getSource()).getText();

				for (int i = 0; i < items.length; i++)
				{
					if (str.equals(items[i].toString()))
					{
						if (xInputFile.getStreamInfo() == null)
							ScanInfo(xInputFile);

						xInputFile.getStreamInfo().setStreamType(i);
						ScanInfo(xInputFile);

						updateCollectionTable(collection.getCollectionAsTable());

						return;
					}
				}

				xInputFile.setStreamInfo(null);
				ScanInfo(xInputFile);

				updateCollectionTable(collection.getCollectionAsTable());
			}

			/**
			 *
			 */
			else if (actName.equals("exit"))
				Common.exitApplication(0);


			/**
			 * shall support manual loading of supported URLs
			 */
			else if (actName.equals("url"))
			{
				String value = null;
				XInputFile inputValue = null;
				URL url = null;

				loop:
				while (true)
				{
					value = CommonGui.getUserInput(Resource.getString("dialog.input.url"));

					if (value == null)
						return;

					try {
						url = new URL(value);

						String protocol = url.getProtocol();

						if (protocol.equals("ftp"))
						{
							XInputDirectory xid = new XInputDirectory(url);
							XInputFile[] xif = xid.getFiles();

							for (int i = 0; i < xif.length; i++)
							{
								if ( new URL(xif[i].toString()).getFile().equals(url.getFile()) )
								{
									inputValue = xif[i];
									break loop;
								}
							}

							Common.setMessage("!> URL incorrect or not accessible: " + url.toString(), true);
							continue loop;
						}

						else if (protocol.equals("file"))
						{
							inputValue = new XInputFile(new File(url.getHost() + url.getFile()));
							break;
						}

						else
							Common.setMessage("!> Protocol not yet supported: " + protocol, true);

						return;

					} catch (Exception u1) {

						Common.setMessage("!> URL Exc: (" + value + ")");
						Common.setExceptionMessage(u1);
					}
				}

				if (inputValue == null)
					return;

				Common.addCollection(false);

				JobCollection collection = Common.getCollection(Common.getActiveCollection());

				collection.addInputFile(inputValue);

				updateCollectionTable(collection.getCollectionAsTable());
				updateCollectionPanel(Common.getActiveCollection());

				return;
			}
		}
	};


	/**
	 * Constructor of X.
	 */
	public MainFrame(StartUp startup)
	{
		frame.setBackground(new Color(200, 200, 200));

		CommonGui.setMainFrame(frame);

		initialize(startup);
	}

	/**
	 *
	 */
	public static void addCollectionAtEnd()
	{
		SilentAction = true;

		comboBox_0.addItem(String.valueOf(comboBox_0.getItemCount()));
		comboBox_0.setSelectedIndex(comboBox_0.getItemCount() - 1);

		SilentAction = false;
	}

	/**
	 *
	 */
	private void updateOutputField(JobCollection collection)
	{
		outfield.setText(collection.getOutputDirectory());

		String str;
		if ( (str = collection.getOutputName()).length() > 0)
		{
			outfield.setText(outfield.getText() + " {" + str + "}");
			outfield.setBackground(new Color(255, 225, 255));
		}

		else
			outfield.setBackground(new Color(225, 255, 225));
	}

	/**
	 * updates GUI JList collection list view
	 *
	 * param1 - array of file objects
	 */
	private void updateCollectionTable(Object[][] objects)
	{
		FileObjectTable = objects == null ? new Object[5][11] : objects;

		tableView.clearSelection();
		tableView.revalidate();
		tableView.repaint();
	}

	/**
	 * updates GUI JList autoload list view
	 *
	 * param1 - array of file objects
	 */
	private void updateAutoloadList(Object[] objects)
	{
		list1.setListData(objects);
	}

	/**
	 * 
	 */
	private void buildGUI(StartUp startup)
	{
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		showStartUpProgress(startup, 10, "Loading Menus...");

		buildMenus();

		showStartUpProgress(startup, 20, "Loading Popup Menus...");

		buildPopupMenu();

		showStartUpProgress(startup, 30, "Loading Quickload Panel...");

		buildAutoloadPanel();

		showStartUpProgress(startup, 40, "Loading Control Panel...");

		/**
		 * mid panel
		 */
		add(buildMainPanel());

		showStartUpProgress(startup, 50, "Loading Status Panel...");

		/**
		 * south panel
		 */
		add(buildStatusPanel(), BorderLayout.SOUTH);

		add(Box.createRigidArea(new Dimension(1, 5)));
	}


	/**
	 *
	 */
	protected void buildPopupMenu()
	{
		popup = new JPopupMenu(Resource.getString("popup.what"));

		JMenuItem menuitem_8 = popup.add(Resource.getString("popup.url"));
		menuitem_8.setActionCommand("url");

		JMenuItem menuitem_4 = popup.add(Resource.getString("popup.rename"));
		menuitem_4.setActionCommand("rename");

		JMenuItem menuitem_5 = popup.add(Resource.getString("popup.openhex"));
		menuitem_5.setActionCommand("viewAsHex");

		JMenuItem menuitem_6 = popup.add(Resource.getString("popup.patchbasics"));
		menuitem_6.setActionCommand("editBasics");

		popup.addSeparator();

		JMenuItem menuitem_10 = popup.add(Resource.getString("popup.changeTimestamp"));
		menuitem_10.setActionCommand("changeTimestamp");

		popup.addSeparator();

	//	JMenuItem menuitem_7 = popup.add(Resource.getString("popup.sendtocl3"));
	//	menuitem_7.setActionCommand("sendTo3");

		JMenuItem menuitem_11 = popup.add(Resource.getString("popup.copyInfoToClipboard"));
		menuitem_11.setActionCommand("clipboard");

		/**
		 *
		 */
		Object[] objects = Keys.ITEMS_FileTypes;

		JMenu streamtype = new JMenu(Resource.getString("popup.assignStreamType"));

		for (int i = 0; i <= objects.length; i++)
		{
			JMenuItem item = new JMenuItem(i == objects.length ? Resource.getString("popup.automatic") : objects[i].toString());
			item.setActionCommand("assignStreamtype");
			item.addActionListener(_MenuListener);

			if (i == objects.length)
				streamtype.addSeparator();

			streamtype.add(item);
		}

		popup.add(streamtype);

		popup.addSeparator();

		JMenuItem menuitem_9 = popup.add(Resource.getString("popup.newOutName"));
		menuitem_9.setActionCommand("newOutName");


		/**
		 *
		 */
		objects = Keys.ITEMS_ConversionMode;

		JMenu action = new JMenu(Resource.getString("popup.assignActionType"));

		for (int i = -1; i < objects.length; i++)
		{
			JMenuItem item = new JMenuItem(i < 0 ? Resource.getString("popup.unspecified") : objects[i].toString());
			item.setActionCommand("applyAction");
			item.addActionListener(_MenuListener);

			if (i == 0)
				action.addSeparator();

			action.add(item);
		}

		popup.add(action);

		popup.pack();

		UIManager.addPropertyChangeListener(new UISwitchListener(popup));

		menuitem_4.addActionListener(_MenuListener);
		menuitem_5.addActionListener(_MenuListener);
		menuitem_6.addActionListener(_MenuListener);	
	//	menuitem_7.addActionListener(_MenuListener);
		menuitem_8.addActionListener(_MenuListener);
		menuitem_9.addActionListener(_MenuListener);
		menuitem_10.addActionListener(_MenuListener);
		menuitem_11.addActionListener(_MenuListener);
	}

	/**
	 *
	 */
	protected void buildMenus()
	{
		JMenuBar menuBar = new JMenuBar();

		menuBar.add(buildFileMenu());
		menuBar.add(buildViewerMenu());
		menuBar.add(buildGeneralMenu());
		menuBar.add(buildPreferencesMenu());
		menuBar.add(buildLanguageMenu());
		menuBar.add(buildAddonMenu());
		menuBar.add(buildHelpMenu());

		frame.setJMenuBar(menuBar);	
	}


	/**
	 *
	 */
	protected JMenu buildFileMenu()
	{
		JMenu file = new JMenu();
		CommonGui.localize(file, "Common.File");

		JMenuItem add = new JMenuItem();
		CommonGui.localize(add, "file.add");
		add.setActionCommand("add");
		add.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));

		JMenuItem url = new JMenuItem();
		CommonGui.localize(url, "file.url");
		url.setActionCommand("url");
		url.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, ActionEvent.CTRL_MASK));

		JMenuItem remove = new JMenuItem();
		CommonGui.localize(remove, "file.remove");
		remove.setActionCommand("remove");
		remove.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, ActionEvent.CTRL_MASK));

		JMenuItem rename = new JMenuItem();
		CommonGui.localize(rename, "file.rename");
		rename.setActionCommand("rename");
		rename.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, ActionEvent.CTRL_MASK));

		JMenuItem exit = new JMenuItem();
		CommonGui.localize(exit, "Common.Exit");
		exit.setActionCommand("exit");
		exit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.ALT_MASK));

		file.add(add);
		file.add(url);
		file.add(remove);
		file.addSeparator();
		file.add(rename);
		file.addSeparator();
		file.add(exit);

		url.addActionListener(_MenuListener);
		add.addActionListener(_MenuListener);
		remove.addActionListener(_MenuListener);
		rename.addActionListener(_MenuListener);
		exit.addActionListener(_MenuListener);

		return file;
	}

	/**
	 *
	 */
	protected JMenu buildSettingsMenu()
	{
		JMenu setting = new JMenu();
		CommonGui.localize(setting, "settings.menu");

		JMenuItem open = new JMenuItem();
		CommonGui.localize(open, "settings.settings");

		setting.add(open);

		return setting;
	}

	/**
	 *
	 */
	protected JMenu buildPreferencesMenu()
	{
		JMenu preferencesMenu = new JMenu();
		CommonGui.localize(preferencesMenu, "Common.Preferences");

		JMenuItem preferences = new JMenuItem();
		CommonGui.localize(preferences, "Common.Preferences");
		preferences.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
		preferences.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				Common.getGuiInterface().showPreSettings();
			}
		});

		preferencesMenu.add(preferences);

		preferencesMenu.addSeparator();

		JCheckBoxMenuItem save = new JCheckBoxMenuItem(Resource.getString(Keys.KEY_SaveSettingsOnExit[0]));
		save.setActionCommand(Keys.KEY_SaveSettingsOnExit[0]);
		save.setState(Common.getSettings().getBooleanProperty(Keys.KEY_SaveSettingsOnExit));
		save.addActionListener(_BoxListener);

		preferencesMenu.add(save);

		return preferencesMenu;
	}

	/**
	 *
	 */
	protected JMenu buildGeneralMenu()
	{
		JMenu general = new JMenu();
		CommonGui.localize(general, "general.menu");

		UIManager.LookAndFeelInfo[] lf_info = UIManager.getInstalledLookAndFeels();

		lf_item = new JRadioButtonMenuItem[lf_info.length];
		ButtonGroup lfgroup = new ButtonGroup();

		ActionListener al = new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				String lnfName = e.getActionCommand();

				Common.getSettings().setProperty(Keys.KEY_LookAndFeel[0], lnfName);
				setLookAndFeel(lnfName);
			}
		};

		for (int a = 0; a < lf_item.length; a++) 
		{
			lf_item[a] = new JRadioButtonMenuItem(lf_info[a].getClassName());
			general.add(lf_item[a]);
			lfgroup.add(lf_item[a]);
			lf_item[a].addActionListener(al);
		}

		setLookAndFeel(Common.getSettings().getProperty(Keys.KEY_LookAndFeel));

		return general;
	}


	/**
	 * sets the new look and feel.
	 * 
	 * @param lnfName
	 */
	private void setLookAndFeel(String lnfName)
	{
		if (lnfName != null && !lnfName.equals("")) 
		{
			JRadioButtonMenuItem selectedRadio = null;

			try {
				// update radio menu items
				for (int a=0; a < lf_item.length; a++) 
				{
					if (lf_item[a].getActionCommand().equals(lnfName))
					{
						lf_item[a].setSelected(true);
						selectedRadio = lf_item[a];
					}
				}

				// now update the components
				UIManager.setLookAndFeel(lnfName);
				SwingUtilities.updateComponentTreeUI(frame);
	
				if(CommonGui.getMainFileChooser() != null) 
					SwingUtilities.updateComponentTreeUI(CommonGui.getMainFileChooser());

			} catch (Exception exc) {

				selectedRadio.getParent().remove(selectedRadio);
				System.err.println("!> Could not load LookAndFeel: " + lnfName);
			}
		}
	}


	/**
	 *
	 */
	protected JMenu buildViewerMenu()
	{
		JMenu preview = new JMenu();
		CommonGui.localize(preview, "options.menu");

		JMenuItem hex = new JMenuItem();
		CommonGui.localize(hex, "options.openhexview");
		hex.setActionCommand("viewAsHex");

		preview.add(hex);
		preview.addSeparator();

		JMenuItem basic = new JMenuItem();
		CommonGui.localize(basic, "options.pachtbasics");
		basic.setActionCommand("editBasics");

		preview.add(basic);
		preview.addSeparator();

		JMenuItem subtitle = new JMenuItem();
		CommonGui.localize(subtitle, "options.subtitlepreview");
		subtitle.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				Common.getGuiInterface().showSubpicture();
			}
		});

		preview.add(subtitle);

		preview.addSeparator();

		JMenuItem pagematrix = new JMenuItem();
		CommonGui.localize(pagematrix, "options.teletext");
		pagematrix.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				Common.getGuiInterface().showTtxPageMatrix();
			}
		});


		preview.add(pagematrix);

		hex.addActionListener(_MenuListener);
		basic.addActionListener(_MenuListener);

		return preview;
	}


	/**
	 * Builds the Language Menu.
	 * 
	 * @return JMenu
	 */
	protected JMenu buildLanguageMenu()
	{
		ActionListener listener = new ActionListener() {
			public void actionPerformed(ActionEvent event)
			{
				String action = event.getActionCommand();

				if (action.equals("check"))
				{
					Common.checkAvailableLanguages();
					return;
				}

				if (action.equals("system"))
					Resource.setChosenLanguage(null);

				else
					Resource.setChosenLanguage(action);

				CommonGui.showMessageDialog(Resource.getString("msg.new.language"), Resource.getString("msg.infomessage"));
			}
		};

		
		JMenu langMenu = new JMenu();
		CommonGui.localize(langMenu, "language.menu");

		JMenuItem item_check = new JMenuItem();
		CommonGui.localize(item_check, "language.check");
		item_check.addActionListener(listener);
		item_check.setActionCommand("check");
		langMenu.add(item_check);

		langMenu.addSeparator();
		
		ButtonGroup group = new ButtonGroup();

		JRadioButtonMenuItem item_sys = new JRadioButtonMenuItem();
		CommonGui.localize(item_sys, "language.system");
		item_sys.addActionListener(listener);
		item_sys.setSelected(Resource.getChosenLanguage() == null);
		item_sys.setActionCommand("system");
		langMenu.add(item_sys);

		group.add(item_sys);

		langMenu.addSeparator();

		Locale[] locales = Resource.getAvailableLocales();

		for (int i = 0; i < locales.length; i++)
		{
			Locale item = locales[i];

			JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem(item.getLanguage());
			menuItem.addActionListener(listener);

			if (Resource.getChosenLanguage() != null)
				menuItem.setSelected(item.getLanguage().equals(Resource.getChosenLanguage()));

			menuItem.setActionCommand(item.getLanguage());
			langMenu.add(menuItem);
			group.add(menuItem);
		}

		return langMenu;
	}

	/**
	 *
	 */
	protected JMenu buildAddonMenu()
	{
		JMenu menu = new JMenu();
		CommonGui.localize(menu, "Common.Addons");

		menu.add(new JMenuItem("available components:"));
		menu.addSeparator();


		// change: read table or list from common.
		if (Common.canAccessFtp())
			menu.add(new JMenuItem("commons-net library (FTP access)"));

		if (Common.canAccessRawRead())
			menu.add(new JMenuItem("rawread dll (ext. disk access)"));

		if (Common.getMpvDecoderClass().isAccelerated())
			menu.add(new JMenuItem("accelerated preview (ext. IDCT)"));

		if (Common.canAccessColorTable())
			menu.add(new JMenuItem("color tables (DVB subpicture)"));
			
		if (Common.canAccessSilentAC3())
			menu.add(new JMenuItem("silent AC3 frames (replacements)"));

		return menu;
	}

	/**
	 *
	 */
	protected JMenu buildHelpMenu()
	{
		JMenu help = new JMenu();
		CommonGui.localize(help, "help.menu");

		JMenuItem about = new JMenuItem();
		CommonGui.localize(about, "help.about");
		about.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				new AboutBox(frame);
			}
		});

		JMenuItem openHtml = new JMenuItem();
		CommonGui.localize(openHtml, "help.help");
		openHtml.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				new Html().show();
			}
		});
		openHtml.setAccelerator(KeyStroke.getKeyStroke("F1"));

		JMenuItem version = new JMenuItem();
		CommonGui.localize(version, "help.version");
		version.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				Common.checkVersion();
			}
		});

		help.add(about);
		help.addSeparator();
		help.add(openHtml);
		help.addSeparator();
		help.add(version);

		return help;
	}

	/**
	 *
	 */
    protected JScrollPane createTable()
	{
		JScrollPane scrollpane;

        // final
        final String[] names = {
			"#", 
			Resource.getString("CollectionTable.Source"), 
			Resource.getString("CollectionTable.FileName"), 
			Resource.getString("CollectionTable.FileLocation"), 
			Resource.getString("CollectionTable.Size"), 
			Resource.getString("CollectionTable.lastModified"), 
			Resource.getString("ScanInfo.Video").substring(0, 1), 
			Resource.getString("ScanInfo.Audio").substring(0, 1), 
			Resource.getString("ScanInfo.Teletext").substring(0, 1), 
			Resource.getString("ScanInfo.Subpicture").substring(0, 1), 
			Resource.getString("CollectionTable.Streamtype")
		};

        // Create a model of the data.
        TableModel dataModel = new AbstractTableModel() {
            public int getColumnCount()
			{
				return names.length;
			}

            public int getRowCount()
			{
				return FileObjectTable.length;
			}

            public Object getValueAt(int row, int col)
			{
				return FileObjectTable[row][col];
			}

            public String getColumnName(int column)
			{
				return names[column];
			}

            public Class getColumnClass(int c)
			{
				Object obj = getValueAt(0, c);

				if (obj == null)
					return String.class;

				return obj.getClass();
			}

            public boolean isCellEditable(int row, int col)
			{
				return false;
				//return getColumnClass(col) == String.class;
			}

            public void setValueAt(Object aValue, int row, int column)
			{
				FileObjectTable[row][column] = aValue;
			}
         };


        // Create the table
        tableView = new JTable(dataModel);

        // Show colors by rendering them in their own color.
		DefaultTableCellRenderer renderer_1 = new DefaultTableCellRenderer();
		DefaultTableCellRenderer renderer_2 = new DefaultTableCellRenderer();
/**		{
			public void setValue(Object value)
			{
				if (value instanceof Color)
				{
					Color c = (Color)value;
					setForeground(c);
					setText(c.getRed() + ", " + c.getGreen() + ", " + c.getBlue());
				}
			}
        };
**/

		renderer_1.setHorizontalAlignment(JLabel.RIGHT);
		renderer_2.setHorizontalAlignment(JLabel.CENTER);

		tableView.setRowHeight(15);
		tableView.setGridColor(new Color(220, 220, 220));
		tableView.removeEditor();
		tableView.setToolTipText(Resource.getString("FilePanel.DragDrop.Tip"));
		tableView.setSelectionMode(2);
		tableView.setSelectionBackground(new Color(220, 220, 255));
		tableView.setSelectionForeground(Color.black);

		tableView.getColumn("#").setCellRenderer(renderer_2);
		tableView.getColumn("#").setMaxWidth(20);

		tableView.getColumn(names[1]).setCellRenderer(renderer_2);
		tableView.getColumn(names[1]).setMinWidth(32);
		tableView.getColumn(names[1]).setMaxWidth(32);

		tableView.getColumn(names[2]).setPreferredWidth(200);
		tableView.getColumn(names[3]).setPreferredWidth(200);

		tableView.getColumn(names[4]).setCellRenderer(renderer_1);
		tableView.getColumn(names[4]).setMinWidth(62);
		tableView.getColumn(names[4]).setMaxWidth(62);

		tableView.getColumn(names[5]).setCellRenderer(renderer_2);
		tableView.getColumn(names[5]).setMinWidth(96);
		tableView.getColumn(names[5]).setMaxWidth(96);

		for (int i = 6; i < 10; i++)
		{
			tableView.getColumn(names[i]).setCellRenderer(renderer_2);
			tableView.getColumn(names[i]).setMinWidth(16);
			tableView.getColumn(names[i]).setMaxWidth(16);
		}

        tableView.getColumn(names[10]).setMinWidth(90);

        tableView.sizeColumnsToFit(JTable.AUTO_RESIZE_LAST_COLUMN);

		tableView.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e)
			{
				if (e.getClickCount() < 1)
					return;

				int row = tableView.getSelectedRow();

				// empty row
				if (row >= 0 && tableView.getValueAt(row, 0) == null)
					row = -1;

				int index = Common.getActiveCollection(); 

				if (e.getModifiers() == MouseEvent.BUTTON3_MASK)
				{
					MenuElement[] elements = popup.getSubElements();

					if (elements == null)
						return;

					for (int i = 1; i < 7; i++)
						elements[i].getComponent().setEnabled(row >= 0);

					for (int i = 7; i < elements.length; i++)
						elements[i].getComponent().setEnabled(index >= 0);

					popup.show(tableView, e.getX(), e.getY() - popup.getHeight());
				}

				else if (row >= 0)
					ScanInfo((XInputFile) Common.getCollection(index).getInputFile(row));
			}
		});

		DropTargetListener dnd1Listener = new DropTargetListener()
		{
			public void drop(DropTargetDropEvent e)
			{
				try {
	
					int dropaction = e.getDropAction();  // 1=copy, 2=move

					if (dropaction == 0 || dropaction > 2)
					{ 
						e.rejectDrop(); 
						return; 
					}

					e.acceptDrop(dropaction);

					Transferable tr = e.getTransferable();
					DataFlavor[] df = tr.getTransferDataFlavors();

					// Get list with one or more File objects
					// List li = (java.util.List)tr.getTransferData(df[0]);
					List list = null;

					Object obj = tr.getTransferData(df[0]);

					try {
						list = (java.util.List) obj;

					} catch (Exception ce1) {

						// MacOsX tiger returns one Url instead of a file list, works only without host specification of the file
						try {
							URL url = (URL)obj;
							File f = new File(url.getFile());
							list = new ArrayList();
							list.add(f);

						} catch (Exception ce2) {

							e.dropComplete(true);
							return;
						}
					}

					// Replace dropped File objects by XInputFile objects
					ArrayList tempList = new ArrayList();

					for (int i = 0; i < list.size(); i++)
						tempList.add(new XInputFile((File)list.get(i)));

					list = tempList;

					if (dropaction == 1)        // copy = new coll each
					{
						Object[] val = list.toArray();

						/**
						 * create new collection for each file
						 */
						for (int i = 0; i < val.length; i++)
						{
							JobCollection collection = Common.addCollection();
							collection.addInputFile(val[i]);

							updateCollectionTable(collection.getCollectionAsTable());
						}
					}

					else if (dropaction == 2)    // move = one coll
					{
						Common.addCollection(false);

						Object[] val = list.toArray();

						if (val.length > 0)
						{
							JobCollection collection = Common.getCollection(comboBox_0.getSelectedIndex());
							collection.addInputFile(val);

							updateCollectionTable(collection.getCollectionAsTable());
						}
					}

					e.dropComplete(true);

					if (list.size() > 0)
						updateCollectionPanel(Common.getActiveCollection());

				} catch (Exception eee) { 

					e.dropComplete(false); 
					Common.setExceptionMessage(eee);
				}

				tableView.setBackground(Color.white);
			}

			public void dragEnter(DropTargetDragEvent e)
			{ 
				tableView.setBackground(Color.green);
			}

			public void dragExit(DropTargetEvent e)
			{
				tableView.setBackground(Color.white);
			}

			public void dragOver(DropTargetDragEvent e)
			{}

			public void dropActionChanged(DropTargetDragEvent e)
			{}
		};

		DropTarget dropTarget_2 = new DropTarget(tableView, dnd1Listener);

        scrollpane = new JScrollPane(tableView);

        return scrollpane;
    }

	/**
	 *
	 */
	protected JPanel buildFilePanel()
	{
		JPanel panel_1 = new JPanel();
		panel_1.setLayout(new ColumnLayout());

		/**
		 * autoload
		 */
		JButton open_autoload = new JButton(CommonGui.loadIcon("fwd_10.gif"));
		open_autoload.setPreferredSize(new Dimension(30, 22));
		open_autoload.setMaximumSize(new Dimension(30, 22));
		open_autoload.setToolTipText(Resource.getString("FilePanel.openAutoloadPanel.Tip"));
		open_autoload.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				autoload.show();
			}
		});
		panel_1.add(open_autoload);

		/**
		 * add
		 */
		JButton file_add = new JButton(CommonGui.loadIcon("add.gif"));
		file_add.setPreferredSize(new Dimension(30, 22));
		file_add.setMaximumSize(new Dimension(30, 22));
		file_add.setToolTipText(Resource.getString("FilePanel.FileAdd.Tip"));
		file_add.setActionCommand("add");
		file_add.addActionListener(_MenuListener);
		panel_1.add(file_add);

		/**
		 * remove
		 */
		JButton file_remove = new JButton(CommonGui.loadIcon("rem.gif"));
		file_remove.setPreferredSize(new Dimension(30, 22));
		file_remove.setMaximumSize(new Dimension(30, 22));
		file_remove.setToolTipText(Resource.getString("FilePanel.FileRemove.Tip"));
		file_remove.setActionCommand("remove");
		file_remove.addActionListener(_MenuListener);
		panel_1.add(file_remove);

		/**
		 * up
		 */
		JButton file_up = new JButton(CommonGui.loadIcon("up.gif"));
		file_up.setPreferredSize(new Dimension(30, 22));
		file_up.setMaximumSize(new Dimension(30, 22));
		file_up.setToolTipText(Resource.getString("FilePanel.FileUp.Tip"));
		file_up.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				int[] indices = tableView.getSelectedRows();

				if (indices.length > 0)
				{
					JobCollection collection = Common.getCollection(comboBox_0.getSelectedIndex());

					for (int i = 0; i < indices.length; i++)
					{
						int index = indices[i];

						if (index > 0 && tableView.getValueAt(index, 0) != null)
						{
							Object object = collection.removeInputFile(index);

							collection.addInputFile(index - 1, object);
							indices[i] = index - 1;
						}
					}
	
					updateCollectionTable(collection.getCollectionAsTable());

					updateCollectionPanel(Common.getActiveCollection());
				}
			}
		});
		panel_1.add(file_up);
	

		/**
		 * down
		 */
		JButton file_down = new JButton(CommonGui.loadIcon("dn.gif"));
		file_down.setPreferredSize(new Dimension(30, 22));
		file_down.setMaximumSize(new Dimension(30, 22));
		file_down.setToolTipText(Resource.getString("FilePanel.FileDown.Tip"));
		file_down.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				int[] indices = tableView.getSelectedRows();

				if (indices.length > 0)
				{
					JobCollection collection = Common.getCollection(comboBox_0.getSelectedIndex());

					for (int i = indices.length - 1; i >= 0; i--)
					{
						int index = indices[i];

						if (index < collection.getInputFilesCount() - 1 && tableView.getValueAt(index, 0) != null)
						{
							Object object = collection.removeInputFile(index);

							collection.addInputFile(index + 1, object);
							indices[i] = index + 1;
						}
					}
	
					updateCollectionTable(collection.getCollectionAsTable());

					updateCollectionPanel(Common.getActiveCollection());
				}
			}
		});
		panel_1.add(file_down);


		/**
		 * 
		 */
		JPanel panel_2 = new JPanel();
		panel_2.setLayout(new BoxLayout(panel_2, BoxLayout.X_AXIS));

		panel_2.add(new JLabel(Resource.getString("FilePanel.OutputDirectory")));

		panel_2.add(Box.createRigidArea(new Dimension(6, 1)));

		outfield = new JTextField();
		outfield.setBackground(new Color(225, 255, 225));
		outfield.setMaximumSize(new Dimension(280, 20));
		outfield.setMinimumSize(new Dimension(280, 20));
		outfield.setEditable(false);
		outfield.setToolTipText(Resource.getString("FilePanel.OutputDirectory.Tip"));

		panel_2.add(outfield);

		panel_2.add(Box.createRigidArea(new Dimension(6, 1)));

		panel_2.add(new JLabel(Resource.getString("FilePanel.recentOutputDirectories")));

		panel_2.add(Box.createRigidArea(new Dimension(6, 1)));


		/**
		 * 
		 */
		// recent output
		final JComboBox comboBox_13 = new JComboBox(Common.getSettings().getOutputDirectories().toArray());
		comboBox_13.setMinimumSize(new Dimension(280, 20));
		comboBox_13.setMaximumSize(new Dimension(280, 20));
		comboBox_13.setMaximumRowCount(8);
		comboBox_13.insertItemAt(Resource.getString("working.output.std"), 0);
		comboBox_13.setSelectedItem(Common.getSettings().getProperty(Keys.KEY_OutputDirectory));
		comboBox_13.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				if (comboBox_13.getItemCount() > 1)
				{
					if (comboBox_13.getSelectedIndex() == 0)
						Common.getSettings().remove(Keys.KEY_OutputDirectory[0]);

					else
						Common.getSettings().setProperty(Keys.KEY_OutputDirectory[0], comboBox_13.getSelectedItem());

					if (comboBox_0.getItemCount() > 0)
					{
						Common.setActiveCollection(comboBox_0.getSelectedIndex());

						JobCollection collection = Common.getCollection(Common.getActiveCollection());

						collection.setOutputDirectory(Common.getSettings().getProperty(Keys.KEY_OutputDirectory));

						updateOutputField(collection);

						updateCollectionTable(collection.getCollectionAsTable());
					}
				}

				else
				{
					Common.getSettings().remove(Keys.KEY_OutputDirectory[0]);

					if (comboBox_0.getItemCount() > 0)
					{
						Common.setActiveCollection(comboBox_0.getSelectedIndex());

						JobCollection collection = Common.getCollection(Common.getActiveCollection());

						collection.setOutputDirectory(Common.getSettings().getProperty(Keys.KEY_OutputDirectory));

						updateOutputField(collection);

						updateCollectionTable(collection.getCollectionAsTable());
					}
				}
			}
		});

		/**
		 * 
		 */
		JButton add_output = new JButton(CommonGui.loadIcon("add.gif"));
		add_output.setMinimumSize(new Dimension(24, 20));
		add_output.setMaximumSize(new Dimension(24, 20));
		add_output.setToolTipText(Resource.getString("FilePanel.addRecentOutputDirectory.Tip"));
		add_output.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				CommonGui.getMainFileChooser().rescanCurrentDirectory();
				CommonGui.getMainFileChooser().setDialogType(JFileChooser.OPEN_DIALOG);
				CommonGui.getMainFileChooser().setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

				int retval = CommonGui.getMainFileChooser().showDialog(frame, null);

				if (retval == JFileChooser.APPROVE_OPTION)
				{
					File theFile = CommonGui.getMainFileChooser().getSelectedFile();
					String file = "";

					if (theFile != null)
					{
						if (theFile.isFile()) 
							file = theFile.getParent(); 

						else if (theFile.isDirectory()) 
							file = theFile.getAbsolutePath(); 

						// do not list duplicates
						for (int i = 0; i < comboBox_13.getItemCount(); i++)
							if (file.equalsIgnoreCase(comboBox_13.getItemAt(i).toString())) 
								return;

						Common.getSettings().addOutputDirectory(file);

						comboBox_13.addItem(file);
						comboBox_13.setSelectedItem(file);
					}
				}
			}
		});


		/**
		 * 
		 */
		JButton remove_output = new JButton(CommonGui.loadIcon("rem.gif"));
		remove_output.setMinimumSize(new Dimension(24, 20));
		remove_output.setMaximumSize(new Dimension(24, 20));
		remove_output.setToolTipText(Resource.getString("FilePanel.removeRecentOutputDirectory.Tip"));
		remove_output.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				if (comboBox_13.getItemCount() > 1)
				{
					int index = comboBox_13.getSelectedIndex();

					if (index > 0)
					{
						Common.getSettings().removeOutputDirectory(index - 1);

						comboBox_13.removeItemAt(index);
					}

					else
						Common.setOSDErrorMessage("Eintrag kann nicht entfernt werden..");
				}

				if (comboBox_13.getItemCount() <= 1)
					Common.getSettings().remove(Keys.KEY_OutputDirectory[0]);
			}
		});


		panel_2.add(add_output);
		panel_2.add(remove_output);
		panel_2.add(comboBox_13);



		/**
		 * table + putput dir's
		 */
		JPanel control_1 = new JPanel(new BorderLayout());
		control_1.setAlignmentX(CENTER_ALIGNMENT);
		control_1.add(createTable(), BorderLayout.CENTER);
		control_1.add(panel_2, BorderLayout.SOUTH);

		/**
		 * file panel at all
		 */
		JPanel control_2 = new JPanel(new BorderLayout());
		control_2.setAlignmentX(CENTER_ALIGNMENT);
		control_2.add(control_1, BorderLayout.CENTER);
		control_2.add(panel_1, BorderLayout.WEST);

		/**
		 * panel
		 */
		JPanel panel = new JPanel();
		panel.setLayout( new GridLayout(1, 1) );
		panel.setBorder(BorderFactory.createEtchedBorder());

		panel.add(control_2);

		panel.setPreferredSize(new Dimension(900, 114));
		panel.setMaximumSize(new Dimension(900, 114));
		panel.setMinimumSize(new Dimension(900, 114));

		return panel;
	}


	/**
	 * 
	 */
	private void close_AutoloadPanel()
	{
		autoload.dispose();
	}

	/**
	 * 
	 */
	protected void buildAutoloadPanel()
	{
		autoload = new JFrame(Resource.getString("autoload.title"));
		autoload.addWindowListener ( new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				close_AutoloadPanel();
			}
		});

		JPanel bb = new JPanel();
		bb.setLayout( new ColumnLayout() );

		/**
		 * 
		 */
		final JComboBox comboBox_12 = new JComboBox(Common.getSettings().getListProperty(Keys.KEY_InputDirectories).toArray());  // recent input
		comboBox_12.setMaximumRowCount(8);
		comboBox_12.setPreferredSize(new Dimension(400, 24));

		/**
		 * 
		 */
		JButton remove_input = new JButton(CommonGui.loadIcon("rem.gif"));
		remove_input.setPreferredSize(new Dimension(50,28));
		remove_input.setMaximumSize(new Dimension(50,28));
		remove_input.setToolTipText(Resource.getString("autoload.dir.remove.tip"));
		remove_input.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				if (comboBox_12.getItemCount() > 0) 
				{
					int index = comboBox_12.getSelectedIndex();

					Common.getSettings().removeInputDirectory(index);
					Common.getSettings().updateInputDirectories();

					comboBox_12.removeItemAt(index);
				}

				reloadInputDirectories();
			}
		});
		bb.add(remove_input);

		/**
		 * 
		 */
		JButton add_input = new JButton(CommonGui.loadIcon("add.gif"));
		add_input.setPreferredSize(new Dimension(50,28));
		add_input.setMaximumSize(new Dimension(50,24));
		add_input.setToolTipText(Resource.getString("autoload.dir.add.tip"));
		add_input.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				CommonGui.getMainFileChooser().rescanCurrentDirectory();
				CommonGui.getMainFileChooser().setDialogType(JFileChooser.OPEN_DIALOG);
				CommonGui.getMainFileChooser().setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

				int retval = CommonGui.getMainFileChooser().showDialog(frame, null);

				if (retval == JFileChooser.APPROVE_OPTION)
				{
					File theFile = CommonGui.getMainFileChooser().getSelectedFile();

					if (theFile != null)
					{
						if (theFile.isFile()) 
							theFile = theFile.getParentFile();

						/**
						 * must use getAbsolutFile to ensure right ClassType, 
						 * sometimes the returned Object.getClass 
						 * from selection is NOT of java.io.File!!
						 */
						String str = Common.getSettings().addInputDirectory(theFile.getAbsoluteFile());

						if (str != null)
						{
							Common.getSettings().updateInputDirectories();

							comboBox_12.addItem(str);
							comboBox_12.setSelectedItem(str);
						}

						reloadInputDirectories();
					}

					autoload.toFront();

					return;
				}

				autoload.toFront();
			}
		});
		bb.add(add_input);


		// Button to add a ftp server directory to the autoload list
		JButton add_inputftp = new JButton(CommonGui.loadIcon("ftp.gif"));
		add_inputftp.setPreferredSize(new Dimension(50,28));
		add_inputftp.setMaximumSize(new Dimension(50,24));
		add_inputftp.setToolTipText(Resource.getString("autoload.ftp.add.tip"));
		add_inputftp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				// Add ftp server directory to autoload list
				FtpChooser ftpChooser = new FtpChooser();

				ftpChooser.pack();
				ftpChooser.show();

				XInputDirectory xInputDirectory = ftpChooser.getXInputDirectory();

				if (ftpChooser.isTested() && xInputDirectory != null)
				{
					String str = Common.getSettings().addInputDirectory(xInputDirectory);

					if (str != null)
					{
						Common.getSettings().updateInputDirectories();

						comboBox_12.addItem(str);
						comboBox_12.setSelectedItem(str);
					}

					reloadInputDirectories();

					autoload.toFront();

					return;
				}

				autoload.toFront();
			}
		});
		bb.add(add_inputftp);

		/**
		 * 
		 */
		JButton refresh_list = new JButton(CommonGui.loadIcon("rf.gif"));
		refresh_list.setPreferredSize(new Dimension(50,28));
		refresh_list.setMaximumSize(new Dimension(50,28));
		refresh_list.setToolTipText(Resource.getString("autoload.dir.refresh.tip"));
		refresh_list.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				reloadInputDirectories(); 
			}
		});
		bb.add(refresh_list);


		bb.add(new JLabel(" "));

		/**
		 * 
		 */
		JButton add_coll_and_files = new JButton(CommonGui.loadIcon("addleft.gif"));
		add_coll_and_files.setPreferredSize(new Dimension(50,28));
		add_coll_and_files.setMaximumSize(new Dimension(50,28));
		add_coll_and_files.setToolTipText(Resource.getString("autoload.add.coll.tip"));
		add_coll_and_files.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				Object[] val = list1.getSelectedValues();

				/**
				 * create new collection for each file
				 */
				for (int i = 0; i < val.length; i++)
				{
					JobCollection collection = Common.addCollection();

					collection.addInputFile(val[i]);

					updateCollectionTable(collection.getCollectionAsTable());
				}

				if (val.length > 0)
					updateCollectionPanel(Common.getActiveCollection());
			}
		});
		bb.add(add_coll_and_files);


		/**
		 * 
		 */
		JButton add_files = new JButton(CommonGui.loadIcon("left.gif"));
		add_files.setPreferredSize(new Dimension(50, 28));
		add_files.setMaximumSize(new Dimension(50, 28));
		add_files.setToolTipText(Resource.getString("autoload.add.file.tip"));
		add_files.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				Object[] val = list1.getSelectedValues();

				if (val.length > 0) // one or more files
				{
					Common.addCollection(false);

					JobCollection collection = Common.getCollection(comboBox_0.getSelectedIndex());
					collection.addInputFile(val);

					updateCollectionTable(collection.getCollectionAsTable());
					updateCollectionPanel(Common.getActiveCollection());
				}
			}
		});
		bb.add(add_files);


		bb.add(new JLabel(" "));

		/**
		 * 
		 */
		JButton close = new JButton(CommonGui.loadIcon("x.gif"));
		close.setPreferredSize(new Dimension(50,28));
		close.setMaximumSize(new Dimension(50,28));
		close.setToolTipText(Resource.getString("autoload.close"));
		close.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				close_AutoloadPanel();
			}
		});
		bb.add(close);


		// in list
		list1 = new JList(new Object[0]);
		list1.setName("inl");
		list1.setVisibleRowCount(8);
		list1.setSelectionMode(2);
		list1.setToolTipText(Resource.getString("autoload.rename.tip"));
		list1.addMouseListener( new MouseAdapter() {
			public void mouseClicked(MouseEvent e)
			{
				int index = list1.locationToIndex( e.getPoint());

				if (e.getClickCount() > 1)
				{
					if (e.getModifiers() == MouseEvent.BUTTON3_MASK && index > -1) // rename file
					{
						try {
							if (((XInputFile)list1.getSelectedValue()).rename())
								reloadInputDirectories();

						} catch (IOException ioe) {}

						autoload.toFront();
					}

					else if (e.getModifiers() == MouseEvent.BUTTON1_MASK && index > -1) // add file to coll
					{
						Common.addCollection(false);

						Object[] val = list1.getSelectedValues();

						if (val.length > 0) // one or more files
						{
							JobCollection collection = Common.getCollection(comboBox_0.getSelectedIndex());
							collection.addInputFile(val);

							updateCollectionTable(collection.getCollectionAsTable());
							updateCollectionPanel(Common.getActiveCollection());
						}
					}
				}

				else if (e.getClickCount() == 1)
				{
					if (list1.getSelectedValue() != null )
						ScanInfo( (XInputFile) list1.getSelectedValue());
				}
			}
		});

		list1.addKeyListener( new KeyAdapter() {
			public void keyPressed(KeyEvent e)
			{
				if (e.getKeyChar() == KeyEvent.VK_ENTER)
				{
					Common.addCollection(false);

					Object[] val = list1.getSelectedValues();

					if (val.length > 0) // one or more files
					{
						JobCollection collection = Common.getCollection(comboBox_0.getSelectedIndex());
						collection.addInputFile(val);

						updateCollectionTable(collection.getCollectionAsTable());
						updateCollectionPanel(Common.getActiveCollection());
					}
				}
			}
		});

		JScrollPane scrolltext = new JScrollPane();
		scrolltext.setViewportView(list1);

		/**
		 * 
		 */
		JPanel control_1 = new JPanel(new BorderLayout());
		control_1.setAlignmentX(CENTER_ALIGNMENT);
		control_1.add(scrolltext, BorderLayout.CENTER);
		control_1.add(comboBox_12, BorderLayout.NORTH);

		JPanel control_2 = new JPanel(new BorderLayout());
		control_2.setAlignmentX(CENTER_ALIGNMENT);
		control_2.add(control_1, BorderLayout.CENTER);
		control_2.add(bb, BorderLayout.WEST);


		autoload.getContentPane().add(control_2);

		UIManager.addPropertyChangeListener(new UISwitchListener(control_2));

		autoload.setBounds(200, 200, 500, 300);
	}

	/**
	 *
	 */
	protected JPanel buildMainPanel()
	{
		/**
		 *
		 */
		JPanel panel_1 = new JPanel();
		panel_1.setToolTipText(Resource.getString("MainPanel.Process.Tip"));
		panel_1.setLayout(new ColumnLayout());

		MemoryMonitor memo = new MemoryMonitor();

		if (Common.showGUI())
			memo.surf.start();

		panel_1.add(memo, BorderLayout.NORTH);

		panel_1.add(Box.createRigidArea(new Dimension(1, 5)));
		panel_1.add(Box.createRigidArea(new Dimension(1, 90)));

		panel_1.add(buildProcessControlPanel());
		panel_1.add(buildCollectionControlPanel());

		panel_1.setPreferredSize(new Dimension(115, 468));
		panel_1.setMaximumSize(new Dimension(115, 468));
		panel_1.setMinimumSize(new Dimension(115, 468));

		/**
		 *
		 */
		JPanel panel_2 = new JPanel();
	//	panel_2.setLayout(new BorderLayout());
		panel_2.setLayout(new BoxLayout(panel_2, BoxLayout.X_AXIS));

		panel_2.add(panel_1, BorderLayout.WEST);
		panel_2.add(collection_panel = new CollectionPanel(), BorderLayout.CENTER);

		panel_2.setPreferredSize(new Dimension(900, 468));
		panel_2.setMaximumSize(new Dimension(900, 468));
		panel_2.setMinimumSize(new Dimension(900, 468));

		/**
		 *
		 */
		JPanel panel = new JPanel();
	//	panel.setLayout(new BorderLayout());
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		panel.add(panel_2, BorderLayout.CENTER);
		panel.add(buildFilePanel(), BorderLayout.SOUTH);

		return panel;
	}

	/**
	 *
	 */
	protected JPanel buildProcessControlPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBorder(BorderFactory.createTitledBorder( BorderFactory.createEtchedBorder(), Resource.getString("ProcessWindow.Title")));

		/**
		 * process window open
		 */
		JButton processwindow = new JButton(Resource.getString("ProcessWindowPanel.Button"));
		processwindow.setPreferredSize(new Dimension(100, 24));
		processwindow.setMaximumSize(new Dimension(100, 24));
		processwindow.setMinimumSize(new Dimension(100, 24));
		processwindow.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				Common.getGuiInterface().showLogWindow();
			}
		});

		panel.add(processwindow);

		return panel;
	}

	/**
	 *
	 */
	protected JPanel buildCollectionControlPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBorder(BorderFactory.createTitledBorder( BorderFactory.createEtchedBorder(), Resource.getString("MainPanel.Collection")));

		final Color idle_color = new Color(230, 230, 230);
		final Color running_color = new Color(245, 215, 215);

		/**
		 *  info field
		 */
		final JTextArea textarea = new JTextArea();
		textarea.setToolTipText(Resource.getString("FilePanel.Textfield.Tip"));
		textarea.setBackground(idle_color);
		textarea.setFont(new Font("Tahoma", Font.PLAIN, 11));
		textarea.setEditable(false);

		JPanel panel_2 = new JPanel();
		panel_2.setLayout(new GridLayout(1,1));
		panel_2.setBorder(BorderFactory.createLoweredBevelBorder());
		panel_2.setPreferredSize(new Dimension(100, 138));
		panel_2.setMaximumSize(new Dimension(100, 138));
		panel_2.setMinimumSize(new Dimension(100, 138));
		panel_2.add(textarea);

		panel.add(panel_2);

		/**
		 *  collection label
		 */
		JLabel coll_label = new JLabel(Resource.getString("FilePanel.CollectionNumber"));
		coll_label.setPreferredSize(new Dimension(50, 24));
		coll_label.setMaximumSize(new Dimension(50, 24));
		coll_label.setHorizontalAlignment(SwingConstants.CENTER);
		coll_label.setToolTipText(Resource.getString("FilePanel.CollectionNumber.Tip"));

		/**
		 *  number of act. coll.
		 */
		comboBox_0 = new JComboBox();
		comboBox_0.setPreferredSize(new Dimension(50, 24));
		comboBox_0.setMaximumSize(new Dimension(50, 24));
		comboBox_0.setMinimumSize(new Dimension(50, 24));
		comboBox_0.setMaximumRowCount(6);
		comboBox_0.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				if (comboBox_0.getItemCount() > 0)
				{
					Common.setActiveCollection(comboBox_0.getSelectedIndex());

					JobCollection collection = Common.getCollection(Common.getActiveCollection());

					updateOutputField(collection);

					updateCollectionTable(collection.getCollectionAsTable());

					if (!SilentAction)
						updateCollectionPanel(Common.getActiveCollection());
				}
				else
				{ 
					Common.setActiveCollection(-1);

					outfield.setText(""); 

					updateCollectionTable(null);
				}
			}
		});


		/**
		 *  add collection
		 */
		JButton remove_coll = new JButton(CommonGui.loadIcon("rem.gif"));
		remove_coll.setPreferredSize(new Dimension(50, 24));
		remove_coll.setMaximumSize(new Dimension(50, 24));
		remove_coll.setToolTipText(Resource.getString("FilePanel.removeCollection.Tip"));
		remove_coll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				if (Common.isCollectionListEmpty())
					return;

				int index = comboBox_0.getSelectedIndex();

				if (!Common.removeCollection(index))
					return;

				comboBox_0.removeAllItems();

				for (int i = 0; i < Common.getCollectionListSize(); i++) 
					comboBox_0.addItem(String.valueOf(i));

				if (index < comboBox_0.getItemCount())
					comboBox_0.setSelectedIndex(index);
			}
		});

		/**
		 *  add collection
		 */
		JButton add_coll = new JButton(CommonGui.loadIcon("add.gif"));
		add_coll.setPreferredSize(new Dimension(50, 24));
		add_coll.setMaximumSize(new Dimension(50, 24));
		add_coll.setToolTipText(Resource.getString("FilePanel.addCollection.Tip"));
		add_coll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				Common.addCollection();
			}
		});


		JPanel panel_0 = new JPanel();
		panel_0.setLayout(new BoxLayout(panel_0, BoxLayout.X_AXIS));

		panel_0.add(add_coll);
		panel_0.add(coll_label);

		panel.add(panel_0);


		JPanel panel_1 = new JPanel();
		panel_1.setLayout(new BoxLayout(panel_1, BoxLayout.X_AXIS));

		panel_1.add(remove_coll);
		panel_1.add(comboBox_0);

		panel.add(panel_1);

		/**
		 * watch on changes
		 */
		class Clock implements Runnable {
			private Thread clockThread = null;

			private String text = "";

			public void start()
			{
				if (clockThread == null)
				{
					clockThread = new Thread(this, "Clock_3");
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
				JobCollection collection = Common.getCollection(Common.getActiveCollection());

				String str = collection == null ? Resource.getString("JobCollection.NoInfo") : collection.getShortSummary();

				if (text.equals(str))
					return;

				text = str;

				textarea.setText(text);
				textarea.setBackground(collection != null && collection.isActive() ? running_color : idle_color);
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
	protected JPanel buildStatusPanel()
	{
		final JLabel status = new JLabel(Resource.getString("run.status"));
		status.setToolTipText("status of processing");

		final JLabel date = new JLabel();
		final JLabel time = new JLabel();

		class Clock implements Runnable {
			private Thread clockThread = null;

			private String StatusString = "";
			private String DateString = "";

			public void start()
			{
				if (clockThread == null)
				{
					clockThread = new Thread(this, "Clock_1");
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
				updateStatusLabel();
				updateDateLabel();
				updateTimeLabel();
			}

			private void updateStatusLabel()
			{
				String str = Common.getStatusString();

				if (str.equals(StatusString))
					return;

				StatusString = str;

				status.setText(StatusString);
			}

			private void updateDateLabel()
			{
				String str = DateFormat.getDateInstance(DateFormat.LONG).format(new Date());

				if (str.equals(DateString))
					return;

				DateString = str;

				date.setText(DateString);
			}

			private void updateTimeLabel()
			{
				time.setText(DateFormat.getTimeInstance(DateFormat.MEDIUM).format(new Date()));
			}

			public void stop()
			{
				clockThread = null;
			}
		}

		new Clock().start();

		JPanel status_1 = new JPanel(new BorderLayout());
		status_1.setBorder(BorderFactory.createLoweredBevelBorder());
		status_1.setPreferredSize(new Dimension(710, 22));
		status_1.setMaximumSize(new Dimension(710, 22));
		status_1.add(status);

		JPanel status_4 = new JPanel(new BorderLayout());
		status_4.setBorder(BorderFactory.createLoweredBevelBorder());
		status_4.setPreferredSize(new Dimension(120, 22));
		status_4.setMaximumSize(new Dimension(120, 22));
		status_4.add(date);

		JPanel status_5 = new JPanel(new BorderLayout());
		status_5.setBorder(BorderFactory.createLoweredBevelBorder());
		status_5.setPreferredSize(new Dimension(80, 22));
		status_5.setMaximumSize(new Dimension(80, 22));
		status_5.add(time);

		JPanel mainStatusPanel = new JPanel();
		mainStatusPanel.setLayout(new BoxLayout(mainStatusPanel, BoxLayout.X_AXIS));
		mainStatusPanel.add(status_1);
		mainStatusPanel.add(status_4);
		mainStatusPanel.add(status_5);

		return mainStatusPanel;
	}

	/**
	 * show ScanInfos
 	 */
	public void ScanInfo(XInputFile aXInputFile)
	{
		if (aXInputFile.getStreamInfo() == null)
			Common.getScanClass().getStreamInfo(aXInputFile);

		CommonGui.getPicturePanel().setStreamInfo(aXInputFile.getStreamInfo());
	}



	/**
	 * refresh inputfileslist
	 */
	public void reloadInputDirectories()
	{
		ArrayList arraylist = new ArrayList();
		ArrayList input_directories = Common.getSettings().getInputDirectories();

		for (int a = 0; a < input_directories.size(); a++)
		{
			// Get input files
			Object item = input_directories.get(a);

			XInputDirectory xInputDirectory = (XInputDirectory)item;
			XInputFile[] addlist = xInputDirectory.getFiles();

			// Sort them
			if (addlist.length > 0)
			{
				class MyComparator implements Comparator {
					public int compare(Object o1, Object o2)
					{
						return o1.toString().compareTo(o2.toString());
					}
				}

				Arrays.sort(addlist, new MyComparator());
			}

			// Add them to the list
			for (int b = 0; b < addlist.length; b++)
				arraylist.add(addlist[b]);
		}

		try {
			// Get input files from topfield raw disk access
			XInputDirectory xInputDirectory = new XInputDirectory(DirType.RAW_DIR);
			XInputFile[] addlist = xInputDirectory.getFiles();

			// Sort them
			if (addlist.length > 0)
			{
				class MyComparator implements Comparator
				{
					public int compare(Object o1, Object o2)
					{
						return o1.toString().compareTo(o2.toString());
					}
				}

				Arrays.sort(addlist, new MyComparator());
			}

			// Add them to the list
			for (int b = 0; b < addlist.length; b++)
				arraylist.add(addlist[b]);

		} catch (Throwable t) {
			// Assume no dll available or no hd or no file, so do nothing!
		}

		updateAutoloadList(arraylist.isEmpty() ? new Object[0] : arraylist.toArray());
	}   

	/**
	 * main
	 */
	private void showStartUpProgress(StartUp startup, int value, String str)
	{
		if (startup == null)
			System.out.println(str);

		else
			startup.setProgress(value, str);
	}

	/**
	 * main
	 */
	private void initialize(StartUp startup)
	{
		String[] version = Common.getVersion();

		//StartUp startup = new StartUp();

		try {
			//startup.show();

			showStartUpProgress(startup, 0, "Loading GUI...");

			buildGUI(startup);

			showStartUpProgress(startup, 70, "Loading Input Directories...");

			reloadInputDirectories();
	
			/**
			 * loading GUI 
			 */
			showStartUpProgress(startup, 80, "Loading Main Frame...");

			frame.addWindowListener (new WindowAdapter() { 
				public void windowClosing(WindowEvent e)
				{ 
				//	X.closeProgram(true);
					Common.exitApplication(0);
				}
			});

			frame.addComponentListener(new ComponentListener() {
				public void componentHidden(ComponentEvent e) {} 
				public void componentMoved(ComponentEvent e) {} 
				public void componentShown(ComponentEvent e) {} 

				public void componentResized(ComponentEvent e)
				{
					int w = Integer.parseInt(Keys.KEY_WindowPositionMain_Width[1]);
					int h = Integer.parseInt(Keys.KEY_WindowPositionMain_Height[1]);

					Component c = e.getComponent();
					Dimension preferred = new Dimension(w, h), current = c.getSize();

					double newHeight = (preferred.getHeight() > current.getHeight()) ? preferred.getHeight() : current.getHeight();
					double newWidth = (preferred.getWidth() > current.getWidth()) ? preferred.getWidth() : current.getWidth();

					c.setSize(new Dimension((int)newWidth, (int)newHeight));
				}
			});
	
			frame.getContentPane().add(this);
	
			frame.setLocation(Common.getSettings().getIntProperty(Keys.KEY_WindowPositionMain_X), Common.getSettings().getIntProperty(Keys.KEY_WindowPositionMain_Y));
			frame.setSize(new Dimension(Common.getSettings().getIntProperty(Keys.KEY_WindowPositionMain_Width), Common.getSettings().getIntProperty(Keys.KEY_WindowPositionMain_Height)));

			setFrameTitle(frametitle = version[0] + "/" + version[1] + " " + version[2] + " " + version[3]);

			showStartUpProgress(startup, 90, "Printing Environment Settings...");

			Common.setMessage(null, false);

			Object[] obj = Common.getJavaEV(Common.getSettings().getInifile());

			Common.setMessage(obj);
			Common.setMessage("");

			// to OSD
			CommonGui.getPicturePanel().setOSDMessage(obj, true);
	
			showStartUpProgress(startup, 100, "Showing Main Frame...");

			if (startup != null)
			{
				startup.set(Common.getSettings().getBooleanProperty(Keys.KEY_Agreement));
		
				if (startup.get())
				{
					setVisible0(true);

					startup.close();
					startup = null;
				}
			}
			else
				setVisible0(true);

		/** 
		 * catch all other unhandled exception
		 */
		} catch(Exception e) {

			/**
			 * in GUI mode clean GUI and show GUI message
			 */
			if (Common.showGUI()) 
			{
				/**
				 * close startup
				 */
				if (startup != null)
				{
					startup.close();
					startup = null;
				}

				/**
				 * close main frame
				 */
				if (frame != null)
				{
					frame.setVisible(false);
				}

				/**
				 * show exception messge
				 */
				StringWriter sw = new StringWriter();
				e.printStackTrace(new PrintWriter(sw));

				CommonGui.showErrorMessageDialog(Resource.getString("startup.error") + "\n\n" + sw.toString(), Resource.getString("startup.error.title"));
			}

			/**
			 * in CLI mode simply show stackTrace
			 */
			else 
			{
				e.printStackTrace();
			}

			Common.exitApplication(1);
		}
	}

	/**
	 *
	 */
	public static void setVisible0(boolean b)
	{
		SwingUtilities.updateComponentTreeUI(frame); // update selecte L&F
		frame.setVisible(b);
	}


	/**
	 * geht nicht..
	 */
	public static void closeProgram(boolean b)
	{
		if (Common.isRunningProcess() && !CommonGui.getUserConfirmation("process is running, really stop'em ?"))
			return;

		Common.exitApplication(GlobalReturnCode);
	}

	/**
	 * 
	 */
	public static void setFrameTitle(String str)
	{
		frame.setTitle(str);
	}

	/**
	 * 
	 */
	public static void resetFrameTitle()
	{
		setFrameTitle(frametitle);
	}

	/**
	 * 
	 */
	public static Rectangle getFrameBounds()
	{
		return frame.getBounds();
	}

	/**
	 * 
	 */
	public static void minimize()
	{
		frame.setState(frame.ICONIFIED);
	}

	/**
	 *
	 */
	public static void showActiveCollection(int index)
	{
		comboBox_0.setSelectedIndex(index);
	}

	/**
	 *
	 */
	public static void updateCollectionPanel(int index)
	{
		collection_panel.entry(index);
	}
}