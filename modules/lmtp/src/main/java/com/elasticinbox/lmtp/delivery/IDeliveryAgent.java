package com.elasticinbox.lmtp.delivery;

import java.io.IOException;
import java.util.Map;

import org.apache.james.protocols.smtp.MailEnvelope;
import org.apache.mailet.MailAddress;

import com.elasticinbox.lmtp.server.api.LMTPReply;

/**
 * Delivery Agent Interface
 * 
 * @author Rustam Aliyev
 */
public interface IDeliveryAgent
{

	/**
	 * Delivers this message to the list of recipients in the message, and sets
	 * the delivery status on each recipient address.
	 * @throws IOException 
	 */
	public Map<MailAddress, LMTPReply> deliver(MailEnvelope env) throws IOException;

}
