/*
 * @(#)Filter.java
 *
 * Copyright (c) 2007 by dvb.matt, All Rights Reserved. 
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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.SwingConstants;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.JList;
import javax.swing.JScrollPane;

import net.sourceforge.dvb.projectx.gui.UISwitchListener;
import net.sourceforge.dvb.projectx.gui.CommonGui;
import net.sourceforge.dvb.projectx.gui.ColumnLayout;


import net.sourceforge.dvb.projectx.common.Keys;
import net.sourceforge.dvb.projectx.common.Common;
import net.sourceforge.dvb.projectx.common.Resource;
import net.sourceforge.dvb.projectx.common.JobCollection;

/**
 *
 */
public class FilterPanel extends JPanel {

	private String title = Resource.getString("General.CollectionProperties");

	private CPComboBoxIndexListener _ComboBoxIndexListener = new CPComboBoxIndexListener();
	private CPComboBoxItemListener _ComboBoxItemListener = new CPComboBoxItemListener();
	private CPCheckBoxListener _CheckBoxListener = new CPCheckBoxListener();
	private CPTextFieldListener _TextFieldListener = new CPTextFieldListener();
	private CPTextFieldKeyListener _TextFieldKeyListener = new CPTextFieldKeyListener();

	private JobCollection collection;

	private JPanel container;
	private JPanel tabPanel;
	private JList includeList;

		class Clock implements Runnable {
			private Thread clockThread = null;

			private int last_collection = 0;

			public void start()
			{
				if (clockThread == null)
				{
					clockThread = new Thread(this, "Clock_6");
					clockThread.setPriority(Thread.MIN_PRIORITY);
					clockThread.start();
				}
			}

			public void run()
			{
				Thread myThread = Thread.currentThread();

				while (clockThread == myThread)
				{
					update();

					try {

						Thread.sleep(1000);
					} catch (InterruptedException e) {}
				}
			}

			private void update()
			{
				if (Common.getCollection() == null || Common.getCollection().hashCode() == last_collection)
					return;

				last_collection = Common.getCollection().hashCode();

				updatePidList();
			}

			private void updatePidList()
			{
				setPIDs();
			}


			public void stop()
			{
				clockThread = null;
			}
		}


	/**
	 * Constructor
	 */
	public FilterPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

		panel.add(buildPidPanel());

		new Clock().start();

		add(panel);

		setVisible(true);
	}

	/**
	 *
	 */
	private boolean getBooleanProperty(String[] str)
	{
		boolean b = false;

		if (collection == null)
			return b;

		b = collection.getSettings().getBooleanProperty(str);

		return b;
	}

	/**
	 *
	 */
	private int getIntProperty(String[] str)
	{
		int value = 0;

		if (collection == null)
			return value;

		value = collection.getSettings().getIntProperty(str);

		return value;
	}

	/**
	 *
	 */
	private String getProperty(String[] str)
	{
		String obj = "";

		if (collection == null)
			return obj;

		obj = collection.getSettings().getProperty(str);

		return obj;
	}

	/**
	 *
	 */
	private void setBooleanProperty(String str, boolean b)
	{
		if (collection == null)
			return;

		collection.getSettings().setBooleanProperty(str, b);

	}

	/**
	 *
	 */
	private void setProperty(String str1, String str2)
	{
		if (collection == null)
			return;

		collection.getSettings().setProperty(str1, str2);

	}

	/**
	 *
	 */
	private void setProperty(String str, Object obj)
	{
		if (collection == null)
			return;

		collection.getSettings().setProperty(str, obj);

	}

	/**
	 *
	 */
	private Object[] getPIDs()
	{
		getCollection();

		if (collection == null)
			return (new Object[0]);

		return collection.getPIDs();
	}

	/**
	 *
	 */
	private void setPIDs()
	{
		if (includeList != null)
			includeList.setListData(getPIDs());
	}

	/**
	 *
	 */
	private void getCollection()
	{
		collection = Common.getCollection();
	}

	/**
	 *
	 */
	protected JPanel buildPidPanel()
	{
		includeList = new JList();
		includeList.setToolTipText(Resource.getString("CollectionPanel.PidList.Tip2"));
		includeList.setBackground(new Color(190, 225, 255));
		setPIDs();

		final JTextField includeField = new JTextField("");
		includeField.setPreferredSize(new Dimension(80, 25));
		includeField.setMaximumSize(new Dimension(80, 25));
		includeField.setEditable(true);
		includeField.setActionCommand("ID");
		includeField.setToolTipText(Resource.getString("CollectionPanel.PidList.Tip1"));

		includeField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				getCollection();

				if (collection == null)
					return;

				if (collection.isActive())
					return;

				String actName = e.getActionCommand();

				try {

					String value = includeField.getText();

					if (!value.equals(""))
					{
						if (!value.startsWith("0x")) 
							value = "0x" + Integer.toHexString(0x1FFF & Integer.parseInt(value));

						value = value.toUpperCase().replace('X', 'x');

						collection.addPID(value);
						includeList.setListData(collection.getPIDs());
						includeList.ensureIndexIsVisible(collection.getPIDs().length - 1);
					}

				} catch (NumberFormatException ne) {

					Common.setOSDErrorMessage(Resource.getString("cutlistener.wrongnumber"));
				}

				includeField.setText("");

				return;
			}
		});


		JScrollPane scrollList = new JScrollPane();
		scrollList.setPreferredSize(new Dimension(80, 96));
		scrollList.setMaximumSize(new Dimension(80, 96));
		scrollList.setViewportView(includeList);

		includeList.addMouseListener( new MouseAdapter() {
			public void mouseClicked(MouseEvent e)
			{
				getCollection();

				if (collection == null)
					return;

				if (e.getClickCount() >= 2)
				{
					Object[] val = includeList.getSelectedValues();

					collection.removePID(val);
					setPIDs();
				}
			}
		});


		JButton pids = new JButton(Resource.getString("CollectionPanel.transferPids1"));
		pids.setPreferredSize(new Dimension(80, 22));
		pids.setMaximumSize(new Dimension(80, 22));
		pids.setActionCommand("transferPIDs");
		pids.setToolTipText(Resource.getString("CollectionPanel.transferPids1.Tip"));
		pids.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				getCollection();

				if (collection == null)
					return;

				if (collection.isActive())
					return;

				String actName = e.getActionCommand();

				try {

					Object[] values = includeList.getSelectedValues();

					if (values.length > 0)
					{
						JobCollection new_collection = Common.addCollection(collection.getNewInstance());

						collection.removePID(values);

						new_collection.clearPIDs();
						new_collection.addPID(values);

						new_collection.determinePrimaryFileSegments();

						setPIDs();
					}

				} catch (Exception e2) {

					Common.setExceptionMessage(e2);
				}

				return;
			}
		});

		/**
		 *
		 */
		JPanel CL4 = new JPanel();
		CL4.setLayout(new ColumnLayout());

		CL4.add(includeField);
		CL4.add(Box.createRigidArea(new Dimension(1, 40)));
		CL4.add(pids);

		JPanel CL5 = new JPanel();
		CL5.setLayout(new BoxLayout(CL5, BoxLayout.X_AXIS));
		CL5.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), Resource.getString("CollectionPanel.PidList")));
		CL5.add(CL4);
		CL5.add(Box.createRigidArea(new Dimension(10, 1)));
		CL5.add(scrollList);

		JPanel panel = new JPanel();
		panel.setLayout( new GridLayout(1, 1) );
		panel.add(CL5);

		return panel;
	}


	/**
	 * 
	 */
	class CPCheckBoxListener implements ActionListener {
		public void actionPerformed(ActionEvent e)
		{
			String actName = e.getActionCommand();

			JCheckBox box = (JCheckBox) e.getSource();
			setBooleanProperty(actName, box.isSelected());
		}
	}

	/**
	 * 
	 */
	class CPComboBoxIndexListener implements ActionListener {
		public void actionPerformed(ActionEvent e)
		{
			String actName = e.getActionCommand();
			JComboBox box = (JComboBox) e.getSource();

			setProperty(actName, String.valueOf(box.getSelectedIndex()));
		}
	}

	/**
	 * 
	 */
	class CPComboBoxItemListener implements ActionListener {
		public void actionPerformed(ActionEvent e)
		{
			String actName = e.getActionCommand();

			// refuse additional actioncommand "comboBoxEdit" from jre > 1.4 ?
			if (actName.indexOf('.') < 0)
				return;

			JComboBox box = (JComboBox) e.getSource();

			setProperty(actName, box.getSelectedItem());
		}
	}

	/**
	 * 
	 */
	class CPTextFieldKeyListener extends KeyAdapter {
		public void keyReleased(KeyEvent e)
		{
			JTextField textfield = (JTextField) e.getSource();

			textfield.postActionEvent();
		}
	}

	/**
	 * 
	 */
	class CPTextFieldListener implements ActionListener {
		public void actionPerformed(ActionEvent e)
		{
			String actName = e.getActionCommand();
			JTextField textfield = (JTextField) e.getSource();

			setProperty(actName, textfield.getText());
		}
	}
}
