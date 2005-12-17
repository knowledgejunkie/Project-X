/*
 * @(#)FileType.java
 *
 * Copyright (c) 2004-2005 by roehrist, All Rights Reserved. 
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

package net.sourceforge.dvb.projectx.xinput;

import java.util.ArrayList;
import java.util.Collection;

public class FileType {

	/**
	 * Stores all instances of FileType
	 */
	private final static Collection fileTypes = new ArrayList(10);

	/**
	 * File in a normal filesystem
	 */
	public final static FileType FILE = new FileType(0, "FILE",
			net.sourceforge.dvb.projectx.xinput.file.XInputFileImpl.class);

	/**
	 * File on a ftp server
	 */
	public final static FileType FTP = new FileType(1, "FTP",
			net.sourceforge.dvb.projectx.xinput.ftp.XInputFileImpl.class);

	/**
	 * File on a harddisk of a topfield receiver in raw format
	 */
	public final static FileType RAW = new FileType(2, "RAW",
			net.sourceforge.dvb.projectx.xinput.topfield_raw.XInputFileImpl.class);

	/**
	 * Default FileType
	 */
	public final static FileType DEFAULT = FILE;

	private int type;

	private String name;

	private Class implementation;

	private FileType(int aType, String aName, Class aImplementation) {
		type = aType;
		name = aName;
		implementation = aImplementation;
		fileTypes.add(this);
	}

	/**
	 * Get type name
	 * 
	 * @return type name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Get type value
	 * 
	 * @return type value
	 */
	public int getType() {
		return type;
	}

	/**
	 * @return Returns the implementation.
	 */
	public Class getImplementation() {
		return implementation;
	}

	/**
	 * @return Returns the fileTypes.
	 */
	public static Collection getFileTypes() {
		return fileTypes;
	}

	public String toString() {
		return getName();
	}
}