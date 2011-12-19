package com.elasticinbox.lmtp.server.api.handler;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Date;

import org.apache.james.protocols.api.Response;
import org.apache.james.protocols.lmtp.LMTPMultiResponse;
import org.apache.james.protocols.smtp.MailEnvelope;
import org.apache.james.protocols.smtp.MailEnvelopeImpl;
import org.apache.james.protocols.smtp.SMTPResponse;
import org.apache.james.protocols.smtp.SMTPSession;
import org.apache.james.protocols.smtp.core.DataLineMessageHookHandler;
import org.apache.mailet.MailAddress;

import com.elasticinbox.lmtp.delivery.IDeliveryAgent;
import com.elasticinbox.lmtp.server.api.Blob;
import com.elasticinbox.lmtp.server.api.LMTPAddress;
import com.elasticinbox.lmtp.server.api.LMTPEnvelope;
import com.elasticinbox.lmtp.utils.MimeUtils;

/**
 * Default class that extends the {@link AbstractDeliveryHandler} class.
 * Provides a default implementation for mail delivery.
 * 
 * @author De Oliveira Edouard &lt;doe_wanted@yahoo.fr&gt;
 * @author Rustam Aliyev
 */
public class ElasticInboxDeliveryHandler extends DataLineMessageHookHandler {

	
    private final IDeliveryAgent backend;


	public ElasticInboxDeliveryHandler(IDeliveryAgent backend) {
        this.backend = backend;
    }
    
	/**
	 * Generates the <tt>Return-Path</tt> and <tt>Received</tt> headers for the
	 * current incoming message.
	 */
	protected String getAdditionalHeaders(SMTPSession session, MailEnvelope env)
	{
		StringBuilder headers = new StringBuilder();

		// assemble Return-Path header
		if (env.getSender() != null) {
			String sender = env.getSender().toString();
			if (sender != null && sender.trim().length() > 0) {
				headers.append(String.format("Return-Path: %s\r\n", sender));
			}
		}

		// assemble Received header
		headers.append("Received: ");

		String timestamp = MimeUtils.getDateAsRFC822String(new Date());
		String localHost = session.getLocalAddress().getHostName();
		InetSocketAddress remoteHost = session.getRemoteAddress();

		String value = String.format("from %s ([%s])\r\n\tby %s with LMTP; %s",
				remoteHost.getHostName(), remoteHost.getAddress().getHostAddress(), 
				localHost, timestamp);
		//headers.append(MimeUtils.fold(value));
		headers.append(value).append("\r\n");

		return headers.toString();
	}

	@Override
	protected Response processExtensions(SMTPSession session, MailEnvelopeImpl env) {
	    // build message blob
        Blob blob = new Blob(env.getMessageInputStream(), env.getSize());
        blob.prepend(getAdditionalHeaders(session, env));

        // tracing
        if (session.getLogger().isTraceEnabled()) {
            // TODO: Fix me
            Charset charset = Charset.forName("US-ASCII");
      
            try {
                InputStream in = blob.getInputStream();
                byte[] buf = new byte[16384];
                CharsetDecoder decoder = charset.newDecoder();
                int len = 0;
                while ((len = in.read(buf)) >= 0) {
                    session.getLogger().trace(decoder.decode(ByteBuffer.wrap(buf, 0, len)).toString());
                }
            } catch (IOException ioex) {
                session.getLogger().debug("Mail data logging failed", ioex);
            }
        }

        LMTPEnvelope lmtpEnv = new LMTPEnvelope();
        for (MailAddress rcpt: env.getRecipients()) {
            lmtpEnv.addRecipient(new LMTPAddress(rcpt.toString(), new String[] {}, null));
        }
        MailAddress sender = env.getSender();
        if (sender != null) {
            lmtpEnv.setSender(new LMTPAddress(sender.toString(),  new String[]{"BODY", "SIZE"}, null));
        }
        // deliver message
        try {
			backend.deliver(lmtpEnv, blob);
		} catch (IOException e) {
			// TODO: Handle me
		}

        LMTPMultiResponse lmtpResponse = null;
        for (LMTPAddress address: lmtpEnv.getRecipients()) {
            SMTPResponse response = new SMTPResponse(address.getDeliveryStatus().toString());
            if (lmtpResponse == null) {
                lmtpResponse = new LMTPMultiResponse(response);
            } else {
                lmtpResponse.addResponse(response);
            }
        }
        return lmtpResponse;
	}


}