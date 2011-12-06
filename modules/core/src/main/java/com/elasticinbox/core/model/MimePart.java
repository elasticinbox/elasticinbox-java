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

import java.io.IOException;
import java.io.InputStream;

import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.ContentType;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elasticinbox.common.utils.IOUtils;

/**
 * Representation of MIME part which is referenced from and belong to MIME
 * {@link Message}.
 * 
 * @author Rustam Aliyev
 * @see {@link Message}
 */
public class MimePart
{
	private final static String UNKNOWN_FILENAME = "unknown.file"; // used for bad filenames
	private final static String UNKNOWN_MIMETYPE = "application/octet-stream";

	private String partId;
	private final String contentId;
	private final String mimeType;
	private final String fileName;
	private final String disposition;
	private final Long size;

	private static final Logger logger = LoggerFactory
			.getLogger(MimePart.class);

	public MimePart(final Part part) throws MessagingException, IOException
	{
		String contentId = null;
		String disposition = null;
		String filename = UNKNOWN_FILENAME;
		String mimeType = UNKNOWN_MIMETYPE;

		this.size = getMimePartSize(part);

		// get filename if any
		try {
			filename = part.getFileName();
		} catch (Exception e) {
			logger.warn("parser was unable to decode not well-formed Content-Disposition params: {}", e.getMessage());
		} finally {
			this.fileName = filename;
		}

		// get mime type
		try {
			ContentType cType = new ContentType(part.getContentType());
			mimeType = cType.getBaseType();
		} catch (Exception e) {
			logger.warn("parser was unable to decode not well-formed Content-Type: {}", e.getMessage());
		} finally {
			this.mimeType = mimeType;
		}

		// get content-id
		try {
			contentId = part.getHeader("Content-ID")[0];
			contentId = contentId.substring(1, contentId.length() - 1); // remove <...> brackets
		} catch (Exception e) {
			// no contentId, ignore, will remain null
		} finally {
			this.contentId = contentId;
		}
		
		// get disposition
		try {
			disposition = part.getDisposition();
		} catch (Exception e) {
			logger.warn("parser was unable to decode not well-formed Content-Disposition: {}", e.getMessage());
		} finally {
			this.disposition = disposition;
		}

	}

	public String getContentId() {
		return this.contentId;
	}

	public String getMimeType() {
		return this.mimeType;
	}

	public String getFileName() {
		return this.fileName;
	}

	public long getSize() {
		return this.size;
	}

	public String getDisposition() {
		return this.disposition;
	}

	@JsonIgnore
	public String getPartId() {
		return partId;
	}
	
	public void setPartId(String partId) {
		this.partId = partId;
	}

	/**
	 * Calculate attachment size. {@link javax.mail.Part#getSize()} is not
	 * useful since it returns raw (encoded) size of the part. This method
	 * returns more accurate, yet estimated size. In case it was impossible to
	 * get estimate size, it will go through the stream and count bytes.
	 * 
	 * @param part
	 * @return
	 * @throws IOException
	 */
	private long getMimePartSize(final Part part) throws IOException
	{
		long size = 0L;

		try {
			// get available bytes in input stream
			InputStream in = part.getInputStream();
			Integer inSize = in.available();
			
			if (inSize == null || inSize < 1)
				size = IOUtils.getInputStreamSize(in);
			else
				size = inSize;

			in.close();
		} catch (Exception e) {
			// ignore, size will be 0
		}

		return size;
	}
	
}
