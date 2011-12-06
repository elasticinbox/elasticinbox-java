/**
 * Copyright (c) 2011 Optimax Software Ltd
 * 
 * This file is part of ElasticInbox.
 * 
 * ElasticInbox is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 2 of the License, or (at your option) any later
 * version.
 * 
 * ElasticInbox is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * ElasticInbox. If not, see <http://www.gnu.org/licenses/>.
 */

package com.elasticinbox.lmtp.delivery;

import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ecyrd.speed4j.StopWatch;
import com.elasticinbox.lmtp.Activator;
import com.elasticinbox.lmtp.server.api.Blob;
import com.elasticinbox.lmtp.server.api.DeliveryException;
import com.elasticinbox.lmtp.server.api.LMTPAddress;
import com.elasticinbox.lmtp.server.api.LMTPEnvelope;
import com.elasticinbox.lmtp.server.api.LMTPReply;
import com.elasticinbox.core.MessageDAO;
import com.elasticinbox.core.OverQuotaException;
import com.elasticinbox.core.message.MimeParser;
import com.elasticinbox.core.message.MimeParserException;
import com.elasticinbox.core.message.id.MessageIdBuilder;
import com.elasticinbox.core.model.Mailbox;
import com.elasticinbox.core.model.Message;
import com.elasticinbox.core.model.ReservedLabels;

/**
 * Delivery agent implementation
 * 
 * @author Rustam Aliyev
 */
public class ElasticInboxDeliveryAgent implements IDeliveryAgent
{
	// (not so) unique delivery id, used only for identifying delivery thread in the logs
	private AtomicInteger deliveryId;

	private static final Logger logger = LoggerFactory
			.getLogger(ElasticInboxDeliveryAgent.class);

	private static Random random = new Random();

	private final MessageDAO messageDAO;

	public ElasticInboxDeliveryAgent(MessageDAO messageDAO)
	{
		this.messageDAO = messageDAO;

		// set initial deliveryId randomly
		deliveryId = new AtomicInteger(random.nextInt(100000));
	}

	@Override
	public void deliver(LMTPEnvelope env, Blob blob) throws IOException
	{
		// update delivery ID
		newDeliveryId();

		StopWatch stopWatch = Activator.getDefault().getStopWatch();
		List<LMTPAddress> recipients = env.getRecipients();
		Message message;

		try {
			MimeParser parser = new MimeParser();
			parser.parse(blob.getInputStream());
			message = parser.getMessage();
		} catch (MimeParserException mpe) {
			logger.error("DID" + deliveryId + ": unable to parse message: ", mpe);
			throw new DeliveryException("Unable to parse message: " + mpe.getMessage());
		} catch (IOException ioe) {
			logger.error("DID" + deliveryId + ": unable to read message stream: ", ioe);
			throw new DeliveryException("Unable to read message stream: " + ioe.getMessage());
		}

		message.setSize(blob.getRawSize()); // update message size
		message.addLabel(ReservedLabels.INBOX.getLabelId()); // default location

		logEnvelope(env, message);

		// Deliver to each recipient
		for (LMTPAddress recipient : recipients)
		{
			LMTPReply reply = LMTPReply.TEMPORARY_FAILURE; // default LMTP reply
			DeliveryAction deliveryAction = DeliveryAction.DELIVER; // default delivery action

			Mailbox mailbox = new Mailbox(recipient.getEmailAddress());
			String logMsg = new StringBuilder(" ").append(mailbox.getId())
								.append(" DID").append(deliveryId).toString();

			try {
				switch (deliveryAction) {
				case DELIVER:
					try {
						// generate new UUID
						UUID messageId = new MessageIdBuilder().
								setSentDate(message.getDate()).build();

						// store message
						messageDAO.put(mailbox, messageId, message, blob.getInputStream());
						
						// successfully delivered
						stopWatch.stop("DELIVERY.success", logMsg);
						reply = LMTPReply.DELIVERY_OK;
					} catch (OverQuotaException e) {
						// account is over quota, reject
						stopWatch.stop("DELIVERY.reject_overQuota", logMsg + " over quota");
						reply = LMTPReply.PERMANENT_FAILURE_OVER_QUOTA;
					} catch (IOException e) {
						// delivery error, defer
						stopWatch.stop("DELIVERY.defer", logMsg);
						logger.error("DID" + deliveryId + ": delivery error: ", e);
						reply = LMTPReply.TEMPORARY_FAILURE;
					}
					break;
				case DISCARD:
					// Local delivery is disabled.
					stopWatch.stop("DELIVERY.discard", logMsg);
					reply = LMTPReply.DELIVERY_OK;
					break;
				case DEFER:
					// Delivery to mailbox skipped. Let MTA retry again later.
					stopWatch.stop("DELIVERY.defer", logMsg);
					reply = LMTPReply.TEMPORARY_FAILURE;
					break;
				case REJECT:
					// Reject delivery. Account or mailbox not found.
					stopWatch.stop("DELIVERY.reject_nonExistent", logMsg + " unknown mailbox");
					reply = LMTPReply.NO_SUCH_USER;
				}
			} catch (Exception e) {
				stopWatch.stop("DELIVERY.defer_failure", logMsg);
				reply = LMTPReply.TEMPORARY_FAILURE;
				logger.error("DID" + deliveryId + ": delivery failed (defered): ", e);
			}

			recipient.setDeliveryStatus(reply); // set delivery status for invoker
		}

	}

	private void logEnvelope(final LMTPEnvelope env, final Message message)
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

	private void newDeliveryId() {
		deliveryId.incrementAndGet();
		if (deliveryId.intValue() > 99999) {
			deliveryId.set(100);
		}
	}

}
