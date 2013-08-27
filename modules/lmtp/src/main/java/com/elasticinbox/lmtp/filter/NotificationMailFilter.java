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

import com.elasticinbox.core.model.Message;
import com.elasticinbox.core.model.ReservedLabels;

/**
 * This filter detects social network notification messages and stores then
 * under Notifications label to keep Inbox clean.
 * 
 * @author Rustam Aliyev
 */
public final class NotificationMailFilter implements Filter<Message>
{
	private static final Logger logger = LoggerFactory
			.getLogger(NotificationMailFilter.class);

	@Override
	public Message filter(Message message)
	{
		try {
			String from = message.getFrom().get(0).getAddress();

			if (from.matches("(.+)\\+(.+)\\@facebookmail\\.com")
					|| from.matches("(.+)\\@facebookappmail\\.com")
					|| from.matches("(.+)\\@pages\\.facebookmail\\.com")
					|| from.matches("bezotveta\\@odnoklassniki\\.ru")
					|| from.matches("admin\\@vkontakte\\.ru")
					|| from.matches("admin\\@notify\\.vk\\.com")
					|| from.matches("(.+)\\@postmaster\\.twitter\\.com")
					|| from.matches("(.+)\\@bounce\\.twitter\\.com")
					|| from.matches("info\\@twitter\\.com")
					|| from.matches("notify\\@twitter\\.com")
					|| from.matches("(.+)\\@email\\.foursquare\\.com")
					|| from.matches("(.+)\\@email\\.pinterest\\.com")
					|| from.matches("(.+)\\@bounce\\.linkedin\\.com")
					|| from.matches("(.+)\\@linkedin\\.com")
					|| from.matches("noreply\\-(.+)\\@plus\\.google\\.com")
					|| from.matches("notifications\\@meetmemail\\.com")
					|| from.matches("notification\\+(.+)\\@netlogmail\\.com")
					|| from.matches("notifications\\+(.+)\\@zyngamail\\.com"))
			{
				logger.debug("Applying filter for NOTIFICATIONS");
				message.addLabel(ReservedLabels.NOTIFICATIONS.getId());
			}
		} catch (Exception ex) {
			logger.info("notification type regex parsing error");
		}

		return message;
	}
}