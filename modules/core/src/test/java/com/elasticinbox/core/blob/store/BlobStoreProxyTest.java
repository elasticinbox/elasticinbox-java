package com.elasticinbox.core.blob.store;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.elasticinbox.common.utils.IOUtils;
import com.elasticinbox.config.Configurator;
import com.elasticinbox.core.blob.BlobDataSource;

public class BlobStoreProxyTest
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
	public void testBlobProxy() throws IOException
	{
		// Create
		long origSize = testWrite();

		// Read
		long newSize = testRead();

		String expextedBlobUrl = "blob://"+ Configurator.getBlobStoreWriteProfileName() + "/" + TEMP_BLOB + 
				(Configurator.isBlobStoreCompressionEnabled() ? ".dfl" : ""); 

		// Check Blob URI
		assertThat(blobUri.toString(), equalTo(expextedBlobUrl));

		// Check Blob size
		assertThat(newSize, equalTo(origSize));

		// Delete
		testDelete();
	}

	private long testWrite() throws IOException
	{
		File file = new File(TEST_FILE);
		InputStream in = new FileInputStream(file);
		blobUri = BlobStoreProxy.write(TEMP_BLOB, in, file.length());
		in.close();
		return file.length(); 
	}
	
	private long testRead() throws IOException
	{
		BlobDataSource ds = new BlobDataSource(blobUri);
		return IOUtils.getInputStreamSize(ds.getInflatedInputStream());
	}

	private void testDelete()
	{
		BlobStoreProxy.delete(blobUri);
	}
}
