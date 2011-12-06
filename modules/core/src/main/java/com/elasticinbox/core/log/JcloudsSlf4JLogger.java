/**
 * Copyright (c) 2011 Optimax Software Ltd
 * 
 * This file is part of ElasticInbox.
 * 
 * ElasticInbox is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 2 of the License, or (at your option) any later
 * version.
 * 
 * ElasticInbox is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * ElasticInbox. If not, see <http://www.gnu.org/licenses/>.
 */

package com.elasticinbox.core.log;

import org.jclouds.logging.BaseLogger;
import org.jclouds.logging.Logger;

/**
* Slf4j logging for Jclouds.
*
* @author Rustam Aliyev
*/
public final class JcloudsSlf4JLogger extends BaseLogger
{
	private final org.slf4j.Logger logger;
	private final String category;

	public static class JcloudsSlf4JLoggerFactory implements LoggerFactory
	{
		public Logger getLogger(String category) {
			return new JcloudsSlf4JLogger(category,
					org.slf4j.LoggerFactory.getLogger(category));
		}
	}

	public JcloudsSlf4JLogger(String category, org.slf4j.Logger logger) {
		this.category = category;
		this.logger = logger;
	}

	@Override
	protected void logTrace(String message) {
		logger.trace(message);
	}

	public boolean isTraceEnabled() {
		return logger.isTraceEnabled();
	}

	@Override
	protected void logDebug(String message) {
		logger.debug(message);
	}

	public boolean isDebugEnabled() {
		return logger.isDebugEnabled();
	}

	@Override
	protected void logInfo(String message) {
		logger.info(message);
	}

	public boolean isInfoEnabled() {
		return logger.isInfoEnabled();
	}

	@Override
	protected void logWarn(String message) {
		logger.warn(message);
	}

	@Override
	protected void logWarn(String message, Throwable e) {
		logger.warn(message, e);
	}

	public boolean isWarnEnabled() {
		return logger.isWarnEnabled();
	}

	@Override
	protected void logError(String message) {
		logger.error(message);
	}

	@Override
	protected void logError(String message, Throwable e) {
		logger.error(message, e);
	}

	public boolean isErrorEnabled() {
		return logger.isErrorEnabled();
	}

	public String getCategory() {
		return category;
	}
}
