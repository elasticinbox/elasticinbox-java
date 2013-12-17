package com.elasticinbox.common.utils;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class JSONUtilsTest
{
	@Test
	public void testNullValueSerialization()
	{
		Map<String, String> testMap = new HashMap<String, String>();
		testMap.put("key1", "val1");
		testMap.put("key2", null);
		
		byte[] bytes = JSONUtils.fromObject(testMap);
		String result = new String(bytes);
		
		assertFalse(result.contains("null"));
	}

}
