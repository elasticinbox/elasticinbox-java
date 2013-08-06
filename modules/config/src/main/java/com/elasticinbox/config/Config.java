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

package com.elasticinbox.config;

import java.util.List;
import java.util.Map;

import com.elasticinbox.config.blob.BlobStoreProfile;
import com.elasticinbox.config.crypto.EncryptionSettings;

public class Config
{
	// Default quota settings
	public Long mailbox_quota_bytes; // maximum mailbox size in bytes
	public Long mailbox_quota_count; // maximum message count in mailbox

	// JMX monitoring
	public Boolean enable_performance_counters;
	public Integer performance_counters_interval;

	// LMTP settings
	public Integer lmtp_port;
	public Integer lmtp_max_connections;
	public boolean lmtp_enable_pop3;

	// POP3 settings
	public Integer pop3_port;
	public Integer pop3_max_connections;

	// Database settings
	public String database_driver;
	public Boolean store_html_message;
	public Boolean store_plain_message;
	public Long database_blob_max_size;

	// Cassandra settings
	public List<String> cassandra_hosts;
	public Boolean cassandra_autodiscovery;
	public String cassandra_cluster_name;
	public String cassandra_keyspace;

	// Blob store settings
	public Map<String, BlobStoreProfile> blobstore_profiles;
	public String blobstore_write_profile;
	public Boolean blobstore_enable_compression;
	
	// Blob store encryption
	public Boolean remote_blobstore_enable_encryption = false;
	public Boolean local_blobstore_enable_encryption = false;
	public String default_encryption_key = null;

	// Encryption options
	public EncryptionSettings encryption = new EncryptionSettings();
	
	// Meta store encryption
	// Currently uses the same key as the blob store
	public Boolean metastore_enable_encryption = false;
}
