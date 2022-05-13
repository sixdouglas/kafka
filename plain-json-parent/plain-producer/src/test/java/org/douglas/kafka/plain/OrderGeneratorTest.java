package org.douglas.kafka.plain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.reactive.messaging.kafka.Record;
import io.smallrye.reactive.messaging.providers.connectors.InMemoryConnector;
import io.smallrye.reactive.messaging.providers.connectors.InMemorySink;
import org.douglas.kafka.plainjson.Order;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.enterprise.inject.Any;
import javax.inject.Inject;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.awaitility.Awaitility.await;
import static org.douglas.kafka.plain.OrderGenerator.ORDER_TOPIC;

@QuarkusTest
@Disabled // Not able to activate both tests at the same time
@QuarkusTestResource(KafkaTestResourceLifecycleManager.class)
class OrderGeneratorTest {

    @Inject
    @Any
    InMemoryConnector connector;

    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void testOrder() throws JsonProcessingException {
        InMemorySink<Record<String, String>> orderOut = connector.sink(ORDER_TOPIC);

        await().<List<? extends Message<Record<String, String>>>>until(orderOut::received, t -> t.size() == 1);

        Record<String, String> queuedOrder = orderOut.received().get(0).getPayload();
        Order order = objectMapper.readValue(queuedOrder.value(), Order.class);
        assertThat(Instant.now()).isCloseTo(order.orderDate(), within(2, ChronoUnit.SECONDS));
    }
}