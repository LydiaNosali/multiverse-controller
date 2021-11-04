package io.nms.central.microservice.digitaltwin.api;

import io.nms.central.microservice.common.RestAPIVerticle;
import io.nms.central.microservice.digitaltwin.DigitalTwinService;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

/**
 * This verticle exposes a HTTP endpoint to process digital twin operations with
 * REST APIs.
 */
public class RestDigitalTwinAPIVerticle extends RestAPIVerticle {

	private static final Logger logger = LoggerFactory.getLogger(RestDigitalTwinAPIVerticle.class);

	public static final String SERVICE_NAME = "digitaltwin-rest-api";

	private static final String API_VERSION = "/v";

	private final DigitalTwinService service;

	public RestDigitalTwinAPIVerticle(DigitalTwinService service) {
		this.service = service;
	}

	@Override
	public void start(Future<Void> future) throws Exception {
		super.start();
		final Router router = Router.router(vertx);
		// body handler
		router.route().handler(BodyHandler.create());

		// API route handler
		router.get(API_VERSION).handler(this::apiVersion);

		// get HTTP host and port from configuration, or use default value
		String host = config().getString("digitaltwin.http.address", "0.0.0.0");
		int port = config().getInteger("digitaltwin.http.port", 8084);

		// create HTTP server and publish REST service
		createHttpServer(router, host, port)
				.compose(serverCreated -> publishHttpEndpoint(SERVICE_NAME, host, port))
				.onComplete(future);
	}

	private void apiVersion(RoutingContext context) {
		context.response().end(new JsonObject().put("name", SERVICE_NAME).put("version", "v1").encodePrettily());
	}

	private void notifyDigitalTwinChange() {
		// TODO: add type of change
		vertx.eventBus().publish(DigitalTwinService.EVENT_ADDRESS, new JsonObject());
	}

	private void notifyFrontend() {
		vertx.eventBus().publish(DigitalTwinService.FROTNEND_ADDRESS, new JsonObject());
	}
}
