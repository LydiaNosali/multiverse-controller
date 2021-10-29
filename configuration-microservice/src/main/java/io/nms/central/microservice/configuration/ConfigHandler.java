package io.nms.central.microservice.configuration;

import java.util.List;

import io.nms.central.microservice.common.BaseMicroserviceVerticle;
import io.nms.central.microservice.topology.TopologyService;
import io.nms.central.microservice.topology.model.Prefix;
import io.nms.central.microservice.topology.model.Vconnection;
import io.nms.central.microservice.topology.model.Vctp;
import io.nms.central.microservice.topology.model.Vnode;
import io.nms.central.microservice.topology.model.Vctp.ConnTypeEnum;
import io.nms.central.microservice.topology.model.Vnode.NodeTypeEnum;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
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
		vertx.eventBus().consumer(TopologyService.EVENT_ADDRESS, ar -> {
			// TODO: test type of change
			generateConfig();
		});
	}

	private void generateConfig() {
		logger.info("Topology change: update configuration");
		ServiceProxyBuilder builder = new ServiceProxyBuilder(vertx)
  			.setAddress(TopologyService.SERVICE_ADDRESS);
		TopologyService service = builder.build(TopologyService.class);

		Promise<List<Vnode>> pNodes = Promise.promise();
		Promise<List<Vconnection>> pEdges = Promise.promise();
		Promise<List<Vctp>> pFaces = Promise.promise();
		Promise<List<Prefix>> pPAs = Promise.promise();
		
		service.getVnodesByType(NodeTypeEnum.NDNFWD, pNodes);
		service.getAllPrefixes(pPAs);
		service.getVctpsByType(ConnTypeEnum.NDN, pFaces);
		service.getVconnectionsByType(ConnTypeEnum.NDN, pEdges);

		CompositeFuture.all(pNodes.future(), pEdges.future(), pFaces.future(), pPAs.future())
				.onComplete(res -> {
			if (res.succeeded()) {
				List<Vnode> nodes = pNodes.future().result();
				List<Vconnection> edges = pEdges.future().result();
				List<Vctp> faces = pFaces.future().result();
				List<Prefix> pas = pPAs.future().result();
				
				configService.computeConfigurations(nodes, edges, faces, pas, done -> {
					if (done.succeeded()) {
						configService.upsertCandidateConfigs(done.result(), r -> {
							if (r.succeeded()) {
								notifyFrontend();
							}
						});
					}
				});
			}
		});
	}

	private void notifyFrontend() {
		vertx.eventBus().publish(ConfigurationService.FROTNEND_ADDRESS, new JsonObject());
	}
}
