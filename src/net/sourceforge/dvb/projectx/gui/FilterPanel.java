/*
 * @(#)Filter.java
 *
 * Copyright (c) 2007-2011 by dvb.matt, All Rights Reserved. 
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

	private CPComboBoxIndexListener _ComboBoxIndexListener = new CPComboBoxIndexListener();
	private CPComboBoxItemListener _ComboBoxItemListener = new CPComboBoxItemListener();
	private CPCheckBoxListener _CheckBoxListener = new CPCheckBoxListener();
	private CPTextFieldListener _TextFieldListener = new CPTextFieldListener();
	private CPTextFieldKeyListener _TextFieldKeyListener = new CPTextFieldKeyListener();

	private JobCollection collection;

	private JPanel container;
	private JPanel tabPanel;
	private JList includeList;

	private boolean actionDenied = false;
	private boolean hasChanged = false;

	private String[][] objects = {
		Keys.KEY_WriteOptions_writeVideo,
		Keys.KEY_WriteOptions_writeAudio,
		Keys.KEY_OptionHorizontalResolution,
		Keys.KEY_OptionDAR,
		Keys.KEY_Streamtype_MpgVideo,
		Keys.KEY_Streamtype_MpgAudio,
		Keys.KEY_Streamtype_Ac3Audio,
		Keys.KEY_Streamtype_PcmAudio,
		Keys.KEY_Streamtype_Teletext,
		Keys.KEY_Streamtype_Subpicture,
		Keys.KEY_Streamtype_Vbi,
		Keys.KEY_useAutoPidFilter
	};

	private JComboBox combobox_34;
	private JComboBox combobox_24;

	private JCheckBox[] box;
	private JTextField language_code;


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
			if (Common.getCollection() == null)
				return;

			if (Common.getCollection().hashCode() == last_collection)
				if (hasChanged && collection.hasSettings())
					return;
				else
					hasChanged = false;

			last_collection = Common.getCollection().hashCode();

			updatePidList();
			updateProperties();
		}

		private void updatePidList()
		{
			setPIDs();
		}

		private void updateProperties()
		{
			setCollectionProperties();
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
		box = new JCheckBox[objects.length];

		for (int i = 0; i < objects.length; i++)
		{
			box[i] = new JCheckBox(Resource.getString(objects[i][0]));
			box[i].setToolTipText(Resource.getString(objects[i][0] + Keys.KEY_Tip));
			box[i].setActionCommand(objects[i][0]);
			box[i].addActionListener(_CheckBoxListener);
		}

		combobox_34 = new JComboBox(Keys.ITEMS_ExportHorizontalResolution);
		combobox_24 = new JComboBox(Keys.ITEMS_ExportDAR);

		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

		panel.add(buildPidPanel());
		panel.add(buildLimitPanel());
		panel.add(buildWritePanel());
		panel.add(buildStreamtypePanel());

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
		if (!performAction())
			return;

		collection.getSettings().setBooleanProperty(str, b);
	}

	/**
	 *
	 */
	private void setProperty(String str1, String str2)
	{
		if (!performAction())
			return;

		collection.getSettings().setProperty(str1, str2);
	}

	/**
	 *
	 */
	private void setProperty(String str, Object obj)
	{
		if (!performAction())
			return;

		collection.getSettings().setProperty(str, obj);
	}

	/**
	 *
	 */
	private boolean performAction()
	{
		boolean b = true;

		if (actionDenied)
			return !b;

		getCollectionAndSettings();

		if (collection == null)
			return !b;

		hasChanged = b;

		return b;
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
	private void setCollectionProperties()
	{
		actionDenied = true;

		getCollection();

		for (int i = 0; i < box.length; i++)
			box[i].setSelected(getBooleanProperty(objects[i]));

		combobox_34.setSelectedItem(getProperty(Keys.KEY_ExportHorizontalResolution));
		combobox_24.setSelectedIndex(getIntProperty(Keys.KEY_ExportDAR));
		language_code.setText(getProperty(Keys.KEY_LanguageFilter));

		actionDenied = false;
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
	private void getCollectionAndSettings()
	{
		getCollection();

		if (collection != null && !collection.hasSettings())
			collection.setSettings(Common.getSettings());
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


	//	JButton pids = new JButton(Resource.getString("CollectionPanel.transferPids1"));
		JButton pids = new JButton(CommonGui.loadIcon("trans.gif"));
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
		CL5.setBorder(BorderFactory.createTitledBorder(Resource.getString("CollectionPanel.PidList")));
//		CL5.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), Resource.getString("CollectionPanel.PidList")));
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
	protected JPanel buildLimitPanel()
	{
		JPanel panel_1 = new JPanel();
		panel_1.setLayout(new ColumnLayout());
		panel_1.setBorder( BorderFactory.createTitledBorder("Filter Options"));

		panel_1.add(box[11]);

		/**
		 *
		 */
		JPanel CL2 = new JPanel();
		CL2.setLayout(new BoxLayout(CL2, BoxLayout.X_AXIS));

		box[2].setPreferredSize(new Dimension(110, 20));
		box[2].setMaximumSize(new Dimension(110, 20));
		CL2.add(box[2]);  


		combobox_34 = new JComboBox(Keys.ITEMS_ExportHorizontalResolution);
		combobox_34.setMaximumRowCount(7);
		combobox_34.setPreferredSize(new Dimension(90, 20));
		combobox_34.setMaximumSize(new Dimension(90, 20));
		combobox_34.setActionCommand(Keys.KEY_ExportHorizontalResolution[0]);
		combobox_34.setEditable(true);
		combobox_34.addActionListener(_ComboBoxItemListener);
		CL2.add(combobox_34);

		panel_1.add(CL2);

		/**
		 *
		 */
		JPanel CL3 = new JPanel();
		CL3.setLayout(new BoxLayout(CL3, BoxLayout.X_AXIS));

		box[3].setPreferredSize(new Dimension(80, 20));
		box[3].setMaximumSize(new Dimension(80, 20));
		CL3.add(box[3]);  

		combobox_24 = new JComboBox(Keys.ITEMS_ExportDAR);
		combobox_24.setMaximumRowCount(7);
		combobox_24.setPreferredSize(new Dimension(120, 20));
		combobox_24.setMaximumSize(new Dimension(120, 20));
		combobox_24.setActionCommand(Keys.KEY_ExportDAR[0]);
		combobox_24.addActionListener(_ComboBoxIndexListener);
		CL3.add(combobox_24);

		panel_1.add(CL3);

		panel_1.add(Box.createRigidArea(new Dimension(1, 10)));

		JButton revert = new JButton(Resource.getString("FilterPanel.ResetAll"));
		revert.setPreferredSize(new Dimension(180, 24));
		revert.setMaximumSize(new Dimension(180, 24));
		revert.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				if (collection != null)
					collection.setSettings(null);
			}
		});

		panel_1.add(revert);


		JPanel panel = new JPanel();
		panel.setLayout( new GridLayout(1, 1) );
		panel.add(panel_1);

		return panel;
	}

	/**
	 *
	 */
	protected JPanel buildWritePanel()
	{
		JPanel panel_1 = new JPanel();
		panel_1.setLayout(new ColumnLayout());
		panel_1.setBorder( BorderFactory.createTitledBorder(Resource.getString("ExportPanel.WriteOptions")) );
		panel_1.setToolTipText(Resource.getString("ExportPanel.WriteOptions.Tip"));

		panel_1.add(box[0]);
		panel_1.add(box[1]);

		panel_1.add(Box.createRigidArea(new Dimension(1, 25)));

		JButton more = new JButton(Resource.getString("FilterPanel.MoreSettings"));
		more.setPreferredSize(new Dimension(180, 24));
		more.setMaximumSize(new Dimension(180, 24));
		more.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				if (collection != null)
					CommonGui.getCollectionProperties().open(Common.getCollection(), Common.getActiveCollection());
			}
		});

		panel_1.add(more);


		JPanel panel = new JPanel();
		panel.setLayout( new GridLayout(1, 1) );
		panel.add(panel_1);

		return panel;
	}

	/**
	 *
	 */
	protected JPanel buildStreamtypePanel()
	{
		JPanel idPanel = new JPanel();
		idPanel.setBorder(BorderFactory.createTitledBorder(Resource.getString("ExportPanel.StreamtypePanel")));
		idPanel.setLayout(new BoxLayout(idPanel, BoxLayout.X_AXIS));
		idPanel.setToolTipText(Resource.getString("ExportPanel.StreamtypePanel.Tip"));

		JPanel panel_1 = new JPanel();
		panel_1.setLayout(new ColumnLayout());

		for (int i = 4; i < 8; i++)
			panel_1.add(box[i]);

		JPanel panel_2 = new JPanel();
		panel_2.setLayout(new ColumnLayout());

		for (int i = 8; i < 11; i++)
			panel_2.add(box[i]);

		language_code = new JTextField(Common.getSettings().getProperty(Keys.KEY_LanguageFilter));
		language_code.setPreferredSize(new Dimension(100, 22));
		language_code.setMaximumSize(new Dimension(100, 22));
		language_code.setToolTipText(Resource.getString(Keys.KEY_LanguageFilter[0] + Keys.KEY_Tip));
		language_code.setEditable(true);
		language_code.setActionCommand(Keys.KEY_LanguageFilter[0]);
		language_code.addActionListener(_TextFieldListener);
		language_code.addKeyListener(_TextFieldKeyListener);
		panel_2.add(language_code);

		idPanel.add(panel_1);
		idPanel.add(panel_2);


		JPanel panel = new JPanel();
		panel.setLayout( new GridLayout(1, 1) );
		panel.add(idPanel);

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
