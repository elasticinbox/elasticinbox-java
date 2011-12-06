package com.elasticinbox.lmtp.validator;

import com.elasticinbox.lmtp.server.api.LMTPAddress;
import com.elasticinbox.lmtp.server.api.LMTPReply;

/**
 * Address validator interface
 * 
 * @author Rustam Aliyev
 */
public interface IValidator
{

	/**
	 * Gets account status.
	 */
	public LMTPReply getAddressStatus(LMTPAddress address);

}