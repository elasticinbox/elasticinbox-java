package com.elasticinbox.lmtp.server.command.impl;

import java.io.IOException;

import org.apache.mina.core.session.IoSession;

import com.elasticinbox.lmtp.server.api.LMTPReply;
import com.elasticinbox.lmtp.server.command.AbstractBaseCommand;
import com.elasticinbox.lmtp.server.core.mina.LMTPContext;

/**
 * The NOOP command implementation.
 * 
 * @author De Oliveira Edouard &lt;doe_wanted@yahoo.fr&gt;
 * @author Rustam Aliyev
 */
public class NoopCommand extends AbstractBaseCommand
{
	public NoopCommand()
	{
		super("NOOP", "The NOOP command does nothing. It can be used to keep the\n"+
				"current session alive pinging it to prevent a timeout");
	}

	public void execute(String commandString, IoSession ioSession, LMTPContext ctx) 
		throws IOException
	{
		sendResponse(ioSession, LMTPReply.OK);
	}
}
