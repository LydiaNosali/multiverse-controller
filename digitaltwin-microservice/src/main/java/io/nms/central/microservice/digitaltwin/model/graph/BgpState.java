package io.nms.central.microservice.digitaltwin.model.graph;

public enum BgpState {
		Idle("Idle"),
		Connect("Connect"),
		Active("Active"),
		OpenSent("OpenSent"),
		OpenConfirm("OpenConfirm"),
		Established("Established");
		private String value;
		private BgpState(String value) { this.value = value; }
		public String getValue() { return this.value; }
}
