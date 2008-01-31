/*
 * @(#)URLDecoder.java	1.23 03/01/23
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

//package java.net;
package net.sourceforge.dvb.projectx.net;

import java.io.*;
import java.net.*;

/**
 * Utility class for HTML form decoding. This class contains static methods
 * for decoding a String from the <CODE>application/x-www-form-urlencoded</CODE>
 * MIME format.
 * <p>
 * To conversion process is the reverse of that used by the URLEncoder class. It is assumed
 * that all characters in the encoded string are one of the following: 
 * &quot;<code>a</code>&quot; through &quot;<code>z</code>&quot;,
 * &quot;<code>A</code>&quot; through &quot;<code>Z</code>&quot;,
 * &quot;<code>0</code>&quot; through &quot;<code>9</code>&quot;, and
 * &quot;<code>-</code>&quot;, &quot;<code>_</code>&quot;,
 * &quot;<code>.</code>&quot;, and &quot;<code>*</code>&quot;. The
 * character &quot;<code>%</code>&quot; is allowed but is interpreted
 * as the start of a special escaped sequence.
 * <p>
 * The following rules are applied in the conversion:
 * <p>
 * <ul>
 * <li>The alphanumeric characters &quot;<code>a</code>&quot; through
 *     &quot;<code>z</code>&quot;, &quot;<code>A</code>&quot; through
 *     &quot;<code>Z</code>&quot; and &quot;<code>0</code>&quot; 
 *     through &quot;<code>9</code>&quot; remain the same.
 * <li>The special characters &quot;<code>.</code>&quot;,
 *     &quot;<code>-</code>&quot;, &quot;<code>*</code>&quot;, and
 *     &quot;<code>_</code>&quot; remain the same. 
 * <li>The plus sign &quot;<code>+</code>&quot; is converted into a
 *     space character &quot;<code>&nbsp;</code>&quot; .
 * <li>A sequence of the form "<code>%<i>xy</i></code>" will be
 *     treated as representing a byte where <i>xy</i> is the two-digit
 *     hexadecimal representation of the 8 bits. Then, all substrings
 *     that contain one or more of these byte sequences consecutively
 *     will be replaced by the character(s) whose encoding would result
 *     in those consecutive bytes. 
 *     The encoding scheme used to decode these characters may be specified, 
 *     or if unspecified, the default encoding of the platform will be used.
 * </ul>
 * <p>
 * There are two possible ways in which this decoder could deal with
 * illegal strings.  It could either leave illegal characters alone or
 * it could throw an <tt>{@link java.lang.IllegalArgumentException}</tt>.
 * Which approach the decoder takes is left to the
 * implementation.
 *
 * @author  Mark Chamness
 * @author  Michael McCloskey
 * @version 1.23, 01/23/03
 * @since   1.2
 */

public class X_URLDecoder {

    // The platform default encoding
    //static String dfltEncName = "UTF-8"URLEncoder.dfltEncName;
    static String dfltEncName = null;

    /**
     * Decodes a <code>x-www-form-urlencoded</code> string.
     * The platform's default encoding is used to determine what characters 
     * are represented by any consecutive sequences of the form 
     * "<code>%<i>xy</i></code>".
     * @param s the <code>String</code> to decode
     * @deprecated The resulting string may vary depending on the platform's
     *          default encoding. Instead, use the decode(String,String) method
     *          to specify the encoding.
     * @return the newly decoded <code>String</code>
     */
    public static String decode(String s) {

	String str = null;

	try {
	    str = decode(s, dfltEncName);
	} catch (UnsupportedEncodingException e) {
	    // The system should always have the platform default
	}

	return str;
    }

    /**
     * Decodes a <code>application/x-www-form-urlencoded</code> string using a specific 
     * encoding scheme.
     * The supplied encoding is used to determine
     * what characters are represented by any consecutive sequences of the
     * form "<code>%<i>xy</i></code>".
     * <p>
     * <em><strong>Note:</strong> The <a href=
     * "http://www.w3.org/TR/html40/appendix/notes.html#non-ascii-chars">
     * World Wide Web Consortium Recommendation</a> states that
     * UTF-8 should be used. Not doing so may introduce
     * incompatibilites.</em>
     *
     * @param s the <code>String</code> to decode
     * @param enc   The name of a supported 
     *    <a href="../lang/package-summary.html#charenc">character
     *    encoding</a>. 
     * @return the newly decoded <code>String</code>
     * @exception  UnsupportedEncodingException
     *             If the named encoding is not supported
     * @see URLEncoder#encode(java.lang.String, java.lang.String)
     * @since 1.4
     */
    public static String decode(String s, String enc) 
	throws UnsupportedEncodingException{
	
	boolean needToChange = false;
	StringBuffer sb = new StringBuffer();
	int numChars = s.length();
	int i = 0;

	if (enc.length() == 0) {
	    throw new UnsupportedEncodingException ("URLDecoder: empty string enc parameter");
	}

	while (i < numChars) {
            char c = s.charAt(i);
            switch (c) {
	    case '+':
		sb.append(' ');
		i++;
		needToChange = true;
		break;
	    case '%':
		/*
		 * Starting with this instance of %, process all
		 * consecutive substrings of the form %xy. Each
		 * substring %xy will yield a byte. Convert all
		 * consecutive  bytes obtained this way to whatever
		 * character(s) they represent in the provided
		 * encoding.
		 */

		try {

		    // (numChars-i)/3 is an upper bound for the number
		    // of remaining bytes
		    byte[] bytes = new byte[(numChars-i)/3];
		    int pos = 0;
		    
		    while ( ((i+2) < numChars) && 
			    (c=='%')) {
			bytes[pos++] = 
			    (byte)Integer.parseInt(s.substring(i+1,i+3),16);
			i+= 3;
			if (i < numChars)
			    c = s.charAt(i);
		    }

		    // A trailing, incomplete byte encoding such as
		    // "%x" will cause an exception to be thrown

		    if ((i < numChars) && (c=='%'))
			throw new IllegalArgumentException(
		         "URLDecoder: Incomplete trailing escape (%) pattern");
		    
		    sb.append(new String(bytes, 0, pos, enc));
		} catch (NumberFormatException e) {
		    throw new IllegalArgumentException(
                    "URLDecoder: Illegal hex characters in escape (%) pattern - " 
		    + e.getMessage());
		}
		needToChange = true;
		break;
	    default: 
		sb.append(c); 
		i++;
		break; 
            }
        }

        return (needToChange? sb.toString() : s);
    }
}
