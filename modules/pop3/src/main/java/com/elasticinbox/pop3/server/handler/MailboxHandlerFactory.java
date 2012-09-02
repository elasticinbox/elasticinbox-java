package com.elasticinbox.pop3.server.handler;

import org.apache.james.protocols.pop3.mailbox.Mailbox;

import com.elasticinbox.core.DAOFactory;
import com.elasticinbox.core.MessageDAO;

public class MailboxHandlerFactory
{
	private final MessageDAO messageDAO;

	public MailboxHandlerFactory() {
		DAOFactory dao = DAOFactory.getDAOFactory();
		messageDAO = dao.getMessageDAO();
	}

	public Mailbox getMailboxHander(String username) {
		return new ElasticInboxMailboxHandler(messageDAO, username);
	}
}
