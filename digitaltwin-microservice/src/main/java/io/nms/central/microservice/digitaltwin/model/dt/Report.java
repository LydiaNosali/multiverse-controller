package io.nms.central.microservice.digitaltwin.model.dt;

import java.util.List;
import java.util.Objects;

import io.nms.central.microservice.common.functional.JSONUtils;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class Report {

	private String ts;
	private List<ReportMessage> report;

	public Report() {}
	public Report(JsonObject json) {
		JSONUtils.fromJson(json, this, Report.class);
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
		return Objects.equals(toString(), ((Report) obj).toString());
	}
	@Override
	public int hashCode() {
		return Objects.hash(ts+report.hashCode());
	}
	
	public String getTs() {
		return ts;
	}
	public void setTs(String ts) {
		this.ts = ts;
	}
	public List<ReportMessage> getReport() {
		return report;
	}
	public void setReport(List<ReportMessage> report) {
		this.report = report;
	}

	/*-----------------------------------------------*/
	
	public class ReportMessage {
		private int code; // To be defined
		private String message;

		public ReportMessage() {}
		public ReportMessage(JsonObject json) {
			JSONUtils.fromJson(json, this, ReportMessage.class);
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
			return Objects.equals(toString(), ((ReportMessage) obj).toString());
		}
		@Override
		public int hashCode() {
			return Objects.hash(code+message);
		}
		
		public int getCode() {
			return code;
		}
		public void setCode(int code) {
			this.code = code;
		}
		public String getMessage() {
			return message;
		}
		public void setMessage(String message) {
			this.message = message;
		}
	}
}
