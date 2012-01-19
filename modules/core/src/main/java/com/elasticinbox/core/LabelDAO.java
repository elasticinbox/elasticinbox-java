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

package com.elasticinbox.core;

import java.io.IOException;
import java.util.Map;

import com.elasticinbox.core.model.Labels;
import com.elasticinbox.core.model.Mailbox;

/**
 * Interface for Label operations
 *
 * @author Rustam Aliyev
 */
public interface LabelDAO
{
	/**
	 * Get all labels in the mailbox.
	 * 
	 * @param mailbox
	 * @return Label IDs and names
	 * @throws IOException
	 */
	public Map<Integer, String> getAll(Mailbox mailbox) throws IOException;

	/**
	 * Get all labels in the mailbox with stats (total size, total messages, new
	 * messages).
	 * 
	 * @param mailbox
	 * @return
	 * @throws IOException
	 */
	public Labels getAllWithMetadata(Mailbox mailbox) throws IOException;

	/**
	 * Add new label to mailbox
	 * 
	 * @param mailbox
	 * @param label New label name
	 * @return New label ID
	 * @throws IOException
	 * @throws IllegalLabelException
	 */
	public int add(Mailbox mailbox, String label) throws IOException, IllegalLabelException;

	/**
	 * Rename existing label
	 * 
	 * @param mailbox
	 * @param labelId
	 *            Existing label ID
	 * @param label
	 *            New name for existing label
	 * @throws IOException
	 * @throws IllegalLabelException
	 */
	public void rename(Mailbox mailbox, Integer labelId, String label) throws IOException, IllegalLabelException;

	/**
	 * Delete existing label
	 * 
	 * @param mailbox
	 * @param labelId
	 *            Existing label ID
	 * @throws IOException
	 * @throws IllegalLabelException
	 */
	public void delete(Mailbox mailbox, Integer labelId) throws IOException, IllegalLabelException;

}
