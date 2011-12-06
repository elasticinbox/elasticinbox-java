package com.elasticinbox.lmtp.server.api.handler;

import java.io.IOException;
import java.io.InputStream;


import com.elasticinbox.lmtp.server.api.LMTPAddress;
import com.elasticinbox.lmtp.server.api.LMTPEnvelope;
import com.elasticinbox.lmtp.server.api.RejectException;
import com.elasticinbox.lmtp.server.api.TooMuchDataException;
import com.elasticinbox.lmtp.server.api.handler.AbstractDeliveryHandler;
import com.elasticinbox.lmtp.server.api.handler.DeliveryContext;

/**
 * @author De Oliveira Edouard &lt;doe_wanted@yahoo.fr&gt;
 * @author Rustam Aliyev
 */
public class DefaultDeliveryHandler extends AbstractDeliveryHandler
{
	/**
	 * An instance of this implementation will be created for each SMTP session.
	 * 
	 * @param ctx
	 *            the delivery context
	 */
	protected DefaultDeliveryHandler(DeliveryContext ctx) {
		super(ctx);
	}

	@Override
	public LMTPEnvelope data(InputStream data, long size) throws RejectException,
			TooMuchDataException, IOException 
	{
		// TODO This is where mail data is really delivered
		// to the listeners
		return null;
	}

	@Override
	public void from(LMTPAddress from) throws RejectException {
		// Called once (unless session is reset). It gives the
		// email address of the sender
	}

	@Override
	public void recipient(LMTPAddress recipient) throws RejectException {
		// Called once for each recipient
	}

	@Override
	public void resetMessageState() {
		// TODO Called if SMTP session is reset thus allowing to clean
		// this class state.
	}
}
