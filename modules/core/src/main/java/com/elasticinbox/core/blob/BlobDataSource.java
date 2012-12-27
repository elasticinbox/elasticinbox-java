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

package com.elasticinbox.core.blob;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import com.elasticinbox.common.utils.Assert;
import com.elasticinbox.core.blob.store.BlobStoreConstants;
import com.elasticinbox.core.blob.store.CompressionHandler;

/**
 * This class builds Blob data source from the given URI. It provides methods
 * for identifying and uncompressing blobs.
 * 
 * @author Rustam Aliyev
 */
public class BlobDataSource
{
	private final InputStream in;
	private final Boolean compressed;
	private final URI blobUri;
	private final CompressionHandler compressionHandler;

	public BlobDataSource(final URI uri, final InputStream in, final CompressionHandler ch)
	{
		Assert.notNull(uri.getPath(), "Invalid blob URI provided, missing URI path: " + uri.toString());

		this.blobUri = uri;
		this.in = in;
		this.compressed = uri.getPath().endsWith(BlobStoreConstants.COMPRESS_SUFFIX);
		this.compressionHandler = ch;
	}

	/**
	 * Check if Blob is compressed. Compression is identified by Blob name
	 * suffix.
	 * 
	 * @return
	 */
	public boolean isCompressed() {
		return compressed;
	}

	/**
	 * Returns unprocessed Blob data. If compressed, Blob will be returned
	 * as binary compressed data.
	 * <p>
	 * For uncompressed stream use {@link #getUncompressedInputStream()}
	 * 
	 * @return
	 * @throws IOException
	 */
	public InputStream getInputStream() throws IOException {
		return in;
	}

	/**
	 * Returns Blob data and ensures that content is always uncompressed.
	 * If not compressed, original Blob will be returned.
	 * <p> 
	 * Use this method if you want to ensure that data is always uncompressed.
	 * 
	 * @return
	 * @throws IOException
	 */
	public InputStream getUncompressedInputStream() throws IOException
	{
		if (compressed && this.compressionHandler != null) {
			return this.compressionHandler.uncompress(in);
		} else {
			return in;
		}
	}

	public String getName() {
		return BlobUtils.relativize(blobUri.getPath());
	}

}
