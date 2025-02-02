package io.nms.central.microservice.ipnet;

import java.util.List;

import io.nms.central.microservice.digitaltwin.model.dt.VerificationReport;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.Bgp;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.Device;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.Link;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.NetInterface;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.Network;
import io.nms.central.microservice.ipnet.model.ApplyConfigResult;
import io.nms.central.microservice.ipnet.model.ConfigChange;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

/**
 * A service interface managing ipnet.
 * <p>
 * This service is an event bus service (aka. service proxy)
 * </p>
 */
@VertxGen
@ProxyGen
public interface IpnetService {

	/**
	 * The name of the event bus service.
	 */
	String SERVICE_NAME = "ipnet-eb-service";

	/**
	 * The address on which the service is published.
	 */
	String SERVICE_ADDRESS = "service.ipnet";
	

	String FROTNEND_ADDRESS = "mvs.to.frontend";
	

	void initializePersistence(Handler<AsyncResult<List<Integer>>> resultHandler);
	
	/* API */
	void runningVerify(Handler<AsyncResult<VerificationReport>> resultHandler);
	void runningGetNetwork(Handler<AsyncResult<Network>> resultHandler);
	void runningGetDeviceInterfaces(String deviceName, Handler<AsyncResult<List<NetInterface>>> resultHandler);
	void runningGetDeviceBgps(String deviceName, Handler<AsyncResult<List<Bgp>>> resultHandler);

	void configGetNetwork(String viewId, Handler<AsyncResult<Network>> resultHandler);
	void configCreateDevice(String viewId, String deviceName, Device device, Handler<AsyncResult<Void>> resultHandler);
	void configUpdateDevice(String viewId, String deviceName, Device device, Handler<AsyncResult<Void>> resultHandler);
	void configDeleteDevice(String viewId, String deviceName, Handler<AsyncResult<Void>> resultHandler);
	void configGetDeviceInterfaces(String viewId, String deviceName, Handler<AsyncResult<List<NetInterface>>> resultHandler);
	void configCreateInterface(String viewId, String deviceName, String itfName, NetInterface netItf, Handler<AsyncResult<Void>> resultHandler);
	void configUpdateInterface(String viewId, String deviceName, String itfName, NetInterface netItf, Handler<AsyncResult<Void>> resultHandler);
	void configDeleteInterface(String viewId, String deviceName, String itfName, Handler<AsyncResult<Void>> resultHandler);
	void configGetDeviceBgps(String viewId, String deviceName, Handler<AsyncResult<List<Bgp>>> resultHandler);
	void configCreateLink(String viewId, String linkName, Link link, Handler<AsyncResult<Void>> resultHandler);
	void configDeleteLink(String viewId, String linkName, Handler<AsyncResult<Void>> resultHandler);
	void configCreateBgp(String viewId, String deviceName, String itfAddr, Bgp bgp, Handler<AsyncResult<Void>> resultHandler);
	void configUpdateBgp(String viewId, String deviceName, String itfAddr, Bgp bgp, Handler<AsyncResult<Void>> resultHandler);
	void configDeleteBgp(String viewId, String deviceName, String itfAddr, Handler<AsyncResult<Void>> resultHandler);
	void configGetDeviceFile(String viewId, String deviceName, Handler<AsyncResult<JsonObject>> resultHandler);

	void getAllConfigChanges(String viewId, Handler<AsyncResult<List<ConfigChange>>> resultHandler);
	// void undoConfigChange(String viewId, Handler<AsyncResult<Void>> resultHandler);
	
	void configVerify(String viewId, Handler<AsyncResult<VerificationReport>> resultHandler);
	void configApply(String viewId, Handler<AsyncResult<ApplyConfigResult>> resultHandler);
	
	void getIntendedNetworkConfig(String viewId, Handler<AsyncResult<JsonObject>> resultHandler);
	void getRunningNetworkConfig(String viewId, Handler<AsyncResult<JsonObject>> resultHandler);
	void updateRunningNetworkConfig(String viewId, JsonObject netConfig, Handler<AsyncResult<Void>> resultHandler);
	
	void getIntendedDeviceConfig(String viewId, String deviceName, Handler<AsyncResult<JsonObject>> resultHandler);
	void getRunningDeviceConfig(String viewId, String deviceName, Handler<AsyncResult<JsonObject>> resultHandler);
}
