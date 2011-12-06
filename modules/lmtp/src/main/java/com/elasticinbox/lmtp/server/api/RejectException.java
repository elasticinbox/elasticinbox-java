package com.elasticinbox.lmtp.server.api;

/**
 * Thrown to reject an LMTP command with a specific code.
 * 
 * @author De Oliveira Edouard &lt;doe_wanted@yahoo.fr&gt;
 * @author Jeff Schnitzer
 * @author Rustam Aliyev
 */
public class RejectException extends Exception
{
	private static final long serialVersionUID = -2518325451992929294L;

	/**
	 * The lmtp error code
	 */
	int code;

	/**
	 * {@inheritDoc}
	 */
	public RejectException() {
		this(554, "Transaction failed");
	}

	/**
	 * {@inheritDoc}
	 */
	public RejectException(int code, String message) {
		super(code + " " + message);
		this.code = code;
	}

	/**
	 * Returns the lmtp error code.
	 */
	public int getCode() {
		return this.code;
	}
}
