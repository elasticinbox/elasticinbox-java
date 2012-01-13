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