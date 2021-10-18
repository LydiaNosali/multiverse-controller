package io.nms.central.microservice.account;

import static io.nms.central.microservice.account.AccountService.SERVICE_ADDRESS;
import static io.nms.central.microservice.account.AccountService.SERVICE_NAME;

import io.nms.central.microservice.account.api.RestAccountAPIVerticle;
import io.nms.central.microservice.account.impl.AccountServiceImpl;
import io.nms.central.microservice.common.BaseMicroserviceVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.serviceproxy.ServiceBinder;


/**
 * A verticle publishing the accounts service.
 *
 */
public class AccountVerticle extends BaseMicroserviceVerticle {

	@Override
	public void start(Future<Void> future) throws Exception {
		super.start();
		// create the service instance
		AccountService accountService = new AccountServiceImpl(vertx, config());
		
		// register the service proxy on event bus
		new ServiceBinder(vertx)
  			.setAddress(SERVICE_ADDRESS)
  			.register(AccountService.class, accountService);

		initAuthDatabase(accountService)
	//		.compose(databaseOkay -> publishEventBusService(SERVICE_NAME, SERVICE_ADDRESS, AccountService.class))
			.compose(databaseOkay -> deployRestVerticle(accountService))
			.onComplete(future);
	}

	private Future<Void> initAuthDatabase(AccountService service) {
		Promise<Void> initPromise = Promise.promise();
		service.initializePersistence(initPromise);
		return initPromise.future();
	}

	private Future<Void> deployRestVerticle(AccountService service) {
		Promise<String> promise = Promise.promise();
		vertx.deployVerticle(new RestAccountAPIVerticle(service),
				new DeploymentOptions().setConfig(config()), promise);
		return promise.future().map(r -> null);
	}

}