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

package com.elasticinbox.core;

import java.util.HashSet;
import java.util.Set;

import com.elasticinbox.core.model.Marker;

/**
 * Modifications which can be applied to the existing message.
 * 
 * @author Rustam Aliyev
 */
public class MessageModification
{
	private Set<Integer> addLabels;
	private Set<Integer> removeLabels;
	private Set<Marker> addMarkers;
	private Set<Marker> removeMarkers;

	public static class Builder
	{
		private Set<Integer> addLabels = new HashSet<Integer>(1);
		private Set<Integer> removeLabels = new HashSet<Integer>(1);
		private Set<Marker> addMarkers = new HashSet<Marker>(1);
		private Set<Marker> removeMarkers = new HashSet<Marker>(1);

		/**
		 * Add labels to the message
		 * 
		 * @param ids
		 * @return
		 */
		public Builder addLabels(Set<Integer> ids)
		{
			addLabels.addAll(ids);
			return this;
		}
	
		/**
		 * Remove labels form the message
		 * @param ids
		 * @return
		 */
		public Builder removeLabels(Set<Integer> ids)
		{
			removeLabels.addAll(ids);
			return this;
		}

		/**
		 * Add markers to the message
		 * @param markers
		 * @return
		 */
		public Builder addMarkers(Set<Marker> markers)
		{
			addMarkers.addAll(markers);
			return this;
		}

		/**
		 * Add marker to the message
		 * @param ids
		 * @return
		 */
		public Builder addMarker(Marker marker)
		{
			addMarkers.add(marker);
			return this;
		}

		/**
		 * Remove markers from the message
		 * 
		 * @param markers
		 * @return
		 */
		public Builder removeMarkers(Set<Marker> markers)
		{
			removeMarkers.addAll(markers);
			return this;
		}

		/**
		 * Remove marker from the message
		 * @param ids
		 * @return
		 */
		public Builder removeMarker(Marker marker)
		{
			removeMarkers.add(marker);
			return this;
		}

		public MessageModification build() {
			return new MessageModification(this);
		}
	}

	private MessageModification(Builder b)
	{
		this.addLabels = b.addLabels;
		this.removeLabels = b.removeLabels;
		this.addMarkers = b.addMarkers;
		this.removeMarkers = b.removeMarkers;
	}

	public Set<Integer> getLabelsToAdd() {
		return addLabels;
	}

	public Set<Integer> getLabelsToRemove() {
		return removeLabels;
	}

	public Set<Marker> getMarkersToAdd() {
		return addMarkers;
	}

	public Set<Marker> getMarkersToRemove() {
		return removeMarkers;
	}
}