package com.elasticinbox.lmtp.server;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;

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
	public static final String NAME = "ElasticInboxLDA";

	/**
	 * Server version
	 */
	public static final String VERSION = "0.2.0";

	/**
	 * 32K by default. The server will buffer incoming messages to disk when
	 * they hit this limit in the DATA received.
	 */
	public final static int DEFAULT_DATA_DEFERRED_SIZE = 1024 * 32;

	/**
	 * The default charset is ISO-8859-1.
	 */
	public final static Charset DEFAULT_CHARSET = Charset.forName("ISO-8859-1");

	/**
	 * The charset in use. Defaults to DEFAULT_CHARSET.
	 */
	private Charset charset = DEFAULT_CHARSET;

	/**
	 * The server host name. Defaults to a lookup of the local address.
	 */
	private String hostName;

	/**
	 * The maximal number of recipients that this server accepts per message
	 * delivery request. Default value is set to 1000.
	 */
	private int maxRecipients = 1000;

	/**
	 * The connection backlog. Defaults to 5000.
	 */
	private int backlog = 5000;

	/**
	 * The socket receive buffer size. Set to -1 to keep MINA/OS default buffer size.
	 */
	private int receiveBufferSize = -1;

	/**
	 * Data limit size before writing it to the disk. Defaults to
	 * DEFAULT_DATA_DEFERRED_SIZE.
	 */
	private int dataDeferredSize = DEFAULT_DATA_DEFERRED_SIZE;

	/**
	 * Set a hard limit on the maximum number of connections this server will
	 * accept once we reach this limit, the server will gracefully reject new
	 * connections. Default is 1000.
	 */
	private int maxConnections = 1000;

	/**
	 * The timeout for waiting for data on a connection is one minute: 1000 *
	 * 60.
	 */
	private int connectionTimeout = 1000 * 60;

	protected LMTPServerConfig() {
		try {
			this.hostName = InetAddress.getLocalHost().getCanonicalHostName();
		} catch (UnknownHostException e) {
			this.hostName = "localhost";
		}
	}

	/**
	 * Returns the receive buffer size.
	 * 
	 * NOTE: read at each lmtp session startup.
	 */
	public int getReceiveBufferSize() {
		return this.receiveBufferSize;
	}

	/**
	 * Sets the receive buffer size.
	 */
	public void setReceiveBufferSize(int receiveBufferSize) {
		this.receiveBufferSize = receiveBufferSize;
	}

	/**
	 * Get the maximum size in bytes of a single message before it is dumped to
	 * a temporary file.
	 * 
	 * NOTE: read when server starts.
	 */
	public int getDataDeferredSize() {
		return dataDeferredSize;
	}

	/**
	 * Set the maximum size in bytes of a single message before it is dumped to
	 * a temporary file. Argument must be a positive power of two in order to
	 * follow the expanding algorithm of
	 * {@link org.apache.mina.common.ByteBuffer} to prevent unnecessary memory
	 * consumption.
	 */
	public void setDataDeferredSize(int dataDeferredSize)
	{
		if (isPowerOfTwo(dataDeferredSize)) {
			this.dataDeferredSize = dataDeferredSize;
		} else {
			throw new IllegalArgumentException(
					"Argument dataDeferredSize must be a positive power of two");
		}
	}

	/**
	 * Demonstration : if x is a power of 2, it can't share any bit with x-1. So
	 * x & (x-1) should be equal to 0. To get rid of negative values, we check
	 * that x is higher than 1 (0 and 1 being of course unacceptable values for
	 * a buffer length).
	 * 
	 * @param x
	 *            the number to test
	 * @return true if x is a power of two
	 */
	private boolean isPowerOfTwo(int x) {
		return (x > 1) && (x & (x - 1)) == 0;
	}

	/**
	 * Returns the backlog.
	 * 
	 * NOTE: read when server starts.
	 */
	public int getBacklog() {
		return this.backlog;
	}

	/**
	 * This is the socket backlog.
	 * 
	 * The backlog argument must be a positive value greater than 0. If the
	 * value passed if equal or less than 0, then the default value will be
	 * assumed.
	 */
	public void setBacklog(int backlog) {
		this.backlog = backlog;
	}

	/**
	 * Returns the maximum number of recipients for a single message.
	 * 
	 * NOTE: read each time a RCPT command is issued.
	 */
	public int getMaxRecipients() {
		return this.maxRecipients;
	}

	/**
	 * Set the maximum number of recipients for a single message. If set to -1
	 * then limit is ignored.
	 */
	public void setMaxRecipients(int maxRecipients) {
		this.maxRecipients = maxRecipients;
	}

	/**
	 * The name of the server software.
	 */
	public String getName() {
		return NAME;
	}

	/**
	 * The name + version of the server software.
	 */
	public String getNameVersion() {
		return NAME + " " + VERSION;
	}

	/**
	 * Returs the host name. If set to null, it will return the 'localhost'
	 * string.
	 * 
	 * @return the host name that will be reported to LMTP clients.
	 */
	public String getHostName()
	{
		if (this.hostName == null) {
			return "localhost";
		} else {
			return this.hostName;
		}
	}

	/**
	 * The host name that will be reported to LMTP clients
	 */
	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	/**
	 * Returns the maximum number of connections.
	 * 
	 * NOTE: read each time a new connection happens.
	 */
	public int getMaxConnections() {
		return this.maxConnections;
	}

	/**
	 * Set's the maximum number of connections this server instance will accept.
	 * If set to -1 then limit is ignored.
	 * 
	 * @param maxConnections
	 */
	public void setMaxConnections(int maxConnections) {
		this.maxConnections = maxConnections;
	}

	/**
	 * Returns the connection timeout.
	 * 
	 * NOTE: read to configure each new session configuration.
	 */
	public int getConnectionTimeout() {
		return this.connectionTimeout;
	}

	/**
	 * Set the connection timeout.
	 */
	public void setConnectionTimeout(int connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	/**
	 * Returns the charset in use.
	 * 
	 * NOTE: read when server starts.
	 */
	public Charset getCharset() {
		return charset;
	}

	/**
	 * Sets the charset to use.
	 */
	public void setCharset(Charset charset) {
		this.charset = charset;
	}
}