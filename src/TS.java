/*
 * @(#)TS.java - constants to create TS packets
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

public class TS
{

static byte[] TFhead = {
	(byte)0xCD,0x39,0xc,0,
	(byte)0xCD,0x39,0xc,0,
	0,0x3c,
	0,0x1f,
	0,0,
	5,
	0,
	6,(byte)0xb0,
	1,2,
	1,0,
	0,(byte)0xe0,
	0,(byte)0xe0,
	(byte)0x80,(byte)0xc0,
	0x4D,0x79,0x20,0x70,0x65,0x72,0x73,0x6F,0x6E,0x61,0x6C,0x20,0x54,0x56,0x20,0x43,0x68,0x61,0x6E,0x6E,0x65,0x6C,0,0,
	5,
	0,
	0x30,(byte)0xc0,
	0x6b,0x6c,
	0,1,

	0x40,0x1f,
	1,1,
	(byte)0xCD,0x39,0xb,0,
	(byte)0xCD,0x39,0xc,0,
	0,0x3c,
	4,
	4,
	84,69,83,84,
	0,0,0,0,0,0,0,0,0,0,0,0,0,0,
	0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
	0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
	0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
	0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
	0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
	0,0,0,0,0,0,0,0,0,0,0,0,

	1,2
};

static byte[] pmt1 = { 
	0x47,0x41,0,0x10,
	0, 2, (byte)0xB0, (byte)0x95, 1, 2,(byte)0xC1, 0, 
	0, (byte)0xE0, (byte)0xE0, (byte)0xF0, 0,
	2, (byte)0xE0, (byte)0xE0, (byte)0xF0, 3, 0x52, 1, 1, 
	3, (byte)0xE0, (byte)0xc0, (byte)0xF0, 0x9, 0x52, 1, 3, 0xA, 4, 0x64, 0x65, 0x75, 1, 
	3, (byte)0xE0, (byte)0xc1, (byte)0xF0, 0x9, 0x52, 1, 4, 0xA, 4, 0x64, 0x65, 0x75, 1, 
	3, (byte)0xE0, (byte)0xc2, (byte)0xF0, 0x9, 0x52, 1, 5, 0xA, 4, 0x64, 0x65, 0x75, 1, 
	3, (byte)0xE0, (byte)0xc3, (byte)0xF0, 0x9, 0x52, 1, 6, 0xA, 4, 0x64, 0x65, 0x75, 1, 
	6, (byte)0xE0, (byte)0x80, (byte)0xF0, 0xC, 0x52, 1, 0x11, 0x6A, 1, 0, 0xA, 4, 0x64, 0x65, 0x75, 0, 
	6, (byte)0xE0, (byte)0x81, (byte)0xF0, 0xC, 0x52, 1, 0x12, 0x6A, 1, 0, 0xA, 4, 0x64, 0x65, 0x75, 0, 
	6, (byte)0xE0, (byte)0x82, (byte)0xF0, 0xC, 0x52, 1, 0x13, 0x6A, 1, 0, 0xA, 4, 0x64, 0x65, 0x75, 0, 

	6, (byte)0xE0, (byte)0x90, (byte)0xF0, 0x10,0x52, 1, (byte)0xC2, 0x56, 5, 0x65, (byte)0x6E, 0x67, 0x9, 0, 0xA, 4, 0x64, 0x65, 0x75, 0,
	(byte)0x85, (byte)0x33, (byte)0x49, (byte)0x7e
};

static byte[] pcr = new byte[188];
static byte[] pat = new byte[188];
static byte[] pmt = new byte[188];
static byte[] head = new byte[512+52];
int count1=0, count2=0, count3=0;

static byte[] pmtHead = { 0x47,1,0,0x10 };
static byte[] pmtStart = { 0, 2, (byte)0xB0, 0, 1, 2,(byte)0xC1, 0 };
static byte[] pmtPCR =  { 0, (byte)0xE0, (byte)0xE0, (byte)0xF0, 0 };
static byte[] pmtMPV =  { 2, (byte)0xE0, (byte)0xE0, (byte)0xF0, 3, 0x52, 1, 1 };
static byte[] pmtMPA =  { 3, (byte)0xE0, (byte)0xC0, (byte)0xF0, 0x9, 0x52, 1, 3, 0xA, 4, 0x64, 0x65, 0x75, 1 };
static byte[] pmtAC3 =  { 6, (byte)0xE0, (byte)0x80, (byte)0xF0, 0xC, 0x52, 1, 4, 0x6A, 1, 0, 0xA, 4, 0x64, 0x65, 0x75, 0 };
//static byte[] pmtTTX =  { 6, (byte)0xE0, (byte)0x90, (byte)0xF0, 0x10,0x52, 1, 5, 0x56, 5, 0x65, (byte)0x6E, 0x67, 0x9, 0, 0xA, 4, 0x64, 0x65, 0x75, 0 };
//DM10032004 081.6 int18 changed for linked sub-pages
static byte[] pmtTTX =  { 6, (byte)0xE0, (byte)0x90, (byte)0xF0, 0x1F, 0x52, 1, 5, 0x56, 20, 0x65, 0x6E, 0x67, 0x9, 0, 0x64, 0x65, 0x75, 0x11, 0x50, 0x67, 0x65, 0x72, 0x17, 0x77, 0x65, 0x6E, 0x67, 0x10, (byte)0x88, 0xA, 4, 0x64, 0x65, 0x75, 0 };
static byte[] autopmt = new byte[0];
static int firstID = 0xE0;
static boolean myTTX=false;

public void setPmtPids(java.util.ArrayList PIDs) throws java.io.IOException {
	if (myTTX) 
		PIDs.add(""+0x39F);

	Object[] Pids = PIDs.toArray();
	if (Pids.length==0) {
		X.Msg("-> no IDs found! ..use fixed PMT");
		autopmt=pmt;
		return;
	}

	java.io.ByteArrayOutputStream pmtout = new java.io.ByteArrayOutputStream();
	java.util.Arrays.sort(Pids);
	int lfn = 1;  // byte 7 = substreamID for program component

	pmtout.write(pmtStart);
	firstID = (0xFF&Integer.parseInt(Pids[0].toString()));
	TFhead[23]=pmtPCR[2]=(byte)firstID;
	pmtout.write(pmtPCR);

	for (int a=0;a<Pids.length;a++) {   // get Pid Hex: 0..=V, 1..=MPA, 2..=AC3, 3..=TTX
		int Pid = Integer.parseInt(Pids[a].toString());
		switch (0xF&(Pid>>>8)) {
		case 0: {   // vid
			pmtMPV[2] = (byte)(0xFF&Pid);
			pmtMPV[7] = (byte)lfn++;
			pmtout.write(pmtMPV);
			break;
		}
		case 1: {   // mpeg-1 (-2) audio
			pmtMPA[2] = (byte)(0xFF&Pid);
			pmtMPA[7] = (byte)lfn++;
			pmtout.write(pmtMPA);
			break;
		}
		case 2: {   // ac3 audio
			pmtAC3[2] = (byte)(0xFF&Pid);
			pmtAC3[7] = (byte)lfn++;
			pmtout.write(pmtAC3);
			break;
		}
		case 3: {   // ttx
			pmtTTX[2] = (byte)(0xFF&Pid);
			pmtTTX[7] = (byte)lfn++;
			pmtout.write(pmtTTX);
			break;
		}
		}
	}

	byte[] newpmt = pmtout.toByteArray();
	int sectionlen = newpmt.length;
	newpmt[2] = (byte)(0xB0 | (0xF&sectionlen>>8));
	newpmt[3] = (byte)(0xFF&sectionlen);
	pmtout.reset();
	pmtout.write(newpmt);
	pmtout.write(CRC.generateCRC32(newpmt, 1)); //DM10042004 081.7 int01 changed
	newpmt = pmtout.toByteArray();

	int pmtpacks = ((newpmt.length-1)/184)+1; // = number of needed pmt packs
	autopmt = new byte[pmtpacks*188];
	java.util.Arrays.fill(autopmt,(byte)0xff);

	int i=0, c=0;
	while (i<newpmt.length) {
		System.arraycopy(pmtHead,0,autopmt,c,4);
		if (newpmt.length >= i+184) { 
			System.arraycopy(newpmt,i,autopmt,4+c,184);
			i+=184; 
			c+=188;
		} else {
			System.arraycopy(newpmt,i,autopmt,4+c,newpmt.length-i);
			break;
		}
	}
	autopmt[1] = 0x41; // = set startpacket bit
	pmtout.close();
}

/** auto PMT **/
public byte[] getAutoPmt() { 
	for (int i=0; i<autopmt.length; i+=188) 
		autopmt[i+3] = (byte)(0x10 | (0xf&(count1++)));
	return autopmt;
}

public int getfirstID() { 
	return firstID; 
}

public void setfirstID() { 
	firstID=0xE0; 
	TFhead[23]=(byte)firstID; 
	pmtPCR[2]=(byte)0xE0; 
}

/** PAT with section 0 and SID = 0x102, PMT = 0x100 , CRC32 **/
static byte[] pat1 = {
	0x47,0x40,0,0x10,
	0, 0, (byte)0xB0, 0xd, 0, 1, 1, 0, 0, 1, 2, (byte)0xE1, 0,
	(byte)0x8f, (byte)0xa5, 0x26, (byte)0xcf,
};

/** counter shall not be updated in PCR only paket (but do it), 42bits for PCR **/
static byte[] pcr1 = {
	0x47,0,(byte)0xe0,0x20,
	(byte)0xB7,0x10,0,0,0,0,0,0
};

static byte[] ttx = {
	0x47,0x40,(byte)0x9F,0x10,
	0,0,1,(byte)0xBD,0,(byte)0xB2,(byte)0x84,(byte)0x80,0x24,
	(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,
	(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,
	(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,
	(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,
	(byte)0xFF,
	0x10,
	// 022C E7E4 40A8 A8CE A80B A80B 7A40
	2,44,-25,-28,64,-88,-88,-50,-88,11,-88,11,122,64,
	38,-50,117,87,-122,79,4,42,-53,-75,-110,118,103,-9,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,
	// 022C E8E4 40D9
	2,44,-24,-28,64,-39,
	//    l  i n  e    2 2  :
	4,55,-105,118,-89,4,76,76, 4,107,67,-110,4,-116,-12,28,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,
	// 022C E9E4 E3D9
	2,44,-23,-28,-29,-39,
	//    line 23 PTS
	4,55,-105,118,-89,4,76,-51, 4,107,67,-110,4,-116,-12,-99,4,11,42,-53,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4
};

public byte[] getTTX(TeleText ttc, byte[] data, String pts) {
	byte[] tPTS = pts.getBytes();
	for (int a=0; a<tPTS.length; a++) 
		tPTS[a] = ttc.bytereverse(ttc.parity(tPTS[a]));

	System.arraycopy(tPTS,0,ttx,169,tPTS.length);
	System.arraycopy(data,9,ttx,13,5);
	ttx[13] &= ~0x10;
	ttx[3] = (byte)(0x10 | (0xf&(count3++)));
	return ttx;
}

public void init(String name, boolean ac3, boolean myTTX) { 
	count1=count2=count3=0;
	this.myTTX = myTTX;
	java.util.Arrays.fill(pat,(byte)0xff);
	java.util.Arrays.fill(pmt,(byte)0xff);
	java.util.Arrays.fill(pcr,(byte)0xff);

	System.arraycopy(TFhead,0,head,0,TFhead.length);
	System.arraycopy(pmt1,0,pmt,0,pmt1.length);
	System.arraycopy(pat1,0,pat,0,pat1.length);
	System.arraycopy(pcr1,0,pcr,0,pcr1.length);

	byte[] nam = new java.io.File(name).getName().getBytes();
	head[75]=(byte)(nam.length-3);
	System.arraycopy(nam,0,head,76,nam.length-3);

	if (ac3) {
		head[26]=0; 
		head[27]=(byte)0x80;  // set 1. AC3 PID as main TFaudio
	} else {
		head[26]=(byte)0x80; 
		head[27]=(byte)0xC0; 
	}
}

public byte[] getHead1() { 
	return head; 
}

public byte[] getPmt() { 
	pmt[3] = (byte)(0x10 | (0xf&(count1++))); 
	return pmt; 
}

public byte[] getPat() { 
	pat[3] = (byte)(0x10 | (0xf&(count2++))); 
	return pat; 
}

public byte[] getPCR(long pts, int count, int PCRPid) {
	/* Construct the PCR, PTS-55000 (2ms) 1Bit ~ 3Ticks (counter) */
	pcr[2] = (byte)(PCRPid);
	pcr[3] = (byte)(0x20 | (0xF & count));
	pcr[6] = (byte)(0xFF & pts>>>25);
	pcr[7] = (byte)(0xFF & pts>>>17);
	pcr[8] = (byte)(0xFF & pts>>>9);
	pcr[9] = (byte)(0xFF & pts>>>1);
	pcr[10] = (byte)((1 & pts) << 7 );
	/* PCR ext is 0, byte10+byte11 */
	return pcr;
}

}