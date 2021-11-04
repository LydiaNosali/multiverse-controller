package io.nms.central.microservice.digitaltwin.model.graph;

import java.util.Objects;

import io.nms.central.microservice.common.functional.JSONUtils;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class QueryResult {

	private String result;
	
	/*-----------------------------------------------*/

	public QueryResult() {}
	public QueryResult(JsonObject json) {
		JSONUtils.fromJson(json, this, QueryResult.class);
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
		return Objects.equals(toString(), ((QueryResult) obj).toString());
	}
	@Override
	public int hashCode() {
		return Objects.hash(result);
	}

}
