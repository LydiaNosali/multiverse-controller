package io.nms.central.microservice.digitaltwin.model.ipnetApi;

import java.util.List;
import java.util.Objects;

import io.nms.central.microservice.common.functional.JsonUtils;
import io.nms.central.microservice.digitaltwin.model.graph.AclTable.AclStageEnum;
import io.nms.central.microservice.digitaltwin.model.graph.AclTable.AclTypeEnum;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class AclTable extends Configurable {

	private String name;
	private String binding;
	private String description;
	private AclStageEnum stage;
	private AclTypeEnum type;
	private List<AclRule> rules;

	/*-----------------------------------------------*/

	public AclTable() {}
	public AclTable(JsonObject json) {
		JsonUtils.fromJson(json, this, AclTable.class);
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
		return Objects.equals(toString(), ((AclTable) obj).toString());
	}
	@Override
	public int hashCode() {
		return Objects.hash(name+binding+description+stage.getValue()+type.getValue());
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getBinding() {
		return binding;
	}
	public void setBinding(String binding) {
		this.binding = binding;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public AclStageEnum getStage() {
		return stage;
	}
	public void setStage(AclStageEnum stage) {
		this.stage = stage;
	}
	public AclTypeEnum getType() {
		return type;
	}
	public void setType(AclTypeEnum type) {
		this.type = type;
	}
	public List<AclRule> getRules() {
		return rules;
	}
	public void setRules(List<AclRule> rules) {
		this.rules = rules;
	}
}
