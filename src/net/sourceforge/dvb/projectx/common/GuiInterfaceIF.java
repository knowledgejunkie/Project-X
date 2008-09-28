/*
 * @(#)GuiInterfaceIF.java
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


public interface GuiInterfaceIF {

	public void loadGui();
	public void showTtxPageMatrix();
	public void initTtxPageMatrix(String str);
	public void updateTtxPageMatrix(String str);
	public void showPreSettings();
	public void resetBitrateMonitor();
	public void updateBitrateMonitor(int value, byte[] array, String str);
	public void updateTtxHeader(String str);
	public void updateVpsLabel(String str);
	public void showAVOffset(String str);
	public void showExportStatus(String str);
	public void showExportStatus(String str, int value);
	public void updateProgressBar(int percent);
	public void updateProgressBar(String str);
	public void setMessage(String msg, boolean tofront, int background);
	public void addPidToExtract(Object obj);
	public void closeLogWindow();
	public void showLogWindow();
	public String getUserInputDialog(String arg1, String arg2);
	public boolean getUserConfirmationDialog(String str);
	public void showErrorMessageDialog(Object message, String title);
	public void showMessageDialog(Object message, String title);
	public Object getMainFrameBounds();
	public void showMainFrame(boolean b);
	public void setMainFrameTitle(String str);
	public void resetMainFrameTitle();
	public void addCollectionAtEnd();
	public void showActiveCollection(int index);
	public void updateCollectionPanel(int index);
	public void setSubpictureTitle(String str);
	public void showSubpicture();
	public void hideSubpicture();
	public boolean isSubpictureVisible();
	public void repaintSubpicture();
	public void setOSDMessage(String str, boolean b);
	public void showCutIcon(boolean b, Object[] obj, Object list);
	public void showChapterIcon(Object[] obj, Object list);
	public void updatePreviewPixel();
	public void repaintPicturePanel();
	public byte[] editGOP(byte[] data, long[][] pts_indices);

}

