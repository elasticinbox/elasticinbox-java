package com.elasticinbox.core.message;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Test;

import com.elasticinbox.core.model.Message;
import com.elasticinbox.core.model.MimePart;

public class MimeParserTest
{
	private final static String TEST_INLINE_ATTACH_FILE = "../../itests/src/test/resources/03-inline-attach.eml";

	/**
	 * Loop through message parts and check if retrieval by PartId is consistent
	 * with retrieval by ContentId.
	 * 
	 * @throws IOException
	 * @throws MimeParserException
	 */
	@Test
	public void testGetInputStreamByPartIdAndContentId() throws IOException, MimeParserException
	{
		File file = new File(TEST_INLINE_ATTACH_FILE);
		InputStream in = new FileInputStream(file);

		MimeParser mp = new MimeParser(in);
		Message message = mp.getMessage();
		
		Map<String, MimePart> parts = message.getParts();
		
		for (String partId : parts.keySet())
		{
			MimePart part = parts.get(partId);
			InputStream attachmentContentByPartId = mp.getInputStreamByPartId(partId);
			String attachmentHashByPartId = DigestUtils.md5Hex(attachmentContentByPartId);
			
			InputStream attachmentContentByContentId = mp.getInputStreamByContentId(part.getContentId());
			String attachmentHashByContentId = DigestUtils.md5Hex(attachmentContentByContentId);
			
			assertEquals(attachmentHashByPartId, attachmentHashByContentId);
		}
	}

}
