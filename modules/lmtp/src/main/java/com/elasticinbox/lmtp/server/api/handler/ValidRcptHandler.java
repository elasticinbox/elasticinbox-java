package com.elasticinbox.lmtp.server.api.handler;

import org.apache.james.protocols.smtp.MailAddress;
import org.apache.james.protocols.smtp.SMTPSession;
import org.apache.james.protocols.smtp.core.fastfail.AbstractValidRcptHandler;

import com.elasticinbox.core.account.validator.IValidator;
import com.elasticinbox.core.account.validator.IValidator.AccountStatus;
import com.elasticinbox.core.account.validator.ValidatorFactory;
import com.elasticinbox.core.model.Mailbox;

public class ValidRcptHandler extends AbstractValidRcptHandler
{
	IValidator validator = ValidatorFactory.getValidator();

	@Override
	protected boolean isValidRecipient(SMTPSession session, MailAddress recipient)
	{
		Mailbox mailbox = new Mailbox(recipient.toString());
		AccountStatus status = validator.getAccountStatus(mailbox);
		session.getLogger().debug("Validated account (" + mailbox.getId() + 
				") status is " + status.toString());

		return status.equals(AccountStatus.ACTIVE) ? true : false;
	}

	@Override
	protected boolean isLocalDomain(SMTPSession session, String domain)
	{
		// ignore domain check
		return true;
	}

}
