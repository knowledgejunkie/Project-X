/*
 * @(#)CRC.java - carries basic CRC funtions 
 *
 * Copyright (c) 2004 by dvb.matt. 
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
 * the needed part of CRC32 calculation was mostly derived from:
 * Description: General Encoder/Decoder for CRC-32
 *              used in MPEG-2 systems
 * Author:   Patrick Persson, Teracom Nu
 * Version:  1.0, 940203
 */

//package X

//DM10042004 081.7 int01 introduced
public final class CRC
{
	private CRC()
	{}

	public static byte[] generateCRC32(byte[] data, int offset)
	{
		// x^32 + x^26 + x^23 + x^22 + x^16 + x^12 + x^11 + x^10 + x^8 + x^7 + x^5 + x^4 + x^2 + x + 1
		int[] g = { 1,1,1,0,1,1,0,1,1,0,1,1,1,0,0,0,1,0,0,0,0,0,1,1,0,0,1,0,0,0,0,0,1 }; 

		int[] shift_reg = new int[32];
		long crc=0;
		byte crc32[] = new byte[4];

		// Initialize shift register's to '1'
		java.util.Arrays.fill(shift_reg, 1);

		// Calculate nr of data bits, summa of bits
		int nr_bits = (data.length - offset) * 8;

		for (int bit_count=0, bit_in_byte=0, data_bit; bit_count < nr_bits; bit_count++)
		{
			// Fetch bit from bitstream
			data_bit = (data[offset] & 0x80>>>(bit_in_byte++)) != 0 ? 1 : 0;

			if ((bit_in_byte &= 7) == 0)
				offset++;

			// Perform the shift and modula 2 addition
			data_bit ^= shift_reg[31];

			for (int i = 31; i > 0; i--)
				shift_reg[i] = g[i]==1 ? (shift_reg[i-1] ^ data_bit) : shift_reg[i-1];

			shift_reg[0] = data_bit;
		}

		for (int i=0; i<32; i++)
			crc = ((crc << 1) | (shift_reg[31-i]));

		for (int i=0; i<4; i++) 
			crc32[i] = (byte)(0xFF & (crc >>>((3-i) * 8)));

		return crc32;
	}

	public static int checkCRC16ofAC3(byte[] data, int offset, int len)
	{
		int[] g = { 1,0,1,0,0,0,0,0,0,0,0,0,0,0,0,1,1 }; // x^16 + x^15 + x^2 + 1

		int[] shift_reg = new int[16];
		int crc = 0, crc1 = 0;
		int nr_bits = (len-offset) * 8;
		int crc1_len = ((len>>>1) + (len>>>3) - offset) * 8;

		for (int bit_count=0, bit_in_byte=0, data_bit; bit_count < nr_bits; bit_count++)
		{
			if (bit_count == crc1_len)
			{
				crc1 = 0;
				for (int i=0; i<16; i++)
					crc1 = ((crc1 << 1) | (shift_reg[15-i]));

				if (crc1 != 0)
					return 1;
			}

			data_bit = (data[offset] & 0x80>>>(bit_in_byte++)) != 0 ? 1 : 0;

			if ((bit_in_byte &= 7) == 0)
				offset++;

			data_bit ^= shift_reg[15];

			for (int i = 15; i > 0; i--)
				shift_reg[i] = g[i]==1 ? (shift_reg[i-1] ^ data_bit) : shift_reg[i-1];

			shift_reg[0] = data_bit;
		}

		for (int i=0; i<16; i++)
			crc = ((crc << 1) | (shift_reg[15-i]));

		if (crc != 0)
			return 2;
		else
			return 0;
	}

	public static int checkCRC16ofMPA(Audio Audio, byte[] data)
	{
		if (Audio.Layer < 2 || Audio.Protection_bit==0)
			return 0;

		int crc_val = (0xFF & data[4])<<8 | (0xFF & data[5]);
		data = Audio.MPA_deleteCRC(data);

		int ch, sb, offset = 2, nr_bits = 16, BitPos[] = { 32 };

		if (Audio.Layer==3) // BAL only, of 32 subbands
		{
			for( sb=0; sb<Audio.Bound; sb++)
				for( ch=0; ch<Audio.Channel; ch++)
					nr_bits += 4;

			for( sb=Audio.Bound; sb<Audio.Sblimit; sb++)
				nr_bits += 4;
		}
		else // BAL and SCFSI, of various subbands
		{
			int table_nbal[];
			int table_alloc[][];
			int allocation[][] = new int[32][2];

			if (Audio.ID==1)
			{
				if (Audio.Sblimit > 20)
				{
					table_nbal = MPAD.table_b2ab_nbal;
					table_alloc = MPAD.table_b2ab;
				}
				else
				{
					table_nbal = MPAD.table_b2cd_nbal;
					table_alloc = MPAD.table_b2cd;
				}
			}
			else
			{
				table_nbal = MPAD.table_MPG2_nbal;
				table_alloc = MPAD.table_MPG2;
			}

			for( sb=0; sb<Audio.Bound; sb++)
			{
				for( ch=0; ch<Audio.Channel; ch++)
				{
					allocation[sb][ch] = table_alloc[sb][getBits(data, BitPos, table_nbal[sb])];
					nr_bits += table_nbal[sb];
				}
			}

			for( sb=Audio.Bound; sb<Audio.Sblimit; sb++)
			{
				allocation[sb][0] = allocation[sb][1] = table_alloc[sb][getBits(data, BitPos, table_nbal[sb])];
				nr_bits += table_nbal[sb];
			}

			for( sb=0; sb<Audio.Sblimit; sb++)
				for( ch=0; ch<Audio.Channel; ch++)
					if (allocation[sb][ch]>0)
						nr_bits += 2;
		}

		int[] g = { 1,0,1,0,0,0,0,0,0,0,0,0,0,0,0,1,1 }; // x^16 + x^15 + x^2 + 1

		int[] shift_reg = new int[16];
		int crc = 0;

		java.util.Arrays.fill(shift_reg, 1);

		for (int bit_count=0, bit_in_byte=0, data_bit; bit_count < nr_bits; bit_count++)
		{
			data_bit = (data[offset] & 0x80>>>(bit_in_byte++)) != 0 ? 1 : 0;

			if ((bit_in_byte &= 7) == 0)
				offset++;

			data_bit ^= shift_reg[15];

			for (int i = 15; i > 0; i--)
				shift_reg[i] = g[i]==1 ? (shift_reg[i-1] ^ data_bit) : shift_reg[i-1];

			shift_reg[0] = data_bit;
		}

		for (int i=0; i<16; i++)
			crc = ((crc << 1) | (shift_reg[15-i]));

		if (crc != crc_val)
			return 1;
		else
			return 0;
	}

	public static int getBits(byte buf[], int BitPos[], int N)
	{
		int Pos, Val;
		Pos = BitPos[0]>>>3;
		Val =   (0xFF & buf[Pos])<<24 | (0xFF & buf[Pos+1])<<16 |
			(0xFF & buf[Pos+2])<<8 | (0xFF & buf[Pos+3]);
		Val <<= BitPos[0] & 7;
		Val >>>= 32-N;
		BitPos[0] += N;
		return Val;
	}

}