/*
 * @(#)AboutBox.java - about box of Project-X, with terms of condition and credits
 *
 * Copyright (c) 2004-2005 by pstorch, All Rights Reserved. 
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
import java.awt.Color;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JViewport;

import net.sourceforge.dvb.projectx.common.Resource;
import net.sourceforge.dvb.projectx.common.Common;

import net.sourceforge.dvb.projectx.gui.CommonGui;


/**
 * AboutBox for Project-X GUI.
 * 
 * @author Peter Storch
 */
public class AboutBox extends JDialog {

	/** Background Color */
	private static final Color BACKGROUND_COLOR = new Color(224,224,224);
	
	/**
	 * Constructor of AboutBox.
	 * 
	 * @param frame
	 */
	public AboutBox(Frame frame)
	{
		super(frame, true);
		setTitle(Resource.getString("about.title"));

		JPanel container = new JPanel();
		container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
		container.setBorder( BorderFactory.createEmptyBorder(10,10,10,10));
		container.setBackground(BACKGROUND_COLOR);
		
		JLabel logo = new JLabel(CommonGui.loadIcon("px.gif"));
		logo.setOpaque(true);
		logo.setBackground(BACKGROUND_COLOR);

		container.add(new JLabel(Resource.getString("about.credits.label")));

		String credits = "\n" + Resource.getString("credits") + "\n";
		JTextArea list = new JTextArea(credits, 5, 10);
		list.setEnabled(false);
		list.setDisabledTextColor(Color.black);
		JScrollPane scroll = new JScrollPane(list);
		scroll.setBackground(Color.white);
		scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
		container.add(scroll);
		
		final CreditsScroller creditScroller = new CreditsScroller(scroll);
		creditScroller.start();

		container.add(new JLabel(" ")); // as spacer

		String terms[] = Resource.getStringByLines("terms");

		for (int a=0; a<terms.length; a++) 
			container.add(new JLabel(terms[a]));

		JButton ok = new JButton(Resource.getString("about.ok"));
		ok.setBackground(BACKGROUND_COLOR);
		ok.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent arg0) {
				creditScroller.stopIt();
				dispose(); 
			}
		});

		JPanel container2 = new JPanel(new BorderLayout());
		//container2.setBorder( BorderFactory.createRaisedBevelBorder());
		container2.setBackground(BACKGROUND_COLOR);
		container2.setOpaque(true);
		container2.add(logo, BorderLayout.NORTH);
		container2.add(container, BorderLayout.CENTER);
		container2.add(ok, BorderLayout.SOUTH);

		getContentPane().add(container2);
		pack();

		setLocation(200,200);
		setResizable(false); 

		addWindowListener (new WindowAdapter() { 
			public void windowClosing(WindowEvent e) { 
				creditScroller.stopIt();
				dispose(); 
			}
		});

		setVisible(true);
	}
	
	/**
	 * A Thread which scrolls a JScrollPane from top to bottom and vice versa
	 * until it is stopped by calling stopIt().
	 */
	private class CreditsScroller extends Thread
	{
		/** direction up? */
		private boolean up = true;
		
		/** should this Thread be stopped? */
		private boolean stopIt = false;
		
		/** the JScrollPane to scroll */
		private JScrollPane scroll;
		
		/**
		 * Constructor of CreditsScroller.
		 * 
		 * @param scroll The JScrollPane of the credits JTextArea
		 */
		public CreditsScroller(JScrollPane scroll)
		{
			this.scroll = scroll;
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		public void run() 
		{
			while (!stopIt)
			{
				try {
					sleep(100);
				} catch (InterruptedException e) {
				}
				
				JViewport viewport = scroll.getViewport();
				int height = viewport.getViewSize().height - viewport.getViewRect().height;
				int viewHeight = (int)viewport.getViewPosition().getY();
				if (up)
				{
					if (viewHeight < height)
					{
						viewHeight++;
					}
					else
					{
						viewHeight--;
						up = false;
					}
				}
				else
				{
					if (viewHeight > 0)
					{
						viewHeight--;
					}
					else
					{
						viewHeight++;
						up = true;
					}
				}
				viewport.setViewPosition(new Point(0,viewHeight));
			}
		}
		
		/**
		 * Sets the variable stopIt to bring this Thread to an end.
		 */
		public void stopIt()
		{
			stopIt = true;
		}
}

}

