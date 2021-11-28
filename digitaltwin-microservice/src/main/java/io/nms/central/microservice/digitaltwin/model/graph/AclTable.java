package io.nms.central.microservice.digitaltwin.model.graph;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.nms.central.microservice.common.functional.JsonUtils;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class AclTable {
	
	public enum AclTypeEnum {
		L3("L3"),
		L3V6("L3V6"),
		CTRLPLANE("CTRLPLANE"),
		MIRROR("MIRROR");
		private String value;
		private AclTypeEnum(String value) { this.value = value; }
		public String getValue() { return this.value; }
	}
	
	public enum AclStageEnum {
		ingress("ingress"),
		egress("egress");
		private String value;
		private AclStageEnum(String value) { this.value = value; }
		public String getValue() { return this.value; }
	}

	@JsonProperty("NAME")
	private String name;
	
	@JsonProperty("TYPE")
	private AclTypeEnum type;
	
	@JsonProperty("BINDING")
	private String binding;
	
	@JsonProperty("DESCRIPTION")
	private String description;
	
	@JsonProperty("STAGE")
	private AclStageEnum stage;

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
		return Objects.hash(name);
	}
	
	/*-----------------------------------------------*/
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public AclTypeEnum getType() {
		return type;
	}
	public void setType(AclTypeEnum type) {
		this.type = type;
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
}
