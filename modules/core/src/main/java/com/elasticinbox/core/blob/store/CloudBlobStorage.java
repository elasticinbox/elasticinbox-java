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
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elasticinbox.config.Configurator;
import com.elasticinbox.core.blob.BlobDataSource;
import com.elasticinbox.core.blob.BlobURI;
import com.elasticinbox.core.blob.BlobUtils;
import com.elasticinbox.core.blob.compression.CompressionHandler;
import com.elasticinbox.core.blob.encryption.EncryptionHandler;
import com.elasticinbox.core.blob.naming.BlobNameBuilder;
import com.elasticinbox.core.model.Mailbox;

public final class CloudBlobStorage extends AbstractBlobStorage
{
	private static final Logger logger = 
			LoggerFactory.getLogger(CloudBlobStorage.class);

	/**
	 * Constructor
	 * 
	 * @param ch Injected Compression Handler
	 * @param eh Injected Encryption Handler
	 */
	public CloudBlobStorage(CompressionHandler ch, EncryptionHandler eh) {
		super(ch, eh);
	}

	@Override
	public URI write(final UUID messageId, final Mailbox mailbox, final String profileName, final InputStream in, final Long size)
			throws IOException, GeneralSecurityException
	{
		// get blob name
		String blobName = new BlobNameBuilder().setMailbox(mailbox)
				.setMessageId(messageId).setMessageSize(size).build();

		InputStream in1, in2;
		Long processedSize = size;

		// prepare URI
		BlobURI blobUri = new BlobURI()
				.setProfile(profileName)
				.setName(blobName);

		// compress stream
		if ((compressionHandler != null) && (size > BlobStoreConstants.MIN_COMPRESS_SIZE))
		{
			in1 = compressionHandler.compress(in);
			blobUri.setCompression(compressionHandler.getType());
			
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

			blobUri.setEncryptionKey(Configurator.getBlobStoreDefaultEncryptionKeyAlias());

			// size changed, set to unknown to recalculate
			processedSize = null;
		} else {
			in2 = in1;
		}

		CloudStoreProxy.write(blobName, profileName, in2, processedSize);

		return blobUri.buildURI();
	}

	@Override
	public BlobDataSource read(final URI uri) throws IOException
	{
		InputStream in;
		
		BlobURI blobUri = new BlobURI().fromURI(uri); 
		String keyAlias = blobUri.getEncryptionKey();

		if (encryptionHandler != null && keyAlias != null)
		{
			try {
				logger.debug("Decrypting object {} with key {}", uri, keyAlias);

				byte[] iv = getCipherIVFromBlobName(BlobUtils.relativize(uri.getPath()));

				in = this.encryptionHandler.decrypt(CloudStoreProxy.read(uri),
						Configurator.getEncryptionKey(keyAlias), iv);
			} catch (GeneralSecurityException gse) {
				throw new IOException("Unable to decrypt message blob: ", gse);
			}
		} else {
			in = CloudStoreProxy.read(uri);
		}

		return new BlobDataSource(uri, in, this.compressionHandler);
	}

	@Override
	public void delete(final URI uri) throws IOException
	{
		CloudStoreProxy.delete(uri);
	}
}
