package io.nms.central.microservice.qconnection.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.nms.central.microservice.common.functional.JsonUtils;
import io.nms.central.microservice.notification.model.Status.StatusEnum;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class Trail {
    // fields    
    private int id;
    private String name;
	private String label;
	private String description;
    private String created;
	private String updated;
	private StatusEnum status;
	private int vsubnetId;
	private Map<String, Object> info = new HashMap<String, Object>();
	private List<CrossConnect> oxcs = new ArrayList<CrossConnect>();
    
    /*-----------------------------------------------*/

	public Trail() {}
	public Trail(int id) {
		this.id = id;
	}
	public Trail(JsonObject json) {
		JsonUtils.fromJson(json, this, Trail.class);
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
		return Objects.equals(toString(), ((Trail) obj).toString());
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
	public Map<String, Object> getInfo() {
		return info;
	}
	public void setInfo(Map<String, Object> info) {
		this.info = info;
	}
	public StatusEnum getStatus() {
		return status;
	}
	public void setStatus(StatusEnum status) {
		this.status = status;
	}
	public int getVsubnetId() {
		return vsubnetId;
	}
	public void setVsubnetId(int vsubnetId) {
		this.vsubnetId = vsubnetId;
	}
	public List<CrossConnect> getOxcs() {
		return oxcs;
	}
	public void setOxcs(List<CrossConnect> oxcs) {
		this.oxcs = oxcs;
	}
}