package io.nms.central.microservice.qconnection.model;

public class pair {
	private int ingress;
	private int egress;

	public pair() {
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

		if (!(obj instanceof pair)) {
			return false;
		}

		pair other = (pair) obj;

		return ingress == other.ingress && egress == other.egress;
	}

	public pair(int ingress, int egress) {
		super();
		this.ingress = ingress;
		this.egress = egress;
	}

}
