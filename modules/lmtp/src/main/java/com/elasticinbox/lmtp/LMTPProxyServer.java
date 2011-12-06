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

import com.elasticinbox.config.Configurator;
import com.elasticinbox.lmtp.server.LMTPServer;
import com.elasticinbox.lmtp.server.api.handler.ElasticInboxDeliveryHandler;

/**
 * LMTP proxy main class which sends traffic to multiple registered handlers
 * 
 * @author Rustam Aliyev
 */
public class LMTPProxyServer
{
	private LMTPServer server;

	protected LMTPProxyServer() {
	}

	public void start() {
		server = new LMTPServer(Activator.getDefault().getBackend());

		server.getDeliveryHandlerFactory().setDeliveryHandlerImplClass(
				ElasticInboxDeliveryHandler.class);

		server.setPort(Configurator.getLmtpPort());
		server.getConfig().setMaxConnections(Configurator.getLmtpMaxConnections());
		// flush to tmp file if data > 32K
		server.getConfig().setDataDeferredSize(32 * 1024);

		server.start();
	}

	public void stop() {
		server.stop();
	}

}
