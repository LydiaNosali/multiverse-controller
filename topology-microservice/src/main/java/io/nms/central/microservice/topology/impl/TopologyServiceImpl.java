package io.nms.central.microservice.topology.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.nms.central.microservice.common.functional.JSONUtils;
import io.nms.central.microservice.common.service.JdbcRepositoryWrapper;
import io.nms.central.microservice.notification.model.Status.StatusEnum;
import io.nms.central.microservice.topology.TopologyService;
import io.nms.central.microservice.topology.model.CrossConnect;
import io.nms.central.microservice.topology.model.EtherConnInfo;
import io.nms.central.microservice.topology.model.NdnConnInfo;
import io.nms.central.microservice.topology.model.Prefix;
import io.nms.central.microservice.topology.model.Vconnection;
import io.nms.central.microservice.topology.model.Vctp;
import io.nms.central.microservice.topology.model.Vctp.ConnTypeEnum;
import io.nms.central.microservice.topology.model.Vnode.NodeTypeEnum;
import io.nms.central.microservice.topology.model.Vlink;
import io.nms.central.microservice.topology.model.VlinkConn;
import io.nms.central.microservice.topology.model.Vltp;
import io.nms.central.microservice.topology.model.Vnode;
import io.nms.central.microservice.topology.model.Vsubnet;
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

/**
 *
 */
public class TopologyServiceImpl extends JdbcRepositoryWrapper implements TopologyService {

	private static final Logger logger = LoggerFactory.getLogger(TopologyServiceImpl.class);

	public TopologyServiceImpl(Vertx vertx, JsonObject config) {
		super(vertx, config);
	}

	@Override
	public TopologyService initializePersistence(Handler<AsyncResult<Void>> resultHandler) {
		List<String> statements = new ArrayList<String>();
		statements.add(ApiSql.CREATE_TABLE_VSUBNET);
		statements.add(ApiSql.CREATE_TABLE_VNODE);
		statements.add(ApiSql.CREATE_TABLE_VLTP);
		statements.add(ApiSql.CREATE_TABLE_VCTP);
		statements.add(ApiSql.CREATE_TABLE_VLINK);
		statements.add(ApiSql.CREATE_TABLE_VLINKCONN);
		statements.add(ApiSql.CREATE_TABLE_VCONNECTION);
		statements.add(ApiSql.CREATE_TABLE_PREFIX);
		statements.add(ApiSql.CREATE_TABLE_CROSS_CONNECTS);
		client.getConnection(conn -> {
			if (conn.succeeded()) {
				conn.result().batch(statements, r -> {
					conn.result().close();
					if (r.succeeded()) {
						initializeStatus(resultHandler);
					} else {
						logger.error("sql error: " + r.cause().getMessage());
						resultHandler.handle(Future.failedFuture(r.cause()));
					}
				});
			} else {
				resultHandler.handle(Future.failedFuture(conn.cause()));
			}
		});
		return this;
	}

	private void initializeStatus(Handler<AsyncResult<Void>> resultHandler) {
		List<String> statements = new ArrayList<String>();
		statements.add(InternalSql.INIT_NODE_STATUS);
		statements.add(InternalSql.INIT_LTP_STATUS);
		statements.add(InternalSql.INIT_CTP_STATUS);
		statements.add(InternalSql.INIT_LINK_STATUS);
		statements.add(InternalSql.INIT_LC_STATUS);
		statements.add(InternalSql.INIT_CONNECTION_STATUS);
		statements.add(InternalSql.INIT_PREFIX_STATUS);
		client.getConnection(conn -> {
			if (conn.succeeded()) {
				conn.result().batch(statements, r -> {
					conn.result().close();
					if (r.succeeded()) {
						resultHandler.handle(Future.succeededFuture());
					} else {
						resultHandler.handle(Future.failedFuture(r.cause()));
					}
				});
			} else {
				resultHandler.handle(Future.failedFuture(conn.cause()));
			}
		});
	}

	/********** Vsubnet **********/
	@Override
	public TopologyService addVsubnet(Vsubnet vsubnet, Handler<AsyncResult<Integer>> resultHandler) {
		JsonArray params = new JsonArray()
				.add(vsubnet.getName())
				.add(vsubnet.getLabel())
				.add(vsubnet.getDescription())
				.add(JSONUtils.pojo2Json(vsubnet.getInfo(), false));
		insert(ApiSql.INSERT_VSUBNET, params, resultHandler);
		return this;
	}

	@Override
	public TopologyService getVsubnet(String vsubnetId, Handler<AsyncResult<Vsubnet>> resultHandler) {
		this.findOne(ApiSql.FETCH_VSUBNET_BY_ID, new JsonArray().add(vsubnetId)).map(json -> {
			return JSONUtils.json2Pojo(json.encode(), Vsubnet.class);
		}).onComplete(resultHandler);
		return this;
	}

	@Override
	public TopologyService getAllVsubnets(Handler<AsyncResult<List<Vsubnet>>> resultHandler) {
		this.find(ApiSql.FETCH_ALL_VSUBNETS).map(rawList -> rawList.stream().map(row -> {
			return JSONUtils.json2Pojo(row.encode(), Vsubnet.class);
		}).collect(Collectors.toList())).onComplete(resultHandler);
		return this;
	}

	@Override
	public TopologyService deleteVsubnet(String vsubnetId, Handler<AsyncResult<Void>> resultHandler) {
		this.delete(ApiSql.DELETE_VSUBNET, new JsonArray().add(vsubnetId), resultHandler);
		return this;
	}

	@Override
	public TopologyService updateVsubnet(String id, Vsubnet vsubnet, Handler<AsyncResult<Void>> resultHandler) {
		JsonArray params = new JsonArray()
				.add(vsubnet.getLabel())
				.add(vsubnet.getDescription())
				.add(JSONUtils.pojo2Json(vsubnet.getInfo(), false))
				.add(id);
		execute(ApiSql.UPDATE_VSUBNET, params, resultHandler);
		return this;
	}

	/********** Vnode **********/
	@Override
	public TopologyService addVnode(Vnode vnode, Handler<AsyncResult<Integer>> resultHandler) {
		String macAddr = validateAndConvertMAC(vnode.getHwaddr());
		if (macAddr.isEmpty()) {
				resultHandler.handle(Future.failedFuture("MAC address not valid"));
				return this;
		}
		vnode.setHwaddr(macAddr);

		if (!checkCidrIp(vnode.getMgmtIp())) {
			resultHandler.handle(Future.failedFuture("IP address not valid"));
			return this;
		}
		JsonArray params = new JsonArray()
				.add(vnode.getName())
				.add(vnode.getLabel())
				.add(vnode.getDescription())
				.add(JSONUtils.pojo2Json(vnode.getInfo(), false))
				.add(vnode.getStatus().getValue())
				.add(vnode.getPosx())
				.add(vnode.getPosy())
				.add(vnode.getLocation())
				.add(vnode.getType())
				.add(vnode.getVsubnetId())
				.add(vnode.getHwaddr())
				.add(vnode.getMgmtIp());
		insert(ApiSql.INSERT_VNODE, params, resultHandler);
		return this;
	}

	@Override
	public TopologyService getVnode(String vnodeId, Handler<AsyncResult<Vnode>> resultHandler) {
		this.findOne(ApiSql.FETCH_VNODE_BY_ID, new JsonArray().add(vnodeId)).map(json -> {
			return JSONUtils.json2Pojo(json.encode(), Vnode.class);
		}).onComplete(resultHandler);
		return this;
	}

	@Override
	public TopologyService getAllVnodes(Handler<AsyncResult<List<Vnode>>> resultHandler) {
		this.find(ApiSql.FETCH_ALL_VNODES).map(rawList -> rawList.stream().map(row -> {
			return JSONUtils.json2Pojo(row.encode(), Vnode.class);
		}).collect(Collectors.toList())).onComplete(resultHandler);
		return this;
	}

	@Override
	public TopologyService getVnodesByVsubnet(String vsubnetId, Handler<AsyncResult<List<Vnode>>> resultHandler) {
		JsonArray params = new JsonArray().add(vsubnetId);
		this.find(ApiSql.FETCH_VNODES_BY_VSUBNET, params).map(rawList -> rawList.stream().map(row -> {
			return JSONUtils.json2Pojo(row.encode(), Vnode.class);
		}).collect(Collectors.toList())).onComplete(resultHandler);
		return this;
	}

	@Override
	public TopologyService getVnodesByType(NodeTypeEnum type, Handler<AsyncResult<List<Vnode>>> resultHandler) {
		JsonArray params = new JsonArray().add(type.getValue());
		this.find(ApiSql.FETCH_VNODES_BY_TYPE, params).map(rawList -> rawList.stream().map(row -> {
			return JSONUtils.json2Pojo(row.encode(), Vnode.class);
		}).collect(Collectors.toList())).onComplete(resultHandler);
		return this;
	}

	@Override
	public TopologyService deleteVnode(String vnodeId, Handler<AsyncResult<Void>> resultHandler) {
		Promise<Void> vltpsDeleted = Promise.promise();
		JsonArray pVnodeId = new JsonArray().add(vnodeId);

		UUID op = UUID.randomUUID();
		beginTransaction(Entity.NODE, op, InternalSql.LOCK_TABLES_FOR_NODE).onComplete(ar -> {
			if (ar.succeeded()) {
				transactionFind(ApiSql.FETCH_VLTPS_BY_VNODE, pVnodeId).onComplete(res -> {
					if (res.succeeded()) {
						Vltp[] vltps = JSONUtils.json2Pojo(new JsonArray(res.result()).encode(), Vltp[].class);
						List<Future> futures = new ArrayList<>();
						for (Vltp vltp : vltps) {
							Promise<Void> p = Promise.promise();
							futures.add(p.future());
							deleteVltp(op, vltp.getId(), p);
						}
						CompositeFuture.all(futures).map((Void) null).onComplete(vltpsDeleted);
					} else {
						resultHandler.handle(Future.failedFuture(res.cause()));
					}
				});
			} else {
				resultHandler.handle(Future.failedFuture(ar.cause()));
			}
		});

		vltpsDeleted.future().onComplete(ar -> {
			if (ar.succeeded()) {
				transactionExecute(ApiSql.DELETE_VNODE, pVnodeId)
						.compose(r -> commitTransaction(Entity.NODE, op))
						.onComplete(resultHandler);
			} else {
				resultHandler.handle(Future.failedFuture(ar.cause()));
			}
		});
		return this;
	}

	@Override
	public TopologyService updateVnode(String id, Vnode vnode, Handler<AsyncResult<Void>> resultHandler) {
		String macAddr = validateAndConvertMAC(vnode.getHwaddr());
		if (macAddr.isEmpty()) {
				resultHandler.handle(Future.failedFuture("MAC address not valid"));
				return this;
		}
		vnode.setHwaddr(macAddr);

		if (!checkCidrIp(vnode.getMgmtIp())) {
			resultHandler.handle(Future.failedFuture("IP address not valid"));
			return this;
		}
		
		JsonArray params = new JsonArray()
				.add(vnode.getLabel())
				.add(vnode.getDescription())
				.add(JSONUtils.pojo2Json(vnode.getInfo(), false))
				.add(vnode.getStatus().getValue())
				.add(vnode.getPosx()).add(vnode.getPosy())
				.add(vnode.getLocation())
				.add(vnode.getHwaddr())
				.add(vnode.getMgmtIp())
				.add(id);
		execute(ApiSql.UPDATE_VNODE, params, resultHandler);
		return this;
	}

	/********** Vltp **********/
	@Override
	public TopologyService addVltp(Vltp vltp, Handler<AsyncResult<Integer>> resultHandler) {
		JsonArray params = new JsonArray()
				.add(vltp.getName())
				.add(vltp.getLabel())
				.add(vltp.getDescription())
				.add(JSONUtils.pojo2Json(vltp.getInfo(), false))
				.add(vltp.getStatus().getValue())
				.add(vltp.getVnodeId())
				.add(vltp.getPort())
				.add(vltp.getBandwidth())
				.add(vltp.getMtu())
				.add(vltp.isBusy());
		insert(ApiSql.INSERT_VLTP, params, resultHandler);
		return this;
	}

	@Override
	public TopologyService getVltp(String vltpId, Handler<AsyncResult<Vltp>> resultHandler) {
		findOne(ApiSql.FETCH_VLTP_BY_ID, new JsonArray().add(vltpId)).map(json -> {
			return JSONUtils.json2Pojo(json.encode(), Vltp.class);
		}).onComplete(resultHandler);
		return this;
	}

	@Override
	public TopologyService getAllVltps(Handler<AsyncResult<List<Vltp>>> resultHandler) {
		find(ApiSql.FETCH_ALL_VLTPS).map(rawList -> rawList.stream().map(row -> {
			return JSONUtils.json2Pojo(row.encode(), Vltp.class);
		}).collect(Collectors.toList())).onComplete(resultHandler);
		return this;
	}

	@Override
	public TopologyService getVltpsByVnode(String vnodeId, Handler<AsyncResult<List<Vltp>>> resultHandler) {
		JsonArray params = new JsonArray().add(vnodeId);
		find(ApiSql.FETCH_VLTPS_BY_VNODE, params).map(rawList -> rawList.stream().map(row -> {
			return JSONUtils.json2Pojo(row.encode(), Vltp.class);
		}).collect(Collectors.toList())).onComplete(resultHandler);
		return this;
	}

	@Override
	public TopologyService deleteVltp(String vltpId, Handler<AsyncResult<Void>> resultHandler) {
		final UUID op = UUID.randomUUID();
		deleteVltp(op, Integer.valueOf(vltpId), resultHandler);
		return this;
	}

	@Override
	public TopologyService updateVltp(String id, Vltp vltp, Handler<AsyncResult<Void>> resultHandler) {
		JsonArray params = new JsonArray()
				.add(vltp.getLabel())
				.add(vltp.getDescription())
				.add(JSONUtils.pojo2Json(vltp.getInfo(), false))
				.add(vltp.getStatus().getValue())
				.add(vltp.isBusy())
				.add(id);
		execute(ApiSql.UPDATE_VLTP, params, resultHandler);
		return this;
	}

	private void deleteVltp(UUID op, int vltpId, Handler<AsyncResult<Void>> resultHandler) {
		Promise<Void> linkDeleted = Promise.promise();
		JsonArray pVltpId = new JsonArray().add(vltpId);
		beginTransaction(Entity.LTP, op, InternalSql.LOCK_TABLES_FOR_LTP).onComplete(ar -> {
			if (ar.succeeded()) {
				transactionFind(InternalSql.FETCH_LINK_BY_LTP, pVltpId).onComplete(res -> {
					if (res.succeeded()) {
						if (!res.result().isEmpty()) {
							Vlink vlink = JSONUtils.json2Pojo(res.result().get(0).encode(), Vlink.class);
							deleteVlink(op, vlink.getId(), linkDeleted);
						} else {
							linkDeleted.complete();
						}
					} else {
						resultHandler.handle(Future.failedFuture(res.cause()));
					}
				});
			} else {
				resultHandler.handle(Future.failedFuture(ar.cause()));
			}
		});

		linkDeleted.future().onComplete(ar -> {
			if (ar.succeeded()) {
				transactionExecute(ApiSql.DELETE_VLTP, pVltpId)
						.compose(r -> commitTransaction(Entity.LTP, op))
						.onComplete(resultHandler);
			} else {
				resultHandler.handle(Future.failedFuture(ar.cause()));
			}
		});
	}

	/********** Vctp **********/
	@Override
	public TopologyService addVctp(Vctp vctp, Handler<AsyncResult<Integer>> resultHandler) {
		ConnTypeEnum ct = vctp.getConnType();
		if (ct.equals(ConnTypeEnum.Ether)) {
			String macAddr = validateAndConvertMAC(((EtherConnInfo)vctp.getConnInfo()).getAddress());
			if (macAddr.isEmpty()) {
				resultHandler.handle(Future.failedFuture("MAC address not valid"));
				return this;
			}
			((EtherConnInfo)vctp.getConnInfo()).setAddress(macAddr);
		}
		if (ct.equals(ConnTypeEnum.NDN)) {
			String lMacAddr = validateAndConvertMAC(((NdnConnInfo) vctp.getConnInfo()).getLocal());
			String rMacAddr = validateAndConvertMAC(((NdnConnInfo) vctp.getConnInfo()).getRemote());
			if (lMacAddr.isEmpty() || rMacAddr.isEmpty()){
				resultHandler.handle(Future.failedFuture("MAC address not valid"));
				return this;
			}
			((NdnConnInfo) vctp.getConnInfo()).setLocal(lMacAddr);
			((NdnConnInfo) vctp.getConnInfo()).setRemote(rMacAddr);
		}

		String queryNode;
		String queryInsert;
		JsonArray pQueryNode = new JsonArray().add(vctp.getParentId());

		if (vctp.getConnType().equals(ConnTypeEnum.Ether)) {
			findOne(ApiSql.FETCH_VLTP_BY_ID, pQueryNode).onComplete(ar -> {
				if (ar.succeeded()) {
					// int nodeId = ar.result().get().getInteger("vnodeId");
					Vltp parentLtp = JSONUtils.json2Pojo(ar.result().encode(), Vltp.class);
					JsonArray params = new JsonArray()
							.add(vctp.getName())
							.add(vctp.getLabel())
							.add(vctp.getDescription())
							.add(JSONUtils.pojo2Json(vctp.getInfo(), false))
							.add(vctp.getConnType())
							.add(JSONUtils.pojo2Json(vctp.getConnInfo(), false))
							.add(vctp.getStatus().getValue())
							.add(vctp.getParentId())
							.add(parentLtp.getVnodeId());
					insert(ApiSql.INSERT_VCTP_VLTP, params, resultHandler);
				} else {
					resultHandler.handle(Future.failedFuture(ar.cause()));
				}
			});
			// queryNode = ApiSql.FETCH_VLTP_BY_ID;
			// queryInsert = ApiSql.INSERT_VCTP_VLTP;
		} else {
			findOne(ApiSql.FETCH_VCTP_BY_ID, pQueryNode).onComplete(ar -> {
				if (ar.succeeded()) {
					// int nodeId = ar.result().get().getInteger("vnodeId");
					Vctp parentCtp = JSONUtils.json2Pojo(ar.result().encode(), Vctp.class);
					JsonArray params = new JsonArray()
							.add(vctp.getName())
							.add(vctp.getLabel())
							.add(vctp.getDescription())
							.add(JSONUtils.pojo2Json(vctp.getInfo(), false))
							.add(vctp.getConnType())
							.add(JSONUtils.pojo2Json(vctp.getConnInfo(), false))
							.add(vctp.getStatus().getValue())
							.add(vctp.getParentId())
							.add(parentCtp.getVnodeId());
					insert(ApiSql.INSERT_VCTP_VLTP, params, resultHandler);
				} else {
					resultHandler.handle(Future.failedFuture(ar.cause()));
				}
			});
			// queryNode = ApiSql.FETCH_VCTP_BY_ID;
			// queryInsert = ApiSql.INSERT_VCTP_VCTP;
		}
		return this;
	}

	@Override
	public TopologyService getVctp(String vctpId, Handler<AsyncResult<Vctp>> resultHandler) {
		findOne(ApiSql.FETCH_VCTP_BY_ID, new JsonArray().add(vctpId)).map(json -> {
			if (json.getInteger("vltpId") == null) {
				json.remove("vltpId");
			}
			if (json.getInteger("vctpId") == null) {
				json.remove("vctpId");
			}
			return JSONUtils.json2Pojo(json.encode(), Vctp.class);
		}).onComplete(resultHandler);
		return this;
	}

	@Override
	public TopologyService getAllVctps(Handler<AsyncResult<List<Vctp>>> resultHandler) {
		find(ApiSql.FETCH_ALL_VCTPS).map(rawList -> rawList.stream().map(row -> {
			if (row.getInteger("vltpId") == null) {
				row.remove("vltpId");
			}
			if (row.getInteger("vctpId") == null) {
				row.remove("vctpId");
			}
			return JSONUtils.json2Pojo(row.encode(), Vctp.class);
		}).collect(Collectors.toList())).onComplete(resultHandler);
		return this;
	}

	@Override
	public TopologyService getVctpsByType(ConnTypeEnum type, Handler<AsyncResult<List<Vctp>>> resultHandler) {
		JsonArray params = new JsonArray().add(type.getValue());
		find(ApiSql.FETCH_VCTPS_BY_TYPE, params).map(rawList -> rawList.stream().map(row -> {
			if (row.getInteger("vltpId") == null) {
				row.remove("vltpId");
			}
			if (row.getInteger("vctpId") == null) {
				row.remove("vctpId");
			}
			return JSONUtils.json2Pojo(row.encode(), Vctp.class);
		}).collect(Collectors.toList())).onComplete(resultHandler);
		return this;
	}

	@Override
	public TopologyService getVctpsByVltp(String vltpId, Handler<AsyncResult<List<Vctp>>> resultHandler) {
		JsonArray params = new JsonArray().add(vltpId);
		find(ApiSql.FETCH_VCTPS_BY_VLTP, params).map(rawList -> rawList.stream().map(row -> {
			row.remove("vctpId");
			return JSONUtils.json2Pojo(row.encode(), Vctp.class);
		}).collect(Collectors.toList())).onComplete(resultHandler);
		return this;
	}

	@Override
	public TopologyService getVctpsByVctp(String vctpId, Handler<AsyncResult<List<Vctp>>> resultHandler) {
		JsonArray params = new JsonArray().add(vctpId);
		find(ApiSql.FETCH_VCTPS_BY_VCTP, params).map(rawList -> rawList.stream().map(row -> {
			row.remove("vltpId");
			return JSONUtils.json2Pojo(row.encode(), Vctp.class);
		}).collect(Collectors.toList())).onComplete(resultHandler);
		return this;
	}

	@Override
	public TopologyService getVctpsByVnode(String vnodeId, Handler<AsyncResult<List<Vctp>>> resultHandler) {
		JsonArray params = new JsonArray().add(vnodeId);
		find(ApiSql.FETCH_VCTPS_BY_VNODE, params).map(rawList -> rawList.stream().map(row -> {
			if (row.getInteger("vltpId") == null) {
				row.remove("vltpId");
			}
			if (row.getInteger("vctpId") == null) {
				row.remove("vctpId");
			}
			return JSONUtils.json2Pojo(row.encode(), Vctp.class);
		}).collect(Collectors.toList())).onComplete(resultHandler);
		return this;
	}

	@Override
	public TopologyService deleteVctp(String vctpId, Handler<AsyncResult<Void>> resultHandler) {
		delete(ApiSql.DELETE_VCTP, new JsonArray().add(vctpId), resultHandler);
		return this;
	}

	@Override
	public TopologyService updateVctp(String id, Vctp vctp, Handler<AsyncResult<Void>> resultHandler) {
		JsonArray params = new JsonArray()
				.add(vctp.getLabel())
				.add(vctp.getDescription())
				.add(JSONUtils.pojo2Json(vctp.getInfo(), false))
				.add(vctp.getStatus().getValue())
				.add(id);
		execute(ApiSql.UPDATE_VCTP, params, resultHandler);
		return this;
	}

	/********** Vlink **********/
	@Override
	public TopologyService addVlink(Vlink vlink, Handler<AsyncResult<Integer>> resultHandler) {
		JsonArray pVlink = new JsonArray()
				.add(vlink.getName())
				.add(vlink.getLabel())
				.add(vlink.getDescription())
				.add(JSONUtils.pojo2Json(vlink.getInfo(), false))
				.add(vlink.getStatus().getValue())
				.add(vlink.getSrcVltpId())
				.add(vlink.getDestVltpId());
		JsonArray updSrcLtp = new JsonArray().add(true).add(vlink.getSrcVltpId());
		JsonArray updDestLtp = new JsonArray().add(true).add(vlink.getDestVltpId());

		UUID opp = UUID.randomUUID();
		beginTransaction(Entity.LINK, opp, InternalSql.LOCK_TABLES_FOR_LINK).onComplete(ar -> {
			if (ar.succeeded()) {
				transactionInsert(ApiSql.INSERT_VLINK, pVlink).onComplete(res -> {
					if (res.succeeded()) {
						Future<Void> trx = transactionExecute(InternalSql.UPDATE_LTP_BUSY, updSrcLtp);
						trx
							.compose(r -> transactionExecute(InternalSql.UPDATE_LTP_BUSY, updDestLtp))
							.compose(r -> commitTransaction(Entity.LINK, opp))
							.onComplete(z -> {
								if (z.succeeded()) {
									resultHandler.handle(Future.succeededFuture(res.result()));
								} else {
									resultHandler.handle(Future.failedFuture(z.cause()));
								}
							});
					} else {
						resultHandler.handle(Future.failedFuture(res.cause()));
					}
				});
			} else {
				resultHandler.handle(Future.failedFuture(ar.cause()));
			}
		});
		return this;
	}

	@Override
	public TopologyService getVlink(String vlinkId, Handler<AsyncResult<Vlink>> resultHandler) {
		findOne(ApiSql.FETCH_VLINK_BY_ID, new JsonArray().add(vlinkId)).map(json -> {
			return JSONUtils.json2Pojo(json.encode(), Vlink.class);
		}).onComplete(resultHandler);
		return this;
	}

	@Override
	public TopologyService getAllVlinks(Handler<AsyncResult<List<Vlink>>> resultHandler) {
		find(ApiSql.FETCH_ALL_VLINKS).map(rawList -> rawList.stream().map(row -> {
			return JSONUtils.json2Pojo(row.encode(), Vlink.class);
		}).collect(Collectors.toList())).onComplete(resultHandler);
		return this;
	}

	@Override
	public TopologyService getVlinksByVsubnet(String vsubnetId, Handler<AsyncResult<List<Vlink>>> resultHandler) {
		JsonArray params = new JsonArray().add(vsubnetId);
		find(ApiSql.FETCH_VLINKS_BY_VSUBNET, params).map(rawList -> rawList.stream().map(row -> {
			return JSONUtils.json2Pojo(row.encode(), Vlink.class);
		}).collect(Collectors.toList())).onComplete(resultHandler);
		return this;
	}

	@Override
	public TopologyService deleteVlink(String vlinkId, Handler<AsyncResult<Void>> resultHandler) {
		final UUID op = UUID.randomUUID();
		deleteVlink(op, Integer.valueOf(vlinkId), resultHandler);
		return this;
	}

	@Override
	public TopologyService updateVlink(String id, Vlink vlink, Handler<AsyncResult<Void>> resultHandler) {
		JsonArray params = new JsonArray()
				.add(vlink.getLabel())
				.add(vlink.getDescription())
				.add(JSONUtils.pojo2Json(vlink.getInfo(), false))
				.add(vlink.getStatus().getValue())
				.add(id);
		execute(ApiSql.UPDATE_VLINK, params, resultHandler);
		return this;
	}

	public void deleteVlink(UUID op, int vlinkId, Handler<AsyncResult<Void>> resultHandler) {
		JsonArray pVlinkId = new JsonArray().add(vlinkId);
		beginTransaction(Entity.LINK, op, InternalSql.LOCK_TABLES_FOR_LINK).onComplete(ar -> {
			if (ar.succeeded()) {
				transactionFindOne(ApiSql.FETCH_VLINK_BY_ID, pVlinkId)
						.onComplete(res -> {
							if (res.succeeded()) {
								Vlink vlink = JSONUtils.json2Pojo(res.result().encode(), Vlink.class);
								JsonArray pSrcLtp = new JsonArray().add(false).add(vlink.getSrcVltpId());
								JsonArray pDestLtp = new JsonArray().add(false).add(vlink.getDestVltpId());

								transactionExecute(ApiSql.DELETE_VLINK, pVlinkId)
										.compose(r -> transactionExecute(InternalSql.UPDATE_LTP_BUSY, pSrcLtp))
										.compose(r -> transactionExecute(InternalSql.UPDATE_LTP_BUSY, pDestLtp))
										.compose(r -> commitTransaction(Entity.LINK, op))
										.onComplete(resultHandler);
							} else {
								resultHandler.handle(Future.failedFuture(res.cause()));
							}
						});
			} else {
				resultHandler.handle(Future.failedFuture(ar.cause()));
			}
		});
	}

	/********** VlinkConn **********/
	@Override
	public TopologyService addVlinkConn(VlinkConn vlinkConn, Handler<AsyncResult<Integer>> resultHandler) {
		// TODO: check CTPs type
		JsonArray params = new JsonArray()
				.add(vlinkConn.getName())
				.add(vlinkConn.getLabel())
				.add(vlinkConn.getDescription())
				.add(JSONUtils.pojo2Json(vlinkConn.getInfo(), false))
				.add(vlinkConn.getStatus().getValue())
				.add(vlinkConn.getSrcVctpId())
				.add(vlinkConn.getDestVctpId())
				.add(vlinkConn.getVlinkId());
		insert(ApiSql.INSERT_VLINKCONN, params, resultHandler);
		return this;
	}

	@Override
	public TopologyService getVlinkConn(String vlinkConnId, Handler<AsyncResult<VlinkConn>> resultHandler) {
		findOne(ApiSql.FETCH_VLINKCONN_BY_ID, new JsonArray().add(vlinkConnId)).map(json -> {
			return JSONUtils.json2Pojo(json.encode(), VlinkConn.class);
		}).onComplete(resultHandler);
		return this;
	}

	@Override
	public TopologyService getAllVlinkConns(Handler<AsyncResult<List<VlinkConn>>> resultHandler) {
		find(ApiSql.FETCH_ALL_VLINKCONNS).map(rawList -> rawList.stream().map(row -> {
			return JSONUtils.json2Pojo(row.encode(), VlinkConn.class);
		}).collect(Collectors.toList())).onComplete(resultHandler);
		return this;
	}

	@Override
	public TopologyService getVlinkConnsByVlink(String vlinkId, Handler<AsyncResult<List<VlinkConn>>> resultHandler) {
		JsonArray params = new JsonArray().add(vlinkId);
		find(ApiSql.FETCH_VLINKCONNS_BY_VLINK, params).map(rawList -> rawList.stream().map(row -> {
			return JSONUtils.json2Pojo(row.encode(), VlinkConn.class);
		}).collect(Collectors.toList())).onComplete(resultHandler);
		return this;
	}

	@Override
	public TopologyService getVlinkConnsByVsubnet(String vsubnetId, Handler<AsyncResult<List<VlinkConn>>> resultHandler) {
		JsonArray params = new JsonArray().add(vsubnetId);
		find(ApiSql.FETCH_VLINKCONNS_BY_VSUBNET, params).map(rawList -> rawList.stream().map(row -> {
			return JSONUtils.json2Pojo(row.encode(), VlinkConn.class);
		}).collect(Collectors.toList())).onComplete(resultHandler);
		return this;
	}

	@Override
	public TopologyService deleteVlinkConn(String vlinkConnId, Handler<AsyncResult<Void>> resultHandler) {
		delete(ApiSql.DELETE_VLINKCONN, new JsonArray().add(vlinkConnId), resultHandler);
		return this;
	}

	@Override
	public TopologyService updateVlinkConn(String id, VlinkConn vlinkConn, Handler<AsyncResult<Void>> resultHandler) {
		JsonArray params = new JsonArray()
				.add(vlinkConn.getLabel())
				.add(vlinkConn.getDescription())
				.add(JSONUtils.pojo2Json(vlinkConn.getInfo(), false))
				.add(vlinkConn.getStatus().getValue())
				.add(id);
		execute(ApiSql.UPDATE_VLINKCONN, params, resultHandler);
		return this;
	}

	/********** Vconnection **********/
	@Override
	public TopologyService addVconnection(Vconnection vconnection, Handler<AsyncResult<Integer>> resultHandler) {
		// TODO: check CTPs type
		JsonArray params = new JsonArray()
				.add(vconnection.getName())
				.add(vconnection.getLabel())
				.add(vconnection.getDescription())
				.add(JSONUtils.pojo2Json(vconnection.getInfo(), false))
				.add(vconnection.getStatus().getValue())
				.add(vconnection.getSrcVctpId())
				.add(vconnection.getDestVctpId());
		insert(ApiSql.INSERT_VCONNECTION, params, resultHandler);
		return this;
	}

	@Override
	public TopologyService getVconnection(String vconnectionId, Handler<AsyncResult<Vconnection>> resultHandler) {
		findOne(ApiSql.FETCH_VCONNECTION_BY_ID, new JsonArray().add(vconnectionId)).map(json -> {
			return JSONUtils.json2Pojo(json.encode(), Vconnection.class);
		}).onComplete(resultHandler);
		return this;
	}

	@Override
	public TopologyService getAllVconnections(Handler<AsyncResult<List<Vconnection>>> resultHandler) {
		find(ApiSql.FETCH_ALL_VCONNECTIONS).map(rawList -> rawList.stream().map(row -> {
			return JSONUtils.json2Pojo(row.encode(), Vconnection.class);
		}).collect(Collectors.toList())).onComplete(resultHandler);
		return this;
	}

	@Override
	public TopologyService getVconnectionsByType(ConnTypeEnum type, Handler<AsyncResult<List<Vconnection>>> resultHandler) {
		JsonArray params = new JsonArray().add(type.getValue());
		find(ApiSql.FETCH_VCONNECTIONS_BY_TYPE, params).map(rawList -> rawList.stream().map(row -> {
			return JSONUtils.json2Pojo(row.encode(), Vconnection.class);
		}).collect(Collectors.toList())).onComplete(resultHandler);
		return this;
	}

	@Override
	public TopologyService getVconnectionsByVsubnet(String vsubnetId, Handler<AsyncResult<List<Vconnection>>> resultHandler) {
		JsonArray params = new JsonArray().add(vsubnetId);
		find(ApiSql.FETCH_VCONNECTIONS_BY_VSUBNET, params).map(rawList -> rawList.stream().map(row -> {
			return JSONUtils.json2Pojo(row.encode(), Vconnection.class);
		}).collect(Collectors.toList())).onComplete(resultHandler);
		return this;
	}

	@Override
	public TopologyService deleteVconnection(String vconnectionId, Handler<AsyncResult<Void>> resultHandler) {
		delete(ApiSql.DELETE_VCONNECTION, new JsonArray().add(vconnectionId), resultHandler);
		return this;
	}

	@Override
	public TopologyService updateVconnection(String id, Vconnection vconnection, Handler<AsyncResult<Void>> resultHandler) {
		JsonArray params = new JsonArray()
				.add(vconnection.getLabel())
				.add(vconnection.getDescription())
				.add(JSONUtils.pojo2Json(vconnection.getInfo(), false))
				.add(vconnection.getStatus().getValue())
				.add(id);
		execute(ApiSql.UPDATE_VCONNECTION, params, resultHandler);
		return this;
	}

	/********** PrefixAnn **********/
	@Override
	public TopologyService addPrefix(Prefix prefix, Handler<AsyncResult<Integer>> resultHandler) {
		JsonArray params = new JsonArray()
				.add(prefix.getName())
				.add(prefix.getOriginId())
				.add(prefix.getAvailable());
		insert(ApiSql.INSERT_PREFIX, params, resultHandler);
		return this;
	}

	@Override
	public TopologyService getPrefix(String prefixId, Handler<AsyncResult<Prefix>> resultHandler) {
		findOne(ApiSql.FETCH_PREFIX_BY_ID, new JsonArray().add(prefixId))
				.map(Prefix::new)
				.onComplete(resultHandler);
		return this;
	}

	@Override
	public TopologyService getAllPrefixes(Handler<AsyncResult<List<Prefix>>> resultHandler) {
		find(ApiSql.FETCH_ALL_PREFIXES)
				.map(rawList -> rawList.stream().map(Prefix::new).collect(Collectors.toList()))
				.onComplete(resultHandler);
		return this;
	}

	@Override
	public TopologyService getPrefixesByVsubnet(String vsubnetId,
			Handler<AsyncResult<List<Prefix>>> resultHandler) {
		JsonArray params = new JsonArray().add(vsubnetId);
		find(ApiSql.FETCH_PREFIXES_BY_VSUBNET, params)
				.map(rawList -> rawList.stream().map(Prefix::new).collect(Collectors.toList()))
				.onComplete(resultHandler);
		return this;
	}

	@Override
	public TopologyService getPrefixesByVnode(String nodeId, Handler<AsyncResult<List<Prefix>>> resultHandler) {
		JsonArray params = new JsonArray().add(nodeId);
		find(ApiSql.FETCH_PREFIXES_BY_NODE, params)
				.map(rawList -> rawList.stream().map(Prefix::new).collect(Collectors.toList()))
				.onComplete(resultHandler);
		return this;
	}

	@Override
	public TopologyService deletePrefix(String prefixId, Handler<AsyncResult<Void>> resultHandler) {
		delete(ApiSql.DELETE_PREFIX, new JsonArray().add(prefixId), resultHandler);
		return this;
	}

	@Override
	public TopologyService deletePrefixByName(int originId, String name, Handler<AsyncResult<Void>> resultHandler) {
		JsonArray params = new JsonArray().add(originId).add(name);
		execute(ApiSql.DELETE_PREFIX_BY_NAME, params, resultHandler);
		return this;
	}

	/********** CrossConnect **********/
	@Override
	public TopologyService addCrossConnect(CrossConnect crossConnect, Handler<AsyncResult<Integer>> resultHandler) {
		JsonArray checkParams = new JsonArray()
				.add(crossConnect.getSwitchId())
				.add(crossConnect.getIngressPortId())
				.add(crossConnect.getEgressPortId());
		
		findOne(ApiSql.XC_CHECK_AND_GET_INFO, checkParams)
			.onComplete(res -> {
				if (res.succeeded()) {
					JsonArray params = new JsonArray()
							.add(crossConnect.getName())
							.add(crossConnect.getLabel())
							.add(crossConnect.getDescription())
							.add(crossConnect.getSwitchId())
							.add(crossConnect.getIngressPortId())
							.add(crossConnect.getEgressPortId());
					insert(ApiSql.INSERT_CROSS_CONNECT, params, resultHandler);
				} else {
					resultHandler.handle(Future.failedFuture(res.cause()));
				}
		});
		return this;
	}

	@Override
	public TopologyService getCrossConnectById(String crossConnectId, Handler<AsyncResult<CrossConnect>> resultHandler) {
		findOne(ApiSql.FETCH_CROSS_CONNECT_BY_ID, new JsonArray().add(crossConnectId)).map(json -> {
			return JSONUtils.json2Pojo(json.encode(), CrossConnect.class);
		}).onComplete(resultHandler);
		return this;
	}

	@Override
	public TopologyService getCrossConnectsByNode(String nodeId, Handler<AsyncResult<List<CrossConnect>>> resultHandler) {
		JsonArray params = new JsonArray().add(nodeId);
		find(ApiSql.FETCH_CROSS_CONNECTS_BY_NODE, params).map(rawList -> rawList.stream().map(row -> {
			return JSONUtils.json2Pojo(row.encode(), CrossConnect.class);
		}).collect(Collectors.toList())).onComplete(resultHandler);
		return this;
	}

	@Override
	public TopologyService deleteCrossConnect(String crossConnectId, Handler<AsyncResult<Void>> resultHandler) {		
		findOne(ApiSql.XC_GET_INFO, new JsonArray().add(crossConnectId))
			.onComplete(res -> {
				if (res.succeeded()) {
					delete(ApiSql.DELETE_CROSS_CONNECT, new JsonArray().add(crossConnectId), resultHandler);
				} else {
					resultHandler.handle(Future.failedFuture(res.cause()));
				}
		});
		return this;
	}


	/* ------------------ STATUS ----------------- */
	@Override
	public TopologyService updateNodeStatus(int id, StatusEnum status, Handler<AsyncResult<Void>> resultHandler) {
		UUID op = UUID.randomUUID();
		JsonArray params = new JsonArray().add(status.getValue()).add(id);
		beginTransaction(Entity.NODE, op, InternalSql.LOCK_TABLES).onComplete(tx -> {
			if (tx.succeeded()) {
				transactionExecute(InternalSql.UPDATE_NODE_STATUS, params).onComplete(u -> {
					if (u.succeeded()) {
						params.remove(0);
						transactionFind(ApiSql.FETCH_VLTPS_BY_VNODE, params).onComplete(ar -> {
							if (ar.succeeded()) {
								List<Future> futures = new ArrayList<>();

								// Update LTPs status
								Vltp[] ltps = JSONUtils.json2Pojo(new JsonArray(ar.result()).encode(), Vltp[].class);
								for (Vltp ltp : ltps) {
									Promise<Void> p = Promise.promise();
									futures.add(p.future());
									updateLtpStatus(ltp.getId(), status, op.toString(), p);
								}

								// Update Prefixes availability
								Promise<Void> p = Promise.promise();
								futures.add(p.future());
								JsonArray pUpdatePrefixes = new JsonArray().add(status.equals(StatusEnum.UP)).add(id);
								transactionExecute(InternalSql.UPDATE_PREFIX_STATUS_BY_NODE, pUpdatePrefixes).onComplete(p);

								CompositeFuture.all(futures).map((Void) null).onComplete(done -> {
									if (done.succeeded()) {
										commitTransaction(Entity.NODE, op).onComplete(resultHandler);
									} else {
										rollback();
										resultHandler.handle(Future.failedFuture(done.cause()));
									}
								});
							} else {
								resultHandler.handle(Future.failedFuture(ar.cause()));
							}
						});
					} else {
						resultHandler.handle(Future.failedFuture(u.cause()));
					}
				});
			} else {
				resultHandler.handle(Future.failedFuture(tx.cause()));
			}
		});
		return this;
	}

	@Override
	public TopologyService updateLtpStatus(int id, StatusEnum status, String op, Handler<AsyncResult<Void>> resultHandler) {
		UUID opp[] = { getUUID(op) };
		beginTransaction(Entity.LTP, opp[0], InternalSql.LOCK_TABLES).onComplete(tx -> {
			if (tx.succeeded()) {
				JsonArray params = new JsonArray().add(status.getValue()).add(id);
				transactionExecute(InternalSql.UPDATE_LTP_STATUS, params).onComplete(u -> {
					if (u.succeeded()) {
						List<Future> fLtp = new ArrayList<>();

						// Update Link status
						Promise<Void> pUpdateLink = Promise.promise();
						fLtp.add(pUpdateLink.future());
						params.remove(0);
						transactionFind(InternalSql.FETCH_LINK_BY_LTP, params).onComplete(ar -> {
							if (ar.succeeded()) {
								if (!ar.result().isEmpty()) {
									Vlink vlink = JSONUtils.json2Pojo(ar.result().get(0).encode(), Vlink.class);
									updateLinkStatus(vlink.getId(), status, opp[0].toString(), pUpdateLink);
								} else {
									pUpdateLink.complete();
								}
							} else {
								pUpdateLink.fail(ar.cause());
							}
						});

						// Update CTPs status
						Promise<Void> pUpdateCtp = Promise.promise();
						fLtp.add(pUpdateCtp.future());
						transactionFind(ApiSql.FETCH_VCTPS_BY_VLTP, params).onComplete(ar -> {
							if (ar.succeeded()) {
								List<Future> fCtpStatus = new ArrayList<>();
								Vctp[] vctps = JSONUtils.json2Pojo(new JsonArray(ar.result()).encode(), Vctp[].class);
								for (Vctp vctp : vctps) {
									Promise<Void> p = Promise.promise();
									fCtpStatus.add(p.future());
									updateCtpStatus(vctp.getId(), vctp.getConnType(), status, opp[0].toString(), p);
								}
								CompositeFuture.all(fCtpStatus).map((Void) null).onComplete(pUpdateCtp);
							} else {
								pUpdateCtp.fail(ar.cause());
							}		
						});

						CompositeFuture.all(fLtp).map((Void) null).onComplete(done -> {
							if (done.succeeded()) {
								commitTransaction(Entity.LTP, opp[0]).onComplete(resultHandler);
							} else {
								rollback();
								resultHandler.handle(Future.failedFuture(done.cause()));
							}
						});
					} else {
						resultHandler.handle(Future.failedFuture(u.cause()));
					}
				});
			} else {
				resultHandler.handle(Future.failedFuture(tx.cause()));
			}
		});
		return this;
	}

	@Override
	public TopologyService updateCtpStatus(int id, ConnTypeEnum type, StatusEnum status, String op, Handler<AsyncResult<Void>> resultHandler) {
		UUID opp[] = { getUUID(op) };
		beginTransaction(Entity.CTP, opp[0], InternalSql.LOCK_TABLES).onComplete(tx -> {
			if (tx.succeeded()) {
				Promise<ConnTypeEnum> pGetCtpType = Promise.promise();
				if (type == null) {
					JsonArray ctpId = new JsonArray().add(id);
					transactionFindOne(ApiSql.FETCH_VCTP_BY_ID, ctpId).map(json -> {
						if (json.getInteger("vltpId") == null) {
							json.remove("vltpId");
						}
						if (json.getInteger("vctpId") == null) {
							json.remove("vctpId");
						}
						return JSONUtils.json2Pojo(json.encode(), Vctp.class);
					}).onComplete(ctp -> {
						if (ctp.succeeded()) {
							pGetCtpType.complete(ctp.result().getConnType());
						} else {
							pGetCtpType.fail(ctp.cause());
						}
					});
				} else {
					pGetCtpType.complete(type);
				}
				pGetCtpType.future().onComplete(go -> {
					if (go.succeeded()) {
						ConnTypeEnum ctpType = go.result();
						JsonArray params = new JsonArray().add(status.getValue()).add(id);
						transactionExecute(InternalSql.UPDATE_CTP_STATUS, params).onComplete(u -> {
							if (u.succeeded()) {
								List<Future> fCtp = new ArrayList<>();
						
								// Update LC or Connection status
								Promise<Void> pUpdate = Promise.promise();
								fCtp.add(pUpdate.future());
								params.remove(0);
								if (ctpType.equals(ConnTypeEnum.Ether)) {
									transactionFind(InternalSql.FETCH_LC_BY_CTP, params).onComplete(ar -> {
										if (ar.succeeded()) {
											if (!ar.result().isEmpty()) {
												VlinkConn vlc = JSONUtils.json2Pojo(ar.result().get(0).encode(), VlinkConn.class);
												updateLcStatus(vlc.getId(), status, opp[0].toString(), pUpdate);
											} else {
												pUpdate.complete();
											}
										} else {
											pUpdate.fail(ar.cause());
										}
									});
								} else {
									transactionFind(InternalSql.FETCH_CONNECTION_BY_CTP, params).onComplete(ar -> {
										if (ar.succeeded()) {
											if (!ar.result().isEmpty()) {
												Vconnection vcon = JSONUtils.json2Pojo(ar.result().get(0).encode(), Vconnection.class);
												updateConnectionStatus(vcon.getId(), status, opp[0].toString(), pUpdate);
											} else {
												pUpdate.complete();
											}
										} else {
											pUpdate.fail(ar.cause());
										}
									});
								}

								// Update CTPs status
								Promise<Void> pUpdateCtp = Promise.promise();
								fCtp.add(pUpdateCtp.future());
								transactionFind(ApiSql.FETCH_VCTPS_BY_VCTP, params).onComplete(ar -> {
									if (ar.succeeded()) {
										List<Future> fCtpStatus = new ArrayList<>();
										Vctp[] vctps = JSONUtils.json2Pojo(new JsonArray(ar.result()).encode(), Vctp[].class);
										for (Vctp vctp : vctps) {
											Promise<Void> p = Promise.promise();
											fCtpStatus.add(p.future());
											updateCtpStatus(vctp.getId(), vctp.getConnType(), status, opp[0].toString(), p);
										}
										CompositeFuture.all(fCtpStatus).map((Void) null).onComplete(pUpdateCtp);
									} else {
										pUpdateCtp.fail(ar.cause());
									}		
								});

								CompositeFuture.all(fCtp).map((Void) null).onComplete(done -> {
									if (done.succeeded()) {
										commitTransaction(Entity.CTP, opp[0]).onComplete(resultHandler);
									} else {
										rollback();
										resultHandler.handle(Future.failedFuture(done.cause()));
									}
								});
							} else {
								resultHandler.handle(Future.failedFuture(u.cause()));
							}
						});
					} else {
						resultHandler.handle(Future.failedFuture(go.cause()));
					}
				});
			} else {
				resultHandler.handle(Future.failedFuture(tx.cause()));
			}
		});
		return this;
	}

	public TopologyService updateLinkStatus(int id, StatusEnum status, String op, Handler<AsyncResult<Void>> resultHandler) {
		UUID opp[] = { getUUID(op) };
		beginTransaction(Entity.LINK, opp[0], InternalSql.LOCK_TABLES).onComplete(tx -> {
			if (tx.succeeded()) {
				Promise<String> linkStatus = Promise.promise();
				JsonArray params = new JsonArray().add(id);
				transactionFind(InternalSql.FETCH_LINK_LTP_STATUS, params).onComplete(ar -> {
					if (ar.succeeded()) {
						if (ar.result().size() == 2) {
							String s0 = ar.result().get(0).getString("status");
							String s1 = ar.result().get(1).getString("status");

							if (s0.equals("UP") && s1.equals("UP")) {
								linkStatus.complete("UP");
							} else {
								linkStatus.complete("DOWN");
							}
						} else {
							linkStatus.fail("Failed to fetch LTPs of Link");
						}
					} else {
						linkStatus.fail(ar.cause());
					}
				});
				linkStatus.future().onComplete(res -> {
					if (res.succeeded()) {
						JsonArray uParams = new JsonArray().add(res.result()).add(id);
						transactionExecute(InternalSql.UPDATE_LINK_STATUS, uParams).onComplete(done -> {
							if (done.succeeded()) {
								commitTransaction(Entity.LINK, opp[0]).onComplete(resultHandler);
							} else {
								rollback();
								resultHandler.handle(Future.failedFuture(done.cause()));
							}
						});
					} else {
						resultHandler.handle(Future.failedFuture(res.cause()));
					}
				});
			} else {
				resultHandler.handle(Future.failedFuture(tx.cause()));
			}
		});
		return this;
	}

	public TopologyService updateLcStatus(int id, StatusEnum status, String op, Handler<AsyncResult<Void>> resultHandler) {
		UUID opp[] = { getUUID(op) };
		beginTransaction(Entity.LC, opp[0], InternalSql.LOCK_TABLES).onComplete(tx -> {
			if (tx.succeeded()) {
				Promise<String> lcStatus = Promise.promise();
				JsonArray params = new JsonArray().add(id);
				transactionFind(InternalSql.FETCH_LC_CTP_STATUS, params).onComplete(ar -> {
					if (ar.succeeded()) {
						if (ar.result().size() == 2) {
							String s0 = ar.result().get(0).getString("status");
							String s1 = ar.result().get(1).getString("status");

							if (s0.equals("UP") && s1.equals("UP")) {
								lcStatus.complete("UP");
							} else {
								lcStatus.complete("DOWN");
							}
						} else {
							lcStatus.fail("Failed to fetch CTPs of LC");
						}
					} else {
						lcStatus.fail(ar.cause());
					}
				});
				lcStatus.future().onComplete(res -> {
					if (res.succeeded()) {
						JsonArray uParams = new JsonArray().add(res.result()).add(id);
						transactionExecute(InternalSql.UPDATE_LC_STATUS, uParams).onComplete(done -> {
							if (done.succeeded()) {
								commitTransaction(Entity.LC, opp[0]).onComplete(resultHandler);
							} else {
								rollback();
								resultHandler.handle(Future.failedFuture(done.cause()));
							}
						});
					} else {
						resultHandler.handle(Future.failedFuture(res.cause()));
					}
				});
			} else {
				resultHandler.handle(Future.failedFuture(tx.cause()));
			}
		});
		return this;
	}

	public TopologyService updateConnectionStatus(int id, StatusEnum status, String op, Handler<AsyncResult<Void>> resultHandler) {
		UUID opp[] = { getUUID(op) };
		beginTransaction(Entity.CONNECTION, opp[0], InternalSql.LOCK_TABLES).onComplete(tx -> {
			if (tx.succeeded()) {
				Promise<String> conStatus = Promise.promise();
				JsonArray params = new JsonArray().add(id);
				transactionFind(InternalSql.FETCH_CONNECTION_CTP_STATUS, params).onComplete(ar -> {
					if (ar.succeeded()) {
						if (ar.result().size() == 2) {
							String s0 = ar.result().get(0).getString("status");
							String s1 = ar.result().get(1).getString("status");

							if (s0.equals("UP") && s1.equals("UP")) {
								conStatus.complete("UP");
							} else {
								conStatus.complete("DOWN");
							}
						} else {
							conStatus.fail("Failed to fetch CTPs of Connection");
						}
					} else {
						conStatus.fail(ar.cause());
					}
				});
				conStatus.future().onComplete(res -> {
					if (res.succeeded()) {
						JsonArray uParams = new JsonArray().add(res.result()).add(id);
						transactionExecute(InternalSql.UPDATE_CONNECTION_STATUS, uParams).onComplete(arr -> {
							if (arr.succeeded()) {
								commitTransaction(Entity.CONNECTION, opp[0]).onComplete(resultHandler);
							} else {
								rollback();
								resultHandler.handle(Future.failedFuture(arr.cause()));
							}
						});
					} else {
						resultHandler.handle(Future.failedFuture(res.cause()));
					}
				});
			} else {
				resultHandler.handle(Future.failedFuture(tx.cause()));
			}
		});
		return this;
	}

	/* Helper methods */
	private UUID getUUID(String op) {
		if (op == null) {
			return UUID.randomUUID();
		}
		try {
			return UUID.fromString(op);
		} catch (IllegalArgumentException e) {
			return UUID.randomUUID();
		}
	}

	private String validateAndConvertMAC(String str) {
		if (str == null) {
			return "";
		}
		// String regex = "^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$";
		String regex = "^([0-9A-Fa-f]{2}[:-])"
                       + "{5}([0-9A-Fa-f]{2})|"
                       + "([0-9a-fA-F]{4}\\."
                       + "[0-9a-fA-F]{4}\\."
                       + "[0-9a-fA-F]{4})$";
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(str);
		if (m.matches()){
			String norm = str.replaceAll("[^a-fA-F0-9]", "");
			return norm.replaceAll("(.{2})", "$1"+":").substring(0,17);
		} else {
			return "";
		}
	}

	private boolean checkCidrIp(String str) {
		if (str == null) {
			return false;
		}
		String regex = "^([0-9]{1,3}\\.){3}[0-9]{1,3}(\\/([0-9]|[1-2][0-9]|3[0-2]))$";
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(str);
		return m.matches();
	}
}