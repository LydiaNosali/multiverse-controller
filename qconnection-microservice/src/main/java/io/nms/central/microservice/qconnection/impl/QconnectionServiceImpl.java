package io.nms.central.microservice.qconnection.impl;

import io.nms.central.microservice.qconnection.QconnectionService;
import io.nms.central.microservice.qconnection.model.Trail;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.mongo.MongoClient;

/**
 *
 */
public class QconnectionServiceImpl implements QconnectionService {

	private static final Logger logger = LoggerFactory.getLogger(QconnectionServiceImpl.class);
	
	private final MongoClient client;

	public QconnectionServiceImpl(Vertx vertx, JsonObject config) {
		this.client = MongoClient.create(vertx, config);
	}

	@Override
	public QconnectionService initialize(Handler<AsyncResult<Void>> resultHandler) {
		resultHandler.handle(Future.succeededFuture());
		return this;
	}

	@Override
	public QconnectionService createPath(Trail path, String finish, Handler<AsyncResult<Integer>> resultHandler) {
		resultHandler.handle(Future.succeededFuture(0));
		// do necessary verifications
		// create Vtrail in topology (status = PENDING)
		// return Vtrail Id
		// create OXCs on switches
		// update status (should = UP)
		return this;
	}

	@Override
	public QconnectionService deletePath(String pathId, Handler<AsyncResult<Void>> resultHandler) {
		resultHandler.handle(Future.succeededFuture());
		return this;
	}

	@Override
	public QconnectionService doHealthCheck(Handler<AsyncResult<Void>> resultHandler) {
		resultHandler.handle(Future.succeededFuture());
		// call switches...
		return this;
	}
}