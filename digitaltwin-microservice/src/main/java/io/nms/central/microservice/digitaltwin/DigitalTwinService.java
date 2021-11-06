package io.nms.central.microservice.digitaltwin;

import java.util.List;
import io.nms.central.microservice.digitaltwin.model.graph.NetConfigCollection;
import io.nms.central.microservice.digitaltwin.model.dt.DtQuery;
import io.nms.central.microservice.digitaltwin.model.dt.DtQueryResult;
import io.nms.central.microservice.digitaltwin.model.dt.Report;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.Bgp;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.Configuration;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.Device;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.NetInterface;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.Network;
import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

/**
 * A service interface managing Digital Twin.
 */
@VertxGen
@ProxyGen
public interface DigitalTwinService {

	/**
	 * The name of the event bus service.
	 */
	String SERVICE_NAME = "digitaltwin-eb-service";

	/**
	 * The address on which the service is published.
	 */
	String SERVICE_ADDRESS = "service.digitaltwin";
	
	String FROTNEND_ADDRESS = "mvs.to.frontend";

	String EVENT_ADDRESS = "digitaltwin.event";
	
	@Fluent	
	DigitalTwinService initializePersistence(Handler<AsyncResult<Void>> resultHandler);
	
	/* Operations on running network */
	@Fluent	
	DigitalTwinService runningProcessNetworkConfig(NetConfigCollection config, Handler<AsyncResult<Report>> resultHandler);
	@Fluent	
	DigitalTwinService runningVerifyNetworkConfig(Handler<AsyncResult<Report>> resultHandler);
	@Fluent	
	DigitalTwinService runningQueryNetwork(DtQuery query, Handler<AsyncResult<DtQueryResult>> resultHandler);
	@Fluent	
	DigitalTwinService runningGetNetwork(Handler<AsyncResult<Network>> resultHandler);
	
	@Fluent
	DigitalTwinService runningGetDeviceInterfaces(String deviceName, Handler<AsyncResult<List<NetInterface>>> resultHandler);
	@Fluent	
	DigitalTwinService runningGetDeviceBgps(String deviceName, Handler<AsyncResult<List<Bgp>>> resultHandler);
	
	/* Operations on view network */
	// view
	@Fluent	
	DigitalTwinService createView(String viewId, Handler<AsyncResult<Void>> resultHandler);
	@Fluent	
	DigitalTwinService deleteView(String viewId, Handler<AsyncResult<Void>> resultHandler);
	// view config
	@Fluent	
	DigitalTwinService viewVerifyConfig(String viewId, Configuration configuration, Handler<AsyncResult<Void>> resultHandler);
	@Fluent	 // TODO: replace JsonOBject with NetworkConfig object
	DigitalTwinService viewGenerateNetworkConfig(String viewId, Configuration configuration, Handler<AsyncResult<JsonObject>> resultHandler);
	
	// view network
	@Fluent	
	DigitalTwinService viewGetNetwork(String viewId, Handler<AsyncResult<Network>> resultHandler);
	
	// view device
	@Fluent	
	DigitalTwinService viewGetDevice(String viewId, String deviceName, Handler<AsyncResult<Device>> resultHandler);
	@Fluent	
	DigitalTwinService viewUpdateDevice(String viewId, String deviceName, Device device, Handler<AsyncResult<Void>> resultHandler);
	
	// view interface
	@Fluent	
	DigitalTwinService viewGetDeviceInterfaces(String viewId, String deviceName, Handler<AsyncResult<List<NetInterface>>> resultHandler);
	@Fluent	
	DigitalTwinService viewGetInterface(String viewId, String deviceName, String itfName, Handler<AsyncResult<NetInterface>> resultHandler);
	@Fluent	
	DigitalTwinService viewUpdateInterface(String viewId, String deviceName, String itfName, NetInterface netInterface, Handler<AsyncResult<Void>> resultHandler);
	
	// view bgp
	@Fluent	
	DigitalTwinService viewGetDeviceBgps(String viewId, String deviceName, Handler<AsyncResult<List<Bgp>>> resultHandler);
	@Fluent	
	DigitalTwinService viewGetBgp(String viewId, String deviceName, String itfAddr, Handler<AsyncResult<Bgp>> resultHandler);
	@Fluent	
	DigitalTwinService viewCreateBgp(String viewId, String deviceName, String itfAddr, Bgp bgp, Handler<AsyncResult<Void>> resultHandler);
	@Fluent	
	DigitalTwinService viewUpdateBgp(String viewId, String deviceName, String itfAddr, Bgp bgp, Handler<AsyncResult<Void>> resultHandler);
	@Fluent	
	DigitalTwinService viewDeleteBgp(String viewId, String deviceName, String itfAddr, Handler<AsyncResult<Void>> resultHandler);
}