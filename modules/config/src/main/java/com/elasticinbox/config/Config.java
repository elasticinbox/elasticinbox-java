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

package com.elasticinbox.config;

import java.util.List;
import java.util.Map;

import com.elasticinbox.config.blob.BlobStoreProfile;

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

	// metadata storage settings
	public String metadata_storage_driver;
	public Boolean store_html_message;
	public Boolean store_plain_message;

	// Cassandra settings
	public List<String> cassandra_hosts;
	public Boolean cassandra_autodiscovery;
	public String cassandra_cluster_name;
	public String cassandra_keyspace;

	// Blob store settings
	public Map<String, BlobStoreProfile> blobstore_profiles;
	public String blobstore_write_profile;
}
