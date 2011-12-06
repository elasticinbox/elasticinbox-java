/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package com.elasticinbox.lmtp.server.core.mina;

import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;

import org.apache.mina.core.buffer.BufferDataException;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

/**
 * A {@link ProtocolDecoder} which decodes incoming LMTP data based on session context.
 * 
 * @author De Oliveira Edouard &lt;doe_wanted@yahoo.fr&gt;
 * @author Rustam Aliyev
 */
public class LMTPCodecDecoder implements ProtocolDecoder 
{
	protected final static String CONTEXT = LMTPCodecDecoder.class.getName() + ".context";
	public final static String DATASIZE_ATTRIBUTE = "MessageSize"; // size of the last message

	protected final static String TMPFILE_PREFIX = "elasticinbox";
	protected final static String TMPFILE_SUFFIX = ".eml";

	private final static byte[] LMTP_CMD_DELIMITER = new byte[] { '\r', '\n' };
	private final static byte[] LMTP_DATA_DELIMITER = new byte[] { '\r', '\n', '.', '\r', '\n' };

	private Charset charset;

	/**
	 * <a href="http://rfc.net/rfc2822.html#s2.1.1.">RFC 2822</a>
	 */
	private int maxLineLength = 998;

	/** When to trigger */
	private int threshold;

	/**
	 * Creates a new instance with the specified <tt>charset</tt> and the
	 * specified <tt>thresholdBytes</tt> deferring size.
	 */
	public LMTPCodecDecoder(Charset charset, int thresholdBytes) {
		setup(charset, thresholdBytes);
	}

    public void setup(Charset charset, int thresholdBytes) 
    {
		if (charset == null) {
			throw new NullPointerException("charset");
		}

		this.charset = charset;
		this.threshold = thresholdBytes;
    }

	/** */
	public void setDataDeferredSize(int dataDeferredSize) {
		this.threshold = dataDeferredSize;
	}

	/**
	 * Returns the allowed maximum size of the line to be decoded. If the size
	 * of the line to be decoded exceeds this value, the decoder will throw a
	 * {@link BufferDataException}. The default value is <tt>998</tt> bytes.
	 */
	public int getMaxLineLength() {
		return maxLineLength;
	}

    /**
	 * Sets the allowed maximum size of the line to be decoded. If the size of
	 * the line to be decoded exceeds this value, the decoder will throw a
	 * {@link BufferDataException}. The default value is <tt>998</tt> bytes.
	 */
    public void setMaxLineLength(int maxLineLength) 
    {
		if (maxLineLength <= 0) {
			throw new IllegalArgumentException("maxLineLength: " + maxLineLength);
		}

		this.maxLineLength = maxLineLength;
    }

    /** */
    private LMTPDecoderContext getContext(IoSession session) 
    {
		LMTPDecoderContext ctx = (LMTPDecoderContext) session.getAttribute(CONTEXT);
		if (ctx == null) {
			ctx = new LMTPDecoderContext(this);
			session.setAttribute(CONTEXT, ctx);
		}
		return ctx;
    }

    /** */
    public void finishDecode(IoSession session, ProtocolDecoderOutput out)
            throws Exception 
    {
    }

    /** */
	public void dispose(IoSession session) throws Exception
	{
		LMTPDecoderContext ctx = (LMTPDecoderContext) session.getAttribute(CONTEXT);
		if (ctx != null) {
			ctx.getBuffer().clear();
			ctx.closeOutputStream();
			session.removeAttribute(CONTEXT);
		}
	}

    /** */
	public void decode(IoSession session, IoBuffer in, ProtocolDecoderOutput out)
			throws Exception
	{
		LMTPDecoderContext ctx = getContext(session);
		int matchCount = ctx.getMatchCount();

		LMTPContext minaCtx = (LMTPContext) 
				session.getAttribute(LMTPConnectionHandler.CONTEXT_ATTRIBUTE);

		boolean dataMode = minaCtx.getSession().isDataMode();
		ctx.setDataMode(dataMode);
		byte[] delimBuf = dataMode ? LMTP_DATA_DELIMITER : LMTP_CMD_DELIMITER;

		// Try to find a match
		int oldPos = in.position();
		int oldLimit = in.limit();

		if (matchCount == delimBuf.length)
			matchCount = 0;

        while (in.hasRemaining()) 
        {
            byte b = in.get();
            if (delimBuf[matchCount] == b) 
            {
				matchCount++;
				if (matchCount == delimBuf.length)
				{
					// Found a match.
					int pos = in.position();
					in.limit(pos);
					in.position(oldPos);

					ctx.write(in);

					in.limit(oldLimit);
					in.position(pos);

					if (ctx.getOverflowPosition() == 0)
					{
						IoBuffer buf = ctx.getBuffer();
						buf.flip();

						try {
							if (dataMode) {
								// set size
								session.setAttribute(DATASIZE_ATTRIBUTE, ctx.getDataSize());
								// send stream
								out.write(ctx.getInputStream());
								// switch back to command mode
								delimBuf = LMTP_CMD_DELIMITER;
								dataMode = false;
								ctx.setDataMode(dataMode);
							} else {
								buf.limit(buf.limit() - matchCount);
								out.write(buf.getString(ctx.getDecoder()));
							}
						} catch (IOException ioex) {
							throw new CharacterCodingException();
						} finally {
							ctx.reset();
							buf.clear();
						}
					} else {
						String msg = "Line is too long: "
								+ ctx.getOverflowPosition();
						ctx.reset();
						throw new BufferDataException(msg);
					}

                    oldPos = pos;
                    matchCount = 0;
                }
            } 
            else
            {
				// fix for DIRMINA-506
				in.position(Math.max(0, in.position() - matchCount));
                matchCount = 0;
            }
        }

		// Put remainder to buf.
		in.position(oldPos);
		ctx.write(in);

		ctx.setMatchCount(matchCount);
	}

	public Charset getCharset() {
		return charset;
	}

	public int getThreshold() {
		return threshold;
	}
}