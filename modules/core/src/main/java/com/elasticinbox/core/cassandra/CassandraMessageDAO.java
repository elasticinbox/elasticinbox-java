/**
 * Copyright (c) 2011-2013 Optimax Software Ltd.
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
import com.elasticinbox.core.MessageModification;
import com.elasticinbox.core.OverQuotaException;
import com.elasticinbox.core.blob.BlobDataSource;
import com.elasticinbox.core.blob.compression.CompressionHandler;
import com.elasticinbox.core.blob.compression.DeflateCompressionHandler;
import com.elasticinbox.core.blob.encryption.AESEncryptionHandler;
import com.elasticinbox.core.blob.encryption.EncryptionHandler;
import com.elasticinbox.core.blob.store.BlobStorage;
import com.elasticinbox.core.blob.store.BlobStorageMediator;
import com.elasticinbox.core.cassandra.persistence.*;
import com.elasticinbox.core.cassandra.utils.BatchConstants;
import com.elasticinbox.core.cassandra.utils.ThrottlingMutator;
import com.elasticinbox.core.model.Label;
import com.elasticinbox.core.model.LabelCounters;
import com.elasticinbox.core.model.LabelMap;
import com.elasticinbox.core.model.Mailbox;
import com.elasticinbox.core.model.Marker;
import com.elasticinbox.core.model.Message;
import com.elasticinbox.core.model.ReservedLabels;
import com.google.common.collect.Lists;

public final class CassandraMessageDAO extends AbstractMessageDAO implements MessageDAO
{
	private final Keyspace keyspace;
	private final static StringSerializer strSe = StringSerializer.get();
	
	private final BlobStorage blobStorage;

	private final static Logger logger = 
			LoggerFactory.getLogger(CassandraMessageDAO.class);
	
	public CassandraMessageDAO(Keyspace keyspace)
	{
		this.keyspace = keyspace;
		
		// Create BlobStorage instance with AES encryption and Deflate compression
		CompressionHandler compressionHandler = 
				Configurator.isBlobStoreCompressionEnabled() ? new DeflateCompressionHandler() : null;
		EncryptionHandler encryptionHandler = 
				Configurator.isBlobStoreEncryptionEnabled() ? new AESEncryptionHandler() : null;

		this.blobStorage = new BlobStorageMediator(compressionHandler, encryptionHandler);
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
		return blobStorage.read(metadata.getLocation());
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
		if (in != null)
		{
			try {
				uri = blobStorage.write(messageId, mailbox,
						Configurator.getBlobStoreWriteProfileName(), in, message.getSize())
						.buildURI();

				// update location in metadata
				message.setLocation(uri);
			} catch (Exception e) {
				throw new IOException("Failed to store blob: ", e);
			} finally {
				if (in != null) {
					in.close();
				}
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
			if (uri != null) {
				blobStorage.delete(uri);
			}

			throw new IOException("Unable to store message metadata: ", e);
		}
	}
	
	@Override
	public void modify(Mailbox mailbox, List<UUID> messageIds, MessageModification mod)
	{
		// label "all" cannot be removed from message
		if (mod.getLabelsToRemove().contains(ReservedLabels.ALL_MAILS.getId())) {
			throw new IllegalLabelException("This label cannot be removed");
		}

		// begin batch operation
		ThrottlingMutator<String> mutator = new ThrottlingMutator<String>(keyspace, strSe,
				BatchConstants.BATCH_WRITES, BatchConstants.BATCH_WRITE_INTERVAL);

		// prepare message attributes
		Set<String> labelsToAddAsAttributes = labelsToMessageAttibutes(mod.getLabelsToAdd());
		Set<String> labelsToRemoveAsAttributes = labelsToMessageAttibutes(mod.getLabelsToRemove());
		Set<String> markersToAddAsAttributes = markersToMessageAttibutes(mod.getMarkersToAdd());
		Set<String> markersToRemoveAsAttributes = markersToMessageAttibutes(mod.getMarkersToRemove());

		// get message stats for counters
		MessageAggregator ma = new MessageAggregator(mailbox, messageIds);

		for (UUID messageId : ma.getValidMessageIds())
		{
			Message message = ma.getMessage(messageId);

			// add labels
			if (!mod.getLabelsToAdd().isEmpty())
			{
				// add labels to messages
				MessagePersistence.persistAttributes(mutator, mailbox.getId(), messageId, labelsToAddAsAttributes);

				// add messages to label index
				LabelIndexPersistence.add(mutator, mailbox.getId(), messageId, mod.getLabelsToAdd());

				// increment label counters
				for (int labelId : mod.getLabelsToAdd())
				{
					// count only if message does not have label
					if (!message.getLabels().contains(labelId)) {
						LabelCounterPersistence.add(mutator, mailbox.getId(), labelId, message.getLabelCounters());
					}
				}
			}

			// remove labels
			if (!mod.getLabelsToRemove().isEmpty())
			{
				// remove labels from messages
				MessagePersistence.deleteAttributes(mutator, mailbox.getId(), messageId, labelsToRemoveAsAttributes);

				// remove messages from label index
				LabelIndexPersistence.remove(mutator, mailbox.getId(), messageId, mod.getLabelsToRemove());

				// decrement label counters
				for (int labelId : mod.getLabelsToRemove())
				{
					// count only if message had label
					if (message.getLabels().contains(labelId)) {
						LabelCounterPersistence.subtract(mutator, mailbox.getId(), labelId, message.getLabelCounters());
					}
				}
			}

			// add markers
			if (!mod.getMarkersToAdd().isEmpty())
			{
				// add markers to messages
				MessagePersistence.persistAttributes(mutator, mailbox.getId(), messageIds, markersToAddAsAttributes);

				// decrement unread message counter only if message does not have SEEN marker
				if (mod.getMarkersToAdd().contains(Marker.SEEN) && !message.getMarkers().contains(Marker.SEEN))
				{
					LabelCounters labelCounters = new LabelCounters();
					labelCounters.setUnreadMessages(1L);
					LabelCounterPersistence.subtract(mutator, mailbox.getId(), message.getLabels(), labelCounters);
				}
			}
			
			// remove markers
			if (!mod.getMarkersToRemove().isEmpty())
			{
				// remove markers from messages
				MessagePersistence.deleteAttributes(mutator, mailbox.getId(), messageIds, markersToRemoveAsAttributes);

				// increment unread message counter only if message has SEEN marker
				if (mod.getMarkersToRemove().contains(Marker.SEEN) && message.getMarkers().contains(Marker.SEEN))
				{
					LabelCounters labelCounters = new LabelCounters();
					labelCounters.setUnreadMessages(1L);
					LabelCounterPersistence.add(mutator, mailbox.getId(), message.getLabels(), labelCounters);
				}
			}

			mutator.executeIfFull();
		}

		mutator.execute();
	}

	@Override
	public void delete(final Mailbox mailbox, final List<UUID> messageIds)
	{
		// begin batch operation
		ThrottlingMutator<String> mutator = new ThrottlingMutator<String>(keyspace, strSe,
				BatchConstants.BATCH_WRITES, BatchConstants.BATCH_WRITE_INTERVAL);
		
		// READ:WRITE ration is 1:5
		final int readBatchSize = BatchConstants.BATCH_WRITES / 5;

		for (List<UUID> idSubList : Lists.partition(messageIds, readBatchSize))
		{
			// get label stats
			MessageAggregator ma = new MessageAggregator(mailbox, idSubList);
			LabelMap labels = ma.aggregateCountersByLabel();

			// validate message ids
			List<UUID> validMessageIds = new ArrayList<UUID>(ma.getValidMessageIds());
			List<UUID> invalidMessageIds = new ArrayList<UUID>(ma.getInvalidMessageIds());

			// add only valid messages to purge index
			PurgeIndexPersistence.add(mutator, mailbox.getId(), validMessageIds);

			// remove valid message ids from label indexes, including "all"
			LabelIndexPersistence.remove(mutator, mailbox.getId(), validMessageIds, labels.getIds());

			// decrement label counters (add negative value)
			for (Integer labelId : labels.getIds()) {
				LabelCounterPersistence.subtract(mutator, mailbox.getId(), labelId, labels.get(labelId).getCounters());
			}

			// remove invalid message ids from all known labels
			LabelMap allLabels = AccountPersistence.getLabels(mailbox.getId());
			LabelIndexPersistence.remove(mutator, mailbox.getId(), invalidMessageIds, allLabels.getIds());

			// signal end of batch
			mutator.executeIfFull();
		}

		// commit batch operation
		mutator.execute();
	}

	@Override
	public void purge(final Mailbox mailbox, final Date age) throws IOException
	{
		Map<UUID, UUID> purgeIndex = null;

		logger.debug("Purging all messages older than {} for {}", age.toString(), mailbox);

		// initiate throttling mutator 
		ThrottlingMutator<String> mutator = new ThrottlingMutator<String>(keyspace, strSe,
				BatchConstants.BATCH_WRITES, BatchConstants.BATCH_WRITE_INTERVAL);

		// READ:WRITE ratio is 1:2
		final int readBatchSize = BatchConstants.BATCH_WRITES / 2;

		// loop until we process all purged items
		do {
			// get message IDs of messages to purge
			purgeIndex = PurgeIndexPersistence.get(mailbox.getId(), age, readBatchSize);

			// get metadata/blob location
			Map<UUID, Message> messages = 
					MessagePersistence.fetch(mailbox.getId(), purgeIndex.values(), false);

			// delete message sources from object store
			for(UUID messageId : messages.keySet()) {
				blobStorage.delete(messages.get(messageId).getLocation());
			}

			// purge expired (older than age) messages
			MessagePersistence.deleteMessage(mutator, mailbox.getId(), purgeIndex.values());

			// remove from purge index
			PurgeIndexPersistence.remove(mutator, mailbox.getId(), purgeIndex.keySet());
			
			// signal end of batch
			mutator.executeIfFull();
		}
		while (purgeIndex.size() >= readBatchSize);

		// commit remaining items
		mutator.execute();
	}

	@Override
	public LabelMap scrub(final Mailbox mailbox, final boolean rebuildIndex)
	{
		LabelMap labels = new LabelMap();
		Map<UUID, Message> messages;
		Set<UUID> purgePendingMessages = new HashSet<UUID>();
		
		// initiate throttling mutator 
		ThrottlingMutator<String> mutator = new ThrottlingMutator<String>(keyspace, strSe,
				BatchConstants.BATCH_WRITES, BatchConstants.BATCH_WRITE_INTERVAL);
		
		logger.debug("Recalculating counters for {}", mailbox);

		// Get message IDs pending purge. Such messages should be excluded during calculation.
		purgePendingMessages = PurgeIndexPersistence.getAll(mailbox.getId());

		logger.debug("Found {} messages pending purge. Will exclude them from calculations.", purgePendingMessages.size());

		UUID start = TimeUUIDUtils.getUniqueTimeUUIDinMillis();
		do {
			// reset start, read messages and calculate label counters
			messages = MessagePersistence.getRange(
					mailbox.getId(), start, BatchConstants.BATCH_READS);

			for (UUID messageId : messages.keySet())
			{
				start = messageId; // shift next query start

				// skip messages from purge queue
				if (purgePendingMessages.contains(messageId)) continue;

				Message message = messages.get(messageId);

				// add counters for each of the labels
				for (int labelId : message.getLabels())
				{
					if (!labels.containsId(labelId)) {
						Label label = new Label(labelId).setCounters(message.getLabelCounters()); 
						labels.put(label);
					} else {
						labels.get(labelId).incrementCounters(message.getLabelCounters());
					}

					if (rebuildIndex)
					{
						// add message ID to the label index
						LabelIndexPersistence.add(mutator, mailbox.getId(), messageId, labelId);
						mutator.executeIfFull();
					}
				}

				logger.debug("Counters state after message {} is {}", messageId, labels.toString());
			}
			
		}
		while (messages.size() >= BatchConstants.BATCH_READS);

		// commit remaining items
		mutator.execute();

		return labels;
	}

	/**
	 * Convert label IDs to message attributes.
	 *  
	 * @param labelIds
	 * @return
	 */
	private static Set<String> labelsToMessageAttibutes(Set<Integer> labelIds)
	{
		Set<String> attributes = new HashSet<String>(labelIds.size());
		for (Integer labelId : labelIds) {
			attributes.add(Marshaller.CN_LABEL_PREFIX + labelId);
		}

		return attributes;
	}

	/**
	 * Convert markers to message attributes.
	 *  
	 * @param labelIds
	 * @return
	 */
	private static Set<String> markersToMessageAttibutes(Set<Marker> markers)
	{
		Set<String> attributes = new HashSet<String>(markers.size());
		for (Marker marker : markers)
		{
			String a = new StringBuilder(Marshaller.CN_MARKER_PREFIX)
					.append(marker.toInt()).toString();
			attributes.add(a);
		}

		return attributes;
	}

	/**
	 * Aggregate messages to provide stats
	 */
	private class MessageAggregator
	{
		private final Map<UUID, Message> messages;
		private final HashSet<UUID> invalidMessageIds;

		public MessageAggregator(final Mailbox mailbox, final List<UUID> messageIds)
		{
			// get message headers
			messages = MessagePersistence.fetch(mailbox.getId(), messageIds, false);

			invalidMessageIds = new HashSet<UUID>(messageIds);
			invalidMessageIds.removeAll(this.getValidMessageIds());
		}

		/**
		 * Get message
		 * 
		 * @param messageId
		 * @return
		 */
		public Message getMessage(UUID messageId)
		{
			return messages.get(messageId);
		}

		/**
		 * Get aggregated {@link LabelCounter} stats for each label in the list of
		 * messages. Results aggregated by label ID.
		 * 
		 * @return
		 */
		public LabelMap aggregateCountersByLabel()
		{
			LabelMap labels = new LabelMap();

			// get all labels of all messages, including label "all"
			for (UUID messageId : this.messages.keySet())
			{
				Set<Integer> messageLabels = this.messages.get(messageId).getLabels();
	
				for (int labelId : messageLabels)
				{
					if (!labels.containsId(labelId)) {
						Label label = new Label(labelId).
								setCounters(this.messages.get(messageId).getLabelCounters());
						labels.put(label);
					} else {
						labels.get(labelId).getCounters().add(
								this.messages.get(messageId).getLabelCounters());
					}
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
