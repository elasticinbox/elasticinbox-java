/**
 * Copyright (c) 2011-2012 Optimax Software Ltd.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *  * Neither the name of Optimax Software, ElasticInbox, nor the names
 *    of its contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.elasticinbox.lmtp.server.api.handler;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.james.protocols.api.Response;
import org.apache.james.protocols.api.handler.WiringException;
import org.apache.james.protocols.lmtp.LMTPMultiResponse;
import org.apache.james.protocols.lmtp.core.DataLineMessageHookHandler;
import org.apache.james.protocols.smtp.MailAddress;
import org.apache.james.protocols.smtp.MailEnvelopeImpl;
import org.apache.james.protocols.smtp.SMTPResponse;
import org.apache.james.protocols.smtp.SMTPRetCode;
import org.apache.james.protocols.smtp.SMTPSession;
import org.apache.james.protocols.smtp.dsn.DSNStatus;

import com.elasticinbox.lmtp.delivery.IDeliveryAgent;
import com.elasticinbox.lmtp.server.api.DeliveryReturnCode;

/**
 * Default class that extends the {@link AbstractDeliveryHandler} class.
 * Provides a default implementation for mail delivery.
 * 
 * @author Rustam Aliyev
 */
public class ElasticInboxDeliveryHandler extends DataLineMessageHookHandler
{
	private final IDeliveryAgent backend;

	public ElasticInboxDeliveryHandler(IDeliveryAgent backend) {
		this.backend = backend;
	}

	@Override
	protected Response processExtensions(SMTPSession session, MailEnvelopeImpl env)
	{
		// tracing
		if (session.getLogger().isTraceEnabled()) {
			logMessage(session, env);
		}

		Map<MailAddress, DeliveryReturnCode> replies;
		// deliver message
		try {
			replies = backend.deliver(env);
		} catch (IOException e) {
			// TODO: Handle me
			replies = new HashMap<MailAddress, DeliveryReturnCode>();
			for (MailAddress address : env.getRecipients()) {
				replies.put(address, DeliveryReturnCode.TEMPORARY_FAILURE);
			}
		}

		LMTPMultiResponse lmtpResponse = null;
		for (MailAddress address : replies.keySet())
		{
			DeliveryReturnCode code = replies.get(address);
			SMTPResponse response;

			switch (code) {
			case OK:
				response = new SMTPResponse(SMTPRetCode.MAIL_OK,
						DSNStatus.getStatus(DSNStatus.SUCCESS, DSNStatus.UNDEFINED_STATUS)
						+ " Ok");
				break;
			case NO_SUCH_USER:
				response = new SMTPResponse(SMTPRetCode.MAILBOX_PERM_UNAVAILABLE,
						DSNStatus.getStatus(DSNStatus.PERMANENT, DSNStatus.ADDRESS_MAILBOX)
						+ " Unknown user " + address.toString());
				break;
			case OVER_QUOTA:
				response = new SMTPResponse(SMTPRetCode.QUOTA_EXCEEDED,
						DSNStatus.getStatus(DSNStatus.PERMANENT, DSNStatus.MAILBOX_FULL)
						+ " User over quota");
				break;
			case PERMANENT_FAILURE:
				response = new SMTPResponse(SMTPRetCode.TRANSACTION_FAILED,
						DSNStatus.getStatus(DSNStatus.PERMANENT, DSNStatus.SYSTEM_OTHER) 
						+ " Unable to deliver message");
				break;
			case TEMPORARY_FAILURE:
				response = new SMTPResponse(SMTPRetCode.LOCAL_ERROR,
						DSNStatus.getStatus(DSNStatus.TRANSIENT, DSNStatus.SYSTEM_OTHER) 
						+ " Unable to process request");
				break;
			default:
				response = new SMTPResponse(SMTPRetCode.LOCAL_ERROR,
						DSNStatus.getStatus(DSNStatus.TRANSIENT, DSNStatus.SYSTEM_OTHER)
						+ " Unable to process request");
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

    @SuppressWarnings("rawtypes")
    @Override
    public void wireExtensions(Class interfaceName, List extension) throws WiringException {
        // do nothing
    }

    @Override
    protected void checkMessageHookCount(List<?> messageHandlers) throws WiringException {
        // do noting
    }

    @Override
    public List<Class<?>> getMarkerInterfaces() {
        return Collections.emptyList();
    }
    
    /**
     * Log message contents
     * 
     * @param env
     */
    private void logMessage(SMTPSession session, MailEnvelopeImpl env)
    {
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

}