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

package com.elasticinbox.rest;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;

import javax.ws.rs.core.MediaType;

/**
 * This response filter adds missing <code>charset</code> information to the
 * HTTP response
 * 
 * @author Rustam Aliyev
 */
public class CharsetResponseFilter implements ContainerResponseFilter
{

    public ContainerResponse filter(ContainerRequest request, ContainerResponse response)
    {
		MediaType contentType = response.getMediaType();

		// For JSON responses use UTF-8 charset
		if((contentType != null) && contentType.equals(MediaType.APPLICATION_JSON_TYPE)) {
			response.getHttpHeaders().putSingle("Content-Type",
					contentType.toString() + ";charset=UTF-8");
		}

        return response;
    }

}
