package io.nms.central.microservice.digitaltwin.model.dt;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.nms.central.microservice.common.functional.JsonUtils;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class VerificationReport {

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
	private String timestamp;
	private String netId;
	private List<ReportMessage> reports;

	public VerificationReport() {}
	public VerificationReport(JsonObject json) {
		JsonUtils.fromJson(json, this, VerificationReport.class);
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
		return Objects.equals(toString(), ((VerificationReport) obj).toString());
	}
	@Override
	public int hashCode() {
		return Objects.hash(timestamp.hashCode()+reports.hashCode());
	}
	
	public String getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}
	public List<ReportMessage> getReports() {
		return reports;
	}
	public void setReports(List<ReportMessage> reports) {
		this.reports = reports;
	}
	public String getNetId() {
		return netId;
	}
	public void setNetId(String netId) {
		this.netId = netId;
	}
}
