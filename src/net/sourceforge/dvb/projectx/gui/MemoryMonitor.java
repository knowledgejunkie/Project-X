/*
 * @(#)MemoryMonitor.java	1.26 99/04/23
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

/**
 * 05/07/31, added sequential system.gc() call
 */

package net.sourceforge.dvb.projectx.gui;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.Date;
import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import net.sourceforge.dvb.projectx.common.Common;


/**
 * Tracks Memory allocated & used, displayed in graph form.
 */
public class MemoryMonitor extends JPanel {

    public Surface surf;
    JPanel controls;
    boolean doControls;
    JTextField tf;
    JCheckBox box;

    public MemoryMonitor() {
        setLayout(new BorderLayout());
        setBorder(new TitledBorder(new EtchedBorder(), "Memory Monitor"));
        add(surf = new Surface());
        controls = new JPanel();
        controls.setToolTipText("click to start/stop monitoring + memory saving");
        Font font = new Font("serif", Font.PLAIN, 10);
        JLabel label;
        tf = new JTextField("1000");
        tf.setPreferredSize(new Dimension(40,14));
        controls.add(tf);
        controls.add(label = new JLabel("ms"));

        box = new JCheckBox("call gc()");
        box.setPreferredSize(new Dimension(80,14));
        box.setSelected(true);
        controls.add(box);

        controls.setPreferredSize(new Dimension(100, 36));
        controls.setMaximumSize(new Dimension(100, 36));
        label.setFont(font);
        label.setForeground(Color.black);
        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e)
			{
               removeAll();

                if ((doControls = !doControls))
				{
                   surf.stop();
                   add(controls);
				}
				else
				{
                   try { 
					long val = Long.parseLong(tf.getText().trim());

					if (val >= 50)
						surf.sleepAmount = val;
                   } catch (Exception ex) {}

                   surf.start();
                   add(surf);
				}
				validate();
				repaint();
            }
        });
    }


    public class Surface extends JPanel implements Runnable {

        public Thread thread;
        public long sleepAmount = 1000;
        private int w, h;
        private BufferedImage bimg;
        private Graphics2D big;
        private Font font = new Font("Times New Roman", Font.PLAIN, 11);
        private Runtime r = Runtime.getRuntime();
        private int columnInc;
        private int pts[];
        private int ptNum;
        private int ascent, descent;
        private float freeMemory, totalMemory;
        private Rectangle graphOutlineRect = new Rectangle();
        private Rectangle2D mfRect = new Rectangle2D.Float();
        private Rectangle2D muRect = new Rectangle2D.Float();
        private Line2D graphLine = new Line2D.Float();
        private Color graphColor = new Color(46, 139, 87);
        private Color mfColor = new Color(0, 100, 0);
        private String usedStr;

		private int gc_counter = 0;

        public Surface() {
            setBackground(Color.black);
            addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (thread == null) start(); else stop();
                }
            });
        }

        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        public Dimension getPreferredSize() {
            return new Dimension(100, 36);
        }

            
        public void paint(Graphics g) {

            if (big == null) {
                return;
            }

            big.setBackground(getBackground());
            big.clearRect(0,0,w,h);

            float freeMemory = (float) r.freeMemory();
            float totalMemory = (float) r.totalMemory();

            // .. Draw allocated and used strings ..
            big.setColor(Color.green);
            big.drawString(String.valueOf((int) totalMemory/1024) + "K allocated",  4.0f, (float) ascent+0.5f);

            usedStr = String.valueOf(((int) (totalMemory - freeMemory))/1024) + "K used";
            big.drawString(usedStr, 4, 0.5f + ascent * 2);

			float maxmem = 82;
			float usedmem = ((totalMemory - freeMemory) * maxmem) / totalMemory;

            // .. Memory Used ..
            big.drawRect(4, h - 10, (int)maxmem, 6);

            big.setColor(new Color(0, 150, 0));
			big.fillRect(5, h - 9, (int)usedmem - 2, 5);

			if (gc_counter > 4)
			{
				if (thread != null && box.isSelected() && freeMemory < 2048000L)
				{
					big.setColor(Color.red);
					big.fillRect(90, h - 9, 4, 4);

					System.gc();
				}

				gc_counter = 0;
			}
			else
				gc_counter++;

            g.drawImage(bimg, 0, 0, this);
        }


        public void start() {
            thread = new Thread(this);
            thread.setPriority(Thread.MIN_PRIORITY);
            thread.setName("MemoryMonitor");
            thread.start();
        }


        public synchronized void stop() {
            thread = null;
            notify();
        }


        public void run() {

            Thread me = Thread.currentThread();

            while (thread == me && !isShowing() || getSize().width == 0) {
                try {
                    thread.sleep(500);
                } catch (InterruptedException e) { return; }
            }
    
            while (thread == me && isShowing()) {
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

                repaint();

                try {
                    thread.sleep(sleepAmount);
                } catch (InterruptedException e) { break; }
            }
            thread = null;
        }
    }
}
