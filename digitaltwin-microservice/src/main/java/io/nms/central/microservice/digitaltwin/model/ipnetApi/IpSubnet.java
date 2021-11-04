package io.nms.central.microservice.digitaltwin.model.ipnetApi;

import java.util.Objects;

import io.nms.central.microservice.common.functional.JSONUtils;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class IpSubnet {
	
	private String netAddress;
    public String getNetAddress() {
		return netAddress;
	}
	public void setNetAddress(String netAddress) {
		this.netAddress = netAddress;
	}
	public int getUsedAddresses() {
		return usedAddresses;
	}
	public void setUsedAddresses(int usedAddresses) {
		this.usedAddresses = usedAddresses;
	}
	private int usedAddresses;
	
	/*-----------------------------------------------*/
	
	public IpSubnet() {}
	public IpSubnet(JsonObject json) {
		JSONUtils.fromJson(json, this, IpSubnet.class);
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
		return Objects.equals(toString(), ((IpSubnet) obj).toString());
	}
	@Override
	public int hashCode() {
		return Objects.hash(netAddress);
	}
}
