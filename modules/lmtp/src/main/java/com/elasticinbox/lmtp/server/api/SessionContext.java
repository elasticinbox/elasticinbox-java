package com.elasticinbox.lmtp.server.api;

import java.net.SocketAddress;

import com.elasticinbox.lmtp.server.api.handler.AbstractDeliveryHandler;
import com.elasticinbox.lmtp.server.api.handler.DeliveryContext;

/**
 * Interface which provides the session context to the {@link MessageListener}.
 * This is a subset of the original {@link DeliveryContext} to decomplexify
 * the interface.
 * 
 * It also provides the session attributes handling methods to provide the
 * implementors with some extended customization possibilities without having
 * to implement the {@link AbstractDeliveryHandler}.
 * 
 * @author De Oliveira Edouard &lt;doe_wanted@yahoo.fr&gt;
 * @author Rustam Aliyev
 */
public interface SessionContext
{
	/**
	 * @return the IP address of the remote server.
	 */
	public SocketAddress getRemoteAddress();
	
	/**
	 * Adds an attribute to the current session object. The lifetime of an 
	 * attribute is the same as the one of the LMTP session. 
	 */
	public void addAttribute(String key, Object attr);
	
	/**
	 * Sets the value of attribute stored under the given key.
	 */
	public void setAttribute(String key, Object attr);
	
	/**
	 * Removes an attribute stored under the given key.
	 */
	public void removeAttribute(String key);
	
	/**
	 * Get the attribute value stored under the given key.
	 */
	public Object getAttribute(String key);
}