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

package com.elasticinbox.itests;

import static org.ops4j.pax.exam.CoreOptions.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.net.pop3.POP3Client;
import org.apache.commons.net.pop3.POP3MessageInfo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.EagerSingleStagedReactorFactory;

import com.elasticinbox.core.model.ReservedLabels;
import com.elasticinbox.core.utils.Base64UUIDUtils;
import com.google.common.collect.ObjectArrays;

/**
 * Integration test for POP3.
 * 
 * Some parts of these code are taken from Apache JAMES Protocols project.
 * 
 * @author Rustam Aliyev
 */
@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(EagerSingleStagedReactorFactory.class)
public class Pop3IT extends AbstractIntegrationTest
{
	private final static int POP3_PORT = 2110;
	private final static String POP3_HOST = "localhost";

	/**
	 * Append POP3 Specific config options
	 * 
	 * @return
	 */
	@Configuration()
	public Option[] config()
	{
		return ObjectArrays.concat(super.config(), options(
				// POP3 Test Bundles
				mavenBundle().groupId("commons-net").artifactId("commons-net").versionAsInProject(),
				mavenBundle().groupId("org.apache.james.protocols").artifactId("protocols-netty").versionAsInProject(),
				mavenBundle().groupId("org.apache.james.protocols").artifactId("protocols-api").versionAsInProject(),
				mavenBundle().groupId("org.apache.james.protocols").artifactId("protocols-pop3").versionAsInProject(),
				mavenBundle().groupId("io.netty").artifactId("netty").versionAsInProject(),

				// ElasticInbox Bundles
				scanDir("../modules/pop3/target/")
			), Option.class);
	}

	@Test
	public void testListUidl() throws IOException
	{
		initAccount();

		// load messages with POP3 label
		long mailSizeRegular = getResourceSize(EMAIL_REGULAR);
		long mailSizeAttach = getResourceSize(EMAIL_LARGE_ATT);

		Map<String, UUID> messages = new HashMap<String, UUID>(2);
		Integer labelId = ReservedLabels.POP3.getId();

		messages.put("headers", RestV2IT.addMessage(EMAIL_REGULAR, labelId));
		messages.put("attach", RestV2IT.addMessage(EMAIL_LARGE_ATT, labelId));

		// initialize POP3 client
		POP3Client client = new POP3Client();
		client.connect(POP3_HOST, POP3_PORT);

		boolean loginSuccess = client.login(TEST_ACCOUNT, TEST_PASSWORD);
		assertThat(loginSuccess, is(true));

		// LIST all messages
		POP3MessageInfo[] info = client.listMessages();
		assertThat(info.length, equalTo(2));
		assertThat((int) mailSizeAttach, equalTo(info[0].size));
		assertThat((int) mailSizeRegular, equalTo(info[1].size));
		assertThat(info[0].number, equalTo(1));
		assertThat(info[1].number, equalTo(2));

		// LIST one message
		POP3MessageInfo msgInfo = client.listMessage(1);
		assertThat((int) mailSizeAttach, equalTo(msgInfo.size));
		assertThat(msgInfo.number, equalTo(1));

		// LIST message that does not exist
		msgInfo = client.listMessage(10);
		assertThat(msgInfo, nullValue());

		// UIDL all messages
		info = client.listUniqueIdentifiers();
		assertThat(info.length, equalTo(2));
		assertThat(info[0].identifier,
				equalTo(Base64UUIDUtils.encode(messages.get("attach"))));
		assertThat(info[1].identifier,
				equalTo(Base64UUIDUtils.encode(messages.get("headers"))));
		assertThat(info[0].number, equalTo(1));
		assertThat(info[1].number, equalTo(2));

		// UIDL one message
		msgInfo = client.listUniqueIdentifier(1);
		assertThat(msgInfo.identifier,
				equalTo(Base64UUIDUtils.encode(messages.get("attach"))));
		assertThat(msgInfo.number, equalTo(1));

		// UIDL message that does not exist
		msgInfo = client.listUniqueIdentifier(10);
		assertThat(msgInfo, nullValue());

		boolean logoutSuccess = client.logout();
		assertThat(logoutSuccess, is(true));
	}

	@Test
	public void testDele() throws IOException
	{
		initAccount();

		Map<String, UUID> messages = new HashMap<String, UUID>(2);
		Integer labelId = ReservedLabels.POP3.getId();

		messages.put("headers", RestV2IT.addMessage(EMAIL_REGULAR, labelId));
		messages.put("attach", RestV2IT.addMessage(EMAIL_LARGE_ATT, labelId));

		// initialize POP3 client
		POP3Client client = new POP3Client();
		client.connect(POP3_HOST, POP3_PORT);

		// Login
		boolean loginSuccess = client.login(TEST_ACCOUNT, TEST_PASSWORD);
		assertThat(loginSuccess, is(true));

		// LIST all messages
		POP3MessageInfo[] info = client.listMessages();
		assertThat(info.length, equalTo(2));

		// DELE message 1
		boolean deleteResult = client.deleteMessage(1);
		assertThat(deleteResult, is(true));

		// LIST remaining
		info = client.listMessages();
		assertThat(info.length, equalTo(1));

		// DELE message 1 again
		deleteResult = client.deleteMessage(1);
		assertThat(deleteResult, is(false));
		
		// LIST remaining
		info = client.listMessages();
		assertThat(info.length, equalTo(1));

		// DELE message 2
		deleteResult = client.deleteMessage(2);
		assertThat(deleteResult, is(true));

		info = client.listMessages();
		assertThat(info.length, equalTo(0));

		// QUIT so the messages get expunged
		boolean logoutSuccess = client.logout();
		assertThat(logoutSuccess, is(true));

		// reconnect
		client.connect(POP3_HOST, POP3_PORT);

		// Login
		loginSuccess = client.login(TEST_ACCOUNT, TEST_PASSWORD);
		assertThat(loginSuccess, is(true));

		info = client.listMessages();
		assertThat(info.length, equalTo(0));

		logoutSuccess = client.logout();
		assertThat(logoutSuccess, is(true));
	}
	
	@Test
	public void testRset() throws IOException
	{
		initAccount();

		Integer labelId = ReservedLabels.POP3.getId();
		RestV2IT.addMessage(EMAIL_REGULAR, labelId);

		// initialize POP3 client
		POP3Client client = new POP3Client();
		client.connect(POP3_HOST, POP3_PORT);

		// Login
		boolean loginSuccess = client.login(TEST_ACCOUNT, TEST_PASSWORD);
		assertThat(loginSuccess, is(true));

		// LIST all messages
		POP3MessageInfo[] info = client.listMessages();
		assertThat(info.length, equalTo(1));

		// DELE message 1
		boolean deleteResult = client.deleteMessage(1);
		assertThat(deleteResult, is(true));

		// LIST remaining
		info = client.listMessages();
		assertThat(info.length, equalTo(0));

		// RSET. After this the deleted mark should be removed again
		boolean resetRestult = client.reset();
		assertThat(resetRestult, is(true));

		// LIST all messages
		info = client.listMessages();
		assertThat(info.length, equalTo(1));

		// Logout
		boolean logoutSuccess = client.logout();
		assertThat(logoutSuccess, is(true));
	}
	
	@Test
	public void testStat() throws IOException
	{
		initAccount();

		// load messages with POP3 label
		long mailSizeRegular = getResourceSize(EMAIL_REGULAR);
		long mailSizeAttach = getResourceSize(EMAIL_LARGE_ATT);

		Integer labelId = ReservedLabels.POP3.getId();
		RestV2IT.addMessage(EMAIL_REGULAR, labelId);
		RestV2IT.addMessage(EMAIL_LARGE_ATT, labelId);

		// initialize POP3 client
		POP3Client client = new POP3Client();
		client.connect(POP3_HOST, POP3_PORT);

		boolean loginSuccess = client.login(TEST_ACCOUNT, TEST_PASSWORD);
		assertThat(loginSuccess, is(true));

		POP3MessageInfo info = client.status();
		assertThat(info.size, equalTo((int) (mailSizeRegular + mailSizeAttach)));
		assertThat(info.number, equalTo(2));

		// Logout
		boolean logoutSuccess = client.logout();
		assertThat(logoutSuccess, is(true));
	}
	
	@Test
	public void testRetr() throws IOException
	{
		initAccount();

		Integer labelId = ReservedLabels.POP3.getId();
		RestV2IT.addMessage(EMAIL_SIMPLE, labelId);

		// initialize POP3 client
		POP3Client client = new POP3Client();
		client.connect(POP3_HOST, POP3_PORT);

		// Login
		boolean loginSuccess = client.login(TEST_ACCOUNT, TEST_PASSWORD);
		assertThat(loginSuccess, is(true));

		// RETR message
		Reader retrMessage = client.retrieveMessage(1);
		assertThat(retrMessage, notNullValue());

		checkMessage(EMAIL_SIMPLE, retrMessage);
		retrMessage.close();
		
        // RETR non-existing message
		retrMessage = client.retrieveMessage(10);
        assertThat(retrMessage, nullValue());
        
        // Delete and RETR the message again
		boolean deleteResult = client.deleteMessage(1);
		assertThat(deleteResult, is(true));
		
		retrMessage = client.retrieveMessage(1);
        assertThat(retrMessage, nullValue());
	}
	
	@Test
	public void testTop() throws IOException
	{
		initAccount();

		Integer labelId = ReservedLabels.POP3.getId();
		RestV2IT.addMessage(EMAIL_SIMPLE, labelId);

		// initialize POP3 client
		POP3Client client = new POP3Client();
		client.connect(POP3_HOST, POP3_PORT);

		// Login
		boolean loginSuccess = client.login(TEST_ACCOUNT, TEST_PASSWORD);
		assertThat(loginSuccess, is(true));

		// TOP message with all lines
		Reader topMessage = client.retrieveMessageTop(1, 1000);
		assertThat(topMessage, notNullValue());
		checkMessage(EMAIL_SIMPLE, topMessage);
		topMessage.close();

		// TOP message with 5 lines
		topMessage = client.retrieveMessageTop(1, 5);
		assertThat(topMessage, notNullValue());
		checkMessage(EMAIL_SIMPLE, topMessage, 5);
		topMessage.close();

		// TOP non-existing message
		topMessage = client.retrieveMessageTop(10, 10);
		assertThat(topMessage, nullValue());

		// Delete and TOP the message again
		boolean deleteResult = client.deleteMessage(1);
		assertThat(deleteResult, is(true));

		topMessage = client.retrieveMessageTop(1, 1000);
		assertThat(topMessage, nullValue());
	}

	private void checkMessage(String message, Reader reader) throws IOException {
		checkMessage(message, reader, null);
	}

	private void checkMessage(String message, Reader reader, Integer limit) throws IOException
	{
		Reader orig;

		if (limit != null) {
			orig = new InputStreamReader(new CountingBodyInputStream(
					this.getClass().getResourceAsStream(message), limit), "US-ASCII");
		} else {
			orig = new InputStreamReader(this.getClass().getResourceAsStream(message), "US-ASCII");
		}

		int i = -1;
		int j = -1;

		while (((i = reader.read()) != -1) && ((j = orig.read()) != -1)) {
			assertThat(j, equalTo(i));
		}

		// read final byte and assert
		j = orig.read();
		assertThat(j, equalTo(i));

		orig.close();
	}

	/**
	 * This {@link InputStream} implementation can be used to return all message
	 * headers and limit the body lines which will be read from the wrapped
	 * {@link InputStream}.
	 */
	private final class CountingBodyInputStream extends InputStream
	{
		private int count = 0;
		private int limit = -1;
		private int lastChar;
		private InputStream in;
		private boolean isBody = false; // starting from header
		private boolean isEmptyLine = false;

		/**
		 * 
		 * @param in
		 *            InputStream to read from
		 * @param limit
		 *            the lines to read. -1 is used for no limits
		 */
		public CountingBodyInputStream(InputStream in, int limit) {
			this.in = in;
			this.limit = limit;
		}

		@Override
		public int read() throws IOException
		{
			if (limit != -1) {
				if (count <= limit) {
					int a = in.read();

					// check for empty line
					if (!isBody && isEmptyLine && lastChar == '\r' && a == '\n') {
						// reached body
						isBody = true;
					}

					if (lastChar == '\r' && a == '\n') {
						// reset empty line flag
						isEmptyLine = true;

						if (isBody) {
							count++;
						}
					} else if (lastChar == '\n' && a != '\r') {
						isEmptyLine = false;
					}

					lastChar = a;

					return a;
				} else {
					return -1;
				}
			} else {
				return in.read();
			}

		}

		@Override
		public long skip(long n) throws IOException {
			return in.skip(n);
		}

		@Override
		public int available() throws IOException {
			return in.available();
		}

		@Override
		public void close() throws IOException {
			in.close();
		}

		@Override
		public void mark(int readlimit) {
			// not supported
		}

		@Override
		public void reset() throws IOException {
			// do nothing as mark is not supported
		}

		@Override
		public boolean markSupported() {
			return false;
		}

	}

}