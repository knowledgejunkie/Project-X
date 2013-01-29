/*
 * @(#)StreamParser
 *
 * Copyright (c) 2005-2013 by dvb.matt, All rights reserved.
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
 * multicolor subtitling patch by Duncan (Shannock9) UK
 * 2008-12
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
import java.util.StringTokenizer;

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
import net.sourceforge.dvb.projectx.subtitle.Sup2VobSub;
import net.sourceforge.dvb.projectx.subtitle.ColorAreas;                                         //S9

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

	private String Extension = ".new";

	private boolean debug;
	private boolean KeepOriginalTimecode;
	private boolean ExportAsVobSub;
	private boolean UseAdditionalOffset;
	private boolean ShowSubpictureWindow;
	private boolean Message_2;

	private int AdditionalOffset_Value;

	private int X_Offset = 0;
	private int Y_Offset = 0;
	private int DisplayMode = 0;
	private int ExportType = 0;
	private int Pictures = 0;
	private int LastPGCSet = 0;

	private String SubpictureColorModel;
	private String PageId_Value;
	private String SubtitleExportFormat;
	private String FileParent;

	private ArrayList bdn_events = new ArrayList();

	/**
	 * 
	 */
	public StreamProcessSubpicture(String extension)
	{
		super();

		Extension = extension;
	}

	/**
	 * 
	 */
	public StreamProcessSubpicture(JobCollection collection, XInputFile xInputFile, String filename_pts, String filename_type, String videofile_pts, int isElementaryStream)
	{
		super();

		get_XY_Offset(collection, isElementaryStream);
		getDisplayMode(collection, isElementaryStream);

		String SubtitleExportFormat = collection.getSettings().getProperty(Keys.KEY_SubtitleExportFormat);

		processStream(collection, xInputFile, filename_pts, filename_type, videofile_pts, isElementaryStream, SubtitleExportFormat);

		// 2nd export format, new run
		SubtitleExportFormat = collection.getSettings().getProperty(Keys.KEY_SubtitleExportFormat_2);

		if (!SubtitleExportFormat.equalsIgnoreCase("null"))
			processStream(collection, xInputFile, filename_pts, filename_type, videofile_pts, isElementaryStream, SubtitleExportFormat);
	}

	/**
	 * set new X with or without offset
	 */
	public void set_XY_Offset(int x_value, int y_value)
	{
		X_Offset = x_value;
		Y_Offset = y_value;
	}

	/**
	 * set new X with or without offset
	 */
	private void get_XY_Offset(JobCollection collection, int isElementaryStream)
	{
		//if (isElementaryStream != CommonParsing.ES_TYPE)
		//	return;

		StringTokenizer st = new StringTokenizer(collection.getSettings().getProperty(Keys.KEY_SubtitleMovePosition_Value), ",");
		int a = 0;
		int[] values = new int[2];

		while (st.hasMoreTokens() && a < values.length)
		{
			try {
				values[a] = Integer.parseInt(st.nextToken());

			} catch (Exception e) {}

			a++;
		}

		X_Offset = values[0];
		Y_Offset = values[1];
	}

	/**
	 * set new display to be forced or not
	 */
	private void getDisplayMode(JobCollection collection, int isElementaryStream)
	{
		//if (isElementaryStream != CommonParsing.ES_TYPE)
		//	return;

		DisplayMode = collection.getSettings().getIntProperty(Keys.KEY_SubtitleChangeDisplay);
	}

	/**
	 * decoding subpicture stream, forces SUP, simple method for modifying a SUP stream from teletext process
	 */
	public void processStream(JobCollection collection, XInputFile xInputFile, int isElementaryStream)
	{
		processStream(collection, xInputFile, "-1", "sp", "-1", isElementaryStream, Keys.ITEMS_SubtitleExportFormat[7].toString());
	}

	/**
	 * decoding subpicture stream
	 */
	private void processStream(JobCollection collection, XInputFile xInputFile, String filename_pts, String filename_type, String videofile_pts, int isElementaryStream, String SubtitleExportFormat)
	{
		Subpicture subpicture = Common.getSubpictureClass();

		JobProcessing job_processing = collection.getJobProcessing();

		String fchild = isElementaryStream == CommonParsing.ES_TYPE ? collection.getOutputName(xInputFile.getName()) : xInputFile.getName();
		FileParent = collection.getOutputNameParent(fchild);

		FileParent += isElementaryStream == CommonParsing.ES_TYPE ? Extension : "";

		String subfile = FileParent + ".sup";

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
		int v = 0;
		int packetlength = 0;
		ExportType = 0;
		Pictures = 0;
		LastPGCSet = 0;

		boolean vptsdata = false;
		boolean ptsdata = false;
		boolean write = false;
		boolean missing_syncword = false;
		boolean DVBpicture = false;

		debug = collection.getSettings().getBooleanProperty(Keys.KEY_DebugLog);
		KeepOriginalTimecode = isElementaryStream == CommonParsing.ES_TYPE ? true : collection.getSettings().getBooleanProperty(Keys.KEY_SubtitlePanel_keepOriginalTimecode);
		ExportAsVobSub = collection.getSettings().getBooleanProperty(Keys.KEY_SubtitlePanel_exportAsVobSub);
		UseAdditionalOffset = collection.getSettings().getBooleanProperty(Keys.KEY_additionalOffset);
		ShowSubpictureWindow = collection.getSettings().getBooleanProperty(Keys.KEY_showSubpictureWindow);
		Message_2 = collection.getSettings().getBooleanProperty(Keys.KEY_MessagePanel_Msg2);

		AdditionalOffset_Value = collection.getSettings().getIntProperty(Keys.KEY_ExportPanel_additionalOffset_Value);

		SubpictureColorModel = collection.getSettings().getProperty(Keys.KEY_SubpictureColorModel);
		PageId_Value = collection.getSettings().getProperty(Keys.KEY_SubtitlePanel_PageId_Value);
	//	SubtitleExportFormat = collection.getSettings().getProperty(Keys.KEY_SubtitleExportFormat);

		try {
			if (ShowSubpictureWindow)
				Common.getGuiInterface().showSubpicture();

			Hashtable user_table = new Hashtable();

			ArrayList subpicture_colormodel = Common.getColorModelsList();

			if (subpicture_colormodel.indexOf(SubpictureColorModel) > 2) // 0,1,2 internal
				user_table = Common.getUserColourTable(SubpictureColorModel);

			subpicture.reset();

			subpicture.dvb.setIRD(2<<subpicture_colormodel.indexOf(SubpictureColorModel), user_table, debug, PageId_Value);

			Common.setMessage(Resource.getString("subpicture.msg.model", SubpictureColorModel) + " " + PageId_Value);

			if (SubtitleExportFormat.equalsIgnoreCase(Keys.ITEMS_SubtitleExportFormat[6].toString()))
			{
				subfile = FileParent + ".son";
				ExportType = 1;
			}
			else if (SubtitleExportFormat.equalsIgnoreCase(Keys.ITEMS_SubtitleExportFormat[11].toString()))
			{
				subfile = FileParent + ".bdn";
				ExportType = 2;
			}

		//	Common.setMessage("");
			Common.setMessage(Resource.getString("subpicture.msg.output") + " " + subfile.substring(subfile.length() - 3));

			PushbackInputStream in = new PushbackInputStream(xInputFile.getInputStream(), 65536);

			IDDBufferedOutputStream out = new IDDBufferedOutputStream(new FileOutputStream(subfile),65536);

			PrintStream print_out = new PrintStream(out);

			Common.setMessage(Resource.getString("subpicture.msg.tmpfile", xInputFile.getName(), "" + size));

			// SUP with changed settings
			if (ExportType == 0)
			{
				subpicture.set_XY_Offset(X_Offset, Y_Offset);
				subpicture.setDisplayMode(DisplayMode);
			}

			if (X_Offset != 0 || Y_Offset != 0)
				Common.setMessage("-> move source picture position: X " + X_Offset + ", Y " + Y_Offset);

			if (DisplayMode != 0)
				Common.setMessage("-> set new picture display mode: " + Keys.ITEMS_SubtitleChangeDisplay[DisplayMode]);

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

			//	if (source_pts == pts_offset) // first pts would be 0, ever
				if (source_pts == pts_offset && first_pts != -1)
					source_pts = last_pts;

				else
					last_pts = source_pts;

				//remember pts of 1st packet
				if (first_pts == -1)
					first_pts = source_pts;


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
					Common.setMessage(Resource.getString("subpicture.msg.error", subdecode_errors[Math.abs((int)display_time)], String.valueOf(count - packetlength)));

				if (debug)
					System.out.println("PTS: source " + Common.formatTime_1(source_pts / 90) + "(" + source_pts + ")" + " /new " + Common.formatTime_1(new_pts / 90) + "(" + new_pts + ")" + " / write: " + write + " / dec.state: " + display_time);

				if (display_time < 0)  //dvb_subpic
				{
					if (!DVBpicture)
						Common.setMessage(Resource.getString("subpicture.msg.dvbsource"));

					DVBpicture = true;

					if (display_time == -1) // -1 full data, -2 forced end_time
						process_dvbsubpicture(job_processing, print_out, out, subpicture, subfile, new_pts);
				}

				else if (write) //dvd_subpic
				{
					for (int a = 0; a < 8; a++)
						packet[2 + a] = (byte)(0xFFL & new_pts>>>(a * 8));

					//later, to allow overlapping on cut boundaries 
					//if (display_time > 0)
					//	packet = subpicture.setTime(packet,display_time);

					out.write(packet);

					Common.getGuiInterface().showExportStatus(Resource.getString("subpicture.status"), ++Pictures);
					Common.getGuiInterface().setSubpictureTitle(" " + Resource.getString("subpicture.preview.title.dvdexport", "" + Pictures, Common.formatTime_1(new_pts / 90)) + " " + Common.formatTime_1(display_time / 90));

					String str = subpicture.isForced_Msg();

					if (str != null)
						Common.setMessage(str + " " + Resource.getString("subpicture.msg.forced") + " " + Pictures);
				}

				else
					Common.getGuiInterface().setSubpictureTitle(" " + Resource.getString("subpicture.preview.title.noexport"));

				if (debug)
				{
					System.out.println("-> wr " + write + " /v " + v + " /npts " + new_pts + " /tdif " + time_difference + " /pic " + Pictures + " /dtim " + display_time);
					System.out.println("");
				}
			}

			// check whether there is still a pic waiting from dvb subpic , assume max 10sec display time
			if (display_time == -2)
			{
			//	if ((display_time = subpicture.decode_picture(packet, 10, Common.getGuiInterface().isSubpictureVisible(), job_processing.getStatusStrings(), new_pts + 1024000, write, Common.getGuiInterface().isSubpictureVisible())) < -2)
				if ((display_time = subpicture.decode_picture(packet, 10, Common.getGuiInterface().isSubpictureVisible(), job_processing.getStatusStrings(), -1, write, Common.getGuiInterface().isSubpictureVisible())) < -2)
					Common.setMessage(Resource.getString("subpicture.msg.error", subdecode_errors[Math.abs((int)display_time)], String.valueOf(count - packetlength)));

				if (debug)
					System.out.println("last picture in memory PTS: source " + Common.formatTime_1(source_pts / 90) + "(" + source_pts + ")" + " /new " + Common.formatTime_1((new_pts) / 90) + "(" + new_pts + ")" + " / write: " + write + " / dec.state: " + display_time);

				if (display_time == -1)
					process_dvbsubpicture(job_processing, print_out, out, subpicture, subfile, new_pts);
			}

			in.close();

			//completion of BDN
			if (ExportType == 2)
			{
				String firstInTC = bdn_events.get(0).toString();
				firstInTC = firstInTC.substring(firstInTC.indexOf("InTC=") + 6, firstInTC.indexOf("InTC=") + 17);
				String lastOutTC = bdn_events.get(bdn_events.size() - 3).toString();
				lastOutTC = lastOutTC.substring(lastOutTC.indexOf("OutTC=") + 7, lastOutTC.indexOf("OutTC=") + 18);

				String[] BDNhead = Common.getTeletextClass().getBDNHead(new File(subfile).getName(), (long)CommonParsing.getVideoFramerate(), job_processing.getStatusStrings(), firstInTC, lastOutTC, bdn_events.size() / 3);

				for (int i = 0; i < 9; i++) 
					print_out.println(BDNhead[i]);

				for (int i = 0; i < bdn_events.size(); i++) 
					print_out.println(bdn_events.get(i).toString());

				for (int i = 9; i < BDNhead.length; i++) 
					print_out.println(BDNhead[i]);

				bdn_events.clear();
			}

			print_out.flush();
			print_out.close();

			out.flush(); 
			out.close();

			if (filename_pts.equals("-1"))
				Common.setMessage(Resource.getString("subpicture.msg.pts.start_end", Common.formatTime_1(first_pts / 90)) + " " + Common.formatTime_1(source_pts / 90));

			Common.setMessage(Resource.getString("subpicture.msg.summary", "" + Pictures));

			if (!DVBpicture && ExportType == 1)
			{
				String renamed_file = subfile.substring(0, subfile.length() - 3) + "sup";
				Common.renameTo(subfile, renamed_file);
				subfile = renamed_file;
			}

			File subfile1 = new File(subfile); 

			if (Pictures == 0) 
				subfile1.delete();

			else
			{ 
				if (DVBpicture && ExportType == 0)
				{
				  if (ColorAreas.active)  			       //multicolor DVB to SUP active                                       //S9
					job_processing.countMediaFilesExportLength(Ifo.createIfo(subfile, ColorAreas.clut_pgc));                    //S9
				  else
					job_processing.countMediaFilesExportLength(Ifo.createIfo(subfile, subpicture.getUserColorTableArray()));    //S9
				}
				else if (DVBpicture && ExportType == 1)
					job_processing.countMediaFilesExportLength(new File( BMP.write_ColorTable(FileParent, subpicture.getUserColorTable(), 256)).length());

				Common.setMessage(Resource.getString("msg.newfile") + " " + subfile);
				job_processing.countMediaFilesExportLength(subfile1.length());
				job_processing.addSummaryInfo(Resource.getString("subpicture.summary", Common.adaptString(job_processing.countPictureStream(), 2), "" + Pictures, infoPTSMatch(filename_pts, videofile_pts, vptsdata, ptsdata)) + "'" + subfile1 + "'");

				//vobsub
				if (ExportType == 0 && ExportAsVobSub)
					new Sup2VobSub(subfile, subpicture.getUserColorTableArray());
			}

			Common.updateProgressBar(size, size);

		} catch (IOException e2) { 

			Common.setExceptionMessage(e2);
		}

		if (ShowSubpictureWindow)
			Common.getGuiInterface().hideSubpicture();

		ColorAreas.active = false;                                     //S9
	}


	private void process_dvbsubpicture(JobProcessing job_processing, PrintStream print_out, IDDBufferedOutputStream out, Subpicture subpicture, String subfile, long new_pts)
	{
		try {
			String num = "00000" + Pictures;
			String outfile_base = FileParent + "_st" + num.substring(num.length() - 5);

			String key, object_id_str, outfile;
			int object_id;

			//read out loop
			for (Enumeration e = BMP.getKeys(); e.hasMoreElements() ; )
			{
				key = e.nextElement().toString();

				object_id = Integer.parseInt(key);
				object_id_str = Integer.toHexString(object_id).toUpperCase();
				outfile = outfile_base + "p" + object_id_str;

				Bitmap bitmap = BMP.getBitmap(object_id);

				if (ExportType == 0)  //.sup
					out.write( subpicture.writeRLE(bitmap));

				else if (ExportType == 2)   //.bdn
				{
					if (Pictures == 0)
						bdn_events.clear();

					subpicture.updateUserColorTable(bitmap);
					outfile = BMP.buildBMP_palettized_ARGB(outfile, bitmap, subpicture.getUserColorTable(), 256, true);

					job_processing.countMediaFilesExportLength(new File(outfile).length());

					//all pgc must be in BMP

					bdn_events.add("    <Event InTC=\"" + Common.formatTime_2(bitmap.getInTime() / 90, (long)CommonParsing.getVideoFramerate()) + "\" OutTC=\"" + Common.formatTime_2((bitmap.getInTime() / 90) + (bitmap.getPlayTime() * 10), (long)CommonParsing.getVideoFramerate()) + "\" Forced=\"False\">");
					bdn_events.add("      <Graphic Width=\"" + bitmap.getWidth() + "\" Height=\"" + bitmap.getHeight() + "\" X=\"" + bitmap.getX() + "\" Y=\"" + bitmap.getY() + "\">" + (new File(outfile).getName()) + "</Graphic>");
					bdn_events.add("    </Event>");

					if (debug)
						System.out.println("-> " + outfile);
				}

				else    //.son + .bmp
				{
					if (Pictures == 0)
					{
						String[] SONhead = Common.getTeletextClass().getSONHead(new File(subfile).getParent(), (long)CommonParsing.getVideoFramerate());

						for (int a=0; a < SONhead.length; a++) 
							print_out.println(SONhead[a]);
					}

					subpicture.updateUserColorTable(bitmap);
					outfile = BMP.buildBMP_palettized(outfile, bitmap, subpicture.getUserColorTable(), 256);

					job_processing.countMediaFilesExportLength(new File(outfile).length());

					int pgc_values = subpicture.setPGClinks();

					// a change in color_links
					if ((0xFFFF & pgc_values) != (0xFFFF & LastPGCSet))
					{
						String pgc_colors = "";

						for (int a = 0; a < 4; a++)
							pgc_colors += "" + (0xF & pgc_values>>>(a * 4)) + " ";

						print_out.println("Color\t\t(" + pgc_colors.trim() + ")");
					}

					// a change in alpha_links
					if ((0xFFFF0000 & pgc_values) != (0xFFFF0000 & LastPGCSet))
					{
						String pgc_alphas = "";

						for (int a = 0; a < 4; a++)
							pgc_alphas += "" + (0xF & pgc_values>>>((4 + a) * 4)) + " ";

						print_out.println("Contrast\t(" + pgc_alphas.trim() + ")");
					}

					LastPGCSet = pgc_values;

					print_out.println("Display_Area\t(" + Common.adaptString(bitmap.getX(), 3) + " " + Common.adaptString(bitmap.getY(), 3) + " " + Common.adaptString(bitmap.getMaxX(), 3) + " " + Common.adaptString(bitmap.getMaxY(), 3) + ")");
					print_out.println(outfile_base.substring(outfile_base.length() - 4) + "\t\t" + Common.formatTime_2(bitmap.getInTime() / 90, (long)CommonParsing.getVideoFramerate()) + "\t" + Common.formatTime_2((bitmap.getInTime() / 90) + (bitmap.getPlayTime() * 10), (long)CommonParsing.getVideoFramerate()) + "\t" + new File(outfile).getName());

					if (debug)
						System.out.println("-> " + outfile);
				}

				Common.getGuiInterface().setSubpictureTitle(" " + Resource.getString("subpicture.preview.title.dvbexport", "" + bitmap.getPageId(), "" + Pictures, Common.formatTime_1(new_pts / 90)) + " " + Common.formatTime_1(bitmap.getPlayTime() * 10));
			}

			if (!BMP.isEmpty())
				Common.getGuiInterface().showExportStatus(Resource.getString("subpicture.status"), ++Pictures);

			BMP.clear();

		} catch (IOException e) { 

			Common.setExceptionMessage(e);
		}
	}
}
