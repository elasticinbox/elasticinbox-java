package com.elasticinbox.lmtp.server.core.mina;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.mina.core.buffer.BufferDataException;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.SocketSessionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elasticinbox.lmtp.server.LMTPServerConfig;
import com.elasticinbox.lmtp.server.api.LMTPReply;
import com.elasticinbox.lmtp.server.api.TooMuchDataException;
import com.elasticinbox.lmtp.server.command.CommandException;
import com.elasticinbox.lmtp.server.command.CommandHandler;
import com.elasticinbox.lmtp.server.core.DeliveryHandlerFactory;

/**
 * The IoHandler that handles a connection. This class
 * passes most of it's responsibilities off to the
 * CommandHandler.
 * 
 * @author De Oliveira Edouard &lt;doe_wanted@yahoo.fr&gt;
 * @author Rustam Aliyev
 */
public class LMTPConnectionHandler extends IoHandlerAdapter
{
	private static Logger logger = LoggerFactory
			.getLogger(LMTPConnectionHandler.class);

	// Session objects
	protected final static String CONTEXT_ATTRIBUTE = LMTPConnectionHandler.class
			.getName() + ".ctx";

	private LMTPServerConfig config;
	private CommandHandler commandHandler;
	private DeliveryHandlerFactory factory;

	/**
	 * A thread safe variable that represents the number of active connections.
	 */
	private AtomicInteger numberOfConnections = new AtomicInteger(0);

	public LMTPConnectionHandler(LMTPServerConfig cfg, CommandHandler handler,
			DeliveryHandlerFactory factory)
	{
		this.config = cfg;
		this.commandHandler = handler;
		this.factory = factory;
	}

	/**
	 * Are we over the maximum amount of connections ?
	 */
	private boolean hasTooManyConnections()
	{
		return (config.getMaxConnections() > -1 && getNumberOfConnections() >= config
				.getMaxConnections());
	}

	/**
	 * Update the number of active connections.
	 * 
	 * @param delta
	 */
	private void updateNumberOfConnections(int delta)
	{
		int count = numberOfConnections.addAndGet(delta);
		logger.info("Active connections count {} of {}", count,
				config.getMaxConnections());
	}

	/**
	 * @return The number of open connections
	 */
	public int getNumberOfConnections() {
		return numberOfConnections.get();
	}

	/** */
	public void sessionCreated(IoSession session)
	{
		updateNumberOfConnections(+1);

		if (session.getTransportMetadata().getSessionConfigType() == SocketSessionConfig.class) {
			if(config.getReceiveBufferSize() > 0) {
				((SocketSessionConfig) session.getConfig())
						.setReceiveBufferSize(config.getReceiveBufferSize());
				((SocketSessionConfig) session.getConfig()).setSendBufferSize(64);
			}
		}

		session.getConfig().setIdleTime(IdleStatus.READER_IDLE,
				config.getConnectionTimeout() / 1000);

		// Init protocol internals
		LMTPContext minaCtx = new LMTPContext(config, factory, session);
		session.setAttribute(CONTEXT_ATTRIBUTE, minaCtx);

		try {
			if (hasTooManyConnections()) {
				logger.debug("Too many connections");
				sendResponse(session, LMTPReply.TOO_MANY_CONNECTIONS);
			} else {
				sendResponse(session, "220 " + config.getHostName() + " LMTP " + config.getName());
			}
		} catch (IOException e1) {
			try {
				sendResponse(session, LMTPReply.TEMPORARY_CONNECTION_FAILURE);
			} catch (IOException e) {
				// ignore
			}

			logger.debug("Error on session creation: ", e1);
			session.close(false);
		}
	}

	/**
	 * Session closed.
	 */
	public void sessionClosed(IoSession session) throws Exception {
		updateNumberOfConnections(-1);
	}

	/**
	 * Sends a response telling that the session is idle and closes it.
	 */
	public void sessionIdle(IoSession session, IdleStatus status)
	{
		try {
			sendResponse(session, LMTPReply.TIMEOUT);
		} catch (IOException ioex) {
			// ignore?
		} finally {
			session.close(false);
		}
	}

	/** */
	public void exceptionCaught(IoSession session, Throwable cause)
	{
		logger.debug("Exception occured: ", cause);
		boolean fatal = true;

		try {
			if (cause instanceof BufferDataException) {
				sendResponse(session, "501 " + cause.getMessage());
			} else if (cause instanceof CommandException) {
				fatal = false;
				sendResponse(session, LMTPReply.SYNTAX_ERROR);
			} else if (cause instanceof IOException) {
				// ignore, trying to send response or close session may cause
				// an infinite loop
				fatal = false;
			} else {
				// primarily if things fail during the
				// MessageListener.deliver(), then try to send a temporary
				// failure back so that the server will try to resend the
				// message later.
				sendResponse(session, LMTPReply.TEMPORARY_FAILURE);
			}
		} catch (IOException e) {
			// ignore?
		} finally {
			if (fatal) {
				session.close(false);
			}
		}
	}

	/** */
	public void messageReceived(IoSession session, Object message)
			throws Exception
	{
		if (message == null) {
			logger.debug("no more lines from client");
			return;
		}

		LMTPContext minaCtx = (LMTPContext) session.getAttribute(CONTEXT_ATTRIBUTE);

		if (message instanceof InputStream) {
			minaCtx.setInputStream((InputStream) message);

			try {
				this.commandHandler.handleCommand("DATA_END", session, minaCtx);
			} catch (TooMuchDataException tmde) {
				sendResponse(session, LMTPReply.PERMANENT_FAILURE_TOO_MUCH_DATA);
			}
		} else {
			String line = (String) message;
			logger.debug("LMTP C: {}", line);
			this.commandHandler.handleCommand(line, session, minaCtx);
		}
	}

	/** */
	public static void sendResponse(IoSession session, String response)
	{
		logger.debug("LMTP S: {}", response);

		if (response != null) {
			session.write(response);
		}
		
		LMTPContext minaCtx = (LMTPContext) session.getAttribute(CONTEXT_ATTRIBUTE);

		if (!minaCtx.getSession().isActive()) {
			session.close(false);
		}
	}

	public static void sendResponse(IoSession session, LMTPReply response)
			throws IOException
	{
		sendResponse(session, response.toString());
	}
}