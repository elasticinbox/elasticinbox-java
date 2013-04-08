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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.util.UUID;

import com.elasticinbox.common.utils.Assert;
import com.elasticinbox.config.Configurator;
import com.elasticinbox.config.DatabaseConstants;
import com.elasticinbox.core.blob.BlobDataSource;
import com.elasticinbox.core.blob.BlobURI;
import com.elasticinbox.core.blob.compression.CompressionHandler;
import com.elasticinbox.core.blob.encryption.EncryptionHandler;
import com.elasticinbox.core.model.Mailbox;

/**
 * Blob storage mediator is an abstraction layer which contains logic which
 * determines where to store or how to access given blob.
 * 
 * @author Rustam Aliyev
 */
public final class BlobStorageMediator implements BlobStorage
{
	private BlobStorage cloudBlobStorage;
	private BlobStorage dbBlobStorage;

	public BlobStorageMediator(CompressionHandler ch, EncryptionHandler eh) {
		cloudBlobStorage = new CloudBlobStorage(ch, eh);
		dbBlobStorage = new CassandraBlobStorage(ch, eh);
	}

	public URI write(UUID messageId, Mailbox mailbox, String profileName,
			InputStream in, Long size) throws IOException,
			GeneralSecurityException
	{
		Assert.notNull(in, "No data to store");

		if (size <= Configurator.getDatabaseBlobMaxSize()) {
			return dbBlobStorage.write(messageId, mailbox, null, in, size);
		} else {
			return cloudBlobStorage.write(messageId, mailbox, Configurator.getBlobStoreWriteProfileName(), in, size);
		}
	}

	public BlobDataSource read(URI uri) throws IOException
	{
		// check if blob was stored for the message
		Assert.notNull(uri, "URI cannot be null");

		boolean isDbProfile = new BlobURI().fromURI(uri).getProfile()
				.equals(DatabaseConstants.DATABASE_PROFILE);

		if (isDbProfile) {
			return dbBlobStorage.read(uri);
		} else {
			return cloudBlobStorage.read(uri);
		}
	}

	public void delete(URI uri) throws IOException
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