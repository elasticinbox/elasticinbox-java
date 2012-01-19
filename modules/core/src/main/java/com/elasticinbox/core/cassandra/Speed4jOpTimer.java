/**
 * Copyright (c) 2011-2012 Optimax Software Ltd.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *  * Neither the name of Optimax Software, ElasticInbox, nor the names
 *    of its contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
