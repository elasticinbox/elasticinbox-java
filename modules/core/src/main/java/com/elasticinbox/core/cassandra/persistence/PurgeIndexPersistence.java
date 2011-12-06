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

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elasticinbox.core.cassandra.CassandraDAOFactory;

import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.serializers.UUIDSerializer;
import me.prettyprint.cassandra.utils.TimeUUIDUtils;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.ColumnSlice;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.SliceQuery;

public final class PurgeIndexPersistence
{
	private final static Keyspace keyspace = CassandraDAOFactory.getKeyspace();

	private final static StringSerializer strSe = StringSerializer.get();
	private final static UUIDSerializer uuidSe = UUIDSerializer.get();

	protected final static String PURGE_LABEL_ID = "purge"; // specific label id for purge index 

	private final static Logger logger = 
		LoggerFactory.getLogger(PurgeIndexPersistence.class);

	/**
	 * Add message to purge index
	 * 
	 * @param mutator
	 * @param mailbox
	 * @param messageIds
	 */
	public static void add(Mutator<String> mutator, final String mailbox,
			final List<UUID> messageIds)
	{
		UUID timeuuid;

		// unique purge label key as "john@example.com:purge" 
		String indexKey = new StringBuilder(mailbox.toLowerCase())
				.append(LabelIndexPersistence.COMPOSITE_KEY_DELIMITER)
				.append(PURGE_LABEL_ID).toString();

		for (UUID messageId : messageIds) {
			timeuuid = TimeUUIDUtils.getUniqueTimeUUIDinMillis();

			mutator.addInsertion(indexKey, CF_LABEL_INDEX,
					createColumn(timeuuid, messageId, uuidSe, uuidSe));
		}
	}

	/**
	 * Get all message IDs deleted before given date
	 * 
	 * @param mailbox
	 * @param age
	 * @return
	 */
	public static Map<UUID, UUID> get(final String mailbox, final Date age,
			final int count)
	{
		Map<UUID, UUID> messageIds = new HashMap<UUID, UUID>(count);

		UUID start = TimeUUIDUtils.getTimeUUID(age.getTime());
		
		String key = new StringBuilder(mailbox)
				.append(LabelIndexPersistence.COMPOSITE_KEY_DELIMITER)
				.append(PURGE_LABEL_ID).toString();

		// Create a query
		SliceQuery<String, UUID, UUID> q = createSliceQuery(keyspace, strSe,
				uuidSe, uuidSe);

		// set key, cf, range
		q.setColumnFamily(CF_LABEL_INDEX);
		q.setKey(key);
		q.setRange(start, null, true, count);

		// execute
		QueryResult<ColumnSlice<UUID, UUID>> r = q.execute();

		// read message ids from the result
		for (HColumn<UUID, UUID> c : r.get().getColumns()) {
			if ((c != null) && (c.getValue() != null)) {
				messageIds.put(c.getName(), c.getValue());
			}
		}

		return messageIds;
	}

	/**
	 * Remove all message IDs from purge index deleted before given date
	 * 
	 * @param mutator
	 * @param mailbox
	 * @param age
	 */
	public static void remove(Mutator<String> mutator, final String mailbox,
			final Date age, final int count)
	{
		// remove_slice not yet supported (CASSANDRA-494)
		// so for now just fetch messageIds and feed to delete query
		Map<UUID, UUID> result = get(mailbox, age, count);
		remove(mutator, mailbox, result.keySet());
	}

	/**
	 * Remove message IDs from purge index by ID
	 * 
	 * @param mutator
	 * @param mailbox
	 * @param purgeIndexIds
	 */
	public static void remove(Mutator<String> mutator, final String mailbox,
			final Collection<UUID> purgeIndexIds)
	{
		String indexKey = new StringBuilder(mailbox)
				.append(LabelIndexPersistence.COMPOSITE_KEY_DELIMITER)
				.append(PURGE_LABEL_ID).toString();

		for (UUID purgeIndexId : purgeIndexIds) {
			logger.debug("Removing purge index ID {} from {}", purgeIndexId, indexKey);
			mutator.addDeletion(indexKey, CF_LABEL_INDEX, purgeIndexId, uuidSe);
		}
	}

}
