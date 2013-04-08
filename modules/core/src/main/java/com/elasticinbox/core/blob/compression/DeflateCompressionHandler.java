/**
 * Copyright (c) 2011-2012 Optimax Software Ltd.
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

package com.elasticinbox.core.blob.compression;

import java.io.InputStream;
import java.util.zip.DeflaterInputStream;
import java.util.zip.InflaterInputStream;

/**
 * This class provides compression/decompression using Deflate algorithm.
 * <p>
 * Deflate compression (RFC1951) used in ElasticInbox due to a number of reasons:
 * <p>
 * 1. It is possible to serve deflated content over HTTP without decompression.
 * <p>
 * 2. It is possible to serve deflated content over IMAP without decompression.
 *    IMAP COMPRESS Extension (RFC4978) recognizes only deflate.
 * <p>
 * 3. Deflate has better performance in comparison to Gzip and provides good trade
 *    off between size and speed.
 * 
 * @author Rustam Aliyev
 */
public class DeflateCompressionHandler implements CompressionHandler
{
	public final static String COMPRESSION_TYPE_DEFLATE = "dfl";

	@Override
	public InputStream compress(InputStream in) {
		return new DeflaterInputStream(in);
	}

	@Override
	public InputStream uncompress(InputStream in) {
		return new InflaterInputStream(in);
	}

	@Override
	public String getType() {
		return COMPRESSION_TYPE_DEFLATE;
	}
}
