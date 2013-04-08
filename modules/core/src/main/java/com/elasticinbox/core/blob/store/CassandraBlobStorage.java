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

package com.elasticinbox.core.blob.store;

import static com.elasticinbox.config.DatabaseConstants.*;
import static com.elasticinbox.core.blob.store.BlobStoreConstants.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elasticinbox.common.utils.Assert;
import com.elasticinbox.core.blob.BlobDataSource;
import com.elasticinbox.core.blob.BlobURI;
import com.elasticinbox.core.blob.compression.CompressionHandler;
import com.elasticinbox.core.blob.encryption.EncryptionHandler;
import com.elasticinbox.core.cassandra.persistence.BlobPersistence;
import com.elasticinbox.core.model.Mailbox;
import com.google.common.io.ByteStreams;

/**
 * Blob storage proxy for Cassandra
 * 
 * @author Rustam Aliyev
 */
public final class CassandraBlobStorage extends AbstractBlobStorage
{
	private static final Logger logger = 
			LoggerFactory.getLogger(CassandraBlobStorage.class);

	/**
	 * Constructor
	 * 
	 * @param ch Injected Compression Handler
	 * @param eh Injected Encryption Handler
	 */
	public CassandraBlobStorage(CompressionHandler ch, EncryptionHandler eh) {
		super(ch, eh);
	}

	@Override
	public URI write(final UUID messageId, final Mailbox mailbox, final String profileName, final InputStream in, final Long size)
			throws IOException, GeneralSecurityException
	{
		Assert.isTrue(size <= MAX_BLOB_SIZE, "Blob larger than " + MAX_BLOB_SIZE
				+ " bytes can't be stored in Cassandra. Provided blob size: " + size + " bytes");

		logger.debug("Storing blob {} in Cassandra", messageId);

		// prepare URI
		BlobURI blobUri = new BlobURI()
				.setProfile(DATABASE_PROFILE)
				.setName(messageId.toString()).setBlockCount(1);

		InputStream in1;

		// compress stream
		if ((compressionHandler != null) && (size > MIN_COMPRESS_SIZE))
		{
			in1 = compressionHandler.compress(in);
			blobUri.setCompression(compressionHandler.getType());
		} else {
			in1 = in;
		}

		// store blob
		// TODO: currently we allow only single block writes (blockid=0). in future we can split blobs to multiple blocks
		BlobPersistence.writeBlock(messageId, DATABASE_DEFAULT_BLOCK_ID, ByteStreams.toByteArray(in1));

		return blobUri.buildURI();
	}

	@Override
	public BlobDataSource read(final URI uri) throws IOException
	{
		logger.debug("Reading blob {} from Cassandra", uri);

		BlobURI blobUri = new BlobURI().fromURI(uri);
		Assert.isTrue(blobUri.getProfile().equals(DATABASE_PROFILE), "Blob store profile does not match database.");

		UUID messageId = UUID.fromString(blobUri.getName());
		byte[] messageBlock = BlobPersistence.readBlock(messageId, DATABASE_DEFAULT_BLOCK_ID);
		InputStream in = ByteStreams.newInputStreamSupplier(messageBlock).getInput();

		return new BlobDataSource(uri, in, this.compressionHandler);
	}

	@Override
	public void delete(URI uri) throws IOException
	{
		logger.debug("Deleting blob {}", uri);

		BlobURI blobUri = new BlobURI().fromURI(uri);
		Assert.isTrue(blobUri.getProfile().equals(DATABASE_PROFILE), "Blob store profile does not match database.");

		UUID messageId = UUID.fromString(blobUri.getName());
		BlobPersistence.deleteBlock(messageId, BlobStoreConstants.DATABASE_DEFAULT_BLOCK_ID);
	}
	
}
