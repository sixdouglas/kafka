package org.douglas.kafka.plain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.reactive.messaging.kafka.Record;
import io.smallrye.reactive.messaging.providers.connectors.InMemoryConnector;
import io.smallrye.reactive.messaging.providers.connectors.InMemorySink;
import org.douglas.kafka.plainjson.Customer;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.enterprise.inject.Any;
import javax.inject.Inject;
import java.util.List;

import static org.awaitility.Awaitility.await;
import static org.douglas.kafka.plain.CustomerGenerator.CUSTOMER_TOPIC;

@QuarkusTest
//@Disabled // Not able to activate both tests at the same time
@QuarkusTestResource(KafkaTestResourceLifecycleManager.class)
class CustomerGeneratorTest {

    @Inject
    @Any
    InMemoryConnector connector;

    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void testCustomer() throws JsonProcessingException {
        InMemorySink<Record<String, String>> customerOut = connector.sink(CUSTOMER_TOPIC);

        await().<List<? extends Message<Record<String, String>>>>until(customerOut::received, t -> t.size() == 1);

        Record<String, String> queuedCustomer = customerOut.received().get(0).getPayload();
        Customer customer = objectMapper.readValue(queuedCustomer.value(), Customer.class);
        Assertions.assertEquals("12 rue nationale", customer.addresses().get(0).road());
    }
}