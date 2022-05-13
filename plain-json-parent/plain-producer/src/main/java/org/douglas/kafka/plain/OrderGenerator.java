package org.douglas.kafka.plain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.messaging.kafka.Record;
import org.douglas.kafka.plainjson.Address;
import org.douglas.kafka.plainjson.Customer;
import org.douglas.kafka.plainjson.Order;
import org.douglas.kafka.plainjson.OrderLine;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.douglas.kafka.plain.CustomerGenerator.CUSTOMERS;

@ApplicationScoped
public class OrderGenerator {

    static final String ORDER_TOPIC = "order";

    private static final Logger LOG = Logger.getLogger(OrderGenerator.class);

    private final Random random = new Random();
    final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Outgoing(ORDER_TOPIC)
    public Multi<Record<String, String>> generate() {
        return Multi.createFrom().ticks().every(Duration.ofMillis(500))
                .onOverflow().drop()
                .map(tick -> {
                    Customer customer = CUSTOMERS.get(random.nextInt(CUSTOMERS.size()));
                    Order order = new Order(null, UUID.randomUUID().toString(), customer.customerId(), Instant.now(), List.of(new OrderLine(UUID.randomUUID().toString(), UUID.randomUUID().toString(), BigDecimal.valueOf(random.nextDouble() * 100), BigDecimal.valueOf(random.nextDouble() * 100))));

                    LOG.infov("customer id: {0}, order id: {1}", customer.customerId(), order.orderId());
                    return getOrderRecord(order);
                });
    }

    private Record<String, String> getOrderRecord(Order order) {
        LOG.infov("Actual order: {0}", order);
        try {
            return Record.of(order.orderId(), mapper.writeValueAsString(order));
        } catch (JsonProcessingException e) {
            LOG.error("Error during serialisation of Order: " + order.orderId(), e);
        }
        return Record.of(order.orderId(), "");
    }
}
