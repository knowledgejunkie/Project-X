/*
 * @(#)PatchPanel.java - patching video basics
 *
 * Copyright (c) 2001-2005 by dvb.matt, All rights reserved.
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

package net.sourceforge.dvb.projectx.video;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;

import net.sourceforge.dvb.projectx.common.Resource;
import net.sourceforge.dvb.projectx.common.X;
import net.sourceforge.dvb.projectx.gui.UISwitchListener;
import net.sourceforge.dvb.projectx.xinput.XInputFile;

/**
 * Class Patch Panel.
 */
public class PatchPanel extends JDialog {

	/** patchfield */
	private JTextField[] patchfield = new JTextField[3];
	
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
	public PatchPanel(JFrame f) {
		super(f, Resource.getString("patch.title"), true);

		JPanel container = new JPanel();
		container.setLayout( new BorderLayout() );

		JPanel grid = new JPanel();
		grid.setLayout(new BoxLayout(grid, BoxLayout.X_AXIS));

		for (int a=0;a<3;a++) {
			patchfield[a] = new JTextField("");
			patchfield[a].setPreferredSize(new Dimension(65,22));
			patchfield[a].setMaximumSize(new Dimension(65,22));
			grid.add(new JLabel(notes[a]));
			grid.add(patchfield[a]);
		}
		grid.add(new JLabel(notes[3]));

		JButton changebutton = new JButton();
		Resource.localize(changebutton, "patch.change");
		changebutton.setActionCommand("change");
		changebutton.addActionListener(patchAction);
		grid.add(changebutton);

		JButton cancelbutton = new JButton();
		Resource.localize(cancelbutton, "patch.cancel");
		cancelbutton.setActionCommand("cancel");
		cancelbutton.addActionListener(patchAction);
		grid.add(cancelbutton);

		getRootPane().setDefaultButton(cancelbutton);

		container.add(grid);
		getContentPane().add(container);

		if (!X.CLI_mode)
			pack();

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
	private boolean search(XInputFile aXInputFile, byte[] os) {
		try 
		{
		long size = aXInputFile.length();
			byte[] ps = new byte[((size<650000)?(int)size:650000)];
			this.os = os;
			aXInputFile.randomAccessSingleRead(ps, 0);
			for (int a=0;a<ps.length-15;a++) {
				if (ps[a]!=0 || ps[a+1]!=0 || ps[a+2]!=1 || ps[a+3]!=(byte)0xB3 || 
				ps[a+4]!=os[4] || ps[a+5]!=os[5] || ps[a+6]!=os[6] || ps[a+7]!=os[7] ) continue;
				ins=a;
				patchfield[0].setText(""+((255&ps[a+4])<<4 | (240&ps[a+5])>>>4));
				patchfield[1].setText(""+((15&ps[a+5])<<8 | (255&ps[a+6])));
				patchfield[2].setText(""+(((255&ps[a+8])<<10 | (255&ps[a+9])<<2 | (192 & ps[a+10])>>>6)*400));
				return true;
			}
		} 
		catch (IOException e) { X.Msg(Resource.getString("patch.error") + " " + e); }
		return false;
	}

	/**
	 * @param file1
	 * @param os
	 */
	public void entry(XInputFile aXInputFile, byte[] os) {
		xInputFile = aXInputFile;
		if (search(xInputFile, os))
			this.show();
	}

	/**
	 * Centers this dialog on the users screen.
	 */
	protected void centerDialog() {
		Dimension screenSize = this.getToolkit().getScreenSize();
		Dimension size = this.getSize();
		screenSize.height = screenSize.height/2;
		screenSize.width = screenSize.width/2;
		size.height = size.height/2;
		size.width = size.width/2;
		int y = screenSize.height - size.height;
		int x = screenSize.width - size.width;
		this.setLocation(x,y);
	}

 	/**
 	 * Callback for the cancel event.
 	 */
 	private void cancel() {
		this.setVisible(false);
	}

 	/**
 	 * Callback for the change event.
 	 */
	private void change() {
		try 
		{
			int hsize=Integer.parseInt(patchfield[0].getText());
			int vsize=Integer.parseInt(patchfield[1].getText());
			int brate=Integer.parseInt(patchfield[2].getText())/400;
			os[4] = (byte)(0xFF&hsize>>>4);
			os[5] = (byte)((0xF0&hsize<<4) | (0xF&vsize>>>8));
			os[6] = (byte)(0xFF&vsize);
			os[8] = (byte)(0xFF&brate>>>10);
			os[9] = (byte)(0xFF&brate>>>2);
			os[10]= (byte)((0x3F&os[10]) | (0xC0&brate<<6));
			dochange();
		} 
		catch (NumberFormatException e) {  }
		catch (NullPointerException e) {  }
	}

	/**
	 * Does the actual change to the file.
	 */
	private void dochange() {
		try 
		{
			xInputFile.randomAccessOpen("rw");
			xInputFile.randomAccessSeek(ins);
			xInputFile.randomAccessWrite(os);
			xInputFile.randomAccessClose();
		} 
		catch (IOException e) { X.Msg(Resource.getString("patch.error2") + " " + e); }
		this.setVisible(false);
	}

}
