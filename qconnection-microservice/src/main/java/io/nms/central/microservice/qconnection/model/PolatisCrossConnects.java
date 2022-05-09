package io.nms.central.microservice.qconnection.model;

import java.util.List;

public class PolatisCrossConnects {
	List<PolatisCrossConnect> polatisCrossconnects;

	public List<PolatisCrossConnect> getPolatisCrossconnects() {
		return polatisCrossconnects;
	}

	public void setPolatisCrossconnects(List<PolatisCrossConnect> polatisCrossconnects) {
		this.polatisCrossconnects = polatisCrossconnects;
	}

	public PolatisCrossConnects(List<PolatisCrossConnect> polatisCrossconnects) {
		super();
		this.polatisCrossconnects = polatisCrossconnects;
	}

	public PolatisCrossConnects() {
		super();
		// TODO Auto-generated constructor stub
	}
}
