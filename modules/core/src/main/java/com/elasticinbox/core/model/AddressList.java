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
	 * Create new address list
	 * 
	 * @param addresses
	 * 				A List that contains only Address objects.
	 */
	public AddressList(List<Address> addresses)
	{
		if (addresses != null) {
			this.addresses = new ArrayList<Address>(addresses);
		} else {
			this.addresses = Collections.emptyList();
		}
	}

	/**
	 * Create new address list
	 * 
	 * @param address
	 */
	public AddressList(Address address)
	{
		if (address != null) {
			this.addresses = new ArrayList<Address>(1);
			this.addresses.add(address);
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
