/*
 * @(#)GOPEditor
 *
 * Copyright (c) 2008 by dvb.matt, All Rights Reserved.
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

import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.table.TableModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import net.sourceforge.dvb.projectx.common.Common;

public class GOPEditor extends JDialog {

	private StreamObject streamobject;
	private ArrayList sourcedata = new ArrayList();
	private ArrayList sinkdata = new ArrayList();
	private JList sourcelist = new JList();
	private JList sinklist = new JList();
	private JTable tableView;
	private JTextField textField;

	private Thread thread;

	private byte[] data = null;
	private long[][] pts_indices = null;

	private int s1 = 12, s2 = 9;

	private boolean stopCount = false;
	private int cnt = 0;

	private final int FRAME = 0;
	private final int SEQUENCE = 0xB3;
	private final int SEQUENCE_END = 0xB7;
	private final int GOP = 0xB8;
	private final int USER_DATA = 0xB2;
	private final int EXTENSION = 0xB5;

	private double lastFrameRate = -1;

	private Object[][] ObjectTable = new Object[s1][s2];


	/**
	 *
	 */
    public GOPEditor(JFrame frame)
	{
		super(frame, "GOP Editor", true);

		addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				close();
			}
		});

		JPanel container = new JPanel();
		container.setLayout(new GridLayout(1,2));

		setBounds(200, 100, 770, 500);

		container.add(buildPanel1());

		getContentPane().add(container);
	}

	/**
	 *
	 */
	protected JPanel buildPanel1()
	{
		JPanel panel1 = new JPanel();
		panel1.setLayout(new BoxLayout(panel1, BoxLayout.X_AXIS));

		JButton undoButton = new JButton("Reset All");
		undoButton.setMnemonic('o');
		undoButton.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				reload();
			}
		});

		JButton moveButton = new JButton("copy chunk");
		moveButton.setMnemonic('c');
		moveButton.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				copy();
			}
		});

		JButton removeButton = new JButton("remove chunk(s)");
		removeButton.setMnemonic('r');
		removeButton.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				remove();
			}
		});

		JButton checkButton = new JButton("check changes");
		checkButton.setMnemonic('e');
		checkButton.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				check();
			}
		});

		JButton saveButton = new JButton("save & close");
		saveButton.setMnemonic('a');
		saveButton.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				save();
			}
		});

		JButton closeButton = new JButton("close unchanged");
		closeButton.setMnemonic('u');
		closeButton.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				close();
			}
		});

		panel1.add(undoButton);
		panel1.add(Box.createRigidArea(new Dimension(10, 1)));
		panel1.add(moveButton);
		panel1.add(Box.createRigidArea(new Dimension(10, 1)));
		panel1.add(removeButton);
		panel1.add(Box.createRigidArea(new Dimension(10, 1)));
		panel1.add(checkButton);
		panel1.add(Box.createRigidArea(new Dimension(10, 1)));
		panel1.add(saveButton);
		panel1.add(Box.createRigidArea(new Dimension(10, 1)));
		panel1.add(closeButton);

		JScrollPane scroll = new JScrollPane();
		scroll.setViewportView(sourcelist);

		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		panel.add(createTable(), BorderLayout.CENTER);
		panel.add(textField = new JTextField());
		panel.add(panel1);

		return panel;
	}

	/**
	 *
	 */
    protected JScrollPane createTable()
	{
		JScrollPane scrollpane;

        // final
        final String[] names = {
			"Chunk", "Start", "End", "t.Ref", "Delta(ms)", "ind.PTS", "Timecode", "Type", "ErrID"
		};

		DefaultTableCellRenderer renderer_1 = new DefaultTableCellRenderer();
		DefaultTableCellRenderer renderer_2 = new DefaultTableCellRenderer();

		renderer_1.setHorizontalAlignment(JLabel.RIGHT);
		renderer_2.setHorizontalAlignment(JLabel.CENTER);

        // Show colors by rendering them in their own color.
        DefaultTableCellRenderer colorRenderer1 = new DefaultTableCellRenderer() {
			Color redColor = new Color(255, 180, 180);
			String str;

			public void setValue(Object value)
			{
				setBackground(Color.white);

				setText("");

				if (value == null)
					return;

				str = value.toString();

				if (!str.equals("0")) //errID
				{
					setText(Integer.toHexString(Integer.parseInt(str)));
					setBackground(redColor);
				}
			}
        };

        colorRenderer1.setHorizontalAlignment(JLabel.CENTER);

        // Show colors by rendering them in their own color.
        DefaultTableCellRenderer colorRenderer2 = new DefaultTableCellRenderer() {
			Color cyanColor = new Color(220, 255, 255);
			Color greenColor = new Color(220, 255, 220);
			Color blueColor = new Color(220, 220, 255);
			Color yellowColor = new Color(255, 255, 220);
			Color magentaColor = new Color(255, 220, 255);
			Color redColor = new Color(255, 180, 180);
			String str;

			public void setValue(Object value)
			{
				setBackground(Color.white);

				if (value == null)
				{
					setText("");
					return;
				}

				str = value.toString();
				setText(str);

				if (str.indexOf("Frame I") >= 0)
					setBackground(greenColor);

				else if (str.indexOf("Frame P") >= 0)
					setBackground(blueColor);

				else if (str.indexOf("Frame B") >= 0)
					setBackground(yellowColor);

				else if (str.indexOf("Sequence") >= 0)
					setBackground(cyanColor);

				else if (str.indexOf("GOP") >= 0)
					setBackground(magentaColor);

				if (str.indexOf("!") >= 0) //error
					setBackground(redColor);

			//	else if (str.indexOf("Frame D") >= 0)
			//		setBackground(whiteColor);
			}
        };

        colorRenderer2.setHorizontalAlignment(JLabel.LEFT);

        // Show colors by rendering them in their own color.
        DefaultTableCellRenderer colorRenderer3 = new DefaultTableCellRenderer() {
			Color redColor = new Color(255, 180, 180);
			String str;
			double val;

			public void setValue(Object value)
			{
				setBackground(Color.white);

				if (value == null)
				{
					setText("");
					return;
				}

				str = value.toString();
				setText(str);

				val = Double.parseDouble(str);

				if (Math.abs(val * 90.0) > getLastFrameRate() / 2.0)
					setBackground(redColor);
			}
        };

        colorRenderer3.setHorizontalAlignment(JLabel.CENTER);

        // Show colors by rendering them in their own color.
        DefaultTableCellRenderer colorRenderer4 = new DefaultTableCellRenderer() {
			Color redColor = new Color(255, 180, 180);
			String str;

			public void setValue(Object value)
			{
				setBackground(Color.white);

				setText("");

				if (value == null)
					return;

				str = value.toString();
				setText(str);

				if (str.indexOf("e") >= 0) //tRef
					setBackground(redColor);
			}
        };

        colorRenderer4.setHorizontalAlignment(JLabel.CENTER);

        // Create a model of the data.
        TableModel dataModel = new AbstractTableModel() {
			String str;

            public int getColumnCount()
			{
				return names.length;
			}

            public int getRowCount()
			{
				return ObjectTable.length;
			}

            public Object getValueAt(int row, int col)
			{
				return ObjectTable[row][col];
			}

            public String getColumnName(int column)
			{
				return names[column];
			}

            public Class getColumnClass(int c)
			{
				Object obj = getValueAt(0, c);

				if (obj == null)
					return String.class;

				return obj.getClass();
			}

            public boolean isCellEditable(int row, int col)
			{
				//return false;
				return getColumnClass(col) == String.class;
			}

            public void setValueAt(Object aValue, int row, int column)
			{
				ObjectTable[row][column] = aValue;
			}
		};

        // Create the table
        tableView = new JTable(dataModel);

		tableView.setRowHeight(20);
		tableView.setGridColor(new Color(220, 220, 220));

		tableView.setSelectionBackground(new Color(220, 220, 255));
		tableView.setSelectionForeground(Color.black);

		tableView.getColumn(names[0]).setCellRenderer(renderer_2);
		tableView.getColumn(names[0]).setMaxWidth(45);

		tableView.getColumn(names[1]).setCellRenderer(renderer_1);
		tableView.getColumn(names[1]).setMaxWidth(70);

		tableView.getColumn(names[2]).setCellRenderer(renderer_1);
		tableView.getColumn(names[2]).setMaxWidth(70);

		tableView.getColumn(names[3]).setMaxWidth(50);
        tableView.getColumn(names[3]).setCellRenderer(colorRenderer4);

		tableView.getColumn(names[4]).setCellRenderer(colorRenderer3);
		tableView.getColumn(names[4]).setMaxWidth(70);

		tableView.getColumn(names[5]).setCellRenderer(renderer_2);
		tableView.getColumn(names[5]).setMaxWidth(100);

		tableView.getColumn(names[6]).setCellRenderer(renderer_2);
		tableView.getColumn(names[6]).setMaxWidth(100);

		tableView.getColumn(names[7]).setMaxWidth(280);
        tableView.getColumn(names[7]).setCellRenderer(colorRenderer2);

		tableView.getColumn(names[8]).setMaxWidth(50);
        tableView.getColumn(names[8]).setCellRenderer(colorRenderer1);

        tableView.sizeColumnsToFit(JTable.AUTO_RESIZE_LAST_COLUMN);

		tableView.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e)
			{
				if (e.getClickCount() < 1)
					return;

				int row = tableView.getSelectedRow();

				// empty row
				if (row >= 0 && tableView.getValueAt(row, 0) == null)
					row = -1;

			}
		});

        scrollpane = new JScrollPane(tableView);

        return scrollpane;
    }

	/**
	 * 
	 */
	private void close()
	{ 
		dispose();
	}

	/**
	 *
	 */
	private void remove()
	{
		int[] rows = tableView.getSelectedRows();

		for (int i = rows.length - 1; i >= 0; i--) 
			sourcedata.remove(rows[i]);

		setSourceTable();
	}

	/**
	 *
	 */
	private void copy()
	{
		int row = tableView.getSelectedRow();

		if (row < 0)
			return;

		streamobject = (StreamObject) sourcedata.get(row);
		
		if (streamobject instanceof Frame)
		{
			Frame frame1 = (Frame) streamobject;
			Frame frame2 = new Frame();

			frame2.copy(frame1.getType(), frame1.getDataType(), frame1.getOffset(), frame1.getLength(), frame1.getChunk() + 1000);
			frame2.copy2(frame1.getFrameType(), frame1.getTempRef());
		
			sourcedata.add(row + 1, frame2);

			setSourceTable();
		}
	}

	/**
	 *
	 */
	private void reload()
	{
		editGOP(data, pts_indices);
	}

	/**
	 *
	 */
	private void check()
	{
		
	}

	/**
	 * 
	 */
	private byte[] save()
	{ 
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		try {

			int pos, len;

			for (int i = 0, j = sinkdata.size(); i < j; i++)
			{
				streamobject = (StreamObject) sinkdata.get(i);

				pos = streamobject.getOffset();
				len = streamobject.getLength();

				if (streamobject instanceof Frame)
				{
					Frame frame = (Frame) streamobject;

					int val = frame.getTempRef();

					if (val >= 0)
						setTempRef(data, pos, val);
				}

				out.write(data, pos, len);
			}

			out.flush();
			out.close();

			JOptionPane.showMessageDialog(this, "export done..");

		} catch (Exception e) {
			System.out.println("error");
		}

		return out.toByteArray();
	}

	/**
	 *
	 */
	private void setTempRef(byte[] array, int offs, int val)
	{
		array[offs + 4] = (byte)(0xFF & val>>2);
		array[offs + 5] = (byte)((0xC0 & val<<6) | (0x3F & array[offs + 5]));
	}

	/**
	 *
	 */
	private void updateTable(Object[][] objects)
	{
		ObjectTable = objects == null ? new Object[s1][s2] : objects;

		tableView.clearSelection();
		tableView.revalidate();
		tableView.repaint();
	}

	/**
	 *
	 */
	private void setTextField(String str)
	{
		textField.setText(str);
	}

	/**
	 *
	 */
	private String getTextField()
	{
		return textField.getText();
	}

	/**
	 *
	 */
	private void setSourceTable()
	{
		int size = sourcedata.size();

		ObjectTable = new Object[size][s2];
		StreamObject obj;

		for (int i = 0; i < size; i++)
		{
			obj = (StreamObject) sourcedata.get(i);

			ObjectTable[i] = obj.getObjectTable();
		}

		tableView.clearSelection();
		tableView.revalidate();
		tableView.repaint();
	}

	/**
	 * entry point
	 */
    public byte[] editGOP(byte[] _data, long[][] _pts_indices)
	{
		int pos = 0;
		int chunk = 0;
		int last_id = -1;
		int lastSelect = -1;

		int sliceCount = 0;

		data = _data;
		pts_indices = _pts_indices;

		setTextField("");
		sourcedata.clear();
		sinkdata.clear();

		for (int i = 0, j = 0, slice_id = -1, k = data.length - 3, id; i < k; i++)
		{
			if (data[i] != 0 || data[i + 1] != 0 || data[i + 2] != 1)
				continue;

			id = 0xFF & data[i + 3];

			if (id > FRAME && id < 0xB0)
			{
				if (id == slice_id)
					sliceCount |= 0x10000;

				else if (id < slice_id)
					sliceCount |= 0x20000;

				else if (id > slice_id + 1)
					sliceCount |= 0x40000;

				sliceCount++;
			}

			switch (id)  //include extensions 0xB5 in main header length
			{
			case FRAME:  
			case SEQUENCE:
			case SEQUENCE_END:
			case GOP:
		//	case USER_DATA:

				setSliceInfo(sliceCount);

				if (id == FRAME)
				{
					sliceCount = 0;
					slice_id = id;
				}

				if (last_id < 0)
				{
					last_id = id;
					j = i;
					continue;
				}

				streamobject = setObject(last_id, pos, data, j, i - j, chunk++);

				for (int p = pts_indices[0].length - 1; p >= 0; p--)
				{
					if (lastSelect != p && pts_indices[1][p] <= j && streamobject != null)
					{
						streamobject.setPTSValue(pts_indices[0][p]);
						lastSelect = p;
						break;
					}
				}

				last_id = id;
				j = i;
				break;

			default:

				if (id > FRAME && id < 0xB0)
					slice_id = id;

				continue;
			}
		}

		setSliceInfo(sliceCount); //last frame update
		updatePTSInfo();
		validatePTSInfo();
		validateSequence();
		getErrors();

		setSourceTable();

		setSourceList();
		setSinkList();

// edit here - modal dialog
		this.show();

		return data;
	}

	/**
	 *
	 */
	private void setSliceInfo(int val)
	{
		if (streamobject != null && streamobject.getType() == 0) // is next frame
		{
			String str = streamobject.getDataType() + " - Slices: " + String.valueOf(0xFFFF & val);

			if ((0x10000 & val) != 0)
				str += " dupl! ";

			if ((0x20000 & val) != 0)
				str += " sequ! "; 

			if ((0x40000 & val) != 0)
				str += " miss! ";

			streamobject.setError(streamobject.getError() | val>>>16);
			streamobject.setDataType(str);
		}
	}

	/**
	 *
	 */
	private void updatePTSInfo()
	{
		long pts = -1;

		// set PTS of first frame if it's empty
		for (int i = 0, j = sourcedata.size(); i < j; i++)
		{
			streamobject = (StreamObject) sourcedata.get(i);

			if (streamobject.getPTSValue() != -1)
				pts = streamobject.getPTSValue();

			if (streamobject instanceof Frame)
			{
				if (streamobject.getPTSValue() == -1 && pts != -1)
					streamobject.setPTSValue(pts);

				break;
			}
		}
	}

	/**
	 *
	 */
	private void validatePTSInfo()
	{
		Frame frame;
		double zero_pts = -1, pts;
		int tRef;
		double tmp1, tmp2, framerate = getLastFrameRate();

		if (framerate < 0)
			return;

		for (int i = 0, j = sourcedata.size(); i < j; i++)
		{
			streamobject = (StreamObject) sourcedata.get(i);

			if (streamobject instanceof Frame)
			{
				frame = (Frame) streamobject;
				tRef = frame.getTempRef();
				pts = frame.getPTSValue();

				tmp1 = pts - (tRef * framerate);

				if (zero_pts < 0)
					zero_pts = tmp1;

				tmp2 = (pts - (tRef * framerate)) - zero_pts;

				if (Math.abs(tmp2) > framerate / 2.0)
					frame.setError(frame.getError() | 0x10);

				frame.setDelta(tmp2);

				continue;
			}
		}
	}

	/**
	 *
	 */
	private void validateSequence()
	{
		Frame frame;
		double framerate = getLastFrameRate();
		int tRef, tmp;
		int[] list = new int[sourcedata.size()];
		boolean b = false;

		Arrays.fill(list, -1);

		for (int i = 0, j = sourcedata.size(); i < j; i++)
		{
			streamobject = (StreamObject) sourcedata.get(i);

			if (streamobject instanceof Frame)
			{
				frame = (Frame) streamobject;
				tRef = frame.getTempRef();

				list[i] = tRef<<16 | i;

				continue;
			}
		}

		Arrays.sort(list);

		for (int i = 0, lastRef = -1, j = list.length; i < j; i++)
		{
			if (list[i] < 0)
				continue;

			tmp = 0xFFFF & list[i];
			tRef = list[i]>>>16;

			if (lastRef < 0)
				lastRef = tRef;

			else if (tRef != lastRef)
			{
				streamobject = (StreamObject) sourcedata.get(tmp);
				streamobject.setError(streamobject.getError() | 0x20);
			}

			lastRef++;
		}
	}

	/**
	 *
	 */
	public void getErrors()
	{
		int errors = 0;

		for (int i = 0, j = sourcedata.size(); i < j; i++)
		{
			streamobject = (StreamObject) sourcedata.get(i);

			errors |= streamobject.getError();
		}

		if ((0x7 & errors) != 0)
			setTextField(getTextField() + "Error Frame; ");

		if ((0x10 & errors) != 0)
			setTextField(getTextField() + "Error PTS Indication; ");

		if ((0x20 & errors) != 0)
			setTextField(getTextField() + "Error tRef's; ");
	}

	/**
	 *
	 */
	private void setLastFrameRate(double val)
	{
		lastFrameRate = val;
	}

	/**
	 *
	 */
	private double getLastFrameRate()
	{
		return lastFrameRate;
	}

	/**
	 *
	 */
	private void setSourceList()
	{
		sourcelist.setListData(sourcedata.toArray());
	}

	/**
	 *
	 */
	private void setSinkList()
	{
		sinklist.setListData(sinkdata.toArray());
	}

	/**
	 *
	 */
	private StreamObject setObject(int last_id, int pos, byte[] buf, int offs, int len, int chunk)
	{
		switch (last_id)
		{
		case FRAME:
			streamobject = new Frame();
			streamobject.set(pos, buf, offs, len, chunk);
			break;

		case SEQUENCE:
			streamobject = new SequenceHeader();
			streamobject.set(pos, buf, offs, len, chunk);
			break;

		case SEQUENCE_END:
			streamobject = new SequenceEnd();
			streamobject.set(pos, buf, offs, len, chunk);
			break;

		case GOP:
			streamobject = new GOPHeader();
			streamobject.set(pos, buf, offs, len, chunk);
			break;

		case USER_DATA:
			streamobject = new UserData();
			streamobject.set(pos, buf, offs, len, chunk);
			break;

		default:
			return null;
		}

		sourcedata.add(streamobject);

		return streamobject;
	}






	class Frame extends StreamObject {

		private int frametype = 0;
		private int tempref = -1;

		private String[] types = { "-", "I", "P", "B", "D", "-", "-", "-" };

		public Frame()
		{
			type = FRAME;
			datatype = "Frame ";

			setDataType(datatype);
		}

		public void copy2(int _frametype, int _tempref)
		{
			frametype = _frametype;
			tempref = _tempref;

			objecttable[3] = String.valueOf(getTempRef());
		}

		public void set(int pos, byte[] array, int offs, int len, int _chunk)
		{
			frametype = 7 & array[offs + 5]>>3;
			tempref = (0xFF & array[offs + 4])<<2 | (3 & array[offs + 5]>>6);

			setDataType(getDataType() + types[getFrameType()]);

			objecttable[3] = String.valueOf(getTempRef());

			setOffsets(pos, offs, len, _chunk);
		}

		public int getFrameType()
		{
			return frametype;
		}

		public int getTempRef()
		{
			return tempref;
		}

		public String toString()
		{
			return ("#" + chunk + " (" + getOffset() + "->" + (getOffset() + getLength()) + ")" + datatype + types[frametype] + " - tref: " + String.valueOf(tempref));
		}
	}

	class SequenceHeader extends StreamObject {

		private String[] aspratio = {"res.","1.000 (1:1)","0.6735 (4:3)","0.7031 (16:9)","0.7615 (2.21:1)","0.8055","0.8437","0.9375","0.9815","1.0255","1.0695","1.1250","1.1575","1.2015","res." };
		private String[] fps_table_str = {"forbidden fps","23.976fps","24fps","25fps","29.97fps","30fps","50fps","59.94fps","60fps","n.def.","n.def.","n.def.","n.def.","n.def.","n.def.","n.def."};
		private double[] fps_table = { -1, 90000.0 / 23.976, 90000.0 / 24.0, 90000.0 / 25.0, 90000.0 / 29.97, 90000.0 / 30.0, 90000.0 / 50.0, 90000.0 / 59.94, 90000.0 / 60.0, -1, -1, -1, -1, -1, -1, -1 };

		public SequenceHeader()
		{
			type = SEQUENCE;
			datatype = "SequenceHeader";

			setDataType(datatype);
		}

		public void set(int pos, byte[] array, int offs, int len, int _chunk)
		{
			String str = " ";
			str += String.valueOf((0xFF & array[offs + 4])<<4 | (0xF0 & array[offs + 5])>>>4);
			str += "*";
			str += String.valueOf((0xF & array[offs + 5])<<8 | (0xFF & array[offs + 6]));
			str += ", ";
			str += fps_table_str[0xF & array[offs + 7]];
			str += ", ";
			str += aspratio[0xF & array[offs + 7]>>>4];

			datatype += str;

			setDataType(datatype);

			setOffsets(pos, offs, len, _chunk);

			setLastFrameRate(fps_table[0xF & array[offs + 7]]);
		}
	}

	class GOPHeader extends StreamObject {

		private String[] drop = { "--,", "drop," };
		private String[] closed = { "--,", "closed," };
		private String[] broken = { "--", "broken" };

		public GOPHeader()
		{
			type = GOP;
			datatype = "GOPHeader";

			setDataType(datatype);
		}

		public void set(int pos, byte[] array, int offs, int len, int _chunk)
		{
			String str = " ";

			str += drop[1 & array[offs + 4]>>7];
			str += closed[1 & array[offs + 7]>>6];
			str += broken[1 & array[offs + 7]>>5];
			str += " ";

			str += String.valueOf((0x7C & array[offs + 4])>>>2);
			str += ":";
			str += String.valueOf((3 & array[offs + 4])<<4 | (0xF0 & array[offs + 5])>>>4);
			str += ":";
			str += String.valueOf((7 & array[offs + 5])<<3 | (0xE0 & array[offs + 6])>>>5);
			str += ":";
			str += String.valueOf((0x1F & array[offs + 6])<<1 | (0x80 & array[offs + 7])>>>7);

			datatype += str;

			setDataType(datatype);

			setOffsets(pos, offs, len, _chunk);
		}
	}

	class UserData extends StreamObject {

		public UserData()
		{
			type = USER_DATA;
			datatype = "UserData";

			setDataType(datatype);
		}

		public void set(int pos, byte[] array, int offs, int len, int _chunk)
		{
			String str = new String(array, offs + 4, len - 4);

			datatype += " '" + str + "'";

			setDataType(datatype);

			setOffsets(pos, offs, len, _chunk);
		}
	}

	class SequenceEnd extends StreamObject {

		public SequenceEnd()
		{
			type = SEQUENCE_END;
			datatype = "SequenceEnd";

			setDataType(datatype);
		}
	}

	class StreamObject {

		public int chunk = -1;
		public int type = -1;

		public int offset = -1;
		public int length = -1;
		public int error = 0;

		public Object[] objecttable = new Object[s2];
		public long pts = -1;
		public double delta = -1;

		public String datatype = "StreamObject";

		public StreamObject()
		{}

		public void copy(int _type, String _datatype, int _offset, int _length, int _chunk)
		{
			type = _type;
			offset = _offset;
			length = _length;
			chunk = _chunk;

			setDataType(_datatype);

			objecttable[0] = String.valueOf(chunk);
			objecttable[1] = String.valueOf(offset);
			objecttable[2] = String.valueOf(offset + length);
		}

		public void set(int pos, byte[] array, int offs, int len, int _chunk)
		{
			setOffsets(pos, offs, len, _chunk);
		}

		public void setOffsets(int pos, int offs, int len, int _chunk)
		{
			offset = pos + offs;
			length = len;
			chunk = _chunk;

			objecttable[0] = String.valueOf(chunk);
			objecttable[1] = String.valueOf(offset);
			objecttable[2] = String.valueOf(offset + length);
		}

		public void setPTSValue(long val)
		{
			pts = val;
			objecttable[5] = String.valueOf(pts);
			objecttable[6] = Common.formatTime_1(pts / 90L);
		}

		public long getPTSValue()
		{
			return pts;
		}

		public int getOffset()
		{
			return offset;
		}

		public int getLength()
		{
			return length;
		}

		public int getType()
		{
			return type;
		}

		public void setDelta(double val)
		{
			delta = val;

			double tmp = (delta / 90.0) * 1000.0;
			tmp = Math.round(tmp) / 1000.0;

			objecttable[4] = String.valueOf(tmp); // in millisec
		}

		public int getError()
		{
			return error;
		}

		public void setError(int val)
		{
			error = val;
			objecttable[8] = String.valueOf(error);

			if ((val & 0x20) != 0) //tref error
				objecttable[3] = objecttable[3].toString() + "e";
		}

		public void setDataType(String str)
		{
			datatype = str;
			objecttable[7] = datatype;
		}

		public String getDataType()
		{
			return datatype;
		}

		public int getChunk()
		{
			return chunk;
		}

		public String toString()
		{
			return ("#" + getChunk() + " (" + getOffset() + "->" + (getOffset() + getLength()) + ")" + datatype);
		}

		public Object[] getObjectTable()
		{
			return objecttable;
		}

	}
}