package org.douglas.kafka.avro.streams;

import org.douglas.kafka.avro.schema.Customer;
import org.douglas.kafka.avro.schema.EnrichedOrder;
import org.douglas.kafka.avro.schema.Order;

public record ComposedOrder(Customer customer, Order order) {

    public EnrichedOrder toAggregated() {
        return new EnrichedOrder(order.getMetadata(), order.getOrderId(), customer, order.getOrderDate(), order.getOrderLines());
    }
}
