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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elasticinbox.core.ExistingLabelException;
import com.elasticinbox.core.IllegalLabelException;
import com.elasticinbox.core.LabelDAO;
import com.elasticinbox.core.MessageDAO;
import com.elasticinbox.core.MessageModification;
import com.elasticinbox.core.cassandra.persistence.AccountPersistence;
import com.elasticinbox.core.cassandra.persistence.LabelCounterPersistence;
import com.elasticinbox.core.cassandra.persistence.LabelIndexPersistence;
import com.elasticinbox.core.cassandra.utils.BatchConstants;
import com.elasticinbox.core.model.Label;
import com.elasticinbox.core.model.LabelCounters;
import com.elasticinbox.core.model.LabelMap;
import com.elasticinbox.core.model.Mailbox;
import com.elasticinbox.core.model.ReservedLabels;
import com.elasticinbox.core.utils.LabelUtils;

import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.mutation.Mutator;

public final class CassandraLabelDAO implements LabelDAO
{
	private final Keyspace keyspace;
	private final static StringSerializer strSe = StringSerializer.get();

	private final static Logger logger = 
			LoggerFactory.getLogger(CassandraLabelDAO.class);

	public CassandraLabelDAO(Keyspace keyspace) {
		this.keyspace = keyspace;
	}

	@Override
	public LabelMap getAllWithMetadata(final Mailbox mailbox)
			throws IOException
	{
		// get labels
		LabelMap labels = AccountPersistence.getLabels(mailbox.getId());

		// set labels' counters
		Map<Integer, LabelCounters> counters = LabelCounterPersistence.getAll(mailbox.getId());

		for (int labelId : counters.keySet())
		{
			if (labels.containsId(labelId) && counters.containsKey(labelId)) {
				labels.get(labelId).setCounters(counters.get(labelId));
			} else if (labels.containsId(labelId) && !counters.containsKey(labelId)) {
				// assume zeros for all counters if not yet initialised
				labels.get(labelId).setCounters(new LabelCounters());
			} else if (!labels.containsId(labelId) && counters.containsKey(labelId)) {
				logger.warn("Found counters for label {}, but label does not exist.", labelId);
			}
		}

		return labels;
	}

	@Override
	public Map<Integer, String> getAll(final Mailbox mailbox) {
		return AccountPersistence.getLabels(mailbox.getId()).getNameMap();
	}

	@Override
	public int add(Mailbox mailbox, Label label)
	{
		// get all existing labels
		LabelMap existingLabels = AccountPersistence.getLabels(mailbox.getId());

		LabelUtils.validateLabelName(label.getName(), existingLabels);

		try {
			// generate new label id
			int labelId = LabelUtils.getNewLabelId(existingLabels.getIds());
			label.setId(labelId);
		} catch (IllegalLabelException ile) {
			// log and rethrow
			logger.warn("{} reached max random label id attempts with {} labels",
						mailbox, existingLabels.size());
			throw ile;
		}

		// begin batch operation
		Mutator<String> mutator = createMutator(keyspace, strSe);

		// add new label
		AccountPersistence.putLabel(mutator, mailbox.getId(), label);

		// commit batch operation
		mutator.execute();

		return label.getId();
	}
	
	@Override
	public void update(Mailbox mailbox, Label label) throws IOException
	{
		// get all existing labels
		LabelMap existingLabels = AccountPersistence.getLabels(mailbox.getId());

		// validate only if name is changed (skips letter case changes)
		if (label.getName() != null && !existingLabels.containsName(label.getName())) {
			LabelUtils.validateLabelName(label.getName(), existingLabels);
		}

		// check if label id reserved
		if (ReservedLabels.contains(label.getId())) {
			throw new ExistingLabelException("This is reserved label and can't be modified");
		}

		// check if label id exists
		if (!existingLabels.containsId(label.getId())) {
			throw new IllegalLabelException("Label does not exist");
		}

		// begin batch operation
		Mutator<String> mutator = createMutator(keyspace, strSe);

		// set new name
		AccountPersistence.putLabel(mutator, mailbox.getId(), label);
		
		// commit batch operation
		mutator.execute();
	}

	@Override
	public void delete(final Mailbox mailbox, final Integer labelId)
	{
		// check if label reserved
		if(ReservedLabels.contains(labelId)) {
			throw new IllegalLabelException("This is reserved label and can't be modified");
		}

		// get message DAO object
		MessageDAO messageDAO = new CassandraMessageDAO(keyspace);

		List<UUID> messageIds = null;
		Set<Integer> labelIds = new HashSet<Integer>(1);
		labelIds.add(labelId);

		// loop until we delete all items
		do {
			// get message ids of label
			messageIds = LabelIndexPersistence.get(mailbox.getId(), labelId,
					null, BatchConstants.BATCH_READS, false);

			// remove label from message metadata
			messageDAO.modify(mailbox, messageIds, 
					new MessageModification.Builder().removeLabels(labelIds).build());
		}
		while (messageIds.size() >= BatchConstants.BATCH_READS);

		// begin batch operation
		Mutator<String> m = createMutator(keyspace, strSe);

		// delete label index
		LabelIndexPersistence.deleteIndex(m, mailbox.getId(), labelId);

		// delete label counters
		LabelCounterPersistence.delete(m, mailbox.getId(), labelId);
		
		// delete label info from account mailbox
		AccountPersistence.deleteLabel(m, mailbox.getId(), labelId);

		// commit batch operation
		m.execute();
	}

	@Override
	public void setCounters(Mailbox mailbox, LabelMap newCounters)
	{
		Map<Integer, LabelCounters> existingCounters = 
				LabelCounterPersistence.getAll(mailbox.getId());

		// begin batch operation
		Mutator<String> m = createMutator(keyspace, strSe);

		// update with the new counter values
		for (Label label : newCounters.values())
		{
			int labelId = label.getId();
			LabelCounters diff = new LabelCounters(label.getCounters());

			if (existingCounters.containsKey(labelId)) {
				diff.add(existingCounters.get(labelId).getInverse());
			}

			logger.debug(
					"Recalculated counters for label {}:\n\tCurrent: {}\n\tCalculated: {}\n\tDiff: {}",
					new Object[] { labelId, existingCounters.get(labelId), label.getCounters(), diff });

			LabelCounterPersistence.add(m, mailbox.getId(), labelId, diff);
		}

		// reset non-existing counters
		for (int labelId : existingCounters.keySet())
		{
			if (!newCounters.containsId(labelId)) {
				LabelCounterPersistence.subtract(
						m, mailbox.getId(), labelId, existingCounters.get(labelId));
			}
		}

		m.execute();
	}

}
