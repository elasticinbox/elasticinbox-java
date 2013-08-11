package com.elasticinbox.core.model;

import static org.junit.Assert.*;

import org.junit.Test;

public class LabelCountersTest
{
	@Test
	public void testEqual()
	{
		LabelCounters c1 = new LabelCounters();
		LabelCounters c2 = new LabelCounters();
		assertEquals(c1, c2);
		
		c1.setTotalBytes(123L);
		c1.setTotalMessages(23L);
		c1.setUnreadMessages(335L);
		
		c2.setTotalBytes(123L);
		c2.setTotalMessages(23L);
		c2.setUnreadMessages(335L);

		assertEquals(c1, c2);
	}
	
	@Test
	public void testInverse()
	{
		LabelCounters c1 = new LabelCounters();
		LabelCounters c2 = new LabelCounters();

		c1.setTotalBytes(123L);
		c1.setTotalMessages(23L);
		c1.setUnreadMessages(335L);
		
		c2.setTotalBytes(-123L);
		c2.setTotalMessages(-23L);
		c2.setUnreadMessages(-335L);

		assertEquals(c1.getInverse(), c2);
	}
	
	@Test
	public void testAdd()
	{
		LabelCounters c1 = new LabelCounters();
		LabelCounters c2 = new LabelCounters();
		LabelCounters c3 = new LabelCounters();

		c1.setTotalBytes(123L);
		c1.setTotalMessages(23L);
		c1.setUnreadMessages(335L);
		
		c2.setTotalBytes(223L);
		c2.setTotalMessages(223L);
		c2.setUnreadMessages(685L);

		c3.setTotalBytes(100L);
		c3.setTotalMessages(200L);
		c3.setUnreadMessages(350L);
		
		c1.add(c3);
		
		assertEquals(c1, c2);
	}
}
