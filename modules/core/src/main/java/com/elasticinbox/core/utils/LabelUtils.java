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

package com.elasticinbox.core.utils;

import java.util.Random;

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
		int labelId = ReservedLabels.MAX_RESERVED_LABEL_ID
				+ random.nextInt(Labels.MAX_LABEL_ID - ReservedLabels.MAX_RESERVED_LABEL_ID);
		return labelId;
	}
}