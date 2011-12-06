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

package com.elasticinbox.core.cassandra;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elasticinbox.config.Configurator;
import com.elasticinbox.core.AccountDAO;
import com.elasticinbox.core.DAOFactory;
import com.elasticinbox.core.LabelDAO;
import com.elasticinbox.core.MessageDAO;

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

	private final static Keyspace keyspace;

	public final static String CF_ACCOUNTS = "Accounts";
	public final static String CF_METADATA = "MessageMetadata";
	public final static String CF_LABEL_INDEX = "IndexLabels";
	public final static String CF_COUNTERS = "Counters";
	public final static int MAX_COLUMNS_PER_REQUEST = 500;

	public static Keyspace getKeyspace() {
	    return keyspace;
	}

	@Override
	public AccountDAO getAccountDAO() {
		return new CassandraAccountDAO();
	}

	@Override
	public MessageDAO getMessageDAO() {
		return new CassandraMessageDAO();
	}

	@Override
	public LabelDAO getLabelDAO() {
		return new CassandraLabelDAO();
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
		// FailoverPolicy fp = new FailoverPolicy(3, 200);

		// Consistency Level Policy
		ConsistencyLevelPolicy clp = new QuorumConsistencyLevel();

		// Use/Create keyspace and set Consistency Level
		keyspace = HFactory.createKeyspace(Configurator.getCassandraKeyspace(),
				cluster, clp, FailoverPolicy.ON_FAIL_TRY_ALL_AVAILABLE);
	}

}
