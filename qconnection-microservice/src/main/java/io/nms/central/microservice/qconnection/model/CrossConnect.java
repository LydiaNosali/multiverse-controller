package io.nms.central.microservice.qconnection.model;

public class CrossConnect {
	private pair pair;

	public CrossConnect() {
		super();
		// TODO Auto-generated constructor stub
	}

	public CrossConnect(pair pair) {
		super();
		this.pair = pair;
	}

	public pair getPair() {
		return pair;
	}

	public void setPair(pair pair) {
		this.pair = pair;
	}
}
