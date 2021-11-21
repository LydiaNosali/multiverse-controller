package io.nms.central.microservice.digitaltwin.impl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class ConfigGenerator {
	
	private static final Logger logger = LoggerFactory.getLogger(ConfigGenerator.class);

	private JsonObject output;
	
	public ConfigGenerator() {}

	/* public void processNetConfig(List<JsonObject> configs, Handler<AsyncResult<JsonObject>> resultHandler) {
		CompletableFuture.supplyAsync(() -> {
			output = new JsonObject();
			for (JsonObject device : configs) {
				output.put(device.getString("device"), getDeviceConfig(device.getJsonObject("config")));
			}
			return output;
		}).thenAccept((r) -> {
			logger.info("final net config: " + r.encodePrettily());
			resultHandler.handle(Future.succeededFuture(r));
		});
	} */
	
	public JsonObject processNetConfig(List<JsonObject> configs) {
		output = new JsonObject();
		for (JsonObject device : configs) {
			output.put(device.getString("device"), getDeviceConfig(device.getJsonObject("config")));
		}
		return output;
	}
	
	public JsonObject getDeviceConfig(JsonObject config) {
		JsonObject deviceConfigOutput = new JsonObject();
		putConfigMetadata(deviceConfigOutput, config.getJsonArray("metadata").getList());
		putConfigInterfaces(deviceConfigOutput, config.getJsonArray("interface").getList());
		putConfigLoopbackInterfaces(deviceConfigOutput, config.getJsonArray("loopbackInterface").getList());
		putConfigPorts(deviceConfigOutput, config.getJsonArray("port").getList());
		putConfigVlans(deviceConfigOutput, config.getJsonArray("vlan").getList());
		putConfigVlanMembers(deviceConfigOutput, config.getJsonArray("vlanMember").getList());
		putConfigBgpNeighbors(deviceConfigOutput, config.getJsonArray("bgpNeighbor").getList());
		putConfigAclTables(deviceConfigOutput, config.getJsonArray("aclTable").getList());
		putConfigAclRules(deviceConfigOutput, config.getJsonArray("aclRule").getList());
		return deviceConfigOutput;
	}
	
	/*
	"DEVICE_METADATA": {
        "localhost": {
            "bgp_asn": "65101",
            "default_bgp_status": "up",
            "default_pfcwd_status": "disable",
            "hostname": "leaf01",
            "hwsku": "Force10-S6000",
            "mac": "52:11:00:11:11:56",
            "platform": "x86_64-kvm_x86_64-r0",
            "type": "LeafRouter"
        }
    } */
	private void putConfigMetadata(JsonObject output, List<JsonObject> records) {
		JsonObject metadata = new JsonObject();
		JsonObject rec = records.get(0);
		JsonObject localhost = new JsonObject();
		localhost.put("bgp_asn", rec.getString("bgpAsn"));
		localhost.put("default_bgp_status", rec.getString("bgpStatus"));
		localhost.put("default_pfcwd_status", "?");	// not found in collected config!!!
		localhost.put("hostname", rec.getString("hostname"));
		localhost.put("hwsku", rec.getString("hwsku"));
		localhost.put("mac", rec.getString("mac"));
		localhost.put("platform", rec.getString("platform"));
		localhost.put("type", rec.getString("type"));

		metadata.put("localhost", localhost);
		output.put("DEVICE_METADATA", metadata);
	}
	
	/*
	"INTERFACE": {
        "Ethernet0": {},
        "Ethernet20|172.0.1.1/31": {},
    } */
	private void putConfigInterfaces(JsonObject output, List<JsonObject> records) {
		JsonObject interfaces = new JsonObject();
		for (JsonObject r: records) {
			JsonObject intf = new JsonObject();
			String name = r.getString("interface");
			String ipAddress = r.getString("ipAddress");
			String key = name;
			if (ipAddress != null) {
				key+="|"+ipAddress;
			}
			interfaces.put(key, intf);
		}
		output.put("INTERFACE", interfaces);
	}
	
	/*
	"LOOPBACK_INTERFACE": {
        "Loopback0": {},
        "Loopback0|10.255.255.1/32": {}
    } */
	private void putConfigLoopbackInterfaces(JsonObject output, List<JsonObject> records) {
		JsonObject interfaces = new JsonObject();
		for (JsonObject r: records) {
			JsonObject intf = new JsonObject();
			String name = r.getString("interface");
			String ipAddress = r.getString("ipAddress");
			interfaces.put(name, intf);
			interfaces.put(name+"|"+ipAddress, intf);
		}
		output.put("LOOPBACK_INTERFACE", interfaces);
	}
	
	/*
	"PORT": {
        "Ethernet0": {
            "admin_status": "up",
            "alias": "fortyGigE0/0",
            "index": "0",
            "lanes": "25,26,27,28",
            "mtu": "9216",
            "speed": "40000"
        },
    } */
	private void putConfigPorts(JsonObject output, List<JsonObject> records) {
		JsonObject ports = new JsonObject();
		for (JsonObject r: records) {
			JsonObject port = new JsonObject();
			String name = r.getString("name");
			
			port.put("admin_status", r.getString("adminStatus"));
			port.put("alias", "?");		// not found in collected config!!!
			port.put("index", r.getString("index"));
			port.put("lanes", "?");		// not found in collected config!!!
			port.put("mtu", r.getString("mtu"));
			port.put("speed", r.getString("speed"));
			ports.put(name, port);
		}
		output.put("PORT", ports);
	}
	
	/*
	"VLAN": {
        "Vlan101": {
            "members": [
                "Ethernet0"
            ],
            "vlanid": "101"
        }
    },
    "VLAN_INTERFACE": {
        "Vlan101": {},
        "Vlan101|10.0.101.1/24": {}
    }
	*/
	private void putConfigVlans(JsonObject output, List<JsonObject> records) {
		JsonObject vlans = new JsonObject();
		JsonObject vlanInterfaces = new JsonObject();
		for (JsonObject r: records) {	
			String name = r.getString("name");
			String vlanid = r.getString("vlanid");
			String vlanaddr = r.getString("vlanaddr");
			JsonArray members = r.getJsonArray("members");
			
			JsonObject vl = new JsonObject();
			vl.put("members", members);
			vl.put("vlanid", vlanid);
			vlans.put(name, vl);
			
			JsonObject vlInterface = new JsonObject();
			vlanInterfaces.put(name, vlInterface);
			vlanInterfaces.put(name+"|"+vlanaddr, vlInterface);	
		}
		output.put("VLAN", vlans);
		output.put("VLAN_INTERFACE", vlanInterfaces);
	}
	
	/*
	"VLAN_MEMBER": {
        "Vlan101|Ethernet0": {
            "tagging_mode": "untagged"
        }
    }
	*/
	private void putConfigVlanMembers(JsonObject output, List<JsonObject> records) {
		JsonObject vlanMembers = new JsonObject();
		for (JsonObject r: records) {
			String name = r.getString("name");
			String member = r.getString("member");
			String taggingMode = r.getString("mode");
			
			JsonObject vlMember = new JsonObject();
			vlMember.put("tagging_mode", taggingMode);
			vlanMembers.put(name+"|"+member, vlMember);
		}
		output.put("VLAN_MEMBER", vlanMembers);
	}
	
	/*
	"BGP_NEIGHBOR": {
        "172.0.1.0": {
            "asn": "65199",
            "holdtime": "9",
            "keepalive": "3",
            "local_addr": "172.0.1.1",
            "name": "spine01",
            "nhopself": "0",
            "rrclient": "0"
     }
	 */
	private void putConfigBgpNeighbors(JsonObject output, List<JsonObject> records) {
		JsonObject bgpNeighbors = new JsonObject();
		for (JsonObject r: records) {
			String ngbrName = r.getString("name");
			String localAddr = r.getString("local_addr");
			String remoteAddr = r.getString("remote_addr");
			String ngbrAsn = r.getString("asn");
			
			JsonObject bgpNgbr = new JsonObject();
			bgpNgbr.put("asn", ngbrAsn);
			bgpNgbr.put("name", ngbrName);
			bgpNgbr.put("local_addr", localAddr);
			bgpNgbr.put("holdtime", "-");
			bgpNgbr.put("keepalive", "-");
			bgpNgbr.put("nhopself", "-");
			bgpNgbr.put("rrclient", "-");
			
			bgpNeighbors.put(remoteAddr, bgpNgbr);
		}
		output.put("BGP_NEIGHBOR", bgpNeighbors);
	}
	
	/*
	"ACL_TABLE": {
        "BLK_SSH": {
            "policy_desc": "drop",
            "services": [
                    "22"
            ],
            "stage": "ingress",
            "type": "CTRLPLANE"
        }
    }
	*/
	private void putConfigAclTables(JsonObject output, List<JsonObject> records) {
		JsonObject aclTables = new JsonObject();
		for (JsonObject r: records) {
			String tblName = r.getString("name");
			String services = r.getString("services");
			String stage = r.getString("stage");
			String type = r.getString("type");
			String description = r.getString("description");
			
			JsonObject aclTbl = new JsonObject();
			aclTbl.put("policy_desc", description);
			aclTbl.put("services", services);
			aclTbl.put("stage", stage);
			aclTbl.put("type", type);
			
			aclTables.put(tblName, aclTbl);
		}
		output.put("ACL_TABLE", aclTables);
	}
	
	/*
	"ACL_RULE": {
        "BLK_SSH|ACE_FORWARD": {
            "PACKET_ACTION": "FORWARD",
            "PRIORITY": "1",
            "IP_TYPE": "ANY"
     }
    }
	*/
	private void putConfigAclRules(JsonObject output, List<JsonObject> records) {
		JsonObject aclRules = new JsonObject();
		for (JsonObject r: records) {
			String ruleName = r.getString("name");
			String action = r.getString("action");
			long priority = r.getLong("priority");
			String match = r.getString("matching");
			String table = r.getString("table");
			
			JsonObject rule = new JsonObject();
			rule.put("PACKET_ACTION", action);
			rule.put("PRIORITY", String.valueOf(priority));
			
			String[] matchParse = match.split(":");
			if (matchParse.length == 2) {
				String key = matchParse[0].trim();
				String value = matchParse[1].trim();
				rule.put(key, value);
			}

			aclRules.put(table+"|"+ruleName, rule);
		}
		output.put("ACL_RULE", aclRules);
	}
	
}
