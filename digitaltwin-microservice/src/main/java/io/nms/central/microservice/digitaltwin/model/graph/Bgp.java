package io.nms.central.microservice.digitaltwin.model.graph;

import java.util.Objects;

import io.nms.central.microservice.common.functional.JSONUtils;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class Bgp {
		
	public enum BgpState {
		IDLE("Idle"),
		CONNECT("Connect"),
		ACTIVE("Active"),
		OPENSENT("OpenSent"),
		OPENCONFIRM("OpenConfirm"),
		ESTABLISHED("Established");
		private String value;
		private BgpState(String value) { this.value = value; }
		public String getValue() { return this.value; }
	};

	private String localAddress;
	private String neighborAddress;
	private String localAs;
	private String remoteAs;
	private String localRouterId;
	private String remoteRouterId;
	private String holdTime;
	private String keepAlive;
	private BgpState bgpState;
	
	/*-----------------------------------------------*/

	public Bgp() {}
	public Bgp(JsonObject json) {
		JSONUtils.fromJson(json, this, Bgp.class);
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
	public BgpState getBgpState() {
		return bgpState;
	}
	public void setBgpState(BgpState bgpState) {
		this.bgpState = bgpState;
	}

	/*-----------------------------------------------*/

}
