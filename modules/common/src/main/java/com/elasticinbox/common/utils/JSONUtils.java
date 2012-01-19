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
