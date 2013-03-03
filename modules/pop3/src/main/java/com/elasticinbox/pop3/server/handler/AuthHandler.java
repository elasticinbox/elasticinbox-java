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

package com.elasticinbox.pop3.server.handler;

import org.apache.james.protocols.pop3.POP3Session;
import org.apache.james.protocols.pop3.core.AbstractPassCmdHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elasticinbox.core.account.authenticator.AuthenticationException;
import com.elasticinbox.core.account.authenticator.AuthenticatorFactory;
import com.elasticinbox.core.account.authenticator.IAuthenticator;
import com.elasticinbox.core.account.validator.IValidator;
import com.elasticinbox.core.account.validator.ValidatorFactory;
import com.elasticinbox.core.account.validator.IValidator.AccountStatus;
import com.elasticinbox.core.model.Mailbox;

/**
 * POP3 Authentication Handler (AUTH)
 *
 * @author Rustam Aliyev
 */
public final class AuthHandler extends AbstractPassCmdHandler
{
	private static final Logger logger = 
			LoggerFactory.getLogger(AuthHandler.class);

	private MailboxHandlerFactory backend;

	private IValidator validator = ValidatorFactory.getValidator();
	private IAuthenticator authenticator = AuthenticatorFactory.getAuthenticator();

	public AuthHandler(MailboxHandlerFactory backend) {
		this.backend = backend;
	}

	@Override
	protected org.apache.james.protocols.pop3.mailbox.Mailbox auth(POP3Session session, String username, String password) throws Exception
	{
		Mailbox mailbox;

		logger.debug("POP3: Authenticating session {}, user {}, pass {}",
				new Object[] { session.getSessionID(), username, password });

		try {
			// authenticate mailbox, if failed return null
			AccountStatus status = validator.getAccountStatus(username);
			session.getLogger().debug("Validated account (" + username + ") status is " + status.toString());
	
			if (!status.equals(AccountStatus.ACTIVE)) {
				throw new AuthenticationException("User " + username + " does not exist or inactive");
			}
	
			// authenticate user with password
			mailbox = authenticator.authenticate(username, password);
	
			// return POP3 handler for mailbox
			return backend.getMailboxHander(mailbox);
		} catch (IllegalArgumentException iae) {
			logger.debug("POP3 Authentication failed. Invalid username [{}]: {}", username, iae.getMessage());
			return null;
		} catch (AuthenticationException ae) {
			logger.debug("POP3 Authentication failed. Invalid username [{}] or password [{}]", username, password);
			return null;
		}
	}
}