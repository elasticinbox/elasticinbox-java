/**
 * Copyright (c) 2011 Optimax Software Ltd
 * 
 * This file is part of ElasticInbox.
 * 
 * ElasticInbox is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 2 of the License, or (at your option) any later
 * version.
 * 
 * ElasticInbox is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * ElasticInbox. If not, see <http://www.gnu.org/licenses/>.
 */

package com.elasticinbox.core.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.codehaus.jackson.annotate.JsonValue;

/**
 * This class stores multiple labels with counters (total size, total messages,
 * new messages).
 * 
 * @author Rustam Aliyev
 * @see {@link Label}
 * @see {@link LabelCounters}
 */
public class Labels
{
	/** Maximum label count per mailbox (including reserved labels) */
	public final static int MAX_LABEL_ID = 9999;

	private Map<Integer, String> labels;
	private Map<Integer, LabelCounters> counters;

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
		labels.put(label.getLabelId(), label.getLabelName());
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
	 * Add counters to a single label
	 * 
	 * @param labelId
	 * @param counters
	 */
	public void addCounters(int labelId, LabelCounters counters) {
		this.counters.put(labelId, counters);
	}

	/**
	 * Add counters to multiple labels
	 * 
	 * @param counters
	 */
	public void addCounters(Map<Integer, LabelCounters> counters) {
		this.counters.putAll(counters);
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
	 * Checks whether label with given name exists
	 *  
	 * @param labelName
	 * @return
	 */
	public boolean containsName(String labelName) {
		return labels.containsValue(labelName);
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
			metadata.get(labelId).put("name", label.getValue());

			if (counters.containsKey(labelId)) {
				metadata.get(labelId).put("size", counters.get(labelId).getTotalBytes());
				metadata.get(labelId).put("total",counters.get(labelId).getTotalMessages());
				metadata.get(labelId).put("new",  counters.get(labelId).getNewMessages());
			} else {
				metadata.get(labelId).put("size", 0);
				metadata.get(labelId).put("total", 0);
				metadata.get(labelId).put("new", 0);
			}
		}

		return metadata;
	}
}
