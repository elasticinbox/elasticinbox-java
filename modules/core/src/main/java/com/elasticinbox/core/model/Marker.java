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

package com.elasticinbox.core.model;

/**
 * This is the representation of message markers. Each message can be marked
 * with one or more markers.
 * 
 * @author Rustam Aliyev
 */
public enum Marker
{
	/** Mark message as Seen */
	SEEN(1),

	/** Mark message as Replied */
	REPLIED(2),

	/** Mark message as Forwarded */
	FORWARDED(3);

	private int value;

	Marker(int value) {
		this.value = value;
	}

	public int toInt() {
		return value;
	}

	public static Marker fromInt(int value)
	{
		switch (value) {
		case 1:
			return SEEN;
		case 2:
			return REPLIED;
		default:
			return FORWARDED;
		}
	}

	public static Marker fromString(String value)
	{
		if (value == null) {
			throw new IllegalArgumentException();
		}

		if (value.equals("seen")) {
			return Marker.SEEN;
		} else if (value.equals("replied")) {
			return Marker.REPLIED;
		} else if (value.equals("forwarded")) {
			return Marker.FORWARDED;
		} else {
			throw new IllegalArgumentException();
		}
	}

	public String toString()
	{
		switch (this) {
		case SEEN:
			return "seen";
		case REPLIED:
			return "replied";
		case FORWARDED:
			return "forwarded";
		}

		return null;
	}
}
