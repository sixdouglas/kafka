package org.douglas.kafka.avro.producer;

import io.cloudevents.CloudEventMetadata;
import io.smallrye.reactive.messaging.ce.OutgoingCloudEventMetadata;
import io.smallrye.reactive.messaging.kafka.Record;
import org.douglas.kafka.avro.schema.Customer;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.CompletionStage;

@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path("/v1/customers")
public class CustomerProducerResource {

    private static final Logger LOGGER = Logger.getLogger(CustomerProducerResource.class);

    @Channel("customers")
    Emitter<Record<String, Customer>> emitter;

    @ConfigProperty(name = "mp.messaging.connector.smallrye-kafka.schema.registry.url")
    String registryUrl;

    @ConfigProperty(name = "mp.messaging.outgoing.customers.topic")
    String topic;

    @POST
    public CompletionStage<Object> enqueueOrders(Customer customer) {
        LOGGER.infof("Sending Customer %s to Kafka", customer.getCustomerId());

        return emitter.send(Record.of(customer.getCustomerId(), customer))
                .thenApply(unused -> buildResponse(customer));
    }

    private Response buildResponse(Customer customer) {
        final OutgoingCloudEventMetadata<Object> cloudEventMetadata = buildOutgoingCloudEventMetadata(customer);
        return Response.accepted(cloudEventMetadata).build();
    }

    private OutgoingCloudEventMetadata<Object> buildOutgoingCloudEventMetadata(Customer customer) {
        final CloudEventMetadata metadata;
        if (customer.getMetadata() == null) {
            metadata = new CloudEventMetadata();
        } else {
            metadata = customer.getMetadata();
        }

        final OutgoingCloudEventMetadata<Object> cloudEventMetadata = OutgoingCloudEventMetadata.builder()
                .withId(metadata.getId() == null ? "id-customer-" + customer.getCustomerId() : metadata.getId())
                .withSource(metadata.getSource() == null ? URI.create("https://customer.website.io/").resolve(customer.getCustomerId()) : URI.create(metadata.getSource()))
                .withSubject(metadata.getSubject() == null ? "avro.customers-value" : metadata.getSubject())
                .withType(metadata.getType() == null ? customer.getClass().getName() : metadata.getType())
                .withDataContentType(metadata.getDataContentType() == null ? MediaType.APPLICATION_JSON : metadata.getDataContentType())
                .withTimestamp(metadata.getTimestamp() == null ? ZonedDateTime.now(ZoneId.of("Europe/Paris")) : ZonedDateTime.parse(metadata.getTimestamp())) // ECT: Europe/Paris
                .withDataSchema(URI.create(registryUrl).resolve("subjects/" + topic + "-value/versions"))
                .build();

        setCloudEventMetadata(customer, cloudEventMetadata);
        return cloudEventMetadata;
    }

    private void setCloudEventMetadata(Customer customer, OutgoingCloudEventMetadata<Object> cloudEventMetadata) {
        customer.setMetadata(CloudEventMetadata.newBuilder()
                .setId(cloudEventMetadata.getId())
                .setSource(cloudEventMetadata.getSource().toASCIIString())
                .setSubject(cloudEventMetadata.getSubject().orElse(null))
                .setType(cloudEventMetadata.getType())
                .setDataContentType(cloudEventMetadata.getDataContentType().orElse(null))
                .setTimestamp(cloudEventMetadata.getTimeStamp().map(ZonedDateTime::toString).orElse(null))
                .build());
    }

}
