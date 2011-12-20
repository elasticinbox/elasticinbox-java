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

package com.elasticinbox.lmtp.delivery;

import java.util.HashMap;
import java.util.Map;

import org.apache.james.protocols.smtp.MailEnvelope;
import org.apache.james.protocols.smtp.MailAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elasticinbox.lmtp.server.api.DeliveryReturnCode;

/**
 * Delivery backend implementation
 * 
 * @author Rustam Aliyev
 */
public class MulticastDeliveryAgent implements IDeliveryAgent
{
	private static final Logger logger = LoggerFactory
					.getLogger(MulticastDeliveryAgent.class);

	private IDeliveryAgent[] agents;

	public MulticastDeliveryAgent(IDeliveryAgent... agents)
	{
		this.agents = agents;
	}

	@Override
	public Map<MailAddress, DeliveryReturnCode> deliver(MailEnvelope env)
	{
		logger.debug("deliver(" + env +")");

        Map<MailAddress, DeliveryReturnCode> map = new HashMap<MailAddress, DeliveryReturnCode>();
        
		for (IDeliveryAgent agent : agents) {
			try {
			    map.putAll(agent.deliver(env));
	        } catch (Exception e) {
	            logger.warn(agent.getClass().getName() + 
	            		" delivery deferred: mail delivery failed: ", e);
            	for (MailAddress address: env.getRecipients()){
            	     map.put(address,DeliveryReturnCode.TEMPORARY_FAILURE);
            	}
	        }
		}
		return map;
	}

}