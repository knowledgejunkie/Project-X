/*
 * @(#)HEXVIEWER.java - simple hexviewer
 *
 * Copyright (c) 2002-2004 by dvb.matt. 
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


import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.io.*;

//DM26032004 081.6 int18 changed
public class HexViewer extends JFrame {

private String file = "";
private JTextArea HexArea;
private JTextField Field, Field1, from, fsize;
private JScrollPane scroll;
private JViewport viewport;
private JLabel flen, hexn;
private JSlider slider;
private JFileChooser chooser;
private JCheckBox textonly;

public HexViewer() {
	init();
}

protected void init()
{
	addWindowListener (new WindowAdapter(){ public void windowClosing(WindowEvent e) { close(); } });
	setTitle("Hex Viewer");

	chooser = new JFileChooser();
	scroll = new JScrollPane();
	HexArea = new LogArea();
	HexArea.setFont(new Font("Courier New", Font.PLAIN, 12));
	scroll.setViewportView(HexArea);
	viewport = scroll.getViewport();

	slider = new JSlider(JSlider.VERTICAL,0,15,0);
	slider.setInverted(true);
	slider.addChangeListener(new ChangeListener() {
		public void stateChanged(ChangeEvent e) {
			readfile((16L*slider.getValue()));
	}});

	Field = new JTextField("0");
	Field.setToolTipText("hit henter to jump to file position");
	Field.setPreferredSize(new Dimension(100,25));
	Field.setMaximumSize(new Dimension(100,25));
	Field.setEditable(true);

	hexn = new JLabel("= hex: ");
	hexn.setPreferredSize(new Dimension(120,25));
	hexn.setMaximumSize(new Dimension(120,25));

	Field1 = new JTextField("0");
	Field1.setToolTipText("hit henter to jump to file position");
	Field1.setPreferredSize(new Dimension(100,25));
	Field1.setMaximumSize(new Dimension(100,25));
	Field1.setEditable(true);

	JPanel container = new JPanel();
	container.setLayout( new BorderLayout() );

	JPanel menu = new JPanel();
	menu.setLayout( new BoxLayout(menu,BoxLayout.X_AXIS ));
	menu.setToolTipText("hit henter to jump to file position");
	menu.add(new JLabel("Jump to Dec.:"));
	menu.add(Field);
	menu.add(hexn);
	menu.add(new JLabel("Jump to Hex.:"));
	menu.add(Field1);

	flen = new JLabel(" fsize: ");
	menu.add(flen);

	Field.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			try 
			{
			if (!Field.getText().equals("")) {
				hexn.setText("= hex: "+Long.toHexString(Long.parseLong(Field.getText())).toUpperCase());
				slider.setValue((int)(Long.parseLong(Field.getText())/16));
			}
			} 
			catch (Exception e1) {}
		}
	});

	Field1.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			try 
			{
			if (!Field1.getText().equals("")) {
				slider.setValue((int)(Long.parseLong(Field1.getText(),16)/16));
			}
			} 
			catch (Exception e1) {}
		}
	});


	JPanel grid = new JPanel();
	grid.setLayout( new GridLayout(1,1) );
	grid.add(scroll);

	JPanel menu2 = new JPanel();
	menu2.setLayout( new BoxLayout(menu2,BoxLayout.X_AXIS ));
	menu2.setToolTipText("extract file segment to new file");

	from = new JTextField("0");
	from.setPreferredSize(new Dimension(100,25));
	from.setMaximumSize(new Dimension(100,25));
	from.setEditable(true);

	fsize = new JTextField("1000");
	fsize.setPreferredSize(new Dimension(100,25));
	fsize.setMaximumSize(new Dimension(100,25));
	fsize.setEditable(true);

	JButton extract = new JButton("extract from: (hex.)");
	extract.setToolTipText("click to extract to file");

	extract.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			try 
			{
			if (!from.getText().equals("") && !fsize.getText().equals("")) {
				long pos = Long.parseLong(from.getText(),16);
				long size = Long.parseLong(fsize.getText(),16) - pos;
				savefile(pos,size);
			} 
			} 
			catch (Exception e1) {}
		}
	});

	menu2.add(extract);
	menu2.add(from);
	menu2.add(new JLabel(" to : (hex.)"));
	menu2.add(fsize);

	JButton close = new JButton(" close ");
	close.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			close();
		}
	});

	getRootPane().setDefaultButton(close);
	menu2.add(close);

	textonly = new JCheckBox(" text mode");
	textonly.setSelected(false);
	menu2.add(textonly);

	JPanel menu3 = new JPanel();
	menu3.setLayout( new GridLayout(2,1));
	menu3.add(menu);
	menu3.add(menu2);

	container.add(slider, BorderLayout.EAST);
	container.add(grid);
	container.add(menu3, BorderLayout.SOUTH);

	getContentPane().add(container);
	centerDialog();
	UIManager.addPropertyChangeListener(new UISwitchListener(container));
}

//DM06092003+  changed
private void savefile(long startPos, long size) {
	long len = new File(file).length();
	size = (startPos+size>len) ? (len-startPos) : size;

	if (startPos>=len || startPos<0 || size<1) 
		return;

	String newfile = file+"(0x"+Long.toHexString(startPos)+" to 0x"+Long.toHexString(startPos+size)+").bin";
	chooser.setSelectedFile(new File(newfile));
	chooser.rescanCurrentDirectory();

	int retval = chooser.showSaveDialog(this);
	if(retval == JFileChooser.APPROVE_OPTION) {
		File theFile = chooser.getSelectedFile();
		if(theFile != null && !theFile.isDirectory()) {
			newfile = theFile.getAbsolutePath();
		}
	} else 
		return;

	setTitle("Hex Viewer, saving "+newfile); //DM30122003 081.6 int10 add

	try 
	{
	int buf=3072000;
	BufferedInputStream hex = new BufferedInputStream(new FileInputStream(file),buf);
	BufferedOutputStream hex1 = new BufferedOutputStream(new FileOutputStream(newfile),buf);
	long filePos=0, endPos=startPos+size;

	while (filePos < startPos)
		filePos += hex.skip(startPos-filePos);

	byte data[];
	int datalen;
	while (filePos < endPos) {
		datalen = (endPos-filePos) < (long)buf ? (int)(endPos-filePos) : buf;
		data = new byte[datalen];
		datalen = hex.read(data);
		hex1.write(data,0,datalen);
		filePos += datalen;
	}
	hex.close();
	hex1.flush();
	hex1.close();
	}
	catch (IOException e) { 
		HexArea.setText(".. cannot access file : "+file); 
	}
	setTitle("Hex Viewer for File: "+file); //DM30122003 081.6 int10 add
}
//DM06092003-

private void readfile(long position) {
	try 
	{
	RandomAccessFile hex = new RandomAccessFile(file,"r");
	long len = hex.length();
	if (position<len) {
		if (textonly.isSelected()) {
			hex.seek(position);
			String text = "";
			if (position!=0) 
				hex.readLine();
			for (int a=0; a<24 && hex.getFilePointer()<len; a++) 
				text += hex.readLine() + "\n";
			HexArea.setText(text);
		} else {
			int viewsize = (int)(((len-position)>=384L) ? 384 : (len-position) );
			byte[] data = new byte[viewsize];
			hex.seek(position);
			hex.read(data);
			print(data,position);
		}
	}
	hex.close();

	} 
	catch (IOException e) { 
		HexArea.setText(".. cannot access file : "+file); 
	}
}

private void print(byte[] data,long position) {
	String fill = "0000000000";
	String text = "";
	for (int a=0; a<data.length; a+=16) {
		String ascii = " : ", stuff = "   ";
		String pos = Long.toHexString(position+a).toUpperCase();
		text += fill.substring(0,10-pos.length())+pos+" : ";
		int b=0;
		for (; b<16 && a+b<data.length; b++) { 
			String val = Integer.toHexString((0xFF&data[a+b])).toUpperCase();
			text += fill.substring(0,2-val.length())+val+((b==7)?"-":" ");
			ascii += ((0xFF&data[a+b])>31 && (0xFF&data[a+b])<127) ? ""+((char)data[a+b]) : ".";
		}
		for (;b<16;b++) 
			text += stuff;
		text += ascii+"\n";
	}
	HexArea.setText(text);
}

public void view(String file1) {
	long filelen = new File(file1).length();
	if (!(file).equals(file1)) {
		HexArea.setText("");
		slider.setMaximum((int)(filelen/16));
		file = file1;
		if (slider.getValue()==0) 
			readfile(0);
		else 
			slider.setValue(0);
	}
	file = file1;
	setTitle("Hex Viewer for File: "+file);
	flen.setText(" fsize: "+filelen+"  ");
	this.show();
}

protected void centerDialog() {
	this.setLocation(200,200);
	this.setSize(600,435);
}

private void close() {
	dispose(); //DM26032004 081.6 int18 add
	System.gc();
}


}
