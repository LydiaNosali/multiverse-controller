package io.nms.central.microservice.digitaltwin.model.dt;

import java.util.Objects;

import io.nms.central.microservice.common.functional.JsonUtils;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class DtQuery {

	private String text;
	
	/*-----------------------------------------------*/

	public DtQuery() {}
	public DtQuery(JsonObject json) {
		JsonUtils.fromJson(json, this, DtQuery.class);
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
		return Objects.equals(toString(), ((DtQuery) obj).toString());
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
