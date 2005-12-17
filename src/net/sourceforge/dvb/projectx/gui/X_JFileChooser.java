/*
 * @(#)X_JFileChooser.java - extracted JFileChooser
 *
 * Copyright (c) 2005 by dvb.matt, All Rights Reserved. 
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

import javax.swing.JFileChooser;
import java.io.File;

import net.sourceforge.dvb.projectx.common.Resource;
import net.sourceforge.dvb.projectx.common.Common;
import net.sourceforge.dvb.projectx.common.Keys;


public class X_JFileChooser extends JFileChooser {

	/**
	 *
	 */
	public X_JFileChooser()
	{
		super();
		localize();
	}

	/**
	 *
	 */
	public X_JFileChooser(String current_directory)
	{
		super(current_directory);
		localize();
	}

	/**
	 *
	 */
	private void localize()
	{
		setCurrentDirectory(Common.getSettings().getProperty(Keys.KEY_ActiveDirectory));
		setApproveButtonText(Resource.getString("FileChooser.Select"));
		setDialogTitle(Resource.getString("FileChooser.Title"));
	}

	/**
	 *
	 */
	public void setCurrentDirectory(String current_directory)
	{
		if (current_directory.startsWith("?"))
			super.setCurrentDirectory(new File(current_directory.substring(1)));

	//	else if (!current_directory.equals("") && super.getCurrentDirectory().toString().equals(System.getProperty("user.home")))
		else if (!current_directory.equals(""))
			super.setCurrentDirectory(new File(current_directory));
	}

	/**
	 *
	 */
	public void rescanCurrentDirectory()
	{
		String current_directory = Common.getSettings().getProperty(Keys.KEY_StartPath_Value);

		setCurrentDirectory(current_directory);

		super.rescanCurrentDirectory();

		Common.getSettings().setProperty(Keys.KEY_ActiveDirectory[0], super.getCurrentDirectory().toString());
	}
}
