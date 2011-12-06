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

import java.util.Set;
import java.util.UUID;

import com.elasticinbox.core.MessageDAO;
import com.elasticinbox.core.model.Mailbox;
import com.elasticinbox.core.model.Marker;
import com.google.common.collect.ImmutableList;

/**
 * A partial implementation of the {@link MessageDAO} interface which translates
 * single message operations into multi-message.
 * 
 * @author Rustam Aliyev
 */
public abstract class AbstractMessageDAO implements MessageDAO
{
	@Override
	public void addMarker(final Mailbox mailbox, final Set<Marker> markers, final UUID messageId) {
		addMarker(mailbox, markers, ImmutableList.of(messageId));
	}

	@Override
	public void removeMarker(final Mailbox mailbox, final Set<Marker> markers, final UUID messageId) {
		removeMarker(mailbox, markers, ImmutableList.of(messageId));
	}

	@Override
	public void addLabel(final Mailbox mailbox, final Set<Integer> labelIds, final UUID messageId) {
		addLabel(mailbox, labelIds, ImmutableList.of(messageId));
	}

	@Override
	public void removeLabel(final Mailbox mailbox, final Set<Integer> labelIds, final UUID messageId) {
		removeLabel(mailbox, labelIds, ImmutableList.of(messageId));
	}

	@Override
	public void delete(final Mailbox mailbox, final UUID messageId) {
		delete(mailbox, ImmutableList.of(messageId));
	}
}
