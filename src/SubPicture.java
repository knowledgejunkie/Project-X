/*
 * @(#)SUBPICTURE.java - creates SUP file to use as DVD subtitles
 *
 * Copyright (c) 2003-2004 by dvb.matt. 
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
 * thanx to Samuel Hocevar for his very helpful annotations of DVD subtitle RLE stuff
 * http://www.via.ecp.fr/~sam/doc/dvd/
 */

import java.awt.*;
import java.awt.font.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.io.*;
import java.util.*;
import java.awt.image.BufferedImage;

public class SubPicture extends JFrame {

public Picture picture;

String title = "Subtitle PreViewer";

public SubPicture() {
	addWindowListener(new WindowAdapter() {
		public void windowClosing(WindowEvent e) { close();}
	});
	picture = new Picture();
	getContentPane().add("Center",picture);

	setTitle(title);
	setSize(new Dimension(730,300)); //DM24012004 081.6 int11 changed
	setLocation(100,100);
	setVisible(false);
}

public void newTitle(String newtitle) { 
	setTitle(title+" "+newtitle); 
}

public void open() { 
	show(); //DM26032004 081.6 int18 changed
}

public void close() { 
	setVisible(false); 
}


public class Picture extends JPanel implements Runnable {

	public java.text.DateFormat sms = new java.text.SimpleDateFormat("HH:mm:ss.SSS");
	public Thread thread;
	private int w=720, h=288, x=20, nibble=0, val=0, even=2, alpha=10; //DM24012004 081.6 int11 changed
	private BufferedImage bimg;
	private Graphics2D big;
	private Font font, font_alt, font_std; //DM30122003 081.6 int10 add, //DM01032004 081.6 int18 add
	private FontRenderContext frc;
	private byte[] suppic = new byte[0];
	private Color[] color = {
		Color.gray,    // black /gray
		Color.red,     // red
		Color.green,   // green
		Color.yellow,  // yellow
		new Color(0xFF9090FF),    // blue, not to deep
		Color.magenta, // magenta
		Color.cyan,    // cyan
		Color.white,   // white
		Color.black,   // black background
	};
	private Object[] str = new Object[0];
	private byte[] RLEheader = { 0x53,0x50,0,0,0,0,0,0,0,0,0,0,0,0 }; // startcode + later reverse 5PTS, DTS=0
	private byte[] sections = {
		0,0,           // size  
		3,0x32,0x10,         // color palette linkage
		4,(byte)0xFF,(byte)0xFA,         // color alpha channel linkage F=opaque
		5,0,0,0,0,0,0, // coordinates Xa,Ya,Xe,Ye
		6,0,0,0,0,     // bytepos start odd, start even
		1,(byte)0xFF,    // 0xFF
		1,0x50,0,0,2,(byte)0xFF     // section 01, end sequ: timedur in pts/900, size s.a. , add 0xFF if size is not even
	};
	private ByteArrayOutputStream out = new ByteArrayOutputStream();
	private byte[] newline = { 0,0 };
	private int Rect[] = new int[4], pos[] = new int[4];
	private int[] pgc = new int[4], pgca = { 0,13,14,15 };
	private int[] option = new int[8];
	private int[] std = { 28,10,32,60,600,720,576 };  // ("Font pointsize; Backgr. Alpha value; Xoffset; Yoffset; Screenwidth");


	public Picture() { 
		setBackground(Color.gray);
		setVisible(true);
		sms.setTimeZone(java.util.TimeZone.getTimeZone("GMT+0:00"));
	}

	public void paint(Graphics g) {
		if (big == null) 
			return;
		g.drawImage(bimg, 0, 0, this);
	}

	/*** paint pic from ttx **/
	public void showPicTTX(Object[] str) {
		this.str = str;
		buildImgTTX();
		repaint();
	}

	// set display time
	//DM22032004 081.6 int18 changed
	public byte[] setTime(byte tmp[], long out_time)
	{
		long in_time = 0;

		for (int a=0; a<4; a++) // in_pts
			in_time |= (0xFF & tmp[a+2])<<(a*8);

		long difference = (long)Math.round((out_time - in_time) / 900.0);

		int tp = (0xFF & tmp[12])<<8 | (0xFF & tmp[13]);
		tmp[34+tp] = (byte)(0xFF & difference>>>8);
		tmp[35+tp] = (byte)(0xFF & difference);
		newTitle(" / in: "+sms.format(new java.util.Date(in_time / 90))+" duration: "+sms.format(new java.util.Date(difference * 10)) );

		return tmp;
	}

	/*** build Image **/
	public void buildImgTTX() {   //DM30122003 081.6 int10 changed
		Rect[0] = option[3];          // x 60
		Rect[1] = h-option[2]-(option[0]*(1+str.length));   // y 188-32-(28*2) = 100
		Rect[2] = option[4];          // w 600
		Rect[3] = 12+(option[0]*str.length);      // h 12+(28*1) höhe
		pos[0] = Rect[0];     //x = 60
		pos[1] = option[6]-option[2]-Rect[3];    //+y 576-32-40
		pos[2] = pos[0]+Rect[2];   //x2 = 660
		pos[3] = pos[1]+Rect[3]; //+y2  = (504+40) 544
		//   pos[3] = pos[1]+Rect[1]+Rect[3]; //+y2

		//DM08032004 081.6 int18 add
		paintVideoSize();
		big.setColor(Color.white);
		big.drawRect(Rect[0]-1,Rect[1]-1,Rect[2]+2,Rect[3]+2);
		big.setFont(font_std);
		big.drawString("x"+pos[0]+", y"+pos[1]+" / "+(pos[2]-pos[0])+"*"+(pos[3]-pos[1]), Rect[0] - 1, Rect[1] - 5);

		big.setColor(color[8]);
		big.fillRect(Rect[0],Rect[1],Rect[2],Rect[3]); // black background

		for (int a=0;a<str.length;a++) {  // paint ascii char
			int[] chars = (int[])str[a];
			x=option[3];
			for (int b=0;b<chars.length;b++) {
				if ((0xF&(chars[b]>>>4))>0)
					big.setFont(font_alt);
				else
					big.setFont(font);
				big.setColor(color[7&chars[b]]);
				big.drawString(""+(char)(chars[b]>>>8),x,(h-option[2])-(option[0]*(str.length-a)));
				x += font.getStringBounds(""+(char)(chars[b]>>>8),frc).getWidth();
			}
		}
	}

	/*** return sup format for writing **/
	public byte[] writeRLE(long pts, int duration) {
		try 
		{

		int[] pixels = new int[Rect[2]*Rect[3]];  
		bimg.getRGB(Rect[0],Rect[1],Rect[2],Rect[3],pixels,0,Rect[2]);
		System.arraycopy(pgca,0,pgc,0,4);

		out.reset(); 
		out.write(RLEheader);   //start picture in .sup form
		even=0;

		for (int i=0,l=0,a=0,b=0,co=0; i<2; i++) {        // read out interlaced RGB
			for (l=0,co=0,a=i*Rect[2]; a<pixels.length; a+=(2*Rect[2])) {   // odd lines first
				for (l=0,co=0,b=0; b<Rect[2]; b++,l++) {   // l-1 = len??
					if ((0xFFFFFF&pixels[a+b])!=co) {
						updateRLE(l,co);      // write last RLE nibbles, while color change
						co=(0xFFFFFF&pixels[a+b]); 
						l=0;
					} else if ( l>254 ) {
						updateRLE(l,co);      // write last RLE nibbles, cannot incl. more than 255 pixels
						l=0;
					}
					// std: adds l-bit to active color
				}
				while ( l>255 ) { 
					updateRLE(255,co); 
					l=l-255; 
				}
				updateRLE(l,co);   // write last RLE nibbles, line end
				alignRLE();
				out.write(newline);  // new line CR, byte aligned
			}
			alignRLE();
			if (even==0) 
				even = out.size()-10;        // save startpos of even pic (size-14)
		}

		int pack = out.size()-12;
		int control = pack+24;
		int dur = out.size()+22;

		// set planned pic pos. on tvscreen
		sections[9] = (byte)(pos[0]>>>4);
		sections[10] = (byte)(pos[0]<<4 | pos[2]>>>8);
		sections[11] = (byte)pos[2];
		sections[12] = (byte)(pos[1]>>>4);
		sections[13] = (byte)(pos[1]<<4 | pos[3]>>>8);
		sections[14] = (byte)pos[3];

		// set byte pos. for pic start
		sections[16] = 0;
		sections[17] = 4;
		sections[18] = (byte)(0xFF&even>>>8);
		sections[19] = (byte)(0xFF&even);
		sections[0] = sections[24] = (byte)(0xFF&control>>>8);
		sections[1] = sections[25] = (byte)(0xFF&control);

		// set color links for PGCITI
		sections[3] = (byte)(pgc[3]<<4 | pgc[2]);
		sections[4] = (byte)(pgc[1]<<4 | pgc[0]);
		sections[7] = (byte)((0xF0&sections[7]) | (0xF&alpha));

		out.write(sections);  //write control block
		if ((out.size()&1) == 1)   // changed for X0.81
			out.write((byte)255);
		out.flush();
		suppic = out.toByteArray();
		int size = suppic.length-10;
		suppic[10] = (byte)(0xFF&size>>>8);
		suppic[11] = (byte)(0xFF&size);
		suppic[12] = (byte)(0xFF&pack>>>8);
		suppic[13] = (byte)(0xFF&pack);

		for (int a=0;a<4;a++) 
			suppic[a+2] = (byte)(0xFF & pts>>>(a*8));
		suppic[dur] = (byte)(0xFF & duration>>>8);
		suppic[dur+1] = (byte)(0xFF & duration);

		} 
		catch (IOException e) { 
			X.Msg("-> subpic write error"); 
		}

		return suppic;
	}

	/***  **/
	public void alignRLE() {  // write last nibble, if it was not aligned
		if (nibble==0) 
			return;
		else { 
			out.write((byte)val); 
			val=nibble=0; 
		}
	}

	/*** add pixels to RLE **/
	public void updateRLE(int l, int co) {  // max 1 bgr + 3 text color links per RLE
		int pgcco=0;
		switch (co) { 
			case        0: { pgcco=0; break; }   //black meant for half trans background
			case 0xFFFFFF: { pgcco=1; break; }    //white
			case 0xFF0000: { pgcco=2; break; }    //red
			case   0xFF00: { pgcco=3; break; }    //green
			case 0x9090FF: { pgcco=4; break; }    //light blue
			case 0xFFFF00: { pgcco=5; break; }    //yellow
			case   0xFFFF: { pgcco=6; break; }    //cyan
			case 0xFF00FF: { pgcco=7; break; }    //magenta
			case 0x808080: { pgcco=12; break; }    //gray
			default:       { pgcco=0; break; }
		}

		for (int a=1; pgcco>0 && a<pgc.length; a++) {  // set colorlinks for PGCITI
			co=-1;
			if (pgc[a]>12 || pgc[a]==pgcco) { 
				pgc[a]=pgcco; 
				co=a; 
				break; 
			}
		}
		if (co==-1) 
			co=1;

		l = l<<2 | co;  // combine bits + color

		if (nibble==0) {  // new byte begin
			if (l>0xFF) { 
				out.write((byte)(0xFF & l>>>8)); 
				out.write((byte)(0xFF & l)); 
			} // 16
			else if (l>0x3F) { 
				out.write((byte)(0xFF & l>>>4)); 
				val=0xF0 & l<<4; 
				nibble=4; 
			}  // 12;
			else if (l>0xF)  { 
				out.write((byte)(0xFF & l)); 
			} // 8
			else { 
				val=0xF0 & l<<4; 
				nibble=4; 
			}   // 4
		} else {          // middle of byte
			if (l>0xFF) { 
				out.write((byte)(val | (0xF & l>>>12))); 
				out.write((byte)(0xFF & l>>>4)); 
				val=0xF0 & l<<4; 
			} // 16
			else if (l>0x3F) { 
				out.write((byte)(val | (0xF & l>>>8))); 
				out.write((byte)(0xFF & l)); 
				val=nibble=0; 
			}  // 12;
			else if (l>0xF)  { 
				out.write((byte)(val | (0xF & l>>>4))); 
				val=0xF0 & l<<4; 
			} // 8
			else { 
				out.write((byte)(val | (0xF & l))); 
				val=nibble=0; 
			}   // 4
		}
	}


	public void set2() { 
		option[2]=option[7]; 
	};

	/*** set user data ("Font pointsize; Backgr. Alpha value; Yoffset; Xoffset; Screenwidth"); **/
	public int set(String nm, String values) {
		System.arraycopy(std,0,option,0,std.length);
		StringTokenizer st = new StringTokenizer(values,";");
		int a=0;
		while (st.hasMoreTokens() && a<option.length) {
			option[a] = Integer.parseInt(st.nextToken());
			a++;
		}
		alpha = 0xF & option[1];
		font = new Font(nm, Font.BOLD, option[0]);
		font_alt = new Font(nm, Font.BOLD|Font.ITALIC, option[0]); //DM30122003 081.6 int10 add
		font_std = new Font("Sans Serif", Font.PLAIN, 14); //DM01032004 081.6 int18 add
		return option[7];
	}

	public void run() {
		bimg = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		big = bimg.createGraphics();
		set("SansSerif", (""+"28;10;32;60;600;720;576;-1"));
		//   big.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		frc = big.getFontRenderContext();
	}

	//DM08032004 081.6 int18 new
	private int paintVideoSize()
	{
		int[] video_basics = X.getVideoBasics();
		if (video_basics[0]==0)  // H
			video_basics[0] = 720;
		if (video_basics[1]==0)  // V
			video_basics[1] = 576;

		//deep red background to verify picture rectangle with given video resolution
		big.setColor(new Color(0xFF550000));
		big.fillRect(0, 0, w, h);

		//picture area which the subpicture must not exceed, have to adjust to the hor. middle of it
		big.setColor(Color.gray);
		big.fillRect(0, 0, video_basics[0], video_basics[1]-h);

		return video_basics[1];
	}

	//DM28022004 081.6 int18 new
	private int Get_Bits(byte buf[], int BPos[], int N)
	{
		int Pos, Val;
		Pos = BPos[1]>>>3;
		Val =   (0xFF&buf[Pos])<<24 |
			(0xFF&buf[Pos+1])<<16 |
			(0xFF&buf[Pos+2])<<8 |
			(0xFF&buf[Pos+3]);
		Val <<= BPos[1] & 7;
		Val >>>= 32-N;
		BPos[1] += N;
		BPos[0] = BPos[1]>>>3;
		return Val;
	}

	//DM28022004 081.6 int18 new
	private int Show_Bits(byte buf[], int BPos[], int N)
	{
		int Pos, Val;
		Pos = BPos[1]>>>3;
		Val =   (0xFF&buf[Pos])<<24 |
			(0xFF&buf[Pos+1])<<16 |
			(0xFF&buf[Pos+2])<<8 |
			(0xFF&buf[Pos+3]);
		Val <<= BPos[1] & 7;
		Val >>>= 32-N;
		return Val;
	}

	//DM28022004 081.6 int18 new
	private void Flush_Bits(int BPos[], int N)
	{
		BPos[1] += N;
		BPos[0] = BPos[1]>>>3;
	}

	//DM28022004 081.6 int18 new
	private void align_Bits(int BPos[])
	{
		if ((1 & BPos[1]>>>2) != 0)
			Flush_Bits( BPos, 4);
	}

	//DM28022004 081.6 int18 new
	private final int std_colors[] = {
		0xFF000000, 0xFFFFFFFF, 0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 
		0xFFFFFF00, 0xFFFF00FF,	0xFF00FFFF, 0xFFFF5555, 0xFF55FF55,	
		0xFF5555FF, 0xFFFFFF55, 0xFFFF55FF,	0xFF55FFFF, 0xFF993333, 
		0xFF333399
	};

	//DM28022004 081.6 int18 new
	public int decode_picture(byte packet[], int off, boolean decode)
	{
		boolean simple_picture = false;
		int picture_length = packet.length;
		byte data[] = new byte[picture_length +4];
		System.arraycopy(packet,0,data,0,picture_length);


		int BPos[] = { off, off<<3 }; //BytePos, BitPos
		int position[] = new int[4];
		int start_pos[] = new int[3];
		int print_colors[] = new int[4];

		if (BPos[0] > picture_length-4)
			return -1;

		int packetlength = Get_Bits(data, BPos, 16); // required pack length
		if (BPos[0] + packetlength != picture_length + 2)
			return -2;

		start_pos[2] = Get_Bits(data, BPos, 16) - 2;
		Flush_Bits(BPos, start_pos[2]<<3); // jump to sections chunk

		int playtime_pos = Get_Bits(data, BPos, 16);

		if (playtime_pos == start_pos[2] + 2)
		{
			playtime_pos = packetlength;
			simple_picture = true;
			X.Msg("!> simple picture packet");
		}
		else
			start_pos[2] += off+2;

		while (BPos[0] < off + playtime_pos)  // read sections chunk
		{
			switch(Get_Bits(data, BPos, 8))
			{
			case 0: // alias
				break;
			case 1: // alias
				Flush_Bits(BPos, 8);
				break;
			case 3: // 4 color links
				for (int b=0; b<4; b++)
					print_colors[b] |= (std_colors[Get_Bits(data, BPos, 4)] & 0xFFFFFF);
				break;
			case 4: // alpha blending
				for (int b=0; b<4; b++)
					print_colors[b] |= (0x11 * (0xF ^ Get_Bits(data, BPos, 4)))<<24;
				break;
			case 5: // x,y pos.
				for (int b=0; b<4; b++)
					position[b] = Get_Bits(data, BPos, 12);
				break;
			case 6: // pos. of decode_start of a field
				for (int b=0; b<2; b++)
					start_pos[b] = Get_Bits(data, BPos, 16);
				break;
			}
		}

		if (off + playtime_pos != BPos[0])
			return -3;

		int playtime = 0;
		if (!simple_picture)
		{
			playtime = Get_Bits(data, BPos, 16);
			if (playtime_pos != Get_Bits(data, BPos, 16))
				return -4;

			if (Get_Bits(data, BPos, 8) != 2)
				return -5;

			Flush_Bits( BPos, ((BPos[0] & 1) != 1) ? 16 : 8 );
		}

		if (BPos[0] != picture_length)
			return -6;

		if (!decode)
			return (playtime * 900);


		for (int b=0; b<2; b++)
			start_pos[b] += off;

		int v_res = paintVideoSize();

		int y0 = position[2] - (v_res - h);
		big.setColor(Color.white);
		big.drawRect(position[0]-1,y0-1,position[1]-position[0]+2,position[3]-position[2]+2);
		big.setFont(font_std);
		big.drawString("x"+position[0]+", y"+position[2]+" / "+(position[1]-position[0])+"*"+(position[3]-position[2]), position[0] - 1, y0 - 5);

		for (int b=0; b<2; b++)
		{
			int Val=0, x1 = position[0], y1 = y0 + b;
			BPos[1] = (BPos[0] = start_pos[b])<<3; // top_field at first

			while (BPos[0] < start_pos[b+1]) // stop at pos_marker
			{
				if ((Val = Get_Bits(data, BPos, 4)) > 3) //4..F (0..3 never encodable)
				{
					big.setColor(new Color(std_colors[Val & 3]));
					big.drawLine(x1,y1,(x1+=Val>>>2),y1);
				}
				else if ((Val = Val<<4 | Get_Bits(data, BPos, 4)) > 0xF) //10..3F
				{
					big.setColor(new Color(std_colors[Val & 3]));
					big.drawLine(x1,y1,(x1+=Val>>>2),y1);
				}
				else if ((Val = Val<<4 | Get_Bits(data, BPos, 4)) > 0x3F) //40..FF
				{
					big.setColor(new Color(std_colors[Val & 3]));
					big.drawLine(x1,y1,(x1+=Val>>>2),y1);
				}
				else if ((Val = Val<<4 | Get_Bits(data, BPos, 4)) > 0) //100..3FF
				{
					big.setColor(new Color(std_colors[Val & 3]));
					big.drawLine(x1,y1,(x1+=Val>>>2),y1);
				}
				else  // 0 forced carriage return
				{
					x1=position[0];
					y1+=2;
					align_Bits(BPos);
					continue;
				}

			/**
				if (x1 >= position[1]) // line end, carriage return
				{
					x1=position[0];
					y1+=2;
					align_Bits(BPos);
				}
			**/
			}
		}

		repaint();

		return (playtime * 900);
	}


}

}
