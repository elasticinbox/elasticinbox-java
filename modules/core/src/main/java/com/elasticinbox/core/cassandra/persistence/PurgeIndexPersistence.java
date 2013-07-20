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

package com.elasticinbox.core.cassandra.persistence;

import static com.elasticinbox.core.cassandra.CassandraDAOFactory.CF_LABEL_INDEX;
import static me.prettyprint.hector.api.factory.HFactory.createColumn;
import static me.prettyprint.hector.api.factory.HFactory.createSliceQuery;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elasticinbox.core.cassandra.CassandraDAOFactory;
import com.elasticinbox.core.cassandra.utils.BatchConstants;
import com.google.common.collect.Iterables;

import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.serializers.UUIDSerializer;
import me.prettyprint.cassandra.utils.TimeUUIDUtils;
import me.prettyprint.hector.api.beans.ColumnSlice;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.SliceQuery;

public final class PurgeIndexPersistence
{
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
		String indexKey = LabelIndexPersistence.getLabelKey(mailbox, PURGE_LABEL_ID);

		for (UUID messageId : messageIds)
		{
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
		UUID start = TimeUUIDUtils.getTimeUUID(age.getTime());
		return get(mailbox, start, count);
	}

	/**
	 * Get all message IDs deleted before given UUID
	 * 
	 * @param mailbox
	 * @param age
	 * @return
	 */
	public static Map<UUID, UUID> get(final String mailbox, final UUID start,
			final int count)
	{
		Map<UUID, UUID> messageIds = new LinkedHashMap<UUID, UUID>(count);
		String key = LabelIndexPersistence.getLabelKey(mailbox, PURGE_LABEL_ID);

		// Create a query
		SliceQuery<String, UUID, UUID> q = createSliceQuery(
				CassandraDAOFactory.getKeyspace(), strSe, uuidSe, uuidSe);

		// set key, cf, range
		q.setColumnFamily(CF_LABEL_INDEX);
		q.setKey(key);
		q.setRange(start, null, true, count);

		// execute
		QueryResult<ColumnSlice<UUID, UUID>> r = q.execute();

		// read message ids from the result
		for (HColumn<UUID, UUID> c : r.get().getColumns())
		{
			if ((c != null) && (c.getValue() != null)) {
				messageIds.put(c.getName(), c.getValue());
			}
		}

		return messageIds;
	}

	/**
	 * Get all message IDs pending purge
	 * 
	 * @param mailbox
	 * @return
	 */
	public static Set<UUID> getAll(final String mailbox)
	{
		Map<UUID, UUID> purgeIndex;
		Set<UUID> pendingMessages = new HashSet<UUID>();

		// get all message IDs from purge queue
		UUID start = TimeUUIDUtils.getUniqueTimeUUIDinMillis();
		do {
			purgeIndex = PurgeIndexPersistence.get(
					mailbox, start, BatchConstants.BATCH_READS);

			if (!purgeIndex.isEmpty()) {
				pendingMessages.addAll(purgeIndex.values());
				start = Iterables.getLast(purgeIndex.keySet());
			}
		}
		while (purgeIndex.size() >= BatchConstants.BATCH_READS);

		return pendingMessages;
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
		String indexKey = LabelIndexPersistence.getLabelKey(mailbox, PURGE_LABEL_ID);

		for (UUID purgeIndexId : purgeIndexIds)
		{
			logger.debug("Removing purge index ID {} from {}", purgeIndexId, indexKey);
			mutator.addDeletion(indexKey, CF_LABEL_INDEX, purgeIndexId, uuidSe);
		}
	}

}
