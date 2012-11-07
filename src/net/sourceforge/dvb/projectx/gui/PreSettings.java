/*
 * @(#)PreSettings.java
 *
 * Copyright (c) 2005-2011 by dvb.matt, All Rights Reserved. 
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

/**
 *
 */
public class PreSettings extends JFrame {

	private String title = Resource.getString("PreferencesPanel.Title");

	private final Color head_color = new Color(224, 224, 224);

	private ComboBoxIndexListener _ComboBoxIndexListener = new ComboBoxIndexListener();
	private ComboBoxItemListener _ComboBoxItemListener = new ComboBoxItemListener();
	private CheckBoxListener _CheckBoxListener = new CheckBoxListener();
	private TextFieldListener _TextFieldListener = new TextFieldListener();
	private TextFieldKeyListener _TextFieldKeyListener = new TextFieldKeyListener();

	/**
	 * Constructor
	 */
	public PreSettings()
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
		setBounds(200, 100, 720, 480);
		setResizable(false);

		UIManager.addPropertyChangeListener(new UISwitchListener(getRootPane()));
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
	public void savePreferences()
	{ 
		String str = CommonGui.getUserInput(this, "save ini", "save inifile", Common.getSettings().getInifile());

		if (str != null && str.length() > 0)
			Common.saveSettings(str);

		toFront();
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

		JMenuItem save = new JMenuItem();
		CommonGui.localize(save, "Common.SaveAs");
		save.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				savePreferences();
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
	protected JPanel buildTabPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBorder(BorderFactory.createEmptyBorder(5, 2, 2, 2));

		JTabbedPane logtab = new JTabbedPane(SwingConstants.LEFT);

	//	logtab.addTab( "", buildMainPanel());
		logtab.addTab( Resource.getString("TabPanel.OptionPanel"), buildOptionPanel());
		logtab.addTab( Resource.getString("TabPanel.ExportPanel"), buildExportPanel());
		logtab.addTab( Resource.getString("TabPanel.SpecialPanel"), buildSpecialPanel());
		logtab.addTab( Resource.getString("TabPanel.VideoPanel"), buildVideoPanel());
		logtab.addTab( Resource.getString("TabPanel.AudioPanel"), buildAudioPanel());
		logtab.addTab( Resource.getString("TabPanel.SubtitlePanel"), buildSubtitlePanel());
		logtab.addTab( Resource.getString("TabPanel.ExternPanel"), buildExternPanel());
		logtab.addTab( Resource.getString("TabPanel.NetPanel"), buildNetPanel());
		logtab.addTab( Resource.getString("TabPanel.PostCommandsPanel"), buildPostCommandsPanel());

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
			Keys.KEY_VOB_resetPts,
			Keys.KEY_PVA_FileOverlap,
			Keys.KEY_PVA_Audio,
			Keys.KEY_TS_ignoreScrambled,
			Keys.KEY_TS_blindSearch,
			Keys.KEY_TS_joinPackets,
			Keys.KEY_TS_HumaxAdaption,
			Keys.KEY_TS_FinepassAdaption,
			Keys.KEY_TS_JepssenAdaption,
			Keys.KEY_TS_KoscomAdaption,
			//Keys.KEY_TS_ArionAdaption,
			Keys.KEY_TS_generatePmt,
			Keys.KEY_TS_generateTtx,
			Keys.KEY_TS_setMainAudioAc3
		};

		for (int i = 0, padd_offs = 2; i < objects.length; i++)
		{
			JCheckBox box = new JCheckBox(Resource.getString(objects[i][0]));
			box.setToolTipText(Resource.getString(objects[i][0] + Keys.KEY_Tip));
			box.setPreferredSize(new Dimension(270, 20));
			box.setMaximumSize(new Dimension(270, 20));
			box.setActionCommand(objects[i][0]);
			box.setSelected(Common.getSettings().getBooleanProperty(objects[i]));
			box.addActionListener(_CheckBoxListener);

		//	if (i == padd_offs)
		//		idPanel3.add(Box.createRigidArea(new Dimension(1, 8)));

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
			Keys.KEY_Input_useReadOverhead,
			Keys.KEY_Audio_ignoreErrors,
			Keys.KEY_Audio_limitPts,
			Keys.KEY_Audio_allowFormatChanges,
			Keys.KEY_Video_ignoreErrors,
			Keys.KEY_Video_trimPts,
			Keys.KEY_Video_cutPts
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

			if (i == 3 || i == 6)
				idPanel2.add(Box.createRigidArea(new Dimension(1, 2)));

			idPanel2.add(box);
		}

		//idPanel2.add(Box.createRigidArea(new Dimension(1, 4)));

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
	protected JPanel buildExportPanel()
	{
		JPanel exportPanel = new JPanel();
		exportPanel.setLayout( new GridLayout(2, 2) );

		JPanel op1 = new JPanel();
		op1.setLayout( new ColumnLayout() );
		op1.setBorder( BorderFactory.createTitledBorder(Resource.getString("ExportPanel.SplitPanel")) );

		String[][] objects = {
			Keys.KEY_SplitSize,
			Keys.KEY_Streamtype_MpgVideo,
			Keys.KEY_Streamtype_MpgAudio,
			Keys.KEY_Streamtype_Ac3Audio,
			Keys.KEY_Streamtype_PcmAudio,
			Keys.KEY_Streamtype_Teletext,
			Keys.KEY_Streamtype_Subpicture,
			Keys.KEY_Streamtype_Vbi,
			Keys.KEY_WriteOptions_writeVideo,
			Keys.KEY_WriteOptions_writeAudio,
			Keys.KEY_OptionHorizontalResolution,
			Keys.KEY_OptionDAR,
			Keys.KEY_additionalOffset,
			Keys.KEY_ExportPanel_Export_Overlap
		};

		JCheckBox[] box = new JCheckBox[objects.length];

		for (int i = 0; i < objects.length; i++)
		{
			box[i] = new JCheckBox(Resource.getString(objects[i][0]));
			box[i].setToolTipText(Resource.getString(objects[i][0] + Keys.KEY_Tip));
			box[i].setActionCommand(objects[i][0]);
			box[i].setSelected(Common.getSettings().getBooleanProperty(objects[i]));
			box[i].addActionListener(_CheckBoxListener);
		}

		JComboBox split_sizes = new JComboBox(Keys.ITEMS_Export_SplitSize);
		split_sizes.setPreferredSize(new Dimension(100, 22));
		split_sizes.setMaximumSize(new Dimension(100, 22));
		split_sizes.setEditable(true);
		split_sizes.setActionCommand(Keys.KEY_ExportPanel_SplitSize_Value[0]);
		split_sizes.setSelectedItem(Common.getSettings().getProperty(Keys.KEY_ExportPanel_SplitSize_Value));
		split_sizes.addActionListener(_ComboBoxItemListener);

		JPanel sp1 = new JPanel();
		sp1.setLayout(new BoxLayout(sp1, BoxLayout.X_AXIS));
		sp1.add(box[0]);  
		sp1.add(split_sizes);  

		op1.add(sp1);


		JComboBox overlap = new JComboBox(Keys.ITEMS_Export_Overlap);
		overlap.setPreferredSize(new Dimension(100, 22));
		overlap.setMaximumSize(new Dimension(100, 22));
		overlap.setActionCommand(Keys.KEY_ExportPanel_Overlap_Value[0]);
		overlap.setSelectedIndex(Common.getSettings().getIntProperty(Keys.KEY_ExportPanel_Overlap_Value));
		overlap.addActionListener(_ComboBoxIndexListener);

		JPanel sp2 = new JPanel();
		sp2.setLayout(new BoxLayout(sp2, BoxLayout.X_AXIS));
		sp2.add(box[13]);  
		sp2.add(overlap);  

		op1.add(sp2);

		op1.add(Box.createRigidArea(new Dimension(1, 10)));

		JPanel op6 = new JPanel();
		op6.setLayout(new BoxLayout(op6, BoxLayout.X_AXIS));
		op6.add(new JLabel(Resource.getString("ExportPanel.WriteOptions.InfoScan")));

		JComboBox infoscan = new JComboBox(Keys.ITEMS_Infoscan);
		infoscan.setPreferredSize(new Dimension(60, 22));
		infoscan.setMaximumSize(new Dimension(60, 22));
		infoscan.setEditable(true);
		infoscan.setActionCommand(Keys.KEY_ExportPanel_Infoscan_Value[0]);
		infoscan.setSelectedItem(Common.getSettings().getProperty(Keys.KEY_ExportPanel_Infoscan_Value));
		infoscan.addActionListener(_ComboBoxItemListener);
		op6.add(infoscan);

		op1.add(op6);

		exportPanel.add(op1);

		JPanel idPanel = new JPanel();
		idPanel.setBorder(BorderFactory.createTitledBorder(Resource.getString("ExportPanel.StreamtypePanel")));
		idPanel.setLayout(new BoxLayout(idPanel, BoxLayout.X_AXIS));
		idPanel.setToolTipText(Resource.getString("ExportPanel.StreamtypePanel.Tip"));

		JPanel panel_1 = new JPanel();
		panel_1.setLayout ( new ColumnLayout() );

		for (int i = 1; i < 5; i++)
			panel_1.add(box[i]);

		JPanel panel_2 = new JPanel();
		panel_2.setLayout ( new ColumnLayout() );

		for (int i = 5; i < 8; i++)
			panel_2.add(box[i]);

		JTextField language_code = new JTextField(Common.getSettings().getProperty(Keys.KEY_LanguageFilter));
		language_code.setPreferredSize(new Dimension(100, 22));
		language_code.setMaximumSize(new Dimension(100, 22));
		language_code.setToolTipText(Resource.getString(Keys.KEY_LanguageFilter[0] + Keys.KEY_Tip));
		language_code.setEditable(true);
		language_code.setActionCommand(Keys.KEY_LanguageFilter[0]);
		language_code.addActionListener(_TextFieldListener);
		language_code.addKeyListener(_TextFieldKeyListener);
		panel_2.add(language_code);

		idPanel.add(panel_1);
		idPanel.add(panel_2);

		exportPanel.add(idPanel);

		JPanel op4 = new JPanel();
		op4.setLayout( new ColumnLayout() );
		op4.setBorder( BorderFactory.createTitledBorder(Resource.getString("ExportPanel.WriteOptions")) );
		op4.setToolTipText(Resource.getString("ExportPanel.WriteOptions.Tip"));

		op4.add(box[8]);
		op4.add(box[9]);

		op4.add(Box.createRigidArea(new Dimension(1, 5)));

		/**
		 *
		 */
		JPanel CL2 = new JPanel();
		CL2.setLayout(new BoxLayout(CL2, BoxLayout.X_AXIS));

		box[10].setPreferredSize(new Dimension(110, 20));
		box[10].setMaximumSize(new Dimension(110, 20));
		CL2.add(box[10]);  


		JComboBox combobox_34 = new JComboBox(Keys.ITEMS_ExportHorizontalResolution);
		combobox_34.setMaximumRowCount(7);
		combobox_34.setPreferredSize(new Dimension(90, 20));
		combobox_34.setMaximumSize(new Dimension(90, 20));
		combobox_34.setActionCommand(Keys.KEY_ExportHorizontalResolution[0]);
		combobox_34.setEditable(true);
		combobox_34.setSelectedItem(Common.getSettings().getProperty(Keys.KEY_ExportHorizontalResolution));
		combobox_34.addActionListener(_ComboBoxItemListener);
		CL2.add(combobox_34);

		op4.add(CL2);

		/**
		 *
		 */
		JPanel CL3 = new JPanel();
		CL3.setLayout(new BoxLayout(CL3, BoxLayout.X_AXIS));

		box[11].setPreferredSize(new Dimension(80, 20));
		box[11].setMaximumSize(new Dimension(80, 20));
		CL3.add(box[11]);  

		JComboBox combobox_24 = new JComboBox(Keys.ITEMS_ExportDAR);
		combobox_24.setMaximumRowCount(7);
		combobox_24.setPreferredSize(new Dimension(120, 20));
		combobox_24.setMaximumSize(new Dimension(120, 20));
		combobox_24.setActionCommand(Keys.KEY_ExportDAR[0]);
		combobox_24.setSelectedIndex(Common.getSettings().getIntProperty(Keys.KEY_ExportDAR));
		combobox_24.addActionListener(_ComboBoxIndexListener);
		CL3.add(combobox_24);

		op4.add(CL3);

		exportPanel.add(op4);

		JPanel op5 = new JPanel();
		op5.setLayout( new ColumnLayout() );
		op5.setBorder( BorderFactory.createTitledBorder(Resource.getString("ExportPanel.additionalOffset.Title")) );

		JPanel op7 = new JPanel();
		op7.setLayout(new BoxLayout(op7, BoxLayout.X_AXIS));

		op7.add(box[12]);

		JTextField offset_value = new JTextField(Common.getSettings().getProperty(Keys.KEY_ExportPanel_additionalOffset_Value));
		offset_value.setPreferredSize(new Dimension(80, 22));
		offset_value.setMaximumSize(new Dimension(80, 22));
		offset_value.setToolTipText(Resource.getString(Keys.KEY_ExportPanel_additionalOffset_Value[0] + Keys.KEY_Tip));
		offset_value.setEditable(true);
		offset_value.setActionCommand(Keys.KEY_ExportPanel_additionalOffset_Value[0]);
		offset_value.addActionListener(_TextFieldListener);
		offset_value.addKeyListener(_TextFieldKeyListener);
		op7.add(offset_value);

		op5.add(op7);

		exportPanel.add(op5);

		return buildHeadPanel(exportPanel, Resource.getString("TabPanel.ExportPanel"));
	}

	/**
	 *
	 */
	protected JPanel buildVideoPanel()
	{
		JPanel video1 = new JPanel();
		video1.setLayout( new GridLayout(1, 2) );

		JPanel video2Panel = new JPanel();
		video2Panel.setLayout( new ColumnLayout() );
		video2Panel.setBorder( BorderFactory.createTitledBorder(Resource.getString("VideoPanel.Title1")) );

		String[][] objects = {
			Keys.KEY_VideoPanel_addEndcode,
			Keys.KEY_VideoPanel_insertEndcode,
			Keys.KEY_VideoPanel_addSequenceHeader,
			Keys.KEY_VideoPanel_clearCDF,
			Keys.KEY_VideoPanel_patchToProgressive,
			Keys.KEY_VideoPanel_patchToInterlaced,
			Keys.KEY_VideoPanel_toggleFieldorder,
			Keys.KEY_VideoPanel_addSde
		};

		final JCheckBox[] box = new JCheckBox[objects.length];

		for (int i = 0; i < objects.length; i++)
		{
			box[i] = new JCheckBox(Resource.getString(objects[i][0]));
			box[i].setPreferredSize(new Dimension(270, 20));
			box[i].setMaximumSize(new Dimension(270, 20));
			box[i].setToolTipText(Resource.getString(objects[i][0] + Keys.KEY_Tip));
			box[i].setActionCommand(objects[i][0]);
			box[i].setSelected(Common.getSettings().getBooleanProperty(objects[i]));
			box[i].addActionListener(_CheckBoxListener);
		}

		ActionListener al = new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				JCheckBox checkBox = (JCheckBox)e.getSource();
				String str = checkBox.getActionCommand();

				if (str.equals(Keys.KEY_VideoPanel_patchToProgressive[0]) && checkBox.isSelected())
				{
					box[5].setSelected(false);
					Common.getSettings().setBooleanProperty(Keys.KEY_VideoPanel_patchToInterlaced[0], false);
					return;
				}

				else if (str.equals(Keys.KEY_VideoPanel_patchToInterlaced[0]) && checkBox.isSelected())
				{
					box[4].setSelected(false);
					Common.getSettings().setBooleanProperty(Keys.KEY_VideoPanel_patchToProgressive[0], false);
					return;
				}
			}
		};
	
		box[4].addActionListener(al);
		box[5].addActionListener(al);

		for (int i = 0; i < 7; i++)
			video2Panel.add(box[i]);

		JPanel SdePanel = new JPanel();
		SdePanel.setLayout(new BoxLayout(SdePanel, BoxLayout.X_AXIS));

		box[7].setPreferredSize(new Dimension(180, 20));
		box[7].setMaximumSize(new Dimension(180, 20));
		SdePanel.add(box[7]);

		JTextField sde_value = new JTextField(Common.getSettings().getProperty(Keys.KEY_VideoPanel_SdeValue));
		sde_value.setPreferredSize(new Dimension(80, 20));
		sde_value.setMaximumSize(new Dimension(80, 20));
		sde_value.setToolTipText(Resource.getString(Keys.KEY_VideoPanel_SdeValue[0] + Keys.KEY_Tip));
		sde_value.setEditable(true);
		sde_value.setActionCommand(Keys.KEY_VideoPanel_SdeValue[0]);
		sde_value.addActionListener(_TextFieldListener);
		sde_value.addKeyListener(_TextFieldKeyListener);
		SdePanel.add(sde_value);

		video2Panel.add(SdePanel);

		video2Panel.add(new JLabel (Resource.getString("VideoPanel.patchResolution")));


		JPanel hPPanel = new JPanel();
		hPPanel.setLayout(new BoxLayout(hPPanel, BoxLayout.X_AXIS));
		hPPanel.setToolTipText(Resource.getString("VideoPanel.patchResolution.Tip"));

		JComboBox combobox_35 = new JComboBox(Keys.ITEMS_ConditionalHorizontalPatch);
		combobox_35.setPreferredSize(new Dimension(160, 20));
		combobox_35.setMaximumSize(new Dimension(160, 20));
		combobox_35.setActionCommand(Keys.KEY_ConditionalHorizontalPatch[0]);
		combobox_35.setSelectedIndex(Common.getSettings().getIntProperty(Keys.KEY_ConditionalHorizontalPatch));
		combobox_35.addActionListener(_ComboBoxIndexListener);
		hPPanel.add(combobox_35);

		JComboBox combobox_22 = new JComboBox(Keys.ITEMS_ExportHorizontalResolution);
		combobox_22.setPreferredSize(new Dimension(50, 20));
		combobox_22.setMaximumSize(new Dimension(50, 20));
		combobox_22.setActionCommand(Keys.KEY_ConditionalHorizontalResolution[0]);
		combobox_22.setSelectedItem(Common.getSettings().getProperty(Keys.KEY_ConditionalHorizontalResolution));
		combobox_22.addActionListener(_ComboBoxItemListener);
		hPPanel.add(combobox_22);

		video2Panel.add(hPPanel);

		video2Panel.add(Box.createRigidArea(new Dimension(1, 10)));

		JPanel video2 = new JPanel();
		video2.setLayout(new ColumnLayout());
		video2.setBorder( BorderFactory.createTitledBorder(Resource.getString("VideoPanel.Title1")) );

		String[] labels = {
			Resource.getString("VideoPanel.ChangeVbvBuffer"),
			Resource.getString("VideoPanel.ChangeVbvDelay"),
			Resource.getString("VideoPanel.ChangeAspectRatio")
		};

		Object[][] items = {
			Keys.ITEMS_ChangeVbvBuffer,
			Keys.ITEMS_ChangeVbvDelay,
			Keys.ITEMS_ChangeAspectRatio,
		};

		String[][] keys = {
			Keys.KEY_ChangeVbvBuffer,
			Keys.KEY_ChangeVbvDelay,
			Keys.KEY_ChangeAspectRatio
		};

		for (int i = 0; i < keys.length; i++)
		{
			JLabel label = new JLabel(labels[i]);
			label.setPreferredSize(new Dimension(120, 20));
			label.setMaximumSize(new Dimension(120, 20));

			JComboBox combobox = new JComboBox(items[i]);
			combobox.setPreferredSize(new Dimension(150, 20));
			combobox.setMaximumSize(new Dimension(150, 20));
			combobox.setActionCommand(keys[i][0]);
			combobox.setSelectedIndex(Common.getSettings().getIntProperty(keys[i]));
			combobox.addActionListener(_ComboBoxIndexListener);

			JPanel panel = new JPanel();
			panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
			panel.setToolTipText(labels[i] + Keys.KEY_Tip);
			panel.add(label);
			panel.add(combobox);

			video2Panel.add(panel);
		}

		video1.add(video2Panel);


		JPanel video3 = new JPanel();
		video3.setLayout( new GridLayout(2, 1) );


		JPanel newBrPanel = new JPanel();
		newBrPanel.setLayout(new ColumnLayout());
		newBrPanel.setBorder( BorderFactory.createTitledBorder(Resource.getString("VideoPanel.Title2")) );

		String[] labels_2 = {
			Resource.getString("VideoPanel.patchBitrateValue"),
			Resource.getString("VideoPanel.patch1stBitrateValue")
		};

		Object[][] items_2 = {
			Keys.ITEMS_BitrateInAllSequences,
			Keys.ITEMS_BitrateInFirstSequence
		};

		String[][] keys_2 = {
			Keys.KEY_ChangeBitrateInAllSequences,
			Keys.KEY_ChangeBitrateInFirstSequence
		};

		for (int i = 0; i < keys_2.length; i++)
		{
			JLabel label = new JLabel(labels_2[i]);
			label.setPreferredSize(new Dimension(270, 20));
			label.setMaximumSize(new Dimension(270, 20));
			label.setToolTipText(labels_2[i] + Keys.KEY_Tip);

			JComboBox combobox = new JComboBox(items_2[i]);
			combobox.setPreferredSize(new Dimension(270, 20));
			combobox.setMaximumSize(new Dimension(270, 20));
			combobox.setActionCommand(keys_2[i][0]);
			combobox.setSelectedIndex(Common.getSettings().getIntProperty(keys_2[i]));
			combobox.addActionListener(_ComboBoxIndexListener);

			newBrPanel.add(label);
			newBrPanel.add(combobox);
		}

		video3.add(newBrPanel);

		video1.add(video3);

		return buildHeadPanel(video1, Resource.getString("TabPanel.VideoPanel"));
	}

	/**
	 *
	 */
	protected JPanel buildExternPanel()
	{
		JPanel video2 = new JPanel();
		video2.setLayout( new GridLayout(1, 2) );

		JPanel video2Panel = new JPanel();
		video2Panel.setLayout( new ColumnLayout() );
		video2Panel.setBorder( BorderFactory.createTitledBorder(Resource.getString("ExternPanel.Title1")) );
		video2Panel.setToolTipText(Resource.getString("ExternPanel.Title1.Tip"));

		//	Keys.KEY_ExternPanel_save1stFrameOfGop,

		String[][] objects = {
			Keys.KEY_ExternPanel_createVdrIndex,
			Keys.KEY_ExternPanel_createCellTimes,
			Keys.KEY_ExternPanel_exportPts,
			Keys.KEY_ExternPanel_createChapters,
			Keys.KEY_ExternPanel_renameAudio,
			Keys.KEY_ExternPanel_renameVideo,
			Keys.KEY_ExternPanel_appendExtension,
			Keys.KEY_ExternPanel_appendPidToFileName,
			Keys.KEY_ExternPanel_appendLangToFileName,
			Keys.KEY_ExternPanel_createM2sIndex,
            Keys.KEY_ExternPanel_createInfoIndex,
			Keys.KEY_ExternPanel_createD2vIndex,
			Keys.KEY_ExternPanel_createDgiIndex,
			Keys.KEY_ExternPanel_splitProjectFile
		};

		JCheckBox[] box = new JCheckBox[objects.length];

		for (int i = 0, j = 12; i < objects.length; i++)
		{
			box[i] = new JCheckBox(Resource.getString(objects[i][0]));
			box[i].setPreferredSize(new Dimension(270, 20));
			box[i].setMaximumSize(new Dimension(270, 20));
			box[i].setToolTipText(Resource.getString(objects[i][0] + Keys.KEY_Tip));
			box[i].setActionCommand(objects[i][0]);
			box[i].setSelected(Common.getSettings().getBooleanProperty(objects[i]));
			box[i].addActionListener(_CheckBoxListener);

			if (i == j)
				box[i].setEnabled(false);
		}

		// left grid
		for (int i = 0; i < 9; i++)
			video2Panel.add(box[i]);

		video2.add(video2Panel);

		// next grid

		JPanel video3Panel = new JPanel();
		video3Panel.setLayout( new ColumnLayout() );
		video3Panel.setBorder( BorderFactory.createTitledBorder(Resource.getString("ExternPanel.Title2")) );
		video3Panel.setToolTipText(Resource.getString("ExternPanel.Title2.Tip"));

		video3Panel.add(new JLabel(Resource.getString("ExternPanel.createM2sIndex")));

		// right grid
		video3Panel.add(box[9]);

		video3Panel.add(Box.createRigidArea(new Dimension(1, 10)));

        video3Panel.add(new JLabel(Resource.getString("ExternPanel.createInfoLabel")));

        video3Panel.add(box[10]);

        video3Panel.add(Box.createRigidArea(new Dimension(1, 10)));

		video3Panel.add(new JLabel(Resource.getString("ExternPanel.createD2vIndex")));

		// right grid
		for (int i = 11; i < objects.length; i++)
			video3Panel.add(box[i]);

		JTextField d2v_splitsize = new JTextField(Common.getSettings().getProperty(Keys.KEY_ExternPanel_ProjectFileSplitSize));
		d2v_splitsize.setPreferredSize(new Dimension(70, 20));
		d2v_splitsize.setToolTipText(Resource.getString(Keys.KEY_ExternPanel_ProjectFileSplitSize[0] + Keys.KEY_Tip));
		d2v_splitsize.setEditable(true);
		d2v_splitsize.setActionCommand(Keys.KEY_ExternPanel_ProjectFileSplitSize[0]);
		d2v_splitsize.addActionListener(_TextFieldListener);
		d2v_splitsize.addKeyListener(_TextFieldKeyListener);

		JPanel d2vPanel = new JPanel();
		JLabel d2vLabel = new JLabel (Resource.getString("ExternPanel.ProjectFileSplitSize"));
		d2vPanel.add(d2vLabel);
		d2vPanel.add(d2v_splitsize);

		video3Panel.add(d2vPanel);

		video2.add(video3Panel);

		return buildHeadPanel(video2, Resource.getString("TabPanel.ExternPanel"));
	}

	/**
	 *
	 */
	protected JPanel buildAudioPanel()
	{
		JPanel audio = new JPanel();
		audio.setLayout( new GridLayout(1,2) );

		JPanel audio0 = new JPanel();
		audio0.setLayout( new ColumnLayout() );
		audio0.setBorder( BorderFactory.createTitledBorder(Resource.getString("AudioPanel.Title1")) );

		audio0.add(new JLabel(Resource.getString("AudioPanel.losslessMpaConversion.Tip1")));
		audio0.add(new JLabel(Resource.getString("AudioPanel.losslessMpaConversion.Tip2")));
		audio0.setToolTipText(Resource.getString("AudioPanel.losslessMpaConversion.Tip"));

		JComboBox conversion_selection = new JComboBox(Keys.ITEMS_losslessMpaConversionMode);
		conversion_selection.setPreferredSize(new Dimension(270, 20));
		conversion_selection.setMaximumSize(new Dimension(270, 20));
		conversion_selection.setActionCommand(Keys.KEY_AudioPanel_losslessMpaConversionMode[0]);
		conversion_selection.setSelectedIndex(Common.getSettings().getIntProperty(Keys.KEY_AudioPanel_losslessMpaConversionMode));
		conversion_selection.addActionListener(_ComboBoxIndexListener);
		audio0.add(conversion_selection);

		//audio0.add(new JLabel(" "));
		audio0.add(Box.createRigidArea(new Dimension(1, 20)));

		String[][] objects = {
			Keys.KEY_AudioPanel_decodeMpgAudio,
			Keys.KEY_AudioPanel_Normalize,
			Keys.KEY_AudioPanel_Downmix,
			Keys.KEY_AudioPanel_fadeInOut,
			Keys.KEY_AudioPanel_changeByteorder,
			Keys.KEY_AudioPanel_addRiffHeader,
			Keys.KEY_AudioPanel_addAiffHeader,
			Keys.KEY_AudioPanel_validateCRC,
			Keys.KEY_AudioPanel_clearCRC,
			Keys.KEY_AudioPanel_fillGapsWithLastFrame,
			Keys.KEY_AudioPanel_addFrames,
			Keys.KEY_AudioPanel_allowSpaces,
			Keys.KEY_AudioPanel_addRiffToMpgAudio,
			Keys.KEY_AudioPanel_addRiffToMpgAudioL3,
			Keys.KEY_AudioPanel_addRiffToAc3,
			Keys.KEY_AudioPanel_AC3_patch1stHeader,
			Keys.KEY_AudioPanel_AC3_replaceWithSilence,
			Keys.KEY_AudioPanel_AC3_BitrateAdaption,
			Keys.KEY_AudioPanel_createDDWave
		};

		final JCheckBox[] box = new JCheckBox[objects.length];

		for (int i = 0; i < objects.length; i++)
		{
			box[i] = new JCheckBox(Resource.getString(objects[i][0]));
			box[i].setPreferredSize(new Dimension(270, 20));
			box[i].setMaximumSize(new Dimension(270, 20));
			box[i].setToolTipText(Resource.getString(objects[i][0] + Keys.KEY_Tip));
			box[i].setActionCommand(objects[i][0]);
			box[i].setSelected(Common.getSettings().getBooleanProperty(objects[i]));
			box[i].addActionListener(_CheckBoxListener);
		}

		audio0.add(box[0]);

		JComboBox resample_selection = new JComboBox(Keys.ITEMS_resampleAudioMode);
		resample_selection.setPreferredSize(new Dimension(270, 20));
		resample_selection.setMaximumSize(new Dimension(270, 20));
		resample_selection.setActionCommand(Keys.KEY_AudioPanel_resampleAudioMode[0]);
		resample_selection.setSelectedIndex(Common.getSettings().getIntProperty(Keys.KEY_AudioPanel_resampleAudioMode));
		resample_selection.addActionListener(_ComboBoxIndexListener);
		audio0.add(resample_selection);

		final JTextField normalize_value = new JTextField(Common.getSettings().getProperty(Keys.KEY_AudioPanel_NormalizeValue));
		normalize_value.setPreferredSize(new Dimension(50, 20));
		normalize_value.setMaximumSize(new Dimension(50, 20));
		normalize_value.setToolTipText(Resource.getString(Keys.KEY_AudioPanel_NormalizeValue[0] + Keys.KEY_Tip));
		normalize_value.setEditable(true);
		normalize_value.setActionCommand(Keys.KEY_AudioPanel_NormalizeValue[0]);
		normalize_value.addActionListener(_TextFieldListener);
		normalize_value.addKeyListener(_TextFieldKeyListener);
		normalize_value.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				String str = normalize_value.getText();

				if (str.length() == 0)
				{
					normalize_value.setText("98");
					return;
				}

				try {
					int val = Integer.parseInt(str);

					if (val > 100 || val < 0)
						val = 98;

					normalize_value.setText("" + val);

				} catch (Exception pe) {

					normalize_value.setText("98");
				}
			}
		});

		box[1].setPreferredSize(new Dimension(180, 20));
		box[1].setMaximumSize(new Dimension(180, 20));

		JPanel audio5 = new JPanel();
		audio5.setLayout(new BoxLayout(audio5, BoxLayout.X_AXIS));
		audio5.add(box[1]);
		audio5.add(normalize_value);

		audio0.add(audio5);

		for (int i = 2; i < 7; i++)
			audio0.add(box[i]);


		ActionListener al = new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				JCheckBox checkBox = (JCheckBox)e.getSource();
				String str = checkBox.getActionCommand();

				if (str.equals(Keys.KEY_AudioPanel_changeByteorder[0]) && Common.getSettings().getBooleanProperty(Keys.KEY_AudioPanel_addAiffHeader))
				{
					box[4].setSelected(true);
					Common.getSettings().setBooleanProperty(Keys.KEY_AudioPanel_changeByteorder[0], true);
					return;
				}

				else if (str.equals(Keys.KEY_AudioPanel_addRiffHeader[0]) && checkBox.isSelected())
				{
					box[6].setSelected(false);
					Common.getSettings().setBooleanProperty(Keys.KEY_AudioPanel_addAiffHeader[0], false);
					return;
				}

				else if (str.equals(Keys.KEY_AudioPanel_addAiffHeader[0]) && checkBox.isSelected())
				{
					box[4].setSelected(true);
					box[5].setSelected(false);
					Common.getSettings().setBooleanProperty(Keys.KEY_AudioPanel_changeByteorder[0], true);
					Common.getSettings().setBooleanProperty(Keys.KEY_AudioPanel_addRiffHeader[0], false);
					return;
				}
			}
		};

		box[4].addActionListener(al);
		box[5].addActionListener(al);
		box[6].addActionListener(al);


		JPanel audio1 = new JPanel();
		audio1.setLayout( new ColumnLayout() );
		audio1.setBorder( BorderFactory.createTitledBorder(Resource.getString("AudioPanel.Title2")) );

		for (int i = 7; i < objects.length; i++)
		{
			if (i == 12 || i == 15 || i == 18)
				audio1.add(Box.createRigidArea(new Dimension(1, 10)));

			audio1.add(box[i]);
		}

		ActionListener al_2 = new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				JCheckBox checkBox = (JCheckBox)e.getSource();
				String str = checkBox.getActionCommand();

				if (str.equals(Keys.KEY_AudioPanel_addRiffToMpgAudioL3[0]) && checkBox.isSelected())
				{
					box[12].setSelected(false);
					Common.getSettings().setBooleanProperty(Keys.KEY_AudioPanel_addRiffToMpgAudio[0], false);
					return;
				}

				else if (str.equals(Keys.KEY_AudioPanel_addRiffToMpgAudio[0]) && checkBox.isSelected())
				{
					box[13].setSelected(false);
					Common.getSettings().setBooleanProperty(Keys.KEY_AudioPanel_addRiffToMpgAudioL3[0], false);
					return;
				}
			}
		};

		box[12].addActionListener(al_2);
		box[13].addActionListener(al_2);

		audio.add(audio0);
		audio.add(audio1);

		return buildHeadPanel(audio, Resource.getString("TabPanel.AudioPanel"));
	}

	/**
	 *
	 */
	protected JPanel buildSubtitlePanel()
	{
		JPanel teletext = new JPanel();
		teletext.setLayout( new GridLayout( 1, 2) );

		JPanel panel_0 = new JPanel();
		panel_0.setLayout( new ColumnLayout() );
		panel_0.setBorder( BorderFactory.createTitledBorder(Resource.getString("SubtitlePanel.Title.Teletext")) );

		String[][] objects = {
			//Keys.KEY_SubtitlePanel_decodeMegaradio,
			Keys.KEY_SubtitlePanel_decodeHiddenRows,
			Keys.KEY_SubtitlePanel_rebuildPTStoggle,
			Keys.KEY_SubtitlePanel_rebuildPTS,
			Keys.KEY_SubtitlePanel_rebuildPictPTS,
			Keys.KEY_SubtitlePanel_keepOriginalTimecode,
			Keys.KEY_SubtitlePanel_TtxExportBoxedOnly,
			Keys.KEY_SubtitlePanel_exportTextAsUnicode,
			Keys.KEY_SubtitlePanel_exportTextAsUTF8,
			Keys.KEY_SubtitlePanel_useTextOutline,
			Keys.KEY_SubtitlePanel_useTextAlignment,
			Keys.KEY_SubtitlePanel_keepColourTable,
			Keys.KEY_SubtitlePanel_exportAsVobSub
		};

		final JCheckBox[] box = new JCheckBox[objects.length];

		for (int i = 0; i < objects.length; i++)
		{
			box[i] = new JCheckBox(Resource.getString(objects[i][0]));
			box[i].setPreferredSize(new Dimension(260, 20));
			box[i].setMaximumSize(new Dimension(260, 20));
			box[i].setToolTipText(Resource.getString(objects[i][0] + Keys.KEY_Tip));
			box[i].setActionCommand(objects[i][0]);
			box[i].setSelected(Common.getSettings().getBooleanProperty(objects[i]));
			box[i].addActionListener(_CheckBoxListener);
		}

		for (int i = 0; i < 8; i++)
			panel_0.add(box[i]);

		//toggle action
		ActionListener al = new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				JCheckBox checkBox = (JCheckBox)e.getSource();
				String str = checkBox.getActionCommand();

				if (str.equals(Keys.KEY_SubtitlePanel_exportTextAsUnicode[0]) && checkBox.isSelected())
				{
					box[6].setSelected(false);
					Common.getSettings().setBooleanProperty(Keys.KEY_SubtitlePanel_exportTextAsUTF8[0], false);
					return;
				}

				else if (str.equals(Keys.KEY_SubtitlePanel_exportTextAsUTF8[0]) && checkBox.isSelected())
				{
					box[5].setSelected(false);
					Common.getSettings().setBooleanProperty(Keys.KEY_SubtitlePanel_exportTextAsUnicode[0], false);
					return;
				}
			}
		};
	
		box[6].addActionListener(al);
		box[7].addActionListener(al);


	//	panel_0.add(Box.createRigidArea(new Dimension(1, 10)));

		JLabel page_decode = new JLabel(Resource.getString("SubtitlePanel.TtxPages"));
		page_decode.setToolTipText(Resource.getString("SubtitlePanel.TtxPages.Tip"));
		panel_0.add(page_decode);

		JPanel panel_0_1 = new JPanel();
		panel_0_1.setLayout(new BoxLayout(panel_0_1, BoxLayout.X_AXIS));

		JPanel panel_0_2 = new JPanel();
		panel_0_2.setLayout(new BoxLayout(panel_0_2, BoxLayout.X_AXIS));

		String[][] keys = {
			Keys.KEY_SubtitlePanel_TtxPage1,
			Keys.KEY_SubtitlePanel_TtxPage2,
			Keys.KEY_SubtitlePanel_TtxPage3,
			Keys.KEY_SubtitlePanel_TtxPage4,
			Keys.KEY_SubtitlePanel_TtxPage5,
			Keys.KEY_SubtitlePanel_TtxPage6,
			Keys.KEY_SubtitlePanel_TtxPage7,
			Keys.KEY_SubtitlePanel_TtxPage8
		};

		Object[] pagenumber = { "null", "149", "150", "199", "299", "599", "691", "692", "693", "694", "699", "777", "779", "784", "785", "786", "881", "882", "884", "885", "886", "887", "888", "889" };

		for (int i = 0; i < keys.length; i++)
		{
			JComboBox combobox = new JComboBox(pagenumber);
			combobox.setPreferredSize(new Dimension(64, 22));
			combobox.setMaximumSize(new Dimension(64, 22));
			combobox.setEditable(true);
			combobox.setActionCommand(keys[i][0]);
			combobox.setSelectedItem(Common.getSettings().getProperty(keys[i]));
			combobox.addActionListener(_ComboBoxItemListener);

			if (i < 4)
				panel_0_1.add(combobox);
			else
				panel_0_2.add(combobox);
		}

		panel_0.add(panel_0_1);
		panel_0.add(panel_0_2);

		JPanel panel_0_3 = new JPanel();
		panel_0_3.setLayout(new BoxLayout(panel_0_3, BoxLayout.X_AXIS));

		JLabel lang_decode = new JLabel(Resource.getString("SubtitlePanel.Language"));
		lang_decode.setToolTipText(Resource.getString("SubtitlePanel.Language.Tip"));
		lang_decode.setPreferredSize(new Dimension(80, 22));
		lang_decode.setMaximumSize(new Dimension(80, 22));
		panel_0_3.add(lang_decode);

		JComboBox language_pair = new JComboBox(Keys.ITEMS_TtxLanguagePair);
		language_pair.setPreferredSize(new Dimension(140, 22));
		language_pair.setMaximumSize(new Dimension(140, 22));
		language_pair.setActionCommand(Keys.KEY_TtxLanguagePair[0]);
		language_pair.setSelectedIndex(Common.getSettings().getIntProperty(Keys.KEY_TtxLanguagePair));
		language_pair.addActionListener(_ComboBoxIndexListener);
		panel_0_3.add(language_pair);

		panel_0.add(panel_0_3);

		panel_0.add(Box.createRigidArea(new Dimension(1, 10)));

		JPanel panel_0_4 = new JPanel();
		panel_0_4.setLayout(new BoxLayout(panel_0_4, BoxLayout.X_AXIS));
		panel_0_4.setToolTipText(Resource.getString("SubtitlePanel.Format.Tip"));
		panel_0_4.add(new JLabel("1. " + Resource.getString("SubtitlePanel.Format")));

		JComboBox export_format = new JComboBox(Keys.ITEMS_SubtitleExportFormat);
		export_format.setPreferredSize(new Dimension(80, 22));
		export_format.setMaximumSize(new Dimension(80, 22));
		export_format.setActionCommand(Keys.KEY_SubtitleExportFormat[0]);
		export_format.setSelectedItem(Common.getSettings().getProperty(Keys.KEY_SubtitleExportFormat));
		export_format.addActionListener(_ComboBoxItemListener);
		panel_0_4.add(export_format);
	
		panel_0.add(panel_0_4);

		JPanel panel_0_5 = new JPanel();
		panel_0_5.setLayout(new BoxLayout(panel_0_5, BoxLayout.X_AXIS));
		panel_0_5.setToolTipText(Resource.getString("SubtitlePanel.Format.Tip"));
		panel_0_5.add(new JLabel("2. " + Resource.getString("SubtitlePanel.Format")));

		JComboBox export_format_2 = new JComboBox(Keys.ITEMS_SubtitleExportFormat);
		export_format_2.setPreferredSize(new Dimension(80, 22));
		export_format_2.setMaximumSize(new Dimension(80, 22));
		export_format_2.setActionCommand(Keys.KEY_SubtitleExportFormat_2[0]);
		export_format_2.setSelectedItem(Common.getSettings().getProperty(Keys.KEY_SubtitleExportFormat_2));
		export_format_2.addActionListener(_ComboBoxItemListener);
		panel_0_5.add(export_format_2);
	
		panel_0.add(panel_0_5);

		teletext.add(panel_0);


		JPanel panel_1 = new JPanel();
		panel_1.setLayout( new ColumnLayout() );
	//	panel_1.setBorder( BorderFactory.createTitledBorder(Resource.getString("SubtitlePanel.Title")) );
		panel_1.setBorder( BorderFactory.createTitledBorder(Resource.getString("SubtitlePanel.Title.Teletext")) );

	//	panel_1.add(new JLabel(Resource.getString("SubtitlePanel.Title.Teletext")));

		panel_1.add(box[8]);
		panel_1.add(box[9]);

		JPanel panel_1_2 = new JPanel();
		panel_1_2.setLayout(new BoxLayout(panel_1_2, BoxLayout.X_AXIS));

		JLabel font = new JLabel(Resource.getString("SubtitlePanel.Font"));
		font.setToolTipText(Resource.getString("SubtitlePanel.Font.Tip"));
		font.setPreferredSize(new Dimension(100, 22));
		font.setMaximumSize(new Dimension(100, 22));
		panel_1_2.add(font);

		JComboBox font_list = new JComboBox(Common.getFonts());
		font_list.setPreferredSize(new Dimension(160, 22));
		font_list.setMaximumSize(new Dimension(160, 22));
		font_list.setActionCommand(Keys.KEY_SubtitleFont[0]);
		font_list.setSelectedItem(Common.getSettings().getProperty(Keys.KEY_SubtitleFont));
		font_list.addActionListener(_ComboBoxItemListener);
		panel_1_2.add(font_list);

		panel_1.add(panel_1_2);

		JPanel panel_1_3 = new JPanel();
		panel_1_3.setLayout(new BoxLayout(panel_1_3, BoxLayout.X_AXIS));

		JLabel sup_label = new JLabel(Resource.getString("SubtitlePanel.SupValues"));
		sup_label.setPreferredSize(new Dimension(60, 22));
		sup_label.setMaximumSize(new Dimension(60, 22));
		panel_1_3.add(sup_label);

		JTextField subpicture_values = new JTextField(Common.getSettings().getProperty(Keys.KEY_SubtitlePanel_Format_SUP_Values));
		subpicture_values.setPreferredSize(new Dimension(200, 22));
		subpicture_values.setMaximumSize(new Dimension(200, 22));
		subpicture_values.setToolTipText(Resource.getString(Keys.KEY_SubtitlePanel_Format_SUP_Values[0] + Keys.KEY_Tip));
		subpicture_values.setEditable(true);
		subpicture_values.setActionCommand(Keys.KEY_SubtitlePanel_Format_SUP_Values[0]);
		subpicture_values.addActionListener(_TextFieldListener);
		subpicture_values.addKeyListener(_TextFieldKeyListener);

		panel_1_3.add(subpicture_values);

		panel_1.add(panel_1_3);

		panel_1.add(Box.createRigidArea(new Dimension(1, 15)));

		JLabel color_model = new JLabel(Resource.getString("SubtitlePanel.Colormodel"));
		color_model.setToolTipText(Resource.getString("SubtitlePanel.Colormodel.Tip"));
		panel_1.add(color_model);

		JComboBox color_table = new JComboBox(Common.getColorModels());
		color_table.setPreferredSize(new Dimension(130, 22));
		color_table.setMaximumSize(new Dimension(130, 22));
		color_table.setActionCommand(Keys.KEY_SubpictureColorModel[0]);
		color_table.setSelectedItem(Common.getSettings().getProperty(Keys.KEY_SubpictureColorModel));
		color_table.addActionListener(_ComboBoxItemListener);
		panel_1.add(color_table);

		JPanel panel_2_1 = new JPanel();
		panel_2_1.setLayout(new BoxLayout(panel_2_1, BoxLayout.X_AXIS));

		panel_2_1.add(new JLabel(Resource.getString("SubtitlePanel.PageId")));

		JTextField page_id = new JTextField(Common.getSettings().getProperty(Keys.KEY_SubtitlePanel_PageId_Value));
		page_id.setPreferredSize(new Dimension(40, 20));
		page_id.setMaximumSize(new Dimension(100, 20));
		page_id.setToolTipText(Resource.getString(Keys.KEY_SubtitlePanel_PageId_Value[0] + Keys.KEY_Tip));
		page_id.setEditable(true);
		page_id.setActionCommand(Keys.KEY_SubtitlePanel_PageId_Value[0]);
		page_id.addActionListener(_TextFieldListener);
		page_id.addKeyListener(_TextFieldKeyListener);

		panel_2_1.add(page_id);

		panel_1.add(panel_2_1);

		panel_1.add(Box.createRigidArea(new Dimension(1, 10)));
		panel_1.add(new JLabel(Resource.getString("SubtitlePanel.Title")));

		JPanel panel_2_2 = new JPanel();
		panel_2_2.setLayout(new BoxLayout(panel_2_2, BoxLayout.X_AXIS));
		panel_2_2.setToolTipText(Resource.getString("SubtitlePanel.ChangeDisplay.Tip"));

		JLabel label_2_2_1 = new JLabel(Resource.getString("SubtitlePanel.ChangeDisplay"));
		label_2_2_1.setPreferredSize(new Dimension(140, 22));
		label_2_2_1.setMaximumSize(new Dimension(140, 22));
		panel_2_2.add(label_2_2_1);

		JComboBox display_mode = new JComboBox(Keys.ITEMS_SubtitleChangeDisplay);
		display_mode.setPreferredSize(new Dimension(120, 22));
		display_mode.setMaximumSize(new Dimension(120, 22));
		display_mode.setActionCommand(Keys.KEY_SubtitleChangeDisplay[0]);
		display_mode.setSelectedIndex(Common.getSettings().getIntProperty(Keys.KEY_SubtitleChangeDisplay));
		display_mode.addActionListener(_ComboBoxIndexListener);
		panel_2_2.add(display_mode);
	
		panel_1.add(panel_2_2);

		JPanel panel_2_3 = new JPanel();
		panel_2_3.setLayout(new BoxLayout(panel_2_3, BoxLayout.X_AXIS));
		panel_2_3.setToolTipText(Resource.getString("SubtitlePanel.MovePosition.Tip"));

		JLabel label_2_3_1 = new JLabel(Resource.getString("SubtitlePanel.MovePosition"));
		label_2_3_1.setPreferredSize(new Dimension(140, 22));
		label_2_3_1.setMaximumSize(new Dimension(140, 22));
		panel_2_3.add(label_2_3_1);

		JTextField position_values = new JTextField(Common.getSettings().getProperty(Keys.KEY_SubtitleMovePosition_Value));
		position_values.setPreferredSize(new Dimension(120, 22));
		position_values.setMaximumSize(new Dimension(120, 22));
		position_values.setEditable(true);
		position_values.setActionCommand(Keys.KEY_SubtitleMovePosition_Value[0]);
		position_values.addActionListener(_TextFieldListener);
		position_values.addKeyListener(_TextFieldKeyListener);
		panel_2_3.add(position_values);
	
		panel_1.add(panel_2_3);

		panel_1.add(Box.createRigidArea(new Dimension(1, 10)));
		panel_1.add(box[10]);
		panel_1.add(box[11]);

		teletext.add(panel_1);

		return buildHeadPanel(teletext, Resource.getString("TabPanel.SubtitlePanel"));
	}

	/**
	 *
	 */
	protected JPanel buildOptionPanel()
	{
		JPanel option = new JPanel();
		option.setLayout( new GridLayout(1,2) );

		JPanel op0 = new JPanel();
		op0.setLayout( new ColumnLayout() );
		op0.setBorder( BorderFactory.createTitledBorder(Resource.getString("OptionPanel.Various.Title")) );

		String[][] objects = {
			Keys.KEY_dumpDroppedGop,
			Keys.KEY_additionalInputBuffer,
			Keys.KEY_SubtitlePanel_enableHDSub
		};

		JCheckBox[] box = new JCheckBox[objects.length];

		for (int i = 0; i < objects.length; i++)
		{
			box[i] = new JCheckBox(Resource.getString(objects[i][0]));
			box[i].setPreferredSize(new Dimension(270, 20));
			box[i].setMaximumSize(new Dimension(270, 20));
			box[i].setToolTipText(Resource.getString(objects[i][0] + Keys.KEY_Tip));
			box[i].setActionCommand(objects[i][0]);
			box[i].setSelected(Common.getSettings().getBooleanProperty(objects[i]));
			box[i].addActionListener(_CheckBoxListener);

			if (i < 1)
				op0.add(box[i]);
		}

		op0.add(Box.createRigidArea(new Dimension(1, 4)));

		JTextField start_path = new JTextField(Common.getSettings().getProperty(Keys.KEY_StartPath_Value));
		start_path.setPreferredSize(new Dimension(250, 25));
		start_path.setToolTipText(Resource.getString(Keys.KEY_StartPath_Value[0] + Keys.KEY_Tip));
		start_path.setEditable(true);
		start_path.setActionCommand(Keys.KEY_StartPath_Value[0]);
		start_path.addActionListener(_TextFieldListener);
		start_path.addKeyListener(_TextFieldKeyListener);

		op0.add(new JLabel(Resource.getString("OptionPanel.StartPath")));
		op0.add(start_path);

		op0.add(Box.createRigidArea(new Dimension(1, 4)));

		JTextField keyset = new JTextField();
		keyset.setPreferredSize(new Dimension(250, 25));
		keyset.setToolTipText("type any key and its value, hit enter");
		keyset.setEditable(true);
		keyset.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				String actName = e.getActionCommand();
				JTextField textfield = (JTextField) e.getSource();

				String str = textfield.getText();
				int index = str.indexOf("=");

				if (index < 0) //show key and value
					textfield.setText(str + "=" + Common.getSettings().getProperty(str));

				else if (str.length() <= index + 1) // remove key
				{
					Common.getSettings().remove(str.substring(0, index));
					textfield.setText("");
				}

				else //set value of key
					Common.getSettings().setProperty(str.substring(0, index), str.substring(index + 1));
			}
		});

		op0.add(new JLabel("settings key/value editor"));
		op0.add(keyset);

		option.add(op0);

		JPanel op2 = new JPanel();
		op2.setLayout( new ColumnLayout() );
		op2.setBorder( BorderFactory.createTitledBorder(Resource.getString("OptionPanel.Buffer.Title")) );

		op2.add(box[1]);

		String[][] keys = {
			Keys.KEY_MainBuffer,
			Keys.KEY_ScanBuffer
		};

		Object[][] buffersizes = {
			{ "10240000", "8192000", "7168000", "6144000", "5120000", "4096000", "3072000", "2048000", "1024000" },
			{ "384000", "512000", "1024000", "1536000", "2048000", "2560000", "3072000" },
		};

		for (int i = 0; i < keys.length; i++)
		{
			JComboBox combobox = new JComboBox(buffersizes[i]);
			combobox.setPreferredSize(new Dimension(100, 24));
			combobox.setMaximumSize(new Dimension(100, 24));
			combobox.setEditable(true);
			combobox.setMaximumRowCount(6);
			combobox.setActionCommand(keys[i][0]);
			combobox.setSelectedItem(Common.getSettings().getProperty(keys[i]));
			combobox.addActionListener(_ComboBoxItemListener);

			JLabel label = new JLabel(Resource.getString(keys[i][0]));
			label.setToolTipText(Resource.getString(keys[i][0] + Keys.KEY_Tip));

			op2.add(label);
			op2.add(combobox);
		}

		op2.add(Box.createRigidArea(new Dimension(1, 6)));

		op2.add(box[2]);
		op2.add(new JLabel("takes more JavaVM memory!"));
		op2.add(new JLabel("requires a restart of ProjectX"));

		option.add(op2);

		return buildHeadPanel(option, Resource.getString("TabPanel.OptionPanel"));
	}

	/**
	 *
	 */
	protected JPanel buildNetPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout( new GridLayout(1, 2) );

		JPanel panel_1 = new JPanel();
		panel_1.setLayout( new ColumnLayout() );
		panel_1.setBorder( BorderFactory.createTitledBorder(Resource.getString("FtpPanel.Title")) );

		/**
		 *
		 */
		String[][] objects = {
			Keys.KEY_killFtpClient,
			Keys.KEY_useFtpServerResume,
			Keys.KEY_autostartWebServer
		};

		JCheckBox[] box = new JCheckBox[objects.length];

		for (int i = 0; i < 2; i++)
		{
			box[i] = new JCheckBox(Resource.getString(objects[i][0]));
			box[i].setPreferredSize(new Dimension(270, 20));
			box[i].setMaximumSize(new Dimension(270, 20));
			box[i].setToolTipText(Resource.getString(objects[i][0] + Keys.KEY_Tip));
			box[i].setActionCommand(objects[i][0]);
			box[i].setSelected(Common.getSettings().getBooleanProperty(objects[i]));
			box[i].addActionListener(_CheckBoxListener);

			panel_1.add(box[i]);
		}

		panel_1.add(Box.createRigidArea(new Dimension(1, 5)));

		/**
		 *
		 */
		JPanel ftpPanel = new JPanel();
		ftpPanel.setLayout(new BoxLayout(ftpPanel, BoxLayout.X_AXIS));

		JLabel label_1 = new JLabel (Resource.getString("ftp.command.label"));
		label_1.setPreferredSize(new Dimension(100, 20));
		label_1.setMaximumSize(new Dimension(100, 20));
		label_1.setToolTipText(Resource.getString("ftp.command.tip"));
		ftpPanel.add(label_1);

		JTextField ftpcommand = new JTextField(Common.getSettings().getProperty(Keys.KEY_FtpServer_Commands));
		ftpcommand.setPreferredSize(new Dimension(160, 20));
		ftpcommand.setMaximumSize(new Dimension(160, 20));
		ftpcommand.setToolTipText(Resource.getString(Keys.KEY_FtpServer_Commands[0] + Keys.KEY_Tip));
		ftpcommand.setActionCommand(Keys.KEY_FtpServer_Commands[0]);
		ftpcommand.addActionListener(_TextFieldListener);
		ftpcommand.addKeyListener(_TextFieldKeyListener);
		ftpPanel.add(ftpcommand);

		panel_1.add(ftpPanel);

		panel.add(panel_1);


		JPanel panel_2 = new JPanel();
		panel_2.setLayout( new ColumnLayout() );
		panel_2.setBorder( BorderFactory.createTitledBorder(Resource.getString("NetPanel.Title")) );

		/**
		 *
		 */
		for (int i = 2; i < objects.length; i++)
		{
			box[i] = new JCheckBox(Resource.getString(objects[i][0]));
			box[i].setPreferredSize(new Dimension(270, 20));
			box[i].setMaximumSize(new Dimension(270, 20));
			box[i].setToolTipText(Resource.getString(objects[i][0] + Keys.KEY_Tip));
			box[i].setActionCommand(objects[i][0]);
			box[i].setSelected(Common.getSettings().getBooleanProperty(objects[i]));
			box[i].addActionListener(_CheckBoxListener);

			panel_2.add(box[i]);
		}

		panel_2.add(Box.createRigidArea(new Dimension(1, 5)));

		/**
		 *
		 */
		JPanel netPanel = new JPanel();
		netPanel.setLayout(new BoxLayout(netPanel, BoxLayout.X_AXIS));

		JLabel label_2 = new JLabel ("Port:");
		label_2.setPreferredSize(new Dimension(100, 20));
		label_2.setMaximumSize(new Dimension(100, 20));
		netPanel.add(label_2);

		JTextField port = new JTextField(Common.getSettings().getProperty(Keys.KEY_WebServerPort));
		port.setPreferredSize(new Dimension(160, 20));
		port.setMaximumSize(new Dimension(160, 20));
		port.setToolTipText(Resource.getString(Keys.KEY_WebServerPort[0] + Keys.KEY_Tip));
		port.setActionCommand(Keys.KEY_WebServerPort[0]);
		port.addActionListener(_TextFieldListener);
		port.addKeyListener(_TextFieldKeyListener);
		netPanel.add(port);

		panel_2.add(netPanel);

		/**
		 *
		 */
		JPanel netPanel2 = new JPanel();
		netPanel2.setLayout(new BoxLayout(netPanel2, BoxLayout.X_AXIS));

		JLabel label_3 = new JLabel("Keyword:");
		label_3.setPreferredSize(new Dimension(100, 20));
		label_3.setMaximumSize(new Dimension(100, 20));
		label_3.setToolTipText("mustn't be empty");
		netPanel2.add(label_3);

		JTextField access = new JTextField(Common.getSettings().getProperty(Keys.KEY_WebServerAccess));
		access.setPreferredSize(new Dimension(160, 20));
		access.setMaximumSize(new Dimension(160, 20));
		access.setToolTipText(Resource.getString(Keys.KEY_WebServerAccess[0] + Keys.KEY_Tip));
		access.setActionCommand(Keys.KEY_WebServerAccess[0]);
		access.addActionListener(_TextFieldListener);
		access.addKeyListener(_TextFieldKeyListener);
		netPanel2.add(access);

		panel_2.add(netPanel2);

		JButton button_1 = new JButton("re-/start WebIF");
		button_1.setPreferredSize(new Dimension(120, 22));
		button_1.setMaximumSize(new Dimension(120, 22));
		button_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				Common.startWebServer();
			}
		});

		JButton button_2 = new JButton("stop WebIF");
		button_2.setPreferredSize(new Dimension(120, 22));
		button_2.setMaximumSize(new Dimension(120, 22));
		button_2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				Common.stopWebServer();
			}
		});

		panel_2.add(button_1);
		panel_2.add(button_2);

		panel.add(panel_2);


		return buildHeadPanel(panel, Resource.getString("TabPanel.NetPanel"));
	}

	/**
	 *
	 */
	protected JPanel buildPostCommandsPanel()
	{
		ActionListener _ExecuteListener = new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				try {
					String actName = e.getActionCommand();
					String str = "";

					if (actName.equals(Keys.KEY_PostCommands_Cmd1[0])) 
						str = Common.getSettings().getProperty(Keys.KEY_PostCommands_Cmd1);

					else if (actName.equals(Keys.KEY_PostCommands_Cmd2[0])) 
						str = Common.getSettings().getProperty(Keys.KEY_PostCommands_Cmd2);

					else if (actName.equals(Keys.KEY_PostCommands_Cmd3[0])) 
						str = Common.getSettings().getProperty(Keys.KEY_PostCommands_Cmd3);

					Common.performCommand(str);

				} catch (Exception ex) { 

					Common.setExceptionMessage(ex); 
				}
			}
		};

		JPanel container = new JPanel();
		container.setLayout( new ColumnLayout() );
		container.setBorder( BorderFactory.createTitledBorder(Resource.getString("PostCommands.Title")));

		String[][] objects = {
			Keys.KEY_PostCommands_Cmd1,
			Keys.KEY_PostCommands_Cmd2,
			Keys.KEY_PostCommands_Cmd3,
			Keys.KEY_PostCommands_Cmd4,
			Keys.KEY_PostCommands_Cmd5,
			Keys.KEY_PostCommands_Cmd6,
			Keys.KEY_PostCommands_Cmd7,
			Keys.KEY_PostCommands_Cmd8
		};

		for (int i = 0; i < 3; i++)
		{
			JTextField text_field = new JTextField(Common.getSettings().getProperty(objects[i]));
			text_field.setPreferredSize(new Dimension(400, 25));
			text_field.setEditable(true);
			text_field.setActionCommand(objects[i][0]);
			text_field.addActionListener(_TextFieldListener);
			text_field.addKeyListener(_TextFieldKeyListener);

			JButton exe = new JButton(Resource.getString("PostCommands.Execute"));
			exe.setActionCommand(objects[i][0]);
			exe.setPreferredSize(new Dimension(100, 20));
			exe.addActionListener(_ExecuteListener);

			JPanel panel = new JPanel();
			panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
			panel.add(text_field); 
			panel.add(exe);  

			container.add(panel);
		}

		container.add(Box.createRigidArea(new Dimension(1, 10)));

		JLabel label = new JLabel(Resource.getString("PostCommands.PostProcessing"));
		label.setToolTipText(Resource.getString("PostCommands.PostProcessing.Tip"));

		container.add(label);


		for (int i = 3; i < objects.length; i++)
		{
			JTextField text_field = new JTextField(Common.getSettings().getProperty(objects[i]));
			text_field.setPreferredSize(new Dimension(400, 25));
			text_field.setEditable(true);
			text_field.setActionCommand(objects[i][0]);
			text_field.addActionListener(_TextFieldListener);
			text_field.addKeyListener(_TextFieldKeyListener);

			JPanel panel = new JPanel();
			panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
			panel.add(text_field); 
			panel.add(new JLabel(Keys.ITEMS_ConversionMode[i - 3].toString()));  

			container.add(panel);
		}

		JCheckBox box = new JCheckBox(Resource.getString(Keys.KEY_PostProcessCompletion[0]));
		box.setToolTipText(Resource.getString(Keys.KEY_PostProcessCompletion[0] + Keys.KEY_Tip));
		box.setActionCommand(Keys.KEY_PostProcessCompletion[0]);
		box.setSelected(Common.getSettings().getBooleanProperty(Keys.KEY_PostProcessCompletion));
		box.addActionListener(_CheckBoxListener);

		container.add(box);


		return buildHeadPanel(container, Resource.getString("TabPanel.PostCommandsPanel"));
	}

}
