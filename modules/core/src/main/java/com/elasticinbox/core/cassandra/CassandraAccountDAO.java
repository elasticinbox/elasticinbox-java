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

import static me.prettyprint.hector.api.factory.HFactory.createMutator;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
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
import com.elasticinbox.core.blob.BlobProxy;
import com.elasticinbox.core.cassandra.persistence.AccountPersistence;
import com.elasticinbox.core.cassandra.persistence.LabelCounterPersistence;
import com.elasticinbox.core.cassandra.persistence.LabelIndexPersistence;
import com.elasticinbox.core.cassandra.persistence.MessagePersistence;
import com.elasticinbox.core.model.Label;
import com.elasticinbox.core.model.Mailbox;
import com.elasticinbox.core.model.Message;
import com.elasticinbox.core.model.ReservedLabels;

public final class CassandraAccountDAO implements AccountDAO
{
	private final static Keyspace keyspace = CassandraDAOFactory.getKeyspace();
	private final static StringSerializer strSe = StringSerializer.get();

	@SuppressWarnings("unused")
	private final static Logger logger = LoggerFactory
			.getLogger(CassandraAccountDAO.class);

	@Override
	public void add(final Mailbox mailbox) throws IOException, IllegalArgumentException
	{
		Map<String, Object> attributes = new HashMap<String, Object>();

		// TODO: make quota configurable on the mailbox level
		//attributes.put("quota:bytes", DEFAULT_QUOTA_BYTES);
		//attributes.put("quota:messages", DEFAULT_QUOTA_MESSAGES);
		//attributes.put("version", CURRENT_MAILBOX_VERSION);

		// Add predefined labels
		for (Label l : ReservedLabels.getAll()) {
			String labelKey = new StringBuilder(AccountPersistence.CN_LABEL_PREFIX).
					append(l.getLabelId()).toString();
			attributes.put(labelKey, l.getLabelName());
		}

		AccountPersistence.set(mailbox.getId(), attributes);
	}

	@Override
	public void delete(final Mailbox mailbox) throws IOException
	{
		// purge all previously deleted objects
		// TODO: we should not instantinate here
		MessageDAO messageDAO = new CassandraMessageDAO();
		messageDAO.purge(mailbox, new Date());

		// delete all objects from object store
		try {
			List<UUID> messageIds = null;
			UUID start = null;
	
			// loop until we delete all items
			do {
				// get all message ids
				messageIds = LabelIndexPersistence.get(mailbox.getId(),
						ReservedLabels.ALL_MAILS.getLabelId(), start,
						CassandraDAOFactory.MAX_COLUMNS_PER_REQUEST, true);
	
				// get all message headers
				Map<UUID, Message> messages = 
						MessagePersistence.fetch(mailbox.getId(), messageIds, false);
	
				// delete message sources from object store
				for(UUID messageId : messages.keySet()) {
					BlobProxy.delete(messages.get(messageId).getLocation());
				}
	
				// set start element for the next loop
				start = messageIds.isEmpty() ? null : messageIds.get(messageIds.size()-1);
			}
			while (messageIds.size() >= CassandraDAOFactory.MAX_COLUMNS_PER_REQUEST);
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
