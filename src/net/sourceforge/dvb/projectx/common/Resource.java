/*
 * @(#)Resource.java - resource and i18n
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;

/**
 * Project-X resource and localization handling.
 * 
 * @author Peter Storch
 */
public class Resource {
	
	/** the prefix of all pjx resource files */
	private static final String PJX_RESOURCE_PREFIX = "pjxresources";

	/** current working directory */
	public static final String workdir = System.getProperty("user.dir");
	
	/** os dependent file separator */
	public static final String filesep = System.getProperty("file.separator");

	/** the users locale */
	private static Locale locale = null;

	/** the default resource bundle */
	private static ResourceBundle defaultResource = null;

	/** the resource bundle for the current users locale or language setting */
	private static ResourceBundle resource = null;
	
	/**
	 * Loads a resource bundle for the given locale.
	 * 
	 * @param locale
	 * @return ResourceBundle
	 */
	private static ResourceBundle loadResourceBundle(Locale locale) throws MissingResourceException
	{
		ResourceBundle newBundle = null;
		String resourceName = PJX_RESOURCE_PREFIX + "_" + locale.getLanguage() + ".properties";
		
		// first we try to find one in the current working directory
		try 
		{
			File file = new File(workdir + filesep + resourceName);
			if (file.exists() && file.canRead())
			{
				newBundle = new PropertyResourceBundle(new FileInputStream(file));
				return newBundle;
			}
		} 
		catch (Exception e) 
		{
			// shit happens, go on and try to find one in our jar file
		}
		
		try 
		{
			URL url = Resource.class.getClassLoader().getResource(resourceName);
			newBundle = new PropertyResourceBundle(url.openStream());
		} 
		catch (Exception e) 
		{
			throw new MissingResourceException("couldn't find " + resourceName, Resource.class.getName(), resourceName);
		}
		
		return newBundle;
	}
	
	static{
		// the default resource must be available
		defaultResource = loadResourceBundle(Locale.ENGLISH);
		try 
		{
			// now try to load the resource bundle form the users locale
			resource = loadResourceBundle(Locale.getDefault());
		} catch (MissingResourceException e) {
			// our fallback is english
			resource = defaultResource;
		}
	}
		
	/**
	 * Constructor of Resource.
	 */
	private Resource()
	{
		// singleton
	}
	
	/**
	 * Loads Language from ini file.
	 * 
	 * @param filename Name of the inifile.
	 */
	public static void loadLang(String inifile)
	{
		try 
		{
			if (new File(inifile).exists())
			{
				BufferedReader inis = new BufferedReader(new FileReader(inifile));
				String line=null; 
			
				while ((line = inis.readLine()) != null)
				{
					// look for the line with the language information
					if (line.startsWith("lang="))
					{
						String lang = line.substring(5);
						locale=new Locale(lang, "");
						try {
							resource = loadResourceBundle(locale);
						} catch (MissingResourceException e) {
							// our fallback is english
							resource = defaultResource;
						}
						
						// we have found what we need, stop reading this file
						break;
					}
				}
				inis.close();
			}
		}
		catch (IOException e1)
		{
			//DM25072004 081.7 int07 add
			System.out.println(resource.getString("msg.loadlang.error") + " " + e1);
		}
	}
		
	/**
	 * Saves the language information.
	 * 
	 * @param pw
	 */
	public static void saveLang(PrintWriter pw)
	{
		if (locale != null)
		{
			pw.println("// language");
			pw.println("lang="+locale);
		}
	}
	
	/**
	 * Gets a String from the Resource file. If the key is not found, the key
	 * itself is returned as text.
	 * 
	 * @param key
	 * @return String
	 */
	public static String getString(String key)
	{
		String text = null;
		try 
		{
			text = resource.getString(key);
		} 
		catch (MissingResourceException e) 
		{
			try 
			{
				// fallback to defaultResource
				text = defaultResource.getString(key);
			} 
			catch (MissingResourceException e2) 
			{
				System.out.println("ResourceKey '" + key + "' not found in pjxresources");
			}
		}
		
		// use key as text as fallback
		if (text == null)
		{
			text = "?" + key + "?";
		}
		
		return text;
	}

	/**
	 * Returns a resource String as a String Array of lines.
	 * 
	 * @return String[]
	 */
	public static String[] getStringByLines(String key)
	{
		List lines = new ArrayList();
		StringTokenizer st = new StringTokenizer(getString(key), "\n");
		while (st.hasMoreTokens())
		{
			lines.add(st.nextToken());
		}
		
		return (String[])lines.toArray(new String[0]);
	}

	/**
	 * Gets a String from the resource and inserts optional arguments.
	 * 
	 * @param key
	 * @param args
	 * @return
	 */
	public static String getString(String key, Object args[])
	{
		return MessageFormat.format(getString(key), args);
	}
	
	/**
	 * Gets a String from the resource and inserts an optional argument.
	 * 
	 * @param key
	 * @param args
	 * @return
	 */
	public static String getString(String key, Object arg)
	{
		return MessageFormat.format(getString(key), new Object[]{arg});
	}

	/**
	 * Gets a String from the resource and inserts two optional arguments.
	 * 
	 * @param key
	 * @param arg1
	 * @param arg2
	 * @return
	 */
	public static String getString(String key, Object arg1, Object arg2)
	{
		return MessageFormat.format(getString(key), new Object[]{arg1, arg2});
	}

	/**
	 * Gets a String from the resource and inserts three optional arguments.
	 * 
	 * @param key
	 * @param arg1
	 * @param arg2
	 * @param arg3
	 * @return
	 */
	public static String getString(String key, Object arg1, Object arg2, Object arg3)
	{
		return MessageFormat.format(getString(key), new Object[]{arg1, arg2, arg3});
	}

	/**
	 * Gets a String from the resource and inserts four optional arguments.
	 * 
	 * @param key
	 * @param arg1
	 * @param arg2
	 * @param arg3
	 * @param arg4
	 * @return
	 */
	public static String getString(String key, Object arg1, Object arg2, Object arg3, Object arg4)
	{
		return MessageFormat.format(getString(key), new Object[]{arg1, arg2, arg3, arg4});
	}

	/**
	 * Gets a String from the resource and inserts five optional arguments.
	 * 
	 * @param key
	 * @param arg1
	 * @param arg2
	 * @param arg3
	 * @param arg4
	 * @param arg5
	 * @return
	 */
	public static String getString(String key, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5)
	{
		return MessageFormat.format(getString(key), new Object[]{arg1, arg2, arg3, arg4, arg5});
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
	public static final void localize(AbstractButton button, String key) {
		String text = getString(key);
		
		int pos = text.indexOf('&');
		if (pos != -1)
		{
			char mnemonic = text.charAt(pos+1);
			button.setMnemonic(mnemonic);
			text = text.substring(0, pos) + text.substring(pos+1);
		}
		button.setText(text);
	}

	/**
	 * Builds the Language Menu.
	 * 
	 * @return JMenu
	 */
	public static JMenu buildLanguageMenu()
	{
		ActionListener listener = new LangListener();
		
		JMenu langMenu = new JMenu();
		localize(langMenu, "language.menu");
		
		ButtonGroup group = new ButtonGroup();

		JRadioButtonMenuItem item_sys = new JRadioButtonMenuItem();
		localize(item_sys, "language.system");
		item_sys.addActionListener(listener);
		item_sys.setSelected(locale == null);
		item_sys.setActionCommand("system");
		langMenu.add(item_sys);
		group.add(item_sys);

		langMenu.addSeparator();

		Locale[] locales = getAvailableLocales();
		for (int i = 0; i < locales.length; i++) {
			Locale item = locales[i];
			JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem(item.getLanguage());
			menuItem.addActionListener(listener);
			if (locale != null)
			{
				menuItem.setSelected(item.getLanguage().equals(locale.getLanguage()));
			}
			menuItem.setActionCommand(item.getLanguage());
			langMenu.add(menuItem);
			group.add(menuItem);
		}

		return langMenu;
	}
	
	/**
	 * Inner class LangListener. Handles the actions from the Language Menu.
	 */
	private static class LangListener implements ActionListener
	{

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
		public void actionPerformed(ActionEvent event) {
			String action = event.getActionCommand();
			if (action.equals("system"))
			{
				locale = null;
			}
			else
			{
				locale = new Locale(action, "", "");
			}
			JOptionPane.showMessageDialog(null, Resource.getString("msg.new.language"), Resource.getString("msg.infomessage"), JOptionPane.INFORMATION_MESSAGE);
		}
		
	}

	/**
	 * Returns the available Locales for pjxresources.
	 * 
	 * @return Locale[]
	 */
	private static Locale[] getAvailableLocales() {
		Set locales = new HashSet();
		String defLang = Locale.getDefault().getLanguage();

		try {
			// we know we have an english resource, so first find this one to find the location
			URL url = ClassLoader.getSystemResource(PJX_RESOURCE_PREFIX + "_en.properties");
			if (url != null) {
				URLConnection urlc = null;
				urlc = url.openConnection();
				
				// If the resources are located in a JAR file, we need this
				// version to get the available locales.
				if (urlc != null && urlc instanceof JarURLConnection) {
					JarURLConnection jurlc = (JarURLConnection) urlc;
					addAvailableLocalesFromJar(locales, jurlc);
				}
				// .. else if the resources are in the file system, we use the
				// default version to get the available locales.
				else {
					File enFile = new File(url.getFile());
					File dir = enFile.getParentFile();
					addAvailableLocalesFromFileSystem(locales, dir);
				}
			} else {
				System.err.println("Couldn't find \"" + PJX_RESOURCE_PREFIX
						+ "\"*.properties");
			}
			
			// also look into the current working directory for additional resource files
			File workDirFile = new File(workdir);
			addAvailableLocalesFromFileSystem(locales, workDirFile);
		} catch (Exception e) {
			System.out.println(e);
		}
		
		return (Locale[])locales.toArray(new Locale[0]);
	}
	
	/**
	 * Adds available Locales from the file system.
	 * 
	 * @param locales
	 * @param dir
	 */
	private static void addAvailableLocalesFromFileSystem(Set locales, File dir) {
		File[] files = dir.listFiles();
		if (files != null) {
			for (int i = 0; i < files.length; i++) {
				File file = files[i];
				if (file.isFile() && file.getName().startsWith(PJX_RESOURCE_PREFIX))
				{
					try {
						String code = file.getName();
						int pos = code.indexOf('_');
						if (pos != -1) {
							code = code.substring(pos + 1);
						}
						pos = code.indexOf('.');
						if (pos != -1)
						{
							code = code.substring(0, pos);
						}
						Locale locale = new Locale(code, "");
						locales.add(locale);
					} catch (Exception e) {
						System.out.println(e);
					}
				}
			}
		}
	}

	/**
	 * Adds available Locales from a Jar file location
	 * 
	 * @param locales
	 * @param jurlc
	 */
	private static void addAvailableLocalesFromJar(Set locales, JarURLConnection jurlc) {
		JarFile jarf = null;
		try {
			jarf = jurlc.getJarFile();
		} catch (Exception e) {
			System.out.println(e);
		}
		if (jarf != null) {
			for (Enumeration en = jarf.entries(); en.hasMoreElements();) {
				JarEntry jare = (JarEntry) en.nextElement();
				String name = jare.getName();
				if (name.startsWith(PJX_RESOURCE_PREFIX)) {
					String code = name.substring(0,
							name.length() - ".properties".length());
					int pos = code.indexOf('_');
					if (pos != -1) {
						code = code.substring(pos + 1);
					}
					pos = code.indexOf('.');
					if (pos != -1)
					{
						code = code.substring(0, pos);
					}
					Locale locale = new Locale(code, "");
					locales.add(locale);
				}
			}
		}
	}

	/**
	 * Returns a resource (e.g. from the jar file) as an URL.
	 * 
	 * @param resource the name of the resource
	 * @return URL
	 */
	public static URL getResourceURL(String resource)
	{
		try
		{
			String filename = workdir + filesep + resource;
			File file = new File(filename);
			if (file.exists() && file.canRead())
			{
				return file.toURL();
			}
		}
		catch(Exception e)
		{
			// ignore it, it was just a try to get this resource from the filesystem
		}
		
		// for the classloader we need to replace all backslashes to forward slashes.
		// this is only necessary on windows systems and doesn't harm others
		resource = resource.replace('\\', '/');
		
		// ok, not founde in the filesystem, now try the classloader
		return Resource.class.getClassLoader().getResource(resource);
	}

	/**
	 * Returns a resource (e.g. from the jar file) as an URL.
	 * 
	 * @param resourceName the name of the resource
	 * @return URL
	 */
	public static URL getLocalizedResourceURL(String path, String resourceName)
	{
		Locale usedLocale = null;
		if (locale != null)
		{
			usedLocale = locale;
		}
		else
		{
			usedLocale = Locale.getDefault();
		}

		String localizedResource = path + filesep + usedLocale.getLanguage() + filesep + resourceName;
		
		URL url = getResourceURL(localizedResource);
		if (url != null)
		{
			return url;
		}

		// there is no localized version of this file, try the default version
		return getResourceURL(path + filesep + resourceName);
	}

	/**
	 * Loads an image as ImageIcon.
	 * 
	 * @param iconName
	 * @return ImageIcon
	 */
	public static ImageIcon loadIcon(String iconName)
	{
		return new ImageIcon(getResourceURL(iconName));
	}
	
}
