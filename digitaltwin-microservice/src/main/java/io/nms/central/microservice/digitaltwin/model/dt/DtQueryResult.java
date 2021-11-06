package io.nms.central.microservice.digitaltwin.model.dt;

import java.util.Objects;

import io.nms.central.microservice.common.functional.JSONUtils;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class DtQueryResult {

	private String result;
	
	/*-----------------------------------------------*/

	public DtQueryResult() {}
	public DtQueryResult(JsonObject json) {
		JSONUtils.fromJson(json, this, DtQueryResult.class);
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
		return Objects.equals(toString(), ((DtQueryResult) obj).toString());
	}
	@Override
	public int hashCode() {
		return Objects.hash(result);
	}

}
