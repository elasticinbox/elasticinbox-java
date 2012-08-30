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

package com.elasticinbox.lmtp.delivery;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.james.protocols.smtp.MailEnvelope;
import org.apache.james.protocols.smtp.MailAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ecyrd.speed4j.StopWatch;
import com.elasticinbox.lmtp.Activator;
import com.elasticinbox.lmtp.filter.*;
import com.elasticinbox.lmtp.server.api.DeliveryException;
import com.elasticinbox.lmtp.server.api.DeliveryReturnCode;
import com.elasticinbox.core.MessageDAO;
import com.elasticinbox.core.OverQuotaException;
import com.elasticinbox.core.message.MimeParser;
import com.elasticinbox.core.message.MimeParserException;
import com.elasticinbox.core.message.id.MessageIdBuilder;
import com.elasticinbox.core.model.Mailbox;
import com.elasticinbox.core.model.Message;

/**
 * Delivery agent implementation
 * 
 * @author Rustam Aliyev
 */
public class ElasticInboxDeliveryAgent implements IDeliveryAgent
{
	private static final Logger logger = LoggerFactory
			.getLogger(ElasticInboxDeliveryAgent.class);

	private final MessageDAO messageDAO;

	public ElasticInboxDeliveryAgent(MessageDAO messageDAO)
	{
		this.messageDAO = messageDAO;
	}

	@Override
	public Map<MailAddress, DeliveryReturnCode> deliver(MailEnvelope env, final String deliveryId)
			throws IOException
	{
		StopWatch stopWatch = Activator.getDefault().getStopWatch();
		Message message;

		try {
			MimeParser parser = new MimeParser();
			parser.parse(env.getMessageInputStream());
			message = parser.getMessage();
		} catch (MimeParserException mpe) {
			logger.error("DID" + deliveryId + ": unable to parse message: ", mpe);
			throw new DeliveryException("Unable to parse message: " + mpe.getMessage());
		} catch (IOException ioe) {
			logger.error("DID" + deliveryId + ": unable to read message stream: ", ioe);
			throw new DeliveryException("Unable to read message stream: " + ioe.getMessage());
		}

		message.setSize((long) env.getSize()); // update message size

		FilterProcessor<Message> processor = new FilterProcessor<Message>();
		//processor.add(new NotificationMailFilter());
		processor.add(new SpamMailFilter());
		processor.add(new DefaultMailFilter());
		message = processor.doFilter(message);

		logEnvelope(env, message, deliveryId);

		Map<MailAddress, DeliveryReturnCode> replies = new HashMap<MailAddress, DeliveryReturnCode>();
		// Deliver to each recipient
		for (MailAddress recipient : env.getRecipients())
		{
			DeliveryReturnCode reply = DeliveryReturnCode.TEMPORARY_FAILURE; // default LMTP reply
			DeliveryAction deliveryAction = DeliveryAction.DELIVER; // default delivery action

			Mailbox mailbox = new Mailbox(recipient.toString());
			String logMsg = new StringBuilder(" ").append(mailbox.getId())
								.append(" DID").append(deliveryId).toString();

			try {
				switch (deliveryAction) {
				case DELIVER:
					try {
						// generate new UUID
						UUID messageId = new MessageIdBuilder().build();

						// store message
						messageDAO.put(mailbox, messageId, message, env.getMessageInputStream());

						// successfully delivered
						stopWatch.stop("DELIVERY.success", logMsg);
						reply = DeliveryReturnCode.OK;
					} catch (OverQuotaException e) {
						// account is over quota, reject
						stopWatch.stop("DELIVERY.reject_overQuota", logMsg + " over quota");
						reply = DeliveryReturnCode.OVER_QUOTA;
					} catch (IOException e) {
						// delivery error, defer
						stopWatch.stop("DELIVERY.defer", logMsg);
						logger.error("DID" + deliveryId + ": delivery error: ", e);
						reply = DeliveryReturnCode.TEMPORARY_FAILURE;
					}
					break;
				case DISCARD:
					// Local delivery is disabled.
					stopWatch.stop("DELIVERY.discard", logMsg);
					reply = DeliveryReturnCode.OK;
					break;
				case DEFER:
					// Delivery to mailbox skipped. Let MTA retry again later.
					stopWatch.stop("DELIVERY.defer", logMsg);
					reply = DeliveryReturnCode.TEMPORARY_FAILURE;
					break;
				case REJECT:
					// Reject delivery. Account or mailbox not found.
					stopWatch.stop("DELIVERY.reject_nonExistent", logMsg + " unknown mailbox");
					reply = DeliveryReturnCode.NO_SUCH_USER;
				}
			} catch (Exception e) {
				stopWatch.stop("DELIVERY.defer_failure", logMsg);
				reply = DeliveryReturnCode.TEMPORARY_FAILURE;
				logger.error("DID" + deliveryId + ": delivery failed (defered): ", e);
			}

			replies.put(recipient, reply); // set delivery status for invoker
		}
		return replies;
	}

	private void logEnvelope(final MailEnvelope env, final Message message, final String deliveryId)
	{
        logger.info("DID{}: size={}, nrcpts={}, from=<{}>, msgid={}",
            	new Object[] {
            		deliveryId,
	                message.getSize(),
	                env.getRecipients().size(),
	                env.getSender(),
	                message.getMessageId() == null ? "" : message.getMessageId()
            	});
	}

}
