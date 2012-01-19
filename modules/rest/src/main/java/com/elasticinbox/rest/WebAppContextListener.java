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
