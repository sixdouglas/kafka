package org.douglas.kafka.avro.streams;

import org.douglas.kafka.avro.schema.Customer;
import org.douglas.kafka.avro.schema.EnrichedOrder;
import org.douglas.kafka.avro.schema.Order;
import org.douglas.kafka.avro.schema.OrderLine;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public record ComposedOrder(Customer customer, Order order) {

    public EnrichedOrder toAggregated() {
        List<OrderLine> orderLines = new ArrayList<>(order.getOrderLines().size());
        AtomicReference<Float> orderTotalPrice = new AtomicReference<>(0F);

        order.getOrderLines().forEach(orderLine -> {
            orderLines.add(OrderLine.newBuilder()
                .setLineId(orderLine.getLineId())
                .setProductId(orderLine.getProductId())
                .setQuantity(orderLine.getQuantity())
                .setTotalPrice(orderLine.getQuantity() * orderLine.getUnitPrice())
                .setUnitPrice(orderLine.getUnitPrice())
                .build());

            orderTotalPrice.updateAndGet(v -> v + orderLine.getQuantity() * orderLine.getUnitPrice());
        });

        return new EnrichedOrder(order.getMetadata(), order.getOrderId(), customer, order.getOrderDate(), orderTotalPrice.get(), orderLines);
    }
}
