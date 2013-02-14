/*
 * @(#)StreamParser
 *
 * Copyright (c) 2005-2009 by dvb.matt, All rights reserved.
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
 *
 * Support for SRT with font tags, W3C TTML and GPAC TTEXT
 * added by Simon Liddicott, 2012,2013
 *
 */

package net.sourceforge.dvb.projectx.parser;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.PushbackInputStream;
import java.io.RandomAccessFile;
import java.io.ByteArrayOutputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Date;
import java.util.TimeZone;
import java.util.StringTokenizer;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import net.sourceforge.dvb.projectx.common.Common;
import net.sourceforge.dvb.projectx.common.Resource;
import net.sourceforge.dvb.projectx.common.Keys;
import net.sourceforge.dvb.projectx.common.JobCollection;
import net.sourceforge.dvb.projectx.common.JobProcessing;

import net.sourceforge.dvb.projectx.io.RawFile;

import net.sourceforge.dvb.projectx.xinput.XInputFile;

import net.sourceforge.dvb.projectx.parser.CommonParsing;
import net.sourceforge.dvb.projectx.parser.StreamConverter;
import net.sourceforge.dvb.projectx.parser.StreamDemultiplexer;

import net.sourceforge.dvb.projectx.parser.StreamProcessBase;
import net.sourceforge.dvb.projectx.parser.StreamProcessSubpicture;

import net.sourceforge.dvb.projectx.subtitle.Teletext;
import net.sourceforge.dvb.projectx.subtitle.UnicodeWriter;
import net.sourceforge.dvb.projectx.subtitle.Subpicture;
import net.sourceforge.dvb.projectx.subtitle.Sup2VobSub;

import net.sourceforge.dvb.projectx.thirdparty.Ifo;

/**
 * main thread
 */
public class StreamProcessTeletext extends StreamProcessBase {


	private final int MEGARADIO   = 0;
	private final int EXPORT_TEXT = 1;
	private final int EXPORT_SC   = 2;
	private final int EXPORT_SUB  = 3;
	private final int EXPORT_SRT  = 4;
	private final int EXPORT_SSA  = 5;
	private final int EXPORT_SUP  = 6;
	private final int EXPORT_STL  = 7;
	private final int EXPORT_SON  = 8;
	private final int EXPORT_SRTC = 9;
	private final int EXPORT_W3C  = 10;
	private final int EXPORT_GPAC = 11;

	private byte tmp_byte_value = -1;
	private int tmp_int_value = -1;

	/**
	 * 
	 */
	public StreamProcessTeletext(JobCollection collection, XInputFile xInputFile, String filename_pts, String filename_type, String videofile_pts, int isElementaryStream)
	{
		super();

		String SubtitleExportFormat = collection.getSettings().getProperty(Keys.KEY_SubtitleExportFormat);

		processStream(collection, xInputFile, filename_pts, filename_type, videofile_pts, isElementaryStream, SubtitleExportFormat);

		// 2nd export format, new run
		SubtitleExportFormat = collection.getSettings().getProperty(Keys.KEY_SubtitleExportFormat_2);

		if (!SubtitleExportFormat.equalsIgnoreCase("null"))
			processStream(collection, xInputFile, filename_pts, filename_type, videofile_pts, isElementaryStream, SubtitleExportFormat);
	}

	/**
	 * decoding teletext stream
	 */
	private void processStream(JobCollection collection, XInputFile xInputFile, String filename_pts, String filename_type, String videofile_pts, int isElementaryStream, String SubtitleExportFormat)
	{
		int[] SUP_Offset = { -1, -1 }; //1st + 2nd Offset
		long size = 0;

		boolean debug = collection.getSettings().getBooleanProperty(Keys.KEY_DebugLog);
		boolean Message_2 = collection.getSettings().getBooleanProperty(Keys.KEY_MessagePanel_Msg2);
		boolean ShowSubpictureWindow = collection.getSettings().getBooleanProperty(Keys.KEY_showSubpictureWindow);
		boolean DecodeMegaradio = collection.getSettings().getBooleanProperty(Keys.KEY_SubtitlePanel_decodeMegaradio);
		boolean ExportTextAsUnicode = collection.getSettings().getBooleanProperty(Keys.KEY_SubtitlePanel_exportTextAsUnicode);
		boolean ExportTextAsUTF8 = collection.getSettings().getBooleanProperty(Keys.KEY_SubtitlePanel_exportTextAsUTF8);
		boolean DecodeHiddenRows = collection.getSettings().getBooleanProperty(Keys.KEY_SubtitlePanel_decodeHiddenRows);
		boolean KeepOriginalTimecode = collection.getSettings().getBooleanProperty(Keys.KEY_SubtitlePanel_keepOriginalTimecode);
		boolean ExportAsVobSub = collection.getSettings().getBooleanProperty(Keys.KEY_SubtitlePanel_exportAsVobSub);
		boolean BoxedMode = collection.getSettings().getBooleanProperty(Keys.KEY_SubtitlePanel_TtxExportBoxedOnly);
		boolean TextAlignment = collection.getSettings().getBooleanProperty(Keys.KEY_SubtitlePanel_useTextAlignment);

//		boolean SpecialTermination = collection.getSettings().getBooleanProperty(Keys.KEY_SubtitlePanel_specialTermination);
		boolean SpecialTermination = true;

		boolean KeepColourTable = collection.getSettings().getBooleanProperty(Keys.KEY_SubtitlePanel_keepColourTable);

		String SubtitleFont = collection.getSettings().getProperty(Keys.KEY_SubtitleFont);
		String Format_SUP_Values = collection.getSettings().getProperty(Keys.KEY_SubtitlePanel_Format_SUP_Values);

		JobProcessing job_processing = collection.getJobProcessing();

		Subpicture subpicture = Common.getSubpictureClass();
		Teletext teletext = Common.getTeletextClass();

		if (ShowSubpictureWindow && (SubtitleExportFormat.equalsIgnoreCase(Keys.ITEMS_SubtitleExportFormat[6].toString()) || SubtitleExportFormat.equalsIgnoreCase(Keys.ITEMS_SubtitleExportFormat[7].toString())))
			Common.getGuiInterface().showSubpicture();

		if (SubtitleExportFormat.equalsIgnoreCase(Keys.ITEMS_SubtitleExportFormat[7].toString()) || SubtitleExportFormat.equalsIgnoreCase(Keys.ITEMS_SubtitleExportFormat[6].toString())) // SUP + SON, set variables
			SUP_Offset = subpicture.set(SubtitleFont, Format_SUP_Values, KeepColourTable);

		ArrayList userdefined_pages = new ArrayList();

		userdefined_pages.add(collection.getSettings().getProperty(Keys.KEY_SubtitlePanel_TtxPage1));
		userdefined_pages.add(collection.getSettings().getProperty(Keys.KEY_SubtitlePanel_TtxPage2));
		userdefined_pages.add(collection.getSettings().getProperty(Keys.KEY_SubtitlePanel_TtxPage3));
		userdefined_pages.add(collection.getSettings().getProperty(Keys.KEY_SubtitlePanel_TtxPage4));
		userdefined_pages.add(collection.getSettings().getProperty(Keys.KEY_SubtitlePanel_TtxPage5));
		userdefined_pages.add(collection.getSettings().getProperty(Keys.KEY_SubtitlePanel_TtxPage6));
		userdefined_pages.add(collection.getSettings().getProperty(Keys.KEY_SubtitlePanel_TtxPage7));
		userdefined_pages.add(collection.getSettings().getProperty(Keys.KEY_SubtitlePanel_TtxPage8));

		String page_list = "";

		// read "777,888-890,150" etc
		// read all pages and expand sparated pages 888,889,890 etc, if exists
		for (int i = 0, ix = 0; i < userdefined_pages.size(); i++)
		{
			page_list = userdefined_pages.get(i).toString();

			ix = page_list.indexOf(",");

			if (ix > 0)  // separated values
			{
				StringTokenizer st = new StringTokenizer(page_list, ",");

				userdefined_pages.remove(i);

				while (st.hasMoreTokens())
					userdefined_pages.add(i, st.nextToken());
			}
		}

		// read all pages and expand 888-890 etc, if exists
		for (int i = 0, ix = 0, firstvalue = 0, lastvalue = 0; i < userdefined_pages.size(); i++)
		{
			page_list = userdefined_pages.get(i).toString();

			ix = page_list.indexOf("-");

			if (ix > 0)  // values are in Hex
			{
				try {

					firstvalue = Integer.parseInt(page_list.substring(0, ix), 16);
					lastvalue  = Integer.parseInt(page_list.substring(ix + 1), 16);

					userdefined_pages.remove(i);

					for (int j = lastvalue; j >= firstvalue; j--)
						userdefined_pages.add(i, Integer.toHexString(j).toUpperCase());

				} catch (Exception e) {
					Common.setExceptionMessage(e);
				}
			}
		}


		for (int pn = 0; pn < userdefined_pages.size(); pn++)
		{
			String page = "0";

			if (!DecodeMegaradio)
			{
				page = userdefined_pages.get(pn).toString();

				if (page.equalsIgnoreCase("null")) 
					continue;
			}
			else
				pn = userdefined_pages.size();

			String fchild = xInputFile.getName();
			String fparent = collection.getOutputNameParent(fchild);

			size = xInputFile.length();

			Common.getGuiInterface().initTtxPageMatrix(fchild);

			teletext.clearEnhancements();

			if (!DecodeMegaradio) 
				fparent += "[" + page + "]";


			String ttxfile;

			int subtitle_type;

			if (DecodeMegaradio) 
			{
				ttxfile = fparent + ".mgr";
				subtitle_type = MEGARADIO;
			}

			else if (SubtitleExportFormat.equalsIgnoreCase(Keys.ITEMS_SubtitleExportFormat[1].toString()))
			{
				ttxfile = fparent + ".sc";
				subtitle_type = EXPORT_SC;
			}

			else if (SubtitleExportFormat.equalsIgnoreCase(Keys.ITEMS_SubtitleExportFormat[2].toString()))
			{
				ttxfile = fparent + ".sub";
				subtitle_type = EXPORT_SUB;
			}

			else if (SubtitleExportFormat.equalsIgnoreCase(Keys.ITEMS_SubtitleExportFormat[3].toString())) 
			{
				ttxfile = fparent + ".srt";
				subtitle_type = EXPORT_SRT;
			}

			else if (SubtitleExportFormat.equalsIgnoreCase(Keys.ITEMS_SubtitleExportFormat[8].toString())) 
			{
				ttxfile = fparent + "[c].srt";
				subtitle_type = EXPORT_SRTC;
			}

			else if (SubtitleExportFormat.equalsIgnoreCase(Keys.ITEMS_SubtitleExportFormat[9].toString())) 
			{
				ttxfile = fparent + "[W3C].xml";
				subtitle_type = EXPORT_W3C;
			}

			else if (SubtitleExportFormat.equalsIgnoreCase(Keys.ITEMS_SubtitleExportFormat[10].toString())) 
			{
				ttxfile = fparent + "[GPAC].ttxt";
				subtitle_type = EXPORT_GPAC;
			}

			else if (SubtitleExportFormat.equalsIgnoreCase(Keys.ITEMS_SubtitleExportFormat[5].toString())) 
			{
				ttxfile = fparent + ".ssa";
				subtitle_type = EXPORT_SSA;
			}

			else if (SubtitleExportFormat.equalsIgnoreCase(Keys.ITEMS_SubtitleExportFormat[7].toString())) 
			{
				ttxfile = fparent + ".sup";
				subtitle_type = EXPORT_SUP;
			}

			else if (SubtitleExportFormat.equalsIgnoreCase(Keys.ITEMS_SubtitleExportFormat[4].toString())) 
			{
				ttxfile = fparent + ".stl";
				subtitle_type = EXPORT_STL;
			}

			else if (SubtitleExportFormat.equalsIgnoreCase(Keys.ITEMS_SubtitleExportFormat[6].toString())) //placeholder for .son export
			{
				ttxfile = fparent + ".ssa"; //.son
				subtitle_type = EXPORT_SSA;  // 8
			}

			else if (SubtitleExportFormat.equalsIgnoreCase("null"))
				continue;

			else   // free format
			{
				ttxfile = fparent + ".txt";
				subtitle_type = EXPORT_TEXT;
			}

			Common.setMessage("");
			Common.setMessage(Resource.getString("teletext.msg.output") + " " + ttxfile.substring(ttxfile.length() - 3));

			if (ExportTextAsUnicode && subtitle_type != EXPORT_SUP)
				Common.setMessage("-> " + Resource.getString(Keys.KEY_SubtitlePanel_exportTextAsUnicode[0]));

			if (ExportTextAsUTF8 && subtitle_type != EXPORT_SUP)
				Common.setMessage("-> " + Resource.getString(Keys.KEY_SubtitlePanel_exportTextAsUTF8[0]));

			if (DecodeHiddenRows)
				Common.setMessage("-> " + Resource.getString(Keys.KEY_SubtitlePanel_decodeHiddenRows[0]));

			if (BoxedMode)
				Common.setMessage("-> " + Resource.getString(Keys.KEY_SubtitlePanel_TtxExportBoxedOnly[0]));

			if (KeepOriginalTimecode)
				Common.setMessage("-> " + Resource.getString(Keys.KEY_SubtitlePanel_keepOriginalTimecode[0]));

	//		if (SpecialTermination)
	//			Common.setMessage("-> " + Resource.getString(Keys.KEY_SubtitlePanel_specialTermination[0]));
			if (KeepColourTable)
				Common.setMessage("-> " + Resource.getString(Keys.KEY_SubtitlePanel_keepColourTable[0]));


			DateFormat timeformat_1 = new SimpleDateFormat("HH:mm:ss.SSS");
			timeformat_1.setTimeZone(TimeZone.getTimeZone("GMT+0:00"));

			DateFormat timeformat_2 = new SimpleDateFormat("HH:mm:ss,SSS");
			timeformat_2.setTimeZone(TimeZone.getTimeZone("GMT+0:00"));

			boolean vptsdata = false;
			boolean ptsdata = false;
			boolean write = false;
			boolean loadpage = false;
			boolean valid = false;

			long count = 0;
			long time_difference = 0;
			long source_pts = 0;
			long startPoint = 0;

			int page_value = subtitle_type == MEGARADIO ? 0x800 : Integer.parseInt(page, 16);
			int x = 0;
			int seiten = 0;
			int v = 0;
			int w = 0;

			String page_number = "";
			String subpage_number = "";

			char txline[];

			try {

				PushbackInputStream in = new PushbackInputStream(xInputFile.getInputStream(), 94);
				BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(ttxfile), 655350);
				ByteArrayOutputStream byte_buffer = new ByteArrayOutputStream();

				UnicodeWriter print_buffer = new UnicodeWriter(byte_buffer, ExportTextAsUnicode, ExportTextAsUTF8);

				Common.setMessage(Resource.getString("teletext.msg.tmpfile", xInputFile.getName(), "" + size));
				Common.setMessage(Resource.getString("teletext.msg.search") + " " + (subtitle_type == MEGARADIO ? Resource.getString("teletext.msg.megaradio") : Resource.getString("teletext.msg.page") + " " + page));

				Common.updateProgressBar(Resource.getString("teletext.progress") + " " + (subtitle_type == MEGARADIO ? Resource.getString("teletext.msg.megaradio") : Resource.getString("teletext.msg.page") + " " + page), 0, 0);

				long[] pts_value = {0};
				long[] pts_position = {0};
				long[] video_pts_value = {0};
				long[] vtime = {0};

				Common.getGuiInterface().showExportStatus(Resource.getString("teletext.status"), seiten);

				long[][] obj = loadTempOtherPts(filename_pts, "teletext.msg.discard", "audio.msg.pts.firstonly", "teletext.msg.pts.start_end", "teletext.msg.pts.missed", CommonParsing.TELETEXT, false, debug);

				if (obj != null)
				{
					pts_value = obj[0];
					pts_position = obj[1];
					ptsdata = true;
					obj = null;
				}

				obj = loadTempVideoPts(videofile_pts, debug);

				if (obj != null)
				{
					video_pts_value = obj[0];
					vtime = obj[1];
					vptsdata = true;
					obj = null;
				}

				// 1st line 
				switch (subtitle_type)
				{
				case EXPORT_SC:
					print_buffer.println("Subtitle File Mark:"+((CommonParsing.getVideoFramerate()==3600L) ? "2" : "1"));
					print_buffer.flush();
					byte_buffer.writeTo(out);
					byte_buffer.reset();
					break;

				case EXPORT_W3C:
					String[] W3Chead = teletext.getW3CHead();

					for (int a = 0; a < W3Chead.length; a++) 
						print_buffer.println(W3Chead[a]);

					print_buffer.flush();
					byte_buffer.writeTo(out);
					byte_buffer.reset();
					break;

				case EXPORT_GPAC:
					String[] GPAChead = teletext.getGPACHead();

					for (int a = 0; a < GPAChead.length; a++) 
						print_buffer.println(GPAChead[a]);

					print_buffer.flush();
					byte_buffer.writeTo(out);
					byte_buffer.reset();
					break;

				case EXPORT_SSA:
					String[] SSAhead = teletext.getSSAHead();

					for (int a = 0; a < SSAhead.length; a++) 
						print_buffer.println(SSAhead[a]);

					print_buffer.flush();
					byte_buffer.writeTo(out);
					byte_buffer.reset();
					break;

				case EXPORT_STL:
					String[] STLhead = teletext.getSTLHead(Common.getVersionName() + " on " + DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(System.currentTimeMillis())));

					for (int a = 0; a < STLhead.length; a++) 
						print_buffer.println(STLhead[a]);

					print_buffer.flush();
					byte_buffer.writeTo(out);
					byte_buffer.reset();
					break;

				case EXPORT_SON:  //DM14052004 081.7 int02 add, still unused!
					String[] SONhead = teletext.getSONHead(new File(ttxfile).getParent(), (long)CommonParsing.getVideoFramerate());

					for (int a = 0; a < SONhead.length; a++) 
						print_buffer.println(SONhead[a]);

					print_buffer.flush();
					byte_buffer.writeTo(out);
					byte_buffer.reset();
				} 

				if (vptsdata && ptsdata)
				{
					int jump = checkPTSMatch(video_pts_value, pts_value);

					if (jump < 0)
					{
						Common.setMessage(Resource.getString("teletext.msg.pts.mismatch"));  

						vptsdata = false; 
						x = 0; 
					}

					else
						x = jump;
				}

				x = 0;  //start at 0

				if (ptsdata)
				{ 
					source_pts = pts_value[x]; 
					startPoint = pts_position[x]; 

					if (vptsdata) 
					{
						Common.setMessage(Resource.getString("teletext.msg.adjust.at.video"));
						time_difference = video_pts_value[0];
					}
					else
					{
						Common.setMessage(Resource.getString("teletext.msg.adjust.at.own"));
						time_difference = 0;
					}
				}

				while (count < startPoint)
					count += in.skip(startPoint-count);


				boolean missing_syncword = false;
				boolean vps = false;
				boolean wss = false;
				boolean page_match = false;
				boolean lastpage_match = false;

				int row = -1;
				int magazine = -1;
				int vbi = 0;
				int character_set = 0;
				int data_unit_id = -1;
				int required_data_unit_id = -1;

				byte packet[] = new byte[48];

				Hashtable load_buffer = new Hashtable();
				Hashtable write_buffer = new Hashtable();
				Hashtable flags = new Hashtable();

				ArrayList picture_String = new ArrayList();

				String provider = "";
				String program_title = "";
				String vps_str = "";
				String wss_str = "";
				int character_count = 0;

				readloop:
				while ( count < size )
				{
					Common.updateProgressBar(count, size);

					while (pause())
					{}

					if (CommonParsing.isProcessCancelled())
					{ 
						CommonParsing.setProcessCancelled(false);
						job_processing.setSplitSize(0); 

						break readloop; 
					}

					in.read(packet);

					if (packet[1] != 0x2C && packet[47] != 0x2C && packet[1] != 0x5A && (0xFF & packet[1]) != 0x88 && (0xFF & packet[1]) != 0xFF)
					{
						if (Message_2 && !missing_syncword)
							Common.setMessage(Resource.getString("teletext.msg.syncword.lost") + " " + count);

						missing_syncword = true;
						count++;
						in.unread(packet, 1, 47);

						continue readloop;
					}

					/**
					 * stuffing packet with size 0x5A + 2
					 */
					else if (packet[1] == 0x5A && packet[0] == -1)
					{
						in.skip(0x5C - 48);
						count += 0x5C;

						continue readloop;
					}

					/**
					 * stuffing packet with size 0x88 + 2
					 */
					else if ((0xFF & packet[1]) == 0x88 && packet[0] == -1)
					{
						in.skip(0x8A - 48);
						count += 0x8A;

						continue readloop;
					}

					/**
					 * stuffing packet without size value, skips std packet size 46
					 */
					else if (packet[1] == -1 && packet[0] == -1)
					{
						in.unread(packet, 46, 2);
						count += 0x2E;

						continue readloop;
					}

					else
						in.unread(packet, 46, 2);

					if (Message_2 && missing_syncword)
						Common.setMessage(Resource.getString("teletext.msg.syncword.found") + " " + count);

					missing_syncword = false;

					count += 46;

					vps = false;
					wss = false;
					valid = false;
					data_unit_id = 0xFF & packet[0];

					switch (data_unit_id)
					{
					case 2:    // 0x02 EBU non subtitle data
					case 3:    // 0x03 EBU subtitle data
						valid = true; 
						break; 

					case 0xC3: // VPS
						valid = true; 
						vps = true; 
						break; 

					case 0xC4: // WSS
						valid = true; 
						wss = true; 
						break; 

					case 0xFF: // hidden 
						if (DecodeHiddenRows)
							valid = true;
						break; 

					default:  // others, unknown
						if (debug) 
							System.out.println(" unkn_" + Integer.toHexString(0xFF & packet[0]) + "/" + (count - 46));
						//continue readloop;
					}

					// logging
					if (debug)
					{
						System.out.println();
						System.out.println("pos: " + (count - 46));

						for (int a = 0; a < 46; a++)
							System.out.print(" " + ((0xFF & packet[a])<0x10 ? "0" : "") + Integer.toHexString(0xFF & packet[a]).toUpperCase());

						System.out.println();
					}

					if (!valid)
						continue readloop;

					vbi = ((0x20 & packet[2]) != 0 ? 0 : 313) + (0x1F & packet[2]);

					if (vps)
					{
						if ((0x3F & packet[2]) != 0x30)
							continue readloop;

						/**
						 * show vps status of VBI 16 in GUI
						 */
						String str = VBI.decodeVPS(packet, 2);

						if (str != null && !str.equals(vps_str))
						{
							vps_str = str;

							if (Common.getSettings().getBooleanProperty(Keys.KEY_showTtxHeader)) //interactive checkbox
								Common.getGuiInterface().updateVpsLabel(vps_str);

							Common.setMessage(Resource.getString("teletext.msg.vps", str) + " " + Common.formatTime_1(source_pts / 90));
						}

						continue readloop;
					}

					else if (wss)
					{
						if ((0x3F & packet[2]) != 0x37)
							continue readloop;

						if (!Common.getSettings().getBooleanProperty(Keys.KEY_MessagePanel_Msg5)) //interactive checkbox
							continue readloop;

						/**
						 * show wss status of VBI 23 in GUI
						 */
						String str = VBI.decodeWSS(packet, 2);

						if (str != null && !str.equals(wss_str))
						{
							wss_str = str;

							if (wss_str.length() == 0)
								Common.setMessage("-> WSS Status - no change @ PTS " + Common.formatTime_1(source_pts / 90));

							else
							{
								Common.setMessage("-> WSS Status - changed @ PTS " + Common.formatTime_1(source_pts / 90));
								Common.setMessage(wss_str);
							}
						}

						continue readloop;
					}

					else
					{
						tmp_int_value = (teletext.hamming_8_4(packet[4]))<<4 | teletext.hamming_8_4(packet[5]);

						if (tmp_int_value < 0) // decode error
						{
							row = -1;
							magazine = -1;
						}

						else
						{
						//	row = 0xFF & teletext.bytereverse((byte)((0xF & teletext.hamming_8_4(packet[4]))<<4 | (0xF & teletext.hamming_8_4(packet[5]))));

							row = 0xFF & teletext.bytereverse((byte) tmp_int_value);
							magazine = (7 & row) == 0 ? 8 : (7 & row);
							row >>>= 3;
						}
					}


					// X3/31.1 ttx provider
					if (magazine == 3 && row == 31 && packet[7] == 0x40 && packet[8] == 0x57 && provider.equals(""))
					{
						provider = teletext.buildString(packet, 10, 34, 31, 0, 0, false).trim();
						Common.setMessage(Resource.getString("teletext.msg.provider") + " " + provider);
					}

					// X8/30.0 program title
					else if (magazine == 8 && row == 30 && packet[7] == (byte)0xA8)
					{
						String str = teletext.buildString(packet, 26, 20, 30, 0, 0, true).trim() + " ";

						if (!str.equals(program_title))
						{
							program_title = str;
							Common.setMessage(Resource.getString("teletext.msg.program") + " " + program_title);
						}
					}


					if (row == 0)
					{
						int flag = 0;

						for (int a = 0; a < 6; a++)
							flag |= (0xF & teletext.bytereverse((byte) teletext.hamming_8_4(packet[8+a]) )>>>4 ) <<(a*4);

						page_number = Integer.toHexString(0xF & teletext.bytereverse((byte) teletext.hamming_8_4(packet[7]) )>>>4 ).toUpperCase() +
							Integer.toHexString(0xF & teletext.bytereverse((byte) teletext.hamming_8_4(packet[6]) )>>>4 ).toUpperCase();

						int o[] = { 0xF, 7, 0xF, 3 };
						subpage_number = "";

						for (int a = 3; a > -1; a--)
							subpage_number += Integer.toHexString(o[a] & flag>>>(a*4)).toUpperCase();

						flags.put("data_unit_id", "" + data_unit_id);
						flags.put("magazine", "" + magazine);
						flags.put("page_number", page_number);
						flags.put("subpage_number", subpage_number);
						flags.put("news", "" + (1 & flag>>>14));
						flags.put("subtitle", "" + (1 & flag>>>15));
						flags.put("erase", "" + (1 & flag>>>7));
						flags.put("suppressed_head", "" + (1 & flag>>>16));
						flags.put("update", "" + (1 & flag>>>17));
						flags.put("interrupt", "" + (1 & flag>>>18));
						flags.put("inhibit", "" + (1 & flag>>>19));
						flags.put("magazine_serial", "" + (1 & flag>>>20));
						flags.put("character_set", "" + (7 & flag>>>21));

						// page_number matches -- subpage_numer currently always accepted
						if ( page.equalsIgnoreCase( Integer.toHexString(magazine) + page_number) )
						{
							character_set = 7 & flag>>>21;
							page_match = true;
						}

						else
							page_match = false;

						Common.getGuiInterface().updateTtxPageMatrix("" + magazine + page_number);

						// show header_line in GUI
						if (Common.getSettings().getBooleanProperty(Keys.KEY_showTtxHeader) || debug) 
						{
							String str = magazine + page_number + "  " + subpage_number + "  " + teletext.buildString(packet, 14, 32, 0, (7 & flag>>>21), 0, true) + "  " + program_title;

							if (Common.getSettings().getBooleanProperty(Keys.KEY_showTtxHeader)) //interactive checkbox
								Common.getGuiInterface().updateTtxHeader(str);

							if (debug)
								System.out.println(str);
						}

						if (debug)
							System.out.println(flags.toString());
					}

					if (ptsdata)
					{
						write = !vptsdata;

						while (pts_position[x+1] != -1 && pts_position[x+1] <= count - 46)
						{
							x++;
							source_pts = pts_value[x];
						}

						rangeloop:
						while (vptsdata && v < video_pts_value.length)  //pic_start_pts must be in range ATM
						{ 
							if (source_pts < video_pts_value[v])
							{
								//write_buffer.put("cut_in_time", "" + video_pts_value[v]); // save value for cuts
								break rangeloop;
							}

							else if (source_pts == video_pts_value[v] || source_pts < video_pts_value[v+1])
							{
								write = true;
								break rangeloop;
							}

							v += 2;

							if (v < video_pts_value.length)
							{
								//write_buffer.put("cut_out_time", "" + video_pts_value[v-1]); // save value for cuts
								time_difference += (video_pts_value[v] - video_pts_value[v-1]);
							}
						}
					}

					else
						write = true;


					// logging
					if (debug)
						System.out.println("pos "+ (count-46) + "/vbi "+vbi+"/ "+magazine+"-"+row+"-"+page_number+"-"+subpage_number+"/pts "+source_pts+"/ "+timeformat_1.format(new Date(source_pts/90))+"/ "+Integer.toHexString(page_value)+"/wr "+write+"/lo "+loadpage+"/lastp "+lastpage_match+"/pagm "+page_match+"/v "+(v<video_pts_value.length ? video_pts_value[v] : v));

					if (row != 0 && magazine != page_value>>>8) //row > 0, but not of current magazine
						continue readloop;

					if (row == 0)  //accept all 0-rows of all magazines till now
					{
						boolean interrupt_loading = false;

						//stop loading if same magazine, but other page
						//stop loading if interrupted_flag by any page only if magazine_serial == 1, means rows of diff. pages of multiple magazines don't overlap by sent order
						if (page_match || magazine == page_value>>>8 || flags.get("magazine_serial").toString().equals("1") )
							interrupt_loading = true;

						if (debug)
							System.out.println("int " + interrupt_loading +"/load "+ loadpage + "/wri "+ write + "/wb " + write_buffer.size() + "/lb " + load_buffer.size());

						// page header does not interrupt an active loading
						if (!interrupt_loading)
							continue readloop;

						// megaradio_mode, magazine 8 only, other magazines are already ignored before page check
						if (subtitle_type == MEGARADIO)
						{
							switch (Integer.parseInt(page_number, 16))
							{
							case 0xA:
							case 0xB:
							case 0xC:
								loadpage=true; 
							}

							switch (Integer.parseInt(subpage_number, 16))
							{
							case 0x0D:
							case 0x0E:
							case 0x1C:
							case 0x1D:
							//case 0x2A:
							case 0x2B:
							case 0x2C:
							case 0x2D:
								loadpage=false; 
							}

							continue readloop; 
						}

						// row 0 defines out_time of current buffered page forced by an interrupt event, 
						// sets the out_time of displaying and write it out
						if (lastpage_match)
						{
							long out_time = source_pts - time_difference;

							/**
							 * adapt out_time of page, if termination of it was detected later than 80ms after last row
							 */
							Object buffer_time = load_buffer.get("buffer_time");

							if (buffer_time != null)
							{
								long l = Long.parseLong(buffer_time.toString());

								if (source_pts - l > 7200)
								{
									out_time = l - time_difference + 7200;

									if (debug)
										System.out.println("termination for in_time too late, new out_time: " + out_time);
								}
							}

							if (write)  //buffered page can be written
							{
								write_buffer.put("out_time", "" + out_time);

								while (true)
								{
									character_count = 0;

									if ( !write_buffer.containsKey("active") )
										break;

									if ( !write_buffer.containsKey("in_time") )
										break;
//start + end equals ?										
									if ( write_buffer.get("in_time").toString().equals(write_buffer.get("out_time").toString()) )
										break;

									switch (subtitle_type)
									{
									case EXPORT_TEXT:  // free
										print_buffer.print( "in_" + timeformat_1.format( new Date( Long.parseLong( write_buffer.get("in_time").toString()) / 90) ));
										print_buffer.println( "|out_" + timeformat_1.format( new Date( Long.parseLong( write_buffer.get("out_time").toString()) / 90) )); 
										break;

									case EXPORT_SC:  // SC
										print_buffer.print( teletext.SMPTE( timeformat_1.format( new Date( Long.parseLong( write_buffer.get("in_time").toString()) / 90) ), (long)CommonParsing.getVideoFramerate()) + "&");
										print_buffer.print( teletext.SMPTE( timeformat_1.format( new Date( Long.parseLong( write_buffer.get("out_time").toString()) / 90) ), (long)CommonParsing.getVideoFramerate()) + "#");
										break;

									case EXPORT_SUB:  // SUB
										print_buffer.print( "{" + ( (long)(Long.parseLong( write_buffer.get("in_time").toString()) / CommonParsing.getVideoFramerate())) + "}");
										print_buffer.print( "{" + ( (long)(Long.parseLong( write_buffer.get("out_time").toString()) / CommonParsing.getVideoFramerate())) + "}");
										break;

									case EXPORT_SRT:  // SRT
									case EXPORT_SRTC: // SRT colored
										print_buffer.println( "" + (seiten + 1));
										print_buffer.print( timeformat_2.format( new Date( Long.parseLong( write_buffer.get("in_time").toString()) / 90) ));
										print_buffer.println(" --> " + timeformat_2.format( new Date( Long.parseLong( write_buffer.get("out_time").toString()) / 90) ));
										break;

									case EXPORT_W3C:  // W3C Timed-Text
										print_buffer.print( "<p begin=\"" + timeformat_1.format( new Date( Long.parseLong( write_buffer.get("in_time").toString()) / 90) ) + "\" end=\"" + timeformat_1.format( new Date( Long.parseLong( write_buffer.get("out_time").toString()) / 90) ) + "\">" );
										break;

									case EXPORT_GPAC:  // GPAC Timed-Text
										print_buffer.print( "<TextSample sampleTime=\"" + timeformat_1.format( new Date( Long.parseLong( write_buffer.get("in_time").toString()) / 90) ) + "\" text=\"'" );
										break;

									case EXPORT_SSA:  // SSA
										print_buffer.print( teletext.getSSALine()[0] + timeformat_1.format( new Date( Long.parseLong( write_buffer.get("in_time").toString()) / 90) ).substring(1, 11) + ",");
										print_buffer.print( timeformat_1.format( new Date( Long.parseLong( write_buffer.get("out_time").toString()) / 90) ).substring(1, 11) + teletext.getSSALine()[1]);
										break;
	
									case EXPORT_STL:  // STL
										print_buffer.print( teletext.SMPTE(timeformat_1.format( new Date( Long.parseLong( write_buffer.get("in_time").toString()) / 90) ), (long)CommonParsing.getVideoFramerate()) + ",");
										print_buffer.print( teletext.SMPTE(timeformat_1.format( new Date( Long.parseLong( write_buffer.get("out_time").toString()) / 90) ), (long)CommonParsing.getVideoFramerate()) + ",");
										break;

									case EXPORT_SUP:  // SUP
										long sup_in_time = Long.parseLong( write_buffer.get("in_time").toString() );

										if ( sup_in_time >= 0 )
										{
											for (int a = 1; a < 24; a++)
												if ( write_buffer.containsKey("" + a) )
													picture_String.add(write_buffer.get("" + a));

											while (picture_String.size() > subpicture.getMaximumLines()) // max. lines as defined
												picture_String.remove(0);

											subpicture.createPictureFromTeletext( picture_String.toArray(), job_processing.getStatusStrings());
											//byte_buffer.write( subpicture.writeRLE( sup_in_time, 0xB4)); // alias duration 1.800 sec if out_time is missing

											//if ( write_buffer.containsKey("out_time") )
											//	out.write(subpicture.setTime( byte_buffer.toByteArray(), Long.parseLong( write_buffer.get("out_time").toString())));

											int displaytime = write_buffer.containsKey("out_time") ? subpicture.setTime(sup_in_time, Long.parseLong(write_buffer.get("out_time").toString())) : 0xB4;

											byte_buffer.write( subpicture.writeRLE(sup_in_time, displaytime));
											out.write(byte_buffer.toByteArray());
										}

										picture_String.clear();
									}

									int b = 0;

									for (int a = 1; subtitle_type != EXPORT_SUP && a < 24; a++)
									{
										if ( !write_buffer.containsKey("" + a) )
											continue;

										String str = write_buffer.get("" + a).toString();

										switch (subtitle_type)
										{
										case EXPORT_TEXT:  // free
										case EXPORT_SRT:  // SRT
										case EXPORT_SRTC:  // SRTC
											print_buffer.println(str); 
											break;

										case EXPORT_W3C:  // W3C Timed Text
											print_buffer.print( (b > 0 ? "<br />" : "") + str); 
											break;

										case EXPORT_GPAC:  // GPAC Timed Text
											print_buffer.print( (b > 0 ? "''" : "") + str);
											break;

										case EXPORT_SC:  // SC
											print_buffer.print(str); 
											break;

										case EXPORT_SUB:  //	 SUB
										case EXPORT_STL:  // STL
											print_buffer.print( (b > 0 ? "|" : "") + str);
											break;

										case EXPORT_SSA:  // SSA
											print_buffer.print( (b > 0 ? "\\n" : "") + str);
										}

										b++;
									}

									if (subtitle_type == EXPORT_W3C)
									{
										print_buffer.print("</p>");
									}

									if (subtitle_type == EXPORT_GPAC)
									{
										print_buffer.println("'\">");
										for (int a = 1; a < 24; a++)
										{
											if ( !write_buffer.containsKey("color" + a) )
												continue;

											StringTokenizer colors = new StringTokenizer(write_buffer.get("color" + a).toString(), "|");
											while (colors.hasMoreTokens()) {
												print_buffer.println(colors.nextToken());
											}
										}
										print_buffer.print("</TextSample>");
									}

									if (subtitle_type != EXPORT_SUP && b > 0)
									{
										print_buffer.println();
										print_buffer.flush();
										byte_buffer.writeTo(out); 
									}

									seiten++;
									Common.getGuiInterface().showExportStatus(Resource.getString("teletext.status"), seiten);
									break;
								}
							}

							byte_buffer.reset(); 
							write_buffer.clear();
						}

						lastpage_match = page_match;

						// row 0 defines completion of current page to buffer, 
						// sets the in_time of displaying but cannot write it w/o still unknown out_time
						if (loadpage)
						{
							write_buffer.clear();

							if (!vptsdata && time_difference == 0 && !KeepOriginalTimecode)
								time_difference = source_pts;

							long in_time = source_pts - time_difference;
							boolean rows = false;

							/**
							 * adapt in_time of page, if termination of it was detected later than 80ms after last row
							 */
							Object buffer_time = load_buffer.get("buffer_time");

							if (buffer_time != null)
							{
								long l = Long.parseLong(buffer_time.toString());

								if (source_pts - l > 7200)
								{
									in_time = l - time_difference + 7200;

									if (debug)
										System.out.println("termination too late, new in_time: " + in_time);
								}
							}


							// copy keys+values to clear for next page, only row 1..23 used instead of 0..31
							for (int a = 1; a < 24; a++) 
							{
								if ( !load_buffer.containsKey("" + a) )
									continue;

								rows = true; // non blank page
								write_buffer.put("" + a, load_buffer.get("" + a));

								if ( !load_buffer.containsKey("color" + a) )
									continue;

								write_buffer.put("color" + a, load_buffer.get("color" + a));
							}

							if (rows && write) // if false, in_time has to be set/updated at synccheck above until an exported gop pts area
								write_buffer.put("in_time", "" + in_time);

							if (!rows)
								lastpage_match = false;

							else
								write_buffer.put("active", "1");

							teletext.clearEnhancements();

							load_buffer.clear();
						}

						loadpage = page_match;

	/**
	 * test 0903
	 * updates current timestamp of last packet
	 */
	if (SpecialTermination)
		load_buffer.put("buffer_time", String.valueOf(source_pts));


						// logging
						if (debug)
							System.out.println("lo " + loadpage + "/lp_p "+lastpage_match+"/pg_m "+page_match+"/wbuf: " + write_buffer.toString());

						continue readloop;
					}

					// only rows > 0

					// logging
					if (debug)
						System.out.println("load " + loadpage + "/lbuf " + load_buffer.toString());

					// ignore if row is not of expected magazine
					if (magazine != page_value>>>8)
						continue readloop;

					// load and filter re-defined chars from X26/0..15 triplets
					if (row > 23 && subtitle_type != 0)
					{
						if (row == 29 || loadpage)
							teletext.setEnhancements(packet, row, character_set);

						continue readloop;
					}

					if (!loadpage)
						continue readloop;

					if (subtitle_type == MEGARADIO)  // megaradio, simple decode the bytes of row 1..23
					{
						for (int b = (row == 1) ? 17: 0; row < 24 && b < 39; b++) // framebytes to MSB
							out.write(teletext.bytereverse(packet[7+b]));

						continue readloop;
					}

					// decode row 1..23 , 0=header, 24 fasttext labels, 25 supressedheader, >26 non text packets 

					String str = null;
					int[] picture_data = null;

					switch (subtitle_type)
					{
					case EXPORT_TEXT:
						str = teletext.buildString(packet, 6, 40, row, character_set, 0, true, BoxedMode);
						break;

					case EXPORT_SC:
					case EXPORT_STL:
					case EXPORT_SUB:
					case EXPORT_SRT:
						str = teletext.buildString(packet, 6, 40, row, character_set, 0, true, BoxedMode).trim();
						break;

					case EXPORT_SRTC:
						str = teletext.buildString(packet, 6, 40, row, character_set, 2, true, BoxedMode).trim();
						break;

					case EXPORT_W3C:
						str = teletext.buildString(packet, 6, 40, row, character_set, 3, true, BoxedMode).trim();
						break;

					case EXPORT_GPAC:
						str = teletext.buildString(packet, 6, 40, row, character_set, 0, true, BoxedMode).trim();
						load_buffer.put("color" + row, teletext.buildString(packet, 6, 40, row, character_set, 4, true, BoxedMode, character_count).trim());
						character_count += str.length() + 1;
						str = escapeXml(str);
						break;

					case EXPORT_SSA:
						str = teletext.buildString(packet, 6, 40, row, character_set, 1, true, BoxedMode).trim();
						break;

					case EXPORT_SUP:
						picture_data = teletext.buildCharArray(packet, 6, 40, row, character_set, true, BoxedMode, TextAlignment);
					}

					if (str != null && !str.equals(""))
						load_buffer.put("" + row, str);

					else if (picture_data != null)
						load_buffer.put("" + row, picture_data);

					if (debug) 
						System.out.println("row " + row + ": " + str + "/lb " + load_buffer.size()); 

					/**
					 * updates current timestamp of last packet
					 */
					load_buffer.put("buffer_time", String.valueOf(source_pts));

				} // return to read next packet

  
				//write out last page in buffer
				if (!write_buffer.isEmpty() && write)
				{
					long out_time = (vptsdata ? video_pts_value[video_pts_value.length - 1] : source_pts) - time_difference;
					write_buffer.put("out_time", "" + out_time);

					while (true)
					{
						if ( !write_buffer.containsKey("active") || !write_buffer.containsKey("in_time") )
							break;

						switch (subtitle_type)
						{
						case EXPORT_TEXT:  // free
							print_buffer.print( "in_" + timeformat_1.format( new Date( Long.parseLong( write_buffer.get("in_time").toString()) / 90) ));
							print_buffer.println( "|out_" + timeformat_1.format( new Date( Long.parseLong( write_buffer.get("out_time").toString()) / 90) )); 
							break;

						case EXPORT_SC:  // SC
							print_buffer.print( teletext.SMPTE(timeformat_1.format( new Date( Long.parseLong( write_buffer.get("in_time").toString()) / 90) ), (long)CommonParsing.getVideoFramerate()) + "&");
							print_buffer.print( teletext.SMPTE(timeformat_1.format( new Date( Long.parseLong( write_buffer.get("out_time").toString()) / 90) ), (long)CommonParsing.getVideoFramerate()) + "#");
							break;

						case EXPORT_SUB:  // SUB
							print_buffer.print( "{" + ( (long)(Long.parseLong( write_buffer.get("in_time").toString()) / CommonParsing.getVideoFramerate())) + "}");
							print_buffer.print( "{" + ( (long)(Long.parseLong( write_buffer.get("out_time").toString()) / CommonParsing.getVideoFramerate())) + "}");
							break;

						case EXPORT_SRT:  // SRT
						case EXPORT_SRTC: // SRT colored
							print_buffer.println( "" + (seiten + 1));
							print_buffer.print( timeformat_2.format( new Date( Long.parseLong( write_buffer.get("in_time").toString()) / 90) ));
							print_buffer.println(" --> " + timeformat_2.format( new Date( Long.parseLong( write_buffer.get("out_time").toString()) / 90) ));
							break;

						case EXPORT_W3C:  // W3C Timed-Text
							print_buffer.print( "<p begin=\"" + timeformat_1.format( new Date( Long.parseLong( write_buffer.get("in_time").toString()) / 90) ) + "\" end=\"" + timeformat_1.format( new Date( Long.parseLong( write_buffer.get("out_time").toString()) / 90) ) + "\">" );
							break;

						case EXPORT_GPAC:  // GPAC Timed-Text
							print_buffer.print( "<TextSample sampleTime=\"" + timeformat_1.format( new Date( Long.parseLong( write_buffer.get("in_time").toString()) / 90) ) + "\" text=\"'" );
							break;

						case EXPORT_SSA:  // SSA
							print_buffer.print( teletext.getSSALine()[0] + timeformat_1.format( new Date( Long.parseLong( write_buffer.get("in_time").toString()) / 90) ).substring(1, 11) + ",");
							print_buffer.print( timeformat_1.format( new Date( Long.parseLong( write_buffer.get("out_time").toString()) / 90) ).substring(1, 11) + teletext.getSSALine()[1]);
							break;

						case EXPORT_STL:  // STL
							print_buffer.print( teletext.SMPTE(timeformat_1.format( new Date( Long.parseLong( write_buffer.get("in_time").toString()) / 90) ), (long)CommonParsing.getVideoFramerate()) + ",");
							print_buffer.print( teletext.SMPTE(timeformat_1.format( new Date( Long.parseLong( write_buffer.get("out_time").toString()) / 90) ), (long)CommonParsing.getVideoFramerate()) + ",");
							break;

						case EXPORT_SUP:  // SUP
							long sup_in_time = Long.parseLong( write_buffer.get("in_time").toString() );

							if ( sup_in_time >= 0 )
							{
								for (int a = 1; a < 24; a++)
									if ( write_buffer.containsKey("" + a) )
										picture_String.add(write_buffer.get("" + a));

								while (picture_String.size() > subpicture.getMaximumLines())
									picture_String.remove(0);

								subpicture.createPictureFromTeletext( picture_String.toArray(), job_processing.getStatusStrings());
							//	byte_buffer.write( subpicture.writeRLE( sup_in_time, 0xB4)); // alias duration 2.000 sec if out_time is missing
							//	out.write(subpicture.setTime( byte_buffer.toByteArray(), Long.parseLong( write_buffer.get("out_time").toString())));

								int displaytime = subpicture.setTime(sup_in_time, Long.parseLong(write_buffer.get("out_time").toString()));

								byte_buffer.write( subpicture.writeRLE(sup_in_time, displaytime));
								out.write(byte_buffer.toByteArray());
							}

							picture_String.clear();
						}

						int b = 0;

						for (int a = 1; subtitle_type != EXPORT_SUP && a < 24; a++)
						{
							if ( !write_buffer.containsKey("" + a) )
								continue;

							String str = write_buffer.get("" + a).toString();

							switch (subtitle_type)
							{
							case EXPORT_TEXT:  // free
							case EXPORT_SRT:  // SRT
								print_buffer.println(str); 
								break;

							case EXPORT_SRTC:  // SRT colored
								print_buffer.println(str); 
								break;

							case EXPORT_W3C:  // W3C Timed Text
								print_buffer.print( (b > 0 ? "<br />" : "") + str); 
								break;

							case EXPORT_GPAC:  // GPAC Timed Text
								print_buffer.print( (b > 0 ? "''" : "") + str);
								break;

							case EXPORT_SC:  // SC
								print_buffer.print(str); 
								break;

							case EXPORT_SUB:  // SUB
							case EXPORT_STL:  // STL
								print_buffer.print( (b > 0 ? "|" : "") + str);
								break;

							case EXPORT_SSA:  // SSA
								print_buffer.print( (b > 0 ? "\\n" : "") + str);
							}

							b++;
						}

						if (subtitle_type == EXPORT_W3C)
						{
							print_buffer.print("</p>");
						}

						if (subtitle_type == EXPORT_GPAC)
						{
							print_buffer.println("'\">");
							for (int a = 1; a < 24; a++)
							{
								if ( !write_buffer.containsKey("color" + a) )
									continue;

								StringTokenizer colors = new StringTokenizer(write_buffer.get("color" + a).toString(), "|");
								while (colors.hasMoreTokens()) {
									print_buffer.println(colors.nextToken());
								}
							}
							print_buffer.print("</TextSample>");
						}

						if (subtitle_type != EXPORT_SUP && b > 0)
						{
							print_buffer.println();
							print_buffer.flush();
							byte_buffer.writeTo(out); 
						}

						seiten++;
						Common.getGuiInterface().showExportStatus(Resource.getString("teletext.status"), seiten);
						break;
					}
				}

				if (subtitle_type == EXPORT_GPAC)
				{
					String[] GPACfoot = teletext.getGPACFoot();

					for (int a = 0; a < GPACfoot.length; a++)
						print_buffer.println(GPACfoot[a]);

					print_buffer.flush();
					byte_buffer.writeTo(out);
					byte_buffer.reset();
				}

				if (subtitle_type == EXPORT_W3C)
				{
					String[] W3Cfoot = teletext.getW3CFoot();

					for (int a = 0; a < W3Cfoot.length; a++)
						print_buffer.println(W3Cfoot[a]);

					print_buffer.flush();
					byte_buffer.writeTo(out);
					byte_buffer.reset();
				}

				if (debug) 
					System.out.println();

				Common.setMessage(Resource.getString("teletext.msg.summary", "" + seiten, page));

				if (seiten > 0) 
					Common.setMessage(Resource.getString("msg.newfile") + " " + ttxfile);

				write_buffer.clear();

				in.close();
				print_buffer.flush(); 
				print_buffer.close();
				byte_buffer.flush(); 
				byte_buffer.close();
				out.flush(); 
				out.close();

				File ttxfile1 = new File(ttxfile); 

				if (subtitle_type != MEGARADIO && seiten==0) 
					ttxfile1.delete();

				else if (subtitle_type == MEGARADIO)
				{
					String pts_file = fparent + ".pts";

					RandomAccessFile log = new RandomAccessFile(pts_file, "rw");

					log.writeLong(0L); 
					log.writeLong(0L); 
					log.close();

					Common.setMessage(ttxfile);
					Common.setMessage(Resource.getString("working.filetype", Keys.ITEMS_FileTypes[CommonParsing.ES_MPA_TYPE]));

					// audiofile goes to synch methode 
					new StreamProcess(CommonParsing.MPEG_AUDIO, collection, ttxfile, pts_file, "mp", "-1");

					new File(ttxfile).delete();
					new File(pts_file).delete();

					return;
				}

				else
				{ 
					if (subtitle_type == EXPORT_SUP)
						Ifo.createIfo(ttxfile, subpicture.getUserColorTableArray());

					job_processing.countMediaFilesExportLength(ttxfile1.length());
					job_processing.addSummaryInfo(Resource.getString("teletext.summary", Common.adaptString(job_processing.countPictureStream(), 2), "" + seiten, "" + page, infoPTSMatch(filename_pts, videofile_pts, vptsdata, ptsdata)) + "'" + ttxfile1 + "'");

					//vobsub
					if (subtitle_type == EXPORT_SUP && ExportAsVobSub)
						new Sup2VobSub(ttxfile, subpicture.getUserColorTableArray());
				}

			} catch (IOException e2) { 

				Common.setExceptionMessage(e2);
			}


			//2nd offset applies
			if (subtitle_type == EXPORT_SUP && SUP_Offset[1] >= 0)
			{
				Common.setMessage("");
				Common.setMessage(Resource.getString("teletext.msg.newrun") + ": " + SUP_Offset[1]);

				StreamProcessSubpicture streamprocess = new StreamProcessSubpicture("[P1]");

				streamprocess.set_XY_Offset(0, SUP_Offset[0] - SUP_Offset[1]); //offset x +- 0, y = 32 - 96 (= -64 e.g.)
				streamprocess.processStream(collection, new XInputFile(new File(ttxfile)), CommonParsing.ES_TYPE);
			}

		}  // end for

		Common.updateProgressBar(size, size);

		if (ShowSubpictureWindow)
			Common.getGuiInterface().hideSubpicture();

	}

	/**
	 * Get String representation of the object.
	 * 
	 * @return String representation of the object
	 */
	private String escapeXml(String str) {

		str = replaceStringByString(str, "&", "&amp;"); //1st
		str = replaceStringByString(str, "\"", "&quot;");
		str = replaceStringByString(str, "'", "&apos;");
		str = replaceStringByString(str, ">", "&gt;");
		str = replaceStringByString(str, "<", "&lt;");

		return str;
	}

	/**
	 * @return String, checked of arg1 and replaced with arg2 JDK 1.2.2
	 *         compatibility, replacement of newer String.replaceAll()
	 */
	private String replaceStringByString(String name, String arg1, String arg2) {

		if (name == null) return name;

		StringBuffer sb = new StringBuffer(name);

		for (int i = 0; (i = sb.toString().indexOf(arg1, i)) != -1;)
			sb.replace(i, i + 1, arg2);

		return sb.toString();
	}

}
