/**
 * Copyright (c) 2011-2012 Optimax Software Ltd.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *  * Neither the name of Optimax Software, ElasticInbox, nor the names
 *    of its contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
