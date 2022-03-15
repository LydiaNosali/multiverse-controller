package io.nms.central.microservice.digitaltwin.api;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;

import io.nms.central.microservice.common.RestAPIVerticle;
import io.nms.central.microservice.common.functional.JsonUtils;
import io.nms.central.microservice.digitaltwin.DigitalTwinService;
import io.nms.central.microservice.digitaltwin.model.graph.DeviceState;
import io.nms.central.microservice.digitaltwin.model.graph.NetworkState;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.Bgp;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.Device;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.Link;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.NetInterface;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.Vlan;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.VlanMember;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.graphql.GraphQLHandler;

/**
 * This verticle exposes REST API endpoints to process Digital-Twin service operation
 */
public class RestDigitalTwinAPIVerticle extends RestAPIVerticle {

	private static final Logger logger = LoggerFactory.getLogger(RestDigitalTwinAPIVerticle.class);

	public static final String SERVICE_NAME = "digitaltwin-rest-api";

	private static final String API_VERSION = "/v";

	private static final String API_RUNNING_STATE = "/running/state";
	private static final String API_RUNNING_VERIFY = "/running/verify";
	private static final String API_RUNNING_CONFIG = "/running/config";
	private static final String API_RUNNING_NETWORK = "/running/network";
	private static final String API_RUNNING_ONE_DEVICE = "/running/device/:deviceName";
	private static final String API_RUNNING_DEVICE_CONFIG = "/running/device/:deviceName/config";
	private static final String API_RUNNING_INTERFACES = "/running/device/:deviceName/interfaces";
	private static final String API_RUNNING_ONE_INTERFACE = "/running/device/:deviceName/interface/:itfName";
	private static final String API_RUNNING_BGPS = "/running/device/:deviceName/bgps";
	private static final String API_RUNNING_ONE_BGP = "/running/device/:deviceName/bgp/:itfAddr";
	private static final String API_RUNNING_VLANS = "/running/device/:deviceName/vlans";
	private static final String API_RUNNING_VLAN_MEMBERS = "/running/device/:deviceName/vlan/:vid/members";

	private static final String API_VIEW = "/view";
	private static final String API_ONE_VIEW = "/view/:viewId";
	private static final String API_VIEW_VERIFY = "/view/:viewId/verify";
	private static final String API_VIEW_CONFIG = "/view/:viewId/config";
	private static final String API_VIEW_NETWORK = "/view/:viewId/network";
	private static final String API_VIEW_ONE_DEVICE = "/view/:viewId/device/:deviceName";
	private static final String API_VIEW_DEVICE_CONFIG = "/view/:viewId/device/:deviceName/config";
	private static final String API_VIEW_INTERFACES = "/view/:viewId/device/:deviceName/interfaces";
	private static final String API_VIEW_ONE_INTERFACE = "/view/:viewId/device/:deviceName/interface/:itfName";
	private static final String API_VIEW_LINK = "/view/:viewId/link";
	private static final String API_VIEW_ONE_LINK = "/view/:viewId/link/:linkName";
	private static final String API_VIEW_BGPS = "/view/:viewId/device/:deviceName/bgps";
	private static final String API_VIEW_ONE_BGP = "/view/:viewId/device/:deviceName/bgp/:itfAddr";

	private static final String API_VIEW_VLANS = "/view/:viewId/device/:deviceName/vlans";
	private static final String API_VIEW_ONE_VLAN = "/view/:viewId/device/:deviceName/vlan/:vid";
	private static final String API_VIEW_VLAN_MEMBERS = "/view/:viewId/device/:deviceName/vlan/:vid/members";
	private static final String API_VIEW_ONE_VLAN_MEMBER = "/view/:viewId/device/:deviceName/vlan/:vid/member/:itfName";

	private static final String API_GRAPHQL = "/nqe";

	private DigitalTwinService service;
	private NetworkQueryEngine nqe;

	public RestDigitalTwinAPIVerticle(DigitalTwinService service) {
		this.service = service;
	}

	@Override
	public void start(Future<Void> future) throws Exception {
		super.start();
		final Router router = Router.router(vertx);

		// body handler
		router.route().handler(BodyHandler.create());

		// version
		router.get(API_VERSION).handler(this::apiVersion);

		/* Operations on running network */
		router.post(API_RUNNING_STATE).handler(this::checkAdminRole).handler(this::apiProcessNetworkRunningState);

		router.get(API_RUNNING_VERIFY).handler(this::checkAdminRole).handler(this::apiRunningVerifyNetwork);
		router.get(API_RUNNING_CONFIG).handler(this::checkAdminRole).handler(this::apiRunningGenerateNetworkConfig);

		router.get(API_RUNNING_NETWORK).handler(this::checkAdminRole).handler(this::apiRunningGetNetwork);
		router.get(API_RUNNING_ONE_DEVICE).handler(this::checkAdminRole).handler(this::apiRunningGetDevice);
		router.get(API_RUNNING_INTERFACES).handler(this::checkAdminRole).handler(this::apiRunningGetDeviceInterfaces);
		router.get(API_RUNNING_ONE_INTERFACE).handler(this::checkAdminRole).handler(this::apiRunningGetInterface);
		router.get(API_RUNNING_BGPS).handler(this::checkAdminRole).handler(this::apiRunningGetDeviceBgps);
		router.get(API_RUNNING_ONE_BGP).handler(this::checkAdminRole).handler(this::apiRunningGetBgp);
		router.get(API_RUNNING_VLANS).handler(this::checkAdminRole).handler(this::apiRunningGetVlans);
		router.get(API_RUNNING_VLAN_MEMBERS).handler(this::checkAdminRole).handler(this::apiRunningGetVlanMembers);

		router.get(API_RUNNING_DEVICE_CONFIG).handler(this::checkAdminRole).handler(this::apiRunningGetDeviceConfig);

		/* Operations on view network */
		router.post(API_VIEW).handler(this::checkAdminRole).handler(this::apiCreateView);
		router.delete(API_ONE_VIEW).handler(this::checkAdminRole).handler(this::apiDeleteView);
		router.get(API_VIEW_VERIFY).handler(this::checkAdminRole).handler(this::apiViewVerify);
		router.get(API_VIEW_CONFIG).handler(this::checkAdminRole).handler(this::apiViewGenerateNetworkConfig);
		router.get(API_VIEW_NETWORK).handler(this::checkAdminRole).handler(this::apiViewGetNetwork);
		router.get(API_VIEW_ONE_DEVICE).handler(this::checkAdminRole).handler(this::apiViewGetDevice);
		router.put(API_VIEW_ONE_DEVICE).handler(this::checkAdminRole).handler(this::apiViewCreateDevice);
		router.delete(API_VIEW_ONE_DEVICE).handler(this::checkAdminRole).handler(this::apiViewDeleteDevice);
		router.get(API_VIEW_INTERFACES).handler(this::checkAdminRole).handler(this::apiViewGetDeviceInterfaces);
		router.get(API_VIEW_ONE_INTERFACE).handler(this::checkAdminRole).handler(this::apiViewGetInterface);
		router.put(API_VIEW_ONE_INTERFACE).handler(this::checkAdminRole).handler(this::apiViewCreateInterface);
		router.delete(API_VIEW_ONE_INTERFACE).handler(this::checkAdminRole).handler(this::apiViewDeleteInterface);

		router.post(API_VIEW_LINK).handler(this::checkAdminRole).handler(this::apiViewCreateLink);
		router.delete(API_VIEW_ONE_LINK).handler(this::checkAdminRole).handler(this::apiViewDeleteLink);

		router.get(API_VIEW_BGPS).handler(this::checkAdminRole).handler(this::apiViewGetDeviceBgps);
		router.get(API_VIEW_ONE_BGP).handler(this::checkAdminRole).handler(this::apiViewGetBgp);
		router.put(API_VIEW_ONE_BGP).handler(this::checkAdminRole).handler(this::apiViewUpdateBgp);
		router.delete(API_VIEW_ONE_BGP).handler(this::checkAdminRole).handler(this::apiViewDeleteBgp);

		router.get(API_VIEW_VLANS).handler(this::checkAdminRole).handler(this::apiViewGetVlans);
		router.put(API_VIEW_ONE_VLAN).handler(this::checkAdminRole).handler(this::apiViewCreateVlan);
		router.delete(API_VIEW_ONE_VLAN).handler(this::checkAdminRole).handler(this::apiViewDeleteVlan);
		router.get(API_VIEW_VLAN_MEMBERS).handler(this::checkAdminRole).handler(this::apiViewGetVlanMembers);
		router.put(API_VIEW_ONE_VLAN_MEMBER).handler(this::checkAdminRole).handler(this::apiViewAddVlanMember);
		router.delete(API_VIEW_ONE_VLAN_MEMBER).handler(this::checkAdminRole).handler(this::apiViewRemoveVlanMember);

		router.get(API_VIEW_DEVICE_CONFIG).handler(this::checkAdminRole).handler(this::apiViewGetDeviceConfig);

		// get HTTP host and port from configuration, or use default value
		/* GraphQL endpoint for NQE */
		nqe = new NetworkQueryEngine(service);
		String schema = vertx.fileSystem().readFileBlocking("schema.graphqls").toString();
		GraphQLHandler nqeHandler = nqe.setupQueryEngine(schema);
		router.post(API_GRAPHQL).handler(this::checkAdminRole).handler(nqeHandler);

		// get HTTP host and port from configuration, or use default value
		String host = config().getString("digitaltwin.http.address", "0.0.0.0");
		int port = config().getInteger("digitaltwin.http.port", 8084);

		// create HTTP server and publish REST service
		createHttpServer(router, host, port)
		.compose(serverCreated -> publishHttpEndpoint(SERVICE_NAME, host, port))
		.onComplete(future);
	}

	/* Operations on running network */
	private void apiProcessNetworkRunningState(RoutingContext context) {
		try {
			TypeReference<HashMap<String,DeviceState>> typeRef 
			= new TypeReference<HashMap<String,DeviceState>>() {};
			final Map<String, DeviceState> deviceStates 
			= JsonUtils.json2Pojo(context.getBodyAsString(), typeRef);
			final NetworkState netState = new NetworkState();
			netState.setConfigs(deviceStates);
			service.processNetworkRunningState(netState, resultHandlerNonEmpty(context));
		} catch (Exception e) {
			logger.info("API input argument exception: " + e.getMessage());
			badRequest(context, e);
		}
	}
	private void apiRunningVerifyNetwork(RoutingContext context) {
		service.runningVerifyNetwork(resultHandlerNonEmpty(context));
	}
	private void apiRunningGenerateNetworkConfig(RoutingContext context) {
		service.runningGetNetworkConfig(resultHandler(context, Json::encodePrettily));
	}
	private void apiRunningGetNetwork(RoutingContext context) {
		service.runningGetNetwork(resultHandlerNonEmpty(context));
	}
	private void apiRunningGetDevice(RoutingContext context) {
		String deviceName = context.request().getParam("deviceName");
		service.runningGetDevice(deviceName, resultHandlerNonEmpty(context));
	}
	private void apiRunningGetDeviceInterfaces(RoutingContext context) {
		String deviceName = context.request().getParam("deviceName");
		service.runningGetDeviceInterfaces(deviceName, resultHandler(context, Json::encodePrettily));
	}
	private void apiRunningGetInterface(RoutingContext context) {
		String deviceName = context.request().getParam("deviceName");
		String itfName = context.request().getParam("itfName");
		service.runningGetInterface(deviceName, itfName, resultHandlerNonEmpty(context));
	}
	private void apiRunningGetDeviceBgps(RoutingContext context) {
		String deviceName = context.request().getParam("deviceName");
		service.runningGetDeviceBgps(deviceName, resultHandler(context, Json::encodePrettily));
	}
	private void apiRunningGetBgp(RoutingContext context) {
		String deviceName = context.request().getParam("deviceName");
		String itfAddr = context.request().getParam("itfAddr");
		service.runningGetBgp(deviceName, itfAddr, resultHandlerNonEmpty(context));
	}
	private void apiRunningGetVlans(RoutingContext context) {
		String deviceName = context.request().getParam("deviceName");
		service.runningGetDeviceVlans(deviceName, resultHandler(context, Json::encodePrettily));
	}
	private void apiRunningGetVlanMembers(RoutingContext context) {
		String deviceName = context.request().getParam("deviceName");
		String vid = context.request().getParam("vid");
		service.runningGetVlanMembers(deviceName, vid, resultHandler(context, Json::encodePrettily));
	}
	private void apiRunningGetDeviceConfig(RoutingContext context) {
		String deviceName = context.request().getParam("deviceName");
		service.runningGetDeviceConfig(deviceName, resultHandler(context, Json::encodePrettily));
	}
	// view
	private void apiCreateView(RoutingContext context) {
		JsonObject principal = new JsonObject(context.request().getHeader("user-principal"));
		String viewId = principal.getString("username");
		// String viewId = context.request().getParam("viewId");
		service.createView(viewId, createResultHandler(context));
	}
	private void apiDeleteView(RoutingContext context) {
		String viewId = context.request().getParam("viewId");
		service.deleteView(viewId, deleteResultHandler(context));
	}

	// view config
	private void apiViewVerify(RoutingContext context) {
		String viewId = context.request().getParam("viewId");
		service.viewVerify(viewId, (resultHandlerNonEmpty(context)));
	}
	private void apiViewGenerateNetworkConfig(RoutingContext context) {
		String viewId = context.request().getParam("viewId");
		service.viewGetNetworkConfig(viewId, resultHandler(context, Json::encodePrettily));
	}
	private void apiViewGetDeviceConfig(RoutingContext context) {
		String viewId = context.request().getParam("viewId");
		String deviceName = context.request().getParam("deviceName");
		service.viewGetDeviceConfig(viewId, deviceName, resultHandler(context, Json::encodePrettily));
	}

	// view network
	private void apiViewGetNetwork(RoutingContext context) {
		String viewId = context.request().getParam("viewId");
		service.viewGetNetwork(viewId, resultHandlerNonEmpty(context));
	}

	// view device
	private void apiViewGetDevice(RoutingContext context) {
		String viewId = context.request().getParam("viewId");
		String deviceName = context.request().getParam("deviceName");
		service.viewGetDevice(viewId, deviceName, resultHandlerNonEmpty(context));
	}
	private void apiViewCreateDevice(RoutingContext context) {
		String viewId = context.request().getParam("viewId");
		String deviceName = context.request().getParam("deviceName");
		try {
			final Device device 
			= JsonUtils.json2PojoE(context.getBodyAsString(), Device.class);
			service.viewCreateDevice(viewId, deviceName, device, createResultHandler(context));
		} catch (Exception e) {
			logger.info("API input argument exception: " + e.getMessage());
			badRequest(context, e);
		}
	}
	private void apiViewDeleteDevice(RoutingContext context) {
		String viewId = context.request().getParam("viewId");
		String deviceName = context.request().getParam("deviceName");
		service.viewDeleteDevice(viewId, deviceName, deleteResultHandler(context));
	}

	// view interface
	private void apiViewGetDeviceInterfaces(RoutingContext context) {
		String viewId = context.request().getParam("viewId");
		String deviceName = context.request().getParam("deviceName");
		service.viewGetDeviceInterfaces(viewId, deviceName, resultHandler(context, Json::encodePrettily));
	}
	private void apiViewGetInterface(RoutingContext context) {
		String viewId = context.request().getParam("viewId");
		String deviceName = context.request().getParam("deviceName");
		String itfName = context.request().getParam("itfName");
		service.viewGetInterface(viewId, deviceName, itfName, resultHandlerNonEmpty(context));
	}
	private void apiViewCreateInterface(RoutingContext context) {
		String viewId = context.request().getParam("viewId");
		String deviceName = context.request().getParam("deviceName");
		String itfName = context.request().getParam("itfName");
		try {
			final NetInterface netItf
			= JsonUtils.json2PojoE(context.getBodyAsString(), NetInterface.class);
			service.viewCreateInterface(viewId, deviceName, itfName, netItf, createResultHandler(context));
		} catch (Exception e) {
			logger.info("API input argument exception: " + e.getMessage());
			badRequest(context, e);
		}
	}
	private void apiViewDeleteInterface(RoutingContext context) {
		String viewId = context.request().getParam("viewId");
		String deviceName = context.request().getParam("deviceName");
		String itfName = context.request().getParam("itfName");
		service.viewDeleteInterface(viewId, deviceName, itfName, deleteResultHandler(context));
	}

	// view link
	private void apiViewCreateLink(RoutingContext context) {
		String viewId = context.request().getParam("viewId");
		String linkName = context.request().getParam("linkName");
		try {
			final Link link
					= JsonUtils.json2PojoE(context.getBodyAsString(), Link.class);
			service.viewCreateLink(viewId, linkName, link, createResultHandler(context));
		} catch (Exception e) {
			logger.info("API input argument exception: " + e.getMessage());
			badRequest(context, e);
		}
	}
	private void apiViewDeleteLink(RoutingContext context) {
		String viewId = context.request().getParam("viewId");
		String linkName = context.request().getParam("linkName");
		service.viewDeleteLink(viewId, linkName, deleteResultHandler(context));
	}

	// view bgp
	private void apiViewGetDeviceBgps(RoutingContext context) {
		String viewId = context.request().getParam("viewId");
		String deviceName = context.request().getParam("deviceName");
		service.viewGetDeviceBgps(viewId, deviceName, resultHandler(context, Json::encodePrettily));
	}
	private void apiViewGetBgp(RoutingContext context) {
		String viewId = context.request().getParam("viewId");
		String deviceName = context.request().getParam("deviceName");
		String itfAddr = context.request().getParam("itfAddr");
		service.viewGetBgp(viewId, deviceName, itfAddr, resultHandlerNonEmpty(context));
	}
	private void apiViewUpdateBgp(RoutingContext context) {
		String viewId = context.request().getParam("viewId");
		String deviceName = context.request().getParam("deviceName");
		String itfAddr = context.request().getParam("itfAddr");
		try {
			final Bgp bgp 
			= JsonUtils.json2PojoE(context.getBodyAsString(), Bgp.class);
			service.viewUpdateBgp(viewId, deviceName, itfAddr, bgp, updateResultHandler(context));
		} catch (Exception e) {
			logger.info("API input argument exception: " + e.getMessage());
			badRequest(context, e);
		}
	}
	private void apiViewDeleteBgp(RoutingContext context) {
		String viewId = context.request().getParam("viewId");
		String deviceName = context.request().getParam("deviceName");
		String itfAddr = context.request().getParam("itfAddr");
		service.viewDeleteBgp(viewId, deviceName, itfAddr, deleteResultHandler(context));
	}

	// view vlan
	private void apiViewGetVlans(RoutingContext context) {
		String viewId = context.request().getParam("viewId");
		String deviceName = context.request().getParam("deviceName");
		service.viewGetDeviceVlans(viewId, deviceName, resultHandler(context, Json::encodePrettily));
	}
	private void apiViewCreateVlan(RoutingContext context) {
		String viewId = context.request().getParam("viewId");
		String deviceName = context.request().getParam("deviceName");
		String vid = context.request().getParam("vid");
		try {
			final Vlan vlan
			= JsonUtils.json2PojoE(context.getBodyAsString(), Vlan.class);
			service.viewCreateVlan(viewId, deviceName, vid, vlan, createResultHandler(context));
		} catch (Exception e) {
			logger.info("API input argument exception: " + e.getMessage());
			badRequest(context, e);
		}
	}
	private void apiViewDeleteVlan(RoutingContext context) {
		String viewId = context.request().getParam("viewId");
		String deviceName = context.request().getParam("deviceName");
		String vid = context.request().getParam("vid");
		service.viewDeleteVlan(viewId, deviceName, vid, deleteResultHandler(context));
	}

	// view vlan-member
	private void apiViewGetVlanMembers(RoutingContext context) {
		String viewId = context.request().getParam("viewId");
		String deviceName = context.request().getParam("deviceName");
		String vid = context.request().getParam("vid");
		service.viewGetVlanMembers(viewId, deviceName, vid, resultHandler(context, Json::encodePrettily));
	}
	private void apiViewAddVlanMember(RoutingContext context) {
		String viewId = context.request().getParam("viewId");
		String deviceName = context.request().getParam("deviceName");
		String vid = context.request().getParam("vid");
		String itfName = context.request().getParam("itfName");
		try {
			final VlanMember vlanMember
			= JsonUtils.json2PojoE(context.getBodyAsString(), VlanMember.class);
			service.viewAddVlanMember(viewId, deviceName, vid, itfName, vlanMember, createResultHandler(context));
		} catch (Exception e) {
			logger.info("API input argument exception: " + e.getMessage());
			badRequest(context, e);
		}
	}
	private void apiViewRemoveVlanMember(RoutingContext context) {
		String viewId = context.request().getParam("viewId");
		String deviceName = context.request().getParam("deviceName");
		String vid = context.request().getParam("vid");
		String itfName = context.request().getParam("itfName");
		service.viewRemoveVlanMember(viewId, deviceName, vid, itfName, deleteResultHandler(context));
	}

	/* API version */
	private void apiVersion(RoutingContext context) {
		context.response().end(new JsonObject()
				.put("name", SERVICE_NAME)
				.put("version", "v1").encodePrettily());
	}

	/* eventbus messages */
	private void notifyDigitalTwinChange() {
		// TODO: add type of change
		vertx.eventBus().publish(DigitalTwinService.EVENT_ADDRESS, new JsonObject());
	}

	private void notifyFrontend() {
		vertx.eventBus().publish(DigitalTwinService.FROTNEND_ADDRESS, new JsonObject());
	}
}
