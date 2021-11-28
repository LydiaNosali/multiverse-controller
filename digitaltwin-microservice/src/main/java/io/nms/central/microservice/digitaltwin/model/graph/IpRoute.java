package io.nms.central.microservice.digitaltwin.model.graph;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.nms.central.microservice.common.functional.JsonUtils;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class IpRoute {

	@JsonProperty("INTERFACE")
	private String netInterface;
	
	@JsonProperty("VIA")
	private String via;
	
	@JsonProperty("TO")
	private String to;
	
	@JsonProperty("TYPE")
	private String type;
	
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
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}

	/*-----------------------------------------------*/

	public IpRoute() {}
	public IpRoute(JsonObject json) {
		JsonUtils.fromJson(json, this, IpRoute.class);
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
		return Objects.equals(toString(), ((IpRoute) obj).toString());
	}
	@Override
	public int hashCode() {
		return Objects.hash(netInterface+via+to+type);
	}
}
