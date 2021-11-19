package io.nms.central.microservice.digitaltwin.model.ipnetApi;

import java.util.Objects;

import io.nms.central.microservice.common.functional.JsonUtils;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class Link {
	
	private String srcDevice;
    private String srcInterface;
    private String destDevice;
    private String destInterface;
	
	/*-----------------------------------------------*/
	
	public String getSrcDevice() {
		return srcDevice;
	}
	public void setSrcDevice(String srcDevice) {
		this.srcDevice = srcDevice;
	}
	public String getSrcInterface() {
		return srcInterface;
	}
	public void setSrcInterface(String srcInterface) {
		this.srcInterface = srcInterface;
	}
	public String getDestDevice() {
		return destDevice;
	}
	public void setDestDevice(String destDevice) {
		this.destDevice = destDevice;
	}
	public String getDestInterface() {
		return destInterface;
	}
	public void setDestInterface(String destInterface) {
		this.destInterface = destInterface;
	}
	public Link() {}
	public Link(JsonObject json) {
		JsonUtils.fromJson(json, this, Link.class);
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
		return Objects.equals(toString(), ((Link) obj).toString());
	}
	@Override
	public int hashCode() {
		return Objects.hash(srcDevice+srcInterface+destDevice+destInterface);
	}
}
