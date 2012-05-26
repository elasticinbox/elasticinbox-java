package com.elasticinbox.core;

import static com.elasticinbox.core.cassandra.CassandraDAOFactory.CF_LABEL_INDEX;
import static me.prettyprint.hector.api.factory.HFactory.createColumn;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;

import java.util.UUID;

import me.prettyprint.cassandra.serializers.BytesArraySerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.serializers.UUIDSerializer;
import me.prettyprint.cassandra.service.CassandraHostConfigurator;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.ConsistencyLevelPolicy;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.elasticinbox.core.cassandra.utils.QuorumConsistencyLevel;
import com.elasticinbox.core.cassandra.utils.ThrottlingMutator;
import com.elasticinbox.core.message.id.MessageIdBuilder;

public class ThrottlingMutatorTest
{
	final static StringSerializer strSe = StringSerializer.get();
	final static UUIDSerializer uuidSe = UUIDSerializer.get();
	final static BytesArraySerializer byteSe = BytesArraySerializer.get();

	final static String KEYSPACE = "ElasticInbox";
	final static String MAILBOX = "throttling@elasticinbox.com";
	final static int LABEL = 5555;
	Cluster cluster;
	Keyspace keyspace;

	@Before
	public void setupCase() {
		// Consistency Level Policy
		ConsistencyLevelPolicy clp = new QuorumConsistencyLevel();

		// Host config
		CassandraHostConfigurator conf = new CassandraHostConfigurator("127.0.0.1:9160");

		cluster = HFactory.getOrCreateCluster("TestCluster", conf);
		keyspace = HFactory.createKeyspace(KEYSPACE, cluster, clp);
	}

	@After
	public void teardownCase() {
		keyspace = null;
		cluster = null;
	}

	@Test
	public void testThrottlingMutatorDelay()
	{
		// throttle at 100 ops/ 500 ms
		Mutator<String> m = new ThrottlingMutator<String>(keyspace, strSe, 100, 500L);

		UUID uuid;
		String indexKey;
		long ts = System.currentTimeMillis();

		// should take 1 sec to insert 200 cols at 5ms rate
		for (int i = 0; i < 201; i++) {
			uuid = new MessageIdBuilder().build();
			indexKey = MAILBOX + ":" + LABEL;
			m.addInsertion(indexKey, CF_LABEL_INDEX, createColumn(uuid, new byte[0], uuidSe, byteSe));
		}

		m.execute();

		long elapsed = System.currentTimeMillis() - ts;

		// check if it took more than 1 sec and no more than 1.2 sec
		assertThat(elapsed, greaterThan(1000L));
		assertThat(elapsed, lessThan(1200L));
	}
}
