package org.douglas.kafka.plain;

import org.douglas.kafka.plainjson.Customer;
import org.douglas.kafka.plainjson.EnrichedOrder;
import org.douglas.kafka.plainjson.Order;

public record ComposedOrder(Customer customer, Order order) {

    public EnrichedOrder toAggregated() {
        return new EnrichedOrder(order.metadata(), order.orderId(), customer, order.orderDate(), order.orderLines());
    }
}
