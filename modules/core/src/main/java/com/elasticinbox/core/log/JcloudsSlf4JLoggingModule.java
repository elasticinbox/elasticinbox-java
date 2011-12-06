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

package com.elasticinbox.core.log;

import org.jclouds.logging.Logger.LoggerFactory;
import org.jclouds.logging.config.LoggingModule;

/**
* Configures jclouds logging of type {@link JcloudsSlf4JLogger}
*
* @author Rustam Aliyev
*
*/
public final class JcloudsSlf4JLoggingModule extends LoggingModule
{
	@Override
	public LoggerFactory createLoggerFactory() {
		return new JcloudsSlf4JLogger.JcloudsSlf4JLoggerFactory();
	}
}
