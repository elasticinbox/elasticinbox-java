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
import com.elasticinbox.config.Configurator;
import com.elasticinbox.core.blob.BlobDataSource;
import com.elasticinbox.core.blob.BlobURI;
import com.elasticinbox.core.blob.BlobUtils;
import com.elasticinbox.core.blob.naming.BlobNameBuilder;
import com.elasticinbox.core.encryption.AESEncryptionHandler;
import com.elasticinbox.core.encryption.EncryptionHandler;
import com.elasticinbox.core.cassandra.persistence.BlobPersistence;
import com.elasticinbox.core.model.Mailbox;
import com.google.common.io.ByteStreams;
import com.google.common.io.FileBackedOutputStream;

/**
 * Blob storage proxy for Cassandra
 * 
 * @author Rustam Aliyev
 */
public final class CassandraBlobStorage extends BlobStorage {
	private static final Logger logger = LoggerFactory
			.getLogger(CassandraBlobStorage.class);

	/**
	 * Constructor
	 * <p>
	 * Cassandra blob storage does not perform compression of encryption.
	 */
	public CassandraBlobStorage() {

	}

	/**
	 * Constructor
	 * 
	 * @param eh
	 *            Injected Encryption Handler
	 */
	public CassandraBlobStorage(EncryptionHandler eh) {
		encryptionHandler = eh;
	}

	@Override
	public BlobURI write(final UUID messageId, final Mailbox mailbox, final String profileName, final InputStream in, final Long size)
			throws IOException, GeneralSecurityException
	{
		Assert.isTrue(size <= MAX_BLOB_SIZE, "Blob larger than " + MAX_BLOB_SIZE
				+ " bytes can't be stored in Cassandra. Provided blob size: " + size + " bytes");

		logger.debug("Storing blob {} in Cassandra", messageId);
		// get blob name
		String blobName = new BlobNameBuilder().setMailbox(mailbox)
				.setMessageId(messageId).setMessageSize(size).build();

		// prepare URI
		BlobURI blobUri = new BlobURI()
				.setProfile(DATABASE_PROFILE)
				.setName(messageId.toString()).setBlockCount(1);

		InputStream in1;
		// encrypt stream
		if (encryptionHandler != null) {
			byte[] iv = AESEncryptionHandler.getCipherIVFromBlobName(blobName);

			InputStream encryptedInputStream = this.encryptionHandler.encrypt(
					in, Configurator.getDefaultEncryptionKey(), iv);
			FileBackedOutputStream fbout = new FileBackedOutputStream(
					MAX_MEMORY_FILE_SIZE, true);

			ByteStreams.copy(encryptedInputStream, fbout);
			
			in1 = fbout.getSupplier().getInput();

			blobUri.setEncryptionKey(Configurator
					.getDefaultEncryptionKeyAlias());
		} else {
			in1 = in;
		}

		// store blob
		// TODO: currently we allow only single block writes (blockid=0). in future we can split blobs to multiple blocks
		BlobPersistence.writeBlock(messageId, DATABASE_DEFAULT_BLOCK_ID, ByteStreams.toByteArray(in));

		return blobUri;
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
		String keyAlias = blobUri.getEncryptionKey();

		if (keyAlias != null) {
			// currently we only support AES encryption, use by default
			EncryptionHandler eh = new AESEncryptionHandler();

			try {
				logger.debug("Decrypting object {} with key {}", uri, keyAlias);

				byte[] iv = AESEncryptionHandler.getCipherIVFromBlobName(BlobUtils.relativize(uri
						.getPath()));

				in = eh.decrypt(in, Configurator.getEncryptionKey(keyAlias), iv);

				// Configurator.getEncryptionKey(keyAlias), iv);
			} catch (GeneralSecurityException gse) {
				throw new IOException("Unable to decrypt message blob: ", gse);
			}
		}

		return new BlobDataSource(uri, in);
	}

	@Override
	public void delete(final URI uri) throws IOException
	{
		logger.debug("Deleting blob {}", uri);

		BlobURI blobUri = new BlobURI().fromURI(uri);
		Assert.isTrue(blobUri.getProfile().equals(DATABASE_PROFILE), "Blob store profile does not match database.");

		UUID messageId = UUID.fromString(blobUri.getName());
		BlobPersistence.deleteBlock(messageId, BlobStoreConstants.DATABASE_DEFAULT_BLOCK_ID);
	}
	
}
