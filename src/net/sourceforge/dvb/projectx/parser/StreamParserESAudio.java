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
import java.io.FileOutputStream;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Hashtable;

import net.sourceforge.dvb.projectx.common.Common;
import net.sourceforge.dvb.projectx.common.Resource;
import net.sourceforge.dvb.projectx.common.Keys;
import net.sourceforge.dvb.projectx.common.JobCollection;
import net.sourceforge.dvb.projectx.common.JobProcessing;

import net.sourceforge.dvb.projectx.io.StandardBuffer;

import net.sourceforge.dvb.projectx.xinput.XInputFile;
import net.sourceforge.dvb.projectx.xinput.StreamInfo;

import net.sourceforge.dvb.projectx.parser.CommonParsing;
import net.sourceforge.dvb.projectx.parser.StreamConverter;
import net.sourceforge.dvb.projectx.parser.StreamDemultiplexer;
import net.sourceforge.dvb.projectx.parser.StreamParserBase;
import net.sourceforge.dvb.projectx.parser.StreamProcess;


/**
 * main thread
 */
public class StreamParserESAudio extends StreamParserBase {

	/**
	 * 
	 */
	public StreamParserESAudio()
	{
		super();
	}

	/**
	 * check audio ES
	 */
	public String parseStream(JobCollection collection, XInputFile aXInputFile, int pes_streamtype, int action, String vptslog)
	{
		String fchild = collection.getOutputName(aXInputFile.getName());
		String fparent = collection.getOutputNameParent(fchild);

		String audio_pts = fparent + ".pts";

		CommonParsing.logAlias(collection.getJobProcessing(), vptslog, audio_pts);

		String type = "mp";

		int es_streamtype = CommonParsing.MPEG_AUDIO;

		switch (pes_streamtype)
		{
		case CommonParsing.ES_AC3_TYPE:
		case CommonParsing.ES_AC3_A_TYPE:
			type = "ac";
			es_streamtype = CommonParsing.AC3_AUDIO;
			break;

		case CommonParsing.ES_DTS_TYPE:
		case CommonParsing.ES_DTS_A_TYPE:
			type = "ac";
			es_streamtype = CommonParsing.DTS_AUDIO;
			break;

		case CommonParsing.ES_MPA_TYPE:
			type = "mp";
			es_streamtype = CommonParsing.MPEG_AUDIO;
			break;

		case CommonParsing.ES_RIFF_TYPE:
			type = "wa";
			es_streamtype = CommonParsing.WAV_AUDIO;
			break;
		}

		/**
		 * audiofile goes to synch methode 
		 */
		new StreamProcess(es_streamtype, collection, aXInputFile, audio_pts, type, vptslog, CommonParsing.ES_TYPE);

		new File(audio_pts).delete();

		return null;
	}

}
