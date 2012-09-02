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
	public final static Label POP3		= new Label(10, "pop3");

	private final static ImmutableMap<Integer, Label> labels = 
			new ImmutableMap.Builder<Integer, Label>()
				.put(ALL_MAILS.getId(), ALL_MAILS)
				.put(INBOX.getId(), INBOX)
				.put(DRAFTS.getId(), DRAFTS)
				.put(SENT.getId(), SENT)
				.put(TRASH.getId(), TRASH)
				.put(SPAM.getId(), SPAM)
				.put(STARRED.getId(), STARRED)
				.put(IMPORTANT.getId(), IMPORTANT)
				.put(NOTIFICATIONS.getId(), NOTIFICATIONS)
				.put(ATTACHMENTS.getId(), ATTACHMENTS)
				.put(POP3.getId(), POP3)
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
