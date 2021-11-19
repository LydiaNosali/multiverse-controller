package io.nms.central.microservice.digitaltwin.model.dt;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.nms.central.microservice.common.functional.JsonUtils;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class CreationReport {

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
	private String timestamp;
	
	private List<String> configProcessor;
	private List<String> queriesGenerator;
	private List<String> graphCreator;

	public CreationReport() {}
	public CreationReport(JsonObject json) {
		JsonUtils.fromJson(json, this, CreationReport.class);
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
		return Objects.equals(toString(), ((CreationReport) obj).toString());
	}
	@Override
	public int hashCode() {
		return Objects.hash(timestamp.hashCode()+configProcessor.hashCode()
		+queriesGenerator.hashCode()+graphCreator.hashCode());
	}
	
	public String getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}
	public List<String> getConfigProcessor() {
		return configProcessor;
	}
	public void setConfigProcessor(List<String> configProcessor) {
		this.configProcessor = configProcessor;
	}
	public List<String> getQueriesGenerator() {
		return queriesGenerator;
	}
	public void setQueriesGenerator(List<String> queriesGenerator) {
		this.queriesGenerator = queriesGenerator;
	}
	public List<String> getGraphCreator() {
		return graphCreator;
	}
	public void setGraphCreator(List<String> graphCreator) {
		this.graphCreator = graphCreator;
	}
}
