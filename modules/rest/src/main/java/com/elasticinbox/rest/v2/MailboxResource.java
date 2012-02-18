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

import java.util.Date;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elasticinbox.common.utils.JSONUtils;
import com.elasticinbox.core.DAOFactory;
import com.elasticinbox.core.LabelDAO;
import com.elasticinbox.core.MessageDAO;
import com.elasticinbox.core.model.Mailbox;

/**
 * This JAX-RS resource is responsible for mailbox operations such as listing
 * available labels and purging mailbox.
 * 
 * @author Rustam Aliyev
 */
@Path("{domain}/{user}/mailbox")
public final class MailboxResource
{
	private final LabelDAO labelDAO;
	private final MessageDAO messageDAO;

	private final static Logger logger = LoggerFactory
			.getLogger(MailboxResource.class);

	public MailboxResource()
	{
		DAOFactory dao = DAOFactory.getDAOFactory();
		labelDAO = dao.getLabelDAO();
		messageDAO = dao.getMessageDAO();
	}

	/**
	 * Get list of all labels in the mailbox. Optionally returns number of new
	 * and total messages, and total size of messages for each label.
	 * 
	 * @param account
	 * @param getMetadata
	 *            Returns counters for each of the labels if set to true.
	 *            Default is false.
	 * @return
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getLabels(
			@PathParam("user") final String user,
			@PathParam("domain") final String domain,
			@QueryParam("metadata") @DefaultValue("false") boolean getMetadata)
	{
		Mailbox mailbox = new Mailbox(user, domain);
		byte[] response;

		try {
			if (getMetadata) {
				response = JSONUtils.fromObject(labelDAO.getAllWithMetadata(mailbox));
			} else {
				response = JSONUtils.fromObject(labelDAO.getAll(mailbox));
			}
		} catch (Exception e) {
			logger.error("Failed to get label list:", e);
			throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
		}

		return Response.ok(response).build();
	}

	/**
	 * Purge deleted messages older than given date
	 * 
	 * @param account
	 * @param age
	 * @return
	 */
	@PUT
	@Path("purge")
	@Produces(MediaType.APPLICATION_JSON)
	public Response purge(
			@PathParam("user") final String user,
			@PathParam("domain") final String domain,
			@QueryParam("age") Date age)
	{
		Mailbox mailbox = new Mailbox(user, domain);

		// set date to now if not given (purges all messages)
		if (age == null)
			age = new Date();

		try {
			messageDAO.purge(mailbox, age);
		} catch (Exception e) {
			logger.error("Failed to purge messages:", e);
			throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
		}

		return Response.noContent().build();
	}

}
