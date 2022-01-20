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
	    
	    processAutoBridge();
	    
	    JsonArray links = input.getJsonArray("link");
	    processLinks(links);
	    
	    processAutoLc();
	    
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
		Duration timeElapsed = Duration.between(start, end);
		logger.info("Graph queries creation: " + timeElapsed.getNano() / 1000000 + " ms.");
	    return true;
	}

	private void processHosts(JsonArray hosts) {
		String query = "T4@CREATE (:Host {name: '%s', hostname: '%s', type: '%s', mac: '%s', platform: '%s', "
				+ "bgpAsn: '%s', bgpStatus: '%s', hwsku: '%s'});";
		hosts.forEach(e -> {
	    	JsonObject host = (JsonObject) e;
	    	String result = String.format(query, 
	    			host.getString("name"), host.getString("hostname"),
    				host.getString("type"), host.getString("mac"),
    				host.getString("platform"),
    				host.getString("bgpAsn"), host.getString("bgpStatus"), host.getString("hwsku"));
	    	output.add(result);
	    });
	}
	
	private void processLtps(JsonArray ltps) {
		String query = "T5@MATCH (r:Host) WHERE r.name = '%s' "
				+ "CREATE (r)-[:CONTAINS]->(:Ltp {name: '%s', type: '%s', adminStatus: '%s', "
				+ "index: '%s', speed: '%s', mtu: '%s'});";
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
		String q = "T6@MATCH (h:Host {name:'%s'})-[:CONTAINS]->(l:Ltp{name:'%s'}) "
				+ "CREATE (l)-[:CONTAINS]->(c:EtherCtp {macAddr: '%s', vlan: '%s', mode: '%s'});";
		ctps.forEach(e -> {
			JsonObject ctp = (JsonObject) e;
	    	String result = String.format(q, 
	    			ctp.getString("host"), 
	    			ctp.getString("interface"), 
	    			ctp.getString("macAddr"), 
	    			ctp.getString("vlan"),
	    			ctp.getString("mode", "-"));
	    	output.add(result);
	    });
	}
	 
	private void processIp4Ctps(JsonArray ctps) {
		String q = "T7@MATCH (r:Host {name:'%s'})-[:CONTAINS]->(:Ltp{name:'%s'})-[:CONTAINS]->(c:EtherCtp) "
				+ "CREATE (c)-[:CONTAINS]->(ipc:Ip4Ctp {ipAddr:'%s', netMask:'%s', netAddr: '%s', svi:'%s', vlan:'%s'});";
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
		/* String q = "T9@MATCH (sR:Host {name:'%s'})-[:CONTAINS]->(src:Ltp{name:'%s'}) "
				+ "MATCH (tR:Host {name:'%s'})-[:CONTAINS]->(dst:Ltp{name:'%s'}) "
				+ "CREATE (src)-[:LINKED]->(dst);"; */
		String q = "T9@MATCH (sR:Host {name:'%s'})-[:CONTAINS]->(src:Ltp{name:'%s'})\r\n"
				+ "MATCH (tR:Host {name:'%s'})-[:CONTAINS]->(dst:Ltp{name:'%s'})\r\n"
				+ "WITH DISTINCT sR,tR,src,dst\r\n"
				+ "CALL apoc.do.case([\r\n"
				+ "(sR.type = 'LeafRouter' AND tR.type = 'LeafRouter'),\r\n"
				+ " 'CREATE (src)-[:LINKED_VLT {name: '%s'}]->(dst)',\r\n"
				+ "(sR.type = 'Server' OR tR.type ='Server'),\r\n"
				+ " 'CREATE (src)-[:LINKED_L2 {name: '%s'}]->(dst)'\r\n"
				+ "],'CREATE (src)-[:LINKED_L3 {name: '%s'}]->(dst)',{src:src, dst:dst})\r\n"
				+ "YIELD value\r\n"
				+ "RETURN value;";
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
		String q = "T10@MATCH (sC:EtherCtp)<-[:CONTAINS]-(sL:Ltp)-[:LINKED_L2|:LINKED_L3|:LINKED_VLT]->(dL:Ltp)-[:CONTAINS]->(dC:EtherCtp) "
				+ "WHERE NOT (sC)-[:LINK_CONN]-() AND NOT (dC)-[:LINK_CONN]-() "
				+ "AND NOT (sL)-[:CONTAINS]-(:Host {type:'Switch'}) AND NOT (dL)-[:CONTAINS]-(:Host {type:'Switch'}) "
				+ "CREATE (sC)-[r:LINK_CONN]->(dC);";
		// String vlan = "MATCH (c1:EtherCtp)-[:LINK_CONN]-(c2:EtherCtp) WHERE c1.vlan = 0 AND c2.vlan <> 0 "
		// 		+ "SET c1.vlan = c2.vlan;";
    	output.add(q);
    	// output.add(vlan);
	}
	
	private void processAutoBridge() {
		String q = "T17@MATCH (h:Host)\r\n"
				+ "MATCH (h)-[:CONTAINS*3]->(svi:Ip4Ctp) WHERE svi.svi <> '-' \r\n"
				+ "MATCH (h)-[:CONTAINS]->(l:Ltp)-[:CONTAINS]->(e:EtherCtp) WHERE e.vlan = svi.vlan AND NOT (e)-[:CONTAINS]->(:Ip4Ctp)\r\n"
				+ "CREATE (e)-[b:BRIDGE]->(svi)\r\n"
				+ "RETURN h, svi, b, e;";
    	output.add(q);
	}
	
	private void processIpConns(JsonArray ipConns) {
		String q = "T11@MATCH (r:Host {name:'%s'})-[:CONTAINS]->(sL:Ltp{name:'%s'})-[:CONTAINS*2]->(s:Ip4Ctp) "
				+ "MATCH (dC:EtherCtp{macAddr:'%s'})-[:CONTAINS]->(d:Ip4Ctp{ipAddr:'%s'}) "
				+ "CREATE (s)-[ip:IP_CONN]->(d);";
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
		String q = "T12@MATCH(r:Host {name:'%s'})-[:CONTAINS*3]->(c:Ip4Ctp{ipAddr:'%s'})\r\n"
				+ "CREATE (c)-[:HAS_CONFIG]->(b:Bgp {lId: '%s', lAsn: '%s', rAddr:'%s', rId:'%s', rAsn:'%s', "
				+ "state: '%s', holdTime: '%s', keepAlive: '%s'});";
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
		String q = "T13@MATCH (b1:Bgp)<-[:HAS_CONFIG]-(c1:Ip4Ctp)-[:IP_CONN]->(c2:Ip4Ctp)-[:HAS_CONFIG]->(b2:Bgp)\r\n"
				+ "WHERE EXISTS((c2)-[:IP_CONN]->(c1))\r\n"
				+ "AND b1.rAddr=c2.ipAddr AND b2.rAddr=c1.ipAddr AND b1.rAsn=b2.lAsn AND b2.rAsn=b1.lAsn AND b1.rId=b2.lId AND b2.rId=b1.lId\r\n"
				+ "WITH DISTINCT b1,b2\r\n"
				+ "CREATE (b1)-[:BGP_PEER]->(b2);";
		// TODO: consider status
    	output.add(q);
	}
	
	// ACL table
	private void sortAclTables(JsonArray aclTables) {
		// TODO: egress ACL
		// aclInInterface = new JsonArray();
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
		// ingress
		/* String qInGbl = "T14@CREATE (a:Acl{stage: '%s', name: '%s', binding: '%s', type: '%s', description: '%s'})\r\n"
				+ "WITH a\r\n"
				+ "MATCH (f:Host {name:'%s'})-[:CONTAINS]->(l:Ltp)-[:CONTAINS*2]->(c:Ip4Ctp)\r\n"
				+ "WHERE (NOT l.name STARTS WITH 'Loopback') AND (NOT (c)-[:HAS_ACL]->())\r\n"
				+ "CREATE (c)-[:HAS_ACL]->(a);"; */
		String qInGbl = "T14@CREATE (a:Acl{stage: '%s', name: '%s', binding: '%s', type: '%s', description: '%s', rules:[]})\r\n"
				+ "WITH a\r\n"
				+ "MATCH (h:Host {name:'%s'})\r\n"
				+ "CREATE (h)-[:ACL]->(a);";
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
		/* String q = "T15@CREATE (nr:AclRule {name:'%s', priority: %s, action:'%s', matching:'%s'})\r\n"
				+ "WITH nr\r\n"
				+ "MATCH (f:Host {name:'%s'})-[:CONTAINS*3]->(:Ip4Ctp)-[:HAS_ACL]->(t:Acl{name:'%s'})\r\n"
				+ "OPTIONAL MATCH (t)-[:NEXT_RULE*]->(r:AclRule) WHERE NOT EXISTS ((r)-[:NEXT_RULE]->())\r\n"
				+ "WITH DISTINCT nr, t, r\r\n"
				+ "CALL apoc.do.case([\r\n"
				+ "  (t IS NOT null AND r IS NOT null AND nr.action = 'FORWARD'),\r\n"
				+ "  'CREATE (r)-[:NEXT_RULE]->(nr)-[:ACCEPT]->(t)',\r\n"
				+ "  (t IS NOT null AND r IS NOT null AND nr.action = 'DROP'),\r\n"
				+ "  'CREATE (r)-[:NEXT_RULE]->(nr)',\r\n"
				+ "  (t IS NOT null AND r IS null AND nr.action = 'FORWARD'),\r\n"
				+ "  'CREATE (t)-[:NEXT_RULE]->(nr)-[:ACCEPT]->(t)',\r\n"
				+ "  (t IS NOT null AND r IS null AND nr.action = 'DROP'),\r\n"
				+ "  'CREATE (t)-[:NEXT_RULE]->(nr)'\r\n"
				+ "  ],\r\n"
				+ "  'DELETE nr',{nr:nr, t:t, r:r})\r\n"
				+ "YIELD value\r\n"
				+ "RETURN value;"; */
		String q = "T15@CREATE (nr:AclRule {name:'%s', priority: %s, action:'%s', matching:'%s'})\r\n"
				+ "WITH nr\r\n"
				+ "MATCH (h:Host {name:'%s'})-[:ACL]->(t:Acl{name:'%s'})\r\n"
				+ "OPTIONAL MATCH (t)-[:NEXT_RULE*]->(r:AclRule) WHERE NOT EXISTS ((r)-[:NEXT_RULE]->())\r\n"
				+ "WITH DISTINCT nr, t, r\r\n"
				+ "CALL apoc.do.case([\r\n"
				+ "  (t IS NOT null AND r IS NOT null AND nr.action = 'FORWARD'),\r\n"
				+ "  'CREATE (r)-[:NEXT_RULE]->(nr)-[:ACCEPT]->(t)',\r\n"
				+ "  (t IS NOT null AND r IS NOT null AND nr.action = 'DROP'),\r\n"
				+ "  'CREATE (r)-[:NEXT_RULE]->(nr)',\r\n"
				+ "  (t IS NOT null AND r IS null AND nr.action = 'FORWARD'),\r\n"
				+ "  'CREATE (t)-[:NEXT_RULE]->(nr)-[:ACCEPT]->(t)',\r\n"
				+ "  (t IS NOT null AND r IS null AND nr.action = 'DROP'),\r\n"
				+ "  'CREATE (t)-[:NEXT_RULE]->(nr)'\r\n"
				+ "  ],\r\n"
				+ "  'DELETE nr',{nr:nr, t:t, r:r})\r\n"
				+ "YIELD value\r\n"
				+ "RETURN value;";
		aclRules.forEach(e -> {
			JsonObject table = (JsonObject) e;
	    	String result = String.format(q, 
	    			table.getString("name"), 
	    			table.getLong("priority"), 
	    			table.getString("action"),
	    			table.getString("match"),
	    			table.getString("host"),
	    			table.getString("table"));
	    	output.add(result);
	    });
	}
	
	private void processIpRoutes(JsonArray ipRoutes) {		 
		String qItf = "T16@CREATE(r:Route {to: '%s', via: '%s', type: '%s'}) \r\n"
				+ "WITH r\r\n"
				+ "MATCH (h:Host {name:'%s'})-[:CONTAINS]->(l:Ltp{name:'%s'})-[:CONTAINS*2]->(rEx:Ip4Ctp)\r\n"
				+ "CREATE (r)-[:EGRESS]->(rEx)\r\n"
				+ "WITH r, h\r\n"
				+ "OPTIONAL MATCH (h)-[:ACL]->(ag:Acl)\r\n"
				+ "WITH DISTINCT h, ag, r\r\n"
				+ "CALL apoc.do.case([\r\n"
				+ "  (ag IS NOT null),\r\n"
				+ "  'CREATE (ag)-[:TO_ROUTE]->(r)'\r\n"
				+ "],\r\n"
				+ "'CREATE (h)-[:TO_ROUTE]->(r)',{h:h, ag:ag, r:r})\r\n"
				+ "YIELD value\r\n"
				+ "RETURN value;";
		String qSvi = "T17@CREATE(r:Route {to: '%s', via: '%s', type: '%s'}) \r\n"
				+ "WITH r\r\n"
				+ "MATCH (h:Host {name:'%s'})-[:CONTAINS*3]->(rEx:Ip4Ctp{svi:'%s'})\r\n"
				+ "CREATE (r)-[:EGRESS]->(rEx)\r\n"
				+ "WITH r, h\r\n"
				+ "OPTIONAL MATCH (h)-[:ACL]->(ag:Acl)\r\n"
				+ "WITH DISTINCT h, ag, r\r\n"
				+ "CALL apoc.do.case([\r\n"
				+ "  (ag IS NOT null),\r\n"
				+ "  'CREATE (ag)-[:TO_ROUTE]->(r)'\r\n"
				+ "],\r\n"
				+ "'CREATE (h)-[:TO_ROUTE]->(r)',{h:h, ag:ag, r:r})\r\n"
				+ "YIELD value\r\n"
				+ "RETURN value;";
		/* String qItf = "T16@CREATE(r:Route {to: '%s', via: '%s', type: '%s'}) \r\n"
				+ "WITH r\r\n"
				+ "MATCH (h:Host {name:'%s'})-[:CONTAINS]->(l:Ltp{name:'%s'})-[:CONTAINS*2]->(rEx:Ip4Ctp)\r\n"
				+ "CREATE (r)-[:EGRESS]->(rEx)\r\n"
				+ "WITH r, h\r\n"
				+ "MATCH (h)-[:CONTAINS]->(lo:Ltp) WHERE lo.name <> '%s'\r\n"
				+ "OPTIONAL MATCH (lo)-[:CONTAINS*2]->(:Ip4Ctp)-[:HAS_ACL]->(ag:Acl)\r\n"
				+ "OPTIONAL MATCH (lo)-[:CONTAINS*2]->(co:Ip4Ctp) WHERE NOT (co)-[:HAS_ACL]->(:Acl)\r\n"
				+ "WITH DISTINCT co, ag, r\r\n"
				+ "CALL apoc.do.case([\r\n"
				+ "  (ag IS NOT null),\r\n"
				+ "  'CREATE (ag)-[:TO_ROUTE]->(r)',\r\n"
				+ "  (co IS NOT null),\r\n"
				+ "  'CREATE (co)-[:TO_ROUTE]->(r)'\r\n"
				+ "],\r\n"
				+ "'',{co:co, ag:ag, r:r})\r\n"
				+ "YIELD value\r\n"
				+ "RETURN value;"; */
		/* String qSvi = "T17@CREATE(r:Route {to: '%s', via: '%s', type: '%s'}) \r\n"
				+ "WITH r\r\n"
				+ "MATCH (h:Host {name:'%s'})-[:CONTAINS*3]->(rEx:Ip4Ctp{svi:'%s'})\r\n"
				+ "CREATE (r)-[:EGRESS]->(rEx)\r\n"
				+ "WITH r, h\r\n"
				+ "MATCH (h)-[:CONTAINS]->(lo:Ltp) WHERE lo.name <> 'Bridge'\r\n"
				+ "OPTIONAL MATCH (lo)-[:CONTAINS*2]->(:Ip4Ctp)-[:HAS_ACL]->(ag:Acl)\r\n"
				+ "OPTIONAL MATCH (lo)-[:CONTAINS*2]->(co:Ip4Ctp) WHERE NOT (co)-[:HAS_ACL]->(:Acl)\r\n"
				+ "WITH DISTINCT co, ag, r\r\n"
				+ "CALL apoc.do.case([\r\n"
				+ "  (ag IS NOT null),\r\n"
				+ "  'CREATE (ag)-[:TO_ROUTE]->(r)',\r\n"
				+ "  (co IS NOT null),\r\n"
				+ "  'CREATE (co)-[:TO_ROUTE]->(r)'\r\n"
				+ "],\r\n"
				+ "'',{co:co, ag:ag, r:r})\r\n"
				+ "YIELD value\r\n"
				+ "RETURN value;"; */
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
