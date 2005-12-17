/*
 * @(#)StringCommandListener.java - for ftp access
 *
 * Copyright (c) 2004-2005 by roehrist, All Rights Reserved. 
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
 * requires Jakarta Commons Net library, developed by the
 * Apache Software Foundation (http://www.apache.org/).
 */

package net.sourceforge.dvb.projectx.xinput.ftp;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.commons.net.ProtocolCommandEvent;
import org.apache.commons.net.ProtocolCommandListener;

/*******************************************************************************
 * This is a support class for some of the example programs. It is a sample
 * implementation of the ProtocolCommandListener interface which just prints out
 * to a specified stream all command/reply traffic.
 * <p>
 ******************************************************************************/

class StringCommandListener implements ProtocolCommandListener {

	private StringWriter sWriter = new StringWriter();

	private PrintWriter pWriter = new PrintWriter(sWriter);

	public void protocolCommandSent(ProtocolCommandEvent event) {
		pWriter.print(event.getMessage());
		pWriter.flush();
	}

	public void protocolReplyReceived(ProtocolCommandEvent event) {
		pWriter.print(event.getMessage());
		pWriter.flush();
	}

	public String getMessages() {
		return sWriter.toString();
	}

	public void reset() {
		pWriter.close();
		try {
			sWriter.close();
		} catch (IOException e) {
			// do nothing
		}

		sWriter = new StringWriter();
		pWriter = new PrintWriter(sWriter);
	}
}