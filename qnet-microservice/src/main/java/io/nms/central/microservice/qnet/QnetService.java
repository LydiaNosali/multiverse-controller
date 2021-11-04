package io.nms.central.microservice.qnet;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

/**
 * A service interface managing qnet.
 * <p>
 * This service is an event bus service (aka. service proxy)
 * </p>
 */
@VertxGen
@ProxyGen
public interface QnetService {

	/**
	 * The name of the event bus service.
	 */
	String SERVICE_NAME = "qnet-eb-service";

	/**
	 * The address on which the service is published.
	 */
	String SERVICE_ADDRESS = "service.qnet";
	
	
	String FROTNEND_ADDRESS = "mvs.to.frontend";

	String EVENT_ADDRESS = "qnet.event";
	
	
	@Fluent	
	QnetService initializePersistence(Handler<AsyncResult<Void>> resultHandler);

}