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

package com.elasticinbox.lmtp.server.api;

import java.util.LinkedList;
import java.util.List;

/**
 * Email envelope containing information about LMTP delivery (e.g. sender,
 * recipients, etc.).
 * 
 * @author Rustam Aliyev
 */
public class LMTPEnvelope
{
	private List<LMTPAddress> mRecipients;
	private LMTPAddress sender;
    private LMTPBodyType mBodyType;

	public LMTPEnvelope() {
		mRecipients = new LinkedList<LMTPAddress>();
	}

	public boolean hasSender() {
		return sender != null;
	}

	public boolean hasRecipients() {
		return mRecipients.size() > 0;
	}

	public void setSender(LMTPAddress sender) {
		this.sender = sender;
	}

	public void addRecipient(LMTPAddress recipient) {
		mRecipients.add(recipient);
	}

	public List<LMTPAddress> getRecipients() {
		return mRecipients;
	}

	public LMTPAddress getSender() {
		return sender;
	}

    public LMTPBodyType getBodyType() {
		return mBodyType;
	}

    public void setBodyType(LMTPBodyType bodyType) {
		mBodyType = bodyType;
	}
}