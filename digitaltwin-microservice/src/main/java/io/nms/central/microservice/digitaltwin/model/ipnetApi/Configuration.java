package io.nms.central.microservice.digitaltwin.model.ipnetApi;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import io.nms.central.microservice.common.functional.JSONUtils;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class Configuration {
	
	private Map<String, ConfigChange> configChanges = new HashMap<String, ConfigChange>();

	public Configuration() {}
	public Configuration(JsonObject json) {
		JSONUtils.fromJson(json, this, Configuration.class);
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
		return Objects.equals(toString(), ((Configuration) obj).toString());
	}
	@Override
	public int hashCode() {
		return Objects.hash(configChanges.hashCode());
	}
	
	public Map<String, ConfigChange> getConfigs() {
		return configChanges;
	}
	public void setConfigs(Map<String, ConfigChange> configChanges) {
		this.configChanges = configChanges;
	}
}
