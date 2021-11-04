package io.nms.central.microservice.digitaltwin.model.ipnetApi;

import java.util.Objects;

import io.nms.central.microservice.common.functional.JSONUtils;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class NetInterface extends Configurable {
	
	public enum InterfaceType {
		BRIDGE("Bridge"),
		VLAN("Vlan"),
		OTHER("Other");
		private String value;
		private InterfaceType(String value) { this.value = value; }
		public String getValue() { return this.value; }
	};
	
	public enum InterfaceStatus {
		UP("UP"),
		DOWN("DOWN");
		private String value;
		private InterfaceStatus(String value) { this.value = value; }
		public String getValue() { return this.value; }
	};

	private String name;
	private InterfaceStatus adminStatus;
	private String index;
	private String mtu;
	private String speed;
	private String macAddr;
	private String vlan;
	public String getVlan() {
		return vlan;
	}
	public void setVlan(String vlan) {
		this.vlan = vlan;
	}
	public boolean isSvi() {
		return isSvi;
	}
	public void setSvi(boolean isSvi) {
		this.isSvi = isSvi;
	}
	private String ipAddr;
	private InterfaceType type;
	private boolean isSvi = false;
	
	/*-----------------------------------------------*/

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public InterfaceStatus getAdminStatus() {
		return adminStatus;
	}
	public void setAdminStatus(InterfaceStatus adminStatus) {
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
	public InterfaceType getType() {
		return type;
	}
	public void setType(InterfaceType type) {
		this.type = type;
	}
	public NetInterface() {}
	public NetInterface(JsonObject json) {
		JSONUtils.fromJson(json, this, NetInterface.class);
	}

	/*-----------------------------------------------*/

	public JsonObject toJson() {
		return new JsonObject(JSONUtils.pojo2Json(this, false));
	}
	@Override
	public String toString() {
		return JSONUtils.pojo2Json(this, false);
	}
	@Override
	public boolean equals(Object obj) {
		return Objects.equals(toString(), ((NetInterface) obj).toString());
	}
	@Override
	public int hashCode() {
		return Objects.hash(name+macAddr);
	}
}

