package io.nms.central.microservice.digitaltwin.model.graph;

import java.util.Objects;

import io.nms.central.microservice.common.functional.JSONUtils;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class Vlan {

	private String name;
	private String vid;
	private String member;
	private String mode; // enum: [untagged, tagged]
	
	/*-----------------------------------------------*/

	public Vlan() {}
	public Vlan(JsonObject json) {
		JSONUtils.fromJson(json, this, Vlan.class);
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
		return Objects.equals(toString(), ((Vlan) obj).toString());
	}
	@Override
	public int hashCode() {
		return Objects.hash(name+vid+member);
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getVid() {
		return vid;
	}
	public void setVid(String vid) {
		this.vid = vid;
	}
	public String getMember() {
		return member;
	}
	public void setMember(String member) {
		this.member = member;
	}
	public String getMode() {
		return mode;
	}
	public void setMode(String mode) {
		this.mode = mode;
	}

	/*-----------------------------------------------*/
}
