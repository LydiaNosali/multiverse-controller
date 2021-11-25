package io.nms.central.microservice.topology.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import io.nms.central.microservice.common.functional.JsonUtils;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class Vsubnet {
	
	public enum SubnetTypeEnum {
		QNET("QNET"),
		NDN("NDN");

		private String value;
		private SubnetTypeEnum(String value) { this.value = value; }
		public String getValue() { return this.value; }
	};

	// common fields
	private int id = 0;
	private String name;
	private String label;
	private String description;
	private SubnetTypeEnum type;
	private String created;
	private String updated;
	private Map<String, Object> info = new HashMap<String, Object>();

	/*-----------------------------------------------*/

	public Vsubnet() {}
	public Vsubnet(int id) {
		this.id = id;
	}
	public Vsubnet(JsonObject json) {
		JsonUtils.fromJson(json, this, Vsubnet.class);
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
		return Objects.equals(toString(), ((Vsubnet) obj).toString());
	}
	@Override
	public int hashCode() {
		return Objects.hash(this.id);
	}
	
	/*-----------------------------------------------*/

	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}

	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}

	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}

	public String getCreated() {
		return created;
	}
	public void setCreated(String created) {
		this.created = created;
	}

	public String getUpdated() {
		return updated;
	}
	public void setUpdated(String updated) {
		this.updated = updated;
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	public Map<String, Object> getInfo() {
		return info;
	}
	public void setInfo(Map<String, Object> info) {
		this.info = info;
	}
	public SubnetTypeEnum getType() {
		return type;
	}
	public void setType(SubnetTypeEnum type) {
		this.type = type;
	}
}

