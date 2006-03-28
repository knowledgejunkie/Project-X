/* Copyright (C) 1996, MPEG Software Simulation Group. All Rights Reserved. */

/*
 * Disclaimer of Warranty
 *
 * These software programs are available to the user without any license fee or
 * royalty on an "as is" basis.  The MPEG Software Simulation Group disclaims
 * any and all warranties, whether express, implied, or statuary, including any
 * implied warranties or merchantability or of fitness for a particular
 * purpose.  In no event shall the copyright-holder be liable for any
 * incidental, punitive, or consequential damages of any kind whatsoever
 * arising from the use of these programs.
 *
 * This disclaimer of warranty extends to the user of these programs and user's
 * customers, employees, agents, transferees, successors, and assigns.
 *
 * The MPEG Software Simulation Group does not represent or warrant that the
 * programs furnished hereunder are free of infringement of any third-party
 * patents.
 *
 * Commercial implementations of MPEG-1 and MPEG-2 video, including shareware,
 * are subject to royalty fees to patent holders.  Many of these patents are
 * general enough such that they are unavoidable regardless of implementation
 * design.
 *
 */
/*
 * net.sourceforge.dvb.projectx.io.BitWalker
 * 
 * Parts of MpvDecoder.java copied - especially to retrieve the picture_structure
 * from the PICTURE_CODING_EXTENSION of a picture.
 * And some other stuff in this context for further use.
 * 
 * --- cminfo ---
 *
 * Created on 27.02.2006 by Arno
 *
 */
package net.sourceforge.dvb.projectx.io;

public class BitWalker {

    private final int USER_DATA_START_CODE = 0x1B2;

    private final int EXTENSION_START_CODE = 0x1B5;

    /* extension start code IDs */
    private final int PICTURE_CODING_EXTENSION_ID = 8;

    public static final int FRAME_PICTURE = 3;

    private int BitPos = 0;

    private int BufferPos = 0;

    private long StartPos = 0;

    private byte[] buf = new byte[0];

    public BitWalker() {
        super();
    }

    public BitWalker(byte[] inpBuf, int inpStartPos) {
        super();
        setBuf(inpBuf, inpStartPos);
    }

    public void resetCounters() {
        BufferPos = (int) StartPos;
        BitPos = BufferPos << 3;
    }

    public void setStartPos(int inpStartPos) {
        StartPos = inpStartPos;
        resetCounters();
    }

    public void setBuf(byte[] inpBuf, int inpStartPos) {
        buf = inpBuf;
        setStartPos(inpStartPos);
    }

    private void skipBits(int n) {
        BitPos += n;
        BufferPos = BitPos >>> 3;
    }

    private void Flush_Bits(int n) {
        skipBits(n);
    }

    private int showBits(int n) {
        int Pos = BitPos >>> 3;
        int Val = (0xFF & buf[Pos]) << 24 | (0xFF & buf[Pos + 1]) << 16
                | (0xFF & buf[Pos + 2]) << 8 | (0xFF & buf[Pos + 3]);
        Val <<= BitPos & 7;
        Val >>>= 32 - n;
        return Val;
    }

    private int Show_Bits(int n) {
        return showBits(n);
    }

    private int popBits(int n) {
        int Val = showBits(n);
        BitPos += n;
        BufferPos = BitPos >>> 3;
        return Val;
    }

    private int Get_Bits(int n) {
        return popBits(n);
    }

    /* align to start of next next_start_code */
    private void next_start_code() {
        skipBits((8 - (BitPos & 7)) & 7);
        while (showBits(24) != 1)
            skipBits(8);
    }

    // ISO/IEC 13818-2 sections 6.3.4.1 and 6.2.2.2.2
    private void user_data() {
        skipBits(32);
        while (popBits(24) != 0x000001) {
            skipBits(8);
        }
    }

    /* decode picture coding extension */
    private int search_picture_structure() {
        // f_code:4[2][2];
        // int intra_dc_precision:2
        skipBits(18);
        int picture_structure = popBits(2);
        skipBits(8);
        int progressive_frame = popBits(1);
        if (progressive_frame == 1) {
            return FRAME_PICTURE;
        }
        return picture_structure;
    }

    /* decode extension and user data */
    /* ISO/IEC 13818-2 section 6.2.2.2 */
    private int find_picture_structure() {
        int code;

        next_start_code();

        while ((code = showBits(32)) == EXTENSION_START_CODE
                || code == USER_DATA_START_CODE) {
            if (code == EXTENSION_START_CODE) {
                skipBits(32);
                int ext_ID = popBits(4);

                switch (ext_ID) {
                case PICTURE_CODING_EXTENSION_ID:
                    return search_picture_structure();
                }
            } else {
                user_data();
            }
            next_start_code();
        }
        return FRAME_PICTURE; // not found
    }

    public int getPictureStructure(boolean inpIsPicHd) {
        int tmpStruct = FRAME_PICTURE;
        try {
            resetCounters();
            tmpStruct = find_picture_structure();
        } catch (Exception e) {
            // maybe ArrayOutOfBounds
            // e.printStackTrace();
        }
        return tmpStruct;
    }

}
