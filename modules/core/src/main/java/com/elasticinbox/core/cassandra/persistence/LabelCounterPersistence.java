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

import static me.prettyprint.hector.api.factory.HFactory.createMultigetSuperSliceCounterQuery;
import static me.prettyprint.hector.api.factory.HFactory.createCounterSuperColumn;
import static me.prettyprint.hector.api.factory.HFactory.createCounterColumn;
import static com.elasticinbox.core.cassandra.CassandraDAOFactory.CF_COUNTERS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.CounterSuperRows;
import me.prettyprint.hector.api.beans.CounterSuperSlice;
import me.prettyprint.hector.api.beans.HCounterColumn;
import me.prettyprint.hector.api.beans.HCounterSuperColumn;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.MultigetSuperSliceCounterQuery;
import me.prettyprint.hector.api.query.QueryResult;

import com.elasticinbox.core.cassandra.CassandraDAOFactory;
import com.elasticinbox.core.model.LabelCounters;
import com.elasticinbox.core.model.Labels;
import com.elasticinbox.core.model.ReservedLabels;

public final class LabelCounterPersistence
{
	public final static String CN_TOTAL_BYTES = "total_bytes";
	public final static String CN_TOTAL_MESSAGES = "total_msg";
	public final static String CN_NEW_MESSAGES = "new_msg";
	public final static String CN_LABEL_PREFIX = "l:";

	private final static Keyspace keyspace = CassandraDAOFactory.getKeyspace();
	private final static StringSerializer strSe = StringSerializer.get();

	private final static Logger logger = 
			LoggerFactory.getLogger(LabelCounterPersistence.class);

	public static Map<Integer, LabelCounters> getAll(final String mailbox)
	{
		final String startColumnName = 
				new StringBuilder(CN_LABEL_PREFIX).append(0).toString();

		Map<Integer, LabelCounters> result = 
				new HashMap<Integer, LabelCounters>(ReservedLabels.MAX_RESERVED_LABEL_ID);

		MultigetSuperSliceCounterQuery<String, String, String> q = 
				createMultigetSuperSliceCounterQuery(keyspace, strSe, strSe, strSe);

		q.setColumnFamily(CF_COUNTERS);
		q.setRange(startColumnName, null, false, Labels.MAX_LABEL_ID);
		q.setKeys(mailbox);

		QueryResult<CounterSuperRows<String, String, String>> r = q.execute();
		
		CounterSuperSlice<String, String> slice = r.get().getByKey(mailbox).getSuperSlice();

		for (HCounterSuperColumn<String, String> sc : slice.getSuperColumns())
		{
			if(sc.getName().startsWith(CN_LABEL_PREFIX)) {
				LabelCounters labelCounters = labelCountersToObject(sc.getColumns());
				Integer labelId = Integer.parseInt(sc.getName().split("\\:")[1]);

				logger.debug("Fetched counters for label {} with {}", labelId, labelCounters);
				result.put(labelId, labelCounters);
			}
		}

		return result;
	}

	public static LabelCounters get(final String mailbox, final Integer labelId)
	{
		// TODO: get exact super-column instead of all?
		Map<Integer, LabelCounters> allLabelCounters = getAll(mailbox);
		return (allLabelCounters.containsKey(labelId)) ? allLabelCounters.get(labelId) : new LabelCounters();
	}

	/**
	 * Increment or decrement of the label counters. Use negative values for
	 * decrement.
	 * 
	 * @param mutator
	 * @param mailbox
	 * @param labelIds
	 * @param labelCounters
	 */
	public static void add(Mutator<String> mutator, final String mailbox,
			final Set<Integer> labelIds, final LabelCounters labelCounters)
	{
		// prepare column addition (increments or decrements)
		List<HCounterColumn<String>> columns = new ArrayList<HCounterColumn<String>>(3);

		// update value only if not null
		if (labelCounters.getTotalBytes() != 0)
			columns.add(createCounterColumn(CN_TOTAL_BYTES, labelCounters.getTotalBytes()));

		if (labelCounters.getTotalMessages() != 0)
			columns.add(createCounterColumn(CN_TOTAL_MESSAGES, labelCounters.getTotalMessages()));

		if (labelCounters.getNewMessages() != 0)
			columns.add(createCounterColumn(CN_NEW_MESSAGES, labelCounters.getNewMessages()));

		// batch add of counters for each of the labels
		for (Integer labelId : labelIds)
		{
			logger.debug("Updating counters for label {} with {}", labelId, labelCounters);

			String columnName = new StringBuilder(CN_LABEL_PREFIX).append(labelId).toString();

			HCounterSuperColumn<String, String> sc = 
					createCounterSuperColumn(columnName, columns, strSe, strSe);

			mutator.addCounter(mailbox, CF_COUNTERS, sc);
		}
	}

	public static void subtract(Mutator<String> mutator, final String mailbox,
			final Set<Integer> labelIds, final LabelCounters labelCounters)
	{
		// inverse values
		LabelCounters negativeCounters = new LabelCounters();
		negativeCounters.setTotalBytes(-labelCounters.getTotalBytes());
		negativeCounters.setTotalMessages(-labelCounters.getTotalMessages());
		negativeCounters.setNewMessages(-labelCounters.getNewMessages());

		// perform add
		add(mutator, mailbox, labelIds, negativeCounters);
	}

	public static void add(Mutator<String> mutator, final String mailbox,
			final Integer labelId, final LabelCounters labelCounters)
	{
		Set<Integer> labelIds = new HashSet<Integer>(1);
		labelIds.add(labelId);
		add(mutator, mailbox, labelIds, labelCounters);
	}

	public static void subtract(Mutator<String> mutator, final String mailbox,
			final Integer labelId, final LabelCounters labelCounters)
	{
		Set<Integer> labelIds = new HashSet<Integer>(1);
		labelIds.add(labelId);
		subtract(mutator, mailbox, labelIds, labelCounters);
	}

	/**
	 * Delete label counters
	 * 
	 * @param mutator
	 * @param mailbox
	 * @param labelId
	 */
	public static void delete(Mutator<String> mutator, final String mailbox,
			final Integer labelId)
	{
		// reset all counters (since delete won't work in most cases)
		// see: http://cassandra-user-incubator-apache-org.3065146.n2.nabble.com/possible-coming-back-to-life-bug-with-counters-tp6464338p6475427.html
		LabelCounters labelCounters = get(mailbox, labelId);

		// if counter super-column for this label exists
		if(labelCounters != null) {
			subtract(mutator, mailbox, labelId, labelCounters);

			// delete counters
			String key = new StringBuilder(CN_LABEL_PREFIX).append(labelId).toString();
			mutator.addDeletion(mailbox, CF_COUNTERS, key, strSe);
		}
	}
	
	/**
	 * Delete all label counters
	 * 
	 * @param mailbox
	 */
	public static void deleteAll(Mutator<String> mutator, final String mailbox)
	{
		// reset all counters (since delete won't work in most cases)
		// see: http://cassandra-user-incubator-apache-org.3065146.n2.nabble.com/possible-coming-back-to-life-bug-with-counters-tp6464338p6475427.html
		Map<Integer, LabelCounters> counters = getAll(mailbox);
		for (Integer labelId : counters.keySet()) {
			LabelCounters labelCounters = counters.get(labelId);
			subtract(mutator, mailbox, labelId, labelCounters);
		}

		// delete all label counters
		mutator.delete(mailbox, CF_COUNTERS, null, strSe);
	}

	/**
	 * Map counters from Cassandra to {@link LabelCounters} object
	 * 
	 * @param counters
	 * @return
	 */
	private static LabelCounters labelCountersToObject(
			final List<HCounterColumn<String>> counters)
	{
		LabelCounters labelCounters = new LabelCounters();

		for (HCounterColumn<String> c : counters)
		{
			if ((c != null) && (c.getValue() != null)) {

				// map counters to LabelCounters object
				if (c.getName().equals(CN_TOTAL_BYTES)) {
					labelCounters.setTotalBytes(c.getValue());
				} else if (c.getName().equals(CN_TOTAL_MESSAGES)) {
					labelCounters.setTotalMessages(c.getValue());
				} else if (c.getName().equals(CN_NEW_MESSAGES)) {
					labelCounters.setNewMessages(c.getValue());
				}
			}
		}

		return labelCounters;
	}

}
