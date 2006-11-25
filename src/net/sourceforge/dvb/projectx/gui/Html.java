/*
 * @(#)Html.java - provides an external HTML pane for additional infos
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

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.*;

import javax.swing.text.*;
import javax.swing.event.*;

import edu.stanford.ejalbert.BrowserLauncher;

import net.sourceforge.dvb.projectx.common.Resource;
import net.sourceforge.dvb.projectx.common.Common;


/**
 * Html.java - provides an external HTML pane for additional infos.
 * 
 * @since DM20032004 081.6 int18
 */
public class Html extends JFrame {

	/**
	 * Constructor of Html.
	 */
	public Html()
	{
		init(null);
	}

	public Html(String str)
	{
		init(str);
	}

	private void init(String str)
	{
		setBounds( 200, 25, 600, 600);
		HtmlPane html = new HtmlPane(str);
		setContentPane(html);
		setTitle(Resource.getString("html.title"));		

		addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				quit();
			}
		});

		UIManager.addPropertyChangeListener(new UISwitchListener((JComponent)getRootPane()));
	}

	/**
	 * Quit disposes this window.
	 */
	public void quit()
	{
		dispose();
	}
}

/**
 * HtmlPane.
 */
class HtmlPane extends JScrollPane implements HyperlinkListener
{
	/** the JEditorPane */
	private JEditorPane html;

	/**
	 * Constructor of HtmlPane.
	 */
	public HtmlPane(String url_str)
	{
		try
		{
			if (url_str != null)
				html = new JEditorPane(url_str);

			else
			{
				url_str = "htmls/index.html";
				html = new JEditorPane(Resource.getLocalizedResourceURL("htmls", "index.html"));
			}
		}
		catch (MalformedURLException e)
		{
			Common.setMessage("Malformed URL: " + e);
			html = new JEditorPane();
			html.setText("wrong url ! :\n" + url_str);
		}
		catch (IOException e)
		{
			Common.setMessage("IOException: " + e);
			html = new JEditorPane();
			html.setText("missing resource ! :\n" + url_str);
		}	

		html.setEditable(false);
		html.addHyperlinkListener(this);

		JViewport vp = getViewport();
		vp.add(html);
	}

	/**
	 * Notification of a change relative to a hyperlink.
	 * 
	 * @see javax.swing.event.HyperlinkListener#hyperlinkUpdate(javax.swing.event.HyperlinkEvent)
	 */
	public void hyperlinkUpdate(HyperlinkEvent e)
	{
		if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
		{
			linkActivated(e.getURL());
		}
	}

	/**
	 * Called if someone has activated a hyperlink.
	 * 
	 * @param u URL of the link
	 */
	protected void linkActivated(URL u)
	{
		// external links are loaded by the BrowserLauncher
		if (u.getProtocol().equals("http")
			|| u.getProtocol().equals("https"))
		{
			try 
			{
				BrowserLauncher.openURL(u.toString());
			} 
			catch (IOException e) 
			{
				Common.setMessage(Resource.getString("msg.browser.launcher.error") + " " + e);
			}
		}
		else
		{
			// all local links are loaded into the JEditorPane
			Cursor c = html.getCursor();
			Cursor waitCursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
			html.setCursor(waitCursor);
			SwingUtilities.invokeLater(new PageLoader(u, c));
		}
	}

	/**
	 * PageLoader provides a thread for loading a page.
	 */
	private class PageLoader implements Runnable
	{
		private URL url;
		private Cursor cursor;

		/**
		 * Constructor of PageLoader.
		 * 
		 * @param u URL to load
		 * @param c Cursor to display
		 */
		PageLoader(URL u, Cursor c)
		{
			url = u;
			cursor = c;
		}

		/* (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		public void run()
		{
			if (url == null)
			{
				// restore the original cursor
				html.setCursor(cursor);

				Container parent = html.getParent();
				parent.repaint();
			}
			else
			{
				Document doc = html.getDocument();
				try
				{
					html.setPage(url);
				}
				catch (IOException ioe)
				{
					html.setDocument(doc);
					getToolkit().beep();
				}
				finally
				{
					// schedule the cursor to revert after
					// the paint has happended.
					url = null;
					SwingUtilities.invokeLater(this);
				}
			}
		}
	}
}
