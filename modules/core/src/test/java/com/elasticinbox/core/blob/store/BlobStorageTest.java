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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.GeneralSecurityException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.elasticinbox.common.utils.IOUtils;
import com.elasticinbox.config.Configurator;
import com.elasticinbox.core.blob.BlobDataSource;

public class BlobStorageTest
{
	private final static String TEST_FILE = "../../itests/src/test/resources/01-attach-utf8.eml";
	private final static String TEMP_BLOB = "tmp-email-id-0001";
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
		// BlobStorage without encryption or compression
		BlobStorage bs = new BlobStorage(null, null);
		String expextedBlobUrl = "blob://"+ Configurator.getBlobStoreWriteProfileName() + "/" + TEMP_BLOB; 

		// Write blob
		long origSize = testWrite(bs);

		// Read blob back
		BlobDataSource ds = bs.read(blobUri, null);
		long newSize = IOUtils.getInputStreamSize(ds.getUncompressedInputStream());

		// Check written Blob URI
		assertThat(blobUri.toString(), equalTo(expextedBlobUrl));

		// Check written Blob size
		assertThat(newSize, equalTo(origSize));

		// Delete
		testDelete();		
	}

	@Test
	public void testBlobStorageWithEcnryptionAndCompression() throws IOException, GeneralSecurityException
	{
		// BlobStorage with encryption or compression
		BlobStorage bs = new BlobStorage(new DeflateCompressionHandler(), new AESEncryptionHandler());
		String expextedBlobUrl = "blob://"+ Configurator.getBlobStoreWriteProfileName() + "/" + TEMP_BLOB + BlobStoreConstants.COMPRESS_SUFFIX; 

		// Write blob
		long origSize = testWrite(bs);

		// Check Blob URI
		assertThat(blobUri.toString(), equalTo(expextedBlobUrl));

		// Read blob back
		BlobDataSource ds = bs.read(blobUri, Configurator.getBlobStoreDefaultEncryptionKeyAlias());

		// Verify that suffix matches
		assertThat(ds.isCompressed(), equalTo(true));

		// Verify that compressed size is smaller
		long compressedSize = IOUtils.getInputStreamSize(ds.getInputStream());
		assertThat(compressedSize, lessThan(origSize));
		
		// Read blob back again (can't reuse same InputStream)
		ds = bs.read(blobUri, Configurator.getBlobStoreDefaultEncryptionKeyAlias());
		long newSize = IOUtils.getInputStreamSize(ds.getUncompressedInputStream());

		// Check Blob size
		assertThat(newSize, equalTo(origSize));

		// Delete
		testDelete();		
	}

	private long testWrite(BlobStorage bs) throws IOException, GeneralSecurityException
	{
		File file = new File(TEST_FILE);
		InputStream in = new FileInputStream(file);
		
		blobUri = bs.write(TEMP_BLOB, Configurator.getBlobStoreWriteProfileName(), in, file.length());
		in.close();

		return file.length(); 
	}

	private void testDelete()
	{
		BlobStoreProxy.delete(blobUri);
	}
}