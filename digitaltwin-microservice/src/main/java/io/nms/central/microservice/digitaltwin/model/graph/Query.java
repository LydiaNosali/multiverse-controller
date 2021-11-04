package io.nms.central.microservice.digitaltwin.model.graph;

import java.util.Objects;

import io.nms.central.microservice.common.functional.JSONUtils;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class Query {

	private String text;
	
	/*-----------------------------------------------*/

	public Query() {}
	public Query(JsonObject json) {
		JSONUtils.fromJson(json, this, Query.class);
	}

	/*-----------------------------------------------*/

	public JsonObject toJson() {
		return new JsonObject(JSONUtils.pojo2Json(this, false));
	}
	@Override
	public String toString() {
		return JSONUtils.pojo2Json(this, false);
	}
	@Override
	public boolean equals(Object obj) {
		return Objects.equals(toString(), ((Query) obj).toString());
	}
	@Override
	public int hashCode() {
		return Objects.hash(text);
	}
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
}
