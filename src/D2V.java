/*
 * @(#)D2V.java - simple independent implementation to create a d2v Projectfile
 *
 * Copyright (c) 2002-2004 by dvb.matt, All Rights Reserved. 
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

import java.io.*;
import java.util.*;

public class D2V {

ArrayList d2v = new ArrayList();
int[]  fps_table = { 0,23976,24000,25000,29970,30000,50000,59940,60000,0,0,0,0,0,0,0 };
String[] type = { " 2"," 9" };
String lastGOP = "", nextGOP = "";
int entry=0, file=0, frame1=0, frame2=0;
String d2vfile = "";
String[] d2voptions = { "iDCT_Algorithm=2", "YUVRGB_Scale=1", "Luminance=128,0",
		"Picture_Size=0,0,0,0,0,0", "Field_Operation=0" };

public void Init(String fparent) {
	entry=0;
	file=0;
	d2v.clear();
	d2vfile = fparent+".d2v";
	d2v.add("DVD2AVIProjectFile");
	d2v.add("1");
	d2v.add(""+(fparent.length()+4)+" "+fparent+".mpv");
	d2v.add("");
	d2v.add("Stream_Type=0,0,0");
	d2v.add(d2voptions[0]);
	d2v.add(d2voptions[1]);
	d2v.add(d2voptions[2]);
	d2v.add(d2voptions[3]);
	d2v.add(d2voptions[4]);
	entry+=9;
}

public int getPart() {
	return file;
}

public void setFile(String fparent) {
	d2v.add(3+file,""+fparent.length()+" "+fparent);
	file++;
	entry++;
}

public String[] readOptions() {
	return d2voptions;
}

public void setOptions(String[] newoptions) {
	d2voptions = (String[])newoptions.clone();
}

public void FrameRate(byte framerate) {
	d2v.add("Frame_Rate="+(fps_table[15&framerate]));
	d2v.add("");
	entry += 2;
}

public void addGOP(long pos, int frames) {
	String fill = "7 "+file+" "+Integer.toHexString((int)(pos/2048L)).toUpperCase();
	for (int a=0;a<frames;a++) 
		fill += type[0];
	lastGOP = fill+type[0]+type[0]+type[1];
	d2v.add(fill);
	entry++;
}

public void write(long pos, long frame3) {
	d2v.set(1,""+(file+1));
	d2v.add(11+file,"Location=0,0,"+file+","+Integer.toHexString((int)(pos/2048L)).toUpperCase());
	entry++;
	d2v.set(entry,lastGOP);
	d2v.add("");
	d2v.add("FINISHED");
	if (d2v.size()<14) { 
		d2v.clear(); 
		return; 
	}

	try 
	{
	BufferedWriter d2vout = new BufferedWriter(new FileWriter(d2vfile));
	for (int a=0;a<d2v.size();a++) {
		d2vout.write(d2v.get(a).toString());
		d2vout.newLine();
	}
	d2vout.close();

	}
	catch (IOException e) { 
		X.Msg("-> d2v write error!"); 
	}
	d2v.clear();
}

}
