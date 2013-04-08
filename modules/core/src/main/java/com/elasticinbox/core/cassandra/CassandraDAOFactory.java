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

package com.elasticinbox.core.cassandra;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elasticinbox.config.Configurator;
import com.elasticinbox.core.AccountDAO;
import com.elasticinbox.core.DAOFactory;
import com.elasticinbox.core.LabelDAO;
import com.elasticinbox.core.MessageDAO;
import com.elasticinbox.core.cassandra.utils.QuorumConsistencyLevel;
import com.elasticinbox.core.cassandra.utils.Speed4jOpTimer;

import me.prettyprint.cassandra.service.CassandraHostConfigurator;
import me.prettyprint.cassandra.service.FailoverPolicy;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.ConsistencyLevelPolicy;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.factory.HFactory;

public final class CassandraDAOFactory extends DAOFactory
{
	private final static Logger logger = 
		LoggerFactory.getLogger(CassandraDAOFactory.class);

	private static Keyspace keyspace;

	public final static String CF_ACCOUNTS = "Accounts";
	public final static String CF_METADATA = "MessageMetadata";
	public final static String CF_BLOB = "MessageBlob";
	public final static String CF_LABEL_INDEX = "IndexLabels";
	public final static String CF_COUNTERS = "Counters";

	public static Keyspace getKeyspace() {
	    return keyspace;
	}

	/**
	 * This method sets system-wide keyspace. Should be used only for unit test
	 * injection.
	 * 
	 * @param k
	 */
	public static void setKeyspace(Keyspace k) {
		keyspace = k;
	}

	@Override
	public AccountDAO getAccountDAO() {
		return new CassandraAccountDAO(keyspace);
	}

	@Override
	public MessageDAO getMessageDAO() {
		return new CassandraMessageDAO(keyspace);
	}

	@Override
	public LabelDAO getLabelDAO() {
		return new CassandraLabelDAO(keyspace);
	}

	static
	{
		// Build connection string
		String hosts = StringUtils.join(Configurator.getCassandraHosts(), ",");
		logger.info("Connecting to cassandra hosts: {}", hosts);

		// Create host configuration
		CassandraHostConfigurator cassConfig = new CassandraHostConfigurator(hosts);

		// Load Balancing Policy
		//config.setLoadBalancingPolicy(new LeastActiveBalancingPolicy());

		// Auto-discover Cassandra hosts
		if(Configurator.isCassandraAutodiscoveryEnabled()) {
			cassConfig.setAutoDiscoverHosts(true);
		}

		// Enable performance logging
		if (Configurator.isPerformanceCountersEnabled()) {
			cassConfig.setOpTimer(new Speed4jOpTimer());
		}

		// Create cluster connections
		Cluster cluster = HFactory.getOrCreateCluster(
				Configurator.getCassandraClusterName(), cassConfig);

		// Failover Policy
		FailoverPolicy fp = new FailoverPolicy(Configurator.getCassandraHosts().size(), 0);

		// Consistency Level Policy
		ConsistencyLevelPolicy clp = new QuorumConsistencyLevel();

		// Use/Create keyspace and set Consistency Level
		keyspace = HFactory.createKeyspace(Configurator.getCassandraKeyspace(), cluster, clp, fp);
	}

}
