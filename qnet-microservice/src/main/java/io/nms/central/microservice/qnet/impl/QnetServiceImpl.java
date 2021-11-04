package io.nms.central.microservice.qnet.impl;

import io.nms.central.microservice.common.service.JdbcRepositoryWrapper;
import io.nms.central.microservice.qnet.QnetService;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 *
 */
public class QnetServiceImpl extends JdbcRepositoryWrapper implements QnetService {

	private static final Logger logger = LoggerFactory.getLogger(QnetServiceImpl.class);

	public QnetServiceImpl(Vertx vertx, JsonObject config) {
		super(vertx, config);
	}

	@Override
	public QnetService initializePersistence(Handler<AsyncResult<Void>> resultHandler) {
		resultHandler.handle(Future.succeededFuture());
		return this;
	}
}