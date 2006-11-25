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
import javax.swing.JSlider;

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

	private String title = Resource.getString("General.FileProperties");

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
	//	logtab.addTab( Resource.getString("TabPanel.SpecialPanel"), buildSpecialPanel());

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
		panel.setLayout( new GridLayout(2, 2) );

		JPanel op1 = new JPanel();
		op1.setLayout( new ColumnLayout() );
		op1.setBorder( BorderFactory.createTitledBorder("Preview") );

		JSlider slider = new JSlider();
		slider.setMaximum(100);
		slider.setValue(0);
		op1.add(slider);

		panel.add(op1);

		return buildHeadPanel(panel, "Main");
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
