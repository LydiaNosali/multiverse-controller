package io.nms.central.microservice.qconnection.model;

public class PolatisPair {
	private int ingress;
	private int egress;

	public PolatisPair() {
		super();
		// TODO Auto-generated constructor stub
	}

	public int getIngress() {
		return ingress;
	}

	public void setIngress(int ingress) {
		this.ingress = ingress;
	}

	public int getEgress() {
		return egress;
	}

	public void setEgress(int egress) {
		this.egress = egress;
	}

	@Override
	public String toString() {
		return "{\"ingress\":" + ingress + ",\"egress\":" + egress + "}";
	}

	@Override
	public boolean equals(Object obj) {

		if (obj == this) {
			return true;
		}

		if (!(obj instanceof PolatisPair)) {
			return false;
		}

		PolatisPair other = (PolatisPair) obj;

		return ingress == other.ingress && egress == other.egress;
	}

	public PolatisPair(int ingress, int egress) {
		super();
		this.ingress = ingress;
		this.egress = egress;
	}

}
