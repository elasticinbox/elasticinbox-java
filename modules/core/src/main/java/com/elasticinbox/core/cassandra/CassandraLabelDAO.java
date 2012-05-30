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
import com.elasticinbox.core.cassandra.persistence.AccountPersistence;
import com.elasticinbox.core.cassandra.persistence.LabelCounterPersistence;
import com.elasticinbox.core.cassandra.persistence.LabelIndexPersistence;
import com.elasticinbox.core.cassandra.utils.BatchConstants;
import com.elasticinbox.core.model.LabelCounters;
import com.elasticinbox.core.model.Labels;
import com.elasticinbox.core.model.Mailbox;
import com.elasticinbox.core.model.ReservedLabels;
import com.elasticinbox.core.utils.LabelUtils;

import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.mutation.Mutator;

public final class CassandraLabelDAO implements LabelDAO
{
	private final static Keyspace keyspace = CassandraDAOFactory.getKeyspace();
	private final static StringSerializer strSe = StringSerializer.get();

	private final static int MAX_NEW_LABEL_ID_ATTEMPTS = 200;

	private final static Logger logger = 
			LoggerFactory.getLogger(CassandraLabelDAO.class);

	@Override
	public Labels getAllWithMetadata(final Mailbox mailbox)
			throws IOException
	{
		// get labels
		Labels labels = new Labels();
		labels.add(AccountPersistence.getLabels(mailbox.getId()));

		// get labels' counters
		labels.setCounters(LabelCounterPersistence.getAll(mailbox.getId()));
		
		return labels;
	}

	@Override
	public Map<Integer, String> getAll(final Mailbox mailbox) {
		return AccountPersistence.getLabels(mailbox.getId());
	}

	@Override
	public int add(final Mailbox mailbox, String label)
	{
		// get all existing labels
		Labels existingLabels = new Labels();
		existingLabels.add(AccountPersistence.getLabels(mailbox.getId()));

		LabelUtils.validateLabelName(label, existingLabels);

		// generate new unique label id
		int labelId = LabelUtils.getNewLabelId();
		int attempts = 1;
		while(existingLabels.containsId(labelId))
		{
			logger.debug("Generating new label ID");
	
			if (attempts > MAX_NEW_LABEL_ID_ATTEMPTS) {
				// too many attempts to get new random id! too many labels?
				logger.info("{} reached max random label id attempts with {} labels",
						mailbox, existingLabels.getIds().size());
				throw new IllegalLabelException("Too many labels");
			}

			labelId = LabelUtils.getNewLabelId();
			attempts++;
		}

		// add new label
		AccountPersistence.setLabel(mailbox.getId(), labelId, label);

		return labelId;
	}

	@Override
	public void rename(final Mailbox mailbox, final Integer labelId, String label)
			throws IOException
	{
		// get all existing labels
		Labels existingLabels = new Labels();
		existingLabels.add(AccountPersistence.getLabels(mailbox.getId()));

		// validate only if name is changed (skip for letter case changes) 
		if (!existingLabels.getName(labelId).equalsIgnoreCase(label)) {
			LabelUtils.validateLabelName(label, existingLabels);
		}

		// check if label id reserved
		if(ReservedLabels.contains(labelId)) {
			throw new ExistingLabelException("This is reserved label and can't be modified");
		}

		// check if label id exists
		if(!existingLabels.containsId(labelId)) {
			throw new IllegalLabelException("Label does not exist");
		}

		// set new name
		AccountPersistence.setLabel(mailbox.getId(), labelId, label);
	}

	@Override
	public void delete(final Mailbox mailbox, final Integer labelId)
	{
		// check if label reserved
		if(ReservedLabels.contains(labelId)) {
			throw new IllegalLabelException("This is reserved label and can't be modified");
		}

		// get message DAO object
		CassandraDAOFactory dao = new CassandraDAOFactory();
		MessageDAO messageDAO = dao.getMessageDAO();

		List<UUID> messageIds = null;
		Set<Integer> labelIds = new HashSet<Integer>(1);
		labelIds.add(labelId);

		// loop until we delete all items
		do {
			// get message ids of label
			messageIds = LabelIndexPersistence.get(mailbox.getId(), labelId,
					null, BatchConstants.BATCH_READS, false);

			// remove label from message metadata
			messageDAO.removeLabel(mailbox, labelIds, messageIds);
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
	public void setCounters(Mailbox mailbox, Labels calculatedCounters)
	{
		Map<Integer, LabelCounters> existingCounters = 
				LabelCounterPersistence.getAll(mailbox.getId());

		// begin batch operation
		Mutator<String> m = createMutator(keyspace, strSe);

		// update calculated counters
		for (int labelId : calculatedCounters.getIds())
		{
			LabelCounters diff = new LabelCounters(calculatedCounters.getLabelCounters(labelId));

			if (existingCounters.containsKey(labelId)) {
				diff.add(existingCounters.get(labelId).getInverse());
			}

			logger.debug(
					"Recalculated counters for label {}:\n\tCurrent: {}\n\tCalculated: {}\n\tDiff: {}",
					new Object[] { labelId, existingCounters.get(labelId),
							calculatedCounters.getLabelCounters(labelId), diff });

			LabelCounterPersistence.add(m, mailbox.getId(), labelId, diff);
		}
		
		// reset non-existing counters
		for (int labelId : existingCounters.keySet()) {
			if (!calculatedCounters.containsId(labelId)) {
				LabelCounterPersistence.subtract(
						m, mailbox.getId(), labelId, existingCounters.get(labelId));
			}
		}

		m.execute();
	}

}
