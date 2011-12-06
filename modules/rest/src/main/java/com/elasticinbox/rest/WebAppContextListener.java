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

package com.elasticinbox.rest;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebAppContextListener implements BundleActivator,
		ServletContextListener
{
	private static EventAdmin ea;
	private BundleContext bc;
	private ServiceReference eaRef;

	private final static Logger logger = LoggerFactory
			.getLogger(WebAppContextListener.class);

	synchronized static EventAdmin getEventAdmin() {
		return ea;
	}

	synchronized static void setEventAdmin(EventAdmin ea) {
		WebAppContextListener.ea = ea;
	}

	@Override
	public void contextInitialized(final ServletContextEvent sce)
	{
		if (getEventAdmin() != null) {
			Dictionary<String, String> properties = new Hashtable<String,String>();
	        properties.put("context-path" , sce.getServletContext().getContextPath());

			getEventAdmin().sendEvent(new Event("com/elasticinbox/rest/DEPLOYED", properties));
			logger.info("REST API Deployed");
		}
	}

	@Override
	public void contextDestroyed(final ServletContextEvent sce)
	{
		if (getEventAdmin() != null) {
			Dictionary<String, String> properties = new Hashtable<String,String>();
	        properties.put("context-path" , sce.getServletContext().getContextPath());
			getEventAdmin().sendEvent(
					new Event("com/elasticinbox/rest/UNDEPLOYED", properties));
			logger.info("REST API Undeployed");
		}
	}

	@Override
	public void start(BundleContext context) throws Exception
	{
		bc = context;
		eaRef = bc.getServiceReference(EventAdmin.class.getName());
		if (eaRef != null) {
			setEventAdmin((EventAdmin) bc.getService(eaRef));
		}
	}

	@Override
	public void stop(BundleContext context) throws Exception
	{
		if (eaRef != null) {
			setEventAdmin(null);
			bc.ungetService(eaRef);
		}
	}
}
