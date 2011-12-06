package com.elasticinbox.lmtp.validator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elasticinbox.lmtp.server.api.LMTPAddress;
import com.elasticinbox.lmtp.server.api.LMTPReply;

/**
 * Checks that emails are valid for delivery
 * 
 * @author Rustam Aliyev
 */
public class DummyValidator implements IValidator
{
	private static final Logger logger = LoggerFactory
			.getLogger(DummyValidator.class);

	@Override
	public LMTPReply getAddressStatus(LMTPAddress address)
	{
		logger.debug("validating " + address);
		return LMTPReply.RECIPIENT_OK;
	}

}