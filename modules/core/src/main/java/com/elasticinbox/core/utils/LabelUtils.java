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

package com.elasticinbox.core.utils;

import java.util.Random;

import com.elasticinbox.core.ExistingLabelException;
import com.elasticinbox.core.IllegalLabelException;
import com.elasticinbox.core.model.Label;
import com.elasticinbox.core.model.LabelConstants;
import com.elasticinbox.core.model.Labels;
import com.elasticinbox.core.model.ReservedLabels;

public final class LabelUtils
{
	private final static Random random = new Random();

	/**
	 * Generate random label ID
	 * 
	 * @return
	 */
	public static Integer getNewLabelId()
	{
		// New label ID whould be greater than reserved label IDs and within
		// allowed range (less than MAX_LABEL_ID).
		int labelId = LabelConstants.MAX_RESERVED_LABEL_ID
				+ random.nextInt(LabelConstants.MAX_LABEL_ID - LabelConstants.MAX_RESERVED_LABEL_ID);
		return labelId;
	}

	/**
	 * Validate label name and check within existing labels
	 * 
	 * @param label
	 */
	public static void validateLabelName(final String label, final Labels existingLabels)
	{
		// check total length of label
		if (label.length() > LabelConstants.MAX_LABEL_NAME_LENGTH)
			throw new IllegalLabelException("Label name exceeds maximum allowed length");

		// check if label already exists
		if(existingLabels.containsName(label))
			throw new ExistingLabelException("Label with this name already exists");

		// check if starts with reserved label
		for (Label l : ReservedLabels.getAll()) {
			if (label.startsWith(l.getName() + LabelConstants.NESTED_LABEL_SEPARATOR.toString()))
				throw new IllegalLabelException("Netsted labels are not allowed under reserved labels");
		}

		if (label.contains(LabelConstants.NESTED_LABEL_SEPARATOR.toString() + LabelConstants.NESTED_LABEL_SEPARATOR.toString()))
			throw new IllegalLabelException("Illegal use of nested label separator");

		// check special symbols? for now we allow any symbol
	}
}
