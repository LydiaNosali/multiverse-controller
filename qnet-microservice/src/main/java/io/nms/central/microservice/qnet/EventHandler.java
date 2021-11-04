package io.nms.central.microservice.qnet;

import io.nms.central.microservice.common.BaseMicroserviceVerticle;
import io.nms.central.microservice.topology.TopologyService;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.WebClient;

public class EventHandler extends BaseMicroserviceVerticle {

	private static final Logger logger = LoggerFactory.getLogger(EventHandler.class);
	
	private final QnetService qnetService;
	private WebClient webClient;
	// private Map<Integer,Long> statusTimers;

	public EventHandler(QnetService qnetService) {
		this.qnetService = qnetService;
		webClient = WebClient.create(vertx);
		// this.statusTimers = new HashMap<Integer,Long>();
	}

	@Override
	public void start(Promise<Void> promise) throws Exception {
		super.start(promise);
		vertx.eventBus().consumer(TopologyService.EVENT_ADDRESS, ar -> {
		});
	}

	private void handleTopologyEvent() {
	}
	
	private void notifyFrontend() {
		vertx.eventBus().publish(QnetService.FROTNEND_ADDRESS, new JsonObject());
	}
	 
	private void notifyQnetChange() {
		vertx.eventBus().publish(QnetService.EVENT_ADDRESS, new JsonObject());
	}
	
	/* String requestPath = "/api/data/cross-connects/" + String.valueOf(res.result().getString("ingressPort"));
	webClient
			.delete(8008, res.result().getString("switchIpAddr").split("/")[0], requestPath) 
			.send(ar -> {
				if (ar.succeeded()) {
					// TODO: check DB delete result
					removeOne(crossConnectId, ApiSql.DELETE_CROSS_CONNECT, resultHandler);
				} else {
					resultHandler.handle(Future.failedFuture("Failed to delete Cross-connect on the switch"));
				}
	}); */
	
	/* webClient
			.post(8008, res.result().getString("switchIpAddr").split("/")[0], "/api/data/cross-connects")
			.sendJsonObject(new JsonObject()
					.put("ingress", res.result().getString("ingressPort"))
					.put("egress", res.result().getString("egressPort")), ar -> {
			if (ar.succeeded()) {
				// TODO: check DB insert result
				insertAndGetId(params, ApiSql.INSERT_CROSS_CONNECT, resultHandler);
			} else {
				resultHandler.handle(Future.failedFuture("Failed to create Cross-connect"));
			}
	}); */
}
