/*
 * @(#)WSS.java - mini info about WSS
 *
 * Copyright (c) 2004 by dvb.matt, All Rights Reserved. 
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

//DM30072004 introduced with 081.7 int07

public final class WSS
{
	private static int pixels[] = new int[267];
	private static int a;
	private static String str;

	public WSS()
	{
		init(new int[0], 0);
	}

	public static void init(int source_pixels[], int width)
	{
		str = null;

		if (source_pixels.length < 200)
			return;

		scale(source_pixels, width);
	}

	public static String getWSS()
	{
		return str;
	}

	private static void scale(int source_pixels[], int width)
	{
		java.util.Arrays.fill(pixels, 0);

		int nx = 267;
		float fx = 0;
		float Xdecimate = width / (float)(nx);

		for (int x = 0; fx < width && x < nx; fx += Xdecimate, x++)
		{
			pixels[x] = source_pixels[(int)fx];
			pixels[x] = Ifo.RGBtoYUV(pixels[x])>>>16;
		}

		a = 0;

		handlepixels();  // read out
	}

	private static void handlepixels()
	{
		str = "WSS status:<p>";

		if (getRunIn())
		{
			str += "Run-In-Code found @ " + a + "<p>";
			a += 29;

			if (getStartCode())
			{
				str += "Start-Code found @ " + a + "<p>";

				a += 24;

				str += "Group 1 (Picture Format) start @ " + a + " :" + "<p>";
				str += " * " + getGroup1() + "<p>";

				a += 24;

				str += "Group 2 (Picture Enhancements) start @ " + a + " :" + "<p>";

				String[] group = getGroup2();

				for (int d=0; d < 4; d++) 
					str += " * " + group[d] + "<p>";

				a += 24;

				str += "Group 3 (Subtitles) start @ " + a + " :" + "<p>";

				group = getGroup3();

				for (int d=0; d < 2; d++) 
					str += " * " + group[d] + "<p>";

				a += 18;

				str += "Group 4 (others) start @ " + a + " :" + "<p>";

				group = getGroup4();

				for (int d=0; d < 2; d++) 
					str += " * " + group[d] + "<p>";

			}
			else
				str += "no Start-Code found";
		}
		else
		{
			str += "no Run-In-Code found";
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
			return "  4:3 full format, 576 lines, full screen"; 

		case 0x95: // 1000  Biphase 10010101
			return "  14:9 letterbox, 504 lines, center"; 

		case 0x65: // 0100  Biphase 01100101
			return "  14:9 letterbox, 504 lines, top"; 

		case 0xA6: // 1101  Biphase 10100110
			return "  16:9 letterbox, 432 lines, center"; 

		case 0x59: // 0010  Biphase 01011001
			return "  16:9 letterbox, 432 lines, top"; 

		case 0x6A: // 0111  Biphase 01101010
			return "  14:9 full format, 576 lines, center, full screen"; 

		case 0xA9: // 1110  Biphase 10101001
			return "  16:9 full format, 576 lines, full screen"; 

		default:  
			return "  error parsing group 1 (bits0..3)";
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
			group2[0]= "  camera mode";
			break; 

		case 2: // 1  Biphase 10
			group2[0]= "  film mode"; 
			break;

		default:  
			group2[0]= "  error parsing bit4";
		}       

		switch (0x3 & (b1>>>4))
		{
		case 1: // 0  Biphase 01
			group2[1]= "  standard PAL"; 
			break;

		case 2: // 1  Biphase 10
			group2[1]= "  MACP (motion adaptive color plus)"; 
			break;

		default:  
			group2[1]= "  error parsing bit5"; 
		}       

		switch (0x3 & (b1>>>2))
		{
		case 1:  // 0  Biphase 01
			group2[2]= "  no helper"; 
			break;

		case 2:  // 1  Biphase 10
			group2[2]= "  helper modulation (PALplus)"; 
			break;

		default:  
			group2[2]= "  error parsing bit6";
		}       

		switch (0x3 & b1)
		{
		case 1: // 0  Biphase 01
			group2[3]= "  reserved (0)"; 
			break;

		case 2:  // 1  Biphase 10
			group2[3]= "  reserved (1)"; 
			break;

		default:  
			group2[3]= "  error parsing bit7";
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
			group2[0]= "  no subtitles in teletext"; 
			break;

		case 2: // 1  Biphase 10
			group2[0]= "  subtitles in teletext"; 
			break;

		default:
			group2[0]= "  error parsing bit8";
		}       

		switch (0xF & b1)
		{
		case 5:  // 00  Biphase 0101
			group2[1]= "  no 'open subtitles'"; 
			break;

		case 6:  // 01  Biphase 0110
			group2[1]= "  'open subtitles' inside active picture"; 
			break;

		case 9:  // 10  Biphase 1001
			group2[1]= "  'open subtitles' outside active picture"; 
			break; 

		case 0xA:  // 11  Biphase 1010
			group2[1]= "  reserved (11)"; 
			break;

		default: 
			group2[1]= "  error parsing bit9..10";
		}       

		return group2;
	}

	private static String[] getGroup4()
	{
		int b1 = 0;

		for (int c=0; c < 6; c++)
			b1 |= pixels[a + (3 * c)] < 120 ? 0 : (1<<(5 - c));

		String[] group2 = new String[2];

		switch (0x3 & (b1>>>4))
		{
		case 1:  // 0  Biphase 01
			group2[0]= "  no surround sound";
			break;

		case 2:  // 1  Biphase 10
			group2[0]= "  surround sound";
			break;

		default:
			group2[0]= "  error parsing bit11";
		}       

		switch (0xF & b1)
		{
		case 5: // 00  Biphase 0101
			group2[1]= "  reserved (00)";
			break;

		case 6: // 01  Biphase 0110
			group2[1]= "  reserved (01)"; 
			break; 

		case 9:  // 10  Biphase 1001
			group2[1]= "  reserved (10)"; 
			break;

		case 0xA:  // 11  Biphase 1010
			group2[1]= "  reserved (11)"; 
			break;

		default:
			group2[1]= "  error parsing bit12..13";
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