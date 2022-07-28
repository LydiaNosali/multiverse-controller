package io.nms.central.microservice.qconnection.api;

import io.nms.central.microservice.common.RestAPIVerticle;
import io.nms.central.microservice.common.functional.JsonUtils;
import io.nms.central.microservice.qconnection.QconnectionService;
import io.nms.central.microservice.qconnection.model.CrossConnect;
import io.nms.central.microservice.qconnection.model.Trail;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

/**
 * This verticle exposes a HTTP endpoint to qconnection with REST APIs.
 */
public class RestQconnectionAPIVerticle extends RestAPIVerticle {

	private static final Logger logger = LoggerFactory.getLogger(RestQconnectionAPIVerticle.class);

	public static final String SERVICE_NAME = "qconnection-rest-api";

	private static final String API_VERSION = "/v";

	private static final String API_ONE_PATH = "/path/:pathId";
	private static final String API_ALL_PATHS = "/path/finish=:finish";

	private static final String API_ONE_OXC = "/oxc/:oxcId";
	private static final String API_ALL_OXC = "/oxc";

	private final QconnectionService service;

	public RestQconnectionAPIVerticle(QconnectionService service) {
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

		router.put(API_ALL_PATHS).handler(this::checkAdminRole).handler(this::apiCreatePath);
		router.delete(API_ONE_PATH).handler(this::checkAdminRole).handler(this::apiDeletePath);

		router.put(API_ALL_OXC).handler(this::checkAdminRole).handler(this::apiCreateOXC);
		router.delete(API_ONE_OXC).handler(this::checkAdminRole).handler(this::apiDeleteOXC);

		// get HTTP host and port from configuration, or use default value
		String host = config().getString("qconnection.http.address", "0.0.0.0");
		int port = config().getInteger("qconnection.http.port", 8090);

		// create HTTP server and publish REST service
		createHttpServer(router, host, port).compose(serverCreated -> publishHttpEndpoint(SERVICE_NAME, host, port))
				.onComplete(future);
	}

	// Optical path API
	private void apiCreatePath(RoutingContext context) {
		try {
			final Trail trail = JsonUtils.json2PojoE(context.getBodyAsString(), Trail.class);
			logger.info(trail.toJson().encodePrettily());
			String finish = context.request().getParam("finish");
			service.createPath(trail, finish, createResultHandler(context, "/path"));
		} catch (Exception e) {
			logger.info("Exception: " + e.getMessage());
			badRequest(context, e);
		}
	}

	private void apiDeletePath(RoutingContext context) {
		String pathId = context.request().getParam("pathId");
		service.deletePath(pathId, deleteResultHandler(context));
	}

	private void apiCreateOXC(RoutingContext context) {
		try {
			final CrossConnect crossConnect = JsonUtils.json2PojoE(context.getBodyAsString(), CrossConnect.class);
			logger.info(crossConnect.toJson().encodePrettily());
			service.createCrossConnect(crossConnect, createResultHandler(context, "/oxc"));
		} catch (Exception e) {
			logger.info("Exception: " + e.getMessage());
			badRequest(context, e);
		}
	}

	private void apiDeleteOXC(RoutingContext context) {
		String oxcId = context.request().getParam("oxcId");
		service.deleteCrossConnect(oxcId, deleteResultHandler(context));
	}

	private void apiVersion(RoutingContext context) {
		context.response().end(new JsonObject().put("name", SERVICE_NAME).put("version", "v1").encodePrettily());
	}

	private void notifyFrontend() {
		vertx.eventBus().publish(QconnectionService.FROTNEND_ADDRESS, new JsonObject());
	}

	private void notifyQconnectionChange() {
		vertx.eventBus().publish(QconnectionService.EVENT_ADDRESS, new JsonObject());
	}

}
