package com.elasticinbox.core.cassandra.utils;

import me.prettyprint.cassandra.model.ExecutionResult;
import me.prettyprint.cassandra.service.CassandraHost;
import me.prettyprint.hector.api.mutation.MutationResult;

public class ThrottlingMutationResult extends ExecutionResult<Void> implements MutationResult
{
	  ThrottlingMutationResult(boolean success, long execTime, CassandraHost cassandraHost) {
	    super(null, execTime, cassandraHost);
	  }

	  ThrottlingMutationResult(ExecutionResult<Void> res) {
	    super(null, res.getExecutionTimeNano(), res.getHostUsed());
	  }

	  @Override
	  public String toString() {
	    return formatMessage("ThrottlingMutationResult", "n/a");
	  }
}
