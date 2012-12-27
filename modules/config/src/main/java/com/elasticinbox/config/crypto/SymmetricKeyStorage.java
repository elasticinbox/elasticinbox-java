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

package com.elasticinbox.config.crypto;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elasticinbox.config.ConfigurationException;

public class SymmetricKeyStorage
{
	private static final Logger logger = 
			LoggerFactory.getLogger(SymmetricKeyStorage.class);

	private final static String KEYSTORE_TYPE = "JCEKS";

	private static KeyStore ks;
	private static ConcurrentHashMap<String, SecretKey> keys = new ConcurrentHashMap<String, SecretKey>(5);
	
	public SymmetricKeyStorage()
	{
		// by default do not initialize keys
	}

	public SymmetricKeyStorage(final File keystore, final String password) 
			throws ConfigurationException
	{
		this.loadAllKeys(keystore, password);
	}

	/**
	 * Get symmetric key
	 * 
	 * @param keyAlias Key alias
	 * @return
	 */
	public SecretKey getKey(String alias)
	{
		checkArgument(!keys.isEmpty(), "No symmetric keys found in keystore.");
		checkArgument(keys.containsKey(alias), "Symmetric key alias \"%s\" not found in keystore.", alias);

		return keys.get(alias);
	}

	/**
	 * Check if key exists
	 * 
	 * @param keyAlias
	 * @return
	 */
	public boolean containsKey(String alias)
	{
		return keys.containsKey(alias);
	}

	/**
	 * Load all symmetric keys into memory
	 * 
	 * @param keystore Keystore file
	 * @param password Password
	 * @throws ConfigurationException 
	 */
	private void loadAllKeys(final File keystore, final String password) throws ConfigurationException
	{
		try {
			ks = KeyStore.getInstance(KEYSTORE_TYPE);

			FileInputStream fis = null;
			try {
			    fis = new FileInputStream(keystore);
			    ks.load(fis, password.toCharArray());
			} finally {
			    if (fis != null) {
			        fis.close();
			    }
			}

			Enumeration<String> aliases = ks.aliases();

			while(aliases.hasMoreElements())
			{
				String alias = aliases.nextElement();

				if (ks.isKeyEntry(alias))
				{
					SecretKey sk = (SecretKey) ks.getKey(alias, password.toCharArray());
					keys.put(alias, sk);
					logger.debug("Loaded encyption key {} from keystore.", alias);
				}
			}
		} catch (IOException ioe) {
			throw new ConfigurationException("Unable to access key store: " + ioe.getMessage(), ioe);
		} catch (GeneralSecurityException gse) {
			throw new ConfigurationException("Unable to load encryption keys: " + gse.getMessage(), gse);
		}
	}
}
