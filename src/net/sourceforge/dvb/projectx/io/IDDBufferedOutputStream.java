/*
 * @(#)IDDBufferedOutputStream.java - export
 *
 * Copyright (c) 2003-2008 by dvb.matt, All Rights Reserved.
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

/**
 * cuttermaran info part from Arnaud
 *
 */

package net.sourceforge.dvb.projectx.io;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import java.util.Arrays;

import net.sourceforge.dvb.projectx.common.Common;

import net.sourceforge.dvb.projectx.parser.CommonParsing;

public class IDDBufferedOutputStream extends BufferedOutputStream {

    long pos = 0;
	int type = 0;

	String name = "";
	String chaptersname = "";

	boolean sequenceend = false;
	boolean chapters = false;
	boolean CreateAc3Wave = false;
	boolean CreateDtsWave = false;
	boolean CreateDts48Wave = false;

	byte[][] IddHeader = {
		{ (byte)'i', (byte)'d', (byte)'d', 2 },  //video V2
		{ (byte)'i', (byte)'d', (byte)'d', 3 }   //audio V3
	};

	/**
	 * Lil' endian - header, one frame, length of this frame in bits
	 */
	byte[] DD_header = { 0x72, (byte)0xF8, 0x1F, 0x4E, 1, 0, 0, 0 };
	byte[] padding_block = null;

	BufferedOutputStream IddOut = null;
	PrintWriter ChaptersOut = null;
	
    // cminfo ----- BEGIN -----
    private String aInfoName = "";
    private BufferedOutputStream aInfoOut = null; // --- cminfo ---
    private boolean aInfoSeqEnd = false;
    private BitWalker aBitWalker = null;
    private StringBuffer aStringBuffer = null;
    // cminfo ------ END ------
    
	int filenumber = 1;

	/**
	 *
	 */
	public IDDBufferedOutputStream(OutputStream out)
	{
		super(out);
	}

	/**
	 *
	 */
	public IDDBufferedOutputStream(OutputStream out, int size)
	{
		super(out, size);
	}

	/**
	 *
	 */
	public synchronized void write(byte b[], int off, int len) throws IOException
	{
		switch (type)
		{
		case 3: //vdrindex
			if ((0xF0 & b[off + 3]) != 0xE0)
				break;

			for (int a = off + 9 + (0xFF & b[8]), ret; a < off + len - 3; a++)
			{
				if ((ret = CommonParsing.validateStartcode(b, a)) < 0)
				{
					a += (-ret) - 1;
					continue;
				}

				if (b[a + 3] != 0)
				{
					a += 3;
					continue;
				}

				if (a + 5 >= off + len)
					break;

				int frametype = (7 & b[a + 5]>>>3);
				if (frametype == 0 || frametype > 3)
					break;
				IddOut.write(VdrIndex()); //pos
				IddOut.write(frametype); //type
				IddOut.write(filenumber);
				IddOut.write(new byte[2]);
				break;
			}

			break;

		case 1:  //idd Video
            // cminfo ----- BEGIN -----
            if (aInfoOut != null)
			{
                writeInfo(b, off, len);

                if (IddOut == null)
                    break;
            }
            // cminfo ------ END ------

			for (int a = off, ret; a < off + len - 3; a++)
			{
				if ((ret = CommonParsing.validateStartcode(b, a)) < 0)
				{
					a += (-ret) - 1;
					continue;
				}

				if ((0xFF & b[a + 3]) == 0xB3)
				{
					IddOut.write(0xB3);
					IddOut.write(littleEndian(a - off));
					a += 12;
				}

				else if ((0xFF & b[a + 3]) == 0xB7)
				{
					IddOut.write(0xB7);
					IddOut.write(littleEndian(a - off));
					sequenceend = true;
					a += 3;
				}

				else if ((0xFF & b[a + 3]) == 0xB8)
				{
					IddOut.write(0xB8);
					IddOut.write(littleEndian(a - off));
					a += 7;
				}

				else if (b[a + 3] == 0)
				{
					IddOut.write(0); //type
					IddOut.write(littleEndian(a - off)); //pos
					int tref = (3 & b[a + 5]>>>6) | (0xFF & b[a + 4])<<2;
					IddOut.write(0xFF & tref);
					IddOut.write(tref>>>8);
					IddOut.write(7 & b[a + 5]>>>3); //pic
					a += 8;
				}
			}
			break;

		case 2: //idd audio
			if ( !(pos == 0 && b.length <= 0x50))
				IddOut.write(littleEndian(0));
		}


		/**
		 * build new frame with header + padding
		 */
		if (CreateAc3Wave)
		{
			int bitlen = len<<3;

			DD_header[6] = (byte) (0xFF & bitlen);
			DD_header[7] = (byte) (0xFF & bitlen>>8);

			super.write(DD_header, 0, 8);  // header

			pos += 8;

			Common.changeByteOrder(b, off, len);

			super.write(b, off, len);  // the reversed frame
			super.write(padding_block, len, padding_block.length - len);  // the padding zero's

			pos += padding_block.length;
		}

		/**
		 * build new frame with padding
		 */
		else if (CreateDtsWave)
		{
			if (CreateDts48Wave)
			{
				int len_1 = len > 0x800 ? 0x1000 : 0x800;

				System.arraycopy(b, off, padding_block, 0, len); //copy frame
				Arrays.fill(padding_block, len, len_1 + 1, (byte)0);

				Common.changeByteOrder(padding_block, 0, len_1);

				super.write(padding_block, 0, len_1);  // the reversed frame
				pos += len_1;
			}

			else
			{
				int j = 0;
				long val;

				for (int i = off, k = off + len; i < k; i += 7, j += 8)
				{
					val = CommonParsing.getValue(b, i, 7, !CommonParsing.BYTEREORDERING);

					padding_block[j] = (byte)(0xFF & val>>42);
					padding_block[j + 1] = (byte)((byte)(0xFC & val>>48)>>2);
					padding_block[j + 2] = (byte)(0xFF & val>>28);
					padding_block[j + 3] = (byte)((byte)(0xFC & val>>34)>>2);
					padding_block[j + 4] = (byte)(0xFF & val>>14);
					padding_block[j + 5] = (byte)((byte)(0xFC & val>>20)>>2);
					padding_block[j + 6] = (byte)(0xFF & val);
					padding_block[j + 7] = (byte)((byte)(0xFC & val>>6)>>2);
				}

				super.write(padding_block, 0, j);  // the reversed frame
				pos += j;
			}
		}

		else
		{
			super.write(b, off, len);
			pos += len;
		}
	}

	/**
	 *
	 */
	public synchronized void write(int b) throws IOException
	{
		super.write(b);
		pos++;
	}

	/**
	 *
	 */
	public byte[] VdrIndex()
	{
		byte bpos[] = new byte[4];

		for (int a = 0; a < 4; a++)
			bpos[a] = (byte)(0xFFL & pos>>>(a * 8));

		return bpos;
	}

	/**
	 *
	 */
	public void setWave(boolean b1, boolean b2, boolean b3, int val) throws IOException
	{
		if (!b1)
			return;

		CreateAc3Wave = b2;
		CreateDtsWave = b3;
		CreateDts48Wave = (CreateDtsWave && val > 1411200);

		if (padding_block == null)
			padding_block = new byte[0x17F8];
	}

	/**
	 *
	 */
	public void InitVdr(String vdrname, int file_number) throws IOException
	{
		name = vdrname;
		filenumber = file_number + 1;
		type = 3;

		IddOut = new BufferedOutputStream(new FileOutputStream(name, filenumber == 1 ? false : true), 655350);
	}

	/**
	 *
	 */
	public String renameVdrTo(String parent, String oldName)
	{
		String num = "000" + filenumber + ".vdr";
		String newName = parent + num.substring(num.length() - 7);

		File nname = new File(newName);
		File oname = new File(oldName);

		if (!oname.getName().equals(nname.getName()) && nname.exists())
			nname.delete();

		Common.renameTo(oname, nname);

		return newName;
	}

	/**
	 *
	 */
	public byte[] littleEndian(int off)
	{
		byte bpos[] = new byte[8];

		for (int a = 0; a < 8; a++)
			bpos[a] = (byte)(0xFFL & (pos + off)>>>(a * 8));

		return bpos;
	}
    
	/**
	 *
	 */
	public void InitIdd(String iddname, int iddtype)
	{
		try {
			name = iddname + ".id";
			type = iddtype;

			IddOut = new BufferedOutputStream(new FileOutputStream(name), 655350);
			IddOut.write(IddHeader[type - 1]);

		} catch (IOException e) {

			Common.setExceptionMessage(e);
		}
	}

    /**
	 *
	 */
	public void renameIddTo(File newName)
	{
		String str = newName.toString();
		File nname = new File(str + ".idd");
		File file = new File(str + ".m2s.txt");

		if (newName.exists())
		{
			if (nname.exists())
				nname.delete();

			if (new File(name).exists())
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

    /**
     *
     */
    public void renameVideoIddTo(String newName)
    {
        File nname = new File(newName + ".idd");

        if (nname.exists())
            nname.delete();

        Common.renameTo(new File(name), nname);
    }

	/**
	 *
	 */
	public void deleteIdd()
	{
		new File(name).delete();
		new File(chaptersname).delete();
	}

	/**
	 *
	 */
	public void InitChapters(String filename) throws IOException
	{
		chaptersname = filename + ".chp";
		chapters = true;

		ChaptersOut = new PrintWriter(new FileOutputStream(chaptersname));
	}

	/**
	 *
	 */
	public void addChapter(String str) throws IOException
	{
		if (!chapters)
			return;

		ChaptersOut.println(str);
	}

	/**
	 *
	 */
	public synchronized void flush() throws IOException
	{
		super.flush();
	}

	/**
	 *
	 */
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
            // cminfo ----- BEGIN -----
            if (aInfoOut != null)
			{
                closeInfo();

                if (IddOut == null)
                    break;
            }
            // cminfo ------ END ------

			if(!sequenceend)
			{
				IddOut.write(0xB7);
				IddOut.write(littleEndian(0));
			}

			IddOut.flush();
			IddOut.close();
			IddOut = null; // cminfo
			break;

		case 2:
			IddOut.write(littleEndian(0));

		case 3:
			IddOut.flush();
			IddOut.close();
		}

		super.close();
	}

    // cminfo ----- BEGIN -----
    /**
     * @param inpName
     * @throws IOException
     */
    public void InitInfo(String inpName) throws IOException
	{
        aInfoName = inpName + ".info";
        type = 1;

        aInfoOut = new BufferedOutputStream(new FileOutputStream(aInfoName), 655350);
        // ???
        aInfoOut.write((byte)0xEF);
        aInfoOut.write((byte)0xBB);
        aInfoOut.write((byte)0xBF);

        StringBuffer tmpBuf = getStringBuffer();
        tmpBuf.setLength(0);
        tmpBuf.append("<?xml version=\"1.0\" encoding=\"utf-8\"?><IndexContainer xmlns=\"http://www.cuttermaran.de\"><Version>1.61</Version>");

        dumpInfo(tmpBuf);
    }

    /**
     * @param newName
     */
    public void renameVideoInfoTo(String newName)
	{
        File nname = new File(newName + ".info");

        if (nname.exists())
            nname.delete();

        Common.renameTo(new File(aInfoName), nname);

        // 2 Minuten - 120000ms (?) für CM dazuschummeln ;-)
        nname.setLastModified(nname.lastModified() + 120000);
    }
   
    private void dumpInfo(StringBuffer inpBuf) throws IOException
	{
        int len = (inpBuf == null) ? 0 : inpBuf.length();

        for (int i = 0; i < len; i++)  // for i
            aInfoOut.write((byte) inpBuf.charAt(i));
    }

    /**
     * @throws IOException
     */
    private synchronized void closeInfo() throws IOException
	{
        StringBuffer tmpBuf = getStringBuffer();
        tmpBuf.setLength(0);

        if (!aInfoSeqEnd)
		{
            tmpBuf.append("<SeqEnd adr=\"");
            tmpBuf.append(pos);
            tmpBuf.append('\"');
            tmpBuf.append(" />");
        }

        tmpBuf.append("</IndexContainer>");
        dumpInfo(tmpBuf);

        aInfoOut.flush();
        aInfoOut.close();
        aInfoOut = null;

        aInfoSeqEnd = false;

        setBitWalker(null);
        setStringBuffer(null);
    }
    
    /**
     * 
     */
    public void deleteInfo()
	{
        new File(aInfoName).delete();
    }
    
    /**
     * Write .info for CM
     * 
     * @param b
     * @param off
     * @param len
     * @throws IOException
     */
    private synchronized void writeInfo(byte b[], int off, int len) throws IOException
	{
        if (aInfoOut == null)
            return;

        for (int a = off, ret; a < off + len - 3; a++)
		{
            if ((ret = CommonParsing.validateStartcode(b, a)) < 0)
			{
                a += (-ret) - 1;
                continue;
            }

            StringBuffer tmpBuf;

            if ((0xFF & b[a + 3]) == 0xB3)
			{
                int ratio = 0xF & b[a + 7] >>> 4;

                tmpBuf = getStringBuffer();
                tmpBuf.setLength(0);
                tmpBuf.append("<Seq adr=\"");
                tmpBuf.append(pos + a - off);
                tmpBuf.append('\"');
                tmpBuf.append(" ratio=\"");
                tmpBuf.append(ratio);
                tmpBuf.append('\"');
                tmpBuf.append(" />");
                dumpInfo(tmpBuf);

                a += 12;
            }

			else if ((0xFF & b[a + 3]) == 0xB7)
			{
                tmpBuf = getStringBuffer();
                tmpBuf.setLength(0);
                tmpBuf.append("<SeqEnd adr=\"");
                tmpBuf.append(pos + a - off);
                tmpBuf.append('\"');
                tmpBuf.append(" />");
                dumpInfo(tmpBuf);

                aInfoSeqEnd = true;

                a += 3;
            }

			else if ((0xFF & b[a + 3]) == 0xB8)
			{
                tmpBuf = getStringBuffer();
                tmpBuf.setLength(0);
                tmpBuf.append("<GOP adr=\"");
                tmpBuf.append(pos + a - off);
                tmpBuf.append('\"');
                tmpBuf.append(" />");
                dumpInfo(tmpBuf);

                a += 7;
            }

			else if (b[a + 3] == 0)
			{
                tmpBuf = getStringBuffer();
                tmpBuf.setLength(0);
                tmpBuf.append("<Pic adr=\"");
                tmpBuf.append(pos + a - off);
                tmpBuf.append('\"');

                int tref = (3 & b[a + 5] >>> 6) | (0xFF & b[a + 4]) << 2;

                tmpBuf.append(" tempRef=\"");
                tmpBuf.append(tref);
                tmpBuf.append('\"');

                int typ = 7 & b[a + 5] >>> 3;

                tmpBuf.append(" type=\"");
                tmpBuf.append(typ);
                tmpBuf.append('\"');

                // picture_structure
                BitWalker tmpWalker = getBitWalker();
                tmpWalker.setBuf(b, a + 3);

                int struct = tmpWalker.getPictureStructure(true);

                if (struct != BitWalker.FRAME_PICTURE)
				{
                    tmpBuf.append(" struct=\"");
                    tmpBuf.append(struct);
                    tmpBuf.append('\"');
                }

                tmpBuf.append(" />");
                dumpInfo(tmpBuf);

                a += 8;
            }
        } // for a

    }

    private BitWalker getBitWalker()
	{
        if (aBitWalker == null)
            aBitWalker = new BitWalker();

        return aBitWalker;
    }

    private void setBitWalker(BitWalker inpBitWalker)
	{
        aBitWalker = inpBitWalker;
    }

    private StringBuffer getStringBuffer()
	{
        if (aStringBuffer == null)
            aStringBuffer = new StringBuffer();

        return aStringBuffer;
    }

    private void setStringBuffer(StringBuffer inpStringBuffer)
	{
        aStringBuffer = inpStringBuffer;
    }

    // cminfo ------ END ------

}
