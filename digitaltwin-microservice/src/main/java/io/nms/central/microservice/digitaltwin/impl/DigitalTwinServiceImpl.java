package io.nms.central.microservice.digitaltwin.impl;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;

import io.nms.central.microservice.common.functional.Functional;
import io.nms.central.microservice.common.functional.JsonUtils;
import io.nms.central.microservice.digitaltwin.DigitalTwinService;
import io.nms.central.microservice.digitaltwin.model.dt.CreationReport;
import io.nms.central.microservice.digitaltwin.model.dt.VerificationReport;
import io.nms.central.microservice.digitaltwin.model.graph.DeviceState;
import io.nms.central.microservice.digitaltwin.model.graph.NetworkState;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.AclTable;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.Arp;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.Bgp;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.Device;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.IpRoute;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.IpSubnet;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.Link;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.NetInterface;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.Network;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.Path;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.PathHop;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.RouteHop;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.Vlan;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.VlanMember;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
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
	private final NetworkAnalyser na;

	public DigitalTwinServiceImpl(Vertx vertx, JsonObject config) {
		super(vertx, config);
		na = new NetworkAnalyser(this);
	}

	@Override
	public DigitalTwinService initializePersistence(Handler<AsyncResult<Void>> resultHandler) {
		List<String> constraints = new ArrayList<String>();
		// constraints.add(CypherQuery.CLEAR_DB);
		constraints.add(CypherQuery.Constraints.UNIQUE_HOST);
		bulkExecute(MAIN_DB, constraints, res -> {
			if (res.succeeded()) {
				logger.info("Neo4j DB initialized");
				loadExampleNetwork(resultHandler);
			} else {
				logger.error(res.cause());
				resultHandler.handle(Future.failedFuture(res.cause()));
			}
		});
		return this;
	}

	@Override
	public DigitalTwinService processNetworkRunningState(NetworkState netState,
			Handler<AsyncResult<CreationReport>> resultHandler) {
		CreationReport report = new CreationReport();
		resultHandler.handle(Future.succeededFuture(report));
		
		processNetworkState(netState, res -> {
			if (res.succeeded()) {
				logger.info("Running state processed. Report: " + res.result().toJson().encodePrettily());
			} else {
				logger.info("Failed to process running state: " + res.cause());
			}
		});
		return this;
	}

	@Override
	public DigitalTwinService runningVerifyNetwork(Handler<AsyncResult<VerificationReport>> resultHandler) {
		na.verifyNetwork(MAIN_DB, resultHandler);
		return this;
	}
	
	@Override
	public DigitalTwinService runningGetNetworkConfig(Handler<AsyncResult<JsonObject>> resultHandler) {
		generateNetworkConfig(MAIN_DB, resultHandler);
		return this;
	}

	@Override
	public DigitalTwinService runningGetNetwork(Handler<AsyncResult<Network>> resultHandler) {
		getNetwork(MAIN_DB, resultHandler);
		return this;
	}
	
	@Override
	public DigitalTwinService runningGetDevice(String deviceName, Handler<AsyncResult<Device>> resultHandler) {
		getDevice(MAIN_DB, deviceName, resultHandler);
		return this;
	}
	@Override
	public DigitalTwinService runningGetDeviceInterfaces(String deviceName,
			Handler<AsyncResult<List<NetInterface>>> resultHandler) {
		getDeviceInterfaces(MAIN_DB, deviceName, resultHandler);
		return this;
	}
	@Override
	public DigitalTwinService runningGetInterface(String deviceName, String itfName,
			Handler<AsyncResult<NetInterface>> resultHandler) {
		getInterface(MAIN_DB, deviceName, itfName, resultHandler);
		return this;
	}
	@Override
	public DigitalTwinService runningGetDeviceBgps(String deviceName, 
			Handler<AsyncResult<List<Bgp>>> resultHandler) {
		getDeviceBgps(MAIN_DB, deviceName, resultHandler);
		return this;
	}
	@Override
	public DigitalTwinService runningGetBgp(String deviceName, String itfAddr,
			Handler<AsyncResult<Bgp>> resultHandler) {
		getBgp(MAIN_DB, deviceName, itfAddr, resultHandler);
		return this;
	}
	@Override
	public DigitalTwinService runningGetDeviceVlans(String deviceName, Handler<AsyncResult<List<Vlan>>> resultHandler) {
		getDeviceVlans(MAIN_DB, deviceName, resultHandler);
		return this;
	}
	@Override
	public DigitalTwinService runningGetVlanMembers(String deviceName, String vid,
			Handler<AsyncResult<List<VlanMember>>> resultHandler) {
		getVlanMembers(MAIN_DB, deviceName, vid, resultHandler);
		return this;
	}
	@Override
	public DigitalTwinService runningGetDeviceConfig(String deviceName, 
			Handler<AsyncResult<JsonObject>> resultHandler) {
		generateDeviceConfig(MAIN_DB, deviceName, resultHandler);
		return this;
	}
	@Override
	public DigitalTwinService runningGetDeviceIpRoutes(String deviceName, 
			Handler<AsyncResult<List<IpRoute>>> resultHandler) {
		JsonObject params = new JsonObject().put("deviceName", deviceName);
		find(MAIN_DB, CypherQuery.Api.GET_HOST_IPROUTES, params, res -> {
			if (res.succeeded()) {
				List<IpRoute> ipRoutes = res.result().stream()
						.map(o -> {return new IpRoute(o);})
						.collect(Collectors.toList());
				resultHandler.handle(Future.succeededFuture(ipRoutes));
			} else {
				resultHandler.handle(Future.failedFuture(res.cause()));
			}
		});
		return this;
	}
	@Override
	public DigitalTwinService runningGetDeviceIpRoutesTo(String deviceName, String to, 
			Handler<AsyncResult<List<IpRoute>>> resultHandler) {
		JsonObject params = new JsonObject()
				.put("deviceName", deviceName)
				.put("to", to);
		find(MAIN_DB, CypherQuery.Api.GET_HOST_IPROUTES_TO, params, res -> {
			if (res.succeeded()) {
				List<IpRoute> ipRoutes = res.result().stream()
						.map(o -> {return new IpRoute(o);})
						.collect(Collectors.toList());
				resultHandler.handle(Future.succeededFuture(ipRoutes));
			} else {
				resultHandler.handle(Future.failedFuture(res.cause()));
			}
		});
		return this;
	}
	@Override
	public DigitalTwinService runningGetDeviceArps(String deviceName, 
			Handler<AsyncResult<List<Arp>>> resultHandler) {
		JsonObject params = new JsonObject().put("deviceName", deviceName);
		find(MAIN_DB, CypherQuery.Api.GET_HOST_ARPS, params, res -> {
			if (res.succeeded()) {
				List<Arp> arps = res.result().stream()
						.map(o -> {return new Arp(o);})
						.collect(Collectors.toList());
				resultHandler.handle(Future.succeededFuture(arps));
			} else {
				resultHandler.handle(Future.failedFuture(res.cause()));
			}
		});
		return this;
	}
	@Override
	public DigitalTwinService runningGetDeviceAclTables(String deviceName, 
			Handler<AsyncResult<List<AclTable>>> resultHandler) {
		JsonObject params = new JsonObject().put("deviceName", deviceName);
		find(MAIN_DB, CypherQuery.Api.GET_HOST_ACLTABLES, params, res -> {
			if (res.succeeded()) {
				List<AclTable> aclTables = res.result().stream()
						.map(o -> {return new AclTable(o);})
						.collect(Collectors.toList());
				resultHandler.handle(Future.succeededFuture(aclTables));
			} else {
				resultHandler.handle(Future.failedFuture(res.cause()));
			}
		});
		return this;
	}

	/* Operations on view */
	@Override
	public DigitalTwinService createView(String viewId, Handler<AsyncResult<Void>> resultHandler) {
		JsonObject params = new JsonObject().put("viewId", viewId);
		Instant start = Instant.now();
		execute(MAIN_DB, CypherQuery.View.CREATE_VIEW, params, created -> {
			if (created.succeeded()) {
				logger.info("DB for view created");
				execute(viewId, CypherQuery.View.INIT_VIEW.get(0), init1 -> {
					if (init1.succeeded()) {
						execute(viewId, CypherQuery.View.INIT_VIEW.get(1), init2 -> {
							if (init2.succeeded()) {
								logger.info("View initialized");
								String eq = CypherQuery.View.getExtractionQuery(MAIN_DB, dbUser, dbPassword);
									execute(viewId, eq, done -> {
										Instant end = Instant.now();
										if (done.succeeded()) {
											logger.info("View ready");
											Duration timeElapsed = Duration.between(start, end);
											logger.info("View creation time: " + timeElapsed.getNano() / 1000000 + " ms.");
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
	public DigitalTwinService createView2(String viewId, Handler<AsyncResult<Void>> resultHandler) {
		vertx.fileSystem().readFile("graph_queries.cypher", arr -> {
			if (arr.succeeded()) {
				Instant start = Instant.now();
				List<String> queries = arr.result().toJsonArray().stream().map(s -> ((String)s)).collect(Collectors.toList());
				JsonObject params = new JsonObject().put("viewId", viewId);
				execute(MAIN_DB, CypherQuery.View.CREATE_VIEW, params, created -> {
					if (created.succeeded()) {
						logger.info("View DB created");
						createGraph(viewId, queries, done -> {
							Instant end = Instant.now();
							if (done.succeeded()) {
								resultHandler.handle(Future.succeededFuture());
								Duration timeElapsed = Duration.between(start, end);
								logger.info("View creation time: " + timeElapsed.getNano() / 1000000 + " ms");
							} else {
								resultHandler.handle(Future.failedFuture(done.cause()));
								deleteView(viewId, ignore -> {});
							}
						});
					} else {
						resultHandler.handle(Future.failedFuture(created.cause()));
					}
				});
			} else {
				logger.info("Graph queries not found");
				resultHandler.handle(Future.failedFuture("Graph queries not found"));
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
	public DigitalTwinService viewGetNetwork(String viewId, Handler<AsyncResult<Network>> resultHandler) {
		getNetwork(viewId, resultHandler);
		return this;
	}

	@Override
	public DigitalTwinService viewGetDevice(String viewId, String deviceName,
			Handler<AsyncResult<Device>> resultHandler) {
		getDevice(viewId, deviceName, resultHandler);
		return this;
	}
	@Override
	public DigitalTwinService viewUpsertDevice(String viewId, String deviceName, 
			Device device, Handler<AsyncResult<Void>> resultHandler) {
		JsonObject params = new JsonObject()
				.put("deviceName", deviceName)
				.put("hostname", device.getHostname())
				.put("bgpStatus", device.getBgpStatus())
				.put("bgpAsn", device.getBgpAsn())
				.put("type", device.getType().getValue())
				.put("platform", device.getPlatform())
				.put("mac", device.getMac())
				.put("hwsku", device.getHwsku());
		execute(viewId, String.format(CypherQuery.Api.UPSERT_HOST, device.getType().getValue()), params, res -> {
			if (res.succeeded()) {
				resultHandler.handle(Future.succeededFuture());
			} else {
				resultHandler.handle(Future.failedFuture(res.cause()));
			}
		});
		return this;
	}
	@Override
	public DigitalTwinService viewDeleteDevice(String viewId, String deviceName, 
			Handler<AsyncResult<Void>> resultHandler) {
		JsonObject params = new JsonObject()
				.put("deviceName", deviceName);
		execute(viewId, CypherQuery.Api.DELETE_HOST, params, res -> {
			if (res.succeeded()) {
				resultHandler.handle(Future.succeededFuture());
			} else {
				resultHandler.handle(Future.failedFuture(res.cause()));
			}
		});
		return this;
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
		getInterface(viewId, deviceName, itfName, resultHandler);
		return this;
	}
	@Override
	public DigitalTwinService viewCreateInterface(String viewId, String deviceName, String itfName, 
			NetInterface netItf, Handler<AsyncResult<Void>> resultHandler) {
		String updateQuery = CypherQuery.Api.CREATE_INTERFACE_NOIP;
		JsonObject params = new JsonObject()
				.put("deviceName", deviceName).put("itfName", itfName)
				.put("adminStatus", netItf.getAdminStatus().getValue()).put("index", netItf.getIndex())
				.put("type", netItf.getType().getValue()).put("speed", netItf.getSpeed())
				.put("mtu", netItf.getMtu()).put("mode", netItf.getMode().getValue())
				.put("vlan", netItf.getVlan()).put("macAddr", netItf.getMacAddr());
		if (netItf.getIpAddr() != null) {
			logger.info("Create interface includes IP config");
			String[] cird = netItf.getIpAddr().split("/");
			String subnetAddr = Functional.parseSubnetAddress(netItf.getIpAddr());
			params
					.put("ipAddr", cird[0]).put("netMask", "/"+cird[1])
					.put("netAddr", subnetAddr).put("svi", netItf.getSvi());
			updateQuery = CypherQuery.Api.CREATE_INTERFACE_IP;
		}
		execute(viewId, updateQuery, params, res -> {
			if (res.succeeded()) {
				if (res.result().getInteger("nodesCreated") > 0) {
					resultHandler.handle(Future.succeededFuture());
				} else {
					resultHandler.handle(Future.failedFuture("CONFLICT"));
				}
			} else {
				resultHandler.handle(Future.failedFuture(res.cause()));
			}
		});
		return this;
	}
	@Override
	public DigitalTwinService viewUpdateInterface(String viewId, String deviceName, String itfName,
			NetInterface netItf, Handler<AsyncResult<Void>> resultHandler) {
		String updateQuery = CypherQuery.Api.UPDATE_INTERFACE_NOIP;
		JsonObject params = new JsonObject()
				.put("deviceName", deviceName).put("itfName", itfName)
				.put("adminStatus", netItf.getAdminStatus().getValue()).put("index", netItf.getIndex())
				.put("type", netItf.getType().getValue()).put("speed", netItf.getSpeed())
				.put("mtu", netItf.getMtu()).put("mode", netItf.getMode().getValue())
				.put("vlan", netItf.getVlan()).put("macAddr", netItf.getMacAddr());
		if (netItf.getIpAddr() != null) {
			logger.info("Update interface includes IP config");
			String[] cird = netItf.getIpAddr().split("/");
			String subnetAddr = Functional.parseSubnetAddress(netItf.getIpAddr());
			params
					.put("ipAddr", cird[0]).put("netMask", "/"+cird[1])
					.put("netAddr", subnetAddr).put("svi", netItf.getSvi());
			updateQuery = CypherQuery.Api.UPDATE_INTERFACE_IP;
		}
		execute(viewId, updateQuery, params, res -> {
			if (res.succeeded()) {
				resultHandler.handle(Future.succeededFuture());
			} else {
				resultHandler.handle(Future.failedFuture(res.cause()));
			}
		});
		return this;
	}
	@Override
	public DigitalTwinService viewDeleteInterface(String viewId, String deviceName, 
			String itfName, Handler<AsyncResult<Void>> resultHandler) {
		JsonObject params = new JsonObject()
				.put("deviceName", deviceName)
				.put("itfName", itfName);
		execute(viewId, CypherQuery.Api.DELETE_INTERFACE, params, res -> {
			if (res.succeeded()) {
				resultHandler.handle(Future.succeededFuture());
			} else {
				resultHandler.handle(Future.failedFuture(res.cause()));
			}
		});
		return this;
	}
	
	@Override
	public DigitalTwinService viewCreateLink(String viewId, String linkName,
			Link link, Handler<AsyncResult<Void>> resultHandler) {
		JsonObject params = new JsonObject()
				.put("srcDevice", link.getSrcDevice())
				.put("srcInterface", link.getSrcInterface())
				.put("destDevice", link.getDestDevice())
				.put("destInterface", link.getDestInterface())
				.put("linkName", linkName);
		execute(viewId, CypherQuery.Api.CREATE_LINK, params, res -> {
			if (res.succeeded()) {
				if (res.result().getInteger("propertiesSet") > 0) {
					resultHandler.handle(Future.succeededFuture());
				} else {
					resultHandler.handle(Future.failedFuture("CONFLICT"));
				}
			} else {
				resultHandler.handle(Future.failedFuture(res.cause()));
			}
		});
		return this;
	}
	@Override
	public DigitalTwinService viewDeleteLink(String viewId, String linkName, Handler<AsyncResult<Void>> resultHandler) {
		JsonObject params = new JsonObject()
				.put("linkName", linkName);
		execute(viewId, CypherQuery.Api.DELETE_LINK, params, res -> {
			if (res.succeeded()) {
				resultHandler.handle(Future.succeededFuture());
			} else {
				resultHandler.handle(Future.failedFuture(res.cause()));
			}
		});
		return this;
	}

	@Override
	public DigitalTwinService viewGetDeviceBgps(String viewId, String deviceName,
			Handler<AsyncResult<List<Bgp>>> resultHandler) {
		getDeviceBgps(viewId, deviceName, resultHandler);
		return this;
	}
	@Override
	public DigitalTwinService viewGetDeviceConfig(String viewId, String deviceName, 
			Handler<AsyncResult<JsonObject>> resultHandler) {
		generateDeviceConfig(viewId, deviceName, resultHandler);
		return this;
	}

	@Override
	public DigitalTwinService viewGetBgp(String viewId, String deviceName, String itfAddr,
			Handler<AsyncResult<Bgp>> resultHandler) {
		getBgp(viewId, deviceName, itfAddr, resultHandler);
		return this;
	}
	@Override
	public DigitalTwinService viewUpsertBgp(String viewId, String deviceName, String itfAddr, Bgp bgp,
			Handler<AsyncResult<Void>> resultHandler) {
		JsonObject params = new JsonObject()
				.put("deviceName", deviceName)
				.put("itfAddr", itfAddr)
				.put("localAsn", bgp.getLocalAsn())
				.put("localId", bgp.getLocalId())
				.put("remoteAddr", bgp.getRemoteAddr())
				.put("remoteAsn", bgp.getRemoteAsn())
				.put("remoteId", bgp.getRemoteId())
				.put("holdTime", bgp.getHoldTime())
				.put("keepAlive", bgp.getKeepAlive())
				.put("state", bgp.getState().getValue());
		execute(viewId, CypherQuery.Api.UPSERT_BGP, params, res -> {
			if (res.succeeded()) {
				List<String> bgpQ = 
						Arrays.asList(CypherQuery.Internal.DISCONNECT_BGP, CypherQuery.Internal.RECONNECT_BGP);
				bulkExecute(viewId, bgpQ, done -> {
					if (done.succeeded()) {
						resultHandler.handle(Future.succeededFuture());
					} else {
						resultHandler.handle(Future.failedFuture(done.cause()));
					}
				});
			} else {
				resultHandler.handle(Future.failedFuture(res.cause()));
			}
		});
		return this;
	}
	@Override
	public DigitalTwinService viewDeleteBgp(String viewId, String deviceName, String itfAddr,
			Handler<AsyncResult<Void>> resultHandler) {
		JsonObject params = new JsonObject()
				.put("deviceName", deviceName)
				.put("itfAddr", itfAddr);
		execute(viewId, CypherQuery.Api.DELETE_BGP, params, res -> {
			if (res.succeeded()) {
				resultHandler.handle(Future.succeededFuture());
			} else {
				resultHandler.handle(Future.failedFuture(res.cause()));
			}
		});
		return this;
	}
	
	@Override
	public DigitalTwinService viewGetDeviceVlans(String viewId, String deviceName,
			Handler<AsyncResult<List<Vlan>>> resultHandler) {
		getDeviceVlans(viewId, deviceName, resultHandler);
		return this;
	}

	@Override
	public DigitalTwinService viewCreateVlan(String viewId, String deviceName, String vid, Vlan vlan,
			Handler<AsyncResult<Void>> resultHandler) {
		JsonObject params = new JsonObject();
		execute(viewId, CypherQuery.Api.CREATE_VLAN, params, res -> {
			if (res.succeeded()) {
				resultHandler.handle(Future.succeededFuture());
			} else {
				resultHandler.handle(Future.failedFuture(res.cause()));
			}
		});
		return this;
	}

	@Override
	public DigitalTwinService viewDeleteVlan(String viewId, String deviceName, String vid,
			Handler<AsyncResult<Void>> resultHandler) {
		JsonObject params = new JsonObject();
		execute(viewId, CypherQuery.Api.DELETE_VLAN, params, res -> {
			if (res.succeeded()) {
				resultHandler.handle(Future.succeededFuture());
			} else {
				resultHandler.handle(Future.failedFuture(res.cause()));
			}
		});
		return this;
	}

	@Override
	public DigitalTwinService viewGetVlanMembers(String viewId, String deviceName, String vid,
			Handler<AsyncResult<List<VlanMember>>> resultHandler) {
		getVlanMembers(viewId, deviceName, vid, resultHandler);
		return this;
	}

	@Override
	public DigitalTwinService viewAddVlanMember(String viewId, String deviceName, String vid, String itfName,
			VlanMember vlanMember, Handler<AsyncResult<Void>> resultHandler) {
		JsonObject params = new JsonObject();
		execute(viewId, CypherQuery.Api.ADD_VLAN_MEMBER, params, res -> {
			if (res.succeeded()) {
				resultHandler.handle(Future.succeededFuture());
			} else {
				resultHandler.handle(Future.failedFuture(res.cause()));
			}
		});
		return this;
	}

	@Override
	public DigitalTwinService viewRemoveVlanMember(String viewId, String deviceName, String vid, String itfName,
			Handler<AsyncResult<Void>> resultHandler) {
		JsonObject params = new JsonObject();
		execute(viewId, CypherQuery.Api.REMOVE_VLAN_MEMBER, params, res -> {
			if (res.succeeded()) {
				resultHandler.handle(Future.succeededFuture());
			} else {
				resultHandler.handle(Future.failedFuture(res.cause()));
			}
		});
		return this;
	}
	
	/* ---------------------------------------------------------------------------- */
	@Override
	public DigitalTwinService viewVerify(String viewId, 
			Handler<AsyncResult<VerificationReport>> resultHandler) {
		na.verifyNetwork(viewId, resultHandler);
		return this;
	}
	@Override
	public DigitalTwinService viewGetNetworkConfig(String viewId, 
			Handler<AsyncResult<JsonObject>> resultHandler) {
		generateNetworkConfig(viewId, resultHandler);
		return this;
	}
	
	/* Operations on path search */
	@Override
	public DigitalTwinService runningFindPathByHostnames(String from, String to,
			Handler<AsyncResult<List<Path>>> resultHandler) {
		JsonObject params = new JsonObject()
				.put("from", from)
				.put("to", to);
		find(MAIN_DB, CypherQuery.PathSearch.HOST_TO_HOST, params, res -> {
        	if (res.succeeded()) {
        		List<Path> paths = res.result().stream()
						.map(o -> {return new Path(o);})
						.collect(Collectors.toList());
				resultHandler.handle(Future.succeededFuture(paths));
        	} else {
        		resultHandler.handle(Future.failedFuture(res.cause()));
        	}
        });
		return this;
	}

	@Override
	public DigitalTwinService runningFindPathByIpAddrs(String from, String to,
			Handler<AsyncResult<List<Path>>> resultHandler) {
		JsonObject params = new JsonObject()
				.put("from", from)
				.put("to", to);
		find(MAIN_DB, CypherQuery.PathSearch.IP_TO_IP, params, res -> {
        	if (res.succeeded()) {
        		List<Path> paths = res.result().stream()
						.map(o -> {return new Path(o);})
						.collect(Collectors.toList());
				resultHandler.handle(Future.succeededFuture(paths));
        	} else {
        		resultHandler.handle(Future.failedFuture(res.cause()));
        	}
        });
		return this;
	}
	
	@Override
	public DigitalTwinService runningGetIpRoutesOfPath(List<PathHop> path, 
			Handler<AsyncResult<List<RouteHop>>> resultHandler) {
		String query = CypherQuery.PathSearch.getFindIpPathQuery(path);
		find(MAIN_DB, query, res -> {
        	if (res.succeeded()) {
        		List<RouteHop> ipPaths = res.result().stream()
						.map(o -> {return new RouteHop(o.getJsonObject("route"));})
						.collect(Collectors.toList());
				resultHandler.handle(Future.succeededFuture(ipPaths));
        	} else {
        		resultHandler.handle(Future.failedFuture(res.cause()));
        	}
        });
		return this;
	}
	
	/* Processing functions */
	private void processNetworkState(NetworkState netState, 
			Handler<AsyncResult<CreationReport>> resultHandler) {
		List<String> queries = new ArrayList<String>();
		CreationReport report = new CreationReport();
		vertx.executeBlocking(future -> {
			report.setTimestamp(OffsetDateTime.now().toLocalDateTime().toString());
			
			ConfigProcessor cp = new ConfigProcessor(netState);
			if (!cp.process()) {
				future.fail("Failed to process config");
				// resultHandler.handle(Future.failedFuture("Failed to process config"));
				return;
			}
			report.setConfigProcessor(cp.getReport());

			GraphCreator gc = new GraphCreator(cp.getOutput());
			if (!gc.process()) {
				// resultHandler.handle(Future.failedFuture("Failed to create graph queries"));
				future.fail("Failed to create graph queries");
				return;
			}
			report.setQueriesGenerator(gc.getReport());
			
			queries.add(CypherQuery.CLEAR_DB);
			queries.addAll(gc.getOutput().stream().map(s -> s.split("@")[1]).collect(Collectors.toList()));
			
			future.complete();
		}, res -> {
			if (res.succeeded()) {
				logger.info("Create graph with "+queries.size()+" queries");
				createGraph(MAIN_DB, queries, done -> {
					if (res.succeeded()) {
						report.setGraphCreator(done.result());
						resultHandler.handle(Future.succeededFuture(report));
					
						// save queries for static views creation
						saveQueries(new JsonArray(queries));
					} else {
						resultHandler.handle(Future.failedFuture(done.cause()));	
					}
				});
			} else {
				resultHandler.handle(Future.failedFuture(res.cause()));	
			}
		});
	}
	
	/* private String joinQueries(List<String> queries) {
		String res = "";
		for (String q: queries) {
			res+=q.substring(0, q.length() - 1);
			res+=" ";
		}
		return res;
	} */
	
	/* private void processNetworkStateBlocking(NetworkState netState, 
			Handler<AsyncResult<CreationReport>> resultHandler) {
		List<String> queries = new ArrayList<String>();
		CreationReport report = new CreationReport();
		vertx.executeBlocking(future -> {
			report.setTimestamp(OffsetDateTime.now().toLocalDateTime().toString());
			
			ConfigProcessor cp = new ConfigProcessor(netState);
			if (!cp.process()) {
				future.fail("Failed to process config");
				// resultHandler.handle(Future.failedFuture("Failed to process config"));
				return;
			}
			report.setConfigProcessor(cp.getReport());

			GraphCreator gc = new GraphCreator(cp.getOutput());
			if (!gc.process()) {
				// resultHandler.handle(Future.failedFuture("Failed to create graph queries"));
				future.fail("Failed to create graph queries");
				return;
			}
			report.setQueriesGenerator(gc.getReport());
			
			queries.add(CypherQuery.CLEAR_DB);
			queries.addAll(gc.getOutput().stream().map(s -> s.split("@")[1]).collect(Collectors.toList()));
			
			future.complete();
		}, res -> {
			if (res.succeeded()) {
				logger.info("Create graph with "+queries.size()+" queries");
				createGraphBlocking(MAIN_DB, queries, done -> {
					if (done.succeeded()) {
						logger.info("Total queries: " + queries.size());
						// report.setGraphCreator(done.result());
						resultHandler.handle(Future.succeededFuture(report));

						// save queries for static views creation
						saveQueries(new JsonArray(queries));
					} else {
						resultHandler.handle(Future.failedFuture(done.cause()));	
					}
				});
			} else {
				resultHandler.handle(Future.failedFuture(res.cause()));	
			}
		});
	} */
	
	private void saveQueries(JsonArray queries) {
		vertx.fileSystem().writeFile("graph_queries.json", queries.toBuffer(), arw -> {
			if (arw.succeeded()) {
				logger.info("Graph queries saved");
			}
		});
	}
	
	private void generateNetworkConfig(String db, 
			Handler<AsyncResult<JsonObject>> resultHandler) {
		ConfigGenerator cg = new ConfigGenerator();
		find(db, CypherQuery.ConfigGen.GET_DEVICES, res -> {
			if (res.succeeded()) {
				List<String> devices = res.result().stream()
						.map(d -> d.getString("device"))
						.collect(Collectors.toList());
				List<Future<JsonObject>> futures = new ArrayList<Future<JsonObject>>();
				for (String device: devices) {
					Promise<JsonObject> p = Promise.promise();
					futures.add(p.future());
					retrieveDeviceConfig(db, device, p);
				}
				Functional.allOfFutures(futures).onComplete(done -> {
					vertx.runOnContext(v -> {
						if (done.succeeded()) {
							JsonObject finalRes = cg.processNetConfig(done.result());
							resultHandler.handle(Future.succeededFuture(finalRes));
						} else {
							resultHandler.handle(Future.failedFuture(done.cause()));
						}
					});	
				});
			} else {
				resultHandler.handle(Future.failedFuture(res.cause()));
			}
		});
	}
	
	private void generateDeviceConfig(String db, String deviceName, 
			Handler<AsyncResult<JsonObject>> resultHandler) {
		findOne(db, CypherQuery.Internal.CHECK_HOST, 
				new JsonObject().put("deviceName", deviceName), ar -> {
        	if (ar.succeeded()) {
        		retrieveDeviceConfig(db, deviceName, res -> {
        			vertx.runOnContext(v -> {
        				if (res.succeeded()) {
        					ConfigGenerator cg = new ConfigGenerator();
        					resultHandler.handle(Future.succeededFuture(cg
        							.getDeviceConfig(res.result().getJsonObject("config"))));
        				} else {
        					resultHandler.handle(Future.failedFuture(res.cause()));
        				}
        			});	
        		});
        	} else {
        		resultHandler.handle(Future.failedFuture(ar.cause()));
        	}
        });
	}

	private void getNetwork(String db, Handler<AsyncResult<Network>> resultHandler) {
		Network network = new Network();
		find(db, CypherQuery.Api.GET_NETWORK_HOSTS, hs -> {
			if (hs.succeeded()) {
				List<Device> devices = hs.result().stream()
						.map(o -> {return new Device(o);})
						.collect(Collectors.toList());
				network.setDevices(devices);
				find(MAIN_DB, CypherQuery.Api.GET_NETWORK_LINKS, ls -> {
					if (ls.succeeded()) {
						List<Link> links = ls.result().stream()
								.map(o -> {return new Link(o);})
								.collect(Collectors.toList());
						network.setLinks(links);
						find(MAIN_DB, CypherQuery.Api.GET_NETWORK_SUBNETS, sn -> {
							if (sn.succeeded()) {
								List<IpSubnet> subnets = sn.result().stream()
										.map(o -> {return new IpSubnet(o);})
										.collect(Collectors.toList());
								network.setSubnets(subnets);
								resultHandler.handle(Future.succeededFuture(network));
							} else {
								resultHandler.handle(Future.failedFuture(sn.cause()));
							}
						});
					} else {
						resultHandler.handle(Future.failedFuture(ls.cause()));
					}
				});
			} else {
				resultHandler.handle(Future.failedFuture(hs.cause()));
			}
		});
	}
	
	private void getDevice(String db, String deviceName,
			Handler<AsyncResult<Device>> resultHandler) {
		JsonObject params = new JsonObject().put("deviceName", deviceName);
		find(db, CypherQuery.Api.GET_HOST, params, res -> {
			if (res.succeeded()) {
				if (res.result().size() == 1) {
					Device device = JsonUtils.json2Pojo(res.result().get(0).encode(), Device.class);
					resultHandler.handle(Future.succeededFuture(device));
				} else {
					resultHandler.handle(Future.succeededFuture(null));
				}
			} else {
				resultHandler.handle(Future.failedFuture(res.cause()));
			}
		});
	}
	
	private void getDeviceInterfaces(String db, String deviceName,
			Handler<AsyncResult<List<NetInterface>>> resultHandler) {
		JsonObject params = new JsonObject().put("deviceName", deviceName);
		find(db, CypherQuery.Api.GET_HOST_INTERFACES, params, res -> {
			if (res.succeeded()) {
				List<NetInterface> netItfs = res.result().stream()
						.map(o -> {return new NetInterface(o);})
						.collect(Collectors.toList());
				resultHandler.handle(Future.succeededFuture(netItfs));
			} else {
				resultHandler.handle(Future.failedFuture(res.cause()));
			}
		});
	}
	
	private void getInterface(String db, String deviceName, String itfName,
			Handler<AsyncResult<NetInterface>> resultHandler) {
		JsonObject params = new JsonObject()
				.put("deviceName", deviceName)
				.put("itfName", itfName);
		find(db, CypherQuery.Api.GET_INTERFACE, params, res -> {
			if (res.succeeded()) {
				if (res.result().size() == 1) {
					NetInterface netItf = JsonUtils.json2Pojo(res.result().get(0).encode(), NetInterface.class);
					resultHandler.handle(Future.succeededFuture(netItf));
				} else {
					resultHandler.handle(Future.succeededFuture(null));
				}
			} else {
				resultHandler.handle(Future.failedFuture(res.cause()));
			}
		});
	}
	
	private void getDeviceBgps(String db, String deviceName, Handler<AsyncResult<List<Bgp>>> resultHandler) {
		JsonObject params = new JsonObject().put("deviceName", deviceName);
		find(db, CypherQuery.Api.GET_HOST_BGPS, params, res -> {
			if (res.succeeded()) {
				List<Bgp> bgps = res.result().stream()
						.map(o -> {return new Bgp(o);})
						.collect(Collectors.toList());
				resultHandler.handle(Future.succeededFuture(bgps));
			} else {
				resultHandler.handle(Future.failedFuture(res.cause()));
			}
		});
	}
	
	private void getBgp(String db, String deviceName, String itfAddr,
			Handler<AsyncResult<Bgp>> resultHandler) {
		JsonObject params = new JsonObject()
				.put("deviceName", deviceName)
				.put("itfAddr", itfAddr);
		find(db, CypherQuery.Api.GET_BGP, params, res -> {
			if (res.succeeded()) {
				if (res.result().size() == 1) {
					Bgp bgp = JsonUtils.json2Pojo(res.result().get(0).encode(), Bgp.class);
					resultHandler.handle(Future.succeededFuture(bgp));
				} else {
					resultHandler.handle(Future.succeededFuture(null));
				}
			} else {
				resultHandler.handle(Future.failedFuture(res.cause()));
			}
		});
	}
	
	private void getDeviceVlans(String db, String deviceName, Handler<AsyncResult<List<Vlan>>> resultHandler) {
		JsonObject params = new JsonObject().put("deviceName", deviceName);
		find(db, CypherQuery.Api.GET_HOST_VLANS, params, res -> {
			if (res.succeeded()) {
				List<Vlan> vlans = res.result().stream()
						.map(o -> {return new Vlan(o);})
						.collect(Collectors.toList());
				resultHandler.handle(Future.succeededFuture(vlans));
			} else {
				resultHandler.handle(Future.failedFuture(res.cause()));
			}
		});
	}
	
	private void getVlanMembers(String db, String deviceName, String vid, Handler<AsyncResult<List<VlanMember>>> resultHandler) {
		JsonObject params = new JsonObject().put("deviceName", deviceName);
		find(db, CypherQuery.Api.GET_VLAN_MEMBERS, params, res -> {
			if (res.succeeded()) {
				List<VlanMember> vlanMembers = res.result().stream()
						.map(o -> {return new VlanMember(o);})
						.collect(Collectors.toList());
				resultHandler.handle(Future.succeededFuture(vlanMembers));
			} else {
				resultHandler.handle(Future.failedFuture(res.cause()));
			}
		});
	}
	
	private void retrieveDeviceConfig(String db, String deviceName, 
			Handler<AsyncResult<JsonObject>> resultHandler) {
		Map<String,String> queries = CypherQuery.ConfigGen.queryMap(deviceName);
		List<Future<JsonObject>> futures = new ArrayList<Future<JsonObject>>();

		for (Map.Entry<String,String> entry : queries.entrySet()) {
			Promise<JsonObject> p = Promise.promise();
			futures.add(p.future());
            find(db, entry.getValue(), res -> {
            	if (res.succeeded()) {
            		JsonObject json = new JsonObject()
            				.put("type", entry.getKey())
            				.put("value", new JsonArray(res.result()));
            		p.complete(json);
            	} else {
            		p.fail(res.cause());
            	}
            });
        }
		Functional.allOfFutures(futures).onComplete(done -> {
			vertx.runOnContext(v -> {
				if (done.succeeded()) {
					JsonObject configs = new JsonObject();
					for (JsonObject stage : done.result()) {
						configs.put(stage.getString("type"), stage.getJsonArray("value"));
					}
					JsonObject deviceConfig = new JsonObject()
        					.put("device", deviceName)
        					.put("config", configs);
					resultHandler.handle(Future.succeededFuture(deviceConfig));
				} else {
					resultHandler.handle(Future.failedFuture(done.cause()));
				}
			});	
		});
	}
	
	private void loadExampleNetwork(Handler<AsyncResult<Void>> resultHandler) {
		String stateColl = vertx.fileSystem().readFileBlocking("state-collection.json").toString();
		final TypeReference<HashMap<String,DeviceState>> typeRef 
				= new TypeReference<HashMap<String,DeviceState>>() {};
		final Map<String, DeviceState> configs 
				= JsonUtils.json2Pojo(stateColl, typeRef);
		final NetworkState netConfig = new NetworkState();
		netConfig.setConfigs(configs);
		processNetworkRunningState(netConfig, done -> {
			resultHandler.handle(Future.succeededFuture());
			if (done.succeeded()) {
				logger.info("Example network loaded");
			} else {
				logger.info("Failed to load example network: " + done.cause().getMessage());
			}
		});
	}
}