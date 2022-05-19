package io.nms.central.microservice.telemetry.casper;

import java.util.HashMap;
import java.util.Map;

import io.nms.central.microservice.telemetry.model.Capability;
import io.nms.central.microservice.telemetry.model.Interrupt;
import io.nms.central.microservice.telemetry.model.Message;
import io.nms.central.microservice.telemetry.model.Receipt;
import io.nms.central.microservice.telemetry.model.Result;
import io.nms.central.microservice.telemetry.model.Specification;
import io.vertx.amqp.AmqpClient;
import io.vertx.amqp.AmqpClientOptions;
import io.vertx.amqp.AmqpConnection;
import io.vertx.amqp.AmqpMessage;
import io.vertx.amqp.AmqpReceiver;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public abstract class CasperAPIVerticle extends AbstractVerticle {
	
	protected abstract void onCapability(Capability cap);	
	protected abstract void onResult(Result res);
	// protected abstract void onReceipt(Result res);
	protected Map<String,AmqpReceiver> subscribers = new HashMap<String,AmqpReceiver>();
	
	private static final Logger logger = LoggerFactory.getLogger(CasperAPIVerticle.class);
	
	private AmqpConnection connection = null;
	// private Random rand = new Random();
	
	protected void connect(JsonObject config, Handler<AsyncResult<Void>> resultHandler) {
		if (config == null) {
			resultHandler.handle(Future.failedFuture("AMQP config object is missing"));
			return;
		}
		
		String host = config.getString("host");
		Integer port = config.getInteger("port");	
		if (host == null || port == null) {
			resultHandler.handle(Future.failedFuture("AMQP parameters missing"));
			return;
		}
		
		AmqpClientOptions options = new AmqpClientOptions()
				.setHost(host)
				.setPort(port);
		AmqpClient client = AmqpClient.create(options);
		client.connect(ar -> {
			if (ar.succeeded()) {
				connection = ar.result();
				logger.info("Connected to the messaging platform.");
				resultHandler.handle(Future.succeededFuture());
			} else {
				logger.error("Failed to connect to the messaging platform", ar.cause());
				resultHandler.handle(Future.failedFuture(ar.cause()));
			}
		});
	}
	
	protected void subscribeToCapabilities(String topic, Handler<AsyncResult<Void>> resultHandler) {
		if (subscribers.containsKey(topic)) {
			logger.info("already subscribed to " + topic + " topic");
			resultHandler.handle(Future.succeededFuture());
			return;
		}
		if (connection == null) {
			resultHandler.handle(Future.failedFuture("not connected to the messaging platform"));
			return;
		}
		connection.createReceiver("topic://"+topic, ar -> {
			if (ar.succeeded()) {
				AmqpReceiver receiver = ar.result();
				subscribers.put(topic, receiver);
				receiver
						.exceptionHandler(t -> {})
				        .handler(msg -> {
				        	onCapability(Message.fromJsonString(msg.bodyAsString(), Capability.class));				  
				        });
				resultHandler.handle(Future.succeededFuture());
				logger.info("subscribed to " + topic + " topic");
			} else {
				logger.error("failed to subscribe to " + topic, ar.cause());
				resultHandler.handle(Future.failedFuture(ar.cause()));
			}
		});
	}
	
	protected void publishSpecAwaitReceipt(Specification spec, String topic, Handler<AsyncResult<Receipt>> resultHandler) {
		if (connection == null) {
			resultHandler.handle(Future.failedFuture("not connected to the messaging platform"));
			return;
		}
		connection.createDynamicReceiver(replyReceiver -> {
			if (replyReceiver.succeeded()) {
				Long rctTimeoutTimerId[] = { (long) 0 };
				
				String replyToAddress = replyReceiver.result().address();
				logger.info("subscribed to Receipt topic: " + replyToAddress);
				replyReceiver.result().handler(msg -> {
					vertx.cancelTimer(rctTimeoutTimerId[0]);
					replyReceiver.result().close(ig -> {});

					Receipt rct = Message.fromJsonString(msg.bodyAsString(), Receipt.class);									
					resultHandler.handle(Future.succeededFuture(rct));
					logger.info("receipt received: " + msg.bodyAsString());
				});
				connection.createSender("topic://"+topic, sender -> {
					if (sender.succeeded()) {
						sender.result().send(AmqpMessage.create()
								.replyTo(replyToAddress)
								.id(replyToAddress)
								.withBody(spec.toString()).build());
						logger.info("published Spec to topic: " + topic);
						
						// Rct timeout timer
						rctTimeoutTimerId[0] = vertx.setTimer(1000, new Handler<Long>() {
						    @Override
						    public void handle(Long aLong) {
						    	logger.info("receipt timeout");
						    	replyReceiver.result().close(ig -> {});
						    	resultHandler.handle(Future.failedFuture("receipt timeout"));
						    }
						});
					} else {
						logger.info("failed to publish Spec: " + sender.cause());
						resultHandler.handle(Future.failedFuture(sender.cause()));
					}
				});
			} else {
				logger.error("failed to subscribe to Receipt topic. ", replyReceiver.cause());
				resultHandler.handle(Future.failedFuture(replyReceiver.cause()));
			}
		});
	}
	
	protected void publishItrAwaitReceipt(Interrupt itr, String topic, Handler<AsyncResult<Receipt>> resultHandler) {
		if (connection == null) {
			resultHandler.handle(Future.failedFuture("not connected to the messaging platform"));
			return;
		}
		connection.createDynamicReceiver(replyReceiver -> {
			if (replyReceiver.succeeded()) {
				Long rctTimeoutTimerId[] = { (long) 0 };

				String replyToAddress = replyReceiver.result().address();
				logger.info("subscribed to Receipt topic: " + replyToAddress);
				replyReceiver.result().handler(msg -> {
					vertx.cancelTimer(rctTimeoutTimerId[0]);
					replyReceiver.result().close(ig -> {});

					Receipt rct = Message.fromJsonString(msg.bodyAsString(), Receipt.class);									
					resultHandler.handle(Future.succeededFuture(rct));
					logger.info("receipt received: " + msg.bodyAsString());
				});
				connection.createSender("topic://"+topic, sender -> {
					if (sender.succeeded()) {
						sender.result().send(AmqpMessage.create()
								.replyTo(replyToAddress)
								.id(replyToAddress)
								.withBody(itr.toString()).build());
						logger.info("published Itr to topic: " + topic);
						
						// Rct timeout timer
						rctTimeoutTimerId[0] = vertx.setTimer(1000, new Handler<Long>() {
						    @Override
						    public void handle(Long aLong) {
						    	logger.info("receipt timeout");
						    	replyReceiver.result().close(ig -> {});
						    	resultHandler.handle(Future.failedFuture("receipt timeout"));
						    }
						});
					} else {
						logger.info("failed to publish Itr: " + sender.cause());
						resultHandler.handle(Future.failedFuture(sender.cause()));
					}
				});
			} else {
				logger.error("failed to subscribe to Receipt topic. ", replyReceiver.cause());
				resultHandler.handle(Future.failedFuture(replyReceiver.cause()));
			}
		});
	}
	
	protected void subscribeToResults(String topic, Handler<AsyncResult<Void>> resultHandler) {
		if (subscribers.containsKey(topic)) {
			logger.info("already subscribed to " + topic + " topic");
			resultHandler.handle(Future.succeededFuture());
			return;
		}
		if (connection == null) {
			resultHandler.handle(Future.failedFuture("not connected to the messaging platform"));
			return;
		}
		connection.createReceiver("topic://"+topic, ar -> {
			if (ar.succeeded()) {
				AmqpReceiver receiver = ar.result();
				subscribers.put(topic, receiver);
				receiver
						.exceptionHandler(t -> {})
				        .handler(msg -> {
				        	onResult(Message.fromJsonString(msg.bodyAsString(), Result.class));
				        });
				resultHandler.handle(Future.succeededFuture());
				logger.info("subscribed to " + topic + " topic");
			} else {
				logger.error("failed to subscribe to " + topic, ar.cause());
				resultHandler.handle(Future.failedFuture(ar.cause()));
			}
		});
	}
}
