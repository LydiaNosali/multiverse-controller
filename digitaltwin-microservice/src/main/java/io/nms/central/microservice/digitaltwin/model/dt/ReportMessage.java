package io.nms.central.microservice.digitaltwin.model.dt;

import java.util.List;
import java.util.Objects;

import io.nms.central.microservice.common.functional.JsonUtils;
import io.vertx.core.json.JsonObject;

public class ReportMessage {
	private String code; // To be defined
	private List<String> messages;

	public ReportMessage() {}
	public ReportMessage(JsonObject json) {
		JsonUtils.fromJson(json, this, ReportMessage.class);
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
		return Objects.equals(toString(), ((ReportMessage) obj).toString());
	}
	@Override
	public int hashCode() {
		return Objects.hash(code+messages.hashCode());
	}
	
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	public List<String> getMessages() {
		return messages;
	}
	public void setMessages(List<String> messages) {
		this.messages = messages;
	}
}