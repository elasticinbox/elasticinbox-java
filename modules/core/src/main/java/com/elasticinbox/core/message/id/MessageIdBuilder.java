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

package com.elasticinbox.core.message.id;

import java.util.Date;
import java.util.UUID;

/**
 * This builder generates new message UUID based on the provided parameters and
 * specific {@link AbstractMessageIdPolicy} implementation.
 * 
 * @author Rustam Aliyev
 */
public final class MessageIdBuilder
{
	protected String messageIdHeader; // message-id header
	protected Date sentDate;
	
	private static AbstractMessageIdPolicy sentDatePolicy = new SentDateMessageIdPolicy();
	private static AbstractMessageIdPolicy currentTimePolicy = new CurrentTimeMessageIdPolicy();

	/**
	 * Set <code>date</code> header from email
	 * 
	 * @param messageId
	 * @return
	 */
	public MessageIdBuilder setSentDate(Date date) {
		this.sentDate = date;
		return this;
	}

	/**
	 * Set <code>message-id</code> header from email
	 * 
	 * @param messageId
	 * @return
	 */
	public MessageIdBuilder setMessageIdHeader(String messageId) {
		this.messageIdHeader = messageId;
		return this;
	}

	/**
	 * Build new message UUID
	 * 
	 * @return
	 */
	public UUID build()
	{
		UUID messageId = null;
		
		if (sentDate != null) {
			messageId = sentDatePolicy.getMessageId(this);
		} else {
			messageId = currentTimePolicy.getMessageId(this);
		}

		return messageId;
	}
}
