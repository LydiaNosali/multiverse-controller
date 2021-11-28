package io.nms.central.microservice.ipnet;

import io.nms.central.microservice.common.BaseMicroserviceVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class EventHandler extends BaseMicroserviceVerticle {

	private static final Logger logger = LoggerFactory.getLogger(EventHandler.class);
	private final IpnetService configService;

	public EventHandler(IpnetService configService) {
		this.configService = configService;
	}

	@Override
	public void start(Promise<Void> promise) throws Exception {
		super.start(promise);
	}

	private void notifyFrontend() {
		vertx.eventBus().publish(IpnetService.FROTNEND_ADDRESS, new JsonObject());
	}
}
