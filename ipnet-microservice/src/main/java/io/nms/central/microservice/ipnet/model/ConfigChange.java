package io.nms.central.microservice.ipnet.model;

import java.util.Objects;
import java.util.UUID;

import io.nms.central.microservice.common.functional.JsonUtils;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class ConfigChange {
	
	public enum ResourceTypeEnum {
		DEVICE("DEVICE"), INTERFACE("INTERFACE"), 
		BGP("BGP"), IPROUTE("IPROUTE"),
		ACLTABLE("ACLTABLE"), ACLRULE("ACLRULE"),
		LINK("LINK");
        private String value;
        private ResourceTypeEnum(String value) {this.value = value;}
        public String getValue() {return this.value;}
    };
    
    public enum ActionEnum {
		CREATE("CREATE"), UPDATE("UPDATE"), DELETE("DELETE");
        private String value;
        private ActionEnum(String value) {this.value = value;}
        public String getValue() {return this.value;}
    };

    private String id;
	private ResourceTypeEnum type;
	private ActionEnum action;
	private String location;
	private String datetime;
	// private Report report;
	
	public ConfigChange() {
		this.id = UUID.randomUUID().toString();
	}
	public ConfigChange(String id) {
		this.id = id;
	}
	public ConfigChange(JsonObject json) {
		JsonUtils.fromJson(json, this, ConfigChange.class);
	}

	public JsonObject toJson() {
		return new JsonObject(JsonUtils.pojo2Json(this, false));
	}
	@Override
	public String toString() {
		return JsonUtils.pojo2Json(this, false);
	}
	@Override
	public boolean equals(Object obj) {
		return Objects.equals(toString(), ((ConfigChange) obj).toString());
	}
	@Override
	public int hashCode() {
		return Objects.hash(id+type.getValue()+location+action.getValue());
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public ResourceTypeEnum getType() {
		return type;
	}
	public void setType(ResourceTypeEnum type) {
		this.type = type;
	}
	public String getLocation() {
		return location;
	}
	public void setLocation(String location) {
		this.location = location;
	}
	public ActionEnum getAction() {
		return action;
	}
	public void setAction(ActionEnum action) {
		this.action = action;
	}
	public String getDatetime() {
		return datetime;
	}
	public void setDatetime(String datetime) {
		this.datetime = datetime;
	}
}
