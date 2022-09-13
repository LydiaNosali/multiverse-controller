package io.nms.central.microservice.qconnection;

import io.nms.central.microservice.common.BaseMicroserviceVerticle;
import io.nms.central.microservice.topology.TopologyService;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.WebClient;
import io.vertx.core.Handler;

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
		vertx.eventBus().consumer(TopologyService.EVENT_ADDRESS, event -> {
			// wait 10s then update
			vertx.setTimer(10000, new Handler<Long>() {
				@Override
				public void handle(Long aLong) {
					qconnectionService.getOpticalNetwork(ar -> {
						if (ar.succeeded()) {
							qconnectionService.synchNetworkWithTopology(done -> {
								if (done.succeeded()) {
									logger.info("Optical network updated");
								} else {
									logger.error("Failed to update optical network: " + ar.cause());
								}
							});
						} else {
							logger.error("Failed to update optical network: " + ar.cause());
						}
					});
				}
			});
		});
	}
	
	private void notifyFrontend() {
		vertx.eventBus().publish(QconnectionService.FROTNEND_ADDRESS, new JsonObject());
	}
	 
	private void notifyQconnectionChange() {
		vertx.eventBus().publish(QconnectionService.EVENT_ADDRESS, new JsonObject());
	}
}
