package org.douglas.kafka.plainjson;

import java.util.List;

public record Customer(io.cloudevents.CloudEventMetadata metadata, String customerId,
                       String firstname, String lastname,
                       List<Address> addresses) {
}
