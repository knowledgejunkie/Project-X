/*
 * @(#)PatchDialog.java - patching video basics
 *
 * Copyright (c) 2001-2005 by dvb.matt, All rights reserved.
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JComboBox;
import javax.swing.UIManager;

import net.sourceforge.dvb.projectx.common.Resource;
import net.sourceforge.dvb.projectx.common.Common;
import net.sourceforge.dvb.projectx.common.Keys;

import net.sourceforge.dvb.projectx.gui.UISwitchListener;
import net.sourceforge.dvb.projectx.gui.CommonGui;

import net.sourceforge.dvb.projectx.xinput.XInputFile;

/**
 * Class Patch Panel.
 */
public class PatchDialog extends JDialog {

	/** patchfield */
	private JTextField[] patchfield = new JTextField[3];
	
	private JComboBox aspectbox;

	/** the file to be patched */
	XInputFile xInputFile=null;
	
	/** the labels */
	private String[] notes = { " H:"," V:"," BR:","bps " };
	
	private long ins=0;
	private byte[] os = new byte[1];
	
	/** instance of the PatchListener */
	private PatchListener patchAction = new PatchListener();

	/**
	 * ActionListener for the PatchPanel. 
	 */
	private class PatchListener implements ActionListener {
		
		/* (non-Javadoc)
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
		public void actionPerformed(ActionEvent e) {
			String actName = e.getActionCommand();

			if (actName.equals("change"))
				change();

			else if (actName.equals("cancel"))
				cancel();
		}
	}

	/**
	 * Constructor of the PatchPanel.
	 * 
	 * @param f The parent Frame
	 */
	public PatchDialog(JFrame f)
	{
		super(f, Resource.getString("PatchPanel.Title"), true);

		JPanel container = new JPanel();
		container.setLayout( new BorderLayout() );

		JPanel grid = new JPanel();
		grid.setLayout(new BoxLayout(grid, BoxLayout.X_AXIS));

		for (int a = 0; a < patchfield.length; a++)
		{
			patchfield[a] = new JTextField("");
			patchfield[a].setPreferredSize(new Dimension(65,22));
			patchfield[a].setMaximumSize(new Dimension(65,22));

			grid.add(Box.createRigidArea(new Dimension(5, 1)));

			grid.add(new JLabel(notes[a]));
			grid.add(patchfield[a]);
		}

		grid.add(new JLabel(notes[3]));

		grid.add(Box.createRigidArea(new Dimension(5, 1)));

		aspectbox = new JComboBox(Keys.ITEMS_ChangeAspectRatio);
		aspectbox.setPreferredSize(new Dimension(120,22));
		aspectbox.setMaximumSize(new Dimension(120,22));
		aspectbox.addActionListener(patchAction);
		grid.add(aspectbox);

		grid.add(Box.createRigidArea(new Dimension(5, 1)));

		JButton changebutton = new JButton();
		CommonGui.localize(changebutton, "PatchPanel.Change");
		changebutton.setActionCommand("change");
		changebutton.addActionListener(patchAction);
		grid.add(changebutton);

		grid.add(Box.createRigidArea(new Dimension(5, 1)));

		JButton cancelbutton = new JButton();
		CommonGui.localize(cancelbutton, "PatchPanel.Cancel");
		cancelbutton.setActionCommand("cancel");
		cancelbutton.addActionListener(patchAction);
		grid.add(cancelbutton);

		getRootPane().setDefaultButton(cancelbutton);

		container.add(grid);
		getContentPane().add(container);

		//pack();
		setSize(600, 150);
		centerDialog();

		UIManager.addPropertyChangeListener(new UISwitchListener(container));
	}

	/**
	 * Search the given file for video basics.
	 * 
	 * @param file
	 * @param os
	 * @return
	 */
	private boolean search(XInputFile aXInputFile, byte[] os)
	{
		try {
			long size = aXInputFile.length();

			byte[] ps = new byte[((size<650000)?(int)size:650000)];

			aXInputFile.randomAccessSingleRead(ps, 0);

			for (int a = 0; a < ps.length - 15; a++)
			{
				if (ps[a] != 0 || ps[a + 1] != 0 || ps[a + 2] != 1 || ps[a + 3] != (byte)0xB3 || 
				ps[a + 4] != os[4] || ps[a + 5] != os[5] || ps[a + 6] != os[6] || ps[a + 7] != os[7] )
					continue;

				ins = a;
				patchfield[0].setText("" + ((255 & ps[a + 4])<<4 | (240 & ps[a + 5])>>>4));
				patchfield[1].setText("" + ((15 & ps[a + 5])<<8 | (255 & ps[a + 6])));
				patchfield[2].setText("" + (((255 & ps[a + 8])<<10 | (255 & ps[a + 9])<<2 | (192 & ps[a + 10])>>>6) * 400));

				aspectbox.setSelectedIndex((0xFF & ps[a + 7])>>4);

				return true;
			}

		} catch (IOException e) { 

			Common.setExceptionMessage(e);
		}

		return false;
	}

	/**
	 * @param file1
	 * @param os
	 */
	public boolean entry(XInputFile aXInputFile)
	{
		xInputFile = aXInputFile;

		os = xInputFile.getStreamInfo().getVideoHeader();

		if (os == null)
			return false;

		if (search(xInputFile, os))
			this.show();

		return true;
	}

	/**
	 * Centers this dialog on the users screen.
	 */
	protected void centerDialog() {
		Dimension screenSize = this.getToolkit().getScreenSize();
		Dimension size = this.getSize();
		screenSize.height = screenSize.height / 2;
		screenSize.width = screenSize.width / 2;
		size.height = size.height / 2;
		size.width = size.width / 2;
		int y = screenSize.height - size.height;
		int x = screenSize.width - size.width;
		this.setLocation(x,y);
	}

 	/**
 	 * Callback for the cancel event.
 	 */
 	private void cancel()
	{
		this.setVisible(false);
	}

 	/**
 	 * Callback for the change event.
 	 */
	private void change()
	{
		try {
			int hsize=Integer.parseInt(patchfield[0].getText());
			int vsize=Integer.parseInt(patchfield[1].getText());
			int brate=Integer.parseInt(patchfield[2].getText()) / 400;
			int aspectindex = aspectbox.getSelectedIndex();

			os[4] = (byte)(0xFF & hsize>>>4);
			os[5] = (byte)((0xF0 & hsize<<4) | (0xF & vsize>>>8));
			os[6] = (byte)(0xFF & vsize);
			os[8] = (byte)(0xFF & brate>>>10);
			os[9] = (byte)(0xFF & brate>>>2);
			os[10]= (byte)((0x3F & os[10]) | (0xC0 & brate<<6));

			if (aspectindex >= 0)
				os[7] = (byte)((0xF & os[7]) | (aspectindex<<4));

			dochange();

		} 
		catch (NumberFormatException e) {}
		catch (NullPointerException e) {}
	}

	/**
	 * applies the actual change to the file.
	 */
	private void dochange()
	{
		try {
			xInputFile.randomAccessOpen("rw");
			xInputFile.randomAccessSeek(ins);
			xInputFile.randomAccessWrite(os);
			xInputFile.randomAccessClose();

		} catch (IOException e) {
			Common.setExceptionMessage(e);
		}

		this.setVisible(false);
	}

}
