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

package com.elasticinbox.lmtp.filter;

import java.util.LinkedList;
import java.util.List;

/**
 * This processor is modified implementation of Chain of Responsibility pattern.
 * Filters are applied in the given order. Each filter (handler) processes input
 * and passes output to the next one. Final result returned by processor.
 * 
 * @author rustam
 * 
 * @param <T>
 */
public class FilterProcessor<T>
{
	private List<Filter<T>> filters;

	public FilterProcessor() {
		filters = new LinkedList<Filter<T>>();
	}

	/**
	 * Add filter to the bottom of the chain
	 * 
	 * @param filter
	 */
	public void add(Filter<T> filter) {
		filters.add(filter);
	}

	/**
	 * Perform filtering process
	 * 
	 * @param input
	 * @return
	 */
	public T doFilter(final T input)
	{
		T output = input;
		for (Filter<T> filter : filters) {
			output = filter.filter(output);
		}

		return output;
	}
}
