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

package com.elasticinbox.core.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * This class stores Labels indexed by Label ID and provides necessary control
 * methods.
 * 
 * @author Rustam Aliyev
 * @see {@link Label}
 */
public final class LabelMap
{
	Map<Integer, Label> labels;

	private final static String JSON_NAME = "name";
	private final static String JSON_SIZE = "size";
	private final static String JSON_ATTRIBUTES = "attributes";
	private final static String JSON_MESSAGES_TOTAL = "total";
	private final static String JSON_MESSAGES_UNREAD = "unread";

	public LabelMap() {
		labels = new HashMap<Integer, Label>();
	}

	public Label get(Integer labelId) {
		return labels.get(labelId);
	}
	
	public Label put(Label label)
	{
		return labels.put(label.getId(), label);
	}
	
	public Set<Integer> getIds()
	{
		return labels.keySet();
	}
	
	/**
	 * Returns map of label (ID, name) pairs.
	 * 
	 * @return
	 */
	public Map<Integer, String> getNameMap()
	{
		Map<Integer, String> nameMap = new HashMap<Integer, String>(labels.size());

		for (Label label : labels.values())
		{
			nameMap.put(label.getId(), label.getName());
		}

		return nameMap;
	}
	
	/**
	 * Check if label with given ID exists.
	 * 
	 * @param labelId
	 * @return
	 */
	public boolean containsId(int labelId)
	{
		return labels.containsKey(labelId);
	}

	/**
	 * Check if label with given name exists. Case insensitive.
	 * 
	 * @param labelName
	 * @return
	 */
	public boolean containsName(String labelName)
	{
		for (Label label : labels.values())
		{
			if (label.getName().equalsIgnoreCase(labelName)) {
				return true;
			}
		}
		
		return false;
	}

	public Collection<Label> values() {
		return labels.values();
	}
	
	public int size() {
		return labels.size();
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
		for (Map.Entry<Integer, Label> entry : this.labels.entrySet())
		{
			Integer labelId = entry.getKey();
			Label label = entry.getValue();

			metadata.put(labelId, new HashMap<String, Object>(4));
			metadata.get(labelId).put(JSON_NAME, label.getName());
			
			if (label.getCounters() != null)
			{
				// never return negative values
				metadata.get(labelId).put(JSON_MESSAGES_TOTAL,
						Math.max(0, label.getCounters().getTotalMessages()));
				metadata.get(labelId).put(JSON_MESSAGES_UNREAD,
						Math.max(0, label.getCounters().getUnreadMessages()));

				// display size only for ALL_MAILS
				if (labelId == ReservedLabels.ALL_MAILS.getId())
				{
					metadata.get(labelId).put(JSON_SIZE,
							Math.max(0, label.getCounters().getTotalBytes()));
				}
			} else {
				metadata.get(labelId).put(JSON_MESSAGES_TOTAL, 0);
				metadata.get(labelId).put(JSON_MESSAGES_UNREAD, 0);

				// display size only for ALL_MAILS
				if (labelId == ReservedLabels.ALL_MAILS.getId()) {
					metadata.get(labelId).put(JSON_SIZE, 0);
				}
			}

			// add attributes if any
			if (label.getAttributes() != null && !label.getAttributes().isEmpty())
			{
				metadata.get(labelId).put(JSON_ATTRIBUTES, label.getAttributes());
			}
		}

		return metadata;
	}

	@Override
	public String toString()
	{
		return toJson().toString();
	}
}
