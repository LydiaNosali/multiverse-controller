package io.nms.central.microservice.qconnection.model;

import java.util.Objects;

import io.nms.central.microservice.common.functional.JsonUtils;
import io.nms.central.microservice.notification.model.Status.StatusEnum;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class CrossConnects {
    // fields    
    private int id;
	private int switchId;
	private int ingressPortId;
	private int egressPortId;
	private StatusEnum status;

    /*-----------------------------------------------*/

	public CrossConnects() {}
	public CrossConnects(int id) {
		this.id = id;
	}
	public CrossConnects(JsonObject json) {
		JsonUtils.fromJson(json, this, CrossConnects.class);
	}

	/*-----------------------------------------------*/

	public JsonObject toJson() {
		return new JsonObject(JsonUtils.pojo2Json(this, false));
	}
	@Override
	public String toString() {
		return JsonUtils.pojo2Json(this, false);
	}
	@Override
	public boolean equals(Object obj) {
		return Objects.equals(toString(), ((CrossConnects) obj).toString());
	}
	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	/*-----------------------------------------------*/

    public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}

    public int getSwitchId() {
		return switchId;
	}
	public void setSwitchId(int switchId) {
		this.switchId = switchId;
	}

	public int getIngressPortId() {
		return ingressPortId;
	}
	public void setIngressPortId(int ingressPortId) {
		this.ingressPortId = ingressPortId;
	}

	public int getEgressPortId() {
		return egressPortId;
	}
	public void setEgressPortId(int egressPortId) {
		this.egressPortId = egressPortId;
	}
	public StatusEnum getStatus() {
		return status;
	}
	public void setStatus(StatusEnum status) {
		this.status = status;
	}
}