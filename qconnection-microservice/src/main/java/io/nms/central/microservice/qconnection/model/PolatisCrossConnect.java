package io.nms.central.microservice.qconnection.model;

public class PolatisCrossConnect {
	private PolatisPair PolatisPair;

	public PolatisCrossConnect() {
		super();
		// TODO Auto-generated constructor stub
	}

	public PolatisCrossConnect(PolatisPair PolatisPair) {
		super();
		this.PolatisPair = PolatisPair;
	}

	public PolatisPair getPair() {
		return PolatisPair;
	}

	public void setPair(PolatisPair PolatisPair) {
		this.PolatisPair = PolatisPair;
	}
}
