/*
 * @(#)Gop
 *
 * Copyright (c) 2005-2009 by dvb.matt, All Rights Reserved.
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

import java.io.IOException;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.Calendar;
import java.util.TimeZone;

import net.sourceforge.dvb.projectx.common.Resource;
import net.sourceforge.dvb.projectx.common.Keys;
import net.sourceforge.dvb.projectx.common.Common;
import net.sourceforge.dvb.projectx.common.JobProcessing;
import net.sourceforge.dvb.projectx.common.JobCollection;

import net.sourceforge.dvb.projectx.parser.CommonParsing;
import net.sourceforge.dvb.projectx.video.Video;

import net.sourceforge.dvb.projectx.io.IDDBufferedOutputStream;

import net.sourceforge.dvb.projectx.thirdparty.D2V;

/**
 * placeholder for future class
 */
public class Gop extends Object {

	private byte[] headerrescue = new byte[1];
	private long VbvBuffer_Value = 0;

	private boolean Debug;
	private boolean CreateD2vIndex;
	private boolean SplitProjectFile;
	private boolean AddSequenceHeader;
	private boolean Message_3;
	private boolean AddSequenceDisplayExension;
	private boolean PatchToProgressive;
	private boolean PatchToInterlaced;
	private boolean ToggleFieldorder;
	private boolean ClearCDF;
	private boolean Save1stFrameOfGop;
	private boolean Preview_AllGops;
	private boolean Preview_fastDecode;
	private boolean TrimPts;
	private boolean IgnoreErrors;
	private boolean OptionDAR;
	private boolean OptionHorizontalResolution;
	private boolean WriteVideo;
	private boolean InsertEndcode;
	private boolean DumpDroppedGop;

	private boolean UseGOPEditor;

	private int ChangeBitrateInAllSequences;
	private int ChangeVbvDelay;
	private int CutMode;
	private int ExportDAR;
	private int ChangeAspectRatio;
	private int ChangeVbvBuffer;
	private int Preview_YGain;

	private String ExportHorizontalResolution;
	private String SDE_Value;

	private JobProcessing job_processing;

	public Gop(JobCollection collection)
	{
		getSettings(collection);
	}

	private void getSettings(JobCollection collection)
	{
		Debug = collection.getSettings().getBooleanProperty(Keys.KEY_DebugLog);
		CreateD2vIndex = collection.getSettings().getBooleanProperty(Keys.KEY_ExternPanel_createD2vIndex);
		SplitProjectFile = collection.getSettings().getBooleanProperty(Keys.KEY_ExternPanel_splitProjectFile);
		AddSequenceHeader = collection.getSettings().getBooleanProperty(Keys.KEY_VideoPanel_addSequenceHeader);
		Message_3 = collection.getSettings().getBooleanProperty(Keys.KEY_MessagePanel_Msg3);
		AddSequenceDisplayExension = collection.getSettings().getBooleanProperty(Keys.KEY_VideoPanel_addSde);
		PatchToProgressive = collection.getSettings().getBooleanProperty(Keys.KEY_VideoPanel_patchToProgressive); 
		PatchToInterlaced = collection.getSettings().getBooleanProperty(Keys.KEY_VideoPanel_patchToInterlaced); 
		ToggleFieldorder = collection.getSettings().getBooleanProperty(Keys.KEY_VideoPanel_toggleFieldorder);
		ClearCDF = collection.getSettings().getBooleanProperty(Keys.KEY_VideoPanel_clearCDF);
		Save1stFrameOfGop = collection.getSettings().getBooleanProperty(Keys.KEY_ExternPanel_save1stFrameOfGop);
		Preview_AllGops = collection.getSettings().getBooleanProperty(Keys.KEY_Preview_AllGops);
		Preview_fastDecode = collection.getSettings().getBooleanProperty(Keys.KEY_Preview_fastDecode);
		TrimPts = collection.getSettings().getBooleanProperty(Keys.KEY_Video_trimPts);
		IgnoreErrors = collection.getSettings().getBooleanProperty(Keys.KEY_Video_ignoreErrors);
		OptionDAR = collection.getSettings().getBooleanProperty(Keys.KEY_OptionDAR);
		OptionHorizontalResolution = collection.getSettings().getBooleanProperty(Keys.KEY_OptionHorizontalResolution);
		WriteVideo = collection.getSettings().getBooleanProperty(Keys.KEY_WriteOptions_writeVideo);
		InsertEndcode = collection.getSettings().getBooleanProperty(Keys.KEY_VideoPanel_insertEndcode);
		DumpDroppedGop = collection.getSettings().getBooleanProperty(Keys.KEY_dumpDroppedGop);

		ChangeBitrateInAllSequences = collection.getSettings().getIntProperty(Keys.KEY_ChangeBitrateInAllSequences);
		ChangeVbvDelay = collection.getSettings().getIntProperty(Keys.KEY_ChangeVbvDelay);
		CutMode = collection.getSettings().getIntProperty(Keys.KEY_CutMode);
		ExportDAR = collection.getSettings().getIntProperty(Keys.KEY_ExportDAR);
		ChangeAspectRatio = collection.getSettings().getIntProperty(Keys.KEY_ChangeAspectRatio);
		ChangeVbvBuffer = collection.getSettings().getIntProperty(Keys.KEY_ChangeVbvBuffer);
		Preview_YGain = collection.getSettings().getIntProperty(Keys.KEY_Preview_YGain);

		ExportHorizontalResolution = collection.getSettings().getProperty(Keys.KEY_ExportHorizontalResolution);
		SDE_Value = collection.getSettings().getProperty(Keys.KEY_VideoPanel_SdeValue);

		UseGOPEditor = collection.getSettings().getBooleanProperty(Keys.KEY_useGOPEditor);
	}


	/**
	 * gop dropp
	 */
	private void messageDropError(byte[] gop, byte[] pts, int maxtref, int frame, int gop_number, int frame_number, long startpts, long lastpts, String dumpname, int errorcode)
	{
		Common.setMessage(Resource.getString("video.msg.error.gop.drop", String.valueOf(gop_number - 1), Common.formatTime_1(startpts / 90L), String.valueOf(startpts)) + ", errorcode: " + Integer.toHexString(errorcode).toUpperCase());
		Common.setMessage(Resource.getString("video.msg.error.gop.diff", String.valueOf(maxtref + 1) + "/" + (frame + 1), String.valueOf((lastpts - startpts) / 90)) + " " + Common.formatTime_1((long)(frame_number * (double)(CommonParsing.getVideoFramerate() / 90.0f) )));

		/**
		 * dump dropped gop to file
		 */
		if (DumpDroppedGop)
		{
			String gopdumpname = dumpname + "-GOP#" + (gop_number - 1) + ".bin";

			byte[] dumpfill = new byte[16];
			Arrays.fill(dumpfill, (byte)0xFF);

			try {
				DataOutputStream dump = new DataOutputStream(new FileOutputStream(gopdumpname));

				dump.writeInt(gop_number - 1); 
				dump.writeInt(maxtref); 
				dump.writeInt(frame);
				dump.write(dumpfill, 0, 4); 
				dump.write(pts); 
				dump.write(dumpfill); 
				dump.write(gop); 
				dump.write(Video.getSequenceEndCode());

				dump.flush(); 
				dump.close();

				Common.setMessage(Resource.getString("video.msg.error.gop.dump") + " " + dumpname);

			} catch (IOException e) {

				Common.setExceptionMessage(e);
			}
		}
	}

	/**
	 * gop changing/testing
	 */
	public void goptest(JobProcessing job_processing, IDDBufferedOutputStream video_sequence, byte[] gop, byte[] pts, DataOutputStream log, String dumpname, int[] MPGVideotype, List CutpointList, List ChapterpointList)
	{
		goptest(job_processing, video_sequence, gop, pts, log, dumpname, MPGVideotype, CutpointList, ChapterpointList, true);
	}

	/**
	 * gop changing/testing
	 */
	public void goptest(JobProcessing job_processing, IDDBufferedOutputStream video_sequence, byte[] gop, byte[] pts, DataOutputStream log, String dumpname, int[] MPGVideotype, List CutpointList, List ChapterpointList, boolean doWrite)
	{
		int[] clv = job_processing.getStatusVariables();
		int ErrorCode = 0;

		String[] VBASIC = job_processing.getStatusStrings();

		Common.setFps(job_processing.getSourceVideoFrameNumber());

		if (gop.length < 12)
		{
			Common.setMessage(Resource.getString("video.msg.error.lackofdata", String.valueOf(clv[6])));
			return;
		}

		if (pts.length == 0)
		{
			double npts = 0;
			double thisTC = 0;
			double diff = 0;
			double ref = 0;

			int p = 0;

			for (int i = 0, returncode, pes_ID; i < gop.length - 7; )
			{
				if ((returncode = CommonParsing.validateStartcode(gop, i)) < 0)
				{
					i += (-returncode);
					continue;
				}

				pes_ID = CommonParsing.getPES_IdField(gop, i);

				if (pes_ID == CommonParsing.GROUP_START_CODE)
				{
					/** options[8] ist ende PTS vom letzten GOP = beginn dieses Gop
					wie bei ttx die diff merken zw. PTS und TC zu beginn, 
					dann vergleichen und startpts neu setzen,
					NUR wenn TC genau past zur erwarteten PTS, dann nehm wa den (TC 0 was dann)
					**/

					thisTC = 90.0 
						* (3600000.0 * ((0x7C & gop[i + 4])>>>2)
						+ 60000.0 * ((3 & gop[i + 4])<<4 | (0xF0 & gop[i + 5])>>>4)
						+ 1000.0 * ((7 & gop[i + 5])<<3 | (0xE0 & gop[i + 6])>>>5)
						+ (((0x1F & gop[i + 6])<<1 | (0x80 & gop[i + 7])>>>7)
						* (CommonParsing.getVideoFramerate() / 90.0f)) );

					i += 7;
				}

				else if (pes_ID == CommonParsing.PICTURE_START_CODE)
				{
					p = i;
					ref = CommonParsing.getVideoFramerate() * ((0xFF & gop[i + 4])<<2 | (0xC0 & gop[i + 5])>>>6);
					npts = ref + job_processing.getEndPtsOfGop();

					break;
				}

				else
					i += 4;
			}

			diff = thisTC - job_processing.getLastGopTimecode();

			// TC diff >=0ms <5min
			if (diff >= 0 && diff < 27000000)
			{  
				npts = job_processing.getLastGopPts() + diff + ref;
				Common.setMessage(Resource.getString("video.msg.error.nopts.use_goptc", "" + clv[6]));
			}

			else
				Common.setMessage(Resource.getString("video.msg.error.nopts.use_lastpts", "" + clv[6]));

			pts = new byte[16];

			for (int i = 0; i < 8; i++)
			{
				pts[7 - i] = (byte)(0xFF & (long)npts>>>(i * 8));
				pts[15 - i] = (byte)(0xFF & (long)p>>>(i * 8));
			}

			job_processing.setLastGopTimecode((long)thisTC);
		}

		/* vpts[0] = pts, vpts[1] = for next frame following this byteposition in sequ_array */
		long[][] vpts = new long[2][pts.length/16];

		for (int i = 0; i < (pts.length / 16); i++)
		{
			for (int j = 0; j < 8; j++)
			{
				vpts[0][i] |= (0xFFL & pts[(i * 16) + j])<<((7 - j) * 8);
				vpts[1][i] |= (0xFFL & pts[(i * 16) + 8 + j])<<((7 - j) * 8);
			}
		}

		//horrible nebula PTS stuff
		if (vpts[0].length > 1 && Math.abs(vpts[0][vpts[0].length-1] - vpts[0][0]) < 100)
		{
			long a1 = vpts[0][0];
			long a2 = vpts[1][0];

			vpts = new long[2][1];
			vpts[0][0] = a1;
			vpts[1][0] = a2;

			clv[8]++;
		}

		if (Debug)
		{
			System.out.println("\ngop" + clv[6] + "/tc_o53 " + job_processing.getLastGopTimecode() + "/lp_o54 " + job_processing.getLastGopPts() + "/lp_o8 " + job_processing.getEndPtsOfGop() + "/lp_o40 " + job_processing.getLastSimplifiedPts());

			for (int i = 0; i < vpts[0].length; i++)
				System.out.println("p" + i + " " + vpts[0][i] + "/ " + vpts[1][i]);
		}

		Calendar cal = Calendar.getInstance();

		String ct = Common.formatTime_1((long)(job_processing.getExportedVideoFrameNumber() * (double)(CommonParsing.getVideoFramerate() / 90.0f)));
		String nv = "";
		String[] aspratio = {"res.","1.000 (1:1)","0.6735 (4:3)","0.7031 (16:9)","0.7615 (2.21:1)","0.8055","0.8437","0.9375","0.9815","1.0255","1.0695","1.1250","1.1575","1.2015","res." };
		String[] fps_tabl1 = {"forbidden fps","23.976fps","24fps","25fps","29.97fps","30fps","50fps","59.94fps","60fps","n.def.","n.def.","n.def.","n.def.","n.def.","n.def.","n.def."};

		double[] fps_tabl2 = { 0, 3753.7537, 3750, 3600, 3003.003, 3000, 1800, 1501.5015, 1500, 0,0,0,0,0,0,0};

		int pulldownfields = 0;
		int repeat_first_field = 0;

		int tref = 0;
		int maxtref = 0;
		int frame = -1;
		int newframes = 0;
		int progressive = 0;
		int closedgop = 0;
		int smark = 0;
		int trefcheck = 0;
		int vbvdelay = 65535;
		int lastframes = job_processing.getExportedVideoFrameNumber();
		int frametype = 0;
		int SDE_marker = -1;
		int pD_marker = -1;
		int prog_seq = -1;
		int s = 0;
		int[] frt = { 5,50,20,5,5,5,5,5 };

		boolean start = false;
		boolean last = false;
		boolean changegop = false;
		boolean writeframe = true;
		boolean is_I_Frame = false;
		boolean d2vinsert = false;
		boolean mpeg2type = false;
		boolean format_changed = false;
		boolean error = false;
		boolean broken_link = false;
		boolean gop_closed = false;
		boolean sequenceheader_complete = false;
		boolean SDE_found = false;
		boolean doExport = false;

		long videotimes = job_processing.getVideoExportTime();
		long lTC = job_processing.getLastGopTimecode();
		long TC = 0;
		long lastpts = job_processing.getEndPtsOfGop() == -10000 ? -20000 : job_processing.getEndPtsOfGop();
		long startpts = lastpts;
		long tmppts = lastpts;

		long cutposition = 0;

		String frT[] = { "0","I","P","B","D","5","6","7" };

		List newPics = new ArrayList();
		List dropList = new ArrayList();
		List infos = new ArrayList();
		List newcut = new ArrayList();

		ByteArrayOutputStream gopbuffer = new ByteArrayOutputStream();
		ByteArrayOutputStream frametypebuffer = new ByteArrayOutputStream();

		byte d2vframerate = 0;

		try {

			if (job_processing.hasSequenceHeader()) 
				frametypebuffer.write((byte)0x88);

			else 
				frametypebuffer.write((byte)0x80);

			if (job_processing.getSplitSize() > 0 && !job_processing.hasSequenceHeader() && job_processing.getExportedVideoFrameNumber() == 0)
			{
				byte[] newgop = new byte[headerrescue.length + gop.length];

				System.arraycopy(headerrescue, 0, newgop, 0, headerrescue.length);
				System.arraycopy(gop, 0, newgop, headerrescue.length, gop.length);

				gop = newgop; 

				job_processing.setSequenceHeader(true);

				for (int a = 0; a < vpts[1].length; a++) 
					vpts[1][a] += headerrescue.length;
			}

			else if (!CutpointList.isEmpty() && !job_processing.hasSequenceHeader() && !CommonParsing.getCutStatus())
			{
				byte[] newgop = new byte[headerrescue.length + gop.length];

				System.arraycopy(headerrescue, 0, newgop, 0, headerrescue.length);
				System.arraycopy(gop, 0, newgop, headerrescue.length, gop.length);

				gop = newgop; 

				job_processing.setSequenceHeader(true);

				for (int a = 0; a < vpts[1].length; a++) 
					vpts[1][a] += headerrescue.length;
			}

			else if (!job_processing.hasSequenceHeader() && AddSequenceHeader)
			{
				byte[] newgop = new byte[headerrescue.length + gop.length];

				System.arraycopy(headerrescue, 0, newgop, 0, headerrescue.length);
				System.arraycopy(gop, 0, newgop, headerrescue.length, gop.length);

				gop = newgop; 

				job_processing.setSequenceHeader(true);

				for (int a = 0; a < vpts[1].length; a++) 
					vpts[1][a] += headerrescue.length;
			}

			/* header check */
			if (job_processing.hasSequenceHeader())
			{
				/**
				 * get new header, check if valid and not 0!!
				 */
				int _h_reso = ((0xFF & gop[s + 4])<<4 | (0xF0 & gop[s + 5])>>>4);
				int _v_reso = ((0xF & gop[s + 5])<<8 | (0xFF & gop[s + 6]));
				int _framerate = 0xF & gop[s + 7];
				int _dar = 0xF & gop[s + 7]>>>4;

				double d = fps_tabl2[_framerate];

				if (_h_reso == 0 || _v_reso == 0 || d == 0 || _dar == 0 || _dar > 13)
				{
					clv[4]++;
					ErrorCode |= 0x80;

					messageDropError(gop, pts, maxtref, frame, clv[6], job_processing.getExportedVideoFrameNumber(), startpts, lastpts, dumpname, ErrorCode);

					job_processing.setSequenceHeader(false);

					gopbuffer.close();
					frametypebuffer.close();
					gop = null;

					return;
				}

				CommonParsing.setVideoFramerate(d);  // framerateconstant

				VbvBuffer_Value = 16 * 1024 * ( (0x1F & gop[s + 10])<<5 | (0xF8 & gop[s + 11])>>>3 );

				String[] vbasics = { 
					String.valueOf(_h_reso),
					String.valueOf(_v_reso),
					fps_tabl1[_framerate],
					aspratio[_dar],
					""
				};

				clv[7] = _dar - 1;

				if (job_processing.isNewVideoStream() || job_processing.getExportedVideoFrameNumber() == 0)
				{
					nv = Resource.getString("video.msg.basics", "" + vbasics[0] + "*" + vbasics[1] + " @ " + vbasics[2] + " @ " + vbasics[3] + " @ " + ( ((255&gop[s+8])<<10 | (255&gop[s+9])<<2 | (192 & gop[s+10])>>>6)*400  )) + " " + ( (31&gop[s+10])<<5 | (248&gop[s+11])>>>3 );

					infos.add(nv);

					/**
					 * no frames written 'til now
					 */
					if (job_processing.getExportedVideoFrameNumber() == 0)
					{
						d2vinsert = true;
						d2vframerate = gop[s + 7];
					}

					else
						format_changed = true;

					System.arraycopy(vbasics, 0, VBASIC, 0, VBASIC.length);
				}

				if (!Arrays.equals(VBASIC, vbasics))
				{
					String str = Resource.getString("video.msg.basics", vbasics[0] + "*" + vbasics[1] + " @ " + vbasics[2] + " @ " + vbasics[3] + " @ " + ( ((255&gop[s+8])<<10 | (255&gop[s+9])<<2 | (192 & gop[s+10])>>>6)*400  )) + " " + ( (31&gop[s+10])<<5 | (248&gop[s+11])>>>3 );

					Common.setMessage("-> " + Resource.getString("video.msg.newformat", "" + clv[6]) + " (" + ct + ")");
					Common.setMessage(str);

					job_processing.getChapters().addChapter(ct, str);

					format_changed = true;

					System.arraycopy(vbasics, 0, VBASIC, 0, VBASIC.length);
				}

				s = 12;
			}

			clv[6]++;


			/* gop check */
			goploop:
			for (int returncode, pes_ID; s < gop.length - 10; s++ )
			{
				if ((returncode = CommonParsing.validateStartcode(gop, s)) < 0)
				{
					s += (-returncode) - 1;

					continue goploop;
				}

				/**
				 * action on next found startcode
				 */
				if (pD_marker > -1 && ClearCDF)
				{
					Arrays.fill( gop, pD_marker, s, (byte)0); //clear privare data on behalf of cdf flag setting

					pD_marker = -1;
				}

//
				if ((dropList.size() & 1) != 0 && !dropList.get(dropList.size() - 1).toString().startsWith("_"))
				{
					dropList.add(String.valueOf(s));
				}

				pes_ID = CommonParsing.getPES_IdField(gop, s);

				/**
				 * slices
				 */
				if (pes_ID >= CommonParsing.SLICE_START_CODE_MIN && pes_ID <= CommonParsing.SLICE_START_CODE_MAX)
				{
					if (Debug)
						System.out.println("A " + s + " /slice " + pes_ID);

					s += 7;

					continue goploop;
				}

				/**
				 * user data
				 */
				if (pes_ID == CommonParsing.USER_DATA_START_CODE)
				{
					if ((dropList.size() & 1) == 0)
						dropList.add(String.valueOf(s));

					pD_marker = s;

					s += 3;

					continue goploop;
				}
//

				/**
				 * shit progdvb_data check
				 */
				else if ((0xF0 & gop[s+3]) == 0xE0)  
				{
					int drop_length = 4;	

					if (CommonParsing.getPES_LengthField(gop, s) == 0 )
					{
						drop_length += 5 + (0xFF & gop[s + 8]);

						if (s + drop_length >= gop.length)
							drop_length = gop.length - s;

						Arrays.fill( gop, s, s + drop_length, (byte)0);

						if (Message_3)
							Common.setMessage(Resource.getString("video.msg.error.pesext_in_es", String.valueOf(clv[6] - 1), String.valueOf(s)));
					}

					else
					{
						Arrays.fill( gop, s, s + drop_length, (byte)0);

						if (Message_3)
							Common.setMessage(Resource.getString("video.msg.error.pes_in_es", String.valueOf(clv[6] - 1), String.valueOf(s)));
					}

					s += drop_length - 1;

					continue goploop;
				}

				else if (!mpeg2type && pes_ID == CommonParsing.EXTENSION_START_CODE && gop[s + 4]>>>4 == 1)    /*** 0xb5 sequence extension ***/
				{
					MPGVideotype[0] = 1;
					mpeg2type = true; 
					prog_seq = s + 5;
					SDE_marker = s + 10;
					s += 9;

					if (job_processing.hasSequenceHeader() && frametypebuffer.size() == 1)
					{
						frametypebuffer.reset();
						frametypebuffer.write((byte)(8 | (8 & gop[prog_seq])<<4));
					}
				}

				else if (!sequenceheader_complete && pes_ID == CommonParsing.EXTENSION_START_CODE && gop[s + 4]>>>4 == 2)    /*** 0xb5 MPEG-2 seq dis extension ***/
				{
					if (AddSequenceDisplayExension && job_processing.hasSequenceHeader())
						Video.setSequenceDisplayExtension( gop, s, SDE_Value, VBASIC);

					SDE_found = true;

					s += 8;
				}

				/**
				 * 0xb8 gop header + timecode 
				 */
				else if (pes_ID == CommonParsing.GROUP_START_CODE)
				{
					sequenceheader_complete = true;

					closedgop = s + 7;
					writeframe = true;

					gop_closed = (0x40 & gop[s + 7]) != 0;
					broken_link = (0x20 & gop[s + 7]) != 0; 

					//gop TC 
					TC = 90L * (3600000L * ((0x7C & gop[s + 4])>>>2) + 
						60000L * ((3 & gop[s + 4])<<4 | (0xF0 & gop[s + 5])>>>4) +
						1000L * ((7 & gop[s + 5])<<3 | (0xE0 & gop[s + 6])>>>5) +
						(long)(((0x1F & gop[s + 6])<<1 | (0x80 & gop[s + 7])>>>7) * (double)(CommonParsing.getVideoFramerate() / 90.0f)) );

					if (Math.abs(TC - job_processing.getLastGopTimecode()) < CommonParsing.getVideoFramerate() || job_processing.getSourceVideoFrameNumber() == 0)
						job_processing.setLastGopTimecode(TC);

					if (Debug)
						System.out.println("\n//b8 " + TC + "/ " + Integer.toHexString((0x80&gop[s+4]) | (0x7F&gop[s+7])) + "/ " + s + "/ " + job_processing.hasSequenceHeader());

					if (job_processing.hasSequenceHeader())
					{
						headerrescue = new byte[s];
						System.arraycopy(gop, 0, headerrescue, 0, s);
					}

					/**
					 * set Timezone (videoframerate may be 0!!, check this)
					 */
					Date videotime = new Date((long)(job_processing.getExportedVideoFrameNumber() * (double)(CommonParsing.getVideoFramerate() / 90.0f)));
					cal.setTimeZone(TimeZone.getTimeZone("GMT+0:00"));
					cal.setTime(videotime);

					/**
					 * get SMPTE (videoframerate may be 0!!, check this)
					 */
					int vh = cal.get(11);
					int vm = cal.get(12);
					int vs = cal.get(13); 
					int vf = (cal.get(14) / ((int)CommonParsing.getVideoFramerate() / 90)) ; // earlier versions +1 

					gop[4 + s] = (byte)( (128 & gop[s+4]) | vh<<2 | vm>>>4  );
					gop[5 + s] = (byte)( (15 & vm)<<4 | 8 | vs>>>3 );
					gop[6 + s] = (byte)( (7 & vs)<<5 | vf>>>1 );
					gop[7 + s] = (byte)( 127 & gop[s+7] | vf<<7 );

					s += 6;
				}

				/**
				 * 0x0 new frame 
				 */
				else if (pes_ID == CommonParsing.PICTURE_START_CODE)
				{
					sequenceheader_complete = true;

					tref = ((0xFF & gop[s + 4]) << 2) | (0xC0 & gop[s + 5])>>>6;  // temporalrefence of picture
					frametype = (0x38 & gop[s + 5])>>>3;

					if (frametype < CommonParsing.FRAME_I_TYPE || frametype > CommonParsing.FRAME_D_TYPE)
					{
						Common.setMessage(Resource.getString("video.msg.error.frame.wrong", "" + frametype) + " " + tref);
						error = true;
						ErrorCode |= 1;
					}

					newPics.add(String.valueOf(tref<<4 | frametype));

					/**
					 * vbv delay to 0xffff 
					 */
					if (ChangeBitrateInAllSequences != 2 && ChangeBitrateInAllSequences > 0 && ChangeVbvDelay > 0)
					{
						gop[s + 5] |= 0x07; 
						gop[s + 6] |= 0xFF; 
						gop[s + 7] |= 0xF8;
					} 
 
					if (tref > maxtref) 
					{
						maxtref = tref;
						tmppts = vpts[0][vpts[0].length - 1]; //pulldown
					}

					if (Debug) 
						System.out.println(frame + "/ " + maxtref + "/ " + tref + "/ " + s + " * " + frT[frametype] + "/ " + gop.length);

					if (tref == 0)  //pulldown
					{
						/* frame is increased below, use +1 to get the right array index. */
						if (frame + 1 < vpts[0].length)
							startpts = vpts[0][frame + 1];
						else
							startpts = vpts[0][0];
 					}
					/* The correct number for pulldownfields can only be determined after the
					 * frames after maxtref are read. Set lastpts after the last frame (after
					 * this loop), but use the pts from the maxtref frame (tmppts).
					 */

				//	if (!start && s >= vpts[1][0])
				//	{
				//		startpts = vpts[0][0] - (long)(CommonParsing.getVideoFramerate() * tref); 
				//		start = true;
				//	}

				//	else if (!last && s >= vpts[1][vpts[1].length - 1])
				//	{
				//		lastpts = vpts[0][vpts[0].length-1] - (long)(CommonParsing.getVideoFramerate() * tref); 
				//		last = true;
				//	}

					/**
					 *  determine cuts 
					 */
					if (frame == -1 && frametype == CommonParsing.FRAME_I_TYPE)
					{
						/**
						 * determine vbv-delay of I-frame 
						 */
						vbvdelay = (7 & gop[s + 5])<<13 | (0xFF & gop[s + 6])<<5 | (0xF8 & gop[s + 7])>>>3;
						is_I_Frame = true;

						if (job_processing.getExportedVideoFrameNumber() == 0)
							infos.add(Resource.getString("video.msg.export.start") + " " + (clv[6] - 1));

						/**
						 * drop B-Frames, also if broken_link flag is set
						 */
						if (tref > 0 && (broken_link || Math.abs(startpts - job_processing.getEndPtsOfGop()) > CommonParsing.getVideoFramerate()))
						{
							if (!gop_closed || (gop_closed && broken_link))
							{
								gop[s + 4] = 0;
								gop[s + 5] &= 0x3F;  /* set first I-Frame's tref to 0 */
								gop[closedgop] |= 0x40;

								if (broken_link)
									gop[closedgop] &= ~0x20;

								gopbuffer.write(gop, 0, s); 
	
								smark = s;

								if (job_processing.getExportedVideoFrameNumber() > 0)
									infos.add(Resource.getString("video.msg.pts.diff", String.valueOf(startpts - job_processing.getEndPtsOfGop()), Common.formatTime_1((startpts - job_processing.getEndPtsOfGop()) / 90)) + " " + (broken_link ? Resource.getString("video.msg.error.brokenlink") : ""));
	
								infos.add(Resource.getString("video.msg.frame.drop", "" + (clv[6] - 1)) + " " + Common.formatTime_1((long)(job_processing.getExportedVideoFrameNumber() * (double)(CommonParsing.getVideoFramerate() / 90.0f))));

								job_processing.countExportedVideoFrameNumber(-tref);
								newframes -= tref;
								trefcheck = tref;
								changegop = true;
								clv[0]++;  //cut
							}
						}
					}

					/**
					 * recalculate tref, timecode, frames + delete b-frames 
					 */
					if (frame > -1 && changegop)
					{
						if (writeframe)
							gopbuffer.write(gop, smark, s - smark);

						smark = s;

						/**
						 * drop b-frame from temporal sequence
						 */
						if ( trefcheck > tref )
						{
							writeframe = false;

							if ((dropList.size() & 1) == 0)
								dropList.add("_" + String.valueOf(s));
						}

						else
						{
							writeframe = true;
							gop[s + 4] = (byte)( (tref - trefcheck)>>>2);
							gop[s + 5] = (byte)( (63 & gop[s + 5]) | ((tref - trefcheck)<<6));

							if ((dropList.size() & 1) == 1)
								dropList.add("_" + String.valueOf(s));
						}
					}

					/**
					 * P-frames for cut in gop
					 */
					if (frametype == CommonParsing.FRAME_P_TYPE)
					{
						if (!changegop)
						{
							long[] cutdata = { job_processing.getSourceVideoFrameNumber(), s };
							newcut.add(cutdata);
						}

						else
						{
							long[] cutdata = { job_processing.getSourceVideoFrameNumber(), gopbuffer.size() };
							newcut.add(cutdata);
						}
					}

					job_processing.countSourceVideoFrameNumber(+1);
					job_processing.countExportedVideoFrameNumber(+1);
					frame++;
					newframes++;
					progressive = 0x80;         /* 0xb5 pic coding extension */

					for (int i = s + 6; i < s + 15 && i + 10 < gop.length; i++ ) //i + 8 doesn't reach
					{
						if ((returncode = CommonParsing.validateStartcode(gop, i)) < 0)
						{
							i += (-returncode) - 1;
							continue;
						}

						if (gop[i + 3] != (byte)0xb5 || (0xF0 & gop[i + 4]) != 0x80) 
							continue;

						//pulldown
						repeat_first_field = (0x02 & gop[i + 7]);
						if (repeat_first_field != 0)
							pulldownfields += 1;
	
						progressive = (0x80 & gop[i + 8]);

						if (PatchToProgressive) 
							gop[i + 8] |= (byte)0x80;  // mark as progressiv

						else if (PatchToInterlaced) 
							gop[i + 8] &= (byte)~0x80;  // mark as interlaced

						if (ToggleFieldorder) 
							gop[i + 7] ^= (byte)0x80;  // toggle top field first

						/**
						 * zero'es the comp.disp.flag infos
						 */
						if (ClearCDF && (0x40 & gop[i + 8]) != 0)
						{
							gop[i + 8] &= (byte)0x80;
							gop[i + 9]  = 0;
							gop[i + 10] = 0;

							if ((dropList.size() & 1) == 0)
								dropList.add(String.valueOf(i + 9));

							s = i;
						}

						break;
					}

					if (is_I_Frame || !changegop || (changegop && writeframe)) 
						frametypebuffer.write((byte)(frametype | progressive));

					s += 7; // slices B min 5, I min 50, p min 25
					is_I_Frame = false;
				}
			} // end of gop search

			/* Set last_pts after all frames in the GOP are read. */
			//pulldown
			lastpts = tmppts - (long)(CommonParsing.getVideoFramerate() * (tref + pulldownfields/2 ));


		/**
			for (int dl = 0; dl < dropList.size(); dl++)
			{
				String str = dropList.get(dl).toString();

				if (str.startsWith("_"))
					str = "_" + Integer.toHexString(Integer.parseInt(str.substring(1)));

				else
					str = Integer.toHexString(Integer.parseInt(str));

				Common.setMessage("dl " + dl + " : '0x" + str.toUpperCase() + "'");
			}
		**/

			/**
			 * put the rest of data into buffer, if gop was changed 
			 */
			if (changegop)
			{
				if (writeframe)  
					gopbuffer.write(gop, smark, gop.length - smark);

				gop = gopbuffer.toByteArray();
				gopbuffer.reset();
				changegop = false;
			}

			if (prog_seq != -1 && prog_seq < gop.length)
			{
				if (PatchToProgressive) 
					gop[prog_seq] |= (byte)8;  // mark as progressiv_seq

				else if (PatchToInterlaced) 
					gop[prog_seq] &= (byte)~8;  // unmark ^
			} 

			if (vpts[0].length < 2)
			{
				lastpts = startpts; 
				clv[1]++;
			}

			if (job_processing.get1stVideoPTS() == -1)
				job_processing.set1stVideoPTS(startpts);

			int Pics[] = new int[newPics.size()];

			for (int a = 0; a < Pics.length; a++)
				Pics[a] = Integer.parseInt(newPics.get(a).toString());

			int newTref[] = new int[Pics.length];
			Arrays.fill(newTref, -1);

			// error, no Frames found
			if (Pics.length == 0)
			{
				Common.setMessage(Resource.getString("video.msg.error.frame.not", String.valueOf(clv[6] - 1)));
				error = true;
				ErrorCode |= 2;
			}

			// error, no leading I-Frame
			if (Pics.length > 0 && (Pics[0] & 0xF) != 1)
				Common.setMessage(Resource.getString("video.msg.error.frame.not.i", String.valueOf(clv[6] - 1)));

			for (int i = 0; !error && i < Pics.length; i++)
			{
				int Tref = Pics[i]>>>4;

				if (Tref < 0 || Tref > Pics.length - 1 || newTref[Tref] != -1)
//test			if (Tref < 0 || newTref[Tref] != -1)
				{
					error = true;
					ErrorCode |= 4;
					break;
				}

				newTref[Tref] = Pics[i];
			}

			for (int i = 0; !error && i < newTref.length; i++)
				if (newTref[i] == -1)
				{
					error = true;
					ErrorCode |= 8;
				}


			/**
			 * show and save I-frame when demuxing
			 * disabled, will be changed later
			 */
			if (Save1stFrameOfGop)
			{
				Common.getMpvDecoderClass().decodeArray(gop, false, Preview_AllGops, Preview_fastDecode, Preview_YGain);
				//CommonGui.getPicturePanel().saveBMP(true, job_processing.isRunningFromCLI());
			}

			/**
			 * error, start pts is to early as last
			 */
			if (startpts < job_processing.getLastGopPts() - CommonParsing.getVideoFramerate() / 2)
			{
				Common.setMessage(Resource.getString("video.msg.error.pts.early", String.valueOf(clv[6] - 1), String.valueOf(job_processing.getLastGopPts())));
				error = true;
				ErrorCode |= 0x10;
			}

			/**
			 * falls startpts kleiner als options[8] (lende letzte GOP) aber innerhalb toleranz, 
			 * dann options[8] schreiben, setzen des neuen Endes aber mit ermittelter lastpts dieser gop
			 */
			if (TrimPts && startpts < job_processing.getEndPtsOfGop() && (double)(job_processing.getEndPtsOfGop() - startpts) < (CommonParsing.getVideoFramerate() / 2.0))
			{
				if (Debug)
					System.out.println("videostart trimmed to o8 " + job_processing.getEndPtsOfGop() + " /sp " + startpts);

				startpts = job_processing.getEndPtsOfGop();
			}

			/**
			 * error, if pts diff. between frames > half of a frame
			 */
			if (maxtref != frame || Math.abs(lastpts - startpts) > 2000)
			{
				error = true;
				ErrorCode |= 0x20;
			}

			/**
			 * ignore v-errors 
	 		*/
			if (job_processing.getSourceVideoFrameNumber() > 0 && IgnoreErrors)
			{
				lastpts = startpts;
				maxtref = frame;
				error = false;
			}

			/**
			 * error, if gop on mp@ml is too big
			 */
			if (Integer.parseInt(VBASIC[0]) <= 720 && gop.length > 2750000)
			{
				error = true;
				ErrorCode |= 0x40;
			}

			/** 
			 * return last orig pts for plain mpv
			 */
		//	job_processing.setLastSimplifiedPts(startpts + (long)(trefcheck * CommonParsing.getVideoFramerate()) + (long)((maxtref - trefcheck + 1) * CommonParsing.getVideoFramerate()));
			job_processing.setLastSimplifiedPts(startpts + (long)(trefcheck * CommonParsing.getVideoFramerate()) + (long)((maxtref + pulldownfields/2 - trefcheck + 1 ) * CommonParsing.getVideoFramerate()));


// test - edit fehlerhafte gop
			if (error)
			{
				if (UseGOPEditor)
					gop = Common.getGuiInterface().editGOP(gop, vpts);
			}
//

			/** 
			 * message error
			 */
			if (error)
			{
				job_processing.setExportedVideoFrameNumber(lastframes);
				clv[4]++;

				messageDropError(gop, pts, maxtref, frame, clv[6], job_processing.getExportedVideoFrameNumber(), startpts, lastpts, dumpname, ErrorCode);
			}

			else
			{
				/**
				 * unused!
				 * temp delete, disable for P-frame cutout
				 */
				newcut.clear(); 

				/**
				 * read out gop timecode as long ,TC 081.5++ 
				 */
			//	job_processing.countLastGopTimecode((long)(CommonParsing.getVideoFramerate() * (maxtref + 1)));
			//	job_processing.setLastGopPts(startpts + (long)(CommonParsing.getVideoFramerate() * (maxtref + 1)));
				job_processing.countLastGopTimecode((long)(CommonParsing.getVideoFramerate() * (maxtref + pulldownfields/2 + 1)));
				job_processing.setLastGopPts(startpts + (long)(CommonParsing.getVideoFramerate() * (maxtref + pulldownfields/2 + 1)));
 
				/**
				 * how to cut 
				 */
				switch (CutMode)
				{
				case CommonParsing.CUTMODE_BYTE:
					cutposition = job_processing.getCutByteposition();
					break;

				case CommonParsing.CUTMODE_GOP:
					cutposition = clv[6];
					break;

				case CommonParsing.CUTMODE_FRAME:
					cutposition = job_processing.getSourceVideoFrameNumber();
					break;

				case CommonParsing.CUTMODE_PTS:
					cutposition = startpts + 1000; //exclude jitter
					break;

				case CommonParsing.CUTMODE_TIME:
					cutposition = startpts - job_processing.get1stVideoPTS(); 
				}

				/**
				 * cut using bytepos, frame#, gop#, timecode or pts
				 */
				if (!CommonParsing.makecut(job_processing, dumpname, startpts, cutposition, newcut, lastframes, CutpointList, clv[6] - 1, job_processing.getCellTimes()))
					job_processing.setExportedVideoFrameNumber(lastframes);

				/**
				 * DAR request for auto cut  
				 */
				else if (OptionDAR && ExportDAR != clv[7]) 
					job_processing.setExportedVideoFrameNumber(lastframes);

				/**
				 * H Resolution request for auto cut  
				 */
				else if (OptionHorizontalResolution && !ExportHorizontalResolution.equals(VBASIC[0]))
					job_processing.setExportedVideoFrameNumber(lastframes);

				else if (!doWrite)
				{}

				else
				{
					startpts += (long)(trefcheck * CommonParsing.getVideoFramerate());

					/**
					 * videoframe-pts-counter 
					 */
				//	job_processing.setEndPtsOfGop(startpts + (long)((maxtref - trefcheck + 1) * CommonParsing.getVideoFramerate()));
					job_processing.setEndPtsOfGop(startpts + (long)((maxtref + pulldownfields/2 - trefcheck + 1) * CommonParsing.getVideoFramerate()));
 
					/**
					 * write V-PTS Log for audio/data sync
					 */
					log.writeLong(startpts);
					log.writeLong(job_processing.getEndPtsOfGop());

					/**
					 * write V-Time Log for audio/data sync
					 */
					log.writeLong(videotimes);
				//	log.writeLong(job_processing.countVideoExportTime((long)((maxtref - trefcheck + 1) * CommonParsing.getVideoFramerate())));
					log.writeLong(job_processing.countVideoExportTime((long)((maxtref + pulldownfields/2 - trefcheck + 1) * CommonParsing.getVideoFramerate())));

					/**
					 * value for gop bitrate per second  
					 */
					double svbr = CommonParsing.getVideoFramerate() * (maxtref - trefcheck + 1);

					if (svbr <= 0) 
						svbr = CommonParsing.getVideoFramerate() * 10;

					/**
					 * calculate the bitrate from gop length
					 * !!renew determination
					 */
					int vbr =  (int)( ( (90000L * (gop.length * 8)) / svbr ) / 400);

					/**
					 * set value for gop bitrate per second 
					 */
					if (job_processing.hasSequenceHeader())
					{
						/**
						 * value for bitrate per vbv 
						 */
						if (ChangeBitrateInAllSequences == 2 && vbvdelay < 65535 )
							vbr =  (int)( (90000L * VbvBuffer_Value) / vbvdelay / 400);

						/**
						 * set new aspectratio 
						 */
						if (ChangeAspectRatio > 0) 
							gop[7] = (byte)((0xF & gop[7]) | ChangeAspectRatio<<4);

						/**
						 * set new vbvsize 
						 */
						if (ChangeVbvBuffer > 0)
						{
							gop[10] = (byte)((0xE0 & gop[10]) | 112>>>5);
							gop[11] = (byte)((7 & gop[11]) | (0x1F & 112)<<3);
						}
					}

					/**
					 * determ. avg bitrates 
					 */
					if (vbr < job_processing.getMinBitrate()) 
						job_processing.setMinBitrate(vbr);

					if (vbr > job_processing.getMaxBitrate()) 
						job_processing.setMaxBitrate(vbr);

					if (job_processing.hasSequenceHeader())
					{
						/**
						 * set new bitrate in SH
						 */
						if (ChangeBitrateInAllSequences > 0)
						{
							int val = (ChangeBitrateInAllSequences - 3) * 2500 * 3;

							if (val > 0) 
								vbr = val;

							int vbr1 = vbr;

							if (ChangeBitrateInAllSequences == 3) 
								vbr1 = 262143;

							/**
							 * set sequence bitrate 
							 */
							gop[8] = (byte)(vbr1>>>10);
							gop[9] = (byte)(0xFF & (vbr1>>2));
							gop[10] = (byte)( (0x3F & gop[10]) | (3 & vbr1)<<6 );

							clv[2]++;
						}

						else
							clv[3]++;
					}

					/**
					 * update data for the gop info picture
					 */
					frametypebuffer.flush();

					byte ftb[] = frametypebuffer.toByteArray();
					int fields_in_frame = 0, min = 0x7FFF & clv[9]>>>15, max = 0x7FFF & clv[9], prog_flag = clv[9]>>>30;

					for (int i = 1; i < ftb.length; i++)
					{
						fields_in_frame += 2;

						prog_flag |= (0x80 & ftb[i]) != 0 ? 2 : 1;
					}

					if (fields_in_frame < min || min == 0) 
						min = fields_in_frame;
	
					if (fields_in_frame > max || max == 0) 
						max = fields_in_frame;

					clv[9] = prog_flag<<30 | (0x7FFF & min)<<15 | (0x7FFF & max);
			
					/**
					 * refresh the gop info picture
					 */
					Common.getGuiInterface().updateBitrateMonitor(vbr, ftb, Common.formatTime_1((job_processing.getVideoExportTimeSummary() + job_processing.getVideoExportTime()) / 90).substring(0, 8));

					/**
					 * add chapter index of a change in basic data
			 		 */
					if (job_processing.isNewVideoStream() && infos.size() > 0)
						job_processing.getChapters().addChapter(ct, nv);

					/**
					 * print cached messages
					 */
					for (int i = 0; i < infos.size(); i++)
					{
						Common.setMessage(infos.get(i).toString());
						job_processing.setNewVideoStream(false);
					}

					/**
					 * d2v project, update framerate
					 */
					if (d2vinsert && (CreateD2vIndex || SplitProjectFile))
					{ 
						job_processing.getProjectFileD2V().FrameRate(d2vframerate);
						d2vinsert = false;
					}

					/**
					 * d2v project, update gop line, even if video export is disabled
					 */
					job_processing.getProjectFileD2V().addGOP(job_processing.getProjectFileExportLength(), newframes);

					/**
					 * add SEC on a change of basic data
					 */
					if (format_changed && InsertEndcode)
					{
						if (WriteVideo)
						{
							video_sequence.write(Video.getSequenceEndCode());

							job_processing.countMediaFilesExportLength(+4);
							job_processing.countAllMediaFilesExportLength(+4);
							job_processing.countProjectFileExportLength(+4);
						}

						job_processing.addCellTime(String.valueOf(job_processing.getExportedVideoFrameNumber()));
						Common.setMessage("-> save ChapterFrameIndex: " + job_processing.getExportedVideoFrameNumber());
					}

					/**
					 * if write is enabled, write gop
					 */
					if (WriteVideo)
					{ 
						/**
						 * add SDE, mpeg2 only
						 */
						if (mpeg2type && AddSequenceDisplayExension && !SDE_found && job_processing.hasSequenceHeader())
						{
							int offs = SDE_marker != -1 ? SDE_marker : headerrescue.length;

							video_sequence.write(gop, 0, offs);
							video_sequence.write(Video.setSequenceDisplayExtension(SDE_Value, VBASIC));
							video_sequence.write(gop, offs, gop.length - offs);

							job_processing.countMediaFilesExportLength(+12);
							job_processing.countAllMediaFilesExportLength(+12);
							job_processing.countProjectFileExportLength(+12);
						}

						else
						{
							video_sequence.write(gop);
						}

						job_processing.countMediaFilesExportLength(gop.length);

						doExport = true;
					}

					if (ChapterpointList.indexOf(String.valueOf(cutposition)) >= 0)
					{
						job_processing.addCellTime(String.valueOf(job_processing.getExportedVideoFrameNumber()));
						Common.setMessage("-> save ChapterFrameIndex: " + job_processing.getExportedVideoFrameNumber());
					}

					job_processing.countProjectFileExportLength(gop.length);
					job_processing.countAllMediaFilesExportLength(gop.length);

				} // end if makecut

				Common.getGuiInterface().showExportStatus(doExport ? Resource.getString("audio.status.write") : Resource.getString("audio.status.pause"));
			}

			infos.clear();

			job_processing.setSequenceHeader(false);

			gopbuffer.close();
			frametypebuffer.close();

			gop = null;

		} catch (IOException e) {

			Common.setExceptionMessage(e);
		}
	}

}
