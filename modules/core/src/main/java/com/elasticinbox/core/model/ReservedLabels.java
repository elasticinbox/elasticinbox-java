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

package com.elasticinbox.core.model;

import java.util.Collection;

import com.google.common.collect.ImmutableMap;

/**
 * This class contains a list of the labels which are reserved for an internal
 * use. Reserverd labels are created by default in each mailbox.
 * 
 * @author Rustam Aliyev
 */
public final class ReservedLabels
{
	public final static Label ALL_MAILS	= new Label(0, "all");
	public final static Label INBOX		= new Label(1, "inbox");
	public final static Label DRAFTS	= new Label(2, "drafts");
	public final static Label SENT		= new Label(3, "sent");
	public final static Label TRASH		= new Label(4, "trash");
	public final static Label SPAM		= new Label(5, "spam");
	public final static Label STARRED	= new Label(6, "starred");
	public final static Label IMPORTANT	= new Label(7, "important");
	public final static Label NOTIFICATIONS	= new Label(8, "notifications");
	public final static Label ATTACHMENTS	= new Label(9, "attachments");
	public final static int MAX_RESERVED_LABEL_ID = 20;

	private final static ImmutableMap<Integer, Label> labels = 
			new ImmutableMap.Builder<Integer, Label>()
				.put(ALL_MAILS.getLabelId(), ALL_MAILS)
				.put(INBOX.getLabelId(), INBOX)
				.put(DRAFTS.getLabelId(), DRAFTS)
				.put(SENT.getLabelId(), SENT)
				.put(TRASH.getLabelId(), TRASH)
				.put(SPAM.getLabelId(), SPAM)
				.put(STARRED.getLabelId(), STARRED)
				.put(IMPORTANT.getLabelId(), IMPORTANT)
				.put(NOTIFICATIONS.getLabelId(), NOTIFICATIONS)
				.put(ATTACHMENTS.getLabelId(), ATTACHMENTS)
				.build();

	public static Label get(final int labelId) {
		return labels.get(labelId);
	}

	public static Collection<Label> getAll() {
		return labels.values();
	}

	public static boolean contains(final int labelId) {
		return labels.containsKey(labelId);
	}
}
