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

package com.elasticinbox.core.cassandra.utils;

import static com.elasticinbox.core.cassandra.CassandraDAOFactory.CF_LABEL_INDEX;
import static me.prettyprint.hector.api.factory.HFactory.createColumn;
import static me.prettyprint.hector.api.factory.HFactory.createSliceQuery;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertNotNull;

import java.util.HashSet;
import java.util.UUID;

import me.prettyprint.cassandra.serializers.BytesArraySerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.serializers.UUIDSerializer;
import me.prettyprint.cassandra.service.CassandraHostConfigurator;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.ConsistencyLevelPolicy;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.ColumnSlice;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.SliceQuery;

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
	final static int LABEL_T1 = 5555;
	final static int LABEL_T2 = 5000;
	final static String MAILBOX = "throttling@elasticinbox.com";
	final static String KEY_T1 = MAILBOX + ":" + LABEL_T1;
	final static String KEY_T2 = MAILBOX + ":" + LABEL_T2;
	Cluster cluster;
	Keyspace keyspace;

	@Before
	public void setupCase()
	{
		// Consistency Level Policy
		ConsistencyLevelPolicy clp = new QuorumConsistencyLevel();

		// Host config
		CassandraHostConfigurator conf = new CassandraHostConfigurator("127.0.0.1:9160");

		cluster = HFactory.getOrCreateCluster("TestCluster", conf);
		keyspace = HFactory.createKeyspace(KEYSPACE, cluster, clp);
		
		// clenaup from previous runs
		Mutator<String> m = new ThrottlingMutator<String>(keyspace, strSe, 50, 500L);
		m.addDeletion(KEY_T1, CF_LABEL_INDEX, null, strSe);
		m.addDeletion(KEY_T2, CF_LABEL_INDEX, null, strSe);
		m.execute();
	}

	@After
	public void teardownCase()
	{
		keyspace = null;
		cluster = null;
	}

	/**
	 * Test throttler's delay functionality.
	 */
	@Test
	public void testThrottlingMutatorDelay()
	{
		// throttle at 100 ops/ 500 ms
		ThrottlingMutator<String> m = new ThrottlingMutator<String>(keyspace, strSe, 100, 500L);

		long ts = System.currentTimeMillis();

		// should take 1 sec to insert 200 cols at 5ms rate
		for (int i = 0; i < 201; i++)
		{
			UUID uuid = new MessageIdBuilder().build();
			m.addInsertion(KEY_T1, CF_LABEL_INDEX, createColumn(uuid, new byte[0], uuidSe, byteSe));
			m.executeIfFull();
		}

		m.execute();

		long elapsed = System.currentTimeMillis() - ts;

		// check if it took more than 1 sec and no more than 1.2 sec
		assertThat(elapsed, greaterThan(1000L));
		assertThat(elapsed, lessThan(1200L));
	}
	
	@Test
	public void testThrottlingMutatorConsistency()
	{
		int sampleCount = 250;
		
		// throttle at 50 ops/ 100 ms
		ThrottlingMutator<String> m = new ThrottlingMutator<String>(keyspace, strSe, 100, 500L);

		HashSet<UUID> messageIds = new HashSet<UUID>();
		final byte[] value = "consistent".getBytes();

		// STEP: add samples
		for (int i = 0; i < sampleCount; i++)
		{
			UUID uuid = new MessageIdBuilder().build();
			m.addInsertion(KEY_T2, CF_LABEL_INDEX, createColumn(uuid, value, uuidSe, byteSe));
			messageIds.add(uuid);
			m.executeIfFull();
		}

		m.execute();
		
		// STEP: validate additions
		SliceQuery<String, UUID, byte[]> q = 
				createSliceQuery(keyspace, strSe, uuidSe, byteSe);
		q.setColumnFamily(CF_LABEL_INDEX);
		q.setKey(KEY_T2);
		q.setRange(null, null, false, 500);

		QueryResult<ColumnSlice<UUID, byte[]>> r = q.execute();

		for (HColumn<UUID, byte[]> c : r.get().getColumns())
		{
			assertNotNull(c);
			assertNotNull(c.getValue());
			assertThat(messageIds, hasItem(c.getName()));
			assertThat(value, equalTo(c.getValue()));
		}
		
		assertThat(sampleCount, equalTo(r.get().getColumns().size()));
		
		// STEP: delete samples
		for (UUID uuid : messageIds)
		{
			m.addDeletion(KEY_T2, CF_LABEL_INDEX, uuid, uuidSe);
			m.executeIfFull();
		}

		m.execute();
		
		// STEP: validate deletions 
		r = q.execute();

		assertThat(0, equalTo(r.get().getColumns().size()));
	}
}
