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
