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

package com.elasticinbox.core;

import java.io.IOException;

import com.elasticinbox.core.model.Mailbox;

/**
 * Interface for Account operations
 * 
 * @author Rustam Aliyev
 */
public interface AccountDAO
{
	/**
	 * Add new account and initialize set of predefined labels. 
	 * 
	 * @param mailbox
	 * @throws IOException
	 * @throws IllegalArgumentException
	 */
	public void add(Mailbox mailbox) throws IOException, IllegalArgumentException;

	/**
	 * Delete account and all messages associated with it. Messages will be
	 * deleted immediately from blob store and metadata store. All other
	 * metadata information will also be cleared.
	 * 
	 * @param mailbox
	 * @throws IOException
	 */
	public void delete(Mailbox mailbox) throws IOException;
	
}
