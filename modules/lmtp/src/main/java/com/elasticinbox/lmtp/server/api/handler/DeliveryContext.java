package com.elasticinbox.lmtp.server.api.handler;

import java.io.InputStream;
import java.net.SocketAddress;

import com.elasticinbox.lmtp.server.LMTPServerConfig;

/**
 * Interface which provides context to the message handlers.
 * 
 * @author De Oliveira Edouard &lt;doe_wanted@yahoo.fr&gt;
 * @author Rustam Aliyev
 */
public interface DeliveryContext
{
	/**
	 * @return the server configuration.
	 */
	public LMTPServerConfig getLMTPServerConfig();

	/**
	 * @return the IP address of the remote server.
	 */
	public SocketAddress getRemoteAddress();

	/**
	 * @return the original data stream.
	 */
	public InputStream getInputStream();

}