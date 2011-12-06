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

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

/**
 * This class is the representation of mailbox object. Each mailbox uniquely
 * identified by RFC5322 compatible email address.
 * 
 * @author Rustam Aliyev
 * @see <a href="http://tools.ietf.org/html/rfc5322">RFC5322</a>
 */
public final class Mailbox
{
	private final String id;

	public Mailbox(String email)
	{
		isEmailAddress(email, "Mailbox ID should be valid RFC5322 email address");
		
		// use email as mailbox id
		this.id = email.toLowerCase();

		//TODO: add support for mailbox aliasing
	}

	/**
	 * Returns unique mailbox identifier
	 * 
	 * @return
	 */
	public String getId() {
		return id;
	}
	
	@Override
	public String toString() {
		return new StringBuilder("Mailbox: {id:").append(id).append("}").toString();
	}

	/**
	 * Verify email address against RFC5322
	 * 
	 * @param email
	 * @param message
	 */
	public static void isEmailAddress(String email, String message)
	{
		try {
			InternetAddress emailAddr = new InternetAddress(email);
			emailAddr.validate();
		} catch (AddressException ex) {
			throw new IllegalArgumentException(message);
		}
	}

}
