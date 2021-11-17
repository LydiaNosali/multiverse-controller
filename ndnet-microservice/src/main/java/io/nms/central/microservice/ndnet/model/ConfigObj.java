package io.nms.central.microservice.ndnet.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.nms.central.microservice.ndnet.model.ConfigObjConverter;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class ConfigObj {
	
	private int nodeId;
	private Config config = new Config();
	
	public ConfigObj() {}
	
	public ConfigObj(JsonObject json) {
		ConfigObjConverter.fromJson(json, this);
	}
	
	public JsonObject toJson() {
		JsonObject json = new JsonObject();
		ConfigObjConverter.toJson(this, json);
		return json;
	}

	@Override
	public int hashCode() {
		return Objects.hash(nodeId + getConfig().hashCode());
	}

	@Override
	public boolean equals(Object obj) {
		return getConfig().equals(((ConfigObj) obj).getConfig());
	}

	@Override
	public String toString() {
		return this.toJson().encodePrettily();
	}

	public int getNodeId() {
		return nodeId;
	}

	public void setNodeId(int nodeId) {
		this.nodeId = nodeId;
	}

	public Config getConfig() {
		return config;
	}

	public void setConfig(Config config) {
		this.config = config;
	}

}
