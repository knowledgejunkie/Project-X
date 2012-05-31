/*
 * @(#)CutPanel
 *
 * Copyright (c) 2006-2009 by dvb.matt, All Rights Reserved. 
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

import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.MemoryImageSource;
import java.awt.Font;
import java.awt.GridLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.BorderFactory;

import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

//
import javax.swing.JSlider;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import net.sourceforge.dvb.projectx.common.Keys;
import net.sourceforge.dvb.projectx.common.Common;
import net.sourceforge.dvb.projectx.common.Resource;
import net.sourceforge.dvb.projectx.common.JobCollection;

import net.sourceforge.dvb.projectx.parser.CommonParsing;

import net.sourceforge.dvb.projectx.video.Preview;
import net.sourceforge.dvb.projectx.video.PreviewObject;

import net.sourceforge.dvb.projectx.xinput.XInputFile;
import net.sourceforge.dvb.projectx.gui.ComboBoxIndexListener;

/**
 *
 */
public class CutPanel extends JPanel {

	private int active_collection = 0;
	private int loadSizeForward = 2560000;

	private long lastPosition = 0;
	private long divisor = 16L;

	private boolean matchingPoint = false;

	private boolean cut_top = true;
	private boolean cut_bottom = false;
	private boolean cut_match = false;
	private boolean action = false;

	private String file = " ";

	private String navigation[] = { 
		"leftsteparrow1x.gif", "leftarrow3x.gif", "leftarrow2x.gif",
		"rightarrow2x.gif", "rightarrow3x.gif", "rightsteparrow1x.gif",
		"leftarrowstep1x.gif", "rightarrowstep1x.gif"
	};

	private JComboBox cutIndexList;
	private JComboBox chapterIndexList;
	private JComboBox cutmode_combobox;

	private JTextField includeField;
	private JTextField positionField;
	private JTextField estimatedSizeField;
	private JTextField chapterCountField;
	private JTextField chapterIndexField;
	private JTextField cutCountField;
	private JTextField cutIndexField;

	private JButton cutAdd;
	private JButton cutDelete;
	private JButton chapterAdd;
	private JButton chapterDelete;

	private JButton[] jump;

	private JSlider slider;

	private X_JFileChooser chooser;

	private DNDListener2 dnd2 = new DNDListener2();
	private JumpListener jumpAction = new JumpListener();
	private CutListener cutAction = new CutListener();

	private JobCollection collection;

	private CutView cutview;
	private Preview Preview = new Preview(loadSizeForward);

	private List previewList = new ArrayList();

	private ComboBoxIndexListener _ComboBoxIndexListener = new ComboBoxIndexListener();

	private SlideShow cl = new SlideShow();

	private CutMatrix cm = new CutMatrix();

	private boolean slideshow = false;

	private JPanel sliderPanel;

	/**
	 *
	 */
	private class DNDListener2 implements DropTargetListener
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
					loadCutList(val[0].toString());

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

	/**
	 *
	 */
	private class CutListener implements ActionListener
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
				if (cutIndexList.getItemCount() > 0)
					removeCutpoint(cutIndexList.getSelectedIndex());
			}

			else if (actName.equals("cutnumber") || actName.equals("addpoint"))
			{
				String value = positionField.getText();

				if (!value.equals("") && addCutpoint(value))
				{
					if (Common.getSettings().getIntProperty(Keys.KEY_CutMode) == CommonParsing.CUTMODE_BYTE)
						collection.setCutImage(value, Common.getMpvDecoderClass().getCutImage());
				}
			}

			else if (actName.equals("delchapter"))
			{
				if (chapterIndexList.getItemCount() > 0)
					removeChapterpoint(chapterIndexList.getSelectedIndex());
			}

			else if (actName.equals("addchapter"))
			{
				String value = positionField.getText();

				if (!value.equals("") && addChapterpoint(value))
				{}
			}

			else if (actName.equals("movecutleft"))
			{
				if (cutIndexList.getItemCount() > 0)
				{
					int index = cutIndexList.getSelectedIndex();

					action = true;
					jump[6].doClick();
					action = false;

					removeCutpoint(index);
					addCutpoint(positionField.getText());
				}
			}

			else if (actName.equals("movecutright"))
			{
				if (cutIndexList.getItemCount() > 0)
				{
					int index = cutIndexList.getSelectedIndex();

					action = true;
					jump[7].doClick();
					action = false;

					removeCutpoint(index);
					addCutpoint(positionField.getText());
				}
			}

			else if (actName.equals("movechapterleft"))
			{
				if (chapterIndexList.getItemCount() > 0)
				{
					int index = chapterIndexList.getSelectedIndex();

					action = true;
					jump[6].doClick();
					action = false;

					removeChapterpoint(index);
					addChapterpoint(positionField.getText());
				}
			}

			else if (actName.equals("movechapterright"))
			{
				if (chapterIndexList.getItemCount() > 0)
				{
					int index = chapterIndexList.getSelectedIndex();

					action = true;
					jump[7].doClick();
					action = false;

					removeChapterpoint(index);
					addChapterpoint(positionField.getText());
				}
			}

			/**
			 * stuff completion for add and del cutpoints
			 */
			if (cutIndexList.getItemCount() > 0)
			{
				Object[] obj = collection.getCutpoints();

				int index = cutIndexList.getSelectedIndex();

				setCutIndexField(index + 1);

				showCutInfo((index & 1) == 0, obj, previewList);
				//Common.getGuiInterface().showCutIcon((index & 1) == 0, obj, previewList);

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
				showCutInfo(true, null, previewList);
				//Common.getGuiInterface().showCutIcon(true, null, previewList);

				setCutIndexField(-1);

				updateCutView(String.valueOf(lastPosition));
			}

			/**
			 * stuff completion for add and del chapterpoints
			 */
			if (chapterIndexList.getItemCount() > 0)
			{
				Object[] obj = collection.getChapterpoints();

				int index = chapterIndexList.getSelectedIndex();

				setChapterIndexField(index + 1);

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

				setChapterIndexField(-1);
			}

			/**
			 * 
			 */
			if (Common.getSettings().getIntProperty(Keys.KEY_CutMode) == CommonParsing.CUTMODE_BYTE)
				slider.requestFocus();

			getExpectedSize();
			updatePositionField();

			if (actName.startsWith("move"))
				updateCutView(String.valueOf(lastPosition));


			action = true;
		}
	}

	/**
	 *
	 */
	private class JumpListener implements ActionListener
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
				int ic = cutIndexList.getItemCount(); 

				if (ic > 0)
				{ 
					i = ic - 1; 

					if (lastPosition > Long.parseLong(cutIndexList.getItemAt(0).toString()))
						while (lastPosition <= Long.parseLong(cutIndexList.getItemAt(i).toString()))
							i--; 

					cutIndexList.setSelectedIndex(i); 
				} 
			}

			else if (actName.equals(navigation[1]))
				slider.setValue(val - 3125000);

			else if (actName.equals(navigation[2]))
				slider.setValue(val - 312500);

			else if (actName.equals(navigation[6]))
				slider.setValue(val - 2);

			else if (actName.equals(navigation[7]))
				slider.setValue(val + 2);

			else if (actName.equals(navigation[3]))
				slider.setValue(val + 312500);

			else if (actName.equals(navigation[4]))
				slider.setValue(val + 3125000);

			/**
			 * next cut point pos.
			 */
			else if (actName.equals(navigation[5]))
			{
				int i = 0;
				int ic = cutIndexList.getItemCount(); 

				if (ic > 0)
				{ 
					if (lastPosition < Long.parseLong(cutIndexList.getItemAt(ic - 1).toString()))
						while (lastPosition >= Long.parseLong(cutIndexList.getItemAt(i).toString())) 
							i++; 

					cutIndexList.setSelectedIndex(i); 
				}                   
			}

			else if (actName.equals("load_cutlist"))
				loadCutList();

			else if (actName.equals("save_cutlist"))
				saveCutList();

			else if (actName.equals("load_chapterlist"))
				loadChapterList();

			else if (actName.equals("save_chapterlist"))
				saveChapterList();

			else if (actName.equals("cut_scan"))
				restartMatrix();

//+
			// changes will be saved immediately
			else if (actName.equals("cut_split"))
			{
				try {

					int NumOfPts = 0;

					if ((NumOfPts = cutIndexList.getItemCount()) > 2) //2cutpoints are not enough
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
					}

				} catch (Exception e2) {

					Common.setExceptionMessage(e2);
				}

				action = true;

				return;
			}
//+
		}
	}

	/**
	 * class to control play of  slideshow
	 */
	private class SlideShow implements Runnable {

		private Thread clockThread = null;

		private long value = 0;
		private long skip = 50;

		private Object[] cutpoints = null;

		/**
		 *
		 */
		public void start(long _value)
		{
			if (clockThread == null)
			{
				clockThread = new Thread(this, "SlideShow");
			//	clockThread.setPriority(Thread.MIN_PRIORITY);
				clockThread.setPriority(Thread.NORM_PRIORITY);

				value = _value;
				//skip = getLoadSize()>>>5;
				skip = getLoadSize()>>>6;

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

					CommonGui.getPlayerFrame().repaintPicture(1);

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

	/**
	 * class to control matrix preview
	 */
	private class CutMatrix implements Runnable {

		private Thread clockThread = null;

		private boolean stopit = false;

		private long tmp = 0;
		private long value = 0;
		private long start_value = 0;
		private int scale = 1;

		/**
		 *
		 */
		public void restart(long value_1)
		{
			start(value_1);
			scale = 1;
		}

		/**
		 *
		 */
		public void start(long value_1)
		{
			breakLoop();

			if (clockThread == null)
			{
				clockThread = new Thread(this, "CutMatrix");
				clockThread.setPriority(Thread.MIN_PRIORITY);

				start_value = value_1;
				stopit = false;

				clockThread.start();
			}
		}

		/**
		 *
		 */
		public void breakLoop()
		{
			stopit = true;
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

					if (!previewList.isEmpty())
					{
						int[][] matrix_index = CommonGui.getPicturePanel().getMatrixTable();
						long end = ((PreviewObject) previewList.get(previewList.size() - 1)).getEnd();
						long div = end / matrix_index.length;
						String str;

						div /= scale;
						scale <<= 1;

						CommonGui.getPicturePanel().resetMatrixPositions(end);

						for (int i = 0; !stopit && i < matrix_index.length; i++)
						{
							tmp = start_value + (i * div);

							if (tmp > end)
								break;

							value = update(tmp, i);
							str = Common.getMpvDecoderClass().getInfo_2();
							str = str.substring(0, str.indexOf(","));

							CommonGui.getPicturePanel().setMatrixIndexPosition(i, value, str);

							Thread.sleep(5);
						}
					}

					stop();

				} catch (InterruptedException e) {}

			}
		}

		/**
		 *
		 */
		private long update(long val, int index)
		{
			return previewMatrix(val, index);
		}

		/**
		 *
		 */
		public void stop()
		{
			clockThread = null;
		}
	}


	/**
	 *
	 */
	public CutPanel()
	{ 
		chooser = CommonGui.getMainFileChooser();

	//	setLayout(new BorderLayout());

		sliderPanel = buildSliderPanel();

		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
		panel.add(buildPointPanel());
		panel.add(buildNavigationPanel());
		panel.add(cutview = new CutView());


		//cut-preview pic handling
		cutview.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent e)
			{
				if (e.getClickCount() < 2)
					return;

				try {
					if (e.getX() < cutview.getBottom() - 20 && e.getY() < 104)
					{
						if (cutview.getTopIndex() != null)
							preview(Long.parseLong(cutview.getTopIndex().toString()));
					}

					else if (e.getX() > cutview.getBottom() && e.getY() < 104)
					{
						if (cutview.getBottomIndex() != null)
							preview(Long.parseLong(cutview.getBottomIndex().toString()));
					}

				} catch (Exception exc) {}
			}
		});

		cutview.addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseDragged(MouseEvent e)
			{
				if (e.getY() > 104)
				{
					cutview.setSlider(e.getX());

					if (collection == null)
						return;

					CommonGui.getPicturePanel().setMixedPreviewPixel(collection.getCutImage(cutview.getPreviewPosition()), cutview.getTransparencyValue());
					CommonGui.getPicturePanel().repaint();
				}
			}

			public void mouseMoved(MouseEvent e)
			{
			}
		});


		add(panel);

		setVisible(true);
	}

	/**
	 *
	 */
	protected JPanel buildSliderPanel()
	{
		slider = new JSlider(0, (int)(10240000L / divisor), 0);
		slider.setPreferredSize(new Dimension(860, 30));
		slider.setMaximumSize(new Dimension(860, 30));
		slider.setMaximum(1);
		slider.setMajorTickSpacing(1);
		slider.setMinorTickSpacing(1);
		slider.setPaintTicks(true);
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

				long val = (Common.getSettings().getIntProperty(Keys.KEY_CutMode) == CommonParsing.CUTMODE_BYTE ? divisor : 1L) * slider.getValue();

				if (Common.getSettings().getIntProperty(Keys.KEY_CutMode) == CommonParsing.CUTMODE_BYTE && val != (lastPosition & ~15))
					preview(val);

				else if (Common.getSettings().getIntProperty(Keys.KEY_CutMode) != CommonParsing.CUTMODE_BYTE)
				{
					Common.getMpvDecoderClass().resetProcessedPosition();
					Common.getMpvDecoderClass().setPidAndFileInfo(Resource.getString("CollectionPanel.Preview.offline"));
				}

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
					ic=cutIndexList.getItemCount(); 
					if (ic > 0) { 
						i = ic - 1; 
						if (lastPosition>Long.parseLong(cutIndexList.getItemAt(0).toString()))
							while (lastPosition <= Long.parseLong(cutIndexList.getItemAt(i).toString()))
								i--; 
						cutIndexList.setSelectedIndex(i); 
					} 
					return; 

				case 'n': 
					ic=cutIndexList.getItemCount(); 
					if (ic > 0) { 
						if (lastPosition<Long.parseLong(cutIndexList.getItemAt(ic - 1).toString()))
							while (lastPosition >= Long.parseLong(cutIndexList.getItemAt(i).toString())) 
								i++; 
						cutIndexList.setSelectedIndex(i); 
					}                   
					return; 

				case 'a': 
					cutAdd.doClick(); 
					return; 

				case 'd': 
					cutDelete.doClick(); 
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


		JPanel panel = new JPanel(new BorderLayout());

		if (!Common.getSettings().getBooleanProperty(Keys.KEY_Preview_SliderWidth))
		{
			panel.add(Box.createRigidArea(new Dimension(145, 20)), BorderLayout.WEST);
			panel.add(slider, BorderLayout.CENTER);
			panel.add(Box.createRigidArea(new Dimension(194, 20)), BorderLayout.EAST);
		}
		else
			panel.add(slider, BorderLayout.CENTER);


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
	protected JPanel buildNavigationPanel()
	{
		JPanel panel_1 = new JPanel();
		panel_1.setLayout(new BoxLayout(panel_1, BoxLayout.X_AXIS));

		JPanel panel_2 = new JPanel();
		panel_2.setLayout(new BoxLayout(panel_2, BoxLayout.X_AXIS));

		jump = new JButton[navigation.length];

		for (int i = 0; i < jump.length; i++)
		{
			jump[i] = new JButton(CommonGui.loadIcon(navigation[i]));
			jump[i].setPreferredSize(new Dimension(34, 24));
			jump[i].setMaximumSize(new Dimension(34, 24));
			jump[i].setActionCommand(navigation[i]);
			jump[i].addActionListener(jumpAction);

			if (i < 6)
				panel_1.add(jump[i]);
			else
				panel_2.add(jump[i]);
		}

		positionField = new JTextField("");
		positionField.setPreferredSize(new Dimension(136, 24));
		positionField.setMaximumSize(new Dimension(136, 24));
		positionField.setToolTipText(Resource.getString("CollectionPanel.CutPanel.Tip4"));
		positionField.setActionCommand("cutnumber");
		positionField.addActionListener(cutAction);
		panel_2.add(positionField);


		cutmode_combobox = new JComboBox(Keys.ITEMS_CutMode);
		cutmode_combobox.setPreferredSize(new Dimension(204, 24));
		cutmode_combobox.setMaximumSize(new Dimension(204, 24));
		cutmode_combobox.setActionCommand(Keys.KEY_CutMode[0]);
		cutmode_combobox.setSelectedIndex(Common.getSettings().getIntProperty(Keys.KEY_CutMode));
		cutmode_combobox.addActionListener(_ComboBoxIndexListener);


		JPanel panel_3 = new JPanel();
		panel_3.setLayout(new BoxLayout(panel_3, BoxLayout.X_AXIS));

		estimatedSizeField = new JTextField("");
		//estimatedSizeField.setToolTipText(Resource.getString("CollectionPanel.NumberOfPoints.Tip"));
		estimatedSizeField.setBackground(new java.awt.Color(230, 230, 230));
		estimatedSizeField.setEditable(false);
		estimatedSizeField.setPreferredSize(new Dimension(150, 24));
		estimatedSizeField.setMaximumSize(new Dimension(150, 24));
		estimatedSizeField.setMinimumSize(new Dimension(150, 24));
		panel_3.add(estimatedSizeField);

		JButton play = new JButton(CommonGui.loadIcon("slidestart.gif"));
		play.setToolTipText("start SlideShow");
		play.setPreferredSize(new Dimension(27, 24));
		play.setMaximumSize(new Dimension(27, 24));
		play.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				if (slideshow || previewList.isEmpty() || Common.getSettings().getIntProperty(Keys.KEY_CutMode) != CommonParsing.CUTMODE_BYTE)
				{
					slideshow = false;
					return;
				}

				slideshow = true;
				cl.start(lastPosition);
			}
		});

		panel_3.add(play);

		JButton stop = new JButton(CommonGui.loadIcon("slidestop.gif"));
		stop.setToolTipText("stop SlideShow");
		stop.setPreferredSize(new Dimension(27, 24));
		stop.setMaximumSize(new Dimension(27, 24));
		stop.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				slideshow = false;
			}
		});

		panel_3.add(stop);

		/**
		 *
		 */
		JPanel panel = new JPanel();
		panel.setLayout(new ColumnLayout());
		panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Navigation"));
		panel.add(panel_1);
		panel.add(panel_2);
		panel.add(panel_3);
		panel.add(cutmode_combobox);


		return panel;
	}

	/**
	 *
	 */
	protected JPanel buildPointPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

		panel.add(buildChapterPointPanel());
		panel.add(buildCutPointPanel());

		return panel;
	}

	/**
	 *
	 */
	protected JPanel buildCutPointPanel()
	{
		/**
		 * row 1
		 */
		cutIndexField = new JTextField();
		cutIndexField.setBackground(new Color(230, 230, 230));
		cutIndexField.setPreferredSize(new Dimension(48, 24));
		cutIndexField.setMaximumSize(new Dimension(48, 24));
		cutIndexField.setEditable(false);
		cutIndexField.setHorizontalAlignment(JTextField.CENTER);

		JButton cutMoveLeft = new JButton(CommonGui.loadIcon("leftcut.gif"));
		cutMoveLeft.setActionCommand("movecutleft");
		cutMoveLeft.setPreferredSize(new Dimension(36, 24));
		cutMoveLeft.setMaximumSize(new Dimension(36, 24));
		cutMoveLeft.addActionListener(cutAction);

		JButton cutMoveRight = new JButton(CommonGui.loadIcon("rightcut.gif"));
		cutMoveRight.setActionCommand("movecutright");
		cutMoveRight.setPreferredSize(new Dimension(36, 24));
		cutMoveRight.setMaximumSize(new Dimension(36, 24));
		cutMoveRight.addActionListener(cutAction);

		JPanel panel_1 = new JPanel();
		panel_1.setLayout(new BoxLayout(panel_1, BoxLayout.X_AXIS));
		panel_1.add(cutMoveLeft);
		panel_1.add(cutMoveRight);
		panel_1.add(cutIndexField);


		/**
		 * row 2
		 */
		cutAdd = new JButton(CommonGui.loadIcon("addcut.gif"));
		cutAdd.setActionCommand("addpoint");
		cutAdd.setPreferredSize(new Dimension(36, 24));
		cutAdd.setMaximumSize(new Dimension(36, 24));
		cutAdd.addActionListener(cutAction);

		DropTarget dropTarget_4 = new DropTarget(cutAdd, dnd2);

		cutDelete = new JButton(CommonGui.loadIcon("remcut.gif"));
		cutDelete.setActionCommand("delpoint");
		cutDelete.setPreferredSize(new Dimension(36, 24));
		cutDelete.setMaximumSize(new Dimension(36, 24));
		cutDelete.addActionListener(cutAction);

		cutCountField = new JTextField();
		cutCountField.setToolTipText(Resource.getString("CollectionPanel.NumberOfPoints.Tip"));
		cutCountField.setBackground(new Color(230, 230, 230));
		cutCountField.setPreferredSize(new Dimension(48, 24));
		cutCountField.setMaximumSize(new Dimension(48, 24));
		cutCountField.setHorizontalAlignment(JTextField.CENTER);
		cutCountField.setEditable(false);

		JPanel panel_2 = new JPanel();
		panel_2.setLayout(new BoxLayout(panel_2, BoxLayout.X_AXIS));
		panel_2.add(cutAdd);
		panel_2.add(cutDelete);
		panel_2.add(cutCountField);


		/**
		 * row 3
		 */
		cutIndexList = new JComboBox();
		cutIndexList.setMaximumRowCount(5);
		cutIndexList.setPreferredSize(new Dimension(120, 24));
		cutIndexList.setMaximumSize(new Dimension(120, 24));
		cutIndexList.setActionCommand("cutbox");
		cutIndexList.addActionListener(cutAction);

		JPanel panel_3 = new JPanel();
		panel_3.setLayout(new BoxLayout(panel_3, BoxLayout.X_AXIS));
		panel_3.add(cutIndexList);


		/**
		 * row 4
		 */
		JButton cut_loadlist = new JButton(CommonGui.loadIcon("open.gif"));
		cut_loadlist.setPreferredSize(new Dimension(30, 24));
		cut_loadlist.setMaximumSize(new Dimension(30, 24));
		cut_loadlist.setToolTipText(Resource.getString("CollectionPanel.loadCutpointList.Tip")); //DM18022004 081.6 int17 new
		cut_loadlist.setActionCommand("load_cutlist");
		cut_loadlist.addActionListener(jumpAction);

		JButton cut_savelist = new JButton(CommonGui.loadIcon("save.gif"));
		cut_savelist.setPreferredSize(new Dimension(30, 24));
		cut_savelist.setMaximumSize(new Dimension(30, 24));
		cut_savelist.setActionCommand("save_cutlist");
		cut_savelist.addActionListener(jumpAction);

		JButton cut_split = new JButton(CommonGui.loadIcon("split.gif"));
		cut_split.setPreferredSize(new Dimension(30, 24));
		cut_split.setMaximumSize(new Dimension(30, 24));
		cut_split.setActionCommand("cut_split");
		cut_split.addActionListener(jumpAction);
		cut_split.setEnabled(true);

		JButton cut_scan = new JButton(CommonGui.loadIcon("matrix.gif"));
		cut_scan.setPreferredSize(new Dimension(30, 24));
		cut_scan.setMaximumSize(new Dimension(30, 24));
		cut_scan.setActionCommand("cut_scan");
		cut_scan.addActionListener(jumpAction);
		cut_scan.setEnabled(true);

		JPanel panel_4 = new JPanel();
		panel_4.setLayout(new BoxLayout(panel_4, BoxLayout.X_AXIS));
		panel_4.add(cut_loadlist);
		panel_4.add(cut_savelist);
		panel_4.add(cut_split);
		panel_4.add(cut_scan);


		/**
		 *
		 */
		JPanel panel = new JPanel();
		panel.setLayout(new ColumnLayout());
		panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), Resource.getString("CollectionPanel.NumberOfPoints")));
		panel.add(panel_2);
		panel.add(panel_1);
		panel.add(panel_3);
		panel.add(panel_4);

		Color c = new Color(212, 175, 212); //new Color(200, 150, 200);
	//	setComponentColor(cutAdd, c);
	//	setComponentColor(cutDelete, c);
		setComponentColor(panel, c);
		setComponentColor(panel_4, c);

		return panel;
	}

	/**
	 *
	 */
	protected JPanel buildChapterPointPanel()
	{
		/**
		 * row 1
		 */
		chapterIndexField = new JTextField();
		chapterIndexField.setBackground(new Color(230, 230, 230));
		chapterIndexField.setPreferredSize(new Dimension(48, 24));
		chapterIndexField.setMaximumSize(new Dimension(48, 24));
		chapterIndexField.setEditable(false);
		chapterIndexField.setHorizontalAlignment(JTextField.CENTER);

		JButton chapterMoveLeft = new JButton(CommonGui.loadIcon("leftchap.gif"));
		chapterMoveLeft.setActionCommand("movechapterleft");
		chapterMoveLeft.setPreferredSize(new Dimension(36, 24));
		chapterMoveLeft.setMaximumSize(new Dimension(36, 24));
		chapterMoveLeft.addActionListener(cutAction);

		JButton chapterMoveRight = new JButton(CommonGui.loadIcon("rightchap.gif"));
		chapterMoveRight.setActionCommand("movechapterright");
		chapterMoveRight.setPreferredSize(new Dimension(36, 24));
		chapterMoveRight.setMaximumSize(new Dimension(36, 24));
		chapterMoveRight.addActionListener(cutAction);

		JPanel panel_1 = new JPanel();
		panel_1.setLayout(new BoxLayout(panel_1, BoxLayout.X_AXIS));
		panel_1.add(chapterMoveLeft);
		panel_1.add(chapterMoveRight);
		panel_1.add(chapterIndexField);


		/**
		 * row 2
		 */
		chapterAdd = new JButton(CommonGui.loadIcon("addchap.gif"));
		chapterAdd.setActionCommand("addchapter");
		chapterAdd.setPreferredSize(new Dimension(36, 24));
		chapterAdd.setMaximumSize(new Dimension(36, 24));
		chapterAdd.addActionListener(cutAction);

		chapterDelete = new JButton(CommonGui.loadIcon("remchap.gif"));
		chapterDelete.setActionCommand("delchapter");
		chapterDelete.setPreferredSize(new Dimension(36, 24));
		chapterDelete.setMaximumSize(new Dimension(36, 24));
		chapterDelete.addActionListener(cutAction);

		chapterCountField = new JTextField();
		chapterCountField.setToolTipText(Resource.getString("CollectionPanel.NumberOfChapters.Tip"));
		chapterCountField.setBackground(new Color(230, 230, 230));
		chapterCountField.setPreferredSize(new Dimension(48, 24));
		chapterCountField.setMaximumSize(new Dimension(48, 24));
		chapterCountField.setEditable(false);
		chapterCountField.setHorizontalAlignment(JTextField.CENTER);
		chapterCountField.addActionListener(cutAction);

		JPanel panel_2 = new JPanel();
		panel_2.setLayout(new BoxLayout(panel_2, BoxLayout.X_AXIS));
		panel_2.add(chapterAdd);
		panel_2.add(chapterDelete);
		panel_2.add(chapterCountField);


		/**
		 * row 3
		 */
		chapterIndexList = new JComboBox();
		chapterIndexList.setMaximumRowCount(5);
		chapterIndexList.setPreferredSize(new Dimension(120, 24));
		chapterIndexList.setMaximumSize(new Dimension(120, 24));
		chapterIndexList.setActionCommand("chapterbox");
		chapterIndexList.addActionListener(cutAction);

		JPanel panel_3 = new JPanel();
		panel_3.setLayout(new BoxLayout(panel_3, BoxLayout.X_AXIS));
		panel_3.add(chapterIndexList);

		/**
		 * row 4
		 */
		JButton chapter_loadlist = new JButton(CommonGui.loadIcon("open.gif"));
		chapter_loadlist.setPreferredSize(new Dimension(32, 24));
		chapter_loadlist.setMaximumSize(new Dimension(32, 24));
		chapter_loadlist.setToolTipText(Resource.getString("CollectionPanel.loadCutpointList.Tip")); //DM18022004 081.6 int17 new
		chapter_loadlist.setActionCommand("load_chapterlist");
		chapter_loadlist.addActionListener(jumpAction);

	//	JButton chapter_savelist = new JButton("save");
		JButton chapter_savelist = new JButton(CommonGui.loadIcon("save.gif"));
		chapter_savelist.setPreferredSize(new Dimension(32, 24));
		chapter_savelist.setMaximumSize(new Dimension(32, 24));
		chapter_savelist.setActionCommand("save_chapterlist");
		chapter_savelist.addActionListener(jumpAction);

	//	JButton chapter_scan = new JButton("scan");
		JButton chapter_scan = new JButton(CommonGui.loadIcon("scan2.gif"));
		chapter_scan.setPreferredSize(new Dimension(50, 24));
		chapter_scan.setMaximumSize(new Dimension(50, 24));
		chapter_scan.setActionCommand("chapter_scan");
		chapter_scan.addActionListener(jumpAction);
		chapter_scan.setEnabled(false);

		JPanel panel_4 = new JPanel();
		panel_4.setLayout(new BoxLayout(panel_4, BoxLayout.X_AXIS));
		panel_4.add(chapter_loadlist);
		panel_4.add(chapter_savelist);
		panel_4.add(Box.createRigidArea(new Dimension(6, 1)));
		panel_4.add(chapter_scan);

		/**
		 *
		 */
		JPanel panel = new JPanel();
		panel.setLayout(new ColumnLayout());
		panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), Resource.getString("CollectionPanel.NumberOfChapters")));
		panel.add(panel_2);
		panel.add(panel_1);
		panel.add(panel_3);
		panel.add(panel_4);

		Color c = new Color(210, 217, 255); //new Color(195, 205, 255);
	//	setComponentColor(chapterAdd, c);
	//	setComponentColor(chapterDelete, c);
		setComponentColor(panel, c);
		setComponentColor(panel_4, c);

		return panel;
	}


	/**
	 *
	 */
	public JPanel getSliderPanel()
	{
		return sliderPanel;
	}

	/**
	 *
	 */
	private void saveCutList()
	{
		Object[] object = collection.getCutpoints();

		if (object.length == 0)
			return;

		String newfile = file + "(" + active_collection + ").Xcl";

		chooser.setSelectedFile(new File(newfile));
		chooser.rescanCurrentDirectory();

		int retval = chooser.showSaveDialog(this);

		if (retval == JFileChooser.APPROVE_OPTION)
		{
			File theFile = chooser.getSelectedFile();

			if (theFile != null && !theFile.isDirectory())
			{
				newfile = theFile.getAbsolutePath();

				if (theFile.exists() && !CommonGui.getUserConfirmation(Resource.getString("msg.overwrite", newfile)))
					return;
			}
			else
				return;
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
	private void loadCutList()
	{
		loadCutList("");
	}

	/**
	 *
	 */
	private void loadCutList(String newfile)
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
			cutIndexList.setSelectedIndex(cutIndexList.getItemCount() - 1);
		}

		getExpectedSize();
		getType();

		action = true;
	}

	/**
	 *
	 */
	private void saveChapterList()
	{
		Object[] object = collection.getChapterpoints();

		if (object.length == 0)
			return;

		String newfile = file + "[" + active_collection + "].Xcp";

		chooser.setSelectedFile(new File(newfile));
		chooser.rescanCurrentDirectory();

		int retval = chooser.showSaveDialog(this);

		if(retval == JFileChooser.APPROVE_OPTION)
		{
			File theFile = chooser.getSelectedFile();

			if (theFile != null && !theFile.isDirectory())
			{
				newfile = theFile.getAbsolutePath();

				if (theFile.exists() && !CommonGui.getUserConfirmation(Resource.getString("msg.overwrite", newfile)))
					return;
			}
			else
				return;
		}
		else 
			return;

		if (!CommonGui.getUserConfirmation(Resource.getString("msg.overwrite", newfile)))
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
	private void loadChapterList()
	{
		loadChapterList("");
	}

	/**
	 *
	 */
	private void loadChapterList(String newfile)
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
			long[] chpPoints = new long[obj.length];

			for (int i = 0; i < chpPoints.length; i++)
				chpPoints[i] = CommonParsing.parseCutValue(obj[i].toString(), false);

			Arrays.sort(chpPoints);

			for (int i = 0; i < chpPoints.length; i++)
				collection.addChapterpoint(CommonParsing.parseCutValue(chpPoints[i]));

			action = false;

			reloadChapterpoints();
			chapterIndexList.setSelectedIndex(chapterIndexList.getItemCount() - 1);
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
			estimatedSizeField.setText(" ");
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

		String length = Common.getSettings().getIntProperty(Keys.KEY_CutMode) == CommonParsing.CUTMODE_BYTE ? (Resource.getString("CollectionPanel.expectedSize") + " " + ((end - diff) / 1048576L) + "MB") : "";

		estimatedSizeField.setText(length);
		chapterCountField.setText(String.valueOf(chapterIndexList.getItemCount()));
		cutCountField.setText(String.valueOf(cutIndexList.getItemCount()));
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

			showCutInfo((index & 1) == 0, obj, previewList);
			//Common.getGuiInterface().showCutIcon((index & 1) == 0, obj, previewList);
		}

		else
			showCutInfo(true, null, previewList);
			//Common.getGuiInterface().showCutIcon(true, null, previewList);

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
	 *
	 */
	public long preview(long position)
	{
		boolean backward = false;

		try {

			CommonGui.getPicturePanel().disableMatrix();

			if (Common.getSettings().getIntProperty(Keys.KEY_CutMode) != CommonParsing.CUTMODE_BYTE || previewList.isEmpty())
			{
				Common.getMpvDecoderClass().resetProcessedPosition();
				Common.getMpvDecoderClass().setPidAndFileInfo(Resource.getString("CollectionPanel.Preview.offline"));

				return lastPosition;
			}

			action = false;

			int loadSize = getLoadSize(); // bytes for searching the next I-frame ;changed for X0.81

			if (position / divisor >= (long)slider.getMaximum())   // last
			{
				position = position > loadSize ? position - loadSize : 0;
				backward = true;
			}

			else if (position > 0 && position < lastPosition && ((lastPosition / divisor) - (position / divisor)) < 3L )
			{
				if (lastPosition - position < 50)
					position = lastPosition;

				position = position > loadSize ? position - loadSize : 0;
				backward = true;
			}

			position = Preview.load(position, ((backward && position == 0) ? (int)lastPosition : loadSize), previewList, backward, Common.getSettings().getBooleanProperty(Keys.KEY_Preview_AllGops), Common.getSettings().getBooleanProperty(Keys.KEY_Preview_fastDecode), Common.getSettings().getIntProperty(Keys.KEY_Preview_YGain), collection.getPIDs(), active_collection);

			String str = Preview.getProcessedFile();

			if (str.length() > 32)
			{
				int i = str.indexOf('-');

				String _str = str.substring(0, i + 2);
				str = _str + "..." + str.substring(i + 2 + (str.length() - 34 - i), str.length());
			}

			lastPosition = position;

			slider.setValue((int)(lastPosition / divisor));
			setPositionField(lastPosition);
			slider.requestFocus();

		} catch (Exception e6) {

			Common.setExceptionMessage(e6);
		}

		getExpectedSize();

		updateCutView(String.valueOf(lastPosition));

		action = true;

		return lastPosition;
	}

	/**
	 *
	 */
	public void startMatrix(long from_position)
	{
		cm.start(from_position);
	}

	/**
	 *
	 */
	public void restartMatrix()
	{
		cm.restart(0);
	}

	/**
	 *
	 */
	public void stopMatrix()
	{
		cm.breakLoop();
	}

	/**
	 *
	 */
	private long previewMatrix(long position, int matrix_index)
	{
		if (Common.getSettings().getIntProperty(Keys.KEY_CutMode) != CommonParsing.CUTMODE_BYTE || previewList.isEmpty())
			return position;

		action = false;

		position = Preview.silentload(position, getLoadSize(), previewList, false, Common.getSettings().getBooleanProperty(Keys.KEY_Preview_AllGops), Common.getSettings().getBooleanProperty(Keys.KEY_Preview_fastDecode), Common.getSettings().getIntProperty(Keys.KEY_Preview_YGain), collection.getPIDs(), active_collection);

		CommonGui.getPicturePanel().setMatrixPreviewPixel(matrix_index);
		CommonGui.getPicturePanel().repaint();

		action = true;

		return position;
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
	private int getLoadSize()
	{
		try {
			int val = Common.getPreviewBufferValue();

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
	private boolean checkActiveCollection()
	{
		if (active_collection >= 0)
			return true;

		collection = null;

		action = false;

		previewList.clear();
		reloadCutpoints();
		reloadChapterpoints();
		showCutInfo(true, null, previewList);
		//Common.getGuiInterface().showCutIcon(true, null, previewList);
		Common.getGuiInterface().showChapterIcon(null, previewList);

		slider.setMaximum(1);

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
		Common.getMpvDecoderClass().resetProcessedPosition();
		Common.getMpvDecoderClass().setPidAndFileInfo(Resource.getString("CollectionPanel.Preview.offline"));

		/**
		 *
		 */
		if (!checkActiveCollection())
			return;

		CommonGui.getPicturePanel().showCollectionNumber(active_collection);

		cutmode_combobox.setSelectedIndex(Common.getSettings().getIntProperty(Keys.KEY_CutMode));

		collection = Common.getCollection(active_collection);

		List input_files = collection.getInputFilesAsList();
		previewList.clear();

		file = !input_files.isEmpty() ? input_files.get(0).toString() : "";

		/**
		 * determine primary file segments, scan only if changed (date modified) or not scanned
		 */
		long start = 0;
		long end = 0;
		long maximum;
		int size = input_files.size();
		int primaryFilesCount = 1;

		if (Common.getSettings().getBooleanProperty(Keys.KEY_Preview_disable))
			size = 0;

		filesearch:
		for (int a = 0, b = -1, type = -1; a < size; a++)
		{
			XInputFile xInputFile = ((XInputFile) input_files.get(a)).getNewInstance();

			if (xInputFile == null)
				continue;

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

		// reduce slider maximum, fit int requirements
		for (divisor = 16L; ; divisor += 16L)
		{
			maximum = end / divisor;

			if (maximum < 0x7FFFFFFFL)
				break;
		}

		if (maximum > divisor)
		{
			slider.setMaximum((int) maximum);
			slider.setMajorTickSpacing((int) (maximum / 10));
			slider.setMinorTickSpacing((int) (maximum / 100));
			slider.setPaintTicks(true);
		}

		else
		{
			slider.setMaximum(1);
			slider.setMajorTickSpacing(1);
			slider.setMinorTickSpacing(1);
			slider.setPaintTicks(true);
		}

		action = true;

		Common.setLastPreviewBitrate(CommonParsing.MAX_SD_BITRATE_VALUE);

		if (Common.getSettings().getIntProperty(Keys.KEY_CutMode) == CommonParsing.CUTMODE_BYTE && !previewList.isEmpty())
			preview(0);

		else
		{
			Common.getMpvDecoderClass().clearPreviewPixel();
			Common.setOSDMessage(Resource.getString("CollectionPanel.Preview.offline"));
		}

		action = false;

		reloadCutpoints();
		reloadChapterpoints();
		cutIndexList.setSelectedIndex(cutIndexList.getItemCount() - 1);

		getExpectedSize();
		getType();

		action = true;
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

		chapterIndexList.setSelectedItem(value);

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
 
			chapterIndexList.setSelectedIndex(_index);
		}

		return obj;
	}

	/**
	 * 
	 */
	private void reloadChapterpoints()
	{
		chapterIndexList.removeAllItems();

		Object[] object = collection != null ? collection.getChapterpoints() : new Object[0];

		for (int i = 0; i < object.length; i++)
			chapterIndexList.addItem(object[i]);
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

		cutIndexList.setSelectedItem(value);

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
 
			cutIndexList.setSelectedIndex(_index);
		}

		return obj;
	}

	/**
	 * 
	 */
	private void reloadCutpoints()
	{
		cutIndexList.removeAllItems();

		Object[] object = collection != null ? collection.getCutpoints() : new Object[0];

		for (int i = 0; i < object.length; i++)
			cutIndexList.addItem(object[i]);
	}

	/**
	 * 
	 */
	private void setCutIndexField(int value)
	{
		Object[] obj = collection != null ? collection.getCutpoints() : null;

		cutIndexField.setText(obj == null || value < 0 ? "" : String.valueOf(value) + "/" + String.valueOf(obj.length));
	}

	/**
	 * 
	 */
	private void setChapterIndexField(int value)
	{
		Object[] obj = collection != null ? collection.getChapterpoints() : null;

		chapterIndexField.setText(obj == null || value < 0 ? "" : String.valueOf(value) + "/" + String.valueOf(obj.length));
	}

	/**
	 * 
	 */
	private void setPositionField(long value)
	{
		updatePositionField(String.valueOf(value));
	}

	/**
	 * 
	 */
	private void updatePositionField()
	{
		updatePositionField(positionField.getText());
	}

	/**
	 * 
	 */
	private void updatePositionField(String str)
	{
		boolean b = action;

		List list_1 = collection.getCutpointList();
		List list_2 = collection.getChapterpointList();

		action = false;

		if (list_1.contains(str))
			positionField.setBackground(new Color(225, 200, 225));

		else if (list_2.contains(str))
			positionField.setBackground(new Color(225, 230, 255));

		else
			positionField.setBackground(Color.white);

		positionField.setText(str);

		action = b;
	}

	/**
	 * 
	 */
	private void showCutInfo(boolean b, Object[] obj, Object list)
	{
		boolean b1 = false;

		if (!((List) list).isEmpty())
		{
			if (collection.getSettings().getBooleanProperty(Keys.KEY_OptionDAR) &&
				collection.getSettings().getIntProperty(Keys.KEY_ExportDAR) + 1 != Common.getMpvDecoderClass().getAspectRatio())
				b1 = true;

			else if (collection.getSettings().getBooleanProperty(Keys.KEY_OptionHorizontalResolution) &&
				collection.getSettings().getIntProperty(Keys.KEY_ExportHorizontalResolution) != Common.getMpvDecoderClass().getWidth())
				b1 = true;
		}

		CommonGui.getPicturePanel().setFilterStatus(b1);
		Common.getGuiInterface().showCutIcon(b, obj, list);
	}
}
