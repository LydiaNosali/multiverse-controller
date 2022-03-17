package io.nms.central.microservice.qconnection.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import io.nms.central.microservice.qconnection.model.CrossConnect;
import io.nms.central.microservice.qconnection.model.PolatisPair;
import io.nms.central.microservice.common.BaseMicroserviceVerticle;
import io.nms.central.microservice.notification.model.Status.StatusEnum;
import io.nms.central.microservice.qconnection.QconnectionService;
import io.nms.central.microservice.qconnection.model.Trail;
import io.nms.central.microservice.topology.TopologyService;
import io.nms.central.microservice.topology.model.VcrossConnect;
import io.nms.central.microservice.topology.model.Vltp;
import io.nms.central.microservice.topology.model.Vnode;
import io.nms.central.microservice.topology.model.Vtrail;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.serviceproxy.ServiceProxyBuilder;

/**
 *
 */
public class QconnectionServiceImpl extends BaseMicroserviceVerticle implements QconnectionService {

	private static final Logger logger = LoggerFactory.getLogger(QconnectionServiceImpl.class);

	private final WebClient webClient;

	private final MongoClient client;

	private List<CrossConnect> crossConnectsCreated = new ArrayList<CrossConnect>();

	public QconnectionServiceImpl(Vertx vertx, JsonObject config) {
		this.client = MongoClient.create(vertx, config);
		this.webClient = WebClient.create(vertx);
	}

	@Override
	public QconnectionService initialize(Handler<AsyncResult<Void>> resultHandler) {
		resultHandler.handle(Future.succeededFuture());
		return this;
	}

	@Override
	public QconnectionService createPath(Trail trail, String finish, Handler<AsyncResult<Integer>> resultHandler) {
		// do necessary verifications -- param of trail and finish
		// create Vtrail and Voxc in topology (status = PENDING)
		// return Vtrail Id / call resultHandler
		// create OXCs on switches
		// update status (should = UP)

		ServiceProxyBuilder builder = new ServiceProxyBuilder(vertx).setAddress(TopologyService.SERVICE_ADDRESS);
		TopologyService service = builder.build(TopologyService.class);

		Promise<Void> pTrailAdded = Promise.promise();
		Vtrail vtrail = new Vtrail();
		vtrail.setName(trail.getName());
		vtrail.setLabel(trail.getLabel());
		vtrail.setDescription(trail.getDescription());
		vtrail.setStatus(StatusEnum.PENDING);
		vtrail.setVsubnetId(trail.getVsubnetId());
		vtrail.setInfo(trail.getInfo());

		service.addVtrail(vtrail, sn -> {
			if (sn.succeeded()) {
				int trailId = sn.result();
				// return id of vtrail in result handler
				resultHandler.handle(Future.succeededFuture(trailId));
				// create path on switch before creating it in the topology
				// why : avoid the deletion in topology
				CompletableFuture<Void> stage = CompletableFuture.completedFuture(null);

				for (CrossConnect crossConnect : trail.getOxcs()) {
					PolatisPair polatisPair = new PolatisPair();

					// ingressportnumber
					Promise<Vltp> pltpIngress = Promise.promise();
					service.getVltp(String.valueOf(crossConnect.getIngressPortId()), pltpIngress);
					// egressportnumber
					Promise<Vltp> pltpEgress = Promise.promise();
					service.getVltp(String.valueOf(crossConnect.getEgressPortId()), pltpEgress);
					// switchip
					Promise<Vnode> pNode = Promise.promise();
					service.getVnode(String.valueOf(crossConnect.getSwitchId()), pNode);
					
					polatisPair.setIngress(Integer.parseInt(pltpIngress.future().result().getPort()));
					polatisPair.setEgress(Integer.parseInt(pltpEgress.future().result().getPort()) + 8);
					
					stage = stage.thenCompose(r -> createOXC(pNode.future().result().getMgmtIp(), polatisPair));
				}
				
				stage.whenComplete((result, error) -> {
					// TODO update status by update here or in notif microservice
					if (error != null) {
						pTrailAdded.fail(error.getCause());
						deleteCrossConnectsCreated(ignore -> {
						});
						resultHandler.handle(Future.failedFuture(error.getCause()));
					} else {
						pTrailAdded.complete();
						resultHandler.handle(Future.succeededFuture(0));
					}
				});
				
				
				
				List<CrossConnect> cross_connects = trail.getOxcs();

				List<Future> allOXCsAdded = new ArrayList<Future>();
				cross_connects.forEach(e -> {
					Promise<Void> pOXCAdded = Promise.promise();
					allOXCsAdded.add(pOXCAdded.future());

					VcrossConnect vcrossConnect = new VcrossConnect();
					vcrossConnect.setName(trail.getName() + "");
					vcrossConnect.setLabel("");
					vcrossConnect.setDescription("");
					vcrossConnect.setTrailId(trailId);
					vcrossConnect.setSwitchId(e.getSwitchId());
					vcrossConnect.setIngressPortId(e.getIngressPortId());
					vcrossConnect.setEgressPortId(e.getEgressPortId());
					vcrossConnect.setStatus(StatusEnum.PENDING);

					service.addVcrossConnect(vcrossConnect, ar2 -> {
						if (ar2.succeeded()) {
							pOXCAdded.complete();
						} else {
							pOXCAdded.fail(ar2.cause());
						}
					});
				});
				CompositeFuture.all(allOXCsAdded).map((Void) null).onComplete(pTrailAdded);
			} else {
				pTrailAdded.fail(sn.cause());
			}
		});

//		CompletableFuture<Void> stage = CompletableFuture.completedFuture(null);
//		for (CrossConnect crossConnect : trail.getOxcs()) {
//
//			// ingressportnumber
//			Promise<Vltp> pltpIngress = Promise.promise();
//			service.getVltp(String.valueOf(crossConnect.getIngressPortId()), pltpIngress);
//			// egressportnumber
//			Promise<Vltp> pltpEgress = Promise.promise();
//			service.getVltp(String.valueOf(crossConnect.getEgressPortId()), pltpEgress);
//			// switchname
//			Promise<Vnode> pNode = Promise.promise();
//			service.getVnode(String.valueOf(crossConnect.getSwitchId()), pNode);
//			// create new class polatis (obj pair) to add to the network directly
//			CrossConnect physical_crossConnect = new CrossConnect();
//			physical_crossConnect.setId(crossConnect.getId());
//			physical_crossConnect.setIngressPortId(Integer.parseInt(pltpIngress.future().result().getPort()));
//			physical_crossConnect.setEgressPortId(Integer.parseInt(pltpEgress.future().result().getPort()) + 8);// engress
//																												// port
//			// number + 8
//			physical_crossConnect.setSwitchId(Integer.parseInt(pNode.future().result().getName()));
//			physical_crossConnect.setStatus(crossConnect.getStatus());
//
//			stage = stage.thenCompose(r -> createOXC(physical_crossConnect));
//		}
//		stage.whenComplete((result, error) -> {
//			// TODO update status by update here or in notif microservice
//			if (error != null) {
//				pTrailAdded.fail(error.getCause());
//				deleteCrossConnectsCreated(ignore -> {
//				});
//				resultHandler.handle(Future.failedFuture(error.getCause()));
//			} else {
//				pTrailAdded.complete();
//				resultHandler.handle(Future.succeededFuture(0));
//			}
//		});
		return this;
	}

	private CompletableFuture<Void> createOXC(CrossConnect crossConnect) {
		ServiceProxyBuilder builder = new ServiceProxyBuilder(vertx).setAddress(TopologyService.SERVICE_ADDRESS);
		TopologyService service = builder.build(TopologyService.class);

		Promise<Vnode> pNode = Promise.promise();

		service.getVnode(String.valueOf(crossConnect.getSwitchId()), pNode);

		CompletableFuture<Void> cs = new CompletableFuture<>();
		// create map (switchid, mgmtip)
		// add the items in create method, use in deleteoxc
		webClient.put(pNode.future().result().getMgmtIp()).sendJson(crossConnect, response -> {
			if (response.succeeded()) {
				HttpResponse<Buffer> httpResponse = response.result();
				System.out.println("Post Response : " + httpResponse.statusMessage());
				if (httpResponse.statusCode() == 201) {
					cs.complete(null);
					crossConnectsCreated.add(crossConnect);// add to list of created crossconnect
				} else
					cs.completeExceptionally(new RuntimeException("Conflict error!"));
			} else {
				System.out.println("ERROR : " + response.cause().getMessage());
				cs.completeExceptionally(response.cause());
			}
		});
		return cs;
	}

	public QconnectionService deleteCrossConnectsCreated(Handler<AsyncResult<Void>> resultHandler) {
		ServiceProxyBuilder builder = new ServiceProxyBuilder(vertx).setAddress(TopologyService.SERVICE_ADDRESS);
		TopologyService service = builder.build(TopologyService.class);
		for (CrossConnect crossConnect : crossConnectsCreated) {
			Promise<Vnode> pNode = Promise.promise();
			service.getVnode(String.valueOf(crossConnect.getSwitchId()), pNode);
			webClient.delete(pNode.future().result().getMgmtIp()).sendJson(crossConnect, response -> {
				if (response.succeeded()) {
					HttpResponse<Buffer> httpResponse = response.result();
					System.out.println("Post Response : " + httpResponse.statusMessage());
					resultHandler.handle(Future.succeededFuture());
				} else {
					System.out.println("ERROR : " + response.cause().getMessage());
					resultHandler.handle(Future.failedFuture(response.cause()));
				}
			});
		}
		;
		return this;
	}

	public QconnectionService doHealthCheck(Handler<AsyncResult<Void>> resultHandler) {
		resultHandler.handle(Future.succeededFuture());
		// call switches...
		return this;
	}

}