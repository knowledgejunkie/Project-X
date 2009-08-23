/*
 * @(#)MainFrame.java - holds main gui
 *
 * Copyright (c) 2001-2009 by dvb.matt, All rights reserved.
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
import java.util.Collections;

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
import java.net.URLDecoder;

import net.sourceforge.dvb.projectx.net.X_URLDecoder;

import net.sourceforge.dvb.projectx.parser.CommonParsing;
import net.sourceforge.dvb.projectx.parser.MainProcess;
import net.sourceforge.dvb.projectx.parser.HpFix;
import net.sourceforge.dvb.projectx.parser.StripAudio;
import net.sourceforge.dvb.projectx.parser.StripRelook;
import net.sourceforge.dvb.projectx.parser.StripMedion;

import net.sourceforge.dvb.projectx.xinput.DirType;
import net.sourceforge.dvb.projectx.xinput.XInputDirectory;
import net.sourceforge.dvb.projectx.xinput.XInputFile;

import net.sourceforge.dvb.projectx.video.Preview;

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
import net.sourceforge.dvb.projectx.gui.GOPEditor;


import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.ClipboardOwner;

import java.io.FilenameFilter;
//import org.apache.oro.io.RegexFilenameFilter;

/**
 *
 */
public class MainFrame extends JPanel {

	private static String frametitle = "";

	private static int GlobalReturnCode = 0;

	private static boolean SilentAction = true;

	//create empty table
	private static Object[][] FileObjectTable = new Object[10][12];

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
	private JPopupMenu popup;
	private JFrame autoload;

	private PatchDialog patch_panel;

	private int loadSizeForward = 1024000;

	private Preview Preview = new Preview(loadSizeForward);

	private static GOPEditor gop_editor;

	/**
	 * copy fileinfo to clipboard, see popup, menulistener
	 */
	private static ClipboardOwner defaultClipboardOwner = new ClipboardObserver();

	static class ClipboardObserver implements ClipboardOwner {
		public void lostOwnership(Clipboard clipboard, Transferable contents)
		{}
	}

		private DropTargetListener dnd1Listener = new DropTargetListener()
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
					List list = new ArrayList();

					Object obj = df.length == 0 ? new Object() : tr.getTransferData(df[0]);

					String eyetvCollectionName = null;
		
					if (obj instanceof java.util.List)
					{
						list = (java.util.List) obj;

						Collections.sort(list);

						// Replace dropped File objects by XInputFile objects
						ArrayList tempList = new ArrayList();

						for (int i = 0; i < list.size(); i++)
							tempList.add(new XInputFile((File)list.get(i)));

						list = tempList;
					}

					else if (obj instanceof URL)
					{
						// MacOsX tiger returns one Url instead of a file list, works only without host specification of the file
						URL url = (URL) obj;

						String protocol = url.getProtocol();

						if (protocol.equals("file"))
						{
							//File f = new File(URLDecoder.decode(url.getFile()));
							File f = new File(X_URLDecoder.decode(url.getFile(), "UTF-8"));

							if (f.isDirectory() && f.getName().endsWith(".eyetv"))
							{
								// handle EyeTV recording directory
								// output filename will be the directory name
								// filename is to be found inside the directory as ".mpg"
								String name = f.getName();
								name = name.substring(0, name.lastIndexOf(".eyetv"));
								File[] theMPGFile = new File[1];
								
								
								theMPGFile = f.listFiles(new FilenameFilter() {
									public boolean accept(File f, String s) {
										return s.toUpperCase().endsWith(".MPG");
									}
								});
								
								if(theMPGFile.length != 0)
								{
									list.add( new XInputFile(theMPGFile[0].getAbsoluteFile()));
									eyetvCollectionName = name + ".mpg";
								}
							}

							else if (f.exists())
							{
								list.add(new XInputFile(f));

								eyetvCollectionName = null;
							}

							else
								Common.setOSDErrorMessage("dropped File Object(s) not accessible.. : " + url.toString());
						}

						else if (protocol.equals("ftp"))
						{
							XInputDirectory xid = new XInputDirectory(url);
							XInputFile[] xif = xid.getFiles();

							int i = 0;
							for (; i < xif.length; i++)
								if ( new URL(xif[i].toString()).getFile().equals(url.getFile()) )
								{
									list.add(xif[i]);
									break;
								}

							if (i >= xif.length)
								Common.setOSDErrorMessage("File Object not accessible: " + url.toString());
						}
					}

					else
					{
						e.dropComplete(true);
						tableView.setBackground(Color.white);
						Common.setOSDErrorMessage("can't drop Object(s) to Collection.. : " + obj.getClass().getName());

						return;
					}


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

							if(eyetvCollectionName != null)
								collection.setOutputName(eyetvCollectionName);

							updateCollectionTable(collection.getCollectionAsTable());
							updateOutputField(collection);
						}
					}

					else if (dropaction == 2)    // move = one coll
					{
						Object[] val = list.toArray();

						if (val.length > 0)
						{
							Common.addCollection(false);

							JobCollection collection = Common.getCollection(comboBox_0.getSelectedIndex());
							collection.addInputFile(val);

							if(eyetvCollectionName != null)
								collection.setOutputName(eyetvCollectionName);

							updateCollectionTable(collection.getCollectionAsTable());
							updateOutputField(collection);
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
					String str = Common.getSettings().getProperty(Keys.KEY_PostCommands_Cmd3);

					if (str.trim().length() > 0)
						Common.performCommand(str + " \"" + Common.getCollection().getInputFile(index).toString() + "\"");

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

						JobCollection collection = Common.getCollection();

						/**
						 * must use getAbsolutFile to ensure right ClassType,
						 * sometimes the returned Object.getClass
						 * from selection is NOT of java.io.File!!
						 */
	 					for (int i = 0; i < theFiles.length; i++)
						{
							if (theFiles[i].isFile())
								collection.addInputFile( new XInputFile(theFiles[i].getAbsoluteFile()));

							else if (theFiles[i].isDirectory() && theFiles[i].getName().endsWith(".eyetv"))
							{
								// handle EyeTV recording directory
								// output filename will be the directory name
								// filename is to be found inside the directory as ".mpg"
								String name = theFiles[i].getName();
								name = name.substring(0, name.lastIndexOf(".eyetv"));
								File[] theMPGFile = new File[1];
								
								
								theMPGFile = theFiles[i].listFiles(new FilenameFilter() {
									public boolean accept(File f, String s) {
										return s.toUpperCase().endsWith(".MPG");
									}
								});
								if(theMPGFile.length != 0)
								{
									collection.addInputFile( new XInputFile(theMPGFile[0].getAbsoluteFile()));
									collection.setOutputName(name + ".mpg");
								}
							}

 							else
								Common.setOSDErrorMessage("File Object not accessible.. : " + theFiles[i].toString());
						}

						updateCollectionTable(collection.getCollectionAsTable());
						updateCollectionPanel(Common.getActiveCollection());
						updateOutputField(collection);
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

				JobCollection collection = Common.getCollection();

				if (index < 0 || index >= collection.getInputFilesCount())
					return;

				String name = ((XInputFile) collection.getInputFiles()[0]).getName();

				String newoutname = CommonGui.getUserInput( name, Resource.getString("popup.newOutName") + " " + name, collection.getOutputName());

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
					JobCollection collection = Common.getCollection();

					if (collection == null)
						return;

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

				JobCollection collection = Common.getCollection();

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

				JobCollection collection = Common.getCollection();

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

				JobCollection collection = Common.getCollection();

				XInputFile xInputFile = ((XInputFile) collection.getInputFile(index)).getNewInstance();

				if (xInputFile != null && xInputFile.exists())
					new HexViewer().view(xInputFile);
			}

			/**
			 *
			 */
			else if (actName.equals("viewInSupViewer"))
			{
				int index = tableView.getSelectedRow();

				if (index < 0 || tableView.getValueAt(index, 0) == null)
					return;

				JobCollection collection = Common.getCollection();

				XInputFile xInputFile = ((XInputFile) collection.getInputFile(index)).getNewInstance();

				if (xInputFile != null && xInputFile.exists() && xInputFile.getStreamInfo().getStreamType() == CommonParsing.ES_SUP_TYPE)
					CommonGui.getSubpictureFrame().loadPreview(xInputFile);
			}

			/**
			 *
			 */
			else if (actName.equals("fixHpAc3"))
			{
				int index = tableView.getSelectedRow();

				if (index < 0 || tableView.getValueAt(index, 0) == null)
					return;

				JobCollection collection = Common.getCollection();

				XInputFile xInputFile = ((XInputFile) collection.getInputFile(index)).getNewInstance();

				if (xInputFile != null && xInputFile.exists() && CommonGui.getUserConfirmation("really process '" + xInputFile.getName() + "' ?"))
				{
					HpFix hpfix = new HpFix();

					Common.setOSDMessage("fixing wrong Hp Ac3 File...");

					xInputFile = hpfix.process(xInputFile);

					collection.removeInputFile(index);

					if (xInputFile != null)
						collection.addInputFile(index, xInputFile);

					updateCollectionTable(collection.getCollectionAsTable());
					updateCollectionPanel(Common.getActiveCollection());

					tableView.clearSelection();
				}
			}

			/**
			 *
			 */
			else if (actName.equals("stripAudio"))
			{
				int index = tableView.getSelectedRow();

				if (index < 0 || tableView.getValueAt(index, 0) == null)
					return;

				JobCollection collection = Common.getCollection();

				XInputFile xInputFile = ((XInputFile) collection.getInputFile(index)).getNewInstance();

				if (xInputFile != null && xInputFile.exists() && xInputFile.getStreamInfo().getStreamType() == CommonParsing.ES_RIFF_TYPE && CommonGui.getUserConfirmation("really process '" + xInputFile.getName() + "' ?"))
				{
					StripAudio stripAudio = new StripAudio();

					Common.setOSDMessage("strip audio data...");

					xInputFile = stripAudio.process(xInputFile);

					collection.removeInputFile(index);

					if (xInputFile != null)
						collection.addInputFile(index, xInputFile);

					updateCollectionTable(collection.getCollectionAsTable());
					updateCollectionPanel(Common.getActiveCollection());

					tableView.clearSelection();
				}
			}

			/**
			 *
			 */
			else if (actName.equals("FileProperties"))
			{
				int index = tableView.getSelectedRow();

				if (index < 0 || tableView.getValueAt(index, 0) == null)
					return;

				JobCollection collection = Common.getCollection();

				XInputFile xInputFile = (XInputFile) collection.getInputFile(index);

				if (xInputFile != null && xInputFile.exists())
				{
					CommonGui.getFileProperties().open(xInputFile, Common.getActiveCollection(), index);

					updateCollectionTable(collection.getCollectionAsTable());
					updateCollectionPanel(Common.getActiveCollection());

					tableView.clearSelection();
				}

			}

			/**
			 *
			 */
			else if (actName.equals("CollectionProperties"))
			{
				if (Common.isCollectionListEmpty())
					return;

				CommonGui.getCollectionProperties().open(Common.getCollection(), Common.getActiveCollection());
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
			else if (actName.equals("editBasics"))
			{
				int index = tableView.getSelectedRow();

				if (index < 0 || tableView.getValueAt(index, 0) == null)
					return;

				JobCollection collection = Common.getCollection();

				XInputFile xInputFile = (XInputFile) collection.getInputFile(index);

				if (patch_panel == null)
					patch_panel = new PatchDialog(frame);

				if (patch_panel.entry(xInputFile))
				{
					getScanInfo(xInputFile, xInputFile.getStreamInfo().getStreamType());
					updateCollectionTable(collection.getCollectionAsTable());
					updateCollectionPanel(Common.getActiveCollection());
				}
			}

			/**
			 *
			 */
			else if (actName.equals("clipboard"))
			{
				int index = tableView.getSelectedRow();

				if (index < 0 || tableView.getValueAt(index, 0) == null)
					return;

				JobCollection collection = Common.getCollection();

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

				Common.getCollection().setActionType(val);
			}

			/**
			 *
			 */
			else if (actName.equals("assignStreamtype"))
			{
				int index = tableView.getSelectedRow();

				if (index < 0 || tableView.getValueAt(index, 0) == null)
					return;

				JobCollection collection = Common.getCollection();

				XInputFile xInputFile = (XInputFile) collection.getInputFile(index);

				Object[] items = Keys.ITEMS_FileTypes;
				String str = ((JMenuItem) e.getSource()).getText();

				for (int i = 0; i < items.length; i++)
				{
					if (str.equals(items[i].toString()))
					{
						if (xInputFile.getStreamInfo() == null)
							getScanInfo(xInputFile);

						xInputFile.getStreamInfo().setStreamType(i);
						getScanInfo(xInputFile, i);

						updateCollectionTable(collection.getCollectionAsTable());
						updateCollectionPanel(Common.getActiveCollection());

						return;
					}
				}

				xInputFile.setStreamInfo(null);
				getScanInfo(xInputFile);

				updateCollectionTable(collection.getCollectionAsTable());
				updateCollectionPanel(Common.getActiveCollection());
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
							File f = new File(url.getHost() + url.getFile());

							if (!f.exists())
								f = new File(url.getFile());

							if (f.exists())
								inputValue = new XInputFile(f);

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

				JobCollection collection = Common.getCollection();

				collection.addInputFile(inputValue);

				updateCollectionTable(collection.getCollectionAsTable());
				updateCollectionPanel(Common.getActiveCollection());

				return;
			}
		}

		/**
		 *
		 */
		private void stripRelook(int type)
		{
			int index = tableView.getSelectedRow();

			if (index < 0 || tableView.getValueAt(index, 0) == null)
				return;

			JobCollection collection = Common.getCollection();

			XInputFile xInputFile = ((XInputFile) collection.getInputFile(index)).getNewInstance();

			if (xInputFile != null && xInputFile.exists() && xInputFile.getStreamInfo().getStreamType() == CommonParsing.PES_AV_TYPE && CommonGui.getUserConfirmation("really process '" + xInputFile.getName() + "' ?"))
			{
				StripRelook stripRelook = new StripRelook(type);

				Common.setOSDMessage("strip Relook® data, type " + type + "...");

				XInputFile[] xif = stripRelook.process(xInputFile, collection.getOutputDirectory());

				collection.removeInputFile(index);

				if (xif != null)
				{
					for (int i = 0, j = index; i < xif.length; i++)
					{
						if (xif[i] != null)
							collection.addInputFile(j++, xif[i]);
					}
				}

				updateCollectionTable(collection.getCollectionAsTable());
				updateCollectionPanel(Common.getActiveCollection());

				tableView.clearSelection();
			}
		}

		/**
		 *
		 */
		private void stripMedion()
		{
			int index = tableView.getSelectedRow();

			if (index < 0 || tableView.getValueAt(index, 0) == null)
				return;

			JobCollection collection = Common.getCollection();

			XInputFile xInputFile = ((XInputFile) collection.getInputFile(index)).getNewInstance();

			if (xInputFile != null && xInputFile.exists() && xInputFile.getStreamInfo().getStreamType() == CommonParsing.PES_AV_TYPE && CommonGui.getUserConfirmation("really process '" + xInputFile.getName() + "' ?"))
			{
				StripMedion stripMedion = new StripMedion();

				Common.setOSDMessage("strip Medion® data...");

				XInputFile[] xif = stripMedion.process(xInputFile, collection.getOutputDirectory());

				collection.removeInputFile(index);

				if (xif != null)
				{
					for (int i = 0, j = index; i < xif.length; i++)
					{
						if (xif[i] != null)
							collection.addInputFile(j++, xif[i]);
					}
				}

				updateCollectionTable(collection.getCollectionAsTable());
				updateCollectionPanel(Common.getActiveCollection());

				tableView.clearSelection();
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
		FileObjectTable = objects == null ? new Object[10][12] : objects;

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
		menuitem_8.addActionListener(_MenuListener);

		JMenuItem menuitem_2 = new JMenuItem();
		CommonGui.localize(menuitem_2, "file.add");
		menuitem_2.setActionCommand("add");
		menuitem_2.addActionListener(_MenuListener);
		popup.add(menuitem_2);

		JMenuItem menuitem_3 = new JMenuItem();
		CommonGui.localize(menuitem_3, "file.remove");
		menuitem_3.setActionCommand("remove");
		menuitem_3.addActionListener(_MenuListener);
		popup.add(menuitem_3);

		popup.addSeparator();

		JMenuItem menuitem_5 = popup.add(Resource.getString("popup.openhex"));
		menuitem_5.setActionCommand("viewAsHex");
		menuitem_5.addActionListener(_MenuListener);

		JMenuItem menuitem_6 = popup.add(Resource.getString("popup.opensup"));
		menuitem_6.setActionCommand("viewInSupViewer");
		menuitem_6.addActionListener(_MenuListener);

		JMenuItem menuitem_7 = popup.add(Resource.getString("popup.sendtocl3"));
		menuitem_7.setActionCommand("sendTo3");
		menuitem_7.addActionListener(_MenuListener);

		JMenuItem menuitem_11 = popup.add(Resource.getString("popup.copyInfoToClipboard"));
		menuitem_11.setActionCommand("clipboard");
		menuitem_11.addActionListener(_MenuListener);

		popup.addSeparator();

		JMenuItem menuitem_17 = popup.add(Resource.getString("General.FileProperties") + "..");
		menuitem_17.setActionCommand("FileProperties");
		menuitem_17.addActionListener(_MenuListener);

		popup.addSeparator();

		JMenuItem menuitem_9 = popup.add(Resource.getString("popup.newOutName"));
		menuitem_9.setActionCommand("newOutName");
		menuitem_9.addActionListener(_MenuListener);


		/**
		 *
		 */
		Object[] objects = Keys.ITEMS_ConversionMode;

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

		popup.addSeparator();

		//coll properties
		JMenuItem menuitem_18 = popup.add(Resource.getString("General.CollectionProperties") + "..");
		menuitem_18.setActionCommand("CollectionProperties");
		menuitem_18.addActionListener(_MenuListener);


		popup.pack();

		UIManager.addPropertyChangeListener(new UISwitchListener(popup));
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
				Common.setErrorMessage("!> Could not load LookAndFeel: " + lnfName);
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
		hex.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, ActionEvent.CTRL_MASK));

		preview.add(hex);
		preview.addSeparator();

		JMenuItem basic = new JMenuItem();
		CommonGui.localize(basic, "options.pachtbasics");
		basic.setActionCommand("editBasics");
		basic.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, ActionEvent.CTRL_MASK));

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
		subtitle.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, ActionEvent.CTRL_MASK));

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
		pagematrix.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, ActionEvent.CTRL_MASK));

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
					new Html("http://project-x.sourceforge.net/optional/resources/").show();
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
			"ID",
			Resource.getString("CollectionTable.Source"),
			"#",
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

		renderer_1.setHorizontalAlignment(JLabel.RIGHT);
		renderer_2.setHorizontalAlignment(JLabel.CENTER);

		tableView.setRowHeight(15);
		tableView.setGridColor(new Color(220, 220, 220));
		tableView.removeEditor();
		tableView.setToolTipText(Resource.getString("FilePanel.DragDrop.Tip"));
		tableView.setSelectionMode(2);
		tableView.setSelectionBackground(new Color(220, 220, 255));
		tableView.setSelectionForeground(Color.black);

		tableView.getColumn(names[2]).setCellRenderer(renderer_2);
		tableView.getColumn(names[2]).setMaxWidth(25);

		tableView.getColumn(names[0]).setCellRenderer(renderer_2);
		tableView.getColumn(names[0]).setMaxWidth(25);

		tableView.getColumn(names[1]).setCellRenderer(renderer_2);
		tableView.getColumn(names[1]).setMinWidth(32);
		tableView.getColumn(names[1]).setMaxWidth(32);

		tableView.getColumn(names[3]).setMinWidth(165); //200
		tableView.getColumn(names[4]).setMinWidth(160);

		tableView.getColumn(names[5]).setCellRenderer(renderer_1);
		tableView.getColumn(names[5]).setMinWidth(62);
		tableView.getColumn(names[5]).setMaxWidth(62);

		tableView.getColumn(names[6]).setCellRenderer(renderer_2);
		tableView.getColumn(names[6]).setMinWidth(100);
		tableView.getColumn(names[6]).setMaxWidth(100);

		for (int i = 7; i < 11; i++)
		{
			tableView.getColumn(names[i]).setCellRenderer(renderer_2);
			tableView.getColumn(names[i]).setMinWidth(16);
			tableView.getColumn(names[i]).setMaxWidth(16);
		}

        tableView.getColumn(names[11]).setMinWidth(90);

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

					for (int i = 3; i < 8; i++)
						elements[i].getComponent().setEnabled(row >= 0);

					for (int i = 8; i < elements.length; i++)
						elements[i].getComponent().setEnabled(index >= 0);

					popup.show(tableView, e.getX(), e.getY() - popup.getHeight());
				}

				else if (row >= 0)
					getScanInfo((XInputFile) Common.getCollection(index).getInputFile(row));

				if (e.getClickCount() >= 2 && e.getModifiers() == MouseEvent.BUTTON1_MASK && !Common.isCollectionListEmpty() && row >= 0)
					CommonGui.getFileProperties().open((XInputFile) Common.getCollection(index).getInputFile(row), Common.getActiveCollection(), row);
			}
		});

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
		JButton open_autoload = new JButton(CommonGui.loadIcon("openmulti.gif"));
		open_autoload.setPreferredSize(new Dimension(30, 22));
		open_autoload.setMaximumSize(new Dimension(30, 22));
		open_autoload.setToolTipText(Resource.getString("FilePanel.openAutoloadPanel.Tip"));
		open_autoload.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				autoload.setState(0);
				autoload.show();
			}
		});
		panel_1.add(open_autoload);

		/**
		 * add
		 */
		JButton file_add = new JButton(CommonGui.loadIcon("open.gif"));
		file_add.setPreferredSize(new Dimension(30, 22));
		file_add.setMaximumSize(new Dimension(30, 22));
		file_add.setToolTipText(Resource.getString("FilePanel.FileAdd.Tip"));
		file_add.setActionCommand("add");
		file_add.addActionListener(_MenuListener);
		panel_1.add(file_add);

		/**
		 * remove
		 */
		JButton file_remove = new JButton(CommonGui.loadIcon("remove.gif"));
		file_remove.setPreferredSize(new Dimension(30, 22));
		file_remove.setMaximumSize(new Dimension(30, 22));
		file_remove.setToolTipText(Resource.getString("FilePanel.FileRemove.Tip"));
		file_remove.setActionCommand("remove");
		file_remove.addActionListener(_MenuListener);
		panel_1.add(file_remove);

		/**
		 * up
		 */
		JButton file_up = new JButton(CommonGui.loadIcon("up2.gif"));
	//	JButton file_up = new JButton(CommonGui.loadIcon("up.gif"));
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

					if (collection == null)
						return;

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
		JButton file_down = new JButton(CommonGui.loadIcon("down.gif"));
	//	JButton file_down = new JButton(CommonGui.loadIcon("dn.gif"));
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

					if (collection == null)
						return;

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
					Common.getSettings().setProperty(Keys.KEY_OutputDirectory[0], comboBox_13.getSelectedItem());

				if (comboBox_0.getItemCount() == 0)
					return;

				Common.setActiveCollection(comboBox_0.getSelectedIndex());

				JobCollection collection = Common.getCollection();

				collection.setOutputDirectory(Common.getSettings().getProperty(Keys.KEY_OutputDirectory));

				updateOutputField(collection);

				updateCollectionTable(collection.getCollectionAsTable());
			}
		});

		/**
		 *
		 */
		JButton add_output = new JButton(CommonGui.loadIcon("open.gif"));
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
		JButton remove_output = new JButton(CommonGui.loadIcon("remove.gif"));
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
		 * table + output dir's
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

		panel.setPreferredSize(new Dimension(860, 114));
		panel.setMaximumSize(new Dimension(860, 114));
		panel.setMinimumSize(new Dimension(860, 114));

		return panel;
	}


	/**
	 *
	 */
	private void closeAutoloadPanel()
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
				closeAutoloadPanel();
			}
		});

		JMenu fileMenu = new JMenu();
		CommonGui.localize(fileMenu, "Common.File");

		JMenuItem closemenu = new JMenuItem();
		CommonGui.localize(closemenu, "Common.Close");
		closemenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.ALT_MASK));
		closemenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				closeAutoloadPanel();
			}
		});

		fileMenu.add(closemenu);


		JMenu editMenu = new JMenu();
		CommonGui.localize(editMenu, "Common.Edit");

		JCheckBoxMenuItem subdir = new JCheckBoxMenuItem(Resource.getString(Keys.KEY_InputDirectoriesDepth[0]));
		subdir.setToolTipText(Resource.getString(Keys.KEY_InputDirectoriesDepth[0] + Keys.KEY_Tip));
		subdir.setActionCommand(Keys.KEY_InputDirectoriesDepth[0]);
		subdir.setState(Common.getSettings().getBooleanProperty(Keys.KEY_InputDirectoriesDepth));
		subdir.addActionListener(_BoxListener);

		editMenu.add(subdir);

		/**
		 *
		 */
		JMenuBar menuBar = new JMenuBar();
		menuBar.add(fileMenu);
		menuBar.add(editMenu);
		autoload.setJMenuBar(menuBar);


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
		JButton remove_input = new JButton(CommonGui.loadIcon("remove.gif"));
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
		JButton add_input = new JButton(CommonGui.loadIcon("open.gif"));
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
		JButton add_inputftp = new JButton(CommonGui.loadIcon("openftp.gif"));
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
				}

				autoload.setState(0);
				autoload.toFront();
			}
		});
		bb.add(add_inputftp);

		/**
		 *
		 */
		JButton refresh_list = new JButton(CommonGui.loadIcon("refresh.gif"));
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
		JButton add_coll_and_1file = new JButton(CommonGui.loadIcon("addcollfile.gif"));
		add_coll_and_1file.setPreferredSize(new Dimension(50,28));
		add_coll_and_1file.setMaximumSize(new Dimension(50,28));
		add_coll_and_1file.setToolTipText(Resource.getString("autoload.add.coll.tip"));
		add_coll_and_1file.addActionListener(new ActionListener() {
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

				autoload.toFront();
			}
		});
		bb.add(add_coll_and_1file);

		/**
		 *
		 */
		JButton add_coll_and_files = new JButton(CommonGui.loadIcon("addcollfiles.gif"));
		add_coll_and_files.setPreferredSize(new Dimension(50,28));
		add_coll_and_files.setMaximumSize(new Dimension(50,28));
		add_coll_and_files.setToolTipText(Resource.getString("autoload.add.coll2.tip"));
		add_coll_and_files.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				Object[] val = list1.getSelectedValues();

				if (val.length == 0)
					return;

				/**
				 * create new collection add all selected files
				 */
				JobCollection collection = Common.addCollection();

				collection.addInputFile(val);

				updateCollectionTable(collection.getCollectionAsTable());
				updateCollectionPanel(Common.getActiveCollection());

				autoload.toFront();
			}
		});
		bb.add(add_coll_and_files);

		/**
		 *
		 */
		JButton add_files = new JButton(CommonGui.loadIcon("addfile.gif"));
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

					autoload.toFront();
				}
			}
		});
		bb.add(add_files);


		bb.add(new JLabel(" "));

		/**
		 *
		 */
		JButton close = new JButton(CommonGui.loadIcon("close.gif"));
		close.setPreferredSize(new Dimension(50,28));
		close.setMaximumSize(new Dimension(50,28));
		close.setToolTipText(Resource.getString("autoload.close"));
		close.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				closeAutoloadPanel();
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

						autoload.toFront();
					}
				}

				else if (e.getClickCount() == 1)
				{
					if (list1.getSelectedValue() != null )
						getScanInfo( (XInputFile) list1.getSelectedValue());
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

					autoload.toFront();
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

		autoload.setBounds(200, 200, 700, 350);
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
	//	panel_1.setLayout(new ColumnLayout());
		panel_1.setLayout(new BorderLayout());

		MemoryMonitor memo = new MemoryMonitor();

		if (Common.showGUI())
			memo.surf.start();

		panel_1.add(memo, BorderLayout.NORTH);

		JPanel panel_3 = new JPanel();
		panel_3.setLayout(new ColumnLayout());
		panel_3.add(buildProcessControlPanel());
		panel_3.add(buildCollectionControlPanel());

		panel_1.add(panel_3, BorderLayout.SOUTH);

		//panel_1.setPreferredSize(new Dimension(115, 362));
		//panel_1.setMaximumSize(new Dimension(115, 362));
		//panel_1.setMinimumSize(new Dimension(115, 362));

		/**
		 *
		 */
		JPanel panel_2 = new JPanel();
	//	panel_2.setLayout(new BoxLayout(panel_2, BoxLayout.X_AXIS));
		panel_2.setLayout(new BorderLayout());

		panel_2.add(panel_1, BorderLayout.WEST);
		panel_2.add(new CollectionPanel(), BorderLayout.CENTER);

		//panel_2.setPreferredSize(new Dimension(860, 362));
		//panel_2.setMaximumSize(new Dimension(860, 362));
		//panel_2.setMinimumSize(new Dimension(860, 362));

		/**
		 *
		 */
		JPanel panel = new JPanel();
	//	panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setLayout(new BorderLayout());

		panel.add(panel_2, BorderLayout.CENTER);
		panel.add(buildFilePanel1(), BorderLayout.SOUTH);

		return panel;
	}

	/**
	 *
	 */
	protected JPanel buildFilePanel1()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());

		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.setTabPlacement(SwingConstants.BOTTOM);

		tabbedPane.addTab(Resource.getString("General.FileTable"), buildFilePanel());
		tabbedPane.addTab(Resource.getString("General.CutControl"), CommonGui.getCutPanel());
		tabbedPane.addTab(Resource.getString("General.FilterControl"), new FilterPanel());

		panel.add(CommonGui.getCutPanel().getSliderPanel(), BorderLayout.NORTH);
		panel.add(tabbedPane);

		return panel;
	}

	/**
	 *
	 */
	protected JPanel buildProcessControlPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBorder(BorderFactory.createTitledBorder( BorderFactory.createEtchedBorder(), Resource.getString("MainPanel.Process")));

		/**
		 *
		 */
		JButton process = new JButton(Resource.getString("MainPanel.QuickStart"));
		process.setToolTipText(Resource.getString("MainPanel.QuickStart.Tip"));
		process.setMnemonic('q');
		process.setPreferredSize(new Dimension(120, 24));
		process.setMaximumSize(new Dimension(120, 24));
		process.setMinimumSize(new Dimension(120, 24));
		process.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				if (Common.startProcess())
					Common.startMainProcess();
			}
		});

		panel.add(process);

		panel.add(Box.createRigidArea(new Dimension(1, 5)));

		/**
		 * process window open
		 */
		JButton processwindow = new JButton(Resource.getString("ProcessWindowPanel.Button"));
		processwindow.setPreferredSize(new Dimension(120, 24));
		processwindow.setMaximumSize(new Dimension(120, 24));
		processwindow.setMinimumSize(new Dimension(120, 24));
		processwindow.setToolTipText(Resource.getString("MainPanel.Process") + " " + Resource.getString("ProcessWindowPanel.Button"));
		processwindow.setMnemonic('p');
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
		final Color settings_color = new Color(245, 245, 215);

		/**
		 *  info field
		 */
		final JTextArea textarea = new JTextArea();
		textarea.setToolTipText(Resource.getString("FilePanel.Textfield.Tip"));
		textarea.setBackground(idle_color);
		textarea.setFont(new Font("Tahoma", Font.PLAIN, 11));
		textarea.setEditable(false);

		DropTarget dropTarget_1 = new DropTarget(textarea, dnd1Listener);

		JPanel panel_2 = new JPanel();
		panel_2.setLayout(new GridLayout(1,1));
		panel_2.setBorder(BorderFactory.createLoweredBevelBorder());
		panel_2.setPreferredSize(new Dimension(120, 138));
		panel_2.setMaximumSize(new Dimension(120, 138));
		panel_2.setMinimumSize(new Dimension(120, 138));
		panel_2.add(textarea);

		panel.add(panel_2);

		/**
		 *  collection label
		 */
		JLabel coll_label = new JLabel(Resource.getString("FilePanel.CollectionNumber"));
		coll_label.setPreferredSize(new Dimension(60, 24));
		coll_label.setMaximumSize(new Dimension(60, 24));
		coll_label.setHorizontalAlignment(SwingConstants.CENTER);
		coll_label.setToolTipText(Resource.getString("FilePanel.CollectionNumber.Tip"));

		/**
		 *  number of act. coll.
		 */
		comboBox_0 = new JComboBox();
		comboBox_0.setPreferredSize(new Dimension(60, 24));
		comboBox_0.setMaximumSize(new Dimension(60, 24));
		comboBox_0.setMinimumSize(new Dimension(60, 24));
		comboBox_0.setMaximumRowCount(6);
		comboBox_0.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				if (comboBox_0.getItemCount() > 0)
				{
					Common.setActiveCollection(comboBox_0.getSelectedIndex());

					JobCollection collection = Common.getCollection();

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
		 *  remove collection
		 */
		JButton remove_coll = new JButton(CommonGui.loadIcon("remcoll.gif"));
		remove_coll.setPreferredSize(new Dimension(60, 24));
		remove_coll.setMaximumSize(new Dimension(60, 24));
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

				if (!Common.isCollectionListEmpty() && index >= comboBox_0.getItemCount())
					comboBox_0.setSelectedIndex(comboBox_0.getItemCount() - 1);

				if (Common.isCollectionListEmpty())
					updateCollectionPanel(-1);
			}
		});

		/**
		 *  add collection
		 */
		JButton add_coll = new JButton(CommonGui.loadIcon("addcoll.gif"));
		add_coll.setPreferredSize(new Dimension(60, 24));
		add_coll.setMaximumSize(new Dimension(60, 24));
		add_coll.setToolTipText(Resource.getString("FilePanel.addCollection.Tip"));
		add_coll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				Common.addCollection();

				updateCollectionPanel(Common.getActiveCollection());
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
				JobCollection collection = Common.getCollection();

				String str = collection == null ? Resource.getString("JobCollection.NoInfo") : collection.getShortSummary();

				if (text.equals(str))
					return;

				text = str;

				textarea.setText(text);

				if (collection != null && collection.isActive())
					textarea.setBackground(running_color);

				else if (collection != null && collection.hasSettings())
					textarea.setBackground(settings_color);

				else
					textarea.setBackground(idle_color);
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

		final JLabel settings = new JLabel(CommonGui.loadIcon("save_yes.gif"));
		settings.setToolTipText("do or don't save settings on exit");
		settings.setEnabled(false);

		final JLabel date = new JLabel();
		final JLabel time = new JLabel();

		final JLabel onlineIcon = new JLabel("OFF");
		onlineIcon.setToolTipText("WebIF online status");

		final DateFormat date_format = DateFormat.getDateInstance(DateFormat.LONG);
		final DateFormat time_format = DateFormat.getTimeInstance(DateFormat.LONG);


		class Clock implements Runnable {
			private Thread clockThread = null;

			private String StatusString = "";
			private String DateString = "";
			private boolean SaveSettings = false;
			private boolean WebIFisOnline = false;

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
				updateWebIFLabel();
				updateSettingsLabel();
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

			private void updateWebIFLabel()
			{
				boolean b = Common.isWebServerOnline();

				if (b == WebIFisOnline)
					return;

				WebIFisOnline = b;

				onlineIcon.setText(WebIFisOnline ? "ON" : "OFF");
			}

			private void updateSettingsLabel()
			{
				boolean b = Common.getSettings().getBooleanProperty(Keys.KEY_SaveSettingsOnExit);

				if (b == SaveSettings)
					return;

				SaveSettings = b;

				settings.setEnabled(SaveSettings);
			//	settings.setIcon(CommonGui.loadIcon(SaveSettings ? "save_yes.gif" : "save_no.gif"));
			}

			private void updateDateLabel()
			{
				String str = date_format.format(new Date());

				if (str.equals(DateString))
					return;

				DateString = str;

				date.setText(DateString);
			}

			private void updateTimeLabel()
			{
				time.setText(time_format.format(new Date()));
			}

			public void stop()
			{
				clockThread = null;
			}
		}

		new Clock().start();

		JPanel status_1 = new JPanel(new BorderLayout());
		status_1.setBorder(BorderFactory.createLoweredBevelBorder());
		status_1.setPreferredSize(new Dimension(580, 22));
		status_1.setMaximumSize(new Dimension(580, 22));
		status_1.add(status);

		JPanel status_2 = new JPanel(new BorderLayout());
		status_2.setBorder(BorderFactory.createLoweredBevelBorder());
		status_2.setPreferredSize(new Dimension(30, 22));
		status_2.setMaximumSize(new Dimension(30, 22));
		status_2.add(onlineIcon);

		JPanel status_3 = new JPanel(new BorderLayout());
		status_3.setBorder(BorderFactory.createLoweredBevelBorder());
		status_3.setPreferredSize(new Dimension(30, 22));
		status_3.setMaximumSize(new Dimension(30, 22));
		status_3.add(settings);

		JPanel status_4 = new JPanel(new BorderLayout());
		status_4.setBorder(BorderFactory.createLoweredBevelBorder());
		status_4.setPreferredSize(new Dimension(130, 22));
		status_4.setMaximumSize(new Dimension(130, 22));
		status_4.add(date);

		JPanel status_5 = new JPanel(new BorderLayout());
		status_5.setBorder(BorderFactory.createLoweredBevelBorder());
		status_5.setPreferredSize(new Dimension(130, 22));
		status_5.setMaximumSize(new Dimension(130, 22));
		status_5.add(time);

		JPanel mainStatusPanel = new JPanel();
		mainStatusPanel.setLayout(new BoxLayout(mainStatusPanel, BoxLayout.X_AXIS));
		mainStatusPanel.add(status_1);
		mainStatusPanel.add(status_2);
		mainStatusPanel.add(status_3);
		mainStatusPanel.add(status_4);
		mainStatusPanel.add(status_5);

		return mainStatusPanel;
	}

	/**
	 * show ScanInfos
 	 */
	public void getScanInfo(XInputFile aXInputFile)
	{
		getScanInfo(aXInputFile, -1);
	}

	/**
	 * show ScanInfos, only directly called from manual stream assignment
 	 */
	public void getScanInfo(XInputFile aXInputFile, int streamtype)
	{
		if (aXInputFile.getStreamInfo() == null || streamtype > -1)
			Common.getScanClass().getStreamInfo(aXInputFile, 0, streamtype);

		if (aXInputFile.getStreamInfo().getThumbnail() == null)
		{
			switch (aXInputFile.getStreamInfo().getStreamType())
			{
				case CommonParsing.PES_AV_TYPE:
				case CommonParsing.MPEG1PS_TYPE:
				case CommonParsing.MPEG2PS_TYPE:
				case CommonParsing.PVA_TYPE:
				case CommonParsing.TS_TYPE:
				case CommonParsing.ES_MPV_TYPE:
					Preview.previewFile(aXInputFile, 0, loadSizeForward, Common.getSettings().getBooleanProperty(Keys.KEY_Preview_AllGops), Common.getSettings().getBooleanProperty(Keys.KEY_Preview_fastDecode), Common.getSettings().getIntProperty(Keys.KEY_Preview_YGain));
					aXInputFile.getStreamInfo().setThumbnail(Common.getMpvDecoderClass().getScaledCutImage());
					break;

				default:
					aXInputFile.getStreamInfo().setThumbnail(new int[0]);
			}
		}

		CommonGui.getPicturePanel().setStreamInfo(aXInputFile.getStreamInfo());
	}

	/**
	 * refresh inputfileslist
	 */
	public void reloadInputDirectories()
	{
		updateAutoloadList(Common.reloadInputDirectories());
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

					Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();

					if (newHeight > preferred.getHeight())
						newWidth = (newHeight - 344) * 16 / 9 + 340;

					if (newWidth > screen.getWidth())
					{
						newWidth = screen.getWidth();
						newHeight = (newWidth - 340) * 9 / 16 + 344;
					}

					else if (newWidth > preferred.getWidth())
						newHeight = (newWidth - 340) * 9 / 16 + 344;

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
	public static void showFrame(boolean b)
	{
		frame.setState(b ? frame.NORMAL : frame.ICONIFIED);
	}

	/**
	 *
	 */
	public static void showActiveCollection(int index)
	{
		if (index >= 0 && index < Common.getCollectionListSize())
			comboBox_0.setSelectedIndex(index);

		if (Common.isCollectionListEmpty())
		{
			Common.setActiveCollection(-1);

			comboBox_0.removeAllItems();

			updateCollectionPanel(-1);
		}
	}

	/**
	 *
	 */
	public static void updateCollectionPanel(int index)
	{
		CommonGui.getCutPanel().entry(index);
	}

	/**
	 *
	 */
	public static byte[] editGOP(byte[] data, long[][] pts_indices)
	{
		if (gop_editor == null)
			gop_editor = new GOPEditor(frame);

		return gop_editor.editGOP(data, pts_indices);
	}
}
