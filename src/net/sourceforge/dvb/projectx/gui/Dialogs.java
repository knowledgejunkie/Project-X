/*
 * @(#)Dialogs.java - extracted JDialogs
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

package net.sourceforge.dvb.projectx.gui;

import javax.swing.JOptionPane;
import javax.swing.JFrame;

public class Dialogs
{
	private static JFrame frame = null;

	private Dialogs()
	{}

	public Dialogs(JFrame frame)
	{
		this.frame = frame;
	}

	public static String getUserInput(String arg1)
	{
		return JOptionPane.showInputDialog(arg1);
	}

	public static String getUserInput(String arg1, String arg2)
	{
		return JOptionPane.showInputDialog(frame, arg1 , arg2, JOptionPane.QUESTION_MESSAGE );
	}

	public static boolean getUserConfirmation(String arg1)
	{
		int option = JOptionPane.showConfirmDialog(frame, arg1);

		if (option == JOptionPane.YES_OPTION)
			return true;

		return false;
	}
}

