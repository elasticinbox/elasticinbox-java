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

package com.elasticinbox.core.blob;

import static com.elasticinbox.core.blob.store.BlobStoreConstants.*;

import java.net.URI;
import java.net.URISyntaxException;

import me.prettyprint.cassandra.utils.Assert;

import org.jclouds.http.Uris;
import org.jclouds.http.Uris.UriBuilder;
import org.jclouds.http.utils.Queries;

import com.google.common.collect.Multimap;

/**
 * This class provides Blob URI builder and parser tools.
 * <p>
 * Blobs URI contains following parts:
 * <ul>
 * <li>Schema - always "blob"</li>
 * <li>Host - blob storage profile name</li>
 * <li>Path - path where blob stored</li>
 * <li>Query parameters - various blob attributes such as compression algorithm,
 * encryption key, block count, etc.</li>
 * </ul>
 * <p>
 * Examples:
 * <p>
 * <code>blob://db/f1ca99e0-99a0-11e2-95f0-040cced3bd7a?c=dfl&b=1</code>
 * <p>
 * <code>blob://aws3-bucket/f1ca99e0-99a0-11e2-95f0-040cced3bd7a:myemail?c=dfl&e=ekey2</code> 
 * 
 * @author Rustam Aliyev
 */
public class BlobURI
{
	private String profile;
	private String name;
	private String compression;
	private String encryptionKey;
	private Integer blockCount;

	/**
	 * Blob storage profile name.
	 * @return
	 */
	public String getProfile() {
		return profile;
	}

	/**
	 * Blob name
	 * 
	 * @return
	 */
	public String getName() {
		return BlobUtils.relativize(name);
	}

	/**
	 * Compression type (e.g. {@code "dfl"} for Deflate)
	 * 
	 * @return
	 */
	public String getCompression() {
		return compression;
	}

	/**
	 * Encryption key name
	 * 
	 * @return
	 */
	public String getEncryptionKey() {
		return encryptionKey;
	}

	/**
	 * Total count of blocks
	 * @return
	 */
	public Integer getBlockCount() {
		return blockCount;
	}

	/**
	 * Blob storage profile name.
	 * <p>
	 * This parameter will be stored as a host of a URI.
	 * 
	 * @param profile
	 * @return
	 */
	public BlobURI setProfile(String profile) {
		this.profile = profile;
		return this;
	}

	/**
	 * Blob name
	 * <p>
	 * This parameter will be stored as a path of a URI. 
	 * 
	 * @param path
	 * @return
	 */
	public BlobURI setName(String path)
	{
		// URI requires absolute path, add leading "/" if not already set
		this.name = (path.charAt(0) == '/') ? path : "/" + path;
		return this;
	}

	/**
	 * Compression type (e.g. {@code "dfl"} for Deflate)
	 * <p>
	 * This parameter will be stored in the query part of a URI.
	 * 
	 * @param compressionType
	 * @return
	 */
	public BlobURI setCompression(String compressionType) {
		this.compression = compressionType;
		return this;
	}

	/**
	 * Encryption key name which can was used for blob encryption.
	 * <p>
	 * This parameter will be stored in the query part of a URI.
	 * 
	 * @param encryptionKey
	 * @return
	 */
	public BlobURI setEncryptionKey(String encryptionKey) {
		this.encryptionKey = encryptionKey;
		return this;
	}

	/**
	 * Total number of blocks blob was split into.
	 * <p>
	 * This parameter will be stored in the query part of a URI.
	 * 
	 * @param blockCount
	 * @return
	 */
	public BlobURI setBlockCount(Integer blockCount) {
		this.blockCount = blockCount;
		return this;
	}

	/**
	 * Parse URI and populate fields
	 * 
	 * @return
	 */
	public BlobURI fromURI(URI uri)
	{
		Assert.notNull(uri, "URI cannot be null");
		Assert.isTrue(uri.getScheme().equals(URI_SCHEME), "Invalid URI scheme specified for blob: " + uri.getScheme());
		Assert.notNull(uri.getHost(), "Invalid storage profile specified, unable to parse URI" + uri.toString());
		Assert.notNull(uri.getPath(), "Invalid blob name provided, unable to parse URI: " + uri.toString());

		this.profile = uri.getHost();
		this.name = uri.getPath();

		Multimap<String, String> queryParams = Queries.queryParser().apply(uri.getQuery());
		
		if (queryParams.containsKey(URI_PARAM_ENCRYPTION_KEY)) {
			this.encryptionKey = queryParams.get(URI_PARAM_ENCRYPTION_KEY).toArray(new String[0])[0];
		}

		if (queryParams.containsKey(URI_PARAM_COMPRESSION)) {
			this.compression = queryParams.get(URI_PARAM_COMPRESSION).toArray(new String[0])[0];
		}

		if (queryParams.containsKey(URI_PARAM_BLOCK_COUNT)) {
			this.blockCount = Integer.parseInt(queryParams.get(URI_PARAM_BLOCK_COUNT).toArray(new String[0])[0]);
		}

		return this;
	}
	
	/**
	 * Build blob {@link URI} from given parameters
	 * <p>
	 * Examples:
	 * <p>
	 * <code>blob://db/f1ca99e0-99a0-11e2-95f0-040cced3bd7a?c=dfl&b=1</code>
	 * <p>
	 * <code>blob://aws3-bucket/f1ca99e0-99a0-11e2-95f0-040cced3bd7a:myemail?c=dfl&e=ekey2</code> 
	 */
	public URI buildURI()
	{
		Assert.notNull(this.profile, "Blob storage profile must be specified");
		Assert.notNull(this.name, "Blob name must be provided");

		URI baseuri;
		
		try {
			baseuri = new URI(URI_SCHEME, this.profile, this.name, null);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Invalid blob profile or path: ", e);
		}

		UriBuilder ub = Uris.uriBuilder(baseuri);

		if (this.compression != null) {
			ub.addQuery(URI_PARAM_COMPRESSION, this.compression);
		}
		
		if (this.encryptionKey != null) {
			ub.addQuery(URI_PARAM_ENCRYPTION_KEY, this.encryptionKey);
		}

		if (this.blockCount != null) {
			ub.addQuery(URI_PARAM_BLOCK_COUNT, Integer.toString(this.blockCount));
		}

		return ub.build();
	}
}