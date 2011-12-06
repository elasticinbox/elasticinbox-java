package com.elasticinbox.lmtp.server.command.impl;

import java.io.IOException;

import org.apache.mina.core.session.IoSession;

import com.elasticinbox.lmtp.server.api.LMTPAddress;
import com.elasticinbox.lmtp.server.api.LMTPBodyType;
import com.elasticinbox.lmtp.server.api.LMTPReply;
import com.elasticinbox.lmtp.server.api.RejectException;
import com.elasticinbox.lmtp.server.command.AbstractBaseCommand;
import com.elasticinbox.lmtp.server.core.Session;
import com.elasticinbox.lmtp.server.core.mina.LMTPContext;

/**
 * The MAIL command implementation.
 * 
 * @author De Oliveira Edouard &lt;doe_wanted@yahoo.fr&gt;
 * @author Ian McFarland &lt;ian@neo.com&gt;
 * @author Jon Stevens
 * @author Rustam Aliyev
 */
public class MailCommand extends AbstractBaseCommand
{
	public MailCommand()
	{
		super("MAIL", "The MAIL FROM command specifies the sender", 
				"FROM: <address>\n address = the email address of the sender");
	}

	public void execute(String commandString, IoSession ioSession, LMTPContext ctx) 
		throws IOException
	{
		Session session = ctx.getSession();
		if (!session.getHasSeenHelo()) {
			sendResponse(ioSession, "503 Error: send HELO/EHLO first");
		} else if (session.getHasSender()) {
			sendResponse(ioSession, "503 Sender already specified");
		} else {
			if (commandString.trim().equals("MAIL FROM:")) {
				sendResponse(ioSession, "501 Syntax: MAIL FROM: <address>");
				return;
			}

			String args = getArgPredicate(commandString);
			if (!args.toUpperCase().startsWith("FROM:")) {
				sendResponse(ioSession, LMTPReply.INVALID_SENDER_ADDRESS);
				return;
			}

			// skip FROM: part
			final int fromColonLength = 5;
			args = args.substring(fromColonLength);

			LMTPAddress senderAddress = 
					new LMTPAddress(args, new String[]{"BODY", "SIZE"}, null);

			if (!senderAddress.isValid()) {
				sendResponse(ioSession, LMTPReply.INVALID_SENDER_ADDRESS);
				//sendResponse(ioSession, "553 <" + emailAddress + "> Invalid email address");
				return;
			}

			LMTPBodyType type = null;
			String body = senderAddress.getParameter("BODY");
			if (body != null) {
				type = LMTPBodyType.getInstance(body);
				if (type == null) {
					sendResponse(ioSession, LMTPReply.INVALID_BODY_PARAMETER);
					return;
				}
			}

			String size = senderAddress.getParameter("SIZE");
			if (size != null) {
				try {
					Integer.parseInt(size);
				} catch (NumberFormatException nfe) {
					sendResponse(ioSession, LMTPReply.INVALID_SIZE_PARAMETER);
					return;
				}
			}

			try {
				ctx.getDeliveryHandler().from(senderAddress);
				session.setHasSender(true);
				sendResponse(ioSession, LMTPReply.OK);
			} catch (RejectException ex) {
				sendResponse(ioSession, ex.getMessage());
			}
			
		}
	}
}
