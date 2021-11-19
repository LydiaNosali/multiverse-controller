package io.nms.central.microservice.digitaltwin.model.graph;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.nms.central.microservice.common.functional.JSONUtils;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class Metadata {
	
	public enum BgpStatus {
		up("up"),
		down("down");
		private String value;
		private BgpStatus(String value) { this.value = value; }
		public String getValue() { return this.value; }
}

	@JsonProperty("HOSTNAME")
	private String hostname;
	
	@JsonProperty("MAC")
	private String mac;
	
	@JsonProperty("PLATFORM")
	private String platform;
	
	@JsonProperty("TYPE")
	private HostType type;
	
	@JsonProperty("BGP_ASN")
	private String bgpAsn;
	
	@JsonProperty("BGP_STATUS")
	private BgpStatus bgpStatus;
	
	@JsonProperty("HWSKU")
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
	public BgpStatus getBgpStatus() {
		return bgpStatus;
	}
	public void setBgpStatus(BgpStatus bgpStatus) {
		this.bgpStatus = bgpStatus;
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
