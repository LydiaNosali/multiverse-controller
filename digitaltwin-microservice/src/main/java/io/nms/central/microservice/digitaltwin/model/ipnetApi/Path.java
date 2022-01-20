package io.nms.central.microservice.digitaltwin.model.ipnetApi;

import java.util.List;
import java.util.Objects;
import io.nms.central.microservice.common.functional.JsonUtils;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class Path {

	private List<PathHop> path;
	private List<RouteHop> route;

	public Path() {}
	public Path(JsonObject json) {
		JsonUtils.fromJson(json, this, Path.class);
	}

	public JsonObject toJson() {
		return new JsonObject(JsonUtils.pojo2Json(this, false));
	}
	@Override
	public String toString() {
		return JsonUtils.pojo2Json(this, false);
	}
	@Override
	public boolean equals(Object obj) {
		return Objects.equals(toString(), ((Path) obj).toString());
	}
	@Override
	public int hashCode() {
		return Objects.hash(path.hashCode()+route.hashCode());
	}
	public List<PathHop> getPath() {
		return path;
	}
	public void setPath(List<PathHop> path) {
		this.path = path;
	}
	public List<RouteHop> getRoute() {
		return route;
	}
	public void setRoute(List<RouteHop> route) {
		this.route = route;
	}
}
