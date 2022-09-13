package io.nms.central.microservice.qconnection;

import io.nms.central.microservice.qconnection.model.CrossConnect;
import io.nms.central.microservice.qconnection.model.Trail;
import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

/**
 * A service interface managing qconnection.
 * <p>
 * This service is an event bus service (aka. service proxy)
 * </p>
 */
@VertxGen
@ProxyGen
public interface QconnectionService {

	/**
	 * The name of the event bus service.
	 */
	String SERVICE_NAME = "qconnection-eb-service";

	/**
	 * The address on which the service is published.
	 */
	String SERVICE_ADDRESS = "service.qconnection";

	String FROTNEND_ADDRESS = "mvs.to.frontend";

	String EVENT_ADDRESS = "qconnection.event";

	@Fluent
	QconnectionService initialize(Handler<AsyncResult<Void>> resultHandler);

	@Fluent
	QconnectionService getOpticalNetwork(Handler<AsyncResult<Void>> resultHandler);
	
	@Fluent
	QconnectionService synchNetworkWithTopology(Handler<AsyncResult<Void>> resultHandler);

	@Fluent
	QconnectionService createPath(Trail path, String finish, Handler<AsyncResult<Integer>> resultHandler);

	@Fluent
	QconnectionService deletePath(String trailId, Handler<AsyncResult<Void>> resultHandler);

	@Fluent
	QconnectionService doHealthCheck(Handler<AsyncResult<Void>> resultHandler);
	
	@Fluent
	QconnectionService createCrossConnect(CrossConnect crossConnect, Handler<AsyncResult<Integer>> resultHandler);

	@Fluent
	QconnectionService deleteCrossConnect(String oxcId, Handler<AsyncResult<Void>> resultHandler);

}