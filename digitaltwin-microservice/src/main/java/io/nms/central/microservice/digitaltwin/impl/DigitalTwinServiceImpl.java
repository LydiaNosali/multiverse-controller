package io.nms.central.microservice.digitaltwin.impl;

import java.time.Duration;
import java.time.Instant;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.summary.ResultSummary;

import io.nms.central.microservice.digitaltwin.DigitalTwinService;
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
public class DigitalTwinServiceImpl implements DigitalTwinService {

	private static final Logger logger = LoggerFactory.getLogger(DigitalTwinServiceImpl.class);

	private static final String MAIN_DB = "neo4j";
	Driver driver;
	
	public DigitalTwinServiceImpl(Vertx vertx, JsonObject config) {
		driver = GraphDatabase.driver(config.getString("url"), 
				AuthTokens.basic(config.getString("user"), config.getString("password")));
	}

	@Override
	public DigitalTwinService initializePersistence(Handler<AsyncResult<Void>> resultHandler) {
		// Driver driver = GraphDatabase.driver("bolt://localhost:7687", AuthTokens.basic("neo4j", "12345"));
		try (Session session = driver.session(SessionConfig.forDatabase(MAIN_DB))) {
			session.run(ApiCypher.CLEAR_DB);
			
			session.close();
			driver.close();
			resultHandler.handle(Future.succeededFuture());
		} catch (Exception e) {
			driver.close();
			resultHandler.handle(Future.failedFuture(e));
		}
		return this;
	}
}