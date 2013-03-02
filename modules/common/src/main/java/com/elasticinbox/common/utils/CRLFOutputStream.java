/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package com.elasticinbox.common.utils;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Modified version of the CRLFOutputStream from Apache James project.
 * 
 * A Filter for use with LMTP or other protocols in which lines must end with
 * CRLF. Converts every "isolated" occurrence of \r or \n with \r\n.
 * 
 * RFC 2821 #2.3.7 mandates that line termination is CRLF, and that CR and LF
 * must not be transmitted except in that pairing. If we get a naked LF, convert
 * to CRLF.
 */
public class CRLFOutputStream extends FilterOutputStream
{
	/**
	 * Counter for number of last (0A or 0D).
	 */
	private int statusLast;
	private final static int LAST_WAS_OTHER = 0;
	private final static int LAST_WAS_CR = 1;
	private final static int LAST_WAS_LF = 2;

	/**
	 * Constructor that wraps an OutputStream.
	 * 
	 * @param out
	 *            the OutputStream to be wrapped
	 */
	public CRLFOutputStream(OutputStream out)
	{
		super(out);
		statusLast = LAST_WAS_LF;	// we already assume a CRLF at beginning
									// (otherwise TOP would not work correctly!)
	}

	/**
	 * Writes a byte to the stream Fixes any naked CR or LF to the RFC 2821
	 * mandated CFLF pairing.
	 * 
	 * @param b
	 *            the byte to write
	 * 
	 * @throws IOException
	 *             if an error occurs writing the byte
	 */
	public void write(int b) throws IOException
	{
		switch (b) {
		case '\r':
			out.write('\r');
			out.write('\n');
			statusLast = LAST_WAS_CR;
			break;
		case '\n':
			if (statusLast != LAST_WAS_CR) {
				out.write('\r');
				out.write('\n');
			}
			statusLast = LAST_WAS_LF;
			break;
		default:
			// we're no longer at the start of a line
			out.write(b);
			statusLast = LAST_WAS_OTHER;
			break;
		}
	}

	/**
	 * @see java.io.FilterOutputStream#write(byte[], int, int)
	 */
	public synchronized void write(byte buffer[], int offset, int length) throws IOException
	{
		/* optimized */
		int lineStart = offset;

		for (int i = offset; i < length + offset; i++)
		{
			switch (buffer[i])
			{
				case '\r':
					// CR case. Write down the last line
					// and position the new lineStart at the next char
					out.write(buffer, lineStart, i - lineStart);
					out.write('\r');
					out.write('\n');
					lineStart = i + 1;
					statusLast = LAST_WAS_CR;
					break;
				case '\n':
					if (statusLast != LAST_WAS_CR) {
						out.write(buffer, lineStart, i - lineStart);
						out.write('\r');
						out.write('\n');
					}
					lineStart = i + 1;
					statusLast = LAST_WAS_LF;
					break;
				default:
					statusLast = LAST_WAS_OTHER;
			}
		}

		if (length + offset > lineStart) {
			out.write(buffer, lineStart, length + offset - lineStart);
		}
	}

	/**
	 * Ensure that the stream is CRLF terminated.
	 * 
	 * @throws IOException
	 *             if an error occurs writing the byte
	 */
	public void checkCRLFTerminator() throws IOException
	{
		if (statusLast == LAST_WAS_OTHER)
		{
			out.write('\r');
			out.write('\n');
			statusLast = LAST_WAS_CR;
		}
	}
}