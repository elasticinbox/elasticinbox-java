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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elasticinbox.core.cassandra.CassandraDAOFactory;
import com.elasticinbox.core.model.LabelMap;
import com.google.common.collect.ImmutableList;

import me.prettyprint.cassandra.serializers.BytesArraySerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.serializers.UUIDSerializer;
import me.prettyprint.hector.api.beans.ColumnSlice;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.exceptions.HectorException;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.SliceQuery;

public final class LabelIndexPersistence
{
	private final static String COMPOSITE_KEY_DELIMITER = ":";

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
	public static void add(Mutator<String> mutator, final String mailbox,
			final List<UUID> messageIds, final Set<Integer> labels)
			throws HectorException
	{
		// insert value
		for (Integer label : labels)
		{
			String indexKey = getLabelKey(mailbox, label);

			for (UUID messageId : messageIds) {
				logger.debug("Adding message {} to index {}", messageId, indexKey);

				mutator.addInsertion(indexKey, CF_LABEL_INDEX,
						createColumn(messageId, new byte[0], uuidSe, byteSe));
			}
		}
	}

	/**
	 * Add message ID to label indexes
	 * 
	 * @param mailbox
	 * @param messageIds
	 * @param labels
	 * @throws IOException 
	 */
	public static void add(Mutator<String> mutator, final String mailbox, final UUID messageId,
			final Set<Integer> labels) throws HectorException
	{
		final List<UUID> messageIds = new ArrayList<UUID>(1);
		messageIds.add(messageId);
		add(mutator, mailbox, messageIds, labels);
	}

	/**
	 * Add message to label index
	 * 
	 * @param mutator
	 * @param mailbox
	 * @param messageId
	 * @param labelId
	 * @throws HectorException
	 */
	public static void add(Mutator<String> mutator, final String mailbox, final UUID messageId,
			final int labelId) throws HectorException
	{
		String indexKey = getLabelKey(mailbox, labelId);
		logger.debug("Adding message {} to index {}", messageId, indexKey);

		mutator.addInsertion(indexKey, CF_LABEL_INDEX,
				createColumn(messageId, new byte[0], uuidSe, byteSe));
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
		for (Integer label : labels)
		{
			String indexKey = getLabelKey(mailbox, label);

			for (UUID messageId : messageIds)
			{
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

		String key = getLabelKey(mailbox, labelId);

		// Create a query
		SliceQuery<String, UUID, byte[]> q = 
				createSliceQuery(CassandraDAOFactory.getKeyspace(), strSe, uuidSe, byteSe);

		// set key, cf, range
		q.setColumnFamily(CF_LABEL_INDEX);
		q.setKey(key);
		q.setRange(start, null, reverse, count);

		// execute
		QueryResult<ColumnSlice<UUID, byte[]>> r = q.execute();

		// read message ids from the result
		for (HColumn<UUID, byte[]> c : r.get().getColumns())
		{
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
	public static void deleteIndex(Mutator<String> mutator, final String mailbox, final Integer labelId)
	{
		String key = getLabelKey(mailbox, labelId);
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
		LabelMap labels = AccountPersistence.getLabels(mailbox);

		for (Integer labelId : labels.getIds()) {
			deleteIndex(mutator, mailbox, labelId);
		}

		// delete purge index
		String key = getLabelKey(mailbox, PurgeIndexPersistence.PURGE_LABEL_ID);
		mutator.addDeletion(key, CF_LABEL_INDEX, null, strSe);
	}

	/**
	 * Generates unique label key as "john@example.com:123".
	 * 
	 * @param mailbox
	 * @param label
	 * @return
	 */
	static String getLabelKey(final String mailbox, final int label)
	{
		return getLabelKey(mailbox, Integer.toString(label));
	}

	/**
	 * Generates unique label key as "john@example.com:label".
	 * 
	 * @param mailbox
	 * @param label
	 * @return
	 */
	static String getLabelKey(final String mailbox, final String label)
	{
		return mailbox + COMPOSITE_KEY_DELIMITER + label;
	}
}
