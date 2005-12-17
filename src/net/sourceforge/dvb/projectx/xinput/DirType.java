/*
 * @(#)DirType.java
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

public class DirType {

	/**
	 * Stores all instances of DirType
	 */
	private final static Collection dirTypes = new ArrayList(10);

	/**
	 * Directory in a normal filesystem
	 */
	public final static DirType FILE_DIR = new DirType(0, "FILE_DIR",
			net.sourceforge.dvb.projectx.xinput.file.XInputDirectoryImpl.class);

	/**
	 * Directory on a ftp server
	 */
	public final static DirType FTP_DIR = new DirType(1, "FTP_DIR",
			net.sourceforge.dvb.projectx.xinput.ftp.XInputDirectoryImpl.class);

	/**
	 * Directory on a harddisk of a topfield receiver in raw format
	 */
	public final static DirType RAW_DIR = new DirType(2, "RAW_DIR",
			net.sourceforge.dvb.projectx.xinput.topfield_raw.XInputDirectoryImpl.class);

	/**
	 * Default DirType
	 */
	public final static DirType DEFAULT = FILE_DIR;

	private int type;

	private String name;

	private Class implementation;

	private DirType(int aType, String aName, Class aImplementation) {
		type = aType;
		name = aName;
		implementation = aImplementation;
		dirTypes.add(this);
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
	public static Collection getDirTypes() {
		return dirTypes;
	}

	public String toString() {
		return getName();
	}
}