/*
 * @(#)WSS.java - mini info about WSS
 *
 * Copyright (c) 2004-2005 by dvb.matt, All Rights Reserved. 
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

package net.sourceforge.dvb.projectx.video;

import net.sourceforge.dvb.projectx.common.Resource;

import java.util.Arrays;

//DM30072004 introduced with 081.7 int07
public final class WSS extends Object {

	private static boolean isPalplus = false;

	private static int pixels[] = new int[267];
	private static int a;
	private static String str;

	private static String format;

	private static String start = Resource.getString("wss.start");

	public WSS()
	{
		init(new int[0], 0);
	}

	public static void init(int source_pixels[], int width)
	{
		str = null;
		format = "";

		if (source_pixels.length < 200)
			return;

		scale(source_pixels, width);
	}

	public static String getWSS()
	{
		return str;
	}

	public static boolean isPalPlus()
	{
		return isPalplus;
	}

	public static String getFormatInfo()
	{
		return format;
	}

	private static void scale(int source_pixels[], int width)
	{
		Arrays.fill(pixels, 0);

		int nx = 267;
		float fx = 0;
		float Xdecimate = width / (float)(nx);

		for (int x = 0; fx < width && x < nx; fx += Xdecimate, x++)
			pixels[x] = 0xFF & source_pixels[(int)fx]>>>16;

		a = 0;

		/**
		 * WSS of PAL 625-lines
		 */
		handlepixels("line 0 (23)");  // read out

		if (str == null)
		{
			fx = 0;

			for (int x = 0; fx < width && x < nx; fx += Xdecimate, x++)
				pixels[x] = 0xFF & source_pixels[width + (int)fx]>>>16;

			a = 0;

			handlepixels("line 1 (335)");  // read out
		}
	}

	private static void handlepixels(String _str)
	{
		isPalplus = false;
		format = "";

		str = "WSS status @ " + _str;
		str += ":<p>";

		if (getRunIn())
		{
			str += Resource.getString("wss.run_in") + " @ " + a + "<p>";
			a += 29;

			if (getStartCode())
			{
				str += Resource.getString("wss.startcode") + " @ " + a + "<p>";

				a += 24;

				str += Resource.getString("wss.group_1") + " " + start + " @ " + a + " :" + "<p>";
				str += " * " + getGroup1() + "<p>";

				a += 24;

				str += Resource.getString("wss.group_2") + " " + start + " @ " + a + " :" + "<p>";

				String[] group = getGroup2();

				for (int d = 0; d < group.length; d++) 
					str += " * " + group[d] + "<p>";

				a += 24;

				str += Resource.getString("wss.group_3") + " " + start + " @ " + a + " :" + "<p>";

				group = getGroup3();

				for (int d = 0; d < group.length; d++) 
					str += " * " + group[d] + "<p>";

				a += 18;

				str += Resource.getString("wss.group_4") + " " + start + " @ " + a + " :" + "<p>";

				group = getGroup4();

				for (int d = 0; d < group.length; d++) 
					str += " * " + group[d] + "<p>";

			}
			else
				str += Resource.getString("wss.no_startcode");
		}
		else
		{
			str += Resource.getString("wss.no_run_in");
			str = null;
		}

		a=0;
	}

	private static boolean getRunIn()
	{
	        for (; a < 30; a++)
			if (	pixels[a] > 120 
				&& pixels[a + 2] >= 120 
				&& pixels[a + 5] < 120
				&& pixels[a + 8] >= 120
				&& pixels[a + 11] < 120
				&& pixels[a + 14] >= 120
				&& pixels[a + 17] < 120
				&& pixels[a + 20] >= 120
				&& pixels[a + 23] < 120
				&& pixels[a + 26] >= 120 )
				return true;

		return false;
	}

	private static boolean getStartCode()
	{
		if (	pixels[a] < 120
			&& pixels[a + 3] >= 120
			&& pixels[a + 7] < 120
			&& pixels[a + 10] >= 120
			&& pixels[a + 14] < 120
			&& pixels[a + 19] >= 120 )
			return true;

        	return false;
	}

	private static String getGroup1()
	{
		int b1=0;

		for (int c=0; c < 8; c++) 
			b1 |= pixels[a + (3 * c)] < 120 ? 0 : (1<<(7 - c));

		switch (b1)
		{
		case 0x56: // 0001  Biphase 01010110
			format = "[4:3 full]";
			return ("  " + Resource.getString("wss.group_1.0001"));

		case 0x95: // 1000  Biphase 10010101
			format = "[14:9 LB center]";
			return ("  " + Resource.getString("wss.group_1.1000"));

		case 0x65: // 0100  Biphase 01100101
			format = "[14:9 LB top]";
			return ("  " + Resource.getString("wss.group_1.0100"));

		case 0xA6: // 1101  Biphase 10100110
			format = "[16:9 LB center]";
			return ("  " + Resource.getString("wss.group_1.1101"));

		case 0x59: // 0010  Biphase 01011001
			format = "[16:9 LB top]";
			return ("  " + Resource.getString("wss.group_1.0010"));

		case 0x6A: // 0111  Biphase 01101010
			format = "[14:9 full]";
			return ("  " + Resource.getString("wss.group_1.0111"));

		case 0xA9: // 1110  Biphase 10101001
			format = "[16:9 full]";
			return ("  " + Resource.getString("wss.group_1.1110"));

		default:  
			return ("  " + Resource.getString("wss.group_1.error"));
		}
	}

	private static String[] getGroup2()
	{
		int b1=0;

		for (int c=0; c < 8; c++) 
			b1 |= pixels[a + (3 * c)] < 120 ? 0 : (1<<(7 - c));

		String[] group2 = new String[4];

		switch (b1>>>6)
		{
		case 1: // 0  Biphase 01
			group2[0]= "  " + Resource.getString("wss.group_2.0.01");
			break; 

		case 2: // 1  Biphase 10
			group2[0]= "  " + Resource.getString("wss.group_2.0.10"); 
			break;

		default:  
			group2[0]= "  " + Resource.getString("wss.group_2.0.00");
		}       

		switch (0x3 & (b1>>>4))
		{
		case 1: // 0  Biphase 01
			group2[1]= "  " + Resource.getString("wss.group_2.1.01"); 
			break;

		case 2: // 1  Biphase 10
			group2[1]= "  " + Resource.getString("wss.group_2.1.10"); 
			break;

		default:  
			group2[1]= "  " + Resource.getString("wss.group_2.1.00"); 
		}       

		switch (0x3 & (b1>>>2))
		{
		case 1:  // 0  Biphase 01
			group2[2]= "  " + Resource.getString("wss.group_2.2.01"); 
			break;

		case 2:  // 1  Biphase 10
			group2[2]= "  " + Resource.getString("wss.group_2.2.10"); 
			isPalplus = true;
			break;

		default:  
			group2[2]= "  " + Resource.getString("wss.group_2.2.00");
		}       

		switch (0x3 & b1)
		{
		case 1: // 0  Biphase 01
			group2[3]= "  " + Resource.getString("wss.group_2.3.01"); 
			break;

		case 2:  // 1  Biphase 10
			group2[3]= "  " + Resource.getString("wss.group_2.3.10"); 
			break;

		default:  
			group2[3]= "  " + Resource.getString("wss.group_2.3.00");
		}       

		return group2;
	}

	private static String[] getGroup3()
	{
		int b1 = 0;

		for (int c=0; c < 6; c++)
			b1 |= pixels[a + (3 * c)] < 120 ? 0 : (1<<(5 - c));

		String[] group2 = new String[2];

		switch (0x3 & (b1>>>4))
		{
		case 1: // 0  Biphase 01
			group2[0]= "  " + Resource.getString("wss.group_3.0.01"); 
			break;

		case 2: // 1  Biphase 10
			group2[0]= "  " + Resource.getString("wss.group_3.0.10"); 
			format += "[UT]";
			break;

		default:
			group2[0]= "  " + Resource.getString("wss.group_3.0.00");
		}       

		switch (0xF & b1)
		{
		case 5:  // 00  Biphase 0101
			group2[1]= "  " + Resource.getString("wss.group_3.1.00"); 
			break;

		case 6:  // 01  Biphase 0110
			group2[1]= "  " + Resource.getString("wss.group_3.1.01"); 
			break;

		case 9:  // 10  Biphase 1001
			group2[1]= "  " + Resource.getString("wss.group_3.1.10"); 
			break; 

		case 0xA:  // 11  Biphase 1010
			group2[1]= "  " + Resource.getString("wss.group_3.1.11"); 
			break;

		default: 
			group2[1]= "  " + Resource.getString("wss.group_3.1.err");
		}       

		return group2;
	}

	private static String[] getGroup4()
	{
		int b1 = 0;

		for (int c = 0; c < 6; c++)
			b1 |= pixels[a + (3 * c)] < 120 ? 0 : (1<<(5 - c));

		String[] group2 = new String[3];

		switch (3 & (b1>>>4))
		{
		case 1:  // 0  Biphase 01
			group2[0]= "  " + Resource.getString("wss.group_4.0.01");
			break;

		case 2:  // 1  Biphase 10
			group2[0]= "  " + Resource.getString("wss.group_4.0.10");
			break;

		default:
			group2[0]= "  " + Resource.getString("wss.group_4.0.00");
		}       

		switch (3 & (b1>>>2))
		{
		case 1:  // 0  Biphase 01
			group2[1]= "  " + Resource.getString("wss.group_4.1.01");
			break;

		case 2:  // 1  Biphase 10
			group2[1]= "  " + Resource.getString("wss.group_4.1.10");
			break;

		default:
			group2[1]= "  " + Resource.getString("wss.group_4.1.00");
		}       

		switch (3 & b1)
		{
		case 1:  // 0  Biphase 01
			group2[2]= "  " + Resource.getString("wss.group_4.2.01");
			break;

		case 2:  // 1  Biphase 10
			group2[2]= "  " + Resource.getString("wss.group_4.2.10");
			break;

		default:
			group2[2]= "  " + Resource.getString("wss.group_4.2.00");
		}       

		return group2;
	}
}

/***
 WSS
 takt = 200ns
 1 bit = 3*200ns = 600ns
 1NRZ bit = 2*3*200 = 1200ns

 864 bit auf 64000ns PAL  @ 13.5MHz  74.074   (864*15.625khz)
 320 bit auf 64000ns @  5MHz  200ns  

 = 74.074 ns / bit
 = 702 pix auf 52000ns
 = 720 pix auf 53333ns  = 266.666 bits bei 200ns/b
 142 + 20
 
 858 bit auf 63560ns NTSC @ 13.5MHz



**/