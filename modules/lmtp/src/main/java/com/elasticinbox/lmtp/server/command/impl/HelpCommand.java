package com.elasticinbox.lmtp.server.command.impl;

import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

import org.apache.mina.core.session.IoSession;

import com.elasticinbox.lmtp.server.LMTPServerConfig;
import com.elasticinbox.lmtp.server.command.AbstractBaseCommand;
import com.elasticinbox.lmtp.server.command.CommandException;
import com.elasticinbox.lmtp.server.core.mina.LMTPContext;

/**
 * The HELP command implementation.
 * 
 * @author Ian McFarland &lt;ian@neo.com&gt;
 * @author Jon Stevens
 * @author De Oliveira Edouard &lt;doe_wanted@yahoo.fr&gt;
 * @author Rustam Aliyev
 */
public class HelpCommand extends AbstractBaseCommand
{
	public HelpCommand()
	{
		super("HELP", "The HELP command gives help info about the topic specified.\r\n"
						+ "For a list of topics, type HELP by itself.\r\n", 
				"[<topic>]\n topic = the topic we want help info about\n");
	}

	public void execute(String commandString, IoSession ioSession, LMTPContext ctx) 
		throws IOException
	{
		String args = getArgPredicate(commandString);
		if ("".equals(args)) {
			sendResponse(ioSession, getCommandMessage(ctx.getLMTPServerConfig()));
			return;
		}

		try {
			sendResponse(ioSession, getHelp(args).toString());
		} catch (CommandException e) {
			sendResponse(ioSession, "504 HELP topic \"" + args + "\" unknown");
		}
	}

	private String getCommandMessage(LMTPServerConfig cfg)
	{
		StringBuilder response = new StringBuilder();
		response.append("214-This is ");
		response.append(cfg.getNameVersion());
		response.append(" server running on ");
		response.append(cfg.getHostName());
		response.append("\r\n");
		response.append("214-Available commands:\r\n");
		getFormattedCommandsList(response);
		response.append("214-For more info use \"HELP <command>\".\r\n");
		response.append("214-For more information about this server, visit:\r\n");
		response.append("214-    http://tedorg.free.fr/projects/projects.php?projects_section=3\r\n");
		response.append("214-To report bugs in the implementation, send email to:\r\n");
		response.append("214-    doe_wanted@yahoo.fr\r\n");
		response.append("214-For local information send email to Postmaster at your site.\r\n");
		response.append("214 End of HELP info");

		return response.toString();
	}

	private void getFormattedCommandsList(StringBuilder sb)
	{
		Set<String> set = new TreeSet<String>(super.getHelp().keySet());

		for (String key : set) {
			if ("DATA_END".equals(key))
				continue; // skip it as it is not a real command

			sb.append("214-  ");
			sb.append(key);
			sb.append("\r\n");
		}
	}
}
