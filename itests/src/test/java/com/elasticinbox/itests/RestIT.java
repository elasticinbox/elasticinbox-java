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

import static org.hamcrest.Matchers.*;
import static com.jayway.restassured.RestAssured.*;
import static com.jayway.restassured.path.json.JsonPath.*;

import groovyx.net.http.ContentType;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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

import com.elasticinbox.core.model.LabelCounters;
import com.elasticinbox.core.model.Marker;
import com.elasticinbox.core.model.ReservedLabels;
import com.google.common.io.ByteStreams;
import com.jayway.restassured.response.Response;

/**
 * Integration test for REST APIs
 * 
 * @author Rustam Aliyev
 */
@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(EagerSingleStagedReactorFactory.class)
public class RestIT
{
	private static final String TEST_ACCOUNT = "test@elasticinbox.com";
	private static final String REST_PATH = "/rest/v1/" + TEST_ACCOUNT;
	private static final String EMAIL_LARGE_ATT = "/01-attach-utf8.eml";
	private static final String EMAIL_REGULAR = "/01-headers-utf8.eml";

	private final static Logger logger = 
			LoggerFactory.getLogger(RestIT.class);

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
				scanDir("../modules/rest/target/")
		);
	}

	public static void main(String[] args) throws TimeoutException, IOException
	{
		createContainer(
				createTestSystem(
						combine(new RestIT().config(), profile("gogo")))).start();
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
	public void reservedLabelsTest()
	{
		// check reserved labels
		expect().
			statusCode(200).and().
			body("\"" + ReservedLabels.ALL_MAILS.getLabelId() + "\"",	equalTo(ReservedLabels.ALL_MAILS.getLabelName())).
			body("\"" + ReservedLabels.INBOX.getLabelId() + "\"",		equalTo(ReservedLabels.INBOX.getLabelName())).
			body("\"" + ReservedLabels.DRAFTS.getLabelId() + "\"",		equalTo(ReservedLabels.DRAFTS.getLabelName())).
			body("\"" + ReservedLabels.SENT.getLabelId() + "\"",		equalTo(ReservedLabels.SENT.getLabelName())).
			body("\"" + ReservedLabels.TRASH.getLabelId() + "\"",		equalTo(ReservedLabels.TRASH.getLabelName())).
			body("\"" + ReservedLabels.SPAM.getLabelId() + "\"",		equalTo(ReservedLabels.SPAM.getLabelName())).
			body("\"" + ReservedLabels.STARRED.getLabelId() + "\"",		equalTo(ReservedLabels.STARRED.getLabelName())).
			body("\"" + ReservedLabels.IMPORTANT.getLabelId() + "\"",	equalTo(ReservedLabels.IMPORTANT.getLabelName())).
			body("\"" + ReservedLabels.NOTIFICATIONS.getLabelId() + "\"",equalTo(ReservedLabels.NOTIFICATIONS.getLabelName())).
			body("\"" + ReservedLabels.ATTACHMENTS.getLabelId() + "\"",	equalTo(ReservedLabels.ATTACHMENTS.getLabelName())).
		when().
			get(REST_PATH + "/mailbox");
	}

	@Test
	public void reservedLabelsWithMetadataTest()
	{
		// check labels metadata
		expect().
			statusCode(200).and().
			body("\"" + ReservedLabels.INBOX.getLabelId() + "\".name",	equalTo(ReservedLabels.INBOX.getLabelName())).
			body("\"" + ReservedLabels.INBOX.getLabelId() + "\".size",	greaterThanOrEqualTo(0)).
			body("\"" + ReservedLabels.INBOX.getLabelId() + "\".new",	greaterThanOrEqualTo(0)).
			body("\"" + ReservedLabels.INBOX.getLabelId() + "\".total",	greaterThanOrEqualTo(0)).
		when().
			get(REST_PATH + "/mailbox?metadata=true");
	}

	@Test
	public void labelListAddDeleteTest()
	{
		String labelName = "MyLabel";
		String labelRename = "MyAnotherLabel";
		Integer labelId = null;

		// add label
		labelId = addLabel(labelName);

		// check added label
		expect().
			statusCode(200).and().
			body("\"" + labelId + "\"", equalTo(labelName)).
		when().
			get(REST_PATH + "/mailbox");

		logger.info("Add label test OK");

		// rename label
		given().
			pathParam("labelId", labelId).
		expect().
			statusCode(204).
		when().
			put(REST_PATH + "/mailbox/label/{labelId}?name=" + labelRename);

		// check renamed label
		expect().
			statusCode(200).and().
			body("\"" + labelId + "\"", equalTo(labelRename)).
		when().
			get(REST_PATH + "/mailbox");
		
		logger.info("Rename label test OK");
		
		// adding existing label, should fail
		given().
			pathParam("labelName", labelRename).
		expect().
			statusCode(400).
		when().
			post(REST_PATH + "/mailbox/label?name={labelName}");
		
		// adding label under existing label hierarchically, should fail
//		given().
//			pathParam("labelName", labelRename + "/subLabel").
//		expect().
//			statusCode(400).
//		when().
//			post(REST_PATH + "/mailbox/label?name={labelName}");

		// delete label
		given().
			pathParam("labelId", labelId).
		expect().
			statusCode(204).
		when().
			delete(REST_PATH + "/mailbox/label/{labelId}");
		
		// check deleted label
		expect().
			statusCode(200).and().
			body("\"" + labelId + "\"", is(nullValue())).
		when().
			get(REST_PATH + "/mailbox").asString();

		logger.info("Delete label test OK");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void messageAddListViewDeletePurgeTest() throws IOException
	{
		long fileSizeA = getResourceSize(EMAIL_REGULAR);

		String messageId = null;
		Map<String, UUID> messages = new HashMap<String, UUID>(2);
		Integer labelId = ReservedLabels.INBOX.getLabelId(); 

		// add message
		messages.put("headers", addMessage(EMAIL_REGULAR, labelId));
		messages.put("attach",  addMessage(EMAIL_LARGE_ATT, labelId));

		logger.info("Message Store Test OK");

		// check message listing
		given().
			pathParam("labelId", labelId).
		expect().
			statusCode(200).and().
			body("", hasItems(messages.get("headers").toString(), messages.get("attach").toString())).
		when().
			get(REST_PATH + "/mailbox/label/{labelId}");

		logger.info("Message List Test OK");

		// TODO: need to implement sequence order to test reverese
		// check reverse message listing (only oldest message should be listed)
//		given().
//			pathParam("labelId", labelId).
//		expect().
//			statusCode(200).and().
//			body("", not( hasItems(messages.get("headers").toString()) )).
//			body("", not( hasItems(messages.get("attach").toString()) )).
//		when().
//			get(REST_PATH + "/mailbox/label/{labelId}?reverse=false&count=1");
//
//		logger.info("Message Reverse List Test OK");

		// check message list with metadata
		messageId = messages.get("headers").toString();
		given().
			pathParam("labelId", labelId).
			pathParam("messageId", messageId).
		expect().
			statusCode(200).and().
			body(messageId + ".labels", hasItems(0, 1)).
			body(messageId + ".size", equalTo((int) fileSizeA)).
			body(messageId + ".from.address", hasItems(containsString("@"))).
			body(messageId + ".to.address", hasItems(containsString("@"))).
			body(messageId + ".subject", is(notNullValue())).
		when().
			get(REST_PATH + "/mailbox/label/{labelId}?metadata=true&count=2&start={messageId}");

		logger.info("Message List with Metadata Test OK");

		// check parsed message
		messageId = messages.get("headers").toString();
		given().
			pathParam("labelId", labelId).
			pathParam("messageId", messageId).
		expect().
			statusCode(200).and().
			body("message.labels", hasItems(0, 1)).
			body("message.size", equalTo((int) fileSizeA)).
			body("message.from.address", hasItems(containsString("@"))).
			body("message.to.address", hasItems(containsString("@"))).
			body("message.subject", is(notNullValue())).
			body("message.htmlBody", is(notNullValue())).
			body("message.textBody", is(nullValue())).
		when().
			get(REST_PATH + "/mailbox/message/{messageId}?adjacent=true&label={labelId}");

		logger.info("Parsed Message Fetch Test OK");

		// delete message (from all labels)
		given().
			pathParam("messageId", messageId).
		expect().
			statusCode(204).
		when().
			delete(REST_PATH + "/mailbox/message/{messageId}");

		// check deleted message in labels
		given().
			pathParam("labelId", labelId).
		expect().
			statusCode(200).and().
			body(messageId, is(nullValue())).
		when().
			get(REST_PATH + "/mailbox/label/{labelId}?metadata=true");

		// message should remain until purged
		given().
			pathParam("messageId", messageId).
		expect().
			statusCode(200).and().
			body("message.labels", hasItems(0, 1)).
		when().
			get(REST_PATH + "/mailbox/message/{messageId}");

		logger.info("Delete Message Test OK");

		// purge deleted messages 
		given().
			pathParam("ageDate", new SimpleDateFormat("yyyy/MM/dd").format(new Date()) + " 23:59").
		expect().
			statusCode(204).
		when().
			put(REST_PATH + "/mailbox/purge?age={ageDate}");

		// check purged message
		given().
			pathParam("messageId", messageId).
		expect().
			statusCode(400).
		when().
			get(REST_PATH + "/mailbox/message/{messageId}");

		logger.info("Purge Message Test OK");
	}

	@Test
	public void messageAddRemoveLabelsMarkersTest() throws IOException
	{
		// add labels
		Integer labelId1 = addLabel("CustomLabelTest3739");
		Integer labelId2 = addLabel("CustomLabelTest2398");

		// add message
		UUID messageId = addMessage(EMAIL_REGULAR, ReservedLabels.INBOX.getLabelId());

		// assign labels and marker to the message
		given().
			pathParam("messageId", messageId).
			pathParam("labelId1", labelId1).
			pathParam("labelId2", labelId2).
			pathParam("seenMarker", Marker.SEEN.toString().toLowerCase()).
		expect().
			statusCode(204).
		when().
			put(REST_PATH + "/mailbox/message/{messageId}?addlabel={labelId1}&addlabel={labelId2}&addmarker={seenMarker}");

		// verify labels and marker
		given().
			pathParam("messageId", messageId).
		expect().
			statusCode(200).and().
			body("message.labels", hasItems(0, 1, labelId1, labelId2)).
			body("message.markers", hasItems(Marker.SEEN.toString().toUpperCase())).
		when().
			get(REST_PATH + "/mailbox/message/{messageId}");

		// assign/remove labels and markers to the message
		given().
			pathParam("messageId", messageId).
			pathParam("removeLabel", labelId1).
			pathParam("addLabel", ReservedLabels.SPAM.getLabelId()).
			pathParam("removeMarker", Marker.SEEN.toString().toLowerCase()).
			pathParam("addMarker", Marker.REPLIED.toString().toLowerCase()).
		expect().
			statusCode(204).
		when().
			put(REST_PATH + "/mailbox/message/{messageId}?addlabel={addLabel}&removelabel={removeLabel}&removemarker={removeMarker}&addmarker={addMarker}");
		
		// verify labels and markers
		given().
			pathParam("messageId", messageId).
		expect().
			statusCode(200).and().
			body("message.labels", hasItems(0, 1, labelId2, ReservedLabels.SPAM.getLabelId())).
			body("message.labels", not( hasItems(labelId1) )).
			body("message.markers", hasItems(Marker.REPLIED.toString().toUpperCase())).
			body("message.markers", not( hasItems(Marker.SEEN.toString().toUpperCase()) )).
		when().
			get(REST_PATH + "/mailbox/message/{messageId}");
	}

	@Test
	public void messageBatchMofifyDeleteTest() throws IOException
	{
		// add labels
		Integer labelId1 = addLabel("BatchLabelTest0912");
		Integer labelId2 = addLabel("BatchLabelTest1290");

		// add messages
		UUID messageId1 = addMessage(EMAIL_REGULAR, ReservedLabels.INBOX.getLabelId());
		UUID messageId2 = addMessage(EMAIL_LARGE_ATT, ReservedLabels.INBOX.getLabelId());

		// batch add labels and markers
		given().
			pathParam("labelId1", labelId1).
			pathParam("labelId2", labelId2).
			pathParam("seenMarker", Marker.SEEN.toString().toLowerCase()).
			request().body("[\"" + messageId1.toString() + "\", \"" + messageId2.toString() + "\"]").contentType(ContentType.JSON).
		expect().
			statusCode(204).
		when().
			put(REST_PATH + "/mailbox/message?addlabel={labelId1}&addlabel={labelId2}&addmarker={seenMarker}");

		// verify labels and markers
		given().
			pathParam("labelId", ReservedLabels.ALL_MAILS.getLabelId()).
		expect().
			statusCode(200).and().
			body(messageId1.toString() + ".labels", hasItems(0, 1, labelId1, labelId2)).
			body(messageId2.toString() + ".labels", hasItems(0, 1, labelId1, labelId2)).
			body(messageId1.toString() + ".markers", hasItems(Marker.SEEN.toString().toUpperCase())).
			body(messageId2.toString() + ".markers", hasItems(Marker.SEEN.toString().toUpperCase())).
		when().
			get(REST_PATH + "/mailbox/label/{labelId}?metadata=true");

		/**
		 * Batch delete test is skipped at the moment. HTTP DELETE request body
		 * is not supported by RestAssured/HTTPBuilder at the moment. See:
		 * 
		 * http://code.google.com/p/rest-assured/issues/detail?id=48
		 * http://stackoverflow.com/questions/299628/is-an-entity-body-allowed-for-an-http-delete-request
		 * 
		 */

		// batch delete messages
//		given().
//			request().body("[\"" + messageId1.toString() + "\", \"" + messageId2.toString() + "\"]").contentType(ContentType.JSON).
//		expect().
//			statusCode(204).
//		when().
//			delete(REST_PATH + "/mailbox/message");

		// verify batch delete
//		given().
//			pathParam("labelId", ReservedLabels.ALL_MAILS.getLabelId()).
//		expect().
//			statusCode(200).and().
//			body(messageId1.toString(), is(nullValue())).
//			body(messageId2.toString(), is(nullValue())).
//		when().
//			get(REST_PATH + "/mailbox/label/{labelId}?metadata=true");
	}

	@Test
	public void messageAttachmentAndRawTest() throws IOException
	{
		// add message
		UUID messageId = addMessage(EMAIL_LARGE_ATT, ReservedLabels.INBOX.getLabelId());
		long fileSize = getResourceSize(EMAIL_LARGE_ATT);

		// get attachment by part id
		given().
			pathParam("messageId", messageId).
			pathParam("partId", 3).
		expect().
			statusCode(200).and().
			header("Content-Type", equalTo("application/pdf")).
			//header("Content-Length", equalTo("64736")).
			header("Content-Disposition", containsString("attachment; filename=")).
		when().
			get(REST_PATH + "/mailbox/message/{messageId}/{partId}");

		// get attachment by content id
		given().
			pathParam("messageId", messageId).
			pathParam("contentId", "<image-001>").
		expect().
			statusCode(200).and().
			header("Content-Type", equalTo("image/png")).
			//header("Content-Length", equalTo("27136")).
			header("Content-Disposition", containsString("attachment; filename=")).
		when().
			get(REST_PATH + "/mailbox/message/{messageId}/{contentId}");

		// get raw message source
		given().
			pathParam("messageId", messageId).
		expect().
			statusCode(200).and().
			header("Content-Type", equalTo("text/plain")).
			header("Content-Length", equalTo(String.valueOf(fileSize))).
		when().
			get(REST_PATH + "/mailbox/message/{messageId}/raw");
	}

	@Test
	public void messageUpdateTest() throws IOException
	{
		// add message
		UUID messageId = addMessage(EMAIL_LARGE_ATT, ReservedLabels.INBOX.getLabelId());

		// add labels and markers
		given().
			pathParam("messageId", messageId).
			pathParam("labelId1", ReservedLabels.IMPORTANT.getLabelId()).
			pathParam("labelId2", ReservedLabels.STARRED.getLabelId()).
			pathParam("marker1", Marker.SEEN.toString().toLowerCase()).
		expect().
			statusCode(204).
		when().
			put(REST_PATH + "/mailbox/message/{messageId}?addlabel={labelId1}&addlabel={labelId2}&addmarker={marker1}");

		// overwrite message
		InputStream fin = this.getClass().getResourceAsStream(EMAIL_REGULAR);
		byte[] messageBytes = ByteStreams.toByteArray(fin);
		fin.close();

		Response response = 
			given().
				pathParam("messageId", messageId.toString()).
				request().body(messageBytes).contentType(ContentType.BINARY).
			expect().
				statusCode(201).
			when().
				post(REST_PATH + "/mailbox/message/{messageId}");

		UUID newMessageId = UUID.fromString( with(response.asString()).getString("id") );

		// verify that message updated and labels/markers preserved
		given().
			pathParam("messageId", newMessageId.toString()).
		expect().
			statusCode(200).and().
			body("message.labels", hasItems(ReservedLabels.IMPORTANT.getLabelId(), ReservedLabels.STARRED.getLabelId())).
			body("message.markers", hasItems(Marker.SEEN.toString().toUpperCase())).
		when().
			get(REST_PATH + "/mailbox/message/{messageId}");

	}

	@Test
	public void countersTest() throws IOException
	{
		LabelCounters allCounters = new LabelCounters();
		LabelCounters inboxCounters = new LabelCounters();
		LabelCounters spamCounters = new LabelCounters();

		// check label counter before message added
		String jsonResponse = expect().statusCode(200).when().get(REST_PATH + "/mailbox?metadata=true").asString();
		allCounters = getCounters(jsonResponse, ReservedLabels.ALL_MAILS.getLabelId());
		inboxCounters = getCounters(jsonResponse, ReservedLabels.INBOX.getLabelId());
		spamCounters = getCounters(jsonResponse, ReservedLabels.SPAM.getLabelId());

		// add message A
		long fileSizeA = getResourceSize(EMAIL_REGULAR);
		UUID messageIdA = addMessage(EMAIL_REGULAR, ReservedLabels.INBOX.getLabelId());

		// check label counters
		jsonResponse = 
			expect().
				statusCode(200).and().
				body("\"" + ReservedLabels.ALL_MAILS.getLabelId() + "\".size", 
						equalTo((int) (allCounters.getTotalBytes().longValue() + fileSizeA))).
				body("\"" + ReservedLabels.ALL_MAILS.getLabelId() + "\".total", 
						equalTo((int) (allCounters.getTotalMessages().longValue() + 1))).
				body("\"" + ReservedLabels.ALL_MAILS.getLabelId() + "\".new", 
						equalTo((int) (allCounters.getNewMessages().longValue() + 1))).
				body("\"" + ReservedLabels.INBOX.getLabelId() + "\".size", 
						equalTo((int) (inboxCounters.getTotalBytes().longValue() + fileSizeA))).
				body("\"" + ReservedLabels.INBOX.getLabelId() + "\".total", 
						equalTo((int) (inboxCounters.getTotalMessages().longValue() + 1))).
				body("\"" + ReservedLabels.INBOX.getLabelId() + "\".new", 
						equalTo((int) (inboxCounters.getNewMessages().longValue() + 1))).
			when().
				get(REST_PATH + "/mailbox?metadata=true").asString();

		// add label SPAM to message A
		given().
			pathParam("messageId", messageIdA.toString()).
			pathParam("labelId", ReservedLabels.SPAM.getLabelId()).
		expect().
			statusCode(204).
		when().
			put(REST_PATH + "/mailbox/message/{messageId}?addlabel={labelId}");

		// check label counters
		jsonResponse = 
			expect().
				statusCode(200).and().
				body("\"" + ReservedLabels.SPAM.getLabelId() + "\".size", 
						equalTo((int) (spamCounters.getTotalBytes().longValue() + fileSizeA))).
				body("\"" + ReservedLabels.SPAM.getLabelId() + "\".total", 
						equalTo((int) (spamCounters.getTotalMessages().longValue() + 1))).
				body("\"" + ReservedLabels.SPAM.getLabelId() + "\".new", 
						equalTo((int) (spamCounters.getNewMessages().longValue() + 1))).
			when().
				get(REST_PATH + "/mailbox?metadata=true").asString();

		// update counters
		allCounters = getCounters(jsonResponse, ReservedLabels.ALL_MAILS.getLabelId());
		inboxCounters = getCounters(jsonResponse, ReservedLabels.INBOX.getLabelId());
		spamCounters = getCounters(jsonResponse, ReservedLabels.SPAM.getLabelId());

		// mark message as read
		given().
			pathParam("messageId", messageIdA.toString()).
			pathParam("marker", Marker.SEEN.toString().toLowerCase()).
		expect().
			statusCode(204).
		when().
			put(REST_PATH + "/mailbox/message/{messageId}?addmarker={marker}");

		// check label counters
		jsonResponse = 
			expect().
				statusCode(200).and().
				body("\"" + ReservedLabels.ALL_MAILS.getLabelId() + "\".new", 
						equalTo((int) (allCounters.getNewMessages().longValue() - 1))).
				body("\"" + ReservedLabels.INBOX.getLabelId() + "\".new", 
						equalTo((int) (inboxCounters.getNewMessages().longValue() - 1))).
				body("\"" + ReservedLabels.SPAM.getLabelId() + "\".new", 
						equalTo((int) (spamCounters.getNewMessages().longValue() - 1))).
			when().
				get(REST_PATH + "/mailbox?metadata=true").asString();

		// update counters
		allCounters = getCounters(jsonResponse, ReservedLabels.ALL_MAILS.getLabelId());
		inboxCounters = getCounters(jsonResponse, ReservedLabels.INBOX.getLabelId());
		spamCounters = getCounters(jsonResponse, ReservedLabels.SPAM.getLabelId());

		// remove label INBOX from message A
		given().
			pathParam("messageId", messageIdA.toString()).
			pathParam("labelId", ReservedLabels.INBOX.getLabelId()).
		expect().
			statusCode(204).
		when().
			put(REST_PATH + "/mailbox/message/{messageId}?removelabel={labelId}");

		// check label counters
		jsonResponse = 
			expect().
				statusCode(200).and().
				body("\"" + ReservedLabels.ALL_MAILS.getLabelId() + "\".size", 
						equalTo((int) (allCounters.getTotalBytes().longValue()))).
				body("\"" + ReservedLabels.ALL_MAILS.getLabelId() + "\".total", 
						equalTo((int) (allCounters.getTotalMessages().longValue()))).
				body("\"" + ReservedLabels.ALL_MAILS.getLabelId() + "\".new", 
						equalTo((int) (allCounters.getNewMessages().longValue()))).
				body("\"" + ReservedLabels.INBOX.getLabelId() + "\".size", 
						equalTo((int) (inboxCounters.getTotalBytes().longValue() - fileSizeA))).
				body("\"" + ReservedLabels.INBOX.getLabelId() + "\".total", 
						equalTo((int) (inboxCounters.getTotalMessages().longValue() - 1))).
				body("\"" + ReservedLabels.INBOX.getLabelId() + "\".new", 
						equalTo((int) (inboxCounters.getNewMessages().longValue()))).
				body("\"" + ReservedLabels.SPAM.getLabelId() + "\".size", 
						equalTo((int) (spamCounters.getTotalBytes().longValue()))).
				body("\"" + ReservedLabels.SPAM.getLabelId() + "\".total", 
						equalTo((int) (spamCounters.getTotalMessages().longValue()))).
				body("\"" + ReservedLabels.SPAM.getLabelId() + "\".new", 
						equalTo((int) (spamCounters.getNewMessages().longValue()))).
			when().
				get(REST_PATH + "/mailbox?metadata=true").asString();

		// update counters
		allCounters = getCounters(jsonResponse, ReservedLabels.ALL_MAILS.getLabelId());
		inboxCounters = getCounters(jsonResponse, ReservedLabels.INBOX.getLabelId());
		spamCounters = getCounters(jsonResponse, ReservedLabels.SPAM.getLabelId());

		// add message B to SPAM
		long fileSizeB = getResourceSize(EMAIL_LARGE_ATT);
		addMessage(EMAIL_LARGE_ATT, ReservedLabels.SPAM.getLabelId());

		// check label counters
		jsonResponse = 
			expect().
				statusCode(200).and().
				body("\"" + ReservedLabels.ALL_MAILS.getLabelId() + "\".size", 
						equalTo((int) (allCounters.getTotalBytes().longValue() + fileSizeB))).
				body("\"" + ReservedLabels.ALL_MAILS.getLabelId() + "\".total", 
						equalTo((int) (allCounters.getTotalMessages().longValue() + 1))).
				body("\"" + ReservedLabels.ALL_MAILS.getLabelId() + "\".new", 
						equalTo((int) (allCounters.getNewMessages().longValue() + 1))).
				body("\"" + ReservedLabels.INBOX.getLabelId() + "\".size", 
						equalTo((int) (inboxCounters.getTotalBytes().longValue()))).
				body("\"" + ReservedLabels.INBOX.getLabelId() + "\".total", 
						equalTo((int) (inboxCounters.getTotalMessages().longValue()))).
				body("\"" + ReservedLabels.INBOX.getLabelId() + "\".new", 
						equalTo((int) (inboxCounters.getNewMessages().longValue()))).
				body("\"" + ReservedLabels.SPAM.getLabelId() + "\".size", 
						equalTo((int) (spamCounters.getTotalBytes().longValue() + fileSizeB))).
				body("\"" + ReservedLabels.SPAM.getLabelId() + "\".total", 
						equalTo((int) (spamCounters.getTotalMessages().longValue() + 1))).
				body("\"" + ReservedLabels.SPAM.getLabelId() + "\".new", 
						equalTo((int) (spamCounters.getNewMessages().longValue() + 1))).
			when().
				get(REST_PATH + "/mailbox?metadata=true").asString();

		// update counters
		allCounters = getCounters(jsonResponse, ReservedLabels.ALL_MAILS.getLabelId());
		inboxCounters = getCounters(jsonResponse, ReservedLabels.INBOX.getLabelId());
		spamCounters = getCounters(jsonResponse, ReservedLabels.SPAM.getLabelId());

		// remove message A
		given().
			pathParam("messageId", messageIdA).
		expect().
			statusCode(204).
		when().
			delete(REST_PATH + "/mailbox/message/{messageId}");

		// check label counters
		jsonResponse = 
			expect().
				statusCode(200).and().
				body("\"" + ReservedLabels.ALL_MAILS.getLabelId() + "\".size", 
						equalTo((int) (allCounters.getTotalBytes().longValue() - fileSizeA))).
				body("\"" + ReservedLabels.ALL_MAILS.getLabelId() + "\".total", 
						equalTo((int) (allCounters.getTotalMessages().longValue() - 1))).
				body("\"" + ReservedLabels.ALL_MAILS.getLabelId() + "\".new", 
						equalTo((int) (allCounters.getNewMessages().longValue()))).
				body("\"" + ReservedLabels.SPAM.getLabelId() + "\".size", 
						equalTo((int) (spamCounters.getTotalBytes().longValue() - fileSizeA))).
				body("\"" + ReservedLabels.SPAM.getLabelId() + "\".total", 
						equalTo((int) (spamCounters.getTotalMessages().longValue() - 1))).
				body("\"" + ReservedLabels.SPAM.getLabelId() + "\".new", 
						equalTo((int) (spamCounters.getNewMessages().longValue()))).
			when().
				get(REST_PATH + "/mailbox?metadata=true").asString();

		logger.info("Counters Test OK");
	}

	/**
	 * Adds message throught REST API and returns new message UUID
	 * 
	 * @param messageFile
	 * @param labelId
	 * @return
	 * @throws IOException
	 */
	private UUID addMessage(String messageFile, Integer labelId) throws IOException
	{
		InputStream in = null;
		byte[] messageBytes;

		try {
			in = this.getClass().getResourceAsStream(messageFile);
			messageBytes = ByteStreams.toByteArray(in);
		} finally {
			in.close();
		}

		Response response = 
			given().
				pathParam("labelId", labelId).
				request().body(messageBytes).contentType(ContentType.BINARY).
			expect().
				statusCode(201).
			when().
				post(REST_PATH + "/mailbox/message?label={labelId}");

		return UUID.fromString( with(response.asString()).getString("id") );
	}

	/**
	 * Adds labels through REST API and returns new label ID
	 * 
	 * @param labelName
	 * @return
	 */
	private Integer addLabel(String labelName)
	{
		Response response = 
			given().
				pathParam("labelName", labelName).
			expect().
				statusCode(201).
			when().
				post(REST_PATH + "/mailbox/label?name={labelName}");

		return with(response.asString()).getInt("id");
	}

	/**
	 * Parses json response and returns {@link LabelCounters} for Label
	 * 
	 * @param json
	 * @param labelId
	 * @return
	 */
	private LabelCounters getCounters(String json, Integer labelId)
	{
		LabelCounters lc = new LabelCounters();
		String l = Integer.toString(labelId);

		lc.setTotalBytes( (long) from(json).getInt("\"" + l + "\".size") );
		lc.setNewMessages( (long) from(json).getInt("\"" + l + "\".new") );
		lc.setTotalMessages( (long) from(json).getInt("\"" + l + "\".total") );

		return lc;
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