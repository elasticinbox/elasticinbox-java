package com.elasticinbox.core.account.validator;

/**
 * Creates account validator
 * 
 * @author Rustam Aliyev
 */
public class ValidatorFactory
{
	private ValidatorFactory() {
		// ensure non-instantiability
	}

	public static IValidator getValidator() {
		return new DummyValidator();
	}
}