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

	private boolean ToggleControls;

	ComboBoxIndexListener _ComboBoxIndexListener = new ComboBoxIndexListener();
	ComboBoxItemListener _ComboBoxItemListener = new ComboBoxItemListener();
	CheckBoxListener _CheckBoxListener = new CheckBoxListener();

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
	protected JPanel buildPreviewControlPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout (new ColumnLayout());

		String[][] objects = {
			Keys.KEY_holdStreamInfoOnOSD,
			Keys.KEY_Preview_disable,
			Keys.KEY_Preview_fastDecode,
			Keys.KEY_Preview_LiveUpdate,
			Keys.KEY_Preview_AllGops
		};

		for (int i = 0; i < objects.length; i++)
		{
			JCheckBox box = new JCheckBox(Resource.getString(objects[i][0]));
			box.setPreferredSize(new Dimension(250, 20));
			box.setMaximumSize(new Dimension(250, 20));
			box.setToolTipText(Resource.getString(objects[i][0] + Keys.KEY_Tip));
			box.setActionCommand(objects[i][0]);
			box.setSelected(Common.getSettings().getBooleanProperty(objects[i]));
			box.addActionListener(_CheckBoxListener);

			if (i == 1)
				panel.add(Box.createRigidArea(new Dimension(1, 10)));

			panel.add(box);
		}

		String[][] keys = {
			Keys.KEY_Preview_YGain,
			Keys.KEY_PreviewBuffer
		};

		Object[][] object_items = {
			{ "1", "-128", "-112", "-96", "-80", "-64", "-48", "-32", "-16", "0", "16", "32", "48", "64", "80", "96", "112", "128" },
			{ "auto", "256000", "384000", "512000", "768000", "1024000", "1536000", "2048000", "2560000", "3072000" }
		};

		for (int i = 0; i < keys.length; i++)
		{
			JComboBox combobox = new JComboBox(object_items[i]);
			combobox.setPreferredSize(new Dimension(100, 24));
			combobox.setMaximumSize(new Dimension(100, 24));
			combobox.setEditable(true);
			combobox.setMaximumRowCount(6);
			combobox.setActionCommand(keys[i][0]);
			combobox.setSelectedItem(Common.getSettings().getProperty(keys[i]));
			combobox.addActionListener(_ComboBoxItemListener);

			JLabel label = new JLabel(Resource.getString(keys[i][0]));
			label.setToolTipText(Resource.getString(keys[i][0] + Keys.KEY_Tip));

			JPanel panel_1 = new JPanel();
			panel_1.add(label);
			panel_1.add(combobox);

			panel.add(panel_1);
		}

	//	panel.add(Box.createRigidArea(new Dimension(1, 4)));

		return panel;
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
		final JPanel previewPanel = new JPanel();
		previewPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLoweredBevelBorder(), Resource.getString("CollectionPanel.CutPanel")));
		previewPanel.setLayout ( new BorderLayout() );
		previewPanel.setToolTipText(Resource.getString("CollectionPanel.CutPanel.Tip1"));
		previewPanel.add(CommonGui.getPicturePanel());

		/**
		 *
		 */
		final JPanel previewControlPanel = buildPreviewControlPanel();

        previewPanel.addMouseListener(new MouseAdapter()
		{
            public void mouseClicked(MouseEvent e)
			{
				previewPanel.removeAll();

                if ((ToggleControls = !ToggleControls))
                   previewPanel.add(previewControlPanel);

				else
				{
					previewPanel.add(CommonGui.getPicturePanel());
				}

				previewPanel.validate();
				previewPanel.repaint();
            }
        });


		grid.add(previewPanel);
		grid.add(buildScanViewPanel(), BorderLayout.EAST);

		add(grid);
	}

	/**
	 *
	 */
	protected JPanel buildScanViewPanel()
	{
		JPanel panel = new JPanel(new BorderLayout());

		JSlider scanSlider = new JSlider();
		scanSlider.setOrientation(JSlider.VERTICAL);
		scanSlider.setInverted(true);
		scanSlider.setMaximum(100);
		scanSlider.setMajorTickSpacing(10);
		scanSlider.setMinorTickSpacing(1);
		scanSlider.setPaintTicks(true);
		scanSlider.setValue(0);
		scanSlider.setPreferredSize(new Dimension(30, 400));
		scanSlider.setMaximumSize(new Dimension(30, 400));
		scanSlider.setMinimumSize(new Dimension(30, 400));

		panel.add(scanSlider, BorderLayout.SOUTH);

		return panel;
	}
}
