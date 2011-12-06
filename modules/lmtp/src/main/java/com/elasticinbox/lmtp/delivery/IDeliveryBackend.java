package com.elasticinbox.lmtp.delivery;

import com.elasticinbox.lmtp.server.api.Blob;
import com.elasticinbox.lmtp.server.api.LMTPAddress;
import com.elasticinbox.lmtp.server.api.LMTPEnvelope;
import com.elasticinbox.lmtp.server.api.LMTPReply;
import com.elasticinbox.lmtp.validator.IValidator;

/**
 * Delivery Backend Interface
 * 
 * @author Rustam Aliyev
 */
public interface IDeliveryBackend extends IDeliveryAgent, IValidator
{
	/**
	 * Gets account status.
	 */
	public LMTPReply getAddressStatus(LMTPAddress address);

	/**
	 * Delivers this message to the list of recipients in the message, and sets
	 * the delivery status on each recipient address.
	 */
	public void deliver(LMTPEnvelope env, Blob blob);

}