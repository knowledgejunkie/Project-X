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
import java.io.PrintStream;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Date;
import java.util.TimeZone;
import java.util.Enumeration;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import net.sourceforge.dvb.projectx.common.Common;
import net.sourceforge.dvb.projectx.common.Resource;
import net.sourceforge.dvb.projectx.common.Keys;
import net.sourceforge.dvb.projectx.common.JobCollection;
import net.sourceforge.dvb.projectx.common.JobProcessing;

import net.sourceforge.dvb.projectx.io.IDDBufferedOutputStream;

import net.sourceforge.dvb.projectx.xinput.XInputFile;

import net.sourceforge.dvb.projectx.parser.CommonParsing;
import net.sourceforge.dvb.projectx.parser.StreamConverter;
import net.sourceforge.dvb.projectx.parser.StreamDemultiplexer;
import net.sourceforge.dvb.projectx.parser.StreamProcessBase;

import net.sourceforge.dvb.projectx.subtitle.Subpicture;
import net.sourceforge.dvb.projectx.subtitle.BMP;
import net.sourceforge.dvb.projectx.subtitle.Bitmap;
import net.sourceforge.dvb.projectx.subtitle.Teletext;

import net.sourceforge.dvb.projectx.thirdparty.Ifo;


/**
 * main thread
 */
public class StreamProcessSubpicture extends StreamProcessBase {

	private final String subdecode_errors[] = {
		"",
		"", // -1 = correct decoded dvb-subpicture segments, can export
		"", // -2 = correct decoded dvb-subpicture segments, w/o export
		Resource.getString("subpicture.msg.error3"), // -3 = error while decoding dvb-subpicture
		Resource.getString("subpicture.msg.error4"),
		Resource.getString("subpicture.msg.error5"),
		Resource.getString("subpicture.msg.error6"),
		Resource.getString("subpicture.msg.error7"),
		Resource.getString("subpicture.msg.error8"),
		Resource.getString("subpicture.msg.error9")
	};

	/**
	 * 
	 */
	public StreamProcessSubpicture(JobCollection collection, XInputFile xInputFile, String filename_pts, String filename_type, String videofile_pts, int isElementaryStream)
	{
		super();

		processStream(collection, xInputFile, filename_pts, filename_type, videofile_pts, isElementaryStream);
	}

	/**
	 * decoding subpicture stream
	 */
	private void processStream(JobCollection collection, XInputFile xInputFile, String filename_pts, String filename_type, String videofile_pts, int isElementaryStream)
	{
		Subpicture subpicture = Common.getSubpictureClass();

		JobProcessing job_processing = collection.getJobProcessing();

		String fchild = isElementaryStream == CommonParsing.ES_TYPE ? collection.getOutputName(xInputFile.getName()) : xInputFile.getName();
		String fparent = collection.getOutputNameParent(fchild);

		fparent += isElementaryStream == CommonParsing.ES_TYPE ? ".new" : "";

		String subfile = fparent + ".sup";

		long size = xInputFile.length();

		byte[] parse12 = new byte[12];
		byte[] packet = new byte[0];

		long count = 0;
		long startPoint = 0;
		long time_difference = 0;
		long display_time = 0;
		long source_pts = 0;
		long new_pts = 0;
		long first_pts = -1;
		long last_pts = 0;

		int x = 0;
		int pics = 0;
		int v = 0;
		int packetlength = 0;
		int export_type = 0;
		int last_pgc_set = 0;

		boolean vptsdata = false;
		boolean ptsdata = false;
		boolean write = false;
		boolean missing_syncword = false;
		boolean DVBpicture = false;
		boolean debug = collection.DebugMode();
		boolean KeepOriginalTimecode = Common.getSettings().getBooleanProperty(Keys.KEY_SubtitlePanel_keepOriginalTimecode);
		boolean UseAdditionalOffset = Common.getSettings().getBooleanProperty(Keys.KEY_additionalOffset);
		boolean ShowSubpictureWindow = Common.getSettings().getBooleanProperty(Keys.KEY_showSubpictureWindow);
		boolean Message_2 = Common.getSettings().getBooleanProperty(Keys.KEY_MessagePanel_Msg2);

		int AdditionalOffset_Value = Common.getSettings().getIntProperty(Keys.KEY_ExportPanel_additionalOffset_Value);

		String SubpictureColorModel = Common.getSettings().getProperty(Keys.KEY_SubpictureColorModel);
		String PageId_Value = Common.getSettings().getProperty(Keys.KEY_SubtitlePanel_PageId_Value);
		String SubtitleExportFormat = Common.getSettings().getProperty(Keys.KEY_SubtitleExportFormat);

		try {
			if (ShowSubpictureWindow)
				Common.getGuiInterface().showSubpicture();

			Hashtable user_table = new Hashtable();

			ArrayList subpicture_colormodel = Common.getColorModelsList();

			if (subpicture_colormodel.indexOf(SubpictureColorModel) > 2)
				user_table = Common.getUserColourTable(SubpictureColorModel);

			subpicture.reset();

			subpicture.dvb.setIRD(2<<subpicture_colormodel.indexOf(SubpictureColorModel), user_table, debug, PageId_Value);

			Common.setMessage(Resource.getString("subpicture.msg.model", SubpictureColorModel) + " " + PageId_Value);

			if (SubtitleExportFormat.equalsIgnoreCase(Keys.ITEMS_SubtitleExportFormat[6].toString()))
			{
				subfile = fparent + ".son";
				export_type = 1;
			}

			Common.setMessage(Resource.getString("subpicture.msg.output") + " " + subfile.substring(subfile.length() - 3));

			PushbackInputStream in = new PushbackInputStream(xInputFile.getInputStream(), 65536);

			IDDBufferedOutputStream out = new IDDBufferedOutputStream(new FileOutputStream(subfile),65536);

			PrintStream print_out = new PrintStream(out);

			Common.setMessage(Resource.getString("subpicture.msg.tmpfile", xInputFile.getName(), "" + size));

			Common.updateProgressBar(Resource.getString("subpicture.progress") + " " + xInputFile.getName(), 0, 0);


			long[] ptsval = {0};
			long[] ptspos = {0};
			long[] vptsval = {0};

			long pts_offset = UseAdditionalOffset ? 90L * AdditionalOffset_Value : 0;

			//System.gc();

			long[][] obj = loadTempOtherPts(filename_pts, "subpicture.msg.discard", "audio.msg.pts.firstonly", "subpicture.msg.pts.start_end", "", CommonParsing.SUBPICTURE, false, debug);

			if (obj != null)
			{
				ptsval = obj[0];
				ptspos = obj[1];
				ptsdata = true;
				obj = null;
			}

			ptsdata = true;

			obj = loadTempVideoPts(videofile_pts, debug);

			if (obj != null)
			{
				vptsval = obj[0];
				vptsdata = true;
				obj = null;
			}

			//System.gc();


			if (vptsdata && ptsdata)
			{
				int jump = checkPTSMatch(vptsval, ptsval);

				if (jump < 0)
				{
					Common.setMessage(Resource.getString("subpicture.msg.pts.mismatch"));  

					vptsdata = false; 
					x = 0; 
				}

				else
					x = jump;

			}

			if (vptsdata && ptsdata)
			{
				Common.setMessage(Resource.getString("subpicture.msg.adjust.at.video"));

				time_difference = vptsval[0];
			}

			if (!vptsdata && ptsdata)
			{
				Common.setMessage(Resource.getString("subpicture.msg.adjust.at.own"));

				time_difference = 0;
			}

			if (ptsdata)
			{ 
				source_pts = ptsval[x]; 
				startPoint = ptspos[x]; 
			}

			//don't need it anymore
			ptsval = null;
			ptspos = null;

			while (count < startPoint)
				count += in.skip(startPoint-count);

			//yield();


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

				in.read(parse12, 0, 12);

				if (parse12[0] != 0x53 || parse12[1] != 0x50) // find "SP"
				{
					if (Message_2 && !missing_syncword)
						Common.setMessage(Resource.getString("subpicture.msg.syncword.lost") + " " + count);

					missing_syncword = true;
					count++;

					in.unread(parse12, 1, 11);

					continue readloop;
				}

				if (Message_2 && missing_syncword)
					Common.setMessage(Resource.getString("subpicture.msg.syncword.found") + " " + count);

				in.unread(parse12, 0, 12);

				missing_syncword = false;

				packetlength = ((0xFF & parse12[10])<<8 | (0xFF & parse12[11])) + 10;
				packet = new byte[packetlength];

				in.read(packet);
				count += packetlength;

				source_pts = 0;

				for (int a = 0; a < 5; a++) // 5bytes for pts, maybe wrong
					source_pts |= (0xFFL & packet[2+a])<<(a*8);

				//DM15072004 081.7 int06 add, use add. time offset if not applied by class-piddemux (e.g. ES)
				if (filename_pts.equals("-1"))
					source_pts += pts_offset;

				if (first_pts == -1)
					first_pts = source_pts;

				if (source_pts == pts_offset)
					source_pts = last_pts;

				else
					last_pts = source_pts;

				if (debug)
					System.out.println(" " + (count - packetlength) + "/ " + packetlength + "/ " + source_pts);

				if (ptsdata)
				{
					write = !vptsdata;

					rangeloop:
					while (vptsdata && v < vptsval.length)  //pic_start_pts must be in range ATM
					{ 
						if (source_pts < vptsval[v])
							break rangeloop;

						else if (source_pts == vptsval[v] || source_pts < vptsval[v + 1])
						{
							write = true;
							break rangeloop;
						}

						v += 2;

						if (v < vptsval.length)
							time_difference += (vptsval[v] - vptsval[v-1]);
					}
				}
				else
					write = true;

				if (!vptsdata && time_difference == 0 && !KeepOriginalTimecode)
					time_difference = source_pts;

				new_pts = source_pts - time_difference;

				if ((display_time = subpicture.decode_picture(packet, 10, Common.getGuiInterface().isSubpictureVisible(), job_processing.getStatusStrings(), new_pts, write, Common.getGuiInterface().isSubpictureVisible())) < -2)
					Common.setMessage(Resource.getString("subpicture.msg.error", subdecode_errors[Math.abs((int)display_time)], "" + (count - packetlength)));

				if (debug)
					System.out.println("PTS: source " + Common.formatTime_1(source_pts / 90) + "(" + source_pts + ")" + " /new " + Common.formatTime_1(new_pts / 90) + "(" + new_pts + ")" + " / write: " + write + " / dec.state: " + display_time);

				if (display_time < 0)  //dvb_subpic
				{
					if (!DVBpicture)
						Common.setMessage(Resource.getString("subpicture.msg.dvbsource"));

					DVBpicture = true;

					if (display_time == -1) // -1 full data, -2 forced end_time
					{
						String num = "00000" + pics;
						String outfile_base = fparent + "_st" + num.substring(num.length() - 5);

						String key, object_id_str, outfile;
						int object_id;

						for (Enumeration e = BMP.getKeys(); e.hasMoreElements() ; )
						{
							key = e.nextElement().toString();
							object_id = Integer.parseInt(key);
							object_id_str = Integer.toHexString(object_id).toUpperCase();
							outfile = outfile_base + "p" + object_id_str;

							Bitmap bitmap = BMP.getBitmap(object_id);

							if (export_type == 0)  //.sup
								out.write( subpicture.writeRLE(bitmap));

							else    //.son + .bmp
							{
								if (pics == 0)
								{
									String[] SONhead = Teletext.getSONHead(new File(subfile).getParent(), (long)CommonParsing.getVideoFramerate());

									for (int a=0; a < SONhead.length; a++) 
										print_out.println(SONhead[a]);
								}

								subpicture.updateUserColorTable(bitmap);
								outfile = BMP.buildBMP_palettized(outfile, bitmap, subpicture.getUserColorTable(), 256);

								job_processing.countMediaFilesExportLength(new File(outfile).length());

								int pgc_values = subpicture.setPGClinks();

								// a change in color_links
								if ((0xFFFF & pgc_values) != (0xFFFF & last_pgc_set))
								{
									String pgc_colors = "";

									for (int a = 0; a < 4; a++)
										pgc_colors += "" + (0xF & pgc_values>>>(a * 4)) + " ";

									print_out.println("Color\t\t(" + pgc_colors.trim() + ")");
								}

								// a change in alpha_links
								if ((0xFFFF0000 & pgc_values) != (0xFFFF0000 & last_pgc_set))
								{
									String pgc_alphas = "";

									for (int a = 0; a < 4; a++)
										pgc_alphas += "" + (0xF & pgc_values>>>((4 + a) * 4)) + " ";

									print_out.println("Contrast\t(" + pgc_alphas.trim() + ")");
								}

								last_pgc_set = pgc_values;

								print_out.println("Display_Area\t(" + Common.adaptString(bitmap.getX(), 3) + " " + Common.adaptString(bitmap.getY(), 3) + " " + Common.adaptString(bitmap.getMaxX(), 3) + " " + Common.adaptString(bitmap.getMaxY(), 3) + ")");
								print_out.println(outfile_base.substring(outfile_base.length() - 4) + "\t\t" + Common.formatTime_2(bitmap.getInTime() / 90, (long)CommonParsing.getVideoFramerate()) + "\t" + Common.formatTime_2((bitmap.getInTime() / 90) + (bitmap.getPlayTime() * 10), (long)CommonParsing.getVideoFramerate()) + "\t" + new File(outfile).getName());
							}

							//Common.setMessage(subpicture.getArea());
							//BMP.buildBMP_24bit(outfile, key);

							Common.getGuiInterface().setSubpictureTitle(" " + Resource.getString("subpicture.preview.title.dvbexport", "" + bitmap.getPageId(), "" + pics, Common.formatTime_1(new_pts / 90)) + " " + Common.formatTime_1(bitmap.getPlayTime() * 10));
						}

						if (!BMP.isEmpty())
							Common.getGuiInterface().showExportStatus(Resource.getString("subpicture.status"), ++pics);

						BMP.clear();
					}
				}

				else if (write) //dvd_subpic
				{
					for (int a = 0; a < 8; a++)
						packet[2 + a] = (byte)(0xFFL & new_pts>>>(a * 8));

					//later, to allow overlapping on cut boundaries 
					//if (display_time > 0)
					//	packet = subpicture.setTime(packet,display_time);

					out.write(packet);

					Common.getGuiInterface().showExportStatus(Resource.getString("subpicture.status"), ++pics);
					Common.getGuiInterface().setSubpictureTitle(" " + Resource.getString("subpicture.preview.title.dvdexport", "" + pics, Common.formatTime_1(new_pts / 90)) + " " + Common.formatTime_1(display_time / 90));

					String str = subpicture.isForced_Msg();

					if (str != null)
						Common.setMessage(str + " " + Resource.getString("subpicture.msg.forced") + " " + pics);
				}

				else
					Common.getGuiInterface().setSubpictureTitle(" " + Resource.getString("subpicture.preview.title.noexport"));

				if (debug)
					System.out.println(" -> " + write + "/ " + v + "/ " + new_pts + "/ " + time_difference + "/ " + pics + "/ " + display_time);
			}

			in.close();

			print_out.flush();
			print_out.close();

			out.flush(); 
			out.close();

			if (filename_pts.equals("-1"))
				Common.setMessage(Resource.getString("subpicture.msg.pts.start_end", Common.formatTime_1(first_pts / 90)) + " " + Common.formatTime_1(source_pts / 90));

			Common.setMessage(Resource.getString("subpicture.msg.summary", "" + pics));

			if (!DVBpicture && export_type == 1)
			{
				String renamed_file = subfile.substring(0, subfile.length() - 3) + "sup";
				Common.renameTo(subfile, renamed_file);
				subfile = renamed_file;
			}

			File subfile1 = new File(subfile); 

			if (pics == 0) 
				subfile1.delete();

			else
			{ 
				if (DVBpicture && export_type == 0)
					job_processing.countMediaFilesExportLength(Ifo.createIfo(subfile, subpicture.getUserColorTableArray()));

				else if (DVBpicture && export_type == 1)
					job_processing.countMediaFilesExportLength(new File( BMP.write_ColorTable(fparent, subpicture.getUserColorTable(), 256)).length());

				Common.setMessage(Resource.getString("msg.newfile") + " " + subfile);
				job_processing.countMediaFilesExportLength(subfile1.length());
				job_processing.addSummaryInfo(Resource.getString("subpicture.summary", "" + job_processing.countPictureStream(), "" + pics, infoPTSMatch(filename_pts, videofile_pts, vptsdata, ptsdata)) + "'" + subfile1 + "'");
			}

			Common.updateProgressBar(size, size);

		} catch (IOException e2) { 

			Common.setExceptionMessage(e2);
		}

		if (ShowSubpictureWindow)
			Common.getGuiInterface().hideSubpicture();
	}


}
