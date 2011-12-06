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

import com.elasticinbox.common.utils.Assert;

/**
 * Generate unique Blob name based on the mailbox name and message UUID.
 * 
 * @author Rustam Aliyev
 */
public final class UuidBlobNamingPolicy extends AbstractBlobNamingPolicy
{
	@Override
	public String getBlobName(BlobNameBuilder builder)
	{
		Assert.notNull(builder.messageId, "message id cannot be null");
		Assert.notNull(builder.mailbox, "mailbox cannot be null");

		return new StringBuilder(builder.mailbox.getId()).append(":").append(builder.messageId).toString();
	}
	
}
