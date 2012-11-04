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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.DeflaterInputStream;

import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.filesystem.reference.FilesystemConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elasticinbox.common.utils.Assert;
import com.elasticinbox.config.Configurator;
import com.elasticinbox.config.blob.BlobStoreProfile;
import com.elasticinbox.core.log.JcloudsSlf4JLoggingModule;
import com.google.common.collect.ImmutableSet;

/**
 * This is a proxy class for jClouds Blobstore API.
 * 
 * <p>
 * Each blob store has its own configuration profile {@link BlobStoreProfile}
 * which contains connection information. Connections to the blob stores (such
 * as S3, OpenStack, etc.) are established on demand and cached.
 * 
 * @author Rustam Aliyev
 * @see {@link BlobStoreProfile}
 * @see {@link BlobStore}
 * @see {@link BlobStoreContext}
 * @see <a href="http://www.jclouds.org/">jClouds</a>
 */
public final class BlobStoreProxy
{
	private static final Logger logger = 
			LoggerFactory.getLogger(BlobStoreProxy.class);

	private static final String PROVIDER_FILESYSTEM = "filesystem";
	private static final String PROVIDER_TRANSIENT = "transient";

	private static ConcurrentHashMap<String, BlobStoreContext> blobStoreContexts = 
			new ConcurrentHashMap<String, BlobStoreContext>();

	/**
	 * Store Blob
	 * 
	 * @param blobName
	 *            Blob filename including relative path
	 * @param in
	 *            Payload
	 * @param size
	 *            Payload size in bytes
	 * @return
	 * @throws IOException 
	 */
	public static URI write(String blobName, InputStream in, final Long size) throws IOException
	{
		Assert.notNull(in, "No data to store");

		final String profileName = Configurator.getBlobStoreWriteProfileName(); 
		final String container = Configurator.getBlobStoreProfile(profileName).getContainer();
		final String provider = Configurator.getBlobStoreProfile(profileName).getProvider();
		BlobStoreContext context = getBlobStoreContext(profileName);

		logger.debug("Storing blob {} on {}", blobName, profileName);

		BlobStore blobStore = context.getBlobStore();

		// add blob
		Blob blob;

		if ((size > BlobStoreConstants.MIN_COMPRESS_SIZE)
				&& BlobStoreConstants.CHUNKED_ENCODING_CAPABILITY.contains(provider))
		{
			// compressed stream, size unknown
			InputStream dis = new DeflaterInputStream(in);
			blobName = blobName + BlobStoreConstants.COMPRESS_SUFFIX;
			blob = blobStore.blobBuilder(blobName).payload(dis).build();
		} else {
			blob = blobStore.blobBuilder(blobName).payload(in).contentLength(size).build();
		}

		blobStore.putBlob(container, blob);
		return buildURI(profileName, blobName);
	}

	/**
	 * Read Blob contents
	 * 
	 * @param uri
	 * @return
	 */
	public static InputStream read(URI uri)
	{
		// check if blob was stored for the message
		Assert.notNull(uri, "URI cannot be null");

		logger.debug("Reading object {}", uri);

		String profileName = uri.getHost();
		String container = Configurator.getBlobStoreProfile(profileName).getContainer();
		BlobStoreContext context = getBlobStoreContext(profileName);
		String path = relativize(uri.getPath());

		InputStream in = context.getBlobStore()
				.getBlob(container, path)
				.getPayload().getInput();

		return in;
	}

	/**
	 * Delete blob
	 * 
	 * @param uri
	 */
	public static void delete(URI uri)
	{
		// check if blob was stored for the message, skip if not
		if (uri == null) return; 

		logger.debug("Deleting object {}", uri);

		String profileName = uri.getHost();
		BlobStoreProfile profile = Configurator.getBlobStoreProfile(profileName);
		String path = relativize(uri.getPath());
		
		if (profile.getProvider().equals(PROVIDER_FILESYSTEM)) {
			// Following part is added as a replacement for jClouds delete due to
			// performance issues with jClouds

			String fileName = new StringBuilder(profile.getEndpoint())
					.append(File.separator).append(profile.getContainer())
					.append(File.separator).append(path).toString();
			
			File f = new File(fileName);

			// If file does not exist, skip 
			if (!f.exists())
				return;

			// Make sure the file is writable
			Assert.isTrue(f.canWrite(), "File is write protected: " + fileName);

			// Check if it is a directory
			Assert.isTrue(!f.isDirectory(), "Can't delete directory: " + fileName);

			// Attempt to delete it
			boolean success = f.delete();

			Assert.isTrue(success, "Deletion failed");
		} else {
			String container = profile.getContainer();
			BlobStoreContext context = getBlobStoreContext(profileName);
			context.getBlobStore().removeBlob(container, path);
		}
	}

	/**
	 * Build {@link BlobStoreContext} from blob profile
	 * 
	 * @param profile
	 *            blob store profile name
	 * @return
	 */
	private static BlobStoreContext getBlobStoreContext(String profileName)
	{
		if(blobStoreContexts.containsKey(profileName)) {
			return blobStoreContexts.get(profileName);
		} else {
			synchronized (BlobStoreProxy.class)
			{
				logger.debug("Creating new connection for '{}' blob store.", profileName);
				
				Properties properties = new Properties();
				BlobStoreProfile profile = Configurator.getBlobStoreProfile(profileName);
				ContextBuilder contextBuilder = ContextBuilder.newBuilder(profile.getProvider());

				if (profile.getProvider().equals(PROVIDER_FILESYSTEM)) {
					// use endpoint as fs basedir, see: http://code.google.com/p/jclouds/issues/detail?id=776
					properties.setProperty(FilesystemConstants.PROPERTY_BASEDIR, profile.getEndpoint());
					contextBuilder.endpoint(profile.getEndpoint());
					//properties.setProperty(PROPERTY_CREDENTIAL, "dummy");
				} else if (BlobStoreConstants.BLOBSTORE_PROVIDERS.contains(profile.getProvider())) {
					if (profile.getEndpoint() != null) {
						contextBuilder.endpoint(profile.getEndpoint());
					}
					if (profile.getApiversion() != null) {
						contextBuilder.apiVersion(profile.getApiversion());
					}
					if (profile.getIdentity() != null && profile.getCredential() != null) {
						contextBuilder.credentials(profile.getIdentity(), profile.getCredential());
					}
				} else {
					throw new UnsupportedOperationException(
							"Unsupported Blobstore provider: " + profile.getProvider());
				}

				// get a context with filesystem that offers the portable BlobStore api
				BlobStoreContext context = contextBuilder
						.overrides(properties)
						.modules(ImmutableSet.of(new JcloudsSlf4JLoggingModule()))
						.buildView(BlobStoreContext.class);

				// create container for transient store
				if(profile.getProvider().equals(PROVIDER_TRANSIENT)) {
					context.getBlobStore().createContainerInLocation(null, profile.getContainer());
				}

				blobStoreContexts.put(profileName, context);
			}

			return blobStoreContexts.get(profileName);
		}
	}

	/**
	 * Build {@link URI} from blobstore profile and blob path
	 * 
	 * @param profile blobstore profile name
	 * @param path blob path
	 * @return
	 */
	public static URI buildURI(final String profile, final String path)
	{
		// URI requires absolute path, add leading "/" if not already set
		String absolutePath = (path.charAt(0) == '/') ? path : "/" + path;

		try {
			return new URI(BlobStoreConstants.BLOB_URI_SCHEMA, profile, absolutePath, null);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Invalid blob profile or path: ", e);
		}
	}

	/**
	 * Make absolute path relative by dropping first forward-slash
	 * 
	 * @param path
	 * @return
	 */
	public static String relativize(String path) {
		String relativePath = (path.charAt(0) == '/') ? path.substring(1) : path;
		return relativePath;
	}

	/**
	 * Close all blob store connections
	 */
	public static synchronized void closeAll()
	{
		for (String profileName : blobStoreContexts.keySet()) {
			blobStoreContexts.get(profileName).close();
			blobStoreContexts.remove(profileName);
		}
	}
	
}
