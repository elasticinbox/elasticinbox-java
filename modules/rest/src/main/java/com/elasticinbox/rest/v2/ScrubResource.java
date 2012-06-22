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

package com.elasticinbox.rest.v2;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.elasticinbox.core.DAOFactory;
import com.elasticinbox.core.LabelDAO;
import com.elasticinbox.core.MessageDAO;
import com.elasticinbox.core.model.Labels;
import com.elasticinbox.core.model.Mailbox;

/**
 * This JAX-RS resource is responsible for maintaining mailbox consitency
 * through scrub operation.
 * 
 * @author Rustam Aliyev
 */
@Path("{domain}/{user}/mailbox/scrub")
public class ScrubResource
{
	private final MessageDAO messageDAO;
	private final LabelDAO labelDAO;

	@Context UriInfo uriInfo;

	public ScrubResource() {
		DAOFactory dao = DAOFactory.getDAOFactory();
		messageDAO = dao.getMessageDAO();
		labelDAO = dao.getLabelDAO();
	}

	/**
	 * Scrub mailbox and recalculate counters
	 * 
	 * @param account
	 * @return
	 */
	@POST
	@Path("counters")
	@Produces(MediaType.APPLICATION_JSON)
	public Response scrubIndex(
			@PathParam("user") final String user,
			@PathParam("domain") final String domain)
	{
		Mailbox mailbox = new Mailbox(user, domain);
		
		Labels calculatedCounters = messageDAO.calculateCounters(mailbox);
		labelDAO.setCounters(mailbox, calculatedCounters);
		
		return Response.noContent().build();
	}

}
