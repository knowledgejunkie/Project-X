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
	public final static FileType FILE = new FileType(0, "FILE", net.sourceforge.dvb.projectx.xinput.file.XInputFileImpl.class);

	/**
	 * File on a ftp server
	 */
	public final static FileType FTP = new FileType(1, "FTP", net.sourceforge.dvb.projectx.xinput.ftp.XInputFileImpl.class);

	/**
	 * File on a harddisk of a topfield receiver in raw format
	 */
	public final static FileType TFRAW = new FileType(2, "TFRAW", net.sourceforge.dvb.projectx.xinput.topfield_raw.XInputFileImpl.class);

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