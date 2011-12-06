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

package com.elasticinbox.core.cassandra;

import me.prettyprint.cassandra.connection.HOpTimer;

import com.ecyrd.speed4j.StopWatch;
import com.ecyrd.speed4j.StopWatchFactory;
import com.ecyrd.speed4j.log.PeriodicalLog;
import com.elasticinbox.config.Configurator;

/**
 * Implementation of {@link HOpTimer} for JMX performance counters without
 * speed4j.properties file.
 * 
 * @author Rustam Aliyev
 */
public final class Speed4jOpTimer implements HOpTimer
{
	private final StopWatchFactory stopWatchFactory;

	public Speed4jOpTimer()
	{
		// Instantiate a new Periodical logger
		PeriodicalLog pLog = new PeriodicalLog();
		pLog.setName("ElasticInbox-Hector");
		pLog.setPeriod(Configurator.getPerformanceCountersInterval());
		pLog.setJmx("READ.success_,WRITE.success_,READ.fail_,WRITE.fail_,META_READ.success_,META_READ.fail_");
		pLog.setSlf4jLogname("com.elasticinbox.speed4j.cassandra.HectorPeriodicalLogger");
		stopWatchFactory = StopWatchFactory.getInstance(pLog);
	}

	@Override
	public Object start() {
		return stopWatchFactory.getStopWatch();
	}

	@Override
	public void stop(Object token, String tagName, boolean success) {
		((StopWatch) token).stop(tagName.concat(success ? ".success_" : ".fail_"));
	}

}
