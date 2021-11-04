package io.nms.central.microservice.digitaltwin.model.graph;

public enum HostType {

	SpineRouter("SpineRouter"),
	SpineLeaf("SpineLeaf"),
	Server("Server");

	private String value;
	private HostType(String value) { this.value = value; }
	public String getValue() { return this.value; }
}
