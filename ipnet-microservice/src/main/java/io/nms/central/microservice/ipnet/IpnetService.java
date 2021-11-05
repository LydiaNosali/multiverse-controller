package io.nms.central.microservice.ipnet;

import java.util.List;

import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

/**
 * A service interface managing ipnet.
 * <p>
 * This service is an event bus service (aka. service proxy)
 * </p>
 */
@VertxGen
@ProxyGen
public interface IpnetService {

	/**
	 * The name of the event bus service.
	 */
	String SERVICE_NAME = "ipnet-eb-service";

	/**
	 * The address on which the service is published.
	 */
	String SERVICE_ADDRESS = "service.ipnet";
	

	String FROTNEND_ADDRESS = "mvs.to.frontend";
	
	
	void initializePersistence(Handler<AsyncResult<List<Integer>>> resultHandler);

	
	/* API */
		
	/* Processing */
}
