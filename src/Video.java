/*
 * @(#)VIDEO.java - some video constants
 *
 * Copyright (c) 2003,2004 by dvb.matt, All Rights Reserved. 
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


public class Video {

String[] aspratio = {"res.","1:1","4:3","16:9","2.21:1","0.8055","0.8437","0.9375","0.9815","1.0255","1.0695","1.1250","1.1575","1.2015","res.","res." };
String[] fps_tabl1 = {"forbidden fps","23.976fps","24fps","25fps","29.97fps","30fps","50fps","59.94fps","60fps","n.def.","n.def.","n.def.","n.def.","n.def.","n.def.","n.def."};
// int[] fps_tabl2 = {0,3753,3750,3600,3003,3000,1800,1501,1500,0,0,0,0,0,0,0};

/***************************
 * format display from byte *
 ***************************/
public String videoformatByte(byte[] gop) {
	return ""+((255&gop[4])<<4 | (240&gop[5])>>>4)+"*"+((15&gop[5])<<8 | (255&gop[6]))+", "+fps_tabl1[15&gop[7]]+", "+aspratio[(255&gop[7])>>>4]+", "+( ((255&gop[8])<<10 | (255&gop[9])<<2 | (192 & gop[10])>>>6)*400  )+"bps, vbv "+( (31&gop[10])<<5 | (248&gop[11])>>>3 );
}

}