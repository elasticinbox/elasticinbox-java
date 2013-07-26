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
import com.elasticinbox.config.crypto.SymmetricKeyStorage;

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
	private static SymmetricKeyStorage keyManager;

	/**
	 * Inspect the classpath to find storage configuration file
	 */
	static URI getStorageConfigURL() throws ConfigurationException
	{
		URI uri;
		String configUrl = System.getProperty("elasticinbox.config");

		if (configUrl == null) {
			configUrl = DEFAULT_CONFIGURATION;
		}

		try {
			File file = new File(configUrl);
			if(!file.canRead()) {
				throw new ConfigurationException("Cannot read config file: " + configUrl);
			}
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
			File configFile;
			try {
				configFile = new File(uri);
				input = new FileInputStream(configFile);
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

			// verify max database blob size
			if(conf.database_blob_max_size > DatabaseConstants.MAX_BLOB_SIZE) {
				throw new ConfigurationException("Blobs larger than "
						+ DatabaseConstants.MAX_BLOB_SIZE + " bytes cannot be stored in the database");
			}

			// verify that blobstore profile name is not conflicting with internal name
			if (conf.blobstore_profiles.containsKey(DatabaseConstants.DATABASE_PROFILE)) {
				throw new ConfigurationException("BlobStore profile name cannot be '"
						+ DatabaseConstants.DATABASE_PROFILE + "'");
			}

			// verify that default blobstore profile exists
			if (!conf.blobstore_profiles.containsKey(conf.blobstore_write_profile)) {
				throw new ConfigurationException("Default BlobStore Profile '"
						+ conf.blobstore_write_profile + "' not found");
			}

			if (conf.encryption.keystore != null)
			{
				// keystore path is relative to the config file
				File keystoreFile = new File(configFile.getParent() + "/" + conf.encryption.keystore);
				// initialise symmetric key storage
				keyManager = new SymmetricKeyStorage(keystoreFile, conf.encryption.keystore_password);
	
				// verify that default blobstore encryption key exists
				if (!keyManager.containsKey(conf.blobstore_default_encryption_key)) {
					throw new ConfigurationException("Default encryption key for BlobStore '"
							+ conf.blobstore_default_encryption_key + "' not found");
				}
				
			} else {
				// initialize empty key store
				keyManager = new SymmetricKeyStorage();
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

	public static boolean isLmtpPop3Enabled() {
		return conf.lmtp_enable_pop3;
	}

	public static Integer getPop3Port() {
		return conf.pop3_port;
	}

	public static Integer getPop3MaxConnections() {
		return conf.pop3_max_connections;
	}

	public static String getDatabaseDriver() {
		return (conf.database_driver.equalsIgnoreCase("cassandra")) ? "CASSANDRA" : "UNKNOWN";
	}
	
	public static Long getDatabaseBlobMaxSize() {
		return conf.database_blob_max_size;
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

	/**
	 * Compress BLOB before writing to the object store
	 * 
	 * @return
	 */
	public static Boolean isBlobStoreCompressionEnabled() {
		return conf.blobstore_enable_compression;
	}
	
	public static Boolean isBlobStoreEncryptionEnabled() {
		return conf.blobstore_enable_encryption;
	}

	public static String getBlobStoreDefaultEncryptionKeyAlias() {
		return conf.blobstore_default_encryption_key;
	}

	public static java.security.Key getEncryptionKey(String alias) {
		return keyManager.getKey(alias);
	}

	public static java.security.Key getBlobStoreDefaultEncryptionKey() {
		return keyManager.getKey(conf.blobstore_default_encryption_key);
	}

	public static boolean isMetaStoreEncryptionEnabled() {
		return conf.metastore_enable_encryption;
	}
	

	public static java.security.Key getMetaStoreDefaultEncryptionKey() {
		return keyManager.getKey(conf.blobstore_default_encryption_key);
	}

}
