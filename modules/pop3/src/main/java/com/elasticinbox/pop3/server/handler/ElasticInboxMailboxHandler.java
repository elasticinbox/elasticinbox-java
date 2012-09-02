package com.elasticinbox.pop3.server.handler;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.james.protocols.pop3.mailbox.MessageMetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elasticinbox.core.MessageDAO;
import com.elasticinbox.core.model.Mailbox;
import com.elasticinbox.core.model.Message;
import com.elasticinbox.core.model.ReservedLabels;

public class ElasticInboxMailboxHandler implements org.apache.james.protocols.pop3.mailbox.Mailbox 
{
	private static final Logger logger = 
			LoggerFactory.getLogger(ElasticInboxMailboxHandler.class);
	
	private final static int MAX_POP3_SESSION_MESSAGES = 500;

	private Map<UUID, Message> messages;

	public ElasticInboxMailboxHandler(final MessageDAO dao, final String username)
	{
		// initialize mailbox
		logger.debug("POP3: Initializing session for {}", username);
		
		// get list of messages
		Mailbox mailbox = new Mailbox(username);
		messages = dao.getMessageIdsWithHeaders(
				mailbox, ReservedLabels.POP3.getId(), null, MAX_POP3_SESSION_MESSAGES, true);
	}

	@Override
	public InputStream getMessageBody(long uid) throws IOException {
		return null;
	}

	@Override
	public InputStream getMessageHeaders(long uid) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InputStream getMessage(long uid) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<MessageMetaData> getMessages() throws IOException
	{
		logger.debug("POP3: List messages");
		
		List<MessageMetaData> list = new ArrayList<MessageMetaData>(messages.size());

		// convert to James Protocols list
		for (Map.Entry<UUID, Message> entry : messages.entrySet()) {
			MessageMetaData md = new MessageMetaData(
					entry.getKey().timestamp(), entry.getValue().getSize());
			list.add(md);
		}

		return list;
	}

	@Override
	public void remove(long... uids) throws IOException {
		logger.debug("POP3: Remove messages");
	}

	@Override
	public String getIdentifier() throws IOException {
		logger.debug("POP3: Get identifier");
		return "Caracas";
	}

	@Override
	public void close() throws IOException {
		logger.debug("POP3: Close");
	}

}