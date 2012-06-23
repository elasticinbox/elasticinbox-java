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

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.mutation.Mutator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elasticinbox.config.Configurator;
import com.elasticinbox.core.AccountDAO;
import com.elasticinbox.core.MessageDAO;
import com.elasticinbox.core.blob.store.BlobStoreProxy;
import com.elasticinbox.core.cassandra.persistence.AccountPersistence;
import com.elasticinbox.core.cassandra.persistence.LabelCounterPersistence;
import com.elasticinbox.core.cassandra.persistence.LabelIndexPersistence;
import com.elasticinbox.core.cassandra.persistence.MessagePersistence;
import com.elasticinbox.core.cassandra.utils.BatchConstants;
import com.elasticinbox.core.model.Mailbox;
import com.elasticinbox.core.model.Message;
import com.elasticinbox.core.model.ReservedLabels;

public final class CassandraAccountDAO implements AccountDAO
{
	private final Keyspace keyspace;
	private final static StringSerializer strSe = StringSerializer.get();

	@SuppressWarnings("unused")
	private final static Logger logger = LoggerFactory
			.getLogger(CassandraAccountDAO.class);

	public CassandraAccountDAO(Keyspace keyspace) {
		this.keyspace = keyspace;
	}

	@Override
	public void add(final Mailbox mailbox) throws IOException, IllegalArgumentException
	{
		// currently nothing happens here

		//Map<String, Object> attributes = new HashMap<String, Object>();

		// TODO: make quota configurable on the mailbox level
		//attributes.put("quota:bytes", DEFAULT_QUOTA_BYTES);
		//attributes.put("quota:messages", DEFAULT_QUOTA_MESSAGES);
		//attributes.put("version", CURRENT_MAILBOX_VERSION);

		//AccountPersistence.set(mailbox.getId(), attributes);
	}

	@Override
	public void delete(final Mailbox mailbox) throws IOException
	{
		// purge all previously deleted objects
		// TODO: we should not instantinate here
		MessageDAO messageDAO = new CassandraMessageDAO(keyspace);
		messageDAO.purge(mailbox, new Date());

		// delete all objects from object store
		try {
			List<UUID> messageIds = null;
			UUID start = null;
	
			// loop until we delete all items
			do {
				// get all message ids
				messageIds = LabelIndexPersistence.get(mailbox.getId(),
						ReservedLabels.ALL_MAILS.getId(), start,
						BatchConstants.BATCH_READS, true);
	
				// get all message headers
				Map<UUID, Message> messages = 
						MessagePersistence.fetch(mailbox.getId(), messageIds, false);
	
				// delete message sources from object store
				for(UUID messageId : messages.keySet()) {
					BlobStoreProxy.delete(messages.get(messageId).getLocation());
				}
	
				// set start element for the next loop
				start = messageIds.isEmpty() ? null : messageIds.get(messageIds.size()-1);
			}
			while (messageIds.size() >= BatchConstants.BATCH_READS);
		} catch (Exception e) {
			throw new IOException(e);
		}

		// begin batch operation
		Mutator<String> m = createMutator(keyspace, strSe);

		// delete all MessageMetadata
		MessagePersistence.deleteAllMessages(m, mailbox.getId());

		// delete all indexes from IndexLabel
		LabelIndexPersistence.deleteIndexes(m, mailbox.getId());

		// delete all counters
		LabelCounterPersistence.deleteAll(m, mailbox.getId());

		// delete Account data
		AccountPersistence.delete(m, mailbox.getId());
		
		// commit batch operation
		m.execute();
	}

	/**
	 * Get quota (maximum) bytes for the given mailbox
	 *  
	 * @param mailbox
	 * @return
	 */
	public Long getQuotaBytes(final Mailbox mailbox) {
		// TODO: add account quota attribute check
		return Configurator.getDefaultQuotaBytes();
	}
	
	/**
	 * Get quota (maximum) messages for the given mailbox
	 * 
	 * @param mailbox
	 * @return
	 */
	public Long getQuotaCount(final Mailbox mailbox) {
		// TODO: add account quota attribute check
		return Configurator.getDefaultQuotaCount();
	}

}
