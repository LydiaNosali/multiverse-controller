package io.nms.central.microservice.ipnet.impl;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

import io.nms.central.microservice.common.functional.JSONUtils;
import io.nms.central.microservice.digitaltwin.DigitalTwinService;
import io.nms.central.microservice.digitaltwin.model.dt.Report;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.Bgp;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.Configurable;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.Device;
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
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.serviceproxy.ServiceProxyBuilder;

/**
 * This verticle implements the ipnet service
 */
public class IpnetServiceImpl implements IpnetService {

	private static final Logger logger = LoggerFactory.getLogger(IpnetServiceImpl.class);

	private static final String COLL_CONFIG_PROFILE = "config-profile";
	private static final String COLL_CONFIG_CHANGE = "config-change";
	private static final String COLL_RESOURCE_BKP = "resource-bkp";
	
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
	
	/* Read running state */
	@Override
	public void runningVerify(Handler<AsyncResult<Report>> resultHandler) {
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

	/* Read/Write View */
	@Override
	public void configGetNetwork(String viewId, 
			Handler<AsyncResult<Network>> resultHandler) {
		digitalTwinSvcProxy().viewGetNetwork(viewId, resultHandler);
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
						saveResourceBkp(cc.getId(), deviceBkp, saved -> {
							if (saved.succeeded()) {
								digitalTwinSvcProxy()
										.viewUpdateDevice(viewId, deviceName, device, updated -> {
									if (updated.succeeded()) {
										saveConfigChange(viewId, cc, done -> {
											if (done.succeeded()) {
												resultHandler.handle(Future.succeededFuture());
											} else {
												resultHandler.handle(Future.failedFuture(done.cause()));
												// undo update, delete bkp
												digitalTwinSvcProxy()
														.viewUpdateDevice(viewId, deviceName, deviceBkp, 
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
	public void configGetDeviceInterfaces(String viewId, String deviceName, 
			Handler<AsyncResult<List<NetInterface>>> resultHandler) {
		digitalTwinSvcProxy().viewGetDeviceInterfaces(viewId, deviceName, resultHandler);
	}

	@Override
	public void configUpdateInterface(String viewId, String deviceName, String itfName, NetInterface netItf,
			Handler<AsyncResult<Void>> resultHandler) {
		//
	}

	@Override
	public void configGetDeviceBgps(String viewId, String deviceName, 
			Handler<AsyncResult<List<Bgp>>> resultHandler) {
		digitalTwinSvcProxy().viewGetDeviceBgps(viewId, deviceName, resultHandler);
	}

	@Override
	public void configCreateBgp(String viewId, String deviceName, String itfAddr, Bgp bgp, 
			Handler<AsyncResult<Void>> resultHandler) {
		// 
	}

	@Override
	public void configUpdateBgp(String viewId, String deviceName, String itfAddr, Bgp bgp, 
			Handler<AsyncResult<Void>> resultHandler) {
		// 
	}

	@Override
	public void configDeleteBgp(String viewId, String deviceName, String itfAddr, 
			Handler<AsyncResult<Void>> resultHandler) {
		// 
	}

	/* Verify and Apply */
	@Override
	public void configVerify(String viewId, Handler<AsyncResult<Report>> resultHandler) {
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
		deleteConfigProfile(viewId, res -> {
			// TODO: get device configs
			resultHandler.handle(Future.succeededFuture(new ApplyConfigResult()));
		});
	}

	/* Read/Write Config Changes */
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
	public void undoConfigChange(String viewId, String id, Handler<AsyncResult<Void>> resultHandler) {
		Promise<ConfigChange> reverted = Promise.promise();
		findConfigChange(id, res -> {
			if (res.succeeded()) {
				ConfigChange cc = res.result();
				if (cc.getType().equals(ResourceTypeEnum.DEVICE)) {
					undoDeviceConfig(viewId, cc, reverted);
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
	private void undoDeviceConfig(String viewId, ConfigChange cc, 
			Handler<AsyncResult<ConfigChange>> resultHandler) {
		findResourceBkp(cc.getId(), Device.class, res -> {
			if (res.succeeded()) {
				Device deviceBkp = res.result();
				if (cc.getAction().equals(ActionEnum.UPDATE)) {
					digitalTwinSvcProxy()
							.viewUpdateDevice(viewId, deviceBkp.getName(), deviceBkp, done -> {
						if (done.succeeded()) {
							resultHandler.handle(Future.succeededFuture(cc));
						} else {
							resultHandler.handle(Future.failedFuture(done.cause()));
						}
					});
				}
			} else {
				resultHandler.handle(Future.failedFuture(res.cause()));
			}
		});
	}
	private void findConfigChange(String id, Handler<AsyncResult<ConfigChange>> resultHandler) {
		JsonObject query = new JsonObject().put("id", id);
		client.findOne(COLL_CONFIG_CHANGE, query, null, ar -> {
			if (ar.succeeded()) {
				if (ar.result() != null) {
					ar.result().remove("_id");
					ar.result().remove("_viewId");
					final ConfigChange cc = new ConfigChange(ar.result());
					resultHandler.handle(Future.succeededFuture(cc));
				} else {
					resultHandler.handle(Future.failedFuture("NOT_FOUND"));
				}
			} else {
				resultHandler.handle(Future.failedFuture(ar.cause()));
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
	
	/* Resource config backup */
	private <T> void findResourceBkp(String cgId, Class<T> clazz, Handler<AsyncResult<T>> resultHandler) {
		JsonObject query = new JsonObject().put("_cgId", cgId);
		client.findOne(COLL_RESOURCE_BKP, query, null, ar -> {
			if (ar.succeeded()) {
				if (ar.result() != null) {
					ar.result().remove("_cgId");
					ar.result().remove("_id");
					final T resource = JSONUtils.json2Pojo(ar.result().encode(), clazz);
					resultHandler.handle(Future.succeededFuture(resource));
				} else {
					resultHandler.handle(Future.failedFuture("NOT_FOUND"));
				}
			} else {
				resultHandler.handle(Future.failedFuture(ar.cause()));
			}
		});
	}
	private void saveResourceBkp(String cgId, Configurable resource, Handler<AsyncResult<Void>> resultHandler) {
		JsonObject doc = resource.toJson();
		doc.put("_cgId", cgId);
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

	/* ConfigProfile COW */
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
	
	/* ConfigProfile DB operations */
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
	private void deleteConfigProfile(String viewId, Handler<AsyncResult<Void>> resultHandler) {
		JsonObject query = new JsonObject().put("viewId", viewId);
		client.removeDocument(COLL_CONFIG_PROFILE, query, ar -> {
			if (ar.succeeded()) {
				resultHandler.handle(Future.succeededFuture());
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

