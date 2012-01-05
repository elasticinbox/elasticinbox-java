package com.elasticinbox.lmtp.server;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.james.protocols.lmtp.LMTPConfigurationImpl;

/**
 * This class holds the configuration options of the {@link LMTPServer}.
 * 
 * @author De Oliveira Edouard &lt;doe_wanted@yahoo.fr&gt;
 * @author Rustam Aliyev
 */
public class LMTPServerConfig extends LMTPConfigurationImpl
{
	/**
	 * Server name.
	 */
	private static final String SERVER_NAME = "ElasticInbox";

	/**
	 * The server host name. Defaults to a lookup of the local address.
	 */
	private String hostName = "localhost";

	/**
	 * The timeout for waiting for data on a connection is one minute: 60 sec
	 */
	public final static int CONNECTION_TIMEOUT = 60;

	public LMTPServerConfig()
	{
		try {
			this.hostName = InetAddress.getLocalHost().getCanonicalHostName();
		} catch (UnknownHostException e) {
			this.hostName = "localhost";
		}

		setHelloName(hostName);
		setSoftwareName(SERVER_NAME);

		// TODO: make msg size configurable
		//setMaxMessageSize(0);
	}

}