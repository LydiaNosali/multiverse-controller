package io.nms.central.microservice.digitaltwin.model.ipnetApi;

import java.util.Objects;
import io.nms.central.microservice.common.functional.JsonUtils;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.NetInterface.InterfaceStatus;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.NetInterface.InterfaceType;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class PathHop {

	private String host;
	private String itf;
	private InterfaceType type;
	private InterfaceStatus adminStatus;
	private String index;
	private String speed;
	private String mtu;
	private String macAddr;
	private String vlan;
	private String mode;
	private String ipAddr; 		// CIDR
	private String svi;

	public PathHop() {}
	public PathHop(JsonObject json) {
		JsonUtils.fromJson(json, this, PathHop.class);
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
		return Objects.equals(toString(), ((PathHop) obj).toString());
	}
	@Override
	public int hashCode() {
		return Objects.hash(host+itf);
	}
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public String getItf() {
		return itf;
	}
	public void setItf(String itf) {
		this.itf = itf;
	}
	public InterfaceType getType() {
		return type;
	}
	public void setType(InterfaceType type) {
		this.type = type;
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
	public String getSpeed() {
		return speed;
	}
	public void setSpeed(String speed) {
		this.speed = speed;
	}
	public String getMtu() {
		return mtu;
	}
	public void setMtu(String mtu) {
		this.mtu = mtu;
	}
	public String getMacAddr() {
		return macAddr;
	}
	public void setMacAddr(String macAddr) {
		this.macAddr = macAddr;
	}
	public String getVlan() {
		return vlan;
	}
	public void setVlan(String vlan) {
		this.vlan = vlan;
	}
	public String getMode() {
		return mode;
	}
	public void setMode(String mode) {
		this.mode = mode;
	}
	public String getIpAddr() {
		return ipAddr;
	}
	public void setIpAddr(String ipAddr) {
		this.ipAddr = ipAddr;
	}
	public String getSvi() {
		return svi;
	}
	public void setSvi(String svi) {
		this.svi = svi;
	}
}
