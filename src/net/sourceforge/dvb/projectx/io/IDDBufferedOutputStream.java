/*
 * @(#)IDDBufferedOutputStream.java - new Mpeg2Schnitt export
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

package net.sourceforge.dvb.projectx.io;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import net.sourceforge.dvb.projectx.common.Common;

public class IDDBufferedOutputStream extends BufferedOutputStream
{
	long pos = 0;
	int type = 0;
	String name = "", chaptersname = "";
	boolean sequenceend = false, chapters = false;

	byte IddHeader[][] = {
		{ (byte)'i',(byte)'d',(byte)'d',2 },  //video V2
		{ (byte)'i',(byte)'d',(byte)'d',3 }   //audio V3
	};

	BufferedOutputStream IddOut = null;
	PrintWriter ChaptersOut = null;

	int filenumber=1;  //DM18022004 081.6 int17 new

	public IDDBufferedOutputStream(OutputStream out)
	{
		super(out);
	}

	public IDDBufferedOutputStream(OutputStream out, int size)
	{
		super(out,size);
	}

	public synchronized void write(byte b[], int off, int len) throws IOException
	{
		switch (type)
		{
		case 3: //DM18022004 081.6 int17 new, vdrindex, //DM21022004 081.6 int18 changed
			if ((0xF0 & b[off+3])!=0xE0)
				break;

			for (int a = off + 9+ (0xFF & b[8]); a<off+len-3; a++)
			{
				if (b[a]!=0 || b[a+1]!=0 || b[a+2]!=1 || b[a+3]!=0)
					continue;

				if (a+5 >= off+len)
					break;

				int frametype = (7 & b[a+5]>>>3);
				if (frametype==0 || frametype>3)
					break;
				IddOut.write(VdrIndex()); //pos
				IddOut.write(frametype); //type
				IddOut.write(filenumber);
				IddOut.write(new byte[2]);
				break;
			}

			break;

		case 1:
			for (int a=0; a<b.length-3; a++)
			{
				if (b[a]!=0 || b[a+1]!=0 || b[a+2]!=1)
					continue;

				if ((0xFF&b[a+3])==0xB3)
				{
					IddOut.write(0xB3);
					IddOut.write(littleEndian(a));
					a+=12;
				}

				else if ((0xFF&b[a+3])==0xB7)
				{
					IddOut.write(0xB7);
					IddOut.write(littleEndian(a));
					sequenceend=true;
					a+=3;
				}

				else if ((0xFF&b[a+3])==0xB8)
				{
					IddOut.write(0xB8);
					IddOut.write(littleEndian(a));
					a+=7;
				}

				else if (b[a+3]==0)
				{
					IddOut.write(0); //type
					IddOut.write(littleEndian(a)); //pos
					int tref=(3&b[a+5]>>>6) | (0xFF&b[a+4])<<2;
					IddOut.write(0xFF&tref);
					IddOut.write(tref>>>8);
					IddOut.write(7&b[a+5]>>>3); //pic
					a+=8; //DM14122003 081.6 int06
				}
			}
			break;

		case 2:
			if (!(pos==0 && b.length<=0x50))
				IddOut.write(littleEndian(0));
		}

		super.write(b,off,len);
		pos+=len;
	}

	public synchronized void write(int b) throws IOException
	{
		super.write(b);
		pos++;
	}

	//DM18022004 081.6 int17 new
	public byte[] VdrIndex()
	{
		byte bpos[] = new byte[4];

		for (int a=0; a<4; a++)
			bpos[a]=(byte)(0xFFL & pos>>>(a*8));

		return bpos;
	}

	public void InitVdr(String vdrname, int file_number) throws IOException
	{
		name=vdrname;
		filenumber=file_number+1;
		type=3;

		IddOut=new BufferedOutputStream(new FileOutputStream(name,filenumber==1?false:true),655350);
	}

	public String renameVdrTo(String parent, String oldName)
	{
		String num = "000"+filenumber+".vdr";
		String newName = parent + num.substring(num.length()-7);

		File nname = new File(newName);
		File oname = new File(oldName);

		//DM09072004 081.7 int06 changed
		if (!oname.getName().equals(nname.getName()) && nname.exists())
			nname.delete();

		Common.renameTo(oname, nname); //DM13042004 081.7 int01 changed

		return newName;
	}

	public byte[] littleEndian(int off)
	{
		byte bpos[] = new byte[8];

		for (int a=0; a < 8; a++)
			bpos[a] = (byte)(0xFFL & (pos + off)>>>(a * 8));

		return bpos;
	}

	public void InitIdd(String iddname, int iddtype) throws IOException
	{
		name = iddname + ".id";
		type = iddtype;

		IddOut = new BufferedOutputStream(new FileOutputStream(name),655350);
		IddOut.write(IddHeader[type-1]);
	}

	public void renameIddTo(File newName)
	{
		String str = newName.toString();
		File nname = new File(str + ".idd");
		File file = new File(str + ".m2s.txt");

		if (newName.exists())
		{
			if (nname.exists())
				nname.delete();

			if (new File(name).exists())  //DM13042004 081.7 int01 changed
				Common.renameTo(new File(name), nname);
		}
		else
			new File(name).delete();

		if (chapters)
		{
			if (file.exists())
				file.delete();

			if (new File(chaptersname).exists())
				Common.renameTo(new File(chaptersname), file);
		}
	}

	public void renameVideoIddTo(String newName)
	{
		File nname = new File(newName + ".idd");

		if (nname.exists())
			nname.delete();

		Common.renameTo(new File(name), nname);
	}

	public void deleteIdd()
	{
		new File(name).delete();
		new File(chaptersname).delete();
	}

	public void InitChapters(String filename) throws IOException
	{
		chaptersname = filename + ".chp";
		chapters = true;

		ChaptersOut = new PrintWriter(new FileOutputStream(chaptersname));
	}

	public void addChapter(String str) throws IOException
	{
		if (!chapters)
			return;

		ChaptersOut.println(str);
	}

	//DM18022004 081.6 int17 changed
	public synchronized void close() throws IOException
	{
		if (chapters)
		{
			ChaptersOut.flush();
			ChaptersOut.close();
		}

		switch (type)
		{
		case 1:
			if(!sequenceend)
			{
				IddOut.write(0xB7);
				IddOut.write(littleEndian(0));
			}

			IddOut.flush();
			IddOut.close();

			break;

		case 2:
			IddOut.write(littleEndian(0));

		case 3:
			IddOut.flush();
			IddOut.close();
		}

		super.close();
	}
}
