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
import com.elasticinbox.config.DatabaseConstants;
import com.elasticinbox.core.blob.BlobDataSource;
import com.elasticinbox.core.blob.BlobURI;
import com.elasticinbox.core.blob.compression.CompressionHandler;
import com.elasticinbox.core.blob.compression.DeflateCompressionHandler;
import com.elasticinbox.core.encryption.AESEncryptionHandler;
import com.elasticinbox.core.encryption.EncryptionHandler;
import com.elasticinbox.core.model.Mailbox;
import com.google.common.io.ByteStreams;
import com.google.common.io.FileBackedOutputStream;

/**
 * Blob storage mediator is an abstraction layer containing logic which
 * determines where to store or how to access given blob.
 * 
 * @author Rustam Aliyev
 */
public final class BlobStorageMediator extends BlobStorage {
	private static final Logger logger = LoggerFactory
			.getLogger(BlobStorageMediator.class);

	protected static byte[] getCipherIVFromBlobName(final String blobName)
			throws IOException {
		return null;
	}

	protected final CompressionHandler compressionHandler;

	private BlobStorage cloudBlobStorage;
	private BlobStorage dbBlobStorage;

	/**
	 * Initialise mediator with compression and encryption handlers for writes.
	 * To disable compression/encryption set value to null.
	 * 
	 * @param ch
	 *            Injected compression handler
	 * @param eh
	 *            Injected encryption handler
	 */
			
	public BlobStorageMediator(final CompressionHandler ch, final EncryptionHandler eh)
	{
		this.compressionHandler = ch;
		
		if (Configurator.isRemoteBlobStoreEncryptionEnabled()) {
			cloudBlobStorage = new CloudBlobStorage(eh);
		} else {
			cloudBlobStorage = new CloudBlobStorage(null);
		}
		
		if (Configurator.isLocalBlobStoreEncryptionEnabled()) {
			dbBlobStorage = new CassandraBlobStorage(eh);
		} else {
			dbBlobStorage = new CassandraBlobStorage(null);
		}
	}
	
	public BlobURI write(final UUID messageId, final Mailbox mailbox, final String profileName,
			final InputStream in, final Long size) throws IOException,
			GeneralSecurityException
	{
		Assert.notNull(in, "No data to store");

		BlobURI blobUri;
		InputStream in1;
		Long updatedSize = size;
		boolean compressed = false;

		// compress stream and calculate compressed size
		if ((compressionHandler != null) && (size > MIN_COMPRESS_SIZE))
		{
			InputStream compressedInputStream = compressionHandler.compress(in); 
			FileBackedOutputStream fbout = new FileBackedOutputStream(MAX_MEMORY_FILE_SIZE, true);
			updatedSize = ByteStreams.copy(compressedInputStream, fbout);
			in1 = fbout.getSupplier().getInput();
			compressed = true;
		} else {
			in1 = in;
		}

		if (updatedSize <= Configurator.getDatabaseBlobMaxSize())
		{
			logger.debug(
					"Storing Blob in the database because size ({}KB) was less than database threshold {}KB",
					updatedSize, Configurator.getDatabaseBlobMaxSize());
			blobUri = dbBlobStorage.write(messageId, mailbox, null, in1, updatedSize);
		} else {
			logger.debug(
					"Storing Blob in the cloud because size ({}KB) was greater than database threshold {}KB",
					updatedSize, Configurator.getDatabaseBlobMaxSize());
			blobUri = cloudBlobStorage.write(messageId, mailbox, Configurator.getBlobStoreWriteProfileName(), in1, updatedSize);
		}

		// add compression information to the blob URI
		if (compressed) {
			blobUri.setCompression(compressionHandler.getType());
		}

		return blobUri;
	}

	public BlobDataSource read(final URI uri) throws IOException
	{
		// check if blob was stored for the message
		Assert.notNull(uri, "URI cannot be null");

		BlobDataSource blobDS;
		BlobURI blobUri = new BlobURI().fromURI(uri);

		if (blobUri.getProfile().equals(DatabaseConstants.DATABASE_PROFILE)) {
			blobDS = dbBlobStorage.read(uri);
		} else {
			blobDS = cloudBlobStorage.read(uri);
		}

		// if compressed, add compression handler to data source
		if ((blobUri.getCompression() != null && blobUri.getCompression()
				.equals(DeflateCompressionHandler.COMPRESSION_TYPE_DEFLATE)) ||
				// TODO: deprecated suffix based compression detection
				// kept for backward compatibility with 0.3
				blobUri.getName().endsWith(BlobStoreConstants.COMPRESS_SUFFIX))
		{
			CompressionHandler ch = new DeflateCompressionHandler();
			return new BlobDataSource(uri, blobDS.getInputStream(), ch);
		} else {
			return blobDS;
		}
	}

	public void delete(final URI uri) throws IOException
	{
		// check if blob was stored for the message, silently skip otherwise
		if (uri == null) {
			return; 
		}

		boolean isDbProfile = new BlobURI().fromURI(uri).getProfile()
				.equals(DatabaseConstants.DATABASE_PROFILE);

		if (isDbProfile) {
			dbBlobStorage.delete(uri);
		} else {
			cloudBlobStorage.delete(uri);
		}
	}
}