package io.nms.central.microservice.ipnet.impl;

import java.util.List;
import java.util.stream.Collectors;

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
	public void runningGetDeviceInterfaces(String deviceName, Handler<AsyncResult<List<NetInterface>>> resultHandler) {
		digitalTwinSvcProxy().runningGetDeviceInterfaces(deviceName, resultHandler);
	}
	@Override
	public void runningGetDeviceBgps(String deviceName, Handler<AsyncResult<List<Bgp>>> resultHandler) {
		digitalTwinSvcProxy().runningGetDeviceBgps(deviceName, resultHandler);
	}

	/* Read/Write View */
	@Override
	public void configGetNetwork(String viewId, Handler<AsyncResult<Network>> resultHandler) {
		digitalTwinSvcProxy().viewGetNetwork(viewId, resultHandler);
	}

	@Override
	public void configUpdateDevice(String viewId, String deviceName, Device device, 
			Handler<AsyncResult<Void>> resultHandler) {
		ConfigChange cc = new ConfigChange();
		cc.setType(ResourceTypeEnum.DEVICE);
		cc.setAction(ActionEnum.UPDATE);
		cc.setLocation(getConfigDeviceURL(deviceName));

		digitalTwinSvcProxy().viewGetDevice(viewId, deviceName, res -> {
			if (res.succeeded()) {
				Device deviceBkp = res.result();
				saveResourceBkp(cc.getId(), deviceBkp, saved -> {
					if (saved.succeeded()) {
						digitalTwinSvcProxy().viewUpdateDevice(viewId, deviceName, device, updated -> {
							if (updated.succeeded()) {
								saveConfigChange(viewId, cc, done -> {
									if (done.succeeded()) {
										resultHandler.handle(Future.succeededFuture());
									} else {
										resultHandler.handle(Future.failedFuture("Config change not saved"));
										// undo update, delete bkp
										digitalTwinSvcProxy().viewUpdateDevice(viewId, deviceName, deviceBkp, 
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
				resultHandler.handle(Future.failedFuture("Resource not found"));
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
	public void configGetDeviceBgps(String viewId, String deviceName, Handler<AsyncResult<List<Bgp>>> resultHandler) {
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
		digitalTwinSvcProxy().viewVerify(viewId, resultHandler);
	}
	@Override
	public void configApply(String viewId, Handler<AsyncResult<ApplyConfigResult>> resultHandler) {
		// 
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
								raw.remove("_viewId");
								ConfigChange cc = new ConfigChange(raw);
								return cc;
							})
						.collect(Collectors.toList());
					resultHandler.handle(Future.succeededFuture(result));
				}
			} else {
				resultHandler.handle(Future.failedFuture(ar.cause()));
			}
		});
	}
	@Override
	public void undoConfigChange(String viewId, String id, Handler<AsyncResult<Void>> resultHandler) {
		// get cc, get bkp (if needed), revert action, delete cc
	}
	@Override
	public void saveConfigChange(String viewId, ConfigChange cc, Handler<AsyncResult<Void>> resultHandler) {
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
	
	/* Resource config backup */
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
	private void createConfigProfile(String viewId, Handler<AsyncResult<ConfigProfile>> resultHandler) {
		// TODO: ...
	}
	
	/* ConfigProfile DB operations */
	private void getConfigProfile(String viewId, Handler<AsyncResult<ConfigProfile>> resultHandler) {
		JsonObject query = new JsonObject().put("viewId", viewId);
		client.findOne(COLL_CONFIG_PROFILE, query, null, ar -> {
			if (ar.succeeded()) {
				final ConfigProfile cp = new ConfigProfile(ar.result());
				resultHandler.handle(Future.succeededFuture(cp));
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

