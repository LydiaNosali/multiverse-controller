package io.nms.central.microservice.topology.model;

import java.util.Objects;

import io.nms.central.microservice.common.functional.JSONUtils;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class CrossConnect {
    // fields
    private int id;
    private String name;
	private String label;
	private String description;
    private int switchId;
    private String switchIpAddr;
    private int ingressPortId; 
    private int egressPortId;
    private String created;
	private String updated;
    
    /*-----------------------------------------------*/

	public CrossConnect() {}
	public CrossConnect(int id) {
		this.id = id;
	}
	public CrossConnect(JsonObject json) {
		JSONUtils.fromJson(json, this, CrossConnect.class);
	}

	/*-----------------------------------------------*/

	public JsonObject toJson() {
		return new JsonObject(JSONUtils.pojo2Json(this, false));
	}
	@Override
	public String toString() {
		return JSONUtils.pojo2Json(this, false);
	}
	@Override
	public boolean equals(Object obj) {
		return Objects.equals(toString(), ((CrossConnect) obj).toString());
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
	
	public String getSwitchIpAddr() {
		return switchIpAddr;
	}
	public void setSwitchIpAddr(String switchIpAddr) {
		this.switchIpAddr = switchIpAddr;
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

	public String getCreated() {
		return created;
	}
	public void setCreated(String created) {
		this.created = created;
	}

	public String getUpdated() {
		return updated;
	}
	public void setUpdated(String updated) {
		this.updated = updated;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
}