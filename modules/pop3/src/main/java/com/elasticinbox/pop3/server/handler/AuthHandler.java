package com.elasticinbox.pop3.server.handler;

import org.apache.james.protocols.pop3.POP3Session;
import org.apache.james.protocols.pop3.core.AbstractPassCmdHandler;
import org.apache.james.protocols.pop3.mailbox.Mailbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthHandler extends AbstractPassCmdHandler
{
	private static final Logger logger = 
			LoggerFactory.getLogger(AuthHandler.class);

	private MailboxHandlerFactory backend;

	public AuthHandler(MailboxHandlerFactory backend) {
		this.backend = backend;
	}

	@Override
	protected Mailbox auth(POP3Session session, String username, String password) throws Exception
	{
		logger.debug("POP3: Authenticating session {}, user {}, pass {}",
				new Object[] { session.getSessionID(), username, password });

		// authenticate mailbox, if failed return null

		// get mailbox info
		Mailbox mailbox = backend.getMailboxHander(username);
		return mailbox;
	}
}