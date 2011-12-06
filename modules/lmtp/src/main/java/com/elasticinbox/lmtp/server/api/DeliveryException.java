package com.elasticinbox.lmtp.server.api;

import java.io.IOException;

/**
 * Thrown by delivery backend when delivery failed due to problems with message
 * blob
 * 
 * @author Rustam Aliyev
 */
public class DeliveryException extends IOException
{
	private static final long serialVersionUID = 5323215105334028562L;

	/**
	 * {@inheritDoc}
	 */
	public DeliveryException() {
		super();
	}

	/**
	 * {@inheritDoc}
	 */
	public DeliveryException(String message) {
		super(message);
	}

}
