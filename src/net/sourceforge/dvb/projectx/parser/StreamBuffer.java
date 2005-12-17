/*
 * @(#)StreamBuffer.java - 
 *
 * Copyright (c) 2005 by dvb.matt, All Rights Reserved.
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

import java.io.ByteArrayOutputStream;


/**
 * stuff for pre-buffering of PID data
 */
public class StreamBuffer extends Object {

	private boolean pidstart = false;
	private boolean pidscram = false;
	private boolean need = true;

	private int ID = -1;
	private int PID = -1;
	private int demux = -1;
	private int counter = -1;

	private ByteArrayOutputStream buffer;

	public StreamBuffer()
	{
		buffer = new ByteArrayOutputStream();
	}

	public void setStarted(boolean b)
	{
		pidstart = b;
	}

	public boolean isStarted()
	{
		return pidstart;
	}

	public void setScram(boolean b)
	{ 
		pidscram = b;
	}

	public boolean getScram()
	{ 
		return pidscram; 
	}

	public void setID(int val)
	{ 
		ID = val;
	}

	public int getID()
	{ 
		return ID;
	}

	public void setCounter(int val)
	{
		counter = val;
	}

	public int getCounter()
	{
		return counter;
	}

	public void count()
	{
		counter += (counter < 15) ? 1 : -15 ;
	}

	public void countPVA()
	{
		counter += (counter < 255) ? 1 : -255 ;
	}

	public void setPID(int val)
	{
		PID = val;
	}

	public int getPID()
	{
		return PID;
	}

	public void setDemux(int val)
	{
		demux = val;
	}

	public int getDemux()
	{
		return demux;
	}

	public void setneeded(boolean b)
	{
		need = b;
	}

	public boolean isneeded()
	{
		return need;
	}

	public void writeData(byte[] data, int off, int len)
	{
		buffer.write(data, off, len);
	}

	public ByteArrayOutputStream getData()
	{
		return buffer;
	}

	public int getDataSize()
	{
		return buffer.size();
	}

	public void reset()
	{
		buffer.reset();
	}
}
