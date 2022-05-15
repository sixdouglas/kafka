package org.douglas.kafka.plain;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@ApplicationScoped
@Path("/orders")
public class OrderRestEndpoint {

    @Inject
    InteractiveQueries interactiveQueries;

    @ConfigProperty(name = "quarkus.http.ssl-port")
    int sslPort;

    @GET
    @Path("/data/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getWeatherStationData(@PathParam("id") String id) {
        GetDataResult result = interactiveQueries.getOrderData(id);

        if (result.getResult().isPresent()) {
            return Response.ok(result.getResult().get()).build();
        } else if (result.getHost().isPresent()) {
            URI otherUri = getOtherUri(result.getHost().get(), result.getPort().getAsInt(), id);
            return Response.seeOther(otherUri).build();
        } else {
            return Response.status(Status.NOT_FOUND.getStatusCode(), "No data found for order " + id).build();
        }
    }

    @GET
    @Path("/meta-data")
    @Produces(MediaType.APPLICATION_JSON)
    public List<PipelineMetadata> getMetaData() {
        return interactiveQueries.getMetaData();
    }

    private URI getOtherUri(String host, int port, String id) {
        try {
            String scheme = (port == sslPort) ? "https" : "http";
            return new URI(scheme + "://" + host + ":" + port + "/orders/" + id);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
