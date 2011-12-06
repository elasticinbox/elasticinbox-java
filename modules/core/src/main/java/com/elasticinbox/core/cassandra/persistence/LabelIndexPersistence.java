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

package com.elasticinbox.core.cassandra.persistence;

import static com.elasticinbox.core.cassandra.CassandraDAOFactory.CF_LABEL_INDEX;
import static me.prettyprint.hector.api.factory.HFactory.createColumn;
import static me.prettyprint.hector.api.factory.HFactory.createSliceQuery;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elasticinbox.core.cassandra.CassandraDAOFactory;
import com.google.common.collect.ImmutableList;

import me.prettyprint.cassandra.serializers.BytesArraySerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.serializers.UUIDSerializer;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.ColumnSlice;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.exceptions.HectorException;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.SliceQuery;

public final class LabelIndexPersistence
{
	protected final static String COMPOSITE_KEY_DELIMITER = ":";
	private final static Keyspace keyspace = CassandraDAOFactory.getKeyspace();

	private final static StringSerializer strSe = StringSerializer.get();
	private final static UUIDSerializer uuidSe = UUIDSerializer.get();
	private final static BytesArraySerializer byteSe = BytesArraySerializer.get();

	private final static Logger logger = 
		LoggerFactory.getLogger(LabelIndexPersistence.class);

	/**
	 * Add message IDs to label indexes
	 * 
	 * @param mailbox
	 * @param messageIds
	 * @param labels
	 * @throws IOException 
	 */
	public static void add(Mutator<String> m, final String mailbox,
			final List<UUID> messageIds, final Set<Integer> labels)
			throws HectorException
	{
		// insert value
		for (Integer label : labels)
		{
			// unique label key as "john@example.com:1" 
			String indexKey = new StringBuilder(mailbox)
					.append(COMPOSITE_KEY_DELIMITER).append(label).toString();

			for (UUID messageId : messageIds) {
				logger.debug("Adding message {} to index {}", messageId, indexKey);

				m.addInsertion(indexKey, CF_LABEL_INDEX,
						createColumn(messageId, new byte[0], uuidSe, byteSe));
			}
		}
	}

	public static void add(Mutator<String> mutator, final String mailbox, final UUID messageId,
			final Set<Integer> labels) throws HectorException
	{
		final List<UUID> messageIds = new ArrayList<UUID>(1);
		messageIds.add(messageId);
		add(mutator, mailbox, messageIds, labels);
	}

	/**
	 * Remove message IDs from label indexes
	 * 
	 * @param mutator
	 * @param mailbox
	 * @param messageIds
	 * @param labels
	 */
	public static void remove(Mutator<String> mutator, final String mailbox,
			final List<UUID> messageIds, final Set<Integer> labels)
	{
		for (Integer label : labels) {

			String indexKey = new StringBuilder(mailbox)
					.append(COMPOSITE_KEY_DELIMITER).append(label).toString();

			for (UUID messageId : messageIds) {
				logger.debug("Removing message-id {} from index {}", messageId, indexKey);
				mutator.addDeletion(indexKey, CF_LABEL_INDEX, messageId, uuidSe);
			}
		}
	}

	/**
	 * Get slice of message IDs from label index
	 * 
	 * @param mailbox
	 * @param labelId
	 * @param start
	 * @param count
	 * @param reverse
	 * @return
	 * @throws HectorException
	 */
	public static List<UUID> get(final String mailbox,
			final int labelId, final UUID start, final int count,
			final boolean reverse) throws HectorException
	{
		List<UUID> messageIds = new ArrayList<UUID>(count);

		String key = new StringBuilder(mailbox).append(COMPOSITE_KEY_DELIMITER)
				.append(labelId).toString();

		// Create a query
		SliceQuery<String, UUID, byte[]> q = 
				createSliceQuery(keyspace, strSe, uuidSe, byteSe);

		// set key, cf, range
		q.setColumnFamily(CF_LABEL_INDEX);
		q.setKey(key);
		q.setRange(start, null, reverse, count);

		// execute
		QueryResult<ColumnSlice<UUID, byte[]>> r = q.execute();

		// read message ids from the result
		for (HColumn<UUID, byte[]> c : r.get().getColumns()) {
			if ((c != null) && (c.getValue() != null)) {
				messageIds.add(c.getName());
			}
		}

		return ImmutableList.copyOf(messageIds);
	}


	/**
	 * Delete complete label index
	 * 
	 * @param mutator
	 * @param mailbox
	 * @param labelId
	 */
	public static void deleteIndex(Mutator<String> mutator,
			final String mailbox, final Integer labelId)
	{
		String key = new StringBuilder(mailbox).append(COMPOSITE_KEY_DELIMITER)
				.append(labelId).toString();
		mutator.addDeletion(key, CF_LABEL_INDEX, null, strSe);
	}

	/**
	 * Delete all indexes
	 * 
	 * @param mutator
	 * @param mailbox
	 */
	public static void deleteIndexes(Mutator<String> mutator, final String mailbox)
	{
		// get all labels
		Map<Integer, String> labels = AccountPersistence.getLabels(mailbox);

		for (Integer labelId : labels.keySet()) {
			deleteIndex(mutator, mailbox, labelId);
		}

		// delete purge index
		String key = new StringBuilder(mailbox).append(COMPOSITE_KEY_DELIMITER)
				.append(PurgeIndexPersistence.PURGE_LABEL_ID).toString();
		mutator.addDeletion(key, CF_LABEL_INDEX, null, strSe);
	}
}
