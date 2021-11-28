package io.nms.central.microservice.digitaltwin.model.graph;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.nms.central.microservice.common.functional.JsonUtils;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class Interface {
	
	public enum InterfaceTypeEnum {
		Bridge("Bridge"),
		Vlan("Vlan"),
		Other("Other");
		private String value;
		private InterfaceTypeEnum(String value) { this.value = value; }
		public String getValue() { return this.value; }
	};
	
	public enum InterfaceStatusEnum {
		up("up"),
		down("down");
		private String value;
		private InterfaceStatusEnum(String value) { this.value = value; }
		public String getValue() { return this.value; }
	};

	@JsonProperty("NAME")
	private String name;
	
	@JsonProperty("ADMIN_STATUS")
	private InterfaceStatusEnum adminStatus;
	
	@JsonProperty("INDEX")
	private String index;
	
	@JsonProperty("MTU")
	private String mtu;
	
	@JsonProperty("SPEED")
	private String speed;
	
	@JsonProperty("MACADDR")
	private String macAddr;
	
	@JsonProperty("IPADDR")
	private String ipAddr;
	
	@JsonProperty("TYPE")
	private InterfaceTypeEnum type;
	
	/*-----------------------------------------------*/

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public InterfaceStatusEnum getAdminStatus() {
		return adminStatus;
	}
	public void setAdminStatus(InterfaceStatusEnum adminStatus) {
		this.adminStatus = adminStatus;
	}
	public String getIndex() {
		return index;
	}
	public void setIndex(String index) {
		this.index = index;
	}
	public String getMtu() {
		return mtu;
	}
	public void setMtu(String mtu) {
		this.mtu = mtu;
	}
	public String getSpeed() {
		return speed;
	}
	public void setSpeed(String speed) {
		this.speed = speed;
	}
	public String getMacAddr() {
		return macAddr;
	}
	public void setMacAddr(String macAddr) {
		this.macAddr = macAddr;
	}
	public String getIpAddr() {
		return ipAddr;
	}
	public void setIpAddr(String ipAddr) {
		this.ipAddr = ipAddr;
	}
	public InterfaceTypeEnum getType() {
		return type;
	}
	public void setType(InterfaceTypeEnum type) {
		this.type = type;
	}
	public Interface() {}
	public Interface(JsonObject json) {
		JsonUtils.fromJson(json, this, Interface.class);
	}

	/*-----------------------------------------------*/

	public JsonObject toJson() {
		return new JsonObject(JsonUtils.pojo2Json(this, false));
	}
	@Override
	public String toString() {
		return JsonUtils.pojo2Json(this, false);
	}
	@Override
	public boolean equals(Object obj) {
		return Objects.equals(toString(), ((Interface) obj).toString());
	}
	@Override
	public int hashCode() {
		return Objects.hash(name+macAddr);
	}

	/*-----------------------------------------------*/


}

