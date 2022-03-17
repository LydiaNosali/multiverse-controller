
package io.nms.central.microservice.qconnection.impl;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import io.nms.central.microservice.qconnection.model.CrossConnect;
import io.nms.central.microservice.qconnection.model.PolatisPair;
import io.nms.central.microservice.common.BaseMicroserviceVerticle;
import io.nms.central.microservice.notification.NotificationService;
import io.nms.central.microservice.notification.model.Status;
import io.nms.central.microservice.notification.model.Status.ResTypeEnum;
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

	private Map<Vnode, PolatisPair> crossConnectsCreated = new HashMap<Vnode, PolatisPair>();

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
		// create OXCs on switches
		// create Vtrail and Voxc in topology (status = PENDING)
		// return Vtrail Id / call resultHandler
		// update status (should = UP)
		logger.info("createPath in QconnectionService");
		ServiceProxyBuilder builder = new ServiceProxyBuilder(vertx).setAddress(TopologyService.SERVICE_ADDRESS);
		TopologyService service = builder.build(TopologyService.class);

		Vtrail vtrail = new Vtrail();
		vtrail.setName(trail.getName());
		vtrail.setLabel(trail.getLabel());
		vtrail.setDescription(trail.getDescription());
		vtrail.setStatus(StatusEnum.PENDING);
		vtrail.setVsubnetId(trail.getVsubnetId());
		vtrail.setInfo(trail.getInfo());

		Promise<Void> pTrailAddedToTopology = Promise.promise();

		service.addVtrail(vtrail, sn -> {
			if (sn.succeeded()) {
				int trailId = sn.result();
				// create Vtrail and return id of vtrail in result handler
				resultHandler.handle(Future.succeededFuture(trailId));
				vtrail.setId(trailId);
				// create path on switch before creating it in the topology
				// why : avoid the deletion in topology
				Promise<Void> pTrailAddedToSwitch = Promise.promise();
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

					stage = stage.thenCompose(r -> createOXC(pNode.future().result(), polatisPair));
				}

				stage.whenComplete((result, error) -> {
					if (error != null) {// if error in switch rollback and do not create in topology
						pTrailAddedToSwitch.fail(error.getCause());
						rollbackTrail(ignore -> {
						});
						resultHandler.handle(Future.failedFuture(error.getCause()));
					} else { // if the oxcs were created on the switch, we create them in the topology
						pTrailAddedToSwitch.complete();
						// create voxcs in the topology
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
									// change status of oxc to UP
									Status status = new Status();
									status.setResType(ResTypeEnum.XC);
									status.setStatus(StatusEnum.UP);
									status.setTimestamp(OffsetDateTime.now());
									status.setId(String.valueOf(status.hashCode()));
									vertx.eventBus().publish(NotificationService.STATUS_ADDRESS, status.toJson());
								} else {
									pOXCAdded.fail(ar2.cause());
								}
							});
						});
						CompositeFuture.all(allOXCsAdded).map((Void) null).onComplete(pTrailAddedToTopology);
						resultHandler.handle(Future.succeededFuture(0));
						// change status of Vtrail to UP
						Status status = new Status();
						status.setResType(ResTypeEnum.TRAIL);
						status.setStatus(StatusEnum.UP);
						status.setTimestamp(OffsetDateTime.now());
						status.setId(String.valueOf(status.hashCode()));
						vertx.eventBus().publish(NotificationService.STATUS_ADDRESS, status.toJson());
					}
				});
			} else {
				pTrailAddedToTopology.fail(sn.cause());
				resultHandler.handle(Future.failedFuture(sn.cause()));
			}
		});
		return this;
	}

	private CompletableFuture<Void> createOXC(Vnode pNode, PolatisPair polatisPair) {
		logger.info("createOXC in QconnectionServiceImpl");
		CompletableFuture<Void> cs = new CompletableFuture<>();
		// add the items in create method, use in rollbackTrail
		webClient.put(pNode.getMgmtIp()).putHeader("Content-type", "application/yang-data+json")
				.putHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString("admin:root".getBytes()))
				.sendJson(polatisPair, response -> {
					if (response.succeeded()) {
						HttpResponse<Buffer> httpResponse = response.result();
						System.out.println("Post Response : " + httpResponse.statusMessage());
						if (httpResponse.statusCode() == 201) {
							cs.complete(null);
							crossConnectsCreated.put(pNode, polatisPair);// add to list of created crossconnect
						} else
							cs.completeExceptionally(new RuntimeException("Conflict error!"));
					} else {
						System.out.println("ERROR : " + response.cause().getMessage());
						cs.completeExceptionally(response.cause());
					}
				});
		return cs;
	}

	private QconnectionService rollbackTrail(Handler<AsyncResult<Void>> resultHandler) {
		for (Entry<Vnode, PolatisPair> entry : crossConnectsCreated.entrySet()) {
			webClient.delete(entry.getKey().getMgmtIp()).putHeader("Content-type", "application/yang-data+json")
					.putHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString("admin:root".getBytes()))
					.sendJson(entry.getValue(), response -> {
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

	public QconnectionService deletePath(String trailId, Handler<AsyncResult<Void>> resultHandler) {
		// get XCs from topology (map with LTPs port<->id)
		// delete XCs on switches
		// delete Vtrail in topology service
		ServiceProxyBuilder builder = new ServiceProxyBuilder(vertx).setAddress(TopologyService.SERVICE_ADDRESS);
		TopologyService service = builder.build(TopologyService.class);

		service.getVcrossConnectsByTrail(trailId, ar -> {
			if (ar.succeeded()) {
				for (VcrossConnect vcrossConnect : ar.result()) {

					PolatisPair polatisPair = new PolatisPair();
					// ingressportnumber
					Promise<Vltp> pltpIngress = Promise.promise();
					service.getVltp(String.valueOf(vcrossConnect.getIngressPortId()), pltpIngress);

					// egressportnumber
					Promise<Vltp> pltpEgress = Promise.promise();
					service.getVltp(String.valueOf(vcrossConnect.getEgressPortId()), pltpEgress);

					// switchip
					Promise<Vnode> pNode = Promise.promise();
					service.getVnode(String.valueOf(vcrossConnect.getSwitchId()), pNode);

					polatisPair.setIngress(Integer.parseInt(pltpIngress.future().result().getPort()));
					polatisPair.setEgress(Integer.parseInt(pltpEgress.future().result().getPort()) + 8);

					webClient.delete(pNode.future().result().getMgmtIp())
							.putHeader("Content-type", "application/yang-data+json")
							.putHeader("Authorization",
									"Basic " + Base64.getEncoder().encodeToString("admin:root".getBytes()))
							.sendJson(polatisPair, response -> {
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
				resultHandler.handle(Future.succeededFuture());
			} else {
				System.out.println("ERROR : " + ar.cause().getMessage());
				resultHandler.handle(Future.failedFuture(ar.cause()));

			}
		});
		return this;
	}

	public QconnectionService doHealthCheck(Handler<AsyncResult<Void>> resultHandler) {
		resultHandler.handle(Future.succeededFuture());
		// call switches...
		return this;
	}
}