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

import static me.prettyprint.hector.api.factory.HFactory.createCounterColumn;
import static me.prettyprint.hector.api.factory.HFactory.createCounterSliceQuery;
import static com.elasticinbox.core.cassandra.CassandraDAOFactory.CF_COUNTERS;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.prettyprint.cassandra.serializers.CompositeSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.Composite;
import me.prettyprint.hector.api.beans.CounterSlice;
import me.prettyprint.hector.api.beans.HCounterColumn;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.SliceCounterQuery;

import com.elasticinbox.core.cassandra.CassandraDAOFactory;
import com.elasticinbox.core.model.LabelConstants;
import com.elasticinbox.core.model.LabelCounters;
import com.elasticinbox.core.model.ReservedLabels;

public final class LabelCounterPersistence
{
	/** Counter type for Label counters */
	public final static String CN_TYPE_LABEL = "l";

	/** Label counter subtype for total bytes */
	public final static String CN_SUBTYPE_BYTES = "b";
	/** Label counter subtype for total messages */
	public final static String CN_SUBTYPE_MESSAGES = "m";
	/** Label counter subtype for unread messages */
	public final static String CN_SUBTYPE_UNREAD = "u";

	private final static Keyspace keyspace = CassandraDAOFactory.getKeyspace();
	private final static StringSerializer strSe = StringSerializer.get();

	private final static Logger logger = 
			LoggerFactory.getLogger(LabelCounterPersistence.class);

	/**
	 * Get counters for all label in the given mailbox
	 * 
	 * @param mailbox
	 * @return
	 */
	public static Map<Integer, LabelCounters> getAll(final String mailbox)
	{
		Composite startRange = new Composite();
		startRange.addComponent(0, CN_TYPE_LABEL, Composite.ComponentEquality.EQUAL);

		Composite endRange = new Composite();
		endRange.addComponent(0, CN_TYPE_LABEL, Composite.ComponentEquality.GREATER_THAN_EQUAL);

		SliceCounterQuery<String, Composite> sliceQuery =
				createCounterSliceQuery(keyspace, strSe, new CompositeSerializer());
		sliceQuery.setColumnFamily(CF_COUNTERS);
		sliceQuery.setKey(mailbox);
		sliceQuery.setRange(startRange, endRange, false, LabelConstants.MAX_LABEL_ID);

		QueryResult<CounterSlice<Composite>> r = sliceQuery.execute();

		return compositeColumnsToCounters(r.get().getColumns());
	}

	/**
	 * Get counters for the specified label in the given mailbox
	 * 
	 * @param mailbox
	 * @param labelId
	 * @return
	 */
	public static LabelCounters get(final String mailbox, final Integer labelId)
	{
		Composite startRange = new Composite();
		startRange.addComponent(0, CN_TYPE_LABEL, Composite.ComponentEquality.EQUAL);
		startRange.addComponent(1, labelId.toString(), Composite.ComponentEquality.EQUAL);

		Composite endRange = new Composite();
		endRange.addComponent(0, CN_TYPE_LABEL, Composite.ComponentEquality.EQUAL);
		endRange.addComponent(1, labelId.toString(), Composite.ComponentEquality.GREATER_THAN_EQUAL);

		SliceCounterQuery<String, Composite> sliceQuery =
				createCounterSliceQuery(keyspace, strSe, new CompositeSerializer());
		sliceQuery.setColumnFamily(CF_COUNTERS);
		sliceQuery.setKey(mailbox);
		sliceQuery.setRange(startRange, endRange, false, 5);

		QueryResult<CounterSlice<Composite>> r = sliceQuery.execute();

		Map<Integer, LabelCounters> counters = compositeColumnsToCounters(r.get().getColumns());
		LabelCounters labelCounters = counters.containsKey(labelId) ? counters.get(labelId) : new LabelCounters();

		logger.debug("Fetched counters for single label {} with {}", labelId, labelCounters);

		return labelCounters;
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
		// batch add of counters for each of the labels
		for (Integer labelId : labelIds)
		{
			logger.debug("Updating counters for label {} with {}", labelId, labelCounters);

			// update total bytes only for ALL_MAILS label (i.e. total mailbox usage)
			if ((labelId == ReservedLabels.ALL_MAILS.getId()) && (labelCounters.getTotalBytes() != 0)) {
				HCounterColumn<Composite> col = countersToCompositeColumn(
						labelId, CN_SUBTYPE_BYTES, labelCounters.getTotalBytes()); 
				mutator.addCounter(mailbox, CF_COUNTERS, col);
			}

			if (labelCounters.getTotalMessages() != 0) {
				HCounterColumn<Composite> col = countersToCompositeColumn(
						labelId, CN_SUBTYPE_MESSAGES, labelCounters.getTotalMessages()); 
				mutator.addCounter(mailbox, CF_COUNTERS, col);
			}

			if (labelCounters.getNewMessages() != 0) {
				HCounterColumn<Composite> col = countersToCompositeColumn(
						labelId, CN_SUBTYPE_UNREAD, labelCounters.getNewMessages()); 
				mutator.addCounter(mailbox, CF_COUNTERS, col);
			}
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
		if (labelCounters != null) {
			subtract(mutator, mailbox, labelId, labelCounters);

			// delete counters
			HCounterColumn<Composite> c;
			
			c = countersToCompositeColumn(labelId, CN_SUBTYPE_MESSAGES, labelCounters.getTotalMessages()); 
			mutator.addDeletion(mailbox, CF_COUNTERS, c.getName(), new CompositeSerializer());
			
			c = countersToCompositeColumn(labelId, CN_SUBTYPE_UNREAD, labelCounters.getTotalMessages()); 
			mutator.addDeletion(mailbox, CF_COUNTERS, c.getName(), new CompositeSerializer());

			// delete bytes only if ALL_MAILS
			if (labelId == ReservedLabels.ALL_MAILS.getId()) {
				c = countersToCompositeColumn(labelId, CN_SUBTYPE_BYTES, labelCounters.getTotalMessages()); 
				mutator.addDeletion(mailbox, CF_COUNTERS, c.getName(), new CompositeSerializer());
			}
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
	 * Build composite counter column
	 * 
	 * @param labelId
	 * @param subtype
	 * @param count
	 * @return
	 */
	private static HCounterColumn<Composite> countersToCompositeColumn(
			final Integer labelId, final String subtype, final Long count)
	{
		Composite composite = new Composite();
		composite.addComponent(CN_TYPE_LABEL, strSe);
		composite.addComponent(labelId.toString(), strSe);
		composite.addComponent(subtype, strSe);
		return createCounterColumn(composite, count, new CompositeSerializer());
	}
	
	/**
	 * Convert Hector Composite Columns to {@link LabelCounters}
	 * 
	 * @param columnList
	 * @return
	 */
	private static Map<Integer, LabelCounters> compositeColumnsToCounters(
			List<HCounterColumn<Composite>> columnList)
	{
		Map<Integer, LabelCounters> result = 
				new HashMap<Integer, LabelCounters>(LabelConstants.MAX_RESERVED_LABEL_ID);

		LabelCounters labelCounters = new LabelCounters();
		Integer prevLabelId = 0; // remember previous labelid which is always start form 0

		for (HCounterColumn<Composite> c : columnList)
		{
			Integer labelId = Integer.parseInt(c.getName().get(1, strSe));

			// since columns are ordered by labels, we can
			// flush label counters to result map as we traverse
			if (prevLabelId != labelId) {
				logger.debug("Fetched counters for label {} with {}", labelId, labelCounters);
				result.put(prevLabelId, labelCounters);
				labelCounters = new LabelCounters();
				prevLabelId = labelId;
			}

			String subtype = c.getName().get(2, strSe);

			if (subtype.equals(CN_SUBTYPE_BYTES)) {
				labelCounters.setTotalBytes(c.getValue());
			} else if (subtype.equals(CN_SUBTYPE_MESSAGES)) {
				labelCounters.setTotalMessages(c.getValue());
			} else if (subtype.equals(CN_SUBTYPE_UNREAD)) {
				labelCounters.setNewMessages(c.getValue());
			}
		}

		// flush remaining counters for the last label
		result.put(prevLabelId, labelCounters);

		return result;
	}
}
