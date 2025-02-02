package io.nms.central.microservice.digitaltwin.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.nms.central.microservice.common.functional.Functional;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.PathHop;

public class CypherQuery {
	public static final String CLEAR_DB = "MATCH (n) DETACH DELETE n;";
	
	public static class Graph {
		public static final String CREATE_HOST = "CREATE (h:Host:%s {name: '%s', hostname: '%s', type: '%s', mac: '%s', platform: '%s', "
				+ "bgpAsn: '%s', bgpStatus: '%s', hwsku: '%s'});";
		public static final String CREATE_LTP = "MATCH (r:Host) WHERE r.name = '%s' "
				+ "CREATE (r)-[:CONTAINS]->(:Ltp {name: '%s', type: '%s', adminStatus: '%s', "
				+ "index: '%s', speed: '%s', mtu: '%s'});";
		public static final String CREATE_ETHERCTP = "MATCH (h:Host {name:'%s'})-[:CONTAINS]->(l:Ltp{name:'%s'}) "
				+ "CREATE (l)-[:CONTAINS]->(c:EtherCtp {macAddr: '%s', vlan: '%s', mode: '%s'});";
		public static final String CREATE_IP4CTP = "MATCH (r:Host {name:'%s'})-[:CONTAINS]->(:Ltp{name:'%s'})-[:CONTAINS]->(c:EtherCtp) "
				+ "CREATE (c)-[:CONTAINS]->(ipc:Ip4Ctp {ipAddr:'%s', netMask:'%s', netAddr: '%s', svi:'%s', vlan:'%s'});";
		public static final String CREATE_LINK = "MATCH (sR:Host {name:'%s'})-[:CONTAINS]->(src:Ltp{name:'%s'})\r\n"
				+ "MATCH (tR:Host {name:'%s'})-[:CONTAINS]->(dst:Ltp{name:'%s'})\r\n"
				+ "WITH DISTINCT sR,tR,src,dst\r\n"
				+ "CALL apoc.do.case([\r\n"
				+ "(sR.type = 'LeafRouter' AND tR.type = 'LeafRouter'),\r\n"
				+ "\"CREATE (src)-[r:LINKED_VLT {name: '%s'}]->(dst)\",\r\n"
				+ "(sR.type = 'Server' OR tR.type ='Server'),\r\n"
				+ "\"CREATE (src)-[r:LINKED_L2 {name: '%s'}]->(dst)\"],\r\n"
				+ "\"CREATE (src)-[r:LINKED_L3 {name: '%s'}]->(dst)\", {src:src, dst:dst})\r\n"
				+ "YIELD value\r\n"
				+ "RETURN value;";
		public static final String CREATE_LINK_SIMPLE = "MATCH (sR:Host {name:'%s'})-[:CONTAINS]->(src:Ltp{name:'%s'})\r\n"
				+ "MATCH (tR:Host {name:'%s'})-[:CONTAINS]->(dst:Ltp{name:'%s'})\r\n"
				+ "WITH DISTINCT sR,tR,src,dst\r\n"
				+ "CREATE (src)-[r:LINKED {name: '%s'}]->(dst)\r\n";
		public static final String CREATE_IPCONN = "MATCH (r:Host {name:'%s'})-[:CONTAINS]->(sL:Ltp{name:'%s'})-[:CONTAINS*2]->(s:Ip4Ctp) "
				+ "MATCH (dC:EtherCtp{macAddr:'%s'})-[:CONTAINS]->(d:Ip4Ctp{ipAddr:'%s'}) "
				+ "CREATE (s)-[ip:IP_CONN]->(d);";
		public static final String CREATE_BGP = "MATCH(r:Host {name:'%s'})-[:CONTAINS*3]->(c:Ip4Ctp{ipAddr:'%s'})\r\n"
				+ "CREATE (c)-[:HAS_CONFIG]->(b:Bgp {lId: '%s', lAsn: '%s', rAddr:'%s', rId:'%s', rAsn:'%s', "
				+ "state: '%s', holdTime: '%s', keepAlive: '%s'});";
		public static final String CREATE_ACLTABLE = "CREATE (a:Acl{stage: '%s', name: '%s', binding: '%s', type: '%s', description: '%s', rules:[]})\r\n"
				+ "WITH a\r\n"
				+ "MATCH (h:Host {name:'%s'})\r\n"
				+ "CREATE (h)-[:ACL]->(a);";
		public static final String CREATE_ACLRULE = "CREATE (nr:AclRule {name:'%s', priority: %s, action:'%s', matching:'%s'})\r\n"
				+ "WITH nr\r\n"
				+ "MATCH (h:Host {name:'%s'})-[:ACL]->(t:Acl{name:'%s'})\r\n"
				+ "OPTIONAL MATCH (t)-[:NEXT_RULE*]->(r:AclRule) WHERE NOT EXISTS ((r)-[:NEXT_RULE]->())\r\n"
				+ "WITH DISTINCT nr, t, r\r\n"
				+ "CALL apoc.do.case([\r\n"
				+ "  (t IS NOT null AND r IS NOT null AND nr.action = 'ACCEPT'),\r\n"
				+ "  'CREATE (r)-[:NEXT_RULE]->(nr)-[:ACCEPT]->(t)',\r\n"
				+ "  (t IS NOT null AND r IS NOT null AND nr.action = 'DROP'),\r\n"
				+ "  'CREATE (r)-[:NEXT_RULE]->(nr)',\r\n"
				+ "  (t IS NOT null AND r IS null AND nr.action = 'ACCEPT'),\r\n"
				+ "  'CREATE (t)-[:NEXT_RULE]->(nr)-[:ACCEPT]->(t)',\r\n"
				+ "  (t IS NOT null AND r IS null AND nr.action = 'DROP'),\r\n"
				+ "  'CREATE (t)-[:NEXT_RULE]->(nr)'\r\n"
				+ "  ],\r\n"
				+ "  'DELETE nr',{nr:nr, t:t, r:r})\r\n"
				+ "YIELD value\r\n"
				+ "RETURN value;";
		public static final String CREATE_ROUTE_ITF = "CREATE(r:Route {to: '%s', via: '%s', type: '%s'}) \r\n"
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
		public static final String CREATE_ROUTE_SVI = "CREATE(r:Route {to: '%s', via: '%s', type: '%s'}) \r\n"
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

		public static final String AUTO_LINKCONN = "MATCH (sC:EtherCtp)<-[:CONTAINS]-(sL:Ltp)-[:LINKED_L2|:LINKED_L3|:LINKED_VLT]->(dL:Ltp)-[:CONTAINS]->(dC:EtherCtp) "
				+ "WHERE NOT (sC)-[:LINK_CONN]-() AND NOT (dC)-[:LINK_CONN]-() "
				+ "AND NOT (sL)-[:CONTAINS]-(:Switch) AND NOT (dL)-[:CONTAINS]-(:Switch) "
				+ "CREATE (sC)-[r:LINK_CONN]->(dC);";
		public static final String AUTO_LINKCONN_SIMPLE = "MATCH (sC:EtherCtp)<-[:CONTAINS]-(sL:Ltp)-[:LINKED]->(dL:Ltp)-[:CONTAINS]->(dC:EtherCtp) "
				+ "WHERE NOT (sC)-[:LINK_CONN]-() AND NOT (dC)-[:LINK_CONN]-() "
				+ "AND NOT (sL)-[:CONTAINS]-(:Switch) AND NOT (dL)-[:CONTAINS]-(:Switch) "
				+ "CREATE (sC)-[r:LINK_CONN]->(dC);";
		public static final String AUTO_VLAN_MEMBER = "MATCH (h:Host)\r\n"
				+ "MATCH (h)-[:CONTAINS*3]->(svi:Ip4Ctp) WHERE svi.svi <> '-' \r\n"
				+ "MATCH (h)-[:CONTAINS]->(l:Ltp)-[:CONTAINS]->(e:EtherCtp) WHERE e.vlan = svi.vlan AND NOT (e)-[:CONTAINS]->(:Ip4Ctp)\r\n"
				+ "CREATE (e)-[b:VLAN_MEMBER]->(svi)\r\n"
				+ "RETURN h, svi, b, e;";
		public static final String AUTO_BGP_NEIGHBORS = "MATCH (b1:Bgp)<-[:HAS_CONFIG]-(c1:Ip4Ctp)-[:IP_CONN]->(c2:Ip4Ctp)-[:HAS_CONFIG]->(b2:Bgp)\r\n"
				+ "WHERE EXISTS((c2)-[:IP_CONN]->(c1))\r\n"
				+ "AND b1.rAddr=c2.ipAddr AND b2.rAddr=c1.ipAddr AND b1.rAsn=b2.lAsn AND b2.rAsn=b1.lAsn AND b1.rId=b2.lId AND b2.rId=b1.lId\r\n"
				+ "WITH DISTINCT b1,b2\r\n"
				+ "CREATE (b1)-[:BGP_PEER]->(b2);";
	}
	
	public static class Constraints {
		public static final String UNIQUE_HOST = "CREATE CONSTRAINT unique_host IF NOT EXISTS ON (h:Host) ASSERT h.name IS UNIQUE";
		public static final String UNIQUE_HOSTNAME = "CREATE CONSTRAINT unique_hostname IF NOT EXISTS ON (h:Host) ASSERT h.hostname IS UNIQUE";
		public static final String UNIQUE_LTP = "";
		public static final String UNIQUE_ETHERCTP = "";
		public static final String UNIQUE_IP4CTP = "CREATE CONSTRAINT unique_ip_address IF NOT EXISTS ON (c:Ip4Ctp) ASSERT c.ipAddr IS UNIQUE";
		public static final String UNIQUE_LINKCONN = "";
		public static final String UNIQUE_IPCONN = "";
		public static final String UNIQUE_BGP = "CREATE CONSTRAINT unique_bgp_peer IF NOT EXISTS ON (b:Bgp) ASSERT (b.rAddr, b.lAsn) IS NODE KEY";
		public static final String UNIQUE_ROUTE = "";
		public static final String UNIQUE_ACLTABLE = "";
		public static final String UNIQUE_ACLRULE = "";
	}
	
	public static class Api {
		public static final String UPSERT_HOST = "MERGE (h:Host:%s {name:$deviceName}) SET h.hostname=$hostname, h.bgpStatus=$bgpStatus, "
				+ "h.bgpAsn=$bgpAsn, h.type=$type, h.platform=$platform, h.mac=$mac, h.hwsku=$hwsku";
		public static final String CREATE_INTERFACE_IP = "MATCH (h:Host{name:$deviceName})\r\n"
				+ "WHERE NOT (h)-[:CONTAINS]->(:Ltp {name: $itfName})\r\n"
				+ "CREATE (h)-[:CONTAINS]->(l:Ltp {name: $itfName})-[:CONTAINS]->(e:EtherCtp)-[:CONTAINS]->(c:Ip4Ctp)\r\n"
				+ "SET l.adminStatus=$adminStatus, l.index=$index, l.type=$type, l.speed=$speed, l.mtu=$mtu, "
				+ "e.mode=$mode, e.vlan=$vlan, e.macAddr=$macAddr, "
				+ "c.ipAddr=$ipAddr, c.netMask=$netMask, c.netAddr=$netAddr, c.svi=$svi";
		public static final String CREATE_INTERFACE_NOIP = "MATCH (h:Host{name:$deviceName})\r\n"
				+ "WHERE NOT (h)-[:CONTAINS]->(:Ltp {name: $itfName})\r\n"
				+ "CREATE (h)-[:CONTAINS]->(l:Ltp {name: $itfName})-[:CONTAINS]->(e:EtherCtp)\r\n"
				+ "SET l.adminStatus=$adminStatus, l.index=$index, l.type=$type, l.speed=$speed, l.mtu=$mtu, "
				+ "e.mode=$mode, e.vlan=$vlan, e.macAddr=$macAddr ";
		public static final String CREATE_LINK = "MATCH (sH:Host {name: $srcDevice})-[:CONTAINS]->(src:Ltp{name: $srcInterface})\r\n"
				+ "WHERE NOT (src)-[:LINKED_L2|LINKED_L3|LINKED_VLT]-()\r\n"
				+ "MATCH (dH:Host {name: $destDevice})-[:CONTAINS]->(dst:Ltp{name: $destInterface})\r\n"
				+ "WHERE NOT (dst)-[:LINKED_L2|LINKED_L3|LINKED_VLT]-()\r\n"
				+ "WITH DISTINCT sH,dH,src,dst\r\n"
				+ "CALL apoc.do.case([\r\n"
				+ "(sH.type = 'LeafRouter' AND dH.type = 'LeafRouter'),\r\n"
				+ "\"CREATE (src)-[r:LINKED_VLT]->(dst) RETURN r\",\r\n"
				+ "(sH.type = 'Server' OR dH.type ='Server'),\r\n"
				+ "\"CREATE (src)-[r:LINKED_L2]->(dst) RETURN r\"],\r\n"
				+ "\"CREATE (src)-[r:LINKED_L3]->(dst) RETURN r\", {src:src, dst:dst})\r\n"
				+ "YIELD value\r\n"
				+ "WITH value.r as link SET link.name = $srcDevice + '.' + $srcInterface + '-' + $destDevice + '.' + $destInterface";
		public static final String CREATE_LINK_SIMPLE = "MATCH (sH:Host {name: $srcDevice})-[:CONTAINS]->(src:Ltp{name: $srcInterface})\r\n"
				+ "WHERE NOT (src)-[:LINKED]-()\r\n"
				+ "MATCH (dH:Host {name: $destDevice})-[:CONTAINS]->(dst:Ltp{name: $destInterface})\r\n"
				+ "WHERE NOT (dst)-[:LINKED]-()\r\n"
				+ "WITH DISTINCT sH,dH,src,dst\r\n"
				+ "CREATE (src)-[r:LINKED]->(dst)\r\n"
				+ "WITH r as link SET link.name = $srcDevice + '.' + $srcInterface + '-' + $destDevice + '.' + $destInterface";
		public static final String UPSERT_BGP = "MATCH (h:Host{name:$deviceName})-[:CONTAINS*3]->(c:Ip4Ctp{ipAddr:$itfAddr})\r\n"
				+ "MERGE (c)-[:HAS_CONFIG]->(b:Bgp)\r\n"
				+ "SET b.lAsn=$localAsn, b.lId=$localId, b.rAddr=$remoteAddr, b.rAsn=$remoteAsn, b.rId=$remoteId, "
				+ "b.holdTime=$holdTime, b.keepAlive=$keepAlive, b.state=$state";
		public static final String CREATE_VLAN = "";
		public static final String ADD_VLAN_MEMBER = "";

		public static final String GET_NETWORK_HOSTS = "MATCH (h:Host) RETURN h.name as name, "
				+ "h.hostname as hostname, h.bgpStatus as bgpStatus, h.bgpAsn as bgpAsn, "
				+ "h.type as type, h.platform as platform, h.mac as mac, h.hwsku as hwsku";
		public static final String GET_HOST = "MATCH (h:Host{name:$deviceName}) RETURN h.name as name, "
				+ "h.hostname as hostname, h.bgpStatus as bgpStatus, h.bgpAsn as bgpAsn, "
				+ "h.type as type, h.platform as platform, h.mac as mac, h.hwsku as hwsku";
		public static final String GET_NETWORK_LINKS = "MATCH (sH:Host)-[:CONTAINS]->(sL:Ltp)-[l:LINKED]->(dL:Ltp)<-[:CONTAINS]-(dH:Host) "
				+ "RETURN l.name as name, sH.name as srcDevice, sL.name as srcInterface, dH.name as destDevice, dL.name as destInterface";
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
		public static final String GET_HOST_VLANS = "";
		public static final String GET_VLAN_MEMBERS = "";
		public static final String GET_BGP = "MATCH (h:Host{name: $deviceName})-[:CONTAINS*3]->(c:Ip4Ctp{ipAddr: $itfAddr})-[:HAS_CONFIG]->(b:Bgp)\r\n"
				+ "RETURN c.ipAddr as localAddr, b.lAsn as localAsn, b.lId as localId, b.rAddr as remoteAddr, b.rAsn as remoteAsn, b.rId as remoteId, "
				+ "b.holdTime as holdTime, b.keepAlive as keepAlive, b.state as state";
		public static final String GET_HOST_ACLTABLES = 
				"MATCH (h:Host{name:$deviceName})-[:ACL]->(a:Acl)-[:NEXT_RULE*]->(r:AclRule)\r\n"
				+ "WITH a{.*,rules:collect(r{.*})} as acl\r\n"
				+ "RETURN acl.name as name, acl.binding as binding, acl.description as description, acl.stage as stage, "
				+ "acl.type as type, acl.rules as rules";
		public static final String GET_HOST_IPROUTES = 
				"MATCH (h:Host{name:$deviceName})-[:TO_ROUTE|ACL*..2]->(r:Route)-[:EGRESS]->(:Ip4Ctp)<-[:CONTAINS*2]-(i:Ltp)\r\n"
				+ "RETURN DISTINCT i.name as netInterface, r.to as to, r.via as via, r.type as type";
		public static final String GET_HOST_IPROUTES_TO = 
				"MATCH (h:Host{name:$deviceName})-[:TO_ROUTE|ACL*..2]->(r:Route{to:$to})-[:EGRESS]->(:Ip4Ctp)<-[:CONTAINS*2]-(i:Ltp)\r\n"
				+ "RETURN DISTINCT i.name as netInterface, r.to as to, r.via as via, r.type as type";
		public static final String GET_HOST_ARPS = "MATCH (h:Host{name:$deviceName})-[:CONTAINS]->(l:Ltp)-[:CONTAINS*2]->(lc:Ip4Ctp)-[:IP_CONN]->(rc:Ip4Ctp)<-[:CONTAINS]-(re:EtherCtp) "
				+ "RETURN l.name as netInterface, rc.ipAddr as ipAddr, re.macAddr as macAddr, lc.vlan as vlan";

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

		public static final String DELETE_HOST = "MATCH (h:Host{name:$deviceName})\r\n"
				+ "OPTIONAL MATCH (h)-[:ACL]->(a:Acl)-[:NEXT_RULE*]->(r:AclRule)\r\n"
				+ "DETACH DELETE a,r \r\n"
				+ "WITH h \r\n"
				+ "CALL apoc.path.subgraphAll(h, \r\n"
				+ "{maxLevel: 4, \r\n"
				+ "labelFilter:'+Ltp|EtherCtp|Ip4Ctp|Bgp|Route',\r\n"
				+ "relationshipFilter:'CONTAINS|HAS_CONFIG|EGRESS|TO_ROUTE'\r\n"
				+ "}) YIELD nodes, relationships\r\n"
				+ "UNWIND nodes as x\r\n"
				+ "DETACH DELETE x";
		public static final String DELETE_INTERFACE = "MATCH (h:Host{name:$deviceName})-[:CONTAINS]->(l:Ltp{name:$itfName})-[:CONTAINS]->(e:EtherCtp)\r\n"
				+ "OPTIONAL MATCH (e)-[:CONTAINS]->(c:Ip4Ctp)-[:HAS_CONFIG]->(b:Bgp)\r\n"
				+ "DETACH DELETE l, e, c, b";
		public static final String DELETE_LINK = "MATCH ()-[l:LINKED_VLT|LINKED_L2|LINKED_L3]-() \r\n"
				+ "WHERE l.name = $linkName\r\n"
				+ "DELETE l";
		public static final String DELETE_BGP = "MATCH (h:Host{name:$deviceName})-[:CONTAINS*3]->(c:Ip4Ctp{ipAddr:$itfAddr})-[:HAS_CONFIG]->(b:Bgp)\r\n"
				+ "DETACH DELETE b";
		public static final String DELETE_VLAN = "";
		public static final String REMOVE_VLAN_MEMBER = "";
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
	
	public static class PathSearch {
		public static final String HOST_TO_HOST = 
				"MATCH (from:Sever{hostname:$from})\r\n"
				+ "	MATCH (to:Sever{hostname:$to})\r\n"
			//	+ "	WHERE from.type = 'Server' AND to.type = 'Server'\r\n"
				+ "	WITH from, to\r\n"
				+ "	CALL apoc.path.expandConfig(from, {\r\n"
				+ "    		sequence: \"Host,CONTAINS>,Ltp,LINKED_L2|LINKED_L3,Ltp,<CONTAINS\",\r\n"
				+ "    		terminatorNodes: [to],\r\n"
				+ "    		maxLevel: 12\r\n"
				+ "	})\r\n"
				+ "	YIELD path\r\n"
				+ "WITH path WHERE NONE(n in nodes(path) WHERE (n:Server AND n.name<>from.name AND n.name<>to.name))\r\n"
				+ "WITH path, [n IN nodes(path) WHERE n:Ltp] as ltps, [n IN nodes(path) WHERE n:Host] as hosts\r\n"
				+ "UNWIND hosts as host\r\n"
				+ "UNWIND ltps as ltp\r\n"
				+ "MATCH (host)-[:CONTAINS]->(ltp)-[:CONTAINS]->(mac:EtherCtp)-[:VLAN_MEMBER|:CONTAINS]->(ip:Ip4Ctp)\r\n"
				+ "WITH hosts, ltps, collect({host:host.name, itf:ltp.name, type:ltp.type, adminStatus:ltp.adminStatus, "
				+ "index:ltp.index, speed:ltp.speed, mtu:ltp.mtu, macAddr:mac.macAddr, vlan:mac.vlan, mode:mac.mode, "
				+ "ipAddr:ip.ipAddr+ip.netMask, svi:ip.svi}) as hops\r\n"
				+ "RETURN hops as path\r\n";
		public static final String IP_TO_IP = 
				"MATCH (from:Server)-[:CONTAINS]->(fromItf:Ltp)-[:CONTAINS*2]->(fromIp:Ip4Ctp{ipAddr:$from})\r\n"
				+ "	MATCH (to:Server)-[:CONTAINS]->(toItf:Ltp)-[:CONTAINS*2]->(toIp:Ip4Ctp{ipAddr:$to})\r\n"
		//		+ "	WHERE from.type = 'Server' AND to.type = 'Server'\r\n"
				+ "	WITH from, to, fromItf, toItf\r\n"
				+ "	CALL apoc.path.expandConfig(fromItf, {\r\n"
				+ "    	sequence: \"Ltp,LINKED_L2|LINKED_L3,Ltp,<CONTAINS,Host,CONTAINS>\",\r\n"
				+ "    	terminatorNodes: [toItf],\r\n"
				+ "    	maxLevel: 10\r\n"
				+ "	})\r\n"
				+ "	YIELD path\r\n"
				+ "WITH path WHERE NONE(n in nodes(path) WHERE (n:Server AND n.name<>from.name AND n.name<>to.name))\r\n"
				+ "WITH path, [n IN nodes(path) WHERE n:Ltp] as ltps, from + [n IN nodes(path) WHERE n:Host] + to as hosts\r\n"
				+ "UNWIND hosts as host\r\n"
				+ "UNWIND ltps as ltp\r\n"
				+ "MATCH (host)-[:CONTAINS]->(ltp)-[:CONTAINS]->(mac:EtherCtp)-[:VLAN_MEMBER|:CONTAINS]->(ip:Ip4Ctp)\r\n"
				+ "WITH hosts, ltps, collect({host:host.name, itf:ltp.name, type:ltp.type, adminStatus:ltp.adminStatus, index:ltp.index, speed:ltp.speed, mtu:ltp.mtu, macAddr:mac.macAddr, vlan:mac.vlan, mode:mac.mode, ipAddr:ip.ipAddr+ip.netMask, svi:ip.svi}) as hops\r\n"
				+ "RETURN hops as path";
		private static final String P_IPPATH_FIRST_HOP =
				"MATCH (h%d:Host{name:'%s'})-[:CONTAINS]->(l%d:Ltp)-[:CONTAINS*2]->(c%d:Ip4Ctp{ipAddr:'%s'})\r\n"
				+ "OPTIONAL MATCH (h%d)-[:TO_ROUTE|ACL*1..2]->(r:Route)-[:EGRESS]->(c%d)\r\n"
				+ "WHERE r.to = '%s'\r\n"
				+ "OPTIONAL MATCH (h%d)-[:TO_ROUTE|ACL*1..2]->(dr:Route)-[:EGRESS]->(c%d)\r\n"
				+ "WHERE dr.to = '0.0.0.0/0'\r\n"
				+ "CALL apoc.when(r IS NOT NULL, 'RETURN $r as route', 'RETURN $dr as route',{r:r,dr:dr})\r\n"
				+ "YIELD value\r\n"
				+ "WITH h%d, l%d, c%d, value.route as r%d\r\n"
				+ "OPTIONAL MATCH (c%d)-[arp%d:IP_CONN]->(:Ip4Ctp{ipAddr:'%s'})<-[:CONTAINS*3]-(:Host{name:'%s'})\r\n"
				+ "OPTIONAL MATCH (h%d)-[:ACL]->(a%d:Acl)-[:TO_ROUTE]->(r%d)\r\n"
				+ "OPTIONAL MATCH (a%d)-[:NEXT_RULE*]->(rules%d:AclRule)\r\n"
				+ "WITH h%d, l%d, c%d, r%d, arp%d, a%d{.*,rules:collect(rules%d{.*})} as acl\r\n"
				+ "WITH collect({host:h%d{.*}, route:r%d{.*, netInterface:l%d.name}, acl:acl, arp:arp%d IS NOT NULL}) AS hop%d";		
		private static final String P_IPPATH_ONE_HOP = 
				"\nMATCH (h%d:Host{name:'%s'})-[:CONTAINS]->(l%d:Ltp)-[:CONTAINS*2]->(c%d:Ip4Ctp{ipAddr:'%s'})\r\n"
				+ "OPTIONAL MATCH (h%d)-[:TO_ROUTE|ACL*1..2]->(r:Route)-[:EGRESS]->(c%d)\r\n"
				+ "WHERE r.to = '%s'\r\n"
				+ "OPTIONAL MATCH (h%d)-[:TO_ROUTE|ACL*1..2]->(dr:Route)-[:EGRESS]->(c%d)\r\n"
				+ "WHERE dr.to = '0.0.0.0/0'\r\n"
				+ "CALL apoc.when(r IS NOT NULL, 'RETURN $r as route', 'RETURN $dr as route',{r:r,dr:dr})\r\n"
				+ "YIELD value\r\n"
				+ "WITH hop%d, h%d, l%d, c%d, value.route as r%d\r\n"
				+ "OPTIONAL MATCH (c%d)-[arp%d:IP_CONN]->(:Ip4Ctp{ipAddr:'%s'})<-[:CONTAINS*3]-(:Host{name:'%s'})\r\n"
				+ "OPTIONAL MATCH (h%d)-[:ACL]->(a%d:Acl)-[:TO_ROUTE]->(r%d)\r\n"
				+ "OPTIONAL MATCH (a%d)-[:NEXT_RULE*]->(rules%d:AclRule)\r\n"
				+ "WITH hop%d, h%d, l%d, c%d, r%d, arp%d, a%d{.*,rules:collect(rules%d{.*})} as acl\r\n"
				+ "WITH hop%d + collect({host:h%d{.*}, route:r%d{.*, netInterface:l%d.name}, acl:acl, arp:arp%d IS NOT NULL}) AS hop%d";
		private static final String P_IPPATH_RETURN = 
				"\nUNWIND hop%d as route RETURN route";
		public static String getFindIpPathQuery(List<PathHop> path) {
			PathHop dest = path.get(path.size()-1);
			String destSubnetAddr = Functional.parseSubnetAddress(dest.getIpAddr())+"/"+dest.getIpAddr().split("/")[1];
			int i=0;
			String query = String.format(P_IPPATH_FIRST_HOP, 
					i, path.get(i).getHost(),i,i,path.get(i).getIpAddr().split("/")[0],
					i,i,
					destSubnetAddr,
					i,i,
					i,i,i,i,
					i,i, path.get(i+1).getIpAddr().split("/")[0], path.get(i+1).getHost(),
					i,i,i,
					i,i,
					i,i,i,i,i,i,i,
					i,i,i,i,i);
			for (i=2; i<=path.size()-2; i+=2) {
				query+=String.format(P_IPPATH_ONE_HOP, 
						i, path.get(i).getHost(),i,i,path.get(i).getIpAddr().split("/")[0],
						i,i,
						destSubnetAddr,
						i,i,
						i-2,i,i,i,i,
						i,i, path.get(i+1).getIpAddr().split("/")[0], path.get(i+1).getHost(),
						i,i,i,
						i,i,
						i-2,i,i,i,i,i,i,i,
						i-2,i,i,i,i,i);
			}
			query+=String.format(P_IPPATH_RETURN, path.size()-2);
			return query;
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
		public static final String CHECK_HOST = "MATCH (h:Host{name:$deviceName}) RETURN h.name";
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
		public static final String MISCONFIGURED_DEFAULT_ROUTE = 
				"MATCH (h:Host)-[:TO_ROUTE|ACL*1..2]->(r:Route{to:'0.0.0.0/0'})-[:EGRESS]->(e:Ip4Ctp)\r\n"
				+ "WHERE NOT (e)-[:IP_CONN]->(:Ip4Ctp)\r\n"
				+ "RETURN DISTINCT h.name as deviceName, e.ipAddr as misconfGateway\r\n"
				+ "UNION \r\n"
				+ "MATCH (h:Host)-[:TO_ROUTE|ACL*1..2]->(r:Route{to:'0.0.0.0/0'})-[:EGRESS]->(e:Ip4Ctp)-[:IP_CONN]->(x:Ip4Ctp) WHERE e.netAddr <> x.netAddr\r\n"
				+ "RETURN DISTINCT h.name as deviceName, e.ipAddr as misconfGateway";
	}
	
	public static class ConfigGen {
		public static final String GET_DEVICES = "MATCH (h) WHERE h:SpineRouter OR h:LeafRouter OR h:BorderRouter OR h:Firewall "
				+ "RETURN h.name as device";
		public static final String GET_METADATA = "MATCH (h:Host{name:'%s'}) RETURN h.hostname as hostname, h.type as type, h.platform as platform, "
				+ "h.hwsku as hwsku, h.bgpAsn as bgpAsn, h.bgpStatus as bgpStatus, h.mac as mac";
		public static final String GET_INTERFACES = "MATCH ({name:'%s'})-[:CONTAINS]->(l:Ltp)\r\n"
				+ "WHERE NOT l.name STARTS WITH 'Bridge' AND NOT l.name STARTS WITH 'Loopback' AND NOT l.name STARTS WITH 'eth'\r\n"
				+ "OPTIONAL MATCH (l)-[:CONTAINS*2]->(i:Ip4Ctp) \r\n"
				+ "RETURN l.name as interface, i.ipAddr+i.netMask as ipAddress";
		public static final String GET_LOOPBACK_ITFS = "MATCH ({name:'%s'})-[:CONTAINS]->(l:Ltp)-[:CONTAINS*2]->(i:Ip4Ctp) \r\n"
				+ "WHERE l.name STARTS WITH 'Loopback'\r\n"
				+ "RETURN l.name as interface, i.ipAddr+i.netMask as ipAddress";
		public static final String GET_PORTS = "MATCH (h:Host{name:'%s'})-[:CONTAINS]->(l:Ltp) \r\n"
				+ "WHERE NOT l.name STARTS WITH 'Loopback' AND NOT l.name STARTS WITH 'Bridge' \r\n"
				+ "AND NOT l.name STARTS WITH 'eth'\r\n"	// temporary in emulation network
				+ "RETURN l.name as name, l.adminStatus as adminStatus, l.index as index, l.mtu as mtu, l.speed as speed";
		public static final String GET_VLANS = "MATCH (h {name:'%s'})-[:CONTAINS*3]->(v:Ip4Ctp) WHERE v.svi <> '-'\r\n"
				+ "OPTIONAL MATCH (h)-[:CONTAINS]->(l:Ltp)-[:CONTAINS]->(m:EtherCtp) WHERE m.vlan = v.vlan\r\n"
				+ "RETURN v.svi as name, v.vlan as vlanid, v.ipAddr+v.netMask as vlanaddr, collect(l.name) as members";
		public static final String GET_VLAN_MEMBERS = "MATCH (h {name:'%s'})-[:CONTAINS*3]->(v:Ip4Ctp) WHERE v.svi <> '-'\r\n"
				+ "OPTIONAL MATCH (h)-[:CONTAINS]->(l:Ltp)-[:CONTAINS]->(m:EtherCtp) WHERE m.vlan = v.vlan\r\n"
				+ "RETURN l.name as member, m.mode as mode, v.svi as name";
		public static final String GET_BGP_NEIGHBORS = "MATCH (lh {name:'%s'})-[:CONTAINS*3]->(lc:Ip4Ctp)-[:HAS_CONFIG]->(lb:Bgp)-[:BGP_PEER]->(rb:Bgp)<-[:HAS_CONFIG]-(rc:Ip4Ctp)<-[:CONTAINS*3]-(rh)\r\n"
				+ "RETURN rc.ipAddr as remote_addr, lc.ipAddr as local_addr, rb.lAsn as asn, rh.name as name";
		public static final String GET_ACLTABLES = "MATCH ({name:'%s'})-[:ACL]->(acl:Acl)\r\n"
				+ "RETURN DISTINCT acl.name as name, acl.binding as services, acl.stage as stage, acl.type as type, acl.description as description";
		public static final String GET_ACLRULES = "MATCH ({name:'%s'})-[:ACL]->(t:Acl)\r\n"
				+ "OPTIONAL MATCH (r:AclRule)<-[:NEXT_RULE*]-(t)\r\n"
				+ "RETURN DISTINCT r.name as name, r.action as action, r.priority as priority, r.matching as matching, t.name as table";
		
		public static final Map<String,String> queryMap(String device) {
			Map<String,String> map = new HashMap<String,String>();
			map.put("metadata", String.format(GET_METADATA, device));
			map.put("interface", String.format(GET_INTERFACES, device));
			map.put("loopbackInterface", String.format(GET_LOOPBACK_ITFS, device));
			map.put("port", String.format(GET_PORTS, device));
			map.put("vlan", String.format(GET_VLANS, device));
			map.put("vlanMember", String.format(GET_VLAN_MEMBERS, device));
			map.put("bgpNeighbor", String.format(GET_BGP_NEIGHBORS, device));
			map.put("aclTable", String.format(GET_ACLTABLES, device));
			map.put("aclRule", String.format(GET_ACLRULES, device));
			return map;
		}
	}
}