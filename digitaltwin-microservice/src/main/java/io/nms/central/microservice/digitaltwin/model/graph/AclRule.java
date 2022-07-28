package io.nms.central.microservice.digitaltwin.model.graph;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.nms.central.microservice.common.functional.JsonUtils;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class AclRule {

	@JsonProperty("TABLE")
	private String tableName;
	
	@JsonProperty("RULE")
	private String ruleName;
	
	@JsonProperty("PRIORITY")
	private String priority;
	
	@JsonProperty("ACTION")
	private String action;
	
	@JsonProperty("MATCH")
	private List<String> match;
	
	/*-----------------------------------------------*/

	public AclRule() {}
	public AclRule(JsonObject json) {
		JsonUtils.fromJson(json, this, AclRule.class);
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
		return Objects.equals(toString(), ((AclRule) obj).toString());
	}
	@Override
	public int hashCode() {
		return Objects.hash(tableName+ruleName);
	}
	public String getTableName() {
		return tableName;
	}
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}
	public String getRuleName() {
		return ruleName;
	}
	public void setRuleName(String ruleName) {
		this.ruleName = ruleName;
	}
	public String getPriority() {
		return priority;
	}
	public void setPriority(String priority) {
		this.priority = priority;
	}
	public String getAction() {
		return action;
	}
	public void setAction(String action) {
		this.action = action;
	}
	public List<String> getMatch() {
		return match;
	}
	public void setMatch(List<String> match) {
		this.match = match;
	}

	/*-----------------------------------------------*/
	
}
