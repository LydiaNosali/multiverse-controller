package io.nms.central.microservice.topology.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import io.nms.central.microservice.common.functional.Functional;
import io.nms.central.microservice.common.functional.JsonUtils;
import io.nms.central.microservice.common.service.JdbcRepositoryWrapper;
import io.nms.central.microservice.notification.model.Status.StatusEnum;
import io.nms.central.microservice.topology.TopologyService;
import io.nms.central.microservice.topology.model.EtherConnInfo;
import io.nms.central.microservice.topology.model.NdnConnInfo;
import io.nms.central.microservice.topology.model.Prefix;
import io.nms.central.microservice.topology.model.Vconnection;
import io.nms.central.microservice.topology.model.VcrossConnect;
import io.nms.central.microservice.topology.model.Vctp;
import io.nms.central.microservice.topology.model.Vctp.ConnTypeEnum;
import io.nms.central.microservice.topology.model.Vlink;
import io.nms.central.microservice.topology.model.VlinkConn;
import io.nms.central.microservice.topology.model.Vltp;
import io.nms.central.microservice.topology.model.Vltp.LtpDirectionEnum;
import io.nms.central.microservice.topology.model.Vnode;
import io.nms.central.microservice.topology.model.Vnode.NodeTypeEnum;
import io.nms.central.microservice.topology.model.Vsubnet;
import io.nms.central.microservice.topology.model.Vsubnet.SubnetTypeEnum;
import io.nms.central.microservice.topology.model.Vtrail;
import io.vertx.codegen.annotations.Fluent;
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
	public TopologyService initializePersistence(JsonObject data, Handler<AsyncResult<Void>> resultHandler) {
		List<String> statements = new ArrayList<String>();
		statements.add(ApiSql.CREATE_TABLE_VSUBNET);
		statements.add(ApiSql.CREATE_TABLE_VNODE);
		statements.add(ApiSql.CREATE_TABLE_VLTP);
		statements.add(ApiSql.CREATE_TABLE_VCTP);
		statements.add(ApiSql.CREATE_TABLE_VLINK);
		statements.add(ApiSql.CREATE_TABLE_VLINKCONN);
		statements.add(ApiSql.CREATE_TABLE_VCONNECTION);
		statements.add(ApiSql.CREATE_TABLE_VTRAIL);
		statements.add(ApiSql.CREATE_TABLE_VCROSS_CONNECT);
		statements.add(ApiSql.CREATE_TABLE_PREFIX);
		client.getConnection(conn -> {
			if (conn.succeeded()) {
				conn.result().batch(statements, r -> {
					conn.result().close();
					if (r.succeeded()) {
						createDefaultSubnets(sn -> {
							if (sn.succeeded()) {
								initializeStatus(res -> {
									if (res.succeeded()) {
										loadBaseTopology(data, resultHandler);
									} else {
										resultHandler.handle(Future.failedFuture(res.cause()));
									}
								});
							} else {
								resultHandler.handle(Future.failedFuture(r.cause()));
							}
						});
					} else {
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
		statements.add(InternalSql.INIT_TRAIL_STATUS);
		statements.add(InternalSql.INIT_CROSSCONNECT_STATUS);
		statements.add(InternalSql.INIT_PREFIX_STATUS);
		// statements.add("DELETE FROM Vsubnet WHERE name = 'ex-net'");
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
	
	private void createDefaultSubnets(Handler<AsyncResult<Void>> resultHandler) {
		Vsubnet defaultNdnSubnet = new Vsubnet();
		defaultNdnSubnet.setName("default-ndn-sn");
		defaultNdnSubnet.setLabel("default ndn subnet");
		defaultNdnSubnet.setDescription("Automatically created NDN subnet");
		defaultNdnSubnet.setType(SubnetTypeEnum.NDN);
		defaultNdnSubnet.setInfo(new HashMap<String,Object>());
		JsonArray paramsNdn = new JsonArray()
				.add(defaultNdnSubnet.getName())
				.add(defaultNdnSubnet.getLabel())
				.add(defaultNdnSubnet.getDescription())
				.add(defaultNdnSubnet.getType().getValue())
				.add(JsonUtils.pojo2Json(defaultNdnSubnet.getInfo(), false));
		
		Vsubnet defaultQnetSubnet = new Vsubnet();
		defaultQnetSubnet.setName("default-qnet-sn");
		defaultQnetSubnet.setLabel("default qnet subnet");
		defaultQnetSubnet.setDescription("Automatically created Qnet subnet");
		defaultQnetSubnet.setType(SubnetTypeEnum.QNET);
		defaultQnetSubnet.setInfo(new HashMap<String,Object>());
		JsonArray paramsQnet = new JsonArray()
				.add(defaultQnetSubnet.getName())
				.add(defaultQnetSubnet.getLabel())
				.add(defaultQnetSubnet.getDescription())
				.add(defaultQnetSubnet.getType().getValue())
				.add(JsonUtils.pojo2Json(defaultQnetSubnet.getInfo(), false));

		execute(InternalSql.INSERT_IGNORE_VSUBNET, paramsNdn, sn1 -> {
			if (sn1.succeeded()) {
				execute(InternalSql.INSERT_IGNORE_VSUBNET, paramsQnet, sn2 -> {
					if (sn2.succeeded()) {
						resultHandler.handle(Future.succeededFuture());
					} else {
						resultHandler.handle(Future.failedFuture(sn2.cause()));
					}
				});
			} else {
				resultHandler.handle(Future.failedFuture(sn1.cause()));
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
				.add(vsubnet.getType().getValue())
				.add(JsonUtils.pojo2Json(vsubnet.getInfo(), false));
		insert(ApiSql.INSERT_VSUBNET, params, resultHandler);
		return this;
	}

	@Override
	public TopologyService getVsubnet(String vsubnetId, Handler<AsyncResult<Vsubnet>> resultHandler) {
		this.findOne(ApiSql.FETCH_VSUBNET_BY_ID, new JsonArray().add(vsubnetId)).map(json -> {
			return JsonUtils.json2Pojo(json.encode(), Vsubnet.class);
		}).onComplete(resultHandler);
		return this;
	}

	@Override
	public TopologyService getAllVsubnets(Handler<AsyncResult<List<Vsubnet>>> resultHandler) {
		this.find(ApiSql.FETCH_ALL_VSUBNETS).map(rawList -> rawList.stream().map(row -> {
			return JsonUtils.json2Pojo(row.encode(), Vsubnet.class);
		}).collect(Collectors.toList())).onComplete(resultHandler);
		return this;
	}
	
	@Override
	public TopologyService getVsubnetsByType(SubnetTypeEnum type, Handler<AsyncResult<List<Vsubnet>>> resultHandler) {
		this.find(ApiSql.FETCH_VSUBNETS_BY_TYPE, new JsonArray().add(type.getValue()))
				.map(rawList -> rawList.stream().map(row -> {
			return JsonUtils.json2Pojo(row.encode(), Vsubnet.class);
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
				.add(JsonUtils.pojo2Json(vsubnet.getInfo(), false))
				.add(id);
		execute(ApiSql.UPDATE_VSUBNET, params, resultHandler);
		return this;
	}

	/********** Vnode **********/
	@Override
	public TopologyService addVnode(Vnode vnode, Handler<AsyncResult<Integer>> resultHandler) {
		if (!vnode.getHwaddr().isEmpty()) {
			String macAddr = Functional.validateAndConvertMAC(vnode.getHwaddr());
			if (macAddr.isEmpty()) {
				resultHandler.handle(Future.failedFuture("MAC address not valid"));
				return this;
			}
			vnode.setHwaddr(macAddr);
		}

		if (!Functional.isValidHostIp(vnode.getMgmtIp())) {
			resultHandler.handle(Future.failedFuture("IP address not valid"));
			return this;
		}

		JsonArray params = new JsonArray()
				.add(vnode.getName())
				.add(vnode.getLabel())
				.add(vnode.getDescription())
				.add(JsonUtils.pojo2Json(vnode.getInfo(), false))
				.add(vnode.getStatus().getValue())
				.add(vnode.getPosx())
				.add(vnode.getPosy())
				.add(vnode.getLocation())
				.add(vnode.getType())
				.add(vnode.getVsubnetId())
				.add(vnode.getHwaddr())
				.add(vnode.getMgmtIp());
		insert(ApiSql.INSERT_VNODE, params, res -> {
			// create loppback interface (LTP)
			if (res.succeeded()) {
				Vltp vltp = new Vltp();
				vltp.setName(vnode.getName() + "-loopback");
				vltp.setVnodeId(res.result());
				vltp.setPort("0");
				vltp.setStatus(StatusEnum.UP);
				vltp.setLabel("");
				vltp.setDescription("");
				vltp.setBandwidth("");
				vltp.setMtu(0);	
				vltp.setDirection(LtpDirectionEnum.INOUT);
				addVltp(vltp, done -> {
					if (done.succeeded()) {
						resultHandler.handle(Future.succeededFuture(res.result()));
					} else {
						deleteVnode(String.valueOf(res.result()), ig -> {
							resultHandler.handle(Future.failedFuture("Vnode not created: failed to create loopback LTP."));
							logger.warn("Vnode not created: failed to create loopback LTP.");
						});
					}
				});
			} else {
				resultHandler.handle(Future.failedFuture(res.cause()));
			}
		});
		return this;
	}

	@Override
	public TopologyService getVnode(String vnodeId, Handler<AsyncResult<Vnode>> resultHandler) {
		this.findOne(ApiSql.FETCH_VNODE_BY_ID, new JsonArray().add(vnodeId)).map(json -> {
			return JsonUtils.json2Pojo(json.encode(), Vnode.class);
		}).onComplete(resultHandler);
		return this;
	}

	@Override
	public TopologyService getVnodesByVsubnet(String vsubnetId, Handler<AsyncResult<List<Vnode>>> resultHandler) {
		JsonArray params = new JsonArray().add(vsubnetId);
		this.find(ApiSql.FETCH_VNODES_BY_VSUBNET, params).map(rawList -> rawList.stream().map(row -> {
			return JsonUtils.json2Pojo(row.encode(), Vnode.class);
		}).collect(Collectors.toList())).onComplete(resultHandler);
		return this;
	}

	@Override
	public TopologyService getVnodesByType(NodeTypeEnum type, Handler<AsyncResult<List<Vnode>>> resultHandler) {
		JsonArray params = new JsonArray().add(type.getValue());
		this.find(ApiSql.FETCH_VNODES_BY_TYPE, params).map(rawList -> rawList.stream().map(row -> {
			return JsonUtils.json2Pojo(row.encode(), Vnode.class);
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
						Vltp[] vltps = JsonUtils.json2Pojo(new JsonArray(res.result()).encode(), Vltp[].class);
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
		if (!vnode.getHwaddr().isEmpty()) {
			String macAddr = Functional.validateAndConvertMAC(vnode.getHwaddr());
			if (macAddr.isEmpty()) {
				resultHandler.handle(Future.failedFuture("MAC address not valid"));
				return this;
			}
			vnode.setHwaddr(macAddr);
		}

		if (!Functional.isValidHostIp(vnode.getMgmtIp())) {
			resultHandler.handle(Future.failedFuture("IP address not valid"));
			return this;
		}
		
		JsonArray params = new JsonArray()
				.add(vnode.getLabel())
				.add(vnode.getDescription())
				.add(JsonUtils.pojo2Json(vnode.getInfo(), false))
				.add(vnode.getStatus().getValue())
				.add(vnode.getPosx()).add(vnode.getPosy())
				.add(vnode.getLocation())
				.add(vnode.getHwaddr())
				.add(vnode.getMgmtIp())
				.add(vnode.getType())
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
				.add(JsonUtils.pojo2Json(vltp.getInfo(), false))
				.add(vltp.getStatus().getValue())
				.add(vltp.getVnodeId())
				.add(vltp.getPort())
				.add(vltp.getBandwidth())
				.add(vltp.getMtu())
				.add(vltp.isBusy())
				.add(vltp.getDirection().getValue());
		insert(ApiSql.INSERT_VLTP, params, resultHandler);
		return this;
	}

	@Override
	public TopologyService getVltp(String vltpId, Handler<AsyncResult<Vltp>> resultHandler) {
		findOne(ApiSql.FETCH_VLTP_BY_ID, new JsonArray().add(vltpId)).map(json -> {
			return JsonUtils.json2Pojo(json.encode(), Vltp.class);
		}).onComplete(resultHandler);
		return this;
	}


	@Override
	public TopologyService getVltpsByVnode(String vnodeId, Handler<AsyncResult<List<Vltp>>> resultHandler) {
		JsonArray params = new JsonArray().add(vnodeId);
		find(ApiSql.FETCH_VLTPS_BY_VNODE, params).map(rawList -> rawList.stream().map(row -> {
			return JsonUtils.json2Pojo(row.encode(), Vltp.class);
		}).collect(Collectors.toList())).onComplete(resultHandler);
		return this;
	}
	
	@Override
	public TopologyService getVltpsByVsubnet(String vsubnetId, Handler<AsyncResult<List<Vltp>>> resultHandler) {
		JsonArray params = new JsonArray().add(vsubnetId);
		find(ApiSql.FETCH_VLTPS_BY_VSUBNET, params).map(rawList -> rawList.stream().map(row -> {
			return JsonUtils.json2Pojo(row.encode(), Vltp.class);
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
				.add(JsonUtils.pojo2Json(vltp.getInfo(), false))
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
							Vlink vlink = JsonUtils.json2Pojo(res.result().get(0).encode(), Vlink.class);
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
			String macAddr = Functional.validateAndConvertMAC(((EtherConnInfo)vctp.getConnInfo()).getAddress());
			if (macAddr.isEmpty()) {
				resultHandler.handle(Future.failedFuture("MAC address not valid"));
				return this;
			}
			((EtherConnInfo)vctp.getConnInfo()).setAddress(macAddr);
		}
		if (ct.equals(ConnTypeEnum.NDN)) {
			String lMacAddr = Functional.validateAndConvertMAC(((NdnConnInfo) vctp.getConnInfo()).getLocal());
			String rMacAddr = Functional.validateAndConvertMAC(((NdnConnInfo) vctp.getConnInfo()).getRemote());
			if (lMacAddr.isEmpty() || rMacAddr.isEmpty()){
				resultHandler.handle(Future.failedFuture("MAC address not valid"));
				return this;
			}
			((NdnConnInfo) vctp.getConnInfo()).setLocal(lMacAddr);
			((NdnConnInfo) vctp.getConnInfo()).setRemote(rMacAddr);
		}

		JsonArray pQueryNode = new JsonArray().add(vctp.getParentId());

		if (vctp.getConnType().equals(ConnTypeEnum.Ether) || vctp.getConnType().equals(ConnTypeEnum.QUBIT)) {
			findOne(ApiSql.FETCH_VLTP_BY_ID, pQueryNode).onComplete(ar -> {
				if (ar.succeeded()) {
					Vltp parentLtp = JsonUtils.json2Pojo(ar.result().encode(), Vltp.class);
					JsonArray params = new JsonArray()
							.add(vctp.getName())
							.add(vctp.getLabel())
							.add(vctp.getDescription())
							.add(JsonUtils.pojo2Json(vctp.getInfo(), false))
							.add(vctp.getConnType())
							.add(JsonUtils.pojo2Json(vctp.getConnInfo(), false))
							.add(vctp.getStatus().getValue())
							.add(vctp.getParentId())
							.add(parentLtp.getVnodeId());
					insert(ApiSql.INSERT_VCTP_VLTP, params, resultHandler);
				} else {
					resultHandler.handle(Future.failedFuture(ar.cause()));
				}
			});
		} else {
			findOne(ApiSql.FETCH_VCTP_BY_ID, pQueryNode).onComplete(ar -> {
				if (ar.succeeded()) {
					Vctp parentCtp = JsonUtils.json2Pojo(ar.result().encode(), Vctp.class);
					JsonArray params = new JsonArray()
							.add(vctp.getName())
							.add(vctp.getLabel())
							.add(vctp.getDescription())
							.add(JsonUtils.pojo2Json(vctp.getInfo(), false))
							.add(vctp.getConnType())
							.add(JsonUtils.pojo2Json(vctp.getConnInfo(), false))
							.add(vctp.getStatus().getValue())
							.add(vctp.getParentId())
							.add(parentCtp.getVnodeId());
					insert(ApiSql.INSERT_VCTP_VCTP, params, resultHandler);
				} else {
					resultHandler.handle(Future.failedFuture(ar.cause()));
				}
			});
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
			return JsonUtils.json2Pojo(json.encode(), Vctp.class);
		}).onComplete(resultHandler);
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
			return JsonUtils.json2Pojo(row.encode(), Vctp.class);
		}).collect(Collectors.toList())).onComplete(resultHandler);
		return this;
	}

	@Override
	public TopologyService getVctpsByVltp(String vltpId, Handler<AsyncResult<List<Vctp>>> resultHandler) {
		JsonArray params = new JsonArray().add(vltpId);
		find(ApiSql.FETCH_VCTPS_BY_VLTP, params).map(rawList -> rawList.stream().map(row -> {
			row.remove("vctpId");
			return JsonUtils.json2Pojo(row.encode(), Vctp.class);
		}).collect(Collectors.toList())).onComplete(resultHandler);
		return this;
	}

	@Override
	public TopologyService getVctpsByVctp(String vctpId, Handler<AsyncResult<List<Vctp>>> resultHandler) {
		JsonArray params = new JsonArray().add(vctpId);
		find(ApiSql.FETCH_VCTPS_BY_VCTP, params).map(rawList -> rawList.stream().map(row -> {
			row.remove("vltpId");
			return JsonUtils.json2Pojo(row.encode(), Vctp.class);
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
			return JsonUtils.json2Pojo(row.encode(), Vctp.class);
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
				.add(JsonUtils.pojo2Json(vctp.getInfo(), false))
				.add(vctp.getStatus().getValue())
				.add(id);
		execute(ApiSql.UPDATE_VCTP, params, resultHandler);
		return this;
	}
	
	@Override
    public TopologyService bindVctp(String ctpId, String ltpId, Handler<AsyncResult<Void>> resultHandler) {
		JsonArray params = new JsonArray()
				.add(ltpId)
				.add(ctpId);
		execute(ApiSql.BIND_VCTP, params, resultHandler);
		return this;
	}
	
	@Override
	public TopologyService unbindVctp(String ctpId, Handler<AsyncResult<Void>> resultHandler) {
		JsonArray params = new JsonArray()
				.add(ctpId);
		execute(ApiSql.UNBIND_VCTP, params, resultHandler);
		return this;
	}

	/********** Vlink **********/
	@Override
	public TopologyService addVlink(Vlink vlink, Handler<AsyncResult<Integer>> resultHandler) {
		Promise<Vltp> pVerified = Promise.promise();
		
		Promise<Vltp> pLtp1 = Promise.promise();
		getVltp(String.valueOf(vlink.getSrcVltpId()), pLtp1);
		Promise<Vltp> pLtp2 = Promise.promise();
		getVltp(String.valueOf(vlink.getSrcVltpId()), pLtp2);
		CompositeFuture.all(pLtp1.future(),pLtp2.future()).onSuccess(res -> {
			LtpDirectionEnum ltp1Dir = pLtp1.future().result().getDirection();
			LtpDirectionEnum ltp2Dir = pLtp2.future().result().getDirection();
			if ((ltp1Dir.equals(LtpDirectionEnum.IN) && ltp2Dir.equals(LtpDirectionEnum.IN))
					|| (ltp1Dir.equals(LtpDirectionEnum.OUT) && ltp2Dir.equals(LtpDirectionEnum.OUT))) {
				resultHandler.handle(Future.failedFuture("Wrong LTP directions: " + ltp1Dir.getValue() +" - "+ ltp2Dir.getValue()));
			} else {
				pVerified.complete();
			}
		});
		CompositeFuture.all(pLtp1.future(),pLtp2.future()).onFailure(e -> {
			resultHandler.handle(Future.failedFuture(e.getCause()));
		});

		pVerified.future().onSuccess(ok -> {
			JsonArray pVlink = new JsonArray()
					.add(vlink.getName())
					.add(vlink.getLabel())
					.add(vlink.getDescription())
					.add(JsonUtils.pojo2Json(vlink.getInfo(), false))
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
		});
		return this;
	}

	@Override
	public TopologyService getVlink(String vlinkId, Handler<AsyncResult<Vlink>> resultHandler) {
		findOne(ApiSql.FETCH_VLINK_BY_ID, new JsonArray().add(vlinkId)).map(json -> {
			return JsonUtils.json2Pojo(json.encode(), Vlink.class);
		}).onComplete(resultHandler);
		return this;
	}

	@Override
	public TopologyService getVlinksByVsubnet(String vsubnetId, Handler<AsyncResult<List<Vlink>>> resultHandler) {
		JsonArray params = new JsonArray().add(vsubnetId);
		find(ApiSql.FETCH_VLINKS_BY_VSUBNET, params).map(rawList -> rawList.stream().map(row -> {
			return JsonUtils.json2Pojo(row.encode(), Vlink.class);
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
				.add(JsonUtils.pojo2Json(vlink.getInfo(), false))
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
								Vlink vlink = JsonUtils.json2Pojo(res.result().encode(), Vlink.class);
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
				.add(JsonUtils.pojo2Json(vlinkConn.getInfo(), false))
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
			return JsonUtils.json2Pojo(json.encode(), VlinkConn.class);
		}).onComplete(resultHandler);
		return this;
	}

	@Override
	public TopologyService getVlinkConnsByVlink(String vlinkId, Handler<AsyncResult<List<VlinkConn>>> resultHandler) {
		JsonArray params = new JsonArray().add(vlinkId);
		find(ApiSql.FETCH_VLINKCONNS_BY_VLINK, params).map(rawList -> rawList.stream().map(row -> {
			return JsonUtils.json2Pojo(row.encode(), VlinkConn.class);
		}).collect(Collectors.toList())).onComplete(resultHandler);
		return this;
	}

	@Override
	public TopologyService getVlinkConnsByVsubnet(String vsubnetId, Handler<AsyncResult<List<VlinkConn>>> resultHandler) {
		JsonArray params = new JsonArray().add(vsubnetId);
		find(ApiSql.FETCH_VLINKCONNS_BY_VSUBNET, params).map(rawList -> rawList.stream().map(row -> {
			return JsonUtils.json2Pojo(row.encode(), VlinkConn.class);
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
				.add(JsonUtils.pojo2Json(vlinkConn.getInfo(), false))
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
				.add(JsonUtils.pojo2Json(vconnection.getInfo(), false))
				.add(vconnection.getStatus().getValue())
				.add(vconnection.getSrcVctpId())
				.add(vconnection.getDestVctpId());
		insert(ApiSql.INSERT_VCONNECTION, params, resultHandler);
		return this;
	}

	@Override
	public TopologyService getVconnection(String vconnectionId, Handler<AsyncResult<Vconnection>> resultHandler) {
		findOne(ApiSql.FETCH_VCONNECTION_BY_ID, new JsonArray().add(vconnectionId)).map(json -> {
			return JsonUtils.json2Pojo(json.encode(), Vconnection.class);
		}).onComplete(resultHandler);
		return this;
	}
	
	@Override
	public TopologyService getVconnectionsByType(ConnTypeEnum type, 
			Handler<AsyncResult<List<Vconnection>>> resultHandler) {
		JsonArray params = new JsonArray().add(type.getValue());
		find(ApiSql.FETCH_VCONNECTIONS_BY_TYPE, params).map(rawList -> rawList.stream().map(row -> {
			return JsonUtils.json2Pojo(row.encode(), Vconnection.class);
		}).collect(Collectors.toList())).onComplete(resultHandler);
		return this;
	}

	@Override
	public TopologyService getVconnectionsByVsubnetByType(String vsubnetId, ConnTypeEnum type, 
			Handler<AsyncResult<List<Vconnection>>> resultHandler) {
		JsonArray params = new JsonArray()
				.add(vsubnetId)
				.add(type.getValue());
		find(ApiSql.FETCH_VCONNECTIONS_BY_VSUBNET_BY_TYPE, params).map(rawList -> rawList.stream().map(row -> {
			return JsonUtils.json2Pojo(row.encode(), Vconnection.class);
		}).collect(Collectors.toList())).onComplete(resultHandler);
		return this;
	}

	@Override
	public TopologyService getVconnectionsByVsubnet(String vsubnetId, Handler<AsyncResult<List<Vconnection>>> resultHandler) {
		JsonArray params = new JsonArray().add(vsubnetId);
		find(ApiSql.FETCH_VCONNECTIONS_BY_VSUBNET, params).map(rawList -> rawList.stream().map(row -> {
			return JsonUtils.json2Pojo(row.encode(), Vconnection.class);
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
				.add(JsonUtils.pojo2Json(vconnection.getInfo(), false))
				.add(vconnection.getStatus().getValue())
				.add(id);
		execute(ApiSql.UPDATE_VCONNECTION, params, resultHandler);
		return this;
	}


	/********** Vtrail **********/
	@Override
	public TopologyService addVtrail(Vtrail vtrail, Handler<AsyncResult<Integer>> resultHandler) {
		JsonArray pVtrail = new JsonArray()
				.add(vtrail.getName())
				.add(vtrail.getLabel())
				.add(vtrail.getDescription())
				.add(JsonUtils.pojo2Json(vtrail.getInfo(), false))
				.add(vtrail.getStatus().getValue())
				.add(vtrail.getVsubnetId());
		insert(ApiSql.INSERT_VTRAIL, pVtrail, resultHandler);
		return this;
	}

	@Override
	public TopologyService getVtrail(String id, Handler<AsyncResult<Vtrail>> resultHandler) {
		findOne(ApiSql.FETCH_VTRAIL_BY_ID, new JsonArray().add(id)).map(json -> {
			return JsonUtils.json2Pojo(json.encode(), Vtrail.class);
		}).onComplete(resultHandler);
		return this;
	}

	@Override
	public TopologyService getVtrailsByVsubnet(String vsubnetId, Handler<AsyncResult<List<Vtrail>>> resultHandler) {
		JsonArray params = new JsonArray().add(vsubnetId);
		find(ApiSql.FETCH_VTRAILS_BY_VSUBNET, params).map(rawList -> rawList.stream().map(row -> {
			return JsonUtils.json2Pojo(row.encode(), Vtrail.class);
		}).collect(Collectors.toList())).onComplete(resultHandler);
		return this;
	}

	@Override
	public TopologyService deleteVtrail(String id, Handler<AsyncResult<Void>> resultHandler) {
		delete(ApiSql.DELETE_VTRAIL, new JsonArray().add(id), resultHandler);
		return this;
	}

	@Override
	public TopologyService updateVtrail(String id, Vtrail vtrail, Handler<AsyncResult<Void>> resultHandler) {
		JsonArray params = new JsonArray()
				.add(vtrail.getLabel())
				.add(vtrail.getDescription())
				.add(JsonUtils.pojo2Json(vtrail.getInfo(), false))
				.add(vtrail.getStatus().getValue())
				.add(id);
		execute(ApiSql.UPDATE_VTRAIL, params, resultHandler);
		return this;
	}


	/********** CrossConnect **********/
	@Override
	public TopologyService addVcrossConnect(VcrossConnect vcrossConnect, Handler<AsyncResult<Integer>> resultHandler) {
		JsonArray verifyParams = new JsonArray()
				.add(vcrossConnect.getSwitchId())
				.add(Vnode.NodeTypeEnum.OXC.getValue())
				.add(vcrossConnect.getIngressPortId())
				.add(vcrossConnect.getEgressPortId());
		
		findOne(ApiSql.XC_VERIFY, verifyParams)
			.onComplete(res -> {
				if (res.succeeded()) {
					JsonArray params = new JsonArray()
							.add(vcrossConnect.getName())
							.add(vcrossConnect.getLabel())
							.add(vcrossConnect.getDescription())
							.add(JsonUtils.pojo2Json(vcrossConnect.getInfo(), false))
							.add(vcrossConnect.getStatus().getValue())
							.add(vcrossConnect.getTrailId())
							.add(vcrossConnect.getSwitchId())
							.add(vcrossConnect.getIngressPortId())
							.add(vcrossConnect.getEgressPortId());
					insert(ApiSql.INSERT_VCROSS_CONNECT, params, resultHandler);
				} else {
					resultHandler.handle(Future.failedFuture(res.cause()));
				}
		});
		return this;
	}

	@Override
	public TopologyService getVcrossConnectById(String vcrossConnectId, Handler<AsyncResult<VcrossConnect>> resultHandler) {
		findOne(ApiSql.FETCH_VCROSS_CONNECT_BY_ID, new JsonArray().add(vcrossConnectId)).map(json -> {
			return JsonUtils.json2Pojo(json.encode(), VcrossConnect.class);
		}).onComplete(resultHandler);
		return this;
	}

	@Override
	public TopologyService getVcrossConnectsByNode(String nodeId, Handler<AsyncResult<List<VcrossConnect>>> resultHandler) {
		JsonArray params = new JsonArray().add(nodeId);
		find(ApiSql.FETCH_VCROSS_CONNECTS_BY_NODE, params).map(rawList -> rawList.stream().map(row -> {
			return JsonUtils.json2Pojo(row.encode(), VcrossConnect.class);
		}).collect(Collectors.toList())).onComplete(resultHandler);
		return this;
	}
	
	@Override
	public TopologyService getVcrossConnectsByTrail(String trailId, Handler<AsyncResult<List<VcrossConnect>>> resultHandler) {
		JsonArray params = new JsonArray().add(trailId);
		find(ApiSql.FETCH_VCROSS_CONNECTS_BY_TRAIL, params).map(rawList -> rawList.stream().map(row -> {
			return JsonUtils.json2Pojo(row.encode(), VcrossConnect.class);
		}).collect(Collectors.toList())).onComplete(resultHandler);
		return this;
	}

	@Override
	public TopologyService deleteVcrossConnect(String vcrossConnectId, Handler<AsyncResult<Void>> resultHandler) {		
		delete(ApiSql.DELETE_VCROSS_CONNECT, new JsonArray().add(vcrossConnectId), resultHandler);
		return this;
	}
	
	@Override
	public TopologyService updateVcrossConnect(String id, VcrossConnect vcrossConnect, Handler<AsyncResult<Void>> resultHandler) {
		JsonArray params = new JsonArray()
				.add(vcrossConnect.getLabel())
				.add(vcrossConnect.getDescription())
				.add(JsonUtils.pojo2Json(vcrossConnect.getInfo(), false))
				.add(vcrossConnect.getStatus())
				.add(id);
		execute(ApiSql.UPDATE_VCROSS_CONNECT, params, resultHandler);
		return this;
	}


	/********** Prefix **********/
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
								Vltp[] ltps = JsonUtils.json2Pojo(new JsonArray(ar.result()).encode(), Vltp[].class);
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
		UUID opp[] = { Functional.getUUID(op) };
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
									Vlink vlink = JsonUtils.json2Pojo(ar.result().get(0).encode(), Vlink.class);
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
								Vctp[] vctps = JsonUtils.json2Pojo(new JsonArray(ar.result()).encode(), Vctp[].class);
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
		UUID opp[] = { Functional.getUUID(op) };
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
						return JsonUtils.json2Pojo(json.encode(), Vctp.class);
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
												VlinkConn vlc = JsonUtils.json2Pojo(ar.result().get(0).encode(), VlinkConn.class);
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
												Vconnection vcon = JsonUtils.json2Pojo(ar.result().get(0).encode(), Vconnection.class);
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
										Vctp[] vctps = JsonUtils.json2Pojo(new JsonArray(ar.result()).encode(), Vctp[].class);
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
		UUID opp[] = { Functional.getUUID(op) };
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
		UUID opp[] = { Functional.getUUID(op) };
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
		UUID opp[] = { Functional.getUUID(op) };
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
	
	@Override
	public TopologyService updateTrailStatus(int id, StatusEnum status, String op,
			Handler<AsyncResult<Void>> resultHandler) {
		UUID opp[] = { Functional.getUUID(op) };
		beginTransaction(Entity.TRAIL, opp[0], InternalSql.LOCK_TABLES).onComplete(tx -> {
			if (tx.succeeded()) {
				JsonArray params = new JsonArray().add(status.getValue()).add(id);
				transactionExecute(InternalSql.UPDATE_TRAIL_STATUS, params).onComplete(u -> {
					if (u.succeeded()) {
						commitTransaction(Entity.TRAIL, opp[0]).onComplete(resultHandler);
					} else {
						rollback();
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
	public TopologyService updateCrossConnectStatus(int id, StatusEnum status, String op,
			Handler<AsyncResult<Void>> resultHandler) {
		UUID opp[] = { Functional.getUUID(op) };
		beginTransaction(Entity.XC, opp[0], InternalSql.LOCK_TABLES).onComplete(tx -> {
			if (tx.succeeded()) {
				JsonArray params = new JsonArray().add(status.getValue()).add(id);
				transactionExecute(InternalSql.UPDATE_XC_STATUS, params).onComplete(u -> {
					if (u.succeeded()) {
						commitTransaction(Entity.XC, opp[0]).onComplete(resultHandler);
					} else {
						rollback();
						resultHandler.handle(Future.failedFuture(u.cause()));
					}
				});
			} else {
				resultHandler.handle(Future.failedFuture(tx.cause()));
			}
		});
		return this;
	}
	
	/* *************** Example network topology for optical Quantum mgmt **************** */
	private void loadBaseTopology(JsonObject baseTopology, Handler<AsyncResult<Void>> resultHandler) {
		// logger.info("Load example topology: " + baseTopology.encodePrettily());
		getVsubnetsByType(SubnetTypeEnum.QNET, res -> {
			if (res.succeeded()) {
				String subnetName = baseTopology.getString("name");
				if (!res.result().stream().anyMatch(e -> subnetName.equals(e.getName()))) {
					Vsubnet vsubnet = new Vsubnet();
					vsubnet.setName(subnetName);
					vsubnet.setLabel("example-quantum-network");
					vsubnet.setDescription("example-quantum-network");
					vsubnet.setType(SubnetTypeEnum.QNET);
					addVsubnet(vsubnet, sn -> {
						if (sn.succeeded()) {
							Promise<Void> pNetworkDone = Promise.promise();
							int subnetId = sn.result();
							JsonArray nodes = baseTopology.getJsonArray("nodes");
							JsonArray links = baseTopology.getJsonArray("links");
							JsonArray caps = baseTopology.getJsonArray("capabilities");
							JsonArray trails = baseTopology.getJsonArray("trails");
							HashMap<String,Integer> nodeIds = new HashMap<String,Integer>();
							HashMap<String,Integer> ltpIds = new HashMap<String,Integer>();
							// HashMap<String,Integer> linkIds = new HashMap<String,Integer>();
							HashMap<String,Integer> trailIds = new HashMap<String,Integer>();
							
							List<Future> allNodesAdded = new ArrayList<Future>();
							nodes.forEach(e -> {
								Promise<Void> pNodeAdded = Promise.promise();
								allNodesAdded.add(pNodeAdded.future());
								JsonObject jNode = (JsonObject) e;
								String switchName = jNode.getString("name");
								Vnode vnode = new Vnode();
								vnode.setName(switchName);
								vnode.setVsubnetId(subnetId);
								vnode.setLabel("");
								vnode.setDescription("");
								vnode.setLocation("");
								vnode.setHwaddr("");
								vnode.setType(NodeTypeEnum.OXC);
								vnode.setMgmtIp(jNode.getString("mgmtAddr"));
								vnode.setPosx(jNode.getJsonObject("position").getInteger("x"));
								vnode.setPosy(jNode.getJsonObject("position").getInteger("y"));
								vnode.setStatus(StatusEnum.DOWN);
								HashMap<String,Object> info = new HashMap<String,Object>();
								caps.forEach(c -> {
									JsonObject jCap = (JsonObject) c;
									if (jCap.getJsonObject("location").getString("node").equals(switchName)) {
										info.put(jCap.getString("name"), jCap.encode());
									}
								});
								vnode.setInfo(info);
								addVnode(vnode, ar -> {
									if (ar.succeeded()) {
										nodeIds.put(vnode.getName(), ar.result());
										List<Future> allPortsAdded = new ArrayList<Future>();
										jNode.getJsonArray("ports").forEach(el -> {
											String portName = (String) el;
											Promise<Void> pPortAdded = Promise.promise();
											allPortsAdded.add(pPortAdded.future());
											String fullPortName = vnode.getName() + "." + ((String) portName);
											Vltp vltp = new Vltp();
											vltp.setName(fullPortName);
											vltp.setVnodeId(ar.result());
											vltp.setPort(portName.substring(1));
											vltp.setStatus(StatusEnum.DOWN);
											vltp.setLabel("");
											vltp.setDescription("");
											vltp.setBandwidth("");
											vltp.setMtu(0);	
											vltp.setDirection(LtpDirectionEnum.INOUT);
											addVltp(vltp, ar2 -> {
												if (ar2.succeeded()) {
													ltpIds.put(vltp.getName(), ar2.result());
													pPortAdded.complete();
												} else {
													pPortAdded.fail(ar2.cause());
												}
											});
										});
										CompositeFuture.all(allPortsAdded).map((Void)null).onComplete(pNodeAdded);
									} else {
										pNodeAdded.fail(ar.cause());
									}
								});
							});
							CompositeFuture.all(allNodesAdded).onComplete(res2 -> {
								if (res2.succeeded()) {
									CompletableFuture<Void> stage = CompletableFuture.completedFuture(null);
									for (Object e: links) {
										stage = stage.thenCompose(r -> createLink((JsonObject) e, ltpIds));
									}
									stage.whenComplete((result, error) -> {
						            	if (error != null) {
						            		pNetworkDone.fail(error.getCause());
						                } else {
						                	// create Trails
						                	List<Future> allTrailsAdded = new ArrayList<Future>();
											trails.forEach(e -> {
												Promise<Void> pTrailAdded = Promise.promise();
												allTrailsAdded.add(pTrailAdded.future());
												JsonObject jTrail = (JsonObject) e;
												String trailName = jTrail.getString("name");
												Vtrail vtrail = new Vtrail();
												vtrail.setName(trailName);
												vtrail.setVsubnetId(subnetId);
												vtrail.setLabel("");
												vtrail.setDescription(jTrail.getString("description"));
												vtrail.setStatus(StatusEnum.UP);
												addVtrail(vtrail, ar -> {
													if (ar.succeeded()) {
														trailIds.put(vtrail.getName(), ar.result());
														CompletableFuture<Void> stage2 = CompletableFuture.completedFuture(null);
														for (Object ee: jTrail.getJsonArray("oxcs")) {
															JsonObject xc = (JsonObject) ee;
															String ingressPortName = xc.getString("switch") + "." + xc.getString("ingressPort");
															String egressPortName = xc.getString("switch") + "." + xc.getString("egressPort");
															VcrossConnect vxc = new VcrossConnect();
															vxc.setName(xc.getString("name"));
															vxc.setLabel("");
															vxc.setDescription("");
															vxc.setStatus(StatusEnum.UP);
															vxc.setTrailId(ar.result());
															vxc.setSwitchId(nodeIds.get(xc.getString("switch")));
															vxc.setIngressPortId(ltpIds.get(ingressPortName));
															vxc.setEgressPortId(ltpIds.get(egressPortName));

															stage2 = stage2.thenCompose(r -> createXc(vxc));
														}
														stage2.whenComplete((result2, error2) -> {
											            	if (error2 != null) {
											            		pTrailAdded.fail(error2.getCause());
											                } else {
											                	pTrailAdded.complete();
											                }
											            });
													} else {
														pTrailAdded.fail(ar.cause());
													}
												});
											});
											CompositeFuture.all(allTrailsAdded).onComplete(res3 -> {
												if (res3.succeeded()) {
													pNetworkDone.complete();
												} else {
													pNetworkDone.fail(res3.cause());
												}
											});
						                }
						            });
								} else {
									pNetworkDone.fail(res2.cause());
								}
							});
							// remove subnet if any error
							pNetworkDone.future().onComplete(done -> {
								if (done.succeeded()) {
									logger.info("Example network loaded.");
									resultHandler.handle(Future.succeededFuture());
								} else {
									logger.info("Failed to load example network.");
									resultHandler.handle(Future.failedFuture(done.cause()));
									deleteVsubnet(String.valueOf(subnetId), ignore -> {});
								}
							});
						} else {
							logger.info("Failed to create Vsubnet.");
							resultHandler.handle(Future.failedFuture(sn.cause()));
						}
					});
				} else {
					logger.info("This example optical network already exists.");
					resultHandler.handle(Future.succeededFuture());
				}
			} else {
				logger.info("Failed to get existing optical switches.");
				resultHandler.handle(Future.failedFuture(res.cause()));
			}
		});
	}
	
	private CompletableFuture<Void> createLink(JsonObject jLink, HashMap<String,Integer> ltpIds) {
		CompletableFuture<Void> cs = new CompletableFuture<>();
		String srcPortName = jLink.getString("srcSwitch")+"."+jLink.getString("srcPort");
		String dstPortName = jLink.getString("dstSwitch")+"."+jLink.getString("dstPort");
		Vlink vlink = new Vlink();
		vlink.setName(srcPortName+"-"+dstPortName);
		vlink.setLabel("");
		vlink.setDescription("");
		vlink.setSrcVltpId(ltpIds.get(srcPortName));
		vlink.setDestVltpId(ltpIds.get(dstPortName));
		vlink.setStatus(StatusEnum.DOWN);
		addVlink(vlink, ar3 -> {
			if (ar3.succeeded()) {
				cs.complete(null);
			} else {
				cs.completeExceptionally(ar3.cause());
			}
		});
		return cs;
	}
	
	private CompletableFuture<Void> createXc(VcrossConnect vxc) {
		CompletableFuture<Void> cs = new CompletableFuture<>();
		addVcrossConnect(vxc, ar -> {
			if (ar.succeeded()) {
				cs.complete(null);
			} else {
				cs.completeExceptionally(ar.cause());
			}
		});
		return cs;
	}
}