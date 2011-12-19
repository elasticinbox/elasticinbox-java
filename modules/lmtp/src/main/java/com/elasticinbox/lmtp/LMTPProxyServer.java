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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import org.apache.james.protocols.lmtp.LMTPConfigurationImpl;
import org.apache.james.protocols.lmtp.LMTPProtocolHandlerChain;
import org.apache.james.protocols.netty.NettyServer;
import org.apache.james.protocols.smtp.MailEnvelope;
import org.apache.james.protocols.smtp.SMTPProtocol;
import org.apache.mailet.MailAddress;

import com.elasticinbox.lmtp.delivery.IDeliveryAgent;
import com.elasticinbox.lmtp.server.api.DeliveryReturnCode;
import com.elasticinbox.lmtp.server.api.handler.ElasticInboxDeliveryHandler;

/**
 * LMTP proxy main class which sends traffic to multiple registered handlers
 * 
 * @author Rustam Aliyev
 */
public class LMTPProxyServer {
	private NettyServer server;
	private IDeliveryAgent backend;

	protected LMTPProxyServer(IDeliveryAgent backend) {
	    this.backend = backend;
	}

	public void start() throws Exception {
		LMTPProtocolHandlerChain chain = new LMTPProtocolHandlerChain();
		chain.add(0, new ElasticInboxDeliveryHandler(backend));
		chain.wireExtensibleHandlers();
		server = new NettyServer(new SMTPProtocol(chain,
				new LMTPConfigurationImpl()));

		server.setListenAddresses(new InetSocketAddress(2400));
		// server.set().setMaxConnections(Configurator.getLmtpMaxConnections());
		// flush to tmp file if data > 32K
		// server.getConfig().setDataDeferredSize(32 * 1024);
		server.setUseExecutionHandler(true, 16);
		server.bind();
	}

	public void stop() {
		server.unbind();
	}
	
	public static void main(String[] args) throws Exception {
	    new LMTPProxyServer(new IDeliveryAgent() {

            @Override
            public Map<MailAddress, DeliveryReturnCode> deliver(MailEnvelope env) throws IOException {
                Map<MailAddress, DeliveryReturnCode> map = new HashMap<MailAddress, DeliveryReturnCode>();
                for (MailAddress address: env.getRecipients()) {
                    map.put(address, DeliveryReturnCode.OK);
                }
                return map;
            }

            
        }).start();
	}
}
