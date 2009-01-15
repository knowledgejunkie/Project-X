/*
 * @(#)CommonGui
 *
 * Copyright (c) 2004-2008 by dvb.matt, All Rights Reserved. 
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

import javax.swing.JOptionPane;
import javax.swing.JFrame;
import javax.swing.AbstractButton;
import javax.swing.ImageIcon;

import net.sourceforge.dvb.projectx.gui.X_JFileChooser;
import net.sourceforge.dvb.projectx.gui.PicturePanel;
import net.sourceforge.dvb.projectx.gui.FileProperties;
import net.sourceforge.dvb.projectx.gui.CollectionProperties;
import net.sourceforge.dvb.projectx.gui.PatchDialog;
import net.sourceforge.dvb.projectx.gui.CutPanel;
import net.sourceforge.dvb.projectx.gui.PlayerFrame;
import net.sourceforge.dvb.projectx.gui.SubpictureFrame;

import net.sourceforge.dvb.projectx.common.Resource;

/**
 *
 */
public class CommonGui extends Object {

	private static JFrame frame = null;

	private static Object suggestion = null;

	private static X_JFileChooser chooser;

	private static CutPanel cutpanel;

	private static PicturePanel picturepanel;

	private static FileProperties file_properties;

	private static CollectionProperties collection_properties;

	private static PatchDialog patch_dialog = null;

	private static PlayerFrame player_frame = null;

	private static SubpictureFrame subpicture_frame =  null;

	/**
	 *
	 */
	public CommonGui()
	{
		chooser = new X_JFileChooser();

		picturepanel = new PicturePanel();
		cutpanel = new CutPanel();
		file_properties = new FileProperties();
		collection_properties = new CollectionProperties();
	}

	public static void setMainFrame(JFrame _frame)
	{
		frame = _frame;
	}

	/**
	 *
	 */
	public static String getUserInput(String arg1)
	{
		return getUserInput(arg1, null);
	}

	/**
	 *
	 */
	public static String getUserInput(String arg1, String arg2)
	{
		Object obj = getUserInput(arg1, arg2, suggestion);

		if (obj != null)
		{
			suggestion = obj;
			return obj.toString();
		}

		return null;
	}

	/**
	 * user dialog field filled with suggestion
	 * by 'mcr42' 2005/06/16
	 */
	public static String getUserInput(String arg1, String arg2, Object arg3)
	{
		return getUserInput(null, arg1, arg2, arg3);
	}

	/**
	 *
	 */
	public static String getUserInput(JFrame _frame, String arg1, String arg2, Object arg3)
	{
		if (_frame == null)
			_frame = frame;

		Object obj = JOptionPane.showInputDialog(_frame, arg1, arg2, JOptionPane.QUESTION_MESSAGE, null, null, arg3);

		if (obj != null)
			return obj.toString();

		return null;
	}

	/**
	 *
	 */
	public static boolean getUserConfirmation(String arg1)
	{
		int option = JOptionPane.showConfirmDialog(frame, arg1);

		if (option == JOptionPane.YES_OPTION)
			return true;

		return false;
	}

	/**
	 *
	 */
	public static void showErrorMessageDialog(Object message, String title)
	{
		JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
	}

	/**
	 *
	 */
	public static void showMessageDialog(Object message, String title)
	{
		JOptionPane.showMessageDialog(null, message, title, JOptionPane.INFORMATION_MESSAGE);
	}

	/**
	 * Sets a button's text and mnemonic values using the specified resource
	 * key. The button text is scanned for &. If found the character after it is
	 * used as menmonic.
	 * 
	 * @param button
	 *            the button (e.g. a menu or menu item) to localize
	 * @param key
	 *            the resource string to find
	 */
	public static final void localize(AbstractButton button, String key)
	{
		String text = Resource.getString(key);
		
		int pos = text.indexOf('&');

		if (pos != -1)
		{
			char mnemonic = text.charAt(pos + 1);
			button.setMnemonic(mnemonic);
			text = text.substring(0, pos) + text.substring(pos + 1);
		}

		button.setText(text);
	}

	/**
	 * Loads an image as ImageIcon.
	 * 
	 * @param iconName
	 * @return ImageIcon
	 */
	public static ImageIcon loadIcon(String iconName)
	{
		return new ImageIcon(Resource.getResourceURL(iconName));
	}

	/**
	 *
	 */
	public static X_JFileChooser getMainFileChooser()
	{
		return chooser;
	}

	/**
	 *
	 */
	public static PicturePanel getPicturePanel()
	{
		return picturepanel;
	}

	/**
	 *
	 */
	public static CutPanel getCutPanel()
	{
		return cutpanel;
	}

	/**
	 *
	 */
	public static FileProperties getFileProperties()
	{
		return file_properties;
	}

	/**
	 *
	 */
	public static CollectionProperties getCollectionProperties()
	{
		return collection_properties;
	}

	/**
	 *
	 */
	public static PatchDialog getPatchDialog()
	{
		if (patch_dialog == null)
			patch_dialog = new PatchDialog(frame);

		return patch_dialog;
	}

	/**
	 *
	 */
	public static PlayerFrame getPlayerFrame()
	{
		if (player_frame == null)
			player_frame = new PlayerFrame();

		return player_frame;
	}

	/**
	 *
	 */
	public static SubpictureFrame getSubpictureFrame()
	{
		if (subpicture_frame == null)
			subpicture_frame = new SubpictureFrame();

		return subpicture_frame;
	}
}

