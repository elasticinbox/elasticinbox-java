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
import static org.ops4j.pax.exam.OptionUtils.combine;
import static org.ops4j.pax.exam.spi.PaxExamRuntime.createTestSystem;
import static org.ops4j.pax.exam.spi.PaxExamRuntime.createContainer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static com.jayway.restassured.RestAssured.*;
import static com.jayway.restassured.path.json.JsonPath.*;

import java.io.IOException;
import java.io.InputStream;
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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.TimeoutException;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.EagerSingleStagedReactorFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elasticinbox.core.model.ReservedLabels;

/**
 * Integration test for REST APIs
 * 
 * @author Rustam Aliyev
 */
@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(EagerSingleStagedReactorFactory.class)
public class LmtpIT
{
	private static final String TEST_ACCOUNT = "test@elasticinbox.com";
	private static final String REST_PATH = "/rest/v1/" + TEST_ACCOUNT;
//	private static final String EMAIL_LARGE_ATT = "/01-attach-utf8.eml";
	private static final String EMAIL_REGULAR = "/01-headers-utf8.eml";

	private final static Logger logger = 
			LoggerFactory.getLogger(LmtpIT.class);

	@Configuration()
	public Option[] config()
	{
		return options(
				//junitBundles(),
				felix(),
				workingDirectory("target/paxrunner/"),
				repository("https://repository.apache.org/snapshots/").allowSnapshots(),

				// Configs
				systemProperty("elasticinbox.config").value("../test-classes/elasticinbox.yaml"),
				systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("INFO"),

				// PAX Exam Bundles
				mavenBundle().groupId("org.mortbay.jetty").artifactId("servlet-api").version("2.5-20110124"),
				mavenBundle().groupId("org.ops4j.pax.web").artifactId("pax-web-api").version("1.0.7"),
				mavenBundle().groupId("org.ops4j.pax.web").artifactId("pax-web-spi").version("1.0.7"),
				mavenBundle().groupId("org.ops4j.pax.web").artifactId("pax-web-jetty-bundle").version("1.0.7"),
				mavenBundle().groupId("org.ops4j.pax.web").artifactId("pax-web-extender-war").version("1.0.7"),
				
				// Logging
				mavenBundle().groupId("ch.qos.logback").artifactId("logback-core").versionAsInProject(),
				mavenBundle().groupId("ch.qos.logback").artifactId("logback-classic").versionAsInProject(),

				// REST-Assured Bundles
				wrappedBundle(mavenBundle().groupId("com.jayway.restassured").artifactId("rest-assured").versionAsInProject()),
				wrappedBundle(mavenBundle().groupId("org.codehaus.groovy.modules.http-builder").artifactId("http-builder").version("0.5.2")),
				wrappedBundle(mavenBundle().groupId("org.hamcrest").artifactId("hamcrest-all").version("1.1")),
				wrappedBundle(mavenBundle().groupId("xml-resolver").artifactId("xml-resolver").version("1.2")),
				wrappedBundle(mavenBundle().groupId("net.sf.ezmorph").artifactId("ezmorph").version("1.0.6")),
				wrappedBundle(mavenBundle().groupId("net.sf.json-lib").artifactId("json-lib").version("2.4").classifier("jdk15")),
				wrappedBundle(mavenBundle().groupId("net.sourceforge.nekohtml").artifactId("nekohtml").version("1.9.15")),
				wrappedBundle(mavenBundle().groupId("xerces").artifactId("xercesImpl").version("2.9.1")),
				mavenBundle().groupId("org.codehaus.groovy").artifactId("groovy-all").version("1.7.10"),
				//mavenBundle().groupId("commons-lang").artifactId("commons-lang").version("2.6"),
				mavenBundle().groupId("commons-beanutils").artifactId("commons-beanutils").version("1.8.3"),
				mavenBundle().groupId("commons-collections").artifactId("commons-collections").version("3.2.1"),
				mavenBundle().groupId("org.apache.httpcomponents").artifactId("httpcore-osgi").version("4.1.2"),
				mavenBundle().groupId("org.apache.httpcomponents").artifactId("httpclient-osgi").version("4.1.2"),

				// jClouds and dependencies
				mavenBundle().groupId("com.google.inject").artifactId("guice").versionAsInProject(),
				mavenBundle().groupId("org.jclouds").artifactId("jclouds-core").versionAsInProject(),
				mavenBundle().groupId("org.jclouds").artifactId("jclouds-blobstore").versionAsInProject(),
				mavenBundle().groupId("org.apache.servicemix.bundles").artifactId("org.apache.servicemix.bundles.aopalliance").versionAsInProject(),
				mavenBundle().groupId("org.apache.servicemix.bundles").artifactId("org.apache.servicemix.bundles.commons-io").versionAsInProject(),
				mavenBundle().groupId("org.apache.servicemix.bundles").artifactId("org.apache.servicemix.bundles.commons-lang").versionAsInProject(),
				mavenBundle().groupId("org.apache.servicemix.bundles").artifactId("org.apache.servicemix.bundles.javax-inject").versionAsInProject(),
				mavenBundle().groupId("org.apache.servicemix.bundles").artifactId("org.apache.servicemix.bundles.oauth-commons").versionAsInProject(),
				mavenBundle().groupId("org.apache.servicemix.bundles").artifactId("org.apache.servicemix.bundles.java-xmlbuilder").versionAsInProject(),
				mavenBundle().groupId("com.google.inject.extensions").artifactId("guice-assistedinject").versionAsInProject(),
				mavenBundle().groupId("com.google.code.gson").artifactId("gson").versionAsInProject(),

				// LMTP Test Bundles
				wrappedBundle(mavenBundle().groupId("me.normanmaurer").artifactId("niosmtp").versionAsInProject()),
				mavenBundle().groupId("org.apache.james.protocols").artifactId("protocols-netty").versionAsInProject(),
				mavenBundle().groupId("org.apache.james.protocols").artifactId("protocols-api").versionAsInProject(),
				mavenBundle().groupId("org.apache.james.protocols").artifactId("protocols-smtp").versionAsInProject(),
				mavenBundle().groupId("org.apache.james.protocols").artifactId("protocols-lmtp").versionAsInProject(),
				mavenBundle().groupId("org.jboss.netty").artifactId("netty").versionAsInProject(),

				// ElasticInbox Bundles
				mavenBundle().groupId("com.googlecode.guava-osgi").artifactId("guava-osgi").versionAsInProject(),
				mavenBundle().groupId("org.codehaus.jackson").artifactId("jackson-core-asl").versionAsInProject(),
				mavenBundle().groupId("org.codehaus.jackson").artifactId("jackson-mapper-asl").versionAsInProject(),
				mavenBundle().groupId("org.codehaus.jackson").artifactId("jackson-jaxrs").versionAsInProject(),
				mavenBundle().groupId("com.ning").artifactId("compress-lzf").versionAsInProject(),
				mavenBundle().groupId("com.sun.jersey").artifactId("jersey-core").versionAsInProject(),
				mavenBundle().groupId("com.sun.jersey").artifactId("jersey-server").versionAsInProject(),
				mavenBundle().groupId("com.sun.jersey").artifactId("jersey-servlet").versionAsInProject(),
				mavenBundle().groupId("javax.mail").artifactId("mail").versionAsInProject(),
				scanDir("../bundles/com.ecyrd.speed4j/target/"),
				scanDir("../modules/common/target/"),
				scanDir("../modules/config/target/"),
				scanDir("../modules/core/target/"),
				scanDir("../modules/lmtp/target/"),
				scanDir("../modules/rest/target/")
		);
	}

	public static void main(String[] args) throws TimeoutException, IOException
	{
		createContainer(
				createTestSystem(
						combine(new LmtpIT().config(), profile("gogo")))).start();
	}

	@BeforeClass
	public static void createAccountTest() {
		// create account
		expect().statusCode(201).when().post(REST_PATH);
	}

	@AfterClass
	public static void deleteAccountTest() {
		// delete account
		expect().statusCode(204).when().delete(REST_PATH);
	}

	//@Test
	public void bundleContextTest(BundleContext ctx)
	{
		//assertThat(ctx, is(notNullValue()));
		logger.info("BundleContext of bundle injected: {}", ctx.getBundle().getSymbolicName());
		
		for (Bundle b : ctx.getBundles()) {
			logger.info("Bundle {} [state={}]", b.getSymbolicName(), b.getState());
		}
	}

	@Test
	public void reservedLabelsTest() throws IOException
	{
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

//		logger.info("JSON = {}", jsonResponse);
	}

	/**
	 * Returns resource size
	 *  
	 * @param name
	 * @return
	 * @throws IOException 
	 */
	private long getResourceSize(String messageFile) throws IOException
	{
		InputStream in = null;

		try {
			in = this.getClass().getResourceAsStream(messageFile);
			return in.available();
		} finally {
			in.close();
		}
	}
}