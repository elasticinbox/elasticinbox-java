package com.elasticinbox.lmtp.server.core.mina;

import java.io.InputStream;
import java.net.SocketAddress;

import org.apache.mina.core.session.IoSession;

import com.elasticinbox.lmtp.server.LMTPServerConfig;
import com.elasticinbox.lmtp.server.api.handler.AbstractDeliveryHandler;
import com.elasticinbox.lmtp.server.api.handler.DeliveryContext;
import com.elasticinbox.lmtp.server.core.DeliveryHandlerFactory;
import com.elasticinbox.lmtp.server.core.Session;

/**
 * The context of a LMTP session.
 * 
 * @author De Oliveira Edouard &lt;doe_wanted@yahoo.fr&gt;
 * @author Rustam Aliyev
 */
public class LMTPContext implements DeliveryContext
{
	private LMTPServerConfig cfg;
	private Session session;
	private SocketAddress remoteAddress;
	private InputStream inputStream;
	private AbstractDeliveryHandler deliveryHandler;

	public LMTPContext(LMTPServerConfig cfg, DeliveryHandlerFactory factory,
			IoSession ioSession)
	{
		this.cfg = cfg;
		this.remoteAddress = ioSession.getRemoteAddress();
		this.session = new Session();
		this.deliveryHandler = factory.create(this);
	}

	public AbstractDeliveryHandler getDeliveryHandler() {
		return deliveryHandler;
	}

	public InputStream getInputStream() {
		return inputStream;
	}

	public void setInputStream(InputStream inputStream) {
		this.inputStream = inputStream;
	}

	public Session getSession() {
		return session;
	}

	public SocketAddress getRemoteAddress() {
		return remoteAddress;
	}

	public LMTPServerConfig getLMTPServerConfig() {
		return cfg;
	}
}
