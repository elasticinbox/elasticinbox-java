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

package com.elasticinbox.core.account.validator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elasticinbox.core.model.Mailbox;

/**
 * Dummy validator which will always return ACTIVE status 
 * 
 * @author Rustam Aliyev
 */
final class DummyValidator implements IValidator
{
	private static final Logger logger = LoggerFactory
			.getLogger(DummyValidator.class);

	@Override
	public AccountStatus getAccountStatus(Mailbox mailbox)
	{
		logger.debug("Validating " + mailbox.getId());
		return AccountStatus.ACTIVE;
	}

}