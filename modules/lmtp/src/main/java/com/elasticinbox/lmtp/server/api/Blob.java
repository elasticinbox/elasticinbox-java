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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.elasticinbox.lmtp.utils.SharedStreamUtils;

/**
 * This class stores message contents read from LMTP. It is possible to get
 * raw message size and copy of the message <code>InputStream</code>.
 * 
 * @author Rustam Aliyev
 */
public class Blob
{
	private final long size;
	private InputStream in;
	private String prepend;

	public Blob(InputStream in, final long size)
	{
		if (in == null) {
			throw new NullPointerException("input cannot be null");
		}

		this.in = in;
		this.size = size;
		this.prepend = null;
	}

	/**
	 * Get copy of the message InputStream
	 * 
	 * @return
	 * @throws IOException
	 */
	public InputStream getInputStream() throws IOException
	{
		InputStream cloneStream = SharedStreamUtils.getPrivateInputStream(in);

		if (prepend != null) {
			// prepend data stream with additional headers
			List<InputStream> streams = new ArrayList<InputStream>(2);
			streams.add(new ByteArrayInputStream(prepend.getBytes()));
			streams.add(cloneStream);
			return new SequenceInputStream(Collections.enumeration(streams));
		} else {
			return cloneStream;
		}
	}

	/**
	 * Prepend main stream with given string
	 * 
	 * @param String
	 */
	public void prepend(String s) {
		this.prepend = s;
	}

	/**
	 * Returns the size of the blob's data.
	 */
	public long getRawSize() {
		return (prepend == null) ? size : (size + prepend.length());
	}

}
