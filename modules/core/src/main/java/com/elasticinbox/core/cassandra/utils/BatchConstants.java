package com.elasticinbox.core.cassandra.utils;

/**
 * Constants for batch operations.
 * <p>
 * Reads should be greater than writes, otherwise write time delays will never occur.
 * 
 * @author Rustam Aliyev
 */
public class BatchConstants
{
	/** Maximum number of columns to read at once */
	public final static int BATCH_READS = 250;

	/**
	 * Maximum number of columns to write within time interval. Used for rate
	 * limiting where RATE = WRITES / WRITES_INTERVAL
	 */
	public final static int BATCH_WRITES = 100;

	/**
	 * Time interval within which maximum number of batch writes should occur.
	 * In MILLISECONDS. Used for rate limiting where RATE = WRITES /
	 * WRITES_INTERVAL.
	 */
	public final static long BATCH_WRITE_INTERVAL = 500L;
}
