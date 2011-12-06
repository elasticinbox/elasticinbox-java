package com.elasticinbox.lmtp.server.command.impl;

import java.util.StringTokenizer;

/**
 * This is a helper class to generate the help messages
 * provided by the lmtp HELP command.
 *  
 * @author Ian McFarland &lt;ian@neo.com&gt;
 * @author Jon Stevens
 * @author De Oliveira Edouard &lt;doe_wanted@yahoo.fr&gt;
 * @author Rustam Aliyev
 */
public class HelpMessage
{
	private String commandName;
	private String argumentsDescription;
	private String helpMessage;
	private String outputString;

	public HelpMessage(String commandName, String helpMessage) {
		this(commandName, helpMessage, null);
	}

	public HelpMessage(String commandName, String helpMessage, String argumentsDescription)
	{
		this.commandName = commandName;
		this.argumentsDescription = argumentsDescription;
		this.helpMessage = helpMessage;
		buildOutputString();
	}

	public String getName() {
		return this.commandName;
	}

	public String toString() {
		return outputString;
	}

	private void buildOutputString()
	{
		StringBuilder sb = new StringBuilder();
		StringTokenizer tk = new StringTokenizer(helpMessage, "\n");
		
		while (tk.hasMoreTokens()) {
			sb.append("214-");
			sb.append(tk.nextToken()).append("\r\n");
		}

		if (argumentsDescription != null) {
			sb.append("214-").append(commandName).append(' ');

			tk = new StringTokenizer(argumentsDescription, "\n");
			while (tk.hasMoreTokens()) {
				sb.append(tk.nextToken());
				if (tk.hasMoreTokens())
					sb.append("\r\n214-    ");
			}
			sb.append("\r\n");
		}

		sb.append("214 End of ").append(commandName).append(" info");
		outputString = sb.toString();
	}

	public boolean equals(Object o)
	{
		if (this == o)
			return true;
		
		if (o == null || getClass() != o.getClass())
			return false;
		
		final HelpMessage that = (HelpMessage) o;
		if (argumentsDescription != null 
				? !argumentsDescription.equals(that.argumentsDescription)
				: that.argumentsDescription != null)
			return false;
		
		if (commandName != null ? !commandName.equals(that.commandName)
				: that.commandName != null)
			return false;
		
		if (helpMessage != null ? !helpMessage.equals(that.helpMessage)
				: that.helpMessage != null)
			return false;
		
		return true;
	}

	public int hashCode()
	{
		int result;
		result = (commandName != null ? commandName.hashCode() : 0);
		result = 29 * result
				+ (argumentsDescription != null ? argumentsDescription.hashCode() : 0);
		result = 29 * result
				+ (helpMessage != null ? helpMessage.hashCode() : 0);
		return result;
	}
}