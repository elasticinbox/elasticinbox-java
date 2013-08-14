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

package com.elasticinbox.core.cassandra.persistence;

import static me.prettyprint.hector.api.factory.HFactory.createColumn;
import static me.prettyprint.hector.api.factory.HFactory.createSliceQuery;
import static com.elasticinbox.core.cassandra.CassandraDAOFactory.CF_ACCOUNTS;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.elasticinbox.common.utils.Assert;
import com.elasticinbox.core.IllegalLabelException;
import com.elasticinbox.core.cassandra.CassandraDAOFactory;
import com.elasticinbox.core.cassandra.utils.BatchConstants;
import com.elasticinbox.core.model.Label;
import com.elasticinbox.core.model.LabelMap;
import com.elasticinbox.core.model.ReservedLabels;

import me.prettyprint.cassandra.serializers.BytesArraySerializer;
import me.prettyprint.cassandra.serializers.SerializerTypeInferer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.beans.ColumnSlice;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.SliceQuery;

public final class AccountPersistence
{
	private final static String CN_LABEL_NAME_PREFIX = "label";
	private final static String CN_LABEL_ATTRIBUTE_PREFIX = "lattr";
	private final static String CN_SEPARATOR = ":";

	private final static BytesArraySerializer byteSe = BytesArraySerializer.get();
	private final static StringSerializer strSe = StringSerializer.get();

	/**
	 * Get all account attributes
	 * 
	 * @param mailbox
	 * @return
	 * @throws IOException 
	 */
	public static Map<String, Object> getAll(final String mailbox)
	{
		// Create a query
		SliceQuery<String, String, byte[]> q = 
				createSliceQuery(CassandraDAOFactory.getKeyspace(), strSe, strSe, byteSe);

		// set key, cf, range
		q.setColumnFamily(CF_ACCOUNTS).setKey(mailbox);
		q.setRange(null, null, false, BatchConstants.BATCH_READS); // TODO: make sure we get all columns
		// execute
		QueryResult<ColumnSlice<String, byte[]>> r = q.execute();

		// read attributes from the result
		Map<String, Object> attributes = new HashMap<String, Object>();
		for (HColumn<String, byte[]> c : r.get().getColumns()) { 
			if( (c != null) && (c.getValue() != null)) {
				attributes.put(c.getName(), strSe.fromBytes(c.getValue()));
			}
		}

		return attributes;
	}

	/**
	 * Add or update account attributes (columns)
	 *  
	 * @param account
	 * @param attributes
	 */
	public static <V> void set(Mutator<String> mutator, final String mailbox, final Map<String, V> attributes)
	{
		if (!attributes.isEmpty())
		{
			for (Map.Entry<String, V> a : attributes.entrySet())
			{
				mutator.addInsertion(mailbox, CF_ACCOUNTS,
						createColumn(a.getKey(), a.getValue(), strSe,
								SerializerTypeInferer.getSerializer(a.getValue())));
			}
		}
	}

	/**
	 * Delete account
	 * 
	 * @param mutator
	 * @param mailbox
	 */
	public static void delete(Mutator<String> mutator, final String mailbox)
	{
		mutator.addDeletion(mailbox, CF_ACCOUNTS, null, strSe);
	}

	/**
	 * Get all labels
	 * 
	 * @param mailbox
	 * @return
	 */
	public static LabelMap getLabels(final String mailbox)
	{
		LabelMap labels = new LabelMap();

		// get list of user specific labels from Cassandra
		Map<String, Object> attributes = getAll(mailbox);

		// add user specific labels
		for (Map.Entry<String, Object> a : attributes.entrySet())
		{
			if (a.getKey().startsWith(CN_LABEL_NAME_PREFIX))
			{
				// set label name
				Integer labelId = Integer.parseInt(a.getKey().split(CN_SEPARATOR)[1]);
				String labelName = (String) a.getValue();
				
				if (labels.containsId(labelId)) {
					labels.get(labelId).setName(labelName);
				} else {
					Label label = new Label(labelId, labelName); 
					labels.put(label);
				}
			}
			else if (a.getKey().startsWith(CN_LABEL_ATTRIBUTE_PREFIX))
			{
				// set label custom attribute
				String[] attrKeys = a.getKey().split(CN_SEPARATOR);
				Integer labelId = Integer.parseInt(attrKeys[1]);
				String attrName = attrKeys[2];
				String attrValue = (String) a.getValue();
				
				if (labels.containsId(labelId)) {
					labels.get(labelId).addAttribute(attrName, attrValue);
				} else {
					Label label = new Label(labelId);
					label.addAttribute(attrName, attrValue);
					labels.put(label);
				}
			}
		}

		// add default reserved labels
		for (Label l : ReservedLabels.getAll())
		{
			Label label = new Label(l.getId(), l.getName());
			labels.put(label);
		}

		return labels;
	}

	/**
	 * Inserts new or updates existing label.
	 *
	 * @param mutator
	 * @param mailbox
	 * @param label
	 */
	public static void putLabel(Mutator<String> mutator, final String mailbox, Label label)
	{
		String labelKey = getLabelNameKey(label.getId());
		Map<String, String> attributes = new HashMap<String, String>(1);
		
		// upsert label name
		if (label.getName() != null)
		{
			attributes.put(labelKey, label.getName());
		}

		// upsert custom label attributes (delete if value is null or empty)
		if (label.getAttributes() != null)
		{
			for (Entry<String, String> labelAttr : label.getAttributes().entrySet())
			{
				// custom label attribute should not contain separator char
				if (labelAttr.getKey().contains(CN_SEPARATOR)) {
					throw new IllegalLabelException("Invalid character detected in the custom label attribute name.");
				}

				String labelAttrKey = getLabelAttributeKey(label.getId(), labelAttr.getKey());
				
				if (labelAttr.getValue() != null && !labelAttr.getValue().isEmpty()) {
					// upsert attribute
					attributes.put(labelAttrKey, labelAttr.getValue());
				} else {
					// delete if value is empty 
					mutator.addDeletion(mailbox, CF_ACCOUNTS, labelAttrKey, strSe);	
				}
			}
		}

		AccountPersistence.set(mutator, mailbox, attributes);
	}

	/**
	 * Delete label from account
	 * 
	 * @param mutator
	 * @param mailbox
	 * @param labelId
	 */
	public static void deleteLabel(Mutator<String> mutator, final String mailbox, int labelId)
	{
		String labelNameKey = getLabelNameKey(labelId);
		String labelAttrPrefix = getLabelAttributeKey(labelId, ""); // label attributes prefix
		
		// delete label name
		mutator.addDeletion(mailbox, CF_ACCOUNTS, labelNameKey, strSe);

		// delete all attributes
		Map<String, Object> attributes = getAll(mailbox);
		
		for (String labelAttrKey : attributes.keySet())
		{
			if (labelAttrKey.startsWith(labelAttrPrefix))
			{
				mutator.addDeletion(mailbox, CF_ACCOUNTS, labelAttrKey, strSe);
			}
		}
	}

	/**
	 * Generates key for custom label attribute.
	 * <p>
	 * Example <code>"lattr:123:MyAttribute"</code>
	 * 
	 * @param labelId
	 * @param attributeName Custom attribute name
	 * @return
	 */
	static String getLabelAttributeKey(int labelId, final String attributeName)
	{
		Assert.notNull(attributeName, "Attribute name cannot be null");
		
		return CN_LABEL_ATTRIBUTE_PREFIX + 
					CN_SEPARATOR + labelId + 
					CN_SEPARATOR + attributeName;
	}
	
	/**
	 * Generates key for label name.
	 * <p>
	 * Example <code>"label:123"</code>
	 * 
	 * @param labelId
	 * @return
	 */
	static String getLabelNameKey(int labelId)
	{
		return CN_LABEL_NAME_PREFIX + CN_SEPARATOR + labelId;
	}

}
