package io.nms.central.microservice.ipnet.impl;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

import io.nms.central.microservice.common.functional.JsonUtils;
import io.nms.central.microservice.digitaltwin.DigitalTwinService;
import io.nms.central.microservice.digitaltwin.model.dt.VerificationReport;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.Bgp;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.Configurable;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.Device;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.Link;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.NetInterface;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.Network;
import io.nms.central.microservice.ipnet.IpnetService;
import io.nms.central.microservice.ipnet.model.ApplyConfigResult;
import io.nms.central.microservice.ipnet.model.ConfigChange;
import io.nms.central.microservice.ipnet.model.ConfigChange.ActionEnum;
import io.nms.central.microservice.ipnet.model.ConfigChange.ResourceTypeEnum;
import io.nms.central.microservice.ipnet.model.ConfigProfile;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.mongo.UpdateOptions;
import io.vertx.serviceproxy.ServiceProxyBuilder;

/**
 * This verticle implements the ipnet service
 */
public class IpnetServiceImpl implements IpnetService {

	private static final Logger logger = LoggerFactory.getLogger(IpnetServiceImpl.class);

	private static final String COLL_CONFIG_PROFILE = "config-profile";
	private static final String COLL_CONFIG_CHANGE = "config-change";
	private static final String COLL_RESOURCE_BKP = "resource-bkp";
	private static final String COLL_INTENDED_CONFIG = "intened-config";
	private static final String COLL_RUNNING_CONFIG = "running-config";
	
	private final MongoClient client;
	private final Vertx vertx;
	private final String serviceApi;

	public IpnetServiceImpl(Vertx vertx, JsonObject config) {
		this.vertx = vertx;
		this.client = MongoClient.create(vertx, config);
		this.serviceApi = config.getString("api.name");
	}
	
	@Override
	public void initializePersistence(Handler<AsyncResult<List<Integer>>> resultHandler) {
		resultHandler.handle(Future.succeededFuture());
	}
	
	/* API: Read network running state */
	@Override
	public void runningVerify(Handler<AsyncResult<VerificationReport>> resultHandler) {
		digitalTwinSvcProxy().runningVerifyNetwork(resultHandler);
	}
	@Override
	public void runningGetNetwork(Handler<AsyncResult<Network>> resultHandler) {
		digitalTwinSvcProxy().runningGetNetwork(resultHandler);
	}
	@Override
	public void runningGetDeviceInterfaces(String deviceName, 
			Handler<AsyncResult<List<NetInterface>>> resultHandler) {
		digitalTwinSvcProxy().runningGetDeviceInterfaces(deviceName, resultHandler);
	}
	@Override
	public void runningGetDeviceBgps(String deviceName, 
			Handler<AsyncResult<List<Bgp>>> resultHandler) {
		digitalTwinSvcProxy().runningGetDeviceBgps(deviceName, resultHandler);
	}

	/* View API: Network */
	@Override
	public void configGetNetwork(String viewId, 
			Handler<AsyncResult<Network>> resultHandler) {
		digitalTwinSvcProxy().viewGetNetwork(viewId, resultHandler);
	}

	/* View API: Device */
	@Override
	public void configCreateDevice(String viewId, String deviceName, Device device, 
			Handler<AsyncResult<Void>> resultHandler) {
		getOrCreateConfigProfile(viewId, cp -> {
			if (cp.succeeded()) {
				ConfigChange cc = new ConfigChange();
				cc.setType(ResourceTypeEnum.DEVICE);
				cc.setAction(ActionEnum.CREATE);
				cc.setLocation(getConfigDeviceURL(deviceName));

				digitalTwinSvcProxy().viewCreateDevice(viewId, deviceName, device, 
						created -> {
					if (created.succeeded()) {
						cc.setDatetime(OffsetDateTime.now().toLocalDateTime().toString());
						saveConfigChange(viewId, cc, done -> {
							if (done.succeeded()) {
								resultHandler.handle(Future.succeededFuture());
							} else {
								resultHandler.handle(Future.failedFuture(done.cause()));
								// undo creation
								digitalTwinSvcProxy()
										.viewDeleteDevice(viewId, deviceName, 
										ignore -> {});
							}
						});
					} else {
						resultHandler.handle(Future.failedFuture(created.cause()));
					}
				});
			} else {
				resultHandler.handle(Future.failedFuture(cp.cause()));
			}
		});
	}
	@Override
	public void configUpdateDevice(String viewId, String deviceName, Device device, 
			Handler<AsyncResult<Void>> resultHandler) {
		getOrCreateConfigProfile(viewId, cp -> {
			if (cp.succeeded()) {
				ConfigChange cc = new ConfigChange();
				cc.setType(ResourceTypeEnum.DEVICE);
				cc.setAction(ActionEnum.UPDATE);
				cc.setLocation(getConfigDeviceURL(deviceName));

				digitalTwinSvcProxy().viewGetDevice(viewId, deviceName, res -> {
					if (res.succeeded()) {
						Device deviceBkp = res.result();
						saveResourceBkp(viewId, cc.getId(), deviceBkp, saved -> {
							if (saved.succeeded()) {
								digitalTwinSvcProxy()
										.viewCreateDevice(viewId, deviceName, device, updated -> {
									if (updated.succeeded()) {
										cc.setDatetime(OffsetDateTime.now().toLocalDateTime().toString());
										saveConfigChange(viewId, cc, done -> {
											if (done.succeeded()) {
												resultHandler.handle(Future.succeededFuture());
											} else {
												resultHandler.handle(Future.failedFuture(done.cause()));
												// undo update, delete bkp
												digitalTwinSvcProxy()
														.viewCreateDevice(viewId, deviceName, deviceBkp, 
														undo -> {
													deleteResourceBkp(cc.getId(), ignore -> {});
												});
											}
										});
									} else {
										resultHandler.handle(Future.failedFuture(updated.cause()));
										// delete bkp
										deleteResourceBkp(cc.getId(), ignore -> {});
									}
								});
							} else {
								resultHandler.handle(Future.failedFuture(saved.cause()));
							}
						});
					} else {
						resultHandler.handle(Future.failedFuture(res.cause()));
					}
				});
			} else {
				resultHandler.handle(Future.failedFuture(cp.cause()));
			}
		});
	}
	@Override
	public void configDeleteDevice(String viewId, String deviceName, 
			Handler<AsyncResult<Void>> resultHandler) {
		getOrCreateConfigProfile(viewId, cp -> {
			if (cp.succeeded()) {
				ConfigChange cc = new ConfigChange();
				cc.setType(ResourceTypeEnum.DEVICE);
				cc.setAction(ActionEnum.DELETE);
				cc.setLocation(getConfigDeviceURL(deviceName));

				digitalTwinSvcProxy().viewGetDevice(viewId, deviceName, res -> {
					if (res.succeeded()) {
						Device deviceBkp = res.result();
						// TODO: backup links
						saveResourceBkp(viewId, cc.getId(), deviceBkp, saved -> {
							if (saved.succeeded()) {
								digitalTwinSvcProxy()
										.viewDeleteDevice(viewId, deviceName, deleted -> {
									if (deleted.succeeded()) {
										cc.setDatetime(OffsetDateTime.now().toLocalDateTime().toString());
										saveConfigChange(viewId, cc, done -> {
											if (done.succeeded()) {
												resultHandler.handle(Future.succeededFuture());
											} else {
												resultHandler.handle(Future.failedFuture(done.cause()));
												// undo update, delete bkp
												digitalTwinSvcProxy()
														.viewCreateDevice(viewId, deviceName, deviceBkp,
														undo -> {
													deleteResourceBkp(cc.getId(), ignore -> {});
												});
											}
										});
									} else {
										resultHandler.handle(Future.failedFuture(deleted.cause()));
										// delete bkp
										deleteResourceBkp(cc.getId(), ignore -> {});
									}
								});
							} else {
								resultHandler.handle(Future.failedFuture(saved.cause()));
							}
						});
					} else {
						resultHandler.handle(Future.failedFuture(res.cause()));
					}
				});
			} else {
				resultHandler.handle(Future.failedFuture(cp.cause()));
			}
		});
	}
	
	/* View API: NetInterface */
	@Override
	public void configGetDeviceInterfaces(String viewId, String deviceName, 
			Handler<AsyncResult<List<NetInterface>>> resultHandler) {
		digitalTwinSvcProxy().viewGetDeviceInterfaces(viewId, deviceName, resultHandler);
	}
	@Override
	public void configCreateInterface(String viewId, String deviceName, String itfName, 
			NetInterface netItf, Handler<AsyncResult<Void>> resultHandler) {
		getOrCreateConfigProfile(viewId, cp -> {
			if (cp.succeeded()) {
				ConfigChange cc = new ConfigChange();
				cc.setType(ResourceTypeEnum.INTERFACE);
				cc.setAction(ActionEnum.CREATE);
				cc.setLocation(getConfigInterfaceURL(deviceName, itfName));

				digitalTwinSvcProxy().viewCreateInterface(viewId, deviceName, itfName, netItf, 
						created -> {
					if (created.succeeded()) {
						cc.setDatetime(OffsetDateTime.now().toLocalDateTime().toString());
						saveConfigChange(viewId, cc, done -> {
							if (done.succeeded()) {
								resultHandler.handle(Future.succeededFuture());
							} else {
								resultHandler.handle(Future.failedFuture(done.cause()));
								// undo creation
								digitalTwinSvcProxy()
										.viewDeleteInterface(viewId, deviceName, itfName,
										ignore -> {});
							}
						});
					} else {
						resultHandler.handle(Future.failedFuture(created.cause()));
					}
				});
			} else {
				resultHandler.handle(Future.failedFuture(cp.cause()));
			}
		});
	}
	@Override
	public void configUpdateInterface(String viewId, String deviceName, String itfName, 
			NetInterface netItf, Handler<AsyncResult<Void>> resultHandler) {
		getOrCreateConfigProfile(viewId, cp -> {
			if (cp.succeeded()) {
				ConfigChange cc = new ConfigChange();
				cc.setType(ResourceTypeEnum.INTERFACE);
				cc.setAction(ActionEnum.UPDATE);
				cc.setLocation(getConfigInterfaceURL(deviceName, itfName));

				digitalTwinSvcProxy().viewGetInterface(viewId, deviceName, itfName, res -> {
					if (res.succeeded()) {
						NetInterface netItfBkp = res.result();
						saveResourceBkp(viewId, cc.getId(), netItfBkp, saved -> {
							if (saved.succeeded()) {
								digitalTwinSvcProxy()
										.viewCreateInterface(viewId, deviceName, itfName, netItf, updated -> {
									if (updated.succeeded()) {
										cc.setDatetime(OffsetDateTime.now().toLocalDateTime().toString());
										saveConfigChange(viewId, cc, done -> {
											if (done.succeeded()) {
												resultHandler.handle(Future.succeededFuture());
											} else {
												resultHandler.handle(Future.failedFuture(done.cause()));
												// undo update, delete bkp
												digitalTwinSvcProxy()
														.viewCreateInterface(viewId, deviceName, itfName, netItfBkp, 
														undo -> {
													deleteResourceBkp(cc.getId(), ignore -> {});
												});
											}
										});
									} else {
										resultHandler.handle(Future.failedFuture(updated.cause()));
										// delete bkp
										deleteResourceBkp(cc.getId(), ignore -> {});
									}
								});
							} else {
								resultHandler.handle(Future.failedFuture(saved.cause()));
							}
						});
					} else {
						resultHandler.handle(Future.failedFuture(res.cause()));
					}
				});
			} else {
				resultHandler.handle(Future.failedFuture(cp.cause()));
			}
		});
	}
	@Override
	public void configDeleteInterface(String viewId, String deviceName, String itfName, 
			Handler<AsyncResult<Void>> resultHandler) {
		getOrCreateConfigProfile(viewId, cp -> {
			if (cp.succeeded()) {
				ConfigChange cc = new ConfigChange();
				cc.setType(ResourceTypeEnum.INTERFACE);
				cc.setAction(ActionEnum.DELETE);
				cc.setLocation(getConfigInterfaceURL(deviceName, itfName));

				digitalTwinSvcProxy().viewGetInterface(viewId, deviceName, itfName, res -> {
					if (res.succeeded()) {
						NetInterface netItfBkp = res.result();
						// TODO: backup bgp
						saveResourceBkp(viewId, cc.getId(), netItfBkp, saved -> {
							if (saved.succeeded()) {
								digitalTwinSvcProxy()
										.viewDeleteInterface(viewId, deviceName, itfName, deleted -> {
									if (deleted.succeeded()) {
										cc.setDatetime(OffsetDateTime.now().toLocalDateTime().toString());
										saveConfigChange(viewId, cc, done -> {
											if (done.succeeded()) {
												resultHandler.handle(Future.succeededFuture());
											} else {
												resultHandler.handle(Future.failedFuture(done.cause()));
												// undo update, delete bkp
												digitalTwinSvcProxy()
														.viewCreateInterface(viewId, deviceName, itfName, netItfBkp,
														undo -> {
													deleteResourceBkp(cc.getId(), ignore -> {});
												});
											}
										});
									} else {
										resultHandler.handle(Future.failedFuture(deleted.cause()));
										// delete bkp
										deleteResourceBkp(cc.getId(), ignore -> {});
									}
								});
							} else {
								resultHandler.handle(Future.failedFuture(saved.cause()));
							}
						});
					} else {
						resultHandler.handle(Future.failedFuture(res.cause()));
					}
				});
			} else {
				resultHandler.handle(Future.failedFuture(cp.cause()));
			}
		}); 
	}
	
	/* View API: Link */
	@Override
	public void configCreateLink(String viewId, String linkName, Link link, 
			Handler<AsyncResult<Void>> resultHandler) {
		/* getOrCreateConfigProfile(viewId, cp -> {
			if (cp.succeeded()) {
				ConfigChange cc = new ConfigChange();
				cc.setType(ResourceTypeEnum.LINK);
				cc.setAction(ActionEnum.CREATE);
				cc.setLocation(getConfigLinkURL(deviceName, itfName));

				digitalTwinSvcProxy().viewCreateInterface(viewId, deviceName, itfName, netItf, 
						created -> {
					if (created.succeeded()) {
						cc.setDatetime(OffsetDateTime.now().toLocalDateTime().toString());
						saveConfigChange(viewId, cc, done -> {
							if (done.succeeded()) {
								resultHandler.handle(Future.succeededFuture());
							} else {
								resultHandler.handle(Future.failedFuture(done.cause()));
								// undo creation
								digitalTwinSvcProxy()
										.viewDeleteInterface(viewId, deviceName, itfName,
										ignore -> {});
							}
						});
					} else {
						resultHandler.handle(Future.failedFuture(created.cause()));
					}
				});
			} else {
				resultHandler.handle(Future.failedFuture(cp.cause()));
			}
		}); */
		resultHandler.handle(Future.failedFuture("not implemented"));
	}
	@Override
	public void configDeleteLink(String viewId, String linkName, 
			Handler<AsyncResult<Void>> resultHandler) {
		// ...
	}

	/* View API: BGP peer */
	@Override
	public void configGetDeviceBgps(String viewId, String deviceName, 
			Handler<AsyncResult<List<Bgp>>> resultHandler) {
		digitalTwinSvcProxy().viewGetDeviceBgps(viewId, deviceName, resultHandler);
	}
	@Override
	public void configCreateBgp(String viewId, String deviceName, String itfAddr, Bgp bgp, 
			Handler<AsyncResult<Void>> resultHandler) {
		getOrCreateConfigProfile(viewId, cp -> {
			if (cp.succeeded()) {
				ConfigChange cc = new ConfigChange();
				cc.setType(ResourceTypeEnum.BGP);
				cc.setAction(ActionEnum.CREATE);
				cc.setLocation(getConfigBgpURL(deviceName, itfAddr));

				digitalTwinSvcProxy().viewUpdateBgp(viewId, deviceName, itfAddr, bgp, 
						created -> {
					if (created.succeeded()) {
						cc.setDatetime(OffsetDateTime.now().toLocalDateTime().toString());
						saveConfigChange(viewId, cc, done -> {
							if (done.succeeded()) {
								resultHandler.handle(Future.succeededFuture());
							} else {
								resultHandler.handle(Future.failedFuture(done.cause()));
								// undo creation
								digitalTwinSvcProxy()
										.viewDeleteBgp(viewId, deviceName, itfAddr, 
										ignore -> {});
							}
						});
					} else {
						resultHandler.handle(Future.failedFuture(created.cause()));
					}
				});
			} else {
				resultHandler.handle(Future.failedFuture(cp.cause()));
			}
		}); 
	}
	@Override
	public void configUpdateBgp(String viewId, String deviceName, String itfAddr, Bgp bgp, 
			Handler<AsyncResult<Void>> resultHandler) {
		getOrCreateConfigProfile(viewId, cp -> {
			if (cp.succeeded()) {
				ConfigChange cc = new ConfigChange();
				cc.setType(ResourceTypeEnum.BGP);
				cc.setAction(ActionEnum.UPDATE);
				cc.setLocation(getConfigBgpURL(deviceName, itfAddr));

				digitalTwinSvcProxy().viewGetBgp(viewId, deviceName, itfAddr, res -> {
					if (res.succeeded()) {
						Bgp bgpBkp = res.result();
						saveResourceBkp(viewId, cc.getId(), bgpBkp, saved -> {
							if (saved.succeeded()) {
								digitalTwinSvcProxy()
										.viewUpdateBgp(viewId, deviceName, itfAddr, bgp, updated -> {
									if (updated.succeeded()) {
										cc.setDatetime(OffsetDateTime.now().toLocalDateTime().toString());
										saveConfigChange(viewId, cc, done -> {
											if (done.succeeded()) {
												resultHandler.handle(Future.succeededFuture());
											} else {
												resultHandler.handle(Future.failedFuture(done.cause()));
												// undo update, delete bkp
												digitalTwinSvcProxy()
														.viewUpdateBgp(viewId, deviceName, itfAddr, bgpBkp, 
														undo -> {
													deleteResourceBkp(cc.getId(), ignore -> {});
												});
											}
										});
									} else {
										resultHandler.handle(Future.failedFuture(updated.cause()));
										// delete bkp
										deleteResourceBkp(cc.getId(), ignore -> {});
									}
								});
							} else {
								resultHandler.handle(Future.failedFuture(saved.cause()));
							}
						});
					} else {
						resultHandler.handle(Future.failedFuture(res.cause()));
					}
				});
			} else {
				resultHandler.handle(Future.failedFuture(cp.cause()));
			}
		}); 
	}
	@Override
	public void configDeleteBgp(String viewId, String deviceName, String itfAddr, 
			Handler<AsyncResult<Void>> resultHandler) {
		getOrCreateConfigProfile(viewId, cp -> {
			if (cp.succeeded()) {
				ConfigChange cc = new ConfigChange();
				cc.setType(ResourceTypeEnum.BGP);
				cc.setAction(ActionEnum.DELETE);
				cc.setLocation(getConfigBgpURL(deviceName, itfAddr));

				digitalTwinSvcProxy().viewGetBgp(viewId, deviceName, itfAddr, res -> {
					if (res.succeeded()) {
						Bgp bgpBkp = res.result();
						saveResourceBkp(viewId, cc.getId(), bgpBkp, saved -> {
							if (saved.succeeded()) {
								digitalTwinSvcProxy()
										.viewDeleteBgp(viewId, deviceName, itfAddr, deleted -> {
									if (deleted.succeeded()) {
										cc.setDatetime(OffsetDateTime.now().toLocalDateTime().toString());
										saveConfigChange(viewId, cc, done -> {
											if (done.succeeded()) {
												resultHandler.handle(Future.succeededFuture());
											} else {
												resultHandler.handle(Future.failedFuture(done.cause()));
												// undo update, delete bkp
												digitalTwinSvcProxy()
														.viewCreateBgp(viewId, deviceName, itfAddr, bgpBkp, 
														undo -> {
													deleteResourceBkp(cc.getId(), ignore -> {});
												});
											}
										});
									} else {
										resultHandler.handle(Future.failedFuture(deleted.cause()));
										// delete bkp
										deleteResourceBkp(cc.getId(), ignore -> {});
									}
								});
							} else {
								resultHandler.handle(Future.failedFuture(saved.cause()));
							}
						});
					} else {
						resultHandler.handle(Future.failedFuture(res.cause()));
					}
				});
			} else {
				resultHandler.handle(Future.failedFuture(cp.cause()));
			}
		}); 
	}

	/* API: get device config file */
	public void configGetDeviceFile(String viewId, String deviceName, 
			Handler<AsyncResult<JsonObject>> resultHandler) {
		digitalTwinSvcProxy().viewGetDeviceConfig(viewId, deviceName, resultHandler);
	}
	
	/* API: Verify and Apply config */
	@Override
	public void configVerify(String viewId, Handler<AsyncResult<VerificationReport>> resultHandler) {
		findConfigProfile(viewId, res -> {
			if (res.succeeded()) {
				ConfigProfile cp = res.result();
				digitalTwinSvcProxy().viewVerify(viewId, verified -> {
					if (verified.succeeded()) {
						cp.setVerifyReport(verified.result());
						updateConfigProfile(viewId, cp, done -> {
							if (done.succeeded()) {
								resultHandler.handle(Future.succeededFuture(verified.result()));
							} else {
								resultHandler.handle(Future.failedFuture(done.cause()));
							}
						});
					} else {
						resultHandler.handle(Future.failedFuture(verified.cause()));
					}
				});
			} else {
				resultHandler.handle(Future.failedFuture(res.cause()));
			}
		});
	}
	
	@Override
	public void configApply(String viewId, Handler<AsyncResult<ApplyConfigResult>> resultHandler) {
		digitalTwinSvcProxy().viewGetNetworkConfig(viewId, res -> {
			if (res.succeeded()) {
				JsonObject netConfig = res.result();
				saveNetworkConfig(COLL_INTENDED_CONFIG, viewId, netConfig, saved -> {
					if (saved.succeeded()) {
						// WIP: push config to SSH-EMS
						cleanProfile(viewId, done -> {
							if (done.succeeded()) {
								digitalTwinSvcProxy().deleteView(viewId, ignore -> {
									ApplyConfigResult result = new ApplyConfigResult();
									result.setMessage("OK");
									resultHandler.handle(Future.succeededFuture(result));
								});
							} else {
								resultHandler.handle(Future.failedFuture(done.cause()));
							}
						});
					} else {
						resultHandler.handle(Future.failedFuture(saved.cause()));
					}
				});
			} else {
				resultHandler.handle(Future.failedFuture(res.cause()));
			}
		});
	}

	/* API: Config Changes */
	@Override
	public void getAllConfigChanges(String viewId, Handler<AsyncResult<List<ConfigChange>>> resultHandler) {
		JsonObject query = new JsonObject().put("_viewId", viewId);
		client.find(COLL_CONFIG_CHANGE, query, ar -> {
			if (ar.succeeded()) {
				if (ar.result() == null) {
					resultHandler.handle(Future.succeededFuture());
				} else {
					List<ConfigChange> result = ar.result().stream()
							.map(raw -> {
								raw.remove("_id");
								raw.remove("_viewId");
								ConfigChange cc = new ConfigChange(raw);
								return cc;
							}).collect(Collectors.toList());
					resultHandler.handle(Future.succeededFuture(result));
				}
			} else {
				resultHandler.handle(Future.failedFuture(ar.cause()));
			}
		});
	}
	@Override
	public void undoConfigChange(String viewId, Handler<AsyncResult<Void>> resultHandler) {
		Promise<ConfigChange> reverted = Promise.promise();
		findLatestConfigChange(viewId, res -> {
			if (res.succeeded()) {
				ConfigChange cc = res.result();
				if (cc.getType().equals(ResourceTypeEnum.DEVICE)) {
					undoDeviceConfig(viewId, cc, reverted);
				} else if (cc.getType().equals(ResourceTypeEnum.INTERFACE)) {
					undoInterfaceConfig(viewId, cc, reverted);
				} else if (cc.getType().equals(ResourceTypeEnum.BGP)) {
					undoBgpConfig(viewId, cc, reverted);
				} else {
					reverted.fail("Unexpected resource type: "+cc.getType().getValue());
				}
			} else {
				resultHandler.handle(Future.failedFuture(res.cause()));
			}
		});
		reverted.future().onComplete(res -> {
			if (res.succeeded()) {
				deleteConfigChange(res.result().getId(), resultHandler);
			} else {
				resultHandler.handle(Future.failedFuture(res.cause()));
			}
		});
	}
	
	/* API: Intended and Running Network Config */
	public void getIntendedNetworkConfig(String viewId, Handler<AsyncResult<JsonObject>> resultHandler) {
		findConfig(COLL_INTENDED_CONFIG, viewId, null, resultHandler);
	}
	public void getRunningNetworkConfig(String viewId, Handler<AsyncResult<JsonObject>> resultHandler) {
		findConfig(COLL_RUNNING_CONFIG, viewId, null, resultHandler);
	}
	public void updateRunningNetworkConfig(String viewId, JsonObject netConfig, Handler<AsyncResult<Void>> resultHandler) {
		saveNetworkConfig(COLL_RUNNING_CONFIG, viewId, netConfig, resultHandler);
	}
	
	/* API: Intended and Running Device Config */
	public void getIntendedDeviceConfig(String viewId, String deviceName, Handler<AsyncResult<JsonObject>> resultHandler) {
		findConfig(COLL_INTENDED_CONFIG, viewId, deviceName, resultHandler);
	}
	public void getRunningDeviceConfig(String viewId, String deviceName, Handler<AsyncResult<JsonObject>> resultHandler) {
		findConfig(COLL_RUNNING_CONFIG, viewId, deviceName, resultHandler);
	}

	/* Functional: Config Changes */
	private void undoDeviceConfig(String viewId, ConfigChange cc, 
			Handler<AsyncResult<ConfigChange>> resultHandler) {
		findResourceBkp(cc.getId(), Device.class, res -> {
			if (res.succeeded()) {
				Device deviceBkp = res.result();
				if (cc.getAction().equals(ActionEnum.UPDATE)) {
					digitalTwinSvcProxy()
							.viewCreateDevice(viewId, deviceBkp.getName(), deviceBkp, done -> {
						if (done.succeeded()) {
							resultHandler.handle(Future.succeededFuture(cc));
						} else {
							resultHandler.handle(Future.failedFuture(done.cause()));
						}
					});
				} else {
					resultHandler.handle(Future.failedFuture("Unexpected config action on Device: "
							+cc.getAction().getValue()));
				}
			} else {
				resultHandler.handle(Future.failedFuture(res.cause()));
			}
		});
	}
	private void undoInterfaceConfig(String viewId, ConfigChange cc, 
			Handler<AsyncResult<ConfigChange>> resultHandler) {
		findResourceBkp(cc.getId(), NetInterface.class, res -> {
			if (res.succeeded()) {
				NetInterface netItfBkp = res.result();
				String[] urlParams = cc.getLocation().split("/");
				if (cc.getAction().equals(ActionEnum.UPDATE)) {
					digitalTwinSvcProxy()
							.viewCreateInterface(viewId, urlParams[5], urlParams[7], netItfBkp, done -> {
						if (done.succeeded()) {
							resultHandler.handle(Future.succeededFuture(cc));
						} else {
							resultHandler.handle(Future.failedFuture(done.cause()));
						}
					});
				} else {
					resultHandler.handle(Future.failedFuture(
							"Unexpected config action on Interface: "+cc.getAction().getValue()));
				}
			} else {
				resultHandler.handle(Future.failedFuture(res.cause()));
			}
		});
	}
	private void undoBgpConfig(String viewId, ConfigChange cc, 
			Handler<AsyncResult<ConfigChange>> resultHandler) {
		String[] urlParams = cc.getLocation().split("/");
		
		if (cc.getAction().equals(ActionEnum.CREATE)) {
			digitalTwinSvcProxy().viewDeleteBgp(viewId, urlParams[5], urlParams[7], 
					deleted -> {
				if (deleted.succeeded()) {
					resultHandler.handle(Future.succeededFuture(cc));
				} else {
					resultHandler.handle(Future.failedFuture(deleted.cause()));
				}
			});
			return;
		}
		findResourceBkp(cc.getId(), Bgp.class, res -> {
			if (res.succeeded()) {
				Bgp bgpBkp = res.result();
				if (cc.getAction().equals(ActionEnum.UPDATE) ||
						cc.getAction().equals(ActionEnum.DELETE)) {
					digitalTwinSvcProxy()
							.viewUpdateBgp(viewId, urlParams[5], urlParams[7], bgpBkp, 
							done -> {
						if (done.succeeded()) {
							resultHandler.handle(Future.succeededFuture(cc));
						} else {
							resultHandler.handle(Future.failedFuture(done.cause()));
						}
					});
				} else {
					resultHandler.handle(Future.failedFuture(
							"Unexpected config action on Bgp: "+cc.getAction().getValue()));
				}
			} else {
				resultHandler.handle(Future.failedFuture(res.cause()));
			}
		});
	}
	private void findLatestConfigChange(String viewId, Handler<AsyncResult<ConfigChange>> resultHandler) {
		JsonObject match = new JsonObject().put("_viewId", viewId);
		JsonObject sort = new JsonObject().put("datetime", -1);
			
			JsonObject command = new JsonObject()
				.put("aggregate", COLL_CONFIG_CHANGE)
				.put("pipeline", new JsonArray()
						.add(new JsonObject().put("$match", match))
						.add(new JsonObject().put("$sort", sort))
						.add(new JsonObject().put("$limit", 1)))
				.put("cursor", new JsonObject());
							
			client.runCommand("aggregate", command, res -> {
				if (res.succeeded()) {
					if (res.result().getInteger("ok") == 1) {
						JsonArray batch = res.result().getJsonObject("cursor").getJsonArray("firstBatch");
						if (!batch.isEmpty()) {
							JsonObject result = batch.getJsonObject(0);
							result.remove("_id");
							result.remove("_viewId");
							final ConfigChange cc = new ConfigChange(result);
							resultHandler.handle(Future.succeededFuture(cc));
						} else {
							resultHandler.handle(Future.failedFuture("NOT_FOUND"));
						}
					} else {
						resultHandler.handle(Future.failedFuture("NOT_FOUND"));
					}
				} else {
					resultHandler.handle(Future.failedFuture(res.cause()));
				}
			});
	}
	private void saveConfigChange(String viewId, ConfigChange cc, Handler<AsyncResult<Void>> resultHandler) {
		JsonObject doc = cc.toJson();
		doc.put("_viewId", viewId);
		client.save(COLL_CONFIG_CHANGE, doc, ar -> {
			if (ar.succeeded()) {
				resultHandler.handle(Future.succeededFuture());
			} else {
				resultHandler.handle(Future.failedFuture(ar.cause()));
			}
		});
	}
	private void deleteConfigChange(String id, Handler<AsyncResult<Void>> resultHandler) {
		JsonObject query = new JsonObject().put("id", id);
		client.removeDocument(COLL_CONFIG_CHANGE, query, ar -> {
			if (ar.succeeded()) {
				resultHandler.handle(Future.succeededFuture());
			} else {
				resultHandler.handle(Future.failedFuture(ar.cause()));
			}
		});
	}
	
	/* Functional: Resources backup */
	private <T> void findResourceBkp(String cgId, Class<T> clazz, Handler<AsyncResult<T>> resultHandler) {
		JsonObject query = new JsonObject().put("_cgId", cgId);
		client.findOne(COLL_RESOURCE_BKP, query, null, ar -> {
			if (ar.succeeded()) {
				if (ar.result() != null) {
					ar.result().remove("_cgId");
					ar.result().remove("_viewId");
					ar.result().remove("_id");
					final T resource = JsonUtils.json2Pojo(ar.result().encode(), clazz);
					resultHandler.handle(Future.succeededFuture(resource));
				} else {
					resultHandler.handle(Future.failedFuture("NOT_FOUND"));
				}
			} else {
				resultHandler.handle(Future.failedFuture(ar.cause()));
			}
		});
	}
	private void saveResourceBkp(String viewId, String cgId, Configurable resource, 
			Handler<AsyncResult<Void>> resultHandler) {
		JsonObject doc = resource.toJson();
		doc.put("_cgId", cgId);
		doc.put("_viewId", viewId);
		client.save(COLL_RESOURCE_BKP, doc, ar -> {
			if (ar.succeeded()) {
				resultHandler.handle(Future.succeededFuture());
			} else {
				resultHandler.handle(Future.failedFuture(ar.cause()));
			}
		});
	}
	private void deleteResourceBkp(String cgId, Handler<AsyncResult<Void>> resultHandler) {
		JsonObject query = new JsonObject().put("_cgId", cgId);
		client.removeDocument(COLL_RESOURCE_BKP, query, ar -> {
			if (ar.succeeded()) {
				resultHandler.handle(Future.succeededFuture());
			} else {
				resultHandler.handle(Future.failedFuture(ar.cause()));
			}
		});
	}

	/* Functional: ConfigProfile COW */
	private void getOrCreateConfigProfile(String viewId, Handler<AsyncResult<ConfigProfile>> resultHandler) {
		findConfigProfile(viewId, res -> {
			if (res.succeeded()) {
				resultHandler.handle(Future.succeededFuture(res.result()));
			} else {
				digitalTwinSvcProxy().createView(viewId, created -> {
					if (created.succeeded()) {
						ConfigProfile cp = new ConfigProfile();
						cp.setViewId(viewId);
						cp.setViewUpdated(OffsetDateTime.now().toLocalDateTime().toString());
						saveConfigProfile(cp, done -> {
							if (done.succeeded()) {
								resultHandler.handle(Future.succeededFuture(cp));
							} else {
								resultHandler.handle(Future.failedFuture("Failed to save config profile"));
								digitalTwinSvcProxy().deleteView(viewId, r -> {});
							}
						});
					} else {
						resultHandler.handle(Future.failedFuture("Failed to create config profile"));
					}
				});
			}
		});
	}
	private void findConfigProfile(String viewId, Handler<AsyncResult<ConfigProfile>> resultHandler) {
		JsonObject query = new JsonObject().put("viewId", viewId);
		client.findOne(COLL_CONFIG_PROFILE, query, null, ar -> {
			if (ar.succeeded()) {
				if (ar.result() != null) {
					ar.result().remove("_id");
					final ConfigProfile cp = new ConfigProfile(ar.result());
					resultHandler.handle(Future.succeededFuture(cp));
				} else {
					resultHandler.handle(Future.failedFuture("NOT_FOUND"));
				}
			} else {
				resultHandler.handle(Future.failedFuture(ar.cause()));
			}
		});
	}
	private void saveConfigProfile(ConfigProfile cp, Handler<AsyncResult<Void>> resultHandler) {
		JsonObject doc = cp.toJson();
		client.save(COLL_CONFIG_PROFILE, doc, ar -> {
			if (ar.succeeded()) {
				resultHandler.handle(Future.succeededFuture());
			} else {
				resultHandler.handle(Future.failedFuture(ar.cause()));
			}
		});
	}
	private void updateConfigProfile(String viewId, ConfigProfile cp, Handler<AsyncResult<Void>> resultHandler) {
		JsonObject query = new JsonObject().put("viewId", viewId);
		JsonObject doc = cp.toJson();
		client.findOneAndReplace(COLL_CONFIG_PROFILE, query, doc, ar -> {
			if (ar.succeeded()) {
				resultHandler.handle(Future.succeededFuture());
			} else {
				resultHandler.handle(Future.failedFuture(ar.cause()));
			}
		});
	}
	private void cleanProfile(String viewId, Handler<AsyncResult<Void>> resultHandler) {
		JsonObject qViewId = new JsonObject().put("_viewId", viewId);
		client.removeDocument(COLL_CONFIG_PROFILE, new JsonObject().put("viewId", viewId), 
				ar1 -> {
			if (ar1.succeeded()) {
				client.removeDocuments(COLL_RESOURCE_BKP, qViewId, ar2 -> {
					if (ar2.succeeded()) {
						client.removeDocuments(COLL_CONFIG_CHANGE, qViewId, ar3 -> {
							if (ar3.succeeded()) {
								resultHandler.handle(Future.succeededFuture());
							} else {
								resultHandler.handle(Future.failedFuture(ar3.cause()));
							}
						});
					} else {
						resultHandler.handle(Future.failedFuture(ar2.cause()));
					}
				});
			} else {
				resultHandler.handle(Future.failedFuture(ar1.cause()));
			}
		});
	}
	
	/* Functional: Network config */
	private void saveNetworkConfig(String collection, String viewId, JsonObject netConfig, Handler<AsyncResult<Void>> resultHandler) {
		JsonObject query = new JsonObject().put("_viewId", viewId);
		UpdateOptions opts = new UpdateOptions().setUpsert(true);
		
		netConfig.put("_viewId", viewId);
		JsonObject doc = new JsonObject().put("$set", netConfig);
		client.updateCollectionWithOptions(collection, query, doc, opts, ar -> {
			if (ar.succeeded()) {
				resultHandler.handle(Future.succeededFuture());
			} else {
				resultHandler.handle(Future.failedFuture(ar.cause()));
			}
		});
	}
	private void findConfig(String collection, String viewId, String deviceName, Handler<AsyncResult<JsonObject>> resultHandler) {
		JsonObject query = new JsonObject().put("_viewId", viewId);
		JsonObject filter = null;
		if (deviceName != null) {
			filter = new JsonObject().put(deviceName, 1);
		}
		client.findOne(collection, query, filter, ar -> {
			if (ar.succeeded()) {
				if (ar.result() != null) {
					ar.result().remove("_id");
					ar.result().remove("_viewId");
					if (deviceName != null ) {
						resultHandler.handle(Future.succeededFuture(ar.result().getJsonObject(deviceName)));
					} else {
						resultHandler.handle(Future.succeededFuture(ar.result()));
					}
				} else {
					resultHandler.handle(Future.failedFuture("NOT_FOUND"));
				}
			} else {
				resultHandler.handle(Future.failedFuture(ar.cause()));
			}
		});
	}
	
	/* Helpers */
	private DigitalTwinService digitalTwinSvcProxy() {
		ServiceProxyBuilder builder = new ServiceProxyBuilder(vertx)
	  			.setAddress(DigitalTwinService.SERVICE_ADDRESS);
		return builder.build(DigitalTwinService.class);
	}
	
	private String getConfigDeviceURL(String deviceName) {
		return "/api/"+serviceApi+"/config/device/"+deviceName;
	}
	private String getConfigInterfaceURL(String deviceName, String itfName) {
		return "/api/"+serviceApi+"/config/device/"+deviceName+"/interface/"+itfName;
	}
	private String getConfigBgpURL(String deviceName, String itfAddr) {
		return "/api/"+serviceApi+"/config/device/"+deviceName+"/bgp/"+itfAddr;
	}
}

