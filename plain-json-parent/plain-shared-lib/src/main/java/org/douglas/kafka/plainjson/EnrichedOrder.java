package org.douglas.kafka.plainjson;

import java.time.Instant;
import java.util.List;

public record EnrichedOrder(io.cloudevents.CloudEventMetadata metadata, String orderId, Customer customer,
                            Instant orderDate, List<OrderLine> orderLines){
}
