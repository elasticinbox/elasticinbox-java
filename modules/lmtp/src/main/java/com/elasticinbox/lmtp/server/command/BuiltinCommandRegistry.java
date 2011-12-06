package com.elasticinbox.lmtp.server.command;

import com.elasticinbox.lmtp.server.command.impl.DataCommand;
import com.elasticinbox.lmtp.server.command.impl.DataEndCommand;
import com.elasticinbox.lmtp.server.command.impl.HelpCommand;
import com.elasticinbox.lmtp.server.command.impl.LhloCommand;
import com.elasticinbox.lmtp.server.command.impl.MailCommand;
import com.elasticinbox.lmtp.server.command.impl.NoopCommand;
import com.elasticinbox.lmtp.server.command.impl.QuitCommand;
import com.elasticinbox.lmtp.server.command.impl.ReceiptCommand;
import com.elasticinbox.lmtp.server.command.impl.ResetCommand;
import com.elasticinbox.lmtp.server.command.impl.VerifyCommand;

/**
 * Enumerates all the internal {@link Command} available.
 * 
 * @author De Oliveira Edouard &lt;doe_wanted@yahoo.fr&gt;
 * @author Rustam Aliyev
 */
public enum BuiltinCommandRegistry
{
	DATA(DataCommand.class),
	EHLO(LhloCommand.class), 
	HELP(HelpCommand.class), 
	MAIL(MailCommand.class), 
	NOOP(NoopCommand.class), 
	QUIT(QuitCommand.class), 
	RCPT(ReceiptCommand.class), 
	RSET(ResetCommand.class), 
	VRFY(VerifyCommand.class), 

	// Add a fake command to handle the asynchronous end of DATA 
    DATA_END(DataEndCommand.class);

	private Class<? extends Command> commandClass;

	private BuiltinCommandRegistry(Class<? extends Command> c) {
		this.commandClass = c;
	}

	public String getClassName() {
		return commandClass.getSimpleName();
	}

	public Command getNewInstance() throws InstantiationException,
			IllegalAccessException {
		return (Command) this.commandClass.newInstance();
	}
}
