package io.nms.central.microservice.digitaltwin.model.graph;

public enum HostType {

	SpineRouter("SpineRouter"),
	LeafRouter("LeafRouter"),
	BorderRouter("BorderRouter"),
	Firewall("Firewall"),
	server("server"),
	Switch("Switch");

	private String value;
	private HostType(String value) { this.value = value; }
	public String getValue() { return this.value; }
}
