/*
 * @(#)X_Help.java - provides an external HTML pane for additional infos
 *
 * Copyright (c) 2004 by dvb.matt, All Rights Reserved.
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

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.*;
import javax.swing.text.*;
import javax.swing.event.*;

//DM20032004 081.6 int18 introduced
public class Html extends JFrame
{
	public Html()
	{
		setBounds( 200, 25, 600, 400);
		HtmlPane html = new HtmlPane();
		setContentPane(html);
		setTitle("Html Frame");

		addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				quit();
			}
		});

		UIManager.addPropertyChangeListener(new UISwitchListener((JComponent)getRootPane()));
	}

	public void quit()
	{
		dispose();
	}
}

class HtmlPane extends JScrollPane implements HyperlinkListener
{
	JEditorPane html;

	public HtmlPane()
	{
		try
		{
			File f = new File ("htmls/index.html");
			String s = f.getAbsolutePath();
			s = "file:"+s;
			URL url = new URL(s);
			html = new JEditorPane(s);
			html.setEditable(false);
			html.addHyperlinkListener(this);

			JViewport vp = getViewport();
			vp.add(html);
		}
		catch (MalformedURLException e)
		{
			X.Msg("Malformed URL: " + e);
		}
		catch (IOException e)
		{
			X.Msg("IOException: " + e);
		}	
	}

	// Notification of a change relative to a hyperlink.
	public void hyperlinkUpdate(HyperlinkEvent e)
	{
		if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
		{
			linkActivated(e.getURL());
		}
	}

	protected void linkActivated(URL u)
	{
		Cursor c = html.getCursor();
		Cursor waitCursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
		html.setCursor(waitCursor);
		SwingUtilities.invokeLater(new PageLoader(u, c));
	}

	class PageLoader implements Runnable
	{
		PageLoader(URL u, Cursor c)
		{
			url = u;
			cursor = c;
		}

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

		URL url;
		Cursor cursor;
	}
}
