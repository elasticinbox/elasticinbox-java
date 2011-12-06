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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.YAMLException;

import com.elasticinbox.common.utils.Assert;
import com.elasticinbox.config.blob.BlobStoreProfile;

/**
 * Main configuration class which loads options from YAML and provides to the
 * rest of the application.
 * 
 * Path to the YAML config file should be provided in the 
 * <code>elasticinbox.config</code> system property.
 * 
 * @author Rustam Aliyev
 */
public class Configurator
{
	private static final Logger logger = LoggerFactory.getLogger(Configurator.class);
	private static final String DEFAULT_CONFIGURATION = "elasticinbox.yaml";
	private static Config conf;

	/**
	 * Inspect the classpath to find storage configuration file
	 */
	static URI getStorageConfigURL() throws ConfigurationException
	{
		URI uri;
		String configUrl = System.getProperty("elasticinbox.config");

		if (configUrl == null)
			configUrl = DEFAULT_CONFIGURATION;

		try {
			File file = new File(configUrl);
			if(!file.canRead())
				throw new ConfigurationException("Cannot read config file: " + configUrl);
			uri = file.toURI();
		} catch (Exception e) {
			logger.error("Error opening logfile: ", e);
			throw new ConfigurationException("Cannot locate " + configUrl);
		}

		return uri;
	}

	static
	{
		try {
			URI uri = getStorageConfigURL();

			InputStream input = null;
			try {
				File f = new File(uri);
				input = new FileInputStream(f);
			} catch (IOException e) {
				logger.error("Cannot read config file: {}", e.getMessage());
				// getStorageConfigURL should have ruled this out
				throw new AssertionError(e);
			}

			Constructor constructor = new Constructor(Config.class);
			constructor.addTypeDescription(new TypeDescription(Config.class));
			Yaml yaml = new Yaml(constructor);
			conf = (Config) yaml.load(input);

			// TODO: add config verification here
			// ...

			// verify that default blobstore profile exists
			if (!conf.blobstore_profiles.containsKey(conf.blobstore_write_profile)) {
				throw new ConfigurationException("BlobStore Profile '"
						+ conf.blobstore_write_profile + "' not found");
			}
		} catch (ConfigurationException e) {
			logger.error("Fatal configuration error", e);
			System.err.println(e.getMessage()
					+ "\nFatal configuration error; unable to start server. See log for stacktrace.");
			System.exit(1);
		} catch (YAMLException e) {
			logger.error("Fatal configuration error error", e);
			System.err.println(e.getMessage()
					+ "\nInvalid yaml; unable to start server. See log for stacktrace.");
			System.exit(1);
		}
	}

	public static Integer getLmtpPort() {
		return conf.lmtp_port;
	}

	public static Integer getLmtpMaxConnections() {
		return conf.lmtp_max_connections;
	}

	public static String getMetadataStorageDriver() {
		return (conf.metadata_storage_driver.equalsIgnoreCase("cassandra")) ? "CASSANDRA" : "UNKNOWN";
	}

	public static List<String> getCassandraHosts() {
		return conf.cassandra_hosts;
	}
	
	public static Boolean isCassandraAutodiscoveryEnabled() {
		return conf.cassandra_autodiscovery;
	}

	public static String getCassandraClusterName() {
		return conf.cassandra_cluster_name;
	}

	public static String getCassandraKeyspace() {
		return conf.cassandra_keyspace;
	}

	public static Long getDefaultQuotaBytes() {
		return conf.mailbox_quota_bytes;
	}
	
	public static Long getDefaultQuotaCount() {
		return conf.mailbox_quota_count;
	}
	
	public static Boolean isPerformanceCountersEnabled() {
		return conf.enable_performance_counters;
	}
	
	public static Integer getPerformanceCountersInterval() {
		return conf.performance_counters_interval;
	}
	
	/**
	 * Store HTML message body within metadata
	 *  
	 * @return
	 */
	public static Boolean isStoreHtmlWithMetadata() {
		return conf.store_html_message;
	}

	/**
	 * Store plain text message body within metadata
	 *  
	 * @return
	 */
	public static Boolean isStorePlainWithMetadata() {
		return conf.store_plain_message;
	}

	/**
	 * Get {@link BlobStoreProfile} based on profile name
	 * 
	 * @param profileName
	 * @return
	 */
	public static BlobStoreProfile getBlobStoreProfile(String profileName)
	{
		Assert.isTrue(conf.blobstore_profiles.containsKey(profileName),
				"Unknown BlobStore Profile: " + profileName);
		return conf.blobstore_profiles.get(profileName);
	}

	/**
	 * Get blobstore profile name for storing data
	 * 
	 * @return
	 */
	public static String getBlobStoreWriteProfileName() {
		return conf.blobstore_write_profile;
	}
	
}
