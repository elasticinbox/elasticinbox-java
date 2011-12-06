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

package com.elasticinbox.lmtp.server.api;

/**
 * RFC 1652 based SMTP/LMTP Service Extension for 8bit-MIMEtransport
 * 
 * @author Rustam Aliyev
 */
public final class LMTPBodyType
{
	private String mType;

	private LMTPBodyType(String type) {
		mType = type;
	}

	public String toString() {
		return mType;
	}

	public static final LMTPBodyType BODY_7BIT = new LMTPBodyType("7BIT");
	public static final LMTPBodyType BODY_8BITMIME = new LMTPBodyType("8BITMIME");

	public static LMTPBodyType getInstance(String type)
	{
		if (type.equalsIgnoreCase(BODY_7BIT.toString())) {
			return BODY_7BIT;
		}
		if (type.equalsIgnoreCase(BODY_8BITMIME.toString())) {
			return BODY_8BITMIME;
		}
		return null;
	}
}