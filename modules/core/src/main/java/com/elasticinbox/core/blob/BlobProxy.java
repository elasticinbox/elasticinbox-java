/**
 * Copyright (c) 2011 Optimax Software Ltd
 * 
 * This file is part of ElasticInbox.
 * 
 * ElasticInbox is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 2 of the License, or (at your option) any later
 * version.
 * 
 * ElasticInbox is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * ElasticInbox. If not, see <http://www.gnu.org/licenses/>.
 */

package com.elasticinbox.core.blob;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jclouds.Constants;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.BlobStoreContextFactory;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.filesystem.reference.FilesystemConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elasticinbox.common.utils.Assert;
import com.elasticinbox.common.utils.IOUtils;
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
public final class BlobProxy
{
	private final static Logger logger = 
			LoggerFactory.getLogger(BlobProxy.class);

	public static final String BLOB_URI_SCHEMA = "blob";

	/**
	 * Providers that are independently configurable. Currently invisible form jClouds.
	 * 
	 * @see <a href="http://code.google.com/p/jclouds/issues/detail?id=657" />
	 */
	public static final Set<String> BLOBSTORE_PROVIDERS = ImmutableSet.of("aws-s3",
			"cloudfiles-us", "cloudfiles-uk", "azureblob", "atmos",
			"synaptic-storage", "scaleup-storage", "cloudonestorage", "walrus",
			"googlestorage", "ninefold-storage", "scality-rs2",
			"hosteurope-storage", "tiscali-storage", "swift", "transient",
			"filesystem", "eucalyptus-partnercloud-s3");

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
	 */
	public static URI write(final String blobName, InputStream in, final Long size)
	{
		Assert.notNull(in, "No data to store");

		URI uri = null;
		String profileName = Configurator.getBlobStoreWriteProfileName(); 
		String container = Configurator.getBlobStoreProfile(profileName).getContainer();
		BlobStoreContext context = getBlobStoreContext(profileName);

		logger.debug("Storing blob {} on {}", blobName, profileName);

		BlobStore blobStore = context.getBlobStore();

		// add blob
		Blob blob = blobStore.blobBuilder(blobName).payload(in).contentLength(size).build();
		blobStore.putBlob(container, blob);

		uri = buildURI(profileName, blobName);

		return uri;
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
			synchronized (BlobProxy.class)
			{
				logger.debug("Creating new connection for '{}' blob store.", profileName);

				Properties properties = new Properties();
				BlobStoreProfile profile = Configurator.getBlobStoreProfile(profileName);

				if (profile.getProvider().equals(PROVIDER_FILESYSTEM)) {
					// use endpoint as fs basedir, see: http://code.google.com/p/jclouds/issues/detail?id=776
					properties.setProperty(FilesystemConstants.PROPERTY_BASEDIR, profile.getEndpoint());
					properties.setProperty(Constants.PROPERTY_CREDENTIAL, "dummy");
				} else if (BLOBSTORE_PROVIDERS.contains(profile.getProvider())) {
					if (profile.getEndpoint() != null) {
						properties.setProperty(
								profile.getProvider() + ".endpoint", profile.getEndpoint());
					}
					if (profile.getApiversion() != null) {
						properties.setProperty(
								profile.getProvider() + ".apiversion", profile.getApiversion());
					}
				} else {
					throw new UnsupportedOperationException(
							"Unsupported Blobstore provider: " + profile.getProvider());
				}

				// get a context with filesystem that offers the portable BlobStore api
				BlobStoreContext context = new BlobStoreContextFactory().createContext(
						profile.getProvider(), profile.getIdentity(), profile.getCredential(),
						ImmutableSet.of(new JcloudsSlf4JLoggingModule()), properties);

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
			return new URI(BLOB_URI_SCHEMA, profile, absolutePath, null);
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
	private static String relativize(String path) {
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
