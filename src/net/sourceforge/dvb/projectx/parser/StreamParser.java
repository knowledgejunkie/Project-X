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

import net.sourceforge.dvb.projectx.common.JobCollection;

import net.sourceforge.dvb.projectx.xinput.XInputFile;

import net.sourceforge.dvb.projectx.parser.CommonParsing;
import net.sourceforge.dvb.projectx.parser.StreamParserBase;
import net.sourceforge.dvb.projectx.parser.StreamParserPVA;
import net.sourceforge.dvb.projectx.parser.StreamParserTS;
import net.sourceforge.dvb.projectx.parser.StreamParserPESPrimary;
import net.sourceforge.dvb.projectx.parser.StreamParserPESSecondary;

/**
 * main thread
 */
public class StreamParser extends Object {

	private StreamParserBase impl = null;

	/**
	 * 
	 */
	public StreamParser(int parser_type)
	{
		switch (parser_type)
		{
		case CommonParsing.SECONDARY_PES_PARSER:
			impl = new StreamParserPESSecondary();
			break;

		case CommonParsing.PRIMARY_PES_PARSER:
			impl = new StreamParserPESPrimary();
			break;

		case CommonParsing.TS_PARSER:
			impl = new StreamParserTS();
			break;

		case CommonParsing.PVA_PARSER:
			impl = new StreamParserPVA();
			break;

		case CommonParsing.ES_VIDEO_PARSER:
			impl = new StreamParserESVideo();
			break;

		case CommonParsing.ES_AUDIO_PARSER:
			impl = new StreamParserESAudio();
			break;

		case CommonParsing.ES_SUBPICTURE_PARSER:
			impl = new StreamParserESSubpicture();
			break;

		default:
			impl = new StreamParserBase();
		}
	}

	/**
	 * 
	 */
	public String parseStream(JobCollection collection, XInputFile aXInputFile, int pes_streamtype, int action, String vptslog)
	{
		return (impl != null ? impl.parseStream(collection, aXInputFile, pes_streamtype, action, vptslog) : null);
	}
}
