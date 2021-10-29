package io.nms.central.microservice.configuration.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import io.nms.central.microservice.topology.model.Prefix;
import io.nms.central.microservice.topology.model.Vconnection;
import io.nms.central.microservice.topology.model.Vnode;
import io.nms.central.microservice.configuration.model.Route;
import io.vertx.core.Future;
import io.vertx.core.Promise;

public class Routing {
	
	// private static final Logger logger = LoggerFactory.getLogger(Routing.class);
	private static final int ROUTE_ORIGIN_AUTOGEN = 10; 

	public Routing () {}

	public Future<List<Route>> computeRoutes(List<Vnode> nodes, List<Vconnection> edges, List<Prefix> pas) {
		Promise<List<Route>> promise = Promise.promise();
		
		if (edges.isEmpty()) {
			promise.complete(new ArrayList<Route>());
			return promise.future();
		}
		
		GraphBuilder gb = new GraphBuilder();
		edges.forEach(edge -> {
			gb.addSuccessor(edge.getSrcVnodeId(), edge.getDestVnodeId(), edge.getSrcVctpId(), 0);
			gb.addSuccessor(edge.getDestVnodeId(), edge.getSrcVnodeId(), edge.getDestVctpId(), 0);
		});

		// generate correct Integer IDs for prefixAnns:
		List<Integer> nodeIds = new ArrayList<Integer>(gb.getGraph().keySet());
		Map<Integer, Prefix> paMap = new HashMap<Integer, Prefix>();
		int paId = Collections.max(nodeIds) + 1;
		for(Prefix pa : pas) {
			if (nodeIds.contains(pa.getOriginId())) {
				paMap.put(paId, pa);		
				gb.addSuccessor(pa.getOriginId(), paId, -1, 0);
				gb.addSuccessor(paId, pa.getOriginId(), -1, 0);
				paId+=1;
			}
		}

		List<Route> routes = new ArrayList<Route>(); 
		// STEP 3: Compute shortest path from each node to each prefix
		for (HashMap.Entry<Integer, Prefix> pa : paMap.entrySet()) {
			Integer target = pa.getKey();
			Map<Integer, Stack<Integer>> paths = PathComputation.dijkstra(target, gb.getGraph());
			Set<Integer> processedNodes = new HashSet<Integer>();
			
			for (HashMap.Entry<Integer, Stack<Integer>> entry : paths.entrySet()) {
				Stack<Integer> path = entry.getValue();
				if (!paMap.containsKey(path.peek())) {
					while(path.size() > 2) {						
						// STEP 4: set Routing Table Entry for each node on the path
						Integer thisNode = path.pop();
						if (!processedNodes.contains(thisNode)) {
							int nh = path.peek();
							Route r = new Route();
							r.setPrefix(pa.getValue().getName());
							r.setNodeId(thisNode);
							r.setPaId(pa.getValue().getId());
							r.setNextHopId(nh);
							r.setFaceId(gb.getFace(thisNode, nh));
							r.setCost(path.size()-1);
							r.setOrigin(ROUTE_ORIGIN_AUTOGEN);
							routes.add(r);
							processedNodes.add(thisNode);
						}
					}
				}
			}
		}
		promise.complete(routes);
		return promise.future();
	}
}


