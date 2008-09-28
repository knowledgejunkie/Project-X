/*
 * @(#)GuiInterface.java
 *
 * Copyright (c) 2005-2008 by dvb.matt, All Rights Reserved. 
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

package net.sourceforge.dvb.projectx.common;

import net.sourceforge.dvb.projectx.common.GuiInterfaceIF;


public class GuiInterface implements GuiInterfaceIF {

	private GuiInterfaceIF impl = null;

	private boolean accessible = false;

	public GuiInterface()
	{}

	/**
	 *
	 */
	public GuiInterface(boolean loadGUI)
	{
		getImplementation(loadGUI);
	}

	/**
	 *
	 */
	private void getImplementation(boolean loadGUI)
	{
		try {
			if (loadGUI)
			{
				impl = (GuiInterfaceIF) Class.forName("net.sourceforge.dvb.projectx.gui.GuiInterfaceImpl").newInstance();
				accessible = true;
			}

		} catch (Exception e) {
			//System.out.println(e.toString());
			Common.setExceptionMessage(e);
			// no gui class found
		}
	}

	/**
	 *
	 */
	public boolean isAvailable()
	{
		return accessible;
	}

	/**
	 * load main stuff
	 */
	public void loadGui()
	{
		if (isAvailable())
			impl.loadGui();
	}

	/**
	 *
	 */
	public void showTtxPageMatrix()
	{
		if (isAvailable())
			impl.showTtxPageMatrix();
	}

	/**
	 *
	 */
	public void initTtxPageMatrix(String str)
	{
		if (isAvailable())
			impl.initTtxPageMatrix(str);
	}

	/**
	 *
	 */
	public void updateTtxPageMatrix(String str)
	{
		if (isAvailable())
			impl.updateTtxPageMatrix(str);
	}

	/**
	 *
	 */
	public void showPreSettings()
	{
		if (isAvailable())
			impl.showPreSettings();
	}

	/**
	 *
	 */
	public void resetBitrateMonitor()
	{
		if (isAvailable())
			impl.resetBitrateMonitor();
	}

	/**
	 *
	 */
	public void updateBitrateMonitor(int value, byte[] array, String str)
	{
		if (isAvailable())
			impl.updateBitrateMonitor(value, array, str);
	}

	/**
	 *
	 */
	public void updateTtxHeader(String str)
	{
		if (isAvailable())
			impl.updateTtxHeader(str);
	}

	/**
	 *
	 */
	public void updateVpsLabel(String str)
	{
		if (isAvailable())
			impl.updateVpsLabel(str);
	}

	/**
	 *
	 */
	public void showAVOffset(String str)
	{
		if (isAvailable())
			impl.showAVOffset(str);
	}

	/**
	 *
	 */
	public void showExportStatus(String str)
	{
		if (isAvailable())
			impl.showExportStatus(str);
	}

	/**
	 *
	 */
	public void showExportStatus(String str, int value)
	{
		if (isAvailable())
			impl.showExportStatus(str, value);
	}

	/**
	 *
	 */
	public void updateProgressBar(int percent)
	{
		if (isAvailable())
			impl.updateProgressBar(percent);

		else
			System.out.print("\r" + percent + " %");
	}

	/**
	 *
	 */
	public void updateProgressBar(String str)
	{
		if (isAvailable())
			impl.updateProgressBar(str);

		else
			System.out.println(str);
	}

	/**
	 *
	 */
	public void setMessage(String msg, boolean tofront, int background)
	{
		if (isAvailable())
			impl.setMessage(msg, tofront, background);
	}

	/**
	 *
	 */
	public void addPidToExtract(Object obj)
	{
		if (isAvailable())
			impl.addPidToExtract(obj);
	}

	/**
	 *
	 */
	public void closeLogWindow()
	{
		if (isAvailable())
			impl.closeLogWindow();
	}

	/**
	 *
	 */
	public void showLogWindow()
	{
		if (isAvailable())
			impl.showLogWindow();
	}

	/**
	 *
	 */
	public String getUserInputDialog(String arg1, String arg2)
	{
		return (isAvailable() ? impl.getUserInputDialog(arg1, arg2) : null);
	}

	/**
	 *
	 */
	public boolean getUserConfirmationDialog(String str)
	{
		return (isAvailable() ? impl.getUserConfirmationDialog(str) : false);
	}

	/**
	 *
	 */
	public void showErrorMessageDialog(Object message, String title)
	{
		if (isAvailable())
			impl.showErrorMessageDialog(message, title);

		else
		{
			System.err.println(title);
			System.err.println(message.toString());
		}
	}

	/**
	 *
	 */
	public void showMessageDialog(Object message, String title)
	{
		if (isAvailable())
			impl.showMessageDialog(message, title);

		else
		{
			System.out.println(title);
			System.out.println(message.toString());
		}
	}

	/**
	 * 
	 */
	public Object getMainFrameBounds()
	{
		if (isAvailable())
			return impl.getMainFrameBounds();

		return null;
	}

	/**
	 *
	 */
	public void showMainFrame(boolean b)
	{
		if (isAvailable())
			impl.showMainFrame(b);
	}

	/**
	 *
	 */
	public void setMainFrameTitle(String str)
	{
		if (isAvailable())
			impl.setMainFrameTitle(str);
	}

	/**
	 *
	 */
	public void resetMainFrameTitle()
	{
		if (isAvailable())
			impl.resetMainFrameTitle();
	}

	/**
	 *
	 */
	public void addCollectionAtEnd()
	{
		if (isAvailable())
			impl.addCollectionAtEnd();
	}

	/**
	 *
	 */
	public void showActiveCollection(int index)
	{
		if (isAvailable())
			impl.showActiveCollection(index);
	}

	/**
	 *
	 */
	public void updateCollectionPanel(int index)
	{
		if (isAvailable())
			impl.updateCollectionPanel(index);
	}

	/**
	 *
	 */
	public void setSubpictureTitle(String str)
	{
		if (isAvailable())
			impl.setSubpictureTitle(str);
	}

	/**
	 *
	 */
	public void showSubpicture()
	{
		if (isAvailable())
			impl.showSubpicture();
	}

	/**
	 *
	 */
	public void hideSubpicture()
	{
		if (isAvailable())
			impl.hideSubpicture();
	}

	/**
	 *
	 */
	public boolean isSubpictureVisible()
	{
		if (isAvailable())
			return impl.isSubpictureVisible();

		return false;
	}

	/**
	 *
	 */
	public void repaintSubpicture()
	{
		if (isAvailable())
			impl.repaintSubpicture();
	}

	/**
	 *
	 */
	public void setOSDMessage(String str, boolean b)
	{
		if (isAvailable())
			impl.setOSDMessage(str, b);
	}

	/**
	 *
	 */
	public void showCutIcon(boolean b, Object[] obj, Object list)
	{
		if (isAvailable())
			impl.showCutIcon(b, obj, list);
	}

	/**
	 *
	 */
	public void showChapterIcon(Object[] obj, Object list)
	{
		if (isAvailable())
			impl.showChapterIcon(obj, list);
	}

	/**
	 *
	 */
	public void updatePreviewPixel()
	{
		if (isAvailable())
			impl.updatePreviewPixel();
	}

	/**
	 *
	 */
	public void repaintPicturePanel()
	{
		if (isAvailable())
			impl.repaintPicturePanel();
	}

	/**
	 *
	 */
	public byte[] editGOP(byte[] data, long[][] pts_indices)
	{
		if (isAvailable())
			return impl.editGOP(data, pts_indices);

		return data;
	}
}

