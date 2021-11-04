package io.nms.central.microservice.ndnet;

import java.util.List;

import io.nms.central.microservice.topology.model.Vctp;
import io.nms.central.microservice.ndnet.model.ConfigObj;
import io.nms.central.microservice.ndnet.model.Route;
import io.nms.central.microservice.topology.model.Prefix;
import io.nms.central.microservice.topology.model.Vconnection;
import io.nms.central.microservice.topology.model.Vnode;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;

/**
 * A service interface managing ndnet.
 * <p>
 * This service is an event bus service (aka. service proxy)
 * </p>
 */
@VertxGen
@ProxyGen
public interface NdnetService {

	/**
	 * The name of the event bus service.
	 */
	String SERVICE_NAME = "ndnet-eb-service";

	/**
	 * The address on which the service is published.
	 */
	String SERVICE_ADDRESS = "service.ndnet";
	

	String FROTNEND_ADDRESS = "mvs.to.frontend";
	
	
	void initializePersistence(Handler<AsyncResult<List<Integer>>> resultHandler);

	
	/* API */
	void getCandidateConfig(int nodeId, Handler<AsyncResult<ConfigObj>> resultHandler);
	void removeCandidateConfig(int nodeId, Handler<AsyncResult<Void>> resultHandler);
	
	void upsertRunningConfig(int nodeId, ConfigObj config, Handler<AsyncResult<Void>> resultHandler);
	void updateRunningConfig(int nodeId, JsonArray patch, Handler<AsyncResult<Void>> resultHandler);
	void getRunningConfig(int nodeId, Handler<AsyncResult<ConfigObj>> resultHandler);
	void removeRunningConfig(int nodeId, Handler<AsyncResult<Void>> resultHandler);
	
		
	/* Processing */
	void computeRoutes(List<Vnode> nodes, List<Vconnection> links, List<Prefix> pas, Handler<AsyncResult<List<Route>>> resultHandler);
	void computeConfigurations(List<Vnode> nodes, List<Vconnection> edges, List<Vctp> faces, List<Prefix> pas, Handler<AsyncResult<List<ConfigObj>>> resultHandler);
	void upsertCandidateConfigs(List<ConfigObj> configs, Handler<AsyncResult<Void>> resultHandler);
	
}
