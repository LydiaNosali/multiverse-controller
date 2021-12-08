package io.nms.central.microservice.digitaltwin.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import io.nms.central.microservice.digitaltwin.model.ipnetApi.AclRule;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.AclTable;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.Arp;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.Bgp;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.Device;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.RouteHop;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.IpRoute;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.IpSubnet;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.Link;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.NetInterface;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.Network;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.Path;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.PathHop;
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
		vertx.runOnContext(v -> {
			processNetState(netState, resultHandler);
		});
		return this;
	}

	@Override
	public DigitalTwinService runningVerifyNetwork(Handler<AsyncResult<VerificationReport>> resultHandler) {
		na.verifyNetwork(MAIN_DB, resultHandler);
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
	@Override
	public DigitalTwinService runningGetAclRules(String deviceName, String tableName, 
			Handler<AsyncResult<List<AclRule>>> resultHandler) {
		JsonObject params = new JsonObject()
				.put("deviceName", deviceName)
				.put("tableName", tableName);
		find(MAIN_DB, CypherQuery.Api.GET_ACLRULES, params, res -> {
			if (res.succeeded()) {
				List<AclRule> aclRules = res.result().stream()
						.map(o -> {return new AclRule(o);})
						.collect(Collectors.toList());
				resultHandler.handle(Future.succeededFuture(aclRules));
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
			execute(MAIN_DB, CypherQuery.View.CREATE_VIEW, params, created -> {
				if (created.succeeded()) {
					logger.info("View created");
					execute(viewId, CypherQuery.View.INIT_VIEW.get(0), init1 -> {
						if (init1.succeeded()) {
							execute(viewId, CypherQuery.View.INIT_VIEW.get(1), init2 -> {
								if (init2.succeeded()) {
									logger.info("View initialized");
									String eq = CypherQuery.View.getExtractionQuery(MAIN_DB, dbUser, dbPassword);
										execute(viewId, eq, done -> {
											if (done.succeeded()) {
												logger.info("View creation done");
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
	public DigitalTwinService viewUpdateDevice(String viewId, String deviceName, Device device, 
			Handler<AsyncResult<Void>> resultHandler) {
		JsonObject params = new JsonObject()
				.put("deviceName", deviceName)
				.put("hostname", device.getHostname())
				.put("bgpStatus", device.getBgpStatus())
				.put("bgpAsn", device.getBgpAsn())
				.put("type", device.getType().getValue())
				.put("platform", device.getPlatform())
				.put("mac", device.getMac())
				.put("hwsku", device.getHwsku());
		execute(viewId, CypherQuery.Api.UPDATE_HOST, params, res -> {
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
	public DigitalTwinService viewUpdateInterface(String viewId, String deviceName, String itfName,
			NetInterface netItf, Handler<AsyncResult<Void>> resultHandler) {
		String updateQuery = CypherQuery.Api.UPDATE_INTERFACE_NOIP;
		JsonObject params = new JsonObject()
				.put("deviceName", deviceName).put("itfName", itfName)
				.put("adminStatus", netItf.getAdminStatus().getValue()).put("index", netItf.getIndex())
				.put("type", netItf.getType().getValue()).put("speed", netItf.getSpeed())
				.put("mtu", netItf.getMtu()).put("mode", netItf.getMode())
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
	public DigitalTwinService viewGetDeviceBgps(String viewId, String deviceName,
			Handler<AsyncResult<List<Bgp>>> resultHandler) {
		getDeviceBgps(viewId, deviceName, resultHandler);
		return this;
	}

	@Override
	public DigitalTwinService viewGetBgp(String viewId, String deviceName, String itfAddr,
			Handler<AsyncResult<Bgp>> resultHandler) {
		getBgp(viewId, deviceName, itfAddr, resultHandler);
		return this;
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
		execute(viewId, CypherQuery.Api.CREATE_BGP, params, res -> {
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
	public DigitalTwinService viewVerify(String viewId, 
			Handler<AsyncResult<VerificationReport>> resultHandler) {
		na.verifyNetwork(viewId, resultHandler);
		return this;
	}
	
	@Override
	public DigitalTwinService viewGenerateNetworkConfig(String viewId, 
			Handler<AsyncResult<JsonObject>> resultHandler) {
		ConfigGenerator cg = new ConfigGenerator();
		find(viewId, CypherQuery.ConfigGen.GET_DEVICES, res -> {
			if (res.succeeded()) {
				List<String> devices = res.result().stream()
						.map(d -> d.getString("device"))
						.collect(Collectors.toList());
				List<Future<JsonObject>> futures = new ArrayList<Future<JsonObject>>();
				for (String device: devices) {
					Promise<JsonObject> p = Promise.promise();
					futures.add(p.future());
					retrieveDeviceConfig(viewId, device, p);
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
						.map(o -> {return new RouteHop(o.getJsonObject("ipHop"));})
						.collect(Collectors.toList());
				resultHandler.handle(Future.succeededFuture(ipPaths));
        	} else {
        		resultHandler.handle(Future.failedFuture(res.cause()));
        	}
        });
		return this;
	}
	
	/* Processing functions */
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

	private void processNetState(NetworkState netState, 
			Handler<AsyncResult<CreationReport>> resultHandler) {
		CreationReport report = new CreationReport();
		report.setTimestamp(OffsetDateTime.now().toLocalDateTime().toString());
		
		ConfigProcessor cp = new ConfigProcessor(netState);
		if (!cp.process()) {
			resultHandler.handle(Future.failedFuture("Failed to process config"));
			return;
		}
		report.setConfigProcessor(cp.getReport());

		GraphCreator gc = new GraphCreator(cp.getOutput());
		if (!gc.process()) {
			resultHandler.handle(Future.failedFuture("Failed to create graph queries"));
			return;
		}
		report.setQueriesGenerator(gc.getReport());

		List<String> queries = new ArrayList<String>();
		queries.add(CypherQuery.CLEAR_DB);
		queries.addAll(gc.getOutput().stream().map(s -> s.split("@")[1]).collect(Collectors.toList()));
		
		Instant start = Instant.now();
		createGraph(MAIN_DB, queries, res -> {
			Instant end = Instant.now();
			if (res.succeeded()) {
				Duration timeElapsed = Duration.between(start, end);
				logger.info("Graph creation time: " + timeElapsed.getNano() / 1000000 + " ms");
				logger.info("Queries: " + queries.size());
				// logger.info("Report: " + res.result());
				report.setGraphCreator(res.result());
				resultHandler.handle(Future.succeededFuture(report));	
			} else {
				resultHandler.handle(Future.failedFuture(res.cause()));	
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
	
	private void loadExampleNetwork(Handler<AsyncResult<Void>> resultHandler) {
		InputStream is = getClass().getResourceAsStream("/config-collection.json"); 
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);
		StringBuffer sb = new StringBuffer();
		String line;
		try {
			while ((line = br.readLine()) != null) 
			{
				sb.append(line);
			}
			br.close();
			isr.close();
			is.close();
		} catch (IOException e) {
			e.printStackTrace();
			resultHandler.handle(Future.failedFuture(e.getMessage()));
			return;
		}
		final TypeReference<HashMap<String,DeviceState>> typeRef 
				= new TypeReference<HashMap<String,DeviceState>>() {};
		final Map<String, DeviceState> configs 
				= JsonUtils.json2Pojo(sb.toString(), typeRef);
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