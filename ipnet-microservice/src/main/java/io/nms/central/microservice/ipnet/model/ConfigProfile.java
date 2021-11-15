package io.nms.central.microservice.ipnet.model;

import java.util.Objects;

import io.nms.central.microservice.common.functional.JSONUtils;
import io.nms.central.microservice.digitaltwin.model.dt.Report;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class ConfigProfile {
	
	private String viewId;
	private String viewUpdated;
	private Report verifReport;
	
	public ConfigProfile() {}
	public ConfigProfile(JsonObject json) {
		JSONUtils.fromJson(json, this, ConfigProfile.class);
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
	public Report getVerifReport() {
		return verifReport;
	}
	public void setVerifReport(Report verifReport) {
		this.verifReport = verifReport;
	}
}
