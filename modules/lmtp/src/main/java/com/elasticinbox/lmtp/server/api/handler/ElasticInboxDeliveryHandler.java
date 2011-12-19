package com.elasticinbox.lmtp.server.api.handler;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.james.protocols.api.Response;
import org.apache.james.protocols.lmtp.LMTPMultiResponse;
import org.apache.james.protocols.smtp.MailEnvelope;
import org.apache.james.protocols.smtp.MailEnvelopeImpl;
import org.apache.james.protocols.smtp.SMTPResponse;
import org.apache.james.protocols.smtp.SMTPRetCode;
import org.apache.james.protocols.smtp.SMTPSession;
import org.apache.james.protocols.smtp.core.DataLineMessageHookHandler;
import org.apache.james.protocols.smtp.dsn.DSNStatus;
import org.apache.mailet.MailAddress;

import com.elasticinbox.lmtp.delivery.IDeliveryAgent;
import com.elasticinbox.lmtp.server.api.DeliveryReturnCode;
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
	    // TODO: add header is fron
	    // getAdditionalHeaders(session, env)

        // tracing
        if (session.getLogger().isTraceEnabled()) {
            // TODO: Fix me
            Charset charset = Charset.forName("US-ASCII");
      
            try {
                InputStream in = env.getMessageInputStream();
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

       
        Map<MailAddress, DeliveryReturnCode> replies;
        // deliver message
        try {
			replies = backend.deliver(env);
		} catch (IOException e) {
			// TODO: Handle me
            replies = new HashMap<MailAddress, DeliveryReturnCode>();
	        for (MailAddress address: env.getRecipients()) {
	            replies.put(address, DeliveryReturnCode.TEMPORARY_FAILURE);
	        }
		}

        LMTPMultiResponse lmtpResponse = null;
        for (MailAddress address: replies.keySet()) {
            DeliveryReturnCode code = replies.get(address);
            SMTPResponse response;
            switch(code) {
                case NO_SUCH_USER:
                    response = new SMTPResponse(SMTPRetCode.MAILBOX_PERM_UNAVAILABLE, DSNStatus.getStatus(DSNStatus.PERMANENT,DSNStatus.ADDRESS_MAILBOX) + " Unknown user: " + address.toString());
                    break;
                case OK:
                    response = new SMTPResponse(SMTPRetCode.MAIL_OK, DSNStatus.getStatus(DSNStatus.PERMANENT,DSNStatus.ADDRESS_MAILBOX) + " Unknown user: " + address.toString());
                    break;
                case OVER_QUOTA:
                    response = new SMTPResponse(SMTPRetCode.LOCAL_ERROR, "User over quita");
                    break;
                case PERMANENT_FAILURE:
                    response = new SMTPResponse(SMTPRetCode.TRANSACTION_FAILED, "Unable to deliver message");
                    break;
                case TEMPORARY_FAILURE:
                    response = new SMTPResponse(SMTPRetCode.LOCAL_ERROR, "Unable to process request");
                    break;
                default:
                    response = new SMTPResponse(SMTPRetCode.LOCAL_ERROR, "Unable to process request");
                    break;
            }
            if (lmtpResponse == null) {
                lmtpResponse = new LMTPMultiResponse(response);
            } else {
                lmtpResponse.addResponse(response);
            }
            
        }
        return lmtpResponse;
	}


}