package com.elasticinbox.core.cassandra.persistence;

import static org.junit.Assert.*;

import org.junit.Test;

public class LabelIndexPersistenceTest {

	@Test
	public void testGetLabelKeyStringInt() {
		String key = LabelIndexPersistence.getLabelKey("test@elasticinbox.com", 123);
		assertEquals("test@elasticinbox.com:123", key);
	}

	@Test
	public void testGetLabelKeyStringString() {
		String key = LabelIndexPersistence.getLabelKey("test@elasticinbox.com", "special");
		assertEquals("test@elasticinbox.com:special", key);
	}

}
