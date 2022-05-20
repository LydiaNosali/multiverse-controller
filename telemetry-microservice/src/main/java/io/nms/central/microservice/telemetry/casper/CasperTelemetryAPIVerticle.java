package io.nms.central.microservice.telemetry.casper;

import io.nms.central.microservice.telemetry.TelemetryService;
import io.nms.central.microservice.telemetry.model.Capability;
import io.nms.central.microservice.telemetry.model.Interrupt;
import io.nms.central.microservice.telemetry.model.Receipt;
import io.nms.central.microservice.telemetry.model.Result;
import io.nms.central.microservice.telemetry.model.Specification;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * This verticle implements methods to manage telemetry operations with CaSpeR API.
 *
 * @author Amar Abane
 */
public class CasperTelemetryAPIVerticle extends CasperAPIVerticle {
	
	private static final Logger logger = LoggerFactory.getLogger(CasperTelemetryAPIVerticle.class);
	
	private static final String TOPIC_CAPABILITIES = "/capabilities";
	private static final String TOPIC_SPECIFICATIONS = "/specifications";
	private static final String TOPIC_RESULTS = "/results";
	
	private final TelemetryService service;

	public CasperTelemetryAPIVerticle(TelemetryService service) {
		this.service = service;
	}
	
	@Override
	public void start(Promise<Void> promise) throws Exception {
		super.start();
		connect(config().getJsonObject("amqp"), done -> {
			if (done.succeeded()) {
				// TODO: re-send existent Specs if any...
				subscribeToCapabilities(TOPIC_CAPABILITIES, promise);
			} else {
				promise.fail(done.cause());
			}
		});	
	}
	
	public void processSpecification(String id, Specification spec, Handler<AsyncResult<Receipt>> resultHandler) {
		String specTopic = spec.getEndpoint() + TOPIC_SPECIFICATIONS;
		publishSpecAwaitReceipt(spec, specTopic, ar -> {
			if (ar.succeeded()) {
				Receipt specRct = ar.result();
				if (!specRct.getErrors().isEmpty()) {
					resultHandler.handle(Future.succeededFuture(specRct));
					logger.info("receipt contains error: " + specRct.getErrors().get(0));
					return;
				}
				String resTopic = specRct.getEndpoint() + TOPIC_RESULTS;
				subscribeToResults(resTopic, sub -> {
					if (sub.succeeded()) {
						logger.info("subscribed to results topic: " + resTopic);
						service.saveSpecification(spec, res -> {
							if (res.succeeded()) {
								logger.info("specification saved");
								service.saveReceipt(specRct, done -> {
									if (done.succeeded()) {
										logger.info("receipt saved");
										resultHandler.handle(Future.succeededFuture(ar.result()));
									} else {
										logger.info("failed to save receipt");
										resultHandler.handle(Future.failedFuture(done.cause()));
									}
								});
							} else {
								logger.info("failed to save specification");
								resultHandler.handle(Future.failedFuture(res.cause()));
							}
						});
					} else {
						logger.info("failed to subscribe to results topic: " + resTopic);
						resultHandler.handle(Future.failedFuture(sub.cause()));
					}
				});
			} else {
				resultHandler.handle(Future.failedFuture(ar.cause()));
			}
		});
	}
	
	public void processInterrupt(String id, Interrupt itr, Handler<AsyncResult<Receipt>> resultHandler) {
		// TODO: check receipt and specification existence...
		String topic = itr.getEndpoint() + TOPIC_SPECIFICATIONS;
		publishItrAwaitReceipt(itr, topic, ar -> {
			if (ar.succeeded()) {
				Receipt itrRct = ar.result();
				if (!itrRct.getErrors().isEmpty()) {
					resultHandler.handle(Future.failedFuture(itrRct.getErrors().get(0)));
					return;
				}
				service.removeSpecification(itrRct.getSchema(), res -> {
					if (res.succeeded()) {
						service.removeReceipt(itrRct.getSchema(), done -> {
							if (done.succeeded()) {
								resultHandler.handle(Future.succeededFuture(ar.result()));
							} else {
								resultHandler.handle(Future.failedFuture(done.cause()));
							}
						});
					} else {
						resultHandler.handle(Future.failedFuture(res.cause()));
					}
				});
			} else {
<<<<<<< HEAD
				// delete Spec and Rct anyway, consider correct
				resultHandler.handle(Future.succeededFuture());
				service.removeSpecification(itr.getSchema(), ar0 -> {
					service.removeReceipt(itr.getSchema(), ig -> {});
=======
				resultHandler.handle(Future.failedFuture(ar.cause()));
				
				// delete Spec and Rct anyway
				service.removeSpecification(itr.getSchema(), res -> {
					if (res.succeeded()) {
						service.removeReceipt(itr.getSchema(), done -> {
							if (done.succeeded()) {
								resultHandler.handle(Future.succeededFuture(ar.result()));
							} else {
								resultHandler.handle(Future.failedFuture(done.cause()));
							}
						});
					} else {
						resultHandler.handle(Future.failedFuture(res.cause()));
					}
>>>>>>> e832bdb (fix: include params in results summary)
				});
			}
		});
	}
	
	@Override
	protected void onResult(Result result) {
		service.saveResult(result, done -> {
			if (done.succeeded()) {
				publishUpdateToUI();
				logger.info("result saved"); 
			} else {
				logger.warn("result not saved");
			}
		});
	}

	@Override
	protected void onCapability(Capability capability) {
		logger.info(capability.toJson().encodePrettily());
		service.saveCapability(capability, done -> {
			if (done.succeeded()) {
				publishUpdateToUI();
				logger.info("capability saved"); 
			} else {
				logger.warn("capability not saved: " + done.cause());
			}
		});
	}
	
	private void publishUpdateToUI() {
		vertx.eventBus().publish(TelemetryService.UI_ADDRESS, new JsonObject()
				.put("service", TelemetryService.SERVICE_ADDRESS));
	}

}