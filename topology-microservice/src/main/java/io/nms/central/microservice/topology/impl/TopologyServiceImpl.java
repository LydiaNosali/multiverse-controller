package io.nms.central.microservice.topology.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import io.nms.central.microservice.common.service.JdbcRepositoryWrapper;
import io.nms.central.microservice.topology.TopologyService;
import io.nms.central.microservice.topology.model.Edge;
import io.nms.central.microservice.topology.model.Face;
import io.nms.central.microservice.topology.model.ModelObjectMapper;
import io.nms.central.microservice.topology.model.Node;
import io.nms.central.microservice.topology.model.PrefixAnn;
import io.nms.central.microservice.topology.model.Route;
import io.nms.central.microservice.topology.model.Vctp;
import io.nms.central.microservice.topology.model.Vlink;
import io.nms.central.microservice.topology.model.VlinkConn;
import io.nms.central.microservice.topology.model.Vltp;
import io.nms.central.microservice.topology.model.Vnode;
import io.nms.central.microservice.topology.model.Vsubnet;
import io.nms.central.microservice.topology.model.Vtrail;
import io.nms.central.microservice.topology.model.Vxc;
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
import io.vertx.ext.sql.SQLConnection;

/**
 *
 */
public class TopologyServiceImpl extends JdbcRepositoryWrapper implements TopologyService {

	private static final Logger logger = LoggerFactory.getLogger(TopologyServiceImpl.class);
	private Routing routing;

	public TopologyServiceImpl(Vertx vertx, JsonObject config) {
		super(vertx, config);
		routing = new Routing();
	}

	@Override
	public TopologyService initializePersistence(Handler<AsyncResult<List<Integer>>> resultHandler) {
		List<String> statements = new ArrayList<String>();
		statements.add(ApiSql.CREATE_TABLE_VSUBNET);
		statements.add(ApiSql.CREATE_TABLE_VNODE);
		statements.add(ApiSql.CREATE_TABLE_VLTP);
		statements.add(ApiSql.CREATE_TABLE_VLINK);
		statements.add(ApiSql.CREATE_TABLE_VCTP);
		statements.add(ApiSql.CREATE_TABLE_VLINKCONN);
		statements.add(ApiSql.CREATE_TABLE_VTRAIL);
		statements.add(ApiSql.CREATE_TABLE_VXC);
		statements.add(ApiSql.CREATE_TABLE_PREFIX_ANN);
		statements.add(ApiSql.CREATE_TABLE_FACE);
		statements.add(ApiSql.CREATE_TABLE_ROUTE);
		client.getConnection(connHandler(resultHandler, connection -> {
			connection.batch(statements, r -> {
				resultHandler.handle(r);
				connection.close();
			});
		}));
		return this;
	}


	/********** Vsubnet **********/
	// INSERT_VSUBNET = "INSERT INTO Vsubnet (name, label, description, info, status) "
	@Override
	public TopologyService addVsubnet(Vsubnet vsubnet, Handler<AsyncResult<Integer>> resultHandler) {
		// logger.debug("addSubnet: "+vsubnet.toString());
		JsonArray params = new JsonArray()
				.add(vsubnet.getName())
				.add(vsubnet.getLabel())
				.add(vsubnet.getDescription())
				.add(new JsonObject(vsubnet.getInfo()).encode())
				.add(vsubnet.getStatus());
		insertAndGetId(params, ApiSql.INSERT_VSUBNET, resultHandler);
		return this;
	}
	@Override
	public TopologyService getVsubnet(String vsubnetId, Handler<AsyncResult<Vsubnet>> resultHandler) {
		this.retrieveOne(vsubnetId, ApiSql.FETCH_VSUBNET_BY_ID)
		.map(option -> option.map(json -> {
			Vsubnet vsubnet = new Vsubnet(json);
			vsubnet.setInfo(new JsonObject(json.getString("info")).getMap());
			return vsubnet;
		}).orElse(null))
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService getAllVsubnets(Handler<AsyncResult<List<Vsubnet>>> resultHandler) {
		this.retrieveAll(ApiSql.FETCH_ALL_VSUBNETS)
		.map(rawList -> rawList.stream()
				.map(row -> {
					Vsubnet vsubnet = new Vsubnet(row);
					vsubnet.setInfo(new JsonObject(row.getString("info")).getMap());
					return vsubnet;
				})
				.collect(Collectors.toList())
				)
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService deleteVsubnet(String vsubnetId, Handler<AsyncResult<Void>> resultHandler) {
		this.removeOne(vsubnetId, ApiSql.DELETE_VSUBNET, resultHandler);
		return this;
	}
	// UPDATE_VSUBNET = "UPDATE Vsubnet SET label = ?, description = ?, info = ?, status = ? WHERE id = ?";
	@Override
	public TopologyService updateVsubnet(String id, Vsubnet vsubnet, Handler<AsyncResult<Vsubnet>> resultHandler) {
		JsonArray params = new JsonArray()
				.add(vsubnet.getLabel())
				.add(vsubnet.getDescription())
				.add(new JsonObject(vsubnet.getInfo()).encode())
				.add(vsubnet.getStatus())
				.add(id);
		this.execute(params, ApiSql.UPDATE_VSUBNET, vsubnet, resultHandler);
		return this;
	}


	/********** Vnode **********/
	// INSERT_VNODE = "INSERT INTO Vnode (name, label, description, info, status, posx, posy, location, type, vsubnetId) "
	@Override
	public TopologyService addVnode(Vnode vnode, Handler<AsyncResult<Integer>> resultHandler) {
		JsonArray params = new JsonArray()
				.add(vnode.getName())
				.add(vnode.getLabel())
				.add(vnode.getDescription())
				.add(new JsonObject(vnode.getInfo()).encode())
				.add(vnode.getStatus())
				.add(vnode.getPosx())
				.add(vnode.getPosy())
				.add(vnode.getLocation())
				.add(vnode.getType())	
				.add(vnode.getVsubnetId());
		insertAndGetId(params, ApiSql.INSERT_VNODE, resultHandler);
		return this;
	}
	@Override
	public TopologyService getVnode(String vnodeId, Handler<AsyncResult<Vnode>> resultHandler) {
		JsonArray params = new JsonArray().add(vnodeId);
		this.retrieveMany(params, ApiSql.FETCH_VNODE_BY_ID)
		.map(rawList -> ModelObjectMapper.toVnodeFromJsonRows(rawList))
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService getAllVnodes(Handler<AsyncResult<List<Vnode>>> resultHandler) {
		this.retrieveAll(ApiSql.FETCH_ALL_VNODES)
		.map(rawList -> rawList.stream()
				.map(row -> {
					Vnode vnode = new Vnode(row);
					vnode.setInfo(new JsonObject(row.getString("info")).getMap());
					return vnode;
				})
				.collect(Collectors.toList())
				)
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService getVnodesByVsubnet(String vsubnetId, Handler<AsyncResult<List<Vnode>>> resultHandler) {
		JsonArray params = new JsonArray().add(vsubnetId);
		this.retrieveMany(params, ApiSql.FETCH_VNODES_BY_VSUBNET)
		.map(rawList -> rawList.stream()
				.map(Vnode::new)
				.collect(Collectors.toList())
				)
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService deleteVnode(String vnodeId, Handler<AsyncResult<Void>> resultHandler) {
		this.removeOne(vnodeId, ApiSql.DELETE_VNODE, resultHandler);
		return this;
	}
	// UPDATE_VNODE = "UPDATE Vnode SET label = ?, description = ?, info = ?, status = ?, posx = ?, posy = ?, location = ?, type = ? WHERE id = ?";
	@Override
	public TopologyService updateVnode(String id, Vnode vnode, Handler<AsyncResult<Vnode>> resultHandler) {
		JsonArray params = new JsonArray()
				.add(vnode.getLabel())
				.add(vnode.getDescription())
				.add(new JsonObject(vnode.getInfo()).encode())
				.add(vnode.getStatus())
				.add(vnode.getPosx())
				.add(vnode.getPosy())
				.add(vnode.getLocation())
				.add(vnode.getType())
				.add(id);
		this.execute(params, ApiSql.UPDATE_VNODE, vnode, resultHandler);
		return this;
	}


	/********** Vltp **********/
	// INSERT_VLTP = "INSERT INTO Vltp (name, label, description, info, status, busy, vnodeId) "
	@Override 
	public TopologyService addVltp(Vltp vltp, Handler<AsyncResult<Integer>> resultHandler) {
		JsonArray params = new JsonArray()
				.add(vltp.getName())
				.add(vltp.getLabel())
				.add(vltp.getDescription())
				.add(new JsonObject(vltp.getInfo()).encode())
				.add(vltp.getStatus())
				.add(vltp.isBusy())
				.add(vltp.getVnodeId());
		insertAndGetId(params, ApiSql.INSERT_VLTP, resultHandler);
		return this;
	}
	@Override
	public TopologyService getVltp(String vltpId, Handler<AsyncResult<Vltp>> resultHandler) {
		JsonArray params = new JsonArray().add(vltpId);
		this.retrieveMany(params, ApiSql.FETCH_VLTP_BY_ID)
		.map(rawList -> ModelObjectMapper.toVltpFromJsonRows(rawList))
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService getAllVltps(Handler<AsyncResult<List<Vltp>>> resultHandler) {
		this.retrieveAll(ApiSql.FETCH_ALL_VLTPS)
		.map(rawList -> rawList.stream()
				.map(row -> {
					Vltp vltp = new Vltp(row);
					vltp.setInfo(new JsonObject(row.getString("info")).getMap());
					return vltp;
				})
				.collect(Collectors.toList())
				)
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService getVltpsByVnode(String vnodeId, Handler<AsyncResult<List<Vltp>>> resultHandler) {
		JsonArray params = new JsonArray().add(vnodeId);
		this.retrieveMany(params, ApiSql.FETCH_VLTPS_BY_VNODE)
		.map(rawList -> rawList.stream()
				.map(row -> {
					Vltp vltp = new Vltp(row);
					vltp.setInfo(new JsonObject(row.getString("info")).getMap());
					return vltp;
				})
				.collect(Collectors.toList())
				)
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService deleteVltp(String vltpId, Handler<AsyncResult<Void>> resultHandler) {
		this.removeOne(vltpId, ApiSql.DELETE_VLTP, resultHandler);
		return this;
	}
	// UPDATE_VLTP = "UPDATE Vltp SET label = ?, description = ?, info = ?, status = ?, busy = ? WHERE id = ?";
	@Override
	public TopologyService updateVltp(String id, Vltp vltp, Handler<AsyncResult<Vltp>> resultHandler) {
		JsonArray params = new JsonArray()
				.add(vltp.getLabel())
				.add(vltp.getDescription())
				.add(new JsonObject(vltp.getInfo()).encode())
				.add(vltp.getStatus())
				.add(vltp.isBusy())				
				.add(id);
		this.execute(params, ApiSql.UPDATE_VLTP, vltp, resultHandler);
		return this;
	}


	/********** Vctp **********/
	// INSERT_VCTP = "INSERT INTO Vctp (name, label, description, info, status, vltpId) "
	@Override
	public TopologyService addVctp(Vctp vctp, Handler<AsyncResult<Integer>> resultHandler) {
		JsonArray params = new JsonArray()
				.add(vctp.getName())
				.add(vctp.getLabel())
				.add(vctp.getDescription())
				.add(new JsonObject(vctp.getInfo()).encode())
				.add(vctp.getStatus())
				.add(vctp.isBusy())
				.add(vctp.getVltpId());
		insertAndGetId(params, ApiSql.INSERT_VCTP, resultHandler);
		return this;
	}
	@Override
	public TopologyService getVctp(String vctpId, Handler<AsyncResult<Vctp>> resultHandler) {
		this.retrieveOne(vctpId, ApiSql.FETCH_VCTP_BY_ID)
		.map(option -> option.map(json -> {
			Vctp vctp = new Vctp(json);
			vctp.setInfo(new JsonObject(json.getString("info")).getMap());
			return vctp;
		}).orElse(null))
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService getAllVctps(Handler<AsyncResult<List<Vctp>>> resultHandler) {
		this.retrieveAll(ApiSql.FETCH_ALL_VCTPS)
		.map(rawList -> rawList.stream()
				.map(row -> {
					Vctp vctp = new Vctp(row);
					vctp.setInfo(new JsonObject(row.getString("info")).getMap());
					return vctp;
				})
				.collect(Collectors.toList())
				)
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService getVctpsByVltp(String vltpId, Handler<AsyncResult<List<Vctp>>> resultHandler) {
		JsonArray params = new JsonArray().add(vltpId);
		this.retrieveMany(params, ApiSql.FETCH_VCTPS_BY_VLTP)
		.map(rawList -> rawList.stream()
				.map(row -> {
					Vctp vctp = new Vctp(row);
					vctp.setInfo(new JsonObject(row.getString("info")).getMap());
					return vctp;
				})
				.collect(Collectors.toList())
				)
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService getVctpsByVnode(String vnodeId, Handler<AsyncResult<List<Vctp>>> resultHandler) {
		JsonArray params = new JsonArray().add(vnodeId);
		this.retrieveMany(params, ApiSql.FETCH_VCTPS_BY_VNODE)
		.map(rawList -> rawList.stream()
				.map(row -> {
					Vctp vctp = new Vctp(row);
					vctp.setInfo(new JsonObject(row.getString("info")).getMap());
					return vctp;
				})
				.collect(Collectors.toList())
				)
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService getVctpsByVlink(String vlinkId, Handler<AsyncResult<List<Vctp>>> resultHandler) {
		JsonArray params = new JsonArray().add(vlinkId);
		this.retrieveMany(params, ApiSql.FETCH_VCTPS_BY_VLINK)
		.map(rawList -> rawList.stream()
				.map(row -> {
					Vctp vctp = new Vctp(row);
					vctp.setInfo(new JsonObject(row.getString("info")).getMap());
					return vctp;
				})
				.collect(Collectors.toList())
				)
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService deleteVctp(String vctpId, Handler<AsyncResult<Void>> resultHandler) {
		this.removeOne(vctpId, ApiSql.DELETE_VCTP, resultHandler);
		return this;
	}
	// UPDATE_VCTP = "UPDATE Vctp SET label = ?, description = ?, info = ?, status = ? WHERE id = ?";
	@Override
	public TopologyService updateVctp(String id, Vctp vctp, Handler<AsyncResult<Vctp>> resultHandler) {
		JsonArray params = new JsonArray()
				.add(vctp.getLabel())
				.add(vctp.getDescription())
				.add(new JsonObject(vctp.getInfo()).encode())
				.add(vctp.getStatus())
				.add(vctp.isBusy())
				.add(id);
		this.execute(params, ApiSql.UPDATE_VCTP, vctp, resultHandler);
		return this;
	}


	/********** Vlink **********/
	// INSERT_VLINK = "INSERT INTO Vlink (name, label, description, info, status, type, srcVltpId, destVltpId) "
	@Override 
	public TopologyService addVlink(Vlink vlink, Handler<AsyncResult<Integer>> resultHandler) {
		JsonArray params = new JsonArray()
				.add(vlink.getName())
				.add(vlink.getLabel())
				.add(vlink.getDescription())
				.add(new JsonObject(vlink.getInfo()).encode())
				.add(vlink.getStatus())
				.add(vlink.getType())
				.add(vlink.getSrcVltpId())
				.add(vlink.getDestVltpId());
		JsonArray updSrcLtp = new JsonArray().add(true).add(vlink.getSrcVltpId());
		JsonArray updDestLtp = new JsonArray().add(true).add(vlink.getDestVltpId());
		
		Future<SQLConnection> f =  txnBegin();
		Future<Integer> fId = f.compose(r -> txnExecute(f.result(), ApiSql.INSERT_VLINK, params));
			fId.compose(r -> txnExecuteNoResult(f.result(), InternalSql.UPDATE_LTP_BUSY, updSrcLtp))
			.compose(r -> txnExecuteNoResult(f.result(), InternalSql.UPDATE_LTP_BUSY, updDestLtp))
			.compose(r -> txnEnd(f.result()))
			.map(fId.result())
			.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService getVlink(String vlinkId, Handler<AsyncResult<Vlink>> resultHandler) {
		JsonArray params = new JsonArray().add(vlinkId);
		this.retrieveMany(params, ApiSql.FETCH_VLINK_BY_ID)
		.map(rawList -> ModelObjectMapper.toVlinkFromJsonRows(rawList))
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService getAllVlinks(Handler<AsyncResult<List<Vlink>>> resultHandler) {
		this.retrieveAll(ApiSql.FETCH_ALL_VLINKS)
		.map(rawList -> rawList.stream()
				.map(row -> {
					Vlink vlink = new Vlink(row);
					vlink.setInfo(new JsonObject(row.getString("info")).getMap());
					return vlink;
				})
				.collect(Collectors.toList())
				)
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService getVlinksByVsubnet(String vsubnetId, Handler<AsyncResult<List<Vlink>>> resultHandler) {
		JsonArray params = new JsonArray().add(vsubnetId);
		this.retrieveMany(params, ApiSql.FETCH_VLINKS_BY_VSUBNET)
		.map(rawList -> rawList.stream()
				.map(row -> {
					Vlink vlink = new Vlink(row);
					vlink.setInfo(new JsonObject(row.getString("info")).getMap());
					return vlink;
				})
				.collect(Collectors.toList())
				)
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService deleteVlink(String vlinkId, Handler<AsyncResult<Void>> resultHandler) {
		retrieveOne(vlinkId, ApiSql.FETCH_VLINK_BY_ID)
			.map(option -> option.map(Vlink::new).orElse(null))
			.onComplete(ar -> {
				if (ar.result() != null) {
					JsonArray delLink = new JsonArray().add(vlinkId);
					JsonArray updSrcLtp = new JsonArray().add(false).add(ar.result().getSrcVltpId());
					JsonArray updDestLtp = new JsonArray().add(false).add(ar.result().getDestVltpId());
					Future<SQLConnection> f =  txnBegin();
					f.compose(r -> txnExecuteNoResult(f.result(), ApiSql.DELETE_VLINK, delLink))
						.compose(r -> txnExecuteNoResult(f.result(), InternalSql.UPDATE_LTP_BUSY, updSrcLtp))
						.compose(r -> txnExecuteNoResult(f.result(), InternalSql.UPDATE_LTP_BUSY, updDestLtp))
						.compose(r -> txnEnd(f.result()))
						.onComplete(resultHandler);
				} else {
					resultHandler.handle(Future.failedFuture("Vlink not found"));
				}
			});
		return this;
	}
	// UPDATE_VLINK = "UPDATE Vlink SET label = ?, description = ?, info = ?, status = ?, type = ? WHERE id = ?";
	@Override
	public TopologyService updateVlink(String id, Vlink vlink, Handler<AsyncResult<Vlink>> resultHandler) {
		JsonArray params = new JsonArray()
				.add(vlink.getLabel())
				.add(vlink.getDescription())
				.add(new JsonObject(vlink.getInfo()).encode())
				.add(vlink.getStatus())
				.add(vlink.getType())
				.add(id);
		this.execute(params, ApiSql.UPDATE_VLINK, vlink, resultHandler);
		return this;
	}


	/********** VlinkConn **********/
	// INSERT_VLINKCONN = "INSERT INTO VlinkConn (name, label, description, info, status, srcVctpId, destVctpId) "
	@Override
	public TopologyService addVlinkConn(VlinkConn vlinkConn, Handler<AsyncResult<Integer>> resultHandler) {
			generateCtps(vlinkConn)
			.onComplete(ar -> {
				if (ar.succeeded()) {
					List<Vctp> vctps = ar.result();
					JsonArray srcVctp = new JsonArray()
							.add(vctps.get(0).getName())
							.add(vctps.get(0).getLabel())
							.add(vctps.get(0).getDescription())
							.add(new JsonObject().encode())
							.add(vctps.get(0).getStatus())
							.add(vctps.get(0).isBusy())
							.add(vctps.get(0).getVltpId());
					JsonArray destVctp = new JsonArray()
							.add(vctps.get(1).getName())
							.add(vctps.get(1).getLabel())
							.add(vctps.get(1).getDescription())
							.add(new JsonObject().encode())
							.add(vctps.get(1).getStatus())
							.add(vctps.get(1).isBusy())
							.add(vctps.get(1).getVltpId());
					
					Future<SQLConnection> f = txnBegin();
					Future<Integer> sCtpId = f.compose(r -> txnExecute(f.result(), ApiSql.INSERT_VCTP, srcVctp));
					Future<Integer> dCtpId = f.compose(r -> txnExecute(f.result(), ApiSql.INSERT_VCTP, destVctp));
					
					CompositeFuture.all(sCtpId, dCtpId).onComplete(res -> {
						if (res.succeeded()) {
							vlinkConn.setSrcVctpId(sCtpId.result());
							vlinkConn.setDestVctpId(dCtpId.result());
							JsonArray vlc = new JsonArray()
									.add(vlinkConn.getName())
									.add(vlinkConn.getLabel())
									.add(vlinkConn.getDescription())
									.add(new JsonObject(vlinkConn.getInfo()).encode())
									.add(vlinkConn.getStatus())
									.add(vlinkConn.getSrcVctpId())
									.add(vlinkConn.getDestVctpId())
									.add(vlinkConn.getVlinkId());
							Future<Integer> fId = txnExecute(f.result(), ApiSql.INSERT_VLINKCONN, vlc);
								fId.compose(r -> txnEnd(f.result()))
								.map(fId.result())
								.onComplete(resultHandler);
						} else {
							resultHandler.handle(Future.failedFuture("Failed to create CTPs"));
						}
					});		
				} else {
					resultHandler.handle(Future.failedFuture(ar.cause()));
				}
			});
		return this;
	}
	@Override
	public TopologyService getVlinkConn(String vlinkConnId, Handler<AsyncResult<VlinkConn>> resultHandler) {
		this.retrieveOne(vlinkConnId, ApiSql.FETCH_VLINKCONN_BY_ID)
		.map(option -> option.map(json -> {
			VlinkConn vlinkConn = new VlinkConn(json);
			vlinkConn.setInfo(new JsonObject(json.getString("info")).getMap());
			return vlinkConn;
		}).orElse(null))
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService getAllVlinkConns(Handler<AsyncResult<List<VlinkConn>>> resultHandler) {
		this.retrieveAll(ApiSql.FETCH_ALL_VLINKCONNS)
		.map(rawList -> rawList.stream()
				.map(row -> {
					VlinkConn vlinkConn = new VlinkConn(row);
					vlinkConn.setInfo(new JsonObject(row.getString("info")).getMap());
					return vlinkConn;
				})
				.collect(Collectors.toList())
				)
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService getVlinkConnsByVlink(String vlinkId, Handler<AsyncResult<List<VlinkConn>>> resultHandler) {
		JsonArray params = new JsonArray().add(vlinkId);
		this.retrieveMany(params, ApiSql.FETCH_VLINKCONNS_BY_VLINK)
		.map(rawList -> rawList.stream()
				.map(row -> {
					VlinkConn vlinkConn = new VlinkConn(row);
					vlinkConn.setInfo(new JsonObject(row.getString("info")).getMap());
					return vlinkConn;
				})
				.collect(Collectors.toList())
				)
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService getVlinkConnsByVsubnet(String vsubnetId, Handler<AsyncResult<List<VlinkConn>>> resultHandler) {
		JsonArray params = new JsonArray().add(vsubnetId);
		this.retrieveMany(params, ApiSql.FETCH_VLINKCONNS_BY_VSUBNET)
		.map(rawList -> rawList.stream()
				.map(row -> {
					VlinkConn vlinkConn = new VlinkConn(row);
					vlinkConn.setInfo(new JsonObject(row.getString("info")).getMap());
					return vlinkConn;
				})
				.collect(Collectors.toList())
				)
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService deleteVlinkConn(String vlinkConnId, Handler<AsyncResult<Void>> resultHandler) {
		retrieveOne(vlinkConnId, ApiSql.FETCH_VLINKCONN_BY_ID)
		.map(option -> option.map(VlinkConn::new).orElse(null))
		.onComplete(ar -> {
			if (ar.result() != null) {
				JsonArray delLc = new JsonArray().add(vlinkConnId);
				JsonArray delSrcVctp = new JsonArray().add(ar.result().getSrcVctpId());
				JsonArray delDestVctp = new JsonArray().add(ar.result().getDestVctpId());
				Future<SQLConnection> f = txnBegin();
				f.compose(r -> txnExecuteNoResult(f.result(), ApiSql.DELETE_VLINKCONN, delLc))
					.compose(r -> txnExecuteNoResult(f.result(), ApiSql.DELETE_VCTP, delSrcVctp))
					.compose(r -> txnExecuteNoResult(f.result(), ApiSql.DELETE_VCTP, delDestVctp))
					.compose(r -> txnEnd(f.result()))
					.onComplete(resultHandler);	
			} else {
				resultHandler.handle(Future.failedFuture("VlinkConn not found"));
			}
		});
		return this;
	}
	// UPDATE_VLINKCONN = "UPDATE VlinkConn SET label = ?, description = ?, info = ?, status = ? WHERE id = ?";
	@Override
	public TopologyService updateVlinkConn(String id, VlinkConn vlinkConn, Handler<AsyncResult<VlinkConn>> resultHandler) {
		JsonArray params = new JsonArray()
				.add(vlinkConn.getLabel())
				.add(vlinkConn.getDescription())
				.add(new JsonObject(vlinkConn.getInfo()).encode())
				.add(vlinkConn.getStatus())
				.add(id);
		this.execute(params, ApiSql.UPDATE_VLINKCONN, vlinkConn, resultHandler);
		return this;
	}


	/********** Vtrail **********/
	// INSERT_VTRAIL = "INSERT INTO Vtrail (name, label, description, info, status, srcVctpId, destVctpId) "
	@Override
	public TopologyService addVtrail(Vtrail vtrail, Handler<AsyncResult<Integer>> resultHandler) {
		JsonArray params = new JsonArray()
				.add(vtrail.getName())
				.add(vtrail.getLabel())
				.add(vtrail.getDescription())
				.add(new JsonObject(vtrail.getInfo()).encode())
				.add(vtrail.getStatus())
				.add(vtrail.getSrcVctpId())
				.add(vtrail.getDestVctpId());
		insertAndGetId(params, ApiSql.INSERT_VTRAIL, resultHandler);
		return this;
	}
	@Override
	public TopologyService getVtrail(String vtrailId, Handler<AsyncResult<Vtrail>> resultHandler) {
		JsonArray params = new JsonArray().add(vtrailId);
		this.retrieveMany(params, ApiSql.FETCH_VTRAIL_BY_ID)
		.map(rawList -> ModelObjectMapper.toVtrailFromJsonRows(rawList))
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService getAllVtrails(Handler<AsyncResult<List<Vtrail>>> resultHandler) {
		this.retrieveAll(ApiSql.FETCH_ALL_VTRAILS)
		.map(rawList -> rawList.stream()
				.map(row -> {
					Vtrail vtrail = new Vtrail(row);
					vtrail.setInfo(new JsonObject(row.getString("info")).getMap());
					return vtrail;
				})
				.collect(Collectors.toList())
				)
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService getVtrailsByVsubnet(String vsubnetId, Handler<AsyncResult<List<Vtrail>>> resultHandler) {
		JsonArray params = new JsonArray().add(vsubnetId);
		this.retrieveMany(params, ApiSql.FETCH_VTRAILS_BY_VSUBNET)
		.map(rawList -> rawList.stream()
				.map(row -> {
					Vtrail vtrail = new Vtrail(row);
					vtrail.setInfo(new JsonObject(row.getString("info")).getMap());
					return vtrail;
				})
				.collect(Collectors.toList())
				)
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService deleteVtrail(String vtrailId, Handler<AsyncResult<Void>> resultHandler) {
		this.removeOne(vtrailId, ApiSql.DELETE_VTRAIL, resultHandler);
		return this;
	}
	// UPDATE_VTRAIL = "UPDATE Vtrail SET label = ?, description = ?, info = ?, status = ? WHERE id = ?";
	@Override
	public TopologyService updateVtrail(String id, Vtrail vtrail, Handler<AsyncResult<Vtrail>> resultHandler) {
		JsonArray params = new JsonArray()
				.add(vtrail.getLabel())
				.add(vtrail.getDescription())
				.add(new JsonObject(vtrail.getInfo()).encode())
				.add(vtrail.getStatus())
				.add(id);
		this.execute(params, ApiSql.UPDATE_VTRAIL, vtrail, resultHandler);
		return this;
	}


	/********** Vxc **********/
	// INSERT_VXC = "INSERT INTO Vxc (name, label, description, info, status, type, vnodeId, vtrailId, srcVctpId, destVctpId, dropVctpId) "
	@Override
	public TopologyService addVxc(Vxc vxc, Handler<AsyncResult<Integer>> resultHandler) {
		JsonArray params = new JsonArray()
				.add(vxc.getName())
				.add(vxc.getLabel())
				.add(vxc.getDescription())
				.add(new JsonObject(vxc.getInfo()).encode())
				.add(vxc.getStatus())
				.add(vxc.getType())
				.add(vxc.getVnodeId())
				.add(vxc.getVtrailId())
				.add(vxc.getSrcVctpId())
				.add(vxc.getDestVctpId());				
		if(vxc.getDropVctpId() == 0) {
			insertAndGetId(params, ApiSql.INSERT_VXC_1, resultHandler);
		} else {
			params.add(vxc.getDropVctpId());
			insertAndGetId(params, ApiSql.INSERT_VXC, resultHandler);
		}
		return this;
	}
	@Override
	public TopologyService getVxc(String vxcId, Handler<AsyncResult<Vxc>> resultHandler) {
		this.retrieveOne(vxcId, ApiSql.FETCH_VXC_BY_ID)
		.map(option -> option.map(json -> {
			Vxc vxc = new Vxc(json);
			vxc.setInfo(new JsonObject(json.getString("info")).getMap());
			return vxc;
		}).orElse(null))
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService getAllVxcs(Handler<AsyncResult<List<Vxc>>> resultHandler) {
		this.retrieveAll(ApiSql.FETCH_ALL_VXCS)
		.map(rawList -> rawList.stream()
				.map(row -> {
					Vxc vxc = new Vxc(row);
					vxc.setInfo(new JsonObject(row.getString("info")).getMap());
					return vxc;
				})
				.collect(Collectors.toList())
				)
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService getVxcsByVtrail(String vtrailId, Handler<AsyncResult<List<Vxc>>> resultHandler) {
		JsonArray params = new JsonArray().add(vtrailId);
		this.retrieveMany(params, ApiSql.FETCH_VXC_BY_VTRAIL)
		.map(rawList -> rawList.stream()
				.map(row -> {
					Vxc vxc = new Vxc(row);
					vxc.setInfo(new JsonObject(row.getString("info")).getMap());
					return vxc;
				})
				.collect(Collectors.toList())
				)
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService getVxcsByVnode(String vnodeId, Handler<AsyncResult<List<Vxc>>> resultHandler) {
		JsonArray params = new JsonArray().add(vnodeId);
		this.retrieveMany(params, ApiSql.FETCH_VXC_BY_VNODE)
		.map(rawList -> rawList.stream()
				.map(row -> {
					Vxc vxc = new Vxc(row);
					vxc.setInfo(new JsonObject(row.getString("info")).getMap());
					return vxc;
				})
				.collect(Collectors.toList())
				)
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService deleteVxc(String vxcId, Handler<AsyncResult<Void>> resultHandler) {
		this.removeOne(vxcId, ApiSql.DELETE_VXC, resultHandler);
		return this;
	}
	// UPDATE_VXC = "UPDATE Vxc SET label = ?, description = ?, info = ?, status = ?, type = ? WHERE id = ?";
	@Override
	public TopologyService updateVxc(String id, Vxc vxc, Handler<AsyncResult<Vxc>> resultHandler) {
		JsonArray params = new JsonArray()
				.add(vxc.getLabel())
				.add(vxc.getDescription())
				.add(new JsonObject(vxc.getInfo()).encode())
				.add(vxc.getStatus())
				.add(vxc.getType())
				.add(id);
		this.execute(params, ApiSql.UPDATE_VXC, vxc, resultHandler);
		return this;
	}

	
	/********** PrefixAnn **********/
	// INSERT_PREFIX_ANN = "INSERT INTO PrefixAnn (name, originId, expiration) 
	@Override
	public TopologyService addPrefixAnn(PrefixAnn prefixAnn, Handler<AsyncResult<Integer>> resultHandler) {
		JsonArray params = new JsonArray()
				.add(prefixAnn.getName())
				.add(prefixAnn.getOriginId());
		insertAndGetId(params, ApiSql.INSERT_PREFIX_ANN, resultHandler);
		return this;
	}
	@Override
	public TopologyService getPrefixAnn(String prefixAnnId, Handler<AsyncResult<PrefixAnn>> resultHandler) {
		this.retrieveOne(prefixAnnId, ApiSql.FETCH_PREFIX_ANN_BY_ID)
		.map(option -> option.map(json -> {
			PrefixAnn pa = new PrefixAnn(json);
			return pa;
		}).orElse(null))
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService getAllPrefixAnns(Handler<AsyncResult<List<PrefixAnn>>> resultHandler) {
		this.retrieveAll(ApiSql.FETCH_ALL_PREFIX_ANNS)
		.map(rawList -> rawList.stream()
				.map(PrefixAnn::new)
				.collect(Collectors.toList()))
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService getPrefixAnnsByVsubnet(String vsubnetId, Handler<AsyncResult<List<PrefixAnn>>> resultHandler) {
		JsonArray params = new JsonArray().add(vsubnetId);
		this.retrieveMany(params, ApiSql.FETCH_PREFIX_ANNS_BY_VSUBNET)
		.map(rawList -> rawList.stream()
				.map(PrefixAnn::new)
				.collect(Collectors.toList()))
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService getPrefixAnnsByVnode(String nodeId, Handler<AsyncResult<List<PrefixAnn>>> resultHandler) {
		JsonArray params = new JsonArray().add(nodeId);
		this.retrieveMany(params, ApiSql.FETCH_PREFIX_ANNS_BY_NODE)
		.map(rawList -> rawList.stream()
				.map(PrefixAnn::new)
				.collect(Collectors.toList())
				)
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService deletePrefixAnn(String prefixAnnId, Handler<AsyncResult<Void>> resultHandler) {
		this.removeOne(prefixAnnId, ApiSql.DELETE_PREFIX_ANN, resultHandler);
		return this;
	}

	
	/********** PrefixAnn **********/
	// INSERT_ROUTING_ENTRY = "INSERT INTO RoutingEntry (paId, nodeId, nextHopId, faceId, cost, origin)
	@Override
	public TopologyService addRoute(Route route, Handler<AsyncResult<Integer>> resultHandler) {
		JsonArray params = new JsonArray()
				.add(route.getPaId())
				.add(route.getNodeId())
				.add(route.getNextHopId())
				.add(route.getFaceId())
				.add(route.getCost())				
				.add(route.getOrigin());				
		insertAndGetId(params, ApiSql.INSERT_ROUTE, resultHandler);
		return this;
	}
	@Override
	public TopologyService getRoute(String routeId, Handler<AsyncResult<Route>> resultHandler) {
		this.retrieveOne(routeId, ApiSql.FETCH_ROUTE_BY_ID)
		.map(option -> option.map(Route::new).orElse(null))
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService getAllRoutes(Handler<AsyncResult<List<Route>>> resultHandler) {
		this.retrieveAll(ApiSql.FETCH_ALL_ROUTES)
		.map(rawList -> rawList.stream()
				.map(Route::new)
				.collect(Collectors.toList()))
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService getRoutesByVsubnet(String vsubnetId, Handler<AsyncResult<List<Route>>> resultHandler) {
		JsonArray params = new JsonArray().add(vsubnetId);
		this.retrieveMany(params, ApiSql.FETCH_ROUTES_BY_VSUBNET)
		.map(rawList -> rawList.stream()
				.map(Route::new)
				.collect(Collectors.toList()))
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService getRoutesByNode(String nodeId, Handler<AsyncResult<List<Route>>> resultHandler) {
		JsonArray params = new JsonArray().add(nodeId);
		this.retrieveMany(params, ApiSql.FETCH_ROUTES_BY_NODE)
		.map(rawList -> rawList.stream()
				.map(Route::new)
				.collect(Collectors.toList())
				)
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService deleteRoute(String routeId, Handler<AsyncResult<Void>> resultHandler) {
		this.removeOne(routeId, ApiSql.DELETE_ROUTE, resultHandler);
		return this;
	}
	@Override
	public TopologyService generateRoutesToPrefix(String name, Handler<AsyncResult<Void>> resultHandler) { 
		Future<List<Node>> nodes = this.retrieveAll(InternalSql.FETCH_ROUTEGEN_NODES)
				.map(rawList -> rawList.stream()
						.map(Node::new)
						.collect(Collectors.toList()));
		Future<List<Edge>> edges =this.retrieveAll(InternalSql.FETCH_ROUTEGEN_LCS)
				.map(rawList -> rawList.stream()
						.map(Edge::new)
						.collect(Collectors.toList()));
		JsonArray params = new JsonArray().add(name);
		Future<List<PrefixAnn>> pas = this.retrieveMany(params, InternalSql.FETCH_ROUTEGEN_PAS_BY_NAME)
				.map(rawList -> rawList.stream()
						.map(PrefixAnn::new)
						.collect(Collectors.toList()));
		
		CompositeFuture.all(Arrays.asList(nodes, edges, pas))
			.compose(r -> routing.computeRoutes(nodes.result(), edges.result(), pas.result()))
			.compose(routes -> upsertRoutes(routes))
			.onComplete(resultHandler);			
		return this;
	}

	
	/********** Face **********/
	// INSERT INTO Face (label, local, remote, scheme, vctpId, vlinkConnId)
	@Override
	public TopologyService addFace(Face face, Handler<AsyncResult<Integer>> resultHandler) {
		JsonArray params = new JsonArray()
				.add(face.getLabel())
				.add(face.getLocal())
				.add(face.getRemote())
				.add(face.getScheme())
				.add(face.getVctpId())
				.add(face.getVlinkConnId());
		insertAndGetId(params, ApiSql.INSERT_FACE, resultHandler);
		return this;
	}
	@Override
	public TopologyService getFace(String faceId, Handler<AsyncResult<Face>> resultHandler) {
		this.retrieveOne(faceId, ApiSql.FETCH_FACE_BY_ID)
			.map(option -> option.map(Face::new).orElse(null))
			.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService getAllFaces(Handler<AsyncResult<List<Face>>> resultHandler) {
		this.retrieveAll(ApiSql.FETCH_ALL_FACES)
			.map(rawList -> rawList.stream().map(Face::new)
				.collect(Collectors.toList()))
			.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService getFacesByVsubnet(String vsubnetId, Handler<AsyncResult<List<Face>>> resultHandler) {
		JsonArray params = new JsonArray().add(vsubnetId);
		this.retrieveMany(params, ApiSql.FETCH_FACES_BY_VSUBNET)
		.map(rawList -> rawList.stream()
				.map(Face::new)
				.collect(Collectors.toList()))
		.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService getFacesByNode(String nodeId, Handler<AsyncResult<List<Face>>> resultHandler) {
		JsonArray params = new JsonArray().add(nodeId);
		this.retrieveMany(params, ApiSql.FETCH_FACES_BY_NODE)
			.map(rawList -> rawList.stream().map(Face::new)
				.collect(Collectors.toList()))
			.onComplete(resultHandler);
		return this;
	}
	@Override
	public TopologyService deleteFace(String faceId, Handler<AsyncResult<Void>> resultHandler) {
		this.removeOne(faceId, ApiSql.DELETE_FACE, resultHandler);
		return this;
	}
	@Override
	public TopologyService generateFacesForLc(String linkConnId, Handler<AsyncResult<Void>> resultHandler) {
		generateFaces(linkConnId)
			.compose(faces -> upsertFaces(faces))
			.onComplete(resultHandler);			
		return this;
	}
	

	
	/* ---------------- BG ------------------ */
	private Future<List<Vctp>> generateCtps(VlinkConn vlc) {
		Promise<List<Vctp>> promise = Promise.promise();
		retrieveOne(vlc.getVlinkId(), InternalSql.FETCH_CTPGEN_INFO).onComplete(ar -> {
			if (ar.succeeded()) {
				if (ar.result().isPresent()) {
					JsonObject info = ar.result().get();
					Integer sLtpId = info.getInteger("sLtpId");
					Integer dLtpId = info.getInteger("dLtpId");			
					
					if (sLtpId != null && dLtpId != null ) {
						List<Vctp> ctps = new ArrayList<Vctp>(2);
						String lcName = vlc.getName().split("\\|")[1];
						String sLtpName = info.getString("sLtpName", "");
						String dLtpName = info.getString("dLtpName", "");
				
						Vctp ctp1 = new Vctp();
						ctp1.setName(sLtpName + "|" + lcName);
						ctp1.setLabel("autogen ctp");
						ctp1.setDescription("");
						ctp1.setBusy(true);
						ctp1.setVltpId(sLtpId);
						ctps.add(ctp1);
			
						Vctp ctp2 = new Vctp();
						ctp2.setName(dLtpName + "|" + lcName);
						ctp2.setLabel("autogen ctp");
						ctp2.setDescription("");
						ctp2.setBusy(true);
						ctp2.setVltpId(dLtpId);
						ctps.add(ctp2);
						
						promise.complete(ctps);
					} else {
						promise.fail("No Vlink info found");
					}
				} else {
					promise.fail("No Vlink info found");
				}					
			} else {
				promise.fail(ar.cause());
			}
		});
		return promise.future();
	}
	
	private Future<List<Face>> generateFaces(String vlcId) {
		Promise<List<Face>> promise = Promise.promise();
		
		retrieveOne(vlcId, InternalSql.FETCH_FACEGEN_INFO).onComplete(ar -> {
			if (ar.succeeded()) {
				if (ar.result().isPresent()) {
					JsonObject info = ar.result().get();
					String sLtpPort = info.getString("sLtpPort");
					String dLtpPort = info.getString("dLtpPort");
					
					List<Face> faces = new ArrayList<Face>(2);
					
					if (sLtpPort != null && dLtpPort != null ) {
						Face face1 = new Face();
						face1.setLabel("autogen");
						face1.setScheme("ether");
						face1.setVlinkConnId(info.getInteger("vlinkConnId"));
						face1.setVctpId(info.getInteger("sVctpId"));						
						face1.setLocal(sLtpPort);
						face1.setRemote(dLtpPort);
						faces.add(face1);
					
						Face face2 = new Face();
						face2.setLabel("autogen");
						face2.setScheme("ether");
						face2.setVlinkConnId(info.getInteger("vlinkConnId"));
						face2.setVctpId(info.getInteger("dVctpId"));						
						face2.setLocal(dLtpPort);
						face2.setRemote(sLtpPort);
						faces.add(face2);
						
						promise.complete(faces);
					}					
					else {
						promise.fail("No Vlink info found");
					}
				} else {
					promise.fail("No Vlink info found");
				}					
			} else {
				promise.fail(ar.cause());
			}
		});
		return promise.future();
	}
	
	private Future<Void> upsertRoutes(List<Route> routes) {
		// TODO: TXN...
		Promise<Void> promise = Promise.promise();
		List<Future> fts = new ArrayList<>();
		for (Route r : routes) {
			Promise<Void> p = Promise.promise();
			fts.add(p.future());
			JsonArray params = new JsonArray()
					.add(r.getPaId())
					.add(r.getNodeId())
					.add(r.getNextHopId())
					.add(r.getFaceId())
					.add(r.getCost())				
					.add(r.getOrigin());
			executeNoResult(params, InternalSql.UPDATE_ROUTE, p.future());
		}
		CompositeFuture.all(fts).map((Void) null).onComplete(promise);
		return promise.future();
	}
	
	private Future<Void> upsertFaces(List<Face> faces) {
		Promise<Void> promise = Promise.promise();
		List<Future> fts = new ArrayList<>();
		for (Face face : faces) {
			Promise<Void> p = Promise.promise();
			fts.add(p.future());
			JsonArray params = new JsonArray()
					.add(face.getLabel())
					.add(face.getLocal())
					.add(face.getRemote())
					.add(face.getScheme())
					.add(face.getVctpId())
					.add(face.getVlinkConnId());
			executeNoResult(params, InternalSql.UPDATE_FACE, p.future());
		}
		CompositeFuture.all(fts).map((Void) null).onComplete(promise);
		return promise.future();
	}
}


