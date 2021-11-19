package io.nms.central.microservice.digitaltwin.model.graph;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.nms.central.microservice.common.functional.JsonUtils;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class DeviceConfigCollection {

	private Metadata metadata;
	
	@JsonProperty("interface")
	private List<Interface> netInterface;
	
	private List<Arp> arp;
	private List<Lldp> lldp;
	private List<Bgp> bgp;
	private List<AclTable> aclTable;
	private List<AclRule> aclRule;
	private List<Vlan> vlan;
	private List<IpRoute> ipRoute;
	
	/*-----------------------------------------------*/

	public DeviceConfigCollection() {}
	public DeviceConfigCollection(JsonObject json) {
		JsonUtils.fromJson(json, this, DeviceConfigCollection.class);
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
		return Objects.equals(toString(), ((DeviceConfigCollection) obj).toString());
	}
	@Override
	public int hashCode() {
		return Objects.hash(metadata.hashCode());
	}
	public Metadata getMetadata() {
		return metadata;
	}
	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
	}
	public List<Interface> getNetInterface() {
		return netInterface;
	}
	public void setNetInterface(List<Interface> netInterface) {
		this.netInterface = netInterface;
	}
	public List<Arp> getArp() {
		return arp;
	}
	public void setArp(List<Arp> arp) {
		this.arp = arp;
	}
	public List<Lldp> getLldp() {
		return lldp;
	}
	public void setLldp(List<Lldp> lldp) {
		this.lldp = lldp;
	}
	public List<Bgp> getBgp() {
		return bgp;
	}
	public void setBgp(List<Bgp> bgp) {
		this.bgp = bgp;
	}
	public List<AclTable> getAclTable() {
		return aclTable;
	}
	public void setAclTable(List<AclTable> aclTable) {
		this.aclTable = aclTable;
	}
	public List<AclRule> getAclRule() {
		return aclRule;
	}
	public void setAclRule(List<AclRule> aclRule) {
		this.aclRule = aclRule;
	}
	public List<Vlan> getVlan() {
		return vlan;
	}
	public void setVlan(List<Vlan> vlan) {
		this.vlan = vlan;
	}
	public List<IpRoute> getIpRoute() {
		return ipRoute;
	}
	public void setIpRoute(List<IpRoute> ipRoute) {
		this.ipRoute = ipRoute;
	}
	

	/*-----------------------------------------------*/
}
