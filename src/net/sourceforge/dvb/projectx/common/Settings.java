/*
 * @(#)Settings.java
 *
 * Copyright (c) 2001-2004 by dvb.matt, All rights reserved.
 * 
 * This file is part of X, a free Java based demux utility.
 * X is intended for educational purposes only, as a non-commercial test project.
 * It may not be used otherwise. Most parts are only experimental.
 * 
 *
 * This program is free software; you can redistribute it free of charge
 * and/or modify it under the terms of the GNU General Public License as published by
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

import java.io.BufferedReader;
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

/**
 * The Settings class handles the settings for Project-X.
 * 
 * @author Peter Storch
 */
public class Settings {

	/** the default ini filename */
	private static final String DEFAULT_INI = "X.ini";

	/** the current ini filename */
	private String inifile = "";

	/** all settings are being hold in this properties object */
	private Properties props = new Properties();

	/**
	 * Constructor
	 */
	public Settings() {
		this(Resource.workdir + Resource.filesep + DEFAULT_INI);
	}

	/**
	 * Constructor
	 * 
	 * @param filename
	 */
	public Settings(String filename){
		try{
			inifile = filename;
			BufferedReader r = new BufferedReader(new FileReader(filename));
			String line = null;
			while ((line = r.readLine()) != null){
				if (line.startsWith("#")){
					continue;
				}
				
				int pos = line.indexOf('=');
				if (pos != -1)
				{
					String key = line.substring(0, pos);
					String value = line.substring(pos+1);
					props.put(key, value);
				}
			}
			r.close();
		}catch(IOException e){
			System.out.println(Resource.getString("msg.loadini.error") + " " + e);
		}
	}
	
	/**
	 * Saves the ini file.
	 */
	public void save()
	{
		try{
			PrintWriter w = new PrintWriter(new FileWriter(inifile));
			w.println("# Project-X INI");
			TreeMap map = new TreeMap(props);
			Set keys = map.keySet();
			for (Iterator iter = keys.iterator(); iter.hasNext();) {
				String element = (String) iter.next();
				w.println(element + "=" + map.get(element));
			}
			w.close();
		}catch(IOException e){
			X.Msg(Resource.getString("msg.saveini.error") + " " + e);
		}
	}
	
	/**
	 * Sets a String property.
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	public void setProperty(String key, String value){
		if (value != null){
			props.setProperty(key, value);
		}else{
			props.remove(key);
		}
	}

	/**
	 * Sets a Object property.
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	public void setProperty(String key, Object value){
		if (value != null){
			props.setProperty(key, "" + value);
		}else{
			props.remove(key);
		}
	}

	/**
	 * Returns a String property.
	 * 
	 * @param key
	 * @return
	 */
	public String getProperty(String key){
		return props.getProperty(key);
	}
	
	/**
	 * Returns a String property. If not found it returns the given default value.
	 * 
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public String getProperty(String key, String defaultValue){
		return props.getProperty(key, defaultValue);
	}

	/**
	 * Returns an integer property.
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	public void setIntProperty(String key, int value){
		props.setProperty(key, String.valueOf(value));
	}

	/**
	 * Returns an integer property.
	 * 
	 * @param key
	 * @return
	 */
	public int getIntProperty(String key){
		return Integer.parseInt(props.getProperty(key));
	}
	
	/**
	 * Returns an integer property. If not found or not parseable it returns the default value.
	 * 
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public int getIntProperty(String key, int defaultValue){
		int value = defaultValue;
		try
		{
			value = Integer.parseInt(getProperty(key));
		}
		catch(Exception e)
		{
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
	public void setBooleanProperty(String key, boolean value){
		props.setProperty(key, value?"1":"0");
	}

	/**
	 * Returns a Boolean property.
	 * Null if key not found. Default is false.
	 * 
	 * @param key
	 * @return
	 */
	public Boolean getBooleanProperty(String key){
		String value = getProperty(key);
		if (value == null)
		{
			return null;
		}
		else if (value.equals("1") || value.equals("true") || value.equals("yes") || value.equals("on"))
		{
			return Boolean.TRUE;
		}
		return Boolean.FALSE;
	}
	
	/**
	 * Returns a Boolean property or the given defaultValue.
	 * 
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public boolean getBooleanProperty(String key, boolean defaultValue){
		Boolean value = getBooleanProperty(key);
		if (value == null)
		{
			return defaultValue;
		}

		return value.booleanValue();
	}

	/**
	 * Sets a List of properties starting with key.
	 * 
	 * @param key
	 * @param list
	 * @return
	 */
	public void setListProperty(String key, List list){
		for (int i = 0; i < list.size(); i++) {
			Object element = list.get(i);
			setProperty(key+i, element);
		}
	}

	/**
	 * Gets a List of properties starting with key.
	 * 
	 * @param key
	 * @return
	 */
	public List getListProperty(String key){
		Set keys = props.keySet();
		List list = new ArrayList();
		for (Iterator iter = keys.iterator(); iter.hasNext();) {
			String element = (String) iter.next();
			if (element.startsWith(key)){
				list.add(props.getProperty(element));
			}
		}
		
		return list;
	}

	/**
	 * Sets a Map of properties starting with key.
	 * 
	 * @param key
	 * @param map
	 * @return
	 */
	public void setHashMapProperty(String key, Map map){
		Set keys = map.keySet();
		for (Iterator iter = keys.iterator(); iter.hasNext();) {
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
	public Map getHashMapProperty(String key){
		Set keys = props.keySet();
		Map map = new HashMap();
		for (Iterator iter = keys.iterator(); iter.hasNext();) {
			String element = (String) iter.next();
			if (element.startsWith(key)){
				map.put(element.substring(key.length()), props.getProperty(element));
			}
		}
		
		return map;
	}
	
	/**
	 * Removes a property.
	 * 
	 * @param key
	 */
	public void remove(String key){
		props.remove(key);
	}

	/**
	 * Returns the ini filename.
	 * 
	 * @return
	 */
	public String getInifile() {
		return inifile;
	}
}