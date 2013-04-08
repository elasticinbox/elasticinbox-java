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

package com.elasticinbox.core.blob;

import static org.junit.Assert.*;

import java.net.URI;

import org.junit.Test;

import com.elasticinbox.core.blob.BlobURI;

public class BlobURIUnitTest
{
	@Test
	public void testFromURI1()
	{
		URI testUri = URI.create("blob://aws3-bucket/f1ca99e0-99a0-11e2-95f0-040cced3bd7a:myemail%40elasticinbox.com?c=dfl&e=ekey2");

		BlobURI bu = new BlobURI();
		bu.fromURI(testUri);
		
		assertEquals("aws3-bucket", bu.getProfile());
		assertEquals("f1ca99e0-99a0-11e2-95f0-040cced3bd7a:myemail@elasticinbox.com", bu.getName());
		assertEquals("dfl", bu.getCompression());
		assertEquals("ekey2", bu.getEncryptionKey());
		assertNull(bu.getBlockCount());
	}

	@Test
	public void testFromURI2()
	{
		URI testUri = URI.create("blob://db/f1ca99e0-99a0-11e2-95f0-040cced3bd7a?c=gz&b=1");

		BlobURI bu = new BlobURI();
		bu.fromURI(testUri);
		
		assertEquals("db", bu.getProfile());
		assertEquals("f1ca99e0-99a0-11e2-95f0-040cced3bd7a", bu.getName());
		assertEquals("gz", bu.getCompression());
		assertEquals(new Integer(1), bu.getBlockCount());
		assertNull(bu.getEncryptionKey());
	}

	@Test(expected=IllegalArgumentException.class)
	public void testFromBadSchemeURI()
	{
		URI testUri = URI.create("notblob://db/f1ca99e0-99a0-11e2-95f0-040cced3bd7a");
		new BlobURI().fromURI(testUri);
	}
	
	@Test
	public void testBuildURI()
	{
		URI testUri = URI.create("blob://my-azure-bs/f1ca99e0-99a0-11e2-95f0-040cced3bd7a?c=dfl&e=k2&b=100");

		BlobURI bu = new BlobURI()
				.setProfile("my-azure-bs")
				.setName("f1ca99e0-99a0-11e2-95f0-040cced3bd7a")
				.setBlockCount(100)
				.setCompression("dfl")
				.setEncryptionKey("k2");

		assertEquals(testUri, bu.buildURI());
	}

}
