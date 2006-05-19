/*
 * @(#)FileProperties.java
 *
 * Copyright (c) 2006 by dvb.matt, All Rights Reserved. 
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.KeyEvent;
import java.awt.Color;

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
import javax.swing.UIManager;
import javax.swing.SwingConstants;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

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

import net.sourceforge.dvb.projectx.xinput.XInputFile;

/**
 *
 */
public class FileProperties extends JFrame {

	private String title = "File Properties";

	private final Color head_color = new Color(224, 224, 224);

	private ComboBoxIndexListener _ComboBoxIndexListener = new ComboBoxIndexListener();
	private ComboBoxItemListener _ComboBoxItemListener = new ComboBoxItemListener();
	private CheckBoxListener _CheckBoxListener = new CheckBoxListener();
	private TextFieldListener _TextFieldListener = new TextFieldListener();
	private TextFieldKeyListener _TextFieldKeyListener = new TextFieldKeyListener();

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

		container.add(buildTabPanel());
		getContentPane().add(container);

		setTitle(title);
		setBounds(200, 100, 720, 400);
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

		setJMenuBar(menuBar);
	}

	/**
	 *
	 */
	protected JMenu buildFileMenu()
	{
		JMenu fileMenu = new JMenu();
		CommonGui.localize(fileMenu, "Common.File");

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
	protected JPanel buildTabPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBorder(BorderFactory.createEmptyBorder(5, 2, 2, 2));

		JTabbedPane logtab = new JTabbedPane(SwingConstants.LEFT);

		logtab.addTab( "Main", buildMainPanel());
		logtab.addTab( Resource.getString("TabPanel.SpecialPanel"), buildSpecialPanel());

		logtab.setSelectedIndex(0);

		panel.add(logtab, BorderLayout.CENTER);

		return panel;
	}

	/**
	 *
	 */
	protected JPanel buildHeadPanel(JPanel panel, String str)
	{
		JPanel panel_1 = new JPanel(new BorderLayout());
		panel_1.setBackground(head_color);
		panel_1.setBorder(BorderFactory.createTitledBorder(""));
		panel_1.add(new JLabel(" " + str));

		JPanel panel_2 = new JPanel(new BorderLayout());
		panel_2.add(panel, BorderLayout.CENTER);
		panel_2.add(panel_1, BorderLayout.NORTH);

		return panel_2;
	}

	/**
	 *
	 */
	protected JPanel buildMainPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout( new BorderLayout() );
		panel.setBorder( BorderFactory.createTitledBorder("") );

		return panel;
	}

	/**
	 *
	 */
	protected JPanel buildSpecialPanel()
	{
		JPanel idbigPanel = new JPanel();
		idbigPanel.setLayout( new GridLayout(1,2) );

		JPanel idPanel3 = new JPanel();
		idPanel3.setLayout ( new ColumnLayout() );
		idPanel3.setBorder( BorderFactory.createTitledBorder(Resource.getString("SpecialPanel.Title1")) );

		String[][] objects = {
			Keys.KEY_PVA_FileOverlap,
			Keys.KEY_PVA_Audio,
			Keys.KEY_VOB_resetPts,
			Keys.KEY_TS_ignoreScrambled,
			Keys.KEY_TS_blindSearch,
			Keys.KEY_TS_joinPackets,
			Keys.KEY_TS_HumaxAdaption,
			Keys.KEY_TS_FinepassAdaption,
			Keys.KEY_TS_generatePmt,
			Keys.KEY_TS_generateTtx,
			Keys.KEY_TS_setMainAudioAc3
		};

		for (int i = 0; i < objects.length; i++)
		{
			JCheckBox box = new JCheckBox(Resource.getString(objects[i][0]));
			box.setToolTipText(Resource.getString(objects[i][0] + Keys.KEY_Tip));
			box.setPreferredSize(new Dimension(270, 20));
			box.setMaximumSize(new Dimension(270, 20));
			box.setActionCommand(objects[i][0]);
			box.setSelected(Common.getSettings().getBooleanProperty(objects[i]));
			box.addActionListener(_CheckBoxListener);

			if (i == 2 || i == 3)
				idPanel3.add(Box.createRigidArea(new Dimension(1, 10)));

			idPanel3.add(box);
		}


		JComboBox tsheader_mode = new JComboBox(Keys.ITEMS_TsHeaderMode);
		tsheader_mode.setPreferredSize(new Dimension(270, 20));
		tsheader_mode.setMaximumSize(new Dimension(270, 20));
		tsheader_mode.setActionCommand(Keys.KEY_TsHeaderMode[0]);
		tsheader_mode.setSelectedIndex(Common.getSettings().getIntProperty(Keys.KEY_TsHeaderMode));
		tsheader_mode.addActionListener(_ComboBoxIndexListener);
		idPanel3.add(tsheader_mode);

		idbigPanel.add(idPanel3);


		// next grid

		JPanel idPanel2 = new JPanel();
		idPanel2.setLayout ( new ColumnLayout() );
		idPanel2.setBorder( BorderFactory.createTitledBorder(Resource.getString("SpecialPanel.Title2")) );

		JLabel gpts = new JLabel(Resource.getString("SpecialPanel.PtsShift") + " ");
		gpts.setToolTipText(Resource.getString("SpecialPanel.PtsShift.Tip"));

		JComboBox pts_shift = new JComboBox(Keys.ITEMS_PtsShift);
		pts_shift.setPreferredSize(new Dimension(60, 20));
		pts_shift.setMaximumSize(new Dimension(60, 20));
		pts_shift.setEditable(true);
		pts_shift.setActionCommand(Keys.KEY_PtsShift_Value[0]);
		pts_shift.setSelectedItem(Common.getSettings().getProperty(Keys.KEY_PtsShift_Value));
		pts_shift.addActionListener(_ComboBoxItemListener);

		JPanel spec5 = new JPanel();
		spec5.setLayout(new BoxLayout(spec5, BoxLayout.X_AXIS));
		spec5.add(gpts);  
		spec5.add(pts_shift);

		idPanel2.add(spec5);

		String[][] objects_2 = {
			Keys.KEY_Input_getEnclosedPackets,
			Keys.KEY_Input_concatenateForeignRecords,
			Keys.KEY_Audio_ignoreErrors,
			Keys.KEY_Audio_limitPts,
			Keys.KEY_Video_ignoreErrors,
			Keys.KEY_Video_trimPts
		};

		for (int i = 0; i < objects_2.length; i++)
		{
			JCheckBox box = new JCheckBox(Resource.getString(objects_2[i][0]));
			box.setToolTipText(Resource.getString(objects_2[i][0] + Keys.KEY_Tip));
			box.setPreferredSize(new Dimension(270, 20));
			box.setMaximumSize(new Dimension(270, 20));
			box.setActionCommand(objects_2[i][0]);
			box.setSelected(Common.getSettings().getBooleanProperty(objects_2[i]));
			box.addActionListener(_CheckBoxListener);

			if (i == 2 || i == 4)
				idPanel2.add(Box.createRigidArea(new Dimension(1, 10)));

			idPanel2.add(box);
		}

		idPanel2.add(Box.createRigidArea(new Dimension(1, 10)));

		idPanel2.add(new JLabel(Resource.getString("SpecialPanel.Conversion")));

		String[][] objects_3 = { Keys.KEY_Conversion_startWithVideo };

		JCheckBox box_1 = new JCheckBox(Resource.getString(objects_3[0][0]));
		box_1.setToolTipText(Resource.getString(objects_3[0][0] + Keys.KEY_Tip));
		box_1.setPreferredSize(new Dimension(270, 20));
		box_1.setMaximumSize(new Dimension(270, 20));
		box_1.setActionCommand(objects_3[0][0]);
		box_1.setSelected(Common.getSettings().getBooleanProperty(objects_3[0]));
		box_1.addActionListener(_CheckBoxListener);
		idPanel2.add(box_1);

		String[][] objects_4 = { Keys.KEY_Conversion_addPcrToStream };

		JCheckBox box_2 = new JCheckBox(Resource.getString(objects_4[0][0]));
		box_2.setToolTipText(Resource.getString(objects_4[0][0] + Keys.KEY_Tip));
		box_2.setPreferredSize(new Dimension(192, 20));
		box_2.setMaximumSize(new Dimension(192, 20));
		box_2.setActionCommand(objects_4[0][0]);
		box_2.setSelected(Common.getSettings().getBooleanProperty(objects_4[0]));
		box_2.addActionListener(_CheckBoxListener);

		String[][] objects_5 = { Keys.KEY_Conversion_PcrCounter };

		JCheckBox box_3 = new JCheckBox(Resource.getString(objects_5[0][0]));
		box_3.setToolTipText(Resource.getString(objects_5[0][0] + Keys.KEY_Tip));
		box_3.setPreferredSize(new Dimension(80, 20));
		box_3.setMaximumSize(new Dimension(80, 20));
		box_3.setActionCommand(objects_5[0][0]);
		box_3.setSelected(Common.getSettings().getBooleanProperty(objects_5[0]));
		box_3.addActionListener(_CheckBoxListener);

		JPanel spec3 = new JPanel();
		spec3.setLayout(new BoxLayout(spec3, BoxLayout.X_AXIS));
		spec3.add(box_2);  
		spec3.add(box_3);  

		idPanel2.add(spec3);


		JComboBox pcr_delta = new JComboBox(Keys.ITEMS_PcrDelta);
		pcr_delta.setPreferredSize(new Dimension(60, 20));
		pcr_delta.setMaximumSize(new Dimension(60, 20));
		pcr_delta.setEditable(true);
		pcr_delta.setActionCommand(Keys.KEY_PcrDelta_Value[0]);
		pcr_delta.setSelectedItem(Common.getSettings().getProperty(Keys.KEY_PcrDelta_Value));
		pcr_delta.addActionListener(_ComboBoxItemListener);
		idPanel2.add(pcr_delta);

		idbigPanel.add(idPanel2);

		return buildHeadPanel(idbigPanel, Resource.getString("TabPanel.SpecialPanel"));
	}

	/**
	 *
	 */
	public void close()
	{ 
		dispose();
	}

	/**
	 *
	 */
	public void open(XInputFile xInputFile, int value)
	{ 
		show();
	}
}
