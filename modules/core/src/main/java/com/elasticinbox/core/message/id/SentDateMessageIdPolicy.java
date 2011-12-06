/**
 * Copyright (c) 2011 Optimax Software Ltd
 * 
 * This file is part of ElasticInbox.
 * 
 * ElasticInbox is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 2 of the License, or (at your option) any later
 * version.
 * 
 * ElasticInbox is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * ElasticInbox. If not, see <http://www.gnu.org/licenses/>.
 */

package com.elasticinbox.core.message.id;

import java.util.UUID;

import com.elasticinbox.common.utils.Assert;

import me.prettyprint.cassandra.utils.TimeUUIDUtils;

/**
 * Generates unique message ID based on the time when message was sent, assuring
 * a uniqueness within and across threads.
 * 
 * @author Rustam Aliyev
 * @see {@link me.prettyprint.cassandra.service.clock.MicrosecondsSyncClockResolution}
 */
public final class SentDateMessageIdPolicy extends AbstractMessageIdPolicy
{
	/** The last time value issued. Used to try to prevent duplicates. */
	private static long lastTime = -1;
	private static final long ONE_THOUSAND = 1000L;

	@Override
	protected UUID getMessageId(MessageIdBuilder builder)
	{
		Assert.notNull(builder.sentDate, "sent date cannot be null");

		// Message date has granularity of seconds. The following simulates a
		// microseconds resolution by advancing a static counter every time a
		// client calls the getMessageId method, simulating a tick.

		long us = builder.sentDate.getTime() * ONE_THOUSAND;

		// Synchronized to guarantee unique time within and across threads.
		synchronized (SentDateMessageIdPolicy.class)
		{
			if (us > lastTime) {
				lastTime = us;
			} else {
				// the time i got from the system is equals or less
				// (hope not - clock going backwards)
				// One more "microsecond"
				us = ++lastTime;
			}
		}

		return TimeUUIDUtils.getTimeUUID(us);
	}
}
