/*
 * @(#)SUBPICTURE.java - creates SUP file to use as DVD subtitles
 *
 * Copyright (c) 2003-2005 by dvb.matt, All Rights Reserved.
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

package net.sourceforge.dvb.projectx.subtitle;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.font.FontRenderContext;
import java.awt.image.BufferedImage;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;

import javax.swing.JFrame;
import javax.swing.JPanel;

import net.sourceforge.dvb.projectx.common.Resource;
import net.sourceforge.dvb.projectx.common.X;



public class SubPicture extends JFrame
{

public Picture picture;

String title = Resource.getString("subpicture.title");

public SubPicture()
{
	addWindowListener(new WindowAdapter()
	{
		public void windowClosing(WindowEvent e)
		{
			close();
		}
	});

	picture = new Picture();
	picture.run(); //DM18052004 081.7 int02 add

	getContentPane().add("Center", picture);

	setTitle(title);
	setSize(new Dimension(726,601)); //DM24012004 081.6 int11 changed, //DM20042004 081.7 int02 changed
	setLocation(100,100);
	setResizable(false); //DM17042004 081.7 int02 add
	//setVisible(false);
}

public void newTitle(String newtitle)
{
	setTitle(title+" "+newtitle); 
}

public void close()
{ 
	dispose(); //DM18052004 081.7 int02 changed
}


public class Picture extends JPanel implements Runnable
{
	public java.text.DateFormat sms = new java.text.SimpleDateFormat("HH:mm:ss.SSS");
	public Thread thread;
	private int w=720, h=576, x=20, nibble=0, val=0, default_alpha=10; //DM24012004 081.6 int11 changed, //DM20042004 081.7 int02 changed
	private int modified_alpha = 0;

	private BufferedImage bimg;
	private Graphics2D big;
	private Font font, font_alt, font_std; //DM30122003 081.6 int10 add, //DM01032004 081.6 int18 add
	private FontRenderContext frc;

	//DM26052004 081.7 int03 changed
	private final int default_teletext_colors[] = {
		//bg = 0 = black
		0xFF606060, //Y 40%
		0xFFEB6060, //red lighter
		0xFF10EB10, //green
		0xFFEBEB10, //yellow
		0xFF5050EB, //blue lighter
		0xFFEB60EB, //magenta lighter
		0xFF10EBEB, //cyan
		0xFFEBEBEB, //Y 100%

		//bg = 1 = red
		0xFF606060, //Y black
		0xFFE06060, //red
		0xFF60E060, //green
		0xFFE0E060, //yellow
		0xFF6060E0, //blue
		0xFFE060E0, //magenta
		0xFF60E0E0, //cyan
		0xFFE0E0E0, //white-gray

		//bg = 2 = green
		0xFF707070, //Y black
		0xFFD07070, //red
		0xFF70D070, //green
		0xFFD0D070, //yellow
		0xFF7070D0, //blue
		0xFFD070D0, //magenta
		0xFF70D0D0, //cyan
		0xFFD0D0D0, //white-gray

		//bg = 3 = yellow
		0xFF808080, //Y black
		0xFFC08080, //red
		0xFF80C080, //green
		0xFFC0C080, //yellow
		0xFF8080C0, //blue
		0xFFC080C0, //magenta
		0xFF80C0C0, //cyan
		0xFFC0C0C0, //white-gray

		//bg = 4 = blue
		0xFF909090, //Y black
		0xFFC09090, //red
		0xFF90C090, //green
		0xFFC0C090, //yellow
		0xFF9090C0, //blue
		0xFFC090C0, //magenta
		0xFF90C0C0, //cyan
		0xFFB0B0B0, //white-gray

		//bg = 5 = magenta
		0xFFA0A0A0, //Y black
		0xFFB0A0A0, //red
		0xFFA0B0A0, //green
		0xFFB0B0A0, //yellow
		0xFFA0A0B0, //blue
		0xFFB0A0B0, //magenta
		0xFFA0B0B0, //cyan
		0xFFA0A0A0, //white-gray

		//bg = 6 = cyan
		0xFFB0B0B0, //Y black
		0xFFC0A0A0, //red
		0xFFA0D0A0, //green
		0xFFC0D0A0, //yellow
		0xFFA0A0D0, //blue
		0xFFD0A0D0, //magenta
		0xFFA0D0D0, //cyan
		0xFF909090, //white-gray

		//bg = 7 = white
		0xFFD0D0D0, //Y black
		0xFFE09090, //red
		0xFF90E090, //green
		0xFFE0E090, //yellow
		0xFF9090E0, //blue
		0xFFE090E0, //magenta
		0xFF90E0E0, //cyan
		0xFF808080, //white-gray

		0, // backgr black, user_transparency
		0x80 // backgr blue, sign for full transparency
	};

	private final int default_sup_colors[] = {
		0xFF101010, //black
		0xFFA0A0A0, //Y 50%
		0xFFEBEBEB, //Y 100%
		0xFF606060, //Y 25%
		0xFFEB1010, //red
		0xFF10EB10, //green
		0xFFEBEB10, //yellow
		0xFF1010EB, //blue
		0xFFEB10EB, //magenta
		0xFF10EBEB, //cyan
		0xFFEB8080, //red lighter
		0xFF80EB80, //green lighter
		0xFFEBEB80, //yellow lighter
		0xFF8080EB, //blue lighter
		0xFFEB80EB, //magante lighter
		0xFF80EBEB, //cyan lighter
		0 // full transparency black bg
	};

	private Object[] str = new Object[0];
	private byte[] RLEheader = { 0x53,0x50,0,0,0,0,0,0,0,0,0,0,0,0 }; // startcode + later reverse 5PTS, DTS=0
	private byte[] sections = {
		0, 0,           // next contr sequ.
		3, 0x32, 0x10,         // color palette linkage
		4, (byte)0xFF, (byte)0xFA,         // color alpha channel linkage F=opaque
		5, 0, 0, 0, 0, 0, 0, // coordinates Xa,Ya,Xe,Ye
		6, 0, 0, 0, 0,     // bytepos start top_field, start bottom_field
		1,  // start displ.  //0 means force display
		(byte)0xFF,    // end of sequ.
		1, 0x50,  // time for next sequ,
		0, 0,  //next contr sequ.
		2,   // stop displ.
		(byte)0xFF     // end of sequ: timedur in pts/1100, size s.a. , add 0xFF if size is not WORD aligned
	};

	private ByteArrayOutputStream out = new ByteArrayOutputStream();
	private byte newline[] = { 0,0 };
	private int Rect[] = new int[4];
	private int pos[] = new int[4];
	private int option[] = new int[10]; //DM26052004 081.7 int03 changed

	//DM26052004 081.7 int03 changed
	private int standard_values[] = { 26, 10, 32, 80, 560, 720, 576, -1, 4 };

	private ArrayList user_color_table = new ArrayList();
	private Bitmap bitmap;
	private boolean read_from_Image = false;
	private int isforced_status = 0;
	private boolean global_error = false;

	private int line_offset = 28;

	public DVBSubpicture dvb = new DVBSubpicture(); //DM24042004 081.7 int02 new

	public Picture()
	{ 
		bimg = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		big = bimg.createGraphics();

		set("Tahoma", ("" + "26;10;32;80;560;720;576;-1;4"));
		frc = big.getFontRenderContext();

		//   big.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		setBackground(Color.gray);
		sms.setTimeZone(java.util.TimeZone.getTimeZone("GMT+0:00"));
	}

	public void paint(Graphics g)
	{
		if (big == null) 
			return;

		g.drawImage(bimg, 0, 0, this);
	}

	/*** paint pic from ttx **/
	public void showPicTTX(Object[] str)
	{
		this.str = str;
		buildImgTTX();
		repaint();
	}

	// set display time
	//DM22032004 081.6 int18 changed
	//DM26052004 081.7 int03 changed
	public byte[] setTime(byte tmp[], long out_time)
	{
		long in_time = 0;

		for (int a=0; a<4; a++) // in_pts
			in_time |= (0xFF & tmp[a+2])<<(a*8);

		//long difference = (long)Math.round((out_time - in_time) / 1100.0); // 900.0
		long difference = 1L + ((out_time - in_time) / 1000);

		int tp = (0xFF & tmp[12])<<8 | (0xFF & tmp[13]);
		tmp[34+tp] = (byte)(0xFF & difference>>>8);
		tmp[35+tp] = (byte)(0xFF & difference);

		newTitle(" / " + Resource.getString("subpicture.in_time") + ": " + sms.format(new java.util.Date(in_time / 90)) + " " + Resource.getString("subpicture.duration") + ": " + sms.format(new java.util.Date((out_time - in_time) / 90)) );

		return tmp;
	}

	/*** build Image from text **/
	//DM30122003 081.6 int10 changed
	//DM05052004 081.7 int02 changed
	public void buildImgTTX()
	{
		int space = 6;
		Rect[0] = option[3];
		//Rect[3] = (2 * space) + (option[0] * str.length);
		Rect[3] = (2 * space) + (line_offset * str.length);
		Rect[1] = option[6] - option[2] - Rect[3];
		Rect[2] = option[4];

		pos[0] = Rect[0];
		pos[1] = Rect[1];
		pos[2] = Rect[0] + Rect[2] - 1;
		pos[3] = Rect[1] + Rect[3] - 1;

		//DM08032004 081.6 int18 add
		paintVideoSize();

		big.setColor(Color.white);
		big.drawRect(Rect[0] - 1, Rect[1] - 1, Rect[2] + 1, Rect[3] + 1);
		big.setFont(font_std);
		big.drawString("x" + pos[0] + ", y" + pos[1] + " / " + (pos[2] - pos[0] + 1) + "*" + (pos[3] - pos[1] + 1), Rect[0] - 1, Rect[1] - 5);

		int color_table[] = getColorTable(1);

		big.setFont(font);

		ArrayList list = new ArrayList();
		boolean antialiasing;

		/**
		 * pre-check, whether we have not more than 2 'speaker-colors'
		 */
		for (int a = 0; a < str.length; a++)
		{
			int[] chars = (int[])str[a];

			for (int b = 0; b < chars.length; b++)
			{
				//source background color
				int offset = (7 & chars[b]>>>4)<<3;

				//source modified foreground color
				int paint_color = color_table[offset + (7 & chars[b])];
				String str = Integer.toString(paint_color);
				String sign = new Character((char)(chars[b]>>>8)).toString();

				//remember new color
				if (list.indexOf(str) < 0 && !sign.equals(" "))
					list.add(str);
			}
		}

		/**
		 * define background; if less than 3 front-colors, use 'simple' antialiasing with full transparency
		 */
		if (list.size() < 3 && X.cBox[79].isSelected())
		{
			big.setColor(new Color(color_table[65])); // deep blue, full transp
			modified_alpha = 0;
			antialiasing = true;
		}
		else
		{
			big.setColor(new Color(color_table[64])); // black, half transp
			modified_alpha = default_alpha;
			antialiasing = false;
		}

		big.fillRect(Rect[0], Rect[1], Rect[2], Rect[3]); // background

		Rectangle2D r;

		// paint background of char
		for (int a = 0; antialiasing && a < str.length; a++)
		{
			int[] chars = (int[])str[a];
			String str = "";

			big.setColor(new Color(color_table[64])); // black

			/**
			 * concatenate string, no special colors required
			 */
			for (int i = 0; i < chars.length; i++)
				str += new Character((char)(chars[i]>>>8)).toString();

			x = option[3];
			//int y = Rect[1] + (option[0] * (1 + a));
			int y = Rect[1] + (line_offset * (1 + a));
			int[] offs = { 2, 3, 3, 3, 3, 3, 2 };

			for (int i = 0; i < offs.length; i++) // horiz. lines
			{
				int _x = x;
				int _y = y - (offs.length / 2) + i;

				for (int j = -offs[i]; j < offs[i] + 1; j++)
					big.drawString(str, _x + j, _y);
			}
		}


		// paint ascii char
		for (int a=0; a < str.length; a++)
		{
			int[] chars = (int[])str[a];
			x = option[3];

			//DM26052004 081.7 int03 changed
			for (int b=0; b < chars.length; b++)
			{
				//source background color
				int offset = (7 & chars[b]>>>4)<<3;

				//source foreground color
				big.setColor(new Color(color_table[offset + (7 & chars[b])]));
				//big.drawString("" + (char)(chars[b]>>>8), x, Rect[1] + (option[0] * (1 + a)));
				big.drawString("" + (char)(chars[b]>>>8), x, Rect[1] + (line_offset * (1 + a)));

				x += font.getStringBounds("" + (char)(chars[b]>>>8), frc).getWidth();
			}
		}
	}

	public void resetUserColorTable()
	{
		user_color_table.clear();
	}

	public Object[] getUserColorTableArray()
	{
		return user_color_table.toArray();
	}

	public ArrayList getUserColorTable()
	{
		return user_color_table;
	}

	public void updateUserColorTable(Bitmap new_bitmap)
	{
		bitmap = new_bitmap;

		int pixel[] = bitmap.getPixel();

		for (int a=0; a < pixel.length; a++)
		{
			String pixel_str = "" + pixel[a];

			if (!user_color_table.contains(pixel_str))
				user_color_table.add(pixel_str);

			bitmap.getColorIndex(getUserColorTableIndex(pixel[a]));
		}
	}

	private void updateUserColorTable(int pixel[])
	{
		for (int a=0; a < pixel.length; a++)
		{
			String pixel_str = "" + pixel[a];

			if (user_color_table.contains(pixel_str))
				continue;

			else
				user_color_table.add(pixel_str);
		}
	}

	private int getUserColorTableIndex(int color_index)
	{
		int value;

		if ((value = user_color_table.indexOf("" + color_index)) < 0)
			return 0;

		return value;
	}

	//DM26052004 081.7 int03 changed
	public byte[] writeRLE(long start_time, int onscreen_time) throws IOException
	{
		read_from_Image = true; // use user defined alpha value for color index 0

		bitmap = new Bitmap( Rect[0], Rect[1], Rect[2], Rect[3], bimg.getRGB(Rect[0], Rect[1], Rect[2], Rect[3], null, 0, Rect[2]), 2, 0, 1, 2, start_time, onscreen_time);

		return buildRLE();
	}

	public byte[] writeRLE(Bitmap new_bitmap) throws IOException
	{
		bitmap = new_bitmap;
		setArea();

		return buildRLE();
	}

	private byte[] buildRLE()
	{
		byte picture_packet[] = null;

		try 
		{

		int pixels[] = bitmap.getPixel();

		updateUserColorTable(pixels);

		out.reset(); 
		out.write(RLEheader);   //start picture in .sup form

		int bottom_field_start_pos = 0;

		// read out interlaced RGB
		for (int i=0, l=0, a=0, b=0, color_index=0; i < 2; i++)
		{
			// top_field first
			for (l=0, color_index=0, a = i * bitmap.getWidth(); a < pixels.length; a += (2 * bitmap.getWidth()))
			{
				for (l=0, color_index=0, b=0; b < bitmap.getWidth(); b++, l++)
				{
					if (pixels[a + b] != color_index)
					{
						// write last RLE nibbles, while color change
						updateRLE(l, color_index);
						color_index = pixels[a + b];
						l=0;
					}
					else if ( l > 254 )
					{
						// write last RLE nibbles, cannot incl. more than 255 pixels
						updateRLE(l, color_index);
						l=0;
					}
					// std: adds l-bit to active color
				}

				l -= 1;

				while ( l > 255 )  // never used ?!
				{ 
					updateRLE(255, color_index); 
					l -= 255; 
				}

				updateRLE(l, color_index);   // write last RLE nibbles, line end
				alignRLE();
				out.write(newline);  // new line CR, byte aligned
			}

			alignRLE();

			if (bottom_field_start_pos == 0) 
				bottom_field_start_pos = out.size() - 10;        // save startpos of bottom_field (size-14)
		}

		out.write(newline);  //DM26052004 081.7 int03 add , not the best solution, but need the "0,0" here

		int pack = out.size() - 12;
		int control_block_pos = pack + 24;
		int onscreen_time_pos = out.size() + 22;

		setScreenPosition(bitmap.getX(), bitmap.getY(), bitmap.getMaxX() - 1, bitmap.getMaxY() - 1);
		setControlBlockPosition(control_block_pos, bottom_field_start_pos);
		setPGCsection();

		out.write(sections);  //write control_block

		if ((out.size() & 1) == 1)
			out.write((byte)255);

		out.flush();

		picture_packet = out.toByteArray();

		int size = picture_packet.length - 10;

		picture_packet[10] = (byte)(0xFF & size>>>8);
		picture_packet[11] = (byte)(0xFF & size);
		picture_packet[12] = (byte)(0xFF & pack>>>8);
		picture_packet[13] = (byte)(0xFF & pack);

		for (int a=0; a < 4; a++) 
			picture_packet[a + 2] = (byte)(0xFF & bitmap.getInTime()>>>(a*8));

		picture_packet[onscreen_time_pos] = (byte)(0xFF & bitmap.getPlayTime()>>>8);
		picture_packet[onscreen_time_pos + 1] = (byte)(0xFF & bitmap.getPlayTime());

		} 
		catch (IOException e)
		{ 
			X.Msg(Resource.getString("subpicture.msg1")); 
		}

		read_from_Image = false;

		return picture_packet;
	}

	// write last nibble, if it was not aligned
	private void alignRLE()
	{
		if (nibble == 0) 
			return;

		else
		{ 
			out.write((byte)val); 
			val = nibble = 0; 
		}
	}

	private void updateRLE(int l, int color_index)
	{
		if (l < 1)
			return;

		// color_index shall not exceed value 3!
		int pgc_color = getUserColorTableIndex(color_index);

		pgc_color = bitmap.getColorIndex(pgc_color);

		l = l<<2 | pgc_color;  // combine bits + color_index

		// new byte begin
		if (nibble == 0)
		{  
			if (l > 0xFF) // 16
			{ 
				out.write((byte)(0xFF & l>>>8)); 
				out.write((byte)(0xFF & l)); 
			} 
			else if (l > 0x3F)  // 12
			{ 
				out.write((byte)(0xFF & l>>>4)); 
				val = 0xF0 & l<<4; 
				nibble = 4; 
			}
			else if (l > 0xF)  // 8
			{
				out.write((byte)(0xFF & l)); 
			} 
			else   // 4
			{ 
				val = 0xF0 & l<<4; 
				nibble = 4; 
			}
		}
		else  // middle of byte
		{
			if (l > 0xFF) // 16
			{ 
				out.write((byte)(val | (0xF & l>>>12))); 
				out.write((byte)(0xFF & l>>>4)); 
				val = 0xF0 & l<<4; 
			} 
			else if (l > 0x3F) // 12
			{
				out.write((byte)(val | (0xF & l>>>8))); 
				out.write((byte)(0xFF & l)); 
				val = nibble = 0; 
			}  
			else if (l > 0xF)  // 8
			{ 
				out.write((byte)(val | (0xF & l>>>4))); 
				val = 0xF0 & l<<4; 
			} 
			else  // 4
			{ 
				out.write((byte)(val | (0xF & l))); 
				val = nibble = 0; 
			}  
		}
	}

	private void setScreenPosition(int minX, int minY, int maxX, int maxY)
	{
		// set planned pic pos. on tvscreen
		sections[9]  = (byte)(minX>>>4);
		sections[10] = (byte)(minX<<4 | maxX>>>8);
		sections[11] = (byte)maxX;
		sections[12] = (byte)(minY>>>4);
		sections[13] = (byte)(minY<<4 | maxY>>>8);
		sections[14] = (byte)maxY;
	}

	private void setControlBlockPosition(int control_block_pos, int bottom_field_start_pos)
	{
		// top_field
		sections[16] = 0;
		sections[17] = 4;

		// bottom_field
		sections[18] = (byte)(0xFF & bottom_field_start_pos>>>8);
		sections[19] = (byte)(0xFF & bottom_field_start_pos);

		// control_block
		sections[0] = sections[24] = (byte)(0xFF & control_block_pos>>>8);
		sections[1] = sections[25] = (byte)(0xFF & control_block_pos);
	}

	private void setPGCsection()
	{
		int pgc_values = setPGClinks();

		// color index 3,2 + 1,0
		sections[3] = (byte)(0xFF & pgc_values>>>8);
		sections[4] = (byte)(0xFF & pgc_values);

		// alpha index 3,2 + 1,0
		sections[6] = (byte)(0xFF & pgc_values>>>24);
		sections[7] = (byte)(0xFF & pgc_values>>>16);
	}

	public int setPGClinks()
	{
		Object pgc_color_links[] = bitmap.getColorIndices();
		Object pgc_alpha_links[] = getUserColorTableArray();
		int pgc_colors = 0xFE10;
		int pgc_alphas = 0xFFF9;
		int pgc_color_value, pgc_alpha_value;

		for (int a=0; a < 4; a++)
		{
			if (a < pgc_color_links.length)
			{
				pgc_color_value = 0xF & Integer.parseInt(pgc_color_links[a].toString());
				pgc_alpha_value = 0xF & Integer.parseInt(pgc_alpha_links[pgc_color_value].toString())>>>28;
				pgc_colors = (pgc_colors & ~(0xF<<(a * 4))) | pgc_color_value<<(a * 4);
				pgc_alphas = (pgc_alphas & ~(0xF<<(a * 4))) | pgc_alpha_value<<(a * 4);
			}
		}

		if (read_from_Image)
		//	pgc_alphas &= (0xFFF0 | default_alpha);
			pgc_alphas &= (0xFFF0 | modified_alpha);

		return (pgc_alphas<<16 | pgc_colors);
	}

	public void set2()
	{ 
		option[2] = option[7];
	}

	//DM26052004 081.7 int03 add
	public int getMaximumLines()
	{ 
		return option[8];
	}

	/*** set user data ("Font pointsize; Backgr. Alpha value; Yoffset; Xoffset; Screenwidth"); **/
	public int set(String nm, String values)
	{
		resetUserColorTable();

		System.arraycopy(standard_values, 0, option, 0, standard_values.length);
		StringTokenizer st = new StringTokenizer(values, ";");
		int a=0;

		while (st.hasMoreTokens() && a < option.length)
		{
			option[a] = Integer.parseInt(st.nextToken());
			a++;
		}

		line_offset = option[0] + 2;
		default_alpha = 0xF & option[1];
		font = new Font(nm, Font.BOLD, option[0]);
		font_alt = new Font(nm, Font.BOLD | Font.ITALIC, option[0]); //DM30122003 081.6 int10 add
		font_std = new Font("Tahoma", Font.PLAIN, 14); //DM01032004 081.6 int18 add
		return option[7];
	}

	private int[] getColorTable(int flag)
	{
		//define alternative color_table here
		if (flag == 0)
			return default_sup_colors;

		else
			return default_teletext_colors;
	}

	//DM05052004 081.7 int02 new
	private void setArea()
	{
		Rect[0] = bitmap.getX();
		Rect[1] = bitmap.getY();
		Rect[2] = bitmap.getWidth();
		Rect[3] = bitmap.getHeight();

		pos[0] = bitmap.getX();
		pos[1] = bitmap.getY();
		pos[2] = bitmap.getMaxX();
		pos[3] = bitmap.getMaxY();
	}

	//DM05052004 081.7 int02 new
	public String getArea()
	{
		String string = "";
		string += "x " + Rect[0];
		string += " y " + Rect[1];
		string += " w " + Rect[2];
		string += " h " + Rect[3];

		string += " x1 " + pos[0];
		string += " y1 " + pos[1];
		string += " x2 " + pos[2];
		string += " y2 " + pos[3];

		return string;
	}

	public void run()
	{}

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
		big.fillRect(0, 0, video_basics[0], video_basics[1]); //DM20042004 081.7 int02 changed

		return video_basics[1];
	}

	//DM28022004 081.6 int18 new
	private int Get_Bits(byte buf[], int BPos[], int N)
	{
		int Pos, Val;
		Pos = BPos[1]>>>3;

		if (Pos >= buf.length - 4)
		{
			global_error = true;
			BPos[1] += N;
			BPos[0] = BPos[1]>>>3;
			return 0;
		}

		Val =(0xFF & buf[Pos])<<24 |
			(0xFF & buf[Pos + 1])<<16 |
			(0xFF & buf[Pos + 2])<<8 |
			(0xFF & buf[Pos + 3]);

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

		if (Pos >= buf.length - 4)
		{
			global_error = true;
			return 0;
		}

		Val =(0xFF & buf[Pos])<<24 |
			(0xFF & buf[Pos + 1])<<16 |
			(0xFF & buf[Pos + 2])<<8 |
			(0xFF & buf[Pos + 3]);

		Val <<= BPos[1] & 7;
		Val >>>= 32 - N;

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

	//DM25072004 081.7 int07 add
	public String isForced_Msg()
	{
		String str = null;

		//change of status occured
		if ((isforced_status & 1) == 0)
		{
			if ((isforced_status & 2) > 0)
				str = Resource.getString("subpicture.msg.forced.no");

			else
				str = Resource.getString("subpicture.msg.forced.yes");
		}

		isforced_status |= 1;

		return str;
	}

	//DM25072004 081.7 int07 add
	public void reset()
	{
		isforced_status = 0;
	}

	//DM14052004 081.7 int02 add
	public int decode_picture(byte packet[], int off, boolean decode)
	{
		return decode_picture(packet, off, decode, 0, false, true);
	}

	//DM28022004 081.6 int18 new
	//DM05052004 081.7 int02 changed
	public int decode_picture(byte packet[], int off, boolean decode, long pts, boolean save, boolean visible)
	{
		read_from_Image = false;
		global_error = false;

		boolean simple_picture = false;
		int picture_length = packet.length;
		byte data[] = new byte[picture_length +4];
		System.arraycopy(packet, 0, data, 0, picture_length);

		int BPos[] = { off, off<<3 }; //BytePos, BitPos
		int position[] = new int[4];
		int start_pos[] = new int[3];
		int print_colors[] = new int[4];

		if (BPos[0] > picture_length-4)
			return -4;

		int packetlength = Get_Bits(data, BPos, 16); // required pack length

		//DM13042004 081.7 int01 add
		//DM28042004 081.7 int02 changed
		if (Show_Bits(data, BPos, 24) == 0xF) // DVB subpicture: 8bit padding 0x00 + 8bit subtitle_stream_id 0x00 + start of subtitle segment 0x0F
		{
			//DM15072004 081.7 int06 add
			big.setFont(font_std);

			int ret = dvb.decodeDVBSubpicture(data, BPos, big, bimg, pts, save, visible);

			if (ret > -2)
				repaint();

			return ret;
		}

		if (BPos[0] + packetlength != picture_length + 2)
			return -5;

		start_pos[2] = Get_Bits(data, BPos, 16) - 2;
		Flush_Bits(BPos, start_pos[2]<<3); // jump to sections chunk

		int playtime_pos = Get_Bits(data, BPos, 16);  //fixed pos, so it must follow the 1st ctrl sequ,

		if (playtime_pos == start_pos[2] + 2)
		{
			playtime_pos = packetlength;
			simple_picture = true;
			X.Msg(Resource.getString("subpicture.msg2"));
		}
		else
			start_pos[2] += off+2;

		int color_table[] = getColorTable(0);

		//DM26052004 081.7 int03 changed
		while (BPos[0] < off + playtime_pos)  // read sections chunk
		{
			int cmd_switch = Get_Bits(data, BPos, 8);
			switch(cmd_switch)
			{
			case 0: // force display
				//DM25072004 081.7 int07 changed
				isforced_status = (isforced_status & 5) != 5 ? 4 : 5;
				break;
			case 1: // start display
				//DM25072004 081.7 int07 changed
				isforced_status = (isforced_status & 3) != 3 ? 2 : 3;
				break;
			case 2: // stop display
			case 0xFF: // end of ctrl sequ.
				break;
			case 3: // 4 color links
				for (int b=0; b<4; b++)
					print_colors[3 - b] |= (color_table[Get_Bits(data, BPos, 4)] & 0xFFFFFF);
				break;
			case 4: // alpha blending
				for (int b=0; b<4; b++)
					print_colors[3 - b] |= (0x11 * (0xF ^ Get_Bits(data, BPos, 4)))<<24;
				break;
			case 5: // x,y pos.
				for (int b=0; b<4; b++)
					position[b] = Get_Bits(data, BPos, 12);
				break;
			case 6: // pos. of decode_start of a field
				for (int b=0; b<2; b++)
					start_pos[b] = Get_Bits(data, BPos, 16);
				break;
			default:
				X.Msg(Resource.getString("subpicture.msg3") + ": " + cmd_switch);
			}
		}

		if (off + playtime_pos != BPos[0])
			return -6;

		int playtime = 0;
		if (!simple_picture)
		{
			playtime = Get_Bits(data, BPos, 16);
			if (playtime_pos != Get_Bits(data, BPos, 16))
				return -7;

			if (Get_Bits(data, BPos, 8) != 2)
				return -8;

			Flush_Bits( BPos, ((BPos[0] & 1) != 1) ? 16 : 8 );
		}

		if (BPos[0] != picture_length)
			return -9;

		if (global_error)
			return -3;

		if (!decode)
			return (playtime * 1000); //DM26052004 081.7 int03 changed , 900


		for (int b=0; b<2; b++)
			start_pos[b] += off;

		paintVideoSize();

		int y0 = position[2];

		int width = position[1] - position[0] + 1;
		int height = position[3] - position[2] + 1;

		big.setColor(Color.white);
		big.drawRect(position[0] - 1, y0 - 1, width + 1, height + 1);
		big.setFont(font_std);
		big.drawString("x" + position[0] + ", y" + position[2] + " / " + width + "*" + height, position[0] - 1, y0 - 5);

		for (int b=0; b<2; b++)
		{
			int Val=0, x1 = position[0], y1 = y0 + b;
			BPos[1] = (BPos[0] = start_pos[b])<<3; // top_field at first

			while (BPos[0] < start_pos[b+1]) // stop at pos_marker
			{
				if ((Val = Get_Bits(data, BPos, 4)) > 3) //4..F (0..3 never encodable)
				{
					big.setColor(new Color(print_colors[Val & 3]));
					big.drawLine(x1, y1, (x1 += Val>>>2), y1);
				}
				else if ((Val = Val<<4 | Get_Bits(data, BPos, 4)) > 0xF) //10..3F
				{
					big.setColor(new Color(print_colors[Val & 3]));
					big.drawLine(x1, y1, (x1 += Val>>>2), y1);
				}
				else if ((Val = Val<<4 | Get_Bits(data, BPos, 4)) > 0x3F) //40..FF
				{
					big.setColor(new Color(print_colors[Val & 3]));
					big.drawLine(x1, y1, (x1 += Val>>>2), y1);
				}
				else if ((Val = Val<<4 | Get_Bits(data, BPos, 4)) > 0) //100..3FF
				{
					big.setColor(new Color(print_colors[Val & 3]));
					big.drawLine(x1, y1, (x1 += Val>>>2), y1);
				}
				else  // 0 forced carriage return
				{
					x1 = position[0];
					y1 += 2;
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

		if (global_error)
			return -3;

		return (playtime * 1000); //DM26052004 081.7 int03 changed, 900
	}


} // end inner class
} // end class
