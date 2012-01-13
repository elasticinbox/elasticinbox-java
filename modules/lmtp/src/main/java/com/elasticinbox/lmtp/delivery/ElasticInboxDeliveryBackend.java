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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elasticinbox.lmtp.server.api.Blob;
import com.elasticinbox.lmtp.server.api.LMTPAddress;
import com.elasticinbox.lmtp.server.api.LMTPEnvelope;
import com.elasticinbox.lmtp.server.api.LMTPReply;
import com.elasticinbox.core.account.validator.IValidator;
import com.elasticinbox.core.account.validator.IValidator.AccountStatus;
import com.elasticinbox.core.model.Mailbox;

/**
 * Delivery backend implementation
 * 
 * @author Rustam Aliyev
 */
public class ElasticInboxDeliveryBackend implements IDeliveryBackend
{
	private static final Logger logger = LoggerFactory
					.getLogger(ElasticInboxDeliveryBackend.class);

	private List<IValidator> validator;
	private List<IDeliveryAgent> agents;

	public ElasticInboxDeliveryBackend(List<IValidator> validator,
			List<IDeliveryAgent> agents)
	{
		this.validator = validator;
		this.agents = agents;
	}

	@Override
	public void deliver(LMTPEnvelope env, Blob blob)
	{
		logger.debug("deliver(" + env + ", blob: " + blob);

		for (IDeliveryAgent agent : agents) {
			try {
    			agent.deliver(env, blob);
	        } catch (Exception e) {
	            logger.warn(agent.getClass().getName() + 
	            		" delivery deferred: mail delivery failed: ", e);
            	setDeliveryStatuses(env.getRecipients(), LMTPReply.TEMPORARY_FAILURE);
	        }
		}
	}

	@Override
	public LMTPReply getAddressStatus(LMTPAddress address)
	{
		LMTPReply reply = LMTPReply.NO_SUCH_USER;

		for (IValidator val : validator) {
			 AccountStatus tmp = val.getAccountStatus(new Mailbox(address.getEmailAddress()));

			if (tmp == AccountStatus.ACTIVE) {
				reply = LMTPReply.RECIPIENT_OK;
				break;
			}
		}

		return reply;
	}

	private void setDeliveryStatuses(List<LMTPAddress> recipients, LMTPReply reply)
	{
        for (LMTPAddress recipient : recipients) {
            recipient.setDeliveryStatus(reply);
        }
    }

}