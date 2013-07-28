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

import static me.prettyprint.hector.api.factory.HFactory.createMutator;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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

import com.elasticinbox.core.AccountDAO;
import com.elasticinbox.core.MessageDAO;
import com.elasticinbox.core.OverQuotaException;
import com.elasticinbox.core.cassandra.persistence.LabelIndexPersistence;
import com.elasticinbox.core.cassandra.utils.QuorumConsistencyLevel;
import com.elasticinbox.core.message.id.MessageIdBuilder;
import com.elasticinbox.core.model.Address;
import com.elasticinbox.core.model.AddressList;
import com.elasticinbox.core.model.Mailbox;
import com.elasticinbox.core.model.Message;
import com.elasticinbox.core.model.ReservedLabels;

public class CassandraMessageDAOTest
{
	final static StringSerializer strSe = StringSerializer.get();
	final static UUIDSerializer uuidSe = UUIDSerializer.get();
	final static BytesArraySerializer byteSe = BytesArraySerializer.get();

	final static String KEYSPACE = "ElasticInbox";
	final static String MAILBOX = "testmessagedao@elasticinbox.com";
	Cluster cluster;
	Keyspace keyspace;
	CassandraDAOFactory dao;

	@Before
	public void setupCase() throws IllegalArgumentException, IOException
	{
		System.setProperty("elasticinbox.config", "../../config/elasticinbox.yaml");

		// Consistency Level Policy
		ConsistencyLevelPolicy clp = new QuorumConsistencyLevel();

		// Host config
		CassandraHostConfigurator conf = new CassandraHostConfigurator("127.0.0.1:9160");

		cluster = HFactory.getOrCreateCluster("TestCluster", conf);
		keyspace = HFactory.createKeyspace(KEYSPACE, cluster, clp);
		
		dao = new CassandraDAOFactory();
		CassandraDAOFactory.setKeyspace(keyspace);
		AccountDAO accountDAO = dao.getAccountDAO();
		accountDAO.add(new Mailbox(MAILBOX));
	}

	@After
	public void teardownCase() throws IOException {
		keyspace = null;
		cluster = null;

		AccountDAO accountDAO = dao.getAccountDAO();
		accountDAO.delete(new Mailbox(MAILBOX));
	}

	@Test
	public void testStaleMessageIdRemoval() throws IOException, OverQuotaException
	{
		Mailbox mailbox = new Mailbox(MAILBOX);

		Message message = getDummyMessage();
		message.addLabel(ReservedLabels.NOTIFICATIONS.getId());

		MessageDAO messageDAO = dao.getMessageDAO();
		List<UUID> validMessageIds = new ArrayList<UUID>();
		List<UUID> invalidMessageIds = new ArrayList<UUID>();

		// save message under different message ids, and store message ids
		for (int i=0; i<5; i++) {
			UUID messageId = new MessageIdBuilder().build();
			validMessageIds.add(messageId);
			messageDAO.put(mailbox, messageId, message, null);
		}
		
		// generate stale message ids
		for (int i=0; i<5; i++) {
			UUID messageId = new MessageIdBuilder().build();
			invalidMessageIds.add(messageId);
		}

		// add stale message ids to indexes only (without message metadata)
		Mutator<String> m = createMutator(keyspace, strSe);
		LabelIndexPersistence.add(m, mailbox.getId(), invalidMessageIds, message.getLabels());
		m.execute();

		// get all messages from NOTIFICATION label
		List<UUID> allMessageIds = messageDAO.getMessageIds(mailbox,
				ReservedLabels.NOTIFICATIONS.getId(), new MessageIdBuilder().build(), 100, true);

		// check if all message ids returned
		assertTrue(allMessageIds.containsAll(validMessageIds));
		assertTrue(allMessageIds.containsAll(invalidMessageIds));
		
		// delete all message ids
		messageDAO.delete(mailbox, allMessageIds);

		// get all messages from NOTIFICATION label
		allMessageIds = messageDAO.getMessageIds(mailbox,
				ReservedLabels.NOTIFICATIONS.getId(), new MessageIdBuilder().build(), 100, true);

		// check if all message ids deleted
		assertEquals(0, allMessageIds.size());
	}
	
	private static Message getDummyMessage()
	{
		Address address = new Address("Test", "test@elasticinbox.com");
		AddressList al = new AddressList(address);
		
		Message message = new Message();
		message.setFrom(al);
		message.setTo(al);
		message.setSize(1024L);
		message.setSubject("Test");
		message.setPlainBody("Test");
		message.addLabel(ReservedLabels.ALL_MAILS.getId());
		
		return message;
	}
}
