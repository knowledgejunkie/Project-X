package net.sourceforge.dvb.projectx.xinput.ftp;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import net.sourceforge.dvb.projectx.common.Resource;
import net.sourceforge.dvb.projectx.xinput.XInputDirectory;

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

	boolean isTested;

	XInputDirectory xInputDirectory;

	GridBagLayout gridBagLayout1 = new GridBagLayout();

	JPanel jPanel1 = new JPanel();

	JLabel jLabel1 = new JLabel();

	JLabel jLabel2 = new JLabel();

	JLabel jLabel3 = new JLabel();

	JLabel jLabel4 = new JLabel();

	JTextField tfServer = new JTextField();

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
		jLabel2.setToolTipText(Resource.getString("ftpchooser.user.tip"));
		jLabel2.setText(Resource.getString("ftpchooser.user"));
		jLabel3.setToolTipText(Resource.getString("ftpchooser.password.tip"));
		jLabel3.setText(Resource.getString("ftpchooser.password"));
		jLabel4.setToolTipText(Resource.getString("ftpchooser.directory.tip"));
		jLabel4.setText(Resource.getString("ftpchooser.directory"));
		tfServer.setMinimumSize(new Dimension(80, 21));
		tfServer.setPreferredSize(new Dimension(80, 21));
		tfServer.setToolTipText(Resource.getString("ftpchooser.server.tip"));
		tfServer.setText(Resource.getString("ftpchooser.server.entry"));
		tfServer.addFocusListener(new FtpChooser_tfServer_focusAdapter(this));
		tfUser.setToolTipText(Resource.getString("ftpchooser.user.tip"));
		tfUser.setText(Resource.getString("ftpchooser.user.entry"));
		tfUser.addFocusListener(new FtpChooser_tfUser_focusAdapter(this));
		tfPassword.setToolTipText(Resource.getString("ftpchooser.password.tip"));
		tfPassword.setText(Resource.getString("ftpchooser.password.entry"));
		tfPassword.addFocusListener(new FtpChooser_tfPassword_focusAdapter(this));
		tfDirectory.setMinimumSize(new Dimension(153, 21));
		tfDirectory.setPreferredSize(new Dimension(153, 21));
		tfDirectory.setToolTipText(Resource.getString("ftpchooser.directory.tip"));
		tfDirectory.setText(Resource.getString("ftpchooser.directory.entry"));
		tfDirectory.addFocusListener(new FtpChooser_tfDirectory_focusAdapter(this));
		testButton.setText(Resource.getString("ftpchooser.test"));
		testButton.addActionListener(new FtpChooser_testButton_actionAdapter(this));
		jLabel5.setToolTipText(Resource.getString("ftpchooser.state.tip"));
		jLabel5.setText(Resource.getString("ftpchooser.state"));
		tfState.setEditable(false);
		tfState.setText(Resource.getString("ftpchooser.untested"));
		okButton.setEnabled(false);
		okButton.setText(Resource.getString("ftpchooser.ok"));
		okButton.addActionListener(new FtpChooser_okButton_actionAdapter(this));
		cancelButton.setText(Resource.getString("ftpchooser.cancel"));
		cancelButton.addActionListener(new FtpChooser_cancelButton_actionAdapter(this));
		spState.setViewportView(taState);
		jPanel1.setMinimumSize(new Dimension(600, 266));
		jPanel1.setPreferredSize(new Dimension(600, 266));
		this.setModal(true);
		this.setTitle(Resource.getString("ftpchooser.title"));
		jPanel1.add(jLabel1, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(10, 10, 5, 5), 0, 0));
		jPanel1.add(jLabel2, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(5, 10, 5, 5), 0, 0));
		jPanel1.add(jLabel4, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(5, 10, 5, 5), 0, 0));
		jPanel1.add(jLabel3, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(5, 10, 5, 5), 0, 0));
		jPanel1.add(jLabel5, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(5, 10, 5, 5), 0, -3));
		jPanel1.add(tfServer, new GridBagConstraints(1, 0, 2, 1, 0.5, 0.0, GridBagConstraints.WEST,
				GridBagConstraints.HORIZONTAL, new Insets(10, 5, 5, 5), 64, 0));
		jPanel1.add(tfUser, new GridBagConstraints(1, 1, 2, 1, 0.5, 0.0, GridBagConstraints.WEST,
				GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 119, 0));
		jPanel1.add(tfPassword, new GridBagConstraints(1, 2, 2, 1, 0.5, 0.0, GridBagConstraints.WEST,
				GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 89, 0));
		jPanel1.add(tfDirectory, new GridBagConstraints(1, 3, 2, 1, 0.5, 0.0, GridBagConstraints.WEST,
				GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
		jPanel1.add(tfState, new GridBagConstraints(1, 4, 2, 1, 0.5, 0.0, GridBagConstraints.WEST,
				GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 93, -1));
		jPanel1.add(testButton, new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0, GridBagConstraints.SOUTH,
				GridBagConstraints.NONE, new Insets(5, 10, 10, 5), 15, 0));
		jPanel1.add(okButton, new GridBagConstraints(1, 5, 1, 1, 0.0, 0.0, GridBagConstraints.SOUTH,
				GridBagConstraints.NONE, new Insets(5, 5, 10, 5), 21, 0));
		jPanel1.add(cancelButton, new GridBagConstraints(2, 5, 1, 1, 0.0, 0.0, GridBagConstraints.SOUTH,
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
		FtpVO ftpVO = new FtpVO(tfServer.getText(), tfUser.getText(), tfPassword.getText(), tfDirectory.getText(), null);
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
		this.setVisible(false);
	}

	void cancelButton_actionPerformed(ActionEvent e) {
		isTested = false;
		xInputDirectory = null;
		okButton.setEnabled(false);
		this.setVisible(false);
	}

	void tfServer_focusLost(FocusEvent e) {
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