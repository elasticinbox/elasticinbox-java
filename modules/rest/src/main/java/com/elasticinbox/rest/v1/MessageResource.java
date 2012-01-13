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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.Status;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elasticinbox.common.utils.JSONUtils;
import com.elasticinbox.core.DAOFactory;
import com.elasticinbox.core.IllegalLabelException;
import com.elasticinbox.core.MessageDAO;
import com.elasticinbox.core.OverQuotaException;
import com.elasticinbox.core.message.MimeParser;
import com.elasticinbox.core.message.MimeParserException;
import com.elasticinbox.core.message.id.MessageIdBuilder;
import com.elasticinbox.core.model.Mailbox;
import com.elasticinbox.core.model.Marker;
import com.elasticinbox.core.model.Message;
import com.elasticinbox.rest.BadRequestException;

/**
 * This JAX-RS recourse is responsible for new messages and batch operations
 * (i.e. when message not specified).
 * 
 * @author Rustam Aliyev
 * @see SingleMessageResource
 */
@Path("{account}/mailbox/message")
public final class MessageResource
{
	private final MessageDAO messageDAO;

	private final static Logger logger = 
		LoggerFactory.getLogger(MessageResource.class);

	@Context UriInfo uriInfo;

	public MessageResource() {
		DAOFactory dao = DAOFactory.getDAOFactory();
		messageDAO = dao.getMessageDAO();
	}

	/**
	 * Add/store new message with given labels and markers
	 * 
	 * @param account
	 * @param labels automatically assigned labels
	 * @param markers automatically assigned markers
	 * @param file
	 * @return
	 */
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public Response addMessage(
			@PathParam("account") String account,
			@QueryParam("label") Set<Integer> labels,
			@QueryParam("marker") Set<Marker> markers,
			File file)
	{
		Mailbox mailbox = new Mailbox(account);
		// generate new UUID
		UUID messageId = new MessageIdBuilder().build();

		try {
			FileInputStream in = new FileInputStream(file);
			MimeParser parser = new MimeParser();

			// parse message
			parser.parse(in);
			Message message = parser.getMessage();
			message.setSize(file.length()); // update message size
			in.close();

			// add labels to message
			for(Integer label : labels) {
				message.addLabel(label);
			}
			
			// add markers to message
			for(Marker marker : markers) {
				message.addMarker(marker);
			}

			// store message
			in = new FileInputStream(file);
			messageDAO.put(mailbox, messageId, message, in);
			in.close();
		} catch (MimeParserException mpe) {
			logger.error("Unable to parse message: ", mpe);
			throw new BadRequestException("Parsing error. Invalid MIME message.");
		} catch (OverQuotaException oqe) {
			throw new WebApplicationException(Response
					.status(Status.NOT_ACCEPTABLE).entity(oqe.getMessage())
					.type("text/plain").build());
		} catch (IOException ioe) {
			logger.error("Unable to read message stream: ", ioe);
			throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
		} catch (IllegalArgumentException e) {
			throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
		} catch (Exception e) {
			// HectorException
			throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
		} finally {
			if (file.exists()) {
				file.delete();
			}
		}

		// build response
		URI messageUri = uriInfo.getAbsolutePathBuilder()
				.path(messageId.toString()).build();

		String responseJson = new StringBuilder("{\"id\":\"").append(messageId)
				.append("\"}").toString();

		return Response.created(messageUri).entity(responseJson).build();
	}

	/**
	 * Modify multiple messages' labels and markers
	 * 
	 * @param account
	 * @param addLabels
	 * @param removeLabels
	 * @param addMarkers
	 * @param removeMarkers
	 * @param requestJSONContent
	 *            JSON array of message UUIDs e.g. [uuid1, uuid2, ...]
	 * @return
	 */
	@PUT
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response modifyMessages(
			@PathParam("account") final String account,
			@QueryParam("addlabel") final Set<Integer> addLabels,
			@QueryParam("removelabel") final Set<Integer> removeLabels,
			@QueryParam("addmarker") final Set<Marker> addMarkers,
			@QueryParam("removemarker") final Set<Marker> removeMarkers,
			final String requestJSONContent)
	{
		Mailbox mailbox = new Mailbox(account);
		List<UUID> messageIds = null;

		try {
			messageIds = JSONUtils.toUUIDList(requestJSONContent);
		} catch (IllegalStateException jpe) {
			logger.info("Malformed JSON request: {}", jpe.getMessage());
			throw new BadRequestException("Malformed JSON request");
		}

		if (messageIds == null || messageIds.isEmpty())
			throw new BadRequestException("Malformed JSON request");

		try {
			//TODO: make batch request

			// add labels to message
			messageDAO.addLabel(mailbox, addLabels, messageIds);

			// remove label from message
			messageDAO.removeLabel(mailbox, removeLabels, messageIds);

			// add marker
			messageDAO.addMarker(mailbox, addMarkers, messageIds);

			// remove marker
			messageDAO.removeMarker(mailbox, removeMarkers, messageIds);
		} catch (IllegalLabelException ile) {
			throw new BadRequestException(ile.getMessage());
		} catch (Exception e) {
			logger.error("Message modifications failed: ", e);
			throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
		}

		return Response.noContent().build();
	}

	/**
	 * Delete multiple messages
	 * 
	 * @param account
	 * @param requestJSONContent
	 *            JSON array of message UUIDs e.g. [uuid1, uuid2, ...]
	 * @return
	 */
	@DELETE
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response deleteMessages(
			@PathParam("account") final String account,
			final String requestJSONContent)
	{
		Mailbox mailbox = new Mailbox(account);
		List<UUID> messageIds = null;

		try {
			messageIds = JSONUtils.toUUIDList(requestJSONContent);
		} catch (IllegalStateException jpe) {
			logger.info("Malformed JSON request: {}", jpe.getMessage());
			throw new BadRequestException("Malformed JSON request");
		}

		if (messageIds == null || messageIds.isEmpty())
			throw new BadRequestException("Malformed JSON request");

		try {
			// delete message and ignore other parameters
			messageDAO.delete(mailbox, messageIds);
		} catch (Exception e) {
			logger.error("Message deletion failed: ", e);
			throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
		}

		return Response.noContent().build();
	}
	
}
