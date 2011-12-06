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

import java.util.UUID;

/**
 * Abstract policy class for generating message UUID.
 * 
 * <p>Specific {@link #getMessageId()} implementation should generate new
 * message UUID based on the provided parameters.</p>
 * 
 * <p>IMPORTANT: Make sure that your implementation produces <b>unique</b>
 * message ID. If uniqueness is not guaranteed, messages will be overwritten
 * and discrepancy between counters and messages will occur.</p> 
 * 
 * @author Rustam Aliyev
 */
public abstract class AbstractMessageIdPolicy
{

	protected AbstractMessageIdPolicy() {
	}

	/**
	 * Generate new Blob Name
	 * 
	 * @return
	 */
	protected abstract UUID getMessageId(MessageIdBuilder builder);
	
}
