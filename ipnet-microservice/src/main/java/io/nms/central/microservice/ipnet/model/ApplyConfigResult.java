package io.nms.central.microservice.ipnet.model;

import java.util.Objects;

import io.nms.central.microservice.common.functional.JSONUtils;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class ApplyConfigResult {
	
	private String message;

	public ApplyConfigResult() {}
	public ApplyConfigResult(JsonObject json) {
		JSONUtils.fromJson(json, this, ApplyConfigResult.class);
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
		return Objects.equals(toString(), ((ApplyConfigResult) obj).toString());
	}
	@Override
	public int hashCode() {
		return Objects.hash(message.hashCode());
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
}
