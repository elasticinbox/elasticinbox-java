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
import static com.jayway.restassured.RestAssured.*;
import static com.jayway.restassured.path.json.JsonPath.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;

import me.normanmaurer.niosmtp.SMTPClientFuture;
import me.normanmaurer.niosmtp.core.SMTPMessageImpl;
import me.normanmaurer.niosmtp.delivery.DeliveryRecipientStatus;
import me.normanmaurer.niosmtp.delivery.LMTPDeliveryAgent;
import me.normanmaurer.niosmtp.delivery.SMTPDeliveryAgent;
import me.normanmaurer.niosmtp.delivery.SMTPDeliveryEnvelope;
import me.normanmaurer.niosmtp.delivery.impl.SMTPDeliveryAgentConfigImpl;
import me.normanmaurer.niosmtp.delivery.impl.SMTPDeliveryEnvelopeImpl;
import me.normanmaurer.niosmtp.transport.FutureResult;
import me.normanmaurer.niosmtp.transport.SMTPClientTransport;
import me.normanmaurer.niosmtp.transport.SMTPClientTransportFactory;
import me.normanmaurer.niosmtp.transport.netty.NettyLMTPClientTransportFactory;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.EagerSingleStagedReactorFactory;

import com.elasticinbox.core.model.ReservedLabels;
import com.google.common.collect.ObjectArrays;

/**
 * Integration test for LMTP
 * 
 * @author Rustam Aliyev
 */
@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(EagerSingleStagedReactorFactory.class)
public class LmtpIT extends AbstractIntegrationTest
{
	/**
	 * Append LMTP Specific config options
	 * 
	 * @return
	 */
	@Configuration()
	public Option[] config()
	{
		return ObjectArrays.concat(super.config(), options(
				// LMTP Test Bundles
				wrappedBundle(mavenBundle().groupId("me.normanmaurer").artifactId("niosmtp").versionAsInProject()),
				mavenBundle().groupId("org.apache.james.protocols").artifactId("protocols-netty").versionAsInProject(),
				mavenBundle().groupId("org.apache.james.protocols").artifactId("protocols-api").versionAsInProject(),
				mavenBundle().groupId("org.apache.james.protocols").artifactId("protocols-smtp").versionAsInProject(),
				mavenBundle().groupId("org.apache.james.protocols").artifactId("protocols-lmtp").versionAsInProject(),
				mavenBundle().groupId("io.netty").artifactId("netty").versionAsInProject(),

				// ElasticInbox Bundles
				scanDir("../modules/lmtp/target/")
			), Option.class);
	}

	@Test
	public void lmtpTest() throws IOException
	{
		initAccount();
		
		SMTPClientTransportFactory transportFactory = NettyLMTPClientTransportFactory.createNio();
		SMTPClientTransport transport = transportFactory.createPlain();
		SMTPDeliveryAgent c = new LMTPDeliveryAgent(transport);

		SMTPDeliveryAgentConfigImpl conf = new SMTPDeliveryAgentConfigImpl();
        conf.setConnectionTimeout(2);
        conf.setResponseTimeout(2);

		try {
			SMTPDeliveryEnvelope transaction = new SMTPDeliveryEnvelopeImpl(
					"rustam@elasticinbox.com",
					Arrays.asList(new String[] { TEST_ACCOUNT, "nonexistent@example.com" }),
					new SMTPMessageImpl(this.getClass().getResourceAsStream(EMAIL_REGULAR)));

			SMTPClientFuture<Collection<FutureResult<Iterator<DeliveryRecipientStatus>>>> future = c
					.deliver(new InetSocketAddress(2400), conf, transaction);

			for(Iterator<FutureResult<Iterator<DeliveryRecipientStatus>>> i = future.get().iterator(); i.hasNext();)
			{
				FutureResult<Iterator<DeliveryRecipientStatus>> item = i.next();
				assertThat(item.isSuccess(), is(true));
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			transport.destroy();
		}

		// check latest message
		String jsonResponse = given().
			pathParam("labelId", ReservedLabels.INBOX.getId()).
		expect().
			statusCode(200).
		when().
			get(REST_PATH + "/mailbox/label/{labelId}?count=1").asString();

		final String messageId = with(jsonResponse).get("get(0)");

		// check parsed message
		given().
			pathParam("messageId", messageId).
		expect().
			statusCode(200).and().
			body("message.labels", hasItems(0, 1)).
			body("message.size", equalTo((int) getResourceSize(EMAIL_REGULAR))).
			body("message.from.address", hasItems("rustam@elasticinbox.com")).
			body("message.to.address", hasItems(TEST_ACCOUNT)).
			body("message.subject", is(notNullValue())).
			body("message.htmlBody", is(notNullValue())).
			body("message.textBody", is(nullValue())).
		when().
			get(REST_PATH + "/mailbox/message/{messageId}");
	}
}