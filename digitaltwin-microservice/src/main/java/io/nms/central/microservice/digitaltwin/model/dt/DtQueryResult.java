package io.nms.central.microservice.digitaltwin.model.dt;

import java.util.Objects;

import io.nms.central.microservice.common.functional.JsonUtils;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class DtQueryResult {

	private String result;
	
	/*-----------------------------------------------*/

	public String getResult() {
		return result;
	}
	public void setResult(String result) {
		this.result = result;
	}
	public DtQueryResult() {}
	public DtQueryResult(JsonObject json) {
		JsonUtils.fromJson(json, this, DtQueryResult.class);
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
		return Objects.equals(toString(), ((DtQueryResult) obj).toString());
	}
	@Override
	public int hashCode() {
		return Objects.hash(result);
	}

}
