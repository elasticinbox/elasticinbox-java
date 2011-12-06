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

package com.elasticinbox.common.utils;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.UUID;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.codehaus.jackson.map.SerializationConfig.Feature;
import org.codehaus.jackson.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Set of tools for dealing with JSON objects
 * 
 * @author Rustam Aliyev
 */
public class JSONUtils
{
	private static ObjectMapper JSON_MAPPER = new ObjectMapper();

	private final static Logger logger = 
		LoggerFactory.getLogger(JSONUtils.class);

	/**
	 * Unserialize JSON to Object
	 * 
	 * @param <T> Unserialized Object
	 * @param value Serialized JSON
	 * @param ref Object to use as a reference type (can be empty or null)
	 * @return
	 */
	public static <T> T toObject(byte[] value, T ref)
	{
		JSON_MAPPER.configure(Feature.WRITE_NULL_MAP_VALUES, false);

		try {
			return JSON_MAPPER.readValue(value, 0, value.length,
					new TypeReference<T>() { } );
		} catch (Exception e) {
			throw new IllegalStateException("Cannot map JSON to POJO: "
					+ e.getMessage());
		}
	}

	/**
	 * Map JSON to the list of UUIDs
	 * 
	 * <em>Note:</em> This is workaround since {@link toObject} does not
	 * correctly handle <code>List&lt;UUID&gt;</code> type
	 * 
	 * @param value
	 * @return
	 */
	public static List<UUID> toUUIDList(String value)
	{
		try {
			return JSON_MAPPER.readValue(value,
					new TypeReference<List<UUID>>() {});
		} catch (Exception e) {
			throw new IllegalStateException("Cannot map JSON to POJO: "
					+ e.getMessage());
		}
	}

	/**
	 * Map object to JSON
	 * 
	 * @param <T> Object to serialize
	 * @param value Serialized JSON
	 * @return
	 */
    public static <T> byte[] fromObject(T value)
    {
		JSON_MAPPER.configure(Feature.WRITE_NULL_MAP_VALUES, false);
		JSON_MAPPER.configure(Feature.WRITE_DATES_AS_TIMESTAMPS, false);
		JSON_MAPPER.getSerializationConfig().setSerializationInclusion(Inclusion.NON_NULL);
		//JSON_MAPPER.getSerializationConfig().setDateFormat(myDateFormat);

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            JSON_MAPPER.writeValue(out, value);
            return out.toByteArray();
        } catch (Exception e) {
        	logger.error("Cannot map POJO to JSON: ", e);
			throw new IllegalStateException("Cannot map POJO to JSON: "
					+ e.getMessage());
        }
    }

}
