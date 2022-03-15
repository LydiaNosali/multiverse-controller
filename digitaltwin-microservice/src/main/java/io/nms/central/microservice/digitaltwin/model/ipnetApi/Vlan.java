package io.nms.central.microservice.digitaltwin.model.ipnetApi;

import java.util.Objects;

import io.nms.central.microservice.common.functional.JsonUtils;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class Vlan {
	
	private int vid;
	private String name;
    private String address;
	
	/*-----------------------------------------------*/
	
    public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public int getVid() {
		return vid;
	}
	public void setVid(int vid) {
		this.vid = vid;
	}
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	public Vlan() {}
	public Vlan(JsonObject json) {
		JsonUtils.fromJson(json, this, Vlan.class);
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
		return Objects.equals(toString(), ((Vlan) obj).toString());
	}
	@Override
	public int hashCode() {
		return Objects.hash(vid+name+address);
	}
}
