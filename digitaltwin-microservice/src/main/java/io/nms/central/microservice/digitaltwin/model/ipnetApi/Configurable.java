package io.nms.central.microservice.digitaltwin.model.ipnetApi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.vertx.core.json.JsonObject;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class Configurable {
	public abstract JsonObject toJson();
    protected Configurable() {}
}