package io.nms.central.microservice.ipnet;

import static io.nms.central.microservice.ipnet.IpnetService.SERVICE_ADDRESS;

import java.util.List;
import io.nms.central.microservice.common.BaseMicroserviceVerticle;
import io.nms.central.microservice.ipnet.api.RestIpnetAPIVerticle;
import io.nms.central.microservice.ipnet.impl.IpnetServiceImpl;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.serviceproxy.ServiceBinder;


/**
 * A verticle publishing the ipnet service.
 *
 */
public class IpnetVerticle extends BaseMicroserviceVerticle {

	@Override
	public void start(Future<Void> future) throws Exception {
		super.start();
		
		// create the service instance
		IpnetService ipnetService = new IpnetServiceImpl(vertx, config());
		
		// register the service proxy on event bus
		new ServiceBinder(vertx)
			.setAddress(SERVICE_ADDRESS)
			.register(IpnetService.class, ipnetService);

		initConfigDatabase(ipnetService)
			.compose(databaseOkay -> deployHandler(ipnetService))
			.compose(handlerPrepared -> deployRestVerticle(ipnetService))
			.onComplete(future);
	}

	private Future<List<Integer>> initConfigDatabase(IpnetService service) {
		Promise<List<Integer>> initPromise = Promise.promise();
		service.initializePersistence(initPromise);
		return initPromise.future();
	}

	private Future<Void> deployHandler(IpnetService service) {
		Promise<String> promise = Promise.promise();
		vertx.deployVerticle(new EventHandler(service),
				new DeploymentOptions().setConfig(config()), promise);
		return promise.future().map(r -> null);
	}

	private Future<Void> deployRestVerticle(IpnetService service) {
		Promise<String> promise = Promise.promise();
		vertx.deployVerticle(new RestIpnetAPIVerticle(service),
				new DeploymentOptions().setConfig(config()), promise);
		return promise.future().map(r -> null);
	}

}