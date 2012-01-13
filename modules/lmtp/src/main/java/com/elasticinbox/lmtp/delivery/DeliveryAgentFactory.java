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

import com.elasticinbox.core.DAOFactory;
import com.elasticinbox.core.MessageDAO;

/**
 * This factory creates delivery agents consuming emails from LMTP and storing
 * them.
 * 
 * @author Rustam Aliyev
 */
public class DeliveryAgentFactory
{
	private final MessageDAO messageDAO;

	public DeliveryAgentFactory()
	{
		DAOFactory dao = DAOFactory.getDAOFactory();
		messageDAO = dao.getMessageDAO();
	}

	public IDeliveryAgent getDeliveryAgent() {
		return new ElasticInboxDeliveryAgent(messageDAO);
	}

}
