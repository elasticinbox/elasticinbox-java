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

package com.elasticinbox.core.cassandra.persistence;

import static me.prettyprint.hector.api.factory.HFactory.createColumn;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.prettyprint.cassandra.serializers.BytesArraySerializer;
import me.prettyprint.cassandra.serializers.DateSerializer;
import me.prettyprint.cassandra.serializers.LongSerializer;
import me.prettyprint.cassandra.serializers.SerializerTypeInferer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.beans.HColumn;

import com.elasticinbox.common.utils.IOUtils;
import com.elasticinbox.common.utils.JSONUtils;
import com.elasticinbox.config.Configurator;
import com.elasticinbox.core.model.Address;
import com.elasticinbox.core.model.AddressList;
import com.elasticinbox.core.model.Marker;
import com.elasticinbox.core.model.Message;
import com.elasticinbox.core.model.MimePart;

public final class Marshaller
{
	public final static String CN_DATE = "date";
	public final static String CN_SIZE = "size";
	public final static String CN_FROM = "from";
	public final static String CN_TO = "to";
	public final static String CN_CC = "cc";
	public final static String CN_BCC = "bcc";
	public final static String CN_REPLY_TO = "replyto";
	public final static String CN_MESSAGE_ID = "mid";
	public final static String CN_SUBJECT = "subject";
	public final static String CN_HTML_BODY = "html";
	public final static String CN_PLAIN_BODY = "plain";
	public final static String CN_PARTS = "parts";
	public final static String CN_BRI = "bri"; // Blob Resource Identifier
	public final static String CN_LABEL_PREFIX = "l:";
	public final static String CN_MARKER_PREFIX = "m:";

	private final static DateSerializer dateSe = DateSerializer.get();
	private final static LongSerializer longSe = LongSerializer.get();
	private final static StringSerializer strSe = StringSerializer.get();
	private final static BytesArraySerializer byteSe = BytesArraySerializer.get();

	/**
	 * Unmarshall message contents from Cassandra {@link HColumn} columns and
	 * return resulting {@link Message} object.
	 * 
	 * @param columns
	 *            Cassandra columns to unmarshall
	 * @param includeBody
	 *            Contents of the message body (plain and HTML) will not be
	 *            included in the result if set to false
	 * @return
	 */
	protected static Message unmarshall(
			final List<HColumn<String, byte[]>> columns,
			final boolean includeBody)
	{
		Message message = new Message();

		for (HColumn<String, byte[]> c : columns)
		{
			if (c != null && c.getValue() != null)
			{
				// map columns to Message object
				if (c.getName().equals(CN_DATE)) {
					message.setDate(dateSe.fromBytes(c.getValue()));
				} else if (c.getName().equals(CN_SIZE)) {
					message.setSize(longSe.fromBytes(c.getValue()));
				} else if (c.getName().equals(CN_SUBJECT)) {
					message.setSubject(strSe.fromBytes(c.getValue()));
				} else if (c.getName().equals(CN_MESSAGE_ID)) {
					message.setMessageId(strSe.fromBytes(c.getValue()));
				} else if (c.getName().equals(CN_FROM)) {
					message.setFrom(unserializeAddress(c.getValue()));
				} else if (c.getName().equals(CN_TO)) {
					message.setTo(unserializeAddress(c.getValue()));
				} else if (c.getName().equals(CN_CC)) {
					message.setCc(unserializeAddress(c.getValue()));
				} else if (c.getName().equals(CN_BCC)) {
					message.setBcc(unserializeAddress(c.getValue()));
				} else if (c.getName().equals(CN_REPLY_TO)) {
					message.setReplyTo(unserializeAddress(c.getValue()));
				} else if (c.getName().equals(CN_BRI)) {
					message.setLocation(URI.create(
							strSe.fromBytes(c.getValue())));
				} else if (c.getName().startsWith(CN_LABEL_PREFIX)) {
					Integer labelId = Integer
							.parseInt(c.getName().split("\\:")[1]);
					message.addLabel(labelId);
				} else if (c.getName().startsWith(CN_MARKER_PREFIX)) {
					Integer markerId = Integer
							.parseInt(c.getName().split("\\:")[1]);
					message.addMarker(Marker.fromInt(markerId));
				} else if (includeBody && c.getName().equals(CN_HTML_BODY)) {
					try {
						message.setHtmlBody(strSe.fromBytes(
								IOUtils.decompress(c.getValue())));
					} catch (Exception e) {
						//TODO: logger.error("Decompression of message body failed: ", e);
					}
				} else if (includeBody && c.getName().equals(CN_PLAIN_BODY)) {
					try {
						message.setPlainBody(strSe.fromBytes(
								IOUtils.decompress(c.getValue())));
					} catch (Exception e) {
						//TODO: logger.error("Decompression of message body failed: ", e);
					}
				} else if (c.getName().equals(CN_PARTS)) {
					Map<String, MimePart> parts = null;
					parts = JSONUtils.toObject(c.getValue(), parts);
					message.setParts(parts);
		        }
			}
		}

		return message;
	}

	/**
	 * Marshall the {@link Message} object contents to Cassandra columns
	 * 
	 * @param m Message to marshall
	 * @return
	 * @throws IOException
	 */
	protected static List<HColumn<String, byte[]>> marshall(final Message m)
			throws IOException
	{
		Map<String, Object> columns = new HashMap<String, Object>();

		if (m.getSize() != null) {
			columns.put(CN_SIZE, m.getSize());
		}

		if (m.getDate() != null) {
			columns.put(CN_DATE, m.getDate());
		}

		if (m.getFrom() != null) {
			columns.put(CN_FROM, serializeAddress(m.getFrom()));
		}

		if (m.getTo() != null) {
			columns.put(CN_TO, serializeAddress(m.getTo()));
		}

		if (m.getCc() != null) {
			columns.put(CN_CC, serializeAddress(m.getCc()));
		}

		if (m.getBcc() != null) {
			columns.put(CN_BCC, serializeAddress(m.getBcc()));
		}

		if (m.getReplyTo() != null) {
			columns.put(CN_REPLY_TO, serializeAddress(m.getReplyTo()));
		}

		if (m.getMessageId() != null) {
			columns.put(CN_MESSAGE_ID, m.getMessageId());
		}

		if (m.getSubject() != null) {
			columns.put(CN_SUBJECT, m.getSubject());
		}

		if (m.getLocation() != null) {
			columns.put(CN_BRI, m.getLocation().toString());
		}

		if (m.getParts() != null) {
			columns.put(CN_PARTS, JSONUtils.fromObject(m.getParts()));
		}

		// add markers
		if (!m.getMarkers().isEmpty())
		{
			for (Marker marker : m.getMarkers())
			{
				String cn = CN_MARKER_PREFIX + marker.toInt();
				columns.put(cn, new byte[0]);
			}
		}

		// add labels
		if (!m.getLabels().isEmpty())
		{
			for (Integer labelId : m.getLabels())
			{
				String cn = CN_LABEL_PREFIX + labelId;
				columns.put(cn, new byte[0]);
			}
		}

		// add HTML message text
		// Fallback: if HTML is enabled but not available, store PLAIN message text 
		if (Configurator.isStoreHtmlWithMetadata())
		{
			if (m.getHtmlBody() != null) {
				columns.put(CN_HTML_BODY, IOUtils.compress(m.getHtmlBody()));
			} else if (!Configurator.isStorePlainWithMetadata() && (m.getPlainBody() != null)) {
				columns.put(CN_PLAIN_BODY, IOUtils.compress(m.getPlainBody()));
			}
		}

		// add PLAIN message text
		if (Configurator.isStorePlainWithMetadata() && (m.getPlainBody() != null)) {
			columns.put(CN_PLAIN_BODY, IOUtils.compress(m.getPlainBody()));
		}

		return mapToHColumns(columns);
	}

	/**
	 * Serialize {@link AddressList} to JSON
	 * 
	 * @param addresses
	 * @return
	 */
	private static byte[] serializeAddress(final AddressList addresses)
	{
		List<String[]> result = new ArrayList<String[]>();

		for (Address address : addresses)
		{
			result.add(new String[]{
					(address.getName() == null ? "" : address.getName()),
					(address.getAddress() == null ? "" : address.getAddress()) });
		}

		return JSONUtils.fromObject(result);
	}

	/**
	 * Unserialize JSON sting to {@link AddressList}
	 * 
	 * @param val
	 * @return
	 */
	private static AddressList unserializeAddress(final byte[] val)
	{
		List<Address> result = new ArrayList<Address>();

		List<List<String>> addresses = null;
		addresses = JSONUtils.toObject(val, addresses);

		for (List<String> address : addresses) {
			result.add(new Address(address.get(0), address.get(1)));
		}

		return new AddressList(result);
	}

	/**
	 * Convert Map of arbitrary Objects to the List of HColumns serialized as
	 * <code>byte[]</code>
	 * 
	 * @param map
	 * @return
	 */
	protected static List<HColumn<String, byte[]>> mapToHColumns(
			final Map<String, Object> map)
	{
		List<HColumn<String, byte[]>> columns = 
			new ArrayList<HColumn<String, byte[]>>(map.size());

		for (Map.Entry<String, Object> a : map.entrySet())
		{
			columns.add(createColumn(a.getKey(), SerializerTypeInferer
					.getSerializer(a.getValue()).toBytes(a.getValue()),
					strSe, byteSe));
		}
		
		return columns;
	}
}
