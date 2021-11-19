package io.nms.central.microservice.digitaltwin.impl;

import java.util.Arrays;
import java.util.List;

public class CypherQuery {
	public static final String CLEAR_DB = "MATCH (n) DETACH DELETE n";
	
	
	public static class Graph {
		public static final String CREATE_HOST = "";
		public static final String CREATE_LTP = "";
		public static final String CREATE_ETHERCTP = "";
		public static final String CREATE_IP4CTP = "";
		public static final String CREATE_LINK = "";
		public static final String CREATE_LINKCONN = "";
		public static final String CREATE_IPCONN = "";
		public static final String CREATE_BGP = "";
		public static final String CREATE_ROUTE = "";
		public static final String CREATE_ACLTABLE = "";
		public static final String CREATE_ACLRULE = "";
	}
	
	public static class Constraints {
		public static final String UNIQUE_HOST = "CREATE CONSTRAINT unique_host IF NOT EXISTS ON (h:Host) ASSERT h.name IS UNIQUE";
		public static final String UNIQUE_HOSTNAME = "CREATE CONSTRAINT unique_hostname IF NOT EXISTS ON (h:Host) ASSERT h.hostname IS UNIQUE";
		public static final String UNIQUE_LTP = "";
		public static final String UNIQUE_ETHERCTP = "";
		public static final String UNIQUE_IP4CTP = "CREATE CONSTRAINT unique_ip_address IF NOT EXISTS ON (c:Ip4Ctp) ASSERT c.ipAddr IS UNIQUE";
		public static final String UNIQUE_LINK = "";
		public static final String UNIQUE_LINKCONN = "";
		public static final String UNIQUE_IPCONN = "";
		public static final String UNIQUE_BGP = "CREATE CONSTRAINT unique_bgp_peer IF NOT EXISTS ON (b:Bgp) ASSERT (b.rAddr, b.lAsn) IS NODE KEY";
		public static final String UNIQUE_ROUTE = "";
		public static final String UNIQUE_ACLTABLE = "";
		public static final String UNIQUE_ACLRULE = "";
	}
	
	public static class Api {
		public static final String CREATE_BGP = "MATCH (h:Host{name:$deviceName})-[:CONTAINS*3]->(c:Ip4Ctp{ipAddr:$itfAddr})\r\n"
				+ "MERGE (c)-[:HAS_CONFIG]->(b:Bgp)\r\n"
				+ "SET b.lAsn=$localAsn, b.lId=$localId, b.rAddr=$remoteAddr, b.rAsn=$remoteAsn, b.rId=$remoteId, "
				+ "b.holdTime=$holdTime, b.keepAlive=$keepAlive, b.state=$state";
		
		public static final String GET_NETWORK_HOSTS = "MATCH (h:Host) RETURN h.name as name, "
				+ "h.hostname as hostname, h.bgpStatus as bgpStatus, h.bgpAsn as bgpAsn, "
				+ "h.type as type, h.platform as platform, h.mac as mac, h.hwsku as hwsku";
		public static final String GET_HOST = "MATCH (h:Host{name:$deviceName}) RETURN h.name as name, "
				+ "h.hostname as hostname, h.bgpStatus as bgpStatus, h.bgpAsn as bgpAsn, "
				+ "h.type as type, h.platform as platform, h.mac as mac, h.hwsku as hwsku";
		public static final String GET_NETWORK_LINKS = "MATCH (sH:Host)-[:CONTAINS]->(sL:Ltp)-[l:LINKED]->(dL:Ltp)<-[:CONTAINS]-(dH:Host) "
				+ "RETURN sH.name as srcDevice, sL.name as srcInterface, dH.name as destDevice, dL.name as destInterface";
		public static final String GET_NETWORK_SUBNETS = "MATCH (c:Ip4Ctp) WHERE c.netMask <> '/32' "
				+ "RETURN DISTINCT c.netAddr+c.netMask as netAddress, collect(c.ipAddr) as hostAddresses";
		public static final String GET_HOST_INTERFACES = "MATCH (h:Host{name: $deviceName})-[:CONTAINS]->(l:Ltp)-[:CONTAINS]->(e:EtherCtp) \r\n"
				+ "OPTIONAL MATCH (e)-[:CONTAINS]->(c:Ip4Ctp)\r\n"
				+ "RETURN l.adminStatus as adminStatus, l.name as name, l.index as index, l.type as type, l.speed as speed, l.mtu as mtu, "
				+ "e.mode as mode, e.vlan as vlan, e.macAddr as macAddr, c.ipAddr+c.netMask as ipAddr, c.svi as svi";
		public static final String GET_INTERFACE = "MATCH (h:Host{name: $deviceName})-[:CONTAINS]->(l:Ltp{name: $itfName})-[:CONTAINS]->(e:EtherCtp) \r\n"
				+ "OPTIONAL MATCH (e)-[:CONTAINS]->(c:Ip4Ctp)\r\n"
				+ "RETURN l.adminStatus as adminStatus, l.name as name, l.index as index, l.type as type, l.speed as speed, l.mtu as mtu, "
				+ "e.mode as mode, e.vlan as vlan, e.macAddr as macAddr, c.ipAddr+c.netMask as ipAddr, c.svi as svi";
		public static final String GET_HOST_BGPS = "MATCH (h:Host{name: $deviceName})-[:CONTAINS*3]->(c:Ip4Ctp)-[:HAS_CONFIG]->(b:Bgp)\r\n"
				+ "RETURN c.ipAddr as localAddr, b.lAsn as localAsn, b.lId as localId, b.rAddr as remoteAddr, b.rAsn as remoteAsn, b.rId as remoteId, "
				+ "b.holdTime as holdTime, b.keepAlive as keepAlive, b.state as state";
		public static final String GET_BGP = "MATCH (h:Host{name: $deviceName})-[:CONTAINS*3]->(c:Ip4Ctp{ipAddr: $itfAddr})-[:HAS_CONFIG]->(b:Bgp)\r\n"
				+ "RETURN c.ipAddr as localAddr, b.lAsn as localAsn, b.lId as localId, b.rAddr as remoteAddr, b.rAsn as remoteAsn, b.rId as remoteId, "
				+ "b.holdTime as holdTime, b.keepAlive as keepAlive, b.state as state";
		
		public static final String UPDATE_HOST = "MATCH (h:Host{name:$deviceName}) SET h.hostname=$hostname, h.bgpStatus=$bgpStatus, "
				+ "h.bgpAsn=$bgpAsn, h.type=$type, h.platform=$platform, h.mac=$mac, h.hwsku=$hwsku";
		public static final String UPDATE_INTERFACE_IP = "MATCH (h:Host{name:$deviceName})-[:CONTAINS]->(l:Ltp{name:$itfName})-[:CONTAINS]->(e:EtherCtp)\r\n"
				+ "MERGE (e)-[k:CONTAINS]->(c:Ip4Ctp)\r\n"
				+ "SET l.adminStatus=$adminStatus, l.index=$index, l.type=$type, l.speed=$speed, l.mtu=$mtu, "
				+ "e.mode=$mode, e.vlan=$vlan, e.macAddr=$macAddr, "
				+ "c.ipAddr=$ipAddr, c.netMask=$netMask, c.netAddr=$netAddr, c.svi=$svi";
		public static final String UPDATE_INTERFACE_NOIP = "MATCH (h:Host{name:$deviceName})-[:CONTAINS]->(l:Ltp{name:$itfName})-[:CONTAINS]->(e:EtherCtp)\r\n"
				+ "OPTIONAL MATCH (e)-[k:CONTAINS]->(c:Ip4Ctp)\r\n"
				+ "SET l.adminStatus=$adminStatus, l.index=$index, l.type=$type, l.speed=$speed, l.mtu=$mtu, "
				+ "e.mode=$mode, e.vlan=$vlan, e.macAddr=$macAddr "
				+ "DETACH DELETE c";
		public static final String DELETE_BGP = "MATCH (h:Host{name:$deviceName})-[:CONTAINS*3]->(c:Ip4Ctp{ipAddr:$itfAddr})-[:HAS_CONFIG]->(b:Bgp)\r\n"
				+ "DETACH DELETE b";
	}
	
	public static class View {
		public static final String CREATE_VIEW = "CREATE DATABASE $viewId";
		public static final String IMPORT_VIEW_JSON = "CALL apoc.import.json(\"file:///view.json\")";
		public static final String DELETE_VIEW = "DROP DATABASE $viewId";
	
		public static final List<String> INIT_VIEW = Arrays.asList(
		"CREATE CONSTRAINT n10s_unique_uri ON (r:Resource) ASSERT r.uri IS UNIQUE",
				"CALL n10s.graphconfig.init({handleVocabUris: 'MAP'})");
		private static final String P_EXTRACT_VIEW = "CALL n10s.rdf.import.fetch(\"http://localhost:7474/rdf/%s/cypher\",\"Turtle\",\r\n"
				+ "{ \r\n"
				+ "handleVocabUris: \"IGNORE\" , \r\n"
				+ "headerParams: { Authorization: \"Basic \" + apoc.text.base64Encode(\"%s:%s\") }, \r\n"
				+ "payload: '{ \"cypher\": \"MATCH (h:Host)-[:CONTAINS]->(l:Ltp) WITH h limit 1 CALL apoc.path.subgraphAll(h, {maxLevel: 10, labelFilter:\\'+Host|Ltp|EtherCtp|Ip4Ctp|Bgp|Acl|AclRule\\'}) YIELD nodes, relationships RETURN nodes, relationships\" }'\r\n"
				+ "})";
		public static final String EXTRACT_VIEW_JSON = "MATCH (h:Host)-[:CONTAINS]->(l:Ltp) WITH h limit 1\r\n"
				+ "CALL apoc.path.subgraphAll(h, {maxLevel: 10, labelFilter:'+Host|Ltp|EtherCtp|Ip4Ctp|Bgp|Acl|AclRule'}) YIELD nodes, relationships\r\n"
				+ "WITH nodes as a, relationships as b\r\n"
				+ "CALL apoc.export.json.data(a, b, \"view.json\", null)\r\n"
				+ "YIELD file, source, format, nodes, relationships, properties, time, rows, batchSize, batches, done, data\r\n"
				+ "RETURN done";
		public static String getExtractionQuery(String db, String user, String password) {
			return String.format(P_EXTRACT_VIEW, db, user, password);
		}
	}
	
	public static class Internal {
		public static final String DISCONNECT_BGP = "MATCH ()-[b:BGP_PEER]-() DELETE b";
		public static final String RECONNECT_BGP =
				"MATCH (b1:Bgp)<-[:HAS_CONFIG]-(c1:Ip4Ctp)-[:IP_CONN]->(c2:Ip4Ctp)-[:HAS_CONFIG]->(b2:Bgp)\r\n"
				+ "WHERE EXISTS((c2)-[:IP_CONN]->(c1))\r\n"
				+ "AND b1.rAddr=c2.ipAddr AND b2.rAddr=c1.ipAddr AND b1.rAsn=b2.lAsn AND b2.rAsn=b1.lAsn AND b1.rId=b2.lId AND b2.rId=b1.lId\r\n"
				+ "WITH DISTINCT b1,b2\r\n"
				+ "CREATE (b1)-[:BGP_PEER]->(b2);";
	}
	
	public static class Verify {
		public static final String DUPLICATE_HOSTNAME = "MATCH (h:Host) "
				+ "WITH h.hostname as hn, COLLECT(h) AS hns, COUNT(*) AS count WHERE count > 1 "
				+ "RETURN hn as hostname, count";

		public static final String DUPLICATE_MAC = "MATCH (c:EtherCtp) "
				+ "WITH c.macAddr as m, COLLECT(c) AS ctps, COUNT(*) AS count WHERE count > 1 "
				+ "UNWIND ctps AS x "
				+ "MATCH (h:Host)-[:CONTAINS]->(l:Ltp)-[:CONTAINS]->(x) "
				+ "RETURN h.name as deviceName, l.name as itfName, x.macAddr as dupMacAddr";

		public static final String DUPLICATE_IP = "MATCH (c:Ip4Ctp) "
				+ "WITH c.ipAddr AS addr, c.netMask as mask, COLLECT(c) AS ctps, COUNT(*) AS count WHERE count > 1 "
				+ "UNWIND ctps AS x "
				+ "MATCH (h:Host)-[:CONTAINS]->(l:Ltp)-[:CONTAINS]->(e:EtherCtp)-[:CONTAINS]->(x) "
				+ "RETURN h.name as deviceName, l.name as itfName, x.ipAddr as dupIpAddr";

		public static final String DUPLICATE_VLAN = "MATCH (c1:Ip4Ctp) WHERE c1.svi <> '-' "
				+ "WITH c1 "
				+ "MATCH (c2:Ip4Ctp) WHERE c2.svi <> '-' AND c1.vlan = c2.vlan AND c1.netAddr <> c2.netAddr "
				+ "MATCH (n1:Host)-[:CONTAINS]->(l1:Ltp)-[:CONTAINS]-(e1:EtherCtp)-[:CONTAINS]->(c1) "
				+ "MATCH (n2:Host)-[:CONTAINS]->(l2:Ltp)-[:CONTAINS]-(e2:EtherCtp)-[:CONTAINS]->(c2) "
				+ "RETURN DISTINCT "
				+ "n1.name as deviceName1, l1.name as itfName1, c1.netAddr as netAddr1, "
				+ "n2.name as deviceName2, l2.name as itfName2, c2.netAddr as netAddr2, c1.vlan as vlan";

		public static final String BAD_BGP_PEER = "MATCH (b1:Bgp)<-[:HAS_CONFIG]-(c1:Ip4Ctp)-[:IP_CONN]-(c2:Ip4Ctp)-[:HAS_CONFIG]->(b2:Bgp) "
				+ "WHERE NOT EXISTS((b1)-[:BGP_PEER]-(b2)) "
				+ "WITH DISTINCT c1, b1, c2, b2 "
				+ "MATCH (h1:Host)-[:CONTAINS*3]->(c1) "
				+ "MATCH (h2:Host)-[:CONTAINS*3]->(c2) "
				+ "RETURN "
				+ "h1.name as deviceName1, c1.ipAddr as ipAddr1, b1.lAsn as lAsn1, b1.rAsn as rAsn1, b1.rAddr as rAddr1, "
				+ "h2.name as deviceName2, c2.ipAddr as ipAddr2, b2.lAsn as lAsn2, b2.rAsn as rAsn2, b2.rAddr as rAddr2";
		
		public static final String MISCONFIGURED_DEFAULT_ROUTE = "MATCH (h:Host)-[:CONTAINS*3]->(:Ip4Ctp)-[:TO_ROUTE]->(r:Route{to:'0.0.0.0/0'})-[:EGRESS]->(e:Ip4Ctp) "
				+ "WHERE NOT (e)-[:IP_CONN]->(:Ip4Ctp) "
				+ "RETURN DISTINCT h.name, e.ipAddr "
				+ "UNION "
				+ "MATCH (h:Host)-[:CONTAINS*3]-(:Ip4Ctp)-[:TO_ROUTE]->(r:Route{to:'0.0.0.0/0'})-[:EGRESS]->(e:Ip4Ctp)-[:IP_CONN]->(x:Ip4Ctp) "
				+ "WHERE e.netAddr <> x.netAddr "
				+ "RETURN DISTINCT h.name as deviceName, e.ipAddr as misconfGateway";
	}
}
