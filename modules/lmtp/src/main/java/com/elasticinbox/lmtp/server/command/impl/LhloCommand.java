package com.elasticinbox.lmtp.server.command.impl;

import java.io.IOException;

import org.apache.mina.core.session.IoSession;

import com.elasticinbox.lmtp.server.LMTPServerConfig;
import com.elasticinbox.lmtp.server.api.LMTPReply;
import com.elasticinbox.lmtp.server.command.AbstractBaseCommand;
import com.elasticinbox.lmtp.server.core.Session;
import com.elasticinbox.lmtp.server.core.mina.LMTPContext;

/**
 * The EHLO command implementation.
 * 
 * @author Ian McFarland &lt;ian@neo.com&gt;
 * @author Jon Stevens
 * @author De Oliveira Edouard &lt;doe_wanted@yahoo.fr&gt;
 * @author Rustam Aliyev
 */
public class LhloCommand extends AbstractBaseCommand
{
	public LhloCommand()
	{
		super("LHLO",
			"The LHLO command posts the client hostname info to the server.\n",
			"<hostname>\n hostname = your hostname");
	}

	public void execute(String commandString, IoSession ioSession, LMTPContext ctx) 
		throws IOException
	{
		String[] args = getArgs(commandString);
		if (args.length < 2)
		{
			sendResponse(ioSession, LMTPReply.INVALID_LHLO_PARAMETER);
			return;
		}

		//		Sample response:
		//		250-server.host.name
		//		250-PIPELINING
		//		250-SIZE 10240000
		//		250-ENHANCEDSTATUSCODES
		//		250 8BITMIME

		Session session = ctx.getSession();
		StringBuilder response = new StringBuilder();
		if (!session.getHasSeenHelo())
		{
			LMTPServerConfig cfg = ctx.getLMTPServerConfig();
			session.setHasSeenHelo(true);
			response.append("250-");
			response.append(cfg.getHostName());
			response.append("\r\n");
			response.append("250-8BITMIME\r\n");
			response.append("250-ENHANCEDSTATUSCODES\r\n");
			//response.append("250-SIZE\r\n");
			response.append("250 PIPELINING");
		}
		else
		{
			String remoteHost = args[1];
			response.append("503 ");
			response.append(remoteHost);
			response.append(" Duplicate LHLO");
		}
		sendResponse(ioSession, response.toString());
	}
	
}