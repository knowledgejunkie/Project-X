/*
 * @(#)SUBPICTURE.java - creates SUP file to use as DVD subtitles
 *
 * Copyright (c) 2008, 2009 by Duncan Fenton (Shannock9), All Rights Reserved.
 *
 * This file is addendum to ProjectX, a free Java based demux utility.
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
 * thanks to mpucoder for his very helpful description of the CHG_COLCON cmd
 *
 *      http://rmworkshop.com/dvd_info/subpictures/spu.html#setcolcon
 *
 * ...and of course to dvb matt and others who have contributed to ProjectX
 *
 *      "...because I have stood on the shoulders of giants"
 *                                                            Shannock9
 */

/*
		20090116d	--------get all tables by analysing (UK) target clut--------
		20090116d   purged a lot of (more than usually) temporary code and retested
		20090116c   tested for quant sparse >in TCT< - may fail if sparse >in UCT<
		20090116b   "S9 40" converts existing code to use >>grads<< derived from TCT
		20090116b   "S9 40" converts existing code to use >quants< derived from TCT
		20090116b   quant manager to recognise new/old hues and allocate quant #s

		20090116a	-------prepare to get tables by analysing target clut-------
		20090116a   eliminate all hardcoded constants for special quants & ranges
		20090116a   remove dbgRvLum (old 'sort on inverted lum' method for blu/red)
		20090116a	rev video (lite grnd) on any color using double size grad table
		
		20090114	-----successfully analyse clstrs etc from imported clut-----
		20090114    generate hue+lum lists, allocate to clstrs, generate grads
		20090114    ...including dark/lite fill when < 4 TCT entries available 
		
		20090113	---get DE samples working again after transp grnd changes---
		20090113    general cleanup of interfaces to other ProjectX modules
		20090110    corrected conversion of red shades in ARGBtoQLHI()
		20090110    fewer cases for 'pixel not allowed' now dark grnd ok with any clr
		20090110    corrected sweep for lite pixels: only white can be be lite
		
		20090108	---get UK samples working again after transp grnd changes---
		20090108c	Rev video by adjust color indexes; any clr can be dark/lite grnd
		20090108b	Add dbgTransp "S9 80" to force trnsp bg (except reverse video) 
		20090108a	Collection with transp grnd corrected; tested with ZDF and UK
		20090108a	Count textblks & textgaps separately to decide if variable rh
		20090108a	>>Add bitmap x offset into PX_CTLI column offsets (544x576)<<
		20090108a	Replace cyan with grey in clut_eepb[blue] (was D980 now DC80) 
		
		20090106	---attempt to handle ZDF samples with transp grnd to text---
		20090106	Scan for textblks <= ZDF doesn't follow geometrical rh cells
		20060106	1st try for 'smearing' of transp grnd columns to reduce # areas


*/
package net.sourceforge.dvb.projectx.subtitle;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.TreeSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Hashtable;

import net.sourceforge.dvb.projectx.common.Common;


public  class ColorAreas extends Object {                                                        //S9

	/*===================================== switches and debug =====================================*/

	//To activate this code: IRD colormodel = "256 colors" AND 1st two chars of fix to pageID = "S9"

	public  static boolean active = false;                 //MAIN SWITCH multicolor DVB to SUP   //S9

	private static int     switches = 0;                   //OPTIONS     multicolor DVB to SUP   //S9
	public  static int     getSwitches()           { return switches;     }                      //S9
	public  static void    setSwitches(String s)   { switches=Integer.parseInt(s,16);} // HEX!!  //S9

	public  static boolean dbgSub(int i)  { return i<=(0xF & (switches     ));} //input hex!!    //S9
	private static boolean dbgSpare0()    { return 0!=(0x1 & (switches>>> 4));} //unassigned     //S9 20090109
	private static boolean dbgSpare1()    { return 0!=(0x1 & (switches>>> 5));} //unassigned     //S9 20090116
	private static boolean dbgDynQnt()    { return 0!=(0x1 & (switches>>> 6));} //dynamic quants //S9 20090116
	private static boolean dbgTrnsp()     { return 0!=(0x1 & (switches>>> 7));} //all 'b' transp //S9 20090105
	private static boolean dbgnoCHG()     { return 0!=(0x1 & (switches>>> 8));} //for SupViewer  //S9
	private static boolean dbgnofix()     { return 0!=(0x1 & (switches>>> 9));} //off->fixup OK  //S9
	private static boolean dbg1pic()      { return 0!=(0x1 & (switches>>>10));} //on->chk pic#   //S9
	private static boolean dbg1line()     { return 0!=(0x1 & (switches>>>11));} //on->check L#   //S9
	public  static int     dbgpic()        //turning off the control bit forces return of -1     //S9
		 { return (0!=(0x1 & (switches>>>10))) ? (0xFFF & (switches>>>12)) : -1;} //interlaced!! //S9
	public  static int     dbgline()       //turning off the control bit forces return of -1     //S9
		 { return (0!=(0x1 & (switches>>>11))) ? (0xFFF & (switches>>>12)) : -1;} //interlaced!! //S9

	public  static boolean biglog = false;       //exposed because the original should have been //S9

	public  static void  initialise(int IRD, boolean log, Hashtable table)                       //S9 20090113
	{                                                       //from DVBSubpicture.setIRD()        //S9 20090113
		active = false;                                     //reset main switch                  //S9 20090102
		biglog = log;                                       //tru iff debug log requested by GUI //S9
		picCount = 0;                                       //reset static call counter          //S9
		supCount = 0;                                       //reset static sup file byte index   //S9

		switches = switches & 0xFFFFFFF0 | (log?  0:0);     //default for dbgSub                 //S9
		switches = switches & 0xFFFFFE0F | (log?  0:0)<< 4; //default for dbgSpare0              //S9 20090109
		switches = switches & 0xFFFFFD0F | (log?  0:0)<< 5; //default for dbgSpare1              //S9 20090116
		switches = switches & 0xFFFFFBFF | (log?  0:0)<< 6; //default for dbgDynQnt              //S9 20090116
		switches = switches & 0xFFFFF7FF | (log?  0:0)<< 7; //default for dbgTrnsp               //S9 20090105
		switches = switches & 0xFFFFFEFF | (log?  0:0)<< 8; //default for dbgnoCHG               //S9
		switches = switches & 0xFFFFFDFF | (log?  0:0)<< 9; //default for dbgnofix               //S9
		switches = switches & 0xFFFFFBFF | (log?  0:0)<<10; //default for dbg1pic                //S9
		switches = switches & 0xFFFFF7FF | (log?  0:0)<<11; //default for dbg1line               //S9
		switches = switches & 0xFF000FFF | (log?  0:0)<<12; //default for dbgpic/dbgline         //S9

		//get hidden debug key from ini entry                                                    //dm 20090113
		String dbg = Common.getSettings().getProperty("Subpicture.S9Debug", "").trim();          //dm 20090113

		if ((IRD & 1) == 1)                                                 //MC identifier      //dm 20090113
		{                                                                                        //S9
			active = true;                             //activate multicolor mapping DVB to SUP  //S9
			if (dbg.length()>0) { setSwitches(dbg); }  //capture any options following the "S9 " //S9
			setClut(table);                  // always refreshed, requires full set of 16 colors //dm 20090113
		}                                                                                        //S9

Common.setMessage("Multicolor "+((active)?"ACTIVE":"OFF")+" / switches "+X(8, switches)          //S9
		+" / hooked >"+dbg);                                                                     //dm 20090113
if (active) Common.setMessage("Multicolor:"                                                      //S9 20090109
		+" shw1Line="+dbgline()+" shw1Pic="+dbgpic()+" noRepairs="+dbgnofix()    //#, #, Boolean //S9
		+" NoCOLCON="+dbgnoCHG()+" TrnspBgrd="+dbgTrnsp()+" DynQnt="+dbgDynQnt()                 //S9 20090116
		);  
	}                                                                                            //S9

	//following rigmarole is because string.format/printf is theologically unacceptable here     //S9
	public static String d(int width, int val)                        //printf("%0 width d",val) //S9
		{ String s = "0000000000" + val; if (val<0) s = "          " + val;                      //S9 20090114
		                                 return s.substring(s.length()-width); }                 //S9 20090114
	public static String s(int width, String val)                     //printf("%  width s",val) //S9
		{ String s = "          " + val; return s.substring(s.length()-width); }                 //S9
	public static String X(int width, int val)                        //printf("%0 width X",val) //S9
		{ String s = "0000000000" + Integer.toHexString(val).toUpperCase();                      //S9
		                                 return s.substring(s.length()-width); }                 //S9


	/*======================== load TARGET CLUT (TCT) and elicit structure ========================*/

			//We will be using PGC CLUT(s) optimised for national DVB subtitles.  We             //S9
			//won't be directly using the stream colors but will map them - using very           //S9
			//simple feature detection >in color space< - to the loaded target table (TCT).      //S9
			

	public  static Integer[] clut_pgc = new Integer[16];      //will be filled by user           //dm 20090113
	private static int tables_rdy = 0;                        //has main init been done?         //S9i20090114

	private static void setClut(Hashtable table)              //basic static init from setIRD    //dm 20090113
	{                                                         //rest from 1st call to analyse    //S9i20090114
		for (int i = 0; i < 16; i++)                                                             //dm 20090113
		  if (table.containsKey("" + i))                                                         //dm 20090113
			clut_pgc[i] = new Integer(Integer.parseInt(table.get("" + i).toString().trim(), 16));//dm 20090113
		  else                                                                                   //S9i20090114
			Common.setMessage("Multicolor color table missing entry # "+d(2, i));                //S9i20090114
		  tables_rdy = 0;                                     //remind analyse() to finish init  //S9i20090114		  
	}                                                                                            //dm 20090113

			//Here we analyse the TCT to find the clusters in color space ('quants').            //S9i20090116
			//These are recognised, and the quant #s managed, by code in ARGBtoQLHI().           //S9i20090116
			
			//Each color area will encompass a gradient between one clstr color and one pure     //S9i20090116
			//neutral ('grnd') either black or white.  These grnds have special (low) quant #s.  //S9i20090116
			//THE GRND IS THE GRADIENT NEUTRAL, NOT ALWAYS THE VISUAL BACKGROUND OF THE AREA     //S9i20090116
			
			//The 4 TCT indices which define the gradient to 21pb for the DVD subpic are here    //S9i20090116
			//called a 'grad'. We now prepare suitable grads, indexed by quant, for both grnds.  //S9i20090116

		/*-----------hue cluster analysis tables, field layouts, and constants-----------*/      //S9i20090114
//	private static int[] TCT_rawNHL = new int[16];  //clut neutral flag, hue, luminance          //S9i20090114
//	private static int[] TCT_analys = new int[16];  //clut analysed and SORTED by clstr+lum      //S9i20090114
	private static int[] TCTQ_grads = new int[32];  //by clstr: TCT indices (both dk & lt grnd)  //S9i20090114
	private static int[] TCTQ_steps = new int[16];  //by clstr: holds corresponding lum values   //S9i20090114

	
	//  TCT_rawNHL[i] = (neutral&0x1)<<20 | (hue&0xFF)<<8 | (lum&0xFF);  //neutflag | hue | lum  //S9i20090114
	//  TCT_analys[i] = tmp | (q&0xF)<<20 | (lum&0xFF)<<8 |   (i&0xFF);  // clstr # | lum | TCTi //S9i20090114

	//to support hue+lum comparisons
	private static int rawN(int nhl)  { return (nhl>>16)& 0x01; }     //1 = hue is neutral       //S9i20090116
	private static int rawH(int nhl)  { return (nhl>> 8)& 0xFF; }     //hue from clut entry      //S9i20090116
	private static int rawL(int nhl)  { return (nhl    )& 0xFF; }     //lum from clut entry      //S9i20090116
	private static int rawNHL(int neutralflag, int hue, int lum)                                 //S9i20090116
		                     { return (neutralflag&0x1)<<16 | (hue&0xFF)<< 8 | (lum&0xFF); }     //S9i20090116  

	//clstr analysis for one TCT entry                                                           
//	private static int anlC(int anl)  { return (anl>>24)& 0x0F; }     //clstr members max 15     //S9i20090116
//	private static final int ANLC_INCR = 1<<24;                       //to incr member counter   //S9i20090116
//	private static int anlQ(int anl)  { return (anl>>16)& 0xFF; }     //clstr # (aka quant)      //S9i20090116
//	private static int anlL(int anl)  { return (anl>> 8)& 0xFF; }     //lum from TCT             //S9i20090116
//	private static int anlT(int anl)  { return (anl    )& 0xFF; }     //TCT index max 255        //S9i20090116
//	private static int analys(int count, int quant, int lum, int TCTi)                           //S9i20090116
//		       { return (count&0xF)<<24 | (quant&0xFF)<<16 | (lum&0xFF)<< 8 | (TCTi&0xFF); }     //S9i20090116

	//constants for clstr # (also known as quant, from 'quantised hue')
	/*******************************************************************************************///S9i20090116
	/*   ANY OF THESE Q-CONSTANTS MAY BE USED FOR RANGE CHECKS WHICH ASSUME CORRECT ORDERING   *///S9i20090116
	/*                    (the values can be changed but not the sequence)                     *///S9i20090116
	/*******************************************************************************************///S9i20090116
	private static final int QX = 0;              //null    (should not occur and will show red) //S9i20090116 =+=
	private static final int QT = 1;              //transparent   (only pixels are trnsp; areas) //S9i20090114 =+=
	private static final int QD = 2;              //dark          (....are dark with trnsp grnd) //S9i20090114 =+=
	private static final int QL = 3;              //lite         (hi white, as grnd for a color) //S9i20090114 =+=
	private static final int QN = 4;              //neutral             (white->grey as a color) //S9i20090114 =+=
	private static final int Q1 = 5;              //1st color  (dynamic alloc from here upwards) //S9i20090114 =+=
	
	private void analyseTCT()                                                                    //S9i20090114
	{                                                                                            //S9i20090114
		if (tables_rdy!=0) return;                  //we are only supposed to be called once     //S9i20090116 

		/*-------------analyse TCT entries by hue & lum and allocate clstr #-------------*/      //S9i20090114
		QLHI[] TCT_quants = new QLHI[16];                           //not retained after init    //S9i20090116
		int[]  TCTQ_count = new int[16];                            //# shades alloc to clstr    //S9i20090116
		for (int i = 0; i < 16; i++)             //ask manager for quants for every TCT entry    //S9i20090116
			TCT_quants[i] = ARGBtoQLHI(clut_pgc[i].intValue(),i,1); //...so Q#s are allocated    //S9i20090116
		Arrays.sort(TCT_quants, QLHI_ORDER);     //sort by clstr # + luminance + TCT index       //S9i20090116
		
		/*----------------collect TCT indices into grads for each clustr #---------------*/      //S9i20090114
		//-------------------also collect lum values (not used as yet)-------------------*/      //S9i20090114
		int topWhiteNdx = -1;       //TCT_quants index of lite grnd (index of dark is always 0)  //S9i20090116
		for (int i=15, q=0, t=0; i>=0; i--)      //down analysed cluts collecting for clusters   //S9i20090114
		{                                                                                        //S9i20090114
if (dbgSub(5)) System.out.println("init collect1"                                                //S9i20090114
			+" / TCT_quants[i="+d(2,i)+"]="+X(8,TCT_quants[i].N));                               //S9i20090114
			
			q = TCT_quants[i].quant();                                   //assigned quant        //S9i20090116
			t = TCT_quants[i].inUCT();                                   //original index        //S9i20090116
			TCTQ_grads[q] = TCTQ_grads[q]<<4 | t;                        //collect indices       //S9i20090116
			TCTQ_steps[q] = TCTQ_steps[q]<<8 | TCT_quants[i].lum();      //collect lum vals      //S9i20090116
			TCTQ_count[q]++;                                             //count clstr members   //S9i20090116
			
			if (q == QN && topWhiteNdx < 0) { topWhiteNdx = i; }   //keep index of top neutral   //S9i20090116

if (dbgSub(4)) System.out.println("init collect2"                                                //S9i20090114
			+" / TCT_quants[i="+d(2,i)+"]="+X(8,TCT_quants[i].N)                                 //S9i20090114
			+" / TCTQ_count[q="+d(2,q)+"]="+X(1,TCTQ_count[q])                                   //S9i20090116
			+" / TCTQ_grads[q]="+X(8,TCTQ_grads[q])                                              //S9i20090116
			+" / TCTQ_steps[q]="+X(8,TCTQ_steps[q])                                              //S9i20090116
			+" TCT_CLUT[t="+d(2,t)+"]="+X(8,clut_pgc[t].intValue())                              //S9i20090116
			);                                                                                   //S9i20090114
		} //end for sorted analysed clut entries                                                 //S9i20090114
		

		/*----------fill missing TCT indices making grads for dark & lite grnds----------*/      //S9i20090114
		//whitefill is temp version until the neutral grad has been built (before any colors)    //S9i20090114
		//notice only two bits ever needed (any clstr with only 1 color we will use it twice)    //S9i20090114
		int whitefill = TCT_quants[topWhiteNdx].inUCT()<<8; whitefill |= whitefill<<4; //ee00    //S9i20090116
		int blackfill = TCT_quants[          0].inUCT()   ; blackfill |= blackfill<<4; //00pb    //S9i20090116
		
		for (int i=0, q =0; q < 16; q++) //thru clusters filling grads with lite/dark indices    //S9i20090114
		//This doesn't maintain TCTQ_steps[] - do that later when we decide what it's for :)     //S9i20090114
		//This also MAY need tweaking for black outline style (or we might override the grad)    //S9i20090114
		//For the fill logic, note we'll be doing dark then lite then neutral before any colors  //S9i20090114
		{                                                                                        //S9i20090114
			switch (i = TCTQ_count[q])               //how many indices in clstr?                //S9i20090116
			{                                                                                    //S9i20090114
			  case 0:                                //empty table entries leave them alone      //S9i20090114
				break;                                                                           //S9i20090114
			  case 1:                                //duplicate the one we have, then fill      //S9i20090114
				TCTQ_grads[q   ] = TCTQ_grads[q]<<4  |  TCTQ_grads[q]&0x0F; //duplicate b->p     //S9i20090114
				TCTQ_grads[q+16] = TCTQ_grads[q]     |   whitefill &0xFF00; //fill 2 white ee    //S9i20090114
				TCTQ_grads[q   ] = TCTQ_grads[q]<<8  |   blackfill &0x00FF; //fill 2 black pb    //S9i20090114
				break;                                                                           //S9i20090114
			  case 2:                                //lite: duplicate the upper lum color       //S9i20090114
				TCTQ_grads[q+16] = TCTQ_grads[q]<<4 &0xF00 | TCTQ_grads[q]; //duplicate p->e1    //S9i20090114
				TCTQ_grads[q+16] = TCTQ_grads[q+16]  |   whitefill &0xF000; //fill 1 white e2    //S9i20090114
				if (TCT_quants[q].lum() > 0x60)      //dark: darker clr poor blend->dup upper    //S9i20090114
					TCTQ_grads[q] = TCTQ_grads[q]>>4 | TCTQ_grads[q] &0xF0; //duplicate p->b     //S9i20090114
				TCTQ_grads[q   ] = TCTQ_grads[q]<<8  |   blackfill &0x00FF; //fill 2 black pb    //S9i20090114
				break;                                                                           //S9i20090114
			  case 3:                                //just fill with appropriate grnd           //S9i20090114
				TCTQ_grads[q+16] = TCTQ_grads[q]     |   whitefill &0xF000; //fill 1 white e2    //S9i20090114
				TCTQ_grads[q   ] = TCTQ_grads[q]<<4  |   blackfill &0x000F; //fill 1 black b     //S9i20090114
				break;                                                                           //S9i20090114
			  default:                               //more than 3 means table or logic error    //S9i20090114
Common.setMessage("Multicolor CLUT has "+d(2,i)+" entries same hue as "+d(2,TCTQ_grads[q]&0x0F));//S9i20090114

			} //end switch                                                                       //S9i20090114
			if (q == QN)  { whitefill = TCTQ_grads[q]; }  //better version to fill the colors    //S9i20090114

if (dbgSub(3)) System.out.println("init grads /"                                                 //S9i20090114
			+" TCT_CLUT[t="+d(2,q)+"]="+X(8,clut_pgc[q].intValue())                              //S9i20090114
			+" | TCT_quants[i="+d(2,q)+"]="+X(8,TCT_quants[q].N)+" |"                            //S9i20090116
			+" Q_count[q="+d(2,q)+"]="+X(5,TCTQ_count[q])                                        //S9i20090116
			+" grads[q]="+X(5,TCTQ_grads[q   ])                                                  //S9i20090114
			+" grads[q]="+X(5,TCTQ_grads[q+16])                                                  //S9i20090114
			+" whitefill="+X(5,whitefill)                                                        //S9i20090116
			+" steps[q]="+X(8,TCTQ_steps[q])                                                     //S9i20090114
			);                                                                                   //S9i20090114
		} //end for clusters                                                                     //S9i20090114

		/*---------------finalise tables for use by analyse() & commands()---------------*/      //S9i20090116
		  tables_rdy = 1;                                 //tell analyse() not to call again     //S9i20090114		  
	}                                                                                            //S9i20090114

	/*========================== fixed target PGC_CLUT and mapping tables ==========================*/

			//We will be using a PGC CLUT optimised for national DVB subtitles.  We              //S9
			//won't be directly using the stream colors but will map them - using                //S9
			//very simple feature detection >in color space< - onto the loaded palette.          //S9
	
			//To implement multicolor subtitles on DVD we'll divide the subpic into XY areas     //S9
			//such that each area has one main hue with a few intensities.  We will quantise     //S9
			//the stream colors to ten hue clusters including neutrals, transparents, and        //S9
			//dark & light backgrounds.  Historically the idea was to derive optimum cluster     //S9
			//points *in color space* from the stream itself, but since (for UK DVB) we know     //S9
			//the answers, there's little motivation and I settled for fixed quantisation.       //S9

			//For each subpic there are three key method calls - all in Subpicture.buildRLE()    //S9

			//1. analyse(bitmap, color_table) - quantise color table and find uni-cluster areas  //S9

			//2. pgc_color = ColorAreas.bp12[a + b] - position in bitmap -> 2bit e2e1pb          //S9

			//3. commands(int, int, bitmap) - create the command buffer including a CHG_COLCON   //S9

			//The term "quant" literally refers to a quantised color value.  It's also used	     //S9
			//for the object QHLI which encapsulates the quant and other info about a color      //S9
			//table entry, and therefore about the pixels that refer to that entry.	  Thus       //S9
			//each pixel has a quant, and various areas of the bitmap whose pixels refer to      //S9
			//a single quant can be identified.  Most importantly in a well formed subtitle      //S9
			//subpic (or at least in one text row) each column of pixels refers to a single      //S9
			//quant, and we speak of the quant of the area or say the column 'has' a quant.      //S9

			//All the above usages of "quant" refer to an attribute (of pixel, area, column).    //S9
			//But we also talk of quants as locations in color space - these are values which    //S9
			//the quant attributes may take. Thus we "move some pixels to the dark quant" by     //S9
			//changing the quant attribute of the UCT entries to which they refer. Similarly     //S9
			//we may need to move a light color in the UCT between the lite and white quants.    //S9
			//The purpose of the dark and lite quants will now be explained.                     //S9

			//The clusters appearing frequently on UK DVB (white, yellow, green, cyan) have      //S9
			//a dark background ("dark grnd") but the infrequent blue and red clusters have      //S9
			//a color background with white text. [Magenta has never been seen here, but is      //S9
			//coded like blue & red.]  In all cases we see a luminance gradient of a single      //S9
			//hue, contrasted with either black or white.  For clusters with a white contrast    //S9
			//it proves easiest to say they have a white background ("lite grnd"): the text      //S9
			//uses the lite "background" on a hue clustered "foreground". This allows each       //S9
			//area to need just one color cluster (quant) plus one contrast cluster (grnd).      //S9
			//So "grnd" is a reference neutral (conceptually akin to electrical ground) and      //S9
			//NOT ALWAYS THE VISUAL BACKGROUND. The abstraction is justified by its utility.     //S9

			//A dark or lite grnd is part of every cluster, so >compatible< grnd pixels can      //S9
			//be included in any area.  Dark areas contain only dark pixels, and can be          //S9
			//merged into a neighbor with a dark grnd. There cannot logically be a lite area     //S9
			//since the first pixel will be the visual background, immediately defining the      //S9
			//area's hue cluster. Pixels of any cluster with dark grnd can be added to dark      //S9
			//areas - thus defining that area's hue ("moving the area to that quant").           //S9
			//Depending on context, white can be a hue cluster color (white on black) or a       //S9
			//lite grnd (white on red or blue), which makes for some interesting logic.          //S9
			
/***		---------------------------under review/redesign---------------------------          //S9 20090110
			//For each XY area we assign three subpic RLE values (11,10,01) to three shades      //S9
			//of the single quantised hue; the last value (00) being the black/white grnd.       //S9
			//A transparent visual background is treated as a "dark grnd" with the 'b' pixels    //S9 20090101
			//(RLE=00) being completely transparent; besides the usual rules for a dark area,    //S9 20090101
			//pixels of high transparencies (low alfa) can be included indiscriminately.         //S9 20090101
****		-------------------------------------------------------------------------*/          //S9 20090110
/**/
	private final String[] qNames = { "null", "transp", "dark" , "lite"       //specials         //S9 20090116
			 , "white", "yellow", "green", "cyan", "blue", "magenta", "red"   //color clusters   //S9
			                                                   };             //special          //S9 20090116

	private final int[] clut_eepb = {                                                            //S9

			//The subpic will be divided into areas each with a single main hue                  //S9
			// (the main hues are quantised hence the abbreviation "quant")                      //S9

			//These are the clut_pgc indices (for e2e1pb) to use for each main hue               //S9

/***		---------------------------under review/redesign---------------------------          //S9 20090110
			//The grnd quant (dark or lite) is always assigned to e2e1pb=0.                      //S9
			//The reason will become clear when we discuss the eepb maps.                        //S9
			//So the strongest color saturation is always e2e1pb=3 but this                      //S9
			//may be either lowest or highest luminance within the cluster.                      //S9

			//In toolsets 00=b 01=p 10=e1 11=e2 and we will follow this convention               //S9
			//so 00/b=ground 01/p=ground dither 10/e1=main dither 11/e2=main fill.               //S9
			//None of these is transparent since we control alpha using CHG_COLCON.              //S9
****		-------------------------------------------------------------------------*/          //S9 20090110

			//first part of these temporary tables is for dark ground.  Lite grnd (seen          //S9 20090116
			//only in UK so far) selected from upper part when commands() builds PX_CTLI         //S9 20090116
			//override white on blue = 0xDC00     override white on red/mag = 0xDEEF             //S9=20090116

			//main hue   [00]null [01]transp [02]dark  [03]white [04]lite                        //S9 20090116
			/* e2e1pb */   0xFFFF   ,0x0000   ,0x1111   ,0xDCB1   ,0xDD11                        //S9 20090116
			//main hue  [05]yellow [06]green [07]cyan  [08]blue [09]magenta [0A]red              //S9 20090116
			/* e2e1pb */  ,0x4321   ,0x7651   ,0xA981   ,0x0011   ,0xEE11   ,0xEE11              //S9 20090116
			//main hue  [0B]spare [0C]spare [0D]spare  [0E]spare [0F]spare                       //S9 20090116
			/* e2e1pb */  ,0xF0F0   ,0xF0F0   ,0xF0F0   ,0xF0F0   ,0xF0F0                        //S9 20090116

			//REVERSE   [10]null [11]transp [12]dark  [13]white [14]lite                         //S9 20090116
			/* e2e1pb */  ,0xFFFF   ,0x0000   ,0x1111   ,0xDCB1   ,0xDD11                        //S9 20090116
			//REVERSE   [15]yellow [16]green [17]cyan  [18]blue [19]magenta [1A]red              //S9 20090116
			/* e2e1pb */  ,0xD432   ,0xD765   ,0xDA98   ,0xDC00   ,0xDEEE   ,0xDEEF              //S9 20090116
			//REVERSE   [1B]spare [1C]spare [1D]spare  [1E]spare [1F]spare                       //S9 20090116
			/* e2e1pb */  ,0x0F0F   ,0x0F0F   ,0x0F0F   ,0x0F0F   ,0x0F0F            };          //S9 20090116

	private final int[] clut_maps = {                                                            //S9

			//There are often more or fewer than four shades in the hue cluster.  Here           //S9
			//are the translation maps to e2e1pb on DVD [by # colors to compress from].          //S9
			//0x1234 means top color to e2, next 2 to e1, next 3 to p, 4 to b, but dark          //S9
			//or transparent grnd is always outside this count.                                  //S9

/***		---------------------------under review/redesign---------------------------          //S9 20090110
			//The maps for fewer shades are therefore biased away from b. If we did not          //S9
			//always put grnd to b, we'd need separate maps for quants with dark & lite          //S9
			//grnds. So the strongest color is always e2e1pb=3 but this may be either            //S9
			//lowest or highest luminance within the cluster. That does mean we need to          //S9
			//reverse the sort by luminance for clusters with lite grnd when preparing           //S9
			//to allocate shades to e2e1pb using the maps.  This is done in analyse().           //S9
****		-------------------------------------------------------------------------*/          //S9 20090110
			//
			//Entry[0] maps all shades to b and is available for special situations              //S9

				0x000F,0x1000,0x1100,0x1110,0x1111,0x1112,0x2112,0x2113,     //nice slim glyphs  //S9
			//	0x000F,0x1000,0x1100,0x1110,0x1220,0x1220,0x1221,0x1231,     //nasty fat glyphs  //S9

				0x3113,0x3114,0x4114,0x4115,0x5115,0x5116,0x6116,0x6117      //nice slim glyphs  //S9
			//	0x2231,0x2331,0x2332,0x2333,0x2334,0x2335,0x2336,0x2337      //nasty fat glyphs  //S9
            };                                                                                   //S9

			//The 'artistic' strategy is to keep the displayed text light and slim               //S9
			//so if a lot of dither colors are present, many are mapped to grnd.                 //S9
			//Doing otherwise produces very heavy text with poor legibility.                     //S9

	/*=============== List of Color Areas (cleared and built afresh for each subpic) ===============*/

	private class Area {                                                                         //S9
		public int I;                                                                            //S9

		public int trow()   { return (I & 0x0FF00000) >>>20; }           // text row max  255    //S9 20080102
		public int start()  { return (I & 0x000FFC00) >>>10; }           // 1st x    max 1023    //S9 20080102
		public int bgrnd()  { return (I & 0x00000300) >>> 8; }           // 'b'alfa  max    3    //S9 20080102
		public int clstr()  { return (I & 0x000000FF)      ; }           // clstr #  max  255    //S9 20080102
		public int trow(int t)  { I = I & 0xF00FFFFF | (t & 0xFF) <<20; return trow();  }        //S9 20080102
		public int start(int s) { I = I & 0xFFF003FF | (s & 0x7FF)<<10; return start(); }        //S9 20080102
		public int bgrnd(int b) { I = I & 0xFFFFFCFF | (b & 0x3)  << 8; return bgrnd(); }        //S9 20080102
		public int clstr(int c) { I = I & 0xFFFFFF00 | (c & 0xFF)     ; return clstr(); }        //S9 20080102
		
		public int tfirst() { return start(); }     //index - alist[tfirst] starts trow entries  //S9 20080106    
		public int tlimit() { return clstr(); }     //index - alist[tlimit] limits trow entries  //S9 20080106    
		public int tfirst(int s)  { return start(s); }  //index - set start of trow entries      //S9 20080106    
		public int tlimit(int c)  { return clstr(c); }  //index - set limit of trow entries      //S9 20080106    
		
		public int ytop()   { return trow();  }     //yinfo entry - y for trow top line          //S9 20080106    
		public int ybtm()   { return start(); }     //yinfo entry - y for trow bottom line       //S9 20080106    
		public int yfxd()   { return bgrnd(); }     //yinfo entry - 1 iff yref valid else 0      //S9 20080106    
		public int yref()   { return clstr(); }     //yinfo entry - y for reference line         //S9 20080106    

		public Area() { I = 0; }                                                                 //S9
		public Area(int t, int s, int c)        { I=0;trow(t);start(s);clstr(c); }               //S9
		public Area(int t, int s, int c, int b) { I=0;trow(t);start(s);clstr(c);bgrnd(b); }      //S9 20080101
	}                                                                                            //S9

	private ArrayList alist = new ArrayList();                                                   //S9

	private void dumpAreas(String why)                  { dumpAreas(why, new Area(), ""); }      //S9 20080102

	private void dumpAreas(String why, Area r)          { dumpAreas(why,      r,     ""); }      //S9 20080102

	private void dumpAreas(String why, Area r, String s)                                         //S9
	{   Area a; int trows=((Area)alist.get(0)).trow();   for (int i=0; i<alist.size(); i++)      //S9
		{   a = (Area)alist.get(i); System.out.println(why+" "+X(8,a.I)+" "                      //S9
			  	+d(3,a.trow())+", "+d(4,a.start())+", "+d(1,a.bgrnd())+", "+d(3,a.clstr())+" "   //S9 20080102
				+s(8,(i==0) ? "header" : (i<=trows) ? "index" : (i<=trows*2) ? "yinfo" :         //S9 20080105
				(a.clstr()<qNames.length) ? qNames[a.clstr()] : "")+((a==r)?"<---"+s:""));       //S9 20080102
		}                                                                                        //S9
	}                                                                                            //S9

	/*============== Quantised Color Table (cleared and built afresh for each subpic) ==============*/

	private class QLHI {                             // Quant, Luminance, Hue, Index             //S9
	    //Comparable deliberately not defined to force sort calls to say which sequence          //S9
		private int    N;                                                                        //S9

		public int xbp12() { return (N & 0x1FFFFFFF); }   //all flds but bp12 (for sort)         //S9

		public int  bp12() { return (N & 0xE0000000) >>>29; }  // subpic index e2/e1/p/b         //S9
		//besides the 4 RLE values, bp12 has to hold special signals e.g. lite pixels=7          //S9 20090116
		public int quant() { return (N & 0x1F000000) >>>24; }  // quantised hue max   31         //S9
		//besides <=14 quants for colors, quant takes special (low) values for dark, lite, etc   //S9 20090116
		public int   lum() { return (N & 0x00FF0000) >>>16; }  // luminance     max  240         //S9
		public int  misc() { return (N & 0x0000FF00) >>> 8; }  // opacity       max  255         //S9 20090101
		public int inUCT() { return (N & 0x000000FF)      ; }  // userColorTableIndex            //S9
		public int  bp12(int a) { N = N & 0x1FFFFFFF | (a & 0x07)<<29; return  bp12(); }         //S9
		public int quant(int a) { N = N & 0xE0FFFFFF | (a & 0x1F)<<24; return quant(); }         //S9
		public int   lum(int a) { N = N & 0xFF00FFFF | (a & 0xFF)<<16; return   lum(); }         //S9
		public int  misc(int a) { N = N & 0xFFFF00FF | (a & 0xFF)<< 8; return  misc(); }         //S9 20090101
		public int inUCT(int a) { N = N & 0xFFFFFF00 | (a & 0xFF)    ; return inUCT(); }         //S9

		public QLHI() { N = 0; }                                                                 //S9

		public QLHI(int q, int b, int f, int i)                //forces bp12 to zero!!           //S9
			{ N = 0; quant(q); lum(b); misc(f); inUCT(i); }                                      //S9 20090101
	}                                                                                            //S9

	private Comparator QLHI_ORDER  = new Comparator() {             //all flds but bp12          //S9
		public int compare(Object e1, Object e2)                                                 //S9
			{return (((QLHI)e1).xbp12()>((QLHI)e2).xbp12()) ? +1 :                               //S9
			        (((QLHI)e1).xbp12()<((QLHI)e2).xbp12()) ? -1 : 0;}                           //S9
	};                                                                                           //S9

	private Comparator UCT_ORDER   = new Comparator() {             //align to UCT               //S9
		public int compare(Object e1, Object e2)                                                 //S9
			{return (((QLHI)e1).inUCT()>((QLHI)e2).inUCT()) ? +1 :                               //S9
			        (((QLHI)e1).inUCT()<((QLHI)e2).inUCT()) ? -1 : 0;}                           //S9
	};                                                                                           //S9

	private QLHI[] quants;                                          //init in analyse()          //S9

	public int getQuant(int i) {return quants[i].N; }               //for dbg from other classes //S9

	private void dumpquants(String why) { QLHI q;                                                //S9
		for (int i=0; i<quants.length; i++)  { q = quants[i];                                    //S9
			System.out.println(why+" "+X(8,q.N)+" "+d(2,q.quant())+", "+d(3,q.lum())             //S9
			+", "+d(3,q.misc())+", "+d(3,q.inUCT())+" "+s(8,qNames[q.quant()]));                 //S9 20090101
		}                                                                                        //S9
	}                                                                                            //S9
	private void dumpquants(String why, ArrayList user_color_table) { QLHI q;                    //S9
		for (int i=0; i<quants.length; i++)  { q = quants[i];                                    //S9
			System.out.println(why+" "+X(8,q.N)+" "+d(2,q.quant())+", "+d(3,q.lum())             //S9
			+", "+d(3,q.misc())+", "+d(3,q.inUCT())+" "+s(8,qNames[q.quant()])                   //S9 20090101
			+" UCT="+X(8,Integer.parseInt(user_color_table.get(q.inUCT()).toString())) );        //S9
		}                                                                                        //S9
	}                                                                                            //S9

	/*============ color analysis subroutines - that (probably) should be in Class QLHI ===========*/

	private static int[] TCT_q_hues = new int[16];  //quant manager - the hues we have seen      //S9q20090116 
	private static int[] TCT_quants = new int[16];  //quant manager - quants we gave them        //S9q20090116
	private static int   TCT_q_free = 0;            //quant manager - first free entry           //S9q20090116  
	private static int   TCT_q_maxQ = Q1 - 1;       //quant manager - highest quant allocated    //S9q20090116  
	
	private QLHI ARGBtoQLHI(int ARGB, int index, int flip)   //version for clstrs from TCT       //S9q20090116
	{                                                        //index included for later sort     //S9q20090114
	                                                         //flip = 1 from TCT, 0 from stream  //S9q20090114

		if (!dbgDynQnt() && flip==0) return ARGBtoQLHI(ARGB, index);  //old method unless S9 40  //S9q20090116

		int A    = (flip==1)?0xFF : 0xFF & ARGB>>>24;               //TCT calls assumed opaque   //S9q20090116
		int R = 0xFF & ARGB;                                                                     //S9q
		int G = 0xFF & ARGB>>>8;                                                                 //S9q
		int B = 0xFF & ARGB>>>16;          //pixels from YCbCr stream delivered with red in LSB  //S9q20090114
		if (flip == 1)                     //cluts are imported as RGB so ARGB right way round   //S9q20090114
			{ R = 0xFF & ARGB>>>16; B = 0xFF & ARGB; }                                           //S9q20090114

		int max = (R>G)?R:G; max = (max>B)?max:B;                                                //S9q20090114
		int min = (R<G)?R:G; min = (min<B)?min:B;                                                //S9q20090114
		int lum = (max + min)/2;                                                                 //S9q20090114

		int clr, hue;                                                                            //S9q20090110
		float sat = (max - min);                        //sat<2 means hue neutral (not red/blue) //S9q20090117
		// max HSL is 240 so pure colors are r0, y40, g80, c120, b160, m200, r240                //S9q20090114
		if (sat < 2)  { clr = 3160; } else             //neutrals traditionally set to 160       //S9q20090114
		if (max == R) { clr = (int)((float)(G-B)*40f/sat + 240) % 240;  } else //closest to red  //S9q20090114
		if (max == G) { clr = (int)((float)(B-R)*40f/sat) +  80 + 1000; } else //closest to green//S9q20090114
					  { clr = (int)((float)(R-G)*40f/sat) + 160 + 2000; }      //closest to blue //S9q20090114
		// the extra 1000/2002/3000 is to show which path taken - modulo away after debug print  //S9q20090114
		hue = clr%1000;                                                                          //S9q20090110
		
		//---------------------------this is the "quant manager"---------------------------
if (dbgSub(5) && TCT_q_free==0) {String s = "qmgr1 dump";String t = "qmgr1  hue";                //S9q20090116
			     for (int i=0;i<16;i++) { s=s+"  q["+d(2,i)+"]="+X(2,TCT_quants[i]); t=t+"  h["  //S9q20090116
		         +d(2,i)+"]="+X(2,TCT_q_hues[i]);} System.out.println(s);System.out.println(t);} //S9q20090116

		int qh = (A==0)?QT: (lum<0x28)?QD: (sat<2)?QN: 0;           //all special cases > zero   //S9q20090116
out:    while (qh==0)                                               //look up for normal colors  //S9q20090116
		{                                                                                        //S9q20090116
			int low = 0x1FFFF;                                      //lowest difference so far   //S9q20090116
			int ndx = 0;                                            //index of best hit so far   //S9q20090116
			for (int i=0, dif=0; i<TCT_q_free; i++)                 //cf all prev clstr allocs   //S9q20090116
			{                                                                                    //S9q20090116
				dif = hue - TCT_q_hues[i];                                                       //S9q20090116
				if (dif == 0)  { qh = TCT_quants[i]; break out; }   //efficient exit for freq    //S9q20090116
				if (dif < 0)                      { dif = -dif; }   //just the proximity         //S9q20090116
				if (dif > 120)                 { dif = 240-dif; }   //closer 'round the circle'  //S9q20090116
				if (dif < low)            { low = dif; ndx = i; }   //index of closest so far    //S9q20090116
if (dbgSub(5)) System.out.println("quant search"+" / hue="+X(2,hue)+" / i="+d(2,i)               //S9q20090116
				+" / ndx="+X(3,ndx)+" / dif="+X(6,dif)+" / low="+X(6,low) );                     //S9q20090116

			} //end hue search      =======>> NOTE only here if no exact hit <<=======           //S9q20090116
			if (flip == 1)                                          //TCT caller -> update table //S9q20090116
			{                     //if hit within 1/12 of circle copy quant else issue new quant //S9q20090116
				qh = (low < 0x14) ? TCT_quants[ndx] : ++TCT_q_maxQ; //                           //S9q20090116
				if (low != 0)                                       //not exact hit -> new hue   //S9q20090116
					{ TCT_q_hues[TCT_q_free] = hue; TCT_quants[TCT_q_free++] = qh; }             //S9q20090116
			}                                                                                    //S9q20090116
			else		          //if hit within 1/12 of circle copy quant else qh = 0 (error)  //S9q20090116
				qh = (low < 0x14) ? TCT_quants[ndx] : QX;          //QX==0 so next break vital   //S9q20090116
			break;                                                                               //S9q20090116
		} //end  out:while qh==0                                                                 //S9q20090116
if (dbgSub(5) ) {String s = "quant dump";String t = "   for hue";                                //S9q20090116
			     for (int i=0;i<16;i++) { s=s+"  q["+d(2,i)+"]="+X(2,TCT_quants[i]); t=t+"  h["  //S9q20090116
		         +d(2,i)+"]="+X(2,TCT_q_hues[i]);} System.out.println(s);System.out.println(t);} //S9q20090116
		//----------------------------end of the "quant manager"---------------------------

		QLHI myQHLI = new QLHI(qh, lum, hue, index);         //hue in misc field only for debug  //S9q20090116

if (dbgSub(4)) System.out.println("quant return"+d(3,index)+"/ARGB "+X(8,ARGB)
		    +"/QLHI "+X(8,myQHLI.N)+"/flip "+d(1,flip));                                         //S9q20090116

		return myQHLI;                                                                           //S9q20090116
	}                                                                                            //S9q20090116

	private QLHI ARGBtoQLHI(int ARGB, int index)     // older version - hardcoded color quants   //S9q
	{                                                                                            //S9q
		//convert local "RGB" to Hue & Lum - note "ARGB" is a misnomer, actually the Red         //S9q
		//component is being stored as LSB but it doesn't matter as it's local to this app       //S9q

		//then construct a "quantised hue" clustering the actual hues into eight groups          //S9q

		//the original UCT index is incorporated so we can sort back to that sequence later      //S9q
		
		int A = 0xFF & ARGB>>>24;                                                                //S9q
		int B = 0xFF & ARGB>>>16;                                                                //S9q
		int G = 0xFF & ARGB>>>8;                                                                 //S9q
		int R = 0xFF & ARGB;                                                                     //S9q

		int Max = (R>G)?R:G; Max = (Max>B)?Max:B;                                                //S9q
		int Min = (R<G)?R:G; Min = (Min<B)?Min:B;                                                //S9q

		int Lum = (Max + Min)/2;                                                                 //S9q

		int Clr, Hue;                                                                            //S9q20090110
		float Dif = (Max - Min);                                                                 //S9q
		// max HSL is 240 so pure colors are r0, y40, g80, c120, b160, m200, r240                //S9q
		if (Dif < 2)  { Clr = 3160; } else                 //neutrals traditionally set to 160   //S9q
		if (Max == R) { Clr = (int)((float)(G-B)*40f/Dif + 240) % 240;  } else //closest to red  //S9q
		if (Max == G) { Clr = (int)((float)(B-R)*40f/Dif) +  80 + 1000; } else //closest to green//S9q
					  { Clr = (int)((float)(R-G)*40f/Dif) + 160 + 2000; }      //closest to blue //S9q
		// the extra 1000/2002/3000 is to show which path taken - modulo away after debug print  //S9q
		Hue = Clr%1000;                                                                          //S9q20090110
	
		int ih = (Hue+20)/40; 
		int qh =  (ih + 5)% 6;                             //quantise hue to 6 colors            //S9q
//		qh = (A==0)?QT: (Lum<0x28)?QD: (Dif<2)?QN: (Q1+4)-qh;   //cheap trick to test more grads //S9q20090101 =+=
		qh = (A==0)?QT: (Lum<0x28)?QD: (Dif<2)?QN:  Q1+qh; //add quants for neutrals & trnsprnts //S9q20090101 =+=
														    
if (dbgSub(5))                                                                                   //S9q20090105
		  System.out.println("ARGBtoQLHI:/index "+d(3,index)+"/ARGB "+X(8,ARGB)                  //S9q
		  +"/R "+d(4,R)+"/G "+d(4,G)+"/B "+d(4,B)+"/Max "+d(4,Max)+"/Min "+d(4,Min)              //S9q20090110
		  +"/Dif "+d(4,(int)Dif)+"/Lum "+d(4,Lum)+"/Clr "+d(4,Clr)+"/Hue "+d(3,Hue)              //S9q20090110
		  +"/ih "+d(3,ih)+"/qh "+X(1,qh));                                                       //S9q20090110

		//the return value is optimised for Shannock9's "hue cluster" approach, but the        	 //S9q
		//raw Hue is also returned in case someone else wants to try something different       	 //S9q

		//0 thru 3 and 10 are special values: 0=null, 1=transp, 2=dark , 3=neutrals, 10=lite     //S9q
		//4 thru 9 are six equispaced hues: 4=yellow; 5=green, 6=cyan, 7=blue, 8=magenta, 9=red, //S9q
		//(lite and dark are set by caller later, we just avoid returning those values)          //S9q

		//  return qh << 24 | Lum << 16 | Hue << 8 | index;                                      //S9q
		return new QLHI(qh, Lum, Hue, index);         //hue in misc field only for debug         //S9q
	}                                                                                            //S9q
	
	/*===================== bitmap analysis subroutines - called from analyse() ====================*/

	private void findTextRows(Bitmap bitmap)          //find the arrangement of text rows        //S9t20090102
	{                                                 //results ADDED to alist                   //S9t20090102
	
		//The object of this sub-analysis is to identify the text rows (blocks of video lines    //S9t20090102
		//containing one text line with pure background or 'margin' lines above and/or below).   //S9t20090102
		//Each textrow will later be broken into color areas independently of other textrows.    //S9t20090102
		
		//Early versions relied totally on geometrical breakdown into regular rows, but some     //S9t20090102
		//transmitted subpics do not honor this model. Today we have a hybrid technique with     //S9t20090102
		//subpic content used to 'tune' the row boundaries.   This tuning is only possible       //S9t20090106
		//(and only necessary) for subpics with transparent background (e.g. ZDF).               //S9t20090106
	
		int pixels[] = bitmap.getPixel();                                                        //S9t20090102
		int w = bitmap.getWidth();                                                               //S9t20090102
		int h = bitmap.getHeight();                                                              //S9t20090102

		/*---------estimate subpicture structure geometrically----------*/                       //S9t20090102
		int textrows;                                                                            //S9t20090102
		//following assumes the repeating row height is even (else how to interlace?)            //S9t20090102
		//so exact divisibility by eight is a test for presence of four rows (etc.)              //S9t20090102
		//multiple solutions < 210 for mod 4, 6, and 8 are  at 96,144,160,180,192,200            //S9t20090102
		//test sequence chosen to prefer 3 rows then 4 then 2, always provided rh >=32           //S9t20090102
		if (h>= 91 && (h/6)*6==h) textrows = 3; else   //only give 3 if rh >=32 (etc.)           //S9t20090102
		if (h>=121 && (h/8)*8==h) textrows = 4; else   //smallest rows ever seen were 32         //S9t20090102
		if (h>= 61 && (h/4)*4==h) textrows = 2; else   //...this code fails for rh<=30,          //S9t20090102
			                         textrows = 1;     //...and note 144->3x48 not 4x36          //S9t20090102
		int rh = h/textrows;                           //geometric height of a text row          //S9t20090102

		/*----------look for text by counting edges along lines---------*/                       //S9t20090102
		/*----------find textblocks separated by >1 margin line---------*/                       //S9t20090102
		int[] txtTop = new int[h]; int[] txtBtm = new int[h];                                    //S9t20090102
		int textblks = -1; int textgaps = -1; int stat = -1;     //wtg to collapse wave function //S9t20090102
		for (int r=0, a=0, rEdges=-1; r<h; r++, a=r*w, rEdges=-1)                                //S9t20090102
		{                                                                                        //S9t20090102
			for (int j=0, prv=0, now=0; j<w; j++, prv=now)                                       //S9t20090102
				if (prv!=(now=pixels[a+j])) { rEdges++; }                                        //S9t20090102
			//when changing between lines with text and margin lines a one line delay is imposed
			//so a lone line of opposite kind is ignored (except margins at top/bottom of subpic)
			if (rEdges > 0)                                      // this line seems to be text   //S9t20090106
			{                                                                                    //S9t20090102
				if (stat>=4) {stat=4;                                  }//additional text line   //S9t20090102
				if (stat==1) {stat=4; textblks++; txtTop[textblks]=r-1;}//(delayed) 1st txt line //S9t20090102
				if (stat<=0) {stat=1;                                  }//wait for 2nd text line //S9t20090102
			}                                                                                    //S9t20090102
			else                                                  //this line seems to be margin //S9t20090102
			{                                                                                    //S9t20090102
				if (stat <0) {stat=0; textgaps++;                      }//initial margin line    //S9t20090102
				if (stat<=3) {stat=0;                                  }//additional margin line //S9t20090102
				if (stat==5) {stat=0; textgaps++; txtBtm[textblks]=r-2;}//(delayed) 1st margn ln //S9t20090102
				if (stat==4) {stat=5;                                  }//wait for 2nd margin ln //S9t20090102
			}                                                                                    //S9t20090102
if (dbgSub(4)) System.out.println("line /pic "+d(5,picCount)+" / edges["+d(3,r)+"]="+d(3,rEdges) //S9t20090102
			+" / state "+d(1,stat)+" / textblks "+d(3,textblks)+" / textgaps "+d(3,textgaps));   //S9t20090106
		}                                                                                        //S9t20090102
		if (stat==5)         {stat=0;textgaps++;txtBtm[textblks]=h-2;} // finish any final margn //S9t20090106  
		textgaps++;  //count of margins found;    if zero must use geometry with no adjustments  //S9t20090106                                                          //S9t20090102
		textblks++;  //count of valid txtTop/Btm; but if textgaps=0 then all txtTop/Btm are zero //S9t20090106                                                          //S9t20090102

if (dbgSub(2)) System.out.println("Find textrows /pic "+d(5,picCount)+"/w "+d(3,w)+"/h "+d(3,h)  //S9t20090102
				+"/textrows "+d(3,textrows)+"/geom. rowheight "+d(3,rh)                          //S9t20090106
				+"/textblks "+d(3,textblks)+"/textgaps "+d(3,textgaps) );                        //S9t20090106
				
		/*---------generate text rows based on fixed row heights--------*/                       //S9t20090102
		/*---------if text blocks detected then adjust the rows---------*/                       //S9t20090102
		int[] rowTop = new int[textrows]; int[] rowBtm = new int[textrows];                      //S9t20090102
		
		//textgaps==0 means no lines with single background (probably transp...other...transp)   //S9t20090106 
		//If this happens txtBtm[0]==0 (i.e. txtblks >initially< exhausted) so no adjustments.   //S9t20090106
		//In other words unless entire background is transp we rely entirely on the geometry.    //S9t20090106
		
		//if 0 < textblks < textrows then we will simply not generate the extra textrows         //S9t20090102
		//but if textblks > textrows we are in deep trouble and will have to tell the user :)    //S9t20090102

		if (textblks > textrows)                                                                 //S9t20090102
Common.setMessage("subpic "+d(5,picCount)                                                        //S9t20090102
			+" has too many text rows for the pixel height - please check");                     //S9t20090102
			
		for (int t=0, a=0; t<textrows; t++)                                                      //S9t20090102
		{                                                                                        //S9t20090102
			rowBtm[t] = rh + (rowTop[t] = rh*t);          //from geometry (regular cells)        //S9t20090102
				
if (dbgSub(4)) System.out.println("cell /pic "+d(5,picCount)+"/h "+d(3,h)                        //S9t20090102
				+"/celTop["+d(1,t)+"]="+d(3,rh*t)                                                //S9t20090102
				+"/rowTop["+d(1,t)+"]="+d(3,rowTop[t])+"/txtTop["+d(1,t)+"]="+d(3,txtTop[t])     //S9t20090102
				+"/txtBtm["+d(1,t)+"]="+d(3,txtBtm[t])+"/rowBtm["+d(1,t)+"]="+d(3,rowBtm[t])     //S9t20090102
				+"/celBtm["+d(1,t)+"]="+d(3,rh*t+rh));                                           //S9t20090102
				
			if (t == 0) continue;                         //no upper neighbor yet                //S9t20090102
			if (txtBtm[t-1] == 0) continue;               //textblks previously/always exhausted //S9t20090102
			// when fixing the textrow Btm we include 1 margin row - it might be reclaimed below //S9t20090102
			if ((a = txtBtm[t-1] - rowTop[t] + 1) > 0)    //upper neighbor overlaps us           //S9t20090102
				{ rowTop[t] += a; rowBtm[t-1] += a; }     //move the boundary down incl 1 margin //S9t20090102

if (dbgSub(4)) System.out.println("adj+ /pic "+d(5,picCount)+"/a "+d(5,a)                        //S9t20090102
			+"/rowTop["+d(1,t)+"]="+d(3,rowTop[t])+"/rowBtm["+d(1,t-1)+"]="+d(3,rowBtm[t-1]));   //S9t20090102

			if (txtTop[t] == 0)                           //textblks have become exhausted       //S9t20090102
				{ txtTop[t] = -1; textrows--; continue; } //mark this row 'do not generate'      //S9t20090102
			if ((a = rowTop[t] - txtTop[t]) > 0)          //we overlap upper neighbor            //S9t20090102
				{ rowTop[t] -= a; rowBtm[t-1] -= a; }     //move the boundary up (no margin)     //S9t20090102

if (dbgSub(4)) System.out.println("adj- /pic "+d(5,picCount)+"/a "+d(5,a)                        //S9t20090102
			+"/rowTop["+d(1,t)+"]="+d(3,rowTop[t])+"/rowBtm["+d(1,t-1)+"]="+d(3,rowBtm[t-1]));   //S9t20090102
		}   //end for each textrow                                                               //S9t20090102

		/*--------put empty index and completed yinfo into alist--------*/                       //S9t20090102
		alist.add(new Area(textrows,bitmap.getY(),rh));//useful CHG_COLCON data to start of list //S9t
		for (int t=0; t <textrows; t++)                //make empty index to list areas per row  //S9t
			alist.add(new Area(t,0,0));                //will be updated by collectAreas()       //S9t
		for (int t=0; t<textrows; t++)                 //make yinfo to find lines for each trow  //S9t20090102
		{                                                                                        //S9t20090102
			if (txtTop[t] >= 0)                        //txtTop < 0 means 'do not generate'      //S9t20090102
				//third param selects 1st avail margin row to use for testing column background  //S9t20090102
				//..there "must" be at least one above or below, or this would not be a textblk  //S9t20090102
				//fourth parameter is 0 if no textblks found -> no background row can be chosen  //S9t20090102
				alist.add(new Area(rowTop[t], rowBtm[t],                                         //S9t20090102
					                      ((rowTop[t]<txtTop[t]) ? rowTop[t] : txtBtm[t]+1),     //S9t20090102
					                      ((textgaps==0) ? 0 : 1) ));                            //S9t20090106
				//note collectAreas() doesn't know whether we are using variable row heights; it //S9t20090106
				//..just processes the rows as we here define them in the yinfo entries of alist //S9t20090106
				
if (dbgSub(4)) System.out.println("yinfo/pic "+d(5,picCount)+"/h "+d(3,h)                        //S9t20090102
				+"/celTop["+d(1,t)+"]="+d(3,rh*t)                                                //S9t20090102
				+"/rowTop["+d(1,t)+"]="+d(3,rowTop[t])+"/txtTop["+d(1,t)+"]="+d(3,txtTop[t])     //S9t20090102
				+"/txtBtm["+d(1,t)+"]="+d(3,txtBtm[t])+"/rowBtm["+d(1,t)+"]="+d(3,rowBtm[t])     //S9t20090102
				+"/celBtm["+d(1,t)+"]="+d(3,rh*t+rh));                                           //S9t20090102
		}   //end for each textrow                                                               //S9t20090102

if (dbgSub(2)) dumpAreas("trow top + next + margin row");                                        //S9t20090102
	}                                                                                            //S9t
	

	private void collectAreas(Bitmap bitmap, ArrayList user_color_table)                         //S9a20090105
	                                               //find contiguous areas with similar hue      //S9a20090105
	{                                              //results ADDED to alist; also build BP12[]   //S9a20090105
												   //these two will be used to build subpic      //S9a20090105
	
		//The object of this sub-analysis is to identify within each text row blocks of columns  //S9a20090105
		//that can use the same 4-color subpalette and thus be subject of a single PX_CTLI. Such //S9a20090105 
		//a block must have both colclstr[] and colgrnd[] consistent throughout. We also correct //S9a20090105
		//(where we can) inconsistent horizontal lines that appear to be transmission noise.     //S9a20090105
		
		int pixels[] = bitmap.getPixel();                                                        //S9a
		int w        = bitmap.getWidth();                                                        //S9a
		int h        = bitmap.getHeight();                                                       //S9a

		int[] colclstr;                                //col state: null->transp/{wild->color}   //S9a
		int[] colgrnd;                                 //grnd: (dark)transp / dark(opaque) /lite //S9a20090104

		bp12 = new int[w * h];                         //speed up - build bp12 on same pass      //S9a
		int defPixel; int defBP12;                     //default pxl & result - used for repairs //S9a20090105
		QLHI  myQLHI;                                                                            //S9a

		//things that don't persist long enough to be worth naming...
		int t1=0; int t2=0; int t3=0; int t4=0; int t5=0;                                        //S9a
		//powerful spell needed because java generics theologically unacceptable in this realm   //S9a
		Area[] aindx = (Area[])(alist.toArray(new Area[alist.size()]));   //cast entire index    //S9a
		
		int textrows = aindx[0].trow();                //it was put there by findTextRows()      //S9a20090105
		for (int t=0; t <textrows; t++)                //each text row gets own list of PX_CTLI  //S9a
		{                                                                                        //S9a
			aindx[t+1].tfirst(alist.size());               //update alist index at start of row  //S9a
			int a = w * aindx[t+1+textrows].ytop();        //offset for top line of textrow      //S9a20090103
			int b = w * aindx[t+1+textrows].ybtm();        //offset for btm line of textrow      //S9a20090103
			int m = w * aindx[t+1+textrows].yref();        //offset for ref line of textrow      //S9a20090103
//			int a = t * w * rh; int b = (t+1) * w * rh;    //offsets for textrow and nextrow     //S9a20090103
			int e = 0;                                     //count alien pixels this textrow     //S9a
			colclstr = new int[w];                         //reset all col clusters to null      //S9a
			colgrnd  = new int[w];                         //separate stash for column grnds     //S9a20090104
			for (int j=0; j < w; j++)                      //FOR EACH COL - j is x coordinate    //S9a
			{                                                                                    //S9a
			/*------establish column visual background (rev video keeps state "undecided")-----*///S9a
			/*--------background also used as default for repairs to horizontal glitches-------*///S9a
				if (aindx[t+1+textrows].yfxd() == 1)       //row(s) with just one grnd detected  //S9a20090106
					defPixel = pixels[j+m];                //col ground from designated margin   //S9a20090103
				else                                       //no margin found - use "best of 3"   //S9a20090106
				{                                                                                //S9a20090106
					t2 = quants[user_color_table.indexOf(""+pixels[j+a    ])].quant(); //first   //S9a
					t3 = quants[user_color_table.indexOf(""+pixels[j+a+w  ])].quant(); //second  //S9a
					t4 = quants[user_color_table.indexOf(""+pixels[j+b-w  ])].quant(); //last    //S9a
					t1 = (t2==t3) ? a : (t2==t4) ? a : (t3==t4) ? b-w : -1;//t1 is best of three //S9a20090102
					if (t1 < 0)                                                                  //S9a
					{                                                                            //S9a
						t1 = a+w;                          //no consensus - use 2nd line         //S9a
Common.setMessage("background no consensus for pic "+d(5,picCount)+" at PTS: "                   //S9a
		  +Common.formatTime_1(bitmap.getInTime() / 90)+"/textrow "+d(1,t)+"/column "+d(3,j)     //S9a
		  +"/top "+X(8,pixels[j+a])+"/2nd "+X(8,pixels[j+a+w])+"/btm "+X(8,pixels[j+b-w]));      //S9a
					}                                                                            //S9a
					defPixel = pixels[j+t1];               //save col default pixel for repairs  //S9a
				}                                                                                //S9a20090106
				myQLHI = quants[user_color_table.indexOf("" + defPixel)];     //QLHI for default //S9a
				t5 = myQLHI.quant();                       //hue cluster # ("color") for default //S9a
				//there are no transparent columns, only dark quant ones with transparent grnd   //S9a20090104
				colclstr[j] = (t5<=QT) ? QD : t5;          //clstr for this col (trnsp->dark)    //S9a20090116 =!=
				colgrnd[j] = (t5<=QT)?0:(t5==QD)?1:(t5==QL)?2: 3;  //grnd=3 means grnd undecided //S9a20090116 =!=
				defBP12 = myQLHI.bp12();                   //save col default result for repairs //S9a
				//col clstr can be dark or lite (either to be resolved by any definitive pixel)  //S9a
				//col grnd can be color (to be resolved to dark or lite by any definitive pixel) //S9a20090109

if (dbgpic()==picCount || dbgSub(4))  System.out.println("background " +"/pic "+d(5,picCount)    //S9a20090101
				+"/t "+d(2,t)+"/x "+d(3,j)+"      "+"/colgrnd "+d(1,colgrnd[j])                  //S9a20090105
				+"/colclstr "+d(2,colclstr[j])+"/t2,t3,t4 "+X(1,t2)+"  "+X(1,t3)+"  "+X(1,t4)    //S9a20090101
  	 			+"/Q "+X(8,myQLHI.N)+" pixel "+X(8,defPixel));                                   //S9a20090104

			/*--------go thru column's pixels -> col clstr & grnd -> pixel quant & bp12--------*///S9a
			/*---------bp12 assignment has to support that we may not yet know the grnd--------*///S9a
			/*--------(if don't know grnd by end of col, copy grnd from neighbor later)--------*///S9a
				t3 = t5; t4 = t5;                          //fake 'history' for corrections      //S9a
				for (int i=a; i < b; i+=w)                 //FOR EACH PIXEL in this column       //S9a
				{                                          //i is the y coordinate (times width) //S9a
					t1 = pixels[j+i];                                                            //S9a
					myQLHI = quants[user_color_table.indexOf("" + t1)];                          //S9a
					t2 = myQLHI.quant();                   //hue cluster # ("color") for pixel   //S9a
					bp12[j+i] = myQLHI.bp12();             //assigned 2bit e2e1pb for pixel      //S9a

					//------------------try to resolve undecided col grnd-----------------       //S9a20090109
					if (colgrnd[j] > 2)                    //only a grnd pixel can tell us       //S9a20090109
						colgrnd[j] = (t2<=QT)?0: (t2==QD)?1: (t2==QL)?2: 3;  //3=still undecided //S9a20090116 =!=

					//----------interpret lite pixels as white or grnd of column----------       //S9a
					//pixel lite and column has dark grnd -> treat pixel as specifically white   //S9a20090116 =!=
					if (t2==QL && colgrnd[j] < 2 ) { t2 = QN; }  //bp12 same either way          //S9a20090116 =!=


					//-------------correct pixels that mismatch column opacity------------       //S9a
					if (colgrnd[j] != 0 && t2 == QT)       //opaque grnd & completely transp pxl //S9a20090116 =!=
					{                                      //...fix this ahead of singleton test //S9a
if (dbgSub(4)) System.out.println("alpha err  "+"/pic "+d(5,picCount)                            //S9a20090105
				+"/t "+d(2,t)+"/x "+d(3,j)+"/y "+d(3,(i-a)/w)+"/colgrnd "+d(1,colgrnd[j])        //S9a20090105
				+"/colclstr "+d(2,colclstr[j])+"/t2,t3,t4 "+X(1,t2)+"* "+X(1,t3)+"  "+X(1,t4)    //S9a
				+" /pixel "+X(8,pixels[j+i  ])+" <- "+X(8,defPixel)                              //S9a
				+"/fixit="+((dbgnofix()?"no":"yes")));                                           //S9a

						if (!dbgnofix())                   //in case someone wants to keep noise //S9a
						{                                                                        //S9a
							if (i >= a+w)                  //line above is available             //S9a
							{
								pixels[j+i] = pixels[j+i-w];//copy from line above               //S9a
								bp12[j+i]   = bp12[j+i-w];  //...and fix up results              //S9a
								t2 = t3;                   //prepare the history                 //S9a
							}
							else                           //we are on the first line            //S9a
							{
								pixels[j+i] = defPixel;    //replace with "best of 3" for col    //S9a
								bp12[j+i] = defBP12;       //...and fix up results               //S9a
								t2 = colclstr[j];          //prepare the history                 //S9a
							}
						}   //endif replace with "best of 3"                                     //S9a
					}   //endif background opacity mismatch                                      //S9a

					//------------correct any singleton pixels using line above-----------       //S9a
					if (t2!=t3 && t3!=t4               //singleton >>quant<< NB not single pixel //S9a
					 && i>=a+w                         //..&& line above avail && not grnd pixel //S9a
					 && (colgrnd[j]>1 || t3!=QD)       //not (col grnd dark/transp & pixel dark) //S9a20090116 =!=
					 && (colgrnd[j]>0 || t3!=QT) )     //not (col grnd transp only & pxl transp) //S9a20090116 =!=
					{                                      //....assume noise and repair it      //S9a
						//-----------------------------------------------------------------------//S9a
						//Following code is to repair transmission errors/noise appearing as a   //S9a
						//line of pixels of 'wrong' quant.  We are scanning vertically and can   //S9a
						//easily detect this. By this strategy we'll also 'fix' some singleton   //S9a
						//dithering pixels - though only those on the boundary between clstr     //S9a
						//and grnd, where the visual effect is insignificant. But we won't fix   //S9a
						//any (non lite) grnd singletons to avoid bulking up of text glyphs      //S9a
						//(genuine dark errors look ok, like neg video modulation on analog TV)  //S9a
						//-----------------------------------------------------------------------//S9a
if (dbgSub(3)) System.out.println("singleton  "+"/pic "+d(5,picCount)                            //S9a20090105
				+"/t "+d(2,t)+"/x "+d(3,j)+"/y "+d(3,(i-a)/w)+"/colgrnd "+d(1,colgrnd[j])        //S9a20090105
				+"/colclstr "+d(2,colclstr[j])+"/t2,t3,t4 "+X(1,t2)+"  "+X(1,t3)+"* "+X(1,t4)    //S9a
				+" /pixel "+X(8,pixels[j+i-w])+" <- "+X(8,pixels[j+i-2*w])                       //S9a
				+"/fixit="+((dbgnofix()?"no":"yes")));                                           //S9a

						if (!dbgnofix())                   //in case someone wants to keep noise //S9a
						{                                                                        //S9a
							pixels[j+i-w] = pixels[j+i-2*w]; //copy from line above              //S9a
							bp12[j+i-w]   = bp12[j+i-2*w];   //...and the result                 //S9a
							t3 = t4;                       //rewrite the history of singleton    //S9a
						}   //endif copy from row above                                          //S9a
					}   //endif singleton clstr                                                  //S9a

					//------------on new quant, delay any action for one pixel------------       //S9a
					//first with new quant is potential noise..only vertical runs > 2 credible   //S9a
					t4 = t3; t3 = t2;                      //update stack of recent history      //S9a
					if (t3 != t4)                          //pixel quant differs from one above  //S9a
						if (i < b-w)                       //not last line->no further action    //S9a
							continue;                      //..note history already stacked      //S9a                                     //S9a

if (dbgpic()==picCount)  System.out.println("dbgpic     "+"/pic "+d(5,picCount)                  //S9a20090105
				+"/t "+d(2,t)+"/x "+d(3,j)+"/y "+d(3,(i-a)/w)+"/colgrnd "+d(1,colgrnd[j])        //S9a20090105
				+"/colclstr "+d(2,colclstr[j])+"/pq-2 "+d(2,t3)+"/pq-1 "+d(2,t2)                 //S9a
  	 			+"/Q "+X(8,myQLHI.N)+" pixel "+X(8,t1));                                         //S9a20090106

					//------------if pixel now acceptable add it to the column------------       //S9a
					if (colclstr[j] == t2)       continue; //col clstr already matches pixel     //S9a
					if (colgrnd[j]==2 && t2==QL) continue; //col has lite grnd and pixel lite    //S9a20090116 =!=
					if (colgrnd[j]<=1 && t2==QD) continue; //col grnd dark or transp & pxl dark  //S9a20090116 =!=
					if (colgrnd[j]==0 && t2==QT) continue; //col grnd transp only & pixel transp //S9a20090116 =!=
					
					if ((colclstr[j]==2 || colclstr[j]==QL)//col only has grnd pixels so far...  //S9a20090116 =!=
					                    && t2 >= QN)       //...and pixel has nongrnd color      //S9a20090116 =!=
						{ colclstr[j] = t2; continue; }    //grnd column gets specific color     //S9a

//	should no longer be an error now we can have red/dark and blue/dark columns                  //S9a20090110
//					if (colclstr[j]== 2 && i==b-w && t2>=7)//dark col & last pxl with lite grnd  //S9a
//					{                                      //...(std error on some UK channels)  //S9a
//						pixels[j+i] = defPixel;            //replace with "best of 3" for col    //S9a
//						bp12[j+i] = defBP12;               //...and fix up results               //S9a
//						t3 = colclstr[j];                  //rewrite the stacked history         //S9a
//						continue;                          //...don't bother to log it           //S9a
//					}                                                                            //S9a
					
					//--------pixel doesn't fit hue cluster or grnd for this column-------       //S9a
if (dbgSub(3)) System.out.println("alien pixel"+"/pic "+d(5,picCount)                            //S9a20090105
				+"/t "+d(2,t)+"/x "+d(3,j)+"/y "+d(3,(i-a)/w)+"/colgrnd "+d(1,colgrnd[j])        //S9a20090105
				+"/colclstr "+d(2,colclstr[j])+"/t2,t3,t4 "+X(1,t2)+"  "+X(1,t3)+"* "+X(1,t4)    //S9a
				+" /pixel "+X(8,pixels[j+i  ])+" <- "+X(8,defPixel)                              //S9a
				+"/fixit="+((dbgnofix()?"no":"yes")));                                           //S9a
					e += 1;                                //count alien pixels for main log     //S9a
					if (!dbgnofix())                       //fixups not inhibited                //S9a
					{                                                                            //S9a
						pixels[j+i] = defPixel;            //replace with "best of 3" for col    //S9a
						bp12[j+i] = defBP12;               //...and fix up results               //S9a
						t3 = colclstr[j];                  //rewrite the stacked history         //S9a
						continue;                                                                //S9a
					}                                                                            //S9a
				}   //end for each pixel in column                                               //S9a
			}   //end for each column in textrow                                                 //S9a
			
			if (e > 0)                                                                           //S9a
Common.setMessage(e+" alien pixels "+(dbgnofix()?"not ":"")+"masked in pic "+d(5,picCount)       //S9a
				+" at PTS: "+Common.formatTime_1(bitmap.getInTime() / 90)+"/textrow "+d(1,t));   //S9a

			/*------------join uncolored cols with colored neighbors of same grnd--------------*///S9a
			/*----------join ungrounded cols with same color neighbors of known grnd-----------*///S9a
			/*----------(must know col grnd & quant to set area info -> select grad)-----------*///S9a
			for (int g=0, q=0, j=0; j < w; j++)               //smear col definitions from LEFT  //S9a20090109
			{                                                                                    //S9a20090106
				t1 = colgrnd[j]; t2 = colclstr[j]; t3 = g; t4 = q;          //for debug          //S9a20090109
				
				if (colgrnd[j]==3 && colclstr[j]==q) 
					colgrnd[j] = g;         //same clstr, grnd undecided -> fix undecided grnd   //S9a20090109
				else 
					g = colgrnd[j];                          //update running grnd for next col  //S9a20090109
				
				if (colgrnd[j]==g && colclstr[j]==QD && q>QD)                                    //S9a20090116 =!=
					colclstr[j] = q;        //same gd, col dk/tr, prv not dk/tr -> fix dk/tr col //S9a20090109
				else 
					q = colclstr[j];                         //update running clstr for next col //S9a20090109
				
if (dbgpic()==picCount || dbgSub(4))  System.out.println("smear from left  "                     //S9a20090116
				+"/pic "+d(5,picCount)+"/t "+d(2,t)+"/x "+d(3,j)+"/colgrnd "+X(1,t1)+"->"		 //S9a20090109
				+X(1,colgrnd[j])+" /colclstr "+X(1,t2)+"->"+X(1,colclstr[j])+" / g "+X(1,t3)+"->"//S9a20090109
				+X(1,g)+" / q "+X(1,t4)+"->"+X(1,q)+((colgrnd[j]!=t1||colclstr[j]!=t2)?"*":" "));//S9a20090109
			}                                                                                    //S9a20090106
			for (int g=0, q=0, j=w-1; j >= 0; j--)            //smear col definitions from RIGHT //S9a20090109
			{                                                                                    //S9a20090106
				t1 = colgrnd[j]; t2 = colclstr[j]; t3 = g; t4 = q;          //for debug          //S9a20090109
				
				if (colgrnd[j]==3 && colclstr[j]==q) 
					colgrnd[j] = g;         //same clstr, grnd undecided -> fix undecided grnd   //S9a20090109
				else 
					g = colgrnd[j];                          //update running grnd for next col  //S9a20090109
				
				if (colgrnd[j]==g && colclstr[j]==QD && q>QD)                                    //S9a20090116 =!=
					colclstr[j] = q;        //same gd, col dk/tr, prv not dk/tr -> fix dk/tr col //S9a20090109
				else 
					q = colclstr[j];                         //update running clstr for next col //S9a20090109
				
if (dbgpic()==picCount || dbgSub(4))  System.out.println("smear from right "                     //S9a20090106
				+"/pic "+d(5,picCount)+"/t "+d(2,t)+"/x "+d(3,j)+"/colgrnd "+X(1,t1)+"->"		 //S9a20090109
				+X(1,colgrnd[j])+" /colclstr "+X(1,t2)+"->"+X(1,colclstr[j])+" / g "+X(1,t3)+"->"//S9a20090109
				+X(1,g)+" / q "+X(1,t4)+"->"+X(1,q)+((colgrnd[j]!=t1||colclstr[j]!=t2)?"*":" "));//S9a20090109
			}                                                                                    //S9a20090106
			
			//If we find a lite grnd area with a clstr having < 4 shades >>in the UCT<< then     //S9a20090116
			//the color background won't be 'b' (to match the lite grnd grad) and we will fail.  //S9a20090116
			//Also lite pxls have been mixed up with top color (both 'e2') over the whole area!  //S9a20090116
			//This cannot be fixed earlier - only now do we know the grnd for every col. One     //S9a20090116
			//day we will fix this, but UK (only known lite grnd) doesn't have sparse >>UCTs<<   //S9a20090116
			//                                                                                   //S9a20090116
			//Tentative solution: when lite pxl enters nondark column set bp12 to 7.   Then      //S9a20090116
			//(here) for any lite area, search and fix the bp12s (need to know # shades in UCT)  //S9a20090116
								
			/*--------use col grnd & quant to set area info (-> select grad in commands)-------*///S9a
			/*-------any remaining undefined grnd (would be a logic err) set to rev video------*///S9a
			for (int q=-1, g=-1, j=0;  j < w; j++)         //create list of areas for PX_CTLI    //S9a
				if (colclstr[j]!=q || colgrnd[j]!=g)       //clustr/grnd different->new area     //S9a20090105
					alist.add(new Area(t,j,(q = colclstr[j]),                                    //S9a20090105
					//4th param=0 makes 'b' pixels transp so avoid this for reverse video        //S9a20090108
					//4th param=1 makes 'b' pixels dark; param = 2 selects grad for rev video    //S9a20090108
					( ((g = colgrnd[j])>=2)?2:  (colgrnd[j]<=0 || dbgTrnsp())?0:  1 )   ));      //S9a20090108
					
			aindx[t+1].tlimit(alist.size());               //update alist index at end of row    //S9a
		}   //end for each textrow                                                               //S9a

if (dbgSub(2)) dumpAreas("areas");                                                               //S9a
	}                                                                                            //S9a20090105

	/*============== Bitmap of 2bit e2e1pb (cleared and built afresh for each subpic) ==============*/

	public  int[] bp12;                             //public so Subpicture.buildRLE can use it   //S9

	/*=============================== methods for Subpicture to call ===============================*/

	private static int     picCount = 0;            //accumulate number of calls to analyse()    //S9

	public  void analyse(Bitmap bitmap, ArrayList user_color_table)                              //S9
	{                                               //called at start of Subpicture.buildRLE()   //S9
	                                                //quantises color table & allocates e2e1pb   //S9
		                                            //creates list of XY areas for CH_COLCON     //S9
		picCount++;                                 //will update pixels to clear noise bursts!! //S9
		if (!active) return;                        //main switch for multicolor DVB to DVD      //S9
		
		if (tables_rdy==0) analyseTCT();            //deconstruct target clut (TCT) from user    //S9i20090116 

if (dbgSub(1)) System.out.println("Analysing pic "+d(5,picCount)                                 //S9 20090105
		  +" from PTS:"+Common.formatTime_1(bitmap.getInTime() / 90)                             //S9 20090105
		  +" until"+Common.formatTime_1(bitmap.getInTime() / 90));                               //S9 20090105

		int pixels[] = bitmap.getPixel();                                                        //S9

		int w = bitmap.getWidth();                                                               //S9
		int h = bitmap.getHeight();                                                              //S9

		/*-----USER clut to quantised hue + luminance + freq + index----*/                       //S9
		int t1=0; int t2=0; int t3=0; int t4=0; int t5=0;                                        //S9



		int[] qCounts = new int[qNames.length];      //UCT index of most freq shade [by clstr #] //S9
		for (int a=0, x=qCounts.length; a<x; a++) qCounts[a] = -1;  //mark clusters as not found //S9 20090116

		//finding most frequently used shade per cluster was only needed for lite grnd clusters
		//and then only when luminance inverted for sort (white on the 'b' bit - old algorithm)
		//if this was not done a lighter than light rogue pixel could bias the bp12 allocation
		//the counters are left in place to show on the biglog for interest and diagnosis

		int[] freq    = new int[user_color_table.size()];    //pixel counts for each shade       //S9
		for (int a=0; a<w*h; a++)                            //count usage of each UCT color     //S9
			{ freq[user_color_table.indexOf("" + pixels[a])]++; t5++; }       //...and total     //S9

//last use of w, h, pixels[] till col analysis


		QLHI  myQLHI;                                                                            //S9
		quants = new QLHI[user_color_table.size()];                                              //S9

		for (int a=0; a<user_color_table.size(); a++)                                            //S9
		{                                                                                        //S9
			t1 = 0xFFFFFFFF & Integer.parseInt(user_color_table.get(a).toString());              //S9
			myQLHI = ARGBtoQLHI(t1,a,0);            //quant + lum + hue + UCT index              //S9 20090116
			t2 = myQLHI.quant();                    //quantised hue (aka hue cluster) of color   //S9
			if (myQLHI.lum() < 0x28)                //sweep dark colors to dark grnd quant       //S9
				{ myQLHI.quant(QD); }               //...BEFORE they are given BP12 values       //S9 20090116 =!=

if (dbgSub(2)) System.out.println("QLHI 1st pass: UCT["+d(3,a)+"]="+X(8,t1)                      //S9
			+" / QLHI="+X(8,ARGBtoQLHI(t1,a,0).N)+s(8,qNames[ARGBtoQLHI(t1,a,0).quant()])        //S9 20090116
			+" / grnded="+X(8,myQLHI.N)+s(8,qNames[myQLHI.quant()])+" count="+d(5,freq[a]));     //S9

			myQLHI.misc(t1>>>24);                   //UCT's alfa to fine tune luminance sort     //S9 20090101
			quants[a] = myQLHI;                     //using UCT index as initial quants index    //S9 20090101
			if (myQLHI.quant() <= QD)               //null, transp, dark quants                  //S9 20090116 =!=
				t5 -= freq[a];                      //un-total them to reveal total text pixels  //S9 20090101
		}   //end for each UCT entry                                                             //S9
		if (t5 == 0)                                //could be zero iff all pixels transp/dark   //S9
Common.setMessage("all pixels transp/dark for pic "+d(5,picCount)                                //S9
			+" at PTS: "+Common.formatTime_1(bitmap.getInTime() / 90));                          //S9


		/*--------map QLHI (and hence UCT) shades to BP12 (max 3)-------*/                       //S9

		Arrays.sort(quants, QLHI_ORDER);       // <----- order by quant then lum gradient        //S9
if (dbgSub(4)) dumpquants("QHLI by quant+lum+alfa ");                                            //S9 20090101

		qCounts = new int[qNames.length];           //reset to count shades per cluster          //S9
		for (int i=0; i<quants.length; i++)         //must be done in ascending clstr+lum        //S9
		{                                           //transp & dark counts stay zero...          //S9
			myQLHI = quants[i];                     //...to invoke special clst_maps[0]          //S9
			t2 = myQLHI.quant();                                                                 //S9
			if (qCounts[t2] == 0)                   //new quant - enable shade counting?         //S9
			    t1 = (t2 > QD) ? 1 : 0; 	        //...only if not dark and not transparent    //S9 20090116 =!=
			qCounts[t2] += t1;                      //if enabled count shades in cluster         //S9
		}                                                                                        //S9

		t2 = 0; t3 = 0; t4 = 0;                     //pass three - map to static PGC CLUT        //S9
		for (int i=0; i<quants.length; i++)         //must be done in ascending clstr+lum        //S9
		{                                                                                        //S9
			myQLHI = quants[i];                                                                  //S9
		    t5 = myQLHI.quant();                    //extract the cluster #                      //S9
		    if (t5 != t4)                           //first shade of new cluster                 //S9
		    {                                                                                    //S9
				t3 = qCounts[t5];                   //count of shades in this cluster            //S9
				t3 = clut_maps[t3];                 //BP12 map for that # of shades              //S9
				t2 = -1;                            //prepare the BP12 generator Igor!           //S9
			    t4 = t5;                            //same map till next cluster                 //S9
			}                                                                                    //S9
			//these are the various overides to the basic map tables...
			if (t5 <= QD)  t2 = 0x3C;               //all transp and dark are forced to 'b'      //S9 20090116 =!=
			if (t3 == 0)  t2 |= 0x3C;               //by design: map exhausted->repeat last BP12 //S9
			//each nibble of the map specifies how many shades map to one subpic value           //S9
			//t2 & 0x3C is count down; next nibble done with 'while' as any map nibble           //S9
			//...can be zero;  t2 & 0x03 is current subpic value (b/p/e1/e2 aka BP12)            //S9
			while (t2 < 4) {t2++;  t2 |= (0x0F & t3)<<2; t3>>=4;} //next BP12 & counter          //S9
			t2 -= 4;                                //countdown on one map nibble                //S9
			myQLHI.bp12(0x03 & t2);                 //assign color to e2/e1/p/b                  //S9
			if (t5 == QN && myQLHI.lum() > 0xB8)    //sweep light white (only) to lite quant     //S9 20090116 =!=
				{ myQLHI.quant(QL); }               //...AFTER they are given BP12 values        //S9 20090116 =!=

			t1 = quants[i].inUCT();                                                   // for dbg //S9 20090101
if (dbgSub(2)) System.out.println("BP12 mapping: QLAI["+d(3,i)+"]="+X(8,quants[i].N)             //S9 20090101
		    +s(8,qNames[quants[i].quant()])+" count="+d(5,freq[t1])                              //S9 20090103
		    +" {qCounts "+d(2,qCounts[t5])+" / maps left "+X(4,t3)+"/t2 "+d(2,t2)                //S9 20090101
		    +"} UCT["+d(3,t1)+"]="+X(8,Integer.parseInt(user_color_table.get(t1).toString())));  //S9 20090101
		}                                                                                        //S9

		Arrays.sort(quants, UCT_ORDER);        // <----- order by contributing UCT index         //S9
if (dbgSub(4)) dumpquants("QHLI by UCTidx ",user_color_table);                                   //S9

		/*--------find how many rows of text in this subpicture---------*/                       //S9
		alist.clear();                                 //we deliver new area list & bp12[]       //S9
		findTextRows(bitmap);                          //alist <- header + 2 entries per textrow //S9 20090102
		                                                                 
		/*-----------within each text row analyse the columns-----------*/                       //S9
		collectAreas(bitmap,  user_color_table);      //alist <- 1 entry per color area          //S9 20090105
		
		
		bitmap.setPixel(pixels);                       //send back updates (to clear noise)!!    //S9
		//pass back fixed up bitmap so other formats (e.g. SON) can benefit but it doesn't work  //S9
	}                                                                                            //S9

	/*==============================================================================================*/
	public byte[] commands(int command_start_pos, int bottom_field_start_pos, Bitmap bitmap)     //S9=
                                                    //called near end of Subpicture.buildRLE()   //S9=
                                                    //has complete control of command buffer     //S9=
	{                                               //includes CHG_COLCON with list of XY areas  //S9=
		if (!active) return new byte[0];            //main switch for multicolor DVB to DVD      //S9=

		ByteArrayOutputStream b = new ByteArrayOutputStream();                                   //S9=
		int DCSQ1_link = 0;                                     //position of link to fix        //S9=
		int DCSQ2_link = 0;                                     //position of link to fix        //S9=
		int DCSQ1_pos  = 0;                                     //offset of the DCSQ             //S9=
		int DCSQ2_pos  = 0;                                     //offset of the DCSQ             //S9=

	  try
	  {
		b.reset();                                                                               //S9=

		//-------------------------------first SP_DCSQ-------------------------------
		DCSQ1_pos = command_start_pos + b.size();               //offset of this DCSQ            //S9=
		b.write(0); b.write(0);                                 //delay                          //S9=
		DCSQ1_link = b.size();                                  //where to fix link              //S9=
		b.write(0); b.write(0);                                 //link to be fixed               //S9=

		b.write(3);                                 //command - static colors                    //S9=
		b.write((byte)((dbgnoCHG())?0x74:0x76));                //force color index e2e1         //S9=
		b.write((byte)((dbgnoCHG())?0x31:0x51));                //force color index p b          //S9=

		b.write(4);                                 //command - static alpha                     //S9=
		b.write((byte)((dbgnoCHG())?0xFF:0x00));                //force alpha index e2e1         //S9=
		b.write((byte)((dbgnoCHG())?0xF8:0x00));                //force alpha index p b          //S9=

		b.write(5);                                 //command - screen position                  //S9=
		int minX = bitmap.getX(); int maxX = bitmap.getMaxX() - 1;                               //S9=
		int minY = bitmap.getY(); int maxY = bitmap.getMaxY() - 1;                               //S9=
		b.write((byte)(minX>>>4)); b.write((byte)(minX<<4 | maxX>>>8)); b.write((byte)maxX);     //S9=
		b.write((byte)(minY>>>4)); b.write((byte)(minY<<4 | maxY>>>8)); b.write((byte)maxY);     //S9=

		b.write(6);                                 //command - pixel data pointers              //S9=
		b.write(0); b.write(4);                                 // top_field follows header      //S9=
		b.write((byte)(bottom_field_start_pos>>>8));            // bottom_field MSB              //S9=
		b.write((byte)(bottom_field_start_pos));                // bottom_field LSB              //S9=

		b.write(1);                                 //command - start display                    //S9=

if (!dbgnoCHG())  //suppress CHG_COLCON (and use weird BP12) to allow examination with supviewer //S9=
{
		//powerful spell needed  because generics are theologically unacceptable in this realm   //S9=
		Area[] areas = (Area[])(alist.toArray(new Area[alist.size()]));   //cast entire list     //S9=

		b.write(7);                                 //command - CHG_COLCON                       //S9=
		int textrows  = areas[0].trow();            //how many rows of text                      //S9=
		int bitmapY   = bitmap.getY();              //top line of 1st row                        //S9=20090109
		int bitmapX   = bitmap.getX();              //left edge on screen                        //S9=20091006
		int paramsize = 6;                          //minimum param size (size + final LN_CTLI)  //S9=20090101
		for (int t = 0; t < textrows; t++)          //for each trow emit LN_CTLI (+ n * PX_CTLI) //S9=20081231
		{                                                                                        //S9=20081231
			int bgnrow = areas[t+1].tfirst();       //index of first area this row               //S9=20081231
			int nxtrow = areas[t+1].tlimit();       //index of first area beyond row             //S9=20081231
			int PX   = nxtrow - bgnrow;	            //number of areas in this row                //S9=20081231
			if (PX > 15)	                        //cannot define > 15 areas                   //S9=20081231
			{	                                                                                 //S9=20081231
Common.setMessage(PX+" areas found for pic "+d(5,picCount)                                       //S9=20081231
				+" at PTS: "+Common.formatTime_1(bitmap.getInTime() / 90)+"/textrow "+d(1,t));   //S9=20081231
				PX = 15;                            //restrict count of areas in row             //S9=20081231
				areas[t+1].tlimit(bgnrow + PX);     //restrict limit of areas in row             //S9=20081231
			}	                                                                                 //S9=20081231
			paramsize += PX * 6 + 4;                //extra param size required for this row     //S9=20090101
		}                                                                                        //S9=20081231
if (dbgSub(2)) System.out.println("CHG_COLCON  alist.size "+d(2,alist.size())                    //S9=
				+"/textrows "+d(2,textrows)+"/paramsize "+d(3,paramsize));                       //S9=
		b.write((byte)(paramsize>>>8));                         //MSB CHG_COLCON param size      //S9=
		b.write((byte)(paramsize    ));                         //LSB CHG_COLCON param size      //S9=
		for (int t = 0; t < textrows; t++)          //for each trow emit LN_CTLI (+ n * PX_CTLI) //S9=
		{                                                                                        //S9=
			int topY = bitmapY + areas[t+1+textrows].trow();    //absolute y for trow top line   //S9=20090103
			int btmY = bitmapY + areas[t+1+textrows].start();   //absolute y trow bottom line    //S9=20090103
			int bgnrow = areas[t+1].tfirst();       //index of first area this row               //S9=
			int nxtrow = areas[t+1].tlimit();       //index of last area to process              //S9=
			int PX   = nxtrow - bgnrow;	            //number of areas in this row                //S9=
			// LN_CTLI defines a horizontal band on screen - used to implement one row of text   //S9=
			// LN_CTLI = 0t tt nb bb : t = top line, b = bottom line, n = # of PX_CTLI following //S9=
			byte[] xxLN = new byte[4];                          //local array to ease debug      //S9=
			xxLN[0] = (byte)(               0x0F & topY>>>8);   //MS 4 bits 1st screen line      //S9=
			xxLN[1] = (byte)(                      topY    );   //LS 8 bits 1st screen line      //S9=
			xxLN[2] = (byte)(0xF0 & PX<<4 | 0x0F & btmY>>>8);   //PX count & MS 4 bits last      //S9=
			xxLN[3] = (byte)(                      btmY    );   //LS 8 bits last screen line     //S9=
			b.write(xxLN);                                                                       //S9=
if (dbgSub(2)) System.out.println("LN_CTLI  "                                                    //S9=
				+X(2,xxLN[0])+" "+X(2,xxLN[1])+"  "+X(2,xxLN[2])+" "+X(2,xxLN[3]));              //S9=

			for (int p=bgnrow; p<nxtrow; p++)       //for each color area along row emit PX_CTLI //S9=
			{                                                                                    //S9=
				int firstCol = areas[p].start() + bitmapX;   //from left of screen, not bitmap!  //S9=20090106
				int cluster  = areas[p].clstr();                                                 //S9=
				int neutral  = areas[p].bgrnd();                                                 //S9=

//				cluster = (cluster+2-Q1)%(TCT_q_free-Q1)+Q1; //cheap trick to test more grads    //S9q20090117 =+=
				int color_e2e1bp = 0;                                                            //S9=20090116
				if (dbgDynQnt())                                                                 //S9=20090116
					color_e2e1bp = TCTQ_grads[cluster + ((neutral==2)?16:0)]; //2=lite grnd      //S9=20090116
				else                                                                             //S9=20090116
					color_e2e1bp = clut_eepb[cluster + ((neutral==2)?16:0)];  //2=lite grnd      //S9=20090116
				int alpha_e2e1bp = (areas[p].bgrnd() > 0) ? 0xFFFF : 0xFFF0;  //0 -> trnsp bgd   //S9=20090108
				
				
			// PX_CTLI redefines color & alpha from a given column onward within the LN_CTLI     //S9=
			// PX_CTLI = ss ss cc cc aa aa /s=start col, c=color index e2e1pb, a=alpha e2e1pb    //S9=
				byte[] xxPX = new byte[6];                      //local array to ease debug      //S9=
				xxPX[0] = (byte)(firstCol    >>>8);             //MSB 1st column new colour      //S9=
				xxPX[1] = (byte)(firstCol        );             //LSB 1st column new colour      //S9=
				xxPX[2] = (byte)(color_e2e1bp>>>8);             //new color indices e2+e1        //S9=
				xxPX[3] = (byte)(color_e2e1bp    );             //new color indices p + b        //S9=
				xxPX[4] = (byte)(alpha_e2e1bp>>>8);             //new  alpha values e2+e1        //S9=
				xxPX[5] = (byte)(alpha_e2e1bp    );             //new  alpha values p + b        //S9=
				b.write(xxPX);                                                                   //S9=
if (dbgSub(2)) System.out.println("PX_CTLI  "+X(2,xxPX[0])+" "+X(2,xxPX[1])+"  "                 //S9=
				+X(2,xxPX[2])+" "+X(2,xxPX[3])+"  "+X(2,xxPX[4])+" "+X(2,xxPX[5]));              //S9=
			}                                                                                    //S9=
		}                                                                                        //S9=
		byte[] xxZZ = { 15, -1, -1, -1};            //final LN_CTLI (always 0F FF FF FF)         //S9=
		b.write(xxZZ);                                                                           //S9=
if (dbgSub(2)) System.out.println("LN_CTLI  "                                                    //S9=
				+X(2,xxZZ[0])+" "+X(2,xxZZ[1])+"  "+X(2,xxZZ[2])+" "+X(2,xxZZ[3]));              //S9=

}       //end debug without CHG_COLCON                                                           //S9=

		b.write(-1);                                //command - end of DCSQ                      //S9=

		//-------------------------------second SP_DCSQ------------------------------
		DCSQ2_pos = command_start_pos + b.size();               //offset of this DCSQ            //S9=
		b.write((byte)(bitmap.getPlayTime()  >>>8));            //delay for end display          //S9=
		b.write((byte)(bitmap.getPlayTime()      ));            //delay for end display          //S9=
		DCSQ2_link = b.size();                                  //where to fix link              //S9=
		b.write(0); b.write(0);                                 //link to be fixed               //S9=
		b.write( 2);                                //command - stop display                     //S9=
		b.write(-1);                                //command - end of DCQS                      //S9=

		b.flush();                                                                               //S9=
	  } catch (IOException e) {                                                                  //S9=

		Common.setExceptionMessage(e);                                                           //S9=
	  }                                                                                          //S9=
		byte[] ret = b.toByteArray();                                                            //S9=
		ret[DCSQ1_link  ] = (byte) (DCSQ2_pos >>>8);            //link up the DCSQ chain         //S9=
		ret[DCSQ1_link+1] = (byte) (DCSQ2_pos     );            //DCSQ1 points to DCSQ2          //S9=
		ret[DCSQ2_link  ] = (byte) (DCSQ2_pos >>>8);            //link up the DCSQ chain         //S9=
		ret[DCSQ2_link+1] = (byte) (DCSQ2_pos     );            //DCSQ2 points to itself         //S9=
		//anything more complicated and (e.g.) PowerDVD will ignore the delays!                  //S9=

		return ret;                                             //caller must do word alignment  //S9=
	}                                                                                            //S9=

	/*==============================================================================================*/

	private static long    supCount = 0;    //accumulate byte position in sup file                                //S9

	public  void dumpHdrAndCmd(byte b[]) {                                                                        //S9

		//this is >application< debug code - it relies on the subpic being correctly formed                       //S9
		//therefore it should also work for the original Project X (before multicolor commands)                   //S9
		//it will almost certainly break if the RLE header pointers are wrong                                     //S9


		System.out.println("SUPADR= "+X(8,(int)supCount)+"   pic "+d(5,picCount));              //addr in supfile //S9

		System.out.println(">header "+X(2,b[0])+" "+X(2,b[1])+" start "+X(2,b[2])+" "+X(2,b[3])+" "+X(2,b[4]));   //S9
		System.out.println(" length "+X(2,b[10])+" "+X(2,b[11])+"  cmds "+X(2,b[12])+" "+X(2,b[13]));             //S9

		int c = 10 + ((b[12] & 0xFF)<<8 | b[13] & 0xFF);                                     //offset to commands //S9
		int e = 10 + ((b[10] & 0xFF)<<8 | b[11] & 0xFF);                                //offset to end of buffer //S9
//		assert (c <= e);                                                     //defensive for malformed RLE header //S9
		while (c < e)                                                                  //do entire command buffer //S9
		{
		System.out.println("SUPADR= "+X(8,(int)supCount+c)+" offset "+X(4,c-10));               //addr in supfile //S9
		System.out.println(">delay  "+X(2,b[c++])+" "+X(2,b[c++])+"  next "+X(2,b[c++])+" "+X(2,b[c++]));         //S9
	cmd:  while (c < e)                                                                 //commands in one SP_DCSQ //S9
		  {
			//		System.out.println("c "+X(4,c)+"/e "+X(4,e));                                                 //S9dbg
			switch (b[c]) {                                                                                       //S9
			case 1: System.out.println(" start  "+X(2,b[c++]));                                           break;  //S9
			case 2: System.out.println(" stop   "+X(2,b[c++]));                                           break;  //S9
			case 3: System.out.println(" color  "+X(2,b[c++])+" "+X(2,b[c++])+" "+X(2,b[c++]));           break;  //S9
			case 4: System.out.println(" alpha  "+X(2,b[c++])+" "+X(2,b[c++])+" "+X(2,b[c++]));           break;  //S9
			case 5: System.out.println(" screen "+X(2,b[c++])+" "+X(2,b[c++])+" "+X(2,b[c++])+" "+X(2,b[c++])     //S9
					                         +" "+X(2,b[c++])+" "+X(2,b[c++])+" "+X(2,b[c++]));           break;  //S9
			case 6: System.out.println(" fields "+X(2,b[c++])+" "+X(2,b[c++])+" "+X(2,b[c++])                     //S9
					                         +" "+X(2,b[c++])+" "+X(2,b[c++]));                           break;  //S9
			case 7: System.out.println(" change "+X(2,b[c++])+"   paramsz "+X(2,b[c++])+" "+X(2,b[c++]));         //S9
					System.out.println(" line1  "+X(2,b[c++])+" "+X(2,b[c++])+"  "+X(2,b[c++])+" "+X(2,b[c++]));  //S9
					for(int pm=((0xFF&b[c-6])<<8|0xFF&b[c-5])-6, n=1; pm > 0; pm-=4, n++) { //pm=param bytes left //S9
					  for(int px=(0xFF&b[c-2])>>4; px > 0; px--, pm-=6) {               //px=PX_CTLI (areas) left //S9 20081231
			//		System.out.println("pm "+d(3,pm)+"/px "+d(3,px)+"/c "+X(4,c));                                //S9dbg
					System.out.println(" area"+n+"  "+X(2,b[c++])+" "+X(2,b[c++])                                 //S9
					                         +"  "+X(2,b[c++])+" "+X(2,b[c++])+"  "+X(2,b[c++])+" "+X(2,b[c++])); //S9
					  }                                                                                           //S9
			//		System.out.println("pm "+d(3,pm)+"/      /c "+X(4,c));                                        //S9dbg
					System.out.println(" line"+(n+1)                                                              //S9
					                         +"  "+X(2,b[c++])+" "+X(2,b[c++])+"  "+X(2,b[c++])+" "+X(2,b[c++])); //S9
					}                                                                                     break;  //S9
			case -1:System.out.println(" quit   "+X(2,b[c++]));                      break cmd; //exit inner loop //S9
			default:System.out.println(" other  "+X(2,b[c++]));                                           break;  //S9
		    }         //end switch                                                                                //S9
		  }         //end while one SP_DCSQ                                                                       //S9
		  while (c < e && b[c] == -1)                                                    //0xFF unlikely as delay //S9
		  {                                                                                                       //S9
					System.out.println(" filler "+X(2,b[c++]));                                                   //S9
		  }         //end while padding                                                                           //S9
		}         //end while command buffer                                                                      //S9
		supCount += c;                                                                                            //S9
		System.out.println("SUPADR= "+X(8,(int)supCount+c)+"   size "+X(4,e-10));               //addr in supfile //S9
	}                                                                                                             //S9
}                                                                                                                 //S9
