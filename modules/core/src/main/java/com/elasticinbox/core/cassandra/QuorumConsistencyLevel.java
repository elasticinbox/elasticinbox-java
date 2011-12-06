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

import me.prettyprint.cassandra.service.OperationType;
import me.prettyprint.hector.api.ConsistencyLevelPolicy;
import me.prettyprint.hector.api.HConsistencyLevel;

/**
 * Set Cassandra operations' consistency level to QUORUM for both - read and
 * write operations.
 * 
 * @author Rustam Aliyev
 */
public final class QuorumConsistencyLevel implements ConsistencyLevelPolicy
{
	@Override
	public HConsistencyLevel get(OperationType op)
	{
		switch (op) {
			case READ:
				return HConsistencyLevel.QUORUM;
			case WRITE:
				return HConsistencyLevel.QUORUM;
			default:
				return HConsistencyLevel.QUORUM; // just in Case
		}
	}

	@Override
	public HConsistencyLevel get(OperationType op, String cfName)
	{
		switch (op) {
			case READ:
				return HConsistencyLevel.QUORUM;
			case WRITE:
				return HConsistencyLevel.QUORUM;
			default:
				return HConsistencyLevel.QUORUM; // just in Case
		}
	}

}
