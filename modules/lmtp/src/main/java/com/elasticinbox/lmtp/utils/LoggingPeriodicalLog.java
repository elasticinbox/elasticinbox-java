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

package com.elasticinbox.lmtp.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ecyrd.speed4j.StopWatch;
import com.ecyrd.speed4j.log.PeriodicalLog;

/**
 * A Logging Periodical log has all capabilities of Periodical log, but also
 * writes the stopwatch data to the given SLF4J logger.
 * 
 * @author Rustam Aliyev
 * @see {@link PeriodicalLog}
 */
public class LoggingPeriodicalLog extends PeriodicalLog
{
	private static final Logger logger = LoggerFactory
			.getLogger(LoggingPeriodicalLog.class);

	public LoggingPeriodicalLog()
	{
		super();
	}

	@Override
	public void log(StopWatch sw)
	{
		// use different logger than superclass
		logger.info(sw.toString());
		super.log(sw);
	}

}
