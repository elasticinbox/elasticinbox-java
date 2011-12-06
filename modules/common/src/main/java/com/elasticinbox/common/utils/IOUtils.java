/**
 * Copyright (c) 2011 Optimax Software Ltd
 * 
 * This file is part of ElasticInbox.
 * 
 * ElasticInbox is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 2 of the License, or (at your option) any later
 * version.
 * 
 * ElasticInbox is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * ElasticInbox. If not, see <http://www.gnu.org/licenses/>.
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
