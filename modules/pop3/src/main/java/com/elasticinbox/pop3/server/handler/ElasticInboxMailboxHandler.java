/**
 * Copyright (c) 2011-2013 Optimax Software Ltd.
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

package com.elasticinbox.pop3.server.handler;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.james.protocols.pop3.mailbox.MessageMetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elasticinbox.common.utils.CRLFInputStream;
import com.elasticinbox.core.MessageDAO;
import com.elasticinbox.core.MessageModification;
import com.elasticinbox.core.model.Mailbox;
import com.elasticinbox.core.model.Message;
import com.elasticinbox.core.model.ReservedLabels;
import com.elasticinbox.core.utils.Base64UUIDUtils;

/**
 * POP3 Mailbox implementation
 * 
 * @author Rustam Aliyev
 */
public class ElasticInboxMailboxHandler implements org.apache.james.protocols.pop3.mailbox.Mailbox 
{
	private static final Logger logger = 
			LoggerFactory.getLogger(ElasticInboxMailboxHandler.class);
	
	private final static int MAX_POP3_SESSION_MESSAGES = 300;

	private final Mailbox mailbox;
	private final MessageDAO dao;
	private List<MessageMetaData> messageList;

	public ElasticInboxMailboxHandler(final MessageDAO dao, final Mailbox mailbox)
	{
		// initialize DAO and Mailbox
		this.dao = dao;
		this.mailbox = mailbox;

		// initialize list of messages for the current session
		messageList = getPOP3MessageList();

		logger.debug("Initialized new POP3 session for {}", mailbox);
	}

	@Override
	public InputStream getMessage(String uid) throws IOException
	{
		UUID uuid = Base64UUIDUtils.decode(uid);
		logger.debug("POP3: Get message {}/{} [{}]", mailbox, uuid, uid);

		try {
			InputStream is = dao.getRaw(mailbox, uuid).getUncompressedInputStream();
			return new CRLFInputStream(is);
		} catch (Exception e) {
			logger.error("Error occured while retreiving POP3 message " + mailbox + "/" + uuid + " :", e);
			throw new IOException("Unable to read message");
		}
	}

	@Override
	public List<MessageMetaData> getMessages() throws IOException
	{
		logger.debug("POP3: List messages for {}", mailbox);

		return messageList;
	}

	@Override
	public void remove(String... uids) throws IOException
	{
		logger.debug("POP3: Removing messages {} from {}", uids, mailbox);

		Set<Integer> labels = new HashSet<Integer>(1);
		labels.add(ReservedLabels.POP3.getId());

		List<UUID> uuids = new ArrayList<UUID>(uids.length);

		for (String uid : uids) {
			uuids.add(Base64UUIDUtils.decode(uid));
		}

		dao.modify(mailbox, uuids, 
				new MessageModification.Builder().removeLabels(labels).build());
	}

	@Override
	public String getIdentifier() throws IOException
	{
		logger.debug("POP3: Get identifier");
		return mailbox.toString();
	}

	@Override
	public void close() throws IOException
	{
		logger.debug("POP3: Close");
	}

	@Override
	public InputStream getMessageBody(String uid) throws IOException
	{
		// should never be called
		throw new IOException("Not implemented");
	}

	@Override
	public InputStream getMessageHeaders(String uid) throws IOException
	{
		// should never be called
		throw new IOException("Not implemented");
	}

	/**
	 * Initialise a list of messages visible for the current session
	 * 
	 * @return List of messages
	 */
	private List<MessageMetaData> getPOP3MessageList()
	{
		// get list of messages
		Map<UUID, Message> messages = dao.getMessageIdsWithHeaders(
				mailbox, ReservedLabels.POP3.getId(), null, MAX_POP3_SESSION_MESSAGES, true);

		// convert to James Protocols list
		List<MessageMetaData> list = new ArrayList<MessageMetaData>(messages.size());

		for (Map.Entry<UUID, Message> entry : messages.entrySet())
		{
			MessageMetaData md = new MessageMetaData(
					Base64UUIDUtils.encode(entry.getKey()), entry.getValue().getSize());
			list.add(md);
		}
		
		return list;
	}

}