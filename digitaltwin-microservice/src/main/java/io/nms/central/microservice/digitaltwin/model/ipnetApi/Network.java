package io.nms.central.microservice.digitaltwin.model.ipnetApi;

import java.util.List;
import java.util.Objects;

import io.nms.central.microservice.common.functional.JSONUtils;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class Network {
	
	private List<Device> devices;
	private List<Link> links;
	private List<IpSubnet> subnets;
	
	/*-----------------------------------------------*/
	
	public List<Device> getDevices() {
		return devices;
	}
	public void setDevices(List<Device> devices) {
		this.devices = devices;
	}
	public List<Link> getLinks() {
		return links;
	}
	public void setLinks(List<Link> links) {
		this.links = links;
	}
	public List<IpSubnet> getSubnets() {
		return subnets;
	}
	public void setSubnets(List<IpSubnet> subnets) {
		this.subnets = subnets;
	}
	public Network() {}
	public Network(JsonObject json) {
		JSONUtils.fromJson(json, this, Network.class);
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
		return Objects.equals(toString(), ((Network) obj).toString());
	}
	@Override
	public int hashCode() {
		return Objects.hash(devices.hashCode()+links.hashCode()+subnets.hashCode());
	}
}
