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

import com.elasticinbox.core.IllegalLabelException;
import com.elasticinbox.core.LabelDAO;
import com.elasticinbox.core.MessageDAO;
import com.elasticinbox.core.cassandra.persistence.AccountPersistence;
import com.elasticinbox.core.cassandra.persistence.LabelCounterPersistence;
import com.elasticinbox.core.cassandra.persistence.LabelIndexPersistence;
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
		labels.addCounters(LabelCounterPersistence.getAll(mailbox.getId()));
		
		return labels;
	}

	@Override
	public Map<Integer, String> getAll(final Mailbox mailbox) {
		return AccountPersistence.getLabels(mailbox.getId());
	}

	@Override
	public int add(final Mailbox mailbox, final String label)
	{
		// get labels
		Labels existingLabels = new Labels();
		existingLabels.add(AccountPersistence.getLabels(mailbox.getId()));

		// check if label already exists or reserved
		if(existingLabels.containsName(label))
			throw new IllegalLabelException("Label already exists");

		//TODO: add check for hierarchical lables starting with reserved labels
		//		e.g. disallow "inbox/folder"

		// generate new unique label id
		int labelId = LabelUtils.getNewLabelId();
		int maxAttempts = 1;
		while (true)
		{
			if (!existingLabels.containsId(labelId))
				break;
	
			if (maxAttempts > MAX_NEW_LABEL_ID_ATTEMPTS) {
				// too many attempts to get new random id! too many labels?
				logger.info("{} reached max random label id attempts with {} labels",
						mailbox, existingLabels.getIds().size());
				throw new IllegalLabelException("Too many labels");
			}

			labelId = LabelUtils.getNewLabelId();
			maxAttempts++;
		}

		// add new label
		AccountPersistence.setLabel(mailbox.getId(), labelId, label);

		return labelId;
	}

	@Override
	public void rename(final Mailbox mailbox, final Integer labelId,
			final String label) throws IOException
	{
		// check if label reserved
		if(ReservedLabels.contains(labelId)) {
			throw new IllegalLabelException("This is reserved label and can't be modified");
		}

		// get labels
		Labels existingLabels = new Labels();
		existingLabels.add(AccountPersistence.getLabels(mailbox.getId()));

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
					null, CassandraDAOFactory.MAX_COLUMNS_PER_REQUEST, false);

			// remove label from message metadata
			messageDAO.removeLabel(mailbox, labelIds, messageIds);
		}
		while (messageIds.size() >= CassandraDAOFactory.MAX_COLUMNS_PER_REQUEST);

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

}
