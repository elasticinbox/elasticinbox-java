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

package com.elasticinbox.core.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.codehaus.jackson.annotate.JsonValue;

/**
 * This class stores multiple labels with counters (total size, total messages,
 * unread messages).
 * 
 * @author Rustam Aliyev
 * @see {@link LabelCounters}
 */
public class Labels
{
	private Map<Integer, String> labels;
	private Map<Integer, LabelCounters> counters;

	private final static String JSON_NAME = "name";
	private final static String JSON_SIZE = "size";
	private final static String JSON_MESSAGES_TOTAL = "total";
	private final static String JSON_MESSAGES_UNREAD = "unread";

	public Labels() {
		labels = new HashMap<Integer, String>(1);
		counters = new HashMap<Integer, LabelCounters>(1);
	}

	/**
	 * Add label
	 * 
	 * @param labelId
	 * @param labelName
	 */
	public void add(int labelId, String labelName) {
		labels.put(labelId, labelName);
	}

	/**
	 * Add label
	 * 
	 * @param label
	 */
	public void add(Label label) {
		labels.put(label.getId(), label.getName());
	}

	/**
	 * Add multiple labels
	 * 
	 * @param labels
	 */
	public void add(Map<Integer, String> labels) {
		this.labels.putAll(labels);
	}
	
	/**
	 * Get label name by ID
	 * 
	 * @param labelId
	 * @return
	 */
	public String getName(int labelId) {
		String name = labels.get(labelId);
		return name;
	}

	/**
	 * Set counters of a single label
	 * 
	 * @param labelId
	 * @param counters
	 */
	public void setCounters(final int labelId, LabelCounters counters) {
		this.counters.put(labelId, counters);
	}

	/**
	 * Set counters of multiple labels
	 * 
	 * @param counters
	 */
	public void setCounters(Map<Integer, LabelCounters> counters) {
		this.counters.putAll(counters);
	}

	/**
	 * Increments counters of the given label
	 * 
	 * @param labelId
	 * @param counters
	 */
	public void incrementCounters(final int labelId, LabelCounters counters)
	{
		if (!this.counters.containsKey(labelId)) {
			this.counters.put(labelId, counters);
		} else {
			this.counters.get(labelId).add(counters);
		}
	}

	/**
	 * Get all label IDs (union of both map keys)
	 * 
	 * @return
	 */
	public Set<Integer> getIds()
	{
		Set<Integer> ids = new HashSet<Integer>();
		ids.addAll(labels.keySet());
		ids.addAll(counters.keySet());
		return ids;
	}

	/**
	 * Get counters for specified label
	 * 
	 * @param labelId
	 * @return
	 */
	public LabelCounters getLabelCounters(Integer labelId) {
		return counters.get(labelId);
	}

	/**
	 * Checks whether label with given ID exists
	 *  
	 * @param labelId
	 * @return
	 */
	public boolean containsId(Integer labelId) {
		return this.getIds().contains(labelId);
	}

	/**
	 * Checks whether label with given name exists. Case insensitive.
	 * 
	 * @param labelName
	 * @return
	 */
	public boolean containsName(String labelName)
	{
		for (String v : labels.values()) {
			if (v.equalsIgnoreCase(labelName))
				return true;
		}
		return false;
	}

	/**
	 * This method constructs Map for JSON serialization
	 * 
	 * <p>It will return extended map if there are any records in
	 * <code>counters</code> object. Otherwise simple <code>labels</code> object
	 * will be returned.</p>
	 * 
	 * @return
	 */
	@JsonValue
	public Map<Integer, Map<String, Object>> toJson()
	{
		Map<Integer, Map<String, Object>> metadata = 
							new HashMap<Integer, Map<String, Object>>();

		// build result object
		for (Map.Entry<Integer, String> label : labels.entrySet())
		{
			Integer labelId = label.getKey();
			metadata.put(labelId, new HashMap<String, Object>(4));
			metadata.get(labelId).put(JSON_NAME, label.getValue());

			if (counters.containsKey(labelId))
			{
				// never return negative values
				metadata.get(labelId).put(JSON_MESSAGES_TOTAL,
						Math.max(0, counters.get(labelId).getTotalMessages()));
				metadata.get(labelId).put(JSON_MESSAGES_UNREAD,
						Math.max(0, counters.get(labelId).getUnreadMessages()));

				// display size only for ALL_MAILS
				if(labelId == ReservedLabels.ALL_MAILS.getId()) {
					metadata.get(labelId).put(JSON_SIZE,
							Math.max(0, counters.get(labelId).getTotalBytes()));
				}
			} else {
				metadata.get(labelId).put(JSON_MESSAGES_TOTAL, 0);
				metadata.get(labelId).put(JSON_MESSAGES_UNREAD, 0);
				// display size only for ALL_MAILS
				if(labelId == ReservedLabels.ALL_MAILS.getId()) {
					metadata.get(labelId).put(JSON_SIZE, 0);
				}
			}
		}

		return metadata;
	}
}
