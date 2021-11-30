package io.nms.central.microservice.digitaltwin.model.ipnetApi;

import java.util.Objects;

import io.nms.central.microservice.common.functional.JsonUtils;
import io.nms.central.microservice.digitaltwin.model.graph.HostTypeEnum;
import io.nms.central.microservice.digitaltwin.model.graph.Metadata.BgpStatusEnum;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class Device extends Configurable {
	
	private String name;
	private String hostname;
	private String mac;
	private String platform;
	private HostTypeEnum type;
	private String bgpAsn;
	private BgpStatusEnum bgpStatus;
	private String hwsku;
	
	/*-----------------------------------------------*/

	public Device() {}
	public Device(JsonObject json) {
		JsonUtils.fromJson(json, this, Device.class);
	}
	public String getHostname() {
		return hostname;
	}
	public void setHostname(String hostname) {
		this.hostname = hostname;
	}
	public String getMac() {
		return mac;
	}
	public void setMac(String mac) {
		this.mac = mac;
	}
	public String getPlatform() {
		return platform;
	}
	public void setPlatform(String platform) {
		this.platform = platform;
	}
	public HostTypeEnum getType() {
		return type;
	}
	public void setType(HostTypeEnum type) {
		this.type = type;
	}
	public String getBgpAsn() {
		return bgpAsn;
	}
	public void setBgpAsn(String bgpAsn) {
		this.bgpAsn = bgpAsn;
	}
	public BgpStatusEnum getBgpStatus() {
		return bgpStatus;
	}
	public void setBgpStatus(BgpStatusEnum bgpStatus) {
		this.bgpStatus = bgpStatus;
	}
	public String getHwsku() {
		return hwsku;
	}
	public void setHwsku(String hwsku) {
		this.hwsku = hwsku;
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
		return Objects.equals(toString(), ((Device) obj).toString());
	}
	@Override
	public int hashCode() {
		return Objects.hash(hostname);
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
}
