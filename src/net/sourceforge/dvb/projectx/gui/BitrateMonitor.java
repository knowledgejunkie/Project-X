/*
 * @(#)BitrateMonitor.java
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

/*
 * the base code was derived from MemoryMonitor.java	1.26 99/04/23
 *
 * Copyright (c) 1998, 1999 by Sun Microsystems, Inc. All Rights Reserved.
 * 
 * Sun grants you ("Licensee") a non-exclusive, royalty free, license to use,
 * modify and redistribute this software in source and binary code form,
 * provided that i) this copyright notice and license appear on all copies of
 * the software; and ii) Licensee does not utilize the software in a manner
 * which is disparaging to Sun.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING ANY
 * IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN AND ITS LICENSORS SHALL NOT BE
 * LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING
 * OR DISTRIBUTING THE SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR ITS
 * LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT,
 * INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER
 * CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF
 * OR INABILITY TO USE SOFTWARE, EVEN IF SUN HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGES.
 * 
 * This software is not designed or intended for use in on-line control of
 * aircraft, air traffic, aircraft navigation or aircraft communications; or in
 * the design, construction, operation or maintenance of any nuclear
 * facility. Licensee represents and warrants that it will not use or
 * redistribute the Software for such purposes.
 */

package net.sourceforge.dvb.projectx.gui;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;


public class BitrateMonitor extends JPanel {

	private int w = 110;
	private int h = 32;
	private int divisor = 695;
	private BufferedImage bimg;
	private Graphics2D big;
	private Font font = new Font("Times New Roman", Font.PLAIN, 11);
	private int columnInc;
	private int pts[];
	private int ptNum;
	private int ascent;
	private int descent;
	private int bitrate;
	private int maxbitrate = 0;
	private int minbitrate = 37500;
	private Rectangle graphOutlineRect = new Rectangle();
	private Color graphColor = new Color(46, 139, 87);
	private String usedStr;
	private String timeStr="00:00:00";
	private boolean first = true;
	private boolean greatgop = false;
	private Color[] GOP = { Color.black, Color.cyan, Color.magenta, Color.white, Color.green, Color.red, Color.red, Color.red, Color.yellow };
	private byte[] frame = new byte[0];
	private int maxbitrate_index = 22500;

	/**
	 *
	 */
	public BitrateMonitor()
	{ 
		bimg = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		big = bimg.createGraphics();
		big.setFont(font);

		FontMetrics fm = big.getFontMetrics(font);
		ascent = (int) fm.getAscent();
		descent = (int) fm.getDescent();

		reset();

		setLayout(new BorderLayout());
		setBackground(Color.black);
		setVisible(true);
	}

	/**
	 *
	 */
	public Dimension getMinimumSize()
	{
		return getPreferredSize();
	}

	/**
	 *
	 */
	public Dimension getMaximumSize()
	{ 
		return getPreferredSize();
	}

	/**
	 *
	 */
	public Dimension getPreferredSize()
	{
		return new Dimension(w, h);
	}

	/**
	 *
	 */
	public void paint(Graphics g)
	{
		if (big == null) 
			return;

		g.drawImage(bimg, 0, 0, this);
	}

	/**
	 *
	 */
	private void refresh()
	{
		if (big == null) 
			return;

		big.setColor(Color.black);
		big.clearRect(0, 0, w, h);

		// .. Draw bitrate string ..
		big.setColor(Color.white);
		usedStr = String.valueOf(bitrate * 400 / 1000) + "kbps ";
		big.drawString(usedStr, 60, h - descent - 9 - descent);
		big.drawString(timeStr, 60, h - descent);

		// .. Draw History Graph ..
		int graphX = 2;
		int graphY = 1;
		int graphW = 50;   // w =55
		int graphH = 30;  // h=55. 48

		big.setColor(graphColor);
		graphOutlineRect.setRect(graphX, graphY, graphW, graphH);
		big.draw(graphOutlineRect);

		int graphRow = graphH / 5;

		big.setColor(graphColor);

		// .. Draw row ..
		for (int j = graphY; j <= graphH + graphY; j += graphRow)
			big.drawLine(graphX, j, graphX + graphW, j);
        
		// .. Draw animated column movement ..
		int graphColumn = graphW / 5;

		for (int j = graphX; j < graphW + graphX; j += graphColumn)
			big.drawLine(j, graphY, j, graphY + graphH);

		//.. frametypegraph
		int b = frame.length > 17 ? 17 : frame.length;

		for (int a = 0; a < b; a++)
		{
			big.setColor(GOP[0xF & frame[a]]);

			if ((0x80 & frame[a]) != 0)
			{          // progressive frame
				big.fillRect(60 + (a * 3), 2, 2, 5);
			}
			else
			{                             // interlaced frame
				big.fillRect(60 + (a * 3), 2, 2, 2);
				big.fillRect(60 + (a * 3), 5, 2, 2);
			}
		}

		if (frame.length > 16)
			greatgop = true;

		if (greatgop)
		{
			big.setColor(Color.red);
			big.fillRect(52, 40, 2, 5);
		}

		if (pts == null)
		{
			pts = new int[graphW];
			ptNum = 0;
		}
		else
		{
			big.setColor(Color.yellow);
			pts[ptNum] = ( graphY + graphH - (bitrate / divisor) );

			if (pts[ptNum] < 1) 
				pts[ptNum] = 0;

			for (int j = graphX + graphW - ptNum, k = 0; k < ptNum; k++, j++)
				if (k != 0)
					big.drawLine(j - 1, graphH, j - 1, pts[k]);

			if (ptNum + 2 == pts.length)
			{
				// throw out oldest point
				for (int j = 1; j < ptNum; j++)
					pts[j - 1] = pts[j];

				--ptNum;
			}
			else
				ptNum++;
		}

		//.. 9Mbps
		big.setColor(Color.red);
		big.drawLine(2, 5, 52, 5);

		//.. 2.5Mbps
		big.setColor(Color.magenta);
		big.drawLine(2, 28, 52, 28);

		//.. max,min Mbps
		big.setColor(Color.white);
		big.drawLine(52, (graphY + graphH - (maxbitrate / divisor)), 56, (graphY + graphH - (maxbitrate / 695)));
		big.drawLine(52, (graphY + graphH - (minbitrate / divisor)), 56, (graphY + graphH - (minbitrate / 695)));
	}

	/**
	 *
	 */
	public void reset()
	{
		greatgop = false;
		maxbitrate = 0;
		minbitrate = 37500;
		timeStr = "00:00:00";
		bitrate = 0;
		frame = new byte[0];

		refresh();
		repaint();
	}

	/**
	 *
	 */
	public void update(int _bitrate, byte[] _frames, String str)
	{ 
		bitrate = _bitrate;

		if (bitrate > maxbitrate)
			maxbitrate = bitrate;

		if (bitrate < minbitrate)
			minbitrate = bitrate;

		timeStr = str;
		frame = _frames;

		refresh();
		repaint();
	}

}
