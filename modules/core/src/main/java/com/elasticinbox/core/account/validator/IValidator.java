package com.elasticinbox.core.account.validator;

import com.elasticinbox.core.model.Mailbox;

public interface IValidator
{
	public enum AccountStatus {
		ACTIVE, BLOCKED, NOT_FOUND
	}

	public AccountStatus getAccountStatus(Mailbox mailbox);
}
