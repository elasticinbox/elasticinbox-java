package com.elasticinbox.lmtp.server.api;

import java.io.IOException;

/**
 * Thrown by message listeners if an input stream provides more data than the
 * listener can handle.
 * 
 * @author De Oliveira Edouard &lt;doe_wanted@yahoo.fr&gt;
 * @author Jeff Schnitzer
 */
public class TooMuchDataException extends IOException
{
	private static final long serialVersionUID = -8591524533167917902L;

	/**
	 * {@inheritDoc}
	 */
	public TooMuchDataException() {
		super();
	}

	/**
	 * {@inheritDoc}
	 */
	public TooMuchDataException(String message) {
		super(message);
	}
}