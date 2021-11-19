package io.nms.central.microservice.ipnet.model;

import java.util.Objects;

import io.nms.central.microservice.common.functional.JsonUtils;
import io.nms.central.microservice.digitaltwin.model.dt.VerificationReport;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class ConfigProfile {
	
	private String viewId;
	private String viewUpdated;
	private VerificationReport verifyReport;
	
	public ConfigProfile() {}
	public ConfigProfile(JsonObject json) {
		JsonUtils.fromJson(json, this, ConfigProfile.class);
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
		return Objects.equals(toString(), ((ConfigProfile) obj).toString());
	}
	@Override
	public int hashCode() {
		return Objects.hash(viewId+viewUpdated);
	}
	public String getViewId() {
		return viewId;
	}
	public void setViewId(String viewId) {
		this.viewId = viewId;
	}
	public String getViewUpdated() {
		return viewUpdated;
	}
	public void setViewUpdated(String viewUpdated) {
		this.viewUpdated = viewUpdated;
	}
	public VerificationReport getVerifyReport() {
		return verifyReport;
	}
	public void setVerifyReport(VerificationReport verifyReport) {
		this.verifyReport = verifyReport;
	}
}
