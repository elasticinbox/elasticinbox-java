package com.elasticinbox.core.blob.store;

import java.io.IOException;
import java.security.MessageDigest;

import com.elasticinbox.core.blob.compression.CompressionHandler;
import com.elasticinbox.core.blob.encryption.EncryptionHandler;

public abstract class AbstractBlobStorage implements BlobStorage
{
	protected final CompressionHandler compressionHandler;
	protected final EncryptionHandler encryptionHandler;

	/**
	 * Constructor
	 * 
	 * @param ch Injected Compression Handler
	 * @param eh Injected Encryption Handler
	 */
	public AbstractBlobStorage(CompressionHandler ch, EncryptionHandler eh) {
		this.compressionHandler = ch;
		this.encryptionHandler = eh;
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
	protected static byte[] getCipherIVFromBlobName(final String blobName) throws IOException
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
