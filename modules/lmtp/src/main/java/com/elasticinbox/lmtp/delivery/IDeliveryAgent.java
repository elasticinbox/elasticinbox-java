package com.elasticinbox.lmtp.delivery;

import java.io.IOException;

import com.elasticinbox.lmtp.server.api.Blob;
import com.elasticinbox.lmtp.server.api.LMTPEnvelope;

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
	public void deliver(LMTPEnvelope env, Blob blob) throws IOException;

}
