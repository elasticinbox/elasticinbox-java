package com.elasticinbox.lmtp.server.api.handler;

import org.apache.james.protocols.smtp.MailAddress;
import org.apache.james.protocols.smtp.SMTPSession;
import org.apache.james.protocols.smtp.core.fastfail.AbstractValidRcptHandler;

import com.elasticinbox.core.account.validator.IValidator;
import com.elasticinbox.core.account.validator.IValidator.AccountStatus;
import com.elasticinbox.core.account.validator.ValidatorFactory;

public class ValidRcptHandler extends AbstractValidRcptHandler
{
	private IValidator validator = ValidatorFactory.getValidator();

	@Override
	protected boolean isValidRecipient(SMTPSession session, MailAddress recipient)
	{
		AccountStatus status = validator.getAccountStatus(recipient.toString());
		session.getLogger().debug("Validated account (" + recipient +  ") status is " + status);

		return status.equals(AccountStatus.ACTIVE) ? true : false;
	}

	@Override
	protected boolean isLocalDomain(SMTPSession session, String domain)
	{
		// ignore domain check
		return true;
	}

}
