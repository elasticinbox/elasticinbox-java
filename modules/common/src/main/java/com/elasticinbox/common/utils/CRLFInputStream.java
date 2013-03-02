/**
 * Copyright (c) 2011-2013 Optimax Software Ltd.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *  * Neither the name of Optimax Software, ElasticInbox, nor the names
 *    of its contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.elasticinbox.common.utils;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A Filter for use with POP3 or other protocols in which lines must end with
 * CRLF. Converts every "isolated" occurrence of \r or \n with \r\n.
 * 
 * @author Rustam Aliev
 */
public class CRLFInputStream extends FilterInputStream
{
	/**
	 * Last char indicator (CR, LF, or OTHER).
	 */
	private int statusLast;
	private final static int LAST_WAS_OTHER = 0;
	private final static int LAST_WAS_CR = 1;
	private final static int LAST_WAS_LF = 2;

	private static final int CR = 13;
	private static final int LF = 10;

	private static final int EMPTY_BUFFER = -1;
	private static int buffer = EMPTY_BUFFER;

	public CRLFInputStream(InputStream in)
	{
		super(in);
		statusLast = LAST_WAS_LF; // we already assume a CRLF at beginning
	}

	@Override
	public int read(byte[] b) throws IOException {
		return read(b, 0, b.length);
	}

	@Override
	public int read() throws IOException
	{
		// if buffer not empty, return buffer first
		if (buffer > EMPTY_BUFFER) {
			int b = buffer;
			buffer = EMPTY_BUFFER;
			return b;
		}

		// read byte from stream
		int b = super.read();

		switch (b)
		{
			case CR:
				statusLast = LAST_WAS_CR;
				break;
			case LF:
				if (statusLast != LAST_WAS_CR) {
					// return CR instead
					b = CR;
					statusLast = LAST_WAS_CR;
				} else {
					statusLast = LAST_WAS_LF;
				}
				break;
			default:
				if (statusLast == LAST_WAS_CR) {
					// add current byte to buffer and return LF
					buffer = b;
					b = LF;
					statusLast = LAST_WAS_LF;
				} else {
					// we're no longer at the start of a line
					statusLast = LAST_WAS_OTHER;
				}
				break;
		}

		return b;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException
	{
		// TODO: should be implemented
		// we don't need it right now and it's a bit complicated
		return -1;
	}
}