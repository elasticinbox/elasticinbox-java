/**
 * Copyright (c) 2011-2013 Optimax Software Ltd.
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

import static org.hamcrest.Matchers.*;
import static com.jayway.restassured.RestAssured.*;
import static com.jayway.restassured.path.json.JsonPath.*;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.EagerSingleStagedReactorFactory;

import com.elasticinbox.core.model.LabelConstants;
import com.elasticinbox.core.model.LabelCounters;
import com.elasticinbox.core.model.Labels;
import com.elasticinbox.core.model.Marker;
import com.elasticinbox.core.model.ReservedLabels;
import com.google.common.io.ByteStreams;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;

/**
 * Integration test for REST APIs
 * 
 * @author Rustam Aliyev
 */
@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(EagerSingleStagedReactorFactory.class)
public class RestV2IT extends AbstractIntegrationTest
{
	@Test
	public void reservedLabelsTest()
	{
		initAccount();
		
		// check reserved labels
		expect().
			statusCode(200).and().
			body("'" + ReservedLabels.ALL_MAILS.getId() + "'",		equalTo(ReservedLabels.ALL_MAILS.getName())).
			body("'" + ReservedLabels.INBOX.getId() + "'",			equalTo(ReservedLabels.INBOX.getName())).
			body("'" + ReservedLabels.DRAFTS.getId() + "'",			equalTo(ReservedLabels.DRAFTS.getName())).
			body("'" + ReservedLabels.SENT.getId() + "'",			equalTo(ReservedLabels.SENT.getName())).
			body("'" + ReservedLabels.TRASH.getId() + "'",			equalTo(ReservedLabels.TRASH.getName())).
			body("'" + ReservedLabels.SPAM.getId() + "'",			equalTo(ReservedLabels.SPAM.getName())).
			body("'" + ReservedLabels.STARRED.getId() + "'",		equalTo(ReservedLabels.STARRED.getName())).
			body("'" + ReservedLabels.IMPORTANT.getId() + "'",		equalTo(ReservedLabels.IMPORTANT.getName())).
			body("'" + ReservedLabels.NOTIFICATIONS.getId() + "'",	equalTo(ReservedLabels.NOTIFICATIONS.getName())).
			body("'" + ReservedLabels.ATTACHMENTS.getId() + "'",	equalTo(ReservedLabels.ATTACHMENTS.getName())).
			body("'" + ReservedLabels.POP3.getId() + "'",			equalTo(ReservedLabels.POP3.getName())).
		when().
			get(REST_PATH + "/mailbox");
	}

	@Test
	public void reservedLabelsWithMetadataTest()
	{
		initAccount();

		// check labels metadata
		expect().
			statusCode(200).and().
			body("'" + ReservedLabels.ALL_MAILS.getId() + "'.size",	greaterThanOrEqualTo(0)).
			body("'" + ReservedLabels.INBOX.getId() + "'.name",		equalTo(ReservedLabels.INBOX.getName())).
			body("'" + ReservedLabels.INBOX.getId() + "'.unread",	greaterThanOrEqualTo(0)).
			body("'" + ReservedLabels.INBOX.getId() + "'.total",	greaterThanOrEqualTo(0)).
		when().
			get(REST_PATH + "/mailbox?metadata=true");
	}

	@Test
	public void labelListAddDeleteTest()
	{
		initAccount();

		final String labelA = "MyLabel";
		final String labelACase = "mYlaBel";
		final String labelB = "MyAnotherLabel";
		final String labelBCase = "MYanOTHerLABel";

		// add label
		Integer labelId = addLabel(labelA);

		// check added label
		expect().
			statusCode(200).and().
			body("'" + labelId + "'", equalTo(labelA)).
		when().
			get(REST_PATH + "/mailbox");

		logger.info("Add label test OK");

		// rename label, same name different letter cases
		given().
			pathParam("labelId", labelId).
		expect().
			statusCode(204).
		when().
			put(REST_PATH + "/mailbox/label/{labelId}?name=" + labelACase);

		// check renamed label
		expect().
			statusCode(200).and().
			body("'" + labelId + "'", equalTo(labelACase)).
		when().
			get(REST_PATH + "/mailbox");

		// rename label
		given().
			pathParam("labelId", labelId).
		expect().
			statusCode(204).
		when().
			put(REST_PATH + "/mailbox/label/{labelId}?name=" + labelB);

		// check renamed label
		expect().
			statusCode(200).and().
			body("'" + labelId + "'", equalTo(labelB)).
		when().
			get(REST_PATH + "/mailbox");
		
		logger.info("Rename label test OK");

		// adding existing label, should fail
		given().
			pathParam("labelName", labelB).
		expect().
			statusCode(409).
		when().
			post(REST_PATH + "/mailbox/label?name={labelName}");

		// adding existing label with different letter case, should fail
		given().
			pathParam("labelName", labelBCase).
		expect().
			statusCode(409).
		when().
			post(REST_PATH + "/mailbox/label?name={labelName}");

		// adding nested label for reserved label should fail
		given().
			pathParam("labelName", ReservedLabels.INBOX.getName() + 
					LabelConstants.NESTED_LABEL_SEPARATOR + "nestedLabel").
		expect().
			statusCode(400).
		when().
			post(REST_PATH + "/mailbox/label?name={labelName}");

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
			body("'" + labelId + "'", is(nullValue())).
		when().
			get(REST_PATH + "/mailbox").asString();

		logger.info("Delete label test OK");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void messageAddListViewDeletePurgeTest() throws IOException
	{
		initAccount();

		long fileSizeA = getResourceSize(EMAIL_REGULAR);

		String messageId = null;
		Map<String, UUID> messages = new HashMap<String, UUID>(2);
		Integer labelId = ReservedLabels.INBOX.getId(); 

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
		initAccount();

		// add labels
		Integer labelId1 = addLabel("CustomLabelTest3739");
		Integer labelId2 = addLabel("CustomLabelTest2398");

		// add message
		UUID messageId = addMessage(EMAIL_REGULAR, ReservedLabels.INBOX.getId());

		// assign labels and marker to the message
		given().
			pathParam("messageId", messageId.toString()).
			pathParam("labelId1", labelId1).
			pathParam("labelId2", labelId2).
			pathParam("seenMarker", Marker.SEEN.toString().toLowerCase()).
		expect().
			statusCode(204).
		when().
			put(REST_PATH + "/mailbox/message/{messageId}?addlabel={labelId1}&addlabel={labelId2}&addmarker={seenMarker}");

		// verify labels and marker
		given().
			pathParam("messageId", messageId.toString()).
		expect().
			statusCode(200).and().
			// TODO: uncomment when fixed http://code.google.com/p/rest-assured/issues/detail?id=169
			//body("message.labels", hasItems(0, 1, labelId1, labelId2)).
			body("message.labels", hasItems(0, 1, labelId2)).
			body("message.markers", hasItems(Marker.SEEN.toString().toUpperCase())).
		when().
			get(REST_PATH + "/mailbox/message/{messageId}");

		// assign/remove labels and markers to the message
		given().
			pathParam("messageId", messageId.toString()).
			pathParam("removeLabel", labelId1).
			pathParam("addLabel", ReservedLabels.SPAM.getId()).
			pathParam("removeMarker", Marker.SEEN.toString().toLowerCase()).
			pathParam("addMarker", Marker.REPLIED.toString().toLowerCase()).
		expect().
			statusCode(204).
		when().
			put(REST_PATH + "/mailbox/message/{messageId}?addlabel={addLabel}&removelabel={removeLabel}&removemarker={removeMarker}&addmarker={addMarker}");
		
		// verify labels and markers
		given().
			pathParam("messageId", messageId.toString()).
		expect().
			statusCode(200).and().
			body("message.labels", hasItems(0, 1, labelId2, ReservedLabels.SPAM.getId())).
			body("message.labels", not( hasItems(labelId1) )).
			body("message.markers", hasItems(Marker.REPLIED.toString().toUpperCase())).
			body("message.markers", not( hasItems(Marker.SEEN.toString().toUpperCase()) )).
		when().
			get(REST_PATH + "/mailbox/message/{messageId}");
	}

	@Test
	public void messageBatchMofifyDeleteTest() throws IOException
	{
		initAccount();

		// add labels
		Integer labelId1 = addLabel("BatchLabelTest0912");
		Integer labelId2 = addLabel("BatchLabelTest1290");

		// add messages
		UUID messageId1 = addMessage(EMAIL_REGULAR, ReservedLabels.INBOX.getId());
		UUID messageId2 = addMessage(EMAIL_LARGE_ATT, ReservedLabels.INBOX.getId());

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
			pathParam("labelId", ReservedLabels.ALL_MAILS.getId()).
		expect().
			statusCode(200).and().
			body(messageId1.toString() + ".labels", hasItems(0, 1, labelId2)).
			body(messageId2.toString() + ".labels", hasItems(0, 1, labelId2)).
			// TODO: uncomment when fixed http://code.google.com/p/rest-assured/issues/detail?id=169
			//body(messageId1.toString() + ".labels", hasItems(0, 1, labelId1, labelId2)).
			//body(messageId2.toString() + ".labels", hasItems(0, 1, labelId1, labelId2)).
			body(messageId1.toString() + ".markers", hasItems(Marker.SEEN.toString().toUpperCase())).
			body(messageId2.toString() + ".markers", hasItems(Marker.SEEN.toString().toUpperCase())).
		when().
			get(REST_PATH + "/mailbox/label/{labelId}?metadata=true");

		// batch delete messages
		given().
			request().body("[\"" + messageId1.toString() + "\", \"" + messageId2.toString() + "\"]").contentType(ContentType.JSON).
		expect().
			statusCode(204).
		when().
			delete(REST_PATH + "/mailbox/message");

		// verify batch delete
		given().
			pathParam("labelId", ReservedLabels.ALL_MAILS.getId()).
		expect().
			statusCode(200).and().
			body(messageId1.toString(), is(nullValue())).
			body(messageId2.toString(), is(nullValue())).
		when().
			get(REST_PATH + "/mailbox/label/{labelId}?metadata=true");
	}

	@Test
	public void messageAttachmentAndRawTest() throws IOException
	{
		initAccount();

		// add message
		UUID messageId = addMessage(EMAIL_LARGE_ATT, ReservedLabels.INBOX.getId());
		//long fileSize = getResourceSize(EMAIL_LARGE_ATT);

		// get attachment by part id
		given().
			pathParam("messageId", messageId.toString()).
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
			pathParam("messageId", messageId.toString()).
			pathParam("contentId", "<image-001>").
		expect().
			statusCode(200).and().
			header("Content-Type", equalTo("image/png")).
			//header("Content-Length", equalTo("27136")).
			header("Content-Disposition", containsString("attachment; filename=")).
		when().
			get(REST_PATH + "/mailbox/message/{messageId}/{contentId}");

		// get uncompressed raw message
		// TODO: currently this tests does not validate size due to bug in RESTAssured.
		//       see {@link http://code.google.com/p/rest-assured/issues/detail?id=154}
		given().
			header("Accept-Encoding", "none").
			pathParam("messageId", messageId.toString()).
		expect().
			statusCode(200).and().
			header("Content-Type", equalTo("text/plain")).
//			header("Content-Length", equalTo(String.valueOf(fileSize))).
//			header("Content-Encoding", not(equalTo("deflate"))).
		when().
			get(REST_PATH + "/mailbox/message/{messageId}/raw");

		// get compressed raw message
		given().
			header("Accept-Encoding", "deflate").
			pathParam("messageId", messageId.toString()).
		expect().
			statusCode(200).and().
			header("Content-Encoding", equalTo("deflate")).
			header("Content-Type", equalTo("text/plain")).
		when().
			get(REST_PATH + "/mailbox/message/{messageId}/raw");
	}

	@Test
	public void messageUpdateTest() throws IOException
	{
		initAccount();

		// add message
		UUID messageId = addMessage(EMAIL_LARGE_ATT, ReservedLabels.INBOX.getId());

		// add labels and markers
		given().
			pathParam("messageId", messageId.toString()).
			pathParam("labelId1", ReservedLabels.IMPORTANT.getId()).
			pathParam("marker1", Marker.SEEN.toString().toLowerCase()).
		expect().
			statusCode(204).
		when().
			put(REST_PATH + "/mailbox/message/{messageId}?addlabel={labelId1}&addmarker={marker1}");

		// TODO: merge labelId2 to above request when fixed http://code.google.com/p/rest-assured/issues/detail?id=169
		given().
			pathParam("messageId", messageId.toString()).
			pathParam("labelId2", ReservedLabels.STARRED.getId()).
		expect().
			statusCode(204).
		when().
			put(REST_PATH + "/mailbox/message/{messageId}?addlabel={labelId2}");

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
			body("message.labels", hasItems(ReservedLabels.IMPORTANT.getId(), ReservedLabels.STARRED.getId())).
			body("message.markers", hasItems(Marker.SEEN.toString().toUpperCase())).
		when().
			get(REST_PATH + "/mailbox/message/{messageId}");
	}
	
	@Test
	public void countersTest() throws IOException
	{
		initAccount();

		LabelCounters allCounters = new LabelCounters();
		LabelCounters inboxCounters = new LabelCounters();
		LabelCounters spamCounters = new LabelCounters();

		// check label counter before message added
		String jsonResponse = expect().statusCode(200).when().get(REST_PATH + "/mailbox?metadata=true").asString();
		allCounters = getCounters(jsonResponse, ReservedLabels.ALL_MAILS.getId());
		inboxCounters = getCounters(jsonResponse, ReservedLabels.INBOX.getId());
		spamCounters = getCounters(jsonResponse, ReservedLabels.SPAM.getId());

		// add message A
		long fileSizeA = getResourceSize(EMAIL_REGULAR);
		UUID messageIdA = addMessage(EMAIL_REGULAR, ReservedLabels.INBOX.getId());

		// check label counters
		jsonResponse = 
			expect().
				statusCode(200).and().
				body("'" + ReservedLabels.ALL_MAILS.getId() + "'.size", 
						equalTo((int) (allCounters.getTotalBytes().longValue() + fileSizeA))).
				body("'" + ReservedLabels.ALL_MAILS.getId() + "'.total", 
						equalTo((int) (allCounters.getTotalMessages().longValue() + 1))).
				body("'" + ReservedLabels.ALL_MAILS.getId() + "'.unread", 
						equalTo((int) (allCounters.getUnreadMessages().longValue() + 1))).
				body("'" + ReservedLabels.INBOX.getId() + "'.total", 
						equalTo((int) (inboxCounters.getTotalMessages().longValue() + 1))).
				body("'" + ReservedLabels.INBOX.getId() + "'.unread", 
						equalTo((int) (inboxCounters.getUnreadMessages().longValue() + 1))).
			when().
				get(REST_PATH + "/mailbox?metadata=true").asString();

		// add label SPAM to message A
		given().
			pathParam("messageId", messageIdA.toString()).
			pathParam("labelId", ReservedLabels.SPAM.getId()).
		expect().
			statusCode(204).
		when().
			put(REST_PATH + "/mailbox/message/{messageId}?addlabel={labelId}");

		// check label counters
		jsonResponse = 
			expect().
				statusCode(200).and().
				body("'" + ReservedLabels.SPAM.getId() + "'.total", 
						equalTo((int) (spamCounters.getTotalMessages().longValue() + 1))).
				body("'" + ReservedLabels.SPAM.getId() + "'.unread", 
						equalTo((int) (spamCounters.getUnreadMessages().longValue() + 1))).
			when().
				get(REST_PATH + "/mailbox?metadata=true").asString();

		// update counters
		allCounters = getCounters(jsonResponse, ReservedLabels.ALL_MAILS.getId());
		inboxCounters = getCounters(jsonResponse, ReservedLabels.INBOX.getId());
		spamCounters = getCounters(jsonResponse, ReservedLabels.SPAM.getId());

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
				body("'" + ReservedLabels.ALL_MAILS.getId() + "'.unread", 
						equalTo((int) (allCounters.getUnreadMessages().longValue() - 1))).
				body("'" + ReservedLabels.INBOX.getId() + "'.unread", 
						equalTo((int) (inboxCounters.getUnreadMessages().longValue() - 1))).
				body("'" + ReservedLabels.SPAM.getId() + "'.unread", 
						equalTo((int) (spamCounters.getUnreadMessages().longValue() - 1))).
			when().
				get(REST_PATH + "/mailbox?metadata=true").asString();

		// update counters
		allCounters = getCounters(jsonResponse, ReservedLabels.ALL_MAILS.getId());
		inboxCounters = getCounters(jsonResponse, ReservedLabels.INBOX.getId());
		spamCounters = getCounters(jsonResponse, ReservedLabels.SPAM.getId());

		// remove label INBOX from message A
		given().
			pathParam("messageId", messageIdA.toString()).
			pathParam("labelId", ReservedLabels.INBOX.getId()).
		expect().
			statusCode(204).
		when().
			put(REST_PATH + "/mailbox/message/{messageId}?removelabel={labelId}");

		// check label counters
		jsonResponse = 
			expect().
				statusCode(200).and().
				body("'" + ReservedLabels.ALL_MAILS.getId() + "'.size", 
						equalTo((int) (allCounters.getTotalBytes().longValue()))).
				body("'" + ReservedLabels.ALL_MAILS.getId() + "'.total", 
						equalTo((int) (allCounters.getTotalMessages().longValue()))).
				body("'" + ReservedLabels.ALL_MAILS.getId() + "'.unread", 
						equalTo((int) (allCounters.getUnreadMessages().longValue()))).
				body("'" + ReservedLabels.INBOX.getId() + "'.total", 
						equalTo((int) (inboxCounters.getTotalMessages().longValue() - 1))).
				body("'" + ReservedLabels.INBOX.getId() + "'.unread", 
						equalTo((int) (inboxCounters.getUnreadMessages().longValue()))).
				body("'" + ReservedLabels.SPAM.getId() + "'.total", 
						equalTo((int) (spamCounters.getTotalMessages().longValue()))).
				body("'" + ReservedLabels.SPAM.getId() + "'.unread", 
						equalTo((int) (spamCounters.getUnreadMessages().longValue()))).
			when().
				get(REST_PATH + "/mailbox?metadata=true").asString();

		// update counters
		allCounters = getCounters(jsonResponse, ReservedLabels.ALL_MAILS.getId());
		inboxCounters = getCounters(jsonResponse, ReservedLabels.INBOX.getId());
		spamCounters = getCounters(jsonResponse, ReservedLabels.SPAM.getId());

		// add message B to SPAM
		long fileSizeB = getResourceSize(EMAIL_LARGE_ATT);
		addMessage(EMAIL_LARGE_ATT, ReservedLabels.SPAM.getId());

		// check label counters
		jsonResponse = 
			expect().
				statusCode(200).and().
				body("'" + ReservedLabels.ALL_MAILS.getId() + "'.size", 
						equalTo((int) (allCounters.getTotalBytes().longValue() + fileSizeB))).
				body("'" + ReservedLabels.ALL_MAILS.getId() + "'.total", 
						equalTo((int) (allCounters.getTotalMessages().longValue() + 1))).
				body("'" + ReservedLabels.ALL_MAILS.getId() + "'.unread", 
						equalTo((int) (allCounters.getUnreadMessages().longValue() + 1))).
				body("'" + ReservedLabels.INBOX.getId() + "'.total", 
						equalTo((int) (inboxCounters.getTotalMessages().longValue()))).
				body("'" + ReservedLabels.INBOX.getId() + "'.unread", 
						equalTo((int) (inboxCounters.getUnreadMessages().longValue()))).
				body("'" + ReservedLabels.SPAM.getId() + "'.total", 
						equalTo((int) (spamCounters.getTotalMessages().longValue() + 1))).
				body("'" + ReservedLabels.SPAM.getId() + "'.unread", 
						equalTo((int) (spamCounters.getUnreadMessages().longValue() + 1))).
			when().
				get(REST_PATH + "/mailbox?metadata=true").asString();

		// update counters
		allCounters = getCounters(jsonResponse, ReservedLabels.ALL_MAILS.getId());
		inboxCounters = getCounters(jsonResponse, ReservedLabels.INBOX.getId());
		spamCounters = getCounters(jsonResponse, ReservedLabels.SPAM.getId());

		// remove message A
		given().
			pathParam("messageId", messageIdA.toString()).
		expect().
			statusCode(204).
		when().
			delete(REST_PATH + "/mailbox/message/{messageId}");

		// check label counters
		jsonResponse = 
			expect().
				statusCode(200).and().
				body("'" + ReservedLabels.ALL_MAILS.getId() + "'.size", 
						equalTo((int) (allCounters.getTotalBytes().longValue() - fileSizeA))).
				body("'" + ReservedLabels.ALL_MAILS.getId() + "'.total", 
						equalTo((int) (allCounters.getTotalMessages().longValue() - 1))).
				body("'" + ReservedLabels.ALL_MAILS.getId() + "'.unread", 
						equalTo((int) (allCounters.getUnreadMessages().longValue()))).
				body("'" + ReservedLabels.SPAM.getId() + "'.total", 
						equalTo((int) (spamCounters.getTotalMessages().longValue() - 1))).
				body("'" + ReservedLabels.SPAM.getId() + "'.unread", 
						equalTo((int) (spamCounters.getUnreadMessages().longValue()))).
			when().
				get(REST_PATH + "/mailbox?metadata=true").asString();

		logger.info("Counters Test OK");
	}
	
	@Test
	public void mailboxScrubTest() throws IOException
	{
		initAccount();
		
		Pair<Labels, Map<Integer, List<UUID>>> pair = populateMailbox();
		Labels labels = pair.getLeft();
		Map<Integer, List<UUID>> messages = pair.getRight();
		
		// TODO: wipe off counters and indexes here. need to communicate with metadata store directly

		// check label counters before scrub
		expect().
			statusCode(200).and().
			body("'" + ReservedLabels.INBOX.getId() + "'.total", 
					equalTo(labels.getLabelCounters(ReservedLabels.INBOX.getId()).getTotalMessages().intValue())).
			body("'" + ReservedLabels.INBOX.getId() + "'.unread", 
					equalTo(labels.getLabelCounters(ReservedLabels.INBOX.getId()).getUnreadMessages().intValue())).
			body("'" + ReservedLabels.NOTIFICATIONS.getId() + "'.total", 
					equalTo(labels.getLabelCounters(ReservedLabels.NOTIFICATIONS.getId()).getTotalMessages().intValue())).
			body("'" + ReservedLabels.NOTIFICATIONS.getId() + "'.unread", 
					equalTo(labels.getLabelCounters(ReservedLabels.NOTIFICATIONS.getId()).getUnreadMessages().intValue())).
			body("'" + ReservedLabels.SPAM.getId() + "'.total", 
					equalTo(labels.getLabelCounters(ReservedLabels.SPAM.getId()).getTotalMessages().intValue())).
			body("'" + ReservedLabels.SPAM.getId() + "'.unread", 
					equalTo(labels.getLabelCounters(ReservedLabels.SPAM.getId()).getUnreadMessages().intValue())).
			body("'" + ReservedLabels.TRASH.getId() + "'.total", 
					equalTo(labels.getLabelCounters(ReservedLabels.TRASH.getId()).getTotalMessages().intValue())).
			body("'" + ReservedLabels.TRASH.getId() + "'.unread", 
					equalTo(labels.getLabelCounters(ReservedLabels.TRASH.getId()).getUnreadMessages().intValue())).
		when().
			get(REST_PATH + "/mailbox?metadata=true").asString();

		// scrub label counters
		expect().
			statusCode(204).
		when().
			post(REST_PATH + "/mailbox/scrub/counters");

		// check label counters after scrub
		expect().
			statusCode(200).and().
			body("'" + ReservedLabels.INBOX.getId() + "'.total", 
					equalTo(labels.getLabelCounters(ReservedLabels.INBOX.getId()).getTotalMessages().intValue())).
			body("'" + ReservedLabels.INBOX.getId() + "'.unread", 
					equalTo(labels.getLabelCounters(ReservedLabels.INBOX.getId()).getUnreadMessages().intValue())).
			body("'" + ReservedLabels.NOTIFICATIONS.getId() + "'.total", 
					equalTo(labels.getLabelCounters(ReservedLabels.NOTIFICATIONS.getId()).getTotalMessages().intValue())).
			body("'" + ReservedLabels.NOTIFICATIONS.getId() + "'.unread", 
					equalTo(labels.getLabelCounters(ReservedLabels.NOTIFICATIONS.getId()).getUnreadMessages().intValue())).
			body("'" + ReservedLabels.SPAM.getId() + "'.total", 
					equalTo(labels.getLabelCounters(ReservedLabels.SPAM.getId()).getTotalMessages().intValue())).
			body("'" + ReservedLabels.SPAM.getId() + "'.unread", 
					equalTo(labels.getLabelCounters(ReservedLabels.SPAM.getId()).getUnreadMessages().intValue())).
			body("'" + ReservedLabels.TRASH.getId() + "'.total", 
					equalTo(labels.getLabelCounters(ReservedLabels.TRASH.getId()).getTotalMessages().intValue())).
			body("'" + ReservedLabels.TRASH.getId() + "'.unread", 
					equalTo(labels.getLabelCounters(ReservedLabels.TRASH.getId()).getUnreadMessages().intValue())).
		when().
			get(REST_PATH + "/mailbox?metadata=true").asString();
		
		// scrub label indexes
		expect().
			statusCode(204).
		when().
			post(REST_PATH + "/mailbox/scrub/indexes");
		
		//check indexes
		for (int labelId : new int[] { 
					ReservedLabels.INBOX.getId(), ReservedLabels.NOTIFICATIONS.getId(), 
					ReservedLabels.SPAM.getId(), ReservedLabels.TRASH.getId()} )
		{
			given().
				pathParam("labelId", labelId).
			expect().
				statusCode(200).and().
				body("", hasItems(asStringArray(messages.get(labelId)))).
				body("", hasSize(messages.get(labelId).size())).
			when().
				get(REST_PATH + "/mailbox/label/{labelId}");
		}
}
	
	/**
	 * Populates mailbox with messages and returns expected counters and message IDs.
	 * 
	 * @return
	 * @throws IOException
	 */
	protected static Pair<Labels, Map<Integer, List<UUID>>> populateMailbox() throws IOException
	{
		Map<Integer, List<UUID>> messages = new HashMap<Integer, List<UUID>>();
		Labels labels = new Labels();
		LabelCounters inboxCounters = new LabelCounters();
		LabelCounters notifCounters = new LabelCounters();
		LabelCounters trashCounters = new LabelCounters();
		LabelCounters spamCounters = new LabelCounters();

		// INBOX: add 5 messages, mark 2 as unread
		messages.put(ReservedLabels.INBOX.getId(), new ArrayList<UUID>());
		List<UUID> inboxMessages = messages.get(ReservedLabels.INBOX.getId());
		inboxMessages.add(addMessage(EMAIL_REGULAR, ReservedLabels.INBOX.getId()));
		inboxMessages.add(addMessage(EMAIL_REGULAR, ReservedLabels.INBOX.getId()));
		inboxMessages.add(addMessage(EMAIL_REGULAR, ReservedLabels.INBOX.getId()));
		inboxMessages.add(addMessage(EMAIL_REGULAR, ReservedLabels.INBOX.getId()));
		inboxMessages.add(addMessage(EMAIL_REGULAR, ReservedLabels.INBOX.getId()));
		markAsRead(inboxMessages.get(3));
		markAsRead(inboxMessages.get(4));
		inboxCounters.setTotalMessages(5L);
		inboxCounters.setUnreadMessages(3L);
		labels.incrementCounters(ReservedLabels.INBOX.getId(), inboxCounters);

		// NOTIFICATIONS: add 3 messages, mark 1 as read
		messages.put(ReservedLabels.NOTIFICATIONS.getId(), new ArrayList<UUID>());
		List<UUID> notifMessages = messages.get(ReservedLabels.NOTIFICATIONS.getId());
		notifMessages.add(addMessage(EMAIL_REGULAR, ReservedLabels.NOTIFICATIONS.getId()));
		notifMessages.add(addMessage(EMAIL_REGULAR, ReservedLabels.NOTIFICATIONS.getId()));
		notifMessages.add(addMessage(EMAIL_REGULAR, ReservedLabels.NOTIFICATIONS.getId()));
		markAsRead(notifMessages.get(1));
		notifCounters.setTotalMessages(3L);
		notifCounters.setUnreadMessages(2L);
		labels.incrementCounters(ReservedLabels.NOTIFICATIONS.getId(), notifCounters);

		// SPAM: add 5 messages, keep all unread
		messages.put(ReservedLabels.SPAM.getId(), new ArrayList<UUID>());
		List<UUID> spamMessages = messages.get(ReservedLabels.SPAM.getId());
		spamMessages.add(addMessage(EMAIL_REGULAR, ReservedLabels.SPAM.getId()));
		spamMessages.add(addMessage(EMAIL_REGULAR, ReservedLabels.SPAM.getId()));
		spamMessages.add(addMessage(EMAIL_REGULAR, ReservedLabels.SPAM.getId()));
		spamMessages.add(addMessage(EMAIL_REGULAR, ReservedLabels.SPAM.getId()));
		spamMessages.add(addMessage(EMAIL_REGULAR, ReservedLabels.SPAM.getId()));
		spamCounters.setTotalMessages(5L);
		spamCounters.setUnreadMessages(5L);
		labels.incrementCounters(ReservedLabels.SPAM.getId(), spamCounters);

		// TRASH: add 4 messages, mark 2 as read
		messages.put(ReservedLabels.TRASH.getId(), new ArrayList<UUID>());
		List<UUID> trashMessages = messages.get(ReservedLabels.TRASH.getId());
		trashMessages.add(addMessage(EMAIL_REGULAR, ReservedLabels.TRASH.getId()));
		trashMessages.add(addMessage(EMAIL_REGULAR, ReservedLabels.TRASH.getId()));
		trashMessages.add(addMessage(EMAIL_REGULAR, ReservedLabels.TRASH.getId()));
		trashMessages.add(addMessage(EMAIL_REGULAR, ReservedLabels.TRASH.getId()));
		markAsRead(trashMessages.get(0));
		markAsRead(trashMessages.get(2));
		trashCounters.setTotalMessages(4L);
		trashCounters.setUnreadMessages(2L);
		labels.incrementCounters(ReservedLabels.TRASH.getId(), trashCounters);

		return Pair.of(labels, messages);
	}

	/**
	 * Adds message throught REST API and returns new message UUID
	 * 
	 * @param messageFile
	 * @param labelId
	 * @return
	 * @throws IOException
	 */
	protected static UUID addMessage(String messageFile, Integer labelId) throws IOException
	{
		InputStream in = null;
		byte[] messageBytes;

		try {
			in = RestV2IT.class.getResourceAsStream(messageFile);
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
	 * Mark given message as read (add SEEN marker)
	 * 
	 * @param messageId
	 */
	private static void markAsRead(UUID messageId)
	{
		// assign labels and marker to the message
		given().
			pathParam("messageId", messageId.toString()).
			pathParam("seenMarker", Marker.SEEN.toString().toLowerCase()).
		expect().
			statusCode(204).
		when().
			put(REST_PATH + "/mailbox/message/{messageId}?addmarker={seenMarker}");
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

		lc.setUnreadMessages( (long) from(json).getInt("'" + l + "'.unread") );
		lc.setTotalMessages( (long) from(json).getInt("'" + l + "'.total") );
		// size returned only for ALL_MAILS
		if(labelId == ReservedLabels.ALL_MAILS.getId()) {
			lc.setTotalBytes( (long) from(json).getInt("'" + l + "'.size") );
		}

		return lc;
	}

	/**
	 * Convert List<T> to String array by calling toString() on all elements.
	 * 
	 * @param list
	 * @return
	 */
	private static <T> String[] asStringArray(List<T> list)
	{
		String result[] = new String[list.size()];
		
		for (int i = 0; i < list.size(); i++) {
			result[i] = list.get(i).toString();
		}
		
		return result;
	}
}