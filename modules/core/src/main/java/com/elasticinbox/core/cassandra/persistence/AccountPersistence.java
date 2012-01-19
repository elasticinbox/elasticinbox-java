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

import static me.prettyprint.hector.api.factory.HFactory.createColumn;
import static me.prettyprint.hector.api.factory.HFactory.createMutator;
import static me.prettyprint.hector.api.factory.HFactory.createSliceQuery;
import static com.elasticinbox.core.cassandra.CassandraDAOFactory.CF_ACCOUNTS;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.elasticinbox.core.cassandra.CassandraDAOFactory;

import me.prettyprint.cassandra.serializers.BytesArraySerializer;
import me.prettyprint.cassandra.serializers.SerializerTypeInferer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.ColumnSlice;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.SliceQuery;

public final class AccountPersistence
{
	public final static String CN_LABEL_PREFIX = "label:";

	private final static BytesArraySerializer byteSe = BytesArraySerializer.get();
	private final static StringSerializer strSe = StringSerializer.get();

	private final static Keyspace keyspace = CassandraDAOFactory.getKeyspace();

	/**
	 * Get all account attributes
	 * 
	 * @param mailbox
	 * @return
	 * @throws IOException 
	 */
	public static Map<String, Object> getAll(final String mailbox)
	{
		// Create a query
		SliceQuery<String, String, byte[]> q = 
				createSliceQuery(keyspace, strSe, strSe, byteSe);

		// set key, cf, range
		q.setColumnFamily(CF_ACCOUNTS).setKey(mailbox);
		q.setRange(null, null, false, 1000); //TODO: make sure we get all columns
		// execute
		QueryResult<ColumnSlice<String, byte[]>> r = q.execute();

		// read message ids from the result
		Map<String, Object> attributes = new HashMap<String, Object>();
		for (HColumn<String, byte[]> c : r.get().getColumns()) { 
			if( (c != null) && (c.getValue() != null)) {
				attributes.put(c.getName(), strSe.fromBytes(c.getValue()));
			}
		}

		return attributes;
	}

	/**
	 * Get all labels
	 * 
	 * @param mailbox
	 * @return
	 */
	public static Map<Integer, String> getLabels(final String mailbox)
	{
		Map<Integer, String> labels = new HashMap<Integer, String>();

		// get list of labels from cassandra
		Map<String, Object> attributes = getAll(mailbox);

		// build result
		for (Map.Entry<String, Object> a : attributes.entrySet()) {
			if (a.getKey().startsWith(CN_LABEL_PREFIX)) {
				Integer labelId = Integer.parseInt(a.getKey().split(":")[1]);
				labels.put(labelId, (String) a.getValue());
			}
		}
		
		return labels;
	}

	/**
	 * Add or update account attributes (columns)
	 *  
	 * @param account
	 * @param attributes
	 */
	public static <V> void set(final String mailbox, final Map<String, V> attributes)
	{
		Mutator<String> m = createMutator(keyspace, strSe);

		for (Map.Entry<String, V> a : attributes.entrySet()) {
			m.addInsertion(mailbox,	CF_ACCOUNTS,
					createColumn(a.getKey(), a.getValue(), strSe,
							SerializerTypeInferer.getSerializer(a.getValue())));
		}

		m.execute();
	}

	/**
	 * Delete account
	 * 
	 * @param mutator
	 * @param mailbox
	 */
	public static void delete(Mutator<String> mutator, final String mailbox)
	{
		mutator.addDeletion(mailbox, CF_ACCOUNTS, null, strSe);
	}

	/**
	 * Add label column.
	 * Inserts new or replaces name of the existing label.
	 * 
	 * @param mailbox
	 * @param labelId
	 * @param label
	 */
	public static void setLabel(final String mailbox, final int labelId,
			final String label)
	{
		String labelKey = new StringBuilder(CN_LABEL_PREFIX).append(labelId).toString();
		Map<String, String> attributes = new HashMap<String, String>(1);
		attributes.put(labelKey, label);
		AccountPersistence.set(mailbox, attributes);
	}

	/**
	 * Delete label from account
	 * 
	 * @param mutator
	 * @param mailbox
	 * @param labelId
	 */
	public static void deleteLabel(Mutator<String> mutator,
			final String mailbox, final Integer labelId)
	{
		String labelKey = new StringBuilder(CN_LABEL_PREFIX).append(labelId).toString();
		mutator.addDeletion(mailbox, CF_ACCOUNTS, labelKey, strSe);
	}

}
