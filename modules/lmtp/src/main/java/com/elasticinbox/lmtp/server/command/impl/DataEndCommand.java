package com.elasticinbox.lmtp.server.command.impl;

import java.io.IOException;

import org.apache.mina.core.session.IoSession;

import com.elasticinbox.lmtp.server.api.LMTPAddress;
import com.elasticinbox.lmtp.server.api.LMTPEnvelope;
import com.elasticinbox.lmtp.server.api.RejectException;
import com.elasticinbox.lmtp.server.api.TooMuchDataException;
import com.elasticinbox.lmtp.server.command.AbstractBaseCommand;
import com.elasticinbox.lmtp.server.core.mina.LMTPCodecDecoder;
import com.elasticinbox.lmtp.server.core.mina.LMTPContext;

/**
 * Data command splitted to adapt to MINA framework. Called when
 * <CR><LF>.<CR><LF> is received after entering DATA mode.
 * 
 * @author De Oliveira Edouard &lt;doe_wanted@yahoo.fr&gt;
 * @author Rustam Aliyev
 */
public class DataEndCommand extends AbstractBaseCommand
{
	public DataEndCommand() {
		super("DATA_END", null);
	}

	public void execute(String commandString, IoSession ioSession,
			LMTPContext ctx) throws TooMuchDataException, IOException
	{
		try {
			Long size = (Long) ioSession.getAttribute(
					LMTPCodecDecoder.DATASIZE_ATTRIBUTE, 0L);

			LMTPEnvelope envelope = ctx.getDeliveryHandler().data(
					ctx.getInputStream(), size);

			for (LMTPAddress recipient : envelope.getRecipients()) {
				sendResponse(ioSession, recipient.getDeliveryStatus().toString());
			}

			ResetCommand.resetContext(ctx);
		} catch (RejectException ex) {
			ResetCommand.resetContext(ctx);
			sendResponse(ioSession, ex.getMessage());
		}
	}
}