/*
 * @(#)X.java - main incl. GUI
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

/*
 * X is completely designed as a test, therefore it mostly implements its 
 * own code instead of a derivation of an ISO reference source or 
 * any other code. Considerable effort has been expended to ensure 
 * an useful implementation, even in cases where the standards 
 * are ambiguous or misleading.
 * Do not expect any useful output from it, even if that may possible.
 *
 * For a program compliant to the international standards ISO 11172
 * and ISO 13818 it is inevitable to use methods covered by patents
 * in various countries. The authors of this program disclaim any
 * liability for patent infringement caused by using, modifying or
 * redistributing this program.
 *
 */


import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.Arrays.*;
import java.beans.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import javax.swing.filechooser.*;
import javax.swing.plaf.basic.BasicComboBoxUI;
import java.awt.dnd.*;
import java.awt.datatransfer.*;


public class X extends JPanel
{

static String version[] = { 
	"ProjectX 0.81.8",
	"30.08.2004",
	"TEST PROJECT ONLY",
	", User: " + System.getProperty("user.name")
};

static String terms[] = { 
	" ",
	"TERMS OF CONDITIONS:",
	"(1) this is a free Java based demux utility.",
	"(2) It is intended for educational purposes only, as a non-commercial test project.",
	"(3) It may not be used otherwise. Most parts are only experimental.",
	"(4) released under the terms of the GNU GPL",
	"(5) there is NO WARRANTY of any kind attached to this software",
	"(6) use it at your own risk and for your own education as it was meant",
	" ",
};

//DM18062004 081.7 int05 add
RawInterface raw_interface = new RawInterface();
static int loadSizeForward = 2560000;

static BRMonitor brm;
static SubPicture subpicture = new SubPicture(); //DM06032004 081.6 int18 changed

//TELETEXT Teletext = new TELETEXT(); //now static methods

Scan scan = new Scan();
MPAC MPAConverter = new MPAC();
MPAD MPADecoder = new MPAD();
static MPVD MPVDecoder = new MPVD();
Audio Audio = new Audio();
static D2V d2v = new D2V();
static TS tf = new TS();

static String inifile = "", ColourTablesFile = ""; //DM13062004 081.7 int04 add
static String inidir = System.getProperty("user.dir");
static String filesep = System.getProperty("file.separator");
static String frametitle = "";

static java.text.DateFormat base_time = new java.text.SimpleDateFormat("HH:mm:ss.SSS");

PrintStream logging;
static String loggin="", workouts ="", loggin2="";
static boolean comchange=false, outchange=false, singleraw=false, newvideo=true, running=false, qinfo=false, qbreak=false, qpause=false;
static String outalias = "output to same location as 1st file in collection";
static String newOutName = "";

static long[] options = new long[58];
static byte[] headerrescue= new byte[1];
Object[] aspratio = {"don't change","1.000 (1:1)","0.6735 (4:3)","0.7031 (16:9)","0.7615 (2.21:1)","0.8055","0.8437","0.9375","0.9815","1.0255","1.0695","1.1250","1.1575","1.2015" };
Object[] DAR = {"1.000 (1:1)","0.6735 (4:3)","0.7031 (16:9)","0.7615 (2.21:1)","0.8055","0.8437","0.9375","0.9815","1.0255","1.0695","1.1250","1.1575","1.2015" };
Object[] H_RESOLUTION = { "304","320","352","384","480","528","544","576","640","704","720" };

static int[] clv = new int[10];
static long CP[] = new long[2];
static int bs=8192000;
static Object[] inputfiles = new Object[0], collectionlist = new Object[0], collectionwork = new Object[0],	lastlist = new Object[0];
static ArrayList[] collfiles = new ArrayList[0];
static ArrayList collout = new ArrayList(), tempfiles = new ArrayList();
static ArrayList workinglist = new ArrayList(), combvideo = new ArrayList();
static ArrayList InfoAtEnd = new ArrayList(), cell = new ArrayList();
static ArrayList cutlist = new ArrayList(), ctemp = new ArrayList(), speciallist = new ArrayList();
static ArrayList TSPidlist = new ArrayList(), PVAPidlist = new ArrayList(), AC3list = new ArrayList();
static ArrayList TSdemuxlist = new ArrayList(), PVAdemuxlist = new ArrayList(),	VDRdemuxlist = new ArrayList(), PESdemuxlist = new ArrayList();

static JFrame frame = new JFrame(); //DM20032004 081.6 int18 changed
static int[] framelocation = { 200, 100, 714, 460};  //DM26032004 081.6 int18 changed

static String[] workfiles = {""}, VBASIC = new String[4];
static String messagelog="";

static JButton doitButton, breakButton, scanButton, pauseButton, extract, exeButton, picButton;
static JRadioButton[] RButton = new JRadioButton[25];
static JComboBox[] comBox = new JComboBox[38];

//DM14072004 081.7 int06 changed
//DM20072004 081.7 int07 changed
static JCheckBox[] cBox = new JCheckBox[63];

static JList list1, list3, list4;
static X_JFileChooser chooser; //DM12122003 081.6 int05

static JLabel msoff, audiostatusLabel, splitLabel, cutnum, ttxheaderLabel, ttxvpsLabel, outSize;
static JProgressBar progress;
static JTextField outfield;
static JTextField[] d2vfield = new JTextField[10], //DM18052004 081.7 int02 changed
	patchfield = new JTextField[3], exefield = new JTextField[9];


static boolean bool=false, PureVideo=false;
static byte[] dumpfill = new byte[16], SEndCode = { 0,0,1,(byte)0xb7 };

static int ERRORCODE=0;
static int videotimecount=0, currentcoll=0;
static int origframes=0, cutcount=0, FileNumber=0;
static int activecoll=-1, NoOfAudio=0, NoOfPictures=0, NoOfTTX=0, MPGVideotype=0; //DM04032004 081.6 int18 add
static long CUT_BYTEPOSITION=0;

private static long firstVideoPTS; //DM17012004 081.6 int11 new

COLLECTION dialog = new COLLECTION();
PATCH vpatch = new PATCH(frame);
EXECUTE executePane = new EXECUTE();

TabListener mytabListener = new TabListener();
FileListener my0Listener = new FileListener();
GoListener my2Listener = new GoListener();
DNDListener dnd = new DNDListener();
MenuListener menulistener = new MenuListener(); //DM26032004 081.6 int18 add

static AudioFrameConstants afc = new AudioFrameConstants();

static DropTarget dropTarget_1, dropTarget_2, dropTarget_3, dropTarget_4; //DM26032004 081.6 int18 changed

static JTextArea TextArea, FileInfoTextArea;  //DM26032004 081.6 int18 add
static JViewport viewport, viewport_2; //DM26032004 081.6 int18 add
static JTabbedPane logtab; //DM26032004 081.6 int18 add
static JButton add_files;  //DM26032004 081.6 int18 add
//static Thread thread;  //DM26032004 081.6 int18 add //unused ATM

JPopupMenu popup; //DM26032004 081.6 int18 changed
JMenuBar menuBar; //DM20032004 081.6 int18 add
JFrame autoload; //DM26032004 081.6 int18 add
Hashtable Options = new Hashtable();  //DM26032004 081.6 int18 add, intended for new ini or langauge

TeletextPageMatrix tpm = new TeletextPageMatrix();  //DM17042004 081.7 int02 add

//DM20072004 081.7 int07 add
long fakedPTS = -1;


public X()	//DM20032004 081.6 int18 changed
{

	// don't re-use ATM!
	RButton[0] = new JRadioButton();
	RButton[1] = new JRadioButton();

	/*** unused ***/
	//DM09082004 081.7 int08 moved
	cBox[35] = new JCheckBox();


	chooser = new X_JFileChooser(); //DM12122003 081.6 int05

	if ( !inidir.endsWith(filesep) ) 
		inidir += filesep;
	inifile = inidir + "X.ini";

	//DM13062004 081.7 int04 add
	ColourTablesFile = inidir + "colours.tbl";

	setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

	buildMenus();  //DM20032004 081.6 int18 add
	buildPopupMenu();  //DM26032004 081.6 int18 add
	buildAutoloadPanel();  //DM26032004 081.6 int18 add

	JPanel files = buildFilePanel();
	add(files);

	JPanel main = buildMainPanel();
	add(main, BorderLayout.CENTER);

	JPanel status = buildStatusPanel();
	add(status, BorderLayout.SOUTH);

	add(Box.createRigidArea(new Dimension(1,5)));

	base_time.setTimeZone(java.util.TimeZone.getTimeZone("GMT+0:00"));
	//subpicture.picture.run();  // start Pic , DM18052004 081.7 int02 changed
	java.util.Arrays.fill(dumpfill,(byte)0xFF);

}

//DM20032004 081.6 int18 moved
protected void buildPopupMenu()
{
	popup = new JPopupMenu("what");

	JMenuItem menuitem_1 = popup.add("open Cut/Specials..");
	menuitem_1.setActionCommand("openCut");
	popup.addSeparator();

	JMenuItem menuitem_2 = popup.add("add");
	JMenuItem menuitem_3 = popup.add("remove");
	popup.addSeparator();

	JMenuItem menuitem_4 = popup.add("rename");
	popup.addSeparator();

	JMenuItem menuitem_5 = popup.add("open in Hexviewer..");
	menuitem_5.setActionCommand("viewAsHex");
	JMenuItem menuitem_6 = popup.add("patch Video Basics..");
	menuitem_6.setActionCommand("editBasics");
	JMenuItem menuitem_7 = popup.add("sendTo CL#3..");
	menuitem_7.setActionCommand("sendTo3");

	popup.pack();

	UIManager.addPropertyChangeListener(new UISwitchListener(popup));

	menuitem_1.addActionListener(menulistener);
	menuitem_2.addActionListener(menulistener);
	menuitem_3.addActionListener(menulistener);
	menuitem_4.addActionListener(menulistener);
	menuitem_5.addActionListener(menulistener);
	menuitem_6.addActionListener(menulistener);	
	menuitem_7.addActionListener(menulistener);
}

//DM20032004 081.6 int18 add
protected void buildMenus()
{
	menuBar = new JMenuBar();

	JMenu file = buildFileMenu();
	file.setMnemonic('f');
	menuBar.add(file);

	JMenu preview = buildViewerMenu();
	preview.setMnemonic('o');
	menuBar.add(preview);
/**
	JMenu settings = buildSettingsMenu();
	settings.setMnemonic('s');
	menuBar.add(settings);
**/
	JMenu general = buildGeneralMenu();
	general.setMnemonic('r');
	menuBar.add(general);

	JMenu language = buildLanguageMenu();
	language.setMnemonic('l');
	menuBar.add(language);

	JMenu help = buildHelpMenu();
	help.setMnemonic('h');
	menuBar.add(help);

	frame.setJMenuBar(menuBar);	
}

//DM20032004 081.6 int18 add
protected JMenu buildFileMenu()
{
	JMenu file = new JMenu("File");

	JMenuItem add = new JMenuItem("Add..");
	add.setActionCommand("add");
	add.setMnemonic('a');

	JMenuItem remove = new JMenuItem("Remove");
	remove.setActionCommand("remove");
	remove.setMnemonic('r');

	JMenuItem rename = new JMenuItem("Rename..");
	rename.setActionCommand("rename");
	rename.setMnemonic('n');

	JMenuItem exit = new JMenuItem("Exit");
	exit.setActionCommand("exit");
	exit.setMnemonic('x');

	file.add(add);
	file.add(remove);
	file.addSeparator();
	file.add(rename);
	file.addSeparator();
	file.add(exit);

	add.addActionListener(menulistener);
	remove.addActionListener(menulistener);
	rename.addActionListener(menulistener);
	exit.addActionListener(menulistener);

	return file;
}

//DM20032004 081.6 int18 add
protected JMenu buildLanguageMenu()
{
	JMenu language = new JMenu("Language");

	JMenuItem item_eng = new JMenuItem("English");
	item_eng.setEnabled(false);

	language.add(item_eng);

	return language;
}

//DM20032004 081.6 int18 add
protected JMenu buildSettingsMenu()
{
	JMenu setting = new JMenu("Settings");
	JMenuItem open = new JMenuItem("Settings..");
	open.setMnemonic('s');

	setting.add(open);

	return setting;
}

//DM20032004 081.6 int18 add
protected JMenu buildGeneralMenu()
{
	JMenu general = new JMenu("General");

	ActionListener Al = new ActionListener()
	{
		public void actionPerformed(ActionEvent e)
		{
			String lnfName = e.getActionCommand();

			try 
			{
				UIManager.setLookAndFeel(lnfName);
				SwingUtilities.updateComponentTreeUI(frame);
				comBox[16].setSelectedItem(lnfName);

				if(chooser != null) 
					SwingUtilities.updateComponentTreeUI(chooser);
			} 
			catch (Exception exc)
			{
				comBox[16].removeItemAt(comBox[16].getSelectedIndex());
				System.err.println("Could not load LookAndFeel: " + lnfName);
			}
		}
	};

	UIManager.LookAndFeelInfo[] lf_info =  UIManager.getInstalledLookAndFeels();

	JRadioButtonMenuItem lf_item[] = new JRadioButtonMenuItem[lf_info.length];
	ButtonGroup lfgroup = new ButtonGroup();

	for (int a=0; a < lf_info.length; a++) 
	{
		lf_item[a] = new JRadioButtonMenuItem(lf_info[a].getClassName());
		general.add(lf_item[a]);
		lfgroup.add(lf_item[a]);
		lf_item[a].addActionListener(Al);
	}

	return general;
}

//DM20032004 081.6 int18 add
protected JMenu buildViewerMenu()
{
	JMenu preview = new JMenu("Options");

	JMenuItem video = new JMenuItem("open VideoCut/Specials..");
	video.setActionCommand("openCut");
	video.setMnemonic('v');

	preview.add(video);
	preview.addSeparator();

	JMenuItem hex = new JMenuItem("open in HexViewer..");
	hex.setActionCommand("viewAsHex");
	hex.setMnemonic('h');

	preview.add(hex);
	preview.addSeparator();

	JMenuItem basic = new JMenuItem("patch Video Basics..");
	basic.setActionCommand("editBasics");
	basic.setMnemonic('p');

	preview.add(basic);
	preview.addSeparator();

	JMenuItem subtitle = new JMenuItem("show Subtitle Preview..");
	subtitle.setActionCommand("subpreview");
	subtitle.setMnemonic('s');

	preview.add(subtitle);

	//DM17042004 081.7 int02 add+
	preview.addSeparator();

	JMenuItem pagematrix = new JMenuItem("show TeletextPageMatrix..");
	pagematrix.setActionCommand("pagematrix");
	pagematrix.setMnemonic('t');

	preview.add(pagematrix);

	pagematrix.addActionListener(menulistener);
	//DM17042004 081.7 int02 add-

	video.addActionListener(menulistener);
	subtitle.addActionListener(menulistener);
	hex.addActionListener(menulistener);
	basic.addActionListener(menulistener);

	return preview;
}

//DM20032004 081.6 int18 add
protected JMenu buildHelpMenu()
{
	JMenu help = new JMenu("Help");

	JMenuItem about = new JMenuItem("About..");
	about.setActionCommand("about");
	about.setMnemonic('a');

	JMenuItem openHtml = new JMenuItem("Help..");
	openHtml.setActionCommand("helphtml");
	openHtml.setMnemonic('h');

	help.add(about);
	help.addSeparator();
	help.add(openHtml);

	about.addActionListener(menulistener);
	openHtml.addActionListener(menulistener);

	return help;
}

//DM20032004 081.6 int18 add
protected void showAboutBox()
{
	JOptionPane.showMessageDialog(this, terms, "About..", JOptionPane.INFORMATION_MESSAGE);
}

// file panel
protected JPanel buildFilePanel()
{
	JPanel files = new JPanel();
	files.setLayout( new GridLayout(1,2) );
	files.setBorder(BorderFactory.createEtchedBorder());

	// coll list
	list3 = new JList(collectionwork);
	list3.setName("cw");
	list3.setVisibleRowCount(4);
	list3.setSelectionMode(2);
	list3.setToolTipText("drag&drop file(s) here, or use the context menu");
	list3.addMouseListener(new MouseAdapter()
	{
		public void mouseClicked(MouseEvent e)
		{
			if (e.getClickCount() == 1)
			{
				if (e.getModifiers() == MouseEvent.BUTTON3_MASK) 
					popup.show( list3, e.getX(), e.getY());

				else if (list3.getSelectedValue() != null )
					ScanInfo( list3.getSelectedValue().toString());
			}
			else if (e.getClickCount() > 1)
				if (comBox[0].getItemCount() > 0)
				{
					activecoll = comBox[0].getSelectedIndex(); 
					dialog.entry();
				}
		}
	});

	JScrollPane scrolltext_1 = new JScrollPane();
	scrolltext_1.setViewportView(list3);

	dropTarget_1 = new DropTarget(list3, dnd);


	JPanel aa = new JPanel();
	aa.setLayout(new ColumnLayout());

	comBox[0] = new JComboBox();  // number of act. coll.
	comBox[0].setActionCommand("cb");
	comBox[0].setPreferredSize(new Dimension(50,22));
	comBox[0].setMaximumSize(new Dimension(50,22));
	comBox[0].setMaximumRowCount(8);
	aa.add(comBox[0]);

	JButton file_up = new JButton("^");
	file_up.setActionCommand("up");
	file_up.setPreferredSize(new Dimension(50,22));
	file_up.setMaximumSize(new Dimension(50,22));
	file_up.setToolTipText("move file(s) up in collection list");
	aa.add(file_up);

	JButton file_down = new JButton("v");
	file_down.setActionCommand("down");
	file_down.setPreferredSize(new Dimension(50,22));
	file_down.setMaximumSize(new Dimension(50,22));
	file_down.setToolTipText("move file(s) down in collection list");
	aa.add(file_down);

	JButton file_remove = new JButton("x");
	file_remove.setActionCommand("rf");
	file_remove.setPreferredSize(new Dimension(50,22));
	file_remove.setMaximumSize(new Dimension(50,22));
	file_remove.setToolTipText("remove file(s) from collection list");
	aa.add(file_remove);


	JPanel control_1 = new JPanel(new BorderLayout());
	control_1.setAlignmentX(CENTER_ALIGNMENT);
	control_1.add(scrolltext_1, BorderLayout.CENTER);
	control_1.add(aa, BorderLayout.EAST);

	files.add(control_1);


	JPanel bb = new JPanel();
	bb.setLayout(new ColumnLayout());

	JLabel coll_label = new JLabel("= coll.#");
	coll_label.setPreferredSize(new Dimension(50,22));
	coll_label.setMaximumSize(new Dimension(50,22));
	coll_label.setToolTipText("choose active file collection");
	bb.add(coll_label);

	JButton add_coll = new JButton("+");
	add_coll.setActionCommand("+c");
	add_coll.setPreferredSize(new Dimension(50,22));
	add_coll.setMaximumSize(new Dimension(50,22));
	add_coll.setToolTipText("create new file collection");
	bb.add(add_coll);

	JButton remove_coll = new JButton("-");
	remove_coll.setActionCommand("-c");
	remove_coll.setPreferredSize(new Dimension(50,22));
	remove_coll.setMaximumSize(new Dimension(50,22));
	remove_coll.setToolTipText("delete file collection");
	bb.add(remove_coll);

	JButton open_autoload = new JButton("AL");
	open_autoload.setActionCommand("oa");
	open_autoload.setPreferredSize(new Dimension(50,22));
	open_autoload.setMaximumSize(new Dimension(50,22));
	open_autoload.setToolTipText("open autoload file window");
	bb.add(open_autoload);



	JPanel hh = new JPanel();
	hh.setLayout(new BoxLayout(hh, BoxLayout.Y_AXIS));

	JPanel mm = new JPanel();
	mm.setLayout(new GridLayout(0,1));
	mm.add(new JLabel("output directory:"));

	hh.add(mm);

	JPanel ll = new JPanel();
	ll.setLayout(new GridLayout(0,1));

	outfield = new JTextField();
	outfield.setEditable(false);
	outfield.setToolTipText("specified output directory of selected collection");

	ll.add(outfield);

	hh.add(ll);

	JPanel ii = new JPanel();
	ii.setLayout(new GridLayout(0,2));
	ii.add(new JLabel("recent output directories:"));

	JPanel jj = new JPanel();
	jj.setLayout(new GridLayout(0,2));

	JButton add_output = new JButton("+");
	add_output.setActionCommand("+o");
	add_output.setPreferredSize(new Dimension(50,22));
	add_output.setMaximumSize(new Dimension(50,22));
	add_output.setToolTipText("add directory to recent output directory list");
	jj.add(add_output);

	JButton remove_output = new JButton("-");
	remove_output.setActionCommand("-o");
	remove_output.setPreferredSize(new Dimension(50,22));
	remove_output.setMaximumSize(new Dimension(50,22));
	remove_output.setToolTipText("remove directory from recent output directory list");
	jj.add(remove_output);

	ii.add(jj);

	hh.add(ii);

	JPanel kk = new JPanel();
	kk.setLayout(new GridLayout(0,1));

	comBox[13] = new JComboBox();  // recent output
	comBox[13].setActionCommand("co");
	comBox[13].setMaximumRowCount(8);
	kk.add(comBox[13]);

	hh.add(kk);

	JPanel control_3 = new JPanel(new BorderLayout());
	control_3.setAlignmentX(CENTER_ALIGNMENT);
	control_3.add(hh, BorderLayout.NORTH);

	JPanel control_2 = new JPanel(new BorderLayout());
	control_2.setAlignmentX(CENTER_ALIGNMENT);
	control_2.add(bb, BorderLayout.WEST);
	control_2.add(control_3, BorderLayout.CENTER);

	files.add(control_2);


	add_output.addActionListener(my0Listener);
	remove_output.addActionListener(my0Listener);
	open_autoload.addActionListener(my0Listener);
	file_remove.addActionListener(my0Listener);
	file_down.addActionListener(my0Listener);
	file_up.addActionListener(my0Listener);
	add_coll.addActionListener(my0Listener);
	remove_coll.addActionListener(my0Listener);
	comBox[0].addActionListener(my0Listener);
	comBox[13].addActionListener(my0Listener);


	return files;
}

//DM26032004 081.6 int18 changed
private void close_AutoloadPanel()
{
	autoload.dispose();
}

//DM26032004 081.6 int18 changed
protected void buildAutoloadPanel()
{
	autoload = new JFrame("list of pre-defined input directories");
	autoload.addWindowListener ( new WindowAdapter()
	{
		public void windowClosing(WindowEvent e)
		{
			close_AutoloadPanel();
		}
	});

	JPanel bb = new JPanel();
	bb.setLayout( new ColumnLayout() );

	JButton remove_input = new JButton("-");
	remove_input.setActionCommand("-i");
	remove_input.setPreferredSize(new Dimension(50,28));
	remove_input.setMaximumSize(new Dimension(50,28));
	remove_input.setToolTipText("remove directory from autoload list");
	bb.add(remove_input);

	JButton add_input = new JButton("+");
	add_input.setActionCommand("+i");
	add_input.setPreferredSize(new Dimension(50,28));
	add_input.setMaximumSize(new Dimension(50,24));
	add_input.setToolTipText("add directory to autoload list");
	bb.add(add_input);

	JButton refresh_list = new JButton("o");
	refresh_list.setActionCommand("ri");
	refresh_list.setPreferredSize(new Dimension(50,28));
	refresh_list.setMaximumSize(new Dimension(50,28));
	refresh_list.setToolTipText("refresh file list");
	bb.add(refresh_list);

	bb.add(new JLabel(" "));

	JButton add_coll_and_files = new JButton("+<");
	add_coll_and_files.setPreferredSize(new Dimension(50,28));
	add_coll_and_files.setMaximumSize(new Dimension(50,28));
	add_coll_and_files.setToolTipText("create new collection for each selected file and add it");
	bb.add(add_coll_and_files);

	add_files = new JButton("<");
	add_files.setEnabled(false);
	add_files.setPreferredSize(new Dimension(50,28));
	add_files.setMaximumSize(new Dimension(50,28));
	add_files.setToolTipText("add file(s) to collection");
	bb.add(add_files);

	bb.add(new JLabel(" "));

	JButton close = new JButton("X");
	close.setPreferredSize(new Dimension(50,28));
	close.setMaximumSize(new Dimension(50,28));
	close.setToolTipText("closing window");
	close.addActionListener(new ActionListener()
	{
		public void actionPerformed(ActionEvent e)
		{
			close_AutoloadPanel();
		}
	});
	bb.add(close);


	// in list
	list1 = new JList(inputfiles);
	list1.setName("inl");
	list1.setVisibleRowCount(8);
	list1.setSelectionMode(2);
	list1.setToolTipText("left doubleclick or enter to add file(s) to a collection, right click to rename");
	list1.addMouseListener( new MouseAdapter()
	{
		public void mouseClicked(MouseEvent e)
		{
			int index = list1.locationToIndex( e.getPoint());

			if (e.getClickCount() == 1)
			{
				if (list1.getSelectedValue() != null )
					ScanInfo( list1.getSelectedValue().toString());
			}

			if (e.getClickCount() > 1)
			{
				if (e.getModifiers() == MouseEvent.BUTTON3_MASK && index > -1) // rename file
				{
					String inparent = new File( list1.getSelectedValue().toString()).getParent();
					String inchild = new File( list1.getSelectedValue().toString()).getName();

					if ( !inparent.endsWith(filesep) )  
						inparent += filesep;

					String inputval = JOptionPane.showInputDialog(frame, inchild , "rename " + inparent + inchild, JOptionPane.QUESTION_MESSAGE );

					if (inputval != null && !inputval.equals(""))
					{
						if (new File(inparent + inputval).exists())
						{
							int opt = JOptionPane.showConfirmDialog(frame, "File exists! Overwrite?");

							if (opt == JOptionPane.YES_OPTION)
							{
								new File(inparent + inputval).delete();

								Common.renameTo(inparent + inchild, inparent + inputval); //DM13042004 081.7 int01 changed
								//new File(inparent + inchild).renameTo(new File(inparent + inputval));

								inputlist();
							}
						}
						else
						{
							Common.renameTo(inparent + inchild, inparent + inputval); //DM13042004 081.7 int01 changed
							//new File(inparent + inchild).renameTo(new File(inparent + inputval));

							inputlist();
						}
					}
					autoload.toFront();
				}
				else if (e.getModifiers() == MouseEvent.BUTTON1_MASK && index > -1) // add file to coll
				{
					if (!add_files.isEnabled())
					{
						int ix = comBox[0].getItemCount();
						ArrayList[] cf = collfiles;
						collfiles = new ArrayList[ix+1];
						collfiles[ix] = new ArrayList();

						System.arraycopy(cf, 0, collfiles, 0, cf.length);

						if (comBox[13].getItemCount() > 0) 
							collout.add(comBox[13].getSelectedItem());
						else 
							collout.add(outalias);

						comchange = true;
						comBox[0].addItem("" + ix);
						speciallist.add(new ArrayList());
						cutlist.add(new ArrayList());
						comchange = false;
						comBox[0].setSelectedIndex(ix);
					}
					Object[] val = list1.getSelectedValues();

					if (val.length > 0)
					{
						int icf = comBox[0].getSelectedIndex();

						for (int a=0; a<val.length; a++) 
							collfiles[icf].add(val[a]);

						list3.setListData(collfiles[icf].toArray());
					}
				}
			}
		}
	});

	list1.addKeyListener( new KeyAdapter()
	{
		public void keyPressed(KeyEvent e)
		{
			if (e.getKeyChar() == KeyEvent.VK_ENTER)
			{
				if (!add_files.isEnabled())
				{
					int ix = comBox[0].getItemCount();
					ArrayList[] cf = collfiles;
					collfiles = new ArrayList[ix+1];
					collfiles[ix] = new ArrayList();
					System.arraycopy(cf, 0, collfiles, 0, cf.length);

					if (comBox[13].getItemCount() > 0) 
						collout.add(comBox[13].getSelectedItem());
 					else 
						collout.add(outalias);

					comchange = true;
					comBox[0].addItem("" + ix);
					speciallist.add(new ArrayList());
					cutlist.add(new ArrayList());
					comchange = false;

					comBox[0].setSelectedIndex(ix);
				}
				Object[] val = list1.getSelectedValues();

				if (val.length > 0)
				{
					int icf = comBox[0].getSelectedIndex();

					for (int a=0; a<val.length; a++) 
						collfiles[icf].add(val[a]);

					list3.setListData(collfiles[icf].toArray());
				}
			}
		}
	});

	JScrollPane scrolltext = new JScrollPane();
	scrolltext.setViewportView(list1);

	//dropTarget_2 = new DropTarget(list1, dnd);

	comBox[12] = new JComboBox();  // recent input
	comBox[12].setMaximumRowCount(8);
	comBox[12].setPreferredSize(new Dimension(400,24));

	JPanel control_1 = new JPanel(new BorderLayout());
	control_1.setAlignmentX(CENTER_ALIGNMENT);
	control_1.add(scrolltext, BorderLayout.CENTER);
	control_1.add(comBox[12], BorderLayout.NORTH);

	JPanel control_2 = new JPanel(new BorderLayout());
	control_2.setAlignmentX(CENTER_ALIGNMENT);
	control_2.add(control_1, BorderLayout.CENTER);
	control_2.add(bb, BorderLayout.WEST);


	refresh_list.addActionListener(my0Listener);
	add_files.addActionListener(my0Listener);
	add_coll_and_files.addActionListener(my0Listener);
	add_input.addActionListener(my0Listener);
	remove_input.addActionListener(my0Listener);

	autoload.getContentPane().add(control_2);

	UIManager.addPropertyChangeListener(new UISwitchListener(control_2));

	autoload.setBounds(200,200,500,300);
}

//main panel  ,DM26032004 081.6 int18 changed
protected JPanel buildMainPanel()
{
	JPanel main = new JPanel();
	main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));

	doitButton = new JButton("Go!");
	doitButton.setMnemonic('g');
	doitButton.addActionListener(my2Listener);

	JPanel control05 = new JPanel();
	control05.setLayout(new ColumnLayout());
	control05.add(doitButton);

	brm = new BRMonitor();
	brm.setToolTipText("actual Bitrate of GOP from 0...10Mbps (depends on the kind of computing) / GOP structure / videotime");
	control05.add(brm);

	outSize = new JLabel("out MB");
	outSize.setToolTipText("written MB of new created files of collection (without temporaries)");
	control05.add(outSize);

	scanButton = new JButton("i");
	scanButton.setMnemonic('i');
	scanButton.setMaximumSize(new Dimension(45,22));
	scanButton.setPreferredSize(new Dimension(45,22));
	scanButton.setToolTipText("short processing to inform about the IDs found in streamfile (in specified filesize, see out)");
	scanButton.setEnabled(true);
	scanButton.addActionListener(my2Listener);

	breakButton = new JButton("C");
	breakButton.setMnemonic('c');
	breakButton.setMaximumSize(new Dimension(45,22));
	breakButton.setPreferredSize(new Dimension(45,22));
	breakButton.setToolTipText("cancel the current process");
	breakButton.setEnabled(false);
	breakButton.addActionListener(my2Listener);

	pauseButton = new JButton("P");
	pauseButton.setMnemonic('p');
	pauseButton.setMaximumSize(new Dimension(45,22));
	pauseButton.setPreferredSize(new Dimension(45,22));
	pauseButton.setEnabled(false);
	pauseButton.setToolTipText("stop(pause) or continue the current process");
	pauseButton.addActionListener(my2Listener);

	comBox[9] = new JComboBox();
	comBox[9].setMaximumSize(new Dimension(45,22));
	comBox[9].setPreferredSize(new Dimension(45,22));
	comBox[9].setMaximumRowCount(5);

	extract = new JButton("e");
	extract.setMaximumSize(new Dimension(45,22));
	extract.setPreferredSize(new Dimension(45,22));
	extract.setMnemonic('e');
	extract.setEnabled(false);
	extract.setToolTipText("PVA raw file extraction, using the specified ID:  (press 'i' to get the found IDs)");
	extract.addActionListener(my2Listener);

	JPanel control07 = new JPanel();
	control07.setLayout(new ColumnLayout());
	control07.add(scanButton);
	control07.add(breakButton);
	control07.add(pauseButton);
	control07.add(extract);
	control07.add(comBox[9]);


	JPanel control06 = new JPanel();
	control06.setLayout(new BoxLayout(control06, BoxLayout.X_AXIS));
	control06.add(control05);
	control06.add(control07);

	JPanel control08 = new JPanel();
	control08.setBorder(BorderFactory.createTitledBorder( BorderFactory.createRaisedBevelBorder(), "work"));
	control08.setToolTipText("if not DEMUX, it converts a streamformat to an other (stream content copy, no error correction!!)");
	control08.setLayout(new ColumnLayout());

	control08.add(control06);
	control08.add(Box.createRigidArea(new Dimension(1,5)));

	//DM14062004 081.7 int04 changed
	Object[] convertTo = { 
		"demux",
		"to VDR",
		"to M2P",
		"to PVA",
		"to TS",
		"PIDfilter"
	};
	comBox[19] = new JComboBox(convertTo);
	comBox[19].setPreferredSize(new Dimension(100,22));
	comBox[19].setMaximumSize(new Dimension(100,22));
	comBox[19].setSelectedIndex(0);
	control08.add(comBox[19]);

	cBox[18] = new JCheckBox("all coll's");
	cBox[18].setPreferredSize(new Dimension(100,20));
	cBox[18].setMaximumSize(new Dimension(100,20));
	cBox[18].setToolTipText("enable to work with all collections in one run");
	control08.add(cBox[18]);

	cBox[25] = new JCheckBox("post process.");
	cBox[25].setPreferredSize(new Dimension(110,20));
	cBox[25].setMaximumSize(new Dimension(110,20));
	cBox[25].setToolTipText("enable post processing (->see external panel, if field is not empty )");
	control08.add(cBox[25]);

	cBox[14] = new JCheckBox("MPG=>sPES");
	cBox[14].setPreferredSize(new Dimension(100,20));
	cBox[14].setMaximumSize(new Dimension(100,20));
	cBox[14].setToolTipText("simple PES parsing, enable for MPEG-PS including data w/o a regular sub ID of private_stream_1");
	control08.add(cBox[14]);

	msoff = new JLabel("A/V offset");
	msoff.setToolTipText("<html>shows the A/V timeoffset at the beginning of each GOP in ms:  a/b/c <p>(cuts causes different offsets)<p>a) A/V PTS offset in source <p>b) A/V frame begin offset for actual GOP in demuxed file <p>c) new real A/V offset for actual GOP in demuxed file</html>");
	msoff.setPreferredSize(new Dimension(100,20));
	msoff.setMaximumSize(new Dimension(100,20));
	control08.add(msoff);

	audiostatusLabel = new JLabel("not started");
	audiostatusLabel.setPreferredSize(new Dimension(100,20));
	audiostatusLabel.setMaximumSize(new Dimension(100,20));
	audiostatusLabel.setToolTipText("status of audioexport or written subpictures/pages");
	control08.add(audiostatusLabel);

	JPanel control01 = new JPanel();
	control01.setLayout(new BoxLayout(control01, BoxLayout.X_AXIS));
	control01.add(control08);

	JPanel log = buildLogPanel();
	control01.add(log);

	main.add(control01);

	return main;
}

//DM20032004 081.6 int18 add
protected JPanel buildLogPanel()
{
	JPanel panel = new JPanel();
	panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
	panel.setBorder(BorderFactory.createEmptyBorder(5,2,2,2));

	JPanel logwindow = buildlogwindowPanel();
	JPanel fileinfo = buildfileinfoPanel();
	JPanel msg = buildMessagePanel(); //DM14052004 081.7 int02 add
	JPanel split = buildsplitPanel();
	JPanel ids = buildidPanel();
	JPanel video1 = buildvideo1Panel();
	JPanel extern = buildexternPanel();
	JPanel audio = buildaudioPanel();
	JPanel subtitle = buildsubtitlePanel(); //DM18052004 0817. int02 changed
	JPanel option = buildoptionPanel();

	logtab = new JTabbedPane();
	logtab.addTab( "logwindow", logwindow );
	logtab.setSelectedIndex(0);
	logtab.addTab( "info", fileinfo ); //DM14052004 081.7 int02 changed
	logtab.addTab( "msg", msg ); //DM14052004 081.7 int02 add
	logtab.addTab( "out", split );
	logtab.addTab( "special", ids );
	logtab.addTab( "video", video1 );
	logtab.addTab( "audio", audio );
	logtab.addTab( "subtitle", subtitle ); //DM18052004 0817. int02 changed
	logtab.addTab( "extern", extern );
	logtab.addTab( "options", option );

	panel.add(logtab, BorderLayout.CENTER);

	return panel;
}

//DM20032004 081.6 int18 add
protected JPanel buildlogwindowPanel()
{
	JPanel panel = new JPanel();
	panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

	JPanel main5 = new JPanel();
	main5.setLayout(new BorderLayout());

	cBox[19] = new JCheckBox("TTX: ");
	cBox[19].setToolTipText("shows ttx header line and VPS status (decrease speed, if enabled)");
	main5.add(cBox[19], BorderLayout.WEST);

	ttxheaderLabel = new JLabel("");
	ttxheaderLabel.setToolTipText("header line of actual transmitted page (row 0)");
	main5.add(ttxheaderLabel, BorderLayout.CENTER);

	ttxvpsLabel = new JLabel("");
	ttxvpsLabel.setToolTipText("short VPS status (VBI-line 16), a few stations have meaningful VPS data via DVB-teletext");
	main5.add(ttxvpsLabel, BorderLayout.EAST);

	JPanel main6 = new JPanel();
	main6.setLayout(new GridLayout(1,1));
	main6.add(main5);

	JScrollPane scrolltext = new JScrollPane();
	TextArea = new LogArea();
	scrolltext.setViewportView(TextArea);
	viewport = scrolltext.getViewport();
	//viewport.setScrollMode(JViewport.BLIT_SCROLL_MODE); //enable for >= JDK1.3	
	//viewport.setScrollMode(JViewport.BACKINGSTORE_SCROLL_MODE); //alternative, enable for >= JDK1.3	
	//viewport.setBackingStoreEnabled(true); // enable for < JDK1.3 

	JPanel control04 = new JPanel(new BorderLayout());
	control04.setAlignmentX(CENTER_ALIGNMENT);
	control04.add(main6, BorderLayout.NORTH);
	control04.add(scrolltext, BorderLayout.CENTER);

	panel.add(control04);

	return panel;
}

//DM20032004 081.6 int18 add
protected JPanel buildfileinfoPanel()
{
	JPanel panel = new JPanel();
	panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

	JScrollPane scrolltext = new JScrollPane();
	FileInfoTextArea = new LogArea();
	scrolltext.setViewportView(FileInfoTextArea);
	viewport_2 = scrolltext.getViewport();

	panel.add(scrolltext);

	return panel;
}

//status panel  DM26032004 081.6 int18 changed
protected JPanel buildStatusPanel()
{
	JPanel status = new JPanel();
	status.setLayout(new BoxLayout(status, BoxLayout.X_AXIS));

	status.add(new JLabel("Status:  "));

	progress = new JProgressBar();
	progress.setString("ready...");
	progress.setStringPainted(true);
	progress.addChangeListener(new ChangeListener()
	{
		public void stateChanged(ChangeEvent e)
		{
			if (running)
			{
				System.out.print("\r" + (int)(progress.getPercentComplete() * 100) + "% ");
				System.out.print("" + progress.getString());
			}
			else
				frame.setTitle("" + (int)(progress.getPercentComplete() * 100) + "% (" + currentcoll + ") " + frametitle);
		}
	});

	status.add(progress);

	splitLabel = new JLabel(" " + options[19] + " ");
	splitLabel.setToolTipText("actual part of splitted output");
	status.add(splitLabel);

	return status;
}

//DM14052004 081.7 int02 new, smth moved
protected JPanel buildMessagePanel()
{
	JPanel msgPanel = new JPanel();
	msgPanel.setLayout( new GridLayout(1,2) );

	JPanel msgPanel_1 = new JPanel();
	msgPanel_1.setLayout ( new ColumnLayout() );
	msgPanel_1.setBorder( BorderFactory.createTitledBorder("message handling") );

	cBox[40] = new JCheckBox("don't message 'packets out of sequence'");
	cBox[40].setToolTipText("enable, if this message bombs the logwindow (and decrease the speed)");
	cBox[40].setPreferredSize(new Dimension(300,20));
	cBox[40].setMaximumSize(new Dimension(300,20));
	cBox[40].setSelected(false);
	msgPanel_1.add(cBox[40]);

	cBox[3] = new JCheckBox("don't message 'missing startcode'");
	cBox[3].setToolTipText("enable, if this message bombs the logwindow (and decrease the speed)");
	cBox[3].setPreferredSize(new Dimension(250,20));
	cBox[3].setMaximumSize(new Dimension(250,20));
	cBox[3].setSelected(false);
	msgPanel_1.add(cBox[3]);

	msgPanel.add(msgPanel_1);
	return msgPanel;
}

//DM14052004 081.7 int02 changed
protected JPanel buildidPanel()
{
	JPanel idbigPanel = new JPanel();
	idbigPanel.setLayout( new GridLayout(1,2) );

	JPanel idPanel3 = new JPanel();
	idPanel3.setLayout ( new ColumnLayout() );
	idPanel3.setBorder( BorderFactory.createTitledBorder("specials 1") );

	//DM14052004 081.7 int02 moved++
	JLabel gpts = new JLabel("global PTS shift (in hours): ");
	gpts.setToolTipText("try 'auto or 1..13', in case of source A/V PTS mismatch, or if it doesn't demux audio (std=0)");
	comBox[27] = new JComboBox();
	comBox[27].addItem("auto");
	for (int d=0;d<14;d++) 
		comBox[27].addItem(""+d);
	comBox[27].setPreferredSize(new Dimension(60,20));
	comBox[27].setMaximumSize(new Dimension(60,20));
	comBox[27].setSelectedIndex(1);
	comBox[27].setEditable(true); //DM26022004 081.6 int18 add

	JPanel spec5 = new JPanel();
	spec5.setLayout(new BoxLayout(spec5, BoxLayout.X_AXIS));
	spec5.add(gpts);  
	spec5.add(comBox[27]);  
	idPanel3.add(spec5);
	//DM14052004 081.7 int02 moved--

	//DM14052004 081.7 int02 moved
	cBox[48] = new JCheckBox("check for overlapping PVAs on read");
	cBox[48].setPreferredSize(new Dimension(250,20));
	cBox[48].setMaximumSize(new Dimension(250,20));
	cBox[48].setSelected(false);
	cBox[48].setToolTipText("disable, if multiple PVA's of one recording has been recorded without overlap");
	idPanel3.add(cBox[48]);

	//DM14052004 081.7 int02 moved
	cBox[28] = new JCheckBox("strictly PVA specs. for audio streams");
	cBox[28].setToolTipText("enable to avoid audio detection errors; but on some recordings there are missing flags! (MD)");
	cBox[28].setPreferredSize(new Dimension(250,20));
	cBox[28].setMaximumSize(new Dimension(250,20));
	idPanel3.add(cBox[28]);

	//DM14052004 081.7 int02 add
	idPanel3.add(new JLabel("only TS related:"));

	cBox[38] = new JCheckBox("ignore scrambled packets");
	cBox[38].setToolTipText("disable, if you think the packets are not, but marked as scrambled");
	cBox[38].setPreferredSize(new Dimension(250,20));
	cBox[38].setMaximumSize(new Dimension(250,20));
	cBox[38].setSelected(true);
	idPanel3.add(cBox[38]);

	//DM15072004 081.7 int06 add
	cBox[61] = new JCheckBox("enhanced search for open packets");
	cBox[61].setToolTipText("enable, if the PID detection stops the search at unknown payload");
	cBox[61].setPreferredSize(new Dimension(250,20));
	cBox[61].setMaximumSize(new Dimension(250,20));
	idPanel3.add(cBox[61]);

	//Ghost23012004 081.6 int11 add
	//DM14052004 081.7 int02 moved
	cBox[53] = new JCheckBox("join TS file segments (of Dreambox)");
	cBox[53].setToolTipText("enable to join incomplete TS packets on boundaries of multiple file segments");
	cBox[53].setPreferredSize(new Dimension(250,20));
	cBox[53].setMaximumSize(new Dimension(250,20));
	idPanel3.add(cBox[53]);

	cBox[41] = new JCheckBox("generate PMT stream dependent");
	cBox[41].setToolTipText("disable, to generate an independent pre-defined PMT that also fit most streams");
	cBox[41].setSelected(true);
	cBox[41].setPreferredSize(new Dimension(250,20));
	cBox[41].setMaximumSize(new Dimension(250,20));
	idPanel3.add(cBox[41]);

	cBox[42] = new JCheckBox("generate info TTX service (test)");
	cBox[42].setToolTipText("add a teletext service stream (Pid 0x9F) for status informations on page 150 (only @ autoPMT)"); //DM10032004 081.6 int18 changed
	cBox[42].setSelected(false);
	cBox[42].setPreferredSize(new Dimension(250,20));
	cBox[42].setMaximumSize(new Dimension(250,20));
	idPanel3.add(cBox[42]);

	/**
	cBox[35] = new JCheckBox("add Topfield TF4000 header to TS"); //DM10032004 081.6 int18 changed
	cBox[35].setSelected(false);
	cBox[35].setPreferredSize(new Dimension(250,20));
	cBox[35].setMaximumSize(new Dimension(250,20));
	//idPanel3.add(cBox[35]);
	**/

	//DM09082004 081.7 int08 add
	Object ts_headers[] = { "no additional TS header", "add TF4000 header to TS", "add TF5000 header to TS" };
	comBox[20] = new JComboBox(ts_headers);
	comBox[20].setPreferredSize(new Dimension(250,22));
	comBox[20].setMaximumSize(new Dimension(250,22));
	idPanel3.add(comBox[20]);

	cBox[37] = new JCheckBox("TF-header: set AC3 as main AudioTrack");
	cBox[37].setSelected(false);
	cBox[37].setPreferredSize(new Dimension(250,20));
	cBox[37].setMaximumSize(new Dimension(250,20));
	idPanel3.add(cBox[37]);

	idbigPanel.add(idPanel3);


	JPanel idPanel2 = new JPanel();
	idPanel2.setLayout ( new ColumnLayout() );
	idPanel2.setBorder( BorderFactory.createTitledBorder("specials 2") );

	//DM14052004 081.7 int02 moved
	//DM15072004 081.7 int06 changed
	cBox[33] = new JCheckBox("get only enclosed PES/TS packets");
	cBox[33].setToolTipText("disable, if no packets were found (on packet end, the next mpeg-startcode is missing, e.g. on Images)");
	cBox[33].setSelected(true);
	cBox[33].setPreferredSize(new Dimension(250,20));
	cBox[33].setMaximumSize(new Dimension(250,20));
	idPanel2.add(cBox[33]);

	cBox[49] = new JCheckBox("concatenate different recordings");
	cBox[49].setPreferredSize(new Dimension(250,20));
	cBox[49].setMaximumSize(new Dimension(250,20));
	cBox[49].setSelected(true);
	cBox[49].setToolTipText("enable, if you want merge equal types of PVA, MPEG or VDR files from different recording times");
	idPanel2.add(cBox[49]);

	cBox[16] = new JCheckBox("rename all MPEG-Audios to *.mpa");
	cBox[16].setToolTipText("instead of using the Layer number -> .mp1, .mp2, .mp3");
	cBox[16].addActionListener(mytabListener);
	cBox[16].setPreferredSize(new Dimension(250,20));
	cBox[16].setMaximumSize(new Dimension(250,20));
	idPanel2.add(cBox[16]);

	cBox[32] = new JCheckBox("rename all MPEG-Videos to *.mpv");
	cBox[32].setToolTipText("instead of using the MPEG type -> .m1v, .m2v");
	cBox[32].setSelected(true);
	cBox[32].setPreferredSize(new Dimension(250,20));
	cBox[32].setMaximumSize(new Dimension(250,20));
	idPanel2.add(cBox[32]);

	cBox[24] = new JCheckBox("use only first Audio PTS for sync");
	cBox[24].setToolTipText("counts the audiotime from its framelengths after syncing the A/V-startpoint, skip errors");
	cBox[24].setPreferredSize(new Dimension(250,20));
	cBox[24].setMaximumSize(new Dimension(250,20));
	idPanel2.add(cBox[24]);

	//DM151003 081.5++ simplesync
	cBox[15] = new JCheckBox("limit points of Audio PTS for sync");
	cBox[15].setToolTipText("better than 1st Audio PTS only (as above), Audio may keep in sync");
	cBox[15].setPreferredSize(new Dimension(250,20));
	cBox[15].setMaximumSize(new Dimension(250,20));
	idPanel2.add(cBox[15]);

	cBox[39] = new JCheckBox("ignore Video errors after 1st PTS/GOP");
	cBox[39].setToolTipText("enable, if probably only Video PTS errors occurs (Pics seems ok)");
	cBox[39].setSelected(false);
	cBox[39].setPreferredSize(new Dimension(250,20));
	cBox[39].setMaximumSize(new Dimension(250,20));
	idPanel2.add(cBox[39]);

	//DM14052004 081.7 int02 add
	idPanel2.add(new JLabel("only to'XYZ' conversions related:"));

	cBox[23] = new JCheckBox("ensure 1st PES-packet start with video");
	cBox[23].setToolTipText("enable, if some players have problems if it's not;  disable for audio-only conversions");
	cBox[23].setSelected(true);
	cBox[23].setPreferredSize(new Dimension(270,20));
	cBox[23].setMaximumSize(new Dimension(270,20));
	idPanel2.add(cBox[23]);

	//DM14052004 081.7 int02 moved++
	cBox[36] = new JCheckBox("generate PCR/SCR from PTS");
	cBox[36].setSelected(true);
	cBox[36].setToolTipText("specify the PCR-Offset, DVB needs more than DVD, high bitrates needs more than low");
	cBox[36].setPreferredSize(new Dimension(192,20));
	cBox[36].setMaximumSize(new Dimension(192,20));

	cBox[46] = new JCheckBox("incTScnt");
	cBox[46].setSelected(false);
	cBox[46].setToolTipText("increment TS-packet counter even without payload(PCR-only); non-conform, but sometimes necessary!");
	cBox[46].setPreferredSize(new Dimension(80,20));
	cBox[46].setMaximumSize(new Dimension(80,20));

	JPanel spec3 = new JPanel();
	spec3.setLayout(new BoxLayout(spec3, BoxLayout.X_AXIS));
	spec3.add(cBox[36]);  
	spec3.add(cBox[46]);  
	idPanel2.add(spec3);

	Object[] pcrdelta = { "25000","35000","45000","55000","65000","80000","100000","125000","150000" };
	comBox[23] = new JComboBox(pcrdelta);
	comBox[23].setMaximumRowCount(7);
	comBox[23].setEditable(true);
	comBox[23].setPreferredSize(new Dimension(100,20));
	comBox[23].setMaximumSize(new Dimension(100,20));
	comBox[23].setSelectedIndex(4);
	comBox[23].addActionListener(mytabListener);
	idPanel2.add(comBox[23]);
	//DM14052004 081.7 int02 moved--

	idbigPanel.add(idPanel2);

	return idbigPanel;
}


protected JPanel buildsplitPanel()
{
	JPanel splits = new JPanel();
	splits.setLayout( new GridLayout(2, 2) );

	JPanel op1 = new JPanel();
	op1.setLayout( new ColumnLayout() );
	op1.setBorder( BorderFactory.createTitledBorder("split output") );

	cBox[5] = new JCheckBox("split @ appr. xxx MB");
	cBox[5].setSelected(false);
	cBox[5].setToolTipText("edit field for another size; currently, split size is referred to written videofilesize! (mpv)");

	Object[] es = { "650","700","735","792","2000","4700" };
	comBox[2] = new JComboBox(es);
	comBox[2].setMaximumRowCount(6);
	comBox[2].setEditable(true); 
	comBox[2].setEnabled(false); 
	comBox[2].setSelectedIndex(0);
	comBox[2].setPreferredSize(new Dimension(100,22));
	comBox[2].setMaximumSize(new Dimension(100,22));

	JPanel sp1 = new JPanel();
	sp1.setLayout(new BoxLayout(sp1, BoxLayout.X_AXIS));
	sp1.add(cBox[5]);  
	sp1.add(comBox[2]);  
	op1.add(sp1);

	Object[] so = { "no overlap","1 MB","2 MB","3 MB","4 MB","5 MB","6 MB","7 MB","8 MB","9 MB","10 MB" };
	comBox[25] = new JComboBox(so);
	comBox[25].setMaximumRowCount(6);
	comBox[25].setPreferredSize(new Dimension(110,22));
	comBox[25].setMaximumSize(new Dimension(110,22));
	comBox[25].setSelectedIndex(0);
	op1.add(comBox[25]);
	op1.add(new JLabel("if enabled, each collection will be splitted!"));
	op1.add(new JLabel("disabled, if main source doesn't contain video"));

	splits.add(op1);

	comBox[2].addActionListener(mytabListener);
	cBox[5].addActionListener(mytabListener);


	// main id panel disabled with 081.7 int06 
 	// is now replaced by a global stream type 'enabler'
	JPanel idPanel = new JPanel();
	idPanel.setBorder(BorderFactory.createTitledBorder("stream types to process"));
	idPanel.setLayout(new BoxLayout(idPanel, BoxLayout.X_AXIS));
	idPanel.setToolTipText("de-/select stream types you want to process (does not applies to ES as input)");

	cBox[55] = new JCheckBox("Mpg Video");
	cBox[55].setSelected(true);
	cBox[56] = new JCheckBox("Mpg Audio");
	cBox[56].setSelected(true);
	cBox[57] = new JCheckBox("AC3/DTS Audio");
	cBox[57].setSelected(true);
	cBox[58] = new JCheckBox("LPCM Audio");
	cBox[58].setSelected(true);
	cBox[59] = new JCheckBox("Teletext");
	cBox[59].setSelected(true);
	cBox[60] = new JCheckBox("Subpicture");
	cBox[60].setSelected(true);

	JPanel panel_1 = new JPanel();
	panel_1.setLayout ( new ColumnLayout() );
	panel_1.add(cBox[55]);
	panel_1.add(cBox[56]);
	panel_1.add(cBox[57]);

	JPanel panel_2 = new JPanel();
	panel_2.setLayout ( new ColumnLayout() );
	panel_2.add(cBox[58]);
	panel_2.add(cBox[59]);
	panel_2.add(cBox[60]);

	idPanel.add(panel_1);
	idPanel.add(panel_2);

	splits.add(idPanel);



	JPanel op4 = new JPanel();
	op4.setLayout( new ColumnLayout() );
	op4.setBorder( BorderFactory.createTitledBorder("write options for conversion & demux") );
	op4.setToolTipText("that affects also the 'to VDR/MPG/PVA/TS' function to include Audio and/or Video ES");

	cBox[6] = new JCheckBox("write all video data");
	cBox[6].setSelected(true);
	cBox[6].addActionListener(mytabListener);

	cBox[7] = new JCheckBox("write all other data"); //DM13042004 081.7 int01 changed
	cBox[7].setSelected(true);
	cBox[7].addActionListener(mytabListener);

	op4.add(cBox[6]);
	op4.add(cBox[7]);

	JPanel op6 = new JPanel();
	op6.setLayout(new BoxLayout(op6, BoxLayout.X_AXIS));
	Object[] fs = { "5","10","25" };

	comBox[21] = new JComboBox(fs);
	comBox[21].setSelectedIndex(0);
	op6.add(new JLabel("quick demux/convert of 1st xx MB :"));
	op6.add(comBox[21]);

	op4.add(op6);

	splits.add(op4);


	JPanel op5 = new JPanel();
	op5.setLayout( new ColumnLayout() );
	op5.setBorder( BorderFactory.createTitledBorder("additional time offset") );

	cBox[8] = new JCheckBox("enable offset");
	cBox[8].setSelected(false);
	cBox[8].setToolTipText("original start of stream will be moved with the given offset");

	comBox[8] = new JComboBox();
	comBox[8].setMaximumRowCount(1);
	comBox[8].addItem("0");
	comBox[8].setEditable(true); 
	comBox[8].setEnabled(false); 

	comBox[8].addActionListener(mytabListener);
	cBox[8].addActionListener(mytabListener);

	op5.add(cBox[8]);
	op5.add(comBox[8]);
	op5.add(new JLabel("enter time offset in milliseconds, hit enter"));
	op5.add(new JLabel("affects all streams, which follows on a video"));

	splits.add(op5);

	return splits;
}


/***** 1st panel ****/
protected JPanel buildvideo1Panel() {
	JPanel video1 = new JPanel();
	video1.setLayout( new GridLayout(1, 2) );

	//DM30122003 081.6 int10 moved+
	JPanel video2Panel = new JPanel();
	video2Panel.setLayout( new ColumnLayout() );
	video2Panel.setBorder( BorderFactory.createTitledBorder("video corrections") );

	JPanel vbvsPanel = new JPanel();
	vbvsPanel.setLayout(new BoxLayout(vbvsPanel, BoxLayout.X_AXIS));
	vbvsPanel.setToolTipText("sometimes it's better to define the maximum size");

	comBox[4] = new JComboBox();
	comBox[4].addItem("don't change");
	comBox[4].addItem("maximum (112*16384)");
	comBox[4].setSelectedIndex(0);
	comBox[4].setPreferredSize(new Dimension(150,20));
	comBox[4].setMaximumSize(new Dimension(150,20));
	vbvsPanel.add(new JLabel ("vbv buffer:  "));
	vbvsPanel.add(comBox[4]);

	JPanel vbvdPanel = new JPanel();
	vbvdPanel.setLayout(new BoxLayout(vbvdPanel, BoxLayout.X_AXIS));
	vbvdPanel.setToolTipText("max. time recommended");
	comBox[5] = new JComboBox();
	comBox[5].addItem("don't change");
	comBox[5].addItem("max. time (0xFFFF)");
	comBox[5].setSelectedIndex(1);
	comBox[5].setPreferredSize(new Dimension(150,20));
	comBox[5].setMaximumSize(new Dimension(150,20));
	vbvdPanel.add(new JLabel ("vbv delay:  "));
	vbvdPanel.add(comBox[5]);

	JPanel aspPanel = new JPanel();
	aspPanel.setLayout(new BoxLayout(aspPanel, BoxLayout.X_AXIS));
	aspPanel.setToolTipText("change if you mean it's necessary");
	comBox[6] = new JComboBox(aspratio);
	comBox[6].setSelectedIndex(0);
	comBox[6].setPreferredSize(new Dimension(150,20));
	comBox[6].setMaximumSize(new Dimension(150,20));
	aspPanel.add(new JLabel ("aspect ratio: "));
	aspPanel.add(comBox[6]);

	cBox[13] = new JCheckBox("add sequence end code");
	cBox[13].setPreferredSize(new Dimension(250,20));
	cBox[13].setMaximumSize(new Dimension(250,20));
	cBox[13].setSelected(true);
	cBox[13].setToolTipText("enable for a conform MPEG sequence end");

	cBox[31] = new JCheckBox("patch all frames to progressive");
	cBox[31].setPreferredSize(new Dimension(250,20));
	cBox[31].setMaximumSize(new Dimension(250,20));
	cBox[31].setActionCommand("prog1");
	cBox[31].setSelected(false);
	cBox[31].setToolTipText("set the progressive bits to reduce the field number");

	cBox[44] = new JCheckBox("patch all frames to interlaced");
	cBox[44].setPreferredSize(new Dimension(250,20));
	cBox[44].setMaximumSize(new Dimension(250,20));
	cBox[44].setActionCommand("prog2");
	cBox[44].setSelected(false);
	cBox[44].setToolTipText("delete the progressive bits");

	cBox[45] = new JCheckBox("change field order");
	cBox[45].setPreferredSize(new Dimension(250,20));
	cBox[45].setMaximumSize(new Dimension(250,20));
	cBox[45].setSelected(false);
	cBox[45].setToolTipText("toggle the Top_field_first bit");

	cBox[27] = new JCheckBox("ensure each GOP has a sequenceheader");
	cBox[27].setPreferredSize(new Dimension(250,20));
	cBox[27].setMaximumSize(new Dimension(250,20));
	cBox[27].setSelected(false);
	cBox[27].setToolTipText("re-init (resol. & q_matrix) of Picturedecoding on each GOP boundary (automatically on split)");

	video2Panel.add(vbvsPanel);
	video2Panel.add(vbvdPanel);
	video2Panel.add(aspPanel);
	video2Panel.add(cBox[13]);
	video2Panel.add(cBox[31]);
	video2Panel.add(cBox[44]);
	video2Panel.add(cBox[45]);
	video2Panel.add(cBox[27]);

	//JLA14082003+
	JPanel hPPanel = new JPanel();
	hPPanel.setLayout(new BoxLayout(hPPanel, BoxLayout.X_AXIS));
	hPPanel.setToolTipText("conditional patch 1st horiz. resolution");
	hPPanel.add(new JLabel ("patch 1st h-res: "));

	Object[] cHorizontalPatch = { "never","ever","if <> 352|720","if <> to 352|704|720" };
	comBox[35]= new JComboBox(cHorizontalPatch);
	comBox[35].setPreferredSize(new Dimension(150,20));
	comBox[35].setMaximumSize(new Dimension(150,20));
	hPPanel.add(comBox[35]);

	video2Panel.add(hPPanel);

	Object[] reso = { "352","384","480","528","544","576","640","704","720" };
	comBox[22] = new JComboBox(reso);
	comBox[22].setPreferredSize(new Dimension(50,20));
	comBox[22].setMaximumSize(new Dimension(50,20));
	comBox[22].setSelectedIndex(0);

	video2Panel.add(comBox[22]);

	comBox[35].addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			comBox[22].setEnabled(comBox[35].getSelectedIndex()!=0);
		}
	});
	comBox[35].setSelectedIndex(0);
	//JLA14082003-

	comBox[5].addActionListener(mytabListener);
	comBox[4].addActionListener(mytabListener);
	comBox[6].addActionListener(mytabListener);
	cBox[13].addActionListener(mytabListener);
	cBox[31].addActionListener(mytabListener);
	cBox[44].addActionListener(mytabListener);

	video1.add(video2Panel);
	//DM30122003 081.6 int10 moved-


	JPanel newBrPanel = new JPanel();
	newBrPanel.setLayout(new ColumnLayout());
	newBrPanel.setBorder( BorderFactory.createTitledBorder("new bitrate values") );

	Object BRperSequence[] = {
		"keep original","computed from GOP bitlength","computed from VBV","mark as VBR (MPEG-1 Video)",
		"fix 1 Mbps","fix 2 Mbps","fix 3 Mbps","fix 4 Mbps","fix 5 Mbps","fix 6 Mbps",
		"fix 7 Mbps","fix 8 Mbps","fix 9 Mbps","fix 10 Mbps","fix 11 Mbps",
		"fix 12 Mbps","fix 13 Mbps","fix 14 Mbps","fix 15 Mbps"
	};
	comBox[3] = new JComboBox(BRperSequence);
	comBox[3].setPreferredSize(new Dimension(250,20));
	comBox[3].setMaximumSize(new Dimension(250,20));
	comBox[3].setSelectedIndex(1);
	newBrPanel.add(new JLabel("bitrate values per sequence:"));
	newBrPanel.add(comBox[3]);

        comBox[3].addActionListener(mytabListener);

	Object BRperFile[] = {
		"keep original",
		"computed average (nominal)",
		"computed maximum <= 9.8 Mbps(DVD)",
		"computed maximum (e.g. HDTV)",
		"mark as VBR (MPEG-1 Video)"
	};
	comBox[15] = new JComboBox(BRperFile);
	comBox[15].setPreferredSize(new Dimension(250,20));
	comBox[15].setMaximumSize(new Dimension(250,20));
	comBox[15].setSelectedIndex(2);
	newBrPanel.add(new JLabel("bitrate value in first sequence:"));
	newBrPanel.add(comBox[15]);

	comBox[15].addActionListener(mytabListener);

	video1.add(newBrPanel);

	return video1;
}


/**** 2nd panel ****/
protected JPanel buildexternPanel() { //DM30122003 081.6 int10 changed
	JPanel video2 = new JPanel();
	video2.setLayout( new GridLayout(1, 2) );

	JPanel video2Panel = new JPanel();
	video2Panel.setLayout( new ColumnLayout() );
	video2Panel.setBorder( BorderFactory.createTitledBorder("externals / post processing") );
	video2Panel.setToolTipText("define your prefered applications");

	exeButton = new JButton("external applications dialog");
	exeButton.setActionCommand("exec");
	exeButton.setPreferredSize(new Dimension(250,25));
	video2Panel.add(exeButton);
	exeButton.addActionListener(my0Listener);

	//DM18022004 081.6 int17 new
	cBox[54] = new JCheckBox("create index.vdr when making VDR");
	cBox[54].setPreferredSize(new Dimension(250,20));
	cBox[54].setMaximumSize(new Dimension(250,20));
	cBox[54].setToolTipText("created vdr file segments will then be renamed to 00x.vdr");
	video2Panel.add(cBox[54]);

	//DM14052004 081.7 int02 moved
	cBox[26] = new JCheckBox("create cellTimes.txt for multiple infiles/cuts");
	cBox[26].setToolTipText("demux only, add's an entry for the framenumber on file switchin' and cut-in's (new chapter)");
	cBox[26].setPreferredSize(new Dimension(270,20));
	cBox[26].setMaximumSize(new Dimension(270,20));
	video2Panel.add(cBox[26]);

	//DM18022004 081.6 int17 new
	RButton[11] = new JRadioButton("autosave PTS values of cutpoints");
	RButton[11].setPreferredSize(new Dimension(250,20));
	RButton[11].setMaximumSize(new Dimension(250,20));
	RButton[11].setToolTipText("to share and reload with {PTS cut} in another X instance");
	video2Panel.add(RButton[11]);

	//DM26022004 081.6 int18 new
	RButton[12] = new JRadioButton("autosave GOPs 1st I-Frame as .bmp");
	RButton[12].setPreferredSize(new Dimension(250,20));
	RButton[12].setMaximumSize(new Dimension(250,20));
	RButton[12].setToolTipText("demux only...");
	video2Panel.add(RButton[12]);

	video2.add(video2Panel);

	JPanel video3Panel = new JPanel();
	video3Panel.setLayout( new ColumnLayout() );
	video3Panel.setBorder( BorderFactory.createTitledBorder("external Projectfile settings") );
	video3Panel.setToolTipText("change the entries, if you know what you do");

	//mpeg2schnitt, JLA06082003+, DM24112003
	cBox[34] = new JCheckBox("Mpeg2Schnitt idd version V2/A3");
	cBox[34].setSelected(false);
	cBox[34].setPreferredSize(new Dimension(250,20));
	cBox[34].setMaximumSize(new Dimension(250,20));
	cBox[34].setToolTipText("...create *.idd index file on demux:");
	cBox[34].setActionCommand("idd");
	video3Panel.add(new JLabel("create *.idd Projectfile on demux:"));
	video3Panel.add(cBox[34]);
	//JLA06082003-

	video3Panel.add(new JLabel("create *.d2v Projectfile on demux:"));

	cBox[29] = new JCheckBox("for each demuxed videofile");
	cBox[29].setSelected(false);
	cBox[29].setPreferredSize(new Dimension(250,20));
	cBox[29].setMaximumSize(new Dimension(250,20));
	cBox[29].setToolTipText("if each part should be converted separatly");
	cBox[29].setActionCommand("d2v1");

	cBox[30] = new JCheckBox("auto split video, but keep as one project");
	cBox[30].setSelected(false);
	cBox[30].setPreferredSize(new Dimension(250,20));
	cBox[30].setMaximumSize(new Dimension(250,20));
	cBox[30].setToolTipText("auto split demuxed video in specified part size, not audio; better for big files on <=FAT32 systems");
	cBox[30].setActionCommand("d2v2");
	video3Panel.add(cBox[29]);
	video3Panel.add(cBox[30]);

	d2vfield[5] = new JTextField("2048");
	d2vfield[5].setEditable(true);
	d2vfield[5].setPreferredSize(new Dimension(70,20));

	JPanel d2vPanel = new JPanel();
	JLabel d2vLabel = new JLabel ("-> split only video at xxx MB: ");
	d2vPanel.add(d2vLabel);
	d2vPanel.add(d2vfield[5]);
	video3Panel.add(d2vPanel);

	cBox[29].addActionListener(mytabListener);
	cBox[30].addActionListener(mytabListener);

	String[] d2vtips = { "2 = 32bit SSE MMX ; 3 = 64bit float ; 4 = IEEE-1180 Reference",
				"0 = PC Scale ; 1 = TV Scale",
				"Gain (0..255) std=128 , Offset (-256..256) no=0",
				"Crop: top , bottom , left , right , LB W. , LB H. (ensure, that values are multiples of 8 or 16)",
				"0 = none ; 1 = forced film ; 2 = swap field order" };
	String[] d2vopt = d2v.readOptions();
	for (int a=0;a<5;a++) {
		d2vfield[a] = new JTextField(d2vopt[a]);
		d2vfield[a].setEditable(true);
		d2vfield[a].setPreferredSize(new Dimension(250,20));
		d2vfield[a].setToolTipText(d2vtips[a]);
		video3Panel.add(d2vfield[a]);
	}
	video2.add(video3Panel);

	return video2;
}

/***** 3rd panel ****/
protected JPanel buildaudioPanel() {

	JPanel audio = new JPanel();
	audio.setLayout( new GridLayout(1,2) );

	JPanel audio0 = new JPanel();
	audio0.setLayout( new ColumnLayout() );
	audio0.setBorder( BorderFactory.createTitledBorder("audio conversions") );

	audio0.add(new JLabel("direct losless conversion (still limited"));
	audio0.add(new JLabel("to MPEG-1, Layer2, 48kHz, 56..384kbps) :"));
	audio0.setToolTipText("if conversion is chosen, bitrate & channelmode will be constant over the complete file");

	Object conversionmode[] = { 
		"no conversion",
		"single to dual (ch.A=original, B=quiet)",
		"single to stereo (L+R = original)",
		"single to jointstereo (L+R = original)",
		"split 2channel into 2 single (1=L,2=R)" 
	};
	comBox[7] = new JComboBox(conversionmode);
	comBox[7].setPreferredSize(new Dimension(250,20));
	comBox[7].setMaximumSize(new Dimension(250,20));
	audio0.add(comBox[7]);
        comBox[7].addActionListener(mytabListener);

	audio0.add(new JLabel(" "));

	cBox[50] = new JCheckBox("decode MPEG Layer1,2 to PCM:");
	cBox[50].setToolTipText("MPEG-Audio Decoder (not for Layer3), std output into stereo pcm");
	cBox[50].setSelected(false);
	cBox[50].setPreferredSize(new Dimension(250,20));
	cBox[50].setMaximumSize(new Dimension(250,20));
	audio0.add(cBox[50]);
        cBox[50].addActionListener(mytabListener);

	Object resamplemode[] = { 
		"no resampling",
		"linear resampling 48kHz to 32kHz",
		"linear resampling 48kHz to 44.1kHz" 
	};
	comBox[1] = new JComboBox(resamplemode);
	comBox[1].setPreferredSize(new Dimension(250,20));
	comBox[1].setMaximumSize(new Dimension(250,20));
	audio0.add(comBox[1]);

	//DM10042004 081.7 int01 changed
	RButton[2] = new JRadioButton("normalize to % : ");
	RButton[2].setPreferredSize(new Dimension(120,20));
	RButton[2].setMaximumSize(new Dimension(120,20));
	RButton[2].setToolTipText("normalize L+R equally, values from 0...100 are possible, std is 98%; forces restarts if necessary");

	exefield[8] = new JTextField("98");  //DM10042004 081.7 int01 changed
	exefield[8].setPreferredSize(new Dimension(50,20));
	exefield[8].setMaximumSize(new Dimension(50,20));

	JPanel audio5 = new JPanel();
	audio5.setLayout(new BoxLayout(audio5, BoxLayout.X_AXIS));
	audio5.add(RButton[2]);
	audio5.add(exefield[8]);
	audio0.add(audio5);

	RButton[3] = new JRadioButton("downmix and output as one channel");
	RButton[3].setPreferredSize(new Dimension(250,20));
	RButton[3].setMaximumSize(new Dimension(250,20));
	RButton[3].setToolTipText("downmix stereo/2channel source to one single channel");
	audio0.add(RButton[3]);

	RButton[4] = new JRadioButton("save in Motorola byte order");
	RButton[4].setPreferredSize(new Dimension(250,20));
	RButton[4].setMaximumSize(new Dimension(250,20));
	RButton[4].setToolTipText("== MSB first, nonIntel type");
	audio0.add(RButton[4]);

	RButton[5] = new JRadioButton("add RIFF(RIFX) header to PCM (.wav)");
	RButton[5].setPreferredSize(new Dimension(250,20));
	RButton[5].setMaximumSize(new Dimension(250,20));
	RButton[5].setToolTipText("PC standard");
	RButton[5].setSelected(true);
	RButton[5].setActionCommand("riff");//DM07022004 081.6 int16 add
	audio0.add(RButton[5]);

	//DM07022004 081.6 int16 new
	RButton[9] = new JRadioButton("add AIFF header to PCM (.aif)");
	RButton[9].setPreferredSize(new Dimension(250,20));
	RButton[9].setMaximumSize(new Dimension(250,20));
	RButton[9].setToolTipText("Apple standard");
	RButton[9].setSelected(false);
	RButton[9].setActionCommand("aiff");
	audio0.add(RButton[9]);

	//DM07022004 081.6 int16 new
	RButton[4].addActionListener(mytabListener); //dummy
	RButton[5].addActionListener(mytabListener);
	RButton[9].addActionListener(mytabListener);

	JPanel audio1 = new JPanel();
	audio1.setLayout( new ColumnLayout() );
	audio1.setBorder( BorderFactory.createTitledBorder("audio manipulations") );

	//DM10042004 081.7 int01 add
	RButton[16] = new JRadioButton("check CRC of AC-3 / MPEG-Audio L1,2");
	RButton[16].setToolTipText("if enabled, this ensures lowest erroneous output on costs of performance");
	RButton[16].setPreferredSize(new Dimension(250,20));
	RButton[16].setMaximumSize(new Dimension(250,20));

	cBox[1] = new JCheckBox("delete CRC in MPEG-Audio Layer1,2");
	cBox[1].setToolTipText("if enabled, this may prevent some audio error messages");
	cBox[1].setPreferredSize(new Dimension(250,20));
	cBox[1].setMaximumSize(new Dimension(250,20));

	cBox[0] = new JCheckBox("fill gaps with prev. frame");
	cBox[0].setToolTipText("instead of inserting silent frames; is std for AC-3, when ac3.bin doesn't contain a suitable silent frame for AC-3");
	cBox[0].setSelected(false);

	cBox[20] = new JCheckBox("add frames");
	cBox[20].setToolTipText("if audio ends before video, to reach the video time length");
	cBox[20].setSelected(true);

	cBox[9] = new JCheckBox("patch 1st AC-3 header to 3/2 ch-mode");
	cBox[9].setToolTipText("some AC-3 decoding software needs it to decode all channels even if there are less");
	cBox[9].setPreferredSize(new Dimension(250,20));
	cBox[9].setMaximumSize(new Dimension(250,20));

	cBox[10] = new JCheckBox("replace all non-3/2 AC-3 by 3/2lfe silence");
	cBox[10].setToolTipText("enable, to mask unwanted 2/0 frames by 3/2 silence (48khz,448kbps, ac3.bin required)");
	cBox[10].setPreferredSize(new Dimension(250,20));
	cBox[10].setMaximumSize(new Dimension(250,20));

	cBox[12] = new JCheckBox("add RIFF WAVE header to AC-3 Audio");
	cBox[12].setToolTipText("create a .wav file, using the AC-3 Wave Format (tag 0x2000) Header");
	cBox[12].setSelected(false);

	JPanel audio3 = new JPanel();
	audio3.setLayout(new BoxLayout(audio3, BoxLayout.X_AXIS));
	audio3.setPreferredSize(new Dimension(250,20));
	audio3.setMaximumSize(new Dimension(250,20));
	audio3.add(cBox[0]);  
	audio3.add(cBox[20]);  

	audio1.add(RButton[16]); //DM10042004 081.7 int01 add
	audio1.add(cBox[1]);
	audio1.add(audio3);  
	audio1.add(cBox[10]);
	audio1.add(cBox[9]);  
	audio1.add(cBox[12]);

	cBox[4] = new JCheckBox("add RIFF WAVE header to MPEGAudio: ");
	cBox[4].setToolTipText("create a .wav file, using the Broadcast (tag 0x0050) or ACM (0x0055) Wave Format Header");
	cBox[4].setSelected(false);
	cBox[4].setPreferredSize(new Dimension(250,20));
	cBox[4].setMaximumSize(new Dimension(250,20));
	cBox[4].addActionListener(mytabListener);
	audio1.add(cBox[4]);

	JPanel audio2 = new JPanel();
	audio2.setLayout(new BoxLayout(audio2, BoxLayout.X_AXIS));
	audio2.setPreferredSize(new Dimension(250,20));
	audio2.setMaximumSize(new Dimension(250,20));

	RButton[14] = new JRadioButton("Layer1,2");
	RButton[14].setToolTipText("Broadcast Wave Format (tag 0x0050), better for MPEG Layer 1 & 2 ");
	RButton[14].setSelected(true);

	RButton[15] = new JRadioButton("Layer3");
	RButton[15].setToolTipText("more usual ACM Wave Format (tag 0x0055), better for MPEG Layer 3 ");

	ButtonGroup audioGroup2 = new ButtonGroup();
	audioGroup2.add(RButton[14]);
	audioGroup2.add(RButton[15]);   
	audio2.add(new JLabel("choose tag:  "));
	audio2.add(RButton[14]);
	audio2.add(RButton[15]);
	audio1.add(audio2);

	cBox[51] = new JCheckBox("discard every xxx Audioframe: ");
	cBox[51].setToolTipText("compress/pitch Audio (ES only)");

	JPanel audio4 = new JPanel();
	audio4.setLayout(new BoxLayout(audio4, BoxLayout.X_AXIS));
	audio4.setPreferredSize(new Dimension(260,20));
	audio4.setMaximumSize(new Dimension(260,20));
	audio4.add(cBox[51]);

	d2vfield[7] = new JTextField("0");
	d2vfield[7].setPreferredSize(new Dimension(60,20));
	d2vfield[7].setMaximumSize(new Dimension(60,20));
	audio4.add(d2vfield[7]);

	audio1.add(new JLabel(" "));
	audio1.add(audio4);

	//DM30122003 081.6 int10 new

	RButton[7] = new JRadioButton("allow spaces between frames (h.w.c.!)");
	RButton[7].setPreferredSize(new Dimension(250,20));
	RButton[7].setMaximumSize(new Dimension(250,20));
	RButton[7].setToolTipText("only detect valid frame headers, but without verifying frame length! (resulting in psb. incorrect sizes)");
	RButton[7].setSelected(false);
	audio1.add(RButton[7]);


	cBox[1].addActionListener(mytabListener);
	cBox[0].addActionListener(mytabListener);

	audio.add(audio0);
	audio.add(audio1);

	return audio;
}


//DM18052004 0817. int02 changed
protected JPanel buildsubtitlePanel()
{
	JPanel teletext = new JPanel();
	teletext.setLayout( new GridLayout(1,0) );

	JPanel tt0 = new JPanel();
	tt0.setLayout( new ColumnLayout() );
	tt0.setBorder( BorderFactory.createTitledBorder("DVB teletext, DVB subtitles") ); //DM18052004 0817. int02 changed

	cBox[17] = new JCheckBox("test only: export MegaRadio MP3-stream (from NBC/Giga Teletext)");
	cBox[17].setToolTipText("formerly transmitted via Hotbird 13°E, 11.054GHz Hor., SR 27500, FEC 5/6, TTX-PID 553");
	cBox[17].setPreferredSize(new Dimension(500,22));
	cBox[17].setMaximumSize(new Dimension(500,22));
	tt0.add(cBox[17]);

	cBox[22] = new JCheckBox("advanced: decode hidden rows of teletext (id 0xFF, means offline pages)");
	cBox[22].setToolTipText("some pages or complete teletexts/subtitles are hidden, so that may help");
	cBox[22].setPreferredSize(new Dimension(500,22));
	cBox[22].setMaximumSize(new Dimension(500,22));
	tt0.add(cBox[22]);

	//DM22072004 081.7 int07 add
	cBox[62] = new JCheckBox("advanced: re-build TTX-PTS from 1st MpgAudio stream in a stream file");
	cBox[62].setToolTipText("use it ONLY for TTX streams included in a stream file (TS,PES..) but w/o proper time base to video");
	cBox[62].setPreferredSize(new Dimension(500,22));
	cBox[62].setMaximumSize(new Dimension(500,22));
	tt0.add(cBox[62]);

	//DM09032004 081.6 int18 add
	RButton[13] = new JRadioButton("advanced: keep original Timecode (PTS) on independent decoding");
	RButton[13].setToolTipText("without a leading video to demux, new Timecode doesn't start at 0");
	RButton[13].setPreferredSize(new Dimension(500,25));
	RButton[13].setMaximumSize(new Dimension(500,25));
	tt0.add(RButton[13]);

	JLabel page_decode = new JLabel("teletext pages to decode: (fields editable, set to 'null' to ignore a field)");
	page_decode.setToolTipText("autodetection of special characters based on Teletext Level 1.5 codepages in unicode");
	tt0.add(page_decode);

	Object[] pagenumber = {"null","149","150","691","692","693","694","777","779","784","785","786","881","882","884","885","886","887","888","889"};

	JPanel tt1 = new JPanel();
	tt1.setLayout(new BoxLayout(tt1, BoxLayout.X_AXIS));

	for (int p=0;p<6;p++) {
		comBox[28+p] = new JComboBox(pagenumber);
		comBox[28+p].setPreferredSize(new Dimension(60,25));
		comBox[28+p].setEditable(true);
		tt1.add(comBox[28+p]);
	}

	//DM09082004 081.7 int08 add++
	JLabel lang_decode = new JLabel("  language: ");
	lang_decode.setToolTipText("fix to a language pair, if an automatic switch is not supported by the broadcaster");
	tt1.add(lang_decode);

	Object lang_pairs[] = { 
		"auto", 
		"basic LATIN", 
		"force polish", 
		"force turkish", 
		"force cro,rom", 
		"force est,rus",
		"res.",
		"force greek", 
		"res.", 
		"force arabic", 
		"res.", 
		"force hebrew"
	};

	comBox[18] = new JComboBox(lang_pairs);
	comBox[18].setPreferredSize(new Dimension(120,25));
	tt1.add(comBox[18]);
	//DM09082004 081.7 int08 add--

	tt0.add(tt1);

	JPanel ttPanel = new JPanel();
	ttPanel.setLayout(new BoxLayout(ttPanel, BoxLayout.X_AXIS));
	ttPanel.setToolTipText("select your favourite export format");

	ButtonGroup ttGroup = new ButtonGroup();
	RButton[18] = new JRadioButton("free");
	RButton[18].setToolTipText("teletext only; free text format ");

	RButton[19] = new JRadioButton("SC");
	RButton[19].setToolTipText("teletext only; text, special sub format 1 (timecount & multilines)");

	RButton[20] = new JRadioButton("SUB");
	RButton[20].setToolTipText("teletext only; text, special sub format 2 (framecount & multilines)");
	RButton[20].setSelected(true);

	RButton[21] = new JRadioButton("SRT");
	RButton[21].setToolTipText("teletext only; text, special sub format 3 (timecount & multilines)");

	RButton[24] = new JRadioButton("STL");
	RButton[24].setToolTipText("teletext only; text, special sub format 4 (timecount & multilines)");

	RButton[22] = new JRadioButton("SSA");
	RButton[22].setToolTipText("teletext only; text, Sub Station Alpha Script V4, with color support");

	//DM14052004 081.7 int02 add
	RButton[17] = new JRadioButton("SON");
	RButton[17].setToolTipText("DVB sub's only; text + binary(bitmap), special sub format 5 (timecount & multilines)");

	RButton[23] = new JRadioButton("SUP");
	RButton[23].setToolTipText("teletext, DVB & DVD sub's; binary, 2-bit coloured RLE");

	ttGroup.add(RButton[17]); //DM14052004 081.7 int02 add
	ttGroup.add(RButton[18]);
	ttGroup.add(RButton[19]);
	ttGroup.add(RButton[20]);
	ttGroup.add(RButton[21]);
	ttGroup.add(RButton[22]);
	ttGroup.add(RButton[23]);
	ttGroup.add(RButton[24]);

	ttPanel.add(new JLabel("subtitle export formats: "));
	ttPanel.add(RButton[18]);
	ttPanel.add(RButton[19]);
	ttPanel.add(RButton[20]);
	ttPanel.add(RButton[21]);
	ttPanel.add(RButton[24]);
	ttPanel.add(RButton[22]);
	ttPanel.add(RButton[17]); //DM14052004 081.7 int02 add
	ttPanel.add(RButton[23]);

	tt0.add(ttPanel);

	//DM24042004 081.7 int02 add++
	Object model[] = { "4 colors", "16 colors", "256 colors" };
	comBox[11] = new JComboBox(model);
	comBox[11].setSelectedIndex(1);
	comBox[11].setPreferredSize(new Dimension(100,22));
	comBox[11].setMaximumSize(new Dimension(100,22));

	d2vfield[9] = new JTextField("");
	d2vfield[9].setEditable(true);
	d2vfield[9].setPreferredSize(new Dimension(40,22));
	d2vfield[9].setMaximumSize(new Dimension(100,22));
	d2vfield[9].setToolTipText("empty field = use what comes, else: key in the page ID of interest (depends on broadcast)");

	JPanel IRDPanel = new JPanel();
	IRDPanel.setLayout(new BoxLayout(IRDPanel, BoxLayout.X_AXIS));

	JLabel color_model = new JLabel("simulate DVB Subpictures IRD color model:  ");
	color_model.setToolTipText("that extremely affects the look of exported subtitles, best setting depends on what you want");

	IRDPanel.add(color_model);
	IRDPanel.add(comBox[11]);
	IRDPanel.add(new JLabel("  fix to pageID: "));
	IRDPanel.add(d2vfield[9]);

	tt0.add(IRDPanel);
	//DM24042004 081.7 int02 add--

	tt0.add(new JLabel("settings for SUP format only:"));

	picButton = new JButton("show Subtitle Preview");
	picButton.setPreferredSize(new Dimension(180,25));
	picButton.setActionCommand("picview");
	picButton.addActionListener(my0Listener);

	Object[] fonts = (Object[])GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
	comBox[26] = new JComboBox(fonts);
	comBox[26].setSelectedItem("SansSerif");
	comBox[26].setPreferredSize(new Dimension(150,25));

	d2vfield[6] = new JTextField("26;10;32;80;560;720;576;-1;4"); //DM26052004 081.7 int03 changed
	d2vfield[6].setEditable(true);
	d2vfield[6].setPreferredSize(new Dimension(170,20));
	d2vfield[6].setToolTipText("(offsets from left bottom) Font pointsize; BckGrd Alpha; Yoffset; Xoffset; Xwidth; H(unused); V; Yoffset2");

	JPanel picPanel = new JPanel();
	picPanel.setLayout(new BoxLayout(picPanel, BoxLayout.X_AXIS));
	picPanel.add(picButton);
	picPanel.add(comBox[26]);
	picPanel.add(new JLabel(" Val:"));
	picPanel.add(d2vfield[6]);
	tt0.add(picPanel);

	teletext.add(tt0);

	cBox[17].addActionListener(mytabListener);

	return teletext;
}


/***** 4rd panel ****/
protected JPanel buildoptionPanel() {
	JPanel option = new JPanel();
	option.setLayout( new GridLayout(1,2) );

	JPanel op0 = new JPanel();
	op0.setLayout( new ColumnLayout() );
	op0.setBorder( BorderFactory.createTitledBorder("various") );

	comBox[16] = new JComboBox();
	comBox[16].setPreferredSize(new Dimension(250,25));
	op0.add(new JLabel("installed look & feels:"));
	op0.add(comBox[16]);
	op0.add(new JLabel("choose your preferred look & feel"));
	op0.add(new JLabel("sometimes a l&f crashes internally"));

	comBox[16].addActionListener( new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			if (!comchange) {
				String lnfName = comBox[16].getSelectedItem().toString();
				if (lnfName!=null && !lnfName.equals("")) {
					try 
					{
					UIManager.setLookAndFeel(lnfName);
					SwingUtilities.updateComponentTreeUI(frame);
					if(chooser != null) 
						SwingUtilities.updateComponentTreeUI(chooser);
					} 
					catch (Exception exc) {
						comBox[16].removeItemAt(comBox[16].getSelectedIndex());
						System.err.println("Could not load LookAndFeel: " + lnfName);
					}
				}
			}
		}
 	});


	cBox[11] = new JCheckBox("big log file");
	cBox[11].setToolTipText("reduce speed, only for test purposes");
	cBox[11].setActionCommand("biglog");
	op0.add(cBox[11]);

	cBox[21] = new JCheckBox("normal log file");
	cBox[21].setToolTipText("writes the logwindow content to file");
	cBox[21].setActionCommand("normallog");
	op0.add(cBox[21]);

	//DM24012004 081.6 int11 moved
	cBox[43] = new JCheckBox("dump dropped GOPs to separate files");
	cBox[43].setToolTipText("only meant for later error analysis");
	cBox[43].setPreferredSize(new Dimension(250,20));
	cBox[43].setMaximumSize(new Dimension(250,20));
	cBox[43].setSelected(false);
	op0.add(cBox[43]);

	//DM12122003 081.6 int05
	d2vfield[8] = new JTextField("");
	d2vfield[8].setPreferredSize(new Dimension(250,25));
	d2vfield[8].setToolTipText("leave empty, or type in a start path, a leading '?' ever resets to this path before");
	op0.add(new JLabel("starting path for file selection:"));
	op0.add(d2vfield[8]);

	//DM30122003 081.6 int10
	RButton[8] = new JRadioButton("append new file extensions (PES)");
	RButton[8].setPreferredSize(new Dimension(250,25));
	RButton[8].setToolTipText("will keep the source file name (incl. extension) of all secondary PES streams");
	RButton[8].setSelected(false);
	op0.add(RButton[8]);

	option.add(op0);

	JPanel op2 = new JPanel();
	op2.setLayout( new ColumnLayout() );
	op2.setBorder( BorderFactory.createTitledBorder("Buffer") );

	Object[] bufsize = { "10240000","8192000","7168000","6144000","5120000","4096000","3072000","2048000","1024000" };
	comBox[10]=new JComboBox(bufsize);
	comBox[10].setSelectedIndex(3);
	comBox[10].setEditable(true); //DM21112003 081.5++
	comBox[10].setPreferredSize(new Dimension(100,25));
	comBox[10].setMaximumSize(new Dimension(100,25));

	JLabel iob = new JLabel("main I/O-Buffersize in bytes");
	iob.setToolTipText("lower values may prevent internal memory overflows -> OutOfMemory error");
	op2.add(iob);
	op2.add(comBox[10]);

	//DM21112003 081.5++
	Object[] vdrprebufsize = { "16384","65536","131072","196608","262144","327680","524288","655360","1048576","2097152","3145728" };
	comBox[36]=new JComboBox(vdrprebufsize);
	comBox[36].setSelectedIndex(3);
	comBox[36].setEditable(true);
	comBox[36].setPreferredSize(new Dimension(100,25));
	comBox[36].setMaximumSize(new Dimension(100,25));
	JLabel viob = new JLabel("I-Buffersize in bytes (only PES packetsize=0 !)");
	viob.setToolTipText("try higher values if you work with high bitrate videostreams");
	op2.add(viob);
	op2.add(comBox[36]);

	//DM04122003 081.6_int02
	//DM04072004 081.7 int06 changed
	Object[] scan = { "384000","512000","1024000","1536000","2048000","2560000","3072000" };
	comBox[37]=new JComboBox(scan);
	comBox[37].setSelectedItem("1024000");
	comBox[37].setEditable(true);
	comBox[37].setPreferredSize(new Dimension(100,25));
	comBox[37].setMaximumSize(new Dimension(100,25));
	JLabel scanL = new JLabel("pre-Scan Buffersize in bytes");
	scanL.setToolTipText("higher values if known filetypes are not detected");
	op2.add(scanL);
	op2.add(comBox[37]);


	//DM04052004 081.7 int02 add
	JButton garbagecollector = new JButton("garbage collector");
	garbagecollector.setToolTipText("calls the garbage collector");
	op2.add(new JLabel(" "));
	op2.add(garbagecollector);
	garbagecollector.addActionListener( new ActionListener()
	{
		public void actionPerformed(ActionEvent e)
		{
			System.gc();
		}
 	});


	option.add(op2);

	cBox[11].addActionListener(mytabListener);
	cBox[21].addActionListener(mytabListener);
	comBox[37].addActionListener(mytabListener);

	return option;
}


//DM26032004 081.6 int18 moved
class MenuListener implements ActionListener
{
	public void actionPerformed(ActionEvent e)
	{
		String actName = e.getActionCommand();

		if (actName.equals("sendTo3"))
		{
			if (comBox[0].getItemCount() > 0)
			{
				Object[] indc = list3.getSelectedValues();

				if (indc.length>0)
				{
					String item = exefield[2].getText().toString();
					String exe = item+" \""+indc[0].toString()+"\"";
					try 
					{
						Runtime.getRuntime().exec(exe);
					}
					catch (Exception ex) {
						Msg(" ! execution error: "+ex); 
					}
				} 
			}
		}

		else if (actName.equals("helphtml"))
			new Html().show();

		else if (actName.equals("about"))
			showAboutBox();

		else if (actName.equals("openCut"))
		{
			if (comBox[0].getItemCount() > 0)
			{
				activecoll = comBox[0].getSelectedIndex(); 
				dialog.entry();
			}
		}
		else if (actName.equals("add"))
		{
			if (!add_files.isEnabled())
			{
				int ix = comBox[0].getItemCount();
				ArrayList[] cf = collfiles;
				collfiles = new ArrayList[ix+1];
				collfiles[ix] = new ArrayList();
				System.arraycopy(cf,0,collfiles,0,cf.length);

				if (comBox[13].getItemCount()>0) 
					collout.add(comBox[13].getSelectedItem());
				else 
					collout.add(outalias);

				comchange=true;
				comBox[0].addItem(""+ix);
				speciallist.add(new ArrayList());
				cutlist.add(new ArrayList());
				comchange=false;
				comBox[0].setSelectedIndex(ix);
			}
 
			chooser.rescanCurrentDirectory();
			chooser.setDialogType(JFileChooser.OPEN_DIALOG);
			int retval = chooser.showDialog(frame, null);
			if(retval == JFileChooser.APPROVE_OPTION)
			{
				File theFile = chooser.getSelectedFile();
				if(theFile != null && theFile.isFile())
				{
					int icf = comBox[0].getSelectedIndex();
					collfiles[icf].add(theFile.getAbsolutePath());
					list3.setListData(collfiles[icf].toArray());
				}
				return;
			}
		}
		else if (actName.equals("remove"))
		{
			int[] indc = list3.getSelectedIndices();
			if (indc.length > 0)
			{
				int icf = comBox[0].getSelectedIndex();

				for (int a=indc.length-1;a>-1;a--) 
					collfiles[icf].remove(indc[a]);

				list3.setListData(collfiles[icf].toArray());
			}
		}
		else if (actName.equals("rename"))
		{
			if (list3.isSelectionEmpty()) //DM12042004 081.7 int01 add
				return;

			String inparent = new File( list3.getSelectedValue().toString()).getParent();
			String inchild = new File( list3.getSelectedValue().toString()).getName();

			if ( !inparent.endsWith(filesep) )  
				inparent += filesep;

			String inputval = JOptionPane.showInputDialog(frame, inchild , "rename " + inparent + inchild, JOptionPane.QUESTION_MESSAGE );

			if (inputval != null && !inputval.equals(""))
			{
				if (new File(inparent + inputval).exists())
				{
					int opt = JOptionPane.showConfirmDialog(frame, "File exists! Overwrite?");

					if (opt == JOptionPane.YES_OPTION)
					{
						new File(inparent + inputval).delete();

						Common.renameTo(inparent + inchild, inparent + inputval); //DM13042004 081.7 int01 changed
						//new File(inparent + inchild).renameTo(new File(inparent + inputval));

						inputlist();
					}
				}
				else
				{
					Common.renameTo(inparent + inchild, inparent + inputval); //DM13042004 081.7 int01 changed
					//new File(inparent + inchild).renameTo(new File(inparent + inputval));

					inputlist();
				}
			}
		}

		else if (actName.equals("viewAsHex"))
		{
			if (new File(scan.getFile()).exists())
				new HexViewer().view(scan.getFile());
		}

		else if (actName.equals("editBasics"))
		{
			if (scan.isEditable()) {
				vpatch.entry(scan.getFile());
				ScanInfo(scan.getFile());
			}
		}

		else if (actName.equals("subpreview"))
			subpicture.show();  //DM18052004 081.7 int02 changed

		//DM17042004 081.7 int02 add
		else if (actName.equals("pagematrix"))
			tpm.show();

		else if (actName.equals("exit"))
		{
			inisave();
			System.exit(0); 
		}
	}
}

/*******************************
 * An ActionListener for files *
 *******************************/
//DM26032004 081.6 int18 changed
class FileListener implements ActionListener
{
	public void actionPerformed(ActionEvent e)
	{

		String actName = e.getActionCommand();
		String bb = "";

		if (actName.equals("add"))
		{
			chooser.rescanCurrentDirectory();
			chooser.setDialogType(JFileChooser.OPEN_DIALOG);
			int retval = chooser.showDialog(frame, null);

			if(retval == JFileChooser.APPROVE_OPTION)
			{
				File theFile = chooser.getSelectedFile();
				if(theFile != null && theFile.isFile())
				{
					bb = theFile.getAbsolutePath(); 

					if (comBox[0].getItemAt(0)=="") 
						comBox[0].removeItemAt(0);

					comBox[0].addItem(bb);
					comBox[0].setSelectedIndex(comBox[0].getItemCount()-1);
				}
				return;
			}
		}
		else if (actName.equals("remove"))
		{
			if (comBox[0].getItemCount()>0)
				comBox[0].removeItemAt(comBox[0].getSelectedIndex());
		}
		else if (actName.equals("+i"))
		{
			chooser.rescanCurrentDirectory();
			chooser.setDialogType(JFileChooser.OPEN_DIALOG);
			chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			int retval = chooser.showDialog(frame, null);
			if(retval == JFileChooser.APPROVE_OPTION)
			{
				File theFile = chooser.getSelectedFile();
				if(theFile!=null)
				{
					if (theFile.isFile()) 
						bb = theFile.getParent(); 
					else if (theFile.isDirectory()) 
						bb = theFile.getAbsolutePath(); 

					for (int i=0; i<comBox[12].getItemCount();i++)
						if (bb.equalsIgnoreCase(comBox[12].getItemAt(i).toString())) 
							return;

					comBox[12].addItem(bb);
					comBox[12].setSelectedIndex(comBox[12].getItemCount()-1);
					inputlist();
				}
				autoload.toFront(); //DM26032004 081.6 int18 add
				return;
			}
			autoload.toFront(); //DM26032004 081.6 int18 add
		}
		else if (actName.equals("-i"))
		{
			if (comBox[12].getItemCount()>0) 
				comBox[12].removeItemAt(comBox[12].getSelectedIndex());
			inputlist();
		}
		else if (actName.equals("ri"))  
			inputlist(); 
		else if (actName.equals("+o")) {
			outchange=true;
			chooser.rescanCurrentDirectory();
			chooser.setDialogType(JFileChooser.OPEN_DIALOG);
			chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			int retval = chooser.showDialog(frame, null);
			if(retval == JFileChooser.APPROVE_OPTION) {
				File theFile = chooser.getSelectedFile();
				if(theFile!=null) {
					if (theFile.isFile()) 
						bb = theFile.getParent(); 
					else if (theFile.isDirectory()) 
						bb = theFile.getAbsolutePath(); 
					for (int i=0; i<comBox[13].getItemCount();i++)
						if (bb.equalsIgnoreCase(comBox[13].getItemAt(i).toString())) 
							return;
					comBox[13].addItem(bb);
					outchange=false;
					comBox[13].setSelectedIndex(comBox[13].getItemCount()-1);
				}
				return;
			}
		}
		else if (actName.equals("-o")) {
			outchange=true;
			if (comBox[13].getItemCount()>0) 
				comBox[13].removeItemAt(comBox[13].getSelectedIndex());
			outchange=false;
		}
		else if (actName.equals("+c")) {
			int ix = comBox[0].getItemCount();
			ArrayList[] cf = collfiles;
			collfiles = new ArrayList[ix+1];
			collfiles[ix] = new ArrayList();
			System.arraycopy(cf,0,collfiles,0,cf.length);
			if (comBox[13].getItemCount()>0) 
				collout.add(comBox[13].getSelectedItem());
			else 
				collout.add(outalias);
			comchange=true;
			comBox[0].addItem(""+ix);
			speciallist.add(new ArrayList());
			cutlist.add(new ArrayList());
			comchange=false;
			comBox[0].setSelectedIndex(ix);
		}
		else if (actName.equals("-c")) {
			if (comBox[0].getItemCount()>0) {
				int ix = comBox[0].getSelectedIndex();
				int cl = comBox[0].getItemCount();
				ArrayList[] cf = collfiles;
				collfiles = new ArrayList[cl-1];
				if (cl-1>0) {
					System.arraycopy(cf,0,collfiles,0,ix);
					if (ix<cf.length-1) 
						System.arraycopy(cf,ix+1,collfiles,ix,cf.length-ix-1);
				} 
				collout.remove(ix);
				speciallist.remove(ix);
				cutlist.remove(ix);
				comchange=true;
				comBox[0].removeAllItems();
				for (int a=0;a<cl-1;a++) 
				comBox[0].addItem(""+a);
				comchange=false;
				if (cl-1>ix) 
					comBox[0].setSelectedIndex(ix);
				else 
					comBox[0].setSelectedIndex(ix-1);
			}
		}
		else if (actName.equals("<")) {
			Object[] val = list1.getSelectedValues();
			if (val.length>0) {
				int icf = comBox[0].getSelectedIndex();
				for (int a=0;a<val.length;a++) 
					collfiles[icf].add(val[a]);
				list3.setListData(collfiles[icf].toArray());
			}
		}
		else if (actName.equals("+<")) {
			Object[] val = list1.getSelectedValues();
			if (val.length>0) {
				comchange=true;
				ArrayList[] cf = collfiles;
				collfiles = new ArrayList[cf.length+val.length];
				System.arraycopy(cf,0,collfiles,0,cf.length);
				for (int a=0;a<val.length;a++) {
					if (comBox[0].getItemCount()>0) 
						comBox[0].addItem(""+comBox[0].getItemCount());
					else 
						comBox[0].addItem("0");
					collfiles[cf.length+a] = new ArrayList();
					collfiles[cf.length+a].add(val[a]);
					if (comBox[13].getItemCount()>0) 
						collout.add(comBox[13].getSelectedItem());
					else 
						collout.add(outalias);
					speciallist.add(new ArrayList());
					cutlist.add(new ArrayList());
				}
				comchange=false;
				comBox[0].setSelectedIndex(comBox[0].getItemCount()-1);
			}
		}
		else if (actName.equals("rf")) {
			Object[] val = list3.getSelectedValues();
			int[] indc = list3.getSelectedIndices();
			if (val.length>0) {
				int icf = comBox[0].getSelectedIndex();
				for (int a=indc.length-1;a>-1;a--) 
					collfiles[icf].remove(indc[a]);
				list3.setListData(collfiles[icf].toArray());
			}
		}
		else if (actName.equals("up")) {
			int[] indc = list3.getSelectedIndices();
			int[] indc2 = indc;
			if (indc.length>0) {
				int icf = comBox[0].getSelectedIndex();
				for (int a=0;a<indc.length;a++) {
					if (indc[a]>0) {
						collfiles[icf].add(indc[a]-1,collfiles[icf].get(indc[a]));
						collfiles[icf].remove(indc[a]+1);
						indc2[a]=indc[a]-1;
					}
				}
				list3.setListData(collfiles[icf].toArray());
				list3.setSelectedIndices(indc2);
				list3.ensureIndexIsVisible(indc2[0]);
			}
		}
		else if (actName.equals("down")) {
			int[] indc = list3.getSelectedIndices();
			int[] indc2 = indc;
			if (indc.length>0) {
				int icf = comBox[0].getSelectedIndex();
				for (int a=indc.length-1;a>-1;a--) {
					if (indc[a]<collfiles[icf].size()-1) {
						collfiles[icf].add(indc[a],collfiles[icf].get(indc[a]+1));
						collfiles[icf].remove(indc[a]+2);
						indc2[a]=indc[a]+1;
					}
				}
				list3.setListData(collfiles[icf].toArray());
				list3.setSelectedIndices(indc2);
				list3.ensureIndexIsVisible(indc2[indc2.length-1]);
			}
		}
		else if (actName.equals("cb") && !comchange)
		{
			if (comBox[0].getItemCount() > 0)
			{
				collectionlist = collfiles[comBox[0].getSelectedIndex()].toArray();
				outfield.setText(collout.get(comBox[0].getSelectedIndex()).toString());
			}
			else
			{ 
				collectionlist = new Object[0]; 
				outfield.setText(""); 
			}
			list3.setListData(collectionlist);
		}
		else if (actName.equals("co") && outchange==false)
		{
			if (comBox[13].getItemCount()>0 && comBox[0].getItemCount()>0)
			{
				collout.set(comBox[0].getSelectedIndex(),comBox[13].getSelectedItem());
				outfield.setText(collout.get(comBox[0].getSelectedIndex()).toString());
			} 
		}
		else if (actName.equals("exec")) {
			if (!executePane.isVisible()) 
				executePane.show();
			else 
				executePane.close();
		}
		else if (actName.equals("picview")) {
			if (subpicture.isVisible()) 
				subpicture.close();
			else 
				subpicture.show(); //DM18052004 081.7 int02 changed
		}
		else if (actName.equals("oa")) {
			autoload.show();
		}

		if (comBox[0].getItemCount() > 0) 
			add_files.setEnabled(true);
		else 
			add_files.setEnabled(false);

	}
}


/*****************
 * execute panel *
 *****************/
class EXECUTE extends JFrame {

	public EXECUTE() {
		addWindowListener (new WindowAdapter(){ public void windowClosing(WindowEvent e) { close(); } });
		setTitle("external program executions");

		ActionListener exli = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try 
				{
				String actName = e.getActionCommand();
				if (actName.equals("close")) 
					close();
				else if (actName.equals("exec1")) 
					Runtime.getRuntime().exec(exefield[0].getText().toString());
				else if (actName.equals("exec2")) 
					Runtime.getRuntime().exec(exefield[1].getText().toString());
				else if (actName.equals("exec3")) 
					Runtime.getRuntime().exec(exefield[2].getText().toString());
				}
				catch (Exception ex) { 
					Msg(" ! execution error: "+ex); 
				}
			}
		};

		JPanel container = new JPanel();
		container.setLayout( new ColumnLayout() );

		exefield[0] = new JTextField("commandline #1");
		exefield[0].setPreferredSize(new Dimension(300,25));
		exefield[1] = new JTextField("commandline #2");
		exefield[1].setPreferredSize(new Dimension(300,25));
		exefield[2] = new JTextField("commandline #3");
		exefield[2].setPreferredSize(new Dimension(300,25));

		JButton exe1 = new JButton("exec");
		exe1.setActionCommand("exec1");
		JButton exe2 = new JButton("exec");
		exe2.setActionCommand("exec2");
		JButton exe3 = new JButton("exec");
		exe3.setActionCommand("exec3");
		JButton close = new JButton("close");
		exe1.setPreferredSize(new Dimension(70,20));
		exe2.setPreferredSize(new Dimension(70,20));
		exe3.setPreferredSize(new Dimension(70,20));
		exe1.addActionListener(exli);
		exe2.addActionListener(exli);
		exe3.addActionListener(exli);

		JPanel ex1 = new JPanel();
		ex1.setLayout(new BoxLayout(ex1, BoxLayout.X_AXIS));
		ex1.add(exefield[0]); 
		ex1.add(exe1);  
		JPanel ex2 = new JPanel();
		ex2.setLayout(new BoxLayout(ex2, BoxLayout.X_AXIS));
		ex2.add(exefield[1]); 
		ex2.add(exe2);  
		JPanel ex3 = new JPanel();
		ex3.setLayout(new BoxLayout(ex3, BoxLayout.X_AXIS));
		ex3.add(exefield[2]); 
		ex3.add(exe3);  

		getRootPane().setDefaultButton(close);

		container.add(ex1);
		container.add(ex2);
		container.add(ex3);
		container.add(new JLabel("post commands: "));

		String ett = "execute this command after a coll./split part has been processed";
		String[] ln = { " demux"," to VDR"," to MPG"," to PVA"," to TS" };

		JPanel[] ex4 = new JPanel[5];
		for (int v=0;v<5;v++) {
			exefield[v+3] = new JTextField("");
			exefield[v+3].setPreferredSize(new Dimension(300,25));
			ex4[v] = new JPanel();
			ex4[v].setLayout(new BoxLayout(ex4[v], BoxLayout.X_AXIS));
			ex4[v].add(exefield[v+3]); 
			ex4[v].add(new JLabel(ln[v]));  
			ex4[v].setToolTipText(ett);
			exefield[v+3].setToolTipText(ett);
			container.add(ex4[v]);
		}

		getContentPane().add(container);
		pack();
		centerDialog();
		UIManager.addPropertyChangeListener(new UISwitchListener(container));
	}

	protected void centerDialog() {
		Dimension screenSize = this.getToolkit().getScreenSize();
		Dimension size = this.getSize();
		screenSize.height = screenSize.height/2;
		screenSize.width = screenSize.width/2;
		size.height = size.height/2;
		size.width = size.width/2;
		int y = screenSize.height - size.height;
		int x = screenSize.width - size.width;
		this.setLocation(x,y);
	}

	public void close() {
		dispose(); //DM26032004 081.6 int18 changed
	}
}


/*************
 * patch panel *
 *************/
class PATCH extends JDialog {

	String file="";
	String[] notes = { " H:"," V:"," BR:","bps " };
	long ins=0;
	byte[] os = new byte[1];
	PatchListener patchAction = new PatchListener();

	class PatchListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			String actName = e.getActionCommand();

			if (actName.equals("change"))
				change();
			else if (actName.equals("cancel"))
				cancel();
		}
	}


	public PATCH(JFrame f) {
		super(f, "edit basic video infos in 1st sequence header", true);

		JPanel container = new JPanel();
		container.setLayout( new BorderLayout() );

		JPanel grid = new JPanel();
		grid.setLayout(new BoxLayout(grid, BoxLayout.X_AXIS));

		for (int a=0;a<3;a++) {
			patchfield[a] = new JTextField("");
			patchfield[a].setPreferredSize(new Dimension(65,22));
			patchfield[a].setMaximumSize(new Dimension(65,22));
			grid.add(new JLabel(notes[a]));
			grid.add(patchfield[a]);
		}
		grid.add(new JLabel(notes[3]));

		JButton changebutton = new JButton("change");
		changebutton.addActionListener(patchAction);
		grid.add(changebutton);



















		JButton cancelbutton = new JButton("cancel");
		cancelbutton.addActionListener(patchAction);
		grid.add(cancelbutton);

		getRootPane().setDefaultButton(cancelbutton);

		container.add(grid);
		getContentPane().add(container);
		pack();
		centerDialog();
		UIManager.addPropertyChangeListener(new UISwitchListener(container));
	}

	public boolean search(String file) {
		try 
		{
		RandomAccessFile pfile = new RandomAccessFile(file,"r");
		long size = pfile.length();
		byte[] ps = new byte[((size<650000)?(int)size:650000)];
		os = scan.getVBasic();
		pfile.seek(0);
		pfile.read(ps);
		pfile.close();
		for (int a=0;a<ps.length-15;a++) {
			if (ps[a]!=0 || ps[a+1]!=0 || ps[a+2]!=1 || ps[a+3]!=(byte)0xB3 || 
			ps[a+4]!=os[4] || ps[a+5]!=os[5] || ps[a+6]!=os[6] || ps[a+7]!=os[7] ) continue;
			ins=a;
			patchfield[0].setText(""+((255&ps[a+4])<<4 | (240&ps[a+5])>>>4));
			patchfield[1].setText(""+((15&ps[a+5])<<8 | (255&ps[a+6])));
			patchfield[2].setText(""+(((255&ps[a+8])<<10 | (255&ps[a+9])<<2 | (192 & ps[a+10])>>>6)*400));
			return true;
		}
		} 
		catch (IOException e) { X.Msg("patch error "+e); }
		return false;
	}

	public void entry(String file1) {
		file = file1;
		if (search(file))
			this.show();
	}

	protected void centerDialog() {
		Dimension screenSize = this.getToolkit().getScreenSize();
		Dimension size = this.getSize();
		screenSize.height = screenSize.height/2;
		screenSize.width = screenSize.width/2;
		size.height = size.height/2;
		size.width = size.width/2;
		int y = screenSize.height - size.height;
		int x = screenSize.width - size.width;
		this.setLocation(x,y);
	}

 	public void cancel() {
		this.setVisible(false);
	}

	public void change() {
		try 
		{
		int hsize=Integer.parseInt(patchfield[0].getText());
		int vsize=Integer.parseInt(patchfield[1].getText());
		int brate=Integer.parseInt(patchfield[2].getText())/400;
		os[4] = (byte)(0xFF&hsize>>>4);
		os[5] = (byte)((0xF0&hsize<<4) | (0xF&vsize>>>8));
		os[6] = (byte)(0xFF&vsize);
		os[8] = (byte)(0xFF&brate>>>10);
		os[9] = (byte)(0xFF&brate>>>2);
		os[10]= (byte)((0x3F&os[10]) | (0xC0&brate<<6));
		dochange();
		} 
		catch (NumberFormatException e) {  }
		catch (NullPointerException e) {  }
	}

	public void dochange() {
		try 
		{
		RandomAccessFile pfile = new RandomAccessFile(file,"rw");
		pfile.seek(ins);
		pfile.write(os);
		pfile.close();
		} 
		catch (IOException e) { X.Msg("patch error2 "+e); }
		this.setVisible(false);
	}
}

//DM24062004 081.7 int05
/** outsourced
class PREVIEW
{}
**/

/*************
 * cut panel *
 *************/
class COLLECTION extends JFrame {

	JTextField includeField, framecutfield;
	JList includeList;
	ArrayList dataList = new ArrayList(), previewList = new ArrayList();
	JSlider search;
	JLabel pointscount, firstfile, scannedPID; //DM08022004 081.6 int16 add
	JButton cutdel,cutadd, apply, close, loadlist, savelist;
	DNDListener2 dnd2 = new DNDListener2();	//DM18022004 081.6 int17 new
	CutListener cutAction = new CutListener();
	JumpListener jumpAction = new JumpListener();
	boolean action=false, decode=false;
	String file=" ", title="";
	int filetype=0;
	long lastpos=0;
	long cutPoints[]=new long[0]; //DM17012004 081.6 int11 changed, DM29012004 int12 fix

	//DM24062004 081.7 int05 changed
	Preview Preview = new Preview(loadSizeForward, MPVDecoder, raw_interface);
	//PREVIEW Preview = new PREVIEW();

	//DM18022004 081.6 int17 new
	class DNDListener2 implements DropTargetListener {
		public void drop(DropTargetDropEvent e) {
			try 
			{
			int da = e.getDropAction();  // 1=copy, 2=move

			if ( da==0 || da>2) { 
				e.rejectDrop(); 
				return; 
			}
			e.acceptDrop(da);

			Transferable tr = e.getTransferable();
			DataFlavor[] df = tr.getTransferDataFlavors();
			java.util.List li = (java.util.List)tr.getTransferData(df[0]);

			Object[] val = li.toArray();
			if (val.length>0)
				loadList(val[0].toString());

			e.dropComplete(true);

			}
			catch (Exception eee)
			{ 
				Msg(""+eee); 
				e.dropComplete(false); 
			}
		}

		public void dragEnter(DropTargetDragEvent e)
		{}
		public void dragExit(DropTargetEvent e) 
		{}
		public void dragOver(DropTargetDragEvent e)
		{}
		public void dropActionChanged(DropTargetDragEvent e)
		{}
	}

	class JumpListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			if (!action) 
				return;
			String actName = e.getActionCommand();
			int val = search.getValue();
			if (actName.equals("<<"))
				search.setValue(val-312500);
			else if (actName.equals("<"))
				search.setValue(val-2);
			else if (actName.equals(">"))
				search.setValue(val+2);
			else if (actName.equals(">>"))
				search.setValue(val+312500);
			else if (actName.equals("loadlist"))
				loadList();
			else if (actName.equals("savelist"))
				saveList();
		}
	}

	class CutListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			if (!action)
				return;

			action=false;
			String actName = e.getActionCommand();

			if (actName.equals("del point")) {
				comBox[14].removeItemAt(comBox[14].getSelectedIndex());
				if (comBox[14].getItemCount()>0)
					comBox[14].setSelectedIndex(comBox[14].getItemCount()-1);
			}else if (actName.equals("cutnumber") || actName.equals("add point")) {
				if (!framecutfield.getText().equals("")) {
					comBox[14].addItem(framecutfield.getText());
					comBox[14].setSelectedIndex(comBox[14].getItemCount()-1);
				}
				framecutfield.setText("");
			}else if (actName.equals("apply")) {
				apply();
				action=true;
				return;
			}else if (actName.equals("apply & close")) {
				close();
				action=true;
				return;
			}else if (actName.equals("ID")) {   //DM20032004 081.6 int18 changed
				try
				{
					String val = includeField.getText().toString();

					if (!val.equals(""))
					{
						if (!val.startsWith("0x")) 
							val = "0x" + Integer.toHexString(0x1FFF&Integer.parseInt(val));

						val = val.toUpperCase().replace( 'X', 'x');

						dataList.add(val);
						includeList.setListData(dataList.toArray());
						includeList.ensureIndexIsVisible(dataList.size()-1);
					}
				}
				catch (NumberFormatException ne)
				{
					Msg("-> wrong number as ID");
				}
				includeField.setText("");
				action=true;
				return;
			}else if (actName.equals("transferPIDs")) {  //DM28112003 081.5++
				Object[] val = includeList.getSelectedValues();
				if (val.length>0) {
					comchange=true;
					ArrayList cf[] = collfiles;
					ArrayList infiles = (ArrayList)collfiles[activecoll];
					collfiles = new ArrayList[cf.length+1];
					System.arraycopy(cf,0,collfiles,0,cf.length);

					//transfer files + selected PIDs
					collfiles[cf.length] = new ArrayList();
					for (int a=0; a<infiles.size(); a++)
						collfiles[cf.length].add(infiles.get(a));
					ArrayList npids = new ArrayList();
					for (int a=0; a<val.length; a++) 
						npids.add(val[a]);
					speciallist.add(npids);
					cutlist.add(new ArrayList());
					collout.add(collout.get(activecoll));

					//remove PIDs, for the activecoll they have to applied
					for (int a=0; a<val.length; a++)
						dataList.remove(dataList.indexOf(val[a]));

					includeList.setListData(dataList.toArray());

					if (comBox[0].getItemCount() > 0) //DM26032004 081.6 int18 changed
						comBox[0].addItem("" + comBox[0].getItemCount());
					else 
						comBox[0].addItem("0");

					comchange=false;
				}
				action=true;
				return;
			}else if (actName.equals("transferCuts")) {  //DM28112003 081.5++
				int NumOfPts=0;
				comchange=true;
				if ((NumOfPts=comBox[14].getItemCount())>2){ //2cutpoints are to few
					ArrayList infiles = (ArrayList)collfiles[activecoll];
					for (int b=2; b<NumOfPts; b+=2){
						ArrayList cf[] = collfiles;
						collfiles = new ArrayList[cf.length+1];
						System.arraycopy(cf,0,collfiles,0,cf.length);

						//transfer files + selected PIDs
						collfiles[cf.length] = new ArrayList();
						for (int a=0; a<infiles.size(); a++)
							collfiles[cf.length].add(infiles.get(a));

						ArrayList npids = new ArrayList();
						for (int a=0; a<dataList.size(); a++) 
							npids.add(dataList.get(a));
						speciallist.add(npids);
						collout.add(collout.get(activecoll));

						ArrayList Cuts = new ArrayList();
						for (int a=b; a<NumOfPts && a<b+2; a++) 
							Cuts.add(comBox[14].getItemAt(a));

						cutlist.add(Cuts);

						if (comBox[0].getItemCount() > 0) //DM26032004 081.6 int18 changed
							comBox[0].addItem(""+comBox[0].getItemCount());
						else
							comBox[0].addItem("0");
					}
					while (comBox[14].getItemCount()>2)
						comBox[14].removeItemAt(2);
				}
				comchange=false;
				action=true;
				return;
			}

			//DM17012004 081.6 int11 changed
			//DM29012004 081.6 int12 fix
			if (comBox[14].getItemCount()>0){
				cutdel.setEnabled(true);

				Object selectedItem = comBox[14].getSelectedItem();
				cutPoints = new long[comBox[14].getItemCount()];
				for (int a=0;a<cutPoints.length;a++)
					cutPoints[a] = parseValue(comBox[14].getItemAt(a).toString(),false);
				java.util.Arrays.sort(cutPoints);
				comBox[14].removeAllItems();
				for (int a=0;a<cutPoints.length;a++)
					comBox[14].addItem(parseValue(cutPoints[a]));
				comBox[14].setSelectedItem(selectedItem);

				//DM27042004 081.7 int02 changed
				if ((comBox[14].getSelectedIndex()&1)==1) 
					MPVDecoder.picture.showCut(false, cutPoints, previewList);
				else
					MPVDecoder.picture.showCut(true, cutPoints, previewList);

				if (actName.equals("cutbox") || actName.equals("del point")){
					if (comBox[17].getSelectedIndex()==0){
						long cutpoint = Long.parseLong(selectedItem.toString());
						preview(cutpoint);
					}
				}
			}else{
				cutdel.setEnabled(false);
				cutPoints = new long[0];
				MPVDecoder.picture.showCut(true, cutPoints, previewList); //DM27042004 081.7 int02 changed
			}
			if (comBox[17].getSelectedIndex()==0)
				search.requestFocus();
			getExpectedSize(); //DM24082003 
			action=true;
		}
	}

	public COLLECTION() {
 
		addWindowListener (new WindowAdapter(){ public void windowClosing(WindowEvent e) { close(); } });
		setTitle("collection specials");

		JPanel container = new JPanel();
		container.setLayout( new BorderLayout() );

		//DM27042004 081.7 int02 changed++
		JPanel grid = new JPanel();
		grid.setLayout(new BorderLayout());

		JPanel previewPanel = new JPanel();
		previewPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLoweredBevelBorder(), "videofile cuts (demux only!)")); //DM27042004 081.7 int02 changed
		previewPanel.setLayout ( new BorderLayout() );
		previewPanel.setToolTipText("also use the keyboard keys for navigation"); //DM18052004 081.7 int02 add
		previewPanel.add(MPVDecoder.picture);

		search = new JSlider(0, (10240000/16), 0);
		search.setPreferredSize(new Dimension(512,24));
		search.setMaximumSize(new Dimension(512,24));
		search.setValue(0);

		JPanel jumpPanel = new JPanel();
		jumpPanel.setLayout(new BoxLayout(jumpPanel, BoxLayout.X_AXIS));

		String jumpstring[] = { "<<", "<", ">", ">>" };
		JButton jump[] = new JButton[4];
		for (int a=0; a<4; a++){
			jump[a] = new JButton(jumpstring[a]);
			jump[a].setPreferredSize(new Dimension(60,20));
			jump[a].setMaximumSize(new Dimension(60,20));
			jump[a].addActionListener(jumpAction);
			jumpPanel.add(jump[a]);
		}

		JPanel jump2Panel = new JPanel();
		jump2Panel.setLayout(new ColumnLayout());

		jump2Panel.add(jumpPanel);
		jump2Panel.add((firstfile = new JLabel(file)));
		jump2Panel.add((scannedPID = new JLabel(" ")));

		firstfile.setToolTipText("active segment/number of segments + filename of active segment");
		scannedPID.setToolTipText("processed (P)ID to get the current picture");

		JPanel jump3Panel = new JPanel();
		jump3Panel.setLayout(new ColumnLayout());

		JPanel CL1 = new JPanel();
		CL1.setLayout(new BoxLayout(CL1, BoxLayout.X_AXIS));

		framecutfield = new JTextField("");
		framecutfield.setPreferredSize(new Dimension(150,22));
		framecutfield.setMaximumSize(new Dimension(150,22));
		framecutfield.setToolTipText("key in a number, or drag&drop a cutpoint file over here"); //DM18022004 081.6 int17 changed
		framecutfield.setActionCommand("cutnumber");
		CL1.add(framecutfield);

		cutadd = new JButton("add point");
		cutadd.setPreferredSize(new Dimension(100,25));
		cutadd.setMaximumSize(new Dimension(100,25));
		CL1.add(cutadd);

		jump3Panel.add(CL1);

		JPanel CL5 = new JPanel();
		CL5.setLayout(new BoxLayout(CL5, BoxLayout.X_AXIS));

		comBox[14] = new JComboBox();
		comBox[14].setMaximumRowCount(8);
		comBox[14].setPreferredSize(new Dimension(150,22));
		comBox[14].setMaximumSize(new Dimension(150,22));
		comBox[14].setActionCommand("cutbox");
		CL5.add(comBox[14]);

		cutdel = new JButton("del point");
		cutdel.setPreferredSize(new Dimension(100,22));
		cutdel.setMaximumSize(new Dimension(100,22));
		cutdel.setEnabled(false);
		CL5.add(cutdel);

		jump3Panel.add(CL5);

		JPanel CL0 = new JPanel();
		CL0.setLayout(new BoxLayout(CL0, BoxLayout.X_AXIS));
		CL0.add(new JLabel("Number of Points: "));
		CL0.setToolTipText("all numbers are automatically sorted");
		CL0.add((pointscount = new JLabel("")));

		jump3Panel.add(CL0);

		cutdel.addActionListener(cutAction);
		cutadd.addActionListener(cutAction);
		comBox[14].addActionListener(cutAction);
		framecutfield.addActionListener(cutAction);

		JPanel grid_1 = new JPanel();
		grid_1.setLayout(new GridLayout(0,2));
		grid_1.add(jump2Panel);
		grid_1.add(jump3Panel);

		JPanel grid_2 = new JPanel();
		grid_2.setLayout(new BorderLayout());
		grid_2.setBorder(BorderFactory.createRaisedBevelBorder());
		grid_2.add(search);
		grid_2.add(grid_1, BorderLayout.SOUTH);

		previewPanel.add(grid_2,BorderLayout.SOUTH);
		//DM27042004 081.7 int02 changed--

		search.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e)  {
				if (!search.getValueIsAdjusting() && action){
					long val = ((comBox[17].getSelectedIndex()==0)?16L:1L) * search.getValue();

					if (comBox[17].getSelectedIndex()==0 && val!=(lastpos&~15))
						preview(val);
					else if (comBox[17].getSelectedIndex() > 0) //DM30032004 081.6 int18 add
						scannedPID.setText("preview not available");

					getType();
				}
		}});

		search.addKeyListener(new KeyAdapter() { 
			public void keyPressed(KeyEvent e) { 
				int i=0, ic=0, offs=0; 
				int keyval=e.getKeyCode(); 
				switch(e.getKeyChar()) { 
				case 'p': 
					ic=comBox[14].getItemCount(); 
					if (ic>0) { 
						i=ic-1; 
						if (lastpos>Long.parseLong(comBox[14].getItemAt(0).toString()))
							while (lastpos<=Long.parseLong(comBox[14].getItemAt(i).toString()))
								i--; 
						comBox[14].setSelectedIndex(i); 
					} 
					return; 
				case 'n': 
					ic=comBox[14].getItemCount(); 
					if (ic>0) { 
						if (lastpos<Long.parseLong(comBox[14].getItemAt(ic-1).toString()))
							while (lastpos>=Long.parseLong(comBox[14].getItemAt(i).toString())) 
								i++; 
						comBox[14].setSelectedIndex(i); 
					}                   
					return; 
				case 'a': 
					cutadd.doClick(); 
					return; 
				case 'd': 
					cutdel.doClick(); 
					return; 
				} 

				if (e.isShiftDown()) 
					offs=62500; 
				else if (e.isControlDown()) 
					offs=312500; 
				else if (e.isAltDown()) 
					offs=3125000; 
				else 
					return; 

				switch (keyval) { 
				case KeyEvent.VK_RIGHT: 
					search.setValue(search.getValue()+offs); 
					break; 
				case KeyEvent.VK_LEFT: 
					search.setValue(search.getValue()-offs); 
				}    
			} 
		}); 

		grid.add(previewPanel);


		JPanel cutPanel = new JPanel();
		cutPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createRaisedBevelBorder(), "various")); //DM27042004 081.7 int02 changed
		cutPanel.setLayout ( new ColumnLayout() );

		//DM08022004 081.6 int16 new
		RButton[10] = new JRadioButton("faster/worse decoding of preview");
		RButton[10].setToolTipText("reconstruct and interpolate only the upper/left pixel of each macroblock");
		RButton[10].setPreferredSize(new Dimension(230,20));
		RButton[10].setMaximumSize(new Dimension(230,20));
		cutPanel.add(RButton[10]);

		RButton[6] = new JRadioButton("also preview GOPs w/o sequ.header");
		RButton[6].setToolTipText("for DVB data it's not recommended, enable only for some streams with only one sequ.header!");
		RButton[6].setPreferredSize(new Dimension(230,20));
		RButton[6].setMaximumSize(new Dimension(230,20));
		cutPanel.add(RButton[6]);

		cutPanel.add(new JLabel(" "));

		cutPanel.add(new JLabel("PES(&Sub)-ID/PID list: "));

		includeField = new JTextField("");
		includeField.setPreferredSize(new Dimension(80,25));
		includeField.setEditable(true);
		includeField.setActionCommand("ID");
		includeField.setToolTipText("enter PES-ID or sub-ID (MPG etc) or PID (PVA,TS); empty list == use all IDs");

		includeList = new JList();
		includeList.setToolTipText("doubleclick to remove a selected entry");
		cutPanel.add(includeField);

		includeField.addActionListener(cutAction);

		JScrollPane scrollList = new JScrollPane();
		scrollList.setPreferredSize(new Dimension(80,100));
		scrollList.setViewportView(includeList);
		cutPanel.add(scrollList);

		includeList.addMouseListener( new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				int index = includeList.locationToIndex(e.getPoint());
				if (e.getClickCount() == 2 && index>-1) {

					dataList.remove(index);
					includeList.setListData(dataList.toArray());
				}
			}
		});

		//DM26112003 081.5++
		JButton pids = new JButton("transfer selected (P)IDs to new coll#");
		pids.setPreferredSize(new Dimension(250,20));
		pids.setMaximumSize(new Dimension(250,20));
		pids.setActionCommand("transferPIDs");
		pids.setToolTipText("changes in the active coll# you must apply");
		pids.addActionListener(cutAction);
		cutPanel.add(pids);

		//DM26112003 081.5++
		JButton cpoints = new JButton("transfer cutpoint pairs to new coll#");
		cpoints.setPreferredSize(new Dimension(250,20));
		cpoints.setMaximumSize(new Dimension(250,20));
		cpoints.setActionCommand("transferCuts");
		cpoints.setToolTipText("changes in the active coll# you must apply");
		cpoints.addActionListener(cutAction);
		cutPanel.add(cpoints);

		cutPanel.add(new JLabel(" ")); //DM27042004 081.7 int02 changed

		cBox[2] = new JCheckBox("create coll# as subdirectory");
		cBox[2].setToolTipText("global: create new subdirectory for each collection number (better for same files in diff. collections)");
		cBox[2].setPreferredSize(new Dimension(230,20));
		cBox[2].setMaximumSize(new Dimension(230,20));
		cutPanel.add(cBox[2]);

		cutPanel.add(new JLabel(" ")); //DM27042004 081.7 int02 changed

		cutPanel.add(new JLabel("additional export limits:"));

		cBox[52] = new JCheckBox("H-Resol.: ");
		cBox[52].setToolTipText("global,..that will automatically ignore any other video sequences");
		cBox[52].setPreferredSize(new Dimension(110,20));
		cBox[52].setMaximumSize(new Dimension(110,20));

		comBox[34] = new JComboBox(H_RESOLUTION);
		comBox[34].setMaximumRowCount(7);
		comBox[34].setPreferredSize(new Dimension(90,20));
		comBox[34].setMaximumSize(new Dimension(90,20));
		comBox[34].setSelectedIndex(10);
		comBox[34].setEditable(true);

		JPanel CL2 = new JPanel();
		CL2.setLayout(new BoxLayout(CL2, BoxLayout.X_AXIS));
		CL2.add(cBox[52]);  
		CL2.add(comBox[34]);  
		cutPanel.add(CL2);

		cBox[47] = new JCheckBox("DAR: ");
		cBox[47].setToolTipText("global,..that will automatically ignore any other video sequences");
		cBox[47].setPreferredSize(new Dimension(80,20));
		cBox[47].setMaximumSize(new Dimension(80,20));

		comBox[24] = new JComboBox(DAR);
		comBox[24].setMaximumRowCount(7);
		comBox[24].setPreferredSize(new Dimension(120,20));
		comBox[24].setMaximumSize(new Dimension(120,20));
		comBox[24].setSelectedIndex(2);

		JPanel CL3 = new JPanel();
		CL3.setLayout(new BoxLayout(CL3, BoxLayout.X_AXIS));
		CL3.add(cBox[47]);  
		CL3.add(comBox[24]);  
		cutPanel.add(CL3);

		cutPanel.add(new JLabel(" ")); //DM27042004 081.7 int02 changed

		//DM17012004 081.6 int11 changed, DM18022004 081.6 int17 changed
		Object[] cut_types = 
		{ 
			"use BytePos. for cuts", 
			"use GOP number for cuts",
			"use Frame number for cuts",
			"use PTS for cuts",
			"use Timecode for cuts"
		}; 
		comBox[17] = new JComboBox(cut_types);
		comBox[17].setPreferredSize(new Dimension(200,22));
		comBox[17].setMaximumSize(new Dimension(200,22));
		comBox[17].setSelectedIndex(0);
		cutPanel.add(comBox[17]);

		loadlist = new JButton("load Cutpoints from file");
		loadlist.setPreferredSize(new Dimension(200,22));
		loadlist.setMaximumSize(new Dimension(200,22));
		loadlist.setToolTipText("open a cutpoint file or drag&drop it over here"); //DM18022004 081.6 int17 new
		savelist = new JButton("save Cutpoints to file");
		savelist.setPreferredSize(new Dimension(200,22));
		savelist.setMaximumSize(new Dimension(200,22));
		loadlist.setActionCommand("loadlist");
		savelist.setActionCommand("savelist");
		loadlist.addActionListener(jumpAction);
		savelist.addActionListener(jumpAction);

		cutPanel.add(loadlist);
		cutPanel.add(savelist);

		//DM18022004 081.6 int17 new
		dropTarget_3 = new DropTarget(loadlist, dnd2);
		dropTarget_4 = new DropTarget(framecutfield,dnd2);

		grid.add(cutPanel, BorderLayout.EAST); //DM27042004 081.7 int02 changed

		container.add(grid);

		close = new JButton("apply & close");
		apply = new JButton("apply");
		close.addActionListener(cutAction);
		apply.addActionListener(cutAction);

		JPanel CL4 = new JPanel();
		CL4.setLayout(new GridLayout(1,2));
		CL4.add(apply);
		CL4.add(close);
		container.add(CL4, BorderLayout.SOUTH);

		getRootPane().setDefaultButton(close);
		getContentPane().add(container);
		centerDialog();
		UIManager.addPropertyChangeListener(new UISwitchListener(container));
	}

	public void getType() {
		int keyIndex = java.util.Arrays.binarySearch(cutPoints,lastpos); //DM17012004 081.6 int11 changed , DM29012004 081.6 int12 fix

		//DM27042004 081.7 int02 changed
		if (cutPoints.length!=0)
			MPVDecoder.picture.showCut((keyIndex&1)==0, cutPoints, previewList);
		else
			MPVDecoder.picture.showCut(true, cutPoints, previewList);
	}

	public void preview(long pos) {
		boolean direction=false;

		try 
		{

		//DM09032004 081.6 int18 changed
		if (comBox[17].getSelectedIndex()!=0 || previewList.size()==0)
		{
			scannedPID.setText("preview not available");
			return;
		}

		action=false;

		//DM18062004 081.7 int05 changed
		//int loadSize = 2560000; // bytes for searching the next I-frame ;changed for X0.81
		int loadSize = loadSizeForward; // bytes for searching the next I-frame ;changed for X0.81

		setTitle(title + "processing preview...");

		if (pos>>>4 >= (long)search.getMaximum())   // last
		{
			pos = (pos > loadSize) ? pos - loadSize : 0;
			direction=true;
		}
		else if (pos > 0 && pos < lastpos && ((lastpos>>>4) - (pos>>>4)) < 3L )
		{
			pos = (pos > loadSize) ? pos - loadSize : 0;
			direction = true;
		}

		//DM24062004 081.7 int05 changed
		pos = Preview.load(pos, ((direction && pos==0) ? (int)lastpos : loadSize), previewList, direction, RButton[6].isSelected(), RButton[10].isSelected(), speciallist, activecoll);
		dialog.firstfile.setText(Preview.getProcessedFile());
		dialog.scannedPID.setText(Preview.getProcessedPID());


		lastpos = pos;
		search.setValue((int)(lastpos / 16));
		framecutfield.setText("" + lastpos);
		search.requestFocus();

		} 
		catch (IOException e6)
		{
			//DM25072004 081.7 int07 add
			Msg("preview error: " + e6);
		}
		setTitle(title);
		getExpectedSize();
		action=true;
	}

	public void entry() {

		ArrayList xyz = (ArrayList)speciallist.get(activecoll);
		dataList.clear();

		for (int a=0; a<xyz.size(); a++)
			dataList.add(xyz.get(a));

		includeList.setListData(dataList.toArray());

		ArrayList infiles = (ArrayList)collfiles[activecoll];
		previewList.clear();

		long start=0,end=0;

		//DM24062004 081.7 int05 changed
		filesearch:
		for (int a=0,b=0,type=0; a<infiles.size(); a++)
		{
			file = infiles.get(a).toString();
			type = scan.inputInt(file);

			if (b != 0 && b != type)
				break filesearch;

			switch (type)
			{
			case 1:
			case 2: //DM28112003 081.5++
			case 3:
			case 4:
			case 9:
			case 11:
				b = type;

				//DM18062004 081.7 int05 changed
				start = end;
				end += raw_interface.isAccessibleDisk(file) ? raw_interface.getFileSize(file) : new File(file).length();
				previewList.add(new PreviewObject(start, end, type, file));
				break;

			default:
				break filesearch;
			}
		}

		if (end>16)
			search.setMaximum((int)(end/16));
		else
			search.setMaximum(1);

		//DM09032004 081.6 int18 changed
		if (comBox[17].getSelectedIndex()==0 && previewList.size()>0)
			preview(0);
		else
			scannedPID.setText("preview not available");

		title = "collection specials for coll.# "+activecoll+"  ";
		setTitle(title);

		//DM17012004 081.6 int11 changed
		//DM29012004 081.6 int12 fix
		Object listData[] = ((ArrayList)cutlist.get(activecoll)).toArray(); 
		cutPoints = new long[listData.length];
		for (int a=0;a<cutPoints.length;a++)
			cutPoints[a] = parseValue(listData[a].toString(),false);
		java.util.Arrays.sort(cutPoints);
		action=false;
		comBox[14].removeAllItems();
		if (cutPoints.length==0) {
			cutdel.setEnabled(false);
		} else { 
			for (int a=0;a<cutPoints.length;a++)
				comBox[14].addItem(parseValue(cutPoints[a]));
			action=true;
			comBox[14].setSelectedIndex(comBox[14].getItemCount()-1);
		}
		getExpectedSize(); //DM24082003 //pointscount.setText(""+comBox[14].getItemCount());
		getType();
		action=true;
		this.show();
	}

	protected void centerDialog() {
		Dimension screenSize = this.getToolkit().getScreenSize();
		this.pack(); //DM30122003 081.6 int10 add
		Dimension size = this.getSize();
		screenSize.height = screenSize.height/2;
		screenSize.width = screenSize.width/2;
		size.height = size.height/2;
		size.width = size.width/2;
		int y = screenSize.height - size.height;
		int x = screenSize.width - size.width;
		this.setLocation(x,y);
	}

	public boolean apply() {
		if (activecoll<comBox[0].getItemCount()) {
			ArrayList abc = new ArrayList();
			if (comBox[14].getItemCount()>0) {

				for (int a=0;a<comBox[14].getItemCount();a++)
					abc.add(comBox[14].getItemAt(a).toString());

			}
			cutlist.set(activecoll,abc);

			ArrayList xyz = new ArrayList();
			for (int a=0; a<dataList.size(); a++)
				xyz.add(dataList.get(a));
			speciallist.set(activecoll,xyz);
		}
		return true;
	}

	public void close()
	{
		if (apply()) 
			dispose(); //DM26032004 081.6 int18 changed

		else
		{
			Toolkit.getDefaultToolkit().beep();
			setTitle("collection specials for coll.# "+activecoll+"  -> ERROR! collection doesn't exists ");
		}
	}

	public void saveList() {
		if (comBox[14].getItemCount()==0)
			return;

		String newfile = file+"["+activecoll+"].Xcl";
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

		try
		{
		BufferedWriter listwriter = new BufferedWriter(new FileWriter(newfile));

		//DM09032004 081.6 int18 add
		listwriter.write(comBox[17].getSelectedItem().toString());
		listwriter.newLine();
		
		for (int a=0;a<comBox[14].getItemCount();a++) {
			listwriter.write(comBox[14].getItemAt(a).toString());
			listwriter.newLine();
		}
		listwriter.close();
		}
		catch (IOException e) { 
			Msg(".. cannot access file : "+file); 
		}
	}

	//DM18022004 081.6 int17 new
	public void loadList()
	{
		loadList("");
	}

	//DM18022004 081.6 int17 changed
	public void loadList(String newfile)
	{
		ArrayList pointlist = new ArrayList();
		String point="";

		if (!(new File(newfile).exists()))
		{
			chooser.rescanCurrentDirectory();
			int retval = chooser.showOpenDialog(this);
			if(retval == JFileChooser.APPROVE_OPTION) {
				File theFile = chooser.getSelectedFile();
				if(theFile != null && !theFile.isDirectory()) {
					newfile = theFile.getAbsolutePath();
				}
			} else 
				return;
		}

		try
		{
		BufferedReader listreader = new BufferedReader(new FileReader(newfile));
		while (true) {
			point = listreader.readLine();
			if (point==null) 
				break;
			if (point.trim().equals("")) 
				continue;

			//DM09032004 081.6 int18 add
			if (point.startsWith("use")) 
			{
				comBox[17].setSelectedItem(point);
				continue;
			}

			pointlist.add(point);
		}
		listreader.close();
		} 
		catch (IOException e5) { 
			Msg("error loading "+file);
		}

		//DM17012004 081.6 int11 changed
		//DM29012004 081.6 int12 fix
		Object listData[] = pointlist.toArray(); 
		cutPoints = new long[listData.length];
		for (int a=0;a<cutPoints.length;a++)
			cutPoints[a] = parseValue(listData[a].toString(),false);
		java.util.Arrays.sort(cutPoints);

		action=false;
		comBox[14].removeAllItems();
		if (cutPoints.length==0) {
			cutdel.setEnabled(false);
		} else { 



			for (int a=0;a<cutPoints.length;a++)
				comBox[14].addItem(parseValue(cutPoints[a]));
			action=true;
			comBox[14].setSelectedIndex(comBox[14].getItemCount()-1);
		}
		getExpectedSize(); //DM24082003 //pointscount.setText(""+comBox[14].getItemCount());
		getType();
		action=true;
		pointlist.clear();
	}

	//DM24082003+ , DM17012004 081.6 int11 changed
	//DM24062004 081.7 int05 changed
	public void getExpectedSize()
	{
		if (previewList.size() == 0)
		{
			pointscount.setText("" + comBox[14].getItemCount());
			return;
		}

		long newSize[] = new long[comBox[14].getItemCount()];
		long start=0, diff=0, end=0;

		if (comBox[17].getSelectedIndex() == 0)
		{
			for (int a = 0; a < newSize.length; a++)
				newSize[a] = Long.parseLong(comBox[14].getItemAt(a).toString());

			if (newSize.length == 0 || (newSize.length & 1) == 1)
				end = ((PreviewObject)previewList.get(previewList.size() - 1)).getEnd();

			else
				end = newSize[newSize.length - 1];

			for (int a = 0; a < newSize.length; a += 2)
			{
				diff += newSize[a] - start;
				start = a + 1 < newSize.length ? newSize[a + 1] : start;
			}
		}

		String length = comBox[17].getSelectedIndex()==0 ? (" / exp.Size: "+((end-diff)/1048576L)+"MB") : "";
		pointscount.setText(""+comBox[14].getItemCount() + length);
	}
	//DM24082003-
}

/*********************************/
/*********************************/
/*********************************/




class ColumnLayout implements LayoutManager {

	int xInset = 2;
	int yInset = 0;
	int yGap = 0;

	public void addLayoutComponent(String s, Component c) {}

	public void layoutContainer(Container c) {
		Insets insets = c.getInsets();
		int height = yInset + insets.top;
      
		Component[] children = c.getComponents();
		Dimension compSize = null;
		for (int i = 0; i < children.length; i++) {
			compSize = children[i].getPreferredSize();
			children[i].setSize(compSize.width, compSize.height);
			children[i].setLocation( xInset + insets.left, height);
			height += compSize.height + yGap;
		}
	}

	public Dimension minimumLayoutSize(Container c) {
		Insets insets = c.getInsets();
		int height = yInset + insets.top;
		int width = 0 + insets.left + insets.right;
      
		Component[] children = c.getComponents();
		Dimension compSize = null;
		for (int i = 0; i < children.length; i++) {
			compSize = children[i].getPreferredSize();
			height += compSize.height + yGap;
			width = Math.max(width, compSize.width + insets.left + insets.right + xInset*2);
		}
		height += insets.bottom;
		return new Dimension( width, height);
	}
  
	public Dimension preferredLayoutSize(Container c) {
		return minimumLayoutSize(c);
	}
   
	public void removeLayoutComponent(Component c) {}
}


/**************************
****  Drop-only listener *
**************************/
//DM26032004 081.6 int18 changed
class DNDListener implements DropTargetListener
{
	public void drop(DropTargetDropEvent e)
	{
		try 
		{
		int da = e.getDropAction();  // 1=copy, 2=move

		if ( da==0 || da>2)
		{ 
			e.rejectDrop(); 
			return; 




		}
		e.acceptDrop(da);

		Transferable tr = e.getTransferable();
		DataFlavor[] df = tr.getTransferDataFlavors();

		java.util.List li = (java.util.List)tr.getTransferData(df[0]);

		if (da==1)        // copy = new coll each
		{
			Object[] val = li.toArray();
			if (val.length > 0)
			{
				comchange=true;
				ArrayList[] cf = collfiles;
				collfiles = new ArrayList[cf.length + val.length];
				System.arraycopy(cf, 0, collfiles, 0, cf.length);

				for (int a=0; a<val.length; a++)
				{
					if (comBox[0].getItemCount() > 0) 
						comBox[0].addItem("" + comBox[0].getItemCount());
					else 
						comBox[0].addItem("0");

					collfiles[cf.length+a] = new ArrayList();
					collfiles[cf.length+a].add(val[a]);

					if (comBox[13].getItemCount() > 0) 
						collout.add(comBox[13].getSelectedItem());
					else 
						collout.add(outalias);

					speciallist.add(new ArrayList());
					cutlist.add(new ArrayList());
				}
				comchange=false;
				comBox[0].setSelectedIndex(comBox[0].getItemCount()-1);
			}
		}
		else if (da==2)    // move = one coll
		{
			if (!add_files.isEnabled())
			{
				int ix = comBox[0].getItemCount();
				ArrayList[] cf = collfiles;
				collfiles = new ArrayList[ix+1];
				collfiles[ix] = new ArrayList();
				System.arraycopy(cf, 0, collfiles, 0, cf.length);

				if (comBox[13].getItemCount() > 0) 
					collout.add(comBox[13].getSelectedItem());
				else 
					collout.add(outalias);

				comchange=true;
				comBox[0].addItem(""+ix);
				speciallist.add(new ArrayList());
				cutlist.add(new ArrayList());
				comchange=false;

				comBox[0].setSelectedIndex(ix);
			}

			Object[] val = li.toArray();
			if (val.length > 0)
			{
				int icf = comBox[0].getSelectedIndex();

				for (int a=0; a<val.length; a++) 
					collfiles[icf].add(val[a]);

				list3.setListData(collfiles[icf].toArray());
				comBox[0].setSelectedIndex(icf);
			}
		}
		e.dropComplete(true);
	}
	catch (Exception eee)
	{ 
		Msg("DnD " + eee); 
		e.dropComplete(false); 
	}

	//DM09072004 081.7 int06 add
	list3.setBackground(Color.white);

	}

	public void dragEnter(DropTargetDragEvent e)
	{ 
		list3.setBackground(Color.green);
	}

	public void dragExit(DropTargetEvent e)
	{
		list3.setBackground(Color.white);
	}

	public void dragOver(DropTargetDragEvent e)
	{}
	public void dropActionChanged(DropTargetDragEvent e)
	{}
}



/****************************
 ****  tabbed pane listener *
 ****************************/
class TabListener implements ActionListener {
	public void actionPerformed(ActionEvent e) {
		String actName = e.getActionCommand();

		if (actName.equals("biglog")) 
			cBox[21].setSelected(false);
		else if (actName.equals("normallog")) 
			cBox[11].setSelected(false);
		else if (actName.equals("d2v1")) 
			cBox[30].setSelected(false);
		else if (actName.equals("d2v2")) 
			cBox[29].setSelected(false);
		else if (actName.equals("prog1")) 
			cBox[44].setSelected(false);
		else if (actName.equals("prog2")) 
			cBox[31].setSelected(false);

		//DM07022004 081.6 int16 new
		else if (actName.equals("riff")) 
			RButton[9].setSelected(false);
		else if (actName.equals("aiff")) 
			RButton[5].setSelected(false);

		updateState();
	}
}


public void updateState() {

	bs = Integer.parseInt(comBox[10].getSelectedItem().toString());
	scan.setBuffer(Integer.parseInt(comBox[37].getSelectedItem().toString())); //DM04122003 081.6_int02

	if (!comBox[23].getSelectedItem().toString().equalsIgnoreCase("null"))
		options[57]=Long.parseLong(comBox[23].getSelectedItem().toString());
	else 
		options[57]=55000L;

	//DM07022004 081.6 int16 new
	if (RButton[9].isSelected())
		RButton[4].setSelected(true);

	if (cBox[30].isSelected()) 
		options[51]=Long.parseLong(d2vfield[5].getText())*1048576L;

	options[36] = cBox[17].isSelected() ? 1 : 0;
	options[35] = cBox[16].isSelected() ? 1 : 0;
	options[32] = cBox[13].isSelected() ? 1 : 0;
	options[29] = cBox[9].isSelected() ? 1 : 0;

	options[34] = cBox[15].isSelected() ? 1 : 0;
	//JLA14082003+
	//0: no patch, 1:patch unconditionally, 2:patch if <>720|352, 3:pach if <>720|704|352
	options[34] = comBox[35].getSelectedIndex();
	//JLA14082003-

	if (cBox[8].isSelected()) {
		comBox[8].setEnabled(true);
		long hh = (long)Integer.parseInt(comBox[8].getSelectedItem().toString())*90;
		if (hh!=0) 
			options[28]=hh;
		else
			options[28]=0;
 	} else {
		comBox[8].setEnabled(false);
		options[28]=0;
	}

	if (cBox[5].isSelected()) {
		comBox[2].setEnabled(true);
		options[18]=0x100000L*Integer.parseInt(comBox[2].getSelectedItem().toString());
	} else {
		comBox[2].setEnabled(false);
		options[18]=0;
	}

	options[22]=comBox[4].getSelectedIndex();  //vbvSize
	options[23]=comBox[5].getSelectedIndex();  //vbvDelay
	options[24]=comBox[6].getSelectedIndex();  //AR

	/** BR in all sequ **/
	int BRperSequ = comBox[3].getSelectedIndex();
	if (BRperSequ==0){
		options[0]=0; 
		options[1]=0; 
		options[2]=0; 
		options[3]=1; 
		options[4]=0;
	}else if (BRperSequ==1){
		options[0]=0; 
		options[3]=0; 
		options[4]=0; 
	}else if (BRperSequ==2){
		options[0]=1; 
		options[4]=0; 
	}else if (BRperSequ >3){
		options[0]=0; 
		options[3]=0; 
		options[4]=(comBox[3].getSelectedIndex()-3)*2500;
	}

	/** BR in 1st sequ **/
	switch (comBox[15].getSelectedIndex()){
	case 0:
		options[1]=0; 
		options[2]=0; 
		break;
	case 1:
		options[1]=0; 
		options[2]=1; 
		break;
	case 2:
		options[1]=1; 
		options[2]=0; 
		break;
	case 3:
		options[1]=0; 
		options[2]=0; 
		break;
	}

	/** audio1 **/
	options[16] = cBox[0].isSelected() ? 1 : 0;
	options[38] = cBox[20].isSelected() ? 1 : 0;
	options[9]  = cBox[1].isSelected() ? 1 : 0;

	switch (comBox[7].getSelectedIndex()) {
		case 0:	options[10]=0; 
			cBox[1].setEnabled(true); 
			break;
		case 1:	options[9]=1; 
			options[10]=1; 
			cBox[1].setSelected(true); 
			cBox[1].setEnabled(false); 
			break;
		case 2:	options[9]=1; 
			options[10]=2; 
			cBox[1].setSelected(true); 
			cBox[1].setEnabled(false); 
			break;
		case 3:	options[9]=1; 
			options[10]=3; 
			cBox[1].setSelected(true); 
			cBox[1].setEnabled(false); 
			break;
		case 4: options[9]=1; 
			options[10]=4; 
			cBox[1].setSelected(true); 
			cBox[1].setEnabled(false); 
	} 

	if (cBox[6].isSelected()) 
		options[26]|=1; 
	else 
		options[26]&=~1;

	if (cBox[7].isSelected()) 
		options[26]|=2; 
	else
		options[26]&=~2;

	if (cBox[50].isSelected()) 
		cBox[4].setSelected(false);
	if (cBox[4].isSelected()) 
		cBox[50].setSelected(false);

}





/******************
 * show ScanInfos * 
 ******************/
//DM26032004 081.6 int18 changed
//DM18062004 081.7 int05 changed
public void ScanInfo(String file)
{
	File info = new File(file);
	logtab.setSelectedIndex(1);
	String values = "";
	FileInfoTextArea.setBackground(Color.white);

	values += "Location:\t" + info.getParent() + "\n";
	values += "Name:\t" + info.getName() + "\n";

	int source = raw_interface.isAccessibleDisk(file) ? 2 : (info.exists() ? 1 : 0);
	long size = source == 2 ? raw_interface.getFileSize(file) : info.length();

	if (source > 0)
	{
		values += "Size:\t" + (size / 1048576) + " MB (" + size + " bytes)" + "\n";
		String type = "Type:\t" + scan.Type(file) + "\n"; // must be first when scanning
		values += "Date:\t" + scan.Date(file) + "\n";
		values += "\n";
		values += type;
		values += "Video:\t" + scan.getVideo() + "\n";
		values += "Audio:\t" + scan.getAudio() + "\n";
		values += "Teletext:\t" + scan.getText()+ "\n"; //DM28042004 081.7 int02 changed
		values += "Subpict.:\t" + scan.getPics()+ "\n"; //DM28042004 081.7 int02 add
		values += "est. Playtime:\t" + scan.getPlaytime()+ "\n";
		FileInfoTextArea.setBackground(scan.isSupported() ? new Color(225,255,225) : new Color(255,225,225));
	}

	else
		values += "\nFile doesn't exists!\n";

	FileInfoTextArea.setText(values);
}


/****************
 * load inifile * 
 ****************/
//DM26032004 081.6 int18 changed
//DM13062004 081.7 int04 changed
public void iniload()
{
	try 
	{

		//DM13062004 081.7 int04 add
		Object table_indices[] = Common.checkUserColourTable(ColourTablesFile);
		if (table_indices != null)
		{
			for (int i = 0; i < table_indices.length; i++)
				comBox[11].addItem(table_indices[i]);
		}

	if (new File(inifile).exists())
	{

	BufferedReader inis = new BufferedReader(new FileReader(inifile));
	String path="", ck=""; 
	boolean val = false;

	iniread:
	while (true)
	{
		path = inis.readLine();

		if (path==null) 
			break iniread;

		if (path.startsWith("d"))
		{ 
			if (path.length()>2 && !path.substring(1,2).equals("*"))
				d2vfield[Integer.parseInt(path.substring(1,2))].setText(path.substring(2,path.length()));
		}
		else if (path.startsWith("e"))
		{ 
			if (path.length()>2 && !path.substring(1,2).equals("*"))
				exefield[Integer.parseInt(path.substring(1,2))].setText(path.substring(2,path.length()));
		}
		else if (path.startsWith("i*"))
		{ 
			if (path.substring(2,path.length())!=null && new File(path.substring(2,path.length())).exists())
				comBox[12].addItem(path.substring(2,path.length()));
		}
		else if (path.startsWith("o*"))
		{
			outchange=true;
			if (path.substring(2,path.length())!=null && new File(path.substring(2,path.length())).exists())
				comBox[13].addItem(path.substring(2,path.length()));
			outchange=false;
		}
		else if (path.startsWith("r"))
		{
			ck = path.substring(path.indexOf("*")+1,path.length());
			if (ck.equals("true")) 
				val=true; 
			else 
				val=false;
			RButton[Integer.parseInt(path.substring(1,path.indexOf("*")))].setSelected(val);
		}
		else if (path.startsWith("c"))
		{
			ck = path.substring(path.indexOf("*")+1,path.length());
			if (ck.equals("true")) 
				val=true; 
			else 
				val=false;
			int lfn = Integer.parseInt(path.substring(1,path.indexOf("*")));
			if (lfn < cBox.length) 
				cBox[lfn].setSelected(val);
		}
		else if (path.startsWith("p"))
		{
			outchange=true;
			ck = path.substring(path.indexOf("*") + 1, path.length());
			int lfn = Integer.parseInt(path.substring( 1, path.indexOf("*")));

			if (lfn < comBox.length)
				comBox[lfn].setSelectedItem(ck);

			outchange=false;
		}
		else if (path.startsWith("wx")) 
			framelocation[0]=Integer.parseInt(path.substring(2,path.length()));
		else if (path.startsWith("wy")) 
			framelocation[1]=Integer.parseInt(path.substring(2,path.length()));
		else if (path.startsWith("ww")) 
			framelocation[2]=Integer.parseInt(path.substring(2,path.length()));
		else if (path.startsWith("wh")) 
			framelocation[3]=Integer.parseInt(path.substring(2,path.length()));
		}

		inis.close();
		inputlist();

	}
	} 
	catch (IOException e1)
	{
		//DM25072004 081.7 int07 add
		Msg("ini load error: " + e1);
	}
}   


/****************
 * save inifile *
 ****************/
public static void inisave() //DM26012004 081.6 int12 changed, //DM26032004 081.6 int18 changed
{
	try 
	{
	PrintWriter inis = new PrintWriter(new FileWriter(inifile));

	inis.println("// editable fields");
	for (int a=0; a<d2vfield.length; a++)
	{
		inis.print("d"+a); 
		inis.println(d2vfield[a].getText());
	}

	inis.println("// editable post command fields");
	for (int a=0; a<exefield.length; a++)
	{
		inis.print("e"+a); 
		inis.println(exefield[a].getText());
	}

	inis.println("// autoload directories");
	for (int a=0; a<comBox[12].getItemCount(); a++)
	{
		inis.print("i*"); 
		inis.println(comBox[12].getItemAt(a));
	}

	inis.println("// output directories");
	for (int a=0; a<comBox[13].getItemCount(); a++)
	{
		inis.print("o*"); 
		inis.println(comBox[13].getItemAt(a));
	}

	inis.println("// all radio buttons");
	for (int a=0; a<RButton.length; a++)
	{
		inis.print("r"+a+"*"); 
		inis.println(RButton[a].isSelected());
	}

	inis.println("// all checkboxes");
	for (int a=0; a<cBox.length; a++)
	{
		inis.print("c"+a+"*"); 
		inis.println(cBox[a].isSelected());
	}

	inis.println("// all JComboboxes");
	for (int a=0; a<comBox.length; a++)
	{
		inis.print("p"+a+"*"); 
		inis.println(comBox[a].getSelectedItem());
	}

	inis.println("// last position of main frame");
	inis.println("wx"+frame.getX());
	inis.println("wy"+frame.getY());
	inis.println("ww"+frame.getWidth());
	inis.println("wh"+frame.getHeight());

	inis.close();
	} 
	catch (IOException e1)
	{
		//DM25072004 081.7 int07 add
		Msg("ini save error: " + e1);
	}
}



/**************************
 * refresh inputfileslist * 
 **************************/
//DM18062004 081.7 int05 changed
public void inputlist()
{
	ArrayList arraylist = new ArrayList();

	for (int a=0; a < comBox[12].getItemCount(); a++)
	{
		File addlist[] = new File(comBox[12].getItemAt(a).toString()).listFiles();

		if (addlist.length > 0) 
			Arrays.sort(addlist);

		for (int b=0; b < addlist.length; b++)
		{
			if (addlist[b].isFile() ) 
				arraylist.add(addlist[b].toString());
		}
	}

	// ext disk access
	raw_interface.add_native_files(arraylist);

	if (arraylist.size() > 0) 
		inputfiles = arraylist.toArray();

	else 
		inputfiles = new Object[0];

	list1.setListData(inputfiles);
}   


/*************
 * go thread *
 *************/
class GoListener implements ActionListener
{
	public void actionPerformed(ActionEvent e)
	{
		String actName = e.getActionCommand();
		updateState();

		logtab.setSelectedIndex(0); //DM26032004 081.6 int18 add
		TextArea.setBackground(Color.white); //DM26032004 081.6 int18 add

		if (actName.equals("Go!") && comBox[0].getItemCount()>0)
		{
			comBox[9].removeAllItems();
			options[33]=-1;
			new WORK().start();
		}
		else if (actName.equals("i") && comBox[0].getItemCount()>0)
		{
			comBox[9].removeAllItems();
			options[33]=-1;
			qinfo=true; 
			options[56]= (0x100000L * Integer.parseInt(comBox[21].getSelectedItem().toString())); 
			new WORK().start();
		}
		else if (actName.equals("C"))
		{ 
			Msg("--- process cancelled ----"); 
			TextArea.setBackground(new Color(230, 230, 255)); //DM14052004 081.7 int02 add
			qpause=false; 
			qbreak=true; 
			options[18]=0; 
		}
		else if (actName.equals("P"))
		{
			if (!qpause)
			{ 
				Msg("--- process paused ----"); 
				TextArea.setBackground(new Color(255, 255, 220)); //DM26032004 081.6 int18 add
				qpause=true; 
			}
			else
			{ 
				Msg("--- process resumed ----"); 
				TextArea.setBackground(Color.white); //DM26032004 081.6 int18 add
				qpause=false; 
			}
		}
		else if (actName.equals("e") && comBox[9].getItemCount()>0)
		{
			options[31]=1; 
			options[30]=0;
			options[33]=Integer.parseInt(comBox[9].getSelectedItem().toString(),16);
			TextArea.setText("extracting raw(PES) data of ID 0x"+comBox[9].getSelectedItem().toString()+"...");
			new WORK().start();
		}
	}
}


/****************
 * show java EV *
 ****************/
public void javaEV()
{
	TextArea.setText(java.text.DateFormat.getDateInstance(java.text.DateFormat.FULL).format(new Date()));
	TextArea.append("  " + java.text.DateFormat.getTimeInstance(java.text.DateFormat.FULL).format(new Date()));
	TextArea.append("\njava.version\t" + System.getProperty("java.version"));
	TextArea.append("\njava.vendor\t" + System.getProperty("java.vendor"));
	TextArea.append("\njava.home\t" + System.getProperty("java.home"));
	TextArea.append("\njava.vm.version\t" + System.getProperty("java.vm.version"));
	TextArea.append("\njava.vm.vendor\t" + System.getProperty("java.vm.vendor"));
	TextArea.append("\njava.vm.name\t" + System.getProperty("java.vm.name"));
	TextArea.append("\njava.class.vers\t" + System.getProperty("java.class.version"));
	TextArea.append("\njava.class.path\t" + System.getProperty("java.class.path"));
	TextArea.append("\nos.name\t" + System.getProperty("os.name"));
	TextArea.append("\nos.arch\t" + System.getProperty("os.arch"));
	TextArea.append("\nos.version\t" + System.getProperty("os.version"));
	TextArea.append("\nuser.name\t" + System.getProperty("user.name"));
	TextArea.append("\nuser.home\t" + System.getProperty("user.home"));
	TextArea.append("\nini.file\t" + inifile);

	//DM18062004 081.7 int05 add
	TextArea.append("\ndisk access:\t" + raw_interface.GetLoadStatus());
}


/****************
 *load ac3.bin file *
 ****************/
public static int loadAC3() {
	Audio AC3Audio = new Audio();
	AC3list.clear();
	if (!new File(inidir+"ac3.bin").exists()) 
		return 0;
	try {
	RandomAccessFile in = new RandomAccessFile(inidir+"ac3.bin","r");
	byte[] check = new byte[(int)in.length()];
	in.seek(0); 
	in.read(check); 
	in.close();

	Msg(""); //DM22062004 081.7 int05 add

	int a=0, frame_counter=0;
	while (a < check.length) {
		AC3Audio.AC3_parseHeader(check,a);
		Msg("("+frame_counter+") "+AC3Audio.AC3_saveAnddisplayHeader());
		byte[] ac3data = new byte[AC3Audio.Size];
		System.arraycopy(check,a,ac3data,0,AC3Audio.Size);
		AC3list.add(ac3data);
		a += AC3Audio.Size;
		frame_counter++;
	}
	check = null;
	return frame_counter;
	} 
	catch (IOException e5) { Msg("error loading ac3.bin"); }

	AC3list.clear();
	return 0;
}


/****************
 *load cutfile *
 ****************/
public static void loadCutPoints(String file) {
	try {
		BufferedReader points = new BufferedReader(new FileReader(file));
		String point="";
		ArrayList pointList = new ArrayList();
		while (true) {
			point = points.readLine();
			if (point==null) 
				break;
			if (point.trim().equals("")) 
				continue;

			//DM09032004 081.6 int18 add
			if (point.startsWith("use")) 
			{
				comBox[17].setSelectedItem(point);
				continue;
			}

			pointList.add(point);
		}
		points.close();
		cutlist.add(pointList);
		Msg("-> loading "+pointList.size()+" Cutpoints...");
	} 
	catch (IOException e5) { 
		Msg("error loading "+file);
	}
	return;
}

public static void loadIDs(String nIDs) {  //DM28112003 081.5++
	StringTokenizer st = new StringTokenizer(nIDs,",");
	ArrayList nIDList = new ArrayList();
	String nID=null;
	int a=0;
	while (st.hasMoreTokens()) {
		nID=st.nextToken();
		if (!nID.startsWith("0x"))
			nID = "0x"+Integer.toHexString(Integer.parseInt(nID));
		nIDList.add(nID);
		a++;
	}
	speciallist.add(nIDList);
	Msg("-> loading "+nIDList.size()+" (P)IDs...");
	return;
}


public static void main(String[] args) {

	System.out.println(version[0]+"/"+version[1]+" "+version[2]+" "+version[3]);
	System.out.println();

	System.out.println("quick CL usage: (Note: CL also loads the GUI components, without displaying)");
	System.out.println("without options  ...to start the GUI");
	System.out.println("[options] <sourcefile 1>...[<sourcefile x>]");
	System.out.println(" ");
	System.out.println("options: -dvx1..4, -c <x>, -o <x>, -n <x>, -l, -p <x>, -i <x>, -g");
	System.out.println("-c [path]inifile  ...use that specified iniFile instead of the standard");
	System.out.println("-dvx1  ...create a .d2v ProjectFile on demux");
	System.out.println("-dvx2  ...create a .d2v ProjectFile + .ac3.wav (RIFF WAVE Header)");
	System.out.println("-dvx3  ...create a .d2v ProjectFile + .mpa.wav (RIFF WAVE Header)");
	System.out.println("-dvx4  ...create a .d2v ProjectFile + .ac3.wav + mpa.wav (RIFF WAVE Header)");
	System.out.println("-o <path>  ...use that specified directory for output");
	System.out.println("-n <filename>  ...use that specified filename for output");
	System.out.println("-p <file>  ...use that text based file as cutpoint list");
	System.out.println("-i <tokens>  ...use only these (P)IDs, separated by comma \",\"");
	System.out.println("-g  ...display the GUI using all given CLI options");
	System.out.println("-l  ...write the normal logfile");
	System.out.println("use -dvx OR -c & -o/-n/-l/-p/-i/-g or no options (for std)");
	System.out.println(" ");
	System.out.println("java.version\t"+System.getProperty("java.version"));
	System.out.println("java.vendor\t"+System.getProperty("java.vendor"));
	System.out.println("java.home\t"+System.getProperty("java.home"));
	System.out.println("java.vm.version\t"+System.getProperty("java.vm.version"));
	System.out.println("java.vm.vendor\t"+System.getProperty("java.vm.vendor"));
	System.out.println("java.vm.name\t"+System.getProperty("java.vm.name"));
	System.out.println("java.class.vers\t"+System.getProperty("java.class.version"));
	System.out.println("java.class.path\t"+System.getProperty("java.class.path"));

	if ( args.length>0 && args[0].equalsIgnoreCase("-?") )
		System.exit(0);

	System.out.println();

	for (int a=0;a<terms.length;a++) 
		System.out.println(terms[a]);
	System.out.println();

	//DM15022004 081.6 int17 new
	StartUp startup = new StartUp();
	if (args.length == 0)
		startup.show();

	X panel = new X();

	comchange=true;
	UIManager.LookAndFeelInfo[] lfi =  UIManager.getInstalledLookAndFeels();

	for (int a=0;a<lfi.length;a++) 
		comBox[16].addItem(lfi[a].getClassName());

	comchange=false;

	frametitle = version[0]+"/"+version[1]+" "+version[2]+" "+version[3];
	frame.setTitle(frametitle); //DM20032004 081.6 int18 changed

	//DM28112003 081.5++
	boolean loadGUI=false;
	boolean iniLoaded=false;
	boolean IDsLoaded=false;
	boolean CutsLoaded=false;
	int ac3f=0;

	if (args.length > 0) {

		int aaa1=0;
		if ( args[0].equalsIgnoreCase("-c") ) {
			if ( args.length==1) {
				System.out.println("stopped, no config file ...");
				System.exit(0);
			}
			inifile = args[1];
			if ( !(new File(inifile).exists()) ) {
				System.out.println("stopped, config file "+inifile+" not found ...");
				System.exit(0);
			} 
			System.out.println("use config file "+inifile+" ...");
			aaa1=2;
			panel.iniload();
			iniLoaded=true;
		} else if ( args[0].equalsIgnoreCase("-dvx1") ) {
			aaa1=1;
			cBox[30].setSelected(true);
		} else if ( args[0].equalsIgnoreCase("-dvx2") ) {
			aaa1=1;
			cBox[30].setSelected(true);
			cBox[12].setSelected(true);
		} else if ( args[0].equalsIgnoreCase("-dvx3") ) {
			aaa1=1;
			cBox[30].setSelected(true);
			cBox[4].setSelected(true);
		} else if ( args[0].equalsIgnoreCase("-dvx4") ) {
			aaa1=1;
			cBox[30].setSelected(true);
			cBox[12].setSelected(true);
			cBox[4].setSelected(true);
		} else {
			System.out.println("use last config or standard ...");
			panel.iniload();
			iniLoaded=true;
		}

		if (RButton[0].isSelected() || !RButton[1].isSelected()) {
			System.out.println("-> to agree to these terms you have to start the GUI first");
			//System.exit(0);
		}

		for (int f=aaa1;f<args.length-1;f++) {
			if ( args[f].equalsIgnoreCase("-g") ) {
				loadGUI=true;
				aaa1++;
			} else if ( args[f].equalsIgnoreCase("-o") ) {
				outchange=true;
				if (new File(args[f+1]).exists()) 
					comBox[13].insertItemAt(args[f+1],0);
				outchange=false;
				aaa1+=2;
			} else if ( args[f].equalsIgnoreCase("-n") ) {
				newOutName = args[f+1];
				aaa1+=2;
			} else if ( args[f].equalsIgnoreCase("-l") ) {
				cBox[21].setSelected(true);
				aaa1++;
			} else if ( args[f].equalsIgnoreCase("-p") ) {
				if (new File(args[f+1]).exists())
					loadCutPoints(args[f+1]);
				CutsLoaded=true;
				aaa1+=2;
			} else if ( args[f].equalsIgnoreCase("-i") ) {
				loadIDs(args[f+1]);
				IDsLoaded=true;
				aaa1+=2;
			}
		}

		panel.updateState();

		//DM22062004 081.7 int05 changed
		if ((ac3f = loadAC3()) > 0) 
			Msg("ac3.bin contains " + ac3f + " AC3 frames");

		cBox[11].setSelected(false);
		options[30]=0;
		comchange=true;
		comBox[0].addItem("0"); 
		collfiles = new ArrayList[1];
		collfiles[0] = new ArrayList();

		for (int a=aaa1; a < args.length; a++) 
			collfiles[0].add(args[a]);

		if (comBox[13].getItemCount()>0) 
			collout.add(comBox[13].getItemAt(0));
		else 
			collout.add(outalias);

		if (!IDsLoaded)
			speciallist.add(new ArrayList());
		if (!CutsLoaded)
			cutlist.add(new ArrayList());
		comchange=false;
		comBox[0].setSelectedIndex(comBox[0].getItemCount()-1); //DM26032004 081.6 int18 changed

		if (!loadGUI){ //DM28112003 081.5++
			running = true;
			doitButton.doClick();
		}

	}

	if (args.length == 0 || loadGUI) {
	/***** loading GUI ******/

		if (loadGUI){ //DM28112003 081.5++
			frame.addWindowListener (new WindowAdapter() { 
				public void windowClosing(WindowEvent e) { 
					System.exit(0); 
				}
			});
		}else{
			frame.addWindowListener (new WindowAdapter() { 
				public void windowClosing(WindowEvent e) { 
					X.inisave();
					System.exit(0); 
				}
			});
		}
		frame.addComponentListener(new ComponentListener() {
			public void componentHidden(ComponentEvent e) {} 
			public void componentMoved(ComponentEvent e) {} 
			public void componentShown(ComponentEvent e) {} 
			public void componentResized(ComponentEvent e) {
				Component c = e.getComponent();
				Dimension preferred = new Dimension(714, 460), current = c.getSize();  //DM26032004 081.6 int18 changed
				double newHeight = (preferred.getHeight() > current.getHeight()) ? preferred.getHeight() : current.getHeight();
				double newWidth = (preferred.getWidth() > current.getWidth()) ? preferred.getWidth() : current.getWidth();
				c.setSize(new Dimension((int)newWidth, (int)newHeight));
			}
		});

		frame.getContentPane().add(panel);
		if (!iniLoaded)
			panel.iniload();

		frame.setLocation(framelocation[0],framelocation[1]);
		frame.setSize(new Dimension(framelocation[2],framelocation[3]));

		brm.surf.start();

		panel.updateState();
		panel.javaEV();

		//DM22062004 081.7 int05 changed
		if ((ac3f = loadAC3()) > 0) 
			Msg("ac3.bin contains " + ac3f + " AC3 frames");

		startup.set(RButton[1].isSelected());

		if (startup.get())
		{
			setVisible0(true);
			startup.close();
		}
	}
}   // end methode main

//DM20032004 081.6 int18 add
public static void setVisible0( boolean visible)
{
	frame.setVisible(visible);
}

//DM20032004 081.6 int18 add
public static void setButton(int button, boolean selected)
{
	RButton[button].setSelected(selected);
}

//DM20032004 081.6 int18 add
public static String[] getTerms()
{
	return terms;
}

/************
 * messages *
 ************/
public static void Msg(String msg)
{
	//DM25072004 081.7 int07 add
	if (msg == null) 
		return;

	if (options[30]==1) 
		System.out.println(msg); 

	if (running) 
		System.out.println("\r"+msg); 

	else
	{
		TextArea.append("\r\n"+msg);
		viewport.setViewPosition(new Point(0,TextArea.getHeight()));
		//viewport.repaint();
	}
	messagelog += "\r\n"+msg;
}


/**********************
 * show output MB *
 **********************/
public static void showOutSize()
{
	outSize.setText(""+(options[39]/1048576L)+"MB");

} 

/*****************************************
 * video timelength read from ptslogfile *
 *****************************************/
public static long calcvideotime(String logfile) {
	long vtime=0;
	try 
	{

	long vlogsize = new File(logfile).length();
	RandomAccessFile vlog = new RandomAccessFile(logfile,"r");
	vlog.seek(vlogsize-8);
	vtime = vlog.readLong();
	vlog.close();
	}
	catch (IOException e) {  X.Msg("..ptsfile access error"); }
	return vtime;
}


// parse cut field value
//DM17012004 081.6 int11 new
//DM29012004 081.6 int12 change
public static long parseValue(String value, boolean demux){
	value = value.replace('.',':');

	if (value.indexOf(":")<0)
		return Long.parseLong(value);

	StringTokenizer st = new StringTokenizer(value,":");
	String str=null;
	long val = 0, frametime = !demux?90:options[14];
	long mp[] = { 324000000L,5400000L,90000L,frametime }; //h,m,s,f
	for (int a=0; st.hasMoreTokens() && a<4; a++){
		str = st.nextToken();
		val += (mp[a]*Long.parseLong(str));
	}

	return val;
}

//DM29012004 081.6 int12 new
public static String parseValue(long value){
	if (comBox[17].getSelectedIndex()<4)
		return String.valueOf(value);

	String str="";
	long frametime = 1;
	value/=90;

	java.text.DateFormat time = new java.text.SimpleDateFormat("H:mm:ss:");
	time.setTimeZone(java.util.TimeZone.getTimeZone("GMT+0:00"));

	java.util.Calendar cal = java.util.Calendar.getInstance();
	cal.setTimeZone(java.util.TimeZone.getTimeZone("GMT+0:00"));
	cal.setTime(new java.util.Date(value));
	int frame = (cal.get(14) / ((int)frametime));
	str = time.format(new java.util.Date(value)) + (frame<10?"0":"") + frame;

	return str;
}


/************
 * make cut *
 ************/
//DM17012004 081.6 int11 changed
//DM29012004 081.6 int12 fix
//DM18022004 081.6 int17 changed
public static boolean makecut(String cuts_filename, long startPTS, long comparePoint, ArrayList newcut, int lastframes) {
	if (ctemp.isEmpty())
		return true;

	CP = new long[2];
	long[] abc;

	if ( cutcount < ctemp.size() ) { 
		if ( comparePoint > parseValue(ctemp.get(cutcount).toString(),true) ) {
			if ((cutcount&1)==1) {		//ungerade == cutout
				bool=false; 
				for (int c = newcut.size()-1; c >- 1; c--)
				{
					abc = (long[])newcut.get(c);
					if ( abc[0] < parseValue(ctemp.get(cutcount).toString(),true) )
					{ 
						bool=true; 
						CP=abc;
						Msg("-> cut-in @ GOP#"+(clv[6]-1)+" / new vframe "+lastframes+" / new Timecode "+base_time.format(new java.util.Date((long)(lastframes*(double)(options[14]/90.0f)) )));
						saveCuts(comparePoint,startPTS,lastframes,cuts_filename); //DM18022004 081.6 int17 new

						if (lastframes>0) 
							cell.add(""+lastframes); // celltimes for cutin

						break;
					}
				}
				if (!bool)
				{
					Msg("-> cut-out @ GOP#"+(clv[6]-1));
					saveCuts(comparePoint,startPTS,lastframes,cuts_filename); //DM18022004 081.6 int17 new
				}

				cutcount++;
				return bool;

			}else{				//ungerade == cutin

				bool=true;
				cutcount++;

				Msg("-> cut-in @ GOP#"+(clv[6]-1)+" / new vframe "+lastframes+" / new Timecode "+base_time.format(new java.util.Date((long)(lastframes*(double)(options[14]/90.0f)) )));
				saveCuts(comparePoint,startPTS,lastframes,cuts_filename); //DM18022004 081.6 int17 new

				if (lastframes>0) 
					cell.add(""+lastframes); // celltimes for cutin

				if (cutcount >= ctemp.size()) 
					return bool;

				for (int c=newcut.size()-1;c>-1;c--) {
					abc = (long[])newcut.get(c);
					if ( abc[0] < parseValue(ctemp.get(cutcount).toString(),true) ) { 
						CP=abc;
						cutcount++;
						break;
					}
				}
				return bool;
			}
		}
	} else { 
		if (!bool && options[37]==10000000) 
			options[37]=comparePoint;
	}
	return bool;
}

//DM18022004 081.6 int17 new
private static void saveCuts(long cutposition, long startPTS, long lastframes, String cuts_filename)
{
	if (RButton[11].isSelected())
	{
		try
		{
		cuts_filename += ".Xpl";
		PrintWriter pts_writer = new PrintWriter(new FileOutputStream(cuts_filename,lastframes>0?true:false));

		if (new File(cuts_filename).length()==0)  //DM09032004 081.6 int18 add
			pts_writer.println(comBox[17].getItemAt(3).toString());

		pts_writer.println(startPTS);
		pts_writer.close();
		Msg("-> saving cut PTS value ("+startPTS+") to file..");
		}
		catch (IOException e)
		{
			//DM25072004 081.7 int07 add
			Msg("cutpoint save error: " + e);
		}
	}
}



/**************
 * log thread *
 **************/
class LOG extends Thread {
	public void run() {
		try
		{
		logging=new PrintStream(new FileOutputStream(loggin));
		System.setOut(logging);
		}
		catch (IOException e) { Msg("log error "+e); }
	}
}




/**************
 * main thread *
 **************/
class WORK extends Thread {


/** normal process **/ 
public void run() {

	//thread = this.currentThread(); //DM26032004 081.6 int18 add //unused ATM

	try 
	{

	java.text.DateFormat sms = new java.text.SimpleDateFormat("HH:mm:ss.SSS");
	sms.setTimeZone(java.util.TimeZone.getTimeZone("GMT+0:00"));
	long timeneeded = System.currentTimeMillis();




	TextArea.setBackground(Color.white); //DM26032004 081.6 int18 changed

	int a=0,b=0,d=0;

	doitButton.setEnabled(false);
	scanButton.setEnabled(false);
	breakButton.setEnabled(true);
	pauseButton.setEnabled(true);
	extract.setEnabled(false);

	//DM01042004 081.6 int18 changed
	progress.setString("preparing collection(s)...");
	progress.setStringPainted(true);

	progress.setValue(0);
	msoff.setText("A/V offset");
	ttxheaderLabel.setText("");
	ttxvpsLabel.setText("");

	brm.surf.stop();
	yield();

	if (options[33]==-1) {
		TextArea.setText(java.text.DateFormat.getDateInstance(java.text.DateFormat.FULL).format(new Date())+"  "+java.text.DateFormat.getTimeInstance(java.text.DateFormat.FULL).format(new Date()));
		Msg(""+version[0]+" ("+version[1]+")");
		if (cBox[18].isSelected()) 
			b = comBox[0].getItemCount();
		else {
			a = comBox[0].getSelectedIndex();
			b = a+1;
		}

		Msg("\r\n=== session infos ===");

		for ( ; a<b ; a++,d++) {
			workinglist.clear();
			workinglist = (ArrayList)collfiles[a].clone();
			comBox[0].setSelectedIndex(a);
			Msg("\r\n=> working with collection "+a);
			currentcoll = a;

			if (workinglist.size() > 0) { 
				java.util.Arrays.fill(VBASIC,null); //DM08032004 081.6 int18 add
				ttxheaderLabel.setText("");
				ttxvpsLabel.setText("");
				ctemp = (ArrayList)cutlist.get(a);
				origframes=0;
				cutcount=0;
				FileNumber=0;
				bool=false;
				tf.setfirstID();
				workouts = collout.get(a).toString();
				String firstfile = workinglist.get(0).toString();

				//DM15072004 081.7 int06 moved and add++
				if (options[18]>0) 
					Msg("-> split output @ appr. "+(options[18]/1048576)+" MB");

				if (cBox[8].isSelected())
					Msg("-> add time offset to audio/ttx/pics :  " + (options[28] / 90) + " ms (if it follows on a video)");

				for (int i = 55; i < 61; i++)
					if (!cBox[i].isSelected())
						Msg("-> PES stream type disabled: " + cBox[i].getText());

				Msg(" ");
				//DM15072004 081.7 int06 moved and add--

				if (workouts.equals(outalias))
					workouts = new File(firstfile).getParent();

				//DM26062004 081.7 int05 add
				if (workouts == null)
					workouts = inidir;

				if ( !workouts.endsWith(filesep) ) 
					workouts += filesep;

				if (cBox[2].isSelected()){
					workouts += "("+a+")"+filesep;
					new File(workouts).mkdirs();
				}

				//DM12072004 081.7 int06 add
				if (cBox[54].isSelected() && comBox[19].getSelectedIndex() == 1)
				{
					workouts += new File(firstfile).getName()
						+ filesep
						+ new java.text.SimpleDateFormat("yyyy-MM-dd.HH.mm.ss.SS").format(new Date()) + ".rec"
						+ filesep;

					new File(workouts).mkdirs();
				}

				Msg("-> write output files to :");
				Msg("\t"+workouts);

				if (ctemp.size()>0)
					Msg("-> "+ctemp.size()+" cutpoint(s) defined ("+comBox[17].getSelectedItem()+")");

				String fchilds = (new File(firstfile).getName()).toString();
				if ( fchilds.lastIndexOf(".") != -1 ) 
					fchilds = fchilds.substring(0,fchilds.lastIndexOf("."));

				if (cBox[11].isSelected()) {
					loggin = workouts+fchilds+"_log.txt";
					options[31]=0; 
					options[30]=1;
					new LOG().start();
				} 

				//DM09072004 081.7 int06 changed
				if (cBox[21].isSelected())
				{
					if (cBox[54].isSelected() && comBox[19].getSelectedIndex() == 1)
						loggin2 = workouts + "summary.vdr";

					else
						loggin2 = workouts + fchilds + "_X.log";
				}

				/** quick pre-run for TS autoPMT **/ 
				if (!qinfo && comBox[19].getSelectedIndex()==4 && cBox[41].isSelected()) {
					qinfo=true;
					options[56]= (0x100000L * Integer.parseInt(comBox[21].getSelectedItem().toString()));
					long splitlen = options[18]; 
					options[18]=0;

					working();

					qinfo=false;
					options[18]=splitlen;
					workinglist = (ArrayList)collfiles[a].clone();
					Msg("--- end of quick info ---");
				}

				/** doit standard **/ 
				working();

				java.util.Arrays.fill(VBASIC,null); //DM08032004 081.6 int18 add

			} else {
				msoff.setText("A/V offset");
				progress.setString("");
				progress.setStringPainted(false);
				progress.setValue(0);
				Msg("no input file...");
			}
		}
	} else {  // extract raw

		workinglist.clear();
		currentcoll = comBox[0].getSelectedIndex();
		workinglist = (ArrayList)collfiles[currentcoll].clone();

		if (workinglist.size()>0) {
			String firstfile = workinglist.get(0).toString();
			workouts = new File(firstfile).getParent();
			if ( !workouts.endsWith(filesep) ) 
				workouts += filesep;
				Msg("write separated raw file to "+workouts);
 
				String fchilds = (new File(firstfile).getName()).toString();
				if ( fchilds.lastIndexOf(".") != -1 )
					fchilds = fchilds.substring(0,fchilds.lastIndexOf("."));

				working();
				options[33]=0;
				inputlist();
		} else Msg("Collection is empty... ");
	} 

	timeneeded = System.currentTimeMillis() - timeneeded;
	progress.setString("done...    "+d+" collection(s) processed  @  "+sms.format(new java.util.Date(timeneeded)));
	progress.setStringPainted(true);

	doitButton.setEnabled(true);
	scanButton.setEnabled(true);
	pauseButton.setEnabled(false);
	breakButton.setEnabled(false);
	yield();
	options[30]=0;
	options[31]=0;
	}
	catch (Exception e8) {
		//TextArea.setForeground(new Color(200,0,0));
		Msg("stopped...");
		StringWriter aa = new StringWriter();
		e8.printStackTrace(new PrintWriter(aa));
		Msg(""+aa.toString());
		TextArea.setBackground(new Color(255,225,225)); //DM26032004 081.6 int18 add
	}
	catch (Error e9) {
		//TextArea.setForeground(new Color(200,0,0));
		Msg("stopped...");
		StringWriter aa = new StringWriter();
		e9.printStackTrace(new PrintWriter(aa));
		Msg(""+aa.toString());
		TextArea.setBackground(new Color(255,225,225)); //DM26032004 081.6 int18 add
	}

	doitButton.setEnabled(true);
	scanButton.setEnabled(true);
	pauseButton.setEnabled(false);
	breakButton.setEnabled(false);
	options[30]=0;
	options[31]=0;
	if (qinfo) 
		Msg("--- end of quick info ---");
	qpause=false;
	qbreak=false;
	yield();

	if (comBox[9].getItemCount()>0) 
		extract.setEnabled(true);

	qinfo=false;
	if (running) 
		System.exit(0);
	frame.setTitle(frametitle);
}  


/*********
 * pause *
 *********/

public void pause(){
	try
	{ 
	sleep(1000); 
	}
	catch (InterruptedException ie) { 
		Msg(""+ie); 
	}
}


/*******************************
 * call the important routines *
 *******************************/
public void working() {

	System.gc();
	String vptslog = "-1", oldvptslog="-1";

	long splitsize=options[18];
	options[19]=0;
	options[20]=0;
	options[21]=0;
	options[25]=0;
	options[27]=0;
	options[37]=10000000;
	options[39]=0;
	options[40]=0;
	options[44]=0;
	options[48]=0;
	options[42]=0;
	options[11]=0;
	options[14]=3600;
	options[52]=0; //DM13112003 081.5++
	options[53]=0;
	options[54]=0;
	PureVideo=false;
	combvideo.clear();

	firstVideoPTS=-1; //DM17012004 081.6 int11 new

	//DM09072004 081.7 int06 changed
	//options[43] = (comBox[18].getSelectedIndex()>0) ? (0L+(Integer.parseInt(comBox[18].getSelectedItem().toString().substring(2,4),16))) : 0;
	options[43] = 0; 

	boolean vdr=true, pva=true, mpg2=true, mpg1=true, mpgts=true, raw=true;

	int mpgtovdr = comBox[19].getSelectedIndex();

	//DM14062004 081.7 int04 changed
	String[] convertType = { "=> demux ","=> make a VDR (A/V PES)","=> make a MPG2","=> make a PVA","=> make a TS", "=> simple packet filter" };

	for (int k=0; k<workinglist.size(); k++) {
		int ft = scan.inputInt(workinglist.get(k).toString());

		if (pva && ft==1) {
			combvideo.add(workinglist.get(k));
			vdr=false; 
			mpg2=false; 
			mpg1=false; 
			mpgts=false; 
			raw=false;
			if (k>0) {
				workinglist.remove(k);
				k--;
			} else if (mpgtovdr==0) 
				options[27] = nextFilePTS(0,0,0,0);
		} else if (vdr && ft==4) {
			combvideo.add(workinglist.get(k));
			pva=false; 
			mpg2=false; 
			mpg1=false; 
			mpgts=false; 
			raw=false;
			if (k>0) {
				workinglist.remove(k);
				k--;
			} else if (mpgtovdr==0) 
				options[27] = nextFilePTS(1,0,0,0);
		} else if (raw && (ft==5 || ft==6)) {
			combvideo.add(workinglist.get(k));
			pva=false; 
			mpg2=false; 
			mpg1=false; 
			mpgts=false; 
			vdr=false;
			if (k>0) {
				workinglist.remove(k);
				k--;
			} else if (mpgtovdr==0) 
				options[27] = nextFilePTS(1,0,0,0);
		} else if (mpg2 && ft==3) {
			combvideo.add(workinglist.get(k));
			pva=false; 
			vdr=false; 
			mpg1=false; 
			mpgts=false; 
			raw=false;
			if (k>0) {
				workinglist.remove(k);
				k--;
			} else if (mpgtovdr==0) 
				options[27] = nextFilePTS(1,2,0,0);
		} else if (mpgts && ft==11) {
			combvideo.add(workinglist.get(k));
			pva=false; 
			vdr=false; 
			mpg1=false; 
			mpg2=false; 
			raw=false;
			if (k>0) {
				workinglist.remove(k);
				k--;
			}
		} else if (mpg1 && ft==2) {
			combvideo.add(workinglist.get(k));
			pva=false; 
			vdr=false; 
			mpg2=false; 
			mpgts=false; 
			raw=false;
			if (k>0) {
				workinglist.remove(k);
				k--;
			} else if (mpgtovdr==0) 
				options[27] = nextFilePTS(1,1,0,0);
		} else 
			break;
	}


	/**
	Hashtable ht = new Hashtable();
	ht.put("0",new FileInputStream(file));
	ht.put("1",new FileInputStream(file));
	ht.put("2",new FileInputStream(file));
	SequenceInputStream in2 = new SequenceInputStream(ht.elements());
	File ff = new File(file);
	System.out.println(" "+in2.skip((ff.length()*3)-5000000));
	**/


	brm.surf.start();

	while ( options[21]>=0 ) {

		splitLabel.setText(" "+options[19]+" ");
		showOutSize();
		InfoAtEnd.clear();
		NoOfAudio=0;
		NoOfPictures=0; //DM04032004 081.6 int18 add
		NoOfTTX=0;   //DM04032004 081.6 int18 add
		options[48]+=options[42];
		options[42]=0;

		//DM20072004 081.7 int07 add
		fakedPTS = -1;


		if ( new File(vptslog).exists() ) 
			new File(vptslog).delete();

		argsloop:
		for (int h=0; h<workinglist.size(); h++)
		{
			String file = workinglist.get(h).toString();

			//DM22062004 081.7 int05 changed++
			long file_size = raw_interface.getFileSize(file);
			Msg("\r\n=> File " + h + ":  " + file + " (" + file_size + ")");

			if (file_size < 0)
			{
				Msg(" ? File not found !");
				continue argsloop;
			}
			//DM22062004 081.7 int05 changed--


			/***** scan ** 053c ***/ 
			int filetype = scan.inputInt(file);

			switch (filetype) {
			case 1: {
				Msg("=> File is PVA (Video/Audio PES)");
				if (options[31]==0) 
					Msg(convertType[mpgtovdr]);
				vptslog = pvaparse(file,0,mpgtovdr,vptslog);
				if (mpgtovdr==0) 
					splitreset(vptslog);
				break;
			}
			case 2: {
				Msg("=> File is MPEG-1 PS/SS (Video/Audio PES)");
				if (h>0) 
					pesparse(file,vptslog,1);
				else
				{   //DM06022004 081.6 int15 changed, //DM10042004 081.7 int01 changed
					if (options[31]==0) 
						Msg(convertType[mpgtovdr]);
					vptslog = vdrparse(file,1,mpgtovdr);
					if (mpgtovdr==0) 
						splitreset(vptslog);
				}
				break;
			}
			case 3: {
				Msg("=> File is MPEG-2 PS/SS (Video/Audio PES)");
				if (h>0) 
					pesparse(file,vptslog,2);
				else {
					if (options[31]==0) 
						Msg(convertType[mpgtovdr]);
					vptslog = (cBox[14].isSelected()) ? vdrparse(file,0,mpgtovdr) : vdrparse(file,2,mpgtovdr);
					if (mpgtovdr==0) 
						splitreset(vptslog);
				}
				break;
			}
			case 4: {
				Msg("=> File is Video/Audio/TTX PES");
				if (options[31]==0) 
					Msg(convertType[mpgtovdr]);
				vptslog = vdrparse(file,0,mpgtovdr);
				if (mpgtovdr==0) 
					splitreset(vptslog);
				break;
			}
			case 5: 
			case 6: {
				Msg("=> File is Audio/TTX PES");
				if (h>0) {
					splitreset(vptslog);
					pesparse(file,vptslog,0);
				} else {
					if (options[31]==0) 
						Msg(convertType[mpgtovdr]);
					vptslog = vdrparse(file,0,mpgtovdr);
					if (mpgtovdr==0) 
						splitreset(vptslog);
				}
				break;
			}
			case 10: {
				Msg("=> File is AC-3 Audio ES (psb. SMPTE)");
				splitreset(vptslog);
				rawaudio(file,vptslog,"ac");
				break;
			}
			case 7: {
				Msg("=> File is AC-3 Audio ES");
				splitreset(vptslog);
				rawaudio(file,vptslog,"ac");
				break;
			}
			case 8: {
				Msg("=> File is MPEG Audio ES");
				splitreset(vptslog);
				rawaudio(file,vptslog,"mp");
				break;
			}
			case 9: {
				Msg("=> File is MPEG Video ES");
				vptslog = rawvideo(file);
				splitreset(vptslog);
				break;
			}
			case 11: {
				Msg("=> File is DVB/MPEG-2 TS (Video/Audio/TTX PES)");
				if (options[31]==0) 
					Msg(convertType[mpgtovdr]);
				vptslog = rawparse(file,scan.getPIDs(),mpgtovdr);
				if (mpgtovdr==0) 
					splitreset(vptslog);
				break;
			}
			//DM19122003 081.6 int07 new
			case 12: {
				Msg("=> File is DTS Audio ES");
				splitreset(vptslog);
				rawaudio(file,vptslog,"ac");
				break;
			}
			case 13: {
				Msg("=> File is DTS Audio ES (psb. SMPTE)");
				splitreset(vptslog);
				rawaudio(file,vptslog,"ac");
				break;
			}
			//DM24012004 081.6 int11 add
			case 14:
			{
				Msg("=> File is RIFF PCM Audio ES");
				splitreset(vptslog);
				rawaudio(file,vptslog,"wa");
				break;
			}
			//DM02032004 081.6 int18 add
			case 16:
			{
				Msg("=> File is RLE Subpicture ES");
				splitreset(vptslog);
				rawsub(file,vptslog);
				break;
			}

			default: 
				Msg(" ? Filetype not supported !");
			}
		}

		//**** print end of splitpart
		if (options[18]>0)
			Msg("===> END OF PART "+(options[19]++));
		else 
			options[21]=-100;

		brm.surf.stop();
		Msg("");

		//***** print created files summary
		lastlist = InfoAtEnd.toArray();
		java.util.Arrays.sort(lastlist);
		Msg("summary of created media files:");
		for (int l=0;l<lastlist.length;l++)
			Msg(""+lastlist[l]);

		InfoAtEnd.clear();
		yield();

		/*** post execution ***/
		if (!qinfo && cBox[25].isSelected()) { //DM30122003 081.6 int10 changed
			String com = exefield[3+comBox[19].getSelectedIndex()].getText().toString().trim();
			if (com.length()>0 && lastlist.length>0) {
				int mn = com.lastIndexOf(" ?");
				ArrayList argList = new ArrayList();
				if (mn>-1) {
					mn = Integer.parseInt(com.substring(mn+2));
					argList.add(mn>-1 ? com.substring(0,com.lastIndexOf(" ?")) : com.trim());
					if (mn==0 || mn>lastlist.length) 
						mn = lastlist.length;
					for (int l=0;l<lastlist.length && l<mn;l++) {
						String fn = lastlist[l].toString();
						if (com.startsWith("\""))
							argList.add(""+(char)34+(fn.substring(fn.lastIndexOf("\t ")+2)).trim()+(char)34);
						else
							argList.add(""+(fn.substring(fn.lastIndexOf("\t ")+2)).trim());
					}
				} else
					argList.add(com.trim());

				String commandline="";
				String arguments[] = new String[argList.size()];
				for (int l=0;l<arguments.length;l++){
					arguments[l]=argList.get(l).toString();
					commandline+=(""+arguments[l]+(char)32);
				}

				Msg("-> post command performed: "+commandline.trim());

				try 
				{ 
				Runtime.getRuntime().exec(arguments); 
				}
				catch (IOException re) { 
					Msg(""+re); 
				}
				argList=null;
			}
		}
	}

	Msg("=> "+options[39]+" bytes written...");
	Toolkit.getDefaultToolkit().beep();
	yield();

	File mpegvideolog = new File(vptslog);
	if (mpegvideolog.exists()) 
		mpegvideolog.delete();

	if (cBox[11].isSelected()) 
		logging.close();
	if (cBox[21].isSelected()) {
		try 
		{
		PrintWriter nlf = new PrintWriter(new FileOutputStream(loggin2));
		nlf.print(messagelog);
		nlf.close();
		} 
		catch (IOException e) { 
			Msg("log error2 "+e); 
		}
	}
	messagelog="";

	/*** delete tempfiles which have multi-used ***/
	if (tempfiles.size()>0) {
		for (int si=0;si<tempfiles.size();si+=2)
			if ( new File(tempfiles.get(si).toString()).exists() ) 
				new File(tempfiles.get(si).toString()).delete();
		tempfiles.clear();
	}

	options[18]=splitsize;
}




/***************
 * split reset *
 ***************/
public void splitreset(String vptslog) {
	if ( vptslog.equals("-1") && options[18]>0 ) { 
		options[18]=0;
		Msg("! -> no videofile PTS, splitting disabled");
	}
}

//DM15072004 081.7 int06 new
private int checkPTSMatch(long video_pts_values[], long data_pts_values[])
{
	if (data_pts_values[0] < video_pts_values[0])
	{
		if (data_pts_values[data_pts_values.length - 2] < video_pts_values[0])
		{
			//maybe does match later, jump just to end
			Msg("!> 1st video PTS starts later than the last PTS of this stream");
			return (data_pts_values.length - 2);
		}
		else
		{
			//maybe does match later, jump just to this position
			int end = data_pts_values.length - 1;

			for (int i = 0; i < end; i++)
			{
				if (data_pts_values[i + 1] > video_pts_values[0])
					return i;
			}

			return 0;
		}
	}
	else
	{
		if (data_pts_values[0] >= video_pts_values[video_pts_values.length - 1])
		{
			Msg("!> last video PTS ends before the start PTS of this stream");
			return -1;
		}
		//does match anywhere, no pre-jump
		else
			return 0;
	}
}

//DM15072004 081.7 int06 new
private String infoPTSMatch(String args[], boolean video_pts, boolean data_pts)
{
	if ( !args[3].equals("-1") && !args[1].equals("-1") && !video_pts && data_pts)
		return "? ";

	else
		return "";
}



/**************
 * PES Parser *
 *************/
public void pesparse(String file, String vptslog, int ismpg) {

	String fchild = (newOutName.equals("")) ? (new File(file).getName()).toString() : newOutName;
	String fparent = (!RButton[8].isSelected() && fchild.lastIndexOf(".") != -1 ) ? workouts+fchild.substring(0,fchild.lastIndexOf(".")) : workouts+fchild; //DM30122003 081.6 int10 changed

	/*** split part ***/
	fparent += (options[18]>0) ? "("+options[19]+")" : "" ;

	String paname = fparent+".ma1";

	if ( tempfiles.size()>0 ) {
		for (int tfs=0; tfs<tempfiles.size(); tfs+=4 ) {
			if ( tempfiles.get(tfs+1).toString().equals(file) ) {

				Common.renameTo(tempfiles.get(tfs).toString(), paname); //DM13042004 081.7 int01 changed
				//new File(tempfiles.get(tfs).toString()).renameTo(new File(paname));

				tempfiles.set(tfs,paname);
				String[] tfpes = { tempfiles.get(tfs).toString(), tempfiles.get(tfs+2).toString(), tempfiles.get(tfs+3).toString(), vptslog };
				Msg("=> continue while using extracted raw data from "+file);
				if (tfpes[2].equals("tt")) 
					processTeletext(tfpes);
				else 
					mpt(tfpes);
				return;
			}
		}
	}

	int type, packlength, ptslength, pesID, subID;
	boolean ttx = false;
	clv = new int[10];
	long count=0;

	PESdemuxlist.clear();
	PIDdemux demux = new PIDdemux();

	try 
	{

	//long size = (new File(file)).length();

	//DM18062004 081.7 int05 changed
	PushbackInputStream in = raw_interface.getStream(file, bs);
	long size = raw_interface.getStreamSize();

	progress.setString("demuxing PES file  "+(new File(file)).getName());
	progress.setStringPainted(true);
	progress.setValue(0);
	yield();

	//PushbackInputStream in = new PushbackInputStream(new FileInputStream(file),bs);

	byte[] push6 = new byte[6];
	byte[] data = new byte[1];

	options[5]=262143;
	options[6]=0;
	options[7]=0;
	options[8]=-10000;
	options[12]=200;
	options[13]=1;
	options[41]=0;
	options[50]=0;
	long qexit = count+options[56];
	boolean miss=false; //DM03112003 081.5++ info

	morepva:
	while (true) {

		// start loop fileread
		pvaloop:
		while ( count < size )  {     // till fileend (64bit size)

			progress.setValue((int)(count*100/size)+1);
			yield();

			while (qpause) 
				pause();

			if (qbreak || (qinfo && count>qexit)) {
				qbreak=false; 
				break morepva; 
			}

			in.read(push6,0,6);

			/*** check 0x000001  ***/
			if ( push6[0]!=0 || push6[1]!=0 || push6[2]!=1 || (0xFF&push6[3])<0xB9) { //DM06012004 081.6 int11 changed
				in.unread(push6,1,5);
				if (!cBox[3].isSelected() && !miss) //DM03112003 081.5++ info
					Msg("!> missing startcode @ "+count);
				miss=true;
				count++;
				continue pvaloop;
			}

			if (!cBox[3].isSelected() && miss) //DM03112003 081.5++ info, moved
				Msg("!> found startcode @ "+count);
			miss=false;

			if (ismpg>0 || cBox[14].isSelected()) { //DM22122003 081.6 int08 changed
				switch (0xFF&push6[3]) {
				case 0xB9: {  //DM02112003
					in.unread(push6,4,2);
					count+=4;
					continue pvaloop;
				}
				case 0xBA: {  //DM17022004 081.6 int17 changed
					int sOff = (0xC0 & push6[4])!=0 ? 2 : 0;
					count += 6 + sOff;
					if (sOff==0)
						sOff = 6;
					else
					{
						in.skip(sOff);
						in.read(push6,0,6);
						count += 6;
						sOff = 0x7 & push6[5];
					}
					in.skip(sOff);
					count += sOff;
					continue pvaloop;
				}
				case 0xBB:
				case 0xBC: //DM02112003+ 081.5++
				case 0xBE:
				case 0xBF:
				case 0xF0:
				case 0xF1:
				case 0xF2:
				case 0xF3:
				case 0xF4:
				case 0xF5:
				case 0xF6:
				case 0xF7:
				case 0xF8:
				case 0xF9:
				case 0xFA:
				case 0xFB:
				case 0xFC:
				case 0xFD:
				case 0xFE: //DM02112003- 081.5++
				case 0xFF: {
					packlength = ((255&push6[4])<<8 | (255&push6[5]));
					in.skip(packlength);
					count+=6+packlength;
					continue pvaloop;
				}
				}
			}

			if ( (0xf0&push6[3])!=0xe0 && (0xe0&push6[3])!=0xc0 && (255&push6[3])!=0xbd ) {
				in.unread(push6,1,5);
				count++;
				continue pvaloop;
			}

			type = 255&push6[3];      // 0xe0 is Video1, 0xc0 is Audio1, 0xbd is ac3
			pesID = 255&push6[3];     // 0xe0 is Video1, 0xc0 is Audio1, 0xbd is ac3
			packlength = ((255 & push6[4])<<8 | (255 & push6[5]));    // av pack length bytes to follow

			in.unread(push6,0,6);

			if (packlength==0) { 
				Msg("-> ! packet length is 0 @ "+count);
				count+=6;
				in.skip(6);
				continue pvaloop;
			}

			data = new byte[6+packlength+6];
			in.read(data,0,6+packlength+6);

			if (cBox[33].isSelected() && (data[6+packlength]!=0 || data[7+packlength]!=0 || data[8+packlength]!=1)) {
				if (count+6+packlength < size) {
					if (!cBox[3].isSelected() && !miss) //DM21112003 081.5++ info
						Msg("!> missing next startcode @ "+(count+6+packlength)+" from "+count+" (PES-ID 0x"+Integer.toHexString(pesID).toUpperCase()+"), dropping packet..");
					miss=true;
					in.unread(data,1,data.length-1); 
					count++;
					continue pvaloop;
				}
			} else 
				in.unread(data,6+packlength,6);

			clv[5]++;
			if (options[30]==1) 
				System.out.print("\rpacks: "+clv[5]+" "+((count*100/size))+"% "+(count));

			count += 6+packlength;

			ttx = false;
			subID = 0;
			if (pesID==0xBD) { //DM30122003 081.6 changed
				int off = 9+(0xFF&data[8]);
				if ( off < 6+packlength ) {
					subID = 0xFF&data[off];
					ttx = ((0xFF&data[8])==0x24 && subID>>>4==1) ? true : false; 
					if (ismpg==0 && !ttx) 
						subID=0;
				} else if (ismpg!=1) 
					data[8]=(byte)(packlength-3);
			} 

			int idcheck=-1;
			for (int a=0;a<PESdemuxlist.size();a++) {      // find ID object
				demux = (PIDdemux)PESdemuxlist.get(a);
				if (pesID==demux.getID() && subID==demux.subID() && ttx==demux.isTTX()) {
					idcheck=a; 
					break; 
				}
			}

			if (idcheck==-1) {   // create new ID object
				String IDtype="";
				switch (0xF0 & pesID) {
				case 0xE0: { 
					IDtype="(MPEG Video) -> ignored";
					demux = new PIDdemux();
					demux.setID(pesID);
					demux.setsubID(0);
					demux.setType(3);
					PESdemuxlist.add(demux);
					break; 
				}
				case 0xC0:
				case 0xD0: { 
					IDtype="(MPEG Audio)"; 
					demux = new PIDdemux();
					demux.setID(pesID);
					demux.setsubID(0);
					demux.setStreamType(ismpg);
					PESdemuxlist.add(demux);
					demux.init(fparent,options,bs/PESdemuxlist.size(),PESdemuxlist.size(),0);
					break; 
				}
				}
				switch (pesID) {
				case 0xBD: { //DM30122003 081.6 int10 changed
					IDtype="(private stream 1)";
					IDtype+=(ttx?" TTX ":"")+(subID!=0?" (SubID 0x"+Integer.toHexString(subID).toUpperCase()+")":""); 
					demux = new PIDdemux();
					demux.setID(pesID);
					demux.setsubID(subID);
					demux.setTTX(ttx);    // sets the type, too  (0=ac3,dts,4 lpcm,5 subpic, 1=ttx)
					demux.setStreamType(ismpg);
					PESdemuxlist.add(demux);
					demux.init(fparent,options,bs/PESdemuxlist.size(),PESdemuxlist.size(),0);
					break; 
				}
				}
				Msg("-> found PES-ID 0x"+Integer.toHexString(pesID).toUpperCase()+" "+IDtype+" @ "+(count-6-packlength)); //DM02022004 081.6 int14 changed
			}


			//DM14072004 081.7 int06 add
			if (!demux.StreamEnabled())
			{
				continue pvaloop;
			}

			if (demux.getType()==3) 
				continue pvaloop;
			else 
				demux.write(data,true);


		}  // end while pvaloop

		break morepva;

	} // end while more than 1 pva -> morepva

	Msg("packs: "+clv[5]+" "+((count*100/size))+"% "+count);


	in.close(); 
	yield();

	//DM23022004 081.6 int18 changed
	int[] lfn = new int[6];

	for (int a=0;a<PESdemuxlist.size();a++) {
		demux = (PIDdemux)PESdemuxlist.get(a);
		if (demux.getType()==3) 
			continue;

		String[] values = demux.close(vptslog);
		if (values[0].equals("")) 
			continue;

		String newfile = values[3]+ ((lfn[demux.getType()]>0) ? ("_"+lfn[demux.getType()]) : "") + "." + values[2];

		Common.renameTo(values[0], newfile); //DM13042004 081.7 int01 changed
		//new File(values[0]).renameTo(new File(newfile));

		values[0] = newfile;
		values[3] = vptslog;
		switch (demux.getType())
		{
		case 0:
			if ( demux.subID()!=0 && (0xF0&demux.subID())!=0x80 ) 
				break;
			Msg("");
			Msg("--> AC-3/DTS Audio "+((demux.subID()!=0) ? ("(SubID 0x"+Integer.toHexString(demux.subID()).toUpperCase()+")") : "")); //DM19122003 081.6 int07 changed
			mpt(values);
			break;
		case 1: //DM30122003 081.6 int10 changed
			Msg("");
			Msg("--> Teletext (SubID 0x"+Integer.toHexString(demux.subID()).toUpperCase()+")");
			processTeletext(values);
			break;
		case 2:
			Msg("");
			Msg("--> MPEG Audio (0x"+Integer.toHexString(demux.getID()).toUpperCase()+")");
			mpt(values);
			break;
		case 4: //DM23022004 081.6 int18 add
			Msg("");
			Msg("--> LPCM Audio (SubID 0x"+Integer.toHexString(demux.subID()).toUpperCase()+")");
			processLPCM(values);
			break;
		case 5: //DM23022004 081.6 int18 add, //DM12042004 081.7 int01 changed
			Msg("");
			Msg("--> Subpicture (SubID 0x"+Integer.toHexString(demux.subID()).toUpperCase()+")");
			processSubpicture(values);
			break;
		}

		lfn[demux.getType()]++;
		tempfiles.add(values[0]);
		tempfiles.add(file);
		tempfiles.add(values[1]);
		tempfiles.add(values[2]);

		if (options[18]==0) {
			new File(newfile).delete();
			new File(values[1]).delete();
		}
	}

	// PESdemuxlist.clear();

	}
	catch (EOFException e1) { 
		//DM25072004 081.7 int07 add
		Msg("pesparse EOF reached in error: " + e1); 
	}
	catch (IOException e2) { 
		//DM25072004 081.7 int07 add
		Msg("pesparse File I/O error: " + e2); 
	}

	System.gc();

}  /** end methode pes parse **/




/**************
 * VDR Parser *
 *************/
public String vdrparse(String file, int ismpg, int ToVDR) {

	String fchild = (newOutName.equals("")) ? (new File(file).getName()).toString() : newOutName;
	String fparent = ( fchild.lastIndexOf(".") != -1 ) ? workouts+fchild.substring(0,fchild.lastIndexOf(".")) : workouts+fchild;

	/*** split part ***/
	fparent += (options[18]>0) ? "("+options[19]+")" : "" ;

	int type, packlength=0, pesID, subID;
	boolean ttx=false, ende=false;
	clv = new int[10];
	long ptsoffset=0, lastpts=0;
	String vptslog="-1";

	int pesID0 = ((0xffL & options[43])!=0) ? (0xff & (int)options[43]) : 0;

	/*** d2v project ***/
	if (cBox[29].isSelected() || cBox[30].isSelected()) {
		String[] d2vopt = new String[d2vfield.length];
		for (int x=0;x<d2vfield.length;x++) 
			d2vopt[x] = d2vfield[x].getText();
		d2v.setOptions(d2vopt);
		d2v.Init(fparent);
	}

	String[][] mt = { { "(vdr)","","" },{ "","(mpg)","(mpg)" } };
	int[] newID = { 0xE0, 0xC0, 0x80, 0x90, 0xA0, 0x20 }; //DM22022004 081.6 int18 changed

	PIDdemux demux = new PIDdemux();

	if (options[19]==0) 
		VDRdemuxlist.clear();
	else {
		for (int a=0;a<VDRdemuxlist.size();a++) {
			demux = (PIDdemux)VDRdemuxlist.get(a);

			//DM04032004 081.6 int18 fix
			if (demux.getnewID()!=0)
				newID[demux.getType()]++;

			if (demux.getNum()==-1) 
				continue;
			if (demux.getType()==3) { 
				demux.initVideo2(fparent,options);
				if (pesID0==0) 
					pesID0 = demux.getID();
			} else 
				demux.init2(fparent,options);
		}
	}

	//DM18022004 081.6 int17 changed
	makeVDR makevdr = new makeVDR();
	if (ToVDR==1) 
		makevdr.init(fparent+((options[18]==0)?mt[0][ismpg]:"")+".vdr",options,bs,ToVDR,(int)options[19]);
	else if (ToVDR==2) 
		makevdr.init(fparent+((options[18]==0)?mt[1][ismpg]:"")+".m2p",options,bs,ToVDR,(int)options[19]);
	else if (ToVDR==3) 
		makevdr.init(fparent+".pva",options,bs,ToVDR,(int)options[19]);
	else if (ToVDR==4) 
		makevdr.init(fparent+".ts",options,bs,ToVDR,(int)options[19]);

	//DM14062004 081.7 int04 add
	else if (ToVDR==5) 
		makevdr.init(fparent + "[filtered].pes", options, bs, ToVDR, (int)options[19]);

	try 
	{

	byte[] push6 = new byte[6];
	byte[] data = new byte[1];
	boolean seqhead = false;

	options[5]=262143;
	options[6]=0;
	options[7]=0;
	options[8]=-10000;
	options[12]=(ismpg==1)?10:200;
	options[13]=1;
	options[41]=0;
	options[50]=0;
	boolean writevideo=true;
	int du=0;
	CUT_BYTEPOSITION=0;
	long count=0, size=0, startPoint=0;
	long starts[] = new long[combvideo.size()];

	for (int a=0; a<combvideo.size(); a++){   // maybe a < combideo.size()
		file = combvideo.get(a).toString();
		starts[a] = size;
		size += new File(file).length();
	}

	file = combvideo.get(FileNumber).toString();
	count = starts[FileNumber];
	size = count + new File(file).length();

	//** pid inclusion 
	ArrayList abc = (ArrayList)speciallist.get(currentcoll);
	int[] include = new int[abc.size()];
	for (int a=0; a<abc.size(); a++) 
		include[a] = 0xFF&Integer.parseInt(abc.get(a).toString().substring(2),16);
	if (include.length>0)
		Msg("-> special PES-IDs for searching defined");

	//*** split skipping first
	if (options[18]>0)
		startPoint = options[20]-(comBox[25].getSelectedIndex()*1048576L);

	//*** jump near to first cut-in point to collect more audio
	if (comBox[17].getSelectedIndex()==0 && ctemp.size()>0 && cutcount==0 && (!RButton[6].isSelected() || ToVDR>0)) //DM28112003 081.5++
		startPoint = Long.parseLong(ctemp.get(cutcount).toString())-((ToVDR==0)?2048000:0);

	if (startPoint < 0)
		startPoint = count;  // =0
	else if (startPoint < count){
		for (int a=starts.length; a>0; a--)
			if (starts[a-1] > startPoint)
				FileNumber--;
	}
	else if (startPoint > count){
		for (int a=FileNumber+1; a < starts.length; a++){  // (fix2)
			if (starts[a] > startPoint)
				break;
			else 
				FileNumber++;
		}
	}

	file = combvideo.get(FileNumber).toString();
	count = starts[FileNumber];
	//size = count + new File(file).length();

	if (FileNumber>0)
		Msg("continue with file: "+file);

	//PushbackInputStream in = new PushbackInputStream(new FileInputStream(file),bs);

	//DM18062004 081.7 int05 changed
	PushbackInputStream in = raw_interface.getStream(file, bs);
	size = count + raw_interface.getStreamSize();
	long base = count;

	while (count < startPoint) {
		count += in.skip(startPoint-count);
	}

	progress.setString(((ToVDR==0)?"demuxing":"converting")+" A/V PES file  "+(new File(file)).getName());
	progress.setStringPainted(true);
	progress.setValue((int)((count-base)*100/(size-base))+1);
	yield();

	long qexit = count+options[56];
	boolean miss=false; //DM03112003 081.5++ info
	int packsize0_buffer=Integer.parseInt(comBox[36].getSelectedItem().toString()); //DM21112003 081.5++
	packsize0_buffer=packsize0_buffer>bs?bs:packsize0_buffer; //DM21112003 081.5++

	morepva:
	while (true) {

		// start loop fileread
		pvaloop:
		while ( count < size )  {     // till fileend (64bit size)

			while (qpause) 
				pause();
			if (qbreak || (qinfo && count>qexit)) { 
				qbreak=false; 
				break morepva; 
			}

			/*** cut end reached ***/
			if ((int)options[37]+20 < origframes) {
				ende=true;
				break morepva; 

			}
			if (comBox[17].getSelectedIndex()==0 && ctemp.size()>0) {
				if (cutcount==ctemp.size() && (cutcount&1)==0)
					if (count > Long.parseLong(ctemp.get(cutcount-1).toString())+((ToVDR==0)?2048000:64000)){
						ende=true;
						break morepva;
					}
			}

			in.read(push6,0,6);

			/*** check 0x000001  ***/
			if ( push6[0]!=0 || push6[1]!=0 || push6[2]!=1 || (0xFF&push6[3])<0xB9) { //DM06012004 081.6 int11 changed
				in.unread(push6,1,5);
				if (!cBox[3].isSelected() && !miss) //DM03112003 081.5++ info
					Msg("!> missing startcode @ "+count);
				miss=true;
				count++;
				continue pvaloop;
			}

			if (!cBox[3].isSelected() && miss) //DM04112003 081.5++ info, moved 
				Msg("!> found startcode @ "+count);
			miss=false;

			if (ismpg>0 || cBox[14].isSelected()) { //DM22122003 081.6 int08 changed
				switch (0xFF&push6[3]) {
				case 0xB9: {  //DM02112003
					in.unread(push6,4,2);
					count+=4;
					continue pvaloop;
				}
				case 0xBA: {  //DM17022004 081.6 int17 changed
					int sOff = (0xC0 & push6[4])!=0 ? 2 : 0;
					count += 6 + sOff;
					if (sOff==0)
						sOff = 6;
					else
					{
						in.skip(sOff);
						in.read(push6,0,6);
						count += 6;
						sOff = 0x7 & push6[5];
					}
					in.skip(sOff);
					count += sOff;
					continue pvaloop;
				}
				case 0xBF:  //split cellids
					//DM09112003+ 081.5++
					packlength = ((0xFF&push6[4])<<8 | (0xFF&push6[5]));
					data = new byte[packlength];
					in.read(data,0,packlength);
					if (data[0]==1){
						int cellid = 0xFF&data[0x1C];
						int vobid = (0xFF&data[0x19])<<8 | (0xFF&data[0x1A]);
						if ((0xFF&options[52])!=cellid || (0xFFFF&options[52]>>>16)!=vobid){
							Msg("-> VobID "+vobid+ " CellID "+cellid+" @ "+count+" (GOP#"+clv[6]+" / Frame#"+options[7]+")"); //DM30122003 081.6 int10 changed
							/**
							if (options[18]>0 && options[52]!=0){
								options[52]=cellid|vobid<<16;
								options[20]=count;
								count += 6+packlength;
								break morepva;
							}
							**/
						}
						options[52]=cellid|vobid<<16;
					}
					count += 6+packlength;
					continue pvaloop;
				case 0xBB:
				case 0xBC: //DM02112003+ 081.5++
				case 0xBE:
				case 0xF0:
				case 0xF1:
				case 0xF2:
				case 0xF3:
				case 0xF4:
				case 0xF5:
				case 0xF6:
				case 0xF7:
				case 0xF8:
				case 0xF9:
				case 0xFA:
				case 0xFB:
				case 0xFC:
				case 0xFD:
				case 0xFE: //DM02112003- 081.5++
				case 0xFF: {
					packlength = ((255&push6[4])<<8 | (255&push6[5]));
					in.skip(packlength);
					count += 6+packlength;
					continue pvaloop;
				}
				}
			}

			if ( (0xf0&push6[3])!=0xe0 && (0xe0&push6[3])!=0xc0 && (255&push6[3])!=0xbd ) {
				in.unread(push6,1,5);
				count++;
				continue pvaloop;
			}

			/**** mark for split at sequenceheader ***/
			options[20]=count;
			CUT_BYTEPOSITION=count;

			progress.setValue((int)((count-base)*100/(size-base))+1);
			yield();

			pesID = 255&push6[3];
			packlength = ((255 & push6[4])<<8 | (255 & push6[5]));

			in.unread(push6);

			boolean packlen0=false;
			if (packlength==0) { //DM04122003 081.6_int02 changed
				packlen0=true;
				if (options[30]==1) 
					System.out.println("-> ! packet length is 0 @ "+count);
      
				byte[] data2 = new byte[packsize0_buffer];
				in.read(data2,0,packsize0_buffer);
				boolean nextpack=false;
     
				int a=9;
				int b=packsize0_buffer-6;
				nps:
				for (; a<b; a++)
				{
					//DM04062004 081.7 int04 add
					if (data2[a + 2] != 1)
					{
						if (data2[a + 2] != 0)
							a += 2;

						else if (data2[a + 1] != 0)
							a++;

						continue nps;
					}
					else if (data2[a + 1] != 0)
					{
						a++;
						continue nps;
					}
					else if (data2[a] != 0) 
					{
						continue nps;
					}

					//if (data2[a]!=0 || data2[a+1]!=0 || data2[a+2]!=1) 
					//	continue nps;

					if ((0xff&data2[a+3])>0xB8){
						nextpack=true;
						break nps;
					}
				}
				if (nextpack) {
					data = new byte[a+6];
					System.arraycopy(data2,0,data,0,a+6);
					packlength = a-6;
					in.unread(data2,a+6,(data2.length-a-6));  // unread till next head
				} else {
					in.unread(data2,a,(data2.length-a));  // unread for next head
					Msg("!> missing next startcode (packetsize=0) @ "+(count+a)+" from "+count+" /I-Buffer "+packsize0_buffer);
					data2=null;
					count+=a;
					continue pvaloop;
				}
				data2=null;

			} else {
				data = new byte[6+packlength+6];
				in.read(data,0,6+packlength+6);
			}

			if (cBox[33].isSelected() && (data[6+packlength]!=0 || data[7+packlength]!=0 || data[8+packlength]!=1)) {
				if (count+6+packlength < size) {
					if (!cBox[3].isSelected() && !miss) //DM21112003 081.5++ info
						Msg("!> missing next startcode @ "+(count+6+packlength)+" from "+count+" (PES-ID 0x"+Integer.toHexString(pesID).toUpperCase()+"), dropping packet..");
					miss=true;
					in.unread(data,1,data.length-1); 
					count++;
					continue pvaloop;
				}
			} else 
				in.unread(data,6+packlength,6);

			clv[5]++;
			if (options[30]==1) 
				System.out.print("\rpacks: "+pesID+"/"+clv[5]+"/"+(data.length-6)+"/"+((count*100/size))+"% "+(count));

			count += 6+packlength;

			ttx = false;
			subID = 0;
			if (pesID==0xBD && packlength>2) { //DM30122003 081.6 int10 changed
				int off = 9+(0xFF&data[8]);
				if ( off < 6+packlength ) {
					subID = 0xFF&data[off];
					ttx = ((0xFF&data[8])==0x24 && subID>>>4==1) ? true : false; 
					if (ismpg==0 && !ttx) 
						subID=0;
				} else if (ismpg!=1) 
					data[8]=(byte)(packlength-3);
			}

			/** pid inclusion **/
			includeloop:
			while (include.length>0) {
				for (int v=0; v<include.length; v++) 
					if (pesID==include[v] || subID==include[v]) 
						break includeloop;
				continue pvaloop;
			}

			//DM14062004 081.7 int04 add
			if (ToVDR == 5)
			{
				makevdr.writePacket(data, 0, 6 + packlength);
				continue pvaloop;
			}

			int idcheck=-1;
			for (int a=0;a<VDRdemuxlist.size();a++) {      // find ID object
				demux = (PIDdemux)VDRdemuxlist.get(a);

				if (pesID==demux.getID() && subID==demux.subID() && (ttx==demux.isTTX())) {
					idcheck=a; 
					break; 
				}
			}

			if (idcheck==-1) {   // create new ID object

				/*** dump startpacket ***/
				if (cBox[43].isSelected()) {
					String dumpname = fparent+"("+Integer.toHexString(pesID)+"-"+Integer.toHexString(subID)+"#"+(du++)+"@"+(count-6-packlength)+").bin";
					FileOutputStream dump = new FileOutputStream(dumpname);
					dump.write(data); 
					dump.flush(); 
					dump.close();
					Msg("-> dump 1st packet to file: "+dumpname);
				}

				String IDtype="";
				switch (0xF0 & pesID) {
				case 0xE0: { 
					IDtype="(MPEG Video)";
					demux = new PIDdemux();
					demux.setID(pesID);
					demux.setnewID(newID[0]++);
					demux.setsubID(0);
					demux.setStreamType(ismpg);
					demux.setType(3);
					VDRdemuxlist.add(demux);
					if (pesID0==0 || pesID0==pesID) { 
						if (ToVDR==0) 
							demux.initVideo(fparent,options,bs,VDRdemuxlist.size(),1);

						//DM09072004 081.7 int06 changed
						if (ToVDR > 0) 
							IDtype += " mapped to 0x" + Integer.toHexString(newID[0] - 1).toUpperCase();

						pesID0=pesID;
					} else 
						IDtype+=" -> ignored";
					break; 
				}
				case 0xC0:
				case 0xD0: { 
					IDtype="(MPEG Audio)"; 
					demux = new PIDdemux();
					demux.setID(pesID);
					demux.setnewID(newID[1]++);
					demux.setsubID(0);
					demux.setType(2);
					demux.setStreamType(ismpg);
					VDRdemuxlist.add(demux);
					if (ToVDR==0) 
						demux.init(fparent,options,bs/VDRdemuxlist.size(),VDRdemuxlist.size(),1);

					//DM09072004 081.7 int06 changed
					if (ToVDR > 0) 
						IDtype += " mapped to 0x" + Integer.toHexString(newID[1] - 1).toUpperCase();

					break; 
				}
				}
				switch (pesID) {
				case 0xBD: { //DM30122003 081.6 int10 changed
					IDtype="(private stream 1)";
					IDtype+=(ttx?" TTX ":"")+(subID!=0?" (SubID 0x"+Integer.toHexString(subID).toUpperCase()+")":""); 
					demux = new PIDdemux();
					demux.setID(pesID);
					demux.setsubID(subID);
					demux.setTTX(ttx);    // sets the type, too  (false=ac3,dts,lpcm,subpic, true=ttx)

					//DM23022004 081.6 int18 changed
					if (ttx)
						demux.setnewID(newID[3]++);
					else
					{
						switch(subID>>>4)
						{
							case 0:
							case 8:
								demux.setnewID(newID[2]++);
								break;
							case 0xA:
								demux.setnewID(newID[4]++);
								break;
							case 2:
							case 3:
								demux.setnewID(newID[5]++);
						}
					}

					demux.setStreamType(ismpg);
					VDRdemuxlist.add(demux);

					if (ToVDR==0)
					{
						if (ttx) 
							demux.init(fparent,options,bs/VDRdemuxlist.size(),VDRdemuxlist.size(),1);
						else if (subID==0 || subID>>>4 == 8) //DM23022004 081.6 int18 changed
							demux.init(fparent,options,bs/VDRdemuxlist.size(),VDRdemuxlist.size(),1);
						else if (subID>>>4 == 0xA || subID>>>4 == 2 || subID>>>4 == 3) //DM23022004 081.6 int18 new
							demux.init(fparent,options,bs/VDRdemuxlist.size(),VDRdemuxlist.size(),1);
						else 
							IDtype+=" -> ignored";
					}

					//DM09072004 081.7 int06 changed
					if (ToVDR > 0)
					{
						if (ismpg==0) 
							IDtype+=" mapped to 0x"+Integer.toHexString(demux.getnewID()).toUpperCase();
						else if (ttx || subID>>>4 == 8) //DM23022004 081.6 int18 changed
							IDtype+=" mapped to 0x"+Integer.toHexString(demux.getnewID()).toUpperCase();
						else
						{ 
							IDtype+=" -> ignored"; 
							demux.setType(4); //ändern
						}
					}
					if (ToVDR==1)
					{
						if (ismpg > 0 && subID>>>4 != 8)
						{ 
							IDtype+=" -> ignored"; 
							demux.setType(4); //ändern
						}
					}
					break; 
				}
				}
				Msg("-> found PES-ID 0x"+Integer.toHexString(pesID).toUpperCase()+" "+IDtype+" @ "+options[20]); //DM02022004 081.6 int14 changed

			}


			//DM14072004 081.7 int06 add
			if (!demux.StreamEnabled())
			{
				continue pvaloop;
			}

			//DM04122003 081.6_int02
			byte nPes[];
			byte StdHeader[] = { 0,0,1,0,0,0,(byte)0x80,0,0 };
			StdHeader[3]=data[3];
			int StdSize = 0x1800;
			int PesLength=6+packlength, rPesLength=0, nPesLength=0, nPackLength=0;

			export:
			for (int a=0;;) {
				rPesLength=6+packlength-a;
				if (packlen0){
					if (a==0){
						nPesLength=(rPesLength>=StdSize)?StdSize:rPesLength;
						nPes=new byte[nPesLength+6];
						System.arraycopy(data,0,nPes,0,nPesLength);
						nPackLength=nPesLength-6;
					}else{
						nPesLength=(rPesLength-9<=StdSize)?rPesLength:StdSize-9;
						nPes=new byte[9+nPesLength+6];
						System.arraycopy(StdHeader,0,nPes,0,9);
						System.arraycopy(data,a,nPes,9,nPesLength);
						nPackLength=nPesLength+3;
					}
					nPes[4]=(byte)(0xFF&(nPackLength>>>8));
					nPes[5]=(byte)(0xFF&nPackLength);
					a+=nPesLength;

				}else{

					//DM04022004 081.6 int14 add
					if (ismpg==1 && ToVDR>0)
					{
						int shift = 0, ptslen = 0;
						byte ptsbytes[] = new byte[0];

						skiploop:
						while(true)
						{
							switch (0xC0 & data[6+shift])
							{
							case 0x40:
								shift+=2; 
								continue skiploop; 
							case 0x80:
								shift+=3; 
								continue skiploop; 
							case 0xC0:
								shift++;  
								continue skiploop; 
							case 0:
								break;
							}

							switch (0x30 & data[6+shift])
							{
							case 0x20:  //pts only
								ptslen = 5;  
								ptsbytes = new byte[ptslen];
								System.arraycopy(data,6+shift,ptsbytes,0,ptslen);
								StdHeader[7] = (byte)0x80;
								StdHeader[8] = (byte)ptslen;
								shift+=5;
								break skiploop; 
							case 0x30:  //pts+dts
								ptslen = 10; 
								ptsbytes = new byte[ptslen];
								System.arraycopy(data,6+shift,ptsbytes,0,ptslen);
								StdHeader[7] = (byte)0xC0;
								StdHeader[8] = (byte)ptslen;
								shift+=10;
								break skiploop; 
							case 0x10:  //dts only unused
								shift+=5; 
								continue skiploop; 
							case 0:
								shift++; 
								break skiploop; 
							}
						}

						nPackLength = packlength-shift;
						nPesLength = 9+ptsbytes.length+nPackLength;
						nPes = new byte[nPesLength+6];
						System.arraycopy(StdHeader,0,nPes,0,9);


						nPes[3] = data[3];
						System.arraycopy(ptsbytes,0,nPes,9,ptslen);
						System.arraycopy(data,6+shift,nPes,9+ptsbytes.length,nPackLength);
						nPackLength += 3 + ptsbytes.length;
						nPes[4]=(byte)(0xFF&(nPackLength>>>8));
						nPes[5]=(byte)(0xFF&nPackLength);

						a+=PesLength;
					}
					else
					{   //normal
						nPes=data;
						a+=PesLength;
					}
				}

				//DM12042004 081.7 int01 add
				if ((ismpg != 1 || ToVDR > 0) && nPes.length - 6 <= 9 + (0xFF & nPes[8]))
				{
					Msg("!> PES-ID 0x"+Integer.toHexString(pesID).toUpperCase()+", incoming PES packet without payload or wrong header data @ "+options[20]);
					break export;
				}

				if (ToVDR==1) 
					options = makevdr.writeVDR(nPes,options,demux,6);
				else if (ToVDR==2) 
					options = makevdr.writeMPG(nPes,options,demux,6);
				else if (ToVDR==3) 
					options = makevdr.writePVA(nPes,options,demux,6);
				else if (ToVDR==4) 
					options = makevdr.writeTS(nPes,options,demux,6);

				//DM14062004 081.7 int04 changed
				else if (ToVDR == 0)
				{
					if (demux.getType()==3) { 
						if (pesID0!=demux.getID()) 
							continue pvaloop;
						demux.writeVideo(nPes,options,false);
					} else 
						demux.write(nPes,true);
					if (demux.getPTS()>lastpts) 
						lastpts=demux.getPTS();
				}

				if (a>=PesLength)
					break export;
			}

			showOutSize();
			if (ToVDR>0)  //DM06022004 081.6 int15 fix
				options[20] = count;

			/****** split size reached *****/
			if ( options[18]>0 && options[18]<options[41] ) 
				break pvaloop;

		}  // end pvaloop

		if ( options[18]>0 && options[18]<options[41] )
			break morepva;

		/*** more files ***/
		if (FileNumber < combvideo.size()-1) { 
			in.close();
			System.gc();

			if (cBox[49].isSelected() && ToVDR==0) {
				ptsoffset = nextFilePTS(1,ismpg,lastpts,FileNumber+1);
				if (ptsoffset==-1) 
					ptsoffset=0; 
				else {
					for (int a=0;a<VDRdemuxlist.size();a++) {
						demux = (PIDdemux)VDRdemuxlist.get(a);
						demux.PTSOffset(ptsoffset);
						if (demux.getID()==pesID0) 
							demux.resetVideo();
					}
					options[13]=1;
					newvideo=true;  // global boolean
					cell.add(""+(options[7]));
				}
			}

			String nextfile = combvideo.get(++FileNumber).toString();
			count = size;
			base = count;

			//size += new File(nextfile).length();
			//in = new PushbackInputStream(new FileInputStream(nextfile),bs);

			//DM18062004 081.7 int05 changed
			in = raw_interface.getStream(nextfile, bs);
			size += raw_interface.getStreamSize();


			Msg("-> actual written vframes: "+options[7]);
			Msg("switch to file: "+nextfile);
			progress.setString(((ToVDR==0)?"demuxing":"converting")+" A/V PES file  "+new File(nextfile).getName());
			progress.setStringPainted(true);

		} else 
			break morepva;

	} // end while more than 1 pva -> morepva


	/****** file end reached for split *****/
	if ( (count>=size || ende) && options[18]>0 ) 
		options[21]=-100;

	in.close(); 

	if (ToVDR>0) 
		makevdr.close();
	else {
		int aa=0;
		for (int a=0;a<VDRdemuxlist.size();a++) {
			demux = (PIDdemux)VDRdemuxlist.get(a);
			if (demux.getType()==3) { 
				if (aa>0) 
					continue;

				/*** d2v project ***/
				if ((cBox[29].isSelected() || cBox[30].isSelected()) ) 
					d2v.write(options[50],options[7]);

				//DM12042004 081.7 int01 changed
				Msg("");
				Msg("-> video: fr/ ct/ 1p/ cg/ og/ dg = " + options[7] + "/ " + clv[0] + "/ " + clv[1] + "/ " + clv[2] + "/ " + clv[3] + "/ " + clv[4]);

				vptslog = demux.closeVideo();
				aa++;
			}
		} 
		System.gc();

		//DM23022004 081.6 int18 changed
		int[] lfn = new int[6]; 
		for (int a=0;a<VDRdemuxlist.size();a++) {
			demux = (PIDdemux)VDRdemuxlist.get(a);
			if (demux.getType()==3) 
				continue;

			//if (ismpg>0 && demux.getID()==0xBD && demux.subID()>>>4 != 8)
			//	continue;

			String[] values = demux.close(vptslog);
			if (values[0].equals("")) 
				continue;

			String newfile = values[3]+ ((lfn[demux.getType()]>0) ? ("_"+lfn[demux.getType()]) : "") + "." + values[2];

			Common.renameTo(values[0], newfile); //DM13042004 081.7 int01 changed
			//new File(values[0]).renameTo(new File(newfile));

			values[0] = newfile;
			values[3] = vptslog;
			switch (demux.getType())
			{
			case 0:
				if ( demux.subID()!=0 && (0xF0&demux.subID())!=0x80 ) 
					break;
				Msg("");
				Msg("--> AC-3/DTS Audio "+((demux.subID()!=0) ? ("(SubID 0x"+Integer.toHexString(demux.subID()).toUpperCase()+")") : "")); //DM19122003 081.6 int07 changed
				mpt(values);
				break;
			case 1: //DM30122003 081.6 int10 changed
				Msg("");
				Msg("--> Teletext (SubID 0x"+Integer.toHexString(demux.subID()).toUpperCase()+")");
				processTeletext(values);
				break;
			case 2:
				Msg("");
				Msg("--> MPEG Audio (0x"+Integer.toHexString(demux.getID()).toUpperCase()+")");
				mpt(values);
				break;
			case 4: //DM23022004 081.6 int18 add
				Msg("");
				Msg("--> LPCM Audio (SubID 0x"+Integer.toHexString(demux.subID()).toUpperCase()+")");
				processLPCM(values);
				break;
			case 5: //DM23022004 081.6 int18 add, //DM12042004 081.7 int01 changed
				Msg("");
				Msg("--> Subpicture (SubID 0x"+Integer.toHexString(demux.subID()).toUpperCase()+")");
				processSubpicture(values);
				break;
			}
			lfn[demux.getType()]++;
			new File(newfile).delete();
			new File(values[1]).delete();
		}
	}

	}  // end try
	catch (EOFException e1) { 
		//DM25072004 081.7 int07 add
		Msg("vdrparse EOF reached in error: " + e1); 
	}
	catch (IOException e2) { 
		//DM25072004 081.7 int07 add
		Msg("vdrparse File I/O error: " + e2); 
	}

	System.gc();
	return vptslog;

}  /** end methode vdr parse **/





/******************
 * ts Parser *
 ******************/
public String rawparse(String file, int[] pids, int ToVDR) {

	String fchild = (newOutName.equals("")) ? (new File(file).getName()).toString() : newOutName;
	String fparent = ( fchild.lastIndexOf(".") != -1 ) ? workouts+fchild.substring(0,fchild.lastIndexOf(".")) : workouts+fchild;

	/*** split part ***/
	fparent += (options[18]>0) ? "("+options[19]+")" : "" ;

	String vptslog = "-1";

	boolean ende=false;
	int packlength, ptslength;
	clv = new int[10];

	/*** d2v project ***/
	if (cBox[29].isSelected() || cBox[30].isSelected()) {
		String[] d2vopt = new String[d2vfield.length];
		for (int x=0;x<d2vfield.length;x++) 
			d2vopt[x] = d2vfield[x].getText();
		d2v.setOptions(d2vopt);
		d2v.Init(fparent);
	}

	try 
	{

	int[] newID = { 0xE0, 0xC0, 0x80, 0x90, 0xA0, 0x20 }; //DM27042004 081.7 int02 changed
	byte[] push189 = new byte[189];
	long packet=0;
	boolean pack=false;

	options[5]=262143;
	options[6]=0;
	options[7]=0;
	options[8]=-10000;
	options[12]=200;
	options[13]=1;
	options[41]=0;
	options[50]=0;
	CUT_BYTEPOSITION=0;
	long next_CUT_BYTEPOSITION=0;

	//DM18022004 081.6 int17 changed
	makeVDR makevdr = new makeVDR();

	if (ToVDR==1) 
		makevdr.init(fparent+".vdr",options,bs,ToVDR,(int)options[19]);
	else if (ToVDR==2) 
		makevdr.init(fparent+".m2p",options,bs,ToVDR,(int)options[19]);
	else if (ToVDR==3) 
		makevdr.init(fparent+".pva",options,bs,ToVDR,(int)options[19]);
	else if (ToVDR==4) 
		makevdr.init(fparent+"(ts).ts",options,bs,ToVDR,(int)options[19]); //DM24092003

	//DM14062004 081.7 int04 add
	else if (ToVDR==5) 
		makevdr.init(fparent + "[filtered].ts", options, bs, ToVDR, (int)options[19]);

	TSPID TSPid = new TSPID();
	PIDdemux demux;
	ArrayList usedPIDs = new ArrayList();

	if (options[19]==0) { 
		TSPidlist.clear();
		TSdemuxlist.clear();
	} else {
		for (int a=0;a<TSdemuxlist.size();a++) {       // find PID object
			demux = (PIDdemux)TSdemuxlist.get(a);

			//DM04032004 081.6 int18 fix
			if (demux.getnewID()!=0)
				newID[demux.getType()]++;

			if (demux.getNum()==-1) 
				continue;
			if (demux.getType()==3) 
				demux.initVideo2(fparent,options);
			else 
				demux.init2(fparent,options);
		}
	}

	if (options[19]==0)
	{
		if (pids.length>0) //DM10032004 081.6 int18 changed
		{
			Msg("-> SID 0x"+Integer.toHexString(pids[0]).toUpperCase());
			Msg("-> PMT 0x"+Integer.toHexString(pids[1]).toUpperCase()+" refers to these usable streams:");
			Msg(" Video " + scan.getVideo());
			Msg(" Audio " + scan.getAudio());
			Msg(" Teletext " + scan.getText());
			Msg(" Subpict. " + scan.getPics());  //DM28042004 081.7 int02 add
			Msg("");

			for (int a=2; a<pids.length;a++)
			{
				TSPid = new TSPID();
				TSPid.setPID(pids[a]);
				TSPidlist.add(TSPid);
			}
		}
		else 
			Msg("-> no PMT found (at a short scan)");
	}

	long count=0, size=0, startPoint=0;
	long starts[] = new long[combvideo.size()];

	for (int a=0; a<combvideo.size(); a++){
		file = combvideo.get(a).toString();
		starts[a] = size;
		size += new File(file).length();
	}

	file = combvideo.get(FileNumber).toString();
	count = starts[FileNumber];
	size = count + new File(file).length();

	//** pid inclusion 
	ArrayList abc = (ArrayList)speciallist.get(currentcoll);
	int[] include = new int[abc.size()];
	for (int a=0; a<abc.size(); a++) 
		include[a] = 0x1FFF&Integer.parseInt(abc.get(a).toString().substring(2),16);
	if (abc.size()>0)
		Msg("-> special PIDs for searching defined");

	//*** split skipping first
	if (options[18]>0)
		startPoint = options[20]-(comBox[25].getSelectedIndex()*1048576L);

	//*** jump near to first cut-in point to collect more audio
	if (comBox[17].getSelectedIndex()==0 && ctemp.size()>0 && cutcount==0)
		startPoint = Long.parseLong(ctemp.get(cutcount).toString())-((ToVDR==0)?2048000:0);

	if (startPoint < 0)
		startPoint = count;
	else if (startPoint < count){
		for (int a=starts.length; a>0; a--)
			if (starts[a-1] > startPoint)
				FileNumber--;
	}
	else if (startPoint > count){
		for (int a=FileNumber+1; a < starts.length; a++){  // (fix2)
			if (starts[a] > startPoint)
				break;
			else 
				FileNumber++;
		}
	}

	file = combvideo.get(FileNumber).toString();
	count = starts[FileNumber];

	if (FileNumber>0)
		Msg("continue with file: "+file);

	long base = count;

	//DM18062004 081.7 int05 changed
	PushbackInputStream in = raw_interface.getStream(file, 200);
	size = count + raw_interface.getStreamSize();


	while (count < startPoint) {
		count += in.skip(startPoint-count);
	}

	progress.setString(((ToVDR==0)?"demuxing":"converting")+" DVB MPEG-TS file  "+(new File(file)).getName());
	progress.setStringPainted(true);
	progress.setValue((int)((count-base)*100/(size-base))+1);
	yield();

	boolean error = false, start = false, ttx=false;
	int pid=0, scram=0, addfield=0, counter=0, addlength=0, pesID=0, psiID=0, rd=0; //ghost 23012004 081.6 int11 add
	int counter1 = -1; 
	int subid = 0; //DM27042004 081.7 int02 add
	long qexit = count+options[56];
	boolean miss=false, dontread=false; //ghost 23012004 081.6 int11 add

	morepva:
	while (true) {

		pvaloop:
		while ( count < size )  {     // till fileend (64bit size)

			while (qpause) 
				pause();

			if (qbreak || (qinfo && count>qexit)) { 
				qbreak=false; 
				break morepva; 
			}

			/*** cut end reached ***/
			if ((int)options[37]+20 < origframes) {
				ende=true;
				break morepva; 
			}

			if (comBox[17].getSelectedIndex()==0 && ctemp.size()>0) {
				if (cutcount==ctemp.size() && (cutcount&1)==0)
					if (count > Long.parseLong(ctemp.get(cutcount-1).toString())+2048000){
						ende=true;
						break morepva;
					}
			}

			//in.read(push189,0,189);
			//ghost 23012004 081.6 int11 changed
			if (!dontread || !cBox[53].isSelected())
			{
				rd = in.read(push189,0,189);

				//DM09072004 081.7 int06 add, fix
				if (rd == 188 && size - count == rd)
					push189[188] = 0x47;

				else if ( rd < 189 && cBox[53].isSelected())
				//if ( rd < 189 && cBox[53].isSelected())
				{
					Msg("!> incomplete ts packet at EOF detected @ "+count);
					count += rd;
					break pvaloop;
				}
			}

 			/*** check 0x47 sync  ***/
			//DM15072004 081.7 int06 changed
 			if ( push189[0] != 0x47 || (cBox[33].isSelected() && push189[188] != 0x47) )
			{
				//in.unread(push189,1,188);
				//ghost 23012004 081.6 int11 changed
				if (dontread && cBox[53].isSelected())
				{
					Msg("!> ts packet completion failed :-(");
					in.unread(push189,190-rd,rd-1);
					dontread=false;
				}
				else
					in.unread(push189,1,188);

				if (!cBox[3].isSelected() && !miss) //DM03112003 081.5++ info
					Msg("!> missing sync. byte @ "+count);
				miss=true;
				count++;
				continue pvaloop;
			}
			else if (dontread && cBox[53].isSelected())
				Msg("!> ts packet completion ok :-)");

			if (!cBox[3].isSelected() && miss) //DM03112003 081.5++ info
				Msg("!> found sync. byte @ "+count);
			miss=false;

			in.unread(push189,188,1);

			/**** mark for split ***/
			options[20]=count;
			next_CUT_BYTEPOSITION=count;

			//count+=188;
			//ghost 23012004 081.6 int11 changed
			if (dontread && cBox[53].isSelected())
			{
				count+=(rd-1);
				dontread=false;
			}
			else
				count+=188;

			packet++;

			error = ((0x80&push189[1])!=0) ? true : false;         // TS error indicator
			start = ((0x40&push189[1])!=0) ? true : false;         // new PES packet start
			pid = (0x1F & push189[1])<<8 | (0xFF & push189[2]);    // the PID
			scram = (0xc0 & push189[3])>>>6;                       // packet is scrambled (>0)
			addfield = (0x30 & push189[3])>>>4;                    // has adaption field ?
			counter = (0xf & push189[3]);                          // packet counter 0..f
			addlength = (addfield>1) ? (0xff & push189[4])+1 : 0;  // adaption field length

			//DM18092003+ fix
			progress.setValue((int)((count-base)*100/(size-base))+1);
			yield();

			/** pid inclusion **/
			includeloop:
			while (include.length>0) {
				for (int v=0; v<include.length; v++)
					if (pid==include[v])
						break includeloop;
				continue pvaloop;
			}

			//DM14062004 081.7 int04 add
			if (ToVDR == 5)
			{
				makevdr.writePacket(push189, 0, 188);
				continue pvaloop;
			}

			if ((addfield&1)==0)
				continue pvaloop;

			if (addlength>183 || (addlength>180 && start))
				error=true;

			if (error){
				Msg("!> PID 0x"+Integer.toHexString(pid).toUpperCase()+" -> TS bit error in packet "+packet+" @ pos. "+(count-188)+", dropping..");
				continue pvaloop;
			}

			// PES id, if packet start
			pesID = (start) ? ((255&push189[4+addlength])<<24 | (255&push189[5+addlength])<<16 |
					(255&push189[6+addlength])<<8 | (255&push189[7+addlength])) : 0;
			// PSI id, if packet start
			psiID = (start) ? pesID>>>16 : 0;

			//DM27042004 081.7 int02 changed++
			ttx = false;
			subid = 0;
			if (pesID == 0x1BD) 
			{
				int pes_extension_size = 0xFF & push189[12 + addlength];
				int pes_offset = 13 + addlength + pes_extension_size;
				ttx = (pes_extension_size == 0x24 && (0xFF & push189[pes_offset])>>>4 == 1) ? true : false;

				if (!ttx)
					subid = ((0xFF & push189[pes_offset]) == 0x20 && (0xFF & push189[pes_offset + 1]) == 0 && (0xFF & push189[pes_offset + 2]) == 0xF) ? 0x20 : 0;
			}
			//DM27042004 081.7 int02 changed--
			//DM18092003-

			int pidcheck=-1;
			for (int a=0;a<TSPidlist.size();a++) {      // find PID object
				TSPid = (TSPID)TSPidlist.get(a);
				if (pid==TSPid.getPID()) {
					pidcheck=a; 
					break; 
				}
			}
			if (pidcheck==-1) {   // create new PID object
				TSPid = new TSPID();
				TSPid.setPID(pid);
				if (pid==0x1FFF){
					Msg("--> PID 0x1FFF => stuffing packet -> ignored");
					TSPid.setneeded(false);
				}
				pidcheck = TSPidlist.size();
				TSPidlist.add(TSPid);
			}

			if (options[30]==1) 
				System.out.println(""+packet+"/"+Integer.toHexString(pid)+"/"+Integer.toHexString(pesID)+"/"+TSPid.isneeded()+"/"+ttx+"/"+error+"/"+start+"/"+scram+"/"+addfield+"/"+addlength);

			if (!TSPid.isneeded()) 
				continue pvaloop;        // PID not of interest

			if (cBox[38].isSelected()) {
				if (scram>0) {           // cannot work with scrambled data
					if (!TSPid.getScram()) {
						TSPid.setScram(true);
						Msg("--> PID 0x"+Integer.toHexString(pid).toUpperCase()+" => marked as scrambled data, packet "+packet+" @ "+(count-188)+" -> ignored");
					}
					continue pvaloop;
				} else {
					if (TSPid.getScram()) {
						TSPid.setScram(false);
						Msg("--> PID 0x"+Integer.toHexString(pid).toUpperCase()+" => get clear data, packet "+packet+" @ "+(count-188));
					}
				}
			}

			/** out of sequence? **/
			if (!cBox[40].isSelected() && (cBox[46].isSelected() || (!cBox[46].isSelected() && (1&addfield)!=0 ) )) {  // no payload == no counter++
				if (TSPid.getCounter()!=-1) {
					if (TSPid.isStarted() && counter!=TSPid.getCounter()) { //DM20122003 081.6 int07 changed
						Msg("--> PID 0x"+Integer.toHexString(pid).toUpperCase()+" -> packet "+packet+" @ pos. "+(count-188)+" out of sequence ("+counter+"/"+TSPid.getCounter()+") (shifting..)");
						TSPid.setCounter(counter);
					}
					TSPid.count();
				} else { 
					TSPid.setCounter(counter);
					TSPid.count();
				}
			}

			if (!start) {
				if (TSPid.isneeded() && TSPid.isStarted())
					TSPid.writeData(push189,4+addlength,184-addlength);
			} else {
				TSPid.setStarted(true);
				if (TSPid.getID()==-1) {    // create new demux object
					TSPid.setID(pesID);
					String type="";
					switch (0xfffffff0 & pesID) {
					case 0x1e0:
						type="(MPEG Video)";
						TSPid.setDemux(TSdemuxlist.size());
						demux = new PIDdemux();
						demux.setPID(pid);
						demux.setID(pesID);
						demux.setnewID(newID[0]++);
						demux.setsubID(0);
						demux.setType(3);
						demux.setStreamType(0);
						TSdemuxlist.add(demux);
						if (ToVDR==0){
							if (newID[0]==0xE1) //DM02112003 081.5++
								demux.initVideo(fparent,options,bs/TSdemuxlist.size(),TSdemuxlist.size(),2);
							else
							{
								type+=" -> ignored";
								TSPid.setneeded(false); //DM25012004 081.6 int11 fix add
							}
						}

						//DM09072004 081.7 int06 changed
						if (ToVDR > 0) 
							type += " mapped to 0x" + Integer.toHexString(newID[0] - 1).toUpperCase();

						break;
					case 0x1c0:
					case 0x1d0:
						type="(MPEG Audio)"; 
						TSPid.setDemux(TSdemuxlist.size());
						demux = new PIDdemux();
						demux.setPID(pid);
						demux.setID(pesID);
						demux.setnewID(newID[1]++);
						demux.setsubID(0);
						demux.setType(2);
						demux.setStreamType(0);
						TSdemuxlist.add(demux);
						if (ToVDR==0) 
							demux.init(fparent,options,bs/TSdemuxlist.size(),TSdemuxlist.size(),2);

						//DM09072004 081.7 int06 changed
						if (ToVDR > 0) 
							type+=" mapped to 0x"+Integer.toHexString(newID[1]-1).toUpperCase();

						break;
					}
					switch (pesID)
					{
					//DM27042004 081.7 int02 changed++
					case 0x1bd: 
						type = "(private stream 1)";
						type += (ttx ? " (TTX) ": "") + (subid != 0 ? " (SubID 0x" + Integer.toHexString(subid).toUpperCase() + ")" : ""); 

						TSPid.setDemux(TSdemuxlist.size());
						demux = new PIDdemux();
						demux.setPID(pid);
						demux.setID(pesID);
						demux.setsubID(subid);
						demux.setTTX(ttx);

						if (ttx)
							demux.setnewID(newID[3]++);

						else if (subid == 0x20)
							demux.setnewID(newID[5]++);

						else
							demux.setnewID(newID[2]++);

						demux.setStreamType(subid == 0x20 ? 2 : 0);
						TSdemuxlist.add(demux);

						if (ToVDR == 0) 
							demux.init(fparent, options, bs/TSdemuxlist.size(), TSdemuxlist.size(), 2);

						if (ToVDR > 0 && subid != 0) 
						{
							type += " -> ignored";
							TSPid.setneeded(false);

						}

						//DM09072004 081.7 int06 changed
						if (ToVDR > 0 && !ttx) 
							type += " mapped to 0x" + Integer.toHexString(newID[2]-1).toUpperCase();

						break;
					//DM27042004 081.7 int02 changed--

					case 0x1bf:
						Msg("--> PID 0x"+Integer.toHexString(pid).toUpperCase()+" (private stream 2) -> ignored");

						break;
					}

					//DM10032004 081.6 int08 changed
					//DM23072004 081.7 int07 changed
					if (type.equals(""))
					{
						switch (psiID)
						{

						case 0:
							type = "(PAT)"; 
							break;
						case 1: 
							type = "(CAT)"; 
							break;
						case 2: 
							type = "(PMT)"; 
							break;
						case 3: 
							type = "(TSD)"; 
							break;
						case 4: 
							type = "(PSI)"; 
							break;
						case 6: 
						case 0x40: 
						case 0x41: 
							type = "(NIT)"; 
							break;
						case 0x42: 
						case 0x46: 
							type = "(SDT)"; 
							break;
						case 0x4A: 
							type = "(BAT)"; 
							break;
						case 0x4E: 
						case 0x4F: 
						case 0x50: 
						case 0x60: 
							type = "(EIT)"; 
							break;
						case 0x70: 
							type = "(TDS)"; 
							break;
						case 0x71: 
							type = "(RS)"; 
							break;
						case 0x72: 
							type = "(ST)"; 
							break;
						case 0x73: 
							type = "(TO)"; 
							break;
						case 0x7E: 
							type = "(DI)"; 
							break;
						case 0x7F: 
							type = "(SIT)"; 
							break;
						case 0x82: 
							type = "(EMM)"; 
							break;
						case 0x80:
						case 0x81:
						case 0x83:
						case 0x84: 
							type = "(ECM)"; 
							break;
						case 0x43: 
						case 0x44: 
						case 0x45: 
						case 0x47: 
						case 0x48: 
						case 0x49: 
						case 0x4B: 
						case 0x4C: 
						case 0x4D: 
						case 0xFF: 
							type = "(res.)"; 
							break;
						default:
							if (psiID >= 0x50 && psiID <= 0x6F)
							{
								type = "(EIT)"; 
								break;
							}

							if ((psiID >= 4 && psiID <= 3F) || (psiID >= 0x74 && psiID <= 0x7D))
							{
								type = "(res.)"; 
								break;
							}

							if (psiID >= 0x80 && psiID < 0xFF)
							{
								type = "(user def. 0x" + Integer.toHexString(psiID).toUpperCase() + ")"; 
								break;
							}

							type += "(payload: ";
							for (int f=0; f < 8; f++) {
								String val = Integer.toHexString((0xFF&push189[4+addlength+f])).toUpperCase();
								type += " " + ((val.length()<2) ? ("0"+val) : val);
							}
							type += " ..)";
						}

						if (scram>0 && !cBox[38].isSelected()) {
							type+=" (0x"+Long.toHexString(count-188).toUpperCase()+" #"+packet+") ";  // pos + packno
							if (!TSPid.getScram()) 
								Msg("--> PID 0x"+Integer.toHexString(pid).toUpperCase()+" => marked as scrambled data (not ignored)"+type);
							TSPid.setScram(true);
							TSPid.setStarted(false);
							TSPid.setID(-1);
							TSPid.reset();
							continue pvaloop;
						}
						type+=" (0x"+Long.toHexString(count-188).toUpperCase()+" #"+packet+") ";  // pos + packno
						Msg("--> PID 0x"+Integer.toHexString(pid).toUpperCase()+" "+type+" -> ignored");

						//if (abc.size()==0 || type.indexOf("pay")==-1)  // matt16082003 fix 081.1
						if (!cBox[61].isSelected() || type.indexOf("pay") == -1)
							TSPid.setneeded(false);
						else
							TSPid.setID(-1);

						continue pvaloop;
					} else {
						type+=" (0x"+Long.toHexString(count-188).toUpperCase()+" #"+packet+") ";  // pos + packno
						Msg("ok> PID 0x"+Integer.toHexString(pid).toUpperCase()+" has PES-ID 0x"+Integer.toHexString(0xff&pesID).toUpperCase()+" "+type);
						usedPIDs.add("0x"+Integer.toHexString(pid));
					}
				}

				if (TSPid.getDemux()==-1 || !TSPid.isneeded()) //DM25012004 081.6 int11 fix changed
					continue pvaloop;

				demux = (PIDdemux)TSdemuxlist.get(TSPid.getDemux());

				//DM14072004 081.7 int06 add
				if (!demux.StreamEnabled())
				{}

				else if (TSPid.getDataSize()<7) {
					if (demux.getPackCount()!=-1) 
						Msg("--> PID 0x"+Integer.toHexString(pid).toUpperCase()+" lack of PES Data -> packet skipped");
				} else {

					/***!!! warning , special handling, packet may be bigger than 0xff ff max. size ****/
					byte[] setorig = TSPid.getData().toByteArray();
					byte[] sethead = new byte[9];
					int setcount = 0, pesoff = 0;
					System.arraycopy(setorig,0,sethead,0,6);
					sethead[6] |= (byte)0x80;
					byte[] set = new byte[0];

					//28042004 081.7 int02 add, don't fragment subpicture packet
					int packet_maximum = demux.subID() == 0x20 ? 0xFFFF : 0x17F0; 

					//28042004 081.7 int02 changed++
					while (setcount < setorig.length)
					{
						int newsize = (setorig.length - setcount < packet_maximum) ? (setorig.length - setcount) : packet_maximum; 
						set = new byte[pesoff + newsize];
						System.arraycopy(setorig, setcount, set, pesoff, newsize);

						if (setcount > 0) 
							System.arraycopy(sethead, 0, set, 0, 9);

						setcount += packet_maximum;

						pesoff = 9;
						int setlength = set.length - 6; 
						set[4] = (byte)(setlength>>>8);
						set[5] = (byte)(0xff & setlength);

						if (ToVDR==1) 
							options = makevdr.writeVDR(set,options,demux,0);
						else if (ToVDR==2) 
							options = makevdr.writeMPG(set,options,demux,0);
						else if (ToVDR==3) 
							options = makevdr.writePVA(set,options,demux,0);
						else if (ToVDR==4) 
							options = makevdr.writeTS(set,options,demux,0);
						else if (ToVDR == 0) //DM14062004 081.7 int04 changed
						{
							if (demux.getType()==3)
							{
								demux.writeVideo(set, options, false);
								CUT_BYTEPOSITION = next_CUT_BYTEPOSITION;
							}
							else 
								demux.write(set,true);
						}
					}
					//28042004 081.7 int02 changed--
				}
				TSPid.reset();
				TSPid.writeData(push189,4+addlength,184-addlength);
			}

			clv[5]++;

			showOutSize();
			if (ToVDR>0)  //DM06022004 081.6 int15 fix
				options[20] = count;

			/****** split size reached *****/
			if ( options[18]>0 && options[18]<options[41] ) 
				break pvaloop;

		}  // end while pvaloop

		if ( options[18]>0 && options[18]<options[41] ) 
			break morepva;

		/*** more files ***/
		if (FileNumber < combvideo.size()-1) { 
			in.close();
			System.gc();

			String nextfile = combvideo.get(++FileNumber).toString();
			count = size;

			//DM18062004 081.7 int05 changed++
			in = raw_interface.getStream(nextfile, 200);
			long fsize = raw_interface.getStreamSize();

			//Ghost23012004 081.6 int11 changed
			size += fsize;
			base = count;

			cell.add(""+(options[7]));
			Msg("-> actual written vframes: "+options[7]);
			Msg("switch to file: "+nextfile);
			progress.setString(((ToVDR==0)?"demuxing":"converting")+" DVB MPEG-TS file  "+(new File(nextfile)).getName());
			progress.setStringPainted(true);

			//Ghost23012004 081.6 int11 add
			if ( cBox[53].isSelected() && rd < 189 && fsize >= (189-rd))
			{
				dontread=true;
				rd=in.read(push189, rd, 189-rd);
				Msg("!> try to complete incomplete ts packet...");
			}
		} else 
			break morepva;

	} // end while more than 1 pva -> morepva

	Msg("packs: "+clv[5]+" "+((count*100/size))+"% "+count);

	/****** file end reached for split *****/
	if ( (count>=size || ende) && options[18]>0 ) 
		options[21]=-100;

	in.close(); 
	System.gc();

	if (ToVDR>0) 
		makevdr.close();
	else {
		int aa=0;
		for (int a=0;a<TSdemuxlist.size();a++) {
			demux = (PIDdemux)TSdemuxlist.get(a);
			if (demux.getType()==3) { 
				if (aa>0) 
					continue;

				/*** d2v project ***/
				if ((cBox[29].isSelected() || cBox[30].isSelected()) ) 
					d2v.write(options[50],options[7]);

				//DM12042004 081.7 int01 changed
				Msg("");
				Msg("-> video: fr/ ct/ 1p/ cg/ og/ dg = " + options[7] + "/ " + clv[0] + "/ " + clv[1] + "/ " + clv[2] + "/ " + clv[3] + "/ " + clv[4]);

				vptslog = demux.closeVideo();
				aa++;
			}
		} 

		//DM04032004 081.6 int18 changed
		//DM27042004 081.7 int02 changed
		int[] lfn = new int[6];
		for (int a=0; a < TSdemuxlist.size(); a++)
		{
			demux = (PIDdemux)TSdemuxlist.get(a);

			if (demux.getType()==3) 
				continue;

			String[] values = demux.close(vptslog);

			if (values[0].equals("")) 
				continue;

			String newfile = values[3]+ ((lfn[demux.getType()]>0) ? ("_"+lfn[demux.getType()]) : "") + "." + values[2];

			Common.renameTo(values[0], newfile); //DM13042004 081.7 int01 changed
			//new File(values[0]).renameTo(new File(newfile));

			values[0] = newfile;
			values[3] = vptslog;     // set videolog
			switch (demux.getType())
			{
			case 0:
				Msg("");
				Msg("--> AC-3/DTS Audio on PID 0x"+Integer.toHexString(demux.getPID()).toUpperCase()); //DM19122003 081.6 int07 changed
				mpt(values);
				break;

			case 1: //DM30122003 081.6 int10 changed
				Msg("");
				Msg("--> Teletext on PID 0x"+Integer.toHexString(demux.getPID()).toUpperCase()+" (SubID 0x"+Integer.toHexString(demux.subID()).toUpperCase()+")");
				processTeletext(values);
				break;

			case 2: 
				Msg("");
				Msg("--> MPEG Audio (0x"+Integer.toHexString(0xFF&demux.getID()).toUpperCase()+") on PID 0x"+Integer.toHexString(demux.getPID()).toUpperCase());
				mpt(values);
				break;

			case 5: //DM27042004 081.7 int02 add
				Msg("");
				Msg("--> Subpicture (SubID 0x"+Integer.toHexString(demux.subID()).toUpperCase()+")");
				processSubpicture(values);
				break;
			}
			lfn[demux.getType()]++;
			new File(newfile).delete();
			new File(values[1]).delete();

			System.gc();
		}

	} // tovdr==0


	/*** on qinfo load usable PIDs in specaillist, if list was empty ***/
	activecoll=currentcoll;
	ArrayList xyz = (ArrayList)speciallist.get(activecoll);


	if (qinfo && !(xyz.size()>1)) {
		for (int t=0; t<usedPIDs.size(); t++) 
			xyz.add(usedPIDs.get(t));
		speciallist.set(activecoll,xyz);
		if (dialog.isVisible()) 
			dialog.entry();
	}

	}
	catch (EOFException e1)
	{
		//DM27072004 081.7 int07 changed
		Msg("raw EOF reached in error " + e1); 
	}
	catch (IOException e2)
	{ 
		//DM27072004 081.7 int07 changed
		Msg("raw File I/O error " + e2); 
	}

	yield();

	return vptslog;

}  /** end methode raw2vdr parse **/




/**********************
 * nextfile PTS check *
 **********************/
public long nextFilePTS(int type, int ismpg, long lastpts, int file_number) {
	long pts = 0, count = 0;
	byte[] push25 = new byte[25];
	java.text.DateFormat sms = new java.text.SimpleDateFormat("HH:mm:ss.SSS");
	sms.setTimeZone(java.util.TimeZone.getTimeZone("GMT+0:00"));

	if (combvideo.size() > file_number)
	{
		try 
		{
		//DM13082004 081.7 int09 changed
		long filesize = new File(combvideo.get(file_number).toString()).length();
		long buffersize = Long.parseLong(comBox[37].getSelectedItem().toString());

		if (filesize > buffersize) //DM19122003 081.6_int07 changed
			filesize = buffersize;

		PushbackInputStream in = new PushbackInputStream(new FileInputStream(combvideo.get(file_number).toString()),6500);

		switch (type) {
		case 0: {    // pva
			pvaloop:
			while (count < filesize) {
				in.read(push25,0,25);
				if ((255&push25[0])!=0x41 || (255&push25[1])!=0x56 || (255&push25[4])!=0x55) {
					in.unread(push25,1,24); 
					count++; 
					continue pvaloop;
				}
				int stype = 255&push25[2];     // 0x01 is Video, 0x02 is Audio
				int ptsflag = 255&push25[5];   // read byte pts-flag & pre/postbytes
				int packlength = (255&push25[6])<<8 | (255&push25[7]);    // av pack length bytes to follow
				boolean cpts = ( (16 & ptsflag) !=0 ) ? true : false;

 				if (stype==1) {
					if (cpts) {
						pts = 0xFFFFFFFFL & ((255&push25[8])<<24 | (255&push25[9])<<16 | (255&push25[10])<<8 | (255&push25[11]));        // read pts 32bit +convert to long
						break pvaloop;
					} 
				} else if (stype==2) {
					int b = (cBox[28].isSelected() && !cpts) ? 0 : 3;

					ptsloop:

					for (int a=0;a<b;a++) {
						if (push25[8+a]!=0 || push25[9+a]!=0 || push25[10+a]!=1 || ((0xFF&push25[11+a])!=0xBD && (0xF0&push25[11+a])!=0xC0)) 
							continue ptsloop;
						if ( (0x80&push25[15+a])==0 ) 
							break ptsloop;
						pts = 0xFFFFFFFFL & ( (6&push25[17+a])<<29 | (255&push25[18+a])<<22 | 
								(254&push25[19+a])<<14 | (255&push25[20+a])<<7 | (254&push25[21+a])>>>1 );
						break pvaloop;
					}
				}
				in.unread(push25,8,17);
				in.skip(packlength);
				count+= 8+packlength;
			}
			break;
		}
		case 1: {    // mpg

			int packlength=0, ptslength=0;
			boolean cpts=false, nullpacket=false;
			byte[] push9 = new byte[9];

			pvaloop:
			while (count < filesize) {
				in.read(push9);

				/*** check 0x000001  ***/
				if ( push9[0]!=0 || push9[1]!=0 || push9[2]!=1) { 
					in.unread(push9,1,8); 
					count++; 
					continue pvaloop; 
				}
				if (ismpg>0 && (push9[3]==(byte)0xba || push9[3]==(byte)0xbb) ) { 
					in.skip(3); 
					count+=9+3; 
					continue pvaloop; 
				}
				if (ismpg>0 && (push9[3]==(byte)0xbe || push9[3]==(byte)0xbf) ) {
					packlength = ((255&push9[4])<<8 | (255&push9[5])); 
					in.skip(packlength-3);
					count+=6+packlength; 
					continue pvaloop;
				}
				if ( (0xf0&push9[3])!=0xe0 && (0xf0&push9[3])!=0xc0 && (255&push9[3])!=0xbd ) { 
					in.unread(push9,1,8); 
					count++; 
					continue pvaloop; 
				}

				type = 255&push9[3];     // 0xe0 is Video1, 0xc0 is Audio1, 0xbd is ac3
				if (ismpg==1) {
					packlength = ((255 & push9[4])<<8 | (255 & push9[5]));    // av pack length bytes to follow
					in.unread(push9,6,3); 
					count+=6;
					byte[] push1 = new byte[1];

					skiploop:
					while(true) {
						in.read(push1,0,1);
						switch (0xC0&push1[0]) {
						case 0x40: { 
							in.skip(1); 
							count+=2; 
							packlength-=2; 
							continue skiploop; 
						}
						case 0x80: { 
							in.skip(2); 
							count+=3; 
							packlength-=3; 
							continue skiploop; 
						}
						case 0xC0: { 
							count++; 
							packlength--; 
							continue skiploop; 
						}
						case 0: { 
							break; 
						}
						}
						switch (0x30&push1[0]) {
						case 0x20: { 
							in.unread(push1); 
							cpts=true; 
							ptslength=5; 
							packlength-=5; 
							break skiploop; 
						}
						case 0x30: { 
							in.unread(push1); 
							cpts=true; 
							ptslength=10; 
							packlength-=10; 
							break skiploop; 
						}
						case 0x10: { 
							in.skip(4); 
							cpts=false; 
							ptslength=5; 
							packlength-=5; 
							break skiploop; 
						}
						case 0: { 
							cpts=false; 
							ptslength=0; 
							count++; 
							packlength--; 
							break skiploop; 
						}
						}
					}
				} else {
					cpts = ( (0x80 & push9[7]) !=0 ) ? true : false;
					ptslength = 0xff & push9[8]; 
					count+=9;
					packlength = ((255 & push9[4])<<8 | (255 & push9[5]));
					if (packlength==0) 
						nullpacket=true;
					packlength = packlength-3-ptslength;
				}
				if (cpts) {
					byte[] ptsdata = new byte[ptslength];
					in.read(ptsdata,0,ptslength);
					pts = 0xFFFFFFFFL & ( (6&ptsdata[0])<<29 | (255&ptsdata[1])<<22 | (254&ptsdata[2])<<14 |
							(255&ptsdata[3])<<7 | (254&ptsdata[4])>>>1 );
				} else  





					in.skip(ptslength);

				count+=ptslength+packlength;
				in.skip(packlength);

				if (nullpacket) 
					break;

			} // end while

			break;
		}
		} // end switch

		in.close();

		} 
		catch (IOException e) { 
			Msg("nextfile "+e); 
		}
	}

	if (file_number==0) {  // need global offset?
		pts &= 0xFFFFFFFFL;
		String x = comBox[27].getSelectedItem().toString();
		if (x.equals("auto")) { 
			long newpts = ((pts/324000000L)-1L)*324000000L;
			Msg("-> shift original PTS automatic backward by "+(newpts/324000000L)+" hour(s)");
			return newpts;
		} else if (!x.equals("0")) { 
			Msg("-> shift original PTS manual backward by "+comBox[27].getSelectedItem()+" hour(s)");
			return ((long)(Double.parseDouble(comBox[27].getSelectedItem().toString())*324000000L)); //DM26022004 081.6 int18 changed
		} else 
			return 0L;
	} else {
		pts -= options[27];
		pts &= 0xFFFFFFFFL;
		Msg("-> next file start with PTS: "+sms.format(new java.util.Date(pts/90L))+" / last actual PTS is "+sms.format(new java.util.Date(lastpts/90L)));
		if (Math.abs(pts-lastpts)<900000) //DM06022004 081.6 int15 changed
			return -1L;
		else if (pts>lastpts)  
			return 0L;
		else 
			return ((lastpts+1800000L)-pts); //DM09012004 081.6 int11 changed
	}
}



/**********************
 * read bytes for overlap pva check *
 **********************/
public byte[] overlapPVA(byte[] overlapnext) {
	//if (combvideo.size()>1) {
	if (FileNumber < combvideo.size()-1) {
		try 
		{
		//RandomAccessFile ovl = new RandomAccessFile(combvideo.get(1).toString(),"r");
		RandomAccessFile ovl = new RandomAccessFile(combvideo.get(FileNumber+1).toString(),"r");
		ovl.seek(0);
		ovl.read(overlapnext);
		ovl.close();
		}
		catch (IOException e) {
			Msg("overlap read error "+e); 
		}
	}
	return overlapnext;
}


/**********************************
 * PVA/PSV/PSA alternative Parser *
 **********************************/

public String pvaparse(String pvafile,int ismpg,int ToVDR, String vptslog) {

	String fchild = (newOutName.equals("")) ? (new File(pvafile).getName()).toString() : newOutName;
	String fparent = ( fchild.lastIndexOf(".") != -1 ) ? workouts+fchild.substring(0,fchild.lastIndexOf(".")) : workouts+fchild;

	/*** split part ***/
	fparent += (options[18]>0) ? "("+options[19]+")" : "" ;

	String pa2name = fparent+"_.raw";
	if (options[31]==1)
		pa2name=fparent+"_0x"+Integer.toHexString((int)options[33])+".raw";

	boolean cpts=false, ende=false;
	boolean md = (!cBox[28].isSelected()) ? true : false;
	int pid, ptsflag, packlength, counter;
	clv = new int[10];
	long pts=0, lastpts=0, ptsoffset=0, packet=0;

	int[] newID = { 0xE0, 0xC0, 0x80, 0x90 };

	RAWFILE rawfile = new RAWFILE();
	TSPID TSPid = new TSPID();
	PIDdemux demux = new PIDdemux();

	if (options[19]==0) {
		PVAPidlist.clear();
		PVAdemuxlist.clear();
	} else {
		for (int a=0;a<PVAdemuxlist.size();a++) {
			demux = (PIDdemux)PVAdemuxlist.get(a);

			//DM04032004 081.6 int18 fix
			if (demux.getnewID()!=0)
				newID[demux.getType()]++;

			if (demux.getNum()==-1) 
				continue;
			if (demux.getType()==3) 
				demux.initVideo2(fparent,options);
			else 
				demux.init2(fparent,options);
		}
	}

	//DM18022004 081.6 int17 changed
	makeVDR makevdr = new makeVDR();
	if (ToVDR==1) 
		makevdr.init(fparent+".vdr",options,bs,ToVDR,(int)options[19]);
	else if (ToVDR==2) 
		makevdr.init(fparent+".m2p",options,bs,ToVDR,(int)options[19]);
	else if (ToVDR==3) 
		makevdr.init(fparent+"(new).pva",options,bs,ToVDR,(int)options[19]);
	else if (ToVDR==4) 
		makevdr.init(fparent+".ts",options,bs,ToVDR,(int)options[19]);

	//DM14062004 081.7 int04 add
	else if (ToVDR == 5) 
		makevdr.init(fparent + "[filtered].pva", options, bs, ToVDR, (int)options[19]);


	/*** d2v project ***/
	if (cBox[29].isSelected() || cBox[30].isSelected()) {
		String[] d2vopt = new String[d2vfield.length];
		for (int x=0;x<d2vfield.length;x++) 
			d2vopt[x] = d2vfield[x].getText();
		d2v.setOptions(d2vopt);
		d2v.Init(fparent);
	}

	try 
	{

	byte[] push12 = new byte[12];
	byte[] push256 = new byte[256];
	byte[] push8 = new byte[8];
	byte[] push4 = new byte[4];
	byte[] seqstart = { 0,0,1,(byte)0xb3 };
	byte[] data = new byte[1];
	byte[] data2 = new byte[1];
	byte[] overlapnext = new byte[256];
	byte[] pes1 = { 0,0,1,(byte)0xE0,0,0,(byte)0x80,0,0 };
	byte[] pes2 = { 0,0,1,(byte)0xE0,0,0,(byte)0x80,(byte)0x80,5,0,0,0,0,0 };
	String[] streamtypes = { " (AC-3/DTS)"," (TTX)"," (MPEG Audio)"," (MPEG Video)" };
	int isheader=0;
	options[5]=262143;
	options[6]=0;
	options[7]=0;
	options[8]=-10000;
	options[12]=200;
	options[13]=1;
	options[41]=0;
	options[50]=0;
	CUT_BYTEPOSITION=0;

	long count=0, size=0, startPoint=0;
	long starts[] = new long[combvideo.size()];

	for (int a=0; a<combvideo.size(); a++){
		pvafile = combvideo.get(a).toString();
		starts[a] = size;
		size += new File(pvafile).length();
	}

	pvafile = combvideo.get(FileNumber).toString();
	count = starts[FileNumber];
	size = count + new File(pvafile).length();

	/** pid inclusion **/
	ArrayList abc = (ArrayList)speciallist.get(currentcoll);
	int[] include = new int[abc.size()];
	for (int a=0; a<abc.size(); a++) 
		include[a] = 0xFF&Integer.parseInt(abc.get(a).toString().substring(2),16);
	if (abc.size()>0)
		Msg("-> special PIDs for searching defined");

	//*** split skipping first
	if (options[18]>0)
		startPoint = options[20]-(comBox[25].getSelectedIndex()*1048576L);


	//*** jump near to first cut-in point to collect more audio
	if (comBox[17].getSelectedIndex()==0 && ctemp.size()>0 && cutcount==0)
		startPoint = Long.parseLong(ctemp.get(cutcount).toString())-((ToVDR==0)?2048000:0);

	if (startPoint < 0)
		startPoint = count;
	else if (startPoint < count){
		for (int a=starts.length; a>0; a--)
			if (starts[a-1] > startPoint)
				FileNumber--;
	}
	else if (startPoint > count){
		for (int a=FileNumber+1; a < starts.length; a++){ // (fix2)
			if (starts[a] > startPoint)
				break;
			else 
				FileNumber++;
		}
	}

	pvafile = combvideo.get(FileNumber).toString();
	count = starts[FileNumber];

	//size = count + new File(pvafile).length();

	if (FileNumber>0)
		Msg("continue with file: "+pvafile);

	long base = count;

	//PushbackInputStream in = new PushbackInputStream(new FileInputStream(pvafile),bs);

	//DM18062004 081.7 int05 changed
	PushbackInputStream in = raw_interface.getStream(pvafile, 65600);
	size = count + raw_interface.getStreamSize();


	while (count < startPoint) {
		count += in.skip(startPoint-count);
	}

	if (options[31]==1) 
		rawfile.init(pa2name,bs);

	overlapPVA(overlapnext);

	progress.setString(((ToVDR==0)?"demuxing":"converting")+" PVA file  "+(new File(pvafile)).getName());
	progress.setStringPainted(true);
	progress.setValue((int)((count-base)*100/(size-base))+1);
	yield();
	long qexit = count+options[56];
	boolean miss=false;

	morepva:
	while (true) {

		/*** start loop fileread ****/
		pvaloop:
		while ( count < size )  { 

			while (qpause) 
				pause();
			if (qbreak || (qinfo && count>qexit)) { 
				qbreak=false; 
				break morepva; 
			}

			/*** cut end reached ***/
			if ((int)options[37]+20 < origframes) {
				ende=true;
				break morepva; 
			}

			if (comBox[17].getSelectedIndex()==0 && ctemp.size()>0) {
				if (cutcount==ctemp.size() && (cutcount&1)==0)
					if (count > Long.parseLong(ctemp.get(cutcount-1).toString())+2048000){
						ende=true;
						break morepva;
					}
			}

			in.read(push8,0,8);

			/*** check 0x4156 is PVA (ascii AV) ***/
			if ((255&push8[0])!=0x41 || (255&push8[1])!=0x56 || (255&push8[4])!=0x55) {
				in.unread(push8,1,7);
				if (!cBox[3].isSelected() && !miss) //DM03112003 081.5++ info
					Msg("!> missing syncword @ "+count);
				miss=true;
				count++;
				continue pvaloop;
			}

			if (!cBox[3].isSelected() && miss) //DM03112003 081.5++ info
				Msg("!> found syncword @ "+count);
			miss=false;

			/**** overlapcheck ***/
			//if (cBox[48].isSelected() && combvideo.size()>1) {
			if (cBox[48].isSelected() && FileNumber < combvideo.size()-1) {
				in.unread(push8);
				in.read(push256,0,256);
				if (java.util.Arrays.equals(overlapnext,push256)) { 
					Msg("File-Overlap detected @ filepos.: "+count);
					break pvaloop;
				}
				in.unread(push256);
				in.read(push8,0,8);
			}

			/**** mark for split at sequenceheader ***/
			options[20]=count;
			CUT_BYTEPOSITION=count;

			count+=8;
			packet++;

			pid = 0xFF&push8[2];          // 0x01 is Video, 0x02 is Audio
			ptsflag = 0xFF&push8[5];      // read byte pts-flag & pre/postbytes
			counter = 0xFF&push8[3];                             
			packlength = (0xFF&push8[6])<<8 | (0xFF&push8[7]);    // av pack length bytes to follow
			cpts = ( (16 & ptsflag) !=0 ) ? true : false;

			progress.setValue((int)((count-base)*100/(size-base))+1);
			yield();

			clv[5]++;
			if (options[30]==1) 
				System.out.print("\rpacks: "+clv[5]+" "+((count*100/size))+"% "+(count-8));

			/** pid inclusion **/
			includeloop:
			while (include.length>0) {
				for (int v=0; v<include.length; v++) 
					if (pid==include[v]) 
						break includeloop;
				in.skip(packlength);
				count+=packlength;
				continue pvaloop;
			}

			if (pid==1 && cpts) {
				in.read(push4);
				pts = options[55] = 0xFFFFFFFFL & ((255&push4[0])<<24 | (255&push4[1])<<16 | (255&push4[2])<<8 | (255&push4[3]));
				count+=4;
				packlength-=4;
			} else 
				options[55]=-1;

			if (options[31]==1) {
				if (pid==(int)options[33]) {    // extract raw pes audio pva
					if (pid==1) {
						if (cpts) {
							pes2[4] = (byte)((packlength+8)>>>8);
							pes2[5] = (byte)(0xFF&(packlength+8));
							pes2[9] = (byte)(0x21 | (0xE&(pts>>>29)));
							pes2[10] = (byte)(0xFF&(pts>>>22));
							pes2[11] = (byte)(1 | (0xFE&(pts>>>14)));
							pes2[12] = (byte)(0xFF&(pts>>>7));
							pes2[13] = (byte)(1 | (0xFE&(pts<<1)));
							rawfile.write(pes2);
						} else {
							pes1[4] = (byte)((packlength+3)>>>8);
							pes1[5] = (byte)(0xFF&(packlength+3));
							rawfile.write(pes1);
						}
					}
					byte[] raw = new byte[packlength];
					in.read(raw); 
					rawfile.write(raw);
					options[39]+=raw.length;
				} else {
					long ff = in.skip(packlength);
					if ((int)ff<packlength) 
						in.skip(packlength-ff);
				}
				count+=packlength; 
				continue pvaloop;
			}

			data = new byte[packlength];
			in.read(data);
			count+=packlength;

			//DM14062004 081.7 int04 add
			if (ToVDR == 5)
			{
				makevdr.writePacket(push8, 0, 8);

				if (pid == 1 && cpts)
					makevdr.writePacket(push4, 0, 4);

				makevdr.writePacket(data, 0, packlength);

				continue pvaloop;
			}

			int pidcheck=-1;
			for (int a=0;a<PVAPidlist.size();a++) {
				TSPid = (TSPID)PVAPidlist.get(a);
				if (pid==TSPid.getPID()) { 
					pidcheck=a; 
					break; 
				}
			}
			if (pidcheck==-1) {   // create new PID object
				TSPid = new TSPID();
				TSPid.setPID(pid);
				Msg("-> found ID 0x"+Integer.toHexString(pid).toUpperCase()+" @ "+options[20]); //DM02022004 081.6 int14 changed
				comBox[9].addItem(Integer.toHexString(pid));
				PVAPidlist.add(TSPid);
			} 

			/** out of sequence? **/
			if (!cBox[40].isSelected()) {
				if (TSPid.getCounter()!=-1) {
					if (counter!=TSPid.getCounter()) { 
						Msg("!> ID 0x"+Integer.toHexString(pid).toUpperCase()+" -> packet "+packet+" @ pos. "+(count-8-packlength)+" out of sequence ("+counter+"/"+TSPid.getCounter()+") (shifting..)");
						TSPid.setCounter(counter);
					}
					TSPid.countPVA();
				} else { 

					TSPid.setCounter(counter);
					TSPid.countPVA();
				}
			}

			if (pid!=1 && !cpts && md && packlength>3) {
				int p=0;
				if (data[p]==0 && data[p+1]==0 && data[p+2]==1 && (data[p+3]==(byte)0xBD || (0xE0&data[p+3])==0xC0)) 
					cpts=true;
			}

			if (TSPid.isStarted()) 
				demux = (PIDdemux)PVAdemuxlist.get(TSPid.getID());
			else {   // create new ID object
				String IDtype="";
				switch (pid) {
				case 1: { 
					IDtype="(Video)";
					demux = new PIDdemux();
					TSPid.setStarted(true);
					demux.setID(0xE0);
					demux.setnewID(newID[0]++);
					demux.setPID(pid);
					demux.setsubID(0);
					demux.setStreamType(ismpg);
					demux.setType(3);
					TSPid.setID(PVAdemuxlist.size());
					PVAdemuxlist.add(demux);
					if (ToVDR==0) 
						demux.initVideo(fparent,options,bs,PVAdemuxlist.size(),3);
					else 
						IDtype+=" mapped to 0xE0"+streamtypes[3];
					break; 
				}
				case 2: { 
					IDtype="(mainAudio)";
				/**
					if (!cpts) 
						continue pvaloop;
					int streamID = 0xFF&data[3];
					demux = new PIDdemux();
					TSPid.setStarted(true);
					demux.setID(streamID);
					demux.setnewID(newID[1]++);
					demux.setPID(pid);
					demux.setsubID(0);
					demux.setType(2);
					demux.setStreamType(ismpg);
					TSPid.setID(PVAdemuxlist.size());
					PVAdemuxlist.add(demux);
					IDtype+=" has PES-ID 0x"+Integer.toHexString(streamID).toUpperCase()+streamtypes[2];
					if (ToVDR==0) 
						demux.init(fparent,options,bs/PVAdemuxlist.size(),PVAdemuxlist.size(),3);
					else 
						IDtype+=" mapped to 0x"+Integer.toHexString(demux.getnewID()).toUpperCase();
					break; 
				**/
				}
				default: { 
					IDtype="(additional)"; 
					if (!cpts) 
						continue pvaloop;
					int streamID = 0xFF&data[3];
					if ((0xE0&streamID)!=0xC0 && streamID!=0xBD) { 
						TSPid.setneeded(false); 
						break; 
					}
					demux = new PIDdemux();
					TSPid.setStarted(true);
					demux.setPID(pid);
					demux.setType((streamID!=0xBD)?2:0);    // MPA?
					demux.setID(streamID);
					demux.setsubID(0);
					boolean ttx = ((0xFF&data[8])==0x24) ? true : false;
					demux.setnewID(((streamID==0xBD)?((ttx)?newID[3]++:newID[2]++):newID[1]++));
					demux.setTTX(ttx);
					demux.setStreamType(ismpg);
					TSPid.setID(PVAdemuxlist.size());
					PVAdemuxlist.add(demux);
					IDtype+=" has PES-ID 0x"+Integer.toHexString(streamID).toUpperCase()+streamtypes[demux.getType()];
					if (ToVDR==0) 
						demux.init(fparent,options,bs/PVAdemuxlist.size(),PVAdemuxlist.size(),3);
					else 
						IDtype+=" mapped to 0x"+Integer.toHexString(demux.getnewID()).toUpperCase();
					break; 
				}
				}
				Msg("-> ID 0x"+Integer.toHexString(pid).toUpperCase()+" "+IDtype);
			}


			//DM14072004 081.7 int06 add
			if (!demux.StreamEnabled())
			{
				continue pvaloop;
			}

			if (ToVDR==0) {
				if (demux.getType()==3) 
					demux.writeVideo(data,options,true);
				else { 
					if (cpts) {
						data[4] = (byte)((data.length-6)>>>8);
						data[5] = (byte)(0xFF&(data.length-6));
					}
					demux.write(data,cpts);
				}
				if (demux.getPTS()>lastpts) 
					lastpts=demux.getPTS();

				/****** split size reached *****/
				if ( options[18]>0 && options[18]<options[41] ) 
					break pvaloop;






				continue pvaloop;
			}

			/*** create header + pts for video ***/
			if (pid==1) {
				if (cpts) {
					pes2[4] = (byte)((packlength+8)>>>8);
					pes2[5] = (byte)(0xFF&(packlength+8));
					pes2[9] = (byte)(0x21 | (0xE&(pts>>>29)));
					pes2[10] = (byte)(0xFF&(pts>>>22));
					pes2[11] = (byte)(1 | (0xFE&(pts>>>14)));
					pes2[12] = (byte)(0xFF&(pts>>>7));
					pes2[13] = (byte)(1 | (0xFE&(pts<<1)));

					data2 = new byte[14+data.length];
					System.arraycopy(pes2,0,data2,0,14);
					System.arraycopy(data,0,data2,14,data.length);
				} else {
					pes1[4] = (byte)((packlength+3)>>>8);
					pes1[5] = (byte)(0xFF&(packlength+3));

					data2 = new byte[9+data.length];
					System.arraycopy(pes1,0,data2,0,9);
					System.arraycopy(data,0,data2,9,data.length);
				}
			} else {
				if (cpts) {
					data2=data;
					data2[3] = (byte)(demux.getID());
					data2[4] = (byte)((packlength-6)>>>8);
					data2[5] = (byte)(0xFF&(packlength-6));
					if ((0x80&data2[7])!=0) 
						data2[9] &= ~8;
				} else {
					pes1[4] = (byte)((packlength+3)>>>8);
					pes1[5] = (byte)(0xFF&(packlength+3));
					data2 = new byte[9+data.length];
					System.arraycopy(pes1,0,data2,0,9);
					System.arraycopy(data,0,data2,9,data.length);
					data2[3] = (byte)(demux.getID());
				}
			}

			if (ToVDR==1) 
				options = makevdr.writeVDR(data2,options,demux,0);
			else if (ToVDR==2) 
				options = makevdr.writeMPG(data2,options,demux,0);
			else if (ToVDR==3) 
				options = makevdr.writePVA(data2,options,demux,0);
			else if (ToVDR==4) 
				options = makevdr.writeTS(data2,options,demux,0);

			showOutSize();
			if (ToVDR>0)  //DM06022004 081.6 int15 fix
				options[20] = count;

			/****** split size reached *****/
			if ( options[18]>0 && options[18]<options[41] ) 
				break pvaloop;

		}  // end while pvaloop


		if ( options[18]>0 && options[18]<options[41] )
			break morepva;

		if (FileNumber < combvideo.size()-1) { 
			in.close();
			System.gc();
			if (cBox[49].isSelected() && ToVDR==0) {
				ptsoffset = nextFilePTS(0,0,lastpts,FileNumber+1);
				if (ptsoffset==-1) 
					ptsoffset=0; 
				else {
					for (int a=0;a<PVAdemuxlist.size();a++) {
						demux = (PIDdemux)PVAdemuxlist.get(a);
						demux.PTSOffset(ptsoffset);
						if (demux.getPID()==1) 
							demux.resetVideo();
					}
					options[13]=1;
					newvideo=true;  // global boolean
				}
				cell.add(""+(options[7]));
			}
			String nextfile = combvideo.get(++FileNumber).toString();
			count = size;

			//size += new File(nextfile).length();
			//in = new PushbackInputStream(new FileInputStream(nextfile),bs-1000000);

			//DM18062004 081.7 int05 changed
			in = raw_interface.getStream(nextfile, 65600);
			size += raw_interface.getStreamSize();
			base = count;


			Msg("-> actual written vframes: "+options[7]);
			Msg("switch to file: "+nextfile);
			progress.setString(((ToVDR==0)?"demuxing":"converting")+" PVA file  "+new File(nextfile).getName());
			progress.setStringPainted(true);
			overlapPVA(overlapnext);
		} else 
			break morepva;

	} // end while more than 1 pva -> morepva


	/****** file end reached for split *****/
	if ( (count>=size || ende) && options[18]>0 ) 
		options[21]=-100;

	in.close(); 

	if (ToVDR>0) 
		makevdr.close();
	else {
		int aa=0;
		for (int a=0;a<PVAdemuxlist.size();a++) {
			demux = (PIDdemux)PVAdemuxlist.get(a);
			if (demux.getType()==3) { 
				if (aa>0) 
					continue;

				/*** d2v project ***/

				if ((cBox[29].isSelected() || cBox[30].isSelected()) ) 
					d2v.write(options[50],options[7]);

				//DM12042004 081.7 int01 changed
				Msg("");
				Msg("-> video: fr/ ct/ 1p/ cg/ og/ dg = " + options[7] + "/ " + clv[0] + "/ " + clv[1] + "/ " + clv[2] + "/ " + clv[3] + "/ " + clv[4]);

				vptslog = demux.closeVideo();
				aa++;
			}
		} 
		System.gc();

		//DM04032004 081.6 int18 changed
		int[] lfn = new int[3];
		int[] IDs = { 0xC0,0x80 };
		for (int a=0;a<PVAdemuxlist.size();a++) { //DM30122003 081.6 int10 changed
			demux = (PIDdemux)PVAdemuxlist.get(a);
			if (demux.getType()==3) 
				continue;
			if (demux.getID()==0) 
				continue;
			String[] values = demux.close(vptslog);
			if (values[0].equals("")) 
				continue;
			String newfile = values[3]+ ((lfn[demux.getType()]>0) ? ("_"+lfn[demux.getType()]) : "") + "." + values[2];

			Common.renameTo(values[0], newfile); //DM13042004 081.7 int01 changed
			//new File(values[0]).renameTo(new File(newfile));

			values[0] = newfile;
			values[3] = vptslog;
			switch (demux.getType()) {
			case 0: {
				if ( demux.subID()!=0 && (0xF0&demux.subID())!=0x80 ) 
					break;
				Msg("");
				Msg("--> AC-3/DTS Audio on ID 0x"+Integer.toHexString(demux.getPID()).toUpperCase()+" "+((demux.subID()!=0) ? ("(SubID 0x"+Integer.toHexString(demux.subID()).toUpperCase()+")") : "")); //DM19122003 081.6 int07 changed
				mpt(values);
				break;
			}
			case 1: {
				Msg("");
				Msg("--> Teletext on ID 0x"+Integer.toHexString(demux.getPID()).toUpperCase()+" (SubID 0x"+Integer.toHexString(demux.subID()).toUpperCase()+")");
				processTeletext(values);
				break;
			}
			case 2: { 
				Msg("");
				Msg("--> MPEG Audio on ID 0x"+Integer.toHexString(demux.getPID()).toUpperCase()+" (0x"+Integer.toHexString(demux.getID()).toUpperCase()+")");
				mpt(values);
				break;
			}
			}
			lfn[demux.getType()]++;
			new File(newfile).delete();
			new File(values[1]).delete();
		}
	} // tovdr==0

	if (options[18]==0) {
		PVAdemuxlist.clear();
		PVAPidlist.clear();
	}

	if (options[31]==1) 
		rawfile.close();

	}  // end try
	catch (EOFException e1) { 
		//DM25072004 081.7 int07 add
		Msg("pva EOF reached in error: " + e1); 
	}
	catch (IOException e2) { 
		//DM25072004 081.7 int07 add
		Msg("pva File I/O error: " + e2); 
	}

	yield();

	return vptslog;


}  /** end methode pva parse **/



/*****************
 * synccheck A/V *
 *****************/
public boolean SyncCheck(double timecount, double timelength, long timeline, int mpf, int v, int w, long[] vptsval, long[] vtime, boolean awrite) {

	if (w < vptsval.length) {
		double ms1 = (double)(timeline-vptsval[w+1]);
		double ms2 = (double)(timecount-vtime[w+1]);
		if (options[30]==1) 
			System.out.println("A "+awrite+"/"+v+"/"+w+"/ =1 "+mpf+"/"+vtime[w+1]+"/"+timecount+" ~2 "+vptsval[w+1]+"/"+timeline+" ~3 "+ms1+"/"+ms2+"/"+(ms2-ms1));

		if ( (double)Math.abs(ms2) <= timelength/2.0 ) {
			awrite=false;
			w+=2;
		} else if ( (double)Math.abs(ms1) <= timelength/2.0 ) {
			awrite=false;
			w+=2;
		}
		if (options[30]==1) 
			System.out.println("B "+awrite+"/"+v+"/"+w);
	}

	if (v < vptsval.length) {
		boolean show=false;
		double ms3 = (double)(timeline-vptsval[v]);
		double ms4 = (double)(timecount-vtime[v]);
		if (options[30]==1) 
			System.out.println("C "+awrite+"/"+v+"/"+w+"/ =4 "+mpf+"/"+vtime[v]+"/"+timecount+" ~5 "+vptsval[v]+"/"+timeline+" ~6 "+ms3+"/"+ms4+"/"+(ms4-ms3));

		if (!awrite && (double)Math.abs(ms3) <= timelength/2.0 ) {
			awrite=true; 
			show=true;
			v+=2;
		} else if (!awrite && (double)Math.abs( (double)Math.abs(ms4) - (double)Math.abs(ms3) ) <= timelength/2.0 ) {
			awrite=true; 
			show=true;
			v+=2;
		}
		if (options[30]==1)
			System.out.println("D "+awrite+"/"+v+"/"+w);

		if (v<vptsval.length && awrite && (timecount+(timelength/2.0)) > vtime[v] ) 
			awrite=false;
		if (options[30]==1) 
			System.out.println("E "+awrite+"/"+v+"/"+w);
		if (show && awrite) 
			msoff.setText(""+(int)(ms3/90)+"/"+(int)(ms4/90)+"/"+(int)((ms4-ms3)/90));
	}

	options[49] = ((0xffffffffL & (long)v)<<32 | (0xffffffffL & (long)w));
	return awrite;
}


/************************************************
 * start method for adjusting audio at timeline *
 ************************************************/
public void mpt(String[] args) {
	long op10 = options[10];
	progress.setString("check & synchronize audio file  "+(new File(args[0])).getName());
	progress.setStringPainted(true);
	progress.setValue(0);
	yield();


	MPADecoder.RESET=false;

	//DM10042004 081.7 int01 add+
	MPADecoder.MAX_VALUE = RButton[2].isSelected() ? (Integer.parseInt(exefield[8].getText().toString()) * 32767 / 100) : 32767;
	MPADecoder.MULTIPLY = RButton[2].isSelected() ? 32767 : 1;
	MPADecoder.NORMALIZE = RButton[2].isSelected() ? true : false;

	if (MPADecoder.MAX_VALUE > 32767)
	{
		MPADecoder.MAX_VALUE = 32767;
		exefield[8].setText("100");
	}
	//DM10042004 081.7 int01 add-

	if (comBox[7].getSelectedIndex() > 0){
		Msg("-> convert MPA :");
		Msg("\t"+comBox[7].getSelectedItem().toString());
	}
	if (cBox[50].isSelected()){
		Msg("-> decode MPA :");
		Msg("\t"+comBox[1].getSelectedItem().toString());
		if (RButton[3].isSelected())
			Msg("\t"+RButton[3].getText().toString());
		if (RButton[4].isSelected())
			Msg("\t"+RButton[4].getText().toString());
		if (RButton[5].isSelected())
			Msg("\t"+RButton[5].getText().toString());
		//DM07022004 081.6 int16 new
		if (RButton[9].isSelected())
			Msg("\t"+RButton[9].getText().toString());
	}

	while ( processAudio(args) ) {
		options[17] &= ~0xCL;
		Msg("\n!> restart audio processing, forced by changes @ output frame "+(options[17]>>>18));
		yield();
		if ( (0x10000L&options[17])!=0) 
			options[10]=0;
	}
	options[17] &= 3L;
	options[10] = op10;
}




/********************************
 *  method for audio processing *
 ********************************/
public boolean processAudio(String[] args) {

	String fchild = (newOutName.equals("")) ? (new File(args[0]).getName()).toString() : newOutName;
	String fparent = ( fchild.lastIndexOf(".") != -1 ) ? workouts+fchild.substring(0,fchild.lastIndexOf(".")) : workouts+fchild;

	msoff.setText("A/V offset");
	audiostatusLabel.setText("not started"); //DM18022004 081.6 int17 changed

	if (options[11]==1 && options[18]>0) 
		fparent+="("+options[19]+")";

	String newnameL = fparent+".mpL";
	String newnameR = fparent+".mpR";

	java.text.DateFormat sms = new java.text.SimpleDateFormat("HH:mm:ss.SSS");
	sms.setTimeZone(java.util.TimeZone.getTimeZone("GMT+0:00"));
 
	boolean ptsdata=false, vptsdata=false;
	boolean slloop=false, awrite=false, preloop=true, newformat=true;
	byte[] header_copy = new byte[4];
	byte[][] newframes = new byte[2][1], copyframe= new byte[2][1];
	byte[][] silent_Frame=new byte[2][0];
	byte[] pushback = new byte[10]; //DM19122003 081.6 int07 changed
	byte[] frame = new byte[1], pushmpa = new byte[4], push24 = new byte[24];
	long[] ptsval = {0}, ptspos = {0}, vptsval = {0}, vtime = {0};
	int cb=0, cc=0, cd=0, ce=0, i=0, pos=0, x=0, layertype=-1;
	int v=0, w=0;
	int frame_counter=0, layer=0, samplerate=0, lastheader=0, padding=0, jss=0;
	int compare=0;
	int[] layermode2 = { 0,0,0,0 };
	long n=0, actframe=0, timeline=0;
	String formatdisplay="";
	double time_counter=0.0;

	options[47]=0;
	options[49]=0;

	try 
	{

	System.gc(); //DM22122003 081.6 int09 new

	if ( !args[1].equals("-1") ) {
		if (options[30]==1) 
			System.out.print("\r-> loading audio PTS logfile...");   

		int logsize = (int)(new File(args[1])).length() / 16;
		DataInputStream bin = new DataInputStream(new BufferedInputStream(new FileInputStream(args[1]),655350));

		ptsval = new long[logsize+1]; 
		ptspos = new long[logsize+1];
		ptsval[logsize] = -1;  
		ptspos[logsize] = -1;
		int aa=0;

		for (int a=0; a<logsize; a++) {
			//DM15112003 081.5++ special4lucious
			long ptsVal = bin.readLong();
			long ptsPos = bin.readLong();

			if (options[30]==1) 
				System.out.println(" #"+aa+"/"+a+" _"+ptsVal+" / "+ptsPos);

			if (aa>0 && ptsVal<=ptsval[aa-1]){
				if (aa>1 && Math.abs(ptsVal-ptsval[aa-2])<150000 && Math.abs(ptsVal-ptsval[aa-1])>500000){
					aa--;
					if (options[30]==1)
						System.out.print(" <!^> ");
				}else{
					if (options[30]==1) 
						System.out.print(" <!v> ");
					continue;
				}
			}

			ptsval[aa] = ptsVal;
			ptspos[aa] = ptsPos;
			aa++;

			/**
			ptsval[a] = bin.readLong();
			ptspos[a] = bin.readLong();

			if ( (a>2) && (Math.abs(ptsval[a]-ptsval[a-2]) < 150000) && Math.abs(ptsval[a]-ptsval[a-1])>2000000 ){
				ptsval[a-1] = ((ptsval[a-2]+ptsval[a])/2); 


				aa++; 
			}
			if (options[30]==1) 
				System.out.println(" #"+a+" _"+ptsval[a]+" #"+(a)+" _"+ptspos[a]);
			**/
		}
		if (aa<logsize){ //DM15112003 081.5++ special4lucious
			Msg("!> "+(logsize-aa)+" PTS's discarded in stream");
			long tmp[][] = new long[2][aa];
			System.arraycopy(ptsval,0,tmp[0],0,aa);
			System.arraycopy(ptspos,0,tmp[1],0,aa);
			ptsval = new long[aa+1];
			System.arraycopy(tmp[0],0,ptsval,0,aa);
			ptsval[aa]= -1;
			ptspos = new long[aa+1];
			System.arraycopy(tmp[1],0,ptspos,0,aa);
			ptspos[aa]= -1;
		}

		/**
		if (aa>0) 
			Msg(" "+aa+" PTS's recalculated in stream"); 
		**/

		yield();
		bin.close();

		if (cBox[24].isSelected()) {
			long[] tmp = { ptsval[0],ptspos[0] };
			ptsval = new long[2]; 
			ptsval[0] = tmp[0]; 
			ptsval[1]= - 1;
			ptspos = new long[2]; 




			ptspos[0] = tmp[1]; 
			ptspos[1]= - 1;
			Msg("-> take only first Audio PTS (to sync the starttime");
		}

		Msg("Audio PTS: first packet "+sms.format(new java.util.Date(ptsval[0]/90))+", last packet "+sms.format(new java.util.Date(ptsval[ptsval.length-2]/90)));
		ptsdata=true;
	}

	if ( !args[3].equals("-1") ) {
		if (options[30]==1) 
			System.out.print("\r-> loading video PTS logfile...");   

		int vlogsize = (int)(new File(args[3])).length() / 16;
		DataInputStream vbin = new DataInputStream(new BufferedInputStream(new FileInputStream(args[3]),655350));

		vptsval = new long[vlogsize]; 
		vtime = new long[vlogsize];
		long vdiff = 0;

		for ( int b=0; b < vlogsize; b+=2 ) {
			vptsval[b] = vbin.readLong(); 
			vptsval[b+1] = vbin.readLong();
			vtime[b] = vbin.readLong();
			vtime[b+1] = vbin.readLong(); 
			if (options[30]==1) 
				System.out.println(" #s"+b+" _"+vptsval[b]+" #e"+(b+1)+" _"+vptsval[b+1]+" /#s"+b+" _"+vtime[b]+" #e"+(b+1)+" _"+vtime[b+1]);
		}

		vbin.close();
		Msg("Video PTS: start 1.GOP "+sms.format(new java.util.Date(vptsval[0]/90))+", end last GOP "+sms.format(new java.util.Date(vptsval[vptsval.length-1]/90)));
		vptsdata=true;
	}

	System.gc(); //DM22122003 081.6 int09 new

	//vptsdata=false;

	PushbackInputStream audioin = new PushbackInputStream(new FileInputStream(args[0]),1000000);
	long audiosize = new File(args[0]).length();
	long[] addf = { 0,0 };

	if ( !(audiosize > 1000) ) 
		Msg(" Filesize < 1000 byte");

	IDDBufferedOutputStream audiooutL = new IDDBufferedOutputStream(new FileOutputStream(newnameL),bs/2);
	IDDBufferedOutputStream audiooutR = new IDDBufferedOutputStream(new FileOutputStream(newnameR));
	if (options[10]>=4)
		audiooutR = new IDDBufferedOutputStream(new FileOutputStream(newnameR),(bs/2));

	ByteArrayOutputStream audbuf = new ByteArrayOutputStream();

	if (cBox[34].isSelected()){
		audiooutL.InitIdd(newnameL,2);
		audiooutR.InitIdd(newnameR,2);
	}

	//DM150702004 081.7 int06 changed, fix
	if (vptsdata && ptsdata)
	{
		int jump = checkPTSMatch(vptsval, ptsval);

		if (jump < 0)
		{
			Msg("-> !! video & audio PTS doesn't match at any time!");  
			vptsdata = false; 
			x = 0; 
		}

		else
			x = jump;

		/**
		pvacut:
		while ( ptsval.length>2 && Math.abs(ptsval[x] - vptsval[0]) > 20000000 ) { //DM21022004 081.6 int18 changed
			x++;
			if (x>=ptsval.length-1) { 
				Msg("-> !! video & audio pts doesn't match at any time!");  
				vptsdata=false; 
				x=0; 
				break pvacut; 
			}
			continue pvacut;
		}
		**/
	}

	if (vptsdata) 
		Msg("-> adjusting audio at video-timeline");

	if (ptsdata && !vptsdata && options[11]==0) 
		Msg("-> adjusting audio at its own timeline");

	yield();

	if (ptsdata) { 
		timeline = ptsval[x]; 
		n = ptspos[x]; 
	}
	if (n>0) 
		audioin.skip(n);

	/*** riff wave header ***/
	RIFFHeader[] riffw = new RIFFHeader[2];
	riffw[0] = new RIFFHeader();  // normal, left
	riffw[1] = new RIFFHeader();  // right

	if (cBox[4].isSelected() && args[2].equals("mp")) {
		if (RButton[15].isSelected()) {
			audiooutL.write(riffw[0].ACMnull());
			if (options[10]>=4) 
				audiooutR.write(riffw[1].ACMnull());
			Msg("-> add RIFF WAVE header to MPEG Audio as ACM Waveformat");
		} else {
			audiooutL.write(riffw[0].BWFnull());
			if (options[10]>=4) 
				audiooutR.write(riffw[1].BWFnull());
			Msg("-> add RIFF WAVE header to MPEG Audio as BWF Waveformat");
		}
	} 
	else if (cBox[12].isSelected() && args[2].equals("ac")) {
		audiooutL.write(riffw[0].AC3null());
		Msg("-> add RIFF WAVE header to AC-3 Audio");
	}

	int pitch[] = { 1, Integer.parseInt(d2vfield[7].getText().toString()) };
	int minSync = 0; //DM15102003 081.5++
	boolean miss=false; //DM04112003 081.5++
	boolean is_DTS=false, is_AC3=false; //DM19122003 081.6 int07 new

	bigloop:
	while (true) {

		/******************************
		*  AC-3/DTS Audio *
		******************************/
		readloopdd:
		//while ( (args[2].equals("ac")) && n < (audiosize-8) )  {
		while ( (args[2].equals("ac")) && n < (audiosize-10) )  {

			/********* progressbar ********/
			progress.setValue((int)(100*n/audiosize)+1); yield();
			if (options[30]==1) 
				System.out.println(" n"+n);

			while (qpause) 
				pause();
			if (qbreak) { 
				qbreak=false; 
				break bigloop; 
			}

			//test 081.5++, shall update x, only if n doesn't point to a synchword and overrun.
			if (ptspos[x+1]!=-1 && n>ptspos[x+1]){
				Msg("!> PTS without a frame ("+ptspos[x+1]+"/"+n+")");
				x++;
			}

			/********* read 10 bytes for headercheck ********/
			//R_One18122003 081.6 int07 changed
			audioin.read(pushback,0,10);
			n+=10;

			/*** parse header ********/
			//DM19122003 081.6 int07 changed
			ERRORCODE = (is_AC3 || !is_DTS) ? Audio.AC3_parseHeader(pushback,0) : 0; 
			if (ERRORCODE < 1) { 
				if (!is_AC3 || is_DTS)
					ERRORCODE = Audio.DTS_parseHeader(pushback,0); 
				if (ERRORCODE < 1) { 
					audioin.unread(pushback,1,9); 
					if (!cBox[3].isSelected() && !miss) //DM04112003 081.5++ info 
						Msg("!> missing syncword @ "+(n-10)+", @ "+sms.format(new java.util.Date((long)(time_counter/90.0f)))); //DM07022004 081.6 int16 changed
					miss=true; 
					n-=9; 
					continue readloopdd; 
				} 
				is_DTS=true; 
				is_AC3=false;
			} else { 
				is_DTS=false;
				is_AC3=true;
			}

			/********* prepare fo read entire frame ********/
			//R_One18122003 081.6 int07 changed
			audioin.unread(pushback);
			n-=10;

			/********* read entire frame ********/
			frame = new byte[Audio.Size];
			audioin.read(frame,0,Audio.Size);

			/********* startfileposition of current frame ********/
			actframe = n;

			/********* expected position for following frame ********/
			n += Audio.Size;

			if (cBox[51].isSelected()){  // skip a frame
				if (pitch[1]*pitch[0]==frame_counter){
					Msg("-> discard frame# "+frame_counter+" ("+pitch[0]+")");
					pitch[0]++;
					continue readloopdd;
				}
			}

			/********* finish loop if last frame in file is shorter than nominal size ********/
			if ( n > audiosize ) 
				break readloopdd; 

			/********* read following frame header, not if it is the last frame ********/
			/********* check following frameheader for valid , if not starting with next byte ********/
			//DM19122003 081.6 int07 changed
			if ( n < audiosize-10 ) {
				int d=0;
				if (!RButton[7].isSelected()){ //DM30122003 081.6 int10 new
					audioin.read(push24,0,24);
					miniloop:
					for (; d<(is_DTS?15:17); d++) { //smpte
						ERRORCODE = is_DTS ? Audio.DTS_parseNextHeader(push24,d) : Audio.AC3_parseNextHeader(push24,d);
						if (ERRORCODE>0) 
							break miniloop; 
					} 
					audioin.unread(push24); 
				}

				if (ERRORCODE<1) { 
					audioin.unread(frame,1,frame.length-1);
					n = actframe+1; 
					continue readloopdd; 
				} else {
					layertype = is_DTS ? 4 : 0;
					audioin.skip(d);
					n+=d;
				}
			}

			//DM10042004 081.7 int01
			if (is_AC3 && !is_DTS && RButton[16].isSelected() && (ERRORCODE = CRC.checkCRC16ofAC3(frame, 2, Audio.Size)) != 0 )
			{
				Msg("!> CRC" + ERRORCODE + " check failed @ " + actframe);
				audioin.unread(frame, 2, frame.length - 2);
				n = actframe + 2;
				continue readloopdd; 
			}

			if (!cBox[3].isSelected() && miss) //DM04112003 081.5++ info
				Msg("!> found syncword @ "+actframe);
			miss=false;

			/********* check for change in frametype ********/ 
			//R_One18122003 081.6 int07 changed
			if (is_DTS) 
				compare = Audio.DTS_compareHeader(); 
			else 
				compare = Audio.AC3_compareHeader(); 
			if ( compare > 0 ) 
				newformat=true; 

			if (frame_counter==0) 
				newformat=true;

			Audio.saveHeader();

			/**** replace not 3/2 with silence ac3 3/2 061i++ ****/
			//R_One18122003 081.6 int07 changed
			if (!is_DTS && cBox[10].isSelected() && Audio.Mode != 7 ) {
				for (int c=0;c<AC3list.size();c++) {
					byte[] ac3data = (byte[])AC3list.get(c);
					if ( ((0xE0 & ac3data[6])>>>5) != 7 ) 
						continue;
					frame = ac3data;
					break;
				}
			}

			// timeline ist hier aktuelle audiopts

			/****** preloop if audio starts later than video, and i must insert *****/
			//if ( !( preloop && vptsdata && vptsval[v] < timeline-(Audio.Time_length/2.0) ) ) preloop=false;
			if ( (preloop && v>=vptsval.length) || !( preloop && vptsdata && vptsval[v] < timeline-(Audio.Time_length/2.0) ) ) 
				preloop=false;
			else {

				/****** patch ac-3 to 3/2 *****/
				//DM19122003 081.6 int07 changed
				if (!is_DTS && options[29]==1 && frame_counter==0 )  
					frame[6] = (byte)((0xF&frame[6]) | 0xE0);
				else if (!is_DTS && options[29]==2) 
					frame[6] = (byte)((0xF&frame[6]) | 0xE0);

				long precount=vptsval[v];
				long[] ins = { (long)time_counter,0 };
				audbuf.reset();

				/**** insert silence ac3 061g++ ****/
				//DM19122003 081.6 int07 changed
				if (!is_DTS && options[16]==0) {
					for (int c=0;c<AC3list.size();c++) {
						byte[] ac3data = (byte[])AC3list.get(c);
						if ( (0xFE&ac3data[4])!=(0xFE&frame[4]) || ( (7&ac3data[5]) != (7&frame[5]) ) || (0xE0&ac3data[6])!=(0xE0&frame[6]) ) 
							continue;
						audbuf.write(ac3data);
						break;
					}
				} else 
					audbuf.write(frame);

				while ( precount < timeline - (Audio.Time_length/2.0) ) {

					/****** check if frame write should paused *****/
					if (vptsdata && w<vptsval.length) { 
						double ms1 = (double)(precount-vptsval[w+1]);
						double ms2 = (double)(time_counter-vtime[w+1]);
						if ( (double)Math.abs(ms2) <= Audio.Time_length/2.0 ) {
							awrite=false;
							w+=2;
						}
						else if ( (double)Math.abs(ms1) <= Audio.Time_length/2.0 ) {
							awrite=false;
							w+=2;
						}
					}

					/****** calculate A/V Offset for true *****/
					if (vptsdata && (v < vptsval.length)) {
						double ms3 = precount-vptsval[v], ms4 = time_counter-vtime[v];
						if (options[30]==1) 
							System.out.println(" ö"+ms3+"/"+ms4+"/"+(ms4-ms3));

						if (!awrite && (double)Math.abs((time_counter-vtime[v]) - (precount-vptsval[v])) <= (double)Audio.Time_length/2.0 ) {
							awrite=true;
							v+=2;
							double ms1 = precount-vptsval[v-2], ms2 = time_counter-vtime[v-2];
							msoff.setText(""+(int)(ms1/90)+"/"+(int)(ms2/90)+"/"+(int)((ms2-ms1)/90));
							if (options[30]==1) 
								System.out.println(" ä"+ms1+"/"+ms2+"/"+(ms2-ms1));
						}
					} 

					/****** calculate A/V Offset for true *****/
					if ((v < vptsval.length) ) {
						if ( (double)Math.abs(vptsval[v] - precount) <= ((double)Audio.Time_length/2.0) )  {
							awrite=true;
							v+=2;
							double ms1 = precount-vptsval[v-2], ms2 = time_counter-vtime[v-2];
							msoff.setText(""+(int)(ms1/90)+"/"+(int)(ms2/90)+"/"+(int)((ms2-ms1)/90));
							if (options[30]==1) 
								System.out.println(" ü"+ms1+"/"+ms2+"/"+(ms2-ms1));
						}

						/****** calculate A/V Offset for false *****/
						if (awrite && (double)Math.abs((time_counter-vtime[v-2]) - (precount-vptsval[v-2])) > (double)Audio.Time_length/2.0 ) {
							awrite=false;
							v-=2;
						}
					}

					/****** write message *****/
					if (awrite || !vptsdata)
						audiostatusLabel.setText("pre-inserting..."); //DM18022004 081.6 int17 changed
					else
						audiostatusLabel.setText("paused..."); //DM18022004 081.6 int17 changed

					/****** stop if no more audio needed *****/
					if (precount > vptsval[vptsval.length-1]+10000) {
						progress.setValue(100);
						break readloopdd;
					}

					if (awrite) {
						audbuf.writeTo(audiooutL);

						/*********** RIFF *****/
						if (cBox[12].isSelected()) 
							riffw[0].AC3RiffData(afc.AC3RiffFormat(frame)); 
						frame_counter++;
						cb++;
						ins[1]++;
						time_counter+=Audio.Time_length;
					}

					precount+=Audio.Time_length;
					if (options[30]==1) 
						System.out.println("(6)audio frames: wri/pre/skip/ins/add "+frame_counter+"/"+cb+"/"+ce+"/"+cc+"/"+cd+"  @ "+sms.format( new java.util.Date((long)(time_counter/90.0)) )+"  ");

				} // end while

				n=actframe;
				audioin.unread(frame);

				if (ins[1]>0)  //DM17012004 081.6 int11 changed
					Msg("-> "+ins[1]+" frame(s) ("+FramesToTime((int)ins[1],Audio.Time_length)+"ms) pre-inserted @ "+sms.format(new java.util.Date(ins[0]/90L)));

				continue readloopdd;

			} // end if preloop


			/****** check if frame write should paused *****/
			if (vptsdata) { 
				awrite = SyncCheck(time_counter,Audio.Time_length,timeline,frame_counter,v,w,vptsval,vtime,awrite);
				v = (int)(options[49]>>>32);
				w = (int)(0xffffffffL & options[49]);
			}
			//System.out.println(""+awrite+"/"+v+"/"+w);

			/****** message *****/
			if (awrite || !vptsdata) 
				audiostatusLabel.setText("writing..."); //DM18022004 081.6 int17 changed
			else 
				audiostatusLabel.setText("paused..."); //DM18022004 081.6 int17 changed

			/****** message *****/
			if (options[30]==1) 
				System.out.println(" k)"+timeline+" l)"+(Audio.Time_length/2.0)+" u)"+Audio.Size+" m)"+awrite+" n)"+w+" o)"+v+" p)"+n);

			/****** stop if no more audio needed *****/
			if (vptsdata && timeline > vptsval[vptsval.length-1]+10000) {
				progress.setValue(100);

				break readloopdd;
			}

			/****** message frameformat *****/
			//DM19122003 081.6 int07 changed
			if ((newformat && awrite) || (newformat && !vptsdata)) {
				String hdr = is_DTS ? Audio.DTS_displayHeader() : Audio.AC3_displayHeader();
				if (options[47]<100) 
					Msg("=> src_audio: "+hdr+" @ "+sms.format(new java.util.Date((long)(time_counter/90.0f))));
				else if (options[47]==100) 
					Msg("=> src_audio: stop displaying, more than 100 audio mode changes in one file reduce work speed");
				else if (options[30]==1) 
					System.out.println("=> src_audio: "+hdr+" @ "+sms.format(new java.util.Date((long)(time_counter/90.0f))));
				options[47]++;
				yield();
				newformat=false;
			}

			/****** patch ac-3 to 3/2 *****/
			//DM19122003 081.6 int07 changed
			if (!is_DTS && options[29]==1 && frame_counter==0 )  
				frame[6] = (byte)((0xF&frame[6]) | 0xE0);
			else if (!is_DTS && options[29]==2) 
				frame[6] = (byte)((0xF&frame[6]) | 0xE0);

			/****** message *****/
			if (options[30]==1) 
				System.out.println("(7)audio frames: wri/pre/skip/ins/add "+frame_counter+"/"+cb+"/"+ce+"/"+cc+"/"+cd+"  @ "+sms.format( new java.util.Date((long)(time_counter/90.0f)) ));
			if (options[30]==1) 
				System.out.println(" x"+((x<ptspos.length-1)?x+"/"+ptsval[x+1]+"/"+ptspos[x+1]:"-"));

			/****** pts for next frame!! ****/
			timeline += Audio.Time_length;

			audbuf.reset();
			audbuf.write(frame);

			//test 081.5++ simple sync: only if ptspos[x+1] equals n (on frame boundary)
			if (cBox[15].isSelected() && ptspos[x+1]!=-1 && ptspos[x+1]<n){
				if (options[30]==1)
					System.out.println(" minSync "+minSync+"/ "+x);
				if ( (++minSync) <20)
					x++;
				else
					minSync=0;
			}

			if ( (ptspos[x+1] == -1) || (ptspos[x+1] > n ) ) {
				if (!vptsdata || (vptsdata && awrite)) {
					audiooutL.write(frame);

					/*********** RIFF *****/
					if (cBox[12].isSelected()) 
						riffw[0].AC3RiffData(afc.AC3RiffFormat(frame)); 

					frame_counter++;
					time_counter+=Audio.Time_length;
				}
				continue readloopdd;

			}

			minSync=0; //DM151003 test 081.5++ simple sync

			if ( (double)Math.abs( ptsval[x+1] - timeline) < (double)Audio.Time_length/2.0 ) {
				timeline=ptsval[x+1];
				x++;
				if (!vptsdata || (vptsdata && awrite)) {
					audiooutL.write(frame);

					/*********** RIFF *****/
					if (cBox[12].isSelected()) 
						riffw[0].AC3RiffData(afc.AC3RiffFormat(frame)); 

					frame_counter++;
					time_counter+=Audio.Time_length;
				}
				continue readloopdd;
			}

			if ( ptsval[x+1] > timeline ) 
				slloop=true;

			/***** neu 033 ******/
			if ( ptsval[x+1] < timeline) {
				x++;
				timeline=ptsval[x];
				Msg("-> skipped sourceframe(s) @ "+sms.format(new java.util.Date((long)time_counter/90L)));
				ce++;
			}

			if (slloop) {

				/**** test , write the actual frame and then loop to fill*****/
				if (!vptsdata || (vptsdata && awrite)) {
					audiooutL.write(frame);


					/*********** RIFF *****/
					if (cBox[12].isSelected()) 
						riffw[0].AC3RiffData(afc.AC3RiffFormat(frame)); 

					frame_counter++;
					time_counter+=Audio.Time_length;
					if (options[30]==1) 
						System.out.println("(10)audio frames: wri/pre/skip/ins/add "+frame_counter+"/"+cb+"/"+ce+"/"+cc+"/"+cd+"  @ "+sms.format( new java.util.Date((long)(time_counter/90.0)) )+"  ");
				}
				timeline+=Audio.Time_length;

				/**** test *****/
				/**** insert silence ac3 061g++ ****/
				//R_One18122003 081.6 int07 changed
				if (!is_DTS && options[16]==0) {
					for (int c=0;c<AC3list.size();c++) {
						byte[] ac3data = (byte[])AC3list.get(c);
						if ( (0xFE&ac3data[4])!=(0xFE&frame[4]) || ( (7&ac3data[5]) != (7&frame[5]) ) || (0xE0&ac3data[6])!=(0xE0&frame[6]) ) 
							continue;
						audbuf.reset();
						audbuf.write(ac3data);
						break;
					}
				}

				long[] ins = { (long)time_counter,0 };

				while ( ptsval[x+1] > (timeline-(Audio.Time_length/2.0)) )  {
					if (vptsdata && w<vptsval.length) { 
						double ms1 = (double)(timeline-Audio.Time_length-vptsval[w+1]);
						double ms2 = (double)(time_counter-vtime[w+1]);
						if ( (double)Math.abs(ms2) <= Audio.Time_length/2.0 ) {
							awrite=false;
							w+=2;
						}
						else if ( (double)Math.abs(ms1) <= Audio.Time_length/2.0 ) {
							awrite=false;
							w+=2;
						}
					}

					// neu 047  
					if (vptsdata && (v < vptsval.length)) {
						if (!awrite && (double)Math.abs((time_counter - vtime[v]) -
								(timeline-Audio.Time_length-vptsval[v]) ) <= (double)Audio.Time_length/2.0 ) {
							double ms1 = (double)(timeline-Audio.Time_length-vptsval[v]);
							double ms2 = (double)(time_counter-vtime[v]);
							msoff.setText(""+(int)(ms1/90)+"/"+(int)(ms2/90)+"/"+(int)((ms2-ms1)/90));
							if (options[30]==1) 
								System.out.println(" §"+ms1+"/"+ms2+"/"+(ms2-ms1));
							awrite=true;
							v+=2;
						}
					} 

					if (vptsdata && (v < vptsval.length)) {
						if ( (double)Math.abs(vptsval[v] - (timeline-Audio.Time_length)) <= ((double)Audio.Time_length/2.0) )  {
							double ms1 = (double)(timeline-Audio.Time_length-vptsval[v]), ms2 = (double)(time_counter-vtime[v]);
							msoff.setText(""+(int)(ms1/90)+"/"+(int)(ms2/90)+"/"+(int)((ms2-ms1)/90));
							if (options[30]==1) 
								System.out.println(" ß"+ms1+"/"+ms2+"/"+(ms2-ms1));
							awrite=true;
							v+=2;
						}

						//neu 047
						if (awrite && (double)Math.abs((time_counter - vtime[v-2]) -
								(timeline-Audio.Time_length-vptsval[v-2]) ) > (double)Audio.Time_length/2.0 ) {
							awrite=false;
							v-=2;
						}
					}

					if (awrite || !vptsdata) 
						audiostatusLabel.setText("inserting..."); //DM18022004 081.6 int17 changed
					else 
						audiostatusLabel.setText("paused..."); //DM18022004 081.6 int17 changed

					if (!vptsdata || (vptsdata && awrite)) {
						audbuf.writeTo(audiooutL);



						/*********** RIFF *****/
						if (cBox[12].isSelected()) 
							riffw[0].AC3RiffData(afc.AC3RiffFormat(audbuf.toByteArray())); 

						frame_counter++;
						time_counter+=Audio.Time_length;
						cc++;
						ins[1]++;
					}

					if (options[30]==1) {
						System.out.println("(8)audio frames: wri/pre/skip/ins/add "+frame_counter+"/"+cb+"/"+ce+"/"+cc+"/"+cd+"  @ "+sms.format( new java.util.Date((long)(time_counter/90.0f)) )+" ");
						System.out.println(" t)"+timeline);
						System.out.println(" x"+((x<ptspos.length-1)?x+"/"+ptsval[x+1]+"/"+ptspos[x+1]:"-"));
					}

					timeline += Audio.Time_length;
				} // end while

				timeline -= Audio.Time_length;
				slloop=false;
				x++;

				if (ins[1]>0)  //DM17012004 081.6 int11 changed
					Msg("-> "+ins[1]+" frame(s) ("+FramesToTime((int)ins[1],Audio.Time_length)+"ms) inserted @ "+sms.format(new java.util.Date(ins[0]/90L)));

				/*** reset PTS after inserting 081.5a ***/
				timeline=ptsval[x];

				continue readloopdd;
			} // end if slloop

			if ( (actframe + Audio.Size) >= audiosize ) 
				break readloopdd;

		}  // end while

		/*** add frames at the end ***/
		if ( args[2].equals("ac") && options[38]==1 && vptsdata && awrite && (w < vptsval.length)) {
			timeline+=Audio.Time_length;
			addf[0] = (long)time_counter;

			/**** insert silence ac3 061g++ ****/
			//DM19122003 081.6 int07 changed
			if (!is_DTS && options[16]==0) {
				for (int c=0;c<AC3list.size();c++) {
					byte[] ac3data = (byte[])AC3list.get(c);
					if ( (0xFE&ac3data[4])!=(0xFE&frame[4]) || ( (7&ac3data[5]) != (7&frame[5]) ) || (0xE0&ac3data[6])!=(0xE0&frame[6]) ) 
						continue;
					audbuf.reset();
					audbuf.write(ac3data);
					break;
				}
			}

			while ( w < vptsval.length ) {
				while ( vtime[w+1] > time_counter && (double)Math.abs(vtime[w+1]-time_counter) > (double)Audio.Time_length/2.0 )  {
					audbuf.writeTo(audiooutL);

					/*********** RIFF *****/
					if (cBox[12].isSelected()) 
						riffw[0].AC3RiffData(afc.AC3RiffFormat(audbuf.toByteArray())); 

					audiostatusLabel.setText("adding..."); //DM18022004 081.6 int17 changed
					frame_counter++;
					time_counter+=Audio.Time_length;
					timeline+=Audio.Time_length;
					cd++;
					addf[1]++;
					if (options[30]==1) { 
						System.out.println("(9)audio frames: wri/pre/skip/ins/add "+frame_counter+"/"+cb+"/"+ce+"/"+cc+"/"+cd+"  @ "+sms.format( new java.util.Date((long)(time_counter/90.0f)) ));
						System.out.print(" t)"+(long)(timeline-Audio.Time_length)+" w)"+w);
					}
				}
				w+=2;
			}
			w-=2;
			timeline -= Audio.Time_length;
			if (options[30]==1) 
				System.out.println(" eot_video:"+(vptsval[w+1]/90)+"ms, eot_audio:"+((timeline)/90)+"ms                  ");
		}


		/***** init FFT/window for mpa decoding for 1 file****/
		if (args[2].equals("mp") && cBox[50].isSelected())
		{
			MPADecoder.init_work(comBox[1].getSelectedIndex());
			MPADecoder.DOWNMIX = (RButton[3].isSelected()) ? true : false;
			MPADecoder.MONO = (RButton[3].isSelected() || options[10]==4) ? true : false;
			MPADecoder.MOTOROLA = (RButton[4].isSelected()) ? true : false;
			MPADecoder.WAVE = (RButton[5].isSelected()) ? true : false;
			if (MPADecoder.WAVE)
			{
				audiooutL.write(MPADecoder.RIFF);
				if (options[10]>=4) 
					audiooutR.write(MPADecoder.RIFF);
			}
			//DM07022004 081.6 int16 new
			else if (RButton[9].isSelected()) //AIFF
			{
				audiooutL.write(MPADecoder.AIFF);
				if (options[10]>=4) 
					audiooutR.write(MPADecoder.AIFF);
			}
		}

		/******************************
		 *  MPEG1+2 Audio Layer 1,2,3 *
		 ******************************/
		 readloop:
		 while ( (args[2].equals("mp")) && n < (audiosize-4) ) {

			/********* progressbar ********/
			progress.setValue((int)(100*n/audiosize)+1);
			yield();
			if (options[30]==1) 
				System.out.println(" n"+n);

			while (qpause) 
				pause();

			if (qbreak) { 
				qbreak=false;
				break bigloop;
			}

			/********* fix VBR & restart processing ********/
			if ((0xCL&options[17]) != 0 || MPADecoder.RESET) { 
				options[17] = (0x3FFFFL&options[17]) | ((long)frame_counter)<<18; 
				return true; 
			}

			//test 081.5++, shall update x, only if n doesn't point to a synchword and overrun.
			if (ptspos[x+1]!=-1 && n>ptspos[x+1]){
				Msg("!> PTS without a frame ("+ptspos[x+1]+"/"+n+")");
				x++;
			}

			/********* read 4 bytes for headercheck ********/
			audioin.read(pushmpa,0,4);
			n+=4;

			/*** parse header ********/
			if ( (ERRORCODE = Audio.MPA_parseHeader(pushmpa,0)) < 1) {
				audioin.unread(pushmpa,1,3);
				if (!cBox[3].isSelected() && !miss) //DM04112003 081.5++ info
					Msg("!> missing syncword @ "+(n-4)+", @ "+sms.format(new java.util.Date((long)(time_counter/90.0f)))); //DM07022004 081.6 int16 changed
				miss=true;
				n-=3;
				continue readloop;
			}

			/********* prepare fo read entire frame ********/
			audioin.unread(pushmpa);
			n-=4;

			/********* read entire frame ********/





			frame = new byte[Audio.Size];
			audioin.read(frame,0,frame.length);
			System.arraycopy(frame,0,header_copy,0,4);
			header_copy[3] &= 0xCF;
			header_copy[2] &= ~2;

			/********* startfileposition of current frame ********/
			actframe = n;

			/********* expected position for following frame ********/
			n += Audio.Size;

			/********* pitch ********/
			if (cBox[51].isSelected()){  // skip a frame
				if (pitch[1]*pitch[0]==frame_counter){
					Msg("-> discard frame# "+frame_counter+" ("+pitch[0]+")");
					pitch[0]++;
					continue readloop;
				}
			}

			/********* save current frame for copying, delete crc if nec. ********/
			if (options[16]==1) {
				copyframe[0] = new byte[frame.length];
				System.arraycopy(frame,0,copyframe[0],0,frame.length);
				if (Audio.Layer>1 && options[9]==1) 
					copyframe[0] = Audio.MPA_deleteCRC(copyframe[0]);
			}

			/********* finish loop if last frame in file is shorter than nominal size ********/
			if ( n > audiosize ) 
				break readloop; 

			/********* read following frame header, not if it is the last frame ********/
			/********* check following frameheader for valid mpegaudio, if not starting with next byte ********/
			if ( n < audiosize-4 ) {
				if (!RButton[7].isSelected()){ //DM30122003 081.6 int10 new
					audioin.read(pushmpa,0,4);
					ERRORCODE = Audio.MPA_parseNextHeader(pushmpa,0);
					audioin.unread(pushmpa);
					if (ERRORCODE<1) {
						audioin.unread(frame,1,frame.length-1);
						n = actframe+1;
						continue readloop;
					}
				}
				layertype=Audio.Layer;
			}

			//DM10042004 081.7 int01
			if (RButton[16].isSelected() && (ERRORCODE = CRC.checkCRC16ofMPA(Audio, frame)) != 0 )
			{
				Msg("!> CRC check failed @ " + actframe);
				audioin.unread(frame, 2, frame.length - 2);
				n = actframe + 2;
				continue readloop;
			}

			if (!cBox[3].isSelected() && miss) //DM04112003 081.5++ info
				Msg("!> found syncword @ "+actframe);
			miss=false;

			/********* check for change in frametype ********/
			if ( (compare = Audio.MPA_compareHeader()) > 0 ) {
				newformat=true;
				if (compare==6) {
					jss++;
					newformat=false;
				}
			}
			if (frame_counter==0) 
				newformat=true;

			Audio.saveHeader();

			// timeline ist hier aktuelle audiopts

			/****** message *****/
			if (options[30]==1) 
				System.out.println(" k)"+timeline+" l)"+(Audio.Time_length/2.0)+" m)"+awrite+" n)"+w+" o)"+v+" p)"+n);


			/****** preloop if audio starts later than video, and i must insert *****/
			if ( (preloop && vptsdata && v>=vptsval.length) || !( preloop && vptsdata && vptsval[v] < timeline-(Audio.Time_length/2.0) ) ) 
				preloop=false;
			else {

				silent_Frame[0]=new byte[Audio.Size_base];	//silence without padd, std
				silent_Frame[1]=new byte[Audio.Size];		//silence with padd for 22.05, 44.1
				for (int a=0;a<2;a++){
					System.arraycopy(header_copy,0,silent_Frame[a],0,4);	//copy last header data
					silent_Frame[a][1] |= 1;				//mark noCRC
					silent_Frame[a][2] |= (a*2);				//set padding bit
				}
				int padding_counter=1;						//count padding
				long precount=vptsval[v];
				long[] ins = { (long)time_counter,0 };

				while ( precount < timeline- (Audio.Time_length/2.0) ) {  //better for RTS

					/*********** test *059d*****/
					/****** check if frame write should paused *****/
					if (vptsdata && w<vptsval.length) { 
						double ms1 = (double)(precount-vptsval[w+1]);
						double ms2 = (double)(time_counter-vtime[w+1]);
						if ( (double)Math.abs(ms2) <= Audio.Time_length/2.0 ) {
							awrite=false;
							w+=2;
						}
						else if ( (double)Math.abs(ms1) <= Audio.Time_length/2.0 ) {
							awrite=false;
							w+=2;
						}
					}

					/****** calculate A/V Offset for true *****/
					if (vptsdata && (v < vptsval.length)) {
						double ms3 = precount-vptsval[v], ms4 = time_counter-vtime[v];
						if (options[30]==1) 
							System.out.println(" ö"+ms3+"/"+ms4+"/"+(ms4-ms3));

						if (!awrite && (double)Math.abs((time_counter - vtime[v]) -
								(precount - vptsval[v]) ) <= (double)Audio.Time_length/2.0 ) {
							awrite=true;
							v+=2;
							double ms1 = precount-vptsval[v-2], ms2 = time_counter-vtime[v-2];
							msoff.setText(""+(int)(ms1/90)+"/"+(int)(ms2/90)+"/"+(int)((ms2-ms1)/90));
							if (options[30]==1) 
								System.out.println(" ä"+ms1+"/"+ms2+"/"+(ms2-ms1));
						}
					} 

					/****** calculate A/V Offset for true *****/
					if ((v < vptsval.length) ) {
						if ( (double)Math.abs(vptsval[v] - precount) <= (double)Audio.Time_length/2.0 )  {
							awrite=true;
							v+=2;
							double ms1 = precount-vptsval[v-2], ms2 = time_counter-vtime[v-2];
							msoff.setText(""+(int)(ms1/90)+"/"+(int)(ms2/90)+"/"+(int)((ms2-ms1)/90));
							if (options[30]==1) 
								System.out.println(" ü"+ms1+"/"+ms2+"/"+(ms2-ms1));
						}

						/****** calculate A/V Offset for false *****/
						if (awrite && Math.abs((time_counter - vtime[v-2]) -
								(precount - vptsval[v-2]) ) > Audio.Time_length/2.0 ) {
							awrite=false;
							v-=2;
						}
					}

					/****** write message *****/
					if (awrite || !vptsdata) 
						audiostatusLabel.setText("pre-inserting..."); //DM18022004 081.6 int17 changed
					else 
						audiostatusLabel.setText("paused...");  //DM18022004 081.6 int17 changed

					/****** stop if no more audio needed *****/
					if (precount > vptsval[vptsval.length-1]+10000) {
						progress.setValue(100);
						break readloop;
					}

					if (awrite) {
						if (options[16]==1) {		// copy last frame
							if (Audio.Layer>0 && cBox[50].isSelected()) { //DM30122003 081.6 int10 changed
								audiooutL.write(MPADecoder.decodeArray(copyframe[0]));
								if (options[10]>=4) 
									audiooutR.write(MPADecoder.get2ndArray());
							} else if (Audio.Layer>1 && options[10]>0) {
								newframes = MPAConverter.modifyframe(copyframe[0],options); 
								audiooutL.write(newframes[0]); 
								if (options[10]>=4) 
									audiooutR.write(newframes[1]);

								if (cBox[4].isSelected()) {  // *********** RIFF
									riffw[0].RiffData(afc.RiffFormat(newframes[0])); 
									if (options[10]>=4) 
										riffw[1].RiffData(afc.RiffFormat(newframes[1]));
								}
							} else {
								audiooutL.write(copyframe[0]); 

								/******** RIFF *****/
								if (cBox[4].isSelected()) 
									riffw[0].RiffData(afc.RiffFormat(copyframe[0]));
							}
						} else {
							//if (padding_counter==padding) padding_counter=0;	//reset padd count
							//else if (samplerate==0) padding_counter++;		//count padding

							if (Audio.Layer>0 && cBox[50].isSelected()) { //DM30122003 081.6 int10 changed
								audiooutL.write(MPADecoder.decodeArray(silent_Frame[(padding_counter>0)?0:1]));
								if (options[10]>=4) 
									audiooutR.write(MPADecoder.get2ndArray());
							} else if (Audio.Layer>1 && options[10]>0) {
								newframes = MPAConverter.modifyframe(silent_Frame[(padding_counter>0)?0:1],options);
								audiooutL.write(newframes[0]);
								if (options[10]>=4) 
									audiooutR.write(newframes[1]);

								// *********** RIFF
								if (cBox[4].isSelected()) {  
									riffw[0].RiffData(afc.RiffFormat(newframes[0])); 
									if (options[10]>=4) 
										riffw[1].RiffData(afc.RiffFormat(newframes[1]));
								}
							} else { 
								audiooutL.write(silent_Frame[(padding_counter>0)?0:1]);

								/******** RIFF *****/
								if (cBox[4].isSelected()) 
									riffw[0].RiffData(afc.RiffFormat(silent_Frame[(padding_counter>0)?0:1]));
							}
						}
						frame_counter++;
						time_counter+=Audio.Time_length;
						cb++;
						ins[1]++;
					}

					precount += Audio.Time_length;
					if (options[30]==1) 
						System.out.println("(5)audio frames: wri/pre/skip/ins/add "+frame_counter+"/"+cb+"/"+ce+"/"+cc+"/"+cd+"  @ "+sms.format( new java.util.Date((long)(time_counter/90.0f)) ));

				} /** end while **/

				n=actframe;
				audioin.unread(frame);

				if (ins[1]>0)  //DM17012004 081.6 int11 changed
					Msg("-> "+ins[1]+" frame(s) ("+FramesToTime((int)ins[1],Audio.Time_length)+"ms) pre-inserted @ "+sms.format(new java.util.Date(ins[0]/90L)));

				continue readloop;
			} 


			/****** check if frame write should paused *****/
			if (vptsdata) { 
				awrite = SyncCheck(time_counter,Audio.Time_length,timeline,frame_counter,v,w,vptsval,vtime,awrite);
				v = (int)(options[49]>>>32);
				w = (int)(0xffffffffL & options[49]);
			}
			//  System.out.println(""+awrite+"/"+v+"/"+w);

			/****** write message *****/
			if (awrite || !vptsdata) 
				audiostatusLabel.setText("writing..."); //DM18022004 081.6 int17 changed
			else 
				audiostatusLabel.setText("paused..."); //DM18022004 081.6 int17 changed

			/****** stop if no more audio needed *****/
			if (vptsdata && timeline > vptsval[vptsval.length-1]+10000) {
				progress.setValue(100);
				break readloop;
			}

			/****** message frameformat *****/
			if ((newformat && awrite) || (newformat && !vptsdata)) {
				if (options[47]<100) 
					Msg("=> src_audio: "+Audio.MPA_displayHeader()+" @ "+sms.format(new java.util.Date((long)(time_counter/90.0f))));
				else if (options[47]==100) 
					Msg("=> src_audio: stop displaying, more than 100 audio mode changes in one file reduce work speed");
				else if (options[30]==1) 
					System.out.println("=> src_audio: "+Audio.MPA_displayHeader()+" @ "+sms.format(new java.util.Date((long)(time_counter/90.0f))));
				options[47]++;
				yield();
				newformat=false;
			}

			/******** message *****/
			if (options[30]==1) 
				System.out.println("(1)audio frames: wri/pre/skip/ins/add "+frame_counter+"/"+cb+"/"+ce+"/"+cc+"/"+cd+"  @ "+sms.format( new java.util.Date((long)(time_counter/90.0f)) ));
			if (options[30]==1) 
				System.out.println(" x"+((x<ptspos.length-1)?x+"/"+ptsval[x+1]+"/"+ptspos[x+1]:"-"));

			/****** pts for next frame!! ****/
			timeline += Audio.Time_length;

			/****** delete CRC ****/
			if (Audio.Layer>1 && options[9]==1) 
				frame = Audio.MPA_deleteCRC(frame);

			/****** copy frame header ****/
			System.arraycopy(frame,0,header_copy,0,4);
			header_copy[3] &= 0xCF;
			header_copy[2] &= ~2;

			/******** message *****/
			//if (options[30]==1) System.out.print(" tl"+timeline+" /px "+ptsval[x]+" /1_"+ptsval[x+1]+" /p1-tl "+(ptsval[x+1]-timeline)+" /pp1 "+ptspos[x+1]+" /n "+n);

			//test 081.5++ simple sync: only if ptspos[x+1] equals n (on frame boundary)
			if (cBox[15].isSelected() && ptspos[x+1]!=-1 && ptspos[x+1]<n){
				if (options[30]==1)
					System.out.println(" minSync "+minSync+"/ "+x);
				if ( (++minSync) <20)
					x++;
				else
					minSync=0;
			}

			/******** frame is in last pes packet or packet end not yet reached *****/
			if ( (ptspos[x+1] == -1) || (ptspos[x+1] > n )  )  {
				if (vptsdata && !awrite) 
					continue readloop;

				if (Audio.Layer>0 && cBox[50].isSelected()) { //DM30122003 081.6 int10 changed
					audiooutL.write(MPADecoder.decodeArray(frame));
					if (options[10]>=4) 
						audiooutR.write(MPADecoder.get2ndArray());
				} else if (Audio.Layer>1 && options[10]>0) {
					newframes = MPAConverter.modifyframe(frame,options);
					audiooutL.write(newframes[0]);
					if (options[10]>=4) 
						audiooutR.write(newframes[1]);

					/******* RIFF *****/
					if (cBox[4].isSelected()) {  
						riffw[0].RiffData(afc.RiffFormat(newframes[0])); 
						if (options[10]>=4) 
							riffw[1].RiffData(afc.RiffFormat(newframes[1]));
					}
				} else {
	 				audiooutL.write(frame);
					/******* RIFF *****/
					if (cBox[4].isSelected()) 
						riffw[0].RiffData(afc.RiffFormat(frame)); 
				}
				frame_counter++;
				time_counter+=Audio.Time_length;
				continue readloop;
			}

			minSync=0; //DM151003 test 081.5++ simple sync

			/******** frame is on pes packet corner *****/
			if ( (double)Math.abs( ptsval[x+1] - timeline) < (double)Audio.Time_length/2.0 )  {
				timeline=ptsval[x+1];
				x++;

				if (vptsdata && !awrite) 
					continue readloop;

				if (Audio.Layer>0 && cBox[50].isSelected()) { //DM30122003 081.6 int10 changed
					audiooutL.write(MPADecoder.decodeArray(frame));
					if (options[10]>=4) 
						audiooutR.write(MPADecoder.get2ndArray());
				} else if (Audio.Layer>1 && options[10]>0) {
					newframes = MPAConverter.modifyframe(frame,options);
					audiooutL.write(newframes[0]);
					if (options[10]>=4) 
						audiooutR.write(newframes[1]);

					/******* RIFF *****/
					if (cBox[4].isSelected()) {
						riffw[0].RiffData(afc.RiffFormat(newframes[0])); 
						if (options[10]>=4) 
							riffw[1].RiffData(afc.RiffFormat(newframes[1]));
					}
				} else {
					audiooutL.write(frame);
					/******* RIFF *****/
					if (cBox[4].isSelected()) 
						riffw[0].RiffData(afc.RiffFormat(frame)); 
				}
				frame_counter++;
				time_counter += Audio.Time_length;
				continue readloop;
			}

			if ( ptsval[x+1] > timeline ) 
				slloop=true;

			/***** neu 033 ******/
			if ( ptsval[x+1] < timeline) {
				x++;
				timeline=ptsval[x];
				Msg("-> skipped sourceframe(s) @ "+sms.format(new java.util.Date((long)time_counter/90L)));
				ce++;
			}

			if (slloop) {

				silent_Frame[0]=new byte[Audio.Size_base];	//silence without padd, std
				silent_Frame[1]=new byte[Audio.Size];		//silence with padd for 22.05, 44.1
				for (int a=0;a<2;a++){
					System.arraycopy(header_copy,0,silent_Frame[a],0,4);	//copy last header data
					silent_Frame[a][1] |= 1;				//mark noCRC
					silent_Frame[a][2] |= (a*2);				//set padding bit
				}
				int padding_counter=1;						//count padding
				long[] ins = { (long)time_counter,0 };
		
				// solange nächster ptsval minus nächster framebeginn  ist größer der halben framezeit, füge stille ein
				while ( ptsval[x+1] > (timeline-(Audio.Time_length/2.0)) ) {

					if (vptsdata && w<vptsval.length) { 
						/********059d******/
						double ms1 = (double)(timeline-Audio.Time_length-vptsval[w+1]);
						double ms2 = (double)(time_counter-vtime[w+1]);
						if ( (double)Math.abs(ms2) <= Audio.Time_length/2.0 ) {
							awrite=false;
							w+=2;
						} else if ( (double)Math.abs(ms1) <= Audio.Time_length/2.0 ) {
							awrite=false;
							w+=2;
						}
					}

					// neu 047  
					if (vptsdata && (v < vptsval.length)) {
						if (!awrite && (double)Math.abs((time_counter - vtime[v]) -
								(timeline-Audio.Time_length-vptsval[v]) ) <= (double)Audio.Time_length/2.0 ) {
							double ms1 = (double)(timeline-Audio.Time_length-vptsval[v]);
							double ms2 = (double)(time_counter-vtime[v]);
 							msoff.setText(""+(int)(ms1/90)+"/"+(int)(ms2/90)+"/"+(int)((ms2-ms1)/90));
							if (options[30]==1) 
								System.out.println(" §"+ms1+"/"+ms2+"/"+(ms2-ms1));
							awrite=true;
							v+=2;
						}
					} 

					if (vptsdata && (v < vptsval.length)) {
						if ( (double)Math.abs(vptsval[v] - (timeline-Audio.Time_length)) <= ((double)Audio.Time_length/2.0) )  {
							double ms1 = (double)(timeline-Audio.Time_length-vptsval[v]), ms2 = (double)(time_counter-vtime[v]);
							msoff.setText(""+(int)(ms1/90)+"/"+(int)(ms2/90)+"/"+(int)((ms2-ms1)/90));
							if (options[30]==1) 
								System.out.println(" ß"+ms1+"/"+ms2+"/"+(ms2-ms1));
							awrite=true;
							v+=2;
						}

						//neu 047
						if (awrite && (double)Math.abs((time_counter - vtime[v-2]) -
								(timeline-Audio.Time_length-vptsval[v-2]) ) > (double)Audio.Time_length/2.0 ) {
							awrite=false;
							v-=2;
						}
					}

					/******** message *****/
					if (awrite || !vptsdata) 
						audiostatusLabel.setText("inserting..."); //DM18022004 081.6 int17 changed
					else 
						audiostatusLabel.setText("paused..."); //DM18022004 081.6 int17 changed
  
					if (!vptsdata || (vptsdata && awrite)) {
						if (options[16]==1) {


							if (Audio.Layer>0 && cBox[50].isSelected()) { //DM30122003 081.6 int10 changed
								audiooutL.write(MPADecoder.decodeArray(copyframe[0]));
								if (options[10]>=4) 
									audiooutR.write(MPADecoder.get2ndArray());
							} else if (Audio.Layer>1 && options[10]>0) {
								newframes = MPAConverter.modifyframe(copyframe[0],options); 
								audiooutL.write(newframes[0]);
								if (options[10]>=4) 
									audiooutR.write(newframes[1]);

								/*********** RIFF *****/
								if (cBox[4].isSelected()) { 
									riffw[0].RiffData(afc.RiffFormat(newframes[0])); 
									if (options[10]>=4) 
										riffw[1].RiffData(afc.RiffFormat(newframes[1]));
								}
							} else {
								audiooutL.write(copyframe[0]);

								/*********** RIFF *****/
								if (cBox[4].isSelected()) 
									riffw[0].RiffData(afc.RiffFormat(copyframe[0]));
							}
						} else {
							//if (padding_counter==padding) padding_counter=0;	//reset padd count
							//else if (samplerate==0) padding_counter++;		//count padding

							if (Audio.Layer>0 && cBox[50].isSelected()) { //DM30122003 081.6 int10 changed
								audiooutL.write(MPADecoder.decodeArray(silent_Frame[(padding_counter>0)?0:1]));
								if (options[10]>=4) 
									audiooutR.write(MPADecoder.get2ndArray());
							} else if (Audio.Layer>1 && options[10]>0) {
								newframes = MPAConverter.modifyframe(silent_Frame[(padding_counter>0)?0:1],options);
								audiooutL.write(newframes[0]);
								if (options[10]>=4) 
									audiooutR.write(newframes[1]);

								/*********** RIFF *****/
								if (cBox[4].isSelected()) {  
									riffw[0].RiffData(afc.RiffFormat(newframes[0])); 
									if (options[10]>=4) 
										riffw[1].RiffData(afc.RiffFormat(newframes[1]));
								}
							} else {
								audiooutL.write(silent_Frame[(padding_counter>0)?0:1]);

								/******** RIFF *****/
								if (cBox[4].isSelected()) 
									riffw[0].RiffData(afc.RiffFormat(silent_Frame[(padding_counter>0)?0:1])); 
							}
						}
						frame_counter++;
						time_counter+=Audio.Time_length;
						cc++;
						ins[1]++;
					}

					if (options[30]==1) {
						System.out.println("(2)audio frames: wri/pre/skip/ins/add "+frame_counter+"/"+cb+"/"+ce+"/"+cc+"/"+cd+"  @ "+sms.format( new java.util.Date((long)(time_counter/90.0f)) ));
						System.out.print(" t)"+timeline);
						System.out.println(" x"+((x<ptspos.length-1)?x+"/"+ptsval[x+1]+"/"+ptspos[x+1]:"-"));
					}

					timeline += Audio.Time_length;

				} // end while

				timeline -= Audio.Time_length;
				slloop=false;
				x++;
				if (ins[1]>0)  //DM17012004 081.6 int11 changed
					Msg("-> "+ins[1]+" frame(s) ("+FramesToTime((int)ins[1],Audio.Time_length)+"ms) inserted @ "+sms.format(new java.util.Date(ins[0]/90L)));

				/*** reset PTS after inserting 081.5a ***/
				timeline=ptsval[x];

				continue readloop;
			}

			if ( (actframe + Audio.Size) >= audiosize ) 
				break readloop; 

		}  // end while

		if (options[30]==1) 
			System.out.println("(3)audio frames: wri/pre/skip/ins/add "+frame_counter+"/"+cb+"/"+ce+"/"+cc+"/"+cd+"  @ "+sms.format( new java.util.Date((long)(time_counter/90.0f)) ));

		/*** add frames at the end ***/
		if (args[2].equals("mp") && options[38]==1 && vptsdata && awrite && (w < vptsval.length)) {

			timeline += Audio.Time_length;
			addf[0]=(long)time_counter;

			silent_Frame[0]=new byte[Audio.Size_base];	//silence without padd, std
			silent_Frame[1]=new byte[Audio.Size];		//silence with padd for 22.05, 44.1
			for (int a=0;a<2;a++){
				System.arraycopy(header_copy,0,silent_Frame[a],0,4);	//copy last header data
				silent_Frame[a][1] |= 1;				//mark noCRC
				silent_Frame[a][2] |= (a*2);				//set padding bit
			}
			int padding_counter=1;						//count padding

			while ( w < vptsval.length ) {

				while ( vtime[w+1] > time_counter && 
					(double)Math.abs(vtime[w+1]-time_counter) > (double)Audio.Time_length/2.0 )  {

					if (options[16]==1) {				//add_copy prev. frame
						if (Audio.Layer>0 && cBox[50].isSelected()) { //DM30122003 081.6 int10 changed
							audiooutL.write(MPADecoder.decodeArray(copyframe[0]));
							if (options[10]>=4) 
								audiooutR.write(MPADecoder.get2ndArray());
						} else if (Audio.Layer>1 && options[10]>0) {		//modify frame
							newframes = MPAConverter.modifyframe(copyframe[0],options); 
							audiooutL.write(newframes[0]);
							if (options[10]>=4) 
								audiooutR.write(newframes[1]);

							/**** RIFF ****/
							if (cBox[4].isSelected()) { 
								riffw[0].RiffData(afc.RiffFormat(newframes[0])); 
								if (options[10]>=4) 
									riffw[1].RiffData(afc.RiffFormat(newframes[1]));
							}
						} else {
							audiooutL.write(copyframe[0]);

							/**** RIFF ****/
							if (cBox[4].isSelected()) 
								riffw[0].RiffData(afc.RiffFormat(copyframe[0]));
						}
					} else {	//add silence
						//if (padding_counter==padding) padding_counter=0;	//reset padd count
						//else if (samplerate==0) padding_counter++;		//count padding

						if (Audio.Layer>0 && cBox[50].isSelected()) { //DM30122003 081.6 int10 changed
							audiooutL.write(MPADecoder.decodeArray(silent_Frame[(padding_counter>0)?0:1]));
							if (options[10]>=4) 
								audiooutR.write(MPADecoder.get2ndArray());
						} else if (Audio.Layer>1 && options[10]>0) {		//modify frame
							newframes = MPAConverter.modifyframe(silent_Frame[(padding_counter>0)?0:1],options);
							audiooutL.write(newframes[0]);
							if (options[10]>=4) 
								audiooutR.write(newframes[1]);

							/**** RIFF ****/
							if (cBox[4].isSelected()) {
								riffw[0].RiffData(afc.RiffFormat(newframes[0])); 
								if (options[10]>=4) 
									riffw[1].RiffData(afc.RiffFormat(newframes[1]));
							}
						} else {
							audiooutL.write(silent_Frame[(padding_counter>0)?0:1]);

							/**** RIFF ****/
							if (cBox[4].isSelected()) 
								riffw[0].RiffData(afc.RiffFormat(silent_Frame[(padding_counter>0)?0:1]));
						}
					}

					timeline += Audio.Time_length;
					cd++;
					frame_counter++;
					addf[1]++;
					time_counter += Audio.Time_length;
					audiostatusLabel.setText("adding..."); //DM18022004 081.6 int17 changed

					if (options[30]==1) {
						System.out.println("(4)audio frames: wri/pre/skip/ins/add "+frame_counter+"/"+cb+"/"+ce+"/"+cc+"/"+cd+"  @ "+sms.format( new java.util.Date((long)(time_counter/90.0f)) ));
						System.out.print(" t)"+(long)(timeline-Audio.Time_length)+" w)"+w);
					}
				}
				w+=2;
			}
			w-=2;
			timeline -= Audio.Time_length;
			if (options[30]==1) 
				System.out.println(" eot_video:"+(vptsval[w+1]/90)+"ms, eot_audio:"+((timeline)/90)+"ms                             ");

		}  //end add mpa


		//DM31122003 081.6 int11 new+
		/******************************
		*  PCM Audio *
		******************************/
		if (args[2].equals("wa")){

			// parse header
			frame=new byte[1000];
			audioin.read(frame);
			Audio.WAV_parseHeader(frame,0);
			audioin.unread(frame,Audio.Emphasis,1000-Audio.Emphasis);
			Msg("=> src_audio: "+Audio.WAV_saveAnddisplayHeader()+" @ "+sms.format(new java.util.Date((long)(time_counter/90.0f))));
			layertype = 5;

			n = Audio.Emphasis; //start of pcm data
			long pcm_end_pos = Audio.Emphasis + Audio.Size_base; //whole sample data size
			timeline = ptsval[0];

			audiooutL.write(Audio.getRiffHeader());

			long sample_bytes, skip_bytes;
			long sample_pts, skip_pts;
			int sample_size, read_size = 960000 / Audio.Mode;

			// Size/8 * Channel = bytes per sample
			// 16bit/8 * 2  = 4
			// per sample: Audio.Time_length = 90000.0 / Samplefre 
			// 48000hz = 1.875 ticks (of 90khz) 1 frame = 192 samples = 360ticks
			// 44100hz = 2.040816 

			for (int f=0; f < ptsval.length-1; f++)
			{
				for (int a=0; a < vptsval.length; a+=2)
				{
					while (qpause) 
						pause();

					if (qbreak)
					{ 
						qbreak=false;
						break bigloop;
					}

					if (vptsdata && vptsval[a] < timeline)  //jump back (not yet) or insert silent samples
					{
						sample_pts = vptsval[a+1] > timeline ? timeline - vptsval[a] : vptsval[a+1] - vptsval[a];
						sample_bytes = (long)Math.round(1.0 * Audio.Sampling_frequency * sample_pts / 90000.0) * Audio.Mode;

						if (options[30]==1)
							System.out.println("a "+sample_pts+"/"+sample_bytes+"/"+n+"/"+timeline);

						for (long sample_pos = 0; sample_pos < sample_bytes; )
						{
							sample_size = (sample_bytes - sample_pos) >= read_size ? read_size : (int)(sample_bytes - sample_pos);
							frame = new byte[sample_size];
							sample_pos += sample_size;
							audiooutL.write(frame);
						}
						time_counter += sample_pts;
						frame_counter += (sample_bytes / Audio.Mode);

						if (vptsval[a+1] > timeline)
						{
							sample_pts = vptsval[a+1] - timeline;
							sample_bytes = (long)Math.round(1.0 * Audio.Sampling_frequency * sample_pts / 90000.0) * Audio.Mode;

							if (options[30]==1)
								System.out.println("b "+sample_pts+"/"+sample_bytes+"/"+n+"/"+timeline);

							for (long sample_pos = 0; sample_pos < sample_bytes; )
							{
								sample_size = (sample_bytes - sample_pos) >= read_size ? read_size : (int)(sample_bytes - sample_pos);
								frame = new byte[sample_size];
								audioin.read(frame);
								sample_pos += sample_size;
								audiooutL.write(frame);
							}
							n += sample_bytes;
							timeline += sample_pts;
							time_counter += sample_pts;
							frame_counter += (sample_bytes / Audio.Mode);
						}
					}
					else
					{
						skip_pts = vptsdata ? vptsval[a] - timeline : 0;
						skip_bytes = (long)Math.round(1.0 * Audio.Sampling_frequency * skip_pts / 90000.0) * Audio.Mode;

						sample_pts = vptsdata ? vptsval[a+1] - vptsval[a] : (long)(1.0 * (Audio.Size_base / Audio.Mode) / Audio.Sampling_frequency * 90000.0);
						sample_bytes = (long)Math.round(1.0 * Audio.Sampling_frequency * sample_pts / 90000.0) * Audio.Mode;

						for (long skip_pos = 0; skip_pos < skip_bytes; )
							skip_pos += audioin.skip(skip_bytes - skip_pos);
						n += skip_bytes;

						if (options[30]==1)
							System.out.println("c "+skip_pts+"/"+skip_bytes+"/"+sample_pts+"/"+sample_bytes+"/"+n+"/"+timeline);

						for (long sample_pos = 0; sample_pos < sample_bytes; )
						{
							sample_size = (sample_bytes - sample_pos) >= read_size ? read_size : (int)(sample_bytes - sample_pos);
							frame = new byte[sample_size];
							audioin.read(frame);
							sample_pos += sample_size;
							audiooutL.write(frame);
						}
						n += sample_bytes;
						timeline += (skip_pts + sample_pts);
						time_counter += sample_pts;
						frame_counter += (sample_bytes / Audio.Mode);
					}
					if (options[30]==1)
						System.out.println("(4w)audio frames: wri/pre/skip/ins/add "+frame_counter+"/"+cb+"/"+ce+"/"+cc+"/"+cd+"  @ "+sms.format( new java.util.Date((long)(time_counter/90.0f)) ));

					/********* progressbar ********/
					progress.setValue((int)(100*n/audiosize)+1);
					yield();
					if (options[30]==1) 
						System.out.println(" n"+n);

				}
				break;
			}
		}
		//DM31122003 081.6 int11 new-

		break;

	} // end while bigloop


	if (addf[1]>0) //DM17012004 081.6 int11 changed
		Msg("-> "+addf[1]+" frame(s) ("+FramesToTime((int)addf[1],Audio.Time_length)+"ms) added @ "+sms.format(new java.util.Date(addf[0]/90L)));

	audiostatusLabel.setText("finished..."); //DM18022004 081.6 int17 changed

	String tc = sms.format( new java.util.Date((long)(time_counter/90.0f)) );
	Msg("audio frames: wri/pre/skip/ins/add "+frame_counter+"/"+cb+"/"+ce+"/"+cc+"/"+cd+"  @ "+tc+" done..");

	if (jss>0) 
		Msg("-> "+jss+" stereo/jointstereo change(s) detected...");

	audioin.close(); 
	audbuf.close();
	audiooutL.flush(); 
	audiooutL.close();
	audiooutR.flush(); 
	audiooutR.close();

	//R_One18122003 081.6 int07 changed
	String[][] pureaudio = {
		{ ".ac3",".mp1",".mp2",".mp3",".dts" },
		{ "_0.ac3","_0.mp1","_0.mp2","_0.mp3","_0.dts" }
	};

	if (options[35]==1) {
		for (int g=1; g<4; g++) {
			pureaudio[0][g]=".mpa";
			pureaudio[1][g]="_0.mpa";
		}
	}

	if (cBox[50].isSelected() && Audio.Layer>1) {
		if (MPADecoder.WAVE)
			for (int g=1; g<4; g++)
			{
				pureaudio[0][g]+=".wav";
				pureaudio[1][g]+=".wav";
			}
		//DM07022004 081.6 int16 new
		else if (RButton[9].isSelected())
			for (int g=1; g<4; g++)
			{
				pureaudio[0][g]+=".aif";
				pureaudio[1][g]+=".aif";
			}
		else  
			for (int g=1; g<4; g++)
			{
				pureaudio[0][g]+=".pcm";
				pureaudio[1][g]+=".pcm";
			}

	}
	else if (cBox[4].isSelected()) {
		for (int g=1; g<4; g++) {
			pureaudio[0][g]+=".wav";
			pureaudio[1][g]+=".wav";
		}
	}

	if (cBox[12].isSelected()) {
		pureaudio[0][0]+=".wav";
		pureaudio[1][0]+=".wav";
	}

	File ac3name = new File (fparent+pureaudio[(int)options[11]][0]);
	File mp1name = new File (fparent+pureaudio[(int)options[11]][1]);
	File mp2name = new File (fparent+pureaudio[(int)options[11]][2]);
	File mp3name = new File (fparent+pureaudio[(int)options[11]][3]);
	File mp2nameL = new File (fparent+"_L"+pureaudio[0][2]);
	File mp2nameR = new File (fparent+"_R"+pureaudio[0][2]);
	//R_One18122003 081.6 int07 new
	File dtsname = new File (fparent+pureaudio[(int)options[11]][4]);
	//DM24012004 081.6 int11 add
	File wavname = new File (fparent+"_0.wav");

	/*** make riff ***/
	if (cBox[50].isSelected() && args[2].equals("mp") && MPADecoder.WAVE)
	{
		if (Audio.Layer>1)
		{
			MPADecoder.fillRIFF(newnameL);
			if (options[10]>=4) 
				MPADecoder.fillRIFF(newnameR);
		}
		else
		{
			MPADecoder.deleteRIFF(newnameL);
			if (options[10]>=4) 
				MPADecoder.deleteRIFF(newnameR);
		}
	}
	//DM07022004 081.6 int16 new
	else if (cBox[50].isSelected() && args[2].equals("mp") && RButton[9].isSelected())
	{
		if (Audio.Layer>1)
		{
			MPADecoder.fillAiff(newnameL,(long)(time_counter/90.0f));
			if (options[10]>=4) 
				MPADecoder.fillAiff(newnameR,(long)(time_counter/90.0f));
		}
		else
		{
			MPADecoder.deleteAiff(newnameL);
			if (options[10]>=4) 
				MPADecoder.deleteAiff(newnameR);
		}
	}
	else if (cBox[4].isSelected() && args[2].equals("mp")) {
		RandomAccessFile[] rifffile = { new RandomAccessFile(newnameL,"rw"), new RandomAccessFile(newnameR,"rw") };
		riffw[0].Length( rifffile[0].length() , (long)(time_counter/90.0f) ); 
		riffw[1].Length( rifffile[1].length() , (long)(time_counter/90.0f) );
		rifffile[0].seek(0); 
		rifffile[1].seek(0);
		if (RButton[15].isSelected()) {
			rifffile[0].write(riffw[0].ACM());
			if (options[10]>=4) 
				rifffile[1].write(riffw[1].ACM());
		} else {
			rifffile[0].write(riffw[0].BWF());
			if (options[10]>=4) 
				rifffile[1].write(riffw[1].BWF());
		}
		rifffile[0].close();
		rifffile[1].close();
	
	}
	else if (cBox[12].isSelected() && args[2].equals("ac")) //DM19122003 081.6 int07 changed
	{
		RandomAccessFile rifffile = new RandomAccessFile(newnameL,"rw");
		riffw[0].Length( rifffile.length() , (long)(time_counter/90.0f) );
		rifffile.seek(0);
		rifffile.write(riffw[0].AC3());
		rifffile.close();
	}
	else if (args[2].equals("wa")) //DM24012004 081.6 int11 add
	{
		Audio.fillRiffHeader(newnameL);
	}


	File audioout1 = new File(newnameL);
	File audioout2 = new File(newnameR);

	options[39]+=audioout1.length();
	options[39]+=audioout2.length();
	showOutSize();

	//DM12042004 081.7 int01 add
	String audio_type[] = {
		"(ac3)", "(mp3)", "(mp2)", "(mp1)", "(dts)", "(pcm)"
	};
	if (cBox[50].isSelected())
		audio_type[1] = audio_type[2] = "(pcm)";

	String comparedata = "";	//DM27042004 081.7 int02 moved, fix

	if (layertype < 0)
		layertype = 10;

	else
		//DM12042004 081.7 int01 changed, //DM27042004 081.7 int02 changed
		//DM15072004 081.7 int06 changed
		comparedata = "Audio " + (NoOfAudio++) + " " + audio_type[layertype] + ":\t" + frame_counter + " Frames\t" + tc + "\t" + infoPTSMatch(args, vptsdata, ptsdata) + cb + "/" + ce + "/" + cc + "/" + cd;

	switch (layertype) { 
	case 0: { 
		if (ac3name.exists())
			ac3name.delete();
		if (audioout1.length()<100) 
			audioout1.delete();
		else { 
			Common.renameTo(audioout1, ac3name); //DM13042004 081.7 int01 changed
			//audioout1.renameTo(ac3name); 

			Msg("===> new File: "+ac3name); 
			InfoAtEnd.add(comparedata+"\t "+ac3name);
		}
		if (audioout2.length()<100) 
			audioout2.delete();

		audiooutL.renameIddTo(ac3name);
		audiooutR.deleteIdd();
		break;
	}
	case 1: { 
		if ( mp3name.exists() ) 
			mp3name.delete();
		if (audioout1.length()<100) 
			audioout1.delete();
		else { 
			Common.renameTo(audioout1, mp3name); //DM13042004 081.7 int01 changed
			//audioout1.renameTo(mp3name); 

			Msg("===> new File: "+mp3name); 
			InfoAtEnd.add(comparedata+"\t "+mp3name);
		}
		if (audioout2.length()<100) 
			audioout2.delete();

		audiooutL.renameIddTo(mp3name);
		audiooutR.deleteIdd();
		break;
	}
	case 2: {
		if (options[10]>=4) {
			if ( mp2nameL.exists() ) 
				mp2nameL.delete();
			if ( mp2nameR.exists() ) 
				mp2nameR.delete();
			if (audioout1.length()<100) 
				audioout1.delete();
			else {
				Common.renameTo(audioout1, mp2nameL); //DM13042004 081.7 int01 changed
				//audioout1.renameTo(mp2nameL); 

				Msg("===> new (left) File: "+mp2nameL); 
				InfoAtEnd.add(comparedata+"\t "+mp2nameL); 
			}
			if (audioout2.length()<100) 
				audioout2.delete();
			else { 
				Common.renameTo(audioout2, mp2nameR); //DM13042004 081.7 int01 changed
				//audioout2.renameTo(mp2nameR); 

				Msg("===> new (right)File: "+mp2nameR); 
				InfoAtEnd.add(comparedata+"\t "+mp2nameR); 
			}

			audiooutL.renameIddTo(mp2nameL);
			audiooutR.renameIddTo(mp2nameR);
		} else {
			if ( mp2name.exists() ) 
				mp2name.delete();
			if (audioout1.length()<100) 
				audioout1.delete();
			else { 
				Common.renameTo(audioout1, mp2name); //DM13042004 081.7 int01 changed
				//audioout1.renameTo(mp2name); 

				Msg("===> new File: "+mp2name); 
				InfoAtEnd.add(comparedata+"\t "+mp2name); 
			}
			if (audioout2.length()<100) 
				audioout2.delete();

			audiooutL.renameIddTo(mp2name);
			audiooutR.deleteIdd();
		}
		break;
	}
	case 3: { 


		if ( mp1name.exists() ) 
			mp1name.delete();
		if (audioout1.length()<100) 
			audioout1.delete();
		else { 
			Common.renameTo(audioout1, mp1name); //DM13042004 081.7 int01 changed
			//audioout1.renameTo(mp1name); 

			Msg("===> new File: "+mp1name); 
			InfoAtEnd.add(comparedata+"\t "+mp1name); 
		}
		if (audioout2.length()<100) 
			audioout2.delete();

		audiooutL.renameIddTo(mp1name);
		audiooutR.deleteIdd();
		break;
	}
	//R_One18122003 081.6 int07 changed
	case 4: { 
		if (dtsname.exists())
			dtsname.delete();
		if (audioout1.length()<100) 
			audioout1.delete();
		else { 
			Common.renameTo(audioout1, dtsname); //DM13042004 081.7 int01 changed
			//audioout1.renameTo(dtsname); 

			Msg("===> new File: "+dtsname); 
			InfoAtEnd.add(comparedata+"\t "+dtsname);
		}
		if (audioout2.length()<100) 
			audioout2.delete();

		audiooutL.renameIddTo(dtsname);
		audiooutR.deleteIdd();
		break;
	}
	case 5: //DM24012004 081.6 int11 add
	{
		if (wavname.exists())
			wavname.delete();
		if (audioout1.length()<100) 
			audioout1.delete();
		else { 
			Common.renameTo(audioout1, wavname); //DM13042004 081.7 int01 changed



			//audioout1.renameTo(wavname); 

			Msg("===> new File: "+wavname); 
			InfoAtEnd.add(comparedata+"\t "+wavname);
		}
		if (audioout2.length()<100) 
			audioout2.delete();

		audiooutL.renameIddTo(wavname);
		audiooutR.deleteIdd();
		break;
	}
	case 10: //DM27042004 081.7 int02 add
	{
		Msg("!> no Audio found on this stream data..."); 

		audioout1.delete();
		audioout2.delete();
		audiooutL.deleteIdd();
		audiooutR.deleteIdd();
		break;
	}

	}


	}    // end try
	catch (IOException e) {  
		//DM25072004 081.7 int07 add
		Msg("file I/O error: " + e);  
	}

	progress.setValue(100); //DM13042004 081.7 int01 add
	yield();
	System.gc();

	return false;

}  /* end method processAudio */


//DM17012004 081.6 int11 new , calculation for insertion, adding
public String FramesToTime(int framenumber, double framelength)
{
	return ("" + Math.round(framenumber * framelength / 90.0));
}


/****************************
 * decoding teletext stream *
 ****************************/
public void processTeletext(String[] args)
{
	int LB1 = -1;

	//DM14052004 081.7 int02 changed
	if (RButton[23].isSelected() || RButton[17].isSelected()) // SUP + SON, set variables
		LB1 = subpicture.picture.set(comBox[26].getSelectedItem().toString(),d2vfield[6].getText().toString());

	for (int LB=0; LB<2; LB++)
	{
		for (int pn=0; pn<6; pn++)
		{
			String page = "0";
			if (!cBox[17].isSelected())
			{
				page = comBox[28+pn].getSelectedItem().toString();

				if (page.equalsIgnoreCase("null")) 
					continue;
			}
			else
				pn=6;

			String filename = args[0];
			File inp = new File(filename); 
			long size = inp.length();

			String fchild = (new File(args[0]).getName()).toString();
			String fparent = ( fchild.lastIndexOf(".") != -1 ) ? workouts + fchild.substring(0, fchild.lastIndexOf(".")) : workouts + fchild;

			//DM17042004 081.7 int02 add
			tpm.picture.init(fchild);

			//DM30072004 081.7 int07 add
			Teletext.clearEnhancements();

			String LBs = LB==1 ? "[1]" : "";

			if (!cBox[17].isSelected()) 
				fparent += "[" + page + "]" + LBs;

			String ttxfile;
			int subtitle_type;

			if (cBox[17].isSelected()) 
			{
				ttxfile = fparent + ".mgr";
				subtitle_type = 0;
			}
			else if (RButton[18].isSelected())
			{
				ttxfile = fparent + ".txt";
				subtitle_type = 1;
			}
			else if (RButton[19].isSelected())
			{
				ttxfile = fparent + ".sc";
				subtitle_type = 2;
			}
			else if (RButton[20].isSelected())
			{
				ttxfile = fparent + ".sub";
				subtitle_type = 3;
			}
			else if (RButton[21].isSelected()) 
			{
				ttxfile = fparent + ".srt";
				subtitle_type = 4;
			}
			else if (RButton[22].isSelected()) 
			{
				ttxfile = fparent + ".ssa";
				subtitle_type = 5;
			}
			else if (RButton[23].isSelected()) 
			{
				ttxfile = fparent + ".sup";
				subtitle_type = 6;
			}
			else if (RButton[24].isSelected()) 
			{
				ttxfile = fparent + ".stl";
				subtitle_type = 7;
			}
			else if (RButton[17].isSelected()) //DM14052004 081.7 int02 add, placeholder for .son export
			{
				ttxfile = fparent + ".ssa"; //.son
				subtitle_type = 5;  // 8
			}
			else
			{
				Msg("!> no output format for teletext specified...");
				break;
			}

			Msg("-> export format: " + ttxfile.substring(ttxfile.length() - 3));


			java.text.DateFormat timeformat_1 = new java.text.SimpleDateFormat("HH:mm:ss.SSS");
			timeformat_1.setTimeZone(java.util.TimeZone.getTimeZone("GMT+0:00"));

			java.text.DateFormat timeformat_2 = new java.text.SimpleDateFormat("HH:mm:ss,SSS");
			timeformat_2.setTimeZone(java.util.TimeZone.getTimeZone("GMT+0:00"));

			int page_value = subtitle_type == 0 ? 0x800 : Integer.parseInt(page,16);

			boolean vptsdata=false, ptsdata=false, write=false, loadpage=false, valid=false;
			long count=0, time_difference=0, source_pts=0, startPoint=0;
			int x=0, seiten=0, v=0, w=0;
			String page_number="", subpage_number="";
			char txline[];

			try 
			{

			PushbackInputStream in = new PushbackInputStream(new FileInputStream(filename), 94);
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(ttxfile), 655350);
			ByteArrayOutputStream byte_buffer = new ByteArrayOutputStream();
			PrintWriter print_buffer = new PrintWriter(byte_buffer, true);

			Msg("-> temp. file: " + filename + "  (" + size + " bytes)"); //DM18052004 081.7 int02 changed

			progress.setString("searching & decoding "+(subtitle_type==0 ? "MegaRadio mp3-stream":"page number "+page));
			progress.setStringPainted(true);
			progress.setValue(0);

			Msg("-> looking for "+(subtitle_type==0 ? "MegaRadio mp3-stream":"page number "+page));

			long[] pts_value = {0}, pts_position = {0}, video_pts_value = {0}, vtime = {0};

			showExportStatus("pages:", seiten);

			System.gc();

			if ( !args[1].equals("-1") )
			{
				if (options[30]==1) 
					System.out.print("\r-> loading teletext PTS logfile...");   

				int logsize = (int)(new File(args[1])).length() / 16;
				DataInputStream bin = new DataInputStream(new BufferedInputStream(new FileInputStream(args[1]),655350));

				pts_value = new long[logsize+1]; 
				pts_position = new long[logsize+1];
				pts_value[logsize] = -1;  
				pts_position[logsize] = -1;
				int aa=0;

				for (int a=0; a<logsize; a++)
				{
					long ptsVal = bin.readLong();
					long ptsPos = bin.readLong();

					if (options[30]==1) 
						System.out.println(" #"+aa+"/"+a+" _"+ptsVal+" / "+ptsPos);

					if (aa>0 && ptsVal<=pts_value[aa-1])
					{
						if (aa>1 && Math.abs(ptsVal-pts_value[aa-2])<150000 && Math.abs(ptsVal-pts_value[aa-1])>500000)
						{
							aa--;
							if (options[30]==1)
								System.out.print(" <!^> ");
						}
						else
						{
							if (options[30]==1) 
								System.out.print(" <!v> ");
							continue;
						}
					}
					pts_value[aa] = ptsVal;
					pts_position[aa] = ptsPos;
					aa++;
				}
				if (aa<logsize)
				{
					Msg("!> "+(logsize-aa)+" PTS's discarded in stream");
					long tmp[][] = new long[2][aa];
					System.arraycopy(pts_value,0,tmp[0],0,aa);
					System.arraycopy(pts_position,0,tmp[1],0,aa);
					pts_value = new long[aa+1];
					System.arraycopy(tmp[0],0,pts_value,0,aa);
					pts_value[aa]= -1;
					pts_position = new long[aa+1];
					System.arraycopy(tmp[1],0,pts_position,0,aa);
					pts_position[aa]= -1;
				}
	
				yield();
				bin.close();

				if (!( (pts_value[0]==0xffffffffL) || (pts_value[0] == 0) ))
				{
					Msg("Teletext PTS: first packet "+timeformat_1.format(new java.util.Date(pts_value[0]/90))+", last packet "+timeformat_1.format(new java.util.Date(pts_value[pts_value.length-2]/90)));
					ptsdata=true; 
				}
				else  
					Msg(" teletext stream doesn't use PTS's, sync impossible");
			}


			if ( !args[3].equals("-1") )
			{
				if (options[30]==1) 
					System.out.print("\r-> loading video PTS logfile...");   

				int vlogsize = (int)(new File(args[3])).length() / 16;
				DataInputStream vbin = new DataInputStream(new BufferedInputStream(new FileInputStream(args[3]),655350));
				video_pts_value = new long[vlogsize]; 
				vtime = new long[vlogsize]; 
				long vdiff = 0;

				for ( int b = 0; b < vlogsize; b+=2 )
				{
					video_pts_value[b] = vbin.readLong(); 
					video_pts_value[b+1] = vbin.readLong();
					vtime[b] = vbin.readLong();
					vtime[b+1] = vbin.readLong(); 
					if (options[30]==1) 
						System.out.println(" #s"+b+" _"+video_pts_value[b]+" #e"+(b+1)+" _"+video_pts_value[b+1]+" /#s"+b+" _"+vtime[b]+" #e"+(b+1)+" _"+vtime[b+1]);
				}
				vbin.close();

				Msg("Video PTS: start 1.GOP "+timeformat_1.format(new java.util.Date(video_pts_value[0]/90))+", end last GOP "+timeformat_1.format(new java.util.Date(video_pts_value[video_pts_value.length-1]/90)));
				vptsdata=true;
			}

			System.gc(); //DM22122003 081.6 int09 new

			// 1st line 
			switch (subtitle_type)
			{
			case 2:
				print_buffer.println("Subtitle File Mark:"+((options[14]==3600L) ? "2" : "1"));
				print_buffer.flush();
				byte_buffer.writeTo(out);
				byte_buffer.reset();
				break;

			case 5:
				String[] SSAhead = Teletext.getSSAHead();
				for (int a=0; a<SSAhead.length; a++) 
					print_buffer.println(SSAhead[a]);
				print_buffer.flush();
				byte_buffer.writeTo(out);
				byte_buffer.reset();
				break;

			case 7:
				String[] STLhead = Teletext.getSTLHead(version[0]+" on "+java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM).format(new java.util.Date(System.currentTimeMillis())));
				for (int a=0;a<STLhead.length;a++) 
					print_buffer.println(STLhead[a]);
				print_buffer.flush();
				byte_buffer.writeTo(out);
				byte_buffer.reset();
				break;

			case 8:  //DM14052004 081.7 int02 add, still unused!
				String[] SONhead = Teletext.getSONHead(new File(ttxfile).getParent(), options[14]);

				for (int a=0; a < SONhead.length; a++) 
					print_buffer.println(SONhead[a]);

				print_buffer.flush();
				byte_buffer.writeTo(out);
				byte_buffer.reset();
			} 

			//DM150702004 081.7 int06 changed, fix
			if (vptsdata && ptsdata)
			{
				int jump = checkPTSMatch(video_pts_value, pts_value);

				if (jump < 0)
				{
					Msg("-> !! video & teletext PTS doesn't match at any time!");  
					vptsdata = false; 
					x = 0; 
				}

				else
					x = jump;

				/**
				timebase:
				while ( pts_value.length>2 && Math.abs(pts_value[x] - video_pts_value[0]) > 20000000 )
				{
					x++;
					if (x >= pts_value.length-1)
					{ 
						Msg("-> !! video & teletext PTS doesn't match at any time!"); 
						vptsdata=false; 
						x=0; 
						break timebase; 
					}
				}
				**/
			}


			if (vptsdata && ptsdata) 
			{
				Msg("->adjusting teletext at video-timeline"); //DM24012004 081.6 int11 changed  //DM22032004 081.6 int18 changed
				time_difference = video_pts_value[0];
			}

			if (!vptsdata && ptsdata)
			{
				Msg("-> adjusting teletext at its own timeline ");
				time_difference = 0;
			}

			x=0;  //start at 0

			if (ptsdata)
			{ 
				source_pts = pts_value[x]; 
				startPoint = pts_position[x]; 
			}

			while (count < startPoint)
				count += in.skip(startPoint-count);

			yield();

			boolean miss=false; //DM30122003 081.6 int10 add

			//DM10032004 081.6 int18 moved
			boolean vps = false, page_match = false, lastpage_match = false;
			int row = -1, magazine = -1, vbi = 0, character_set = 0;
			int data_unit_id = -1, required_data_unit_id = -1;

			byte packet[] = new byte[48];
			Hashtable load_buffer = new Hashtable(), write_buffer = new Hashtable();
			Hashtable flags = new Hashtable();
			ArrayList picture_String = new ArrayList();

			//DM24072004 081.7 int07 add
			String provider = "", program_title = "", vps_str = "";

			readloop:
			while ( count < size )
			{

				progress.setValue((int)(100*count/size)+1);
				yield();

				while (qpause) 
					pause();

				if (qbreak)
				{ 
					qbreak=false; 
					break readloop; 
				}

				in.read(packet);

				if (packet[1] != 0x2C && packet[47] != 0x2C && packet[1] != 0x5A && (0xFF & packet[1]) != 0x88)
				{
					if (!cBox[3].isSelected() && !miss)
						Msg("!> missing syncword @ "+count);

					miss = true;
					count++;
					in.unread(packet, 1, 47);
					continue readloop;
				}
				else if (packet[1] == 0x5A && packet[0] == -1)
				{
					in.skip(0x5C - 48);
					count += 0x5C;
					continue readloop;
				}
				else if ((0xFF & packet[1]) == 0x88 && packet[0] == -1)
				{
					in.skip(0x8A - 48);
					count += 0x8A;
					continue readloop;
				}
				else
					in.unread(packet,46,2);

				if (!cBox[3].isSelected() && miss)
					Msg("!> found syncword @ "+count);
				miss = false;

				count += 46;

				vps = false;
				valid = false;
				data_unit_id = 0xFF & packet[0];

				switch (data_unit_id)
				{
				case 2:    // 0x02 EBU non subtitle data
				case 3:    // 0x03 EBU subtitle data
					valid = true; 
					break; 

				case 0xC3: // VPS
					valid = true; 
					vps = true; 
					break; 

				case 0xFF: // hidden 
					if (cBox[22].isSelected())
						valid = true;
					break; 

				default:  // others, unknown
					if (options[30]==1) 
						System.out.println(" unkn_"+Integer.toHexString(0xFF & packet[0])+"/"+(count-46));
					//continue readloop;
				}

				// logging
				if (options[30]==1)
				{
					System.out.println();
					for (int a=0; a<46; a++)
						System.out.print(" "+((0xFF & packet[a])<0x10 ? "0" : "") + Integer.toHexString(0xFF & packet[a]).toUpperCase());
					System.out.println();
				}

				if (!valid)
					continue readloop;

				vbi = ((0x20 & packet[2]) != 0 ? 0 : 313) + (0x1F & packet[2]);

				if (!vps)
				{
					row = 0xFF & Teletext.bytereverse((byte)((0xF & Teletext.hamming_decode(packet[4]))<<4 | (0xF & Teletext.hamming_decode(packet[5]))));
					magazine = (7 & row) == 0 ? 8 : (7 & row);
					row >>>= 3;
				}
				else
				{
					if ((0x3F & packet[2]) != 0x30)
						continue readloop;

					// show vps status of VBI 16 in GUI
					int vps_data = (0x3F & packet[11])<<24 | (0xFF & packet[12])<<16 | (0xFF & packet[13])<<8 | (0xFF & packet[14]);
					String vps_sound_mode[] = { "n/a", "mono", "stereo", "dual" };
					String vps_status = "";

					switch (0x1F & vps_data>>>16)
					{ 
					case 0x1C:
						vps_status = "Contin."; 
						break; 
					case 0x1D:
						vps_status = "Pause  "; 
						break; 
					case 0x1E:
						vps_status = "Stop   "; 
						break; 
					case 0x1F:
						vps_status = "Timer  "; 
						break; 
					default: 
						vps_status = "" + (0x1F & (vps_data>>>25)) + "." + (0xF & (vps_data>>>21)) + ".  " +
							(0x1F & (vps_data>>>16)) + ":" + (0x3F & (vps_data>>>10)) + "  ";
					}

					//DM23072004 081.7 int07 changed
					vps_status += vps_sound_mode[(3 & packet[5]>>>6)] + " " + Integer.toHexString(0xF & vps_data>>>6).toUpperCase() + " " + Integer.toHexString(vps_data & 0x3F).toUpperCase();

					if (cBox[19].isSelected())
						ttxvpsLabel.setText(vps_status);

					//DM23072004 081.7 int07 add
					if (!vps_status.equals(vps_str))
					{
						vps_str = vps_status;
						Msg("-> VPS status: " + vps_str + " @ PTS " + timeformat_1.format( new java.util.Date( source_pts / 90)) );
					}

					continue readloop;
				}


				//DM24072004 081.7 int07 add
				// X3/31.1 ttx provider
				if (magazine == 3 && row == 31 && packet[7] == 0x40 && packet[8] == 0x57 && provider.equals(""))
				{
					provider = Teletext.makestring(packet, 10, 34, 31, 0, 0, false).trim();
					Msg("-> provider: " + provider);
				}

				//DM24072004 081.7 int07 add
				// X8/30.0 program title
				else if (magazine == 8 && row == 30 && packet[7] == (byte)0xA8)
				{
					String str = Teletext.makestring(packet, 26, 20, 30, 0, 0, true).trim() + " ";

					if (!str.equals(program_title))
					{
						program_title = str;
						Msg("-> program: " + program_title);
					}
				}


				if (row == 0)
				{
					int flag = 0;

					for (int a=0; a<6; a++)
						flag |= (0xF & Teletext.bytereverse( Teletext.hamming_decode(packet[8+a]) )>>>4 ) <<(a*4);

					page_number = Integer.toHexString(0xF & Teletext.bytereverse( Teletext.hamming_decode(packet[7]) )>>>4 ).toUpperCase() +
						Integer.toHexString(0xF & Teletext.bytereverse( Teletext.hamming_decode(packet[6]) )>>>4 ).toUpperCase();

					int o[] = { 0xF, 7, 0xF, 3 };
					subpage_number = "";
					for (int a=3; a>-1; a--)
						subpage_number += Integer.toHexString(o[a] & flag>>>(a*4)).toUpperCase();

					flags.put("data_unit_id", "" + data_unit_id);
					flags.put("magazine", "" + magazine);
					flags.put("page_number", page_number);
					flags.put("subpage_number", subpage_number);
					flags.put("news", "" + (1 & flag>>>14));
					flags.put("subtitle", "" + (1 & flag>>>15));
					flags.put("erase", "" + (1 & flag>>>7));
					flags.put("suppressed_head", "" + (1 & flag>>>16));
					flags.put("update", "" + (1 & flag>>>17));
					flags.put("interrupt", "" + (1 & flag>>>18));
					flags.put("inhibit", "" + (1 & flag>>>19));
					flags.put("magazine_serial", "" + (1 & flag>>>20));
					flags.put("character_set", "" + (7 & flag>>>21));

					// page_number matches -- subpage_numer currently always accepted
					if ( page.equalsIgnoreCase( Integer.toHexString(magazine) + page_number) )
					{
						character_set = 7 & flag>>>21;
						page_match = true;
					}
					else
						page_match = false;

					//DM17042004 081.7 int02 add
					tpm.picture.update("" + magazine + page_number);

					// show header_line in GUI
					if (cBox[19].isSelected() || options[30] == 1) 
					{
						//DM24072004 081.7 int07 changed
						String str = magazine + page_number + "  " + subpage_number + "  " + Teletext.makestring(packet, 14, 32, 0, (7 & flag>>>21), 0, true) + "  " + program_title;

						if (cBox[19].isSelected())
							ttxheaderLabel.setText(str);

						if (options[30] == 1)
							System.out.println(str);
					}

					if (options[30] == 1)
						System.out.println(flags.toString());
				}

				if (ptsdata)
				{
					write = vptsdata ? false : true;

					while (pts_position[x+1] != -1 && pts_position[x+1] < count-46)
					{
						x++;
						source_pts = pts_value[x];
					}

					rangeloop:
					while (vptsdata && v < video_pts_value.length)  //pic_start_pts must be in range ATM
					{ 
						if (source_pts < video_pts_value[v])
						{
							//write_buffer.put("cut_in_time", "" + video_pts_value[v]); // save value for cuts
							break rangeloop;
						}

						else if (source_pts == video_pts_value[v] || source_pts < video_pts_value[v+1])
						{
							write=true;
							break rangeloop;
						}

						v += 2;
						if (v < video_pts_value.length)
						{
							//write_buffer.put("cut_out_time", "" + video_pts_value[v-1]); // save value for cuts
							time_difference += (video_pts_value[v] - video_pts_value[v-1]);
						}
					}
				}
				else
					write = true;


				// logging
				if (options[30]==1)
					System.out.println("pos "+ (count-46) + "/vbi "+vbi+"/ "+magazine+"-"+row+"-"+page_number+"-"+subpage_number+"/pts "+source_pts+"/ "+timeformat_1.format(new java.util.Date(source_pts/90))+"/ "+Integer.toHexString(page_value)+"/wr "+write+"/lo "+loadpage+"/lastp "+lastpage_match+"/pagm "+page_match+"/v "+(v<video_pts_value.length ? video_pts_value[v] : v));

				if (row != 0 && magazine != page_value>>>8) //row > 0, but not of current magazine
						continue readloop;

				if (row == 0)  //accept all 0-rows of all magazines till now
				{
					boolean interrupt_loading = false;

					//stop loading if same magazine, but other page
					//stop loading if interrupted_flag by any page only if magazine_serial == 1, means rows of diff. pages of multiple magazines don't overlap by sent order
					if (page_match || magazine == page_value>>>8 || flags.get("magazine_serial").toString().equals("1") )
						interrupt_loading = true;

					if (options[30] == 1)
						System.out.println("int " + interrupt_loading +"/load "+ loadpage + "/wri "+ write + "/wb " + write_buffer.size() + "/lb " + load_buffer.size());

					// page header does not interrupt an active loading
					if (!interrupt_loading)
						continue readloop;

					// megaradio_mode, magazine 8 only, other magazines are already ignored before page check
					if (subtitle_type == 0)
					{
						switch (Integer.parseInt(page_number, 16))
						{
						case 0xA:
						case 0xB:
						case 0xC:
							loadpage=true; 
						}

						switch (Integer.parseInt(subpage_number, 16))
						{
						case 0x0D:
						case 0x0E:
						case 0x1C:
						case 0x1D:
						//case 0x2A:
						case 0x2B:
						case 0x2C:
						case 0x2D:
							loadpage=false; 
						}

						continue readloop; 
					}

					// row 0 defines out_time of current buffered page forced by an interrupt event, 
					// sets the out_time of displaying and write it out
					if (lastpage_match)
					{
						long out_time = source_pts - time_difference;

						if (write)  //buffered page can be written
						{
							write_buffer.put("out_time", "" + out_time);

							while (true)
							{
								if ( !write_buffer.containsKey("active") )
									break;

								if ( !write_buffer.containsKey("in_time") )
									break;

								switch (subtitle_type)
								{
								case 1:  // free
									print_buffer.print( "in_" + timeformat_1.format( new java.util.Date( Long.parseLong( write_buffer.get("in_time").toString()) / 90) ));
									print_buffer.println( "|out_" + timeformat_1.format( new java.util.Date( Long.parseLong( write_buffer.get("out_time").toString()) / 90) )); 
									break;

								case 2:  // SC
									print_buffer.print( Teletext.SMPTE( timeformat_1.format( new java.util.Date( Long.parseLong( write_buffer.get("in_time").toString()) / 90) ), options[14]) + "&");
									print_buffer.print( Teletext.SMPTE( timeformat_1.format( new java.util.Date( Long.parseLong( write_buffer.get("out_time").toString()) / 90) ), options[14]) + "#");
									break;

								case 3:  // SUB
									print_buffer.print( "{" + ( Long.parseLong( write_buffer.get("in_time").toString()) / options[14]) + "}");
									print_buffer.print( "{" + ( Long.parseLong( write_buffer.get("out_time").toString()) / options[14]) + "}");
									break;

								case 4:  // SRT
									print_buffer.println( "" + (seiten + 1));
									print_buffer.print( timeformat_2.format( new java.util.Date( Long.parseLong( write_buffer.get("in_time").toString()) / 90) ));
									print_buffer.println(" --> " + timeformat_2.format( new java.util.Date( Long.parseLong( write_buffer.get("out_time").toString()) / 90) ));
									break;

								case 5:  // SSA
									print_buffer.print( Teletext.getSSALine()[0] + timeformat_1.format( new java.util.Date( Long.parseLong( write_buffer.get("in_time").toString()) / 90) ).substring(1, 11) + ",");
									print_buffer.print( timeformat_1.format( new java.util.Date( Long.parseLong( write_buffer.get("out_time").toString()) / 90) ).substring(1, 11) + Teletext.getSSALine()[1]);
									break;

								case 7:  // STL
									print_buffer.print( Teletext.SMPTE(timeformat_1.format( new java.util.Date( Long.parseLong( write_buffer.get("in_time").toString()) / 90) ), options[14]) + ",");
									print_buffer.print( Teletext.SMPTE(timeformat_1.format( new java.util.Date( Long.parseLong( write_buffer.get("out_time").toString()) / 90) ), options[14]) + ",");
									break;

								case 6:  // SUP
									long sup_in_time = Long.parseLong( write_buffer.get("in_time").toString() );
									if ( sup_in_time >= 0 )
									{
										for (int a=1; a<24; a++)
											if ( write_buffer.containsKey("" + a) )
												picture_String.add(write_buffer.get("" + a));

										//DM26052004 081.7 int03 changed
										while (picture_String.size() > subpicture.picture.getMaximumLines()) // max. lines as defined
											picture_String.remove(0);

										subpicture.picture.showPicTTX( picture_String.toArray());
										byte_buffer.write( subpicture.picture.writeRLE( sup_in_time, 0xB4)); // alias duration 1.800 sec if out_time is missing

										if ( write_buffer.containsKey("out_time") )
											out.write(subpicture.picture.setTime( byte_buffer.toByteArray(), Long.parseLong( write_buffer.get("out_time").toString())));
									}
									picture_String.clear();
								}

								int b = 0;
								for (int a=1; subtitle_type != 6 && a<24; a++)
								{
									if ( !write_buffer.containsKey("" + a) )
										continue;

									String str = write_buffer.get("" + a).toString();

									switch (subtitle_type)
									{
									case 1:  // free
									case 4:  // SRT
										print_buffer.println(str); 
										break;

									case 2:  // SC
										print_buffer.print(str); 
										break;

									case 3:  // SUB
									case 7:  // STL
										print_buffer.print( (b > 0 ? "|" : "") + str);
										break;

									case 5:  // SSA
										print_buffer.print( (b > 0 ? "\\n" : "") + str);
									}

									b++;
								}

								if (subtitle_type != 6 && b > 0)
								{
									print_buffer.println();
									print_buffer.flush();
									byte_buffer.writeTo(out); 
								}

								seiten++;
								showExportStatus("pages:", seiten);
								break;
							}
						}

						byte_buffer.reset(); 
						write_buffer.clear();
					}

					lastpage_match = page_match ? true : false;

					// row 0 defines completion of current page to buffer, 
					// sets the in_time of displaying but cannot write it w/o still unknown out_time
					if (loadpage)
					{
						write_buffer.clear();

						if (!vptsdata && time_difference==0 && !RButton[13].isSelected())
							time_difference = source_pts;

						long in_time = source_pts - time_difference;
						boolean rows = false;

						// copy keys+values to clear for next page, only row 1..23 used instead of 0..31
						for (int a=1; a<24; a++) 
						{
							if ( !load_buffer.containsKey("" + a) )
								continue;

							rows = true; // non blank page
							write_buffer.put("" + a, load_buffer.get("" + a));
						}

						if (rows && write) // if false, in_time has to be set/updated at synccheck above until an exported gop pts area
							write_buffer.put("in_time", "" + in_time);

						if (!rows)
							lastpage_match = false;
						else
							write_buffer.put("active", "1");

						//DM30072004 081.7 int07 add
						Teletext.clearEnhancements();

						load_buffer.clear();
					}

					loadpage = page_match ? true : false;

					// logging
					if (options[30] == 1)
						System.out.println("lo " + loadpage + "/lp_p "+lastpage_match+"/pg_m "+page_match+"/wbuf: " + write_buffer.toString());

					continue readloop;
				}

				// only rows > 0

				// logging
				if (options[30] == 1)
					System.out.println("load " + loadpage + "/lbuf " + load_buffer.toString());

				// ignore if row is not of expected magazine
				//DM30072004 081.7 int07 changed
				if (magazine != page_value>>>8)
					continue readloop;

				// load and filter re-defined chars from X26/0..15 triplets
				//DM30072004 081.7 int07 add
				if (row > 23 && subtitle_type != 0)
				{
					if (row == 29 || loadpage)
						Teletext.setEnhancements(packet, row, character_set);

					continue readloop;
				}

				if (!loadpage)
					continue readloop;

				//DM30072004 081.7 int07 changed
				if (subtitle_type == 0)  // megaradio, simple decode the bytes of row 1..23
				{
					for (int b = (row == 1) ? 17: 0; row < 24 && b < 39; b++) // framebytes to MSB
						out.write(Teletext.bytereverse(packet[7+b]));

					continue readloop;
				}

				// decode row 1..23 , 0=header, 24 fasttext labels, 25 supressedheader, >26 non text packets 

				String str = null;
				int picture_data[] = null;

				//DM24072004 081.7 int07 changed
				switch (subtitle_type)
				{
				case 1:
					str = Teletext.makestring(packet, 6, 40, row, character_set, 0, true);
					break;

				//DM09082004 081.7 int08 changed
				case 2:
				case 7:
				case 3:
				case 4:
					str = Teletext.makestring(packet, 6, 40, row, character_set, 0, true).trim();
					break;

				case 5:
					str = Teletext.makestring(packet, 6, 40, row, character_set, 1, true).trim();
					break;

				case 6:
					picture_data = Teletext.makepic(packet, 6, 40, row, character_set, true);
				}

				if (str != null && !str.equals(""))
					load_buffer.put("" + row, str);

				else if (picture_data != null)
					load_buffer.put("" + row, picture_data);

				if (options[30]==1) 
					System.out.println("row " + row + ": " + str + "/lb " + load_buffer.size()); 

			} // return to read next packet

  
			//write out last page in buffer
			if (!write_buffer.isEmpty() && write)
			{
				long out_time = (vptsdata ? video_pts_value[video_pts_value.length - 1] : source_pts) - time_difference;
				write_buffer.put("out_time", "" + out_time);

				while (true)
				{
					if ( !write_buffer.containsKey("active") || !write_buffer.containsKey("in_time") )
						break;

					switch (subtitle_type)
					{
					case 1:  // free
						print_buffer.print( "in_" + timeformat_1.format( new java.util.Date( Long.parseLong( write_buffer.get("in_time").toString()) / 90) ));
						print_buffer.println( "|out_" + timeformat_1.format( new java.util.Date( Long.parseLong( write_buffer.get("out_time").toString()) / 90) )); 
						break;

					case 2:  // SC
						print_buffer.print( Teletext.SMPTE(timeformat_1.format( new java.util.Date( Long.parseLong( write_buffer.get("in_time").toString()) / 90) ), options[14]) + "&");
						print_buffer.print( Teletext.SMPTE( timeformat_1.format( new java.util.Date( Long.parseLong( write_buffer.get("out_time").toString()) / 90) ), options[14]) + "#");
						break;

					case 3:  // SUB
						print_buffer.print( "{" + ( Long.parseLong( write_buffer.get("in_time").toString()) / options[14]) + "}");
						print_buffer.print( "{" + ( Long.parseLong( write_buffer.get("out_time").toString()) / options[14]) + "}");
						break;

					case 4:  // SRT
						print_buffer.println( "" + (seiten + 1));
						print_buffer.print( timeformat_2.format( new java.util.Date( Long.parseLong( write_buffer.get("in_time").toString()) / 90) ));
						print_buffer.println(" --> " + timeformat_2.format( new java.util.Date( Long.parseLong( write_buffer.get("out_time").toString()) / 90) ));
						break;

					case 5:  // SSA
						print_buffer.print( Teletext.getSSALine()[0] + timeformat_1.format( new java.util.Date( Long.parseLong( write_buffer.get("in_time").toString()) / 90) ).substring(1, 11) + ",");
						print_buffer.print( timeformat_1.format( new java.util.Date( Long.parseLong( write_buffer.get("out_time").toString()) / 90) ).substring(1, 11) + Teletext.getSSALine()[1]);
						break;

					case 7:  // STL
						print_buffer.print( Teletext.SMPTE(timeformat_1.format( new java.util.Date( Long.parseLong( write_buffer.get("in_time").toString()) / 90) ), options[14]) + ",");
						print_buffer.print( Teletext.SMPTE(timeformat_1.format( new java.util.Date( Long.parseLong( write_buffer.get("out_time").toString()) / 90) ), options[14]) + ",");
						break;

					case 6:  // SUP
						long sup_in_time = Long.parseLong( write_buffer.get("in_time").toString() );
						if ( sup_in_time >= 0 )
						{
							for (int a=1; a<24; a++)
								if ( write_buffer.containsKey("" + a) )
									picture_String.add(write_buffer.get("" + a));

							//DM26052004 081.7 int03 changed
							while (picture_String.size() > subpicture.picture.getMaximumLines())
								picture_String.remove(0);

							subpicture.picture.showPicTTX( picture_String.toArray());
							byte_buffer.write( subpicture.picture.writeRLE( sup_in_time, 0xB4)); // alias duration 2.000 sec if out_time is missing

							out.write(subpicture.picture.setTime( byte_buffer.toByteArray(), Long.parseLong( write_buffer.get("out_time").toString())));
						}


						picture_String.clear();
					}

					int b = 0;
					for (int a=1; subtitle_type != 6 && a<24; a++)
					{
						if ( !write_buffer.containsKey("" + a) )
							continue;

						String str = write_buffer.get("" + a).toString();

						switch (subtitle_type)
						{
						case 1:  // free
						case 4:  // SRT
							print_buffer.println(str); 
							break;

						case 2:  // SC
							print_buffer.print(str); 
							break;

						case 3:  // SUB
						case 7:  // STL
							print_buffer.print( (b > 0 ? "|" : "") + str);
							break;

						case 5:  // SSA
							print_buffer.print( (b > 0 ? "\\n" : "") + str);
						}

						b++;
					}

					if (subtitle_type != 6 && b > 0)
					{
						print_buffer.println();
						print_buffer.flush();
						byte_buffer.writeTo(out); 
					}

					seiten++;
					showExportStatus("pages:", seiten);
					break;
				}
			}
 
			if (options[30]==1) 
				System.out.println();

			Msg(" " + seiten + " pages of No. " + page + " written..");

			if (seiten>0) 
				Msg("===> new File "+ttxfile);

			yield();

			write_buffer.clear();

			in.close();
			print_buffer.flush(); 
			print_buffer.close();
			byte_buffer.flush(); 
			byte_buffer.close();
			out.flush(); 
			out.close();

			File ttxfile1 = new File(ttxfile); 

			if (subtitle_type > 0 && seiten==0) 
				ttxfile1.delete();

			else if (subtitle_type == 0)
			{
				RandomAccessFile log = new RandomAccessFile(fparent+".pts","rw");
				log.writeLong(0L); 
				log.writeLong(0L); 
				log.close();
				String[] synchit = { ttxfile,fparent+".pts","mp","-1" };
				Msg(synchit[0]);
				Msg("=> File is MPEG Audio ES");
				yield();
				mpt(synchit);      /* audiofile goes to synch methode */
				new File(synchit[0]).delete();
				new File(synchit[1]).delete();
				System.gc();

				return;
			}
			else
			{ 
				if (subtitle_type == 6)  //DM14052004 081.7 int02 add
					Ifo.createIfo(ttxfile, subpicture.picture.getUserColorTableArray());

				options[39] += ttxfile1.length();

				//DM04032004 081.6 int18 changed
				//DM15072004 081.7 int06 changed
				InfoAtEnd.add("Teletext " + (NoOfPictures++) + ":\t" + seiten + " pages of No. " + page + "\t" + infoPTSMatch(args, vptsdata, ptsdata) + " \t " + ttxfile1);
			}

			}  // end try
			catch (EOFException e1)
			{ 
				//DM25072004 081.7 int07 add
				Msg(" EOF reached in error: " + e1); 
			}
			catch (IOException e2)
			{ 
				//DM25072004 081.7 int07 add
				Msg("File I/O error: "+filename + " / " + e2); 
			}

		System.gc();

		}  // end for

		if (LB1<0) 
			return;
		else
		{ 
			subpicture.picture.set2();
			Msg("-> perform 2nd SUP Yoffset");
		}

	}  // end for


	//DM13042004 081.7 int01 add
	progress.setValue(100);
	yield();

	//DM13042004 081.7 int01 add
	System.gc();

}   // end methode processTeletext


//DM02032004 081.6 int18 new
final String subdecode_errors[] = {
	"",
	"", //DM24042004 081.7 int02 add // -1 = correct decoded dvb-subpicture segments, can export
	"", //DM24042004 081.7 int02 add // -2 = correct decoded dvb-subpicture segments, w/o export
	"!> error while decoding DVB Subpicture", //DM24042004 081.7 int02 add // -3 = error while decoding dvb-subpicture
	"!> lack of packet data",
	"!> packet size doesn't match packet data",
	"!> wrong chunk position index",
	"!> wrong chunk position",
	"!> wrong end of chunk index",
	"!> wrong end of chunk"
};


/****************************
 * decoding subpicture stream *
 ****************************/
//02032004 081.6 int18 new
//18052004 081.7 int02 changed a lot
public void processSubpicture(String[] args)
{

	String filename = args[0];
	long size = new File(filename).length();

	String fchild = (new File(args[0]).getName()).toString();
	String fparent = ( fchild.lastIndexOf(".") != -1 ) ? workouts+fchild.substring(0,fchild.lastIndexOf(".")) : workouts+fchild;
	fparent += options[11] == 1 ? "_0" : "";

	String subfile = fparent + ".sup";

	byte[] parse12 = new byte[12];
	byte[] packet = new byte[0];
	long count=0, startPoint=0, time_difference=0, display_time=0;
	long source_pts = 0, new_pts = 0, first_pts = -1, last_pts = 0;
	boolean vptsdata=false, ptsdata=false, write=false, miss=false;
	int x=0, pics=0, v=0, packetlength=0, export_type = 0, last_pgc_set = 0;

	boolean DVBpicture = false;

	try 
	{

	//DM13062004 081.7 int04 add++
	Hashtable user_table = new Hashtable();

	if (comBox[11].getSelectedIndex() > 2)
		user_table = Common.getUserColourTable(ColourTablesFile, comBox[11].getSelectedItem().toString());
	//DM13062004 081.7 int04 add--

	//DM25072004 081.7 int07 add
	subpicture.picture.reset();

	//DM13062004 081.7 int04 changed
	subpicture.picture.dvb.setIRD(2<<comBox[11].getSelectedIndex(), user_table, (options[30]==1 ? true : false), d2vfield[9].getText().toString());

	Msg("-> selected DVB subpicture color model: " + comBox[11].getSelectedItem() + " ; fixed to page id: " + d2vfield[9].getText().toString());

	if (RButton[17].isSelected())
	{
		subfile = fparent + ".son";
		export_type = 1;
	}

	Msg("-> export format: " + subfile.substring(subfile.length() - 3));

	PushbackInputStream in = new PushbackInputStream(new FileInputStream(filename),65536);
	IDDBufferedOutputStream out = new IDDBufferedOutputStream(new FileOutputStream(subfile),65536);

	PrintStream print_out = new PrintStream(out);

	Msg("-> temp. file: " + filename + "  (" + size + " bytes)");
	progress.setString("check & synchronize "+filename);
	progress.setStringPainted(true);
	progress.setValue(0);

	long[] ptsval = {0}, ptspos = {0}, vptsval = {0};

	//DM15072004 081.7 int06 add
	long pts_offset = options[28];


	System.gc();

	if ( !args[3].equals("-1") )
	{
		if (options[30]==1) 
			System.out.print("\r-> loading video PTS logfile...");   

		int vlogsize = (int)(new File(args[3])).length() / 16;
		DataInputStream vbin = new DataInputStream(new BufferedInputStream(new FileInputStream(args[3]),655350));
		vptsval = new long[vlogsize]; 

		for ( int b = 0; b < vlogsize; b+=2 )
		{
			vptsval[b] = vbin.readLong(); 
			vptsval[b+1] = vbin.readLong();
			vbin.skip(16);
			if (options[30]==1) 
				System.out.println(" #s"+b+" _"+vptsval[b]+" #e"+(b+1)+" _"+vptsval[b+1]);
		}
		vbin.close();

		Msg("Video PTS: start 1.GOP " + Common.formatTime_1(vptsval[0] / 90) + ", end last GOP " + Common.formatTime_1( vptsval[vptsval.length-1] / 90));
		vptsdata=true;
	}

	if ( !args[1].equals("-1") )
	{
		if (options[30]==1) 
			System.out.print("\r-> loading subpicture PTS logfile...");   

		int logsize = (int)(new File(args[1])).length() / 16;
		DataInputStream bin = new DataInputStream(new BufferedInputStream(new FileInputStream(args[1]),655350));

		ptsval = new long[logsize+1]; 
		ptspos = new long[logsize+1];
		ptsval[logsize] = -1;  
		ptspos[logsize] = -1;
		int aa=0;

		for (int a=0; a<logsize; a++)
		{
			long ptsVal = bin.readLong();
			long ptsPos = bin.readLong();

			if (options[30]==1) 
				System.out.println(" #"+aa+"/"+a+" _"+ptsVal+" / "+ptsPos);

			if (aa>0 && ptsVal<=ptsval[aa-1])
			{
				if (aa>1 && Math.abs(ptsVal-ptsval[aa-2])<150000 && Math.abs(ptsVal-ptsval[aa-1])>500000)
				{
					aa--;
					if (options[30]==1)
						System.out.print(" <!^> ");
				}
				else
				{
					if (options[30]==1) 
						System.out.print(" <!v> ");
					continue;
				}
			}
			ptsval[aa] = ptsVal;
			ptspos[aa] = ptsPos;
			aa++;
		}

		if (aa<logsize)
		{
			Msg("!> "+(logsize-aa)+" PTS's discarded in stream");
			long tmp[][] = new long[2][aa];
			System.arraycopy(ptsval,0,tmp[0],0,aa);
			System.arraycopy(ptspos,0,tmp[1],0,aa);
			ptsval = new long[aa+1];
			System.arraycopy(tmp[0],0,ptsval,0,aa);
			ptsval[aa]= -1;
			ptspos = new long[aa+1];
			System.arraycopy(tmp[1],0,ptspos,0,aa);
			ptspos[aa]= -1;
		}
	
		yield();
		bin.close();

		Msg("Subpicture PTS: first packet " + Common.formatTime_1(ptsval[0] / 90) + ", last packet " + Common.formatTime_1(ptsval[ptsval.length-2] / 90));
		ptsdata=true; 
	}

	ptsdata = true;
	System.gc();


	//DM150702004 081.7 int06 changed, fix
	if (vptsdata && ptsdata)
	{
		int jump = checkPTSMatch(vptsval, ptsval);

		if (jump < 0)
		{
			Msg("-> !! video & subpicture PTS doesn't match at any time!");  
			vptsdata = false; 
			x = 0; 
		}

		else
			x = jump;


		/**
		timebase:
		while ( ptsval.length>2 && Math.abs(ptsval[x] - vptsval[0]) > 20000000 )
		{
			x++;
			if (x >= ptsval.length-1)
			{ 
				Msg("-> !! video & subpicture PTS doesn't match at any time!"); 
				vptsdata=false; 
				x=0; 
				break timebase; 
			}
		}
		**/
	}

	if (vptsdata && ptsdata)
	{
		Msg("-> adjusting subpicture at video-timeline ");
		time_difference = vptsval[0];
	}

	if (!vptsdata && ptsdata)
	{
		Msg("-> adjusting subpicture at its own timeline ");
		time_difference = 0;
	}

	if (ptsdata)
	{ 
		source_pts = ptsval[x]; 
		startPoint = ptspos[x]; 
	}

	//don't need it anymore
	ptsval = null;
	ptspos = null;

	while (count < startPoint)
		count += in.skip(startPoint-count);

	yield();

	readloop:
	while ( count < size )
	{ 

		progress.setValue((int)(100*count/size)+1);
		yield();

		while (qpause) 
			pause();

		if (qbreak)
		{ 
			qbreak=false; 
			break readloop; 
		}

		in.read(parse12,0,12);

		if (parse12[0]!=0x53 || parse12[1]!=0x50) // find "SP"
		{
			if (!cBox[3].isSelected() && !miss)
				Msg("!> missing syncword @ "+count);
			miss=true;
			count++;
			in.unread(parse12,1,11);
			continue readloop;
		}

		if (!cBox[3].isSelected() && miss)
			Msg("!> found syncword @ "+count);

		in.unread(parse12,0,12);
		miss=false;

		packetlength = ((0xFF & parse12[10])<<8 | (0xFF & parse12[11])) + 10;
		packet = new byte[packetlength];

		in.read(packet);
		count += packetlength;

		source_pts = 0;
		for (int a=0; a<5; a++) // 5bytes for pts, maybe wrong
			source_pts |= (0xFFL & packet[2+a])<<(a*8);

		//DM15072004 081.7 int06 add, use add. time offset if not applied by class piddemux (e.g. ES)
		if (args[1].equals("-1"))
			source_pts += pts_offset;

		if (first_pts == -1)
			first_pts = source_pts;

		//DM15072004 081.7 int06 changed
		if (source_pts == pts_offset)
			source_pts = last_pts;
		else
			last_pts = source_pts;

		if (options[30]==1)
			System.out.println(" "+(count-packetlength)+"/ "+packetlength+"/ "+source_pts);

		if (ptsdata)
		{
			write = vptsdata ? false : true;

			rangeloop:
			while (vptsdata && v < vptsval.length)  //pic_start_pts must be in range ATM
			{ 
				if (source_pts < vptsval[v])
					break rangeloop;

				else if (source_pts == vptsval[v] || source_pts < vptsval[v+1])
				{
					write=true;
					break rangeloop;
				}

				v += 2;
				if (v < vptsval.length)
					time_difference += (vptsval[v] - vptsval[v-1]);
			}
		}
		else
			write = true;

		if (!vptsdata && time_difference == 0 && !RButton[13].isSelected())
			time_difference = source_pts;

		new_pts = source_pts - time_difference;

		if ((display_time = subpicture.picture.decode_picture(packet, 10, subpicture.isVisible(), new_pts, write, subpicture.isVisible())) < -2)
			Msg(subdecode_errors[Math.abs((int)display_time)] + " @ " + (count-packetlength) + ", dropping picture..");

		if (options[30] == 1)
			System.out.println("PTS: source " + Common.formatTime_1(source_pts / 90) + "(" + source_pts + ")" + " /new " + Common.formatTime_1(new_pts / 90) + "(" + new_pts + ")" + " / write: " + write + " / dec.state: " + display_time);

		if (display_time < 0)  //dvb_subpic
		{
			if (!DVBpicture)
				Msg("-> source is DVB Subtitle..");

			DVBpicture = true;

			if (display_time == -1) // -1 full data, -2 forced end_time
			{
				String num = "00000" + pics;
				String outfile_base = fparent + "_st" + num.substring(num.length() - 5);

				String key, object_id_str, outfile;
				int object_id;

				for (Enumeration e = BMP.getKeys(); e.hasMoreElements() ; )
				{
					key = e.nextElement().toString();
					object_id = Integer.parseInt(key);
					object_id_str = Integer.toHexString(object_id).toUpperCase();
					outfile = outfile_base + "p" + object_id_str;

					Bitmap bitmap = BMP.getBitmap(object_id);

					if (export_type == 0)  //.sup
						out.write( subpicture.picture.writeRLE(bitmap));

					else    //.son + .bmp
					{
						if (pics == 0)
						{
							String[] SONhead = Teletext.getSONHead(new File(subfile).getParent(), options[14]);

							for (int a=0; a < SONhead.length; a++) 
								print_out.println(SONhead[a]);
						}

						subpicture.picture.updateUserColorTable(bitmap);
						outfile = BMP.buildBMP_palettized(outfile, bitmap, subpicture.picture.getUserColorTable(), 256);

						options[39] += new File(outfile).length();

						int pgc_values = subpicture.picture.setPGClinks();

						// a change in color_links
						if ((0xFFFF & pgc_values) != (0xFFFF & last_pgc_set))
						{
							String pgc_colors = "";
							for (int a=0; a < 4; a++)
								pgc_colors += "" + (0xF & pgc_values>>>(a * 4)) + " ";

							print_out.println("Color\t\t(" + pgc_colors.trim() + ")");
						}

						// a change in alpha_links
						if ((0xFFFF0000 & pgc_values) != (0xFFFF0000 & last_pgc_set))
						{
							String pgc_alphas = "";
							for (int a=0; a < 4; a++)
								pgc_alphas += "" + (0xF & pgc_values>>>((4 + a) * 4)) + " ";

							print_out.println("Contrast\t(" + pgc_alphas.trim() + ")");
						}

						last_pgc_set = pgc_values;

						print_out.println("Display_Area\t(" + Common.adaptString(bitmap.getX(), 3) + " " + Common.adaptString(bitmap.getY(), 3) + " " + Common.adaptString(bitmap.getMaxX(), 3) + " " + Common.adaptString(bitmap.getMaxY(), 3) + ")");
						print_out.println(outfile_base.substring(outfile_base.length() - 4) + "\t\t" + Common.formatTime_2(bitmap.getInTime() / 90, options[14]) + "\t" + Common.formatTime_2((bitmap.getInTime() / 90) + (bitmap.getPlayTime() * 10), options[14]) + "\t" + new File(outfile).getName());
					}

					//Msg(subpicture.picture.getArea());
					//BMP.buildBMP_24bit(outfile, key);

					subpicture.newTitle(" / page id " + bitmap.getPageId() + " / picture " + pics + " -> in: " + Common.formatTime_1(new_pts / 90) + " duration: " + Common.formatTime_1(bitmap.getPlayTime() * 10));
				}

				if (!BMP.isEmpty())
					showExportStatus("pic's:", ++pics);

				BMP.clear();
			}
		}

		else if (write) //dvd_subpic
		{
			for (int a=0; a<8; a++)
				packet[2+a] = (byte)(0xFFL & new_pts>>>(a*8));

			//later, to allow overlapping on cut boundaries 
			//if (display_time > 0)
			//	packet = subpicture.picture.setTime(packet,display_time);

			out.write(packet);

			showExportStatus("pic's:", ++pics);
			subpicture.newTitle(" / picture "+pics+" -> in: " + Common.formatTime_1(new_pts / 90) + " duration: " + Common.formatTime_1(display_time / 90));

			//DM25072004 081.7 int07 add
			String str = subpicture.picture.isForced_Msg();
			if (str != null)
				Msg(str + ", from picture " + pics);
		}

		else
			subpicture.newTitle(" / picture not exported...");

		if (options[30]==1)
			System.out.println(" -> "+write+"/ "+v+"/ "+new_pts+"/ "+time_difference+"/ "+pics+"/ "+display_time);
  
	}

	in.close();

	print_out.flush();
	print_out.close();

	out.flush(); 
	out.close();

	if (args[1].equals("-1"))
		Msg("Subpicture PTS: first packet " + Common.formatTime_1(first_pts / 90) + ", last packet " + Common.formatTime_1(source_pts / 90));

	Msg(" " + pics + " subpictures written..");

	if (!DVBpicture && export_type == 1)
	{
		String renamed_file = subfile.substring(0, subfile.length() - 3) + "sup";
		Common.renameTo(subfile, renamed_file);
		subfile = renamed_file;
	}

	File subfile1 = new File(subfile); 

	if (pics==0) 
		subfile1.delete();
	else
	{ 
		if (DVBpicture && export_type == 0)
			options[39] += Ifo.createIfo(subfile, subpicture.picture.getUserColorTableArray());

		else if (DVBpicture && export_type == 1)
			options[39] += new File( BMP.write_ColorTable(fparent, subpicture.picture.getUserColorTable(), 256)).length();

		Msg("===> new File " + subfile);
		options[39] += subfile1.length();

		//DM15072004 081.7 int06 changed
		InfoAtEnd.add("SubPicture " + (NoOfPictures++) + ":\t" + pics + " subpictures\t" + infoPTSMatch(args, vptsdata, ptsdata) + "\t " + subfile1);
	}

	progress.setValue(100);
	yield();

	}  // end try
	catch (EOFException e1)
	{ 
		//DM25072004 081.7 int07 add
		Msg(" EOF reached in error: " + e1); 
	}
	catch (IOException e2)
	{ 
		//DM25072004 081.7 int07 add
		Msg(" IO error "+filename + " / " + e2); 
	}

	System.gc();

}   // end methode processSubpicture


//DM02032004 081.6 int18 new
public void rawsub(String args,String vptslog)
{
	String fchild = (newOutName.equals("")) ? (new File(args).getName()).toString() : newOutName;
	String fparent = ( fchild.lastIndexOf(".") != -1 ) ? workouts+fchild.substring(0,fchild.lastIndexOf(".")) : workouts+fchild;

	options[11]=1;
	String[] synchit = { args,"-1","sp",vptslog };
	processSubpicture(synchit);
	options[11]=0;
}

//DM30032004 081.6 int18 add
private void byteOrder(byte data[], int off, int len)
{
	byte value[] = new byte[len];
	System.arraycopy(data, off, value, 0, len);

	for (int a=0; a<len; a++)
		data[off+a] = value[len-1-a];

	value = null;
}

//30032004 081.6 int18 new
/****************************
 * LPCM stream *
 ****************************/
public void processLPCM(String[] args)
{
	String filename = args[0];
	long size = new File(filename).length();

	String fchild = (new File(args[0]).getName()).toString();
	String fparent = ( fchild.lastIndexOf(".") != -1 ) ? workouts+fchild.substring(0,fchild.lastIndexOf(".")) : workouts+fchild;

	String pcmfile = fparent + (options[11]==1 ? "_0": "") + ".wav";

	java.text.DateFormat sms = new java.text.SimpleDateFormat("HH:mm:ss.SSS");
	sms.setTimeZone(java.util.TimeZone.getTimeZone("GMT+0:00"));

	byte[] parse16 = new byte[16];
	byte[] packet = new byte[0];
	long count=0, startPoint=0, time_difference=0, display_time=0;
	long source_pts=0, new_pts=0, first_pts=-1, packet_pts=0;
	boolean vptsdata=false, ptsdata=false, write=false, miss=false, newformat=false;
	int x=0, samples=0, v=0, packetlength=0;
	options[47]=0;

	try 
	{

	PushbackInputStream in = new PushbackInputStream( new FileInputStream(filename),20);
	IDDBufferedOutputStream out = new IDDBufferedOutputStream( new FileOutputStream(pcmfile),2048000);

	Msg(" .. under development ..");

	Msg(filename + "  (" + size + " bytes)");
	progress.setString("check & synchronize " + filename);
	progress.setStringPainted(true);
	progress.setValue(0);

	long[] ptsval = {0}, ptspos = {0}, vptsval = {0};

	System.gc();

	if ( !args[3].equals("-1") )
	{
		if (options[30]==1) 
			System.out.print("\r-> loading video PTS logfile...");   

		int vlogsize = (int)(new File(args[3])).length() / 16;
		DataInputStream vbin = new DataInputStream(new BufferedInputStream(new FileInputStream(args[3]),655350));
		vptsval = new long[vlogsize]; 

		for ( int b = 0; b < vlogsize; b+=2 )
		{
			vptsval[b] = vbin.readLong(); 
			vptsval[b+1] = vbin.readLong();
			vbin.skip(16);
			if (options[30]==1) 
				System.out.println(" #s"+b+" _"+vptsval[b]+" #e"+(b+1)+" _"+vptsval[b+1]);
		}
		vbin.close();

		Msg("Video PTS: start 1.GOP "+sms.format(new java.util.Date(vptsval[0]/90))+", end last GOP "+sms.format(new java.util.Date(vptsval[vptsval.length-1]/90)));
		vptsdata=true;
	}

/*** 
	// too big caused by too many PTS, will overload memory
	// pts is here inluded in each packet instead
	if ( !args[1].equals("-1") )
	{
		if (options[30]==1) 
			System.out.print("\r-> loading LPCM PTS logfile...");   

		int logsize = (int)(new File(args[1])).length() / 16;
		DataInputStream bin = new DataInputStream(new BufferedInputStream(new FileInputStream(args[1]),655350));

		ptsval = new long[logsize+1]; 
		ptspos = new long[logsize+1];
		ptsval[logsize] = -1;  
		ptspos[logsize] = -1;
		int aa=0;

		for (int a=0; a<logsize; a++)
		{
			long ptsVal = bin.readLong();
			long ptsPos = bin.readLong();

			if (options[30]==1) 
				System.out.println(" #"+aa+"/"+a+" _"+ptsVal+" / "+ptsPos);

			if (aa > 0 && ptsVal <= ptsval[aa-1])
			{
				if (aa > 1 && Math.abs(ptsVal - ptsval[aa-2]) < 150000 && Math.abs(ptsVal - ptsval[aa-1]) > 500000)
				{
					aa--;
					if (options[30]==1)
						System.out.print(" <!^> ");
				}
				else
				{
					if (options[30]==1) 
						System.out.print(" <!v> ");
					continue;
				}
			}
			ptsval[aa] = ptsVal;
			ptspos[aa] = ptsPos;
			aa++;
		}

		if (aa<logsize)
		{
			Msg("!> "+(logsize-aa)+" PTS's discarded in stream");
			long tmp[][] = new long[2][aa];
			System.arraycopy(ptsval,0,tmp[0],0,aa);
			System.arraycopy(ptspos,0,tmp[1],0,aa);
			ptsval = new long[aa+1];
			System.arraycopy(tmp[0],0,ptsval,0,aa);
			ptsval[aa]= -1;
			ptspos = new long[aa+1];
			System.arraycopy(tmp[1],0,ptspos,0,aa);
			ptspos[aa]= -1;
		}
	
		yield();
		bin.close();

		Msg("LPCM PTS: first packet "+sms.format(new java.util.Date(ptsval[0]/90))+", last packet "+sms.format(new java.util.Date(ptsval[ptsval.length-2]/90)));
		ptsdata=true; 
	}
***/

	ptsdata = true;
	System.gc();

	//DM150702004 081.7 int06 changed, fix
	if (vptsdata && ptsdata)
	{
		//int jump = checkPTSMatch(vptsval, ptsval);
		//temp. check disabled
		int jump = 0;

		if (jump < 0)
		{
			Msg("-> !! video & LPCM PTS doesn't match at any time!");  
			vptsdata = false; 
			x = 0; 
		}

		else
			x = jump;


		/**
		timebase:
		while ( ptsval.length>2 && Math.abs(ptsval[x] - vptsval[0]) > 20000000 )
		{
			x++;
			if (x >= ptsval.length-1)
			{ 
				Msg("-> !! video & LPCM PTS doesn't match at any time!"); 
				vptsdata=false; 
				x=0; 
				break timebase; 
			}
		}
		**/
	}

	if (vptsdata && ptsdata)
	{
		Msg("-> adjusting LPCM at video-timeline ");
		time_difference = vptsval[0];
	}

	if (!vptsdata && ptsdata)
	{
		Msg("-> adjusting LPCM at its own timeline ");
		time_difference = 0;
	}

	if (ptsdata)
	{ 
		source_pts = ptsval[x]; 
		startPoint = ptspos[x]; 
	}

	//don't need it anymore
	ptsval = null;
	ptspos = null;

	while (count < startPoint)
		count += in.skip(startPoint - count);

	yield();


	out.write( Audio.getRiffHeader()); //wav header

	readloop:
	while ( count < size )
	{ 

		progress.setValue((int)(100 * count / size) + 1);
		yield();

		while (qpause) 
			pause();

		if (qbreak)
		{ 
			qbreak = false; 
			break readloop; 
		}

		in.read(parse16, 0, 16);  //special X header

		if (parse16[0] != 0x50 || parse16[1] != 0x43 || parse16[2] != 0x4D) // find "PCM"
		{
			if (!cBox[3].isSelected() && !miss)
				Msg("!> missing syncword @ "+count);

			miss=true;
			count++;
			in.unread(parse16, 1, 15);
			continue readloop;
		}

		if (!cBox[3].isSelected() && miss)
			Msg("!> found syncword @ "+count);

		miss=false;
		count += parse16.length;

		packet_pts = 0;
		for (int a=0; a<5; a++) // 5bytes for packet pts
			packet_pts |= (0xFFL & parse16[3+a])<<(a*8);

		if (packet_pts != 0)
			source_pts = packet_pts;

		packetlength = ((0xFF & parse16[8])<<8 | (0xFF & parse16[9])) - 6;
		packet = new byte[packetlength];  //raw sample data

		in.read(packet);
		count += packetlength;

		if (first_pts == -1)
			first_pts = source_pts;

		if (options[30]==1)
			System.out.println(" "+(count-packetlength)+"/ "+packetlength+"/ "+source_pts);

		if (Audio.LPCM_parseHeader(parse16, 10) < 0)
			continue readloop;

		if (Audio.LPCM_compareHeader() > 0 || samples == 0 )
			newformat=true;

		Audio.saveHeader();

		if (ptsdata)
		{
			write = vptsdata ? false : true;

			rangeloop:
			while (vptsdata && v < vptsval.length)  //sample_start_pts must be in range ATM
			{ 
				if (source_pts < vptsval[v])
					break rangeloop;

				else if (source_pts == vptsval[v] || source_pts < vptsval[v+1])
				{
					write=true;
					break rangeloop;
				}

				v += 2;
				if (v < vptsval.length)
					time_difference += (vptsval[v] - vptsval[v-1]);
			}
		}
		else
			write = true;

		if (write)
		{
			new_pts = source_pts - time_difference;

			if (newformat)
			{
				if (options[47] < 100) 
					Msg("=> src_audio: "+Audio.LPCM_displayHeader()+" @ "+sms.format(new java.util.Date( (long)(new_pts / 90.0f))));

				else if (options[47] == 100) 
					Msg("=> src_audio: stop displaying, more than 100 audio mode changes in one file reduce work speed");

				options[47]++;
				yield();
				newformat = false;
			}

			for (int a=0; a<packet.length; a+=2) // intel order
				byteOrder(packet, a, 2);

			if ((packet.length & 1) != 0)
				Msg("!> packet not WORD aligned");

			out.write(packet);
			samples++;
			//showExportStatus("packs:", samples);
			showExportStatus("writing..");
		}
		else
			showExportStatus("paused..");

		if (options[30]==1)
			System.out.println(" -> "+write+"/ "+v+"/ "+new_pts+"/ "+time_difference+"/ "+samples+"/ "+display_time);
  
	}

	in.close();
	out.flush(); 
	out.close();

	if (args[1].equals("-1") || ptsdata)
		Msg("LPCM PTS: first packet "+sms.format(new java.util.Date(first_pts/90))+", last packet "+sms.format(new java.util.Date(source_pts/90)));

	Msg(" " + samples + " packs written..");

	File pcmfile1 = new File(pcmfile); 

	if (samples==0) 
		pcmfile1.delete();
	else
	{ 

		Audio.fillRiffHeader(pcmfile); //update riffheader
		Msg("===> new File " + pcmfile);
		options[39] += pcmfile1.length();

		//DM15072004 081.7 int06 changed
		InfoAtEnd.add("LPCM " + (NoOfPictures++) + ":\t" + samples + " packs\t" + infoPTSMatch(args, vptsdata, ptsdata) + "\t " + pcmfile1);
	}

	yield();

	}  // end try
	catch (EOFException e1)
	{ 
		//DM25072004 081.7 int07 add
		Msg(" EOF reached in error: " + e1); 
	}
	catch (IOException e2)
	{ 
		//DM25072004 081.7 int07 add
		Msg(" IO error: "+filename + " / " + e2); 
	}

	System.gc();

}   // end methode processLPCM

//DM30032004 081.6 int18 add
public void showExportStatus(String str)
{
	audiostatusLabel.setText(str);
}

public void showExportStatus(String str, int value)
{
	audiostatusLabel.setText(str + " " + value);
}


/********************
 * check pure audio *
 ********************/
public void rawaudio(String args,String vptslog, String type) { //DM30122003 081.6 int10 changed
	String fchild = (newOutName.equals("")) ? (new File(args).getName()).toString() : newOutName;
	String fparent = ( fchild.lastIndexOf(".") != -1 ) ? workouts+fchild.substring(0,fchild.lastIndexOf(".")) : workouts+fchild;

	logAlias(vptslog,fparent+".pts");
	options[11]=1;
	String[] synchit = { args,fparent+".pts",type,vptslog };
	mpt(synchit);      /* audiofile goes to synch methode */
	new File(synchit[1]).delete();
	options[11]=0;
}

/********************
 * check pure video *
 ********************/
public String rawvideo(String args) {

	String logfile = "-1";
	boolean valid=false;
	byte[] SEQUENCE_END_CODE = { 0,0,1,(byte)0xb7 };
	byte[] vgl = new byte[4];
	String[] videoext = { ".mpv",".mpv",".m1v",".m2v" };



	try 
	{
	long filelength = (new File(args)).length();
	String fchild = (newOutName.equals("")) ? (new File(args).getName()).toString() : newOutName;
	String fparent = ( fchild.lastIndexOf(".") != -1 ) ? workouts+fchild.substring(0,fchild.lastIndexOf(".")) : workouts+fchild;

	/*** split part ***/
	fparent += (options[18]>0) ? "("+options[19]+")" : "_0" ;

	progress.setString("analyze video stream file  "+fchild);
	progress.setStringPainted(true);
	progress.setValue(0);
	if (options[30]==1) 
		System.out.println(" starting check of video stream file...");
	yield();

	PushbackInputStream in = new PushbackInputStream(new FileInputStream(args), 4);

	IDDBufferedOutputStream vstream = new IDDBufferedOutputStream( new FileOutputStream(fparent+".s1"), bs);
	if (cBox[34].isSelected()) //DM24112003 081.5++ mpeg2schnitt
		vstream.InitIdd(fparent,1);

	DataOutputStream vlog = new DataOutputStream( new FileOutputStream(fparent+".s1.pts") ); 
	ByteArrayOutputStream vbuffer = new ByteArrayOutputStream();

	byte[] vptsbytes = new byte[16];
	byte[] vload = new byte[0];
	boolean first = true;
	clv = new int[10];
	long pos = 0;
	options[5] = 262143;
	options[6] = 0;
	options[7] = 0;
	options[8] = -10000;
	options[12] = 50;
	options[41] = 0;
	options[50] = 0;
	MPGVideotype = 0;
	CUT_BYTEPOSITION = 0;

	int load = bs/2;
	int[] fps_tabl2 = {0,3753,3750,3600,3003,3000,1800,1501,1500,0,0,0,0,0,0,0};
	PureVideo=true;
	long pts=0;
	byte[] vdata;

	/*** d2v project ***/
	if (cBox[29].isSelected() || cBox[30].isSelected()) {
		String[] d2vopt = new String[d2vfield.length];
		for (int x=0;x<d2vfield.length;x++) 
			d2vopt[x] = d2vfield[x].getText();
		d2v.setOptions(d2vopt);
		d2v.Init(fparent);
	}

	/*** split skipping first ***/
	/** if you do so, there's no common entry point with audio anymore
	** therefor only one inputfile allowed **
	if (options[18]>0) {
		long startPoint = options[20]-(comBox[25].getSelectedIndex()*1048576L); //go back for overlapping output
		if (pos < startPoint) 
			startPoint = pos;
		while (pos < startPoint)
			pos += in.skip(startPoint-pos);
	}

	// jump to first cut-in point
	if (comBox[17].getSelectedIndex()==0 && cutcount==0 && ctemp.size()>0) {
		long startPoint = Long.parseLong(ctemp.get(cutcount).toString());
		while (pos < startPoint)
			pos += in.skip(startPoint-pos);
	}
	**/

	//DM31052004 081.7 int03 add
	boolean lead_sequenceheader = false;

	videoloop:
	while (pos < filelength) {

		while (qpause) 
			pause();

		if (qbreak){
			qbreak=false;
			break videoloop;
		}

		/*** cut end reached ***/
		if ((int)options[37]+20 < origframes) 
			break videoloop;

		load = (filelength-pos < (long)load) ? (int)(filelength-pos) : load;
		vload = new byte[load];
		in.read(vload);
		pos += load;

		progress.setValue((int)(100*(pos)/filelength)+1);
		yield();

		int mark =0;

		arrayloop:
		for (int a=0; a<vload.length-3; a++) {

			if (comBox[17].getSelectedIndex()==0 && ctemp.size()>0) {
				if (cutcount==ctemp.size() && (cutcount&1)==0)
					if (pos+a > Long.parseLong(ctemp.get(cutcount-1).toString())){
						break videoloop;
					}


			}


			//if ( vload[a]!=0 || vload[a+1]!=0 || vload[a+2]!=1 ) 
			//	continue arrayloop;

			//DM04062004 081.7 int04 add
			if (vload[a + 2] != 1)
			{
				if (vload[a + 2] != 0)
					a += 2;

				else if (vload[a + 1] != 0)
					a++;

				continue arrayloop;
			}
			else if (vload[a + 1] != 0)
			{
				a++;
				continue arrayloop;
			}
			else if (vload[a] != 0) 
			{
				continue arrayloop;
			}

			//DM06022004 081.6 int15 changed
			int start_code = 0xFF & vload[a+3];

			//DM31052004 081.7 int03 add
			if (start_code == 0xB8 && lead_sequenceheader)
			{
				lead_sequenceheader = false;
				a += 8;
				continue arrayloop;
			}

			if (start_code==0xB3 || start_code==0xB7 || start_code==0xB8)
			{
				//DM31052004 081.7 int03 add
				if (start_code == 0xB3)
					lead_sequenceheader = true;

				vbuffer.write(vload,mark,a-mark);
				mark = a;
				CUT_BYTEPOSITION = pos-load+mark;

				if (!first)
				{
					vdata = vbuffer.toByteArray();
					vbuffer.reset();
         
					firstframeloop:
					for (int b=0; b<6000 && b<vdata.length-5;b++)  //DM24112003 081.5++ mod
					{
						if ( vdata[b]!=0 || vdata[b+1]!=0 || vdata[b+2]!=1 )
							continue firstframeloop;

						if ( vdata[b+3]==(byte)0xb3 )
							videotimecount = fps_tabl2[15&vdata[b+7]]; // rawvideo 052a
						else if ( vdata[b+3]==0 )
						{
							pts = (origframes==0) ? ((255&vdata[b+4])<<2 | (192&vdata[b+5])>>>6) * videotimecount : options[40] + (((255&vdata[b+4])<<2 | (192&vdata[b+5])>>>6) * videotimecount);

							for (int c=0;c<8;c++) 
								vptsbytes[7-c] = (byte)(255L&pts>>>(c*8));

							break firstframeloop;
						}
					}
					goptest( vstream, vdata, vptsbytes, vlog, fparent);
				}
				vbuffer.reset();

				if (start_code==0xB7) 	// sequence_end_code detected
				{
					Msg("-> skip sequence_end_code following GOP#"+clv[6]+" @ "+(pos-load+a));
					a += 4;
					mark = a;
				}
				options[20] = pos-load+a;  // split marker sequence_gop start

				// split size reached
				if ( options[18]>0 && options[18]<options[41] ) 
					break videoloop;

				/****** d2v split reached *****/
				if ( cBox[30].isSelected() && options[50] > options[51] )
				{
					int part = d2v.getPart()+1;
					String newpart = fparent+"["+part+"].mpv";

					/*** sequence end code ***/
					if ( (1L&options[26])!=0 && options[32]==1 && options[7]>0 )
					{
						vstream.write(SEQUENCE_END_CODE);
						options[39]+=4;
					}
					options[41]+=4;
					vstream.flush();
					vstream.close();
					System.gc();

					vstream = new IDDBufferedOutputStream( new FileOutputStream(newpart), bs-1000000);
					if (cBox[34].isSelected())  //mpeg2schnitt
						vstream.InitIdd(newpart,1);
					
					d2v.setFile(newpart);
					options[50]=0;
				}

				if (start_code==0xB8)
				{
					options[13]=0;
					if (options[19]>0) 
						first=false;
				}
				else
				{
					options[13]=1;
					first=false;
				}

				//int diff = ( vload.length-mark < 5000 ) ? (vload.length-mark-4) : 5000;
				//if (diff>4) {

				//DM04022004 081.6 int14 fix
				int diff = ( vload.length-mark-4 < 2500 ) ? (vload.length-mark-4) : 2500;
				if (diff>0)
				{
					vbuffer.write(vload,mark,diff);
					a += diff;
					mark = a;
				}
			}
		}

		/****** file end reached *****/
		if ( pos>=filelength-1 ) { 
			if ( options[18]>0 ) 
				options[21]=-100;
			break videoloop;
		}

		//DM04022004 081.6 int14 fix
		int diff = vload.length < 3 ? vload.length : 3;
		vbuffer.write(vload,mark,vload.length-mark-diff);
		in.unread(vload,vload.length-diff,diff); 
		pos-=diff;

		//vbuffer.write(vload,mark,vload.length-3-mark);
		//in.unread(vload,vload.length-3,3); 
		//pos-=3;

	}

	/*** d2v project ***/
	if (cBox[29].isSelected() || cBox[30].isSelected()) 
		d2v.write(options[50],options[7]);


	/*** sequence end code ***/
	if ( (1L&options[26])!=0 && options[32]==1 && options[7]>0 ) {
		vstream.write(SEQUENCE_END_CODE);
		options[39]+=4;
	}

	in.close();
	vstream.flush();
	vstream.close();
	vlog.flush();
	vlog.close();

	//DM12042004 081.7 int01 changed
	Msg("");
	Msg("-> video: fr/ ct/ 1p/ cg/ og/ dg = " + options[7] + "/ " + clv[0] + "/ " + clv[1] + "/ " + clv[2] + "/ " + clv[3] + "/ " + clv[4]);

	showOutSize();

	if (options[30]==1) 
		System.out.println(" EOD ");

	File newfile = new File(fparent+".s1");



	String videofile="";
	if ( newfile.length()<20 ) 

		newfile.delete();
	else if ( (1L&options[26])==0 ) 
		newfile.delete();
	else {
		int ot = (cBox[32].isSelected() || cBox[29].isSelected() || cBox[30].isSelected()) ? 0 : 2;
		videofile=fparent+videoext[MPGVideotype+ot];
		newfile = new File(videofile);

		if (newfile.exists())
			newfile.delete();

		Common.renameTo(new File(fparent + ".s1"), newfile); //DM13042004 081.7 int01 changed
		//new File(fparent+".s1").renameTo(newfile);

		logfile = fparent+".s1.pts";
		setvideoheader(videofile,logfile);
	}
	if (cBox[34].isSelected()){  //DM24112003 081.5++
		if (new File(videofile).exists())
			vstream.renameVideoIddTo(fparent);
		else
			vstream.deleteIdd();
	}

	}
	catch (IOException e) {
		Msg("stopped...pure video access error"+e);
	}

	// options[14]=0;
	options[15]=0;
	System.gc();

	return logfile;
}



}  // end main thread class work



/***************************************************************************
 * skip leading bytes before first valid startcodes and return fixed array *
 ***************************************************************************/
public static byte[] searchHeader(byte[] data, int type, int overhead) {
	int len=data.length-overhead, start=9+(0xFF&data[8]), end=len-3, s=start;
	boolean found=false;
	byte[] newdata = new byte[0];

	packloop:
	for (; s<end; s++) {
		if (type==3) {
			if ( data[s]!=0 || data[s+1]!=0 || data[s+2]!=1 || data[s+3]!=(byte)0xb3) 
				continue packloop;
			found=true;
			break;
		} else if (type==0) {
			//R_one18122003 081.6 int07 changed
			if ((data[s]!=0xB || data[s+1]!=0x77) && 
				(data[s]!=0x7F || data[s+1]!=(byte)0xFE || data[s+2]!=(byte)0x80 || data[s+3]!=1)) 
				continue packloop;
			found=true; 
			break;
		} else if (type==2) {
			if ( data[s]!=(byte)0xFF || (0xF0&data[s+1])!=0xF0)
				continue packloop;
			found=true; 
			break;
		}
	}



	if (!found) 
		return newdata;

	newdata = new byte[len-s+start+overhead];
	System.arraycopy(data,0,newdata,0,start);
	System.arraycopy(data,s,newdata,start,len-s+overhead);
	newdata[4]=(byte)((newdata.length-6-overhead)>>>8);
	newdata[5]=(byte)(0xFF&(newdata.length-6-overhead));

	return newdata;
}


/************************
* gop changing/testing *
************************/
public static void goptest(IDDBufferedOutputStream vseq, byte[] gop, byte[] pts, DataOutputStream log, String dumpname)
{

	//DM14092003+ fix
	if (gop.length<12){
		Msg("!> GOP#"+(clv[6])+" - lack of data, ignored..");
		return;
	}

	//DM11102003+ 081.5++ change
	if (pts.length==0) {
		long npts=0, thisTC=0, diff=0, ref=0;
		int p=0;

		loop:
		for (int a=0;a<gop.length-3;a++) {
			if (gop[a]!=0 || gop[a+1]!=0 || gop[a+2]!=1)
				continue loop;

			if ((0xFF&gop[a+3])==0xB8) { //DM111003  081.5++
				/** options[8] ist ende PTS vom letzten GOP = beginn dieses Gop
				wie bei ttx die diff merken zw. PTS und TC zu beginn, 
				dann vergleichen und startpts neu setzen,
				NUR wenn TC genau past zur erwarteten PTS, dann nehm wa den (TC 0 was dann)
				**/
				thisTC = 90L* (3600000L*((0x7C&gop[a+4])>>>2) + 
					60000L*((3&gop[a+4])<<4 | (0xF0&gop[a+5])>>>4) +
					1000L* ((7&gop[a+5])<<3 | (0xE0&gop[a+6])>>>5) +
					(long)(((0x1F&gop[a+6])<<1 | (0x80&gop[a+7])>>>7)*(double)(options[14]/90.0f)) );
				a+=6;
				continue loop;
			} else if (gop[a+3]==0) {
				p=a;
				ref=options[14]*((0xFF&gop[a+4])<<2 | (0xC0&gop[a+5])>>>6);
				npts = options[8]+ref;
				break loop;
			}
		}
		diff = thisTC-options[53];

		if (diff>=0 && diff<27000000){  // TC diff >=0ms <5min
			npts = options[54]+diff+ref;
			Msg("!> GOP#"+clv[6]+" has no PTS, use GOP TC for sync");
		}else
			Msg("!> GOP#"+clv[6]+" has no PTS, use last PTS for sync");

		pts = new byte[16];
		for (int c=0;c<8;c++) {
			pts[7-c] = (byte)(0xFF& npts>>>(c*8));
			pts[15-c] = (byte)(0xFF& (long)p>>>(c*8));
		}
		options[53]=thisTC;
	}
	//DM11102003-

	/* vpts[0] = pts, vpts[1] = for next frame following this byteposition in sequ_array */
	long[][] vpts = new long[2][pts.length/16];
	for (int a=0; a<(pts.length/16); a++) {
		for (int b=0; b<8; b++) {
			vpts[0][a] |= (255L & pts[(a*16)+b])<<((7-b)*8);
			vpts[1][a] |= (255L & pts[(a*16)+8+b])<<((7-b)*8);
		}
	}

	//DM12042004 081.7 int01 add , horrible nebula PTS stuff
	if (vpts[0].length > 1 && Math.abs(vpts[0][vpts[0].length-1] - vpts[0][0]) < 100)
	{
		long a1 = vpts[0][0], a2 = vpts[1][0];
		vpts = new long[2][1];
		vpts[0][0] = a1;
		vpts[1][0] = a2;

		clv[8]++;
	}

	if (options[30]==1){  //DM111003 081.5++
		System.out.println("\ngop"+clv[6]+"/tc_o53 "+options[53]+"/lp_o54 "+options[54]+"/lp_o8 "+options[8]+"/lp_o40 "+options[40]);
		for (int a=0; a<vpts[0].length; a++)
			System.out.println("p"+a+" "+vpts[0][a]+"/ "+vpts[1][a]);
	}

	java.text.DateFormat sms = new java.text.SimpleDateFormat("HH:mm:ss.SSS");
	sms.setTimeZone(java.util.TimeZone.getTimeZone("GMT+0:00"));
	java.util.Calendar cal = java.util.Calendar.getInstance();

	try 
	{
	String[] aspratio = {"res.","1.000 (1:1)","0.6735 (4:3)","0.7031 (16:9)","0.7615 (2.21:1)","0.8055","0.8437","0.9375","0.9815","1.0255","1.0695","1.1250","1.1575","1.2015","res." };
	String[] fps_tabl1 = {"forbidden fps","23.976fps","24fps","25fps","29.97fps","30fps","50fps","59.94fps","60fps","n.def.","n.def.","n.def.","n.def.","n.def.","n.def.","n.def."};
	int[] fps_tabl2 = {0,3753,3750,3600,3003,3000,1800,1501,1500,0,0,0,0,0,0,0};
	int tref=0, maxtref=0, frame=-1, newframes=0, progressive=0;
	int closedgop=0, smark=0, trefcheck=0, vbvdelay=65535, s=0, lastframes=(int)options[7];

	boolean start=false, last=false, changegop=false, writeframe=true, iframe=false, d2vinsert=false, mpegtype=false;
	long startpts, lastpts; //DM151003 081.5++
	startpts = lastpts = options[8]==-10000 ? -20000 : options[8];  //DM151003 081.5++
	long videotimes=options[42];

	ArrayList infos = new ArrayList(), newcut = new ArrayList();
	ByteArrayOutputStream gopbuffer = new ByteArrayOutputStream();
	ByteArrayOutputStream frametypebuffer = new ByteArrayOutputStream();
	int frametype=0;
	byte d2vframerate = 0;

	if (options[13]==1) 
		frametypebuffer.write((byte)0x88);
	else 
		frametypebuffer.write((byte)0x80);

	if (options[18]>0 && options[13]==0 && options[7]==0) {
		byte[] newgop = new byte[headerrescue.length+gop.length];
		System.arraycopy(headerrescue,0,newgop,0,headerrescue.length);
		System.arraycopy(gop,0,newgop,headerrescue.length,gop.length);
		gop = new byte[newgop.length];
		gop=newgop; 
		newgop=null;
		options[13]=1;
		for (int a=0; a<vpts[1].length; a++) 
			vpts[1][a]+=headerrescue.length;

	} else if (!ctemp.isEmpty() && options[13]==0 && !bool) {
		byte[] newgop = new byte[headerrescue.length+gop.length];
		System.arraycopy(headerrescue,0,newgop,0,headerrescue.length);
		System.arraycopy(gop,0,newgop,headerrescue.length,gop.length);
		gop = new byte[newgop.length];
		gop=newgop; 
		newgop=null;
		options[13]=1;
		for (int a=0; a<vpts[1].length; a++) 
			vpts[1][a]+=headerrescue.length;

	} else if (options[13]==0 && cBox[27].isSelected()) {  // ** 0.62c Seqhead for each gop
		byte[] newgop = new byte[headerrescue.length+gop.length];
		System.arraycopy(headerrescue,0,newgop,0,headerrescue.length);
		System.arraycopy(gop,0,newgop,headerrescue.length,gop.length);
		gop = new byte[newgop.length];
		gop=newgop; 
		newgop=null;
		options[13]=1;
		for (int a=0; a<vpts[1].length; a++) 
			vpts[1][a]+=headerrescue.length;
	}

	/* header check */
	if (options[13]==1) {

		options[14] = fps_tabl2[15 & gop[s+7]];  // framerateconstant
		options[15] = 16*1024*( (31 & gop[s+10])<<5 | (248 & gop[s+11])>>>3 );

		if ( ((15&gop[s+5])<<8 | gop[s+6]) <480 ) 
			options[12]=options[12]/2; 

		String[] vbasics = { ""+((255&gop[s+4])<<4 | (240&gop[s+5])>>>4),""+((15&gop[s+5])<<8 | (255&gop[s+6])),(fps_tabl1[15&gop[s+7]]),aspratio[(255&gop[s+7])>>>4] };
		clv[7] = ((255&gop[s+7])>>>4)-1;

		if (newvideo || options[7]==0) {
			infos.add("-> video basics: "+vbasics[0]+"*"+vbasics[1]+" @ "+vbasics[2]+" @ "+vbasics[3]+" @ "+( ((255&gop[s+8])<<10 | (255&gop[s+9])<<2 | (192 & gop[s+10])>>>6)*400  )+"bps, vbvBuffer "+( (31&gop[s+10])<<5 | (248&gop[s+11])>>>3 ));
			if (options[7]==0) {
				d2vinsert=true;
				d2vframerate=gop[s+7];
			}
			VBASIC = vbasics;
		}

		if (!java.util.Arrays.equals(VBASIC,vbasics)) {
			Msg("GOP#"+clv[6]+", new format in leading sequenceheader detected:");
			Msg("-> video basics: "+vbasics[0]+"*"+vbasics[1]+" @ "+vbasics[2]+" @ "+vbasics[3]+" @ "+( ((255&gop[s+8])<<10 | (255&gop[s+9])<<2 | (192 & gop[s+10])>>>6)*400  )+"bps, vbvBuffer "+( (31&gop[s+10])<<5 | (248&gop[s+11])>>>3 ));
			VBASIC = vbasics;
		}
		s=12;
	}

	clv[6]++;
	int prog_seq=-1;
	int[] frt = { 5,50,20,5,5,5,5,5 };
	String frT[] = { "0","I","P","B","D","5","6","7" };
	boolean error=false, broken_link=false;
	ArrayList newPics=new ArrayList(); //DM05112003 081.5++
	long TC=0, lTC=options[53]; //DM12112003 081.5++


	/* gop check */
	goploop:
	for ( ; s<(gop.length-10); s++ )
	{
		//DM04062004 081.7 int04 add
		if (gop[s + 2] != 1)
		{
			if (gop[s + 2] != 0)
				s += 2;

			else if (gop[s + 1] != 0)
				s++;

			continue goploop;
		}
		else if (gop[s + 1] != 0)
		{
			s++;
			continue goploop;
		}
		else if (gop[s] != 0) 
		{
			continue goploop;
		}


		/****+ 0x000001 *****/
		//if ( gop[s]!=0 || gop[s+1]!=0 || gop[s+2]!=1 ) 
		//	continue goploop;

		else if ((0xF0 & gop[s+3]) == 0xE0)  //DM20032004 081.6 int18 add -- inofficial! shit progdvb_data check
		{
			int drop_length = 4;	

			if (gop[s+4] == 0 && gop[s+5] == 0 )
			{
				drop_length += 5 + (0xFF & gop[s+8]);

				if (s + drop_length >= gop.length)
					drop_length = gop.length - s;

				java.util.Arrays.fill( gop, s, s + drop_length, (byte)0);

				if (!cBox[3].isSelected())
					Msg("!> GOP#" + (clv[6]-1) + ", PES_header and extension found in ES, filled with zero... (GOP offs. " + s +")");
			}
			else
			{
				java.util.Arrays.fill( gop, s, s + drop_length, (byte)0);

				if (!cBox[3].isSelected())
					Msg("!> GOP#" + (clv[6]-1) + ", PES_header found in ES, filled with zero... (GOP offs. " + s +")");
			}

			s += drop_length - 1;
			continue goploop;
		}

		else if (!mpegtype && gop[s+3]==(byte)0xb5 && gop[s+4]>>>4==1) {   /*** 0xb5 MPEG-2 extension ***/

			MPGVideotype=1; 
			mpegtype=true; 
			prog_seq=s+5;
			s+=3;
			//DM04032004 081.6 int18 add
			if (options[13]==1 && frametypebuffer.size()==1)
			{
				frametypebuffer.reset();
				frametypebuffer.write((byte)(8 | (8 & gop[prog_seq])<<4));
			}

		} else if ( (255 & gop[s+3])==0xb8 ) {   /*** 0xb8 set timecode ***/

			closedgop=s+7;
			writeframe=true;
			broken_link=(0x20&gop[s+7])!=0?true:false; 

			//DM12112003  gop TC 081.5++ 
			TC = 90L* (3600000L*((0x7C&gop[s+4])>>>2) + 
				60000L*((3&gop[s+4])<<4 | (0xF0&gop[s+5])>>>4) +
				1000L* ((7&gop[s+5])<<3 | (0xE0&gop[s+6])>>>5) +
				(long)(((0x1F&gop[s+6])<<1 | (0x80&gop[s+7])>>>7)*(double)(options[14]/90.0f)) );
			if (Math.abs(TC-options[53])<options[14] || origframes==0)
				options[53]=TC;

			if (options[30]==1) //DM131003 081.5++
				System.out.println("\n//b8 "+TC+"/ "+Integer.toHexString((0x80&gop[s+4]) | (0x7F&gop[s+7]))+"/ "+s+"/ "+options[13]);
			if (options[13]==1)
				headerrescue = new byte[s];
			System.arraycopy(gop,0,headerrescue,0,s); //!!copy ever sequ_hader??

			java.util.Date videotime = new java.util.Date((long)((options[7])*(double)(options[14]/90.0f)));
			cal.setTimeZone(java.util.TimeZone.getTimeZone("GMT+0:00"));
			cal.setTime(videotime);
			int vh = cal.get(11), vm = cal.get(12), vs = cal.get(13), vf = (cal.get(14) / ((int)options[14]/90)) ; // earlier versions +1 
			gop[s+4] = (byte)( (128 & gop[s+4]) | vh<<2 | vm>>>4  );
			gop[s+5] = (byte)( (15 & vm)<<4 | 8 | vs>>>3 );
			gop[s+6] = (byte)( (7 & vs)<<5 | vf>>>1 );
			gop[s+7] = (byte)( 127 & gop[s+7] | vf<<7 );

			s+=6;

		} else if ( gop[s+3]==0 ) {   /* 0x0 new frame */

			tref = ((255 & gop[s+4]) << 2) | (192 & gop[s+5])>>>6;  // timerefence of picture
			frametype = (56&gop[s+5])>>>3;
			if (frametype==0 || frametype>4) {
				Msg("!> wrong frametype "+frametype+", tref "+tref);
				error=true;
			}
			newPics.add(""+(tref<<4|frametype));

			// P-frames for cut in gop 
			//if (frametype==2) {
			//	long[] cutdata = { origframes,s };
			//	newcut.add(cutdata);
			//}

			/*** vbv delay to 0xffff ***/
			if (options[0]==0 && options[3]==0 && options[23]>0) {
				gop[s+5] |= 7; 
				gop[s+6] |= 255; 
				gop[s+7] |= 248;
			} 
 
			if (tref > maxtref) 
				maxtref=tref;
			if (options[30]==1) 
				System.out.println(frame+"/ "+maxtref+"/ "+tref+"/ "+s+" * "+frT[frametype]+"/ "+gop.length);

			if (!start && s >= vpts[1][0]) {
				startpts = vpts[0][0] - (long)(options[14]*tref); 
				start=true;
			} else if (!last && s >= vpts[1][vpts[1].length-1]) {
				lastpts = vpts[0][vpts[0].length-1] - (long)(options[14]*tref); 
				last=true;
			}

			/***** determine cuts ******/
			if ( frame==-1 && frametype==1 ) {

				/***** determine vbv-delay of I-frame *****/
				vbvdelay = (7 & gop[s+5])<<13 | (255 & gop[s+6])<<5 | (248 & gop[s+7])>>>3;
				iframe=true;

				//DM08022004 081.6 int16 new
				if (options[7]==0)
					infos.add("-> starting export of video data @ GOP#"+(clv[6]-1));

				//if ( tref>0 && Math.abs(startpts-options[8]) > (int)options[14]) {
				//DM151003 081.5++ discard B-Frames also if broken_link flag is set
				if (tref>0 && (Math.abs(startpts-options[8])>options[14] || broken_link)) {
					gop[s+4]=0;
					gop[s+5] &= 0x3F;  /* set first I-Frame's tref to 0 */
					gop[closedgop] |= 0x40;
					if (broken_link)
						gop[closedgop] &= ~0x20;
					gopbuffer.write(gop,0,s); 

					smark=s;

					//DM03022004 081.6 int14 new
					if (options[7]>0)
						infos.add("!> PTS difference of "+(startpts-options[8])+" ("+sms.format(new java.util.Date((startpts-options[8])/90))+") to last GOP#"+(clv[6]-2)+" detected"+(broken_link?" (broken_link corrected)":""));

					infos.add("!> dropping useless B-Frames @ GOP#"+(clv[6]-1)+" / new Timecode "+sms.format(new java.util.Date((long)((options[7])*(double)(options[14]/90.0f)) )));
					options[7]-=tref;
					newframes-=tref;
					trefcheck=tref;
					changegop=true;
					clv[0]++;  //cut
				}
			}

			/**** recalculate tref, timecode, frames + delete b-frames *****/
			if (frame>-1 && changegop) {
				if (writeframe)
					gopbuffer.write(gop,smark,s-smark);

				smark=s;
				/*** skip b-frame ***/
				if ( trefcheck > tref ) 
					writeframe = false;
				else {
					writeframe = true;
					gop[s+4] = (byte)( (tref-trefcheck)>>>2);
					gop[s+5] = (byte)( (63 & gop[s+5]) | ((tref-trefcheck)<<6));
				}
			}

			/*** P-frames for cut in gop ***/
			if (frametype==2) {
				if (!changegop) {
					long[] cutdata = { origframes,s };
					newcut.add(cutdata);
				} else {
					long[] cutdata = { origframes,gopbuffer.size() };
					newcut.add(cutdata);
				}
			}

			origframes++;
			options[7]++;
			frame++;
			newframes++;
			progressive=0x80;         /* 0xb5 interlaced extension */

			for ( int a=s+6; a<s+15 && a+8<gop.length; a++ ) {
				if ( gop[a]!=0 || gop[a+1]!=0 || gop[a+2]!=1 || gop[a+3]!=(byte)0xb5 || (0xF0&gop[a+4])!=0x80) 
					continue;





				progressive = (0x80 & gop[a+8]);

				if (cBox[31].isSelected()) 
					gop[a+8] |= (byte)0x80;  // mark as progressiv
				else if (cBox[44].isSelected()) 
					gop[a+8] &= (byte)~0x80;  // mark as interlaced

				if (cBox[45].isSelected()) 
					gop[a+7] ^= (byte)0x80;  // toggle top field first

				break;
			}

			if (iframe || !changegop || (changegop && writeframe)) 
				frametypebuffer.write((byte)(frametype | progressive));

			//s += (iframe) ? 950 : (int)options[12];

			s += 8; //DM14122003 081.6 int06  // slices B min 5, I min 50, p min 25
			iframe = false;
		}
	}

	/**** put the rest of data into buffer, if gop was changed *****/
	if (changegop) {
		if (writeframe)  
			gopbuffer.write(gop,smark,gop.length-smark);
		gop = gopbuffer.toByteArray();
		gopbuffer.reset();
		changegop=false;
	}
	if (prog_seq!=-1 && prog_seq<gop.length) {
		if (cBox[31].isSelected()) 
			gop[prog_seq] |= (byte)8;  // mark as progressiv_seq
		else if (cBox[44].isSelected()) 
			gop[prog_seq] &= (byte)~8;  // unmark ^
	} 

	if (vpts[0].length < 2) {
		lastpts = startpts; 
		clv[1]++;
	}

	if (firstVideoPTS==-1) //DM17012004 081.6 int11 new
		firstVideoPTS=startpts;

	//DM05112003 081.5++
	int Pics[] = new int[newPics.size()];
	for (int a=0; a<Pics.length; a++)
		Pics[a]=Integer.parseInt(newPics.get(a).toString());
	int newTref[] = new int[Pics.length];
	java.util.Arrays.fill(newTref,-1);

	if (Pics.length==0) //DM30122003 081.6 int10 changed,fix
	{
		Msg("!> GOP#" + (clv[6]-1) + " contains no frames");
		error=true;
	}

	if (Pics.length > 0 && (Pics[0] & 0xF) != 1) //DM30032004 081.6 int18 changed
		Msg("!> GOP#" + (clv[6]-1) + " doesn't start with an I-Frame at tref=0");

	for (int a=0; !error && a<Pics.length; a++){
		int Tref=Pics[a]>>>4;
		if (Tref<0 || Tref>Pics.length-1 || newTref[Tref]!=-1){
			error=true;
			break;
		}
		newTref[Tref]=Pics[a];
	}
	for (int a=0; !error && a<newTref.length; a++)
		if (newTref[a]==-1)
			error=true;


	//DM26022004 081.6 int18 new
	if (RButton[12].isSelected())
	{
		MPVDecoder.picture.decodeArray(gop, false, RButton[6].isSelected(), RButton[10].isSelected());
		MPVDecoder.picture.saveBMP(true, running);
	}


	//DM13112003 081.5++
	if (startpts<options[54]-options[14]/2){
		Msg("!> startPTS of GOP#"+(clv[6]-1)+" is earlier than the end of last GOP.. (exp. "+options[54]+")");
		error=true;
	}
	if (maxtref!=frame || Math.abs(lastpts-startpts)>2000)
		error=true;

	/** ignore v-errors **/
	if (origframes>0 && cBox[39].isSelected()) {
		lastpts=startpts;
		maxtref=frame;
		error=false;
	}

	/** return last orig pts for plain mpv **/
	options[40] = startpts+(long)(trefcheck*options[14])+(long)((maxtref-trefcheck+1)*options[14]);

	if (error) {
		options[7] = lastframes; 
		clv[4]++;
		Msg("!> dropping GOP#"+(clv[6]-1)+" @ orig.PTS "+sms.format(new java.util.Date(startpts/90L))+" ("+startpts+")");
		Msg(" Pics exp/cnt "+(maxtref+1)+"/"+(frame+1)+", inGOP PTS diff. "+((lastpts-startpts)/90)+"ms, new Timecode "+sms.format(new java.util.Date((long)((options[7])*(double)(options[14]/90.0f)) )));

		//DM18022004 081.6 int17 changed
		if (cBox[43].isSelected()) {
			String gopdumpname = dumpname + "-GOP#"+(clv[6]-1)+".bin";
			DataOutputStream dump = new DataOutputStream(new FileOutputStream(gopdumpname));
			dump.writeInt((clv[6]-1)); 
			dump.writeInt(maxtref); 
			dump.writeInt(frame);
			dump.write(dumpfill,0,4); 
			dump.write(pts); 
			dump.write(dumpfill); 
			dump.write(gop); 
			dump.write(SEndCode);
			dump.flush(); 
			dump.close();
			Msg("-> dump GOP to file: "+dumpname);
		}

	} else {

		newcut.clear(); // temp delete, disable for P-frame cutout

		/*** gop TC 081.5++ **/
		options[53] += (options[14]*(maxtref+1));
		options[54] = startpts+(options[14]*(maxtref+1));

		long cutposition=0;
		switch (comBox[17].getSelectedIndex()){
			case 0:
				cutposition = CUT_BYTEPOSITION;
				break;
			case 1:
				cutposition = clv[6];
				break;
			case 2:
				cutposition = origframes;
				break;
			case 3:  //DM18022004 081.6 int17 add
				cutposition = startpts+1000; //exclude jitter
				break;
			case 4:  //DM17012004 081.6 int11 add
				cutposition = startpts-firstVideoPTS; 
		}

		/*** cut using bytepos,frame#,gop#,timecode,pts  ***/
		//DM18022004 081.6 int17 changed
		if (!makecut(dumpname,startpts,cutposition,newcut,lastframes))
			options[7] = lastframes;

		/*** DAR 16:9<->4:3 request for auto cut  ***/
		else if (cBox[47].isSelected() && comBox[24].getSelectedIndex()!=clv[7]) 
			options[7] = lastframes;

		/*** H Resolution request for auto cut  ***/
		else if (cBox[52].isSelected() && !comBox[34].getSelectedItem().toString().equals(""+VBASIC[0]))
			options[7] = lastframes;

		else{

			//MPVDecoder.picture.decodeArray(gop,false,RButton[6].isSelected(),true);

			//  Msg(""+origframes+"\t"+CP[0]+"\t"+CP[1]+"/"+trefcheck+"/"+maxtref+"/"+newframes+"/"+options[7]);
			/*** check for cutout on p-frames **/
			/**
			if (CP[0]!=0) {
				int diff = origframes-(int)CP[0]-(trefcheck+1);
				maxtref-=diff;
				newframes-=diff;
				options[7]-=diff;
				gopbuffer.reset();
				gopbuffer.write(gop,0,(int)CP[1]);
				gop = gopbuffer.toByteArray();
				bool=false;
			}
			**/
			//Msg(""+origframes+"\t"+CP[0]+"\t"+CP[1]+"/"+trefcheck+"/"+maxtref+"/"+newframes+"/"+options[7]);

			startpts += (long)(trefcheck*options[14]);

			log.writeLong(startpts);
			options[8] = startpts + (long)((maxtref-trefcheck+1)*options[14]);
			log.writeLong(options[8]);

			/** videoframetimecounter ***/
			options[42]+=(long)((maxtref-trefcheck+1)*options[14]);
			log.writeLong(videotimes);
			log.writeLong(options[42]);

			/*** value for gop bitrate per second  ***/
			long svbr = options[14]*(maxtref-trefcheck+1);  // fix 4 crash, if <=0
			if (svbr <= 0) 
				svbr = options[14]*10;
			int vbr =  (int)( ( (90000L*(gop.length*8)) / svbr ) / 400);

			/*** set value for gop bitrate per second ***/
			if (options[13]==1) {

				/*** value for bitrate per vbv ***/
				if (options[0]==1 && vbvdelay<65535 ) 
					vbr =  (int)( (90000L * options[15]) / vbvdelay / 400);

				/*** set new aspectratio ***/
				if (options[24]>0) 
					gop[7] = (byte)((15&gop[7]) | options[24]<<4);

				/*** set new vbvsize ***/
				if (options[22]>0) {
					gop[10] = (byte)((0xE0&gop[10]) | 112>>>5);
					gop[11] = (byte)((7&gop[11]) | (31&112)<<3);
				}
			}

			/*** determ. avg bitrates ***/
			if (vbr < (int)options[5]) 
				options[5]=vbr;
			if (vbr > (int)options[6]) 
				options[6]=vbr;


			if (options[13]==1) {
				/*** set new bitrate ***/
				if (options[3]==0) {
					if (options[4]>0) 
						vbr = (int)options[4];
					int vbr1 = vbr;
					if (comBox[3].getSelectedIndex()==3) 
						vbr1 = 262143;

					/*** set sequence bitrate ***/
					gop[8] = (byte)(vbr1>>>10);
					gop[9] = (byte)(255&(vbr1>>2));
					gop[10]= (byte)( (63 & gop[10]) | (3 & vbr1)<<6 );
					clv[2]++;
				} else
					clv[3]++;
			}
			frametypebuffer.flush();

			//DM12042004 081.7 int01 add+
			byte ftb[] = frametypebuffer.toByteArray();
			int fields_in_frame = 0, min = 0x7FFF & clv[9]>>>15, max = 0x7FFF & clv[9], prog_flag = clv[9]>>>30;
			for (int f=1; f < ftb.length; f++)
			{
				fields_in_frame += 2;
				prog_flag |= (0x80 & ftb[f]) != 0 ? 2 : 1;
			}
			if (fields_in_frame < min || min == 0) 
				min = fields_in_frame;
			if (fields_in_frame > max || max == 0) 
				max = fields_in_frame;

			clv[9] = prog_flag<<30 | (0x7FFF & min)<<15 | (0x7FFF & max);
			//DM12042004 081.7 int01 add-
			
			//DM12042004 081.7 int01 changed
			brm.surf.update(vbr, ftb, sms.format(new java.util.Date((options[48]+options[42])/90)).substring(0,8));

			for (int a=0;a<infos.size();a++) {
				Msg(infos.get(a).toString());
				newvideo=false;
			}

			if (d2vinsert && (cBox[29].isSelected() || cBox[30].isSelected())) { 
				d2v.FrameRate(d2vframerate);
				d2vinsert=false;
			}

			if ((1L&options[26])!=0) { 
				vseq.write(gop);
				options[39] += gop.length;
				showOutSize();
			}

			/*** d2v project ***/
			d2v.addGOP(options[50],newframes);

			options[50] += gop.length;
			options[41] += gop.length;

		} // end if makecut
	}

	infos.clear();
	options[13]=0;
	gopbuffer.close();
	frametypebuffer.close();
	gop=null;

	}
	catch (IOException e) { Msg("goptest1_error "+e); }

}  /* end methode gop test */



/**********************
 * set 1. videoheader *
 **********************/
//DM04032004 081.6 int18 changed
public static void setvideoheader(String videofile, String logfile) {

	java.text.DateFormat sms = new java.text.SimpleDateFormat("HH:mm:ss.SSS");
	sms.setTimeZone(java.util.TimeZone.getTimeZone("GMT+0:00"));
	long time = 0;
	String videotype[] = { "(m1v)", "(m2v)" }; //DM12042004 081.7 int01 add

	//DM12042004 081.7 int01 add
	String frames_used[] = { "no", "interlaced", "progressive", "interlaced & progressive" };

	if (new File(logfile).exists()) {
		time = (calcvideotime(logfile)/90);
		String vt = sms.format( new java.util.Date(( time/10*10 )) );

		//DM12042004 081.7 int01 changed
		Msg("-> video length: " + options[7] + " frames @ " + vt);
		Msg("-> GOP summary: min. " + (0x7FFF & clv[9]>>>15) + ", max. " + (0x7FFF & clv[9]) + " fields; contains " + frames_used[clv[9]>>>30] + " frames");

		//DM12042004 081.7 int01 add
		if (clv[8] > 0)
			Msg("-> found " + clv[8] + " GOPs with the same PTS value for different pictures!");

		//DM12042004 081.7 int01 changed
		InfoAtEnd.add(".Video " + videotype[MPGVideotype] + ":\t" + options[7] + " Frames\t" + vt + "\t\t " + videofile);
	}

	try
	{
	RandomAccessFile pv2 = new RandomAccessFile(videofile,"rw");

	if (options[3]==0) {
		if (time==0) 
			Msg("-> avg. bitrate "+((options[5]+options[6])/2*400)+"bps (min/max: "+(options[5]*400)+"/"+(options[6]*400)+")");
		else 
			Msg("-> avg. nom. bitrate "+((pv2.length()*8000L)/time)+"bps (min/max: "+(options[5]*400)+"/"+(options[6]*400)+")");

	} else if (options[30]==1) 
		System.out.println();

	if ( (1L&options[26])==0 ) {
		pv2.close(); 
		return; 
	}

	/*** bitraten ***/
	if (comBox[15].getSelectedIndex()>0)
	{
		int newmux = (comBox[15].getSelectedIndex()==3) ? (int)options[6] : 24500;   //max - 9.8
		if (options[1]==1 && options[6] < 24500) 
			newmux=(int)options[6];
		if (options[2]==1)
		{
			if (time==0) 
				newmux=(int)((options[5]+options[6]) / 2);   // old calcul. avg.
			else
			{
				newmux = (int)(((pv2.length()*8000L)/time)/400);
				if (newmux < 0 || newmux > 24500) 
					newmux=(int)((options[5]+options[6]) / 2);   // old calcul. avg.
			}
		}

		if (comBox[15].getSelectedIndex()==4)
		{ 
			newmux=262143;
			Msg("-> set first sequence_header bitrate to VBR");
		} else 
			Msg("-> set first sequence_header bitrate to "+(newmux*400)+"bps");

		pv2.seek(8);
		newmux = (newmux<<14) | ((pv2.readInt()<<18)>>>18);
		pv2.seek(8);
		pv2.writeInt(newmux);
	}

	/*** patch resolution DVD-conform ***/
	//JLA14082003+
	//0: no patch, 1:patch unconditionally, 2:patch if <>720|352, 3:pach if <>720|704|352
	if (options[34]!=0) {
		pv2.seek(4);
		int resolutionOrig = pv2.readInt();
		int hresOrg = (resolutionOrig>>>20);
		int resolution = (0xFFFFF&resolutionOrig) | (Integer.parseInt(comBox[22].getSelectedItem().toString())<<20);
		boolean doPatch;
		switch ((int)options[34]) {
			case 2: 
				doPatch = hresOrg!=720 && hresOrg!=352; 
				break;
			case 3: 
				doPatch = hresOrg!=720 && hresOrg!=704 && hresOrg!=352; 
				break;
			default: 
				doPatch=true; 
		}
		if(doPatch) {
			pv2.seek(4);
			pv2.writeInt(resolution);
			Msg("-> set first sequ_header resolution from "+(resolutionOrig>>>20)+"*"+((0xFFF00&resolutionOrig)>>>8)+" to "+(resolution>>>20)+"*"+((0xFFF00&resolution)>>>8));
		}
	}
	//JLA14082003-

	pv2.close();
	Msg("===> new File: "+videofile);

	}
	catch (IOException e) { Msg("..setvideoheader Error\n"+e); }

} // end methode setvideoheader


/*************** create PTS alias **************/
public void logAlias(String vptslog, String datalog)
{
	try
	{

		RandomAccessFile log = new RandomAccessFile(datalog, "rw");
		log.seek(0);

		File vpts = new File(vptslog); 

		if (vpts.exists() && vpts.length() > 0)
		{
			RandomAccessFile vlog = new RandomAccessFile(vptslog, "r");
			long p = vlog.readLong();

			if (!PureVideo && options[19] == 0)
				options[25] = p;

			log.writeLong(options[25] + options[28]);

			vlog.close();
		}
		else 
			log.writeLong((0L + options[28]));

		log.writeLong(0L); 
		log.close();

		Msg(""); //DM20072004 081.7 int07 add
		Msg("--> using faked PTS for following data:"); // *** add (fix5)

	}
	catch (IOException e)
	{
		//DM25072004 081.7 int07 add
		Msg("setaliaslog error: " + e);
	}
}

//DM08032004 081.6 int18 new
public static int[] getVideoBasics()
{
	int[] video_basics = new int[VBASIC.length];

	for (int a=0; a<2; a++) // only h+v
		video_basics[a] = VBASIC[a]==null ? 0 : Integer.parseInt(VBASIC[a]);

	return video_basics;
}



/**********************
 * raw file from pva *

 **********************/
class RAWFILE
{
	IDDBufferedOutputStream out;
	String name="";

	public void init(String name, int buffersize) throws IOException
	{
		this.name = name;
		out = new IDDBufferedOutputStream( new FileOutputStream(name), buffersize);

	}

	public void write(byte[] data) throws IOException
	{
		out.write(data);
	}

	public void close() throws IOException
	{ 
		out.flush();
		out.close();

		if (new File(name).length() < 10) 
			new File(name).delete();

		else 
			Msg("===> new File " + name);
	} 
}


/**************************
 * remember found TS PIDs *
 **************************/
class TSPID {
	boolean pidstart=false, pidscram=false, need=true;
	int ID=-1, PID=-1, demux=-1, counter=-1;
	ByteArrayOutputStream buffer = new ByteArrayOutputStream();

	public void setStarted(boolean pidstart1) {
		pidstart=pidstart1;
	}
	public boolean isStarted() {
		return pidstart;
	}
	public void setScram(boolean pidscram1) { 
		pidscram=pidscram1;
	}
	public boolean getScram() { 
		return pidscram; 
	}
	public void setID(int ID1) { 


		ID=ID1;
	}
	public int getID() { 
		return ID;
	}
	public void setCounter(int count1) {
		counter=count1;
	}
	public int getCounter() {
		return counter;
	}
	public void count() {
		counter += (counter<15) ? 1 : -15 ;
	}
	public void countPVA() {
		counter += (counter<255) ? 1 : -255 ;
	}
	public void setPID(int PID1) {
		PID=PID1;
	}
	public int getPID() {
		return PID;
	}
	public void setDemux(int demux1) {
		demux=demux1;
	}
	public int getDemux() {
		return demux;
	}
	public void setneeded(boolean need1) {
		need=need1;
	}
	public boolean isneeded() {
		return need;
	}
	public void writeData(byte[] data, int off, int len) {
		buffer.write(data,off,len);
	}
	public ByteArrayOutputStream getData() {
		return buffer;
	}
	public int getDataSize() {
		return buffer.size();
	}
	public void reset() {
		buffer.reset();
	}
}


/**********************
 * demux Video/Audio/TTX PIDs *
 **********************/
class PIDdemux {

	long addoffset=0, target=0, ptsoffset=0; 
	boolean ptsover=false, writedata=true, isttx=false, misshead=false;
	int pack=-1, ID=0, newID=0, PID=0, format=0, subid=0x1FF, ismpg=0,
		lfn=-1, buffersize=1024, sourcetype=0;

	IDDBufferedOutputStream out;
	DataOutputStream log;
	String name="", parentname="";
	String[] type = { "ac","tt","mp","mv","pc","sp" }; //DM22022004 081.6 int18 changed
	String[] source = { ".e",".v",".r",".p" };
	String[] videoext = { ".mpv",".mpv",".m1v",".m2v" };

	boolean first=true, overlap=false, seqhead=false;
	ByteArrayOutputStream vidbuf, vptsbytes, packet;
	DataOutputStream vpts;
	long pts=-1, lastPTS=-1; //DM18022004 081.6 int17 changed
	byte[] endcode= { 0,0,1,(byte)0xb7 };
	boolean isPTSwritten=false; //DM09112003 081.5++
	byte[] subpic_header = { 0x53, 0x50, 0, 0, 0, 0, 0, 0, 0, 0 }; //DM24022004 081.6 int18 new
	byte[] pcm_header = { 0x50, 0x43, 0x4D, 0, 0, 0, 0, 0, 0, 0 }; //'PCM'+5b(pts)+2b(size) //DM24022004 081.6 int18 new

	//DM14072004 081.7 int06 add, stream type preselector
	public boolean StreamEnabled()
	{
		switch(newID>>>4)
		{
		case 0xE:
			if (cBox[55].isSelected())
				return true;
			break;
		case 0xC:
		case 0xD:
			if (cBox[56].isSelected())
				return true;
			break;
		case 0x8:
			if (cBox[57].isSelected())
				return true;
			break;
		case 0xA:
			if (cBox[58].isSelected())
				return true;
			break;
		case 0x9:
			if (cBox[59].isSelected())
				return true;
			break;
		case 0x2:
		case 0x3: //DM23072004 081.7 int07 add
			if (cBox[60].isSelected())
				return true;
			break;
		default:
			return true;
		}

		return false;
	}


	public void init(String name1, long[] options, int buffersize1, int lfn1, int sourcetype1) {
		parentname=name1;
		lfn=lfn1;
		buffersize=buffersize1;

		sourcetype=sourcetype1;
		name=name1+source[sourcetype]+lfn;
		addoffset = options[28];                           // time offset for data
		writedata = ((2L&options[26])!=0) ? true : false;  // do write?
		try 
		{
		out = new IDDBufferedOutputStream(new FileOutputStream(name),buffersize);
		log = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(name+".pts"),65535));
		} 
		catch (IOException e) { 
			Msg("PESAudio error1"+e); 
		}
	}

	public void init2(String name1, long[] options) {
		parentname=name1;
		name=name1+source[sourcetype]+lfn;
		addoffset = options[28];                           // time offset for data
		target=0;
		writedata = ((2L&options[26])!=0) ? true : false;  // do write?
		try 
		{
		out = new IDDBufferedOutputStream(new FileOutputStream(name),buffersize);
		log = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(name+".pts"),65535));
		} 
		catch (IOException e) { 
			Msg("PESAudio error1a"+e); 
		}
	}

	public int getNum() { 
		return lfn; 
	}            // Object intialized yet?

	public int getPID() { 
		return PID; 
	}            // get PID for later selection

	public int getID() { 
		return ID; 
	}              // get PES ID for later selection

	public void setPID(int PID1) { 
		PID=PID1; 
	}     // set PID for later selection

	public void setID(int ID1) { 
		ID=ID1; 
	}         // set ID for later selection

	public void setnewID(int ID1) { 
		newID=ID1; 
	}   // set newID for later selection /or subid

	public int getnewID() { 
		return newID; 
	}        // get newID for later selection /or subid

	public int getPackCount() { 
		return pack; 
	}     // get pack count

	public int getType() { 
		return format; 
	}        // what is it

	public void setStreamType(int ismpg1) { 
		ismpg=ismpg1; 
	}        // vdr/es/mpeg1/2...

	public int getStreamType() { 
		return ismpg; 
	}        // vdr/es/mpeg1/2...

	public void setType(int type1) { 
		format=type1; 
	}        // video

	public void setsubID(int subid1) { 
		subid=subid1; 
	}      // subid

	public int subID() { 
		return subid; 
	}           // AC-3 subid needed

	public boolean isTTX() { 
		return isttx; 
	}       // is it ttx

	public void setTTX(boolean ttx1) { 
		isttx=ttx1; 
		if (isttx) 
			format=1; 
	}      // set ttx

	public long getPTS() { 
		return pts; 
	}                // last PTS

	public void PTSOffset(long ptsoffset1) { 
		ptsoffset=ptsoffset1; 
	}   // PTS offset if needed

	public void resetVideo() {     // clean Up for next foreign inputfile in case of multiple
		vidbuf.reset(); 
		packet.reset(); 
		vptsbytes.reset(); 
		first=true;
	}

	//DM30122003 081.6 int10 changed
	public void write(byte[] data, boolean header) {    // data = 1 pespacket from demux
		pack++;
		int datalength=0, ptslength=0, ttx=0, shift=0;

		if (!header)
			datalength=data.length;
		else
		{
			//DM12072004 081.7 int06 add
			if (data[0] != 0 || data[1] != 0 || data[2] != 1)
			{
				Msg("!> invalid startcode, refuse PES packet " + pack + " (" + Integer.toHexString(PID) + "/" + Integer.toHexString(ID) + "/" + Integer.toHexString(newID) + "/" + format + ")");
				return;
			}

			ID = 0xFF&data[3];
			datalength = ((0xFF&data[4])<<8 | (0xFF&data[5]));    // av pack length bytes to follow

			if (ismpg==1)
			{
				skiploop:
				while(true)
				{
					switch (0xC0&data[shift+6])	
					{
					case 0x40:
						shift+=2; 
						continue skiploop;
					case 0x80:
						shift+=3; 
						continue skiploop; 
					case 0xC0:
						shift++;  
						continue skiploop; 
					case 0:
						break;
					}

					switch (0x30&data[shift+6])
					{
					case 0x20:
						ptslength=5;  
						break skiploop; 
					case 0x30:
						ptslength=10; 
						break skiploop; 
					case 0x10:
						shift+=5; 
						break skiploop; 
					case 0:
						shift++; 
						break skiploop; 
					}
				}
			}
			else
			{
				ptslength = (0xFF&data[8]);           // read byte pts-length
				shift=3;
				ttx = (ID==0xBD && ptslength==0x24 && (0xFF&data[9+(0xFF&data[8])])>>>4==1) ? 1: 0;
				if ((0x80&data[7])==0)
				{ //no PTS in PES_header_ext
					shift += ptslength;
					ptslength=0;
				}
			}

			format = (ID==0xBD) ? ttx : 2;
			isttx = (format==1) ? true : false;
			subid = (format<2 && (ismpg!=0 || isttx)) ? (0xFF&data[9+(0xFF&data[8])]) :0 ;

			switch (subid>>>4)
			{
			//DM22022004 081.6 int18 add
			case 0xA: //LPCM from MPG-PS
				format=4;
				break;
			case 2:   //SubPic 0-31 from MPG-PS
			case 3:   //SubPic 32-63 from MPG-PS
				format=5;
				break;
			case 8:   //AC3-DTS from MPG-PS
			case 1:   //TTX
			case 0:   //AC3-DTS from PES/VDR
				break;
			default:
				return;
			}

			datalength -= (shift+ptslength);
			shift+=6;
		}

		if (!writedata) 
			return;


		try 
		{

		//DM20072004 081.7 int07 add
		//if (isttx && ptslength == 0 && pts == -1 && fakedPTS != lastPTS)
		if (isttx && cBox[62].isSelected())
		{
			//do nothing even if PTS is available for TTX, because user wont it

			//lastPTS nutzt die geborgte AudioPTS, pts schaltet die funktion aus, falls ttx doch eigene PTS hat und nur zufällig mal keine zum anfang
			if (fakedPTS != lastPTS)
			{
				lastPTS = fakedPTS;

				if (options[30]==1) //DM131003 081.5++
					System.out.print(" stolen ttx PTS: "+ lastPTS + "/ " + addoffset + "/ " + target);

				log.writeLong(lastPTS);
				log.writeLong(target);
			}
		}

		//DM20072004 081.7 int07 changed
		//if ( ptslength>0 && data.length>=ptslength)
		else if ( ptslength > 0 && data.length >= ptslength)
		{    // read pts, if available
			pts = 0xFFFFFFFFL & ( (6&data[shift])<<29 | (255&data[shift+1])<<22 | (254&data[shift+2])<<14 |
				(255&data[shift+3])<<7 | (254&data[shift+4])>>>1 );

			pts -= options[27];
			pts &= 0xFFFFFFFFL;

			if ( (pts & 0xFF000000L)==0xFF000000L ) 
				ptsover=true;      // bit 33 was set
			if (ptsover && pts<0xF0000000L) 
				pts |= 0x100000000L;
			pts += ptsoffset;

			//DM15072004 081.7 int06 add
			pts += addoffset;

			//DM22122003 081.6 int09 change
			//DM15072004 081.7 int06 changed
			if (lastPTS != pts)
			{
				//log.writeLong(pts+addoffset);
				log.writeLong(pts);
				log.writeLong(target);
			}

			if (options[30]==1) //DM131003 081.5++
				System.out.print(" pda PTS: "+pts+"/ "+addoffset+"/ "+target);

			lastPTS=pts; //DM22122003 081.6 int09 new
		} 

		//DM20072004 081.7 int07 add
		//test, speichert die pts des erstgefundenen mpg-audio stroms, also multiaudio bringt nix durcheinander
		if (newID == 0xC0 && fakedPTS != lastPTS)
			fakedPTS = lastPTS;


		switch(subid>>>4){
		//DM22022004 081.6 int18 add
		case 0xA: //LPCM
			shift+=1; //7
			datalength-=1; //7
			break;
		case 8: //AC3-DTS
			shift+=4; 
			datalength-=4; 
			break;
		//DM22022004 081.6 int18 add
		case 2: //subpic  0.31
		case 3: //subpic 32.63
		case 1: //TTX
			shift+=1; 
			datalength-=1; 
		}

		if (datalength>0) {
			if (ptslength>0) //DM24022004 081.6 int18 add
			{
				switch(format)
				{
				case 5:
					LSB(subpic_header,pts,2,8);
					out.write(subpic_header);
					target += subpic_header.length;

					//DM20022004 081.7 int02 add, DVB subs adaption
					if (Common.nextBits(data, (shift+ptslength)*8, 16) == 0xF)
					{
						out.write(((datalength + 3) >>> 8) & 0xFF);
						out.write((datalength + 3) & 0xFF);
						out.write(0); //padding
						target += 3;
					}

					break;

				case 4:
					LSB(pcm_header,pts,3,5);
					pcm_header[8] = (byte)(0xFF & datalength>>>8);
					pcm_header[9] = (byte)(0xFF & datalength);
					out.write(pcm_header);
					target += pcm_header.length;
					break;
				}
			}

			//DM28042004 081.7 int02 add, prevent lost packets
			else if (format == 5 && Common.nextBits(data, (shift+ptslength)*8, 16) == 0xF)
			{
				LSB(subpic_header, 0, 2, 8);
				out.write(subpic_header);
				target += subpic_header.length;

				out.write(((datalength + 3) >>> 8) & 0xFF);
				out.write((datalength + 3) & 0xFF);
				out.write(0); //padding
				target += 3;
			}

			target += datalength;
			out.write(data, shift+ptslength, datalength);
		}
		} 
		catch (IOException e) { 
			Msg("PESAudio error2"+e); 
		}
	}

	//DM24022004 081.6 int18 add
	private void LSB(byte data[], long value, int off, int len)
	{
		for (int a=0; a<len; a++)
			data[off+a] = (byte)(0xFFL & value>>>(a*8));
	}

	//DM10052004 081.7 int02 changed, fix
	public String[] close(String vptslog)
	{ 
		String pts_name = name + ".pts";
		String parameters[] = { name, pts_name, type[format], parentname  };

		try 
		{

		if (out == null)
		{
			parameters[0] = "";
			return parameters;
		}

		out.flush(); 
		out.close();

		log.flush(); 
		log.close(); 

		if (new File(pts_name).length() < 10) 
			logAlias(vptslog, pts_name); // logAlias

		if (new File(name).length() < 10)
		{
			new File(name).delete();
			new File(pts_name).delete();
			parameters[0] = "";
		}

		} 
		catch (IOException e)
		{ 
			Msg("PESAudio error3"+e); 
		}

		return parameters;
	} 

	public void initVideo(String name1, long[] options, int buffersize1, int lfn1, int sourcetype1) {
		parentname=name1;
		lfn=lfn1;
		buffersize=buffersize1;
		sourcetype=sourcetype1;
		name=name1+source[sourcetype]+lfn;
		format=3;
		MPGVideotype=0;
		writedata = ((1L&options[26])!=0) ? true : false;  // do write?
		try 
		{
		out = new IDDBufferedOutputStream(new FileOutputStream(name),buffersize);
		if (cBox[34].isSelected()) //DM24112003 081.5++ mpeg2schnitt
			out.InitIdd(name,1);
		
		log = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(name+".pts"),65535));
		packet = new ByteArrayOutputStream();
		vidbuf = new ByteArrayOutputStream();
		vptsbytes = new ByteArrayOutputStream();
		vpts = new DataOutputStream(vptsbytes);
		} 
		catch (IOException e) { 
			Msg("writeVideo error1"+e); 
		}
	}

	public void initVideo2(String name1, long[] options) {
		parentname=name1;
		name=name1+source[sourcetype]+lfn;
		writedata = ((1L&options[26])!=0) ? true : false;  // do write?
		first=true;
		MPGVideotype=0;
		try 
		{
		out = new IDDBufferedOutputStream(new FileOutputStream(name),buffersize);
		if (cBox[34].isSelected()) //DM24112003 081.5++ mpeg2schnitt
			out.InitIdd(name,1);
		
		log = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(name+".pts"),65535));
		packet.reset();
	 	vidbuf.reset();
		vptsbytes.reset();
		} 
		catch (IOException e) { 
			Msg("writeVideo error1a"+e); 
		}
	}

	public String closeVideo() { 
		String logfile = "-1";
		try 
		{
		if (options[32]==1 && options[7]>0) {
			out.write(endcode);
			options[39]+=4;
			options[41]+=4;
		}
		packet.close();
		vidbuf.flush(); 
		vidbuf.close();
		out.flush();
		out.close();
		log.flush(); 
		log.close();
		vpts.flush(); 
		vpts.close();
		vptsbytes.flush(); 
		vptsbytes.close();

		String videofile="";
		if (new File(name).length()<10) {
			new File(name).delete();
			if (!writedata && new File(name+".pts").length()>16) 
				logfile = name+".pts";
			else 
				new File(name+".pts").delete();
		} else { 
			int ot = (cBox[32].isSelected() || cBox[29].isSelected() || cBox[30].isSelected()) ? 0 : 2;
			videofile=parentname+videoext[MPGVideotype+ot];
			File newfile = new File(videofile);

			if (newfile.exists()) 
				newfile.delete();

			Common.renameTo(new File(name), newfile); //DM13042004 081.7 int01 changed
			//new File(name).renameTo(newfile);

			logfile = name + ".pts";
			setvideoheader(videofile,logfile);
        
			/** celltimes.txt 062b***/
			if (cBox[26].isSelected()) {
				BufferedWriter cellout = new BufferedWriter(new FileWriter(workouts+"CellTimes.txt"));
				for (int a=0;a<cell.size();a++) {
					cellout.write(cell.get(a).toString());
					cellout.newLine();
				}
				cellout.close();
				Msg("--> Celltimes in: "+workouts+"CellTimes.txt");
				long fl = new File(workouts+"CellTimes.txt").length();
				options[39]+=fl;
				options[41]+=fl;
			}
			cell.clear();
		}

		if (cBox[34].isSelected()) { //DM24112003 081.5++
			if (new File(videofile).exists())
				out.renameVideoIddTo(parentname);
			else
				out.deleteIdd();
		}

		}
		catch (IOException e) { 
			Msg("writeVideo error3"+e); 
		}
		return logfile;
	} 

	/****** write video ******/
	public long[] writeVideo(byte[] origdata, long[] options, boolean isPVA) {    // data = 1 pespacket from demux
		int datalength=0, ptslength=0, ttx=0, shift=0;
		pack++;

		if (isPVA)
		{
			datalength=origdata.length;

			if (options[55]!=-1)
			{
				shift=-4; 
				ptslength=4; 
			}
		}
		else
		{
			//DM12072004 081.7 int06 add
			if (origdata[0] != 0 || origdata[1] != 0 || origdata[2] != 1)
			{
				Msg("!> invalid startcode, refuse video PES packet " + pack + " (" + Integer.toHexString(PID) + "/" + Integer.toHexString(ID) + "/" + Integer.toHexString(newID) + "/" + format + ")");
				return options;
			}

			ID = 0xFF&origdata[3];
			datalength = ((0xFF&origdata[4])<<8 | (0xFF&origdata[5]));    // av pack length bytes to follow

			if (ismpg==1) {
				skiploop:
				while(true) {
					switch (0xC0&origdata[shift+6]) {
					case 0x40: { 
						shift+=2; 
						continue skiploop; 
					}
					case 0x80: { 
						shift+=3; 
						continue skiploop; 
					}
					case 0xC0: { 
						shift++;  
						continue skiploop; 
					}
					case 0: { break; }
					}

					switch (0x30&origdata[shift+6]) {
					case 0x20: { 
						ptslength=5;  
						break skiploop; 
					}
					case 0x30: { 
						ptslength=10; 
						break skiploop; 
					}
					case 0x10: { 
						shift+=5; 
						break skiploop; 
					}
					case 0:    { 
						shift++; 
						break skiploop; 
					}
					}
				}
			} else {
				ptslength = (0xFF&origdata[8]);           // read byte pts-length
				shift=3;
				if ((0x80&origdata[7])==0) { 
					shift += ptslength;
					ptslength=0;
				}
			}
			datalength -= (shift+ptslength);
			shift+=6;
		}

		if ( ptslength>0  && origdata.length>=datalength ) {    // read pts, if available
			pts = (isPVA) ? options[55] :
				(0xFFFFFFFFL & ( (6&origdata[shift])<<29 |
				(255&origdata[shift+1])<<22 | (254&origdata[shift+2])<<14 |
				(255&origdata[shift+3])<<7 | (254&origdata[shift+4])>>>1 )) ;

			pts -= options[27];
			pts &= 0xFFFFFFFFL;

			if ( (pts & 0xFF000000L)==0xFF000000L ) 
				ptsover=true;      // bit 33 was set
			if (ptsover && pts<0xF0000000L) 
				pts |= 0x100000000L;
			pts += ptsoffset;

			if (options[30]==1) //DM131003 081.5++
				System.out.print(" pdv PTS: "+pts);

			isPTSwritten=false; //DM09112003 081.5++
		} 

		try 
		{

		//DM04022004 081.6 int14 fix?
		if (origdata.length < shift+ptslength+datalength)
			Msg("!> incoming PES packet without payload or wrong header data ("+origdata.length+"/"+shift+"/"+ptslength+"/"+datalength+")");
		else
			packet.write(origdata,shift+ptslength,datalength);

		packet.flush();
		byte[] data = packet.toByteArray();
		packet.reset();

		int s=0, t=0;  //DM20032004 081.6 int18 changed
		boolean gop=false, packetfirstframe=true;

		packloop:
		for (s=0, t=0; s<data.length-3; s++) //DM20032004 081.6 int18 changed
		{

			if ( data[s]!=0 || data[s+1]!=0 || data[s+2]!=1 ) 
				continue packloop;

			/**** 0x 00 00 01 00  new frame at first ****/
			//if (ptslength>0 && packetfirstframe && data[s+3]==0 ) {
			if (!isPTSwritten && packetfirstframe && data[s+3]==0 ) {  //DM09112003 081.5++
				if (misshead && s<3) { 
					misshead=false; 
					continue packloop; 
				}

				if (pts!=-1) //DM18022004 081.6 int17 changed
				{
					vpts.writeLong(pts);
					vpts.writeLong((long)vidbuf.size());
					vpts.flush();
				}

				isPTSwritten=true;  //DM09112003 081.5++
				packetfirstframe=false;
				s += 8; //DM14122003 081.6 int06
			}

			/**** 0x 00 00 01 b3 / b7 sequence start/end ****/
			/**** 0x 00 00 01 b8  gop start ****/
			else if (data[s+3]==(byte)0xb3 || data[s+3]==(byte)0xb7 || data[s+3]==(byte)0xb8 ) {

				if (data[s+3]==(byte)0xb3) 
					seqhead=true;
				if (data[s+3]==(byte)0xb8 && seqhead && vidbuf.size()<400) { 
					seqhead=false; 
					continue packloop; 
				}

				vidbuf.write(data,t,s);  //DM20032004 081.6 int18 changed

			/***
				//DM26052004 081.7 int03 add++
				byte vidbuf_array[] = vidbuf.toByteArray();
				byte vptsbytes_array[] = vptsbytes.toByteArray();

				vptsbytes.reset();
				vidbuf.reset(); 

				if (!first) 
					goptest( out, vidbuf.toByteArray(), vptsbytes.toByteArray(), log, parentname);
					//goptest( out, vidbuf_array, vptsbytes_array, log, parentname);
				//DM26052004 081.7 int03 changed--
			***/

				if (!first) 
					goptest( out, vidbuf.toByteArray(), vptsbytes.toByteArray(), log, parentname);

				vptsbytes.reset();
				vidbuf.reset(); 

				/****** split size reached *****/
				if ( options[18]>0 && options[18]<options[41] ) 
					return options;

				/****** d2v split reached *****/
				if ( cBox[30].isSelected() && options[50] > options[51] ) {
					int part = d2v.getPart()+1;
					String newpart = parentname+"["+part+"].mpv";

					/*** sequence end code ***/
					if ( (1L&options[26])!=0 && options[32]==1 && options[7]>0 ) {
						out.write(endcode);
						options[39]+=4;
						options[41]+=4;
					}
					out.flush();
					out.close();
					System.gc();

					out = new IDDBufferedOutputStream( new FileOutputStream(newpart), bs-1000000);
					if (cBox[34].isSelected()) //DM24112003 081.5++ mpeg2schnitt
						out.InitIdd(newpart,1);

					d2v.setFile(newpart);
					options[50]=0;
				}

				//if (ptslength>0 && packetfirstframe) {
				if (!isPTSwritten && packetfirstframe) { //DM09112003 081.5++
					if (pts!=-1) //DM18022004 081.6 int17 changed
					{
						vpts.writeLong(pts);
						vpts.writeLong(vidbuf.size());
						vpts.flush();
					}
					isPTSwritten=true; //DM09112003 081.5++
				}


				//DM04112003 081.5++ change 
				//DM06022004 081.6 int15 add msg
				//DM13082004 081.7 int09 changed
				if (data[s+3] == (byte)0xb7)
				{
					Msg("-> skip sequence_end_code following GOP#" + clv[6]);

					first = true;
					options[13] = 0;
					//vidbuf.write(data, s + 4, data.length - s - 4);

					s += 3;
					continue packloop;
				}
				else 
					vidbuf.write(data, s, data.length - s);

				if (data[s+3]==(byte)0xb8) {
					options[13]=0;
					if (options[19]>0) 
						first=false;
				} else if (data[s+3]==(byte)0xb3){ 
					options[13]=1;
					first=false;
				}
				gop=true;
				misshead=false;
				break packloop;
			}
		} // end packloop

		if (!gop) { 
			if (data.length>2) {
				vidbuf.write(data,0,data.length-3);
				packet.write(data,data.length-3,3);
				misshead=true;
			} else 
				vidbuf.write(data);
		}

		}
		catch (IOException e) { 
			Msg("writeVideo error2"+e); 
		}

		if (vidbuf.size()>6144000){ //DM111003 081.5++
			vptsbytes.reset();
			vidbuf.reset(); 
			packet.reset();
			Msg("-> dropping video data, GOP larger than 6MB");
			misshead=false;
			first=true;
		}

		return options;
	}
}



/**********************
 * create VDR from TS *
 **********************/
class makeVDR {

	IDDBufferedOutputStream out; //DM18022004 081.6 int17 changed
	String name="";
	boolean first=true, makempg=false, toTS=false, ptsover=false;
	boolean[] writedata = { false,false };
	byte[] packBA = { 0,0,1,(byte)0xba,0x44,0,4,0,4,1,0,(byte)0xEA,0x63,(byte)0xF8 };  // 8000kbps
	byte[] packBA1 = { 0,0,1,(byte)0xba,0x44,0,4,0,4,1,0,(byte)0xEA,0x63,(byte)0xF8 };  // 8000kbps
	byte[] packB9 = { 0,0,1,(byte)0xb9 };
	byte[] TSHead = { 0x47,0x40,0,0 };   // 1x = no adap, 3x adap follw
	byte[] TSHead2 = { 0x47,0,0,0 };   // 1x = no adap, 3x adap follw
	byte[] pvaHead1 = { 0x41,0x56,0,0,0x55,0,0,0 };
	byte[] pvaHead2 = { 0x41,0x56,0,0,0x55,0x10,0,0,0,0,0,0 };
	byte[] sysstd = { 0,0,1,(byte)0xBB,0,0xC,(byte)0x80,(byte)0x9C,0x41,4,0x21,0x7f };  // 12+ byte std 1A, 1V  8Mbps (20000 * 400)
	byte[][] sys = { 
		{ (byte)0xE0,(byte)0xE0,(byte)0xE0 },     // 224kb   0 MPV
		{ (byte)0xBD,(byte)0xC0,0x20 },           // 4kb     1 AC3
		{ (byte)0xC0,(byte)0xC0,0x20 }          // 4kb     2 MPA
	};
	byte[] subID = { (byte)0x80,1,0,1 };       // std 0x80,1,0,1
	byte[] sysID = { (byte)0xE0,(byte)0xC0 };  // ID adder mpv+mpa
	byte[] adapt = { 0,0 };

	long[] time = new long[2];
	int[] counter = new int[70];       // pva counter , 1=stream1,2=stream2  4... rest
	int[] streams = new int[2];        // number of audio[0], video[1] streams
	int insert=26;
	byte[] fills = new byte[2324];
	ArrayList stuff = new ArrayList(), IDs = new ArrayList();
	boolean video=false;

	//DM09072004 081.7 int06 disabled
	//int ac3ID = (comBox[20].getSelectedIndex()>0) ? Integer.parseInt(comBox[20].getSelectedItem().toString().substring(2,4),16) : 0x80;

	ByteArrayOutputStream buf = new ByteArrayOutputStream();
	long scr=0, pmtcount=0;
	int toVdr=0; //DM18022004 081.6 int17 new

	public void init(String name, long[] options, int buffersize, int toVdr, int filenumber) {
		this.name = name;
		first=true; 
		video=false; 
		ptsover=false; 
		pmtcount=0; 
		insert=26;
		packBA1[2]=0; 
		time[0]=-1; 
		scr=0;
		writedata[0] = ((1L&options[26])!=0) ? true : false;  // do write video?
		writedata[1] = ((2L&options[26])!=0) ? true : false;  // do write audio?
		java.util.Arrays.fill(fills,(byte)0xff);

		buf.reset();
		if (options[19]==0) 
			IDs.clear();

		try 
		{ 
			//DM18022004 081.6 int17 changed
			out = new IDDBufferedOutputStream(new FileOutputStream(name),buffersize); 
			//DM18022004 081.6 int17 new
			if (toVdr==1 && cBox[54].isSelected())
			{
				this.toVdr = toVdr;
				out.InitVdr(new File(name).getParent()+filesep+"index.vdr",filenumber);
			}
		}
		catch (IOException e) { 
			Msg("writeVDR error1"+e); 
		}
	}

	/******* write MPG ******/
	public long[] writeMPG(byte[] data, long[] options, PIDdemux demux, int overhead) {    // data = 1 pespacket from demux
		boolean PTS=false;

		try 
		{
		if (!makempg) 
			makempg=true;

		if (data.length>overhead) { 
			int type = demux.getType();         // read Type, 0=ac3,1=ttx,2=mpa,3=mpv
			if (!writedata[0] && type==3 ) 
				return options;       // video disabled
			else if (!writedata[1] && type<3 ) 
				return options;   // nonvideo disabled

			int newID = demux.getnewID();       // read new mapped ID
			if (type==1 || type>3)  //DM23022004 081.6 int18 changed
				return options;        // return, ignore TTX (newID 0x90...)
			if (newID==0) 
				return options;       // return, newID is not for mpg (curr. 0x20..,0xA0..)
			if (type==3 && newID>0xE0) 
				return options;    // not more than one video

			int i=0;
			for (;i<IDs.size();i++) {          // new ID arrived, align data
				if (newID==(0xFF&Integer.parseInt(IDs.get(i).toString()))) 





					break;
			}
			if (i==IDs.size()) {
				if (newID!=0xE0 && !video && cBox[23].isSelected()) 
					return options; // must start with video  
				data = searchHeader(data,type,overhead);



				if (data.length==0) 
					return options;
				IDs.add(""+((newID>0xDF)?newID:(0x100+newID)));
			}
			if (first) {
				out.write(packBA); 
				options[39]+= 14; 
				options[41]+= 14;         // write first pack
				out.write(new byte[130]); 
				options[39]+=130; 
				options[41]+=130;  // placeholder for systemheader
				first=false;
			}
			int len = data.length-overhead;           // len of useful data
			if ((0x80&data[7])!=0) { 
				PTS=true;
				data[9] &= ~8;     // in PTS delete bit 33 PTS
			}
			if ((0x40&data[7])!=0) 

				data[14] &= ~8;    // in DTS  -"-
			//        data[6] &= ~8;  //delete PES priority flag

			if (type==0) {
				if (demux.getStreamType()==2) {
					data[9+(0xFF&data[8])] = (byte)newID;  // set to new subID , better in MPEG
					if (!cBox[36].isSelected()) {         // write pack
						out.write(packBA); 
						options[39]+= 14; 
						options[41]+= 14; 
					} else {
						scr += ((len*8)/150);
						out.write(calcSCR(packBA,scr));
						options[39]+= 14; 
						options[41]+= 14;
					}
					out.write(data,0,len); 
					options[39]+=len; 
					options[41]+=len;    // write the rest
					return options;
				} else {
					int size = ((0xFF&data[4])<<8 | (0xFF&data[5]))+4;
					int head = 9+(0xFF&data[8]);
					data[4] = (byte)(size>>>8);    // correct length info
					data[5] = (byte)(0xFF&size);   //   --"--
					subID[0] = (byte)newID;        // set to new subID , better in MPEG
					if (!cBox[36].isSelected()) {     // write pack
						out.write(packBA); 
						options[39]+= 14; 
						options[41]+= 14; 
					} else {
						scr += ((len*8)/150);
						out.write(calcSCR(packBA,scr));
						options[39]+= 14; 
						options[41]+= 14;
					}
					out.write(data,0,head);        // write leading hader
					out.write(subID);              // write 4 byte for subid
					out.write(data,head,len-head); 
					options[39]+=len+4; 
					options[41]+=len+4;    // write the rest
					return options;
				}
			}
			if (!video) {   // remember ID pos.
				if (type==3) 
					video=true;
				else 
					stuff.add(""+(options[41]+17));
			}
			data[3] = (byte)newID;             // set to new ID , better in MPEG
			boolean newscr=false;

			if (type==3 && PTS && cBox[36].isSelected()) {
				/*** looking for I or P-Frameheader ***/
				for (int a=9;a<data.length-6;a++) {
					if (data[a]!=0 || data[a+1]!=0 || data[a+2]!=1 || data[a+3]!=0) 
						continue;
					if ((0x38&data[a+5])==0x8 || (0x38&data[a+5])==0x10) {
						scr = 0xFFFFFFFFL & ( (6&data[9])<<29 | (255&data[10])<<22 | (254&data[11])<<14 |
								(255&data[12])<<7 | (254&data[13])>>>1 );
						scr-=options[57];
						// scr-=55000L;
						packBA = calcSCR(packBA,scr);
						if (packBA1[2]==0) 
							System.arraycopy(packBA,0,packBA1,0,14);
						out.write(packBA); 
						options[39]+= 14; 
						options[41]+= 14;
						newscr=true;
					}
					break;
				}
			}
			if (!newscr) {
				if (!cBox[36].isSelected()) {
					out.write(packBA); 
					options[39]+= 14; 
					options[41]+= 14;
				} else {
					scr += ((len*8)/150);
					out.write(calcSCR(packBA,scr));
					options[39]+= 14; 
					options[41]+= 14;
				}
			}
			out.write(data,0,len); 
			options[39]+=len; 
			options[41]+=len;    // write the rest
		}
		} 
		catch (IOException e) { 
			Msg("writeVDR error2"+e); 
		}

		return options;
	}


	public byte[] calcSCR(byte[] packBA, long scr) {
		packBA[4] = (byte)(0x44 | (0x3&(scr>>>28)) | (0x38&(scr>>>27)));
		packBA[5] = (byte)(0xFF&(scr>>>20));
		packBA[6] = (byte)(0x4 | (0x3&(scr>>>13)) | (0xF8&(scr>>>12)));
		packBA[7] = (byte)(0xFF&(scr>>>5));
		packBA[8] = (byte)(0x4 | (0xF8&(scr<<3)));
		if (options[30]==1) 
			System.out.println("scr "+scr);
		return packBA;
	}


	/******* write VDR ******/
	//DM09072004 081.7 int06 changed
	public long[] writeVDR(byte[] data, long[] options, PIDdemux demux, int overhead)
	{
		// data = 1 pespacket from demux

		try 
		{
			int type = demux.getType();
			int stream = demux.getStreamType();
			int newID = demux.getnewID();       // read new mapped ID

			//DM09072004 081.7 int06 changed
			//if (type == 0 && newID != ac3ID) 
			if (type == 0 && newID != 0x80)
				return options;   // return, additional ac3 subIDs not accept

			if (type == 1 && newID > 0x9F) //DM30122003 081.6 int10 changed
				return options;   // return, nor more ttx  accept

			if (type > 3) //DM23022004 081.6 int18 changed
				return options;   // return, lpcm,subpics not accept

			if (data.length > overhead)
			{ 
				if (!writedata[0] && type == 3 ) 
					return options;   // video disabled

				else if (!writedata[1] && type < 3 ) 
					return options;   // nonvideo disabled

				int len = data.length - overhead;

				if ((0x80 & data[7]) != 0) //remove bit33 PTS
					data[9] &= ~8;

				if ((0x40 & data[7]) != 0) //remove bit33 DTS
					data[14] &= ~8;

				if (type == 1) // remap TTX SubID //DM30122003 081.6 int10 add
					data[9 + (0xFF & data[8])] = (byte)(0x1F & newID);

				if (type == 0 && stream == 2)
				{
					// skip Ac-3 sub-id and only write one stream
					len -= 4;
					int size = len - 6;
					int head = 9 + (0xFF & data[8]);

					data[4] = (byte)(size>>>8);    // correct length info
					data[5] = (byte)(0xFF & size);   //   --"--

					out.write(data, 0, head);        // write leading hader
					out.write(data, head + 4, len - head); 

					options[39] += len; 
					options[41] += len;    // write the rest

					return options;
				}

				//DM09072004 081.7 int06 fix, earlier function is now done by 'PIDfilter'
				data[3] = data[3] != (byte)0xBD ? (byte)newID : data[3];

				out.write(data, 0, len); 

				options[39] += len; 
				options[41] += len;
			}
		} 

		catch (IOException e)
		{ 
			Msg("writeVDR error3" + e); 
		}

		return options;
	}

	/**** pva ******/
	public long[] writePVA(byte[] data, long[] options, PIDdemux demux, int overhead) {    // data = 1 pespacket from demux
		int datalength=0, ptslength=0, shift=0;    // Vpakets max. 6144 , Apakets max. 2048 incl. pvaheader
		try 
		{
		int type = demux.getType();
		int newID = demux.getnewID();       // read new mapped ID
		if (type==3 && newID>0xE0) 
			return options;      // ignore more video
		if (type>3)  //DM23022004 081.6 int18 changed
			return options;   // return, lpcm not accept

		if (data.length>overhead) { 
			if (!writedata[0] && (0xF0&data[3])==0xE0 ) 
				return options;
			else if (!writedata[1] && (0xF0&data[3])!=0xE0 ) 
				return options;

			/*** looking for I or P-Frameheader ***/
			for (int a=9;!video && newID==0xE0 && a<data.length-4;a++) {
				if (data[a]!=0 || data[a+1]!=0 || data[a+2]!=1 || data[a+3]!=(byte)0xB3) 
					continue;
				video=true;
			}
			if (!video && cBox[23].isSelected()) 
				return options; // must start with video  

			switch (newID) {

			case 0xE0: {
				datalength = ((0xFF&data[4])<<8 | (0xFF&data[5]));
				ptslength = (0xFF&data[8]);
				shift=3;
				if ((0x80&data[7])==0) { 
					shift += ptslength; 
					ptslength=0; 
				}
				datalength -= (shift+ptslength);
				shift+=6;

				if ( ptslength>0  && data.length>=datalength ) {    // read pts, if available
					long pts = 0xFFFFFFFFL & ( (6&data[shift])<<29 | (255&data[shift+1])<<22 |
						(254&data[shift+2])<<14 | (255&data[shift+3])<<7 | (254&data[shift+4])>>>1 );

					pvaHead2[2]=1;
					pvaHead2[3]=(byte)(0xFF&counter[1]); 
					counter[1]++;
					for (int a=0;a<4;a++) 
						pvaHead2[11-a] = (byte)(0xFF&(pts>>>(a*8)));
					for (int a=0;a<2;a++) 
						pvaHead2[7-a] = (byte)(0xFF&((datalength+4)>>>(a*8)));
					out.write(pvaHead2); 
					options[39]+=12; 
					options[41]+=12;
				} else {
					pvaHead1[2]=1;
					pvaHead1[3]=(byte)(0xFF&counter[1]); 
					counter[1]++;
					pvaHead1[5]=0;
					for (int a=0;a<2;a++) 
						pvaHead1[7-a] = (byte)(0xFF&((datalength)>>>(a*8)));
					out.write(pvaHead1); 
					options[39]+=8; 
					options[41]+=8;
				}

				//DM04022004 081.6 int14 fix?
				if (data.length-overhead < shift+ptslength+datalength)
					Msg("!> incoming PES packet without payload or wrong header data ("+data.length+"/"+overhead+"/"+shift+"/"+ptslength+"/"+datalength+")");
				else
				{
					out.write(data,shift+ptslength,datalength); 
					options[39]+= datalength; 
					options[41]+= datalength;
				}
				break;
			}
			case 0xC0: {
				pvaHead1[2]=2;
				pvaHead1[3]=(byte)(0xFF&counter[2]); 
				counter[2]++;
				pvaHead1[5]=0x10;
				datalength = data.length-overhead;
				for (int a=0;a<2;a++) 
					pvaHead1[7-a] = (byte)(0xFF&((datalength)>>>(a*8)));
				out.write(pvaHead1); 
				options[39]+=8; 
				options[41]+=8;

				data[3]=(byte)newID;
				if ((0x80&data[7])!=0) 
					data[9] &= ~8;
				out.write(data,0,datalength); 
				options[39]+= datalength; 
				options[41]+= datalength;
				break;
			}
			default: {
				int countID=3;
				switch (0xF0&newID) {
				case 0x90: { 
					countID=newID-0x8C; 
					break; 
				}
				case 0x80: { 
					countID=newID-0x6C; 
					break; 
				}
				case 0xC0:
				case 0xD0: { 
					countID=newID-0x9C; 
					break; 
				}
				}

				pvaHead1[2]=(byte)newID;
				pvaHead1[3]=(byte)(0xFF&counter[countID]); 
				counter[countID]++;
				pvaHead1[5]=0x10;
				datalength = data.length-overhead;

				if (type==0 && demux.getStreamType()==2) {
					datalength-=4;
					int size = datalength-6;
					int head = 9+(0xFF&data[8]);
					data[4] = (byte)(size>>>8);    // correct length info

					data[5] = (byte)(0xFF&size);   //   --"--
					if ((0x80&data[7])!=0) 
						data[9] &= ~8;

					for (int a=0;a<2;a++) 
						pvaHead1[7-a] = (byte)(0xFF&((datalength)>>>(a*8)));

					out.write(pvaHead1);           // pva header
					out.write(data,0,head);        // write leading hader
					out.write(data,head+4,datalength-head); 
					options[39]+=datalength+8; 
					options[41]+=datalength+8;    // write the rest
					return options;
				}

				for (int a=0;a<2;a++) 
					pvaHead1[7-a] = (byte)(0xFF&((datalength)>>>(a*8)));

				out.write(pvaHead1); 
				options[39]+=8; 
				options[41]+=8;

				if ((0x80&data[7])!=0) 
					data[9] &= ~8;
				out.write(data,0,datalength); 
				options[39]+= datalength; 
				options[41]+= datalength;
				break;
			}
			}
		}
		}
		catch (IOException e) { 
			Msg("writeVDR error4"+e); 
		}
		return options;
	}


	/**** TFraw mpeg-ts ******/  //DM01102003 fix
	public long[] writeTS(byte[] data, long[] options, PIDdemux demux, int overhead) {    // data = 1 pespacket from demux
		toTS=true;
		int datalength=0, ptslength=0, shift=0;    // TS packet max. 188, start ID on new pespack with pts
		boolean pcr=false;
		buf.reset();

		try 
		{
		int type = demux.getType();
		int newID = demux.getnewID();       // read new mapped ID
		if (type==3 && newID>0xE0) 
			return options;      // ignore more video
		if (type>3)  //DM23022004 081.6 int18 changed
			return options;        // return, lpcm not accept
		if (type!=3 && !video && cBox[23].isSelected()) 
			return options; // must start with video  

		if (data.length>overhead) { 

			int i=0;
			for (;i<IDs.size();i++) {          // new ID arrived, save for PMT
				if (newID==(0xFF&Integer.parseInt(IDs.get(i).toString()))) 
					break;



			}

			datalength = data.length-overhead;

			if (type==0 && demux.getStreamType()==2) {  //adapt ac3 from mpg-ps
				datalength-=4;
				int size = datalength-6;
				int head = 9+(0xFF&data[8]);
				data[4] = (byte)(size>>>8);    // correct length info
				data[5] = (byte)(0xFF&size);   //   --"--

				byte[] data2 = new byte[datalength];
				System.arraycopy(data,0,data2,0,head);
				System.arraycopy(data,head+4,data2,head,datalength-head);
				data=data2;
			}

			if (i==IDs.size()) {
				int newID2 = newID;
				if (newID2<0x90) 
					newID2 |= 0x200;
				if (newID2<0xC0) 
					newID2 |= 0x300;
				if (newID2<0xE0) 
					newID2 |= 0x100;

				if (!qinfo){
					if (newID!=0xE0 && !video && cBox[23].isSelected()) 
						return options; // must start with video  
					if (type!=1){
						data = searchHeader(data,type,overhead);




						datalength=data.length-overhead;  //DM16112003 081.5++ fix
					}
					if (data.length==0) 
						return options;
				}
				IDs.add(""+newID2);
			}

			if (!writedata[0] && (0xF0&data[3])==0xE0 ) 
				return options;
			else if (!writedata[1] && (0xF0&data[3])!=0xE0 ) 
				return options;

			TSHead[2] = TSHead2[2] = (byte)(0xFF&newID);

			if ((0x80&data[7])!=0) 
				data[9] &= ~8;     // in PTS delete bit 33 PTS
			if ((0x40&data[7])!=0) 
				data[14] &= ~8;    // in DTS  -"-
			if (type==3) 
				data[4]=data[5]=0;

			//add TS header
			//DM09082004 081.7 int08 changed
			if (first)
			{
				buf.write( tf.init( name, cBox[37].isSelected(), cBox[42].isSelected(), comBox[20].getSelectedIndex()));
				first=false; 
			}

			boolean hasPTS=false;
			if ((0x80&data[7])!=0)
				hasPTS=true;

			int countID=3;
			long pcrbase=0;

			if (hasPTS && tf.getfirstID()==(0xFF&newID)) {
				long pts = 0xFFFFFFFFL & ( (6&data[9])<<29 | (255&data[10])<<22 | (254&data[11])<<14 | (255&data[12])<<7 | (254&data[13])>>>1 );
				pcrbase = (pts-options[57]);
				if ( (pts & 0xFF000000L)==0xFF000000L ) 
					ptsover=true;
				if (ptsover && pts<0xF0000000L) 
					pts |= 0x100000000L;
				time[1]=pts;
				pcr=true;
				if (time[0]==-1) 
					time[0]=time[1];
			}

			switch (0xF0&newID) {
			case 0x80: { 
				countID=newID-0x6C; 
				break; 
			}
			case 0x90: { 
				countID=newID-0x8C; 
				break; 
			}
			case 0xC0:
			case 0xD0: { 
				countID=newID-0x9C; 
				break; 
			}
			case 0xE0: { countID=1;
				if (hasPTS) {
					pcr=false;

					/*** looking for I or P-Frameheader ***/
					for (int a=9;a<datalength+overhead-6;a++) {
						if (data[a]!=0 || data[a+1]!=0 || data[a+2]!=1 || data[a+3]!=0) 
							continue;
						if ((0x38&data[a+5])==0x8){
							pcr=true; 
							video=true; 
						}
						break;
					}
				}
				break;
			}
			}

			if (type==3 && !video && cBox[23].isSelected()) 
				return options; // must start with video  

			if (30000L*pmtcount <= options[41]) {
				buf.write(tf.getPat());
				if (cBox[41].isSelected()) 
					buf.write(tf.getAutoPmt());
				else 
					buf.write(tf.getPmt());
				pmtcount++;
			}

			//DM26052004 081.7 int03 changed
			if (pcr && cBox[41].isSelected() && cBox[42].isSelected())
			{
				//buf.write(tf.getTTX(Teletext,data,base_time.format(new java.util.Date((pcrbase+options[57])/90))));
				buf.write( tf.getTTX(data, base_time.format(new java.util.Date((pcrbase + options[57]) / 90))));
			}

			if (pcr && cBox[36].isSelected()) {
				if (!cBox[46].isSelected()) 
					buf.write(tf.getPCR(pcrbase,counter[countID],(0xFF&newID))); // no payload==no counter++
				else 
					buf.write(tf.getPCR(pcrbase,++counter[countID],(0xFF&newID)));
			}
			int a=0;
			while (a<datalength) {
				if (a==0) {
					if (datalength-a < 183) {
						TSHead[3] = (byte)(0x30 | (0xF&++counter[countID]));
						int stuff = 182-(datalength-a);
						adapt[0] = (byte)(stuff+1);
						buf.write(TSHead); 
						buf.write(adapt); 
						buf.write(fills,6,stuff);
						buf.write(data,a,datalength-a); 
						a+=(datalength-a);
					} else if (datalength-a == 184) {
						TSHead[3] = (byte)(0x10 | (0xF&++counter[countID]));
						buf.write(TSHead); 
						buf.write(data,a,184); 
						a+=184;
					} else if (datalength-a < 185) {
						TSHead[3] = (byte)(0x30 | (0xF&++counter[countID]));
						adapt[0] = 1;
						buf.write(TSHead); 
						buf.write(adapt); 
						buf.write(data,a,182); 
						a+=182;
					} else {
						TSHead[3] = (byte)(0x10 | (0xF&++counter[countID]));
						buf.write(TSHead); 
						buf.write(data,a,184); 
						a+=184;
					}
				} else if (datalength-a < 183) {
					TSHead2[3] = (byte)(0x30 | (0xF&++counter[countID]));
					int stuff = 182-(datalength-a);
					adapt[0] = (byte)(stuff+1);
					buf.write(TSHead2); 
					buf.write(adapt); 
					buf.write(fills,6,stuff);
					buf.write(data,a,datalength-a); 
					a+=(datalength-a);
				} else if (datalength-a == 184) {
					TSHead2[3] = (byte)(0x10 | (0xF&++counter[countID]));
					buf.write(TSHead2); 
					buf.write(data,a,184); 
					a+=184;
				} else if (datalength-a < 185) {
					TSHead2[3] = (byte)(0x30 | (0xF&++counter[countID]));
					adapt[0] = 1;
					buf.write(TSHead2); 
					buf.write(adapt); 
					buf.write(data,a,182); 
					a+=182;
				} else {
					TSHead2[3] = (byte)(0x10 | (0xF&++counter[countID]));
					buf.write(TSHead2); 
					buf.write(data,a,184); 
					a+=184;
				}
				options[39]+=188; 
				options[41]+=188;
			}
			buf.writeTo(out);
			buf.reset();
		}
		}
		catch (IOException e) { 
			Msg("writeTS error4"+e); 
		}
		return options;
	}


	//simple write the packet
	//DM14062004 81.7 int04 add
	public long[] writePacket(byte[] data, int off, int len) throws IOException
	{
		out.write(data, off, len); 
		options[39] += len; 
		options[41] += len;

		return options;
	}


	public void close() { 
		try 
		{
		if (makempg) { 
			out.write(packB9);
			options[39]+= 4;
			options[41]+= 4;

		}
		out.flush(); 
		out.close();

		if (makempg) { 
			RandomAccessFile mpg = new RandomAccessFile(name,"rw");
			if (packBA1[2]==1) {
				mpg.seek(0);
				mpg.write(packBA1);
			}
			mpg.seek(insert);

			Object[] ID = IDs.toArray(); 
			java.util.Arrays.sort(ID);     // write systemheader pointer stuff
			for (int a=0;a<ID.length;a++) {
				int IDt = (0xFF&Integer.parseInt(ID[a].toString()));
				if ((0xF0&IDt)==0xE0) { 
					sys[0][0]=(byte)(sysID[0]++); 
					mpg.write(sys[0]); 
					insert+=3; 
					streams[1]++; 
				} else if ((0xF0&IDt)==0x80) { 
					mpg.write(sys[1]); 
					insert+=3; 
					streams[0]++; 
				} else if ((0xE0&IDt)==0xC0) { 
					sys[2][0]=(byte)(sysID[1]++); 
					mpg.write(sys[2]); 
					insert+=3; 
					streams[0]++; 
				}
			}

			/**** add stuff ***/
			mpg.writeInt(0x1be);
			mpg.writeShort((short)(0x8A-insert));
			mpg.write(fills,0,(0x8A-insert));

			int syslen = ((streams[0]+streams[1])*3)+6;      // IDs (3byte) + std
			sysstd[4] = (byte)(syslen>>>8);                  // set systemheader length
			sysstd[5] = (byte)(0xFF&syslen);                 // set systemheader length 
			sysstd[9] = (byte)(streams[0]<<2);               // set number of audios
			sysstd[10] = (byte)(0x20 | (0x1F&streams[1]));   // set number of videos
			mpg.seek(14);
			mpg.write(sysstd);      // write system header

			for (int a=0;video && cBox[23].isSelected() && a<stuff.size();a++) {
				mpg.seek(Long.parseLong(stuff.get(a).toString()));
				mpg.write((byte)0xBE);
			}
			mpg.close();
		}

		if (toTS && qinfo && cBox[41].isSelected()) 
			tf.setPmtPids(IDs);

		//DM09082004 081.7 int08 outsourced
		if (toTS)
			name = TS.updateAdditionalHeader(name, time, comBox[20].getSelectedIndex());


		//DM18022004 081.6 int17 new
		if (toVdr==1 && cBox[54].isSelected() && new File(name).length()<150)
			out.deleteIdd();

		if (new File(name).length()<150) 
			new File(name).delete();
		else { 
			//DM18022004 081.6 int17 new
			if (toVdr==1 && cBox[54].isSelected())
				name = out.renameVdrTo(new File(name).getParent()+filesep,name);

			Msg("===> new File "+name);
			InfoAtEnd.add("Stream:\t "+name);
		}

		}
		catch (IOException e) { 
			Msg("writeVDR error(close)"+e); 
		}

		makempg=false;
		toTS=false;
	} 
}

//DM12122003 081.6 int05
class X_JFileChooser extends JFileChooser
{
	public X_JFileChooser()
	{
		super();
	}
	public void rescanCurrentDirectory()
	{
		String current_directory = d2vfield[8].getText();

		if (current_directory.startsWith("?"))
			chooser.setCurrentDirectory(new File(current_directory.substring(1)));
		else if(!current_directory.equals("") && chooser.getCurrentDirectory().toString().equals(System.getProperty("user.home")))
			chooser.setCurrentDirectory(new File(current_directory));

		super.rescanCurrentDirectory();
	}
}

//DM09082004 081.7 int08 add
public static int getForcedTTXLanguage()
{
	return (comBox[18].getSelectedIndex() - 1);
}


}  /**** end class X ****/









/***************************
 long

 not updated

 options[0] = -v bitrate recalculation from vbv buffer & delay : 0 or 1
 options[1] = -m bitrate in 1. sequence header = max <= 9Mbps  : 0 or 1
 options[2] = -a bitrate in 1. sequence header = average       : 0 or 1
 options[3] = -o do not change bitrate infos  : 0 or 1 , sets options[0] & [1] to 0
 options[4] = -1..9 bitrate in sequence header has a fix value : 2500..22500 = 1..9Mbps
 options[5] = lowest determined bitrate  : 262143..0 (*400)
 options[6] = highest determined bitrate : 0..262143 (*400)
 options[7] = videoframes counter for MPEG-PS : 0 ...
 options[8] = last videopts for MPEG-PS       : -10000 ...
 options[9] = -c delete CRC flag and 2 bytes in MPEG1+2 audio layer 1+2 : 0 or 1
 options[10] = 0; MPEG audio layer1+2 ,  1 = force to dual, 2 = force to stereo, 3 = force to jointstereo, 4 = 2chanel to 2*mono
 options[11] = pure Audio files : 0 or 1 (= another file.extension)
 options[12] = initial loopcounter for frame search in gop :  200...
 options[13] = 0, 1 = gopcheck called from completed sequence
 options[14] = sequence _frameconstants,  25fps = 3600
 options[15] = vbvbuffer , ~112 (1136)
 options[16] = -l copy last audio frame instead of generate a silence frame
 options[17] = s.u.
 options[18] = split size in bytes
 options[19] = splitcounter
 options[20] = position of last valid gop/seq header to continue split
 options[21] = 2nd split counter
 options[22] = vbvsize
 options[23] = vbvdelay
 options[24] = aspectratio
 options[25] = first vpts for simple audio split
 options[26] = write video = bit 0 set, write audio = bit  set
 options[27] = nextfilepts
 options[28] = audio offset
 options[29] = ac3 patch
 options[30] = biglog
 options[31] = extract raw streams from pva , if 1
 options[32] = add sequence end code
 options[33] = stream no. of op31
 options[34] = patch resolution for dvd
 options[35] = pure audio extension
 options[36] = megaradio
 options[37] = can stop cut
 options[38] = add frames
 options[39] = written MB
 options[40] = lastpts for simple video
 options[41] = possible written MB for split
 options[42] = exported videoframe timecounter
 options[43] = remember ids while splitting mpeg2,vdr -> 4)00 3)00 -2)00 1)00 /fix ids
 options[44] = remember ids es -> 4)00 3)00 -2)00 1)00 /fix ids
 options[45] = target return
 options[46] = count return
 options[47] = stop if more than 100 audiochange messages (like VBR)
 options[48] = summa exported videoframe timecounter
 options[49] = awrite v + w
 options[50] = counter for d2v projectfile
 options[51] = editable splitsize for d2v
 options[52] = cellid -vdrparse
 options[53] = ** nextTC
 options[54] = ** nextPTS
 options[55] = pts from pva
 options[56] = quick demux mbytes
 options[57] = pcr delta
******************************/

/***** options[17]
*  bit 1,0 : if options[10]>=4,  not used anymore, but still set
*   00 = std CBR in complete file
*   01 = VBR same mode = same BR
*   10 = VBR each frame/channel its own 
*   11 = free
*  bit 3,2 : 
*   00 = no restart
*   01 = restart due left ch.
*   10 = restart due right ch.
*   11 = restart due both ch.
*
* bit 8,6,5,4 :  bitrate value of last frame 2channel
* bit 12,11,10,9 :  bitrate value of last frame left
* bit 16,15,14,13 :  bitrate value of last frame right
*
* bit 17 : set if conversion isn't possible, clear befor the next file
*
* > bit 18  : current audioframes number
******/
