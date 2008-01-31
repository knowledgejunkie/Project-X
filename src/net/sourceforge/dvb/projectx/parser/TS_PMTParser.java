/*
 * @(#)
 *
 * Copyright (c) 2002-2005 by dvb.matt, All Rights Reserved. 
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
import java.util.ArrayList;

import net.sourceforge.dvb.projectx.common.Common;
import net.sourceforge.dvb.projectx.parser.CommonParsing;

public class TS_PMTParser extends Object {

	private int last_version_number = -1;

	private int service_id = -1;
	private int pmt_pid = -1;
	private int pcr_pid = -1;

	private int pid_mask = 0x1FFF;
	private int pid_mask_inv = 0xE000;
	private int length_mask = 0xFFF;
	private int length_mask_inv = 0xF000;

	private ArrayList video_streams = null; // more than one
	private ArrayList audio_streams = null; // more than one
	private ArrayList teletext_streams = null; // more than one
	private ArrayList subtitle_streams = null; // more than one

	/**
	 *
	 */
	public TS_PMTParser(int _service_id, int _pmt_pid)
	{
		pmt_pid = _pmt_pid;
		service_id = _service_id;

//Common.setMessage("XX " + pmt_pid + " / " + service_id);
	}

	/**
	 *
	 */
	public int getPID()
	{
		return pmt_pid;
	}

	/**
	 *
	 */
	public int getServiceID()
	{
		return service_id;
	}

	/**
	 *
	 */
	public int parsePMT(byte[] packet)
	{ 
		if (packet.length < 6)
			return -1;

//String str = "";
//for (int i = 0; i < packet.length; i++)
//	str += " " + Common.adaptString(Integer.toHexString(0xFF & packet[i]), 2);
//Common.setMessage("FF " + str);

		int table_id = getValue(packet, 0, 2);
		int tmp_value = getValue(packet, 2, 1);

		//id + marker
		if (table_id != 2 || (0xF0 & tmp_value) != 0xB0)
			return -2;

		int section_length = length_mask & getValue(packet, 2, 2);

		if (section_length > packet.length - 4)
			section_length = packet.length - 4;

		int program_number = getValue(packet, 4, 2);

		if (service_id != program_number)
			return -3;

		tmp_value = getValue(packet, 6, 1);

		//marker
		if ((0xC0 & tmp_value) != 0xC0)
			return -4;

		int version_number = 0x1F & tmp_value>>1;

		if (last_version_number >= 0 && last_version_number == version_number) // 0 .. 31, +1 whenever a change occur
			return -5;

		int current_next_indicator = tmp_value & 1;

		if (current_next_indicator == 0) // 1 = current applicable table, 0 announces next
			return -6;

		int section_number = getValue(packet, 7, 1);  // shall be 0
		int last_section_number = getValue(packet, 8, 1);  // shall be 0

		tmp_value = getValue(packet, 9, 2);

		//marker
		if ((tmp_value & pid_mask_inv) != pid_mask_inv)
			return -7;

		pcr_pid = tmp_value & pid_mask;

		tmp_value = getValue(packet, 11, 2);

		//marker
		if ((tmp_value & length_mask_inv) != length_mask_inv)
			return -8;

		int program_info_length = length_mask & tmp_value;
		int packet_offset = 13 + program_info_length;

		for (int streamtype, pid; packet_offset < section_length + 4 && packet_offset < packet.length; )
		{
			streamtype = getValue(packet, packet_offset++, 1);
			tmp_value = getValue(packet, (packet_offset += 2), 2);

			//marker
			if ((tmp_value & pid_mask_inv) != pid_mask_inv)
				continue;

			pid = pid_mask & tmp_value;

			tmp_value = getValue(packet, (packet_offset += 2), 2);

			//marker
			if ((tmp_value & length_mask_inv) != length_mask_inv)
				continue;

			program_info_length = length_mask & tmp_value;

			switch(streamtype)
			{
			case 0x01: //mpv1
			case 0x02: //mpv2
				getDescriptor(packet, packet_offset, program_info_length, pid, 2);
				break; 

			case 0x03: //mpa1
			case 0x04: //mpa2
				getDescriptor(packet, packet_offset, program_info_length, pid, 4);
				break; 

			case 0x1B: //h264
				getDescriptor(packet, packet_offset, program_info_length, pid, 0x1B);
				break; 

			case 0x80:
			case 0x81:  //private data of AC3 in ATSC
			case 0x82: 
			case 0x83: 
			case 0x06:  //private data in DVB
				getDescriptor(packet, packet_offset, program_info_length, pid, 6);
				break; 
			}

			packet_offset += program_info_length;
		}

		last_version_number = version_number;

		return 0;
	}

	/**
	 *
	 */
	private void getDescriptor(byte check[], int off, int len, int pid, int type)
	{
		String str = "";
		int chunk_end = 0;
		int end = off + len;

		try
		{
			loop:
			for (; off < end && off < check.length; off++)
			{
				switch(0xFF & check[off])
				{
				case 0x59:  //dvb subtitle descriptor
					type = 0x59;
					chunk_end = off + 2 + (0xFF & check[off+1]);
					str += "(";

					for (int a=off+2; a<chunk_end; a+=8)
					{
						for (int b=a; b<a+3; b++) //language
							str += (char)(0xFF & check[b]);

						int page_type = 0xFF & check[a+3];
						int comp_page_id = (0xFF & check[a+4])<<16 | (0xFF & check[a+5]);
						int anci_page_id = (0xFF & check[a+6])<<16 | (0xFF & check[a+7]);

						str += "_0x" + Integer.toHexString(page_type).toUpperCase();
						str += "_p" + comp_page_id;
						str += "_a" + anci_page_id + " ";
					}

					str += ")";
					break loop;

				case 0x56:  //teletext descriptor incl. index page + subtitle pages
					type = 0x56;
					chunk_end = off + 2 + (0xFF & check[off+1]);
					str += "(";

					for (int a=off+2; a<chunk_end; a+=5)
					{
						for (int b=a; b<a+3; b++) //language
							str += (char)(0xFF & check[b]);

						int page_type = (0xF8 & check[a+3])>>>3;
						int page_number = 0xFF & check[a+4];

						str += "_";

						switch (page_type)
						{
						case 1:
							str += "i";
							break;
						case 2:
							str += "s";
							break;
						case 3:
							str += "ai";
							break;
						case 4:
							str += "ps";
							break;
						case 5:
							str += "s.hip";
							break;
						default:
							str += "res";
						}

						str += Integer.toHexString((7 & check[a+3]) == 0 ? 8 : (7 & check[a+3])).toUpperCase();
						str += (page_number < 0x10 ? "0" : "") + Integer.toHexString(page_number).toUpperCase() + " ";
					}

					str += ")";
					//break loop;
					off++;
					off += (0xFF & check[off]);
					break;

				case 0xA:  //ISO 639 language descriptor
					str += "(";

					for (int a=off+2; a<off+5; a++)
						str += (char)(0xFF & check[a]);

					str += ")";
					off++;
					off += (0xFF & check[off]);
					break;

				case 0x6A:  //ac3 descriptor
					str += "(AC-3)";
					off++;
					off += (0xFF & check[off]);
					break;

				case 0xC3:  //VBI descriptor
					off++;

					switch (0xFF & check[off + 1])
					{
					case 4:
						str += "(VPS)";
						type = 0xC3;
						break;
					case 5:
						str += "(WSS)";
						break;
					case 6:
						str += "(CC)";
						break;
					case 1:
						str += "(EBU-TTX)";
						break;
					case 7:
						str += "(VBI)";
						break;
					}

					off += (0xFF & check[off]);
					break;

				case 0x52:  //ID of service
					chunk_end = off + 2 + (0xFF & check[off+1]);
					str += "(#" + (0xFF & check[off + 2]) + ")";
					off++;
					off += (0xFF & check[off]);
					break;

				case 0x6B:  //ancillary desc
					chunk_end = off + 2 + (0xFF & check[off + 1]);
					str += "(RDS)";
					off++;
					off += (0xFF & check[off]);
					break;

				case 0x5:  //registration descriptor
					chunk_end = off + 2 + (0xFF & check[off+1]);
					str += "(";

					for (int a=off+2; a<chunk_end; a++)
						str += (char)(0xFF & check[a]);

					str += ")";
	
				default:
					off++;
					off += (0xFF & check[off]);
				}
			}

			String out = "PID: 0x" + Integer.toHexString(pid).toUpperCase();

			switch (type)
			{
			case 0x59:
			//	subtitle_streams.add(out + str);
				subtitle_streams.add(String.valueOf(pid));
				break;

			case 0x56:
			//	teletext_streams.add(out + str);
				teletext_streams.add(String.valueOf(pid));
				break;

			case 0x1B:
			//	video_streams.add(out + str + "(H.264)");
				break;

			case 2:
			//	video_streams.add(out + str);
				video_streams.add(String.valueOf(pid));
				break;

			case 0xC3:
			//	video_streams.add(out + str);
				break;

			case 4:
			//	audio_streams.add(out + str);
				audio_streams.add(String.valueOf(pid));
				break;

			default:
			//	audio_streams.add(out + str + "[PD]");
				audio_streams.add(String.valueOf(pid));
			}

		}
		catch (ArrayIndexOutOfBoundsException ae)
		{
			//playtime += msg_6;
		}

	}

	private int getValue(byte[] array, int offset, int length)
	{
		int value = 0;

		for (int i = 0, j = offset + length - 1; i < length; i++)
			value |= (0xFF & array[j - i])<<(i * 8);

		return value;
	}
}
