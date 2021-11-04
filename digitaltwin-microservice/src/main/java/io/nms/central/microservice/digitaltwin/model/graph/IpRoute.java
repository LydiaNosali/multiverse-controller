package io.nms.central.microservice.digitaltwin.model.graph;

import java.util.Objects;

import io.nms.central.microservice.common.functional.JSONUtils;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class IpRoute {
	
	public enum RouteType {
		K("K>*"),
		B("B>*"),
		C("C>*");
		private String value;
		private RouteType(String value) { this.value = value; }
		public String getValue() { return this.value; }
	};

	/*-----------------------------------------------*/
	
	private String netInterface;
	private String via;
	private String to;
	private RouteType type;
	
	public String getNetInterface() {
		return netInterface;
	}
	public void setNetInterface(String netInterface) {
		this.netInterface = netInterface;
	}
	public String getVia() {
		return via;
	}
	public void setVia(String via) {
		this.via = via;
	}
	public String getTo() {
		return to;
	}
	public void setTo(String to) {
		this.to = to;
	}
	public RouteType getType() {
		return type;
	}
	public void setType(RouteType type) {
		this.type = type;
	}

	/*-----------------------------------------------*/

	public IpRoute() {}
	public IpRoute(JsonObject json) {
		JSONUtils.fromJson(json, this, IpRoute.class);
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
		return Objects.equals(toString(), ((IpRoute) obj).toString());
	}
	@Override
	public int hashCode() {
		return Objects.hash(netInterface+via+to+type);
	}
}
