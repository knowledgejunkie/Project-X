/*
 * @(#)CollectionPanel
 *
 * Copyright (c) 2001-2005 by dvb.matt, All Rights Reserved. 
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Comparator;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JComboBox;
import javax.swing.JScrollPane;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.JComponent;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import net.sourceforge.dvb.projectx.common.Keys;
import net.sourceforge.dvb.projectx.common.Common;
import net.sourceforge.dvb.projectx.common.Resource;
import net.sourceforge.dvb.projectx.common.JobCollection;

import net.sourceforge.dvb.projectx.parser.CommonParsing;

import net.sourceforge.dvb.projectx.video.Preview;
import net.sourceforge.dvb.projectx.video.PreviewObject;

import net.sourceforge.dvb.projectx.xinput.XInputFile;

import net.sourceforge.dvb.projectx.gui.ComboBoxIndexListener;
import net.sourceforge.dvb.projectx.gui.ComboBoxItemListener;
import net.sourceforge.dvb.projectx.gui.CheckBoxListener;
import net.sourceforge.dvb.projectx.gui.CommonGui;

/**
 * collection panel
 */
public class CollectionPanel extends JPanel {

	private X_JFileChooser chooser;

	private JList includeList;
	private JSlider slider;

	private JTextField includeField;
	private JTextField cutfield;
	private JTextField chapterfield;
	private JTextField pointslength;
	private JTextField chp;
	private JTextField firstfile;
	private JTextField scannedPID;

	private JLabel titleLabel;

	private JButton cutdel;
	private JButton cutadd;
	private JButton chapterdel;
	private JButton chapteradd;
	private JButton loadlist;
	private JButton savelist;

	private JComboBox cut_combobox;
	private JComboBox chapter_combobox;
	private JComboBox cutmode_combobox;

	private JTabbedPane tabbedPane;

	private List previewList = new ArrayList();

	private DNDListener2 dnd2 = new DNDListener2();
	private CutListener cutAction = new CutListener();
	private JumpListener jumpAction = new JumpListener();

	private boolean action = false;
	private boolean decode = false;

	private String file = " ";
	private String title = "";
	private String navigation[] = { 
		"bwd_ncp.gif", "bwd_100.gif", "bwd_10.gif", "bwd_gop.gif",
		"fwd_gop.gif", "fwd_10.gif", "fwd_100.gif", "fwd_ncp.gif"
	};

	private int filetype = 0;
	private int active_collection = 0;
	private int loadSizeForward = 2560000;

	private long lastPosition = 0;

	private Preview Preview = new Preview(loadSizeForward);
	private JobCollection collection;

	private CutView cutview;
//

	/**
	 * class to control short OSD fadings
	 */
	private class SlideShow implements Runnable {

		private Thread clockThread = null;

		private long value = 0;
		private long skip = 50000;

		private Object[] cutpoints = null;

		/**
		 *
		 */
		public void start(long _value)
		{
			if (clockThread == null)
			{
				clockThread = new Thread(this, "SlideShow");
				clockThread.setPriority(Thread.MIN_PRIORITY);

				value = _value;
				skip = getLoadSize() / 8;

				getCutPoints();

				clockThread.start();
			}
		}

		/**
		 *
		 */
		private void getCutPoints()
		{
			cutpoints = collection == null ? null : collection.getCutpoints();
		}

		/**
		 *
		 */
		public void run()
		{
			Thread myThread = Thread.currentThread();

			while (clockThread == myThread)
			{
				try {

					for (long val;; )
					{
						val = update(value);
						Thread.sleep(5);

						if (!slideshow || val < value)
							break;

						value = skipArea(val);

						if (value < val)
							break;
					}

					stop();

				} catch (InterruptedException e) {}

			}
		}

		/**
		 *
		 */
		private long update(long val)
		{
			return preview(val);
		}

		/**
		 *
		 */
		private long skipArea(long val)
		{
			// next gop
			if (cutpoints == null || cutpoints.length == 0)
				return (val + skip);

			int index = getCutIndex(cutpoints, String.valueOf(val));

			// area among cutpoints
			if (index < 0)
			{
				// next gop "in" area
				if ((index & 1) == 0)
					return (val + skip);

				// stop
				if (-index > cutpoints.length)
					return (val - skip);

				// jump to next export area (-index - 1)
				return Long.parseLong(cutpoints[index + 1].toString());
			}

			// exact cut point "in" area
			if ((index & 1) == 0)
				return (val + skip);

			// stop
			if (index + 1 >= cutpoints.length)
				return (val - skip);

			// jump to next exported area
			return Long.parseLong(cutpoints[index + 1].toString());
		}

		/**
		 *
		 */
		public void stop()
		{
			clockThread = null;
		}
	}

	private SlideShow cl = new SlideShow();
	private boolean slideshow = false;

//
	ComboBoxIndexListener _ComboBoxIndexListener = new ComboBoxIndexListener();
	ComboBoxItemListener _ComboBoxItemListener = new ComboBoxItemListener();
	CheckBoxListener _CheckBoxListener = new CheckBoxListener();

	class DNDListener2 implements DropTargetListener
	{
		public void drop(DropTargetDropEvent e)
		{
			try {
				int dropaction = e.getDropAction();  // 1=copy, 2=move

				if ( dropaction == 0 || dropaction > 2)
				{ 
					e.rejectDrop(); 

					return; 
				}

				e.acceptDrop(dropaction);

				Transferable tr = e.getTransferable();
				DataFlavor[] df = tr.getTransferDataFlavors();
				List li = (List) tr.getTransferData(df[0]);  // see note about mac os x

				Object[] val = li.toArray();

				if (val.length > 0)
					loadList(val[0].toString());

				e.dropComplete(true);

			} catch (Exception eee) { 

				e.dropComplete(false); 
				Common.setExceptionMessage(eee);
			}
		}

		public void dragEnter(DropTargetDragEvent e)
		{}

		public void dragExit(DropTargetEvent e) 
		{}

		public void dragOver(DropTargetDragEvent e)
		{}

		public void dropActionChanged(DropTargetDragEvent e)
		{}
	}

	class JumpListener implements ActionListener
	{
		public void actionPerformed(ActionEvent e)
		{
			if (collection == null)
				return;

			if (!action) 
				return;

			String actName = e.getActionCommand();
			int val = slider.getValue();

			/**
			 * prev. cut point pos.
			 */
			if (actName.equals(navigation[0]))
			{
				int i = 0;
				int ic = cut_combobox.getItemCount(); 

				if (ic > 0)
				{ 
					i = ic - 1; 

					if (lastPosition > Long.parseLong(cut_combobox.getItemAt(0).toString()))
						while (lastPosition <= Long.parseLong(cut_combobox.getItemAt(i).toString()))
							i--; 

					cut_combobox.setSelectedIndex(i); 
				} 
			}

			else if (actName.equals(navigation[1]))
				slider.setValue(val - 3125000);

			else if (actName.equals(navigation[2]))
				slider.setValue(val - 312500);

			else if (actName.equals(navigation[3]))
				slider.setValue(val - 2);

			else if (actName.equals(navigation[4]))
				slider.setValue(val + 2);

			else if (actName.equals(navigation[5]))
				slider.setValue(val + 312500);

			else if (actName.equals(navigation[6]))
				slider.setValue(val + 3125000);

			/**
			 * next cut point pos.
			 */
			else if (actName.equals(navigation[7]))
			{
				int i = 0;
				int ic = cut_combobox.getItemCount(); 

				if (ic > 0)
				{ 
					if (lastPosition < Long.parseLong(cut_combobox.getItemAt(ic - 1).toString()))
						while (lastPosition >= Long.parseLong(cut_combobox.getItemAt(i).toString())) 
							i++; 

					cut_combobox.setSelectedIndex(i); 
				}                   
			}

			else if (actName.equals("loadlist"))
				loadList();

			else if (actName.equals("savelist"))
				saveList();
		}
	}

	class CutListener implements ActionListener
	{
		public void actionPerformed(ActionEvent e)
		{
			if (collection == null)
				return;

			if (!action)
				return;

			action = false;
			String actName = e.getActionCommand();

			if (actName.equals("delpoint"))
			{
				if (cut_combobox.getItemCount() > 0)
					removeCutpoint(cut_combobox.getSelectedIndex());
			}

			else if (actName.equals("cutnumber") || actName.equals("addpoint"))
			{
				String value = cutfield.getText();

				if (!value.equals("") && addCutpoint(value))
				{
					if (Common.getSettings().getIntProperty(Keys.KEY_CutMode) == CommonParsing.CUTMODE_BYTE)
						collection.setCutImage(value, Common.getMpvDecoderClass().getCutImage());
				}

				cutfield.setText("");
			}

			else if (actName.equals("delchapter"))
			{
				if (chapter_combobox.getItemCount() > 0)
					removeChapterpoint(chapter_combobox.getSelectedIndex());
			}

			else if (actName.equals("addchapter"))
			{
				String value = cutfield.getText();

				if (!value.equals("") && addChapterpoint(value))
				{}
			}

			else if (actName.equals("ID"))
			{
				try {

					String value = includeField.getText();

					if (!value.equals(""))
					{
						if (!value.startsWith("0x")) 
							value = "0x" + Integer.toHexString(0x1FFF & Integer.parseInt(value));

						value = value.toUpperCase().replace('X', 'x');

						collection.addPID(value);
						includeList.setListData(collection.getPIDs());
						includeList.ensureIndexIsVisible(collection.getPIDs().length - 1);
					}

				} catch (NumberFormatException ne) {

					Common.setOSDErrorMessage(Resource.getString("cutlistener.wrongnumber"));
				}

				includeField.setText("");
				action = true;

				return;
			}

			/**
			 * changes will be saved immediately
			 */
			else if (actName.equals("transferPIDs"))
			{
				try {

					Object[] values = includeList.getSelectedValues();

					if (values.length > 0)
					{
						JobCollection new_collection = Common.addCollection(collection.getNewInstance());

						collection.removePID(values);

						new_collection.clearPIDs();
						new_collection.addPID(values);

						new_collection.determinePrimaryFileSegments();

						entry(Common.getActiveCollection());
					}

				} catch (Exception e2) {

					Common.setExceptionMessage(e2);
				}

				action = true;

				return;
			}

			/**
			 * changes will be saved immediately
			 */
			else if (actName.equals("transferCuts"))
			{
				try {

					int NumOfPts = 0;

					if ((NumOfPts = cut_combobox.getItemCount()) > 2) //2cutpoints are not enough
					{
						for (int b = 2; b < NumOfPts; b += 2)
						{
							JobCollection new_collection = Common.addCollection(collection.getNewInstance());

							new_collection.clearCutpoints();

							for (int c = 0; c < 2 && b + c < NumOfPts; c++)
								new_collection.addCutpoint( collection.removeCutpoint(2));

							new_collection.determinePrimaryFileSegments();
						}

						entry(Common.getActiveCollection());

						action = true;
					}

				} catch (Exception e2) {

					Common.setExceptionMessage(e2);
				}

				action = true;

				return;
			}

			/**
			 * stuff completion for add and del cutpoints
			 */
			if (cut_combobox.getItemCount() > 0)
			{
				Object[] obj = collection.getCutpoints();

				int index = cut_combobox.getSelectedIndex();

				Common.getGuiInterface().showCutIcon((index & 1) == 0, obj, previewList);

				if (actName.equals("cutbox") || actName.equals("delpoint"))
				{
					if (Common.getSettings().getIntProperty(Keys.KEY_CutMode) == CommonParsing.CUTMODE_BYTE)
						preview(Long.parseLong(obj[index].toString()));
				}

				if (actName.equals("addpoint"))
					updateCutView(String.valueOf(lastPosition));
			}

			else
			{
				Common.getGuiInterface().showCutIcon(true, null, previewList);
				updateCutView(String.valueOf(lastPosition));
			}

			/**
			 * stuff completion for add and del chapterpoints
			 */
			if (chapter_combobox.getItemCount() > 0)
			{
				Object[] obj = collection.getChapterpoints();

				int index = chapter_combobox.getSelectedIndex();

				chapterfield.setText(String.valueOf(index + 1));

				Common.getGuiInterface().showChapterIcon(obj, previewList);

				if (actName.equals("chapterbox"))
				{
					if (Common.getSettings().getIntProperty(Keys.KEY_CutMode) == CommonParsing.CUTMODE_BYTE)
						preview(Long.parseLong(obj[index].toString()));
				}
			}

			else
			{
				Common.getGuiInterface().showChapterIcon(null, previewList);

				chapterfield.setText("");
			}

			/**
			 * 
			 */
			if (Common.getSettings().getIntProperty(Keys.KEY_CutMode) == CommonParsing.CUTMODE_BYTE)
				slider.requestFocus();

			getExpectedSize();

			action = true;
		}
	}

	/**
	 *
	 */
	public CollectionPanel()
	{
		initialize();
	}


	/**
	 *
	 */
	protected JPanel buildRightPanel()
	{
		JPanel panel = new JPanel();

		tabbedPane = new JTabbedPane();
		tabbedPane.setTabPlacement(SwingConstants.BOTTOM);

		tabbedPane.addTab("Settings", buildPanel_1());
		tabbedPane.addTab("CutViews", buildPanel_2());

		tabbedPane.setSelectedIndex(1);

		panel.add(tabbedPane, BorderLayout.CENTER);

		return panel;
	}

	/**
	 *
	 */
	protected JPanel buildPanel_1()
	{
		JPanel panel = new JPanel();
		panel.setLayout (new ColumnLayout());

		String[][] objects = {
			Keys.KEY_Preview_fastDecode,
			Keys.KEY_Preview_LiveUpdate,
			Keys.KEY_Preview_AllGops,
			Keys.KEY_OptionHorizontalResolution,
			Keys.KEY_OptionDAR
		};

		JCheckBox[] box = new JCheckBox[objects.length];

		for (int i = 0; i < objects.length; i++)
		{
			box[i] = new JCheckBox(Resource.getString(objects[i][0]));
			box[i].setPreferredSize(new Dimension(220, 20));
			box[i].setMaximumSize(new Dimension(220, 20));
			box[i].setToolTipText(Resource.getString(objects[i][0] + Keys.KEY_Tip));
			box[i].setActionCommand(objects[i][0]);
			box[i].setSelected(Common.getSettings().getBooleanProperty(objects[i]));
			box[i].addActionListener(_CheckBoxListener);
		}

		for (int i = 0; i < 3; i++)
			panel.add(box[i]);

		panel.add(Box.createRigidArea(new Dimension(1, 4)));


		panel.add(new JLabel(Resource.getString("CollectionPanel.ExportLimits")));

		/**
		 *
		 */
		JPanel CL2 = new JPanel();
		CL2.setLayout(new BoxLayout(CL2, BoxLayout.X_AXIS));

		box[3].setPreferredSize(new Dimension(110, 20));
		box[3].setMaximumSize(new Dimension(110, 20));
		CL2.add(box[3]);  


		JComboBox combobox_34 = new JComboBox(Keys.ITEMS_ExportHorizontalResolution);
		combobox_34.setMaximumRowCount(7);
		combobox_34.setPreferredSize(new Dimension(90, 20));
		combobox_34.setMaximumSize(new Dimension(90, 20));
		combobox_34.setActionCommand(Keys.KEY_ExportHorizontalResolution[0]);
		combobox_34.setEditable(true);
		combobox_34.setSelectedItem(Common.getSettings().getProperty(Keys.KEY_ExportHorizontalResolution));
		combobox_34.addActionListener(_ComboBoxItemListener);
		CL2.add(combobox_34);

		panel.add(CL2);


		/**
		 *
		 */
		JPanel CL3 = new JPanel();
		CL3.setLayout(new BoxLayout(CL3, BoxLayout.X_AXIS));

		box[4].setPreferredSize(new Dimension(80, 20));
		box[4].setMaximumSize(new Dimension(80, 20));
		CL3.add(box[4]);  

		JComboBox combobox_24 = new JComboBox(Keys.ITEMS_ExportDAR);
		combobox_24.setMaximumRowCount(7);
		combobox_24.setPreferredSize(new Dimension(120, 20));
		combobox_24.setMaximumSize(new Dimension(120, 20));
		combobox_24.setActionCommand(Keys.KEY_ExportDAR[0]);
		combobox_24.setSelectedIndex(Common.getSettings().getIntProperty(Keys.KEY_ExportDAR));
		combobox_24.addActionListener(_ComboBoxIndexListener);
		CL3.add(combobox_24);

		panel.add(CL3);

		panel.add(Box.createRigidArea(new Dimension(1, 4)));


		/**
		 *
		 */
		JPanel CL4 = new JPanel();
		CL4.setLayout(new BoxLayout(CL4, BoxLayout.X_AXIS));
		CL4.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), Resource.getString("CollectionPanel.PidList")));

		includeField = new JTextField("");
		includeField.setPreferredSize(new Dimension(80, 25));
		includeField.setMaximumSize(new Dimension(80, 25));
		includeField.setEditable(true);
		includeField.setActionCommand("ID");
		includeField.setToolTipText(Resource.getString("CollectionPanel.PidList.Tip1"));

		includeList = new JList();
		includeList.setToolTipText(Resource.getString("CollectionPanel.PidList.Tip2"));
		CL4.add(includeField);

		includeField.addActionListener(cutAction);

		CL4.add(new JLabel("=>"));

		JScrollPane scrollList = new JScrollPane();
		scrollList.setPreferredSize(new Dimension(80, 110));
		scrollList.setMaximumSize(new Dimension(80, 110));
		scrollList.setViewportView(includeList);
		CL4.add(scrollList);

		includeList.addMouseListener( new MouseAdapter() {
			public void mouseClicked(MouseEvent e)
			{
				if (collection == null)
					return;

				if (e.getClickCount() >= 2)
				{
					Object[] val = includeList.getSelectedValues();

					collection.removePID(val);
					includeList.setListData(collection.getPIDs());
				}
			}
		});

		panel.add(CL4);

		panel.add(Box.createRigidArea(new Dimension(1, 4)));

		JButton pids = new JButton(Resource.getString("CollectionPanel.transferPids1"));
		pids.setPreferredSize(new Dimension(220, 20));
		pids.setMaximumSize(new Dimension(220, 20));
		pids.setActionCommand("transferPIDs");
		pids.setToolTipText(Resource.getString("CollectionPanel.transferPids1.Tip"));
		pids.addActionListener(cutAction);
		panel.add(pids);

		JButton cpoints = new JButton(Resource.getString("CollectionPanel.transferPids2"));
		cpoints.setPreferredSize(new Dimension(220,20));
		cpoints.setMaximumSize(new Dimension(220,20));
		cpoints.setActionCommand("transferCuts");
		cpoints.setToolTipText(Resource.getString("CollectionPanel.transferPids2.Tip"));
		cpoints.addActionListener(cutAction);
		panel.add(cpoints);

		panel.add(Box.createRigidArea(new Dimension(1, 6)));

		return panel;
	}

	/**
	 *
	 */
	protected JPanel buildPanel_2()
	{
		JPanel panel = new JPanel();
		panel.setLayout (new BorderLayout());

		panel.add(cutview = new CutView());

		return panel;
	}

	/**
	 *
	 */
	protected void setComponentColor(JComponent comp, Color c)
	{
		comp.setBackground(c);
		comp.setForeground(c);
	}

	/**
	 *
	 */
	private void initialize()
	{
		chooser = CommonGui.getMainFileChooser();

		setLayout( new BorderLayout() );

		JPanel grid = new JPanel();
		grid.setLayout(new BorderLayout());

		/**
		 *
		 */
		JPanel previewPanel = new JPanel();
		previewPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLoweredBevelBorder(), Resource.getString("CollectionPanel.CutPanel")));
		previewPanel.setLayout ( new BorderLayout() );
		previewPanel.setToolTipText(Resource.getString("CollectionPanel.CutPanel.Tip1"));
		previewPanel.add(CommonGui.getPicturePanel());


		/**
		 *
		 */
		slider = new JSlider(0, (10240000 / 16), 0);
		slider.setPreferredSize(new Dimension(512, 24));
		slider.setMaximumSize(new Dimension(512, 24));
		slider.setValue(0);

		slider.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent e)
			{
				if (slideshow || previewList.isEmpty() || Common.getSettings().getIntProperty(Keys.KEY_CutMode) != CommonParsing.CUTMODE_BYTE)
				{
					slideshow = false;
					return;
				}

				if (e.getClickCount() < 2)
					return;

				slideshow = true;
				cl.start(lastPosition);
			}
		});


		/**
		 *
		 */
		JPanel grid_3 = new JPanel();
		grid_3.setLayout(new BoxLayout(grid_3, BoxLayout.X_AXIS));
		grid_3.add(buildCutPointPanel());
		grid_3.add(buildChapterPointPanel());

		/**
		 *
		 */
		JPanel grid_1 = new JPanel();
		grid_1.setLayout(new GridLayout(0, 2));
		grid_1.add(buildNavigationPanel());
		grid_1.add(grid_3);

		/**
		 *
		 */
		JPanel grid_2 = new JPanel();
		grid_2.setLayout(new BorderLayout());
		grid_2.setBorder(BorderFactory.createRaisedBevelBorder());
		grid_2.add(slider);
		grid_2.add(grid_1, BorderLayout.SOUTH);

		previewPanel.add(grid_2, BorderLayout.SOUTH);

		slider.addChangeListener(new ChangeListener()
		{
			public void stateChanged(ChangeEvent e)
			{
				if (collection == null)
					return;

				if (!action)
					return;

				if (slider.getValueIsAdjusting() && !Common.getSettings().getBooleanProperty(Keys.KEY_Preview_LiveUpdate))
					return;

				long val = (Common.getSettings().getIntProperty(Keys.KEY_CutMode) == CommonParsing.CUTMODE_BYTE ? 16L : 1L) * slider.getValue();

				if (Common.getSettings().getIntProperty(Keys.KEY_CutMode) == CommonParsing.CUTMODE_BYTE && val != (lastPosition & ~15))
					preview(val);

				else if (Common.getSettings().getIntProperty(Keys.KEY_CutMode) != CommonParsing.CUTMODE_BYTE)
					scannedPID.setText(Resource.getString("CollectionPanel.Preview.offline"));

				getType();
			}
		});

		slider.addKeyListener(new KeyAdapter()
		{ 
			public void keyPressed(KeyEvent e)
			{ 
				if (collection == null)
					return;

				int i = 0; 
				int ic = 0; 
				int offs = 0; 
				int keyval=e.getKeyCode(); 

				switch(e.getKeyChar())
				{ 
				case 'p': 
					ic=cut_combobox.getItemCount(); 
					if (ic > 0) { 
						i = ic - 1; 
						if (lastPosition>Long.parseLong(cut_combobox.getItemAt(0).toString()))
							while (lastPosition <= Long.parseLong(cut_combobox.getItemAt(i).toString()))
								i--; 
						cut_combobox.setSelectedIndex(i); 
					} 
					return; 

				case 'n': 
					ic=cut_combobox.getItemCount(); 
					if (ic > 0) { 
						if (lastPosition<Long.parseLong(cut_combobox.getItemAt(ic - 1).toString()))
							while (lastPosition >= Long.parseLong(cut_combobox.getItemAt(i).toString())) 
								i++; 
						cut_combobox.setSelectedIndex(i); 
					}                   
					return; 

				case 'a': 
					cutadd.doClick(); 
					return; 

				case 'd': 
					cutdel.doClick(); 
					return; 
				} 

				if (e.isShiftDown()) 
					offs = 62500; 

				else if (e.isControlDown()) 
					offs = 312500; 

				else if (e.isAltDown()) 
					offs = 3125000; 

				else 
					return; 

				switch (keyval)
				{ 
				case KeyEvent.VK_RIGHT: 
					slider.setValue(slider.getValue() + offs); 
					break; 

				case KeyEvent.VK_LEFT: 
					slider.setValue(slider.getValue() - offs); 
				}    
			} 
		}); 

		grid.add(previewPanel);


		JPanel cutPanel = new JPanel();
		cutPanel.setBorder(BorderFactory.createTitledBorder( BorderFactory.createEtchedBorder(), Resource.getString("CollectionPanel.Various")));
		cutPanel.setLayout ( new ColumnLayout() );

		titleLabel = new JLabel("");
		titleLabel.setPreferredSize(new Dimension(230, 22));
		titleLabel.setMaximumSize(new Dimension(230, 22));
	//	cutPanel.add(titleLabel);

		cutPanel.add(buildRightPanel());

		cutmode_combobox = new JComboBox(Keys.ITEMS_CutMode);
		cutmode_combobox.setPreferredSize(new Dimension(230, 22));
		cutmode_combobox.setMaximumSize(new Dimension(230, 22));
		cutmode_combobox.setActionCommand(Keys.KEY_CutMode[0]);
		cutmode_combobox.setSelectedIndex(Common.getSettings().getIntProperty(Keys.KEY_CutMode));
		cutmode_combobox.addActionListener(_ComboBoxIndexListener);
		cutPanel.add(cutmode_combobox);


		loadlist = new JButton(Resource.getString("CollectionPanel.loadCutpointList"));
		loadlist.setPreferredSize(new Dimension(230, 22));
		loadlist.setMaximumSize(new Dimension(230, 22));
		loadlist.setToolTipText(Resource.getString("CollectionPanel.loadCutpointList.Tip")); //DM18022004 081.6 int17 new
		loadlist.setActionCommand("loadlist");
		loadlist.addActionListener(jumpAction);

		savelist = new JButton(Resource.getString("CollectionPanel.saveCutpointList"));
		savelist.setPreferredSize(new Dimension(230, 22));
		savelist.setMaximumSize(new Dimension(230, 22));
		savelist.setActionCommand("savelist");
		savelist.addActionListener(jumpAction);

		cutPanel.add(loadlist);
		cutPanel.add(savelist);


		DropTarget dropTarget_3 = new DropTarget(loadlist, dnd2);
		DropTarget dropTarget_4 = new DropTarget(cutfield, dnd2);

		grid.add(cutPanel, BorderLayout.EAST);

		add(grid);

		setTitle(Resource.getString("CollectionPanel.Title2"));
	}

	/**
	 *
	 */
	protected JPanel buildNavigationPanel()
	{
		/**
		 *
		 */
		JPanel jumpPanel = new JPanel();
		jumpPanel.setLayout(new BoxLayout(jumpPanel, BoxLayout.X_AXIS));

		JButton jump[] = new JButton[navigation.length];

		for (int i = 0; i < jump.length; i++)
		{
			jump[i] = new JButton(CommonGui.loadIcon(navigation[i]));
			jump[i].setPreferredSize(new Dimension(31, 22));
			jump[i].setMaximumSize(new Dimension(31, 22));
			jump[i].setActionCommand(navigation[i]);
			jump[i].addActionListener(jumpAction);
			jumpPanel.add(jump[i]);
		}

		/**
		 *
		 */
		firstfile = new JTextField(file);
		firstfile.setToolTipText(Resource.getString("CollectionPanel.CutPanel.Tip2"));
		firstfile.setBackground(new Color(230, 230, 230));
		firstfile.setEditable(false);

		JPanel panel_1 = new JPanel();
		panel_1.setLayout(new GridLayout(1, 1));
		panel_1.setPreferredSize(new Dimension(248, 22));
		panel_1.setMaximumSize(new Dimension(248, 22));
		panel_1.setMinimumSize(new Dimension(248, 22));
		panel_1.add(firstfile);

		/**
		 *
		 */
		scannedPID = new JTextField("");
		scannedPID.setToolTipText(Resource.getString("CollectionPanel.CutPanel.Tip3"));
		scannedPID.setBackground(new java.awt.Color(230, 230, 230));
		scannedPID.setEditable(false);

		JPanel panel_2 = new JPanel();
		panel_2.setLayout(new GridLayout(1, 1));
		panel_2.setPreferredSize(new Dimension(248, 22));
		panel_2.setMaximumSize(new Dimension(248, 22));
		panel_2.setMinimumSize(new Dimension(248, 22));
		panel_2.add(scannedPID);

		/**
		 *
		 */
		JPanel panel = new JPanel();
		panel.setLayout(new ColumnLayout());
		panel.add(jumpPanel);
		panel.add(panel_2);
		panel.add(panel_1);

		return panel;
	}

	/**
	 *
	 */
	protected JPanel buildCutPointPanel()
	{
		/**
		 *
		 */
		cutadd = new JButton(CommonGui.loadIcon("add.gif"));
		cutadd.setActionCommand("addpoint");
		cutadd.setPreferredSize(new Dimension(34, 22));
		cutadd.setMaximumSize(new Dimension(34, 22));

		cutfield = new JTextField("");
		cutfield.setPreferredSize(new Dimension(112, 22));
		cutfield.setMaximumSize(new Dimension(112, 22));
		cutfield.setToolTipText(Resource.getString("CollectionPanel.CutPanel.Tip4"));
		cutfield.setActionCommand("cutnumber");

		/**
		 *
		 */
		JPanel panel_1 = new JPanel();
		panel_1.setLayout(new BoxLayout(panel_1, BoxLayout.X_AXIS));
		panel_1.add(cutadd);
		panel_1.add(cutfield);

		/**
		 *
		 */
		cutdel = new JButton(CommonGui.loadIcon("rem.gif"));
		cutdel.setActionCommand("delpoint");
		cutdel.setPreferredSize(new Dimension(34, 22));
		cutdel.setMaximumSize(new Dimension(34, 22));

		cut_combobox = new JComboBox();
		cut_combobox.setMaximumRowCount(8);
		cut_combobox.setPreferredSize(new Dimension(112, 22));
		cut_combobox.setMaximumSize(new Dimension(112, 22));
		cut_combobox.setActionCommand("cutbox");

		/**
		 *
		 */
		JPanel panel_2 = new JPanel();
		panel_2.setLayout(new BoxLayout(panel_2, BoxLayout.X_AXIS));
		panel_2.add(cutdel);
		panel_2.add(cut_combobox);

		/**
		 *
		 */
		pointslength = new JTextField(Resource.getString("CollectionPanel.NumberOfPoints"));
		pointslength.setToolTipText(Resource.getString("CollectionPanel.NumberOfPoints.Tip"));
		pointslength.setBackground(new java.awt.Color(230, 230, 230));
		pointslength.setEditable(false);

		JPanel panel_3 = new JPanel();
		panel_3.setLayout(new GridLayout(1, 1));
		panel_3.setPreferredSize(new Dimension(146, 22));
		panel_3.setMaximumSize(new Dimension(146, 22));
		panel_3.setMinimumSize(new Dimension(146, 22));
		panel_3.add(pointslength);

		cutdel.addActionListener(cutAction);
		cutadd.addActionListener(cutAction);
		cut_combobox.addActionListener(cutAction);
		cutfield.addActionListener(cutAction);

		/**
		 *
		 */
		JPanel panel = new JPanel();
		panel.setLayout(new ColumnLayout());
		panel.add(panel_1);
		panel.add(panel_2);
		panel.add(panel_3);

		Color c = new Color(200, 150, 200);
		setComponentColor(cutadd, c);
		setComponentColor(cutdel, c);
		setComponentColor(panel, c);

		return panel;
	}

	/**
	 *
	 */
	protected JPanel buildChapterPointPanel()
	{
		/**
		 *
		 */
		chapteradd = new JButton(CommonGui.loadIcon("add.gif"));
		chapteradd.setActionCommand("addchapter");
		chapteradd.setPreferredSize(new Dimension(34, 22));
		chapteradd.setMaximumSize(new Dimension(34, 22));

		chapterdel = new JButton(CommonGui.loadIcon("rem.gif"));
		chapterdel.setActionCommand("delchapter");
		chapterdel.setPreferredSize(new Dimension(34, 22));
		chapterdel.setMaximumSize(new Dimension(34, 22));

		chapterfield = new JTextField("");
		chapterfield.setPreferredSize(new Dimension(34, 22));
		chapterfield.setMaximumSize(new Dimension(34, 22));
		chapterfield.setEditable(false);

		/**
		 *
		 */
		JPanel panel_1 = new JPanel();
		panel_1.setLayout(new BoxLayout(panel_1, BoxLayout.X_AXIS));
		panel_1.add(chapteradd);
		panel_1.add(chapterdel);
		panel_1.add(chapterfield);

		/**
		 *
		 */

		chapter_combobox = new JComboBox();
		chapter_combobox.setMaximumRowCount(8);
		chapter_combobox.setPreferredSize(new Dimension(102, 22));
		chapter_combobox.setMaximumSize(new Dimension(102, 22));
		chapter_combobox.setActionCommand("chapterbox");

		/**
		 *
		 */
		JPanel panel_2 = new JPanel();
		panel_2.setLayout(new BoxLayout(panel_2, BoxLayout.X_AXIS));
		panel_2.add(chapter_combobox);

		/**
		 *
		 */
		chp = new JTextField(Resource.getString("CollectionPanel.NumberOfChapters"));
		chp.setToolTipText(Resource.getString("CollectionPanel.NumberOfChapters.Tip"));
		chp.setBackground(new java.awt.Color(230, 230, 230));
		chp.setEditable(false);

		JPanel panel_3 = new JPanel();
		panel_3.setLayout(new GridLayout(1, 1));
		panel_3.setPreferredSize(new Dimension(102, 22));
		panel_3.setMaximumSize(new Dimension(102, 22));
		panel_3.setMinimumSize(new Dimension(102, 22));
		panel_3.add(chp);


		chapterdel.addActionListener(cutAction);
		chapteradd.addActionListener(cutAction);
		chapter_combobox.addActionListener(cutAction);
		chp.addActionListener(cutAction);

		/**
		 *
		 */
		JPanel panel = new JPanel();
		panel.setLayout(new ColumnLayout());
		panel.add(panel_1);
		panel.add(panel_2);
		panel.add(panel_3);

		Color c = new Color(195, 205, 255);
		setComponentColor(chapteradd, c);
		setComponentColor(chapterdel, c);
		setComponentColor(panel, c);

		return panel;
	}

	/**
	 *
	 */
	private void getType()
	{
		/**
		 * cuts
		 */
		Object[] obj = collection != null ? collection.getCutpoints() : new Object[0];
		int index;

		if (obj.length > 0)
		{
			index = getCutIndex(obj, String.valueOf(lastPosition));

			Common.getGuiInterface().showCutIcon((index & 1) == 0, obj, previewList);
		}

		else
			Common.getGuiInterface().showCutIcon(true, null, previewList);

		/**
		 * chapters
		 */
		obj = collection != null ? collection.getChapterpoints() : new Object[0];

		if (obj.length > 0)
			Common.getGuiInterface().showChapterIcon(obj, previewList);

		else
			Common.getGuiInterface().showChapterIcon(null, previewList);

	}

	/**
	 *
	 */
	private int getLoadSize()
	{
		try {
			int val = Integer.parseInt(Common.getSettings().getProperty(Keys.KEY_PreviewBuffer));

			return val;
		}
		catch (Exception e) {
			Common.setMessage("!> wrong preview_buffer field entry", true);
		}

		return loadSizeForward;
	}

	/**
	 *
	 */
	private long preview(long position)
	{
		boolean direction = false;

		try {

			if (Common.getSettings().getIntProperty(Keys.KEY_CutMode) != CommonParsing.CUTMODE_BYTE || previewList.isEmpty())
			{
				scannedPID.setText(Resource.getString("CollectionPanel.Preview.offline"));

				return lastPosition;
			}

			action = false;

			int loadSize = getLoadSize(); // bytes for searching the next I-frame ;changed for X0.81

			if (position>>>4 >= (long)slider.getMaximum())   // last
			{
				position = position > loadSize ? position - loadSize : 0;
				direction = true;
			}

			else if (position > 0 && position < lastPosition && ((lastPosition>>>4) - (position>>>4)) < 3L )
			{
				position = position > loadSize ? position - loadSize : 0;
				direction = true;
			}

			position = Preview.load(position, ((direction && position == 0) ? (int)lastPosition : loadSize), previewList, direction, Common.getSettings().getBooleanProperty(Keys.KEY_Preview_AllGops), Common.getSettings().getBooleanProperty(Keys.KEY_Preview_fastDecode), collection.getPIDs(), active_collection);

			String str = Preview.getProcessedFile();

			if (str.length() > 32)
			{
				int i = str.indexOf('-');

				String _str = str.substring(0, i + 2);
				str = _str + "..." + str.substring(i + 2 + (str.length() - 34 - i), str.length());
			}

			firstfile.setText(str);

			scannedPID.setText(Resource.getString("CollectionPanel.Preview.processedPid") + Preview.getProcessedPID());

			lastPosition = position;

			slider.setValue((int)(lastPosition / 16));
			cutfield.setText(String.valueOf(lastPosition));
			slider.requestFocus();

		} catch (IOException e6) {

			Common.setExceptionMessage(e6);
		}

		setTitle(title);
		getExpectedSize();

		updateCutView(String.valueOf(lastPosition));

		action = true;

		return lastPosition;
	}

	/**
	 * 
	 */
	private boolean addChapterpoint(String value)
	{
		int index = 0;

		index = getCutIndex(collection.getChapterpoints(), value);

		if (index >= 0)
			return false;

		collection.addChapterpoint(-index - 1, value);

		reloadChapterpoints();

		chapter_combobox.setSelectedItem(value);

		return true;
	}

	/**
	 * 
	 */
	private Object removeChapterpoint(int index)
	{
		Object obj = collection.removeChapterpoint(index);

		reloadChapterpoints();

		Object[] objects = collection.getChapterpoints();

		if (objects.length > 0)
		{
			int _index = -getCutIndex(collection.getChapterpoints(), obj.toString()) - 1;

			if (_index >= objects.length)
				_index = objects.length - 1;
 
			chapter_combobox.setSelectedIndex(_index);
		}

		return obj;
	}

	/**
	 * 
	 */
	private void reloadChapterpoints()
	{
		chapter_combobox.removeAllItems();

		Object[] object = collection != null ? collection.getChapterpoints() : new Object[0];

		for (int i = 0; i < object.length; i++)
			chapter_combobox.addItem(object[i]);
	}

	/**
	 * 
	 */
	private boolean addCutpoint(String value)
	{
		int index = 0;

		index = getCutIndex(collection.getCutpoints(), value);

		if (index >= 0)
			return false;

		collection.addCutpoint(-index - 1, value);

		reloadCutpoints();

		cut_combobox.setSelectedItem(value);

		return true;
	}

	/**
	 * 
	 */
	private Object removeCutpoint(int index)
	{
		Object obj = collection.removeCutpoint(index);

		reloadCutpoints();

		Object[] objects = collection.getCutpoints();

		if (objects.length > 0)
		{
			int _index = -getCutIndex(collection.getCutpoints(), obj.toString()) - 1;

			if (_index >= objects.length)
				_index = objects.length - 1;
 
			cut_combobox.setSelectedIndex(_index);
		}

		return obj;
	}

	/**
	 * 
	 */
	private void reloadCutpoints()
	{
		cut_combobox.removeAllItems();

		Object[] object = collection != null ? collection.getCutpoints() : new Object[0];

		for (int i = 0; i < object.length; i++)
			cut_combobox.addItem(object[i]);
	}

	/**
	 * 
	 */
	private int getCutIndex(Object[] obj, String value)
	{
		class MyComparator implements Comparator
		{
			public int compare(Object o1, Object o2)
			{
				if (Common.getSettings().getIntProperty(Keys.KEY_CutMode) == CommonParsing.CUTMODE_TIME)
					return Long.toString(CommonParsing.parseCutValue(o1.toString(), false)).compareTo(Long.toString(CommonParsing.parseCutValue(o2.toString(), false)));

				else
					return Long.valueOf(o1.toString()).compareTo(Long.valueOf(o2.toString()));
			}
		}

		/**
		 * handle wrong numbers
		 */
		if (Common.getSettings().getIntProperty(Keys.KEY_CutMode) == CommonParsing.CUTMODE_BYTE)
			if (CommonParsing.parseCutValue(value, false) < 0)
				return 0;

		return Arrays.binarySearch(obj, value, new MyComparator()); // is already sorted
	}

	/**
	 * updates last and next cutpoint view
	 */
	private void updateCutView(String value)
	{
		if (Common.getSettings().getIntProperty(Keys.KEY_CutMode) != CommonParsing.CUTMODE_BYTE)
		{
			cutview.clearViews();
			return;
		}

		Object[] listData = collection == null ? null : collection.getCutpoints();

		if (listData == null || listData.length == 0)
		{
			cutview.clearViews();
			return;
		}

		int index = getCutIndex(listData, value);

		if (index < 0)
		{
			int i = -index - 1;

			cutview.setMatchingPoint(false, listData, index);
			cutview.setImage(i < listData.length ? collection.getCutImage(listData[i]) : null, listData, i, cutview.getBottom());
			cutview.setImage(i - 1 >= 0 && i - 1 < listData.length ? collection.getCutImage(listData[i - 1]) : null, listData, i - 1, cutview.getTop());
		}

		else
		{
			if (collection.getCutImage(listData[index]) == null)
				collection.setCutImage(value, Common.getMpvDecoderClass().getCutImage());

			cutview.setMatchingPoint(true, listData, index);
			cutview.setImage(index + 1 < listData.length ? collection.getCutImage(listData[index + 1]) : null, listData, index + 1, cutview.getBottom());
			cutview.setImage(index - 1 >= 0 ? collection.getCutImage(listData[index - 1]) : null, listData, index - 1, cutview.getTop());
		}
	}

	/**
	 *
	 */
	private boolean checkActiveCollection()
	{
		if (active_collection >= 0)
			return true;

		collection = null;

		action = false;

		includeList.setListData(new Object[0]);
		previewList.clear();
		reloadCutpoints();
		reloadChapterpoints();
		Common.getGuiInterface().showCutIcon(true, null, previewList);
		Common.getGuiInterface().showChapterIcon(null, previewList);

		scannedPID.setText(Resource.getString("CollectionPanel.Preview.offline"));
		firstfile.setText("");
		slider.setMaximum(1);
		setTitle(Resource.getString("CollectionPanel.Title2"));

		Common.setOSDMessage(Resource.getString("CollectionPanel.Preview.offline"));

		action = true;

		return false;
	}

	/**
	 *
	 */
	public void entry(int _active_collection)
	{
		if (active_collection != _active_collection)
			cutview.clearViews();

		active_collection = _active_collection;

		Common.getMpvDecoderClass().clearPreviewPixel();

		/**
		 *
		 */
		if (!checkActiveCollection())
			return;

		CommonGui.getPicturePanel().showCollectionNumber(active_collection);

		cutmode_combobox.setSelectedIndex(Common.getSettings().getIntProperty(Keys.KEY_CutMode));

		collection = Common.getCollection(active_collection);

		includeList.setListData(collection.getPIDs());

		List input_files = collection.getInputFilesAsList();
		previewList.clear();

		file = !input_files.isEmpty() ? input_files.get(0).toString() : "";

		/**
		 * determine primary file segments, scan only if changed (date modified) or not scanned
		 */
		long start = 0;
		long end = 0;
		int size = input_files.size();
		int primaryFilesCount = 1;

		filesearch:
		for (int a = 0, b = -1, type = -1; a < size; a++)
		{
			XInputFile xInputFile = ((XInputFile) input_files.get(a)).getNewInstance();

			if (xInputFile.getStreamInfo() == null)
				Common.getScanClass().getStreamInfo(xInputFile); //note: is a new instance

			type = xInputFile.getStreamInfo().getStreamType();

			if (b != -1 && b != type)
			{
				primaryFilesCount = a;
				break filesearch;
			}

			switch (type)
			{
			case CommonParsing.PES_AV_TYPE:
			case CommonParsing.MPEG1PS_TYPE:
			case CommonParsing.MPEG2PS_TYPE:
			case CommonParsing.PVA_TYPE:
			case CommonParsing.TS_TYPE:
			case CommonParsing.ES_MPV_TYPE:
				b = type;

				start = end;
				end += xInputFile.length();
				previewList.add(new PreviewObject(start, end, type, xInputFile));
				break;

			default:
				break filesearch;
			}
		}

		action = false;

		slider.setMaximum(end > 16 ? (int)(end / 16) : 1);

		action = true;

		if (Common.getSettings().getIntProperty(Keys.KEY_CutMode) == CommonParsing.CUTMODE_BYTE && !previewList.isEmpty())
			preview(0);

		else
		{
			scannedPID.setText(Resource.getString("CollectionPanel.Preview.offline"));
			firstfile.setText("");

			Common.getMpvDecoderClass().clearPreviewPixel();
			Common.setOSDMessage(Resource.getString("CollectionPanel.Preview.offline"));
		}

		title = Resource.getString("CollectionPanel.Title2") + " " + active_collection;
		setTitle(title);

		action = false;

		reloadCutpoints();
		reloadChapterpoints();
		cut_combobox.setSelectedIndex(cut_combobox.getItemCount() - 1);

		getExpectedSize();
		getType();

		action = true;
	}

	/**
	 *
	 */
	private void saveList()
	{
		Object[] object = collection.getCutpoints();

		if (object.length == 0)
			return;

		String newfile = file + "[" + active_collection + "].Xcl";

		chooser.setSelectedFile(new File(newfile));
		chooser.rescanCurrentDirectory();

		int retval = chooser.showSaveDialog(this);

		if(retval == JFileChooser.APPROVE_OPTION)
		{
			File theFile = chooser.getSelectedFile();

			if(theFile != null && !theFile.isDirectory())
			{
				newfile = theFile.getAbsolutePath();
			}
		}

		else 
			return;

		try {

			BufferedWriter listwriter = new BufferedWriter(new FileWriter(newfile));

			listwriter.write(Keys.KEY_CutMode[0] + "=" + Common.getSettings().getProperty(Keys.KEY_CutMode));
			listwriter.newLine();
		
			for (int i = 0; i < object.length; i++)
			{
				listwriter.write(object[i].toString());
				listwriter.newLine();
			}

			listwriter.close();
		}

		catch (IOException e) { 

			Common.setMessage(Resource.getString("CollectionPanel.FileAccessError") + " " + file); 
		}
	}

	/**
	 *
	 */
	private void loadList()
	{
		loadList("");
	}

	/**
	 *
	 */
	private void loadList(String newfile)
	{
		List pointlist = new ArrayList();

		String point = "";

		if (!(new File(newfile).exists()))
		{
			chooser.rescanCurrentDirectory();
			int retval = chooser.showOpenDialog(this);

			if(retval == JFileChooser.APPROVE_OPTION)
			{
				File theFile = chooser.getSelectedFile();

				if(theFile != null && !theFile.isDirectory())
					newfile = theFile.getAbsolutePath();
			}
			else 
				return;
		}

		try {

			BufferedReader listreader = new BufferedReader(new FileReader(newfile));

			while (true)
			{
				point = listreader.readLine();

				if (point == null) 
					break;

				if (point.trim().equals("")) 
					continue;

				if (point.startsWith(Keys.KEY_CutMode[0])) 
				{
					cutmode_combobox.setSelectedIndex(Integer.parseInt(point.substring(point.indexOf("=") + 1).trim()));
					continue;
				}

				if (point.startsWith("(")) 
					continue;

				pointlist.add(point);
			}

			listreader.close();

		} catch (IOException e5) { 

			Common.setMessage(Resource.getString("CollectionPanel.loadCutpointList.Error") + " " + file);
		}

		Object[] obj = pointlist.toArray(); 

		if (obj.length > 0)
		{
			long[] cutPoints = new long[obj.length];

			for (int i = 0; i < cutPoints.length; i++)
				cutPoints[i] = CommonParsing.parseCutValue(obj[i].toString(), false);

			Arrays.sort(cutPoints);

			for (int i = 0; i < cutPoints.length; i++)
				collection.addCutpoint(CommonParsing.parseCutValue(cutPoints[i]));

			action = false;

			reloadCutpoints();
			cut_combobox.setSelectedIndex(cut_combobox.getItemCount() - 1);
		}

		getExpectedSize();
		getType();

		action = true;
	}

	/**
	 *
	 */
	private void getExpectedSize()
	{
		if (previewList.isEmpty())
		{
			pointslength.setText(Resource.getString("CollectionPanel.NumberOfPoints") + " " + cut_combobox.getItemCount());
			return;
		}

		Object[] object = collection.getCutpoints();

		long newSize[] = new long[object.length];
		long start = 0;
		long diff = 0;
		long end = 0;

		if (Common.getSettings().getIntProperty(Keys.KEY_CutMode) == CommonParsing.CUTMODE_BYTE)
		{
			for (int i = 0; i < newSize.length; i++)
				newSize[i] = Long.parseLong(object[i].toString());

			if (newSize.length == 0 || (newSize.length & 1) == 1)
				end = ((PreviewObject) previewList.get(previewList.size() - 1)).getEnd();

			else
				end = newSize[newSize.length - 1];

			for (int i = 0; i < newSize.length; i += 2)
			{
				diff += newSize[i] - start;
				start = i + 1 < newSize.length ? newSize[i + 1] : start;
			}
		}

		String length = Common.getSettings().getIntProperty(Keys.KEY_CutMode) == CommonParsing.CUTMODE_BYTE ? (Resource.getString("CollectionPanel.expectedSize") + ((end - diff) / 1048576L) + "MB") : "";

		pointslength.setText(length);
		chp.setText(Resource.getString("CollectionPanel.NumberOfChapters") + " " + chapter_combobox.getItemCount());
	}

	private void setTitle(String str)
	{
		titleLabel.setText(str);
	}
}
