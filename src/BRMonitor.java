/*
 * @(#)BR_MONITOR.java
 *
 * Copyright (c) 2002-2004 by dvb.matt, All Rights Reserved.
 * 
 * This file is part of X, a free Java based demux utility.
 * X is intended for educational purposes only, as a non-commercial test project.
 * It may not be used otherwise. Most parts are only experimental.
 * 
 *
 * This program is free software; you can redistribute it free of charge
 * and/or modify it under the terms of the GNU General Public License as published by
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


import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;


public class BRMonitor extends JPanel {

public Surface surf;

public BRMonitor() {
	setLayout(new BorderLayout());
	setVisible(true);
	add(surf = new Surface());
}

public class Surface extends JPanel implements Runnable {

	public Thread thread;
	public long sleepAmount = 10000;
	private int w, h;
	private BufferedImage bimg;
	private Graphics2D big;
	private Font font = new Font("Times New Roman", Font.PLAIN, 11);
	private int columnInc;
	private int pts[];
	private int ptNum;
	private int ascent, descent;
	private int bitrate, maxbitrate=0, minbitrate=37500;
	private Rectangle graphOutlineRect = new Rectangle();
	private Color graphColor = new Color(46, 139, 87);
	private String usedStr;
	private String timeStr="00:00:00";
	private boolean first = true, greatgop = false;
	private Color[] GOP = { Color.black, Color.cyan, Color.magenta, Color.white, Color.green, Color.red, Color.red, Color.red, Color.yellow };
	private byte[] frame = new byte[0];

	public Surface() { 
		setBackground(Color.black);
		setVisible(true);
	}

	public Dimension getMinimumSize() { return getPreferredSize(); }

	public Dimension getMaximumSize() { return getPreferredSize(); }

	public Dimension getPreferredSize() { return new Dimension(54,72); }

	//DM18052004 081.7 int02 changed
	public void paint(Graphics g)
	{
		if (big == null) 
			return;

		g.drawImage(bimg, 0, 0, this);
	}

	//DM18052004 081.7 int02 changed
	public void refresh()
	{
		if (big == null) 
			return;

		big.setBackground(getBackground());
		big.clearRect(0,0,w,h);

		// .. Draw bitrate string ..
		big.setColor(Color.white);
		usedStr = String.valueOf(bitrate*400/1000) + "kbps ";
		big.drawString(usedStr, 4, h-descent-10-descent);
		big.drawString(timeStr, 4, h-descent);

		// .. Draw History Graph ..
		big.setColor(graphColor);
		int graphX = 2;
		int graphY = 2;
		int graphW = w - graphX - 2;   // w =55
		int graphH = h - descent - 34;  // h=55. 48
		graphOutlineRect.setRect(graphX, graphY, graphW, graphH);
		big.draw(graphOutlineRect);

		int graphRow = graphH/5;

		// .. Draw row ..
		for (int j = graphY; j <= graphH+graphY; j += graphRow)
			big.drawLine(graphX,j,graphX+graphW,j);
        
		// .. Draw animated column movement ..
		int graphColumn = graphW/5;

		for (int j = graphX; j < graphW+graphX; j+=graphColumn)
			big.drawLine(j,graphY,j,graphY+graphH);

		//.. 9Mbps
		big.setColor(Color.red);
		big.drawLine(2,5,52,5);

		//.. 2.5Mbps
		big.setColor(Color.magenta);
		big.drawLine(2,28,52,28);

		//.. max,min Mbps
		big.setColor(Color.white);
		big.drawLine(52,(graphY+graphH-(maxbitrate/695)),56,(graphY+graphH-(maxbitrate/695)));
		big.drawLine(52,(graphY+graphH-(minbitrate/695)),56,(graphY+graphH-(minbitrate/695)));

		//.. frametypegraph
		int b = (frame.length>17) ? 17 : frame.length;
		for (int a=0; a<b; a++) {
			big.setColor(GOP[0xF&frame[a]]);
			if ((0x80&frame[a])!=0) {          // progressive frame
				big.fillRect(2+(a*3),40,2,5);
			} else {                             // interlaced frame
				big.fillRect(2+(a*3),40,2,2);
				big.fillRect(2+(a*3),43,2,2);
			}
		}

		if (frame.length>16)
			greatgop=true;

		if (greatgop) {
			big.setColor(Color.red);
			big.fillRect(52,40,2,5);
		}

		if (pts == null) {
			pts = new int[graphW];
			ptNum = 0;
		} else {
			big.setColor(Color.yellow);
			pts[ptNum] = ( graphY + graphH - (bitrate/695) );
			if (pts[ptNum]<1) 
				pts[ptNum]=0;
			for (int j=graphX+graphW-ptNum, k=0;k < ptNum; k++, j++) {
				if (k != 0) {
					if (pts[k] != pts[k-1])
						big.drawLine(j-1, pts[k-1], j, pts[k]);
					else
						big.fillRect(j, pts[k], 1, 1);
				}
			}
			if (ptNum+2 == pts.length) {
				// throw out oldest point
				for (int j = 1;j < ptNum; j++)
					pts[j-1] = pts[j];
				--ptNum;
			} else
				ptNum++;
		}
	}


	public void start() {
		greatgop=false;
		maxbitrate=0;
		minbitrate=37500;
		timeStr="00:00:00";
		bitrate=0;
		frame = new byte[0];
		thread = new Thread(this);
		thread.setPriority(Thread.MIN_PRIORITY);
		thread.setName("BRMonitor");
		thread.start();
	}


	public synchronized void stop() {
		thread = null;
		notify();
	}

	public void update(int Bitrate, byte[] frame1, String vtime) { 
		bitrate = Bitrate;
		if (bitrate > maxbitrate)
			maxbitrate = bitrate;
		if (bitrate < minbitrate)
			minbitrate = bitrate;
		timeStr = vtime;
		frame = frame1;

		refresh(); //DM18052004 081.7 int02 add

		repaint();
	}

	public void run() {

		Thread me = Thread.currentThread();

		//while (thread == me && isShowing()) {
		while (thread == me) {
			Dimension d = getSize();
			if (d.width != w || d.height != h) {
				w = d.width;
				h = d.height;
				bimg = (BufferedImage) createImage(w, h);
				big = bimg.createGraphics();
				big.setFont(font);
				FontMetrics fm = big.getFontMetrics(font);
				ascent = (int) fm.getAscent();
				descent = (int) fm.getDescent();
			}

			if (first) {
				refresh();
				repaint(); 
				first=false;
			}


			try {
				thread.sleep(sleepAmount);
			}
			catch (InterruptedException e) { 
				break;
			}
		}
		thread = null;
	}

}
}
