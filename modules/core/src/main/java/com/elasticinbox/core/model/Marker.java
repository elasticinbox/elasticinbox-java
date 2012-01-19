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
 * This is the representation of message markers. Each message can be marked
 * with one or more markers.
 * 
 * @author Rustam Aliyev
 */
public enum Marker
{
	/** Mark message as Seen */
	SEEN(1),

	/** Mark message as Replied */
	REPLIED(2),

	/** Mark message as Forwarded */
	FORWARDED(3);

	private int value;

	Marker(int value) {
		this.value = value;
	}

	public int toInt() {
		return value;
	}

	public static Marker fromInt(int value)
	{
		switch (value) {
		case 1:
			return SEEN;
		case 2:
			return REPLIED;
		default:
			return FORWARDED;
		}
	}

	public static Marker fromString(String value)
	{
		if (value == null) {
			throw new IllegalArgumentException();
		}

		if (value.equals("seen")) {
			return Marker.SEEN;
		} else if (value.equals("replied")) {
			return Marker.REPLIED;
		} else if (value.equals("forwarded")) {
			return Marker.FORWARDED;
		} else {
			throw new IllegalArgumentException();
		}
	}

	public String toString()
	{
		switch (this) {
		case SEEN:
			return "seen";
		case REPLIED:
			return "replied";
		case FORWARDED:
			return "forwarded";
		}

		return null;
	}
}
