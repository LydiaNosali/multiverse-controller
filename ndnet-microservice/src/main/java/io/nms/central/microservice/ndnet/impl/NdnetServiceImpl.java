package io.nms.central.microservice.ndnet.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.nms.central.microservice.common.functional.Functional;
import io.nms.central.microservice.ndnet.NdnetService;
import io.nms.central.microservice.ndnet.model.ConfigFace;
import io.nms.central.microservice.ndnet.model.ConfigObj;
import io.nms.central.microservice.ndnet.model.ConfigRoute;
import io.nms.central.microservice.ndnet.model.Route;
import io.nms.central.microservice.notification.model.Status.StatusEnum;
import io.nms.central.microservice.topology.model.NdnConnInfo;
import io.nms.central.microservice.topology.model.Prefix;
import io.nms.central.microservice.topology.model.Vconnection;
import io.nms.central.microservice.topology.model.Vctp;
import io.nms.central.microservice.topology.model.Vnode;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.mongo.MongoClientUpdateResult;
import io.vertx.ext.mongo.UpdateOptions;

/**
 * This verticle implements the ndnet service
 */
public class NdnetServiceImpl implements NdnetService {

	private static final Logger logger = LoggerFactory.getLogger(NdnetServiceImpl.class);
	
	private static final String COLL_CANDIDATE_CONFIG = "canconfig";
	private static final String COLL_RUNNING_CONFIG = "runconfig";
	
	private final MongoClient client;
	private final Vertx vertx;
	private Routing routing;

	public NdnetServiceImpl(Vertx vertx, JsonObject config) {
		this.vertx = vertx;
		routing = new Routing();
		this.client = MongoClient.create(vertx, config);
	}
	
	@Override
	public void initializePersistence(Handler<AsyncResult<Void>> resultHandler) {
		resultHandler.handle(Future.succeededFuture());
	}

	@Override
	public void getAllCandidateConfigs(Handler<AsyncResult<List<ConfigObj>>> resultHandler) {
		JsonObject query = new JsonObject();
		client.find(COLL_CANDIDATE_CONFIG, query, ar -> {
			if (ar.succeeded()) {
				if (ar.result() == null) {
					resultHandler.handle(Future.succeededFuture());
				} else {
					List<ConfigObj> result = ar.result().stream()
							.map(raw -> {
								raw.remove("_id");
								ConfigObj co = new ConfigObj(raw);
								return co;
							}).collect(Collectors.toList());
					resultHandler.handle(Future.succeededFuture(result));
				}
			} else {
				resultHandler.handle(Future.failedFuture(ar.cause()));
			}
		});
	}
	@Override
	public void getCandidateConfig(int nodeId, Handler<AsyncResult<ConfigObj>> resultHandler) {
		JsonObject query = new JsonObject().put("nodeId", nodeId);
		client.findOne(COLL_CANDIDATE_CONFIG, query, null, ar -> {
			if (ar.succeeded()) {
				if (ar.result() == null) {
					resultHandler.handle(Future.succeededFuture());
				} else {
					ConfigObj result = new ConfigObj(ar.result());
					resultHandler.handle(Future.succeededFuture(result));
				}
			} else {
				resultHandler.handle(Future.failedFuture(ar.cause()));
			}
		});
	}
	@Override
	public void removeCandidateConfig(int nodeId, Handler<AsyncResult<Void>> resultHandler) {
		JsonObject query = new JsonObject().put("nodeId", nodeId);
		client.removeDocuments(COLL_CANDIDATE_CONFIG, query, ar -> {
			if (ar.succeeded()) {
				resultHandler.handle(Future.succeededFuture());
			} else {
				resultHandler.handle(Future.failedFuture(ar.cause()));
			}
		});
	}
	
	@Override
	public void getAllRunningConfigs(Handler<AsyncResult<List<ConfigObj>>> resultHandler) {
		JsonObject query = new JsonObject();
		client.find(COLL_RUNNING_CONFIG, query, ar -> {
			if (ar.succeeded()) {
				if (ar.result() == null) {
					resultHandler.handle(Future.succeededFuture());
				} else {
					List<ConfigObj> result = ar.result().stream()
							.map(raw -> {
								raw.remove("_id");
								ConfigObj co = new ConfigObj(raw);
								return co;
							}).collect(Collectors.toList());
					resultHandler.handle(Future.succeededFuture(result));
				}
			} else {
				resultHandler.handle(Future.failedFuture(ar.cause()));
			}
		});
	}
	@Override
	public void getRunningConfig(int nodeId, Handler<AsyncResult<ConfigObj>> resultHandler) {
		JsonObject query = new JsonObject().put("nodeId", nodeId);
		client.findOne(COLL_RUNNING_CONFIG, query, null, ar -> {
			if (ar.succeeded()) {
				if (ar.result() == null) {
					resultHandler.handle(Future.succeededFuture());
				} else {
					ConfigObj result = new ConfigObj(ar.result());
					resultHandler.handle(Future.succeededFuture(result));
				}
			} else {
				resultHandler.handle(Future.failedFuture(ar.cause()));
			}
		});
	}
	@Override
	public void upsertRunningConfig(int nodeId, ConfigObj config, Handler<AsyncResult<Void>> resultHandler) {
		config.setNodeId(nodeId);
		JsonObject query = new JsonObject().put("nodeId", nodeId);
		UpdateOptions opts = new UpdateOptions().setUpsert(true);
		JsonObject upd = new JsonObject().put("$set", config.toJson());
		client.updateCollectionWithOptions(COLL_RUNNING_CONFIG, query, upd, opts, ar -> {
			if (ar.succeeded()) {
				resultHandler.handle(Future.succeededFuture());
			} else {
				resultHandler.handle(Future.failedFuture(ar.cause()));
			}
		});
	}
	@Override
	public void updateRunningConfig(int nodeId, JsonArray patch, Handler<AsyncResult<Void>> resultHandler) {
		JsonObject query = new JsonObject().put("nodeId", nodeId);
		JsonObject fields = new JsonObject().put("_id", 0);
		client.findOne(COLL_RUNNING_CONFIG, query, fields, ar -> {
			if (ar.succeeded()) {
				JsonObject jConfig;
				if (ar.result() == null) {
					ConfigObj cg = new ConfigObj();
					cg.setNodeId(nodeId);
					jConfig = cg.toJson();
				} else {
					jConfig = ar.result();
				}
				try {
					JsonObject uDoc = Functional.computePatched(jConfig, patch);
					uDoc.put("nodeId", nodeId);
					client.replaceDocuments(COLL_RUNNING_CONFIG, query, uDoc, done -> {
						if (done.succeeded()) {
							resultHandler.handle(Future.succeededFuture());
						} else {
							resultHandler.handle(Future.failedFuture(ar.cause()));
						}
					});
				} catch (IllegalArgumentException e) {
					resultHandler.handle(Future.failedFuture(e.getMessage()));
				}
			} else {
				resultHandler.handle(Future.failedFuture(ar.cause()));
			}
		});
	}
	@Override
	public void removeRunningConfig(int nodeId, Handler<AsyncResult<Void>> resultHandler) {
		JsonObject query = new JsonObject().put("nodeId", nodeId);
		client.removeDocuments(COLL_RUNNING_CONFIG, query, ar -> {
			if (ar.succeeded()) {
				resultHandler.handle(Future.succeededFuture());
			} else {
				resultHandler.handle(Future.failedFuture(ar.cause()));
			}
		});
	}

	@Override
	public void computeConfigurations(List<Vnode> nodes, List<Vconnection> edges, List<Vctp> faces, 
			List<Prefix> pas, Handler<AsyncResult<List<ConfigObj>>> resultHandler) {
		
		// keep active nodes only
		List<Vnode> nodesUP = new ArrayList<Vnode>();
		for (Vnode e: nodes) {
			if (!e.getStatus().equals(StatusEnum.DOWN)) {
				nodesUP.add(e);
			}
		}

		// keep active edges only
		List<Vconnection> edgesUP = new ArrayList<Vconnection>();
		for (Vconnection e: edges) {
			if (!e.getStatus().equals(StatusEnum.DOWN)) {
				edgesUP.add(e);
			}
		}

		// keep active PAs only
		List<Prefix> pasUP = new ArrayList<Prefix>();
		for (Prefix e: pas) {
			if (e.getAvailable()) {
				pasUP.add(e);
			}
		}

		computeRoutes(nodesUP, edgesUP, pasUP, res -> {
			if (res.succeeded()) {
				List<Route> routes = res.result();

				Map<Integer,ConfigObj> configsMap = new HashMap<Integer,ConfigObj>();
				for (Vnode node : nodes) {
					ConfigObj c = new ConfigObj();
					c.setNodeId(node.getId());
					configsMap.put(node.getId(), c);
				}
		
				for (Vctp face : faces) {
					int nodeId = face.getVnodeId();
					if (!face.getStatus().equals(StatusEnum.DOWN)) {
						ConfigFace cFace = new ConfigFace();
						cFace.setId(face.getId());
						cFace.setLocal(((NdnConnInfo) face.getConnInfo()).getLocal());
						cFace.setRemote(((NdnConnInfo) face.getConnInfo()).getRemote());
						cFace.setScheme(((NdnConnInfo) face.getConnInfo()).getScheme());
						configsMap.get(nodeId).getConfig().addFace(cFace);
					}
				}
				for (Route route : routes) {
					int nodeId = route.getNodeId();
					int faceId = route.getFaceId();
					if (configsMap.get(nodeId).getConfig().hasFaceId(faceId)){
						ConfigRoute cRoute = new ConfigRoute();
						cRoute.setPrefix(route.getPrefix());
						cRoute.setFaceId(faceId);
						cRoute.setCost(route.getCost());
						cRoute.setOrigin(route.getOrigin());
						configsMap.get(nodeId).getConfig().addRoute(cRoute);
					} else {
						logger.warn("Route references nonexistent FaceId: " + faceId);
					}
				}
				List<ConfigObj> config = new ArrayList<ConfigObj>(configsMap.values());
				resultHandler.handle(Future.succeededFuture(config));
			} else {
				resultHandler.handle(Future.failedFuture(res.cause()));
			}
		});
	}

	@Override
	public void computeRoutes(List<Vnode> nodes, List<Vconnection> edges, List<Prefix> pas, 
			Handler<AsyncResult<List<Route>>> resultHandler) {
		routing.computeRoutes(nodes, edges, pas).onComplete(resultHandler);
	}

	@Override
	public void upsertCandidateConfigs(List<ConfigObj> configs, Handler<AsyncResult<Void>> resultHandler) {
		List<Future> fts = new ArrayList<Future>();
		for (ConfigObj cg : configs) {
			JsonObject query = new JsonObject().put("nodeId", cg.getNodeId());
			JsonObject fields = new JsonObject().put("_id", 0);
			client.findOne(COLL_CANDIDATE_CONFIG, query, fields, ar -> {
				if (ar.succeeded()) {
					if (ar.result() != null) {
						ConfigObj currConfig = new ConfigObj(ar.result());
						if (!currConfig.equals(cg)) {
							Promise<MongoClientUpdateResult> p = Promise.promise();
							fts.add(p.future());
							client.replaceDocuments(COLL_CANDIDATE_CONFIG, query, cg.toJson(), p);
						}
					} else {
						Promise<String> p = Promise.promise();
						fts.add(p.future());
						client.save(COLL_CANDIDATE_CONFIG, cg.toJson(), p);
					}
				} else {
					resultHandler.handle(Future.failedFuture(ar.cause()));
				}
			});
		}
		CompositeFuture.all(fts).map((Void) null).onComplete(resultHandler);
	}
}

