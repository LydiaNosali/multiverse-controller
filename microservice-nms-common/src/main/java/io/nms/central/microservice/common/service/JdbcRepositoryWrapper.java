package io.nms.central.microservice.common.service;

import java.util.List;
import java.util.UUID;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;

/**
 * Helper and wrapper class for JDBC repository services.
 */
public class JdbcRepositoryWrapper {

	private static final Logger logger = LoggerFactory.getLogger(JdbcRepositoryWrapper.class);
	protected final JDBCClient client;
	private SQLConnection globalConn = null;
	protected enum Entity {
		NONE,
	    NODE,
		LTP,
		CTP,
	    LINK,
		LC,
		CONNECTION,
	    PA
	}
	private Entity currEntity = Entity.NONE;
	private UUID currUUID = null;
	private int counter = 0;

	public JdbcRepositoryWrapper(Vertx vertx, JsonObject config) {
		this.client = JDBCClient.create(vertx, config);
	}
	
	// remove optional in findOne -> add NOT_FOUND error
	// update SQLs
	// uniform promise return

	protected void execute(String sql, JsonArray params, Handler<AsyncResult<Void>> resultHandler) {
		client.getConnection(connHandler(resultHandler, connection -> {
			connection.updateWithParams(sql, params, r -> {
				if (r.succeeded()) {
					resultHandler.handle(Future.succeededFuture());
				} else {
					resultHandler.handle(Future.failedFuture(convertError(r.cause())));
					// resultHandler.handle(Future.failedFuture(r.cause()));
					// logger.error("sql error: " + r.cause().getMessage());
				}
				connection.close();
			});
		}));
	}
	
	protected void insert(String sql, JsonArray params, Handler<AsyncResult<Integer>> resultHandler) {
		client.getConnection(connHandler(resultHandler, connection -> {
			connection.updateWithParams(sql, params, r -> {
				if (r.succeeded()) {
					UpdateResult updateResult = r.result();
					if (updateResult.getKeys().size() > 0) {
						resultHandler.handle(Future.succeededFuture(updateResult.getKeys().getInteger(0)));
					} else {
						resultHandler.handle(Future.failedFuture("Not inserted"));
					}
				} else {
					resultHandler.handle(Future.failedFuture(convertError(r.cause())));
					// resultHandler.handle(Future.failedFuture(r.cause()));
				}
				connection.close();
			});
		}));
	}
	
	// find exactly one
	protected Future<JsonObject> findOne(String sql, JsonArray params) {
		return getConnection()
				.compose(connection -> {
					Promise<JsonObject> promise = Promise.promise();
					connection.queryWithParams(sql, params, r -> {
						if (r.succeeded()) {
							List<JsonObject> resList = r.result().getRows();
							if (resList == null || (resList.size() != 1)) {
								promise.fail("NOT_FOUND");
							} else {
								promise.complete(resList.get(0));
							}
						} else {
							// promise.fail(r.cause());
							promise.fail(convertError(r.cause()));
						}
						connection.close();
					});
					return promise.future();
				});
	}
	
	protected Future<JsonObject> findOne(String sql) {
		return getConnection()
				.compose(connection -> {
					Promise<JsonObject> promise = Promise.promise();
					connection.query(sql, r -> {
						if (r.succeeded()) {
							List<JsonObject> resList = r.result().getRows();
							if (resList == null || (resList.size() != 1)) {
								promise.fail("NOT_FOUND");
							} else {
								promise.complete(resList.get(0));
							}
						} else {
							// promise.fail(r.cause());
							promise.fail(convertError(r.cause()));
						}
						connection.close();
					});
					return promise.future();
				});
	}
	
	protected Future<List<JsonObject>> find(String sql, JsonArray params) {
		return getConnection().compose(connection -> {
			Promise<List<JsonObject>> promise = Promise.promise();
			connection.queryWithParams(sql, params, r -> {
				if (r.succeeded()) {
					promise.complete(r.result().getRows());
				} else {
					// promise.fail(r.cause());
					promise.fail(convertError(r.cause()));
				}
				connection.close();
			});
			return promise.future();
		});
	}

	protected Future<List<JsonObject>> find(String sql) {
		return getConnection().compose(connection -> {
			Promise<List<JsonObject>> promise = Promise.promise();
			connection.query(sql, r -> {
				if (r.succeeded()) {
					promise.complete(r.result().getRows());
				} else {
					// promise.fail(r.cause());
					promise.fail(convertError(r.cause()));
				}
				connection.close();
			});
			return promise.future();
		});
	}

	protected void delete(String sql, JsonArray params, Handler<AsyncResult<Void>> resultHandler) {
		client.getConnection(connHandler(resultHandler, connection -> {
			connection.updateWithParams(sql, params, r -> {
				if (r.succeeded()) {
					resultHandler.handle(Future.succeededFuture());
				} else {
					resultHandler.handle(Future.failedFuture(convertError(r.cause())));
					// resultHandler.handle(Future.failedFuture(r.cause()));
				}
				connection.close();
			});
		}));
	}

	protected void delete(String sql, Handler<AsyncResult<Void>> resultHandler) {
		client.getConnection(connHandler(resultHandler, connection -> {
			connection.update(sql, r -> {
				if (r.succeeded()) {
					resultHandler.handle(Future.succeededFuture());
				} else {
					resultHandler.handle(Future.failedFuture(convertError(r.cause())));
					// resultHandler.handle(Future.failedFuture(r.cause()));
				}
				connection.close();
			});
		}));
	}

	protected <R> Handler<AsyncResult<SQLConnection>> connHandler(Handler<AsyncResult<R>> h1, Handler<SQLConnection> h2) {
		return conn -> {
			if (conn.succeeded()) {
				final SQLConnection connection = conn.result();
				h2.handle(connection);
			} else {
				h1.handle(Future.failedFuture(convertError(conn.cause())));
			}
		};
	}
	
	protected Future<SQLConnection> getConnection() {
		Promise<SQLConnection> promise = Promise.promise();
		client.getConnection(promise);
		return promise.future();
	}
	
	/* Transactions */
	protected Future<Void> beginTransaction(Entity entity, UUID uuid, String sql) {
		Promise<Void> promise = Promise.promise();
		if ((currUUID != null) && uuid.equals(currUUID)) {
			counter++;
			promise.complete();
		} else {
			client.getConnection(ar -> {
				if (ar.succeeded()) {
					ar.result().setAutoCommit(false, res -> {
						if (res.succeeded()) {
							ar.result().execute(sql, p -> {
								if (p.succeeded()) {
									globalConn = ar.result();
									currUUID = uuid;
									currEntity = entity;
									counter = 0;
									promise.complete();
								} else {
									// promise.fail(p.cause());
									promise.fail(convertError(p.cause()));
								}
							});
						} else {
							// promise.fail(res.cause());
							promise.fail(convertError(res.cause()));
						}
					});
				} else {
					// promise.fail(ar.cause());
					promise.fail(convertError(ar.cause()));
				}
			});
		}
		return promise.future();
	}
	
	protected Future<Integer> transactionInsert(String sql, JsonArray params) {
		Promise<Integer> promise = Promise.promise();
		globalConn.updateWithParams(sql, params, ar -> {
			if (ar.succeeded()) {
				UpdateResult updateResult = ar.result();
				// if (updateResult.getKeys().size() > 0) {
					promise.complete(updateResult.getKeys().getInteger(0));
				// } else {
				//	rollbackAndUnlock();
				//	promise.fail("Not inserted");
				//}					 
			} else {
				rollback();
				// promise.fail(ar.cause());
				promise.fail(convertError(ar.cause()));
			}
		});
		return promise.future();
	}
	
	protected Future<Void> transactionExecute(String sql, JsonArray params) {
		Promise<Void> promise = Promise.promise();
		globalConn.updateWithParams(sql, params, ar -> {
			if (ar.succeeded()) {
				promise.complete();					 
			} else {
				rollback();
				// promise.fail(ar.cause());
				promise.fail(convertError(ar.cause()));
			}
		});
		return promise.future();
	}
	
	protected Future<Void> transactionExecute(String sql) {
		Promise<Void> promise = Promise.promise();
		globalConn.update(sql, ar -> {
			if (ar.succeeded()) {
				promise.complete();					 
			} else {
				rollback();
				// promise.fail(ar.cause());
				promise.fail(convertError(ar.cause()));
			}
		});
		return promise.future();
	}
	
	protected Future<List<JsonObject>> transactionFind(String sql) { 
		Promise<List<JsonObject>> promise = Promise.promise();
		globalConn.query(sql, r -> {
				if (r.succeeded()) {
					promise.complete(r.result().getRows());
				} else {
					rollback();
					// promise.fail(r.cause());
					promise.fail(convertError(r.cause()));
				}
			});
		return promise.future();
	}
	
	protected Future<List<JsonObject>> transactionFind(String sql, JsonArray params) { 
		Promise<List<JsonObject>> promise = Promise.promise();
		globalConn.queryWithParams(sql, params, r -> {
				if (r.succeeded()) {
					promise.complete(r.result().getRows());
				} else {
					rollback();
					// promise.fail(r.cause());
					promise.fail(convertError(r.cause()));
				}
			});
		return promise.future();
	}
	
	protected Future<JsonObject> transactionFindOne(String sql, JsonArray params) {
		Promise<JsonObject> promise = Promise.promise();
		globalConn.queryWithParams(sql, params, r -> {
			if (r.succeeded()) {
				List<JsonObject> resList = r.result().getRows();
				if (resList == null || (resList.size() != 1)) {
					promise.fail("NOT_FOUND");
				} else {
					promise.complete(resList.get(0));
				}
			} else {
				rollback();
				// promise.fail(r.cause());
				promise.fail(convertError(r.cause()));
			}
		});
		return promise.future();
	}
	
	protected Future<Void> commitTransaction(Entity entity, UUID uuid) {
		Promise<Void> promise = Promise.promise();
		if (currUUID.equals(uuid) && currEntity.equals(entity) && (counter == 0)) {
			globalConn.commit(ar -> {
				if (ar.succeeded()) {
					globalConn.execute("UNLOCK TABLES", done -> {
						currEntity = Entity.NONE;
						currUUID = null;
						globalConn.close();
						globalConn = null;
						if (done.succeeded()) {
							promise.complete();
						} else {
							// promise.fail(done.cause());
							promise.fail(convertError(done.cause()));
						}
					});
				} else {
					globalConn.close();
					globalConn = null;
					// promise.fail(ar.cause());
					promise.fail(convertError(ar.cause()));
				}
			});
		} else {
			counter--;
			promise.complete();
		}
		return promise.future();
	}
	
	protected void rollback() {
		if (!currEntity.equals(Entity.NONE)) {
			globalConn.rollback(ar -> {
				globalConn.execute("UNLOCK TABLES", done -> {
					currEntity = Entity.NONE;
					currUUID = null;
				});
			});
		}
	}

	private String convertError(Throwable error) {
		String msg = error.getMessage();
		logger.error("SQL Exception: ", msg);
		String errorMessage = "INTERNAL";
		if (msg.contains("Duplicate")) {
			errorMessage = "CONFLICT";
		}
		return errorMessage;
	}
}
