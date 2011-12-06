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
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.elasticinbox.core.model.Mailbox;
import com.elasticinbox.core.model.Marker;
import com.elasticinbox.core.model.Message;

/**
 * Interface for Message operations
 *
 * @author Rustam Aliyev
 */
public interface MessageDAO
{
	/**
	 * Get parsed message. This method only retrieves data available in the
	 * metadata store and return it as a {@link Message} object.
	 * 
	 * @param mailbox
	 * @param messageId
	 * @return
	 */
	public Message getParsed(Mailbox mailbox, UUID messageId);

	/**
	 * Get raw message. This method returns <code>InputStream</code> for
	 * the message source from the blob store.
	 * 
	 * @param mailbox
	 * @param messageId
	 * @return
	 * @throws IOException
	 */
	public InputStream getRaw(Mailbox mailbox, UUID messageId) throws IOException;

	/**
	 * Store message metadata and source.
	 * 
	 * <p>First, message source stored in the blob store. Then, blob URI together
	 * with other metadata is stored in metadata store and added to indexes.</p>
	 * 
	 * @param mailbox
	 * @param messageId
	 *            Unique message ID. If message with given ID already exists,
	 *            original message will be overwriten.
	 * @param message
	 *            Parsed message
	 * @param in
	 *            Message source
	 * @throws IOException
	 * @throws OverQuotaException
	 */
	public void put(Mailbox mailbox, UUID messageId, Message message, InputStream in)
			throws IOException, OverQuotaException;

	/**
	 * Get message IDs from the given label.
	 * 
	 * @param mailbox
	 * @param labelId
	 *            Label ID where to lookup messages.
	 * @param start
	 *            Starting message <code>UUID</code>. If set to
	 *            <code>null</code>, will start from the most recent message.
	 * @param count
	 *            Number of message IDs to retrieve.
	 * @param reverse
	 *            Defines order of the retrieval.
	 * @return
	 */
	public List<UUID> getMessageIds(Mailbox mailbox, int labelId, UUID start,
			int count, boolean reverse);

	/**
	 * Get message IDs and message headers from the given label. Only headers
	 * available in metadata store will be returned.
	 * 
	 * @param mailbox
	 * @param labelId
	 *            Label ID where to lookup messages.
	 * @param start
	 *            Starting message <code>UUID</code>. If set to
	 *            <code>null</code>, will start from the most recent message.
	 * @param count
	 *            Number of message IDs to retrieve.
	 * @param reverse
	 *            Defines order of the retrieval.
	 * @return
	 */
	public Map<UUID, Message> getMessageIdsWithHeaders(Mailbox mailbox, int labelId,
			UUID start, int count, boolean reverse);

	/**
	 * Add markers to multiple messages
	 * 
	 * @param mailbox
	 * @param markers
	 * @param messageIds
	 */
	public void addMarker(Mailbox mailbox, Set<Marker> markers, List<UUID> messageIds);

	/**
	 * Add markers to single message
	 * 
	 * @param mailbox
	 * @param markers
	 * @param messageId
	 */
	public void addMarker(Mailbox mailbox, Set<Marker> markers, UUID messageId);

	/**
	 * Remove markers from multiple messages
	 * 
	 * @param mailbox
	 * @param markers
	 * @param messageIds
	 */
	public void removeMarker(Mailbox mailbox, Set<Marker> markers, List<UUID> messageIds);

	/**
	 * Remove markers from single message
	 * 
	 * @param mailbox
	 * @param markers
	 * @param messageId
	 */
	public void removeMarker(Mailbox mailbox, Set<Marker> markers, UUID messageId);

	/**
	 * Add labels to multiple messages
	 * 
	 * @param mailbox
	 * @param labelIds
	 * @param messageIds
	 */
	public void addLabel(Mailbox mailbox, Set<Integer> labelIds, List<UUID> messageIds);

	/**
	 * Add labels to single message
	 * 
	 * @param mailbox
	 * @param labelIds
	 * @param messageId
	 */
	public void addLabel(Mailbox mailbox, Set<Integer> labelIds, UUID messageId);

	/**
	 * Remove labels from multiple messages
	 * 
	 * @param mailbox
	 * @param labelIds
	 * @param messageIds
	 * @throws IllegalLabelException
	 */
	public void removeLabel(Mailbox mailbox, Set<Integer> labelIds, List<UUID> messageIds)
			throws IllegalLabelException;

	/**
	 * Remove lables from single message
	 * 
	 * @param mailbox
	 * @param labelIds
	 * @param messageId
	 * @throws IllegalLabelException
	 */
	public void removeLabel(Mailbox mailbox, Set<Integer> labelIds, UUID messageId)
			throws IllegalLabelException;

	/**
	 * Delete multiple messages
	 * 
	 * @param mailbox
	 * @param messageIds
	 */
	public void delete(Mailbox mailbox, List<UUID> messageIds);

	/**
	 * Delete single message
	 * 
	 * @param mailbox
	 * @param messageId
	 */
	public void delete(Mailbox mailbox, UUID messageId);

	/**
	 * Purge deleted messages older than given age
	 * 
	 * @param mailbox
	 * @param age
	 */
	public void purge(Mailbox mailbox, Date age);

}
