package io.nms.central.microservice.qnet;

import static io.nms.central.microservice.qnet.QnetService.SERVICE_ADDRESS;
import static io.nms.central.microservice.qnet.QnetService.SERVICE_NAME;

import io.nms.central.microservice.common.BaseMicroserviceVerticle;
import io.nms.central.microservice.qnet.api.RestQnetAPIVerticle;
import io.nms.central.microservice.qnet.impl.QnetServiceImpl;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.serviceproxy.ServiceBinder;


/**
 * A verticle publishing the qnet service.
 */
public class QnetVerticle extends BaseMicroserviceVerticle {

  @Override
  public void start(Future<Void> future) throws Exception {
    super.start();
    
    // create the service instance
    QnetService qnetService = new QnetServiceImpl(vertx, config());
    
    // register the service proxy on event bus
    new ServiceBinder(vertx)
        .setAddress(SERVICE_ADDRESS)
        .register(QnetService.class, qnetService);
    
    initQnetDatabase(qnetService)
    	.compose(databaseOkay -> deployRestVerticle(qnetService))
    	.onComplete(future);
  }
  
  private Future<Void> initQnetDatabase(QnetService service) {
	  Promise<Void> initPromise = Promise.promise();
	    service.initializePersistence(initPromise);
	    return initPromise.future();
  }

  private Future<Void> deployRestVerticle(QnetService service) {
    Promise<String> promise = Promise.promise();
    vertx.deployVerticle(new RestQnetAPIVerticle(service),
      new DeploymentOptions().setConfig(config()), promise);
    return promise.future().map(r -> null);
  }

}