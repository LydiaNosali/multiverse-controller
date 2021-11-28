package io.nms.central.microservice.account.model;

import io.vertx.core.json.JsonObject;
import java.util.Objects;
import io.vertx.codegen.annotations.DataObject;

@DataObject(generateConverter = true)
public class Agent {
	private String username;
	private String password;
	private String role;
	private int nodeId;
	
	public Agent() {}
	
	public Agent(JsonObject json) {
		AgentConverter.fromJson(json, this);
	}
	
	public JsonObject toJson() {
		JsonObject json = new JsonObject();
		AgentConverter.toJson(this, json);
		return json;
	}

	@Override
	public int hashCode() {
		return Objects.hash(username);
	}

	@Override
	public boolean equals(Object obj) {
		return Objects.equals(hashCode(), ((Agent) obj).hashCode());
	}

	@Override
	public String toString() {
		return this.toJson().encodePrettily();
	}
	
	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public int getNodeId() {
		return nodeId;
	}

	public void setNodeId(int nodeId) {
		this.nodeId = nodeId;
	}
}
