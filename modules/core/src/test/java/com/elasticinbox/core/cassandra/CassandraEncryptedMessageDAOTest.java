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
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import java.io.IOException;
import java.util.Date;
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
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import com.elasticinbox.config.Configurator;
import com.elasticinbox.core.AccountDAO;
import com.elasticinbox.core.MessageDAO;
import com.elasticinbox.core.OverQuotaException;
import com.elasticinbox.core.cassandra.utils.QuorumConsistencyLevel;
import com.elasticinbox.core.message.id.MessageIdBuilder;
import com.elasticinbox.core.model.Address;
import com.elasticinbox.core.model.AddressList;
import com.elasticinbox.core.model.Mailbox;
import com.elasticinbox.core.model.Message;
import com.elasticinbox.core.model.ReservedLabels;

/*
 * @author itembase GmbH, John Wiesel <jw@itembase.biz>
 */
public class CassandraEncryptedMessageDAOTest {
	final static StringSerializer strSe = StringSerializer.get();
	final static UUIDSerializer uuidSe = UUIDSerializer.get();
	final static BytesArraySerializer byteSe = BytesArraySerializer.get();

	final static String KEYSPACE = "elasticinbox";
	final static String MAILBOX = "testmessagedao@elasticinbox.com";
	Cluster cluster;
	Keyspace keyspace;
	CassandraDAOFactory dao;

	@Before
	public void setupCase() throws IllegalArgumentException, IOException {
		System.setProperty("elasticinbox.config",
				"../../config/elasticinbox.yaml");

		// Consistency Level Policy
		ConsistencyLevelPolicy clp = new QuorumConsistencyLevel();

		// Host config
		CassandraHostConfigurator conf = new CassandraHostConfigurator();

		cluster = HFactory.getOrCreateCluster("Elastic", conf);
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
	public void testEncryptedMessageStorage() throws IOException,
			OverQuotaException {

		/*
		 * if meta storage encryption is not enabled, there is nothing to test
		 * here
		 */
		Assume.assumeTrue(Configurator.isMetaStoreEncryptionEnabled());

		Mailbox mailbox = new Mailbox(MAILBOX);

		Message message = getDummyMessage();

		MessageDAO messageDAO = dao.getMessageDAO();

		UUID messageId = new MessageIdBuilder().build();
		messageDAO.put(mailbox, messageId, message, null);

		Mutator<String> m = createMutator(keyspace, strSe);
		m.execute();

		Message readMessage = messageDAO.getParsed(mailbox, messageId);

		/* compare original message and encrypted message */
		assertThat(message.getPlainBody(),
				not(getDummyMessage().getPlainBody()));
		assertThat(message.getFrom().getDisplayString(), not(getDummyMessage()
				.getFrom().getDisplayString()));
		assertThat(message.getTo().getDisplayString(), not(getDummyMessage()
				.getTo().getDisplayString()));
		assertThat(message.getSubject(), not(getDummyMessage().getSubject()));
		assertThat(getDummyMessage().getDate().compareTo(message.getDate()),
				is(1));
		assertThat(message.getPlainBody(),
				not(getDummyMessage().getPlainBody()));
		assertThat(message.getHtmlBody(), is(getDummyMessage().getHtmlBody()));

		/* compare original message and decrypted message */
		assertThat(readMessage.getFrom().getDisplayString(),
				is(getDummyMessage().getFrom().getDisplayString()));
		assertThat(readMessage.getTo().getDisplayString(), is(getDummyMessage()
				.getTo().getDisplayString()));
		assertThat(readMessage.getCc(), is(getDummyMessage().getCc()));
		assertThat(readMessage.getBcc(), is(getDummyMessage().getBcc()));
		assertThat(readMessage.getSubject(), is(getDummyMessage().getSubject()));
		assertThat(
				getDummyMessage().getDate().compareTo(readMessage.getDate()),
				is(1));
		assertThat(readMessage.getPlainBody(), is(getDummyMessage()
				.getPlainBody()));
		assertThat(getDummyMessage().getHtmlBody(),
				is(readMessage.getHtmlBody()));

		// delete all message ids
		messageDAO.delete(mailbox, messageId);

	}

	private static Message getDummyMessage() {
		Address address = new Address("Test", "test@elasticinbox.com");
		AddressList al = new AddressList(address);

		Message message = new Message();
		message.setFrom(al);
		message.setTo(al);
		message.setSize(1024L);
		message.setSubject("Test");
		message.setPlainBody("Test");
		message.setDate(new Date());
		message.addLabel(ReservedLabels.ALL_MAILS);

		return message;
	}
}
