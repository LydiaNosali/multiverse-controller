package io.nms.central.microservice.topology.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import io.nms.central.microservice.common.functional.JsonUtils;
import io.nms.central.microservice.notification.model.Status.StatusEnum;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class Vnode {

	public enum NodeTypeEnum {
		SWITCH("SWITCH"),
		ROUTER("ROUTER"),
		NDNFWD("NDNFWD"),
		ENDSYS("ENDSYS");

		private String value;
		private NodeTypeEnum(String value) { this.value = value; }
		public String getValue() { return this.value; }
	};

	// common fields
	private int id = 0;
	private String name;
	private String label;
	private String description;
	private String created;
	private String updated;
	private StatusEnum status;
	private Map<String, Object> info = new HashMap<String, Object>();

	// specific Vnode fields
	private String location;
	private int posx;
	private int posy;
	private int vsubnetId;
	private NodeTypeEnum type;
	private String hwaddr;
	private String mgmtIp;

	/*-----------------------------------------------*/

	public Vnode() {}
	public Vnode(int id) {
		this.id = id;
	}
	public Vnode(JsonObject json) {
		JsonUtils.fromJson(json, this, Vnode.class);
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
		return Objects.equals(toString(), ((Vnode) obj).toString());
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

	public NodeTypeEnum getType() {
		return type;
	}
	public void setType(NodeTypeEnum type) {
		this.type = type;
	}

	public String getLocation() {
		return location;
	}
	public void setLocation(String location) {
		this.location = location;
	}

	public int getPosx() {
		return posx;
	}
	public void setPosx(int posx) {
		this.posx = posx;
	}

	public int getPosy() {
		return posy;
	}
	public void setPosy(int posy) {
		this.posy = posy;
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

	public Map<String, Object> getInfo() {
		return info;
	}
	public void setInfo(Map<String, Object> info) {
		this.info = info;
	}

	public String getHwaddr() {
		return hwaddr;
	}
	public void setHwaddr(String hwaddr) {
		this.hwaddr = hwaddr;
	}

	public String getMgmtIp() {
		return mgmtIp;
	}
	public void setMgmtIp(String mgmtIp) {
		this.mgmtIp = mgmtIp;
	}
}
