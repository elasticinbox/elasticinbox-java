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

import java.net.InetSocketAddress;

import org.apache.james.protocols.api.logger.Logger;
import org.apache.james.protocols.pop3.POP3Protocol;
import org.apache.james.protocols.pop3.POP3ProtocolHandlerChain;
import org.apache.james.protocols.netty.NettyServer;

import com.elasticinbox.config.Configurator;
import com.elasticinbox.pop3.server.POP3ServerConfig;
import com.elasticinbox.pop3.server.handler.AuthHandler;
import com.elasticinbox.pop3.server.handler.MailboxHandlerFactory;
import com.elasticinbox.pop3.utils.POP3ProtocolLogger;

/**
 * POP3 proxy main class
 * 
 * @author Rustam Aliyev
 */
public class POP3ProxyServer
{
	private NettyServer server;
	private MailboxHandlerFactory backend;

	protected POP3ProxyServer(final MailboxHandlerFactory backend) {
		this.backend = backend;
	}

	public void start() throws Exception
	{
		Logger logger = new POP3ProtocolLogger();

//		POP3ProtocolHandlerChain chain = new POP3ProtocolHandlerChain();
//		chain.add(0, new PassCmdHandler(backend));
//		chain.wireExtensibleHandlers();

		POP3ProtocolHandlerChain chain = new POP3ProtocolHandlerChain(new AuthHandler(backend));

		server = new NettyServer(new POP3Protocol(chain, new POP3ServerConfig(), logger));
		server.setListenAddresses(new InetSocketAddress(Configurator.getPop3Port()));
		server.setMaxConcurrentConnections(Configurator.getPop3MaxConnections());
		server.setTimeout(POP3ServerConfig.CONNECTION_TIMEOUT);
		server.setUseExecutionHandler(true, 16);
		server.bind();
	}

	public void stop() {
		server.unbind();
	}
}
