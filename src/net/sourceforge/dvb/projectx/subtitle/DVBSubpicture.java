/*
 * @(#)DVBSubpicture.java - decodes DVB subtitles
 *
 * Copyright (c) 2004-2009 by dvb.matt, All rights reserved
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
 * example of a basic implementation of a DVB subtitle decoder
 * 
 * it does not yet implement export (only log it) of encoded string characters, only bitmapped pictures
 * 
 */
/*
 * stenographic subtitling patch (BBC) by Duncan (Shannock9) UK
 * 2008-12
 */

package net.sourceforge.dvb.projectx.subtitle;

//DM24042004 081.7 int02 introduced

import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Arrays;

import net.sourceforge.dvb.projectx.common.Resource;
import net.sourceforge.dvb.projectx.common.Common;
import net.sourceforge.dvb.projectx.common.Keys;

import net.sourceforge.dvb.projectx.subtitle.ColorAreas;                                         //S9

public class DVBSubpicture extends Object {

	private byte data[];

	private int width = Common.getSettings().getBooleanProperty(Keys.KEY_SubtitlePanel_enableHDSub) ? 1920 : 720;
	private int height = Common.getSettings().getBooleanProperty(Keys.KEY_SubtitlePanel_enableHDSub) ? 1088 : 576;

	private int BytePosition;
	private int BitPosition;

	private Graphics2D big;
	private BufferedImage bimg;
	private Epoch epoch;
	private Page page;
	private Region region;
	private CLUT clut;
	private OBJECT object;
	private Hashtable epoches = new Hashtable();

	private int table_CLUT_8bit[];
	private int IRD;
	private int epoch_id = 0;
	private boolean biglog;
	private int[] pixel_data;
	private int[] preview_pixel_data;
	private long pts;
	private int from_index;
	private int to_index;
	private boolean picture_saved;
	private boolean save;
	private boolean preview_visible = false;
	private int fix_page_id;

	//DM13062004 081.7 int04 add
	private Hashtable user_table = new Hashtable();
	private boolean user_table_enabled;

	//DM30072004 081.7 int07 add
	private boolean global_error = false;

	public DVBSubpicture()
	{
		table_CLUT_8bit = generateDefaultCLUT_8Bits();
		setIRD(8, user_table, false, ""); 	//DM13062004 081.7 int04 changed
	}

	public void setIRD(int val, Hashtable table, boolean log, String page_id_str)
	{
		IRD = val;  //2,4,8 = 4,16,256-color support

		if (!table.isEmpty())
			IRD = Integer.parseInt(table.get("model").toString().trim());

		//goes inactive if bit0 is not set through model ID
		ColorAreas.initialise(IRD, log, table);                           //stream startup event //S9 20090113

		//sets full 8 bit decode and disable colortable if CA is active
		if ((IRD & 1) == 1)
		{
			IRD = 8;
			user_table = null;
			user_table_enabled = false;
		}

		else
		{
			user_table = table;
			user_table_enabled = !user_table.isEmpty();
		}

		biglog = log;
		resetEpoch();

		if (page_id_str.trim().length() != 0)
			fix_page_id = Integer.parseInt(page_id_str.trim());
		else
			fix_page_id = -1;

		preview_visible = false;
	}

	private void addBigMessage(String msg)
	{
		if (!biglog)
			return;

		System.out.println(msg);
	}

	private void resetEpoch()
	{
		epoches.clear();
		epoch = setEpoch(epoch_id);
		preview_pixel_data = new int[width * height];
		picture_saved = false;
		save = false;
	}

	private int getBits(int N)
	{
		int Pos, Val;
		Pos = BitPosition>>>3;

		//DM03082004 081.7 int07 add
		if (Pos >= data.length - 4)
		{
			global_error = true;
			BitPosition += N;
			BytePosition = BitPosition>>>3;
			return 0;
		}

		Val =   (0xFF & data[Pos])<<24 |
			(0xFF & data[Pos+1])<<16 |
			(0xFF & data[Pos+2])<<8 |
			(0xFF & data[Pos+3]);

		Val <<= BitPosition & 7;
		Val >>>= 32-N;

		BitPosition += N;
		BytePosition = BitPosition>>>3;

		return Val;
	}

	private int nextBits(int N)
	{
		int Pos, Val;
		Pos = BitPosition>>>3;

		//DM03082004 081.7 int07 add
		if (Pos >= data.length - 4)
		{
			global_error = true;
			return 0;
		}

		Val =   (0xFF & data[Pos])<<24 |
			(0xFF & data[Pos+1])<<16 |
			(0xFF & data[Pos+2])<<8 |
			(0xFF & data[Pos+3]);

		Val <<= BitPosition & 7;
		Val >>>= 32-N;

		return Val;
	}

	private void flushBits(int N)
	{
		BitPosition += N;
		BytePosition = BitPosition>>>3;
	}

	private void alignToByte()
	{
		alignToByte(1);
	}

	private void alignToByte(int N)
	{
		while ( (7 & BitPosition) != 0 )
			flushBits(N);
	}

	private void alignToWord()
	{
		if ((1 & BytePosition) != 0 && nextBits(8) != 0x0F)
			flushBits(8);
	}

	public int getTimeOut()
	{
		return page.getTimeOut();
	}

	public int decodeDVBSubpicture(byte packet[], int BPos[], Graphics2D big, BufferedImage bimg, long pts, boolean save, boolean preview_visible)
	{
		data = packet;  //packet + 4 bytes overhead
		BytePosition = BPos[0];  //bytpos
		BitPosition = BPos[0]<<3;  //bitpos
		this.big = big;
		this.bimg = bimg;
		this.pts = pts;
		this.save = save;
		this.preview_visible = preview_visible;

		picture_saved = false;

		//DM30072004 081.7 int07 add
		global_error = false;

		flushBits(8); //padding
		int stream_ident = getBits(8); // 0 = std
		int segment_type = 0;
		int sync_byte = 0;

		//DM30072004 081.7 int07 changed
		//while ( nextBits(8) == 0x0F )
		for (int ret; BytePosition < data.length - 4; )
		{
			ret = nextBits(8);

			addBigMessage("ret " + Integer.toHexString(ret) + " /bi " + BitPosition + " /by " + BytePosition);

			if (ret == 0xFF)
				break;

			if (ret != 0x0F)
			{
				flushBits(8);
				continue;
			}

			segment_type = Subtitle_Segment();

			alignToByte();

			if (global_error && region != null)
				region.setError(4);
		}

		if ( nextBits(8) == 0xFF )
		{}

		if (picture_saved)
			return -1;

		return -2;
	}

	private int Subtitle_Segment()
	{
		if ( getBits(8) != 0x0F ) //syncbyte
			return -1;

		int segment_type = getBits(8);

		int page_id = getBits(16); //page_id

		if (fix_page_id >= 0 && page_id != fix_page_id) // exclude unwanted pages 
		{
			stuffing();
			return 0xFF;
		}

		page = epoch.setPage(page_id);

		addBigMessage("segm: 0x" + Integer.toHexString(segment_type) + " / " + pts);

		switch (segment_type)
		{
		case 0x10:
			return page_composition();
			//return segment_type;

		case 0x11:
			region_composition();
			return segment_type;

		case 0x12:
			CLUT_definition();
			return segment_type;

		case 0x13:
			object_data();
			return segment_type;

		case 0x80:
			end_display();
			return segment_type;

		case 0xFF:
			stuffing();
			return segment_type;

		default:
			stuffing();
			return segment_type;
		}
	}

	private void end_display()
	{
		int segment_length = getBits(16);
		flushBits(segment_length * 8); //DM18062004 081.7 int05 changed
	}

	private void stuffing()
	{
		int segment_length = getBits(16); //+ BytePosition;
		flushBits(segment_length * 8); //DM18062004 081.7 int05 changed
	}

	private void prepare_output()
	{
		//DM26052004 081.7 int03 changed
		//long new_time_out = (long)Math.round((pts - page.getTimeIn()) / 900.0); 1000
		long new_time_out = 1L + ((pts - page.getTimeIn()) / 1024);

		if (page.getTimeOut() > 0 && new_time_out > page.getTimeOut()) // maybe wrong
			new_time_out = page.getTimeOut();
	//	if (page.getTimeOut() > 0 && new_time_out > (page.getTimeOut() / 10))
	//		new_time_out = page.getTimeOut() / 10;
		if (page.getTimeOut() > 0 && pts == -1) // -1 means take proposed play time
			new_time_out = page.getTimeOut() / 10;

//
		if (page.getTimeOut() > 0 && Common.getCollection().getSettings().getBooleanProperty(Keys.KEY_SubtitlePanel_rebuildPictPTS)) // -1 means take proposed play time
			new_time_out = page.getTimeOut() / 10;
//

//Common.setMessage("!> debug Info : px " + page.getX() + " / py " + page.getY() + " / pw " + page.getWidth() + " / ph " + page.getHeight() + " (VN): " + page.getVersionNumber());

		//reduce pixel size
		page.reduce_size(preview_pixel_data, width);


		if (page.getWidth() <= 0 || page.getHeight() <= 0 || page.getWidth() > width || page.getHeight() > height)
		{
			Common.setMessage("!> Page ignored (VN): " + page.getVersionNumber() + "; (size error) " + page.getWidth() + " * " + page.getHeight());
			return;
		}

		int page_pixel_data[] = new int[page.getWidth() * page.getHeight()];

		for (int y = 0; y < page.getHeight(); y++)
				System.arraycopy(preview_pixel_data, page.getX() + ((page.getY() + y) * width), page_pixel_data, y * page.getWidth(), page.getWidth());

		if (page.getWriteStatus())
			BMP.savePixels( new Bitmap( page.getX(), page.getY(), page.getWidth(), page.getHeight(), page_pixel_data, IRD, page.getId(), region.getId(), 0, page.getTimeIn(), (int)new_time_out));

		if (preview_visible)
			bimg.setRGB(page.getX(), page.getY(), page.getWidth(), page.getHeight(), page_pixel_data, 0, page.getWidth());

		paintRegionBorder(page.getX(), page.getY(), page.getWidth(), page.getHeight());

		picture_saved = true;

		addBigMessage("time: in " + page.getTimeIn() + " /len " + new_time_out + " /save " + save + " /prev " + preview_visible    //S9dbg
					+ "                               <<prepare.output>>");                                                        //S9dbg
	}


	private int page_composition()
	{
		int segment_end = getBits(16) + BytePosition;
		int segment_type = 0x10;

		int time_out = getBits(8) * 100; //page_time_out, milliseconds (here ticks) 'til disappearing without another erase event

		page.setVersionNumber(getBits(4)); //page_version_number, if number exists, update isn't necessary

		page.setState(getBits(2)); //page_state 
		//0 = only updates of this page contents
		//1 = new page instance, all contents with new definitions
		//2 = new epoch, see 1, reset all, complete dec. model (IRD) changes possible

		flushBits(2);

		addBigMessage("pagecomp: state " + page.getState() + " /page " + page.getId() + " /pv " + page.getVersionNumber() + " /to " + time_out   //S9dbg
		              + "                      " + ((page.getState()<1) ? "" : (page.getState()<2) ? "<<acquisition pt>>" : "<<new epoch>>"));   //S9dbg

		if (page.getState() > 0)
		{
			clearBackground();
			page.clearArea();
		}

		for (Enumeration e = epoch.getRegions(); e.hasMoreElements() ; )
		{
			region = epoch.setRegion(Integer.parseInt(e.nextElement().toString()));

			//DM23062004 081.7 int05 add
			if (region.getErrors() > 0)
			{
				Common.setMessage(Resource.getString("subpicture.msg.error.dvbdecoding", "" + region.getErrors(), "" + region.getId(), "" + page.getTimeIn()));

				if ((region.getErrors() & 4) != 0)
				{
					Common.setMessage("!> Region ignored (VN): " + region.getVersionNumber());
					region.setActive(false);
				}
			}

			addBigMessage("enum: region " + region.getId() + " /err " + region.getErrors() + " /acti " + region.isActive() + " /chng " + region.isChanged() + " /ti_o " + time_out);

			if ( page.getState() == 0 && time_out > 6000 )
				continue;


			region.setError(0); //DM23062004 081.7 int05 add

			if ( !region.isActive() || !region.isChanged() )
				continue;

			region.setChanged(false);
			pixel_data = region.getPixel();

			page.addArea(region.getXBound(), region.getYBound(), region.getWidth(), region.getHeight());

			try {

				for (int a = 0; a < region.getHeight(); a++)
					System.arraycopy(pixel_data, a * region.getWidth(), preview_pixel_data, region.getXBound() + ((region.getYBound() + a) * width), region.getWidth());

			} catch (Exception ex) {

				region.setError(8);
				Common.setMessage(Resource.getString("subpicture.msg.error.dvbdecoding", "" + region.getErrors(), "" + region.getId(), "" + page.getTimeIn()));
			}

			segment_type = 0x80;

			addBigMessage("addToBMP: region " + region.getId());
			addBigMessage("newSize: x " + page.getX() + " y " + page.getY() + " w " + page.getWidth() + " h " + page.getHeight());
		}

		if (segment_type == 0x80)
			prepare_output();

		if (page.getState() > 0) //if not update, page content has to die
		{
			page = epoch.newPage(page.getId()); 
			page.setTimeIn(pts);
			page.setWriteStatus(save);
			epoch.clearRegions();
			epoch.clearObjects();
		}

		page.setTimeIn(pts);                          // for stenographic  subtitling    //S9

		page.setTimeOut(time_out); //page_time_out, seconds to stand

		//empty pages may define CLUTs without regions
		while (BytePosition < segment_end)
		{
			region = epoch.setRegion(getBits(8)); //region_ids of this epoch
			region.setActive(true);
			region.setChanged(true);

			flushBits(8);

			region.setHorizontalAddress(getBits(16)); // start_x
			region.setVerticalAddress(getBits(16));   // start_y

			addBigMessage("addreg: reg " + region.getId() + " /x " + region.getXBound() + " /y " + region.getYBound());
		}

		return segment_type;
	}

	private void region_composition()
	{
		int segment_end = getBits(16) + BytePosition;

		region = epoch.setRegion(getBits(8)); //region_id
		region.setVersionNumber(getBits(4)); //region_version_number, if number exists, update isn't necessary
		region.setFillFlag(getBits(1));

		flushBits(3);

		int old_w = region.getWidth();
		int old_h = region.getHeight();

		region.setWidth(getBits(16));
		region.setHeight(getBits(16));
		region.setLevelOfCompatibility(getBits(3));
		region.setDepth(getBits(3));

		flushBits(2);

		int CLUT_id = getBits(8);
		clut = epoch.setCLUT(CLUT_id); //CLUT_id

		region.setCLUT_id(CLUT_id);

		//background pixel code
		region.setPixelCode_8bit(getBits(8));
		region.setPixelCode_4bit(getBits(4));
		region.setPixelCode_2bit(getBits(2));

		if (!region.isActive() || !region.getFillFlag())              //retain prev obj data     //S9
		{
			if (old_w == region.getWidth() && old_h == region.getHeight())
				pixel_data = region.getPixel();                   //...for stenographic      //S9
			else                                                                             //S9
				pixel_data = region.initPixel();
		}
		else                                                                             //S9
			pixel_data = region.initPixel();

		if (pixel_data == null)                                 //but during acquisition   //S9
			pixel_data = region.initPixel();                  //...need dummy previous   //S9

		flushBits(2);

		paintRegionBackground();

		addBigMessage("regcomp: page " + page.getId() + " /reg " + region.getId() + " /rv " + region.getVersionNumber()
				+ " /rw " + region.getWidth() + " /rh " + region.getHeight() + " /pxl " + pixel_data.length
				+ " /lv " + region.getCompatibility() + " /clut " + clut.getId()
				+ " /activ " + region.isActive() + " /fill " + region.getFillFlag());                              //S9

		while (BytePosition < segment_end)
		{
			object = epoch.setObject(getBits(16)); //object_id
			object.setRegionId(region.getId());
			object.setType(getBits(2));
			object.setProvider(getBits(2));
			object.setHorizontalPosition(getBits(12));

			flushBits(4);

			object.setVerticalPosition(getBits(12));

			if (object.getType() == 1 || object.getType() == 2) //character or ch.strings
			{
				object.setForegroundCode(getBits(8)); //foreground_pixel_code
				object.setBackgroundCode(getBits(8)); //background_pixel_code
			}

			addBigMessage("addobj: reg " + region.getId() + " /obj " + Integer.toHexString(object.getId()).toUpperCase() + " /x " + object.getHorizontalPosition() + " /y " + object.getVerticalPosition());
		}
	}

	private void CLUT_definition()
	{
		int segment_end = getBits(16) + BytePosition;

		clut = epoch.setCLUT(getBits(8)); //CLUT_id
		clut.setVersionNumber(getBits(4)); //CLUT_version_number, if number exists, update isn't necessary

		addBigMessage("clutcomp: " + clut.getId() + " /v " + clut.getVersionNumber());

		flushBits(4);

		//user table
		//DM13062004 081.7 int04 add
		if (user_table_enabled)
		{
			setUserClut();
			flushBits( (segment_end - BytePosition) * 8);
			return;
		}

		while (BytePosition < segment_end)
		{
			int CLUT_entry_id = getBits(8);

			int CLUT_flag_2bit_entry = getBits(1);
			int CLUT_flag_4bit_entry = getBits(1);
			int CLUT_flag_8bit_entry = getBits(1);
			int flag = CLUT_flag_8bit_entry<<3 
					| CLUT_flag_4bit_entry<<2
					| CLUT_flag_2bit_entry<<1;

			flushBits(4);

			int full_range_flag = getBits(1);
			int ARGB, Y, Cr, Cb, T;

			if (full_range_flag == 1)
			{
				Y = getBits(8);
				Cr = getBits(8);
				Cb = getBits(8);
				T = getBits(8);
			}
			else //only MSB transmitted
			{
				Y = getBits(6)<<2;
				Cr = getBits(4)<<4;
				Cb = getBits(4)<<4;
				T = getBits(2)<<6;
			}

			ARGB = YUVtoRGB(Y, Cr, Cb, T);

			addBigMessage("addclut: " + CLUT_entry_id + " /flag " + Integer.toHexString(flag).toUpperCase() + " /ARGB " + Integer.toHexString(ARGB).toUpperCase() + " /range " + full_range_flag);

			for (int i=0; i<3; i++)
				clut.setClutEntry(mapColorIndex(CLUT_entry_id, getRegionDepth(), 2<<i), (flag & 2<<i), ARGB);
		}
	}

	private void object_data()
	{
		int segment_end = getBits(16) + BytePosition;

		object = epoch.setObject(getBits(16)); //object_id

		int region_id = object.getRegionId();

		if (region_id < 0)
		{
			flushBits( (segment_end - BytePosition) * 8);  //DM18062004 081.7 int05 changed
			addBigMessage("object_id " + object.getId() + " with no region_id");
			return;
		}

		region = epoch.setRegion(region_id); //use the right region
		region.resetXY();

		pixel_data = region.getPixel();

		addBigMessage("objdata: reg " + region.getId() + " /obj " + Integer.toHexString(object.getId()).toUpperCase());

		object.setVersionNumber(getBits(4)); //object_version_number, if number exists, update isn't necessary
		int object_coding_method = getBits(2);

		object.setNonModify(getBits(1)); //non_modifying_colour_flag
		flushBits(1);

		//assign region specific CLUT
		clut = epoch.setCLUT(region.getCLUT_id());

		/**
		 * modify user_table here, if no clut definition were sent
		 */
		if (user_table_enabled && clut.getModifyFlags() == 0)
			setUserClut();

		if (object_coding_method == 0) //pixels
		{
			int top_field_data_block_end = getBits(16);
			int bottom_field_data_block_end = getBits(16);
			object.setCopyTopFieldFlag(bottom_field_data_block_end);

			top_field_data_block_end += BytePosition;

			while (BytePosition < top_field_data_block_end)
				pixel_block();

			region.nextField();
			bottom_field_data_block_end += BytePosition;

			while (BytePosition < bottom_field_data_block_end) //if 0-length, copy top field line
				pixel_block();

			//alignToWord(); // flush 8 bits if not aligned
		}

		if (object_coding_method == 1) //text chars
		{
			int number_of_codes = getBits(8); //all chars in a line

			int character_code;
			String str = "";

			for (int i=0; i < number_of_codes; i++)
				str += (char)(character_code = getBits(16));

			addBigMessage("chars: " + str);

			paintStringObjects(region.getX(0), region.getY(), str);
		}
	}

	private void pixel_block()
	{
		int data_type = getBits(8);

		if (data_type == 0x10)
		{
			while (pixel_code_string_2bit() > 0)
			{}

			alignToByte(); // flush 2 bits if not aligned
		}

		else if (data_type == 0x11)
		{
			while (pixel_code_string_4bit() > 0)
			{}

			alignToByte(); //flush 4 bits if not aligned
		}

		else if (data_type == 0x12)
		{
			while (pixel_code_string_8bit() > 0)
			{}
		}

		else if (data_type == 0x20)
			for (int a=0; a<4; a++) //getBits(16) = 4 entries
				object.setMapTable_2to4bit( a, getBits(4));

		else if (data_type == 0x21)
			for (int a=0; a<4; a++) //getBits(32) = 4 entries
				object.setMapTable_2to8bit( a, getBits(8));

		else if (data_type == 0x22)
			for (int a=0; a<16; a++)  //getBits(128) = 16 entries
				object.setMapTable_4to8bit( a, getBits(8));

		else if (data_type == 0xF0)
		{
			if (object.getCopyTopFieldFlag())
				paintCopiedField(region.getX(0), region.getY() + 1, region.getWidth());

			region.nextLine();
		}
	}

	private int pixel_code_string_2bit()
	{
		if (nextBits(2) != 0)
			paintPixelLine(region.getX(1), region.getY(), 1, getBits(2), 2);

		else
		{
			flushBits(2);

			if (getBits(1) == 1)  //switch_1
			{
				int run_length_3to10 = getBits(3) + 3; //pixel_len + 3
				paintPixelLine(region.getX(run_length_3to10), region.getY(), run_length_3to10, getBits(2), 2);
			}
			else
			{
				int switch_2 = getBits(1);

				if (switch_2 == 1)
					paintPixelLine(region.getX(1), region.getY(), 1, 0, 2);

				else if (switch_2 == 0)
				{
					int switch_3 = getBits(2);

					if (switch_3 == 0)
					 	return 0; // end_of_string_signal = 0

					else if (switch_3 == 1)
						paintPixelLine(region.getX(2), region.getY(), 2, 0, 2);

					else if (switch_3 == 2)
					{
						int run_length_12to27 = getBits(4) + 12; //pixel_len + 29
						paintPixelLine(region.getX(run_length_12to27), region.getY(), run_length_12to27, getBits(2), 2);
					}

					else if (switch_3 == 3)
					{
						int run_length_29to284 = getBits(8) + 29;  //pixel_len + 29
						paintPixelLine(region.getX(run_length_29to284), region.getY(), run_length_29to284, getBits(2), 2);
					}
				}
			}
		}

		return 1;
	}


	private int pixel_code_string_4bit()
	{
		if (nextBits(4) != 0) //1 pixel of color enry 1..15
			paintPixelLine(region.getX(1), region.getY(), 1, getBits(4), 4);

		else
		{
			flushBits(4);

			if (getBits(1) == 0)  //switch_1
			{
				if (nextBits(3) != 0)
				{
					int run_length_3to9 = getBits(3) + 2; //pixel_len + 2
					paintPixelLine(region.getX(run_length_3to9), region.getY(), run_length_3to9, 0, 4);
				}
				else
					return getBits(3); // end_of_string_signal = 0
			}
			else
			{
				if (getBits(1) == 0) //switch_2
				{
					int run_length_4to7 = getBits(2) + 4; //pixel_len + 4
					paintPixelLine(region.getX(run_length_4to7), region.getY(), run_length_4to7, getBits(4), 4);
				}
				else
				{
					int switch_3 = getBits(2);

					if (switch_3 < 2)
					{
						int run_length_1to2 = switch_3 + 1; //pixel_len (1 or 2)
						paintPixelLine(region.getX(run_length_1to2), region.getY(), run_length_1to2, 0, 4);
					}

					else if (switch_3 == 2)
					{
						int run_length_9to24 = getBits(4) + 9; //pixel_len + 9
						paintPixelLine(region.getX(run_length_9to24), region.getY(), run_length_9to24, getBits(4), 4);
					}

					else if (switch_3 == 3)
					{
						int run_length_25to280 = getBits(8) + 25; //pixel_len + 25
						paintPixelLine(region.getX(run_length_25to280), region.getY(), run_length_25to280, getBits(4), 4);
					}
				}
			}
		}

		return 1;
	}

	private int pixel_code_string_8bit()
	{
		int pixel_code_8bit;

		if (nextBits(8) != 0)
			paintPixelLine(region.getX(1), region.getY(), 1, getBits(8), 8);

		else
		{
			flushBits(8);

			if (getBits(1) == 0) //switch_1
			{
				if (nextBits(7) != 0)
				{
					int run_length_1to127 = getBits(7); //pixel_len 
					paintPixelLine(region.getX(run_length_1to127), region.getY(), run_length_1to127, 0, 8);
				}
				else
					return getBits(7); //end_of_string_signal = 0
			}
			else
			{
				int run_length_3to127 = getBits(7); //pixel_len with colorindex in next 8bits
				paintPixelLine(region.getX(run_length_3to127), region.getY(), run_length_3to127, getBits(8), 8);
			}
		}

		return 1;
	}

	private int YUVtoRGB(int Y, int Cr, int Cb, int T)
	{
		if (Y == 0)
			return 0;

		int R = (int)((float)Y +1.402f * (Cr-128));
		int G = (int)((float)Y -0.34414 * (Cb-128) -0.71414 * (Cr-128));
		int B = (int)((float)Y +1.722 * (Cb-128));
		R = R < 0 ? 0 : (R > 0xFF ? 0xFF : R);
		G = G < 0 ? 0 : (G > 0xFF ? 0xFF : G);
		B = B < 0 ? 0 : (B > 0xFF ? 0xFF : B);
		T = 0xFF - (T < 0 ? 0 : (T > 0xFF ? 0xFF : T));

		return (T<<24 | R<<16 | G<<8 | B);
	}

	private int[] generateDefaultCLUT_8Bits()
	{
		int table[] = new int[256];

		for (int i=0; i<256; i++)
			table[i] = generateClutEntry_8Bits(i);

		return table;
	}

	private int generateClutEntry_4Bits(int i)
	{
		int T, R, G, B;

		if ((i & 8) == 0)
		{
			if ((i & 7) == 0)
				T = R = G = B = 0;

			else
			{
				R = (i & 1) != 0 ? 0xFF : 0;
				G = (i & 2) != 0 ? 0xFF : 0;
				B = (i & 4) != 0 ? 0xFF : 0;
				T = 0xFF;
			}
		}
		else
		{
			R = (i & 1) != 0 ? 0x80 : 0;
			G = (i & 2) != 0 ? 0x80 : 0;
			B = (i & 4) != 0 ? 0x80 : 0;
			T = 0xFF;
		}

		return (T<<24 | R<<16 | G<<8 | B);
	}

	private int generateClutEntry_8Bits(int i)
	{
		int T=0, R=0, G=0, B=0;

		if ((i & 0x88) == 0)
		{
			if ((i & 0x70) == 0)
			{
				if ((i & 7) == 0)
					T = R = G = B = 0;

				else
				{
					R = (i & 1) != 0 ? 0xFF : 0;
					G = (i & 2) != 0 ? 0xFF : 0;
					B = (i & 4) != 0 ? 0xFF : 0;
					T = 0x40;
				}
			}
			else
			{
				R = ((i & 1) != 0 ? 0x55 : 0) + ((i & 0x10) != 0 ? 0xAA : 0);
				G = ((i & 2) != 0 ? 0x55 : 0) + ((i & 0x20) != 0 ? 0xAA : 0);
				B = ((i & 4) != 0 ? 0x55 : 0) + ((i & 0x40) != 0 ? 0xAA : 0);
				T = 0xFF;
			}
		}

		else if ((i & 0x88) == 8)
		{
			R = ((i & 1) != 0 ? 0x55 : 0) + ((i & 0x10) != 0 ? 0xAA : 0);
			G = ((i & 2) != 0 ? 0x55 : 0) + ((i & 0x20) != 0 ? 0xAA : 0);
			B = ((i & 4) != 0 ? 0x55 : 0) + ((i & 0x40) != 0 ? 0xAA : 0);
			T = 0x80;
		}

		else if ((i & 0x88) == 0x80)
		{
			R = ((i & 1) != 0 ? 0x2A : 0) + ((i & 0x10) != 0 ? 0x55 : 0) + 0x80;
			G = ((i & 2) != 0 ? 0x2A : 0) + ((i & 0x20) != 0 ? 0x55 : 0) + 0x80;
			B = ((i & 4) != 0 ? 0x2A : 0) + ((i & 0x40) != 0 ? 0x55 : 0) + 0x80;
			T = 0xFF;
		}

		else if ((i & 0x88) == 0x88)
		{
			R = ((i & 1) != 0 ? 0x2A : 0) + ((i & 0x10) != 0 ? 0x55 : 0);
			G = ((i & 2) != 0 ? 0x2A : 0) + ((i & 0x20) != 0 ? 0x55 : 0);
			B = ((i & 4) != 0 ? 0x2A : 0) + ((i & 0x40) != 0 ? 0x55 : 0);
			T = 0xFF;
		}

		return (T<<24 | R<<16 | G<<8 | B);
	}

	private void paintRegionBackground()
	{
		if (!region.isActive() || !region.getFillFlag())
			return;

		int color;

		if (IRD == 2)
			color = clut.getCLUT_2bit()[mapColorIndex(region.getPixelCode_2bit(), 2, IRD)];

		else if (IRD == 4)
			color = clut.getCLUT_4bit()[mapColorIndex(region.getPixelCode_4bit(), 4, IRD)];

		else
			color = clut.getCLUT_8bit()[mapColorIndex(region.getPixelCode_8bit(), 8, IRD)];

		color = scaleRGB(color);

		Arrays.fill(pixel_data, color);
	}

	// 
	private void paintRegionBorder(int x, int y, int w, int h)
	{
		big.setColor(Color.white);
		big.drawRect( x -1, y -1, w +2, h +1);
		big.drawString("x" + x + ", y" + y + " / " + w + "*" + h, x , y - 6);
	}

	// only for painted preview picture, else not necessary
	private void clearBackground()
	{
		Arrays.fill(preview_pixel_data, 0x60);

		big.setColor( new Color(0, 0, 0x60));   //deep blue to see full transparency
		big.fillRect( bimg.getMinX(), bimg.getMinY(), bimg.getWidth(), bimg.getHeight());
	}

	private void paintPixelLine(int x, int y, int w, int color_index, int depth)
	{
		x += object.getHorizontalPosition();
		y += object.getVerticalPosition();

		int color = 0, color_model = IRD;

		color_index = mapColorIndex(color_index, depth, IRD);

		if (clut.getModifyFlags() > 0 && (clut.getModifyFlags() & IRD) == 0) //remap if no alternative color def.
		{
			color_index = mapColorIndex(color_index, IRD, depth);
			color_model = depth;
		}

		if (color_model == 2)
			color = clut.getCLUT_2bit()[color_index];

		else if (color_model == 4)
			color = clut.getCLUT_4bit()[color_index];

		else
			color = clut.getCLUT_8bit()[color_index];

		color = scaleRGB(color);

		if ((0xFF000000 & color) == 0) //keep underlying pixel if new pixel is full transparent
			return;

		//DM23062004 081.7 int05 add
		if (x > region.getWidth() - 1 || y > region.getHeight() - 1)
		{
			region.setError(2);
			return;
		}

		from_index = x + y * region.getWidth();
		to_index = from_index + w;

		//DM23062004 081.7 int05 add
		if (x + w > region.getWidth())
		{
			to_index = from_index + region.getWidth() - x;
			region.setError(1);
		}

		//don't fit into the pixel area
		if (from_index < 0 || from_index > pixel_data.length || to_index < 0 || to_index > pixel_data.length)
		{
			region.setError(1);
			return;
		}

		Arrays.fill(pixel_data, from_index, to_index, color);
	}

	// not yet exported, only info for existence
	private void paintStringObjects(int x, int y, String str)
	{
		x += object.getHorizontalPosition();
		y += object.getVerticalPosition();

		big.setColor(Color.cyan);
		big.drawString(str, x , y + 26);
	}

	private void paintCopiedField(int x, int y, int w)
	{
		System.arraycopy(pixel_data, x + y * w, pixel_data, x + (y + 1) * w, w);
	}

	private int scaleRGB(int ARGB)
	{
		if ((ARGB & 0xFF000000) == 0)  //deep blue to see full transparency
			return 0x60;

		int R = 15 + (0xFF & ARGB>>>16);
		int G = 15 + (0xFF & ARGB>>>8);
		int B = 15 + (0xFF & ARGB);

		R = R > 0xEB ? 0xEB : R;
		G = G > 0xEB ? 0xEB : G;
		B = B > 0xEB ? 0xEB : B;

		// color float scale 16..235 for RGB
		//int R = 16 + (int)(0.85546875f * (0xFF & ARGB>>>16));
		//int G = 16 + (int)(0.85546875f * (0xFF & ARGB>>>8));
		//int B = 16 + (int)(0.85546875f * (0xFF & ARGB));

		int T = 0xFF & ARGB>>>24;

		return (T<<24 | R<<16 | G<<8 | B);
	}	

	private int mapColorIndex(int color_index, int depth, int new_depth)  // depth is 2,4,8!
	{
		switch (new_depth)
		{
		case 2:
			if (depth == 8)
				color_index >>>= 4;

			if (depth > 2)
				color_index = (2 & color_index>>>2) | (1 & color_index>>>2) | (1 & color_index>>>1) | (1 & color_index);

			break;

		case 4:
			if (depth == 2)
				color_index = (object != null) ? object.getMapTable_2to4bit()[color_index] : color_index;

			else if (depth == 8)
				color_index >>>= 4;

			break;

		case 8:
			if (depth == 2)
				color_index = (object != null) ? object.getMapTable_2to8bit()[color_index] : color_index;

			else if (depth == 4)
				color_index = (object != null) ? object.getMapTable_4to8bit()[color_index] : color_index;
		}

		return color_index;
	}

	//in case of undefined regions take regular 8 bit (256col) depth = flag 4
	private int getRegionDepth()
	{
		return (region != null ? region.getDepth() : 4);
	}

	//DM13062004 081.7 int04 add
	private void setUserClut()
	{
		int model = Integer.parseInt(user_table.get("model").toString().trim());
		int max_indices = model > 2 ? (model > 4 ? 256 : 16) : 4;

		if (getRegionDepth() < model)
			max_indices = getRegionDepth();

		for (int i = 0; i < max_indices; i++)
		{
			if (user_table.containsKey("" + i))
			{
				addBigMessage("addUserClut: " + i + " /ARGB " + user_table.get("" + i));

				clut.setClutEntry(mapColorIndex(i, getRegionDepth(), model), model, (int)Long.parseLong(user_table.get("" + i).toString().trim(), 16));
			}
		}
	}


	private Epoch setEpoch(int epoch_id)
	{
		String epoch_id_str = "" + epoch_id;

		if ( !epoches.containsKey(epoch_id_str) )
			epoches.put(epoch_id_str, new Epoch(epoch_id) );

		return (Epoch)epoches.get(epoch_id_str);
	}


	class Epoch
	{
		private int id;

		private Hashtable pages = new Hashtable();
		private Hashtable cluts = new Hashtable();
		private Hashtable regions = new Hashtable();
		private Hashtable objects = new Hashtable();

		private Epoch()
		{}

		private Epoch(int val)
		{
			id = val;
		}

		private void setId(int val)
		{
			id = val;
		}

		private int getId()
		{
			return id;
		}

		private Page setPage(int page_id)
		{
			String page_id_str = "" + page_id;

			if ( !pages.containsKey(page_id_str) )
				pages.put(page_id_str, new Page(page_id) );

			return (Page)pages.get(page_id_str);
		}

		private Page newPage(int page_id)
		{
			String page_id_str = "" + page_id;

			pages.put(page_id_str, new Page(page_id) );

			return (Page)pages.get(page_id_str);
		}

		private Region setRegion(int region_id)
		{
			String region_id_str = "" + region_id;

			if (!regions.containsKey(region_id_str) )
				regions.put(region_id_str, new Region(region_id) );

			return (Region)regions.get(region_id_str);
		}

		private CLUT setCLUT(int CLUT_id)
		{
			String CLUT_id_str = "" + CLUT_id;

			if ( !cluts.containsKey(CLUT_id_str) )
				cluts.put(CLUT_id_str, new CLUT(CLUT_id) );

			return (CLUT)cluts.get(CLUT_id_str);
		}

		private OBJECT setObject(int object_id)
		{
			String object_id_str = "" + object_id;

			if ( !objects.containsKey(object_id_str) )
				objects.put(object_id_str, new OBJECT(object_id) );

			return (OBJECT)objects.get(object_id_str);
		}

		private void clearRegions()
		{
			for (Enumeration e = regions.keys(); e.hasMoreElements() ; )
				((Region)regions.get(e.nextElement().toString())).setActive(false);
		}

		private Enumeration getRegions()
		{
			return regions.keys();
		}

		private void clearObjects()
		{
				objects.clear();
		}
	}


	class Page
	{
		private int id;
		private int version_number = -1;
		private long time_in = 0;
		private int time_out = 0;
		private int state;
		private int minX = width;
		private int minY = height;
		private int maxX = 0;
		private int maxY = 0;
		private int pixel[];
		private boolean write = false;

		private Page()
		{}

		private Page(int val)
		{
			id = val;
		}

		private void setId(int val)
		{
			id = val;
		}

		private int getId()
		{
			return id;
		}

		private void setVersionNumber(int val) //modulo16, if a change
		{
			version_number = val;
		}

		private int getVersionNumber()
		{
			return version_number;
		}

		private void setWriteStatus(boolean b)
		{
			write = b;
		}

		private boolean getWriteStatus()
		{
			return write;
		}

		private void setTimeIn(long val)
		{
			time_in = val;
		}

		private long getTimeIn()
		{
			return time_in;
		}

		private void setTimeOut(int val)
		{
			time_out = val;
		}

		private int getTimeOut()
		{
			return time_out;
		}

		private int[] initPixel()
		{
			return (pixel = new int[width * height]);
		}

		private int[] getPixel()
		{
			return pixel;
		}

		private void setState(int val)
		{
			state = val;
		}

		private int getState()
		{
			return state;
		}

		private void clearArea()
		{
			minX = width;
			minY = height;
			maxX = maxY = 0;
		}

		private void addArea(int x, int y, int w, int h)
		{
			int x2 = x + w;
			int y2 = y + h;

			int _minX = x < minX ? x : minX;
			int _minY = y < minY ? y : minY;
			int _maxX = x2 > maxX ? x2 : maxX;
			int _maxY = y2 > maxY ? y2 : maxY;

			if (_minX < 0 || _minY < 0 || _maxX < 0 || _maxY < 0 || (_maxX - _minX) < 0 || (_maxY - _minY) < 0)
			{
				Common.setMessage("!> decoding error: page area, page_id " + getId() + "[" + _minX + "," + _minY + "," + _maxX + "," + _maxY + "] (pts " + getTimeIn() + ")");
				return;
			}

			minX = _minX;
			minY = _minY;
			maxX = _maxX;
			maxY = _maxY;
		}

		private int getX()
		{
			return minX;
		}

		private int getY()
		{
			return minY;
		}

		private int getWidth()
		{
			return (maxX - minX);
		}

		private int getHeight()
		{
			return (maxY - minY);
		}

		private void reduce_size(int[] pv_pixel, int pv_width)
		{
			int firstpos = getX() + (getY() * pv_width); // up.left
			int lastpos = getX() + ((getY() + getHeight()) * pv_width); //bot.right
			int last = preview_pixel_data[lastpos];

			int min = firstpos + (32 * pv_width); //min 32 lines
			int max = getHeight();

			int split_b = getWidth() > 720 ? 1 : 0; //HD
			int[][] split = {{ 61, 93, 121 },{ 93, 141, 185 }}; //line split

			//adaption to ColorAreas.java (multicolor)
			//compare pixel value, reverse
			for (int i = lastpos; i >= firstpos; i--)
				if (last != pv_pixel[i] || i < min)
				{
					max = (i / pv_width); //may be odd
					max -= minY;
//Common.setMessage("A max " + max + " /w " + pv_width + " /pw " + getWidth());

					if (getWidth() > 720)
						max = max == 97 ? 96 : max == 147 ? 144 : max; //ZDF HD  , 2 / 3
					else
						max = max == 97 ? 92 : max == 143 ? 138 : max; //ARD SD ,  2 / 3

					if (max >= split[split_b][1]) //3
						max = (max / 6) * 6;
					else if (max >= split[split_b][2]) //4
						max = (max / 8) * 8;
					else if (max >= split[split_b][0]) //2
						max = (max / 4) * 4;
					else
						max &= ~1;  //1

					maxY = minY + max;
//Common.setMessage("B max " + max);
					return;
				}
		}
	}

	class Region
	{
		private int id;
		private int horizontal_address;
		private int vertical_address;
		private int version_number = -1;
		private boolean fill_flag;
		private boolean active = false;
		private boolean changed = false;
		private int r_width;
		private int r_height;
		private int x;
		private int y;
		private int level_of_compatibility;
		private int depth;
		private int CLUT_id;
		private int pixel_code_8bit;
		private int pixel_code_4bit;
		private int pixel_code_2bit;
		private int pixel[];

		//DM23062004 081.7 int05 add
		private int error = 0;

		private Region()
		{}

		private Region(int val)
		{
			id = val;
		}

		private void setId(int val)
		{
			id = val;
		}

		private int getId()
		{
			return id;
		}

		private void setHorizontalAddress(int val)
		{
			horizontal_address = val;
			x = 0;
		}

		private void setVerticalAddress(int val)
		{
			vertical_address = val;
			y = 0;
		}

		private void setVersionNumber(int val) //modulo16, if a change
		{
			version_number = val;
		}

		private int getVersionNumber()
		{
			return version_number;
		}

		private int[] initPixel()
		{
			return (pixel = new int[r_width * r_height]);
		}

		private int[] getPixel()
		{
			return pixel;
		}

		private void setFillFlag(int val)  // fill background with color of region_pixel_code_xbit index
		{
			fill_flag = val != 0;
		}

		private boolean getFillFlag()
		{
			return fill_flag;
		}

		private void setActive(boolean b)
		{
			active = b;
		}

		private boolean isActive()
		{
			return active;
		}


		private void setWidth(int val)  //max 720, if h_address =1
		{
			r_width = val;
		}

		private int getWidth()
		{
			return r_width;
		}

		private void setHeight(int val) //max 576, if v_address =1
		{
			r_height = val;
		}

		private int getHeight()
		{
			return r_height;
		}

		private void setLevelOfCompatibility(int val) //1,2,3 = 2,4,8bit supported colors by IRD, X can use all :)
		{
			level_of_compatibility = val;
		}

		private int getCompatibility()
		{
			return level_of_compatibility;
		}

		private void setDepth(int val) //1,2,3 = 2,4,8bit pixel depth
		{
			depth = 1<<val;
		}

		private int getDepth()
		{
			return depth;
		}

		private void setCLUT_id(int val)
		{
			CLUT_id = val;
		}

		private int getCLUT_id()
		{
			return CLUT_id;
		}

		private void setPixelCode_8bit(int val) //see fillflag, backgr CLUT entry
		{
			pixel_code_8bit = val;
		}

		private void setPixelCode_4bit(int val) //see fillflag, backgr CLUT entry
		{
			pixel_code_4bit = val;
		}

		private void setPixelCode_2bit(int val) //see fillflag, backgr CLUT entry
		{
			pixel_code_2bit = val;
		}

		private int getPixelCode_8bit()
		{
			return pixel_code_8bit;
		}

		private int getPixelCode_4bit()
		{
			return pixel_code_4bit;
		}

		private int getPixelCode_2bit()
		{
			return pixel_code_2bit;
		}

		private int getXBound()
		{
			return horizontal_address;
		}

		private int getYBound()
		{
			return vertical_address;
		}

		private int getX(int x_len)
		{
			int nx = x;
			x += x_len;

			return nx;
		}

		private int getY()
		{
			return y;
		}

		private void nextLine()
		{
			x = 0;
			y += 2;
		}

		private void nextField()
		{
			x = 0;
			y = 1;
		}

		private void resetXY()
		{
			x = 0;
			y = 0;
		}

		private void setChanged(boolean b)
		{
			changed = b;
		}

		private boolean isChanged()
		{
			return changed;
		}

		//DM23062004 081.7 int05 add
		private int getErrors()
		{
			return error;
		}

		//DM23062004 081.7 int05 add
		private void setError(int val)
		{
			error |= val;

			if (val == 0)
				error = 0;
		}
	}

	class CLUT
	{
		private int id;
		private int version_number = -1;
		private int modify_flags = 0;

		private final int default_CLUT_2bit[] = { 
			0, 0xFFFFFFFF, 0xFF000000, 0xFF808080 
		};
		private final int default_CLUT_4bit[] = { 
			0, 0xFFFF0000, 0xFF00FF00, 0xFFFFFF00, 0xFF0000FF, 0xFFFF00FF,
			0xFF00FFFF, 0xFFFFFFFF, 0xFF000000, 0xFF800000, 0xFF008000, 
			0xFF808000, 0xFF000080, 0xFF800080, 0xFF008080, 0xFF808080
		};
		private final int default_CLUT_8bit[] = table_CLUT_8bit;

		private int CLUT_2bit[];
		private int CLUT_4bit[];
		private int CLUT_8bit[];

		private CLUT()
		{
			init(0);
		}

		private CLUT(int val)
		{
			init(val);
		}

		private void init(int val)
		{
			id = val;

			CLUT_2bit = default_CLUT_2bit;
			CLUT_4bit = default_CLUT_4bit;
			CLUT_8bit = default_CLUT_8bit;
		}

		private void setId(int val)
		{
			id = val;
		}

		private int getId()
		{
			return id;
		}

		private void setVersionNumber(int val) //modulo16, if a change
		{
			version_number = val;
		}

		private int getVersionNumber()
		{
			return version_number;
		}

		private int[] getCLUT_2bit()
		{
			return CLUT_2bit;
		}

		private int[] getCLUT_4bit()
		{
			return CLUT_4bit;
		}

		private int[] getCLUT_8bit()
		{
			return CLUT_8bit;
		}

		private void setClutEntry(int index, int flag, int ARGB)
		{
			if ((flag & 8) != 0)
				CLUT_8bit[index] = ARGB;

			else if ((flag & 4) != 0)
				CLUT_4bit[index] = ARGB;

			else if ((flag & 2) != 0)
				CLUT_2bit[index] = ARGB;

			modify_flags |= flag;
		}

		private int getModifyFlags()
		{
			return modify_flags;
		}
	}


	class OBJECT
	{
		private int id;
		private int version_number = -1;
		private int type;
		private int region_id = -1;
		private int provider_flag;
		private int horizontal_position;
		private int vertical_position;
		private int foreground_pixel_code;
		private int background_pixel_code;
		private boolean non_modifying_color_flag;
		private boolean copy_top_field_flag;

		private final int default_map_table_2to4bit[] = { 0, 7, 8, 0xF };
		private final int default_map_table_2to8bit[] = { 0, 0x77, 0x88, 0xFF };
		private final int default_map_table_4to8bit[] = { 
			0, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 
			0x88, 0x99, 0xAA, 0xBB, 0xCC, 0xDD, 0xEE, 0xFF
		};
		private int map_table_2to4bit[];
		private int map_table_2to8bit[];
		private int map_table_4to8bit[];

		private Hashtable regions = new Hashtable();

		private OBJECT()
		{
			init();
		}

		private OBJECT(int val)
		{
			id = val;
			init();
		}

		private void init()
		{
			map_table_2to4bit = default_map_table_2to4bit;
			map_table_2to8bit = default_map_table_2to8bit;
			map_table_4to8bit = default_map_table_4to8bit;
		}

		private void setId(int val)
		{
			id = val;
		}

		private int getId()
		{
			return id;
		}

		private void setType(int val)
		{
			type = val;
		}

		private int getType() //0 bitmap,1 char, 2 chars
		{
			return type;
		}

		private void setProvider(int val)  //0 def. in stream, 1 read from ROM, 2+3 res.
		{
			provider_flag = val;
		}

		private void setHorizontalPosition(int val)
		{
			horizontal_position = val;
		}

		private int getHorizontalPosition()
		{
			return horizontal_position;
		}

		private void setVerticalPosition(int val)
		{
			vertical_position = val;
		}

		private int getVerticalPosition()
		{
			return vertical_position;
		}

		private void setForegroundCode(int val) // color entry in 8bit CLUT
		{
			foreground_pixel_code = val;
		}

		private void setBackgroundCode(int val) // color entry in 8bit CLUT
		{
			background_pixel_code = val;
		}

		private void setVersionNumber(int val) //modulo16, if a change
		{
			version_number = val;
		}

		private int getVersionNumber()
		{
			return version_number;
		}

		private void setNonModify(int val) // pixel with CLUT color entry 1 = dont overwrite it
		{
			non_modifying_color_flag = val != 0;
		}

		private void setCopyTopFieldFlag(int val) // bottomfield isn't transmitted, so copy topfield
		{
			copy_top_field_flag = val == 0;
		}

		private boolean getCopyTopFieldFlag()
		{
			return copy_top_field_flag;
		}

		private void setMapTable_2to4bit(int i, int val)
		{
			map_table_2to4bit[i] = val;
		}

		private void setMapTable_2to8bit(int i, int val)
		{
			map_table_2to8bit[i] = val;
		}

		private void setMapTable_4to8bit(int i, int val)
		{
			map_table_4to8bit[i] = val;
		}

		private int[] getMapTable_2to4bit()
		{
			return map_table_2to4bit;
		}

		private int[] getMapTable_2to8bit()
		{
			return map_table_2to8bit;
		}

		private int[] getMapTable_4to8bit()
		{
			return map_table_4to8bit;
		}

		private void setRegionId(int val)
		{
			region_id = val;
		}

		private int getRegionId()
		{
			return region_id;
		}

		private void resetRegionId()
		{
			region_id = -1;
		}
	}
}