package org.douglas.kafka.avro.producer;

import io.cloudevents.CloudEventMetadata;
import io.smallrye.reactive.messaging.ce.OutgoingCloudEventMetadata;
import io.smallrye.reactive.messaging.kafka.Record;
import org.douglas.kafka.avro.schema.Order;
import org.douglas.kafka.avro.schema.OrderLine;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletionStage;

@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path("/v1/orders")
public class OrderProducerResource {

    private static final Logger LOGGER = Logger.getLogger(OrderProducerResource.class);

    @Channel("orders")
    Emitter<Record<String, Order>> emitter;

    @POST
    public CompletionStage<Object> enqueueOrders(Order order) {
        LOGGER.infof("Sending Order %s to Kafka", order.getOrderId());

        final OutgoingCloudEventMetadata<Object> cloudEventMetadata = buildOutgoingCloudEventMetadata(order);
        setCloudEventMetadata(order, cloudEventMetadata);

        return emitter.send(Record.of(order.getOrderId(), order))
                .thenApply(unused -> Response.accepted(cloudEventMetadata).build());
    }

    private OutgoingCloudEventMetadata<Object> buildOutgoingCloudEventMetadata(Order order) {
        final CloudEventMetadata metadata;
        if (order.getMetadata() == null) {
            metadata = new CloudEventMetadata();
        } else {
            metadata = order.getMetadata();
        }

        return OutgoingCloudEventMetadata.builder()
                .withId(metadata.getId() == null ? "id-order-" + order.getOrderId() : metadata.getId())
                .withSource(metadata.getSource() == null ? URI.create("https://order.website.io/").resolve(order.getOrderId()) : URI.create(metadata.getSource()))
                .withSubject(metadata.getSubject() == null ? "order-value" : metadata.getSubject())
                .withType(metadata.getType() == null ? order.getClass().getName() : metadata.getType())
                .withDataContentType(metadata.getDataContentType() == null ? MediaType.APPLICATION_JSON : metadata.getDataContentType())
                .withTimestamp(metadata.getTimestamp() == null ? ZonedDateTime.now(ZoneId.of("Europe/Paris")) : ZonedDateTime.parse(metadata.getTimestamp())) // ECT: Europe/Paris
                .build();
    }

    private void setCloudEventMetadata(Order order, OutgoingCloudEventMetadata<Object> cloudEventMetadata) {
        order.setMetadata(CloudEventMetadata.newBuilder()
                .setId(cloudEventMetadata.getId())
                .setSource(cloudEventMetadata.getSource().toASCIIString())
                .setSubject(cloudEventMetadata.getSubject().orElse(null))
                .setType(cloudEventMetadata.getType())
                .setDataContentType(cloudEventMetadata.getDataContentType().orElse(null))
                .setTimestamp(cloudEventMetadata.getTimeStamp().map(ZonedDateTime::toString).orElse(null))
                .build());
    }

}
