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

package com.elasticinbox.lmtp.server.api;

/**
 * Standard and extended LMTP status codes
 * 
 * @author Rustam Aliyev
 * @see <a href="http://tools.ietf.org/html/rfc3463">RFC3463</a>
 */
public enum LMTPReply
{
	OK(250, "2.0.0", "OK"),
	SENDER_OK(250, "2.0.0", "Sender OK"),
	RECIPIENT_OK(250, "2.1.5", "Recipient OK"),
	DELIVERY_OK(250, "2.1.5", "Delivery OK"),
	OK_TO_SEND_DATA(354, null, "End data with <CR><LF>.<CR><LF>"),
	USE_RCPT_INSTEAD(252, "2.3.3", "Use RCPT to deliver messages"),

	BYE(221, null, "Bye"),
	GREETING(220, null, "%s ElasticInboxLDA %s LMTP ready"),
	TIMEOUT(421, null, "Timeout waiting for data from client"),

	TOO_MANY_CONNECTIONS(421, "4.2.1", "Too many concurrent connections. Please try again later."),
	SERVICE_DISABLED(421, "4.3.2", "Service not available, closing transmission channel"),
	MAILBOX_DISABLED(450, "4.2.1", "Mailbox disabled, not accepting messages"),
	MAILBOX_NOT_ON_THIS_SERVER(450, "4.2.0", "Mailbox is not on this server"),
	TEMPORARY_CONNECTION_FAILURE(450, "4.0.0", "Problem when connecting. Please try again later."),
	TEMPORARY_FAILURE(451, "4.0.0", "Temporary message delivery failure try again"),
	TEMPORARY_FAILURE_OVER_QUOTA(452, "4.2.2", "Over quota"),
	TOO_MANY_RECIPIENTS(452, "4.5.3", "Too many recipients"),

	SYNTAX_ERROR(500, "5.5.2", "Syntax error"),
	INVALID_RECIPIENT_ADDRESS(500, "5.5.2", "Syntax error in recipient address"),
	INVALID_SENDER_ADDRESS(501, "5.5.4", "Syntax error in sender address"),
	INVALID_BODY_PARAMETER(501, "5.5.4", "Syntax error in BODY parameter"),
	INVALID_SIZE_PARAMETER(501, "5.5.4", "Syntax error in SIZE parameter"),
	INVALID_LHLO_PARAMETER(501, "5.5.4", "Syntax error in LHLO parameter"),
	NESTED_MAIL_COMMAND(503, "5.5.1", "Nested MAIL command"),
	NO_RECIPIENTS(503, "5.5.1", "Need RCPT command"),
	MISSING_MAIL_TO(503, "5.5.1", "Need MAIL command"),

	NO_SUCH_USER(550, "5.1.1", "No such user here"),
	PERMANENT_FAILURE_OVER_QUOTA(552, "5.2.2", "Over quota"),
	PERMANENT_FAILURE_TOO_MUCH_DATA(552, "5.2.2", "Too much mail data"),
	PERMANENT_FAILURE(554, "5.0.0",	"Permanent message delivery failure");

	private int mCode;
	private String mEnhancedCode;
	private String mDetail;
	private DetailCB mDetailCallback;

	private abstract static class DetailCB {
		protected abstract String detail();
	}

	private LMTPReply(int code, String enhancedCode, String detail)
	{
		mCode = code;
		mEnhancedCode = enhancedCode;
		mDetail = detail;
	}

	private LMTPReply(int code, String enhancedCode, DetailCB detail)
	{
		mCode = code;
		mEnhancedCode = enhancedCode;
		mDetailCallback = detail;
	}

	public String toString()
	{
		String detail;

		if (mDetailCallback != null) {
			detail = mDetailCallback.detail();
		} else {
			detail = mDetail;
		}

		if (mEnhancedCode == null) {
			return mCode + " " + detail;
		} else {
			return mCode + " " + mEnhancedCode + " " + detail;
		}
	}

	public boolean success() {
		return (mCode > 199 && mCode < 400) ? true : false;
	}
}
