package io.nms.central.microservice.digitaltwin.model.graph;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import io.nms.central.microservice.common.functional.JsonUtils;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class NetConfigCollection {

	private int id = 0;
	// collectionTime
	// collectionAgent
	public Map<String, DeviceConfigCollection> getConfigs() {
		return configs;
	}
	public void setConfigs(Map<String, DeviceConfigCollection> configs) {
		this.configs = configs;
	}
	private Map<String, DeviceConfigCollection> configs = new HashMap<String, DeviceConfigCollection>();
	
	/*-----------------------------------------------*/

	public NetConfigCollection() {}
	public NetConfigCollection(int id) {
		this.id = id;
	}
	public NetConfigCollection(JsonObject json) {
		JsonUtils.fromJson(json, this, NetConfigCollection.class);
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
		return Objects.equals(toString(), ((NetConfigCollection) obj).toString());
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
