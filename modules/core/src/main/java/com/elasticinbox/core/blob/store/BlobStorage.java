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

package com.elasticinbox.core.blob.store;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elasticinbox.common.utils.Assert;
import com.elasticinbox.config.Configurator;
import com.elasticinbox.core.blob.BlobDataSource;
import com.elasticinbox.core.blob.BlobUtils;

public class BlobStorage
{
	private static final Logger logger = 
			LoggerFactory.getLogger(BlobStorage.class);

	private final CompressionHandler compressionHandler;
	private final EncryptionHandler encryptionHandler;

	/**
	 * Constructor
	 * 
	 * @param ch Injected Compression Handler
	 * @param eh Injected Encryption Handler
	 */
	public BlobStorage(CompressionHandler ch, EncryptionHandler eh) {
		this.compressionHandler = ch;
		this.encryptionHandler = eh;
	}

	public URI write(String blobName, final InputStream in, final Long size)
			throws IOException, GeneralSecurityException
	{
		Assert.notNull(in, "No data to store");

		InputStream in1, in2;
		Long processedSize = size;

		// compress stream
		if ((compressionHandler != null) && (size > BlobStoreConstants.MIN_COMPRESS_SIZE))
		{
			in1 = compressionHandler.compress(in);
			blobName = blobName + BlobStoreConstants.COMPRESS_SUFFIX;
			
			// size changed, set to unknown to recalculate
			processedSize = null;
		} else {
			in1 = in;
		}

		// encrypt stream
		if (encryptionHandler != null)
		{
			byte[] iv = getCipherIVFromBlobName(blobName);
			in2 = this.encryptionHandler.encrypt(in1, Configurator.getBlobStoreDefaultEncryptionKey(), iv);

			// size changed, set to unknown to recalculate
			processedSize = null;
		} else {
			in2 = in1;
		}

		URI uri = BlobStoreProxy.write(blobName, in2, processedSize);
		
		return uri; 
	}

	/**
	 * Read Blob contents and decrypt
	 * 
	 * @param uri Blob URI
	 * @param keyAlias Cipher Key Alias
	 * @param uncompress Specifies if blob should be uncompressed.
	 * @return
	 * @throws IOException 
	 */
	public BlobDataSource read(final URI uri, final String keyAlias) throws IOException
	{
		InputStream in;

		if (encryptionHandler != null && keyAlias != null)
		{
			try {
				logger.debug("Decrypting object {} with key {}", uri, keyAlias);

				byte[] iv = getCipherIVFromBlobName(BlobUtils.relativize(uri.getPath()));

				in = this.encryptionHandler.decrypt(BlobStoreProxy.read(uri),
						Configurator.getEncryptionKey(keyAlias), iv);
			} catch (GeneralSecurityException gse) {
				throw new IOException("Unable to decrypt message blob: ", gse);
			}
		} else {
			in = BlobStoreProxy.read(uri);
		}

		return new BlobDataSource(uri, in, this.compressionHandler);
	}
	
	/**
	 * Generate cipher initialisation vector (IV) from Blob name.
	 * 
	 * IV should be unique but not necessarily secure. Since blob names are
	 * based on Type1 UUID they are unique.
	 * 
	 * @param blobName
	 * @return
	 * @throws IOException 
	 */
	private static byte[] getCipherIVFromBlobName(final String blobName) throws IOException
	{
		byte[] iv;

		try {
			byte[] nameBytes = blobName.getBytes("UTF-8");
			MessageDigest md = MessageDigest.getInstance("MD5");
			iv = md.digest(nameBytes);
		} catch (Exception e) {
			// should never happen
			throw new IOException(e);
		}

		return iv;
	}

}
