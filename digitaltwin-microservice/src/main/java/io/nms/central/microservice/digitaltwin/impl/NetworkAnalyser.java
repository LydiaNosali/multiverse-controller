package io.nms.central.microservice.digitaltwin.impl;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import io.nms.central.microservice.digitaltwin.model.dt.Report;
import io.nms.central.microservice.digitaltwin.model.dt.ReportMessage;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class NetworkAnalyser {
	
	private static final Logger logger = LoggerFactory.getLogger(NetworkAnalyser.class);
	
	private static final String DUPLICATE_HOSTNAME_CODE = "Multiverse.Verify.DuplicateHostname";
	private static final String DUPLICATE_MAC_CODE = "Multiverse.Verify.DuplicateMacAddress";
	private static final String DUPLICATE_IP_CODE = "Multiverse.Verify.DuplicateIpAddress";
	private static final String DUPLICATE_VLAN_CODE = "Multiverse.Verify.DuplicateVlan";
	private static final String BAG_BGP_PEER_CODE = "Multiverse.Verify.MisconfiguredBgpPeer";
	
	private final Neo4jWrapper neo4j;

	public NetworkAnalyser(Neo4jWrapper neo4j) {
		this.neo4j = neo4j;
	}
	
	public void verifyNetwork(String networkId, Handler<AsyncResult<Report>> resultHandler) {
		Promise<ReportMessage> pDupHost = Promise.promise();
		Promise<ReportMessage> pDupMac = Promise.promise();
		Promise<ReportMessage> pDupIp = Promise.promise();
		Promise<ReportMessage> pDupVlan = Promise.promise();
		Promise<ReportMessage> pBadBgp = Promise.promise();
		
		verifyDupHostname(networkId, pDupHost);
		verifyDupMacAddress(networkId, pDupMac);
		verifyDupIpAddress(networkId, pDupIp);
		verifyDupVlan(networkId, pDupVlan);
		verifyBadBgp(networkId, pBadBgp);
		
		CompositeFuture.all(pDupHost.future(), pDupMac.future(), pDupIp.future(), pDupVlan.future(), pBadBgp.future())
				.onComplete(done -> {
			if (done.succeeded()) {
				Report report = new Report();
				report.setTimestamp(OffsetDateTime.now().toLocalDateTime().toString());
				report.setNetId(networkId);
				
				List<ReportMessage> reportMsgs = new ArrayList<ReportMessage>();
				reportMsgs.add(pDupHost.future().result());
				reportMsgs.add(pDupMac.future().result());
				reportMsgs.add(pDupIp.future().result());
				reportMsgs.add(pDupVlan.future().result());
				reportMsgs.add(pBadBgp.future().result());
				
				report.setReports(reportMsgs);
				resultHandler.handle(Future.succeededFuture(report));
			} else {
				resultHandler.handle(Future.failedFuture("Failed to verify network. " + done.cause().getMessage()));
			}
		});
	}

	public void verifyDupHostname(String networkId, Handler<AsyncResult<ReportMessage>> resultHandler) {
		neo4j.find(networkId, CypherQuery.Verify.DUPLICATE_HOSTNAME, result -> {
			if (result.succeeded()) {
				ReportMessage report = new ReportMessage();
				report.setCode(DUPLICATE_HOSTNAME_CODE);
				List<String> messages = new ArrayList<String>();
				for(JsonObject row: result.result()) {
					// Hostname, count
					String msg = "Hostname <" + row.getString("hostname") + "> is used on " 
							+ row.getInteger("count") + " devices"; 
					messages.add(msg);
				}
				report.setMessages(messages);
				resultHandler.handle(Future.succeededFuture(report));
			} else {
				resultHandler.handle(Future.failedFuture(result.cause()));
			}
		});
	}

	public void verifyDupMacAddress(String networkId, Handler<AsyncResult<ReportMessage>> resultHandler) {
		neo4j.find(networkId, CypherQuery.Verify.DUPLICATE_MAC, result -> {
			if (result.succeeded()) {
				ReportMessage report = new ReportMessage();
				report.setCode(DUPLICATE_MAC_CODE);
				List<String> messages = new ArrayList<String>();
				for(JsonObject row: result.result()) {
					// deviceName, itfName, dupMacAddr
					String msg = "Duplicate MACADDR <" + row.getString("dupMacAddr") + "> on device "
							+ "<" + row.getString("deviceName") + "> " 
							+ "interface <" + row.getString("itfName") + ">"; 
					messages.add(msg);
				}
				report.setMessages(messages);
				resultHandler.handle(Future.succeededFuture(report));
			} else {
				resultHandler.handle(Future.failedFuture(result.cause()));
			}
		});
	}
	
	public void verifyDupIpAddress(String networkId, Handler<AsyncResult<ReportMessage>> resultHandler) {
		neo4j.find(networkId, CypherQuery.Verify.DUPLICATE_IP, result -> {
			if (result.succeeded()) {
				ReportMessage report = new ReportMessage();
				report.setCode(DUPLICATE_IP_CODE);
				List<String> messages = new ArrayList<String>();
				for(JsonObject row: result.result()) {
					// deviceName, itfName, dupIpAddr
					String msg = "Duplicate IPADDR <" + row.getString("dupIpAddr") + "> on device "
							+ "<" + row.getString("deviceName") + "> " 
							+ "interface <" + row.getString("itfName") + ">"; 
					messages.add(msg);
				}
				report.setMessages(messages);
				resultHandler.handle(Future.succeededFuture(report));
			} else {
				resultHandler.handle(Future.failedFuture(result.cause()));
			}
		});
	}
	
	public void verifyDupVlan(String networkId, Handler<AsyncResult<ReportMessage>> resultHandler) {
		neo4j.find(networkId, CypherQuery.Verify.DUPLICATE_VLAN, result -> {
			if (result.succeeded()) {
				ReportMessage report = new ReportMessage();
				report.setCode(DUPLICATE_VLAN_CODE);
				List<String> messages = new ArrayList<String>();
				for(JsonObject row: result.result()) {
					// deviceName1, itfName1, netAddr1,
					// deviceName2, itfName2, netAddr2, vlan
					String msg = "VLAN <" + row.getString("vlan") + "> is associated with "
							+ "subnet <" + row.getString("netAddr1") + "> on device <" + row.getString("deviceName1") + "> "
							+ "interface <" + row.getString("itfName1") + "> "
							+ "and "
							+ "subnet <" + row.getString("netAddr2") + "> on device <" + row.getString("deviceName2") + "> "
							+ "interface <" + row.getString("itfName2") + "> ";
					messages.add(msg);
				}
				report.setMessages(messages);
				resultHandler.handle(Future.succeededFuture(report));
			} else {
				resultHandler.handle(Future.failedFuture(result.cause()));
			}
		});
	}
	
	public void verifyBadBgp(String networkId, Handler<AsyncResult<ReportMessage>> resultHandler) {
		neo4j.find(networkId, CypherQuery.Verify.BAD_BGP_PEER, result -> {
			if (result.succeeded()) {
				ReportMessage report = new ReportMessage();
				report.setCode(BAG_BGP_PEER_CODE);
				List<String> messages = new ArrayList<String>();
				for(JsonObject row: result.result()) {
					// deviceName1, ipAddr1, lAsn1, rAsn1, rAddr1,
					// deviceName2, ipAddr2, lAsn2, rAsn2, rAddr2
					String msg = "BGP peer between "
							+ "device <" + row.getString("deviceName1") + "> on <" + row.getString("ipAddr1") + "> "
							+ String.format("(LocalAsn=%s, RemoteAsn=%s, remoteAddr=%s)",
									row.getString("lAsn1"), row.getString("rAsn1") , row.getString("rAddr1"))
							+ " and "
							+ "device <" + row.getString("deviceName2") + "> on <" + row.getString("ipAddr2") + "> "
							+ String.format("(LocalAsn=%s, RemoteAsn=%s, remoteAddr=%s)",
									row.getString("lAsn2"), row.getString("rAsn2") , row.getString("rAddr2"))
							+ " does not match";
					messages.add(msg);
				}
				report.setMessages(messages);
				resultHandler.handle(Future.succeededFuture(report));
			} else {
				resultHandler.handle(Future.failedFuture(result.cause()));
			}
		});
	}
}
