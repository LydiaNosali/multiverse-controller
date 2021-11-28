package io.nms.central.microservice.digitaltwin.model.graph;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.nms.central.microservice.common.functional.JsonUtils;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class Bgp {

	@JsonProperty("LOCALADDRESS")
	private String localAddress;
	
	@JsonProperty("NEIGHBORADDRESS")
	private String neighborAddress;
	
	@JsonProperty("LOCALAS")
	private String localAs;
	
	@JsonProperty("REMOTEAS")
	private String remoteAs;
	
	@JsonProperty("LOCALROUTERID")
	private String localRouterId;
	
	@JsonProperty("REMOTEROUTERID")
	private String remoteRouterId;
	
	@JsonProperty("HOLDTIME")
	private String holdTime;
	
	@JsonProperty("KEEPALIVE")
	private String keepAlive;
	
	@JsonProperty("BGPSTATE")
	private BgpStateEnum bgpState;
	
	/*-----------------------------------------------*/

	public Bgp() {}
	public Bgp(JsonObject json) {
		JsonUtils.fromJson(json, this, Bgp.class);
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
		return Objects.equals(toString(), ((Bgp) obj).toString());
	}
	@Override
	public int hashCode() {
		return Objects.hash(localAddress+localAs+localRouterId+neighborAddress+remoteAs+remoteRouterId);
	}
	public String getLocalAddress() {
		return localAddress;
	}
	public void setLocalAddress(String localAddress) {
		this.localAddress = localAddress;
	}
	public String getNeighborAddress() {
		return neighborAddress;
	}
	public void setNeighborAddress(String neighborAddress) {
		this.neighborAddress = neighborAddress;
	}
	public String getLocalAs() {
		return localAs;
	}
	public void setLocalAs(String localAs) {
		this.localAs = localAs;
	}
	public String getRemoteAs() {
		return remoteAs;
	}
	public void setRemoteAs(String remoteAs) {
		this.remoteAs = remoteAs;
	}
	public String getLocalRouterId() {
		return localRouterId;
	}
	public void setLocalRouterId(String localRouterId) {
		this.localRouterId = localRouterId;
	}
	public String getRemoteRouterId() {
		return remoteRouterId;
	}
	public void setRemoteRouterId(String remoteRouterId) {
		this.remoteRouterId = remoteRouterId;
	}
	public String getHoldTime() {
		return holdTime;
	}
	public void setHoldTime(String holdTime) {
		this.holdTime = holdTime;
	}
	public String getKeepAlive() {
		return keepAlive;
	}
	public void setKeepAlive(String keepAlive) {
		this.keepAlive = keepAlive;
	}
	public BgpStateEnum getBgpState() {
		return bgpState;
	}
	public void setBgpState(BgpStateEnum bgpState) {
		this.bgpState = bgpState;
	}

	/*-----------------------------------------------*/

}
