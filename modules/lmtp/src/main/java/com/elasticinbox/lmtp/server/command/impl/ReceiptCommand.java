package com.elasticinbox.lmtp.server.command.impl;

import java.io.IOException;

import org.apache.mina.core.session.IoSession;

import com.elasticinbox.lmtp.server.api.LMTPAddress;
import com.elasticinbox.lmtp.server.api.LMTPReply;
import com.elasticinbox.lmtp.server.api.RejectException;
import com.elasticinbox.lmtp.server.command.AbstractBaseCommand;
import com.elasticinbox.lmtp.server.core.Session;
import com.elasticinbox.lmtp.server.core.mina.LMTPContext;

/**
 * The RCPT command implementation.
 * 
 * @author De Oliveira Edouard &lt;doe_wanted@yahoo.fr&gt;
 * @author Ian McFarland &lt;ian@neo.com&gt;
 * @author Jon Stevens
 * @author Jeff Schnitzer
 * @author Rustam Aliyev
 */
public class ReceiptCommand extends AbstractBaseCommand
{
	private final static String RECIPIENT_DELIMITER = "+";

	public ReceiptCommand()
	{
		super("RCPT", "The RCPT command specifies the recipient. This command can be used\n" +
				"any number of times to specify multiple recipients.", 
				"TO: <recipient>\n recipient = the email address of the recipient of the message");
	}

	public void execute(String commandString, IoSession ioSession, LMTPContext ctx) 
		throws IOException
	{
		Session session = ctx.getSession();
		if (!session.getHasSender()) {
			sendResponse(ioSession, LMTPReply.MISSING_MAIL_TO);
			return;
		}

		int max = ctx.getLMTPServerConfig().getMaxRecipients();
		if (max > -1 && session.getRecipientCount() >= max) {
			sendResponse(ioSession, LMTPReply.TOO_MANY_RECIPIENTS);
			return;
		}

		String args = getArgPredicate(commandString);
		if (!args.toUpperCase().startsWith("TO:")) {
			sendResponse(ioSession, LMTPReply.INVALID_RECIPIENT_ADDRESS);
			return;
		} else {
			// skip TO:
			final int toColonLength = 3;
			args = args.substring(toColonLength);

			LMTPAddress recipientAddress = 
					new LMTPAddress(args, null, RECIPIENT_DELIMITER);

			if(recipientAddress.isValid()) {
				try {
					ctx.getDeliveryHandler().recipient(recipientAddress);
					session.addRecipient();
					sendResponse(ioSession, LMTPReply.OK);
				} catch (RejectException ex) {
					sendResponse(ioSession, ex.getMessage());
				}
			} else {
				sendResponse(ioSession, LMTPReply.INVALID_RECIPIENT_ADDRESS);
			}
		}
	}
}
