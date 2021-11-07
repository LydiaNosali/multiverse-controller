package io.nms.central.microservice.digitaltwin.impl;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.neo4j.driver.AccessMode;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Record;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.driver.async.AsyncSession;
import org.neo4j.driver.async.ResultCursor;
import org.neo4j.driver.summary.ResultSummary;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Helper and wrapper class for JDBC repository services.
 */
public class Neo4jWrapper {

	private static final Logger logger = LoggerFactory.getLogger(Neo4jWrapper.class);
	
	private static final Value EMPTY_PARAMS = Values.parameters();

	private Driver driver;
	private final Vertx vertx;

	public Neo4jWrapper(Vertx vertx, JsonObject config) {
		this.vertx = vertx;
		this.driver = GraphDatabase.driver(config.getString("url"), 
				AuthTokens.basic(config.getString("user"), config.getString("password")));
		try {
			driver.verifyConnectivity();
			logger.info("Connected to neo4j");
		} catch (Exception e) {
			driver.close();
			logger.error(e.getCause());
		}
	}

	/* protected void execute(String db, String sQuery, JsonObject params, Handler<AsyncResult<Void>> resultHandler) {
		try ( Session session = driver.session( sessionConfig );
				Transaction transaction = session.beginTransaction() )
		{
			Query query = new Query(sQuery, Values.parameters(params.getMap()));		
			Result result = transaction.run( query );
			// String greeting = result.single().get( 0 ).asString();
			transaction.commit(); // commit immediately here
			session.close();
		}
		// "CREATE (a:Greeting) SET a.message = $message RETURN a.message + ', from node ' + id(a)"
		// Values.parameters( "message", message )
	} */
	
	protected void execute(String db, String query, JsonObject params, 
			Handler<AsyncResult<ResultSummary>> resultHandler) {
		executeWriteTransaction(db, query, Values.parameters(params.getMap()), ResultCursor::consumeAsync, resultHandler);
	}
	protected void execute(String db, String query,  
			Handler<AsyncResult<ResultSummary>> resultHandler) {
		executeWriteTransaction(db, query, EMPTY_PARAMS, ResultCursor::consumeAsync, resultHandler);
	}
	
	protected void delete(String db, String query, Value parameters, 
			Handler<AsyncResult<List<Record>>> resultHandler) {
		executeWriteTransaction(db, query, parameters, ResultCursor::listAsync, resultHandler);
	}
	protected void delete(String db, String query, 
			Handler<AsyncResult<List<Record>>> resultHandler) {
		executeWriteTransaction(db, query, EMPTY_PARAMS, ResultCursor::listAsync, resultHandler);
	}
	
	protected void findOne(String db, String query, Value parameters, 
			Handler<AsyncResult<Record>> resultHandler) {
        executeReadTransaction(db, query, parameters, ResultCursor::singleAsync, resultHandler);
    }
	protected void findOne(String db, String query, Handler<AsyncResult<Record>> resultHandler) {
        executeReadTransaction(db, query, EMPTY_PARAMS, ResultCursor::singleAsync, resultHandler);
    }
    
	protected void find(String db, String query, Value parameters, 
    		Handler<AsyncResult<List<Record>>> resultHandler) {
        executeReadTransaction(db, query, parameters, ResultCursor::listAsync, resultHandler);
    }
	protected void find(String db, String query,
    		Handler<AsyncResult<List<Record>>> resultHandler) {
        executeReadTransaction(db, query, EMPTY_PARAMS, ResultCursor::listAsync, resultHandler);
    }

	private <T> void executeWriteTransaction(String db, String query, Value parameters, Function<ResultCursor, CompletionStage<T>> resultFunction, Handler<AsyncResult<T>> resultHandler) {
		AsyncSession session = driver.asyncSession(configBuilder(db, AccessMode.WRITE));
		Context context = vertx.getOrCreateContext();
		session.writeTransactionAsync(tx -> tx.runAsync(query, parameters).thenCompose(resultFunction))
				.whenComplete(wrapCallback(context, resultHandler))
				.thenCompose(ignore -> session.closeAsync());
	}

	private <T> void executeReadTransaction(String db, String query, Value parameters, Function<ResultCursor, CompletionStage<T>> resultFunction, Handler<AsyncResult<T>> resultHandler) {
		AsyncSession session = driver.asyncSession(configBuilder(db, AccessMode.READ));
		Context context = vertx.getOrCreateContext();
		session.readTransactionAsync(tx -> tx.runAsync(query, parameters)
				.thenCompose(resultFunction))
				.whenComplete(wrapCallback(context, resultHandler))
				.thenCompose(ignore -> session.closeAsync());
	}
	
	private SessionConfig configBuilder(String db, AccessMode am) {
		return SessionConfig.builder()
				.withDatabase(db)
				.withDefaultAccessMode(am)
				.build();
	}
	
	private static <T> BiConsumer<T, Throwable> wrapCallback(Context context, Handler<AsyncResult<T>> resultHandler) {
        return (result, error) -> {
            context.runOnContext(v -> {
                if (error != null) {
                    resultHandler.handle(Future.failedFuture(Optional.ofNullable(error.getCause()).orElse(error)));
                    logger.error(error.getCause());
                } else {
                	// TODO: transform to JsonObject
                    resultHandler.handle(Future.succeededFuture(result));
                }
            });
        };
    }

}
