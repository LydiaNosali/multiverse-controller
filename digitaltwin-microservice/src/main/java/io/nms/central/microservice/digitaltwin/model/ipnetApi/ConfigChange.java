package io.nms.central.microservice.digitaltwin.model.ipnetApi;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import io.nms.central.microservice.common.functional.JSONUtils;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class ConfigChange {
	
	public enum ResourceTypeEnum {
		DEVICE("DEVICE"), INTERFACE("INTERFACE"), 
		BGP("BGP"), IPROUTE("IPROUTE"), VLAN("VLAN"),
		ACLTABLE("ACLTABLE"), ACLRULE("ACLRULE");
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

	private ResourceTypeEnum resourceType;
	private String resourceLocation;
	private ActionEnum action;

	@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "resourceType")
	@JsonSubTypes({@Type(value = Device.class, name = "DEVICE"),
        @Type(value = NetInterface.class, name = "INTERFACE"), @Type(value = Bgp.class, name = "BGP")})
	private Configurable content;
	
	public ConfigChange() {}
	public ConfigChange(JsonObject json) {
		JSONUtils.fromJson(json, this, ConfigChange.class);
	}

	public JsonObject toJson() {
		return new JsonObject(JSONUtils.pojo2Json(this, false));
	}
	@Override
	public String toString() {
		return JSONUtils.pojo2Json(this, false);
	}
	@Override
	public boolean equals(Object obj) {
		return Objects.equals(toString(), ((ConfigChange) obj).toString());
	}
	@Override
	public int hashCode() {
		return Objects.hash(resourceType.getValue()+resourceLocation+action.getValue()+content.hashCode());
	}
	public ResourceTypeEnum getResourceType() {
		return resourceType;
	}
	public void setResourceType(ResourceTypeEnum resourceType) {
		this.resourceType = resourceType;
	}
	public String getResourceLocation() {
		return resourceLocation;
	}
	public void setResourceLocation(String resourceLocation) {
		this.resourceLocation = resourceLocation;
	}
	public ActionEnum getAction() {
		return action;
	}
	public void setAction(ActionEnum action) {
		this.action = action;
	}
	public Configurable getContent() {
		return content;
	}
	public void setContent(Configurable content) {
		this.content = content;
	}
}
