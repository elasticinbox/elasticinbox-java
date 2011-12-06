package com.elasticinbox.lmtp.validator;


/**
 * Creates email address validators
 * 
 * @author Rustam Aliyev
 */
public class ValidatorFactory
{
	public ValidatorFactory() {
	}

	public IValidator getValidator() {
		return new DummyValidator();
	}

}