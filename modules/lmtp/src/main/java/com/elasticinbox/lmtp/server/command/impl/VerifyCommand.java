package com.elasticinbox.lmtp.server.command.impl;

import java.io.IOException;

import org.apache.mina.core.session.IoSession;

import com.elasticinbox.lmtp.server.api.LMTPReply;
import com.elasticinbox.lmtp.server.command.AbstractBaseCommand;
import com.elasticinbox.lmtp.server.core.mina.LMTPContext;

/**
 * The VRFY command implementation.
 * 
 * RFC 2821, Section 7.3: If a site disables these commands for security
 * reasons, the SMTP server MUST return a 252 response, rather than a code that
 * could be confused with successful or unsuccessful verification.
 * 
 * RFC 1892: X.3.3 System not capable of selected features
 * 
 * @author Rustam Aliyev
 */
public class VerifyCommand extends AbstractBaseCommand
{
	public VerifyCommand()
	{
		super("VRFY", "The VRFY command verifies the recipient\n"+
				"currently not supported");
	}

	public void execute(String commandString, IoSession ioSession, LMTPContext ctx) 
		throws IOException
	{
		sendResponse(ioSession, LMTPReply.USE_RCPT_INSTEAD);
	}
}
