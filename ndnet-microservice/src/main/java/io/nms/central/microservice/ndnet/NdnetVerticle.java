package io.nms.central.microservice.ndnet;

import static io.nms.central.microservice.ndnet.NdnetService.SERVICE_ADDRESS;
import static io.nms.central.microservice.ndnet.NdnetService.SERVICE_NAME;

import java.util.List;

import io.nms.central.microservice.common.BaseMicroserviceVerticle;
import io.nms.central.microservice.ndnet.api.RestNdnetAPIVerticle;
import io.nms.central.microservice.ndnet.impl.NdnetServiceImpl;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.serviceproxy.ServiceBinder;


/**
 * A verticle publishing the ndnet service.
 *
 */
public class NdnetVerticle extends BaseMicroserviceVerticle {

	@Override
	public void start(Future<Void> future) throws Exception {
		super.start();
		
		// create the service instance
		NdnetService ndnetService = new NdnetServiceImpl(vertx, config());
		
		// register the service proxy on event bus
		new ServiceBinder(vertx)
			.setAddress(SERVICE_ADDRESS)
			.register(NdnetService.class, ndnetService);

		initConfigDatabase(ndnetService)
	//		.compose(databaseOkay -> publishEventBusService(SERVICE_NAME, SERVICE_ADDRESS, ConfigurationService.class))
			.compose(databaseOkay -> deployHandler(ndnetService))
			.compose(handlerPrepared -> deployRestVerticle(ndnetService))
			.onComplete(future);
	}

	private Future<List<Integer>> initConfigDatabase(NdnetService service) {
		Promise<List<Integer>> initPromise = Promise.promise();
		service.initializePersistence(initPromise);
		return initPromise.future();
	}

	private Future<Void> deployHandler(NdnetService service) {
		Promise<String> promise = Promise.promise();
		vertx.deployVerticle(new EventHandler(service),
				new DeploymentOptions().setConfig(config()), promise);
		return promise.future().map(r -> null);
	}

	private Future<Void> deployRestVerticle(NdnetService service) {
		Promise<String> promise = Promise.promise();
		vertx.deployVerticle(new RestNdnetAPIVerticle(service),
				new DeploymentOptions().setConfig(config()), promise);
		return promise.future().map(r -> null);
	}

}