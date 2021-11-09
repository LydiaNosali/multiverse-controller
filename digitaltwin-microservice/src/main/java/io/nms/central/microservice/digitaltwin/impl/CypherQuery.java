package io.nms.central.microservice.digitaltwin.impl;

public class CypherQuery {
	public static final String CLEAR_DB = "MATCH (n) DETACH DELETE n;";
	
	
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
		public static final String UNIQUE_HOST = "";
		public static final String UNIQUE_LTP = "";
		public static final String UNIQUE_ETHERCTP = "";
		public static final String UNIQUE_IP4CTP = "";
		public static final String UNIQUE_LINK = "";
		public static final String UNIQUE_LINKCONN = "";
		public static final String UNIQUE_IPCONN = "";
		public static final String UNIQUE_BGP = "";
		public static final String UNIQUE_ROUTE = "";
		public static final String UNIQUE_ACLTABLE = "";
		public static final String UNIQUE_ACLRULE = "";
	}
	
	public static class Api {
		public static final String CREATE_VIEW = "";
		public static final String CREATE_BGP = "";
		
		public static final String GET_HOST_INTERFACES = "";
		public static final String GET_HOST_BGPS = "";
		
		public static final String GET_NETWORK_HOSTS = "";
		public static final String GET_NETWORK_LINKS = "";
		public static final String GET_NETWORK_SUBNETS = "MATCH (c:Ip4Ctp) WHERE c.netMask <> '/32' "
				+ "RETURN DISTINCT c.netAddr+c.netMask as netAddress, collect(c.ipAddr) as hostAddresses";
		
		public static final String UPDATE_HOST = "";
		public static final String UPDATE_INTERFACE = "";
		public static final String UPDATE_BGP = "";

		public static final String DELETE_VIEW = "";
		public static final String DELETE_BGP = "";
	}
	
	public static class Verify {
		public static final String DUPLICATE_HOSTNAME = "";
	}
}
