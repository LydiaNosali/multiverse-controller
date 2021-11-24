package io.nms.central.microservice.digitaltwin.model.graph;

public enum BgpStateEnum {
		Idle("Idle"),
		Connect("Connect"),
		Active("Active"),
		OpenSent("OpenSent"),
		OpenConfirm("OpenConfirm"),
		Established("Established");
		private String value;
		private BgpStateEnum(String value) { this.value = value; }
		public String getValue() { return this.value; }
}
