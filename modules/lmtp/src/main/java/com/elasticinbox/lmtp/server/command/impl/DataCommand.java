package com.elasticinbox.lmtp.server.command.impl;

import java.io.IOException;

import org.apache.mina.core.session.IoSession;

import com.elasticinbox.lmtp.server.api.LMTPReply;
import com.elasticinbox.lmtp.server.command.AbstractBaseCommand;
import com.elasticinbox.lmtp.server.core.Session;
import com.elasticinbox.lmtp.server.core.mina.LMTPContext;

/**
 * The DATA command implementation.
 * 
 * @author De Oliveira Edouard &lt;doe_wanted@yahoo.fr&gt;
 * @author Ian McFarland &lt;ian@neo.com&gt;
 * @author Jon Stevens
 * @author Jeff Schnitzer
 * @author Rustam Aliyev
 */
public class DataCommand extends AbstractBaseCommand
{
	public DataCommand()
	{
		super("DATA", "The DATA command initiates the message transmission.\n"
				+ "Message ends with <CR><LF>.<CR><LF>");
	}

	public void execute(String commandString, IoSession ioSession, LMTPContext ctx) 
		throws IOException
	{
		Session session = ctx.getSession();

		if (!session.getHasSender()) {
			sendResponse(ioSession, LMTPReply.MISSING_MAIL_TO);
			return;
		} else if (session.getRecipientCount() == 0) {
			sendResponse(ioSession, LMTPReply.NO_RECIPIENTS);
			return;
		}

		session.setDataMode(true);
		//LMTPDecoderContext decCtx = (LMTPDecoderContext) ioSession.getAttribute(LMTPCodecDecoder.CONTEXT);
		sendResponse(ioSession, LMTPReply.OK_TO_SEND_DATA);		
	}
}