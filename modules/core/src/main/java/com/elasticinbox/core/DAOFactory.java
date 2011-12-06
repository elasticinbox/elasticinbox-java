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

package com.elasticinbox.core;

import com.elasticinbox.core.cassandra.CassandraDAOFactory;

/**
 * Core Data Access Factory
 *
 * @author Rustam Aliyev
 */
public abstract class DAOFactory
{
	// List of metadata DAO types supported by ElasticInbox
	public static final int CASSANDRA = 1;
	public static final int HBASE = 2;

	// Data Access decomposed into the following DAOs:
	public abstract AccountDAO getAccountDAO();
	public abstract MessageDAO getMessageDAO();
	public abstract LabelDAO getLabelDAO();

	public static DAOFactory getDAOFactory(int whichFactory)
	{
		switch (whichFactory) {
		case CASSANDRA:
			return new CassandraDAOFactory();
		case HBASE:
			// some day...
			return null;
		default:
			return null;
		}
	}
}
