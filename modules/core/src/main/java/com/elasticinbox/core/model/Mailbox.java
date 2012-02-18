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

	/**
	 * Build Mailbox object from email
	 * 
	 * @param email
	 */
	public Mailbox(String email)
	{
		isEmailAddress(email, "Mailbox ID should be valid RFC5322 email address");
		
		// use email as mailbox id
		this.id = email.toLowerCase();

		//TODO: add support for mailbox aliasing
	}

	/**
	 * Build Mailbox from user name and domain name
	 * 
	 * @param user User name
	 * @param domain Domain name
	 */
	public Mailbox(String user, String domain)
	{
		this(new StringBuilder(user).append("@").append(domain).toString());
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
