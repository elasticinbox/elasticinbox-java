package com.elasticinbox.lmtp.server.command;

public class UnknownCommandException extends CommandException
{
	private static final long serialVersionUID = 6579786559432851561L;

	/**
	 * {@inheritDoc}
	 */
	public UnknownCommandException() {
		super();
	}

	/**
	 * {@inheritDoc}
	 */
	public UnknownCommandException(String string) {
		super(string);
	}

	/**
	 * {@inheritDoc}
	 */
	public UnknownCommandException(String string, Throwable throwable) {
		super(string, throwable);
	}

	/**
	 * {@inheritDoc}
	 */
	public UnknownCommandException(Throwable throwable) {
		super(throwable);
	}
}
