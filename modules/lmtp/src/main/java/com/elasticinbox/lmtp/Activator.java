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

package com.elasticinbox.lmtp;

import java.util.LinkedList;
import java.util.List;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ecyrd.speed4j.StopWatch;
import com.ecyrd.speed4j.StopWatchFactory;
import com.ecyrd.speed4j.log.Slf4jLog;
import com.elasticinbox.config.Configurator;
import com.elasticinbox.lmtp.delivery.IDeliveryAgent;
import com.elasticinbox.lmtp.delivery.DeliveryAgentFactory;
import com.elasticinbox.lmtp.delivery.ElasticInboxDeliveryBackend;
import com.elasticinbox.lmtp.utils.LoggingPeriodicalLog;
import com.elasticinbox.lmtp.validator.IValidator;
import com.elasticinbox.lmtp.validator.ValidatorFactory;

public class Activator implements BundleActivator
{
	private static final Logger logger = 
					LoggerFactory.getLogger(Activator.class);

	private static final String SPEED4J_LOG_NAME = "ElasticInbox-LMTP"; 
	private StopWatchFactory stopWatchFactory;
	private ElasticInboxDeliveryBackend backend;
	private LMTPProxyServer server;

	// The shared instance
	private static Activator plugin;
	private static BundleContext bundleContext;

	public void start(BundleContext context) throws Exception
	{
		plugin = this;
		bundleContext = context;

		// Setup performance logger for LMTP
		if(Configurator.isPerformanceCountersEnabled()) {
			LoggingPeriodicalLog pLog = new LoggingPeriodicalLog();
			pLog.setName(SPEED4J_LOG_NAME);
			pLog.setPeriod(Configurator.getPerformanceCountersInterval());
			pLog.setJmx("DELIVERY.success,DELIVERY.discard,DELIVERY.defer,DELIVERY.defer_failure,DELIVERY.reject_overQuota,DELIVERY.reject_nonExistent");
			pLog.setSlf4jLogname("com.elasticinbox.speed4j.lmtp.PeriodicalLogger");
			stopWatchFactory = StopWatchFactory.getInstance(pLog);
		} else {
			Slf4jLog pLog = new Slf4jLog();
			pLog.setName(SPEED4J_LOG_NAME);
			pLog.setSlf4jLogname("com.elasticinbox.speed4j.lmtp.PeriodicalLogger");
			stopWatchFactory = StopWatchFactory.getInstance(pLog);
		}

		List<IValidator> validators = new LinkedList<IValidator>();
		List<IDeliveryAgent> agents = new LinkedList<IDeliveryAgent>();

		ValidatorFactory vf = new ValidatorFactory();
		validators.add(vf.getValidator());

		DeliveryAgentFactory mdf = new DeliveryAgentFactory();
		agents.add(mdf.getDeliveryAgent());

		backend = new ElasticInboxDeliveryBackend(validators, agents);

		logger.debug("Starting LMTP daemon...");
		server = new LMTPProxyServer();
		server.start();
		logger.info("LMTP daemon started.");
	}

	public void stop(BundleContext context) throws Exception
	{
		logger.debug("Stopping LMTP daemon...");
		server.stop();
		server = null;
		//StopWatchFactory.getInstance(SPEED4J_LOG_NAME).shutdown();
		logger.info("LMTP daemon stopped.");
	}

	public static Activator getDefault() {
		return plugin;
	}

	public ElasticInboxDeliveryBackend getBackend() {
		return backend;
	}

	public static BundleContext getContext() {
		return bundleContext;
	}
	
	public StopWatch getStopWatch() {
		return stopWatchFactory.getStopWatch();
	}
}
