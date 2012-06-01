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

package com.elasticinbox.core.cassandra.utils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import me.prettyprint.hector.api.Serializer;

import org.apache.cassandra.thrift.Column;
import org.apache.cassandra.thrift.ColumnOrSuperColumn;
import org.apache.cassandra.thrift.CounterColumn;
import org.apache.cassandra.thrift.CounterSuperColumn;
import org.apache.cassandra.thrift.Deletion;
import org.apache.cassandra.thrift.Mutation;
import org.apache.cassandra.thrift.SuperColumn;

public final class ThrottlingBatchMutation<K>
{
	private final Map<ByteBuffer, Map<String, List<Mutation>>> mutationMap;
	private final Serializer<K> keySerializer;
	private final int batchSize;
	private AtomicInteger pendingMutationsCount = new AtomicInteger(0);

	public ThrottlingBatchMutation(Serializer<K> serializer, int batchSize)
	{
		this.keySerializer = serializer;
		this.batchSize = batchSize;
		mutationMap = new HashMap<ByteBuffer, Map<String, List<Mutation>>>(batchSize);
	}

	private ThrottlingBatchMutation(Serializer<K> serializer,
			Map<ByteBuffer, Map<String, List<Mutation>>> mutationMap, int batchSize)
	{
		this.keySerializer = serializer;
		this.mutationMap = mutationMap;
		this.batchSize = batchSize;
	}

	/**
	 * Add an Column insertion (or update) to the batch mutation request.
	 */
	public ThrottlingBatchMutation<K> addInsertion(K key,
			List<String> columnFamilies, Column column)
	{
		Mutation mutation = new Mutation();
		mutation.setColumn_or_supercolumn(new ColumnOrSuperColumn()
				.setColumn(column));
		addMutation(key, columnFamilies, mutation);
		return this;
	}

	/**
	 * Add a SuperColumn insertion (or update) to the batch mutation request.
	 */
	public ThrottlingBatchMutation<K> addSuperInsertion(K key,
			List<String> columnFamilies, SuperColumn superColumn)
	{
		Mutation mutation = new Mutation();
		mutation.setColumn_or_supercolumn(new ColumnOrSuperColumn()
				.setSuper_column(superColumn));
		addMutation(key, columnFamilies, mutation);
		return this;
	}

	/**
	 * Add a ColumnCounter insertion (or update)
	 */
	public ThrottlingBatchMutation<K> addCounterInsertion(K key,
			List<String> columnFamilies, CounterColumn counterColumn)
	{
		Mutation mutation = new Mutation();
		mutation.setColumn_or_supercolumn(new ColumnOrSuperColumn()
				.setCounter_column(counterColumn));
		addMutation(key, columnFamilies, mutation);
		return this;
	}

	/**
	 * Add a SuperColumnCounter insertion (or update)
	 */
	public ThrottlingBatchMutation<K> addSuperCounterInsertion(K key,
			List<String> columnFamilies, CounterSuperColumn counterSuperColumn)
	{
		Mutation mutation = new Mutation();
		mutation.setColumn_or_supercolumn(new ColumnOrSuperColumn()
				.setCounter_super_column(counterSuperColumn));
		addMutation(key, columnFamilies, mutation);
		return this;
	}

	/**
	 * Add a deletion request to the batch mutation.
	 */
	public ThrottlingBatchMutation<K> addDeletion(K key,
			List<String> columnFamilies, Deletion deletion)
	{
		Mutation mutation = new Mutation();
		mutation.setDeletion(deletion);
		addMutation(key, columnFamilies, mutation);
		return this;
	}

	private void addMutation(K key, List<String> columnFamilies, Mutation mutation)
	{
		Map<String, List<Mutation>> innerMutationMap = getInnerMutationMap(key);
		for (String columnFamily : columnFamilies) {
			List<Mutation> mutList = innerMutationMap.get(columnFamily);
			if (mutList == null) {
				mutList = new ArrayList<Mutation>(batchSize);
				innerMutationMap.put(columnFamily, mutList);
			}
			mutList.add(mutation);
			pendingMutationsCount.incrementAndGet();
		}
	}

	private Map<String, List<Mutation>> getInnerMutationMap(K key)
	{
		Map<String, List<Mutation>> innerMutationMap = mutationMap
				.get(keySerializer.toByteBuffer(key));
		if (innerMutationMap == null) {
			innerMutationMap = new HashMap<String, List<Mutation>>();
			mutationMap.put(keySerializer.toByteBuffer(key), innerMutationMap);
		}
		return innerMutationMap;
	}

	public Map<ByteBuffer, Map<String, List<Mutation>>> getMutationMap() {
		return mutationMap;
	}

	/**
	 * Makes a shallow copy of the mutation object.
	 * 
	 * @return
	 */
	public ThrottlingBatchMutation<K> makeCopy() {
		return new ThrottlingBatchMutation<K>(keySerializer, mutationMap, batchSize);
	}

	/**
	 * Checks whether the mutation object contains any mutations.
	 * 
	 * @return
	 */
	public boolean isEmpty() {
		return mutationMap.isEmpty();
	}

	/**
	 * Return the current size of the underlying map
	 * 
	 * @return
	 */
	public int getSize() {
		return mutationMap.size();
	}
	
	/**
	 * Return total number of total mutations 
	 * 
	 * @return
	 */
	public int getMutationsCount() {
		return pendingMutationsCount.get();
	}

}