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
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.mail.internet.MimeUtility;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
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
import com.elasticinbox.core.MessageModification;
import com.elasticinbox.core.OverQuotaException;
import com.elasticinbox.core.blob.BlobDataSource;
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
@Path("{domain}/{user}/mailbox/message/{messageid}")
public final class SingleMessageResource
{
	private final MessageDAO messageDAO;

	private final static Logger logger = 
		LoggerFactory.getLogger(SingleMessageResource.class);

	@Context UriInfo uriInfo;

	public SingleMessageResource() {
		DAOFactory dao = DAOFactory.getDAOFactory();
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
			@PathParam("user") final String user,
			@PathParam("domain") final String domain,
			@PathParam("messageid") final UUID messageId,
			@QueryParam("label") final Integer labelId,
			@QueryParam("markseen") @DefaultValue("false") final boolean markAsSeen,
			@QueryParam("adjacent") @DefaultValue("false") final boolean getAdjacentIds)
	{
		Mailbox mailbox = new Mailbox(user, domain);

		byte[] response;
		Map<String, Object> result = new HashMap<String, Object>(3);

		try {
			Message message = messageDAO.getParsed(mailbox, messageId);
			result.put("message", message);

			// automatically mark as seen if requested and not seen yet
			if (markAsSeen && !message.getMarkers().contains(Marker.SEEN))
			{
				messageDAO.modify(mailbox, messageId,
						new MessageModification.Builder().addMarker(Marker.SEEN).build());
			}

			// get adjacent message ids (prev/next)
			if (getAdjacentIds)
			{
				Assert.notNull(labelId, "Adjacent messages require label.");

				// fetch next message ID
				List<UUID> ids = messageDAO.getMessageIds(mailbox, labelId, messageId, 2, true);
				if (ids.size() == 2) {
					result.put("next", ids.get(1));
				}

				// fetch previous message ID
				ids = messageDAO.getMessageIds(mailbox, labelId, messageId, 2, false);
				if (ids.size() == 2) {
					result.put("prev", ids.get(1));
				}
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
	public Response getRawMessage(
			@HeaderParam(HttpHeaders.ACCEPT_ENCODING) String acceptEncoding,
			@PathParam("user") final String user,
			@PathParam("domain") final String domain,
			@PathParam("messageid") final UUID messageId)
	{
		Mailbox mailbox = new Mailbox(user, domain);

		Response response;

		try {
			BlobDataSource blobDS = messageDAO.getRaw(mailbox, messageId);

			if (acceptEncoding != null && acceptEncoding.contains("deflate")
					&& blobDS.isCompressed())
			{
				response = Response
						.ok(blobDS.getInputStream(), MediaType.TEXT_PLAIN)
						.header(HttpHeaders.CONTENT_ENCODING, "deflate").build();
			} else {
				response = Response.ok(blobDS.getUncompressedInputStream(),
						MediaType.TEXT_PLAIN).build();
			}
		} catch (IllegalArgumentException iae) {
			throw new BadRequestException(iae.getMessage());
		} catch (Exception e) {
			logger.warn("Internal Server Error: ", e);
			throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
		}

		return response;
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
			@PathParam("user") final String user,
			@PathParam("domain") final String domain,
			@PathParam("messageid") final UUID messageId)
	{
		Mailbox mailbox = new Mailbox(user, domain);
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
			@PathParam("user") final String user,
			@PathParam("domain") final String domain,
			@PathParam("messageid") final UUID messageId,
			@PathParam("partid") final String partId)
			throws IOException
	{
		Mailbox mailbox = new Mailbox(user, domain);
		InputStream rawIn = null;
		InputStream partIn = null;
		MimePart part = null;

		try {
			rawIn = messageDAO.getRaw(mailbox, messageId).getUncompressedInputStream();
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
			@PathParam("user") final String user,
			@PathParam("domain") final String domain,
			@PathParam("messageid") final UUID messageId,
			@PathParam("contentid") final String contentId)
			throws IOException
	{
		Mailbox mailbox = new Mailbox(user, domain);
		InputStream rawIn = null;
		InputStream partIn = null;
		MimePart part = null;

		try {
			rawIn = messageDAO.getRaw(mailbox, messageId).getUncompressedInputStream();
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
			@PathParam("user") final String user,
			@PathParam("domain") final String domain,
			@PathParam("messageid") final UUID messageId,
			File file)
	{
		Mailbox mailbox = new Mailbox(user, domain);

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
			@PathParam("user") final String user,
			@PathParam("domain") final String domain,
			@PathParam("messageid") UUID messageId,
			@QueryParam("addlabel") Set<Integer> addLabels,
			@QueryParam("removelabel") Set<Integer> removeLabels,
			@QueryParam("addmarker") Set<Marker> addMarkers,
			@QueryParam("removemarker") Set<Marker> removeMarkers)
	{
		Mailbox mailbox = new Mailbox(user, domain);

		try {
			MessageModification modification = new MessageModification.Builder()
					.addLabels(addLabels).removeLabels(removeLabels)
					.addMarkers(addMarkers).removeMarkers(removeMarkers)
					.build();

			messageDAO.modify(mailbox, messageId, modification);
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
			@PathParam("user") final String user,
			@PathParam("domain") final String domain,
			@PathParam("messageid") UUID messageId)
	{
		Mailbox mailbox = new Mailbox(user, domain);

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
