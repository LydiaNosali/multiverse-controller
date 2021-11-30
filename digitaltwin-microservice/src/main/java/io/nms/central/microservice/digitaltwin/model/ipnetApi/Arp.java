package io.nms.central.microservice.digitaltwin.model.ipnetApi;

import java.util.Objects;

import io.nms.central.microservice.common.functional.JsonUtils;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class Arp {

	private String ipAddr;
    private String macAddr;
    private String netInterface;
    private String vlan;
	
	/*-----------------------------------------------*/

	public Arp() {}
	public Arp(JsonObject json) {
		JsonUtils.fromJson(json, this, Arp.class);
	}

	/*-----------------------------------------------*/

	public String getIpAddr() {
		return ipAddr;
	}
	public void setIpAddr(String ipAddr) {
		this.ipAddr = ipAddr;
	}
	public String getMacAddr() {
		return macAddr;
	}
	public void setMacAddr(String macAddr) {
		this.macAddr = macAddr;
	}
	public String getNetInterface() {
		return netInterface;
	}
	public void setNetInterface(String netInterface) {
		this.netInterface = netInterface;
	}
	public String getVlan() {
		return vlan;
	}
	public void setVlan(String vlan) {
		this.vlan = vlan;
	}
	public JsonObject toJson() {
		return new JsonObject(JsonUtils.pojo2Json(this, false));
	}
	@Override
	public String toString() {
		return JsonUtils.pojo2Json(this, false);
	}
	@Override
	public boolean equals(Object obj) {
		return Objects.equals(toString(), ((Arp) obj).toString());
	}
	@Override
	public int hashCode() {
		return Objects.hash(macAddr+netInterface+ipAddr+vlan);
	}

}
