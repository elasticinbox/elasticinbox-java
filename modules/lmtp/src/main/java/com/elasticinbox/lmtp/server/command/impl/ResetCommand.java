package com.elasticinbox.lmtp.server.command.impl;

import java.io.IOException;

import org.apache.mina.core.session.IoSession;

import com.elasticinbox.lmtp.server.api.LMTPReply;
import com.elasticinbox.lmtp.server.command.AbstractBaseCommand;
import com.elasticinbox.lmtp.server.core.mina.LMTPContext;

/**
 * The RSET command implementation.
 * 
 * @author De Oliveira Edouard &lt;doe_wanted@yahoo.fr&gt;
 * @author Ian McFarland &lt;ian@neo.com&gt;
 * @author Jon Stevens
 * @author Rustam Aliyev
 */
public class ResetCommand extends AbstractBaseCommand
{
	public ResetCommand() {
		super("RSET", "The RSET command resets the state of the mail session");
	}

	public void execute(String commandString, IoSession ioSession, LMTPContext ctx) 
		throws IOException
	{
		resetContext(ctx);
		sendResponse(ioSession, LMTPReply.OK);
	}
	
	public static void resetContext(LMTPContext ctx)
	{
		ctx.getSession().reset();
		ctx.getDeliveryHandler().resetMessageState();
	}
}
