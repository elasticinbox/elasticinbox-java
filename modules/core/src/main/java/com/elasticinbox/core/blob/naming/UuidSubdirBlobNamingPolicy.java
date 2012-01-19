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

package com.elasticinbox.core.blob.naming;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elasticinbox.common.utils.Assert;

/**
 * Generate unique Blob name based on the mailbox name and message UUID. Blobs
 * are equally distributed in the tree of 2^16 subdirectories.
 * 
 * <p>Distribution is based on MD5 hash and avoids storing all Blobs in the same
 * directory (mostly relevant for filesystem type blob stores).
 * 
 * @author Rustam Aliyev
 */
public final class UuidSubdirBlobNamingPolicy extends AbstractBlobNamingPolicy
{
	private final static Logger logger = 
			LoggerFactory.getLogger(UuidSubdirBlobNamingPolicy.class);

	@Override
	public String getBlobName(BlobNameBuilder builder)
	{
		Assert.notNull(builder.messageId, "message id cannot be null");
		Assert.notNull(builder.mailbox, "mailbox cannot be null");
		
		String uniqId = new StringBuilder(builder.mailbox.getId()).append(":").append(builder.messageId).toString();
		String path = "00" + File.separator + "00";

		MessageDigest md5;
		try {
			md5 = MessageDigest.getInstance("MD5");
			final byte[] digestBytes = md5.digest(uniqId.getBytes("UTF-8"));
			path = new StringBuilder(byteToHex(digestBytes[0]))
					.append(File.separator)
					.append(byteToHex(digestBytes[1]))
					.toString();
		} catch (NoSuchAlgorithmException e) {
			logger.error("Can't calculate digest, fallback to the default path '00/00': ", e);
		} catch (UnsupportedEncodingException e) {
			logger.error("Can't calculate digest, fallback to the default path '00/00': ", e);
		}

		return path + File.separator + uniqId;
	}

	private final static String byteToHex(byte b)
	{
		StringBuilder hexString = new StringBuilder();
		String hex = Integer.toHexString(0xFF & b);
		if(hex.length() == 1) {
			hexString.append('0');
		}
		hexString.append(hex);
		return hexString.toString();
	}
}
