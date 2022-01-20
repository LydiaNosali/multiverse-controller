package io.nms.central.microservice.digitaltwin.model.ipnetApi;

import java.util.Objects;
import io.nms.central.microservice.common.functional.JsonUtils;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class RouteHop {

	private Device host;
	private IpRoute route;
	private AclTable acl;
	private boolean arp;

	public RouteHop() {}
	public RouteHop(JsonObject json) {
		JsonUtils.fromJson(json, this, RouteHop.class);
	}

	public JsonObject toJson() {
		return new JsonObject(JsonUtils.pojo2Json(this, false));
	}
	@Override
	public String toString() {
		return JsonUtils.pojo2Json(this, false);
	}
	@Override
	public boolean equals(Object obj) {
		return Objects.equals(toString(), ((RouteHop) obj).toString());
	}
	@Override
	public int hashCode() {
		return Objects.hash(host.hashCode()+route.hashCode()+acl.hashCode());
	}
	public Device getHost() {
		return host;
	}
	public void setHost(Device host) {
		this.host = host;
	}
	public IpRoute getRoute() {
		return route;
	}
	public void setRoute(IpRoute route) {
		this.route = route;
	}
	public AclTable getAcl() {
		return acl;
	}
	public void setAcl(AclTable acl) {
		this.acl = acl;
	}
	public boolean isArp() {
		return arp;
	}
	public void setArp(boolean arp) {
		this.arp = arp;
	}
}
