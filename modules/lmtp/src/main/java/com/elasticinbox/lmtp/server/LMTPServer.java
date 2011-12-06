package com.elasticinbox.lmtp.server;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.SocketAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elasticinbox.lmtp.delivery.IDeliveryBackend;
import com.elasticinbox.lmtp.server.api.handler.AbstractDeliveryHandler;
import com.elasticinbox.lmtp.server.command.CommandHandler;
import com.elasticinbox.lmtp.server.core.DeliveryHandlerFactory;
import com.elasticinbox.lmtp.server.core.mina.LMTPCodecDecoder;
import com.elasticinbox.lmtp.server.core.mina.LMTPCodecFactory;
import com.elasticinbox.lmtp.server.core.mina.LMTPConnectionHandler;

/**
 * Main LMTPServer class. Construct this object, set the hostName, port, and
 * bind address if you wish to override the defaults, and call start().
 * 
 * This class starts opens a <a href="http://mina.apache.org/">Mina</a> based
 * listener and creates a new instance of the LMTPConnectionHandler class when a
 * new connection comes in. The LMTPConnectionHandler then parses the incoming
 * LMTP stream and hands off the processing to the CommandHandler which will
 * execute the appropriate LMTP command class.
 * 
 * Using this server is easy just use the constructor passing in
 * {@link IDeliveryBackend}. This is a higher, and sometimes more convenient
 * level of abstraction. You can further manipulate the list of listeners using
 * the methods provided by the {@link DeliveryHandlerFactory}.
 * 
 * You can also customize the way that messages are delivered to the backend
 * by writing your own delivery handler implementation by extending
 * {@link AbstractDeliveryHandler} and registering it with the server using the
 * following code line :
 * 
 * server.getDeliveryHandlerFactory(). setDeliveryHandlerImplClass(Class<?
 * extends AbstractDeliveryHandler> impl);
 * 
 * In neither case is the LMTP server (this library) responsible for deciding
 * what recipients to accept or what to do with the incoming data. That is left
 * to you.
 * 
 * @author De Oliveira Edouard &lt;doe_wanted@yahoo.fr&gt;
 * @author Rustam Aliyev
 * 
 * NB: some code comes from a fork from the SubethaSMTP project.
 */
public class LMTPServer
{
	private static final Logger logger = LoggerFactory
			.getLogger(LMTPServer.class);

	// IoService JMX name.
	//private static final String IO_SERVICE_MBEAN_NAME = "elasticinbox.mina.server:type=IoServiceMBean";

	// default to all interfaces
	private InetAddress bindAddress = null;

	// default to 24
	private int port = 24;
	
	private DeliveryHandlerFactory deliveryHandlerFactory;
	private CommandHandler commandHandler;
	private LMTPConnectionHandler handler;
	
	private SocketAcceptor acceptor;
	private ExecutorService executor;
	private LMTPCodecFactory codecFactory;
	
	private boolean running = false;
	private boolean shutdowned = false;

	/**
	 * The server configuration.
	 */
	private LMTPServerConfig config = new LMTPServerConfig();

	public LMTPServer(IDeliveryBackend backend) {
		this.deliveryHandlerFactory = new DeliveryHandlerFactory(backend);
		this.commandHandler = new CommandHandler();
		initService();		
	}

	/**
	 * Start the JMX service.
	 * @throws NullPointerException 
	 * @throws MalformedObjectNameException 
	 * @throws NotCompliantMBeanException 
	 * @throws MBeanRegistrationException 
	 * @throws InstanceAlreadyExistsException 
	 */
	/*public void startJMXService(IoService svc) 
		throws InstanceAlreadyExistsException, MBeanRegistrationException, 
				NotCompliantMBeanException, MalformedObjectNameException, NullPointerException
	{
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		mbs.registerMBean(new IoServiceMBean(svc), new ObjectName(IO_SERVICE_MBEAN_NAME));
	}*/

	/**
	 * Stop the JMX service.
	 * @throws NullPointerException 
	 * @throws MalformedObjectNameException 
	 * @throws MBeanRegistrationException 
	 * @throws InstanceNotFoundException 
	 * 
	 * @throws InstanceNotFoundException
	 * @throws MBeanRegistrationException
	 */
	/*public void stopJMXService() 
		throws InstanceNotFoundException, MBeanRegistrationException, 
				MalformedObjectNameException, NullPointerException
	{
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		mbs.unregisterMBean(new ObjectName(IO_SERVICE_MBEAN_NAME));
	}*/

	/**
	 * Initializes the runtime service.
	 */
	private void initService()
	{
		try {
			// allocate heap buffers by default
			IoBuffer.setUseDirectBuffer(false);

			codecFactory = new LMTPCodecFactory(config);

			acceptor = new NioSocketAcceptor(Runtime.getRuntime().availableProcessors());
			acceptor.setReuseAddress(true);
			acceptor.getFilterChain().addLast("lmtp_codec",
							new ProtocolCodecFilter(codecFactory));

			handler = new LMTPConnectionHandler(getConfig(),
							getCommandHandler(), getDeliveryHandlerFactory());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Call this method to get things rolling after instantiating the
	 * LMTPServer.
	 */
	public synchronized void start()
	{
		if (running) {
			logger.info("LMTP server is already started.");
			return;
		}

		if (shutdowned) {
			throw new RuntimeException("Error: server has been shutdown previously");
		}

		// Read LMTP decoder configuration options
		((LMTPCodecDecoder) codecFactory.getDecoder(null)).setup(
				getConfig().getCharset(), getConfig().getDataDeferredSize());

		InetSocketAddress isa;

		if (this.bindAddress == null) {
			isa = new InetSocketAddress(this.port);
		} else {
			isa = new InetSocketAddress(this.bindAddress, this.port);
		}

		//acceptor.setBacklog(config.getBacklog());
		acceptor.setHandler(handler);

		try {
			acceptor.bind(isa);
			running = true;
			logger.info("LMTP server started ...");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Stops the server by unbinding server socket. To really clean
	 * things out, one must call {@link #shutdown()}.
	 */
	public synchronized void stop()
	{
		try {
			try {
				acceptor.unbind();
			} catch (Exception e) {
				logger.error("Unable to stop LMTP server: ", e);
			}

			logger.info("LMTP server stopped.");
		} finally {
			running = false;
		}
	}
	
	/**
	 * Shut things down gracefully. Please pay attention to the fact 
	 * that a shutdown implies that the server would fail to restart 
	 * because some internal resources have been freed.
	 * 
	 * You can directly call shutdown() if you do not intend to restart 
	 * it later. Calling start() after shutdown() will throw a 
	 * {@link RuntimeException}.
	 */
	public synchronized void shutdown()
	{
		try {
			logger.info("LMTP server shutting down...");
			if (isRunning()) {
				stop();
			}

			try {
				executor.shutdown();
			} catch (Exception e) {
				logger.error("Unable to shutdown LMTP server: ", e);
			}

			shutdowned = true;
			logger.info("LMTP server shutdown complete.");
		} finally {
			running = false;
		}
	}

	/**
	 * Is the server running after start() has been called?
	 */
	public synchronized boolean isRunning() {
		return this.running;
	}

	/**
	 * Returns the bind address. Null means all interfaces.
	 */
	public InetAddress getBindAddress() {
		return this.bindAddress;
	}

	/**
	 * Sets the bind address. Null means all interfaces.
	 */
	public void setBindAddress(InetAddress bindAddress) {
		this.bindAddress = bindAddress;
	}

	/**
	 * Returns the port the server is running on.
	 */
	public int getPort() {
		return this.port;
	}

	/**
	 * Sets the port the server will run on.
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * All LMTP data is routed through the handler.
	 */
	public DeliveryHandlerFactory getDeliveryHandlerFactory() {
		return this.deliveryHandlerFactory;
	}

	/**
	 * The CommandHandler manages handling the LMTP commands such as QUIT, MAIL,
	 * RCPT, DATA, etc.
	 * 
	 * @return An instance of CommandHandler
	 */
	public CommandHandler getCommandHandler() {
		return this.commandHandler;
	}

	/**
	 * Returns the server configuration.
	 */
	public LMTPServerConfig getConfig() {
		return config;
	}
}