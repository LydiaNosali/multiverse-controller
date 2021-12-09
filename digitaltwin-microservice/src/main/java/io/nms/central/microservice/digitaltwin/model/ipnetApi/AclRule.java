package io.nms.central.microservice.digitaltwin.model.ipnetApi;

import java.util.Objects;

import io.nms.central.microservice.common.functional.JsonUtils;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class AclRule extends Configurable {

	private String name;
	private String priority;
	private String action;
	private String matching;
	
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
		return Objects.hash(name+priority+action+matching);
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
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
	public String getMatching() {
		return matching;
	}
	public void setMatching(String matching) {
		this.matching = matching;
	}	
}
