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
