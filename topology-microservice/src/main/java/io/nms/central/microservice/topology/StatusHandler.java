package io.nms.central.microservice.topology;

import java.util.HashMap;
import java.util.Map;

import io.nms.central.microservice.common.BaseMicroserviceVerticle;
import io.nms.central.microservice.notification.NotificationService;
import io.nms.central.microservice.notification.model.Status;
import io.nms.central.microservice.notification.model.Status.ResTypeEnum;
import io.nms.central.microservice.notification.model.Status.StatusEnum;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class StatusHandler extends BaseMicroserviceVerticle {

	private static final Logger logger = LoggerFactory.getLogger(StatusHandler.class);
	
	private final TopologyService topologyService;
	private Map<Integer,Long> statusTimers;

	public StatusHandler(TopologyService topologyService) {
		this.topologyService = topologyService;
		this.statusTimers = new HashMap<Integer,Long>();
	}

	@Override
	public void start(Promise<Void> promise) throws Exception {
		super.start(promise);
		vertx.eventBus().consumer(NotificationService.STATUS_ADDRESS, ar -> {
			Status status = new Status(((JsonObject)ar.body()));
			initHandleStatus(status, done -> {});
		});
	}

	private void initHandleStatus(Status status, Handler<AsyncResult<Void>> resultHandler) {
		if (!status.getResType().equals(ResTypeEnum.NODE)) {
			dispatchStatus(status, resultHandler);
			return;
		}
		int resId = status.getResId();
		if (statusTimers.containsKey(resId)) {
			if (status.getStatus().equals(StatusEnum.UP) || status.getStatus().equals(StatusEnum.DISCONN)) {
				vertx.cancelTimer(statusTimers.get(resId));
				statusTimers.remove(resId);
				dispatchStatus(status, resultHandler);
			}
		} else {
			if (status.getStatus().equals(StatusEnum.UP) || status.getStatus().equals(StatusEnum.DISCONN)) {
				dispatchStatus(status, resultHandler);
			} else {
				long timerId = vertx.setTimer(5000, new Handler<Long>() {
				    @Override
				    public void handle(Long aLong) {
				    	dispatchStatus(status, resultHandler);
				    	statusTimers.remove(status.getResId());
				    }
				});
				statusTimers.put(resId, timerId);
			}
		}
	}
	private void dispatchStatus(Status status, Handler<AsyncResult<Void>> resultHandler) {
		int resId = status.getResId();
		StatusEnum resStatus = status.getStatus();

		switch(status.getResType()) {
			case NODE:
				topologyService.updateNodeStatus(resId, resStatus, ar -> {
					if (ar.succeeded()) {
						resultHandler.handle(Future.succeededFuture());
						notifyFrontend();
						notifyTopologyChange();
					} else {
						resultHandler.handle(Future.failedFuture(ar.cause()));
						ar.cause().printStackTrace();
					}
				});
			break;
			case LTP:
				topologyService.updateLtpStatus(resId, resStatus, null, ar -> {
					if (ar.succeeded()) {
						resultHandler.handle(Future.succeededFuture());
						notifyFrontend();
						notifyTopologyChange();
					} else {
						resultHandler.handle(Future.failedFuture(ar.cause()));
						ar.cause().printStackTrace();
					}
				});
			break;
			case CTP:
				topologyService.updateCtpStatus(resId, null, resStatus, null, ar -> {
					if (ar.succeeded()) {
						resultHandler.handle(Future.succeededFuture());
						notifyFrontend();
						notifyTopologyChange();
					} else {
						resultHandler.handle(Future.failedFuture(ar.cause()));
						ar.cause().printStackTrace();
					}
				});
			break;
			case LINK:
				topologyService.updateLinkStatus(resId, resStatus, null, ar -> {
					if (ar.succeeded()) {
						resultHandler.handle(Future.succeededFuture());
						notifyFrontend();
						notifyTopologyChange();
					} else {
						resultHandler.handle(Future.failedFuture(ar.cause()));
						ar.cause().printStackTrace();
					}
				});
			break;
			case LC:
				topologyService.updateLcStatus(resId, resStatus, null, ar -> {
					if (ar.succeeded()) {
						resultHandler.handle(Future.succeededFuture());
						notifyFrontend();
						notifyTopologyChange();
					} else {
						resultHandler.handle(Future.failedFuture(ar.cause()));
						ar.cause().printStackTrace();
					}
				});
			break;
			case CONNECTION:
				topologyService.updateConnectionStatus(resId, resStatus, null, ar -> {
					if (ar.succeeded()) {
						resultHandler.handle(Future.succeededFuture());
						notifyFrontend();
						notifyTopologyChange();
					} else {
						resultHandler.handle(Future.failedFuture(ar.cause()));
						ar.cause().printStackTrace();
					}
				});
			break;
			case TRAIL:
				topologyService.updateTrailStatus(resId, resStatus, null, ar -> {
					if (ar.succeeded()) {
						resultHandler.handle(Future.succeededFuture());
						notifyFrontend();
						notifyTopologyChange();
					} else {
						resultHandler.handle(Future.failedFuture(ar.cause()));
						ar.cause().printStackTrace();
					}
				});
			break;
			case XC:
				topologyService.updateCrossConnectStatus(resId, resStatus, null, ar -> {
					if (ar.succeeded()) {
						resultHandler.handle(Future.succeededFuture());
						notifyFrontend();
						notifyTopologyChange();
					} else {
						resultHandler.handle(Future.failedFuture(ar.cause()));
						ar.cause().printStackTrace();
					}
				});
			break;
			default:
			break;
		  }
	}
	
	private void notifyFrontend() {
		vertx.eventBus().publish(TopologyService.FROTNEND_ADDRESS, new JsonObject());
	}
	 
	private void notifyTopologyChange() {
		vertx.eventBus().publish(TopologyService.EVENT_ADDRESS, new JsonObject());
	}
}