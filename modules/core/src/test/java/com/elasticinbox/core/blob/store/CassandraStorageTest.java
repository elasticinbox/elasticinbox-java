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

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.elasticinbox.common.utils.IOUtils;
import com.elasticinbox.config.Configurator;
import com.elasticinbox.config.DatabaseConstants;
import com.elasticinbox.core.blob.BlobDataSource;
import com.elasticinbox.core.encryption.AESEncryptionHandler;
import com.elasticinbox.core.encryption.EncryptionHandler;
import com.elasticinbox.core.model.Mailbox;

public class CassandraStorageTest
{
	private final static String TEST_FILE = "../../itests/src/test/resources/01-simple-ascii.eml";
	private final static String TEST_LARGE_FILE = "../../itests/src/test/resources/01-attach-utf8.eml";
	private final static UUID MESSAGE_ID = UUID.fromString("f1ca99e0-99a0-11e2-95f0-040cced3bd7a");
	private final static Mailbox MAILBOX = new Mailbox("test@elasticinbox.com");
	private URI blobUri;

	@Before
	public void setupCase()
	{
		//System.setProperty("elasticinbox.config", "../../config/elasticinbox.yaml");
		System.setProperty("elasticinbox.config", "../../itests/src/test/resources/elasticinbox.yaml");
	}

	@After
	public void teardownCase() {
	}

	@Test
	public void testBlobStorage() throws IOException, GeneralSecurityException
	{
		String expextedBlobUrl = "blob://"
				+ DatabaseConstants.DATABASE_PROFILE + "/"
				+ MESSAGE_ID + "?"
				+ BlobStoreConstants.URI_PARAM_BLOCK_COUNT + "=1";

		// BlobStorage without encryption or compression
		BlobStorage bs = new CassandraBlobStorage();

		// Write blob
		long origSize = testWrite(bs, TEST_FILE);

		// Check written Blob URI
		assertThat(blobUri.toString(), equalTo(expextedBlobUrl));

		// Read blob back
		BlobDataSource ds = bs.read(blobUri);
		long newSize = IOUtils.getInputStreamSize(ds.getUncompressedInputStream());

		// Check written Blob size
		assertThat(newSize, equalTo(origSize));

		// Delete
		bs.delete(blobUri);		
	}

	@Test(expected=IllegalArgumentException.class)
	public void testLargeBlobStorage() throws IOException, GeneralSecurityException
	{
		// BlobStorage without encryption or compression
		BlobStorage bs = new CassandraBlobStorage();

		// Write blob which is too large for DB storage. Should throw exception.
		testWrite(bs, TEST_LARGE_FILE);
	}

	/*
	 * @author itembase GmbH, John Wiesel <jw@itembase.biz>
	 */
	@Test
	public void testEncryptedBlobStorage() throws IOException, GeneralSecurityException
	{
		String expextedBlobUrl = "blob://"
				+ DatabaseConstants.DATABASE_PROFILE + "/"				
				+ MESSAGE_ID + "?"
				+ BlobStoreConstants.URI_PARAM_ENCRYPTION_KEY + "=" + Configurator.getBlobStoreDefaultEncryptionKeyAlias() + "&"
				+ BlobStoreConstants.URI_PARAM_BLOCK_COUNT + "=1";

		EncryptionHandler encryptionHandler =  new AESEncryptionHandler();

		// BlobStorage with encryption
		BlobStorage bs = new CassandraBlobStorage(encryptionHandler);

		// Write blob
		long origSize = testWrite(bs, TEST_FILE);

		// Check written Blob URI
		assertThat(blobUri.toString(), equalTo(expextedBlobUrl));

		// Read blob back
		BlobDataSource ds = bs.read(blobUri);
		
		long newSize = IOUtils.getInputStreamSize(ds.getUncompressedInputStream());

		// Check written Blob size
		assertThat(newSize, equalTo(origSize));

		// Delete
		bs.delete(blobUri);		
	}
	
	private long testWrite(BlobStorage bs, String filename) throws IOException, GeneralSecurityException
	{
		File file = new File(filename);
		InputStream in = new FileInputStream(file);
		
		blobUri = bs.write(MESSAGE_ID, MAILBOX, Configurator.getBlobStoreWriteProfileName(), in, file.length()).buildURI();
		in.close();

		return file.length(); 
	}

}