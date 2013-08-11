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

import java.util.Arrays;

import org.apache.cassandra.thrift.Cassandra;
import org.apache.cassandra.thrift.Deletion;
import org.apache.cassandra.thrift.SlicePredicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.prettyprint.cassandra.model.ExecutingKeyspace;
import me.prettyprint.cassandra.model.HColumnImpl;
import me.prettyprint.cassandra.model.HCounterColumnImpl;
import me.prettyprint.cassandra.model.HCounterSuperColumnImpl;
import me.prettyprint.cassandra.model.HSuperColumnImpl;
import me.prettyprint.cassandra.model.KeyspaceOperationCallback;
import me.prettyprint.cassandra.model.thrift.ThriftConverter;
import me.prettyprint.cassandra.model.thrift.ThriftFactory;
import me.prettyprint.cassandra.serializers.TypeInferringSerializer;
import me.prettyprint.cassandra.service.KeyspaceService;
import me.prettyprint.cassandra.service.Operation;
import me.prettyprint.cassandra.service.OperationType;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.Serializer;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.HCounterColumn;
import me.prettyprint.hector.api.beans.HCounterSuperColumn;
import me.prettyprint.hector.api.beans.HSuperColumn;
import me.prettyprint.hector.api.exceptions.HectorException;
import me.prettyprint.hector.api.mutation.MutationResult;
import me.prettyprint.hector.api.mutation.Mutator;

public final class ThrottlingMutator<K> implements Mutator<K>
{
	private final static Logger logger = 
			LoggerFactory.getLogger(ThrottlingMutator.class);

	private final static int DEFAULT_BATCH_SIZE = 50;

	protected final Serializer<K> keySerializer;
	private final ExecutingKeyspace keyspace;
	private final int batchSize;
	private final Long batchInterval;
	private ThrottlingBatchMutation<K> pendingMutations;

	/**
	 * Throttling mutator executes operations at the given rate (rate =
	 * ops/interval)
	 * 
	 * Default batch size is 50 operations. If no time interval specified,
	 * operations will be executed in batches without delay.
	 * 
	 * @param keyspace
	 * @param keySerializer
	 * @param batchSize
	 *            Number of operations per interval
	 * @param batchInterval
	 *            Time interval
	 */
	public ThrottlingMutator(Keyspace keyspace, Serializer<K> keySerializer, int batchSize, Long batchInterval) {
		this.keyspace = (ExecutingKeyspace) keyspace;
		this.keySerializer = keySerializer;
		this.batchInterval = batchInterval;
		this.batchSize = batchSize;
	}

	public ThrottlingMutator(Keyspace keyspace, Serializer<K> keySerializer, int batchSize) {
		this(keyspace, keySerializer, batchSize, null);
	}

	public ThrottlingMutator(Keyspace keyspace, Serializer<K> keySerializer) {
		this(keyspace, keySerializer, DEFAULT_BATCH_SIZE, null);
	}

	public ThrottlingMutator(Keyspace keyspace, int batchSize) {
		this(keyspace, TypeInferringSerializer.<K> get(), batchSize);
	}

	public ThrottlingMutator(Keyspace keyspace) {
		this(keyspace, TypeInferringSerializer.<K> get());
	}

	// Simple and immediate insertion of a column
	@Override
	public <N, V> MutationResult insert(final K key, final String cf, final HColumn<N, V> c) {
		addInsertion(key, cf, c);
		return execute();
	}

	// overloaded insert-super
	@Override
	public <SN, N, V> MutationResult insert(final K key, final String cf, final HSuperColumn<SN, N, V> superColumn) {
		addInsertion(key, cf, superColumn);
		return execute();
	}

	@Override
	public <N> MutationResult delete(final K key, final String cf, final N columnName, final Serializer<N> nameSerializer) {
		addDeletion(key, cf, columnName, nameSerializer);
		return execute();
	}

	@Override
	public <N> MutationResult delete(K key, String cf, N columnName, Serializer<N> nameSerializer, long clock) {
		addDeletion(key, cf, columnName, nameSerializer, clock);
		return execute();
	}

	/**
	 * Deletes a subcolumn of a supercolumn
	 * 
	 * @param <SN>
	 *            super column type
	 * @param <N>
	 *            subcolumn type
	 */
	@Override
	public <SN, N> MutationResult subDelete(final K key, final String cf,
			final SN supercolumnName, final N columnName,
			final Serializer<SN> sNameSerializer,
			final Serializer<N> nameSerializer)
	{
		return new ThrottlingMutationResult(keyspace.doExecute(new KeyspaceOperationCallback<Void>() {
				@Override
				public Void doInKeyspace(KeyspaceService ks) throws HectorException {
					ks.remove(keySerializer.toByteBuffer(key), ThriftFactory.createSuperColumnPath(cf,
							supercolumnName, columnName, sNameSerializer, nameSerializer));
					return null;
				}
		}));
	}

	@Override
	public <SN> MutationResult superDelete(final K key, final String cf,
			final SN supercolumnName, final Serializer<SN> sNameSerializer)
	{
		return new ThrottlingMutationResult(keyspace.doExecute(new KeyspaceOperationCallback<Void>() {
				@Override
				public Void doInKeyspace(KeyspaceService ks) throws HectorException {
					// Remove a Super Column.
					ks.remove(keySerializer.toByteBuffer(key),
							ThriftFactory.createSuperColumnPath(cf, supercolumnName, sNameSerializer));
					return null;
				}
		}));
	}

	/**
	 * Deletes the columns defined in the HSuperColumn. If there are no HColumns
	 * attached, we delete the whole thing.
	 * 
	 */
	public <SN, N, V> Mutator<K> addSubDelete(K key, String cf, HSuperColumn<SN, N, V> sc) {
		return addSubDelete(key, cf, sc, keyspace.createClock());
	}

	public <SN, N, V> Mutator<K> addSubDelete(K key, String cf, HSuperColumn<SN, N, V> sc, long clock)
	{
		Deletion d = new Deletion().setTimestamp(clock);

		if (sc.getColumns() != null) {
			SlicePredicate pred = new SlicePredicate();
			for (HColumn<N, V> col : sc.getColumns()) {
				pred.addToColumn_names(col.getNameSerializer().toByteBuffer(
						col.getName()));
			}
			d.setPredicate(pred);
		}

		d.setSuper_column(sc.getNameByteBuffer());
		getPendingMutations().addDeletion(key, Arrays.asList(cf), d);

		return this;
	}

	// schedule an insertion to be executed in batch by the execute method
	// CAVEAT: a large number of calls with a typo in one of them will leave things in an
	// indeterminant state if we dont validate against LIVE (but cached of course)
	// keyspaces and CFs on each add/delete call
	// also, should throw a typed StatementValidationException or similar perhaps?
	@Override
	public <N, V> Mutator<K> addInsertion(K key, String cf, HColumn<N, V> c) {
		getPendingMutations().addInsertion(key, Arrays.asList(cf),
				((HColumnImpl<N, V>) c).toThrift());
		return this;
	}

	/**
	 * Schedule an insertion of a supercolumn to be inserted in batch mode by
	 * {@link #execute()}
	 */
	@Override
	public <SN, N, V> Mutator<K> addInsertion(K key, String cf, HSuperColumn<SN, N, V> sc) {
		getPendingMutations().addSuperInsertion(key, Arrays.asList(cf),
				((HSuperColumnImpl<SN, N, V>) sc).toThrift());
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <N> Mutator<K> addDeletion(K key, String cf, N columnName, Serializer<N> nameSerializer) {
		addDeletion(key, cf, columnName, nameSerializer, keyspace.createClock());
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <N> Mutator<K> addDeletion(K key, String cf) {
		addDeletion(key, cf, null, null, keyspace.createClock());
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <N> Mutator<K> addDeletion(K key, String cf, long clock) {
		addDeletion(key, cf, null, null, clock);
		return this;
	}

	@Override
	public <N> Mutator<K> addDeletion(Iterable<K> keys, String cf) {
		return addDeletion(keys, cf, keyspace.createClock());
	}

	@Override
	public <N> Mutator<K> addDeletion(Iterable<K> keys, String cf, long clock) {
		for (K key : keys) {
			addDeletion(key, cf, null, null, clock);
		}
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <N> Mutator<K> addDeletion(K key, String cf, N columnName,
			Serializer<N> nameSerializer, long clock) {
		Deletion d;
		if (columnName != null) {
			SlicePredicate sp = new SlicePredicate();
			sp.addToColumn_names(nameSerializer.toByteBuffer(columnName));
			d = new Deletion().setTimestamp(clock).setPredicate(sp);
		} else {
			d = new Deletion().setTimestamp(clock);
		}
		getPendingMutations().addDeletion(key, Arrays.asList(cf), d);
		return this;
	}

	// Counters support.

	@Override
	public <N> MutationResult insertCounter(final K key, final String cf,
			final HCounterColumn<N> c) {
		addCounter(key, cf, c);
		return execute();
	}

	@Override
	public <N> MutationResult incrementCounter(final K key, final String cf,
			final N columnName, final long increment) {
		return insertCounter(key, cf, new HCounterColumnImpl<N>(columnName,
				increment, TypeInferringSerializer.<N> get()));
	}

	@Override
	public <N> MutationResult decrementCounter(final K key, final String cf,
			final N columnName, final long increment) {
		return incrementCounter(key, cf, columnName, increment * -1L);
	}

	@Override
	public <SN, N> MutationResult insertCounter(K key, String cf,
			HCounterSuperColumn<SN, N> superColumn) {
		addCounter(key, cf, superColumn);
		return execute();
	}

	@Override
	public <N> MutationResult deleteCounter(final K key, final String cf,
			final N counterColumnName, final Serializer<N> nameSerializer) {
		addCounterDeletion(key, cf, counterColumnName, nameSerializer);
		return execute();
	}

	@Override
	public <SN, N> MutationResult subDeleteCounter(final K key,
			final String cf, final SN supercolumnName, final N columnName,
			final Serializer<SN> sNameSerializer,
			final Serializer<N> nameSerializer) {
		addCounterSubDeletion(key, cf, new HCounterSuperColumnImpl<SN, N>(
				sNameSerializer, nameSerializer).setName(supercolumnName)
				.addSubCounterColumn(new HCounterColumnImpl<N>(nameSerializer)));
		return execute();
	}

	@Override
	public <N> Mutator<K> addCounter(K key, String cf, HCounterColumn<N> c) {
		getPendingMutations().addCounterInsertion(key, Arrays.asList(cf),
				((HCounterColumnImpl<N>) c).toThrift());
		return this;
	}

	@Override
	public <SN, N> Mutator<K> addCounter(K key, String cf,
			HCounterSuperColumn<SN, N> sc) {
		getPendingMutations().addSuperCounterInsertion(key, Arrays.asList(cf),
				((HCounterSuperColumnImpl<SN, N>) sc).toThrift());
		return this;
	}

	@Override
	public <N> Mutator<K> addCounterDeletion(K key, String cf,
			N counterColumnName, Serializer<N> nameSerializer) {
		Deletion d;
		if (counterColumnName != null) {
			SlicePredicate sp = new SlicePredicate();
			sp.addToColumn_names(nameSerializer.toByteBuffer(counterColumnName));
			d = new Deletion().setPredicate(sp);
		} else {
			d = new Deletion();
		}
		getPendingMutations().addDeletion(key, Arrays.asList(cf), d);
		return this;
	}

	@Override
	public <N> Mutator<K> addCounterDeletion(K key, String cf) {
		getPendingMutations().addDeletion(key, Arrays.asList(cf),
				new Deletion());
		return this;
	}

	@Override
	public <SN, N> Mutator<K> addCounterSubDeletion(K key, String cf,
			HCounterSuperColumn<SN, N> sc) {
		Deletion d = new Deletion();
		if (sc.getColumns() != null) {
			SlicePredicate pred = new SlicePredicate();
			for (HCounterColumn<N> col : sc.getColumns()) {
				pred.addToColumn_names(col.getNameSerializer().toByteBuffer(
						col.getName()));
			}
			d.setPredicate(pred);
		}
		d.setSuper_column(sc.getNameByteBuffer());
		getPendingMutations().addDeletion(key, Arrays.asList(cf), d);
		return this;
	}

	@Override
	public <SN, N> Mutator<K> addSubDelete(K key, String cf, SN sColumnName,
			N columnName, Serializer<SN> sNameSerializer,
			Serializer<N> nameSerializer) {
		return addSubDelete(key, cf, sColumnName, columnName, sNameSerializer,
				nameSerializer, keyspace.createClock());
	}

	@Override
	public <SN, N> Mutator<K> addSubDelete(K key, String cf, SN sColumnName,
			N columnName, Serializer<SN> sNameSerializer,
			Serializer<N> nameSerializer, long clock) {
		Deletion d = new Deletion().setTimestamp(clock);
		SlicePredicate predicate = new SlicePredicate();
		predicate.addToColumn_names(nameSerializer.toByteBuffer(columnName));
		d.setPredicate(predicate);
		d.setSuper_column(sNameSerializer.toByteBuffer(sColumnName));
		getPendingMutations().addDeletion(key, Arrays.asList(cf), d);
		return this;
	}

	@Override
	public <SN> Mutator<K> addSuperDelete(K key, String cf, SN sColumnName,
			Serializer<SN> sNameSerializer) {
		Deletion d = new Deletion().setTimestamp(keyspace.createClock());
		d.setSuper_column(sNameSerializer.toByteBuffer(sColumnName));
		getPendingMutations().addDeletion(key, Arrays.asList(cf), d);

		return this;

	}

	/**
	 * Batch executes all mutations scheduled to this Mutator instance by
	 * addInsertion, addDeletion etc. May throw a HectorException which is a
	 * RuntimeException.
	 * 
	 * @return A MutationResult holds the status.
	 */
	@Override
	public MutationResult execute()
	{
		if (pendingMutations == null || pendingMutations.isEmpty()) {
			return new ThrottlingMutationResult(true, 0, null);
		}

		final ThrottlingBatchMutation<K> mutations = pendingMutations.makeCopy();
		pendingMutations = null;

		return new ThrottlingMutationResult(keyspace.doExecuteOperation(new Operation<Void>(OperationType.WRITE)
				{
					@Override
					public Void execute(Cassandra.Client cassandra) throws Exception {
						cassandra.batch_mutate(mutations.getMutationMap(),
								ThriftConverter.consistencyLevel(consistencyLevelPolicy.get(operationType)));
						return null;
					}
				}));
	}
	
	/**
	 * Batch executes all mutations scheduled to this Mutator instance only when
	 * maximum batch size is exceeded. It will also introduce a delay if specified.
	 * May throw a HectorException which is a RuntimeException.
	 * 
	 * @return A MutationResult holds the status. Success with 0ms execution
	 *         time is returned if operation deferred.
	 */
	public MutationResult executeIfFull()
	{
		if (pendingMutations == null || pendingMutations.getMutationsCount() < batchSize)
		{
			return new ThrottlingMutationResult(true, 0, null);
		}
		
		// execute pending mutations if reached max batch size
		logger.debug("Batch reached max ({}), flushing.", batchSize);
		MutationResult result = execute();
		pendingMutations = new ThrottlingBatchMutation<K>(keySerializer, batchSize);

		if (batchInterval != null)
		{
			// sleep remaining time (batch time - execution time)
			long sleepInterval = batchInterval - (long) (result.getExecutionTimeMicro() / 1000);

			if (sleepInterval > 0)
			{
				try {
					logger.debug("sleeping {}ms to throttle.", sleepInterval);
					Thread.sleep(sleepInterval);
				} catch (InterruptedException e) {
					// rethrow as hector exception
					throw new HectorException(e.getMessage());
				}
			}
		}

		return result;
	}

	/**
	 * Discards all pending mutations.
	 */
	@Override
	public Mutator<K> discardPendingMutations() {
		pendingMutations = null;
		return this;
	}

	@Override
	public int getPendingMutationCount() {
		return getPendingMutations().getSize();
	}

	@Override
	public String toString() {
		return "ThrottlingMutator(" + keyspace.toString() + ")";
	}

	private ThrottlingBatchMutation<K> getPendingMutations()
	{
		if (pendingMutations == null) {
			pendingMutations = new ThrottlingBatchMutation<K>(keySerializer, batchSize);
		}

		return pendingMutations;
	}

}
