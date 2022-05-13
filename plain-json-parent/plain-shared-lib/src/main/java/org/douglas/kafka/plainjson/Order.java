package org.douglas.kafka.plainjson;

import java.time.Instant;
import java.util.List;

public record Order(io.cloudevents.CloudEventMetadata metadata, String orderId, String customerId, Instant orderDate, List<OrderLine> orderLines){
}
