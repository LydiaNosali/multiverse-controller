package io.nms.central.microservice.digitaltwin.api;

import java.util.List;

import graphql.GraphQL;
import graphql.GraphQLContext;
import graphql.execution.DataFetcherResult;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.schema.idl.TypeRuntimeWiring;
import io.nms.central.microservice.digitaltwin.DigitalTwinService;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.AclRule;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.AclTable;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.Arp;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.Bgp;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.Device;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.IpRoute;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.NetInterface;
import io.nms.central.microservice.digitaltwin.model.ipnetApi.Network;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.handler.graphql.GraphQLHandler;
import io.vertx.ext.web.handler.graphql.VertxDataFetcher;

public class NetworkQueryEngine {

	private static final Logger logger = LoggerFactory.getLogger(NetworkQueryEngine.class);

	private DigitalTwinService service;

	public NetworkQueryEngine(DigitalTwinService service) {
		this.service = service;
	}

	public GraphQLHandler setupQueryEngine(String schema) {
		SchemaParser schemaParser = new SchemaParser();
		TypeDefinitionRegistry typeDefinitionRegistry = schemaParser.parse(schema);

		RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring()
				.type(TypeRuntimeWiring.newTypeWiring("Query")
						.dataFetcher("network", networkDataFetcher))
				.type(TypeRuntimeWiring.newTypeWiring("Device")
						.dataFetcher("interfaces", interfacesDataFetcher)
						.dataFetcher("ipRoutes", ipRoutesDataFetcher)
						.dataFetcher("arp", arpsDataFetcher)
						.dataFetcher("acl", aclTablesDataFetcher))
				.type(TypeRuntimeWiring.newTypeWiring("Interface")
						.dataFetcher("bgp", bgpDataFetcher))
				.type(TypeRuntimeWiring.newTypeWiring("AclTable")
						.dataFetcher("rules", aclRulesDataFetcher))
				.build();

		SchemaGenerator schemaGenerator = new SchemaGenerator();
		GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);

		GraphQL gql = GraphQL.newGraphQL(graphQLSchema).build();
		return GraphQLHandler.create(gql);
	}

	/* Network level */
	VertxDataFetcher<Network> networkDataFetcher = new VertxDataFetcher<>((environment, future) -> {
		service.runningGetNetwork(future);
	});

	/* Device level */
	VertxDataFetcher<DataFetcherResult<List<NetInterface>>> interfacesDataFetcher = 
			new VertxDataFetcher<>((environment, future) -> {
		Device device = environment.getSource();
		String deviceName = device.getName();

		service.runningGetDeviceInterfaces(deviceName, ar -> {
			if (ar.succeeded()) {
				GraphQLContext rc = GraphQLContext.newContext().build();
				rc.put("deviceName", deviceName);
				future.complete(DataFetcherResult.<List<NetInterface>>newResult()
						.data(ar.result())
						.localContext(rc)
						.build());
			} else {
				future.fail(ar.cause());
			}
		});
	});
	VertxDataFetcher<List<IpRoute>> ipRoutesDataFetcher = 
			new VertxDataFetcher<>((environment, future) -> {
		Device device = environment.getSource();
		String deviceName = device.getName();
		service.runningGetDeviceIpRoutes(deviceName, future);
	});
	VertxDataFetcher<List<Arp>> arpsDataFetcher = 
			new VertxDataFetcher<>((environment, future) -> {
		Device device = environment.getSource();
		String deviceName = device.getName();
		service.runningGetDeviceArps(deviceName, future);
	});
	VertxDataFetcher<DataFetcherResult<List<AclTable>>> aclTablesDataFetcher = 
			new VertxDataFetcher<>((environment, future) -> {
		Device device = environment.getSource();
		String deviceName = device.getName();

		service.runningGetDeviceAclTables(deviceName, ar -> {
			if (ar.succeeded()) {
				GraphQLContext rc = GraphQLContext.newContext().build();
				rc.put("deviceName", deviceName);
				future.complete(DataFetcherResult.<List<AclTable>>newResult()
						.data(ar.result())
						.localContext(rc)
						.build());
			} else {
				future.fail(ar.cause());
			}
		});
	});

	/* Interface level */
	VertxDataFetcher<Bgp> bgpDataFetcher = 
			new VertxDataFetcher<>((environment, future) -> {
		NetInterface itf = environment.getSource();
		String itfAddr = itf.getIpAddr();
		GraphQLContext rc = environment.getLocalContext();
		if (itfAddr != null) {
			service.runningGetBgp(rc.get("deviceName"), itfAddr, future);
		} else {
			future.complete(null);
		}
	});

	/* AclTable level */
	VertxDataFetcher<List<AclRule>> aclRulesDataFetcher = 
			new VertxDataFetcher<>((environment, future) -> {
		AclTable table = environment.getSource();
		String tableName = table.getName();
		GraphQLContext rc = environment.getLocalContext();
		service.runningGetAclRules(rc.get("deviceName"), tableName, future);
	});
}
