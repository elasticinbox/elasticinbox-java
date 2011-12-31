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
