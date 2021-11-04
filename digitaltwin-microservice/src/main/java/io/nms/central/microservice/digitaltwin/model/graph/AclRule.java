package io.nms.central.microservice.digitaltwin.model.graph;

import java.util.Objects;

import io.nms.central.microservice.common.functional.JSONUtils;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class AclRule {

	private String table;
	private String rule;
	private int priority;
	private String action;
	private String match;
	
	/*-----------------------------------------------*/

	public AclRule() {}
	public AclRule(JsonObject json) {
		JSONUtils.fromJson(json, this, AclRule.class);
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
		return Objects.equals(toString(), ((AclRule) obj).toString());
	}
	@Override
	public int hashCode() {
		return Objects.hash(table+rule);
	}
	public String getTable() {
		return table;
	}
	public void setTable(String table) {
		this.table = table;
	}
	public String getRule() {
		return rule;
	}
	public void setRule(String rule) {
		this.rule = rule;
	}
	public int getPriority() {
		return priority;
	}
	public void setPriority(int priority) {
		this.priority = priority;
	}
	public String getAction() {
		return action;
	}
	public void setAction(String action) {
		this.action = action;
	}
	public String getMatch() {
		return match;
	}
	public void setMatch(String match) {
		this.match = match;
	}

	/*-----------------------------------------------*/
	
}
