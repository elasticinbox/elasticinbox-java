package com.elasticinbox.lmtp.server.command;

import java.io.IOException;

import org.apache.mina.core.session.IoSession;

import com.elasticinbox.lmtp.server.core.mina.LMTPContext;

/**
 * The interface each LMTP command has to implement.
 * 
 * @author Edouard De Oliveira &lt;doe_wanted@yahoo.fr&gt;
 * @author Rustam Aliyev
 */
public interface Command
{
	public void execute(String commandString, IoSession ioSession,
			LMTPContext ctx) throws IOException;

	public String getName();
}
