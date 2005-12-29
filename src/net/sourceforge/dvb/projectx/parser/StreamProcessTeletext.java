/*
 * @(#)StreamParser
 *
 * Copyright (c) 2005 by dvb.matt, All rights reserved.
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

import net.sourceforge.dvb.projectx.subtitle.Teletext;
import net.sourceforge.dvb.projectx.subtitle.UnicodeWriter;
import net.sourceforge.dvb.projectx.subtitle.Subpicture;

import net.sourceforge.dvb.projectx.thirdparty.Ifo;

/**
 * main thread
 */
public class StreamProcessTeletext extends StreamProcessBase {

	/**
	 * 
	 */
	public StreamProcessTeletext(JobCollection collection, XInputFile xInputFile, String filename_pts, String filename_type, String videofile_pts, int isElementaryStream)
	{
		super();

		processStream(collection, xInputFile, filename_pts, filename_type, videofile_pts, isElementaryStream);
	}

	/**
	 * decoding teletext stream
	 */
	private void processStream(JobCollection collection, XInputFile xInputFile, String filename_pts, String filename_type, String videofile_pts, int isElementaryStream)
	{
		int LB1 = -1;
		long size = 0;
		boolean debug = collection.DebugMode();

		boolean Message_2 = Common.getSettings().getBooleanProperty(Keys.KEY_MessagePanel_Msg2);
		boolean ShowSubpictureWindow = Common.getSettings().getBooleanProperty(Keys.KEY_showSubpictureWindow);
		boolean DecodeMegaradio = Common.getSettings().getBooleanProperty(Keys.KEY_SubtitlePanel_decodeMegaradio);
		boolean ExportTextAsUnicode = Common.getSettings().getBooleanProperty(Keys.KEY_SubtitlePanel_exportTextAsUnicode);
		boolean DecodeHiddenRows = Common.getSettings().getBooleanProperty(Keys.KEY_SubtitlePanel_decodeHiddenRows);
		boolean KeepOriginalTimecode = Common.getSettings().getBooleanProperty(Keys.KEY_SubtitlePanel_keepOriginalTimecode);
//test
		boolean SpecialTermination = Common.getSettings().getBooleanProperty(Keys.KEY_SubtitlePanel_specialTermination);

		String SubtitleExportFormat = Common.getSettings().getProperty(Keys.KEY_SubtitleExportFormat);
		String SubtitleFont = Common.getSettings().getProperty(Keys.KEY_SubtitleFont);
		String Format_SUP_Values = Common.getSettings().getProperty(Keys.KEY_SubtitlePanel_Format_SUP_Values);

		JobProcessing job_processing = collection.getJobProcessing();

		Subpicture subpicture = Common.getSubpictureClass();

		if (ShowSubpictureWindow && (SubtitleExportFormat.equalsIgnoreCase(Keys.ITEMS_SubtitleExportFormat[6].toString()) || SubtitleExportFormat.equalsIgnoreCase(Keys.ITEMS_SubtitleExportFormat[7].toString())))
			Common.getGuiInterface().showSubpicture();

		if (SubtitleExportFormat.equalsIgnoreCase(Keys.ITEMS_SubtitleExportFormat[7].toString()) || SubtitleExportFormat.equalsIgnoreCase(Keys.ITEMS_SubtitleExportFormat[6].toString())) // SUP + SON, set variables
			LB1 = subpicture.set(SubtitleFont, Format_SUP_Values);

		for (int LB = 0; LB < 2; LB++)
		{
			String[] userdefined_pages = {
				Common.getSettings().getProperty(Keys.KEY_SubtitlePanel_TtxPage1),
				Common.getSettings().getProperty(Keys.KEY_SubtitlePanel_TtxPage2),
				Common.getSettings().getProperty(Keys.KEY_SubtitlePanel_TtxPage3),
				Common.getSettings().getProperty(Keys.KEY_SubtitlePanel_TtxPage4),
				Common.getSettings().getProperty(Keys.KEY_SubtitlePanel_TtxPage5),
				Common.getSettings().getProperty(Keys.KEY_SubtitlePanel_TtxPage6)
			};

			for (int pn = 0; pn < userdefined_pages.length; pn++)
			{
				String page = "0";

				if (!DecodeMegaradio)
				{
					page = userdefined_pages[pn];

					if (page.equalsIgnoreCase("null")) 
						continue;
				}
				else
					pn = userdefined_pages.length;

			//  not supported as an elementary stream
			//	String fchild = isElementaryStream == CommonParsing.ES_TYPE ? collection.getOutputName(xInputFile.getName()) : xInputFile.getName();
			//	String fchild = collection.getOutputName(xInputFile.getName());
				String fchild = xInputFile.getName();
				String fparent = collection.getOutputNameParent(fchild);

				size = xInputFile.length();

				Common.getGuiInterface().initTtxPageMatrix(fchild);

				Teletext.clearEnhancements();

				String LBs = LB == 1 ? "[1]" : "";

				if (!DecodeMegaradio) 
					fparent += "[" + page + "]" + LBs;


				String ttxfile;

				int subtitle_type;

				if (DecodeMegaradio) 
				{
					ttxfile = fparent + ".mgr";
					subtitle_type = 0;
				}

				else if (SubtitleExportFormat.equalsIgnoreCase(Keys.ITEMS_SubtitleExportFormat[1].toString()))
				{
					ttxfile = fparent + ".sc";
					subtitle_type = 2;
				}

				else if (SubtitleExportFormat.equalsIgnoreCase(Keys.ITEMS_SubtitleExportFormat[2].toString()))
				{
					ttxfile = fparent + ".sub";
					subtitle_type = 3;
				}

				else if (SubtitleExportFormat.equalsIgnoreCase(Keys.ITEMS_SubtitleExportFormat[3].toString())) 
				{
					ttxfile = fparent + ".srt";
					subtitle_type = 4;
				}

				else if (SubtitleExportFormat.equalsIgnoreCase(Keys.ITEMS_SubtitleExportFormat[5].toString())) 
				{
					ttxfile = fparent + ".ssa";
					subtitle_type = 5;
				}

				else if (SubtitleExportFormat.equalsIgnoreCase(Keys.ITEMS_SubtitleExportFormat[7].toString())) 
				{
					ttxfile = fparent + ".sup";
					subtitle_type = 6;
				}

				else if (SubtitleExportFormat.equalsIgnoreCase(Keys.ITEMS_SubtitleExportFormat[4].toString())) 
				{
					ttxfile = fparent + ".stl";
					subtitle_type = 7;
				}

				else if (SubtitleExportFormat.equalsIgnoreCase(Keys.ITEMS_SubtitleExportFormat[6].toString())) //placeholder for .son export
				{
					ttxfile = fparent + ".ssa"; //.son
					subtitle_type = 5;  // 8
				}

				else if (SubtitleExportFormat.equalsIgnoreCase("null"))
					continue;

				else   // free format
				{
					ttxfile = fparent + ".txt";
					subtitle_type = 1;
				}

				Common.setMessage(Resource.getString("teletext.msg.output") + " " + ttxfile.substring(ttxfile.length() - 3));

				if (ExportTextAsUnicode && subtitle_type != 6)
					Common.setMessage("-> " + Resource.getString(Keys.KEY_SubtitlePanel_exportTextAsUnicode[0]));

				if (DecodeHiddenRows)
					Common.setMessage("-> " + Resource.getString(Keys.KEY_SubtitlePanel_decodeHiddenRows[0]));

				if (KeepOriginalTimecode)
					Common.setMessage("-> " + Resource.getString(Keys.KEY_SubtitlePanel_keepOriginalTimecode[0]));

				if (SpecialTermination)
					Common.setMessage("-> " + Resource.getString(Keys.KEY_SubtitlePanel_specialTermination[0]));


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

				int page_value = subtitle_type == 0 ? 0x800 : Integer.parseInt(page, 16);
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

					UnicodeWriter print_buffer = new UnicodeWriter(byte_buffer, ExportTextAsUnicode);

					Common.setMessage(Resource.getString("teletext.msg.tmpfile", xInputFile.getName(), "" + size));
					Common.setMessage(Resource.getString("teletext.msg.search") + " " + (subtitle_type == 0 ? Resource.getString("teletext.msg.megaradio") : Resource.getString("teletext.msg.page") + " " + page));

					Common.updateProgressBar(Resource.getString("teletext.progress") + " " + (subtitle_type == 0 ? Resource.getString("teletext.msg.megaradio") : Resource.getString("teletext.msg.page") + " " + page), 0, 0);

					long[] pts_value = {0};
					long[] pts_position = {0};
					long[] video_pts_value = {0};
					long[] vtime = {0};

					Common.getGuiInterface().showExportStatus(Resource.getString("teletext.status"), seiten);

					//System.gc();

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

					//System.gc();

					// 1st line 
					switch (subtitle_type)
					{
					case 2:
						print_buffer.println("Subtitle File Mark:"+((CommonParsing.getVideoFramerate()==3600L) ? "2" : "1"));
						print_buffer.flush();
						byte_buffer.writeTo(out);
						byte_buffer.reset();
						break;

					case 5:
						String[] SSAhead = Teletext.getSSAHead();

						for (int a = 0; a < SSAhead.length; a++) 
							print_buffer.println(SSAhead[a]);

						print_buffer.flush();
						byte_buffer.writeTo(out);
						byte_buffer.reset();
						break;

					case 7:
						String[] STLhead = Teletext.getSTLHead(Common.getVersionName() + " on " + DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(System.currentTimeMillis())));

						for (int a = 0; a < STLhead.length; a++) 
							print_buffer.println(STLhead[a]);

						print_buffer.flush();
						byte_buffer.writeTo(out);
						byte_buffer.reset();
						break;

					case 8:  //DM14052004 081.7 int02 add, still unused!
						String[] SONhead = Teletext.getSONHead(new File(ttxfile).getParent(), (long)CommonParsing.getVideoFramerate());

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

					//yield();

					boolean missing_syncword = false;
					boolean vps = false;
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

					readloop:
					while ( count < size )
					{
						Common.updateProgressBar(count, size);

						//yield();

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

						case 0xFF: // hidden 
							if (DecodeHiddenRows)
								valid = true;
							break; 

						default:  // others, unknown
							if (debug) 
								System.out.println(" unkn_"+Integer.toHexString(0xFF & packet[0])+"/"+(count-46));
							//continue readloop;
						}

						// logging
						if (debug)
						{
							System.out.println();

							for (int a = 0; a < 46; a++)
								System.out.print(" " + ((0xFF & packet[a])<0x10 ? "0" : "") + Integer.toHexString(0xFF & packet[a]).toUpperCase());

							System.out.println();
						}

						if (!valid)
							continue readloop;

						vbi = ((0x20 & packet[2]) != 0 ? 0 : 313) + (0x1F & packet[2]);

						if (!vps)
						{
							row = 0xFF & Teletext.bytereverse((byte)((0xF & Teletext.hamming_decode(packet[4]))<<4 | (0xF & Teletext.hamming_decode(packet[5]))));
							magazine = (7 & row) == 0 ? 8 : (7 & row);
							row >>>= 3;
						}

						else
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


						// X3/31.1 ttx provider
						if (magazine == 3 && row == 31 && packet[7] == 0x40 && packet[8] == 0x57 && provider.equals(""))
						{
							provider = Teletext.makestring(packet, 10, 34, 31, 0, 0, false).trim();
							Common.setMessage(Resource.getString("teletext.msg.provider") + " " + provider);
						}

						// X8/30.0 program title
						else if (magazine == 8 && row == 30 && packet[7] == (byte)0xA8)
						{
							String str = Teletext.makestring(packet, 26, 20, 30, 0, 0, true).trim() + " ";

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
								flag |= (0xF & Teletext.bytereverse( Teletext.hamming_decode(packet[8+a]) )>>>4 ) <<(a*4);

							page_number = Integer.toHexString(0xF & Teletext.bytereverse( Teletext.hamming_decode(packet[7]) )>>>4 ).toUpperCase() +
								Integer.toHexString(0xF & Teletext.bytereverse( Teletext.hamming_decode(packet[6]) )>>>4 ).toUpperCase();

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
								String str = magazine + page_number + "  " + subpage_number + "  " + Teletext.makestring(packet, 14, 32, 0, (7 & flag>>>21), 0, true) + "  " + program_title;

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
							if (subtitle_type == 0)
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
										if ( !write_buffer.containsKey("active") )
											break;

										if ( !write_buffer.containsKey("in_time") )
											break;

										switch (subtitle_type)
										{
										case 1:  // free
											print_buffer.print( "in_" + timeformat_1.format( new Date( Long.parseLong( write_buffer.get("in_time").toString()) / 90) ));
											print_buffer.println( "|out_" + timeformat_1.format( new Date( Long.parseLong( write_buffer.get("out_time").toString()) / 90) )); 
											break;

										case 2:  // SC
											print_buffer.print( Teletext.SMPTE( timeformat_1.format( new Date( Long.parseLong( write_buffer.get("in_time").toString()) / 90) ), (long)CommonParsing.getVideoFramerate()) + "&");
											print_buffer.print( Teletext.SMPTE( timeformat_1.format( new Date( Long.parseLong( write_buffer.get("out_time").toString()) / 90) ), (long)CommonParsing.getVideoFramerate()) + "#");
											break;

										case 3:  // SUB
											print_buffer.print( "{" + ( (long)(Long.parseLong( write_buffer.get("in_time").toString()) / CommonParsing.getVideoFramerate())) + "}");
											print_buffer.print( "{" + ( (long)(Long.parseLong( write_buffer.get("out_time").toString()) / CommonParsing.getVideoFramerate())) + "}");
											break;

										case 4:  // SRT
											print_buffer.println( "" + (seiten + 1));
											print_buffer.print( timeformat_2.format( new Date( Long.parseLong( write_buffer.get("in_time").toString()) / 90) ));
											print_buffer.println(" --> " + timeformat_2.format( new Date( Long.parseLong( write_buffer.get("out_time").toString()) / 90) ));
											break;

										case 5:  // SSA
											print_buffer.print( Teletext.getSSALine()[0] + timeformat_1.format( new Date( Long.parseLong( write_buffer.get("in_time").toString()) / 90) ).substring(1, 11) + ",");
											print_buffer.print( timeformat_1.format( new Date( Long.parseLong( write_buffer.get("out_time").toString()) / 90) ).substring(1, 11) + Teletext.getSSALine()[1]);
											break;
	
										case 7:  // STL
											print_buffer.print( Teletext.SMPTE(timeformat_1.format( new Date( Long.parseLong( write_buffer.get("in_time").toString()) / 90) ), (long)CommonParsing.getVideoFramerate()) + ",");
											print_buffer.print( Teletext.SMPTE(timeformat_1.format( new Date( Long.parseLong( write_buffer.get("out_time").toString()) / 90) ), (long)CommonParsing.getVideoFramerate()) + ",");
											break;

										case 6:  // SUP
											long sup_in_time = Long.parseLong( write_buffer.get("in_time").toString() );

											if ( sup_in_time >= 0 )
											{
												for (int a = 1; a < 24; a++)
													if ( write_buffer.containsKey("" + a) )
														picture_String.add(write_buffer.get("" + a));

												while (picture_String.size() > subpicture.getMaximumLines()) // max. lines as defined
													picture_String.remove(0);

												subpicture.showPicTTX( picture_String.toArray(), job_processing.getStatusStrings());
												byte_buffer.write( subpicture.writeRLE( sup_in_time, 0xB4)); // alias duration 1.800 sec if out_time is missing

												if ( write_buffer.containsKey("out_time") )
													out.write(subpicture.setTime( byte_buffer.toByteArray(), Long.parseLong( write_buffer.get("out_time").toString())));
											}

											picture_String.clear();
										}

										int b = 0;

										for (int a = 1; subtitle_type != 6 && a < 24; a++)
										{
											if ( !write_buffer.containsKey("" + a) )
												continue;

											String str = write_buffer.get("" + a).toString();

											switch (subtitle_type)
											{
											case 1:  // free
											case 4:  // SRT
												print_buffer.println(str); 
												break;

											case 2:  // SC
												print_buffer.print(str); 
												break;

											case 3:  //	 SUB
											case 7:  // STL
												print_buffer.print( (b > 0 ? "|" : "") + str);
												break;

											case 5:  // SSA
												print_buffer.print( (b > 0 ? "\\n" : "") + str);
											}

											b++;
										}

										if (subtitle_type != 6 && b > 0)
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
								}

								if (rows && write) // if false, in_time has to be set/updated at synccheck above until an exported gop pts area
									write_buffer.put("in_time", "" + in_time);

								if (!rows)
									lastpage_match = false;

								else
									write_buffer.put("active", "1");

								Teletext.clearEnhancements();

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
								Teletext.setEnhancements(packet, row, character_set);

							continue readloop;
						}

						if (!loadpage)
							continue readloop;

						if (subtitle_type == 0)  // megaradio, simple decode the bytes of row 1..23
						{
							for (int b = (row == 1) ? 17: 0; row < 24 && b < 39; b++) // framebytes to MSB
								out.write(Teletext.bytereverse(packet[7+b]));

							continue readloop;
						}

						// decode row 1..23 , 0=header, 24 fasttext labels, 25 supressedheader, >26 non text packets 

						String str = null;
						int[] picture_data = null;

						switch (subtitle_type)
						{
						case 1:
							str = Teletext.makestring(packet, 6, 40, row, character_set, 0, true);
							break;

						case 2:
						case 7:
						case 3:
						case 4:
							str = Teletext.makestring(packet, 6, 40, row, character_set, 0, true).trim();
							break;

						case 5:
							str = Teletext.makestring(packet, 6, 40, row, character_set, 1, true).trim();
							break;

						case 6:
							picture_data = Teletext.makepic(packet, 6, 40, row, character_set, true);
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
							case 1:  // free
								print_buffer.print( "in_" + timeformat_1.format( new Date( Long.parseLong( write_buffer.get("in_time").toString()) / 90) ));
								print_buffer.println( "|out_" + timeformat_1.format( new Date( Long.parseLong( write_buffer.get("out_time").toString()) / 90) )); 
								break;

							case 2:  // SC
								print_buffer.print( Teletext.SMPTE(timeformat_1.format( new Date( Long.parseLong( write_buffer.get("in_time").toString()) / 90) ), (long)CommonParsing.getVideoFramerate()) + "&");
								print_buffer.print( Teletext.SMPTE(timeformat_1.format( new Date( Long.parseLong( write_buffer.get("out_time").toString()) / 90) ), (long)CommonParsing.getVideoFramerate()) + "#");
								break;

							case 3:  // SUB
								print_buffer.print( "{" + ( (long)(Long.parseLong( write_buffer.get("in_time").toString()) / CommonParsing.getVideoFramerate())) + "}");
								print_buffer.print( "{" + ( (long)(Long.parseLong( write_buffer.get("out_time").toString()) / CommonParsing.getVideoFramerate())) + "}");
								break;

							case 4:  // SRT
								print_buffer.println( "" + (seiten + 1));
								print_buffer.print( timeformat_2.format( new Date( Long.parseLong( write_buffer.get("in_time").toString()) / 90) ));
								print_buffer.println(" --> " + timeformat_2.format( new Date( Long.parseLong( write_buffer.get("out_time").toString()) / 90) ));
								break;

							case 5:  // SSA
								print_buffer.print( Teletext.getSSALine()[0] + timeformat_1.format( new Date( Long.parseLong( write_buffer.get("in_time").toString()) / 90) ).substring(1, 11) + ",");
								print_buffer.print( timeformat_1.format( new Date( Long.parseLong( write_buffer.get("out_time").toString()) / 90) ).substring(1, 11) + Teletext.getSSALine()[1]);
								break;

							case 7:  // STL
								print_buffer.print( Teletext.SMPTE(timeformat_1.format( new Date( Long.parseLong( write_buffer.get("in_time").toString()) / 90) ), (long)CommonParsing.getVideoFramerate()) + ",");
								print_buffer.print( Teletext.SMPTE(timeformat_1.format( new Date( Long.parseLong( write_buffer.get("out_time").toString()) / 90) ), (long)CommonParsing.getVideoFramerate()) + ",");
								break;

							case 6:  // SUP
								long sup_in_time = Long.parseLong( write_buffer.get("in_time").toString() );

								if ( sup_in_time >= 0 )
								{
									for (int a = 1; a < 24; a++)
										if ( write_buffer.containsKey("" + a) )
											picture_String.add(write_buffer.get("" + a));

									while (picture_String.size() > subpicture.getMaximumLines())
										picture_String.remove(0);

									subpicture.showPicTTX( picture_String.toArray(), job_processing.getStatusStrings());
									byte_buffer.write( subpicture.writeRLE( sup_in_time, 0xB4)); // alias duration 2.000 sec if out_time is missing

									out.write(subpicture.setTime( byte_buffer.toByteArray(), Long.parseLong( write_buffer.get("out_time").toString())));
								}

								picture_String.clear();
							}

							int b = 0;

							for (int a = 1; subtitle_type != 6 && a < 24; a++)
							{
								if ( !write_buffer.containsKey("" + a) )
									continue;

								String str = write_buffer.get("" + a).toString();

								switch (subtitle_type)
								{
								case 1:  // free
								case 4:  // SRT
									print_buffer.println(str); 
									break;

								case 2:  // SC
									print_buffer.print(str); 
									break;

								case 3:  // SUB
								case 7:  // STL
									print_buffer.print( (b > 0 ? "|" : "") + str);
									break;

								case 5:  // SSA
									print_buffer.print( (b > 0 ? "\\n" : "") + str);
								}

								b++;
							}

							if (subtitle_type != 6 && b > 0)
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

					if (subtitle_type > 0 && seiten==0) 
						ttxfile1.delete();

					else if (subtitle_type == 0)
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
						if (subtitle_type == 6)
							Ifo.createIfo(ttxfile, subpicture.getUserColorTableArray());

						job_processing.countMediaFilesExportLength(ttxfile1.length());
						job_processing.addSummaryInfo(Resource.getString("teletext.summary", "" + job_processing.countPictureStream(), "" + seiten, "" + page, infoPTSMatch(filename_pts, videofile_pts, vptsdata, ptsdata)) + "'" + ttxfile1 + "'");
					}

				} catch (IOException e2) { 

					Common.setExceptionMessage(e2);
				}

			}  // end for

			if (LB1 < 0) 
				break;

			else
			{ 
				subpicture.set2();
				Common.setMessage("");
				Common.setMessage(Resource.getString("teletext.msg.newrun"));
			}
		}  // end for

		Common.updateProgressBar(size, size);

		if (ShowSubpictureWindow)
			Common.getGuiInterface().hideSubpicture();

	}

}
