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
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.utils.TimeUUIDUtils;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.mutation.Mutator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elasticinbox.config.Configurator;
import com.elasticinbox.core.IllegalLabelException;
import com.elasticinbox.core.MessageDAO;
import com.elasticinbox.core.OverQuotaException;
import com.elasticinbox.core.blob.BlobDataSource;
import com.elasticinbox.core.blob.naming.BlobNameBuilder;
import com.elasticinbox.core.blob.store.BlobStoreProxy;
import com.elasticinbox.core.cassandra.persistence.*;
import com.elasticinbox.core.cassandra.utils.BatchConstants;
import com.elasticinbox.core.cassandra.utils.ThrottlingMutator;
import com.elasticinbox.core.model.LabelCounters;
import com.elasticinbox.core.model.Labels;
import com.elasticinbox.core.model.Mailbox;
import com.elasticinbox.core.model.Marker;
import com.elasticinbox.core.model.Message;
import com.elasticinbox.core.model.ReservedLabels;
import com.google.common.collect.Iterables;

public final class CassandraMessageDAO extends AbstractMessageDAO implements MessageDAO
{
	private final Keyspace keyspace;
	private final static StringSerializer strSe = StringSerializer.get();

	private final static Logger logger = 
			LoggerFactory.getLogger(CassandraMessageDAO.class);
	
	public CassandraMessageDAO(Keyspace keyspace) {
		this.keyspace = keyspace;
	}

	@Override
	public Message getParsed(final Mailbox mailbox, final UUID messageId)
	{
		return MessagePersistence.fetch(mailbox.getId(), messageId, true);
	}

	@Override
	public BlobDataSource getRaw(final Mailbox mailbox, final UUID messageId)
			throws IOException
	{
		Message metadata = MessagePersistence.fetch(mailbox.getId(), messageId, false);
		return new BlobDataSource(metadata.getLocation());
	}

	@Override
	public Map<UUID, Message> getMessageIdsWithHeaders(final Mailbox mailbox,
			final int labelId, final UUID start, final int count, boolean reverse)
	{
		List<UUID> messageIds = 
				getMessageIds(mailbox, labelId, start, count, reverse);

		return MessagePersistence.fetch(mailbox.getId(), messageIds, false);
	}

	@Override
	public List<UUID> getMessageIds(final Mailbox mailbox, final int labelId,
			final UUID start, final int count, final boolean reverse)
	{
		return LabelIndexPersistence.get(mailbox.getId(), labelId, start, count, reverse);
	}

	@Override
	public void put(final Mailbox mailbox, UUID messageId, Message message, InputStream in)
			throws IOException, OverQuotaException
	{
		URI uri = null;
		logger.debug("Storing message: key={}", messageId.toString());

		// Check quota
		LabelCounters mailboxCounters = LabelCounterPersistence.get(
				mailbox.getId(), ReservedLabels.ALL_MAILS.getId());

		long requiredBytes = mailboxCounters.getTotalBytes() + message.getSize();
		long requiredCount = mailboxCounters.getTotalMessages() + 1;

		if ((requiredBytes > Configurator.getDefaultQuotaBytes()) ||
			(requiredCount > Configurator.getDefaultQuotaCount()))
		{
			logger.info("Mailbox is over quota: {} size={}/{}, count={}/{}",
					new Object[] { mailbox.getId(), requiredBytes,
							Configurator.getDefaultQuotaBytes(), requiredCount,
							Configurator.getDefaultQuotaCount() });

			throw new OverQuotaException("Mailbox is over quota");
		}

		// Order is important, add to label after message written

		// store blob
		if (in != null) {
			try {
				// get blob name
				String blobName = new BlobNameBuilder()
						.setMailbox(mailbox).setMessageId(messageId)
						.setMessageSize(message.getSize()).build();

				// store message in blobstore
				uri = BlobStoreProxy.write(blobName, in, message.getSize());
				
				// update location in metadata
				message.setLocation(uri);
			} catch (Exception e) {
				throw new IOException("Failed to store blob: ", e);
			} finally {
				if(in != null) 
					in.close();
			}
		}

		// automatically add "all" label to all new messages
		message.addLabel(ReservedLabels.ALL_MAILS.getId());

		try {
			// begin batch operation
			Mutator<String> m = createMutator(keyspace, strSe);

			// store metadata
			MessagePersistence.persistMessage(m, mailbox.getId(), messageId, message);
			// add indexes
			LabelIndexPersistence.add(m, mailbox.getId(), messageId, message.getLabels());
			// update counters
			LabelCounterPersistence.add(m, mailbox.getId(), message.getLabels(), message.getLabelCounters());

			// commit batch operation
			m.execute();
		} catch (Exception e) {
			logger.warn(
					"Unable to store metadata for message {}, deleting blob {}",
					messageId, uri);

			// rollback
			if (uri != null)
				BlobStoreProxy.delete(uri);

			throw new IOException("Unable to store message metadata: ", e);
		}
	}

	@Override
	public void addMarker(final Mailbox mailbox, final Set<Marker> markers,
			final List<UUID> messageIds)
	{
		if(markers.isEmpty() || messageIds.isEmpty())
			return;

		Labels labels = null;

		// get label stats, only required for SEEN marker change
		if (markers.contains(Marker.SEEN)) {
			MessageAggregator ma = new MessageAggregator(mailbox, messageIds);
			labels = ma.aggregateCountersByLabel();
		}

		// build list of attributes
		Set<String> attributes = new HashSet<String>(markers.size());
		for (Marker marker : markers) {
			String a = new StringBuilder(Marshaller.CN_MARKER_PREFIX)
					.append(marker.toInt()).toString();
			attributes.add(a);
		}

		// begin batch operation
		Mutator<String> m = createMutator(keyspace, strSe);

		// add markers to messages
		MessagePersistence.persistAttributes(m, mailbox.getId(), messageIds, attributes);

		// decrement new message counter for each of the labels
		if (markers.contains(Marker.SEEN)) {
			for (Integer labelId : labels.getIds()) {
				LabelCounters labelCounters = new LabelCounters();
				labelCounters.setUnreadMessages(labels.getLabelCounters(labelId).getUnreadMessages());
				LabelCounterPersistence.subtract(m, mailbox.getId(), labelId, labelCounters);
			}
		}

		// commit batch operation
		m.execute();
	}

	@Override
	public void removeMarker(final Mailbox mailbox, final Set<Marker> markers,
			final List<UUID> messageIds)
	{
		if(markers.isEmpty() || messageIds.isEmpty())
			return;

		Labels labels = null;

		// get label stats, only required for SEEN marker change
		if (markers.contains(Marker.SEEN)) {
			MessageAggregator ma = new MessageAggregator(mailbox, messageIds);
			labels = ma.aggregateCountersByLabel();
		}

		// build list of attributes
		Set<String> attributes = new HashSet<String>(markers.size());
		for (Marker marker : markers) {
			String a = new StringBuilder(Marshaller.CN_MARKER_PREFIX)
					.append(marker.toInt()).toString();
			attributes.add(a);
		}

		// begin batch operation
		Mutator<String> m = createMutator(keyspace, strSe);

		// remove markers from messages
		MessagePersistence.deleteAttributes(m, mailbox.getId(), messageIds, attributes);

		// increment new message counter for each of the labels
		if (markers.contains(Marker.SEEN)) {
			for (Integer labelId : labels.getIds()) {
				LabelCounters labelCounters = new LabelCounters();

				// only seen messages will be marked as new, so we count only seen 
				Long seenMessages = 
						labels.getLabelCounters(labelId).getTotalMessages()
						- labels.getLabelCounters(labelId).getUnreadMessages();
				labelCounters.setUnreadMessages(seenMessages);

				LabelCounterPersistence.add(m, mailbox.getId(), labelId, labelCounters);
			}
		}

		// commit batch operation
		m.execute();
	}

	@Override
	public void addLabel(final Mailbox mailbox, final Set<Integer> labelIds,
			final List<UUID> messageIds)
	{
		if(labelIds.isEmpty() || messageIds.isEmpty())
			return;

		// build list of attributes
		Set<String> attributes = new HashSet<String>(labelIds.size());
		for (Integer labelId : labelIds) {
			attributes.add(Marshaller.CN_LABEL_PREFIX + labelId);
		}

		// begin batch operation
		Mutator<String> m = new ThrottlingMutator<String>(keyspace, strSe,
				BatchConstants.BATCH_WRITES, BatchConstants.BATCH_WRITE_INTERVAL);

		// get message stats for counters
		MessageAggregator ma = new MessageAggregator(mailbox, messageIds);
		LabelCounters labelCounters = ma.aggregateCounters();

		// add labels to messages
		MessagePersistence.persistAttributes(m, mailbox.getId(), messageIds, attributes);

		// add messages to label index
		LabelIndexPersistence.add(m, mailbox.getId(), messageIds, labelIds);

		// increment label counters
		LabelCounterPersistence.add(m, mailbox.getId(), labelIds, labelCounters);

		// commit batch operation
		m.execute();
	}

	@Override
	public void removeLabel(final Mailbox mailbox, final Set<Integer> labelIds,
			final List<UUID> messageIds)
	{
		if(labelIds.isEmpty() || messageIds.isEmpty())
			return;

		// label "all" cannot be removed from message
		if (labelIds.contains(ReservedLabels.ALL_MAILS.getId()))
			throw new IllegalLabelException("This label cannot be removed");

		// build list of attributes
		Set<String> attributes = new HashSet<String>(labelIds.size());
		for (Integer labelId : labelIds) {
			attributes.add(Marshaller.CN_LABEL_PREFIX + labelId);
		}

		// begin batch operation
		Mutator<String> m = new ThrottlingMutator<String>(keyspace, strSe,
				BatchConstants.BATCH_WRITES, BatchConstants.BATCH_WRITE_INTERVAL);

		// get message stats for counters, negative
		MessageAggregator ma = new MessageAggregator(mailbox, messageIds);
		LabelCounters labelCounters = ma.aggregateCounters();

		// remove labels from messages
		MessagePersistence.deleteAttributes(m, mailbox.getId(), messageIds, attributes);

		// remove messages from label index
		LabelIndexPersistence.remove(m, mailbox.getId(), messageIds, labelIds);

		// decrement label counters (add negative value)
		LabelCounterPersistence.subtract(m, mailbox.getId(), labelIds, labelCounters);

		// commit batch operation
		m.execute();
	}

	@Override
	public void delete(final Mailbox mailbox, final List<UUID> messageIds)
	{
		// get label stats
		MessageAggregator ma = new MessageAggregator(mailbox, messageIds);
		Labels labels = ma.aggregateCountersByLabel();

		// validate message ids
		List<UUID> validMessageIds = new ArrayList<UUID>(ma.getValidMessageIds());
		List<UUID> invalidMessageIds = new ArrayList<UUID>(ma.getInvalidMessageIds());
		
		// begin batch operation
		Mutator<String> m = new ThrottlingMutator<String>(keyspace, strSe,
				BatchConstants.BATCH_WRITES, BatchConstants.BATCH_WRITE_INTERVAL);

		// add only valid messages to purge index
		PurgeIndexPersistence.add(m, mailbox.getId(), validMessageIds);

		// remove valid message ids from label indexes, including "all"
		LabelIndexPersistence.remove(m, mailbox.getId(), validMessageIds, labels.getIds());

		// decrement label counters (add negative value)
		for (Integer labelId : labels.getIds()) {
			LabelCounterPersistence.subtract(m, mailbox.getId(), labelId, labels.getLabelCounters(labelId));
		}

		// remove invalid message ids from all known labels
		Map<Integer, String> allLabels = AccountPersistence.getLabels(mailbox.getId());
		LabelIndexPersistence.remove(m, mailbox.getId(), invalidMessageIds, allLabels.keySet());

		// commit batch operation
		m.execute();
	}

	@Override
	public void purge(final Mailbox mailbox, final Date age)
	{
		Map<UUID, UUID> purgeIndex = null;

		logger.debug("Purging all messages older than {} for {}", age.toString(), mailbox);

		// initiate throttling mutator 
		Mutator<String> m = new ThrottlingMutator<String>(keyspace, strSe,
				BatchConstants.BATCH_WRITES, BatchConstants.BATCH_WRITE_INTERVAL);

		// loop until we process all purged items
		do {
			// get message IDs of messages to purge
			purgeIndex = PurgeIndexPersistence.get(mailbox.getId(), age, BatchConstants.BATCH_READS);

			// get metadata/blob location
			Map<UUID, Message> messages = 
					MessagePersistence.fetch(mailbox.getId(), purgeIndex.values(), false);

			// delete message sources from object store
			for(UUID messageId : messages.keySet()) {
				BlobStoreProxy.delete(messages.get(messageId).getLocation());
			}

			// purge expired (older than age) messages
			MessagePersistence.deleteMessage(m, mailbox.getId(), purgeIndex.values());

			// remove from purge index
			PurgeIndexPersistence.remove(m, mailbox.getId(), purgeIndex.keySet());
		}
		while (purgeIndex.size() >= BatchConstants.BATCH_READS);

		// commit remaining items
		m.execute();
	}

	@Override
	public Labels calculateCounters(final Mailbox mailbox)
	{
		Labels labels = new Labels();
		Map<UUID, Message> messages;
		Map<UUID, UUID> purgeIndex;
		Set<UUID> deletedMessages = new HashSet<UUID>();

		logger.debug("Recalculating counters for {}", mailbox);

		// get deleted message IDs from purge queue
		// deleted messages should be excluded during calculation
		UUID start = TimeUUIDUtils.getUniqueTimeUUIDinMillis();
		do {
			purgeIndex = PurgeIndexPersistence.get(
					mailbox.getId(), start, BatchConstants.BATCH_READS);

			if (!purgeIndex.isEmpty()) {
				deletedMessages.addAll(purgeIndex.values());
				start = Iterables.getLast(purgeIndex.keySet());
			}
		}
		while (purgeIndex.size() >= BatchConstants.BATCH_READS);

		// reset start, read messages and calculate label counters
		start = TimeUUIDUtils.getUniqueTimeUUIDinMillis();
		do {
			messages = MessagePersistence.getRange(
					mailbox.getId(), start, BatchConstants.BATCH_READS);

			for (UUID messageId : messages.keySet())
			{
				// skip messages from purge queue
				if (deletedMessages.contains(messageId)) continue;

				Message message = messages.get(messageId);

				// add counters for each of the labels
				for (int labelId : message.getLabels()) {
					labels.incrementCounters(labelId, message.getLabelCounters());
				}
				
				start = messageId;
			}
		}
		while (messages.size() >= BatchConstants.BATCH_READS);

		return labels;
	}

	/**
	 * Aggregate messages to provide stats
	 */
	private class MessageAggregator
	{
		private final Map<UUID, Message> messages;
		private final HashSet<UUID> invalidMessageIds;

		public MessageAggregator(final Mailbox mailbox, final List<UUID> messageIds) {
			// get message headers
			messages = MessagePersistence.fetch(mailbox.getId(), messageIds, false);

			invalidMessageIds = new HashSet<UUID>(messageIds);
			invalidMessageIds.removeAll(this.getValidMessageIds());
		}

		/**
		 * Get aggregated {@link LabelCounter} stats for the list of messages
		 * 
		 * @param mailbox
		 * @param messageIds
		 * @return
		 */
		public LabelCounters aggregateCounters()
		{
			LabelCounters labelCounters = new LabelCounters();
	
			for(UUID messageId : messages.keySet()) {
				labelCounters.add(messages.get(messageId).getLabelCounters());
			}
	
			return labelCounters;
		}
	
		/**
		 * Get aggregated {@link LabelCounter} stats for each label in the list of
		 * messages. Results aggregated by label ID.
		 * 
		 * @param mailbox
		 * @param messageIds
		 * @return
		 */
		public Labels aggregateCountersByLabel()
		{
			Labels labels = new Labels();

			// get all labels of all messages, including label "all"
			for (UUID messageId : messages.keySet())
			{
				Set<Integer> messageLabels = messages.get(messageId).getLabels();
	
				for (int labelId : messageLabels) {
					if(!labels.containsId(labelId))
						labels.setCounters(labelId, new LabelCounters());
	
					labels.getLabelCounters(labelId).add(
							messages.get(messageId).getLabelCounters());
				}
			}
	
			return labels;
		}

		/**
		 * Returns message IDs which exist in message metadata.
		 * 
		 * In some cases, message can be deleted from metadata but not from
		 * index. Use this method to filter out such messages.
		 * 
		 * @return
		 */
		public Set<UUID> getValidMessageIds() {
			return messages.keySet();
		}

		/**
		 * Returns message IDs which do not exist in message metadata.
		 * 
		 * @return
		 */
		public Set<UUID> getInvalidMessageIds() {
			return invalidMessageIds;
		}
	}

}
