package org.douglas.kafka.ksqldb.rest.restclient;

import io.smallrye.mutiny.Multi;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/")
@RegisterRestClient(configKey="ksqldb-api")
public interface IKsqlDbClient {

    @POST
    @Path("query")
    @Consumes("application/vnd.ksql.v1+json")
    @Produces(MediaType.APPLICATION_JSON)
    Multi<String> getOrders(String body);
}
