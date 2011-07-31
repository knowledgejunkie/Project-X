/*
 * @(#)Gop
 *
 * Copyright (c) 2005-2010 by dvb.matt, All Rights Reserved.
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
 * 2009-07 pulldown patch by vq
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
	private boolean progressive_sequence = false; //vq static variable to remember current status
	private long total_pulldownfields = 0;	//vq Global counter for fields added by repeat_first_field.
	 										//vq For esthetic reasons this may need to move into the
											//vq job_processing object
 
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

		//old /* vpts[0] = pts, vpts[1] = for next frame following this byteposition in sequ_array */
		//pd /*vq vpts[0] = pts, vpts[1] = frame starts at this byteposition in sequ_array */
		long[][] vpts = new long[2][pts.length / 16];

		/*vq The frepeat array stores the number of fields to be repeated for
		 *vq the frame, indexed by temporal reference, when repeat_top_field is set.
		 *vq If I would speak decent Java I would make this a List object,
		 *vq but as it is I reserve room for the maximum tref number possible.
		 */
		int[] frepeat = new int[0x400];
		//Arrays.fill(frepeat, 0); //always 0
	
		for (int i = 0; i < (pts.length / 16); i++)
		{
			for (int j = 0; j < 8; j++)
			{
				vpts[0][i] |= (0xFFL & pts[(i * 16) + j])<<((7 - j) * 8);
				vpts[1][i] |= (0xFFL & pts[(i * 16) + 8 + j])<<((7 - j) * 8);
			}
		}

		//horrible nebula PTS stuff
		//vq If more than one PTS is available (means probably there is one for each frame
		//vq but the first and the last have the same PTS, than keep only the first vpts entry
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

		String[] MPGVideoType_Str = { "MPEG-1", "MPEG-2", "MPEG-4/H.264" };

		String[] aspratio = {"res.","1.000 (1:1)","0.6735 (4:3)","0.7031 (16:9)","0.7615 (2.21:1)","0.8055","0.8437","0.9375","0.9815","1.0255","1.0695","1.1250","1.1575","1.2015","res." };
		String[] fps_tabl1 = {"forbidden fps","23.976fps","24fps","25fps","29.97fps","30fps","50fps","59.94fps","60fps","n.def.","n.def.","n.def.","n.def.","n.def.","n.def.","n.def."};

		double[] fps_tabl2 = { 0, 3753.7537, 3750, 3600, 3003.003, 3000, 1800, 1501.5015, 1500, 0,0,0,0,0,0,0};

		//vq
		int starttref = -1;
		int lasttref = -1;
		int repeat_first_field = 0;
		int top_field_first = 0;
		int pulldownfields = 0;
		int pulldownmult = 1;
		int startpd = 0;
		int lastpd = 0;

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
					nv = Resource.getString("video.msg.basics") + " " + vbasics[0] + "*" + vbasics[1] + " @ " + vbasics[2] + " @ " + vbasics[3] + " @ " + ( ((255&gop[s+8])<<10 | (255&gop[s+9])<<2 | (192 & gop[s+10])>>>6)*400  ) + " bps - vbv " + ( (31&gop[s+10])<<5 | (248&gop[s+11])>>>3 );

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
					String str = Resource.getString("video.msg.basics") + " " + vbasics[0] + "*" + vbasics[1] + " @ " + vbasics[2] + " @ " + vbasics[3] + " @ " + ( ((255&gop[s+8])<<10 | (255&gop[s+9])<<2 | (192 & gop[s+10])>>>6)*400  ) + " bps - vbv " + ( (31&gop[s+10])<<5 | (248&gop[s+11])>>>3 );

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
					progressive_sequence = (8 & gop[prog_seq]) != 0; //vq
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
						maxtref = tref;

					if (Debug) 
						System.out.println(frame + "/ " + maxtref + "/ " + tref + "/ " + s + " * " + frT[frametype] + "/ " + gop.length);

					if (!start && s >= vpts[1][0])
					{
						startpts = vpts[0][0] - (long)(CommonParsing.getVideoFramerate() * tref); 
						starttref = tref; //vq
						start = true;
					}

					else if (!last && s >= vpts[1][vpts[1].length - 1])
					{
						lastpts = vpts[0][vpts[0].length-1] - (long)(CommonParsing.getVideoFramerate() * tref); 
						lasttref = tref; //vq
						last = true;
					}

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

								//vq This should use writeframe instead
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

					repeat_first_field = 0; //vq
					top_field_first = 0; //vq

					for (int i = s + 6; i < s + 15 && i + 10 < gop.length; i++ ) //i + 8 doesn't reach
					{
						if ((returncode = CommonParsing.validateStartcode(gop, i)) < 0)
						{
							i += (-returncode) - 1;
							continue;
						}

						if (gop[i + 3] != (byte)0xb5 || (0xF0 & gop[i + 4]) != 0x80) 
							continue;

						repeat_first_field = (0x02 & gop[i + 7]); //vq

						progressive = (0x80 & gop[i + 8]);

						if (PatchToProgressive) 
							gop[i + 8] |= (byte)0x80;  // mark as progressiv

						else if (PatchToInterlaced) 
							gop[i + 8] &= (byte)~0x80;  // mark as interlaced

						if (ToggleFieldorder) 
							gop[i + 7] ^= (byte)0x80;  // toggle top field first

						top_field_first = gop[i + 7] | 0x80; //vq

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

					//vq Calculate extra number of frames
					if (repeat_first_field != 0 && writeframe)
					{
						pulldownmult = (progressive_sequence ? 2 : 1);

						int extrafields = pulldownmult;

						// top_field_first set when progressive implies another repeat
						if (progressive_sequence && top_field_first != 0 )
							extrafields += pulldownmult;

						// tref is a 10bit positive integer
						frepeat[tref] = extrafields;
						pulldownfields += extrafields;
						total_pulldownfields += extrafields;

						if (total_pulldownfields % 2 == 0 || progressive_sequence)
						{
							extrafields = (extrafields < 2 ? 1 : extrafields / 2);
							job_processing.countSourceVideoFrameNumber(extrafields);
							job_processing.countExportedVideoFrameNumber(extrafields);
						}
					}

					if (is_I_Frame || !changegop || (changegop && writeframe)) 
						frametypebuffer.write((byte)(frametype | progressive));

					s += 7; // slices B min 5, I min 50, p min 25
					is_I_Frame = false;
				}
			} // end of gop search

			//vq For pulldown: correct startpts and lastpts accordingly
			if (pulldownfields > 0)
			{
				 for (int i = 0; i < starttref; i++ )
					 startpd += frepeat[i];

				 lastpd = startpd;

				 for (int i = starttref; i < lasttref; i++ )
					 lastpd += frepeat[i];

				 startpts -= (long)(CommonParsing.getVideoFramerate() * startpd / 2 );

				 if (vpts[0].length > 1)
					 lastpts -= (long)(CommonParsing.getVideoFramerate() * lastpd / 2 );
				 else
					 lastpts = startpts;
			}


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
			 * error, start pts is too early as last
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
			//old
			//job_processing.setLastSimplifiedPts(startpts + (long)(trefcheck * CommonParsing.getVideoFramerate()) + (long)((maxtref - trefcheck + 1) * CommonParsing.getVideoFramerate()));
			
			//vq Force conversion to float by the 2.0 below, because the duration can be half a frame
			//vq longer when odd numbers for pulldownfields occur.  Same comment applies at various
			//vq other places where pulldownfields occurs.
			job_processing.setLastSimplifiedPts(startpts + (long)(trefcheck * CommonParsing.getVideoFramerate()) + (long)((maxtref + pulldownfields / 2.0 - trefcheck + 1) * CommonParsing.getVideoFramerate()));


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
				//old
				//job_processing.countLastGopTimecode((long)(CommonParsing.getVideoFramerate() * (maxtref + 1)));
				//job_processing.setLastGopPts(startpts + (long)(CommonParsing.getVideoFramerate() * (maxtref + 1)));

				//vq
				job_processing.countLastGopTimecode((long)(CommonParsing.getVideoFramerate() * (maxtref + pulldownfields / 2.0 + 1)));
				job_processing.setLastGopPts(startpts + (long)(CommonParsing.getVideoFramerate() * (maxtref + pulldownfields / 2.0 + 1)));

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
					//old
					//job_processing.setEndPtsOfGop(startpts + (long)((maxtref - trefcheck + 1) * CommonParsing.getVideoFramerate()));

					//vq
					job_processing.setEndPtsOfGop(startpts + (long)((maxtref + pulldownfields / 2.0 - trefcheck + 1) * CommonParsing.getVideoFramerate()));

					/**
					 * write V-PTS Log for audio/data sync
					 */
					log.writeLong(startpts);
					log.writeLong(job_processing.getEndPtsOfGop());

					/**
					 * write V-Time Log for audio/data sync
					 */
					log.writeLong(videotimes);
					//old
					//log.writeLong(job_processing.countVideoExportTime((long)((maxtref - trefcheck + 1) * CommonParsing.getVideoFramerate())));
					//vq
					log.writeLong(job_processing.countVideoExportTime((long)((maxtref + pulldownfields/2.0 - trefcheck + 1) * CommonParsing.getVideoFramerate())));

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


	/**
	 * gop h264 testing
	 */
	public void h264test(JobProcessing job_processing, IDDBufferedOutputStream video_sequence, byte[] gop, byte[] pts, DataOutputStream log, String dumpname, int[] MPGVideotype, List CutpointList, List ChapterpointList)
	{
		MPGVideotype[0] = 2; //h264

		String[] MPGVideoType_Str = { "MPEG-1", "MPEG-2", "MPEG-4/H.264" };

		ByteArrayOutputStream frametypebuffer = new ByteArrayOutputStream();
		ByteArrayOutputStream gopbuffer = new ByteArrayOutputStream();

		boolean doExport = false;
		int[] clv = job_processing.getStatusVariables();
		int ErrorCode = 0;
		long lastpts = job_processing.getEndPtsOfGop() == -10000 ? -20000 : job_processing.getEndPtsOfGop();
		long startpts = lastpts;
		long videotimes = job_processing.getVideoExportTime();
		int lastframes = job_processing.getExportedVideoFrameNumber();
		long cutposition = 0;
		boolean format_changed = false;
		boolean error = false;


		List newcut = new ArrayList(); // unused
		List infos = new ArrayList();

		long[][] vpts = new long[2][pts.length / 16];
		long pts_value = -1;

		for (int i = 0; i < (pts.length / 16); i++)
		{
			for (int j = 0; j < 8; j++)
			{
				vpts[0][i] |= (0xFFL & pts[(i * 16) + j])<<((7 - j) * 8); //pts
				vpts[1][i] |= (0xFFL & pts[(i * 16) + 8 + j])<<((7 - j) * 8); //valid for next pic. from pos.
			}
		}

		if (Debug)
		{
			System.out.println("\ngop" + clv[6] + "/tc_o53 " + job_processing.getLastGopTimecode() + "/lp_o54 " + job_processing.getLastGopPts() + "/lp_o8 " + job_processing.getEndPtsOfGop() + "/lp_o40 " + job_processing.getLastSimplifiedPts());

			for (int i = 0; i < vpts[0].length; i++)
				System.out.println("p" + i + " " + vpts[0][i] + "/ " + vpts[1][i]);
		}

		int au_cnt = 0;
		int frame = -1;
		int field = 0;
		int primary_pic_type = 0;
		int[] preceding_fields = new int[2];

		boolean accessunit = false;
		boolean sequ_head = false;
		int sequ_head_end = -1;
		int accessunit_pos = 0;
		int vpts_pos = 0;

		String[] VBASIC = job_processing.getStatusStrings();
		String[] vbasics = new String[5];
		int[] temp_values = new int[12];

		double[] base_pts = new double[3];
		byte[] sequ_end = { 0, 0, 0, 1, 10 };

		AccessUnit au = null;
		ArrayList accessunit_list = new ArrayList();
		ArrayList reorder_list = new ArrayList();

		try {

			// gop read
			goploop:
			for (int s = 0, returncode, unit_type, unit_ref; s < gop.length - 10; s++ )
			{
				if ((returncode = CommonParsing.validateMp4Startcode(gop, s)) < 0)
				{
					s += (-returncode) - 1;

					continue goploop;
				}

				if ((unit_type = gop[s + 4]) < 0) // is forb_zero set ?
					continue goploop;
				else
					unit_type &= 0x1F;

				unit_ref  = 3 & gop[s + 4]>>5;

				if (Debug)
					System.out.println("Au " + accessunit + " /Ap " + accessunit_pos + " /U " + s + " /ppt " + (7 & gop[s+5]>>5) + " /ref " + unit_ref + " /typ " + unit_type);

				//save pos for SEI inserting
				if (sequ_head && sequ_head_end < 0)
					sequ_head_end = s;

				switch (unit_type)
				{
				case 1: //nonIDR
				case 5: //IDR 
					if (accessunit)
					{
						pts_value = -1; //reset to pts-less state

						for (int i = vpts_pos; i < vpts[1].length; i++)
						{
							if (vpts[1][i] <= accessunit_pos)
							{
								pts_value = vpts[0][i];
								vpts_pos = i+1;
								break;
							}
						}

						if (au != null)
							au.setEnd(accessunit_pos);

						//slice header, get frame_num etc
						slice_wo_partitioning(gop, s, unit_type, temp_values);

						au = new AccessUnit(au_cnt, primary_pic_type, (7 & gop[s + 5]>>5), temp_values, pts_value, unit_type, unit_ref, accessunit_pos);
						accessunit_list.add(au);

						au_cnt++;
						frametypebuffer.write((byte)(((1 - temp_values[6])<<7 | (primary_pic_type + 1))));
						accessunit = false;
					}

					break;

				case 2: //slice p A
				case 3: //slice p B
				case 4: //slice p C
					break;

				case 6: //SEI
					readSEIMessage(gop, s);
					break;

				case 7: //sequ set
					sequ_head = true;
					readH264SequenceParameterSet(gop, s, vbasics, temp_values);

					frametypebuffer.write((byte)0x88);
					CommonParsing.setVideoFramerate(90000.0 / Double.parseDouble((vbasics[2].substring(0, vbasics[2].indexOf(" ", 2))).trim()));  // framerateconstant

					if (job_processing.isNewVideoStream() || job_processing.getExportedVideoFrameNumber() == 0)
					{
						infos.add(Resource.getString("video.msg.basics") + " " + MPGVideoType_Str[MPGVideotype[0]] + " " + vbasics[4] + ", " + vbasics[0] + "*" + vbasics[1] + ", " + vbasics[2] + " @ " + vbasics[3]);

						// no frames written 'til now
						if (job_processing.getExportedVideoFrameNumber() > 0)
							format_changed = true;

						System.arraycopy(vbasics, 0, VBASIC, 0, VBASIC.length);
					}

					if (!Arrays.equals(VBASIC, vbasics))
					{
						String str = Resource.getString("video.msg.basics") + " MPEG-4/H.264, " + vbasics[4] + ", " + vbasics[0] + "*" + vbasics[1] + ", " + vbasics[2] + " @ " + vbasics[3];

						Common.setMessage("-> " + Resource.getString("video.msg.newformat", "" + clv[6]));
						Common.setMessage(str);

						format_changed = true;

						System.arraycopy(vbasics, 0, VBASIC, 0, VBASIC.length);
					}

					break;

				case 8: //pict set
					break;

				case 9: //access  
					accessunit = true;
					accessunit_pos = s;
					primary_pic_type = (7 & gop[s + 5]>>5); //XXX0-0000  0 to 7 prim pic type p.73
					break;

				case 10: //seq end
					Common.setMessage("-> SequenceEndMarker detected at rel. pos " + s);
					break;

				case 11: //stream end
					Common.setMessage("-> StreamEndMarker detected at rel. pos " + s);
					break;

				case 12: //fill
					break;
				}

			}

			if (au != null)
				au.setEnd(gop.length);


			//error checks
			if (accessunit_list.size() > 0)
			{
				au = (AccessUnit) accessunit_list.get(0);

				if (au.getPPTAU() != 0) 
				{
					// error, no leading I-Frame
					Common.setMessage(Resource.getString("video.msg.error.frame.not.i", String.valueOf(clv[6] - 1)));
					ErrorCode |= 1;
				}
				else
					startpts = au.getPTS();
			}
			else
			{
				// error, no Frames found
				Common.setMessage(Resource.getString("video.msg.error.frame.not", String.valueOf(clv[6] - 1)));
				ErrorCode |= 2;
			}


			if (ErrorCode != 0)
				error = true;
//info
			if (!error)
			{
				for (int i = 0, fr = 0, j = 0, k = 0; i < accessunit_list.size(); i++)
				{
					au = (AccessUnit) accessunit_list.get(i);

				//	Common.setMessage(au.toString());

					if (i == 0) // first frame, can have Picorder from last seq
					{
						base_pts[0] = au.getPTS() - au.getPicOrder() * (CommonParsing.getVideoFramerate() / 2.0);
						fr = au.getFrameNum();
						reorder_list.add(au);
					}
					else
					{
						base_pts[1] = au.getPTS() - au.getPicOrder() * (CommonParsing.getVideoFramerate() / 2.0);

						//base pts, cannot detect missing frames
						if (au.getPTS() != -1 && Math.abs(base_pts[0] - base_pts[1]) > 100)
						{
							//Common.setMessage("!> base pts validation failed: " + Math.abs(base_pts[0] - base_pts[1]));
							ErrorCode |= 4;
						}
					}

					// field cnt
					field += au.isFieldPic() ? 1 : 2;

					//determine 1st pts of Sequ
					if (au.getPTS() < startpts && au.getPTS() != -1)
						startpts = au.getPTS();

					//determine framenum discontinuity, may not detect missing frames
					if (au.getFrameNum() != fr && au.getFrameNum() != ((1 + fr) & (-1>>>(32 - temp_values[0]))))
					{
						ErrorCode |= 8;
						Common.setMessage("!> discontinuity in framenum: exp/rec " + (fr + 1) + "/" + au.getFrameNum());
					}

					fr = au.getFrameNum();

					//reorder to pts indicated order
					for (k = j, j = 0; i > 0 && j < reorder_list.size(); j++)
					{
						if (au.getPTS() == -1)
						{
							reorder_list.add(k + 1, au);
							break;
						}

						if (au.getPTS() < ((AccessUnit) reorder_list.get(j)).getPTS())
						{
							reorder_list.add(j, au);
							break;
						}
					}

					if (j == reorder_list.size())
						reorder_list.add(au);
				}

				//detect missing frames in reordered list
				for (int i = 0, j = 0, k = 0; i < reorder_list.size(); i++)
				{
					au = (AccessUnit) reorder_list.get(i);

					if (i == 0)
						base_pts[2] = au.getPTS();

					j = au.isFieldPic() ? 2 : 1; //offs-length of last field/frame

					if (i > 0 && au.getPTS() != -1 && Math.abs(au.getPTS() - base_pts[2]) > 100)
						ErrorCode |= 0x20;

					base_pts[2] += CommonParsing.getVideoFramerate() / j;

					//count preceding fields ref'd from last gop
					if (au.getPPTAU() == 0) // || au.getUnitRef() != 0)
						k = 1;

					else if (k == 0)
					{
						preceding_fields[0]++; //au cnt
						preceding_fields[1] += au.isFieldPic() ? 1 : 2; //field cnt
					}
				}

				//no missing frame, reset pts base validation error 
				if ((0x24 & ErrorCode) == 4)
					ErrorCode &= ~4;

				//frame count, maybe not suitable for pulldown
				frame = field / 2; 

				//cleaned pts for gop end
				lastpts = startpts + Math.round(field * CommonParsing.getVideoFramerate() / 2.0);

			}

			clv[6]++;  //GOP count

			// error, start pts is too early against last
			if (startpts < job_processing.getLastGopPts() - CommonParsing.getVideoFramerate() / 2)
			{
				Common.setMessage(Resource.getString("video.msg.error.pts.early", String.valueOf(clv[6] - 1), String.valueOf(job_processing.getLastGopPts())));
				ErrorCode |= 0x10;
			}

			if (ErrorCode != 0)
				error = true;

			if (Debug)
			{
				for (int i = 0; i < accessunit_list.size(); i++)
					System.out.println("E " + accessunit_list.get(i).toString());

				for (int i = 0; i < reorder_list.size(); i++)
					System.out.println("R " + reorder_list.get(i).toString());
			}

			// message error
			if (error)
			{
				//Common.setMessage("<< error >>");
				//Common.setMessage("fld " + field + " /sp " + startpts + " /lp " + lastpts + " /E " + error);

				job_processing.setExportedVideoFrameNumber(lastframes);
				clv[4]++;

				messageDropError(gop, pts, (int)((base_pts[2] - startpts) / CommonParsing.getVideoFramerate()), frame, clv[6], job_processing.getExportedVideoFrameNumber(), startpts, lastpts, dumpname, ErrorCode);
			//	messageDropError(gop, pts, au_cnt, frame, clv[6], job_processing.getExportedVideoFrameNumber(), startpts, lastpts, dumpname, ErrorCode);
			}

			else
			{
				// how to cut 
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

				// cut using bytepos, frame#, gop#, timecode or pts
				if (!CommonParsing.makecut(job_processing, dumpname, startpts, cutposition, newcut, lastframes, CutpointList, clv[6] - 1, job_processing.getCellTimes()))
					job_processing.setExportedVideoFrameNumber(lastframes);

				// DAR request for auto cut  
				else if (OptionDAR && ExportDAR != clv[7]) 
					job_processing.setExportedVideoFrameNumber(lastframes);

				// H Resolution request for auto cut  
				else if (OptionHorizontalResolution && !ExportHorizontalResolution.equals(VBASIC[0]))
					job_processing.setExportedVideoFrameNumber(lastframes);

				//else if (!doWrite)
				//{}

				else
				{
					if (job_processing.getExportedVideoFrameNumber() == 0)
						infos.add(Resource.getString("video.msg.export.start") + " " + (clv[6] - 1));

					// gap > 1/2 frame time to last gop, with possibly unref'd frames in gop
					if (preceding_fields[1] > 0 && job_processing.getLastGopPts() < startpts - CommonParsing.getVideoFramerate() / 2)
					{
						for (int i = 0, j = 0; i < preceding_fields[0]; i++)
						{
							j = ((AccessUnit) reorder_list.get(i)).getCount();

							//remove first unref'd frames
							for (int k = 0; k < accessunit_list.size(); k++)
							{
								au = (AccessUnit) accessunit_list.get(k);

								if (j == au.getCount())
								{
									field -= au.isFieldPic() ? 1 : 2;
									startpts += CommonParsing.getVideoFramerate() / (au.isFieldPic() ? 2 : 1);

									accessunit_list.remove(k);
									break;
								}
							}
						}

						//rewrite new gop, adapt picorder ??
						for (int i = 0; i < accessunit_list.size(); i++)
						{
							au = (AccessUnit) accessunit_list.get(i);
						/**
							if (i == 0 && job_processing.getExportedVideoFrameNumber() > 0)
							{
								gopbuffer.write(gop, au.getStart(), sequ_head_end);
								gopbuffer.write(setSEIMsg6(au.getFrameNum()));
								gopbuffer.write(gop, sequ_head_end, au.getLength());
							}
							else
						**/
								gopbuffer.write(gop, au.getStart(), au.getLength());

							if (Debug)
								System.out.println("bu " + i + " /au " + au.getCount() + " /fn " + au.getFrameNum() + " /pptau " + au.getPPTAU() + " /ppt " + au.getPPT() + " /st " + au.getStart() + " /en " + au.getEnd());
						}

						gopbuffer.flush();
						gop = gopbuffer.toByteArray();

						frame = field / 2;
						infos.add("!> removing " + preceding_fields[1] + " preceding fields from GOP #" + (clv[6] - 1));
					}

					// print cached messages
					for (int i = 0; i < infos.size(); i++)
					{
						Common.setMessage(infos.get(i).toString());
						job_processing.setNewVideoStream(false);
					}

					// if write is enabled, write gop
					if (WriteVideo)
					{ 
						// value for gop bitrate per second  
						double svbr = CommonParsing.getVideoFramerate() * frame;

						if (svbr <= 0) //20 frames schätzung
							svbr = CommonParsing.getVideoFramerate() * 20;
	
						int vbr = (int)( ( (90000L * (gop.length * 8)) / svbr ) / 400);
	
						// determ. avg bitrates 
						if (vbr < job_processing.getMinBitrate()) 
							job_processing.setMinBitrate(vbr);

						if (vbr > job_processing.getMaxBitrate()) 
							job_processing.setMaxBitrate(vbr);

						// update data for the gop info picture
						frametypebuffer.flush();

						byte[] ftb = frametypebuffer.toByteArray();
						int min = 0x7FFF & clv[9]>>>15;
						int max = 0x7FFF & clv[9];
						int prog_flag = clv[9]>>>30;

						for (int i = 1; i < ftb.length; i++)
							prog_flag |= (0x80 & ftb[i]) != 0 ? 2 : 1;

						if (field < min || min == 0) 
							min = field;
	
						if (field > max || max == 0) 
							max = field;

						clv[9] = prog_flag<<30 | (0x7FFF & min)<<15 | (0x7FFF & max);
						//<--

						Common.getGuiInterface().updateBitrateMonitor(vbr, ftb, Common.formatTime_1((job_processing.getVideoExportTimeSummary() + job_processing.getVideoExportTime()) / 90).substring(0, 8));

						/**gap what breaks sequence
						if (job_processing.getExportedVideoFrameNumber() > 0 && job_processing.getLastGopPts() < startpts - CommonParsing.getVideoFramerate() / 2)
						{
							video_sequence.write(sequ_end);
							Common.setMessage("-> sequence closed after GOP# " + (clv[6] - 1));

							job_processing.countMediaFilesExportLength(sequ_end.length);
							job_processing.countAllMediaFilesExportLength(sequ_end.length);
						}
						**/

						video_sequence.write(gop);

						//clv[6]++; //GOP count
						clv[3]++; //unchanged GOP count

						job_processing.countExportedVideoFrameNumber(frame);

						job_processing.setEndPtsOfGop(lastpts);
						job_processing.setLastGopPts(lastpts);

						// write V-PTS Log for audio/data sync
						log.writeLong(startpts);
						log.writeLong(job_processing.getEndPtsOfGop());

						// write V-Time Log for audio/data sync
						log.writeLong(videotimes);
						job_processing.countVideoExportTime(lastpts - startpts);

						log.writeLong(job_processing.getVideoExportTime());

						job_processing.countMediaFilesExportLength(gop.length);

						job_processing.countAllMediaFilesExportLength(gop.length);

						doExport = true; // no cut ATM

						Common.getGuiInterface().showExportStatus(doExport ? Resource.getString("audio.status.write") : Resource.getString("audio.status.pause"));
					}
				}
			}

			infos.clear();


		} catch (IOException e) {

			Common.setExceptionMessage(e);
		}
	}

	/**
	 * H264 SequenceParameterSet
	 */
	private boolean readH264SequenceParameterSet(byte[] array, int offset, String[] vbasics, int[] temp_values)
	{ 
		byte[] check = new byte[100];

		int[] BitPosition = { 0 };
		BitPosition[0] = (4 + offset)<<3;

		int zero = getBits(array, BitPosition, 1); //forb_zero = 0x80 & check[4 + i];
		int nal_ref = getBits(array, BitPosition, 2); //nal_ref  = (0xE0 & check[4 + i])>>>5;
		int nal_unit = getBits(array, BitPosition, 5); //nal_unit = 0x1F & check[4 + i];

		if (zero != 0 || nal_unit != 7)
			return false;

		//emulation prevention
		for (int m = 5 + offset, rbsp = 0; rbsp < 100 - 3; m++)
		{
			if (array[m] == 0 && array[m + 1] == 0 && array[m + 2] == 3)
			{
				rbsp += 2; //2 bytes value 0
				m += 2; //emulation_prevention_three_byte /* equal to 0x03 */
			}
			else
				check[rbsp++] = array[m];
		}

		//reset for check
		BitPosition[0] = 0;

		//seq_param
		int profile_idc = getBits(check, BitPosition, 8); //profile = 0xFF & check[5 + i];
		getBits(check, BitPosition, 4); //constraint 0,1,2,3
		zero = getBits(check, BitPosition, 4); //4 res_zero_bits

		if (zero != 0)
			return false;

		int level_idc = getBits(check, BitPosition, 8); //0xFF & check[7 + i];
		int flag = getCodeNum(check, BitPosition); // seq_parameter_set_id 0 ue(v)

		int chroma_format_idc = 1; //dflt

		if (profile_idc == 100 || profile_idc == 110 || profile_idc == 122 || profile_idc == 44 || profile_idc == 244)
		{
			chroma_format_idc = getCodeNum(check, BitPosition); //chroma_format_idc 0 ue(v)

			if (chroma_format_idc == 3)
				getBits(check, BitPosition, 1); //separate_colour_plane_flag 0 u(1)

			getCodeNum(check, BitPosition); //bit_depth_luma_minus8 0 ue(v)
			getCodeNum(check, BitPosition); //bit_depth_chroma_minus8 0 ue(v)

			getBits(check, BitPosition, 1); //qpprime_y_zero_transform_bypass_flag 0 u(1)
			flag = getBits(check, BitPosition, 1); //seq_scaling_matrix_present_flag 0 u(1)

			if (flag == 1)
			{
				for (int i = 0; i < ((chroma_format_idc != 3) ? 8 : 12); i++)
				{
					//seq_scaling_list_present_flag[ i ] 0 u(1)

					if (getBits(check, BitPosition, 1) == 1)
						if (i < 6)
							scaling_list(check, BitPosition, null, 16, null); //scaling_list( ScalingList4x4[ i ], 16,
							//UseDefaultScalingMatrix4x4Flag[ i ])
						else
							scaling_list(check, BitPosition, null, 64, null); //scaling_list( ScalingList8x8[ i – 6 ], 64,
							//UseDefaultScalingMatrix8x8Flag[ i – 6 ] )
				}
			}
		}

		temp_values[0] = 4 + getCodeNum(check, BitPosition); // log2_max_frame_num_minus4 0 ue(v)
		temp_values[2] = getCodeNum(check, BitPosition); // pic_order_cnt_type 0 ue(v)

		if (temp_values[2] == 0)
			temp_values[3] = 4 + getCodeNum(check, BitPosition); // log2_max_pic_order_cnt_lsb_minus4 0 ue(v)

		else if (temp_values[2] == 1)
		{
			getBits(check, BitPosition, 1); //delta_pic_order_always_zero_flag 0 u(1)
			getSignedCodeNum(check, BitPosition); //offset_for_non_ref_pic 0 se(v)
			getSignedCodeNum(check, BitPosition); //offset_for_top_to_bottom_field 0 se(v)
			flag = getCodeNum(check, BitPosition); // num_ref_frames_in_pic_order_cnt_cycle 0 ue(v)

			for (int k = 0; k < flag; k++)
				getSignedCodeNum(check, BitPosition); //offset_for_ref_frame[ i ] 0 se(v)
		}

		temp_values[8] = getCodeNum(check, BitPosition); //num_ref_frames 0 ue(v)
		temp_values[9] = getBits(check, BitPosition, 1); //gaps_in_frame_num_value_allowed_flag 0 u(1)

		if (Debug)
			System.out.println("num_ref_fr " + temp_values[8] + " /gapsallow " + temp_values[9]);

		int hori = 16 * (1 + getCodeNum(check, BitPosition)); //pic_width_in_mbs_minus1 0 ue(v)
		int vert = 16 * (1 + getCodeNum(check, BitPosition)); //pic_height_in_map_units_minus1 0 ue(v)

		flag = getBits(check, BitPosition, 1); //frame_mbs_only_flag 0 u(1)
		temp_values[5] = flag;

		vbasics[0] = String.valueOf(hori);
		vbasics[1] = String.valueOf(flag == 0 ? vert<<1 : vert);

		if (profile_idc == 66)
			vbasics[4] = "Base@";
		else if (profile_idc == 77)
			vbasics[4] = "Main@";
		else if (profile_idc == 88)
			vbasics[4] = "Ext@";
		else if (profile_idc == 100)
			vbasics[4] = "High@";
		else if (profile_idc == 110)
			vbasics[4] = "High10@";
		else if (profile_idc == 122)
			vbasics[4] = "High422";
		else if (profile_idc == 144)
			vbasics[4] = "High444@";
		else if (profile_idc == 44)
			vbasics[4] = "High444a@";
		else if (profile_idc == 244)
			vbasics[4] = "High444b@";
		//else if (profile_idc == 166)
		//	vbasics[2] = "High@";
		//else if (profile_idc == 188)
		//	vbasics[2] = "High@";
		else
			vbasics[4] = String.valueOf(profile_idc) + "@";

		vbasics[4] += String.valueOf(level_idc / 10.0);

		if (flag == 0)  //if( !frame_mbs_only_flag )
			getBits(check, BitPosition, 1); //mb_adaptive_frame_field_flag 0 u(1)

		getBits(check, BitPosition, 1); //direct_8x8_inference_flag 0 u(1)
		flag = getBits(check, BitPosition, 1); //frame_cropping_flag 0 u(1)

		if (flag == 1) //if( frame_cropping_flag ) {
		{
			getCodeNum(check, BitPosition); //frame_crop_left_offset 0 ue(v)
			getCodeNum(check, BitPosition); //frame_crop_right_offset 0 ue(v)
			getCodeNum(check, BitPosition); //frame_crop_top_offset 0 ue(v)
			getCodeNum(check, BitPosition); //frame_crop_bottom_offset 0 ue(v)
		}

		flag = getBits(check, BitPosition, 1); //vui_parameters_present_flag 0 u(1)

		if (flag == 1) //if( vui_parameters_present_flag )
			vui_parameters(check, BitPosition, vbasics); //vui_parameters( ) 0

		//rbsp_trailing_bits( ) 0

		return true;
	}

	// h264 parse signed code num
	private int getSignedCodeNum(byte[] array, int[] BitPosition)
	{
		int codeNum = getCodeNum(array, BitPosition);

		codeNum = (codeNum & 1) == 0 ? codeNum>>>1 : -(codeNum>>>1);

		return codeNum;
	}

	// h264 parse unsigned code num
	private int getCodeNum(byte[] array, int[] BitPosition)
	{
		int leadingZeroBits = -1;
		int codeNum;

		for (int b = 0; b == 0; leadingZeroBits++)
			b = getBits(array, BitPosition, 1);

		codeNum = (1<<leadingZeroBits) - 1 + getBits(array, BitPosition, leadingZeroBits);

		return codeNum;
	}

	// 7.3.2.1.1 Scaling list syntax
	private void scaling_list(byte[] check, int[] BitPosition, int[] scalingList, int sizeOfScalingList, boolean[] useDefaultScalingMatrixFlag)
	{
		int lastScale = 8;
		int nextScale = 8;

		for (int j = 0; j < sizeOfScalingList; j++)
		{
			if (nextScale != 0)
			{
				int delta_scale = getSignedCodeNum(check, BitPosition); //delta_scale 0 | 1 se(v)
				nextScale = (lastScale + delta_scale + 256) % 256;
				//useDefaultScalingMatrixFlag = ( j == 0 && nextScale == 0 );
			}

			//scalingList[ j ] = ( nextScale = = 0 ) ? lastScale : nextScale
			lastScale = (nextScale == 0) ? lastScale : nextScale; //lastScale = scalingList[ j ]
		}
	}

	// E1.1 VUI parameters syntax
	private void vui_parameters(byte[] check, int[] BitPosition, String[] vbasics)
	{
		String[] aspect_ratio_string_h264 = {
			"Unspec.", "1:1", "12:11", "10:11", "16:11", "40:33", "24:11", "20:11",
			"32:11", "80:33", "18:11", "15:11",	"64:33", "160:99", "4:3", "3:2", "2:1"
		};
		double[] aspect_ratio_double_h264 = {
			1.0, 1.0, 1.0909, 0.90909, 1.45454, 1.21212, 2.18181, 1.81818,
			2.90909, 2.42424, 1.63636, 1.36364, 1.93939, 1.61616, 1.33333, 1.5, 2
		};

		int flag = getBits(check, BitPosition, 1); //aspect_ratio_info_present_flag 0 u(1)

		if (flag == 1) //if( aspect_ratio_info_present_flag ) {
		{
			int aspect_ratio_idc = getBits(check, BitPosition, 8); //aspect_ratio_idc 0 u(8)

			if (aspect_ratio_idc == 255) //if( aspect_ratio_idc = = Extended_SAR ) {
			{
				int sar_w = getBits(check, BitPosition, 16); //sar_width 0 u(16)
				int sar_h = getBits(check, BitPosition, 16); //sar_height 0 u(16)
				vbasics[3] = "SAR: " + sar_w + ":" + sar_h;
				vbasics[3] += " >> " + String.valueOf((int)(Double.parseDouble(vbasics[0]) * sar_w / sar_h)) + "*" + vbasics[1];
			}
			else
			{
				vbasics[3] = "SAR: " + (aspect_ratio_idc < 17 ? aspect_ratio_string_h264[aspect_ratio_idc] : "res.");
				vbasics[3] += " >> " + (aspect_ratio_idc < 17 ? String.valueOf(Math.round(Double.parseDouble(vbasics[0]) * aspect_ratio_double_h264[aspect_ratio_idc])) : "res.") + "*" + vbasics[1];
			}
		}

		flag = getBits(check, BitPosition, 1); //overscan_info_present_flag 0 u(1)

		if (flag == 1) //if( overscan_info_present_flag )
			getBits(check, BitPosition, 1); //overscan_appropriate_flag 0 u(1)

		flag = getBits(check, BitPosition, 1); //video_signal_type_present_flag 0 u(1)

		if (flag == 1) //if( video_signal_type_present_flag ) {
		{
			getBits(check, BitPosition, 3); //video_format 0 u(3)
			getBits(check, BitPosition, 1); //video_full_range_flag 0 u(1)

			flag = getBits(check, BitPosition, 1); //colour_description_present_flag 0 u(1)

			if (flag == 1) //if( colour_description_present_flag ) {
			{
				getBits(check, BitPosition, 8); //colour_primaries 0 u(8)
				getBits(check, BitPosition, 8); //transfer_characteristics 0 u(8)
				getBits(check, BitPosition, 8); //matrix_coefficients 0 u(8)
			}
		}

		flag = getBits(check, BitPosition, 1); //chroma_loc_info_present_flag 0 u(1)

		if (flag == 1) //if( chroma_loc_info_present_flag ) {
		{
			getCodeNum(check, BitPosition); //chroma_sample_loc_type_top_field 0 ue(v)
			getCodeNum(check, BitPosition); //chroma_sample_loc_type_bottom_field 0 ue(v)
		}

		flag = getBits(check, BitPosition, 1); //timing_info_present_flag 0 u(1)

		if (flag == 1) //if( timing_info_present_flag ) {
		{
			int num_units_ticks = getBits(check, BitPosition, 24)<<8 | getBits(check, BitPosition, 8); //num_units_in_tick 0 u(32)
			int time_scale = getBits(check, BitPosition, 24)<<8 | getBits(check, BitPosition, 8); //time_scale 0 u(32)
			int fixed_framerate = getBits(check, BitPosition, 1); //fixed_frame_rate_flag 0 u(1)

			vbasics[2] = ((1000 * time_scale / (2 * num_units_ticks)) / 1000.0) + " fps (" + (fixed_framerate == 1 ? "f)" : "v)");
		}

		//getBits(check, BitPosition, 1); //nal_hrd_parameters_present_flag 0 u(1)
		//if( nal_hrd_parameters_present_flag )
		//hrd_parameters( )
		//vcl_hrd_parameters_present_flag 0 u(1)
		//if( vcl_hrd_parameters_present_flag )
		//hrd_parameters( )
		//if( nal_hrd_parameters_present_flag | | vcl_hrd_parameters_present_flag )
		//low_delay_hrd_flag 0 u(1)
		//pic_struct_present_flag 0 u(1)
		//bitstream_restriction_flag 0 u(1)
		//if( bitstream_restriction_flag ) {
		//motion_vectors_over_pic_boundaries_flag 0 u(1)
		//max_bytes_per_pic_denom 0 ue(v)
		//max_bits_per_mb_denom 0 ue(v)
		//log2_max_mv_length_horizontal 0 ue(v)
		//log2_max_mv_length_vertical 0 ue(v)
		//num_reorder_frames 0 ue(v)
		//max_dec_frame_buffering 0 ue(v)
		//}
	}

	// 7.3.2.8 + 7.3.3 slice w/o partitioning
	private void slice_wo_partitioning(byte[] check, int offset, int unittype, int[] temp_values)
	{
		int flag;
		int[] BitPosition = { 0 };
		BitPosition[0] = (5 + offset)<<3;

		flag = getCodeNum(check, BitPosition); //first_mb_in_slice 2 ue(v)
		temp_values[11] = getCodeNum(check, BitPosition); //slice_type 2 ue(v)
		flag = getCodeNum(check, BitPosition); //pic_parameter_set_id 2 ue(v)
		temp_values[1] = getBits(check, BitPosition, temp_values[0]);//frame_num 2 u(v)

		if (temp_values[5] == 0) //if( !frame_mbs_only_flag )
		{
			temp_values[6] = getBits(check, BitPosition, 1); //field_pic_flag 2 u(1)

			if (temp_values[6] == 1) //if( field_pic_flag )
				temp_values[7] = getBits(check, BitPosition, 1); //bottom_field_flag 2 u(1)
		}

		if (unittype == 5) //if( nal_unit_type = = 5 )
			getCodeNum(check, BitPosition); //idr_pic_id 2 ue(v)

		if (temp_values[2] == 0) //if( pic_order_cnt_type = = 0 )
		{
			temp_values[10] = BitPosition[0]; //mark position
			temp_values[4] = getBits(check, BitPosition, temp_values[3]); //pic_order_cnt_lsb 2 u(v)
			//if( pic_order_present_flag && !field_pic_flag )
			//delta_pic_order_cnt_bottom 2 se(v)
		}

		//if( pic_order_cnt_type = = 1 && !delta_pic_order_always_zero_flag ) {
		//delta_pic_order_cnt[ 0 ] 2 se(v)
		//if( pic_order_present_flag && !field_pic_flag )
		//delta_pic_order_cnt[ 1 ] 2 se(v)
		//}
	}

	// 7.3.2.3 SEI
	private void readSEIMessage(byte[] check, int offset)
	{
		int val = 0;
		int[] BitPosition = { 0 };
		BitPosition[0] = (5 + offset)<<3;

		int payloadType = 0; //payloadType = 0

		while ((val = getBits(check, BitPosition, 8)) == 0xFF) //while( next_bits( 8 ) = = 0xFF )
		{
			//ff_byte /* equal to 0xFF */ 5 f(8)
			payloadType += 255; //payloadType += 255
		}

		//last_payload_type_byte 5 u(8)
		payloadType += val; //payloadType += last_payload_type_byte

		int payloadSize = 0; //payloadSize = 0

		while ((val = getBits(check, BitPosition, 8)) == 0xFF)//while( next_bits( 8 ) = = 0xFF ) {
		{
			//ff_byte /* equal to 0xFF */ 5 f(8)
			payloadSize += 255; //payloadSize += 255
		}

		//last_payload_size_byte 5 u(8)
		payloadSize += val; //payloadSize += last_payload_size_byte

		if (Debug)
			System.out.println("SEI pos " + offset + " /pT " + payloadType + " /pS " + payloadSize);
		//sei_payload( payloadType, payloadSize ) 5
	}

	// SEI recovery point D2.7
	private byte[] setSEIMsg6(int framenum)
	{
		//00 00 00 01 06 06 01 A4 80 
		//rec_frmcnt ue(v) = 1 = 0 = framenum-start of gop
		//exactmatch  1    = 0
		//brokenlink  1    = 1
		//chang_slic  2    = 00
		//endmark    ..    = 100

		int val = 0x00000000; // 01 00 00 00 - 01 length-v

		//exp golomb set ue(v)
		for (int i = 31, nval = framenum + 1, j = 0; i >= 0; i--)
		{
			if ((nval & 1<<i) != 0)
			{
				j = 24 - 1 - (i<<1);
				val |= nval<<j; //framenum

				j -= 4;
				val |= 4<<j; //exm, bl, chng

				if ((j & 7) != 0)
				{
					j -= 1;
					val |= 1<<j;
				}

				j >>>= 3;
				val |= (3 - j)<<24; //len

				j -= 1;
				val |= 0x80<<(j<<3);

				// 00 00 00 01 06 06 0x aa bb
				byte[] sei = new byte[10 - j];
				sei[3] = 1;
				sei[4] = 6;
				sei[5] = 6;
				sei[6] = (byte)(val>>24);
				sei[7] = (byte)(val>>16);
				sei[8] = (byte)(val>>8);

				if (sei.length > 9)
					sei[9] = (byte)val;

				if (Debug)
				{
					for (int m = 0; m < sei.length; m++)
						System.out.println("m " + m + " / " + Integer.toHexString(0xFF & sei[m]));
				}

				return sei;
			}
		}

		return (new byte[0]); // failure
	}


	// parse bits
	private int getBits(byte[] array, int[] BitPosition, int N)
	{
		int Pos, Val;
		Pos = BitPosition[0]>>>3;

		if (N == 0)
			return 0;

		if (Pos >= array.length - 4)
		{
			BitPosition[0] += N;
			return -1;
		}

		Val =   (0xFF & array[Pos])<<24 |
			(0xFF & array[Pos + 1])<<16 |
			(0xFF & array[Pos + 2])<<8 |
			(0xFF & array[Pos + 3]);

		Val <<= BitPosition[0] & 7;
		Val >>>= 32 - N;

		BitPosition[0] += N;

		return Val;
	}


	class AccessUnit {

		int cnt = -1;
		int pptau = -1;
		int ppt = -1;
		int framenum = -1;
		int picorder = -1;
		long pts = -1;
		int unittype = -1;
		int unitref = -1;
		int start = -1;
		int end = -1;
		int fieldpic = 0;
		int btmfield = 0;
		int marker = 0;
		int slicetype = 0;

		public AccessUnit(int _cnt, int _pptau, int _ppt, int[] _tempval, long _pts, int _unittype, int _unitref, int _start)
		{
			cnt = _cnt;
			pptau = _pptau;
			ppt = _ppt;
			framenum = _tempval[1];
			picorder = _tempval[4];
			fieldpic = _tempval[6];
			btmfield = _tempval[7];
			marker = _tempval[10];
			slicetype = _tempval[11];
			pts = _pts;
			start = _start;
			unittype = _unittype;
			unitref = _unitref;
		}

		private void setEnd(int _end)
		{
			end = _end;
		}

		private int getCount()
		{
			return cnt;
		}

		private int getPPTAU()
		{
			return pptau;
		}

		private int getPPT()
		{
			return ppt;
		}

		private int getPicOrder()
		{
			return picorder;
		}

		private boolean isFieldPic()
		{
			return (fieldpic != 0);
		}

		private boolean isBtmPic()
		{
			return (btmfield != 0);
		}

		private int getFrameNum()
		{
			return framenum;
		}

		private int getUnitType()
		{
			return unittype;
		}

		private int getUnitRef()
		{
			return unitref;
		}

		private int getStart()
		{
			return start;
		}

		private int getEnd()
		{
			return end;
		}

		private int getLength()
		{
			return end - start;
		}

		private int getMarker()
		{
			return marker;
		}

		private long getPTS()
		{
			return pts;
		}

		public String toString()
		{
			return ("AU " + cnt + " /frnm " + framenum + " /picord " + picorder + " /pptau " + pptau + " /ppt " + ppt + " /slicetyp " + slicetype + " /fldpic " + fieldpic + " /btmfld " + btmfield + " /ut " + unittype + " /ur " + unitref + " /pts " + pts + " /st " + start + " /en " + end);
		}

	}
}
