package com.elasticinbox.core.model;

public class LabelConstants
{
	/** Maximum label count per mailbox (including reserved labels) */
	public final static int MAX_LABEL_ID = 9999;

	/** Maximum label name length */
	public final static int MAX_LABEL_NAME_LENGTH = 250;

	/** Character used as nested label separator */
	public final static Character NESTED_LABEL_SEPARATOR = '^';

	/**
	 * Maximum reserved label ID. I.e. reserved label ID can be between
	 * 0..MAX_RESERVED_LABEL_ID
	 */
	public final static int MAX_RESERVED_LABEL_ID = 20;
}
