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

import org.codehaus.jackson.annotate.JsonIgnore;

/**
 * This class represents a single e-mail address.
 */
public class Address
{
	private final String name;
	private final String address;

	/**
	 * Creates new address
	 * 
	 * @param name
	 * 				The name of the email address. May be <code>null</code>.
	 * @param address
	 * 				Email address <i>localPart@domain.com</i>. May be <code>null</code>.
	 */
	public Address(String name, String address) {
		this.name = name;
		this.address = address;
	}

    /**
     * Returns the name of the mailbox or <code>null</code> if it does not
     * have a name.
     */
	public String getName() {
		return this.name;
	}

    /**
     * Returns the e-mail address ("user@example.com").
     */
	public String getAddress() {
		return this.address;
	}

	@JsonIgnore
	@Override
	public String toString() {
        boolean includeAngleBrackets = (name != null);

        StringBuilder sb = new StringBuilder();

        if (name != null) {
            sb.append(name).append(' ');
        }

        if (includeAngleBrackets) {
            sb.append('<');
        }

        sb.append(address);

        if (includeAngleBrackets) {
            sb.append('>');
        }

        return sb.toString();
	}
	
}
