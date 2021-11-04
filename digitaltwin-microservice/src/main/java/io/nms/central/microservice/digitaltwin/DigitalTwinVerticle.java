package io.nms.central.microservice.digitaltwin;

import static io.nms.central.microservice.digitaltwin.DigitalTwinService.SERVICE_ADDRESS;

import io.nms.central.microservice.common.BaseMicroserviceVerticle;
import io.nms.central.microservice.digitaltwin.api.RestDigitalTwinAPIVerticle;
import io.nms.central.microservice.digitaltwin.impl.DigitalTwinServiceImpl;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.serviceproxy.ServiceBinder;


/**
 * A verticle publishing the digital twin service.
 */
public class DigitalTwinVerticle extends BaseMicroserviceVerticle {

  @Override
  public void start(Future<Void> future) throws Exception {
    super.start();
    
    // create the service instance
    DigitalTwinService digitalTwinService = new DigitalTwinServiceImpl(vertx, config());
    
    // register the service proxy on event bus
    new ServiceBinder(vertx)
        .setAddress(SERVICE_ADDRESS)
        .register(DigitalTwinService.class, digitalTwinService);
    
    initDigitaltwinDatabase(digitalTwinService)
    	.compose(r -> deployRestVerticle(digitalTwinService))
    	.onComplete(future);
  }
  
  private Future<Void> initDigitaltwinDatabase(DigitalTwinService service) {
	  Promise<Void> initPromise = Promise.promise();
	    service.initializePersistence(initPromise);
	    return initPromise.future();
  }

  private Future<Void> deployRestVerticle(DigitalTwinService service) {
    Promise<String> promise = Promise.promise();
    vertx.deployVerticle(new RestDigitalTwinAPIVerticle(service),
      new DeploymentOptions().setConfig(config()), promise);
    return promise.future().map(r -> null);
  }
}