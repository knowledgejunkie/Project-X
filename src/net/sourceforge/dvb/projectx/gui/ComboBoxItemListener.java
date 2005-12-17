/*
 * @(#)ComboBoxItemListener
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

import javax.swing.JComboBox;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import net.sourceforge.dvb.projectx.common.Common;

/**
 * 
 */
public class ComboBoxItemListener implements ActionListener
{
	public void actionPerformed(ActionEvent e)
	{
		String actName = e.getActionCommand();

		// refuse additional actioncommand "comboBoxEdit" from jre > 1.4 ?
		if (actName.indexOf('.') < 0)
			return;

		JComboBox box = (JComboBox)e.getSource();

		Common.getSettings().setProperty(actName, box.getSelectedItem());
	}
}
