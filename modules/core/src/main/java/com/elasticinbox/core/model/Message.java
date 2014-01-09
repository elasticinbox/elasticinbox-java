/**
 * Copyright (c) 2011-2014 Optimax Software Ltd.
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

import java.net.URI;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import com.elasticinbox.core.blob.BlobURI;

/**
 * Representation of MIME message containing message headers, labels, markers,
 * bodies (HTML and plain) and attachments ({@link MimePart}).
 * <p>
 * Attachments are indexed by MIME part number, where part number is a string of
 * integers delimited by period which index into a body part list as per the
 * IMAP4 RFC3501 specification.
 * 
 * @author Rustam Aliyev
 * @see {@link MimePart}
 * @see <a href="http://tools.ietf.org/html/rfc3501#section-6.4.5">RFC3501</a>
 */
@JsonInclude(Include.NON_NULL)
public class Message
{
	private URI location;
	private Long size;

	// Message headers
	private AddressList from;
	private AddressList to;
	private AddressList cc;
	private AddressList bcc;
	private String subject;
	private Date date;
	private String messageId;
	private AddressList replyTo;

	// Message content
	private String plainBody;
	private String htmlBody;

	private HashSet<Integer> labels = new HashSet<Integer>();
	private EnumSet<Marker> markers = EnumSet.noneOf(Marker.class);

	/**
	 * List of MIME non-body parts (attachments) indexed by part number.
	 */
	private Map<String, MimePart> parts = new HashMap<String, MimePart>(1);

	/**
	 * Reverse index for message Content-id, where key is content-id and value
	 * is part number.
	 */
	@JsonIgnore
	private Map<String, String> partsByContentId = new HashMap<String, String>(1);

	/**
	 * Additional headers which are not stored in metadata, but used for filtering.
	 */
	@JsonIgnore
	private Map<String, String> minorHeaders = new HashMap<String, String>(1);
	
	/**
	 * Returns the value of the "From" header fields. If this 
     * header field is absent, the "Sender" header field is used.
     * 
	 * @return
	 */
	public AddressList getFrom() {
		return from;
	}

	public void setFrom(AddressList from) {
		this.from = from;
	}

	/**
	 * Returns the value of the "To" header fields.
	 * 
	 * @return
	 */
	public AddressList getTo() {
		return to;
	}

	public void setTo(AddressList to) {
		this.to = to;
	}

	/**
	 * Returns the value of the "CC" header fields.
	 * 
	 * @return
	 */
	public AddressList getCc() {
		return cc;
	}

	public void setCc(AddressList cc) {
		this.cc = cc;
	}

	/**
	 * Returns the value of the "BCC" header fields.
	 * 
	 * @return
	 */
	public AddressList getBcc() {
		return bcc;
	}

	public void setBcc(AddressList bcc) {
		this.bcc = bcc;
	}

	/**
	 * Returns the value of the "Subject" header field.
	 * 
	 * @return
	 */
	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	/**
	 * Returns the value of the "Date" header field.
	 * 
	 * @return
	 */
	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	/**
	 * Returns the value of the "Message-ID" header field.
	 * 
	 * @return
	 */
	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}

	/**
	 * Returns the value of the "Reply-To" header field.
	 * 
	 * @return
	 */
	public AddressList getReplyTo() {
		return replyTo;
	}

	public void setReplyTo(AddressList replyTo) {
		this.replyTo = replyTo;
	}

	/**
	 * Return the size of the content of this message in bytes.
	 * 
	 * @return
	 */
	public Long getSize() {
		return size;
	}

	public void setSize(Long size) {
		this.size = size;
	}

	/**
	 * Returns blob URI containing raw message content
	 * 
	 * @return
	 */
	public URI getLocation() {
		return location;
	}

	public void setLocation(URI uri)
	{
		// validate URI, should throw exception if invalid
		new BlobURI().fromURI(uri);

		this.location = uri;
	}

	/**
	 * Returns plain text message body if available.
	 *  
	 * @return
	 */
	public String getPlainBody() {
		return plainBody;
	}

	public void setPlainBody(String plainBody) {
		this.plainBody = plainBody;
	}

	/**
	 * Returns HTML message body if available.
	 * 
	 * @return
	 */
	public String getHtmlBody() {
		return htmlBody;
	}

	public void setHtmlBody(String htmlBody) {
		this.htmlBody = htmlBody;
	}

	/**
	 * Returns information about all message attachments (MIME parts)
	 * 
	 * @return
	 */
	public Map<String, MimePart> getParts() {
		return this.parts.isEmpty() ? null : parts;
	}

	public void setParts(Map<String, MimePart> attachments) {
		this.parts = attachments;
	}

	@JsonIgnore
	public void addPart(String partId, MimePart part) {
		part.setPartId(partId);
		this.parts.put(partId, part);

		// add to content-id index if any
		if (part.getContentId() != null) {
			this.partsByContentId.put(part.getContentId(), partId);
		}
	}

	/**
	 * Returns binary content for the given MIME part ID.
	 * 
	 * @param partId
	 */
	@JsonIgnore
	public MimePart getPart(String partId)
	{
		if(!this.parts.containsKey(partId))
			throw new IllegalArgumentException(
					"Message does not contain part with ID " + partId);

		return this.parts.get(partId);
	}

	/**
	 * Returns binary content for the given MIME Content-ID.
	 * 
	 * @param contentId
	 * @return
	 */
	@JsonIgnore
	public MimePart getPartByContentId(String contentId)
	{
		if (!this.partsByContentId.containsKey(contentId))
			throw new IllegalArgumentException(
					"Message does not contain part with Content-ID " + contentId);

		return getPart(this.partsByContentId.get(contentId));
	}

	@JsonIgnore
	public boolean hasParts() {
		return !this.parts.isEmpty();
	}

//	@JsonIgnore
//	public boolean hasAttachment() {
//		//TODO: check if any content-disposition is attachment, ignore inline
//		return false;
//	}

	/**
	 * Returns all labels assigned to the message.
	 * 
	 * @return
	 */
	public Set<Integer> getLabels() {
		return this.labels;
	}

	public void setLabels(Set<Integer> labels) {
		this.labels = new HashSet<Integer>(labels);
	}

	@JsonIgnore
	public void addLabel(Integer labelId) {
		this.labels.add(labelId);
	}

	/**
	 * Returns all markers assigned to the message.
	 * 
	 * @return
	 */
	public Set<Marker> getMarkers() {
		return this.markers;
	}

	public void setMarkers(Set<Marker> markers) {
		this.markers = EnumSet.copyOf(markers);
	}

	@JsonIgnore
	public void addMarker(Marker marker) {
		this.markers.add(marker);
	}

	/**
	 * Get message header by name
	 * 
	 * @param headerName
	 * @return
	 */
	@JsonIgnore
	public String getMinorHeader(String headerName) {
		return this.minorHeaders.get(headerName.toLowerCase());
	}

	/**
	 * Add message header (will update if exists).
	 * 
	 * @param headerName
	 *            Name of the message header
	 * @param headerValue
	 *            Value of the message header
	 */
	@JsonIgnore
	public void addMinorHeader(String headerName, String headerValue)
	{
		if (headerValue != null) {
			this.minorHeaders.put(headerName.toLowerCase(), headerValue);
		}
	}

	/**
	 * Get Message parameters as {@link LabelCounters} object
	 * 
	 * @return
	 */
	@JsonIgnore
	public LabelCounters getLabelCounters()
	{
		LabelCounters lc = new LabelCounters();
		
		lc.setTotalMessages(1L);

		if (this.size != null) {
			lc.setTotalBytes(this.size);
		}

		if (this.markers == null || !this.markers.contains(Marker.SEEN)) {
			lc.setUnreadMessages(1L);
		}

		return lc;
	}

}
