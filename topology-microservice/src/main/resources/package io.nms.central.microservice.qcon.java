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
import io.nms.central.microservice.qconnection.model.CrossConnects;
import io.nms.central.microservice.qconnection.model.pair;
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
import io.nms.central.microservice.topology.model.Vnode.NodeTypeEnum;
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
	private final Vertx vertx;
	private final MongoClient client;

	private Map<String, Integer> crossConnectsCreated = new HashMap<String, Integer>();
	private Map<Integer, String> mgmtIpbyswitchId = new HashMap<Integer, String>();
	private Map<Integer, String> portNbbyportId = new HashMap<Integer, String>();

	public QconnectionServiceImpl(Vertx vertx, JsonObject config) {
		this.vertx = vertx;
		this.client = MongoClient.create(vertx, config);
		this.webClient = WebClient.create(vertx);
	}

	@Override
	public QconnectionService initialize(Handler<AsyncResult<Void>> resultHandler) {
		getOptNodes(resultHandler);
		// sync method
		// apply desired state (in topology service) to the network
		return this;
	}

	private QconnectionService getOptNodes(Handler<AsyncResult<Void>> resultHandler) {
		ServiceProxyBuilder builder = new ServiceProxyBuilder(vertx).setAddress(TopologyService.SERVICE_ADDRESS);
		TopologyService service = builder.build(TopologyService.class);

		service.getVnodesByType(NodeTypeEnum.OPTSWITCH, ar -> {// use vnodebttype because dont have vsubnetid
			if (ar.succeeded()) {
				List<Vnode> nodes = ar.result();
				List<Future> allVltpsRead = new ArrayList<Future>();
				for (Vnode node : nodes) {
					mgmtIpbyswitchId.put(node.getId(), node.getMgmtIp());
					Promise<Void> pVltpRead = Promise.promise();
					allVltpsRead.add(pVltpRead.future());
					service.getVltpsByVnode(String.valueOf(node.getId()), ar2 -> {
						if (ar2.succeeded()) {
							List<Vltp> vltps = ar2.result();
							pVltpRead.complete();
							for (Vltp vltp : vltps)
								portNbbyportId.put(vltp.getId(), vltp.getPort());
						} else {
							pVltpRead.fail(ar2.cause());
						}
					});
				}
				CompositeFuture.all(allVltpsRead).map((Void) null).onComplete(resultHandler);
			} else {
				logger.info("ERROR in get VtrailbyId : " + ar.cause().getMessage());
				resultHandler.handle(Future.failedFuture(ar.cause()));
			}
		});
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

				resultHandler.handle(Future.succeededFuture(trailId)); // create Vtrail and return id of vtrail in
																		// result handler
				vtrail.setId(trailId);// create path on switch before creating it in the topology | why : avoid the
										// deletion in topology

				Promise<Void> pTrailAddedToSwitch = Promise.promise();
				CompletableFuture<Void> stage = CompletableFuture.completedFuture(null);

				for (CrossConnects crossConnects : trail.getOxcs()) {
					pair pair = new pair();

					pair.setIngress(Integer.parseInt(portNbbyportId.get(crossConnects.getIngressPortId())));
					int i = Integer.parseInt(portNbbyportId.get(crossConnects.getEgressPortId())) + 8;
					pair.setEgress(i);

					stage = stage.thenCompose(r -> createOXC(mgmtIpbyswitchId.get(crossConnects.getSwitchId()), pair));
				}

				stage.whenComplete((result, error) -> {
					if (error != null) {
						logger.info("fail to create crossconnects on switches " + error.getCause());
						pTrailAddedToSwitch.fail(error.getCause());
						rollbackTrail(trailId, ignore -> {
						});
					} else { // if the oxcs were created on the switch, we create them in the topology
						pTrailAddedToSwitch.complete();

						List<CrossConnects> cross_connects = trail.getOxcs();
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
							vcrossConnect.setStatus(StatusEnum.UP);

							service.addVcrossConnect(vcrossConnect, ar2 -> {
								if (ar2.succeeded()) {
									pOXCAdded.complete();
								} else {
									logger.info("fail to add Vcrossconnects to topology service" + sn.cause());
									pOXCAdded.fail(ar2.cause());
								}
							});
						});
						CompositeFuture.all(allOXCsAdded).map((Void) null).onComplete(res -> {
							if (res.succeeded()) {
								pTrailAddedToTopology.complete();

								// change status of Vtrail to UP
								Status status = new Status();
								status.setResId(trailId);
								status.setResType(ResTypeEnum.TRAIL);
								status.setStatus(StatusEnum.UP);
								status.setTimestamp(OffsetDateTime.now());
								status.setId(String.valueOf(status.hashCode()));
								vertx.eventBus().publish(NotificationService.STATUS_ADDRESS, status.toJson());
							} else {
								logger.info("fail to add Vtrail to topology service" + res.cause());
								pTrailAddedToTopology.fail(res.cause());
							}
						});
					}
				});
			} else {
				logger.info("fail to create Vtrail " + sn.cause());
				resultHandler.handle(Future.failedFuture(sn.cause()));
			}
		});
		return this;
	}

	private CompletableFuture<Void> createOXC(String mgmtIp, pair pair) {
		logger.info("createOXC in QconnectionServiceImpl");
		CompletableFuture<Void> cs = new CompletableFuture<>();
		CrossConnect crossconnects = new CrossConnect(pair);

		webClient.post(mgmtIp, "/api/data/optical-switch:cross-connects")
				.putHeader("Content-type", "application/yang-data+json")
				.putHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString("admin:root".getBytes()))
				.sendJson(crossconnects, response -> {
					if (response.succeeded()) {
						HttpResponse<Buffer> httpResponse = response.result();
						logger.info("Post Response : " + httpResponse.statusMessage());
						if (httpResponse.statusCode() == 201) {
							cs.complete(null);
							crossConnectsCreated.put(mgmtIp, pair.getIngress());// add to list of created
							// crossconnect
						} else
							cs.completeExceptionally(new RuntimeException("Conflict error!"));
					} else {
						logger.info("ERROR in POST: " + response.cause().getMessage());
						cs.completeExceptionally(response.cause());
					}
				});
		return cs;
	}

	private QconnectionService rollbackTrail(int trailId, Handler<AsyncResult<Void>> resultHandler) {
		logger.info("rollBack in QconnectionServiceImpl");

		CompletableFuture<Void> stage = CompletableFuture.completedFuture(null);
		for (Entry<String, Integer> entry : crossConnectsCreated.entrySet()) {
			stage = stage.thenCompose(r -> deleteOXC(entry.getKey(), entry.getValue()));
		}
		;
		stage.whenComplete((result, error) -> {
			if (error != null) {
				logger.info("ERROR in delete in switch: " + error.getCause());
			} else {
				// change status of Vtrail to EROOR
				Status status = new Status();
				status.setResId(trailId);
				status.setResType(ResTypeEnum.TRAIL);
				status.setStatus(StatusEnum.ERROR);
				status.setTimestamp(OffsetDateTime.now());
				status.setId(String.valueOf(status.hashCode()));
				vertx.eventBus().publish(NotificationService.STATUS_ADDRESS, status.toJson());
			}
		});

		return this;
	}

	public QconnectionService deletePath(String trailId, Handler<AsyncResult<Void>> resultHandler) {
		logger.info("deletePath in QconnectionService");
		ServiceProxyBuilder builder = new ServiceProxyBuilder(vertx).setAddress(TopologyService.SERVICE_ADDRESS);
		TopologyService service = builder.build(TopologyService.class);

		service.getVcrossConnectsByTrail(trailId, ar -> {
			if (ar.succeeded()) {
				CompletableFuture<Void> stage = CompletableFuture.completedFuture(null);

				for (VcrossConnect vcrossConnect : ar.result()) {
					stage = stage.thenCompose(r -> deleteOXC(mgmtIpbyswitchId.get(vcrossConnect.getSwitchId()),
							Integer.parseInt(portNbbyportId.get(vcrossConnect.getIngressPortId()))));
					logger.info("Integer.parseInt(portNbbyportId.get(vcrossConnect.getIngressPortId())) = "
							+ Integer.parseInt(portNbbyportId.get(vcrossConnect.getIngressPortId())));
				}

				stage.whenComplete((result, error) -> {
					if (error != null) {
						logger.info("ERROR in delete in switch: " + error.getCause());
					} else {
						service.deleteVtrail(trailId, ar3 -> {
							if (ar3.succeeded()) {
								resultHandler.handle(Future.succeededFuture());
							} else {
								logger.info("ERROR in delete Vtrail in topology service: " + ar3.cause().getMessage());
							}
						});
					}
				});

			} else {
				logger.info("ERROR in get VtrailbyId : " + ar.cause().getMessage());
				resultHandler.handle(Future.failedFuture(ar.cause()));
			}
		});

		return this;
	}

	private CompletableFuture<Void> deleteOXC(String mgmtIp, int pairIngress) {
		logger.info("deleteOXC in QconnectionService");
		CompletableFuture<Void> cs = new CompletableFuture<>();

		webClient.delete(mgmtIp, "/api/data/optical-switch:cross-connects/pair=" + pairIngress)
				.putHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString("admin:root".getBytes()))
				.send(response -> {
					if (response.succeeded()) {
						HttpResponse<Buffer> httpResponse = response.result();
						logger.info("Delete Response : " + httpResponse.statusMessage());
						if (httpResponse.statusCode() == 204)
							cs.complete(null);
						else
							cs.completeExceptionally(new RuntimeException("Conflict error!"));
					} else {
						logger.info("ERROR in delete in switch : " + response.cause().getMessage());
						cs.completeExceptionally(response.cause());
					}
				});
		return cs;
	}

	public QconnectionService doHealthCheck(Handler<AsyncResult<Void>> resultHandler) {
		resultHandler.handle(Future.succeededFuture());
		// call switches...
		return this;
	}
}