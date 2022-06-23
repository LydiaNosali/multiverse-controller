package io.nms.central.microservice.digitaltwin.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import io.nms.central.microservice.digitaltwin.model.graph.AclTable.AclStageEnum;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class GraphCreator {
	
	private static final Logger logger = LoggerFactory.getLogger(GraphCreator.class);
	
	private JsonObject input;
	private List<String> output;
	private List<String> report;
	
	// sort acl tables
	// private JsonArray aclInInterface = null;
	private JsonArray aclInGlobal = null;
	
	public GraphCreator(JsonObject input) {
		this.input = input;
		this.output = new ArrayList<String>();
	}
	
	public List<String> getOutput() {
		return output;
	}
	
	public List<String> getReport() {
		return report;
	}

	public boolean process() {
		if (input == null) {
			logger.info("Input object is null");
			return false;
		}
		report = new ArrayList<String>();
		logger.info("GraphDB creation... ");
		Instant start = Instant.now();
		
		output = new ArrayList<String>();
		
		JsonArray hosts = input.getJsonArray("host");
	    processHosts(hosts); 
	    
	    JsonArray ltps = input.getJsonArray("ltp");
	    processLtps(ltps);
	    
	    JsonArray etherCtps = input.getJsonArray("etherCtp");
	    processEtherCtps(etherCtps);
	    
	    JsonArray ip4Ctps = input.getJsonArray("ip4Ctp");
	    processIp4Ctps(ip4Ctps);
	    
	    JsonArray sviCtps = input.getJsonArray("sviCtp");
	    processIp4Ctps(sviCtps);
	    
	    processAutoVlanMembers();
	    
	    JsonArray links = input.getJsonArray("link");
	    // processLinks(links);
	    processLinksSimple(links);
	    
	    // processAutoLc();
	    processAutoLcSimple();
	    
	    JsonArray ipConns = input.getJsonArray("ipConn");
	    processIpConns(ipConns);
	    
	    JsonArray bgpNeighbors = input.getJsonArray("bgp");
	    processBgpNeighbors(bgpNeighbors);
	    connectBgpNeighbors();

	    JsonArray aclTables = input.getJsonArray("aclTable");
	    sortAclTables(aclTables);
	    processAclTables();

	    JsonArray aclRules = input.getJsonArray("aclRule");
	    JsonArray sortedAclRules = sortAclRules(aclRules);
	    processAclRules(sortedAclRules);
	    
	    JsonArray ipRoutes = input.getJsonArray("ipRoute");
	    processIpRoutes(ipRoutes);
	    
	    Instant end = Instant.now();
		logger.info("2- Queries creation time: " + Duration.between(start, end).toMillis() + " ms.");
	    return true;
	}

	private void processHosts(JsonArray hosts) {
		String query = "T4@" + CypherQuery.Graph.CREATE_HOST;
		hosts.forEach(e -> {
	    	JsonObject host = (JsonObject) e;
	    	String result = String.format(query, host.getString("type"),
	    			host.getString("name"), host.getString("hostname"),
    				host.getString("type"), host.getString("mac"),
    				host.getString("platform"),
    				host.getString("bgpAsn"), host.getString("bgpStatus"), host.getString("hwsku"));
	    	output.add(result);
	    });
	}
	
	private void processLtps(JsonArray ltps) {
		String query = "T5@" + CypherQuery.Graph.CREATE_LTP;
		ltps.forEach(e -> {
			JsonObject ltp = (JsonObject) e;
	    	String result = String.format(query, 
    				ltp.getString("host"), 
    				ltp.getString("name"), ltp.getString("type"), ltp.getString("adminStatus"),
    				ltp.getString("index"), ltp.getString("speed"), ltp.getString("mtu"));
    		output.add(result);
	    });
	}

	private void processEtherCtps(JsonArray ctps) {
		String q = "T6@" + CypherQuery.Graph.CREATE_ETHERCTP;
		ctps.forEach(e -> {
			JsonObject ctp = (JsonObject) e;
	    	String result = String.format(q, 
	    			ctp.getString("host"), 
	    			ctp.getString("interface"), 
	    			ctp.getString("macAddr"), 
	    			ctp.getString("vlan"),
	    			ctp.getString("mode"));
	    	output.add(result);
	    });
	}
	 
	private void processIp4Ctps(JsonArray ctps) {
		String q = "T7@" + CypherQuery.Graph.CREATE_IP4CTP;
		ctps.forEach(e -> {
			JsonObject ctp = (JsonObject) e;
	    	String result = String.format(q, 
	    			ctp.getString("host"), 
	    			ctp.getString("interface"), 
	    			ctp.getString("ipAddr"),
	    			ctp.getString("netMask"),
	    			ctp.getString("netAddr"),
	    			ctp.getString("svi"),
	    			ctp.getString("vlan"));
	    	output.add(result);
	    });
	}
	
	private void processLinks(JsonArray links) {
		String q = "T9@" + CypherQuery.Graph.CREATE_LINK;
		links.forEach(e -> {
			JsonObject link = (JsonObject) e;
	    	String result = String.format(q, 
	    			link.getString("srcHost"),
	    			link.getString("srcInterface"),
	    			link.getString("destHost"),
	    			link.getString("destInterface"),
	    			link.getString("name"),
	    			link.getString("name"),
	    			link.getString("name"));
	    	output.add(result);
	    });
	}
	
	private void processLinksSimple(JsonArray links) {
		String q = "T9@" + CypherQuery.Graph.CREATE_LINK_SIMPLE;
		links.forEach(e -> {
			JsonObject link = (JsonObject) e;
	    	String result = String.format(q, 
	    			link.getString("srcHost"),
	    			link.getString("srcInterface"),
	    			link.getString("destHost"),
	    			link.getString("destInterface"),
	    			link.getString("name"));
	    	output.add(result);
	    });
	}
	
	private void processAutoLc() {
		String q = "T10@" + CypherQuery.Graph.AUTO_LINKCONN;
    	output.add(q);
	}
	
	private void processAutoLcSimple() {
		String q = "T10@" + CypherQuery.Graph.AUTO_LINKCONN_SIMPLE;
    	output.add(q);
	}
	
	private void processAutoVlanMembers() {
		String q = "T17@" + CypherQuery.Graph.AUTO_VLAN_MEMBER;
    	output.add(q);
	}
	
	private void processIpConns(JsonArray ipConns) {
		String q = "T11@" + CypherQuery.Graph.CREATE_IPCONN;
		ipConns.forEach(e -> {
			JsonObject ipConn = (JsonObject) e;
	    	String result = String.format(q, 
	    			ipConn.getString("host"), 
	    			ipConn.getString("interface"),
	    			ipConn.getString("macAddr"),
	    			ipConn.getString("ipAddr"));
	    	output.add(result);
	    });
	}
	
	private void processBgpNeighbors(JsonArray bgpNeighbors) {
		String q = "T12@" + CypherQuery.Graph.CREATE_BGP;
		bgpNeighbors.forEach(e -> {
			JsonObject bgpN = (JsonObject) e;
	    	String result = String.format(q, 
	    			bgpN.getString("host"), 
	    			bgpN.getString("localAddr"),
	    			bgpN.getString("localId"),
	    			bgpN.getString("localAsn"), 
	    			bgpN.getString("remoteAddr"), 
	    			bgpN.getString("remoteId"),
	    			bgpN.getString("remoteAsn"),
	    			bgpN.getString("state"),
	    			bgpN.getString("holdTime"),
	    			bgpN.getString("keepAlive"));
	    	output.add(result);
	    });
	}
	
	private void connectBgpNeighbors() {
		String q = "T13@" + CypherQuery.Graph.AUTO_BGP_NEIGHBORS;
    	output.add(q);
	}
	
	// ACL table
	private void sortAclTables(JsonArray aclTables) {
		// TODO: egress ACL
		aclInGlobal = new JsonArray();
		aclTables.forEach(e -> {
			JsonObject table = (JsonObject) e;
	    	String stage = table.getString("stage");
	    	// String binding = table.getString("binding");
	    	if (stage.equals(AclStageEnum.ingress.getValue())) {
	    		aclInGlobal.add(table);
	    	}
	    });
	}
	
	private void processAclTables() {
		// ingress only
		String qInGbl = "T14@" + CypherQuery.Graph.CREATE_ACLTABLE;
		aclInGlobal.forEach(e -> {
			JsonObject table = (JsonObject) e;
	    	String result = String.format(qInGbl, 
	    			table.getString("stage"),
	    			table.getString("name"),
	    			table.getString("binding"),
	    			table.getString("type"),
	    			table.getString("description"),
	    			table.getString("host"));
	    	output.add(result);
	    });
	}
	
	// ACL rule
	private JsonArray sortAclRules(JsonArray aclRules) {	
		JsonArray sortedAclRules = new JsonArray();

		List<JsonObject> jsonValues = new ArrayList<JsonObject>();
		for (int i = 0; i < aclRules.size(); i++) {
		    jsonValues.add(aclRules.getJsonObject(i));
		}
		Collections.sort(jsonValues, new Comparator<JsonObject>() {
		    private static final String KEY = "priority";

		    @Override
		    public int compare(JsonObject a, JsonObject b) {
		        long valA, valB;
		        valA = a.getLong(KEY);
		        valB = b.getLong(KEY);
		        return (int) (valB - valA);
		    }
		});

		for (int i = 0; i < jsonValues.size(); i++) {
		    sortedAclRules.add((JsonObject)jsonValues.get(i));
		}
		return sortedAclRules;
	}

	private void processAclRules(JsonArray aclRules) {
		String q = "T15@" + CypherQuery.Graph.CREATE_ACLRULE;
		aclRules.forEach(e -> {
			JsonObject table = (JsonObject) e;
	    	String result = String.format(q, 
	    			table.getString("name"), 
	    			table.getLong("priority"), 
	    			table.getString("action"),
	    			table.getJsonArray("match").encode(),
	    			table.getString("host"),
	    			table.getString("table"));
	    	output.add(result);
	    });
	}
	
	private void processIpRoutes(JsonArray ipRoutes) {		 
		String qItf = "T16@" + CypherQuery.Graph.CREATE_ROUTE_ITF;
		String qSvi = "T17@" + CypherQuery.Graph.CREATE_ROUTE_SVI;
		ipRoutes.forEach(e -> {
			JsonObject ipRoute = (JsonObject) e;
	    	String itfName = ipRoute.getString("interface");
	    	if (itfName.startsWith("Vlan")) {
	    		String result = String.format(qSvi, 
	    				ipRoute.getString("to"),
		    			ipRoute.getString("via"),
		    			ipRoute.getString("type"),
		    			ipRoute.getString("host"),
		    			itfName);
		    	output.add(result);
	    	} else {
	    		String result = String.format(qItf, 
	    				ipRoute.getString("to"), 
		    			ipRoute.getString("via"),
		    			ipRoute.getString("type"),
		    			ipRoute.getString("host"),
		    			itfName);
		    	output.add(result);
	    	}
	    });
	}
}
