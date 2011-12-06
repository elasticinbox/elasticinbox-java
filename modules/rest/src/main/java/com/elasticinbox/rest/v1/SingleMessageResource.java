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
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.mail.internet.MimeUtility;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
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


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elasticinbox.common.utils.Assert;
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
import com.elasticinbox.core.model.MimePart;
import com.elasticinbox.rest.BadRequestException;

/**
 * This JAX-RS resource is responsible for manipulating specific message.
 * 
 * @author Rustam Aliyev
 * @see MessageResource
 */
@Path("{account}/mailbox/message/{messageid}")
public final class SingleMessageResource
{
	private final MessageDAO messageDAO;

	private final static Logger logger = 
		LoggerFactory.getLogger(SingleMessageResource.class);

	@Context UriInfo uriInfo;

	public SingleMessageResource() {
		DAOFactory dao = DAOFactory.getDAOFactory(DAOFactory.CASSANDRA);
		messageDAO = dao.getMessageDAO();
	}

	/**
	 * Get parsed message contents (headers and body)
	 * 
	 * @param account
	 * @param messageId
	 * @param labelId
	 * @param markAsSeen Automatically mark as SEEN
	 * @param getAdjacentIds Get prev/next message IDs in given label
	 * @return
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getMessage(
			@PathParam("account") final String account,
			@PathParam("messageid") final UUID messageId,
			@QueryParam("label") final Integer labelId,
			@QueryParam("markseen") @DefaultValue("false") final boolean markAsSeen,
			@QueryParam("adjacent") @DefaultValue("false") final boolean getAdjacentIds)
	{
		Mailbox mailbox = new Mailbox(account);

		byte[] response;
		Map<String, Object> result = new HashMap<String, Object>(3);

		try {
			Message message = messageDAO.getParsed(mailbox, messageId);
			result.put("message", message);

			// automatically mark as seen if requested and not seen yet
			if (markAsSeen && 
					(message.getMarkers() == null || 
							!message.getMarkers().contains(Marker.SEEN))) {
				Set<Marker> markers = new HashSet<Marker>(1);
				markers.add(Marker.SEEN);
				messageDAO.addMarker(mailbox, markers, messageId);
			}

			// get adjacent message ids (prev/next)
			if (getAdjacentIds) {
				Assert.notNull(labelId, "Adjacent messages require label.");

				// fetch next message ID
				List<UUID> ids = messageDAO.getMessageIds(mailbox, labelId, messageId, 2, true);
				if(ids.size() == 2)
					result.put("next", ids.get(1));

				// fetch previous message ID
				ids = messageDAO.getMessageIds(mailbox, labelId, messageId, 2, false);
				if(ids.size() == 2)
					result.put("prev", ids.get(1));
			}

			// get message as JSON
			response = JSONUtils.fromObject(result);

		} catch (IllegalArgumentException iae) {
			throw new BadRequestException(iae.getMessage());
		} catch (Exception e) {
			logger.warn("Internal Server Error: ", e);
			throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
		}

		return Response.ok(response, MediaType.APPLICATION_JSON).build();
	}

	/**
	 * Get original message contents
	 * 
	 * @param account
	 * @param messageId
	 * @return
	 */
	@GET
	@Path("raw")
	@Produces(MediaType.TEXT_PLAIN)
	public Response getOriginalMessage(
			@PathParam("account") final String account,
			@PathParam("messageid") final UUID messageId)
	{
		Mailbox mailbox = new Mailbox(account);

		// Create input pipe
		//PipedInputStream in = new PipedInputStream();
    	InputStream in = null;

		try {
			in = messageDAO.getRaw(mailbox, messageId);
		} catch (IllegalArgumentException iae) {
			throw new BadRequestException(iae.getMessage());
		} catch (Exception e) {
			logger.warn("Internal Server Error: ", e);
			throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
		}

		return Response.ok(in, MediaType.TEXT_PLAIN)
				//.header("Content-Encoding", "deflate")
				.build();
	}

	/**
	 * Redirect to original message blob URI
	 * 
	 * @param account
	 * @param messageId
	 * @return
	 */
	@GET
	@Path("url")
	public Response getMessageUrl(
			@PathParam("account") final String account,
			@PathParam("messageid") final UUID messageId)
	{
		Mailbox mailbox = new Mailbox(account);
		URI uri = null;

		try {
			Message message = messageDAO.getParsed(mailbox, messageId);
			uri = message.getLocation();
			Assert.notNull(uri, "No source message");
		} catch (IllegalArgumentException iae) {
			throw new BadRequestException(iae.getMessage());
		} catch (Exception e) {
			logger.warn("Internal Server Error: ", e);
			throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
		}

		return Response.temporaryRedirect(uri).build();
	}

	/**
	 * Get message part by MIME Part ID
	 * 
	 * @param account
	 * @param messageId
	 * @param partId
	 * @return
	 * @throws IOException 
	 */
	@GET
	@Path("{partid: [0-9]+(\\.[0-9]+)*}")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response getMessagePart(
			@PathParam("account") final String account,
			@PathParam("messageid") final UUID messageId,
			@PathParam("partid") final String partId)
			throws IOException
	{
		Mailbox mailbox = new Mailbox(account);
		InputStream rawIn = null;
		InputStream partIn = null;
		MimePart part = null;

		try {
			rawIn = messageDAO.getRaw(mailbox, messageId);
			MimeParser mimeParser = new MimeParser();
			mimeParser.parse(rawIn);
			part = mimeParser.getMessage().getPart(partId);
			partIn = mimeParser.getInputStreamByPartId(partId);
			rawIn.close();
		} catch (IllegalArgumentException iae) {
			throw new BadRequestException(iae.getMessage());
		} catch (Exception e) {
			logger.warn("Internal Server Error: ", e);
			throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
		} finally {
			if (rawIn != null) rawIn.close();
		}

		return Response
				.ok(partIn, part.getMimeType())
				.header("Content-Disposition", filenameToContentDisposition(part.getFileName()))
				.build();
	}

	/**
	 * Get message part by Content ID
	 * 
	 * @param account
	 * @param messageId
	 * @param contentId
	 * @return
	 * @throws IOException 
	 */
	@GET
	@Path("<{contentid}>")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response getMessagePartByContentId(
			@PathParam("account") final String account,
			@PathParam("messageid") final UUID messageId,
			@PathParam("contentid") final String contentId)
			throws IOException
	{
		Mailbox mailbox = new Mailbox(account);
		InputStream rawIn = null;
		InputStream partIn = null;
		MimePart part = null;

		try {
			rawIn = messageDAO.getRaw(mailbox, messageId);
			MimeParser mimeParser = new MimeParser();
			mimeParser.parse(rawIn);
			part = mimeParser.getMessage().getPartByContentId(contentId);
			partIn = mimeParser.getInputStreamByContentId(contentId);
			rawIn.close();
		} catch (IllegalArgumentException iae) {
			throw new BadRequestException(iae.getMessage());
		} catch (Exception e) {
			logger.info("Internal Server Error: ", e);
			throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
		} finally {
			if (rawIn != null) rawIn.close();
		}

		return Response
				.ok(partIn, part.getMimeType())
				.header("Content-Disposition", filenameToContentDisposition(part.getFileName()))
				.build();
    }

	/**
	 * Update existing message contents, set labels and markers
	 * 
	 * @param account
	 * @param messageId
	 * @param file
	 * @return
	 */
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public Response updateMessage(
			@PathParam("account") final String account,
			@PathParam("messageid") final UUID messageId,
			File file)
	{
		Mailbox mailbox = new Mailbox(account);

		// generate new UUID
		UUID newMessageId = new MessageIdBuilder().build();

		try {
			Message oldMessage = messageDAO.getParsed(mailbox, messageId);

			FileInputStream in = new FileInputStream(file);
			MimeParser parser = new MimeParser();

			// parse message
			parser.parse(in);
			Message newMessage = parser.getMessage();
			newMessage.setSize(file.length()); // update message size
			in.close();

			// add labels to message
			for(Integer label : oldMessage.getLabels()) {
				newMessage.addLabel(label);
			}

			// add markers to message
			for(Marker marker : oldMessage.getMarkers()) {
				newMessage.addMarker(marker);
			}

			// store message
			in = new FileInputStream(file);
			messageDAO.put(mailbox, newMessageId, newMessage, in);
			in.close();

			// delete old message
			messageDAO.delete(mailbox, messageId);
		} catch (MimeParserException mpe) {
			logger.error("Unable to parse message: ", mpe);
			throw new BadRequestException("Parsing error. Invalid MIME message.");
		} catch (OverQuotaException oqe) {
			throw new WebApplicationException(Response.Status.NOT_ACCEPTABLE);
		} catch (IOException ioe) {
			logger.error("Unable to read message stream: ", ioe);
			throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
		} catch (IllegalArgumentException e) {
			throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
		} finally {
			if (file.exists()) {
				file.delete();
			}
		}

		// build response
		URI messageUri = uriInfo.getAbsolutePathBuilder()
				.path(newMessageId.toString()).build();

		String responseJson = new StringBuilder("{\"id\":\"").append(newMessageId)
				.append("\"}").toString();

		return Response.created(messageUri).entity(responseJson).build();
	}

	/**
	 * Modify message labels and markers
	 * 
	 * @param account
	 * @param messageId
	 * @param addLabels
	 * @param removeLabels
	 * @param addMarkers
	 * @param removeMarkers
	 * @return
	 */
	@PUT
	@Produces(MediaType.APPLICATION_JSON)
	public Response modifyMessage(
			@PathParam("account") String account,
			@PathParam("messageid") UUID messageId,
			@QueryParam("addlabel") Set<Integer> addLabels,
			@QueryParam("removelabel") Set<Integer> removeLabels,
			@QueryParam("addmarker") Set<Marker> addMarkers,
			@QueryParam("removemarker") Set<Marker> removeMarkers)
	{
		Mailbox mailbox = new Mailbox(account);

		try {
			// add labels to message
			messageDAO.addLabel(mailbox, addLabels, messageId);

			// remove labels from message
			messageDAO.removeLabel(mailbox, removeLabels, messageId);

			// add markers
			messageDAO.addMarker(mailbox, addMarkers, messageId);

			// remove markers
			messageDAO.removeMarker(mailbox, removeMarkers, messageId);
		} catch (IllegalLabelException ile) {
			throw new BadRequestException(ile.getMessage());
		} catch (Exception e) {
			logger.warn("Internal Server Error: ", e);
			throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
		}

		return Response.noContent().build();
	}

	/**
	 * Delete message
	 * 
	 * @param account
	 * @param messageId
	 * @return
	 */
	@DELETE
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteMessage(
			@PathParam("account") String account,
			@PathParam("messageid") UUID messageId)
	{
		Mailbox mailbox = new Mailbox(account);

		try {
			messageDAO.delete(mailbox, messageId);
		} catch (Exception e) {
			logger.warn("Internal Server Error: ", e);
			throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
		}

		return Response.noContent().build();
    }

	/**
	 * Encodes filename and produces string for content disposition value
	 * 
	 * @param fileName
	 * @return
	 * @throws UnsupportedEncodingException 
	 */
	private static String filenameToContentDisposition(String fileName) throws UnsupportedEncodingException
	{
		if(fileName != null) {
			// TODO: For IE, URLEncoder.encode(filename, "utf-8") should be used instead
			return new StringBuilder("attachment; filename=\"")
					.append(MimeUtility.encodeWord(fileName, "utf-8", "Q"))
					.append("\"").toString();
		} else {
			// filename is optional, see RFC2138
			return "attachment;";
		}
	}

}
