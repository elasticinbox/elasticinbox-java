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

import java.util.Set;

import com.google.common.collect.ImmutableSet;

/**
 * Blob store constants
 * 
 * @author Rustam Aliyev
 */
public final class BlobStoreConstants
{
	/** URI schema used to identify blobs. */
	public static final String URI_SCHEME = "blob";

	/** URI query parameter specifying compression */
	public static final String URI_PARAM_COMPRESSION = "c";

	/** URI query parameter specifying encryption key */
	public static final String URI_PARAM_ENCRYPTION_KEY = "e";

	/** URI query parameter specifying total block count */
	public static final String URI_PARAM_BLOCK_COUNT = "b";

	/** This suffix used to differentiate compressed files from not compressed. */
	@Deprecated
	public static final String COMPRESS_SUFFIX = ".dfl";

	/** Files smaller that this parameter should not be compressed. In bytes. */
	public static final Integer MIN_COMPRESS_SIZE = 256;

	/** Default block ID. Currently only single block DB operations supported. */
	public static final int DATABASE_DEFAULT_BLOCK_ID = 0;

	/** Threshold for switching from memory to file based buffering **/ 
	public static final int MAX_MEMORY_FILE_SIZE = 204800; // 200KB

	/**
	 * Providers that are independently configurable. Currently invisible form jClouds.
	 * 
	 * @see <a href="http://code.google.com/p/jclouds/issues/detail?id=657" />
	 */
	public static final Set<String> BLOBSTORE_PROVIDERS = ImmutableSet.of(
			"aws-s3", "cloudfiles-us", "cloudfiles-uk", "azureblob", "atmos",
			"synaptic-storage", "scaleup-storage", "cloudonestorage", "walrus",
			"googlestorage", "ninefold-storage", "scality-rs2",
			"hosteurope-storage", "tiscali-storage", "swift", "transient",
			"filesystem", "eucalyptus-partnercloud-s3");

	/**
	 * List of the providers supporting chunked/streamed data, where the size
	 * needn't be known in advance.
	 * <p>
	 * This is temporary workaround. Final solution should utilise
	 * {@link org.jclouds.blobstore.attr.BlobCapability} which is not currently
	 * available.
	 */
	public static final Set<String> CHUNKED_ENCODING_CAPABILITY = ImmutableSet
			.of("transient", "filesystem", "openstack");
}
