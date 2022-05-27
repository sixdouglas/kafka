package org.douglas.kafka.avro.streams;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.cloudevents.CloudEventMetadata;
import org.douglas.kafka.avro.schema.Address;
import org.douglas.kafka.avro.schema.Customer;
import org.douglas.kafka.avro.schema.EnrichedOrder;
import org.douglas.kafka.avro.schema.Order;
import org.douglas.kafka.avro.schema.OrderLine;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

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
    private static final Logger LOG = Logger.getLogger(OrderRestEndpoint.class);

    @Inject
    InteractiveQueries interactiveQueries;

    @ConfigProperty(name = "quarkus.http.ssl-port")
    int sslPort;

    ObjectMapper mapper = new ObjectMapper()
            .addMixIn(CloudEventMetadata.class, IgnoreAvroProperties.class)
            .addMixIn(Address.class, IgnoreAvroProperties.class)
            .addMixIn(Customer.class, IgnoreAvroProperties.class)
            .addMixIn(EnrichedOrder.class, IgnoreAvroProperties.class)
            .addMixIn(Order.class, IgnoreAvroProperties.class)
            .addMixIn(OrderLine.class, IgnoreAvroProperties.class)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(SerializationFeature.WRITE_DATES_WITH_CONTEXT_TIME_ZONE)
            .registerModule(new JavaTimeModule())
            ;

    @GET
    @Path("/data/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getWeatherStationData(@PathParam("id") String id) {
        GetDataResult result = interactiveQueries.getOrderData(id);

        if (result.getResult().isPresent()) {
            return Response.ok(getValueAsString(result)).build();
        } else if (result.getHost().isPresent()) {
            URI otherUri = getOtherUri(result.getHost().get(), result.getPort().getAsInt(), id);
            return Response.seeOther(otherUri).build();
        } else {
            return Response.status(Status.NOT_FOUND.getStatusCode(), "No data found for order " + id).build();
        }
    }

    private String getValueAsString(GetDataResult result) {
        try {
            return mapper.writeValueAsString(result.getResult().get());
        } catch (JsonProcessingException e) {
            LOG.error("Error serializing response", e);
            throw new RuntimeException(e);
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
