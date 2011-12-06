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

package com.elasticinbox.config.blob;

/**
 * This class stores access information of the specific blob store.
 * 
 * <p>Using combination of <code>BlobStoreProfile</code> and blob name (including
 * relative path) it is possible to uniquely identify any blob within
 * multi-cloud environment.
 * 
 * @author Rustam Aliyev
 */
public class BlobStoreProfile
{
	private String provider;
	private String endpoint;
	private String identity;
	private String credential;
	private String container;
	private String basedir;
	private String apiversion;

	public String getApiversion() {
		return apiversion;
	}

	public void setApiversion(String apiversion) {
		this.apiversion = apiversion;
	}

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public String getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	public String getIdentity() {
		return identity;
	}

	public void setIdentity(String identity) {
		this.identity = identity;
	}

	public String getCredential() {
		return credential;
	}

	public void setCredential(String credential) {
		this.credential = credential;
	}

	public String getContainer() {
		return container;
	}

	public void setContainer(String container) {
		this.container = container;
	}

	public String getBasedir() {
		return basedir;
	}

	public void setBasedir(String basedir) {
		this.basedir = basedir;
	}
}
