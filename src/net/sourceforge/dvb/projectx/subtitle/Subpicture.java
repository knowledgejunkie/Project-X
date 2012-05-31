/*
 * @(#)SUBPICTURE.java - creates SUP file to use as DVD subtitles
 *
 * Copyright (c) 2003-2011 by dvb.matt, All Rights Reserved.
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

/*
 * thanx to Samuel Hocevar for his very helpful annotations of DVD subtitle RLE stuff
 * http://sam.zoy.org/writings/dvd/subtitles/
 */
/*
 * multicolor subtitling patch (UK Freeview) by Duncan (Shannock9) UK
 * 2008-12
 */


package net.sourceforge.dvb.projectx.subtitle;

//
import java.awt.Image;

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
import java.util.HashMap;
import java.util.Arrays;
import java.util.StringTokenizer;

import net.sourceforge.dvb.projectx.common.Resource;
import net.sourceforge.dvb.projectx.common.Keys;
import net.sourceforge.dvb.projectx.common.Common;

import net.sourceforge.dvb.projectx.parser.CommonParsing;


public class Subpicture extends Object {

	private int w = Common.getSettings().getBooleanProperty(Keys.KEY_SubtitlePanel_enableHDSub) ? 1920 : 720;
	private int h = Common.getSettings().getBooleanProperty(Keys.KEY_SubtitlePanel_enableHDSub) ? 1088 : 576;
	private int x = 20;

	private int default_alpha = 10;
	private int modified_alpha = 0;

	private BufferedImage bimg;
	private Graphics2D big;
	private Font font, font_std;
	private FontRenderContext frc;

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

	private final int basic_teletext_colors[] = {
		//bg & ft = 0..7
		0xFF101010, //black   0
		0xFFEB1010, //red     1
		0xFF10EB10, //green   2
		0xFFEBEB10, //yellow  3
		0xFF1010EB, //blue    4
		0xFFEB10EB, //magenta 5
		0xFF10EBEB, //cyan    6
		0xFFEBEBEB, //white   7
		0xFF801010, //red2    8
		0xFF108010, //green2  9
		0xFF808010, //yellow2 A
		0xFF101080, //blue2   B
		0xFF801080, //magenta2C
		0xFF108080, //cyan2   D
		0xFF808080, //white2  E
		0x80 // backgr blue,  F sign for full transparency
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

	private int[] alternative_sup_colors = new int[17];

	private Object[] textrows = new Object[0];

	private byte[] RLEheader = { 0x53,0x50,0,0,0,0,0,0,0,0,0,0,0,0 }; // startcode + later reverse 5PTS, DTS=0

	/**	control block defs, after picture
		0, 0,           // next contr sequ.
		1,  // start displ.  //0 means force display
		3, 0x32, 0x10,         // color palette linkage
		4, (byte)0xFF, (byte)0xFA,         // color alpha channel linkage F=opaque
		5, 0, 0, 0, 0, 0, 0, // coordinates Xa,Ya,Xe,Ye
		6, 0, 0, 0, 0,     // bytepos start top_field, start bottom_field
		(byte)0xFF,    // end of sequ.
		1, 0x50,  // time for next sequ,
		0, 0,  //next contr sequ.
		2,   // stop displ.
		(byte)0xFF     // end of sequ: timedur in pts/1100, size s.a. , add 0xFF if size is not WORD aligned
	**/

	private ByteArrayOutputStream out = new ByteArrayOutputStream();

	private byte[] newline = { 0,0 };

	private int[] Rect = new int[4];
	private int[] pos = new int[4];
	private int[] option = new int[11];
	private int[] standard_values = { 26, 10, 32, 80, 560, 720, 576, -1, 4, 3, 1 };
	private int isforced_status = 0;
	private int ismulticolor_status = 0;
	private int textrow_height = 28;

	private ArrayList user_color_table = new ArrayList();
	private ArrayList ccb_list = new ArrayList();

	private Bitmap bitmap;

	private boolean ReadFromImage = false;
	private boolean GlobalError = false;

	private int X_Offset = 0;
	private int Y_Offset = 0;
	private int DisplayMode = 0;


	public DVBSubpicture dvb = new DVBSubpicture();

	public ColorAreas s9CA = new ColorAreas();                                                   //S9

	/**
	 *
	 */
	public Subpicture()
	{ 
		bimg = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		big = bimg.createGraphics();

		set("Tahoma", ("26;10;32;80;560;720;576;-1;4;2;1"));
		frc = big.getFontRenderContext();

		//   big.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
	}

	/**
	 *
	 */
	public void repaint()
	{
		if (big == null) 
			return;

		Common.getGuiInterface().repaintSubpicture();
	}

	/**
	 * 
	 */
	public Image getImage()
	{
		return bimg;
	}

	/**
	 * 
	 */
	public Image getScaledImage(int scaled_w, int scaled_h)
	{
		return bimg.getScaledInstance(scaled_w, scaled_h, Image.SCALE_FAST);
	}

	/**
	 * set display time
	 */
	public int setTime(long in_time, long out_time)
	{
		int difference = (int)(1L + ((out_time - in_time) / 1024));

		Common.getGuiInterface().setSubpictureTitle(" / " + Resource.getString("subpicture.in_time") + ": " + Common.formatTime_1(in_time / 90) + " " + Resource.getString("subpicture.duration") + ": " + Common.formatTime_1((out_time - in_time) / 90) );

	//	if (debug)
	//		System.out.println("in " + in_time + "/out " + out_time + "/d1 " + (out_time - in_time) + "/90 " + ((out_time - in_time)/90) + "/d2 " + difference);

		return difference;
	}

	/**
	 * paint any picture, for export it should only have 2bit depth
	 */
	public void paintPicture(byte[] array, int _width, int _height, int _scansize, int _x, int _y)
	{
		big.setColor(Color.gray);
		big.fillRect(0, 0, w, h); 
	//	bimg.setRGB(_x, _y, _width, _height, array, 0, _scansize);
          
		repaint();
	}

	/**
	 * create pic from ttx 
	 */
	public void createPictureFromTeletext(Object[] _textrows, Object obj)
	{
		textrows = _textrows;
		paintPictureFromTeletext(obj);
		repaint();
	}

	/**
	 * paint Image from ttx 
	 */
	private void paintPictureFromTeletext(Object obj)
	{
		int space = 4; // space at top & bottom

		big.setFont(font);

		int font_height = (int)font.getMaxCharBounds(frc).getHeight();
		//determine top and bottom string overhead
		int textrow_shift = 2 + font_height - textrow_height; 

		//top left x of rectangle
		Rect[0] = option[3];

		//max width of rectangle for export
		Rect[2] = option[4];  

		//max height of rectangle for export
		Rect[3] = (2 * space) + (font_height * textrows.length);

		//top left y of rectangle -> frame_height - bottom_offset - rectangle height
		Rect[1] = option[6] - option[2] - Rect[3]; 

		//all x,y of rectangle outside
		pos[0] = Rect[0];
		pos[1] = Rect[1];
		pos[2] = Rect[0] + Rect[2] - 1;
		pos[3] = Rect[1] + Rect[3] - 1;

		paintVideoSize(obj);
		paintPictureRectangle(Rect, pos);

		big.setFont(font); // re-activate font

		int color_table[] = getColorTable(1);

		ArrayList colors_list = new ArrayList();
		boolean antialiasing;

		// pre-check, whether we have not more than supported 'speaker-colors'
		for (int i = 0; i < textrows.length; i++)
		{
			int[] chars = (int[]) textrows[i];

			for (int j = 0, bgcolor_index, paint_color; j < chars.length; j++)
			{
				//source background color
				bgcolor_index = (7 & chars[j]>>>4)<<3;

				//source modified foreground color
				paint_color = color_table[bgcolor_index + (7 & chars[j])];
				String nstr = Integer.toString(paint_color);
				String sign = new Character((char)(chars[j]>>>8)).toString();

				//remember new color
				if (colors_list.indexOf(nstr) < 0 && !sign.equals(" "))
					colors_list.add(nstr);
			}
		}

		// define background; if less than supported number of front-colors, 
		// use 'simple' antialiasing with full transparency
	//	if (list.size() < 3 && Common.getSettings().getBooleanProperty(Keys.KEY_SubtitlePanel_useTextOutline))
		if (colors_list.size() < 15 && Common.getSettings().getBooleanProperty(Keys.KEY_SubtitlePanel_useTextOutline))
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

		// paint entire background, either black or transparent color
		big.fillRect(Rect[0], Rect[1], Rect[2], Rect[3]); 

		// paint outline background of char
		for (int i = 0, y; antialiasing && i < textrows.length; i++)
		{
			int[] chars = (int[]) textrows[i];
			String nstr = "";

			big.setColor(new Color(color_table[64])); // black

			// concatenate string, no special colors required
			for (int j = 0; j < chars.length; j++)
				nstr += new Character((char)(chars[j]>>>8)).toString();

			x = option[3];
			y = Rect[1] + space + (font_height * (1 + i));

			int[] offs = new int[(option[9] * 2) + 1];

			// overhead around a pixel: x pix WEST, 1 pix MID, x pix EAST
			// pix > 4 not recommended, option[9] = outline_pixels
			offs[0] = offs[offs.length - 1] = option[9] - 1;
			Arrays.fill(offs, 1, offs.length - 1, option[9]);

			for (int j = 0, _x, _y; j < offs.length; j++) // horiz. lines
			{
				_x = x;
				_y = y - (offs.length / 2) + j;

				for (int k = -offs[j]; k < offs[j] + 1; k++)
					big.drawString(nstr, _x + k, _y - textrow_shift);
			}
		}


		ccb_list.clear();
		ChangeColorControlBlock ccb = new ChangeColorControlBlock(); //init as base

		// paint all chars front colors
		for (int i = 0, y; i < textrows.length; i++)
		{
			int[] chars = (int[]) textrows[i];
			int ftcolor_index = -1;
			int last_ftcolor_index = -1;
			String str;
			x = option[3];
			y = Rect[1] + space + (font_height * (1 + i));

			for (int j = 0, bgcolor_index; j < chars.length; j++)
			{
				//source background color
				bgcolor_index = (7 & chars[j]>>>4)<<3;
				ftcolor_index = color_table[bgcolor_index + (7 & chars[j])];
				str = String.valueOf((char)(chars[j]>>>8));

				//initiate new block def, exclude space chars
				if (ftcolor_index != last_ftcolor_index)
				{
					ccb = new ChangeColorControlBlock();
					ccb.bottom_line = y; //+5
					ccb.top_line = ccb.bottom_line - font_height;//+3
					ccb.left_column = x;
					ccb.right_column = ccb.left_column + (int)font.getStringBounds(str, frc).getWidth();
					ccb.color_index = ftcolor_index;
					ccb.ftcolor = 7 & chars[j];
					ccb.bgcolor = (7 & chars[j])>>4;
					ccb.string = str;
					ccb.row = i;
					ccb_list.add(ccb);
				}
				else //move end of column to the right at same color of next char
				{
					ccb.right_column = x + (int)font.getStringBounds(str, frc).getWidth();
					ccb.string += str;
				}

				last_ftcolor_index = ftcolor_index;

				//source foreground color
				big.setColor(new Color(ftcolor_index));
				big.drawString(str, x, y - textrow_shift);

				x += font.getStringBounds(str, frc).getWidth();
			}
		}

		//remove empty strings area definitions
		for (int i = 0; i < ccb_list.size(); )
		{
			ccb = (ChangeColorControlBlock) ccb_list.get(i);

			if (ccb.string.trim().length() == 0)
				ccb_list.remove(i);
			else
				i++;
		}
	}


	class ChangeColorControlBlock {

		public int top_line = 0;
		public int bottom_line = 0;
		public int left_column = 0;
		public int right_column = 0;
		public int color_index = 0;
		public int ftcolor = 0;
		public int bgcolor = 0;
		public String string = "";
		public int row = 0;

	}


	private int paintVideoSize(Object obj)
	{
		String[] str = (String[]) obj;
		int video_horizontal = w;
		int video_vertical = h;

		// H
		video_horizontal = str[0] == null ? video_horizontal : Integer.parseInt(str[0]);

		// V
		video_vertical = str[1] == null ? video_vertical : Integer.parseInt(str[1]);

		//deep red background to verify picture rectangle with given video resolution
		big.setColor(new Color(0xFF550000));
		big.fillRect(0, 0, w, h);

		//picture area which the subpicture must not exceed, have to adjust to the hor. middle of it
		big.setColor(Color.gray);
		big.fillRect(0, 0, video_horizontal, video_vertical); 

		return video_vertical;
	}

	private void paintPictureRectangle(int[] Rect, int[] pos)
	{
		big.setColor(Color.white);
		big.drawRect(Rect[0] - 1, Rect[1] - 1, Rect[2] + 1, Rect[3] + 1);
		big.setFont(font_std);
		big.drawString("x" + pos[0] + ", y" + pos[1] + " / " + (pos[2] - pos[0] + 1) + "*" + (pos[3] - pos[1] + 1), Rect[0] - 1, Rect[1] - 5);
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

		for (int i = 0; i < pixel.length; i++)
		{
			String pixel_str = String.valueOf(pixel[i]);

			if (!user_color_table.contains(pixel_str))
				user_color_table.add(pixel_str);

			bitmap.getColorIndex(getUserColorTableIndex(pixel[i]));
		}
	}

	private void updateUserColorTable(int pixel[])
	{
		for (int i = 0; i < pixel.length; i++)
		{
			String pixel_str = String.valueOf(pixel[i]);

			if (user_color_table.contains(pixel_str))
				continue;

			user_color_table.add(pixel_str);
		}
	}

	private int getUserColorTableIndex(int color_index)
	{
		int value;

		if ((value = user_color_table.indexOf(String.valueOf(color_index))) < 0)
			return 0;

		return value;
	}

	public byte[] writeRLE(long start_time, int onscreen_time) throws IOException
	{
		ReadFromImage = true; // use user defined alpha value for color index 0

		//ceates bitmap from painted preview image
		bitmap = new Bitmap( Rect[0], Rect[1], Rect[2], Rect[3], bimg.getRGB(Rect[0], Rect[1], Rect[2], Rect[3], null, 0, Rect[2]), 2, 0, 1, 2, start_time, onscreen_time);

		return createRLEPicture();
	}

	public byte[] writeRLE(Bitmap new_bitmap) throws IOException
	{
		//takes given bitmap 
		bitmap = new_bitmap;

		setArea();

		return createRLEPicture();
	}

	private byte[] createRLEPicture()
	{
		byte picture_packet[] = null;

		try {

			if (ColorAreas.active)  			               //multicolor DVB to SUP active        //S9
				resetUserColorTable();                         //so retention counterproductive      //S9

			updateUserColorTable(bitmap.getPixel());           //base code re-ordered                //S9

			if (ColorAreas.active)  			               //multicolor DVB to SUP active        //S9
				s9CA.analyse(bitmap, getUserColorTable());     //which can correct noisy pixels!!    //S9

			int pixels[] = bitmap.getPixel();                  //base code re-ordered                //S9

			out.reset(); 
			out.write(RLEheader);   //start picture in .sup form

			int bottom_field_start_pos = 0;
			int pgc_color = 0;                                 //holds bp12 for a run of pixels      //S9
			int[] nibble_value = new int[2];

			// read out interlaced RGB
			for (int i = 0, l = 0, a = 0, b = 0, color_index = 0; i < 2; i++)
			{
				// top_field first
				for (l = 0, color_index = 0, a = i * bitmap.getWidth(); a < pixels.length; a += (2 * bitmap.getWidth()))
				{
					for (l = 0, color_index = 0, b = 0; b < bitmap.getWidth(); b++, l++)
					{
						if (ColorAreas.active)                 //multicolor DVB to SUP active        //S9
							pgc_color = s9CA.bp12[a + b];      //bp12 was filled during analyse()    //S9
						else                                   //pre-existing approach but fewer runs//S9
						{
							pgc_color = pixels[a + b];
					//		pgc_color = bitmap.getColorIndex(getUserColorTableIndex(color_index));   //S9
					//		pgc_color = bitmap.getColorIndex(pgc_color);
						}

						if (s9CA.active && s9CA.dbgline() == a / bitmap.getWidth())   //see every pixel on specific dbgline //S9dbg
						{  
							int q = s9CA.getQuant(getUserColorTableIndex(pixels[a + b]));    //quant as Integer  //S9dbg

							System.out.println("dbg line " + s9CA.d(3, a / bitmap.getWidth()) + "/x " + s9CA.d(3, b)       //S9dbg
							+ "/pixel " + s9CA.X(8, pixels[a + b]) + "/clr_ndx " + s9CA.X(2, color_index)                 //S9dbg
							+ "/pgc_clr " + s9CA.X(2, pgc_color) + "/Q " + s9CA.X(8, q) + "/run " + s9CA.d(3, l));
						}           //S9dbg

						if (pgc_color != color_index)                                                //S9
						{
							// write last RLE nibbles, while color change
							updateRLE(l, color_index, nibble_value);
							color_index = pgc_color;                                                 //S9
							l = 0;
						}
						else if ( l > 254 )
						{
							// write last RLE nibbles, cannot incl. more than 255 pixels
							updateRLE(l, color_index, nibble_value);
							l = 0;
						}
						// std: adds l-bit to active color
					}

					// when last pixel is length = 1 then it will be 0 and missing ?
					// see CR 
					//l -= 1; 

					while ( l > 255 )  // never used ?!
					{ 
						updateRLE(255, color_index, nibble_value); 
						l -= 255; 
					}

					updateRLE(l, color_index, nibble_value);   // write last RLE nibbles, line end
					alignRLE(nibble_value);

					if (b < bitmap.getWidth()) //fix, add CR only when less pixel have been painted 
						out.write(newline);  // new line CR, byte aligned
				}

				alignRLE(nibble_value);

				if (bottom_field_start_pos == 0) 
					bottom_field_start_pos = out.size() - 10;        // save startpos of bottom_field (size-14)
			}

			if (ColorAreas.active)                                     //++multicolor DVB to SUP active++    //S9
			{                                                                                                //S9
					//commands() must encapsulate command buffer design - do not patch command buffer from   //S9
					//here.  All delay timing fields will be set by commands() from information in bitmap    //S9

				int command_start_pos = out.size() - 10;        //offset in subpic >including 1st delay< //S9

				out.write(s9CA.commands(command_start_pos, bottom_field_start_pos, bitmap));//cmd buffer //S9

				if ((out.size() & 1) == 1)
					out.write((byte)0xFF);

				out.flush();

				//Fixups to integrate command buffer into subpic are legitimate part of this method      //S9
				//Note "0xFF &" is redundant - the (byte) cast does that anyway.   Mask only needed      //S9
				//for moving byte to int (implicitly or explicitly) to overcome any sign extension.      //S9

				picture_packet = out.toByteArray();

				int size = picture_packet.length - 10;

				picture_packet[10] = (byte)(0xFF & size>>>8);
				picture_packet[11] = (byte)(0xFF & size);
				picture_packet[12] = (byte)(0xFF & command_start_pos>>>8);                               //S9
				picture_packet[13] = (byte)(0xFF & command_start_pos);                                   //S9

				for (int a = 0; a < 4; a++)
					picture_packet[a + 2] = (byte)(0xFF & bitmap.getInTime()>>>(a * 8));
			}                                                                                                //S9
			else                                                   //++original code pre multicolor DVB++    //S9
			{                                                                                                //S9
				//RLE picture data completed
				int coded_picture_length = out.size() - 10; //minus 10 bytes RLE header in buffer

				byte[][] section_value = new byte[11][];

				//section_value[0] see later
				section_value[1] = setStartDisplay(); //1, xx start, not forced
				section_value[2] = setPGCsection(); //3, color + 4, alpha
				section_value[3] = setScreenPosition(bitmap.getX(), bitmap.getY(), bitmap.getMaxX() - 1, bitmap.getMaxY() - 1); //5, xxx
				section_value[4] = setFieldStartPosition(bottom_field_start_pos); //6, xxxx
				section_value[5] = setChangeColorAlpha(section_value[2]); //7, xxxx , may be empty
				section_value[6] = setEndOfSequence(); //EOS
				section_value[7] = setDisplayTime(bitmap.getPlayTime()); // xx, delay time for next control sequence
				//section_value[8] see later
				section_value[9] = setStopDisplay(); //2
				section_value[10] = setEndOfSequence(); //EOS

				int control_block_pos = coded_picture_length + 4; //incl. 4byte for 2x length info

				for (int s = 1; s < 7; s++)
					control_block_pos += section_value[s].length;

				section_value[0] = setControlBlockPosition(control_block_pos); //0+1,  24+25 (alt), 2mal verwenden
				section_value[8] = section_value[0]; //repeat control_block_pos

				//0, 0 = delaytime for 1st command
				out.write(newline);

				//write control_blocks
				for (int s = 0; s < section_value.length; s++)
					out.write(section_value[s]);

				//align to word
				if ((out.size() & 1) == 1)
					out.write((byte)0xFF);

				out.flush();

				picture_packet = out.toByteArray();

				int picture_packet_length = picture_packet.length - 10;

				picture_packet[10] = (byte)(picture_packet_length>>>8);
				picture_packet[11] = (byte) picture_packet_length;
				picture_packet[12] = (byte)(coded_picture_length>>>8);
				picture_packet[13] = (byte) coded_picture_length;

				for (int a = 0; a < 4; a++) //set pts 
					picture_packet[a + 2] = (byte)(0xFF & bitmap.getInTime()>>>(a * 8));

			}                                                //++endif original code pre multicolor DVB++    //S9

			if (s9CA.dbgSub(1))
				s9CA.dumpHdrAndCmd(picture_packet);         //so what was generated?         //S9dbg

		} catch (IOException e) { 

			Common.setExceptionMessage(e);
		}

		ReadFromImage = false;
		ccb_list.clear();

		if (picture_packet.length >= 0xFFFF)
			Common.setMessage("!> error: subpicture exceeds 65k size limit");

		return picture_packet;
	}

	// write last nibble, if it was not aligned
	private void alignRLE(int[] nibble_value)
	{
		if (nibble_value[0] == 0) 
			return;

		else
		{ 
			out.write((byte)nibble_value[1]); 
			nibble_value[1] = nibble_value[0] = 0; 
		}
	}

//	private void updateRLE(int l, int pgc_color)                                                 //S9
	private void updateRLE(int l, int color_index, int[] nibble_value)                                                 //S9
	{
		if (l < 1)
			return;

		//pgc_color shall not exceed value 3!                                                    //S9
		int pgc_color = getUserColorTableIndex(color_index);

		//look-up of pgc_color refactored to caller <= it's the RESOLVED 2 bits we want runs of  //S9
		if (ColorAreas.active)
			pgc_color = color_index;
		else
			pgc_color = bitmap.getColorIndex(pgc_color);

		l = l<<2 | pgc_color;  // combine bits + color_index

		// new byte begin
		if (nibble_value[0] == 0)
		{  
			if (l > 0xFF) // 16
			{ 
				out.write((byte)(0xFF & l>>>8)); 
				out.write((byte)(0xFF & l)); 
			} 
			else if (l > 0x3F)  // 12
			{ 
				out.write((byte)(0xFF & l>>>4)); 
				nibble_value[1] = 0xF0 & l<<4; 
				nibble_value[0] = 4; 
			}
			else if (l > 0xF)  // 8
				out.write((byte)(0xFF & l)); 

			else   // 4
			{ 
				nibble_value[1] = 0xF0 & l<<4; 
				nibble_value[0] = 4; 
			}

			return;
		}

		 // middle of byte
		if (l > 0xFF) // 16
		{ 
			out.write((byte)(nibble_value[1] | (0xF & l>>>12))); 
			out.write((byte)(0xFF & l>>>4)); 
			nibble_value[1] = 0xF0 & l<<4; 
		} 
		else if (l > 0x3F) // 12
		{
			out.write((byte)(nibble_value[1] | (0xF & l>>>8))); 
			out.write((byte)(0xFF & l)); 
			nibble_value[1] = nibble_value[0] = 0; 
		}  
		else if (l > 0xF)  // 8
		{ 
			out.write((byte)(nibble_value[1] | (0xF & l>>>4))); 
			nibble_value[1] = 0xF0 & l<<4; 
		} 
		else  // 4
		{ 
			out.write((byte)(nibble_value[1] | (0xF & l))); 
			nibble_value[1] = nibble_value[0] = 0; 
		}  
	}

	// control_block
	private byte[] setControlBlockPosition(int control_block_pos)
	{
		return new byte[] { (byte)(control_block_pos>>>8), (byte)control_block_pos };
	}

	// color index 3,2 + 1,0
	// alpha index 3,2 + 1,0
	private byte[] setPGCsection()
	{
		int pgc_values = setPGClinks();

		return new byte[] { 3,  (byte)(pgc_values>>>8), (byte)pgc_values, 4, (byte)(pgc_values>>>24), (byte)(pgc_values>>>16) };
	}

	public int setPGClinks()
	{
		Object pgc_color_links[] = bitmap.getColorIndices();
		Object pgc_alpha_links[] = getUserColorTableArray();
		int pgc_colors = 0xFE10;
		int pgc_alphas = 0xFFF9;
		int pgc_color_value;
		int pgc_alpha_value;

		for (int i = 0; i < 4; i++)
		{
			if (i < pgc_color_links.length)
			{
				pgc_color_value = 0xF & Integer.parseInt(pgc_color_links[i].toString());
				pgc_alpha_value = 0xF & Integer.parseInt(pgc_alpha_links[pgc_color_value].toString())>>>28;
				pgc_colors = (pgc_colors & ~(0xF<<(i * 4))) | pgc_color_value<<(i * 4);
				pgc_alphas = (pgc_alphas & ~(0xF<<(i * 4))) | pgc_alpha_value<<(i * 4);
			}
		}

		if (ReadFromImage)
		//	pgc_alphas &= (0xFFF0 | default_alpha);
			pgc_alphas &= (0xFFF0 | modified_alpha);

		return (pgc_alphas<<16 | pgc_colors);
	}

	// set planned pic pos. on tvscreen
	private byte[] setScreenPosition(int minX, int minY, int maxX, int maxY)
	{
		return new byte[] { 5, (byte)(minX>>>4), (byte)(minX<<4 | maxX>>>8), (byte)maxX, (byte)(minY>>>4), (byte)(minY<<4 | maxY>>>8), (byte)maxY };
	}

	// top_field
	// bottom_field
	private byte[] setFieldStartPosition(int bottom_field_start_pos)
	{
		return new byte[] { 6, 0, 4, (byte)(bottom_field_start_pos>>>8), (byte)bottom_field_start_pos };
	}

	private byte[] setChangeColorAlpha(byte[] PGCcontrol)
	{
		ChangeColorControlBlock ccb;
		HashMap ccb_color = new HashMap();

		//remove area definitions when less than 3 front colors (means no enhancement necessary => fallback)
		for (int i = 0; i < ccb_list.size(); )
		{
			ccb = (ChangeColorControlBlock) ccb_list.get(i);

			if (!ccb_color.containsKey(String.valueOf(ccb.color_index)))
				ccb_color.put(String.valueOf(ccb.color_index), "0");

			if (ccb_color.size() < 3)
				ccb_list.remove(i);
			else
				i++;
		}

		// no change to apply
		if (ccb_list.size() == 0)
			return new byte[0];

		byte[] control = { 7, 0, 0 }; //control, size word
		byte[] row_header = { 0, 0, 0, 0 }; //0 + topline + count (max 15) + bottomline
		byte[] data_block = { 0, 0, 0, 0, 0, 0 }; //left column 0000, color 3210, transp 3210
		byte[] end_block = { 0x0F, (byte)0xFF, (byte)0xFF, (byte)0xFF };

		ArrayList cnt_list = new ArrayList();

		ByteArrayOutputStream block = new ByteArrayOutputStream();

		try {

			block.write(control);

			int cnt = 0;

			for (int i = 0, last_row = -1, ftcolor; i < ccb_list.size(); i++)
			{
				ccb = (ChangeColorControlBlock) ccb_list.get(i);
				
				if (cnt > 0 && ccb.row != last_row)
				{
					cnt_list.add(new Integer(cnt));
					cnt = 0;
				}

				if (cnt == 0)
				{
					row_header[0] = (byte)(ccb.top_line>>>8);
					row_header[1] = (byte) ccb.top_line;
					row_header[2] = (byte)(ccb.bottom_line>>>8);
					row_header[3] = (byte) ccb.bottom_line;
					block.write(row_header);
				}

				last_row = ccb.row;

				ftcolor = getUserColorTableIndex(ccb.color_index); //must be < 15

				if (cnt > 15) //max 15 parameters per row
					continue;

				data_block[0] = (byte)(ccb.left_column>>>8);
				data_block[1] = (byte) ccb.left_column;
				data_block[2] = (byte)(ftcolor | ftcolor<<4); //color 3,2
//				data_block[2] = PGCcontrol[1]; //color 3,2
				data_block[3] = PGCcontrol[2]; //color 1,0
				data_block[4] = PGCcontrol[4]; //alpha 3,2 FF
				data_block[5] = PGCcontrol[5]; //alpha 1,0 F0
				block.write(data_block);

				cnt++;
			}

			//last row numbers
			if (cnt > 0)
				cnt_list.add(new Integer(cnt));

			block.write(end_block);
			block.flush();

		} catch (Exception e) {
			Common.setExceptionMessage(e);
		}

		byte[] controlblock = block.toByteArray();

		//add numbers in control field
		for (int i = 0, j = 5; i < cnt_list.size(); i++)
		{
			int cnt = ((Integer) cnt_list.get(i)).intValue();
			controlblock[j] = (byte)((cnt<<4) | (0xF & controlblock[j]));
			j += 4 + (cnt * 6);
		}

		//set length
		int length = controlblock.length - 1;

		controlblock[1] = (byte)(length>>>8);
		controlblock[2] = (byte)(length);

		return controlblock;
	}

	private byte[] setDisplayTime(int display_time)
	{
		return new byte[] { (byte)(display_time>>>8), (byte)display_time }; //(display-) time to next control sequence
	}

	private byte[] setStartDisplay()
	{
		return new byte[] { 1 }; //start, not forced
	}

	private byte[] setStopDisplay()
	{
		return new byte[] { 2 }; //stop
	}

	private byte[] setEndOfSequence()
	{
		return new byte[] { (byte)0xFF }; //end of sequence
	}


	public int getMaximumLines()
	{ 
		return option[8];
	}

	public void set2()
	{ 
		option[2] = option[7];
	}

	/***
	 * set user packet ("Font pointsize; Backgr. Alpha value; Yoffset; Xoffset; Screenwidth"); 
	 */
	public int[] set(String nm, String values)
	{
		return set(nm, values, false);
	}

	/*** 
	 * set user packet ("Font pointsize; Backgr. Alpha value; Yoffset; Xoffset; Screenwidth"); 
	 */
	public int[] set(String nm, String values, boolean keepColourTable)
	{
		if (!keepColourTable)
			resetUserColorTable();

		System.arraycopy(standard_values, 0, option, 0, standard_values.length);

		StringTokenizer st = new StringTokenizer(values, ";");
		int a = 0;

		while (st.hasMoreTokens() && a < option.length)
			option[a++] = Integer.parseInt(st.nextToken());

		textrow_height = option[0];
		default_alpha = 0xF & option[1];

		font = new Font(nm, option[10] == 0 ? Font.PLAIN : Font.BOLD, option[0]);
		font_std = new Font("Tahoma", Font.PLAIN, 14); //DM01032004 081.6 int18 add

		int[] ret_val = { option[2], option[7] };

		return ret_val;
	}

	/**
	 * define alternative color_table 
	 */
	public int[] getColorTable(int flag)
	{
		if (flag == 1)
			return default_teletext_colors;

		if (flag == 2)
			return basic_teletext_colors;

		if (alternative_sup_colors[0] == 0)
			return default_sup_colors;

		return alternative_sup_colors;
	}

	/**
	 *
	 */
	public void setColorTable(int[] values)
	{
		//define alternative color_table here
		if (values == null)
			Arrays.fill(alternative_sup_colors, 0);

		else
			System.arraycopy(values, 0, alternative_sup_colors, 0, values.length);
	}

	/**
	 *
	 */
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

	/**
	 *
	 */
	public String getArea()
	{
		String area = "";
		area += "x " + Rect[0];
		area += " y " + Rect[1];
		area += " w " + Rect[2];
		area += " h " + Rect[3];

		area += " x1 " + pos[0];
		area += " y1 " + pos[1];
		area += " x2 " + pos[2];
		area += " y2 " + pos[3];

		return area;
	}

	/**
	 *
	 */
	private void Set_Bits(byte buf[], int BPos[], int N, int Val)
	{
		int Pos = BPos[1]>>>3;
		int BitOffs = BPos[1] & 7;

		if (Pos >= buf.length || BitOffs + N >= (BPos[1] & ~7) + 32)
		{
			GlobalError = true;
		}

		else
		{
			int NoOfBytes = 1 + ((BitOffs + N - 1)>>>3);
			int NoOfBits = NoOfBytes<<3;

			int tmp_value = CommonParsing.getIntValue(buf, Pos, NoOfBytes, !CommonParsing.BYTEREORDERING);

			int mask = (-1)>>>(32 - N);
			int k = NoOfBits - N - BitOffs;

			mask <<= k;
			Val <<= k;

			tmp_value &= ~mask;
			tmp_value |= (Val & mask);

			CommonParsing.setValue(buf, Pos, NoOfBytes, !CommonParsing.BYTEREORDERING, tmp_value);
		}

		BPos[1] += N;
		BPos[0] = BPos[1]>>>3;
	}

	/**
	 *
	 */
	private int Get_Bits(byte buf[], int BPos[], int N)
	{
		int Pos, Val;
		Pos = BPos[1]>>>3;

		if (Pos >= buf.length)
		{
			GlobalError = true;
			BPos[1] += N;
			BPos[0] = BPos[1]>>>3;
			return 0;
		}

		Val =  (0xFF & buf[Pos++])<<24;

		if (Pos < buf.length)
			Val |= (0xFF & buf[Pos++])<<16;

		if (Pos < buf.length)
			Val |= (0xFF & buf[Pos++])<<8;

		if (Pos < buf.length)
			Val |= (0xFF & buf[Pos]);

		Val <<= BPos[1] & 7;
		Val >>>= 32-N;

		BPos[1] += N;
		BPos[0] = BPos[1]>>>3;

		return Val;
	}

	/**
	 *
	 */
	private int Show_Bits(byte buf[], int BPos[], int N)
	{
		int Pos, Val;
		Pos = BPos[1]>>>3;

		if (Pos >= buf.length)
		{
			GlobalError = true;
			return 0;
		}

		Val =  (0xFF & buf[Pos++])<<24;

		if (Pos < buf.length)
			Val |= (0xFF & buf[Pos++])<<16;

		if (Pos < buf.length)
			Val |= (0xFF & buf[Pos++])<<8;

		if (Pos < buf.length)
			Val |= (0xFF & buf[Pos]);

		Val <<= BPos[1] & 7;
		Val >>>= 32 - N;

		return Val;
	}

	/**
	 *
	 */
	private void Flush_Bits(int BPos[], int N)
	{
		BPos[1] += N;
		BPos[0] = BPos[1]>>>3;
	}

	/**
	 *
	 */
	private void align_Bits(int BPos[])
	{
		if ((1 & BPos[1]>>>2) != 0)
			Flush_Bits( BPos, 4);
	}

	/**
	 *
	 */
	public String isForced_Msg()
	{
		return isForced_Msg(0);
	}

	/**
	 *
	 */
	public String isForced_Msg(int val)
	{
		String str = null;

		//change of status occured
		if ((isforced_status & 1) == 0 || val == 1)
			str = (isforced_status & 2) > 0 ? Resource.getString("subpicture.msg.forced.no") : Resource.getString("subpicture.msg.forced.yes");

		isforced_status |= 1;

		return str;
	}

	/**
	 *
	 */
	public void reset()
	{
		isforced_status = 0;
		ismulticolor_status = 0;
		set_XY_Offset(0, 0);
		setDisplayMode(0);
	}

	/**
	 * modify X,Y Position
	 */
	public void set_XY_Offset(int x_value, int y_value)
	{
		X_Offset = x_value;
		Y_Offset = y_value;
	}

	/**
	 * modify force display flag
	 */
	public void setDisplayMode(int value)
	{
		DisplayMode = value;
	}

	/**
	 *
	 */
	public int decode_picture(byte[] packet, int off, boolean decode, Object obj)
	{
		return decode_picture(packet, off, decode, obj, 0, false, true);
	}

	/**
	 *
	 */
	public int decode_picture(byte[] packet, int off, boolean decode, Object obj, Image previewImage, int previewflags)
	{
		return decode_picture(packet, off, decode, obj, 0, false, true, previewImage, previewflags);
	}

	/**
	 *
	 */
	public int decode_picture(byte[] packet, int off, boolean decode, Object obj, long pts, boolean save, boolean visible)
	{
		return decode_picture(packet, off, decode, obj, pts, save, visible, null, 0);
	}

	/**
	 *
	 */
	public int decode_picture(byte[] packet, int off, boolean decode, Object obj, long pts, boolean save, boolean visible, Image previewImage, int previewflags)
	{
		ReadFromImage = false;
		GlobalError = false;

		boolean simple_picture = false;
		int picture_length = packet.length;

		int[] BPos = { off, off<<3 }; //BytePos, BitPos
		int[] position = new int[4];
		int[] start_pos = new int[3];
		int default_indices = 0;

		ArrayList colcon = new ArrayList();

		if (BPos[0] > picture_length)
			return -4;

		int packetlength = Get_Bits(packet, BPos, 16); // required pack length

		if (Show_Bits(packet, BPos, 24) == 0xF) // DVB subpicture: 8bit padding 0x00 + 8bit subtitle_stream_id 0x00 + start of subtitle segment 0x0F
		{
			big.setFont(font_std);

			byte[] data = new byte[picture_length + 4];
			System.arraycopy(packet, 0, data, 0, picture_length);

			int ret = dvb.decodeDVBSubpicture(data, BPos, big, bimg, pts, save, visible);

			if (ret > -2)
				repaint();

			return ret;
		}

		if (BPos[0] + packetlength != picture_length + 2)
			return -5;

		start_pos[2] = Get_Bits(packet, BPos, 16) - 2;
		Flush_Bits(BPos, start_pos[2]<<3); // jump to sections chunk

		//fixed pos, so it must follow the 1st ctrl sequ,
		//delay of 2nd ctrl sequ execution - usually stop displaying
		int playtime_pos = Get_Bits(packet, BPos, 16);  

		if (playtime_pos == start_pos[2] + 2)
		{
			playtime_pos = packetlength;
			simple_picture = true;
			Common.setMessage(Resource.getString("subpicture.msg2"));
		}
		else
			start_pos[2] += off + 2;

		int[] color_table = getColorTable(0);

		while (BPos[0] < off + playtime_pos)  // read sections chunk
		{
			int cmd_switch = Show_Bits(packet, BPos, 8); // show at first, to enable editing

			switch(cmd_switch)
			{
			case 0: // force display flag
				isforced_status = (isforced_status & 5) != 5 ? 4 : 5;

				if (DisplayMode == 2) //set normal
					Set_Bits(packet, BPos, 8, 1);

				else
					Flush_Bits(BPos, 8);

				break;

			case 1: // start display flag, normal
				isforced_status = (isforced_status & 3) != 3 ? 2 : 3;

				if (DisplayMode == 1) //set forced
					Set_Bits(packet, BPos, 8, 0);

				else
					Flush_Bits(BPos, 8);

				break;

			case 2: // stop display flag
				Flush_Bits(BPos, 8);
				break;

			case 3: // 4 color links
				Flush_Bits(BPos, 8);
				default_indices |= 0xFFFF & Get_Bits(packet, BPos, 16);
				break;

			case 4: // alpha blending
				Flush_Bits(BPos, 8);
				default_indices |= (0xFFFF & Get_Bits(packet, BPos, 16))<<16;
				break;

			case 5: // x,y pos.
				Flush_Bits(BPos, 8);

				if (X_Offset != 0) //move X-pos
				{
					position[0] = Show_Bits(packet, BPos, 12) + X_Offset; //X-links
					Set_Bits(packet, BPos, 12, position[0]); //X-links

					position[1] = Show_Bits(packet, BPos, 12) + X_Offset; //X-rechts
					Set_Bits(packet, BPos, 12, position[1]); //X-rechts
				}
				else
				{
					position[0] = Get_Bits(packet, BPos, 12); //X-links
					position[1] = Get_Bits(packet, BPos, 12); //X-rechts
				}

				if (Y_Offset != 0) //move Y-pos
				{
					position[2] = Show_Bits(packet, BPos, 12) + Y_Offset; //Y-oben
					Set_Bits(packet, BPos, 12, position[2]); //Y-oben

					position[3] = Show_Bits(packet, BPos, 12) + Y_Offset; //Y-unten
					Set_Bits(packet, BPos, 12, position[3]); //Y-unten
				}
				else
				{
					position[2] = Get_Bits(packet, BPos, 12); //X-links
					position[3] = Get_Bits(packet, BPos, 12); //X-rechts
				}

				break;

			case 6: // pos. of decode_start of a field
				Flush_Bits(BPos, 8);

				for (int b = 0; b < 2; b++)
					start_pos[b] = Get_Bits(packet, BPos, 16);

				break;

			case 7: // extra alpha + color area definition
				Flush_Bits(BPos, 8);
				int blen = Get_Bits(packet, BPos, 16); // get length

				if (ismulticolor_status == 0)
				{
					ismulticolor_status |= 1;
					Common.setMessage("-> contains extra area definitions!");
				}

				int area_size = 0;
				int endofcmd = BPos[0] + blen;

				while (BPos[0] < endofcmd)
				{
					area_size = Show_Bits(packet, BPos, 32);  // read 4 bytes linenumbers & def. index

					if (area_size == 0x0FFFFFFF) //end marker
					{
						Flush_Bits(BPos, 32);
						break;
					}

					if (Y_Offset != 0) //move Y-pos
					{
						area_size = (0xF000 & area_size) | ((0xFFF & area_size>>16) + Y_Offset)<<16 | ((0xFFF & area_size) + Y_Offset); //Y
						Set_Bits(packet, BPos, 32, area_size); 
					}
					else
						Flush_Bits(BPos, 32);

					for (int i = 0, j = 0xF & area_size>>12; i < j; i++) //parameter count
					{
						int[] area_defs = new int[5]; // 5 parameters

						area_defs[0] = 0xFFF & area_size>>16; //from top line number
						area_defs[1] = 0xFFF & area_size;  //to bottom line number

						if (X_Offset != 0) //move X-pos
						{
							area_defs[2] = Show_Bits(packet, BPos, 16) + X_Offset; // read 2 bytes column, start of def.
							Set_Bits(packet, BPos, 16, area_defs[2]); //set new
						}
						else
							area_defs[2] = Get_Bits(packet, BPos, 16); // read 2 bytes column, start of def.

						area_defs[3] = Get_Bits(packet, BPos, 16); // read 2 bytes 4x new index of color
						area_defs[4] = Get_Bits(packet, BPos, 16); // read 2 bytes 4x new index of contrast

						colcon.add(area_defs);
					}
				}

				break;

			case 0xFF: // end of ctrl sequ.
				Flush_Bits(BPos, 8);
				break;

			default:
				Common.setMessage(Resource.getString("subpicture.msg3") + ": " + cmd_switch);
				Flush_Bits(BPos, 8);
			}
		}

		if (off + playtime_pos != BPos[0])
			return -6;

		int playtime = 0;

		if (!simple_picture)
		{
			playtime = Get_Bits(packet, BPos, 16);
			if (playtime_pos != Get_Bits(packet, BPos, 16))
				return -7;

			if (Get_Bits(packet, BPos, 8) != 2)
				return -8;

			Flush_Bits( BPos, ((BPos[0] & 1) != 1) ? 16 : 8 );
		}

		if (BPos[0] != picture_length)
			return -9;

		if (GlobalError)
			return -3;

		if (!decode)
			return (playtime * 1024); //DM26052004 081.7 int03 changed , 900, 1000


		start_pos[0] += off;
		start_pos[1] += off;

		paintVideoSize(obj);

		//paint picture at background, fixed size
		if (previewImage != null)
		{
			int aro = Common.getMpvDecoderClass().getMpg2AspectRatioOffset();

			big.setColor(new Color(0xFF505050));
			big.fillRect(0, 0, w, h); 

			int pic_preview_width = 0xFFF & previewflags >> 20;
			int pic_preview_height = 0xFFF & previewflags >> 8;

			if (aro != 0) //4:3 portion of widescreen preview
				big.drawImage(previewImage, 0, 0, pic_preview_width, pic_preview_height, 64, 0, 448, 288, null);

			else if ((previewflags & 2) != 0) // letterbox of widescreen
			{
				int blackborder = (pic_preview_height - ((pic_preview_height * 3) / 4)) / 2;
				big.drawImage(previewImage, 0, blackborder, pic_preview_width, (pic_preview_height * 3) / 4, null);
			}

			else   // anamorph widescreen
				big.drawImage(previewImage, 0, 0, pic_preview_width, pic_preview_height, null);

		}

		int x0 = position[0];
		int y0 = position[2];

		int width = position[1] - position[0] + 1;
		int height = position[3] - position[2] + 1;

		big.setColor(Color.white);
		big.drawRect(x0 - 1, y0 - 1, width + 1, height + 1);
		big.setFont(font_std);
		big.drawString("x" + x0 + ", y" + y0 + " / " + width + "*" + height
				+ " , C-" + Common.adaptString(Integer.toHexString(0xFFFF & default_indices).toUpperCase(), 4)
				+ ", T-" + Common.adaptString(Integer.toHexString(0xFFFF & default_indices>>16).toUpperCase(), 4)
				, x0 + 1, y0 - 5);

//
		// subarray - color index for each pixel 0xFFFFFFFF = Contrast/Color 32103210
		int[] colcon_indices = new int[width * height];

		// default filling, keeps default when no other def's exist
		Arrays.fill(colcon_indices, default_indices);

		// fill with new def's
		for (int b = 0; (previewflags & 8) == 0 && b < colcon.size(); b++)
		{
			int[] area_defs = (int[]) colcon.get(b); // one def block

			// replace indices per line from column to end
			for (int i = (area_defs[0] - y0) * width, j = (area_defs[1] - y0) * width; i < j; i += width)
				Arrays.fill(colcon_indices, i + area_defs[2] - x0, i + width, area_defs[3] | area_defs[4]<<16);

		//	area_defs[2]  2 bytes column, start of def.
		//	area_defs[3]  2 bytes 4x new index of color
		//	area_defs[4]  2 bytes 4x new index of contrast
		}
//

		for (int b = 0; b < 2; b++) // 2 fields painting
		{
			int Val = 0, x1 = x0, y1 = y0 + b;
			BPos[1] = (BPos[0] = start_pos[b])<<3; // top_field at first

			while (BPos[0] < start_pos[b + 1]) // stop at pos_marker
			{
				if ((Val = Get_Bits(packet, BPos, 4)) > 3) //4..F (0..3 never encodable)
					x1 = paintPixel(Val, x1, y1, colcon_indices, color_table, x0, y0, width, previewflags);

				else if ((Val = Val<<4 | Get_Bits(packet, BPos, 4)) > 0xF) //10..3F
					x1 = paintPixel(Val, x1, y1, colcon_indices, color_table, x0, y0, width, previewflags);

				else if ((Val = Val<<4 | Get_Bits(packet, BPos, 4)) > 0x3F) //40..FF
					x1 = paintPixel(Val, x1, y1, colcon_indices, color_table, x0, y0, width, previewflags);

				else if ((Val = Val<<4 | Get_Bits(packet, BPos, 4)) > 3) //100..3FF
					x1 = paintPixel(Val, x1, y1, colcon_indices, color_table, x0, y0, width, previewflags);

				else  // 0 forced carriage return
				{
					if ((Val & 3) != 0)
					{
						Val |= (width - x1)<<2;
						x1 = paintPixel(Val, x1, y1, colcon_indices, color_table, x0, y0, width, previewflags);
					}

					x1 = x0;
					y1 += 2;
					align_Bits(BPos);
					continue;
				}

				// line end, carriage return
				if (x1 > position[1])
				{
					x1 = position[0];
					y1 += 2;
					align_Bits(BPos);
				}
			}
		}

		// paint rectangles of extra col-con definitions
		for (int i = colcon.size() - 1; (previewflags & 4) == 0 && i >= 0; i--)
		{
			int[] area_defs = (int[]) colcon.get(i);

			big.setColor(Color.magenta);
			big.drawRect(area_defs[2] - 1, area_defs[0] - 1, width - area_defs[2] + x0 + 1, area_defs[1] - area_defs[0]);

			big.setColor(Color.yellow);
			big.drawString("area: " + i + " - " + area_defs[0] + ", " + area_defs[1] + ", " + area_defs[2]
							+ ", C-" + Common.adaptString(Integer.toHexString(area_defs[3]).toUpperCase(), 4)
							+ ", T-" + Common.adaptString(Integer.toHexString(area_defs[4]).toUpperCase(), 4)
							, position[0], y0 - 22 - (i * 16));
		}

		repaint();

		if (GlobalError)
			return -3;

		return (playtime * 1024); //DM26052004 081.7 int03 changed, 900, 1000
	}

	/**
	 * paint preview pixel from .sup
	 */
	private int paintPixel(int Val, int x1, int y1, int[] colcon_indices, int[] color_table, int x0, int y0, int width, int previewflags)
	{
		int table_index = Val & 3;
		int line_length = Val>>>2;
		int array_index;
		int contrast_index = 0;
		int color_index = 0; 
		boolean opaque = (previewflags & 0x10) != 0;

		big.setColor(new Color(0));

		for (int j = x1 + line_length, color, lastcolor = 0; x1 < j; x1++)
		{
			array_index = (y1 - y0) * width + x1 - x0;

			//error condition
			if (array_index >= colcon_indices.length)
				break;

			contrast_index = 0xF & colcon_indices[array_index]>>(16 + table_index * 4);
			color_index = 0xF & colcon_indices[array_index]>>(table_index * 4); 

			// set ARGB color
			color = (0x11 * (0xF ^ contrast_index))<<24 | (color_table[color_index] & 0xFFFFFF);

			// check for a change
			if (color != lastcolor)
				big.setColor(new Color(color)); // needs (color, true) for alpha - but with less performance

			lastcolor = color;

			if (opaque || contrast_index > 0) // dont paint full transp. pixel
				big.drawLine(x1, y1, x1, y1);
			//	big.drawLine(x1, y1, x1 + 1, y1);
		}

		return x1;
	}
}
