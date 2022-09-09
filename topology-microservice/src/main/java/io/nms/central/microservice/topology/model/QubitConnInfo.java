package io.nms.central.microservice.topology.model;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class QubitConnInfo extends ConnInfo {

	private String id;

	/*-----------------------------------------------*/
	
	public QubitConnInfo() {}
	public QubitConnInfo(JsonObject json) {}
	
	public JsonObject toJson() {
		return new JsonObject(Json.encode(this));
	}
	
	/*-----------------------------------------------*/
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
}
