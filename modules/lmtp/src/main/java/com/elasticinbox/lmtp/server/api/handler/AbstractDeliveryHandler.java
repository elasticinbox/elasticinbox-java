package com.elasticinbox.lmtp.server.api.handler;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketAddress;
import java.util.HashMap;

import com.elasticinbox.lmtp.delivery.IDeliveryBackend;
import com.elasticinbox.lmtp.server.api.LMTPAddress;
import com.elasticinbox.lmtp.server.api.LMTPEnvelope;
import com.elasticinbox.lmtp.server.api.RejectException;
import com.elasticinbox.lmtp.server.api.SessionContext;
import com.elasticinbox.lmtp.server.api.TooMuchDataException;

/**
 * A simple base class to make implementation of delivery handlers easier.
 * 
 * @author De Oliveira Edouard &lt;doe_wanted@yahoo.fr&gt;
 * @author Rustam Aliyev
 */
abstract public class AbstractDeliveryHandler 	
{
	private class SessionContextImpl implements SessionContext
	{
		private HashMap<String, Object> attrs = new HashMap<String, Object>();
		
		public SessionContextImpl() {
		}

		public void addAttribute(String key, Object attr) {
			attrs.put(key, attr);
		}

		public Object getAttribute(String key) {
			return attrs.get(key);
		}

		public SocketAddress getRemoteAddress() {
			return ctx.getRemoteAddress();
		}

		public void removeAttribute(String key) {
			attrs.remove(key);
		}

		public void setAttribute(String key, Object attr) {
			attrs.put(key, attr);
		}

	}
	
	private IDeliveryBackend backend;
	private DeliveryContext ctx;
	private SessionContextImpl sessionCtx;

	protected AbstractDeliveryHandler(DeliveryContext ctx)
	{
		this.ctx = ctx;
		this.sessionCtx = new SessionContextImpl();
	}

	public DeliveryContext getDeliveryContext() {
		return ctx;
	}

	public SessionContext getSessionContext() {
		return sessionCtx;
	}
	
	public IDeliveryBackend getDeliveryBackend() {
		return backend;
	}

	public void setDeliveryBackend(IDeliveryBackend backend) {
		this.backend = backend;
	}

	/** */
	public void resetState()
	{
	}
	
	/**
	 * Called first, after the MAIL FROM during a LMTP exchange.
	 *
	 * @param from is the sender as specified by the client.  It will
	 *  be a rfc822-compliant email address, already validated by
	 *  the server.
	 * @throws RejectException if the sender should be denied.
	 */
	public abstract void from(LMTPAddress from) throws RejectException;
	
	/**
	 * Called once for every RCPT TO during a LMTP exchange.
	 * This will occur after a from() call.
	 *
	 * @param recipient is a rfc822-compliant email address,
	 *  validated by the server.
	 * @throws RejectException if the recipient should be denied.
	 */
	public abstract void recipient(LMTPAddress recipient) throws RejectException;
	
	/**
	 * Called when the DATA part of the LMTP exchange begins.  Will
	 * only be called if at least one recipient was accepted.
	 *
	 * @param data will be the lmtp data stream, stripped of any extra '.' chars
	 * @return 
	 *
	 * @throws RejectException if at any point the data should be rejected.
	 * @throws TooMuchDataException if the listener can't handle that much data.
	 *         An error will be reported to the client.
	 * @throws IOException if there is an IO error reading the input data.
	 */
	public abstract LMTPEnvelope data(InputStream data, long size) throws RejectException, TooMuchDataException, IOException;
	
	/**
	 * This method is called whenever a RSET command is sent or after the end of 
	 * the DATA command. It can be used to clean up any pending deliveries.
	 */
	public abstract void resetMessageState();	
}