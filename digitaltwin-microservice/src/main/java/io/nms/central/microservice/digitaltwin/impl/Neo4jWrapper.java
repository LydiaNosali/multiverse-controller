package io.nms.central.microservice.digitaltwin.impl;

import java.util.List;
import java.util.Optional;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Helper and wrapper class for JDBC repository services.
 */
public class Neo4jWrapper implements AutoCloseable {

	private static final Logger logger = LoggerFactory.getLogger(Neo4jWrapper.class);

	Driver driver;

	public Neo4jWrapper(Vertx vertx, JsonObject config) {
		driver = GraphDatabase.driver(config.getString("url"), 
				AuthTokens.basic(config.getString("user"), config.getString("password")));
	}

	protected void executeNoResult(String db, JsonArray params, String sql, Handler<AsyncResult<Void>> resultHandler) {
		/* SessionConfig sessionConfig = SessionConfig.builder()
				.withDatabase("neo4j")
			    .withDefaultAccessMode(AccessMode.WRITE)
			    .build();

		try ( Session session = driver.session( sessionConfig );
				Transaction transaction = session.beginTransaction() )
		{
			Query query = new Query( "CREATE (a:Greeting) SET a.message = $message RETURN a.message + ', from node ' + id(a)", 
					Values.parameters( "message", message ) );
			Result result = transaction.run( query );
			String greeting = result.single().get( 0 ).asString();
			transaction.commit(); // commit immediately here
		} */
		try (Session session = driver.session(SessionConfig.forDatabase(db))) {
			session.run(sql);
			session.close();
			driver.close();
			resultHandler.handle(Future.succeededFuture());
		} catch (Exception e) {
			driver.close();
			resultHandler.handle(Future.failedFuture(e));
		}
	}
	
	protected void insertAndGetId(JsonArray params, String sql, Handler<AsyncResult<Integer>> resultHandler) {
	}
	
	// update or insert ONE row and return Id
	protected void upsert(JsonArray params, String sql, Handler<AsyncResult<Integer>> resultHandler) {
	}
	
	/* execute and return the number of updated elements */
	protected void update(JsonArray params, String sql, Handler<AsyncResult<Integer>> resultHandler) {
	}

	protected <K> Promise<Optional<JsonObject>> retrieveOne(K param, String sql) {
		return null;
	}
	
	protected Future<Optional<JsonObject>> retrieveOne(JsonArray params, String sql) {
		return null;
	}
	
	protected <K> Future<Optional<List<JsonObject>>> retrieveOneNested(K param, String sql) {
		return null;
	}

	
	protected Future<List<JsonObject>> retrieveMany(JsonArray param, String sql) {
		return null;
	}

	protected Future<List<JsonObject>> retrieveAll(String sql) {
		return null;
	}

	protected <K> void removeOne(K id, String sql, Handler<AsyncResult<Void>> resultHandler) {
	}

	protected void removeAll(String sql, Handler<AsyncResult<Void>> resultHandler) {
	}

	@Override
	public void close() throws Exception {
		// TODO Auto-generated method stub
		
	}

}
