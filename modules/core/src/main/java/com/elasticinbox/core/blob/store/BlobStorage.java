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
import java.security.MessageDigest;
import java.util.UUID;

import com.elasticinbox.core.blob.BlobDataSource;
import com.elasticinbox.core.blob.BlobURI;
import com.elasticinbox.core.encryption.EncryptionHandler;
import com.elasticinbox.core.model.Mailbox;

public abstract class BlobStorage {

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
	protected static byte[] getCipherIVFromBlobName(final String blobName)
			throws IOException {
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

	protected EncryptionHandler encryptionHandler;

	/**
	 * Store blob contents, optionally compress and encrypt.
	 * 
	 * @param messageId
	 *            Unique message ID
	 * @param mailbox
	 *            Message owner's Mailbox
	 * @param profileName
	 *            Blob store profile name
	 * @param in
	 *            Payload
	 * @param size
	 *            Payload size in bytes
	 * @return
	 * @throws IOException
	 * @throws GeneralSecurityException
	 */
	public abstract BlobURI write(final UUID messageId, final Mailbox mailbox,
			final String profileName, final InputStream in, final Long size)
			throws IOException, GeneralSecurityException;

	/**
	 * Read blob contents and decrypt
	 * 
	 * @param uri
	 *            Blob URI
	 * @return
	 * @throws IOException
	 */
	public abstract BlobDataSource read(final URI uri) throws IOException;

	/**
	 * Delete blob
	 * 
	 * @param uri
	 * @throws IOException
	 */
	public abstract void delete(final URI uri) throws IOException;

}
