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

import static com.elasticinbox.core.cassandra.CassandraDAOFactory.CF_METADATA;
import static me.prettyprint.hector.api.factory.HFactory.createColumn;
import static me.prettyprint.hector.api.factory.HFactory.createSuperColumn;
import static me.prettyprint.hector.api.factory.HFactory.createSuperSliceQuery;
import static me.prettyprint.hector.api.factory.HFactory.createMultigetSuperSliceQuery;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elasticinbox.common.utils.Assert;
import com.elasticinbox.core.cassandra.CassandraDAOFactory;
import com.elasticinbox.core.model.Message;

import me.prettyprint.cassandra.serializers.BytesArraySerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.serializers.UUIDSerializer;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.HSuperColumn;
import me.prettyprint.hector.api.beans.SuperRows;
import me.prettyprint.hector.api.beans.SuperSlice;
import me.prettyprint.hector.api.exceptions.HectorException;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.MultigetSuperSliceQuery;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.SuperSliceQuery;

public final class MessagePersistence
{
	private final static Keyspace keyspace = CassandraDAOFactory.getKeyspace();

	private final static UUIDSerializer uuidSe = UUIDSerializer.get();
	private final static StringSerializer strSe = StringSerializer.get();
	private final static BytesArraySerializer byteSe = BytesArraySerializer.get();

	private final static Logger logger = 
			LoggerFactory.getLogger(MessagePersistence.class);

	/**
	 * Fetch attributes of multiple messages
	 * 
	 * @param mailbox
	 * @param messageIds
	 * @param includeBody
	 * @return
	 */
	public static Map<UUID, Message> fetch(final String mailbox,
			final Collection<UUID> messageIds, final boolean includeBody)
	{
		// read message ids from the result
		Map<UUID, Message> result = 
				new LinkedHashMap<UUID, Message>(messageIds.size());

		// Create a query
		MultigetSuperSliceQuery<String, UUID, String, byte[]> q = 
				createMultigetSuperSliceQuery(keyspace, strSe, uuidSe, strSe, byteSe);

		// set keys, cf, range
		q.setColumnFamily(CF_METADATA);
		q.setKeys(mailbox);
		q.setColumnNames(messageIds);

		// execute
		QueryResult<SuperRows<String, UUID, String, byte[]>> r = q.execute();

		SuperSlice<UUID, String, byte[]> slice = 
				r.get().getByKey(mailbox).getSuperSlice();

		for (UUID messageId : messageIds)
		{
			if ((slice.getColumnByName(messageId) != null)
					&& !slice.getColumnByName(messageId).getColumns().isEmpty())
			{
				result.put(messageId, Marshaller.unmarshall(
						slice.getColumnByName(messageId).getColumns(), includeBody));
			} else {
				logger.debug(
						"message {} not found in supercolumn slice for {} mailbox",
						messageId, mailbox);
			}
		}

		return result;
	}

	/**
	 * Fetch messsage attributes
	 * 
	 * @param mailbox
	 * @param messageId
	 * @param includeBody
	 * @return
	 */
	public static Message fetch(final String mailbox, final UUID messageId,
			final boolean includeBody)
	{
		List<UUID> messageIds = new ArrayList<UUID>(1);
		messageIds.add(messageId);

		Map<UUID, Message> messages = fetch(mailbox, messageIds, includeBody); 
		Assert.notNull(messages.get(messageId), "Message not found");

		return messages.get(messageId);
	}

	/**
	 * Get messages within given range (excludes body)
	 * 
	 * @param mailbox
	 * @param start
	 * @param count
	 * @return
	 */
	public static Map<UUID, Message> getRange(final String mailbox,
			final UUID start, final int count)
	{
		// read message ids from the result
		Map<UUID, Message> result = new LinkedHashMap<UUID, Message>();

		// Create a query
		SuperSliceQuery<String, UUID, String, byte[]> q = 
				createSuperSliceQuery(keyspace, strSe, uuidSe, strSe, byteSe);

		// set keys, cf, range
		q.setColumnFamily(CF_METADATA);
		q.setKey(mailbox);
		q.setRange(start, null, false, count);

		// execute
		QueryResult<SuperSlice<UUID, String, byte[]>> r = q.execute();

		List<HSuperColumn<UUID, String, byte[]>> superColumns = r.get().getSuperColumns();

		for (HSuperColumn<UUID, String, byte[]> superColumn : superColumns)
		{
			result.put(superColumn.getName(), 
					Marshaller.unmarshall(superColumn.getColumns(), false));
		}

		return result;
	}

	/**
	 * Persist {@link Message} object with given ID
	 * 
	 * @param mailbox
	 * @param messageId
	 * @param message
	 * @throws IOException
	 */
	public static void persistMessage(Mutator<String> mutator, final String mailbox,
			final UUID messageId, final Message message) throws IOException
	{
		logger.debug("Persisting metadata for message {} in mailbox {}",
				messageId, mailbox);

		mutator.addInsertion(mailbox, CF_METADATA, createSuperColumn(
				messageId, Marshaller.marshall(message), uuidSe, strSe, byteSe));
	}

	/**
	 * Persist attributes for multiple messages
	 * 
	 * @param mailbox
	 * @param messageIds
	 * @param attributes
	 * @throws HectorException
	 */
	private static void persistAttributes(Mutator<String> mutator, final String mailbox,
			final List<UUID> messageIds, final Map<String, Object> attributes)
			throws HectorException
	{
		List<HColumn<String, byte[]>> columns = Marshaller.mapToHColumns(attributes);

		for (UUID messageId : messageIds) {
			logger.debug("Persisting metadata for message {} in mailbox {}",
					messageId.toString(), mailbox);

			mutator.addInsertion(mailbox, CF_METADATA, 
					createSuperColumn(messageId, columns, uuidSe, strSe, byteSe));
		}
	}

	/**
	 * Set flag attribute to multiple messages
	 * 
	 * @param mailbox
	 * @param messageIds
	 * @param name
	 */
	public static void persistAttributes(Mutator<String> mutator, final String mailbox,
			final List<UUID> messageIds, final Set<String> attributes)
	{
		Map<String, Object> attr = new HashMap<String, Object>(attributes.size());
		for (String attribute : attributes) {
			attr.put(attribute, new byte[0]);
		}
		persistAttributes(mutator, mailbox, messageIds, attr);
	}

	/**
	 * Delete attributes from multiple messages
	 * 
	 * @param mutator
	 * @param mailbox
	 * @param messageIds
	 * @param attributes
	 */
	public static void deleteAttributes(Mutator<String> mutator, final String mailbox,
			final List<UUID> messageIds, final Set<String> attributes)
	{
		List<HColumn<String, byte[]>> columns = 
			new ArrayList<HColumn<String, byte[]>>(attributes.size());

		for (String name : attributes) {
			// FIXME: value should be null not "". 
			// see https://github.com/rantav/hector/issues/#issue/145 
			columns.add(createColumn(name, "".getBytes(), strSe, byteSe));
		}

		for (UUID messageId : messageIds) {
			mutator.addSubDelete(mailbox, CF_METADATA, 
				createSuperColumn(messageId, columns, uuidSe, strSe, byteSe));
		}
	}

	/**
	 * Delete single attribute from multiple messages
	 * 
	 * @param mutator
	 * @param mailbox
	 * @param messageIds
	 * @param attribute
	 */
	public static void deleteAttribute(Mutator<String> mutator, final String mailbox,
			final List<UUID> messageIds, final String attribute)
	{
		Set<String> names = new HashSet<String>(1);
		names.add(attribute);
		deleteAttributes(mutator, mailbox, messageIds, names);
	}

	/**
	 * Delete message and all its attributes
	 * 
	 * @param mutator
	 * @param mailbox
	 * @param messageIds
	 */
	public static void deleteMessage(Mutator<String> mutator, final String mailbox,
			final Collection<UUID> messageIds)
	{
		for (UUID messageId : messageIds) {
			mutator.addDeletion(mailbox, CF_METADATA, messageId, uuidSe);
		}
	}
	
	/**
	 * Delete all message metadata for account
	 * 
	 * @param mutator
	 * @param mailbox
	 */
	public static void deleteAllMessages(Mutator<String> mutator, final String mailbox)
	{
		mutator.addDeletion(mailbox, CF_METADATA, null, strSe);
	}

}
