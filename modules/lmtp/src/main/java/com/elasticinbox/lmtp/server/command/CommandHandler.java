package com.elasticinbox.lmtp.server.command;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elasticinbox.lmtp.server.core.mina.LMTPConnectionHandler;
import com.elasticinbox.lmtp.server.core.mina.LMTPContext;

/**
 * This class manages execution of a LMTP command.
 *
 * @author Jon Stevens
 * @author Edouard De Oliveira &lt;doe_wanted@yahoo.fr&gt;
 * @author Rustam Aliyev
 */
public class CommandHandler
{
	private Map<String, Command> commandMap = new HashMap<String, Command>();
	private static Logger log = LoggerFactory.getLogger(CommandHandler.class);

	/**
	 * Populates a default set of commands based with the BuiltinCommandRegistry
	 * commands.
	 */
	public CommandHandler()
	{
		for (BuiltinCommandRegistry entry : BuiltinCommandRegistry.values()) {
			try {
				addCommand(entry.getNewInstance());
			} catch (Exception e) {
				log.info("Unable to instantiate command {}", entry.getClassName());
			}
		}
	}

	/**
	 * Pass in a Collection of Command objects.
	 * 
	 * @param availableCommands
	 */
	public CommandHandler(Collection<Command> availableCommands)
	{
		for (Command command : availableCommands) {
			addCommand(command);
		}
	}

	/**
	 * Adds a new command to the map.
	 * 
	 * @param command
	 *            the command to add
	 */
	public void addCommand(Command command)
	{
		log.debug("Added command {}", command.getName());

		if (command instanceof AbstractBaseCommand) {
			((AbstractBaseCommand) command).setCommandHandler(this);
		}
		
		this.commandMap.put(command.getName(), command);
	}

	/**
	 * Does the map contain the named command?
	 * 
	 * @param command
	 * @return true if the command exists
	 */
	public boolean containsCommand(String command) {
		return this.commandMap.containsKey(command);
	}

	/**
	 * Calls the execute method on a command.
	 * 
	 * @param context
	 * @param commandString
	 * @throws SocketTimeoutException
	 * @throws IOException
	 */
	public void handleCommand(String commandString, IoSession session, LMTPContext ctx)
		throws IOException
	{
		try {
			Command cmd = getCommandFromString(commandString);
			cmd.execute(commandString, session, ctx);
		} catch (CommandException e) {
			LMTPConnectionHandler
					.sendResponse(session, "500 " + e.getMessage());
		}
	}

	/**
	 * Given a string, find the Command object.
	 * 
	 * @param commandString
	 * @return The command object.
	 * @throws UnknownCommandException
	 * @throws InvalidCommandNameException
	 */
	public Command getCommandFromString(String commandString)
		throws UnknownCommandException, InvalidCommandNameException
	{
        String verb = toVerb(commandString);

	    Command command = this.commandMap.get(verb);
		if (command == null) {
		    throw new UnknownCommandException("Command not implemented");
		}
		
		return command;
	}
	
	/**
	 * 
	 * @param string
	 * @return
	 * @throws InvalidCommandNameException
	 */
	private String toVerb(String string) throws InvalidCommandNameException
	{
        if (string == null || string.length() < 4) {
        	throw new InvalidCommandNameException("Syntax error");
        }

        StringTokenizer st = new StringTokenizer(string);
        
        if (!st.hasMoreTokens()) {
        	throw new InvalidCommandNameException("Syntax error");
        }
		
        return st.nextToken().toUpperCase();
	}
}