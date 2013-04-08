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

package com.elasticinbox.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.elasticinbox.core.blob.BlobDataSource;
import com.elasticinbox.core.model.Labels;
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
	public BlobDataSource getRaw(Mailbox mailbox, UUID messageId) throws IOException;

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
	 * @throws IOException 
	 */
	public void purge(Mailbox mailbox, Date age) throws IOException;

	/**
	 * Calculates counters for all labels bu scanning through all messages. Used
	 * for scrub operation.
	 * 
	 * @return
	 */
	public Labels calculateCounters(Mailbox mailbox);
}
