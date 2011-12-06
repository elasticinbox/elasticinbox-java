package com.elasticinbox.lmtp.server.api.handler;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elasticinbox.lmtp.server.api.Blob;
import com.elasticinbox.lmtp.server.api.LMTPAddress;
import com.elasticinbox.lmtp.server.api.LMTPEnvelope;
import com.elasticinbox.lmtp.server.api.RejectException;
import com.elasticinbox.lmtp.server.api.TooMuchDataException;
import com.elasticinbox.lmtp.utils.MimeUtils;

/**
 * Default class that extends the {@link AbstractDeliveryHandler} class.
 * Provides a default implementation for mail delivery.
 * 
 * @author De Oliveira Edouard &lt;doe_wanted@yahoo.fr&gt;
 * @author Rustam Aliyev
 */
public class ElasticInboxDeliveryHandler extends AbstractDeliveryHandler
{
	private static final Logger logger = 
			LoggerFactory.getLogger(ElasticInboxDeliveryHandler.class);

	private LMTPEnvelope envelope;

	public ElasticInboxDeliveryHandler(DeliveryContext ctx) {
		super(ctx);
		envelope = new LMTPEnvelope();
	}

	/**
	 * {@inheritDoc}
	 */
	public void from(LMTPAddress from) throws RejectException {
		envelope.setSender(from);
	}

	/** 
	 * {@inheritDoc}
	 */	
	public void recipient(LMTPAddress recipient) throws RejectException {
		envelope.addRecipient(recipient);
	}

	/**
	 * {@inheritDoc}
	 */
	public void resetMessageState() {
		this.envelope = new LMTPEnvelope();
	}

	/**
	 * Implementation of the data receiving portion of things. By default
	 * deliver a copy of the stream to each recipient of the message(the first 
	 * recipient is provided the original stream to save memory space). If
	 * you would like to change this behaviour, then you should implement
	 * the MessageHandler interface yourself.
	 */
	public LMTPEnvelope data(InputStream data, long size) throws IOException,
			TooMuchDataException
	{
		// build message blob
		Blob blob = new Blob(data, size);
		blob.prepend(getAdditionalHeaders());

		// tracing
		if (logger.isTraceEnabled()) {
			Charset charset = getDeliveryContext().getLMTPServerConfig().getCharset();
			InputStream in = blob.getInputStream();
			byte[] buf = new byte[16384];

			try {
				CharsetDecoder decoder = charset.newDecoder();
				int len = 0;
				while ((len = in.read(buf)) >= 0) {
					logger.trace(decoder.decode(ByteBuffer.wrap(buf, 0, len)).toString());
				}
			} catch (IOException ioex) {
				logger.debug("Mail data logging failed", ioex);
			}
		}

		// deliver message
		getDeliveryBackend().deliver(envelope, blob);

		// return delivery statuses 
		return envelope;
	}

	/**
	 * Generates the <tt>Return-Path</tt> and <tt>Received</tt> headers for the
	 * current incoming message.
	 */
	protected String getAdditionalHeaders()
	{
		StringBuilder headers = new StringBuilder();

		// assemble Return-Path header
		if (envelope.hasSender()) {
			String sender = envelope.getSender().getEmailAddress();
			if (sender != null && sender.trim().length() > 0) {
				headers.append(String.format("Return-Path: %s\r\n", sender));
			}
		}

		// assemble Received header
		headers.append("Received: ");

		String timestamp = MimeUtils.getDateAsRFC822String(new Date());
		String localHost = getDeliveryContext().getLMTPServerConfig().getHostName();
		InetSocketAddress remoteHost = (InetSocketAddress) getSessionContext().getRemoteAddress();

		String value = String.format("from %s ([%s])\r\n\tby %s with LMTP; %s",
				remoteHost.getHostName(), remoteHost.getAddress().getHostAddress(), 
				localHost, timestamp);
		//headers.append(MimeUtils.fold(value));
		headers.append(value).append("\r\n");

		return headers.toString();
	}

}