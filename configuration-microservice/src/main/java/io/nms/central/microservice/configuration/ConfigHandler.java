package io.nms.central.microservice.configuration;

import java.util.List;

import io.nms.central.microservice.common.BaseMicroserviceVerticle;
import io.nms.central.microservice.topology.TopologyService;
import io.nms.central.microservice.topology.model.Route;
import io.nms.central.microservice.topology.model.Vctp;
import io.nms.central.microservice.topology.model.Vnode;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.impl.CompositeFutureImpl;
import io.vertx.core.json.JsonObject;
import io.vertx.servicediscovery.types.MessageSource;
import io.vertx.serviceproxy.ServiceProxyBuilder;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class ConfigHandler extends BaseMicroserviceVerticle {

	private static final Logger logger = LoggerFactory.getLogger(ConfigHandler.class);
	private final ConfigurationService configService;

	public ConfigHandler(ConfigurationService configService) {
		this.configService = configService;
	}

	@Override
	public void start(Promise<Void> promise) throws Exception {
		super.start(promise);
		vertx.eventBus().consumer(TopologyService.EVENT_ADDRESS, ar ->{
			generateConfig();
		});
		
		/* MessageSource.<JsonObject>getConsumer(discovery,
				new JsonObject().put("name", "topology-message-source"),
				ar -> {
					if (ar.succeeded() && ar.result() != null) {
						MessageConsumer<JsonObject> eventConsumer = ar.result();
						eventConsumer.handler(message -> {
							generateConfig(message);
						});
						promise.complete();
						// setTopologyEventListener(promise);
					} else {
						promise.fail(ar.cause());
					}
				}); */
	}

	private void generateConfig() {
		logger.info("Topology change -> update configuration");
		ServiceProxyBuilder builder = new ServiceProxyBuilder(vertx)
  			.setAddress(TopologyService.SERVICE_ADDRESS);
		TopologyService service = builder.build(TopologyService.class);

		Promise<List<Route>> pRoute = Promise.promise();
		Promise<List<Vctp>> pFace = Promise.promise();
		Promise<List<Vnode>> pNode = Promise.promise();
		service.getAllRoutes(pRoute);
		service.getVctpsByType("NDN", pFace);
		service.getAllVnodes(pNode);
		CompositeFutureImpl.all(pRoute.future(), pFace.future(), pNode.future()).onComplete(res -> {
			if (res.succeeded()) {
				List<Route> routes = pRoute.future().result();
				List<Vctp> faces = pFace.future().result();
				List<Vnode> nodes = pNode.future().result();
				configService.computeConfigurations(routes, faces, nodes, done -> {
					if (done.succeeded()) {
						configService.upsertCandidateConfigs(done.result(), r -> {
							if (r.succeeded()) {
								notifyFrontend();
							} // else {
								// sender.fail(5000, r.cause().getMessage());
							// }
						});
					} // else {
						// sender.fail(5000, done.cause().getMessage());
					// }
				});
			} // else {
			//	sender.fail(5000, res.cause().getMessage());
			// }
		});
	}

	private void notifyFrontend() {
		vertx.eventBus().publish(ConfigurationService.FROTNEND_ADDRESS, new JsonObject());
	}
}
