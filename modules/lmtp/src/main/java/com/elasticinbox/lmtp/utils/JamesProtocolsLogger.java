package com.elasticinbox.lmtp.utils;

import org.apache.james.protocols.api.logger.Logger;
import org.slf4j.LoggerFactory;

public class JamesProtocolsLogger implements Logger
{
	private static final org.slf4j.Logger logger = 
			LoggerFactory.getLogger(JamesProtocolsLogger.class);

	@Override
	public boolean isDebugEnabled() {
		return logger.isDebugEnabled();
	}

	@Override
	public boolean isErrorEnabled() {
		return logger.isErrorEnabled();
	}

	@Override
	public boolean isInfoEnabled() {
		return logger.isInfoEnabled();
	}

	@Override
	public boolean isTraceEnabled() {
		return logger.isTraceEnabled();
	}

	@Override
	public boolean isWarnEnabled() {
		return logger.isWarnEnabled();
	}

	@Override
	public void trace(String message) {
		logger.trace(message);
	}

	@Override
	public void trace(String message, Throwable t) {
		logger.trace(message, t);
	}

	@Override
	public void debug(String message) {
		logger.debug(message);
	}

	@Override
	public void debug(String message, Throwable t) {
		logger.debug(message, t);
	}

	@Override
	public void info(String message) {
		logger.info(message);
	}

	@Override
	public void info(String message, Throwable t) {
		logger.info(message, t);
	}

	@Override
	public void warn(String message) {
		logger.warn(message);
	}

	@Override
	public void warn(String message, Throwable t) {
		logger.warn(message, t);
	}

	@Override
	public void error(String message) {
		logger.error(message);
	}

	@Override
	public void error(String message, Throwable t) {
		logger.error(message, t);
	}

}
