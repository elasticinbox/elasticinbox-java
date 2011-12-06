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

package com.elasticinbox.core.model;

import java.net.URI;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.codehaus.jackson.annotate.JsonIgnore;

import com.elasticinbox.core.blob.BlobProxy;

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
public class Message
{
	private AddressList from;
	private AddressList to;
	private AddressList cc;
	private AddressList bcc;
	private String subject;
	private Date date;
	private String messageId;
	private Long size;
	private URI location;

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
	
	public AddressList getFrom() {
		return from;
	}

	public void setFrom(AddressList from) {
		this.from = from;
	}

	public AddressList getTo() {
		return to;
	}

	public void setTo(AddressList to) {
		this.to = to;
	}

	public AddressList getCc() {
		return cc;
	}

	public void setCc(AddressList cc) {
		this.cc = cc;
	}

	public AddressList getBcc() {
		return bcc;
	}

	public void setBcc(AddressList bcc) {
		this.bcc = bcc;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}

	public Long getSize() {
		return size;
	}

	public void setSize(Long size) {
		this.size = size;
	}

	public URI getLocation() {
		return location;
	}

	public void setLocation(URI uri)
	{
		if (uri.getScheme().equals(BlobProxy.BLOB_URI_SCHEMA)) {
			this.location = uri;
		} else {
			throw new IllegalArgumentException(
					"Invalid URI scheme specified for blob: " + uri.getScheme());
		}
	}

	public void setLocation(String profile, String path)
	{
		this.location = BlobProxy.buildURI(profile, path);
	}

	public String getPlainBody() {
		return plainBody;
	}

	public void setPlainBody(String plainBody) {
		this.plainBody = plainBody;
	}

	public String getHtmlBody() {
		return htmlBody;
	}

	public void setHtmlBody(String htmlBody) {
		this.htmlBody = htmlBody;
	}

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

	@JsonIgnore
	public MimePart getPart(String partId)
	{
		if(!this.parts.containsKey(partId))
			throw new IllegalArgumentException(
					"Message does not contain part with ID " + partId);

		return this.parts.get(partId);
	}

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

		lc.setTotalBytes(this.size);
		lc.setTotalMessages(1L);

		if(this.markers == null || !this.markers.contains(Marker.SEEN))
			lc.setNewMessages(1L);

		return lc;
	}

}
