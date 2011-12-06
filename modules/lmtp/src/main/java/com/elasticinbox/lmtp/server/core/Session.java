package com.elasticinbox.lmtp.server.core;

/**
 * A session describes events which happen during a LMTP session. It keeps track
 * of all of the recipients who will receive the message.
 * 
 * @author Edouard De Oliveira &lt;doe_wanted@yahoo.fr&gt;
 * @author Ian McFarland &lt;ian@neo.com&gt;
 * @author Jon Stevens
 * @author Jeff Schnitzer
 */
public class Session {
	private boolean dataMode = false;
	private boolean hasSeenHelo = false;
	private boolean active = true;
	private boolean hasSender = false;
	private int recipientCount = 0;

	public Session() {
	}

	public boolean isActive() {
		return this.active;
	}

	public void quit() {
		this.active = false;
	}

	public boolean getHasSender() {
		return this.hasSender;
	}

	public void setHasSender(boolean value) {
		this.hasSender = value;
	}

	public boolean getHasSeenHelo() {
		return this.hasSeenHelo;
	}

	public void setHasSeenHelo(boolean hasSeenHelo) {
		this.hasSeenHelo = hasSeenHelo;
	}

	public boolean isDataMode() {
		return this.dataMode;
	}

	public void setDataMode(boolean dataMode) {
		this.dataMode = dataMode;
	}

	public void addRecipient() {
		this.recipientCount++;
	}

	public int getRecipientCount() {
		return this.recipientCount;
	}

	/**
	 * Executes a full reset() of the session which requires a new HELO command
	 * to be sent
	 */
	public void resetAll() {
		reset(false);
	}

	/**
	 * Reset session, but don't require new HELO/EHLO
	 */
	public void reset() {
		reset(true);
	}

	private void reset(boolean hasSeenHelo) {
		this.hasSender = false;
		this.dataMode = false;
		this.active = true;
		this.hasSeenHelo = hasSeenHelo;
		this.recipientCount = 0;
	}
}
