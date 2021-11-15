package io.nms.central.microservice.digitaltwin.model.graph;

import java.util.Objects;

import io.nms.central.microservice.common.functional.JSONUtils;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class Metadata {

	private String hostname;
	private String mac;
	private String platform;
	private HostType type;
	private String bgpAsn;
	private String BgpStatus;
	private String hwsku;
	
	/*-----------------------------------------------*/
	
	public Metadata() {}
	public Metadata(JsonObject json) {
		JSONUtils.fromJson(json, this, Metadata.class);
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
	public HostType getType() {
		return type;
	}
	public void setType(HostType type) {
		this.type = type;
	}
	public String getBgpAsn() {
		return bgpAsn;
	}
	public void setBgpAsn(String bgpAsn) {
		this.bgpAsn = bgpAsn;
	}
	public String getBgpStatus() {
		return BgpStatus;
	}
	public void setBgpStatus(String bgpStatus) {
		BgpStatus = bgpStatus;
	}
	public String getHwsku() {
		return hwsku;
	}
	public void setHwsku(String hwsku) {
		this.hwsku = hwsku;
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
		return Objects.equals(toString(), ((Metadata) obj).toString());
	}
	@Override
	public int hashCode() {
		return Objects.hash(hostname);
	}
}
