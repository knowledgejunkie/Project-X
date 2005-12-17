/*
 * @(#)FtpChooser.java - for ftp access
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

package net.sourceforge.dvb.projectx.gui;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusAdapter;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Dimension;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import net.sourceforge.dvb.projectx.common.Resource;
import net.sourceforge.dvb.projectx.common.Common;
import net.sourceforge.dvb.projectx.xinput.XInputDirectory;

import net.sourceforge.dvb.projectx.xinput.ftp.FtpVO;

/**
 * <p>
 * Überschrift:
 * </p>
 * <p>
 * Beschreibung:
 * </p>
 * <p>
 * Copyright: Copyright (c) 2004
 * </p>
 * <p>
 * Organisation:
 * </p>
 *
 * @author unbekannt
 * @version 1.0
 */

public class FtpChooser extends JDialog {

	private final String _base_key = "FtpServer.";

	private final String ftp_server = "192.168.0.5";
	private final String ftp_port = "21";
	private final String ftp_user = "root";
	private final String ftp_password = "dreambox";
	private final String ftp_directory = "/hdd/movie";

	boolean isTested;

	XInputDirectory xInputDirectory;

	GridBagLayout gridBagLayout1 = new GridBagLayout();

	JPanel jPanel1 = new JPanel();

	JLabel jLabel1 = new JLabel();

	JLabel jLabel1a = new JLabel();

	JLabel jLabel2 = new JLabel();

	JLabel jLabel3 = new JLabel();

	JLabel jLabel4 = new JLabel();

	JTextField tfServer = new JTextField();

	JTextField tfPort = new JTextField();

	JTextField tfUser = new JTextField();

	JTextField tfPassword = new JTextField();

	JTextField tfDirectory = new JTextField();

	JButton testButton = new JButton();

	JLabel jLabel5 = new JLabel();

	JTextField tfState = new JTextField();

	JButton okButton = new JButton();

	JButton cancelButton = new JButton();

	JScrollPane spState = new JScrollPane();

	JTextArea taState = new JTextArea();

	GridBagLayout gridBagLayout2 = new GridBagLayout();

	public FtpChooser() {

		try {

			jbInit();
			loadFields();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public XInputDirectory getXInputDirectory() {
		return xInputDirectory;
	}

	public void setXInputDirectory(XInputDirectory aXInputDirectory) {
		isTested = false;
		okButton.setEnabled(false);

		tfServer.setText(aXInputDirectory.getServer());
		tfPort.setText(aXInputDirectory.getPort());
		tfUser.setText(aXInputDirectory.getUser());
		tfPassword.setText(aXInputDirectory.getPassword());
		tfDirectory.setText(aXInputDirectory.getDirectory());
	}

	public boolean isTested() {
		return isTested;
	}

	private void jbInit() throws Exception {

		this.getContentPane().setLayout(gridBagLayout1);
		jPanel1.setLayout(gridBagLayout2);

		jLabel1.setToolTipText(Resource.getString("ftpchooser.server.tip"));
		jLabel1.setText(Resource.getString("ftpchooser.server"));

		jLabel1a.setToolTipText(Resource.getString("ftpchooser.port.tip"));
		jLabel1a.setText(Resource.getString("ftpchooser.port"));

		jLabel2.setToolTipText(Resource.getString("ftpchooser.user.tip"));
		jLabel2.setText(Resource.getString("ftpchooser.user"));

		jLabel3.setToolTipText(Resource.getString("ftpchooser.password.tip"));
		jLabel3.setText(Resource.getString("ftpchooser.password"));

		jLabel4.setToolTipText(Resource.getString("ftpchooser.directory.tip"));
		jLabel4.setText(Resource.getString("ftpchooser.directory"));

		tfServer.setMinimumSize(new Dimension(80, 21));
		tfServer.setNextFocusableComponent(tfPort);
		tfServer.setPreferredSize(new Dimension(80, 21));
		tfServer.setToolTipText(Resource.getString("ftpchooser.server.tip"));
		tfServer.setText(ftp_server);
		tfServer.addFocusListener(new FtpChooser_tfServer_focusAdapter(this));

		tfPort.setMinimumSize(new Dimension(80, 21));
		tfPort.setNextFocusableComponent(tfUser);
		tfPort.setPreferredSize(new Dimension(80, 21));
		tfPort.setToolTipText(Resource.getString("ftpchooser.port.tip"));
		tfPort.setText(ftp_port);
		tfPort.addFocusListener(new FtpChooser_tfPort_focusAdapter(this));

		tfUser.setNextFocusableComponent(tfPassword);
		tfUser.setToolTipText(Resource.getString("ftpchooser.user.tip"));
		tfUser.setText(ftp_user);
		tfUser.addFocusListener(new FtpChooser_tfUser_focusAdapter(this));

		tfPassword.setNextFocusableComponent(tfDirectory);
		tfPassword.setToolTipText(Resource.getString("ftpchooser.password.tip"));
		tfPassword.setText(ftp_password);
		tfPassword.addFocusListener(new FtpChooser_tfPassword_focusAdapter(this));

		tfDirectory.setMinimumSize(new Dimension(153, 21));
		tfDirectory.setNextFocusableComponent(testButton);
		tfDirectory.setPreferredSize(new Dimension(153, 21));
		tfDirectory.setToolTipText(Resource.getString("ftpchooser.directory.tip"));
		tfDirectory.setText(ftp_directory);
		tfDirectory.addFocusListener(new FtpChooser_tfDirectory_focusAdapter(this));

		testButton.setNextFocusableComponent(okButton);
		testButton.setText(Resource.getString("ftpchooser.test"));
		testButton.addActionListener(new FtpChooser_testButton_actionAdapter(this));

		jLabel5.setToolTipText(Resource.getString("ftpchooser.state.tip"));
		jLabel5.setText(Resource.getString("ftpchooser.state"));

		tfState.setEditable(false);
		tfState.setText(Resource.getString("ftpchooser.untested"));

		okButton.setEnabled(false);
		okButton.setNextFocusableComponent(cancelButton);
		okButton.setText(Resource.getString("ftpchooser.ok"));
		okButton.addActionListener(new FtpChooser_okButton_actionAdapter(this));

		cancelButton.setNextFocusableComponent(tfServer);
		cancelButton.setText(Resource.getString("ftpchooser.cancel"));
		cancelButton.addActionListener(new FtpChooser_cancelButton_actionAdapter(this));

		spState.setViewportView(taState);

		jPanel1.setMinimumSize(new Dimension(600, 266));
		jPanel1.setPreferredSize(new Dimension(600, 266));

		this.setModal(true);
		this.setTitle(Resource.getString("ftpchooser.title"));

		taState.setEditable(false);

		jPanel1.add(jLabel1, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(10, 10, 5, 5), 0, 0));
		jPanel1.add(jLabel1a, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(10, 10, 5, 5), 0, 0));
		jPanel1.add(jLabel2, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(5, 10, 5, 5), 0, 0));
		jPanel1.add(jLabel4, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(5, 10, 5, 5), 0, 0));
		jPanel1.add(jLabel3, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(5, 10, 5, 5), 0, 0));
		jPanel1.add(jLabel5, new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(5, 10, 5, 5), 0, -3));
		jPanel1.add(tfServer, new GridBagConstraints(1, 0, 2, 1, 0.5, 0.0, GridBagConstraints.WEST,
				GridBagConstraints.HORIZONTAL, new Insets(10, 5, 5, 5), 64, 0));
		jPanel1.add(tfPort, new GridBagConstraints(1, 1, 2, 1, 0.5, 0.0, GridBagConstraints.WEST,
				GridBagConstraints.HORIZONTAL, new Insets(10, 5, 5, 5), 64, 0));
		jPanel1.add(tfUser, new GridBagConstraints(1, 2, 2, 1, 0.5, 0.0, GridBagConstraints.WEST,
				GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 119, 0));
		jPanel1.add(tfPassword, new GridBagConstraints(1, 3, 2, 1, 0.5, 0.0, GridBagConstraints.WEST,
				GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 89, 0));
		jPanel1.add(tfDirectory, new GridBagConstraints(1, 4, 2, 1, 0.5, 0.0, GridBagConstraints.WEST,
				GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
		jPanel1.add(tfState, new GridBagConstraints(1, 5, 2, 1, 0.5, 0.0, GridBagConstraints.WEST,
				GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 93, -1));
		jPanel1.add(testButton, new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0, GridBagConstraints.SOUTH,
				GridBagConstraints.NONE, new Insets(5, 10, 10, 5), 15, 0));
		jPanel1.add(okButton, new GridBagConstraints(1, 6, 1, 1, 0.0, 0.0, GridBagConstraints.SOUTH,
				GridBagConstraints.NONE, new Insets(5, 5, 10, 5), 21, 0));
		jPanel1.add(cancelButton, new GridBagConstraints(2, 6, 1, 1, 0.0, 0.0, GridBagConstraints.SOUTH,
				GridBagConstraints.NONE, new Insets(5, 5, 10, 5), 3, 0));
		jPanel1.add(spState, new GridBagConstraints(3, 0, 1, 6, 0.5, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH,
				new Insets(10, 5, 10, 10), 193, 229));
		this.getContentPane().add(
				jPanel1,
				new GridBagConstraints(0, 0, 4, 4, 1.0, 1.0, GridBagConstraints.SOUTHEAST, GridBagConstraints.BOTH, new Insets(
						0, 0, 0, 0), 0, 0));

		xInputDirectory = null;
		isTested = false;
		okButton.setEnabled(false);

		setLocation(200, 200);
	}

	void testButton_actionPerformed(ActionEvent e) {
		saveFields();
		FtpVO ftpVO = new FtpVO(tfServer.getText(), tfUser.getText(), tfPassword.getText(), tfDirectory.getText(), tfPort.getText(), null);
		xInputDirectory = new XInputDirectory(ftpVO);

		isTested = xInputDirectory.test();

		okButton.setEnabled(isTested);
		tfState.setText(xInputDirectory.getTestMsg());
		taState.setText(xInputDirectory.getLog());

		if (!isTested) {
			xInputDirectory = null;
		}
	}
	
	void okButton_actionPerformed(ActionEvent e) {
		saveFields();
		setVisible(false);
	}

	void cancelButton_actionPerformed(ActionEvent e) {
		isTested = false;
		xInputDirectory = null;
		okButton.setEnabled(false);
		setVisible(false);
	}

	void tfServer_focusLost(FocusEvent e) {
		okButton.setEnabled(false);
	}

	void tfPort_focusLost(FocusEvent e) {
		okButton.setEnabled(false);
	}

	void tfUser_focusLost(FocusEvent e) {
		okButton.setEnabled(false);
	}

	void tfPassword_focusLost(FocusEvent e) {
		okButton.setEnabled(false);
	}

	void tfDirectory_focusLost(FocusEvent e) {
		okButton.setEnabled(false);
	}


	/**
	 *
	 */
	private void loadFields()
	{
		String str = null;

		str = Common.getSettings().getProperty( _base_key + "Server");

		if (str != null)
			tfServer.setText(str);

		str = Common.getSettings().getProperty( _base_key + "Port");

		if (str != null)
			tfPort.setText(str);

		str = Common.getSettings().getProperty( _base_key + "User");

		if (str != null)
			tfUser.setText(str);

		str = Common.getSettings().getProperty( _base_key + "Password");

		if (str != null)
			tfPassword.setText(str);

		str = Common.getSettings().getProperty( _base_key + "Directory");

		if (str != null)
			tfDirectory.setText(str);
	}

	/**
	 *
	 */
	private void saveFields()
	{
		Common.getSettings().setProperty( _base_key + "Server" , tfServer.getText());
		Common.getSettings().setProperty( _base_key + "Port" , tfPort.getText());
		Common.getSettings().setProperty( _base_key + "User" , tfUser.getText());
		Common.getSettings().setProperty( _base_key + "Password" , tfPassword.getText());
		Common.getSettings().setProperty( _base_key + "Directory" , tfDirectory.getText());
	}

}

class FtpChooser_testButton_actionAdapter implements java.awt.event.ActionListener {

	FtpChooser adaptee;

	FtpChooser_testButton_actionAdapter(FtpChooser adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.testButton_actionPerformed(e);
	}
}

class FtpChooser_okButton_actionAdapter implements java.awt.event.ActionListener {

	FtpChooser adaptee;

	FtpChooser_okButton_actionAdapter(FtpChooser adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.okButton_actionPerformed(e);
	}
}

class FtpChooser_cancelButton_actionAdapter implements java.awt.event.ActionListener {

	FtpChooser adaptee;

	FtpChooser_cancelButton_actionAdapter(FtpChooser adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.cancelButton_actionPerformed(e);
	}
}

class FtpChooser_tfServer_focusAdapter extends java.awt.event.FocusAdapter {

	FtpChooser adaptee;

	FtpChooser_tfServer_focusAdapter(FtpChooser adaptee) {
		this.adaptee = adaptee;
	}

	public void focusLost(FocusEvent e) {
		adaptee.tfServer_focusLost(e);
	}
}

class FtpChooser_tfPort_focusAdapter extends java.awt.event.FocusAdapter {

	FtpChooser adaptee;

	FtpChooser_tfPort_focusAdapter(FtpChooser adaptee) {
		this.adaptee = adaptee;
	}

	public void focusLost(FocusEvent e) {
		adaptee.tfPort_focusLost(e);
	}
}

class FtpChooser_tfUser_focusAdapter extends java.awt.event.FocusAdapter {

	FtpChooser adaptee;

	FtpChooser_tfUser_focusAdapter(FtpChooser adaptee) {
		this.adaptee = adaptee;
	}

	public void focusLost(FocusEvent e) {
		adaptee.tfUser_focusLost(e);
	}
}

class FtpChooser_tfPassword_focusAdapter extends java.awt.event.FocusAdapter {

	FtpChooser adaptee;

	FtpChooser_tfPassword_focusAdapter(FtpChooser adaptee) {
		this.adaptee = adaptee;
	}

	public void focusLost(FocusEvent e) {
		adaptee.tfPassword_focusLost(e);
	}
}

class FtpChooser_tfDirectory_focusAdapter extends java.awt.event.FocusAdapter {

	FtpChooser adaptee;

	FtpChooser_tfDirectory_focusAdapter(FtpChooser adaptee) {
		this.adaptee = adaptee;
	}

	public void focusLost(FocusEvent e) {
		adaptee.tfDirectory_focusLost(e);
	}
}