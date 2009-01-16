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

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JComboBox;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import net.sourceforge.dvb.projectx.common.Keys;
import net.sourceforge.dvb.projectx.common.Common;
import net.sourceforge.dvb.projectx.common.Resource;

import net.sourceforge.dvb.projectx.gui.ComboBoxItemListener;
import net.sourceforge.dvb.projectx.gui.CheckBoxListener;
import net.sourceforge.dvb.projectx.gui.CommonGui;

/**
 * collection panel
 */
public class CollectionPanel extends JPanel {

	private boolean ToggleControls;

	private ComboBoxItemListener _ComboBoxItemListener;
	private CheckBoxListener _CheckBoxListener;

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
	private void initialize()
	{
		_ComboBoxItemListener = new ComboBoxItemListener();
		_CheckBoxListener = new CheckBoxListener();

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
	//	grid.add(CommonGui.getPicturePanel().getSliderPanel(), BorderLayout.EAST);

		add(grid);
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
			Keys.KEY_Preview_AllGops,
			Keys.KEY_Preview_SliderWidth,
			Keys.KEY_Preview_fullScaled
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

			if (i == 2 && Common.getMpvDecoderClass().isAccelerated())
			{
				box.setSelected(true);
				Common.getSettings().setBooleanProperty(objects[i][0], box.isSelected());
			}

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
}
