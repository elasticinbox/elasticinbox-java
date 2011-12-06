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

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class represents a list of {@link Address} objects
 * 
 * @author Rustam Aliyev
 * @see {@link Address}
 */
public class AddressList extends AbstractList<Address>
{
	private final List<Address> addresses;

	/**
	 * 
	 * @param addresses
	 * 				A List that contains only Mailbox objects.
	 */
	public AddressList(List<Address> addresses)
	{
		if (addresses != null) {
			this.addresses = new ArrayList<Address>(addresses);
		} else {
			this.addresses = Collections.emptyList();
		}
	}

	public String getDisplayString()
	{
		try {
			String s = this.addresses.toString();
			return s.substring(1, s.length() - 1);
		} catch (Exception e) {
			return null;
		}
	}

    /**
     * Gets an address.
     */
    @Override
    public Address get(int index) {
        return addresses.get(index);
    }

	/**
	 * The number of elements in this list.
	 */
	@Override
	public int size() {
		return addresses.size();
	}

}
