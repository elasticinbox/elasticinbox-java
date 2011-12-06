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

package com.elasticinbox.rest.v1;

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
@Path("{account}/mailbox")
public final class MailboxResource
{
	private final LabelDAO labelDAO;
	private final MessageDAO messageDAO;

	private final static Logger logger = LoggerFactory
			.getLogger(MailboxResource.class);

	public MailboxResource()
	{
		DAOFactory dao = DAOFactory.getDAOFactory(DAOFactory.CASSANDRA);
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
			@PathParam("account") String account,
			@QueryParam("metadata") @DefaultValue("false") boolean getMetadata)
	{
		Mailbox mailbox = new Mailbox(account);
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
			@PathParam("account") String account,
			@QueryParam("age") Date age)
	{
		Mailbox mailbox = new Mailbox(account);

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
