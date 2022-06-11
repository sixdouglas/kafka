package org.douglas.kafka.ksqldb.rest.javaclient;

import io.confluent.ksql.api.client.Client;
import io.confluent.ksql.api.client.StreamedQueryResult;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@ApplicationScoped
@Path("/javaclient/orders")
public class OrderRestEndpoint {

    @Inject
    Client ksqldbClient;

    @GET
    @Path("/data/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<StreamedQueryResult> getOrderData(@PathParam("id") String id) {
        return Uni.createFrom()
                .future(ksqldbClient.streamQuery("SELECT * FROM orders_enriched WHERE order_id = '" + id + "';"));
    }

    @GET
    @Path("/data/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Multi<StreamedQueryResult> getOrdersData() {
        return Multi.createFrom()
                .completionStage(ksqldbClient.streamQuery("SELECT * FROM orders_enriched;"));
    }
}
