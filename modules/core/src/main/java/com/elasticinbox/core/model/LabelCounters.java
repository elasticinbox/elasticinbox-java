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

/**
 * This class represents triplet of the lablel counters
 *  
 * @author Rustam Aliyev
 * @see {@link Label}
 */
public final class LabelCounters
{
	private Long totalBytes;
	private Long totalMessages;
	private Long unreadMessages;

	public LabelCounters() {
		totalBytes = 0L;
		totalMessages = 0L;
		unreadMessages = 0L;
	}

	public LabelCounters(LabelCounters l) {
		totalBytes = l.getTotalBytes();
		totalMessages = l.getTotalMessages();
		unreadMessages = l.getUnreadMessages();
	}

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

	public Long getUnreadMessages() {
		return unreadMessages;
	}

	public void setUnreadMessages(Long unreadMessages) {
		this.unreadMessages = unreadMessages;
	}
	
	/**
	 * Performs mathematical addition operation for each of the counters.
	 * 
	 * @param diff
	 *            Triplet to add. Negative values will perform subtraction.
	 */
	public void add(final LabelCounters diff)
	{
		if (diff.totalBytes != null) {
			this.totalBytes += diff.totalBytes;
		}

		if (diff.totalMessages != null) {
			this.totalMessages += diff.totalMessages;
		}

		if (diff.unreadMessages != null) {
			this.unreadMessages += diff.unreadMessages;
		}
	}
	
	/**
	 * Returns inverse value of each counter. Can be used for subtraction.
	 * 
	 * @return
	 */
	public LabelCounters getInverse()
	{
		LabelCounters inverse = new LabelCounters();

		inverse.setTotalBytes(-this.totalBytes);
		inverse.setTotalMessages(-this.totalMessages);
		inverse.setUnreadMessages(-this.unreadMessages);

		return inverse;
	}
	
	@Override
	public String toString() {
		return new StringBuilder("LabelCounters: {totalBytes:")
				.append(totalBytes).append(", totalMessage:")
				.append(totalMessages).append(", unreadMessages:")
				.append(unreadMessages).append("}").toString();
	}
}
