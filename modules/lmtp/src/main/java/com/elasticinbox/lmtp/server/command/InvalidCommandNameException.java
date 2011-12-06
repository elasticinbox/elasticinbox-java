package com.elasticinbox.lmtp.server.command;

public class InvalidCommandNameException extends CommandException
{
	private static final long serialVersionUID = 8650069874808249416L;

	/**
	 * {@inheritDoc}
	 */
	public InvalidCommandNameException() {
		super();
	}

	/**
	 * {@inheritDoc}
	 */
	public InvalidCommandNameException(String string) {
		super(string);
	}

	/**
	 * {@inheritDoc}
	 */
	public InvalidCommandNameException(String string, Throwable throwable) {
		super(string, throwable);
	}

	/**
	 * {@inheritDoc}
	 */
	public InvalidCommandNameException(Throwable throwable) {
		super(throwable);
	}
}
