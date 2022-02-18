package io.nms.central.microservice.qconnection;

import io.nms.central.microservice.common.BaseMicroserviceVerticle;
import io.nms.central.microservice.topology.TopologyService;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.WebClient;

public class EventHandler extends BaseMicroserviceVerticle {

	private static final Logger logger = LoggerFactory.getLogger(EventHandler.class);
	
	private final QconnectionService qconnectionService;
	// private WebClient webClient;

	public EventHandler(QconnectionService qconnectionService) {
		this.qconnectionService = qconnectionService;
		// this.webClient = WebClient.create(vertx);
	}

	@Override
	public void start(Promise<Void> promise) throws Exception {
		super.start(promise);
		vertx.eventBus().consumer(TopologyService.EVENT_ADDRESS, ar -> {
			// TODO: process topology events
		});
	}

	private void handleTopologyEvent() {
	}
	
	private void notifyFrontend() {
		vertx.eventBus().publish(QconnectionService.FROTNEND_ADDRESS, new JsonObject());
	}
	 
	private void notifyQconnectionChange() {
		vertx.eventBus().publish(QconnectionService.EVENT_ADDRESS, new JsonObject());
	}
}
