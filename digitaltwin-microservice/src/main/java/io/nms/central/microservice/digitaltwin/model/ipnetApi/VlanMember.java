package io.nms.central.microservice.digitaltwin.model.ipnetApi;

import java.util.Objects;

import io.nms.central.microservice.common.functional.JsonUtils;
import io.nms.central.microservice.digitaltwin.model.graph.Vlan.VlanModeEnum;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class VlanMember {
	
	private int itfName;
	private VlanModeEnum mode;

	/*-----------------------------------------------*/

	public VlanMember() {}
	public int getItfName() {
		return itfName;
	}
	public void setItfName(int itfName) {
		this.itfName = itfName;
	}
	public VlanModeEnum getMode() {
		return mode;
	}
	public void setMode(VlanModeEnum mode) {
		this.mode = mode;
	}
	public VlanMember(JsonObject json) {
		JsonUtils.fromJson(json, this, VlanMember.class);
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
		return Objects.equals(toString(), ((VlanMember) obj).toString());
	}
	@Override
	public int hashCode() {
		return Objects.hash(itfName+mode.getValue());
	}
}
