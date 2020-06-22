package io.nms.central.microservice.topology;

import java.util.List;

import io.nms.central.microservice.topology.model.Vctp;
import io.nms.central.microservice.topology.model.Vlink;
import io.nms.central.microservice.topology.model.VlinkConn;
import io.nms.central.microservice.topology.model.Vltp;
import io.nms.central.microservice.topology.model.Vnode;
import io.nms.central.microservice.topology.model.Vsubnet;
import io.nms.central.microservice.topology.model.Vtrail;
import io.nms.central.microservice.topology.model.Vxc;
import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

/**
 * A service interface managing products.
 * <p>
 * This service is an event bus service (aka. service proxy)
 * </p>
 *
 * @author Eric Zhao
 */
@VertxGen
@ProxyGen
public interface TopologyService {

	/**
	 * The name of the event bus service.
	 */
	String SERVICE_NAME = "topology-eb-service";

	/**
	 * The address on which the service is published.
	 */
	String SERVICE_ADDRESS = "service.topology";
	
	@Fluent	
	TopologyService initializePersistence(Handler<AsyncResult<List<Integer>>> resultHandler);

	
	/* Vsubnet */
	@Fluent	
	TopologyService addVsubnet(Vsubnet vsubnet, Handler<AsyncResult<Void>> resultHandler);
	
	@Fluent	
	TopologyService getVsubnet(String vsubnetId, Handler<AsyncResult<Vsubnet>> resultHandler);
	
	@Fluent
	TopologyService getAllVsubnets(Handler<AsyncResult<List<Vsubnet>>> resultHandler);
	
	@Fluent	
	TopologyService deleteVsubnet(String vsubnetId, Handler<AsyncResult<Void>> resultHandler);
	
	@Fluent 
	TopologyService updateVsubnet(Vsubnet vsubnet, Handler<AsyncResult<Vsubnet>> resultHandler);
	
	
	/* Vnode */
	@Fluent	
	TopologyService addVnode(Vnode vnode, Handler<AsyncResult<Void>> resultHandler);
	
	@Fluent	
	TopologyService getVnode(String vnodeId, Handler<AsyncResult<Vnode>> resultHandler);
	
	@Fluent	
	TopologyService getAllVnodes(Handler<AsyncResult<List<Vnode>>> resultHandler);
	
	@Fluent	
	TopologyService getVnodesByVsubnet(String vsubnetId, Handler<AsyncResult<List<Vnode>>> resultHandler);
	
	@Fluent	
	TopologyService deleteVnode(String vnodeId, Handler<AsyncResult<Void>> resultHandler);
	
	@Fluent 
	TopologyService updateVnode(Vnode vnode, Handler<AsyncResult<Vnode>> resultHandler);
	
	
	/* Vltp */
	@Fluent	
	TopologyService addVltp(Vltp vltp, Handler<AsyncResult<Void>> resultHandler);
	
	@Fluent	
	TopologyService getVltp(String vltpId, Handler<AsyncResult<Vltp>> resultHandler);
	
	@Fluent	
	TopologyService getAllVltps(Handler<AsyncResult<List<Vltp>>> resultHandler);
	
	@Fluent	
	TopologyService getVltpsByVnode(String vnodeId, Handler<AsyncResult<List<Vltp>>> resultHandler);
	
	@Fluent	
	TopologyService deleteVltp(String vltpId, Handler<AsyncResult<Void>> resultHandler);
	
	@Fluent 
	TopologyService updateVltp(Vltp vltp, Handler<AsyncResult<Vltp>> resultHandler);
	

	/* Vctp */
	@Fluent	
	TopologyService addVctp(Vctp vctp, Handler<AsyncResult<Void>> resultHandler);
	
	@Fluent	
	TopologyService getVctp(String vctpId, Handler<AsyncResult<Vctp>> resultHandler);
	
	@Fluent	
	TopologyService getAllVctps(Handler<AsyncResult<List<Vctp>>> resultHandler);
	
	@Fluent	
	TopologyService getVctpsByVltp(String vltpId, Handler<AsyncResult<List<Vctp>>> resultHandler);
	
	@Fluent	
	TopologyService deleteVctp(String vctpId, Handler<AsyncResult<Void>> resultHandler);
	
	@Fluent 
	TopologyService updateVctp(Vctp vctp, Handler<AsyncResult<Vctp>> resultHandler);
	
	
	/* Vlink */
	@Fluent	
	TopologyService addVlink(Vlink vlink, Handler<AsyncResult<Void>> resultHandler);
	
	@Fluent	
	TopologyService getVlink(String vlinkId, Handler<AsyncResult<Vlink>> resultHandler);
	
	@Fluent	
	TopologyService getAllVlinks(Handler<AsyncResult<List<Vlink>>> resultHandler);
	
	@Fluent	
	TopologyService getVlinksByVsubnet(String vsubnetId, Handler<AsyncResult<List<Vlink>>> resultHandler);
	
	@Fluent	
	TopologyService deleteVlink(String vlinkId, Handler<AsyncResult<Void>> resultHandler);
	
	@Fluent 
	TopologyService updateVlink(Vlink vlink, Handler<AsyncResult<Vlink>> resultHandler);
	

	/* VlinkConn */
	@Fluent	
	TopologyService addVlinkConn(VlinkConn vlinkConn, Handler<AsyncResult<Void>> resultHandler);
	
	@Fluent	
	TopologyService getVlinkConn(String vlinkConnId, Handler<AsyncResult<VlinkConn>> resultHandler);
	
	@Fluent	
	TopologyService getAllVlinkConns(Handler<AsyncResult<List<VlinkConn>>> resultHandler);
	
	@Fluent	
	TopologyService getVlinkConnsByVlink(String vlinkId, Handler<AsyncResult<List<VlinkConn>>> resultHandler);
	
	@Fluent	
	TopologyService deleteVlinkConn(String vlinkConnId, Handler<AsyncResult<Void>> resultHandler);
	
	@Fluent 
	TopologyService updateVlinkConn(VlinkConn vlinkConn, Handler<AsyncResult<VlinkConn>> resultHandler);
	
	
	/* Vtrail */
	@Fluent	
	TopologyService addVtrail(Vtrail vtrail, Handler<AsyncResult<Void>> resultHandler);
	
	@Fluent	
	TopologyService getVtrail(String vtrailId, Handler<AsyncResult<Vtrail>> resultHandler);
	
	@Fluent	
	TopologyService deleteVtrail(String vtrailId, Handler<AsyncResult<Void>> resultHandler);
	
	@Fluent
	TopologyService getAllVtrails(Handler<AsyncResult<List<Vtrail>>> resultHandler);
	
	@Fluent 
	TopologyService updateVtrail(Vtrail vtrail, Handler<AsyncResult<Vtrail>> resultHandler);
	
	
	/* Vxc */
	@Fluent	
	TopologyService addVxc(Vxc vxc, Handler<AsyncResult<Void>> resultHandler);
	
	@Fluent	
	TopologyService getVxc(String vxcId, Handler<AsyncResult<Vxc>> resultHandler);
	
	@Fluent	
	TopologyService getAllVxcs(Handler<AsyncResult<List<Vxc>>> resultHandler);
	
	@Fluent	
	TopologyService getVxcsByVtrail(String vtrailId, Handler<AsyncResult<List<Vxc>>> resultHandler);
	
	@Fluent	
	TopologyService deleteVxc(String vxcId, Handler<AsyncResult<Void>> resultHandler);
	
	@Fluent 
	TopologyService updateVxc(Vxc vxc, Handler<AsyncResult<Vxc>> resultHandler);
}