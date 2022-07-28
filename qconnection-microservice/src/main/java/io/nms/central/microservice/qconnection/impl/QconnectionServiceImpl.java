package io.nms.central.microservice.qconnection.impl;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import com.fasterxml.jackson.core.type.TypeReference;
import io.nms.central.microservice.qconnection.model.PolatisCrossConnect;
import io.nms.central.microservice.qconnection.model.CrossConnect;
import io.nms.central.microservice.qconnection.model.PolatisPair;
import io.nms.central.microservice.common.BaseMicroserviceVerticle;
import io.nms.central.microservice.common.functional.JsonUtils;
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
import io.nms.central.microservice.topology.model.Vsubnet;
import io.nms.central.microservice.topology.model.Vsubnet.SubnetTypeEnum;
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
	private Map<Integer, List<VcrossConnect>> dbOXCsbytrailId = new HashMap<Integer, List<VcrossConnect>>();
	private Map<Integer, List<PolatisPair>> polatisPairbySwitchId = new HashMap<Integer, List<PolatisPair>>();
	private Map<Integer, List<VcrossConnect>> voxcsbySwitchId = new HashMap<Integer, List<VcrossConnect>>();
	private Map<Integer, List<Integer>> portIdbySwitchId = new HashMap<Integer, List<Integer>>();
	private int default_cliId = 0;
	private int default_mvsId = 0;

	public QconnectionServiceImpl(Vertx vertx, JsonObject config) {
		this.vertx = vertx;
		this.client = MongoClient.create(vertx, config);
		this.webClient = WebClient.create(vertx);
	}

	@Override
	public QconnectionService initialize(Handler<AsyncResult<Void>> resultHandler) {
		logger.info("initialize in QconnectionService");
		getOpticalNetwork(res -> {
			if (res.succeeded()) {
				synchNetworkWithTopology(r -> {
					resultHandler.handle(Future.succeededFuture());
					if (r.succeeded()) {
						logger.info("Synchronization done successfuly");
					} else {
						logger.info("synchronization ERROR : " + r.cause());
					}
				});
			} else {
				logger.info("Update ERROR : " + res.cause());
			}
		});
		// TODO periodic update
//		vertx.setPeriodic(5 * 60 * 1000, id -> { // add synchro from network to
//		topology getOpticalNetwork(re -> { if (re.succeeded()) {
//		logger.info("Update done successfuly"); } else {
//		logger.info("ERROR, update went wrong"); } }); });

		// TODO notification per switch instead of periodic gets on all switches
		return this;
	}

	private QconnectionService getOpticalNetwork(Handler<AsyncResult<Void>> resultHandler) {
		logger.info("getOpticalNetwork in QconnectionService");
		dbOXCsbytrailId.clear();
		mgmtIpbyswitchId.clear();
		portNbbyportId.clear();
		polatisPairbySwitchId.clear();
		voxcsbySwitchId.clear();
		portIdbySwitchId.clear();

		ServiceProxyBuilder builder = new ServiceProxyBuilder(vertx).setAddress(TopologyService.SERVICE_ADDRESS);
		TopologyService service = builder.build(TopologyService.class);

		service.getVnodesByType(NodeTypeEnum.OPTSWITCH, ar -> {
			if (ar.succeeded()) {
				List<Vnode> nodes = ar.result();
				List<Future> allVltpsReadVcrossconnectsRead = new ArrayList<Future>();
				for (Vnode node : nodes) {
					mgmtIpbyswitchId.put(node.getId(), node.getMgmtIp());
					Promise<Void> pVltpRead = Promise.promise();
					allVltpsReadVcrossconnectsRead.add(pVltpRead.future());

					service.getVltpsByVnode(String.valueOf(node.getId()), ar2 -> {
						if (ar2.succeeded()) {
							List<Vltp> vltps = ar2.result();
							List<Integer> portsId = new ArrayList<Integer>();
							for (Vltp vltp : vltps) {
								portNbbyportId.put(vltp.getId(), vltp.getPort());
								portsId.add(vltp.getId());
							}
							portIdbySwitchId.put(node.getId(), portsId);
							pVltpRead.complete();
						} else {
							pVltpRead.fail(ar2.cause());
						}
					});
					Promise<Void> pVcrossconectsRead = Promise.promise();
					allVltpsReadVcrossconnectsRead.add(pVcrossconectsRead.future());
					service.getVcrossConnectsByNode(String.valueOf(node.getId()), ar3 -> {
						if (ar3.succeeded()) {
							List<VcrossConnect> vcrossConnects = ar3.result();
							voxcsbySwitchId.put(node.getId(), vcrossConnects);
							pVcrossconectsRead.complete();
						} else {
							pVcrossconectsRead.fail(ar3.cause());
						}
					});

					// From network
					Promise<Void> pOxcsRead = Promise.promise();
					allVltpsReadVcrossconnectsRead.add(pOxcsRead.future());
					getallOXC(node.getMgmtIp(), res -> {
						if (res.succeeded()) {
							polatisPairbySwitchId.put(node.getId(), res.result());
							pOxcsRead.complete();
						} else {
							pOxcsRead.fail(res.cause());
						}
					});
				}
				CompositeFuture.all(allVltpsReadVcrossconnectsRead).map((Void) null).onComplete(res -> {
					service.getVsubnetsByType(SubnetTypeEnum.QNET, r -> {
						if (r.succeeded()) {
							List<Vsubnet> vsubnets = r.result();
							List<Future> allVtrailRead = new ArrayList<Future>();
							for (Vsubnet vsubnet : vsubnets) {
								Promise<Void> pAllVtrailsOfVsubnetRead = Promise.promise();
								allVtrailRead.add(pAllVtrailsOfVsubnetRead.future());
								service.getVtrailsByVsubnet(String.valueOf(vsubnet.getId()), ar2 -> {
									if (ar2.succeeded()) {
										List<Vtrail> vtrails = ar2.result();

										List<Future> allVoxcsRead = new ArrayList<Future>();
										for (Vtrail vtrail : vtrails) {
											if (vtrail.getName().contains("default-cli")) {
												default_cliId = vtrail.getId();
											}
											if (vtrail.getName().contains("default-mvs")) {
												default_mvsId = vtrail.getId();
											}

											Promise<Void> pVoxcRead = Promise.promise();
											allVoxcsRead.add(pVoxcRead.future());
											service.getVcrossConnectsByTrail(String.valueOf(vtrail.getId()), ar3 -> {
												if (ar3.succeeded()) {
													List<VcrossConnect> vcrossConnects = ar3.result();
													dbOXCsbytrailId.put(vtrail.getId(), vcrossConnects);
													pVoxcRead.complete();
												} else {
													logger.info("ERROR in get VcrossconnectsbyVtrails: "
															+ ar3.cause().getMessage());
													pVoxcRead.fail(ar3.cause());
												}
											});
										}
										CompositeFuture.all(allVoxcsRead).map((Void) null)
												.onComplete(pAllVtrailsOfVsubnetRead);
									} else {
										logger.info("ERROR in get VtrailsbyVsubnet : " + ar2.cause().getMessage());
										pAllVtrailsOfVsubnetRead.fail(ar2.cause());
									}
								});
							}
							CompositeFuture.all(allVtrailRead).map((Void) null).onComplete(resultHandler);
						} else {
							logger.info("ERROR in get Vsubnets : " + r.cause().getMessage());
							resultHandler.handle(Future.failedFuture(r.cause()));
						}
					});
				});
			} else {
				logger.info("ERROR in get Vnodebytype : " + ar.cause().getMessage());
				resultHandler.handle(Future.failedFuture(ar.cause()));
			}
		});

		return this;
	}

	private QconnectionService synchTopologyToNetwork(Handler<AsyncResult<Void>> resultHandler) {
		logger.info("synchTopologyToNetwork in QconnectionService");

		List<Future> allTrailsUP = new ArrayList<Future>();
		for (Entry<Integer, List<VcrossConnect>> entry : dbOXCsbytrailId.entrySet()) {
			crossConnectsCreated.clear();
			Promise<Void> pTrailUP = Promise.promise();
			allTrailsUP.add(pTrailUP.future());
			CompletableFuture<Void> stage = CompletableFuture.completedFuture(null);
			for (VcrossConnect vcrossconnect : entry.getValue()) {

				PolatisPair polatisPair = new PolatisPair();
				polatisPair.setIngress(Integer.parseInt(portNbbyportId.get(vcrossconnect.getIngressPortId())));
				int i = Integer.parseInt(portNbbyportId.get(vcrossconnect.getEgressPortId())) + 8;
				polatisPair.setEgress(i);
				if (!polatisPairbySwitchId.get(entry.getKey()).contains(polatisPair)) {
					stage = stage.thenCompose(
							r -> createOXC(mgmtIpbyswitchId.get(vcrossconnect.getSwitchId()), polatisPair));
				}
			}
			stage.whenComplete((result, error) -> {
				if (error != null) {
					logger.info("fail to upload Trail on switches " + error.getCause());
					rollbackTrail(entry.getKey(), res2 -> {
						updateStatus(entry.getKey(), ResTypeEnum.TRAIL, StatusEnum.ERROR);
						for (VcrossConnect vcrossconnect : entry.getValue()) {
							updateStatus(vcrossconnect.getId(), ResTypeEnum.XC, StatusEnum.ERROR);
						}
					});
					pTrailUP.fail(error.getCause());
				} else {
					pTrailUP.complete();
					updateStatus(entry.getKey(), ResTypeEnum.TRAIL, StatusEnum.UP);
					for (VcrossConnect vcrossconnect : entry.getValue()) {
						updateStatus(vcrossconnect.getId(), ResTypeEnum.XC, StatusEnum.UP);
					}
				}
			});
		}
		CompositeFuture.all(allTrailsUP).map((Void) null).onComplete(resultHandler);
		return this;
	}

	private QconnectionService synchNetworkWithTopology(Handler<AsyncResult<Void>> resultHandler) {
		logger.info("synchNetworkToTopology in QconnectionService");
		ServiceProxyBuilder builder = new ServiceProxyBuilder(vertx).setAddress(TopologyService.SERVICE_ADDRESS);
		TopologyService service = builder.build(TopologyService.class);

		// turn voxcsbySwitchId to polatisPairbySwitchId
		if (!voxcsbySwitchId.isEmpty() || !polatisPairbySwitchId.isEmpty()) {
			Map<Integer, List<PolatisPair>> dbPolatispairBySwitchId = new HashMap<Integer, List<PolatisPair>>();
			for (Entry<Integer, List<VcrossConnect>> entry : voxcsbySwitchId.entrySet()) {
				List<PolatisPair> oxcsintopology = new ArrayList<PolatisPair>();
				for (VcrossConnect vcrossConnect : entry.getValue()) {
					PolatisPair PolatisPair = new PolatisPair();
					PolatisPair.setIngress(Integer.parseInt(portNbbyportId.get(vcrossConnect.getIngressPortId())));
					int i = Integer.parseInt(portNbbyportId.get(vcrossConnect.getEgressPortId())) + 8;
					PolatisPair.setEgress(i);
					oxcsintopology.add(PolatisPair);
				}
				dbPolatispairBySwitchId.put(entry.getKey(), oxcsintopology);
			}

			// compare dbPolatispairBySwitchId (DB) and polatisPairbySwitchId (Netwrok)
			if (polatisPairbySwitchId.equals(dbPolatispairBySwitchId)) {
				logger.info("polatisPairbySwitchId equals dbPolatispairBySwitchId");
				resultHandler.handle(Future.succeededFuture());
				return this;
			}
			for (Entry<Integer, List<PolatisPair>> switchEntry : polatisPairbySwitchId.entrySet()) {
				for (Entry<Integer, List<PolatisPair>> dbEntry : dbPolatispairBySwitchId.entrySet()) {
					if (switchEntry.getKey() == dbEntry.getKey()) {// same switch
						List<PolatisPair> onlySwitchPolatisPair = new ArrayList<PolatisPair>();
						onlySwitchPolatisPair.addAll(switchEntry.getValue());
						onlySwitchPolatisPair.removeAll(dbEntry.getValue()); // onlySwitchPolatisPair has oxs that are
																				// in switch but not in DB
						// add oxcs of onlySwitchPolatisPair to trail.name = default-cli
						logger.info("onlySwitchPolatisPair = " + onlySwitchPolatisPair);
						if (onlySwitchPolatisPair.size() > 0) {
							List<Future> allOXCsAdded = new ArrayList<Future>();
							for (PolatisPair polatisPair : onlySwitchPolatisPair) {
								Promise<Void> pOXCAdded = Promise.promise();
								allOXCsAdded.add(pOXCAdded.future());
								VcrossConnect vcrossConnect = new VcrossConnect();
								vcrossConnect.setName("default-cli" + "");
								vcrossConnect.setLabel("");
								vcrossConnect.setDescription("");
								vcrossConnect.setTrailId(default_cliId);
								vcrossConnect.setSwitchId(switchEntry.getKey());
								List<Integer> portsId = portIdbySwitchId.get(switchEntry.getKey());
								for (Integer key : portNbbyportId.keySet()) {
									if (portsId.contains(key) && portNbbyportId.get(key)
											.equals(String.valueOf(polatisPair.getIngress()))) {
										vcrossConnect.setIngressPortId(key);
									}
									if (portsId.contains(key) && portNbbyportId.get(key)
											.equals(String.valueOf((polatisPair.getEgress() - 8)))) {
										vcrossConnect.setEgressPortId(key);
									}
								}
								vcrossConnect.setStatus(StatusEnum.UP);

								service.addVcrossConnect(vcrossConnect, ar -> {
									if (ar.succeeded()) {
										pOXCAdded.complete();
										logger.info("VcrossConnect added succesfully");
									} else {
										logger.info("fail to add Vcrossconnects to topology service" + ar.cause());
										pOXCAdded.fail(ar.cause());
									}
								});
							}
							CompositeFuture.all(allOXCsAdded).map((Void) null).onComplete(res -> {
								if (res.succeeded()) {
									logger.info("onlySwitchPolatisPair in switchid=" + switchEntry.getKey()
											+ " added succesfully to DB");
								} else {
									logger.info("fail to add cli vcrossconnects to DB" + res.cause());

								}
							});
						}
						List<PolatisPair> onlyDBPolatisPair = new ArrayList<PolatisPair>();
						onlyDBPolatisPair.addAll(dbEntry.getValue());
						onlyDBPolatisPair.removeAll(switchEntry.getValue()); // onlyDBPolatisPair has oxs that are in DB
																				// but not in switch

						if (onlyDBPolatisPair.size() > 0) {
							List<Future> allTrailsUP = new ArrayList<Future>();
							for (PolatisPair polatisPair : onlyDBPolatisPair) {
								for (Entry<Integer, List<VcrossConnect>> entry : dbOXCsbytrailId.entrySet()) {
									Promise<Void> pTrailRestored = Promise.promise();
									allTrailsUP.add(pTrailRestored.future());
									CompletableFuture<Void> stage = CompletableFuture.completedFuture(null);
									for (VcrossConnect vcrossConnect : entry.getValue()) {
										int egressportnb = Integer
												.valueOf(portNbbyportId.get(vcrossConnect.getEgressPortId())) + 8;
										// oxcs in default-cli --> delete from trail default-cli
										if (entry.getKey() == default_cliId) {
											if ((vcrossConnect.getSwitchId() == switchEntry.getKey())
													&& (polatisPair.getIngress() == Integer.valueOf(
															portNbbyportId.get(vcrossConnect.getIngressPortId())))
													&& polatisPair.getEgress() == (Integer.valueOf(
															portNbbyportId.get(vcrossConnect.getEgressPortId())) + 8)) {// trail.name=
																														// default-cli
																														// delete
																														// oxc
																														// from
																														// DB
												service.deleteVcrossConnect(String.valueOf(vcrossConnect.getId()),
														res -> {
															if (res.succeeded()) {
																logger.info(
																		"vcrossconnect deleted successfully from default-cli");
															} else {
																logger.info(
																		"error in deleting vcrossconnect from default-cli");
															}
														});
											}
										} else {
											// oxcs in default-mvs --> delete from trail default-mvs
											if (entry.getKey() == default_mvsId) {
												if ((vcrossConnect.getSwitchId() == switchEntry.getKey())
														&& (polatisPair.getIngress() == Integer.valueOf(
																portNbbyportId.get(vcrossConnect.getIngressPortId())))
														&& polatisPair.getEgress() == (Integer.valueOf(
																portNbbyportId.get(vcrossConnect.getEgressPortId()))
																+ 8)) {// trail.name=
																		// default-cli
																		// delete
																		// oxc
																		// from
																		// DB
													service.deleteVcrossConnect(String.valueOf(vcrossConnect.getId()),
															res -> {
																if (res.succeeded()) {
																	logger.info(
																			"vcrossconnect deleted successfully from default-mvs");
																} else {
																	logger.info(
																			"error in deleting vcrossconnect from default-mvs");
																}
															});
												}
											}
											// oxcs of trail in DB not in switch --> delete the trail
											else {
												logger.info("delete trail");
												deletePath(String.valueOf(entry.getKey()), ignore -> {
												});
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
		resultHandler.handle(Future.succeededFuture());
		return this;
	}

	private void updateStatus(Integer resId, Status.ResTypeEnum resTypeEnum, Status.StatusEnum resStatus) {
		logger.info("updateStatus in QconnectionService");
		Status status = new Status();
		status.setResId(resId);
		status.setResType(resTypeEnum);
		status.setStatus(resStatus);
		status.setTimestamp(OffsetDateTime.now());
		status.setId(String.valueOf(status.hashCode()));
		vertx.eventBus().publish(NotificationService.STATUS_ADDRESS, status.toJson());
	}

	@Override
	public QconnectionService createPath(Trail trail, String finish, Handler<AsyncResult<Integer>> resultHandler) {
		// do necessary verifications -- param of trail and finish
		logger.info("createPath in QconnectionService");
		for (CrossConnect crossConnect : trail.getOxcs()) {
			for (Entry<Integer, List<VcrossConnect>> entry : dbOXCsbytrailId.entrySet()) {
				for (VcrossConnect vcrossConnect : entry.getValue()) {
					logger.info("vcrossConnect.getIngressPortId()= " + vcrossConnect.getIngressPortId());
					logger.info("vcrossConnect.getEgressPortId()= " + vcrossConnect.getEgressPortId());
					if (crossConnect.getSwitchId() == vcrossConnect.getSwitchId()
							&& (crossConnect.getIngressPortId() == vcrossConnect.getIngressPortId()
									|| crossConnect.getEgressPortId() == vcrossConnect.getEgressPortId())) {
						resultHandler.handle(Future.failedFuture("CONFLICT oxc already exists"));
						return this;
					}
				}
			}
		}

		crossConnectsCreated.clear();
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

				resultHandler.handle(Future.succeededFuture(trailId)); // create Vtrail
																		// return id of vtrail in result handler
				logger.info("Trail created");
				vtrail.setId(trailId);// create path on switch before creating it in the topology
										// why : avoid the deletion in topology

				Promise<Void> pTrailAddedToSwitch = Promise.promise();
				CompletableFuture<Void> stage = CompletableFuture.completedFuture(null);

				for (CrossConnect crossconnect : trail.getOxcs()) {
					PolatisPair PolatisPair = new PolatisPair();

					PolatisPair.setIngress(Integer.parseInt(portNbbyportId.get(crossconnect.getIngressPortId())));
					int i = Integer.parseInt(portNbbyportId.get(crossconnect.getEgressPortId())) + 8;
					PolatisPair.setEgress(i);

					stage = stage
							.thenCompose(r -> createOXC(mgmtIpbyswitchId.get(crossconnect.getSwitchId()), PolatisPair));
				}

				stage.whenComplete((result, error) -> {
					if (error != null) {
						logger.info("fail to create crossconnects on switches " + error.getCause());
						pTrailAddedToSwitch.fail(error.getCause());
						rollbackTrail(trailId, res -> {
							updateStatus(trailId, ResTypeEnum.TRAIL, StatusEnum.ERROR);
						});
					} else { // if the oxcs were created on the switch, we create them in the topology
						pTrailAddedToSwitch.complete();

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
							vcrossConnect.setStatus(StatusEnum.UP);

							service.addVcrossConnect(vcrossConnect, ar2 -> {
								if (ar2.succeeded()) {
									logger.info("Vcrossconnects successfully added to DB" + sn.cause());
									pOXCAdded.complete();
								} else {
									logger.info("fail to add Vcrossconnects to DB" + sn.cause());
									pOXCAdded.fail(ar2.cause());
								}
							});
						});
						CompositeFuture.all(allOXCsAdded).map((Void) null).onComplete(res -> {
							if (res.succeeded()) {
								pTrailAddedToTopology.complete();
								updateStatus(trailId, ResTypeEnum.TRAIL, StatusEnum.UP);
							} else {
								logger.info("fail to add Vtrail to DB" + res.cause());
								pTrailAddedToTopology.fail(res.cause());
							}
						});
					}
				});
			} else {
				logger.info("fail to create Vtrail in DB" + sn.cause());
				resultHandler.handle(Future.failedFuture(sn.cause()));
			}
		});
		return this;
	}

	private QconnectionService getOXC(String mgmtIp, String ingress, Handler<AsyncResult<Void>> resultHandler) {
		logger.info("getOXC in QconnectionService");
		webClient.get(mgmtIp, "/api/data/optical-switch:cross-connects/pair=" + ingress)
				.putHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString("admin:root".getBytes()))
				.send(response -> {
					if (response.succeeded()) {
						HttpResponse<Buffer> httpResponse = response.result();
						logger.info("GET Response With Filter Response : " + httpResponse.bodyAsString());
						if (httpResponse.statusCode() == 201) {
							resultHandler.handle(Future.succeededFuture());
						} else {
							resultHandler.handle(Future.failedFuture("CONFLICT"));
						}
					} else {
						logger.info("the OXC does not exist : " + response.cause().getMessage());
						resultHandler.handle(Future.failedFuture("ERROR in get method"));
					}
				});
		return this;

	}

	private QconnectionService getallOXC(String mgmtIp, Handler<AsyncResult<List<PolatisPair>>> resultHandler) {
		logger.info("getallOXC in QconnectionService");
		webClient.get(mgmtIp, "/api/data/optical-switch:cross-connects/pair")
				.putHeader("Content-type", "application/yang-data+json")
				.putHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString("admin:root".getBytes()))
				.send(response -> {
					if (response.succeeded()) {
						HttpResponse<Buffer> httpResponse = response.result();
						// logger.info("GET ALL Response : " + httpResponse.bodyAsString());
						if (httpResponse.statusCode() == 200) {
							List<PolatisPair> crossConnects = JsonUtils.json2Pojo(
									httpResponse.bodyAsJsonObject().getValue("optical-switch:pair").toString(),
									new TypeReference<List<PolatisPair>>() {
									});
							// logger.info("switch getAllOxcs: " + crossConnects);
							resultHandler.handle(Future.succeededFuture(crossConnects));
						} else {
							resultHandler.handle(Future.failedFuture("CONFLICT"));
						}
					} else {
						logger.info("getallOXC error : " + response.cause().getMessage());
						resultHandler.handle(Future.failedFuture("ERROR in get method"));
					}
				});
		return this;
	}

	public void set_label(String mgmtIp, int ingress, String label) {
		webClient.patch(mgmtIp, "/api/data/optical-switch:port-config/port=" + ingress)
				.putHeader("Content-type", "application/yang-data+json")
				.putHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString("admin:root".getBytes()))
				.sendJson(new JsonObject("{\"port\": {\"label\":\"" + label + "\"}}"), response -> {
					if (response.succeeded()) {
						HttpResponse<Buffer> httpResponse = response.result();
						System.out.println("Set label: " + httpResponse.bodyAsString());
					} else {
						System.out.println("ERROR : " + response.cause().getMessage());
					}
				});
	}

	public void get_label(String mgmtIp, int ingress) {
		webClient.get(mgmtIp, "/api/data/optical-switch:port-config/port=" + ingress + "?fields=label")
				.putHeader("Content-type", "application/yang-data+json")
				.putHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString("admin:root".getBytes()))
				.send(response -> {
					if (response.succeeded()) {
						HttpResponse<Buffer> httpResponse = response.result();
						System.out.println("GET label: " + httpResponse.bodyAsString());
					} else {
						System.out.println("ERROR : " + response.cause().getMessage());
					}
				});
	}

	private CompletableFuture<Void> createOXC(String mgmtIp, PolatisPair PolatisPair) {
		logger.info("createOXC in QconnectionServiceImpl");
		CompletableFuture<Void> cs = new CompletableFuture<>();
		PolatisCrossConnect crossconnects = new PolatisCrossConnect(PolatisPair);

		webClient.post(mgmtIp, "/api/data/optical-switch:cross-connects")
				.putHeader("Content-type", "application/yang-data+json")
				.putHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString("admin:root".getBytes()))
				.sendJson(crossconnects, response -> {
					if (response.succeeded()) {
						HttpResponse<Buffer> httpResponse = response.result();
						logger.info("Post Response : " + httpResponse.statusMessage());
						if (httpResponse.statusCode() == 201) {
							cs.complete(null);
							crossConnectsCreated.put(mgmtIp, PolatisPair.getIngress());// add to list of created
							// add label to ingress port
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
		stage.whenComplete((result, error) -> {
			if (error != null) {
				logger.info("ERROR in delete in switch: " + error.getCause());
				resultHandler.handle(Future.failedFuture(error.getCause()));
			} else {
				logger.info("ROLLBACK succeed ");
				resultHandler.handle(Future.succeededFuture());
			}
		});
		return this;
	}

	public QconnectionService deletePath(String trailId, Handler<AsyncResult<Void>> resultHandler) {
		logger.info("deletePath in QconnectionService");
		if (Integer.valueOf(trailId) == default_cliId) {
			resultHandler.handle(Future.failedFuture("CONFLICT Path created with CLI"));
			return this;
		}

		ServiceProxyBuilder builder = new ServiceProxyBuilder(vertx).setAddress(TopologyService.SERVICE_ADDRESS);
		TopologyService service = builder.build(TopologyService.class);

		service.getVcrossConnectsByTrail(trailId, ar -> {
			if (ar.succeeded()) {
				CompletableFuture<Void> stage = CompletableFuture.completedFuture(null);

				for (VcrossConnect vcrossConnect : ar.result()) {
					stage = stage.thenCompose(r -> deleteOXC(mgmtIpbyswitchId.get(vcrossConnect.getSwitchId()),
							Integer.parseInt(portNbbyportId.get(vcrossConnect.getIngressPortId()))));
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
						if (httpResponse.statusCode() == 204 || httpResponse.statusCode() == 404)
							cs.complete(null);
						// delete ingress port label
						else
							cs.completeExceptionally(new RuntimeException("Conflict error!"));
					} else {
						logger.info("ERROR in delete in switch : " + response.cause().getMessage());
						cs.completeExceptionally(response.cause());
					}
				});
		return cs;
	}

	@Override
	public QconnectionService createCrossConnect(CrossConnect crossConnect,
			Handler<AsyncResult<Integer>> resultHandler) {
		logger.info("createCrossConnect in QconnectionService");
		ServiceProxyBuilder builder = new ServiceProxyBuilder(vertx).setAddress(TopologyService.SERVICE_ADDRESS);
		TopologyService service = builder.build(TopologyService.class);
		PolatisPair PolatisPair = new PolatisPair();

		PolatisPair.setIngress(Integer.parseInt(portNbbyportId.get(crossConnect.getIngressPortId())));
		int i = Integer.parseInt(portNbbyportId.get(crossConnect.getEgressPortId())) + 8;
		PolatisPair.setEgress(i);
		CompletableFuture<Void> stage = CompletableFuture.completedFuture(null);
		stage = stage.thenCompose(r -> createOXC(mgmtIpbyswitchId.get(crossConnect.getSwitchId()), PolatisPair));
		stage.whenComplete((result, error) -> {
			if (error != null) {
				logger.info("fail to create crossconnect on switche " + error.getCause());
				resultHandler.handle(Future.failedFuture(error.getCause()));
			} else {
				VcrossConnect vcrossConnect = new VcrossConnect();
				vcrossConnect.setName("default_mvs" + "");
				vcrossConnect.setLabel("");
				vcrossConnect.setDescription("");
				vcrossConnect.setTrailId(default_mvsId);
				vcrossConnect.setSwitchId(crossConnect.getSwitchId());
				vcrossConnect.setIngressPortId(crossConnect.getIngressPortId());
				vcrossConnect.setEgressPortId(crossConnect.getEgressPortId());
				vcrossConnect.setStatus(StatusEnum.UP);
				service.addVcrossConnect(vcrossConnect, ar -> {
					if (ar.succeeded()) {
						int oxcId = ar.result();
						logger.info("Vcrossconnect successfully added to default-mvs in DB");
						resultHandler.handle(Future.succeededFuture(oxcId));
					} else {
						logger.info("fail to add Vcrossconnect to default-mvs DB" + ar.cause());
						deleteOXC(mgmtIpbyswitchId.get(crossConnect.getSwitchId()), crossConnect.getEgressPortId());
						resultHandler.handle(Future.failedFuture(ar.cause()));
					}
				});
			}
		});
		return this;
	}

	@Override
	public QconnectionService deleteCrossConnect(String oxcId, Handler<AsyncResult<Void>> resultHandler) {
		ServiceProxyBuilder builder = new ServiceProxyBuilder(vertx).setAddress(TopologyService.SERVICE_ADDRESS);
		TopologyService service = builder.build(TopologyService.class);

		service.getVcrossConnectById(oxcId, ar -> {
			if (ar.succeeded()) {
				VcrossConnect vcrossConnect = ar.result();
				CompletableFuture<Void> stage = CompletableFuture.completedFuture(null);
				stage = stage.thenCompose(r -> deleteOXC(mgmtIpbyswitchId.get(vcrossConnect.getSwitchId()),
						Integer.parseInt(portNbbyportId.get(vcrossConnect.getIngressPortId()))));

				stage.whenComplete((result, error) -> {
					if (error != null) {
						logger.info("ERROR in delete oxc in switch: " + error.getCause());
					} else {
						service.deleteVtrail(oxcId, ar3 -> {
							if (ar3.succeeded()) {
								resultHandler.handle(Future.succeededFuture());
								logger.info("vcrossConnect deleted successfully");
							} else {
								logger.info("ERROR in delete vcrossConnect in topology service: "
										+ ar3.cause().getMessage());
							}
						});
					}
				});

			} else {
				logger.info("ERROR in get VcrossConnectById : " + ar.cause().getMessage());
				resultHandler.handle(Future.failedFuture(ar.cause()));
			}
		});
		return this;
	}

	// TODO function that gets events from switch using notifications from netconf
	// create a sort of a wrapper to use vertx with netconf java code
	public QconnectionService doHealthCheck(Handler<AsyncResult<Void>> resultHandler) {
		resultHandler.handle(Future.succeededFuture());
		// TODO call switches...
		// list of devices
		// function thats sends periodic request to check the availability of each
		// device
		// if no response retry n = 3 times with a waiting time of 5-10s
		// if no response at the nth time --> device is down
		return this;
	}
}