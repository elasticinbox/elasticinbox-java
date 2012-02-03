package com.elasticinbox.core;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;

import java.util.Date;
import java.util.UUID;

import me.prettyprint.cassandra.utils.TimeUUIDUtils;

import org.junit.Test;

import com.elasticinbox.core.message.id.MessageIdBuilder;

public class MessageIdPolicyTest
{
	@Test
	public void testSentDateMessageIdPolicy()
	{
		Date date = new Date();
		UUID uuid2;
		long ts2;

		for (int i = 0; i < 10000; i++) {
			uuid2 = new MessageIdBuilder().setSentDate(date).build();
			ts2 = TimeUUIDUtils.getTimeFromUUID(uuid2);
			assertThat(1000L, greaterThan(ts2 - date.getTime()));
		}
	}
	
	@Test
	public void testCurrentDateMessageIdPolicy()
	{
		UUID uuid;
		UUID prev = new MessageIdBuilder().build();
		long ts;

		for (int i = 0; i < 10000; i++) {
			uuid = new MessageIdBuilder().build();
			ts = TimeUUIDUtils.getTimeFromUUID(uuid);
			
			if(uuid.equals(prev))
				fail("Not unique, same as previous!");

			prev = uuid;
			
			assertThat(100L, greaterThan(System.currentTimeMillis() - ts));
		}
	}

}