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

/**
 * This class represents triplet of the lablel counters
 *  
 * @author Rustam Aliyev
 * @see {@link Label}
 */
public final class LabelCounters
{
	private Long totalBytes = 0L;
	private Long totalMessages = 0L;
	private Long newMessages = 0L;

	public Long getTotalBytes() {
		return totalBytes;
	}

	public void setTotalBytes(Long totalBytes) {
		this.totalBytes = totalBytes;
	}

	public Long getTotalMessages() {
		return totalMessages;
	}

	public void setTotalMessages(Long totalMessages) {
		this.totalMessages = totalMessages;
	}

	public Long getNewMessages() {
		return newMessages;
	}

	public void setNewMessages(Long newMessages) {
		this.newMessages = newMessages;
	}
	
	/**
	 * Performs mathematical addition operation for each of the counters.
	 * 
	 * @param diff
	 *            Triplet to add. Negative values will perform subtraction.
	 */
	public void add(final LabelCounters diff) {
		this.totalBytes += diff.totalBytes;
		this.totalMessages += diff.totalMessages;
		this.newMessages += diff.newMessages;
	}
	
	@Override
	public String toString() {
		return new StringBuilder("LabelCounters: {totalBytes:")
				.append(totalBytes).append(", totalMessage:")
				.append(totalMessages).append(", newMessages:")
				.append(newMessages).append("}").toString();
	}
}
