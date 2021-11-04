package io.nms.central.microservice.digitaltwin.model.graph;

import java.util.Objects;

import io.nms.central.microservice.common.functional.JSONUtils;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class Lldp {

	private String localPort;
	private String remoteDevice;
	private String remotePort;
	
	/*-----------------------------------------------*/

	public Lldp() {}
	public Lldp(JsonObject json) {
		JSONUtils.fromJson(json, this, Lldp.class);
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
		return Objects.equals(toString(), ((Lldp) obj).toString());
	}
	@Override
	public int hashCode() {
		return Objects.hash(localPort+remoteDevice+remotePort);
	}
	public String getLocalPort() {
		return localPort;
	}
	public void setLocalPort(String localPort) {
		this.localPort = localPort;
	}
	public String getRemoteDevice() {
		return remoteDevice;
	}
	public void setRemoteDevice(String remoteDevice) {
		this.remoteDevice = remoteDevice;
	}
	public String getRemotePort() {
		return remotePort;
	}
	public void setRemotePort(String remotePort) {
		this.remotePort = remotePort;
	}

	/*-----------------------------------------------*/
	
}
