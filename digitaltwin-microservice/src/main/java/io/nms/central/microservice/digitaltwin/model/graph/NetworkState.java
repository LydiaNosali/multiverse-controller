package io.nms.central.microservice.digitaltwin.model.graph;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import io.nms.central.microservice.common.functional.JsonUtils;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class NetworkState {

	private int id = 0;
	// collectionTime
	// collectionAgent
	public Map<String, DeviceState> getConfigs() {
		return configs;
	}
	public void setConfigs(Map<String, DeviceState> configs) {
		this.configs = configs;
	}
	private Map<String, DeviceState> configs = new HashMap<String, DeviceState>();
	
	/*-----------------------------------------------*/

	public NetworkState() {}
	public NetworkState(int id) {
		this.id = id;
	}
	public NetworkState(JsonObject json) {
		JsonUtils.fromJson(json, this, NetworkState.class);
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
		return Objects.equals(toString(), ((NetworkState) obj).toString());
	}
	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	/*-----------------------------------------------*/

	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
}
