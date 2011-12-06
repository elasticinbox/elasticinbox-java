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

package com.elasticinbox.core.blob.naming;

import java.util.UUID;

import com.elasticinbox.core.model.Mailbox;

/**
 * This builder generates new Blob name based on the provided parameters and
 * specific {@link AbstractBlobNamingPolicy} implementation.
 * 
 * @author Rustam Aliyev
 */
public final class BlobNameBuilder
{
	protected Mailbox mailbox;
	protected UUID messageId;
	protected Long messageSize;

	private static AbstractBlobNamingPolicy uuidPolicy = new UuidBlobNamingPolicy();

	public BlobNameBuilder setMailbox(Mailbox mailbox) {
		this.mailbox = mailbox;
		return this;
	}

	public BlobNameBuilder setMessageId(UUID messageId) {
		this.messageId = messageId;
		return this;
	}

	public BlobNameBuilder setMessageSize(Long size) {
		this.messageSize = size;
		return this;
	}

	/**
	 * Generate new Blob name
	 * 
	 * @return
	 */
	public String build() {
		return uuidPolicy.getBlobName(this);
	}
}
