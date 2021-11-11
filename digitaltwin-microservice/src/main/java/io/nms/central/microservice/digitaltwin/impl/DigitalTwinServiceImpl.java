package io.nms.central.microservice.digitaltwin.impl;

import java.util.ArrayList;
import java.util.List;

import io.nms.central.microservice.common.functional.JSONUtils;
import io.nms.central.microservice.digitaltwin.DigitalTwinService;
import io.nms.central.microservice.digitaltwin.model.dt.DtQuery;
import io.nms.central.microservice.digitaltwin.model.dt.DtQueryResult;
import io.nms.central.microservice.digitaltwin.model.dt.Report;
import io.nms.central.microservice.digitaltwin.model.graph.NetConfigCollection;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.Bgp;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.Configuration;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.Device;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.IpSubnet;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.Link;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.NetInterface;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.Network;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Implementation of Digital-Twin service API
 */
public class DigitalTwinServiceImpl extends Neo4jWrapper implements DigitalTwinService {

	private static final Logger logger = LoggerFactory.getLogger(DigitalTwinServiceImpl.class);

	private static final String MAIN_DB = "neo4j";

	public DigitalTwinServiceImpl(Vertx vertx, JsonObject config) {
		super(vertx, config);
	}

	@Override
	public DigitalTwinService initializePersistence(Handler<AsyncResult<Void>> resultHandler) {
		List<String> constraints = new ArrayList<String>();
		// constraints.add(CypherQuery.CLEAR_DB);
		constraints.add(CypherQuery.Constraints.UNIQUE_HOST);
		constraints.add(CypherQuery.Constraints.UNIQUE_HOSTNAME);
		constraints.add(CypherQuery.Constraints.UNIQUE_IP4CTP);
		constraints.add(CypherQuery.Constraints.UNIQUE_BGP);
		bulkExecute(MAIN_DB, constraints, res -> {
			if (res.succeeded()) {
				logger.info("DB initialized: " + res.result().encodePrettily());
				resultHandler.handle(Future.succeededFuture());
			} else {
				logger.error(res.cause());
				resultHandler.handle(Future.failedFuture(res.cause()));
			}
		});
		return this;
	}

	@Override
	public DigitalTwinService runningProcessNetworkConfig(NetConfigCollection config,
			Handler<AsyncResult<Report>> resultHandler) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DigitalTwinService runningVerifyNetworkConfig(Handler<AsyncResult<Report>> resultHandler) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DigitalTwinService runningQueryNetwork(DtQuery query, Handler<AsyncResult<DtQueryResult>> resultHandler) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DigitalTwinService runningGetNetwork(Handler<AsyncResult<Network>> resultHandler) {
		getNetwork(MAIN_DB, resultHandler);
		return this;
	}

	@Override
	public DigitalTwinService runningGetDeviceInterfaces(String deviceName,
			Handler<AsyncResult<List<NetInterface>>> resultHandler) {
		getDeviceInterfaces(MAIN_DB, deviceName, resultHandler);
		return this;
	}

	@Override
	public DigitalTwinService runningGetDeviceBgps(String deviceName, Handler<AsyncResult<List<Bgp>>> resultHandler) {
		getDeviceBgps(MAIN_DB, deviceName, resultHandler);
		return this;
	}

	@Override
	public DigitalTwinService createView(String viewId, Handler<AsyncResult<Void>> resultHandler) {
		JsonObject params = new JsonObject().put("viewId", viewId);
		execute(MAIN_DB, CypherQuery.View.CREATE_VIEW, params, created -> {
			if (created.succeeded()) {
				logger.info("View created");
				execute(viewId, CypherQuery.View.INIT_VIEW.get(0), init1 -> {
					if (init1.succeeded()) {
						execute(viewId, CypherQuery.View.INIT_VIEW.get(1), init2 -> {
							if (init2.succeeded()) {
								logger.info("View initialized");
								String eq = CypherQuery.View.getExtractionQuery(MAIN_DB, dbUser, dbPassword);
									findOne(viewId, eq, done -> {
										if (done.succeeded()) {
											logger.info("View creation done: " + done.result().encodePrettily());
											resultHandler.handle(Future.succeededFuture());
										} else {
											deleteView(viewId, res -> {
												logger.info("Delete view after initialization failed");
											});
											resultHandler.handle(Future.failedFuture(done.cause()));
										}
									});
							} else {
								deleteView(viewId, res -> {
									logger.info("Delete view after initialization failed");
								});
								resultHandler.handle(Future.failedFuture(init2.cause()));
							}
						});
					} else {
						deleteView(viewId, res -> {
							logger.info("Delete view after initialization failed");
						});
						resultHandler.handle(Future.failedFuture(init1.cause()));
					}
				});
			} else {
				resultHandler.handle(Future.failedFuture(created.cause()));
			}
		});
		return this;
	}

	@Override
	public DigitalTwinService deleteView(String viewId, Handler<AsyncResult<Void>> resultHandler) {
		JsonObject params = new JsonObject().put("viewId", viewId);
		execute(MAIN_DB, CypherQuery.View.DELETE_VIEW, params, res -> {
			if (res.succeeded()) {
				resultHandler.handle(Future.succeededFuture());
			} else {
				resultHandler.handle(Future.failedFuture(res.cause()));
			}
		});
		return this;
	}

	@Override
	public DigitalTwinService viewVerifyConfig(String viewId, Configuration configuration,
			Handler<AsyncResult<Void>> resultHandler) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DigitalTwinService viewGenerateNetworkConfig(String viewId, Configuration configuration,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DigitalTwinService viewGetNetwork(String viewId, Handler<AsyncResult<Network>> resultHandler) {
		getNetwork(viewId, resultHandler);
		return this;
	}

	@Override
	public DigitalTwinService viewGetDevice(String viewId, String deviceName,
			Handler<AsyncResult<Device>> resultHandler) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DigitalTwinService viewUpdateDevice(String viewId, String deviceName, Device device, 
			Handler<AsyncResult<Void>> resultHandler) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DigitalTwinService viewGetDeviceInterfaces(String viewId, String deviceName,
			Handler<AsyncResult<List<NetInterface>>> resultHandler) {
		getDeviceInterfaces(viewId, deviceName, resultHandler);
		return this;
	}

	@Override
	public DigitalTwinService viewGetInterface(String viewId, String deviceName, String itfName,
			Handler<AsyncResult<NetInterface>> resultHandler) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DigitalTwinService viewUpdateInterface(String viewId, String deviceName, String itfName,
			NetInterface netInterface, Handler<AsyncResult<Void>> resultHandler) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DigitalTwinService viewGetDeviceBgps(String viewId, String deviceName,
			Handler<AsyncResult<List<Bgp>>> resultHandler) {
		getDeviceBgps(viewId, deviceName, resultHandler);
		return this;
	}

	@Override
	public DigitalTwinService viewGetBgp(String viewId, String deviceName, String itfAddr,
			Handler<AsyncResult<Bgp>> resultHandler) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DigitalTwinService viewCreateBgp(String viewId, String deviceName, String itfAddr, Bgp bgp,
			Handler<AsyncResult<Void>> resultHandler) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DigitalTwinService viewUpdateBgp(String viewId, String deviceName, String itfAddr, Bgp bgp,
			Handler<AsyncResult<Void>> resultHandler) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DigitalTwinService viewDeleteBgp(String viewId, String deviceName, String itfAddr,
			Handler<AsyncResult<Void>> resultHandler) {
		// TODO Auto-generated method stub
		return null;
	}
	
	private void getNetwork(String db, Handler<AsyncResult<Network>> resultHandler) {
		Network network = new Network();
		find(db, CypherQuery.Api.GET_NETWORK_HOSTS, hosts -> {
			if (hosts.succeeded()) {
				network.setDevices(JSONUtils.json2PojoList(new JsonArray(hosts.result()).encode(), Device.class));
				find(MAIN_DB, CypherQuery.Api.GET_NETWORK_LINKS, links -> {
					if (links.succeeded()) {
						network.setLinks(JSONUtils.json2PojoList(new JsonArray(links.result()).encode(), Link.class));
						find(MAIN_DB, CypherQuery.Api.GET_NETWORK_SUBNETS, subnets -> {
							if (subnets.succeeded()) {
								network.setSubnets(JSONUtils.json2PojoList(new JsonArray(subnets.result()).encode(), IpSubnet.class));
								resultHandler.handle(Future.succeededFuture(network));
							} else {
								resultHandler.handle(Future.failedFuture(subnets.cause()));
							}
						});
					} else {
						resultHandler.handle(Future.failedFuture(links.cause()));
					}
				});
			} else {
				resultHandler.handle(Future.failedFuture(hosts.cause()));
			}
		});
	}
	
	private void getDeviceInterfaces(String db, String deviceName,
			Handler<AsyncResult<List<NetInterface>>> resultHandler) {
		JsonObject params = new JsonObject().put("deviceName", deviceName);
		find(db, CypherQuery.Api.GET_HOST_INTERFACES, params, res -> {
			if (res.succeeded()) {
				List<NetInterface> netItfs = JSONUtils.json2PojoList(new JsonArray(res.result()).encode(), NetInterface.class);
				resultHandler.handle(Future.succeededFuture(netItfs));
			} else {
				resultHandler.handle(Future.failedFuture(res.cause()));
			}
		});
	}
	
	private void getDeviceBgps(String db, String deviceName, Handler<AsyncResult<List<Bgp>>> resultHandler) {
		JsonObject params = new JsonObject().put("deviceName", deviceName);
		find(db, CypherQuery.Api.GET_HOST_BGPS, params, res -> {
			if (res.succeeded()) {
				List<Bgp> bgps = JSONUtils.json2PojoList(new JsonArray(res.result()).encode(), Bgp.class);
				resultHandler.handle(Future.succeededFuture(bgps));
			} else {
				resultHandler.handle(Future.failedFuture(res.cause()));
			}
		});
	}
}