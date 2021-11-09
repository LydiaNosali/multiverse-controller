package io.nms.central.microservice.digitaltwin.model.ipnetApi;

import java.util.List;
import java.util.Objects;

import io.nms.central.microservice.common.functional.JSONUtils;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class IpSubnet {
	
	private String netAddress;
	private List<String> hostAddresses;
	
	/*-----------------------------------------------*/
	
	public IpSubnet() {}
	public IpSubnet(JsonObject json) {
		JSONUtils.fromJson(json, this, IpSubnet.class);
	}

	/*-----------------------------------------------*/

	public String getNetAddress() {
		return netAddress;
	}
	public void setNetAddress(String netAddress) {
		this.netAddress = netAddress;
	}
	
	public List<String> getHostAddresses() {
		return hostAddresses;
	}
	public void setHostAddresses(List<String> hostAddresses) {
		this.hostAddresses = hostAddresses;
	}
	
	public JsonObject toJson() {
		return new JsonObject(JSONUtils.pojo2Json(this, false));
	}
	@Override
	public String toString() {
		return JSONUtils.pojo2Json(this, false);
	}
	@Override
	public boolean equals(Object obj) {
		return Objects.equals(toString(), ((IpSubnet) obj).toString());
	}
	@Override
	public int hashCode() {
		return Objects.hash(netAddress);
	}
}
