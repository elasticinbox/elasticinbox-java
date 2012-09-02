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

package com.elasticinbox.pop3;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ecyrd.speed4j.StopWatch;
import com.ecyrd.speed4j.StopWatchFactory;
import com.ecyrd.speed4j.log.PeriodicalLog;
import com.ecyrd.speed4j.log.Slf4jLog;
import com.elasticinbox.config.Configurator;
import com.elasticinbox.pop3.server.handler.MailboxHandlerFactory;
import com.elasticinbox.pop3.utils.LoggingPeriodicalLog;

public class Activator implements BundleActivator
{
	private static final Logger logger = 
					LoggerFactory.getLogger(Activator.class);

	private static final String SPEED4J_LOG_NAME = "ElasticInbox-POP3"; 
	private StopWatchFactory stopWatchFactory;
	private MailboxHandlerFactory backend;
	private POP3ProxyServer server;

	// The shared instance
	private static Activator plugin;
	private static BundleContext bundleContext;

	public void start(BundleContext context) throws Exception
	{
		plugin = this;
		bundleContext = context;

		// Setup performance logger for POP3
		if(Configurator.isPerformanceCountersEnabled()) {
			LoggingPeriodicalLog pLog = new LoggingPeriodicalLog();
			pLog.setName(SPEED4J_LOG_NAME);
			pLog.setMode(PeriodicalLog.Mode.JMX_ONLY);
			pLog.setMaxQueueSize(250000);
			pLog.setPeriod(Configurator.getPerformanceCountersInterval());
			pLog.setJmx("DELIVERY.success,DELIVERY.discard,DELIVERY.defer,DELIVERY.defer_failure,DELIVERY.reject_overQuota,DELIVERY.reject_nonExistent");
			pLog.setSlf4jLogname("com.elasticinbox.speed4j.pop3.PeriodicalLogger");
			stopWatchFactory = StopWatchFactory.getInstance(pLog);
		} else {
			Slf4jLog pLog = new Slf4jLog();
			pLog.setName(SPEED4J_LOG_NAME);
			pLog.setSlf4jLogname("com.elasticinbox.speed4j.pop3.PeriodicalLogger");
			stopWatchFactory = StopWatchFactory.getInstance(pLog);
		}

		backend = new MailboxHandlerFactory();

		logger.debug("Starting POP3 daemon...");
		server = new POP3ProxyServer(backend);
		server.start();
		logger.info("POP3 daemon started.");
	}

	public void stop(BundleContext context) throws Exception
	{
		logger.debug("Stopping POP3 daemon...");
		server.stop();
		server = null;
		//StopWatchFactory.getInstance(SPEED4J_LOG_NAME).shutdown();
		logger.info("POP3 daemon stopped.");
	}

	public static Activator getDefault() {
		return plugin;
	}

	public MailboxHandlerFactory getBackend() {
		return backend;
	}

	public static BundleContext getContext() {
		return bundleContext;
	}

	public StopWatch getStopWatch() {
		return stopWatchFactory.getStopWatch();
	}
}
