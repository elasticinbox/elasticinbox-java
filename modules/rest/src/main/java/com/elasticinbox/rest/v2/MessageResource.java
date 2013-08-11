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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
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
import com.elasticinbox.core.MessageModification;
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
@Path("{domain}/{user}/mailbox/message")
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
			@PathParam("user") final String user,
			@PathParam("domain") final String domain,
			@QueryParam("label") Set<Integer> labels,
			@QueryParam("marker") Set<Marker> markers,
			File file)
	{
		Mailbox mailbox = new Mailbox(user, domain);
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
			@PathParam("user") final String user,
			@PathParam("domain") final String domain,
			@QueryParam("addlabel") final Set<Integer> addLabels,
			@QueryParam("removelabel") final Set<Integer> removeLabels,
			@QueryParam("addmarker") final Set<Marker> addMarkers,
			@QueryParam("removemarker") final Set<Marker> removeMarkers,
			final String requestJSONContent)
	{
		Mailbox mailbox = new Mailbox(user, domain);
		List<UUID> messageIds = null;

		try {
			messageIds = JSONUtils.toUUIDList(requestJSONContent);
		} catch (IllegalStateException jpe) {
			logger.info("Malformed JSON request: {}", jpe.getMessage());
			throw new BadRequestException("Malformed JSON request");
		}

		if (messageIds == null || messageIds.isEmpty()) {
			throw new BadRequestException("Malformed JSON request");
		}

		try {
			// Deduplicate message IDs
			List<UUID> depdupeMessageIds =
				    new ArrayList<UUID>(new LinkedHashSet<UUID>(messageIds));

			// Prepare modification
			MessageModification modification = new MessageModification.Builder()
					.addLabels(addLabels).removeLabels(removeLabels)
					.addMarkers(addMarkers).removeMarkers(removeMarkers)
					.build();

			messageDAO.modify(mailbox, depdupeMessageIds, modification);
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
			@PathParam("user") final String user,
			@PathParam("domain") final String domain,
			final String requestJSONContent)
	{
		Mailbox mailbox = new Mailbox(user, domain);
		List<UUID> messageIds = null;

		try {
			messageIds = JSONUtils.toUUIDList(requestJSONContent);
		} catch (IllegalStateException jpe) {
			logger.info("Malformed JSON request: {}", jpe.getMessage());
			throw new BadRequestException("Malformed JSON request");
		}

		if (messageIds == null || messageIds.isEmpty()) {
			throw new BadRequestException("Malformed JSON request");
		}

		try {
			// Deduplicate message IDs
			List<UUID> depdupeMessageIds =
				    new ArrayList<UUID>(new LinkedHashSet<UUID>(messageIds));

			// delete message and ignore other parameters
			messageDAO.delete(mailbox, depdupeMessageIds);
		} catch (Exception e) {
			logger.error("Message deletion failed: ", e);
			throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
		}

		return Response.noContent().build();
	}
	
}
