package com.elasticinbox.lmtp.server.command.impl;

import java.io.IOException;

import org.apache.mina.core.session.IoSession;

import com.elasticinbox.lmtp.server.api.LMTPReply;
import com.elasticinbox.lmtp.server.command.AbstractBaseCommand;
import com.elasticinbox.lmtp.server.core.mina.LMTPContext;

/**
 * The QUIT command implementation.
 * 
 * @author De Oliveira Edouard &lt;doe_wanted@yahoo.fr&gt;
 * @author Ian McFarland &lt;ian@neo.com&gt;
 * @author Jon Stevens
 * @author Rustam Aliyev
 */
public class QuitCommand extends AbstractBaseCommand
{
	public QuitCommand() {
		super("QUIT", "The QUIT command closes the SMTP session");
	}

	public void execute(String commandString, IoSession ioSession, LMTPContext ctx) 
		throws IOException
	{
		ctx.getSession().quit();
		sendResponse(ioSession, LMTPReply.BYE);		
	}
}
