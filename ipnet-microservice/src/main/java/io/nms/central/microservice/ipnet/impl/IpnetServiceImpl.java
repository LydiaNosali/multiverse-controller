package io.nms.central.microservice.ipnet.impl;

import java.util.List;

import io.nms.central.microservice.ipnet.IpnetService;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.mongo.MongoClient;

/**
 * This verticle implements the ipnet service
 */
public class IpnetServiceImpl implements IpnetService {

	private static final Logger logger = LoggerFactory.getLogger(IpnetServiceImpl.class);
	
	private final MongoClient client;
	private final Vertx vertx;

	public IpnetServiceImpl(Vertx vertx, JsonObject config) {
		this.vertx = vertx;
		this.client = MongoClient.create(vertx, config);
	}
	
	@Override
	public void initializePersistence(Handler<AsyncResult<List<Integer>>> resultHandler) {
		resultHandler.handle(Future.succeededFuture());
	}
}

