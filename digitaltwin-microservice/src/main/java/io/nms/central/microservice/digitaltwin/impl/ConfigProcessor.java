package io.nms.central.microservice.digitaltwin.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.net.util.SubnetUtils;

import io.nms.central.microservice.digitaltwin.model.graph.AclRule;
import io.nms.central.microservice.digitaltwin.model.graph.AclTable;
import io.nms.central.microservice.digitaltwin.model.graph.Arp;
import io.nms.central.microservice.digitaltwin.model.graph.Bgp;
import io.nms.central.microservice.digitaltwin.model.graph.DeviceState;
import io.nms.central.microservice.digitaltwin.model.graph.HostTypeEnum;
import io.nms.central.microservice.digitaltwin.model.graph.Interface;
import io.nms.central.microservice.digitaltwin.model.graph.Interface.InterfaceTypeEnum;
import io.nms.central.microservice.digitaltwin.model.graph.IpRoute;
import io.nms.central.microservice.digitaltwin.model.graph.Lldp;
import io.nms.central.microservice.digitaltwin.model.graph.Metadata;
import io.nms.central.microservice.digitaltwin.model.graph.NetworkState;
import io.nms.central.microservice.digitaltwin.model.graph.Vlan;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class ConfigProcessor {
	
	private static final Logger logger = LoggerFactory.getLogger(ConfigProcessor.class);
	
	// private static FileWriter file;
	private NetworkState netConfig;
	private JsonObject output;
	private List<String> report;
	
	private JsonArray hosts = new JsonArray();
	private JsonArray ltps = new JsonArray();
	private JsonArray etherCtps = new JsonArray();
	private JsonArray ip4Ctps = new JsonArray();
	private JsonArray sviCtps = new JsonArray();
	private JsonArray links = new JsonArray();
	private JsonArray lcs = new JsonArray();
	private JsonArray ipConns = new JsonArray();
	private JsonArray ipRoutes = new JsonArray();	
	private JsonArray bgpPeers = new JsonArray();
	private JsonArray aclTables = new JsonArray();
	private JsonArray aclRules = new JsonArray();
	
	public ConfigProcessor(NetworkState netConfig) {
		this.netConfig = netConfig;
		output = new JsonObject();
		output.put("host", hosts);
		output.put("ltp", ltps);
		output.put("etherCtp", etherCtps);
		output.put("ip4Ctp", ip4Ctps);
		output.put("sviCtp", sviCtps);
		output.put("link", links);
		output.put("lc", lcs);
		output.put("ipConn", ipConns);
		output.put("ipRoute", ipRoutes);	
		output.put("bgp", bgpPeers);
		output.put("aclTable", aclTables);
		output.put("aclRule", aclRules);
	}
	
	public JsonObject getOutput() {
		return output;
	}
	
	public List<String> getReport() {
		return report;
	}

	public boolean process() {
		if (netConfig == null) {
			logger.info("Net config cannot be null");
			return false;
		}
		report = new ArrayList<String>();
		logger.info("Processing collected network state... ");
		Instant start = Instant.now();
		
		// 1st iteration over device configs
		Map<String, DeviceState> deviceConfigs = netConfig.getConfigs();
		for (Map.Entry<String,DeviceState> device : deviceConfigs.entrySet()) {
			String deviceName = device.getKey();
			DeviceState config = device.getValue();
		    
		    // step 0: hosts and metadata
		    Metadata meta = config.getMetadata();
		    HostTypeEnum deviceType = meta.getType();
			
		    JsonObject newDevice = new JsonObject();
		    newDevice.put("name", deviceName);
		    newDevice.put("type", deviceType.getValue());
		    newDevice.put("hostname", meta.getHostname());
		    newDevice.put("mac", meta.getMac());
		    newDevice.put("platform", meta.getPlatform());
		    if (deviceType.equals(HostTypeEnum.server)) {
		    	newDevice.put("bgpAsn", "-");
		    	newDevice.put("bgpStatus", "-");
		    	newDevice.put("hwsku", "-");
		    } else {
		    	newDevice.put("bgpAsn", meta.getBgpAsn());
		    	newDevice.put("bgpStatus", meta.getBgpStatus().getValue());
		    	newDevice.put("hwsku", meta.getHwsku());
		    }
		    hosts.add(newDevice);
	    
		    // step 1: interfaces
		    // JSONArray interfaces = (JSONArray) ((JSONObject) host.get("interface")).get("rows");
		    List<Interface> netItfs = config.getNetInterface();
		    if (deviceType.equals(HostTypeEnum.server)) {
	    		processServerInterfaces(deviceName, deviceType, netItfs);
	    	} else if (deviceType.equals(HostTypeEnum.BorderRouter) 
	    			|| deviceType.equals(HostTypeEnum.SpineRouter) 
	    			|| deviceType.equals(HostTypeEnum.LeafRouter) 
	    			|| deviceType.equals(HostTypeEnum.Firewall)) {
	    		processRouterInterfaces(deviceName, deviceType, netItfs);
	    	}

		    // step 2: VLAN for Routers
		    if (deviceType.equals(HostTypeEnum.BorderRouter) 
	    			|| deviceType.equals(HostTypeEnum.SpineRouter) 
	    			|| deviceType.equals(HostTypeEnum.LeafRouter)) {
		    	List<Vlan> vlans = config.getVlan();
		    	vlans.forEach(v -> {
		    		findEtherAndSetVlan(deviceName, v);
		    	});
		    }
		}
		
		// 2nd iteration over device configs
		for (Map.Entry<String,DeviceState> device : deviceConfigs.entrySet()) {
			String deviceName = device.getKey();
			DeviceState config = device.getValue();
			HostTypeEnum deviceType = config.getMetadata().getType();
		    
		    // step 3: LLDP
			if (deviceType.equals(HostTypeEnum.BorderRouter) 
	    			|| deviceType.equals(HostTypeEnum.SpineRouter) 
	    			|| deviceType.equals(HostTypeEnum.LeafRouter) 
	    			|| deviceType.equals(HostTypeEnum.Firewall)) {
		    	List<Lldp> lldps = config.getLldp();
		    	lldps.forEach(e -> {
		    		String srcHost = deviceName;
		    		String srcInterface = e.getLocalPort();
		    		String destHost = e.getRemoteDevice();
		    		String destInterface = e.getRemotePort();
		    		
		    		if (isMacAddress(destInterface)) {
		    			DeviceState remoteHostConfig = deviceConfigs.get(destHost);
		    			destInterface =
		    				findItfNameByMacAddr(remoteHostConfig.getNetInterface(), destInterface);
		    			// logger.info("On device ["+deviceName+"] at LLDP stage: ");
		    			// logger.info("\tFound MAC address instead of interface name to device "+destHost);
		    			// logger.info("\tReplace with: " + destInterface);
		    		}
		    		
		    		// check if device is included in DT?
		    		if (deviceConfigs.keySet().contains(destHost)) {
		    			if (! linkExists(destHost, destInterface, srcHost, srcInterface)) {
		    				JsonObject link = new JsonObject();
		    				link.put("name", srcHost+"."+srcInterface+"-"+destHost+"."+destInterface);
		    				link.put("srcHost", srcHost);
		    				link.put("srcInterface", srcInterface);
		    				link.put("destHost", destHost);
		    				link.put("destInterface", destInterface);
		    				links.add(link);
		    			}
		    		} else {
		    			String msg = "On device ["+deviceName+"] LLDP at stage: device <"+destHost+"> "
		    					+ "not included in collected config";
		    			report.add("WARN: "+msg);
		    			// logger.info(msg);
		    		}
		    	});
		    }
		    
		    // step 4: CAM (switch)
		    // TODO
		    
		    // step 5: ARP
		    if (!deviceType.equals(HostTypeEnum.Switch)) {
		    	List<Arp> arps = config.getArp();
		    	arps.forEach(e -> {
		    		if (!ipExists(e.getIpAddr())) {
		    			String msg = "On device <"+deviceName+"> at ARP stage: IP <" + e.getIpAddr() + "> "
		    					+ "not found in collected config";
		    			report.add("WARN: "+msg);
		    			// logger.info(msg);
		    			return;
		    		}

		    		JsonObject ipConn = new JsonObject();
		    		ipConn.put("host", deviceName);
		    		ipConn.put("macAddr", e.getMacAddr());
		    		ipConn.put("ipAddr", e.getIpAddr());
		    		
		    		if (e.getNetInterface().equals("-")) {
		    			ipConn.put("interface", "Bridge");
		    			ipConn.put("vlan", e.getVlan());
		    		} else {
		    			ipConn.put("interface", e.getNetInterface());
		    			ipConn.put("vlan", "-");
		    		}
		    		ipConns.add(ipConn);
		    	});
		    }
		    	
		    // step 6: BGP
		    if (config.getBgp() != null) {
		    	List<Bgp> bgps = config.getBgp();
		    	bgps.forEach(e -> {
		    		JsonObject bgpPeer = new JsonObject();
		    		bgpPeer.put("host", deviceName);
		    		bgpPeer.put("localId", e.getLocalRouterId());
		    		bgpPeer.put("localAddr", e.getLocalAddress());
		    		bgpPeer.put("localAsn", e.getLocalAs());
		    		bgpPeer.put("remoteId", e.getRemoteRouterId());
		    		bgpPeer.put("remoteAddr", e.getNeighborAddress());
		    		bgpPeer.put("remoteAsn", e.getRemoteAs());
		    		bgpPeer.put("state", e.getBgpState().getValue());
		    		bgpPeer.put("holdTime", e.getHoldTime());
		    		bgpPeer.put("keepAlive", e.getKeepAlive()); 			
		    		bgpPeers.add(bgpPeer);
		    	});
		    }
		    
		    // step 7: ACL
		    if (deviceType.equals(HostTypeEnum.BorderRouter) 
	    			|| deviceType.equals(HostTypeEnum.SpineRouter) 
	    			|| deviceType.equals(HostTypeEnum.LeafRouter) 
	    			|| deviceType.equals(HostTypeEnum.Firewall)) {
		    	if (config.getAclTable() != null) {
		    		List<AclTable> acls = config.getAclTable();
		    		acls.forEach(e -> {
		    			JsonObject newAclTable = new JsonObject();
		    			newAclTable.put("host", deviceName);
		    			newAclTable.put("name", e.getName());
		    			newAclTable.put("type", e.getType().getValue());
		    			newAclTable.put("binding", e.getBinding());
		    			newAclTable.put("stage", e.getStage().getValue());
		    			newAclTable.put("description", e.getDescription());
		    			aclTables.add(newAclTable);
		    		});
		    	}
		    	
		    	if (config.getAclRule() != null) {
		    		List<AclRule> rules = config.getAclRule();
		    		rules.forEach(e -> {	
		    			JsonObject newAclRule = new JsonObject();
		    			newAclRule.put("host", deviceName);
		    			newAclRule.put("table", e.getTableName());
		    			newAclRule.put("name", e.getRuleName());
		    			newAclRule.put("priority", Long.valueOf(e.getPriority()));
		    			newAclRule.put("action", e.getAction());
		    			newAclRule.put("match", e.getMatch());
		    			aclRules.add(newAclRule);
		    		});
		    	}
		    }
	    
		    // step 8: Routes
		    if (!deviceType.equals(HostTypeEnum.Switch)) {
		    	List<IpRoute> routes = config.getIpRoute();
		    	routes.forEach(e -> {
		    		if (!itfExists(e.getNetInterface())) {
		    			String msg = "On device <"+deviceName+"> at Routing stage: interface <" + e.getNetInterface() + "> "
		    					+ "not found in collected config";
		    			report.add("WARN: "+msg);
		    			logger.info(msg);
		    			return;
		    		}
		    		JsonObject newIpRoute = new JsonObject();
		    		newIpRoute.put("host", deviceName);
		    		newIpRoute.put("interface", e.getNetInterface());
		    		newIpRoute.put("via", e.getVia());
		    		newIpRoute.put("to", e.getTo());
		    		newIpRoute.put("type", e.getType());
		    		ipRoutes.add(newIpRoute);
		    	});
		    }
		}
		Instant end = Instant.now();
		Duration timeElapsed = Duration.between(start, end);
		// logger.info("Config processing: " + timeElapsed.getNano() / 1000000 + " ms.");

		return true;
	}
	
	private void processRouterInterfaces(String deviceName, HostTypeEnum hostType, List<Interface> interfaces) {
		interfaces.forEach(e -> {
	    	// skip useless interfaces
	    	String itfName = e.getName();
	    	if (itfName.equals("dummy") || 
	    			itfName.equals("lo") || 
	    			itfName.equals("docker0")) {
	    		return;
	    	}
	    	
	    	InterfaceTypeEnum itfType = e.getType(); 
	    	if ( !itfType.equals(InterfaceTypeEnum.Vlan) ) {
	    		// LTP
	    		JsonObject ltp = new JsonObject();
	    		ltp.put("host", deviceName);
	    		ltp.put("hostType", hostType.getValue());
	    		ltp.put("name", itfName);
	    		ltp.put("type", itfType.getValue());
	    		ltp.put("adminStatus", e.getAdminStatus().getValue());
	    		ltp.put("index", e.getIndex());
	    		ltp.put("mtu", e.getMtu());
	    		ltp.put("speed", e.getSpeed());
	    		ltps.add(ltp);
	    		
	    		// EtherCtp
		    	JsonObject etherCtp = new JsonObject();
		    	etherCtp.put("host", deviceName);
		    	etherCtp.put("interface", itfName);
		    	etherCtp.put("macAddr", e.getMacAddr());
		    	etherCtp.put("vlan", "0");
		    	etherCtps.add(etherCtp);
		    	
		    	// Ip4Ctp
		    	String ipAddr = e.getIpAddr();
		    	if (!ipAddr.isEmpty()) {
		    		String netAddr = getSubnetAddr(ipAddr);
		    		if (!netAddr.isEmpty()) {
		    			String[] ip = ipAddr.split("/");
		    			JsonObject ip4Ctp = new JsonObject();
		    			ip4Ctp.put("host", deviceName);
		    			ip4Ctp.put("interface", itfName);
		    			ip4Ctp.put("vlan", "0");
		    			ip4Ctp.put("ipAddr", ip[0]);
		    			ip4Ctp.put("netMask", "/"+ip[1]);
		    			ip4Ctp.put("netAddr", netAddr);
		    			ip4Ctp.put("svi", "-");		// for consistent Ip4Ctp object
		    			ip4Ctps.add(ip4Ctp);
		    		} else {
		    			String msg = "Interface <"+itfName+"> on device <"+deviceName+"> has incorrect IP format <"+ipAddr+">";
		    			report.add("WARN: "+msg);
		    			// logger.info(msg);
		    		}
			    }
		    } else { // SVI
		    	String sviItf = 
		    			findItfNameByMacAddr(interfaces, e.getMacAddr());
		    	
		    	String ipAddr = e.getIpAddr();
		    	if (!ipAddr.isEmpty()) {
		    		String netAddr = getSubnetAddr(ipAddr);
		    		if (!netAddr.isEmpty()) {
		    			String[] ip = ipAddr.split("/");
		    			String vid = e.getName().substring(4);		// SONiC-dependent
		    			JsonObject sviCtp = new JsonObject();
		    			sviCtp.put("host", deviceName);
		    			sviCtp.put("interface", sviItf);
		    			sviCtp.put("svi", itfName);
		    			sviCtp.put("vlan", vid);
		    			sviCtp.put("ipAddr", ip[0]);
		    			sviCtp.put("netMask", "/"+ip[1]);
		    			sviCtp.put("netAddr", netAddr);
		    			sviCtps.add(sviCtp);
		    		} else {
		    			String msg = "Interface <"+itfName+"> on device <"+deviceName+"> has incorrect IP format <"+ipAddr+">";
		    			report.add("WARN: "+msg);
		    			// logger.info(msg);
		    		}
		    	}
		    }
	    });
	}
	
	private void processServerInterfaces(String deviceName, HostTypeEnum hostType, List<Interface> interfaces) {
		interfaces.forEach(e -> {
	    	// skip useless interfaces
	    	String itfName = e.getName();
	    	if (itfName.equals("dummy") || 
	    			itfName.equals("lo") || 
	    			itfName.equals("docker0")) {
	    		return;
	    	}

	    	// LTP
	    	JsonObject ltp = new JsonObject();
	    	ltp.put("host", deviceName);
	    	ltp.put("hostType", hostType.getValue());
	   		ltp.put("name", itfName);
	   		ltp.put("type", e.getType().getValue());

	   		// For consistent Ltp object. Not applicable fields?
    		ltp.put("adminStatus", "-");
    		ltp.put("index", "-");
    		ltp.put("mtu", "-");
    		ltp.put("speed", "-");
    		ltps.add(ltp);
	    		
	    	// EtherCtp
		   	JsonObject etherCtp = new JsonObject();
		   	etherCtp.put("host", deviceName);
		   	etherCtp.put("interface", e.getName());
		   	etherCtp.put("macAddr", e.getMacAddr());
	    	etherCtp.put("vlan", "0");
	    	etherCtps.add(etherCtp);
		    	
		    // Ip4Ctp
		   	String ipAddr = e.getIpAddr();
		   	if (!ipAddr.isEmpty()) {
		    	String netAddr = getSubnetAddr(ipAddr);
		    	if (!netAddr.isEmpty()) {
		   			String[] ip = ipAddr.split("/");
		   			JsonObject ip4Ctp = new JsonObject();
		   			ip4Ctp.put("host", deviceName);
		   			ip4Ctp.put("interface", e.getName());
	    			ip4Ctp.put("vlan", "0");
	    			ip4Ctp.put("ipAddr", ip[0]);
		    		ip4Ctp.put("netMask", "/"+ip[1]);
		    		ip4Ctp.put("netAddr", netAddr);
		    		ip4Ctp.put("svi", "-");		// for consistend Ip4Ctp object
		   			ip4Ctps.add(ip4Ctp);
		    	} else {
	    			String msg = "Interface <"+itfName+"> on device <"+deviceName+"> has unexpected IP <"+ipAddr+">";
	    			report.add("WARN: "+msg);
	    			// logger.info(msg);
	    		}
		   	}
	    });
	}
	
	private String findItfNameByMacAddr(List<Interface> interfaces, String macAddr) {
		for(Interface itf : interfaces) {
			if(itf.getMacAddr().equals(macAddr)) {
				return itf.getName();
	    	}
		}
		return "-";
	}

	private void findEtherAndSetVlan(String deviceName, Vlan vlan) {
		String member = vlan.getMember();
		String vid = vlan.getVid();
		String mode = vlan.getMode().getValue();
		
		JsonArray etherCtps = output.getJsonArray("etherCtp");
		for (int i=0; i<etherCtps.size(); i++) {
			String h = etherCtps.getJsonObject(i).getString("host");
			String n = etherCtps.getJsonObject(i).getString("interface");
			if (h.equals(deviceName) && n.equals(member)) {
				JsonObject obj = etherCtps.getJsonObject(i);
				obj.put("vlan", vid);
				obj.put("mode", mode);
				return;
	    	}
		}
	}
	
	private String getSubnetAddr(String cidrIp) {
		try {
			SubnetUtils subnet = new SubnetUtils(cidrIp);
			return subnet.getInfo().getNetworkAddress();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
		return "";
	}
	
	private boolean linkExists(String srcH, String srcI, String dstH, String dstI) {
		JsonArray links = output.getJsonArray("link");
		for (int i=0; i<links.size(); i++) {
			String sH = links.getJsonObject(i).getString("srcHost");
			String sI = links.getJsonObject(i).getString("srcInterface");
			String dH = links.getJsonObject(i).getString("destHost");
			String dI = links.getJsonObject(i).getString("destInterface");
			if (sH.equals(srcH) && sI.equals(srcI) && dH.equals(dstH) && dI.equals(dstI)) {
				return true;
	    	}
		}
		return false;
	}
	
	private boolean ipExists(String ip) {
		JsonArray ip4Ctps = output.getJsonArray("ip4Ctp");
		for (int i=0; i<ip4Ctps.size(); i++) {
			String ipH = ip4Ctps.getJsonObject(i).getString("ipAddr");
			if (ipH.equals(ip)) {
				return true;
	    	}
		}
		JsonArray sviCtps = output.getJsonArray("sviCtp");
		for (int i=0; i<sviCtps.size(); i++) {
			String ipH = sviCtps.getJsonObject(i).getString("ipAddr");
			if (ipH.equals(ip)) {
				return true;
	    	}
		}
		return false;
	}
	
	private boolean itfExists(String name) {
		if (name.startsWith("Vlan")) {
			return true;
		}
		JsonArray ltps = output.getJsonArray("ltp");
		for (int i=0; i<ltps.size(); i++) {
			String itfName = ltps.getJsonObject(i).getString("name");
			if (itfName.equals(name)) {
				return true;
	    	}
		}
		return false;
	}
	
	private boolean isMacAddress(String str) {
		if (str == null) {
			return false;
		}
		String regex = "^([0-9A-Fa-f]{2}[:-])"
                       + "{5}([0-9A-Fa-f]{2})|"
                       + "([0-9a-fA-F]{4}\\."
                       + "[0-9a-fA-F]{4}\\."
                       + "[0-9a-fA-F]{4})$";
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(str);
		return m.matches();
	}
}
