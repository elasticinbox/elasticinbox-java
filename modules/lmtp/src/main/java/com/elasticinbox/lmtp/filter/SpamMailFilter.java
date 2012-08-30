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

package com.elasticinbox.lmtp.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elasticinbox.core.message.MimeParser;
import com.elasticinbox.core.model.Message;
import com.elasticinbox.core.model.ReservedLabels;

/**
 * Labels message as Spam if respective header is found.
 * 
 * @author Rustam Aliyev
 */
public final class SpamMailFilter implements Filter<Message>
{
	private static final Logger logger = LoggerFactory
			.getLogger(SpamMailFilter.class);

	private final static String MIME_HEADER_SPAM_VALUE = "YES";

	@Override
	public Message filter(Message message)
	{
		if (message.getMinorHeader(MimeParser.MIME_HEADER_SPAM) != null
				&& message.getMinorHeader(MimeParser.MIME_HEADER_SPAM).equalsIgnoreCase(MIME_HEADER_SPAM_VALUE))
		{
			logger.debug("Applying filter for SPAM");
			message.addLabel(ReservedLabels.SPAM.getId());
		}

		return message;
	}
}
