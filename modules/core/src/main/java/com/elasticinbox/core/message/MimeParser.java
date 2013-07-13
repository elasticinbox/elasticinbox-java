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

package com.elasticinbox.core.message;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMessage.RecipientType;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.ParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elasticinbox.common.utils.Assert;
import com.elasticinbox.core.model.Address;
import com.elasticinbox.core.model.AddressList;
import com.elasticinbox.core.model.Message;
import com.elasticinbox.core.model.MimePart;
import com.google.common.io.CharStreams;
import com.google.common.io.LimitInputStream;

public final class MimeParser
{
	public final static String MIME_HEADER_SPAM = "X-Spam-Flag";

	/** Read 100K if cannot parse body */ 
	private final static int MAX_UNPARSED_PART_SIZE = 50 * 1024;
	/** Used when encoding is unknown */
	private final static String DEFAULT_ENCODING = "ISO-8859-1";
	private static Properties props = new Properties();

	private Message message;
	private MimeMessage mimeMessage;

    private StringBuilder textBody = new StringBuilder();
    private StringBuilder htmlBody = new StringBuilder();

	private static final Logger logger = LoggerFactory
			.getLogger(MimeParser.class);

	static {
		// Make JavaMail parser more error tolerant
		// see http://javamail.kenai.com/nonav/javadocs/javax/mail/internet/package-summary.html#package_description
		props.setProperty("mail.mime.address.strict", "false");
		props.setProperty("mail.mime.decodetext.strict", "false");
		props.setProperty("mail.mime.decodefilename", "true");
		props.setProperty("mail.mime.decodeparameters", "true");
		props.setProperty("mail.mime.charset", "utf-8");
		props.setProperty("mail.mime.parameters.strict", "false");
		props.setProperty("mail.mime.base64.ignoreerrors", "true");
		props.setProperty("mail.mime.uudecode. ignoreerrors", "true");
		props.setProperty("mail.mime.uudecode.ignoremissingbeginend", "true");
		props.setProperty("mail.mime.multipart.allowempty", "true");
		props.setProperty("mail.mime.ignoreunknownencoding", "true");
		props.setProperty("mail.mime.ignoremultipartencoding", "false");
		props.setProperty("mail.mime.allowencodedmessages", "true");

		// Some of JavaMail properties should be set on System level 
		for (Iterator<Object> iter = props.keySet().iterator(); iter.hasNext();) {
			String key = (String) iter.next();
			System.setProperty(key, (String) props.get(key));
		}
	}

	public MimeParser() {
		//
	}

	public MimeParser(InputStream in) throws IOException, MimeParserException {
		parse(in);
	}

	/**
	 * Parse {@link InputStream} into {@link Message} structure
	 * 
	 * @param in
	 * @throws IOException
	 * @throws MimeParserException
	 */
	public void parse(InputStream in) throws IOException, MimeParserException
	{
		this.message = new Message();
		Session session = Session.getDefaultInstance(props);

		try {
			this.mimeMessage = new MimeMessage(session, in);
			this.message.setFrom(getAddressList(mimeMessage.getFrom()));
			this.message.setTo(getAddressList(mimeMessage.getRecipients(RecipientType.TO)));
			this.message.setCc(getAddressList(mimeMessage.getRecipients(RecipientType.CC)));
			this.message.setBcc(getAddressList(mimeMessage.getRecipients(RecipientType.BCC)));
			this.message.setSubject(mimeMessage.getSubject());
			this.message.setMessageId(mimeMessage.getMessageID());
			this.message.setDate(mimeMessage.getSentDate());
			//this.message.setSize((long) mimeMessage.getSize());

			// extract necessary minor headers
			// TODO: This should be replaced by filters in future
			message.addMinorHeader(MIME_HEADER_SPAM, mimeMessage.getHeader(MIME_HEADER_SPAM, null));

			// extract mime parts and body
			parseMessagePart(mimeMessage, "");
		} catch (MessagingException e) {
			logger.error("Unable to parse MIME message: ", e);
			throw new MimeParserException(e.getMessage());
		}

		if (this.htmlBody.length() > 0) {
			message.setHtmlBody(this.htmlBody.toString());
		}

		if (this.textBody.length() > 0) {
			message.setPlainBody(this.textBody.toString());
		}
	}

	public Message getMessage() throws IOException {
		return message;
	}

	/**
	 * Get InputStream of MIME part identified by Part ID
	 * 
	 * @param contentId
	 * @return
	 * @throws MimeParserException
	 */
	public InputStream getInputStreamByPartId(String partId)
			throws MimeParserException
	{
		Assert.notNull(this.mimeMessage, "No message was processed. Initialize first.");
		message.getPart(partId); // make sure that part exists, otherwise IAE will be thrown

		MimeMultipart mp;
		Object content;
		InputStream in = null;

		// find based on Part ID eg. 1.2.3
		try {
			mp = (MimeMultipart) this.mimeMessage.getContent();
			String[] partNums = partId.split("\\.");

			// loop through parts to reach the final part
			for (int i = 0; i < partNums.length; i++)
			{
				int localPartId = Integer.parseInt(partNums[i]) - 1;
				content = mp.getBodyPart(localPartId).getContent();

				if (content instanceof MimeMultipart) {
					mp = (MimeMultipart) content;
				} else if ((content instanceof String)
						|| (content instanceof InputStream)
						|| (content instanceof MimeMessage)) {
					in = mp.getBodyPart(localPartId).getInputStream();
				} else {
					// normally, we should never get here
					// perhaps bad Part ID
					throw new MessagingException("MIME part not found");
				}
			}
		} catch (IOException e) {
			throw new MimeParserException("Unable to extract attachment from the message: " + e.getMessage());
		} catch (MessagingException e) {
			throw new IllegalArgumentException("Message does not contain part with ID " + partId);
		}

		return in;
	}

	/**
	 * Get InputStream of MIME part identified by Content-ID
	 * 
	 * @param contentId
	 * @return
	 * @throws MimeParserException
	 */
	public InputStream getInputStreamByContentId(String contentId)
			throws MimeParserException
	{
		Assert.notNull(this.mimeMessage, "No message was processed. Initialize first.");
		
		// lookup part ID and make sure that part exists. IAE will be thrown otherwise.
		String partId = message.getPartByContentId(contentId).getPartId();

		return getInputStreamByPartId(partId);
	}

	/**
	 * Recursively walk through parsed MIME message and extract parts info
	 * 
	 * @throws IOException
	 * @throws MessagingException
	 */
	private void parseMessagePart(Part part, String partId) throws IOException,
			MessagingException
	{
		Object content = null;

		// decode part
		try {
			content = part.getContent();
		} catch (UnsupportedEncodingException uee) {
			// TODO: make better handling of unsupported encodings, perhaps using jcharset detector
			if (part.isMimeType("text/*")) {
				// decode text using default encoding for all unknown encodings
				logger.warn("Parser detected unsupported encoding: {}. Will try decoding with {}", uee.getMessage(), DEFAULT_ENCODING);
				InputStream in = part.getInputStream();
				content = CharStreams.toString(new InputStreamReader(in, DEFAULT_ENCODING));
			} else {
				logger.error("Parser detected unsupported encoding: {}. Unparsed part will be used.", uee.getMessage());
				content = readUnparsedPart(part.getInputStream());
			}
		} catch (Exception e) {
			// Content-Type is malformed if we got here
			logger.warn("Unable to parse Content-Type. Thrown by {}: {}. Unparsed part will be used.",
					e.getClass(), e.getMessage());
			content = readUnparsedPart(part.getInputStream());
		}

		logger.debug("Parsing part {} with mime type {}.",
				(partId.isEmpty()) ? "message" : partId, part.getContentType());

		if (content instanceof String) {
			// simple part with text
			
			String dis = null;

			try {
				dis = part.getDisposition();
			} catch (ParseException e) {
				// if parsing of disposition string failed, assume part an attachment
				dis = Part.ATTACHMENT; 
			}

			logger.trace("MIME parser extracted TEXT part: {}", (String) content);

			if ((dis != null) && dis.equals(Part.ATTACHMENT)) {
				// add text part as attachment
				message.addPart(partId, new MimePart(part));
			} else {
				// if no disposition, then add to message body
				if(part.isMimeType("text/html")) {
					htmlBody.append((String) content);
				} else {
					textBody.append((String) content);
				}
			}
		} else if (content instanceof MimeMultipart) {
			MimeMultipart multipart = (MimeMultipart) content;

			for (int i = 0; i <  multipart.getCount(); i++)
			{
				// build next part id
				StringBuilder nextPartId = new StringBuilder(partId);

				// add period if not at root level
				if (!partId.isEmpty())
					nextPartId.append(".");

				int localPartId = i+1; // IMAPv4 MIME part counter starts from 1
				nextPartId.append(localPartId);

				Part nextPart = multipart.getBodyPart(i);
				parseMessagePart(nextPart, nextPartId.toString());
			}
		} else if ((content instanceof InputStream)
				|| (content instanceof MimeMessage)) {
			// binary, message/rfc822 or text attachment
			message.addPart(partId, new MimePart(part));
		} else {
			throw new MessagingException("Unkonwn message part type " + content.getClass().getName());
		}
	}

	/**
	 * Get AddressList from JavaMail Address array
	 * 
	 * @param mailboxes MailboxList
	 * @return AddressList
	 * @throws IllegalArgumentException
	 */
	private static AddressList getAddressList(javax.mail.Address[] al)
			throws IllegalArgumentException
	{
		if (al == null)
			return null;

		ArrayList<Address> addresses = new ArrayList<Address>();
		
		for (int i = 0; i < al.length; i++) {
			InternetAddress ia = (InternetAddress) al[i];
			Address a = new Address(ia.getPersonal(), ia.getAddress());
			addresses.add(a);
		}

		return new AddressList(addresses);
	}

	/**
	 * Read unparsed part.
	 * 
	 * @param in
	 * @return
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 */
	private static String readUnparsedPart(InputStream in)
			throws UnsupportedEncodingException, IOException
	{
		return CharStreams.toString(new InputStreamReader(new LimitInputStream(
				in, MAX_UNPARSED_PART_SIZE), DEFAULT_ENCODING));
	}
}
