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
	public final static DirType FILE_DIR = new DirType(0, "FILE_DIR", net.sourceforge.dvb.projectx.xinput.file.XInputDirectoryImpl.class);

	/**
	 * Directory on a ftp server
	 */
	public final static DirType FTP_DIR = new DirType(1, "FTP_DIR", net.sourceforge.dvb.projectx.xinput.ftp.XInputDirectoryImpl.class);

	/**
	 * Directory on a harddisk of a topfield receiver in raw format
	 */
	public final static DirType TFRAW_DIR = new DirType(2, "TFRAW_DIR", net.sourceforge.dvb.projectx.xinput.topfield_raw.XInputDirectoryImpl.class);

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