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

package com.elasticinbox.common.utils;

/**
* A generic low weight assert utility
* 
*/
public class Assert
{

	public static void notNull(Object object, String message)
	{
		if (object == null) {
			throw new IllegalArgumentException(message);
		}
	}

	public static void noneNull(Object ... object)
	{
		for (int i = 0; i < object.length; ++i) {
			if (object[i] == null) {
				throw new NullPointerException("Null not allowed, number "
						+ (i + 1));
			}
		}
	}

	public static void isTrue(boolean b, String message)
	{
		if (!b) {
			throw new IllegalArgumentException(message);
		}
	}

}
