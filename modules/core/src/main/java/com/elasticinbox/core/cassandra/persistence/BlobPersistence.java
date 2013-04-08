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

package com.elasticinbox.core.cassandra.persistence;

import java.util.UUID;

import com.elasticinbox.core.cassandra.CassandraDAOFactory;

import static com.elasticinbox.config.DatabaseConstants.BLOB_BLOCK_SIZE;
import static com.elasticinbox.core.cassandra.CassandraDAOFactory.CF_BLOB;
import static me.prettyprint.hector.api.factory.HFactory.createColumn;

import me.prettyprint.cassandra.serializers.BytesArraySerializer;
import me.prettyprint.cassandra.serializers.CompositeSerializer;
import me.prettyprint.cassandra.serializers.IntegerSerializer;
import me.prettyprint.cassandra.serializers.UUIDSerializer;
import me.prettyprint.hector.api.beans.Composite;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.ColumnQuery;
import me.prettyprint.hector.api.query.QueryResult;

/**
 * Blob block operations. Currently supports only single block blobs.
 * <p>
 * Do not batch read/write requests. Delete requests can be batched up when
 * multi-block operations introduced).
 * 
 * @author Rustam Aliyev
 */
public class BlobPersistence
{
	private final static IntegerSerializer intSe = IntegerSerializer.get();
	private final static BytesArraySerializer byteSe = BytesArraySerializer.get();
	private final static UUIDSerializer uuidSe = UUIDSerializer.get();

	/** Default sub-block ID. Currently only single sub-block operations supported. */
	public static int DEFAULT_SUB_BLOCK_ID = 0;

	/**
	 * Write blob block into Cassandra.
	 * 
	 * @param objectId Blob ID
	 * @param blockId Block ID (starting from 0)
	 * @param data
	 */
	public static void writeBlock(final UUID objectId, final int blockId, byte[] data)
	{
		if (data.length > BLOB_BLOCK_SIZE) {
			throw new IllegalArgumentException("Data (" + data.length
					+ " bytes) is larger than the maximum block size ("
					+ BLOB_BLOCK_SIZE + " bytes)");
		}

		Mutator<Composite> mutator = HFactory.createMutator(
				CassandraDAOFactory.getKeyspace(), CompositeSerializer.get());
		
		Composite key = new Composite();
		key.addComponent(objectId, uuidSe);
		key.addComponent(blockId, intSe);

		mutator.insert(key, CF_BLOB,
				createColumn(DEFAULT_SUB_BLOCK_ID, data, intSe, byteSe));
	}

	/**
	 * Read blob block from Cassandra.
	 * 
	 * @param objectId Blob ID
	 * @param blockId Block ID
	 * @return
	 */
	public static byte[] readBlock(final UUID objectId, final int blockId)
	{
		Composite key = new Composite();
		key.addComponent(objectId, uuidSe);
		key.addComponent(blockId, intSe);

		ColumnQuery<Composite, Integer, byte[]> q = HFactory.createColumnQuery(
				CassandraDAOFactory.getKeyspace(), CompositeSerializer.get(), intSe, byteSe);
		
		QueryResult<HColumn<Integer, byte[]>> result = q.setColumnFamily(CF_BLOB)
				.setKey(key).setName(DEFAULT_SUB_BLOCK_ID).execute();
		
		return result.get().getValue();
	}

	/**
	 * Delete blob block from Cassandra.
	 * 
	 * @param objectId Blob ID
	 * @param blockId Block ID
	 */
	public static void deleteBlock(final UUID objectId, final int blockId)
	{
		Composite key = new Composite();
		key.addComponent(objectId, uuidSe);
		key.addComponent(blockId, intSe);

		Mutator<Composite> mutator = HFactory.createMutator(
				CassandraDAOFactory.getKeyspace(), CompositeSerializer.get());

		mutator.delete(key, CF_BLOB, null, intSe);
	}
}