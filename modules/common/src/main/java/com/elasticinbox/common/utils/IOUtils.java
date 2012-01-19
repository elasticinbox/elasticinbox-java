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

package com.elasticinbox.common.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.ning.compress.lzf.LZFDecoder;
import com.ning.compress.lzf.LZFEncoder;
import com.ning.compress.lzf.LZFInputStream;
import com.ning.compress.lzf.LZFOutputStream;

public class IOUtils
{
	private final static int DEFAULT_BUFFER_SIZE = 100 * 1024;	// 100K should be ok for emails

	// ensure non-instantiability
	private IOUtils() {
	}

	public static byte[] compress(byte[] in) throws IOException {
		return LZFEncoder.encode(in);
	}

	public static byte[] compress(String in) throws IOException {
		return LZFEncoder.encode(in.getBytes("UTF-8"));
	}

	public static void compress(InputStream in, OutputStream out)
			throws IOException
	{
		LZFOutputStream compressor = null;
		boolean success = true;

		try {
			compressor = new LZFOutputStream(out);
			byte[] buf = ThreadLocalByteBuffer.getBuffer();
			int len;

			while ((len = in.read(buf)) != -1) {
				compressor.write(buf, 0, len);
			}

			compressor.close();
		} catch (IOException e) {
			success = false;
			throw new IllegalStateException(e.getMessage(), e);
		} finally {
			if (!success && (compressor != null)) {
				compressor.close();
			}
		}
	}

	public static byte[] decompress(byte[] in) throws IOException {
		return LZFDecoder.decode(in);
	}

	public static void decompress(InputStream in, OutputStream out)
			throws IOException
	{
		LZFInputStream decompressor = null;

		try {
			decompressor = new LZFInputStream(in);
			byte[] buf = ThreadLocalByteBuffer.getBuffer();
			int len;

			while ((len = decompressor.read(buf)) != -1) {
				out.write(buf, 0, len);
			}
		} finally {
			if (decompressor != null) {
				decompressor.close();
			}
		}
	}

	public static boolean isCompressed(byte[] data) {
		return data[0] == 'Z' && data[1] == 'V';
	}

	public static void copy(InputStream in, OutputStream out) throws IOException
	{
		byte[] buf = ThreadLocalByteBuffer.getBuffer();
		int len;

		while ((len = in.read(buf)) != -1) {
			out.write(buf, 0, len);
		}
	}

	/**
	 * Calculate InputStream size by reading through it
	 * 
	 * @param in
	 * @return Size of the data
	 * @throws IOException
	 */
	public static long getInputStreamSize(InputStream in) throws IOException
	{
		long size = 0L;
		long len;

		while ((len = in.skip(DEFAULT_BUFFER_SIZE)) > 0) {
			size += len;
		}

		return size;
	}
	
}
