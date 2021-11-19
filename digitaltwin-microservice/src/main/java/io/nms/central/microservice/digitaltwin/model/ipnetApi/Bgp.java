package io.nms.central.microservice.digitaltwin.model.ipnetApi;

import java.util.Objects;

import io.nms.central.microservice.common.functional.JSONUtils;
import io.nms.central.microservice.digitaltwin.model.graph.BgpState;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class Bgp extends Configurable {

	private String localAddr;
	private String remoteAddr;
	private String localAsn;
	private String remoteAsn;
	private String localId;
	private String remoteId;
	private String holdTime;
	private String keepAlive;
	private BgpState state;
	
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
		return Objects.hash(localAddr+localAsn+localId+remoteAddr+remoteAsn+remoteId);
	}
	public String getLocalAddr() {
		return localAddr;
	}
	public void setLocalAddr(String localAddr) {
		this.localAddr = localAddr;
	}
	public String getRemoteAddr() {
		return remoteAddr;
	}
	public void setRemoteAddr(String remoteAddr) {
		this.remoteAddr = remoteAddr;
	}
	public String getLocalAsn() {
		return localAsn;
	}
	public void setLocalAsn(String localAsn) {
		this.localAsn = localAsn;
	}
	public String getRemoteAsn() {
		return remoteAsn;
	}
	public void setRemoteAsn(String remoteAsn) {
		this.remoteAsn = remoteAsn;
	}
	public String getLocalId() {
		return localId;
	}
	public void setLocalId(String localId) {
		this.localId = localId;
	}
	public String getRemoteId() {
		return remoteId;
	}
	public void setRemoteId(String remoteId) {
		this.remoteId = remoteId;
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
	public BgpState getState() {
		return state;
	}
	public void setState(BgpState state) {
		this.state = state;
	}
	
}
