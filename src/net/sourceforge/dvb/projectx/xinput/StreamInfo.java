/*
 * @(#)StreamInfo
 *
 * Copyright (c) 2005-2008 by dvb.matt, All Rights Reserved. 
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

package net.sourceforge.dvb.projectx.xinput;

import net.sourceforge.dvb.projectx.common.Resource;
import net.sourceforge.dvb.projectx.common.Keys;

/**
 *
 */
public class StreamInfo extends Object {

	private Object[] videostreams;
	private Object[] audiostreams;
	private Object[] teletextstreams;
	private Object[] subpicturestreams;

	private String file_name;
	private String file_date;
	private String file_size;
	private String file_type;
	private String file_location;
	private String file_playtime;
	private String file_source;

	private String additionals;

	private int streamtype;
	private Object[] pids;
	private byte[] videoheader;

	private int[] thumbnail;

	private long scanposition = 0;

	private String line_separator = System.getProperty("line.separator");

	/**
	 *
	 */
	public StreamInfo()
	{
		setStreamInfo("", "", "", "", "", "", "");
	}

	/**
	 *
	 */
	public StreamInfo(String _file_source, String _file_type, String _file_name, String _file_location, String _file_date, String _file_size, String _file_playtime)
	{
		setStreamInfo(_file_source, _file_type, _file_name, _file_location, _file_date, _file_size, _file_playtime);
	}

	/**
	 *
	 */
	public StreamInfo(String _file_source, String _file_type, String _file_name, String _file_location, String _file_date, String _file_size, String _file_playtime, Object[] _videostreams, Object[] _audiostreams, Object[] _teletextstreams, Object[] _subpicturestreams)
	{
		setStreamInfo(_file_source, _file_type, _file_name, _file_location, _file_date, _file_size, _file_playtime, _videostreams, _audiostreams, _teletextstreams, _subpicturestreams);
	}

	/**
	 *
	 */
	public StreamInfo(int _streamtype, String _file_source, String _file_type, String _file_name, String _file_location, String _file_date, String _file_size, String _file_playtime, Object[] _videostreams, Object[] _audiostreams, Object[] _teletextstreams, Object[] _subpicturestreams, Object[] _pids, byte[] _videoheader, int[] _thumbnail)
	{
		streamtype = _streamtype;
		pids = _pids;
		videoheader = _videoheader;
		thumbnail = _thumbnail;

		setStreamInfo(_file_source, _file_type, _file_name, _file_location, _file_date, _file_size, _file_playtime, _videostreams, _audiostreams, _teletextstreams, _subpicturestreams);
	}

	/**
	 *
	 */
	public void setStreamInfo(String _file_source, String _file_type, String _file_name, String _file_location, String _file_date, String _file_size, String _file_playtime)
	{
		setStreamInfo(_file_source, _file_type, _file_name, _file_location, _file_date, _file_size, _file_playtime, null, null, null, null);
	}

	/**
	 *
	 */
	public void setStreamInfo(String _file_source, String _file_type, String _file_name, String _file_location, String _file_date, String _file_size, String _file_playtime, Object[] _videostreams, Object[] _audiostreams, Object[] _teletextstreams, Object[] _subpicturestreams)
	{
		videostreams = _videostreams;
		audiostreams = _audiostreams;
		teletextstreams = _teletextstreams;
		subpicturestreams = _subpicturestreams;

		file_name = _file_name;
		file_date = _file_date;
		file_size = _file_size;
		file_type = _file_type;
		file_location = _file_location;
		file_playtime = _file_playtime;
		file_source = _file_source;
	}

	/**
	 *
	 */
	public int[] getThumbnail()
	{
		return thumbnail;
	}

	/**
	 *
	 */
	public void setThumbnail(int[] pic)
	{
		thumbnail = new int[pic.length];
		System.arraycopy(pic, 0, thumbnail, 0, pic.length);
	}

	/**
	 *
	 */
	public String getFileName()
	{
		return file_name;
	}

	/**
	 *
	 */
	public String getFileSourceBase()
	{
		return file_source;
	}

	/**
	 *
	 */
	public String getFileSource()
	{
		return "[" + getFileSourceBase() + "]";
	}

	/**
	 *
	 */
	public String getFileSourceAndName()
	{
		return getFileSource() + " - " + getFileName();
	}

	/**
	 *
	 */
	public String getFileDate()
	{
		return file_date;
	}

	/**
	 *
	 */
	public String getFileSize()
	{
		return file_size;
	}

	/**
	 *
	 */
	public String getFileType()
	{
		return file_type;
	}

	/**
	 *
	 */
	public String getFileLocation()
	{
		return file_location;
	}

	/**
	 *
	 */
	public String getPlaytime()
	{
		return file_playtime;
	}

	/**
	 *
	 */
	public Object[] getVideoStreams()
	{
		return videostreams;
	}

	/**
	 *
	 */
	public Object[] getAudioStreams()
	{
		return audiostreams;
	}

	/**
	 *
	 */
	public Object[] getTeletextStreams()
	{
		return teletextstreams;
	}

	/**
	 *
	 */
	public Object[] getSubpictureStreams()
	{
		return subpicturestreams;
	}

	/**
	 *
	 */
	public String getVideo()
	{
		return getString(getVideoStreams());
	}

	/**
	 *
	 */
	public String getAudio()
	{
		return getString(getAudioStreams());
	}

	/**
	 *
	 */
	public String getTeletext()
	{
		return getString(getTeletextStreams());
	}

	/**
	 *
	 */
	public String getSubpicture()
	{
		return getString(getSubpictureStreams());
	}

	/**
	 *
	 */
	public String getAdditionals()
	{
		return additionals;
	}

	/**
	 *
	 */
	private String getString(Object[] obj)
	{
		String str = "";

		if (obj == null || obj.length == 0)
			return "n/a";

		str = obj[0].toString();

		for (int i = 1; i < obj.length; i++)
			str += line_separator + obj[i].toString();

		return str;
	}

	/**
	 *
	 */
	public String getFullInfo()
	{
		String str = "";

		str += Resource.getString("ScanInfo.Location") + line_separator;
		str += getFileSource() + " @ " + getFileLocation() + line_separator;
		str += Resource.getString("ScanInfo.Name") + line_separator;
		str += getFileName() + line_separator;
		str += Resource.getString("ScanInfo.Size") + line_separator;
		str += getFileSize() + line_separator;
		str += Resource.getString("ScanInfo.Date") + line_separator;
		str += getFileDate() + line_separator;
		str += line_separator;
		str += Resource.getString("ScanInfo.Type") + line_separator;
		str += getFileType() + line_separator;
		str += Resource.getString("ScanInfo.Video") + line_separator;
		str += getVideo() + line_separator;
		str += Resource.getString("ScanInfo.Audio") + line_separator;
		str += getAudio() + line_separator;
		str += Resource.getString("ScanInfo.Teletext") + line_separator;
		str += getTeletext() + line_separator;
		str += Resource.getString("ScanInfo.Subpicture") + line_separator;
		str += getSubpicture() + line_separator;
		str += Resource.getString("ScanInfo.Playtime") + line_separator;
		str += getPlaytime();

		return str;
	}

	/**
	 *
	 */
	public long getScanPosition()
	{
		return scanposition;
	}

	/**
	 *
	 */
	public void setScanPosition(long value)
	{
		scanposition = value;
	}

	/**
	 *
	 */
	public void setStreamType(int _streamtype)
	{
		streamtype = _streamtype;
		file_type = Keys.ITEMS_FileTypes[getStreamType()].toString();
	}

	/**
	 *
	 */
	public void setStreamType(int _streamtype, String str)
	{
		streamtype = _streamtype;
		file_type = "[" + getStreamSubType() + "] " + Keys.ITEMS_FileTypes[getStreamType()].toString() + " " + str;
	}

	/**
	 *
	 */
	public int getStreamType()
	{
		return (0xFF & streamtype);
	}

	/**
	 *
	 */
	public int getStreamSubType()
	{
		return (0xFF & streamtype>>>8);
	}

	/**
	 *
	 */
	public int getStreamFullType()
	{
		return streamtype;
	}

	/**
	 *
	 */
	public void setPIDs(Object[] _pids)
	{
		pids = _pids;
	}

	/**
	 *
	 */
	public int[] getPIDs()
	{
		int len = pids == null ? 0 : pids.length;

		int[] array = new int[len];

		for (int i = 0; i < len; i++) 
			array[i] = Integer.parseInt(pids[i].toString());

		return array;
	}

	/**
	 *
	 */
	public int[] getMediaPIDs()
	{
		int len = pids == null || pids.length == 0 ? 0 : pids.length - 1;

		int[] array = new int[len];

		for (int i = 0; i < len; i++) 
			array[i] = Integer.parseInt(pids[1 + i].toString());

		return array;
	}

	/**
	 *
	 */
	public void setVideoHeader(byte[] _videoheader)
	{
		if (_videoheader == null)
			videoheader = null;

		else
		{
			videoheader = new byte[12];
			System.arraycopy(_videoheader, 0, videoheader, 0, _videoheader.length);
		}
	}

	/**
	 *
	 */
	public byte[] getVideoHeader()
	{
		return videoheader;
	}

	/**
	 * 
	 */
	private Object[] copyContent(Object[] _obj)
	{
		if (_obj == null)
			return null;

		Object[] obj = new Object[_obj.length];

		System.arraycopy(_obj, 0, obj, 0, obj.length);

		return obj;
	}

	/**
	 * 
	 */
	private byte[] copyContent(byte[] _array)
	{
		if (_array == null)
			return null;

		byte[] array = new byte[_array.length];

		System.arraycopy(_array, 0, array, 0, array.length);

		return array;
	}

	/**
	 * 
	 */
	private int[] copyContent(int[] _array)
	{
		if (_array == null)
			return null;

		int[] array = new int[_array.length];

		System.arraycopy(_array, 0, array, 0, array.length);

		return array;
	}

	/**
	 *
	 */
	public StreamInfo getNewInstance()
	{
		return new StreamInfo(streamtype, file_source, file_type, file_name, file_location, file_date, file_size, file_playtime, copyContent(videostreams), copyContent(audiostreams), copyContent(teletextstreams), copyContent(subpicturestreams), copyContent(pids), copyContent(videoheader), copyContent(thumbnail));
	}

}
