/*
 * @(#)Settings.java - holds all data of interest to re-use
 *
 * Copyright (c) 2005
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

/*
 * initiated by pstorch
 */

package net.sourceforge.dvb.projectx.common;

import java.io.BufferedReader;
import	java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import net.sourceforge.dvb.projectx.xinput.XInputDirectory;

/**
 * The Settings class handles the settings for Project-X.
 * 
 * @author Peter Storch
 */
public class Settings extends Object {

	/** the default ini filename */
	private static final String DEFAULT_INI = "X.ini";

	/** the current ini filename */
	private String inifile = "";

	/** all settings are being hold in this properties object */
	private Properties props = new Properties();

	/**  */
	private ArrayList input_directories = new ArrayList();

	/**  */
	private ArrayList output_directories = new ArrayList();

	/**
	 * Constructor
	 */
	public Settings()
	{
		this(Resource.workdir + Resource.filesep + DEFAULT_INI);
	}

	/**
	 * Constructor
	 * 
	 * @param filename
	 */
	public Settings(String filename)
	{
		inifile = filename;
		load();
		buildInputDirectories();
		buildOutputDirectories();
	}


	/**
	 * 
	 */
	public void loadProperties(java.io.ByteArrayInputStream is) throws IOException
	{
		props.load(is);
	}

	/**
	 * 
	 */
	public byte[] storeProperties() throws IOException
	{
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		props.store(os, null);

		return os.toByteArray();
	}

	/**
	 * Loads the ini file.
	 */
	public void load()
	{
		try {
			BufferedReader r = new BufferedReader(new FileReader(inifile));

			String line = null;

			while ((line = r.readLine()) != null)
			{
				if (line.startsWith("#"))
					continue;
				
				int pos = line.indexOf('=');

				if (pos != -1)
				{
					String key = line.substring(0, pos);
					String value = line.substring(pos + 1);
					props.put(key, value);
				}
			}

			r.close();

		} catch(IOException e) {

			System.out.println(Resource.getString("msg.loadini.error") + " " + e);
		}
	}
	
	/**
	 * Saves the ini file.
	 */
	public void save()
	{
		save(inifile);
	}

	/**
	 * Saves the ini file (std or extra name)
	 */
	public void save(String str)
	{
		if (str == null)
			str = inifile;

		try {
			PrintWriter w = new PrintWriter(new FileWriter(str));

			String base_key = "# Project-X INI";

			w.println(base_key);

			w.println("# " + Common.getVersionName() + " / " + Common.getVersionDate());

			TreeMap map = new TreeMap(props);

			Set keys = map.keySet();

			for (Iterator iter = keys.iterator(); iter.hasNext(); )
			{
				String element = (String) iter.next();

				String key = element.substring(0, element.indexOf("."));

				if (!base_key.equals(key))
				{
					w.println();
					w.println("# " + key);
					base_key = key;
				}

				w.println(element + "=" + map.get(element));
			}

			w.close();

		} catch(IOException e) {

			Common.setExceptionMessage(e);
		}
	}

	/**
	 * build input_directory Xinput object from property list (strings only)
	 */
	public void buildInputDirectories()
	{
		String key = Keys.KEY_InputDirectories;

		ArrayList input = (ArrayList)getListProperty(key);

		int i = 0;

		for ( ; i < input.size(); i++)
		{
			if (addInputDirectory(input.get(i)) == null)
			{
				input.remove(i);
				i--;
			}
		}

		if (i != input.size())
			setListProperty(key, input);
	}


	/**
	 * updates property list (strings only)
	 */
	public void updateInputDirectories()
	{
		ArrayList input = new ArrayList();

		for (int i = 0; i < input_directories.size(); i++)
			input.add( input_directories.get(i).toString());

		setListProperty(Keys.KEY_InputDirectories, input);
	}

	/**
	 * adds input_directory Xinput object
	 */
	public String addInputDirectory(Object value)
	{
		if (value == null)
			return null;

		try {
			XInputDirectory xInputDirectory = (XInputDirectory) value;

			input_directories.add(xInputDirectory);

			return xInputDirectory.toString();

		} catch (Exception e) {
			// is not yet xinput object
		}

		try {
			XInputDirectory xInputDirectory = new XInputDirectory(value);

			if (xInputDirectory.test())
			{
				input_directories.add(xInputDirectory);

				return xInputDirectory.toString();
			}

		} catch (RuntimeException e) {
			// If there are problems with the directory simply ignore it and do nothing
		}

		return null;
	}

	/**
	 * removes input_directory Xinput object
	 */
	public void removeInputDirectory(int index)
	{
		if (index < 0 || input_directories.isEmpty() || input_directories.size() <= index)
			return;

		input_directories.remove(index);
	}

	/**
	 * returns input_directory Xinput objects
	 */
	public ArrayList getInputDirectories()
	{
		return input_directories;
	}


	/**
	 * build output_directory  from property list (strings only)
	 */
	public void buildOutputDirectories()
	{
		String key = Keys.KEY_OutputDirectories;

		ArrayList output = (ArrayList)getListProperty(key);

		int i = 0;

		for ( ; i < output.size(); i++)
			addOutputDirectory(output.get(i));

		if (i != output.size())
			setListProperty(key, output);
	}


	/**
	 * updates property list (strings only)
	 */
	public void updateOutputDirectories()
	{
		ArrayList output = new ArrayList();

		for (int i = 0; i < output_directories.size(); i++)
			output.add( output_directories.get(i).toString());

		setListProperty(Keys.KEY_OutputDirectories, output);
	}


	/**
	 * adds output_directory string
	 */
	public void addOutputDirectory(Object value)
	{
		addOutputDirectory(value, -1);
	}

	/**
	 * adds output_directory string
	 */
	public void addOutputDirectory(Object value, int index)
	{
		if (value == null)
			return;

		if (index < 0 || index > output_directories.size())
			output_directories.add(value);
		else
			output_directories.add(index, value);

		updateOutputDirectories();
	}

	/**
	 * removes output_directory string
	 */
	public void removeOutputDirectory(int index)
	{
		if (index < 0 || output_directories.size() < index - 1)
			return;

		output_directories.remove(index);

		updateOutputDirectories();
	}

	/**
	 * returns output_directory strings
	 */
	public ArrayList getOutputDirectories()
	{
		return output_directories;
	}
	
	/**
	 * Sets a String property.
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	public void setProperty(String key, String value)
	{
		if (value != null)
			props.setProperty(key, value);

		else
			props.remove(key);
	}

	/**
	 * Sets a Object property.
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	public void setProperty(String key, Object value)
	{
		String str = value == null ? null : String.valueOf(value);

		setProperty(key, str);
	}

	/**
	 * Sets a Object property.
	 * 
	 * @param key of obj
	 * @param value
	 * @return
	 */
	public void setProperty(Object[] key, Object value)
	{
		setProperty(key[0].toString(), String.valueOf(value));
	}

	/**
	 * Returns a String property.
	 * 
	 * @param key
	 * @return
	 */
	public String getProperty(String key)
	{
		return props.getProperty(key);
	}
	
	/**
	 * Returns a String property. If not found it returns the given default value.
	 * 
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public String getProperty(String key, String defaultValue)
	{
		return props.getProperty(key, defaultValue);
	}

	/**
	 * Returns a String property. If not found it returns the given default value.
	 * 
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public String getProperty(String[] key_defaultValue)
	{
		String key = key_defaultValue[0];
		String defaultValue = key_defaultValue[1];

		return props.getProperty(key, defaultValue);
	}

	/**
	 * Returns an integer property.
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	public void setIntProperty(String key, int value)
	{
		props.setProperty(key, String.valueOf(value));
	}

	/**
	 * Returns an integer property.
	 * 
	 * @param key
	 * @return
	 */
	public int getIntProperty(String key)
	{
		return Integer.parseInt(props.getProperty(key));
	}
	
	/**
	 * Returns an integer property. If not found or not parseable it returns the default value.
	 * 
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public int getIntProperty(String key, int defaultValue)
	{
		int value = defaultValue;

		try {
			value = Integer.parseInt(getProperty(key));

		} catch(Exception e) {
			// ok, then we stay with the default value
		}

		return value;
	}

	/**
	 * Returns an integer property. If not found or not parseable it returns the default value.
	 * 
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public int getIntProperty(String[] key_defaultValue)
	{
		String key = key_defaultValue[0];
		int value = Integer.parseInt(key_defaultValue[1]);

		try {
			value = Integer.parseInt(getProperty(key));

		} catch(Exception e) {
			// ok, then we stay with the default value
		}

		return value;
	}
	
	/**
	 * Returns a boolean property.
	 * Default is false.
	 * 
	 * @param key
	 * @return
	 */
	public void setBooleanProperty(String key, boolean value)
	{
		props.setProperty(key, value ? "1" : "0");
	}

	/**
	 * Returns a Boolean property.
	 * Null if key not found. Default is false.
	 * 
	 * @param key
	 * @return
	 */
	public Boolean getBooleanProperty(String key)
	{
		String value = getProperty(key);

		if (value == null)
			return null;

		else if (value.equals("1") || value.equals("true") || value.equals("yes") || value.equals("on"))
			return Boolean.TRUE;

		return Boolean.FALSE;
	}
	
	/**
	 * Returns a Boolean property or the given defaultValue.
	 * 
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public boolean getBooleanProperty(String key, boolean defaultValue)
	{
		Boolean value = getBooleanProperty(key);

		if (value == null)
			return defaultValue;

		return value.booleanValue();
	}

	/**
	 * Returns a Boolean property or the given defaultValue.
	 * 
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public boolean getBooleanProperty(String[] key_defaultValue)
	{
		String key = key_defaultValue[0];
		String defaultValue = key_defaultValue[1];

		Boolean value = getBooleanProperty(key);

		if (value != null)
			return value.booleanValue();

		if (defaultValue.equals("1") || defaultValue.equals("true") || defaultValue.equals("yes") || defaultValue.equals("on"))
			return true;

		return false;
	}

	/**
	 * Sets a List of properties starting with key.
	 * 
	 * @param key
	 * @param list
	 * @return
	 */
	public void setListProperty(String key, List list)
	{
		removeListProperty(key);

		for (int i = 0; i < list.size(); i++)
		{
			Object element = list.get(i);
			setProperty(key + i, element);
		}
	}

	/**
	 * Gets a List of properties starting with key.
	 * 
	 * @param key
	 * @return
	 */
	public List getListProperty(String key)
	{
		Set keys = props.keySet();

		List list = new ArrayList();

		for (Iterator iter = keys.iterator(); iter.hasNext(); )
		{
			String element = (String) iter.next();

			if (element.startsWith(key))
				list.add(props.getProperty(element));
		}
		
		return list;
	}

	/**
	 * removes a List of properties starting with key.
	 * 
	 * @param key
	 * @return
	 */
	public void removeListProperty(String key)
	{
		Set keys = props.keySet();

		ArrayList list = new ArrayList();

		for (Iterator iter = keys.iterator(); iter.hasNext(); )
		{
			String element = (String) iter.next();

			if (element.startsWith(key))
				list.add(element);
		}

		for (int i = 0; i < list.size(); i++)
			remove(list.get(i).toString());
	}

	/**
	 * Sets a Map of properties starting with key.
	 * 
	 * @param key
	 * @param map
	 * @return
	 */
	public void setHashMapProperty(String key, Map map)
	{
		Set keys = map.keySet();

		for (Iterator iter = keys.iterator(); iter.hasNext(); )
		{
			String element = (String) iter.next();
			setProperty(key + element, map.get(element));
		}
	}

	/**
	 * Gets a Map of properties starting with key.
	 * The Map contains the entries with a new key like this:
	 * mapKey = propsKey - key.
	 * 
	 * @param key
	 * @return
	 */
	public Map getHashMapProperty(String key)
	{
		Set keys = props.keySet();
		Map map = new HashMap();

		for (Iterator iter = keys.iterator(); iter.hasNext();)
		{
			String element = (String) iter.next();

			if (element.startsWith(key))
				map.put(element.substring(key.length()), props.getProperty(element));
		}
		
		return map;
	}
	
	/**
	 * Removes a property.
	 * 
	 * @param key
	 */
	public void remove(String key)
	{
		props.remove(key);
	}

	/**
	 * Returns the ini filename.
	 * 
	 * @return
	 */
	public String getInifile()
	{
		return inifile;
	}

}