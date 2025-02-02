package io.nms.central.microservice.ipnet.api;

import io.nms.central.microservice.common.RestAPIVerticle;
import io.nms.central.microservice.common.functional.JsonUtils;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.Bgp;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.Device;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.Link;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.NetInterface;
import io.nms.central.microservice.ipnet.IpnetService;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;


/**
 * This verticle exposes a HTTP endpoint to ipnet with REST APIs.
 *
 * @author Amar Abane
 */
public class RestIpnetAPIVerticle extends RestAPIVerticle {

	private static final Logger logger = LoggerFactory.getLogger(RestIpnetAPIVerticle.class);

	public static final String SERVICE_NAME = "ipnet-rest-api";
	
	private static final String API_VERSION = "/v";
	
	private static final String API_RUNNING_VERIFY = "/running/verify";
	private static final String API_RUNNING_NETWORK = "/running/network";
	private static final String API_RUNNING_INTERFACES = "/running/device/:deviceName/interfaces";
	private static final String API_RUNNING_BGPS = "/running/device/:deviceName/bgps";
	
	private static final String API_CONFIG_NETWORK = "/config/network";
	private static final String API_CONFIG_ONE_DEVICE = "/config/device/:deviceName";
	private static final String API_CONFIG_INTERFACES = "/config/device/:deviceName/interfaces";
	private static final String API_CONFIG_ONE_INTERFACE = "/config/device/:deviceName/interface/:itfName";
	private static final String API_CONFIG_ONE_LINK = "/config/link/:linkName";
	private static final String API_CONFIG_BGPS = "/config/device/:deviceName/bgps";
	private static final String API_CONFIG_ONE_BGP = "/config/device/:deviceName/bgp/:itfAddr";

	private static final String API_CONFIG_DEVICE_FILE = "/config/device/:deviceName/file";

	private static final String API_CONFIG_CHANGES = "/config/changes";
	
	private static final String API_CONFIG_VERIFY = "/config/verify";
	private static final String API_CONFIG_APPLY = "/config/apply";

	private static final String API_INTENDED_NETCONFIG = "/netconfig/intended";
	private static final String API_RUNNING_NETCONFIG = "/netconfig/running";
	private static final String API_INTENDED_NETCONFIG_DEVICE = "/netconfig/device/:deviceName/intended";
	private static final String API_RUNNING_NETCONFIG_DEVICE = "/netconfig/device/:deviceName/running";

	private final IpnetService service;

	public RestIpnetAPIVerticle(IpnetService service) {
		this.service = service;
	}

	@Override
	public void start(Future<Void> future) throws Exception {
		super.start();
		final Router router = Router.router(vertx);
		router.route().handler(BodyHandler.create());
		router.get(API_VERSION).handler(this::apiVersion);
		
		/* Get running network state */
		router.get(API_RUNNING_VERIFY).handler(this::checkAdminRole).handler(this::apiRunningVerify);
		router.get(API_RUNNING_NETWORK).handler(this::checkAdminRole).handler(this::apiRunningGetNetwork);
		router.get(API_RUNNING_INTERFACES).handler(this::checkAdminRole).handler(this::apiRunningGetDeviceInterfaces);
		router.get(API_RUNNING_BGPS).handler(this::checkAdminRole).handler(this::apiRunningGetDeviceBgps);
		
		/* Operations on configuration in-progress */
		router.get(API_CONFIG_NETWORK).handler(this::checkAdminRole).handler(this::apiConfigGetNetwork);
		router.post(API_CONFIG_ONE_DEVICE).handler(this::checkAdminRole).handler(this::apiConfigCreateDevice);
		router.put(API_CONFIG_ONE_DEVICE).handler(this::checkAdminRole).handler(this::apiConfigUpdateDevice);
		router.delete(API_CONFIG_ONE_DEVICE).handler(this::checkAdminRole).handler(this::apiConfigDeleteDevice);
		router.get(API_CONFIG_INTERFACES).handler(this::checkAdminRole).handler(this::apiConfigGetDeviceInterfaces);
		router.post(API_CONFIG_ONE_INTERFACE).handler(this::checkAdminRole).handler(this::apiConfigCreateInterface);
		router.put(API_CONFIG_ONE_INTERFACE).handler(this::checkAdminRole).handler(this::apiConfigUpdateInterface);
		router.delete(API_CONFIG_ONE_INTERFACE).handler(this::checkAdminRole).handler(this::apiConfigDeleteInterface);
		router.post(API_CONFIG_ONE_LINK).handler(this::checkAdminRole).handler(this::apiConfigCreateLink);
		router.delete(API_CONFIG_ONE_LINK).handler(this::checkAdminRole).handler(this::apiConfigDeleteLink);
		router.get(API_CONFIG_BGPS).handler(this::checkAdminRole).handler(this::apiConfigGetDeviceBgps);
		router.post(API_CONFIG_ONE_BGP).handler(this::checkAdminRole).handler(this::apiConfigCreateBgp);
		router.put(API_CONFIG_ONE_BGP).handler(this::checkAdminRole).handler(this::apiConfigUpdateBgp);
		router.delete(API_CONFIG_ONE_BGP).handler(this::checkAdminRole).handler(this::apiConfigDeleteBgp);
		router.get(API_CONFIG_DEVICE_FILE).handler(this::checkAdminRole).handler(this::apiConfigGetDeviceFile);
		
		router.get(API_CONFIG_CHANGES).handler(this::checkAdminRole).handler(this::apiConfigGetAllChanges);
		// router.delete(API_CONFIG_CHANGES).handler(this::checkAdminRole).handler(this::apiConfigUndoChange);

		router.get(API_CONFIG_VERIFY).handler(this::checkAdminRole).handler(this::apiConfigVerify);
		router.get(API_CONFIG_APPLY).handler(this::checkAdminRole).handler(this::apiConfigApply);
		
		router.get(API_INTENDED_NETCONFIG).handler(this::checkAdminRole).handler(this::apiGetIntendedNetConfig);
		router.get(API_RUNNING_NETCONFIG).handler(this::checkAdminRole).handler(this::apiGetRunningNetConfig);
		router.post(API_RUNNING_NETCONFIG).handler(this::checkAdminRole).handler(this::apiSaveRunningNetConfig);
		
		router.get(API_INTENDED_NETCONFIG_DEVICE).handler(this::checkAdminRole).handler(this::apiGetIntendedDeviceConfig);
		router.get(API_RUNNING_NETCONFIG_DEVICE).handler(this::checkAdminRole).handler(this::apiGetRunningDeviceConfig);
		
		// get HTTP host and port from configuration, or use default value
		String host = config().getString("ipnet.http.address", "0.0.0.0");
		int port = config().getInteger("ipnet.http.port", 8087);

		// create HTTP server and publish REST service
		createHttpServer(router, host, port)
				.compose(serverCreated -> publishHttpEndpoint(SERVICE_NAME, host, port))
				.onComplete(future);
	}
	
	/* Get running network state */
	private void apiRunningVerify(RoutingContext context) {
		service.runningVerify(resultHandlerNonEmpty(context));
	}
	private void apiRunningGetNetwork(RoutingContext context) {
		service.runningGetNetwork(resultHandlerNonEmpty(context));
	}
	private void apiRunningGetDeviceInterfaces(RoutingContext context) {
		String deviceName = context.request().getParam("deviceName");
		service.runningGetDeviceInterfaces(deviceName, resultHandler(context, Json::encodePrettily));
	}
	private void apiRunningGetDeviceBgps(RoutingContext context) {
		String deviceName = context.request().getParam("deviceName");
		service.runningGetDeviceBgps(deviceName, resultHandler(context, Json::encodePrettily));
	}
	
	/* Operations on configuration in-progress */
	private void apiConfigGetNetwork(RoutingContext context) {
		JsonObject principal = new JsonObject(context.request().getHeader("user-principal"));
		String username = principal.getString("username");
		service.configGetNetwork(username, resultHandlerNonEmpty(context));
	}
	private void apiConfigCreateDevice(RoutingContext context) {
		JsonObject principal = new JsonObject(context.request().getHeader("user-principal"));
		String username = principal.getString("username");
		String deviceName = context.request().getParam("deviceName");
		try {
			final Device device 
					= JsonUtils.json2PojoE(context.getBodyAsString(), Device.class);
			service.configCreateDevice(username, deviceName, device, createResultHandler(context));
		} catch (Exception e) {
			logger.info("API input argument exception: " + e.getMessage());
			badRequest(context, e);
		}
	}
	private void apiConfigUpdateDevice(RoutingContext context) {
		JsonObject principal = new JsonObject(context.request().getHeader("user-principal"));
		String username = principal.getString("username");
		String deviceName = context.request().getParam("deviceName");
		try {
			final Device device 
					= JsonUtils.json2PojoE(context.getBodyAsString(), Device.class);
			service.configUpdateDevice(username, deviceName, device, resultVoidHandler(context, 200));
		} catch (Exception e) {
			logger.info("API input argument exception: " + e.getMessage());
			badRequest(context, e);
		}
	}
	private void apiConfigDeleteDevice(RoutingContext context) {
		JsonObject principal = new JsonObject(context.request().getHeader("user-principal"));
		String username = principal.getString("username");
		String deviceName = context.request().getParam("deviceName");
		service.configDeleteDevice(username, deviceName, deleteResultHandler(context));
	}
	
	private void apiConfigGetDeviceInterfaces(RoutingContext context) {
		JsonObject principal = new JsonObject(context.request().getHeader("user-principal"));
		String username = principal.getString("username");
		String deviceName = context.request().getParam("deviceName");
		service.configGetDeviceInterfaces(username, deviceName, resultHandler(context, Json::encodePrettily));
	}
	private void apiConfigCreateInterface(RoutingContext context) {
		JsonObject principal = new JsonObject(context.request().getHeader("user-principal"));
		String username = principal.getString("username");
		String deviceName = context.request().getParam("deviceName");
		String itfName = context.request().getParam("itfName");
		try {
			final NetInterface netItf 
					= JsonUtils.json2PojoE(context.getBodyAsString(), NetInterface.class);
			service.configCreateInterface(username, deviceName, itfName, netItf, createResultHandler(context));
		} catch (Exception e) {
			logger.info("API input argument exception: " + e.getMessage());
			badRequest(context, e);
		}
	}
	private void apiConfigUpdateInterface(RoutingContext context) {
		JsonObject principal = new JsonObject(context.request().getHeader("user-principal"));
		String username = principal.getString("username");
		String deviceName = context.request().getParam("deviceName");
		String itfName = context.request().getParam("itfName");
		try {
			final NetInterface netItf 
					= JsonUtils.json2PojoE(context.getBodyAsString(), NetInterface.class);
			service.configUpdateInterface(username, deviceName, itfName, netItf, resultVoidHandler(context, 200));
		} catch (Exception e) {
			logger.info("API input argument exception: " + e.getMessage());
			badRequest(context, e);
		}
	}
	private void apiConfigDeleteInterface(RoutingContext context) {
		JsonObject principal = new JsonObject(context.request().getHeader("user-principal"));
		String username = principal.getString("username");
		String deviceName = context.request().getParam("deviceName");
		String itfName = context.request().getParam("itfName");
		service.configDeleteInterface(username, deviceName, itfName, deleteResultHandler(context));
	}
	
	private void apiConfigCreateLink(RoutingContext context) {
		JsonObject principal = new JsonObject(context.request().getHeader("user-principal"));
		String username = principal.getString("username");
		String linkName = context.request().getParam("linkName");
		try {
			final Link link 
					= JsonUtils.json2PojoE(context.getBodyAsString(), Link.class);
			service.configCreateLink(username, linkName, link, createResultHandler(context));
		} catch (Exception e) {
			logger.info("API input argument exception: " + e.getMessage());
			badRequest(context, e);
		}
	}
	private void apiConfigDeleteLink(RoutingContext context) {
		JsonObject principal = new JsonObject(context.request().getHeader("user-principal"));
		String username = principal.getString("username");
		String linkName = context.request().getParam("linkName");
		service.configDeleteLink(username, linkName, deleteResultHandler(context));
	}

	private void apiConfigGetDeviceBgps(RoutingContext context) {
		JsonObject principal = new JsonObject(context.request().getHeader("user-principal"));
		String username = principal.getString("username");
		String deviceName = context.request().getParam("deviceName");
		service.configGetDeviceBgps(username, deviceName, resultHandler(context, Json::encodePrettily));
	}
	private void apiConfigCreateBgp(RoutingContext context) {
		JsonObject principal = new JsonObject(context.request().getHeader("user-principal"));
		String username = principal.getString("username");
		String deviceName = context.request().getParam("deviceName");
		String itfAddr = context.request().getParam("itfAddr");
		try {
			final Bgp bgp 
					= JsonUtils.json2PojoE(context.getBodyAsString(), Bgp.class);
			service.configCreateBgp(username, deviceName, itfAddr, bgp, createResultHandler(context));
		} catch (Exception e) {
			logger.info("API input argument exception: " + e.getMessage());
			badRequest(context, e);
		}
	}
	private void apiConfigUpdateBgp(RoutingContext context) {
		JsonObject principal = new JsonObject(context.request().getHeader("user-principal"));
		String username = principal.getString("username");
		String deviceName = context.request().getParam("deviceName");
		String itfAddr = context.request().getParam("itfAddr");
		try {
			final Bgp bgp 
					= JsonUtils.json2PojoE(context.getBodyAsString(), Bgp.class);
			service.configUpdateBgp(username, deviceName, itfAddr, bgp, resultVoidHandler(context, 200));
		} catch (Exception e) {
			logger.info("API input argument exception: " + e.getMessage());
			badRequest(context, e);
		}
	}
	private void apiConfigDeleteBgp(RoutingContext context) {
		JsonObject principal = new JsonObject(context.request().getHeader("user-principal"));
		String username = principal.getString("username");
		String deviceName = context.request().getParam("deviceName");
		String itfAddr = context.request().getParam("itfAddr");
		service.configDeleteBgp(username, deviceName, itfAddr, deleteResultHandler(context));
	}

	private void apiConfigGetDeviceFile(RoutingContext context) {
		JsonObject principal = new JsonObject(context.request().getHeader("user-principal"));
		String username = principal.getString("username");
		String deviceName = context.request().getParam("deviceName");
		service.configGetDeviceFile(username, deviceName, resultHandler(context, Json::encodePrettily));
	}

	private void apiConfigVerify(RoutingContext context) {
		JsonObject principal = new JsonObject(context.request().getHeader("user-principal"));
		String username = principal.getString("username");
		service.configVerify(username, resultHandlerNonEmpty(context));
	}
	private void apiConfigApply(RoutingContext context) {
		JsonObject principal = new JsonObject(context.request().getHeader("user-principal"));
		String username = principal.getString("username");
		service.configApply(username, resultHandlerNonEmpty(context));
	}
	
	private void apiConfigGetAllChanges(RoutingContext context) {
		JsonObject principal = new JsonObject(context.request().getHeader("user-principal"));
		String username = principal.getString("username");
		service.getAllConfigChanges(username, resultHandler(context, Json::encodePrettily));
	}
	/* private void apiConfigUndoChange(RoutingContext context) {
		JsonObject principal = new JsonObject(context.request().getHeader("user-principal"));
		String username = principal.getString("username");
		service.undoConfigChange(username, deleteResultHandler(context));
	} */
	
	private void apiGetIntendedNetConfig(RoutingContext context) {
		JsonObject principal = new JsonObject(context.request().getHeader("user-principal"));
		String username = principal.getString("username");
		service.getIntendedNetworkConfig(username, resultHandler(context, Json::encodePrettily));
	}
	private void apiGetRunningNetConfig(RoutingContext context) {
		JsonObject principal = new JsonObject(context.request().getHeader("user-principal"));
		String username = principal.getString("username");
		service.getRunningNetworkConfig(username, resultHandler(context, Json::encodePrettily));
	}
	private void apiSaveRunningNetConfig(RoutingContext context) {
		JsonObject principal = new JsonObject(context.request().getHeader("user-principal"));
		String username = principal.getString("username");
		service.updateRunningNetworkConfig(username, context.getBodyAsJson(), resultVoidHandler(context, 200));
	}
	
	private void apiGetIntendedDeviceConfig(RoutingContext context) {
		JsonObject principal = new JsonObject(context.request().getHeader("user-principal"));
		String username = principal.getString("username");
		String deviceName = context.request().getParam("deviceName");
		service.getIntendedDeviceConfig(username, deviceName, resultHandler(context, Json::encodePrettily));
	}
	private void apiGetRunningDeviceConfig(RoutingContext context) {
		JsonObject principal = new JsonObject(context.request().getHeader("user-principal"));
		String username = principal.getString("username");
		String deviceName = context.request().getParam("deviceName");
		service.getRunningDeviceConfig(username, deviceName, resultHandler(context, Json::encodePrettily));
	}
	
	/* get service version */
	private void apiVersion(RoutingContext context) { 
		context.response()
		.end(new JsonObject()
				.put("name", SERVICE_NAME)
				.put("version", "v1").encodePrettily());
	}
}
