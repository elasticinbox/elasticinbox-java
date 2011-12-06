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
import java.util.Map;

import com.elasticinbox.core.model.Labels;
import com.elasticinbox.core.model.Mailbox;

/**
 * Interface for Label operations
 *
 * @author Rustam Aliyev
 */
public interface LabelDAO
{
	/**
	 * Get all labels in the mailbox.
	 * 
	 * @param mailbox
	 * @return Label IDs and names
	 * @throws IOException
	 */
	public Map<Integer, String> getAll(Mailbox mailbox) throws IOException;

	/**
	 * Get all labels in the mailbox with stats (total size, total messages, new
	 * messages).
	 * 
	 * @param mailbox
	 * @return
	 * @throws IOException
	 */
	public Labels getAllWithMetadata(Mailbox mailbox) throws IOException;

	/**
	 * Add new label to mailbox
	 * 
	 * @param mailbox
	 * @param label New label name
	 * @return New label ID
	 * @throws IOException
	 * @throws IllegalLabelException
	 */
	public int add(Mailbox mailbox, String label) throws IOException, IllegalLabelException;

	/**
	 * Rename existing label
	 * 
	 * @param mailbox
	 * @param labelId
	 *            Existing label ID
	 * @param label
	 *            New name for existing label
	 * @throws IOException
	 * @throws IllegalLabelException
	 */
	public void rename(Mailbox mailbox, Integer labelId, String label) throws IOException, IllegalLabelException;

	/**
	 * Delete existing label
	 * 
	 * @param mailbox
	 * @param labelId
	 *            Existing label ID
	 * @throws IOException
	 * @throws IllegalLabelException
	 */
	public void delete(Mailbox mailbox, Integer labelId) throws IOException, IllegalLabelException;

}
