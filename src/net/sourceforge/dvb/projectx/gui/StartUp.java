/*
 * @(#)StartUp.java - mini info, hold a place for start infos
 *
 * Copyright (c) 2004-2005 by dvb.matt, All Rights Reserved. 
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

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.Dimension;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JProgressBar;
import javax.swing.Box;

import net.sourceforge.dvb.projectx.common.Resource;
import net.sourceforge.dvb.projectx.common.Common;
import net.sourceforge.dvb.projectx.common.Keys;

import net.sourceforge.dvb.projectx.gui.MainFrame;
import net.sourceforge.dvb.projectx.gui.ColumnLayout;
import net.sourceforge.dvb.projectx.gui.CommonGui;

/**
 *
 */
public class StartUp extends JFrame {

	/** Background Color */
	private final Color BACKGROUND_COLOR = new Color(200, 200, 200);

	private boolean agreement = false;

	private JButton agree;
	private JProgressBar progressBar;
	private JLabel progress;
	private JLabel message;

	/**
	 *
	 */
	public StartUp()
	{
		open(Resource.getString("StartUp.Title"));
	}

	/**
	 *
	 */
	public StartUp(String title)
	{
		open(title);
	}

	/**
	 *
	 */
	protected void open(String title)
	{
		setTitle(title);

		JPanel container = new JPanel();
		container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
		container.setBackground(BACKGROUND_COLOR);

		container.add(buildUpperPanel());
		container.add(buildHLinePanel());
		container.add(buildLoadPanel());
		container.add(buildHLinePanel());
		container.add(Box.createRigidArea(new Dimension(1, 10)));
		container.add(buildButtonPanel());
		container.add(Box.createRigidArea(new Dimension(1, 10)));

		JPanel container2 = new JPanel();
		container2.setBorder(BorderFactory.createRaisedBevelBorder());
		container2.setBackground(BACKGROUND_COLOR);
		container2.add(container);

		getContentPane().add(container2);
		pack();

		setLocation(200, 200);
		setResizable(false);

		addWindowListener (new WindowAdapter() { 
			public void windowClosing(WindowEvent e) { 
				Common.exitApplication(0);
			}
		});


		return;
	}

	/**
	 *
	 */
	protected JPanel buildUpperPanel()
	{
		JPanel panel = new JPanel();
		panel.setBackground(new Color(224, 224, 224));
		panel.setLayout(new ColumnLayout());

		panel.add(Box.createRigidArea(new Dimension(1, 20)));
		panel.add(new JLabel("  " + Resource.getString("StartUp.Init")));
		panel.add(Box.createRigidArea(new Dimension(1, 10)));
		panel.add(message = new JLabel("  " + Resource.getString("StartUp.Wait")));
		panel.add(Box.createRigidArea(new Dimension(1, 20)));


		JPanel panel_1 = new JPanel();
		panel_1.setBackground(new Color(224, 224, 224));
		panel_1.setLayout(new BorderLayout());

		panel_1.add(new JLabel(CommonGui.loadIcon("px.gif")), BorderLayout.EAST);
		panel_1.add(panel, BorderLayout.WEST);

		return panel_1;
	}

	/**
	 *
	 */
	protected JPanel buildHLinePanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new ColumnLayout());
		panel.setBorder(BorderFactory.createEtchedBorder());

		return panel;
	}

	/**
	 *
	 */
	protected JPanel buildLoadPanel()
	{
		JPanel panel = new JPanel();
		panel.setBackground(BACKGROUND_COLOR);
		panel.setLayout(new ColumnLayout());

		panel.add(Box.createRigidArea(new Dimension(1, 10)));

		progress = new JLabel("Start...");
		panel.add(progress);

		progressBar = new JProgressBar();
		progressBar.setStringPainted(true);
		progressBar.setValue(0);
		progressBar.setPreferredSize(new Dimension(500, 24));
		progressBar.setForeground(new Color(20, 240, 20));

		panel.add(Box.createRigidArea(new Dimension(1, 5)));

		panel.add(progressBar);

		panel.add(Box.createRigidArea(new Dimension(1, 20)));

		String terms[] = Resource.getStringByLines("terms");

		for (int i = 0; i < terms.length; i++) 
			panel.add(new JLabel(terms[i]));

		panel.add(Box.createRigidArea(new Dimension(1, 10)));

		return panel;
	}

	/**
	 *
	 */
	protected JPanel buildButtonPanel()
	{
		JButton cancel = new JButton();
		CommonGui.localize(cancel, "Common.Cancel");
		cancel.setActionCommand("disagree");
		cancel.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				Common.exitApplication(0);
			}
		});

		JButton disagree = new JButton(Resource.getString("terms.disagree"));
		disagree.setActionCommand("disagree");
		disagree.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				Common.exitApplication(0);
			}
		});

		agree = new JButton(Resource.getString("terms.agree"));
		agree.setActionCommand("agree");
		agree.setEnabled(false);
		agree.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				if (!agree.isEnabled())
					return;

				setVisible(false);
				Common.getSettings().setProperty(Keys.KEY_Agreement[0], "1");
				MainFrame.setVisible0(true);
			}
		});

		JPanel panel = new JPanel();
		panel.setBackground(BACKGROUND_COLOR);
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

		panel.add(agree);
		panel.add(Box.createRigidArea(new Dimension(10, 1)));
		panel.add(disagree);


		JPanel panel_1 = new JPanel();
		panel_1.setBackground(BACKGROUND_COLOR);
		panel_1.setLayout(new BorderLayout());

		panel_1.add(panel, BorderLayout.WEST);
		panel_1.add(cancel, BorderLayout.EAST);

		return panel_1;
	}

	/**
	 *
	 */
	public void set(boolean _agreement)
	{
		agree.setEnabled(!_agreement);

		agreement = _agreement;
		agree.setSelected(agreement);

		if (agreement)
			agree.setForeground(Color.green);

		else
			message.setText("  " + Resource.getString("StartUp.Choose"));
	}

	/**
	 *
	 */
	public boolean get()
	{
		return agree.isSelected();
	}

	/**
	 *
	 */
	public void setProgress(int value, String str)
	{
		progress.setText(str);
		progressBar.setStringPainted(true);
		progressBar.setValue(value);

		System.out.println(str);
	}

	/**
	 *
	 */
	public void close()
	{
		dispose();
	}
}

