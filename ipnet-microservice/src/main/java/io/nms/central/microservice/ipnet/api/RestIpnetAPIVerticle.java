package io.nms.central.microservice.ipnet.api;

import io.nms.central.microservice.common.RestAPIVerticle;
import io.nms.central.microservice.ipnet.IpnetService;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;


/**
 * This verticle exposes a HTTP endpoint to ipnet with REST APIs.
 *
 * @author Amar Abane
 */
public class RestIpnetAPIVerticle extends RestAPIVerticle {

	private static final Logger logger = LoggerFactory.getLogger(RestIpnetAPIVerticle.class);

	public static final String SERVICE_NAME = "ipnet-rest-api";
	
	private static final String API_VERSION = "/v";

	private final IpnetService service;

	public RestIpnetAPIVerticle(IpnetService service) {
		this.service = service;
	}

	@Override
	public void start(Future<Void> future) throws Exception {
		super.start();
		final Router router = Router.router(vertx);
		router.route().handler(BodyHandler.create());
		router.get(API_VERSION).handler(this::apiVersion);
		
		// get HTTP host and port from configuration, or use default value
		String host = config().getString("ipnet.http.address", "0.0.0.0");
		int port = config().getInteger("ipnet.http.port", 8087);

		// create HTTP server and publish REST service
		createHttpServer(router, host, port)
				.compose(serverCreated -> publishHttpEndpoint(SERVICE_NAME, host, port))
				.onComplete(future);
	}

	private void apiVersion(RoutingContext context) { 
		context.response()
		.end(new JsonObject()
				.put("name", SERVICE_NAME)
				.put("version", "v1").encodePrettily());
	}
}
