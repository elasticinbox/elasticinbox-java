package com.elasticinbox.lmtp.server.core;

import java.lang.reflect.Constructor;

import com.elasticinbox.lmtp.delivery.IDeliveryBackend;
import com.elasticinbox.lmtp.server.api.handler.AbstractDeliveryHandler;
import com.elasticinbox.lmtp.server.api.handler.DefaultDeliveryHandler;
import com.elasticinbox.lmtp.server.api.handler.DeliveryContext;

/**
 * This factory creates a delivery handler for each new LMTP session and 
 * uses the configured {@link AuthenticationHandlerFactory} to create an
 * {@link AuthenticationHandler} used for all sessions until replaced
 * by another factory using the following method  
 * {@link #setAuthenticationHandlerFactory(AuthenticationHandlerFactory)}. 
 *
 * @author De Oliveira Edouard &lt;doe_wanted@yahoo.fr&gt; 
 */
public class DeliveryHandlerFactory
{
	private IDeliveryBackend backend;
	private Class<? extends AbstractDeliveryHandler> deliveryHandlerImplClass =
			DefaultDeliveryHandler.class;

	/**
	 * Initializes this factory with the listeners.
	 */
	public DeliveryHandlerFactory(IDeliveryBackend backend) {
		this.backend = backend;
	}

	public AbstractDeliveryHandler create(DeliveryContext ctx) {
		return create(ctx, deliveryHandlerImplClass);
	}

	/**
	 * Sets the {@link IDeliveryBackend} implementation to use when
	 * creating the {@link DeliveryHandlerInterface}.
	 * @param backend
	 */
	public void setLmtpBackend(IDeliveryBackend backend) {
		this.backend = backend;
	}

	/**
	 * Sets the {@link AbstractDeliveryHandler} implementation to use when
	 * creating the {@link DeliveryHandlerInterface}.
	 */
	public void setDeliveryHandlerImplClass(Class<? extends AbstractDeliveryHandler> c) {
		this.deliveryHandlerImplClass = c;
	}

	private AbstractDeliveryHandler create(DeliveryContext ctx, 
			Class<? extends AbstractDeliveryHandler> c)
	{
		try {
			Constructor<? extends AbstractDeliveryHandler> cstr =
					c.getConstructor(DeliveryContext.class);
			AbstractDeliveryHandler handler = cstr.newInstance(ctx);
			handler.setDeliveryBackend(backend);
			return handler;
		} catch (Exception e) {
			throw new IllegalArgumentException(
					"Failed instantiating DeliveryHandler - " + c.getName(), e);
		}
	}

}