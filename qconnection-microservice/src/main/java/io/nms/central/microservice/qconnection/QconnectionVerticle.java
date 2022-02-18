package io.nms.central.microservice.qconnection;

import static io.nms.central.microservice.qconnection.QconnectionService.SERVICE_ADDRESS;
import static io.nms.central.microservice.qconnection.QconnectionService.SERVICE_NAME;

import io.nms.central.microservice.common.BaseMicroserviceVerticle;
import io.nms.central.microservice.qconnection.api.RestQconnectionAPIVerticle;
import io.nms.central.microservice.qconnection.impl.QconnectionServiceImpl;
import io.nms.central.microservice.topology.StatusHandler;
import io.nms.central.microservice.topology.TopologyService;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.serviceproxy.ServiceBinder;


/**
 * A verticle publishing the qconnection service.
 */
public class QconnectionVerticle extends BaseMicroserviceVerticle {

	@Override
	public void start(Future<Void> future) throws Exception {
		super.start();

		// create the service instance
		QconnectionService qconnectionService = new QconnectionServiceImpl(vertx, config());

		// register the service proxy on event bus
		new ServiceBinder(vertx)
		.setAddress(SERVICE_ADDRESS)
		.register(QconnectionService.class, qconnectionService);

		initQconnection(qconnectionService)
				.compose(r -> deployEventHandler(qconnectionService))
				.compose(r -> deployRestVerticle(qconnectionService))
				.onComplete(future);
	}

	private Future<Void> initQconnection(QconnectionService service) {
		Promise<Void> initPromise = Promise.promise();
		service.initialize(initPromise);
		return initPromise.future();
	}

	private Future<Void> deployRestVerticle(QconnectionService service) {
		Promise<String> promise = Promise.promise();
		vertx.deployVerticle(new RestQconnectionAPIVerticle(service),
				new DeploymentOptions().setConfig(config()), promise);
		return promise.future().map(r -> null);
	}
	
	private Future<Void> deployEventHandler(QconnectionService service) {
		Promise<String> promise = Promise.promise();
		vertx.deployVerticle(new EventHandler(service),
				new DeploymentOptions().setConfig(config()), promise);
		return promise.future().map(r -> null);
	}

}