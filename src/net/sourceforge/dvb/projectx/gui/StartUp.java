/*
 * @(#)StartUp.java - mini info, holf a place for start infos
 *
 * Copyright (c) 2004-2005 by dvb.matt, All Rights Reserved. 
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

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import net.sourceforge.dvb.projectx.common.Resource;
import net.sourceforge.dvb.projectx.common.X;


//DM17022004 081.6 int17 introduced, int18 changed
public class StartUp extends JFrame 
{
	/** Background Color */
	private static final Color BACKGROUND_COLOR = new Color(224,224,224,224);

	boolean agreement = false;
	JRadioButton disagree;
	JRadioButton agree;
	Listener listener = new Listener();

	class Listener implements ActionListener
	{
		public void actionPerformed(ActionEvent e)
		{
			String actName = e.getActionCommand();

			if (actName.equals("agree")) 
			{
				setVisible(false);
				X.setButton(1, true);
				X.setVisible0(true);
			}

			else if (actName.equals("disagree")) 
			{
				System.exit(0);
			}
		}
	}

	public StartUp()
	{
		open(Resource.getString("startup.title"));
	}

	public StartUp(String title)
	{
		open(title);
	}

	protected void open(String title)
	{
		setTitle(title);

		JPanel container = new JPanel();
		container.setBackground(BACKGROUND_COLOR);
		container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
		container.setBorder( BorderFactory.createEmptyBorder(10,10,10,10));

		String terms[] = Resource.getStringByLines("terms");

		for (int a=0; a<terms.length; a++) 
			container.add(new JLabel(terms[a]));

		disagree = new JRadioButton(Resource.getString("terms.disagree"));
		disagree.setActionCommand("disagree");
		disagree.setOpaque(false);
		agree = new JRadioButton(Resource.getString("terms.agree"));
		agree.setActionCommand("agree");
		agree.setOpaque(false);
		agree.setEnabled(false); //DM16042004 081.7 int01 add

		ButtonGroup BrGroup = new ButtonGroup();
		BrGroup.add(disagree);
		BrGroup.add(agree);

		container.add(disagree);
		container.add(agree);
		disagree.addActionListener(listener);
		agree.addActionListener(listener);

		JPanel container2 = new JPanel();
		container2.setBackground(BACKGROUND_COLOR);
		container2.setBorder( BorderFactory.createRaisedBevelBorder());
		container2.add(container);

		getContentPane().add(container2);

		if (!X.CLI_mode)
			pack();

		setLocation(200,200);
		setResizable(false); //DM17042004 081.7 int02 add

		addWindowListener (new WindowAdapter() { 
			public void windowClosing(WindowEvent e) { 
				System.exit(0); 
			}
		});

		return;
	}

	public void set( boolean agreement)
	{
		//DM16042004 081.7 int01 add
		agree.setEnabled(true);

		this.agreement = agreement;
		agree.setSelected(agreement);
		if (agreement)
			agree.setForeground(Color.green);
	}

	public boolean get()
	{
		return agree.isSelected();
	}

	public void close()
	{
		dispose();
	}
}

